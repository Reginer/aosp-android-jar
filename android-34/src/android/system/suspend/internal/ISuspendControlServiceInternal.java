/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.system.suspend.internal;
/**
 * Interface exposed by the suspend hal that allows framework to toggle the suspend loop and
 * monitor native wakelocks.
 * @hide
 */
public interface ISuspendControlServiceInternal extends android.os.IInterface
{
  /** Default implementation for ISuspendControlServiceInternal. */
  public static class Default implements android.system.suspend.internal.ISuspendControlServiceInternal
  {
    /**
     * Starts automatic system suspension.
     * 
     * @param token token registering automatic system suspension.
     * When all registered tokens die automatic system suspension is disabled.
     * @return true on success, false otherwise.
     */
    @Override public boolean enableAutosuspend(android.os.IBinder token) throws android.os.RemoteException
    {
      return false;
    }
    /** Suspends the system even if there are wakelocks being held. */
    @Override public boolean forceSuspend() throws android.os.RemoteException
    {
      return false;
    }
    /** Returns a list of wake lock stats. */
    @Override public android.system.suspend.internal.WakeLockInfo[] getWakeLockStats() throws android.os.RemoteException
    {
      return null;
    }
    /** Returns a list of wakeup stats. */
    @Override public android.system.suspend.internal.WakeupInfo[] getWakeupStats() throws android.os.RemoteException
    {
      return null;
    }
    /** Returns stats related to suspend. */
    @Override public android.system.suspend.internal.SuspendInfo getSuspendStats() throws android.os.RemoteException
    {
      return null;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.system.suspend.internal.ISuspendControlServiceInternal
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.system.suspend.internal.ISuspendControlServiceInternal interface,
     * generating a proxy if needed.
     */
    public static android.system.suspend.internal.ISuspendControlServiceInternal asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.system.suspend.internal.ISuspendControlServiceInternal))) {
        return ((android.system.suspend.internal.ISuspendControlServiceInternal)iin);
      }
      return new android.system.suspend.internal.ISuspendControlServiceInternal.Stub.Proxy(obj);
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
      }
      switch (code)
      {
        case TRANSACTION_enableAutosuspend:
        {
          android.os.IBinder _arg0;
          _arg0 = data.readStrongBinder();
          boolean _result = this.enableAutosuspend(_arg0);
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_forceSuspend:
        {
          boolean _result = this.forceSuspend();
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_getWakeLockStats:
        {
          android.system.suspend.internal.WakeLockInfo[] _result = this.getWakeLockStats();
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getWakeupStats:
        {
          android.system.suspend.internal.WakeupInfo[] _result = this.getWakeupStats();
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getSuspendStats:
        {
          android.system.suspend.internal.SuspendInfo _result = this.getSuspendStats();
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.system.suspend.internal.ISuspendControlServiceInternal
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      /**
       * Starts automatic system suspension.
       * 
       * @param token token registering automatic system suspension.
       * When all registered tokens die automatic system suspension is disabled.
       * @return true on success, false otherwise.
       */
      @Override public boolean enableAutosuspend(android.os.IBinder token) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongBinder(token);
          boolean _status = mRemote.transact(Stub.TRANSACTION_enableAutosuspend, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Suspends the system even if there are wakelocks being held. */
      @Override public boolean forceSuspend() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_forceSuspend, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Returns a list of wake lock stats. */
      @Override public android.system.suspend.internal.WakeLockInfo[] getWakeLockStats() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.system.suspend.internal.WakeLockInfo[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getWakeLockStats, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.system.suspend.internal.WakeLockInfo.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Returns a list of wakeup stats. */
      @Override public android.system.suspend.internal.WakeupInfo[] getWakeupStats() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.system.suspend.internal.WakeupInfo[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getWakeupStats, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.system.suspend.internal.WakeupInfo.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Returns stats related to suspend. */
      @Override public android.system.suspend.internal.SuspendInfo getSuspendStats() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.system.suspend.internal.SuspendInfo _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSuspendStats, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readTypedObject(android.system.suspend.internal.SuspendInfo.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_enableAutosuspend = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_forceSuspend = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_getWakeLockStats = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_getWakeupStats = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getSuspendStats = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
  }
  public static final java.lang.String DESCRIPTOR = "android$system$suspend$internal$ISuspendControlServiceInternal".replace('$', '.');
  /**
   * Starts automatic system suspension.
   * 
   * @param token token registering automatic system suspension.
   * When all registered tokens die automatic system suspension is disabled.
   * @return true on success, false otherwise.
   */
  public boolean enableAutosuspend(android.os.IBinder token) throws android.os.RemoteException;
  /** Suspends the system even if there are wakelocks being held. */
  public boolean forceSuspend() throws android.os.RemoteException;
  /** Returns a list of wake lock stats. */
  public android.system.suspend.internal.WakeLockInfo[] getWakeLockStats() throws android.os.RemoteException;
  /** Returns a list of wakeup stats. */
  public android.system.suspend.internal.WakeupInfo[] getWakeupStats() throws android.os.RemoteException;
  /** Returns stats related to suspend. */
  public android.system.suspend.internal.SuspendInfo getSuspendStats() throws android.os.RemoteException;
}
