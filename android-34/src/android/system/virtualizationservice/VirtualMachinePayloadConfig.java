/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.system.virtualizationservice;
public class VirtualMachinePayloadConfig implements android.os.Parcelable
{
  /**
   * Name of the payload executable file in the lib/<ABI> folder of an APK. The payload is in the
   * form of a .so with a defined entry point; inside the VM this file is loaded and the entry
   * function invoked.
   */
  public java.lang.String payloadBinaryName;
  public static final android.os.Parcelable.Creator<VirtualMachinePayloadConfig> CREATOR = new android.os.Parcelable.Creator<VirtualMachinePayloadConfig>() {
    @Override
    public VirtualMachinePayloadConfig createFromParcel(android.os.Parcel _aidl_source) {
      VirtualMachinePayloadConfig _aidl_out = new VirtualMachinePayloadConfig();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public VirtualMachinePayloadConfig[] newArray(int _aidl_size) {
      return new VirtualMachinePayloadConfig[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(payloadBinaryName);
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
      payloadBinaryName = _aidl_parcel.readString();
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
