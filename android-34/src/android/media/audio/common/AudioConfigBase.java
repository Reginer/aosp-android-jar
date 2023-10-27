/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
/** @hide */
public class AudioConfigBase implements android.os.Parcelable
{
  public int sampleRate = 0;
  public android.media.audio.common.AudioChannelLayout channelMask;
  public android.media.audio.common.AudioFormatDescription format;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioConfigBase> CREATOR = new android.os.Parcelable.Creator<AudioConfigBase>() {
    @Override
    public AudioConfigBase createFromParcel(android.os.Parcel _aidl_source) {
      AudioConfigBase _aidl_out = new AudioConfigBase();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioConfigBase[] newArray(int _aidl_size) {
      return new AudioConfigBase[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(sampleRate);
    _aidl_parcel.writeTypedObject(channelMask, _aidl_flag);
    _aidl_parcel.writeTypedObject(format, _aidl_flag);
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
      sampleRate = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      channelMask = _aidl_parcel.readTypedObject(android.media.audio.common.AudioChannelLayout.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      format = _aidl_parcel.readTypedObject(android.media.audio.common.AudioFormatDescription.CREATOR);
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
    _aidl_sj.add("sampleRate: " + (sampleRate));
    _aidl_sj.add("channelMask: " + (java.util.Objects.toString(channelMask)));
    _aidl_sj.add("format: " + (java.util.Objects.toString(format)));
    return "android.media.audio.common.AudioConfigBase" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioConfigBase)) return false;
    AudioConfigBase that = (AudioConfigBase)other;
    if (!java.util.Objects.deepEquals(sampleRate, that.sampleRate)) return false;
    if (!java.util.Objects.deepEquals(channelMask, that.channelMask)) return false;
    if (!java.util.Objects.deepEquals(format, that.format)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(sampleRate, channelMask, format).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(channelMask);
    _mask |= describeContents(format);
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
