/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public class NrVopsInfo implements android.os.Parcelable
{
  public byte vopsSupported = 0;
  public byte emcSupported = 0;
  public byte emfSupported = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<NrVopsInfo> CREATOR = new android.os.Parcelable.Creator<NrVopsInfo>() {
    @Override
    public NrVopsInfo createFromParcel(android.os.Parcel _aidl_source) {
      NrVopsInfo _aidl_out = new NrVopsInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public NrVopsInfo[] newArray(int _aidl_size) {
      return new NrVopsInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeByte(vopsSupported);
    _aidl_parcel.writeByte(emcSupported);
    _aidl_parcel.writeByte(emfSupported);
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
      vopsSupported = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      emcSupported = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      emfSupported = _aidl_parcel.readByte();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final byte EMC_INDICATOR_NOT_SUPPORTED = 0;
  public static final byte EMC_INDICATOR_NR_CONNECTED_TO_5GCN = 1;
  public static final byte EMC_INDICATOR_EUTRA_CONNECTED_TO_5GCN = 2;
  public static final byte EMC_INDICATOR_BOTH_NR_EUTRA_CONNECTED_TO_5GCN = 3;
  public static final byte EMF_INDICATOR_NOT_SUPPORTED = 0;
  public static final byte EMF_INDICATOR_NR_CONNECTED_TO_5GCN = 1;
  public static final byte EMF_INDICATOR_EUTRA_CONNECTED_TO_5GCN = 2;
  public static final byte EMF_INDICATOR_BOTH_NR_EUTRA_CONNECTED_TO_5GCN = 3;
  public static final byte VOPS_INDICATOR_VOPS_NOT_SUPPORTED = 0;
  public static final byte VOPS_INDICATOR_VOPS_OVER_3GPP = 1;
  public static final byte VOPS_INDICATOR_VOPS_OVER_NON_3GPP = 2;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("vopsSupported: " + (vopsSupported));
    _aidl_sj.add("emcSupported: " + (emcSupported));
    _aidl_sj.add("emfSupported: " + (emfSupported));
    return "android.hardware.radio.network.NrVopsInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
