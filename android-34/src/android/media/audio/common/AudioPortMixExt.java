/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
/** @hide */
public class AudioPortMixExt implements android.os.Parcelable
{
  public int handle = 0;
  public android.media.audio.common.AudioPortMixExtUseCase usecase;
  public int maxOpenStreamCount = 0;
  public int maxActiveStreamCount = 0;
  public int recommendedMuteDurationMs = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioPortMixExt> CREATOR = new android.os.Parcelable.Creator<AudioPortMixExt>() {
    @Override
    public AudioPortMixExt createFromParcel(android.os.Parcel _aidl_source) {
      AudioPortMixExt _aidl_out = new AudioPortMixExt();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioPortMixExt[] newArray(int _aidl_size) {
      return new AudioPortMixExt[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(handle);
    _aidl_parcel.writeTypedObject(usecase, _aidl_flag);
    _aidl_parcel.writeInt(maxOpenStreamCount);
    _aidl_parcel.writeInt(maxActiveStreamCount);
    _aidl_parcel.writeInt(recommendedMuteDurationMs);
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
      handle = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      usecase = _aidl_parcel.readTypedObject(android.media.audio.common.AudioPortMixExtUseCase.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxOpenStreamCount = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxActiveStreamCount = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      recommendedMuteDurationMs = _aidl_parcel.readInt();
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
    _aidl_sj.add("handle: " + (handle));
    _aidl_sj.add("usecase: " + (java.util.Objects.toString(usecase)));
    _aidl_sj.add("maxOpenStreamCount: " + (maxOpenStreamCount));
    _aidl_sj.add("maxActiveStreamCount: " + (maxActiveStreamCount));
    _aidl_sj.add("recommendedMuteDurationMs: " + (recommendedMuteDurationMs));
    return "android.media.audio.common.AudioPortMixExt" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioPortMixExt)) return false;
    AudioPortMixExt that = (AudioPortMixExt)other;
    if (!java.util.Objects.deepEquals(handle, that.handle)) return false;
    if (!java.util.Objects.deepEquals(usecase, that.usecase)) return false;
    if (!java.util.Objects.deepEquals(maxOpenStreamCount, that.maxOpenStreamCount)) return false;
    if (!java.util.Objects.deepEquals(maxActiveStreamCount, that.maxActiveStreamCount)) return false;
    if (!java.util.Objects.deepEquals(recommendedMuteDurationMs, that.recommendedMuteDurationMs)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(handle, usecase, maxOpenStreamCount, maxActiveStreamCount, recommendedMuteDurationMs).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(usecase);
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
