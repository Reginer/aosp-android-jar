/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/** {@hide} */
public class EffectDescriptor implements android.os.Parcelable
{
  /** UUID of to the OpenSL ES interface implemented by this effect. */
  public android.media.audio.common.AudioUuid type;
  /** UUID for this particular implementation. */
  public android.media.audio.common.AudioUuid uuid;
  /** Version of the effect control API implemented. */
  public int apiVersion = 0;
  /** Effect engine capabilities/requirements flags. */
  public int flags = 0;
  /** CPU load indication.. */
  public int cpuLoad = 0;
  /** Data Memory usage.. */
  public int memoryUsage = 0;
  /** Human readable effect name. */
  public java.lang.String name;
  /** Human readable effect implementor name. */
  public java.lang.String implementor;
  public static final android.os.Parcelable.Creator<EffectDescriptor> CREATOR = new android.os.Parcelable.Creator<EffectDescriptor>() {
    @Override
    public EffectDescriptor createFromParcel(android.os.Parcel _aidl_source) {
      EffectDescriptor _aidl_out = new EffectDescriptor();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public EffectDescriptor[] newArray(int _aidl_size) {
      return new EffectDescriptor[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(type, _aidl_flag);
    _aidl_parcel.writeTypedObject(uuid, _aidl_flag);
    _aidl_parcel.writeInt(apiVersion);
    _aidl_parcel.writeInt(flags);
    _aidl_parcel.writeInt(cpuLoad);
    _aidl_parcel.writeInt(memoryUsage);
    _aidl_parcel.writeString(name);
    _aidl_parcel.writeString(implementor);
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
      type = _aidl_parcel.readTypedObject(android.media.audio.common.AudioUuid.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      uuid = _aidl_parcel.readTypedObject(android.media.audio.common.AudioUuid.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      apiVersion = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      flags = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      cpuLoad = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      memoryUsage = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      name = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      implementor = _aidl_parcel.readString();
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
    _mask |= describeContents(type);
    _mask |= describeContents(uuid);
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
