/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;

import com.osgifx.console.agent.dto.ConfigValue;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XPropertyDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.agent.dto.XServiceDTO;
import com.osgifx.console.agent.dto.XThreadDTO;
import com.osgifx.console.supervisor.Supervisor;

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

    /**
     * The property key to set the agent's port.
     */
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
     * @param startLevel the start level of the bundle
     * @return A Bundle DTO (cannot be {@code null})
     * @throws Exception if the bundle cannot be installed or updated
     */
    BundleDTO installWithData(String location, byte[] data, int startLevel) throws Exception;

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

    /*******************************************************************
     * Extended usages to manage runtime in all respects
     *******************************************************************/

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
     * @return the detailed information of all the registered DS service
     *         components, otherwise {@code empty} list if the remote runtime
     *         does not have SCR bundle installed
     */
    List<XComponentDTO> getAllComponents();

    /**
     * Get the detailed information of all the configurations
     * <p>
     * Note that, this is only possible if the remote runtime has ConfigAdmin
     * (CM) bundle installed.
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
     * Enables the component description by name
     *
     * @param name The name of the component description to enable.
     * @return the detailed information about the operation whether it succeeded
     *         or failed
     */
    XResultDTO enableComponentByName(String name);

    /**
     * Enables the component description by identifier
     *
     * @param id The id of the component description to enable.
     * @return the detailed information about the operation whether it succeeded
     *         or failed
     */
    XResultDTO enableComponentById(long id);

    /**
     * Disables the component description by name
     *
     * @param name The name of component description to disable.
     * @return the detailed information about the operation whether it succeeded
     *         or failed
     */
    XResultDTO disableComponentByName(String name);

    /**
     * Disables the component description by identifier
     *
     * @param id The id of component description to disable.
     * @return the detailed information about the operation whether it succeeded
     *         or failed
     */
    XResultDTO disableComponentById(long id);

    /**
     * Creates or updates the associated {@code Configuration} object with the
     * specified properties.
     * <p>
     * Note that, this is only possible if the remote runtime has ConfigAdmin
     * (CM) bundle installed.
     *
     * @param pid the configuration PID to update
     * @param newProperties the new properties to associate
     * @return the detailed information about the operation whether it succeeded
     *         or failed
     */
    XResultDTO createOrUpdateConfiguration(String pid, List<ConfigValue> newProperties);

    /**
     * Deletes the associated {@code Configuration} object that corresponds to
     * the specified {@code pid}.
     * <p>
     * Note that, this is only possible if the remote runtime has ConfigAdmin CM
     * bundle installed.
     *
     * @param pid The configuration PID to delete.
     * @return the detailed information about the operation whether it succeeded
     *         or failed
     */
    XResultDTO deleteConfiguration(String pid);

    /**
     * Create the {@code Configuration} object associated with the specified
     * {@code factoryPid} with the specified properties.
     * <p>
     * Note that, this is only possible if the remote runtime has ConfigAdmin
     * (CM) bundle installed.
     *
     * @param factoryPid the Configuration Factory PID
     * @param newProperties the new properties to associate
     * @return the detailed information about the operation whether it succeeded
     *         or failed
     */
    XResultDTO createFactoryConfiguration(String factoryPid, List<ConfigValue> newProperties);

    /**
     * Publish event synchronously (the method does not return until the event is processed)
     *
     * @param topic
     *            topic of the event to be published
     * @param properties
     *            data to be published with the event
     */
    void sendEvent(String topic, List<ConfigValue> properties);

    /**
     * Publish event asynchronously ((this method returns immediately))
     *
     * @param topic
     *            topic of the event to be published
     * @param properties
     *            data to be published with the event
     */
    void postEvent(String topic, List<ConfigValue> properties);

    /**
     * Returns the runtime information of the remote system
     *
     * @return the runtime information
     */
    Map<String, String> getRuntimeInfo();

    /**
     * Returns the set of registered Gogo commands
     * <p>
     * Note that, this is only possible if the remote runtime has Gogo bundle(s)
     * installed
     *
     * @return the set of registered Gogo commands, otherwise {@code empty} set
     *         if the remote runtime does not have Gogo bundle(s) installed
     */
    Set<String> getGogoCommands();

    /**
     * Returns the result from the specified agent extension.
     * <p>
     * <b>Note that,</b> the extension should be registered as a service that
     * implements {@link AgentExtension} and the service must provide a readable
     * name in its {@code agent.extension.name} service property.
     *
     * @param name the name of the extension
     * @param context the context for the extension to be provided for execution
     *            (note that, the map should also contain values supported by
     *            bnd's {@code Converter})
     * @return the value having the type supported by bnd's {@code Converter}
     * @see AgentExtension
     */
    Object executeExtension(String name, Map<String, Object> context);

    /**
     * Executes the specified terminal (CLI) command in a separate process.
     *
     * @param command the command to execute
     * @return the response
     */
    String exec(String command);

    /**
     * Returns the list of suspicious bundles causing probable classloader leaks
     *
     * @return the set of bundles (can be empty)
     */
    Set<XBundleDTO> getClassloaderLeaks();

    /**
     * Returns the HTTP runtime information
     *
     * @return the HTTP runtime information
     */
    RuntimeDTO getHttpRuntimeInfo();
}