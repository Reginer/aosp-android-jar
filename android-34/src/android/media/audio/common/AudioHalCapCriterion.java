/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
public class AudioHalCapCriterion implements android.os.Parcelable
{
  public java.lang.String name;
  public java.lang.String criterionTypeName;
  public java.lang.String defaultLiteralValue;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioHalCapCriterion> CREATOR = new android.os.Parcelable.Creator<AudioHalCapCriterion>() {
    @Override
    public AudioHalCapCriterion createFromParcel(android.os.Parcel _aidl_source) {
      AudioHalCapCriterion _aidl_out = new AudioHalCapCriterion();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioHalCapCriterion[] newArray(int _aidl_size) {
      return new AudioHalCapCriterion[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(name);
    _aidl_parcel.writeString(criterionTypeName);
    _aidl_parcel.writeString(defaultLiteralValue);
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
      criterionTypeName = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      defaultLiteralValue = _aidl_parcel.readString();
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
    _aidl_sj.add("criterionTypeName: " + (java.util.Objects.toString(criterionTypeName)));
    _aidl_sj.add("defaultLiteralValue: " + (java.util.Objects.toString(defaultLiteralValue)));
    return "android.media.audio.common.AudioHalCapCriterion" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioHalCapCriterion)) return false;
    AudioHalCapCriterion that = (AudioHalCapCriterion)other;
    if (!java.util.Objects.deepEquals(name, that.name)) return false;
    if (!java.util.Objects.deepEquals(criterionTypeName, that.criterionTypeName)) return false;
    if (!java.util.Objects.deepEquals(defaultLiteralValue, that.defaultLiteralValue)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(name, criterionTypeName, defaultLiteralValue).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
