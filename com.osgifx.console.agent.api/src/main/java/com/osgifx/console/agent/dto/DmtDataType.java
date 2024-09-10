/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.dto;

/**
 * Enum representing the different data types that can be used in a DMT (Device 
 * Management Tree) structure within the OSGi framework. These types are utilized 
 * to define the format and nature of data stored in or retrieved from a DMT node.
 */
public enum DmtDataType {

    /** Base64-encoded binary data. */
    BASE64,
    
    /** Binary data. */
    BINARY,
    
    /** Boolean value (true or false). */
    BOOLEAN,
    
    /** Date value in a standardized format. */
    DATE,
    
    /** Floating-point numeric value. */
    FLOAT,
    
    /** Integer numeric value. */
    INTEGER,
    
    /** Null value, indicating the absence of data. */
    NULL,
    
    /** String value. */
    STRING,
    
    /** Time value in a standardized format. */
    TIME,
    
    /** XML-formatted data. */
    XML,
    
    /** Long numeric value. */
    LONG,
    
    /** Combined date and time value in a standardized format. */
    DATE_TIME
}