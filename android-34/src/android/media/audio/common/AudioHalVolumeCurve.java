/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
public class AudioHalVolumeCurve implements android.os.Parcelable
{
  public byte deviceCategory = android.media.audio.common.AudioHalVolumeCurve.DeviceCategory.SPEAKER;
  public android.media.audio.common.AudioHalVolumeCurve.CurvePoint[] curvePoints;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioHalVolumeCurve> CREATOR = new android.os.Parcelable.Creator<AudioHalVolumeCurve>() {
    @Override
    public AudioHalVolumeCurve createFromParcel(android.os.Parcel _aidl_source) {
      AudioHalVolumeCurve _aidl_out = new AudioHalVolumeCurve();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioHalVolumeCurve[] newArray(int _aidl_size) {
      return new AudioHalVolumeCurve[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeByte(deviceCategory);
    _aidl_parcel.writeTypedArray(curvePoints, _aidl_flag);
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
      deviceCategory = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      curvePoints = _aidl_parcel.createTypedArray(android.media.audio.common.AudioHalVolumeCurve.CurvePoint.CREATOR);
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
    _aidl_sj.add("deviceCategory: " + (deviceCategory));
    _aidl_sj.add("curvePoints: " + (java.util.Arrays.toString(curvePoints)));
    return "android.media.audio.common.AudioHalVolumeCurve" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioHalVolumeCurve)) return false;
    AudioHalVolumeCurve that = (AudioHalVolumeCurve)other;
    if (!java.util.Objects.deepEquals(deviceCategory, that.deviceCategory)) return false;
    if (!java.util.Objects.deepEquals(curvePoints, that.curvePoints)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(deviceCategory, curvePoints).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(curvePoints);
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
  public static @interface DeviceCategory {
    public static final byte HEADSET = 0;
    public static final byte SPEAKER = 1;
    public static final byte EARPIECE = 2;
    public static final byte EXT_MEDIA = 3;
    public static final byte HEARING_AID = 4;
  }
  public static class CurvePoint implements android.os.Parcelable
  {
    public byte index = 0;
    public int attenuationMb = 0;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<CurvePoint> CREATOR = new android.os.Parcelable.Creator<CurvePoint>() {
      @Override
      public CurvePoint createFromParcel(android.os.Parcel _aidl_source) {
        CurvePoint _aidl_out = new CurvePoint();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public CurvePoint[] newArray(int _aidl_size) {
        return new CurvePoint[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeByte(index);
      _aidl_parcel.writeInt(attenuationMb);
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
        index = _aidl_parcel.readByte();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        attenuationMb = _aidl_parcel.readInt();
      } finally {
        if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
          throw new android.os.BadParcelableException("Overflow in the size of parcelable");
        }
        _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
      }
    }
    public static final byte MIN_INDEX = 0;
    public static final byte MAX_INDEX = 100;
    @Override
    public int describeContents() {
      int _mask = 0;
      return _mask;
    }
  }
}
