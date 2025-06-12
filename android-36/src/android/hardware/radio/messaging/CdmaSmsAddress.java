/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash b28416394e6595c08e97c0473855eb05eed1baed --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.messaging-V4-java-source/gen/android/hardware/radio/messaging/CdmaSmsAddress.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.messaging-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.messaging/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.messaging/4/android/hardware/radio/messaging/CdmaSmsAddress.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.messaging;
/** @hide */
public class CdmaSmsAddress implements android.os.Parcelable
{
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public int digitMode = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public boolean isNumberModeDataNetwork = false;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public int numberType = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public int numberPlan = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public byte[] digits;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CdmaSmsAddress> CREATOR = new android.os.Parcelable.Creator<CdmaSmsAddress>() {
    @Override
    public CdmaSmsAddress createFromParcel(android.os.Parcel _aidl_source) {
      CdmaSmsAddress _aidl_out = new CdmaSmsAddress();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CdmaSmsAddress[] newArray(int _aidl_size) {
      return new CdmaSmsAddress[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(digitMode);
    _aidl_parcel.writeBoolean(isNumberModeDataNetwork);
    _aidl_parcel.writeInt(numberType);
    _aidl_parcel.writeInt(numberPlan);
    _aidl_parcel.writeByteArray(digits);
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
      digitMode = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isNumberModeDataNetwork = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      numberType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      numberPlan = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      digits = _aidl_parcel.createByteArray();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int DIGIT_MODE_FOUR_BIT = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int DIGIT_MODE_EIGHT_BIT = 1;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_UNKNOWN = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_TELEPHONY = 1;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_RESERVED_2 = 2;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_DATA = 3;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_TELEX = 4;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_RESERVED_5 = 5;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_RESERVED_6 = 6;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_RESERVED_7 = 7;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_RESERVED_8 = 8;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_PRIVATE = 9;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_RESERVED_10 = 10;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_RESERVED_11 = 11;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_RESERVED_12 = 12;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_RESERVED_13 = 13;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_RESERVED_14 = 14;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_PLAN_RESERVED_15 = 15;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_TYPE_UNKNOWN = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_TYPE_INTERNATIONAL_OR_DATA_IP = 1;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_TYPE_NATIONAL_OR_INTERNET_MAIL = 2;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_TYPE_NETWORK = 3;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_TYPE_SUBSCRIBER = 4;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_TYPE_ALPHANUMERIC = 5;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_TYPE_ABBREVIATED = 6;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int NUMBER_TYPE_RESERVED_7 = 7;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("digitMode: " + (digitMode));
    _aidl_sj.add("isNumberModeDataNetwork: " + (isNumberModeDataNetwork));
    _aidl_sj.add("numberType: " + (numberType));
    _aidl_sj.add("numberPlan: " + (numberPlan));
    _aidl_sj.add("digits: " + (java.util.Arrays.toString(digits)));
    return "CdmaSmsAddress" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
