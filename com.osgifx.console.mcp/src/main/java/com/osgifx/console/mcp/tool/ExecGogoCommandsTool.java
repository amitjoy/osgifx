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
import java.util.Set;

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
@McpToolDef(name = "osgi_exec_gogo_commands", description = "Executes a Gogo shell command. "
        + "Use this for diagnostics (e.g., 'scr:info', 'inspect'). "
        + "WARNING: Do not use for system state changes (stop/update) unless explicitly asked.")
public class ExecGogoCommandsTool implements McpTool {

    // Blacklist dangerous commands to prevent accidental suicide
    private static final Set<String> BLOCKED_COMMANDS = Set.of("shutdown", "stop 0", "update 0", "rm ", "bundle:update",
            "framework:stop");

    @Reference(cardinality = OPTIONAL, policyOption = GREEDY)
    private volatile Supervisor supervisor;

    @Reference
    private LoggerFactory factory;
    private FluentLogger  logger;

    @Activate
    void activate() {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
    }

    @Override
    public Map<String, Object> inputSchema() {
        return McpToolSchema.builder().arg("command", "string", "The Gogo command to execute (e.g., 'lb', 'scr:list').")
                .build();
    }

    @Override
    public Object execute(final Map<String, Object> args) throws Exception {
        final var command = (String) args.get("command");

        logger.atInfo().log("Executing Gogo command: %s", command);

        final var agent = supervisor.getAgent();
        if (agent == null) {
            logger.atWarning().log("Agent is not connected");
            return "Error: Agent is not connected";
        }

        if (isDangerous(command)) {
            logger.atWarning().log("Blocked dangerous command: %s", command);
            return "Error: Command blocked by safety policy. " + "State-changing operations are restricted.";
        }

        try {
            final var result = agent.execGogoCommand(command);
            return result == null || result.isBlank() ? "Command executed (no output)." : result;
        } catch (final Exception e) {
            logger.atError().withException(e).log("Error executing Gogo command");
            return "Error occurred: " + e.getMessage();
        }
    }

    private boolean isDangerous(final String cmd) {
        if (cmd == null) {
            return true;
        }
        final String lower = cmd.toLowerCase();
        return BLOCKED_COMMANDS.stream().anyMatch(lower::contains);
    }
}
