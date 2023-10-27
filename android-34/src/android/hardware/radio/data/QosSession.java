/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.data;
public class QosSession implements android.os.Parcelable
{
  public int qosSessionId = 0;
  public android.hardware.radio.data.Qos qos;
  public android.hardware.radio.data.QosFilter[] qosFilters;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<QosSession> CREATOR = new android.os.Parcelable.Creator<QosSession>() {
    @Override
    public QosSession createFromParcel(android.os.Parcel _aidl_source) {
      QosSession _aidl_out = new QosSession();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public QosSession[] newArray(int _aidl_size) {
      return new QosSession[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(qosSessionId);
    _aidl_parcel.writeTypedObject(qos, _aidl_flag);
    _aidl_parcel.writeTypedArray(qosFilters, _aidl_flag);
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
      qosSessionId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      qos = _aidl_parcel.readTypedObject(android.hardware.radio.data.Qos.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      qosFilters = _aidl_parcel.createTypedArray(android.hardware.radio.data.QosFilter.CREATOR);
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
    _aidl_sj.add("qosSessionId: " + (qosSessionId));
    _aidl_sj.add("qos: " + (java.util.Objects.toString(qos)));
    _aidl_sj.add("qosFilters: " + (java.util.Arrays.toString(qosFilters)));
    return "android.hardware.radio.data.QosSession" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(qos);
    _mask |= describeContents(qosFilters);
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
