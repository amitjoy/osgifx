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

import org.osgi.annotation.versioning.ProviderType;

/**
 * The {@code AgentSnapshot} interface defines the methods for retrieving
 * compressed binary snapshots of the agent's state.
 * <p>
 * These methods are designed to be highly performance-efficient by returning
 * pre-serialized and compressed (LZ4) byte arrays, minimizing heap occupancy
 * and serialization overhead on the agent side.
 * <p>
 * All snapshot methods return pre-encoded and LZ4-compressed {@code byte[]} that can be
 * decoded using {@link com.osgifx.console.agent.rpc.codec.SnapshotDecoder}.
 *
 * @since 12.0
 */
@ProviderType
public interface AgentSnapshot {

    /**
     * Returns a compressed binary snapshot of all bundles.
     *
     * @return the compressed byte array containing the bundle information
     */
    byte[] bundles();

    /**
     * Returns a compressed binary snapshot of all components.
     *
     * @return the compressed byte array containing the component information
     */
    byte[] components();

    /**
     * Returns a compressed binary snapshot of all services.
     *
     * @return the compressed byte array containing the service information
     */
    byte[] services();

    /**
     * Returns a compressed binary snapshot of all configurations.
     *
     * @return the compressed byte array containing the configuration information
     */
    byte[] configurations();

    /**
     * Returns a compressed binary snapshot of all properties.
     *
     * @return the compressed byte array containing the property information
     */
    byte[] properties();

    /**
     * Returns a compressed binary snapshot of all threads.
     *
     * @return the compressed byte array containing the thread information
     */
    byte[] threads();

    /**
     * Returns a compressed binary snapshot of all roles.
     *
     * @return the compressed byte array containing the role information
     */
    byte[] roles();

    /**
     * Returns a compressed binary snapshot of all health checks.
     *
     * @return the compressed byte array containing the health check information
     */
    byte[] healthChecks();

    /**
     * Returns a compressed binary snapshot of all HTTP components.
     *
     * @return the compressed byte array containing the HTTP component information
     */
    byte[] httpComponents();

    /**
     * Returns a compressed binary snapshot of all JAX-RS components.
     *
     * @return the compressed byte array containing the JAX-RS component information
     */
    byte[] jaxRsComponents();

    /**
     * Returns a compressed binary snapshot of all CDI containers.
     *
     * @return the compressed byte array containing the CDI container information
     */
    byte[] cdiContainers();

    /**
     * Returns a compressed binary snapshot of all bundle logger contexts.
     *
     * @return the compressed byte array containing the bundle logger context information
     */
    byte[] bundleLoggerContexts();

    /**
     * Returns a compressed binary snapshot of the runtime information.
     *
     * @return the compressed byte array containing the runtime information
     */
    byte[] runtime();

    /**
     * Returns a compressed binary snapshot of the heap usage.
     *
     * @return the compressed byte array containing the heap usage information
     */
    byte[] heapUsage();

    /**
     * Returns a compressed binary snapshot of the classloader leaks.
     *
     * @return the compressed byte array containing the classloader leaks information
     */
    byte[] leaks();

    /**
     * Returns a compressed binary snapshot of the runtime capabilities.
     *
     * @return the compressed byte array containing the runtime capabilities information
     */
    byte[] runtimeCapabilities();
}
