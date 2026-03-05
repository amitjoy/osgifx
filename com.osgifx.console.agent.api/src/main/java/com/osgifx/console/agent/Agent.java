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
package com.osgifx.console.agent;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;

import com.osgifx.console.agent.dto.ConfigValue;
import com.osgifx.console.agent.dto.DmtDataType;
import com.osgifx.console.agent.dto.RuntimeDTO;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XBundleLoggerContextDTO;
import com.osgifx.console.agent.dto.XCdiContainerDTO;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XDmtNodeDTO;
import com.osgifx.console.agent.dto.XHealthCheckDTO;
import com.osgifx.console.agent.dto.XHealthCheckResultDTO;
import com.osgifx.console.agent.dto.XHeapUsageDTO;
import com.osgifx.console.agent.dto.XHttpComponentDTO;
import com.osgifx.console.agent.dto.XJaxRsComponentDTO;
import com.osgifx.console.agent.dto.XMemoryInfoDTO;
import com.osgifx.console.agent.dto.XPropertyDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.agent.dto.XRoleDTO;
import com.osgifx.console.agent.dto.XRuntimeCapabilityDTO;
import com.osgifx.console.agent.dto.XServiceDTO;
import com.osgifx.console.agent.dto.XThreadDTO;
import com.osgifx.console.agent.extension.AgentExtension;
import com.osgifx.console.agent.extension.AgentExtensionName;
import com.osgifx.console.agent.rpc.mqtt.api.Mqtt5Publisher;
import com.osgifx.console.agent.rpc.mqtt.api.Mqtt5Subscriber;

/**
 * The {@code Agent} interface defines the remote-management contract for an OSGi
 * framework. An agent implementation runs inside a target OSGi runtime and exposes
 * operations—such as bundle lifecycle management, configuration administration,
 * log inspection, and diagnostics—to a remote {@code Supervisor} (the OSGi.fx desktop
 * console).
 *
 * <h2>Communication Channels</h2>
 * The agent supports two RPC transports:
 * <ul>
 * <li><b>Socket</b> – configured via {@link #AGENT_SOCKET_PORT_KEY}. A plain or
 * TLS-secured TCP connection.</li>
 * <li><b>MQTT 5</b> – configured via {@link #AGENT_MQTT_PROVIDER_KEY},
 * {@link #AGENT_MQTT_PUB_TOPIC_KEY}, and {@link #AGENT_MQTT_SUB_TOPIC_KEY}.
 * Supports OSGi Messaging or custom {@link Mqtt5Publisher}/{@link Mqtt5Subscriber}
 * implementations.</li>
 * </ul>
 *
 * <h2>Graceful Degradation</h2>
 * Methods that depend on optional OSGi compendium services (ConfigAdmin, SCR, EventAdmin,
 * UserAdmin, DMT Admin, etc.) return empty collections or {@code SKIPPED} results when the
 * required service is not wired into the runtime. This allows the agent to operate on
 * minimal OSGi runtimes—including constrained/embedded devices—without forcing the
 * installation of unused services.
 *
 * <h2>Thread Safety</h2>
 * Implementations must be safe for concurrent use from multiple RPC handler threads.
 *
 * @since 1.0
 */
@ProviderType
public interface Agent {

    /**
     * Bundle location prefix for installing new bundles
     */
    String BUNDLE_LOCATION_PREFIX = "manual:";

    /**
     * The pattern for a server port specification: {@code [<interface>:]<port>} .
     */
    Pattern AGENT_SOCKET_PORT_PATTERN = Pattern.compile("(?:([^:]+):)?(\\d+)");

    /**
     * The property key to set the agent's socket port.
     */
    String AGENT_SOCKET_PORT_KEY = "osgi.fx.agent.socket.port";

    /**
     * The property key to set the agent's authentication password.
     */
    String AGENT_SOCKET_PASSWORD_KEY = "osgi.fx.agent.socket.password";

    /**
     * The property key to enable secure agent communication.
     */
    String AGENT_SOCKET_SECURE_COMMUNICATION_KEY = "osgi.fx.agent.socket.secure";

    /**
     * The property key for the custom {@link SSLContext} enabling secure agent communication.
     */
    String AGENT_SOCKET_SECURE_COMMUNICATION_SSL_CONTEXT_FILTER_KEY = "osgi.fx.agent.socket.secure.sslcontext.filter";

    /**
     * The property key to specify the MQTT implementation type to use
     * <p>
     * The supported types are:
     * <p>
     * <ul>
     * <li>{@code osgi-messaging}: OSGi Messaging Implementation</li>
     * <li>{@code custom}: Custom MQTT 5 client</li>
     * </ul>
     * <p>
     * In the latter scenario, you have to implement {@link Mqtt5Publisher} and
     * {@link Mqtt5Subscriber} yourself.
     */
    String AGENT_MQTT_PROVIDER_KEY = "osgi.fx.agent.mqtt.provider";

    /**
     * The property value indicating the OSGi Messaging MQTT implementation to use
     */
    String AGENT_MQTT_PROVIDER_DEFAULT_VALUE = "osgi-messaging";

    /**
     * The property key to specify the publish topic for publishing the data using MQTT
     */
    String AGENT_MQTT_PUB_TOPIC_KEY = "osgi.fx.agent.mqtt.pubtopic";

    /**
     * The property key to specify the subscription topic for receiving the data using MQTT
     */
    String AGENT_MQTT_SUB_TOPIC_KEY = "osgi.fx.agent.mqtt.subtopic";

    /**
     * The property key to enable auto-start of the log capture mechanism
     */
    String AGENT_AUTO_START_LOG_CAPTURE_KEY = "osgi.fx.agent.auto.start.log.capture";

    /**
     * The property key to enable CLI command execution. Defaults to {@code true}.
     */
    String AGENT_CLI_ENABLED_KEY = "osgi.fx.agent.cli.enabled";

    /**
     * The property key to specify the allowlist for CLI command execution. Defaults to {@code *}.
     */
    String AGENT_CLI_ALLOWLIST_KEY = "osgi.fx.agent.cli.allowlist";

    /**
     * The property key to enable Gogo command execution. Defaults to {@code true}.
     */
    String AGENT_GOGO_ENABLED_KEY = "osgi.fx.agent.gogo.enabled";

    /**
     * The property key to specify the allowlist for Gogo command execution. Defaults to {@code *}.
     */
    String AGENT_GOGO_ALLOWLIST_KEY = "osgi.fx.agent.gogo.allowlist";

    /**
     * The property key to specify the maximum total decompressed size of a GZIP stream in an RPC call.
     * <p>
     * This is used to prevent Zip Bomb attacks.
     *
     * @since 11.0
     */
    String AGENT_RPC_MAX_DECOMPRESSED_SIZE_KEY = "osgi.fx.agent.rpc.max.decompressed.size";

    /**
     * The property key to specify the maximum number of elements allowed in a decoded collection.
     * <p>
     * This is used to prevent Collection Bomb attacks.
     *
     * @since 11.0
     */
    String AGENT_RPC_MAX_COLLECTION_SIZE_KEY = "osgi.fx.agent.rpc.max.collection.size";

    /**
     * The property key to specify the maximum number of entries allowed in a decoded map.
     * <p>
     * This is used to prevent Collection Bomb attacks.
     *
     * @since 11.0
     */
    String AGENT_RPC_MAX_MAP_SIZE_KEY = "osgi.fx.agent.rpc.max.map.size";

    /**
     * The property key to specify the maximum total length allowed for a decoded byte array.
     * <p>
     * This is used to prevent memory exhaustion when decoding large byte arrays.
     *
     * @since 11.0
     */
    String AGENT_RPC_MAX_BYTE_ARRAY_SIZE_KEY = "osgi.fx.agent.rpc.max.byte.array.size";

    /**
     * The port for attaching to a remote Gogo CommandSession
     */
    int COMMAND_SESSION = -1;

    /**
     * The port for having no redirect of IO
     */
    int NONE = 0;

    /**
     * The port for System.in, out, err redirecting.
     */
    int CONSOLE = 1;

    /**
     * Install or update a bundle from the specified byte array instance.
     * <p>
     * This method does check if there is any existing bundle with the specified
     * {@code location} identifier. If found, the existing bundle gets updated with
     * the specified byte array instance. Otherwise, a new bundle gets installed
     * with the specified byte array instance.
     *
     * @param location The bundle location (if set to {@code null}, the existing
     *            bundle location is used)
     * @param data The byte array instance from which this bundle will be read
     *            (cannot be {@code null})
     * @param startLevel the start level of the bundle
     * @return A Bundle DTO (cannot be {@code null})
     * @throws Exception if the bundle cannot be installed or updated
     */
    BundleDTO installWithData(String location, byte[] data, int startLevel) throws Exception;

    /**
     * Install or update multiple bundles from the specified byte array instance.
     * <p>
     * This method does check if there is any existing bundle with the bsn. If
     * found, the existing bundles get updated with the specified byte array
     * instances. Otherwise, new bundles get installed with the specified byte array
     * instances.
     *
     * @param data The byte array instances from which the bundle will be read
     *            (cannot be {@code null})
     * @param startLevel the start level of the bundles
     * @return the detailed information about the operation whether it succeeded or
     *         failed
     */
    XResultDTO installWithMultipleData(Collection<byte[]> data, int startLevel);

    /**
     * Install a new bundle at the given location using a url to get the stream.
     * <p>
     * <b>NOTICE:</b> this method makes assumptions about the target e.g. that it
     * will be able to use out-of-band communication to read from the URL and have
     * the necessary url handlers to open the URL stream.
     * </p>
     *
     * @param location the bundle location
     * @param url url of the bundle that can retrieved using url.openStream()
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
     * Returns the Bundle Revisions for the given bundle IDs. If no IDs are given,
     * the revisions for all bundles must be returned.
     *
     * @return the bundle revisions
     */
    List<BundleRevisionDTO> getBundleRevisons(final long... bundleId) throws Exception;

    /**
     * Retrieves the content of a file from the specified bundle's persistent
     * storage area ({@code BundleContext.getDataFile()}).
     * <p>
     * The {@code fileName} is resolved relative to the bundle's private data
     * directory. Path traversal sequences (e.g., {@code ".."}) are not permitted
     * and will result in an {@link IllegalArgumentException}.
     *
     * @param id the bundle ID
     * @param fileName the name of the file to retrieve, relative to the bundle's
     *            data area (cannot be {@code null})
     * @return the content of the file as a string, or {@code null} if the bundle
     *         does not exist, does not have an active {@code BundleContext}, or
     *         the specified file does not exist
     * @throws IllegalArgumentException if {@code fileName} contains path traversal
     *             sequences
     * @throws Exception if an error occurs while reading the file
     */
    String getBundleDataFile(long id, String fileName) throws Exception;

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
     * Executes a remote command on Gogo shell (if present).
     *
     * @param command the command to execute
     * @return the response
     */
    String execGogoCommand(String command) throws Exception;

    /**
     * Checks if the receiving of logs is enabled
     *
     * @return {@code true} if enabled, otherwise, {@code false}
     */
    boolean isReceivingLogEnabled();

    /**
     * Enables receiving logs from remote agent
     */
    void enableReceivingLog();

    /**
     * Disables receiving logs from remote agent
     */
    void disableReceivingLog();

    /**
     * Checks if the receiving of events is enabled
     *
     * @return {@code true} if enabled, otherwise, {@code false}
     */
    boolean isReceivingEventEnabled();

    /**
     * Enables receiving events from remote agent
     */
    void enableReceivingEvent();

    /**
     * Disables receiving events from remote agent
     */
    void disableReceivingEvent();

    /**
     * Executes the specified terminal (CLI) command in a separate process.
     *
     * @param command the command to execute
     * @return the response
     */
    String execCliCommand(String command);

    /**
     * Disconnects the remote agent. The agent should send an event back and die. This is
     * an async method.
     */
    void disconnect() throws Exception;

    /**
     * Ping the remote agent to see if it is still alive.
     */
    boolean ping();

    /**
     * Returns the detailed information of all the installed bundles
     *
     * @return the detailed information of all the installed bundles
     */
    List<XBundleDTO> getAllBundles();

    /**
     * Get the detailed information of all the registered DS service components
     * <p>
     * Note that, this is only possible if the remote runtime has SCR bundle
     * installed.
     *
     * @return the detailed information of all the registered DS service components,
     *         otherwise {@code empty} list if the remote runtime does not have SCR
     *         bundle installed
     */
    List<XComponentDTO> getAllComponents();

    /**
     * Get the detailed information of all the configurations
     * <p>
     * Note that, this is only possible if the remote runtime has ConfigAdmin (CM)
     * bundle installed.
     * <p>
     * Also note that, if metatype bundle is installed, the property descriptors
     * will also be included.
     *
     * @return the detailed information of all the configurations, otherwise
     *         {@code empty} list if the remote runtime does not have CM bundle
     *         installed
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
     * Returns all the children of the specified DMT node URI
     *
     * @param rootURI the root URI for which the children will be returned
     * @return all the children of the specified DMT node
     */
    XDmtNodeDTO readDmtNode(String rootURI);

    /**
     * Update the specified DMT leaf node with the specified value
     *
     * @param uri the DMT node URI
     * @param value the value to set
     * @param format the format to use
     */
    XResultDTO updateDmtNode(String uri, Object value, DmtDataType format);

    /**
     * Updates the logger context of the specified bundle
     *
     * @param bsn the bundle symbolic name denoting the name of the logger
     *            context
     * @param logLevels the log levels to update
     * @return the detailed information about the operation whether it succeeded or
     *         failed
     */
    XResultDTO updateBundleLoggerContext(String bsn, Map<String, String> logLevels);

    /**
     * Enables the component description by name
     *
     * @param name The name of the component description to enable.
     * @return the detailed information about the operation whether it succeeded or
     *         failed
     */
    XResultDTO enableComponentByName(String name);

    /**
     * Enables the component description by identifier
     *
     * @param id The id of the component description to enable.
     * @return the detailed information about the operation whether it succeeded or
     *         failed
     */
    XResultDTO enableComponentById(long id);

    /**
     * Disables the component description by name
     *
     * @param name The name of component description to disable.
     * @return the detailed information about the operation whether it succeeded or
     *         failed
     */
    XResultDTO disableComponentByName(String name);

    /**
     * Disables the component description by identifier
     *
     * @param id The id of component description to disable.
     * @return the detailed information about the operation whether it succeeded or
     *         failed
     */
    XResultDTO disableComponentById(long id);

    /**
     * Creates or updates the associated {@code Configuration} objects with the
     * specified properties.
     * <p>
     * Note that, this is only possible if the remote runtime has ConfigAdmin (CM)
     * bundle installed.
     *
     * @param configurations the configurations (key=PID, value=properties)
     * @return the detailed information about the operations whether it succeeded or
     *         failed
     */
    Map<String, XResultDTO> createOrUpdateConfigurations(Map<String, Map<String, Object>> configurations);

    /**
     * Creates or updates the associated {@code Configuration} object with the
     * specified properties.
     * <p>
     * Note that, this is only possible if the remote runtime has ConfigAdmin (CM)
     * bundle installed.
     *
     * @param pid the configuration PID to update
     * @param newProperties the new properties to associate
     * @return the detailed information about the operation whether it succeeded or
     *         failed
     */
    XResultDTO createOrUpdateConfiguration(String pid, List<ConfigValue> newProperties);

    /**
     * Deletes the associated {@code Configuration} object that corresponds to the
     * specified {@code pid}.
     * <p>
     * Note that, this is only possible if the remote runtime has ConfigAdmin CM
     * bundle installed.
     *
     * @param pid The configuration PID to delete.
     * @return the detailed information about the operation whether it succeeded or
     *         failed
     */
    XResultDTO deleteConfiguration(String pid);

    /**
     * Create the {@code Configuration} object associated with the specified
     * {@code factoryPid} with the specified properties.
     * <p>
     * Note that, this is only possible if the remote runtime has ConfigAdmin (CM)
     * bundle installed.
     *
     * @param factoryPid the Configuration Factory PID
     * @param newProperties the new properties to associate
     * @return the detailed information about the operation whether it succeeded or
     *         failed
     */
    XResultDTO createFactoryConfiguration(String factoryPid, List<ConfigValue> newProperties);

    /**
     * Publish event synchronously (the method does not return until the event is
     * processed)
     *
     * @param topic topic of the event to be published
     * @param properties data to be published with the event
     * @return the detailed information about the operation whether it succeeded or
     *         failed
     */
    XResultDTO sendEvent(String topic, List<ConfigValue> properties);

    /**
     * Publish event asynchronously ((this method returns immediately))
     *
     * @param topic topic of the event to be published
     * @param properties data to be published with the event
     * @return the detailed information about the operation whether it succeeded or
     *         failed
     */
    XResultDTO postEvent(String topic, List<ConfigValue> properties);

    /**
     * Returns the memory information of the remote system
     *
     * @return the memory information
     */
    XMemoryInfoDTO getMemoryInfo();

    /**
     * Returns the set of registered Gogo commands
     * <p>
     * Note that, this is only possible if the remote runtime has Gogo bundle(s)
     * installed
     *
     * @return the set of registered Gogo commands, otherwise {@code empty} set if
     *         the remote runtime does not have Gogo bundle(s) installed
     */
    Set<String> getGogoCommands();

    /**
     * Creates the role as specified by the given name and the type
     *
     * @param name the name of the role (cannot be {@code null})
     * @param type the type of role to create
     * @return the detailed information about the operation whether it succeeded or
     *         failed
     * @throws NullPointerException if the {@code name} or {@code type} is
     *             {@code null}
     */
    XResultDTO createRole(String name, XRoleDTO.Type type);

    /**
     * Updates the role as specified by the given name from the specified
     * {@code dto}
     *
     * @param dto the new information
     * @return the detailed information about the operation whether it succeeded or
     *         failed
     * @throws NullPointerException if the {@code dto} is {@code null}
     */
    XResultDTO updateRole(XRoleDTO dto);

    /**
     * Removes the role as specified by the given name
     *
     * @param name the name of the role (cannot be {@code null})
     * @return the detailed information about the operation whether it succeeded or
     *         failed
     * @throws NullPointerException if the {@code name} is {@code null}
     */
    XResultDTO removeRole(String name);

    /**
     * Returns the existing roles stored in {@code UserAdmin}
     *
     * @return the list of roles (can be empty)
     */
    List<XRoleDTO> getAllRoles();

    /**
     * Returns the existing Felix healthchecks
     *
     * @return the list of healthchecks (can be empty)
     */
    List<XHealthCheckDTO> getAllHealthChecks();

    /**
     * Executes the specified healthchecks
     *
     * @param tags the tags to execute (can be {@code null})
     * @param names the names to execute (can be {@code null})
     * @return the list of results (can be empty)
     */
    List<XHealthCheckResultDTO> executeHealthChecks(List<String> tags, List<String> names);

    /**
     * Returns the result from the specified agent extension.
     * <p>
     * <b>Note that,</b> the extension should be registered as a service that
     * implements {@link AgentExtension} and the service must provide a readable
     * name in its {@code agent.extension.name} service property.
     *
     * @param name the name of the extension
     * @param context the context for the extension to be provided for execution
     *            (note that, the map should be compliant with
     *            {@code OSGi DTO specification}
     * @return the value in compliance with {@code OSGi DTO specification}
     * @see AgentExtension
     * @see AgentExtensionName
     */
    Map<String, Object> executeExtension(String name, Map<String, Object> context);

    /**
     * Returns the list of suspicious bundles causing probable classloader leaks
     *
     * @return the set of bundles (can be empty)
     */
    Set<XBundleDTO> getClassloaderLeaks();

    /**
     * Returns the HTTP runtime information that includes list of all servlets,
     * resources, listeners, filters and error pages
     *
     * @return the list of all HTTP components
     */
    List<XHttpComponentDTO> getHttpComponents();

    /**
     * Returns the JAX-RS runtime information
     *
     * @return the list of all JAX-RS components
     */
    List<XJaxRsComponentDTO> getJaxRsComponents();

    /**
     * Returns the CDI runtime information
     *
     * @return the list of all CDI containers
     */
    List<XCdiContainerDTO> getCdiContainers();

    /**
     * Returns the bundle logger contexts (only valid for OSGi R7)
     *
     * @return the list of all bundle logger contexts
     */
    List<XBundleLoggerContextDTO> getBundleLoggerContexts();

    /**
     * Returns the heap usage information
     *
     * @return the heap usage information
     */
    XHeapUsageDTO getHeapUsage();

    /**
     * Returns the runtime DTOs
     *
     * @return the runtime DTOs
     */
    RuntimeDTO getRuntimeDTO();

    /**
     * Performs a heap dump in the remote machine
     *
     * @return the heap dump information
     */
    byte[] heapdump() throws Exception;

    /**
     * Performs a garbage collection
     */
    void gc() throws Exception;

    /**
     * Returns a binary snapshot of the last N logs.
     *
     * @param count number of logs (0 for all stored logs)
     * @return the raw byte array containing the binary log entries
     * @since 11.0
     */
    byte[] getLogSnapshot(int count);

    /**
     * Returns a binary snapshot of logs within the timeframe.
     *
     * @param fromTime start timestamp (epoch millis, inclusive)
     * @param toTime end timestamp (epoch millis, inclusive)
     * @return the raw byte array containing the binary log entries
     * @since 11.0
     */
    byte[] getLogSnapshot(long fromTime, long toTime);

    /**
     * Searches strictly inside the bundle JAR and its attached fragments.
     * <p>
     * This is a strict wrapper for {@code Bundle.findEntries()}.
     *
     * @param bundleId the bundle ID
     * @param path the path to start searching (e.g., {@code "/icons"})
     * @param pattern the file pattern (e.g., {@code "*.png"})
     * @param recursive {@code true} to recurse into subdirectories
     * @return list of resource paths (can be empty)
     * @since 11.0
     */
    Collection<String> findBundleEntries(long bundleId, String path, String pattern, boolean recursive);

    /**
     * Searches the bundle's class space (including imports).
     * <p>
     * This is a strict wrapper for {@code BundleWiring.listResources()}.
     *
     * @param bundleId the bundle ID
     * @param path the path to start searching (e.g., {@code "com/example/service"})
     * @param pattern the file pattern (e.g., {@code "*.class"})
     * @param options bitwise options ({@code 1}=LOCAL, {@code 2}=RECURSE)
     * @return list of resource paths (can be empty)
     * @since 11.0
     */
    Collection<String> listBundleResources(long bundleId, String path, String pattern, int options);

    /**
     * Estimates the compressed heapdump size based on current heap usage.
     * <p>
     * This uses the current heap usage and applies an estimated compression ratio
     * (typically 20-30% for GZIP compression of heap dumps).
     *
     * @return estimated compressed heapdump size in bytes
     * @since 11.0
     */
    long estimateHeapdumpSize();

    /**
     * Creates a heapdump and saves it locally on the agent device.
     * <p>
     * The heapdump is compressed with GZIP and saved to the specified path.
     * The file is NOT automatically deleted - the caller is responsible for cleanup.
     *
     * @param outputPath the absolute path where the heapdump should be saved
     *            (e.g., "/opt/agent/heapdumps/dump.hprof.gz")
     * @return the absolute path to the created heapdump file
     * @throws Exception if the heapdump creation fails
     * @since 11.0
     */
    String createHeapdumpLocally(String outputPath) throws Exception;

    /**
     * Estimates the snapshot size based on current runtime state.
     * <p>
     * This estimates the size of a JSON snapshot by counting bundles, components,
     * configurations, services, and other runtime objects.
     *
     * @return estimated snapshot size in bytes
     * @since 11.0
     */
    long estimateSnapshotSize();

    /**
     * Creates a snapshot and saves it locally on the agent device.
     * <p>
     * The snapshot is saved as JSON to the specified path.
     * The file is NOT automatically deleted - the caller is responsible for cleanup.
     *
     * @param outputPath the absolute path where the snapshot should be saved
     *            (e.g., "/opt/agent/snapshots/snapshot.json")
     * @return the absolute path to the created snapshot file
     * @throws Exception if the snapshot creation fails
     * @since 11.0
     */
    String createSnapshotLocally(String outputPath) throws Exception;

    /**
     * Returns the availability of all optional OSGi compendium features tracked by
     * the agent (SCR, ConfigAdmin, JAX-RS, HTTP, CDI, HealthCheck, etc.).
     *
     * <p>
     * Each entry in the returned list corresponds to one {@code PackageWirings.Type}
     * value. The check bypasses the internal wiring cache so that the result always
     * reflects the <em>current</em> live state of the connected runtime. Callers
     * should invoke this method only when a bundle-action event has fired, not on
     * every user interaction.
     * </p>
     *
     * @return a non-{@code null}, unmodifiable list of capability descriptors
     * @since 12.0
     */
    List<XRuntimeCapabilityDTO> getRuntimeCapabilities();

}
