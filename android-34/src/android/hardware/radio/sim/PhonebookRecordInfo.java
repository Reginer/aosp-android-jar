/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.sim;
public class PhonebookRecordInfo implements android.os.Parcelable
{
  public int recordId = 0;
  public java.lang.String name;
  public java.lang.String number;
  public java.lang.String[] emails;
  public java.lang.String[] additionalNumbers;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<PhonebookRecordInfo> CREATOR = new android.os.Parcelable.Creator<PhonebookRecordInfo>() {
    @Override
    public PhonebookRecordInfo createFromParcel(android.os.Parcel _aidl_source) {
      PhonebookRecordInfo _aidl_out = new PhonebookRecordInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public PhonebookRecordInfo[] newArray(int _aidl_size) {
      return new PhonebookRecordInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(recordId);
    _aidl_parcel.writeString(name);
    _aidl_parcel.writeString(number);
    _aidl_parcel.writeStringArray(emails);
    _aidl_parcel.writeStringArray(additionalNumbers);
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
      recordId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      name = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      number = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      emails = _aidl_parcel.createStringArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      additionalNumbers = _aidl_parcel.createStringArray();
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
    _aidl_sj.add("recordId: " + (recordId));
    _aidl_sj.add("name: " + (java.util.Objects.toString(name)));
    _aidl_sj.add("number: " + (java.util.Objects.toString(number)));
    _aidl_sj.add("emails: " + (java.util.Arrays.toString(emails)));
    _aidl_sj.add("additionalNumbers: " + (java.util.Arrays.toString(additionalNumbers)));
    return "android.hardware.radio.sim.PhonebookRecordInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
