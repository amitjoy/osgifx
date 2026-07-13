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
package com.osgifx.console.agent.rpc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.rpc.codec.BinaryCodec;


import static com.osgifx.console.agent.Agent.AGENT_RPC_MAX_COLLECTION_SIZE_KEY;
import static com.osgifx.console.agent.Agent.AGENT_RPC_MAX_MAP_SIZE_KEY;

public class BinaryCodecTest {

    private BinaryCodec codec;

    @BeforeEach
    public void setUp() {
        codec = new BinaryCodec();
    }

    @Test
    public void testArrayDecoding() throws Exception {
        String[]              input = { "a", "b", "c" };
        ByteArrayOutputStream baos  = new ByteArrayOutputStream();
        DataOutputStream      daos  = new DataOutputStream(baos);
        codec.encode(input, daos);
        daos.flush();

        byte[]   data   = baos.toByteArray();
        String[] output = codec.decode(data, String[].class);

        assertArrayEquals(input, output);
    }

    @Test
    public void testNestedArrayDecoding() throws Exception {
        String[][]            input = { { "a", "b" }, { "c", "d" } };
        ByteArrayOutputStream baos  = new ByteArrayOutputStream();
        DataOutputStream      daos  = new DataOutputStream(baos);
        codec.encode(input, daos);
        daos.flush();

        byte[]     data   = baos.toByteArray();
        String[][] output = codec.decode(data, String[][].class);

        assertArrayEquals(input[0], output[0]);
        assertArrayEquals(input[1], output[1]);
    }

    @Test
    public void testIntArrayDecoding() throws Exception {
        int[]                 input = { 1, 2, 3 };
        ByteArrayOutputStream baos  = new ByteArrayOutputStream();
        DataOutputStream      daos  = new DataOutputStream(baos);
        codec.encode(input, daos);
        daos.flush();

        byte[] data   = baos.toByteArray();
        int[]  output = codec.decode(data, int[].class);

        assertArrayEquals(input, output);
    }

    public static class SimpleDTO {
        public String name;
        public int    age;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SimpleDTO simpleDTO = (SimpleDTO) o;
            return age == simpleDTO.age && Objects.equals(name, simpleDTO.name);
        }
    }

    @Test
    public void dtoEncodeDecodeRoundTrip() throws Exception {
        SimpleDTO input = new SimpleDTO();
        input.name = "Test";
        input.age = 42;

        byte[] data = codec.encode(input);
        SimpleDTO output = codec.decode(data, SimpleDTO.class);

        assertEquals(input, output);
    }

    @Test
    public void nullEncodeDecodesNull() throws Exception {
        byte[] data = codec.encode(null);
        Object output = codec.decode(data, Object.class);
        assertNull(output);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void listEncodeDecodeRoundTrip() throws Exception {
        List<String> input = Arrays.asList("foo", "bar");
        byte[] data = codec.encode(input);
        
        List<String> output = codec.decode(data, List.class);
        assertEquals(input, output);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mapEncodeDecodeRoundTrip() throws Exception {
        Map<String, String> input = new HashMap<>();
        input.put("key", "value");
        byte[] data = codec.encode(input);
        
        Map<String, String> output = codec.decode(data, Map.class);
        assertEquals(input, output);
    }

    public enum TestEnum {
        A, B, C
    }

    @Test
    public void enumEncodeDecodeRoundTrip() throws Exception {
        TestEnum input = TestEnum.B;
        byte[] data = codec.encode(input);
        TestEnum output = codec.decode(data, TestEnum.class);
        assertEquals(input, output);
    }

    @Test
    public void booleanEncodeDecodeRoundTrip() throws Exception {
        Boolean inputTrue = Boolean.TRUE;
        byte[] dataTrue = codec.encode(inputTrue);
        Boolean outputTrue = codec.decode(dataTrue, Boolean.class);
        assertEquals(inputTrue, outputTrue);

        Boolean inputFalse = Boolean.FALSE;
        byte[] dataFalse = codec.encode(inputFalse);
        Boolean outputFalse = codec.decode(dataFalse, Boolean.class);
        assertEquals(inputFalse, outputFalse);
    }

    @Test
    public void longEncodeDecodeRoundTrip() throws Exception {
        Long input = Long.MAX_VALUE;
        byte[] data = codec.encode(input);
        Long output = codec.decode(data, Long.class);
        assertEquals(input, output);
    }

    @Test
    public void byteArrayEncodeDecodeRoundTrip() throws Exception {
        byte[] input = { 1, 2, 3, 4, 5 };
        byte[] data = codec.encode(input);
        byte[] output = codec.decode(data, byte[].class);
        assertArrayEquals(input, output);
    }

    @Test
    public void collectionSizeLimitEnforced() throws Exception {
        BundleContext ctx = mock(BundleContext.class);
        when(ctx.getProperty(AGENT_RPC_MAX_COLLECTION_SIZE_KEY)).thenReturn("2");
        BinaryCodec customCodec = new BinaryCodec(ctx);

        List<String> input = Arrays.asList("a", "b", "c");
        byte[] data = codec.encode(input); // encode with default limitless codec
        
        // decode with restricted codec should fail
        assertThrows(RuntimeException.class, () -> customCodec.decode(data, List.class));
    }

    @Test
    public void mapSizeLimitEnforced() throws Exception {
        BundleContext ctx = mock(BundleContext.class);
        when(ctx.getProperty(AGENT_RPC_MAX_MAP_SIZE_KEY)).thenReturn("1");
        BinaryCodec customCodec = new BinaryCodec(ctx);

        Map<String, String> input = new HashMap<>();
        input.put("1", "A");
        input.put("2", "B");
        byte[] data = codec.encode(input);
        
        assertThrows(RuntimeException.class, () -> customCodec.decode(data, Map.class));
    }
}
