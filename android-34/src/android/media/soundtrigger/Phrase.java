/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.soundtrigger;
/** @hide */
public class Phrase implements android.os.Parcelable
{
  public int id = 0;
  public int recognitionModes = 0;
  public int[] users;
  public java.lang.String locale;
  public java.lang.String text;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<Phrase> CREATOR = new android.os.Parcelable.Creator<Phrase>() {
    @Override
    public Phrase createFromParcel(android.os.Parcel _aidl_source) {
      Phrase _aidl_out = new Phrase();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public Phrase[] newArray(int _aidl_size) {
      return new Phrase[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(id);
    _aidl_parcel.writeInt(recognitionModes);
    _aidl_parcel.writeIntArray(users);
    _aidl_parcel.writeString(locale);
    _aidl_parcel.writeString(text);
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
      id = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      recognitionModes = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      users = _aidl_parcel.createIntArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      locale = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      text = _aidl_parcel.readString();
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
    _aidl_sj.add("id: " + (id));
    _aidl_sj.add("recognitionModes: " + (recognitionModes));
    _aidl_sj.add("users: " + (java.util.Arrays.toString(users)));
    _aidl_sj.add("locale: " + (java.util.Objects.toString(locale)));
    _aidl_sj.add("text: " + (java.util.Objects.toString(text)));
    return "android.media.soundtrigger.Phrase" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof Phrase)) return false;
    Phrase that = (Phrase)other;
    if (!java.util.Objects.deepEquals(id, that.id)) return false;
    if (!java.util.Objects.deepEquals(recognitionModes, that.recognitionModes)) return false;
    if (!java.util.Objects.deepEquals(users, that.users)) return false;
    if (!java.util.Objects.deepEquals(locale, that.locale)) return false;
    if (!java.util.Objects.deepEquals(text, that.text)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(id, recognitionModes, users, locale, text).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
