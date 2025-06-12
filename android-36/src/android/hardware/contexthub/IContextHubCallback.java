/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash df80fdbb6f95a8a2988bc72b7f08f891847b80eb --stability vintf --min_sdk_version current --ninja -d out/soong/.intermediates/hardware/interfaces/contexthub/aidl/android.hardware.contexthub-V4-java-source/gen/android/hardware/contexthub/IContextHubCallback.java.d -o out/soong/.intermediates/hardware/interfaces/contexthub/aidl/android.hardware.contexthub-V4-java-source/gen -Nhardware/interfaces/contexthub/aidl/aidl_api/android.hardware.contexthub/4 hardware/interfaces/contexthub/aidl/aidl_api/android.hardware.contexthub/4/android/hardware/contexthub/IContextHubCallback.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
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
  public static final int VERSION = 4;
  public static final String HASH = "df80fdbb6f95a8a2988bc72b7f08f891847b80eb";
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
    @Override public void handleMessageDeliveryStatus(char hostEndpointId, android.hardware.contexthub.MessageDeliveryStatus messageDeliveryStatus) throws android.os.RemoteException
    {
    }
    @Override public byte[] getUuid() throws android.os.RemoteException
    {
      return null;
    }
    @Override public java.lang.String getName() throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.contexthub.IContextHubCallback
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
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
      if (code == INTERFACE_TRANSACTION) {
        reply.writeString(descriptor);
        return true;
      }
      else if (code == TRANSACTION_getInterfaceVersion) {
        reply.writeNoException();
        reply.writeInt(getInterfaceVersion());
        return true;
      }
      else if (code == TRANSACTION_getInterfaceHash) {
        reply.writeNoException();
        reply.writeString(getInterfaceHash());
        return true;
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
        case TRANSACTION_handleMessageDeliveryStatus:
        {
          char _arg0;
          _arg0 = (char)data.readInt();
          android.hardware.contexthub.MessageDeliveryStatus _arg1;
          _arg1 = data.readTypedObject(android.hardware.contexthub.MessageDeliveryStatus.CREATOR);
          data.enforceNoDataAvail();
          this.handleMessageDeliveryStatus(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getUuid:
        {
          byte[] _result = this.getUuid();
          reply.writeNoException();
          reply.writeFixedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE, 16);
          break;
        }
        case TRANSACTION_getName:
        {
          java.lang.String _result = this.getName();
          reply.writeNoException();
          reply.writeString(_result);
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
      @Override public void handleMessageDeliveryStatus(char hostEndpointId, android.hardware.contexthub.MessageDeliveryStatus messageDeliveryStatus) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(((int)hostEndpointId));
          _data.writeTypedObject(messageDeliveryStatus, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_handleMessageDeliveryStatus, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method handleMessageDeliveryStatus is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public byte[] getUuid() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        byte[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getUuid, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getUuid is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createFixedArray(byte[].class, 16);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public java.lang.String getName() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getName, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getName is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readString();
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
    static final int TRANSACTION_handleNanoappInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_handleContextHubMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_handleContextHubAsyncEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_handleTransactionResult = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_handleNanSessionRequest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_handleMessageDeliveryStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getUuid = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android$hardware$contexthub$IContextHubCallback".replace('$', '.');
  public static final int CONTEXTHUB_NAN_TRANSACTION_TIMEOUT_MS = 10000;
  public void handleNanoappInfo(android.hardware.contexthub.NanoappInfo[] appInfo) throws android.os.RemoteException;
  public void handleContextHubMessage(android.hardware.contexthub.ContextHubMessage msg, java.lang.String[] msgContentPerms) throws android.os.RemoteException;
  public void handleContextHubAsyncEvent(int evt) throws android.os.RemoteException;
  public void handleTransactionResult(int transactionId, boolean success) throws android.os.RemoteException;
  public void handleNanSessionRequest(android.hardware.contexthub.NanSessionRequest request) throws android.os.RemoteException;
  public void handleMessageDeliveryStatus(char hostEndpointId, android.hardware.contexthub.MessageDeliveryStatus messageDeliveryStatus) throws android.os.RemoteException;
  public byte[] getUuid() throws android.os.RemoteException;
  public java.lang.String getName() throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
