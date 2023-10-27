/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.gnss;
/** @hide */
public class GnssData implements android.os.Parcelable
{
  public android.hardware.gnss.GnssMeasurement[] measurements;
  public android.hardware.gnss.GnssClock clock;
  public android.hardware.gnss.ElapsedRealtime elapsedRealtime;
  public android.hardware.gnss.GnssData.GnssAgc[] gnssAgcs = {};
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<GnssData> CREATOR = new android.os.Parcelable.Creator<GnssData>() {
    @Override
    public GnssData createFromParcel(android.os.Parcel _aidl_source) {
      GnssData _aidl_out = new GnssData();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public GnssData[] newArray(int _aidl_size) {
      return new GnssData[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedArray(measurements, _aidl_flag);
    _aidl_parcel.writeTypedObject(clock, _aidl_flag);
    _aidl_parcel.writeTypedObject(elapsedRealtime, _aidl_flag);
    _aidl_parcel.writeTypedArray(gnssAgcs, _aidl_flag);
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
      measurements = _aidl_parcel.createTypedArray(android.hardware.gnss.GnssMeasurement.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      clock = _aidl_parcel.readTypedObject(android.hardware.gnss.GnssClock.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      elapsedRealtime = _aidl_parcel.readTypedObject(android.hardware.gnss.ElapsedRealtime.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      gnssAgcs = _aidl_parcel.createTypedArray(android.hardware.gnss.GnssData.GnssAgc.CREATOR);
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
    _mask |= describeContents(measurements);
    _mask |= describeContents(clock);
    _mask |= describeContents(elapsedRealtime);
    _mask |= describeContents(gnssAgcs);
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
  public static class GnssAgc implements android.os.Parcelable
  {
    public double agcLevelDb = 0.000000;
    public int constellation = android.hardware.gnss.GnssConstellationType.UNKNOWN;
    public long carrierFrequencyHz = 0L;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<GnssAgc> CREATOR = new android.os.Parcelable.Creator<GnssAgc>() {
      @Override
      public GnssAgc createFromParcel(android.os.Parcel _aidl_source) {
        GnssAgc _aidl_out = new GnssAgc();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public GnssAgc[] newArray(int _aidl_size) {
        return new GnssAgc[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeDouble(agcLevelDb);
      _aidl_parcel.writeInt(constellation);
      _aidl_parcel.writeLong(carrierFrequencyHz);
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
        agcLevelDb = _aidl_parcel.readDouble();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        constellation = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        carrierFrequencyHz = _aidl_parcel.readLong();
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
}
