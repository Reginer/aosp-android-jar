/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
public class AudioPlaybackRate implements android.os.Parcelable
{
  public float speed = 0.000000f;
  public float pitch = 0.000000f;
  public int timestretchMode = android.media.audio.common.AudioPlaybackRate.TimestretchMode.DEFAULT;
  public int fallbackMode = android.media.audio.common.AudioPlaybackRate.TimestretchFallbackMode.SYS_RESERVED_DEFAULT;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioPlaybackRate> CREATOR = new android.os.Parcelable.Creator<AudioPlaybackRate>() {
    @Override
    public AudioPlaybackRate createFromParcel(android.os.Parcel _aidl_source) {
      AudioPlaybackRate _aidl_out = new AudioPlaybackRate();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioPlaybackRate[] newArray(int _aidl_size) {
      return new AudioPlaybackRate[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeFloat(speed);
    _aidl_parcel.writeFloat(pitch);
    _aidl_parcel.writeInt(timestretchMode);
    _aidl_parcel.writeInt(fallbackMode);
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
      speed = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      pitch = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      timestretchMode = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      fallbackMode = _aidl_parcel.readInt();
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
    _aidl_sj.add("speed: " + (speed));
    _aidl_sj.add("pitch: " + (pitch));
    _aidl_sj.add("timestretchMode: " + (timestretchMode));
    _aidl_sj.add("fallbackMode: " + (fallbackMode));
    return "android.media.audio.common.AudioPlaybackRate" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioPlaybackRate)) return false;
    AudioPlaybackRate that = (AudioPlaybackRate)other;
    if (!java.util.Objects.deepEquals(speed, that.speed)) return false;
    if (!java.util.Objects.deepEquals(pitch, that.pitch)) return false;
    if (!java.util.Objects.deepEquals(timestretchMode, that.timestretchMode)) return false;
    if (!java.util.Objects.deepEquals(fallbackMode, that.fallbackMode)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(speed, pitch, timestretchMode, fallbackMode).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
  public static @interface TimestretchMode {
    public static final int DEFAULT = 0;
    public static final int VOICE = 1;
  }
  public static @interface TimestretchFallbackMode {
    public static final int SYS_RESERVED_CUT_REPEAT = -1;
    public static final int SYS_RESERVED_DEFAULT = 0;
    public static final int MUTE = 1;
    public static final int FAIL = 2;
  }
}
