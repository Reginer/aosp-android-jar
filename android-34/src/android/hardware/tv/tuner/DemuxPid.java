/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class DemuxPid implements android.os.Parcelable {
  // tags for union fields
  public final static int tPid = 0;  // int tPid;
  public final static int mmtpPid = 1;  // int mmtpPid;

  private int _tag;
  private Object _value;

  public DemuxPid() {
    int _value = 0;
    this._tag = tPid;
    this._value = _value;
  }

  private DemuxPid(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private DemuxPid(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // int tPid;

  public static DemuxPid tPid(int _value) {
    return new DemuxPid(tPid, _value);
  }

  public int getTPid() {
    _assertTag(tPid);
    return (int) _value;
  }

  public void setTPid(int _value) {
    _set(tPid, _value);
  }

  // int mmtpPid;

  public static DemuxPid mmtpPid(int _value) {
    return new DemuxPid(mmtpPid, _value);
  }

  public int getMmtpPid() {
    _assertTag(mmtpPid);
    return (int) _value;
  }

  public void setMmtpPid(int _value) {
    _set(mmtpPid, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<DemuxPid> CREATOR = new android.os.Parcelable.Creator<DemuxPid>() {
    @Override
    public DemuxPid createFromParcel(android.os.Parcel _aidl_source) {
      return new DemuxPid(_aidl_source);
    }
    @Override
    public DemuxPid[] newArray(int _aidl_size) {
      return new DemuxPid[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case tPid:
      _aidl_parcel.writeInt(getTPid());
      break;
    case mmtpPid:
      _aidl_parcel.writeInt(getMmtpPid());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case tPid: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case mmtpPid: {
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
    case tPid: return "tPid";
    case mmtpPid: return "mmtpPid";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int tPid = 0;
    public static final int mmtpPid = 1;
  }
}
