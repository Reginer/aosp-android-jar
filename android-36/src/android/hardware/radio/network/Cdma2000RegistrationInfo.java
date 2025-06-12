/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash 5867b4f5be491ec815fafea8a3f268b0295427df --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V4-java-source/gen/android/hardware/radio/network/Cdma2000RegistrationInfo.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/4/android/hardware/radio/network/Cdma2000RegistrationInfo.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.network;
/** @hide */
public class Cdma2000RegistrationInfo implements android.os.Parcelable
{
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public boolean cssSupported = false;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public int roamingIndicator = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public int systemIsInPrl = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public int defaultRoamingIndicator = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<Cdma2000RegistrationInfo> CREATOR = new android.os.Parcelable.Creator<Cdma2000RegistrationInfo>() {
    @Override
    public Cdma2000RegistrationInfo createFromParcel(android.os.Parcel _aidl_source) {
      Cdma2000RegistrationInfo _aidl_out = new Cdma2000RegistrationInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public Cdma2000RegistrationInfo[] newArray(int _aidl_size) {
      return new Cdma2000RegistrationInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeBoolean(cssSupported);
    _aidl_parcel.writeInt(roamingIndicator);
    _aidl_parcel.writeInt(systemIsInPrl);
    _aidl_parcel.writeInt(defaultRoamingIndicator);
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
      cssSupported = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      roamingIndicator = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      systemIsInPrl = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      defaultRoamingIndicator = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int PRL_INDICATOR_NOT_REGISTERED = -1;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int PRL_INDICATOR_NOT_IN_PRL = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int PRL_INDICATOR_IN_PRL = 1;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("cssSupported: " + (cssSupported));
    _aidl_sj.add("roamingIndicator: " + (roamingIndicator));
    _aidl_sj.add("systemIsInPrl: " + (systemIsInPrl));
    _aidl_sj.add("defaultRoamingIndicator: " + (defaultRoamingIndicator));
    return "Cdma2000RegistrationInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
