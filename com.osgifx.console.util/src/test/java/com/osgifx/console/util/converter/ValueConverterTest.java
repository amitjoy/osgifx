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
package com.osgifx.console.util.converter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.osgifx.console.agent.dto.XAttributeDefType;

public class ValueConverterTest {

    private ValueConverter converter;

    @BeforeEach
    public void setUp() {
        converter = new ValueConverter();
    }

    @Test
    public void convertStringToStringArray() {
        String[] result = (String[]) converter.convert("a,b,c", XAttributeDefType.STRING_ARRAY);
        assertArrayEquals(new String[]{"a", "b", "c"}, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void convertStringToStringList() {
        List<String> result = (List<String>) converter.convert("a,b", XAttributeDefType.STRING_LIST);
        assertEquals(List.of("a", "b"), result);
    }

    @Test
    public void convertStringToIntArray() {
        int[] result = (int[]) converter.convert("1,2,3", XAttributeDefType.INTEGER_ARRAY);
        assertArrayEquals(new int[]{1, 2, 3}, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void convertStringToIntegerList() {
        List<Integer> result = (List<Integer>) converter.convert("1,2", XAttributeDefType.INTEGER_LIST);
        assertEquals(List.of(1, 2), result);
    }

    @Test
    public void convertStringToBooleanArray() {
        boolean[] result = (boolean[]) converter.convert("true,false", XAttributeDefType.BOOLEAN_ARRAY);
        assertArrayEquals(new boolean[]{true, false}, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void convertStringToBooleanList() {
        List<Boolean> result = (List<Boolean>) converter.convert("true,false", XAttributeDefType.BOOLEAN_LIST);
        assertEquals(List.of(true, false), result);
    }

    @Test
    public void convertStringToDoubleArray() {
        double[] result = (double[]) converter.convert("1.5,2.5", XAttributeDefType.DOUBLE_ARRAY);
        assertArrayEquals(new double[]{1.5, 2.5}, result, 0.0001);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void convertStringToDoubleList() {
        List<Double> result = (List<Double>) converter.convert("1.5,2.5", XAttributeDefType.DOUBLE_LIST);
        assertEquals(List.of(1.5, 2.5), result);
    }

    @Test
    public void convertStringToFloatArray() {
        float[] result = (float[]) converter.convert("1.5,2.5", XAttributeDefType.FLOAT_ARRAY);
        assertArrayEquals(new float[]{1.5f, 2.5f}, result, 0.0001f);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void convertStringToFloatList() {
        List<Float> result = (List<Float>) converter.convert("1.5", XAttributeDefType.FLOAT_LIST);
        assertEquals(List.of(1.5f), result);
    }

    @Test
    public void convertStringToLongArray() {
        long[] result = (long[]) converter.convert("100,200", XAttributeDefType.LONG_ARRAY);
        assertArrayEquals(new long[]{100L, 200L}, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void convertStringToLongList() {
        List<Long> result = (List<Long>) converter.convert("100", XAttributeDefType.LONG_LIST);
        assertEquals(List.of(100L), result);
    }

    @Test
    public void convertStringToCharArray() {
        char[] result = (char[]) converter.convert("a,b", XAttributeDefType.CHAR_ARRAY);
        assertArrayEquals(new char[]{'a', 'b'}, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void convertStringToCharList() {
        List<Character> result = (List<Character>) converter.convert("a,b", XAttributeDefType.CHAR_LIST);
        assertEquals(List.of('a', 'b'), result);
    }

    @Test
    public void convertWithXAttributeDefTypeString() {
        String result = (String) converter.convert("test", XAttributeDefType.STRING);
        assertEquals("test", result);
    }

    @Test
    public void convertWithXAttributeDefTypeInteger() {
        Integer result = (Integer) converter.convert("100", XAttributeDefType.INTEGER);
        assertEquals(100, result);
    }

    @Test
    public void convertWithClassDirectly() {
        Integer result = converter.convert("100", Integer.class);
        assertEquals(100, result);
    }
}
