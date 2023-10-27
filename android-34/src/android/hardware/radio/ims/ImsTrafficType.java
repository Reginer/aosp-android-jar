/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.ims;
public @interface ImsTrafficType {
  public static final int EMERGENCY = 0;
  public static final int EMERGENCY_SMS = 1;
  public static final int VOICE = 2;
  public static final int VIDEO = 3;
  public static final int SMS = 4;
  public static final int REGISTRATION = 5;
  public static final int UT_XCAP = 6;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == EMERGENCY) return "EMERGENCY";
      if (_aidl_v == EMERGENCY_SMS) return "EMERGENCY_SMS";
      if (_aidl_v == VOICE) return "VOICE";
      if (_aidl_v == VIDEO) return "VIDEO";
      if (_aidl_v == SMS) return "SMS";
      if (_aidl_v == REGISTRATION) return "REGISTRATION";
      if (_aidl_v == UT_XCAP) return "UT_XCAP";
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
