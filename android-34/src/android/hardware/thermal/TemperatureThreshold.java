/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.thermal;
/** @hide */
public class TemperatureThreshold implements android.os.Parcelable
{
  public int type;
  public java.lang.String name;
  public float[] hotThrottlingThresholds;
  public float[] coldThrottlingThresholds;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<TemperatureThreshold> CREATOR = new android.os.Parcelable.Creator<TemperatureThreshold>() {
    @Override
    public TemperatureThreshold createFromParcel(android.os.Parcel _aidl_source) {
      TemperatureThreshold _aidl_out = new TemperatureThreshold();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public TemperatureThreshold[] newArray(int _aidl_size) {
      return new TemperatureThreshold[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(type);
    _aidl_parcel.writeString(name);
    _aidl_parcel.writeFloatArray(hotThrottlingThresholds);
    _aidl_parcel.writeFloatArray(coldThrottlingThresholds);
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
      type = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      name = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      hotThrottlingThresholds = _aidl_parcel.createFloatArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      coldThrottlingThresholds = _aidl_parcel.createFloatArray();
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
    _aidl_sj.add("type: " + (type));
    _aidl_sj.add("name: " + (java.util.Objects.toString(name)));
    _aidl_sj.add("hotThrottlingThresholds: " + (java.util.Arrays.toString(hotThrottlingThresholds)));
    _aidl_sj.add("coldThrottlingThresholds: " + (java.util.Arrays.toString(coldThrottlingThresholds)));
    return "android.hardware.thermal.TemperatureThreshold" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
