/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.provider;

import static aQute.bnd.osgi.Constants.LAUNCH_ACTIVATION_EAGER;
import static com.osgifx.console.agent.dto.XResultDTO.ERROR;
import static com.osgifx.console.agent.dto.XResultDTO.SKIPPED;
import static java.util.Objects.requireNonNull;
import static org.osgi.framework.Bundle.START_ACTIVATION_POLICY;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.dto.CapabilityDTO;
import org.osgi.resource.dto.RequirementDTO;
import org.osgi.util.tracker.ServiceTracker;

import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.AgentExtension;
import com.osgifx.console.agent.dto.ConfigValue;
import com.osgifx.console.agent.dto.XAttributeDefType;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XHeapUsageDTO;
import com.osgifx.console.agent.dto.XHeapdumpDTO;
import com.osgifx.console.agent.dto.XHttpComponentDTO;
import com.osgifx.console.agent.dto.XMemoryInfoDTO;
import com.osgifx.console.agent.dto.XPropertyDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.agent.dto.XServiceDTO;
import com.osgifx.console.agent.dto.XThreadDTO;
import com.osgifx.console.supervisor.Supervisor;

import aQute.bnd.exceptions.Exceptions;
import aQute.lib.converter.Converter;
import aQute.lib.converter.TypeReference;
import aQute.lib.io.ByteBufferInputStream;
import aQute.lib.startlevel.StartLevelRuntimeHandler;
import aQute.libg.shacache.ShaCache;
import aQute.libg.shacache.ShaSource;
import aQute.remote.util.Link;

/**
 * Implementation of the Agent. This implementation implements the Agent
 * interfaces and communicates with a Supervisor interfaces.
 */
public class AgentServer implements Agent, Closeable {

	private static final Pattern       BSN_P    = Pattern.compile("\\s*([^;\\s]+).*");
	private static final AtomicInteger sequence = new AtomicInteger(1000);

	//
	// Constant so we do not have to repeat it
	//

	private static final TypeReference<Map<String, String>> MAP_STRING_STRING_T = new TypeReference<Map<String, String>>() {
	};

	private static final long[] EMPTY = {};

	//
	// Known keys in the framework properties since we cannot
	// iterate over framework properties
	//

	@SuppressWarnings("deprecation")
	static final String keys[] = { Constants.FRAMEWORK_BEGINNING_STARTLEVEL, Constants.FRAMEWORK_BOOTDELEGATION,
	        Constants.FRAMEWORK_BSNVERSION, Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_TRUST_REPOSITORIES,
	        Constants.FRAMEWORK_COMMAND_ABSPATH, Constants.FRAMEWORK_EXECPERMISSION, Constants.FRAMEWORK_EXECUTIONENVIRONMENT,
	        Constants.FRAMEWORK_LANGUAGE, Constants.FRAMEWORK_LIBRARY_EXTENSIONS, Constants.FRAMEWORK_OS_NAME,
	        Constants.FRAMEWORK_OS_VERSION, Constants.FRAMEWORK_PROCESSOR, Constants.FRAMEWORK_SECURITY, Constants.FRAMEWORK_STORAGE,
	        Constants.FRAMEWORK_SYSTEMCAPABILITIES, Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA, Constants.FRAMEWORK_SYSTEMPACKAGES,
	        Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, Constants.FRAMEWORK_UUID, Constants.FRAMEWORK_VENDOR, Constants.FRAMEWORK_VERSION,
	        Constants.FRAMEWORK_WINDOWSYSTEM, };

	private Supervisor                     remote;
	private BundleContext                  context;
	private final ShaCache                 cache;
	private ShaSource                      source;
	private final Map<String, String>      installed  = new HashMap<>();
	volatile boolean                       quit;
	private Redirector                     redirector = new NullRedirector();
	private Link<Agent, Supervisor>        link;
	private CountDownLatch                 refresh    = new CountDownLatch(0);
	private final StartLevelRuntimeHandler startlevels;
	private final int                      startOptions;
	private ClassloaderLeakDetector        leakDetector;

	private final ServiceTracker<Object, Object>                 scrTracker;
	private final ServiceTracker<Object, Object>                 metatypeTracker;
	private final ServiceTracker<Object, Object>                 eventAdminTracker;
	private final ServiceTracker<Object, Object>                 configAdminTracker;
	private final ServiceTracker<Object, Object>                 gogoCommandsTracker;
	private final ServiceTracker<Object, Object>                 httpServiceRuntimeTracker;
	private final ServiceTracker<AgentExtension, AgentExtension> agentExtensionTracker;

	private final Set<String>                 gogoCommands    = new CopyOnWriteArraySet<>();
	private final Map<String, AgentExtension> agentExtensions = new ConcurrentHashMap<>();

	/**
	 * An agent server is based on a context and takes a name and cache directory
	 *
	 * @param context a bundle context of the framework
	 * @param cache   the directory for caching
	 */
	public AgentServer(final BundleContext context, final File cache, final ClassloaderLeakDetector leakDetector) throws Exception {
		this(context, cache, StartLevelRuntimeHandler.absent(), leakDetector);
	}

	public AgentServer(final BundleContext context, final File cache, final StartLevelRuntimeHandler startlevels,
	        final ClassloaderLeakDetector leakDetector) throws Exception {
		requireNonNull(context, "Bundle context cannot be null");
		this.context      = context;
		this.leakDetector = leakDetector;

		final boolean eager = context.getProperty(LAUNCH_ACTIVATION_EAGER) != null;
		startOptions = eager ? 0 : START_ACTIVATION_POLICY;

		this.cache       = new ShaCache(cache);
		this.startlevels = startlevels;

		final Filter gogoCommandFilter = context.createFilter("(osgi.command.scope=*)");

		metatypeTracker           = new ServiceTracker<>(context, "org.osgi.service.metatype.MetaTypeService", null);
		eventAdminTracker         = new ServiceTracker<>(context, "org.osgi.service.event.EventAdmin", null);
		configAdminTracker        = new ServiceTracker<>(context, "org.osgi.service.cm.ConfigurationAdmin", null);
		scrTracker                = new ServiceTracker<>(context, "org.osgi.service.component.runtime.ServiceComponentRuntime", null);
		httpServiceRuntimeTracker = new ServiceTracker<>(context, "org.osgi.service.http.runtime.HttpServiceRuntime", null);
		agentExtensionTracker     = new ServiceTracker<AgentExtension, AgentExtension>(context, AgentExtension.class, null) {

										@Override
										public AgentExtension addingService(final ServiceReference<AgentExtension> reference) {
											final Object name = reference.getProperty(AgentExtension.PROPERTY_KEY);
											if (name == null) {
												return null;
											}
											final AgentExtension tracked = super.addingService(reference);
											agentExtensions.put(name.toString(), tracked);
											return tracked;
										}

										@Override
										public void modifiedService(final ServiceReference<AgentExtension> reference,
										        final AgentExtension service) {
											removedService(reference, service);
											addingService(reference);
										}

										@Override
										public void removedService(final ServiceReference<AgentExtension> reference,
										        final AgentExtension service) {
											final Object name = reference.getProperty(AgentExtension.PROPERTY_KEY);
											if (name == null) {
												return;
											}
											agentExtensions.remove(name);
										}
									};
		gogoCommandsTracker       = new ServiceTracker<Object, Object>(context, gogoCommandFilter, null) {
										@Override
										public Object addingService(final ServiceReference<Object> reference) {
											final String   scope     = String.valueOf(reference.getProperty("osgi.command.scope"));
											final String[] functions = adapt(reference.getProperty("osgi.command.function"));
											addCommand(scope, functions);
											return super.addingService(reference);
										}

										@Override
										public void removedService(final ServiceReference<Object> reference, final Object service) {
											final String   scope     = String.valueOf(reference.getProperty("osgi.command.scope"));
											final String[] functions = adapt(reference.getProperty("osgi.command.function"));
											removeCommand(scope, functions);
										}

										private String[] adapt(final Object value) {
											if (value instanceof String[]) {
												return (String[]) value;
											}
											return new String[] { value.toString() };
										}

										private void addCommand(final String scope, final String... commands) {
											Stream.of(commands).forEach(cmd -> gogoCommands.add(scope + ":" + cmd));
										}

										private void removeCommand(final String scope, final String... commands) {
											Stream.of(commands).forEach(cmd -> gogoCommands.remove(scope + ":" + cmd));
										}
									};
		scrTracker.open();
		metatypeTracker.open();
		eventAdminTracker.open();
		configAdminTracker.open();
		gogoCommandsTracker.open();
		agentExtensionTracker.open();
		httpServiceRuntimeTracker.open();
	}

	/**
	 * Get the framework's DTO
	 */
	@Override
	public FrameworkDTO getFramework() throws Exception {
		final FrameworkDTO fw = new FrameworkDTO();
		fw.bundles    = getBundles();
		fw.properties = getProperties();
		fw.services   = getServiceReferences();
		return fw;
	}

	@Override
	public BundleDTO installWithData(String location, final byte[] data, final int startLevel) throws Exception {
		requireNonNull(data);

		Bundle installedBundle;

		if (location == null) {
			location = getLocation(data);
		}

		try (InputStream stream = new ByteBufferInputStream(data)) {
			installedBundle = context.getBundle(location);
			if (installedBundle == null) {
				installedBundle = context.installBundle(location, stream);
				installedBundle.adapt(BundleStartLevel.class).setStartLevel(startLevel);
			} else {
				installedBundle.update(stream);
				refresh(true);
			}
		}
		return toDTO(installedBundle);
	}

	@Override
	public BundleDTO install(final String location, final String sha) throws Exception {
		final InputStream in = cache.getStream(sha, source);
		if (in == null) {
			return null;
		}

		final Bundle b = context.installBundle(location, in);
		installed.put(b.getLocation(), sha);
		return toDTO(b);
	}

	@Override
	public BundleDTO installFromURL(final String location, final String url) throws Exception {
		final InputStream is = new URL(url).openStream();
		final Bundle      b  = context.installBundle(location, is);
		installed.put(b.getLocation(), url);
		return toDTO(b);
	}

	@Override
	public String start(final long... ids) {
		final StringBuilder sb = new StringBuilder();

		for (final long id : ids) {
			final Bundle bundle = context.getBundle(id);
			try {
				bundle.start(startOptions);
			} catch (final BundleException e) {
				sb.append(e.getMessage()).append("\n");
			}
		}
		return sb.length() == 0 ? null : sb.toString();
	}

	@Override
	public String stop(final long... ids) {
		final StringBuilder sb = new StringBuilder();

		for (final long id : ids) {
			final Bundle bundle = context.getBundle(id);
			try {
				bundle.stop();
			} catch (final BundleException e) {
				sb.append(e.getMessage()).append("\n");
			}
		}
		return sb.length() == 0 ? null : sb.toString();
	}

	@Override
	public String uninstall(final long... ids) {
		final StringBuilder sb = new StringBuilder();

		for (final long id : ids) {
			final Bundle bundle = context.getBundle(id);
			try {
				bundle.uninstall();
				installed.remove(bundle.getLocation());
			} catch (final BundleException e) {
				sb.append(e.getMessage()).append("\n");
			}
		}
		return sb.length() == 0 ? null : sb.toString();
	}

	@Override
	public String update(Map<String, String> bundles) throws InterruptedException {

		refresh.await();

		final Formatter out = new Formatter();
		if (bundles == null) {
			bundles = Collections.emptyMap();
		}

		final Set<String> toBeDeleted = new HashSet<>(installed.keySet());
		toBeDeleted.removeAll(bundles.keySet());

		final LinkedHashSet<String> toBeInstalled = new LinkedHashSet<>(bundles.keySet());
		toBeInstalled.removeAll(installed.keySet());

		final Map<String, String> changed = new HashMap<>(bundles);
		changed.values().removeAll(installed.values());
		changed.keySet().removeAll(toBeInstalled);

		final Set<String> affected = new HashSet<>(toBeDeleted);
		affected.addAll(changed.keySet());

		final LinkedHashSet<Bundle> toBeStarted = new LinkedHashSet<>();

		for (final String location : affected) {
			final Bundle b = getBundle(location);
			if (b == null) {
				out.format("Could not location bundle %s to stop it", location);
				continue;
			}

			try {
				if (isActive(b)) {
					toBeStarted.add(b);
				}

				b.stop();
			} catch (final Exception e) {
				printStack(e);
				out.format("Trying to stop bundle %s : %s", b, e);
			}

		}

		for (final String location : toBeDeleted) {
			final Bundle b = getBundle(location);
			if (b == null) {
				out.format("Could not find bundle %s to uninstall it", location);
				continue;
			}

			try {
				b.uninstall();
				installed.remove(location);
				toBeStarted.remove(b);
			} catch (final Exception e) {
				printStack(e);
				out.format("Trying to uninstall %s: %s", location, e);
			}
		}

		for (final String location : toBeInstalled) {
			final String sha = bundles.get(location);

			try {
				final InputStream in = cache.getStream(sha, source);
				if (in == null) {
					out.format("Could not find file with sha %s for bundle %s", sha, location);
					continue;
				}

				final Bundle b = context.installBundle(location, in);
				installed.put(location, sha);
				toBeStarted.add(b);

			} catch (final Exception e) {
				printStack(e);
				out.format("Trying to install %s: %s", location, e);
			}
		}

		for (final Entry<String, String> e : changed.entrySet()) {
			final String location = e.getKey();
			final String sha      = e.getValue();

			try {
				final InputStream in = cache.getStream(sha, source);
				if (in == null) {
					out.format("Cannot find file for sha %s to update %s", sha, location);
					continue;
				}

				final Bundle bundle = getBundle(location);
				if (bundle == null) {
					out.format("No such bundle for location %s while trying to update it", location);
					continue;
				}

				if (bundle.getState() == Bundle.UNINSTALLED) {
					context.installBundle(location, in);
				} else {
					bundle.update(in);
				}

			} catch (final Exception e1) {
				printStack(e1);
				out.format("Trying to update %s: %s", location, e);
			}
		}

		for (final Bundle b : toBeStarted) {
			try {
				b.start(startOptions);
			} catch (final Exception e1) {
				printStack(e1);
				out.format("Trying to start %s: %s", b, e1);
			}
		}

		final String result = out.toString();
		out.close();
		startlevels.afterStart();
		if (result.length() == 0) {
			refresh(true);
			return null;
		}
		return result;
	}

	@Override
	public String update(final long id, final String sha) throws Exception {
		final InputStream in = cache.getStream(sha, source);
		if (in == null) {
			return null;
		}

		final StringBuilder sb = new StringBuilder();

		try {
			final Bundle bundle = context.getBundle(id);
			bundle.update(in);
			refresh(true);
		} catch (final Exception e) {
			sb.append(e.getMessage()).append("\n");
		}

		return sb.length() == 0 ? null : sb.toString();
	}

	@Override
	public String updateFromURL(final long id, final String url) throws Exception {
		final StringBuilder sb = new StringBuilder();
		try (final InputStream is = new URL(url).openStream()) {
			final Bundle bundle = context.getBundle(id);
			bundle.update(is);
			refresh(true);
		} catch (final Exception e) {
			sb.append(e.getMessage()).append("\n");
		}

		return sb.length() == 0 ? null : sb.toString();
	}

	private Bundle getBundle(final String location) {
		try {
			return context.getBundle(location);
		} catch (final Exception e) {
			printStack(e);
		}
		return null;
	}

	private boolean isActive(final Bundle b) {
		return b.getState() == Bundle.ACTIVE || b.getState() == Bundle.STARTING;
	}

	@Override
	public boolean redirect(final int port) throws Exception {
		if (redirector != null) {
			if (redirector.getPort() == port) {
				return false;
			}

			redirector.close();
			redirector = new NullRedirector();
		}

		if (port == Agent.NONE) {
			return true;
		}

		if (port <= Agent.COMMAND_SESSION) {
			try {
				redirector = new GogoRedirector(this, context);
			} catch (final Exception e) {
				throw new IllegalStateException("Gogo is not present in this framework", e);
			}
			return true;
		}

		if (port == Agent.CONSOLE) {
			redirector = new ConsoleRedirector(this);
			return true;
		}

		redirector = new SocketRedirector(this, port);
		return true;
	}

	@Override
	public boolean stdin(final String s) throws Exception {
		if (redirector != null) {
			redirector.stdin(s);
			return true;
		}
		return false;
	}

	@Override
	public String shell(final String cmd) throws Exception {
		redirect(Agent.COMMAND_SESSION);
		stdin(cmd);
		final PrintStream ps = redirector.getOut();
		if (ps instanceof RedirectOutput) {
			final RedirectOutput rout = (RedirectOutput) ps;
			return rout.getLastOutput();
		}

		return null;
	}

	public void setSupervisor(final Supervisor remote) {
		setRemote(remote);
	}

	private List<ServiceReferenceDTO> getServiceReferences() throws Exception {
		final ServiceReference<?>[] refs = context.getAllServiceReferences(null, null);
		if (refs == null) {
			return Collections.emptyList();
		}

		final ArrayList<ServiceReferenceDTO> list = new ArrayList<>(refs.length);
		for (final ServiceReference<?> r : refs) {
			final ServiceReferenceDTO ref = new ServiceReferenceDTO();
			ref.bundle     = r.getBundle().getBundleId();
			ref.id         = (Long) r.getProperty(Constants.SERVICE_ID);
			ref.properties = getProperties(r);
			final Bundle[] usingBundles = r.getUsingBundles();
			if (usingBundles == null) {
				ref.usingBundles = EMPTY;
			} else {
				ref.usingBundles = new long[usingBundles.length];
				for (int i = 0; i < usingBundles.length; i++) {
					ref.usingBundles[i] = usingBundles[i].getBundleId();
				}
			}
			list.add(ref);
		}
		return list;
	}

	private Map<String, Object> getProperties(final ServiceReference<?> ref) {
		final Map<String, Object> map = new HashMap<>();
		for (final String key : ref.getPropertyKeys()) {
			map.put(key, ref.getProperty(key));
		}
		return map;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map<String, Object> getProperties() {
		final Map map = new HashMap(System.getenv());
		map.putAll(System.getProperties());
		for (final String key : keys) {
			final Object value = context.getProperty(key);
			if (value != null) {
				map.put(key, value);
			}
		}
		return map;
	}

	private List<BundleDTO> getBundles() {
		final Bundle[]             bundles = context.getBundles();
		final ArrayList<BundleDTO> list    = new ArrayList<>(bundles.length);
		for (final Bundle b : bundles) {
			list.add(toDTO(b));
		}
		return list;
	}

	private BundleDTO toDTO(final Bundle b) {
		final BundleDTO bd = new BundleDTO();
		bd.id           = b.getBundleId();
		bd.lastModified = b.getLastModified();
		bd.state        = b.getState();
		bd.symbolicName = b.getSymbolicName();
		bd.version      = b.getVersion() == null ? "0" : b.getVersion().toString();
		return bd;
	}

	void cleanup(final int event) throws Exception {
		if (quit) {
			return;
		}

		quit = true;
		update(null);
		redirect(0);
		link.close();
	}

	@Override
	public void close() throws IOException {
		try {
			cleanup(-2);
			startlevels.close();

			scrTracker.close();
			metatypeTracker.close();
			configAdminTracker.close();
			gogoCommandsTracker.close();
			agentExtensionTracker.close();
			httpServiceRuntimeTracker.close();
		} catch (final Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public void abort() throws Exception {
		cleanup(-3);
	}

	private void printStack(final Exception e1) {
		try {
			e1.printStackTrace(redirector.getOut());
		} catch (final Exception e) {
			//
		}
	}

	public void setRemote(final Supervisor supervisor) {
		remote = supervisor;
		source = new ShaSource() {

					@Override
					public boolean isFast() {
						return false;
					}

					@Override
					public InputStream get(final String sha) throws Exception {
						final byte[] data = remote.getFile(sha);
						if (data == null) {
							return null;
						}

						return new ByteArrayInputStream(data);
					}
				};

	}

	@Override
	public boolean isEnvoy() {
		return false;
	}

	@Override
	public Map<String, String> getSystemProperties() throws Exception {
		return Converter.cnv(MAP_STRING_STRING_T, System.getProperties());
	}

	@Override
	public boolean createFramework(final String name, final Collection<String> runpath, final Map<String, Object> properties)
	        throws Exception {
		throw new UnsupportedOperationException("This is an agent, we can't create new frameworks (for now)");
	}

	public Supervisor getSupervisor() {
		return remote;
	}

	public void setLink(final Link<Agent, Supervisor> link) {
		setRemote(link.getRemote());
		this.link = link;
	}

	@Override
	public boolean ping() {
		return true;
	}

	public BundleContext getContext() {
		return context;
	}

	public void refresh(final boolean async) throws InterruptedException {
		final FrameworkWiring f = context.getBundle(0).adapt(FrameworkWiring.class);
		if (f != null) {
			refresh = new CountDownLatch(1);
			f.refreshBundles(null, event -> refresh.countDown());

			if (async) {
				return;
			}
			refresh.await();
		}
	}

	@Override
	public List<BundleDTO> getBundles(final long... bundleId) throws Exception {

		Bundle[] bundles;
		if (bundleId.length == 0) {
			bundles = context.getBundles();
		} else {
			bundles = new Bundle[bundleId.length];
			for (int i = 0; i < bundleId.length; i++) {
				bundles[i] = context.getBundle(bundleId[i]);
				if (bundles[i] == null) {
					throw new IllegalArgumentException("Bundle " + bundleId[i] + " not installed");
				}
			}
		}

		final List<BundleDTO> bundleDTOs = new ArrayList<>(bundles.length);

		for (final Bundle b : bundles) {
			final BundleDTO dto = toDTO(b);
			bundleDTOs.add(dto);
		}

		return bundleDTOs;
	}

	/**
	 * Return the bundle revisions
	 */
	@Override
	public List<BundleRevisionDTO> getBundleRevisons(final long... bundleId) throws Exception {

		Bundle[] bundles;
		if (bundleId.length == 0) {
			bundles = context.getBundles();
		} else {
			bundles = new Bundle[bundleId.length];
			for (int i = 0; i < bundleId.length; i++) {
				bundles[i] = context.getBundle(bundleId[i]);
				if (bundles[i] == null) {
					throw new IllegalArgumentException("Bundle " + bundleId[i] + " does not exist");
				}
			}
		}

		final List<BundleRevisionDTO> revisions = new ArrayList<>(bundles.length);

		for (final Bundle b : bundles) {
			final BundleRevision    resource = b.adapt(BundleRevision.class);
			final BundleRevisionDTO bwd      = toDTO(resource);
			revisions.add(bwd);
		}

		return revisions;
	}

	/*
	 * Turn a bundle in a Bundle Revision dto. On a r6 framework we could do this
	 * with adapt but on earlier frameworks we're on our own
	 */

	private BundleRevisionDTO toDTO(final BundleRevision resource) {
		final BundleRevisionDTO brd = new BundleRevisionDTO();
		brd.bundle       = resource.getBundle().getBundleId();
		brd.id           = sequence.getAndIncrement();
		brd.symbolicName = resource.getSymbolicName();
		brd.type         = resource.getTypes();
		brd.version      = resource.getVersion().toString();

		brd.requirements = new ArrayList<>();

		for (final Requirement r : resource.getRequirements(null)) {
			brd.requirements.add(toDTO(brd.id, r));
		}

		brd.capabilities = new ArrayList<>();
		for (final Capability c : resource.getCapabilities(null)) {
			brd.capabilities.add(toDTO(brd.id, c));
		}

		return brd;
	}

	private RequirementDTO toDTO(final int resource, final Requirement r) {
		final RequirementDTO rd = new RequirementDTO();
		rd.id         = sequence.getAndIncrement();
		rd.resource   = resource;
		rd.namespace  = r.getNamespace();
		rd.directives = r.getDirectives();
		rd.attributes = r.getAttributes();
		return rd;
	}

	private CapabilityDTO toDTO(final int resource, final Capability r) {
		final CapabilityDTO rd = new CapabilityDTO();
		rd.id         = sequence.getAndIncrement();
		rd.resource   = resource;
		rd.namespace  = r.getNamespace();
		rd.directives = r.getDirectives();
		rd.attributes = r.getAttributes();
		return rd;
	}

	private Entry<String, Version> getIdentity(final byte[] data) throws IOException {
		try (JarInputStream jin = new JarInputStream(new ByteArrayInputStream(data))) {
			final Manifest manifest = jin.getManifest();
			if (manifest == null) {
				throw new IllegalArgumentException("No manifest in bundle");
			}
			final Attributes mainAttributes = manifest.getMainAttributes();
			final String     value          = mainAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
			final Matcher    matcher        = BSN_P.matcher(value);

			if (!matcher.matches()) {
				throw new IllegalArgumentException("No proper Bundle-SymbolicName in bundle: " + value);
			}

			final String bsn = matcher.group(1);

			String versionString = mainAttributes.getValue(Constants.BUNDLE_VERSION);
			if (versionString == null) {
				versionString = "0";
			}

			final Version version = Version.parseVersion(versionString);

			return new AbstractMap.SimpleEntry<>(bsn, version);
		}
	}

	private Set<Bundle> findBundles(final String bsn, final Version version) {

		return Stream.of(context.getBundles()).filter(b -> bsn.equals(b.getSymbolicName()))
		        .filter(b -> version == null || version.equals(b.getVersion())).collect(Collectors.toSet());
	}

	private String getLocation(final byte[] data) throws IOException {
		final Map.Entry<String, Version> entry   = getIdentity(data);
		final Set<Bundle>                bundles = findBundles(entry.getKey(), null);
		switch (bundles.size()) {
		case 0:
			return "manual:" + entry.getKey();

		case 1:
			return bundles.iterator().next().getLocation();

		default:
			throw new IllegalArgumentException(
			        "No location specified but there are multiple bundles with the same bsn " + entry.getKey() + ": " + bundles);
		}
	}

	@Override
	public List<XBundleDTO> getAllBundles() {
		return XBundleAdmin.get(context);
	}

	@Override
	public List<XComponentDTO> getAllComponents() {
		final boolean isScrAvailable = PackageWirings.isScrWired(context);
		if (isScrAvailable) {
			final XComponentAdmin scrAdmin = new XComponentAdmin(scrTracker.getService());
			return scrAdmin.getComponents();
		}
		return Collections.emptyList();
	}

	@Override
	public List<XConfigurationDTO> getAllConfigurations() {
		final boolean isConfigAdminAvailable = PackageWirings.isConfigAdminWired(context);
		final boolean isMetatypeAvailable    = PackageWirings.isMetatypeWired(context);

		final List<XConfigurationDTO> configs = new ArrayList<>();
		if (isConfigAdminAvailable) {
			final XConfigurationAdmin configAdmin = new XConfigurationAdmin(context, configAdminTracker.getService(),
			        metatypeTracker.getService());
			configs.addAll(configAdmin.getConfigurations());
		}
		if (isMetatypeAvailable) {
			final XMetaTypeAdmin metatypeAdmin = new XMetaTypeAdmin(context, configAdminTracker.getService(), metatypeTracker.getService());
			configs.addAll(metatypeAdmin.getConfigurations());
		}
		return configs;
	}

	@Override
	public List<XPropertyDTO> getAllProperties() {
		return XPropertyAdmin.get(context);
	}

	@Override
	public List<XServiceDTO> getAllServices() {
		return XServiceAdmin.get(context);
	}

	@Override
	public List<XThreadDTO> getAllThreads() {
		return XThreadAdmin.get();
	}

	@Override
	public XResultDTO enableComponentById(final long id) {
		final boolean isScrAvailable = PackageWirings.isScrWired(context);
		if (isScrAvailable) {
			final XComponentAdmin scrAdmin = new XComponentAdmin(scrTracker.getService());
			return scrAdmin.enableComponent(id);
		}
		return createResult(SKIPPED, "SCR bundle is not installed to process this request");
	}

	@Override
	public XResultDTO enableComponentByName(final String name) {
		requireNonNull(name, "Component name cannot be null");

		final boolean isScrAvailable = PackageWirings.isScrWired(context);
		if (isScrAvailable) {
			final XComponentAdmin scrAdmin = new XComponentAdmin(scrTracker.getService());
			return scrAdmin.enableComponent(name);
		}
		return createResult(SKIPPED, "SCR bundle is not installed to process this request");
	}

	@Override
	public XResultDTO disableComponentById(final long id) {
		final boolean isScrAvailable = PackageWirings.isScrWired(getContext());
		if (isScrAvailable) {
			final XComponentAdmin scrAdmin = new XComponentAdmin(scrTracker.getService());
			return scrAdmin.disableComponent(id);
		}
		return createResult(SKIPPED, "SCR bundle is not installed to process this request");
	}

	@Override
	public XResultDTO disableComponentByName(final String name) {
		requireNonNull(name, "Component name cannot be null");

		final boolean isScrAvailable = PackageWirings.isScrWired(getContext());
		if (isScrAvailable) {
			final XComponentAdmin scrAdmin = new XComponentAdmin(scrTracker.getService());
			return scrAdmin.disableComponent(name);
		}
		return createResult(SKIPPED, "SCR bundle is not installed to process this request");
	}

	@Override
	public XResultDTO createOrUpdateConfiguration(final String pid, final List<ConfigValue> newProperties) {
		requireNonNull(pid, "Configuration PID cannot be null");
		requireNonNull(newProperties, "Configuration properties cannot be null");

		final boolean isConfigAdminAvailable = PackageWirings.isConfigAdminWired(context);
		if (isConfigAdminAvailable) {
			final XConfigurationAdmin configAdmin = new XConfigurationAdmin(context, configAdminTracker.getService(),
			        metatypeTracker.getService());
			try {
				final Map<String, Object> finalProperties = parseProperties(newProperties);
				return configAdmin.createOrUpdateConfiguration(pid, finalProperties);
			} catch (final Exception e) {
				return createResult(ERROR, "One or configuration properties cannot be converted to the requested type");
			}
		}
		return createResult(SKIPPED, "ConfigAdmin bundle is not installed to process this request");
	}

	@Override
	public XResultDTO deleteConfiguration(final String pid) {
		requireNonNull(pid, "Configuration PID cannot be null");

		final boolean isConfigAdminAvailable = PackageWirings.isConfigAdminWired(getContext());
		if (isConfigAdminAvailable) {
			final XConfigurationAdmin configAdmin = new XConfigurationAdmin(context, configAdminTracker.getService(),
			        metatypeTracker.getService());
			return configAdmin.deleteConfiguration(pid);
		}
		return createResult(SKIPPED, "ConfigAdmin bundle is not installed to process this request");
	}

	@Override
	public XResultDTO createFactoryConfiguration(final String factoryPid, final List<ConfigValue> newProperties) {
		requireNonNull(factoryPid, "Configuration factory PID cannot be null");
		requireNonNull(newProperties, "Configuration properties cannot be null");

		final boolean isConfigAdminAvailable = PackageWirings.isConfigAdminWired(context);

		if (isConfigAdminAvailable) {
			final XConfigurationAdmin configAdmin = new XConfigurationAdmin(context, configAdminTracker.getService(),
			        metatypeTracker.getService());
			try {
				final Map<String, Object> finalProperties = parseProperties(newProperties);
				return configAdmin.createFactoryConfiguration(factoryPid, finalProperties);
			} catch (final Exception e) {
				return createResult(ERROR, "One or configuration properties cannot be converted to the requested type");
			}
		}
		return createResult(SKIPPED, "ConfigAdmin bundle is not installed to process this request");
	}

	@Override
	public void sendEvent(final String topic, final List<ConfigValue> properties) {
		requireNonNull(topic, "Event topic cannot be null");
		requireNonNull(properties, "Event properties cannot be null");

		final boolean isEventAdminAvailable = PackageWirings.isEventAdminWired(context);
		if (isEventAdminAvailable) {
			final XEventAdmin eventAdmin = new XEventAdmin(eventAdminTracker.getService());
			try {
				final Map<String, Object> finalProperties = parseProperties(properties);
				eventAdmin.sendEvent(topic, finalProperties);
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void postEvent(final String topic, final List<ConfigValue> properties) {
		requireNonNull(topic, "Event topic cannot be null");
		requireNonNull(properties, "Event properties cannot be null");

		final boolean isEventAdminAvailable = PackageWirings.isEventAdminWired(context);
		if (isEventAdminAvailable) {
			final XEventAdmin eventAdmin = new XEventAdmin(eventAdminTracker.getService());
			try {
				final Map<String, Object> finalProperties = parseProperties(properties);
				eventAdmin.postEvent(topic, finalProperties);
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public XMemoryInfoDTO getMemoryInfo() {
		final XMemoryInfoDTO dto = new XMemoryInfoDTO();

		dto.uptime      = getSystemUptime();
		dto.maxMemory   = Runtime.getRuntime().maxMemory();
		dto.freeMemory  = Runtime.getRuntime().freeMemory();
		dto.totalMemory = Runtime.getRuntime().totalMemory();

		return dto;
	}

	@Override
	public Set<String> getGogoCommands() {
		return new HashSet<>(gogoCommands);
	}

	@Override
	public Object executeExtension(final String name, final Map<String, Object> context) {
		if (!agentExtensions.containsKey(name)) {
			return "Agent extension with name '" + name + "' doesn't exist";
		}
		return agentExtensions.get(name).execute(context);
	}

	@Override
	public String exec(final String command) {
		try {
			final CommandLine                 cmdLine       = CommandLine.parse(command);
			final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

			final ExecuteWatchdog watchdog = new ExecuteWatchdog(30_000L);
			final Executor        executor = new DefaultExecutor();
			executor.setExitValue(1);
			executor.setWatchdog(watchdog);
			final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			final PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
			executor.setStreamHandler(streamHandler);
			executor.execute(cmdLine, resultHandler);

			resultHandler.waitFor();
			return outputStream.toString();
		} catch (IOException | InterruptedException e) {
			Thread.currentThread().interrupt();
			return Exceptions.toString(e);
		}
	}

	@Override
	public Set<XBundleDTO> getClassloaderLeaks() {
		return leakDetector.getSuspiciousBundles();
	}

	@Override
	public List<XHttpComponentDTO> getHttpComponents() {
		final boolean isHttpServiceRuntimeWired = PackageWirings.isHttpServiceRuntimeWired(context);
		if (isHttpServiceRuntimeWired) {
			final Object service = httpServiceRuntimeTracker.getService();
			if (service == null) {
				return null;
			}
			final XHttpAdmin httpAdmin = new XHttpAdmin(service);
			return httpAdmin.runtime();
		}
		return null;
	}

	@Override
	public XHeapUsageDTO getHeapUsage() {
		final boolean isJMXWired = PackageWirings.isJmxWired(context);
		return isJMXWired ? XHeapAdmin.init() : null;
	}

	@Override
	public void gc() {
		System.gc();
	}

	@Override
	public XHeapdumpDTO heapdump() throws Exception {
		final boolean isJMXWired = PackageWirings.isJmxWired(context);
		return isJMXWired ? XHeapAdmin.heapdump() : null;
	}

	@Override
	public boolean isConfigAdminAvailable() {
		return PackageWirings.isConfigAdminWired(context);
	}

	@Override
	public boolean isEventAdminAvailable() {
		return PackageWirings.isEventAdminWired(context);
	}

	private long getSystemUptime() {
		final boolean isJMXWired = PackageWirings.isJmxWired(context);
		return isJMXWired ? ManagementFactory.getRuntimeMXBean().getUptime() : 0;
	}

	public static <K, V> Map<K, V> valueOf(final Dictionary<K, V> dictionary) {
		if (dictionary == null) {
			return null;
		}
		final Map<K, V>      map  = new HashMap<>(dictionary.size());
		final Enumeration<K> keys = dictionary.keys();
		while (keys.hasMoreElements()) {
			final K key = keys.nextElement();
			map.put(key, dictionary.get(key));
		}
		return map;
	}

	public static XResultDTO createResult(final int result, final String response) {
		final XResultDTO dto = new XResultDTO();

		dto.result   = result;
		dto.response = response;

		return dto;
	}

	private Map<String, Object> parseProperties(final List<ConfigValue> newProperties) throws Exception {
		final Map<String, Object> properties = new HashMap<>();
		for (final ConfigValue entry : newProperties) {
			final Object convertedValue = convert(entry);
			properties.put(entry.key, convertedValue);
		}
		return properties;
	}

	public static Object convert(final ConfigValue entry) throws Exception {
		final Object            source = entry.value;
		final XAttributeDefType type   = entry.type;
		switch (type) {
		case STRING_ARRAY:
			return Converter.cnv(String[].class, source);
		case STRING_LIST:
			return Converter.cnv(new TypeReference<List<String>>() {
			}, source);
		case INTEGER_ARRAY:
			return Converter.cnv(int[].class, source);
		case INTEGER_LIST:
			return Converter.cnv(new TypeReference<List<Integer>>() {
			}, source);
		case BOOLEAN_ARRAY:
			return Converter.cnv(boolean[].class, source);
		case BOOLEAN_LIST:
			return Converter.cnv(new TypeReference<List<Boolean>>() {
			}, source);
		case DOUBLE_ARRAY:
			return Converter.cnv(double[].class, source);
		case DOUBLE_LIST:
			return Converter.cnv(new TypeReference<List<Double>>() {
			}, source);
		case FLOAT_ARRAY:
			return Converter.cnv(float[].class, source);
		case FLOAT_LIST:
			return Converter.cnv(new TypeReference<List<Float>>() {
			}, source);
		case CHAR_ARRAY:
			return Converter.cnv(char[].class, source);
		case CHAR_LIST:
			return Converter.cnv(new TypeReference<List<Character>>() {
			}, source);
		case LONG_ARRAY:
			return Converter.cnv(long[].class, source);
		case LONG_LIST:
			return Converter.cnv(new TypeReference<List<Long>>() {
			}, source);
		default:
			return Converter.cnv(XAttributeDefType.clazz(type), source);
		}
	}

}
