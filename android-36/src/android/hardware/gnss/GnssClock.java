/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 2 --hash fc957f1d3d261d065ff5e5415f2d21caa79c310f -t --stability vintf --min_sdk_version platform_apis --ninja -d out/soong/.intermediates/hardware/interfaces/gnss/aidl/android.hardware.gnss-V2-java-source/gen/android/hardware/gnss/GnssClock.java.d -o out/soong/.intermediates/hardware/interfaces/gnss/aidl/android.hardware.gnss-V2-java-source/gen -Nhardware/interfaces/gnss/aidl/aidl_api/android.hardware.gnss/2 hardware/interfaces/gnss/aidl/aidl_api/android.hardware.gnss/2/android/hardware/gnss/GnssClock.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.gnss;
/** @hide */
public class GnssClock implements android.os.Parcelable
{
  public int gnssClockFlags = 0;
  public int leapSecond = 0;
  public long timeNs = 0L;
  public double timeUncertaintyNs = 0.000000;
  public long fullBiasNs = 0L;
  public double biasNs = 0.000000;
  public double biasUncertaintyNs = 0.000000;
  public double driftNsps = 0.000000;
  public double driftUncertaintyNsps = 0.000000;
  public int hwClockDiscontinuityCount = 0;
  public android.hardware.gnss.GnssSignalType referenceSignalTypeForIsb;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<GnssClock> CREATOR = new android.os.Parcelable.Creator<GnssClock>() {
    @Override
    public GnssClock createFromParcel(android.os.Parcel _aidl_source) {
      GnssClock _aidl_out = new GnssClock();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public GnssClock[] newArray(int _aidl_size) {
      return new GnssClock[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(gnssClockFlags);
    _aidl_parcel.writeInt(leapSecond);
    _aidl_parcel.writeLong(timeNs);
    _aidl_parcel.writeDouble(timeUncertaintyNs);
    _aidl_parcel.writeLong(fullBiasNs);
    _aidl_parcel.writeDouble(biasNs);
    _aidl_parcel.writeDouble(biasUncertaintyNs);
    _aidl_parcel.writeDouble(driftNsps);
    _aidl_parcel.writeDouble(driftUncertaintyNsps);
    _aidl_parcel.writeInt(hwClockDiscontinuityCount);
    _aidl_parcel.writeTypedObject(referenceSignalTypeForIsb, _aidl_flag);
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
      gnssClockFlags = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      leapSecond = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      timeNs = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      timeUncertaintyNs = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      fullBiasNs = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      biasNs = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      biasUncertaintyNs = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      driftNsps = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      driftUncertaintyNsps = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      hwClockDiscontinuityCount = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      referenceSignalTypeForIsb = _aidl_parcel.readTypedObject(android.hardware.gnss.GnssSignalType.CREATOR);
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int HAS_LEAP_SECOND = 1;
  public static final int HAS_TIME_UNCERTAINTY = 2;
  public static final int HAS_FULL_BIAS = 4;
  public static final int HAS_BIAS = 8;
  public static final int HAS_BIAS_UNCERTAINTY = 16;
  public static final int HAS_DRIFT = 32;
  public static final int HAS_DRIFT_UNCERTAINTY = 64;
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(referenceSignalTypeForIsb);
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
