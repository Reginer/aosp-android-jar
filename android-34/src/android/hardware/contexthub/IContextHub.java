/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.contexthub;
public interface IContextHub extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "b0fd976b134e549e03726d3ebeeae848e520d3d3";
  /** Default implementation for IContextHub. */
  public static class Default implements android.hardware.contexthub.IContextHub
  {
    @Override public java.util.List<android.hardware.contexthub.ContextHubInfo> getContextHubs() throws android.os.RemoteException
    {
      return null;
    }
    @Override public void loadNanoapp(int contextHubId, android.hardware.contexthub.NanoappBinary appBinary, int transactionId) throws android.os.RemoteException
    {
    }
    @Override public void unloadNanoapp(int contextHubId, long appId, int transactionId) throws android.os.RemoteException
    {
    }
    @Override public void disableNanoapp(int contextHubId, long appId, int transactionId) throws android.os.RemoteException
    {
    }
    @Override public void enableNanoapp(int contextHubId, long appId, int transactionId) throws android.os.RemoteException
    {
    }
    @Override public void onSettingChanged(byte setting, boolean enabled) throws android.os.RemoteException
    {
    }
    @Override public void queryNanoapps(int contextHubId) throws android.os.RemoteException
    {
    }
    @Override public void registerCallback(int contextHubId, android.hardware.contexthub.IContextHubCallback cb) throws android.os.RemoteException
    {
    }
    @Override public void sendMessageToHub(int contextHubId, android.hardware.contexthub.ContextHubMessage message) throws android.os.RemoteException
    {
    }
    @Override public void onHostEndpointConnected(android.hardware.contexthub.HostEndpointInfo hostEndpointInfo) throws android.os.RemoteException
    {
    }
    @Override public void onHostEndpointDisconnected(char hostEndpointId) throws android.os.RemoteException
    {
    }
    @Override public long[] getPreloadedNanoappIds(int contextHubId) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void onNanSessionStateChanged(android.hardware.contexthub.NanSessionStateUpdate update) throws android.os.RemoteException
    {
    }
    @Override public void setTestMode(boolean enable) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.contexthub.IContextHub
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.contexthub.IContextHub interface,
     * generating a proxy if needed.
     */
    public static android.hardware.contexthub.IContextHub asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.contexthub.IContextHub))) {
        return ((android.hardware.contexthub.IContextHub)iin);
      }
      return new android.hardware.contexthub.IContextHub.Stub.Proxy(obj);
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
        case TRANSACTION_getContextHubs:
        {
          java.util.List<android.hardware.contexthub.ContextHubInfo> _result = this.getContextHubs();
          reply.writeNoException();
          reply.writeTypedList(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_loadNanoapp:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.contexthub.NanoappBinary _arg1;
          _arg1 = data.readTypedObject(android.hardware.contexthub.NanoappBinary.CREATOR);
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.loadNanoapp(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_unloadNanoapp:
        {
          int _arg0;
          _arg0 = data.readInt();
          long _arg1;
          _arg1 = data.readLong();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.unloadNanoapp(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_disableNanoapp:
        {
          int _arg0;
          _arg0 = data.readInt();
          long _arg1;
          _arg1 = data.readLong();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.disableNanoapp(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_enableNanoapp:
        {
          int _arg0;
          _arg0 = data.readInt();
          long _arg1;
          _arg1 = data.readLong();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.enableNanoapp(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onSettingChanged:
        {
          byte _arg0;
          _arg0 = data.readByte();
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.onSettingChanged(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_queryNanoapps:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.queryNanoapps(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_registerCallback:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.contexthub.IContextHubCallback _arg1;
          _arg1 = android.hardware.contexthub.IContextHubCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.registerCallback(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_sendMessageToHub:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.contexthub.ContextHubMessage _arg1;
          _arg1 = data.readTypedObject(android.hardware.contexthub.ContextHubMessage.CREATOR);
          data.enforceNoDataAvail();
          this.sendMessageToHub(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onHostEndpointConnected:
        {
          android.hardware.contexthub.HostEndpointInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.contexthub.HostEndpointInfo.CREATOR);
          data.enforceNoDataAvail();
          this.onHostEndpointConnected(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onHostEndpointDisconnected:
        {
          char _arg0;
          _arg0 = (char)data.readInt();
          data.enforceNoDataAvail();
          this.onHostEndpointDisconnected(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getPreloadedNanoappIds:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          long[] _result = this.getPreloadedNanoappIds(_arg0);
          reply.writeNoException();
          reply.writeLongArray(_result);
          break;
        }
        case TRANSACTION_onNanSessionStateChanged:
        {
          android.hardware.contexthub.NanSessionStateUpdate _arg0;
          _arg0 = data.readTypedObject(android.hardware.contexthub.NanSessionStateUpdate.CREATOR);
          data.enforceNoDataAvail();
          this.onNanSessionStateChanged(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setTestMode:
        {
          boolean _arg0;
          _arg0 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setTestMode(_arg0);
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
    private static class Proxy implements android.hardware.contexthub.IContextHub
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
      @Override public java.util.List<android.hardware.contexthub.ContextHubInfo> getContextHubs() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.util.List<android.hardware.contexthub.ContextHubInfo> _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getContextHubs, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getContextHubs is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createTypedArrayList(android.hardware.contexthub.ContextHubInfo.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void loadNanoapp(int contextHubId, android.hardware.contexthub.NanoappBinary appBinary, int transactionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(contextHubId);
          _data.writeTypedObject(appBinary, 0);
          _data.writeInt(transactionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_loadNanoapp, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method loadNanoapp is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void unloadNanoapp(int contextHubId, long appId, int transactionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(contextHubId);
          _data.writeLong(appId);
          _data.writeInt(transactionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unloadNanoapp, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method unloadNanoapp is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void disableNanoapp(int contextHubId, long appId, int transactionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(contextHubId);
          _data.writeLong(appId);
          _data.writeInt(transactionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_disableNanoapp, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method disableNanoapp is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void enableNanoapp(int contextHubId, long appId, int transactionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(contextHubId);
          _data.writeLong(appId);
          _data.writeInt(transactionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_enableNanoapp, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method enableNanoapp is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onSettingChanged(byte setting, boolean enabled) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(setting);
          _data.writeBoolean(enabled);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onSettingChanged, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onSettingChanged is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void queryNanoapps(int contextHubId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(contextHubId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_queryNanoapps, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method queryNanoapps is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void registerCallback(int contextHubId, android.hardware.contexthub.IContextHubCallback cb) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(contextHubId);
          _data.writeStrongInterface(cb);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerCallback, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method registerCallback is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void sendMessageToHub(int contextHubId, android.hardware.contexthub.ContextHubMessage message) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(contextHubId);
          _data.writeTypedObject(message, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendMessageToHub, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method sendMessageToHub is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onHostEndpointConnected(android.hardware.contexthub.HostEndpointInfo hostEndpointInfo) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(hostEndpointInfo, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onHostEndpointConnected, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onHostEndpointConnected is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onHostEndpointDisconnected(char hostEndpointId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(((int)hostEndpointId));
          boolean _status = mRemote.transact(Stub.TRANSACTION_onHostEndpointDisconnected, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onHostEndpointDisconnected is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public long[] getPreloadedNanoappIds(int contextHubId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        long[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(contextHubId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getPreloadedNanoappIds, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getPreloadedNanoappIds is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createLongArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void onNanSessionStateChanged(android.hardware.contexthub.NanSessionStateUpdate update) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(update, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onNanSessionStateChanged, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onNanSessionStateChanged is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setTestMode(boolean enable) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeBoolean(enable);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setTestMode, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setTestMode is unimplemented.");
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
    static final int TRANSACTION_getContextHubs = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_loadNanoapp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_unloadNanoapp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_disableNanoapp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_enableNanoapp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_onSettingChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_queryNanoapps = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_registerCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_sendMessageToHub = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_onHostEndpointConnected = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_onHostEndpointDisconnected = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_getPreloadedNanoappIds = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_onNanSessionStateChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_setTestMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$contexthub$IContextHub".replace('$', '.');
  public static final int EX_CONTEXT_HUB_UNSPECIFIED = -1;
  public java.util.List<android.hardware.contexthub.ContextHubInfo> getContextHubs() throws android.os.RemoteException;
  public void loadNanoapp(int contextHubId, android.hardware.contexthub.NanoappBinary appBinary, int transactionId) throws android.os.RemoteException;
  public void unloadNanoapp(int contextHubId, long appId, int transactionId) throws android.os.RemoteException;
  public void disableNanoapp(int contextHubId, long appId, int transactionId) throws android.os.RemoteException;
  public void enableNanoapp(int contextHubId, long appId, int transactionId) throws android.os.RemoteException;
  public void onSettingChanged(byte setting, boolean enabled) throws android.os.RemoteException;
  public void queryNanoapps(int contextHubId) throws android.os.RemoteException;
  public void registerCallback(int contextHubId, android.hardware.contexthub.IContextHubCallback cb) throws android.os.RemoteException;
  public void sendMessageToHub(int contextHubId, android.hardware.contexthub.ContextHubMessage message) throws android.os.RemoteException;
  public void onHostEndpointConnected(android.hardware.contexthub.HostEndpointInfo hostEndpointInfo) throws android.os.RemoteException;
  public void onHostEndpointDisconnected(char hostEndpointId) throws android.os.RemoteException;
  public long[] getPreloadedNanoappIds(int contextHubId) throws android.os.RemoteException;
  public void onNanSessionStateChanged(android.hardware.contexthub.NanSessionStateUpdate update) throws android.os.RemoteException;
  public void setTestMode(boolean enable) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
