/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.ims;
public @interface ImsDeregistrationReason {
  public static final int REASON_SIM_REMOVED = 1;
  public static final int REASON_SIM_REFRESH = 2;
  public static final int REASON_ALLOWED_NETWORK_TYPES_CHANGED = 3;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == REASON_SIM_REMOVED) return "REASON_SIM_REMOVED";
      if (_aidl_v == REASON_SIM_REFRESH) return "REASON_SIM_REFRESH";
      if (_aidl_v == REASON_ALLOWED_NETWORK_TYPES_CHANGED) return "REASON_ALLOWED_NETWORK_TYPES_CHANGED";
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
