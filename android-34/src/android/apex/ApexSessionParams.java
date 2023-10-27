/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.apex;
public class ApexSessionParams implements android.os.Parcelable
{
  public int sessionId = 0;
  public int[] childSessionIds = {};
  public boolean hasRollbackEnabled = false;
  public boolean isRollback = false;
  public int rollbackId = 0;
  public static final android.os.Parcelable.Creator<ApexSessionParams> CREATOR = new android.os.Parcelable.Creator<ApexSessionParams>() {
    @Override
    public ApexSessionParams createFromParcel(android.os.Parcel _aidl_source) {
      ApexSessionParams _aidl_out = new ApexSessionParams();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ApexSessionParams[] newArray(int _aidl_size) {
      return new ApexSessionParams[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(sessionId);
    _aidl_parcel.writeIntArray(childSessionIds);
    _aidl_parcel.writeInt(((hasRollbackEnabled)?(1):(0)));
    _aidl_parcel.writeInt(((isRollback)?(1):(0)));
    _aidl_parcel.writeInt(rollbackId);
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
      sessionId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      childSessionIds = _aidl_parcel.createIntArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      hasRollbackEnabled = (0!=_aidl_parcel.readInt());
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isRollback = (0!=_aidl_parcel.readInt());
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      rollbackId = _aidl_parcel.readInt();
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
