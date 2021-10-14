package in.bytehue.osgifx.console.agent.provider;

import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.dto.CapabilityDTO;
import org.osgi.resource.dto.RequirementDTO;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.converter.Converter;
import aQute.lib.converter.TypeReference;
import aQute.lib.io.ByteBufferInputStream;
import aQute.lib.startlevel.StartLevelRuntimeHandler;
import aQute.libg.shacache.ShaCache;
import aQute.libg.shacache.ShaSource;
import aQute.remote.util.Link;
import in.bytehue.osgifx.console.agent.Agent;
import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.agent.dto.XComponentDTO;
import in.bytehue.osgifx.console.agent.dto.XConfigurationDTO;
import in.bytehue.osgifx.console.agent.dto.XEventDTO;
import in.bytehue.osgifx.console.agent.dto.XFrameworkEventsDTO;
import in.bytehue.osgifx.console.agent.dto.XPropertyDTO;
import in.bytehue.osgifx.console.agent.dto.XServiceDTO;
import in.bytehue.osgifx.console.agent.dto.XThreadDTO;
import in.bytehue.osgifx.console.supervisor.Supervisor;

/**
 * Implementation of the Agent. This implementation implements the Agent
 * interfaces and communicates with a Supervisor interfaces.
 */
public final class AgentServer implements Agent, Closeable, EventHandler {
    private static final Pattern       BSN_P    = Pattern.compile("\\s*([^;\\s]+).*");
    private static final AtomicInteger sequence = new AtomicInteger(1000);

    //
    // Constant so we do not have to repeat it
    //

    private static final TypeReference<Map<String, String>> MAP_STRING_STRING_T = new TypeReference<Map<String, String>>() {
    };

    private static final long[] EMPTY = {};

    private final Logger logger = LoggerFactory.getLogger(getClass());

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
    private final BundleContext            context;
    private final ShaCache                 cache;
    private ShaSource                      source;
    private final Map<String, String>      installed  = new HashMap<>();
    volatile boolean                       quit;
    private Redirector                     redirector = new NullRedirector();
    private Link<Agent, Supervisor>        link;
    private CountDownLatch                 refresh    = new CountDownLatch(0);
    private final StartLevelRuntimeHandler startlevels;
    private final int                      startOptions;

    private final ServiceTracker<MetaTypeService, MetaTypeService>                 metatypeTracker;
    private final ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime> scrTracker;
    private final ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>           configAdminTracker;

    private final AtomicInteger infoFrameworkEvents    = new AtomicInteger();
    private final AtomicInteger warningFrameworkEvents = new AtomicInteger();
    private final AtomicInteger errorFrameworkEvents   = new AtomicInteger();

    /**
     * An agent server is based on a context and takes a name and cache
     * directory
     *
     * @param name the name of the agent's framework
     * @param context a bundle context of the framework
     * @param cache the directory for caching
     */

    public AgentServer(final String name, final BundleContext context, final File cache) {
        this(name, context, cache, StartLevelRuntimeHandler.absent());
    }

    public AgentServer(final String name, final BundleContext context, final File cache, final StartLevelRuntimeHandler startlevels) {
        this.context = context;

        final boolean eager = context.getProperty(aQute.bnd.osgi.Constants.LAUNCH_ACTIVATION_EAGER) != null;
        startOptions = eager ? 0 : Bundle.START_ACTIVATION_POLICY;

        this.cache       = new ShaCache(cache);
        this.startlevels = startlevels;

        metatypeTracker    = new ServiceTracker<>(context, MetaTypeService.class, null);
        scrTracker         = new ServiceTracker<>(context, ServiceComponentRuntime.class, null);
        configAdminTracker = new ServiceTracker<>(context, ConfigurationAdmin.class, null);

        scrTracker.open();
        metatypeTracker.open();
        configAdminTracker.open();
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
    public BundleDTO installWithData(String location, final byte[] data) throws Exception {
        requireNonNull(data);

        Bundle installedBundle;

        if (location == null) {
            location = getLocation(data);
        }

        try (InputStream stream = new ByteBufferInputStream(data)) {
            installedBundle = context.getBundle(location);
            if (installedBundle == null) {
                installedBundle = context.installBundle(location, stream);
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
        final InputStream   is = new URL(url).openStream();

        try {
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
            scrTracker.close();
            metatypeTracker.close();
            configAdminTracker.close();
            cleanup(-2);
            startlevels.close();
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

    @SuppressWarnings("deprecation")
    public void refresh(final boolean async) throws InterruptedException {
        try {
            final FrameworkWiring f = context.getBundle(0).adapt(FrameworkWiring.class);
            if (f != null) {
                refresh = new CountDownLatch(1);
                f.refreshBundles(null, event -> refresh.countDown());

                if (async) {
                    return;
                }

                refresh.await();
                return;
            }
        } catch (Exception | NoSuchMethodError e) {
            @SuppressWarnings("unchecked")
            final ServiceReference<org.osgi.service.packageadmin.PackageAdmin> ref = (ServiceReference<org.osgi.service.packageadmin.PackageAdmin>) context
                    .getServiceReference(org.osgi.service.packageadmin.PackageAdmin.class.getName());
            if (ref != null) {
                final org.osgi.service.packageadmin.PackageAdmin padmin = context.getService(ref);
                padmin.refreshPackages(null);
                return;
            }
        }
        throw new IllegalStateException("Cannot refresh");
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
     * Turn a bundle in a Bundle Revision dto. On a r6 framework we could do
     * this with adapt but on earlier frameworks we're on our own
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
        return XBundleInfoProvider.get(getContext());
    }

    @Override
    public List<XComponentDTO> getAllComponents() {
        return XComponentInfoProvider.get(scrTracker.getService());
    }

    @Override
    public List<XConfigurationDTO> getAllConfigurations() {
        return XConfigurationtInfoProvider.get(getContext(), configAdminTracker.getService(), metatypeTracker.getService());
    }

    @Override
    public List<XPropertyDTO> getAllProperties() {
        return XPropertytInfoProvider.get(getContext());
    }

    @Override
    public List<XServiceDTO> getAllServices() {
        return XServiceInfoProvider.get(getContext());
    }

    @Override
    public List<XThreadDTO> getAllThreads() {
        return XThreadInfoProvider.get();
    }

    @Override
    public Collection<ComponentDescriptionDTO> getComponentDescriptionDTOs() {
        // @formatter:off
        return Optional.ofNullable(scrTracker.getService())
                       .map(ServiceComponentRuntime::getComponentDescriptionDTOs)
                       .orElse(emptyList());
        // @formatter:on
    }

    @Override
    public Collection<ComponentConfigurationDTO> getComponentConfigurationDTOs(final ComponentDescriptionDTO description) {
        // @formatter:off
        return Optional.ofNullable(scrTracker.getService())
                       .map(scr -> scr.getComponentConfigurationDTOs(description))
                       .orElse(emptyList());
        // @formatter:on
    }

    @Override
    public String enableComponent(final String name) {
        final ServiceComponentRuntime service = scrTracker.getService();
        if (service == null) {
            return "Service Component Runtime service is unavailable";
        }
        final StringBuilder                       builder         = new StringBuilder();
        final Collection<ComponentDescriptionDTO> descriptionDTOs = service.getComponentDescriptionDTOs();

        for (final ComponentDescriptionDTO dto : descriptionDTOs) {
            if (dto.name.equals(name)) {
                try {
                    service.enableComponent(dto).onFailure(e -> builder.append(e.getMessage()).append(lineSeparator())).getValue();
                } catch (InvocationTargetException | InterruptedException e) {
                    builder.append(e.getMessage()).append(lineSeparator());
                }
                final String response = builder.toString();
                return response.isEmpty() ? null : response;
            }
        }
        return null;
    }

    @Override
    public String disableComponent(final long id) {
        final ServiceComponentRuntime service = scrTracker.getService();
        if (service == null) {
            return "Service Component Runtime service is unavailable";
        }
        final StringBuilder                       builder         = new StringBuilder();
        final Collection<ComponentDescriptionDTO> descriptionDTOs = service.getComponentDescriptionDTOs();

        for (final ComponentDescriptionDTO dto : descriptionDTOs) {
            final Collection<ComponentConfigurationDTO> configurationDTOs = service.getComponentConfigurationDTOs(dto);
            for (final ComponentConfigurationDTO configDTO : configurationDTOs) {
                if (configDTO.id == id) {
                    try {
                        service.disableComponent(dto).onFailure(e -> builder.append(e.getMessage()).append(lineSeparator())).getValue();
                    } catch (InvocationTargetException | InterruptedException e) {
                        builder.append(e.getMessage()).append(lineSeparator());
                    }
                    final String response = builder.toString();
                    return response.isEmpty() ? null : response;
                }
            }
        }
        return null;
    }

    @Override
    public Collection<ServiceReferenceDTO> getServiceReferences(final String filter) throws Exception {
        return getFramework().services;
    }

    @Override
    public void deleteConfiguration(final String pid) throws IOException {
        final ConfigurationAdmin configAdmin = configAdminTracker.getService();
        if (configAdmin == null) {
            return;
        }
        try {
            for (final Configuration configuration : configAdmin.listConfigurations(null)) {
                if (configuration.getPid().equals(pid)) {
                    configuration.delete();
                }
            }
        } catch (final Exception e) {
            logger.error("Cannot delete configuration '{}'", pid);
        }
    }

    @Override
    public void updateConfiguration(final String pid, final Map<String, Object> newProperties) throws IOException {
        final ConfigurationAdmin configAdmin = configAdminTracker.getService();
        if (configAdmin == null) {
            return;
        }
        try {
            final Configuration configuration = configAdmin.getConfiguration(pid, "?");
            configuration.update(new Hashtable<>(newProperties));
        } catch (final Exception e) {
            logger.error("Cannot update configuration '{}'", pid);
        }
    }

    @Override
    public void createFactoryConfiguration(final String factoryPid, final Map<String, Object> newProperties) throws IOException {
        final ConfigurationAdmin configAdmin = configAdminTracker.getService();
        if (configAdmin == null) {
            return;
        }
        try {
            final Configuration configuration = configAdmin.getFactoryConfiguration(factoryPid, "?");
            configuration.update(new Hashtable<>(newProperties));
        } catch (final Exception e) {
            logger.error("Cannot update configuration '{}'", factoryPid);
        }
    }

    @Override
    public Map<String, String> runtimeInfo() {
        final Map<String, String> runtime      = new HashMap<>();
        final Bundle              systemBundle = getContext().getBundle(SYSTEM_BUNDLE_ID);

        runtime.put("Framework", systemBundle.getSymbolicName());
        runtime.put("Framework Version", systemBundle.getVersion().toString());
        runtime.put("Memory Total", String.valueOf(Runtime.getRuntime().totalMemory()));
        runtime.put("Memory Free", String.valueOf(Runtime.getRuntime().freeMemory()));
        runtime.put("OS Name", System.getProperty("os.name"));
        runtime.put("OS Version", System.getProperty("os.version"));
        runtime.put("OS Architecture", System.getProperty("os.arch"));
        runtime.put("Uptime", String.valueOf(getSystemUptime()));

        return runtime;
    }

    private static long getSystemUptime() {
        final RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        return rb.getUptime();
    }

    @Override
    public XFrameworkEventsDTO getFrameworkEventsOverview() {
        final XFrameworkEventsDTO dto = new XFrameworkEventsDTO();

        dto.info    = infoFrameworkEvents.get();
        dto.warning = warningFrameworkEvents.get();
        dto.error   = errorFrameworkEvents.get();

        return dto;
    }

    @Override
    public void handleEvent(final Event event) {
        final XEventDTO dto = new XEventDTO();

        dto.received   = System.currentTimeMillis();
        dto.properties = initProperties(event);
        dto.topic      = event.getTopic();

        final Supervisor supervisor = getSupervisor();
        supervisor.onOSGiEvent(dto);
    }

    private Map<String, String> initProperties(final Event event) {
        final Map<String, String> properties = new HashMap<>();

        for (final String propertyName : event.getPropertyNames()) {
            Object propertyValue = event.getProperty(propertyName);
            if (propertyValue instanceof String[]) {
                propertyValue = Arrays.asList((String[]) propertyValue);
            }
            properties.put(propertyName, propertyValue.toString());
        }
        return properties;
    }

}
