/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.data;
public @interface DataThrottlingAction {
  public static final byte NO_DATA_THROTTLING = 0;
  public static final byte THROTTLE_SECONDARY_CARRIER = 1;
  public static final byte THROTTLE_ANCHOR_CARRIER = 2;
  public static final byte HOLD = 3;
  interface $ {
    static String toString(byte _aidl_v) {
      if (_aidl_v == NO_DATA_THROTTLING) return "NO_DATA_THROTTLING";
      if (_aidl_v == THROTTLE_SECONDARY_CARRIER) return "THROTTLE_SECONDARY_CARRIER";
      if (_aidl_v == THROTTLE_ANCHOR_CARRIER) return "THROTTLE_ANCHOR_CARRIER";
      if (_aidl_v == HOLD) return "HOLD";
      return Byte.toString(_aidl_v);
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
        if (_aidl_cls != byte[].class) throw new IllegalArgumentException("wrong type: " + _aidl_cls);
        for (byte e : (byte[]) _aidl_v) {
          _aidl_sj.add(toString(e));
        }
      }
      return _aidl_sj.toString();
    }
  }
}
