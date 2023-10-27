/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * This class is used to contains information about audio mixer.
 * The "Internal" suffix of this type name is to disambiguate it from the
 * android.media.AudioMixerAttributes SDK type.
 * 
 * {@hide}
 */
public class AudioMixerAttributesInternal implements android.os.Parcelable
{
  public android.media.audio.common.AudioConfigBase config;
  public int mixerBehavior;
  public static final android.os.Parcelable.Creator<AudioMixerAttributesInternal> CREATOR = new android.os.Parcelable.Creator<AudioMixerAttributesInternal>() {
    @Override
    public AudioMixerAttributesInternal createFromParcel(android.os.Parcel _aidl_source) {
      AudioMixerAttributesInternal _aidl_out = new AudioMixerAttributesInternal();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioMixerAttributesInternal[] newArray(int _aidl_size) {
      return new AudioMixerAttributesInternal[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(config, _aidl_flag);
    _aidl_parcel.writeInt(mixerBehavior);
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
      config = _aidl_parcel.readTypedObject(android.media.audio.common.AudioConfigBase.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mixerBehavior = _aidl_parcel.readInt();
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
    _mask |= describeContents(config);
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
