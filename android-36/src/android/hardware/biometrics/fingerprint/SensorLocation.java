/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 5 --hash notfrozen -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common_interface/4/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster_interface/4/preprocessed.aidl --previous_api_dir=hardware/interfaces/biometrics/fingerprint/aidl/aidl_api/android.hardware.biometrics.fingerprint/4 --previous_hash 41a730a7a6b5aa9cebebce70ee5b5e509b0af6fb --ninja -d out/soong/.intermediates/hardware/interfaces/biometrics/fingerprint/aidl/android.hardware.biometrics.fingerprint-V5-java-source/gen/android/hardware/biometrics/fingerprint/SensorLocation.java.d -o out/soong/.intermediates/hardware/interfaces/biometrics/fingerprint/aidl/android.hardware.biometrics.fingerprint-V5-java-source/gen -Nhardware/interfaces/biometrics/fingerprint/aidl hardware/interfaces/biometrics/fingerprint/aidl/android/hardware/biometrics/fingerprint/SensorLocation.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.biometrics.fingerprint;
/** @hide */
public class SensorLocation implements android.os.Parcelable
{
  /** @deprecated use the display field instead. This field was never used. */
  @Deprecated
  public int displayId = 0;
  /**
   * The location of the center of the sensor if applicable. For example, sensors of
   * FingerprintSensorType::UNDER_DISPLAY_* would report this value as the distance in pixels,
   * measured from the left edge of the screen.
   */
  public int sensorLocationX = 0;
  /**
   * The location of the center of the sensor if applicable. For example, sensors of
   * FingerprintSensorType::UNDER_DISPLAY_* would report this value as the distance in pixels,
   * measured from the top edge of the screen.
   */
  public int sensorLocationY = 0;
  /**
   * The radius of the sensor if applicable. For example, sensors of
   * FingerprintSensorType::UNDER_DISPLAY_* would report this value as the radius of the sensor,
   * in pixels.
   */
  public int sensorRadius = 0;
  /**
   * The display to which all of the measurements are relative to. This must correspond to the
   * android.view.Display#getUniqueId Android API. The default display is used if this field is
   * empty.
   * 
   * A few examples:
   *   1) A capacitive rear fingerprint sensor would specify the display to which it is behind.
   *   2) An under-display fingerprint sensor would specify the display on which the sensor is
   *      located.
   *   3) A foldable device would specify multiple locations and have a SensorLocation entry
   *      for each display from which the sensor is accessible from.
   */
  public java.lang.String display = "";
  /**
   * The shape of the sensor if applicable. Most useful for the sensor of type
   * SensorType::UNDER_DISPLAY_*.
   */
  public byte sensorShape = android.hardware.biometrics.fingerprint.SensorShape.CIRCLE;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<SensorLocation> CREATOR = new android.os.Parcelable.Creator<SensorLocation>() {
    @Override
    public SensorLocation createFromParcel(android.os.Parcel _aidl_source) {
      SensorLocation _aidl_out = new SensorLocation();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public SensorLocation[] newArray(int _aidl_size) {
      return new SensorLocation[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(displayId);
    _aidl_parcel.writeInt(sensorLocationX);
    _aidl_parcel.writeInt(sensorLocationY);
    _aidl_parcel.writeInt(sensorRadius);
    _aidl_parcel.writeString(display);
    _aidl_parcel.writeByte(sensorShape);
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
      displayId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sensorLocationX = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sensorLocationY = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sensorRadius = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      display = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sensorShape = _aidl_parcel.readByte();
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
