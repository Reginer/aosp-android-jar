/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.se.omapi;
/** @hide */
public interface ISecureElementService extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 1;
  public static final String HASH = "894069bcfe4f35ceb2088278ddf87c83adee8014";
  /** Default implementation for ISecureElementService. */
  public static class Default implements android.se.omapi.ISecureElementService
  {
    @Override public java.lang.String[] getReaders() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.se.omapi.ISecureElementReader getReader(java.lang.String reader) throws android.os.RemoteException
    {
      return null;
    }
    @Override public boolean[] isNfcEventAllowed(java.lang.String reader, byte[] aid, java.lang.String[] packageNames, int userId) throws android.os.RemoteException
    {
      return null;
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
  public static abstract class Stub extends android.os.Binder implements android.se.omapi.ISecureElementService
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.se.omapi.ISecureElementService interface,
     * generating a proxy if needed.
     */
    public static android.se.omapi.ISecureElementService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.se.omapi.ISecureElementService))) {
        return ((android.se.omapi.ISecureElementService)iin);
      }
      return new android.se.omapi.ISecureElementService.Stub.Proxy(obj);
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
        case TRANSACTION_getReaders:
        {
          java.lang.String[] _result = this.getReaders();
          reply.writeNoException();
          reply.writeStringArray(_result);
          break;
        }
        case TRANSACTION_getReader:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          data.enforceNoDataAvail();
          android.se.omapi.ISecureElementReader _result = this.getReader(_arg0);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_isNfcEventAllowed:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          byte[] _arg1;
          _arg1 = data.createByteArray();
          java.lang.String[] _arg2;
          _arg2 = data.createStringArray();
          int _arg3;
          _arg3 = data.readInt();
          data.enforceNoDataAvail();
          boolean[] _result = this.isNfcEventAllowed(_arg0, _arg1, _arg2, _arg3);
          reply.writeNoException();
          reply.writeBooleanArray(_result);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.se.omapi.ISecureElementService
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
      @Override public java.lang.String[] getReaders() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getReaders, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getReaders is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createStringArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.se.omapi.ISecureElementReader getReader(java.lang.String reader) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.se.omapi.ISecureElementReader _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(reader);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getReader, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getReader is unimplemented.");
          }
          _reply.readException();
          _result = android.se.omapi.ISecureElementReader.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean[] isNfcEventAllowed(java.lang.String reader, byte[] aid, java.lang.String[] packageNames, int userId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(reader);
          _data.writeByteArray(aid);
          _data.writeStringArray(packageNames);
          _data.writeInt(userId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isNfcEventAllowed, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method isNfcEventAllowed is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createBooleanArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
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
    public static final java.lang.String DESCRIPTOR = "android$se$omapi$ISecureElementService".replace('$', '.');
    static final int TRANSACTION_getReaders = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_getReader = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_isNfcEventAllowed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public java.lang.String[] getReaders() throws android.os.RemoteException;
  public android.se.omapi.ISecureElementReader getReader(java.lang.String reader) throws android.os.RemoteException;
  public boolean[] isNfcEventAllowed(java.lang.String reader, byte[] aid, java.lang.String[] packageNames, int userId) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
