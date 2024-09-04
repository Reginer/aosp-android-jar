/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 2 --hash ec0dfedf764f3916783848c540ad312a74fa755d --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.ims-V2-java-source/gen/android/hardware/radio/ims/ImsTrafficType.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.ims-V2-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.ims/2 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.ims/2/android/hardware/radio/ims/ImsTrafficType.aidl
 */
package android.hardware.radio.ims;
/** @hide */
public @interface ImsTrafficType {
  public static final int EMERGENCY = 0;
  public static final int EMERGENCY_SMS = 1;
  public static final int VOICE = 2;
  public static final int VIDEO = 3;
  public static final int SMS = 4;
  public static final int REGISTRATION = 5;
  public static final int UT_XCAP = 6;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == EMERGENCY) return "EMERGENCY";
      if (_aidl_v == EMERGENCY_SMS) return "EMERGENCY_SMS";
      if (_aidl_v == VOICE) return "VOICE";
      if (_aidl_v == VIDEO) return "VIDEO";
      if (_aidl_v == SMS) return "SMS";
      if (_aidl_v == REGISTRATION) return "REGISTRATION";
      if (_aidl_v == UT_XCAP) return "UT_XCAP";
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
