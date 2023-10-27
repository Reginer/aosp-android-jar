/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * {@hide}
 * Suffixed with Fw to avoid name conflict with SDK class.
 */
public class AudioPortConfigFw implements android.os.Parcelable
{
  public android.media.audio.common.AudioPortConfig hal;
  public android.media.AudioPortConfigSys sys;
  public static final android.os.Parcelable.Creator<AudioPortConfigFw> CREATOR = new android.os.Parcelable.Creator<AudioPortConfigFw>() {
    @Override
    public AudioPortConfigFw createFromParcel(android.os.Parcel _aidl_source) {
      AudioPortConfigFw _aidl_out = new AudioPortConfigFw();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioPortConfigFw[] newArray(int _aidl_size) {
      return new AudioPortConfigFw[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(hal, _aidl_flag);
    _aidl_parcel.writeTypedObject(sys, _aidl_flag);
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
      hal = _aidl_parcel.readTypedObject(android.media.audio.common.AudioPortConfig.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sys = _aidl_parcel.readTypedObject(android.media.AudioPortConfigSys.CREATOR);
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
    _mask |= describeContents(hal);
    _mask |= describeContents(sys);
    return _mask;
  }
  private int describeContents(Object _v) {
    if (_v == null) return 0;
    if (_v instanceof android.os.Parcelable) {
      return ((android.os.Parcelable) _v).describeContents();
    }
    return 0;
  }
}
