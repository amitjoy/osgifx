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
package com.osgifx.console.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class McpToolSchemaTest {

    @Test
    public void builderCreatesNonNullSchema() {
        assertNotNull(McpToolSchema.builder());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void argAddsToPropertiesAndRequired() {
        Map<String, Object> schema = McpToolSchema.builder().arg("testArg", "string", "A test argument").build();
        
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        List<String> required = (List<String>) schema.get("required");
        
        assertTrue(properties.containsKey("testArg"));
        assertTrue(required.contains("testArg"));
        
        Map<String, Object> argProps = (Map<String, Object>) properties.get("testArg");
        assertEquals("string", argProps.get("type"));
        assertEquals("A test argument", argProps.get("description"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void optionalArgNotInRequired() {
        Map<String, Object> schema = McpToolSchema.builder().optionalArg("optArg", "integer", "Optional arg").build();
        
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        List<String> required = (List<String>) schema.get("required");
        
        assertTrue(properties.containsKey("optArg"));
        assertFalse(required.contains("optArg"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void argArraySetsTypeToArray() {
        Map<String, Object> schema = McpToolSchema.builder().argArray("arrArg", "string", "Array arg").build();
        
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        List<String> required = (List<String>) schema.get("required");
        
        assertTrue(properties.containsKey("arrArg"));
        assertTrue(required.contains("arrArg"));
        
        Map<String, Object> argProps = (Map<String, Object>) properties.get("arrArg");
        assertEquals("array", argProps.get("type"));
        Map<String, Object> items = (Map<String, Object>) argProps.get("items");
        assertEquals("string", items.get("type"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void optionalArgArrayNotInRequired() {
        Map<String, Object> schema = McpToolSchema.builder().optionalArgArray("optArr", "number", "Optional array").build();
        
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        List<String> required = (List<String>) schema.get("required");
        
        assertTrue(properties.containsKey("optArr"));
        assertFalse(required.contains("optArr"));
    }

    @Test
    public void buildContainsTypeObjectKey() {
        Map<String, Object> schema = McpToolSchema.builder().build();
        assertEquals("object", schema.get("type"));
    }

    @Test
    public void buildIsImmutable() {
        Map<String, Object> schema = McpToolSchema.builder().build();
        assertThrows(UnsupportedOperationException.class, () -> schema.put("newKey", "value"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void multipleArgsAccumulate() {
        Map<String, Object> schema = McpToolSchema.builder()
                .arg("arg1", "string", "first")
                .optionalArg("arg2", "integer", "second")
                .build();
                
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        List<String> required = (List<String>) schema.get("required");
        
        assertEquals(2, properties.size());
        assertEquals(1, required.size());
        assertTrue(properties.containsKey("arg1"));
        assertTrue(properties.containsKey("arg2"));
        assertTrue(required.contains("arg1"));
    }
}
