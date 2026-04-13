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
package com.osgifx.console.mcp.server;

import java.util.List;

public final class McpProtocol {

    private McpProtocol() {
    }

    // Ordered from newest to oldest
    public static final List<String> SUPPORTED_VERSIONS = List.of("2025-11-25", "2025-03-26", "2024-11-05");

    /**
     * Negotiates the protocol version.
     * If the client's requested version is supported, it is returned.
     * Otherwise, it falls back to the server's highest supported version.
     */
    public static String negotiateVersion(final String requestedVersion) {
        if (requestedVersion != null && SUPPORTED_VERSIONS.contains(requestedVersion)) {
            return requestedVersion;
        }
        return SUPPORTED_VERSIONS.get(0);
    }
}
