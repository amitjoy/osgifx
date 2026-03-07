/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.osgifx.console.agent.rpc.lz4;

import static java.lang.String.format;

import java.nio.Buffer;
import java.nio.ByteOrder;

import com.osgifx.console.agent.rpc.UnsafeMemory;

final class UnsafeUtil {
    private static final long ADDRESS_OFFSET;

    private UnsafeUtil() {
    }

    static {
        ByteOrder order = ByteOrder.nativeOrder();
        if (!order.equals(ByteOrder.LITTLE_ENDIAN)) {
            throw new IncompatibleJvmException(format("LZ4 requires a little endian platform (found %s)", order));
        }

        try {
            // fetch the address field for direct buffers
            ADDRESS_OFFSET = UnsafeMemory.objectFieldOffset(Buffer.class.getDeclaredField("address"));
        } catch (NoSuchFieldException e) {
            throw new IncompatibleJvmException("LZ4 requires access to java.nio.Buffer raw address field");
        }
    }

    public static long getAddress(Buffer buffer) {
        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("buffer is not direct");
        }

        return UnsafeMemory.getLong(buffer, ADDRESS_OFFSET);
    }
}
