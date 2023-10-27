/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.contexthub;
public interface IContextHubCallback extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "b0fd976b134e549e03726d3ebeeae848e520d3d3";
  /** Default implementation for IContextHubCallback. */
  public static class Default implements android.hardware.contexthub.IContextHubCallback
  {
    @Override public void handleNanoappInfo(android.hardware.contexthub.NanoappInfo[] appInfo) throws android.os.RemoteException
    {
    }
    @Override public void handleContextHubMessage(android.hardware.contexthub.ContextHubMessage msg, java.lang.String[] msgContentPerms) throws android.os.RemoteException
    {
    }
    @Override public void handleContextHubAsyncEvent(int evt) throws android.os.RemoteException
    {
    }
    @Override public void handleTransactionResult(int transactionId, boolean success) throws android.os.RemoteException
    {
    }
    @Override public void handleNanSessionRequest(android.hardware.contexthub.NanSessionRequest request) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.contexthub.IContextHubCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.contexthub.IContextHubCallback interface,
     * generating a proxy if needed.
     */
    public static android.hardware.contexthub.IContextHubCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.contexthub.IContextHubCallback))) {
        return ((android.hardware.contexthub.IContextHubCallback)iin);
      }
      return new android.hardware.contexthub.IContextHubCallback.Stub.Proxy(obj);
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
        case TRANSACTION_handleNanoappInfo:
        {
          android.hardware.contexthub.NanoappInfo[] _arg0;
          _arg0 = data.createTypedArray(android.hardware.contexthub.NanoappInfo.CREATOR);
          data.enforceNoDataAvail();
          this.handleNanoappInfo(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_handleContextHubMessage:
        {
          android.hardware.contexthub.ContextHubMessage _arg0;
          _arg0 = data.readTypedObject(android.hardware.contexthub.ContextHubMessage.CREATOR);
          java.lang.String[] _arg1;
          _arg1 = data.createStringArray();
          data.enforceNoDataAvail();
          this.handleContextHubMessage(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_handleContextHubAsyncEvent:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.handleContextHubAsyncEvent(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_handleTransactionResult:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.handleTransactionResult(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_handleNanSessionRequest:
        {
          android.hardware.contexthub.NanSessionRequest _arg0;
          _arg0 = data.readTypedObject(android.hardware.contexthub.NanSessionRequest.CREATOR);
          data.enforceNoDataAvail();
          this.handleNanSessionRequest(_arg0);
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
    private static class Proxy implements android.hardware.contexthub.IContextHubCallback
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
      @Override public void handleNanoappInfo(android.hardware.contexthub.NanoappInfo[] appInfo) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedArray(appInfo, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_handleNanoappInfo, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method handleNanoappInfo is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void handleContextHubMessage(android.hardware.contexthub.ContextHubMessage msg, java.lang.String[] msgContentPerms) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(msg, 0);
          _data.writeStringArray(msgContentPerms);
          boolean _status = mRemote.transact(Stub.TRANSACTION_handleContextHubMessage, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method handleContextHubMessage is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void handleContextHubAsyncEvent(int evt) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(evt);
          boolean _status = mRemote.transact(Stub.TRANSACTION_handleContextHubAsyncEvent, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method handleContextHubAsyncEvent is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void handleTransactionResult(int transactionId, boolean success) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(transactionId);
          _data.writeBoolean(success);
          boolean _status = mRemote.transact(Stub.TRANSACTION_handleTransactionResult, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method handleTransactionResult is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void handleNanSessionRequest(android.hardware.contexthub.NanSessionRequest request) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(request, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_handleNanSessionRequest, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method handleNanSessionRequest is unimplemented.");
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
    static final int TRANSACTION_handleNanoappInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_handleContextHubMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_handleContextHubAsyncEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_handleTransactionResult = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_handleNanSessionRequest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$contexthub$IContextHubCallback".replace('$', '.');
  public static final int CONTEXTHUB_NAN_TRANSACTION_TIMEOUT_MS = 10000;
  public void handleNanoappInfo(android.hardware.contexthub.NanoappInfo[] appInfo) throws android.os.RemoteException;
  public void handleContextHubMessage(android.hardware.contexthub.ContextHubMessage msg, java.lang.String[] msgContentPerms) throws android.os.RemoteException;
  public void handleContextHubAsyncEvent(int evt) throws android.os.RemoteException;
  public void handleTransactionResult(int transactionId, boolean success) throws android.os.RemoteException;
  public void handleNanSessionRequest(android.hardware.contexthub.NanSessionRequest request) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
