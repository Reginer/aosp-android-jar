/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash af71e6ae2c6861fc2b09bb477e7285e6777cd41c --stability vintf --min_sdk_version 29 --ninja -d out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V4-java-source/gen/android/media/audio/common/AudioPort.java.d -o out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V4-java-source/gen -Nsystem/hardware/interfaces/media/aidl_api/android.media.audio.common.types/4 system/hardware/interfaces/media/aidl_api/android.media.audio.common.types/4/android/media/audio/common/AudioPort.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media.audio.common;
/** @hide */
public class AudioPort implements android.os.Parcelable
{
  public int id = 0;
  public java.lang.String name;
  public android.media.audio.common.AudioProfile[] profiles;
  public android.media.audio.common.AudioIoFlags flags;
  public android.media.audio.common.ExtraAudioDescriptor[] extraAudioDescriptors;
  public android.media.audio.common.AudioGain[] gains;
  public android.media.audio.common.AudioPortExt ext;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioPort> CREATOR = new android.os.Parcelable.Creator<AudioPort>() {
    @Override
    public AudioPort createFromParcel(android.os.Parcel _aidl_source) {
      AudioPort _aidl_out = new AudioPort();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioPort[] newArray(int _aidl_size) {
      return new AudioPort[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(id);
    _aidl_parcel.writeString(name);
    _aidl_parcel.writeTypedArray(profiles, _aidl_flag);
    _aidl_parcel.writeTypedObject(flags, _aidl_flag);
    _aidl_parcel.writeTypedArray(extraAudioDescriptors, _aidl_flag);
    _aidl_parcel.writeTypedArray(gains, _aidl_flag);
    _aidl_parcel.writeTypedObject(ext, _aidl_flag);
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
      id = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      name = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      profiles = _aidl_parcel.createTypedArray(android.media.audio.common.AudioProfile.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      flags = _aidl_parcel.readTypedObject(android.media.audio.common.AudioIoFlags.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      extraAudioDescriptors = _aidl_parcel.createTypedArray(android.media.audio.common.ExtraAudioDescriptor.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      gains = _aidl_parcel.createTypedArray(android.media.audio.common.AudioGain.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      ext = _aidl_parcel.readTypedObject(android.media.audio.common.AudioPortExt.CREATOR);
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
    _aidl_sj.add("id: " + (id));
    _aidl_sj.add("name: " + (java.util.Objects.toString(name)));
    _aidl_sj.add("profiles: " + (java.util.Arrays.toString(profiles)));
    _aidl_sj.add("flags: " + (java.util.Objects.toString(flags)));
    _aidl_sj.add("extraAudioDescriptors: " + (java.util.Arrays.toString(extraAudioDescriptors)));
    _aidl_sj.add("gains: " + (java.util.Arrays.toString(gains)));
    _aidl_sj.add("ext: " + (java.util.Objects.toString(ext)));
    return "AudioPort" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioPort)) return false;
    AudioPort that = (AudioPort)other;
    if (!java.util.Objects.deepEquals(id, that.id)) return false;
    if (!java.util.Objects.deepEquals(name, that.name)) return false;
    if (!java.util.Objects.deepEquals(profiles, that.profiles)) return false;
    if (!java.util.Objects.deepEquals(flags, that.flags)) return false;
    if (!java.util.Objects.deepEquals(extraAudioDescriptors, that.extraAudioDescriptors)) return false;
    if (!java.util.Objects.deepEquals(gains, that.gains)) return false;
    if (!java.util.Objects.deepEquals(ext, that.ext)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(id, name, profiles, flags, extraAudioDescriptors, gains, ext).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(profiles);
    _mask |= describeContents(flags);
    _mask |= describeContents(extraAudioDescriptors);
    _mask |= describeContents(gains);
    _mask |= describeContents(ext);
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
