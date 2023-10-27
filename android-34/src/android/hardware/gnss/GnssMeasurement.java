/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.gnss;
/** @hide */
public class GnssMeasurement implements android.os.Parcelable
{
  public int flags = 0;
  public int svid = 0;
  public android.hardware.gnss.GnssSignalType signalType;
  public double timeOffsetNs = 0.000000;
  public int state = 0;
  public long receivedSvTimeInNs = 0L;
  public long receivedSvTimeUncertaintyInNs = 0L;
  public double antennaCN0DbHz = 0.000000;
  public double basebandCN0DbHz = 0.000000;
  public double pseudorangeRateMps = 0.000000;
  public double pseudorangeRateUncertaintyMps = 0.000000;
  public int accumulatedDeltaRangeState = 0;
  public double accumulatedDeltaRangeM = 0.000000;
  public double accumulatedDeltaRangeUncertaintyM = 0.000000;
  public long carrierCycles = 0L;
  public double carrierPhase = 0.000000;
  public double carrierPhaseUncertainty = 0.000000;
  public int multipathIndicator = android.hardware.gnss.GnssMultipathIndicator.UNKNOWN;
  public double snrDb = 0.000000;
  public double agcLevelDb = 0.000000;
  public double fullInterSignalBiasNs = 0.000000;
  public double fullInterSignalBiasUncertaintyNs = 0.000000;
  public double satelliteInterSignalBiasNs = 0.000000;
  public double satelliteInterSignalBiasUncertaintyNs = 0.000000;
  public android.hardware.gnss.SatellitePvt satellitePvt;
  public android.hardware.gnss.CorrelationVector[] correlationVectors;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<GnssMeasurement> CREATOR = new android.os.Parcelable.Creator<GnssMeasurement>() {
    @Override
    public GnssMeasurement createFromParcel(android.os.Parcel _aidl_source) {
      GnssMeasurement _aidl_out = new GnssMeasurement();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public GnssMeasurement[] newArray(int _aidl_size) {
      return new GnssMeasurement[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(flags);
    _aidl_parcel.writeInt(svid);
    _aidl_parcel.writeTypedObject(signalType, _aidl_flag);
    _aidl_parcel.writeDouble(timeOffsetNs);
    _aidl_parcel.writeInt(state);
    _aidl_parcel.writeLong(receivedSvTimeInNs);
    _aidl_parcel.writeLong(receivedSvTimeUncertaintyInNs);
    _aidl_parcel.writeDouble(antennaCN0DbHz);
    _aidl_parcel.writeDouble(basebandCN0DbHz);
    _aidl_parcel.writeDouble(pseudorangeRateMps);
    _aidl_parcel.writeDouble(pseudorangeRateUncertaintyMps);
    _aidl_parcel.writeInt(accumulatedDeltaRangeState);
    _aidl_parcel.writeDouble(accumulatedDeltaRangeM);
    _aidl_parcel.writeDouble(accumulatedDeltaRangeUncertaintyM);
    _aidl_parcel.writeLong(carrierCycles);
    _aidl_parcel.writeDouble(carrierPhase);
    _aidl_parcel.writeDouble(carrierPhaseUncertainty);
    _aidl_parcel.writeInt(multipathIndicator);
    _aidl_parcel.writeDouble(snrDb);
    _aidl_parcel.writeDouble(agcLevelDb);
    _aidl_parcel.writeDouble(fullInterSignalBiasNs);
    _aidl_parcel.writeDouble(fullInterSignalBiasUncertaintyNs);
    _aidl_parcel.writeDouble(satelliteInterSignalBiasNs);
    _aidl_parcel.writeDouble(satelliteInterSignalBiasUncertaintyNs);
    _aidl_parcel.writeTypedObject(satellitePvt, _aidl_flag);
    _aidl_parcel.writeTypedArray(correlationVectors, _aidl_flag);
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
      svid = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      signalType = _aidl_parcel.readTypedObject(android.hardware.gnss.GnssSignalType.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      timeOffsetNs = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      state = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      receivedSvTimeInNs = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      receivedSvTimeUncertaintyInNs = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      antennaCN0DbHz = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      basebandCN0DbHz = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      pseudorangeRateMps = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      pseudorangeRateUncertaintyMps = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      accumulatedDeltaRangeState = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      accumulatedDeltaRangeM = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      accumulatedDeltaRangeUncertaintyM = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      carrierCycles = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      carrierPhase = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      carrierPhaseUncertainty = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      multipathIndicator = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      snrDb = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      agcLevelDb = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      fullInterSignalBiasNs = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      fullInterSignalBiasUncertaintyNs = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      satelliteInterSignalBiasNs = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      satelliteInterSignalBiasUncertaintyNs = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      satellitePvt = _aidl_parcel.readTypedObject(android.hardware.gnss.SatellitePvt.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      correlationVectors = _aidl_parcel.createTypedArray(android.hardware.gnss.CorrelationVector.CREATOR);
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int HAS_SNR = 1;
  public static final int HAS_CARRIER_FREQUENCY = 512;
  public static final int HAS_CARRIER_CYCLES = 1024;
  public static final int HAS_CARRIER_PHASE = 2048;
  public static final int HAS_CARRIER_PHASE_UNCERTAINTY = 4096;
  public static final int HAS_AUTOMATIC_GAIN_CONTROL = 8192;
  public static final int HAS_FULL_ISB = 65536;
  public static final int HAS_FULL_ISB_UNCERTAINTY = 131072;
  public static final int HAS_SATELLITE_ISB = 262144;
  public static final int HAS_SATELLITE_ISB_UNCERTAINTY = 524288;
  public static final int HAS_SATELLITE_PVT = 1048576;
  public static final int HAS_CORRELATION_VECTOR = 2097152;
  public static final int STATE_UNKNOWN = 0;
  public static final int STATE_CODE_LOCK = 1;
  public static final int STATE_BIT_SYNC = 2;
  public static final int STATE_SUBFRAME_SYNC = 4;
  public static final int STATE_TOW_DECODED = 8;
  public static final int STATE_MSEC_AMBIGUOUS = 16;
  public static final int STATE_SYMBOL_SYNC = 32;
  public static final int STATE_GLO_STRING_SYNC = 64;
  public static final int STATE_GLO_TOD_DECODED = 128;
  public static final int STATE_BDS_D2_BIT_SYNC = 256;
  public static final int STATE_BDS_D2_SUBFRAME_SYNC = 512;
  public static final int STATE_GAL_E1BC_CODE_LOCK = 1024;
  public static final int STATE_GAL_E1C_2ND_CODE_LOCK = 2048;
  public static final int STATE_GAL_E1B_PAGE_SYNC = 4096;
  public static final int STATE_SBAS_SYNC = 8192;
  public static final int STATE_TOW_KNOWN = 16384;
  public static final int STATE_GLO_TOD_KNOWN = 32768;
  public static final int STATE_2ND_CODE_LOCK = 65536;
  public static final int ADR_STATE_UNKNOWN = 0;
  public static final int ADR_STATE_VALID = 1;
  public static final int ADR_STATE_RESET = 2;
  public static final int ADR_STATE_CYCLE_SLIP = 4;
  public static final int ADR_STATE_HALF_CYCLE_RESOLVED = 8;
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(signalType);
    _mask |= describeContents(satellitePvt);
    _mask |= describeContents(correlationVectors);
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
