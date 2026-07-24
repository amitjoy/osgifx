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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BinaryLogBufferTest {

    @TempDir
    public File folder;

    @Test
    public void testWrapAround() {
        BinaryLogBuffer buffer = new BinaryLogBuffer(50, 10);
        buffer.write(1000L, 1L, 3, "AAAAA", null);
        buffer.write(2000L, 2L, 1, "BBBBB", null);

        byte[] snapshot = buffer.getLogSnapshot(1);
        assertEquals(33, snapshot.length);

        ByteBuffer bb = ByteBuffer.wrap(snapshot);
        assertEquals(2000L, bb.getLong());
    }

    @Test
    public void testPersistence() throws Exception {
        File file = new File(folder, "logs.bin");
        BinaryLogBuffer buffer1 = new BinaryLogBuffer(100, 10);
        buffer1.write(1234L, 5L, 2, "PersistMe", null);
        buffer1.toDisk(file);

        BinaryLogBuffer buffer2 = new BinaryLogBuffer(100, 10);
        buffer2.fromDisk(file);

        byte[] snap = buffer2.getLogSnapshot(1);
        assertTrue(snap.length > 0);

        ByteBuffer bb = ByteBuffer.wrap(snap);
        assertEquals(1234L, bb.getLong());
    }

    @Test
    public void testTimeRangeQuery() {
        BinaryLogBuffer buffer = new BinaryLogBuffer(1024, 10);
        buffer.write(1000L, 1L, 1, "Msg1", null);
        buffer.write(2000L, 1L, 1, "Msg2", null);
        buffer.write(3000L, 1L, 1, "Msg3", null);
        buffer.write(4000L, 1L, 1, "Msg4", null);
        buffer.write(5000L, 1L, 1, "Msg5", null);

        assertEquals(3, countEntries(buffer.getLogSnapshot(2000L, 4000L)));
        assertEquals(2000L, getFirstTimestamp(buffer.getLogSnapshot(2000L, 4000L)));

        assertEquals(2, countEntries(buffer.getLogSnapshot(2500L, 4500L)));
        assertEquals(3000L, getFirstTimestamp(buffer.getLogSnapshot(2500L, 4500L)));

        assertEquals(0, countEntries(buffer.getLogSnapshot(0L, 900L)));
        assertEquals(0, countEntries(buffer.getLogSnapshot(6000L, 7000L)));

        assertEquals(1, countEntries(buffer.getLogSnapshot(3000L, 3000L)));
        assertEquals(3000L, getFirstTimestamp(buffer.getLogSnapshot(3000L, 3000L)));
    }

    @Test
    public void testTimeRangeWrapAround() {
        BinaryLogBuffer buffer = new BinaryLogBuffer(100, 5);
        for (int i = 1; i <= 10; i++) {
            buffer.write(i * 1000L, 1L, 1, "Msg" + i, null);
        }

        byte[] range = buffer.getLogSnapshot(8000L, 9000L);
        assertEquals(2, countEntries(range));
        assertEquals(8000L, getFirstTimestamp(range));
    }

    @Test
    public void emptyBufferSnapshotReturnsEmptyArray() {
        BinaryLogBuffer buffer = new BinaryLogBuffer(1024, 10);
        byte[] snapshot = buffer.getLogSnapshot(10);
        assertTrue(snapshot == null || snapshot.length == 0);
    }

    @Test
    public void singleEntryNoException() {
        BinaryLogBuffer buffer = new BinaryLogBuffer(1024, 10);
        buffer.write(1000L, 1L, 1, "NoException", null);
        byte[] snapshot = buffer.getLogSnapshot(10);
        
        assertNotNull(snapshot);
        assertEquals(1, countEntries(snapshot));
    }

    @Test
    public void singleEntryWithExceptionMessage() {
        BinaryLogBuffer buffer = new BinaryLogBuffer(1024, 10);
        buffer.write(1000L, 1L, 1, "WithException", "java.lang.RuntimeException: Error");
        byte[] snapshot = buffer.getLogSnapshot(10);
        
        assertNotNull(snapshot);
        assertEquals(1, countEntries(snapshot));
    }

    @Test
    public void countSnapshotAfterMultipleWrites() {
        BinaryLogBuffer buffer = new BinaryLogBuffer(1024, 10);
        for (int i = 1; i <= 5; i++) {
            buffer.write(i * 1000L, 1L, 1, "Message" + i, null);
        }
        
        byte[] snapshot = buffer.getLogSnapshot(3);
        assertNotNull(snapshot);
        assertEquals(3, countEntries(snapshot));
    }

    @Test
    public void indexWrapAround() {
        BinaryLogBuffer buffer = new BinaryLogBuffer(1024, 2); // only 2 slots
        for (int i = 1; i <= 5; i++) {
            buffer.write(i * 1000L, 1L, 1, "Message" + i, null);
        }
        
        // No exception should be thrown above
        byte[] snapshot = buffer.getLogSnapshot(10);
        assertNotNull(snapshot);
        // Depending on slot overwrites, count might be <= 2
        assertTrue(countEntries(snapshot) <= 2);
    }

    private int countEntries(byte[] data) {
        if (data == null || data.length == 0)
            return 0;
        ByteBuffer bb = ByteBuffer.wrap(data);
        int count = 0;
        while (bb.hasRemaining()) {
            if (bb.remaining() < 28)
                break;
            bb.getLong(); // timestamp
            bb.getLong(); // bundleId
            bb.getInt(); // level
            int msgLen = bb.getInt();
            int excLen = bb.getInt();
            bb.position(bb.position() + msgLen + excLen);
            count++;
        }
        return count;
    }

    private long getFirstTimestamp(byte[] data) {
        if (data == null || data.length < 8)
            return -1;
        return ByteBuffer.wrap(data).getLong();
    }
}
