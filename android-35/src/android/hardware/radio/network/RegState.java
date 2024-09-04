/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash c45c122528c07c449ea08f6eacaace17bb7abc38 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V3-java-source/gen/android/hardware/radio/network/RegState.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/3/android/hardware/radio/network/RegState.aidl
 */
package android.hardware.radio.network;
/** @hide */
public @interface RegState {
  public static final int NOT_REG_MT_NOT_SEARCHING_OP = 0;
  public static final int REG_HOME = 1;
  public static final int NOT_REG_MT_SEARCHING_OP = 2;
  public static final int REG_DENIED = 3;
  public static final int UNKNOWN = 4;
  public static final int REG_ROAMING = 5;
  public static final int NOT_REG_MT_NOT_SEARCHING_OP_EM = 10;
  public static final int NOT_REG_MT_SEARCHING_OP_EM = 12;
  public static final int REG_DENIED_EM = 13;
  public static final int UNKNOWN_EM = 14;
  public static final int REG_EM = 20;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == NOT_REG_MT_NOT_SEARCHING_OP) return "NOT_REG_MT_NOT_SEARCHING_OP";
      if (_aidl_v == REG_HOME) return "REG_HOME";
      if (_aidl_v == NOT_REG_MT_SEARCHING_OP) return "NOT_REG_MT_SEARCHING_OP";
      if (_aidl_v == REG_DENIED) return "REG_DENIED";
      if (_aidl_v == UNKNOWN) return "UNKNOWN";
      if (_aidl_v == REG_ROAMING) return "REG_ROAMING";
      if (_aidl_v == NOT_REG_MT_NOT_SEARCHING_OP_EM) return "NOT_REG_MT_NOT_SEARCHING_OP_EM";
      if (_aidl_v == NOT_REG_MT_SEARCHING_OP_EM) return "NOT_REG_MT_SEARCHING_OP_EM";
      if (_aidl_v == REG_DENIED_EM) return "REG_DENIED_EM";
      if (_aidl_v == UNKNOWN_EM) return "UNKNOWN_EM";
      if (_aidl_v == REG_EM) return "REG_EM";
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
