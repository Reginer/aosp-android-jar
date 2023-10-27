/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
/** @hide */
public class AudioConfig implements android.os.Parcelable
{
  public android.media.audio.common.AudioConfigBase base;
  public android.media.audio.common.AudioOffloadInfo offloadInfo;
  public long frameCount = 0L;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioConfig> CREATOR = new android.os.Parcelable.Creator<AudioConfig>() {
    @Override
    public AudioConfig createFromParcel(android.os.Parcel _aidl_source) {
      AudioConfig _aidl_out = new AudioConfig();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioConfig[] newArray(int _aidl_size) {
      return new AudioConfig[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(base, _aidl_flag);
    _aidl_parcel.writeTypedObject(offloadInfo, _aidl_flag);
    _aidl_parcel.writeLong(frameCount);
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
      base = _aidl_parcel.readTypedObject(android.media.audio.common.AudioConfigBase.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      offloadInfo = _aidl_parcel.readTypedObject(android.media.audio.common.AudioOffloadInfo.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      frameCount = _aidl_parcel.readLong();
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
    _aidl_sj.add("base: " + (java.util.Objects.toString(base)));
    _aidl_sj.add("offloadInfo: " + (java.util.Objects.toString(offloadInfo)));
    _aidl_sj.add("frameCount: " + (frameCount));
    return "android.media.audio.common.AudioConfig" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioConfig)) return false;
    AudioConfig that = (AudioConfig)other;
    if (!java.util.Objects.deepEquals(base, that.base)) return false;
    if (!java.util.Objects.deepEquals(offloadInfo, that.offloadInfo)) return false;
    if (!java.util.Objects.deepEquals(frameCount, that.frameCount)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(base, offloadInfo, frameCount).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(base);
    _mask |= describeContents(offloadInfo);
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
