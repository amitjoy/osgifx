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
 *******************************************************************************/
package com.osgifx.console.mcp.tool;

import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.osgifx.console.agent.dto.RemoteServiceDirection;
import com.osgifx.console.agent.dto.XRemoteServiceDTO;
import com.osgifx.console.agent.rpc.codec.BinaryCodec;
import com.osgifx.console.agent.rpc.codec.SnapshotDecoder;
import com.osgifx.console.mcp.McpTool;
import com.osgifx.console.mcp.McpToolSchema;
import com.osgifx.console.propertytypes.McpToolDef;
import com.osgifx.console.supervisor.Supervisor;

@Component
@McpToolDef(name = "list_remote_services", description = "Lists OSGi Remote Services Admin endpoints (exports and imports). Filter by direction.")
public class ListRemoteServicesTool implements McpTool {

    @Reference(cardinality = OPTIONAL, policyOption = GREEDY)
    private volatile Supervisor supervisor;
    @Reference
    private LoggerFactory       factory;
    private FluentLogger        logger;
    private SnapshotDecoder     decoder;

    @Activate
    void activate(final BundleContext context) {
        logger  = FluentLogger.of(factory.createLogger(getClass().getName()));
        decoder = new SnapshotDecoder(new BinaryCodec(context));
    }

    @Override
    public Map<String, Object> inputSchema() {
        return McpToolSchema.builder()
                .arg("direction", "string", "Optional filter: 'IMPORT' or 'EXPORT'. Returns all if not provided.")
                .build();
    }

    @Override
    public Object execute(final Map<String, Object> args) throws Exception {
        logger.atInfo().log("Executing ListRemoteServicesTool");
        final var agent = supervisor.getAgent();
        if (agent == null) {
            logger.atWarning().log("Agent is not connected");
            return Collections.emptyList();
        }

        final var dirFilter = (String) args.get("direction");

        final byte[] snapshot = agent.remoteServices();
        var          data     = decoder.decodeList(snapshot, XRemoteServiceDTO.class);

        if (dirFilter != null && !dirFilter.isBlank()) {
            final RemoteServiceDirection direction = RemoteServiceDirection.valueOf(dirFilter.toUpperCase());
            data = data.stream().filter(d -> d.direction == direction).collect(Collectors.toList());
        }

        return data;
    }
}
