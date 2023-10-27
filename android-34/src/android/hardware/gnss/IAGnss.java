/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.gnss;
/** @hide */
public interface IAGnss extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "fc957f1d3d261d065ff5e5415f2d21caa79c310f";
  /** Default implementation for IAGnss. */
  public static class Default implements android.hardware.gnss.IAGnss
  {
    @Override public void setCallback(android.hardware.gnss.IAGnssCallback callback) throws android.os.RemoteException
    {
    }
    @Override public void dataConnClosed() throws android.os.RemoteException
    {
    }
    @Override public void dataConnFailed() throws android.os.RemoteException
    {
    }
    @Override public void setServer(int type, java.lang.String hostname, int port) throws android.os.RemoteException
    {
    }
    @Override public void dataConnOpen(long networkHandle, java.lang.String apn, int apnIpType) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.gnss.IAGnss
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.gnss.IAGnss interface,
     * generating a proxy if needed.
     */
    public static android.hardware.gnss.IAGnss asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.gnss.IAGnss))) {
        return ((android.hardware.gnss.IAGnss)iin);
      }
      return new android.hardware.gnss.IAGnss.Stub.Proxy(obj);
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
        case TRANSACTION_setCallback:
        {
          return "setCallback";
        }
        case TRANSACTION_dataConnClosed:
        {
          return "dataConnClosed";
        }
        case TRANSACTION_dataConnFailed:
        {
          return "dataConnFailed";
        }
        case TRANSACTION_setServer:
        {
          return "setServer";
        }
        case TRANSACTION_dataConnOpen:
        {
          return "dataConnOpen";
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
        case TRANSACTION_setCallback:
        {
          android.hardware.gnss.IAGnssCallback _arg0;
          _arg0 = android.hardware.gnss.IAGnssCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.setCallback(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_dataConnClosed:
        {
          this.dataConnClosed();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_dataConnFailed:
        {
          this.dataConnFailed();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setServer:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.setServer(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_dataConnOpen:
        {
          long _arg0;
          _arg0 = data.readLong();
          java.lang.String _arg1;
          _arg1 = data.readString();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.dataConnOpen(_arg0, _arg1, _arg2);
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
    private static class Proxy implements android.hardware.gnss.IAGnss
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
      @Override public void setCallback(android.hardware.gnss.IAGnssCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCallback, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setCallback is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void dataConnClosed() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_dataConnClosed, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method dataConnClosed is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void dataConnFailed() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_dataConnFailed, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method dataConnFailed is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setServer(int type, java.lang.String hostname, int port) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeString(hostname);
          _data.writeInt(port);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setServer, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setServer is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void dataConnOpen(long networkHandle, java.lang.String apn, int apnIpType) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(networkHandle);
          _data.writeString(apn);
          _data.writeInt(apnIpType);
          boolean _status = mRemote.transact(Stub.TRANSACTION_dataConnOpen, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method dataConnOpen is unimplemented.");
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
    static final int TRANSACTION_setCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_dataConnClosed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_dataConnFailed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_setServer = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_dataConnOpen = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$gnss$IAGnss".replace('$', '.');
  public void setCallback(android.hardware.gnss.IAGnssCallback callback) throws android.os.RemoteException;
  public void dataConnClosed() throws android.os.RemoteException;
  public void dataConnFailed() throws android.os.RemoteException;
  public void setServer(int type, java.lang.String hostname, int port) throws android.os.RemoteException;
  public void dataConnOpen(long networkHandle, java.lang.String apn, int apnIpType) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
  public static @interface ApnIpType {
    public static final int INVALID = 0;
    public static final int IPV4 = 1;
    public static final int IPV6 = 2;
    public static final int IPV4V6 = 3;
  }
}
