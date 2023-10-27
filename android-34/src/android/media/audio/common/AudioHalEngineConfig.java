/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
public class AudioHalEngineConfig implements android.os.Parcelable
{
  public int defaultProductStrategyId = -1;
  public android.media.audio.common.AudioHalProductStrategy[] productStrategies;
  public android.media.audio.common.AudioHalVolumeGroup[] volumeGroups;
  public android.media.audio.common.AudioHalEngineConfig.CapSpecificConfig capSpecificConfig;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioHalEngineConfig> CREATOR = new android.os.Parcelable.Creator<AudioHalEngineConfig>() {
    @Override
    public AudioHalEngineConfig createFromParcel(android.os.Parcel _aidl_source) {
      AudioHalEngineConfig _aidl_out = new AudioHalEngineConfig();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioHalEngineConfig[] newArray(int _aidl_size) {
      return new AudioHalEngineConfig[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(defaultProductStrategyId);
    _aidl_parcel.writeTypedArray(productStrategies, _aidl_flag);
    _aidl_parcel.writeTypedArray(volumeGroups, _aidl_flag);
    _aidl_parcel.writeTypedObject(capSpecificConfig, _aidl_flag);
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
      defaultProductStrategyId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      productStrategies = _aidl_parcel.createTypedArray(android.media.audio.common.AudioHalProductStrategy.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      volumeGroups = _aidl_parcel.createTypedArray(android.media.audio.common.AudioHalVolumeGroup.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      capSpecificConfig = _aidl_parcel.readTypedObject(android.media.audio.common.AudioHalEngineConfig.CapSpecificConfig.CREATOR);
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
    _aidl_sj.add("defaultProductStrategyId: " + (defaultProductStrategyId));
    _aidl_sj.add("productStrategies: " + (java.util.Arrays.toString(productStrategies)));
    _aidl_sj.add("volumeGroups: " + (java.util.Arrays.toString(volumeGroups)));
    _aidl_sj.add("capSpecificConfig: " + (java.util.Objects.toString(capSpecificConfig)));
    return "android.media.audio.common.AudioHalEngineConfig" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioHalEngineConfig)) return false;
    AudioHalEngineConfig that = (AudioHalEngineConfig)other;
    if (!java.util.Objects.deepEquals(defaultProductStrategyId, that.defaultProductStrategyId)) return false;
    if (!java.util.Objects.deepEquals(productStrategies, that.productStrategies)) return false;
    if (!java.util.Objects.deepEquals(volumeGroups, that.volumeGroups)) return false;
    if (!java.util.Objects.deepEquals(capSpecificConfig, that.capSpecificConfig)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(defaultProductStrategyId, productStrategies, volumeGroups, capSpecificConfig).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(productStrategies);
    _mask |= describeContents(volumeGroups);
    _mask |= describeContents(capSpecificConfig);
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
  public static class CapSpecificConfig implements android.os.Parcelable
  {
    public android.media.audio.common.AudioHalCapCriterion[] criteria;
    public android.media.audio.common.AudioHalCapCriterionType[] criterionTypes;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<CapSpecificConfig> CREATOR = new android.os.Parcelable.Creator<CapSpecificConfig>() {
      @Override
      public CapSpecificConfig createFromParcel(android.os.Parcel _aidl_source) {
        CapSpecificConfig _aidl_out = new CapSpecificConfig();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public CapSpecificConfig[] newArray(int _aidl_size) {
        return new CapSpecificConfig[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeTypedArray(criteria, _aidl_flag);
      _aidl_parcel.writeTypedArray(criterionTypes, _aidl_flag);
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
        criteria = _aidl_parcel.createTypedArray(android.media.audio.common.AudioHalCapCriterion.CREATOR);
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        criterionTypes = _aidl_parcel.createTypedArray(android.media.audio.common.AudioHalCapCriterionType.CREATOR);
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
      _mask |= describeContents(criteria);
      _mask |= describeContents(criterionTypes);
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
}
