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
package com.osgifx.console.agent.rpc.codec;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility to decode LZ4-compressed binary snapshots from the agent.
 * <p>
 * This decoder handles decompression (using {@link Lz4Codec#decompressWithLength})
 * and deserialization (using {@link BinaryCodec#decode}) in a single operation.
 * <p>
 * Typical usage on the supervisor side:
 * 
 * <pre>{@code
 * SnapshotDecoder decoder = new SnapshotDecoder(codec);
 * byte[] snapshot = agent.bundles();
 * List<XBundleDTO> bundles = decoder.decodeList(snapshot, XBundleDTO.class);
 * }
 * </pre>
 *
 * @since 12.0
 */
public final class SnapshotDecoder {

    private final BinaryCodec codec;

    public SnapshotDecoder(final BinaryCodec codec) {
        this.codec = codec;
    }

    /**
     * Decompresses and decodes a snapshot into a list of DTOs.
     *
     * @param <T> the DTO type
     * @param snapshot the compressed binary snapshot
     * @param type the expected DTO type
     * @return the list of decoded DTOs, or an empty list if snapshot is null
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> decodeList(final byte[] snapshot, final Class<T> type) {
        if (snapshot == null || snapshot.length == 0) {
            return new ArrayList<>();
        }
        try {
            final byte[] decompressed = Lz4Codec.decompressWithLength(snapshot, Integer.MAX_VALUE);
            return (List<T>) codec.decode(decompressed, new ListParameterizedType(type));
        } catch (final IOException e) {
            throw new RuntimeException("Failed to decompress snapshot", e);
        }
    }

    /**
     * Decompresses and decodes a snapshot into a set of DTOs.
     *
     * @param <T> the DTO type
     * @param snapshot the compressed binary snapshot
     * @param type the expected DTO type
     * @return the set of decoded DTOs, or an empty set if snapshot is null
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> decodeSet(final byte[] snapshot, final Class<T> type) {
        if (snapshot == null || snapshot.length == 0) {
            return new HashSet<>();
        }
        try {
            final byte[] decompressed = Lz4Codec.decompressWithLength(snapshot, Integer.MAX_VALUE);
            return (Set<T>) codec.decode(decompressed, new SetParameterizedType(type));
        } catch (final IOException e) {
            throw new RuntimeException("Failed to decompress snapshot", e);
        }
    }

    /**
     * Decompresses and decodes a snapshot into a single DTO.
     *
     * @param <T> the DTO type
     * @param snapshot the compressed binary snapshot
     * @param type the expected DTO type
     * @return the decoded DTO, or null if snapshot is null
     */
    public <T> T decode(final byte[] snapshot, final Class<T> type) {
        if (snapshot == null || snapshot.length == 0) {
            return null;
        }
        try {
            final byte[] decompressed = Lz4Codec.decompressWithLength(snapshot, Integer.MAX_VALUE);
            return codec.decode(decompressed, type);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to decompress snapshot", e);
        }
    }

    private static class ListParameterizedType implements ParameterizedType {
        private final Class<?> type;

        private ListParameterizedType(final Class<?> type) {
            this.type = type;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[] { type };
        }

        @Override
        public Type getRawType() {
            return List.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }

    private static class SetParameterizedType implements ParameterizedType {
        private final Class<?> type;

        private SetParameterizedType(final Class<?> type) {
            this.type = type;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[] { type };
        }

        @Override
        public Type getRawType() {
            return Set.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }

}
