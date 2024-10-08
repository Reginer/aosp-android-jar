// Copyright 2014 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.


// This file is autogenerated by
//     third_party/jni_zero/jni_generator.py
// For
//     android/net/connectivity/org/chromium/base/SysUtils

#ifndef android_net_connectivity_org_chromium_base_SysUtils_JNI
#define android_net_connectivity_org_chromium_base_SysUtils_JNI

#include <jni.h>

#include "third_party/jni_zero/jni_export.h"
#include "third_party/jni_zero/jni_zero_helper.h"


// Step 1: Forward declarations.

JNI_ZERO_COMPONENT_BUILD_EXPORT extern const char
    kClassPath_android_net_connectivity_org_chromium_base_SysUtils[];
const char kClassPath_android_net_connectivity_org_chromium_base_SysUtils[] =
    "android/net/connectivity/org/chromium/base/SysUtils";
// Leaking this jclass as we cannot use LazyInstance from some threads.
JNI_ZERO_COMPONENT_BUILD_EXPORT std::atomic<jclass>
    g_android_net_connectivity_org_chromium_base_SysUtils_clazz(nullptr);
#ifndef android_net_connectivity_org_chromium_base_SysUtils_clazz_defined
#define android_net_connectivity_org_chromium_base_SysUtils_clazz_defined
inline jclass android_net_connectivity_org_chromium_base_SysUtils_clazz(JNIEnv* env) {
  return base::android::LazyGetClass(env,
      kClassPath_android_net_connectivity_org_chromium_base_SysUtils,
      &g_android_net_connectivity_org_chromium_base_SysUtils_clazz);
}
#endif


// Step 2: Constants (optional).


// Step 3: Method stubs.
namespace base {
namespace android {

static void JNI_SysUtils_LogPageFaultCountToTracing(JNIEnv* env);

JNI_BOUNDARY_EXPORT void Java_android_net_connectivity_J_N_MZfdBYbM(
    JNIEnv* env,
    jclass jcaller) {
  return JNI_SysUtils_LogPageFaultCountToTracing(env);
}


static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_base_SysUtils_amountOfPhysicalMemoryKB0(nullptr);
static jint Java_SysUtils_amountOfPhysicalMemoryKB(JNIEnv* env) {
  jclass clazz = android_net_connectivity_org_chromium_base_SysUtils_clazz(env);
  CHECK_CLAZZ(env, clazz,
      android_net_connectivity_org_chromium_base_SysUtils_clazz(env), 0);

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_STATIC>(
          env,
          clazz,
          "amountOfPhysicalMemoryKB",
          "()I",
          &g_android_net_connectivity_org_chromium_base_SysUtils_amountOfPhysicalMemoryKB0);

  jint ret =
      env->CallStaticIntMethod(clazz,
          call_context.base.method_id);
  return ret;
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_base_SysUtils_isCurrentlyLowMemory0(nullptr);
static jboolean Java_SysUtils_isCurrentlyLowMemory(JNIEnv* env) {
  jclass clazz = android_net_connectivity_org_chromium_base_SysUtils_clazz(env);
  CHECK_CLAZZ(env, clazz,
      android_net_connectivity_org_chromium_base_SysUtils_clazz(env), false);

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_STATIC>(
          env,
          clazz,
          "isCurrentlyLowMemory",
          "()Z",
          &g_android_net_connectivity_org_chromium_base_SysUtils_isCurrentlyLowMemory0);

  jboolean ret =
      env->CallStaticBooleanMethod(clazz,
          call_context.base.method_id);
  return ret;
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_base_SysUtils_isLowEndDevice0(nullptr);
static jboolean Java_SysUtils_isLowEndDevice(JNIEnv* env) {
  jclass clazz = android_net_connectivity_org_chromium_base_SysUtils_clazz(env);
  CHECK_CLAZZ(env, clazz,
      android_net_connectivity_org_chromium_base_SysUtils_clazz(env), false);

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_STATIC>(
          env,
          clazz,
          "isLowEndDevice",
          "()Z",
          &g_android_net_connectivity_org_chromium_base_SysUtils_isLowEndDevice0);

  jboolean ret =
      env->CallStaticBooleanMethod(clazz,
          call_context.base.method_id);
  return ret;
}

}  // namespace android
}  // namespace base

#endif  // android_net_connectivity_org_chromium_base_SysUtils_JNI
