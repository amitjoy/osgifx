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
import com.osgifx.console.agent.dto.XServiceDTO;
import com.osgifx.console.agent.dto.XThreadDTO;
import com.osgifx.console.agent.extension.AgentExtension;
import com.osgifx.console.agent.extension.AgentExtensionName;

/**
 * An agent runs on remote OSGi framework and provides the means to control this
 * framework
 */
@ProviderType
public interface Agent {

    /**
     * The default port. The port can be overridden with the System/framework
     * property {$value {@link #AGENT_SERVER_PORT_KEY}.
     */
    int DEFAULT_PORT = 29998;

    /**
     * The property key to set the agent's port.
     */
    String AGENT_SERVER_PORT_KEY = "osgi.fx.agent.port";

    /**
     * The property key to enable secure agent communication.
     */
    String AGENT_SECURE_COMMUNICATION_KEY = "osgi.fx.agent.secure";

    /**
     * The property key for the custom {@link SSLContext} enabling secure agent communication.
     */
    String AGENT_SECURE_COMMUNICATION_SSL_CONTEXT_FILTER_KEY = "osgi.fx.agent.secure.sslcontext.filter";

    /**
     * The property key to enable agent logs
     */
    String TRACE_LOG_KEY = "osgi.fx.agent.logs.enabled";

    /**
     * The pattern for a server port specification: {@code [<interface>:]<port>} .
     */
    Pattern PORT_PATTERN = Pattern.compile("(?:([^:]+):)?(\\d+)");

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
     * Executes the specified terminal (CLI) command in a separate process.
     *
     * @param command the command to execute
     * @return the response
     */
    String execCliCommand(String command);

    /**
     * Abort the remote agent. The agent should send an event back and die. This is
     * an async method.
     */
    void abort() throws Exception;

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
    void gc();
}
