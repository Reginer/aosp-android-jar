/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.gnss.measurement_corrections;
/** @hide */
public class MeasurementCorrections implements android.os.Parcelable
{
  public double latitudeDegrees = 0.000000;
  public double longitudeDegrees = 0.000000;
  public double altitudeMeters = 0.000000;
  public double horizontalPositionUncertaintyMeters = 0.000000;
  public double verticalPositionUncertaintyMeters = 0.000000;
  public long toaGpsNanosecondsOfWeek = 0L;
  public android.hardware.gnss.measurement_corrections.SingleSatCorrection[] satCorrections;
  public boolean hasEnvironmentBearing = false;
  public float environmentBearingDegrees = 0.000000f;
  public float environmentBearingUncertaintyDegrees = 0.000000f;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<MeasurementCorrections> CREATOR = new android.os.Parcelable.Creator<MeasurementCorrections>() {
    @Override
    public MeasurementCorrections createFromParcel(android.os.Parcel _aidl_source) {
      MeasurementCorrections _aidl_out = new MeasurementCorrections();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public MeasurementCorrections[] newArray(int _aidl_size) {
      return new MeasurementCorrections[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeDouble(latitudeDegrees);
    _aidl_parcel.writeDouble(longitudeDegrees);
    _aidl_parcel.writeDouble(altitudeMeters);
    _aidl_parcel.writeDouble(horizontalPositionUncertaintyMeters);
    _aidl_parcel.writeDouble(verticalPositionUncertaintyMeters);
    _aidl_parcel.writeLong(toaGpsNanosecondsOfWeek);
    _aidl_parcel.writeTypedArray(satCorrections, _aidl_flag);
    _aidl_parcel.writeBoolean(hasEnvironmentBearing);
    _aidl_parcel.writeFloat(environmentBearingDegrees);
    _aidl_parcel.writeFloat(environmentBearingUncertaintyDegrees);
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
      horizontalPositionUncertaintyMeters = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      verticalPositionUncertaintyMeters = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      toaGpsNanosecondsOfWeek = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      satCorrections = _aidl_parcel.createTypedArray(android.hardware.gnss.measurement_corrections.SingleSatCorrection.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      hasEnvironmentBearing = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      environmentBearingDegrees = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      environmentBearingUncertaintyDegrees = _aidl_parcel.readFloat();
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
    _mask |= describeContents(satCorrections);
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
