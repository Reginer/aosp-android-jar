/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.biometrics.fingerprint;
/** @hide */
public class SensorProps implements android.os.Parcelable
{
  public android.hardware.biometrics.common.CommonProps commonProps;
  public byte sensorType = android.hardware.biometrics.fingerprint.FingerprintSensorType.UNKNOWN;
  public android.hardware.biometrics.fingerprint.SensorLocation[] sensorLocations;
  public boolean supportsNavigationGestures = false;
  public boolean supportsDetectInteraction = false;
  public boolean halHandlesDisplayTouches = false;
  public boolean halControlsIllumination = false;
  public android.hardware.biometrics.fingerprint.TouchDetectionParameters touchDetectionParameters;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<SensorProps> CREATOR = new android.os.Parcelable.Creator<SensorProps>() {
    @Override
    public SensorProps createFromParcel(android.os.Parcel _aidl_source) {
      SensorProps _aidl_out = new SensorProps();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public SensorProps[] newArray(int _aidl_size) {
      return new SensorProps[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(commonProps, _aidl_flag);
    _aidl_parcel.writeByte(sensorType);
    _aidl_parcel.writeTypedArray(sensorLocations, _aidl_flag);
    _aidl_parcel.writeBoolean(supportsNavigationGestures);
    _aidl_parcel.writeBoolean(supportsDetectInteraction);
    _aidl_parcel.writeBoolean(halHandlesDisplayTouches);
    _aidl_parcel.writeBoolean(halControlsIllumination);
    _aidl_parcel.writeTypedObject(touchDetectionParameters, _aidl_flag);
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
      commonProps = _aidl_parcel.readTypedObject(android.hardware.biometrics.common.CommonProps.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sensorType = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sensorLocations = _aidl_parcel.createTypedArray(android.hardware.biometrics.fingerprint.SensorLocation.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      supportsNavigationGestures = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      supportsDetectInteraction = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      halHandlesDisplayTouches = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      halControlsIllumination = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      touchDetectionParameters = _aidl_parcel.readTypedObject(android.hardware.biometrics.fingerprint.TouchDetectionParameters.CREATOR);
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
    _mask |= describeContents(commonProps);
    _mask |= describeContents(sensorLocations);
    _mask |= describeContents(touchDetectionParameters);
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
