/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public class EmergencyRegResult implements android.os.Parcelable
{
  public int accessNetwork;
  public int regState;
  public int emcDomain;
  public boolean isVopsSupported = false;
  public boolean isEmcBearerSupported = false;
  public byte nwProvidedEmc = 0;
  public byte nwProvidedEmf = 0;
  public java.lang.String mcc = "";
  public java.lang.String mnc = "";
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<EmergencyRegResult> CREATOR = new android.os.Parcelable.Creator<EmergencyRegResult>() {
    @Override
    public EmergencyRegResult createFromParcel(android.os.Parcel _aidl_source) {
      EmergencyRegResult _aidl_out = new EmergencyRegResult();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public EmergencyRegResult[] newArray(int _aidl_size) {
      return new EmergencyRegResult[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(accessNetwork);
    _aidl_parcel.writeInt(regState);
    _aidl_parcel.writeInt(emcDomain);
    _aidl_parcel.writeBoolean(isVopsSupported);
    _aidl_parcel.writeBoolean(isEmcBearerSupported);
    _aidl_parcel.writeByte(nwProvidedEmc);
    _aidl_parcel.writeByte(nwProvidedEmf);
    _aidl_parcel.writeString(mcc);
    _aidl_parcel.writeString(mnc);
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
      accessNetwork = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      regState = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      emcDomain = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isVopsSupported = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isEmcBearerSupported = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      nwProvidedEmc = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      nwProvidedEmf = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mcc = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mnc = _aidl_parcel.readString();
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
    _aidl_sj.add("accessNetwork: " + (android.hardware.radio.AccessNetwork.$.toString(accessNetwork)));
    _aidl_sj.add("regState: " + (android.hardware.radio.network.RegState.$.toString(regState)));
    _aidl_sj.add("emcDomain: " + (android.hardware.radio.network.Domain.$.toString(emcDomain)));
    _aidl_sj.add("isVopsSupported: " + (isVopsSupported));
    _aidl_sj.add("isEmcBearerSupported: " + (isEmcBearerSupported));
    _aidl_sj.add("nwProvidedEmc: " + (nwProvidedEmc));
    _aidl_sj.add("nwProvidedEmf: " + (nwProvidedEmf));
    _aidl_sj.add("mcc: " + (java.util.Objects.toString(mcc)));
    _aidl_sj.add("mnc: " + (java.util.Objects.toString(mnc)));
    return "android.hardware.radio.network.EmergencyRegResult" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
