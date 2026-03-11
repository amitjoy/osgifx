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

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

public final class UnsafeMemory {

    @FunctionalInterface
    public interface GetByte {
        byte get(Object o, long offset);
    }

    @FunctionalInterface
    public interface PutByte {
        void put(Object o, long offset, byte x);
    }

    @FunctionalInterface
    public interface GetShort {
        short get(Object o, long offset);
    }

    @FunctionalInterface
    public interface PutShort {
        void put(Object o, long offset, short x);
    }

    @FunctionalInterface
    public interface GetInt {
        int get(Object o, long offset);
    }

    @FunctionalInterface
    public interface PutInt {
        void put(Object o, long offset, int x);
    }

    @FunctionalInterface
    public interface GetLong {
        long get(Object o, long offset);
    }

    @FunctionalInterface
    public interface PutLong {
        void put(Object o, long offset, long x);
    }

    @FunctionalInterface
    public interface CopyMemory {
        void copy(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes);
    }

    @FunctionalInterface
    public interface ArrayBaseOffset {
        int offset(Class<?> c);
    }

    @FunctionalInterface
    public interface ObjectFieldOffset {
        long offset(Field f);
    }

    private static final GetByte           GET_BYTE;
    private static final PutByte           PUT_BYTE;
    private static final GetShort          GET_SHORT;
    private static final PutShort          PUT_SHORT;
    private static final GetInt            GET_INT;
    private static final PutInt            PUT_INT;
    private static final GetLong           GET_LONG;
    private static final PutLong           PUT_LONG;
    private static final CopyMemory        COPY_MEMORY;
    private static final ArrayBaseOffset   ARRAY_BASE_OFFSET;
    private static final ObjectFieldOffset OBJECT_FIELD_OFFSET;

    public static final int ARRAY_BYTE_BASE_OFFSET;

    static {
        try {
            Class<?> unsafeClass = null;
            try {
                unsafeClass = Class.forName("sun.misc.Unsafe");
            } catch (ClassNotFoundException e) {
                unsafeClass = Class.forName("sun.misc.Unsafe", true, null);
            }
            Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object unsafe = f.get(null);

            MethodHandles.Lookup lookup = MethodHandles.lookup();

            GET_BYTE            = create(lookup, unsafeClass, unsafe, "getByte", GetByte.class, byte.class,
                    Object.class, long.class);
            PUT_BYTE            = create(lookup, unsafeClass, unsafe, "putByte", PutByte.class, void.class,
                    Object.class, long.class, byte.class);
            GET_SHORT           = create(lookup, unsafeClass, unsafe, "getShort", GetShort.class, short.class,
                    Object.class, long.class);
            PUT_SHORT           = create(lookup, unsafeClass, unsafe, "putShort", PutShort.class, void.class,
                    Object.class, long.class, short.class);
            GET_INT             = create(lookup, unsafeClass, unsafe, "getInt", GetInt.class, int.class, Object.class,
                    long.class);
            PUT_INT             = create(lookup, unsafeClass, unsafe, "putInt", PutInt.class, void.class, Object.class,
                    long.class, int.class);
            GET_LONG            = create(lookup, unsafeClass, unsafe, "getLong", GetLong.class, long.class,
                    Object.class, long.class);
            PUT_LONG            = create(lookup, unsafeClass, unsafe, "putLong", PutLong.class, void.class,
                    Object.class, long.class, long.class);
            COPY_MEMORY         = create(lookup, unsafeClass, unsafe, "copyMemory", CopyMemory.class, void.class,
                    Object.class, long.class, Object.class, long.class, long.class);
            ARRAY_BASE_OFFSET   = create(lookup, unsafeClass, unsafe, "arrayBaseOffset", ArrayBaseOffset.class,
                    int.class, Class.class);
            OBJECT_FIELD_OFFSET = create(lookup, unsafeClass, unsafe, "objectFieldOffset", ObjectFieldOffset.class,
                    long.class, Field.class);

            ARRAY_BYTE_BASE_OFFSET = ARRAY_BASE_OFFSET.offset(byte[].class);
        } catch (Throwable t) {
            throw new RuntimeException("Unsafe not available", t);
        }
    }

    private static <T> T create(MethodHandles.Lookup lookup,
                                Class<?> unsafeClass,
                                Object unsafeInstance,
                                String methodName,
                                Class<T> interfaceClass,
                                Class<?> retType,
                                Class<?>... pTypes) throws Throwable {
        MethodType   mt          = MethodType.methodType(retType, pTypes);
        MethodHandle mh          = lookup.findVirtual(unsafeClass, methodName, mt);
        MethodType   factoryType = MethodType.methodType(interfaceClass, unsafeClass);

        CallSite site = LambdaMetafactory.metafactory(lookup, interfaceClass.getMethods()[0].getName(), factoryType, mt,
                mh, mt);
        return (T) site.getTarget().invoke(unsafeInstance);
    }

    public static byte getByte(Object o, long offset) {
        return GET_BYTE.get(o, offset);
    }

    public static void putByte(Object o, long offset, byte x) {
        PUT_BYTE.put(o, offset, x);
    }

    public static short getShort(Object o, long offset) {
        return GET_SHORT.get(o, offset);
    }

    public static void putShort(Object o, long offset, short x) {
        PUT_SHORT.put(o, offset, x);
    }

    public static int getInt(Object o, long offset) {
        return GET_INT.get(o, offset);
    }

    public static void putInt(Object o, long offset, int x) {
        PUT_INT.put(o, offset, x);
    }

    public static long getLong(Object o, long offset) {
        return GET_LONG.get(o, offset);
    }

    public static void putLong(Object o, long offset, long x) {
        PUT_LONG.put(o, offset, x);
    }

    public static void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {
        COPY_MEMORY.copy(srcBase, srcOffset, destBase, destOffset, bytes);
    }

    public static int arrayBaseOffset(Class<?> c) {
        return ARRAY_BASE_OFFSET.offset(c);
    }

    public static long objectFieldOffset(Field f) {
        return OBJECT_FIELD_OFFSET.offset(f);
    }
}
