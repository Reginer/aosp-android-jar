/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
/** @hide */
public class AudioProfile implements android.os.Parcelable
{
  public java.lang.String name;
  public android.media.audio.common.AudioFormatDescription format;
  public android.media.audio.common.AudioChannelLayout[] channelMasks;
  public int[] sampleRates;
  public int encapsulationType = android.media.audio.common.AudioEncapsulationType.NONE;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioProfile> CREATOR = new android.os.Parcelable.Creator<AudioProfile>() {
    @Override
    public AudioProfile createFromParcel(android.os.Parcel _aidl_source) {
      AudioProfile _aidl_out = new AudioProfile();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioProfile[] newArray(int _aidl_size) {
      return new AudioProfile[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(name);
    _aidl_parcel.writeTypedObject(format, _aidl_flag);
    _aidl_parcel.writeTypedArray(channelMasks, _aidl_flag);
    _aidl_parcel.writeIntArray(sampleRates);
    _aidl_parcel.writeInt(encapsulationType);
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
      name = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      format = _aidl_parcel.readTypedObject(android.media.audio.common.AudioFormatDescription.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      channelMasks = _aidl_parcel.createTypedArray(android.media.audio.common.AudioChannelLayout.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sampleRates = _aidl_parcel.createIntArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      encapsulationType = _aidl_parcel.readInt();
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
    _aidl_sj.add("name: " + (java.util.Objects.toString(name)));
    _aidl_sj.add("format: " + (java.util.Objects.toString(format)));
    _aidl_sj.add("channelMasks: " + (java.util.Arrays.toString(channelMasks)));
    _aidl_sj.add("sampleRates: " + (java.util.Arrays.toString(sampleRates)));
    _aidl_sj.add("encapsulationType: " + (encapsulationType));
    return "android.media.audio.common.AudioProfile" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioProfile)) return false;
    AudioProfile that = (AudioProfile)other;
    if (!java.util.Objects.deepEquals(name, that.name)) return false;
    if (!java.util.Objects.deepEquals(format, that.format)) return false;
    if (!java.util.Objects.deepEquals(channelMasks, that.channelMasks)) return false;
    if (!java.util.Objects.deepEquals(sampleRates, that.sampleRates)) return false;
    if (!java.util.Objects.deepEquals(encapsulationType, that.encapsulationType)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(name, format, channelMasks, sampleRates, encapsulationType).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(format);
    _mask |= describeContents(channelMasks);
    return _mask;
  }
  private int describeContents(Object _v) {
    if (_v == null) return 0;
    if (_v instanceof Object[]) {
      int _mask = 0;
      for (Object o : (Object[]) _v) {
        _mask |= describeContents(o);
      }
      return _mask;
    }
    if (_v instanceof android.os.Parcelable) {
      return ((android.os.Parcelable) _v).describeContents();
    }
    return 0;
  }
}
