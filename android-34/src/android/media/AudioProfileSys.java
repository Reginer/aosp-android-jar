/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * Provides indication whether the parameters of the AudioProfiles in the
 * AudioPort are dynamic. Each instance of AudioProfileSys corresponds
 * to an instance of AudioProfile.
 * 
 * {@hide}
 */
public class AudioProfileSys implements android.os.Parcelable
{
  public boolean isDynamicFormat = false;
  public boolean isDynamicChannels = false;
  public boolean isDynamicRate = false;
  public static final android.os.Parcelable.Creator<AudioProfileSys> CREATOR = new android.os.Parcelable.Creator<AudioProfileSys>() {
    @Override
    public AudioProfileSys createFromParcel(android.os.Parcel _aidl_source) {
      AudioProfileSys _aidl_out = new AudioProfileSys();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioProfileSys[] newArray(int _aidl_size) {
      return new AudioProfileSys[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeBoolean(isDynamicFormat);
    _aidl_parcel.writeBoolean(isDynamicChannels);
    _aidl_parcel.writeBoolean(isDynamicRate);
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
      isDynamicFormat = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isDynamicChannels = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isDynamicRate = _aidl_parcel.readBoolean();
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
