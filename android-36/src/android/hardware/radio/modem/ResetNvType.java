/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash 787419262f7c39ea36c0fbe22681bada95d1f97b --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.modem-V4-java-source/gen/android/hardware/radio/modem/ResetNvType.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.modem-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.modem/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.modem/4/android/hardware/radio/modem/ResetNvType.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.modem;
/** @hide */
public @interface ResetNvType {
  public static final int RELOAD = 0;
  /** @deprecated NV APIs are deprecated starting from Android U. */
  @Deprecated
  public static final int ERASE = 1;
  /** @deprecated NV APIs are deprecated starting from Android U. */
  @Deprecated
  public static final int FACTORY_RESET = 2;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == RELOAD) return "RELOAD";
      if (_aidl_v == ERASE) return "ERASE";
      if (_aidl_v == FACTORY_RESET) return "FACTORY_RESET";
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
