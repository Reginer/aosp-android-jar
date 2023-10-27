/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.data;
public class DataProfileInfo implements android.os.Parcelable
{
  public int profileId = 0;
  public java.lang.String apn;
  public int protocol;
  public int roamingProtocol;
  public int authType;
  public java.lang.String user;
  public java.lang.String password;
  public int type = 0;
  public int maxConnsTime = 0;
  public int maxConns = 0;
  public int waitTime = 0;
  public boolean enabled = false;
  public int supportedApnTypesBitmap = 0;
  public int bearerBitmap = 0;
  public int mtuV4 = 0;
  public int mtuV6 = 0;
  public boolean preferred = false;
  public boolean persistent = false;
  public boolean alwaysOn = false;
  public android.hardware.radio.data.TrafficDescriptor trafficDescriptor;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<DataProfileInfo> CREATOR = new android.os.Parcelable.Creator<DataProfileInfo>() {
    @Override
    public DataProfileInfo createFromParcel(android.os.Parcel _aidl_source) {
      DataProfileInfo _aidl_out = new DataProfileInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public DataProfileInfo[] newArray(int _aidl_size) {
      return new DataProfileInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(profileId);
    _aidl_parcel.writeString(apn);
    _aidl_parcel.writeInt(protocol);
    _aidl_parcel.writeInt(roamingProtocol);
    _aidl_parcel.writeInt(authType);
    _aidl_parcel.writeString(user);
    _aidl_parcel.writeString(password);
    _aidl_parcel.writeInt(type);
    _aidl_parcel.writeInt(maxConnsTime);
    _aidl_parcel.writeInt(maxConns);
    _aidl_parcel.writeInt(waitTime);
    _aidl_parcel.writeBoolean(enabled);
    _aidl_parcel.writeInt(supportedApnTypesBitmap);
    _aidl_parcel.writeInt(bearerBitmap);
    _aidl_parcel.writeInt(mtuV4);
    _aidl_parcel.writeInt(mtuV6);
    _aidl_parcel.writeBoolean(preferred);
    _aidl_parcel.writeBoolean(persistent);
    _aidl_parcel.writeBoolean(alwaysOn);
    _aidl_parcel.writeTypedObject(trafficDescriptor, _aidl_flag);
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
      profileId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      apn = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      protocol = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      roamingProtocol = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      authType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      user = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      password = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      type = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxConnsTime = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxConns = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      waitTime = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      enabled = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      supportedApnTypesBitmap = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      bearerBitmap = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mtuV4 = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mtuV6 = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      preferred = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      persistent = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      alwaysOn = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      trafficDescriptor = _aidl_parcel.readTypedObject(android.hardware.radio.data.TrafficDescriptor.CREATOR);
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int ID_DEFAULT = 0;
  public static final int ID_TETHERED = 1;
  public static final int ID_IMS = 2;
  public static final int ID_FOTA = 3;
  public static final int ID_CBS = 4;
  public static final int ID_OEM_BASE = 1000;
  public static final int ID_INVALID = -1;
  public static final int TYPE_COMMON = 0;
  public static final int TYPE_3GPP = 1;
  public static final int TYPE_3GPP2 = 2;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("profileId: " + (profileId));
    _aidl_sj.add("apn: " + (java.util.Objects.toString(apn)));
    _aidl_sj.add("protocol: " + (android.hardware.radio.data.PdpProtocolType.$.toString(protocol)));
    _aidl_sj.add("roamingProtocol: " + (android.hardware.radio.data.PdpProtocolType.$.toString(roamingProtocol)));
    _aidl_sj.add("authType: " + (android.hardware.radio.data.ApnAuthType.$.toString(authType)));
    _aidl_sj.add("user: " + (java.util.Objects.toString(user)));
    _aidl_sj.add("password: " + (java.util.Objects.toString(password)));
    _aidl_sj.add("type: " + (type));
    _aidl_sj.add("maxConnsTime: " + (maxConnsTime));
    _aidl_sj.add("maxConns: " + (maxConns));
    _aidl_sj.add("waitTime: " + (waitTime));
    _aidl_sj.add("enabled: " + (enabled));
    _aidl_sj.add("supportedApnTypesBitmap: " + (supportedApnTypesBitmap));
    _aidl_sj.add("bearerBitmap: " + (bearerBitmap));
    _aidl_sj.add("mtuV4: " + (mtuV4));
    _aidl_sj.add("mtuV6: " + (mtuV6));
    _aidl_sj.add("preferred: " + (preferred));
    _aidl_sj.add("persistent: " + (persistent));
    _aidl_sj.add("alwaysOn: " + (alwaysOn));
    _aidl_sj.add("trafficDescriptor: " + (java.util.Objects.toString(trafficDescriptor)));
    return "android.hardware.radio.data.DataProfileInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(trafficDescriptor);
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
