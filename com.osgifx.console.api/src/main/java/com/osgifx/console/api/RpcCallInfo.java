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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Information about an active RPC call.
 * This class uses JavaFX properties to enable UI binding.
 *
 * @since 11.0
 */
public final class RpcCallInfo {

    private final String id;
    private final String methodName;
    private final long startTime;
    private final StringProperty description;
    private final DoubleProperty progress;
    private final StringProperty message;
    private final ObjectProperty<RpcStatus> status;
    private final StringProperty errorMessage;

    /**
     * Creates a new RPC call info object.
     *
     * @param id unique identifier for this RPC call
     * @param methodName the name of the RPC method
     * @param description human-readable description
     */
    public RpcCallInfo(String id, String methodName, String description) {
        this.id = id;
        this.methodName = methodName;
        this.startTime = System.currentTimeMillis();
        this.description = new SimpleStringProperty(description);
        this.progress = new SimpleDoubleProperty(-1.0); // Indeterminate by default
        this.message = new SimpleStringProperty("");
        this.status = new SimpleObjectProperty<>(RpcStatus.RUNNING);
        this.errorMessage = new SimpleStringProperty(null);
    }

    public String getId() {
        return id;
    }

    public String getMethodName() {
        return methodName;
    }

    public long getStartTime() {
        return startTime;
    }

    public String getDescription() {
        return description.get();
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    public double getProgress() {
        return progress.get();
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress.set(progress);
    }

    public String getMessage() {
        return message.get();
    }

    public StringProperty messageProperty() {
        return message;
    }

    public void setMessage(String message) {
        this.message.set(message);
    }

    public RpcStatus getStatus() {
        return status.get();
    }

    public ObjectProperty<RpcStatus> statusProperty() {
        return status;
    }

    public void setStatus(RpcStatus status) {
        this.status.set(status);
    }

    public String getErrorMessage() {
        return errorMessage.get();
    }

    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage.set(errorMessage);
    }

    /**
     * Returns the duration of this RPC call in milliseconds.
     *
     * @return duration in milliseconds
     */
    public long getDurationMs() {
        return System.currentTimeMillis() - startTime;
    }
}
