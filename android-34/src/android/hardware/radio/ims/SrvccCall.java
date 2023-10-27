/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.ims;
public class SrvccCall implements android.os.Parcelable
{
  public int index = 0;
  public int callType;
  public int callState = 0;
  public int callSubstate;
  public int ringbackToneType;
  public boolean isMpty = false;
  public boolean isMT = false;
  public java.lang.String number;
  public int numPresentation = 0;
  public java.lang.String name;
  public int namePresentation = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<SrvccCall> CREATOR = new android.os.Parcelable.Creator<SrvccCall>() {
    @Override
    public SrvccCall createFromParcel(android.os.Parcel _aidl_source) {
      SrvccCall _aidl_out = new SrvccCall();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public SrvccCall[] newArray(int _aidl_size) {
      return new SrvccCall[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(index);
    _aidl_parcel.writeInt(callType);
    _aidl_parcel.writeInt(callState);
    _aidl_parcel.writeInt(callSubstate);
    _aidl_parcel.writeInt(ringbackToneType);
    _aidl_parcel.writeBoolean(isMpty);
    _aidl_parcel.writeBoolean(isMT);
    _aidl_parcel.writeString(number);
    _aidl_parcel.writeInt(numPresentation);
    _aidl_parcel.writeString(name);
    _aidl_parcel.writeInt(namePresentation);
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
      callState = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      callSubstate = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      ringbackToneType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isMpty = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isMT = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      number = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      numPresentation = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      name = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      namePresentation = _aidl_parcel.readInt();
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
    _aidl_sj.add("callState: " + (callState));
    _aidl_sj.add("callSubstate: " + (callSubstate));
    _aidl_sj.add("ringbackToneType: " + (ringbackToneType));
    _aidl_sj.add("isMpty: " + (isMpty));
    _aidl_sj.add("isMT: " + (isMT));
    _aidl_sj.add("number: " + (java.util.Objects.toString(number)));
    _aidl_sj.add("numPresentation: " + (numPresentation));
    _aidl_sj.add("name: " + (java.util.Objects.toString(name)));
    _aidl_sj.add("namePresentation: " + (namePresentation));
    return "android.hardware.radio.ims.SrvccCall" + _aidl_sj.toString()  ;
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
  public static @interface CallSubState {
    public static final int NONE = 0;
    public static final int PREALERTING = 1;
  }
  public static @interface ToneType {
    public static final int NONE = 0;
    public static final int LOCAL = 1;
    public static final int NETWORK = 2;
  }
}
