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
package com.osgifx.console.util.io;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class IOTest {

    @Test
    public void prepareFilenameForPdf() {
        String filename = IO.prepareFilenameFor("pdf");
        assertTrue(filename.startsWith("OSGi.fx"));
        assertTrue(filename.endsWith(".pdf"));
    }

    @Test
    public void prepareFilenameForJson() {
        String filename = IO.prepareFilenameFor("json");
        assertTrue(filename.startsWith("OSGi.fx"));
        assertTrue(filename.endsWith(".json"));
    }

    @Test
    public void prepareFilenameContainsTimestamp() throws InterruptedException {
        String filename1 = IO.prepareFilenameFor("txt");
        Thread.sleep(10); // Ensure timestamp difference
        String filename2 = IO.prepareFilenameFor("txt");
        assertNotEquals(filename1, filename2);
    }

    @Test
    public void prepareFilenameNoNullOrEmpty() {
        String filename = IO.prepareFilenameFor("csv");
        assertNotNull(filename);
        assertFalse(filename.trim().isEmpty());
    }
}
