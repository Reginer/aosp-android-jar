// This file was generated by
//     //third_party/jni_zero/jni_zero.py
// For
//     android.net.connectivity.org.chromium.base.InputHintChecker

#ifndef android_net_connectivity_org_chromium_base_InputHintChecker_JNI
#define android_net_connectivity_org_chromium_base_InputHintChecker_JNI

#include <jni.h>

#include "third_party/jni_zero/jni_export.h"
#include "third_party/jni_zero/jni_zero_internal.h"
namespace base::android {

// Java to native functions
// Forward declaration. To be implemented by the including .cc file.
static jboolean JNI_InputHintChecker_FailedToInitializeForTesting(JNIEnv* env);

JNI_ZERO_BOUNDARY_EXPORT jboolean Java_android_net_connectivity_J_N_Ma_1PfM1B_1ForTesting(
    JNIEnv* env,
    jclass jcaller) {
  auto _ret = JNI_InputHintChecker_FailedToInitializeForTesting(env);
  return _ret;
}

// Forward declaration. To be implemented by the including .cc file.
static jboolean JNI_InputHintChecker_HasInputForTesting(JNIEnv* env);

JNI_ZERO_BOUNDARY_EXPORT jboolean Java_android_net_connectivity_J_N_MvkDa5Md_1ForTesting(
    JNIEnv* env,
    jclass jcaller) {
  auto _ret = JNI_InputHintChecker_HasInputForTesting(env);
  return _ret;
}

// Forward declaration. To be implemented by the including .cc file.
static jboolean JNI_InputHintChecker_HasInputWithThrottlingForTesting(JNIEnv* env);

JNI_ZERO_BOUNDARY_EXPORT jboolean Java_android_net_connectivity_J_N_MH9vZUui_1ForTesting(
    JNIEnv* env,
    jclass jcaller) {
  auto _ret = JNI_InputHintChecker_HasInputWithThrottlingForTesting(env);
  return _ret;
}

// Forward declaration. To be implemented by the including .cc file.
static jboolean JNI_InputHintChecker_IsInitializedForTesting(JNIEnv* env);

JNI_ZERO_BOUNDARY_EXPORT jboolean Java_android_net_connectivity_J_N_MK_1AUy1k_1ForTesting(
    JNIEnv* env,
    jclass jcaller) {
  auto _ret = JNI_InputHintChecker_IsInitializedForTesting(env);
  return _ret;
}

// Forward declaration. To be implemented by the including .cc file.
static void JNI_InputHintChecker_OnCompositorViewHolderTouchEvent(JNIEnv* env);

JNI_ZERO_BOUNDARY_EXPORT void Java_android_net_connectivity_J_N_MhjkgOpF(
    JNIEnv* env,
    jclass jcaller) {
  JNI_InputHintChecker_OnCompositorViewHolderTouchEvent(env);
}

// Forward declaration. To be implemented by the including .cc file.
static void JNI_InputHintChecker_SetIsAfterInputYieldForTesting(JNIEnv* env, jboolean after);

JNI_ZERO_BOUNDARY_EXPORT void Java_android_net_connectivity_J_N_M10BXcwi_1ForTesting(
    JNIEnv* env,
    jclass jcaller,
    jboolean after) {
  JNI_InputHintChecker_SetIsAfterInputYieldForTesting(env, after);
}

// Forward declaration. To be implemented by the including .cc file.
static void JNI_InputHintChecker_SetView(JNIEnv* env, const jni_zero::JavaParamRef<jobject>& view);

JNI_ZERO_BOUNDARY_EXPORT void Java_android_net_connectivity_J_N_MJvw9Zwk(
    JNIEnv* env,
    jclass jcaller,
    jobject view) {
  JNI_InputHintChecker_SetView(env, jni_zero::JavaParamRef<jobject>(env, view));
}



}  // namespace base::android

#endif  // android_net_connectivity_org_chromium_base_InputHintChecker_JNI
