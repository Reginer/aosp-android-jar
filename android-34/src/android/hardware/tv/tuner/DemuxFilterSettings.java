/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class DemuxFilterSettings implements android.os.Parcelable {
  // tags for union fields
  public final static int ts = 0;  // android.hardware.tv.tuner.DemuxTsFilterSettings ts;
  public final static int mmtp = 1;  // android.hardware.tv.tuner.DemuxMmtpFilterSettings mmtp;
  public final static int ip = 2;  // android.hardware.tv.tuner.DemuxIpFilterSettings ip;
  public final static int tlv = 3;  // android.hardware.tv.tuner.DemuxTlvFilterSettings tlv;
  public final static int alp = 4;  // android.hardware.tv.tuner.DemuxAlpFilterSettings alp;

  private int _tag;
  private Object _value;

  public DemuxFilterSettings() {
    android.hardware.tv.tuner.DemuxTsFilterSettings _value = null;
    this._tag = ts;
    this._value = _value;
  }

  private DemuxFilterSettings(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private DemuxFilterSettings(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.tv.tuner.DemuxTsFilterSettings ts;

  public static DemuxFilterSettings ts(android.hardware.tv.tuner.DemuxTsFilterSettings _value) {
    return new DemuxFilterSettings(ts, _value);
  }

  public android.hardware.tv.tuner.DemuxTsFilterSettings getTs() {
    _assertTag(ts);
    return (android.hardware.tv.tuner.DemuxTsFilterSettings) _value;
  }

  public void setTs(android.hardware.tv.tuner.DemuxTsFilterSettings _value) {
    _set(ts, _value);
  }

  // android.hardware.tv.tuner.DemuxMmtpFilterSettings mmtp;

  public static DemuxFilterSettings mmtp(android.hardware.tv.tuner.DemuxMmtpFilterSettings _value) {
    return new DemuxFilterSettings(mmtp, _value);
  }

  public android.hardware.tv.tuner.DemuxMmtpFilterSettings getMmtp() {
    _assertTag(mmtp);
    return (android.hardware.tv.tuner.DemuxMmtpFilterSettings) _value;
  }

  public void setMmtp(android.hardware.tv.tuner.DemuxMmtpFilterSettings _value) {
    _set(mmtp, _value);
  }

  // android.hardware.tv.tuner.DemuxIpFilterSettings ip;

  public static DemuxFilterSettings ip(android.hardware.tv.tuner.DemuxIpFilterSettings _value) {
    return new DemuxFilterSettings(ip, _value);
  }

  public android.hardware.tv.tuner.DemuxIpFilterSettings getIp() {
    _assertTag(ip);
    return (android.hardware.tv.tuner.DemuxIpFilterSettings) _value;
  }

  public void setIp(android.hardware.tv.tuner.DemuxIpFilterSettings _value) {
    _set(ip, _value);
  }

  // android.hardware.tv.tuner.DemuxTlvFilterSettings tlv;

  public static DemuxFilterSettings tlv(android.hardware.tv.tuner.DemuxTlvFilterSettings _value) {
    return new DemuxFilterSettings(tlv, _value);
  }

  public android.hardware.tv.tuner.DemuxTlvFilterSettings getTlv() {
    _assertTag(tlv);
    return (android.hardware.tv.tuner.DemuxTlvFilterSettings) _value;
  }

  public void setTlv(android.hardware.tv.tuner.DemuxTlvFilterSettings _value) {
    _set(tlv, _value);
  }

  // android.hardware.tv.tuner.DemuxAlpFilterSettings alp;

  public static DemuxFilterSettings alp(android.hardware.tv.tuner.DemuxAlpFilterSettings _value) {
    return new DemuxFilterSettings(alp, _value);
  }

  public android.hardware.tv.tuner.DemuxAlpFilterSettings getAlp() {
    _assertTag(alp);
    return (android.hardware.tv.tuner.DemuxAlpFilterSettings) _value;
  }

  public void setAlp(android.hardware.tv.tuner.DemuxAlpFilterSettings _value) {
    _set(alp, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<DemuxFilterSettings> CREATOR = new android.os.Parcelable.Creator<DemuxFilterSettings>() {
    @Override
    public DemuxFilterSettings createFromParcel(android.os.Parcel _aidl_source) {
      return new DemuxFilterSettings(_aidl_source);
    }
    @Override
    public DemuxFilterSettings[] newArray(int _aidl_size) {
      return new DemuxFilterSettings[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case ts:
      _aidl_parcel.writeTypedObject(getTs(), _aidl_flag);
      break;
    case mmtp:
      _aidl_parcel.writeTypedObject(getMmtp(), _aidl_flag);
      break;
    case ip:
      _aidl_parcel.writeTypedObject(getIp(), _aidl_flag);
      break;
    case tlv:
      _aidl_parcel.writeTypedObject(getTlv(), _aidl_flag);
      break;
    case alp:
      _aidl_parcel.writeTypedObject(getAlp(), _aidl_flag);
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case ts: {
      android.hardware.tv.tuner.DemuxTsFilterSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxTsFilterSettings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case mmtp: {
      android.hardware.tv.tuner.DemuxMmtpFilterSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxMmtpFilterSettings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case ip: {
      android.hardware.tv.tuner.DemuxIpFilterSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxIpFilterSettings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case tlv: {
      android.hardware.tv.tuner.DemuxTlvFilterSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxTlvFilterSettings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case alp: {
      android.hardware.tv.tuner.DemuxAlpFilterSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxAlpFilterSettings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    case ts:
      _mask |= describeContents(getTs());
      break;
    case mmtp:
      _mask |= describeContents(getMmtp());
      break;
    case ip:
      _mask |= describeContents(getIp());
      break;
    case tlv:
      _mask |= describeContents(getTlv());
      break;
    case alp:
      _mask |= describeContents(getAlp());
      break;
    }
    return _mask;
  }
  private int describeContents(Object _v) {
    if (_v == null) return 0;
    if (_v instanceof android.os.Parcelable) {
      return ((android.os.Parcelable) _v).describeContents();
    }
    return 0;
  }

  private void _assertTag(int tag) {
    if (getTag() != tag) {
      throw new IllegalStateException("bad access: " + _tagString(tag) + ", " + _tagString(getTag()) + " is available.");
    }
  }

  private String _tagString(int _tag) {
    switch (_tag) {
    case ts: return "ts";
    case mmtp: return "mmtp";
    case ip: return "ip";
    case tlv: return "tlv";
    case alp: return "alp";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int ts = 0;
    public static final int mmtp = 1;
    public static final int ip = 2;
    public static final int tlv = 3;
    public static final int alp = 4;
  }
}
