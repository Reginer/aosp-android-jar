/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class FrontendModulationStatus implements android.os.Parcelable {
  // tags for union fields
  public final static int dvbc = 0;  // android.hardware.tv.tuner.FrontendDvbcModulation dvbc;
  public final static int dvbs = 1;  // android.hardware.tv.tuner.FrontendDvbsModulation dvbs;
  public final static int isdbs = 2;  // android.hardware.tv.tuner.FrontendIsdbsModulation isdbs;
  public final static int isdbs3 = 3;  // android.hardware.tv.tuner.FrontendIsdbs3Modulation isdbs3;
  public final static int isdbt = 4;  // android.hardware.tv.tuner.FrontendIsdbtModulation isdbt;

  private int _tag;
  private Object _value;

  public FrontendModulationStatus() {
    int _value = android.hardware.tv.tuner.FrontendDvbcModulation.UNDEFINED;
    this._tag = dvbc;
    this._value = _value;
  }

  private FrontendModulationStatus(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private FrontendModulationStatus(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.tv.tuner.FrontendDvbcModulation dvbc;

  public static FrontendModulationStatus dvbc(int _value) {
    return new FrontendModulationStatus(dvbc, _value);
  }

  public int getDvbc() {
    _assertTag(dvbc);
    return (int) _value;
  }

  public void setDvbc(int _value) {
    _set(dvbc, _value);
  }

  // android.hardware.tv.tuner.FrontendDvbsModulation dvbs;

  public static FrontendModulationStatus dvbs(int _value) {
    return new FrontendModulationStatus(dvbs, _value);
  }

  public int getDvbs() {
    _assertTag(dvbs);
    return (int) _value;
  }

  public void setDvbs(int _value) {
    _set(dvbs, _value);
  }

  // android.hardware.tv.tuner.FrontendIsdbsModulation isdbs;

  public static FrontendModulationStatus isdbs(int _value) {
    return new FrontendModulationStatus(isdbs, _value);
  }

  public int getIsdbs() {
    _assertTag(isdbs);
    return (int) _value;
  }

  public void setIsdbs(int _value) {
    _set(isdbs, _value);
  }

  // android.hardware.tv.tuner.FrontendIsdbs3Modulation isdbs3;

  public static FrontendModulationStatus isdbs3(int _value) {
    return new FrontendModulationStatus(isdbs3, _value);
  }

  public int getIsdbs3() {
    _assertTag(isdbs3);
    return (int) _value;
  }

  public void setIsdbs3(int _value) {
    _set(isdbs3, _value);
  }

  // android.hardware.tv.tuner.FrontendIsdbtModulation isdbt;

  public static FrontendModulationStatus isdbt(int _value) {
    return new FrontendModulationStatus(isdbt, _value);
  }

  public int getIsdbt() {
    _assertTag(isdbt);
    return (int) _value;
  }

  public void setIsdbt(int _value) {
    _set(isdbt, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<FrontendModulationStatus> CREATOR = new android.os.Parcelable.Creator<FrontendModulationStatus>() {
    @Override
    public FrontendModulationStatus createFromParcel(android.os.Parcel _aidl_source) {
      return new FrontendModulationStatus(_aidl_source);
    }
    @Override
    public FrontendModulationStatus[] newArray(int _aidl_size) {
      return new FrontendModulationStatus[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case dvbc:
      _aidl_parcel.writeInt(getDvbc());
      break;
    case dvbs:
      _aidl_parcel.writeInt(getDvbs());
      break;
    case isdbs:
      _aidl_parcel.writeInt(getIsdbs());
      break;
    case isdbs3:
      _aidl_parcel.writeInt(getIsdbs3());
      break;
    case isdbt:
      _aidl_parcel.writeInt(getIsdbt());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case dvbc: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case dvbs: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case isdbs: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case isdbs3: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case isdbt: {
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
    case dvbc: return "dvbc";
    case dvbs: return "dvbs";
    case isdbs: return "isdbs";
    case isdbs3: return "isdbs3";
    case isdbt: return "isdbt";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int dvbc = 0;
    public static final int dvbs = 1;
    public static final int isdbs = 2;
    public static final int isdbs3 = 3;
    public static final int isdbt = 4;
  }
}
