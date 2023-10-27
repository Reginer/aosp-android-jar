/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.graphics.common;
/** @hide */
public class PlaneLayout implements android.os.Parcelable
{
  public android.hardware.graphics.common.PlaneLayoutComponent[] components;
  public long offsetInBytes = 0L;
  public long sampleIncrementInBits = 0L;
  public long strideInBytes = 0L;
  public long widthInSamples = 0L;
  public long heightInSamples = 0L;
  public long totalSizeInBytes = 0L;
  public long horizontalSubsampling = 0L;
  public long verticalSubsampling = 0L;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<PlaneLayout> CREATOR = new android.os.Parcelable.Creator<PlaneLayout>() {
    @Override
    public PlaneLayout createFromParcel(android.os.Parcel _aidl_source) {
      PlaneLayout _aidl_out = new PlaneLayout();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public PlaneLayout[] newArray(int _aidl_size) {
      return new PlaneLayout[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedArray(components, _aidl_flag);
    _aidl_parcel.writeLong(offsetInBytes);
    _aidl_parcel.writeLong(sampleIncrementInBits);
    _aidl_parcel.writeLong(strideInBytes);
    _aidl_parcel.writeLong(widthInSamples);
    _aidl_parcel.writeLong(heightInSamples);
    _aidl_parcel.writeLong(totalSizeInBytes);
    _aidl_parcel.writeLong(horizontalSubsampling);
    _aidl_parcel.writeLong(verticalSubsampling);
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
      components = _aidl_parcel.createTypedArray(android.hardware.graphics.common.PlaneLayoutComponent.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      offsetInBytes = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sampleIncrementInBits = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      strideInBytes = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      widthInSamples = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      heightInSamples = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      totalSizeInBytes = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      horizontalSubsampling = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      verticalSubsampling = _aidl_parcel.readLong();
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
    _mask |= describeContents(components);
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
