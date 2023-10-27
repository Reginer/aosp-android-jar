/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class DemuxFilterMonitorEvent implements android.os.Parcelable {
  // tags for union fields
  public final static int scramblingStatus = 0;  // android.hardware.tv.tuner.ScramblingStatus scramblingStatus;
  public final static int cid = 1;  // int cid;

  private int _tag;
  private Object _value;

  public DemuxFilterMonitorEvent() {
    int _value = android.hardware.tv.tuner.ScramblingStatus.UNKNOWN;
    this._tag = scramblingStatus;
    this._value = _value;
  }

  private DemuxFilterMonitorEvent(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private DemuxFilterMonitorEvent(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.tv.tuner.ScramblingStatus scramblingStatus;

  public static DemuxFilterMonitorEvent scramblingStatus(int _value) {
    return new DemuxFilterMonitorEvent(scramblingStatus, _value);
  }

  public int getScramblingStatus() {
    _assertTag(scramblingStatus);
    return (int) _value;
  }

  public void setScramblingStatus(int _value) {
    _set(scramblingStatus, _value);
  }

  // int cid;

  public static DemuxFilterMonitorEvent cid(int _value) {
    return new DemuxFilterMonitorEvent(cid, _value);
  }

  public int getCid() {
    _assertTag(cid);
    return (int) _value;
  }

  public void setCid(int _value) {
    _set(cid, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<DemuxFilterMonitorEvent> CREATOR = new android.os.Parcelable.Creator<DemuxFilterMonitorEvent>() {
    @Override
    public DemuxFilterMonitorEvent createFromParcel(android.os.Parcel _aidl_source) {
      return new DemuxFilterMonitorEvent(_aidl_source);
    }
    @Override
    public DemuxFilterMonitorEvent[] newArray(int _aidl_size) {
      return new DemuxFilterMonitorEvent[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case scramblingStatus:
      _aidl_parcel.writeInt(getScramblingStatus());
      break;
    case cid:
      _aidl_parcel.writeInt(getCid());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case scramblingStatus: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case cid: {
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
    case scramblingStatus: return "scramblingStatus";
    case cid: return "cid";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int scramblingStatus = 0;
    public static final int cid = 1;
  }
}
