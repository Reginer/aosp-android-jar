/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.sim;
public class IccIo implements android.os.Parcelable
{
  public int command = 0;
  public int fileId = 0;
  public java.lang.String path;
  public int p1 = 0;
  public int p2 = 0;
  public int p3 = 0;
  public java.lang.String data;
  public java.lang.String pin2;
  public java.lang.String aid;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<IccIo> CREATOR = new android.os.Parcelable.Creator<IccIo>() {
    @Override
    public IccIo createFromParcel(android.os.Parcel _aidl_source) {
      IccIo _aidl_out = new IccIo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public IccIo[] newArray(int _aidl_size) {
      return new IccIo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(command);
    _aidl_parcel.writeInt(fileId);
    _aidl_parcel.writeString(path);
    _aidl_parcel.writeInt(p1);
    _aidl_parcel.writeInt(p2);
    _aidl_parcel.writeInt(p3);
    _aidl_parcel.writeString(data);
    _aidl_parcel.writeString(pin2);
    _aidl_parcel.writeString(aid);
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
      command = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      fileId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      path = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      p1 = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      p2 = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      p3 = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      data = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      pin2 = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      aid = _aidl_parcel.readString();
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
    _aidl_sj.add("command: " + (command));
    _aidl_sj.add("fileId: " + (fileId));
    _aidl_sj.add("path: " + (java.util.Objects.toString(path)));
    _aidl_sj.add("p1: " + (p1));
    _aidl_sj.add("p2: " + (p2));
    _aidl_sj.add("p3: " + (p3));
    _aidl_sj.add("data: " + (java.util.Objects.toString(data)));
    _aidl_sj.add("pin2: " + (java.util.Objects.toString(pin2)));
    _aidl_sj.add("aid: " + (java.util.Objects.toString(aid)));
    return "android.hardware.radio.sim.IccIo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
