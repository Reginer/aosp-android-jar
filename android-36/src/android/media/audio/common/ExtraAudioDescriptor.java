/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash af71e6ae2c6861fc2b09bb477e7285e6777cd41c --stability vintf --min_sdk_version 29 --ninja -d out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V4-java-source/gen/android/media/audio/common/ExtraAudioDescriptor.java.d -o out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V4-java-source/gen -Nsystem/hardware/interfaces/media/aidl_api/android.media.audio.common.types/4 system/hardware/interfaces/media/aidl_api/android.media.audio.common.types/4/android/media/audio/common/ExtraAudioDescriptor.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media.audio.common;
/** @hide */
public class ExtraAudioDescriptor implements android.os.Parcelable
{
  public int standard = android.media.audio.common.AudioStandard.NONE;
  public byte[] audioDescriptor;
  public int encapsulationType = android.media.audio.common.AudioEncapsulationType.NONE;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<ExtraAudioDescriptor> CREATOR = new android.os.Parcelable.Creator<ExtraAudioDescriptor>() {
    @Override
    public ExtraAudioDescriptor createFromParcel(android.os.Parcel _aidl_source) {
      ExtraAudioDescriptor _aidl_out = new ExtraAudioDescriptor();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ExtraAudioDescriptor[] newArray(int _aidl_size) {
      return new ExtraAudioDescriptor[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(standard);
    _aidl_parcel.writeByteArray(audioDescriptor);
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
      standard = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      audioDescriptor = _aidl_parcel.createByteArray();
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
    _aidl_sj.add("standard: " + (standard));
    _aidl_sj.add("audioDescriptor: " + (java.util.Arrays.toString(audioDescriptor)));
    _aidl_sj.add("encapsulationType: " + (encapsulationType));
    return "ExtraAudioDescriptor" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof ExtraAudioDescriptor)) return false;
    ExtraAudioDescriptor that = (ExtraAudioDescriptor)other;
    if (!java.util.Objects.deepEquals(standard, that.standard)) return false;
    if (!java.util.Objects.deepEquals(audioDescriptor, that.audioDescriptor)) return false;
    if (!java.util.Objects.deepEquals(encapsulationType, that.encapsulationType)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(standard, audioDescriptor, encapsulationType).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
