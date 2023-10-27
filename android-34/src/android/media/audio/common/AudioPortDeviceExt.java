/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
/** @hide */
public class AudioPortDeviceExt implements android.os.Parcelable
{
  public android.media.audio.common.AudioDevice device;
  public int flags = 0;
  public android.media.audio.common.AudioFormatDescription[] encodedFormats;
  public int encapsulationModes = 0;
  public int encapsulationMetadataTypes = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioPortDeviceExt> CREATOR = new android.os.Parcelable.Creator<AudioPortDeviceExt>() {
    @Override
    public AudioPortDeviceExt createFromParcel(android.os.Parcel _aidl_source) {
      AudioPortDeviceExt _aidl_out = new AudioPortDeviceExt();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioPortDeviceExt[] newArray(int _aidl_size) {
      return new AudioPortDeviceExt[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(device, _aidl_flag);
    _aidl_parcel.writeInt(flags);
    _aidl_parcel.writeTypedArray(encodedFormats, _aidl_flag);
    _aidl_parcel.writeInt(encapsulationModes);
    _aidl_parcel.writeInt(encapsulationMetadataTypes);
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
      device = _aidl_parcel.readTypedObject(android.media.audio.common.AudioDevice.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      flags = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      encodedFormats = _aidl_parcel.createTypedArray(android.media.audio.common.AudioFormatDescription.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      encapsulationModes = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      encapsulationMetadataTypes = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int FLAG_INDEX_DEFAULT_DEVICE = 0;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("device: " + (java.util.Objects.toString(device)));
    _aidl_sj.add("flags: " + (flags));
    _aidl_sj.add("encodedFormats: " + (java.util.Arrays.toString(encodedFormats)));
    _aidl_sj.add("encapsulationModes: " + (encapsulationModes));
    _aidl_sj.add("encapsulationMetadataTypes: " + (encapsulationMetadataTypes));
    return "android.media.audio.common.AudioPortDeviceExt" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioPortDeviceExt)) return false;
    AudioPortDeviceExt that = (AudioPortDeviceExt)other;
    if (!java.util.Objects.deepEquals(device, that.device)) return false;
    if (!java.util.Objects.deepEquals(flags, that.flags)) return false;
    if (!java.util.Objects.deepEquals(encodedFormats, that.encodedFormats)) return false;
    if (!java.util.Objects.deepEquals(encapsulationModes, that.encapsulationModes)) return false;
    if (!java.util.Objects.deepEquals(encapsulationMetadataTypes, that.encapsulationMetadataTypes)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(device, flags, encodedFormats, encapsulationModes, encapsulationMetadataTypes).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(device);
    _mask |= describeContents(encodedFormats);
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
