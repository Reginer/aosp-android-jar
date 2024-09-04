/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 30b0bc0e84679bc3b5ccb3a52da34c47cda6b7eb --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.messaging-V3-java-source/gen/android/hardware/radio/messaging/CdmaSmsMessage.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.messaging-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.messaging/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.messaging/3/android/hardware/radio/messaging/CdmaSmsMessage.aidl
 */
package android.hardware.radio.messaging;
/** @hide */
public class CdmaSmsMessage implements android.os.Parcelable
{
  public int teleserviceId = 0;
  public boolean isServicePresent = false;
  public int serviceCategory = 0;
  public android.hardware.radio.messaging.CdmaSmsAddress address;
  public android.hardware.radio.messaging.CdmaSmsSubaddress subAddress;
  public byte[] bearerData;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CdmaSmsMessage> CREATOR = new android.os.Parcelable.Creator<CdmaSmsMessage>() {
    @Override
    public CdmaSmsMessage createFromParcel(android.os.Parcel _aidl_source) {
      CdmaSmsMessage _aidl_out = new CdmaSmsMessage();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CdmaSmsMessage[] newArray(int _aidl_size) {
      return new CdmaSmsMessage[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(teleserviceId);
    _aidl_parcel.writeBoolean(isServicePresent);
    _aidl_parcel.writeInt(serviceCategory);
    _aidl_parcel.writeTypedObject(address, _aidl_flag);
    _aidl_parcel.writeTypedObject(subAddress, _aidl_flag);
    _aidl_parcel.writeByteArray(bearerData);
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
      teleserviceId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isServicePresent = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      serviceCategory = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      address = _aidl_parcel.readTypedObject(android.hardware.radio.messaging.CdmaSmsAddress.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      subAddress = _aidl_parcel.readTypedObject(android.hardware.radio.messaging.CdmaSmsSubaddress.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      bearerData = _aidl_parcel.createByteArray();
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
    _aidl_sj.add("teleserviceId: " + (teleserviceId));
    _aidl_sj.add("isServicePresent: " + (isServicePresent));
    _aidl_sj.add("serviceCategory: " + (serviceCategory));
    _aidl_sj.add("address: " + (java.util.Objects.toString(address)));
    _aidl_sj.add("subAddress: " + (java.util.Objects.toString(subAddress)));
    _aidl_sj.add("bearerData: " + (java.util.Arrays.toString(bearerData)));
    return "CdmaSmsMessage" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(address);
    _mask |= describeContents(subAddress);
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
