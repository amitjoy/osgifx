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

import java.io.IOException;

import com.osgifx.console.agent.rpc.lz4.Lz4Compressor;
import com.osgifx.console.agent.rpc.lz4.Lz4Decompressor;

/**
 * LZ4 compression/decompression codec for RPC payloads.
 * Uses vendored LZ4 from io.airlift:aircompressor for zero-allocation compression.
 * Thread-safe via ThreadLocal instances.
 */
public final class Lz4Codec {

    private static final int COMPRESSION_THRESHOLD = 512; // Compress if >= 512 bytes

    // ThreadLocal instances for thread-safe reuse
    private static final ThreadLocal<Lz4Compressor>   COMPRESSOR   = ThreadLocal.withInitial(Lz4Compressor::new);
    private static final ThreadLocal<Lz4Decompressor> DECOMPRESSOR = ThreadLocal.withInitial(Lz4Decompressor::new);

    private Lz4Codec() {
    }

    /**
     * Compresses data using LZ4 if it exceeds the compression threshold.
     *
     * @param data the data to compress
     * @param offset the offset in the data array
     * @param length the length of data to compress
     * @return compressed data, or original data if compression not beneficial
     * @throws IOException if compression fails
     */
    public static byte[] compress(byte[] data, int offset, int length) throws IOException {
        if (length < COMPRESSION_THRESHOLD) {
            // Too small to benefit from compression
            if (offset == 0 && length == data.length) {
                return data;
            }
            byte[] result = new byte[length];
            System.arraycopy(data, offset, result, 0, length);
            return result;
        }

        try {
            Lz4Compressor compressor          = COMPRESSOR.get();
            int           maxCompressedLength = compressor.maxCompressedLength(length);
            byte[]        compressed          = new byte[maxCompressedLength];

            int compressedSize = compressor.compress(data, offset, length, compressed, 0, maxCompressedLength);

            // Only use compressed if it's actually smaller
            if (compressedSize < length) {
                byte[] result = new byte[compressedSize];
                System.arraycopy(compressed, 0, result, 0, compressedSize);
                return result;
            }

            // Compression didn't help, return original
            if (offset == 0 && length == data.length) {
                return data;
            }
            byte[] result = new byte[length];
            System.arraycopy(data, offset, result, 0, length);
            return result;
        } catch (Exception e) {
            throw new IOException("LZ4 compression failed", e);
        }
    }

    /**
     * Decompresses LZ4-compressed data with bomb protection.
     *
     * @param compressed the compressed data
     * @param uncompressedLength the expected uncompressed length
     * @param maxDecompressedSize the maximum allowed decompressed size (bomb protection)
     * @return decompressed data
     * @throws IOException if decompression fails or exceeds size limit
     */
    public static byte[] decompress(byte[] compressed,
                                    int uncompressedLength,
                                    long maxDecompressedSize) throws IOException {
        // Bomb protection: reject before allocation
        if (uncompressedLength > maxDecompressedSize) {
            throw new IOException(String.format(
                    "Decompressed size %d exceeds maximum allowed size %d. "
                            + "This may indicate a zip bomb attack or an unexpectedly large payload.",
                    uncompressedLength, maxDecompressedSize));
        }

        if (uncompressedLength < 0) {
            throw new IOException("Invalid uncompressed length: " + uncompressedLength);
        }

        try {
            byte[] decompressed = new byte[uncompressedLength];

            Lz4Decompressor decompressor = DECOMPRESSOR.get();
            decompressor.decompress(compressed, 0, compressed.length, decompressed, 0, uncompressedLength);

            return decompressed;
        } catch (Exception e) {
            throw new IOException("LZ4 decompression failed", e);
        }
    }

    /**
     * Returns the compression threshold in bytes.
     * Payloads smaller than this are not compressed.
     *
     * @return compression threshold in bytes
     */
    public static int getCompressionThreshold() {
        return COMPRESSION_THRESHOLD;
    }
}
