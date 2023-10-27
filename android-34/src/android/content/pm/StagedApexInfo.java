/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.content.pm;
/**
 * This object is designed for returning information regarding
 * staged APEX that are ready to be installed on next reboot.
 * 
 * @hide
 */
public class StagedApexInfo implements android.os.Parcelable
{
  public java.lang.String moduleName;
  public java.lang.String diskImagePath;
  public long versionCode = 0L;
  public java.lang.String versionName;
  public boolean hasClassPathJars = false;
  public static final android.os.Parcelable.Creator<StagedApexInfo> CREATOR = new android.os.Parcelable.Creator<StagedApexInfo>() {
    @Override
    public StagedApexInfo createFromParcel(android.os.Parcel _aidl_source) {
      StagedApexInfo _aidl_out = new StagedApexInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public StagedApexInfo[] newArray(int _aidl_size) {
      return new StagedApexInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(moduleName);
    _aidl_parcel.writeString(diskImagePath);
    _aidl_parcel.writeLong(versionCode);
    _aidl_parcel.writeString(versionName);
    _aidl_parcel.writeBoolean(hasClassPathJars);
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
      moduleName = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      diskImagePath = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      versionCode = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      versionName = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      hasClassPathJars = _aidl_parcel.readBoolean();
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
    return _mask;
  }
}
