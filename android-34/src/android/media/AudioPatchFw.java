/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * {@hide}
 * The Fw suffix is used to break a namespace collision with an SDK API.
 * It contains the framework version of AudioPortConfig.
 */
public class AudioPatchFw implements android.os.Parcelable
{
  /**
   * Patch unique ID.
   * Interpreted as audio_patch_handle_t.
   */
  public int id = 0;
  public android.media.AudioPortConfigFw[] sources;
  public android.media.AudioPortConfigFw[] sinks;
  public static final android.os.Parcelable.Creator<AudioPatchFw> CREATOR = new android.os.Parcelable.Creator<AudioPatchFw>() {
    @Override
    public AudioPatchFw createFromParcel(android.os.Parcel _aidl_source) {
      AudioPatchFw _aidl_out = new AudioPatchFw();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioPatchFw[] newArray(int _aidl_size) {
      return new AudioPatchFw[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(id);
    _aidl_parcel.writeTypedArray(sources, _aidl_flag);
    _aidl_parcel.writeTypedArray(sinks, _aidl_flag);
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
      id = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sources = _aidl_parcel.createTypedArray(android.media.AudioPortConfigFw.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sinks = _aidl_parcel.createTypedArray(android.media.AudioPortConfigFw.CREATOR);
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
    _mask |= describeContents(sources);
    _mask |= describeContents(sinks);
    return _mask;
  }
  private int describeContents(Object _v) {
    if (_v == null) return 0;
    if (_v instanceof Object[]) {
      int _mask = 0;
      for (Object o : (Object[]) _v) {
        _mask |= describeContents(o);
      }
      return _mask;
    }
    if (_v instanceof android.os.Parcelable) {
      return ((android.os.Parcelable) _v).describeContents();
    }
    return 0;
  }
}
