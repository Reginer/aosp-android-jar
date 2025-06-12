/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash fc1a19a4f86a58981158cc8d956763c9d8ace630 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.config_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.sim-V4-java-source/gen/android/hardware/radio/sim/PhonebookCapacity.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.sim-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.sim/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.sim/4/android/hardware/radio/sim/PhonebookCapacity.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.sim;
/** @hide */
public class PhonebookCapacity implements android.os.Parcelable
{
  public int maxAdnRecords = 0;
  public int usedAdnRecords = 0;
  public int maxEmailRecords = 0;
  public int usedEmailRecords = 0;
  public int maxAdditionalNumberRecords = 0;
  public int usedAdditionalNumberRecords = 0;
  public int maxNameLen = 0;
  public int maxNumberLen = 0;
  public int maxEmailLen = 0;
  public int maxAdditionalNumberLen = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<PhonebookCapacity> CREATOR = new android.os.Parcelable.Creator<PhonebookCapacity>() {
    @Override
    public PhonebookCapacity createFromParcel(android.os.Parcel _aidl_source) {
      PhonebookCapacity _aidl_out = new PhonebookCapacity();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public PhonebookCapacity[] newArray(int _aidl_size) {
      return new PhonebookCapacity[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(maxAdnRecords);
    _aidl_parcel.writeInt(usedAdnRecords);
    _aidl_parcel.writeInt(maxEmailRecords);
    _aidl_parcel.writeInt(usedEmailRecords);
    _aidl_parcel.writeInt(maxAdditionalNumberRecords);
    _aidl_parcel.writeInt(usedAdditionalNumberRecords);
    _aidl_parcel.writeInt(maxNameLen);
    _aidl_parcel.writeInt(maxNumberLen);
    _aidl_parcel.writeInt(maxEmailLen);
    _aidl_parcel.writeInt(maxAdditionalNumberLen);
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
      maxAdnRecords = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      usedAdnRecords = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxEmailRecords = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      usedEmailRecords = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxAdditionalNumberRecords = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      usedAdditionalNumberRecords = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxNameLen = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxNumberLen = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxEmailLen = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxAdditionalNumberLen = _aidl_parcel.readInt();
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
    _aidl_sj.add("maxAdnRecords: " + (maxAdnRecords));
    _aidl_sj.add("usedAdnRecords: " + (usedAdnRecords));
    _aidl_sj.add("maxEmailRecords: " + (maxEmailRecords));
    _aidl_sj.add("usedEmailRecords: " + (usedEmailRecords));
    _aidl_sj.add("maxAdditionalNumberRecords: " + (maxAdditionalNumberRecords));
    _aidl_sj.add("usedAdditionalNumberRecords: " + (usedAdditionalNumberRecords));
    _aidl_sj.add("maxNameLen: " + (maxNameLen));
    _aidl_sj.add("maxNumberLen: " + (maxNumberLen));
    _aidl_sj.add("maxEmailLen: " + (maxEmailLen));
    _aidl_sj.add("maxAdditionalNumberLen: " + (maxAdditionalNumberLen));
    return "PhonebookCapacity" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
