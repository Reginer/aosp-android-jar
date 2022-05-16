/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */


package java.util.concurrent;

import java.util.function.Supplier;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class CompletableFuture<T> implements java.util.concurrent.Future<T>, java.util.concurrent.CompletionStage<T> {

public CompletableFuture() { throw new RuntimeException("Stub!"); }

public static <U> java.util.concurrent.CompletableFuture<U> supplyAsync(java.util.function.Supplier<U> supplier) { throw new RuntimeException("Stub!"); }

public static <U> java.util.concurrent.CompletableFuture<U> supplyAsync(java.util.function.Supplier<U> supplier, java.util.concurrent.Executor executor) { throw new RuntimeException("Stub!"); }

public static java.util.concurrent.CompletableFuture<java.lang.Void> runAsync(java.lang.Runnable runnable) { throw new RuntimeException("Stub!"); }

public static java.util.concurrent.CompletableFuture<java.lang.Void> runAsync(java.lang.Runnable runnable, java.util.concurrent.Executor executor) { throw new RuntimeException("Stub!"); }

public static <U> java.util.concurrent.CompletableFuture<U> completedFuture(U value) { throw new RuntimeException("Stub!"); }

public boolean isDone() { throw new RuntimeException("Stub!"); }

public T get() throws java.util.concurrent.ExecutionException, java.lang.InterruptedException { throw new RuntimeException("Stub!"); }

public T get(long timeout, java.util.concurrent.TimeUnit unit) throws java.util.concurrent.ExecutionException, java.lang.InterruptedException, java.util.concurrent.TimeoutException { throw new RuntimeException("Stub!"); }

public T join() { throw new RuntimeException("Stub!"); }

public T getNow(T valueIfAbsent) { throw new RuntimeException("Stub!"); }

public boolean complete(T value) { throw new RuntimeException("Stub!"); }

public boolean completeExceptionally(java.lang.Throwable ex) { throw new RuntimeException("Stub!"); }

public <U> java.util.concurrent.CompletableFuture<U> thenApply(java.util.function.Function<? super T,? extends U> fn) { throw new RuntimeException("Stub!"); }

public <U> java.util.concurrent.CompletableFuture<U> thenApplyAsync(java.util.function.Function<? super T,? extends U> fn) { throw new RuntimeException("Stub!"); }

public <U> java.util.concurrent.CompletableFuture<U> thenApplyAsync(java.util.function.Function<? super T,? extends U> fn, java.util.concurrent.Executor executor) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<java.lang.Void> thenAccept(java.util.function.Consumer<? super T> action) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<java.lang.Void> thenAcceptAsync(java.util.function.Consumer<? super T> action) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<java.lang.Void> thenAcceptAsync(java.util.function.Consumer<? super T> action, java.util.concurrent.Executor executor) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<java.lang.Void> thenRun(java.lang.Runnable action) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<java.lang.Void> thenRunAsync(java.lang.Runnable action) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<java.lang.Void> thenRunAsync(java.lang.Runnable action, java.util.concurrent.Executor executor) { throw new RuntimeException("Stub!"); }

public <U, V> java.util.concurrent.CompletableFuture<V> thenCombine(java.util.concurrent.CompletionStage<? extends U> other, java.util.function.BiFunction<? super T,? super U,? extends V> fn) { throw new RuntimeException("Stub!"); }

public <U, V> java.util.concurrent.CompletableFuture<V> thenCombineAsync(java.util.concurrent.CompletionStage<? extends U> other, java.util.function.BiFunction<? super T,? super U,? extends V> fn) { throw new RuntimeException("Stub!"); }

public <U, V> java.util.concurrent.CompletableFuture<V> thenCombineAsync(java.util.concurrent.CompletionStage<? extends U> other, java.util.function.BiFunction<? super T,? super U,? extends V> fn, java.util.concurrent.Executor executor) { throw new RuntimeException("Stub!"); }

public <U> java.util.concurrent.CompletableFuture<java.lang.Void> thenAcceptBoth(java.util.concurrent.CompletionStage<? extends U> other, java.util.function.BiConsumer<? super T,? super U> action) { throw new RuntimeException("Stub!"); }

public <U> java.util.concurrent.CompletableFuture<java.lang.Void> thenAcceptBothAsync(java.util.concurrent.CompletionStage<? extends U> other, java.util.function.BiConsumer<? super T,? super U> action) { throw new RuntimeException("Stub!"); }

public <U> java.util.concurrent.CompletableFuture<java.lang.Void> thenAcceptBothAsync(java.util.concurrent.CompletionStage<? extends U> other, java.util.function.BiConsumer<? super T,? super U> action, java.util.concurrent.Executor executor) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<java.lang.Void> runAfterBoth(java.util.concurrent.CompletionStage<?> other, java.lang.Runnable action) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<java.lang.Void> runAfterBothAsync(java.util.concurrent.CompletionStage<?> other, java.lang.Runnable action) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<java.lang.Void> runAfterBothAsync(java.util.concurrent.CompletionStage<?> other, java.lang.Runnable action, java.util.concurrent.Executor executor) { throw new RuntimeException("Stub!"); }

public <U> java.util.concurrent.CompletableFuture<U> applyToEither(java.util.concurrent.CompletionStage<? extends T> other, java.util.function.Function<? super T,U> fn) { throw new RuntimeException("Stub!"); }

public <U> java.util.concurrent.CompletableFuture<U> applyToEitherAsync(java.util.concurrent.CompletionStage<? extends T> other, java.util.function.Function<? super T,U> fn) { throw new RuntimeException("Stub!"); }

public <U> java.util.concurrent.CompletableFuture<U> applyToEitherAsync(java.util.concurrent.CompletionStage<? extends T> other, java.util.function.Function<? super T,U> fn, java.util.concurrent.Executor executor) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<java.lang.Void> acceptEither(java.util.concurrent.CompletionStage<? extends T> other, java.util.function.Consumer<? super T> action) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<java.lang.Void> acceptEitherAsync(java.util.concurrent.CompletionStage<? extends T> other, java.util.function.Consumer<? super T> action) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<java.lang.Void> acceptEitherAsync(java.util.concurrent.CompletionStage<? extends T> other, java.util.function.Consumer<? super T> action, java.util.concurrent.Executor executor) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<java.lang.Void> runAfterEither(java.util.concurrent.CompletionStage<?> other, java.lang.Runnable action) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<java.lang.Void> runAfterEitherAsync(java.util.concurrent.CompletionStage<?> other, java.lang.Runnable action) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<java.lang.Void> runAfterEitherAsync(java.util.concurrent.CompletionStage<?> other, java.lang.Runnable action, java.util.concurrent.Executor executor) { throw new RuntimeException("Stub!"); }

public <U> java.util.concurrent.CompletableFuture<U> thenCompose(java.util.function.Function<? super T,? extends java.util.concurrent.CompletionStage<U>> fn) { throw new RuntimeException("Stub!"); }

public <U> java.util.concurrent.CompletableFuture<U> thenComposeAsync(java.util.function.Function<? super T,? extends java.util.concurrent.CompletionStage<U>> fn) { throw new RuntimeException("Stub!"); }

public <U> java.util.concurrent.CompletableFuture<U> thenComposeAsync(java.util.function.Function<? super T,? extends java.util.concurrent.CompletionStage<U>> fn, java.util.concurrent.Executor executor) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<T> whenComplete(java.util.function.BiConsumer<? super T,? super java.lang.Throwable> action) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<T> whenCompleteAsync(java.util.function.BiConsumer<? super T,? super java.lang.Throwable> action) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<T> whenCompleteAsync(java.util.function.BiConsumer<? super T,? super java.lang.Throwable> action, java.util.concurrent.Executor executor) { throw new RuntimeException("Stub!"); }

public <U> java.util.concurrent.CompletableFuture<U> handle(java.util.function.BiFunction<? super T,java.lang.Throwable,? extends U> fn) { throw new RuntimeException("Stub!"); }

public <U> java.util.concurrent.CompletableFuture<U> handleAsync(java.util.function.BiFunction<? super T,java.lang.Throwable,? extends U> fn) { throw new RuntimeException("Stub!"); }

public <U> java.util.concurrent.CompletableFuture<U> handleAsync(java.util.function.BiFunction<? super T,java.lang.Throwable,? extends U> fn, java.util.concurrent.Executor executor) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<T> toCompletableFuture() { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<T> exceptionally(java.util.function.Function<java.lang.Throwable,? extends T> fn) { throw new RuntimeException("Stub!"); }

public static java.util.concurrent.CompletableFuture<java.lang.Void> allOf(java.util.concurrent.CompletableFuture<?>... cfs) { throw new RuntimeException("Stub!"); }

public static java.util.concurrent.CompletableFuture<java.lang.Object> anyOf(java.util.concurrent.CompletableFuture<?>... cfs) { throw new RuntimeException("Stub!"); }

public boolean cancel(boolean mayInterruptIfRunning) { throw new RuntimeException("Stub!"); }

public boolean isCancelled() { throw new RuntimeException("Stub!"); }

public boolean isCompletedExceptionally() { throw new RuntimeException("Stub!"); }

public void obtrudeValue(T value) { throw new RuntimeException("Stub!"); }

public void obtrudeException(java.lang.Throwable ex) { throw new RuntimeException("Stub!"); }

public int getNumberOfDependents() { throw new RuntimeException("Stub!"); }

public java.lang.String toString() { throw new RuntimeException("Stub!"); }

public <U> java.util.concurrent.CompletableFuture<U> newIncompleteFuture() { throw new RuntimeException("Stub!"); }

public java.util.concurrent.Executor defaultExecutor() { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<T> copy() { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletionStage<T> minimalCompletionStage() { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<T> completeAsync(java.util.function.Supplier<? extends T> supplier, java.util.concurrent.Executor executor) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<T> completeAsync(java.util.function.Supplier<? extends T> supplier) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<T> orTimeout(long timeout, java.util.concurrent.TimeUnit unit) { throw new RuntimeException("Stub!"); }

public java.util.concurrent.CompletableFuture<T> completeOnTimeout(T value, long timeout, java.util.concurrent.TimeUnit unit) { throw new RuntimeException("Stub!"); }

public static java.util.concurrent.Executor delayedExecutor(long delay, java.util.concurrent.TimeUnit unit, java.util.concurrent.Executor executor) { throw new RuntimeException("Stub!"); }

public static java.util.concurrent.Executor delayedExecutor(long delay, java.util.concurrent.TimeUnit unit) { throw new RuntimeException("Stub!"); }

public static <U> java.util.concurrent.CompletionStage<U> completedStage(U value) { throw new RuntimeException("Stub!"); }

public static <U> java.util.concurrent.CompletableFuture<U> failedFuture(java.lang.Throwable ex) { throw new RuntimeException("Stub!"); }

public static <U> java.util.concurrent.CompletionStage<U> failedStage(java.lang.Throwable ex) { throw new RuntimeException("Stub!"); }
@SuppressWarnings({"unchecked", "deprecation", "all"})
public static interface AsynchronousCompletionTask {
}

}
