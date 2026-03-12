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
package com.osgifx.console.agent.spi.payload;

/**
 * Result of a large payload handling operation.
 *
 * @since 11.0
 */
public final class PayloadHandlerResult {

    /**
     * Whether the handling operation was successful
     */
    public boolean success;

    /**
     * The location where the payload was stored.
     * This can be a download URL, file path, or storage identifier.
     * (e.g., "https://s3.amazonaws.com/bucket/heapdump.hprof.gz" or "/var/heapdumps/dump.hprof.gz")
     */
    public String location;

    /**
     * Error message if the operation failed (null if successful)
     */
    public String errorMessage;

    /**
     * Duration of the handling operation in milliseconds
     */
    public long durationMs;

    /**
     * Default constructor for DTO deserialization
     */
    public PayloadHandlerResult() {
    }

    /**
     * Constructor with all fields
     */
    public PayloadHandlerResult(boolean success, String location, String errorMessage, long durationMs) {
        this.success      = success;
        this.location     = location;
        this.errorMessage = errorMessage;
        this.durationMs   = durationMs;
    }
}
