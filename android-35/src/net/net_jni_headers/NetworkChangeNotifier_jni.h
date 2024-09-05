// Copyright 2014 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.


// This file is autogenerated by
//     third_party/jni_zero/jni_generator.py
// For
//     android/net/connectivity/org/chromium/net/NetworkChangeNotifier

#ifndef android_net_connectivity_org_chromium_net_NetworkChangeNotifier_JNI
#define android_net_connectivity_org_chromium_net_NetworkChangeNotifier_JNI

#include <jni.h>

#include "third_party/jni_zero/jni_export.h"
#include "third_party/jni_zero/jni_zero_helper.h"


// Step 1: Forward declarations.

JNI_ZERO_COMPONENT_BUILD_EXPORT extern const char
    kClassPath_android_net_connectivity_org_chromium_net_NetworkChangeNotifier[];
const char kClassPath_android_net_connectivity_org_chromium_net_NetworkChangeNotifier[] =
    "android/net/connectivity/org/chromium/net/NetworkChangeNotifier";
// Leaking this jclass as we cannot use LazyInstance from some threads.
JNI_ZERO_COMPONENT_BUILD_EXPORT std::atomic<jclass>
    g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(nullptr);
#ifndef android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz_defined
#define android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz_defined
inline jclass android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(JNIEnv* env) {
  return base::android::LazyGetClass(env,
      kClassPath_android_net_connectivity_org_chromium_net_NetworkChangeNotifier,
      &g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz);
}
#endif


// Step 2: Constants (optional).


// Step 3: Method stubs.
namespace net {

JNI_BOUNDARY_EXPORT void Java_android_net_connectivity_J_N_MqtHu5YI(
    JNIEnv* env,
    jclass jcaller,
    jlong nativePtr,
    jobject caller,
    jint newConnectionCost) {
  NetworkChangeNotifierDelegateAndroid* native =
      reinterpret_cast<NetworkChangeNotifierDelegateAndroid*>(nativePtr);
  CHECK_NATIVE_PTR(env, jcaller, native, "NotifyConnectionCostChanged");
  return native->NotifyConnectionCostChanged(env, base::android::JavaParamRef<jobject>(env, caller),
      newConnectionCost);
}

JNI_BOUNDARY_EXPORT void Java_android_net_connectivity_J_N_MKvWH5iI(
    JNIEnv* env,
    jclass jcaller,
    jlong nativePtr,
    jobject caller,
    jint newConnectionType,
    jlong defaultNetId) {
  NetworkChangeNotifierDelegateAndroid* native =
      reinterpret_cast<NetworkChangeNotifierDelegateAndroid*>(nativePtr);
  CHECK_NATIVE_PTR(env, jcaller, native, "NotifyConnectionTypeChanged");
  return native->NotifyConnectionTypeChanged(env, base::android::JavaParamRef<jobject>(env, caller),
      newConnectionType, defaultNetId);
}

JNI_BOUNDARY_EXPORT void Java_android_net_connectivity_J_N_MIOACUAc(
    JNIEnv* env,
    jclass jcaller,
    jlong nativePtr,
    jobject caller,
    jint subType) {
  NetworkChangeNotifierDelegateAndroid* native =
      reinterpret_cast<NetworkChangeNotifierDelegateAndroid*>(nativePtr);
  CHECK_NATIVE_PTR(env, jcaller, native, "NotifyMaxBandwidthChanged");
  return native->NotifyMaxBandwidthChanged(env, base::android::JavaParamRef<jobject>(env, caller),
      subType);
}

JNI_BOUNDARY_EXPORT void Java_android_net_connectivity_J_N_MzCImUcu(
    JNIEnv* env,
    jclass jcaller,
    jlong nativePtr,
    jobject caller,
    jlong netId,
    jint connectionType) {
  NetworkChangeNotifierDelegateAndroid* native =
      reinterpret_cast<NetworkChangeNotifierDelegateAndroid*>(nativePtr);
  CHECK_NATIVE_PTR(env, jcaller, native, "NotifyOfNetworkConnect");
  return native->NotifyOfNetworkConnect(env, base::android::JavaParamRef<jobject>(env, caller),
      netId, connectionType);
}

JNI_BOUNDARY_EXPORT void Java_android_net_connectivity_J_N_MMe9mIyq(
    JNIEnv* env,
    jclass jcaller,
    jlong nativePtr,
    jobject caller,
    jlong netId) {
  NetworkChangeNotifierDelegateAndroid* native =
      reinterpret_cast<NetworkChangeNotifierDelegateAndroid*>(nativePtr);
  CHECK_NATIVE_PTR(env, jcaller, native, "NotifyOfNetworkDisconnect");
  return native->NotifyOfNetworkDisconnect(env, base::android::JavaParamRef<jobject>(env, caller),
      netId);
}

JNI_BOUNDARY_EXPORT void Java_android_net_connectivity_J_N_MOFU0znc(
    JNIEnv* env,
    jclass jcaller,
    jlong nativePtr,
    jobject caller,
    jlong netId) {
  NetworkChangeNotifierDelegateAndroid* native =
      reinterpret_cast<NetworkChangeNotifierDelegateAndroid*>(nativePtr);
  CHECK_NATIVE_PTR(env, jcaller, native, "NotifyOfNetworkSoonToDisconnect");
  return native->NotifyOfNetworkSoonToDisconnect(env, base::android::JavaParamRef<jobject>(env,
      caller), netId);
}

JNI_BOUNDARY_EXPORT void Java_android_net_connectivity_J_N_MmdqE1Yd(
    JNIEnv* env,
    jclass jcaller,
    jlong nativePtr,
    jobject caller,
    jlongArray activeNetIds) {
  NetworkChangeNotifierDelegateAndroid* native =
      reinterpret_cast<NetworkChangeNotifierDelegateAndroid*>(nativePtr);
  CHECK_NATIVE_PTR(env, jcaller, native, "NotifyPurgeActiveNetworkList");
  return native->NotifyPurgeActiveNetworkList(env, base::android::JavaParamRef<jobject>(env,
      caller), base::android::JavaParamRef<jlongArray>(env, activeNetIds));
}


static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_addNativeObserver1(nullptr);
static void Java_NetworkChangeNotifier_addNativeObserver(JNIEnv* env, const
    base::android::JavaRef<jobject>& obj, jlong nativeChangeNotifier) {
  jclass clazz = android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env);
  CHECK_CLAZZ(env, obj.obj(),
      android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env));

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_INSTANCE>(
          env,
          clazz,
          "addNativeObserver",
          "(J)V",
          &g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_addNativeObserver1);

     env->CallVoidMethod(obj.obj(),
          call_context.base.method_id, nativeChangeNotifier);
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_fakeConnectionCostChanged1(nullptr);
static void Java_NetworkChangeNotifier_fakeConnectionCostChanged(JNIEnv* env, JniIntWrapper
    connectionCost) {
  jclass clazz = android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env);
  CHECK_CLAZZ(env, clazz,
      android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env));

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_STATIC>(
          env,
          clazz,
          "fakeConnectionCostChanged",
          "(I)V",
&g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_fakeConnectionCostChanged1);

     env->CallStaticVoidMethod(clazz,
          call_context.base.method_id, as_jint(connectionCost));
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_fakeConnectionSubtypeChanged1(nullptr);
static void Java_NetworkChangeNotifier_fakeConnectionSubtypeChanged(JNIEnv* env, JniIntWrapper
    connectionSubtype) {
  jclass clazz = android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env);
  CHECK_CLAZZ(env, clazz,
      android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env));

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_STATIC>(
          env,
          clazz,
          "fakeConnectionSubtypeChanged",
          "(I)V",
&g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_fakeConnectionSubtypeChanged1);

     env->CallStaticVoidMethod(clazz,
          call_context.base.method_id, as_jint(connectionSubtype));
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_fakeDefaultNetwork2(nullptr);
static void Java_NetworkChangeNotifier_fakeDefaultNetwork(JNIEnv* env, jlong netId,
    JniIntWrapper connectionType) {
  jclass clazz = android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env);
  CHECK_CLAZZ(env, clazz,
      android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env));

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_STATIC>(
          env,
          clazz,
          "fakeDefaultNetwork",
          "(JI)V",
          &g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_fakeDefaultNetwork2);

     env->CallStaticVoidMethod(clazz,
          call_context.base.method_id, netId, as_jint(connectionType));
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_fakeNetworkConnected2(nullptr);
static void Java_NetworkChangeNotifier_fakeNetworkConnected(JNIEnv* env, jlong netId,
    JniIntWrapper connectionType) {
  jclass clazz = android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env);
  CHECK_CLAZZ(env, clazz,
      android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env));

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_STATIC>(
          env,
          clazz,
          "fakeNetworkConnected",
          "(JI)V",
          &g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_fakeNetworkConnected2);

     env->CallStaticVoidMethod(clazz,
          call_context.base.method_id, netId, as_jint(connectionType));
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_fakeNetworkDisconnected1(nullptr);
static void Java_NetworkChangeNotifier_fakeNetworkDisconnected(JNIEnv* env, jlong netId) {
  jclass clazz = android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env);
  CHECK_CLAZZ(env, clazz,
      android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env));

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_STATIC>(
          env,
          clazz,
          "fakeNetworkDisconnected",
          "(J)V",
&g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_fakeNetworkDisconnected1);

     env->CallStaticVoidMethod(clazz,
          call_context.base.method_id, netId);
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_fakeNetworkSoonToBeDisconnected1(nullptr);
static void Java_NetworkChangeNotifier_fakeNetworkSoonToBeDisconnected(JNIEnv* env, jlong netId) {
  jclass clazz = android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env);
  CHECK_CLAZZ(env, clazz,
      android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env));

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_STATIC>(
          env,
          clazz,
          "fakeNetworkSoonToBeDisconnected",
          "(J)V",
&g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_fakeNetworkSoonToBeDisconnected1);

     env->CallStaticVoidMethod(clazz,
          call_context.base.method_id, netId);
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_fakePurgeActiveNetworkList1(nullptr);
static void Java_NetworkChangeNotifier_fakePurgeActiveNetworkList(JNIEnv* env, const
    base::android::JavaRef<jlongArray>& activeNetIds) {
  jclass clazz = android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env);
  CHECK_CLAZZ(env, clazz,
      android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env));

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_STATIC>(
          env,
          clazz,
          "fakePurgeActiveNetworkList",
          "([J)V",
&g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_fakePurgeActiveNetworkList1);

     env->CallStaticVoidMethod(clazz,
          call_context.base.method_id, activeNetIds.obj());
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_forceConnectivityState1(nullptr);
static void Java_NetworkChangeNotifier_forceConnectivityState(JNIEnv* env, jboolean
    networkAvailable) {
  jclass clazz = android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env);
  CHECK_CLAZZ(env, clazz,
      android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env));

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_STATIC>(
          env,
          clazz,
          "forceConnectivityState",
          "(Z)V",
&g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_forceConnectivityState1);

     env->CallStaticVoidMethod(clazz,
          call_context.base.method_id, networkAvailable);
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_getCurrentConnectionCost0(nullptr);
static jint Java_NetworkChangeNotifier_getCurrentConnectionCost(JNIEnv* env, const
    base::android::JavaRef<jobject>& obj) {
  jclass clazz = android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env);
  CHECK_CLAZZ(env, obj.obj(),
      android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env), 0);

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_INSTANCE>(
          env,
          clazz,
          "getCurrentConnectionCost",
          "()I",
&g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_getCurrentConnectionCost0);

  jint ret =
      env->CallIntMethod(obj.obj(),
          call_context.base.method_id);
  return ret;
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_getCurrentConnectionSubtype0(nullptr);
static jint Java_NetworkChangeNotifier_getCurrentConnectionSubtype(JNIEnv* env, const
    base::android::JavaRef<jobject>& obj) {
  jclass clazz = android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env);
  CHECK_CLAZZ(env, obj.obj(),
      android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env), 0);

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_INSTANCE>(
          env,
          clazz,
          "getCurrentConnectionSubtype",
          "()I",
&g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_getCurrentConnectionSubtype0);

  jint ret =
      env->CallIntMethod(obj.obj(),
          call_context.base.method_id);
  return ret;
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_getCurrentConnectionType0(nullptr);
static jint Java_NetworkChangeNotifier_getCurrentConnectionType(JNIEnv* env, const
    base::android::JavaRef<jobject>& obj) {
  jclass clazz = android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env);
  CHECK_CLAZZ(env, obj.obj(),
      android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env), 0);

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_INSTANCE>(
          env,
          clazz,
          "getCurrentConnectionType",
          "()I",
&g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_getCurrentConnectionType0);

  jint ret =
      env->CallIntMethod(obj.obj(),
          call_context.base.method_id);
  return ret;
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_getCurrentDefaultNetId0(nullptr);
static jlong Java_NetworkChangeNotifier_getCurrentDefaultNetId(JNIEnv* env, const
    base::android::JavaRef<jobject>& obj) {
  jclass clazz = android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env);
  CHECK_CLAZZ(env, obj.obj(),
      android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env), 0);

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_INSTANCE>(
          env,
          clazz,
          "getCurrentDefaultNetId",
          "()J",
&g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_getCurrentDefaultNetId0);

  jlong ret =
      env->CallLongMethod(obj.obj(),
          call_context.base.method_id);
  return ret;
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_getCurrentNetworksAndTypes0(nullptr);
static base::android::ScopedJavaLocalRef<jlongArray>
    Java_NetworkChangeNotifier_getCurrentNetworksAndTypes(JNIEnv* env, const
    base::android::JavaRef<jobject>& obj) {
  jclass clazz = android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env);
  CHECK_CLAZZ(env, obj.obj(),
      android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env), nullptr);

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_INSTANCE>(
          env,
          clazz,
          "getCurrentNetworksAndTypes",
          "()[J",
&g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_getCurrentNetworksAndTypes0);

  jlongArray ret =
      static_cast<jlongArray>(env->CallObjectMethod(obj.obj(),
          call_context.base.method_id));
  return base::android::ScopedJavaLocalRef<jlongArray>(env, ret);
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_init0(nullptr);
static base::android::ScopedJavaLocalRef<jobject> Java_NetworkChangeNotifier_init(JNIEnv* env) {
  jclass clazz = android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env);
  CHECK_CLAZZ(env, clazz,
      android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env), nullptr);

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_STATIC>(
          env,
          clazz,
          "init",
          "()Landroid/net/connectivity/org/chromium/net/NetworkChangeNotifier;",
          &g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_init0);

  jobject ret =
      env->CallStaticObjectMethod(clazz,
          call_context.base.method_id);
  return base::android::ScopedJavaLocalRef<jobject>(env, ret);
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_registerNetworkCallbackFailed0(nullptr);
static jboolean Java_NetworkChangeNotifier_registerNetworkCallbackFailed(JNIEnv* env, const
    base::android::JavaRef<jobject>& obj) {
  jclass clazz = android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env);
  CHECK_CLAZZ(env, obj.obj(),
      android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env), false);

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_INSTANCE>(
          env,
          clazz,
          "registerNetworkCallbackFailed",
          "()Z",
&g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_registerNetworkCallbackFailed0);

  jboolean ret =
      env->CallBooleanMethod(obj.obj(),
          call_context.base.method_id);
  return ret;
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_removeNativeObserver1(nullptr);
static void Java_NetworkChangeNotifier_removeNativeObserver(JNIEnv* env, const
    base::android::JavaRef<jobject>& obj, jlong nativeChangeNotifier) {
  jclass clazz = android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env);
  CHECK_CLAZZ(env, obj.obj(),
      android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env));

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_INSTANCE>(
          env,
          clazz,
          "removeNativeObserver",
          "(J)V",
          &g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_removeNativeObserver1);

     env->CallVoidMethod(obj.obj(),
          call_context.base.method_id, nativeChangeNotifier);
}

static std::atomic<jmethodID>
    g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_setAutoDetectConnectivityState1(nullptr);
static void Java_NetworkChangeNotifier_setAutoDetectConnectivityState(JNIEnv* env, jboolean
    shouldAutoDetect) {
  jclass clazz = android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env);
  CHECK_CLAZZ(env, clazz,
      android_net_connectivity_org_chromium_net_NetworkChangeNotifier_clazz(env));

  jni_generator::JniJavaCallContextChecked call_context;
  call_context.Init<
      base::android::MethodID::TYPE_STATIC>(
          env,
          clazz,
          "setAutoDetectConnectivityState",
          "(Z)V",
&g_android_net_connectivity_org_chromium_net_NetworkChangeNotifier_setAutoDetectConnectivityState1);

     env->CallStaticVoidMethod(clazz,
          call_context.base.method_id, shouldAutoDetect);
}

}  // namespace net

#endif  // android_net_connectivity_org_chromium_net_NetworkChangeNotifier_JNI