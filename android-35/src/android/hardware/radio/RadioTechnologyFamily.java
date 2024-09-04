/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 58d15e9e2c355be7b3dda6d4d34effd672bfd1cb --stability vintf --min_sdk_version current --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio-V3-java-source/gen/android/hardware/radio/RadioTechnologyFamily.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio/3/android/hardware/radio/RadioTechnologyFamily.aidl
 */
package android.hardware.radio;
/** @hide */
public @interface RadioTechnologyFamily {
  public static final int THREE_GPP = 0;
  public static final int THREE_GPP2 = 1;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == THREE_GPP) return "THREE_GPP";
      if (_aidl_v == THREE_GPP2) return "THREE_GPP2";
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
