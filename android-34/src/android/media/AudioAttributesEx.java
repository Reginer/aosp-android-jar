/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * This is the equivalent of the android::AudioAttributes C++ type.
 * {@hide}
 */
public class AudioAttributesEx implements android.os.Parcelable
{
  public android.media.AudioAttributesInternal attributes;
  public int streamType;
  /** Interpreted as volume_group_t. */
  public int groupId = 0;
  public static final android.os.Parcelable.Creator<AudioAttributesEx> CREATOR = new android.os.Parcelable.Creator<AudioAttributesEx>() {
    @Override
    public AudioAttributesEx createFromParcel(android.os.Parcel _aidl_source) {
      AudioAttributesEx _aidl_out = new AudioAttributesEx();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioAttributesEx[] newArray(int _aidl_size) {
      return new AudioAttributesEx[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(attributes, _aidl_flag);
    _aidl_parcel.writeInt(streamType);
    _aidl_parcel.writeInt(groupId);
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
      attributes = _aidl_parcel.readTypedObject(android.media.AudioAttributesInternal.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      streamType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      groupId = _aidl_parcel.readInt();
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
    _mask |= describeContents(attributes);
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
