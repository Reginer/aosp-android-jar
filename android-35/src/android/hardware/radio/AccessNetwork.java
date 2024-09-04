/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 58d15e9e2c355be7b3dda6d4d34effd672bfd1cb --stability vintf --min_sdk_version current --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio-V3-java-source/gen/android/hardware/radio/AccessNetwork.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio/3/android/hardware/radio/AccessNetwork.aidl
 */
package android.hardware.radio;
/** @hide */
public @interface AccessNetwork {
  public static final int UNKNOWN = 0;
  public static final int GERAN = 1;
  public static final int UTRAN = 2;
  public static final int EUTRAN = 3;
  public static final int CDMA2000 = 4;
  public static final int IWLAN = 5;
  public static final int NGRAN = 6;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == UNKNOWN) return "UNKNOWN";
      if (_aidl_v == GERAN) return "GERAN";
      if (_aidl_v == UTRAN) return "UTRAN";
      if (_aidl_v == EUTRAN) return "EUTRAN";
      if (_aidl_v == CDMA2000) return "CDMA2000";
      if (_aidl_v == IWLAN) return "IWLAN";
      if (_aidl_v == NGRAN) return "NGRAN";
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
