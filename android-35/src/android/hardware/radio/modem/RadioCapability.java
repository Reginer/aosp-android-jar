/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 8586a5528f0085c15cff4b6628f1b8153aca29ad --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.modem-V3-java-source/gen/android/hardware/radio/modem/RadioCapability.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.modem-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.modem/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.modem/3/android/hardware/radio/modem/RadioCapability.aidl
 */
package android.hardware.radio.modem;
/** @hide */
public class RadioCapability implements android.os.Parcelable
{
  public int session = 0;
  public int phase = 0;
  public int raf = 0;
  public java.lang.String logicalModemUuid;
  public int status = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<RadioCapability> CREATOR = new android.os.Parcelable.Creator<RadioCapability>() {
    @Override
    public RadioCapability createFromParcel(android.os.Parcel _aidl_source) {
      RadioCapability _aidl_out = new RadioCapability();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public RadioCapability[] newArray(int _aidl_size) {
      return new RadioCapability[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(session);
    _aidl_parcel.writeInt(phase);
    _aidl_parcel.writeInt(raf);
    _aidl_parcel.writeString(logicalModemUuid);
    _aidl_parcel.writeInt(status);
    int _aidl_end_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.setDataPosition(_aidl_start_pos);
    _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
    _aidl_parcel.setDataPosition(_aidl_end_pos);
  }
  public final void readFromParcel(android.os.Parcel _aidl_parcel)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    int _aidl_parcelable_size = _aidl_parcel.readInt();
    try {
      if (_aidl_parcelable_size < 4) throw new android.os.BadParcelableException("Parcelable too small");;
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      session = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      phase = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      raf = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      logicalModemUuid = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      status = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int PHASE_CONFIGURED = 0;
  public static final int PHASE_START = 1;
  public static final int PHASE_APPLY = 2;
  public static final int PHASE_UNSOL_RSP = 3;
  public static final int PHASE_FINISH = 4;
  public static final int STATUS_NONE = 0;
  public static final int STATUS_SUCCESS = 1;
  public static final int STATUS_FAIL = 2;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("session: " + (session));
    _aidl_sj.add("phase: " + (phase));
    _aidl_sj.add("raf: " + (raf));
    _aidl_sj.add("logicalModemUuid: " + (java.util.Objects.toString(logicalModemUuid)));
    _aidl_sj.add("status: " + (status));
    return "RadioCapability" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
