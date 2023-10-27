/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.data;
public class SetupDataCallResult implements android.os.Parcelable
{
  public int cause;
  public long suggestedRetryTime = 0L;
  public int cid = 0;
  public int active = 0;
  public int type;
  public java.lang.String ifname;
  public android.hardware.radio.data.LinkAddress[] addresses;
  public java.lang.String[] dnses;
  public java.lang.String[] gateways;
  public java.lang.String[] pcscf;
  public int mtuV4 = 0;
  public int mtuV6 = 0;
  public android.hardware.radio.data.Qos defaultQos;
  public android.hardware.radio.data.QosSession[] qosSessions;
  public byte handoverFailureMode = 0;
  public int pduSessionId = 0;
  public android.hardware.radio.data.SliceInfo sliceInfo;
  public android.hardware.radio.data.TrafficDescriptor[] trafficDescriptors;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<SetupDataCallResult> CREATOR = new android.os.Parcelable.Creator<SetupDataCallResult>() {
    @Override
    public SetupDataCallResult createFromParcel(android.os.Parcel _aidl_source) {
      SetupDataCallResult _aidl_out = new SetupDataCallResult();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public SetupDataCallResult[] newArray(int _aidl_size) {
      return new SetupDataCallResult[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(cause);
    _aidl_parcel.writeLong(suggestedRetryTime);
    _aidl_parcel.writeInt(cid);
    _aidl_parcel.writeInt(active);
    _aidl_parcel.writeInt(type);
    _aidl_parcel.writeString(ifname);
    _aidl_parcel.writeTypedArray(addresses, _aidl_flag);
    _aidl_parcel.writeStringArray(dnses);
    _aidl_parcel.writeStringArray(gateways);
    _aidl_parcel.writeStringArray(pcscf);
    _aidl_parcel.writeInt(mtuV4);
    _aidl_parcel.writeInt(mtuV6);
    _aidl_parcel.writeTypedObject(defaultQos, _aidl_flag);
    _aidl_parcel.writeTypedArray(qosSessions, _aidl_flag);
    _aidl_parcel.writeByte(handoverFailureMode);
    _aidl_parcel.writeInt(pduSessionId);
    _aidl_parcel.writeTypedObject(sliceInfo, _aidl_flag);
    _aidl_parcel.writeTypedArray(trafficDescriptors, _aidl_flag);
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
      cause = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      suggestedRetryTime = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      cid = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      active = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      type = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      ifname = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      addresses = _aidl_parcel.createTypedArray(android.hardware.radio.data.LinkAddress.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      dnses = _aidl_parcel.createStringArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      gateways = _aidl_parcel.createStringArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      pcscf = _aidl_parcel.createStringArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mtuV4 = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mtuV6 = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      defaultQos = _aidl_parcel.readTypedObject(android.hardware.radio.data.Qos.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      qosSessions = _aidl_parcel.createTypedArray(android.hardware.radio.data.QosSession.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      handoverFailureMode = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      pduSessionId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sliceInfo = _aidl_parcel.readTypedObject(android.hardware.radio.data.SliceInfo.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      trafficDescriptors = _aidl_parcel.createTypedArray(android.hardware.radio.data.TrafficDescriptor.CREATOR);
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int DATA_CONNECTION_STATUS_INACTIVE = 0;
  public static final int DATA_CONNECTION_STATUS_DORMANT = 1;
  public static final int DATA_CONNECTION_STATUS_ACTIVE = 2;
  public static final byte HANDOVER_FAILURE_MODE_LEGACY = 0;
  public static final byte HANDOVER_FAILURE_MODE_DO_FALLBACK = 1;
  public static final byte HANDOVER_FAILURE_MODE_NO_FALLBACK_RETRY_HANDOVER = 2;
  public static final byte HANDOVER_FAILURE_MODE_NO_FALLBACK_RETRY_SETUP_NORMAL = 3;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("cause: " + (android.hardware.radio.data.DataCallFailCause.$.toString(cause)));
    _aidl_sj.add("suggestedRetryTime: " + (suggestedRetryTime));
    _aidl_sj.add("cid: " + (cid));
    _aidl_sj.add("active: " + (active));
    _aidl_sj.add("type: " + (android.hardware.radio.data.PdpProtocolType.$.toString(type)));
    _aidl_sj.add("ifname: " + (java.util.Objects.toString(ifname)));
    _aidl_sj.add("addresses: " + (java.util.Arrays.toString(addresses)));
    _aidl_sj.add("dnses: " + (java.util.Arrays.toString(dnses)));
    _aidl_sj.add("gateways: " + (java.util.Arrays.toString(gateways)));
    _aidl_sj.add("pcscf: " + (java.util.Arrays.toString(pcscf)));
    _aidl_sj.add("mtuV4: " + (mtuV4));
    _aidl_sj.add("mtuV6: " + (mtuV6));
    _aidl_sj.add("defaultQos: " + (java.util.Objects.toString(defaultQos)));
    _aidl_sj.add("qosSessions: " + (java.util.Arrays.toString(qosSessions)));
    _aidl_sj.add("handoverFailureMode: " + (handoverFailureMode));
    _aidl_sj.add("pduSessionId: " + (pduSessionId));
    _aidl_sj.add("sliceInfo: " + (java.util.Objects.toString(sliceInfo)));
    _aidl_sj.add("trafficDescriptors: " + (java.util.Arrays.toString(trafficDescriptors)));
    return "android.hardware.radio.data.SetupDataCallResult" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(addresses);
    _mask |= describeContents(defaultQos);
    _mask |= describeContents(qosSessions);
    _mask |= describeContents(sliceInfo);
    _mask |= describeContents(trafficDescriptors);
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
