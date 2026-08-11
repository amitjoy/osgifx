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
package com.osgifx.console.agent.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.osgifx.console.agent.dto.ConfigValue;
import com.osgifx.console.agent.dto.XResultDTO;

public class AgentHelperTest {

    @Test
    public void getPropertyFromSystemProperty() {
        System.setProperty("test.prop.1", "sys-value");
        try {
            BundleContext ctx = mock(BundleContext.class);
            when(ctx.getProperty("test.prop.1")).thenReturn("ctx-value");

            String value = AgentHelper.getProperty("test.prop.1", ctx);
            assertEquals("sys-value", value);
        } finally {
            System.clearProperty("test.prop.1");
        }
    }

    @Test
    public void getPropertyFallsBackToNull() {
        String value = AgentHelper.getProperty("non.existent.prop", null);
        assertNull(value);
    }

    @Test
    public void getPropertyIgnoresBlankSystemProperty() {
        System.setProperty("test.prop.2", "  ");
        try {
            BundleContext ctx = mock(BundleContext.class);
            when(ctx.getProperty("test.prop.2")).thenReturn("ctx-value");

            String value = AgentHelper.getProperty("test.prop.2", ctx);
            assertEquals("ctx-value", value);
        } finally {
            System.clearProperty("test.prop.2");
        }
    }

    @Test
    public void substituteVariablesNoPlaceholders() {
        String text = "plain text without placeholders";
        String result = AgentHelper.substituteVariables(text, null);
        assertEquals(text, result);
    }

    @Test
    public void substituteVariablesSystemProperty() {
        System.setProperty("port", "8080");
        try {
            String text = "port={port}";
            String result = AgentHelper.substituteVariables(text, null);
            assertEquals("port=8080", result);
        } finally {
            System.clearProperty("port");
        }
    }

    @Test
    public void substituteVariablesEnv() {
        // We use PATH or HOME, whichever is likely available, or we just rely on env placeholder replacing
        String envKey = "PATH";
        String envValue = System.getenv(envKey);
        if (envValue != null) {
            String text = "{env:" + envKey + "}";
            String result = AgentHelper.substituteVariables(text, null);
            assertEquals(envValue, result);
        }
    }

    @Test
    public void substituteVariablesMissingKeyPreserved() {
        String text = "{unknown.key}";
        String result = AgentHelper.substituteVariables(text, null);
        assertEquals("{unknown.key}", result);
    }

    @Test
    public void substituteVariablesNull() {
        String result = AgentHelper.substituteVariables(null, null);
        assertNull(result);
    }

    @Test
    public void substituteVariablesEmpty() {
        String result = AgentHelper.substituteVariables("", null);
        assertEquals("", result);
    }

    @Test
    public void createResultDTO() {
        XResultDTO result = AgentHelper.createResult(1, "ok");
        assertEquals(1, result.result);
        assertEquals("ok", result.response);
    }

    @Test
    public void valueOfDictionary() {
        Dictionary<String, String> dict = new Hashtable<>();
        dict.put("key1", "val1");
        dict.put("key2", "val2");

        Map<String, String> map = AgentHelper.valueOf(dict);
        assertEquals(2, map.size());
        assertEquals("val1", map.get("key1"));
        assertEquals("val2", map.get("key2"));
    }

    @Test
    public void valueOfNullDictionary() {
        Map<String, String> map = AgentHelper.valueOf(null);
        assertNull(map);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void arrayToStringForObjectArray() {
        ServiceReference<Object> ref = mock(ServiceReference.class);
        when(ref.getPropertyKeys()).thenReturn(new String[] { "test.obj.array" });
        when(ref.getProperty("test.obj.array")).thenReturn(new Object[] { "a", "b" });

        Map<String, String> props = AgentHelper.createProperties(ref);
        assertEquals("[a, b]", props.get("test.obj.array"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void arrayToStringForIntArray() {
        ServiceReference<Object> ref = mock(ServiceReference.class);
        when(ref.getPropertyKeys()).thenReturn(new String[] { "test.int.array" });
        when(ref.getProperty("test.int.array")).thenReturn(new int[] { 1, 2 });

        Map<String, String> props = AgentHelper.createProperties(ref);
        assertEquals("[1, 2]", props.get("test.int.array"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void arrayToStringForBooleanArray() {
        ServiceReference<Object> ref = mock(ServiceReference.class);
        when(ref.getPropertyKeys()).thenReturn(new String[] { "test.bool.array" });
        when(ref.getProperty("test.bool.array")).thenReturn(new boolean[] { true, false });

        Map<String, String> props = AgentHelper.createProperties(ref);
        assertEquals("[true, false]", props.get("test.bool.array"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void arrayToStringForNull() {
        ServiceReference<Object> ref = mock(ServiceReference.class);
        when(ref.getPropertyKeys()).thenReturn(new String[] { "test.null.val" });
        when(ref.getProperty("test.null.val")).thenReturn(null);

        Map<String, String> props = AgentHelper.createProperties(ref);
        assertEquals("null", props.get("test.null.val"));
    }

    @Test
    public void convertStringArrayType() throws Exception {
        ConfigValue entry = ConfigValue.create("key", new String[] { "a", "b" }, com.osgifx.console.agent.dto.XAttributeDefType.STRING_ARRAY);
        Object result = AgentHelper.convert(entry);
        org.junit.jupiter.api.Assertions.assertTrue(result instanceof String[]);
        String[] arr = (String[]) result;
        org.junit.jupiter.api.Assertions.assertEquals("a", arr[0]);
        org.junit.jupiter.api.Assertions.assertEquals("b", arr[1]);
    }

    @Test
    public void convertIntegerArrayType() throws Exception {
        ConfigValue entry = ConfigValue.create("key", new int[] { 1, 2 }, com.osgifx.console.agent.dto.XAttributeDefType.INTEGER_ARRAY);
        Object result = AgentHelper.convert(entry);
        org.junit.jupiter.api.Assertions.assertTrue(result instanceof int[]);
        int[] arr = (int[]) result;
        org.junit.jupiter.api.Assertions.assertEquals(1, arr[0]);
        org.junit.jupiter.api.Assertions.assertEquals(2, arr[1]);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void convertBooleanListType() throws Exception {
        ConfigValue entry = ConfigValue.create("key", java.util.Arrays.asList(true, false), com.osgifx.console.agent.dto.XAttributeDefType.BOOLEAN_LIST);
        Object result = AgentHelper.convert(entry);
        org.junit.jupiter.api.Assertions.assertTrue(result instanceof java.util.List);
        java.util.List<Boolean> list = (java.util.List<Boolean>) result;
        org.junit.jupiter.api.Assertions.assertTrue(list.get(0));
        org.junit.jupiter.api.Assertions.assertFalse(list.get(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void convertLongListType() throws Exception {
        ConfigValue entry = ConfigValue.create("key", java.util.Arrays.asList(1L, 2L), com.osgifx.console.agent.dto.XAttributeDefType.LONG_LIST);
        Object result = AgentHelper.convert(entry);
        org.junit.jupiter.api.Assertions.assertTrue(result instanceof java.util.List);
        java.util.List<Long> list = (java.util.List<Long>) result;
        org.junit.jupiter.api.Assertions.assertEquals(1L, list.get(0));
        org.junit.jupiter.api.Assertions.assertEquals(2L, list.get(1));
    }

}
