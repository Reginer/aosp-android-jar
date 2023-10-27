/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
/** @hide */
public class AudioFormatDescription implements android.os.Parcelable
{
  public byte type = android.media.audio.common.AudioFormatType.DEFAULT;
  public byte pcm = android.media.audio.common.PcmType.DEFAULT;
  public java.lang.String encoding;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioFormatDescription> CREATOR = new android.os.Parcelable.Creator<AudioFormatDescription>() {
    @Override
    public AudioFormatDescription createFromParcel(android.os.Parcel _aidl_source) {
      AudioFormatDescription _aidl_out = new AudioFormatDescription();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioFormatDescription[] newArray(int _aidl_size) {
      return new AudioFormatDescription[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeByte(type);
    _aidl_parcel.writeByte(pcm);
    _aidl_parcel.writeString(encoding);
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
      type = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      pcm = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      encoding = _aidl_parcel.readString();
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
    _aidl_sj.add("type: " + (type));
    _aidl_sj.add("pcm: " + (pcm));
    _aidl_sj.add("encoding: " + (java.util.Objects.toString(encoding)));
    return "android.media.audio.common.AudioFormatDescription" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioFormatDescription)) return false;
    AudioFormatDescription that = (AudioFormatDescription)other;
    if (!java.util.Objects.deepEquals(type, that.type)) return false;
    if (!java.util.Objects.deepEquals(pcm, that.pcm)) return false;
    if (!java.util.Objects.deepEquals(encoding, that.encoding)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(type, pcm, encoding).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
