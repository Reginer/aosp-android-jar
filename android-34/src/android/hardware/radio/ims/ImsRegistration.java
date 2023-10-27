/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.ims;
public class ImsRegistration implements android.os.Parcelable
{
  public int regState;
  public int accessNetworkType;
  public int suggestedAction;
  public int capabilities = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<ImsRegistration> CREATOR = new android.os.Parcelable.Creator<ImsRegistration>() {
    @Override
    public ImsRegistration createFromParcel(android.os.Parcel _aidl_source) {
      ImsRegistration _aidl_out = new ImsRegistration();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ImsRegistration[] newArray(int _aidl_size) {
      return new ImsRegistration[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(regState);
    _aidl_parcel.writeInt(accessNetworkType);
    _aidl_parcel.writeInt(suggestedAction);
    _aidl_parcel.writeInt(capabilities);
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
      regState = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      accessNetworkType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      suggestedAction = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      capabilities = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int IMS_MMTEL_CAPABILITY_NONE = 0;
  public static final int IMS_MMTEL_CAPABILITY_VOICE = 1;
  public static final int IMS_MMTEL_CAPABILITY_VIDEO = 2;
  public static final int IMS_MMTEL_CAPABILITY_SMS = 4;
  public static final int IMS_RCS_CAPABILITIES = 8;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("regState: " + (android.hardware.radio.ims.ImsRegistrationState.$.toString(regState)));
    _aidl_sj.add("accessNetworkType: " + (android.hardware.radio.AccessNetwork.$.toString(accessNetworkType)));
    _aidl_sj.add("suggestedAction: " + (android.hardware.radio.ims.SuggestedAction.$.toString(suggestedAction)));
    _aidl_sj.add("capabilities: " + (capabilities));
    return "android.hardware.radio.ims.ImsRegistration" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
