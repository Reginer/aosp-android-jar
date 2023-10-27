/*
 * This file is auto-generated.  DO NOT MODIFY.
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
    return "android.media.soundtrigger.ModelParameterRange" + _aidl_sj.toString()  ;
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
