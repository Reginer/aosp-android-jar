/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash 5867b4f5be491ec815fafea8a3f268b0295427df --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V4-java-source/gen/android/hardware/radio/network/NetworkScanRequest.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/4/android/hardware/radio/network/NetworkScanRequest.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.network;
/** @hide */
public class NetworkScanRequest implements android.os.Parcelable
{
  public int type = 0;
  public int interval = 0;
  public android.hardware.radio.network.RadioAccessSpecifier[] specifiers;
  public int maxSearchTime = 0;
  public boolean incrementalResults = false;
  public int incrementalResultsPeriodicity = 0;
  public java.lang.String[] mccMncs;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<NetworkScanRequest> CREATOR = new android.os.Parcelable.Creator<NetworkScanRequest>() {
    @Override
    public NetworkScanRequest createFromParcel(android.os.Parcel _aidl_source) {
      NetworkScanRequest _aidl_out = new NetworkScanRequest();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public NetworkScanRequest[] newArray(int _aidl_size) {
      return new NetworkScanRequest[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(type);
    _aidl_parcel.writeInt(interval);
    _aidl_parcel.writeTypedArray(specifiers, _aidl_flag);
    _aidl_parcel.writeInt(maxSearchTime);
    _aidl_parcel.writeBoolean(incrementalResults);
    _aidl_parcel.writeInt(incrementalResultsPeriodicity);
    _aidl_parcel.writeStringArray(mccMncs);
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
      interval = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      specifiers = _aidl_parcel.createTypedArray(android.hardware.radio.network.RadioAccessSpecifier.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxSearchTime = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      incrementalResults = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      incrementalResultsPeriodicity = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mccMncs = _aidl_parcel.createStringArray();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int RADIO_ACCESS_SPECIFIER_MAX_SIZE = 8;
  public static final int INCREMENTAL_RESULTS_PREIODICITY_RANGE_MIN = 1;
  public static final int INCREMENTAL_RESULTS_PREIODICITY_RANGE_MAX = 10;
  public static final int MAX_SEARCH_TIME_RANGE_MIN = 60;
  public static final int MAX_SEARCH_TIME_RANGE_MAX = 3600;
  public static final int SCAN_INTERVAL_RANGE_MIN = 5;
  public static final int SCAN_INTERVAL_RANGE_MAX = 300;
  public static final int SCAN_TYPE_ONE_SHOT = 0;
  public static final int SCAN_TYPE_PERIODIC = 1;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("type: " + (type));
    _aidl_sj.add("interval: " + (interval));
    _aidl_sj.add("specifiers: " + (java.util.Arrays.toString(specifiers)));
    _aidl_sj.add("maxSearchTime: " + (maxSearchTime));
    _aidl_sj.add("incrementalResults: " + (incrementalResults));
    _aidl_sj.add("incrementalResultsPeriodicity: " + (incrementalResultsPeriodicity));
    _aidl_sj.add("mccMncs: " + (java.util.Arrays.toString(mccMncs)));
    return "NetworkScanRequest" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(specifiers);
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
