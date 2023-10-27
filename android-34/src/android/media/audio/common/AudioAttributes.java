/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
public class AudioAttributes implements android.os.Parcelable
{
  public int contentType = android.media.audio.common.AudioContentType.UNKNOWN;
  public int usage = android.media.audio.common.AudioUsage.UNKNOWN;
  public int source = android.media.audio.common.AudioSource.DEFAULT;
  public int flags = 0;
  public java.lang.String[] tags;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioAttributes> CREATOR = new android.os.Parcelable.Creator<AudioAttributes>() {
    @Override
    public AudioAttributes createFromParcel(android.os.Parcel _aidl_source) {
      AudioAttributes _aidl_out = new AudioAttributes();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioAttributes[] newArray(int _aidl_size) {
      return new AudioAttributes[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(contentType);
    _aidl_parcel.writeInt(usage);
    _aidl_parcel.writeInt(source);
    _aidl_parcel.writeInt(flags);
    _aidl_parcel.writeStringArray(tags);
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
      contentType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      usage = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      source = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      flags = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      tags = _aidl_parcel.createStringArray();
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
    _aidl_sj.add("contentType: " + (contentType));
    _aidl_sj.add("usage: " + (usage));
    _aidl_sj.add("source: " + (source));
    _aidl_sj.add("flags: " + (flags));
    _aidl_sj.add("tags: " + (java.util.Arrays.toString(tags)));
    return "android.media.audio.common.AudioAttributes" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioAttributes)) return false;
    AudioAttributes that = (AudioAttributes)other;
    if (!java.util.Objects.deepEquals(contentType, that.contentType)) return false;
    if (!java.util.Objects.deepEquals(usage, that.usage)) return false;
    if (!java.util.Objects.deepEquals(source, that.source)) return false;
    if (!java.util.Objects.deepEquals(flags, that.flags)) return false;
    if (!java.util.Objects.deepEquals(tags, that.tags)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(contentType, usage, source, flags, tags).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
