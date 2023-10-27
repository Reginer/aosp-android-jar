/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.data;
public interface IRadioDataIndication extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "cb458326b02e0e87143f24118543e8cc7d6a9e8e";
  /** Default implementation for IRadioDataIndication. */
  public static class Default implements android.hardware.radio.data.IRadioDataIndication
  {
    @Override public void dataCallListChanged(int type, android.hardware.radio.data.SetupDataCallResult[] dcList) throws android.os.RemoteException
    {
    }
    @Override public void keepaliveStatus(int type, android.hardware.radio.data.KeepaliveStatus status) throws android.os.RemoteException
    {
    }
    @Override public void pcoData(int type, android.hardware.radio.data.PcoDataInfo pco) throws android.os.RemoteException
    {
    }
    @Override public void unthrottleApn(int type, android.hardware.radio.data.DataProfileInfo dataProfileInfo) throws android.os.RemoteException
    {
    }
    @Override public void slicingConfigChanged(int type, android.hardware.radio.data.SlicingConfig slicingConfig) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.radio.data.IRadioDataIndication
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.radio.data.IRadioDataIndication interface,
     * generating a proxy if needed.
     */
    public static android.hardware.radio.data.IRadioDataIndication asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.radio.data.IRadioDataIndication))) {
        return ((android.hardware.radio.data.IRadioDataIndication)iin);
      }
      return new android.hardware.radio.data.IRadioDataIndication.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
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
        case TRANSACTION_dataCallListChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.data.SetupDataCallResult[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.radio.data.SetupDataCallResult.CREATOR);
          data.enforceNoDataAvail();
          this.dataCallListChanged(_arg0, _arg1);
          break;
        }
        case TRANSACTION_keepaliveStatus:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.data.KeepaliveStatus _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.data.KeepaliveStatus.CREATOR);
          data.enforceNoDataAvail();
          this.keepaliveStatus(_arg0, _arg1);
          break;
        }
        case TRANSACTION_pcoData:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.data.PcoDataInfo _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.data.PcoDataInfo.CREATOR);
          data.enforceNoDataAvail();
          this.pcoData(_arg0, _arg1);
          break;
        }
        case TRANSACTION_unthrottleApn:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.data.DataProfileInfo _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.data.DataProfileInfo.CREATOR);
          data.enforceNoDataAvail();
          this.unthrottleApn(_arg0, _arg1);
          break;
        }
        case TRANSACTION_slicingConfigChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.data.SlicingConfig _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.data.SlicingConfig.CREATOR);
          data.enforceNoDataAvail();
          this.slicingConfigChanged(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.radio.data.IRadioDataIndication
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
      @Override public void dataCallListChanged(int type, android.hardware.radio.data.SetupDataCallResult[] dcList) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedArray(dcList, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_dataCallListChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method dataCallListChanged is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void keepaliveStatus(int type, android.hardware.radio.data.KeepaliveStatus status) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedObject(status, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_keepaliveStatus, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method keepaliveStatus is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void pcoData(int type, android.hardware.radio.data.PcoDataInfo pco) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedObject(pco, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_pcoData, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method pcoData is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void unthrottleApn(int type, android.hardware.radio.data.DataProfileInfo dataProfileInfo) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedObject(dataProfileInfo, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unthrottleApn, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method unthrottleApn is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void slicingConfigChanged(int type, android.hardware.radio.data.SlicingConfig slicingConfig) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedObject(slicingConfig, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_slicingConfigChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method slicingConfigChanged is unimplemented.");
          }
        }
        finally {
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
    static final int TRANSACTION_dataCallListChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_keepaliveStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_pcoData = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_unthrottleApn = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_slicingConfigChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$radio$data$IRadioDataIndication".replace('$', '.');
  public void dataCallListChanged(int type, android.hardware.radio.data.SetupDataCallResult[] dcList) throws android.os.RemoteException;
  public void keepaliveStatus(int type, android.hardware.radio.data.KeepaliveStatus status) throws android.os.RemoteException;
  public void pcoData(int type, android.hardware.radio.data.PcoDataInfo pco) throws android.os.RemoteException;
  public void unthrottleApn(int type, android.hardware.radio.data.DataProfileInfo dataProfileInfo) throws android.os.RemoteException;
  public void slicingConfigChanged(int type, android.hardware.radio.data.SlicingConfig slicingConfig) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
