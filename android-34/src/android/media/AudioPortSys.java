/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/** {@hide} */
public class AudioPortSys implements android.os.Parcelable
{
  /** Sink or source. */
  public int role;
  /** Device, mix ... */
  public int type;
  /** System-only parameters for each AudioProfile from 'port.profiles'. */
  public android.media.AudioProfileSys[] profiles;
  /** System-only parameters for each AudioGain from 'port.gains'. */
  public android.media.AudioGainSys[] gains;
  /** Current audio port configuration. */
  public android.media.AudioPortConfigFw activeConfig;
  /** System-only extra parameters for 'port.ext'. */
  public android.media.AudioPortExtSys ext;
  public static final android.os.Parcelable.Creator<AudioPortSys> CREATOR = new android.os.Parcelable.Creator<AudioPortSys>() {
    @Override
    public AudioPortSys createFromParcel(android.os.Parcel _aidl_source) {
      AudioPortSys _aidl_out = new AudioPortSys();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioPortSys[] newArray(int _aidl_size) {
      return new AudioPortSys[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(role);
    _aidl_parcel.writeInt(type);
    _aidl_parcel.writeTypedArray(profiles, _aidl_flag);
    _aidl_parcel.writeTypedArray(gains, _aidl_flag);
    _aidl_parcel.writeTypedObject(activeConfig, _aidl_flag);
    _aidl_parcel.writeTypedObject(ext, _aidl_flag);
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
      role = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      type = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      profiles = _aidl_parcel.createTypedArray(android.media.AudioProfileSys.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      gains = _aidl_parcel.createTypedArray(android.media.AudioGainSys.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      activeConfig = _aidl_parcel.readTypedObject(android.media.AudioPortConfigFw.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      ext = _aidl_parcel.readTypedObject(android.media.AudioPortExtSys.CREATOR);
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
    _mask |= describeContents(profiles);
    _mask |= describeContents(gains);
    _mask |= describeContents(activeConfig);
    _mask |= describeContents(ext);
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
