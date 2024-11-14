/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package com.osgifx.console.agent.dto;

import java.util.List;

import org.osgi.dto.DTO;

/**
 * A data transfer object (DTO) representing an attribute definition.
 * 
 * It encapsulates information about the attribute's ID, name, description,
 * data type, cardinality, option values, and default value(s).
 */
public class XAttributeDefDTO extends DTO {

    /** The ID of the attribute definition */
    public String id;

    /** The name of the attribute definition */
    public String name;

    /** The description of the attribute definition */
    public String description;

    /** The data type of the attribute definition */
    public int type;

    /** The cardinality (number of values) allowed for the attribute */
    public int cardinality;

    /** The list of option values available for the attribute */
    public List<String> optionValues;

    /** The default value(s) assigned to the attribute */
    public List<String> defaultValue;

}
