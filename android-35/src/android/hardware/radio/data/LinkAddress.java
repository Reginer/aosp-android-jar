/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash cd8913a3f9d39f1cc0a5fcf9e90257be94ec38df --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.data-V3-java-source/gen/android/hardware/radio/data/LinkAddress.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.data-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.data/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.data/3/android/hardware/radio/data/LinkAddress.aidl
 */
package android.hardware.radio.data;
/** @hide */
public class LinkAddress implements android.os.Parcelable
{
  public java.lang.String address;
  public int addressProperties = 0;
  public long deprecationTime = 0L;
  public long expirationTime = 0L;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<LinkAddress> CREATOR = new android.os.Parcelable.Creator<LinkAddress>() {
    @Override
    public LinkAddress createFromParcel(android.os.Parcel _aidl_source) {
      LinkAddress _aidl_out = new LinkAddress();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public LinkAddress[] newArray(int _aidl_size) {
      return new LinkAddress[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(address);
    _aidl_parcel.writeInt(addressProperties);
    _aidl_parcel.writeLong(deprecationTime);
    _aidl_parcel.writeLong(expirationTime);
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
      address = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      addressProperties = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      deprecationTime = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      expirationTime = _aidl_parcel.readLong();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int ADDRESS_PROPERTY_NONE = 0;
  public static final int ADDRESS_PROPERTY_DEPRECATED = 32;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("address: " + (java.util.Objects.toString(address)));
    _aidl_sj.add("addressProperties: " + (addressProperties));
    _aidl_sj.add("deprecationTime: " + (deprecationTime));
    _aidl_sj.add("expirationTime: " + (expirationTime));
    return "LinkAddress" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
