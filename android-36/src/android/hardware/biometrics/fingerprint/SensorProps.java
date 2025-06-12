/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 5 --hash notfrozen -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common_interface/4/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster_interface/4/preprocessed.aidl --previous_api_dir=hardware/interfaces/biometrics/fingerprint/aidl/aidl_api/android.hardware.biometrics.fingerprint/4 --previous_hash 41a730a7a6b5aa9cebebce70ee5b5e509b0af6fb --ninja -d out/soong/.intermediates/hardware/interfaces/biometrics/fingerprint/aidl/android.hardware.biometrics.fingerprint-V5-java-source/gen/android/hardware/biometrics/fingerprint/SensorProps.java.d -o out/soong/.intermediates/hardware/interfaces/biometrics/fingerprint/aidl/android.hardware.biometrics.fingerprint-V5-java-source/gen -Nhardware/interfaces/biometrics/fingerprint/aidl hardware/interfaces/biometrics/fingerprint/aidl/android/hardware/biometrics/fingerprint/SensorProps.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.biometrics.fingerprint;
/** @hide */
public class SensorProps implements android.os.Parcelable
{
  /** Statically configured properties that apply to this fingerprint sensor. */
  public android.hardware.biometrics.common.CommonProps commonProps;
  /** A statically configured sensor type representing this fingerprint sensor. */
  public byte sensorType = android.hardware.biometrics.fingerprint.FingerprintSensorType.UNKNOWN;
  /**
   * A list of display-specific locations from where the sensor is usable from. See SensorLocation
   * for more details.
   */
  public android.hardware.biometrics.fingerprint.SensorLocation[] sensorLocations;
  /**
   * Must be set to true for sensors that support "swipe" gestures via
   * android.view.KeyEvent#KEYCODE_SYSTEM_NAVIGATION_*.
   */
  public boolean supportsNavigationGestures = false;
  /** Specifies whether or not the implementation supports ISession#detectInteraction. */
  public boolean supportsDetectInteraction = false;
  /**
   * Whether the HAL is responsible for detecting and processing of display touches. This is only
   * applicable to under-display fingerprint sensors (UDFPS). If the value is false, the framework
   * will be responsible for handling the display touch events and passing them down to the HAL by
   * using ISession#onPointerDown and ISession#onPointerUp. If the value is true, the framework
   * will not notify the HAL about touch events.
   * 
   * This value must be ignored for non-UDFPS sensors.
   */
  public boolean halHandlesDisplayTouches = false;
  /**
   * Whether the HAL is responsible for fingerprint illumination, for example through enabling the
   * display's high-brightness mode. This is only applicable to optical under-display fingerprint
   * sensors (optical UDFPS). If the value is false, the framework will be responsible for
   * illuminating the finger and reporting ISession#onUiReady. If the value is true, the framework
   * will not illuminate the finger and will not report ISession#onUiReady.
   * 
   * This value must be ignored for sensors that aren't optical UDFPS.
   */
  public boolean halControlsIllumination = false;
  /** Parameters used for fingerprint touch detection. */
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
