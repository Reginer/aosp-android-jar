/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.soundtrigger;
/** @hide */
public class PhraseSoundModel implements android.os.Parcelable
{
  public android.media.soundtrigger.SoundModel common;
  public android.media.soundtrigger.Phrase[] phrases;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<PhraseSoundModel> CREATOR = new android.os.Parcelable.Creator<PhraseSoundModel>() {
    @Override
    public PhraseSoundModel createFromParcel(android.os.Parcel _aidl_source) {
      PhraseSoundModel _aidl_out = new PhraseSoundModel();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public PhraseSoundModel[] newArray(int _aidl_size) {
      return new PhraseSoundModel[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(common, _aidl_flag);
    _aidl_parcel.writeTypedArray(phrases, _aidl_flag);
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
      common = _aidl_parcel.readTypedObject(android.media.soundtrigger.SoundModel.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      phrases = _aidl_parcel.createTypedArray(android.media.soundtrigger.Phrase.CREATOR);
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
    _aidl_sj.add("common: " + (java.util.Objects.toString(common)));
    _aidl_sj.add("phrases: " + (java.util.Arrays.toString(phrases)));
    return "android.media.soundtrigger.PhraseSoundModel" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof PhraseSoundModel)) return false;
    PhraseSoundModel that = (PhraseSoundModel)other;
    if (!java.util.Objects.deepEquals(common, that.common)) return false;
    if (!java.util.Objects.deepEquals(phrases, that.phrases)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(common, phrases).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(common);
    _mask |= describeContents(phrases);
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
