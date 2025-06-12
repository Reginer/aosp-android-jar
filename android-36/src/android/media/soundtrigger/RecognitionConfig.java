/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 4659b1a13cfc886bed9b5d1a4545ed3a25e00843 --stability vintf --min_sdk_version current -pout/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/system/hardware/interfaces/media/android.media.soundtrigger.types-V3-java-source/gen/android/media/soundtrigger/RecognitionConfig.java.d -o out/soong/.intermediates/system/hardware/interfaces/media/android.media.soundtrigger.types-V3-java-source/gen -Nsystem/hardware/interfaces/media/aidl_api/android.media.soundtrigger.types/3 system/hardware/interfaces/media/aidl_api/android.media.soundtrigger.types/3/android/media/soundtrigger/RecognitionConfig.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media.soundtrigger;
/** @hide */
public class RecognitionConfig implements android.os.Parcelable
{
  public boolean captureRequested = false;
  public android.media.soundtrigger.PhraseRecognitionExtra[] phraseRecognitionExtras;
  public int audioCapabilities = 0;
  public byte[] data;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<RecognitionConfig> CREATOR = new android.os.Parcelable.Creator<RecognitionConfig>() {
    @Override
    public RecognitionConfig createFromParcel(android.os.Parcel _aidl_source) {
      RecognitionConfig _aidl_out = new RecognitionConfig();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public RecognitionConfig[] newArray(int _aidl_size) {
      return new RecognitionConfig[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeBoolean(captureRequested);
    _aidl_parcel.writeTypedArray(phraseRecognitionExtras, _aidl_flag);
    _aidl_parcel.writeInt(audioCapabilities);
    _aidl_parcel.writeByteArray(data);
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
      captureRequested = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      phraseRecognitionExtras = _aidl_parcel.createTypedArray(android.media.soundtrigger.PhraseRecognitionExtra.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      audioCapabilities = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      data = _aidl_parcel.createByteArray();
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
    _aidl_sj.add("captureRequested: " + (captureRequested));
    _aidl_sj.add("phraseRecognitionExtras: " + (java.util.Arrays.toString(phraseRecognitionExtras)));
    _aidl_sj.add("audioCapabilities: " + (audioCapabilities));
    _aidl_sj.add("data: " + (java.util.Arrays.toString(data)));
    return "RecognitionConfig" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof RecognitionConfig)) return false;
    RecognitionConfig that = (RecognitionConfig)other;
    if (!java.util.Objects.deepEquals(captureRequested, that.captureRequested)) return false;
    if (!java.util.Objects.deepEquals(phraseRecognitionExtras, that.phraseRecognitionExtras)) return false;
    if (!java.util.Objects.deepEquals(audioCapabilities, that.audioCapabilities)) return false;
    if (!java.util.Objects.deepEquals(data, that.data)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(captureRequested, phraseRecognitionExtras, audioCapabilities, data).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(phraseRecognitionExtras);
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
