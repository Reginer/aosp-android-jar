/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation -t --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common_interface/4/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster_interface/4/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/biometrics/fingerprint/aidl/android.hardware.biometrics.fingerprint_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/biometrics/fingerprint/aidl/android.hardware.biometrics.fingerprint.virtualhal-java-source/gen/android/hardware/biometrics/fingerprint/virtualhal/AcquiredInfoAndVendorCode.java.d -o out/soong/.intermediates/hardware/interfaces/biometrics/fingerprint/aidl/android.hardware.biometrics.fingerprint.virtualhal-java-source/gen -Nhardware/interfaces/biometrics/fingerprint/aidl hardware/interfaces/biometrics/fingerprint/aidl/android/hardware/biometrics/fingerprint/virtualhal/AcquiredInfoAndVendorCode.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.biometrics.fingerprint.virtualhal;
/** @hide */
public final class AcquiredInfoAndVendorCode implements android.os.Parcelable {
  // tags for union fields
  public final static int acquiredInfo = 0;  // android.hardware.biometrics.fingerprint.AcquiredInfo acquiredInfo;
  public final static int vendorCode = 1;  // int vendorCode;

  private int _tag;
  private Object _value;

  public AcquiredInfoAndVendorCode() {
    byte _value = android.hardware.biometrics.fingerprint.AcquiredInfo.UNKNOWN;
    this._tag = acquiredInfo;
    this._value = _value;
  }

  private AcquiredInfoAndVendorCode(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private AcquiredInfoAndVendorCode(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.biometrics.fingerprint.AcquiredInfo acquiredInfo;

  /** Acquired info as specified in AcqauiredInfo.aidl */
  public static AcquiredInfoAndVendorCode acquiredInfo(byte _value) {
    return new AcquiredInfoAndVendorCode(acquiredInfo, _value);
  }

  public byte getAcquiredInfo() {
    _assertTag(acquiredInfo);
    return (byte) _value;
  }

  public void setAcquiredInfo(byte _value) {
    _set(acquiredInfo, _value);
  }

  // int vendorCode;

  /** Vendor specific code */
  public static AcquiredInfoAndVendorCode vendorCode(int _value) {
    return new AcquiredInfoAndVendorCode(vendorCode, _value);
  }

  public int getVendorCode() {
    _assertTag(vendorCode);
    return (int) _value;
  }

  public void setVendorCode(int _value) {
    _set(vendorCode, _value);
  }

  public static final android.os.Parcelable.Creator<AcquiredInfoAndVendorCode> CREATOR = new android.os.Parcelable.Creator<AcquiredInfoAndVendorCode>() {
    @Override
    public AcquiredInfoAndVendorCode createFromParcel(android.os.Parcel _aidl_source) {
      return new AcquiredInfoAndVendorCode(_aidl_source);
    }
    @Override
    public AcquiredInfoAndVendorCode[] newArray(int _aidl_size) {
      return new AcquiredInfoAndVendorCode[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case acquiredInfo:
      _aidl_parcel.writeByte(getAcquiredInfo());
      break;
    case vendorCode:
      _aidl_parcel.writeInt(getVendorCode());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case acquiredInfo: {
      byte _aidl_value;
      _aidl_value = _aidl_parcel.readByte();
      _set(_aidl_tag, _aidl_value);
      return; }
    case vendorCode: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    }
    return _mask;
  }

  private void _assertTag(int tag) {
    if (getTag() != tag) {
      throw new IllegalStateException("bad access: " + _tagString(tag) + ", " + _tagString(getTag()) + " is available.");
    }
  }

  private String _tagString(int _tag) {
    switch (_tag) {
    case acquiredInfo: return "acquiredInfo";
    case vendorCode: return "vendorCode";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    /** Acquired info as specified in AcqauiredInfo.aidl */
    public static final int acquiredInfo = 0;
    /** Vendor specific code */
    public static final int vendorCode = 1;
  }
}
