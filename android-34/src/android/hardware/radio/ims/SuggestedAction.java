/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.ims;
public @interface SuggestedAction {
  public static final int NONE = 0;
  public static final int TRIGGER_PLMN_BLOCK = 1;
  public static final int TRIGGER_PLMN_BLOCK_WITH_TIMEOUT = 2;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == NONE) return "NONE";
      if (_aidl_v == TRIGGER_PLMN_BLOCK) return "TRIGGER_PLMN_BLOCK";
      if (_aidl_v == TRIGGER_PLMN_BLOCK_WITH_TIMEOUT) return "TRIGGER_PLMN_BLOCK_WITH_TIMEOUT";
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
