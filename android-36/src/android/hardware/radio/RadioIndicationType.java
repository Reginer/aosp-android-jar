/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash f6e4f3bf2ea241a74ffac5643f8941921f0a2b98 --stability vintf --min_sdk_version current --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio-V4-java-source/gen/android/hardware/radio/RadioIndicationType.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio/4/android/hardware/radio/RadioIndicationType.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio;
/** @hide */
public @interface RadioIndicationType {
  public static final int UNSOLICITED = 0;
  public static final int UNSOLICITED_ACK_EXP = 1;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == UNSOLICITED) return "UNSOLICITED";
      if (_aidl_v == UNSOLICITED_ACK_EXP) return "UNSOLICITED_ACK_EXP";
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
