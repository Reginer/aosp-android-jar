#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_regin_reflect_impl_NativeLib_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
typedef union {
    JNIEnv *env;
    void *venv;
} JNIEnvToVoid;

bool setApiBlacklistExemptions(JNIEnv *env) {
    jclass zygoteInit = env->FindClass("com/android/internal/os/ZygoteInit");
    if (zygoteInit == nullptr) {
        env->ExceptionClear();
        return false;
    }

    jmethodID setApiBlacklistExemptions = env->GetStaticMethodID(zygoteInit, "setApiBlacklistExemptions", "([Ljava/lang/String;)V");

    if (setApiBlacklistExemptions == nullptr) {
#if !defined(DEBUG)
        env->ExceptionClear();
#endif
        return false;
    }
    jclass stringCLass = env->FindClass("java/lang/String");
    jstring lString = env->NewStringUTF("L");
    jobjectArray jObjectArray = env->NewObjectArray(1, stringCLass, nullptr);
    env->SetObjectArrayElement(jObjectArray, 0, lString);

    env->CallStaticVoidMethod(zygoteInit, setApiBlacklistExemptions, jObjectArray);

    env->DeleteLocalRef(lString);
    env->DeleteLocalRef(jObjectArray);
    return true;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnvToVoid venv;
    venv.venv = nullptr;
    jint result = -1;
    JNIEnv *env;
    if (vm->GetEnv(&venv.venv, JNI_VERSION_1_6) != JNI_OK) {
        goto failed;
    }
    env = venv.env;
    if (!setApiBlacklistExemptions(env)) {
        goto failed;
    }
    result = JNI_VERSION_1_6;
    failed:
    return result;
}