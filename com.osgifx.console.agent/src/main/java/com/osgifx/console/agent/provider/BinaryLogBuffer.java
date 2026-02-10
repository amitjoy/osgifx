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
import java.util.concurrent.locks.ReentrantLock;

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

    private final ReentrantLock lock = new ReentrantLock();

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
    public void write(long timestamp, long bundleId, int level, String message, String exception) {
        lock.lock();
        try {
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
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves the last N logs as a raw byte array.
     */
    public byte[] getLogSnapshot(int count) {
        lock.lock();
        try {
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
        } finally {
            lock.unlock();
        }
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

    public void toDisk(File file) throws IOException {
        lock.lock();
        try {
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
        } finally {
            lock.unlock();
        }
    }

    public void fromDisk(File file) throws IOException {
        lock.lock();
        try {
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
        } finally {
            lock.unlock();
        }
    }

    /**
     * Time Range Filter
     * <p>
     * Binary search on ring buffer is complex because 'index' is not sorted (it's a ring)
     * A simplified approach for time range is iterating the index ring from (writeCount - 1) backwards
     * checking timestamps until we find the range. Since index size is small (1024), iteration is cheap.
     */
    public byte[] getLogSnapshot(long fromTime, long toTime) {
        lock.lock();
        try {
            if (writeCount == 0) {
                return new byte[0];
            }

            // Two-pass algorithm to minimize allocations
            // Pass 1: Calculate total size and identify valid entries
            int totalSize = 0;
            // Iterate backward from most recent entry
            long entriesToCheck = Math.min(writeCount, indexCapacity);
            // Stores the start offset of each matching entry
            // Optimization: Reusing the existing index concept, but since we can't easily store a dynamic list of
            // matching offsets without allocation, we'll re-scan in Pass 2.
            // But wait, re-scanning 1024 items is cheap (CPU) compared to allocation (Memory).
            // So we'll just find the start and end logical indices.

            // Logical index of the most recent entry
            long newestLogicalIndex = writeCount - 1;

            long startMatchIndex = -1;
            long endMatchIndex   = -1;

            // backward scan to find range
            for (long i = 0; i < entriesToCheck; i++) {
                long logicalIndex = newestLogicalIndex - i;
                int  offset       = index[(int) (logicalIndex % indexCapacity)];

                if (offset == -1) {
                    continue; // Should not happen given writeCount logic, but safe guard
                }

                long timestamp = readLong(offset);

                if (timestamp > toTime) {
                    continue; // Too new
                }
                if (timestamp < fromTime) {
                    break; // Too old, and since we iterate backward, all subsequent are also too old
                }

                // Match!
                if (endMatchIndex == -1) {
                    endMatchIndex = logicalIndex; // First match (newest in range)
                }
                startMatchIndex = logicalIndex; // Update last match (oldest in range)

                // Calculate size
                // Entry size = 28 (header) + msgLen + excLen
                int msgLen = readInt(offset + 20);
                int excLen = readInt(offset + 24);
                totalSize += (28 + msgLen + excLen);
            }

            if (startMatchIndex == -1) {
                return new byte[0];
            }

            // Pass 2: Allocate and copy
            byte[] result  = new byte[totalSize];
            int    destPos = 0;

            // Iterate forward from oldest match to newest match
            // Note: startMatchIndex is the *oldest* logical index (smaller value)
            // endMatchIndex is the *newest* logical index (larger value)
            for (long i = startMatchIndex; i <= endMatchIndex; i++) {
                int offset = index[(int) (i % indexCapacity)];
                int msgLen = readInt(offset + 20);
                int excLen = readInt(offset + 24);
                int length = 28 + msgLen + excLen;

                copyRange(offset, length, result, destPos);
                destPos += length;
            }

            return result;

        } finally {
            lock.unlock();
        }
    }

    // --- Reader Helpers for Circular Buffer ---

    private long readLong(int offset) {
        long v = 0;
        for (int i = 0; i < 8; i++) {
            v <<= 8;
            v  |= (data[(offset + i) % capacity] & 0xFF);
        }
        return v;
    }

    private int readInt(int offset) {
        int v = 0;
        for (int i = 0; i < 4; i++) {
            v <<= 8;
            v  |= (data[(offset + i) % capacity] & 0xFF);
        }
        return v;
    }

    private void copyRange(int srcOffset, int length, byte[] dest, int destOffset) {
        int availableToEnd = capacity - srcOffset;
        if (availableToEnd >= length) {
            System.arraycopy(data, srcOffset, dest, destOffset, length);
        } else {
            // Wrapped
            System.arraycopy(data, srcOffset, dest, destOffset, availableToEnd);
            System.arraycopy(data, 0, dest, destOffset + availableToEnd, length - availableToEnd);
        }
    }

}
