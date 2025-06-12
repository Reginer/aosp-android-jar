/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 2 --hash e47d23f579ff7a897fb03e7e7f1c3006cfc6036b -t --stability vintf --min_sdk_version platform_apis --ninja -d out/soong/.intermediates/frameworks/hardware/interfaces/location/altitude/aidl/android.frameworks.location.altitude-V2-java-source/gen/android/frameworks/location/altitude/AddMslAltitudeToLocationResponse.java.d -o out/soong/.intermediates/frameworks/hardware/interfaces/location/altitude/aidl/android.frameworks.location.altitude-V2-java-source/gen -Nframeworks/hardware/interfaces/location/altitude/aidl/aidl_api/android.frameworks.location.altitude/2 frameworks/hardware/interfaces/location/altitude/aidl/aidl_api/android.frameworks.location.altitude/2/android/frameworks/location/altitude/AddMslAltitudeToLocationResponse.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.frameworks.location.altitude;
/** @hide */
public class AddMslAltitudeToLocationResponse implements android.os.Parcelable
{
  public double mslAltitudeMeters = 0.000000;
  public float mslAltitudeAccuracyMeters = 0.000000f;
  public boolean success = false;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AddMslAltitudeToLocationResponse> CREATOR = new android.os.Parcelable.Creator<AddMslAltitudeToLocationResponse>() {
    @Override
    public AddMslAltitudeToLocationResponse createFromParcel(android.os.Parcel _aidl_source) {
      AddMslAltitudeToLocationResponse _aidl_out = new AddMslAltitudeToLocationResponse();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AddMslAltitudeToLocationResponse[] newArray(int _aidl_size) {
      return new AddMslAltitudeToLocationResponse[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeDouble(mslAltitudeMeters);
    _aidl_parcel.writeFloat(mslAltitudeAccuracyMeters);
    _aidl_parcel.writeBoolean(success);
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
      mslAltitudeMeters = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mslAltitudeAccuracyMeters = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      success = _aidl_parcel.readBoolean();
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
