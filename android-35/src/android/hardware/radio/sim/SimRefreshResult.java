/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash ea7be3035be8d4869237a6478d2e0bb0efcc1e87 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.config_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.sim-V3-java-source/gen/android/hardware/radio/sim/SimRefreshResult.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.sim-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.sim/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.sim/3/android/hardware/radio/sim/SimRefreshResult.aidl
 */
package android.hardware.radio.sim;
/** @hide */
public class SimRefreshResult implements android.os.Parcelable
{
  public int type = 0;
  public int efId = 0;
  public java.lang.String aid;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<SimRefreshResult> CREATOR = new android.os.Parcelable.Creator<SimRefreshResult>() {
    @Override
    public SimRefreshResult createFromParcel(android.os.Parcel _aidl_source) {
      SimRefreshResult _aidl_out = new SimRefreshResult();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public SimRefreshResult[] newArray(int _aidl_size) {
      return new SimRefreshResult[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(type);
    _aidl_parcel.writeInt(efId);
    _aidl_parcel.writeString(aid);
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
      efId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      aid = _aidl_parcel.readString();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int TYPE_SIM_FILE_UPDATE = 0;
  public static final int TYPE_SIM_INIT = 1;
  public static final int TYPE_SIM_RESET = 2;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("type: " + (type));
    _aidl_sj.add("efId: " + (efId));
    _aidl_sj.add("aid: " + (java.util.Objects.toString(aid)));
    return "SimRefreshResult" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
