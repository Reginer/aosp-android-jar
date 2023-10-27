/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.gnss;
/** @hide */
public class SatellitePvt implements android.os.Parcelable
{
  public int flags = 0;
  public android.hardware.gnss.SatellitePositionEcef satPosEcef;
  public android.hardware.gnss.SatelliteVelocityEcef satVelEcef;
  public android.hardware.gnss.SatelliteClockInfo satClockInfo;
  public double ionoDelayMeters = 0.000000;
  public double tropoDelayMeters = 0.000000;
  public long timeOfClockSeconds = 0L;
  public int issueOfDataClock = 0;
  public long timeOfEphemerisSeconds = 0L;
  public int issueOfDataEphemeris = 0;
  public int ephemerisSource = android.hardware.gnss.SatellitePvt.SatelliteEphemerisSource.OTHER;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<SatellitePvt> CREATOR = new android.os.Parcelable.Creator<SatellitePvt>() {
    @Override
    public SatellitePvt createFromParcel(android.os.Parcel _aidl_source) {
      SatellitePvt _aidl_out = new SatellitePvt();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public SatellitePvt[] newArray(int _aidl_size) {
      return new SatellitePvt[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(flags);
    _aidl_parcel.writeTypedObject(satPosEcef, _aidl_flag);
    _aidl_parcel.writeTypedObject(satVelEcef, _aidl_flag);
    _aidl_parcel.writeTypedObject(satClockInfo, _aidl_flag);
    _aidl_parcel.writeDouble(ionoDelayMeters);
    _aidl_parcel.writeDouble(tropoDelayMeters);
    _aidl_parcel.writeLong(timeOfClockSeconds);
    _aidl_parcel.writeInt(issueOfDataClock);
    _aidl_parcel.writeLong(timeOfEphemerisSeconds);
    _aidl_parcel.writeInt(issueOfDataEphemeris);
    _aidl_parcel.writeInt(ephemerisSource);
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
      flags = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      satPosEcef = _aidl_parcel.readTypedObject(android.hardware.gnss.SatellitePositionEcef.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      satVelEcef = _aidl_parcel.readTypedObject(android.hardware.gnss.SatelliteVelocityEcef.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      satClockInfo = _aidl_parcel.readTypedObject(android.hardware.gnss.SatelliteClockInfo.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      ionoDelayMeters = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      tropoDelayMeters = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      timeOfClockSeconds = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      issueOfDataClock = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      timeOfEphemerisSeconds = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      issueOfDataEphemeris = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      ephemerisSource = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int HAS_POSITION_VELOCITY_CLOCK_INFO = 1;
  public static final int HAS_IONO = 2;
  public static final int HAS_TROPO = 4;
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(satPosEcef);
    _mask |= describeContents(satVelEcef);
    _mask |= describeContents(satClockInfo);
    return _mask;
  }
  private int describeContents(Object _v) {
    if (_v == null) return 0;
    if (_v instanceof android.os.Parcelable) {
      return ((android.os.Parcelable) _v).describeContents();
    }
    return 0;
  }
  public static @interface SatelliteEphemerisSource {
    public static final int DEMODULATED = 0;
    public static final int SERVER_NORMAL = 1;
    public static final int SERVER_LONG_TERM = 2;
    public static final int OTHER = 3;
  }
}
