/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.usb.gadget;
public class GadgetFunction implements android.os.Parcelable
{
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<GadgetFunction> CREATOR = new android.os.Parcelable.Creator<GadgetFunction>() {
    @Override
    public GadgetFunction createFromParcel(android.os.Parcel _aidl_source) {
      GadgetFunction _aidl_out = new GadgetFunction();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public GadgetFunction[] newArray(int _aidl_size) {
      return new GadgetFunction[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
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
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final long NONE = 0L;
  public static final long ADB = 1L;
  public static final long ACCESSORY = 2L;
  public static final long MTP = 4L;
  public static final long MIDI = 8L;
  public static final long PTP = 16L;
  public static final long RNDIS = 32L;
  public static final long AUDIO_SOURCE = 64L;
  public static final long UVC = 128L;
  public static final long NCM = 1024L;
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
