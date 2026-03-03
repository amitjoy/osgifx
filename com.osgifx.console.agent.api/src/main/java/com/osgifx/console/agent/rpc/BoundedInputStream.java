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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An {@link InputStream} wrapper that enforces a maximum number of bytes that can be read
 * from the underlying stream. This is used to protect against zip bomb attacks where a small
 * compressed payload can decompress to gigabytes of data, causing memory exhaustion.
 *
 * <p>
 * When the limit is exceeded, an {@link IOException} is thrown with a descriptive message.
 *
 * <p>
 * <b>Example usage:</b>
 * 
 * <pre>
 * // Limit decompressed GZIP stream to 250 MB
 * InputStream bounded = new BoundedInputStream(new GZIPInputStream(compressedStream), 250 * 1024 * 1024);
 * </pre>
 *
 * @since 11.0
 */
public final class BoundedInputStream extends FilterInputStream {

    private final long maxBytes;
    private long       bytesRead;

    /**
     * Creates a bounded input stream.
     *
     * @param in the underlying input stream
     * @param maxBytes the maximum number of bytes that can be read
     * @throws IllegalArgumentException if maxBytes is negative
     */
    public BoundedInputStream(final InputStream in, final long maxBytes) {
        super(in);
        if (maxBytes < 0) {
            throw new IllegalArgumentException("maxBytes must be non-negative: " + maxBytes);
        }
        this.maxBytes  = maxBytes;
        this.bytesRead = 0;
    }

    @Override
    public int read() throws IOException {
        final int result = in.read();
        if (result != -1) {
            bytesRead++;
            checkLimit(1);
        }
        return result;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int result = in.read(b, off, len);
        if (result != -1) {
            bytesRead += result;
            checkLimit(result);
        }
        return result;
    }

    @Override
    public long skip(final long n) throws IOException {
        final long result = in.skip(n);
        bytesRead += result;
        checkLimit(result);
        return result;
    }

    /**
     * Returns the number of bytes read so far.
     *
     * @return the number of bytes read
     */
    public long getBytesRead() {
        return bytesRead;
    }

    /**
     * Returns the maximum number of bytes that can be read.
     *
     * @return the maximum number of bytes
     */
    public long getMaxBytes() {
        return maxBytes;
    }

    private void checkLimit(final long additionalBytes) throws IOException {
        if (bytesRead > maxBytes) {
            throw new IOException(String.format(
                    "Stream size limit exceeded: read %d bytes, limit is %d bytes (exceeded by %d bytes). "
                            + "This may indicate a zip bomb attack or an unexpectedly large payload.",
                    bytesRead, maxBytes, bytesRead - maxBytes));
        }
    }
}
