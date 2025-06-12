/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash c43fbb9be4a662cc9ace640dba21cccdb84c6c21 -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common_interface/4/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/biometrics/face/aidl/android.hardware.biometrics.face-V4-java-source/gen/android/hardware/biometrics/face/SensorProps.java.d -o out/soong/.intermediates/hardware/interfaces/biometrics/face/aidl/android.hardware.biometrics.face-V4-java-source/gen -Iframeworks/native/aidl/gui -Nhardware/interfaces/biometrics/face/aidl/aidl_api/android.hardware.biometrics.face/4 hardware/interfaces/biometrics/face/aidl/aidl_api/android.hardware.biometrics.face/4/android/hardware/biometrics/face/SensorProps.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.biometrics.face;
/** @hide */
public class SensorProps implements android.os.Parcelable
{
  public android.hardware.biometrics.common.CommonProps commonProps;
  public byte sensorType = android.hardware.biometrics.face.FaceSensorType.UNKNOWN;
  public boolean halControlsPreview = false;
  public int previewDisplayId = 0;
  public int enrollPreviewWidth = 0;
  public int enrollPreviewHeight = 0;
  public float enrollTranslationX = 0.000000f;
  public float enrollTranslationY = 0.000000f;
  public float enrollPreviewScale = 0.000000f;
  public boolean supportsDetectInteraction = false;
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
    _aidl_parcel.writeBoolean(halControlsPreview);
    _aidl_parcel.writeInt(previewDisplayId);
    _aidl_parcel.writeInt(enrollPreviewWidth);
    _aidl_parcel.writeInt(enrollPreviewHeight);
    _aidl_parcel.writeFloat(enrollTranslationX);
    _aidl_parcel.writeFloat(enrollTranslationY);
    _aidl_parcel.writeFloat(enrollPreviewScale);
    _aidl_parcel.writeBoolean(supportsDetectInteraction);
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
      halControlsPreview = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      previewDisplayId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      enrollPreviewWidth = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      enrollPreviewHeight = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      enrollTranslationX = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      enrollTranslationY = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      enrollPreviewScale = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      supportsDetectInteraction = _aidl_parcel.readBoolean();
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
