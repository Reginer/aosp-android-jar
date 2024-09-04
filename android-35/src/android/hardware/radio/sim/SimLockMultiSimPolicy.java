/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash ea7be3035be8d4869237a6478d2e0bb0efcc1e87 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.config_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.sim-V3-java-source/gen/android/hardware/radio/sim/SimLockMultiSimPolicy.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.sim-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.sim/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.sim/3/android/hardware/radio/sim/SimLockMultiSimPolicy.aidl
 */
package android.hardware.radio.sim;
/** @hide */
public @interface SimLockMultiSimPolicy {
  public static final int NO_MULTISIM_POLICY = 0;
  public static final int ONE_VALID_SIM_MUST_BE_PRESENT = 1;
  public static final int APPLY_TO_ALL_SLOTS = 2;
  public static final int APPLY_TO_ONLY_SLOT_1 = 3;
  public static final int VALID_SIM_MUST_PRESENT_ON_SLOT_1 = 4;
  public static final int ACTIVE_SERVICE_ON_SLOT_1_TO_UNBLOCK_OTHER_SLOTS = 5;
  public static final int ACTIVE_SERVICE_ON_ANY_SLOT_TO_UNBLOCK_OTHER_SLOTS = 6;
  public static final int ALL_SIMS_MUST_BE_VALID = 7;
  public static final int SLOT_POLICY_OTHER = 8;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == NO_MULTISIM_POLICY) return "NO_MULTISIM_POLICY";
      if (_aidl_v == ONE_VALID_SIM_MUST_BE_PRESENT) return "ONE_VALID_SIM_MUST_BE_PRESENT";
      if (_aidl_v == APPLY_TO_ALL_SLOTS) return "APPLY_TO_ALL_SLOTS";
      if (_aidl_v == APPLY_TO_ONLY_SLOT_1) return "APPLY_TO_ONLY_SLOT_1";
      if (_aidl_v == VALID_SIM_MUST_PRESENT_ON_SLOT_1) return "VALID_SIM_MUST_PRESENT_ON_SLOT_1";
      if (_aidl_v == ACTIVE_SERVICE_ON_SLOT_1_TO_UNBLOCK_OTHER_SLOTS) return "ACTIVE_SERVICE_ON_SLOT_1_TO_UNBLOCK_OTHER_SLOTS";
      if (_aidl_v == ACTIVE_SERVICE_ON_ANY_SLOT_TO_UNBLOCK_OTHER_SLOTS) return "ACTIVE_SERVICE_ON_ANY_SLOT_TO_UNBLOCK_OTHER_SLOTS";
      if (_aidl_v == ALL_SIMS_MUST_BE_VALID) return "ALL_SIMS_MUST_BE_VALID";
      if (_aidl_v == SLOT_POLICY_OTHER) return "SLOT_POLICY_OTHER";
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
