/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash c45c122528c07c449ea08f6eacaace17bb7abc38 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V3-java-source/gen/android/hardware/radio/network/EutranRegistrationInfo.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/3/android/hardware/radio/network/EutranRegistrationInfo.aidl
 */
package android.hardware.radio.network;
/** @hide */
public class EutranRegistrationInfo implements android.os.Parcelable
{
  public android.hardware.radio.network.LteVopsInfo lteVopsInfo;
  public android.hardware.radio.network.NrIndicators nrIndicators;
  public byte lteAttachResultType;
  public int extraInfo = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<EutranRegistrationInfo> CREATOR = new android.os.Parcelable.Creator<EutranRegistrationInfo>() {
    @Override
    public EutranRegistrationInfo createFromParcel(android.os.Parcel _aidl_source) {
      EutranRegistrationInfo _aidl_out = new EutranRegistrationInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public EutranRegistrationInfo[] newArray(int _aidl_size) {
      return new EutranRegistrationInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(lteVopsInfo, _aidl_flag);
    _aidl_parcel.writeTypedObject(nrIndicators, _aidl_flag);
    _aidl_parcel.writeByte(lteAttachResultType);
    _aidl_parcel.writeInt(extraInfo);
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
      lteVopsInfo = _aidl_parcel.readTypedObject(android.hardware.radio.network.LteVopsInfo.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      nrIndicators = _aidl_parcel.readTypedObject(android.hardware.radio.network.NrIndicators.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      lteAttachResultType = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      extraInfo = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int EXTRA_CSFB_NOT_PREFERRED = 1;
  public static final int EXTRA_SMS_ONLY = 2;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("lteVopsInfo: " + (java.util.Objects.toString(lteVopsInfo)));
    _aidl_sj.add("nrIndicators: " + (java.util.Objects.toString(nrIndicators)));
    _aidl_sj.add("lteAttachResultType: " + (lteAttachResultType));
    _aidl_sj.add("extraInfo: " + (extraInfo));
    return "EutranRegistrationInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(lteVopsInfo);
    _mask |= describeContents(nrIndicators);
    return _mask;
  }
  private int describeContents(Object _v) {
    if (_v == null) return 0;
    if (_v instanceof android.os.Parcelable) {
      return ((android.os.Parcelable) _v).describeContents();
    }
    return 0;
  }
  public static @interface AttachResultType {
    public static final byte NONE = 0;
    public static final byte EPS_ONLY = 1;
    public static final byte COMBINED = 2;
  }
}
