/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.cas;
/** @hide */
public interface IDescrambler extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 1;
  public static final String HASH = "bc51d8d70a55ec4723d3f73d0acf7003306bf69f";
  /** Default implementation for IDescrambler. */
  public static class Default implements android.hardware.cas.IDescrambler
  {
    @Override public int descramble(int scramblingControl, android.hardware.cas.SubSample[] subSamples, android.hardware.cas.SharedBuffer srcBuffer, long srcOffset, android.hardware.cas.DestinationBuffer dstBuffer, long dstOffset) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void release() throws android.os.RemoteException
    {
    }
    @Override public boolean requiresSecureDecoderComponent(java.lang.String mime) throws android.os.RemoteException
    {
      return false;
    }
    @Override public void setMediaCasSession(byte[] sessionId) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.cas.IDescrambler
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.cas.IDescrambler interface,
     * generating a proxy if needed.
     */
    public static android.hardware.cas.IDescrambler asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.cas.IDescrambler))) {
        return ((android.hardware.cas.IDescrambler)iin);
      }
      return new android.hardware.cas.IDescrambler.Stub.Proxy(obj);
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
        case TRANSACTION_descramble:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.cas.SubSample[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.cas.SubSample.CREATOR);
          android.hardware.cas.SharedBuffer _arg2;
          _arg2 = data.readTypedObject(android.hardware.cas.SharedBuffer.CREATOR);
          long _arg3;
          _arg3 = data.readLong();
          android.hardware.cas.DestinationBuffer _arg4;
          _arg4 = data.readTypedObject(android.hardware.cas.DestinationBuffer.CREATOR);
          long _arg5;
          _arg5 = data.readLong();
          data.enforceNoDataAvail();
          int _result = this.descramble(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_release:
        {
          this.release();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_requiresSecureDecoderComponent:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          data.enforceNoDataAvail();
          boolean _result = this.requiresSecureDecoderComponent(_arg0);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_setMediaCasSession:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          data.enforceNoDataAvail();
          this.setMediaCasSession(_arg0);
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
    private static class Proxy implements android.hardware.cas.IDescrambler
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
      @Override public int descramble(int scramblingControl, android.hardware.cas.SubSample[] subSamples, android.hardware.cas.SharedBuffer srcBuffer, long srcOffset, android.hardware.cas.DestinationBuffer dstBuffer, long dstOffset) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(scramblingControl);
          _data.writeTypedArray(subSamples, 0);
          _data.writeTypedObject(srcBuffer, 0);
          _data.writeLong(srcOffset);
          _data.writeTypedObject(dstBuffer, 0);
          _data.writeLong(dstOffset);
          boolean _status = mRemote.transact(Stub.TRANSACTION_descramble, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method descramble is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void release() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_release, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method release is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public boolean requiresSecureDecoderComponent(java.lang.String mime) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(mime);
          boolean _status = mRemote.transact(Stub.TRANSACTION_requiresSecureDecoderComponent, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method requiresSecureDecoderComponent is unimplemented.");
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
      @Override public void setMediaCasSession(byte[] sessionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(sessionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setMediaCasSession, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setMediaCasSession is unimplemented.");
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
    static final int TRANSACTION_descramble = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_release = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_requiresSecureDecoderComponent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_setMediaCasSession = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$cas$IDescrambler".replace('$', '.');
  public int descramble(int scramblingControl, android.hardware.cas.SubSample[] subSamples, android.hardware.cas.SharedBuffer srcBuffer, long srcOffset, android.hardware.cas.DestinationBuffer dstBuffer, long dstOffset) throws android.os.RemoteException;
  public void release() throws android.os.RemoteException;
  public boolean requiresSecureDecoderComponent(java.lang.String mime) throws android.os.RemoteException;
  public void setMediaCasSession(byte[] sessionId) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
