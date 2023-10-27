/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * The "Internal" timestamp is intended to disambiguate from the android.media.AudioTimestamp type.
 * 
 * {@hide}
 */
public class AudioTimestampInternal implements android.os.Parcelable
{
  /** A frame position in AudioTrack::getPosition() units. */
  public int position = 0;
  /** corresponding CLOCK_MONOTONIC when frame is expected to present. */
  public long sec = 0L;
  public int nsec = 0;
  public static final android.os.Parcelable.Creator<AudioTimestampInternal> CREATOR = new android.os.Parcelable.Creator<AudioTimestampInternal>() {
    @Override
    public AudioTimestampInternal createFromParcel(android.os.Parcel _aidl_source) {
      AudioTimestampInternal _aidl_out = new AudioTimestampInternal();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioTimestampInternal[] newArray(int _aidl_size) {
      return new AudioTimestampInternal[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(position);
    _aidl_parcel.writeLong(sec);
    _aidl_parcel.writeInt(nsec);
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
      position = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sec = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      nsec = _aidl_parcel.readInt();
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
