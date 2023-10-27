/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.gnss;
/** @hide */
public class GnssLocation implements android.os.Parcelable
{
  public int gnssLocationFlags = 0;
  public double latitudeDegrees = 0.000000;
  public double longitudeDegrees = 0.000000;
  public double altitudeMeters = 0.000000;
  public double speedMetersPerSec = 0.000000;
  public double bearingDegrees = 0.000000;
  public double horizontalAccuracyMeters = 0.000000;
  public double verticalAccuracyMeters = 0.000000;
  public double speedAccuracyMetersPerSecond = 0.000000;
  public double bearingAccuracyDegrees = 0.000000;
  public long timestampMillis = 0L;
  public android.hardware.gnss.ElapsedRealtime elapsedRealtime;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<GnssLocation> CREATOR = new android.os.Parcelable.Creator<GnssLocation>() {
    @Override
    public GnssLocation createFromParcel(android.os.Parcel _aidl_source) {
      GnssLocation _aidl_out = new GnssLocation();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public GnssLocation[] newArray(int _aidl_size) {
      return new GnssLocation[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(gnssLocationFlags);
    _aidl_parcel.writeDouble(latitudeDegrees);
    _aidl_parcel.writeDouble(longitudeDegrees);
    _aidl_parcel.writeDouble(altitudeMeters);
    _aidl_parcel.writeDouble(speedMetersPerSec);
    _aidl_parcel.writeDouble(bearingDegrees);
    _aidl_parcel.writeDouble(horizontalAccuracyMeters);
    _aidl_parcel.writeDouble(verticalAccuracyMeters);
    _aidl_parcel.writeDouble(speedAccuracyMetersPerSecond);
    _aidl_parcel.writeDouble(bearingAccuracyDegrees);
    _aidl_parcel.writeLong(timestampMillis);
    _aidl_parcel.writeTypedObject(elapsedRealtime, _aidl_flag);
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
      gnssLocationFlags = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      latitudeDegrees = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      longitudeDegrees = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      altitudeMeters = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      speedMetersPerSec = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      bearingDegrees = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      horizontalAccuracyMeters = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      verticalAccuracyMeters = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      speedAccuracyMetersPerSecond = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      bearingAccuracyDegrees = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      timestampMillis = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      elapsedRealtime = _aidl_parcel.readTypedObject(android.hardware.gnss.ElapsedRealtime.CREATOR);
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int HAS_LAT_LONG = 1;
  public static final int HAS_ALTITUDE = 2;
  public static final int HAS_SPEED = 4;
  public static final int HAS_BEARING = 8;
  public static final int HAS_HORIZONTAL_ACCURACY = 16;
  public static final int HAS_VERTICAL_ACCURACY = 32;
  public static final int HAS_SPEED_ACCURACY = 64;
  public static final int HAS_BEARING_ACCURACY = 128;
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(elapsedRealtime);
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
