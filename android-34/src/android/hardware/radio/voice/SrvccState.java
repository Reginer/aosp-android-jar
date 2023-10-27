/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.voice;
public @interface SrvccState {
  public static final int HANDOVER_STARTED = 0;
  public static final int HANDOVER_COMPLETED = 1;
  public static final int HANDOVER_FAILED = 2;
  public static final int HANDOVER_CANCELED = 3;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == HANDOVER_STARTED) return "HANDOVER_STARTED";
      if (_aidl_v == HANDOVER_COMPLETED) return "HANDOVER_COMPLETED";
      if (_aidl_v == HANDOVER_FAILED) return "HANDOVER_FAILED";
      if (_aidl_v == HANDOVER_CANCELED) return "HANDOVER_CANCELED";
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
