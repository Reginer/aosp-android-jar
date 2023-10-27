/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.cas;
/** @hide */
public interface IMediaCasService extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 1;
  public static final String HASH = "bc51d8d70a55ec4723d3f73d0acf7003306bf69f";
  /** Default implementation for IMediaCasService. */
  public static class Default implements android.hardware.cas.IMediaCasService
  {
    @Override public android.hardware.cas.IDescrambler createDescrambler(int CA_system_id) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.cas.ICas createPlugin(int CA_system_id, android.hardware.cas.ICasListener listener) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.cas.AidlCasPluginDescriptor[] enumeratePlugins() throws android.os.RemoteException
    {
      return null;
    }
    @Override public boolean isDescramblerSupported(int CA_system_id) throws android.os.RemoteException
    {
      return false;
    }
    @Override public boolean isSystemIdSupported(int CA_system_id) throws android.os.RemoteException
    {
      return false;
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.cas.IMediaCasService
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.cas.IMediaCasService interface,
     * generating a proxy if needed.
     */
    public static android.hardware.cas.IMediaCasService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.cas.IMediaCasService))) {
        return ((android.hardware.cas.IMediaCasService)iin);
      }
      return new android.hardware.cas.IMediaCasService.Stub.Proxy(obj);
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
        case TRANSACTION_createDescrambler:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          android.hardware.cas.IDescrambler _result = this.createDescrambler(_arg0);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_createPlugin:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.cas.ICasListener _arg1;
          _arg1 = android.hardware.cas.ICasListener.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          android.hardware.cas.ICas _result = this.createPlugin(_arg0, _arg1);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_enumeratePlugins:
        {
          android.hardware.cas.AidlCasPluginDescriptor[] _result = this.enumeratePlugins();
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_isDescramblerSupported:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          boolean _result = this.isDescramblerSupported(_arg0);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_isSystemIdSupported:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          boolean _result = this.isSystemIdSupported(_arg0);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.cas.IMediaCasService
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
      @Override public android.hardware.cas.IDescrambler createDescrambler(int CA_system_id) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.cas.IDescrambler _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(CA_system_id);
          boolean _status = mRemote.transact(Stub.TRANSACTION_createDescrambler, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method createDescrambler is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.cas.IDescrambler.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.cas.ICas createPlugin(int CA_system_id, android.hardware.cas.ICasListener listener) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.cas.ICas _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(CA_system_id);
          _data.writeStrongInterface(listener);
          boolean _status = mRemote.transact(Stub.TRANSACTION_createPlugin, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method createPlugin is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.cas.ICas.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.cas.AidlCasPluginDescriptor[] enumeratePlugins() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.cas.AidlCasPluginDescriptor[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_enumeratePlugins, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method enumeratePlugins is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createTypedArray(android.hardware.cas.AidlCasPluginDescriptor.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean isDescramblerSupported(int CA_system_id) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(CA_system_id);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isDescramblerSupported, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method isDescramblerSupported is unimplemented.");
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
      @Override public boolean isSystemIdSupported(int CA_system_id) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(CA_system_id);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isSystemIdSupported, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method isSystemIdSupported is unimplemented.");
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
    static final int TRANSACTION_createDescrambler = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_createPlugin = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_enumeratePlugins = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_isDescramblerSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_isSystemIdSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$cas$IMediaCasService".replace('$', '.');
  public android.hardware.cas.IDescrambler createDescrambler(int CA_system_id) throws android.os.RemoteException;
  public android.hardware.cas.ICas createPlugin(int CA_system_id, android.hardware.cas.ICasListener listener) throws android.os.RemoteException;
  public android.hardware.cas.AidlCasPluginDescriptor[] enumeratePlugins() throws android.os.RemoteException;
  public boolean isDescramblerSupported(int CA_system_id) throws android.os.RemoteException;
  public boolean isSystemIdSupported(int CA_system_id) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
