/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
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
import java.util.Map;

import org.osgi.dto.DTO;

/**
 * Represents the details and runtime state of a component within the OSGi 
 * framework. This class extends the {@link DTO} class to provide a standardized 
 * data transfer object for component-related information.
 * <p>
 * The {@code XComponentDTO} class contains information such as the component's 
 * identifier, name, state, properties, references, lifecycle methods, and any 
 * related errors or failures. It is used to transfer component data between 
 * different components or systems in a consistent format.
 * </p>
 */
public class XComponentDTO extends DTO {

    /** The unique identifier of the component. */
    public long id;

    /** The name of the component. */
    public String name;

    /** The current state of the component (e.g., "active", "inactive"). */
    public String state;

    /** The symbolic name of the bundle that registers this component. */
    public String registeringBundle;

    /** The unique identifier of the bundle that registers this component. */
    public long registeringBundleId;

    /** The name of the factory that can create instances of this component, if any. */
    public String factory;

    /** The scope of the component (e.g., "singleton", "bundle"). */
    public String scope;

    /** The fully qualified name of the component's implementation class. */
    public String implementationClass;

    /** The configuration policy of the component (e.g., "required", "optional"). */
    public String configurationPolicy;

    /** A list of service interfaces provided by this component. */
    public List<String> serviceInterfaces;

    /** A list of configuration PIDs associated with this component. */
    public List<String> configurationPid;

    /** A map of properties associated with this component. */
    public Map<String, String> properties;

    /** A list of references that this component requires. */
    public List<XReferenceDTO> references;

    /** The failure reason, if the component has failed to activate or perform an operation. */
    public String failure;

    /** The name of the method used to activate the component. */
    public String activate;

    /** The name of the method used to deactivate the component. */
    public String deactivate;

    /** The name of the method used to modify the component's configuration. */
    public String modified;

    /** A list of satisfied references for this component. */
    public List<XSatisfiedReferenceDTO> satisfiedReferences;

    /** A list of unsatisfied references for this component. */
    public List<XUnsatisfiedReferenceDTO> unsatisfiedReferences;

}