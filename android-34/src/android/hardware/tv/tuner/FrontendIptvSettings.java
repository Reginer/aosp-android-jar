/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public class FrontendIptvSettings implements android.os.Parcelable
{
  public int protocol = android.hardware.tv.tuner.FrontendIptvSettingsProtocol.UNDEFINED;
  public android.hardware.tv.tuner.FrontendIptvSettingsFec fec;
  public int igmp = android.hardware.tv.tuner.FrontendIptvSettingsIgmp.UNDEFINED;
  public long bitrate = 0L;
  public android.hardware.tv.tuner.DemuxIpAddress ipAddr;
  public java.lang.String contentUrl;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<FrontendIptvSettings> CREATOR = new android.os.Parcelable.Creator<FrontendIptvSettings>() {
    @Override
    public FrontendIptvSettings createFromParcel(android.os.Parcel _aidl_source) {
      FrontendIptvSettings _aidl_out = new FrontendIptvSettings();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public FrontendIptvSettings[] newArray(int _aidl_size) {
      return new FrontendIptvSettings[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(protocol);
    _aidl_parcel.writeTypedObject(fec, _aidl_flag);
    _aidl_parcel.writeInt(igmp);
    _aidl_parcel.writeLong(bitrate);
    _aidl_parcel.writeTypedObject(ipAddr, _aidl_flag);
    _aidl_parcel.writeString(contentUrl);
    int _aidl_end_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.setDataPosition(_aidl_start_pos);
    _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
    _aidl_parcel.setDataPosition(_aidl_end_pos);
  }
  public final void readFromParcel(android.os.Parcel _aidl_parcel)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    int _aidl_parcelable_size = _aidl_parcel.readInt();
    try {
      if (_aidl_parcelable_size < 4) throw new android.os.BadParcelableException("Parcelable too small");;
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      protocol = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      fec = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendIptvSettingsFec.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      igmp = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      bitrate = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      ipAddr = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxIpAddress.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      contentUrl = _aidl_parcel.readString();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(fec);
    _mask |= describeContents(ipAddr);
    return _mask;
  }
  private int describeContents(Object _v) {
    if (_v == null) return 0;
    if (_v instanceof android.os.Parcelable) {
      return ((android.os.Parcelable) _v).describeContents();
    }
    return 0;
  }
}
