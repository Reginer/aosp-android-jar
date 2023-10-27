/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public class BarringInfo implements android.os.Parcelable
{
  public int serviceType = 0;
  public int barringType = 0;
  public android.hardware.radio.network.BarringTypeSpecificInfo barringTypeSpecificInfo;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<BarringInfo> CREATOR = new android.os.Parcelable.Creator<BarringInfo>() {
    @Override
    public BarringInfo createFromParcel(android.os.Parcel _aidl_source) {
      BarringInfo _aidl_out = new BarringInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public BarringInfo[] newArray(int _aidl_size) {
      return new BarringInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(serviceType);
    _aidl_parcel.writeInt(barringType);
    _aidl_parcel.writeTypedObject(barringTypeSpecificInfo, _aidl_flag);
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
      serviceType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      barringType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      barringTypeSpecificInfo = _aidl_parcel.readTypedObject(android.hardware.radio.network.BarringTypeSpecificInfo.CREATOR);
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int BARRING_TYPE_NONE = 0;
  public static final int BARRING_TYPE_CONDITIONAL = 1;
  public static final int BARRING_TYPE_UNCONDITIONAL = 2;
  public static final int SERVICE_TYPE_CS_SERVICE = 0;
  public static final int SERVICE_TYPE_PS_SERVICE = 1;
  public static final int SERVICE_TYPE_CS_VOICE = 2;
  public static final int SERVICE_TYPE_MO_SIGNALLING = 3;
  public static final int SERVICE_TYPE_MO_DATA = 4;
  public static final int SERVICE_TYPE_CS_FALLBACK = 5;
  public static final int SERVICE_TYPE_MMTEL_VOICE = 6;
  public static final int SERVICE_TYPE_MMTEL_VIDEO = 7;
  public static final int SERVICE_TYPE_EMERGENCY = 8;
  public static final int SERVICE_TYPE_SMS = 9;
  public static final int SERVICE_TYPE_OPERATOR_1 = 1001;
  public static final int SERVICE_TYPE_OPERATOR_2 = 1002;
  public static final int SERVICE_TYPE_OPERATOR_3 = 1003;
  public static final int SERVICE_TYPE_OPERATOR_4 = 1004;
  public static final int SERVICE_TYPE_OPERATOR_5 = 1005;
  public static final int SERVICE_TYPE_OPERATOR_6 = 1006;
  public static final int SERVICE_TYPE_OPERATOR_7 = 1007;
  public static final int SERVICE_TYPE_OPERATOR_8 = 1008;
  public static final int SERVICE_TYPE_OPERATOR_9 = 1009;
  public static final int SERVICE_TYPE_OPERATOR_10 = 1010;
  public static final int SERVICE_TYPE_OPERATOR_11 = 1011;
  public static final int SERVICE_TYPE_OPERATOR_12 = 1012;
  public static final int SERVICE_TYPE_OPERATOR_13 = 1013;
  public static final int SERVICE_TYPE_OPERATOR_14 = 1014;
  public static final int SERVICE_TYPE_OPERATOR_15 = 1015;
  public static final int SERVICE_TYPE_OPERATOR_16 = 1016;
  public static final int SERVICE_TYPE_OPERATOR_17 = 1017;
  public static final int SERVICE_TYPE_OPERATOR_18 = 1018;
  public static final int SERVICE_TYPE_OPERATOR_19 = 1019;
  public static final int SERVICE_TYPE_OPERATOR_20 = 1020;
  public static final int SERVICE_TYPE_OPERATOR_21 = 1021;
  public static final int SERVICE_TYPE_OPERATOR_22 = 1022;
  public static final int SERVICE_TYPE_OPERATOR_23 = 1023;
  public static final int SERVICE_TYPE_OPERATOR_24 = 1024;
  public static final int SERVICE_TYPE_OPERATOR_25 = 1025;
  public static final int SERVICE_TYPE_OPERATOR_26 = 1026;
  public static final int SERVICE_TYPE_OPERATOR_27 = 1027;
  public static final int SERVICE_TYPE_OPERATOR_28 = 1028;
  public static final int SERVICE_TYPE_OPERATOR_29 = 1029;
  public static final int SERVICE_TYPE_OPERATOR_30 = 1030;
  public static final int SERVICE_TYPE_OPERATOR_31 = 1031;
  public static final int SERVICE_TYPE_OPERATOR_32 = 1032;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("serviceType: " + (serviceType));
    _aidl_sj.add("barringType: " + (barringType));
    _aidl_sj.add("barringTypeSpecificInfo: " + (java.util.Objects.toString(barringTypeSpecificInfo)));
    return "android.hardware.radio.network.BarringInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(barringTypeSpecificInfo);
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
