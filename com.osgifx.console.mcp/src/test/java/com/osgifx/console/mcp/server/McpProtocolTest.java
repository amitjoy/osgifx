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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class McpProtocolTest {

    @Test
    public void testSupportedVersionsContainsLatest() {
        assertTrue(McpProtocol.SUPPORTED_VERSIONS.contains("2025-11-25"));
        assertTrue(McpProtocol.SUPPORTED_VERSIONS.contains("2025-03-26"));
        assertTrue(McpProtocol.SUPPORTED_VERSIONS.contains("2024-11-05"));
    }

    @Test
    public void testSupportedVersionsOrderedNewestFirst() {
        assertEquals("2025-11-25", McpProtocol.SUPPORTED_VERSIONS.get(0));
        assertEquals("2025-03-26", McpProtocol.SUPPORTED_VERSIONS.get(1));
        assertEquals("2024-11-05", McpProtocol.SUPPORTED_VERSIONS.get(2));
    }

    @Test
    public void testNegotiateVersionReturnsSupportedVersion() {
        assertEquals("2025-03-26", McpProtocol.negotiateVersion("2025-03-26"));
        assertEquals("2024-11-05", McpProtocol.negotiateVersion("2024-11-05"));
        assertEquals("2025-11-25", McpProtocol.negotiateVersion("2025-11-25"));
    }

    @Test
    public void testNegotiateVersionFallsBackToLatestForUnknown() {
        assertEquals("2025-11-25", McpProtocol.negotiateVersion("1.0.0"));
        assertEquals("2025-11-25", McpProtocol.negotiateVersion("unknown"));
    }

    @Test
    public void testNegotiateVersionFallsBackToLatestForNull() {
        assertEquals("2025-11-25", McpProtocol.negotiateVersion(null));
    }
}
