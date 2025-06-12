/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash d60ca1bb57f94508910cac7b8910c85e2a49a11f -t --stability vintf --min_sdk_version platform_apis --ninja -d out/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster-V4-java-source/gen/android/hardware/keymaster/Timestamp.java.d -o out/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster-V4-java-source/gen -Nhardware/interfaces/keymaster/aidl/aidl_api/android.hardware.keymaster/4 hardware/interfaces/keymaster/aidl/aidl_api/android.hardware.keymaster/4/android/hardware/keymaster/Timestamp.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.keymaster;
/** @hide */
public class Timestamp implements android.os.Parcelable
{
  public long milliSeconds = 0L;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<Timestamp> CREATOR = new android.os.Parcelable.Creator<Timestamp>() {
    @Override
    public Timestamp createFromParcel(android.os.Parcel _aidl_source) {
      Timestamp _aidl_out = new Timestamp();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public Timestamp[] newArray(int _aidl_size) {
      return new Timestamp[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeLong(milliSeconds);
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
      milliSeconds = _aidl_parcel.readLong();
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
