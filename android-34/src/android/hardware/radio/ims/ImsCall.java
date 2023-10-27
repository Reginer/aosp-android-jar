/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.ims;
public class ImsCall implements android.os.Parcelable
{
  public int index = 0;
  public int callType;
  public int accessNetwork;
  public int callState;
  public int direction;
  public boolean isHeldByRemote = false;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<ImsCall> CREATOR = new android.os.Parcelable.Creator<ImsCall>() {
    @Override
    public ImsCall createFromParcel(android.os.Parcel _aidl_source) {
      ImsCall _aidl_out = new ImsCall();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ImsCall[] newArray(int _aidl_size) {
      return new ImsCall[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(index);
    _aidl_parcel.writeInt(callType);
    _aidl_parcel.writeInt(accessNetwork);
    _aidl_parcel.writeInt(callState);
    _aidl_parcel.writeInt(direction);
    _aidl_parcel.writeBoolean(isHeldByRemote);
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
      index = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      callType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      accessNetwork = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      callState = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      direction = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isHeldByRemote = _aidl_parcel.readBoolean();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("index: " + (index));
    _aidl_sj.add("callType: " + (callType));
    _aidl_sj.add("accessNetwork: " + (android.hardware.radio.AccessNetwork.$.toString(accessNetwork)));
    _aidl_sj.add("callState: " + (callState));
    _aidl_sj.add("direction: " + (direction));
    _aidl_sj.add("isHeldByRemote: " + (isHeldByRemote));
    return "android.hardware.radio.ims.ImsCall" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
  public static @interface CallType {
    public static final int NORMAL = 0;
    public static final int EMERGENCY = 1;
  }
  public static @interface CallState {
    public static final int ACTIVE = 0;
    public static final int HOLDING = 1;
    public static final int DIALING = 2;
    public static final int ALERTING = 3;
    public static final int INCOMING = 4;
    public static final int WAITING = 5;
    public static final int DISCONNECTING = 6;
    public static final int DISCONNECTED = 7;
  }
  public static @interface Direction {
    public static final int INCOMING = 0;
    public static final int OUTGOING = 1;
  }
}
