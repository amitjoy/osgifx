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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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
import java.util.function.Supplier;

/**
 * A High-Performance, Hybrid (Unsafe + MethodHandle) binary codec for OSGi DTOs.
 *
 * <p>
 * <b>Features:</b>
 * </p>
 * <ul>
 * <li><b>Hybrid Strategy:</b>
 * <ul>
 * <li><b>Primary (Unsafe):</b> Uses {@code LambdaMetafactory} wrapping {@code sun.misc.Unsafe} for maximum performance
 * and private field access.</li>
 * <li><b>Fallback (Standard):</b> Uses {@code MethodHandle.invokeExact} for fully portable, high-speed access to public
 * fields (when Unsafe is missing).</li>
 * </ul>
 * </li>
 * <li><b>Schema-less:</b> Removes field names from payload.</li>
 * <li><b>Zero-Boxing:</b> Primitives are handled efficiently.</li>
 * <li><b>Portable:</b> Works on all JVMs 1.8+.</li>
 * </ul>
 */
public class BinaryCodec {

    private static final Map<Class<?>, FieldAccessor[]> accessorCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Supplier<?>>     factoryCache  = new ConcurrentHashMap<>();
    private static final MethodHandles.Lookup           lookup        = MethodHandles.lookup();

    // -- Unsafe Capabilities --
    private static final Object unsafe;
    // Unsafe Accessors (Only initialized if unsafe != null)
    private static UnsafeIntReader      GET_INT;
    private static UnsafeIntWriter      PUT_INT;
    private static UnsafeLongReader     GET_LONG;
    private static UnsafeLongWriter     PUT_LONG;
    private static UnsafeBooleanReader  GET_BOOL;
    private static UnsafeBooleanWriter  PUT_BOOL;
    private static UnsafeDoubleReader   GET_DOUBLE;
    private static UnsafeDoubleWriter   PUT_DOUBLE;
    private static UnsafeFloatReader    GET_FLOAT;
    private static UnsafeFloatWriter    PUT_FLOAT;
    private static UnsafeByteReader     GET_BYTE;
    private static UnsafeByteWriter     PUT_BYTE;
    private static UnsafeShortReader    GET_SHORT;
    private static UnsafeShortWriter    PUT_SHORT;
    private static UnsafeCharReader     GET_CHAR;
    private static UnsafeCharWriter     PUT_CHAR;
    private static UnsafeObjectReader   GET_OBJECT;
    private static UnsafeObjectWriter   PUT_OBJECT;
    private static UnsafeOffsetResolver OFFSET_RESOLVER;
    private static UnsafeAllocator      ALLOCATOR;

    static {
        Object u = null;
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field    f           = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            u = f.get(null);

            // Initialize Unsafe Lambdas
            GET_INT         = createAccessor(lookup, unsafeClass, u, "getInt", UnsafeIntReader.class, int.class,
                    Object.class, long.class);
            PUT_INT         = createAccessor(lookup, unsafeClass, u, "putInt", UnsafeIntWriter.class, void.class,
                    Object.class, long.class, int.class);
            GET_LONG        = createAccessor(lookup, unsafeClass, u, "getLong", UnsafeLongReader.class, long.class,
                    Object.class, long.class);
            PUT_LONG        = createAccessor(lookup, unsafeClass, u, "putLong", UnsafeLongWriter.class, void.class,
                    Object.class, long.class, long.class);
            GET_BOOL        = createAccessor(lookup, unsafeClass, u, "getBoolean", UnsafeBooleanReader.class,
                    boolean.class, Object.class, long.class);
            PUT_BOOL        = createAccessor(lookup, unsafeClass, u, "putBoolean", UnsafeBooleanWriter.class,
                    void.class, Object.class, long.class, boolean.class);
            GET_DOUBLE      = createAccessor(lookup, unsafeClass, u, "getDouble", UnsafeDoubleReader.class,
                    double.class, Object.class, long.class);
            PUT_DOUBLE      = createAccessor(lookup, unsafeClass, u, "putDouble", UnsafeDoubleWriter.class, void.class,
                    Object.class, long.class, double.class);
            GET_FLOAT       = createAccessor(lookup, unsafeClass, u, "getFloat", UnsafeFloatReader.class, float.class,
                    Object.class, long.class);
            PUT_FLOAT       = createAccessor(lookup, unsafeClass, u, "putFloat", UnsafeFloatWriter.class, void.class,
                    Object.class, long.class, float.class);
            GET_BYTE        = createAccessor(lookup, unsafeClass, u, "getByte", UnsafeByteReader.class, byte.class,
                    Object.class, long.class);
            PUT_BYTE        = createAccessor(lookup, unsafeClass, u, "putByte", UnsafeByteWriter.class, void.class,
                    Object.class, long.class, byte.class);
            GET_SHORT       = createAccessor(lookup, unsafeClass, u, "getShort", UnsafeShortReader.class, short.class,
                    Object.class, long.class);
            PUT_SHORT       = createAccessor(lookup, unsafeClass, u, "putShort", UnsafeShortWriter.class, void.class,
                    Object.class, long.class, short.class);
            GET_CHAR        = createAccessor(lookup, unsafeClass, u, "getChar", UnsafeCharReader.class, char.class,
                    Object.class, long.class);
            PUT_CHAR        = createAccessor(lookup, unsafeClass, u, "putChar", UnsafeCharWriter.class, void.class,
                    Object.class, long.class, char.class);
            GET_OBJECT      = createAccessor(lookup, unsafeClass, u, "getObject", UnsafeObjectReader.class,
                    Object.class, Object.class, long.class);
            PUT_OBJECT      = createAccessor(lookup, unsafeClass, u, "putObject", UnsafeObjectWriter.class, void.class,
                    Object.class, long.class, Object.class);
            OFFSET_RESOLVER = createAccessor(lookup, unsafeClass, u, "objectFieldOffset", UnsafeOffsetResolver.class,
                    long.class, Field.class);
            ALLOCATOR       = createAccessor(lookup, unsafeClass, u, "allocateInstance", UnsafeAllocator.class,
                    Object.class, Class.class);

        } catch (Throwable t) {
            // Unsafe not available - Fallback to Standard Strategy
            u = null;
        }
        unsafe = u;
    }

    private static <T> T createAccessor(MethodHandles.Lookup lookup,
                                        Class<?> unsafeClass,
                                        Object unsafeInstance,
                                        String methodName,
                                        Class<T> interfaceType,
                                        Class<?> returnType,
                                        Class<?>... parameterTypes) throws Throwable {
        MethodHandle handle              = lookup.findVirtual(unsafeClass, methodName,
                MethodType.methodType(returnType, parameterTypes));
        String       interfaceMethodName = "get";
        if (methodName.startsWith("put"))
            interfaceMethodName = "put";
        if (methodName.equals("allocateInstance"))
            interfaceMethodName = "allocate";
        if (methodName.equals("objectFieldOffset"))
            interfaceMethodName = "offset";

        final String finalInterfaceMethodName = interfaceMethodName;

        MethodType interfaceTypeMethod = Arrays.stream(interfaceType.getMethods())
                .filter(m -> m.getName().equals(finalInterfaceMethodName)).findFirst()
                .map(m -> MethodType.methodType(m.getReturnType(), m.getParameterTypes()))
                .orElseThrow(() -> new IllegalArgumentException("Method " + finalInterfaceMethodName + " not found"));

        CallSite site = LambdaMetafactory.metafactory(lookup, interfaceMethodName,
                MethodType.methodType(interfaceType, unsafeClass), interfaceTypeMethod, handle, interfaceTypeMethod);
        return (T) site.getTarget().invoke(unsafeInstance);
    }

    // Type Tags
    private static final byte NULL   = 0;
    private static final byte BOOL   = 1;
    private static final byte BYTE   = 2;
    private static final byte SHORT  = 3;
    private static final byte INT    = 4;
    private static final byte LONG   = 5;
    private static final byte FLOAT  = 6;
    private static final byte DOUBLE = 7;
    private static final byte STRING = 8;
    private static final byte LIST   = 9;
    private static final byte DTO    = 10;
    private static final byte ARRAY  = 11;
    private static final byte ENUM   = 12;
    private static final byte MAP    = 13;
    private static final byte CHAR   = 14;

    // --- ENCODER ---

    public void encode(Object obj, DataOutputStream out) throws Exception {
        if (obj == null) {
            out.writeByte(NULL);
            return;
        }
        Class<?> clz = obj.getClass();
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
        } else if (clz.isEnum()) {
            out.writeByte(ENUM);
            out.writeUTF(((Enum<?>) obj).name());
        } else if (Collection.class.isAssignableFrom(clz)) {
            out.writeByte(LIST);
            Collection<?> col = (Collection<?>) obj;
            out.writeInt(col.size());
            for (Object item : col)
                encode(item, out);
        } else if (Map.class.isAssignableFrom(clz)) {
            out.writeByte(MAP);
            Map<?, ?> map = (Map<?, ?>) obj;
            out.writeInt(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                encode(entry.getKey(), out);
                encode(entry.getValue(), out);
            }
        } else if (clz.isArray()) {
            if (clz.getComponentType() == byte.class) {
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
        } else {
            // DTO or POJO
            out.writeByte(DTO);
            FieldAccessor[] accessors = getAccessors(clz);
            for (FieldAccessor acc : accessors)
                acc.encode(obj, out, this);
        }
    }

    public <T> T decode(final byte[] data, final Class<T> type) {
        if (data == null || data.length == 0)
            return null;
        // Optimization: Use FastByteArrayInputStream to avoid synchronized overhead of ByteArrayInputStream
        // and reduce strict object creation if we expand this to a pool later.
        // For now, it simply replaces ByteArrayInputStream with a lighter version.
        try (DataInputStream in = new DataInputStream(new FastByteArrayInputStream(data))) {
            return decode(in, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A lightweight, non-synchronized ByteArrayInputStream alternative.
     */
    public static class FastByteArrayInputStream extends InputStream {
        protected byte[] buf;
        protected int    pos;
        protected int    count;

        public FastByteArrayInputStream(byte[] buf) {
            this.buf   = buf;
            this.pos   = 0;
            this.count = buf.length;
        }

        @Override
        public int read() {
            return (pos < count) ? (buf[pos++] & 0xff) : -1;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (pos >= count) {
                return -1;
            }
            int avail = count - pos;
            if (len > avail) {
                len = avail;
            }
            if (len <= 0) {
                return 0;
            }
            System.arraycopy(buf, pos, b, off, len);
            pos += len;
            return len;
        }

        @Override
        public int available() {
            return count - pos;
        }
    }

    @SuppressWarnings({ "unchecked" })
    public <T> T decode(final DataInputStream in, final Type type) throws Exception {
        return (T) decodeObject(in, type);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object decodeObject(DataInputStream in, Type type) throws Exception {
        byte tag = in.readByte();
        if (tag == NULL)
            return null;

        Class<?> rawClass = (type instanceof Class) ? (Class<?>) type
                : (Class<?>) ((ParameterizedType) type).getRawType();

        switch (tag) {
            case LIST: {
                int          size     = in.readInt();
                List<Object> list     = new ArrayList<>(size);
                Type         compType = Object.class;
                if (type instanceof ParameterizedType)
                    compType = ((ParameterizedType) type).getActualTypeArguments()[0];
                else if (rawClass.isArray())
                    compType = rawClass.getComponentType();

                for (int i = 0; i < size; i++)
                    list.add(decode(in, compType));

                if (rawClass.isArray()) {
                    Object arr = Array.newInstance((Class<?>) compType, size);
                    for (int i = 0; i < size; i++)
                        Array.set(arr, i, list.get(i));
                    return arr;
                }
                if (Set.class.isAssignableFrom(rawClass))
                    return new LinkedHashSet<>(list);
                return list;
            }
            case MAP: {
                int                 size  = in.readInt();
                Map<Object, Object> map   = new LinkedHashMap<>(size);
                Type                kType = Object.class;
                Type                vType = Object.class;
                if (type instanceof ParameterizedType) {
                    Type[] args = ((ParameterizedType) type).getActualTypeArguments();
                    kType = args[0];
                    vType = args[1];
                }
                for (int i = 0; i < size; i++)
                    map.put(decode(in, kType), decode(in, vType));
                return map;
            }
            case ARRAY: {
                in.readUTF(); // Skip type signature
                int    len  = in.readInt();
                byte[] data = new byte[len];
                in.readFully(data);
                return data;
            }
            case DTO: {
                Object          instance  = getFactory(rawClass).get();
                FieldAccessor[] accessors = getAccessors(rawClass);
                for (FieldAccessor acc : accessors)
                    acc.decode(instance, in, this);
                return instance;
            }
            // Add other primitive cases handled by decodeObject if invoked with Object.class
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
            case ENUM: {
                final String name = in.readUTF();
                if (rawClass.isEnum()) {
                    return Enum.valueOf((Class<Enum>) rawClass, name);
                }
                return name;
            }
            default:
                throw new IOException("Unknown tag: " + tag);
        }
    }

    private FieldAccessor[] getAccessors(Class<?> clz) {
        return accessorCache.computeIfAbsent(clz, c -> {
            return Arrays.stream(c.getFields()) // Public fields only implies DTO contract is cleaner, but getFields()
                                                // returns public.
                    // Wait! Unsafe could read private fields.
                    // If we use 'unsafe', we should prefer 'getDeclaredFields()', but standard 'getFields()' is
                    // consistent for DTOs.
                    // Let's stick to getFields() for now as user DTOs are likely public.
                    // UPDATE: To mimic Unsafe strategy correctly, we should use getDeclaredFields() if unsafe is
                    // present?
                    // No, existing code used getFields() + accessible check logic implicitly or getFields().
                    // Standard OSGi DTOs use public fields. Let's use getFields() to be safe and spec compliant.
                    .filter(f -> !Modifier.isStatic(f.getModifiers())).sorted(Comparator.comparing(Field::getName))
                    .map(f -> unsafe != null ? createUnsafeAccessor(f) : createStandardAccessor(f))
                    .toArray(FieldAccessor[]::new);
        });
    }

    private Supplier<?> getFactory(Class<?> clz) {
        return factoryCache.computeIfAbsent(clz, c -> {
            try {
                if (unsafe != null) {
                    return () -> ALLOCATOR.allocate(c);
                } else {
                    MethodHandle ctor = lookup.findConstructor(c, MethodType.methodType(void.class));
                    CallSite     site = LambdaMetafactory.metafactory(lookup, "get",
                            MethodType.methodType(Supplier.class), MethodType.methodType(Object.class), ctor,
                            MethodType.methodType(Object.class));
                    return (Supplier<?>) site.getTarget().invokeExact();
                }
            } catch (Throwable e) {
                // If standard constructor missing, we can't do much in Standard mode.
                throw new RuntimeException("Cannot create factory for " + c, e);
            }
        });
    }

    // ===================================
    // FACTORY METHODS
    // ===================================

    private FieldAccessor createUnsafeAccessor(Field f) {
        long     offset = OFFSET_RESOLVER.offset(f);
        Class<?> type   = f.getType();
        if (type == int.class)
            return new LambdaInt(offset);
        if (type == boolean.class)
            return new LambdaBoolean(offset);
        if (type == long.class)
            return new LambdaLong(offset);
        if (type == double.class)
            return new LambdaDouble(offset);
        if (type == float.class)
            return new LambdaFloat(offset);
        if (type == byte.class)
            return new LambdaByte(offset);
        if (type == short.class)
            return new LambdaShort(offset);
        if (type == char.class)
            return new LambdaChar(offset);
        return new LambdaObject(offset, f.getGenericType());
    }

    private FieldAccessor createStandardAccessor(Field f) {
        try {
            MethodHandle get  = lookup.unreflectGetter(f);
            MethodHandle set  = lookup.unreflectSetter(f);
            Class<?>     type = f.getType();

            if (type == int.class)
                return new StandardInt(get, set);
            if (type == boolean.class)
                return new StandardBoolean(get, set);
            if (type == long.class)
                return new StandardLong(get, set);
            if (type == double.class)
                return new StandardDouble(get, set);
            if (type == float.class)
                return new StandardFloat(get, set);
            if (type == byte.class)
                return new StandardByte(get, set);
            if (type == short.class)
                return new StandardShort(get, set);
            if (type == char.class)
                return new StandardChar(get, set);
            return new StandardObject(get, set, f.getGenericType());
        } catch (Exception e) {
            throw new RuntimeException("Failed to access field " + f.getName(), e);
        }
    }

    // ===================================
    // INTERFACES & CLASSES
    // ===================================

    interface FieldAccessor {
        void encode(Object instance, DataOutputStream out, BinaryCodec codec) throws Exception;

        void decode(Object instance, DataInputStream in, BinaryCodec codec) throws Exception;
    }

    // --- UNSAFE IMPLEMENTATIONS (Lambda Wrappers) ---

    @FunctionalInterface
    interface UnsafeIntReader {
        int get(Object target, long offset);
    }

    @FunctionalInterface
    interface UnsafeIntWriter {
        void put(Object target, long offset, int value);
    }

    @FunctionalInterface
    interface UnsafeLongReader {
        long get(Object target, long offset);
    }

    @FunctionalInterface
    interface UnsafeLongWriter {
        void put(Object target, long offset, long value);
    }

    @FunctionalInterface
    interface UnsafeBooleanReader {
        boolean get(Object target, long offset);
    }

    @FunctionalInterface
    interface UnsafeBooleanWriter {
        void put(Object target, long offset, boolean value);
    }

    @FunctionalInterface
    interface UnsafeDoubleReader {
        double get(Object target, long offset);
    }

    @FunctionalInterface
    interface UnsafeDoubleWriter {
        void put(Object target, long offset, double value);
    }

    @FunctionalInterface
    interface UnsafeFloatReader {
        float get(Object target, long offset);
    }

    @FunctionalInterface
    interface UnsafeFloatWriter {
        void put(Object target, long offset, float value);
    }

    @FunctionalInterface
    interface UnsafeByteReader {
        byte get(Object target, long offset);
    }

    @FunctionalInterface
    interface UnsafeByteWriter {
        void put(Object target, long offset, byte value);
    }

    @FunctionalInterface
    interface UnsafeShortReader {
        short get(Object target, long offset);
    }

    @FunctionalInterface
    interface UnsafeShortWriter {
        void put(Object target, long offset, short value);
    }

    @FunctionalInterface
    interface UnsafeCharReader {
        char get(Object target, long offset);
    }

    @FunctionalInterface
    interface UnsafeCharWriter {
        void put(Object target, long offset, char value);
    }

    @FunctionalInterface
    interface UnsafeObjectReader {
        Object get(Object target, long offset);
    }

    @FunctionalInterface
    interface UnsafeObjectWriter {
        void put(Object target, long offset, Object value);
    }

    @FunctionalInterface
    interface UnsafeOffsetResolver {
        long offset(Field f);
    }

    @FunctionalInterface
    interface UnsafeAllocator {
        Object allocate(Class<?> clz);
    }

    static class LambdaInt implements FieldAccessor {
        final long offset;

        LambdaInt(long o) {
            offset = o;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            out.writeByte(INT);
            out.writeInt(GET_INT.get(i, offset));
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != INT)
                throw new IOException();
            PUT_INT.put(i, offset, in.readInt());
        }
    }

    static class LambdaBoolean implements FieldAccessor {
        final long offset;

        LambdaBoolean(long o) {
            offset = o;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            out.writeByte(BOOL);
            out.writeBoolean(GET_BOOL.get(i, offset));
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != BOOL)
                throw new IOException();
            PUT_BOOL.put(i, offset, in.readBoolean());
        }
    }

    static class LambdaLong implements FieldAccessor {
        final long offset;

        LambdaLong(long o) {
            offset = o;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            out.writeByte(LONG);
            out.writeLong(GET_LONG.get(i, offset));
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != LONG)
                throw new IOException();
            PUT_LONG.put(i, offset, in.readLong());
        }
    }

    static class LambdaDouble implements FieldAccessor {
        final long offset;

        LambdaDouble(long o) {
            offset = o;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            out.writeByte(DOUBLE);
            out.writeDouble(GET_DOUBLE.get(i, offset));
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != DOUBLE)
                throw new IOException();
            PUT_DOUBLE.put(i, offset, in.readDouble());
        }
    }

    static class LambdaFloat implements FieldAccessor {
        final long offset;

        LambdaFloat(long o) {
            offset = o;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            out.writeByte(FLOAT);
            out.writeFloat(GET_FLOAT.get(i, offset));
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != FLOAT)
                throw new IOException();
            PUT_FLOAT.put(i, offset, in.readFloat());
        }
    }

    static class LambdaByte implements FieldAccessor {
        final long offset;

        LambdaByte(long o) {
            offset = o;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            out.writeByte(BYTE);
            out.writeByte(GET_BYTE.get(i, offset));
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != BYTE)
                throw new IOException();
            PUT_BYTE.put(i, offset, in.readByte());
        }
    }

    static class LambdaShort implements FieldAccessor {
        final long offset;

        LambdaShort(long o) {
            offset = o;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            out.writeByte(SHORT);
            out.writeShort(GET_SHORT.get(i, offset));
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != SHORT)
                throw new IOException();
            PUT_SHORT.put(i, offset, in.readShort());
        }
    }

    static class LambdaChar implements FieldAccessor {
        final long offset;

        LambdaChar(long o) {
            offset = o;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            out.writeByte(CHAR);
            out.writeChar(GET_CHAR.get(i, offset));
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            if (in.readByte() != CHAR)
                throw new IOException();
            PUT_CHAR.put(i, offset, in.readChar());
        }
    }

    static class LambdaObject implements FieldAccessor {
        final long offset;
        final Type type;

        LambdaObject(long o, Type t) {
            offset = o;
            type   = t;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            c.encode(GET_OBJECT.get(i, offset), out);
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            PUT_OBJECT.put(i, offset, c.decode(in, type));
        }
    }

    static class StandardInt implements FieldAccessor {
        final MethodHandle get;
        final MethodHandle set;

        StandardInt(MethodHandle g, MethodHandle s) {
            get = g;
            set = s;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            try {
                out.writeByte(INT);
                out.writeInt((int) get.invokeExact(i));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            try {
                if (in.readByte() != INT)
                    throw new IOException();
                set.invokeExact(i, in.readInt());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    static class StandardBoolean implements FieldAccessor {
        final MethodHandle get;
        final MethodHandle set;

        StandardBoolean(MethodHandle g, MethodHandle s) {
            get = g;
            set = s;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            try {
                out.writeByte(BOOL);
                out.writeBoolean((boolean) get.invokeExact(i));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            try {
                if (in.readByte() != BOOL)
                    throw new IOException();
                set.invokeExact(i, in.readBoolean());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    static class StandardLong implements FieldAccessor {
        final MethodHandle get;
        final MethodHandle set;

        StandardLong(MethodHandle g, MethodHandle s) {
            get = g;
            set = s;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            try {
                out.writeByte(LONG);
                out.writeLong((long) get.invokeExact(i));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            try {
                if (in.readByte() != LONG)
                    throw new IOException();
                set.invokeExact(i, in.readLong());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    static class StandardDouble implements FieldAccessor {
        final MethodHandle get;
        final MethodHandle set;

        StandardDouble(MethodHandle g, MethodHandle s) {
            get = g;
            set = s;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            try {
                out.writeByte(DOUBLE);
                out.writeDouble((double) get.invokeExact(i));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            try {
                if (in.readByte() != DOUBLE)
                    throw new IOException();
                set.invokeExact(i, in.readDouble());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    static class StandardFloat implements FieldAccessor {
        final MethodHandle get;
        final MethodHandle set;

        StandardFloat(MethodHandle g, MethodHandle s) {
            get = g;
            set = s;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            try {
                out.writeByte(FLOAT);
                out.writeFloat((float) get.invokeExact(i));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            try {
                if (in.readByte() != FLOAT)
                    throw new IOException();
                set.invokeExact(i, in.readFloat());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    static class StandardByte implements FieldAccessor {
        final MethodHandle get;
        final MethodHandle set;

        StandardByte(MethodHandle g, MethodHandle s) {
            get = g;
            set = s;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            try {
                out.writeByte(BYTE);
                out.writeByte((byte) get.invokeExact(i));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            try {
                if (in.readByte() != BYTE)
                    throw new IOException();
                set.invokeExact(i, in.readByte());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    static class StandardShort implements FieldAccessor {
        final MethodHandle get;
        final MethodHandle set;

        StandardShort(MethodHandle g, MethodHandle s) {
            get = g;
            set = s;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            try {
                out.writeByte(SHORT);
                out.writeShort((short) get.invokeExact(i));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            try {
                if (in.readByte() != SHORT)
                    throw new IOException();
                set.invokeExact(i, in.readShort());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    static class StandardChar implements FieldAccessor {
        final MethodHandle get;
        final MethodHandle set;

        StandardChar(MethodHandle g, MethodHandle s) {
            get = g;
            set = s;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            try {
                out.writeByte(CHAR);
                out.writeChar((char) get.invokeExact(i));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            try {
                if (in.readByte() != CHAR)
                    throw new IOException();
                set.invokeExact(i, in.readChar());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    static class StandardObject implements FieldAccessor {
        final MethodHandle get;
        final MethodHandle set;
        final Type         type;

        StandardObject(MethodHandle g, MethodHandle s, Type t) {
            get  = g;
            set  = s;
            type = t;
        }

        public void encode(Object i, DataOutputStream out, BinaryCodec c) throws Exception {
            try {
                c.encode(get.invoke(i), out);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public void decode(Object i, DataInputStream in, BinaryCodec c) throws Exception {
            try {
                set.invoke(i, c.decode(in, type));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }
}