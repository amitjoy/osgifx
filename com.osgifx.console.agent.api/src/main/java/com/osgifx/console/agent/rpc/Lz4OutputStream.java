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
import java.io.OutputStream;

/**
 * LZ4 compression stream that mimics GZIPOutputStream interface.
 * Buffers data, compresses on finish(), and writes: compressed length + uncompressed length + compressed data.
 */
public class Lz4OutputStream extends OutputStream {
    private final OutputStream              out;
    private final FastByteArrayOutputStream buffer;
    private boolean                         finished;

    public Lz4OutputStream(OutputStream out) {
        this.out      = out;
        this.buffer   = new FastByteArrayOutputStream(4096);
        this.finished = false;
    }

    @Override
    public void write(int b) throws IOException {
        if (finished) {
            throw new IOException("Stream already finished");
        }
        buffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (finished) {
            throw new IOException("Stream already finished");
        }
        buffer.write(b, off, len);
    }

    public void finish() throws IOException {
        if (finished) {
            return;
        }
        finished = true;

        int    uncompressedLength = buffer.size();
        byte[] uncompressed       = buffer.getBuffer();

        // Compress
        byte[] compressed = Lz4Codec.compress(uncompressed, 0, uncompressedLength);

        // Write header: compressed length (4 bytes) + uncompressed length (4 bytes)
        writeInt(out, compressed.length);
        writeInt(out, uncompressedLength);

        // Write compressed data
        out.write(compressed);
        out.flush();
    }

    @Override
    public void flush() throws IOException {
        // Don't flush until finish() is called
    }

    @Override
    public void close() throws IOException {
        if (!finished) {
            finish();
        }
        out.close();
    }

    private static void writeInt(OutputStream out, int v) throws IOException {
        out.write((v >>> 24) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 8) & 0xFF);
        out.write(v & 0xFF);
    }
}
