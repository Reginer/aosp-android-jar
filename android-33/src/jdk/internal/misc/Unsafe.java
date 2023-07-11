/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.internal.misc;

import dalvik.annotation.optimization.FastNative;
import jdk.internal.HotSpotIntrinsicCandidate;
import sun.reflect.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * A collection of methods for performing low-level, unsafe operations.
 * Although the class and all methods are public, use of this class is
 * limited because only trusted code can obtain instances of it.
 *
 * @author John R. Rose
 * @see #getUnsafe
 */
public final class Unsafe {
    /** Traditional dalvik name. */
    private static final Unsafe THE_ONE = new Unsafe();

    private static final Unsafe theUnsafe = THE_ONE;
    public static final int INVALID_FIELD_OFFSET   = -1;

    /**
     * This class is only privately instantiable.
     */
    private Unsafe() {}

    /**
     * Gets the unique instance of this class. This is only allowed in
     * very limited situations.
     */
    public static Unsafe getUnsafe() {
        Class<?> caller = Reflection.getCallerClass();
        /*
         * Only code on the bootclasspath is allowed to get at the
         * Unsafe instance.
         */
        ClassLoader calling = (caller == null) ? null : caller.getClassLoader();
        if ((calling != null) && (calling != Unsafe.class.getClassLoader())) {
            throw new SecurityException("Unsafe access denied");
        }

        return THE_ONE;
    }

    /**
     * Gets the raw byte offset from the start of an object's memory to
     * the memory used to store the indicated instance field.
     *
     * @param field non-{@code null}; the field in question, which must be an
     * instance field
     * @return the offset to the field
     */
    public long objectFieldOffset(Field field) {
        if (Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException("valid for instance fields only");
        }
        return field.getOffset();
    }

    /**
     * Reports the location of the field with a given name in the storage
     * allocation of its class.
     *
     * @throws NullPointerException if any parameter is {@code null}.
     * @throws InternalError if there is no field named {@code name} declared
     *         in class {@code c}, i.e., if {@code c.getDeclaredField(name)}
     *         would throw {@code java.lang.NoSuchFieldException}.
     *
     * @see #objectFieldOffset(Field)
     */
    public long objectFieldOffset(Class<?> c, String name) {
        if (c == null || name == null) {
            throw new NullPointerException();
        }

        Field field = null;
        Field[] fields = c.getDeclaredFields();
        for (Field f : fields) {
            if (f.getName().equals(name)) {
                field = f;
                break;
            }
        }
        if (field == null) {
            throw new InternalError();
        }
        return objectFieldOffset(field);
    }

    /**
     * Gets the offset from the start of an array object's memory to
     * the memory used to store its initial (zeroeth) element.
     *
     * @param clazz non-{@code null}; class in question; must be an array class
     * @return the offset to the initial element
     */
    public int arrayBaseOffset(Class clazz) {
        Class<?> component = clazz.getComponentType();
        if (component == null) {
            throw new IllegalArgumentException("Valid for array classes only: " + clazz);
        }
        return getArrayBaseOffsetForComponentType(component);
    }

    /** The value of {@code arrayBaseOffset(boolean[].class)} */
    public static final int ARRAY_BOOLEAN_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(boolean[].class);

    /** The value of {@code arrayBaseOffset(byte[].class)} */
    public static final int ARRAY_BYTE_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(byte[].class);

    /** The value of {@code arrayBaseOffset(short[].class)} */
    public static final int ARRAY_SHORT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(short[].class);

    /** The value of {@code arrayBaseOffset(char[].class)} */
    public static final int ARRAY_CHAR_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(char[].class);

    /** The value of {@code arrayBaseOffset(int[].class)} */
    public static final int ARRAY_INT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(int[].class);

    /** The value of {@code arrayBaseOffset(long[].class)} */
    public static final int ARRAY_LONG_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(long[].class);

    /** The value of {@code arrayBaseOffset(float[].class)} */
    public static final int ARRAY_FLOAT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(float[].class);

    /** The value of {@code arrayBaseOffset(double[].class)} */
    public static final int ARRAY_DOUBLE_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(double[].class);

    /** The value of {@code arrayBaseOffset(Object[].class)} */
    public static final int ARRAY_OBJECT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(Object[].class);

    /**
     * Gets the size of each element of the given array class.
     *
     * @param clazz non-{@code null}; class in question; must be an array class
     * @return &gt; 0; the size of each element of the array
     */
    public int arrayIndexScale(Class clazz) {
      Class<?> component = clazz.getComponentType();
      if (component == null) {
          throw new IllegalArgumentException("Valid for array classes only: " + clazz);
      }
      return getArrayIndexScaleForComponentType(component);
    }

    /** The value of {@code arrayIndexScale(boolean[].class)} */
    public static final int ARRAY_BOOLEAN_INDEX_SCALE
            = theUnsafe.arrayIndexScale(boolean[].class);

    /** The value of {@code arrayIndexScale(byte[].class)} */
    public static final int ARRAY_BYTE_INDEX_SCALE
            = theUnsafe.arrayIndexScale(byte[].class);

    /** The value of {@code arrayIndexScale(short[].class)} */
    public static final int ARRAY_SHORT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(short[].class);

    /** The value of {@code arrayIndexScale(char[].class)} */
    public static final int ARRAY_CHAR_INDEX_SCALE
            = theUnsafe.arrayIndexScale(char[].class);

    /** The value of {@code arrayIndexScale(int[].class)} */
    public static final int ARRAY_INT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(int[].class);

    /** The value of {@code arrayIndexScale(long[].class)} */
    public static final int ARRAY_LONG_INDEX_SCALE
            = theUnsafe.arrayIndexScale(long[].class);

    /** The value of {@code arrayIndexScale(float[].class)} */
    public static final int ARRAY_FLOAT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(float[].class);

    /** The value of {@code arrayIndexScale(double[].class)} */
    public static final int ARRAY_DOUBLE_INDEX_SCALE
            = theUnsafe.arrayIndexScale(double[].class);

    /** The value of {@code arrayIndexScale(Object[].class)} */
    public static final int ARRAY_OBJECT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(Object[].class);

    /** The value of {@code addressSize()} */
    public static final int ADDRESS_SIZE = theUnsafe.addressSize();

    @FastNative
    private static native int getArrayBaseOffsetForComponentType(Class component_class);
    @FastNative
    private static native int getArrayIndexScaleForComponentType(Class component_class);

    /**
     * Fetches a value at some byte offset into a given Java object.
     * More specifically, fetches a value within the given object
     * <code>o</code> at the given offset, or (if <code>o</code> is
     * null) from the memory address whose numerical value is the
     * given offset.  <p>
     *
     * The specification of this method is the same as {@link
     * #getLong(Object, long)} except that the offset does not need to
     * have been obtained from {@link #objectFieldOffset} on the
     * {@link java.lang.reflect.Field} of some Java field.  The value
     * in memory is raw data, and need not correspond to any Java
     * variable.  Unless <code>o</code> is null, the value accessed
     * must be entirely within the allocated object.  The endianness
     * of the value in memory is the endianness of the native platform.
     *
     * <p> The read will be atomic with respect to the largest power
     * of two that divides the GCD of the offset and the storage size.
     * For example, getLongUnaligned will make atomic reads of 2-, 4-,
     * or 8-byte storage units if the offset is zero mod 2, 4, or 8,
     * respectively.  There are no other guarantees of atomicity.
     * <p>
     * 8-byte atomicity is only guaranteed on platforms on which
     * support atomic accesses to longs.
     *
     * @param o Java heap object in which the value resides, if any, else
     *        null
     * @param offset The offset in bytes from the start of the object
     * @return the value fetched from the indicated object
     * @throws RuntimeException No defined exceptions are thrown, not even
     *         {@link NullPointerException}
     * @since 9
     */
    public final long getLongUnaligned(Object o, long offset) {
        if ((offset & 7) == 0) {
            return getLong(o, offset);
        } else if ((offset & 3) == 0) {
            return makeLong(getInt(o, offset),
                    getInt(o, offset + 4));
        } else if ((offset & 1) == 0) {
            return makeLong(getShort(o, offset),
                    getShort(o, offset + 2),
                    getShort(o, offset + 4),
                    getShort(o, offset + 6));
        } else {
            return makeLong(getByte(o, offset),
                    getByte(o, offset + 1),
                    getByte(o, offset + 2),
                    getByte(o, offset + 3),
                    getByte(o, offset + 4),
                    getByte(o, offset + 5),
                    getByte(o, offset + 6),
                    getByte(o, offset + 7));
        }
    }

    /** @see #getLongUnaligned(Object, long) */
    public final int getIntUnaligned(Object o, long offset) {
        if ((offset & 3) == 0) {
            return getInt(o, offset);
        } else if ((offset & 1) == 0) {
            return makeInt(getShort(o, offset),
                    getShort(o, offset + 2));
        } else {
            return makeInt(getByte(o, offset),
                    getByte(o, offset + 1),
                    getByte(o, offset + 2),
                    getByte(o, offset + 3));
        }
    }

    // These methods construct integers from bytes.  The byte ordering
    // is the native endianness of this platform.
    private static long makeLong(byte i0, byte i1, byte i2, byte i3, byte i4, byte i5, byte i6, byte i7) {
        return ((toUnsignedLong(i0))
                | (toUnsignedLong(i1) << 8)
                | (toUnsignedLong(i2) << 16)
                | (toUnsignedLong(i3) << 24)
                | (toUnsignedLong(i4) << 32)
                | (toUnsignedLong(i5) << 40)
                | (toUnsignedLong(i6) << 48)
                | (toUnsignedLong(i7) << 56));
    }
    private static long makeLong(short i0, short i1, short i2, short i3) {
        return ((toUnsignedLong(i0))
                | (toUnsignedLong(i1) << 16)
                | (toUnsignedLong(i2) << 32)
                | (toUnsignedLong(i3) << 48));
    }
    private static long makeLong(int i0, int i1) {
        return (toUnsignedLong(i0))
                | (toUnsignedLong(i1) << 32);
    }
    private static int makeInt(short i0, short i1) {
        return (toUnsignedInt(i0))
                | (toUnsignedInt(i1) << 16);
    }
    private static int makeInt(byte i0, byte i1, byte i2, byte i3) {
        return ((toUnsignedInt(i0))
                | (toUnsignedInt(i1) << 8)
                | (toUnsignedInt(i2) << 16)
                | (toUnsignedInt(i3) << 24));
    }
    private static short makeShort(byte i0, byte i1) {
        return (short)((toUnsignedInt(i0))
                | (toUnsignedInt(i1) << 8));
    }

    // Zero-extend an integer
    private static int toUnsignedInt(byte n)    { return n & 0xff; }
    private static int toUnsignedInt(short n)   { return n & 0xffff; }
    private static long toUnsignedLong(byte n)  { return n & 0xffL; }
    private static long toUnsignedLong(short n) { return n & 0xffffL; }
    private static long toUnsignedLong(int n)   { return n & 0xffffffffL; }

    /**
     * Performs a compare-and-set operation on an {@code int}
     * field within the given object.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @param expectedValue expected value of the field
     * @param newValue new value to store in the field if the contents are
     * as expected
     * @return {@code true} if the new value was in fact stored, and
     * {@code false} if not
     */
    @FastNative
    public native boolean compareAndSwapInt(Object obj, long offset,
            int expectedValue, int newValue);

    /**
     * Performs a compare-and-set operation on a {@code long}
     * field within the given object.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @param expectedValue expected value of the field
     * @param newValue new value to store in the field if the contents are
     * as expected
     * @return {@code true} if the new value was in fact stored, and
     * {@code false} if not
     */
    @FastNative
    public native boolean compareAndSwapLong(Object obj, long offset,
            long expectedValue, long newValue);

    /**
     * Performs a compare-and-set operation on an {@code obj}
     * field (that is, a reference field) within the given object.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @param expectedValue expected value of the field
     * @param newValue new value to store in the field if the contents are
     * as expected
     * @return {@code true} if the new value was in fact stored, and
     * {@code false} if not
     */
    @FastNative
    public native boolean compareAndSwapObject(Object obj, long offset,
            Object expectedValue, Object newValue);

    /**
     * Gets an {@code int} field from the given object,
     * using {@code volatile} semantics.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @return the retrieved value
     */
    @FastNative
    public native int getIntVolatile(Object obj, long offset);

    /**
     * Stores an {@code int} field into the given object,
     * using {@code volatile} semantics.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    @FastNative
    public native void putIntVolatile(Object obj, long offset, int newValue);

    /**
     * Gets a {@code long} field from the given object,
     * using {@code volatile} semantics.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @return the retrieved value
     */
    @FastNative
    public native long getLongVolatile(Object obj, long offset);

    /**
     * Stores a {@code long} field into the given object,
     * using {@code volatile} semantics.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    @FastNative
    public native void putLongVolatile(Object obj, long offset, long newValue);

    /**
     * Gets an {@code obj} field from the given object,
     * using {@code volatile} semantics.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @return the retrieved value
     */
    @FastNative
    public native Object getObjectVolatile(Object obj, long offset);

    /**
     * Stores an {@code obj} field into the given object,
     * using {@code volatile} semantics.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    @FastNative
    public native void putObjectVolatile(Object obj, long offset,
            Object newValue);

    /**
     * Gets an {@code int} field from the given object.
     *
     * @param obj non-{@code null}; object containing int field
     * @param offset offset to the field within {@code obj}
     * @return the retrieved value
     */
    @FastNative
    public native int getInt(Object obj, long offset);

    /**
     * Stores an {@code int} field into the given object.
     *
     * @param obj non-{@code null}; object containing int field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    @FastNative
    public native void putInt(Object obj, long offset, int newValue);

    /**
     * Lazy set an int field.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    @FastNative
    public native void putOrderedInt(Object obj, long offset, int newValue);

    /**
     * Gets a {@code long} field from the given object.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @return the retrieved value
     */
    @FastNative
    public native long getLong(Object obj, long offset);

    /**
     * Stores a {@code long} field into the given object.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    @FastNative
    public native void putLong(Object obj, long offset, long newValue);

    /**
     * Lazy set a long field.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    @FastNative
    public native void putOrderedLong(Object obj, long offset, long newValue);

    /**
     * Gets an {@code obj} field from the given object.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @return the retrieved value
     */
    @FastNative
    public native Object getObject(Object obj, long offset);

    /**
     * Stores an {@code obj} field into the given object.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    @FastNative
    public native void putObject(Object obj, long offset, Object newValue);

    /**
     * Lazy set an object field.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    @FastNative
    public native void putOrderedObject(Object obj, long offset,
            Object newValue);

    /**
     * Gets a {@code boolean} field from the given object.
     *
     * @param obj non-{@code null}; object containing boolean field
     * @param offset offset to the field within {@code obj}
     * @return the retrieved value
     */
    @FastNative
    public native boolean getBoolean(Object obj, long offset);

    /**
     * Stores a {@code boolean} field into the given object.
     *
     * @param obj non-{@code null}; object containing boolean field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    @FastNative
    public native void putBoolean(Object obj, long offset, boolean newValue);

    /**
     * Gets a {@code byte} field from the given object.
     *
     * @param obj non-{@code null}; object containing byte field
     * @param offset offset to the field within {@code obj}
     * @return the retrieved value
     */
    @FastNative
    public native byte getByte(Object obj, long offset);

    /**
     * Stores a {@code byte} field into the given object.
     *
     * @param obj non-{@code null}; object containing byte field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    @FastNative
    public native void putByte(Object obj, long offset, byte newValue);

    /**
     * Gets a {@code char} field from the given object.
     *
     * @param obj non-{@code null}; object containing char field
     * @param offset offset to the field within {@code obj}
     * @return the retrieved value
     */
    @FastNative
    public native char getChar(Object obj, long offset);

    /**
     * Stores a {@code char} field into the given object.
     *
     * @param obj non-{@code null}; object containing char field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    @FastNative
    public native void putChar(Object obj, long offset, char newValue);

    /**
     * Gets a {@code short} field from the given object.
     *
     * @param obj non-{@code null}; object containing short field
     * @param offset offset to the field within {@code obj}
     * @return the retrieved value
     */
    @FastNative
    public native short getShort(Object obj, long offset);

    /**
     * Stores a {@code short} field into the given object.
     *
     * @param obj non-{@code null}; object containing short field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    @FastNative
    public native void putShort(Object obj, long offset, short newValue);

    /**
     * Gets a {@code float} field from the given object.
     *
     * @param obj non-{@code null}; object containing float field
     * @param offset offset to the field within {@code obj}
     * @return the retrieved value
     */
    @FastNative
    public native float getFloat(Object obj, long offset);

    /**
     * Stores a {@code float} field into the given object.
     *
     * @param obj non-{@code null}; object containing float field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    @FastNative
    public native void putFloat(Object obj, long offset, float newValue);

    /**
     * Gets a {@code double} field from the given object.
     *
     * @param obj non-{@code null}; object containing double field
     * @param offset offset to the field within {@code obj}
     * @return the retrieved value
     */
    @FastNative
    public native double getDouble(Object obj, long offset);

    /**
     * Stores a {@code double} field into the given object.
     *
     * @param obj non-{@code null}; object containing double field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    @FastNative
    public native void putDouble(Object obj, long offset, double newValue);

    /**
     * Parks the calling thread for the specified amount of time,
     * unless the "permit" for the thread is already available (due to
     * a previous call to {@link #unpark}. This method may also return
     * spuriously (that is, without the thread being told to unpark
     * and without the indicated amount of time elapsing).
     *
     * <p>See {@link java.util.concurrent.locks.LockSupport} for more
     * in-depth information of the behavior of this method.</p>
     *
     * @param absolute whether the given time value is absolute
     * milliseconds-since-the-epoch ({@code true}) or relative
     * nanoseconds-from-now ({@code false})
     * @param time the (absolute millis or relative nanos) time value
     */

    public native void park(boolean absolute, long time);
    /**
     * Unparks the given object, which must be a {@link Thread}.
     *
     * <p>See {@link java.util.concurrent.locks.LockSupport} for more
     * in-depth information of the behavior of this method.</p>
     *
     * @param obj non-{@code null}; the object to unpark
     */
    @FastNative
    public native void unpark(Object obj);

    /**
     * Allocates an instance of the given class without running the constructor.
     * The class' <clinit> will be run, if necessary.
     */
    public native Object allocateInstance(Class<?> c);

    /**
     * Gets the size of the address value, in bytes.
     *
     * @return the size of the address, in bytes
     */
    @FastNative
    public native int addressSize();

    /**
     * Gets the size of the memory page, in bytes.
     *
     * @return the size of the page
     */
    @FastNative
    public native int pageSize();

    /**
     * Allocates a memory block of size {@code bytes}.
     *
     * @param bytes size of the memory block
     * @return address of the allocated memory
     */
    @FastNative
    public native long allocateMemory(long bytes);

    /**
     * Frees previously allocated memory at given address.
     *
     * @param address address of the freed memory
     */
    @FastNative
    public native void freeMemory(long address);

    /**
     * Fills given memory block with a given value.
     *
     * @param address address of the memoory block
     * @param bytes length of the memory block, in bytes
     * @param value fills memory with this value
     */
    @FastNative
    public native void setMemory(long address, long bytes, byte value);

    /**
     * Gets {@code byte} from given address in memory.
     *
     * @param address address in memory
     * @return {@code byte} value
     */
    @FastNative
    public native byte getByte(long address);

    /**
     * Stores a {@code byte} into the given memory address.
     *
     * @param address address in memory where to store the value
     * @param newValue the value to store
     */
    @FastNative
    public native void putByte(long address, byte x);

    /**
     * Gets {@code short} from given address in memory.
     *
     * @param address address in memory
     * @return {@code short} value
     */
    @FastNative
    public native short getShort(long address);

    /**
     * Stores a {@code short} into the given memory address.
     *
     * @param address address in memory where to store the value
     * @param newValue the value to store
     */
    @FastNative
    public native void putShort(long address, short x);

    /**
     * Gets {@code char} from given address in memory.
     *
     * @param address address in memory
     * @return {@code char} value
     */
    @FastNative
    public native char getChar(long address);

    /**
     * Stores a {@code char} into the given memory address.
     *
     * @param address address in memory where to store the value
     * @param newValue the value to store
     */
    @FastNative
    public native void putChar(long address, char x);

    /**
     * Gets {@code int} from given address in memory.
     *
     * @param address address in memory
     * @return {@code int} value
     */
    @FastNative
    public native int getInt(long address);

    /**
     * Stores a {@code int} into the given memory address.
     *
     * @param address address in memory where to store the value
     * @param newValue the value to store
     */
    @FastNative
    public native void putInt(long address, int x);


    /**
     * Gets {@code long} from given address in memory.
     *
     * @param address address in memory
     * @return {@code long} value
     */
    @FastNative
    public native long getLong(long address);

    /**
     * Stores a {@code long} into the given memory address.
     *
     * @param address address in memory where to store the value
     * @param newValue the value to store
     */
    @FastNative
    public native void putLong(long address, long x);

    /**
     * Gets {@code long} from given address in memory.
     *
     * @param address address in memory
     * @return {@code long} value
     */
    @FastNative
    public native float getFloat(long address);

    /**
     * Stores a {@code float} into the given memory address.
     *
     * @param address address in memory where to store the value
     * @param newValue the value to store
     */
    @FastNative
    public native void putFloat(long address, float x);

    /**
     * Gets {@code double} from given address in memory.
     *
     * @param address address in memory
     * @return {@code double} value
     */
    @FastNative
    public native double getDouble(long address);

    /**
     * Stores a {@code double} into the given memory address.
     *
     * @param address address in memory where to store the value
     * @param newValue the value to store
     */
    @FastNative
    public native void putDouble(long address, double x);

    /**
     * Sets all bytes in a given block of memory to a copy of another
     * block.
     *
     * This method is to be used to copy memory between array objects. The
     * offsets used should be relative to the value reported by {@link
     * #arrayBaseOffset}. For example to copy all elements of an integer
     * array to another:
     *
     * <pre> {@code
     *   unsafe.copyMemory(srcArray, Unsafe.ARRAY_INT_BASE_OFFSET,
     *                     destArray, Unsafe.ARRAY_INT_BASE_OFFSET,
     *                     srcArray.length * 4);
     * }</pre>
     *
     * @param srcBase The source array object from which to copy
     * @param srcOffset The offset within the object from where to copy
     * @param destBase The destination array object to which to copy
     * @param destOffset The offset within the object to where to copy
     * @param bytes The number of bytes to copy
     *
     * @throws RuntimeException if any of the arguments is invalid
     */
    public void copyMemory(Object srcBase, long srcOffset,
                           Object destBase, long destOffset,
                           long bytes) {
        copyMemoryChecks(srcBase, srcOffset, destBase, destOffset, bytes);

        if (bytes == 0) {
            return;
        }

        copyMemory0(srcBase, srcOffset, destBase, destOffset, bytes);
    }

    /**
     * Sets all bytes in a given block of memory to a copy of another block.
     *
     * @param srcAddr address of the source memory to be copied from
     * @param dstAddr address of the destination memory to copy to
     * @param bytes number of bytes to copy
     */
    public void copyMemory(long srcAddr, long dstAddr, long bytes) {
        copyMemory(null, srcAddr, null, dstAddr, bytes);
    }

    /**
     * Validate the arguments to copyMemory
     *
     * @throws RuntimeException if any of the arguments is invalid
     *         (<em>Note:</em> after optimization, invalid inputs may
     *         go undetected, which will lead to unpredictable
     *         behavior)
     */
    private void copyMemoryChecks(Object srcBase, long srcOffset,
                                  Object destBase, long destOffset,
                                  long bytes) {
        checkSize(bytes);
        checkPrimitivePointer(srcBase, srcOffset);
        checkPrimitivePointer(destBase, destOffset);
    }

    @HotSpotIntrinsicCandidate
    @FastNative
    private native void copyMemory0(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes);

    /**
     * Atomically updates Java variable to {@code x} if it is currently
     * holding {@code expected}.
     *
     * <p>This operation has memory semantics of a {@code volatile} read
     * and write.  Corresponds to C11 atomic_compare_exchange_strong.
     *
     * @return {@code true} if successful
     */
    @HotSpotIntrinsicCandidate
    @FastNative
    public final native boolean compareAndSetInt(Object o, long offset,
                                                 int expected,
                                                 int x);

    /**
     * Atomically updates Java variable to {@code x} if it is currently
     * holding {@code expected}.
     *
     * <p>This operation has memory semantics of a {@code volatile} read
     * and write.  Corresponds to C11 atomic_compare_exchange_strong.
     *
     * @return {@code true} if successful
     */
    @HotSpotIntrinsicCandidate
    @FastNative
    public final native boolean compareAndSetLong(Object o, long offset,
                                                  long expected,
                                                  long x);

    /**
     * Atomically updates Java variable to {@code x} if it is currently
     * holding {@code expected}.
     *
     * <p>This operation has memory semantics of a {@code volatile} read
     * and write.  Corresponds to C11 atomic_compare_exchange_strong.
     *
     * @return {@code true} if successful
     */
    @HotSpotIntrinsicCandidate
    @FastNative
    public final native boolean compareAndSetObject(Object o, long offset,
                                                    Object expected,
                                                    Object x);

    // The following contain CAS-based Java implementations used on
    // platforms not supporting native instructions

    /**
     * Atomically adds the given value to the current value of a field
     * or array element within the given object {@code o}
     * at the given {@code offset}.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param delta the value to add
     * @return the previous value
     * @since 1.8
     */
    @HotSpotIntrinsicCandidate
    public final int getAndAddInt(Object o, long offset, int delta) {
        int v;
        do {
            v = getIntVolatile(o, offset);
        } while (!compareAndSwapInt(o, offset, v, v + delta));
        return v;
    }

    /**
     * Atomically adds the given value to the current value of a field
     * or array element within the given object {@code o}
     * at the given {@code offset}.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param delta the value to add
     * @return the previous value
     * @since 1.8
     */
    @HotSpotIntrinsicCandidate
    public final long getAndAddLong(Object o, long offset, long delta) {
        long v;
        do {
            v = getLongVolatile(o, offset);
        } while (!compareAndSwapLong(o, offset, v, v + delta));
        return v;
    }

    /**
     * Atomically exchanges the given value with the current value of
     * a field or array element within the given object {@code o}
     * at the given {@code offset}.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param newValue new value
     * @return the previous value
     * @since 1.8
     */
    @HotSpotIntrinsicCandidate
    public final int getAndSetInt(Object o, long offset, int newValue) {
        int v;
        do {
            v = getIntVolatile(o, offset);
        } while (!compareAndSwapInt(o, offset, v, newValue));
        return v;
    }

    /**
     * Atomically exchanges the given value with the current value of
     * a field or array element within the given object {@code o}
     * at the given {@code offset}.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param newValue new value
     * @return the previous value
     * @since 1.8
     */
    @HotSpotIntrinsicCandidate
    public final long getAndSetLong(Object o, long offset, long newValue) {
        long v;
        do {
            v = getLongVolatile(o, offset);
        } while (!compareAndSwapLong(o, offset, v, newValue));
        return v;
    }

    /**
     * Atomically exchanges the given reference value with the current
     * reference value of a field or array element within the given
     * object {@code o} at the given {@code offset}.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param newValue new value
     * @return the previous value
     * @since 1.8
     */
    @HotSpotIntrinsicCandidate
    public final Object getAndSetObject(Object o, long offset, Object newValue) {
        Object v;
        do {
            v = getObjectVolatile(o, offset);
        } while (!compareAndSwapObject(o, offset, v, newValue));
        return v;
    }

    /** Release version of {@link #putIntVolatile(Object, long, int)} */
    @HotSpotIntrinsicCandidate
    public final void putIntRelease(Object o, long offset, int x) {
        putIntVolatile(o, offset, x);
    }

    /** Acquire version of {@link #getIntVolatile(Object, long)} */
    @HotSpotIntrinsicCandidate
    public final int getIntAcquire(Object o, long offset) {
        return getIntVolatile(o, offset);
    }

    /** Release version of {@link #putLongVolatile(Object, long, long)} */
    @HotSpotIntrinsicCandidate
    public final void putLongRelease(Object o, long offset, long x) {
        putLongVolatile(o, offset, x);
    }

    /** Acquire version of {@link #getLongVolatile(Object, long)} */
    @HotSpotIntrinsicCandidate
    public final long getLongAcquire(Object o, long offset) {
        return getLongVolatile(o, offset);
    }

    /** Release version of {@link #putObjectVolatile(Object, long, Object)} */
    @HotSpotIntrinsicCandidate
    public final void putObjectRelease(Object o, long offset, Object x) {
        putObjectVolatile(o, offset, x);
    }

    /** Acquire version of {@link #getObjectVolatile(Object, long)} */
    @HotSpotIntrinsicCandidate
    public final Object getObjectAcquire(Object o, long offset) {
        return getObjectVolatile(o, offset);
    }

    /**
     * Ensures that loads before the fence will not be reordered with loads and
     * stores after the fence; a "LoadLoad plus LoadStore barrier".
     *
     * Corresponds to C11 atomic_thread_fence(memory_order_acquire)
     * (an "acquire fence").
     *
     * A pure LoadLoad fence is not provided, since the addition of LoadStore
     * is almost always desired, and most current hardware instructions that
     * provide a LoadLoad barrier also provide a LoadStore barrier for free.
     * @since 1.8
     */
    @HotSpotIntrinsicCandidate
    @FastNative
    public native void loadFence();

    /**
     * Ensures that loads and stores before the fence will not be reordered with
     * stores after the fence; a "StoreStore plus LoadStore barrier".
     *
     * Corresponds to C11 atomic_thread_fence(memory_order_release)
     * (a "release fence").
     *
     * A pure StoreStore fence is not provided, since the addition of LoadStore
     * is almost always desired, and most current hardware instructions that
     * provide a StoreStore barrier also provide a LoadStore barrier for free.
     * @since 1.8
     */
    @HotSpotIntrinsicCandidate
    @FastNative
    public native void storeFence();

    /**
     * Ensures that loads and stores before the fence will not be reordered
     * with loads and stores after the fence.  Implies the effects of both
     * loadFence() and storeFence(), and in addition, the effect of a StoreLoad
     * barrier.
     *
     * Corresponds to C11 atomic_thread_fence(memory_order_seq_cst).
     * @since 1.8
     */
    @HotSpotIntrinsicCandidate
    @FastNative
    public native void fullFence();

    /**
     * Ensures the given class has been initialized. This is often
     * needed in conjunction with obtaining the static field base of a
     * class.
     */
    public void ensureClassInitialized(Class<?> c) {
        if (c == null) {
            throw new NullPointerException();
        }

        // Android-changed: Implementation not yet available natively (b/202380950)
        // ensureClassInitialized0(c);
        try {
            Class.forName(c.getName(), true, c.getClassLoader());
        } catch (ClassNotFoundException e) {
            // The function doesn't specify that it's throwing ClassNotFoundException, so it needs
            // to be caught here. We could rethrow as NoClassDefFoundError, however that is not
            // documented for this function and the upstream implementation does not throw an
            // exception.
        }
    }


    /// helper methods for validating various types of objects/values

    /**
     * Create an exception reflecting that some of the input was invalid
     *
     * <em>Note:</em> It is the resposibility of the caller to make
     * sure arguments are checked before the methods are called. While
     * some rudimentary checks are performed on the input, the checks
     * are best effort and when performance is an overriding priority,
     * as when methods of this class are optimized by the runtime
     * compiler, some or all checks (if any) may be elided. Hence, the
     * caller must not rely on the checks and corresponding
     * exceptions!
     *
     * @return an exception object
     */
    private RuntimeException invalidInput() {
        return new IllegalArgumentException();
    }

    /**
     * Check if a value is 32-bit clean (32 MSB are all zero)
     *
     * @param value the 64-bit value to check
     *
     * @return true if the value is 32-bit clean
     */
    private boolean is32BitClean(long value) {
        return value >>> 32 == 0;
    }

    /**
     * Check the validity of a size (the equivalent of a size_t)
     *
     * @throws RuntimeException if the size is invalid
     *         (<em>Note:</em> after optimization, invalid inputs may
     *         go undetected, which will lead to unpredictable
     *         behavior)
     */
    private void checkSize(long size) {
        if (ADDRESS_SIZE == 4) {
            // Note: this will also check for negative sizes
            if (!is32BitClean(size)) {
                throw invalidInput();
            }
        } else if (size < 0) {
            throw invalidInput();
        }
    }

    /**
     * Check the validity of a native address (the equivalent of void*)
     *
     * @throws RuntimeException if the address is invalid
     *         (<em>Note:</em> after optimization, invalid inputs may
     *         go undetected, which will lead to unpredictable
     *         behavior)
     */
    private void checkNativeAddress(long address) {
        if (ADDRESS_SIZE == 4) {
            // Accept both zero and sign extended pointers. A valid
            // pointer will, after the +1 below, either have produced
            // the value 0x0 or 0x1. Masking off the low bit allows
            // for testing against 0.
            if ((((address >> 32) + 1) & ~1) != 0) {
                throw invalidInput();
            }
        }
    }

    /**
     * Check the validity of an offset, relative to a base object
     *
     * @param o the base object
     * @param offset the offset to check
     *
     * @throws RuntimeException if the size is invalid
     *         (<em>Note:</em> after optimization, invalid inputs may
     *         go undetected, which will lead to unpredictable
     *         behavior)
     */
    private void checkOffset(Object o, long offset) {
        if (ADDRESS_SIZE == 4) {
            // Note: this will also check for negative offsets
            if (!is32BitClean(offset)) {
                throw invalidInput();
            }
        } else if (offset < 0) {
            throw invalidInput();
        }
    }

    /**
     * Check the validity of a double-register pointer
     *
     * Note: This code deliberately does *not* check for NPE for (at
     * least) three reasons:
     *
     * 1) NPE is not just NULL/0 - there is a range of values all
     * resulting in an NPE, which is not trivial to check for
     *
     * 2) It is the responsibility of the callers of Unsafe methods
     * to verify the input, so throwing an exception here is not really
     * useful - passing in a NULL pointer is a critical error and the
     * must not expect an exception to be thrown anyway.
     *
     * 3) the actual operations will detect NULL pointers anyway by
     * means of traps and signals (like SIGSEGV).
     *
     * @param o Java heap object, or null
     * @param offset indication of where the variable resides in a Java heap
     *        object, if any, else a memory address locating the variable
     *        statically
     *
     * @throws RuntimeException if the pointer is invalid
     *         (<em>Note:</em> after optimization, invalid inputs may
     *         go undetected, which will lead to unpredictable
     *         behavior)
     */
    private void checkPointer(Object o, long offset) {
        if (o == null) {
            checkNativeAddress(offset);
        } else {
            checkOffset(o, offset);
        }
    }

    /**
     * Check if a type is a primitive array type
     *
     * @param c the type to check
     *
     * @return true if the type is a primitive array type
     */
    private void checkPrimitiveArray(Class<?> c) {
        Class<?> componentType = c.getComponentType();
        if (componentType == null || !componentType.isPrimitive()) {
            throw invalidInput();
        }
    }

    /**
     * Check that a pointer is a valid primitive array type pointer
     *
     * Note: pointers off-heap are considered to be primitive arrays
     *
     * @throws RuntimeException if the pointer is invalid
     *         (<em>Note:</em> after optimization, invalid inputs may
     *         go undetected, which will lead to unpredictable
     *         behavior)
     */
    private void checkPrimitivePointer(Object o, long offset) {
        checkPointer(o, offset);

        if (o != null) {
            // If on heap, it must be a primitive array
            checkPrimitiveArray(o.getClass());
        }
    }



}
