/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class DemuxTsFilterSettingsFilterSettings implements android.os.Parcelable {
  // tags for union fields
  public final static int noinit = 0;  // boolean noinit;
  public final static int section = 1;  // android.hardware.tv.tuner.DemuxFilterSectionSettings section;
  public final static int av = 2;  // android.hardware.tv.tuner.DemuxFilterAvSettings av;
  public final static int pesData = 3;  // android.hardware.tv.tuner.DemuxFilterPesDataSettings pesData;
  public final static int record = 4;  // android.hardware.tv.tuner.DemuxFilterRecordSettings record;

  private int _tag;
  private Object _value;

  public DemuxTsFilterSettingsFilterSettings() {
    boolean _value = false;
    this._tag = noinit;
    this._value = _value;
  }

  private DemuxTsFilterSettingsFilterSettings(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private DemuxTsFilterSettingsFilterSettings(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // boolean noinit;

  public static DemuxTsFilterSettingsFilterSettings noinit(boolean _value) {
    return new DemuxTsFilterSettingsFilterSettings(noinit, _value);
  }

  public boolean getNoinit() {
    _assertTag(noinit);
    return (boolean) _value;
  }

  public void setNoinit(boolean _value) {
    _set(noinit, _value);
  }

  // android.hardware.tv.tuner.DemuxFilterSectionSettings section;

  public static DemuxTsFilterSettingsFilterSettings section(android.hardware.tv.tuner.DemuxFilterSectionSettings _value) {
    return new DemuxTsFilterSettingsFilterSettings(section, _value);
  }

  public android.hardware.tv.tuner.DemuxFilterSectionSettings getSection() {
    _assertTag(section);
    return (android.hardware.tv.tuner.DemuxFilterSectionSettings) _value;
  }

  public void setSection(android.hardware.tv.tuner.DemuxFilterSectionSettings _value) {
    _set(section, _value);
  }

  // android.hardware.tv.tuner.DemuxFilterAvSettings av;

  public static DemuxTsFilterSettingsFilterSettings av(android.hardware.tv.tuner.DemuxFilterAvSettings _value) {
    return new DemuxTsFilterSettingsFilterSettings(av, _value);
  }

  public android.hardware.tv.tuner.DemuxFilterAvSettings getAv() {
    _assertTag(av);
    return (android.hardware.tv.tuner.DemuxFilterAvSettings) _value;
  }

  public void setAv(android.hardware.tv.tuner.DemuxFilterAvSettings _value) {
    _set(av, _value);
  }

  // android.hardware.tv.tuner.DemuxFilterPesDataSettings pesData;

  public static DemuxTsFilterSettingsFilterSettings pesData(android.hardware.tv.tuner.DemuxFilterPesDataSettings _value) {
    return new DemuxTsFilterSettingsFilterSettings(pesData, _value);
  }

  public android.hardware.tv.tuner.DemuxFilterPesDataSettings getPesData() {
    _assertTag(pesData);
    return (android.hardware.tv.tuner.DemuxFilterPesDataSettings) _value;
  }

  public void setPesData(android.hardware.tv.tuner.DemuxFilterPesDataSettings _value) {
    _set(pesData, _value);
  }

  // android.hardware.tv.tuner.DemuxFilterRecordSettings record;

  public static DemuxTsFilterSettingsFilterSettings record(android.hardware.tv.tuner.DemuxFilterRecordSettings _value) {
    return new DemuxTsFilterSettingsFilterSettings(record, _value);
  }

  public android.hardware.tv.tuner.DemuxFilterRecordSettings getRecord() {
    _assertTag(record);
    return (android.hardware.tv.tuner.DemuxFilterRecordSettings) _value;
  }

  public void setRecord(android.hardware.tv.tuner.DemuxFilterRecordSettings _value) {
    _set(record, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<DemuxTsFilterSettingsFilterSettings> CREATOR = new android.os.Parcelable.Creator<DemuxTsFilterSettingsFilterSettings>() {
    @Override
    public DemuxTsFilterSettingsFilterSettings createFromParcel(android.os.Parcel _aidl_source) {
      return new DemuxTsFilterSettingsFilterSettings(_aidl_source);
    }
    @Override
    public DemuxTsFilterSettingsFilterSettings[] newArray(int _aidl_size) {
      return new DemuxTsFilterSettingsFilterSettings[_aidl_size];
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
    case av:
      _aidl_parcel.writeTypedObject(getAv(), _aidl_flag);
      break;
    case pesData:
      _aidl_parcel.writeTypedObject(getPesData(), _aidl_flag);
      break;
    case record:
      _aidl_parcel.writeTypedObject(getRecord(), _aidl_flag);
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
    case av: {
      android.hardware.tv.tuner.DemuxFilterAvSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxFilterAvSettings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case pesData: {
      android.hardware.tv.tuner.DemuxFilterPesDataSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxFilterPesDataSettings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case record: {
      android.hardware.tv.tuner.DemuxFilterRecordSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxFilterRecordSettings.CREATOR);
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
    case av:
      _mask |= describeContents(getAv());
      break;
    case pesData:
      _mask |= describeContents(getPesData());
      break;
    case record:
      _mask |= describeContents(getRecord());
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
    case av: return "av";
    case pesData: return "pesData";
    case record: return "record";
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
    public static final int av = 2;
    public static final int pesData = 3;
    public static final int record = 4;
  }
}
