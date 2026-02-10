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

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An filter stream that takes a string from the supervisor and then provides it
 * as a read to the user of this Input Stream. It also dispatches the text to an
 * original stream.It does not use Piped*Stream because they turned hard to
 * break. This implementation uses a ring buffer.
 */
public class RedirectInput extends InputStream {

    private InputStream             org;
    private final byte[]            ring = new byte[65536];
    private int                     in;
    private int                     out;
    private final ReentrantLock     ringLock = new ReentrantLock();
    private final Condition         dataAvailable = ringLock.newCondition();
    private final ReentrantLock     lock = new ReentrantLock();

    /**
     * Create a redirector input stream with an original input stream
     *
     * @param in the original
     */
    public RedirectInput(final InputStream in) {
        org = in;
    }

    /**
     * Create a redirector without an original
     */
    public RedirectInput() {
    }

    /**
     * Get the original input stream, potentially null
     *
     * @return null or the original input stream
     */
    public InputStream getOrg() {
        return org;
    }

    /**
     * Provide the string that should be treated as input for the running code.
     */
    public void add(final String s) throws IOException {
        lock.lock();
        try {
            final byte[] bytes = s.getBytes();
            for (final byte element : bytes) {
                write(element);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Write to the ring buffer
     */
    private void write(final byte b) {
        ringLock.lock();
        try {
            ring[in] = b;
            in       = (in + 1) % ring.length;
            if (in == out) {
                // skip oldest output
                out = (out + 1) % ring.length;
            }
            dataAvailable.signalAll();
        } finally {
            ringLock.unlock();
        }
    }

    @Override
    public void close() {
        // ignore
    }

    /**
     * Read a byte from the input buffer. We will be fully interruptible, in the
     * case of an interrupt we return -1 (eof)
     */
    @Override
    public int read() throws IOException {
        System.out.flush();
        ringLock.lock();
        try {
            while (in == out) {
                try {
                    dataAvailable.await(400, java.util.concurrent.TimeUnit.MILLISECONDS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return -1;
                }
            }
            final int c = 0xFF & ring[out];
            out = (out + 1) % ring.length;
            return c;
        } finally {
            ringLock.unlock();
        }
    }

    /**
     * And the read for a larger buffer
     */
    @Override
    public int read(final byte[] buffer, final int offset, final int length) throws IOException {
        int n = 0;
        for (int i = offset; i < length; i++) {
            final int c = read();
            if (c < 0) {
                break;
            }
            buffer[i] = (byte) (0xFF & c);
            n++;
            if (c == '\n') {
                break;
            }
        }
        return n;
    }

    @Override
    public int read(final byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }
}
