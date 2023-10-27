/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/** {@hide} */
public class AudioIoDescriptor implements android.os.Parcelable
{
  /** Interpreted as audio_io_handle_t. */
  public int ioHandle = 0;
  public android.media.AudioPatchFw patch;
  public boolean isInput = false;
  public int samplingRate = 0;
  public android.media.audio.common.AudioFormatDescription format;
  public android.media.audio.common.AudioChannelLayout channelMask;
  public long frameCount = 0L;
  public long frameCountHAL = 0L;
  /** Only valid for output. */
  public int latency = 0;
  /**
   * Interpreted as audio_port_handle_t.
   * valid for event AUDIO_CLIENT_STARTED.
   */
  public int portId = 0;
  public static final android.os.Parcelable.Creator<AudioIoDescriptor> CREATOR = new android.os.Parcelable.Creator<AudioIoDescriptor>() {
    @Override
    public AudioIoDescriptor createFromParcel(android.os.Parcel _aidl_source) {
      AudioIoDescriptor _aidl_out = new AudioIoDescriptor();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioIoDescriptor[] newArray(int _aidl_size) {
      return new AudioIoDescriptor[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(ioHandle);
    _aidl_parcel.writeTypedObject(patch, _aidl_flag);
    _aidl_parcel.writeBoolean(isInput);
    _aidl_parcel.writeInt(samplingRate);
    _aidl_parcel.writeTypedObject(format, _aidl_flag);
    _aidl_parcel.writeTypedObject(channelMask, _aidl_flag);
    _aidl_parcel.writeLong(frameCount);
    _aidl_parcel.writeLong(frameCountHAL);
    _aidl_parcel.writeInt(latency);
    _aidl_parcel.writeInt(portId);
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
      ioHandle = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      patch = _aidl_parcel.readTypedObject(android.media.AudioPatchFw.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isInput = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      samplingRate = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      format = _aidl_parcel.readTypedObject(android.media.audio.common.AudioFormatDescription.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      channelMask = _aidl_parcel.readTypedObject(android.media.audio.common.AudioChannelLayout.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      frameCount = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      frameCountHAL = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      latency = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      portId = _aidl_parcel.readInt();
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
    _mask |= describeContents(patch);
    _mask |= describeContents(format);
    _mask |= describeContents(channelMask);
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
