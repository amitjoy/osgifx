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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.ByteBuffer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BinaryLogBufferTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testWrapAround() {
        // Small buffer: 50 bytes. Index: 10 slots
        BinaryLogBuffer buffer = new BinaryLogBuffer(50, 10);

        // Write Log A (20 bytes approx)
        buffer.write(1000L, 1L, 3, "AAAAA", null);
        // 28 header + 5 msg = 33 bytes.
        // head = 33.

        // Write Log B (Wrap)
        buffer.write(2000L, 2L, 1, "BBBBB", null);
        // 28 header + 5 msg = 33 bytes needed.
        // Remaining at end: 50 - 33 = 17.
        // It writes 17 bytes at end, 16 bytes at start.
        // head = 16.

        // Read Last Log (B)
        byte[] snapshot = buffer.getLogSnapshot(1);

        // Manual decode or just check length
        // Size should be 33 bytes
        assertEquals(33, snapshot.length);

        // Verify Content (Timestamp 2000L at start)
        ByteBuffer bb = ByteBuffer.wrap(snapshot);
        long       ts = bb.getLong();
        assertEquals(2000L, ts);
    }

    @Test
    public void testPersistence() throws Exception {
        File            file    = folder.newFile("logs.bin");
        BinaryLogBuffer buffer1 = new BinaryLogBuffer(100, 10);
        buffer1.write(1234L, 5L, 2, "PersistMe", null);

        buffer1.toDisk(file);

        BinaryLogBuffer buffer2 = new BinaryLogBuffer(100, 10);
        buffer2.fromDisk(file);

        byte[] snap = buffer2.getLogSnapshot(1);
        assertEquals(1, snap.length > 0 ? 1 : 0); // Just ensure we got something

        ByteBuffer bb = ByteBuffer.wrap(snap);
        assertEquals(1234L, bb.getLong());
    }

    @Test
    public void testTimeRangeQuery() {
        // Buffer large enough to hold 5 entries
        BinaryLogBuffer buffer = new BinaryLogBuffer(1024, 10);

        // timestamps: 1000, 2000, 3000, 4000, 5000
        buffer.write(1000L, 1L, 1, "Msg1", null);
        buffer.write(2000L, 1L, 1, "Msg2", null);
        buffer.write(3000L, 1L, 1, "Msg3", null);
        buffer.write(4000L, 1L, 1, "Msg4", null);
        buffer.write(5000L, 1L, 1, "Msg5", null);

        // Case 1: Exact Range (2000-4000) -> Should get Msg2, Msg3, Msg4
        byte[] range1 = buffer.getLogSnapshot(2000L, 4000L);
        assertEquals(3, countEntries(range1));
        assertEquals(2000L, getFirstTimestamp(range1));

        // Case 2: Partial Range (2500-4500) -> Should get Msg3, Msg4
        byte[] range2 = buffer.getLogSnapshot(2500L, 4500L);
        assertEquals(2, countEntries(range2));
        assertEquals(3000L, getFirstTimestamp(range2));

        // Case 3: Too old (0-900) -> Empty
        byte[] range3 = buffer.getLogSnapshot(0L, 900L);
        assertEquals(0, countEntries(range3));

        // Case 4: Too new (6000-7000) -> Empty
        byte[] range4 = buffer.getLogSnapshot(6000L, 7000L);
        assertEquals(0, countEntries(range4));

        // Case 5: Single Item (3000-3000) -> Msg3
        byte[] range5 = buffer.getLogSnapshot(3000L, 3000L);
        assertEquals(1, countEntries(range5));
        assertEquals(3000L, getFirstTimestamp(range5));
    }

    @Test
    public void testTimeRangeWrapAround() {
        // Small buffer to force wrap
        // Index size 5 to force index wrap as well
        BinaryLogBuffer buffer = new BinaryLogBuffer(100, 5);

        // Write 10 entries (timestamps 1000-10000)
        // Only last ~3-4 will survive in data buffer, last 5 in index
        for (int i = 1; i <= 10; i++) {
            buffer.write(i * 1000L, 1L, 1, "Msg" + i, null);
        }

        // Current state approx: Msg7, Msg8, Msg9, Msg10 are likely in buffer
        // Request range for Msg8..Msg9 (8000-9000)
        byte[] range = buffer.getLogSnapshot(8000L, 9000L);

        // Should return 2 entries
        assertEquals(2, countEntries(range));
        assertEquals(8000L, getFirstTimestamp(range));
    }

    private int countEntries(byte[] data) {
        if (data.length == 0)
            return 0;
        ByteBuffer bb    = ByteBuffer.wrap(data);
        int        count = 0;
        while (bb.hasRemaining()) {
            // Read 28-byte header
            if (bb.remaining() < 28)
                break;
            bb.getLong(); // timestamp
            bb.getLong(); // bundleId
            bb.getInt(); // level
            int msgLen = bb.getInt();
            int excLen = bb.getInt();
            // Skip bodies
            bb.position(bb.position() + msgLen + excLen);
            count++;
        }
        return count;
    }

    private long getFirstTimestamp(byte[] data) {
        if (data.length < 8)
            return -1;
        return ByteBuffer.wrap(data).getLong();
    }
}
