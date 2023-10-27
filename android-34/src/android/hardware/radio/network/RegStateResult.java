/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public class RegStateResult implements android.os.Parcelable
{
  public int regState;
  public int rat;
  public int reasonForDenial;
  public android.hardware.radio.network.CellIdentity cellIdentity;
  public java.lang.String registeredPlmn;
  public android.hardware.radio.network.AccessTechnologySpecificInfo accessTechnologySpecificInfo;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<RegStateResult> CREATOR = new android.os.Parcelable.Creator<RegStateResult>() {
    @Override
    public RegStateResult createFromParcel(android.os.Parcel _aidl_source) {
      RegStateResult _aidl_out = new RegStateResult();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public RegStateResult[] newArray(int _aidl_size) {
      return new RegStateResult[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(regState);
    _aidl_parcel.writeInt(rat);
    _aidl_parcel.writeInt(reasonForDenial);
    _aidl_parcel.writeTypedObject(cellIdentity, _aidl_flag);
    _aidl_parcel.writeString(registeredPlmn);
    _aidl_parcel.writeTypedObject(accessTechnologySpecificInfo, _aidl_flag);
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
      regState = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      rat = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      reasonForDenial = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      cellIdentity = _aidl_parcel.readTypedObject(android.hardware.radio.network.CellIdentity.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      registeredPlmn = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      accessTechnologySpecificInfo = _aidl_parcel.readTypedObject(android.hardware.radio.network.AccessTechnologySpecificInfo.CREATOR);
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
    _aidl_sj.add("regState: " + (android.hardware.radio.network.RegState.$.toString(regState)));
    _aidl_sj.add("rat: " + (android.hardware.radio.RadioTechnology.$.toString(rat)));
    _aidl_sj.add("reasonForDenial: " + (android.hardware.radio.network.RegistrationFailCause.$.toString(reasonForDenial)));
    _aidl_sj.add("cellIdentity: " + (java.util.Objects.toString(cellIdentity)));
    _aidl_sj.add("registeredPlmn: " + (java.util.Objects.toString(registeredPlmn)));
    _aidl_sj.add("accessTechnologySpecificInfo: " + (java.util.Objects.toString(accessTechnologySpecificInfo)));
    return "android.hardware.radio.network.RegStateResult" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(cellIdentity);
    _mask |= describeContents(accessTechnologySpecificInfo);
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
