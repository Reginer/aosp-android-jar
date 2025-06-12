/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 2 --hash fc957f1d3d261d065ff5e5415f2d21caa79c310f -t --stability vintf --min_sdk_version platform_apis --ninja -d out/soong/.intermediates/hardware/interfaces/gnss/aidl/android.hardware.gnss-V2-java-source/gen/android/hardware/gnss/GnssPowerStats.java.d -o out/soong/.intermediates/hardware/interfaces/gnss/aidl/android.hardware.gnss-V2-java-source/gen -Nhardware/interfaces/gnss/aidl/aidl_api/android.hardware.gnss/2 hardware/interfaces/gnss/aidl/aidl_api/android.hardware.gnss/2/android/hardware/gnss/GnssPowerStats.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.gnss;
/** @hide */
public class GnssPowerStats implements android.os.Parcelable
{
  public android.hardware.gnss.ElapsedRealtime elapsedRealtime;
  public double totalEnergyMilliJoule = 0.000000;
  public double singlebandTrackingModeEnergyMilliJoule = 0.000000;
  public double multibandTrackingModeEnergyMilliJoule = 0.000000;
  public double singlebandAcquisitionModeEnergyMilliJoule = 0.000000;
  public double multibandAcquisitionModeEnergyMilliJoule = 0.000000;
  public double[] otherModesEnergyMilliJoule;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<GnssPowerStats> CREATOR = new android.os.Parcelable.Creator<GnssPowerStats>() {
    @Override
    public GnssPowerStats createFromParcel(android.os.Parcel _aidl_source) {
      GnssPowerStats _aidl_out = new GnssPowerStats();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public GnssPowerStats[] newArray(int _aidl_size) {
      return new GnssPowerStats[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(elapsedRealtime, _aidl_flag);
    _aidl_parcel.writeDouble(totalEnergyMilliJoule);
    _aidl_parcel.writeDouble(singlebandTrackingModeEnergyMilliJoule);
    _aidl_parcel.writeDouble(multibandTrackingModeEnergyMilliJoule);
    _aidl_parcel.writeDouble(singlebandAcquisitionModeEnergyMilliJoule);
    _aidl_parcel.writeDouble(multibandAcquisitionModeEnergyMilliJoule);
    _aidl_parcel.writeDoubleArray(otherModesEnergyMilliJoule);
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
      elapsedRealtime = _aidl_parcel.readTypedObject(android.hardware.gnss.ElapsedRealtime.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      totalEnergyMilliJoule = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      singlebandTrackingModeEnergyMilliJoule = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      multibandTrackingModeEnergyMilliJoule = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      singlebandAcquisitionModeEnergyMilliJoule = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      multibandAcquisitionModeEnergyMilliJoule = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      otherModesEnergyMilliJoule = _aidl_parcel.createDoubleArray();
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
