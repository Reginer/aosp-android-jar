/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.sim;
public @interface PersoSubstate {
  public static final int UNKNOWN = 0;
  public static final int IN_PROGRESS = 1;
  public static final int READY = 2;
  public static final int SIM_NETWORK = 3;
  public static final int SIM_NETWORK_SUBSET = 4;
  public static final int SIM_CORPORATE = 5;
  public static final int SIM_SERVICE_PROVIDER = 6;
  public static final int SIM_SIM = 7;
  public static final int SIM_NETWORK_PUK = 8;
  public static final int SIM_NETWORK_SUBSET_PUK = 9;
  public static final int SIM_CORPORATE_PUK = 10;
  public static final int SIM_SERVICE_PROVIDER_PUK = 11;
  public static final int SIM_SIM_PUK = 12;
  public static final int RUIM_NETWORK1 = 13;
  public static final int RUIM_NETWORK2 = 14;
  public static final int RUIM_HRPD = 15;
  public static final int RUIM_CORPORATE = 16;
  public static final int RUIM_SERVICE_PROVIDER = 17;
  public static final int RUIM_RUIM = 18;
  public static final int RUIM_NETWORK1_PUK = 19;
  public static final int RUIM_NETWORK2_PUK = 20;
  public static final int RUIM_HRPD_PUK = 21;
  public static final int RUIM_CORPORATE_PUK = 22;
  public static final int RUIM_SERVICE_PROVIDER_PUK = 23;
  public static final int RUIM_RUIM_PUK = 24;
  public static final int SIM_SPN = 25;
  public static final int SIM_SPN_PUK = 26;
  public static final int SIM_SP_EHPLMN = 27;
  public static final int SIM_SP_EHPLMN_PUK = 28;
  public static final int SIM_ICCID = 29;
  public static final int SIM_ICCID_PUK = 30;
  public static final int SIM_IMPI = 31;
  public static final int SIM_IMPI_PUK = 32;
  public static final int SIM_NS_SP = 33;
  public static final int SIM_NS_SP_PUK = 34;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == UNKNOWN) return "UNKNOWN";
      if (_aidl_v == IN_PROGRESS) return "IN_PROGRESS";
      if (_aidl_v == READY) return "READY";
      if (_aidl_v == SIM_NETWORK) return "SIM_NETWORK";
      if (_aidl_v == SIM_NETWORK_SUBSET) return "SIM_NETWORK_SUBSET";
      if (_aidl_v == SIM_CORPORATE) return "SIM_CORPORATE";
      if (_aidl_v == SIM_SERVICE_PROVIDER) return "SIM_SERVICE_PROVIDER";
      if (_aidl_v == SIM_SIM) return "SIM_SIM";
      if (_aidl_v == SIM_NETWORK_PUK) return "SIM_NETWORK_PUK";
      if (_aidl_v == SIM_NETWORK_SUBSET_PUK) return "SIM_NETWORK_SUBSET_PUK";
      if (_aidl_v == SIM_CORPORATE_PUK) return "SIM_CORPORATE_PUK";
      if (_aidl_v == SIM_SERVICE_PROVIDER_PUK) return "SIM_SERVICE_PROVIDER_PUK";
      if (_aidl_v == SIM_SIM_PUK) return "SIM_SIM_PUK";
      if (_aidl_v == RUIM_NETWORK1) return "RUIM_NETWORK1";
      if (_aidl_v == RUIM_NETWORK2) return "RUIM_NETWORK2";
      if (_aidl_v == RUIM_HRPD) return "RUIM_HRPD";
      if (_aidl_v == RUIM_CORPORATE) return "RUIM_CORPORATE";
      if (_aidl_v == RUIM_SERVICE_PROVIDER) return "RUIM_SERVICE_PROVIDER";
      if (_aidl_v == RUIM_RUIM) return "RUIM_RUIM";
      if (_aidl_v == RUIM_NETWORK1_PUK) return "RUIM_NETWORK1_PUK";
      if (_aidl_v == RUIM_NETWORK2_PUK) return "RUIM_NETWORK2_PUK";
      if (_aidl_v == RUIM_HRPD_PUK) return "RUIM_HRPD_PUK";
      if (_aidl_v == RUIM_CORPORATE_PUK) return "RUIM_CORPORATE_PUK";
      if (_aidl_v == RUIM_SERVICE_PROVIDER_PUK) return "RUIM_SERVICE_PROVIDER_PUK";
      if (_aidl_v == RUIM_RUIM_PUK) return "RUIM_RUIM_PUK";
      if (_aidl_v == SIM_SPN) return "SIM_SPN";
      if (_aidl_v == SIM_SPN_PUK) return "SIM_SPN_PUK";
      if (_aidl_v == SIM_SP_EHPLMN) return "SIM_SP_EHPLMN";
      if (_aidl_v == SIM_SP_EHPLMN_PUK) return "SIM_SP_EHPLMN_PUK";
      if (_aidl_v == SIM_ICCID) return "SIM_ICCID";
      if (_aidl_v == SIM_ICCID_PUK) return "SIM_ICCID_PUK";
      if (_aidl_v == SIM_IMPI) return "SIM_IMPI";
      if (_aidl_v == SIM_IMPI_PUK) return "SIM_IMPI_PUK";
      if (_aidl_v == SIM_NS_SP) return "SIM_NS_SP";
      if (_aidl_v == SIM_NS_SP_PUK) return "SIM_NS_SP_PUK";
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
