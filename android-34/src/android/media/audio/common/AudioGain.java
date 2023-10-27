/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
/** @hide */
public class AudioGain implements android.os.Parcelable
{
  public int mode = 0;
  public android.media.audio.common.AudioChannelLayout channelMask;
  public int minValue = 0;
  public int maxValue = 0;
  public int defaultValue = 0;
  public int stepValue = 0;
  public int minRampMs = 0;
  public int maxRampMs = 0;
  public boolean useForVolume = false;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioGain> CREATOR = new android.os.Parcelable.Creator<AudioGain>() {
    @Override
    public AudioGain createFromParcel(android.os.Parcel _aidl_source) {
      AudioGain _aidl_out = new AudioGain();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioGain[] newArray(int _aidl_size) {
      return new AudioGain[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(mode);
    _aidl_parcel.writeTypedObject(channelMask, _aidl_flag);
    _aidl_parcel.writeInt(minValue);
    _aidl_parcel.writeInt(maxValue);
    _aidl_parcel.writeInt(defaultValue);
    _aidl_parcel.writeInt(stepValue);
    _aidl_parcel.writeInt(minRampMs);
    _aidl_parcel.writeInt(maxRampMs);
    _aidl_parcel.writeBoolean(useForVolume);
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
      mode = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      channelMask = _aidl_parcel.readTypedObject(android.media.audio.common.AudioChannelLayout.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      minValue = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxValue = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      defaultValue = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      stepValue = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      minRampMs = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxRampMs = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      useForVolume = _aidl_parcel.readBoolean();
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
    _aidl_sj.add("mode: " + (mode));
    _aidl_sj.add("channelMask: " + (java.util.Objects.toString(channelMask)));
    _aidl_sj.add("minValue: " + (minValue));
    _aidl_sj.add("maxValue: " + (maxValue));
    _aidl_sj.add("defaultValue: " + (defaultValue));
    _aidl_sj.add("stepValue: " + (stepValue));
    _aidl_sj.add("minRampMs: " + (minRampMs));
    _aidl_sj.add("maxRampMs: " + (maxRampMs));
    _aidl_sj.add("useForVolume: " + (useForVolume));
    return "android.media.audio.common.AudioGain" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioGain)) return false;
    AudioGain that = (AudioGain)other;
    if (!java.util.Objects.deepEquals(mode, that.mode)) return false;
    if (!java.util.Objects.deepEquals(channelMask, that.channelMask)) return false;
    if (!java.util.Objects.deepEquals(minValue, that.minValue)) return false;
    if (!java.util.Objects.deepEquals(maxValue, that.maxValue)) return false;
    if (!java.util.Objects.deepEquals(defaultValue, that.defaultValue)) return false;
    if (!java.util.Objects.deepEquals(stepValue, that.stepValue)) return false;
    if (!java.util.Objects.deepEquals(minRampMs, that.minRampMs)) return false;
    if (!java.util.Objects.deepEquals(maxRampMs, that.maxRampMs)) return false;
    if (!java.util.Objects.deepEquals(useForVolume, that.useForVolume)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(mode, channelMask, minValue, maxValue, defaultValue, stepValue, minRampMs, maxRampMs, useForVolume).toArray());
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
