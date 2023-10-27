/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class DemuxFilterEvent implements android.os.Parcelable {
  // tags for union fields
  public final static int section = 0;  // android.hardware.tv.tuner.DemuxFilterSectionEvent section;
  public final static int media = 1;  // android.hardware.tv.tuner.DemuxFilterMediaEvent media;
  public final static int pes = 2;  // android.hardware.tv.tuner.DemuxFilterPesEvent pes;
  public final static int tsRecord = 3;  // android.hardware.tv.tuner.DemuxFilterTsRecordEvent tsRecord;
  public final static int mmtpRecord = 4;  // android.hardware.tv.tuner.DemuxFilterMmtpRecordEvent mmtpRecord;
  public final static int download = 5;  // android.hardware.tv.tuner.DemuxFilterDownloadEvent download;
  public final static int ipPayload = 6;  // android.hardware.tv.tuner.DemuxFilterIpPayloadEvent ipPayload;
  public final static int temi = 7;  // android.hardware.tv.tuner.DemuxFilterTemiEvent temi;
  public final static int monitorEvent = 8;  // android.hardware.tv.tuner.DemuxFilterMonitorEvent monitorEvent;
  public final static int startId = 9;  // int startId;

  private int _tag;
  private Object _value;

  public DemuxFilterEvent() {
    android.hardware.tv.tuner.DemuxFilterSectionEvent _value = null;
    this._tag = section;
    this._value = _value;
  }

  private DemuxFilterEvent(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private DemuxFilterEvent(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.tv.tuner.DemuxFilterSectionEvent section;

  public static DemuxFilterEvent section(android.hardware.tv.tuner.DemuxFilterSectionEvent _value) {
    return new DemuxFilterEvent(section, _value);
  }

  public android.hardware.tv.tuner.DemuxFilterSectionEvent getSection() {
    _assertTag(section);
    return (android.hardware.tv.tuner.DemuxFilterSectionEvent) _value;
  }

  public void setSection(android.hardware.tv.tuner.DemuxFilterSectionEvent _value) {
    _set(section, _value);
  }

  // android.hardware.tv.tuner.DemuxFilterMediaEvent media;

  public static DemuxFilterEvent media(android.hardware.tv.tuner.DemuxFilterMediaEvent _value) {
    return new DemuxFilterEvent(media, _value);
  }

  public android.hardware.tv.tuner.DemuxFilterMediaEvent getMedia() {
    _assertTag(media);
    return (android.hardware.tv.tuner.DemuxFilterMediaEvent) _value;
  }

  public void setMedia(android.hardware.tv.tuner.DemuxFilterMediaEvent _value) {
    _set(media, _value);
  }

  // android.hardware.tv.tuner.DemuxFilterPesEvent pes;

  public static DemuxFilterEvent pes(android.hardware.tv.tuner.DemuxFilterPesEvent _value) {
    return new DemuxFilterEvent(pes, _value);
  }

  public android.hardware.tv.tuner.DemuxFilterPesEvent getPes() {
    _assertTag(pes);
    return (android.hardware.tv.tuner.DemuxFilterPesEvent) _value;
  }

  public void setPes(android.hardware.tv.tuner.DemuxFilterPesEvent _value) {
    _set(pes, _value);
  }

  // android.hardware.tv.tuner.DemuxFilterTsRecordEvent tsRecord;

  public static DemuxFilterEvent tsRecord(android.hardware.tv.tuner.DemuxFilterTsRecordEvent _value) {
    return new DemuxFilterEvent(tsRecord, _value);
  }

  public android.hardware.tv.tuner.DemuxFilterTsRecordEvent getTsRecord() {
    _assertTag(tsRecord);
    return (android.hardware.tv.tuner.DemuxFilterTsRecordEvent) _value;
  }

  public void setTsRecord(android.hardware.tv.tuner.DemuxFilterTsRecordEvent _value) {
    _set(tsRecord, _value);
  }

  // android.hardware.tv.tuner.DemuxFilterMmtpRecordEvent mmtpRecord;

  public static DemuxFilterEvent mmtpRecord(android.hardware.tv.tuner.DemuxFilterMmtpRecordEvent _value) {
    return new DemuxFilterEvent(mmtpRecord, _value);
  }

  public android.hardware.tv.tuner.DemuxFilterMmtpRecordEvent getMmtpRecord() {
    _assertTag(mmtpRecord);
    return (android.hardware.tv.tuner.DemuxFilterMmtpRecordEvent) _value;
  }

  public void setMmtpRecord(android.hardware.tv.tuner.DemuxFilterMmtpRecordEvent _value) {
    _set(mmtpRecord, _value);
  }

  // android.hardware.tv.tuner.DemuxFilterDownloadEvent download;

  public static DemuxFilterEvent download(android.hardware.tv.tuner.DemuxFilterDownloadEvent _value) {
    return new DemuxFilterEvent(download, _value);
  }

  public android.hardware.tv.tuner.DemuxFilterDownloadEvent getDownload() {
    _assertTag(download);
    return (android.hardware.tv.tuner.DemuxFilterDownloadEvent) _value;
  }

  public void setDownload(android.hardware.tv.tuner.DemuxFilterDownloadEvent _value) {
    _set(download, _value);
  }

  // android.hardware.tv.tuner.DemuxFilterIpPayloadEvent ipPayload;

  public static DemuxFilterEvent ipPayload(android.hardware.tv.tuner.DemuxFilterIpPayloadEvent _value) {
    return new DemuxFilterEvent(ipPayload, _value);
  }

  public android.hardware.tv.tuner.DemuxFilterIpPayloadEvent getIpPayload() {
    _assertTag(ipPayload);
    return (android.hardware.tv.tuner.DemuxFilterIpPayloadEvent) _value;
  }

  public void setIpPayload(android.hardware.tv.tuner.DemuxFilterIpPayloadEvent _value) {
    _set(ipPayload, _value);
  }

  // android.hardware.tv.tuner.DemuxFilterTemiEvent temi;

  public static DemuxFilterEvent temi(android.hardware.tv.tuner.DemuxFilterTemiEvent _value) {
    return new DemuxFilterEvent(temi, _value);
  }

  public android.hardware.tv.tuner.DemuxFilterTemiEvent getTemi() {
    _assertTag(temi);
    return (android.hardware.tv.tuner.DemuxFilterTemiEvent) _value;
  }

  public void setTemi(android.hardware.tv.tuner.DemuxFilterTemiEvent _value) {
    _set(temi, _value);
  }

  // android.hardware.tv.tuner.DemuxFilterMonitorEvent monitorEvent;

  public static DemuxFilterEvent monitorEvent(android.hardware.tv.tuner.DemuxFilterMonitorEvent _value) {
    return new DemuxFilterEvent(monitorEvent, _value);
  }

  public android.hardware.tv.tuner.DemuxFilterMonitorEvent getMonitorEvent() {
    _assertTag(monitorEvent);
    return (android.hardware.tv.tuner.DemuxFilterMonitorEvent) _value;
  }

  public void setMonitorEvent(android.hardware.tv.tuner.DemuxFilterMonitorEvent _value) {
    _set(monitorEvent, _value);
  }

  // int startId;

  public static DemuxFilterEvent startId(int _value) {
    return new DemuxFilterEvent(startId, _value);
  }

  public int getStartId() {
    _assertTag(startId);
    return (int) _value;
  }

  public void setStartId(int _value) {
    _set(startId, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<DemuxFilterEvent> CREATOR = new android.os.Parcelable.Creator<DemuxFilterEvent>() {
    @Override
    public DemuxFilterEvent createFromParcel(android.os.Parcel _aidl_source) {
      return new DemuxFilterEvent(_aidl_source);
    }
    @Override
    public DemuxFilterEvent[] newArray(int _aidl_size) {
      return new DemuxFilterEvent[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case section:
      _aidl_parcel.writeTypedObject(getSection(), _aidl_flag);
      break;
    case media:
      _aidl_parcel.writeTypedObject(getMedia(), _aidl_flag);
      break;
    case pes:
      _aidl_parcel.writeTypedObject(getPes(), _aidl_flag);
      break;
    case tsRecord:
      _aidl_parcel.writeTypedObject(getTsRecord(), _aidl_flag);
      break;
    case mmtpRecord:
      _aidl_parcel.writeTypedObject(getMmtpRecord(), _aidl_flag);
      break;
    case download:
      _aidl_parcel.writeTypedObject(getDownload(), _aidl_flag);
      break;
    case ipPayload:
      _aidl_parcel.writeTypedObject(getIpPayload(), _aidl_flag);
      break;
    case temi:
      _aidl_parcel.writeTypedObject(getTemi(), _aidl_flag);
      break;
    case monitorEvent:
      _aidl_parcel.writeTypedObject(getMonitorEvent(), _aidl_flag);
      break;
    case startId:
      _aidl_parcel.writeInt(getStartId());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case section: {
      android.hardware.tv.tuner.DemuxFilterSectionEvent _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxFilterSectionEvent.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case media: {
      android.hardware.tv.tuner.DemuxFilterMediaEvent _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxFilterMediaEvent.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case pes: {
      android.hardware.tv.tuner.DemuxFilterPesEvent _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxFilterPesEvent.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case tsRecord: {
      android.hardware.tv.tuner.DemuxFilterTsRecordEvent _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxFilterTsRecordEvent.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case mmtpRecord: {
      android.hardware.tv.tuner.DemuxFilterMmtpRecordEvent _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxFilterMmtpRecordEvent.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case download: {
      android.hardware.tv.tuner.DemuxFilterDownloadEvent _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxFilterDownloadEvent.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case ipPayload: {
      android.hardware.tv.tuner.DemuxFilterIpPayloadEvent _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxFilterIpPayloadEvent.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case temi: {
      android.hardware.tv.tuner.DemuxFilterTemiEvent _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxFilterTemiEvent.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case monitorEvent: {
      android.hardware.tv.tuner.DemuxFilterMonitorEvent _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxFilterMonitorEvent.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case startId: {
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
    case section:
      _mask |= describeContents(getSection());
      break;
    case media:
      _mask |= describeContents(getMedia());
      break;
    case pes:
      _mask |= describeContents(getPes());
      break;
    case tsRecord:
      _mask |= describeContents(getTsRecord());
      break;
    case mmtpRecord:
      _mask |= describeContents(getMmtpRecord());
      break;
    case download:
      _mask |= describeContents(getDownload());
      break;
    case ipPayload:
      _mask |= describeContents(getIpPayload());
      break;
    case temi:
      _mask |= describeContents(getTemi());
      break;
    case monitorEvent:
      _mask |= describeContents(getMonitorEvent());
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
    case section: return "section";
    case media: return "media";
    case pes: return "pes";
    case tsRecord: return "tsRecord";
    case mmtpRecord: return "mmtpRecord";
    case download: return "download";
    case ipPayload: return "ipPayload";
    case temi: return "temi";
    case monitorEvent: return "monitorEvent";
    case startId: return "startId";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int section = 0;
    public static final int media = 1;
    public static final int pes = 2;
    public static final int tsRecord = 3;
    public static final int mmtpRecord = 4;
    public static final int download = 5;
    public static final int ipPayload = 6;
    public static final int temi = 7;
    public static final int monitorEvent = 8;
    public static final int startId = 9;
  }
}
