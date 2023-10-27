/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public interface IDvr extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "f8d74c149f04e76b6d622db2bd8e465dae24b08c";
  /** Default implementation for IDvr. */
  public static class Default implements android.hardware.tv.tuner.IDvr
  {
    @Override public void getQueueDesc(android.hardware.common.fmq.MQDescriptor<Byte,Byte> queue) throws android.os.RemoteException
    {
    }
    @Override public void configure(android.hardware.tv.tuner.DvrSettings settings) throws android.os.RemoteException
    {
    }
    @Override public void attachFilter(android.hardware.tv.tuner.IFilter filter) throws android.os.RemoteException
    {
    }
    @Override public void detachFilter(android.hardware.tv.tuner.IFilter filter) throws android.os.RemoteException
    {
    }
    @Override public void start() throws android.os.RemoteException
    {
    }
    @Override public void stop() throws android.os.RemoteException
    {
    }
    @Override public void flush() throws android.os.RemoteException
    {
    }
    @Override public void close() throws android.os.RemoteException
    {
    }
    @Override public void setStatusCheckIntervalHint(long milliseconds) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.tv.tuner.IDvr
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.tv.tuner.IDvr interface,
     * generating a proxy if needed.
     */
    public static android.hardware.tv.tuner.IDvr asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.tv.tuner.IDvr))) {
        return ((android.hardware.tv.tuner.IDvr)iin);
      }
      return new android.hardware.tv.tuner.IDvr.Stub.Proxy(obj);
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
        case TRANSACTION_getQueueDesc:
        {
          android.hardware.common.fmq.MQDescriptor<Byte,Byte> _arg0;
          _arg0 = new android.hardware.common.fmq.MQDescriptor<Byte,Byte>();
          data.enforceNoDataAvail();
          this.getQueueDesc(_arg0);
          reply.writeNoException();
          reply.writeTypedObject(_arg0, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_configure:
        {
          android.hardware.tv.tuner.DvrSettings _arg0;
          _arg0 = data.readTypedObject(android.hardware.tv.tuner.DvrSettings.CREATOR);
          data.enforceNoDataAvail();
          this.configure(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_attachFilter:
        {
          android.hardware.tv.tuner.IFilter _arg0;
          _arg0 = android.hardware.tv.tuner.IFilter.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.attachFilter(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_detachFilter:
        {
          android.hardware.tv.tuner.IFilter _arg0;
          _arg0 = android.hardware.tv.tuner.IFilter.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.detachFilter(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_start:
        {
          this.start();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_stop:
        {
          this.stop();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_flush:
        {
          this.flush();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_close:
        {
          this.close();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setStatusCheckIntervalHint:
        {
          long _arg0;
          _arg0 = data.readLong();
          data.enforceNoDataAvail();
          this.setStatusCheckIntervalHint(_arg0);
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
    private static class Proxy implements android.hardware.tv.tuner.IDvr
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
      @Override public void getQueueDesc(android.hardware.common.fmq.MQDescriptor<Byte,Byte> queue) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getQueueDesc, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getQueueDesc is unimplemented.");
          }
          _reply.readException();
          if ((0!=_reply.readInt())) {
            queue.readFromParcel(_reply);
          }
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void configure(android.hardware.tv.tuner.DvrSettings settings) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(settings, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_configure, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method configure is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void attachFilter(android.hardware.tv.tuner.IFilter filter) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(filter);
          boolean _status = mRemote.transact(Stub.TRANSACTION_attachFilter, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method attachFilter is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void detachFilter(android.hardware.tv.tuner.IFilter filter) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(filter);
          boolean _status = mRemote.transact(Stub.TRANSACTION_detachFilter, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method detachFilter is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void start() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_start, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method start is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void stop() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stop, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method stop is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void flush() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_flush, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method flush is unimplemented.");
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
      @Override public void setStatusCheckIntervalHint(long milliseconds) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(milliseconds);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setStatusCheckIntervalHint, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setStatusCheckIntervalHint is unimplemented.");
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
    static final int TRANSACTION_getQueueDesc = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_configure = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_attachFilter = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_detachFilter = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_start = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_stop = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_flush = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_close = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_setStatusCheckIntervalHint = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$tv$tuner$IDvr".replace('$', '.');
  public void getQueueDesc(android.hardware.common.fmq.MQDescriptor<Byte,Byte> queue) throws android.os.RemoteException;
  public void configure(android.hardware.tv.tuner.DvrSettings settings) throws android.os.RemoteException;
  public void attachFilter(android.hardware.tv.tuner.IFilter filter) throws android.os.RemoteException;
  public void detachFilter(android.hardware.tv.tuner.IFilter filter) throws android.os.RemoteException;
  public void start() throws android.os.RemoteException;
  public void stop() throws android.os.RemoteException;
  public void flush() throws android.os.RemoteException;
  public void close() throws android.os.RemoteException;
  public void setStatusCheckIntervalHint(long milliseconds) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
