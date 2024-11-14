/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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

import org.osgi.dto.DTO;

/**
 * A data transfer object (DTO) representing a node in the OSGi Device Management
 * Tree (DMT).
 * 
 * It encapsulates information about the URI, value, data format, creation timestamp,
 * and child nodes of the DMT node.
 */
public class XDmtNodeDTO extends DTO {

    /** The URI of the DMT node */
    public String uri;

    /** The value associated with the DMT node */
    public String value;

    /** The data format type of the DMT node */
    public DmtDataType format;

    /** The timestamp when the DMT node was created */
    public String createdAt;

    /** The list of child nodes belonging to the DMT node */
    public List<XDmtNodeDTO> children;

}
