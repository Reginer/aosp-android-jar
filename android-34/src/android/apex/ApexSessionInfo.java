/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.apex;
public class ApexSessionInfo implements android.os.Parcelable
{
  public int sessionId = 0;
  // Maps to apex::proto::SessionState::State enum.
  public boolean isUnknown = false;
  public boolean isVerified = false;
  public boolean isStaged = false;
  public boolean isActivated = false;
  public boolean isRevertInProgress = false;
  public boolean isActivationFailed = false;
  public boolean isSuccess = false;
  public boolean isReverted = false;
  public boolean isRevertFailed = false;
  public java.lang.String crashingNativeProcess;
  public java.lang.String errorMessage;
  public static final android.os.Parcelable.Creator<ApexSessionInfo> CREATOR = new android.os.Parcelable.Creator<ApexSessionInfo>() {
    @Override
    public ApexSessionInfo createFromParcel(android.os.Parcel _aidl_source) {
      ApexSessionInfo _aidl_out = new ApexSessionInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ApexSessionInfo[] newArray(int _aidl_size) {
      return new ApexSessionInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(sessionId);
    _aidl_parcel.writeInt(((isUnknown)?(1):(0)));
    _aidl_parcel.writeInt(((isVerified)?(1):(0)));
    _aidl_parcel.writeInt(((isStaged)?(1):(0)));
    _aidl_parcel.writeInt(((isActivated)?(1):(0)));
    _aidl_parcel.writeInt(((isRevertInProgress)?(1):(0)));
    _aidl_parcel.writeInt(((isActivationFailed)?(1):(0)));
    _aidl_parcel.writeInt(((isSuccess)?(1):(0)));
    _aidl_parcel.writeInt(((isReverted)?(1):(0)));
    _aidl_parcel.writeInt(((isRevertFailed)?(1):(0)));
    _aidl_parcel.writeString(crashingNativeProcess);
    _aidl_parcel.writeString(errorMessage);
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
      isUnknown = (0!=_aidl_parcel.readInt());
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isVerified = (0!=_aidl_parcel.readInt());
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isStaged = (0!=_aidl_parcel.readInt());
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isActivated = (0!=_aidl_parcel.readInt());
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isRevertInProgress = (0!=_aidl_parcel.readInt());
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isActivationFailed = (0!=_aidl_parcel.readInt());
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isSuccess = (0!=_aidl_parcel.readInt());
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isReverted = (0!=_aidl_parcel.readInt());
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isRevertFailed = (0!=_aidl_parcel.readInt());
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      crashingNativeProcess = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      errorMessage = _aidl_parcel.readString();
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
