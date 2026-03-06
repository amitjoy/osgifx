/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.mcp.tool;

import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.osgifx.console.decompiler.FxDecompiler;
import com.osgifx.console.mcp.McpTool;
import com.osgifx.console.mcp.McpToolSchema;
import com.osgifx.console.propertytypes.McpToolDef;
import com.osgifx.console.supervisor.Supervisor;

@Component(service = McpTool.class)
@McpToolDef(name = "decompile_class", description = "Decompiles a Java class from a remote OSGi bundle")
public final class DecompileClassTool implements McpTool {

    @Reference
    private FxDecompiler decompiler;

    @Reference(cardinality = OPTIONAL, policyOption = GREEDY)
    private volatile Supervisor supervisor;

    @Override
    public Map<String, Object> inputSchema() {
        return McpToolSchema.builder().arg("bundleId", "number", "The ID of the bundle containing the class")
                .arg("className", "string", "The fully qualified class name (e.g., com.example.MyClass)")
                .optionalArg("useClassloader", "boolean",
                        "If true, resolves the class via the bundle's classloader (supports dynamic imports/fragments). If false, reads directly from the bundle JAR entry.")
                .build();
    }

    @Override
    public Object execute(final Map<String, Object> args) throws Exception {
        final var agent = supervisor == null ? null : supervisor.getAgent();
        if (agent == null) {
            return "Error: Agent is not connected";
        }

        final Number bundleIdNum = (Number) args.get("bundleId");
        if (bundleIdNum == null) {
            return "Error: bundleId is required";
        }
        final long bundleId = bundleIdNum.longValue();

        final String className = (String) args.get("className");
        if (className == null || className.trim().isEmpty()) {
            return "Error: className is required";
        }

        final Boolean useClassloaderBool = (Boolean) args.get("useClassloader");
        final boolean useClassloader     = useClassloaderBool != null ? useClassloaderBool : false;

        final String classPath = className.replace('.', '/') + ".class";
        final byte[] classBytes;

        try {
            if (useClassloader) {
                classBytes = agent.getBundleResourceBytes(bundleId, classPath);
            } else {
                classBytes = agent.getBundleEntryBytes(bundleId, classPath);
            }
        } catch (Exception e) {
            return "Error retrieving class bytes from remote agent: " + e.getMessage();
        }

        if (classBytes == null) {
            return "Error: Class not found in bundle " + bundleId + " at path " + classPath;
        }

        try {
            return decompiler.decompile(classBytes, className);
        } catch (Exception e) {
            return "Error decompiling class: " + e.getMessage();
        }
    }
}
