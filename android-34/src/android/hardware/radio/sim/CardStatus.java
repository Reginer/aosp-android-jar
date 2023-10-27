/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.sim;
public class CardStatus implements android.os.Parcelable
{
  public int cardState = 0;
  public int universalPinState;
  public int gsmUmtsSubscriptionAppIndex = 0;
  public int cdmaSubscriptionAppIndex = 0;
  public int imsSubscriptionAppIndex = 0;
  public android.hardware.radio.sim.AppStatus[] applications;
  public java.lang.String atr;
  public java.lang.String iccid;
  public java.lang.String eid;
  public android.hardware.radio.config.SlotPortMapping slotMap;
  public int supportedMepMode = android.hardware.radio.config.MultipleEnabledProfilesMode.NONE;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CardStatus> CREATOR = new android.os.Parcelable.Creator<CardStatus>() {
    @Override
    public CardStatus createFromParcel(android.os.Parcel _aidl_source) {
      CardStatus _aidl_out = new CardStatus();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CardStatus[] newArray(int _aidl_size) {
      return new CardStatus[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(cardState);
    _aidl_parcel.writeInt(universalPinState);
    _aidl_parcel.writeInt(gsmUmtsSubscriptionAppIndex);
    _aidl_parcel.writeInt(cdmaSubscriptionAppIndex);
    _aidl_parcel.writeInt(imsSubscriptionAppIndex);
    _aidl_parcel.writeTypedArray(applications, _aidl_flag);
    _aidl_parcel.writeString(atr);
    _aidl_parcel.writeString(iccid);
    _aidl_parcel.writeString(eid);
    _aidl_parcel.writeTypedObject(slotMap, _aidl_flag);
    _aidl_parcel.writeInt(supportedMepMode);
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
      cardState = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      universalPinState = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      gsmUmtsSubscriptionAppIndex = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      cdmaSubscriptionAppIndex = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      imsSubscriptionAppIndex = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      applications = _aidl_parcel.createTypedArray(android.hardware.radio.sim.AppStatus.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      atr = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      iccid = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      eid = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      slotMap = _aidl_parcel.readTypedObject(android.hardware.radio.config.SlotPortMapping.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      supportedMepMode = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int STATE_ABSENT = 0;
  public static final int STATE_PRESENT = 1;
  public static final int STATE_ERROR = 2;
  public static final int STATE_RESTRICTED = 3;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("cardState: " + (cardState));
    _aidl_sj.add("universalPinState: " + (android.hardware.radio.sim.PinState.$.toString(universalPinState)));
    _aidl_sj.add("gsmUmtsSubscriptionAppIndex: " + (gsmUmtsSubscriptionAppIndex));
    _aidl_sj.add("cdmaSubscriptionAppIndex: " + (cdmaSubscriptionAppIndex));
    _aidl_sj.add("imsSubscriptionAppIndex: " + (imsSubscriptionAppIndex));
    _aidl_sj.add("applications: " + (java.util.Arrays.toString(applications)));
    _aidl_sj.add("atr: " + (java.util.Objects.toString(atr)));
    _aidl_sj.add("iccid: " + (java.util.Objects.toString(iccid)));
    _aidl_sj.add("eid: " + (java.util.Objects.toString(eid)));
    _aidl_sj.add("slotMap: " + (java.util.Objects.toString(slotMap)));
    _aidl_sj.add("supportedMepMode: " + (android.hardware.radio.config.MultipleEnabledProfilesMode.$.toString(supportedMepMode)));
    return "android.hardware.radio.sim.CardStatus" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(applications);
    _mask |= describeContents(slotMap);
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
