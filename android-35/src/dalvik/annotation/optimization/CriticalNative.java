/*
 * Copyright (C) 2016 The Android Open Source Project
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

package dalvik.annotation.optimization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An ART runtime built-in optimization for {@code native} methods to speed up JNI transitions:
 * Methods that are annotated with {@literal @}{@code CriticalNative} use the fastest
 * available JNI transitions from managed code to the native code and back.
 * This annotation can be applied only to native methods that do not use managed
 * objects (in parameters or return values, or as an implicit {@code this}).
 *
 * <p>
 * The {@literal @}{@code CriticalNative} annotation changes the JNI transition ABI.
 * The native implementation must exclude the {@code JNIEnv} and {@code jclass} parameters
 * from its function signature.
 * </p>
 *
 * <p>
 * While executing a {@literal @}{@code CriticalNative} method, the garbage collection cannot
 * suspend the thread for essential work and may become blocked. Use with caution. Do not use
 * this annotation for long-running methods, including usually-fast, but generally unbounded,
 * methods. In particular, the code should not perform significant I/O operations or acquire
 * native locks that can be held for a long time. (Some logging or native allocations, which
 * internally acquire native locks for a short time, are generally OK. However, as the cost
 * of several such operations adds up, the {@literal @}{@code CriticalNative} performance gain
 * can become insignificant and overshadowed by potential GC delays.)
 * </p>
 *
 * <p>
 * For performance critical methods that need this annotation, it is strongly recommended
 * to explicitly register the method(s) with JNI {@code RegisterNatives} instead of relying
 * on the built-in dynamic JNI linking.
 * </p>
 *
 * <p>
 * The {@literal @}{@code CriticalNative} optimization was implemented for system use since
 * Android 8 and became CTS-tested public API in Android 14. Developers aiming for maximum
 * compatibility should avoid calling {@literal @}{@code CriticalNative} methods on Android 13-.
 * The optimization is likely to work also on Android 8-13 devices (after all, it was used
 * in the system, albeit without the strong CTS guarantees), especially those that use
 * unmodified versions of ART, such as Android 12+ devices with the official ART Module.
 * The built-in dynamic JNI linking is working only in Android 12+, the explicit registration
 * with JNI {@code RegisterNatives} is strictly required for running on Android versions 8-11.
 * The annotation is ignored on Android 7-, so the ABI mismatch would lead to wrong argument
 * marshalling and likely crashes.
 * </p>
 *
 * <p>
 * A similar annotation, {@literal @}{@link FastNative}, exists for methods that need fast
 * transitions but absolutely need to use managed objects, whether as the implicit {@code this}
 * for non-static methods, or method arguments, return values or to otherwise call back to
 * managed code (say, static methods), or access managed heap objects (say, static fields).
 * </p>
 *
 * <p>
 * Performance of JNI transitions:
 * <ul>
 * <li>Regular JNI cost in nanoseconds: 115
 * <li>Fast {@code (!)} JNI cost in nanoseconds: 60
 * <li>{@literal @}{@link FastNative} cost in nanoseconds: 35
 * <li>{@literal @}{@code CriticalNative} cost in nanoseconds: 25
 * </ul>
 * (Measured on angler-userdebug in 07/2016).
 * </p>
 *
 * <p>
 * <b>Deadlock Warning:</b> As a rule of thumb, any native locks acquired in a
 * {@literal @}{@link CriticalNative} call (despite the above warning that this is an unbounded
 * operation that can block GC for a long time) must be released before returning to managed code.
 * </p>
 *
 * <p>
 * Say some code does:
 *
 * <code>
 * critical_native_call_to_grab_a_lock();
 * does_some_java_work();
 * critical_native_call_to_release_a_lock();
 * </code>
 *
 * <p>
 * This code can lead to deadlocks. Say thread 1 just finishes
 * {@code critical_native_call_to_grab_a_lock()} and is in {@code does_some_java_work()}.
 * GC kicks in and suspends thread 1. Thread 2 now is in
 * {@code critical_native_call_to_grab_a_lock()} but is blocked on grabbing the
 * native lock since it's held by thread 1. Now thread suspension can't finish
 * since thread 2 can't be suspended since it's doing CriticalNative JNI.
 * </p>
 *
 * <p>
 * Normal natives don't have the issue since once it's executing in native code,
 * it is considered suspended from the runtime's point of view.
 * CriticalNative natives however don't do the state transition done by the normal natives.
 * </p>
 *
 * <p>
 * This annotation has no effect when used with non-native methods.
 * </p>
 *
 * <p>
 * The runtime shall throw a {@link java.lang.VerifyError} during verification if this annotation
 * is present on a native method that is non-static, or contains object parameters, or returns an
 * object.
 * </p>
 */
@Retention(RetentionPolicy.CLASS)  // Save memory, don't instantiate as an object at runtime.
@Target(ElementType.METHOD)
public @interface CriticalNative {}
