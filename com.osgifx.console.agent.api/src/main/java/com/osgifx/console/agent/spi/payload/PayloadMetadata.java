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
 * Metadata about a large payload file.
 *
 * @since 11.0
 */
public final class PayloadMetadata {

    /**
     * The filename (e.g., "heapdump-2026-03-02-12-30-45.hprof.gz")
     */
    public String filename;

    /**
     * The size of the file in bytes
     */
    public long sizeBytes;

    /**
     * The content type (e.g., "application/x-hprof-gzip" or "application/json")
     */
    public String contentType;

    /**
     * The type of payload (HEAPDUMP / SNAPSHOT / THREADDUMP)
     */
    public PayloadType type;

    /**
     * The timestamp when the payload was created (epoch milliseconds)
     */
    public long timestamp;

    /**
     * Default constructor for DTO deserialization
     */
    public PayloadMetadata() {
    }

    /**
     * Constructor with all fields
     */
    public PayloadMetadata(String filename, long sizeBytes, String contentType, PayloadType type, long timestamp) {
        this.filename    = filename;
        this.sizeBytes   = sizeBytes;
        this.contentType = contentType;
        this.type        = type;
        this.timestamp   = timestamp;
    }
}
