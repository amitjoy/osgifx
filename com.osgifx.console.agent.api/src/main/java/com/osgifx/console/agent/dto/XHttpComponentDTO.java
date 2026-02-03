/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.dto;

import java.util.List;

import org.osgi.dto.DTO;

/**
 * Represents an HTTP component within the OSGi framework. This class extends 
 * the {@link DTO} class to provide a standardized data transfer object for HTTP 
 * component-related information.
 * <p>
 * The {@code XHttpComponentDTO} class contains details about various types of 
 * HTTP components, such as servlets, filters, listeners, and error pages. It 
 * includes information like component name, service ID, context details, supported 
 * patterns, and specific settings related to each component type.
 * </p>
 */
public class XHttpComponentDTO extends DTO {

    // General Component Information
    /** The name of the HTTP component. */
    public String name;

    /** Indicates whether asynchronous processing is supported by this component. */
    public boolean asyncSupported;

    /** The service ID associated with this HTTP component. */
    public long serviceId;

    /** A list of URL patterns that this HTTP component is mapped to. */
    public List<String> patterns;

    /** The name of the servlet context in which this component is registered. */
    public String contextName;

    /** The context path for the servlet context in which this component is registered. */
    public String contextPath;

    /** The service ID of the servlet context associated with this component. */
    public long contextServiceId;

    /** The type of the HTTP component (e.g., "servlet", "filter", "listener"). */
    public String type;

    // Error Page Information
    /** A list of exception classes for which this component serves as an error page. */
    public List<String> exceptions;

    /** A list of HTTP error codes for which this component serves as an error page. */
    public List<Long> errorCodes;

    // Filter Information
    /** A list of servlet names that this filter is applied to. */
    public List<String> servletNames;

    /** A list of regular expressions for URL patterns to which this filter is applied. */
    public List<String> regexs;

    /** A list of dispatcher types that this filter uses (e.g., "REQUEST", "FORWARD"). */
    public List<String> dispatcher;

    // Listener Information
    /** A list of event types handled by this listener. */
    public List<String> types;

    // Resource Information
    /** The prefix used for serving static resources by this component. */
    public String prefix;

    // Servlet Information
    /** Information about the servlet, such as its description or version. */
    public String servletInfo;

}