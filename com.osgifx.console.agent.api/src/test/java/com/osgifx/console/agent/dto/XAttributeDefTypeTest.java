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
package com.osgifx.console.agent.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;

public class XAttributeDefTypeTest {

    @Test
    public void getTypeForString() {
        assertEquals(XAttributeDefType.STRING, XAttributeDefType.getType("hello"));
    }

    @Test
    public void getTypeForStringArray() {
        assertEquals(XAttributeDefType.STRING_ARRAY, XAttributeDefType.getType(new String[] { "a" }));
    }

    @Test
    public void getTypeForStringList() {
        assertEquals(XAttributeDefType.STRING_LIST, XAttributeDefType.getType(Collections.singletonList("a")));
    }

    @Test
    public void getTypeForInteger() {
        assertEquals(XAttributeDefType.INTEGER, XAttributeDefType.getType(Integer.valueOf(1)));
    }

    @Test
    public void getTypeForIntArray() {
        assertEquals(XAttributeDefType.INTEGER_ARRAY, XAttributeDefType.getType(new int[] { 1 }));
    }

    @Test
    public void getTypeForIntegerList() {
        assertEquals(XAttributeDefType.INTEGER_LIST, XAttributeDefType.getType(Collections.singletonList(1)));
    }

    @Test
    public void getTypeForBoolean() {
        assertEquals(XAttributeDefType.BOOLEAN, XAttributeDefType.getType(Boolean.TRUE));
    }

    @Test
    public void getTypeForBooleanArray() {
        assertEquals(XAttributeDefType.BOOLEAN_ARRAY, XAttributeDefType.getType(new boolean[] { true }));
    }

    @Test
    public void getTypeForBooleanList() {
        assertEquals(XAttributeDefType.BOOLEAN_LIST, XAttributeDefType.getType(Collections.singletonList(true)));
    }

    @Test
    public void getTypeForDouble() {
        assertEquals(XAttributeDefType.DOUBLE, XAttributeDefType.getType(Double.valueOf(1.0)));
    }

    @Test
    public void getTypeForLong() {
        assertEquals(XAttributeDefType.LONG, XAttributeDefType.getType(Long.valueOf(1L)));
    }

    @Test
    public void getTypeForFloat() {
        assertEquals(XAttributeDefType.FLOAT, XAttributeDefType.getType(Float.valueOf(1.0f)));
    }

    @Test
    public void getTypeForChar() {
        assertEquals(XAttributeDefType.CHAR, XAttributeDefType.getType(Character.valueOf('a')));
    }

    @Test
    public void clazzForString() {
        assertEquals(String.class, XAttributeDefType.clazz(XAttributeDefType.STRING));
    }

    @Test
    public void clazzForPassword() {
        assertEquals(String.class, XAttributeDefType.clazz(XAttributeDefType.PASSWORD));
    }

    @Test
    public void clazzForInteger() {
        assertEquals(Integer.class, XAttributeDefType.clazz(XAttributeDefType.INTEGER));
    }

    @Test
    public void clazzForBoolean() {
        assertEquals(Boolean.class, XAttributeDefType.clazz(XAttributeDefType.BOOLEAN));
    }

}
