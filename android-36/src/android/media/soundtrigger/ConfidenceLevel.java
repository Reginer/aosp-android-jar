/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 4659b1a13cfc886bed9b5d1a4545ed3a25e00843 --stability vintf --min_sdk_version current -pout/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/system/hardware/interfaces/media/android.media.soundtrigger.types-V3-java-source/gen/android/media/soundtrigger/ConfidenceLevel.java.d -o out/soong/.intermediates/system/hardware/interfaces/media/android.media.soundtrigger.types-V3-java-source/gen -Nsystem/hardware/interfaces/media/aidl_api/android.media.soundtrigger.types/3 system/hardware/interfaces/media/aidl_api/android.media.soundtrigger.types/3/android/media/soundtrigger/ConfidenceLevel.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media.soundtrigger;
/** @hide */
public class ConfidenceLevel implements android.os.Parcelable
{
  public int userId = 0;
  public int levelPercent = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<ConfidenceLevel> CREATOR = new android.os.Parcelable.Creator<ConfidenceLevel>() {
    @Override
    public ConfidenceLevel createFromParcel(android.os.Parcel _aidl_source) {
      ConfidenceLevel _aidl_out = new ConfidenceLevel();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ConfidenceLevel[] newArray(int _aidl_size) {
      return new ConfidenceLevel[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(userId);
    _aidl_parcel.writeInt(levelPercent);
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
      userId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      levelPercent = _aidl_parcel.readInt();
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
    _aidl_sj.add("userId: " + (userId));
    _aidl_sj.add("levelPercent: " + (levelPercent));
    return "ConfidenceLevel" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof ConfidenceLevel)) return false;
    ConfidenceLevel that = (ConfidenceLevel)other;
    if (!java.util.Objects.deepEquals(userId, that.userId)) return false;
    if (!java.util.Objects.deepEquals(levelPercent, that.levelPercent)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(userId, levelPercent).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
