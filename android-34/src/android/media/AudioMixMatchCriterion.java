/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/** {@hide} */
public class AudioMixMatchCriterion implements android.os.Parcelable
{
  /** When true, rule becomes "exclude" instead of "include". */
  public boolean invert = false;
  public android.media.AudioMixMatchCriterionValue value;
  public static final android.os.Parcelable.Creator<AudioMixMatchCriterion> CREATOR = new android.os.Parcelable.Creator<AudioMixMatchCriterion>() {
    @Override
    public AudioMixMatchCriterion createFromParcel(android.os.Parcel _aidl_source) {
      AudioMixMatchCriterion _aidl_out = new AudioMixMatchCriterion();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioMixMatchCriterion[] newArray(int _aidl_size) {
      return new AudioMixMatchCriterion[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeBoolean(invert);
    _aidl_parcel.writeTypedObject(value, _aidl_flag);
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
      invert = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      value = _aidl_parcel.readTypedObject(android.media.AudioMixMatchCriterionValue.CREATOR);
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
    _mask |= describeContents(value);
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
