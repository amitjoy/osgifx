/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
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

import static com.osgifx.console.agent.dto.XResultDTO.ERROR;
import static com.osgifx.console.agent.dto.XResultDTO.SKIPPED;
import static com.osgifx.console.agent.dto.XResultDTO.SUCCESS;
import static com.osgifx.console.agent.helper.AgentHelper.createResult;
import static com.osgifx.console.agent.helper.AgentHelper.packageNotWired;
import static com.osgifx.console.agent.provider.AgentServer.RpcType.SOCKET_RPC;
import static com.osgifx.console.agent.provider.PackageWirings.Type.CM;
import static com.osgifx.console.agent.provider.PackageWirings.Type.DMT;
import static com.osgifx.console.agent.provider.PackageWirings.Type.EVENT_ADMIN;
import static com.osgifx.console.agent.provider.PackageWirings.Type.HC;
import static com.osgifx.console.agent.provider.PackageWirings.Type.HTTP;
import static com.osgifx.console.agent.provider.PackageWirings.Type.JMX;
import static com.osgifx.console.agent.provider.PackageWirings.Type.LOG;
import static com.osgifx.console.agent.provider.PackageWirings.Type.R7_LOGGER;
import static com.osgifx.console.agent.provider.PackageWirings.Type.SCR;
import static com.osgifx.console.agent.provider.PackageWirings.Type.USER_ADMIN;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
import static org.osgi.framework.Constants.BUNDLE_VERSION;
import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.dto.CapabilityDTO;
import org.osgi.resource.dto.RequirementDTO;
import org.osgi.util.tracker.ServiceTracker;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.admin.XBundleAdmin;
import com.osgifx.console.agent.admin.XComponentAdmin;
import com.osgifx.console.agent.admin.XConfigurationAdmin;
import com.osgifx.console.agent.admin.XDmtAdmin;
import com.osgifx.console.agent.admin.XDtoAdmin;
import com.osgifx.console.agent.admin.XEventAdmin;
import com.osgifx.console.agent.admin.XHcAdmin;
import com.osgifx.console.agent.admin.XHttpAdmin;
import com.osgifx.console.agent.admin.XJmxAdmin;
import com.osgifx.console.agent.admin.XLogReaderAdmin;
import com.osgifx.console.agent.admin.XLoggerAdmin;
import com.osgifx.console.agent.admin.XMetaTypeAdmin;
import com.osgifx.console.agent.admin.XPropertyAdmin;
import com.osgifx.console.agent.admin.XServiceAdmin;
import com.osgifx.console.agent.admin.XThreadAdmin;
import com.osgifx.console.agent.admin.XUserAdmin;
import com.osgifx.console.agent.di.DI;
import com.osgifx.console.agent.dto.ConfigValue;
import com.osgifx.console.agent.dto.DmtDataType;
import com.osgifx.console.agent.dto.RuntimeDTO;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XBundleLoggerContextDTO;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XDmtNodeDTO;
import com.osgifx.console.agent.dto.XHealthCheckDTO;
import com.osgifx.console.agent.dto.XHealthCheckResultDTO;
import com.osgifx.console.agent.dto.XHeapUsageDTO;
import com.osgifx.console.agent.dto.XHttpComponentDTO;
import com.osgifx.console.agent.dto.XMemoryInfoDTO;
import com.osgifx.console.agent.dto.XPropertyDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.agent.dto.XRoleDTO;
import com.osgifx.console.agent.dto.XRoleDTO.Type;
import com.osgifx.console.agent.dto.XServiceDTO;
import com.osgifx.console.agent.dto.XThreadDTO;
import com.osgifx.console.agent.extension.AgentExtension;
import com.osgifx.console.agent.handler.OSGiEventHandler;
import com.osgifx.console.agent.handler.OSGiLogListener;
import com.osgifx.console.agent.helper.AgentHelper;
import com.osgifx.console.agent.redirector.ConsoleRedirector;
import com.osgifx.console.agent.redirector.GogoRedirector;
import com.osgifx.console.agent.redirector.NullRedirector;
import com.osgifx.console.agent.redirector.RedirectOutput;
import com.osgifx.console.agent.redirector.Redirector;
import com.osgifx.console.agent.redirector.SocketRedirector;
import com.osgifx.console.agent.rpc.RemoteRPC;
import com.osgifx.console.supervisor.Supervisor;

import aQute.bnd.exceptions.Exceptions;
import aQute.lib.converter.Converter;
import aQute.lib.converter.TypeReference;

public final class AgentServer implements Agent, Closeable {

    public enum RpcType {
        MQTT_RPC,
        SOCKET_RPC
    }

    private static final Duration      RESULT_TIMEOUT                        = Duration.ofSeconds(20);
    private static final Duration      WATCHDOG_TIMEOUT                      = Duration.ofSeconds(30);
    private static final AtomicInteger sequence                              = new AtomicInteger(1000);
    private static final Pattern       BSN_PATTERN                           = Pattern.compile("\\s*([^;\\s]+).*");
    public static final String         PROPERTY_ENABLE_LOGGING               = "osgi.fx.enable.logging";
    public static final String         PROPERTY_ENABLE_EVENTING              = "osgi.fx.enable.eventing";
    private static final String        COMMAND_EXECUTOR_DEFAULT_THREAD_NAME  = "exec-default";
    private static final String        COMMAND_EXECUTOR_WATCHDOG_THREAD_NAME = "exec-watchdog";

    public volatile boolean              quit;
    private final RpcType                rpcType;
    private Supervisor                   remote;
    private RemoteRPC<Agent, Supervisor> remoteRPC;
    private final Map<String, String>    installed  = new HashMap<>();
    private Redirector                   redirector = new NullRedirector();
    private final BinaryLogBuffer        logBuffer  = new BinaryLogBuffer();

    private ServiceTracker<Object, Object> logReaderTracker;

    private Closeable              osgiLogListenerCloser;
    private OSGiLogListener        logListener;
    private ServiceRegistration<?> osgiEventListenerServiceReg;

    private final DI           di;
    private final FluentLogger logger = LoggerFactory.getFluentLogger(getClass());

    public AgentServer(final DI di, final RpcType rpcType) {
        this.di      = di;
        this.rpcType = rpcType;

        // Check for Auto-Start of Log Capture
        if (Boolean.getBoolean(AGENT_AUTO_START_LOG_CAPTURE_KEY)) {
            // Restore persistence
            try {
                if (getContext() != null) { // Might be null if DI not ready?
                    // Actually BundleContext is needed for dataFile.
                    // Assuming DI has it if Activator started us.
                    logBuffer.fromDisk(getContext().getDataFile("osgifx_logs.bin"));
                }
            } catch (Exception e) {
                // Ignore
            }
            // Register listener immediately
            osgiLogListenerCloser = initOSGiLogging();
        }
    }

    public BundleContext getContext() {
        return di.getInstance(BundleContext.class);
    }

    @Override
    public BundleDTO installWithData(final String location, final byte[] data, final int startLevel) throws Exception {
        return installBundleWithData(location, data, startLevel, true);
    }

    @Override
    public XResultDTO installWithMultipleData(final Collection<byte[]> data, final int startLevel) {
        requireNonNull(data, "Data cannot be null");

        final XResultDTO    result = new XResultDTO();
        final StringBuilder b      = new StringBuilder();
        try {
            for (final byte[] d : data) {
                try {
                    installBundleWithData(null, d, startLevel, false);
                } catch (final Exception e) {
                    b.append(e.getMessage()).append(System.lineSeparator());
                }
            }
        } catch (final Exception e) {
            result.result = XResultDTO.ERROR;
        } finally {
            result.response = b.toString();
            // if there are no errors at all, perform the refresh operation
            if (result.response.isEmpty()) {
                try {
                    // this sometimes causes https://issues.apache.org/jira/browse/FELIX-3414
                    refresh(true);
                } catch (final Exception e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        result.result = XResultDTO.SUCCESS;
        return result;
    }

    @Override
    public BundleDTO installFromURL(final String location, final String url) throws Exception {
        requireNonNull(location, "Bundle location cannot be null");
        requireNonNull(url, "Bundle URL cannot be null");

        final InputStream is = new URL(url).openStream();
        final Bundle      b  = di.getInstance(BundleContext.class).installBundle(location, is);
        installed.put(b.getLocation(), url);
        return toDTO(b);
    }

    @Override
    public String start(final long... ids) {
        requireNonNull(ids, "Bundle IDs cannot be null");

        final StringBuilder sb = new StringBuilder();
        for (final long id : ids) {
            final Bundle bundle = di.getInstance(BundleContext.class).getBundle(id);
            try {
                bundle.start();
            } catch (final BundleException e) {
                sb.append(e.getMessage()).append("\n");
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    @Override
    public String stop(final long... ids) {
        requireNonNull(ids, "Bundle IDs cannot be null");

        final StringBuilder sb = new StringBuilder();
        for (final long id : ids) {
            final Bundle bundle = di.getInstance(BundleContext.class).getBundle(id);
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
        requireNonNull(ids, "Bundle IDs cannot be null");

        final StringBuilder sb = new StringBuilder();
        for (final long id : ids) {
            final Bundle bundle = di.getInstance(BundleContext.class).getBundle(id);
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
    public List<BundleRevisionDTO> getBundleRevisons(final long... ids) throws Exception {
        requireNonNull(ids, "Bundle IDs cannot be null");

        Bundle[] bundles;
        if (ids.length == 0) {
            bundles = di.getInstance(BundleContext.class).getBundles();
        } else {
            bundles = new Bundle[ids.length];
            for (int i = 0; i < ids.length; i++) {
                bundles[i] = di.getInstance(BundleContext.class).getBundle(ids[i]);
                if (bundles[i] == null) {
                    throw new IllegalArgumentException("Bundle " + ids[i] + " does not exist");
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

    @Override
    public boolean redirect(final int port) throws Exception {
        if (redirector != null) {
            if (redirector.getPort() == port) {
                return false;
            }
            redirector.close();
            redirector = new NullRedirector();
        }
        if (port == NONE) {
            return true;
        }
        if (port <= COMMAND_SESSION) {
            try {
                redirector = new GogoRedirector(this, di.getInstance(BundleContext.class));
            } catch (final Exception e) {
                throw new IllegalStateException("Gogo is not present in this framework", e);
            }
            return true;
        }
        if (port == CONSOLE) {
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
    public String execGogoCommand(final String command) throws Exception {
        requireNonNull(command, "Gogo command cannot be null");

        redirect(COMMAND_SESSION);
        stdin(command);
        final PrintStream ps = redirector.getOut();
        if (ps instanceof RedirectOutput) {
            final RedirectOutput rout = (RedirectOutput) ps;
            return rout.getLastOutput();
        }
        return null;
    }

    @Override
    public boolean isReceivingLogEnabled() {
        return Boolean.getBoolean(PROPERTY_ENABLE_LOGGING);
    }

    @Override
    public void enableReceivingLog() {
        System.setProperty(PROPERTY_ENABLE_LOGGING, String.valueOf(true));
    }

    @Override
    public void disableReceivingLog() {
        System.setProperty(PROPERTY_ENABLE_LOGGING, String.valueOf(false));
    }

    @Override
    public boolean isReceivingEventEnabled() {
        return Boolean.getBoolean(PROPERTY_ENABLE_EVENTING);
    }

    @Override
    public void enableReceivingEvent() {
        System.setProperty(PROPERTY_ENABLE_EVENTING, String.valueOf(true));
    }

    @Override
    public void disableReceivingEvent() {
        System.setProperty(PROPERTY_ENABLE_EVENTING, String.valueOf(false));
    }

    @Override
    public String execCliCommand(final String command) {
        requireNonNull(command, "CLI command cannot be null");

        final List<String> commandEntries = new ArrayList<>();
        if (OS.isFamilyWindows()) {
            commandEntries.add("cmd.exe");
            commandEntries.add("/C");
        }
        parseCommand(command, commandEntries);
        try {
            final CommandLine cmdLine = CommandLine.parse(commandEntries.get(0));
            for (int i = 1; i < commandEntries.size(); i++) {
                cmdLine.addArgument(commandEntries.get(i));
            }
            final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
            final ExecuteWatchdog             watchdog      = ExecuteWatchdog.builder().setTimeout(WATCHDOG_TIMEOUT)
                    .setThreadFactory(r -> new Thread(r, COMMAND_EXECUTOR_WATCHDOG_THREAD_NAME)).get();
            final Executor                    executor      = DefaultExecutor.builder()
                    .setThreadFactory(r -> new Thread(r, COMMAND_EXECUTOR_DEFAULT_THREAD_NAME)).get();
            final ByteArrayOutputStream       outputStream  = new ByteArrayOutputStream();
            final PumpStreamHandler           streamHandler = new PumpStreamHandler(outputStream);
            executor.setWatchdog(watchdog);
            executor.setStreamHandler(streamHandler);
            executor.execute(cmdLine, resultHandler);
            resultHandler.waitFor(RESULT_TIMEOUT);

            return outputStream.toString();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return Exceptions.toString(e);
        } catch (final Exception e) {
            return Exceptions.toString(e);
        }
    }

    private List<String> parseCommand(String command, final List<String> commandEntries) {
        command = command.trim();
        if (command.isEmpty()) {
            return Collections.emptyList();
        }
        final String[] entry = command.split(" ");
        // @formatter:off
        return Stream.of(entry)
                     .filter(e -> !e.isEmpty())
                     .map(String::trim)
                     .collect(toCollection(() -> commandEntries));
        // @formatter:on
    }

    public void setSupervisor(final Supervisor remote) {
        setRemote(remote);
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

    void cleanup() throws Exception {
        if (quit) {
            return;
        }
        if (rpcType == SOCKET_RPC) {
            quit = true;
            redirect(0);
            remoteRPC.close();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            cleanup();
            // Snapshot logs to disk on close
            if (getContext() != null) {
                logBuffer.toDisk(getContext().getDataFile("osgifx_logs.bin"));
            }

            if (logReaderTracker != null) {
                logReaderTracker.close();
            }
            if (osgiEventListenerServiceReg != null) {
                osgiEventListenerServiceReg.unregister();
            }
            if (osgiLogListenerCloser != null) {
                osgiLogListenerCloser.close();
            }
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public void disconnect() throws Exception {
        cleanup();
    }

    public void setRemote(final Supervisor supervisor) {
        remote = supervisor;
    }

    public Supervisor getSupervisor() {
        return remote;
    }

    public void setEndpoint(final RemoteRPC<Agent, Supervisor> remoteRPC) {
        setRemote(remoteRPC.getRemote());
        this.remoteRPC = remoteRPC;

        // the following can only be initialized if and only if the RPC link is
        // established

        // If not already started by auto-start
        if (osgiLogListenerCloser == null) {
            // Try to restore logs from previous run (if not done in constructor)
            try {
                logBuffer.fromDisk(getContext().getDataFile("osgifx_logs.bin"));
            } catch (Exception e) {
                // Ignore if file doesn't exist or corrupted
            }
            osgiLogListenerCloser = initOSGiLogging();
        }
        // Update supervisor reference
        if (logListener != null) {
            logListener.setSupervisor(remoteRPC.getRemote());
        }

        osgiEventListenerServiceReg = initOSGiEventing();
    }

    @Override
    public boolean ping() {
        return true;
    }

    public void refresh(final boolean async) throws InterruptedException {
        final FrameworkWiring wiring = di.getInstance(BundleContext.class).getBundle(SYSTEM_BUNDLE_ID)
                .adapt(FrameworkWiring.class);
        if (wiring != null) {
            final CountDownLatch refresh = new CountDownLatch(1);
            wiring.refreshBundles(null, event -> refresh.countDown());
            if (async) {
                return;
            }
            refresh.await();
        }
    }

    private Entry<String, Version> getIdentity(final byte[] data) throws IOException {
        try (JarInputStream jin = new JarInputStream(new ByteArrayInputStream(data))) {
            final Manifest manifest = jin.getManifest();
            if (manifest == null) {
                throw new IllegalArgumentException("No manifest in bundle");
            }
            final Attributes mainAttributes = manifest.getMainAttributes();
            final String     value          = mainAttributes.getValue(BUNDLE_SYMBOLICNAME);
            final Matcher    matcher        = BSN_PATTERN.matcher(value);

            if (!matcher.matches()) {
                throw new IllegalArgumentException("No proper Bundle-SymbolicName in bundle: " + value);
            }
            final String bsn           = matcher.group(1);
            String       versionString = mainAttributes.getValue(BUNDLE_VERSION);
            if (versionString == null) {
                versionString = "0";
            }
            final Version version = Version.parseVersion(versionString);
            return new AbstractMap.SimpleEntry<>(bsn, version);
        }
    }

    private Set<Bundle> findBundles(final String bsn, final Version version) {
        // @formatter:off
        return Stream.of(di.getInstance(BundleContext.class)
                     .getBundles())
                     .filter(b -> bsn.equals(b.getSymbolicName()))
                     .filter(b -> version == null || version.equals(b.getVersion()))
                     .collect(toSet());
        // @formatter:on
    }

    private String getLocation(final byte[] data) throws IOException {
        final Map.Entry<String, Version> entry   = getIdentity(data);
        final Set<Bundle>                bundles = findBundles(entry.getKey(), null);
        switch (bundles.size()) {
            case 0:
                return BUNDLE_LOCATION_PREFIX + entry.getKey();
            case 1:
                return bundles.iterator().next().getLocation();
            default:
                throw new IllegalArgumentException("No location specified but there are multiple bundles with the same bsn "
                        + entry.getKey() + ": " + bundles);
        }
    }

    @Override
    public List<XBundleDTO> getAllBundles() {
        return di.getInstance(XBundleAdmin.class).get();
    }

    @Override
    public List<XComponentDTO> getAllComponents() {
        final boolean isScrAvailable = di.getInstance(PackageWirings.class).isScrWired();
        if (isScrAvailable) {
            return di.getInstance(XComponentAdmin.class).getComponents();
        }
        return Collections.emptyList();
    }

    @Override
    public List<XConfigurationDTO> getAllConfigurations() {
        final boolean isConfigAdminAvailable = di.getInstance(PackageWirings.class).isConfigAdminWired();
        final boolean isMetatypeAvailable    = di.getInstance(PackageWirings.class).isMetatypeWired();
        final boolean isScrAvailable         = di.getInstance(PackageWirings.class).isScrWired();

        final Map<String, XConfigurationDTO> configMap = new java.util.HashMap<>();
        if (isConfigAdminAvailable) {
            di.getInstance(XConfigurationAdmin.class).getConfigurations().forEach(c -> configMap.put(c.pid, c));
        }
        if (isMetatypeAvailable) {
            di.getInstance(XMetaTypeAdmin.class).getConfigurations().forEach(c -> configMap.put(c.pid, c));
        }
        final List<XConfigurationDTO> configs = new ArrayList<>(configMap.values());

        if (isScrAvailable) {
            configs.forEach(c -> di.getInstance(XConfigurationAdmin.class).setComponentReferenceFilters(c));
        }
        return configs;
    }

    @Override
    public List<XPropertyDTO> getAllProperties() {
        return di.getInstance(XPropertyAdmin.class).get();
    }

    @Override
    public List<XServiceDTO> getAllServices() {
        return di.getInstance(XServiceAdmin.class).get();
    }

    @Override
    public List<XThreadDTO> getAllThreads() {
        return di.getInstance(XThreadAdmin.class).get();
    }

    @Override
    public XDmtNodeDTO readDmtNode(final String rootURI) {
        requireNonNull(rootURI, "DMT node root URI cannot be null");

        final boolean isDmtAdminAvailable = di.getInstance(PackageWirings.class).isDmtAdminWired();
        if (isDmtAdminAvailable) {
            return di.getInstance(XDmtAdmin.class).readDmtNode(rootURI);
        }
        logger.atWarn().msg(packageNotWired(DMT)).log();
        return null;
    }

    @Override
    public XResultDTO updateDmtNode(final String uri, final Object value, final DmtDataType format) {
        requireNonNull(uri, "DMT node URI cannot be null");
        requireNonNull(value, "DMT value cannot be null");
        requireNonNull(format, "DMT value type cannot be null");

        final boolean isDmtAdminAvailable = di.getInstance(PackageWirings.class).isDmtAdminWired();
        if (isDmtAdminAvailable) {
            return di.getInstance(XDmtAdmin.class).updateDmtNode(uri, value, format);
        }
        logger.atWarn().msg(packageNotWired(DMT)).log();
        return createResult(SKIPPED, packageNotWired(DMT));
    }

    @Override
    public XResultDTO updateBundleLoggerContext(final String bsn, final Map<String, String> logLevels) {
        final boolean isR7LogAvailable = di.getInstance(PackageWirings.class).isR7LoggerAdminWired();
        if (isR7LogAvailable) {
            return di.getInstance(XLoggerAdmin.class).updateLoggerContext(bsn, logLevels);
        }
        logger.atWarn().msg(packageNotWired(R7_LOGGER)).log();
        return createResult(SKIPPED, packageNotWired(R7_LOGGER));
    }

    @Override
    public XResultDTO enableComponentById(final long id) {
        final boolean isScrAvailable = di.getInstance(PackageWirings.class).isScrWired();
        if (isScrAvailable) {
            return di.getInstance(XComponentAdmin.class).enableComponent(id);
        }
        logger.atWarn().msg(packageNotWired(SCR)).log();
        return createResult(SKIPPED, packageNotWired(SCR));
    }

    @Override
    public XResultDTO enableComponentByName(final String name) {
        requireNonNull(name, "Component name cannot be null");

        final boolean isScrAvailable = di.getInstance(PackageWirings.class).isScrWired();
        if (isScrAvailable) {
            return di.getInstance(XComponentAdmin.class).enableComponent(name);
        }
        logger.atWarn().msg(packageNotWired(SCR)).log();
        return createResult(SKIPPED, packageNotWired(SCR));
    }

    @Override
    public XResultDTO disableComponentById(final long id) {
        final boolean isScrAvailable = di.getInstance(PackageWirings.class).isScrWired();
        if (isScrAvailable) {
            return di.getInstance(XComponentAdmin.class).disableComponent(id);
        }
        logger.atWarn().msg(packageNotWired(SCR)).log();
        return createResult(SKIPPED, packageNotWired(SCR));
    }

    @Override
    public XResultDTO disableComponentByName(final String name) {
        requireNonNull(name, "Component name cannot be null");

        final boolean isScrAvailable = di.getInstance(PackageWirings.class).isScrWired();
        if (isScrAvailable) {
            return di.getInstance(XComponentAdmin.class).disableComponent(name);
        }
        logger.atWarn().msg(packageNotWired(SCR)).log();
        return createResult(SKIPPED, packageNotWired(SCR));
    }

    @Override
    public Collection<String> findBundleEntries(final long bundleId,
                                                final String path,
                                                final String pattern,
                                                final boolean recursive) {
        requireNonNull(path, "Path cannot be null");
        requireNonNull(pattern, "Pattern cannot be null");
        return di.getInstance(XBundleAdmin.class).findEntries(bundleId, path, pattern, recursive);
    }

    @Override
    public Collection<String> listBundleResources(final long bundleId,
                                                  final String path,
                                                  final String pattern,
                                                  final int options) {
        requireNonNull(path, "Path cannot be null");
        requireNonNull(pattern, "Pattern cannot be null");
        return di.getInstance(XBundleAdmin.class).listResources(bundleId, path, pattern, options);
    }

    @Override
    public XResultDTO createOrUpdateConfiguration(final String pid, final List<ConfigValue> newProperties) {
        requireNonNull(newProperties, "Configuration properties cannot be null");

        try {
            final Map<String, Object> finalProperties = parseProperties(newProperties);
            return createOrUpdateConfig(pid, finalProperties);
        } catch (final Exception e) {
            logger.atError().msg("Configurations cannot be converted").throwable(e).log();
            return createResult(ERROR,
                    "One or more configuration properties cannot be converted to the requested type");
        }
    }

    @Override
    public Map<String, XResultDTO> createOrUpdateConfigurations(final Map<String, Map<String, Object>> configurations) {
        final Map<String, XResultDTO> results = new HashMap<>();
        configurations.forEach((k, v) -> results.put(k, createOrUpdateConfig(k, v)));
        return results;
    }

    @Override
    public XResultDTO deleteConfiguration(final String pid) {
        requireNonNull(pid, "Configuration PID cannot be null");

        final boolean isConfigAdminAvailable = di.getInstance(PackageWirings.class).isConfigAdminWired();
        if (isConfigAdminAvailable) {
            return di.getInstance(XConfigurationAdmin.class).deleteConfiguration(pid);
        }
        logger.atWarn().msg(packageNotWired(CM)).log();
        return createResult(SKIPPED, packageNotWired(CM));
    }

    @Override
    public XResultDTO createFactoryConfiguration(final String factoryPid, final List<ConfigValue> newProperties) {
        requireNonNull(factoryPid, "Configuration factory PID cannot be null");
        requireNonNull(newProperties, "Configuration properties cannot be null");

        final boolean isConfigAdminAvailable = di.getInstance(PackageWirings.class).isConfigAdminWired();

        if (isConfigAdminAvailable) {
            try {
                final Map<String, Object> finalProperties = parseProperties(newProperties);
                return di.getInstance(XConfigurationAdmin.class).createFactoryConfiguration(factoryPid,
                        finalProperties);
            } catch (final Exception e) {
                return createResult(ERROR, "One or configuration properties cannot be converted to the requested type");
            }
        }
        logger.atWarn().msg(packageNotWired(CM)).log();
        return createResult(SKIPPED, packageNotWired(CM));
    }

    @Override
    public XResultDTO sendEvent(final String topic, final List<ConfigValue> properties) {
        requireNonNull(topic, "Event topic cannot be null");
        requireNonNull(properties, "Event properties cannot be null");

        final boolean isEventAdminAvailable = di.getInstance(PackageWirings.class).isEventAdminWired();
        if (isEventAdminAvailable) {
            try {
                final Map<String, Object> finalProperties = parseProperties(properties);
                di.getInstance(XEventAdmin.class).sendEvent(topic, finalProperties);
                return createResult(SUCCESS, "Event has been sent successfully");
            } catch (final Exception e) {
                return createResult(ERROR, "Event could not be sent successfully");
            }
        }
        logger.atWarn().msg(packageNotWired(EVENT_ADMIN)).log();
        return createResult(SKIPPED, packageNotWired(EVENT_ADMIN));
    }

    @Override
    public XResultDTO postEvent(final String topic, final List<ConfigValue> properties) {
        requireNonNull(topic, "Event topic cannot be null");
        requireNonNull(properties, "Event properties cannot be null");

        final boolean isEventAdminAvailable = di.getInstance(PackageWirings.class).isEventAdminWired();
        if (isEventAdminAvailable) {
            try {
                final Map<String, Object> finalProperties = parseProperties(properties);
                di.getInstance(XEventAdmin.class).postEvent(topic, finalProperties);
                return createResult(SUCCESS, "Event has been sent successfully");
            } catch (final Exception e) {
                return createResult(ERROR, "Event could not be sent successfully");
            }
        }
        logger.atWarn().msg(packageNotWired(EVENT_ADMIN)).log();
        return createResult(SKIPPED, packageNotWired(EVENT_ADMIN));
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
    @SuppressWarnings("unchecked")
    public Set<String> getGogoCommands() {
        final Set<String> gogoCommands = di.getInstance(Set.class);
        return new HashSet<>(gogoCommands);
    }

    @Override
    public XResultDTO createRole(final String name, final Type type) {
        requireNonNull(name, "Role name cannot be null");
        requireNonNull(type, "Role type name cannot be null");

        final boolean isUserAdminAvailable = di.getInstance(PackageWirings.class).isUserAdminWired();
        if (isUserAdminAvailable) {
            try {
                return di.getInstance(XUserAdmin.class).createRole(name, type);
            } catch (final Exception e) {
                return createResult(ERROR, "The role cannot be created");
            }
        }
        logger.atWarn().msg(packageNotWired(USER_ADMIN)).log();
        return createResult(SKIPPED, packageNotWired(USER_ADMIN));
    }

    @Override
    public XResultDTO updateRole(final XRoleDTO dto) {
        requireNonNull(dto, "New role information cannot be null");

        final boolean isUserAdminAvailable = di.getInstance(PackageWirings.class).isUserAdminWired();
        if (isUserAdminAvailable) {
            try {
                return di.getInstance(XUserAdmin.class).updateRole(dto);
            } catch (final Exception e) {
                return createResult(ERROR, "The role cannot be updated");
            }
        }
        logger.atWarn().msg(packageNotWired(USER_ADMIN)).log();
        return createResult(SKIPPED, packageNotWired(USER_ADMIN));
    }

    @Override
    public XResultDTO removeRole(final String name) {
        requireNonNull(name, "Role name cannot be null");

        final boolean isUserAdminAvailable = di.getInstance(PackageWirings.class).isUserAdminWired();
        if (isUserAdminAvailable) {
            try {
                return di.getInstance(XUserAdmin.class).removeRole(name);
            } catch (final Exception e) {
                return createResult(ERROR, "The role cannot be removed");
            }
        }
        logger.atWarn().msg(packageNotWired(USER_ADMIN)).log();
        return createResult(SKIPPED, packageNotWired(USER_ADMIN));
    }

    @Override
    public List<XRoleDTO> getAllRoles() {
        final boolean isUserAdminAvailable = di.getInstance(PackageWirings.class).isUserAdminWired();
        if (isUserAdminAvailable) {
            return di.getInstance(XUserAdmin.class).getRoles();
        }
        logger.atWarn().msg(packageNotWired(USER_ADMIN)).log();
        return Collections.emptyList();
    }

    @Override
    public List<XBundleLoggerContextDTO> getBundleLoggerContexts() {
        final boolean isR7LogAvailable = di.getInstance(PackageWirings.class).isR7LoggerAdminWired();
        if (isR7LogAvailable) {
            return di.getInstance(XLoggerAdmin.class).getLoggerContexts();
        }
        logger.atWarn().msg(packageNotWired(R7_LOGGER)).log();
        return Collections.emptyList();
    }

    @Override
    public List<XHealthCheckDTO> getAllHealthChecks() {
        final boolean isFelixHcAvailable = di.getInstance(PackageWirings.class).isFelixHcWired();
        if (isFelixHcAvailable) {
            return di.getInstance(XHcAdmin.class).getHealthchecks();
        }
        logger.atWarn().msg(packageNotWired(HC)).log();
        return Collections.emptyList();
    }

    @Override
    public List<XHealthCheckResultDTO> executeHealthChecks(final List<String> tags, final List<String> names) {
        final boolean isFelixHcAvailable = di.getInstance(PackageWirings.class).isFelixHcWired();
        if (isFelixHcAvailable) {
            return di.getInstance(XHcAdmin.class).executeHealthChecks(tags, names);
        }
        logger.atWarn().msg(packageNotWired(HC)).log();
        return Collections.emptyList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> executeExtension(final String name, final Map<String, Object> context) {
        requireNonNull(name, "Agent extension name cannot be null");
        requireNonNull(context, "Agent extension execution context cannot be null");

        final Map<String, AgentExtension<DTO, DTO>> agentExtensions = di.getInstance(Map.class);

        if (!agentExtensions.containsKey(name)) {
            final String msg = "Agent extension with name '" + name + "' doesn't exist";
            logger.atError().msg(msg).log();
            throw new RuntimeException(msg);
        }
        try {
            final AgentExtension<DTO, DTO> extension = agentExtensions.get(name);
            final DTO                      cnv       = Converter.cnv(extension.getContextType(), context);
            final DTO                      result    = extension.execute(cnv);
            return Converter.cnv(new TypeReference<Map<String, Object>>() {
            }, result);
        } catch (final Exception e) {
            logger.atError().msg("Error occurred while executing extension - '%s'").arg(name).throwable(e).log();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<XBundleDTO> getClassloaderLeaks() {
        return di.getInstance(ClassloaderLeakDetector.class).getSuspiciousBundles();
    }

    @Override
    public List<XHttpComponentDTO> getHttpComponents() {
        final boolean isHttpServiceRuntimeWired = di.getInstance(PackageWirings.class).isHttpServiceRuntimeWired();
        if (isHttpServiceRuntimeWired) {
            return di.getInstance(XHttpAdmin.class).runtime();
        }
        logger.atWarn().msg(packageNotWired(HTTP)).log();
        return Collections.emptyList();
    }

    @Override
    public XHeapUsageDTO getHeapUsage() {
        final boolean isJMXWired = di.getInstance(PackageWirings.class).isJmxWired();
        if (isJMXWired) {
            return di.getInstance(XJmxAdmin.class).init();
        }
        logger.atWarn().msg(packageNotWired(JMX)).log();
        return null;
    }

    @Override
    public RuntimeDTO getRuntimeDTO() {
        return di.getInstance(XDtoAdmin.class).runtime();
    }

    @Override
    public void gc() throws Exception {
        final boolean isJMXWired = di.getInstance(PackageWirings.class).isJmxWired();
        if (isJMXWired) {
            di.getInstance(XJmxAdmin.class).gc();
            return;
        }
        System.gc();
        logger.atWarn().msg(packageNotWired(JMX)).log();
    }

    @Override
    public byte[] heapdump() throws Exception {
        final boolean isJMXWired = di.getInstance(PackageWirings.class).isJmxWired();
        if (isJMXWired) {
            return di.getInstance(XJmxAdmin.class).heapdump();
        }
        logger.atWarn().msg(packageNotWired(JMX)).log();
        return null;
    }

    @Override
    public byte[] getLogSnapshot(int count) {
        return logBuffer.getLogSnapshot(count);
    }

    @Override
    public byte[] getLogSnapshot(long fromTime, long toTime) {
        return logBuffer.getLogSnapshot(fromTime, toTime);
    }

    private long getSystemUptime() {
        final boolean isJMXWired = di.getInstance(PackageWirings.class).isJmxWired();
        if (isJMXWired) {
            return ManagementFactory.getRuntimeMXBean().getUptime();
        }
        logger.atWarn().msg(packageNotWired(JMX)).log();
        return 0L;
    }

    private BundleDTO installBundleWithData(String location,
                                            final byte[] data,
                                            final int startLevel,
                                            final boolean shouldRefresh) throws IOException, BundleException, InterruptedException {
        requireNonNull(data);

        Bundle installedBundle;
        if (location == null) {
            location = getLocation(data);
        }
        try (InputStream stream = new ByteArrayInputStream(data)) {
            installedBundle = di.getInstance(BundleContext.class).getBundle(location);
            if (installedBundle == null) {
                installedBundle = di.getInstance(BundleContext.class).installBundle(location, stream);
                installedBundle.adapt(BundleStartLevel.class).setStartLevel(startLevel);
            } else {
                installedBundle.update(stream);
                if (shouldRefresh) {
                    refresh(true);
                }
            }
        }
        return toDTO(installedBundle);
    }

    private Map<String, Object> parseProperties(final List<ConfigValue> newProperties) throws Exception {
        final Map<String, Object> properties = new HashMap<>();
        for (final ConfigValue entry : newProperties) {
            final Object convertedValue = AgentHelper.convert(entry);
            properties.put(entry.key, convertedValue);
        }
        return properties;
    }

    private XResultDTO createOrUpdateConfig(final String pid, final Map<String, Object> newProperties) {
        requireNonNull(pid, "Configuration PID cannot be null");
        requireNonNull(newProperties, "Configuration properties cannot be null");

        final boolean isConfigAdminAvailable = di.getInstance(PackageWirings.class).isConfigAdminWired();
        if (isConfigAdminAvailable) {
            try {
                return di.getInstance(XConfigurationAdmin.class).createOrUpdateConfiguration(pid, newProperties);
            } catch (final Exception e) {
                return createResult(ERROR,
                        "One or more configuration properties cannot be converted to the requested type");
            }
        }
        logger.atWarn().msg(packageNotWired(CM)).log();
        return createResult(SKIPPED, packageNotWired(CM));
    }

    private ServiceRegistration<?> initOSGiEventing() {
        final boolean isEventAdminAvailable = di.getInstance(PackageWirings.class).isEventAdminWired();
        if (isEventAdminAvailable) {
            return di.getInstance(OSGiEventHandler.class).register();
        }
        logger.atWarn().msg(packageNotWired(EVENT_ADMIN)).log();
        return null;
    }

    private Closeable initOSGiLogging() {
        final boolean isLogAvailable = di.getInstance(PackageWirings.class).isLogWired();
        if (isLogAvailable) {
            if (logListener == null) {
                logListener = di.getInstance(OSGiLogListener.class);
                logListener.setLogBuffer(logBuffer);
            }
            // If remote is already known (e.g. late start), set it
            if (remote != null) {
                logListener.setSupervisor(remote);
            }
            return trackLogReader(logListener);
        }
        logger.atWarn().msg(packageNotWired(LOG)).log();
        return null;
    }

    private Closeable trackLogReader(final OSGiLogListener logListener) {
        logReaderTracker = new ServiceTracker<Object, Object>(di.getInstance(BundleContext.class),
                                                              "org.osgi.service.log.LogReaderService", null) {

            @Override
            public Object addingService(final ServiceReference<Object> reference) {
                final boolean isLogAvailable = di.getInstance(PackageWirings.class).isLogWired();
                final Object  service        = super.addingService(reference);
                if (isLogAvailable) {
                    di.getInstance(XLogReaderAdmin.class).register(service, logListener);
                }
                return service;
            }

            @Override
            public void removedService(final ServiceReference<Object> reference, final Object service) {
                final boolean isLogAvailable = di.getInstance(PackageWirings.class).isLogWired();
                if (isLogAvailable) {
                    di.getInstance(XLogReaderAdmin.class).unregister(service, logListener);
                }
            }
        };
        logReaderTracker.open();
        return () -> logReaderTracker.close();
    }

}
