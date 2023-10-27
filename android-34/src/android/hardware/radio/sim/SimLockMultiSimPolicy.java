/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.sim;
public @interface SimLockMultiSimPolicy {
  public static final int NO_MULTISIM_POLICY = 0;
  public static final int ONE_VALID_SIM_MUST_BE_PRESENT = 1;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == NO_MULTISIM_POLICY) return "NO_MULTISIM_POLICY";
      if (_aidl_v == ONE_VALID_SIM_MUST_BE_PRESENT) return "ONE_VALID_SIM_MUST_BE_PRESENT";
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
