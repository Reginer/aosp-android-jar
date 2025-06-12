/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash d60ca1bb57f94508910cac7b8910c85e2a49a11f -t --stability vintf --min_sdk_version platform_apis --ninja -d out/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster-V4-java-source/gen/android/hardware/keymaster/VerificationToken.java.d -o out/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster-V4-java-source/gen -Nhardware/interfaces/keymaster/aidl/aidl_api/android.hardware.keymaster/4 hardware/interfaces/keymaster/aidl/aidl_api/android.hardware.keymaster/4/android/hardware/keymaster/VerificationToken.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.keymaster;
/** @hide */
public class VerificationToken implements android.os.Parcelable
{
  public long challenge = 0L;
  public android.hardware.keymaster.Timestamp timestamp;
  public int securityLevel = android.hardware.keymaster.SecurityLevel.SOFTWARE;
  public byte[] mac;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<VerificationToken> CREATOR = new android.os.Parcelable.Creator<VerificationToken>() {
    @Override
    public VerificationToken createFromParcel(android.os.Parcel _aidl_source) {
      VerificationToken _aidl_out = new VerificationToken();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public VerificationToken[] newArray(int _aidl_size) {
      return new VerificationToken[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeLong(challenge);
    _aidl_parcel.writeTypedObject(timestamp, _aidl_flag);
    _aidl_parcel.writeInt(securityLevel);
    _aidl_parcel.writeByteArray(mac);
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
      challenge = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      timestamp = _aidl_parcel.readTypedObject(android.hardware.keymaster.Timestamp.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      securityLevel = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mac = _aidl_parcel.createByteArray();
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
    _mask |= describeContents(timestamp);
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
