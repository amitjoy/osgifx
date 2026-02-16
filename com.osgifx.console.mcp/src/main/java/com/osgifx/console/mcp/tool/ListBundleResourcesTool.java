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

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.osgifx.console.mcp.McpTool;
import com.osgifx.console.mcp.McpToolSchema;
import com.osgifx.console.propertytypes.McpToolDef;
import com.osgifx.console.supervisor.Supervisor;
import org.osgi.framework.wiring.BundleWiring;

@Component(service = McpTool.class)
@McpToolDef(name = "list_bundle_resources", description = "Finds resources in the bundle's classpath (includes imports)")
public class ListBundleResourcesTool implements McpTool {

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
        return McpToolSchema.builder().arg("bundleId", "integer", "The ID of the bundle")
                .arg("path", "string", "The path to start searching (default: '/')")
                .arg("pattern", "string", "The glob pattern to search for (default: '*')")
                .arg("local", "boolean", "Searches only local bundle, no imports (default: false)")
                .arg("recursive", "boolean", "Recursively search subdirectories (default: true)").build();
    }

    @Override
    public Object execute(final Map<String, Object> args) throws Exception {
        logger.atInfo().log("Executing ListBundleResourcesTool");
        final var agent = supervisor.getAgent();
        if (agent == null) {
            logger.atWarning().log("Agent is not connected");
            return Collections.emptyList();
        }

        final var bundleId  = ((Number) args.get("bundleId")).longValue();
        final var path      = (String) args.getOrDefault("path", "/");
        final var pattern   = (String) args.getOrDefault("pattern", "*");
        final var local     = (boolean) args.getOrDefault("local", false);
        final var recursive = (boolean) args.getOrDefault("recursive", true);

        var options = 0;
        if (recursive) {
            options |= BundleWiring.LISTRESOURCES_RECURSE;
        }
        if (local) {
            options |= BundleWiring.LISTRESOURCES_LOCAL;
        }

        return agent.listBundleResources(bundleId, path, pattern, options);
    }
}
