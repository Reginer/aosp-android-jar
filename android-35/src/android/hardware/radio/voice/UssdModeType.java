/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 78fb79bcb32590a868b3eb7affb39ab90e4ca782 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.voice-V3-java-source/gen/android/hardware/radio/voice/UssdModeType.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.voice-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.voice/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.voice/3/android/hardware/radio/voice/UssdModeType.aidl
 */
package android.hardware.radio.voice;
/** @hide */
public @interface UssdModeType {
  public static final int NOTIFY = 0;
  public static final int REQUEST = 1;
  public static final int NW_RELEASE = 2;
  public static final int LOCAL_CLIENT = 3;
  public static final int NOT_SUPPORTED = 4;
  public static final int NW_TIMEOUT = 5;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == NOTIFY) return "NOTIFY";
      if (_aidl_v == REQUEST) return "REQUEST";
      if (_aidl_v == NW_RELEASE) return "NW_RELEASE";
      if (_aidl_v == LOCAL_CLIENT) return "LOCAL_CLIENT";
      if (_aidl_v == NOT_SUPPORTED) return "NOT_SUPPORTED";
      if (_aidl_v == NW_TIMEOUT) return "NW_TIMEOUT";
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
