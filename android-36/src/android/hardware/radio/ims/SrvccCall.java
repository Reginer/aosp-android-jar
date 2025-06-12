/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash b2a615a151c7114c4216b1987fd32d40c797d00a --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.ims-V3-java-source/gen/android/hardware/radio/ims/SrvccCall.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.ims-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.ims/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.ims/3/android/hardware/radio/ims/SrvccCall.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.ims;
/** @hide */
public class SrvccCall implements android.os.Parcelable
{
  public int index = 0;
  public int callType = android.hardware.radio.ims.SrvccCall.CallType.NORMAL;
  public int callState = 0;
  public int callSubstate = android.hardware.radio.ims.SrvccCall.CallSubState.NONE;
  public int ringbackToneType = android.hardware.radio.ims.SrvccCall.ToneType.NONE;
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
    return "SrvccCall" + _aidl_sj.toString()  ;
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
