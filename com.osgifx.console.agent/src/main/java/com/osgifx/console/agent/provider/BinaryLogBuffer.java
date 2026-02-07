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
package com.osgifx.console.agent.provider;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A fixed-size circular buffer for storing logs in a packed binary format.
 * <p>
 * Designed for constrained devices to avoid the memory overhead of {@code List<LogEntry>}.
 * It maintains a "Flight Recorder" history of the last N MBs of logs.
 */
public class BinaryLogBuffer {

    // 1 MB default
    public static final int DEFAULT_BUFFER_SIZE = 1024 * 1024;
    // 1024 entries index
    public static final int DEFAULT_INDEX_SIZE = 1024;

    private final byte[] data;
    private final int[]  index;
    private final int    capacity;
    private final int    indexCapacity;

    private int  head       = 0;
    private long writeCount = 0;

    public BinaryLogBuffer() {
        this(DEFAULT_BUFFER_SIZE, DEFAULT_INDEX_SIZE);
    }

    public BinaryLogBuffer(int bufferSize, int indexSize) {
        this.capacity      = bufferSize;
        this.indexCapacity = indexSize;
        this.data          = new byte[capacity];
        this.index         = new int[indexCapacity];
        Arrays.fill(index, -1);
    }

    /**
     * Writes a log entry into the circular buffer.
     */
    public synchronized void write(long timestamp, long bundleId, int level, String message, String exception) {
        byte[] msgBytes = message != null ? message.getBytes(StandardCharsets.UTF_8) : new byte[0];
        byte[] excBytes = exception != null ? exception.getBytes(StandardCharsets.UTF_8) : new byte[0];

        // Header: Time(8) + Bundle(8) + Lvl(4) + MsgLen(4) + ExcLen(4) = 28 bytes

        // 1. Record index
        index[(int) (writeCount % indexCapacity)] = head;

        // 2. Write Data (Handle wrapping)
        writeLong(timestamp);
        writeLong(bundleId);
        writeInt(level);
        writeInt(msgBytes.length);
        writeInt(excBytes.length);
        writeBytes(msgBytes);
        writeBytes(excBytes);

        writeCount++;
    }

    /**
     * Retrieves the last N logs as a raw byte array.
     */
    public synchronized byte[] getLogSnapshot(int count) {
        if (count <= 0 || count > indexCapacity) {
            count = indexCapacity;
        }
        if (writeCount == 0) {
            return new byte[0];
        }

        long actualCount = Math.min(count, writeCount);
        int  startIndex  = (int) ((writeCount - actualCount) % indexCapacity);
        int  startOffset = index[startIndex];

        if (startOffset == -1) {
            // Should not happen if logic is correct, but safe fallback
            return new byte[0];
        }

        return readFrom(startOffset);
    }

    /**
     * Reads all bytes from startOffset to head (handling wrap-around)
     */
    private byte[] readFrom(int startOffset) {
        int length;
        if (head >= startOffset) {
            length = head - startOffset;
            byte[] result = new byte[length];
            System.arraycopy(data, startOffset, result, 0, length);
            return result;
        } else {
            // Wrapped
            int part1Len = capacity - startOffset;
            int part2Len = head;
            length = part1Len + part2Len;
            byte[] result = new byte[length];
            System.arraycopy(data, startOffset, result, 0, part1Len);
            System.arraycopy(data, 0, result, part1Len, part2Len);
            return result;
        }
    }

    // --- Primitive Write Helpers (Request Zero Allocation) ---

    private void writeBytes(byte[] b) {
        if (b.length == 0) {
            return;
        }
        int availableAtEnd = capacity - head;
        if (availableAtEnd >= b.length) {
            System.arraycopy(b, 0, data, head, b.length);
            head += b.length;
        } else {
            // Wrap
            System.arraycopy(b, 0, data, head, availableAtEnd);
            int remaining = b.length - availableAtEnd;
            System.arraycopy(b, availableAtEnd, data, 0, remaining);
            head = remaining;
        }
        if (head == capacity) {
            head = 0;
        }
    }

    private void writeInt(int v) {
        // 4 bytes
        writeByte((byte) (v >>> 24));
        writeByte((byte) (v >>> 16));
        writeByte((byte) (v >>> 8));
        writeByte((byte) (v >>> 0));
    }

    private void writeLong(long v) {
        // 8 bytes
        writeByte((byte) (v >>> 56));
        writeByte((byte) (v >>> 48));
        writeByte((byte) (v >>> 40));
        writeByte((byte) (v >>> 32));
        writeByte((byte) (v >>> 24));
        writeByte((byte) (v >>> 16));
        writeByte((byte) (v >>> 8));
        writeByte((byte) (v >>> 0));
    }

    private void writeByte(byte b) {
        data[head] = b;
        head++;
        if (head == capacity) {
            head = 0;
        }
    }

    // --- Persistence ---

    public synchronized void toDisk(File file) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            dos.writeInt(head);
            dos.writeLong(writeCount);
            dos.writeInt(data.length);
            dos.write(data);
            // We generally assume index size is constant, but could save it too
            dos.writeInt(index.length);
            for (int i : index) {
                dos.writeInt(i);
            }
        }
    }

    public synchronized void fromDisk(File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            head       = dis.readInt();
            writeCount = dis.readLong();
            int savedDataLen = dis.readInt();
            if (savedDataLen != capacity) {
                // Buffer size changed, invalidate cache
                return;
            }
            dis.readFully(data);

            int savedIndexLen = dis.readInt();
            if (savedIndexLen != indexCapacity) {
                return;
            }
            for (int i = 0; i < indexCapacity; i++) {
                index[i] = dis.readInt();
            }
        }
    }

    /**
     * Time Range Filter
     * <p>
     * Binary search on ring buffer is complex because 'index' is not sorted (it's a ring)
     * A simplified approach for time range is iterating the index ring from (writeCount - 1) backwards
     * checking timestamps until we find the range. Since index size is small (1024), iteration is cheap.
     */
    public synchronized byte[] getLogSnapshot(long fromTime, long toTime) {
        if (writeCount == 0)
            return new byte[0];

        // Placeholder for complex time slicing logic.
        // For simplicity in this first iteration, we just return empty.
        return new byte[0];
    }

}
