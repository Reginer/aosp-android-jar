/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.sim;
public class SimApdu implements android.os.Parcelable
{
  public int sessionId = 0;
  public int cla = 0;
  public int instruction = 0;
  public int p1 = 0;
  public int p2 = 0;
  public int p3 = 0;
  public java.lang.String data;
  public boolean isEs10 = false;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<SimApdu> CREATOR = new android.os.Parcelable.Creator<SimApdu>() {
    @Override
    public SimApdu createFromParcel(android.os.Parcel _aidl_source) {
      SimApdu _aidl_out = new SimApdu();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public SimApdu[] newArray(int _aidl_size) {
      return new SimApdu[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(sessionId);
    _aidl_parcel.writeInt(cla);
    _aidl_parcel.writeInt(instruction);
    _aidl_parcel.writeInt(p1);
    _aidl_parcel.writeInt(p2);
    _aidl_parcel.writeInt(p3);
    _aidl_parcel.writeString(data);
    _aidl_parcel.writeBoolean(isEs10);
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
      sessionId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      cla = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      instruction = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      p1 = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      p2 = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      p3 = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      data = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isEs10 = _aidl_parcel.readBoolean();
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
    _aidl_sj.add("sessionId: " + (sessionId));
    _aidl_sj.add("cla: " + (cla));
    _aidl_sj.add("instruction: " + (instruction));
    _aidl_sj.add("p1: " + (p1));
    _aidl_sj.add("p2: " + (p2));
    _aidl_sj.add("p3: " + (p3));
    _aidl_sj.add("data: " + (java.util.Objects.toString(data)));
    _aidl_sj.add("isEs10: " + (isEs10));
    return "android.hardware.radio.sim.SimApdu" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
