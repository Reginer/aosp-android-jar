/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public class RadioAccessSpecifier implements android.os.Parcelable
{
  public int accessNetwork;
  public android.hardware.radio.network.RadioAccessSpecifierBands bands;
  public int[] channels;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<RadioAccessSpecifier> CREATOR = new android.os.Parcelable.Creator<RadioAccessSpecifier>() {
    @Override
    public RadioAccessSpecifier createFromParcel(android.os.Parcel _aidl_source) {
      RadioAccessSpecifier _aidl_out = new RadioAccessSpecifier();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public RadioAccessSpecifier[] newArray(int _aidl_size) {
      return new RadioAccessSpecifier[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(accessNetwork);
    _aidl_parcel.writeTypedObject(bands, _aidl_flag);
    _aidl_parcel.writeIntArray(channels);
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
      accessNetwork = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      bands = _aidl_parcel.readTypedObject(android.hardware.radio.network.RadioAccessSpecifierBands.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      channels = _aidl_parcel.createIntArray();
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
    _aidl_sj.add("accessNetwork: " + (android.hardware.radio.AccessNetwork.$.toString(accessNetwork)));
    _aidl_sj.add("bands: " + (java.util.Objects.toString(bands)));
    _aidl_sj.add("channels: " + (java.util.Arrays.toString(channels)));
    return "android.hardware.radio.network.RadioAccessSpecifier" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(bands);
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
