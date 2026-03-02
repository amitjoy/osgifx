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
package com.osgifx.console.api;

import org.osgi.annotation.versioning.ProviderType;

import javafx.beans.property.IntegerProperty;
import javafx.collections.ObservableList;

/**
 * Service for tracking active RPC calls globally across all UI tabs.
 * This is a singleton service that provides visibility into all ongoing
 * RPC operations, enabling Eclipse-style progress tracking in the status bar.
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * // Start tracking an RPC call
 * String trackerId = tracker.startRpc("getAllBundles", "Fetching all bundles");
 *
 * try {
 *     // Execute RPC call
 *     List&lt;XBundleDTO&gt; bundles = agent.getAllBundles();
 *
 *     // Mark as complete
 *     tracker.completeRpc(trackerId);
 * } catch (Exception e) {
 *     // Mark as failed
 *     tracker.failRpc(trackerId, e.getMessage());
 * }
 * </pre>
 *
 * @since 11.0
 */
@ProviderType
public interface RpcProgressTracker {

    /**
     * Registers an RPC call and returns a unique tracker ID.
     *
     * @param methodName the name of the RPC method (e.g., "getAllBundles")
     * @param description a human-readable description of the operation
     * @return a unique tracker ID for this RPC call
     */
    String startRpc(String methodName, String description);

    /**
     * Updates the progress of an RPC call.
     *
     * @param trackerId the tracker ID returned by {@link #startRpc}
     * @param progress the progress value (0.0 to 1.0 for determinate, -1 for indeterminate)
     */
    void updateProgress(String trackerId, double progress);

    /**
     * Updates the status message for an RPC call.
     *
     * @param trackerId the tracker ID returned by {@link #startRpc}
     * @param message the status message (e.g., "Compressing... 60%")
     */
    void updateMessage(String trackerId, String message);

    /**
     * Marks an RPC call as completed successfully.
     *
     * @param trackerId the tracker ID returned by {@link #startRpc}
     */
    void completeRpc(String trackerId);

    /**
     * Marks an RPC call as failed.
     *
     * @param trackerId the tracker ID returned by {@link #startRpc}
     * @param errorMessage the error message describing the failure
     */
    void failRpc(String trackerId, String errorMessage);

    /**
     * Returns an observable list of all active RPC calls.
     * UI components can bind to this list to display real-time progress.
     *
     * @return an observable list of {@link RpcCallInfo} objects
     */
    ObservableList<RpcCallInfo> getActiveRpcCalls();

    /**
     * Returns a property that tracks the number of active RPC calls.
     * UI components can bind to this property to show/hide progress indicators.
     *
     * @return an integer property representing the active RPC count
     */
    IntegerProperty activeRpcCountProperty();
}
