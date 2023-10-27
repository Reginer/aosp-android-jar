/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.modem;
public class HardwareConfig implements android.os.Parcelable
{
  public int type = 0;
  public java.lang.String uuid;
  public int state = 0;
  public android.hardware.radio.modem.HardwareConfigModem[] modem;
  public android.hardware.radio.modem.HardwareConfigSim[] sim;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<HardwareConfig> CREATOR = new android.os.Parcelable.Creator<HardwareConfig>() {
    @Override
    public HardwareConfig createFromParcel(android.os.Parcel _aidl_source) {
      HardwareConfig _aidl_out = new HardwareConfig();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public HardwareConfig[] newArray(int _aidl_size) {
      return new HardwareConfig[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(type);
    _aidl_parcel.writeString(uuid);
    _aidl_parcel.writeInt(state);
    _aidl_parcel.writeTypedArray(modem, _aidl_flag);
    _aidl_parcel.writeTypedArray(sim, _aidl_flag);
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
      type = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      uuid = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      state = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      modem = _aidl_parcel.createTypedArray(android.hardware.radio.modem.HardwareConfigModem.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sim = _aidl_parcel.createTypedArray(android.hardware.radio.modem.HardwareConfigSim.CREATOR);
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int STATE_ENABLED = 0;
  public static final int STATE_STANDBY = 1;
  public static final int STATE_DISABLED = 2;
  public static final int TYPE_MODEM = 0;
  public static final int TYPE_SIM = 1;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("type: " + (type));
    _aidl_sj.add("uuid: " + (java.util.Objects.toString(uuid)));
    _aidl_sj.add("state: " + (state));
    _aidl_sj.add("modem: " + (java.util.Arrays.toString(modem)));
    _aidl_sj.add("sim: " + (java.util.Arrays.toString(sim)));
    return "android.hardware.radio.modem.HardwareConfig" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(modem);
    _mask |= describeContents(sim);
    return _mask;
  }
  private int describeContents(Object _v) {
    if (_v == null) return 0;
    if (_v instanceof Object[]) {
      int _mask = 0;
      for (Object o : (Object[]) _v) {
        _mask |= describeContents(o);
      }
      return _mask;
    }
    if (_v instanceof android.os.Parcelable) {
      return ((android.os.Parcelable) _v).describeContents();
    }
    return 0;
  }
}
