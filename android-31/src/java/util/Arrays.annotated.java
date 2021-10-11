/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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


package java.util;

import java.lang.reflect.Array;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.stream.Stream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.DoubleStream;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class Arrays {

Arrays() { throw new RuntimeException("Stub!"); }

public static void sort(int @libcore.util.NonNull [] a) { throw new RuntimeException("Stub!"); }

public static void sort(int @libcore.util.NonNull [] a, int fromIndex, int toIndex) { throw new RuntimeException("Stub!"); }

public static void sort(long @libcore.util.NonNull [] a) { throw new RuntimeException("Stub!"); }

public static void sort(long @libcore.util.NonNull [] a, int fromIndex, int toIndex) { throw new RuntimeException("Stub!"); }

public static void sort(short @libcore.util.NonNull [] a) { throw new RuntimeException("Stub!"); }

public static void sort(short @libcore.util.NonNull [] a, int fromIndex, int toIndex) { throw new RuntimeException("Stub!"); }

public static void sort(char @libcore.util.NonNull [] a) { throw new RuntimeException("Stub!"); }

public static void sort(char @libcore.util.NonNull [] a, int fromIndex, int toIndex) { throw new RuntimeException("Stub!"); }

public static void sort(byte @libcore.util.NonNull [] a) { throw new RuntimeException("Stub!"); }

public static void sort(byte @libcore.util.NonNull [] a, int fromIndex, int toIndex) { throw new RuntimeException("Stub!"); }

public static void sort(float @libcore.util.NonNull [] a) { throw new RuntimeException("Stub!"); }

public static void sort(float @libcore.util.NonNull [] a, int fromIndex, int toIndex) { throw new RuntimeException("Stub!"); }

public static void sort(double @libcore.util.NonNull [] a) { throw new RuntimeException("Stub!"); }

public static void sort(double @libcore.util.NonNull [] a, int fromIndex, int toIndex) { throw new RuntimeException("Stub!"); }

public static void parallelSort(byte @libcore.util.NonNull [] a) { throw new RuntimeException("Stub!"); }

public static void parallelSort(byte @libcore.util.NonNull [] a, int fromIndex, int toIndex) { throw new RuntimeException("Stub!"); }

public static void parallelSort(char @libcore.util.NonNull [] a) { throw new RuntimeException("Stub!"); }

public static void parallelSort(char @libcore.util.NonNull [] a, int fromIndex, int toIndex) { throw new RuntimeException("Stub!"); }

public static void parallelSort(short @libcore.util.NonNull [] a) { throw new RuntimeException("Stub!"); }

public static void parallelSort(short @libcore.util.NonNull [] a, int fromIndex, int toIndex) { throw new RuntimeException("Stub!"); }

public static void parallelSort(int @libcore.util.NonNull [] a) { throw new RuntimeException("Stub!"); }

public static void parallelSort(int @libcore.util.NonNull [] a, int fromIndex, int toIndex) { throw new RuntimeException("Stub!"); }

public static void parallelSort(long @libcore.util.NonNull [] a) { throw new RuntimeException("Stub!"); }

public static void parallelSort(long @libcore.util.NonNull [] a, int fromIndex, int toIndex) { throw new RuntimeException("Stub!"); }

public static void parallelSort(float @libcore.util.NonNull [] a) { throw new RuntimeException("Stub!"); }

public static void parallelSort(float @libcore.util.NonNull [] a, int fromIndex, int toIndex) { throw new RuntimeException("Stub!"); }

public static void parallelSort(double @libcore.util.NonNull [] a) { throw new RuntimeException("Stub!"); }

public static void parallelSort(double @libcore.util.NonNull [] a, int fromIndex, int toIndex) { throw new RuntimeException("Stub!"); }

public static <T extends java.lang.Comparable<? super T>> void parallelSort(T @libcore.util.NonNull [] a) { throw new RuntimeException("Stub!"); }

public static <T extends java.lang.Comparable<? super T>> void parallelSort(T @libcore.util.NonNull [] a, int fromIndex, int toIndex) { throw new RuntimeException("Stub!"); }

public static <T> void parallelSort(T @libcore.util.NonNull [] a, @libcore.util.Nullable java.util.Comparator<? super @libcore.util.NullFromTypeParam T> cmp) { throw new RuntimeException("Stub!"); }

public static <T> void parallelSort(T @libcore.util.NonNull [] a, int fromIndex, int toIndex, @libcore.util.Nullable java.util.Comparator<? super @libcore.util.NullFromTypeParam T> cmp) { throw new RuntimeException("Stub!"); }

public static void sort(java.lang.@libcore.util.NonNull Object @libcore.util.NonNull [] a) { throw new RuntimeException("Stub!"); }

public static void sort(java.lang.@libcore.util.NonNull Object @libcore.util.NonNull [] a, int fromIndex, int toIndex) { throw new RuntimeException("Stub!"); }

public static <T> void sort(T @libcore.util.NonNull [] a, @libcore.util.Nullable java.util.Comparator<? super @libcore.util.NullFromTypeParam T> c) { throw new RuntimeException("Stub!"); }

public static <T> void sort(T @libcore.util.NonNull [] a, int fromIndex, int toIndex, @libcore.util.Nullable java.util.Comparator<? super @libcore.util.NullFromTypeParam T> c) { throw new RuntimeException("Stub!"); }

public static <T> void parallelPrefix(T @libcore.util.NonNull [] array, @libcore.util.NonNull java.util.function.BinaryOperator<@libcore.util.NullFromTypeParam T> op) { throw new RuntimeException("Stub!"); }

public static <T> void parallelPrefix(T @libcore.util.NonNull [] array, int fromIndex, int toIndex, @libcore.util.NonNull java.util.function.BinaryOperator<@libcore.util.NullFromTypeParam T> op) { throw new RuntimeException("Stub!"); }

public static void parallelPrefix(long @libcore.util.NonNull [] array, @libcore.util.NonNull java.util.function.LongBinaryOperator op) { throw new RuntimeException("Stub!"); }

public static void parallelPrefix(long @libcore.util.NonNull [] array, int fromIndex, int toIndex, @libcore.util.NonNull java.util.function.LongBinaryOperator op) { throw new RuntimeException("Stub!"); }

public static void parallelPrefix(double @libcore.util.NonNull [] array, @libcore.util.NonNull java.util.function.DoubleBinaryOperator op) { throw new RuntimeException("Stub!"); }

public static void parallelPrefix(double @libcore.util.NonNull [] array, int fromIndex, int toIndex, @libcore.util.NonNull java.util.function.DoubleBinaryOperator op) { throw new RuntimeException("Stub!"); }

public static void parallelPrefix(int @libcore.util.NonNull [] array, @libcore.util.NonNull java.util.function.IntBinaryOperator op) { throw new RuntimeException("Stub!"); }

public static void parallelPrefix(int @libcore.util.NonNull [] array, int fromIndex, int toIndex, @libcore.util.NonNull java.util.function.IntBinaryOperator op) { throw new RuntimeException("Stub!"); }

public static int binarySearch(long @libcore.util.NonNull [] a, long key) { throw new RuntimeException("Stub!"); }

public static int binarySearch(long @libcore.util.NonNull [] a, int fromIndex, int toIndex, long key) { throw new RuntimeException("Stub!"); }

public static int binarySearch(int @libcore.util.NonNull [] a, int key) { throw new RuntimeException("Stub!"); }

public static int binarySearch(int @libcore.util.NonNull [] a, int fromIndex, int toIndex, int key) { throw new RuntimeException("Stub!"); }

public static int binarySearch(short @libcore.util.NonNull [] a, short key) { throw new RuntimeException("Stub!"); }

public static int binarySearch(short @libcore.util.NonNull [] a, int fromIndex, int toIndex, short key) { throw new RuntimeException("Stub!"); }

public static int binarySearch(char @libcore.util.NonNull [] a, char key) { throw new RuntimeException("Stub!"); }

public static int binarySearch(char @libcore.util.NonNull [] a, int fromIndex, int toIndex, char key) { throw new RuntimeException("Stub!"); }

public static int binarySearch(byte @libcore.util.NonNull [] a, byte key) { throw new RuntimeException("Stub!"); }

public static int binarySearch(byte @libcore.util.NonNull [] a, int fromIndex, int toIndex, byte key) { throw new RuntimeException("Stub!"); }

public static int binarySearch(double @libcore.util.NonNull [] a, double key) { throw new RuntimeException("Stub!"); }

public static int binarySearch(double @libcore.util.NonNull [] a, int fromIndex, int toIndex, double key) { throw new RuntimeException("Stub!"); }

public static int binarySearch(float @libcore.util.NonNull [] a, float key) { throw new RuntimeException("Stub!"); }

public static int binarySearch(float @libcore.util.NonNull [] a, int fromIndex, int toIndex, float key) { throw new RuntimeException("Stub!"); }

public static int binarySearch(java.lang.@libcore.util.NonNull Object @libcore.util.NonNull [] a, @libcore.util.NonNull java.lang.Object key) { throw new RuntimeException("Stub!"); }

public static int binarySearch(java.lang.@libcore.util.NonNull Object @libcore.util.NonNull [] a, int fromIndex, int toIndex, @libcore.util.NonNull java.lang.Object key) { throw new RuntimeException("Stub!"); }

public static <T> int binarySearch(T @libcore.util.NonNull [] a, @libcore.util.NullFromTypeParam T key, @libcore.util.Nullable java.util.Comparator<? super @libcore.util.NullFromTypeParam T> c) { throw new RuntimeException("Stub!"); }

public static <T> int binarySearch(T @libcore.util.NonNull [] a, int fromIndex, int toIndex, @libcore.util.NullFromTypeParam T key, @libcore.util.Nullable java.util.Comparator<? super @libcore.util.NullFromTypeParam T> c) { throw new RuntimeException("Stub!"); }

public static boolean equals(long @libcore.util.Nullable [] a, long @libcore.util.Nullable [] a2) { throw new RuntimeException("Stub!"); }

public static boolean equals(int @libcore.util.Nullable [] a, int @libcore.util.Nullable [] a2) { throw new RuntimeException("Stub!"); }

public static boolean equals(short @libcore.util.Nullable [] a, short @libcore.util.Nullable [] a2) { throw new RuntimeException("Stub!"); }

public static boolean equals(char @libcore.util.Nullable [] a, char @libcore.util.Nullable [] a2) { throw new RuntimeException("Stub!"); }

public static boolean equals(byte @libcore.util.Nullable [] a, byte @libcore.util.Nullable [] a2) { throw new RuntimeException("Stub!"); }

public static boolean equals(boolean @libcore.util.Nullable [] a, boolean @libcore.util.Nullable [] a2) { throw new RuntimeException("Stub!"); }

public static boolean equals(double @libcore.util.Nullable [] a, double @libcore.util.Nullable [] a2) { throw new RuntimeException("Stub!"); }

public static boolean equals(float @libcore.util.Nullable [] a, float @libcore.util.Nullable [] a2) { throw new RuntimeException("Stub!"); }

public static boolean equals(java.lang.@libcore.util.Nullable Object @libcore.util.Nullable [] a, java.lang.@libcore.util.Nullable Object @libcore.util.Nullable [] a2) { throw new RuntimeException("Stub!"); }

public static void fill(long @libcore.util.NonNull [] a, long val) { throw new RuntimeException("Stub!"); }

public static void fill(long @libcore.util.NonNull [] a, int fromIndex, int toIndex, long val) { throw new RuntimeException("Stub!"); }

public static void fill(int @libcore.util.NonNull [] a, int val) { throw new RuntimeException("Stub!"); }

public static void fill(int @libcore.util.NonNull [] a, int fromIndex, int toIndex, int val) { throw new RuntimeException("Stub!"); }

public static void fill(short @libcore.util.NonNull [] a, short val) { throw new RuntimeException("Stub!"); }

public static void fill(short @libcore.util.NonNull [] a, int fromIndex, int toIndex, short val) { throw new RuntimeException("Stub!"); }

public static void fill(char @libcore.util.NonNull [] a, char val) { throw new RuntimeException("Stub!"); }

public static void fill(char @libcore.util.NonNull [] a, int fromIndex, int toIndex, char val) { throw new RuntimeException("Stub!"); }

public static void fill(byte @libcore.util.NonNull [] a, byte val) { throw new RuntimeException("Stub!"); }

public static void fill(byte @libcore.util.NonNull [] a, int fromIndex, int toIndex, byte val) { throw new RuntimeException("Stub!"); }

public static void fill(boolean @libcore.util.NonNull [] a, boolean val) { throw new RuntimeException("Stub!"); }

public static void fill(boolean @libcore.util.NonNull [] a, int fromIndex, int toIndex, boolean val) { throw new RuntimeException("Stub!"); }

public static void fill(double @libcore.util.NonNull [] a, double val) { throw new RuntimeException("Stub!"); }

public static void fill(double @libcore.util.NonNull [] a, int fromIndex, int toIndex, double val) { throw new RuntimeException("Stub!"); }

public static void fill(float @libcore.util.NonNull [] a, float val) { throw new RuntimeException("Stub!"); }

public static void fill(float @libcore.util.NonNull [] a, int fromIndex, int toIndex, float val) { throw new RuntimeException("Stub!"); }

public static void fill(java.lang.@libcore.util.Nullable Object @libcore.util.NonNull [] a, @libcore.util.Nullable java.lang.Object val) { throw new RuntimeException("Stub!"); }

public static void fill(java.lang.@libcore.util.Nullable Object @libcore.util.NonNull [] a, int fromIndex, int toIndex, @libcore.util.Nullable java.lang.Object val) { throw new RuntimeException("Stub!"); }

public static <T> T @libcore.util.NonNull [] copyOf(T @libcore.util.NonNull [] original, int newLength) { throw new RuntimeException("Stub!"); }

public static <T, U> T @libcore.util.NonNull [] copyOf(U @libcore.util.NonNull [] original, int newLength, @libcore.util.NonNull java.lang.Class<? extends T[]> newType) { throw new RuntimeException("Stub!"); }

public static byte @libcore.util.NonNull [] copyOf(byte @libcore.util.NonNull [] original, int newLength) { throw new RuntimeException("Stub!"); }

public static short @libcore.util.NonNull [] copyOf(short @libcore.util.NonNull [] original, int newLength) { throw new RuntimeException("Stub!"); }

public static int @libcore.util.NonNull [] copyOf(int @libcore.util.NonNull [] original, int newLength) { throw new RuntimeException("Stub!"); }

public static long @libcore.util.NonNull [] copyOf(long @libcore.util.NonNull [] original, int newLength) { throw new RuntimeException("Stub!"); }

public static char @libcore.util.NonNull [] copyOf(char @libcore.util.NonNull [] original, int newLength) { throw new RuntimeException("Stub!"); }

public static float @libcore.util.NonNull [] copyOf(float @libcore.util.NonNull [] original, int newLength) { throw new RuntimeException("Stub!"); }

public static double @libcore.util.NonNull [] copyOf(double @libcore.util.NonNull [] original, int newLength) { throw new RuntimeException("Stub!"); }

public static boolean @libcore.util.NonNull [] copyOf(boolean @libcore.util.NonNull [] original, int newLength) { throw new RuntimeException("Stub!"); }

public static <T> T @libcore.util.NonNull [] copyOfRange(T @libcore.util.NonNull [] original, int from, int to) { throw new RuntimeException("Stub!"); }

public static <T, U> T @libcore.util.NonNull [] copyOfRange(U @libcore.util.NonNull [] original, int from, int to, @libcore.util.NonNull java.lang.Class<? extends T[]> newType) { throw new RuntimeException("Stub!"); }

public static byte @libcore.util.NonNull [] copyOfRange(byte @libcore.util.NonNull [] original, int from, int to) { throw new RuntimeException("Stub!"); }

public static short @libcore.util.NonNull [] copyOfRange(short @libcore.util.NonNull [] original, int from, int to) { throw new RuntimeException("Stub!"); }

public static int @libcore.util.NonNull [] copyOfRange(int @libcore.util.NonNull [] original, int from, int to) { throw new RuntimeException("Stub!"); }

public static long @libcore.util.NonNull [] copyOfRange(long @libcore.util.NonNull [] original, int from, int to) { throw new RuntimeException("Stub!"); }

public static char @libcore.util.NonNull [] copyOfRange(char @libcore.util.NonNull [] original, int from, int to) { throw new RuntimeException("Stub!"); }

public static float @libcore.util.NonNull [] copyOfRange(float @libcore.util.NonNull [] original, int from, int to) { throw new RuntimeException("Stub!"); }

public static double @libcore.util.NonNull [] copyOfRange(double @libcore.util.NonNull [] original, int from, int to) { throw new RuntimeException("Stub!"); }

public static boolean @libcore.util.NonNull [] copyOfRange(boolean @libcore.util.NonNull [] original, int from, int to) { throw new RuntimeException("Stub!"); }

@java.lang.SafeVarargs
@libcore.util.NonNull public static <T> java.util.List<@libcore.util.NullFromTypeParam T> asList(T @libcore.util.NonNull ... a) { throw new RuntimeException("Stub!"); }

public static int hashCode(long @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

public static int hashCode(int @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

public static int hashCode(short @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

public static int hashCode(char @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

public static int hashCode(byte @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

public static int hashCode(boolean @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

public static int hashCode(float @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

public static int hashCode(double @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

public static int hashCode(java.lang.@libcore.util.Nullable Object @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

public static int deepHashCode(java.lang.@libcore.util.Nullable Object @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

public static boolean deepEquals(java.lang.@libcore.util.Nullable Object @libcore.util.Nullable [] a1, java.lang.@libcore.util.Nullable Object @libcore.util.Nullable [] a2) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String toString(long @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String toString(int @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String toString(short @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String toString(char @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String toString(byte @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String toString(boolean @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String toString(float @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String toString(double @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String toString(java.lang.@libcore.util.Nullable Object @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String deepToString(java.lang.@libcore.util.Nullable Object @libcore.util.Nullable [] a) { throw new RuntimeException("Stub!"); }

public static <T> void setAll(T @libcore.util.NonNull [] array, @libcore.util.NonNull java.util.function.IntFunction<? extends @libcore.util.NullFromTypeParam T> generator) { throw new RuntimeException("Stub!"); }

public static <T> void parallelSetAll(T @libcore.util.NonNull [] array, @libcore.util.NonNull java.util.function.IntFunction<? extends @libcore.util.NullFromTypeParam T> generator) { throw new RuntimeException("Stub!"); }

public static void setAll(int @libcore.util.NonNull [] array, @libcore.util.NonNull java.util.function.IntUnaryOperator generator) { throw new RuntimeException("Stub!"); }

public static void parallelSetAll(int @libcore.util.NonNull [] array, @libcore.util.NonNull java.util.function.IntUnaryOperator generator) { throw new RuntimeException("Stub!"); }

public static void setAll(long @libcore.util.NonNull [] array, @libcore.util.NonNull java.util.function.IntToLongFunction generator) { throw new RuntimeException("Stub!"); }

public static void parallelSetAll(long @libcore.util.NonNull [] array, @libcore.util.NonNull java.util.function.IntToLongFunction generator) { throw new RuntimeException("Stub!"); }

public static void setAll(double @libcore.util.NonNull [] array, @libcore.util.NonNull java.util.function.IntToDoubleFunction generator) { throw new RuntimeException("Stub!"); }

public static void parallelSetAll(double @libcore.util.NonNull [] array, @libcore.util.NonNull java.util.function.IntToDoubleFunction generator) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.Spliterator<@libcore.util.NullFromTypeParam T> spliterator(T @libcore.util.NonNull [] array) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.Spliterator<@libcore.util.NullFromTypeParam T> spliterator(T @libcore.util.NonNull [] array, int startInclusive, int endExclusive) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.Spliterator.OfInt spliterator(int @libcore.util.NonNull [] array) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.Spliterator.OfInt spliterator(int @libcore.util.NonNull [] array, int startInclusive, int endExclusive) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.Spliterator.OfLong spliterator(long @libcore.util.NonNull [] array) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.Spliterator.OfLong spliterator(long @libcore.util.NonNull [] array, int startInclusive, int endExclusive) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.Spliterator.OfDouble spliterator(double @libcore.util.NonNull [] array) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.Spliterator.OfDouble spliterator(double @libcore.util.NonNull [] array, int startInclusive, int endExclusive) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.stream.Stream<@libcore.util.NullFromTypeParam T> stream(T @libcore.util.NonNull [] array) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.stream.Stream<@libcore.util.NullFromTypeParam T> stream(T @libcore.util.NonNull [] array, int startInclusive, int endExclusive) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.stream.IntStream stream(int @libcore.util.NonNull [] array) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.stream.IntStream stream(int @libcore.util.NonNull [] array, int startInclusive, int endExclusive) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.stream.LongStream stream(long @libcore.util.NonNull [] array) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.stream.LongStream stream(long @libcore.util.NonNull [] array, int startInclusive, int endExclusive) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.stream.DoubleStream stream(double @libcore.util.NonNull [] array) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.stream.DoubleStream stream(double @libcore.util.NonNull [] array, int startInclusive, int endExclusive) { throw new RuntimeException("Stub!"); }
}
