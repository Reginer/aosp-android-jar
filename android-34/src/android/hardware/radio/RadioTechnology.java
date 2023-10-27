/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio;
public @interface RadioTechnology {
  public static final int UNKNOWN = 0;
  public static final int GPRS = 1;
  public static final int EDGE = 2;
  public static final int UMTS = 3;
  public static final int IS95A = 4;
  public static final int IS95B = 5;
  public static final int ONE_X_RTT = 6;
  public static final int EVDO_0 = 7;
  public static final int EVDO_A = 8;
  public static final int HSDPA = 9;
  public static final int HSUPA = 10;
  public static final int HSPA = 11;
  public static final int EVDO_B = 12;
  public static final int EHRPD = 13;
  public static final int LTE = 14;
  public static final int HSPAP = 15;
  public static final int GSM = 16;
  public static final int TD_SCDMA = 17;
  public static final int IWLAN = 18;
  /** @deprecated use LTE instead and indicate carrier aggregation through multiple physical channel configurations in IRadioNetwork::currentPhysicalChannelConfigs. */
  @Deprecated
  public static final int LTE_CA = 19;
  public static final int NR = 20;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == UNKNOWN) return "UNKNOWN";
      if (_aidl_v == GPRS) return "GPRS";
      if (_aidl_v == EDGE) return "EDGE";
      if (_aidl_v == UMTS) return "UMTS";
      if (_aidl_v == IS95A) return "IS95A";
      if (_aidl_v == IS95B) return "IS95B";
      if (_aidl_v == ONE_X_RTT) return "ONE_X_RTT";
      if (_aidl_v == EVDO_0) return "EVDO_0";
      if (_aidl_v == EVDO_A) return "EVDO_A";
      if (_aidl_v == HSDPA) return "HSDPA";
      if (_aidl_v == HSUPA) return "HSUPA";
      if (_aidl_v == HSPA) return "HSPA";
      if (_aidl_v == EVDO_B) return "EVDO_B";
      if (_aidl_v == EHRPD) return "EHRPD";
      if (_aidl_v == LTE) return "LTE";
      if (_aidl_v == HSPAP) return "HSPAP";
      if (_aidl_v == GSM) return "GSM";
      if (_aidl_v == TD_SCDMA) return "TD_SCDMA";
      if (_aidl_v == IWLAN) return "IWLAN";
      if (_aidl_v == LTE_CA) return "LTE_CA";
      if (_aidl_v == NR) return "NR";
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
