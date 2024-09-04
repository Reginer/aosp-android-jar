/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 78fb79bcb32590a868b3eb7affb39ab90e4ca782 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.voice-V3-java-source/gen/android/hardware/radio/voice/CdmaCallWaiting.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.voice-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.voice/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.voice/3/android/hardware/radio/voice/CdmaCallWaiting.aidl
 */
package android.hardware.radio.voice;
/** @hide */
public class CdmaCallWaiting implements android.os.Parcelable
{
  public java.lang.String number;
  public int numberPresentation = 0;
  public java.lang.String name;
  public android.hardware.radio.voice.CdmaSignalInfoRecord signalInfoRecord;
  public int numberType = 0;
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
  public static final int NUMBER_PLAN_UNKNOWN = 0;
  public static final int NUMBER_PLAN_ISDN = 1;
  public static final int NUMBER_PLAN_DATA = 3;
  public static final int NUMBER_PLAN_TELEX = 4;
  public static final int NUMBER_PLAN_NATIONAL = 8;
  public static final int NUMBER_PLAN_PRIVATE = 9;
  public static final int NUMBER_PRESENTATION_ALLOWED = 0;
  public static final int NUMBER_PRESENTATION_RESTRICTED = 1;
  public static final int NUMBER_PRESENTATION_UNKNOWN = 2;
  public static final int NUMBER_TYPE_UNKNOWN = 0;
  public static final int NUMBER_TYPE_INTERNATIONAL = 1;
  public static final int NUMBER_TYPE_NATIONAL = 2;
  public static final int NUMBER_TYPE_NETWORK_SPECIFIC = 3;
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
