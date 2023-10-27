/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.security.keymint;
/** @hide */
public interface IKeyMintOperation extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 3;
  public static final String HASH = "74a538630d5d90f732f361a2313cbb69b09eb047";
  /** Default implementation for IKeyMintOperation. */
  public static class Default implements android.hardware.security.keymint.IKeyMintOperation
  {
    @Override public void updateAad(byte[] input, android.hardware.security.keymint.HardwareAuthToken authToken, android.hardware.security.secureclock.TimeStampToken timeStampToken) throws android.os.RemoteException
    {
    }
    @Override public byte[] update(byte[] input, android.hardware.security.keymint.HardwareAuthToken authToken, android.hardware.security.secureclock.TimeStampToken timeStampToken) throws android.os.RemoteException
    {
      return null;
    }
    @Override public byte[] finish(byte[] input, byte[] signature, android.hardware.security.keymint.HardwareAuthToken authToken, android.hardware.security.secureclock.TimeStampToken timestampToken, byte[] confirmationToken) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void abort() throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.security.keymint.IKeyMintOperation
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.security.keymint.IKeyMintOperation interface,
     * generating a proxy if needed.
     */
    public static android.hardware.security.keymint.IKeyMintOperation asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.security.keymint.IKeyMintOperation))) {
        return ((android.hardware.security.keymint.IKeyMintOperation)iin);
      }
      return new android.hardware.security.keymint.IKeyMintOperation.Stub.Proxy(obj);
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
        case TRANSACTION_updateAad:
        {
          return "updateAad";
        }
        case TRANSACTION_update:
        {
          return "update";
        }
        case TRANSACTION_finish:
        {
          return "finish";
        }
        case TRANSACTION_abort:
        {
          return "abort";
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
        case TRANSACTION_updateAad:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          android.hardware.security.keymint.HardwareAuthToken _arg1;
          _arg1 = data.readTypedObject(android.hardware.security.keymint.HardwareAuthToken.CREATOR);
          android.hardware.security.secureclock.TimeStampToken _arg2;
          _arg2 = data.readTypedObject(android.hardware.security.secureclock.TimeStampToken.CREATOR);
          data.enforceNoDataAvail();
          this.updateAad(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_update:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          android.hardware.security.keymint.HardwareAuthToken _arg1;
          _arg1 = data.readTypedObject(android.hardware.security.keymint.HardwareAuthToken.CREATOR);
          android.hardware.security.secureclock.TimeStampToken _arg2;
          _arg2 = data.readTypedObject(android.hardware.security.secureclock.TimeStampToken.CREATOR);
          data.enforceNoDataAvail();
          byte[] _result = this.update(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeByteArray(_result);
          break;
        }
        case TRANSACTION_finish:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          byte[] _arg1;
          _arg1 = data.createByteArray();
          android.hardware.security.keymint.HardwareAuthToken _arg2;
          _arg2 = data.readTypedObject(android.hardware.security.keymint.HardwareAuthToken.CREATOR);
          android.hardware.security.secureclock.TimeStampToken _arg3;
          _arg3 = data.readTypedObject(android.hardware.security.secureclock.TimeStampToken.CREATOR);
          byte[] _arg4;
          _arg4 = data.createByteArray();
          data.enforceNoDataAvail();
          byte[] _result = this.finish(_arg0, _arg1, _arg2, _arg3, _arg4);
          reply.writeNoException();
          reply.writeByteArray(_result);
          break;
        }
        case TRANSACTION_abort:
        {
          this.abort();
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
    private static class Proxy implements android.hardware.security.keymint.IKeyMintOperation
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
      @Override public void updateAad(byte[] input, android.hardware.security.keymint.HardwareAuthToken authToken, android.hardware.security.secureclock.TimeStampToken timeStampToken) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(input);
          _data.writeTypedObject(authToken, 0);
          _data.writeTypedObject(timeStampToken, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_updateAad, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method updateAad is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public byte[] update(byte[] input, android.hardware.security.keymint.HardwareAuthToken authToken, android.hardware.security.secureclock.TimeStampToken timeStampToken) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        byte[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(input);
          _data.writeTypedObject(authToken, 0);
          _data.writeTypedObject(timeStampToken, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_update, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method update is unimplemented.");
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
      @Override public byte[] finish(byte[] input, byte[] signature, android.hardware.security.keymint.HardwareAuthToken authToken, android.hardware.security.secureclock.TimeStampToken timestampToken, byte[] confirmationToken) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        byte[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(input);
          _data.writeByteArray(signature);
          _data.writeTypedObject(authToken, 0);
          _data.writeTypedObject(timestampToken, 0);
          _data.writeByteArray(confirmationToken);
          boolean _status = mRemote.transact(Stub.TRANSACTION_finish, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method finish is unimplemented.");
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
      @Override public void abort() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_abort, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method abort is unimplemented.");
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
    static final int TRANSACTION_updateAad = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_update = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_finish = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_abort = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$security$keymint$IKeyMintOperation".replace('$', '.');
  public void updateAad(byte[] input, android.hardware.security.keymint.HardwareAuthToken authToken, android.hardware.security.secureclock.TimeStampToken timeStampToken) throws android.os.RemoteException;
  public byte[] update(byte[] input, android.hardware.security.keymint.HardwareAuthToken authToken, android.hardware.security.secureclock.TimeStampToken timeStampToken) throws android.os.RemoteException;
  public byte[] finish(byte[] input, byte[] signature, android.hardware.security.keymint.HardwareAuthToken authToken, android.hardware.security.secureclock.TimeStampToken timestampToken, byte[] confirmationToken) throws android.os.RemoteException;
  public void abort() throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
