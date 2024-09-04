/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 1e3dcfffc1e90fc886cf5a22ecaa94601b115710 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.config-V3-java-source/gen/android/hardware/radio/config/PhoneCapability.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.config-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.config/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.config/3/android/hardware/radio/config/PhoneCapability.aidl
 */
package android.hardware.radio.config;
/** @hide */
public class PhoneCapability implements android.os.Parcelable
{
  public byte maxActiveData = 0;
  public byte maxActiveInternetData = 0;
  public boolean isInternetLingeringSupported = false;
  public byte[] logicalModemIds;
  public byte maxActiveVoice = -1;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<PhoneCapability> CREATOR = new android.os.Parcelable.Creator<PhoneCapability>() {
    @Override
    public PhoneCapability createFromParcel(android.os.Parcel _aidl_source) {
      PhoneCapability _aidl_out = new PhoneCapability();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public PhoneCapability[] newArray(int _aidl_size) {
      return new PhoneCapability[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeByte(maxActiveData);
    _aidl_parcel.writeByte(maxActiveInternetData);
    _aidl_parcel.writeBoolean(isInternetLingeringSupported);
    _aidl_parcel.writeByteArray(logicalModemIds);
    _aidl_parcel.writeByte(maxActiveVoice);
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
      maxActiveData = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxActiveInternetData = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isInternetLingeringSupported = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      logicalModemIds = _aidl_parcel.createByteArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxActiveVoice = _aidl_parcel.readByte();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final byte UNKNOWN = -1;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("maxActiveData: " + (maxActiveData));
    _aidl_sj.add("maxActiveInternetData: " + (maxActiveInternetData));
    _aidl_sj.add("isInternetLingeringSupported: " + (isInternetLingeringSupported));
    _aidl_sj.add("logicalModemIds: " + (java.util.Arrays.toString(logicalModemIds)));
    _aidl_sj.add("maxActiveVoice: " + (maxActiveVoice));
    return "PhoneCapability" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
