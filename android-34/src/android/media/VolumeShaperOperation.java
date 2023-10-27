/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/** {@hide} */
public class VolumeShaperOperation implements android.os.Parcelable
{
  /** Operations to do. Bitmask of VolumeShaperOperationFlag. */
  public int flags = 0;
  /** If >= 0 the id to remove in a replace operation. */
  public int replaceId = 0;
  /** Position in the curve to set if a valid number (not nan). */
  public float xOffset = 0.000000f;
  public static final android.os.Parcelable.Creator<VolumeShaperOperation> CREATOR = new android.os.Parcelable.Creator<VolumeShaperOperation>() {
    @Override
    public VolumeShaperOperation createFromParcel(android.os.Parcel _aidl_source) {
      VolumeShaperOperation _aidl_out = new VolumeShaperOperation();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public VolumeShaperOperation[] newArray(int _aidl_size) {
      return new VolumeShaperOperation[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(flags);
    _aidl_parcel.writeInt(replaceId);
    _aidl_parcel.writeFloat(xOffset);
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
      flags = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      replaceId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      xOffset = _aidl_parcel.readFloat();
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
