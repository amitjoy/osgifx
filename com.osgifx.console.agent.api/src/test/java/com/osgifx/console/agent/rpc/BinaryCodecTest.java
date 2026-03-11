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

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import org.junit.Test;

import com.osgifx.console.agent.rpc.codec.BinaryCodec;

public class BinaryCodecTest {

    @Test
    public void testArrayDecoding() throws Exception {
        BinaryCodec codec = new BinaryCodec();

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
        BinaryCodec codec = new BinaryCodec();

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
        BinaryCodec codec = new BinaryCodec();

        int[]                 input = { 1, 2, 3 };
        ByteArrayOutputStream baos  = new ByteArrayOutputStream();
        DataOutputStream      daos  = new DataOutputStream(baos);
        codec.encode(input, daos);
        daos.flush();

        byte[] data   = baos.toByteArray();
        int[]  output = codec.decode(data, int[].class);

        assertArrayEquals(input, output);
    }
}
