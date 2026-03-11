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
package com.osgifx.console.agent.spi;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Service interface for handling large payloads (heapdumps, snapshots, threaddumps)
 * that exceed RPC transport limits. Implementations can upload to HTTP servers,
 * SFTP, S3, NFS, or any other storage mechanism.
 *
 * <p>
 * This is a {@code @ConsumerType} API - the remote runtime provides implementations,
 * and the framework only defines the interface.
 *
 * <p>
 * <b>Example Implementation:</b>
 * 
 * <pre>
 * {@code @Component}
 * public class S3PayloadHandler implements LargePayloadHandler {
 *     {@code @Override}
 *     public PayloadHandlerResult handle(String localPath, PayloadMetadata metadata) {
 *         // Upload to S3
 *         String s3Url = s3Client.upload(localPath, metadata.filename);
 *         // Delete local file after successful upload
 *         new File(localPath).delete();
 *         return new PayloadHandlerResult(true, s3Url, null, uploadDuration);
 *     }
 *
 *     {@code @Override}
 *     public long getMaxPayloadSize() {
 *         return 5L * 1024 * 1024 * 1024; // 5 GB
 *     }
 *
 *     {@code @Override}
 *     public String getHandlerName() {
 *         return "Corporate S3 Upload";
 *     }
 *
 *     {@code @Override}
 *     public String getDescription() {
 *         return "Uploads to corporate S3 bucket";
 *     }
 * }
 * </pre>
 *
 * @since 11.0
 */
@ConsumerType
public interface LargePayloadHandler {

    /**
     * Handles a large payload file from the agent.
     * The implementation decides where and how to store/upload the file.
     *
     * <p>
     * The local file at {@code localPath} should be deleted after successful
     * handling to avoid filling up disk space.
     *
     * @param localPath the absolute path to the local file on the agent device
     * @param metadata metadata about the payload (filename, size, type, etc.)
     * @return the result of the handling operation
     * @throws Exception if the handling operation fails
     */
    PayloadHandlerResult handle(String localPath, PayloadMetadata metadata) throws Exception;

    /**
     * Returns the maximum payload size this handler can process, in bytes.
     *
     * @return the maximum payload size in bytes, or {@code Long.MAX_VALUE} for unlimited
     */
    long getMaxPayloadSize();

    /**
     * Returns the handler name for display in the UI.
     *
     * @return a human-readable handler name (e.g., "Corporate S3 Upload")
     */
    String getHandlerName();

    /**
     * Returns a description of what this handler does.
     *
     * @return a human-readable description (e.g., "Uploads to corporate S3 bucket")
     */
    String getDescription();
}
