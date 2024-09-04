/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 8586a5528f0085c15cff4b6628f1b8153aca29ad --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.modem-V3-java-source/gen/android/hardware/radio/modem/DeviceStateType.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.modem-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.modem/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.modem/3/android/hardware/radio/modem/DeviceStateType.aidl
 */
package android.hardware.radio.modem;
/** @hide */
public @interface DeviceStateType {
  public static final int POWER_SAVE_MODE = 0;
  public static final int CHARGING_STATE = 1;
  public static final int LOW_DATA_EXPECTED = 2;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == POWER_SAVE_MODE) return "POWER_SAVE_MODE";
      if (_aidl_v == CHARGING_STATE) return "CHARGING_STATE";
      if (_aidl_v == LOW_DATA_EXPECTED) return "LOW_DATA_EXPECTED";
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
