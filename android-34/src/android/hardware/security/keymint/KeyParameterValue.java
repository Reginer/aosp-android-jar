/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.security.keymint;
/** @hide */
public final class KeyParameterValue implements android.os.Parcelable {
  // tags for union fields
  public final static int invalid = 0;  // int invalid;
  public final static int algorithm = 1;  // android.hardware.security.keymint.Algorithm algorithm;
  public final static int blockMode = 2;  // android.hardware.security.keymint.BlockMode blockMode;
  public final static int paddingMode = 3;  // android.hardware.security.keymint.PaddingMode paddingMode;
  public final static int digest = 4;  // android.hardware.security.keymint.Digest digest;
  public final static int ecCurve = 5;  // android.hardware.security.keymint.EcCurve ecCurve;
  public final static int origin = 6;  // android.hardware.security.keymint.KeyOrigin origin;
  public final static int keyPurpose = 7;  // android.hardware.security.keymint.KeyPurpose keyPurpose;
  public final static int hardwareAuthenticatorType = 8;  // android.hardware.security.keymint.HardwareAuthenticatorType hardwareAuthenticatorType;
  public final static int securityLevel = 9;  // android.hardware.security.keymint.SecurityLevel securityLevel;
  public final static int boolValue = 10;  // boolean boolValue;
  public final static int integer = 11;  // int integer;
  public final static int longInteger = 12;  // long longInteger;
  public final static int dateTime = 13;  // long dateTime;
  public final static int blob = 14;  // byte[] blob;

  private int _tag;
  private Object _value;

  public KeyParameterValue() {
    int _value = 0;
    this._tag = invalid;
    this._value = _value;
  }

  private KeyParameterValue(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private KeyParameterValue(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // int invalid;

  public static KeyParameterValue invalid(int _value) {
    return new KeyParameterValue(invalid, _value);
  }

  public int getInvalid() {
    _assertTag(invalid);
    return (int) _value;
  }

  public void setInvalid(int _value) {
    _set(invalid, _value);
  }

  // android.hardware.security.keymint.Algorithm algorithm;

  public static KeyParameterValue algorithm(int _value) {
    return new KeyParameterValue(algorithm, _value);
  }

  public int getAlgorithm() {
    _assertTag(algorithm);
    return (int) _value;
  }

  public void setAlgorithm(int _value) {
    _set(algorithm, _value);
  }

  // android.hardware.security.keymint.BlockMode blockMode;

  public static KeyParameterValue blockMode(int _value) {
    return new KeyParameterValue(blockMode, _value);
  }

  public int getBlockMode() {
    _assertTag(blockMode);
    return (int) _value;
  }

  public void setBlockMode(int _value) {
    _set(blockMode, _value);
  }

  // android.hardware.security.keymint.PaddingMode paddingMode;

  public static KeyParameterValue paddingMode(int _value) {
    return new KeyParameterValue(paddingMode, _value);
  }

  public int getPaddingMode() {
    _assertTag(paddingMode);
    return (int) _value;
  }

  public void setPaddingMode(int _value) {
    _set(paddingMode, _value);
  }

  // android.hardware.security.keymint.Digest digest;

  public static KeyParameterValue digest(int _value) {
    return new KeyParameterValue(digest, _value);
  }

  public int getDigest() {
    _assertTag(digest);
    return (int) _value;
  }

  public void setDigest(int _value) {
    _set(digest, _value);
  }

  // android.hardware.security.keymint.EcCurve ecCurve;

  public static KeyParameterValue ecCurve(int _value) {
    return new KeyParameterValue(ecCurve, _value);
  }

  public int getEcCurve() {
    _assertTag(ecCurve);
    return (int) _value;
  }

  public void setEcCurve(int _value) {
    _set(ecCurve, _value);
  }

  // android.hardware.security.keymint.KeyOrigin origin;

  public static KeyParameterValue origin(int _value) {
    return new KeyParameterValue(origin, _value);
  }

  public int getOrigin() {
    _assertTag(origin);
    return (int) _value;
  }

  public void setOrigin(int _value) {
    _set(origin, _value);
  }

  // android.hardware.security.keymint.KeyPurpose keyPurpose;

  public static KeyParameterValue keyPurpose(int _value) {
    return new KeyParameterValue(keyPurpose, _value);
  }

  public int getKeyPurpose() {
    _assertTag(keyPurpose);
    return (int) _value;
  }

  public void setKeyPurpose(int _value) {
    _set(keyPurpose, _value);
  }

  // android.hardware.security.keymint.HardwareAuthenticatorType hardwareAuthenticatorType;

  public static KeyParameterValue hardwareAuthenticatorType(int _value) {
    return new KeyParameterValue(hardwareAuthenticatorType, _value);
  }

  public int getHardwareAuthenticatorType() {
    _assertTag(hardwareAuthenticatorType);
    return (int) _value;
  }

  public void setHardwareAuthenticatorType(int _value) {
    _set(hardwareAuthenticatorType, _value);
  }

  // android.hardware.security.keymint.SecurityLevel securityLevel;

  public static KeyParameterValue securityLevel(int _value) {
    return new KeyParameterValue(securityLevel, _value);
  }

  public int getSecurityLevel() {
    _assertTag(securityLevel);
    return (int) _value;
  }

  public void setSecurityLevel(int _value) {
    _set(securityLevel, _value);
  }

  // boolean boolValue;

  public static KeyParameterValue boolValue(boolean _value) {
    return new KeyParameterValue(boolValue, _value);
  }

  public boolean getBoolValue() {
    _assertTag(boolValue);
    return (boolean) _value;
  }

  public void setBoolValue(boolean _value) {
    _set(boolValue, _value);
  }

  // int integer;

  public static KeyParameterValue integer(int _value) {
    return new KeyParameterValue(integer, _value);
  }

  public int getInteger() {
    _assertTag(integer);
    return (int) _value;
  }

  public void setInteger(int _value) {
    _set(integer, _value);
  }

  // long longInteger;

  public static KeyParameterValue longInteger(long _value) {
    return new KeyParameterValue(longInteger, _value);
  }

  public long getLongInteger() {
    _assertTag(longInteger);
    return (long) _value;
  }

  public void setLongInteger(long _value) {
    _set(longInteger, _value);
  }

  // long dateTime;

  public static KeyParameterValue dateTime(long _value) {
    return new KeyParameterValue(dateTime, _value);
  }

  public long getDateTime() {
    _assertTag(dateTime);
    return (long) _value;
  }

  public void setDateTime(long _value) {
    _set(dateTime, _value);
  }

  // byte[] blob;

  public static KeyParameterValue blob(byte[] _value) {
    return new KeyParameterValue(blob, _value);
  }

  public byte[] getBlob() {
    _assertTag(blob);
    return (byte[]) _value;
  }

  public void setBlob(byte[] _value) {
    _set(blob, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<KeyParameterValue> CREATOR = new android.os.Parcelable.Creator<KeyParameterValue>() {
    @Override
    public KeyParameterValue createFromParcel(android.os.Parcel _aidl_source) {
      return new KeyParameterValue(_aidl_source);
    }
    @Override
    public KeyParameterValue[] newArray(int _aidl_size) {
      return new KeyParameterValue[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case invalid:
      _aidl_parcel.writeInt(getInvalid());
      break;
    case algorithm:
      _aidl_parcel.writeInt(getAlgorithm());
      break;
    case blockMode:
      _aidl_parcel.writeInt(getBlockMode());
      break;
    case paddingMode:
      _aidl_parcel.writeInt(getPaddingMode());
      break;
    case digest:
      _aidl_parcel.writeInt(getDigest());
      break;
    case ecCurve:
      _aidl_parcel.writeInt(getEcCurve());
      break;
    case origin:
      _aidl_parcel.writeInt(getOrigin());
      break;
    case keyPurpose:
      _aidl_parcel.writeInt(getKeyPurpose());
      break;
    case hardwareAuthenticatorType:
      _aidl_parcel.writeInt(getHardwareAuthenticatorType());
      break;
    case securityLevel:
      _aidl_parcel.writeInt(getSecurityLevel());
      break;
    case boolValue:
      _aidl_parcel.writeBoolean(getBoolValue());
      break;
    case integer:
      _aidl_parcel.writeInt(getInteger());
      break;
    case longInteger:
      _aidl_parcel.writeLong(getLongInteger());
      break;
    case dateTime:
      _aidl_parcel.writeLong(getDateTime());
      break;
    case blob:
      _aidl_parcel.writeByteArray(getBlob());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case invalid: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case algorithm: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case blockMode: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case paddingMode: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case digest: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case ecCurve: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case origin: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case keyPurpose: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case hardwareAuthenticatorType: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case securityLevel: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case boolValue: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    case integer: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case longInteger: {
      long _aidl_value;
      _aidl_value = _aidl_parcel.readLong();
      _set(_aidl_tag, _aidl_value);
      return; }
    case dateTime: {
      long _aidl_value;
      _aidl_value = _aidl_parcel.readLong();
      _set(_aidl_tag, _aidl_value);
      return; }
    case blob: {
      byte[] _aidl_value;
      _aidl_value = _aidl_parcel.createByteArray();
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
    case invalid: return "invalid";
    case algorithm: return "algorithm";
    case blockMode: return "blockMode";
    case paddingMode: return "paddingMode";
    case digest: return "digest";
    case ecCurve: return "ecCurve";
    case origin: return "origin";
    case keyPurpose: return "keyPurpose";
    case hardwareAuthenticatorType: return "hardwareAuthenticatorType";
    case securityLevel: return "securityLevel";
    case boolValue: return "boolValue";
    case integer: return "integer";
    case longInteger: return "longInteger";
    case dateTime: return "dateTime";
    case blob: return "blob";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int invalid = 0;
    public static final int algorithm = 1;
    public static final int blockMode = 2;
    public static final int paddingMode = 3;
    public static final int digest = 4;
    public static final int ecCurve = 5;
    public static final int origin = 6;
    public static final int keyPurpose = 7;
    public static final int hardwareAuthenticatorType = 8;
    public static final int securityLevel = 9;
    public static final int boolValue = 10;
    public static final int integer = 11;
    public static final int longInteger = 12;
    public static final int dateTime = 13;
    public static final int blob = 14;
  }
}
