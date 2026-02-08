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
package com.osgifx.console.mcp.tool;

import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.Map;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.osgifx.console.mcp.McpTool;
import com.osgifx.console.mcp.McpToolSchema;
import com.osgifx.console.propertytypes.McpToolDef;
import com.osgifx.console.supervisor.Supervisor;

@Component(service = McpTool.class)
@McpToolDef(name = "fetch_log_snapshot", description = "Retrieves system logs as a Base64 encoded string. Provide 'fromTime' and 'toTime' (timestamps) for a specific range, OR 'count' for the last N logs. Time range takes precedence.")
public class GetLogSnapshotTool implements McpTool {

    @Reference(cardinality = OPTIONAL, policyOption = GREEDY)
    private volatile Supervisor supervisor;
    @Reference
    private LoggerFactory       factory;
    private FluentLogger        logger;

    @Activate
    void activate() {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
    }

    @Override
    public Map<String, Object> inputSchema() {
        return McpToolSchema.builder().arg("count", "integer", "number of logs (0 for all stored logs)")
                .arg("fromTime", "integer", "start timestamp (long)").arg("toTime", "integer", "end timestamp (long)")
                .build();
    }

    @Override
    public Object execute(final Map<String, Object> args) throws Exception {
        logger.atInfo().log("Executing GetLogSnapshotTool");
        final var agent = supervisor.getAgent();
        if (agent == null) {
            logger.atWarning().log("Agent is not connected");
            return new byte[0];
        }

        final var fromTimeObj = args.get("fromTime");
        final var toTimeObj   = args.get("toTime");

        if (fromTimeObj instanceof Number fromNumber && toTimeObj instanceof Number toNumber) {
            final var fromTime = fromNumber.longValue();
            final var toTime   = toNumber.longValue();

            final var snapshot = agent.getLogSnapshot(fromTime, toTime);
            logger.atInfo().log("Retrieved log snapshot (time range): %s bytes", snapshot.length);
            return snapshot;
        }

        // Default to count-based if time range is not provided
        final var countObj = args.get("count");
        final var count    = countObj instanceof Number number ? number.intValue() : 0;

        final var snapshot = agent.getLogSnapshot(count);
        logger.atInfo().log("Retrieved log snapshot (count): %s bytes", snapshot.length);
        return snapshot;
    }
}
