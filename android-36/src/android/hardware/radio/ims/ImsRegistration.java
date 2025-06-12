/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash b2a615a151c7114c4216b1987fd32d40c797d00a --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.ims-V3-java-source/gen/android/hardware/radio/ims/ImsRegistration.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.ims-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.ims/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.ims/3/android/hardware/radio/ims/ImsRegistration.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.ims;
/** @hide */
public class ImsRegistration implements android.os.Parcelable
{
  public int regState = android.hardware.radio.ims.ImsRegistrationState.NOT_REGISTERED;
  public int accessNetworkType = android.hardware.radio.AccessNetwork.UNKNOWN;
  public int suggestedAction = android.hardware.radio.ims.SuggestedAction.NONE;
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
    return "ImsRegistration" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
