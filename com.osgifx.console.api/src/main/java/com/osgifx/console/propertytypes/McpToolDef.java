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
package com.osgifx.console.propertytypes;

import org.osgi.service.component.annotations.ComponentPropertyType;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Component Property Type for defining an MCP Tool.
 * <p>
 * Apply this annotation to a {@link org.osgi.service.component.annotations.Component}
 * to automatically register the {@code mcp.tool.name} and {@code mcp.tool.description}
 * service properties.
 * <p>
 * This prevents naming conflicts when implementing the {@code McpTool} interface in the same class.
 * <p>
 * Example:
 * 
 * <pre>
 * &#64;Component
 * &#64;McpToolDef(name = "my_tool", description = "Everything")
 * public class MyTool implements McpTool { ... }
 * </pre>
 */
@ComponentPropertyType
@Target(TYPE)
@Retention(CLASS)
public @interface McpToolDef {

    /**
     * The unique name of the tool (e.g. "osgi_list_bundles").
     * <p>
     * Mapped to service property: {@code mcp.tool.name}
     *
     * @return the tool name
     */
    String name();

    /**
     * The description/prompt for the AI.
     * <p>
     * Mapped to service property: {@code mcp.tool.description}
     *
     * @return the tool description
     */
    String description();

    /**
     * Helper prefix for the property mapping.
     */
    String PREFIX_ = "mcp.tool.";
}