package in.bytehue.osgifx.console.agent;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;
import org.osgi.service.cm.Configuration.ConfigurationAttribute;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.cm.ReadOnlyConfigurationException;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.agent.dto.XComponentDTO;
import in.bytehue.osgifx.console.agent.dto.XConfigurationDTO;
import in.bytehue.osgifx.console.agent.dto.XFrameworkEventsDTO;
import in.bytehue.osgifx.console.agent.dto.XPropertyDTO;
import in.bytehue.osgifx.console.agent.dto.XServiceDTO;
import in.bytehue.osgifx.console.agent.dto.XThreadDTO;
import in.bytehue.osgifx.console.supervisor.Supervisor;

/**
 * An agent runs on remote OSGi framework and provides the means to control this
 * framework. This API can also be used to install a framework before an agent
 * is started. Such a pre-agent is called an Envoy. An Envoy implements
 * {@link #createFramework(String, Collection, Map)} and {@link #isEnvoy()} only
 * but switches to the agent API once the framework is installed.
 */
@ProviderType
public interface Agent {

    /**
     * The default port. The port can be overridden with the System/framework
     * property {$value {@link #AGENT_SERVER_PORT_KEY}.
     */
    int DEFAULT_PORT = 29998;

    /** The system property to set the port for communication */
    String AGENT_SERVER_PORT_KEY = "osgi.fx.agent.port";

    /**
     * The pattern for a server port specification: {@code [<interface>:]<port>}
     * .
     */
    Pattern PORT_P          = Pattern.compile("(?:([^:]+):)?(\\d+)");
    /**
     * The port for attaching to a remote Gogo CommandSession
     */
    int     COMMAND_SESSION = -1;

    /**
     * The port for having no redircet of IO
     */
    int NONE = 0;

    /**
     * The port for System.in, out, err redirecting.
     */
    int CONSOLE = 1;

    /**
     * An Envoy is an agent that can install a framework (well, -runpath) and
     * launch it with an Agent. An envoy can only handle this method and
     * {@link #createFramework(String, Collection, Map)} so other methods should
     * not be called. This rather awkward model is necessary so that we do not
     * have to reconnect to the actual agent.
     *
     * @return true if this is a limited envoy, otherwise true for a true Agent.
     */
    boolean isEnvoy();

    /**
     * Get the Bundles for the given ids. If no ids are given, all bundles are
     * returned.
     */
    List<BundleDTO> getBundles(long... bundleId) throws Exception;

    /**
     * Get the Bundle Revisions for the given ids. If no ids are given, the
     * revisions for all bundles must be returned.
     */

    List<BundleRevisionDTO> getBundleRevisons(long... bundleId) throws Exception;

    /**
     * Get the framework DTO
     */
    FrameworkDTO getFramework() throws Exception;

    /**
     * Install or update a bundle from the specified byte array instance.
     * <p>
     * This method does check if there is any existing bundle with the specified
     * {@code location} identifier. If found, the existing bundle gets updated
     * with the specified byte array instance. Otherwise, a new bundle gets
     * installed with the specified byte array instance.
     *
     * @param location The bundle location (cannot be {@code null})
     * @param data The byte array instance from which this bundle will be read
     *            (cannot be {@code null})
     * @return A Bundle DTO (cannot be {@code null})
     * @throws Exception if the bundle cannot be installed or updated
     */
    BundleDTO installWithData(String location, byte[] data) throws Exception;

    /**
     * Install a new bundle at the given bundle location. The SHA identifies the
     * file and should be retrievable through {@link Supervisor#getFile(String)}
     * .
     *
     * @param location the bundle location
     * @param sha the sha of the bundle's JAR
     * @return A Bundle DTO
     */
    BundleDTO install(String location, String sha) throws Exception;

    /**
     * Install a new bundle at the given location using a url to get the stream.
     * <p>
     * <b>NOTICE:</b> this method makes assumptions about the target e.g. that
     * it will be able to use out-of-band communication to read from the URL and
     * have the necessary url handlers to open the URL stream.
     * </p>
     *
     * @param location the bundle location
     * @param url url of the bundle that can retrived using url.openStream()
     * @return A bundle DTO
     */
    BundleDTO installFromURL(String location, String url) throws Exception;

    /**
     * Start a number of bundles
     *
     * @param id the bundle ids
     * @return any errors that occurred
     */
    String start(long... id) throws Exception;

    /**
     * Stop a number of bundles
     *
     * @param id the bundle ids
     * @return any errors that occurred
     */
    String stop(long... id) throws Exception;

    /**
     * Uninstall a number of bundles
     *
     * @param id the bundle ids
     * @return any errors that occurred
     */

    String uninstall(long... id) throws Exception;

    /**
     * Update the bundles in the framework. Each agent compares this map against
     * a map of installed bundles. The map maps a bundle location to SHA. Any
     * differences are reflected in the installed bundles. That is, a change in
     * the SHA will update, a new entry will install, and a removed entry will
     * uninstall. This is the preferred way to keep the remote framework
     * synchronized since it is idempotent.
     *
     * @param bundles the bundles to update
     */
    String update(Map<String, String> bundles) throws Exception;

    /**
     * Updates a single bundle by id in the framework. The SHA identifies the
     * file and should be retrievable through {@link Supervisor#getFile(String)}
     *
     * @param id the bundle id
     * @param sha the sha of the bundle
     * @return any errors that occurred
     */
    String update(long id, String sha) throws Exception;

    /**
     * Updates a single bundle from a url
     * <p>
     * <b>NOTICE:</b> this method makes assumptions about the target e.g. that
     * it will be able to use out-of-band communication to read from the URL and
     * have the necessary url handlers to open the URL stream.
     * </p>
     *
     * @param id bundle to update
     * @param url location of bundle contents
     * @return any errors that occurred
     */
    String updateFromURL(long id, String url) throws Exception;

    /**
     * Redirect I/O from port. Port can be {@link #CONSOLE},
     * {@link #COMMAND_SESSION}, {@link #NONE}, or a TCP Telnet port.
     *
     * @param port the port to redirect from
     * @return if the redirection was changed
     */
    boolean redirect(int port) throws Exception;

    /**
     * Send a text to the potentially redirected stdin stream so that remotely
     * executing code will read it from an InputStream.
     *
     * @param s text that should be read as input
     * @return true if this was redirected
     */
    boolean stdin(String s) throws Exception;

    /**
     * Execute a remote command on Gogo (if present) and return the result.
     *
     * @param cmd the command to execute
     * @return the result
     */
    String shell(String cmd) throws Exception;

    /**
     * Get the remote's system's System properties
     *
     * @return the remote systems properties
     */
    Map<String, String> getSystemProperties() throws Exception;

    /**
     * This method is only implemented in the Envoy (the pre-Agent). It is meant
     * to install a -runpath before the framework runs. An Envoy can actally
     * created multiple independent frameworks. If this framework already
     * existed, and the given parameters are identical, that framework will be
     * used for the aget that will take over. Otherwise the current framework is
     * stopped and a new framework is started.
     *
     * @param name the name of the framework
     * @param runpath the runpath the install
     * @param properties the framework properties
     * @return if this created a new framework
     */
    boolean createFramework(String name, Collection<String> runpath, Map<String, Object> properties) throws Exception;

    /**
     * Abort the remote agent. The agent should send an event back and die. This
     * is an async method.
     */
    void abort() throws Exception;

    /**
     * Ping the remote agent to see if it is still alive.
     */
    boolean ping();

    /**
     * Get the detailed information of all the installed bundles
     *
     * @return the detailed information of all the installed bundles
     */
    List<XBundleDTO> getAllBundles();

    /**
     * Get the detailed information of all the registered DS service components
     *
     * @return the detailed information of all the registered DS service components
     */
    List<XComponentDTO> getAllComponents();

    /**
     * Get the detailed information of all the configurations
     *
     * @return the detailed information of all the configurations
     */
    List<XConfigurationDTO> getAllConfigurations();

    /**
     * Get the detailed information of all the properties
     *
     * @return the detailed information of all the properties
     */
    List<XPropertyDTO> getAllProperties();

    /**
     * Get the detailed information of all services
     *
     * @return the detailed information of all services
     */
    List<XServiceDTO> getAllServices();

    /**
     * Get the detailed information of all the threads
     *
     * @return the detailed information of all the threads
     */
    List<XThreadDTO> getAllThreads();

    /**
     * Returns all the component descriptions.
     *
     * <p>
     * Only component descriptions from active bundles are returned.
     *
     * @return The declared component descriptions of all the active {@code bundles}.
     *         An empty collection is returned if there are no component descriptions
     *         for the specified active bundles.
     */
    Collection<ComponentDescriptionDTO> getComponentDescriptionDTOs();

    /**
     * Returns the component configurations for the specified component
     * description.
     *
     * @param description The component description. Must not be {@code null}.
     * @return A collection containing a snapshot of the current component
     *         configurations for the specified component description. An empty
     *         collection is returned if there are none or if the provided
     *         component description does not belong to an active bundle.
     */
    Collection<ComponentConfigurationDTO> getComponentConfigurationDTOs(ComponentDescriptionDTO description);

    /**
     * Enables the specified component description.
     *
     * <p>
     * If the specified component description is currently enabled, this method
     * has no effect.
     *
     * <p>
     * This method must return after changing the enabled state of the specified
     * component description. Any actions that result from this, such as
     * activating or deactivating a component configuration, must occur
     * asynchronously to this method call.
     *
     * @param name The name of the component description to enable.
     * @return the error response as string
     */
    String enableComponent(String name);

    /**
     * Disables the specified component description.
     * <p>
     * If the specified component description is currently disabled, this method
     * has no effect.
     * <p>
     * This method must return after changing the enabled state of the specified
     * component description. Any actions that result from this, such as
     * activating or deactivating a component configuration, must occur
     * asynchronously to this method call.
     *
     * @param id The id of component description to enable.
     * @return the error response as string
     */
    String disableComponent(long id);

    /**
     *
     * Returns an array of {@code ServiceReferenceDTO} objects. The returned array
     * of {@code ServiceReferenceDTO} objects contains services that were
     * registered under the specified class and match the specified filter
     * expression.
     *
     * <p>
     * The list is valid at the time of the call to this method. However since
     * the Framework is a very dynamic environment, services can be modified or
     * unregistered at any time.
     *
     * <p>
     * The specified {@code filter} expression is used to select the registered
     * services whose service properties contain keys and values which satisfy
     * the filter expression. See {@link Filter} for a description of the filter
     * syntax. If the specified {@code filter} is {@code null}, all registered
     * services are considered to match the filter. If the specified
     * {@code filter} expression cannot be parsed, an
     * {@link InvalidSyntaxException} will be thrown with a human readable
     * message where the filter became unparsable.
     *
     * @param filter The filter expression or {@code null} for all services
     * @return An array of {@code ServiceReferenceDTO} objects or {@code empty} array if
     *         no services are registered which satisfy the search.
     * @throws InvalidSyntaxException If the specified {@code filter} contains
     *             an invalid filter expression that cannot be parsed.
     * @throws IllegalStateException If this BundleContext is no longer valid.
     */
    Collection<ServiceReferenceDTO> getServiceReferences(String filter) throws Exception;

    /**
     * Delete the associated {@code Configuration} object.
     * <p>
     * Removes this configuration object from the persistent store. Notify
     * asynchronously the corresponding Managed Service or Managed Service
     * Factory. A {@link ManagedService} object is notified by a call to its
     * {@code updated} method with a {@code null} properties argument. A
     * {@link ManagedServiceFactory} object is notified by a call to its
     * {@code deleted} method.
     * <p>
     * Also notifies all Configuration Listeners with a
     * {@link ConfigurationEvent#CM_DELETED} event.
     *
     * @throws ReadOnlyConfigurationException If the configuration is
     *             {@link ConfigurationAttribute#READ_ONLY read only}.
     * @throws IOException If delete fails.
     * @throws IllegalStateException If this configuration has been deleted.
     */
    void deleteConfiguration(String pid) throws IOException;

    /**
     * Update the {@code Configuration} object associated with the specified
     * {@code pid} with the current properties.
     *
     * @param pid the Configuration PID to update
     * @param newProperties the new properties to associate
     *
     * @throws IOException if update cannot access the properties in persistent
     *             storage
     * @throws IllegalStateException If this configuration has been deleted.
     */
    void updateConfiguration(String pid, Map<String, Object> newProperties) throws IOException;

    /**
     * Create the {@code Configuration} object associated with the specified
     * {@code factoryPid} with the current properties.
     *
     * @param factoryPid the Configuration Factory PID
     * @param newProperties the new properties to associate
     *
     * @throws IOException if update cannot access the properties in persistent
     *             storage
     * @throws IllegalStateException If this configuration has been deleted.
     */
    void createFactoryConfiguration(String factoryPid, Map<String, Object> newProperties) throws IOException;

    /**
     * Returns the runtime information of the remote system
     *
     * @return the runtime information
     */
    Map<String, String> runtimeInfo();

    /**
     * Returns the overview of the framework events
     *
     * @return the overview of the framework events
     */
    XFrameworkEventsDTO getFrameworkEventsOverview();

}
