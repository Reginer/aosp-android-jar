/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash df80fdbb6f95a8a2988bc72b7f08f891847b80eb --stability vintf --min_sdk_version current --ninja -d out/soong/.intermediates/hardware/interfaces/contexthub/aidl/android.hardware.contexthub-V4-java-source/gen/android/hardware/contexthub/IEndpointCommunication.java.d -o out/soong/.intermediates/hardware/interfaces/contexthub/aidl/android.hardware.contexthub-V4-java-source/gen -Nhardware/interfaces/contexthub/aidl/aidl_api/android.hardware.contexthub/4 hardware/interfaces/contexthub/aidl/aidl_api/android.hardware.contexthub/4/android/hardware/contexthub/IEndpointCommunication.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.contexthub;
public interface IEndpointCommunication extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 4;
  public static final String HASH = "df80fdbb6f95a8a2988bc72b7f08f891847b80eb";
  /** Default implementation for IEndpointCommunication. */
  public static class Default implements android.hardware.contexthub.IEndpointCommunication
  {
    @Override public void registerEndpoint(android.hardware.contexthub.EndpointInfo endpoint) throws android.os.RemoteException
    {
    }
    @Override public void unregisterEndpoint(android.hardware.contexthub.EndpointInfo endpoint) throws android.os.RemoteException
    {
    }
    @Override public int[] requestSessionIdRange(int size) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void openEndpointSession(int sessionId, android.hardware.contexthub.EndpointId destination, android.hardware.contexthub.EndpointId initiator, java.lang.String serviceDescriptor) throws android.os.RemoteException
    {
    }
    @Override public void sendMessageToEndpoint(int sessionId, android.hardware.contexthub.Message msg) throws android.os.RemoteException
    {
    }
    @Override public void sendMessageDeliveryStatusToEndpoint(int sessionId, android.hardware.contexthub.MessageDeliveryStatus msgStatus) throws android.os.RemoteException
    {
    }
    @Override public void closeEndpointSession(int sessionId, byte reason) throws android.os.RemoteException
    {
    }
    @Override public void endpointSessionOpenComplete(int sessionId) throws android.os.RemoteException
    {
    }
    @Override public void unregister() throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.contexthub.IEndpointCommunication
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.contexthub.IEndpointCommunication interface,
     * generating a proxy if needed.
     */
    public static android.hardware.contexthub.IEndpointCommunication asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.contexthub.IEndpointCommunication))) {
        return ((android.hardware.contexthub.IEndpointCommunication)iin);
      }
      return new android.hardware.contexthub.IEndpointCommunication.Stub.Proxy(obj);
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
        case TRANSACTION_registerEndpoint:
        {
          android.hardware.contexthub.EndpointInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.contexthub.EndpointInfo.CREATOR);
          data.enforceNoDataAvail();
          this.registerEndpoint(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_unregisterEndpoint:
        {
          android.hardware.contexthub.EndpointInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.contexthub.EndpointInfo.CREATOR);
          data.enforceNoDataAvail();
          this.unregisterEndpoint(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_requestSessionIdRange:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          int[] _result = this.requestSessionIdRange(_arg0);
          reply.writeNoException();
          reply.writeFixedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE, 2);
          break;
        }
        case TRANSACTION_openEndpointSession:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.contexthub.EndpointId _arg1;
          _arg1 = data.readTypedObject(android.hardware.contexthub.EndpointId.CREATOR);
          android.hardware.contexthub.EndpointId _arg2;
          _arg2 = data.readTypedObject(android.hardware.contexthub.EndpointId.CREATOR);
          java.lang.String _arg3;
          _arg3 = data.readString();
          data.enforceNoDataAvail();
          this.openEndpointSession(_arg0, _arg1, _arg2, _arg3);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_sendMessageToEndpoint:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.contexthub.Message _arg1;
          _arg1 = data.readTypedObject(android.hardware.contexthub.Message.CREATOR);
          data.enforceNoDataAvail();
          this.sendMessageToEndpoint(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_sendMessageDeliveryStatusToEndpoint:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.contexthub.MessageDeliveryStatus _arg1;
          _arg1 = data.readTypedObject(android.hardware.contexthub.MessageDeliveryStatus.CREATOR);
          data.enforceNoDataAvail();
          this.sendMessageDeliveryStatusToEndpoint(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_closeEndpointSession:
        {
          int _arg0;
          _arg0 = data.readInt();
          byte _arg1;
          _arg1 = data.readByte();
          data.enforceNoDataAvail();
          this.closeEndpointSession(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_endpointSessionOpenComplete:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.endpointSessionOpenComplete(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_unregister:
        {
          this.unregister();
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
    private static class Proxy implements android.hardware.contexthub.IEndpointCommunication
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
      @Override public void registerEndpoint(android.hardware.contexthub.EndpointInfo endpoint) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(endpoint, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerEndpoint, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method registerEndpoint is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void unregisterEndpoint(android.hardware.contexthub.EndpointInfo endpoint) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(endpoint, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterEndpoint, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method unregisterEndpoint is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public int[] requestSessionIdRange(int size) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(size);
          boolean _status = mRemote.transact(Stub.TRANSACTION_requestSessionIdRange, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method requestSessionIdRange is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createFixedArray(int[].class, 2);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void openEndpointSession(int sessionId, android.hardware.contexthub.EndpointId destination, android.hardware.contexthub.EndpointId initiator, java.lang.String serviceDescriptor) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sessionId);
          _data.writeTypedObject(destination, 0);
          _data.writeTypedObject(initiator, 0);
          _data.writeString(serviceDescriptor);
          boolean _status = mRemote.transact(Stub.TRANSACTION_openEndpointSession, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method openEndpointSession is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void sendMessageToEndpoint(int sessionId, android.hardware.contexthub.Message msg) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sessionId);
          _data.writeTypedObject(msg, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendMessageToEndpoint, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method sendMessageToEndpoint is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void sendMessageDeliveryStatusToEndpoint(int sessionId, android.hardware.contexthub.MessageDeliveryStatus msgStatus) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sessionId);
          _data.writeTypedObject(msgStatus, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendMessageDeliveryStatusToEndpoint, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method sendMessageDeliveryStatusToEndpoint is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void closeEndpointSession(int sessionId, byte reason) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sessionId);
          _data.writeByte(reason);
          boolean _status = mRemote.transact(Stub.TRANSACTION_closeEndpointSession, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method closeEndpointSession is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void endpointSessionOpenComplete(int sessionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sessionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_endpointSessionOpenComplete, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method endpointSessionOpenComplete is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void unregister() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregister, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method unregister is unimplemented.");
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
    static final int TRANSACTION_registerEndpoint = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_unregisterEndpoint = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_requestSessionIdRange = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_openEndpointSession = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_sendMessageToEndpoint = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_sendMessageDeliveryStatusToEndpoint = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_closeEndpointSession = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_endpointSessionOpenComplete = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_unregister = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android$hardware$contexthub$IEndpointCommunication".replace('$', '.');
  public void registerEndpoint(android.hardware.contexthub.EndpointInfo endpoint) throws android.os.RemoteException;
  public void unregisterEndpoint(android.hardware.contexthub.EndpointInfo endpoint) throws android.os.RemoteException;
  public int[] requestSessionIdRange(int size) throws android.os.RemoteException;
  public void openEndpointSession(int sessionId, android.hardware.contexthub.EndpointId destination, android.hardware.contexthub.EndpointId initiator, java.lang.String serviceDescriptor) throws android.os.RemoteException;
  public void sendMessageToEndpoint(int sessionId, android.hardware.contexthub.Message msg) throws android.os.RemoteException;
  public void sendMessageDeliveryStatusToEndpoint(int sessionId, android.hardware.contexthub.MessageDeliveryStatus msgStatus) throws android.os.RemoteException;
  public void closeEndpointSession(int sessionId, byte reason) throws android.os.RemoteException;
  public void endpointSessionOpenComplete(int sessionId) throws android.os.RemoteException;
  public void unregister() throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
