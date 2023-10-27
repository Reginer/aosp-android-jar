/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class DemuxAlpFilterSettingsFilterSettings implements android.os.Parcelable {
  // tags for union fields
  public final static int noinit = 0;  // boolean noinit;
  public final static int section = 1;  // android.hardware.tv.tuner.DemuxFilterSectionSettings section;

  private int _tag;
  private Object _value;

  public DemuxAlpFilterSettingsFilterSettings() {
    boolean _value = false;
    this._tag = noinit;
    this._value = _value;
  }

  private DemuxAlpFilterSettingsFilterSettings(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private DemuxAlpFilterSettingsFilterSettings(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // boolean noinit;

  public static DemuxAlpFilterSettingsFilterSettings noinit(boolean _value) {
    return new DemuxAlpFilterSettingsFilterSettings(noinit, _value);
  }

  public boolean getNoinit() {
    _assertTag(noinit);
    return (boolean) _value;
  }

  public void setNoinit(boolean _value) {
    _set(noinit, _value);
  }

  // android.hardware.tv.tuner.DemuxFilterSectionSettings section;

  public static DemuxAlpFilterSettingsFilterSettings section(android.hardware.tv.tuner.DemuxFilterSectionSettings _value) {
    return new DemuxAlpFilterSettingsFilterSettings(section, _value);
  }

  public android.hardware.tv.tuner.DemuxFilterSectionSettings getSection() {
    _assertTag(section);
    return (android.hardware.tv.tuner.DemuxFilterSectionSettings) _value;
  }

  public void setSection(android.hardware.tv.tuner.DemuxFilterSectionSettings _value) {
    _set(section, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<DemuxAlpFilterSettingsFilterSettings> CREATOR = new android.os.Parcelable.Creator<DemuxAlpFilterSettingsFilterSettings>() {
    @Override
    public DemuxAlpFilterSettingsFilterSettings createFromParcel(android.os.Parcel _aidl_source) {
      return new DemuxAlpFilterSettingsFilterSettings(_aidl_source);
    }
    @Override
    public DemuxAlpFilterSettingsFilterSettings[] newArray(int _aidl_size) {
      return new DemuxAlpFilterSettingsFilterSettings[_aidl_size];
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
  }
}
