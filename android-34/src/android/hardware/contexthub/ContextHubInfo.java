/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.contexthub;
public class ContextHubInfo implements android.os.Parcelable
{
  public java.lang.String name;
  public java.lang.String vendor;
  public java.lang.String toolchain;
  public int id = 0;
  public float peakMips = 0.000000f;
  public int maxSupportedMessageLengthBytes = 0;
  public long chrePlatformId = 0L;
  public byte chreApiMajorVersion = 0;
  public byte chreApiMinorVersion = 0;
  public char chrePatchVersion = '\0';
  public java.lang.String[] supportedPermissions;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<ContextHubInfo> CREATOR = new android.os.Parcelable.Creator<ContextHubInfo>() {
    @Override
    public ContextHubInfo createFromParcel(android.os.Parcel _aidl_source) {
      ContextHubInfo _aidl_out = new ContextHubInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ContextHubInfo[] newArray(int _aidl_size) {
      return new ContextHubInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(name);
    _aidl_parcel.writeString(vendor);
    _aidl_parcel.writeString(toolchain);
    _aidl_parcel.writeInt(id);
    _aidl_parcel.writeFloat(peakMips);
    _aidl_parcel.writeInt(maxSupportedMessageLengthBytes);
    _aidl_parcel.writeLong(chrePlatformId);
    _aidl_parcel.writeByte(chreApiMajorVersion);
    _aidl_parcel.writeByte(chreApiMinorVersion);
    _aidl_parcel.writeInt(((int)chrePatchVersion));
    _aidl_parcel.writeStringArray(supportedPermissions);
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
      name = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      vendor = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      toolchain = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      id = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      peakMips = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxSupportedMessageLengthBytes = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      chrePlatformId = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      chreApiMajorVersion = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      chreApiMinorVersion = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      chrePatchVersion = (char)_aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      supportedPermissions = _aidl_parcel.createStringArray();
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
