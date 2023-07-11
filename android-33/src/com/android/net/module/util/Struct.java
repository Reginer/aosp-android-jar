/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.net.module.util;

import android.net.MacAddress;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Define a generic class that helps to parse the structured message.
 *
 * Example usage:
 *
 *    // C-style NduserOption message header definition in the kernel:
 *    struct nduseroptmsg {
 *        unsigned char nduseropt_family;
 *        unsigned char nduseropt_pad1;
 *        unsigned short nduseropt_opts_len;
 *        int nduseropt_ifindex;
 *        __u8 nduseropt_icmp_type;
 *        __u8 nduseropt_icmp_code;
 *        unsigned short nduseropt_pad2;
 *        unsigned int nduseropt_pad3;
 *    }
 *
 *    - Declare a subclass with explicit constructor or not which extends from this class to parse
 *      NduserOption header from raw bytes array.
 *
 *    - Option w/ explicit constructor:
 *      static class NduserOptHeaderMessage extends Struct {
 *          @Field(order = 0, type = Type.U8, padding = 1)
 *          final short family;
 *          @Field(order = 1, type = Type.U16)
 *          final int len;
 *          @Field(order = 2, type = Type.S32)
 *          final int ifindex;
 *          @Field(order = 3, type = Type.U8)
 *          final short type;
 *          @Field(order = 4, type = Type.U8, padding = 6)
 *          final short code;
 *
 *          NduserOptHeaderMessage(final short family, final int len, final int ifindex,
 *                  final short type, final short code) {
 *              this.family = family;
 *              this.len = len;
 *              this.ifindex = ifindex;
 *              this.type = type;
 *              this.code = code;
 *          }
 *      }
 *
 *      - Option w/o explicit constructor:
 *        static class NduserOptHeaderMessage extends Struct {
 *            @Field(order = 0, type = Type.U8, padding = 1)
 *            short family;
 *            @Field(order = 1, type = Type.U16)
 *            int len;
 *            @Field(order = 2, type = Type.S32)
 *            int ifindex;
 *            @Field(order = 3, type = Type.U8)
 *            short type;
 *            @Field(order = 4, type = Type.U8, padding = 6)
 *            short code;
 *        }
 *
 *    - Parse the target message and refer the members.
 *      final ByteBuffer buf = ByteBuffer.wrap(RAW_BYTES_ARRAY);
 *      buf.order(ByteOrder.nativeOrder());
 *      final NduserOptHeaderMessage nduserHdrMsg = Struct.parse(NduserOptHeaderMessage.class, buf);
 *      assertEquals(10, nduserHdrMsg.family);
 */
public class Struct {
    public enum Type {
        U8,          // unsigned byte,  size = 1 byte
        U16,         // unsigned short, size = 2 bytes
        U32,         // unsigned int,   size = 4 bytes
        U63,         // unsigned long(MSB: 0), size = 8 bytes
        U64,         // unsigned long,  size = 8 bytes
        S8,          // signed byte,    size = 1 byte
        S16,         // signed short,   size = 2 bytes
        S32,         // signed int,     size = 4 bytes
        S64,         // signed long,    size = 8 bytes
        UBE16,       // unsigned short in network order, size = 2 bytes
        UBE32,       // unsigned int in network order,   size = 4 bytes
        UBE63,       // unsigned long(MSB: 0) in network order, size = 8 bytes
        UBE64,       // unsigned long in network order,  size = 8 bytes
        ByteArray,   // byte array with predefined length
        EUI48,       // IEEE Extended Unique Identifier, a 48-bits long MAC address in network order
        Ipv4Address, // IPv4 address in network order
        Ipv6Address, // IPv6 address in network order
    }

    /**
     * Indicate that the field marked with this annotation will automatically be managed by this
     * class (e.g., will be parsed by #parse).
     *
     * order:     The placeholder associated with each field, consecutive order starting from zero.
     * type:      The primitive data type listed in above Type enumeration.
     * padding:   Padding bytes appear after the field for alignment.
     * arraysize: The length of byte array.
     *
     * Annotation associated with field MUST have order and type properties at least, padding
     * and arraysize properties depend on the specific usage, if these properties are absent,
     * then default value 0 will be applied.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Field {
        int order();
        Type type();
        int padding() default 0;
        int arraysize() default 0;
    }

    private static class FieldInfo {
        @NonNull
        public final Field annotation;
        @NonNull
        public final java.lang.reflect.Field field;

        FieldInfo(final Field annotation, final java.lang.reflect.Field field) {
            this.annotation = annotation;
            this.field = field;
        }
    }
    private static ConcurrentHashMap<Class, FieldInfo[]> sFieldCache = new ConcurrentHashMap();

    private static void checkAnnotationType(final Field annotation, final Class fieldType) {
        switch (annotation.type()) {
            case U8:
            case S16:
                if (fieldType == Short.TYPE) return;
                break;
            case U16:
            case S32:
            case UBE16:
                if (fieldType == Integer.TYPE) return;
                break;
            case U32:
            case U63:
            case S64:
            case UBE32:
            case UBE63:
                if (fieldType == Long.TYPE) return;
                break;
            case U64:
            case UBE64:
                if (fieldType == BigInteger.class) return;
                break;
            case S8:
                if (fieldType == Byte.TYPE) return;
                break;
            case ByteArray:
                if (fieldType != byte[].class) break;
                if (annotation.arraysize() <= 0) {
                    throw new IllegalArgumentException("Invalid ByteArray size: "
                            + annotation.arraysize());
                }
                return;
            case EUI48:
                if (fieldType == MacAddress.class) return;
                break;
            case Ipv4Address:
                if (fieldType == Inet4Address.class) return;
                break;
            case Ipv6Address:
                if (fieldType == Inet6Address.class) return;
                break;
            default:
                throw new IllegalArgumentException("Unknown type" + annotation.type());
        }
        throw new IllegalArgumentException("Invalid primitive data type: " + fieldType
                + " for annotation type: " + annotation.type());
    }

    private static int getFieldLength(final Field annotation) {
        int length = 0;
        switch (annotation.type()) {
            case U8:
            case S8:
                length = 1;
                break;
            case U16:
            case S16:
            case UBE16:
                length = 2;
                break;
            case U32:
            case S32:
            case UBE32:
                length = 4;
                break;
            case U63:
            case U64:
            case S64:
            case UBE63:
            case UBE64:
                length = 8;
                break;
            case ByteArray:
                length = annotation.arraysize();
                break;
            case EUI48:
                length = 6;
                break;
            case Ipv4Address:
                length = 4;
                break;
            case Ipv6Address:
                length = 16;
                break;
            default:
                throw new IllegalArgumentException("Unknown type" + annotation.type());
        }
        return length + annotation.padding();
    }

    private static boolean isStructSubclass(final Class clazz) {
        return clazz != null && Struct.class.isAssignableFrom(clazz) && Struct.class != clazz;
    }

    private static int getAnnotationFieldCount(final Class clazz) {
        int count = 0;
        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Field.class)) count++;
        }
        return count;
    }

    private static boolean allFieldsFinal(final FieldInfo[] fields, boolean immutable) {
        for (FieldInfo fi : fields) {
            if (Modifier.isFinal(fi.field.getModifiers()) != immutable) return false;
        }
        return true;
    }

    private static boolean hasBothMutableAndImmutableFields(final FieldInfo[] fields) {
        return !allFieldsFinal(fields, true /* immutable */)
                && !allFieldsFinal(fields, false /* mutable */);
    }

    private static boolean matchConstructor(final Constructor cons, final FieldInfo[] fields) {
        final Class[] paramTypes = cons.getParameterTypes();
        if (paramTypes.length != fields.length) return false;
        for (int i = 0; i < paramTypes.length; i++) {
            if (!paramTypes[i].equals(fields[i].field.getType())) return false;
        }
        return true;
    }

    /**
     * Read U64/UBE64 type data from ByteBuffer and output a BigInteger instance.
     *
     * @param buf The byte buffer to read.
     * @param type The annotation type.
     *
     * The magnitude argument of BigInteger constructor is a byte array in big-endian order.
     * If BigInteger data is read from the byte buffer in little-endian, reverse the order of
     * the bytes is required; if BigInteger data is read from the byte buffer in big-endian,
     * then just keep it as-is.
     */
    private static BigInteger readBigInteger(final ByteBuffer buf, final Type type) {
        final byte[] input = new byte[8];
        boolean reverseBytes = (type == Type.U64 && buf.order() == ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 8; i++) {
            input[reverseBytes ? input.length - 1 - i : i] = buf.get();
        }
        return new BigInteger(1, input);
    }

    /**
     * Get the last 8 bytes of a byte array. If there are less than 8 bytes,
     * the first bytes are replaced with zeroes.
     */
    private static byte[] getLast8Bytes(final byte[] input) {
        final byte[] tmp = new byte[8];
        System.arraycopy(
                input,
                Math.max(0, input.length - 8), // srcPos: read at most last 8 bytes
                tmp,
                Math.max(0, 8 - input.length), // dstPos: pad output with that many zeroes
                Math.min(8, input.length));    // length
        return tmp;
    }

    /**
     * Convert U64/UBE64 type data interpreted by BigInteger class to bytes array, output are
     * always 8 bytes.
     *
     * @param bigInteger The number to convert.
     * @param order Indicate ByteBuffer is read as little-endian or big-endian.
     * @param type The annotation U64 type.
     *
     * BigInteger#toByteArray returns a byte array containing the 2's complement representation
     * of this BigInteger, in big-endian. If annotation type is U64 and ByteBuffer is read as
     * little-endian, then reversing the order of the bytes is required.
     */
    private static byte[] bigIntegerToU64Bytes(final BigInteger bigInteger, final ByteOrder order,
            final Type type) {
        final byte[] bigIntegerBytes = bigInteger.toByteArray();
        final byte[] output = getLast8Bytes(bigIntegerBytes);

        if (type == Type.U64 && order == ByteOrder.LITTLE_ENDIAN) {
            for (int i = 0; i < 4; i++) {
                byte tmp = output[i];
                output[i] = output[7 - i];
                output[7 - i] = tmp;
            }
        }
        return output;
    }

    private static Object getFieldValue(final ByteBuffer buf, final FieldInfo fieldInfo)
            throws BufferUnderflowException {
        final Object value;
        checkAnnotationType(fieldInfo.annotation, fieldInfo.field.getType());
        switch (fieldInfo.annotation.type()) {
            case U8:
                value = (short) (buf.get() & 0xFF);
                break;
            case U16:
                value = (int) (buf.getShort() & 0xFFFF);
                break;
            case U32:
                value = (long) (buf.getInt() & 0xFFFFFFFFL);
                break;
            case U64:
                value = readBigInteger(buf, Type.U64);
                break;
            case S8:
                value = buf.get();
                break;
            case S16:
                value = buf.getShort();
                break;
            case S32:
                value = buf.getInt();
                break;
            case U63:
            case S64:
                value = buf.getLong();
                break;
            case UBE16:
                if (buf.order() == ByteOrder.LITTLE_ENDIAN) {
                    value = (int) (Short.reverseBytes(buf.getShort()) & 0xFFFF);
                } else {
                    value = (int) (buf.getShort() & 0xFFFF);
                }
                break;
            case UBE32:
                if (buf.order() == ByteOrder.LITTLE_ENDIAN) {
                    value = (long) (Integer.reverseBytes(buf.getInt()) & 0xFFFFFFFFL);
                } else {
                    value = (long) (buf.getInt() & 0xFFFFFFFFL);
                }
                break;
            case UBE63:
                if (buf.order() == ByteOrder.LITTLE_ENDIAN) {
                    value = Long.reverseBytes(buf.getLong());
                } else {
                    value = buf.getLong();
                }
                break;
            case UBE64:
                value = readBigInteger(buf, Type.UBE64);
                break;
            case ByteArray:
                final byte[] array = new byte[fieldInfo.annotation.arraysize()];
                buf.get(array);
                value = array;
                break;
            case EUI48:
                final byte[] macAddress = new byte[6];
                buf.get(macAddress);
                value = MacAddress.fromBytes(macAddress);
                break;
            case Ipv4Address:
            case Ipv6Address:
                final boolean isIpv6 = (fieldInfo.annotation.type() == Type.Ipv6Address);
                final byte[] address = new byte[isIpv6 ? 16 : 4];
                buf.get(address);
                try {
                    value = InetAddress.getByAddress(address);
                } catch (UnknownHostException e) {
                    throw new IllegalArgumentException("illegal length of IP address", e);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown type:" + fieldInfo.annotation.type());
        }

        // Skip the padding data for alignment if any.
        if (fieldInfo.annotation.padding() > 0) {
            buf.position(buf.position() + fieldInfo.annotation.padding());
        }
        return value;
    }

    @Nullable
    private Object getFieldValue(@NonNull java.lang.reflect.Field field) {
        try {
            return field.get(this);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access field: " + field, e);
        }
    }

    private static void putFieldValue(final ByteBuffer output, final FieldInfo fieldInfo,
            final Object value) throws BufferUnderflowException {
        switch (fieldInfo.annotation.type()) {
            case U8:
                output.put((byte) (((short) value) & 0xFF));
                break;
            case U16:
                output.putShort((short) (((int) value) & 0xFFFF));
                break;
            case U32:
                output.putInt((int) (((long) value) & 0xFFFFFFFFL));
                break;
            case U63:
                output.putLong((long) value);
                break;
            case U64:
                output.put(bigIntegerToU64Bytes((BigInteger) value, output.order(), Type.U64));
                break;
            case S8:
                output.put((byte) value);
                break;
            case S16:
                output.putShort((short) value);
                break;
            case S32:
                output.putInt((int) value);
                break;
            case S64:
                output.putLong((long) value);
                break;
            case UBE16:
                if (output.order() == ByteOrder.LITTLE_ENDIAN) {
                    output.putShort(Short.reverseBytes((short) (((int) value) & 0xFFFF)));
                } else {
                    output.putShort((short) (((int) value) & 0xFFFF));
                }
                break;
            case UBE32:
                if (output.order() == ByteOrder.LITTLE_ENDIAN) {
                    output.putInt(Integer.reverseBytes(
                            (int) (((long) value) & 0xFFFFFFFFL)));
                } else {
                    output.putInt((int) (((long) value) & 0xFFFFFFFFL));
                }
                break;
            case UBE63:
                if (output.order() == ByteOrder.LITTLE_ENDIAN) {
                    output.putLong(Long.reverseBytes((long) value));
                } else {
                    output.putLong((long) value);
                }
                break;
            case UBE64:
                output.put(bigIntegerToU64Bytes((BigInteger) value, output.order(), Type.UBE64));
                break;
            case ByteArray:
                checkByteArraySize((byte[]) value, fieldInfo);
                output.put((byte[]) value);
                break;
            case EUI48:
                final byte[] macAddress = ((MacAddress) value).toByteArray();
                output.put(macAddress);
                break;
            case Ipv4Address:
            case Ipv6Address:
                final byte[] address = ((InetAddress) value).getAddress();
                output.put(address);
                break;
            default:
                throw new IllegalArgumentException("Unknown type:" + fieldInfo.annotation.type());
        }

        // padding zero after field value for alignment.
        for (int i = 0; i < fieldInfo.annotation.padding(); i++) output.put((byte) 0);
    }

    private static FieldInfo[] getClassFieldInfo(final Class clazz) {
        if (!isStructSubclass(clazz)) {
            throw new IllegalArgumentException(clazz.getName() + " is not a subclass of "
                    + Struct.class.getName() + ", its superclass is "
                    + clazz.getSuperclass().getName());
        }

        final FieldInfo[] cachedAnnotationFields = sFieldCache.get(clazz);
        if (cachedAnnotationFields != null) {
            return cachedAnnotationFields;
        }

        // Since array returned from Class#getDeclaredFields doesn't guarantee the actual order
        // of field appeared in the class, that is a problem when parsing raw data read from
        // ByteBuffer. Store the fields appeared by the order() defined in the Field annotation.
        final FieldInfo[] annotationFields = new FieldInfo[getAnnotationFieldCount(clazz)];
        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;

            final Field annotation = field.getAnnotation(Field.class);
            if (annotation == null) {
                throw new IllegalArgumentException("Field " + field.getName()
                        + " is missing the " + Field.class.getSimpleName()
                        + " annotation");
            }
            if (annotation.order() < 0 || annotation.order() >= annotationFields.length) {
                throw new IllegalArgumentException("Annotation order: " + annotation.order()
                        + " is negative or non-consecutive");
            }
            if (annotationFields[annotation.order()] != null) {
                throw new IllegalArgumentException("Duplicated annotation order: "
                        + annotation.order());
            }
            annotationFields[annotation.order()] = new FieldInfo(annotation, field);
        }
        sFieldCache.putIfAbsent(clazz, annotationFields);
        return annotationFields;
    }

    /**
     * Parse raw data from ByteBuffer according to the pre-defined annotation rule and return
     * the type-variable object which is subclass of Struct class.
     *
     * TODO:
     * 1. Support subclass inheritance.
     * 2. Introduce annotation processor to enforce the subclass naming schema.
     */
    public static <T> T parse(final Class<T> clazz, final ByteBuffer buf) {
        try {
            final FieldInfo[] foundFields = getClassFieldInfo(clazz);
            if (hasBothMutableAndImmutableFields(foundFields)) {
                throw new IllegalArgumentException("Class has both final and non-final fields");
            }

            Constructor<?> constructor = null;
            Constructor<?> defaultConstructor = null;
            final Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            for (Constructor cons : constructors) {
                if (matchConstructor(cons, foundFields)) constructor = cons;
                if (cons.getParameterTypes().length == 0) defaultConstructor = cons;
            }

            if (constructor == null && defaultConstructor == null) {
                throw new IllegalArgumentException("Fail to find available constructor");
            }
            if (constructor != null) {
                final Object[] args = new Object[foundFields.length];
                for (int i = 0; i < args.length; i++) {
                    args[i] = getFieldValue(buf, foundFields[i]);
                }
                return (T) constructor.newInstance(args);
            }

            final Object instance = defaultConstructor.newInstance();
            for (FieldInfo fi : foundFields) {
                fi.field.set(instance, getFieldValue(buf, fi));
            }
            return (T) instance;
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new IllegalArgumentException("Fail to create a instance from constructor", e);
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Fail to read raw data from ByteBuffer", e);
        }
    }

    private static int getSizeInternal(final FieldInfo[] fieldInfos) {
        int size = 0;
        for (FieldInfo fi : fieldInfos) {
            size += getFieldLength(fi.annotation);
        }
        return size;
    }

    // Check whether the actual size of byte array matches the array size declared in
    // annotation. For other annotation types, the actual length of field could be always
    // deduced from annotation correctly.
    private static void checkByteArraySize(@Nullable final byte[] array,
            @NonNull final FieldInfo fieldInfo) {
        Objects.requireNonNull(array, "null byte array for field " + fieldInfo.field.getName());
        int annotationArraySize = fieldInfo.annotation.arraysize();
        if (array.length == annotationArraySize) return;
        throw new IllegalStateException("byte array actual length: "
                + array.length + " doesn't match the declared array size: " + annotationArraySize);
    }

    private void writeToByteBufferInternal(final ByteBuffer output, final FieldInfo[] fieldInfos) {
        for (FieldInfo fi : fieldInfos) {
            final Object value = getFieldValue(fi.field);
            try {
                putFieldValue(output, fi, value);
            } catch (BufferUnderflowException e) {
                throw new IllegalArgumentException("Fail to fill raw data to ByteBuffer", e);
            }
        }
    }

    /**
     * Get the size of Struct subclass object.
     */
    public static <T extends Struct> int getSize(final Class<T> clazz) {
        final FieldInfo[] fieldInfos = getClassFieldInfo(clazz);
        return getSizeInternal(fieldInfos);
    }

    /**
     * Convert the parsed Struct subclass object to ByteBuffer.
     *
     * @param output ByteBuffer passed-in from the caller.
     */
    public final void writeToByteBuffer(final ByteBuffer output) {
        final FieldInfo[] fieldInfos = getClassFieldInfo(this.getClass());
        writeToByteBufferInternal(output, fieldInfos);
    }

    /**
     * Convert the parsed Struct subclass object to byte array.
     *
     * @param order indicate ByteBuffer is outputted as little-endian or big-endian.
     */
    public final byte[] writeToBytes(final ByteOrder order) {
        final FieldInfo[] fieldInfos = getClassFieldInfo(this.getClass());
        final byte[] output = new byte[getSizeInternal(fieldInfos)];
        final ByteBuffer buffer = ByteBuffer.wrap(output);
        buffer.order(order);
        writeToByteBufferInternal(buffer, fieldInfos);
        return output;
    }

    /** Convert the parsed Struct subclass object to byte array with native order. */
    public final byte[] writeToBytes() {
        return writeToBytes(ByteOrder.nativeOrder());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || this.getClass() != obj.getClass()) return false;

        final FieldInfo[] fieldInfos = getClassFieldInfo(this.getClass());
        for (int i = 0; i < fieldInfos.length; i++) {
            try {
                final Object value = fieldInfos[i].field.get(this);
                final Object otherValue = fieldInfos[i].field.get(obj);

                // Use Objects#deepEquals because the equals method on arrays does not check the
                // contents of the array. The only difference between Objects#deepEquals and
                // Objects#equals is that the former will call Arrays#deepEquals when comparing
                // arrays. In turn, the only difference between Arrays#deepEquals is that it
                // supports nested arrays. Struct does not currently support these, and if it did,
                // Objects#deepEquals might be more correct.
                if (!Objects.deepEquals(value, otherValue)) return false;
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot access field: " + fieldInfos[i].field, e);
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        final FieldInfo[] fieldInfos = getClassFieldInfo(this.getClass());
        final Object[] values = new Object[fieldInfos.length];
        for (int i = 0; i < fieldInfos.length; i++) {
            final Object value = getFieldValue(fieldInfos[i].field);
            // For byte array field, put the hash code generated based on the array content into
            // the Object array instead of the reference to byte array, which might change and cause
            // to get a different hash code even with the exact same elements.
            if (fieldInfos[i].field.getType() == byte[].class) {
                values[i] = Arrays.hashCode((byte[]) value);
            } else {
                values[i] = value;
            }
        }
        return Objects.hash(values);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final FieldInfo[] fieldInfos = getClassFieldInfo(this.getClass());
        for (int i = 0; i < fieldInfos.length; i++) {
            sb.append(fieldInfos[i].field.getName()).append(": ");
            final Object value = getFieldValue(fieldInfos[i].field);
            if (value == null) {
                sb.append("null");
            } else if (fieldInfos[i].annotation.type() == Type.ByteArray) {
                sb.append("0x").append(HexDump.toHexString((byte[]) value));
            } else if (fieldInfos[i].annotation.type() == Type.Ipv4Address
                    || fieldInfos[i].annotation.type() == Type.Ipv6Address) {
                sb.append(((InetAddress) value).getHostAddress());
            } else {
                sb.append(value.toString());
            }
            if (i != fieldInfos.length - 1) sb.append(", ");
        }
        return sb.toString();
    }

    /** A simple Struct which only contains a u8 field. */
    public static class U8 extends Struct {
        @Struct.Field(order = 0, type = Struct.Type.U8)
        public final short val;

        public U8(final short val) {
            this.val = val;
        }
    }

    public static class U32 extends Struct {
        @Struct.Field(order = 0, type = Struct.Type.U32)
        public final long val;

        public U32(final long val) {
            this.val = val;
        }
    }

    public static class S64 extends Struct {
        @Struct.Field(order = 0, type = Struct.Type.S64)
        public final long val;

        public S64(final long val) {
            this.val = val;
        }
    }
}
