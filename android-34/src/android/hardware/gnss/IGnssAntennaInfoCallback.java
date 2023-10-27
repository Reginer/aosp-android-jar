/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.gnss;
/** @hide */
public interface IGnssAntennaInfoCallback extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "fc957f1d3d261d065ff5e5415f2d21caa79c310f";
  /** Default implementation for IGnssAntennaInfoCallback. */
  public static class Default implements android.hardware.gnss.IGnssAntennaInfoCallback
  {
    @Override public void gnssAntennaInfoCb(android.hardware.gnss.IGnssAntennaInfoCallback.GnssAntennaInfo[] gnssAntennaInfos) throws android.os.RemoteException
    {
    }
    @Override
    public int getInterfaceVersion() {
      return 0;
    }
    @Override
    public String getInterfaceHash() {
      return "";
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.hardware.gnss.IGnssAntennaInfoCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.gnss.IGnssAntennaInfoCallback interface,
     * generating a proxy if needed.
     */
    public static android.hardware.gnss.IGnssAntennaInfoCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.gnss.IGnssAntennaInfoCallback))) {
        return ((android.hardware.gnss.IGnssAntennaInfoCallback)iin);
      }
      return new android.hardware.gnss.IGnssAntennaInfoCallback.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    /** @hide */
    public static java.lang.String getDefaultTransactionName(int transactionCode)
    {
      switch (transactionCode)
      {
        case TRANSACTION_gnssAntennaInfoCb:
        {
          return "gnssAntennaInfoCb";
        }
        case TRANSACTION_getInterfaceVersion:
        {
          return "getInterfaceVersion";
        }
        case TRANSACTION_getInterfaceHash:
        {
          return "getInterfaceHash";
        }
        default:
        {
          return null;
        }
      }
    }
    /** @hide */
    public java.lang.String getTransactionName(int transactionCode)
    {
      return this.getDefaultTransactionName(transactionCode);
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
        case TRANSACTION_getInterfaceVersion:
        {
          reply.writeNoException();
          reply.writeInt(getInterfaceVersion());
          return true;
        }
        case TRANSACTION_getInterfaceHash:
        {
          reply.writeNoException();
          reply.writeString(getInterfaceHash());
          return true;
        }
      }
      switch (code)
      {
        case TRANSACTION_gnssAntennaInfoCb:
        {
          android.hardware.gnss.IGnssAntennaInfoCallback.GnssAntennaInfo[] _arg0;
          _arg0 = data.createTypedArray(android.hardware.gnss.IGnssAntennaInfoCallback.GnssAntennaInfo.CREATOR);
          data.enforceNoDataAvail();
          this.gnssAntennaInfoCb(_arg0);
          reply.writeNoException();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.gnss.IGnssAntennaInfoCallback
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      private int mCachedVersion = -1;
      private String mCachedHash = "-1";
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public void gnssAntennaInfoCb(android.hardware.gnss.IGnssAntennaInfoCallback.GnssAntennaInfo[] gnssAntennaInfos) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedArray(gnssAntennaInfos, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_gnssAntennaInfoCb, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method gnssAntennaInfoCb is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override
      public int getInterfaceVersion() throws android.os.RemoteException {
        if (mCachedVersion == -1) {
          android.os.Parcel data = android.os.Parcel.obtain(asBinder());
          android.os.Parcel reply = android.os.Parcel.obtain();
          try {
            data.writeInterfaceToken(DESCRIPTOR);
            boolean _status = mRemote.transact(Stub.TRANSACTION_getInterfaceVersion, data, reply, 0);
            reply.readException();
            mCachedVersion = reply.readInt();
          } finally {
            reply.recycle();
            data.recycle();
          }
        }
        return mCachedVersion;
      }
      @Override
      public synchronized String getInterfaceHash() throws android.os.RemoteException {
        if ("-1".equals(mCachedHash)) {
          android.os.Parcel data = android.os.Parcel.obtain(asBinder());
          android.os.Parcel reply = android.os.Parcel.obtain();
          try {
            data.writeInterfaceToken(DESCRIPTOR);
            boolean _status = mRemote.transact(Stub.TRANSACTION_getInterfaceHash, data, reply, 0);
            reply.readException();
            mCachedHash = reply.readString();
          } finally {
            reply.recycle();
            data.recycle();
          }
        }
        return mCachedHash;
      }
    }
    static final int TRANSACTION_gnssAntennaInfoCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$gnss$IGnssAntennaInfoCallback".replace('$', '.');
  public void gnssAntennaInfoCb(android.hardware.gnss.IGnssAntennaInfoCallback.GnssAntennaInfo[] gnssAntennaInfos) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
  public static class Row implements android.os.Parcelable
  {
    public double[] row;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<Row> CREATOR = new android.os.Parcelable.Creator<Row>() {
      @Override
      public Row createFromParcel(android.os.Parcel _aidl_source) {
        Row _aidl_out = new Row();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public Row[] newArray(int _aidl_size) {
        return new Row[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeDoubleArray(row);
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
        row = _aidl_parcel.createDoubleArray();
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
      return _mask;
    }
  }
  public static class Coord implements android.os.Parcelable
  {
    public double x = 0.000000;
    public double xUncertainty = 0.000000;
    public double y = 0.000000;
    public double yUncertainty = 0.000000;
    public double z = 0.000000;
    public double zUncertainty = 0.000000;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<Coord> CREATOR = new android.os.Parcelable.Creator<Coord>() {
      @Override
      public Coord createFromParcel(android.os.Parcel _aidl_source) {
        Coord _aidl_out = new Coord();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public Coord[] newArray(int _aidl_size) {
        return new Coord[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeDouble(x);
      _aidl_parcel.writeDouble(xUncertainty);
      _aidl_parcel.writeDouble(y);
      _aidl_parcel.writeDouble(yUncertainty);
      _aidl_parcel.writeDouble(z);
      _aidl_parcel.writeDouble(zUncertainty);
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
        x = _aidl_parcel.readDouble();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        xUncertainty = _aidl_parcel.readDouble();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        y = _aidl_parcel.readDouble();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        yUncertainty = _aidl_parcel.readDouble();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        z = _aidl_parcel.readDouble();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        zUncertainty = _aidl_parcel.readDouble();
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
      return _mask;
    }
  }
  public static class GnssAntennaInfo implements android.os.Parcelable
  {
    public long carrierFrequencyHz = 0L;
    public android.hardware.gnss.IGnssAntennaInfoCallback.Coord phaseCenterOffsetCoordinateMillimeters;
    public android.hardware.gnss.IGnssAntennaInfoCallback.Row[] phaseCenterVariationCorrectionMillimeters;
    public android.hardware.gnss.IGnssAntennaInfoCallback.Row[] phaseCenterVariationCorrectionUncertaintyMillimeters;
    public android.hardware.gnss.IGnssAntennaInfoCallback.Row[] signalGainCorrectionDbi;
    public android.hardware.gnss.IGnssAntennaInfoCallback.Row[] signalGainCorrectionUncertaintyDbi;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<GnssAntennaInfo> CREATOR = new android.os.Parcelable.Creator<GnssAntennaInfo>() {
      @Override
      public GnssAntennaInfo createFromParcel(android.os.Parcel _aidl_source) {
        GnssAntennaInfo _aidl_out = new GnssAntennaInfo();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public GnssAntennaInfo[] newArray(int _aidl_size) {
        return new GnssAntennaInfo[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeLong(carrierFrequencyHz);
      _aidl_parcel.writeTypedObject(phaseCenterOffsetCoordinateMillimeters, _aidl_flag);
      _aidl_parcel.writeTypedArray(phaseCenterVariationCorrectionMillimeters, _aidl_flag);
      _aidl_parcel.writeTypedArray(phaseCenterVariationCorrectionUncertaintyMillimeters, _aidl_flag);
      _aidl_parcel.writeTypedArray(signalGainCorrectionDbi, _aidl_flag);
      _aidl_parcel.writeTypedArray(signalGainCorrectionUncertaintyDbi, _aidl_flag);
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
        carrierFrequencyHz = _aidl_parcel.readLong();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        phaseCenterOffsetCoordinateMillimeters = _aidl_parcel.readTypedObject(android.hardware.gnss.IGnssAntennaInfoCallback.Coord.CREATOR);
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        phaseCenterVariationCorrectionMillimeters = _aidl_parcel.createTypedArray(android.hardware.gnss.IGnssAntennaInfoCallback.Row.CREATOR);
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        phaseCenterVariationCorrectionUncertaintyMillimeters = _aidl_parcel.createTypedArray(android.hardware.gnss.IGnssAntennaInfoCallback.Row.CREATOR);
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        signalGainCorrectionDbi = _aidl_parcel.createTypedArray(android.hardware.gnss.IGnssAntennaInfoCallback.Row.CREATOR);
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        signalGainCorrectionUncertaintyDbi = _aidl_parcel.createTypedArray(android.hardware.gnss.IGnssAntennaInfoCallback.Row.CREATOR);
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
      _mask |= describeContents(phaseCenterOffsetCoordinateMillimeters);
      _mask |= describeContents(phaseCenterVariationCorrectionMillimeters);
      _mask |= describeContents(phaseCenterVariationCorrectionUncertaintyMillimeters);
      _mask |= describeContents(signalGainCorrectionDbi);
      _mask |= describeContents(signalGainCorrectionUncertaintyDbi);
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
}
