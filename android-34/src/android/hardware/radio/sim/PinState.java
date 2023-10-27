/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.sim;
public @interface PinState {
  public static final int UNKNOWN = 0;
  public static final int ENABLED_NOT_VERIFIED = 1;
  public static final int ENABLED_VERIFIED = 2;
  public static final int DISABLED = 3;
  public static final int ENABLED_BLOCKED = 4;
  public static final int ENABLED_PERM_BLOCKED = 5;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == UNKNOWN) return "UNKNOWN";
      if (_aidl_v == ENABLED_NOT_VERIFIED) return "ENABLED_NOT_VERIFIED";
      if (_aidl_v == ENABLED_VERIFIED) return "ENABLED_VERIFIED";
      if (_aidl_v == DISABLED) return "DISABLED";
      if (_aidl_v == ENABLED_BLOCKED) return "ENABLED_BLOCKED";
      if (_aidl_v == ENABLED_PERM_BLOCKED) return "ENABLED_PERM_BLOCKED";
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
