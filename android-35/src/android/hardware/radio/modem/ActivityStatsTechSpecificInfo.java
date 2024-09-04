/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 8586a5528f0085c15cff4b6628f1b8153aca29ad --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.modem-V3-java-source/gen/android/hardware/radio/modem/ActivityStatsTechSpecificInfo.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.modem-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.modem/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.modem/3/android/hardware/radio/modem/ActivityStatsTechSpecificInfo.aidl
 */
package android.hardware.radio.modem;
/** @hide */
public class ActivityStatsTechSpecificInfo implements android.os.Parcelable
{
  public int rat;
  public int frequencyRange = 0;
  public int[] txmModetimeMs;
  public int rxModeTimeMs = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<ActivityStatsTechSpecificInfo> CREATOR = new android.os.Parcelable.Creator<ActivityStatsTechSpecificInfo>() {
    @Override
    public ActivityStatsTechSpecificInfo createFromParcel(android.os.Parcel _aidl_source) {
      ActivityStatsTechSpecificInfo _aidl_out = new ActivityStatsTechSpecificInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ActivityStatsTechSpecificInfo[] newArray(int _aidl_size) {
      return new ActivityStatsTechSpecificInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(rat);
    _aidl_parcel.writeInt(frequencyRange);
    _aidl_parcel.writeIntArray(txmModetimeMs);
    _aidl_parcel.writeInt(rxModeTimeMs);
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
      rat = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      frequencyRange = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      txmModetimeMs = _aidl_parcel.createIntArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      rxModeTimeMs = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int FREQUENCY_RANGE_UNKNOWN = 0;
  public static final int FREQUENCY_RANGE_LOW = 1;
  public static final int FREQUENCY_RANGE_MID = 2;
  public static final int FREQUENCY_RANGE_HIGH = 3;
  public static final int FREQUENCY_RANGE_MMWAVE = 4;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("rat: " + (android.hardware.radio.AccessNetwork.$.toString(rat)));
    _aidl_sj.add("frequencyRange: " + (frequencyRange));
    _aidl_sj.add("txmModetimeMs: " + (java.util.Arrays.toString(txmModetimeMs)));
    _aidl_sj.add("rxModeTimeMs: " + (rxModeTimeMs));
    return "ActivityStatsTechSpecificInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
