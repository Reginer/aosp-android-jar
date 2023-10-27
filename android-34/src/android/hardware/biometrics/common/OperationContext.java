/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.biometrics.common;
/** @hide */
public class OperationContext implements android.os.Parcelable
{
  public int id = 0;
  public byte reason = android.hardware.biometrics.common.OperationReason.UNKNOWN;
  /** @deprecated use displayState instead. */
  @Deprecated
  public boolean isAod = false;
  public boolean isCrypto = false;
  public int wakeReason = android.hardware.biometrics.common.WakeReason.UNKNOWN;
  public int displayState = android.hardware.biometrics.common.DisplayState.UNKNOWN;
  public android.hardware.biometrics.common.AuthenticateReason authenticateReason;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<OperationContext> CREATOR = new android.os.Parcelable.Creator<OperationContext>() {
    @Override
    public OperationContext createFromParcel(android.os.Parcel _aidl_source) {
      OperationContext _aidl_out = new OperationContext();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public OperationContext[] newArray(int _aidl_size) {
      return new OperationContext[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(id);
    _aidl_parcel.writeByte(reason);
    _aidl_parcel.writeBoolean(isAod);
    _aidl_parcel.writeBoolean(isCrypto);
    _aidl_parcel.writeInt(wakeReason);
    _aidl_parcel.writeInt(displayState);
    _aidl_parcel.writeTypedObject(authenticateReason, _aidl_flag);
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
      id = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      reason = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isAod = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isCrypto = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      wakeReason = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      displayState = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      authenticateReason = _aidl_parcel.readTypedObject(android.hardware.biometrics.common.AuthenticateReason.CREATOR);
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
    _mask |= describeContents(authenticateReason);
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
