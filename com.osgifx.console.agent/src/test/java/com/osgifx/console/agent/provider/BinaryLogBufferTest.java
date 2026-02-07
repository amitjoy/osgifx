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
        File file = folder.newFile("logs.bin");

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
}
