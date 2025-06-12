/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash 576f05d082e9269bcf773b0c9b9112d507ab4b9a --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.voice-V4-java-source/gen/android/hardware/radio/voice/CdmaOtaProvisionStatus.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.voice-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.voice/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.voice/4/android/hardware/radio/voice/CdmaOtaProvisionStatus.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.voice;
/** @hide */
public @interface CdmaOtaProvisionStatus {
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int SPL_UNLOCKED = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int SPC_RETRIES_EXCEEDED = 1;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int A_KEY_EXCHANGED = 2;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int SSD_UPDATED = 3;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NAM_DOWNLOADED = 4;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int MDN_DOWNLOADED = 5;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int IMSI_DOWNLOADED = 6;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int PRL_DOWNLOADED = 7;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int COMMITTED = 8;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int OTAPA_STARTED = 9;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int OTAPA_STOPPED = 10;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int OTAPA_ABORTED = 11;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == SPL_UNLOCKED) return "SPL_UNLOCKED";
      if (_aidl_v == SPC_RETRIES_EXCEEDED) return "SPC_RETRIES_EXCEEDED";
      if (_aidl_v == A_KEY_EXCHANGED) return "A_KEY_EXCHANGED";
      if (_aidl_v == SSD_UPDATED) return "SSD_UPDATED";
      if (_aidl_v == NAM_DOWNLOADED) return "NAM_DOWNLOADED";
      if (_aidl_v == MDN_DOWNLOADED) return "MDN_DOWNLOADED";
      if (_aidl_v == IMSI_DOWNLOADED) return "IMSI_DOWNLOADED";
      if (_aidl_v == PRL_DOWNLOADED) return "PRL_DOWNLOADED";
      if (_aidl_v == COMMITTED) return "COMMITTED";
      if (_aidl_v == OTAPA_STARTED) return "OTAPA_STARTED";
      if (_aidl_v == OTAPA_STOPPED) return "OTAPA_STOPPED";
      if (_aidl_v == OTAPA_ABORTED) return "OTAPA_ABORTED";
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
