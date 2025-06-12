/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash 576f05d082e9269bcf773b0c9b9112d507ab4b9a --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.voice-V4-java-source/gen/android/hardware/radio/voice/LastCallFailCause.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.voice-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.voice/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.voice/4/android/hardware/radio/voice/LastCallFailCause.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.voice;
/** @hide */
public @interface LastCallFailCause {
  public static final int INVALID = 0;
  public static final int UNOBTAINABLE_NUMBER = 1;
  public static final int NO_ROUTE_TO_DESTINATION = 3;
  public static final int CHANNEL_UNACCEPTABLE = 6;
  public static final int OPERATOR_DETERMINED_BARRING = 8;
  public static final int NORMAL = 16;
  public static final int BUSY = 17;
  public static final int NO_USER_RESPONDING = 18;
  public static final int NO_ANSWER_FROM_USER = 19;
  public static final int CALL_REJECTED = 21;
  public static final int NUMBER_CHANGED = 22;
  public static final int PREEMPTION = 25;
  public static final int DESTINATION_OUT_OF_ORDER = 27;
  public static final int INVALID_NUMBER_FORMAT = 28;
  public static final int FACILITY_REJECTED = 29;
  public static final int RESP_TO_STATUS_ENQUIRY = 30;
  public static final int NORMAL_UNSPECIFIED = 31;
  public static final int CONGESTION = 34;
  public static final int NETWORK_OUT_OF_ORDER = 38;
  public static final int TEMPORARY_FAILURE = 41;
  public static final int SWITCHING_EQUIPMENT_CONGESTION = 42;
  public static final int ACCESS_INFORMATION_DISCARDED = 43;
  public static final int REQUESTED_CIRCUIT_OR_CHANNEL_NOT_AVAILABLE = 44;
  public static final int RESOURCES_UNAVAILABLE_OR_UNSPECIFIED = 47;
  public static final int QOS_UNAVAILABLE = 49;
  public static final int REQUESTED_FACILITY_NOT_SUBSCRIBED = 50;
  public static final int INCOMING_CALLS_BARRED_WITHIN_CUG = 55;
  public static final int BEARER_CAPABILITY_NOT_AUTHORIZED = 57;
  public static final int BEARER_CAPABILITY_UNAVAILABLE = 58;
  public static final int SERVICE_OPTION_NOT_AVAILABLE = 63;
  public static final int BEARER_SERVICE_NOT_IMPLEMENTED = 65;
  public static final int ACM_LIMIT_EXCEEDED = 68;
  public static final int REQUESTED_FACILITY_NOT_IMPLEMENTED = 69;
  public static final int ONLY_DIGITAL_INFORMATION_BEARER_AVAILABLE = 70;
  public static final int SERVICE_OR_OPTION_NOT_IMPLEMENTED = 79;
  public static final int INVALID_TRANSACTION_IDENTIFIER = 81;
  public static final int USER_NOT_MEMBER_OF_CUG = 87;
  public static final int INCOMPATIBLE_DESTINATION = 88;
  public static final int INVALID_TRANSIT_NW_SELECTION = 91;
  public static final int SEMANTICALLY_INCORRECT_MESSAGE = 95;
  public static final int INVALID_MANDATORY_INFORMATION = 96;
  public static final int MESSAGE_TYPE_NON_IMPLEMENTED = 97;
  public static final int MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE = 98;
  public static final int INFORMATION_ELEMENT_NON_EXISTENT = 99;
  public static final int CONDITIONAL_IE_ERROR = 100;
  public static final int MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE = 101;
  public static final int RECOVERY_ON_TIMER_EXPIRED = 102;
  public static final int PROTOCOL_ERROR_UNSPECIFIED = 111;
  public static final int INTERWORKING_UNSPECIFIED = 127;
  public static final int CALL_BARRED = 240;
  public static final int FDN_BLOCKED = 241;
  public static final int IMSI_UNKNOWN_IN_VLR = 242;
  public static final int IMEI_NOT_ACCEPTED = 243;
  public static final int DIAL_MODIFIED_TO_USSD = 244;
  public static final int DIAL_MODIFIED_TO_SS = 245;
  public static final int DIAL_MODIFIED_TO_DIAL = 246;
  public static final int RADIO_OFF = 247;
  public static final int OUT_OF_SERVICE = 248;
  public static final int NO_VALID_SIM = 249;
  public static final int RADIO_INTERNAL_ERROR = 250;
  public static final int NETWORK_RESP_TIMEOUT = 251;
  public static final int NETWORK_REJECT = 252;
  public static final int RADIO_ACCESS_FAILURE = 253;
  public static final int RADIO_LINK_FAILURE = 254;
  public static final int RADIO_LINK_LOST = 255;
  public static final int RADIO_UPLINK_FAILURE = 256;
  public static final int RADIO_SETUP_FAILURE = 257;
  public static final int RADIO_RELEASE_NORMAL = 258;
  public static final int RADIO_RELEASE_ABNORMAL = 259;
  public static final int ACCESS_CLASS_BLOCKED = 260;
  public static final int NETWORK_DETACH = 261;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int CDMA_LOCKED_UNTIL_POWER_CYCLE = 1000;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int CDMA_DROP = 1001;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int CDMA_INTERCEPT = 1002;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int CDMA_REORDER = 1003;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int CDMA_SO_REJECT = 1004;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int CDMA_RETRY_ORDER = 1005;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int CDMA_ACCESS_FAILURE = 1006;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int CDMA_PREEMPTED = 1007;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int CDMA_NOT_EMERGENCY = 1008;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int CDMA_ACCESS_BLOCKED = 1009;
  public static final int OEM_CAUSE_1 = 61441;
  public static final int OEM_CAUSE_2 = 61442;
  public static final int OEM_CAUSE_3 = 61443;
  public static final int OEM_CAUSE_4 = 61444;
  public static final int OEM_CAUSE_5 = 61445;
  public static final int OEM_CAUSE_6 = 61446;
  public static final int OEM_CAUSE_7 = 61447;
  public static final int OEM_CAUSE_8 = 61448;
  public static final int OEM_CAUSE_9 = 61449;
  public static final int OEM_CAUSE_10 = 61450;
  public static final int OEM_CAUSE_11 = 61451;
  public static final int OEM_CAUSE_12 = 61452;
  public static final int OEM_CAUSE_13 = 61453;
  public static final int OEM_CAUSE_14 = 61454;
  public static final int OEM_CAUSE_15 = 61455;
  public static final int ERROR_UNSPECIFIED = 65535;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == INVALID) return "INVALID";
      if (_aidl_v == UNOBTAINABLE_NUMBER) return "UNOBTAINABLE_NUMBER";
      if (_aidl_v == NO_ROUTE_TO_DESTINATION) return "NO_ROUTE_TO_DESTINATION";
      if (_aidl_v == CHANNEL_UNACCEPTABLE) return "CHANNEL_UNACCEPTABLE";
      if (_aidl_v == OPERATOR_DETERMINED_BARRING) return "OPERATOR_DETERMINED_BARRING";
      if (_aidl_v == NORMAL) return "NORMAL";
      if (_aidl_v == BUSY) return "BUSY";
      if (_aidl_v == NO_USER_RESPONDING) return "NO_USER_RESPONDING";
      if (_aidl_v == NO_ANSWER_FROM_USER) return "NO_ANSWER_FROM_USER";
      if (_aidl_v == CALL_REJECTED) return "CALL_REJECTED";
      if (_aidl_v == NUMBER_CHANGED) return "NUMBER_CHANGED";
      if (_aidl_v == PREEMPTION) return "PREEMPTION";
      if (_aidl_v == DESTINATION_OUT_OF_ORDER) return "DESTINATION_OUT_OF_ORDER";
      if (_aidl_v == INVALID_NUMBER_FORMAT) return "INVALID_NUMBER_FORMAT";
      if (_aidl_v == FACILITY_REJECTED) return "FACILITY_REJECTED";
      if (_aidl_v == RESP_TO_STATUS_ENQUIRY) return "RESP_TO_STATUS_ENQUIRY";
      if (_aidl_v == NORMAL_UNSPECIFIED) return "NORMAL_UNSPECIFIED";
      if (_aidl_v == CONGESTION) return "CONGESTION";
      if (_aidl_v == NETWORK_OUT_OF_ORDER) return "NETWORK_OUT_OF_ORDER";
      if (_aidl_v == TEMPORARY_FAILURE) return "TEMPORARY_FAILURE";
      if (_aidl_v == SWITCHING_EQUIPMENT_CONGESTION) return "SWITCHING_EQUIPMENT_CONGESTION";
      if (_aidl_v == ACCESS_INFORMATION_DISCARDED) return "ACCESS_INFORMATION_DISCARDED";
      if (_aidl_v == REQUESTED_CIRCUIT_OR_CHANNEL_NOT_AVAILABLE) return "REQUESTED_CIRCUIT_OR_CHANNEL_NOT_AVAILABLE";
      if (_aidl_v == RESOURCES_UNAVAILABLE_OR_UNSPECIFIED) return "RESOURCES_UNAVAILABLE_OR_UNSPECIFIED";
      if (_aidl_v == QOS_UNAVAILABLE) return "QOS_UNAVAILABLE";
      if (_aidl_v == REQUESTED_FACILITY_NOT_SUBSCRIBED) return "REQUESTED_FACILITY_NOT_SUBSCRIBED";
      if (_aidl_v == INCOMING_CALLS_BARRED_WITHIN_CUG) return "INCOMING_CALLS_BARRED_WITHIN_CUG";
      if (_aidl_v == BEARER_CAPABILITY_NOT_AUTHORIZED) return "BEARER_CAPABILITY_NOT_AUTHORIZED";
      if (_aidl_v == BEARER_CAPABILITY_UNAVAILABLE) return "BEARER_CAPABILITY_UNAVAILABLE";
      if (_aidl_v == SERVICE_OPTION_NOT_AVAILABLE) return "SERVICE_OPTION_NOT_AVAILABLE";
      if (_aidl_v == BEARER_SERVICE_NOT_IMPLEMENTED) return "BEARER_SERVICE_NOT_IMPLEMENTED";
      if (_aidl_v == ACM_LIMIT_EXCEEDED) return "ACM_LIMIT_EXCEEDED";
      if (_aidl_v == REQUESTED_FACILITY_NOT_IMPLEMENTED) return "REQUESTED_FACILITY_NOT_IMPLEMENTED";
      if (_aidl_v == ONLY_DIGITAL_INFORMATION_BEARER_AVAILABLE) return "ONLY_DIGITAL_INFORMATION_BEARER_AVAILABLE";
      if (_aidl_v == SERVICE_OR_OPTION_NOT_IMPLEMENTED) return "SERVICE_OR_OPTION_NOT_IMPLEMENTED";
      if (_aidl_v == INVALID_TRANSACTION_IDENTIFIER) return "INVALID_TRANSACTION_IDENTIFIER";
      if (_aidl_v == USER_NOT_MEMBER_OF_CUG) return "USER_NOT_MEMBER_OF_CUG";
      if (_aidl_v == INCOMPATIBLE_DESTINATION) return "INCOMPATIBLE_DESTINATION";
      if (_aidl_v == INVALID_TRANSIT_NW_SELECTION) return "INVALID_TRANSIT_NW_SELECTION";
      if (_aidl_v == SEMANTICALLY_INCORRECT_MESSAGE) return "SEMANTICALLY_INCORRECT_MESSAGE";
      if (_aidl_v == INVALID_MANDATORY_INFORMATION) return "INVALID_MANDATORY_INFORMATION";
      if (_aidl_v == MESSAGE_TYPE_NON_IMPLEMENTED) return "MESSAGE_TYPE_NON_IMPLEMENTED";
      if (_aidl_v == MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE) return "MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE";
      if (_aidl_v == INFORMATION_ELEMENT_NON_EXISTENT) return "INFORMATION_ELEMENT_NON_EXISTENT";
      if (_aidl_v == CONDITIONAL_IE_ERROR) return "CONDITIONAL_IE_ERROR";
      if (_aidl_v == MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE) return "MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE";
      if (_aidl_v == RECOVERY_ON_TIMER_EXPIRED) return "RECOVERY_ON_TIMER_EXPIRED";
      if (_aidl_v == PROTOCOL_ERROR_UNSPECIFIED) return "PROTOCOL_ERROR_UNSPECIFIED";
      if (_aidl_v == INTERWORKING_UNSPECIFIED) return "INTERWORKING_UNSPECIFIED";
      if (_aidl_v == CALL_BARRED) return "CALL_BARRED";
      if (_aidl_v == FDN_BLOCKED) return "FDN_BLOCKED";
      if (_aidl_v == IMSI_UNKNOWN_IN_VLR) return "IMSI_UNKNOWN_IN_VLR";
      if (_aidl_v == IMEI_NOT_ACCEPTED) return "IMEI_NOT_ACCEPTED";
      if (_aidl_v == DIAL_MODIFIED_TO_USSD) return "DIAL_MODIFIED_TO_USSD";
      if (_aidl_v == DIAL_MODIFIED_TO_SS) return "DIAL_MODIFIED_TO_SS";
      if (_aidl_v == DIAL_MODIFIED_TO_DIAL) return "DIAL_MODIFIED_TO_DIAL";
      if (_aidl_v == RADIO_OFF) return "RADIO_OFF";
      if (_aidl_v == OUT_OF_SERVICE) return "OUT_OF_SERVICE";
      if (_aidl_v == NO_VALID_SIM) return "NO_VALID_SIM";
      if (_aidl_v == RADIO_INTERNAL_ERROR) return "RADIO_INTERNAL_ERROR";
      if (_aidl_v == NETWORK_RESP_TIMEOUT) return "NETWORK_RESP_TIMEOUT";
      if (_aidl_v == NETWORK_REJECT) return "NETWORK_REJECT";
      if (_aidl_v == RADIO_ACCESS_FAILURE) return "RADIO_ACCESS_FAILURE";
      if (_aidl_v == RADIO_LINK_FAILURE) return "RADIO_LINK_FAILURE";
      if (_aidl_v == RADIO_LINK_LOST) return "RADIO_LINK_LOST";
      if (_aidl_v == RADIO_UPLINK_FAILURE) return "RADIO_UPLINK_FAILURE";
      if (_aidl_v == RADIO_SETUP_FAILURE) return "RADIO_SETUP_FAILURE";
      if (_aidl_v == RADIO_RELEASE_NORMAL) return "RADIO_RELEASE_NORMAL";
      if (_aidl_v == RADIO_RELEASE_ABNORMAL) return "RADIO_RELEASE_ABNORMAL";
      if (_aidl_v == ACCESS_CLASS_BLOCKED) return "ACCESS_CLASS_BLOCKED";
      if (_aidl_v == NETWORK_DETACH) return "NETWORK_DETACH";
      if (_aidl_v == CDMA_LOCKED_UNTIL_POWER_CYCLE) return "CDMA_LOCKED_UNTIL_POWER_CYCLE";
      if (_aidl_v == CDMA_DROP) return "CDMA_DROP";
      if (_aidl_v == CDMA_INTERCEPT) return "CDMA_INTERCEPT";
      if (_aidl_v == CDMA_REORDER) return "CDMA_REORDER";
      if (_aidl_v == CDMA_SO_REJECT) return "CDMA_SO_REJECT";
      if (_aidl_v == CDMA_RETRY_ORDER) return "CDMA_RETRY_ORDER";
      if (_aidl_v == CDMA_ACCESS_FAILURE) return "CDMA_ACCESS_FAILURE";
      if (_aidl_v == CDMA_PREEMPTED) return "CDMA_PREEMPTED";
      if (_aidl_v == CDMA_NOT_EMERGENCY) return "CDMA_NOT_EMERGENCY";
      if (_aidl_v == CDMA_ACCESS_BLOCKED) return "CDMA_ACCESS_BLOCKED";
      if (_aidl_v == OEM_CAUSE_1) return "OEM_CAUSE_1";
      if (_aidl_v == OEM_CAUSE_2) return "OEM_CAUSE_2";
      if (_aidl_v == OEM_CAUSE_3) return "OEM_CAUSE_3";
      if (_aidl_v == OEM_CAUSE_4) return "OEM_CAUSE_4";
      if (_aidl_v == OEM_CAUSE_5) return "OEM_CAUSE_5";
      if (_aidl_v == OEM_CAUSE_6) return "OEM_CAUSE_6";
      if (_aidl_v == OEM_CAUSE_7) return "OEM_CAUSE_7";
      if (_aidl_v == OEM_CAUSE_8) return "OEM_CAUSE_8";
      if (_aidl_v == OEM_CAUSE_9) return "OEM_CAUSE_9";
      if (_aidl_v == OEM_CAUSE_10) return "OEM_CAUSE_10";
      if (_aidl_v == OEM_CAUSE_11) return "OEM_CAUSE_11";
      if (_aidl_v == OEM_CAUSE_12) return "OEM_CAUSE_12";
      if (_aidl_v == OEM_CAUSE_13) return "OEM_CAUSE_13";
      if (_aidl_v == OEM_CAUSE_14) return "OEM_CAUSE_14";
      if (_aidl_v == OEM_CAUSE_15) return "OEM_CAUSE_15";
      if (_aidl_v == ERROR_UNSPECIFIED) return "ERROR_UNSPECIFIED";
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
