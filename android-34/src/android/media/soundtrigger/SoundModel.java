/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.soundtrigger;
/** @hide */
public class SoundModel implements android.os.Parcelable
{
  public int type = android.media.soundtrigger.SoundModelType.INVALID;
  public java.lang.String uuid;
  public java.lang.String vendorUuid;
  public android.os.ParcelFileDescriptor data;
  public int dataSize = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<SoundModel> CREATOR = new android.os.Parcelable.Creator<SoundModel>() {
    @Override
    public SoundModel createFromParcel(android.os.Parcel _aidl_source) {
      SoundModel _aidl_out = new SoundModel();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public SoundModel[] newArray(int _aidl_size) {
      return new SoundModel[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(type);
    _aidl_parcel.writeString(uuid);
    _aidl_parcel.writeString(vendorUuid);
    _aidl_parcel.writeTypedObject(data, _aidl_flag);
    _aidl_parcel.writeInt(dataSize);
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
      type = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      uuid = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      vendorUuid = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      data = _aidl_parcel.readTypedObject(android.os.ParcelFileDescriptor.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      dataSize = _aidl_parcel.readInt();
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
    _aidl_sj.add("type: " + (type));
    _aidl_sj.add("uuid: " + (java.util.Objects.toString(uuid)));
    _aidl_sj.add("vendorUuid: " + (java.util.Objects.toString(vendorUuid)));
    _aidl_sj.add("data: " + (java.util.Objects.toString(data)));
    _aidl_sj.add("dataSize: " + (dataSize));
    return "android.media.soundtrigger.SoundModel" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof SoundModel)) return false;
    SoundModel that = (SoundModel)other;
    if (!java.util.Objects.deepEquals(type, that.type)) return false;
    if (!java.util.Objects.deepEquals(uuid, that.uuid)) return false;
    if (!java.util.Objects.deepEquals(vendorUuid, that.vendorUuid)) return false;
    if (!java.util.Objects.deepEquals(data, that.data)) return false;
    if (!java.util.Objects.deepEquals(dataSize, that.dataSize)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(type, uuid, vendorUuid, data, dataSize).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(data);
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
