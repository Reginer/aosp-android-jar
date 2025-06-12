/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 2 --hash ee2e6f0bd51391955f79f4d5eeeafc37c668cd40 --min_sdk_version current --ninja -d out/soong/.intermediates/system/update_engine/stable/libupdate_engine_stable-V2-java-source/gen/android/os/IUpdateEngineStable.java.d -o out/soong/.intermediates/system/update_engine/stable/libupdate_engine_stable-V2-java-source/gen -Nsystem/update_engine/stable/aidl_api/libupdate_engine_stable/2 system/update_engine/stable/aidl_api/libupdate_engine_stable/2/android/os/IUpdateEngineStable.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.os;
/** @hide */
public interface IUpdateEngineStable extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "ee2e6f0bd51391955f79f4d5eeeafc37c668cd40";
  /** Default implementation for IUpdateEngineStable. */
  public static class Default implements android.os.IUpdateEngineStable
  {
    /** @hide */
    @Override public void applyPayloadFd(android.os.ParcelFileDescriptor pfd, long payload_offset, long payload_size, java.lang.String[] headerKeyValuePairs) throws android.os.RemoteException
    {
    }
    /** @hide */
    @Override public boolean bind(android.os.IUpdateEngineStableCallback callback) throws android.os.RemoteException
    {
      return false;
    }
    /** @hide */
    @Override public boolean unbind(android.os.IUpdateEngineStableCallback callback) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.os.IUpdateEngineStable
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.os.IUpdateEngineStable interface,
     * generating a proxy if needed.
     */
    public static android.os.IUpdateEngineStable asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.os.IUpdateEngineStable))) {
        return ((android.os.IUpdateEngineStable)iin);
      }
      return new android.os.IUpdateEngineStable.Stub.Proxy(obj);
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
        case TRANSACTION_applyPayloadFd:
        {
          android.os.ParcelFileDescriptor _arg0;
          _arg0 = data.readTypedObject(android.os.ParcelFileDescriptor.CREATOR);
          long _arg1;
          _arg1 = data.readLong();
          long _arg2;
          _arg2 = data.readLong();
          java.lang.String[] _arg3;
          _arg3 = data.createStringArray();
          data.enforceNoDataAvail();
          this.applyPayloadFd(_arg0, _arg1, _arg2, _arg3);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_bind:
        {
          android.os.IUpdateEngineStableCallback _arg0;
          _arg0 = android.os.IUpdateEngineStableCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          boolean _result = this.bind(_arg0);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_unbind:
        {
          android.os.IUpdateEngineStableCallback _arg0;
          _arg0 = android.os.IUpdateEngineStableCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          boolean _result = this.unbind(_arg0);
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
    private static class Proxy implements android.os.IUpdateEngineStable
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
      /** @hide */
      @Override public void applyPayloadFd(android.os.ParcelFileDescriptor pfd, long payload_offset, long payload_size, java.lang.String[] headerKeyValuePairs) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(pfd, 0);
          _data.writeLong(payload_offset);
          _data.writeLong(payload_size);
          _data.writeStringArray(headerKeyValuePairs);
          boolean _status = mRemote.transact(Stub.TRANSACTION_applyPayloadFd, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method applyPayloadFd is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** @hide */
      @Override public boolean bind(android.os.IUpdateEngineStableCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_bind, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method bind is unimplemented.");
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
      /** @hide */
      @Override public boolean unbind(android.os.IUpdateEngineStableCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unbind, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method unbind is unimplemented.");
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
    static final int TRANSACTION_applyPayloadFd = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_bind = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_unbind = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android$os$IUpdateEngineStable".replace('$', '.');
  /** @hide */
  public void applyPayloadFd(android.os.ParcelFileDescriptor pfd, long payload_offset, long payload_size, java.lang.String[] headerKeyValuePairs) throws android.os.RemoteException;
  /** @hide */
  public boolean bind(android.os.IUpdateEngineStableCallback callback) throws android.os.RemoteException;
  /** @hide */
  public boolean unbind(android.os.IUpdateEngineStableCallback callback) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
