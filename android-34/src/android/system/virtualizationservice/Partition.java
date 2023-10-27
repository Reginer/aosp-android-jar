/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.system.virtualizationservice;
/** A partition to be assembled into a composite image. */
public class Partition implements android.os.Parcelable
{
  /** A label for the partition. */
  public java.lang.String label;
  /** The backing file descriptor of the partition image. */
  public android.os.ParcelFileDescriptor image;
  /** Whether the partition should be writable by the VM. */
  public boolean writable = false;
  public static final android.os.Parcelable.Creator<Partition> CREATOR = new android.os.Parcelable.Creator<Partition>() {
    @Override
    public Partition createFromParcel(android.os.Parcel _aidl_source) {
      Partition _aidl_out = new Partition();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public Partition[] newArray(int _aidl_size) {
      return new Partition[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(label);
    _aidl_parcel.writeTypedObject(image, _aidl_flag);
    _aidl_parcel.writeBoolean(writable);
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
      label = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      image = _aidl_parcel.readTypedObject(android.os.ParcelFileDescriptor.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      writable = _aidl_parcel.readBoolean();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(image);
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
