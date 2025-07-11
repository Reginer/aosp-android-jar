// This file was generated by
//     //third_party/jni_zero/jni_zero.py
// For
//     android.net.connectivity.org.chromium.base.RadioUtils

#ifndef android_net_connectivity_org_chromium_base_RadioUtils_JNI
#define android_net_connectivity_org_chromium_base_RadioUtils_JNI

#include <jni.h>

#include "third_party/jni_zero/jni_export.h"
#include "third_party/jni_zero/jni_zero_internal.h"

// Class Accessors
#ifndef android_net_connectivity_org_chromium_base_RadioUtils_clazz_defined
#define android_net_connectivity_org_chromium_base_RadioUtils_clazz_defined
inline jclass android_net_connectivity_org_chromium_base_RadioUtils_clazz(JNIEnv* env) {
  static const char kClassName[] = "android.net.connectivity.org.chromium.base.RadioUtils";
  static std::atomic<jclass> cached_class;
  return jni_zero::internal::LazyGetClass(env, kClassName, &cached_class);
}
#endif


namespace base::android {

// Native to Java functions
static jint Java_RadioUtils_getCellDataActivity(JNIEnv* env) {
  static std::atomic<jmethodID> cached_method_id(nullptr);
  jclass clazz = android_net_connectivity_org_chromium_base_RadioUtils_clazz(env);
  CHECK_CLAZZ(env, clazz, clazz, 0);
  jni_zero::internal::JniJavaCallContext<true> call_context;
  call_context.Init<jni_zero::MethodID::TYPE_STATIC>(
      env,
      clazz,
      "getCellDataActivity",
      "()I",
      &cached_method_id);
  auto _ret = env->CallStaticIntMethod(clazz, call_context.method_id());
  return _ret;
}

static jint Java_RadioUtils_getCellSignalLevel(JNIEnv* env) {
  static std::atomic<jmethodID> cached_method_id(nullptr);
  jclass clazz = android_net_connectivity_org_chromium_base_RadioUtils_clazz(env);
  CHECK_CLAZZ(env, clazz, clazz, 0);
  jni_zero::internal::JniJavaCallContext<true> call_context;
  call_context.Init<jni_zero::MethodID::TYPE_STATIC>(
      env,
      clazz,
      "getCellSignalLevel",
      "()I",
      &cached_method_id);
  auto _ret = env->CallStaticIntMethod(clazz, call_context.method_id());
  return _ret;
}

static jboolean Java_RadioUtils_isSupported(JNIEnv* env) {
  static std::atomic<jmethodID> cached_method_id(nullptr);
  jclass clazz = android_net_connectivity_org_chromium_base_RadioUtils_clazz(env);
  CHECK_CLAZZ(env, clazz, clazz, false);
  jni_zero::internal::JniJavaCallContext<true> call_context;
  call_context.Init<jni_zero::MethodID::TYPE_STATIC>(
      env,
      clazz,
      "isSupported",
      "()Z",
      &cached_method_id);
  auto _ret = env->CallStaticBooleanMethod(clazz, call_context.method_id());
  return _ret;
}

static jboolean Java_RadioUtils_isWifiConnected(JNIEnv* env) {
  static std::atomic<jmethodID> cached_method_id(nullptr);
  jclass clazz = android_net_connectivity_org_chromium_base_RadioUtils_clazz(env);
  CHECK_CLAZZ(env, clazz, clazz, false);
  jni_zero::internal::JniJavaCallContext<true> call_context;
  call_context.Init<jni_zero::MethodID::TYPE_STATIC>(
      env,
      clazz,
      "isWifiConnected",
      "()Z",
      &cached_method_id);
  auto _ret = env->CallStaticBooleanMethod(clazz, call_context.method_id());
  return _ret;
}



}  // namespace base::android

#endif  // android_net_connectivity_org_chromium_base_RadioUtils_JNI
