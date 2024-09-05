/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 30b0bc0e84679bc3b5ccb3a52da34c47cda6b7eb --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.messaging-V3-java-source/gen/android/hardware/radio/messaging/CdmaBroadcastSmsConfigInfo.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.messaging-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.messaging/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.messaging/3/android/hardware/radio/messaging/CdmaBroadcastSmsConfigInfo.aidl
 */
package android.hardware.radio.messaging;
/** @hide */
public class CdmaBroadcastSmsConfigInfo implements android.os.Parcelable
{
  public int serviceCategory = 0;
  public int language = 0;
  public boolean selected = false;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CdmaBroadcastSmsConfigInfo> CREATOR = new android.os.Parcelable.Creator<CdmaBroadcastSmsConfigInfo>() {
    @Override
    public CdmaBroadcastSmsConfigInfo createFromParcel(android.os.Parcel _aidl_source) {
      CdmaBroadcastSmsConfigInfo _aidl_out = new CdmaBroadcastSmsConfigInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CdmaBroadcastSmsConfigInfo[] newArray(int _aidl_size) {
      return new CdmaBroadcastSmsConfigInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(serviceCategory);
    _aidl_parcel.writeInt(language);
    _aidl_parcel.writeBoolean(selected);
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
      serviceCategory = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      language = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      selected = _aidl_parcel.readBoolean();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("serviceCategory: " + (serviceCategory));
    _aidl_sj.add("language: " + (language));
    _aidl_sj.add("selected: " + (selected));
    return "CdmaBroadcastSmsConfigInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}