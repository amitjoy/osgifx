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
package com.osgifx.console.agent.rpc.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SnapshotDecoderTest {

    private BinaryCodec codec;
    private SnapshotDecoder decoder;

    @BeforeEach
    public void setUp() {
        codec = new BinaryCodec();
        decoder = new SnapshotDecoder(codec);
    }

    public static class TestDTO {
        public String name;
        public int    value;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestDTO testDTO = (TestDTO) o;
            return value == testDTO.value && Objects.equals(name, testDTO.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value);
        }
    }

    @Test
    public void decodeListReturnsEmptyForNullSnapshot() {
        List<TestDTO> result = decoder.decodeList(null, TestDTO.class);
        assertTrue(result.isEmpty());
    }

    @Test
    public void decodeListReturnsEmptyForEmptySnapshot() {
        List<TestDTO> result = decoder.decodeList(new byte[0], TestDTO.class);
        assertTrue(result.isEmpty());
    }

    @Test
    public void decodeListRoundTrip() throws Exception {
        TestDTO dto1 = new TestDTO();
        dto1.name = "A";
        dto1.value = 1;

        TestDTO dto2 = new TestDTO();
        dto2.name = "B";
        dto2.value = 2;

        List<TestDTO> input = Arrays.asList(dto1, dto2);
        byte[] binary = codec.encode(input);
        byte[] snapshot = Lz4Codec.compressWithLength(binary);

        List<TestDTO> output = decoder.decodeList(snapshot, TestDTO.class);
        assertEquals(input, output);
    }

    @Test
    public void decodeSetRoundTrip() throws Exception {
        TestDTO dto1 = new TestDTO();
        dto1.name = "A";
        dto1.value = 1;

        TestDTO dto2 = new TestDTO();
        dto2.name = "B";
        dto2.value = 2;

        Set<TestDTO> input = new HashSet<>(Arrays.asList(dto1, dto2));
        byte[] binary = codec.encode(input);
        byte[] snapshot = Lz4Codec.compressWithLength(binary);

        Set<TestDTO> output = decoder.decodeSet(snapshot, TestDTO.class);
        assertEquals(input, output);
    }

    @Test
    public void decodeSingleDtoRoundTrip() throws Exception {
        TestDTO input = new TestDTO();
        input.name = "Single";
        input.value = 42;

        byte[] binary = codec.encode(input);
        byte[] snapshot = Lz4Codec.compressWithLength(binary);

        TestDTO output = decoder.decode(snapshot, TestDTO.class);
        assertEquals(input, output);
    }

    @Test
    public void decodeNullSnapshotReturnsNull() {
        TestDTO result = decoder.decode(null, TestDTO.class);
        assertNull(result);
    }
}
