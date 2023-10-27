/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.sim;
public class AppStatus implements android.os.Parcelable
{
  public int appType = 0;
  public int appState = 0;
  public int persoSubstate;
  public java.lang.String aidPtr;
  public java.lang.String appLabelPtr;
  public boolean pin1Replaced = false;
  public int pin1;
  public int pin2;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AppStatus> CREATOR = new android.os.Parcelable.Creator<AppStatus>() {
    @Override
    public AppStatus createFromParcel(android.os.Parcel _aidl_source) {
      AppStatus _aidl_out = new AppStatus();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AppStatus[] newArray(int _aidl_size) {
      return new AppStatus[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(appType);
    _aidl_parcel.writeInt(appState);
    _aidl_parcel.writeInt(persoSubstate);
    _aidl_parcel.writeString(aidPtr);
    _aidl_parcel.writeString(appLabelPtr);
    _aidl_parcel.writeBoolean(pin1Replaced);
    _aidl_parcel.writeInt(pin1);
    _aidl_parcel.writeInt(pin2);
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
      appType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      appState = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      persoSubstate = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      aidPtr = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      appLabelPtr = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      pin1Replaced = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      pin1 = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      pin2 = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int APP_STATE_UNKNOWN = 0;
  public static final int APP_STATE_DETECTED = 1;
  public static final int APP_STATE_PIN = 2;
  public static final int APP_STATE_PUK = 3;
  public static final int APP_STATE_SUBSCRIPTION_PERSO = 4;
  public static final int APP_STATE_READY = 5;
  public static final int APP_TYPE_UNKNOWN = 0;
  public static final int APP_TYPE_SIM = 1;
  public static final int APP_TYPE_USIM = 2;
  public static final int APP_TYPE_RUIM = 3;
  public static final int APP_TYPE_CSIM = 4;
  public static final int APP_TYPE_ISIM = 5;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("appType: " + (appType));
    _aidl_sj.add("appState: " + (appState));
    _aidl_sj.add("persoSubstate: " + (android.hardware.radio.sim.PersoSubstate.$.toString(persoSubstate)));
    _aidl_sj.add("aidPtr: " + (java.util.Objects.toString(aidPtr)));
    _aidl_sj.add("appLabelPtr: " + (java.util.Objects.toString(appLabelPtr)));
    _aidl_sj.add("pin1Replaced: " + (pin1Replaced));
    _aidl_sj.add("pin1: " + (android.hardware.radio.sim.PinState.$.toString(pin1)));
    _aidl_sj.add("pin2: " + (android.hardware.radio.sim.PinState.$.toString(pin2)));
    return "android.hardware.radio.sim.AppStatus" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
