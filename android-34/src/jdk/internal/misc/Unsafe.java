/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
import jdk.internal.vm.annotation.IntrinsicCandidate;
import sun.reflect.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static jdk.internal.misc.UnsafeConstants.*;

/**
 * A collection of methods for performing low-level, unsafe operations.
 * Although the class and all methods are public, use of this class is
 * limited because only trusted code can obtain instances of it.
 *
 * <em>Note:</em> It is the responsibility of the caller to make sure
 * arguments are checked before methods of this class are
 * called. While some rudimentary checks are performed on the input,
 * the checks are best effort and when performance is an overriding
 * priority, as when methods of this class are optimized by the
 * runtime compiler, some or all checks (if any) may be elided. Hence,
 * the caller must not rely on the checks and corresponding
 * exceptions!
 *
 * @author John R. Rose
 * @see #getUnsafe
 */
public final class Unsafe {
    /** Traditional dalvik name. */
    private static final Unsafe THE_ONE = new Unsafe();

    private static final Unsafe theUnsafe = THE_ONE;

    /**
     * This class is only privately instantiable.
     */
    private Unsafe() {}

    /**
     * Gets the unique instance of this class. This is only allowed in
     * very limited situations.
     */
    public static Unsafe getUnsafe() {
        // BEGIN Android-changed: Check caller is in bootclasspath.
        // return theUnsafe;
        Class<?> caller = Reflection.getCallerClass();
        /*
         * Only code on the bootclasspath is allowed to get at the
         * Unsafe instance.
         */
        ClassLoader calling = (caller == null) ? null : caller.getClassLoader();
        if ((calling != null) && (calling != Unsafe.class.getClassLoader())) {
            throw new SecurityException("Unsafe access denied");
        // END Android-changed: Check caller is in bootclasspath.
        }

        return THE_ONE;
    }

    /// peek and poke operations
    /// (compilers should optimize these to memory ops)

    // These work on object fields in the Java heap.
    // They will not work on elements of packed arrays.

    /**
     * Fetches a value from a given Java variable.
     * More specifically, fetches a field or array element within the given
     * object {@code o} at the given offset, or (if {@code o} is null)
     * from the memory address whose numerical value is the given offset.
     * <p>
     * The results are undefined unless one of the following cases is true:
     * <ul>
     * <li>The offset was obtained from {@link #objectFieldOffset} on
     * the {@link java.lang.reflect.Field} of some Java field and the object
     * referred to by {@code o} is of a class compatible with that
     * field's class.
     *
     * <li>The offset and object reference {@code o} (either null or
     * non-null) were both obtained via {@link #staticFieldOffset}
     * and {@link #staticFieldBase} (respectively) from the
     * reflective {@link Field} representation of some Java field.
     *
     * <li>The object referred to by {@code o} is an array, and the offset
     * is an integer of the form {@code B+N*S}, where {@code N} is
     * a valid index into the array, and {@code B} and {@code S} are
     * the values obtained by {@link #arrayBaseOffset} and {@link
     * #arrayIndexScale} (respectively) from the array's class.  The value
     * referred to is the {@code N}<em>th</em> element of the array.
     *
     * </ul>
     * <p>
     * If one of the above cases is true, the call references a specific Java
     * variable (field or array element).  However, the results are undefined
     * if that variable is not in fact of the type returned by this method.
     * <p>
     * This method refers to a variable by means of two parameters, and so
     * it provides (in effect) a <em>double-register</em> addressing mode
     * for Java variables.  When the object reference is null, this method
     * uses its offset as an absolute address.  This is similar in operation
     * to methods such as {@link #getInt(long)}, which provide (in effect) a
     * <em>single-register</em> addressing mode for non-Java variables.
     * However, because Java variables may have a different layout in memory
     * from non-Java variables, programmers should not assume that these
     * two addressing modes are ever equivalent.  Also, programmers should
     * remember that offsets from the double-register addressing mode cannot
     * be portably confused with longs used in the single-register addressing
     * mode.
     *
     * @param o Java heap object in which the variable resides, if any, else
     *        null
     * @param offset indication of where the variable resides in a Java heap
     *        object, if any, else a memory address locating the variable
     *        statically
     * @return the value fetched from the indicated Java variable
     * @throws RuntimeException No defined exceptions are thrown, not even
     *         {@link NullPointerException}
     */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native int getInt(Object o, long offset);

    /**
     * Stores a value into a given Java variable.
     * <p>
     * The first two parameters are interpreted exactly as with
     * {@link #getInt(Object, long)} to refer to a specific
     * Java variable (field or array element).  The given value
     * is stored into that variable.
     * <p>
     * The variable must be of the same type as the method
     * parameter {@code x}.
     *
     * @param o Java heap object in which the variable resides, if any, else
     *        null
     * @param offset indication of where the variable resides in a Java heap
     *        object, if any, else a memory address locating the variable
     *        statically
     * @param x the value to store into the indicated Java variable
     * @throws RuntimeException No defined exceptions are thrown, not even
     *         {@link NullPointerException}
     */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native void putInt(Object o, long offset, int x);

    /**
     * Fetches a reference value from a given Java variable.
     * @see #getInt(Object, long)
     */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native Object getReference(Object o, long offset);

    /**
     * Stores a reference value into a given Java variable.
     * <p>
     * Unless the reference {@code x} being stored is either null
     * or matches the field type, the results are undefined.
     * If the reference {@code o} is non-null, card marks or
     * other store barriers for that object (if the VM requires them)
     * are updated.
     * @see #putInt(Object, long, int)
     */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native void putReference(Object o, long offset, Object x);

    /** @see #getInt(Object, long) */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native boolean getBoolean(Object o, long offset);

    /** @see #putInt(Object, long, int) */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native void    putBoolean(Object o, long offset, boolean x);

    /** @see #getInt(Object, long) */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native byte    getByte(Object o, long offset);

    /** @see #putInt(Object, long, int) */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native void    putByte(Object o, long offset, byte x);

    /** @see #getInt(Object, long) */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native short   getShort(Object o, long offset);

    /** @see #putInt(Object, long, int) */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native void    putShort(Object o, long offset, short x);

    /** @see #getInt(Object, long) */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native char    getChar(Object o, long offset);

    /** @see #putInt(Object, long, int) */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native void    putChar(Object o, long offset, char x);

    /** @see #getInt(Object, long) */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native long    getLong(Object o, long offset);

    /** @see #putInt(Object, long, int) */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native void    putLong(Object o, long offset, long x);

    /** @see #getInt(Object, long) */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native float   getFloat(Object o, long offset);

    /** @see #putInt(Object, long, int) */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native void    putFloat(Object o, long offset, float x);

    /** @see #getInt(Object, long) */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native double  getDouble(Object o, long offset);

    /** @see #putInt(Object, long, int) */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native void    putDouble(Object o, long offset, double x);

    // BEGIN Android-removed: Not used in Android.
    /*
    /**
     * Fetches a native pointer from a given memory address.  If the address is
     * zero, or does not point into a block obtained from {@link
     * #allocateMemory}, the results are undefined.
     *
     * <p>If the native pointer is less than 64 bits wide, it is extended as
     * an unsigned number to a Java long.  The pointer may be indexed by any
     * given byte offset, simply by adding that offset (as a simple integer) to
     * the long representing the pointer.  The number of bytes actually read
     * from the target address may be determined by consulting {@link
     * #addressSize}.
     *
     * @see #allocateMemory
     * @see #getInt(Object, long)
     * /
    @ForceInline
    public long getAddress(Object o, long offset) {
        if (ADDRESS_SIZE == 4) {
            return Integer.toUnsignedLong(getInt(o, offset));
        } else {
            return getLong(o, offset);
        }
    }

    /**
     * Stores a native pointer into a given memory address.  If the address is
     * zero, or does not point into a block obtained from {@link
     * #allocateMemory}, the results are undefined.
     *
     * <p>The number of bytes actually written at the target address may be
     * determined by consulting {@link #addressSize}.
     *
     * @see #allocateMemory
     * @see #putInt(Object, long, int)
     * /
    @ForceInline
    public void putAddress(Object o, long offset, long x) {
        if (ADDRESS_SIZE == 4) {
            putInt(o, offset, (int)x);
        } else {
            putLong(o, offset, x);
        }
    }

    // These read VM internal data.

    /**
     * Fetches an uncompressed reference value from a given native variable
     * ignoring the VM's compressed references mode.
     *
     * @param address a memory address locating the variable
     * @return the value fetched from the indicated native variable
     * /
    public native Object getUncompressedObject(long address);

     */
    // END Android-removed: Not used in Android.

    /**
     * Fetches a value from a given memory address.  If the address is zero, or
     * does not point into a block obtained from {@link #allocateMemory}, the
     * results are undefined.
     *
     * @see #allocateMemory
     */
    // BEGIN Android-changed: Implemented as native call.
    /*
    @ForceInline
    public byte getByte(long address) {
        return getByte(null, address);
    }
     */
    @FastNative
    public native byte getByte(long address);
    // END Android-changed: Implemented as native call.

    /**
     * Stores a value into a given memory address.  If the address is zero, or
     * does not point into a block obtained from {@link #allocateMemory}, the
     * results are undefined.
     *
     * @see #getByte(long)
     */
    // BEGIN Android-changed: Implemented as native call.
    /*
    @ForceInline
    public void putByte(long address, byte x) {
        putByte(null, address, x);
    }
     */
    @FastNative
    public native void putByte(long address, byte x);
    // END Android-changed: Implemented as native call.


    /** @see #getByte(long) */
    // BEGIN Android-changed: Implemented as native call.
    /*
    @ForceInline
    public short getShort(long address) {
        return getShort(null, address);
    }
     */
    @FastNative
    public native short getShort(long address);
    // END Android-changed: Implemented as native call.

    /** @see #putByte(long, byte) */
    // BEGIN Android-changed: Implemented as native call.
    /*
    @ForceInline
    public void putShort(long address, short x) {
        putShort(null, address, x);
    }
     */
    @FastNative
    public native void putShort(long address, short x);
    // END Android-changed: Implemented as native call.

    /** @see #getByte(long) */
    // BEGIN Android-changed: Implemented as native call.
    /*
    @ForceInline
    public char getChar(long address) {
        return getChar(null, address);
    }
     */
    @FastNative
    public native char getChar(long address);
    // END Android-changed: Implemented as native call.

    /** @see #putByte(long, byte) */
    // BEGIN Android-changed: Implemented as native call.
    /*
    @ForceInline
    public void putChar(long address, char x) {
        putChar(null, address, x);
    }
     */
    @FastNative
    public native void putChar(long address, char x);
    // END Android-changed: Implemented as native call.

    /** @see #getByte(long) */
    // BEGIN Android-changed: Implemented as native call.
    /*
    @ForceInline
    public int getInt(long address) {
        return getInt(null, address);
    }
     */
    @FastNative
    public native int getInt(long address);
    // END Android-changed: Implemented as native call.

    /** @see #putByte(long, byte) */
    // BEGIN Android-changed: Implemented as native call.
    /*
    @ForceInline
    public void putInt(long address, int x) {
        putInt(null, address, x);
    }
     */
    @FastNative
    public native void putInt(long address, int x);
    // END Android-changed: Implemented as native call.

    /** @see #getByte(long) */
    // BEGIN Android-changed: Implemented as native call.
    /*
    @ForceInline
    public long getLong(long address) {
        return getLong(null, address);
    }
     */
    @FastNative
    public native long getLong(long address);
    // END Android-changed: Implemented as native call.

    /** @see #putByte(long, byte) */
    // BEGIN Android-changed: Implemented as native call.
    /*
    @ForceInline
    public void putLong(long address, long x) {
        putLong(null, address, x);
    }
     */
    @FastNative
    public native void putLong(long address, long x);
    // END Android-changed: Implemented as native call.

    /** @see #getByte(long) */
    // BEGIN Android-changed: Implemented as native call.
    /*
    @ForceInline
    public float getFloat(long address) {
        return getFloat(null, address);
    }
     */
    @FastNative
    public native float getFloat(long address);
    // END Android-changed: Implemented as native call.

    /** @see #putByte(long, byte) */
    // BEGIN Android-changed: Implemented as native call.
    /*
    @ForceInline
    public void putFloat(long address, float x) {
        putFloat(null, address, x);
    }
     */
    @FastNative
    public native void putFloat(long address, float x);
    // END Android-changed: Implemented as native call.

    /** @see #getByte(long) */
    // BEGIN Android-changed: Implemented as native call.
    /*
    @ForceInline
    public double getDouble(long address) {
        return getDouble(null, address);
    }
     */
    @FastNative
    public native double getDouble(long address);
    // END Android-changed: Implemented as native call.

    /** @see #putByte(long, byte) */
    // BEGIN Android-changed: Implemented as native call.
    /*
    @ForceInline
    public void putDouble(long address, double x) {
        putDouble(null, address, x);
    }
     */
    @FastNative
    public native void putDouble(long address, double x);
    // END Android-changed: Implemented as native call.

    // BEGIN Android-removed: Not used in Android.
    /*
    /** @see #getAddress(Object, long) * /
    @ForceInline
    public long getAddress(long address) {
        return getAddress(null, address);
    }

    /** @see #putAddress(Object, long, long) * /
    @ForceInline
    public void putAddress(long address, long x) {
        putAddress(null, address, x);
    }
     */
    // END Android-removed: Not used in Android.

    /// helper methods for validating various types of objects/values

    /**
     * Create an exception reflecting that some of the input was invalid
     *
     * <em>Note:</em> It is the responsibility of the caller to make
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

    /// wrappers for malloc, realloc, free:

    // BEGIN Android-removed: Not used in Android.
    /*
    /**
     * Round up allocation size to a multiple of HeapWordSize.
     * /
    private long alignToHeapWordSize(long bytes) {
        if (bytes >= 0) {
            return (bytes + ADDRESS_SIZE - 1) & ~(ADDRESS_SIZE - 1);
        } else {
            throw invalidInput();
        }
    }
     */
    // END Android-removed: Not used in Android.

    /**
     * Allocates a new block of native memory, of the given size in bytes.  The
     * contents of the memory are uninitialized; they will generally be
     * garbage.  The resulting native pointer will never be zero, and will be
     * aligned for all value types.  Dispose of this memory by calling {@link
     * #freeMemory}, or resize it with {@link #reallocateMemory}.
     *
     * <em>Note:</em> It is the responsibility of the caller to make
     * sure arguments are checked before the methods are called. While
     * some rudimentary checks are performed on the input, the checks
     * are best effort and when performance is an overriding priority,
     * as when methods of this class are optimized by the runtime
     * compiler, some or all checks (if any) may be elided. Hence, the
     * caller must not rely on the checks and corresponding
     * exceptions!
     *
     * @throws RuntimeException if the size is negative or too large
     *         for the native size_t type
     *
     * @throws OutOfMemoryError if the allocation is refused by the system
     *
     * @see #getByte(long)
     * @see #putByte(long, byte)
     */
    @FastNative
    public native long allocateMemory(long bytes);
    // BEGIN Android-removed: Not used in Android.
    /*
    public long allocateMemory(long bytes) {
        bytes = alignToHeapWordSize(bytes);

        allocateMemoryChecks(bytes);

        if (bytes == 0) {
            return 0;
        }

        long p = allocateMemory0(bytes);
        if (p == 0) {
            throw new OutOfMemoryError("Unable to allocate " + bytes + " bytes");
        }

        return p;
    }

    /**
     * Validate the arguments to allocateMemory
     *
     * @throws RuntimeException if the arguments are invalid
     *         (<em>Note:</em> after optimization, invalid inputs may
     *         go undetected, which will lead to unpredictable
     *         behavior)
     * /
    private void allocateMemoryChecks(long bytes) {
        checkSize(bytes);
    }

    /**
     * Resizes a new block of native memory, to the given size in bytes.  The
     * contents of the new block past the size of the old block are
     * uninitialized; they will generally be garbage.  The resulting native
     * pointer will be zero if and only if the requested size is zero.  The
     * resulting native pointer will be aligned for all value types.  Dispose
     * of this memory by calling {@link #freeMemory}, or resize it with {@link
     * #reallocateMemory}.  The address passed to this method may be null, in
     * which case an allocation will be performed.
     *
     * <em>Note:</em> It is the responsibility of the caller to make
     * sure arguments are checked before the methods are called. While
     * some rudimentary checks are performed on the input, the checks
     * are best effort and when performance is an overriding priority,
     * as when methods of this class are optimized by the runtime
     * compiler, some or all checks (if any) may be elided. Hence, the
     * caller must not rely on the checks and corresponding
     * exceptions!
     *
     * @throws RuntimeException if the size is negative or too large
     *         for the native size_t type
     *
     * @throws OutOfMemoryError if the allocation is refused by the system
     *
     * @see #allocateMemory
     * /
    public long reallocateMemory(long address, long bytes) {
        bytes = alignToHeapWordSize(bytes);

        reallocateMemoryChecks(address, bytes);

        if (bytes == 0) {
            freeMemory(address);
            return 0;
        }

        long p = (address == 0) ? allocateMemory0(bytes) : reallocateMemory0(address, bytes);
        if (p == 0) {
            throw new OutOfMemoryError("Unable to allocate " + bytes + " bytes");
        }

        return p;
    }

    /**
     * Validate the arguments to reallocateMemory
     *
     * @throws RuntimeException if the arguments are invalid
     *         (<em>Note:</em> after optimization, invalid inputs may
     *         go undetected, which will lead to unpredictable
     *         behavior)
     * /
    private void reallocateMemoryChecks(long address, long bytes) {
        checkPointer(null, address);
        checkSize(bytes);
    }

    /**
     * Sets all bytes in a given block of memory to a fixed value
     * (usually zero).
     *
     * <p>This method determines a block's base address by means of two parameters,
     * and so it provides (in effect) a <em>double-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.  When the object reference is null,
     * the offset supplies an absolute base address.
     *
     * <p>The stores are in coherent (atomic) units of a size determined
     * by the address and length parameters.  If the effective address and
     * length are all even modulo 8, the stores take place in 'long' units.
     * If the effective address and length are (resp.) even modulo 4 or 2,
     * the stores take place in units of 'int' or 'short'.
     *
     * <em>Note:</em> It is the responsibility of the caller to make
     * sure arguments are checked before the methods are called. While
     * some rudimentary checks are performed on the input, the checks
     * are best effort and when performance is an overriding priority,
     * as when methods of this class are optimized by the runtime
     * compiler, some or all checks (if any) may be elided. Hence, the
     * caller must not rely on the checks and corresponding
     * exceptions!
     *
     * @throws RuntimeException if any of the arguments is invalid
     *
     * @since 1.7
     * /
    public void setMemory(Object o, long offset, long bytes, byte value) {
        setMemoryChecks(o, offset, bytes, value);

        if (bytes == 0) {
            return;
        }

        setMemory0(o, offset, bytes, value);
    }
     */
    // END Android-removed: Not used in Android.

    // BEGIN Android-changed: setMemory implemented as a native call.
    /**
     * Fills given memory block with a given value.
     *
     * @param address address of the memoory block
     * @param bytes length of the memory block, in bytes
     * @param value fills memory with this value
     */
    @FastNative
    public native void setMemory(long address, long bytes, byte value);
    /*
    /**
     * Sets all bytes in a given block of memory to a fixed value
     * (usually zero).  This provides a <em>single-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.
     *
     * <p>Equivalent to {@code setMemory(null, address, bytes, value)}.
     * /
    public void setMemory(long address, long bytes, byte value) {
        setMemory(null, address, bytes, value);
    }

    /**
     * Validate the arguments to setMemory
     *
     * @throws RuntimeException if the arguments are invalid
     *         (<em>Note:</em> after optimization, invalid inputs may
     *         go undetected, which will lead to unpredictable
     *         behavior)
     * /
    private void setMemoryChecks(Object o, long offset, long bytes, byte value) {
        checkPrimitivePointer(o, offset);
        checkSize(bytes);
    }
     */
    // END Android-changed: setMemory implemented as a native call.

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
     * <em>Note:</em> It is the responsibility of the caller to make
     * sure arguments are checked before the methods are called. While
     * some rudimentary checks are performed on the input, the checks
     * are best effort and when performance is an overriding priority,
     * as when methods of this class are optimized by the runtime
     * compiler, some or all checks (if any) may be elided. Hence, the
     * caller must not rely on the checks and corresponding
     * exceptions!
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

    // BEGIN Android-removed: Not used in Android.
    /*
    /**
     * Copies all elements from one block of memory to another block,
     * *unconditionally* byte swapping the elements on the fly.
     *
     * <p>This method determines each block's base address by means of two parameters,
     * and so it provides (in effect) a <em>double-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.  When the object reference is null,
     * the offset supplies an absolute base address.
     *
     * <em>Note:</em> It is the responsibility of the caller to make
     * sure arguments are checked before the methods are called. While
     * some rudimentary checks are performed on the input, the checks
     * are best effort and when performance is an overriding priority,
     * as when methods of this class are optimized by the runtime
     * compiler, some or all checks (if any) may be elided. Hence, the
     * caller must not rely on the checks and corresponding
     * exceptions!
     *
     * @throws RuntimeException if any of the arguments is invalid
     *
     * @since 9
     * /
    public void copySwapMemory(Object srcBase, long srcOffset,
                               Object destBase, long destOffset,
                               long bytes, long elemSize) {
        copySwapMemoryChecks(srcBase, srcOffset, destBase, destOffset, bytes, elemSize);

        if (bytes == 0) {
            return;
        }

        copySwapMemory0(srcBase, srcOffset, destBase, destOffset, bytes, elemSize);
    }

    private void copySwapMemoryChecks(Object srcBase, long srcOffset,
                                      Object destBase, long destOffset,
                                      long bytes, long elemSize) {
        checkSize(bytes);

        if (elemSize != 2 && elemSize != 4 && elemSize != 8) {
            throw invalidInput();
        }
        if (bytes % elemSize != 0) {
            throw invalidInput();
        }

        checkPrimitivePointer(srcBase, srcOffset);
        checkPrimitivePointer(destBase, destOffset);
    }

    /**
     * Copies all elements from one block of memory to another block, byte swapping the
     * elements on the fly.
     *
     * This provides a <em>single-register</em> addressing mode, as
     * discussed in {@link #getInt(Object,long)}.
     *
     * Equivalent to {@code copySwapMemory(null, srcAddress, null, destAddress, bytes, elemSize)}.
     * /
    public void copySwapMemory(long srcAddress, long destAddress, long bytes, long elemSize) {
        copySwapMemory(null, srcAddress, null, destAddress, bytes, elemSize);
    }

     */
    // END Android-removed: Not used in Android.

    /**
     * Frees previously allocated memory at given address.
     *
     * <p>This method determines each block's base address by means of two parameters,
     * and so it provides (in effect) a <em>double-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.  When the object reference is null,
     * the offset supplies an absolute base address.
     *
     * <em>Note:</em> It is the responsibility of the caller to make
     * sure arguments are checked before the methods are called. While
     * some rudimentary checks are performed on the input, the checks
     * are best effort and when performance is an overriding priority,
     * as when methods of this class are optimized by the runtime
     * compiler, some or all checks (if any) may be elided. Hence, the
     * caller must not rely on the checks and corresponding
     * exceptions!
     *
     * @param address address of the freed memory
     *
     * @throws RuntimeException if any of the arguments is invalid
     *
     * @since 9
     */
    // BEGIN Android-changed: Implemented as a native call.
    @FastNative
    public native void freeMemory(long address);
    /*
    public void freeMemory(long address) {
        freeMemoryChecks(address);

        if (address == 0) {
            return;
        }

        freeMemory0(address);
    }
     */
    // END Android-changed: Implemented as a native call.

    // BEGIN Android-removed: Not used in Android.
    /*
    /**
     * Validate the arguments to freeMemory
     *
     * @throws RuntimeException if the arguments are invalid
     *         (<em>Note:</em> after optimization, invalid inputs may
     *         go undetected, which will lead to unpredictable
     *         behavior)
     * /
    private void freeMemoryChecks(long address) {
        checkPointer(null, address);
    }

    /**
     * Ensure writeback of a specified virtual memory address range
     * from cache to physical memory. All bytes in the address range
     * are guaranteed to have been written back to physical memory on
     * return from this call i.e. subsequently executed store
     * instructions are guaranteed not to be visible before the
     * writeback is completed.
     *
     * @param address
     *        the lowest byte address that must be guaranteed written
     *        back to memory. bytes at lower addresses may also be
     *        written back.
     *
     * @param length
     *        the length in bytes of the region starting at address
     *        that must be guaranteed written back to memory.
     *
     * @throws RuntimeException if memory writeback is not supported
     *         on the current hardware of if the arguments are invalid.
     *         (<em>Note:</em> after optimization, invalid inputs may
     *         go undetected, which will lead to unpredictable
     *         behavior)
     *
     * @since 14
     * /

    public void writebackMemory(long address, long length) {
        checkWritebackEnabled();
        checkWritebackMemory(address, length);

        // perform any required pre-writeback barrier
        writebackPreSync0();

        // write back one cache line at a time
        long line = dataCacheLineAlignDown(address);
        long end = address + length;
        while (line < end) {
            writeback0(line);
            line += dataCacheLineFlushSize();
        }

        // perform any required post-writeback barrier
        writebackPostSync0();
    }

    /**
     * Validate the arguments to writebackMemory
     *
     * @throws RuntimeException if the arguments are invalid
     *         (<em>Note:</em> after optimization, invalid inputs may
     *         go undetected, which will lead to unpredictable
     *         behavior)
     * /
    private void checkWritebackMemory(long address, long length) {
        checkNativeAddress(address);
        checkSize(length);
    }

    /**
     * Validate that the current hardware supports memory writeback.
     * (<em>Note:</em> this is a belt and braces check.  Clients are
     * expected to test whether writeback is enabled by calling
     * ({@link isWritebackEnabled #isWritebackEnabled} and avoid
     * calling method {@link writeback #writeback} if it is disabled).
     *
     *
     * @throws RuntimeException if memory writeback is not supported
     * /
    private void checkWritebackEnabled() {
        if (!isWritebackEnabled()) {
            throw new RuntimeException("writebackMemory not enabled!");
        }
    }

    /**
     * force writeback of an individual cache line.
     *
     * @param address
     *        the start address of the cache line to be written back
     * /
    @IntrinsicCandidate
    private native void writeback0(long address);

     /**
      * Serialize writeback operations relative to preceding memory writes.
      * /
    @IntrinsicCandidate
    private native void writebackPreSync0();

     /**
      * Serialize writeback operations relative to following memory writes.
      * /
    @IntrinsicCandidate
    private native void writebackPostSync0();
     */
    // END Android-removed: Not used in Android.

    /// random queries

    /**
     * This constant differs from all results that will ever be returned from
     * {@link #staticFieldOffset}, {@link #objectFieldOffset},
     * or {@link #arrayBaseOffset}.
     */
    public static final int INVALID_FIELD_OFFSET   = -1;

    /**
     * Gets the raw byte offset from the start of an object's memory to
     * the memory used to store the indicated instance field.
     *
     * @param field non-{@code null}; the field in question, which must be an
     * instance field
     * @return the offset to the field
     */
    public long objectFieldOffset(Field f) {
        // BEGIN Android-changed: Implemented differently on Android.
        if (Modifier.isStatic(f.getModifiers())) {
            throw new IllegalArgumentException("valid for instance fields only");
        }
        return f.getOffset();
        /*
        if (f == null) {
            throw new NullPointerException();
        }

        return objectFieldOffset0(f);
         */
        // END Android-changed: Implemented differently on Android.
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

    /**
     * Gets the size of the address value, in bytes.
     *
     * @return the size of the address, in bytes
     */
    @FastNative
    public native int addressSize();

    /** The value of {@code addressSize()} */
    // Android-changed: Use different source for the address size.
    // public static final int ADDRESS_SIZE = ADDRESS_SIZE0;
    public static final int ADDRESS_SIZE = theUnsafe.addressSize();

    /**
     * Gets the size of the memory page, in bytes.
     *
     * @return the size of the page
     */
    // Android-changed: Implemented as native call.
    // public int pageSize() { return PAGE_SIZE; }
    @FastNative
    public native int pageSize();

    // BEGIN Android-removed: Not used in Android.
    /*
    /**
     * Reports the size in bytes of a data cache line written back by
     * the hardware cache line flush operation available to the JVM or
     * 0 if data cache line flushing is not enabled.
     * /
    public int dataCacheLineFlushSize() { return DATA_CACHE_LINE_FLUSH_SIZE; }

    /**
     * Rounds down address to a data cache line boundary as
     * determined by {@link #dataCacheLineFlushSize}
     * @return the rounded down address
     * /
    public long dataCacheLineAlignDown(long address) {
        return (address & ~(DATA_CACHE_LINE_FLUSH_SIZE - 1));
    }

    /**
     * Returns true if data cache line writeback
     * /
    public static boolean isWritebackEnabled() { return DATA_CACHE_LINE_FLUSH_SIZE != 0; }

    /// random trusted operations from JNI:

    /**
     * Tells the VM to define a class, without security checks.  By default, the
     * class loader and protection domain come from the caller's class.
     * /
    public Class<?> defineClass(String name, byte[] b, int off, int len,
                                ClassLoader loader,
                                ProtectionDomain protectionDomain) {
        if (b == null) {
            throw new NullPointerException();
        }
        if (len < 0) {
            throw new ArrayIndexOutOfBoundsException();
        }

        return defineClass0(name, b, off, len, loader, protectionDomain);
    }

    public native Class<?> defineClass0(String name, byte[] b, int off, int len,
                                        ClassLoader loader,
                                        ProtectionDomain protectionDomain);

    /**
     * Allocates an instance but does not run any constructor.
     * Initializes the class if it has not yet been.
     * /
    @IntrinsicCandidate
    public native Object allocateInstance(Class<?> cls)
        throws InstantiationException;

    /**
     * Allocates an array of a given type, but does not do zeroing.
     * <p>
     * This method should only be used in the very rare cases where a high-performance code
     * overwrites the destination array completely, and compilers cannot assist in zeroing elimination.
     * In an overwhelming majority of cases, a normal Java allocation should be used instead.
     * <p>
     * Users of this method are <b>required</b> to overwrite the initial (garbage) array contents
     * before allowing untrusted code, or code in other threads, to observe the reference
     * to the newly allocated array. In addition, the publication of the array reference must be
     * safe according to the Java Memory Model requirements.
     * <p>
     * The safest approach to deal with an uninitialized array is to keep the reference to it in local
     * variable at least until the initialization is complete, and then publish it <b>once</b>, either
     * by writing it to a <em>volatile</em> field, or storing it into a <em>final</em> field in constructor,
     * or issuing a {@link #storeFence} before publishing the reference.
     * <p>
     * @implnote This method can only allocate primitive arrays, to avoid garbage reference
     * elements that could break heap integrity.
     *
     * @param componentType array component type to allocate
     * @param length array size to allocate
     * @throws IllegalArgumentException if component type is null, or not a primitive class;
     *                                  or the length is negative
     * /
    public Object allocateUninitializedArray(Class<?> componentType, int length) {
       if (componentType == null) {
           throw new IllegalArgumentException("Component type is null");
       }
       if (!componentType.isPrimitive()) {
           throw new IllegalArgumentException("Component type is not primitive");
       }
       if (length < 0) {
           throw new IllegalArgumentException("Negative length");
       }
       return allocateUninitializedArray0(componentType, length);
    }
     */
    // END Android-removed: Not used in Android.


    /**
     * Allocates an instance of the given class without running the constructor.
     * The class' <clinit> will be run, if necessary.
     */
    @IntrinsicCandidate
    public native Object allocateInstance(Class<?> cls);
    // Android-changed: No throw specification
    //     throws InstantiationException;

    // BEGIN Android-removed: Not used in Android.
    /*
    /**
     * Allocates an array of a given type, but does not do zeroing.
     * <p>
     * This method should only be used in the very rare cases where a high-performance code
     * overwrites the destination array completely, and compilers cannot assist in zeroing elimination.
     * In an overwhelming majority of cases, a normal Java allocation should be used instead.
     * <p>
     * Users of this method are <b>required</b> to overwrite the initial (garbage) array contents
     * before allowing untrusted code, or code in other threads, to observe the reference
     * to the newly allocated array. In addition, the publication of the array reference must be
     * safe according to the Java Memory Model requirements.
     * <p>
     * The safest approach to deal with an uninitialized array is to keep the reference to it in local
     * variable at least until the initialization is complete, and then publish it <b>once</b>, either
     * by writing it to a <em>volatile</em> field, or storing it into a <em>final</em> field in constructor,
     * or issuing a {@link #storeFence} before publishing the reference.
     * <p>
     * @implnote This method can only allocate primitive arrays, to avoid garbage reference
     * elements that could break heap integrity.
     *
     * @param componentType array component type to allocate
     * @param length array size to allocate
     * @throws IllegalArgumentException if component type is null, or not a primitive class;
     *                                  or the length is negative
     * /
    public Object allocateUninitializedArray(Class<?> componentType, int length) {
       if (componentType == null) {
           throw new IllegalArgumentException("Component type is null");
       }
       if (!componentType.isPrimitive()) {
           throw new IllegalArgumentException("Component type is not primitive");
       }
       if (length < 0) {
           throw new IllegalArgumentException("Negative length");
       }
       return allocateUninitializedArray0(componentType, length);
    }

    @IntrinsicCandidate
    private Object allocateUninitializedArray0(Class<?> componentType, int length) {
       // These fallbacks provide zeroed arrays, but intrinsic is not required to
       // return the zeroed arrays.
       if (componentType == byte.class)    return new byte[length];
       if (componentType == boolean.class) return new boolean[length];
       if (componentType == short.class)   return new short[length];
       if (componentType == char.class)    return new char[length];
       if (componentType == int.class)     return new int[length];
       if (componentType == float.class)   return new float[length];
       if (componentType == long.class)    return new long[length];
       if (componentType == double.class)  return new double[length];
       return null;
    }

    /** Throws the exception without telling the verifier. * /
    public native void throwException(Throwable ee);

     */
    // END Android-removed: Not used in Android.

    /**
     * Atomically updates Java variable to {@code x} if it is currently
     * holding {@code expected}.
     *
     * <p>This operation has memory semantics of a {@code volatile} read
     * and write.  Corresponds to C11 atomic_compare_exchange_strong.
     *
     * @return {@code true} if successful
     */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public final native boolean compareAndSetReference(Object o, long offset,
                                                       Object expected,
                                                       Object x);

    // BEGIN Android-removed: Not used in Android.
    /*
    @IntrinsicCandidate
    public final native Object compareAndExchangeReference(Object o, long offset,
                                                           Object expected,
                                                           Object x);

    @IntrinsicCandidate
    public final Object compareAndExchangeReferenceAcquire(Object o, long offset,
                                                           Object expected,
                                                           Object x) {
        return compareAndExchangeReference(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final Object compareAndExchangeReferenceRelease(Object o, long offset,
                                                           Object expected,
                                                           Object x) {
        return compareAndExchangeReference(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final boolean weakCompareAndSetReferencePlain(Object o, long offset,
                                                         Object expected,
                                                         Object x) {
        return compareAndSetReference(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final boolean weakCompareAndSetReferenceAcquire(Object o, long offset,
                                                           Object expected,
                                                           Object x) {
        return compareAndSetReference(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final boolean weakCompareAndSetReferenceRelease(Object o, long offset,
                                                           Object expected,
                                                           Object x) {
        return compareAndSetReference(o, offset, expected, x);
    }
     */
    // END Android-removed: Not used in Android.

    @IntrinsicCandidate
    public final boolean weakCompareAndSetReference(Object o, long offset,
                                                    Object expected,
                                                    Object x) {
        return compareAndSetReference(o, offset, expected, x);
    }

    /**
     * Atomically updates Java variable to {@code x} if it is currently
     * holding {@code expected}.
     *
     * <p>This operation has memory semantics of a {@code volatile} read
     * and write.  Corresponds to C11 atomic_compare_exchange_strong.
     *
     * @return {@code true} if successful
     */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public final native boolean compareAndSetInt(Object o, long offset,
                                                 int expected,
                                                 int x);

    // BEGIN Android-removed: Not used in Android.
    /*
    @IntrinsicCandidate
    public final native int compareAndExchangeInt(Object o, long offset,
                                                  int expected,
                                                  int x);

    @IntrinsicCandidate
    public final int compareAndExchangeIntAcquire(Object o, long offset,
                                                         int expected,
                                                         int x) {
        return compareAndExchangeInt(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final int compareAndExchangeIntRelease(Object o, long offset,
                                                         int expected,
                                                         int x) {
        return compareAndExchangeInt(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final boolean weakCompareAndSetIntPlain(Object o, long offset,
                                                   int expected,
                                                   int x) {
        return compareAndSetInt(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final boolean weakCompareAndSetIntAcquire(Object o, long offset,
                                                     int expected,
                                                     int x) {
        return compareAndSetInt(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final boolean weakCompareAndSetIntRelease(Object o, long offset,
                                                     int expected,
                                                     int x) {
        return compareAndSetInt(o, offset, expected, x);
    }
     */
    // END Android-removed: Not used in Android.

    @IntrinsicCandidate
    public final boolean weakCompareAndSetInt(Object o, long offset,
                                              int expected,
                                              int x) {
        return compareAndSetInt(o, offset, expected, x);
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    @IntrinsicCandidate
    public final byte compareAndExchangeByte(Object o, long offset,
                                             byte expected,
                                             byte x) {
        long wordOffset = offset & ~3;
        int shift = (int) (offset & 3) << 3;
        if (BIG_ENDIAN) {
            shift = 24 - shift;
        }
        int mask           = 0xFF << shift;
        int maskedExpected = (expected & 0xFF) << shift;
        int maskedX        = (x & 0xFF) << shift;
        int fullWord;
        do {
            fullWord = getIntVolatile(o, wordOffset);
            if ((fullWord & mask) != maskedExpected)
                return (byte) ((fullWord & mask) >> shift);
        } while (!weakCompareAndSetInt(o, wordOffset,
                                                fullWord, (fullWord & ~mask) | maskedX));
        return expected;
    }

    @IntrinsicCandidate
    public final boolean compareAndSetByte(Object o, long offset,
                                           byte expected,
                                           byte x) {
        return compareAndExchangeByte(o, offset, expected, x) == expected;
    }

    @IntrinsicCandidate
    public final boolean weakCompareAndSetByte(Object o, long offset,
                                               byte expected,
                                               byte x) {
        return compareAndSetByte(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final boolean weakCompareAndSetByteAcquire(Object o, long offset,
                                                      byte expected,
                                                      byte x) {
        return weakCompareAndSetByte(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final boolean weakCompareAndSetByteRelease(Object o, long offset,
                                                      byte expected,
                                                      byte x) {
        return weakCompareAndSetByte(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final boolean weakCompareAndSetBytePlain(Object o, long offset,
                                                    byte expected,
                                                    byte x) {
        return weakCompareAndSetByte(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final byte compareAndExchangeByteAcquire(Object o, long offset,
                                                    byte expected,
                                                    byte x) {
        return compareAndExchangeByte(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final byte compareAndExchangeByteRelease(Object o, long offset,
                                                    byte expected,
                                                    byte x) {
        return compareAndExchangeByte(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final short compareAndExchangeShort(Object o, long offset,
                                               short expected,
                                               short x) {
        if ((offset & 3) == 3) {
            throw new IllegalArgumentException("Update spans the word, not supported");
        }
        long wordOffset = offset & ~3;
        int shift = (int) (offset & 3) << 3;
        if (BIG_ENDIAN) {
            shift = 16 - shift;
        }
        int mask           = 0xFFFF << shift;
        int maskedExpected = (expected & 0xFFFF) << shift;
        int maskedX        = (x & 0xFFFF) << shift;
        int fullWord;
        do {
            fullWord = getIntVolatile(o, wordOffset);
            if ((fullWord & mask) != maskedExpected) {
                return (short) ((fullWord & mask) >> shift);
            }
        } while (!weakCompareAndSetInt(o, wordOffset,
                                                fullWord, (fullWord & ~mask) | maskedX));
        return expected;
    }

    @IntrinsicCandidate
    public final boolean compareAndSetShort(Object o, long offset,
                                            short expected,
                                            short x) {
        return compareAndExchangeShort(o, offset, expected, x) == expected;
    }

    @IntrinsicCandidate
    public final boolean weakCompareAndSetShort(Object o, long offset,
                                                short expected,
                                                short x) {
        return compareAndSetShort(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final boolean weakCompareAndSetShortAcquire(Object o, long offset,
                                                       short expected,
                                                       short x) {
        return weakCompareAndSetShort(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final boolean weakCompareAndSetShortRelease(Object o, long offset,
                                                       short expected,
                                                       short x) {
        return weakCompareAndSetShort(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final boolean weakCompareAndSetShortPlain(Object o, long offset,
                                                     short expected,
                                                     short x) {
        return weakCompareAndSetShort(o, offset, expected, x);
    }


    @IntrinsicCandidate
    public final short compareAndExchangeShortAcquire(Object o, long offset,
                                                     short expected,
                                                     short x) {
        return compareAndExchangeShort(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final short compareAndExchangeShortRelease(Object o, long offset,
                                                    short expected,
                                                    short x) {
        return compareAndExchangeShort(o, offset, expected, x);
    }

    @ForceInline
    private char s2c(short s) {
        return (char) s;
    }

    @ForceInline
    private short c2s(char s) {
        return (short) s;
    }

    @ForceInline
    public final boolean compareAndSetChar(Object o, long offset,
                                           char expected,
                                           char x) {
        return compareAndSetShort(o, offset, c2s(expected), c2s(x));
    }

    @ForceInline
    public final char compareAndExchangeChar(Object o, long offset,
                                             char expected,
                                             char x) {
        return s2c(compareAndExchangeShort(o, offset, c2s(expected), c2s(x)));
    }

    @ForceInline
    public final char compareAndExchangeCharAcquire(Object o, long offset,
                                            char expected,
                                            char x) {
        return s2c(compareAndExchangeShortAcquire(o, offset, c2s(expected), c2s(x)));
    }

    @ForceInline
    public final char compareAndExchangeCharRelease(Object o, long offset,
                                            char expected,
                                            char x) {
        return s2c(compareAndExchangeShortRelease(o, offset, c2s(expected), c2s(x)));
    }

    @ForceInline
    public final boolean weakCompareAndSetChar(Object o, long offset,
                                               char expected,
                                               char x) {
        return weakCompareAndSetShort(o, offset, c2s(expected), c2s(x));
    }

    @ForceInline
    public final boolean weakCompareAndSetCharAcquire(Object o, long offset,
                                                      char expected,
                                                      char x) {
        return weakCompareAndSetShortAcquire(o, offset, c2s(expected), c2s(x));
    }

    @ForceInline
    public final boolean weakCompareAndSetCharRelease(Object o, long offset,
                                                      char expected,
                                                      char x) {
        return weakCompareAndSetShortRelease(o, offset, c2s(expected), c2s(x));
    }

    @ForceInline
    public final boolean weakCompareAndSetCharPlain(Object o, long offset,
                                                    char expected,
                                                    char x) {
        return weakCompareAndSetShortPlain(o, offset, c2s(expected), c2s(x));
    }

    /**
     * The JVM converts integral values to boolean values using two
     * different conventions, byte testing against zero and truncation
     * to least-significant bit.
     *
     * <p>The JNI documents specify that, at least for returning
     * values from native methods, a Java boolean value is converted
     * to the value-set 0..1 by first truncating to a byte (0..255 or
     * maybe -128..127) and then testing against zero. Thus, Java
     * booleans in non-Java data structures are by convention
     * represented as 8-bit containers containing either zero (for
     * false) or any non-zero value (for true).
     *
     * <p>Java booleans in the heap are also stored in bytes, but are
     * strongly normalized to the value-set 0..1 (i.e., they are
     * truncated to the least-significant bit).
     *
     * <p>The main reason for having different conventions for
     * conversion is performance: Truncation to the least-significant
     * bit can be usually implemented with fewer (machine)
     * instructions than byte testing against zero.
     *
     * <p>A number of Unsafe methods load boolean values from the heap
     * as bytes. Unsafe converts those values according to the JNI
     * rules (i.e, using the "testing against zero" convention). The
     * method {@code byte2bool} implements that conversion.
     *
     * @param b the byte to be converted to boolean
     * @return the result of the conversion
     * /
    @ForceInline
    private boolean byte2bool(byte b) {
        return b != 0;
    }

    /**
     * Convert a boolean value to a byte. The return value is strongly
     * normalized to the value-set 0..1 (i.e., the value is truncated
     * to the least-significant bit). See {@link #byte2bool(byte)} for
     * more details on conversion conventions.
     *
     * @param b the boolean to be converted to byte (and then normalized)
     * @return the result of the conversion
     * /
    @ForceInline
    private byte bool2byte(boolean b) {
        return b ? (byte)1 : (byte)0;
    }

    @ForceInline
    public final boolean compareAndSetBoolean(Object o, long offset,
                                              boolean expected,
                                              boolean x) {
        return compareAndSetByte(o, offset, bool2byte(expected), bool2byte(x));
    }

    @ForceInline
    public final boolean compareAndExchangeBoolean(Object o, long offset,
                                                   boolean expected,
                                                   boolean x) {
        return byte2bool(compareAndExchangeByte(o, offset, bool2byte(expected), bool2byte(x)));
    }

    @ForceInline
    public final boolean compareAndExchangeBooleanAcquire(Object o, long offset,
                                                    boolean expected,
                                                    boolean x) {
        return byte2bool(compareAndExchangeByteAcquire(o, offset, bool2byte(expected), bool2byte(x)));
    }

    @ForceInline
    public final boolean compareAndExchangeBooleanRelease(Object o, long offset,
                                                       boolean expected,
                                                       boolean x) {
        return byte2bool(compareAndExchangeByteRelease(o, offset, bool2byte(expected), bool2byte(x)));
    }

    @ForceInline
    public final boolean weakCompareAndSetBoolean(Object o, long offset,
                                                  boolean expected,
                                                  boolean x) {
        return weakCompareAndSetByte(o, offset, bool2byte(expected), bool2byte(x));
    }

    @ForceInline
    public final boolean weakCompareAndSetBooleanAcquire(Object o, long offset,
                                                         boolean expected,
                                                         boolean x) {
        return weakCompareAndSetByteAcquire(o, offset, bool2byte(expected), bool2byte(x));
    }

    @ForceInline
    public final boolean weakCompareAndSetBooleanRelease(Object o, long offset,
                                                         boolean expected,
                                                         boolean x) {
        return weakCompareAndSetByteRelease(o, offset, bool2byte(expected), bool2byte(x));
    }

    @ForceInline
    public final boolean weakCompareAndSetBooleanPlain(Object o, long offset,
                                                       boolean expected,
                                                       boolean x) {
        return weakCompareAndSetBytePlain(o, offset, bool2byte(expected), bool2byte(x));
    }

    @ForceInline
    public final boolean compareAndSetFloat(Object o, long offset,
                                            float expected,
                                            float x) {
        return compareAndSetInt(o, offset,
                                 Float.floatToRawIntBits(expected),
                                 Float.floatToRawIntBits(x));
    }

    @ForceInline
    public final float compareAndExchangeFloat(Object o, long offset,
                                               float expected,
                                               float x) {
        int w = compareAndExchangeInt(o, offset,
                                      Float.floatToRawIntBits(expected),
                                      Float.floatToRawIntBits(x));
        return Float.intBitsToFloat(w);
    }

    @ForceInline
    public final float compareAndExchangeFloatAcquire(Object o, long offset,
                                                  float expected,
                                                  float x) {
        int w = compareAndExchangeIntAcquire(o, offset,
                                             Float.floatToRawIntBits(expected),
                                             Float.floatToRawIntBits(x));
        return Float.intBitsToFloat(w);
    }

    @ForceInline
    public final float compareAndExchangeFloatRelease(Object o, long offset,
                                                  float expected,
                                                  float x) {
        int w = compareAndExchangeIntRelease(o, offset,
                                             Float.floatToRawIntBits(expected),
                                             Float.floatToRawIntBits(x));
        return Float.intBitsToFloat(w);
    }

    @ForceInline
    public final boolean weakCompareAndSetFloatPlain(Object o, long offset,
                                                     float expected,
                                                     float x) {
        return weakCompareAndSetIntPlain(o, offset,
                                     Float.floatToRawIntBits(expected),
                                     Float.floatToRawIntBits(x));
    }

    @ForceInline
    public final boolean weakCompareAndSetFloatAcquire(Object o, long offset,
                                                       float expected,
                                                       float x) {
        return weakCompareAndSetIntAcquire(o, offset,
                                            Float.floatToRawIntBits(expected),
                                            Float.floatToRawIntBits(x));
    }

    @ForceInline
    public final boolean weakCompareAndSetFloatRelease(Object o, long offset,
                                                       float expected,
                                                       float x) {
        return weakCompareAndSetIntRelease(o, offset,
                                            Float.floatToRawIntBits(expected),
                                            Float.floatToRawIntBits(x));
    }

    @ForceInline
    public final boolean weakCompareAndSetFloat(Object o, long offset,
                                                float expected,
                                                float x) {
        return weakCompareAndSetInt(o, offset,
                                             Float.floatToRawIntBits(expected),
                                             Float.floatToRawIntBits(x));
    }

    /**
     * Atomically updates Java variable to {@code x} if it is currently
     * holding {@code expected}.
     *
     * <p>This operation has memory semantics of a {@code volatile} read
     * and write.  Corresponds to C11 atomic_compare_exchange_strong.
     *
     * @return {@code true} if successful
     * /
    @ForceInline
    public final boolean compareAndSetDouble(Object o, long offset,
                                             double expected,
                                             double x) {
        return compareAndSetLong(o, offset,
                                 Double.doubleToRawLongBits(expected),
                                 Double.doubleToRawLongBits(x));
    }

    @ForceInline
    public final double compareAndExchangeDouble(Object o, long offset,
                                                 double expected,
                                                 double x) {
        long w = compareAndExchangeLong(o, offset,
                                        Double.doubleToRawLongBits(expected),
                                        Double.doubleToRawLongBits(x));
        return Double.longBitsToDouble(w);
    }

    @ForceInline
    public final double compareAndExchangeDoubleAcquire(Object o, long offset,
                                                        double expected,
                                                        double x) {
        long w = compareAndExchangeLongAcquire(o, offset,
                                               Double.doubleToRawLongBits(expected),
                                               Double.doubleToRawLongBits(x));
        return Double.longBitsToDouble(w);
    }

    @ForceInline
    public final double compareAndExchangeDoubleRelease(Object o, long offset,
                                                        double expected,
                                                        double x) {
        long w = compareAndExchangeLongRelease(o, offset,
                                               Double.doubleToRawLongBits(expected),
                                               Double.doubleToRawLongBits(x));
        return Double.longBitsToDouble(w);
    }

    @ForceInline
    public final boolean weakCompareAndSetDoublePlain(Object o, long offset,
                                                      double expected,
                                                      double x) {
        return weakCompareAndSetLongPlain(o, offset,
                                     Double.doubleToRawLongBits(expected),
                                     Double.doubleToRawLongBits(x));
    }

    @ForceInline
    public final boolean weakCompareAndSetDoubleAcquire(Object o, long offset,
                                                        double expected,
                                                        double x) {
        return weakCompareAndSetLongAcquire(o, offset,
                                             Double.doubleToRawLongBits(expected),
                                             Double.doubleToRawLongBits(x));
    }

    @ForceInline
    public final boolean weakCompareAndSetDoubleRelease(Object o, long offset,
                                                        double expected,
                                                        double x) {
        return weakCompareAndSetLongRelease(o, offset,
                                             Double.doubleToRawLongBits(expected),
                                             Double.doubleToRawLongBits(x));
    }

    @ForceInline
    public final boolean weakCompareAndSetDouble(Object o, long offset,
                                                 double expected,
                                                 double x) {
        return weakCompareAndSetLong(o, offset,
                                              Double.doubleToRawLongBits(expected),
                                              Double.doubleToRawLongBits(x));
    }
     */
    // END Android-removed: Not used in Android.

    /**
     * Atomically updates Java variable to {@code x} if it is currently
     * holding {@code expected}.
     *
     * <p>This operation has memory semantics of a {@code volatile} read
     * and write.  Corresponds to C11 atomic_compare_exchange_strong.
     *
     * @return {@code true} if successful
     */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public final native boolean compareAndSetLong(Object o, long offset,
                                                  long expected,
                                                  long x);

    // BEGIN Android-removed: Not used in Android.
    /*
    @IntrinsicCandidate
    public final native long compareAndExchangeLong(Object o, long offset,
                                                    long expected,
                                                    long x);

    @IntrinsicCandidate
    public final long compareAndExchangeLongAcquire(Object o, long offset,
                                                           long expected,
                                                           long x) {
        return compareAndExchangeLong(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final long compareAndExchangeLongRelease(Object o, long offset,
                                                           long expected,
                                                           long x) {
        return compareAndExchangeLong(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final boolean weakCompareAndSetLongPlain(Object o, long offset,
                                                    long expected,
                                                    long x) {
        return compareAndSetLong(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final boolean weakCompareAndSetLongAcquire(Object o, long offset,
                                                      long expected,
                                                      long x) {
        return compareAndSetLong(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final boolean weakCompareAndSetLongRelease(Object o, long offset,
                                                      long expected,
                                                      long x) {
        return compareAndSetLong(o, offset, expected, x);
    }

    @IntrinsicCandidate
    public final boolean weakCompareAndSetLong(Object o, long offset,
                                               long expected,
                                               long x) {
        return compareAndSetLong(o, offset, expected, x);
    }
     */
    // END Android-removed: Not used in Android.

    /**
     * Fetches a reference value from a given Java variable, with volatile
     * load semantics. Otherwise identical to {@link #getReference(Object, long)}
     */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native Object getReferenceVolatile(Object o, long offset);

    /**
     * Stores a reference value into a given Java variable, with
     * volatile store semantics. Otherwise identical to {@link #putReference(Object, long, Object)}
     */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native void putReferenceVolatile(Object o, long offset, Object x);

    /**
     * Gets an {@code int} field from the given object,
     * using {@code volatile} semantics.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @return the retrieved value
     */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native int getIntVolatile(Object obj, long offset);

    /**
     * Stores an {@code int} field into the given object,
     * using {@code volatile} semantics.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native void putIntVolatile(Object obj, long offset, int newValue);

    // BEGIN Android-removed: Not used in Android.
    /*
    /** Volatile version of {@link #getBoolean(Object, long)}  * /
    @IntrinsicCandidate
    public native boolean getBooleanVolatile(Object o, long offset);

    /** Volatile version of {@link #putBoolean(Object, long, boolean)}  * /
    @IntrinsicCandidate
    public native void    putBooleanVolatile(Object o, long offset, boolean x);

    /** Volatile version of {@link #getByte(Object, long)}  * /
    @IntrinsicCandidate
    public native byte    getByteVolatile(Object o, long offset);

    /** Volatile version of {@link #putByte(Object, long, byte)}  * /
    @IntrinsicCandidate
    public native void    putByteVolatile(Object o, long offset, byte x);

    /** Volatile version of {@link #getShort(Object, long)}  * /
    @IntrinsicCandidate
    public native short   getShortVolatile(Object o, long offset);

    /** Volatile version of {@link #putShort(Object, long, short)}  * /
    @IntrinsicCandidate
    public native void    putShortVolatile(Object o, long offset, short x);

    /** Volatile version of {@link #getChar(Object, long)}  * /
    @IntrinsicCandidate
    public native char    getCharVolatile(Object o, long offset);

    /** Volatile version of {@link #putChar(Object, long, char)}  * /
    @IntrinsicCandidate
    public native void    putCharVolatile(Object o, long offset, char x);
     */
    // END Android-removed: Not used in Android.

    /**
     * Gets a {@code long} field from the given object,
     * using {@code volatile} semantics.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @return the retrieved value
     */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native long getLongVolatile(Object obj, long offset);

    /**
     * Stores a {@code long} field into the given object,
     * using {@code volatile} semantics.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native void putLongVolatile(Object obj, long offset, long newValue);

    // BEGIN Android-removed: Not used in Android.
    /*
    /** Volatile version of {@link #getFloat(Object, long)}  * /
    @IntrinsicCandidate
    public native float   getFloatVolatile(Object o, long offset);

    /** Volatile version of {@link #putFloat(Object, long, float)}  * /
    @IntrinsicCandidate
    public native void    putFloatVolatile(Object o, long offset, float x);

    /** Volatile version of {@link #getDouble(Object, long)}  * /
    @IntrinsicCandidate
    public native double  getDoubleVolatile(Object o, long offset);

    /** Volatile version of {@link #putDouble(Object, long, double)}  * /
    @IntrinsicCandidate
    public native void    putDoubleVolatile(Object o, long offset, double x);
     */
    // END Android-removed: Not used in Android.


    /** Acquire version of {@link #getReferenceVolatile(Object, long)} */
    @IntrinsicCandidate
    public final Object getReferenceAcquire(Object o, long offset) {
        return getReferenceVolatile(o, offset);
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    /** Acquire version of {@link #getBooleanVolatile(Object, long)} * /
    @IntrinsicCandidate
    public final boolean getBooleanAcquire(Object o, long offset) {
        return getBooleanVolatile(o, offset);
    }

    /** Acquire version of {@link #getByteVolatile(Object, long)} * /
    @IntrinsicCandidate
    public final byte getByteAcquire(Object o, long offset) {
        return getByteVolatile(o, offset);
    }

    /** Acquire version of {@link #getShortVolatile(Object, long)} * /
    @IntrinsicCandidate
    public final short getShortAcquire(Object o, long offset) {
        return getShortVolatile(o, offset);
    }

    /** Acquire version of {@link #getCharVolatile(Object, long)} * /
    @IntrinsicCandidate
    public final char getCharAcquire(Object o, long offset) {
        return getCharVolatile(o, offset);
    }
     */
    // END Android-removed: Not used in Android.

    /** Acquire version of {@link #getIntVolatile(Object, long)} */
    @IntrinsicCandidate
    public final int getIntAcquire(Object o, long offset) {
        return getIntVolatile(o, offset);
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    /** Acquire version of {@link #getFloatVolatile(Object, long)} * /
    @IntrinsicCandidate
    public final float getFloatAcquire(Object o, long offset) {
        return getFloatVolatile(o, offset);
    }
     */
    // END Android-removed: Not used in Android.

    /** Acquire version of {@link #getLongVolatile(Object, long)} */
    @IntrinsicCandidate
    public final long getLongAcquire(Object o, long offset) {
        return getLongVolatile(o, offset);
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    /** Acquire version of {@link #getDoubleVolatile(Object, long)} * /
    @IntrinsicCandidate
    public final double getDoubleAcquire(Object o, long offset) {
        return getDoubleVolatile(o, offset);
    }

    /*
     * Versions of {@link #putReferenceVolatile(Object, long, Object)}
     * that do not guarantee immediate visibility of the store to
     * other threads. This method is generally only useful if the
     * underlying field is a Java volatile (or if an array cell, one
     * that is otherwise only accessed using volatile accesses).
     *
     * Corresponds to C11 atomic_store_explicit(..., memory_order_release).
     * /
     */
    // END Android-removed: Not used in Android.

    /** Release version of {@link #putReferenceVolatile(Object, long, Object)} */
    @IntrinsicCandidate
    public final void putReferenceRelease(Object o, long offset, Object x) {
        putReferenceVolatile(o, offset, x);
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    /** Release version of {@link #putBooleanVolatile(Object, long, boolean)} * /
    @IntrinsicCandidate
    public final void putBooleanRelease(Object o, long offset, boolean x) {
        putBooleanVolatile(o, offset, x);
    }

    /** Release version of {@link #putByteVolatile(Object, long, byte)} * /
    @IntrinsicCandidate
    public final void putByteRelease(Object o, long offset, byte x) {
        putByteVolatile(o, offset, x);
    }

    /** Release version of {@link #putShortVolatile(Object, long, short)} * /
    @IntrinsicCandidate
    public final void putShortRelease(Object o, long offset, short x) {
        putShortVolatile(o, offset, x);
    }

    /** Release version of {@link #putCharVolatile(Object, long, char)} * /
    @IntrinsicCandidate
    public final void putCharRelease(Object o, long offset, char x) {
        putCharVolatile(o, offset, x);
    }
     */
    // END Android-removed: Not used in Android.

    /** Release version of {@link #putIntVolatile(Object, long, int)} */
    @IntrinsicCandidate
    public final void putIntRelease(Object o, long offset, int x) {
        putIntVolatile(o, offset, x);
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    /** Release version of {@link #putFloatVolatile(Object, long, float)} * /
    @IntrinsicCandidate
    public final void putFloatRelease(Object o, long offset, float x) {
        putFloatVolatile(o, offset, x);
    }
      */
    // END Android-removed: Not used in Android.

    /** Release version of {@link #putLongVolatile(Object, long, long)} */
    @IntrinsicCandidate
    public final void putLongRelease(Object o, long offset, long x) {
        putLongVolatile(o, offset, x);
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    /** Release version of {@link #putDoubleVolatile(Object, long, double)} * /
    @IntrinsicCandidate
    public final void putDoubleRelease(Object o, long offset, double x) {
        putDoubleVolatile(o, offset, x);
    }
     */
    // END Android-removed: Not used in Android.

    // ------------------------------ Opaque --------------------------------------

    /** Opaque version of {@link #getReferenceVolatile(Object, long)} */
    @IntrinsicCandidate
    public final Object getReferenceOpaque(Object o, long offset) {
        return getReferenceVolatile(o, offset);
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    /** Opaque version of {@link #getBooleanVolatile(Object, long)} * /
    @IntrinsicCandidate
    public final boolean getBooleanOpaque(Object o, long offset) {
        return getBooleanVolatile(o, offset);
    }

    /** Opaque version of {@link #getByteVolatile(Object, long)} * /
    @IntrinsicCandidate
    public final byte getByteOpaque(Object o, long offset) {
        return getByteVolatile(o, offset);
    }

    /** Opaque version of {@link #getShortVolatile(Object, long)} * /
    @IntrinsicCandidate
    public final short getShortOpaque(Object o, long offset) {
        return getShortVolatile(o, offset);
    }

    /** Opaque version of {@link #getCharVolatile(Object, long)} * /
    @IntrinsicCandidate
    public final char getCharOpaque(Object o, long offset) {
        return getCharVolatile(o, offset);
    }
     */
    // END Android-removed: Not used in Android.

    /** Opaque version of {@link #getIntVolatile(Object, long)} */
    @IntrinsicCandidate
    public final int getIntOpaque(Object o, long offset) {
        return getIntVolatile(o, offset);
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    /** Opaque version of {@link #getFloatVolatile(Object, long)} * /
    @IntrinsicCandidate
    public final float getFloatOpaque(Object o, long offset) {
        return getFloatVolatile(o, offset);
    }
     */
    // END Android-removed: Not used in Android.

    /** Opaque version of {@link #getLongVolatile(Object, long)} */
    @IntrinsicCandidate
    public final long getLongOpaque(Object o, long offset) {
        return getLongVolatile(o, offset);
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    /** Opaque version of {@link #getDoubleVolatile(Object, long)} * /
    @IntrinsicCandidate
    public final double getDoubleOpaque(Object o, long offset) {
        return getDoubleVolatile(o, offset);
    }
     */
    // END Android-removed: Not used in Android.

    /** Opaque version of {@link #putReferenceVolatile(Object, long, Object)} */
    @IntrinsicCandidate
    public final void putReferenceOpaque(Object o, long offset, Object x) {
        putReferenceVolatile(o, offset, x);
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    /** Opaque version of {@link #putBooleanVolatile(Object, long, boolean)} * /
    @IntrinsicCandidate
    public final void putBooleanOpaque(Object o, long offset, boolean x) {
        putBooleanVolatile(o, offset, x);
    }

    /** Opaque version of {@link #putByteVolatile(Object, long, byte)} * /
    @IntrinsicCandidate
    public final void putByteOpaque(Object o, long offset, byte x) {
        putByteVolatile(o, offset, x);
    }

    /** Opaque version of {@link #putShortVolatile(Object, long, short)} * /
    @IntrinsicCandidate
    public final void putShortOpaque(Object o, long offset, short x) {
        putShortVolatile(o, offset, x);
    }

    /** Opaque version of {@link #putCharVolatile(Object, long, char)} * /
    @IntrinsicCandidate
    public final void putCharOpaque(Object o, long offset, char x) {
        putCharVolatile(o, offset, x);
    }
     */
    // END Android-removed: Not used in Android.

    /** Opaque version of {@link #putIntVolatile(Object, long, int)} */
    @IntrinsicCandidate
    public final void putIntOpaque(Object o, long offset, int x) {
        putIntVolatile(o, offset, x);
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    /** Opaque version of {@link #putFloatVolatile(Object, long, float)} * /
    @IntrinsicCandidate
    public final void putFloatOpaque(Object o, long offset, float x) {
        putFloatVolatile(o, offset, x);
    }
     */
    // END Android-removed: Not used in Android.

    /** Opaque version of {@link #putLongVolatile(Object, long, long)} */
    @IntrinsicCandidate
    public final void putLongOpaque(Object o, long offset, long x) {
        putLongVolatile(o, offset, x);
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    /** Opaque version of {@link #putDoubleVolatile(Object, long, double)} * /
    @IntrinsicCandidate
    public final void putDoubleOpaque(Object o, long offset, double x) {
        putDoubleVolatile(o, offset, x);
    }
     */
    // END Android-removed: Not used in Android.

    /**
     * Unparks the given object, which must be a {@link Thread}.
     *
     * <p>See {@link java.util.concurrent.locks.LockSupport} for more
     * in-depth information of the behavior of this method.</p>
     *
     * @param obj non-{@code null}; the object to unpark
     */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native void unpark(Object thread);

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
    @IntrinsicCandidate
    public native void park(boolean isAbsolute, long time);

    /*
    // BEGIN Android-removed: Not used in Android.
    /**
     * Gets the load average in the system run queue assigned
     * to the available processors averaged over various periods of time.
     * This method retrieves the given {@code nelem} samples and
     * assigns to the elements of the given {@code loadavg} array.
     * The system imposes a maximum of 3 samples, representing
     * averages over the last 1,  5,  and  15 minutes, respectively.
     *
     * @param loadavg an array of double of size nelems
     * @param nelems the number of samples to be retrieved and
     *        must be 1 to 3.
     *
     * @return the number of samples actually retrieved; or -1
     *         if the load average is unobtainable.
     * /
    public int getLoadAverage(double[] loadavg, int nelems) {
        if (nelems < 0 || nelems > 3 || nelems > loadavg.length) {
            throw new ArrayIndexOutOfBoundsException();
        }

        return getLoadAverage0(loadavg, nelems);
    }
     */
    // END Android-removed: Not used in Android.

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
    @IntrinsicCandidate
    public final int getAndAddInt(Object o, long offset, int delta) {
        int v;
        do {
            v = getIntVolatile(o, offset);
        } while (!weakCompareAndSetInt(o, offset, v, v + delta));
        return v;
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    @ForceInline
    public final int getAndAddIntRelease(Object o, long offset, int delta) {
        int v;
        do {
            v = getInt(o, offset);
        } while (!weakCompareAndSetIntRelease(o, offset, v, v + delta));
        return v;
    }

    @ForceInline
    public final int getAndAddIntAcquire(Object o, long offset, int delta) {
        int v;
        do {
            v = getIntAcquire(o, offset);
        } while (!weakCompareAndSetIntAcquire(o, offset, v, v + delta));
        return v;
    }
     */
    // END Android-removed: Not used in Android.

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
    @IntrinsicCandidate
    public final long getAndAddLong(Object o, long offset, long delta) {
        long v;
        do {
            v = getLongVolatile(o, offset);
        // Android-changed: weakCompareAndSetLong not available.
        // } while (!weakCompareAndSetLong(o, offset, v, v + delta));
        } while (!compareAndSwapLong(o, offset, v, v + delta));
        return v;
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    @ForceInline
    public final long getAndAddLongRelease(Object o, long offset, long delta) {
        long v;
        do {
            v = getLong(o, offset);
        } while (!weakCompareAndSetLongRelease(o, offset, v, v + delta));
        return v;
    }

    @ForceInline
    public final long getAndAddLongAcquire(Object o, long offset, long delta) {
        long v;
        do {
            v = getLongAcquire(o, offset);
        } while (!weakCompareAndSetLongAcquire(o, offset, v, v + delta));
        return v;
    }

    @IntrinsicCandidate
    public final byte getAndAddByte(Object o, long offset, byte delta) {
        byte v;
        do {
            v = getByteVolatile(o, offset);
        } while (!weakCompareAndSetByte(o, offset, v, (byte) (v + delta)));
        return v;
    }

    @ForceInline
    public final byte getAndAddByteRelease(Object o, long offset, byte delta) {
        byte v;
        do {
            v = getByte(o, offset);
        } while (!weakCompareAndSetByteRelease(o, offset, v, (byte) (v + delta)));
        return v;
    }

    @ForceInline
    public final byte getAndAddByteAcquire(Object o, long offset, byte delta) {
        byte v;
        do {
            v = getByteAcquire(o, offset);
        } while (!weakCompareAndSetByteAcquire(o, offset, v, (byte) (v + delta)));
        return v;
    }

    @IntrinsicCandidate
    public final short getAndAddShort(Object o, long offset, short delta) {
        short v;
        do {
            v = getShortVolatile(o, offset);
        } while (!weakCompareAndSetShort(o, offset, v, (short) (v + delta)));
        return v;
    }

    @ForceInline
    public final short getAndAddShortRelease(Object o, long offset, short delta) {
        short v;
        do {
            v = getShort(o, offset);
        } while (!weakCompareAndSetShortRelease(o, offset, v, (short) (v + delta)));
        return v;
    }

    @ForceInline
    public final short getAndAddShortAcquire(Object o, long offset, short delta) {
        short v;
        do {
            v = getShortAcquire(o, offset);
        } while (!weakCompareAndSetShortAcquire(o, offset, v, (short) (v + delta)));
        return v;
    }

    @ForceInline
    public final char getAndAddChar(Object o, long offset, char delta) {
        return (char) getAndAddShort(o, offset, (short) delta);
    }

    @ForceInline
    public final char getAndAddCharRelease(Object o, long offset, char delta) {
        return (char) getAndAddShortRelease(o, offset, (short) delta);
    }

    @ForceInline
    public final char getAndAddCharAcquire(Object o, long offset, char delta) {
        return (char) getAndAddShortAcquire(o, offset, (short) delta);
    }

    @ForceInline
    public final float getAndAddFloat(Object o, long offset, float delta) {
        int expectedBits;
        float v;
        do {
            // Load and CAS with the raw bits to avoid issues with NaNs and
            // possible bit conversion from signaling NaNs to quiet NaNs that
            // may result in the loop not terminating.
            expectedBits = getIntVolatile(o, offset);
            v = Float.intBitsToFloat(expectedBits);
        } while (!weakCompareAndSetInt(o, offset,
                                                expectedBits, Float.floatToRawIntBits(v + delta)));
        return v;
    }

    @ForceInline
    public final float getAndAddFloatRelease(Object o, long offset, float delta) {
        int expectedBits;
        float v;
        do {
            // Load and CAS with the raw bits to avoid issues with NaNs and
            // possible bit conversion from signaling NaNs to quiet NaNs that
            // may result in the loop not terminating.
            expectedBits = getInt(o, offset);
            v = Float.intBitsToFloat(expectedBits);
        } while (!weakCompareAndSetIntRelease(o, offset,
                                               expectedBits, Float.floatToRawIntBits(v + delta)));
        return v;
    }

    @ForceInline
    public final float getAndAddFloatAcquire(Object o, long offset, float delta) {
        int expectedBits;
        float v;
        do {
            // Load and CAS with the raw bits to avoid issues with NaNs and
            // possible bit conversion from signaling NaNs to quiet NaNs that
            // may result in the loop not terminating.
            expectedBits = getIntAcquire(o, offset);
            v = Float.intBitsToFloat(expectedBits);
        } while (!weakCompareAndSetIntAcquire(o, offset,
                                               expectedBits, Float.floatToRawIntBits(v + delta)));
        return v;
    }

    @ForceInline
    public final double getAndAddDouble(Object o, long offset, double delta) {
        long expectedBits;
        double v;
        do {
            // Load and CAS with the raw bits to avoid issues with NaNs and
            // possible bit conversion from signaling NaNs to quiet NaNs that
            // may result in the loop not terminating.
            expectedBits = getLongVolatile(o, offset);
            v = Double.longBitsToDouble(expectedBits);
        } while (!weakCompareAndSetLong(o, offset,
                                                 expectedBits, Double.doubleToRawLongBits(v + delta)));
        return v;
    }

    @ForceInline
    public final double getAndAddDoubleRelease(Object o, long offset, double delta) {
        long expectedBits;
        double v;
        do {
            // Load and CAS with the raw bits to avoid issues with NaNs and
            // possible bit conversion from signaling NaNs to quiet NaNs that
            // may result in the loop not terminating.
            expectedBits = getLong(o, offset);
            v = Double.longBitsToDouble(expectedBits);
        } while (!weakCompareAndSetLongRelease(o, offset,
                                                expectedBits, Double.doubleToRawLongBits(v + delta)));
        return v;
    }

    @ForceInline
    public final double getAndAddDoubleAcquire(Object o, long offset, double delta) {
        long expectedBits;
        double v;
        do {
            // Load and CAS with the raw bits to avoid issues with NaNs and
            // possible bit conversion from signaling NaNs to quiet NaNs that
            // may result in the loop not terminating.
            expectedBits = getLongAcquire(o, offset);
            v = Double.longBitsToDouble(expectedBits);
        } while (!weakCompareAndSetLongAcquire(o, offset,
                                                expectedBits, Double.doubleToRawLongBits(v + delta)));
        return v;
    }
     */
    // END Android-removed: Not used in Android.

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
    @IntrinsicCandidate
    public final int getAndSetInt(Object o, long offset, int newValue) {
        int v;
        do {
            v = getIntVolatile(o, offset);
        } while (!weakCompareAndSetInt(o, offset, v, newValue));
        return v;
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    @ForceInline
    public final int getAndSetIntRelease(Object o, long offset, int newValue) {
        int v;
        do {
            v = getInt(o, offset);
        } while (!weakCompareAndSetIntRelease(o, offset, v, newValue));
        return v;
    }

    @ForceInline
    public final int getAndSetIntAcquire(Object o, long offset, int newValue) {
        int v;
        do {
            v = getIntAcquire(o, offset);
        } while (!weakCompareAndSetIntAcquire(o, offset, v, newValue));
        return v;
    }
     */
    // END Android-removed: Not used in Android.

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
    @IntrinsicCandidate
    public final long getAndSetLong(Object o, long offset, long newValue) {
        long v;
        do {
            v = getLongVolatile(o, offset);
        // Android-changed: weakCompareAndSetLongRelease not available.
        // } while (!weakCompareAndSetLongRelease(o, offset, v, newValue));
        } while (!compareAndSwapLong(o, offset, v, newValue));
        return v;
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    @ForceInline
    public final long getAndSetLongRelease(Object o, long offset, long newValue) {
        long v;
        do {
            v = getLong(o, offset);
        } while (!weakCompareAndSetLongRelease(o, offset, v, newValue));
        return v;
    }

    @ForceInline
    public final long getAndSetLongAcquire(Object o, long offset, long newValue) {
        long v;
        do {
            v = getLongAcquire(o, offset);
        } while (!weakCompareAndSetLongAcquire(o, offset, v, newValue));
        return v;
    }
    */
    // END Android-removed: Not used in Android.

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
    @IntrinsicCandidate
    public final Object getAndSetReference(Object o, long offset, Object newValue) {
        Object v;
        do {
            v = getReferenceVolatile(o, offset);
        } while (!weakCompareAndSetReference(o, offset, v, newValue));
        return v;
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    @ForceInline
    public final Object getAndSetReferenceRelease(Object o, long offset, Object newValue) {
        Object v;
        do {
            v = getReference(o, offset);
        } while (!weakCompareAndSetReferenceRelease(o, offset, v, newValue));
        return v;
    }

    @ForceInline
    public final Object getAndSetReferenceAcquire(Object o, long offset, Object newValue) {
        Object v;
        do {
            v = getReferenceAcquire(o, offset);
        } while (!weakCompareAndSetReferenceAcquire(o, offset, v, newValue));
        return v;
    }

    @IntrinsicCandidate
    public final byte getAndSetByte(Object o, long offset, byte newValue) {
        byte v;
        do {
            v = getByteVolatile(o, offset);
        } while (!weakCompareAndSetByte(o, offset, v, newValue));
        return v;
    }

    @ForceInline
    public final byte getAndSetByteRelease(Object o, long offset, byte newValue) {
        byte v;
        do {
            v = getByte(o, offset);
        } while (!weakCompareAndSetByteRelease(o, offset, v, newValue));
        return v;
    }

    @ForceInline
    public final byte getAndSetByteAcquire(Object o, long offset, byte newValue) {
        byte v;
        do {
            v = getByteAcquire(o, offset);
        } while (!weakCompareAndSetByteAcquire(o, offset, v, newValue));
        return v;
    }

    @ForceInline
    public final boolean getAndSetBoolean(Object o, long offset, boolean newValue) {
        return byte2bool(getAndSetByte(o, offset, bool2byte(newValue)));
    }

    @ForceInline
    public final boolean getAndSetBooleanRelease(Object o, long offset, boolean newValue) {
        return byte2bool(getAndSetByteRelease(o, offset, bool2byte(newValue)));
    }

    @ForceInline
    public final boolean getAndSetBooleanAcquire(Object o, long offset, boolean newValue) {
        return byte2bool(getAndSetByteAcquire(o, offset, bool2byte(newValue)));
    }

    @IntrinsicCandidate
    public final short getAndSetShort(Object o, long offset, short newValue) {
        short v;
        do {
            v = getShortVolatile(o, offset);
        } while (!weakCompareAndSetShort(o, offset, v, newValue));
        return v;
    }

    @ForceInline
    public final short getAndSetShortRelease(Object o, long offset, short newValue) {
        short v;
        do {
            v = getShort(o, offset);
        } while (!weakCompareAndSetShortRelease(o, offset, v, newValue));
        return v;
    }

    @ForceInline
    public final short getAndSetShortAcquire(Object o, long offset, short newValue) {
        short v;
        do {
            v = getShortAcquire(o, offset);
        } while (!weakCompareAndSetShortAcquire(o, offset, v, newValue));
        return v;
    }

    @ForceInline
    public final char getAndSetChar(Object o, long offset, char newValue) {
        return s2c(getAndSetShort(o, offset, c2s(newValue)));
    }

    @ForceInline
    public final char getAndSetCharRelease(Object o, long offset, char newValue) {
        return s2c(getAndSetShortRelease(o, offset, c2s(newValue)));
    }

    @ForceInline
    public final char getAndSetCharAcquire(Object o, long offset, char newValue) {
        return s2c(getAndSetShortAcquire(o, offset, c2s(newValue)));
    }

    @ForceInline
    public final float getAndSetFloat(Object o, long offset, float newValue) {
        int v = getAndSetInt(o, offset, Float.floatToRawIntBits(newValue));
        return Float.intBitsToFloat(v);
    }

    @ForceInline
    public final float getAndSetFloatRelease(Object o, long offset, float newValue) {
        int v = getAndSetIntRelease(o, offset, Float.floatToRawIntBits(newValue));
        return Float.intBitsToFloat(v);
    }

    @ForceInline
    public final float getAndSetFloatAcquire(Object o, long offset, float newValue) {
        int v = getAndSetIntAcquire(o, offset, Float.floatToRawIntBits(newValue));
        return Float.intBitsToFloat(v);
    }

    @ForceInline
    public final double getAndSetDouble(Object o, long offset, double newValue) {
        long v = getAndSetLong(o, offset, Double.doubleToRawLongBits(newValue));
        return Double.longBitsToDouble(v);
    }

    @ForceInline
    public final double getAndSetDoubleRelease(Object o, long offset, double newValue) {
        long v = getAndSetLongRelease(o, offset, Double.doubleToRawLongBits(newValue));
        return Double.longBitsToDouble(v);
    }

    @ForceInline
    public final double getAndSetDoubleAcquire(Object o, long offset, double newValue) {
        long v = getAndSetLongAcquire(o, offset, Double.doubleToRawLongBits(newValue));
        return Double.longBitsToDouble(v);
    }


    // The following contain CAS-based Java implementations used on
    // platforms not supporting native instructions

    @ForceInline
    public final boolean getAndBitwiseOrBoolean(Object o, long offset, boolean mask) {
        return byte2bool(getAndBitwiseOrByte(o, offset, bool2byte(mask)));
    }

    @ForceInline
    public final boolean getAndBitwiseOrBooleanRelease(Object o, long offset, boolean mask) {
        return byte2bool(getAndBitwiseOrByteRelease(o, offset, bool2byte(mask)));
    }

    @ForceInline
    public final boolean getAndBitwiseOrBooleanAcquire(Object o, long offset, boolean mask) {
        return byte2bool(getAndBitwiseOrByteAcquire(o, offset, bool2byte(mask)));
    }

    @ForceInline
    public final boolean getAndBitwiseAndBoolean(Object o, long offset, boolean mask) {
        return byte2bool(getAndBitwiseAndByte(o, offset, bool2byte(mask)));
    }

    @ForceInline
    public final boolean getAndBitwiseAndBooleanRelease(Object o, long offset, boolean mask) {
        return byte2bool(getAndBitwiseAndByteRelease(o, offset, bool2byte(mask)));
    }

    @ForceInline
    public final boolean getAndBitwiseAndBooleanAcquire(Object o, long offset, boolean mask) {
        return byte2bool(getAndBitwiseAndByteAcquire(o, offset, bool2byte(mask)));
    }

    @ForceInline
    public final boolean getAndBitwiseXorBoolean(Object o, long offset, boolean mask) {
        return byte2bool(getAndBitwiseXorByte(o, offset, bool2byte(mask)));
    }

    @ForceInline
    public final boolean getAndBitwiseXorBooleanRelease(Object o, long offset, boolean mask) {
        return byte2bool(getAndBitwiseXorByteRelease(o, offset, bool2byte(mask)));
    }

    @ForceInline
    public final boolean getAndBitwiseXorBooleanAcquire(Object o, long offset, boolean mask) {
        return byte2bool(getAndBitwiseXorByteAcquire(o, offset, bool2byte(mask)));
    }


    @ForceInline
    public final byte getAndBitwiseOrByte(Object o, long offset, byte mask) {
        byte current;
        do {
            current = getByteVolatile(o, offset);
        } while (!weakCompareAndSetByte(o, offset,
                                                  current, (byte) (current | mask)));
        return current;
    }

    @ForceInline
    public final byte getAndBitwiseOrByteRelease(Object o, long offset, byte mask) {
        byte current;
        do {
            current = getByte(o, offset);
        } while (!weakCompareAndSetByteRelease(o, offset,
                                                 current, (byte) (current | mask)));
        return current;
    }

    @ForceInline
    public final byte getAndBitwiseOrByteAcquire(Object o, long offset, byte mask) {
        byte current;
        do {
            // Plain read, the value is a hint, the acquire CAS does the work
            current = getByte(o, offset);
        } while (!weakCompareAndSetByteAcquire(o, offset,
                                                 current, (byte) (current | mask)));
        return current;
    }

    @ForceInline
    public final byte getAndBitwiseAndByte(Object o, long offset, byte mask) {
        byte current;
        do {
            current = getByteVolatile(o, offset);
        } while (!weakCompareAndSetByte(o, offset,
                                                  current, (byte) (current & mask)));
        return current;
    }

    @ForceInline
    public final byte getAndBitwiseAndByteRelease(Object o, long offset, byte mask) {
        byte current;
        do {
            current = getByte(o, offset);
        } while (!weakCompareAndSetByteRelease(o, offset,
                                                 current, (byte) (current & mask)));
        return current;
    }

    @ForceInline
    public final byte getAndBitwiseAndByteAcquire(Object o, long offset, byte mask) {
        byte current;
        do {
            // Plain read, the value is a hint, the acquire CAS does the work
            current = getByte(o, offset);
        } while (!weakCompareAndSetByteAcquire(o, offset,
                                                 current, (byte) (current & mask)));
        return current;
    }

    @ForceInline
    public final byte getAndBitwiseXorByte(Object o, long offset, byte mask) {
        byte current;
        do {
            current = getByteVolatile(o, offset);
        } while (!weakCompareAndSetByte(o, offset,
                                                  current, (byte) (current ^ mask)));
        return current;
    }

    @ForceInline
    public final byte getAndBitwiseXorByteRelease(Object o, long offset, byte mask) {
        byte current;
        do {
            current = getByte(o, offset);
        } while (!weakCompareAndSetByteRelease(o, offset,
                                                 current, (byte) (current ^ mask)));
        return current;
    }

    @ForceInline
    public final byte getAndBitwiseXorByteAcquire(Object o, long offset, byte mask) {
        byte current;
        do {
            // Plain read, the value is a hint, the acquire CAS does the work
            current = getByte(o, offset);
        } while (!weakCompareAndSetByteAcquire(o, offset,
                                                 current, (byte) (current ^ mask)));
        return current;
    }


    @ForceInline
    public final char getAndBitwiseOrChar(Object o, long offset, char mask) {
        return s2c(getAndBitwiseOrShort(o, offset, c2s(mask)));
    }

    @ForceInline
    public final char getAndBitwiseOrCharRelease(Object o, long offset, char mask) {
        return s2c(getAndBitwiseOrShortRelease(o, offset, c2s(mask)));
    }

    @ForceInline
    public final char getAndBitwiseOrCharAcquire(Object o, long offset, char mask) {
        return s2c(getAndBitwiseOrShortAcquire(o, offset, c2s(mask)));
    }

    @ForceInline
    public final char getAndBitwiseAndChar(Object o, long offset, char mask) {
        return s2c(getAndBitwiseAndShort(o, offset, c2s(mask)));
    }

    @ForceInline
    public final char getAndBitwiseAndCharRelease(Object o, long offset, char mask) {
        return s2c(getAndBitwiseAndShortRelease(o, offset, c2s(mask)));
    }

    @ForceInline
    public final char getAndBitwiseAndCharAcquire(Object o, long offset, char mask) {
        return s2c(getAndBitwiseAndShortAcquire(o, offset, c2s(mask)));
    }

    @ForceInline
    public final char getAndBitwiseXorChar(Object o, long offset, char mask) {
        return s2c(getAndBitwiseXorShort(o, offset, c2s(mask)));
    }

    @ForceInline
    public final char getAndBitwiseXorCharRelease(Object o, long offset, char mask) {
        return s2c(getAndBitwiseXorShortRelease(o, offset, c2s(mask)));
    }

    @ForceInline
    public final char getAndBitwiseXorCharAcquire(Object o, long offset, char mask) {
        return s2c(getAndBitwiseXorShortAcquire(o, offset, c2s(mask)));
    }


    @ForceInline
    public final short getAndBitwiseOrShort(Object o, long offset, short mask) {
        short current;
        do {
            current = getShortVolatile(o, offset);
        } while (!weakCompareAndSetShort(o, offset,
                                                current, (short) (current | mask)));
        return current;
    }

    @ForceInline
    public final short getAndBitwiseOrShortRelease(Object o, long offset, short mask) {
        short current;
        do {
            current = getShort(o, offset);
        } while (!weakCompareAndSetShortRelease(o, offset,
                                               current, (short) (current | mask)));
        return current;
    }

    @ForceInline
    public final short getAndBitwiseOrShortAcquire(Object o, long offset, short mask) {
        short current;
        do {
            // Plain read, the value is a hint, the acquire CAS does the work
            current = getShort(o, offset);
        } while (!weakCompareAndSetShortAcquire(o, offset,
                                               current, (short) (current | mask)));
        return current;
    }

    @ForceInline
    public final short getAndBitwiseAndShort(Object o, long offset, short mask) {
        short current;
        do {
            current = getShortVolatile(o, offset);
        } while (!weakCompareAndSetShort(o, offset,
                                                current, (short) (current & mask)));
        return current;
    }

    @ForceInline
    public final short getAndBitwiseAndShortRelease(Object o, long offset, short mask) {
        short current;
        do {
            current = getShort(o, offset);
        } while (!weakCompareAndSetShortRelease(o, offset,
                                               current, (short) (current & mask)));
        return current;
    }

    @ForceInline
    public final short getAndBitwiseAndShortAcquire(Object o, long offset, short mask) {
        short current;
        do {
            // Plain read, the value is a hint, the acquire CAS does the work
            current = getShort(o, offset);
        } while (!weakCompareAndSetShortAcquire(o, offset,
                                               current, (short) (current & mask)));
        return current;
    }

    @ForceInline
    public final short getAndBitwiseXorShort(Object o, long offset, short mask) {
        short current;
        do {
            current = getShortVolatile(o, offset);
        } while (!weakCompareAndSetShort(o, offset,
                                                current, (short) (current ^ mask)));
        return current;
    }

    @ForceInline
    public final short getAndBitwiseXorShortRelease(Object o, long offset, short mask) {
        short current;
        do {
            current = getShort(o, offset);
        } while (!weakCompareAndSetShortRelease(o, offset,
                                               current, (short) (current ^ mask)));
        return current;
    }

    @ForceInline
    public final short getAndBitwiseXorShortAcquire(Object o, long offset, short mask) {
        short current;
        do {
            // Plain read, the value is a hint, the acquire CAS does the work
            current = getShort(o, offset);
        } while (!weakCompareAndSetShortAcquire(o, offset,
                                               current, (short) (current ^ mask)));
        return current;
    }
     */
    // END Android-removed: Not used in Android.

    // Android-removed: @ForceInline is an unsupported attribute.
    // @ForceInline
    public final int getAndBitwiseOrInt(Object o, long offset, int mask) {
        int current;
        do {
            current = getIntVolatile(o, offset);
        } while (!weakCompareAndSetInt(o, offset,
                                                current, current | mask));
        return current;
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    @ForceInline
    public final int getAndBitwiseOrIntRelease(Object o, long offset, int mask) {
        int current;
        do {
            current = getInt(o, offset);
        } while (!weakCompareAndSetIntRelease(o, offset,
                                               current, current | mask));
        return current;
    }

    @ForceInline
    public final int getAndBitwiseOrIntAcquire(Object o, long offset, int mask) {
        int current;
        do {
            // Plain read, the value is a hint, the acquire CAS does the work
            current = getInt(o, offset);
        } while (!weakCompareAndSetIntAcquire(o, offset,
                                               current, current | mask));
        return current;
    }
     */
    // END Android-removed: Not used in Android.

    /**
     * Atomically replaces the current value of a field or array element within
     * the given object with the result of bitwise AND between the current value
     * and mask.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param mask the mask value
     * @return the previous value
     * @since 9
     */
    // Android-removed: @ForceInline is an unsupported attribute.
    // @ForceInline
    public final int getAndBitwiseAndInt(Object o, long offset, int mask) {
        int current;
        do {
            current = getIntVolatile(o, offset);
        } while (!weakCompareAndSetInt(o, offset,
                                                current, current & mask));
        return current;
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    @ForceInline
    public final int getAndBitwiseAndIntRelease(Object o, long offset, int mask) {
        int current;
        do {
            current = getInt(o, offset);
        } while (!weakCompareAndSetIntRelease(o, offset,
                                               current, current & mask));
        return current;
    }

    @ForceInline
    public final int getAndBitwiseAndIntAcquire(Object o, long offset, int mask) {
        int current;
        do {
            // Plain read, the value is a hint, the acquire CAS does the work
            current = getInt(o, offset);
        } while (!weakCompareAndSetIntAcquire(o, offset,
                                               current, current & mask));
        return current;
    }
     */
    // END Android-removed: Not used in Android.

    // Android-removed: @ForceInline is an unsupported attribute.
    // @ForceInline
    public final int getAndBitwiseXorInt(Object o, long offset, int mask) {
        int current;
        do {
            current = getIntVolatile(o, offset);
        } while (!weakCompareAndSetInt(o, offset,
                                                current, current ^ mask));
        return current;
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    @ForceInline
    public final int getAndBitwiseXorIntRelease(Object o, long offset, int mask) {
        int current;
        do {
            current = getInt(o, offset);
        } while (!weakCompareAndSetIntRelease(o, offset,
                                               current, current ^ mask));
        return current;
    }

    @ForceInline
    public final int getAndBitwiseXorIntAcquire(Object o, long offset, int mask) {
        int current;
        do {
            // Plain read, the value is a hint, the acquire CAS does the work
            current = getInt(o, offset);
        } while (!weakCompareAndSetIntAcquire(o, offset,
                                               current, current ^ mask));
        return current;
    }


    @ForceInline
    public final long getAndBitwiseOrLong(Object o, long offset, long mask) {
        long current;
        do {
            current = getLongVolatile(o, offset);
        } while (!weakCompareAndSetLong(o, offset,
                                                current, current | mask));
        return current;
    }

    @ForceInline
    public final long getAndBitwiseOrLongRelease(Object o, long offset, long mask) {
        long current;
        do {
            current = getLong(o, offset);
        } while (!weakCompareAndSetLongRelease(o, offset,
                                               current, current | mask));
        return current;
    }

    @ForceInline
    public final long getAndBitwiseOrLongAcquire(Object o, long offset, long mask) {
        long current;
        do {
            // Plain read, the value is a hint, the acquire CAS does the work
            current = getLong(o, offset);
        } while (!weakCompareAndSetLongAcquire(o, offset,
                                               current, current | mask));
        return current;
    }

    @ForceInline
    public final long getAndBitwiseAndLong(Object o, long offset, long mask) {
        long current;
        do {
            current = getLongVolatile(o, offset);
        } while (!weakCompareAndSetLong(o, offset,
                                                current, current & mask));
        return current;
    }

    @ForceInline
    public final long getAndBitwiseAndLongRelease(Object o, long offset, long mask) {
        long current;
        do {
            current = getLong(o, offset);
        } while (!weakCompareAndSetLongRelease(o, offset,
                                               current, current & mask));
        return current;
    }

    @ForceInline
    public final long getAndBitwiseAndLongAcquire(Object o, long offset, long mask) {
        long current;
        do {
            // Plain read, the value is a hint, the acquire CAS does the work
            current = getLong(o, offset);
        } while (!weakCompareAndSetLongAcquire(o, offset,
                                               current, current & mask));
        return current;
    }

    @ForceInline
    public final long getAndBitwiseXorLong(Object o, long offset, long mask) {
        long current;
        do {
            current = getLongVolatile(o, offset);
        } while (!weakCompareAndSetLong(o, offset,
                                                current, current ^ mask));
        return current;
    }

    @ForceInline
    public final long getAndBitwiseXorLongRelease(Object o, long offset, long mask) {
        long current;
        do {
            current = getLong(o, offset);
        } while (!weakCompareAndSetLongRelease(o, offset,
                                               current, current ^ mask));
        return current;
    }

    @ForceInline
    public final long getAndBitwiseXorLongAcquire(Object o, long offset, long mask) {
        long current;
        do {
            // Plain read, the value is a hint, the acquire CAS does the work
            current = getLong(o, offset);
        } while (!weakCompareAndSetLongAcquire(o, offset,
                                               current, current ^ mask));
        return current;
    }
     */
    // END Android-removed: Not used in Android.

    /**
     * Ensures that loads before the fence will not be reordered with loads and
     * stores after the fence; a "LoadLoad plus LoadStore barrier".
     *
     * Corresponds to C11 atomic_thread_fence(memory_order_acquire)
     * (an "acquire fence").
     *
     * Provides a LoadLoad barrier followed by a LoadStore barrier.
     *
     * @since 1.8
     */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native void loadFence();

    /**
     * Ensures that loads and stores before the fence will not be reordered with
     * stores after the fence; a "StoreStore plus LoadStore barrier".
     *
     * Corresponds to C11 atomic_thread_fence(memory_order_release)
     * (a "release fence").
     *
     * Provides a StoreStore barrier followed by a LoadStore barrier.
     *
     *
     * @since 1.8
     */
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
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
    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    public native void fullFence();

    /**
     * Ensures that loads before the fence will not be reordered with
     * loads after the fence.
     *
     * @implNote
     * This method is operationally equivalent to {@link #loadFence()}.
     *
     * @since 9
     */
    public final void loadLoadFence() {
        loadFence();
    }

    /**
     * Ensures that stores before the fence will not be reordered with
     * stores after the fence.
     *
     * @implNote
     * This method is operationally equivalent to {@link #storeFence()}.
     *
     * @since 9
     */
    public final void storeStoreFence() {
        storeFence();
    }


    // BEGIN Android-removed: Not used in Android.
    /*
    /**
     * Throws IllegalAccessError; for use by the VM for access control
     * error support.
     * @since 1.8
     * /
    private static void throwIllegalAccessError() {
        throw new IllegalAccessError();
    }

    /**
     * Throws NoSuchMethodError; for use by the VM for redefinition support.
     * @since 13
     * /
    private static void throwNoSuchMethodError() {
        throw new NoSuchMethodError();
    }

    /**
     * @return Returns true if the native byte ordering of this
     * platform is big-endian, false if it is little-endian.
     * /
    public final boolean isBigEndian() { return BIG_ENDIAN; }

    /**
     * @return Returns true if this platform is capable of performing
     * accesses at addresses which are not aligned for the type of the
     * primitive type being accessed, false otherwise.
     * /
    public final boolean unalignedAccess() { return UNALIGNED_ACCESS; }
     */
    // END Android-removed: Not used in Android.

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
    @IntrinsicCandidate
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
    @IntrinsicCandidate
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

    // BEGIN Android-removed: Not used in Android.
    /*
    /** @see #getLongUnaligned(Object, long, boolean) * /
    public final int getIntUnaligned(Object o, long offset, boolean bigEndian) {
        return convEndian(bigEndian, getIntUnaligned(o, offset));
    }

    /** @see #getLongUnaligned(Object, long) * /
    @IntrinsicCandidate
    public final short getShortUnaligned(Object o, long offset) {
        if ((offset & 1) == 0) {
            return getShort(o, offset);
        } else {
            return makeShort(getByte(o, offset),
                             getByte(o, offset + 1));
        }
    }
    /** @see #getLongUnaligned(Object, long, boolean) * /
    public final short getShortUnaligned(Object o, long offset, boolean bigEndian) {
        return convEndian(bigEndian, getShortUnaligned(o, offset));
    }

    /** @see #getLongUnaligned(Object, long) * /
    @IntrinsicCandidate
    public final char getCharUnaligned(Object o, long offset) {
        if ((offset & 1) == 0) {
            return getChar(o, offset);
        } else {
            return (char)makeShort(getByte(o, offset),
                                   getByte(o, offset + 1));
        }
    }

    /** @see #getLongUnaligned(Object, long, boolean) * /
    public final char getCharUnaligned(Object o, long offset, boolean bigEndian) {
        return convEndian(bigEndian, getCharUnaligned(o, offset));
    }

    /**
     * Stores a value at some byte offset into a given Java object.
     * <p>
     * The specification of this method is the same as {@link
     * #getLong(Object, long)} except that the offset does not need to
     * have been obtained from {@link #objectFieldOffset} on the
     * {@link java.lang.reflect.Field} of some Java field.  The value
     * in memory is raw data, and need not correspond to any Java
     * variable.  The endianness of the value in memory is the
     * endianness of the native platform.
     * <p>
     * The write will be atomic with respect to the largest power of
     * two that divides the GCD of the offset and the storage size.
     * For example, putLongUnaligned will make atomic writes of 2-, 4-,
     * or 8-byte storage units if the offset is zero mod 2, 4, or 8,
     * respectively.  There are no other guarantees of atomicity.
     * <p>
     * 8-byte atomicity is only guaranteed on platforms on which
     * support atomic accesses to longs.
     *
     * @param o Java heap object in which the value resides, if any, else
     *        null
     * @param offset The offset in bytes from the start of the object
     * @param x the value to store
     * @throws RuntimeException No defined exceptions are thrown, not even
     *         {@link NullPointerException}
     * @since 9
     * /
    @IntrinsicCandidate
    public final void putLongUnaligned(Object o, long offset, long x) {
        if ((offset & 7) == 0) {
            putLong(o, offset, x);
        } else if ((offset & 3) == 0) {
            putLongParts(o, offset,
                         (int)(x >> 0),
                         (int)(x >>> 32));
        } else if ((offset & 1) == 0) {
            putLongParts(o, offset,
                         (short)(x >>> 0),
                         (short)(x >>> 16),
                         (short)(x >>> 32),
                         (short)(x >>> 48));
        } else {
            putLongParts(o, offset,
                         (byte)(x >>> 0),
                         (byte)(x >>> 8),
                         (byte)(x >>> 16),
                         (byte)(x >>> 24),
                         (byte)(x >>> 32),
                         (byte)(x >>> 40),
                         (byte)(x >>> 48),
                         (byte)(x >>> 56));
        }
    }

    /**
     * As {@link #putLongUnaligned(Object, long, long)} but with an additional
     * argument which specifies the endianness of the value as stored in memory.
     * @param o Java heap object in which the value resides
     * @param offset The offset in bytes from the start of the object
     * @param x the value to store
     * @param bigEndian The endianness of the value
     * @throws RuntimeException No defined exceptions are thrown, not even
     *         {@link NullPointerException}
     * @since 9
     * /
    public final void putLongUnaligned(Object o, long offset, long x, boolean bigEndian) {
        putLongUnaligned(o, offset, convEndian(bigEndian, x));
    }

    /** @see #putLongUnaligned(Object, long, long) * /
    @IntrinsicCandidate
    public final void putIntUnaligned(Object o, long offset, int x) {
        if ((offset & 3) == 0) {
            putInt(o, offset, x);
        } else if ((offset & 1) == 0) {
            putIntParts(o, offset,
                        (short)(x >> 0),
                        (short)(x >>> 16));
        } else {
            putIntParts(o, offset,
                        (byte)(x >>> 0),
                        (byte)(x >>> 8),
                        (byte)(x >>> 16),
                        (byte)(x >>> 24));
        }
    }
    /** @see #putLongUnaligned(Object, long, long, boolean) * /
    public final void putIntUnaligned(Object o, long offset, int x, boolean bigEndian) {
        putIntUnaligned(o, offset, convEndian(bigEndian, x));
    }

    /** @see #putLongUnaligned(Object, long, long) * /
    @IntrinsicCandidate
    public final void putShortUnaligned(Object o, long offset, short x) {
        if ((offset & 1) == 0) {
            putShort(o, offset, x);
        } else {
            putShortParts(o, offset,
                          (byte)(x >>> 0),
                          (byte)(x >>> 8));
        }
    }
    /** @see #putLongUnaligned(Object, long, long, boolean) * /
    public final void putShortUnaligned(Object o, long offset, short x, boolean bigEndian) {
        putShortUnaligned(o, offset, convEndian(bigEndian, x));
    }

    /** @see #putLongUnaligned(Object, long, long) * /
    @IntrinsicCandidate
    public final void putCharUnaligned(Object o, long offset, char x) {
        putShortUnaligned(o, offset, (short)x);
    }
    /** @see #putLongUnaligned(Object, long, long, boolean) * /
    public final void putCharUnaligned(Object o, long offset, char x, boolean bigEndian) {
        putCharUnaligned(o, offset, convEndian(bigEndian, x));
    }

     */
    // END Android-removed: Not used in Android.

    private static int pickPos(int top, int pos) { return BIG_ENDIAN ? top - pos : pos; }

    // These methods construct integers from bytes.  The byte ordering
    // is the native endianness of this platform.
    private static long makeLong(byte i0, byte i1, byte i2, byte i3, byte i4, byte i5, byte i6, byte i7) {
        return ((toUnsignedLong(i0) << pickPos(56, 0))
              | (toUnsignedLong(i1) << pickPos(56, 8))
              | (toUnsignedLong(i2) << pickPos(56, 16))
              | (toUnsignedLong(i3) << pickPos(56, 24))
              | (toUnsignedLong(i4) << pickPos(56, 32))
              | (toUnsignedLong(i5) << pickPos(56, 40))
              | (toUnsignedLong(i6) << pickPos(56, 48))
              | (toUnsignedLong(i7) << pickPos(56, 56)));
    }
    private static long makeLong(short i0, short i1, short i2, short i3) {
        return ((toUnsignedLong(i0) << pickPos(48, 0))
              | (toUnsignedLong(i1) << pickPos(48, 16))
              | (toUnsignedLong(i2) << pickPos(48, 32))
              | (toUnsignedLong(i3) << pickPos(48, 48)));
    }
    private static long makeLong(int i0, int i1) {
        return (toUnsignedLong(i0) << pickPos(32, 0))
             | (toUnsignedLong(i1) << pickPos(32, 32));
    }
    private static int makeInt(short i0, short i1) {
        return (toUnsignedInt(i0) << pickPos(16, 0))
             | (toUnsignedInt(i1) << pickPos(16, 16));
    }
    private static int makeInt(byte i0, byte i1, byte i2, byte i3) {
        return ((toUnsignedInt(i0) << pickPos(24, 0))
              | (toUnsignedInt(i1) << pickPos(24, 8))
              | (toUnsignedInt(i2) << pickPos(24, 16))
              | (toUnsignedInt(i3) << pickPos(24, 24)));
    }
    private static short makeShort(byte i0, byte i1) {
        return (short)((toUnsignedInt(i0) << pickPos(8, 0))
                     | (toUnsignedInt(i1) << pickPos(8, 8)));
    }

    // BEGIN Android-removed: Not used in Android.
    /*
    private static byte  pick(byte  le, byte  be) { return BIG_ENDIAN ? be : le; }
    private static short pick(short le, short be) { return BIG_ENDIAN ? be : le; }
    private static int   pick(int   le, int   be) { return BIG_ENDIAN ? be : le; }

    // These methods write integers to memory from smaller parts
    // provided by their caller.  The ordering in which these parts
    // are written is the native endianness of this platform.
    private void putLongParts(Object o, long offset, byte i0, byte i1, byte i2, byte i3, byte i4, byte i5, byte i6, byte i7) {
        putByte(o, offset + 0, pick(i0, i7));
        putByte(o, offset + 1, pick(i1, i6));
        putByte(o, offset + 2, pick(i2, i5));
        putByte(o, offset + 3, pick(i3, i4));
        putByte(o, offset + 4, pick(i4, i3));
        putByte(o, offset + 5, pick(i5, i2));
        putByte(o, offset + 6, pick(i6, i1));
        putByte(o, offset + 7, pick(i7, i0));
    }
    private void putLongParts(Object o, long offset, short i0, short i1, short i2, short i3) {
        putShort(o, offset + 0, pick(i0, i3));
        putShort(o, offset + 2, pick(i1, i2));
        putShort(o, offset + 4, pick(i2, i1));
        putShort(o, offset + 6, pick(i3, i0));
    }
    private void putLongParts(Object o, long offset, int i0, int i1) {
        putInt(o, offset + 0, pick(i0, i1));
        putInt(o, offset + 4, pick(i1, i0));
    }
    private void putIntParts(Object o, long offset, short i0, short i1) {
        putShort(o, offset + 0, pick(i0, i1));
        putShort(o, offset + 2, pick(i1, i0));
    }
    private void putIntParts(Object o, long offset, byte i0, byte i1, byte i2, byte i3) {
        putByte(o, offset + 0, pick(i0, i3));
        putByte(o, offset + 1, pick(i1, i2));
        putByte(o, offset + 2, pick(i2, i1));
        putByte(o, offset + 3, pick(i3, i0));
    }
    private void putShortParts(Object o, long offset, byte i0, byte i1) {
        putByte(o, offset + 0, pick(i0, i1));
        putByte(o, offset + 1, pick(i1, i0));
    }
     */
    // END Android-removed: Not used in Android.

    // Zero-extend an integer
    private static int toUnsignedInt(byte n)    { return n & 0xff; }
    private static int toUnsignedInt(short n)   { return n & 0xffff; }
    private static long toUnsignedLong(byte n)  { return n & 0xffL; }
    private static long toUnsignedLong(short n) { return n & 0xffffL; }
    private static long toUnsignedLong(int n)   { return n & 0xffffffffL; }

    // BEGIN Android-removed: Not used in Android.
    /*
    // Maybe byte-reverse an integer
    private static char convEndian(boolean big, char n)   { return big == BIG_ENDIAN ? n : Character.reverseBytes(n); }
    private static short convEndian(boolean big, short n) { return big == BIG_ENDIAN ? n : Short.reverseBytes(n)    ; }
    private static int convEndian(boolean big, int n)     { return big == BIG_ENDIAN ? n : Integer.reverseBytes(n)  ; }
    private static long convEndian(boolean big, long n)   { return big == BIG_ENDIAN ? n : Long.reverseBytes(n)     ; }



    private native long allocateMemory0(long bytes);
    private native long reallocateMemory0(long address, long bytes);
    private native void freeMemory0(long address);
    private native void setMemory0(Object o, long offset, long bytes, byte value);
     */
    // END Android-removed: Not used in Android.

    // Android-added: FastNative annotation.
    @FastNative
    @IntrinsicCandidate
    private native void copyMemory0(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes);

    // BEGIN Android-removed: Not used in Android.
    /*
    private native void copySwapMemory0(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes, long elemSize);
    private native long objectFieldOffset0(Field f);
    private native long objectFieldOffset1(Class<?> c, String name);
    private native long staticFieldOffset0(Field f);
    private native Object staticFieldBase0(Field f);
    private native boolean shouldBeInitialized0(Class<?> c);
    private native void ensureClassInitialized0(Class<?> c);
    private native int arrayBaseOffset0(Class<?> arrayClass);
    private native int arrayIndexScale0(Class<?> arrayClass);
    private native int getLoadAverage0(double[] loadavg, int nelems);


    /**
     * Invokes the given direct byte buffer's cleaner, if any.
     *
     * @param directBuffer a direct byte buffer
     * @throws NullPointerException     if {@code directBuffer} is null
     * @throws IllegalArgumentException if {@code directBuffer} is non-direct,
     *                                  or is a {@link java.nio.Buffer#slice slice}, or is a
     *                                  {@link java.nio.Buffer#duplicate duplicate}
     * /
    public void invokeCleaner(java.nio.ByteBuffer directBuffer) {
        if (!directBuffer.isDirect())
            throw new IllegalArgumentException("buffer is non-direct");

        DirectBuffer db = (DirectBuffer) directBuffer;
        if (db.attachment() != null)
            throw new IllegalArgumentException("duplicate or slice");

        Cleaner cleaner = db.cleaner();
        if (cleaner != null) {
            cleaner.clean();
        }
    }
     */
    // END Android-removed: Not used in Android.

    @Deprecated(since="12", forRemoval=true)
    public final Object getObject(Object o, long offset) {
        return getReference(o, offset);
    }
    @Deprecated(since="12", forRemoval=true)
    public final Object getObjectVolatile(Object o, long offset) {
        return getReferenceVolatile(o, offset);
    }
    @Deprecated(since="12", forRemoval=true)
    public final Object getObjectAcquire(Object o, long offset) {
        return getReferenceAcquire(o, offset);
    }

    @Deprecated(since="12", forRemoval=true)
    public final void putObject(Object o, long offset, Object x) {
        putReference(o, offset, x);
    }

    @Deprecated(since="12", forRemoval=true)
    public final void putObjectVolatile(Object o, long offset, Object x) {
        putReferenceVolatile(o, offset, x);
    }
    @Deprecated(since="12", forRemoval=true)
    public final void putObjectRelease(Object o, long offset, Object x) {
        putReferenceRelease(o, offset, x);
    }

    @Deprecated(since="12", forRemoval=true)
    public final Object getAndSetObject(Object o, long offset, Object newValue) {
        return getAndSetReference(o, offset, newValue);
    }

    @Deprecated(since="12", forRemoval=true)
    public final boolean compareAndSetObject(Object o, long offset, Object expected, Object x) {
        return compareAndSetReference(o, offset, expected, x);
    }

    // BEGIN Android-added: Methods added for the Android platform.
    @FastNative
    private static native int getArrayBaseOffsetForComponentType(Class component_class);
    @FastNative
    private static native int getArrayIndexScaleForComponentType(Class component_class);

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
     * Lazy set an int field.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    @FastNative
    public native void putOrderedInt(Object obj, long offset, int newValue);

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
     * Lazy set an object field.
     *
     * @param obj non-{@code null}; object containing the field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    @FastNative
    public native void putOrderedObject(Object obj, long offset,
            Object newValue);

    // END Android-added: Methods added for the Android platform.


}
