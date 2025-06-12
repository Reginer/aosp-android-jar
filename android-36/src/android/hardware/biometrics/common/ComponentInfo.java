/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash 8a6cd86630181a4df6f20056259ec200ffe39209 -t --stability vintf --min_sdk_version platform_apis --ninja -d out/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common-V4-java-source/gen/android/hardware/biometrics/common/ComponentInfo.java.d -o out/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common-V4-java-source/gen -Nhardware/interfaces/biometrics/common/aidl/aidl_api/android.hardware.biometrics.common/4 hardware/interfaces/biometrics/common/aidl/aidl_api/android.hardware.biometrics.common/4/android/hardware/biometrics/common/ComponentInfo.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.biometrics.common;
/** @hide */
public class ComponentInfo implements android.os.Parcelable
{
  public java.lang.String componentId;
  public java.lang.String hardwareVersion;
  public java.lang.String firmwareVersion;
  public java.lang.String serialNumber;
  public java.lang.String softwareVersion;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<ComponentInfo> CREATOR = new android.os.Parcelable.Creator<ComponentInfo>() {
    @Override
    public ComponentInfo createFromParcel(android.os.Parcel _aidl_source) {
      ComponentInfo _aidl_out = new ComponentInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ComponentInfo[] newArray(int _aidl_size) {
      return new ComponentInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(componentId);
    _aidl_parcel.writeString(hardwareVersion);
    _aidl_parcel.writeString(firmwareVersion);
    _aidl_parcel.writeString(serialNumber);
    _aidl_parcel.writeString(softwareVersion);
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
      componentId = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      hardwareVersion = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      firmwareVersion = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      serialNumber = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      softwareVersion = _aidl_parcel.readString();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
