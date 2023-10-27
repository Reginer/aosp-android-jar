/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.gnss.measurement_corrections;
/** @hide */
public class SingleSatCorrection implements android.os.Parcelable
{
  public int singleSatCorrectionFlags = 0;
  public int constellation;
  public int svid = 0;
  public long carrierFrequencyHz = 0L;
  public float probSatIsLos = 0.000000f;
  public float combinedExcessPathLengthMeters = 0.000000f;
  public float combinedExcessPathLengthUncertaintyMeters = 0.000000f;
  public float combinedAttenuationDb = 0.000000f;
  public android.hardware.gnss.measurement_corrections.SingleSatCorrection.ExcessPathInfo[] excessPathInfos;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<SingleSatCorrection> CREATOR = new android.os.Parcelable.Creator<SingleSatCorrection>() {
    @Override
    public SingleSatCorrection createFromParcel(android.os.Parcel _aidl_source) {
      SingleSatCorrection _aidl_out = new SingleSatCorrection();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public SingleSatCorrection[] newArray(int _aidl_size) {
      return new SingleSatCorrection[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(singleSatCorrectionFlags);
    _aidl_parcel.writeInt(constellation);
    _aidl_parcel.writeInt(svid);
    _aidl_parcel.writeLong(carrierFrequencyHz);
    _aidl_parcel.writeFloat(probSatIsLos);
    _aidl_parcel.writeFloat(combinedExcessPathLengthMeters);
    _aidl_parcel.writeFloat(combinedExcessPathLengthUncertaintyMeters);
    _aidl_parcel.writeFloat(combinedAttenuationDb);
    _aidl_parcel.writeTypedArray(excessPathInfos, _aidl_flag);
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
      singleSatCorrectionFlags = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      constellation = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      svid = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      carrierFrequencyHz = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      probSatIsLos = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      combinedExcessPathLengthMeters = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      combinedExcessPathLengthUncertaintyMeters = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      combinedAttenuationDb = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      excessPathInfos = _aidl_parcel.createTypedArray(android.hardware.gnss.measurement_corrections.SingleSatCorrection.ExcessPathInfo.CREATOR);
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int SINGLE_SAT_CORRECTION_HAS_SAT_IS_LOS_PROBABILITY = 1;
  public static final int SINGLE_SAT_CORRECTION_HAS_COMBINED_EXCESS_PATH_LENGTH = 2;
  public static final int SINGLE_SAT_CORRECTION_HAS_COMBINED_EXCESS_PATH_LENGTH_UNC = 4;
  public static final int SINGLE_SAT_CORRECTION_HAS_COMBINED_ATTENUATION = 16;
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(excessPathInfos);
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
  public static class ExcessPathInfo implements android.os.Parcelable
  {
    public int excessPathInfoFlags = 0;
    public float excessPathLengthMeters = 0.000000f;
    public float excessPathLengthUncertaintyMeters = 0.000000f;
    public android.hardware.gnss.measurement_corrections.ReflectingPlane reflectingPlane;
    public float attenuationDb = 0.000000f;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<ExcessPathInfo> CREATOR = new android.os.Parcelable.Creator<ExcessPathInfo>() {
      @Override
      public ExcessPathInfo createFromParcel(android.os.Parcel _aidl_source) {
        ExcessPathInfo _aidl_out = new ExcessPathInfo();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public ExcessPathInfo[] newArray(int _aidl_size) {
        return new ExcessPathInfo[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeInt(excessPathInfoFlags);
      _aidl_parcel.writeFloat(excessPathLengthMeters);
      _aidl_parcel.writeFloat(excessPathLengthUncertaintyMeters);
      _aidl_parcel.writeTypedObject(reflectingPlane, _aidl_flag);
      _aidl_parcel.writeFloat(attenuationDb);
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
        excessPathInfoFlags = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        excessPathLengthMeters = _aidl_parcel.readFloat();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        excessPathLengthUncertaintyMeters = _aidl_parcel.readFloat();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        reflectingPlane = _aidl_parcel.readTypedObject(android.hardware.gnss.measurement_corrections.ReflectingPlane.CREATOR);
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        attenuationDb = _aidl_parcel.readFloat();
      } finally {
        if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
          throw new android.os.BadParcelableException("Overflow in the size of parcelable");
        }
        _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
      }
    }
    public static final int EXCESS_PATH_INFO_HAS_EXCESS_PATH_LENGTH = 1;
    public static final int EXCESS_PATH_INFO_HAS_EXCESS_PATH_LENGTH_UNC = 2;
    public static final int EXCESS_PATH_INFO_HAS_REFLECTING_PLANE = 4;
    public static final int EXCESS_PATH_INFO_HAS_ATTENUATION = 8;
    @Override
    public int describeContents() {
      int _mask = 0;
      _mask |= describeContents(reflectingPlane);
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
}
