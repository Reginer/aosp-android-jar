/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class DemuxFilterScIndexMask implements android.os.Parcelable {
  // tags for union fields
  public final static int scIndex = 0;  // int scIndex;
  public final static int scAvc = 1;  // int scAvc;
  public final static int scHevc = 2;  // int scHevc;
  public final static int scVvc = 3;  // int scVvc;

  private int _tag;
  private Object _value;

  public DemuxFilterScIndexMask() {
    int _value = 0;
    this._tag = scIndex;
    this._value = _value;
  }

  private DemuxFilterScIndexMask(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private DemuxFilterScIndexMask(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // int scIndex;

  public static DemuxFilterScIndexMask scIndex(int _value) {
    return new DemuxFilterScIndexMask(scIndex, _value);
  }

  public int getScIndex() {
    _assertTag(scIndex);
    return (int) _value;
  }

  public void setScIndex(int _value) {
    _set(scIndex, _value);
  }

  // int scAvc;

  public static DemuxFilterScIndexMask scAvc(int _value) {
    return new DemuxFilterScIndexMask(scAvc, _value);
  }

  public int getScAvc() {
    _assertTag(scAvc);
    return (int) _value;
  }

  public void setScAvc(int _value) {
    _set(scAvc, _value);
  }

  // int scHevc;

  public static DemuxFilterScIndexMask scHevc(int _value) {
    return new DemuxFilterScIndexMask(scHevc, _value);
  }

  public int getScHevc() {
    _assertTag(scHevc);
    return (int) _value;
  }

  public void setScHevc(int _value) {
    _set(scHevc, _value);
  }

  // int scVvc;

  public static DemuxFilterScIndexMask scVvc(int _value) {
    return new DemuxFilterScIndexMask(scVvc, _value);
  }

  public int getScVvc() {
    _assertTag(scVvc);
    return (int) _value;
  }

  public void setScVvc(int _value) {
    _set(scVvc, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<DemuxFilterScIndexMask> CREATOR = new android.os.Parcelable.Creator<DemuxFilterScIndexMask>() {
    @Override
    public DemuxFilterScIndexMask createFromParcel(android.os.Parcel _aidl_source) {
      return new DemuxFilterScIndexMask(_aidl_source);
    }
    @Override
    public DemuxFilterScIndexMask[] newArray(int _aidl_size) {
      return new DemuxFilterScIndexMask[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case scIndex:
      _aidl_parcel.writeInt(getScIndex());
      break;
    case scAvc:
      _aidl_parcel.writeInt(getScAvc());
      break;
    case scHevc:
      _aidl_parcel.writeInt(getScHevc());
      break;
    case scVvc:
      _aidl_parcel.writeInt(getScVvc());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case scIndex: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case scAvc: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case scHevc: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case scVvc: {
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
    case scIndex: return "scIndex";
    case scAvc: return "scAvc";
    case scHevc: return "scHevc";
    case scVvc: return "scVvc";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int scIndex = 0;
    public static final int scAvc = 1;
    public static final int scHevc = 2;
    public static final int scVvc = 3;
  }
}
