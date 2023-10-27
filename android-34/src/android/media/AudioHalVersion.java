/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * The audio HAL version definition.
 * 
 * {@hide}
 */
public class AudioHalVersion implements android.os.Parcelable
{
  public int type = android.media.AudioHalVersion.Type.HIDL;
  /** Major version number. */
  public int major = 0;
  /** Minor version number. */
  public int minor = 0;
  public static final android.os.Parcelable.Creator<AudioHalVersion> CREATOR = new android.os.Parcelable.Creator<AudioHalVersion>() {
    @Override
    public AudioHalVersion createFromParcel(android.os.Parcel _aidl_source) {
      AudioHalVersion _aidl_out = new AudioHalVersion();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioHalVersion[] newArray(int _aidl_size) {
      return new AudioHalVersion[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(type);
    _aidl_parcel.writeInt(major);
    _aidl_parcel.writeInt(minor);
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
      type = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      major = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      minor = _aidl_parcel.readInt();
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
  public static @interface Type {
    /**
     * Indicate the audio HAL is implemented with HIDL (HAL interface definition language).
     * @see <a href="https://source.android.com/docs/core/architecture/hidl/">HIDL</a>
     */
    public static final int HIDL = 0;
    /**
     * Indicate the audio HAL is implemented with AIDL (Android Interface Definition Language).
     * @see <a href="https://source.android.com/docs/core/architecture/aidl/">AIDL</a>
     */
    public static final int AIDL = 1;
  }
}
