/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.messaging;
public class CdmaSmsAddress implements android.os.Parcelable
{
  public int digitMode = 0;
  public boolean isNumberModeDataNetwork = false;
  public int numberType = 0;
  public int numberPlan = 0;
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
  public static final int DIGIT_MODE_FOUR_BIT = 0;
  public static final int DIGIT_MODE_EIGHT_BIT = 1;
  public static final int NUMBER_PLAN_UNKNOWN = 0;
  public static final int NUMBER_PLAN_TELEPHONY = 1;
  public static final int NUMBER_PLAN_RESERVED_2 = 2;
  public static final int NUMBER_PLAN_DATA = 3;
  public static final int NUMBER_PLAN_TELEX = 4;
  public static final int NUMBER_PLAN_RESERVED_5 = 5;
  public static final int NUMBER_PLAN_RESERVED_6 = 6;
  public static final int NUMBER_PLAN_RESERVED_7 = 7;
  public static final int NUMBER_PLAN_RESERVED_8 = 8;
  public static final int NUMBER_PLAN_PRIVATE = 9;
  public static final int NUMBER_PLAN_RESERVED_10 = 10;
  public static final int NUMBER_PLAN_RESERVED_11 = 11;
  public static final int NUMBER_PLAN_RESERVED_12 = 12;
  public static final int NUMBER_PLAN_RESERVED_13 = 13;
  public static final int NUMBER_PLAN_RESERVED_14 = 14;
  public static final int NUMBER_PLAN_RESERVED_15 = 15;
  public static final int NUMBER_TYPE_UNKNOWN = 0;
  public static final int NUMBER_TYPE_INTERNATIONAL_OR_DATA_IP = 1;
  public static final int NUMBER_TYPE_NATIONAL_OR_INTERNET_MAIL = 2;
  public static final int NUMBER_TYPE_NETWORK = 3;
  public static final int NUMBER_TYPE_SUBSCRIBER = 4;
  public static final int NUMBER_TYPE_ALPHANUMERIC = 5;
  public static final int NUMBER_TYPE_ABBREVIATED = 6;
  public static final int NUMBER_TYPE_RESERVED_7 = 7;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("digitMode: " + (digitMode));
    _aidl_sj.add("isNumberModeDataNetwork: " + (isNumberModeDataNetwork));
    _aidl_sj.add("numberType: " + (numberType));
    _aidl_sj.add("numberPlan: " + (numberPlan));
    _aidl_sj.add("digits: " + (java.util.Arrays.toString(digits)));
    return "android.hardware.radio.messaging.CdmaSmsAddress" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
