/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import jdk.internal.HotSpotIntrinsicCandidate;

// BEGIN Android-added: Clarification for usage of StringUTF16 and not StringLatin1.
/**
 * In upstream, this class (StringUTF16) contains implementations of various APIs and their helper
 * methods for uncompressed strings. Libcore makes use of it to implement APIs for both compressed
 * and uncompressed strings.
 *
 * Upstream implementation also has StringLatin1 class which contains same implementations for
 * compressed strings (and this class is for uncompressed). In our case we use only StringUTF16
 * (and StringLatin1 is intentionally not imported) as string characters are managed by ART and
 * everything is implemented in terms of {@link String#charAt(int)} an other primitives which do not
 * have direct access to underlying character array. From those two, if we choose to implement
 * everything in those primitives, StringUTF16 has less difference with upstream, so it is chosen
 * here.
 *
 * @hide
 */
// END Android-added: Clarification for usage of StringUTF16 and not StringLatin1.
final class StringUTF16 {

    // BEGIN Android-changed: Pass String instead of byte[]; implement in terms of charAt().
    // @HotSpotIntrinsicCandidate
    // intrinsic performs no bounds checks
    /*
    static char getChar(byte[] val, int index) {
        assert index >= 0 && index < length(val) : "Trusted caller missed bounds check";
        index <<= 1;
        return (char)(((val[index++] & 0xff) << HI_BYTE_SHIFT) |
                      ((val[index]   & 0xff) << LO_BYTE_SHIFT));
     */
    @HotSpotIntrinsicCandidate
    static char getChar(String val, int index) {
        return val.charAt(index);
    }
    // END Android-changed: Pass String instead of byte[]; implement in terms of charAt().

    // BEGIN Android-changed: Pass String instead of byte[].
    /*
    public static int length(byte[] value) {
        return value.length >> 1;
     */
    public static int length(String value) {
        return value.length();
    // END Android-changed: Pass String instead of byte[].
    }

    // BEGIN Android-changed: Pass String instead of byte[].
    /*
    public static int indexOfNonWhitespace(byte[] value) {
        int length = value.length >> 1;
     */
    public static int indexOfNonWhitespace(String value) {
        int length = value.length();
        int left = 0;
        while (left < length) {
            /*
            int codepoint = codePointAt(value, left, length);
             */
            int codepoint = value.codePointAt(left);
    // END Android-changed: Pass String instead of byte[].
            if (codepoint != ' ' && codepoint != '\t' && !Character.isWhitespace(codepoint)) {
                break;
            }
            left += Character.charCount(codepoint);
        }
        return left;
    }

    // BEGIN Android-changed: Pass String instead of byte[].
    /*
    public static int lastIndexOfNonWhitespace(byte[] value) {
        int length = value.length >> 1;
        int right = length;
     */
    public static int lastIndexOfNonWhitespace(String value) {
        int right = value.length();
        while (0 < right) {
            /*
            int codepoint = codePointBefore(value, right);
             */
            int codepoint = value.codePointBefore(right);
    // END Android-changed: Pass String instead of byte[].
            if (codepoint != ' ' && codepoint != '\t' && !Character.isWhitespace(codepoint)) {
                break;
            }
            right -= Character.charCount(codepoint);
        }
        return right;
    }

    // BEGIN Android-changed: Pass String instead of byte[].
    /*
    public static String strip(byte[] value) {
        int length = value.length >> 1;
     */
    public static String strip(String value) {
        int length = value.length();
    // END Android-changed: Pass String instead of byte[].
        int left = indexOfNonWhitespace(value);
        if (left == length) {
            return "";
        }
        int right = lastIndexOfNonWhitespace(value);
        return ((left > 0) || (right < length)) ? newString(value, left, right - left) : null;
    }

    // BEGIN Android-changed: Pass String instead of byte[].
    /*
    public static String stripLeading(byte[] value) {
        int length = value.length >> 1;
     */
    public static String stripLeading(String value) {
        int length = value.length();
    // END Android-changed: Pass String instead of byte[].
        int left = indexOfNonWhitespace(value);
        if (left == length) {
            return "";
        }
        return (left != 0) ? newString(value, left, length - left) : null;
    }

    // BEGIN Android-changed: Pass String instead of byte[].
    /*
    public static String stripTrailing(byte[] value) {
        int length = value.length >> 1;
     */
    public static String stripTrailing(String value) {
        int length = value.length();
    // END Android-changed: Pass String instead of byte[].
        int right = lastIndexOfNonWhitespace(value);
        if (right == 0) {
            return "";
        }
        return (right != length) ? newString(value, 0, right) : null;
    }

    private final static class LinesSpliterator implements Spliterator<String> {
        // BEGIN Android-changed: Pass String instead of byte[].
        /*
        private byte[] value;
         */
        private String value;
        // END Android-changed: Pass String instead of byte[].
        private int index;        // current index, modified on advance/split
        private final int fence;  // one past last index

        // BEGIN Android-changed: Pass String instead of byte[].
        /*
        LinesSpliterator(byte[] value) {
            this(value, 0, value.length >>> 1);
        */
        LinesSpliterator(String value) {
            this(value, 0, value.length());
        // END Android-changed: Pass String instead of byte[].
        }

        // BEGIN Android-changed: Pass String instead of byte[].
        /*
        LinesSpliterator(byte[] value, int start, int length) {
         */
        LinesSpliterator(String value, int start, int length) {
        // END Android-changed: Pass String instead of byte[].
            this.value = value;
            this.index = start;
            this.fence = start + length;
        }

        private int indexOfLineSeparator(int start) {
            for (int current = start; current < fence; current++) {
                char ch = getChar(value, current);
                if (ch == '\n' || ch == '\r') {
                    return current;
                }
            }
            return fence;
        }

        private int skipLineSeparator(int start) {
            if (start < fence) {
                if (getChar(value, start) == '\r') {
                    int next = start + 1;
                    if (next < fence && getChar(value, next) == '\n') {
                        return next + 1;
                    }
                }
                return start + 1;
            }
            return fence;
        }

        private String next() {
            int start = index;
            int end = indexOfLineSeparator(start);
            index = skipLineSeparator(end);
            return newString(value, start, end - start);
        }

        @Override
        public boolean tryAdvance(Consumer<? super String> action) {
            if (action == null) {
                throw new NullPointerException("tryAdvance action missing");
            }
            if (index != fence) {
                action.accept(next());
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(Consumer<? super String> action) {
            if (action == null) {
                throw new NullPointerException("forEachRemaining action missing");
            }
            while (index != fence) {
                action.accept(next());
            }
        }

        @Override
        public Spliterator<String> trySplit() {
            int half = (fence + index) >>> 1;
            int mid = skipLineSeparator(indexOfLineSeparator(half));
            if (mid < fence) {
                int start = index;
                index = mid;
                return new LinesSpliterator(value, start, mid - start);
            }
            return null;
        }

        @Override
        public long estimateSize() {
            return fence - index + 1;
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL;
        }
    }

    // BEGIN Android-changed: Pass String instead of byte[].
    /*
    static Stream<String> lines(byte[] value) {
     */
    static Stream<String> lines(String value) {
        return StreamSupport.stream(new LinesSpliterator(value), false);
    // END Android-changed: Pass String instead of byte[].
    }

    // BEGIN Android-changed: Pass String instead of byte[]; implement in terms of substring().
    /*
    public static String newString(byte[] val, int index, int len) {
        if (String.COMPACT_STRINGS) {
            byte[] buf = compress(val, index, len);
            if (buf != null) {
                return new String(buf, LATIN1);
            }
        }
        int last = index + len;
        return new String(Arrays.copyOfRange(val, index << 1, last << 1), UTF16);
    }
     */
    public static String newString(String val, int index, int len) {
        return val.substring(index, index + len);
    }
    // END Android-changed: Pass String instead of byte[]; implement in terms of substring().

    static class CharsSpliterator implements Spliterator.OfInt {
        // BEGIN Android-changed: Pass String instead of byte[].
        /*
        private final byte[] array;
         */
        private final String array;
        // END Android-changed: Pass String instead of byte[].
        private int index;        // current index, modified on advance/split
        private final int fence;  // one past last index
        private final int cs;

        // BEGIN Android-changed: Pass String instead of byte[].
        /*
        CharsSpliterator(byte[] array, int acs) {
            this(array, 0, array.length >> 1, acs);
         */
        CharsSpliterator(String array, int acs) {
            this(array, 0, array.length(), acs);
        // END Android-changed: Pass String instead of byte[].
        }

        // BEGIN Android-changed: Pass String instead of byte[].
        /*
        CharsSpliterator(byte[] array, int origin, int fence, int acs) {
         */
        CharsSpliterator(String array, int origin, int fence, int acs) {
        // END Android-changed: Pass String instead of byte[].
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.cs = acs | Spliterator.ORDERED | Spliterator.SIZED
                      | Spliterator.SUBSIZED;
        }

        @Override
        public OfInt trySplit() {
            int lo = index, mid = (lo + fence) >>> 1;
            return (lo >= mid)
                   ? null
                   : new CharsSpliterator(array, lo, index = mid, cs);
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            // BEGIN Android-changed: Pass String instead of byte[].
            /*
            byte[] a; int i, hi; // hoist accesses and checks from loop
             */
            String a; int i, hi; // hoist accesses and checks from loop
            // END Android-changed: Pass String instead of byte[].
            if (action == null)
                throw new NullPointerException();
            // BEGIN Android-changed: Pass String instead of byte[].
            /*
            if (((a = array).length >> 1) >= (hi = fence) &&
             */
            if (((a = array).length()) >= (hi = fence) &&
            // END Android-changed: Pass String instead of byte[].
                (i = index) >= 0 && i < (index = hi)) {
                do {
                    action.accept(charAt(a, i));
                } while (++i < hi);
            }
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (action == null)
                throw new NullPointerException();
            int i = index;
            if (i >= 0 && i < fence) {
                action.accept(charAt(array, i));
                index++;
                return true;
            }
            return false;
        }

        @Override
        public long estimateSize() { return (long)(fence - index); }

        @Override
        public int characteristics() {
            return cs;
        }
    }

    static class CodePointsSpliterator implements Spliterator.OfInt {
        // BEGIN Android-changed: Pass String instead of byte[].
        /*
        private final byte[] array;
         */
        private final String array;
        // END Android-changed: Pass String instead of byte[].
        private int index;        // current index, modified on advance/split
        private final int fence;  // one past last index
        private final int cs;

        // BEGIN Android-changed: Pass String instead of byte[].
        /*
        CodePointsSpliterator(byte[] array, int acs) {
            this(array, 0, array.length >> 1, acs);
         */
        CodePointsSpliterator(String array, int acs) {
            this(array, 0, array.length(), acs);
        // END Android-changed: Pass String instead of byte[].
        }

        // BEGIN Android-changed: Pass String instead of byte[].
        /*
        CodePointsSpliterator(byte[] array, int origin, int fence, int acs) {
         */
        CodePointsSpliterator(String array, int origin, int fence, int acs) {
        // END Android-changed: Pass String instead of byte[].
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.cs = acs | Spliterator.ORDERED;
        }

        @Override
        public OfInt trySplit() {
            int lo = index, mid = (lo + fence) >>> 1;
            if (lo >= mid)
                return null;

            int midOneLess;
            // If the mid-point intersects a surrogate pair
            if (Character.isLowSurrogate(charAt(array, mid)) &&
                Character.isHighSurrogate(charAt(array, midOneLess = (mid -1)))) {
                // If there is only one pair it cannot be split
                if (lo >= midOneLess)
                    return null;
                // Shift the mid-point to align with the surrogate pair
                return new CodePointsSpliterator(array, lo, index = midOneLess, cs);
            }
            return new CodePointsSpliterator(array, lo, index = mid, cs);
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            // BEGIN Android-changed: Pass String instead of byte[].
            /*
            byte[] a; int i, hi; // hoist accesses and checks from loop
             */
            String a; int i, hi; // hoist accesses and checks from loop
            // END Android-changed: Pass String instead of byte[].
            if (action == null)
                throw new NullPointerException();
            // BEGIN Android-changed: Pass String instead of byte[].
            /*
            if (((a = array).length >> 1) >= (hi = fence) &&
             */
            if (((a = array).length()) >= (hi = fence) &&
            // END Android-changed: Pass String instead of byte[].
                (i = index) >= 0 && i < (index = hi)) {
                do {
                    i = advance(a, i, hi, action);
                } while (i < hi);
            }
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (action == null)
                throw new NullPointerException();
            if (index >= 0 && index < fence) {
                index = advance(array, index, fence, action);
                return true;
            }
            return false;
        }

        // Advance one code point from the index, i, and return the next
        // index to advance from
        // BEGIN Android-changed: Pass String instead of byte[].
        /*
        private static int advance(byte[] a, int i, int hi, IntConsumer action) {
         */
        private static int advance(String a, int i, int hi, IntConsumer action) {
        // END Android-changed: Pass String instead of byte[].
            char c1 = charAt(a, i++);
            int cp = c1;
            if (Character.isHighSurrogate(c1) && i < hi) {
                char c2 = charAt(a, i);
                if (Character.isLowSurrogate(c2)) {
                    i++;
                    cp = Character.toCodePoint(c1, c2);
                }
            }
            action.accept(cp);
            return i;
        }

        @Override
        public long estimateSize() { return (long)(fence - index); }

        @Override
        public int characteristics() {
            return cs;
        }
    }

    // BEGIN Android-changed: Pass String instead of byte[].
    /*
    public static char charAt(byte[] value, int index) {
     */
    public static char charAt(String value, int index) {
    // END Android-changed: Pass String instead of byte[].
        checkIndex(index, value);
        return getChar(value, index);
    }

    // BEGIN Android-changed: Pass String instead of byte[].
    /*
    public static void checkIndex(int off, byte[] val) {
     */
    public static void checkIndex(int off, String val) {
    // END Android-changed: Pass String instead of byte[].
        String.checkIndex(off, length(val));
    }

}
