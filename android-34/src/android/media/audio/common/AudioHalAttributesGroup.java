/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
public class AudioHalAttributesGroup implements android.os.Parcelable
{
  public int streamType = android.media.audio.common.AudioStreamType.INVALID;
  public java.lang.String volumeGroupName;
  public android.media.audio.common.AudioAttributes[] attributes;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioHalAttributesGroup> CREATOR = new android.os.Parcelable.Creator<AudioHalAttributesGroup>() {
    @Override
    public AudioHalAttributesGroup createFromParcel(android.os.Parcel _aidl_source) {
      AudioHalAttributesGroup _aidl_out = new AudioHalAttributesGroup();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioHalAttributesGroup[] newArray(int _aidl_size) {
      return new AudioHalAttributesGroup[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(streamType);
    _aidl_parcel.writeString(volumeGroupName);
    _aidl_parcel.writeTypedArray(attributes, _aidl_flag);
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
      streamType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      volumeGroupName = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      attributes = _aidl_parcel.createTypedArray(android.media.audio.common.AudioAttributes.CREATOR);
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
    _aidl_sj.add("streamType: " + (streamType));
    _aidl_sj.add("volumeGroupName: " + (java.util.Objects.toString(volumeGroupName)));
    _aidl_sj.add("attributes: " + (java.util.Arrays.toString(attributes)));
    return "android.media.audio.common.AudioHalAttributesGroup" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioHalAttributesGroup)) return false;
    AudioHalAttributesGroup that = (AudioHalAttributesGroup)other;
    if (!java.util.Objects.deepEquals(streamType, that.streamType)) return false;
    if (!java.util.Objects.deepEquals(volumeGroupName, that.volumeGroupName)) return false;
    if (!java.util.Objects.deepEquals(attributes, that.attributes)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(streamType, volumeGroupName, attributes).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(attributes);
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
