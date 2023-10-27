/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
public class AudioHalVolumeGroup implements android.os.Parcelable
{
  public java.lang.String name;
  public int minIndex = 0;
  public int maxIndex = 0;
  public android.media.audio.common.AudioHalVolumeCurve[] volumeCurves;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioHalVolumeGroup> CREATOR = new android.os.Parcelable.Creator<AudioHalVolumeGroup>() {
    @Override
    public AudioHalVolumeGroup createFromParcel(android.os.Parcel _aidl_source) {
      AudioHalVolumeGroup _aidl_out = new AudioHalVolumeGroup();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioHalVolumeGroup[] newArray(int _aidl_size) {
      return new AudioHalVolumeGroup[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(name);
    _aidl_parcel.writeInt(minIndex);
    _aidl_parcel.writeInt(maxIndex);
    _aidl_parcel.writeTypedArray(volumeCurves, _aidl_flag);
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
      minIndex = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxIndex = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      volumeCurves = _aidl_parcel.createTypedArray(android.media.audio.common.AudioHalVolumeCurve.CREATOR);
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int INDEX_DEFERRED_TO_AUDIO_SERVICE = -1;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("name: " + (java.util.Objects.toString(name)));
    _aidl_sj.add("minIndex: " + (minIndex));
    _aidl_sj.add("maxIndex: " + (maxIndex));
    _aidl_sj.add("volumeCurves: " + (java.util.Arrays.toString(volumeCurves)));
    return "android.media.audio.common.AudioHalVolumeGroup" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioHalVolumeGroup)) return false;
    AudioHalVolumeGroup that = (AudioHalVolumeGroup)other;
    if (!java.util.Objects.deepEquals(name, that.name)) return false;
    if (!java.util.Objects.deepEquals(minIndex, that.minIndex)) return false;
    if (!java.util.Objects.deepEquals(maxIndex, that.maxIndex)) return false;
    if (!java.util.Objects.deepEquals(volumeCurves, that.volumeCurves)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(name, minIndex, maxIndex, volumeCurves).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(volumeCurves);
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
