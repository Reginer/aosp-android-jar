/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash af71e6ae2c6861fc2b09bb477e7285e6777cd41c --stability vintf --min_sdk_version 29 --ninja -d out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V4-java-source/gen/android/media/audio/common/AudioHalCapRule.java.d -o out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V4-java-source/gen -Nsystem/hardware/interfaces/media/aidl_api/android.media.audio.common.types/4 system/hardware/interfaces/media/aidl_api/android.media.audio.common.types/4/android/media/audio/common/AudioHalCapRule.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media.audio.common;
/** @hide */
public class AudioHalCapRule implements android.os.Parcelable
{
  public byte compoundRule = android.media.audio.common.AudioHalCapRule.CompoundRule.INVALID;
  public android.media.audio.common.AudioHalCapRule.CriterionRule[] criterionRules;
  public android.media.audio.common.AudioHalCapRule[] nestedRules;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioHalCapRule> CREATOR = new android.os.Parcelable.Creator<AudioHalCapRule>() {
    @Override
    public AudioHalCapRule createFromParcel(android.os.Parcel _aidl_source) {
      AudioHalCapRule _aidl_out = new AudioHalCapRule();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioHalCapRule[] newArray(int _aidl_size) {
      return new AudioHalCapRule[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeByte(compoundRule);
    _aidl_parcel.writeTypedArray(criterionRules, _aidl_flag);
    _aidl_parcel.writeTypedArray(nestedRules, _aidl_flag);
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
      compoundRule = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      criterionRules = _aidl_parcel.createTypedArray(android.media.audio.common.AudioHalCapRule.CriterionRule.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      nestedRules = _aidl_parcel.createTypedArray(android.media.audio.common.AudioHalCapRule.CREATOR);
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
    _aidl_sj.add("compoundRule: " + (compoundRule));
    _aidl_sj.add("criterionRules: " + (java.util.Arrays.toString(criterionRules)));
    _aidl_sj.add("nestedRules: " + (java.util.Arrays.toString(nestedRules)));
    return "AudioHalCapRule" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioHalCapRule)) return false;
    AudioHalCapRule that = (AudioHalCapRule)other;
    if (!java.util.Objects.deepEquals(compoundRule, that.compoundRule)) return false;
    if (!java.util.Objects.deepEquals(criterionRules, that.criterionRules)) return false;
    if (!java.util.Objects.deepEquals(nestedRules, that.nestedRules)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(compoundRule, criterionRules, nestedRules).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(criterionRules);
    _mask |= describeContents(nestedRules);
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
  public static @interface CompoundRule {
    public static final byte INVALID = 0;
    public static final byte ANY = 1;
    public static final byte ALL = 2;
  }
  public static @interface MatchingRule {
    public static final byte INVALID = -1;
    public static final byte IS = 0;
    public static final byte IS_NOT = 1;
    public static final byte INCLUDES = 2;
    public static final byte EXCLUDES = 3;
  }
  public static class CriterionRule implements android.os.Parcelable
  {
    public byte matchingRule = android.media.audio.common.AudioHalCapRule.MatchingRule.INVALID;
    public android.media.audio.common.AudioHalCapCriterionV2 criterionAndValue;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<CriterionRule> CREATOR = new android.os.Parcelable.Creator<CriterionRule>() {
      @Override
      public CriterionRule createFromParcel(android.os.Parcel _aidl_source) {
        CriterionRule _aidl_out = new CriterionRule();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public CriterionRule[] newArray(int _aidl_size) {
        return new CriterionRule[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeByte(matchingRule);
      _aidl_parcel.writeTypedObject(criterionAndValue, _aidl_flag);
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
        matchingRule = _aidl_parcel.readByte();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        criterionAndValue = _aidl_parcel.readTypedObject(android.media.audio.common.AudioHalCapCriterionV2.CREATOR);
      } finally {
        if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
          throw new android.os.BadParcelableException("Overflow in the size of parcelable");
        }
        _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
      }
    }
    @Override
    public int describeContents() {
      int _mask = 0;
      _mask |= describeContents(criterionAndValue);
      return _mask;
    }
    private int describeContents(Object _v) {
      if (_v == null) return 0;
      if (_v instanceof android.os.Parcelable) {
        return ((android.os.Parcelable) _v).describeContents();
      }
      return 0;
    }
  }
}
