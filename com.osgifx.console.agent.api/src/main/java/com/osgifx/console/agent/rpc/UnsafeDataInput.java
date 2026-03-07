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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class UnsafeDataInput implements Closeable {

    private static final long    ARRAY_BASE_OFFSET = UnsafeMemory.ARRAY_BYTE_BASE_OFFSET;
    private static final boolean IS_BIG_ENDIAN     = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

    private final byte[] buffer;
    private int          position;
    private final int    limit;

    public UnsafeDataInput(byte[] buffer) {
        this.buffer   = buffer;
        this.position = 0;
        this.limit    = buffer.length;
    }

    public UnsafeDataInput(byte[] buffer, int offset, int length) {
        this.buffer   = buffer;
        this.position = offset;
        this.limit    = offset + length;
    }

    public byte readByte() {
        byte v = UnsafeMemory.getByte(buffer, ARRAY_BASE_OFFSET + position);
        position += 1;
        return v;
    }

    public short readShort() {
        short v = UnsafeMemory.getShort(buffer, ARRAY_BASE_OFFSET + position);
        position += 2;
        return IS_BIG_ENDIAN ? Short.reverseBytes(v) : v;
    }

    public int readInt() {
        int v = UnsafeMemory.getInt(buffer, ARRAY_BASE_OFFSET + position);
        position += 4;
        return IS_BIG_ENDIAN ? Integer.reverseBytes(v) : v;
    }

    public long readLong() {
        long v = UnsafeMemory.getLong(buffer, ARRAY_BASE_OFFSET + position);
        position += 8;
        return IS_BIG_ENDIAN ? Long.reverseBytes(v) : v;
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    public boolean readBoolean() {
        return readByte() != 0;
    }

    public byte[] readBytes() {
        int len = readInt();
        if (len == -1) {
            return null;
        }
        if (len == 0) {
            return new byte[0];
        }
        byte[] v = new byte[len];
        UnsafeMemory.copyMemory(buffer, ARRAY_BASE_OFFSET + position, v, ARRAY_BASE_OFFSET, len);
        position += len;
        return v;
    }

    public String readString() {
        int len = readInt();
        if (len == -1) {
            return null;
        }
        if (len == 0) {
            return "";
        }

        // Optimistically check for pure ASCII
        boolean isAscii = true;
        for (int i = 0; i < len; i++) {
            if (UnsafeMemory.getByte(buffer, ARRAY_BASE_OFFSET + position + i) < 0) {
                isAscii = false;
                break;
            }
        }

        if (isAscii) {
            char[] chars = new char[len];
            for (int i = 0; i < len; i++) {
                chars[i] = (char) UnsafeMemory.getByte(buffer, ARRAY_BASE_OFFSET + position + i);
            }
            position += len;
            return new String(chars);
        } else {
            String s = new String(buffer, position, len, StandardCharsets.UTF_8);
            position += len;
            return s;
        }
    }

    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    public void readFully(byte[] b, int off, int len) throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (position + len > limit) {
            throw new IOException("Not enough data to read");
        }
        UnsafeMemory.copyMemory(buffer, ARRAY_BASE_OFFSET + position, b, ARRAY_BASE_OFFSET + off, len);
        position += len;
    }

    @Override
    public void close() throws IOException {
        // No-op for byte array backed input
    }
}
