/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 2 --hash f8d74c149f04e76b6d622db2bd8e465dae24b08c --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/fmq/aidl/android.hardware.common.fmq_interface/1/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V2-java-source/gen/android/hardware/tv/tuner/DemuxFilterAvSettings.java.d -o out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V2-java-source/gen -Nhardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/2 hardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/2/android/hardware/tv/tuner/DemuxFilterAvSettings.aidl
 */
package android.hardware.tv.tuner;
/** @hide */
public class DemuxFilterAvSettings implements android.os.Parcelable
{
  public boolean isPassthrough = false;
  public boolean isSecureMemory = false;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<DemuxFilterAvSettings> CREATOR = new android.os.Parcelable.Creator<DemuxFilterAvSettings>() {
    @Override
    public DemuxFilterAvSettings createFromParcel(android.os.Parcel _aidl_source) {
      DemuxFilterAvSettings _aidl_out = new DemuxFilterAvSettings();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public DemuxFilterAvSettings[] newArray(int _aidl_size) {
      return new DemuxFilterAvSettings[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeBoolean(isPassthrough);
    _aidl_parcel.writeBoolean(isSecureMemory);
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
      isPassthrough = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isSecureMemory = _aidl_parcel.readBoolean();
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