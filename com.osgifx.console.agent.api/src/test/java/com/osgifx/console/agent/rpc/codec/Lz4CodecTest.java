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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class Lz4CodecTest {

    @Test
    public void compressAndDecompressLargePayload() throws Exception {
        // Create payload > 512 bytes with compressible pattern
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("This is a highly compressible pattern ");
        }
        byte[] original = sb.toString().getBytes(StandardCharsets.UTF_8);
        assertTrue(original.length > 512);

        byte[] compressed = Lz4Codec.compress(original, 0, original.length);
        assertTrue(compressed.length < original.length); // verify it actually compressed

        byte[] decompressed = Lz4Codec.decompress(compressed, original.length, original.length * 2L);
        assertArrayEquals(original, decompressed);
    }

    @Test
    public void smallPayloadIsNotCompressed() throws Exception {
        byte[] original = "Small payload".getBytes(StandardCharsets.UTF_8);
        byte[] compressed = Lz4Codec.compress(original, 0, original.length);
        
        // When < 512, compress just returns the original array
        assertArrayEquals(original, compressed);
    }

    @Test
    public void compressWithLengthDecompressWithLength() throws Exception {
        // Create payload > 512 bytes
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("This is a highly compressible pattern ");
        }
        byte[] original = sb.toString().getBytes(StandardCharsets.UTF_8);

        byte[] withLength = Lz4Codec.compressWithLength(original);
        // Flag + 4 bytes length = 5 bytes prefix
        assertEquals(1, withLength[0]); // 1 means compressed

        byte[] decompressed = Lz4Codec.decompressWithLength(withLength, original.length * 2L);
        assertArrayEquals(original, decompressed);
    }

    @Test
    public void emptyArrayRoundTrip() throws Exception {
        byte[] original = new byte[0];
        byte[] withLength = Lz4Codec.compressWithLength(original);
        assertEquals(0, withLength[0]); // 0 means uncompressed
        
        byte[] decompressed = Lz4Codec.decompressWithLength(withLength, 100L);
        assertArrayEquals(original, decompressed);
    }

    @Test
    public void decompressWithLengthRespectsMaxSize() throws Exception {
        // Create payload > 512 bytes
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("This is a highly compressible pattern ");
        }
        byte[] original = sb.toString().getBytes(StandardCharsets.UTF_8);

        byte[] withLength = Lz4Codec.compressWithLength(original);
        
        // Attempt to decompress with a max size smaller than original
        assertThrows(IOException.class, () -> {
            Lz4Codec.decompressWithLength(withLength, original.length - 10L);
        });
    }
}
