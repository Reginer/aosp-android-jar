/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.gnss.measurement_corrections;
/** @hide */
public class ReflectingPlane implements android.os.Parcelable
{
  public double latitudeDegrees = 0.000000;
  public double longitudeDegrees = 0.000000;
  public double altitudeMeters = 0.000000;
  public double reflectingPlaneAzimuthDegrees = 0.000000;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<ReflectingPlane> CREATOR = new android.os.Parcelable.Creator<ReflectingPlane>() {
    @Override
    public ReflectingPlane createFromParcel(android.os.Parcel _aidl_source) {
      ReflectingPlane _aidl_out = new ReflectingPlane();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ReflectingPlane[] newArray(int _aidl_size) {
      return new ReflectingPlane[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeDouble(latitudeDegrees);
    _aidl_parcel.writeDouble(longitudeDegrees);
    _aidl_parcel.writeDouble(altitudeMeters);
    _aidl_parcel.writeDouble(reflectingPlaneAzimuthDegrees);
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
      latitudeDegrees = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      longitudeDegrees = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      altitudeMeters = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      reflectingPlaneAzimuthDegrees = _aidl_parcel.readDouble();
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
