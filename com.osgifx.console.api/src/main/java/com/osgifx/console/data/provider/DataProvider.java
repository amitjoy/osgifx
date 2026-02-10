/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.data.provider;

import java.util.concurrent.CompletableFuture;

import org.osgi.annotation.versioning.ProviderType;

import com.osgifx.console.agent.dto.RuntimeDTO;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XBundleLoggerContextDTO;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XDmtNodeDTO;
import com.osgifx.console.agent.dto.XEventDTO;
import com.osgifx.console.agent.dto.XHealthCheckDTO;
import com.osgifx.console.agent.dto.XHeapUsageDTO;
import com.osgifx.console.agent.dto.XHttpComponentDTO;
import com.osgifx.console.agent.dto.XLogEntryDTO;
import com.osgifx.console.agent.dto.XMemoryInfoDTO;
import com.osgifx.console.agent.dto.XPropertyDTO;
import com.osgifx.console.agent.dto.XRoleDTO;
import com.osgifx.console.agent.dto.XServiceDTO;
import com.osgifx.console.agent.dto.XThreadDTO;

import javafx.collections.ObservableList;

/**
 * Provides utility methods for retrieving data from the runtime for display on the UI.
 */
@ProviderType
public interface DataProvider {

    /**
     * Retrieves information from the remote runtime based on the provided identifier.
     *
     * @param id the identifier for the specific information to retrieve, or {@code null} to retrieve all information
     * @param isAsync {@code true} if the information should be retrieved asynchronously, otherwise {@code false}
     */
    void retrieveInfo(String id, boolean isAsync);

    /**
     * Returns an observable list of bundles.
     *
     * @return the observable list of bundles
     */
    ObservableList<XBundleDTO> bundles();

    /**
     * Returns an observable list of packages.
     *
     * @return the observable list of packages
     */
    ObservableList<PackageDTO> packages();

    /**
     * Returns an observable list of services.
     *
     * @return the observable list of services
     */
    ObservableList<XServiceDTO> services();

    /**
     * Returns an observable list of components.
     *
     * @return the observable list of components
     */
    ObservableList<XComponentDTO> components();

    /**
     * Returns an observable list of configurations.
     *
     * @return the observable list of configurations
     */
    ObservableList<XConfigurationDTO> configurations();

    /**
     * Returns an observable list of events.
     *
     * @return the observable list of events
     */
    ObservableList<XEventDTO> events();

    /**
     * Returns an observable list of logs.
     *
     * @return the observable list of logs
     */
    ObservableList<XLogEntryDTO> logs();

    /**
     * Returns an observable list of properties.
     *
     * @return the observable list of properties
     */
    ObservableList<XPropertyDTO> properties();

    /**
     * Returns an observable list of threads.
     *
     * @return the observable list of threads
     */
    ObservableList<XThreadDTO> threads();

    /**
     * Returns an observable list of classloader leaks.
     *
     * @return the observable list of classloader leaks
     */
    ObservableList<XBundleDTO> leaks();

    /**
     * Returns HTTP runtime information, including lists of all servlets, resources, listeners, filters, and error
     * pages.
     *
     * @return the observable list of all HTTP components
     */
    ObservableList<XHttpComponentDTO> httpComponents();

    /**
     * Returns the Felix health checks.
     *
     * @return the observable list of all Felix health checks
     */
    ObservableList<XHealthCheckDTO> healthchecks();

    /**
     * Returns the roles (users and groups) stored in {@code UserAdmin}.
     *
     * @return the observable list of all roles
     */
    ObservableList<XRoleDTO> roles();

    /**
     * Returns the logger contexts.
     *
     * @return the observable list of all logger contexts
     */
    ObservableList<XBundleLoggerContextDTO> loggerContexts();

    /**
     * Returns the memory information of the remote runtime.
     *
     * @return a CompletableFuture containing the memory information
     */
    CompletableFuture<XMemoryInfoDTO> memory();

    /**
     * Returns the DMT node information of the specified node.
     *
     * @param rootURI the root URI of the node to read
     * @return a CompletableFuture containing the DMT node information
     */
    CompletableFuture<XDmtNodeDTO> readDmtNode(String rootURI);

    /**
     * Returns the runtime DTOs together.
     *
     * @return a CompletableFuture containing the runtime DTOs
     */
    CompletableFuture<RuntimeDTO> readRuntimeDTO();

    /**
     * Returns the heap usage information.
     *
     * @return a CompletableFuture containing the heap usage information
     */
    CompletableFuture<XHeapUsageDTO> heapUsage();
}
