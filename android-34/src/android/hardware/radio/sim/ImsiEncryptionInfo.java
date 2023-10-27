/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.sim;
public class ImsiEncryptionInfo implements android.os.Parcelable
{
  public java.lang.String mcc;
  public java.lang.String mnc;
  public byte[] carrierKey;
  public java.lang.String keyIdentifier;
  public long expirationTime = 0L;
  public byte keyType = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<ImsiEncryptionInfo> CREATOR = new android.os.Parcelable.Creator<ImsiEncryptionInfo>() {
    @Override
    public ImsiEncryptionInfo createFromParcel(android.os.Parcel _aidl_source) {
      ImsiEncryptionInfo _aidl_out = new ImsiEncryptionInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ImsiEncryptionInfo[] newArray(int _aidl_size) {
      return new ImsiEncryptionInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(mcc);
    _aidl_parcel.writeString(mnc);
    _aidl_parcel.writeByteArray(carrierKey);
    _aidl_parcel.writeString(keyIdentifier);
    _aidl_parcel.writeLong(expirationTime);
    _aidl_parcel.writeByte(keyType);
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
      mcc = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mnc = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      carrierKey = _aidl_parcel.createByteArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      keyIdentifier = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      expirationTime = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      keyType = _aidl_parcel.readByte();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final byte PUBLIC_KEY_TYPE_EPDG = 1;
  public static final byte PUBLIC_KEY_TYPE_WLAN = 2;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("mcc: " + (java.util.Objects.toString(mcc)));
    _aidl_sj.add("mnc: " + (java.util.Objects.toString(mnc)));
    _aidl_sj.add("carrierKey: " + (java.util.Arrays.toString(carrierKey)));
    _aidl_sj.add("keyIdentifier: " + (java.util.Objects.toString(keyIdentifier)));
    _aidl_sj.add("expirationTime: " + (expirationTime));
    _aidl_sj.add("keyType: " + (keyType));
    return "android.hardware.radio.sim.ImsiEncryptionInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
