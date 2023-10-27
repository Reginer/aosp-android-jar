/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.security.metrics;
/**
 * Atom that encapsulates the repeated fields in key creation events.
 * @hide
 */
public class KeyCreationWithPurposeAndModesInfo implements android.os.Parcelable
{
  public int algorithm;
  public int purpose_bitmap = 0;
  public int padding_mode_bitmap = 0;
  public int digest_bitmap = 0;
  public int block_mode_bitmap = 0;
  public static final android.os.Parcelable.Creator<KeyCreationWithPurposeAndModesInfo> CREATOR = new android.os.Parcelable.Creator<KeyCreationWithPurposeAndModesInfo>() {
    @Override
    public KeyCreationWithPurposeAndModesInfo createFromParcel(android.os.Parcel _aidl_source) {
      KeyCreationWithPurposeAndModesInfo _aidl_out = new KeyCreationWithPurposeAndModesInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public KeyCreationWithPurposeAndModesInfo[] newArray(int _aidl_size) {
      return new KeyCreationWithPurposeAndModesInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(algorithm);
    _aidl_parcel.writeInt(purpose_bitmap);
    _aidl_parcel.writeInt(padding_mode_bitmap);
    _aidl_parcel.writeInt(digest_bitmap);
    _aidl_parcel.writeInt(block_mode_bitmap);
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
      algorithm = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      purpose_bitmap = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      padding_mode_bitmap = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      digest_bitmap = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      block_mode_bitmap = _aidl_parcel.readInt();
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
