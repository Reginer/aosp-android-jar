/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash 576f05d082e9269bcf773b0c9b9112d507ab4b9a --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.voice-V4-java-source/gen/android/hardware/radio/voice/CdmaCallWaiting.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.voice-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.voice/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.voice/4/android/hardware/radio/voice/CdmaCallWaiting.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.voice;
/** @hide */
public class CdmaCallWaiting implements android.os.Parcelable
{
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public java.lang.String number;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public int numberPresentation = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public java.lang.String name;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public android.hardware.radio.voice.CdmaSignalInfoRecord signalInfoRecord;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public int numberType = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public int numberPlan = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CdmaCallWaiting> CREATOR = new android.os.Parcelable.Creator<CdmaCallWaiting>() {
    @Override
    public CdmaCallWaiting createFromParcel(android.os.Parcel _aidl_source) {
      CdmaCallWaiting _aidl_out = new CdmaCallWaiting();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CdmaCallWaiting[] newArray(int _aidl_size) {
      return new CdmaCallWaiting[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(number);
    _aidl_parcel.writeInt(numberPresentation);
    _aidl_parcel.writeString(name);
    _aidl_parcel.writeTypedObject(signalInfoRecord, _aidl_flag);
    _aidl_parcel.writeInt(numberType);
    _aidl_parcel.writeInt(numberPlan);
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
      number = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      numberPresentation = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      name = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      signalInfoRecord = _aidl_parcel.readTypedObject(android.hardware.radio.voice.CdmaSignalInfoRecord.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      numberType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      numberPlan = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_UNKNOWN = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_ISDN = 1;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_DATA = 3;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_TELEX = 4;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_NATIONAL = 8;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_PRIVATE = 9;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PRESENTATION_ALLOWED = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PRESENTATION_RESTRICTED = 1;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PRESENTATION_UNKNOWN = 2;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_TYPE_UNKNOWN = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_TYPE_INTERNATIONAL = 1;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_TYPE_NATIONAL = 2;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_TYPE_NETWORK_SPECIFIC = 3;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_TYPE_SUBSCRIBER = 4;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("number: " + (java.util.Objects.toString(number)));
    _aidl_sj.add("numberPresentation: " + (numberPresentation));
    _aidl_sj.add("name: " + (java.util.Objects.toString(name)));
    _aidl_sj.add("signalInfoRecord: " + (java.util.Objects.toString(signalInfoRecord)));
    _aidl_sj.add("numberType: " + (numberType));
    _aidl_sj.add("numberPlan: " + (numberPlan));
    return "CdmaCallWaiting" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(signalInfoRecord);
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
