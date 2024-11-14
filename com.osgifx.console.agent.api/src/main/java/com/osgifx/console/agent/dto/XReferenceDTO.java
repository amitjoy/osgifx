/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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

import org.osgi.dto.DTO;

/**
 * Represents a reference to a service or a component within the OSGi framework.
 * This class extends the {@link DTO} class to provide a standardized data transfer 
 * object for information related to component references.
 * <p>
 * The {@code XReferenceDTO} class includes details such as the reference name, 
 * interface, cardinality, policy, and methods for binding and unbinding the reference. 
 * It is used to transfer reference data between different components or systems 
 * in a consistent format.
 * </p>
 */
public class XReferenceDTO extends DTO {

    // Basic Reference Information
    /** The name of the reference. */
    public String name;

    /** The fully qualified name of the service interface for this reference. */
    public String interfaceName;

    // Cardinality and Policy Information
    /** The cardinality of the reference (e.g., "1..1", "0..n"). */
    public String cardinality;

    /** The policy for this reference (e.g., "static", "dynamic"). */
    public String policy;

    /** The policy option for this reference (e.g., "greedy", "reluctant"). */
    public String policyOption;

    // Target Filter Information
    /** The target filter expression for this reference. */
    public String target;

    // Binding and Lifecycle Methods
    /** The method name used to bind the reference. */
    public String bind;

    /** The method name used to unbind the reference. */
    public String unbind;

    /** The method name used to update the reference. */
    public String updated;

    // Field and Injection Information
    /** The field name used for injecting this reference. */
    public String field;

    /** The field option for this reference (e.g., "replace", "update"). */
    public String fieldOption;

    /** The scope of the reference (e.g., "singleton", "prototype"). */
    public String scope;

    // Parameter and Collection Information
    /** The parameter index associated with this reference in a constructor or method. */
    public Integer parameter;

    /** The collection type for this reference if it is a collection (e.g., "list", "set"). */
    public String collectionType;

}