/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 4659b1a13cfc886bed9b5d1a4545ed3a25e00843 --stability vintf --min_sdk_version current -pout/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/system/hardware/interfaces/media/android.media.soundtrigger.types-V3-java-source/gen/android/media/soundtrigger/ModelParameterRange.java.d -o out/soong/.intermediates/system/hardware/interfaces/media/android.media.soundtrigger.types-V3-java-source/gen -Nsystem/hardware/interfaces/media/aidl_api/android.media.soundtrigger.types/3 system/hardware/interfaces/media/aidl_api/android.media.soundtrigger.types/3/android/media/soundtrigger/ModelParameterRange.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media.soundtrigger;
/** @hide */
public class ModelParameterRange implements android.os.Parcelable
{
  public int minInclusive = 0;
  public int maxInclusive = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<ModelParameterRange> CREATOR = new android.os.Parcelable.Creator<ModelParameterRange>() {
    @Override
    public ModelParameterRange createFromParcel(android.os.Parcel _aidl_source) {
      ModelParameterRange _aidl_out = new ModelParameterRange();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ModelParameterRange[] newArray(int _aidl_size) {
      return new ModelParameterRange[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(minInclusive);
    _aidl_parcel.writeInt(maxInclusive);
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
      minInclusive = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxInclusive = _aidl_parcel.readInt();
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
    _aidl_sj.add("minInclusive: " + (minInclusive));
    _aidl_sj.add("maxInclusive: " + (maxInclusive));
    return "ModelParameterRange" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof ModelParameterRange)) return false;
    ModelParameterRange that = (ModelParameterRange)other;
    if (!java.util.Objects.deepEquals(minInclusive, that.minInclusive)) return false;
    if (!java.util.Objects.deepEquals(maxInclusive, that.maxInclusive)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(minInclusive, maxInclusive).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
