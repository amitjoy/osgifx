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

public class UnsafeDataOutput implements Closeable {

    private static final long    ARRAY_BASE_OFFSET = UnsafeMemory.ARRAY_BYTE_BASE_OFFSET;
    private static final boolean IS_BIG_ENDIAN     = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

    private byte[]                    buffer;
    private int                       position;
    private FastByteArrayOutputStream outputStream;

    public UnsafeDataOutput(int initialCapacity) {
        this.buffer       = new byte[initialCapacity];
        this.position     = 0;
        this.outputStream = null;
    }

    public UnsafeDataOutput(FastByteArrayOutputStream outputStream) {
        this.outputStream = outputStream;
        this.buffer       = null;
        this.position     = 0;
    }

    private void ensureCapacity(int minCapacity) {
        if (outputStream != null) {
            // When using outputStream mode, get the buffer from it
            buffer   = outputStream.getBuffer();
            position = outputStream.size();
            if (minCapacity > buffer.length) {
                // Let the outputStream handle growth
                outputStream.write(new byte[minCapacity - buffer.length], 0, 0);
                buffer = outputStream.getBuffer();
            }
        } else if (minCapacity > buffer.length) {
            int    newCapacity = Math.max(buffer.length << 1, minCapacity);
            byte[] newBuffer   = new byte[newCapacity];
            System.arraycopy(buffer, 0, newBuffer, 0, position);
            buffer = newBuffer;
        }
    }

    public void writeByte(byte v) {
        if (outputStream != null) {
            outputStream.write(v);
        } else {
            ensureCapacity(position + 1);
            UnsafeMemory.putByte(buffer, ARRAY_BASE_OFFSET + position, v);
            position += 1;
        }
    }

    public void writeShort(short v) {
        if (outputStream != null) {
            short val = IS_BIG_ENDIAN ? Short.reverseBytes(v) : v;
            outputStream.write((byte) val);
            outputStream.write((byte) (val >> 8));
        } else {
            ensureCapacity(position + 2);
            UnsafeMemory.putShort(buffer, ARRAY_BASE_OFFSET + position, IS_BIG_ENDIAN ? Short.reverseBytes(v) : v);
            position += 2;
        }
    }

    public void writeInt(int v) {
        if (outputStream != null) {
            int val = IS_BIG_ENDIAN ? Integer.reverseBytes(v) : v;
            outputStream.write((byte) val);
            outputStream.write((byte) (val >> 8));
            outputStream.write((byte) (val >> 16));
            outputStream.write((byte) (val >> 24));
        } else {
            ensureCapacity(position + 4);
            UnsafeMemory.putInt(buffer, ARRAY_BASE_OFFSET + position, IS_BIG_ENDIAN ? Integer.reverseBytes(v) : v);
            position += 4;
        }
    }

    public void writeLong(long v) {
        if (outputStream != null) {
            long val = IS_BIG_ENDIAN ? Long.reverseBytes(v) : v;
            outputStream.write((byte) val);
            outputStream.write((byte) (val >> 8));
            outputStream.write((byte) (val >> 16));
            outputStream.write((byte) (val >> 24));
            outputStream.write((byte) (val >> 32));
            outputStream.write((byte) (val >> 40));
            outputStream.write((byte) (val >> 48));
            outputStream.write((byte) (val >> 56));
        } else {
            ensureCapacity(position + 8);
            UnsafeMemory.putLong(buffer, ARRAY_BASE_OFFSET + position, IS_BIG_ENDIAN ? Long.reverseBytes(v) : v);
            position += 8;
        }
    }

    public void writeFloat(float v) {
        writeInt(Float.floatToIntBits(v));
    }

    public void writeDouble(double v) {
        writeLong(Double.doubleToLongBits(v));
    }

    public void writeBoolean(boolean v) {
        writeByte((byte) (v ? 1 : 0));
    }

    public void writeBytes(byte[] v) {
        if (v == null) {
            writeInt(-1);
            return;
        }
        int len = v.length;
        writeInt(len);
        if (len > 0) {
            write(v, 0, len);
        }
    }

    public void write(byte[] b, int off, int len) {
        if (outputStream != null) {
            outputStream.write(b, off, len);
        } else {
            ensureCapacity(position + len);
            UnsafeMemory.copyMemory(b, ARRAY_BASE_OFFSET + off, buffer, ARRAY_BASE_OFFSET + position, len);
            position += len;
        }
    }

    public void writeString(String s) {
        if (s == null) {
            writeInt(-1);
            return;
        }
        int len = s.length();
        if (len == 0) {
            writeInt(0);
            return;
        }

        boolean isAscii = true;
        for (int i = 0; i < len; i++) {
            if (s.charAt(i) >= 128) {
                isAscii = false;
                break;
            }
        }

        if (isAscii) {
            writeInt(len);
            if (outputStream != null) {
                for (int i = 0; i < len; i++) {
                    outputStream.write((byte) s.charAt(i));
                }
            } else {
                ensureCapacity(position + len);
                for (int i = 0; i < len; i++) {
                    UnsafeMemory.putByte(buffer, ARRAY_BASE_OFFSET + position + i, (byte) s.charAt(i));
                }
                position += len;
            }
        } else {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            writeInt(bytes.length);
            write(bytes, 0, bytes.length);
        }
    }

    public byte[] getBuffer() {
        if (outputStream != null) {
            return outputStream.getBuffer();
        }
        return buffer;
    }

    public int size() {
        if (outputStream != null) {
            return outputStream.size();
        }
        return position;
    }

    public byte[] toByteArray() {
        if (outputStream != null) {
            return outputStream.toByteArray();
        }
        byte[] copy = new byte[position];
        System.arraycopy(buffer, 0, copy, 0, position);
        return copy;
    }

    public void reset() {
        if (outputStream != null) {
            outputStream.reset();
        }
        position = 0;
    }

    @Override
    public void close() throws IOException {
        // No-op for byte array backed output
    }
}
