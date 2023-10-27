/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.voice;
public class StkCcUnsolSsResult implements android.os.Parcelable
{
  public int serviceType = 0;
  public int requestType = 0;
  public int teleserviceType = 0;
  public int serviceClass = 0;
  public int result;
  public android.hardware.radio.voice.SsInfoData[] ssInfo;
  public android.hardware.radio.voice.CfData[] cfData;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<StkCcUnsolSsResult> CREATOR = new android.os.Parcelable.Creator<StkCcUnsolSsResult>() {
    @Override
    public StkCcUnsolSsResult createFromParcel(android.os.Parcel _aidl_source) {
      StkCcUnsolSsResult _aidl_out = new StkCcUnsolSsResult();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public StkCcUnsolSsResult[] newArray(int _aidl_size) {
      return new StkCcUnsolSsResult[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(serviceType);
    _aidl_parcel.writeInt(requestType);
    _aidl_parcel.writeInt(teleserviceType);
    _aidl_parcel.writeInt(serviceClass);
    _aidl_parcel.writeInt(result);
    _aidl_parcel.writeTypedArray(ssInfo, _aidl_flag);
    _aidl_parcel.writeTypedArray(cfData, _aidl_flag);
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
      requestType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      teleserviceType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      serviceClass = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      result = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      ssInfo = _aidl_parcel.createTypedArray(android.hardware.radio.voice.SsInfoData.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      cfData = _aidl_parcel.createTypedArray(android.hardware.radio.voice.CfData.CREATOR);
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int REQUEST_TYPE_ACTIVATION = 0;
  public static final int REQUEST_TYPE_DEACTIVATION = 1;
  public static final int REQUEST_TYPE_INTERROGATION = 2;
  public static final int REQUEST_TYPE_REGISTRATION = 3;
  public static final int REQUEST_TYPE_ERASURE = 4;
  public static final int SERVICE_TYPE_CFU = 0;
  public static final int SERVICE_TYPE_CF_BUSY = 1;
  public static final int SERVICE_TYPE_CF_NO_REPLY = 2;
  public static final int SERVICE_TYPE_CF_NOT_REACHABLE = 3;
  public static final int SERVICE_TYPE_CF_ALL = 4;
  public static final int SERVICE_TYPE_CF_ALL_CONDITIONAL = 5;
  public static final int SERVICE_TYPE_CLIP = 6;
  public static final int SERVICE_TYPE_CLIR = 7;
  public static final int SERVICE_TYPE_COLP = 8;
  public static final int SERVICE_TYPE_COLR = 9;
  public static final int SERVICE_TYPE_WAIT = 10;
  public static final int SERVICE_TYPE_BAOC = 11;
  public static final int SERVICE_TYPE_BAOIC = 12;
  public static final int SERVICE_TYPE_BAOIC_EXC_HOME = 13;
  public static final int SERVICE_TYPE_BAIC = 14;
  public static final int SERVICE_TYPE_BAIC_ROAMING = 15;
  public static final int SERVICE_TYPE_ALL_BARRING = 16;
  public static final int SERVICE_TYPE_OUTGOING_BARRING = 17;
  public static final int SERVICE_TYPE_INCOMING_BARRING = 18;
  public static final int TELESERVICE_TYPE_ALL_TELE_AND_BEARER_SERVICES = 0;
  public static final int TELESERVICE_TYPE_ALL_TELESEVICES = 1;
  public static final int TELESERVICE_TYPE_TELEPHONY = 2;
  public static final int TELESERVICE_TYPE_ALL_DATA_TELESERVICES = 3;
  public static final int TELESERVICE_TYPE_SMS_SERVICES = 4;
  public static final int TELESERVICE_TYPE_ALL_TELESERVICES_EXCEPT_SMS = 5;
  public static final int SUPP_SERVICE_CLASS_NONE = 0;
  public static final int SUPP_SERVICE_CLASS_VOICE = 1;
  public static final int SUPP_SERVICE_CLASS_DATA = 2;
  public static final int SUPP_SERVICE_CLASS_FAX = 4;
  public static final int SUPP_SERVICE_CLASS_SMS = 8;
  public static final int SUPP_SERVICE_CLASS_DATA_SYNC = 16;
  public static final int SUPP_SERVICE_CLASS_DATA_ASYNC = 32;
  public static final int SUPP_SERVICE_CLASS_PACKET = 64;
  public static final int SUPP_SERVICE_CLASS_PAD = 128;
  public static final int SUPP_SERVICE_CLASS_MAX = 128;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("serviceType: " + (serviceType));
    _aidl_sj.add("requestType: " + (requestType));
    _aidl_sj.add("teleserviceType: " + (teleserviceType));
    _aidl_sj.add("serviceClass: " + (serviceClass));
    _aidl_sj.add("result: " + (android.hardware.radio.RadioError.$.toString(result)));
    _aidl_sj.add("ssInfo: " + (java.util.Arrays.toString(ssInfo)));
    _aidl_sj.add("cfData: " + (java.util.Arrays.toString(cfData)));
    return "android.hardware.radio.voice.StkCcUnsolSsResult" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(ssInfo);
    _mask |= describeContents(cfData);
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
