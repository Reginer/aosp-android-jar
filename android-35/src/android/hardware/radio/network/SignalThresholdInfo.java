/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash c45c122528c07c449ea08f6eacaace17bb7abc38 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V3-java-source/gen/android/hardware/radio/network/SignalThresholdInfo.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/3/android/hardware/radio/network/SignalThresholdInfo.aidl
 */
package android.hardware.radio.network;
/** @hide */
public class SignalThresholdInfo implements android.os.Parcelable
{
  public int signalMeasurement = 0;
  public int hysteresisMs = 0;
  public int hysteresisDb = 0;
  public int[] thresholds;
  public boolean isEnabled = false;
  public int ran;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<SignalThresholdInfo> CREATOR = new android.os.Parcelable.Creator<SignalThresholdInfo>() {
    @Override
    public SignalThresholdInfo createFromParcel(android.os.Parcel _aidl_source) {
      SignalThresholdInfo _aidl_out = new SignalThresholdInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public SignalThresholdInfo[] newArray(int _aidl_size) {
      return new SignalThresholdInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(signalMeasurement);
    _aidl_parcel.writeInt(hysteresisMs);
    _aidl_parcel.writeInt(hysteresisDb);
    _aidl_parcel.writeIntArray(thresholds);
    _aidl_parcel.writeBoolean(isEnabled);
    _aidl_parcel.writeInt(ran);
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
      signalMeasurement = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      hysteresisMs = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      hysteresisDb = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      thresholds = _aidl_parcel.createIntArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isEnabled = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      ran = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int SIGNAL_MEASUREMENT_TYPE_RSSI = 1;
  public static final int SIGNAL_MEASUREMENT_TYPE_RSCP = 2;
  public static final int SIGNAL_MEASUREMENT_TYPE_RSRP = 3;
  public static final int SIGNAL_MEASUREMENT_TYPE_RSRQ = 4;
  public static final int SIGNAL_MEASUREMENT_TYPE_RSSNR = 5;
  public static final int SIGNAL_MEASUREMENT_TYPE_SSRSRP = 6;
  public static final int SIGNAL_MEASUREMENT_TYPE_SSRSRQ = 7;
  public static final int SIGNAL_MEASUREMENT_TYPE_SSSINR = 8;
  public static final int SIGNAL_MEASUREMENT_TYPE_ECNO = 9;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("signalMeasurement: " + (signalMeasurement));
    _aidl_sj.add("hysteresisMs: " + (hysteresisMs));
    _aidl_sj.add("hysteresisDb: " + (hysteresisDb));
    _aidl_sj.add("thresholds: " + (java.util.Arrays.toString(thresholds)));
    _aidl_sj.add("isEnabled: " + (isEnabled));
    _aidl_sj.add("ran: " + (android.hardware.radio.AccessNetwork.$.toString(ran)));
    return "SignalThresholdInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
