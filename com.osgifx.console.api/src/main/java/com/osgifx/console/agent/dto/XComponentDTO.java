/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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
package com.osgifx.console.agent.dto;

import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

public class XComponentDTO extends DTO {

    public String                         id;
    public String                         name;
    public String                         state;
    public String                         registeringBundle;
    public long                           registeringBundleId;
    public String                         factory;
    public String                         scope;
    public String                         implementationClass;
    public String                         configurationPolicy;
    public List<String>                   serviceInterfaces;
    public List<String>                   configurationPid;
    public Map<String, String>            properties;
    public List<XReferenceDTO>            references;
    public String                         failure;
    public String                         activate;
    public String                         deactivate;
    public String                         modified;
    public List<XSatisfiedReferenceDTO>   satisfiedReferences;
    public List<XUnsatisfiedReferenceDTO> unsatisfiedReferences;

}
