/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.voice;
public @interface UssdModeType {
  public static final int NOTIFY = 0;
  public static final int REQUEST = 1;
  public static final int NW_RELEASE = 2;
  public static final int LOCAL_CLIENT = 3;
  public static final int NOT_SUPPORTED = 4;
  public static final int NW_TIMEOUT = 5;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == NOTIFY) return "NOTIFY";
      if (_aidl_v == REQUEST) return "REQUEST";
      if (_aidl_v == NW_RELEASE) return "NW_RELEASE";
      if (_aidl_v == LOCAL_CLIENT) return "LOCAL_CLIENT";
      if (_aidl_v == NOT_SUPPORTED) return "NOT_SUPPORTED";
      if (_aidl_v == NW_TIMEOUT) return "NW_TIMEOUT";
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
