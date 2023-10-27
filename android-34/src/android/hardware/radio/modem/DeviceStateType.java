/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.modem;
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
