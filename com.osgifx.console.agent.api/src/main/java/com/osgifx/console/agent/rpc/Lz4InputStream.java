/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.rpc;

import java.io.IOException;
import java.io.InputStream;

import com.osgifx.console.agent.rpc.codec.Lz4Codec;

/**
 * LZ4 decompression stream that mimics GZIPInputStream interface.
 * Reads compressed length + uncompressed length header, then decompresses the payload.
 */
public class Lz4InputStream extends InputStream {
    private final InputStream in;
    private final long        maxDecompressedSize;
    private byte[]            decompressed;
    private int               position;
    private boolean           headerRead;

    public Lz4InputStream(InputStream in, long maxDecompressedSize) {
        this.in                  = in;
        this.maxDecompressedSize = maxDecompressedSize;
        this.position            = 0;
        this.headerRead          = false;
    }

    private void ensureDecompressed() throws IOException {
        if (headerRead) {
            return;
        }
        headerRead = true;

        // Read header: compressed length (4 bytes) + uncompressed length (4 bytes)
        int compressedLength   = readInt(in);
        int uncompressedLength = readInt(in);

        // Bomb protection
        if (uncompressedLength > maxDecompressedSize) {
            throw new IOException(String.format("Decompressed size %d exceeds maximum %d", uncompressedLength,
                    maxDecompressedSize));
        }

        // Read compressed data
        byte[] compressed = new byte[compressedLength];
        int    totalRead  = 0;
        while (totalRead < compressedLength) {
            int read = in.read(compressed, totalRead, compressedLength - totalRead);
            if (read < 0) {
                throw new IOException("Unexpected end of stream");
            }
            totalRead += read;
        }

        // Decompress
        decompressed = Lz4Codec.decompress(compressed, uncompressedLength, maxDecompressedSize);
        position     = 0;
    }

    @Override
    public int read() throws IOException {
        ensureDecompressed();
        if (position >= decompressed.length) {
            return -1;
        }
        return decompressed[position++] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        ensureDecompressed();
        if (position >= decompressed.length) {
            return -1;
        }
        int available = decompressed.length - position;
        int toRead    = Math.min(len, available);
        System.arraycopy(decompressed, position, b, off, toRead);
        position += toRead;
        return toRead;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    private static int readInt(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new IOException("Unexpected end of stream");
        }
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4);
    }
}
