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

import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.mcp.McpTool;
import com.osgifx.console.mcp.McpToolSchema;
import com.osgifx.console.propertytypes.McpToolDef;
import com.osgifx.console.supervisor.Supervisor;

@Component(service = McpTool.class)
@McpToolDef(name = "update_logger_context", description = "Updates the Logger Context for a bundle (BSN) with a map of logger names to log levels.")
public class UpdateLoggerContextTool implements McpTool {

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
        return McpToolSchema.builder().arg("bsn", "string", "The Bundle Symbolic Name (BSN)")
                .arg("logLevels", "object", "Map of logger names to log levels (e.g. ROOT=DEBUG)").build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object execute(final Map<String, Object> args) throws Exception {
        logger.atInfo().log("Executing UpdateLoggerContextTool");
        final var agent = supervisor.getAgent();
        if (agent == null) {
            logger.atWarning().log("Agent is not connected");
            return "Agent is not connected";
        }

        final var bsn       = (String) args.get("bsn");
        final var logLevels = (Map<String, Object>) args.get("logLevels");

        // Convert Map<String, Object> to Map<String, String>
        final Map<String, String> levels = logLevels.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));

        final var result = agent.updateBundleLoggerContext(bsn, levels);

        if (result.result == XResultDTO.ERROR) {
            return "Error updating logger context for '" + bsn + "': " + result.response;
        }
        return "Logger context for '" + bsn + "' updated successfully";
    }
}
