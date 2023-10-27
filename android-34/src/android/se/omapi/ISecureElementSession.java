/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.se.omapi;
/** @hide */
public interface ISecureElementSession extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 1;
  public static final String HASH = "894069bcfe4f35ceb2088278ddf87c83adee8014";
  /** Default implementation for ISecureElementSession. */
  public static class Default implements android.se.omapi.ISecureElementSession
  {
    @Override public byte[] getAtr() throws android.os.RemoteException
    {
      return null;
    }
    @Override public void close() throws android.os.RemoteException
    {
    }
    @Override public void closeChannels() throws android.os.RemoteException
    {
    }
    @Override public boolean isClosed() throws android.os.RemoteException
    {
      return false;
    }
    @Override public android.se.omapi.ISecureElementChannel openBasicChannel(byte[] aid, byte p2, android.se.omapi.ISecureElementListener listener) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.se.omapi.ISecureElementChannel openLogicalChannel(byte[] aid, byte p2, android.se.omapi.ISecureElementListener listener) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.se.omapi.ISecureElementSession
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.se.omapi.ISecureElementSession interface,
     * generating a proxy if needed.
     */
    public static android.se.omapi.ISecureElementSession asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.se.omapi.ISecureElementSession))) {
        return ((android.se.omapi.ISecureElementSession)iin);
      }
      return new android.se.omapi.ISecureElementSession.Stub.Proxy(obj);
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
        case TRANSACTION_getAtr:
        {
          byte[] _result = this.getAtr();
          reply.writeNoException();
          reply.writeByteArray(_result);
          break;
        }
        case TRANSACTION_close:
        {
          this.close();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_closeChannels:
        {
          this.closeChannels();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_isClosed:
        {
          boolean _result = this.isClosed();
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_openBasicChannel:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          byte _arg1;
          _arg1 = data.readByte();
          android.se.omapi.ISecureElementListener _arg2;
          _arg2 = android.se.omapi.ISecureElementListener.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          android.se.omapi.ISecureElementChannel _result = this.openBasicChannel(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_openLogicalChannel:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          byte _arg1;
          _arg1 = data.readByte();
          android.se.omapi.ISecureElementListener _arg2;
          _arg2 = android.se.omapi.ISecureElementListener.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          android.se.omapi.ISecureElementChannel _result = this.openLogicalChannel(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.se.omapi.ISecureElementSession
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
      @Override public byte[] getAtr() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        byte[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getAtr, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getAtr is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createByteArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void close() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_close, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method close is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void closeChannels() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_closeChannels, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method closeChannels is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public boolean isClosed() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isClosed, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method isClosed is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readBoolean();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.se.omapi.ISecureElementChannel openBasicChannel(byte[] aid, byte p2, android.se.omapi.ISecureElementListener listener) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.se.omapi.ISecureElementChannel _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(aid);
          _data.writeByte(p2);
          _data.writeStrongInterface(listener);
          boolean _status = mRemote.transact(Stub.TRANSACTION_openBasicChannel, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method openBasicChannel is unimplemented.");
          }
          _reply.readException();
          _result = android.se.omapi.ISecureElementChannel.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.se.omapi.ISecureElementChannel openLogicalChannel(byte[] aid, byte p2, android.se.omapi.ISecureElementListener listener) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.se.omapi.ISecureElementChannel _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(aid);
          _data.writeByte(p2);
          _data.writeStrongInterface(listener);
          boolean _status = mRemote.transact(Stub.TRANSACTION_openLogicalChannel, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method openLogicalChannel is unimplemented.");
          }
          _reply.readException();
          _result = android.se.omapi.ISecureElementChannel.Stub.asInterface(_reply.readStrongBinder());
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
    public static final java.lang.String DESCRIPTOR = "android$se$omapi$ISecureElementSession".replace('$', '.');
    static final int TRANSACTION_getAtr = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_close = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_closeChannels = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_isClosed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_openBasicChannel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_openLogicalChannel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public byte[] getAtr() throws android.os.RemoteException;
  public void close() throws android.os.RemoteException;
  public void closeChannels() throws android.os.RemoteException;
  public boolean isClosed() throws android.os.RemoteException;
  public android.se.omapi.ISecureElementChannel openBasicChannel(byte[] aid, byte p2, android.se.omapi.ISecureElementListener listener) throws android.os.RemoteException;
  public android.se.omapi.ISecureElementChannel openLogicalChannel(byte[] aid, byte p2, android.se.omapi.ISecureElementListener listener) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
