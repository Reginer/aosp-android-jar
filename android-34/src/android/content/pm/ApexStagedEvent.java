/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.content.pm;
/**
 * This event is designed for notification to native code listener about
 * any changes to set of apex packages staged for installation on next boot.
 * 
 * @hide
 */
public class ApexStagedEvent implements android.os.Parcelable
{
  public java.lang.String[] stagedApexModuleNames;
  public static final android.os.Parcelable.Creator<ApexStagedEvent> CREATOR = new android.os.Parcelable.Creator<ApexStagedEvent>() {
    @Override
    public ApexStagedEvent createFromParcel(android.os.Parcel _aidl_source) {
      ApexStagedEvent _aidl_out = new ApexStagedEvent();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ApexStagedEvent[] newArray(int _aidl_size) {
      return new ApexStagedEvent[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeStringArray(stagedApexModuleNames);
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
      stagedApexModuleNames = _aidl_parcel.createStringArray();
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
