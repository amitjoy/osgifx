/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.rpc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

/**
 * A High-Performance, Hybrid (Unsafe + Reflection) binary codec for OSGi DTOs.
 * *
 * <p>
 * <b>Features:</b>
 * </p>
 * <ul>
 * <li><b>Adaptive:</b> Automatically detects if {@code sun.misc.Unsafe} is wired in the OSGi environment.</li>
 * <li><b>Schema-less:</b> Removes field names from payload (relies on deterministic field order).</li>
 * <li><b>Zero-Boxing:</b> Primitives are read/written directly without object allocation in Unsafe mode.</li>
 * <li><b>Collection Support:</b> Transparently handles Sets, Lists, Maps, and Arrays.</li>
 * </ul>
 */
public class BinaryCodec {

    private static final CodecStrategy                  strategy;
    private static final Map<Class<?>, FieldAccessor[]> accessorCache = new ConcurrentHashMap<>();

    // --- STATIC INITIALIZER: OSGi WIRING CHECK ---
    static {
        CodecStrategy selected = new ReflectionStrategy(); // Default to Safe Mode
        try {
            // Get the Bundle containing this Codec
            Bundle  bundle       = FrameworkUtil.getBundle(BinaryCodec.class);
            boolean canUseUnsafe = false;

            if (bundle != null) {
                // Inspect the Wiring (OSGi Capabilities)
                BundleWiring wiring = bundle.adapt(BundleWiring.class);
                if (wiring != null) {
                    List<BundleWire> imports = wiring.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);

                    // Check if we are explicitly wired to 'sun.misc'
                    // This avoids ClassLoader hacks and respects OSGi security
                    canUseUnsafe = imports.stream().anyMatch(wire -> "sun.misc"
                            .equals(wire.getCapability().getAttributes().get(BundleRevision.PACKAGE_NAMESPACE)));
                }
            } else {
                // Fallback for Non-OSGi environments (e.g., Unit Tests)
                try {
                    Class.forName("sun.misc.Unsafe");
                    canUseUnsafe = true;
                } catch (Throwable ignore) {
                }
            }

            if (canUseUnsafe) {
                // Double-check: Try to actually instantiate it
                selected = new UnsafeStrategy();
            }
        } catch (Throwable t) {
            // Fallback to Reflection if anything fails (SecurityManager, etc.)
            selected = new ReflectionStrategy();
        }
        strategy = selected;
    }

    // Type Tags
    private static final byte NULL = 0, BOOL = 1, BYTE = 2, SHORT = 3, INT = 4, LONG = 5, FLOAT = 6, DOUBLE = 7,
            STRING = 8, LIST = 9, DTO = 10, ARRAY = 11, ENUM = 12, MAP = 13, CHAR = 14;

    public boolean isUsingUnsafe() {
        return strategy instanceof UnsafeStrategy;
    }

    // --- ENCODER ---

    public void encode(Object obj, DataOutputStream out) throws Exception {
        if (obj == null) {
            out.writeByte(NULL);
            return;
        }

        Class<?> clz = obj.getClass();

        // Primitive Handling (Standard Java Wrapper Types)
        if (clz == Integer.class) {
            out.writeByte(INT);
            out.writeInt((Integer) obj);
        } else if (clz == String.class) {
            out.writeByte(STRING);
            out.writeUTF((String) obj);
        } else if (clz == Boolean.class) {
            out.writeByte(BOOL);
            out.writeBoolean((Boolean) obj);
        } else if (clz == Long.class) {
            out.writeByte(LONG);
            out.writeLong((Long) obj);
        } else if (clz == Double.class) {
            out.writeByte(DOUBLE);
            out.writeDouble((Double) obj);
        } else if (clz == Float.class) {
            out.writeByte(FLOAT);
            out.writeFloat((Float) obj);
        } else if (clz == Byte.class) {
            out.writeByte(BYTE);
            out.writeByte((Byte) obj);
        } else if (clz == Short.class) {
            out.writeByte(SHORT);
            out.writeShort((Short) obj);
        } else if (clz == Character.class) {
            out.writeByte(CHAR);
            out.writeChar((Character) obj);
        }

        // Enum Handling
        else if (clz.isEnum()) {
            out.writeByte(ENUM);
            out.writeUTF(((Enum<?>) obj).name());
        }

        // Collection Handling
        else if (Collection.class.isAssignableFrom(clz)) {
            out.writeByte(LIST);
            Collection<?> col = (Collection<?>) obj;
            out.writeInt(col.size());
            for (Object item : col)
                encode(item, out);
        }
        // Map Handling
        else if (Map.class.isAssignableFrom(clz)) {
            out.writeByte(MAP);
            Map<?, ?> map = (Map<?, ?>) obj;
            out.writeInt(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                encode(entry.getKey(), out);
                encode(entry.getValue(), out);
            }
        }
        // Array Handling
        else if (clz.isArray()) {
            if (clz.getComponentType() == byte.class) {
                // Optimization for byte[] (Common in OSGi)
                out.writeByte(ARRAY);
                out.writeUTF("[B");
                byte[] bytes = (byte[]) obj;
                out.writeInt(bytes.length);
                out.write(bytes);
            } else {
                out.writeByte(LIST);
                int len = Array.getLength(obj);
                out.writeInt(len);
                for (int i = 0; i < len; i++)
                    encode(Array.get(obj, i), out);
            }
        }
        // DTO Handling
        else {
            out.writeByte(DTO);
            FieldAccessor[] accessors = getAccessors(clz);
            for (FieldAccessor acc : accessors) {
                acc.encode(obj, out, this);
            }
        }
    }

    // --- DECODER ---

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object decode(DataInputStream in, Type type) throws Exception {
        byte tag = in.readByte();
        if (tag == NULL)
            return null;

        // Primitive/Basic Type Handling based on Class Type
        if (type instanceof Class) {
            Class<?> clz = (Class<?>) type;
            if (clz == Boolean.class || clz == boolean.class)
                return in.readBoolean();
            if (clz == Integer.class || clz == int.class)
                return in.readInt();
            if (clz == String.class)
                return in.readUTF();
            if (clz == Long.class || clz == long.class)
                return in.readLong();
            if (clz == Double.class || clz == double.class)
                return in.readDouble();
            if (clz == Float.class || clz == float.class)
                return in.readFloat();
            if (clz == Byte.class || clz == byte.class)
                return in.readByte();
            if (clz == Short.class || clz == short.class)
                return in.readShort();
            if (clz == Character.class || clz == char.class)
                return in.readChar();
            if (clz.isEnum())
                return Enum.valueOf((Class<Enum>) clz, in.readUTF());
            if (clz.isArray() && tag == ARRAY) {
                in.readUTF(); // Skip type identifier
                int    len  = in.readInt();
                byte[] data = new byte[len];
                in.readFully(data);
                return data;
            }
        }

        // Untyped Object Handling (e.g. Map<String, Object>)
        // Reads the tag to determine the concrete type
        if (type == Object.class) {
            switch (tag) {
                case BOOL:
                    return in.readBoolean();
                case INT:
                    return in.readInt();
                case LONG:
                    return in.readLong();
                case DOUBLE:
                    return in.readDouble();
                case FLOAT:
                    return in.readFloat();
                case BYTE:
                    return in.readByte();
                case SHORT:
                    return in.readShort();
                case CHAR:
                    return in.readChar();
                case STRING:
                    return in.readUTF();
                case ENUM:
                    return in.readUTF(); // Unknown enum type, return String
                case LIST:
                    type = List.class;
                    break;
                case MAP:
                    type = Map.class;
                    break;
            }
        }

        // Collection Handling
        if (tag == LIST) {
            int          size = in.readInt();
            List<Object> list = new ArrayList<>(size);

            Type itemType = Object.class;
            if (type instanceof ParameterizedType) {
                itemType = ((ParameterizedType) type).getActualTypeArguments()[0];
            } else if (type instanceof Class && ((Class<?>) type).isArray()) {
                itemType = ((Class<?>) type).getComponentType();
            }

            for (int i = 0; i < size; i++) {
                list.add(decode(in, itemType));
            }

            // Array Conversion
            if (type instanceof Class && ((Class<?>) type).isArray()) {
                Class<?> compType = ((Class<?>) type).getComponentType();
                Object   arr      = Array.newInstance(compType, size);
                for (int i = 0; i < size; i++)
                    Array.set(arr, i, list.get(i));
                return arr;
            }

            // Set Conversion (Fix for Set<String> return types)
            Class<?> rawType = (type instanceof Class) ? (Class<?>) type
                    : (type instanceof ParameterizedType) ? (Class<?>) ((ParameterizedType) type).getRawType() : null;
            if (rawType != null && Set.class.isAssignableFrom(rawType)) {
                return new LinkedHashSet<>(list);
            }
            return list;
        }

        // Map Handling
        if (tag == MAP) {
            int                 size    = in.readInt();
            Map<Object, Object> map     = new LinkedHashMap<>(size);
            Type                keyType = Object.class;
            Type                valType = Object.class;
            if (type instanceof ParameterizedType) {
                Type[] args = ((ParameterizedType) type).getActualTypeArguments();
                keyType = args[0];
                valType = args[1];
            }
            for (int i = 0; i < size; i++) {
                map.put(decode(in, keyType), decode(in, valType));
            }
            return map;
        }

        // DTO Handling (Strategy Delegated)
        if (tag == DTO) {
            Class<?> clz = (Class<?>) type;
            // Use strategy to create instance (Unsafe allocates without constructor)
            Object instance = strategy.newInstance(clz);

            FieldAccessor[] accessors = getAccessors(clz);
            for (FieldAccessor acc : accessors) {
                acc.decode(instance, in, this);
            }
            return instance;
        }

        throw new IOException("Unknown tag: " + tag);
    }

    private FieldAccessor[] getAccessors(Class<?> clz) {
        return accessorCache.computeIfAbsent(clz, c -> {
            return Arrays.stream(c.getFields()).filter(f -> !Modifier.isStatic(f.getModifiers()))
                    .sorted(Comparator.comparing(Field::getName)).map(f -> strategy.createAccessor(f))
                    .toArray(FieldAccessor[]::new);
        });
    }

    // ==================================================================================
    // STRATEGY & ACCESSOR INTERFACES
    // ==================================================================================

    interface CodecStrategy {
        Object newInstance(Class<?> clz) throws Exception;

        FieldAccessor createAccessor(Field f);
    }

    interface FieldAccessor {
        void encode(Object instance, DataOutputStream out, BinaryCodec codec) throws Exception;

        void decode(Object instance, DataInputStream in, BinaryCodec codec) throws Exception;
    }

    // ==================================================================================
    // REFLECTION IMPLEMENTATION (SAFE FALLBACK)
    // ==================================================================================

    static class ReflectionStrategy implements CodecStrategy {
        @Override
        public Object newInstance(Class<?> clz) throws Exception {
            return clz.getDeclaredConstructor().newInstance();
        }

        @Override
        public FieldAccessor createAccessor(Field f) {
            return new ReflAccessor(f);
        }
    }

    static class ReflAccessor implements FieldAccessor {
        final Field    field;
        final Class<?> type;

        ReflAccessor(Field f) {
            this.field = f;
            this.type  = f.getType();
        }

        @Override
        public void encode(Object instance, DataOutputStream out, BinaryCodec codec) throws Exception {
            if (type == int.class) {
                out.writeByte(INT);
                out.writeInt(field.getInt(instance));
            } else if (type == boolean.class) {
                out.writeByte(BOOL);
                out.writeBoolean(field.getBoolean(instance));
            } else if (type == long.class) {
                out.writeByte(LONG);
                out.writeLong(field.getLong(instance));
            } else if (type == String.class) {
                out.writeByte(STRING);
                out.writeUTF((String) field.get(instance));
            } else {
                codec.encode(field.get(instance), out);
            }
        }

        @Override
        public void decode(Object instance, DataInputStream in, BinaryCodec codec) throws Exception {
            Object val = codec.decode(in, field.getGenericType());
            field.set(instance, val);
        }
    }

    // ==================================================================================
    // UNSAFE IMPLEMENTATION (HIGH PERFORMANCE)
    // ==================================================================================

    @SuppressWarnings("restriction")
    static class UnsafeStrategy implements CodecStrategy {
        private final sun.misc.Unsafe unsafe;

        UnsafeStrategy() throws Exception {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            this.unsafe = (sun.misc.Unsafe) f.get(null);
        }

        @Override
        public Object newInstance(Class<?> clz) throws Exception {
            return unsafe.allocateInstance(clz); // Skips constructor for speed
        }

        @Override
        public FieldAccessor createAccessor(Field f) {
            long     offset = unsafe.objectFieldOffset(f);
            Class<?> type   = f.getType();
            if (type == int.class)
                return new UnsafeInt(unsafe, offset);
            if (type == boolean.class)
                return new UnsafeBoolean(unsafe, offset);
            if (type == long.class)
                return new UnsafeLong(unsafe, offset);
            if (type == double.class)
                return new UnsafeDouble(unsafe, offset);
            if (type == float.class)
                return new UnsafeFloat(unsafe, offset);
            if (type == byte.class)
                return new UnsafeByte(unsafe, offset);
            if (type == short.class)
                return new UnsafeShort(unsafe, offset);
            if (type == char.class)
                return new UnsafeChar(unsafe, offset);
            return new UnsafeObject(unsafe, offset, f.getGenericType());
        }
    }

    @SuppressWarnings("restriction")
    static class UnsafeInt implements FieldAccessor {
        final sun.misc.Unsafe unsafe;
        final long            offset;

        UnsafeInt(sun.misc.Unsafe u, long o) {
            unsafe = u;
            offset = o;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws IOException {
            out.writeByte(INT);
            out.writeInt(unsafe.getInt(i, offset));
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != INT)
                throw new IOException("Exp INT");
            unsafe.putInt(i, offset, in.readInt());
        }
    }

    @SuppressWarnings("restriction")
    static class UnsafeBoolean implements FieldAccessor {
        final sun.misc.Unsafe unsafe;
        final long            offset;

        UnsafeBoolean(sun.misc.Unsafe u, long o) {
            unsafe = u;
            offset = o;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws IOException {
            out.writeByte(BOOL);
            out.writeBoolean(unsafe.getBoolean(i, offset));
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != BOOL)
                throw new IOException("Exp BOOL");
            unsafe.putBoolean(i, offset, in.readBoolean());
        }
    }

    @SuppressWarnings("restriction")
    static class UnsafeLong implements FieldAccessor {
        final sun.misc.Unsafe unsafe;
        final long            offset;

        UnsafeLong(sun.misc.Unsafe u, long o) {
            unsafe = u;
            offset = o;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws IOException {
            out.writeByte(LONG);
            out.writeLong(unsafe.getLong(i, offset));
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != LONG)
                throw new IOException("Exp LONG");
            unsafe.putLong(i, offset, in.readLong());
        }
    }

    @SuppressWarnings("restriction")
    static class UnsafeDouble implements FieldAccessor {
        final sun.misc.Unsafe unsafe;
        final long            offset;

        UnsafeDouble(sun.misc.Unsafe u, long o) {
            unsafe = u;
            offset = o;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws IOException {
            out.writeByte(DOUBLE);
            out.writeDouble(unsafe.getDouble(i, offset));
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != DOUBLE)
                throw new IOException("Exp DOUBLE");
            unsafe.putDouble(i, offset, in.readDouble());
        }
    }

    @SuppressWarnings("restriction")
    static class UnsafeFloat implements FieldAccessor {
        final sun.misc.Unsafe unsafe;
        final long            offset;

        UnsafeFloat(sun.misc.Unsafe u, long o) {
            unsafe = u;
            offset = o;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws IOException {
            out.writeByte(FLOAT);
            out.writeFloat(unsafe.getFloat(i, offset));
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != FLOAT)
                throw new IOException("Exp FLOAT");
            unsafe.putFloat(i, offset, in.readFloat());
        }
    }

    @SuppressWarnings("restriction")
    static class UnsafeByte implements FieldAccessor {
        final sun.misc.Unsafe unsafe;
        final long            offset;

        UnsafeByte(sun.misc.Unsafe u, long o) {
            unsafe = u;
            offset = o;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws IOException {
            out.writeByte(BYTE);
            out.writeByte(unsafe.getByte(i, offset));
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != BYTE)
                throw new IOException("Exp BYTE");
            unsafe.putByte(i, offset, in.readByte());
        }
    }

    @SuppressWarnings("restriction")
    static class UnsafeShort implements FieldAccessor {
        final sun.misc.Unsafe unsafe;
        final long            offset;

        UnsafeShort(sun.misc.Unsafe u, long o) {
            unsafe = u;
            offset = o;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws IOException {
            out.writeByte(SHORT);
            out.writeShort(unsafe.getShort(i, offset));
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != SHORT)
                throw new IOException("Exp SHORT");
            unsafe.putShort(i, offset, in.readShort());
        }
    }

    @SuppressWarnings("restriction")
    static class UnsafeChar implements FieldAccessor {
        final sun.misc.Unsafe unsafe;
        final long            offset;

        UnsafeChar(sun.misc.Unsafe u, long o) {
            unsafe = u;
            offset = o;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws IOException {
            out.writeByte(CHAR);
            out.writeChar(unsafe.getChar(i, offset));
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() == CHAR) {
                unsafe.putChar(i, offset, in.readChar());
            } else
                throw new IOException("Exp CHAR");
        }
    }

    @SuppressWarnings("restriction")
    static class UnsafeObject implements FieldAccessor {
        final sun.misc.Unsafe unsafe;
        final long            offset;
        final Type            type;

        UnsafeObject(sun.misc.Unsafe u, long o, Type t) {
            unsafe = u;
            offset = o;
            type   = t;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            c.encode(unsafe.getObject(i, offset), out);
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            unsafe.putObject(i, offset, c.decode(in, type));
        }
    }
}