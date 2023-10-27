/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
/** @hide */
public class AudioGainConfig implements android.os.Parcelable
{
  public int index = 0;
  public int mode = 0;
  public android.media.audio.common.AudioChannelLayout channelMask;
  public int[] values;
  public int rampDurationMs = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioGainConfig> CREATOR = new android.os.Parcelable.Creator<AudioGainConfig>() {
    @Override
    public AudioGainConfig createFromParcel(android.os.Parcel _aidl_source) {
      AudioGainConfig _aidl_out = new AudioGainConfig();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioGainConfig[] newArray(int _aidl_size) {
      return new AudioGainConfig[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(index);
    _aidl_parcel.writeInt(mode);
    _aidl_parcel.writeTypedObject(channelMask, _aidl_flag);
    _aidl_parcel.writeIntArray(values);
    _aidl_parcel.writeInt(rampDurationMs);
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
      index = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mode = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      channelMask = _aidl_parcel.readTypedObject(android.media.audio.common.AudioChannelLayout.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      values = _aidl_parcel.createIntArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      rampDurationMs = _aidl_parcel.readInt();
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
    _aidl_sj.add("index: " + (index));
    _aidl_sj.add("mode: " + (mode));
    _aidl_sj.add("channelMask: " + (java.util.Objects.toString(channelMask)));
    _aidl_sj.add("values: " + (java.util.Arrays.toString(values)));
    _aidl_sj.add("rampDurationMs: " + (rampDurationMs));
    return "android.media.audio.common.AudioGainConfig" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioGainConfig)) return false;
    AudioGainConfig that = (AudioGainConfig)other;
    if (!java.util.Objects.deepEquals(index, that.index)) return false;
    if (!java.util.Objects.deepEquals(mode, that.mode)) return false;
    if (!java.util.Objects.deepEquals(channelMask, that.channelMask)) return false;
    if (!java.util.Objects.deepEquals(values, that.values)) return false;
    if (!java.util.Objects.deepEquals(rampDurationMs, that.rampDurationMs)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(index, mode, channelMask, values, rampDurationMs).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
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
