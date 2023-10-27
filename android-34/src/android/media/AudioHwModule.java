/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * A representation of a HAL module configuration.
 * {@hide}
 */
public class AudioHwModule implements android.os.Parcelable
{
  public int handle = 0;
  public java.lang.String name;
  public android.media.audio.common.AudioPort[] ports;
  public android.media.AudioRoute[] routes;
  public static final android.os.Parcelable.Creator<AudioHwModule> CREATOR = new android.os.Parcelable.Creator<AudioHwModule>() {
    @Override
    public AudioHwModule createFromParcel(android.os.Parcel _aidl_source) {
      AudioHwModule _aidl_out = new AudioHwModule();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioHwModule[] newArray(int _aidl_size) {
      return new AudioHwModule[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(handle);
    _aidl_parcel.writeString(name);
    _aidl_parcel.writeTypedArray(ports, _aidl_flag);
    _aidl_parcel.writeTypedArray(routes, _aidl_flag);
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
      handle = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      name = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      ports = _aidl_parcel.createTypedArray(android.media.audio.common.AudioPort.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      routes = _aidl_parcel.createTypedArray(android.media.AudioRoute.CREATOR);
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
    _mask |= describeContents(ports);
    _mask |= describeContents(routes);
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
