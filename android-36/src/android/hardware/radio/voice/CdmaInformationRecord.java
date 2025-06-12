/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash 576f05d082e9269bcf773b0c9b9112d507ab4b9a --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.voice-V4-java-source/gen/android/hardware/radio/voice/CdmaInformationRecord.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.voice-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.voice/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.voice/4/android/hardware/radio/voice/CdmaInformationRecord.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.voice;
/** @hide */
public class CdmaInformationRecord implements android.os.Parcelable
{
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public int name = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public android.hardware.radio.voice.CdmaDisplayInfoRecord[] display;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public android.hardware.radio.voice.CdmaNumberInfoRecord[] number;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public android.hardware.radio.voice.CdmaSignalInfoRecord[] signal;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public android.hardware.radio.voice.CdmaRedirectingNumberInfoRecord[] redir;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public android.hardware.radio.voice.CdmaLineControlInfoRecord[] lineCtrl;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public android.hardware.radio.voice.CdmaT53ClirInfoRecord[] clir;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public android.hardware.radio.voice.CdmaT53AudioControlInfoRecord[] audioCtrl;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CdmaInformationRecord> CREATOR = new android.os.Parcelable.Creator<CdmaInformationRecord>() {
    @Override
    public CdmaInformationRecord createFromParcel(android.os.Parcel _aidl_source) {
      CdmaInformationRecord _aidl_out = new CdmaInformationRecord();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CdmaInformationRecord[] newArray(int _aidl_size) {
      return new CdmaInformationRecord[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(name);
    _aidl_parcel.writeTypedArray(display, _aidl_flag);
    _aidl_parcel.writeTypedArray(number, _aidl_flag);
    _aidl_parcel.writeTypedArray(signal, _aidl_flag);
    _aidl_parcel.writeTypedArray(redir, _aidl_flag);
    _aidl_parcel.writeTypedArray(lineCtrl, _aidl_flag);
    _aidl_parcel.writeTypedArray(clir, _aidl_flag);
    _aidl_parcel.writeTypedArray(audioCtrl, _aidl_flag);
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
      name = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      display = _aidl_parcel.createTypedArray(android.hardware.radio.voice.CdmaDisplayInfoRecord.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      number = _aidl_parcel.createTypedArray(android.hardware.radio.voice.CdmaNumberInfoRecord.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      signal = _aidl_parcel.createTypedArray(android.hardware.radio.voice.CdmaSignalInfoRecord.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      redir = _aidl_parcel.createTypedArray(android.hardware.radio.voice.CdmaRedirectingNumberInfoRecord.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      lineCtrl = _aidl_parcel.createTypedArray(android.hardware.radio.voice.CdmaLineControlInfoRecord.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      clir = _aidl_parcel.createTypedArray(android.hardware.radio.voice.CdmaT53ClirInfoRecord.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      audioCtrl = _aidl_parcel.createTypedArray(android.hardware.radio.voice.CdmaT53AudioControlInfoRecord.CREATOR);
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int CDMA_MAX_NUMBER_OF_INFO_RECS = 10;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NAME_DISPLAY = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NAME_CALLED_PARTY_NUMBER = 1;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NAME_CALLING_PARTY_NUMBER = 2;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NAME_CONNECTED_NUMBER = 3;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NAME_SIGNAL = 4;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NAME_REDIRECTING_NUMBER = 5;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NAME_LINE_CONTROL = 6;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NAME_EXTENDED_DISPLAY = 7;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NAME_T53_CLIR = 8;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NAME_T53_RELEASE = 9;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NAME_T53_AUDIO_CONTROL = 10;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("name: " + (name));
    _aidl_sj.add("display: " + (java.util.Arrays.toString(display)));
    _aidl_sj.add("number: " + (java.util.Arrays.toString(number)));
    _aidl_sj.add("signal: " + (java.util.Arrays.toString(signal)));
    _aidl_sj.add("redir: " + (java.util.Arrays.toString(redir)));
    _aidl_sj.add("lineCtrl: " + (java.util.Arrays.toString(lineCtrl)));
    _aidl_sj.add("clir: " + (java.util.Arrays.toString(clir)));
    _aidl_sj.add("audioCtrl: " + (java.util.Arrays.toString(audioCtrl)));
    return "CdmaInformationRecord" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(display);
    _mask |= describeContents(number);
    _mask |= describeContents(signal);
    _mask |= describeContents(redir);
    _mask |= describeContents(lineCtrl);
    _mask |= describeContents(clir);
    _mask |= describeContents(audioCtrl);
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
