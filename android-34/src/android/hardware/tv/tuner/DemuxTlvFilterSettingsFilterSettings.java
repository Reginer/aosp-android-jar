/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class DemuxTlvFilterSettingsFilterSettings implements android.os.Parcelable {
  // tags for union fields
  public final static int noinit = 0;  // boolean noinit;
  public final static int section = 1;  // android.hardware.tv.tuner.DemuxFilterSectionSettings section;
  public final static int bPassthrough = 2;  // boolean bPassthrough;

  private int _tag;
  private Object _value;

  public DemuxTlvFilterSettingsFilterSettings() {
    boolean _value = false;
    this._tag = noinit;
    this._value = _value;
  }

  private DemuxTlvFilterSettingsFilterSettings(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private DemuxTlvFilterSettingsFilterSettings(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // boolean noinit;

  public static DemuxTlvFilterSettingsFilterSettings noinit(boolean _value) {
    return new DemuxTlvFilterSettingsFilterSettings(noinit, _value);
  }

  public boolean getNoinit() {
    _assertTag(noinit);
    return (boolean) _value;
  }

  public void setNoinit(boolean _value) {
    _set(noinit, _value);
  }

  // android.hardware.tv.tuner.DemuxFilterSectionSettings section;

  public static DemuxTlvFilterSettingsFilterSettings section(android.hardware.tv.tuner.DemuxFilterSectionSettings _value) {
    return new DemuxTlvFilterSettingsFilterSettings(section, _value);
  }

  public android.hardware.tv.tuner.DemuxFilterSectionSettings getSection() {
    _assertTag(section);
    return (android.hardware.tv.tuner.DemuxFilterSectionSettings) _value;
  }

  public void setSection(android.hardware.tv.tuner.DemuxFilterSectionSettings _value) {
    _set(section, _value);
  }

  // boolean bPassthrough;

  public static DemuxTlvFilterSettingsFilterSettings bPassthrough(boolean _value) {
    return new DemuxTlvFilterSettingsFilterSettings(bPassthrough, _value);
  }

  public boolean getBPassthrough() {
    _assertTag(bPassthrough);
    return (boolean) _value;
  }

  public void setBPassthrough(boolean _value) {
    _set(bPassthrough, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<DemuxTlvFilterSettingsFilterSettings> CREATOR = new android.os.Parcelable.Creator<DemuxTlvFilterSettingsFilterSettings>() {
    @Override
    public DemuxTlvFilterSettingsFilterSettings createFromParcel(android.os.Parcel _aidl_source) {
      return new DemuxTlvFilterSettingsFilterSettings(_aidl_source);
    }
    @Override
    public DemuxTlvFilterSettingsFilterSettings[] newArray(int _aidl_size) {
      return new DemuxTlvFilterSettingsFilterSettings[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case noinit:
      _aidl_parcel.writeBoolean(getNoinit());
      break;
    case section:
      _aidl_parcel.writeTypedObject(getSection(), _aidl_flag);
      break;
    case bPassthrough:
      _aidl_parcel.writeBoolean(getBPassthrough());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case noinit: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    case section: {
      android.hardware.tv.tuner.DemuxFilterSectionSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxFilterSectionSettings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case bPassthrough: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    case section:
      _mask |= describeContents(getSection());
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
    case noinit: return "noinit";
    case section: return "section";
    case bPassthrough: return "bPassthrough";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int noinit = 0;
    public static final int section = 1;
    public static final int bPassthrough = 2;
  }
}
