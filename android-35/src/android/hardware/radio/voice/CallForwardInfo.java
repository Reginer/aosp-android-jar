/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 78fb79bcb32590a868b3eb7affb39ab90e4ca782 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.voice-V3-java-source/gen/android/hardware/radio/voice/CallForwardInfo.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.voice-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.voice/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.voice/3/android/hardware/radio/voice/CallForwardInfo.aidl
 */
package android.hardware.radio.voice;
/** @hide */
public class CallForwardInfo implements android.os.Parcelable
{
  public int status = 0;
  public int reason = 0;
  public int serviceClass = 0;
  public int toa = 0;
  public java.lang.String number;
  public int timeSeconds = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CallForwardInfo> CREATOR = new android.os.Parcelable.Creator<CallForwardInfo>() {
    @Override
    public CallForwardInfo createFromParcel(android.os.Parcel _aidl_source) {
      CallForwardInfo _aidl_out = new CallForwardInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CallForwardInfo[] newArray(int _aidl_size) {
      return new CallForwardInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(status);
    _aidl_parcel.writeInt(reason);
    _aidl_parcel.writeInt(serviceClass);
    _aidl_parcel.writeInt(toa);
    _aidl_parcel.writeString(number);
    _aidl_parcel.writeInt(timeSeconds);
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
      status = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      reason = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      serviceClass = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      toa = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      number = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      timeSeconds = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int STATUS_DISABLE = 0;
  public static final int STATUS_ENABLE = 1;
  public static final int STATUS_INTERROGATE = 2;
  public static final int STATUS_REGISTRATION = 3;
  public static final int STATUS_ERASURE = 4;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("status: " + (status));
    _aidl_sj.add("reason: " + (reason));
    _aidl_sj.add("serviceClass: " + (serviceClass));
    _aidl_sj.add("toa: " + (toa));
    _aidl_sj.add("number: " + (java.util.Objects.toString(number)));
    _aidl_sj.add("timeSeconds: " + (timeSeconds));
    return "CallForwardInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
