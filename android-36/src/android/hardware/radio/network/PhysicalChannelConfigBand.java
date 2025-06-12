/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash 5867b4f5be491ec815fafea8a3f268b0295427df --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V4-java-source/gen/android/hardware/radio/network/PhysicalChannelConfigBand.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/4/android/hardware/radio/network/PhysicalChannelConfigBand.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.network;
/** @hide */
public final class PhysicalChannelConfigBand implements android.os.Parcelable {
  // tags for union fields
  public final static int noinit = 0;  // boolean noinit;
  public final static int geranBand = 1;  // android.hardware.radio.network.GeranBands geranBand;
  public final static int utranBand = 2;  // android.hardware.radio.network.UtranBands utranBand;
  public final static int eutranBand = 3;  // android.hardware.radio.network.EutranBands eutranBand;
  public final static int ngranBand = 4;  // android.hardware.radio.network.NgranBands ngranBand;

  private int _tag;
  private Object _value;

  public PhysicalChannelConfigBand() {
    boolean _value = false;
    this._tag = noinit;
    this._value = _value;
  }

  private PhysicalChannelConfigBand(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private PhysicalChannelConfigBand(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // boolean noinit;

  public static PhysicalChannelConfigBand noinit(boolean _value) {
    return new PhysicalChannelConfigBand(noinit, _value);
  }

  public boolean getNoinit() {
    _assertTag(noinit);
    return (boolean) _value;
  }

  public void setNoinit(boolean _value) {
    _set(noinit, _value);
  }

  // android.hardware.radio.network.GeranBands geranBand;

  public static PhysicalChannelConfigBand geranBand(int _value) {
    return new PhysicalChannelConfigBand(geranBand, _value);
  }

  public int getGeranBand() {
    _assertTag(geranBand);
    return (int) _value;
  }

  public void setGeranBand(int _value) {
    _set(geranBand, _value);
  }

  // android.hardware.radio.network.UtranBands utranBand;

  public static PhysicalChannelConfigBand utranBand(int _value) {
    return new PhysicalChannelConfigBand(utranBand, _value);
  }

  public int getUtranBand() {
    _assertTag(utranBand);
    return (int) _value;
  }

  public void setUtranBand(int _value) {
    _set(utranBand, _value);
  }

  // android.hardware.radio.network.EutranBands eutranBand;

  public static PhysicalChannelConfigBand eutranBand(int _value) {
    return new PhysicalChannelConfigBand(eutranBand, _value);
  }

  public int getEutranBand() {
    _assertTag(eutranBand);
    return (int) _value;
  }

  public void setEutranBand(int _value) {
    _set(eutranBand, _value);
  }

  // android.hardware.radio.network.NgranBands ngranBand;

  public static PhysicalChannelConfigBand ngranBand(int _value) {
    return new PhysicalChannelConfigBand(ngranBand, _value);
  }

  public int getNgranBand() {
    _assertTag(ngranBand);
    return (int) _value;
  }

  public void setNgranBand(int _value) {
    _set(ngranBand, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<PhysicalChannelConfigBand> CREATOR = new android.os.Parcelable.Creator<PhysicalChannelConfigBand>() {
    @Override
    public PhysicalChannelConfigBand createFromParcel(android.os.Parcel _aidl_source) {
      return new PhysicalChannelConfigBand(_aidl_source);
    }
    @Override
    public PhysicalChannelConfigBand[] newArray(int _aidl_size) {
      return new PhysicalChannelConfigBand[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case noinit:
      _aidl_parcel.writeBoolean(getNoinit());
      break;
    case geranBand:
      _aidl_parcel.writeInt(getGeranBand());
      break;
    case utranBand:
      _aidl_parcel.writeInt(getUtranBand());
      break;
    case eutranBand:
      _aidl_parcel.writeInt(getEutranBand());
      break;
    case ngranBand:
      _aidl_parcel.writeInt(getNgranBand());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case noinit: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    case geranBand: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case utranBand: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case eutranBand: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case ngranBand: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    }
    return _mask;
  }

  @Override
  public String toString() {
    switch (_tag) {
    case noinit: return "PhysicalChannelConfigBand.noinit(" + (getNoinit()) + ")";
    case geranBand: return "PhysicalChannelConfigBand.geranBand(" + (android.hardware.radio.network.GeranBands.$.toString(getGeranBand())) + ")";
    case utranBand: return "PhysicalChannelConfigBand.utranBand(" + (android.hardware.radio.network.UtranBands.$.toString(getUtranBand())) + ")";
    case eutranBand: return "PhysicalChannelConfigBand.eutranBand(" + (android.hardware.radio.network.EutranBands.$.toString(getEutranBand())) + ")";
    case ngranBand: return "PhysicalChannelConfigBand.ngranBand(" + (android.hardware.radio.network.NgranBands.$.toString(getNgranBand())) + ")";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }
  private void _assertTag(int tag) {
    if (getTag() != tag) {
      throw new IllegalStateException("bad access: " + _tagString(tag) + ", " + _tagString(getTag()) + " is available.");
    }
  }

  private String _tagString(int _tag) {
    switch (_tag) {
    case noinit: return "noinit";
    case geranBand: return "geranBand";
    case utranBand: return "utranBand";
    case eutranBand: return "eutranBand";
    case ngranBand: return "ngranBand";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int noinit = 0;
    public static final int geranBand = 1;
    public static final int utranBand = 2;
    public static final int eutranBand = 3;
    public static final int ngranBand = 4;
  }
}
