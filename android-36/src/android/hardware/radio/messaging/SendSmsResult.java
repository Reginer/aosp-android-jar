/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash b28416394e6595c08e97c0473855eb05eed1baed --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.messaging-V4-java-source/gen/android/hardware/radio/messaging/SendSmsResult.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.messaging-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.messaging/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.messaging/4/android/hardware/radio/messaging/SendSmsResult.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.messaging;
/** @hide */
public class SendSmsResult implements android.os.Parcelable
{
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public int messageRef = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public java.lang.String ackPDU;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public int errorCode = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<SendSmsResult> CREATOR = new android.os.Parcelable.Creator<SendSmsResult>() {
    @Override
    public SendSmsResult createFromParcel(android.os.Parcel _aidl_source) {
      SendSmsResult _aidl_out = new SendSmsResult();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public SendSmsResult[] newArray(int _aidl_size) {
      return new SendSmsResult[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(messageRef);
    _aidl_parcel.writeString(ackPDU);
    _aidl_parcel.writeInt(errorCode);
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
      messageRef = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      ackPDU = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      errorCode = _aidl_parcel.readInt();
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
    _aidl_sj.add("messageRef: " + (messageRef));
    _aidl_sj.add("ackPDU: " + (java.util.Objects.toString(ackPDU)));
    _aidl_sj.add("errorCode: " + (errorCode));
    return "SendSmsResult" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
