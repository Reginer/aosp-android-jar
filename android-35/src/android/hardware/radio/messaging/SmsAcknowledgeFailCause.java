/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 30b0bc0e84679bc3b5ccb3a52da34c47cda6b7eb --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.messaging-V3-java-source/gen/android/hardware/radio/messaging/SmsAcknowledgeFailCause.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.messaging-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.messaging/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.messaging/3/android/hardware/radio/messaging/SmsAcknowledgeFailCause.aidl
 */
package android.hardware.radio.messaging;
/** @hide */
public @interface SmsAcknowledgeFailCause {
  public static final int MEMORY_CAPACITY_EXCEEDED = 211;
  public static final int UNSPECIFIED_ERROR = 255;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == MEMORY_CAPACITY_EXCEEDED) return "MEMORY_CAPACITY_EXCEEDED";
      if (_aidl_v == UNSPECIFIED_ERROR) return "UNSPECIFIED_ERROR";
      return Integer.toString(_aidl_v);
    }
    static String arrayToString(Object _aidl_v) {
      if (_aidl_v == null) return "null";
      Class<?> _aidl_cls = _aidl_v.getClass();
      if (!_aidl_cls.isArray()) throw new IllegalArgumentException("not an array: " + _aidl_v);
      Class<?> comp = _aidl_cls.getComponentType();
      java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "[", "]");
      if (comp.isArray()) {
        for (int _aidl_i = 0; _aidl_i < java.lang.reflect.Array.getLength(_aidl_v); _aidl_i++) {
          _aidl_sj.add(arrayToString(java.lang.reflect.Array.get(_aidl_v, _aidl_i)));
        }
      } else {
        if (_aidl_cls != int[].class) throw new IllegalArgumentException("wrong type: " + _aidl_cls);
        for (int e : (int[]) _aidl_v) {
          _aidl_sj.add(toString(e));
        }
      }
      return _aidl_sj.toString();
    }
  }
}
