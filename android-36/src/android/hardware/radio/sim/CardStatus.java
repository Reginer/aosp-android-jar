/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash fc1a19a4f86a58981158cc8d956763c9d8ace630 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.config_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.sim-V4-java-source/gen/android/hardware/radio/sim/CardStatus.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.sim-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.sim/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.sim/4/android/hardware/radio/sim/CardStatus.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.sim;
/** @hide */
public class CardStatus implements android.os.Parcelable
{
  public int cardState = 0;
  public int universalPinState = android.hardware.radio.sim.PinState.UNKNOWN;
  public int gsmUmtsSubscriptionAppIndex = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
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
    return "CardStatus" + _aidl_sj.toString()  ;
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
