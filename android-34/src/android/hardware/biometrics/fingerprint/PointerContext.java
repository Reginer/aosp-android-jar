/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.biometrics.fingerprint;
/** @hide */
public class PointerContext implements android.os.Parcelable
{
  public int pointerId = -1;
  public float x = 0.000000f;
  public float y = 0.000000f;
  public float minor = 0.000000f;
  public float major = 0.000000f;
  public float orientation = 0.000000f;
  public boolean isAod = false;
  public long time = 0L;
  public long gestureStart = 0L;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<PointerContext> CREATOR = new android.os.Parcelable.Creator<PointerContext>() {
    @Override
    public PointerContext createFromParcel(android.os.Parcel _aidl_source) {
      PointerContext _aidl_out = new PointerContext();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public PointerContext[] newArray(int _aidl_size) {
      return new PointerContext[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(pointerId);
    _aidl_parcel.writeFloat(x);
    _aidl_parcel.writeFloat(y);
    _aidl_parcel.writeFloat(minor);
    _aidl_parcel.writeFloat(major);
    _aidl_parcel.writeFloat(orientation);
    _aidl_parcel.writeBoolean(isAod);
    _aidl_parcel.writeLong(time);
    _aidl_parcel.writeLong(gestureStart);
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
      pointerId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      x = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      y = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      minor = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      major = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      orientation = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isAod = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      time = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      gestureStart = _aidl_parcel.readLong();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof PointerContext)) return false;
    PointerContext that = (PointerContext)other;
    if (!java.util.Objects.deepEquals(pointerId, that.pointerId)) return false;
    if (!java.util.Objects.deepEquals(x, that.x)) return false;
    if (!java.util.Objects.deepEquals(y, that.y)) return false;
    if (!java.util.Objects.deepEquals(minor, that.minor)) return false;
    if (!java.util.Objects.deepEquals(major, that.major)) return false;
    if (!java.util.Objects.deepEquals(orientation, that.orientation)) return false;
    if (!java.util.Objects.deepEquals(isAod, that.isAod)) return false;
    if (!java.util.Objects.deepEquals(time, that.time)) return false;
    if (!java.util.Objects.deepEquals(gestureStart, that.gestureStart)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(pointerId, x, y, minor, major, orientation, isAod, time, gestureStart).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
