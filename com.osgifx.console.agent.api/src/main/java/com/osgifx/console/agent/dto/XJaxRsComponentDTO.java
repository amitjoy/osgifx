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
 * Represents an OSGi JAX-RS component within the framework.
 */
public class XJaxRsComponentDTO extends DTO {

    /** The name of the JAX-RS component. */
    public String name;

    /** The service ID associated with this component. */
    public long serviceId;

    /** The type of the component (e.g. Application, Extension, Resource). */
    public String type;

    /** The base URI for Application types. */
    public String base;

    /** The extension types mapping for Extension types. */
    public List<String> extensionTypes;

    /** Consumes media types. */
    public List<String> consumes;

    /** Produces media types. */
    public List<String> produces;

    /** The NameBinding annotations that apply to this extension. */
    public List<String> nameBindings;

    /** Resource methods mapped to resources. */
    public List<XResourceMethodInfoDTO> resourceMethods;

    /** True if the component failed to register or be used. */
    public boolean isFailed;

    /** The reason for the failure, if failed. */
    public int failureReason;
}
