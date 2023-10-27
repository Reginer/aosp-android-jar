/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/** {@hide} */
public class InterpolatorConfig implements android.os.Parcelable
{
  public int type = android.media.InterpolatorType.CUBIC;
  /** For cubic interpolation, the boundary conditions in slope. */
  public float firstSlope = 0.000000f;
  public float lastSlope = 0.000000f;
  /** A flattened list of <x, y> pairs, monotonically increasing in x. */
  public float[] xy;
  public static final android.os.Parcelable.Creator<InterpolatorConfig> CREATOR = new android.os.Parcelable.Creator<InterpolatorConfig>() {
    @Override
    public InterpolatorConfig createFromParcel(android.os.Parcel _aidl_source) {
      InterpolatorConfig _aidl_out = new InterpolatorConfig();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public InterpolatorConfig[] newArray(int _aidl_size) {
      return new InterpolatorConfig[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(type);
    _aidl_parcel.writeFloat(firstSlope);
    _aidl_parcel.writeFloat(lastSlope);
    _aidl_parcel.writeFloatArray(xy);
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
      firstSlope = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      lastSlope = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      xy = _aidl_parcel.createFloatArray();
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
    return _mask;
  }
}
