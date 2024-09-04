/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash ea7be3035be8d4869237a6478d2e0bb0efcc1e87 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.config_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.sim-V3-java-source/gen/android/hardware/radio/sim/CarrierRestrictions.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.sim-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.sim/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.sim/3/android/hardware/radio/sim/CarrierRestrictions.aidl
 */
package android.hardware.radio.sim;
/** @hide */
public class CarrierRestrictions implements android.os.Parcelable
{
  /** @deprecated use @List<CarrierInfo> allowedCarrierInfoList */
  @Deprecated
  public android.hardware.radio.sim.Carrier[] allowedCarriers;
  /** @deprecated use @List<CarrierInfo> excludedCarrierInfoList */
  @Deprecated
  public android.hardware.radio.sim.Carrier[] excludedCarriers;
  public boolean allowedCarriersPrioritized = false;
  public int status;
  public android.hardware.radio.sim.CarrierInfo[] allowedCarrierInfoList = {};
  public android.hardware.radio.sim.CarrierInfo[] excludedCarrierInfoList = {};
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CarrierRestrictions> CREATOR = new android.os.Parcelable.Creator<CarrierRestrictions>() {
    @Override
    public CarrierRestrictions createFromParcel(android.os.Parcel _aidl_source) {
      CarrierRestrictions _aidl_out = new CarrierRestrictions();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CarrierRestrictions[] newArray(int _aidl_size) {
      return new CarrierRestrictions[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedArray(allowedCarriers, _aidl_flag);
    _aidl_parcel.writeTypedArray(excludedCarriers, _aidl_flag);
    _aidl_parcel.writeBoolean(allowedCarriersPrioritized);
    _aidl_parcel.writeInt(status);
    _aidl_parcel.writeTypedArray(allowedCarrierInfoList, _aidl_flag);
    _aidl_parcel.writeTypedArray(excludedCarrierInfoList, _aidl_flag);
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
      allowedCarriers = _aidl_parcel.createTypedArray(android.hardware.radio.sim.Carrier.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      excludedCarriers = _aidl_parcel.createTypedArray(android.hardware.radio.sim.Carrier.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      allowedCarriersPrioritized = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      status = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      allowedCarrierInfoList = _aidl_parcel.createTypedArray(android.hardware.radio.sim.CarrierInfo.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      excludedCarrierInfoList = _aidl_parcel.createTypedArray(android.hardware.radio.sim.CarrierInfo.CREATOR);
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
    _aidl_sj.add("allowedCarriers: " + (java.util.Arrays.toString(allowedCarriers)));
    _aidl_sj.add("excludedCarriers: " + (java.util.Arrays.toString(excludedCarriers)));
    _aidl_sj.add("allowedCarriersPrioritized: " + (allowedCarriersPrioritized));
    _aidl_sj.add("status: " + (status));
    _aidl_sj.add("allowedCarrierInfoList: " + (java.util.Arrays.toString(allowedCarrierInfoList)));
    _aidl_sj.add("excludedCarrierInfoList: " + (java.util.Arrays.toString(excludedCarrierInfoList)));
    return "CarrierRestrictions" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(allowedCarriers);
    _mask |= describeContents(excludedCarriers);
    _mask |= describeContents(allowedCarrierInfoList);
    _mask |= describeContents(excludedCarrierInfoList);
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
  public static @interface CarrierRestrictionStatus {
    public static final int UNKNOWN = 0;
    public static final int NOT_RESTRICTED = 1;
    public static final int RESTRICTED = 2;
  }
}
