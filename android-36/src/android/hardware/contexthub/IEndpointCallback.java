/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash df80fdbb6f95a8a2988bc72b7f08f891847b80eb --stability vintf --min_sdk_version current --ninja -d out/soong/.intermediates/hardware/interfaces/contexthub/aidl/android.hardware.contexthub-V4-java-source/gen/android/hardware/contexthub/IEndpointCallback.java.d -o out/soong/.intermediates/hardware/interfaces/contexthub/aidl/android.hardware.contexthub-V4-java-source/gen -Nhardware/interfaces/contexthub/aidl/aidl_api/android.hardware.contexthub/4 hardware/interfaces/contexthub/aidl/aidl_api/android.hardware.contexthub/4/android/hardware/contexthub/IEndpointCallback.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.contexthub;
public interface IEndpointCallback extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 4;
  public static final String HASH = "df80fdbb6f95a8a2988bc72b7f08f891847b80eb";
  /** Default implementation for IEndpointCallback. */
  public static class Default implements android.hardware.contexthub.IEndpointCallback
  {
    @Override public void onEndpointStarted(android.hardware.contexthub.EndpointInfo[] endpointInfos) throws android.os.RemoteException
    {
    }
    @Override public void onEndpointStopped(android.hardware.contexthub.EndpointId[] endpointIds, byte reason) throws android.os.RemoteException
    {
    }
    @Override public void onMessageReceived(int sessionId, android.hardware.contexthub.Message msg) throws android.os.RemoteException
    {
    }
    @Override public void onMessageDeliveryStatusReceived(int sessionId, android.hardware.contexthub.MessageDeliveryStatus msgStatus) throws android.os.RemoteException
    {
    }
    @Override public void onEndpointSessionOpenRequest(int sessionId, android.hardware.contexthub.EndpointId destination, android.hardware.contexthub.EndpointId initiator, java.lang.String serviceDescriptor) throws android.os.RemoteException
    {
    }
    @Override public void onCloseEndpointSession(int sessionId, byte reason) throws android.os.RemoteException
    {
    }
    @Override public void onEndpointSessionOpenComplete(int sessionId) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.contexthub.IEndpointCallback
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.contexthub.IEndpointCallback interface,
     * generating a proxy if needed.
     */
    public static android.hardware.contexthub.IEndpointCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.contexthub.IEndpointCallback))) {
        return ((android.hardware.contexthub.IEndpointCallback)iin);
      }
      return new android.hardware.contexthub.IEndpointCallback.Stub.Proxy(obj);
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
        case TRANSACTION_onEndpointStarted:
        {
          android.hardware.contexthub.EndpointInfo[] _arg0;
          _arg0 = data.createTypedArray(android.hardware.contexthub.EndpointInfo.CREATOR);
          data.enforceNoDataAvail();
          this.onEndpointStarted(_arg0);
          break;
        }
        case TRANSACTION_onEndpointStopped:
        {
          android.hardware.contexthub.EndpointId[] _arg0;
          _arg0 = data.createTypedArray(android.hardware.contexthub.EndpointId.CREATOR);
          byte _arg1;
          _arg1 = data.readByte();
          data.enforceNoDataAvail();
          this.onEndpointStopped(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onMessageReceived:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.contexthub.Message _arg1;
          _arg1 = data.readTypedObject(android.hardware.contexthub.Message.CREATOR);
          data.enforceNoDataAvail();
          this.onMessageReceived(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onMessageDeliveryStatusReceived:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.contexthub.MessageDeliveryStatus _arg1;
          _arg1 = data.readTypedObject(android.hardware.contexthub.MessageDeliveryStatus.CREATOR);
          data.enforceNoDataAvail();
          this.onMessageDeliveryStatusReceived(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onEndpointSessionOpenRequest:
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
          this.onEndpointSessionOpenRequest(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_onCloseEndpointSession:
        {
          int _arg0;
          _arg0 = data.readInt();
          byte _arg1;
          _arg1 = data.readByte();
          data.enforceNoDataAvail();
          this.onCloseEndpointSession(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onEndpointSessionOpenComplete:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.onEndpointSessionOpenComplete(_arg0);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.contexthub.IEndpointCallback
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
      @Override public void onEndpointStarted(android.hardware.contexthub.EndpointInfo[] endpointInfos) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedArray(endpointInfos, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onEndpointStarted, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method onEndpointStarted is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onEndpointStopped(android.hardware.contexthub.EndpointId[] endpointIds, byte reason) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedArray(endpointIds, 0);
          _data.writeByte(reason);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onEndpointStopped, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method onEndpointStopped is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onMessageReceived(int sessionId, android.hardware.contexthub.Message msg) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sessionId);
          _data.writeTypedObject(msg, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onMessageReceived, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method onMessageReceived is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onMessageDeliveryStatusReceived(int sessionId, android.hardware.contexthub.MessageDeliveryStatus msgStatus) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sessionId);
          _data.writeTypedObject(msgStatus, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onMessageDeliveryStatusReceived, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method onMessageDeliveryStatusReceived is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onEndpointSessionOpenRequest(int sessionId, android.hardware.contexthub.EndpointId destination, android.hardware.contexthub.EndpointId initiator, java.lang.String serviceDescriptor) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sessionId);
          _data.writeTypedObject(destination, 0);
          _data.writeTypedObject(initiator, 0);
          _data.writeString(serviceDescriptor);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onEndpointSessionOpenRequest, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method onEndpointSessionOpenRequest is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onCloseEndpointSession(int sessionId, byte reason) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sessionId);
          _data.writeByte(reason);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onCloseEndpointSession, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method onCloseEndpointSession is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onEndpointSessionOpenComplete(int sessionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sessionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onEndpointSessionOpenComplete, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method onEndpointSessionOpenComplete is unimplemented.");
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
    static final int TRANSACTION_onEndpointStarted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onEndpointStopped = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_onMessageReceived = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_onMessageDeliveryStatusReceived = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_onEndpointSessionOpenRequest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_onCloseEndpointSession = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_onEndpointSessionOpenComplete = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android$hardware$contexthub$IEndpointCallback".replace('$', '.');
  public void onEndpointStarted(android.hardware.contexthub.EndpointInfo[] endpointInfos) throws android.os.RemoteException;
  public void onEndpointStopped(android.hardware.contexthub.EndpointId[] endpointIds, byte reason) throws android.os.RemoteException;
  public void onMessageReceived(int sessionId, android.hardware.contexthub.Message msg) throws android.os.RemoteException;
  public void onMessageDeliveryStatusReceived(int sessionId, android.hardware.contexthub.MessageDeliveryStatus msgStatus) throws android.os.RemoteException;
  public void onEndpointSessionOpenRequest(int sessionId, android.hardware.contexthub.EndpointId destination, android.hardware.contexthub.EndpointId initiator, java.lang.String serviceDescriptor) throws android.os.RemoteException;
  public void onCloseEndpointSession(int sessionId, byte reason) throws android.os.RemoteException;
  public void onEndpointSessionOpenComplete(int sessionId) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
