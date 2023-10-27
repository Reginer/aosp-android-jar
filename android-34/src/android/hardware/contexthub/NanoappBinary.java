/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.contexthub;
public class NanoappBinary implements android.os.Parcelable
{
  public long nanoappId = 0L;
  public int nanoappVersion = 0;
  public int flags = 0;
  public byte targetChreApiMajorVersion = 0;
  public byte targetChreApiMinorVersion = 0;
  public byte[] customBinary;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<NanoappBinary> CREATOR = new android.os.Parcelable.Creator<NanoappBinary>() {
    @Override
    public NanoappBinary createFromParcel(android.os.Parcel _aidl_source) {
      NanoappBinary _aidl_out = new NanoappBinary();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public NanoappBinary[] newArray(int _aidl_size) {
      return new NanoappBinary[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeLong(nanoappId);
    _aidl_parcel.writeInt(nanoappVersion);
    _aidl_parcel.writeInt(flags);
    _aidl_parcel.writeByte(targetChreApiMajorVersion);
    _aidl_parcel.writeByte(targetChreApiMinorVersion);
    _aidl_parcel.writeByteArray(customBinary);
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
      nanoappId = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      nanoappVersion = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      flags = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      targetChreApiMajorVersion = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      targetChreApiMinorVersion = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      customBinary = _aidl_parcel.createByteArray();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int FLAG_SIGNED = 1;
  public static final int FLAG_ENCRYPTED = 2;
  public static final int FLAG_TCM_CAPABLE = 4;
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
