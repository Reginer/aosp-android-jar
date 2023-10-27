/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public @interface IndicationFilter {
  public static final int NONE = 0;
  public static final int ALL = -1;
  public static final int SIGNAL_STRENGTH = 1;
  public static final int FULL_NETWORK_STATE = 2;
  public static final int DATA_CALL_DORMANCY_CHANGED = 4;
  public static final int LINK_CAPACITY_ESTIMATE = 8;
  public static final int PHYSICAL_CHANNEL_CONFIG = 16;
  public static final int REGISTRATION_FAILURE = 32;
  public static final int BARRING_INFO = 64;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == NONE) return "NONE";
      if (_aidl_v == ALL) return "ALL";
      if (_aidl_v == SIGNAL_STRENGTH) return "SIGNAL_STRENGTH";
      if (_aidl_v == FULL_NETWORK_STATE) return "FULL_NETWORK_STATE";
      if (_aidl_v == DATA_CALL_DORMANCY_CHANGED) return "DATA_CALL_DORMANCY_CHANGED";
      if (_aidl_v == LINK_CAPACITY_ESTIMATE) return "LINK_CAPACITY_ESTIMATE";
      if (_aidl_v == PHYSICAL_CHANNEL_CONFIG) return "PHYSICAL_CHANNEL_CONFIG";
      if (_aidl_v == REGISTRATION_FAILURE) return "REGISTRATION_FAILURE";
      if (_aidl_v == BARRING_INFO) return "BARRING_INFO";
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
