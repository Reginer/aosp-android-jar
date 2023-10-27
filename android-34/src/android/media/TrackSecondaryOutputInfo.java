/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * This is a class that contains port handle for a track and handles for all secondary
 * outputs of the track.
 * @hide
 */
public class TrackSecondaryOutputInfo implements android.os.Parcelable
{
  public int portId = 0;
  // audio_port_handle_t
  public int[] secondaryOutputIds;
  public static final android.os.Parcelable.Creator<TrackSecondaryOutputInfo> CREATOR = new android.os.Parcelable.Creator<TrackSecondaryOutputInfo>() {
    @Override
    public TrackSecondaryOutputInfo createFromParcel(android.os.Parcel _aidl_source) {
      TrackSecondaryOutputInfo _aidl_out = new TrackSecondaryOutputInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public TrackSecondaryOutputInfo[] newArray(int _aidl_size) {
      return new TrackSecondaryOutputInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(portId);
    _aidl_parcel.writeIntArray(secondaryOutputIds);
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
      portId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      secondaryOutputIds = _aidl_parcel.createIntArray();
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
