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
package com.osgifx.console.application.rpc;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;

import com.osgifx.console.api.RpcCallInfo;
import com.osgifx.console.api.RpcProgressTracker;
import com.osgifx.console.api.RpcStatus;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Implementation of {@link RpcProgressTracker} that tracks all active RPC calls
 * globally across the application.
 *
 * @since 11.0
 */
@Component(service = RpcProgressTracker.class)
public class RpcProgressTrackerImpl implements RpcProgressTracker {

    private final ObservableList<RpcCallInfo> activeRpcCalls;
    private final ConcurrentHashMap<String, RpcCallInfo> rpcCallMap;
    private final IntegerProperty activeRpcCount;

    public RpcProgressTrackerImpl() {
        this.activeRpcCalls = FXCollections.observableArrayList();
        this.rpcCallMap = new ConcurrentHashMap<>();
        this.activeRpcCount = new SimpleIntegerProperty(0);
    }

    @Override
    public String startRpc(String methodName, String description) {
        final String trackerId = UUID.randomUUID().toString();
        final RpcCallInfo info = new RpcCallInfo(trackerId, methodName, description);

        rpcCallMap.put(trackerId, info);

        // Update UI on JavaFX thread
        Platform.runLater(() -> {
            activeRpcCalls.add(info);
            activeRpcCount.set(activeRpcCalls.size());
        });

        return trackerId;
    }

    @Override
    public void updateProgress(String trackerId, double progress) {
        final RpcCallInfo info = rpcCallMap.get(trackerId);
        if (info != null) {
            Platform.runLater(() -> info.setProgress(progress));
        }
    }

    @Override
    public void updateMessage(String trackerId, String message) {
        final RpcCallInfo info = rpcCallMap.get(trackerId);
        if (info != null) {
            Platform.runLater(() -> info.setMessage(message));
        }
    }

    @Override
    public void completeRpc(String trackerId) {
        final RpcCallInfo info = rpcCallMap.remove(trackerId);
        if (info != null) {
            Platform.runLater(() -> {
                info.setStatus(RpcStatus.COMPLETED);
                activeRpcCalls.remove(info);
                activeRpcCount.set(activeRpcCalls.size());
            });
        }
    }

    @Override
    public void failRpc(String trackerId, String errorMessage) {
        final RpcCallInfo info = rpcCallMap.remove(trackerId);
        if (info != null) {
            Platform.runLater(() -> {
                info.setStatus(RpcStatus.FAILED);
                info.setErrorMessage(errorMessage);
                activeRpcCalls.remove(info);
                activeRpcCount.set(activeRpcCalls.size());
            });
        }
    }

    @Override
    public ObservableList<RpcCallInfo> getActiveRpcCalls() {
        return FXCollections.unmodifiableObservableList(activeRpcCalls);
    }

    @Override
    public IntegerProperty activeRpcCountProperty() {
        return activeRpcCount;
    }
}
