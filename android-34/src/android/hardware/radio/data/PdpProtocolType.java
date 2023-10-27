/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.data;
public @interface PdpProtocolType {
  public static final int UNKNOWN = -1;
  public static final int IP = 0;
  public static final int IPV6 = 1;
  public static final int IPV4V6 = 2;
  public static final int PPP = 3;
  public static final int NON_IP = 4;
  public static final int UNSTRUCTURED = 5;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == UNKNOWN) return "UNKNOWN";
      if (_aidl_v == IP) return "IP";
      if (_aidl_v == IPV6) return "IPV6";
      if (_aidl_v == IPV4V6) return "IPV4V6";
      if (_aidl_v == PPP) return "PPP";
      if (_aidl_v == NON_IP) return "NON_IP";
      if (_aidl_v == UNSTRUCTURED) return "UNSTRUCTURED";
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
