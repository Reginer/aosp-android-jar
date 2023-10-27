/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.vibrator;
public interface IVibratorManager extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "ea8742d6993e1a82917da38b9938e537aa7fcb54";
  /** Default implementation for IVibratorManager. */
  public static class Default implements android.hardware.vibrator.IVibratorManager
  {
    @Override public int getCapabilities() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int[] getVibratorIds() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.vibrator.IVibrator getVibrator(int vibratorId) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void prepareSynced(int[] vibratorIds) throws android.os.RemoteException
    {
    }
    @Override public void triggerSynced(android.hardware.vibrator.IVibratorCallback callback) throws android.os.RemoteException
    {
    }
    @Override public void cancelSynced() throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.vibrator.IVibratorManager
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.vibrator.IVibratorManager interface,
     * generating a proxy if needed.
     */
    public static android.hardware.vibrator.IVibratorManager asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.vibrator.IVibratorManager))) {
        return ((android.hardware.vibrator.IVibratorManager)iin);
      }
      return new android.hardware.vibrator.IVibratorManager.Stub.Proxy(obj);
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
        case TRANSACTION_getCapabilities:
        {
          int _result = this.getCapabilities();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getVibratorIds:
        {
          int[] _result = this.getVibratorIds();
          reply.writeNoException();
          reply.writeIntArray(_result);
          break;
        }
        case TRANSACTION_getVibrator:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          android.hardware.vibrator.IVibrator _result = this.getVibrator(_arg0);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_prepareSynced:
        {
          int[] _arg0;
          _arg0 = data.createIntArray();
          data.enforceNoDataAvail();
          this.prepareSynced(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_triggerSynced:
        {
          android.hardware.vibrator.IVibratorCallback _arg0;
          _arg0 = android.hardware.vibrator.IVibratorCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.triggerSynced(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_cancelSynced:
        {
          this.cancelSynced();
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
    private static class Proxy implements android.hardware.vibrator.IVibratorManager
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
      @Override public int getCapabilities() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCapabilities, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getCapabilities is unimplemented.");
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
      @Override public int[] getVibratorIds() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getVibratorIds, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getVibratorIds is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createIntArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.vibrator.IVibrator getVibrator(int vibratorId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.vibrator.IVibrator _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(vibratorId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getVibrator, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getVibrator is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.vibrator.IVibrator.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void prepareSynced(int[] vibratorIds) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeIntArray(vibratorIds);
          boolean _status = mRemote.transact(Stub.TRANSACTION_prepareSynced, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method prepareSynced is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void triggerSynced(android.hardware.vibrator.IVibratorCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_triggerSynced, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method triggerSynced is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void cancelSynced() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_cancelSynced, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method cancelSynced is unimplemented.");
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
    static final int TRANSACTION_getCapabilities = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_getVibratorIds = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_getVibrator = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_prepareSynced = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_triggerSynced = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_cancelSynced = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$vibrator$IVibratorManager".replace('$', '.');
  public static final int CAP_SYNC = 1;
  public static final int CAP_PREPARE_ON = 2;
  public static final int CAP_PREPARE_PERFORM = 4;
  public static final int CAP_PREPARE_COMPOSE = 8;
  public static final int CAP_MIXED_TRIGGER_ON = 16;
  public static final int CAP_MIXED_TRIGGER_PERFORM = 32;
  public static final int CAP_MIXED_TRIGGER_COMPOSE = 64;
  public static final int CAP_TRIGGER_CALLBACK = 128;
  public int getCapabilities() throws android.os.RemoteException;
  public int[] getVibratorIds() throws android.os.RemoteException;
  public android.hardware.vibrator.IVibrator getVibrator(int vibratorId) throws android.os.RemoteException;
  public void prepareSynced(int[] vibratorIds) throws android.os.RemoteException;
  public void triggerSynced(android.hardware.vibrator.IVibratorCallback callback) throws android.os.RemoteException;
  public void cancelSynced() throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
