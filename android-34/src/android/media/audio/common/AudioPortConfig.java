/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
/** @hide */
public class AudioPortConfig implements android.os.Parcelable
{
  public int id = 0;
  public int portId = 0;
  public android.media.audio.common.Int sampleRate;
  public android.media.audio.common.AudioChannelLayout channelMask;
  public android.media.audio.common.AudioFormatDescription format;
  public android.media.audio.common.AudioGainConfig gain;
  public android.media.audio.common.AudioIoFlags flags;
  public android.media.audio.common.AudioPortExt ext;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioPortConfig> CREATOR = new android.os.Parcelable.Creator<AudioPortConfig>() {
    @Override
    public AudioPortConfig createFromParcel(android.os.Parcel _aidl_source) {
      AudioPortConfig _aidl_out = new AudioPortConfig();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioPortConfig[] newArray(int _aidl_size) {
      return new AudioPortConfig[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(id);
    _aidl_parcel.writeInt(portId);
    _aidl_parcel.writeTypedObject(sampleRate, _aidl_flag);
    _aidl_parcel.writeTypedObject(channelMask, _aidl_flag);
    _aidl_parcel.writeTypedObject(format, _aidl_flag);
    _aidl_parcel.writeTypedObject(gain, _aidl_flag);
    _aidl_parcel.writeTypedObject(flags, _aidl_flag);
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
      id = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      portId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sampleRate = _aidl_parcel.readTypedObject(android.media.audio.common.Int.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      channelMask = _aidl_parcel.readTypedObject(android.media.audio.common.AudioChannelLayout.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      format = _aidl_parcel.readTypedObject(android.media.audio.common.AudioFormatDescription.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      gain = _aidl_parcel.readTypedObject(android.media.audio.common.AudioGainConfig.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      flags = _aidl_parcel.readTypedObject(android.media.audio.common.AudioIoFlags.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      ext = _aidl_parcel.readTypedObject(android.media.audio.common.AudioPortExt.CREATOR);
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("id: " + (id));
    _aidl_sj.add("portId: " + (portId));
    _aidl_sj.add("sampleRate: " + (java.util.Objects.toString(sampleRate)));
    _aidl_sj.add("channelMask: " + (java.util.Objects.toString(channelMask)));
    _aidl_sj.add("format: " + (java.util.Objects.toString(format)));
    _aidl_sj.add("gain: " + (java.util.Objects.toString(gain)));
    _aidl_sj.add("flags: " + (java.util.Objects.toString(flags)));
    _aidl_sj.add("ext: " + (java.util.Objects.toString(ext)));
    return "android.media.audio.common.AudioPortConfig" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioPortConfig)) return false;
    AudioPortConfig that = (AudioPortConfig)other;
    if (!java.util.Objects.deepEquals(id, that.id)) return false;
    if (!java.util.Objects.deepEquals(portId, that.portId)) return false;
    if (!java.util.Objects.deepEquals(sampleRate, that.sampleRate)) return false;
    if (!java.util.Objects.deepEquals(channelMask, that.channelMask)) return false;
    if (!java.util.Objects.deepEquals(format, that.format)) return false;
    if (!java.util.Objects.deepEquals(gain, that.gain)) return false;
    if (!java.util.Objects.deepEquals(flags, that.flags)) return false;
    if (!java.util.Objects.deepEquals(ext, that.ext)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(id, portId, sampleRate, channelMask, format, gain, flags, ext).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(sampleRate);
    _mask |= describeContents(channelMask);
    _mask |= describeContents(format);
    _mask |= describeContents(gain);
    _mask |= describeContents(flags);
    _mask |= describeContents(ext);
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
