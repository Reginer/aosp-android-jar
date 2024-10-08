// Copyright 2014 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.


// This file is autogenerated by
//     third_party/jni_zero/jni_generator.py
// For
//     android/net/connectivity/org/chromium/base/JavaHandlerThread

#ifndef android_net_connectivity_org_chromium_base_JavaHandlerThread_JNI
#define android_net_connectivity_org_chromium_base_JavaHandlerThread_JNI

#include <jni.h>

#include "third_party/jni_zero/jni_export.h"
#include "third_party/jni_zero/jni_zero_helper.h"


// Step 1: Forward declarations.

JNI_ZERO_COMPONENT_BUILD_EXPORT extern const char
    kClassPath_android_net_connectivity_org_chromium_base_JavaHandlerThread[];
const char kClassPath_android_net_connectivity_org_chromium_base_JavaHandlerThread[] =
    "android/net/connectivity/org/chromium/base/JavaHandlerThread";
// Leaking this jclass as we cannot use LazyInstance from some threads.
JNI_ZERO_COMPONENT_BUILD_EXPORT std::atomic<jclass>
    g_android_net_connectivity_org_chromium_base_JavaHandlerThread_clazz(nullptr);
#ifndef android_net_connectivity_org_chromium_base_JavaHandlerThread_clazz_defined
#define android_net_connectivity_org_chromium_base_JavaHandlerThread_clazz_defined
inline jclass android_net_connectivity_org_chromium_base_JavaHandlerThread_clazz(JNIEnv* env) {
  return base::android::LazyGetClass(env,
      kClassPath_android_net_connectivity_org_chromium_base_JavaHandlerThread,
      &g_android_net_connectivity_org_chromium_base_JavaHandlerThread_clazz);
}
#endif


// Step 2: Constants (optional).


// Step 3: Method stubs.
namespace base {
namespace android {

JNI_BOUNDARY_EXPORT void Java_android_net_connectivity_J_N_M_1Z7ceOr(
    JNIEnv* env,
    jclass jcaller,
    jlong nativeJavaHandlerThread,
    jlong nativeEvent) {
  JavaHandlerThread* native = reinterpret_cast<JavaHandlerThread*>(nativeJavaHandlerThread);
  CHECK_NATIVE_PTR(env, jcaller, native, "InitializeThread");
  return native->InitializeThread(env, nativeEvent);
}

JNI_BOUNDARY_EXPORT void Java_android_net_connectivity_J_N_MHuj_1tLF(
    JNIEnv* env,
    jclass jcaller,
    jlong nativeJavaHandlerThread) {
  JavaHandlerThread* native = reinterpret_cast<JavaHandlerThread*>(nativeJavaHandlerThread);
  CHECK_NATIVE_PTR(env, jcaller, native, "OnLooperStopped");
  return native->OnLooperStopped(env);
}


static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_base_JavaHandlerThread_create2(nullptr);
static base::android::ScopedJavaLocalRef<jobject> Java_JavaHandlerThread_create(JNIEnv* env, const
    base::android::JavaRef<jstring>& name,
    JniIntWrapper priority) {
  jclass clazz = android_net_connectivity_org_chromium_base_JavaHandlerThread_clazz(env);
  CHECK_CLAZZ(env, clazz,
      android_net_connectivity_org_chromium_base_JavaHandlerThread_clazz(env), nullptr);

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_STATIC>(
          env,
          clazz,
          "create",
          "(Ljava/lang/String;I)Landroid/net/connectivity/org/chromium/base/JavaHandlerThread;",
          &g_android_net_connectivity_org_chromium_base_JavaHandlerThread_create2);

  jobject ret =
      env->CallStaticObjectMethod(clazz,
          call_context.base.method_id, name.obj(), as_jint(priority));
  return base::android::ScopedJavaLocalRef<jobject>(env, ret);
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_base_JavaHandlerThread_getUncaughtExceptionIfAny0(nullptr);
static base::android::ScopedJavaLocalRef<jthrowable>
    Java_JavaHandlerThread_getUncaughtExceptionIfAny(JNIEnv* env, const
    base::android::JavaRef<jobject>& obj) {
  jclass clazz = android_net_connectivity_org_chromium_base_JavaHandlerThread_clazz(env);
  CHECK_CLAZZ(env, obj.obj(),
      android_net_connectivity_org_chromium_base_JavaHandlerThread_clazz(env), nullptr);

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_INSTANCE>(
          env,
          clazz,
          "getUncaughtExceptionIfAny",
          "()Ljava/lang/Throwable;",
&g_android_net_connectivity_org_chromium_base_JavaHandlerThread_getUncaughtExceptionIfAny0);

  jthrowable ret =
      static_cast<jthrowable>(env->CallObjectMethod(obj.obj(),
          call_context.base.method_id));
  return base::android::ScopedJavaLocalRef<jthrowable>(env, ret);
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_base_JavaHandlerThread_isAlive0(nullptr);
static jboolean Java_JavaHandlerThread_isAlive(JNIEnv* env, const base::android::JavaRef<jobject>&
    obj) {
  jclass clazz = android_net_connectivity_org_chromium_base_JavaHandlerThread_clazz(env);
  CHECK_CLAZZ(env, obj.obj(),
      android_net_connectivity_org_chromium_base_JavaHandlerThread_clazz(env), false);

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_INSTANCE>(
          env,
          clazz,
          "isAlive",
          "()Z",
          &g_android_net_connectivity_org_chromium_base_JavaHandlerThread_isAlive0);

  jboolean ret =
      env->CallBooleanMethod(obj.obj(),
          call_context.base.method_id);
  return ret;
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_base_JavaHandlerThread_joinThread0(nullptr);
static void Java_JavaHandlerThread_joinThread(JNIEnv* env, const base::android::JavaRef<jobject>&
    obj) {
  jclass clazz = android_net_connectivity_org_chromium_base_JavaHandlerThread_clazz(env);
  CHECK_CLAZZ(env, obj.obj(),
      android_net_connectivity_org_chromium_base_JavaHandlerThread_clazz(env));

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_INSTANCE>(
          env,
          clazz,
          "joinThread",
          "()V",
          &g_android_net_connectivity_org_chromium_base_JavaHandlerThread_joinThread0);

     env->CallVoidMethod(obj.obj(),
          call_context.base.method_id);
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_base_JavaHandlerThread_listenForUncaughtExceptionsForTesting0(nullptr);
static void Java_JavaHandlerThread_listenForUncaughtExceptionsForTesting(JNIEnv* env, const
    base::android::JavaRef<jobject>& obj) {
  jclass clazz = android_net_connectivity_org_chromium_base_JavaHandlerThread_clazz(env);
  CHECK_CLAZZ(env, obj.obj(),
      android_net_connectivity_org_chromium_base_JavaHandlerThread_clazz(env));

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_INSTANCE>(
          env,
          clazz,
          "listenForUncaughtExceptionsForTesting",
          "()V",
&g_android_net_connectivity_org_chromium_base_JavaHandlerThread_listenForUncaughtExceptionsForTesting0);

     env->CallVoidMethod(obj.obj(),
          call_context.base.method_id);
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_base_JavaHandlerThread_quitThreadSafely1(nullptr);
static void Java_JavaHandlerThread_quitThreadSafely(JNIEnv* env, const
    base::android::JavaRef<jobject>& obj, jlong nativeThread) {
  jclass clazz = android_net_connectivity_org_chromium_base_JavaHandlerThread_clazz(env);
  CHECK_CLAZZ(env, obj.obj(),
      android_net_connectivity_org_chromium_base_JavaHandlerThread_clazz(env));

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_INSTANCE>(
          env,
          clazz,
          "quitThreadSafely",
          "(J)V",
          &g_android_net_connectivity_org_chromium_base_JavaHandlerThread_quitThreadSafely1);

     env->CallVoidMethod(obj.obj(),
          call_context.base.method_id, nativeThread);
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_base_JavaHandlerThread_startAndInitialize2(nullptr);
static void Java_JavaHandlerThread_startAndInitialize(JNIEnv* env, const
    base::android::JavaRef<jobject>& obj, jlong nativeThread,
    jlong nativeEvent) {
  jclass clazz = android_net_connectivity_org_chromium_base_JavaHandlerThread_clazz(env);
  CHECK_CLAZZ(env, obj.obj(),
      android_net_connectivity_org_chromium_base_JavaHandlerThread_clazz(env));

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_INSTANCE>(
          env,
          clazz,
          "startAndInitialize",
          "(JJ)V",
          &g_android_net_connectivity_org_chromium_base_JavaHandlerThread_startAndInitialize2);

     env->CallVoidMethod(obj.obj(),
          call_context.base.method_id, nativeThread, nativeEvent);
}

}  // namespace android
}  // namespace base

#endif  // android_net_connectivity_org_chromium_base_JavaHandlerThread_JNI
