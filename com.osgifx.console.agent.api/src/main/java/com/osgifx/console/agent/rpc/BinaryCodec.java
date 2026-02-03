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
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
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
 *
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
                try {
                    selected = new UnsafeStrategy();
                } catch (Throwable t) {
                    // Unsafe initialization failed (e.g. security restricted), fallback to reflection
                    t.printStackTrace(); // Optional: log failure
                }
            }
        } catch (Throwable t) {
            // Fallback to Reflection if anything fails
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
                    return in.readUTF();
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

            // Set Conversion
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

        // DTO Handling
        if (tag == DTO) {
            Class<?>        clz       = (Class<?>) type;
            Object          instance  = strategy.newInstance(clz);
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
    // REFLECTIVE UNSAFE IMPLEMENTATION (HIGH PERFORMANCE VIA METHODHANDLES)
    // ==================================================================================

    static class UnsafeStrategy implements CodecStrategy {
        private final Object       unsafe;
        private final MethodHandle allocateInstance;
        private final MethodHandle objectFieldOffset;

        private final MethodHandle getInt;
        private final MethodHandle putInt;
        private final MethodHandle getBoolean;
        private final MethodHandle putBoolean;
        private final MethodHandle getLong;
        private final MethodHandle putLong;
        private final MethodHandle getFloat;
        private final MethodHandle putFloat;
        private final MethodHandle getDouble;
        private final MethodHandle putDouble;
        private final MethodHandle getByte;
        private final MethodHandle putByte;
        private final MethodHandle getShort;
        private final MethodHandle putShort;
        private final MethodHandle getChar;
        private final MethodHandle putChar;
        private final MethodHandle getObject;
        private final MethodHandle putObject;

        UnsafeStrategy() throws Exception {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field    f           = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            this.unsafe = f.get(null);

            MethodHandles.Lookup lookup = MethodHandles.lookup();

            this.allocateInstance  = lookup.unreflect(unsafeClass.getMethod("allocateInstance", Class.class));
            this.objectFieldOffset = lookup.unreflect(unsafeClass.getMethod("objectFieldOffset", Field.class));

            this.getInt     = lookup.unreflect(unsafeClass.getMethod("getInt", Object.class, long.class));
            this.putInt     = lookup.unreflect(unsafeClass.getMethod("putInt", Object.class, long.class, int.class));
            this.getBoolean = lookup.unreflect(unsafeClass.getMethod("getBoolean", Object.class, long.class));
            this.putBoolean = lookup
                    .unreflect(unsafeClass.getMethod("putBoolean", Object.class, long.class, boolean.class));
            this.getLong    = lookup.unreflect(unsafeClass.getMethod("getLong", Object.class, long.class));
            this.putLong    = lookup.unreflect(unsafeClass.getMethod("putLong", Object.class, long.class, long.class));
            this.getFloat   = lookup.unreflect(unsafeClass.getMethod("getFloat", Object.class, long.class));
            this.putFloat   = lookup
                    .unreflect(unsafeClass.getMethod("putFloat", Object.class, long.class, float.class));
            this.getDouble  = lookup.unreflect(unsafeClass.getMethod("getDouble", Object.class, long.class));
            this.putDouble  = lookup
                    .unreflect(unsafeClass.getMethod("putDouble", Object.class, long.class, double.class));
            this.getByte    = lookup.unreflect(unsafeClass.getMethod("getByte", Object.class, long.class));
            this.putByte    = lookup.unreflect(unsafeClass.getMethod("putByte", Object.class, long.class, byte.class));
            this.getShort   = lookup.unreflect(unsafeClass.getMethod("getShort", Object.class, long.class));
            this.putShort   = lookup
                    .unreflect(unsafeClass.getMethod("putShort", Object.class, long.class, short.class));
            this.getChar    = lookup.unreflect(unsafeClass.getMethod("getChar", Object.class, long.class));
            this.putChar    = lookup.unreflect(unsafeClass.getMethod("putChar", Object.class, long.class, char.class));
            this.getObject  = lookup.unreflect(unsafeClass.getMethod("getObject", Object.class, long.class));
            this.putObject  = lookup
                    .unreflect(unsafeClass.getMethod("putObject", Object.class, long.class, Object.class));
        }

        @Override
        public Object newInstance(Class<?> clz) throws Exception {
            try {
                return allocateInstance.invoke(unsafe, clz);
            } catch (Throwable t) {
                if (t instanceof Exception)
                    throw (Exception) t;
                throw new RuntimeException(t);
            }
        }

        @Override
        public FieldAccessor createAccessor(Field f) {
            try {
                long     offset = (long) objectFieldOffset.invoke(unsafe, f);
                Class<?> type   = f.getType();

                if (type == int.class)
                    return new UnsafeInt(unsafe, offset, getInt, putInt);
                if (type == boolean.class)
                    return new UnsafeBoolean(unsafe, offset, getBoolean, putBoolean);
                if (type == long.class)
                    return new UnsafeLong(unsafe, offset, getLong, putLong);
                if (type == double.class)
                    return new UnsafeDouble(unsafe, offset, getDouble, putDouble);
                if (type == float.class)
                    return new UnsafeFloat(unsafe, offset, getFloat, putFloat);
                if (type == byte.class)
                    return new UnsafeByte(unsafe, offset, getByte, putByte);
                if (type == short.class)
                    return new UnsafeShort(unsafe, offset, getShort, putShort);
                if (type == char.class)
                    return new UnsafeChar(unsafe, offset, getChar, putChar);
                return new UnsafeObject(unsafe, offset, f.getGenericType(), getObject, putObject);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to create unsafe accessor for " + f, t);
            }
        }
    }

    static class UnsafeInt implements FieldAccessor {
        final Object       unsafe;
        final long         offset;
        final MethodHandle get;
        final MethodHandle put;

        UnsafeInt(Object u, long o, MethodHandle g, MethodHandle p) {
            unsafe = u;
            offset = o;
            get    = g;
            put    = p;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            out.writeByte(INT);
            try {
                out.writeInt((int) get.invoke(unsafe, i, offset));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != INT)
                throw new IOException("Exp INT");
            try {
                put.invoke(unsafe, i, offset, in.readInt());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    static class UnsafeBoolean implements FieldAccessor {
        final Object       unsafe;
        final long         offset;
        final MethodHandle get;
        final MethodHandle put;

        UnsafeBoolean(Object u, long o, MethodHandle g, MethodHandle p) {
            unsafe = u;
            offset = o;
            get    = g;
            put    = p;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            out.writeByte(BOOL);
            try {
                out.writeBoolean((boolean) get.invoke(unsafe, i, offset));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != BOOL)
                throw new IOException("Exp BOOL");
            try {
                put.invoke(unsafe, i, offset, in.readBoolean());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    static class UnsafeLong implements FieldAccessor {
        final Object       unsafe;
        final long         offset;
        final MethodHandle get;
        final MethodHandle put;

        UnsafeLong(Object u, long o, MethodHandle g, MethodHandle p) {
            unsafe = u;
            offset = o;
            get    = g;
            put    = p;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            out.writeByte(LONG);
            try {
                out.writeLong((long) get.invoke(unsafe, i, offset));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != LONG)
                throw new IOException("Exp LONG");
            try {
                put.invoke(unsafe, i, offset, in.readLong());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    static class UnsafeDouble implements FieldAccessor {
        final Object       unsafe;
        final long         offset;
        final MethodHandle get;
        final MethodHandle put;

        UnsafeDouble(Object u, long o, MethodHandle g, MethodHandle p) {
            unsafe = u;
            offset = o;
            get    = g;
            put    = p;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            out.writeByte(DOUBLE);
            try {
                out.writeDouble((double) get.invoke(unsafe, i, offset));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != DOUBLE)
                throw new IOException("Exp DOUBLE");
            try {
                put.invoke(unsafe, i, offset, in.readDouble());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    static class UnsafeFloat implements FieldAccessor {
        final Object       unsafe;
        final long         offset;
        final MethodHandle get;
        final MethodHandle put;

        UnsafeFloat(Object u, long o, MethodHandle g, MethodHandle p) {
            unsafe = u;
            offset = o;
            get    = g;
            put    = p;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            out.writeByte(FLOAT);
            try {
                out.writeFloat((float) get.invoke(unsafe, i, offset));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != FLOAT)
                throw new IOException("Exp FLOAT");
            try {
                put.invoke(unsafe, i, offset, in.readFloat());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    static class UnsafeByte implements FieldAccessor {
        final Object       unsafe;
        final long         offset;
        final MethodHandle get;
        final MethodHandle put;

        UnsafeByte(Object u, long o, MethodHandle g, MethodHandle p) {
            unsafe = u;
            offset = o;
            get    = g;
            put    = p;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            out.writeByte(BYTE);
            try {
                out.writeByte((byte) get.invoke(unsafe, i, offset));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != BYTE)
                throw new IOException("Exp BYTE");
            try {
                put.invoke(unsafe, i, offset, in.readByte());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    static class UnsafeShort implements FieldAccessor {
        final Object       unsafe;
        final long         offset;
        final MethodHandle get;
        final MethodHandle put;

        UnsafeShort(Object u, long o, MethodHandle g, MethodHandle p) {
            unsafe = u;
            offset = o;
            get    = g;
            put    = p;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            out.writeByte(SHORT);
            try {
                out.writeShort((short) get.invoke(unsafe, i, offset));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != SHORT)
                throw new IOException("Exp SHORT");
            try {
                put.invoke(unsafe, i, offset, in.readShort());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    static class UnsafeChar implements FieldAccessor {
        final Object       unsafe;
        final long         offset;
        final MethodHandle get;
        final MethodHandle put;

        UnsafeChar(Object u, long o, MethodHandle g, MethodHandle p) {
            unsafe = u;
            offset = o;
            get    = g;
            put    = p;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            out.writeByte(CHAR);
            try {
                out.writeChar((char) get.invoke(unsafe, i, offset));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() == CHAR) {
                try {
                    put.invoke(unsafe, i, offset, in.readChar());
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            } else
                throw new IOException("Exp CHAR");
        }
    }

    static class UnsafeObject implements FieldAccessor {
        final Object       unsafe;
        final long         offset;
        final Type         type;
        final MethodHandle get;
        final MethodHandle put;

        UnsafeObject(Object u, long o, Type t, MethodHandle g, MethodHandle p) {
            unsafe = u;
            offset = o;
            type   = t;
            get    = g;
            put    = p;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            try {
                c.encode(get.invoke(unsafe, i, offset), out);
            } catch (Throwable t) {
                if (t instanceof Exception)
                    throw (Exception) t;
                throw new RuntimeException(t);
            }
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            try {
                put.invoke(unsafe, i, offset, c.decode(in, type));
            } catch (Throwable t) {
                if (t instanceof Exception)
                    throw (Exception) t;
                throw new RuntimeException(t);
            }
        }
    }
}