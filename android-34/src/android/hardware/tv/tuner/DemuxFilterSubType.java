/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class DemuxFilterSubType implements android.os.Parcelable {
  // tags for union fields
  public final static int tsFilterType = 0;  // android.hardware.tv.tuner.DemuxTsFilterType tsFilterType;
  public final static int mmtpFilterType = 1;  // android.hardware.tv.tuner.DemuxMmtpFilterType mmtpFilterType;
  public final static int ipFilterType = 2;  // android.hardware.tv.tuner.DemuxIpFilterType ipFilterType;
  public final static int tlvFilterType = 3;  // android.hardware.tv.tuner.DemuxTlvFilterType tlvFilterType;
  public final static int alpFilterType = 4;  // android.hardware.tv.tuner.DemuxAlpFilterType alpFilterType;

  private int _tag;
  private Object _value;

  public DemuxFilterSubType() {
    int _value = android.hardware.tv.tuner.DemuxTsFilterType.UNDEFINED;
    this._tag = tsFilterType;
    this._value = _value;
  }

  private DemuxFilterSubType(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private DemuxFilterSubType(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.tv.tuner.DemuxTsFilterType tsFilterType;

  public static DemuxFilterSubType tsFilterType(int _value) {
    return new DemuxFilterSubType(tsFilterType, _value);
  }

  public int getTsFilterType() {
    _assertTag(tsFilterType);
    return (int) _value;
  }

  public void setTsFilterType(int _value) {
    _set(tsFilterType, _value);
  }

  // android.hardware.tv.tuner.DemuxMmtpFilterType mmtpFilterType;

  public static DemuxFilterSubType mmtpFilterType(int _value) {
    return new DemuxFilterSubType(mmtpFilterType, _value);
  }

  public int getMmtpFilterType() {
    _assertTag(mmtpFilterType);
    return (int) _value;
  }

  public void setMmtpFilterType(int _value) {
    _set(mmtpFilterType, _value);
  }

  // android.hardware.tv.tuner.DemuxIpFilterType ipFilterType;

  public static DemuxFilterSubType ipFilterType(int _value) {
    return new DemuxFilterSubType(ipFilterType, _value);
  }

  public int getIpFilterType() {
    _assertTag(ipFilterType);
    return (int) _value;
  }

  public void setIpFilterType(int _value) {
    _set(ipFilterType, _value);
  }

  // android.hardware.tv.tuner.DemuxTlvFilterType tlvFilterType;

  public static DemuxFilterSubType tlvFilterType(int _value) {
    return new DemuxFilterSubType(tlvFilterType, _value);
  }

  public int getTlvFilterType() {
    _assertTag(tlvFilterType);
    return (int) _value;
  }

  public void setTlvFilterType(int _value) {
    _set(tlvFilterType, _value);
  }

  // android.hardware.tv.tuner.DemuxAlpFilterType alpFilterType;

  public static DemuxFilterSubType alpFilterType(int _value) {
    return new DemuxFilterSubType(alpFilterType, _value);
  }

  public int getAlpFilterType() {
    _assertTag(alpFilterType);
    return (int) _value;
  }

  public void setAlpFilterType(int _value) {
    _set(alpFilterType, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<DemuxFilterSubType> CREATOR = new android.os.Parcelable.Creator<DemuxFilterSubType>() {
    @Override
    public DemuxFilterSubType createFromParcel(android.os.Parcel _aidl_source) {
      return new DemuxFilterSubType(_aidl_source);
    }
    @Override
    public DemuxFilterSubType[] newArray(int _aidl_size) {
      return new DemuxFilterSubType[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case tsFilterType:
      _aidl_parcel.writeInt(getTsFilterType());
      break;
    case mmtpFilterType:
      _aidl_parcel.writeInt(getMmtpFilterType());
      break;
    case ipFilterType:
      _aidl_parcel.writeInt(getIpFilterType());
      break;
    case tlvFilterType:
      _aidl_parcel.writeInt(getTlvFilterType());
      break;
    case alpFilterType:
      _aidl_parcel.writeInt(getAlpFilterType());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case tsFilterType: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case mmtpFilterType: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case ipFilterType: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case tlvFilterType: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case alpFilterType: {
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
    case tsFilterType: return "tsFilterType";
    case mmtpFilterType: return "mmtpFilterType";
    case ipFilterType: return "ipFilterType";
    case tlvFilterType: return "tlvFilterType";
    case alpFilterType: return "alpFilterType";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int tsFilterType = 0;
    public static final int mmtpFilterType = 1;
    public static final int ipFilterType = 2;
    public static final int tlvFilterType = 3;
    public static final int alpFilterType = 4;
  }
}
