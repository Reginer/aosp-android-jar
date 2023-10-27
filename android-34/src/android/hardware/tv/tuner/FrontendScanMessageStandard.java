/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class FrontendScanMessageStandard implements android.os.Parcelable {
  // tags for union fields
  public final static int sStd = 0;  // android.hardware.tv.tuner.FrontendDvbsStandard sStd;
  public final static int tStd = 1;  // android.hardware.tv.tuner.FrontendDvbtStandard tStd;
  public final static int sifStd = 2;  // android.hardware.tv.tuner.FrontendAnalogSifStandard sifStd;

  private int _tag;
  private Object _value;

  public FrontendScanMessageStandard() {
    byte _value = android.hardware.tv.tuner.FrontendDvbsStandard.UNDEFINED;
    this._tag = sStd;
    this._value = _value;
  }

  private FrontendScanMessageStandard(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private FrontendScanMessageStandard(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.tv.tuner.FrontendDvbsStandard sStd;

  public static FrontendScanMessageStandard sStd(byte _value) {
    return new FrontendScanMessageStandard(sStd, _value);
  }

  public byte getSStd() {
    _assertTag(sStd);
    return (byte) _value;
  }

  public void setSStd(byte _value) {
    _set(sStd, _value);
  }

  // android.hardware.tv.tuner.FrontendDvbtStandard tStd;

  public static FrontendScanMessageStandard tStd(byte _value) {
    return new FrontendScanMessageStandard(tStd, _value);
  }

  public byte getTStd() {
    _assertTag(tStd);
    return (byte) _value;
  }

  public void setTStd(byte _value) {
    _set(tStd, _value);
  }

  // android.hardware.tv.tuner.FrontendAnalogSifStandard sifStd;

  public static FrontendScanMessageStandard sifStd(int _value) {
    return new FrontendScanMessageStandard(sifStd, _value);
  }

  public int getSifStd() {
    _assertTag(sifStd);
    return (int) _value;
  }

  public void setSifStd(int _value) {
    _set(sifStd, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<FrontendScanMessageStandard> CREATOR = new android.os.Parcelable.Creator<FrontendScanMessageStandard>() {
    @Override
    public FrontendScanMessageStandard createFromParcel(android.os.Parcel _aidl_source) {
      return new FrontendScanMessageStandard(_aidl_source);
    }
    @Override
    public FrontendScanMessageStandard[] newArray(int _aidl_size) {
      return new FrontendScanMessageStandard[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case sStd:
      _aidl_parcel.writeByte(getSStd());
      break;
    case tStd:
      _aidl_parcel.writeByte(getTStd());
      break;
    case sifStd:
      _aidl_parcel.writeInt(getSifStd());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case sStd: {
      byte _aidl_value;
      _aidl_value = _aidl_parcel.readByte();
      _set(_aidl_tag, _aidl_value);
      return; }
    case tStd: {
      byte _aidl_value;
      _aidl_value = _aidl_parcel.readByte();
      _set(_aidl_tag, _aidl_value);
      return; }
    case sifStd: {
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
    case sStd: return "sStd";
    case tStd: return "tStd";
    case sifStd: return "sifStd";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int sStd = 0;
    public static final int tStd = 1;
    public static final int sifStd = 2;
  }
}
