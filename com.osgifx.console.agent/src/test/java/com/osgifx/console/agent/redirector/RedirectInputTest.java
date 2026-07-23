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
package com.osgifx.console.agent.redirector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RedirectInputTest {

    private RedirectInput redirectInput;

    @BeforeEach
    public void setUp() {
        redirectInput = new RedirectInput();
    }

    @Test
    public void addAndReadSingleChar() throws IOException {
        redirectInput.add("A");
        assertEquals('A', redirectInput.read());
    }

    @Test
    public void addAndReadMultipleChars() throws IOException {
        redirectInput.add("Hi");
        assertEquals('H', redirectInput.read());
        assertEquals('i', redirectInput.read());
    }

    @Test
    public void addAndReadPreservesOrder() throws IOException {
        redirectInput.add("A");
        redirectInput.add("B");
        redirectInput.add("C");
        
        assertEquals('A', redirectInput.read());
        assertEquals('B', redirectInput.read());
        assertEquals('C', redirectInput.read());
    }

    @Test
    public void ringBufferOverwritesOldData() throws IOException {
        // The buffer size is 65536 bytes
        StringBuilder sb = new StringBuilder(65536);
        sb.append("0123456789"); // First 10 bytes are unique
        for (int i = 0; i < 65526; i++) {
            sb.append('A');
        }
        redirectInput.add(sb.toString());

        // Now we write 3 more bytes, this will overwrite the oldest 3 bytes ("012")
        redirectInput.add("XYZ");

        // The oldest 4 bytes ('0', '1', '2', '3') should be skipped because buffer capacity is actually 65535.
        assertEquals('4', redirectInput.read());
        assertEquals('5', redirectInput.read());
        assertEquals('6', redirectInput.read());
    }

    @Test
    public void readBlocksUntilDataAdded() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            Thread asyncAddThread = new Thread(() -> {
                try {
                    Thread.sleep(50);
                    redirectInput.add("X");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            asyncAddThread.start();
            
            // This read() call should block until the thread adds 'X'
            assertEquals('X', redirectInput.read());
        });
    }

    @Test
    public void getOrgReturnsOriginalStream() {
        InputStream mockStream = new ByteArrayInputStream(new byte[0]);
        RedirectInput withOrg = new RedirectInput(mockStream);
        
        assertSame(mockStream, withOrg.getOrg());
    }
}
