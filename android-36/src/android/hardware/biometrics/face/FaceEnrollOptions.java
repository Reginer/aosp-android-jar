/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash c43fbb9be4a662cc9ace640dba21cccdb84c6c21 -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common_interface/4/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/biometrics/face/aidl/android.hardware.biometrics.face-V4-java-source/gen/android/hardware/biometrics/face/FaceEnrollOptions.java.d -o out/soong/.intermediates/hardware/interfaces/biometrics/face/aidl/android.hardware.biometrics.face-V4-java-source/gen -Iframeworks/native/aidl/gui -Nhardware/interfaces/biometrics/face/aidl/aidl_api/android.hardware.biometrics.face/4 hardware/interfaces/biometrics/face/aidl/aidl_api/android.hardware.biometrics.face/4/android/hardware/biometrics/face/FaceEnrollOptions.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.biometrics.face;
/** @hide */
public class FaceEnrollOptions implements android.os.Parcelable
{
  public android.hardware.keymaster.HardwareAuthToken hardwareAuthToken;
  public byte enrollmentType;
  public byte[] features;
  /** @deprecated use {@link surfacePreview} instead {@link NativeHandle} a handle used to render content from the face HAL. Note that only one of [{@link surfacePreview}, {@link nativeHandlePreview}] should be set at one time. */
  @Deprecated
  public android.hardware.common.NativeHandle nativeHandlePreview;
  public android.view.Surface surfacePreview;
  public android.hardware.biometrics.common.OperationContext context;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<FaceEnrollOptions> CREATOR = new android.os.Parcelable.Creator<FaceEnrollOptions>() {
    @Override
    public FaceEnrollOptions createFromParcel(android.os.Parcel _aidl_source) {
      FaceEnrollOptions _aidl_out = new FaceEnrollOptions();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public FaceEnrollOptions[] newArray(int _aidl_size) {
      return new FaceEnrollOptions[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(hardwareAuthToken, _aidl_flag);
    _aidl_parcel.writeByte(enrollmentType);
    _aidl_parcel.writeByteArray(features);
    _aidl_parcel.writeTypedObject(nativeHandlePreview, _aidl_flag);
    _aidl_parcel.writeTypedObject(surfacePreview, _aidl_flag);
    _aidl_parcel.writeTypedObject(context, _aidl_flag);
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
      hardwareAuthToken = _aidl_parcel.readTypedObject(android.hardware.keymaster.HardwareAuthToken.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      enrollmentType = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      features = _aidl_parcel.createByteArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      nativeHandlePreview = _aidl_parcel.readTypedObject(android.hardware.common.NativeHandle.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      surfacePreview = _aidl_parcel.readTypedObject(android.view.Surface.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      context = _aidl_parcel.readTypedObject(android.hardware.biometrics.common.OperationContext.CREATOR);
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
    _mask |= describeContents(hardwareAuthToken);
    _mask |= describeContents(nativeHandlePreview);
    _mask |= describeContents(surfacePreview);
    _mask |= describeContents(context);
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
