/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public @interface RegistrationFailCause {
  public static final int NONE = 0;
  public static final int IMSI_UNKNOWN_IN_HLR = 2;
  public static final int ILLEGAL_MS = 3;
  public static final int IMSI_UNKNOWN_IN_VLR = 4;
  public static final int IMEI_NOT_ACCEPTED = 5;
  public static final int ILLEGAL_ME = 6;
  public static final int GPRS_SERVICES_NOT_ALLOWED = 7;
  public static final int GPRS_AND_NON_GPRS_SERVICES_NOT_ALLOWED = 8;
  public static final int MS_IDENTITY_CANNOT_BE_DERIVED_BY_NETWORK = 9;
  public static final int IMPLICITLY_DETACHED = 10;
  public static final int PLMN_NOT_ALLOWED = 11;
  public static final int LOCATION_AREA_NOT_ALLOWED = 12;
  public static final int ROAMING_NOT_ALLOWED = 13;
  public static final int GPRS_SERVICES_NOT_ALLOWED_IN_PLMN = 14;
  public static final int NO_SUITABLE_CELLS = 15;
  public static final int MSC_TEMPORARILY_NOT_REACHABLE = 15;
  public static final int NETWORK_FAILURE = 17;
  public static final int MAC_FAILURE = 20;
  public static final int SYNC_FAILURE = 21;
  public static final int CONGESTION = 22;
  public static final int GSM_AUTHENTICATION_UNACCEPTABLE = 23;
  public static final int NOT_AUTHORIZED_FOR_THIS_CSG = 25;
  public static final int SMS_PROVIDED_BY_GPRS_IN_ROUTING_AREA = 26;
  public static final int SERVICE_OPTION_NOT_SUPPORTED = 32;
  public static final int SERVICE_OPTION_NOT_SUBSCRIBED = 33;
  public static final int SERVICE_OPTION_TEMPORARILY_OUT_OF_ORDER = 34;
  public static final int CALL_CANNOT_BE_IDENTIFIED = 38;
  public static final int NO_PDP_CONTEXT_ACTIVATED = 40;
  public static final int RETRY_UPON_ENTRY_INTO_NEW_CELL_1 = 48;
  public static final int RETRY_UPON_ENTRY_INTO_NEW_CELL_2 = 49;
  public static final int RETRY_UPON_ENTRY_INTO_NEW_CELL_3 = 50;
  public static final int RETRY_UPON_ENTRY_INTO_NEW_CELL_4 = 51;
  public static final int RETRY_UPON_ENTRY_INTO_NEW_CELL_5 = 52;
  public static final int RETRY_UPON_ENTRY_INTO_NEW_CELL_6 = 53;
  public static final int RETRY_UPON_ENTRY_INTO_NEW_CELL_7 = 54;
  public static final int RETRY_UPON_ENTRY_INTO_NEW_CELL_8 = 55;
  public static final int RETRY_UPON_ENTRY_INTO_NEW_CELL_9 = 56;
  public static final int RETRY_UPON_ENTRY_INTO_NEW_CELL_10 = 57;
  public static final int RETRY_UPON_ENTRY_INTO_NEW_CELL_11 = 58;
  public static final int RETRY_UPON_ENTRY_INTO_NEW_CELL_12 = 59;
  public static final int RETRY_UPON_ENTRY_INTO_NEW_CELL_13 = 60;
  public static final int RETRY_UPON_ENTRY_INTO_NEW_CELL_14 = 61;
  public static final int RETRY_UPON_ENTRY_INTO_NEW_CELL_15 = 62;
  public static final int RETRY_UPON_ENTRY_INTO_NEW_CELL_16 = 63;
  public static final int SEMANTICALLY_INCORRECT_MESSAGE = 95;
  public static final int INVALID_MANDATORY_INFORMATION = 96;
  public static final int MESSAGE_TYPE_NON_EXISTENT_OR_NOT_IMPLEMENTED = 97;
  public static final int MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE = 98;
  public static final int INFORMATION_ELEMENT_NON_EXISTENT_OR_NOT_IMPLEMENTED = 99;
  public static final int CONDITIONAL_IE_ERROR = 100;
  public static final int MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE = 101;
  public static final int PROTOCOL_ERROR_UNSPECIFIED = 111;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == NONE) return "NONE";
      if (_aidl_v == IMSI_UNKNOWN_IN_HLR) return "IMSI_UNKNOWN_IN_HLR";
      if (_aidl_v == ILLEGAL_MS) return "ILLEGAL_MS";
      if (_aidl_v == IMSI_UNKNOWN_IN_VLR) return "IMSI_UNKNOWN_IN_VLR";
      if (_aidl_v == IMEI_NOT_ACCEPTED) return "IMEI_NOT_ACCEPTED";
      if (_aidl_v == ILLEGAL_ME) return "ILLEGAL_ME";
      if (_aidl_v == GPRS_SERVICES_NOT_ALLOWED) return "GPRS_SERVICES_NOT_ALLOWED";
      if (_aidl_v == GPRS_AND_NON_GPRS_SERVICES_NOT_ALLOWED) return "GPRS_AND_NON_GPRS_SERVICES_NOT_ALLOWED";
      if (_aidl_v == MS_IDENTITY_CANNOT_BE_DERIVED_BY_NETWORK) return "MS_IDENTITY_CANNOT_BE_DERIVED_BY_NETWORK";
      if (_aidl_v == IMPLICITLY_DETACHED) return "IMPLICITLY_DETACHED";
      if (_aidl_v == PLMN_NOT_ALLOWED) return "PLMN_NOT_ALLOWED";
      if (_aidl_v == LOCATION_AREA_NOT_ALLOWED) return "LOCATION_AREA_NOT_ALLOWED";
      if (_aidl_v == ROAMING_NOT_ALLOWED) return "ROAMING_NOT_ALLOWED";
      if (_aidl_v == GPRS_SERVICES_NOT_ALLOWED_IN_PLMN) return "GPRS_SERVICES_NOT_ALLOWED_IN_PLMN";
      if (_aidl_v == NO_SUITABLE_CELLS) return "NO_SUITABLE_CELLS";
      if (_aidl_v == MSC_TEMPORARILY_NOT_REACHABLE) return "MSC_TEMPORARILY_NOT_REACHABLE";
      if (_aidl_v == NETWORK_FAILURE) return "NETWORK_FAILURE";
      if (_aidl_v == MAC_FAILURE) return "MAC_FAILURE";
      if (_aidl_v == SYNC_FAILURE) return "SYNC_FAILURE";
      if (_aidl_v == CONGESTION) return "CONGESTION";
      if (_aidl_v == GSM_AUTHENTICATION_UNACCEPTABLE) return "GSM_AUTHENTICATION_UNACCEPTABLE";
      if (_aidl_v == NOT_AUTHORIZED_FOR_THIS_CSG) return "NOT_AUTHORIZED_FOR_THIS_CSG";
      if (_aidl_v == SMS_PROVIDED_BY_GPRS_IN_ROUTING_AREA) return "SMS_PROVIDED_BY_GPRS_IN_ROUTING_AREA";
      if (_aidl_v == SERVICE_OPTION_NOT_SUPPORTED) return "SERVICE_OPTION_NOT_SUPPORTED";
      if (_aidl_v == SERVICE_OPTION_NOT_SUBSCRIBED) return "SERVICE_OPTION_NOT_SUBSCRIBED";
      if (_aidl_v == SERVICE_OPTION_TEMPORARILY_OUT_OF_ORDER) return "SERVICE_OPTION_TEMPORARILY_OUT_OF_ORDER";
      if (_aidl_v == CALL_CANNOT_BE_IDENTIFIED) return "CALL_CANNOT_BE_IDENTIFIED";
      if (_aidl_v == NO_PDP_CONTEXT_ACTIVATED) return "NO_PDP_CONTEXT_ACTIVATED";
      if (_aidl_v == RETRY_UPON_ENTRY_INTO_NEW_CELL_1) return "RETRY_UPON_ENTRY_INTO_NEW_CELL_1";
      if (_aidl_v == RETRY_UPON_ENTRY_INTO_NEW_CELL_2) return "RETRY_UPON_ENTRY_INTO_NEW_CELL_2";
      if (_aidl_v == RETRY_UPON_ENTRY_INTO_NEW_CELL_3) return "RETRY_UPON_ENTRY_INTO_NEW_CELL_3";
      if (_aidl_v == RETRY_UPON_ENTRY_INTO_NEW_CELL_4) return "RETRY_UPON_ENTRY_INTO_NEW_CELL_4";
      if (_aidl_v == RETRY_UPON_ENTRY_INTO_NEW_CELL_5) return "RETRY_UPON_ENTRY_INTO_NEW_CELL_5";
      if (_aidl_v == RETRY_UPON_ENTRY_INTO_NEW_CELL_6) return "RETRY_UPON_ENTRY_INTO_NEW_CELL_6";
      if (_aidl_v == RETRY_UPON_ENTRY_INTO_NEW_CELL_7) return "RETRY_UPON_ENTRY_INTO_NEW_CELL_7";
      if (_aidl_v == RETRY_UPON_ENTRY_INTO_NEW_CELL_8) return "RETRY_UPON_ENTRY_INTO_NEW_CELL_8";
      if (_aidl_v == RETRY_UPON_ENTRY_INTO_NEW_CELL_9) return "RETRY_UPON_ENTRY_INTO_NEW_CELL_9";
      if (_aidl_v == RETRY_UPON_ENTRY_INTO_NEW_CELL_10) return "RETRY_UPON_ENTRY_INTO_NEW_CELL_10";
      if (_aidl_v == RETRY_UPON_ENTRY_INTO_NEW_CELL_11) return "RETRY_UPON_ENTRY_INTO_NEW_CELL_11";
      if (_aidl_v == RETRY_UPON_ENTRY_INTO_NEW_CELL_12) return "RETRY_UPON_ENTRY_INTO_NEW_CELL_12";
      if (_aidl_v == RETRY_UPON_ENTRY_INTO_NEW_CELL_13) return "RETRY_UPON_ENTRY_INTO_NEW_CELL_13";
      if (_aidl_v == RETRY_UPON_ENTRY_INTO_NEW_CELL_14) return "RETRY_UPON_ENTRY_INTO_NEW_CELL_14";
      if (_aidl_v == RETRY_UPON_ENTRY_INTO_NEW_CELL_15) return "RETRY_UPON_ENTRY_INTO_NEW_CELL_15";
      if (_aidl_v == RETRY_UPON_ENTRY_INTO_NEW_CELL_16) return "RETRY_UPON_ENTRY_INTO_NEW_CELL_16";
      if (_aidl_v == SEMANTICALLY_INCORRECT_MESSAGE) return "SEMANTICALLY_INCORRECT_MESSAGE";
      if (_aidl_v == INVALID_MANDATORY_INFORMATION) return "INVALID_MANDATORY_INFORMATION";
      if (_aidl_v == MESSAGE_TYPE_NON_EXISTENT_OR_NOT_IMPLEMENTED) return "MESSAGE_TYPE_NON_EXISTENT_OR_NOT_IMPLEMENTED";
      if (_aidl_v == MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE) return "MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE";
      if (_aidl_v == INFORMATION_ELEMENT_NON_EXISTENT_OR_NOT_IMPLEMENTED) return "INFORMATION_ELEMENT_NON_EXISTENT_OR_NOT_IMPLEMENTED";
      if (_aidl_v == CONDITIONAL_IE_ERROR) return "CONDITIONAL_IE_ERROR";
      if (_aidl_v == MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE) return "MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE";
      if (_aidl_v == PROTOCOL_ERROR_UNSPECIFIED) return "PROTOCOL_ERROR_UNSPECIFIED";
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
