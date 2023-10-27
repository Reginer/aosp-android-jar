/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public interface IDescrambler extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "f8d74c149f04e76b6d622db2bd8e465dae24b08c";
  /** Default implementation for IDescrambler. */
  public static class Default implements android.hardware.tv.tuner.IDescrambler
  {
    @Override public void setDemuxSource(int demuxId) throws android.os.RemoteException
    {
    }
    @Override public void setKeyToken(byte[] keyToken) throws android.os.RemoteException
    {
    }
    @Override public void addPid(android.hardware.tv.tuner.DemuxPid pid, android.hardware.tv.tuner.IFilter optionalSourceFilter) throws android.os.RemoteException
    {
    }
    @Override public void removePid(android.hardware.tv.tuner.DemuxPid pid, android.hardware.tv.tuner.IFilter optionalSourceFilter) throws android.os.RemoteException
    {
    }
    @Override public void close() throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.tv.tuner.IDescrambler
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.tv.tuner.IDescrambler interface,
     * generating a proxy if needed.
     */
    public static android.hardware.tv.tuner.IDescrambler asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.tv.tuner.IDescrambler))) {
        return ((android.hardware.tv.tuner.IDescrambler)iin);
      }
      return new android.hardware.tv.tuner.IDescrambler.Stub.Proxy(obj);
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
        case TRANSACTION_setDemuxSource:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.setDemuxSource(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setKeyToken:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          data.enforceNoDataAvail();
          this.setKeyToken(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_addPid:
        {
          android.hardware.tv.tuner.DemuxPid _arg0;
          _arg0 = data.readTypedObject(android.hardware.tv.tuner.DemuxPid.CREATOR);
          android.hardware.tv.tuner.IFilter _arg1;
          _arg1 = android.hardware.tv.tuner.IFilter.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.addPid(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_removePid:
        {
          android.hardware.tv.tuner.DemuxPid _arg0;
          _arg0 = data.readTypedObject(android.hardware.tv.tuner.DemuxPid.CREATOR);
          android.hardware.tv.tuner.IFilter _arg1;
          _arg1 = android.hardware.tv.tuner.IFilter.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.removePid(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_close:
        {
          this.close();
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
    private static class Proxy implements android.hardware.tv.tuner.IDescrambler
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
      @Override public void setDemuxSource(int demuxId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(demuxId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setDemuxSource, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setDemuxSource is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setKeyToken(byte[] keyToken) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(keyToken);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setKeyToken, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setKeyToken is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void addPid(android.hardware.tv.tuner.DemuxPid pid, android.hardware.tv.tuner.IFilter optionalSourceFilter) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(pid, 0);
          _data.writeStrongInterface(optionalSourceFilter);
          boolean _status = mRemote.transact(Stub.TRANSACTION_addPid, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method addPid is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void removePid(android.hardware.tv.tuner.DemuxPid pid, android.hardware.tv.tuner.IFilter optionalSourceFilter) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(pid, 0);
          _data.writeStrongInterface(optionalSourceFilter);
          boolean _status = mRemote.transact(Stub.TRANSACTION_removePid, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method removePid is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
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
    static final int TRANSACTION_setDemuxSource = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_setKeyToken = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_addPid = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_removePid = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_close = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$tv$tuner$IDescrambler".replace('$', '.');
  public void setDemuxSource(int demuxId) throws android.os.RemoteException;
  public void setKeyToken(byte[] keyToken) throws android.os.RemoteException;
  public void addPid(android.hardware.tv.tuner.DemuxPid pid, android.hardware.tv.tuner.IFilter optionalSourceFilter) throws android.os.RemoteException;
  public void removePid(android.hardware.tv.tuner.DemuxPid pid, android.hardware.tv.tuner.IFilter optionalSourceFilter) throws android.os.RemoteException;
  public void close() throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
