/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.soundtrigger_middleware;
/**
 * Interface for injecting global events to the fake STHAL.
 * {@hide}
 */
public interface IInjectGlobalEvent extends android.os.IInterface
{
  /** Default implementation for IInjectGlobalEvent. */
  public static class Default implements android.media.soundtrigger_middleware.IInjectGlobalEvent
  {
    /**
     * Trigger a fake STHAL restart.
     * This invalidates the {@link IInjectGlobalEvent}.
     */
    @Override public void triggerRestart() throws android.os.RemoteException
    {
    }
    /**
     * Set global resource contention within the fake STHAL. Loads/startRecognition
     * will fail with {@code RESOURCE_CONTENTION} until unset.
     * @param isContended - true to enable resource contention. false to disable resource contention
     *                      and resume normal functionality.
     * @param callback - Call {@link IAcknowledgeEvent#eventReceived()} on this interface once
     * the contention status is successfully set.
     */
    @Override public void setResourceContention(boolean isContended, android.media.soundtrigger_middleware.IAcknowledgeEvent callback) throws android.os.RemoteException
    {
    }
    /**
     * Trigger an
     * {@link android.hardware.soundtrigger3.ISoundTriggerHwGlobalCallback#onResourcesAvailable}
     * callback from the fake STHAL. This callback is used to signal to the framework that
     * previous operations which failed may now succeed.
     */
    @Override public void triggerOnResourcesAvailable() throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.media.soundtrigger_middleware.IInjectGlobalEvent
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.media.soundtrigger_middleware.IInjectGlobalEvent interface,
     * generating a proxy if needed.
     */
    public static android.media.soundtrigger_middleware.IInjectGlobalEvent asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.media.soundtrigger_middleware.IInjectGlobalEvent))) {
        return ((android.media.soundtrigger_middleware.IInjectGlobalEvent)iin);
      }
      return new android.media.soundtrigger_middleware.IInjectGlobalEvent.Stub.Proxy(obj);
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
        case TRANSACTION_triggerRestart:
        {
          this.triggerRestart();
          break;
        }
        case TRANSACTION_setResourceContention:
        {
          boolean _arg0;
          _arg0 = data.readBoolean();
          android.media.soundtrigger_middleware.IAcknowledgeEvent _arg1;
          _arg1 = android.media.soundtrigger_middleware.IAcknowledgeEvent.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.setResourceContention(_arg0, _arg1);
          break;
        }
        case TRANSACTION_triggerOnResourcesAvailable:
        {
          this.triggerOnResourcesAvailable();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.media.soundtrigger_middleware.IInjectGlobalEvent
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
       * Trigger a fake STHAL restart.
       * This invalidates the {@link IInjectGlobalEvent}.
       */
      @Override public void triggerRestart() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_triggerRestart, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Set global resource contention within the fake STHAL. Loads/startRecognition
       * will fail with {@code RESOURCE_CONTENTION} until unset.
       * @param isContended - true to enable resource contention. false to disable resource contention
       *                      and resume normal functionality.
       * @param callback - Call {@link IAcknowledgeEvent#eventReceived()} on this interface once
       * the contention status is successfully set.
       */
      @Override public void setResourceContention(boolean isContended, android.media.soundtrigger_middleware.IAcknowledgeEvent callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeBoolean(isContended);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setResourceContention, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Trigger an
       * {@link android.hardware.soundtrigger3.ISoundTriggerHwGlobalCallback#onResourcesAvailable}
       * callback from the fake STHAL. This callback is used to signal to the framework that
       * previous operations which failed may now succeed.
       */
      @Override public void triggerOnResourcesAvailable() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_triggerOnResourcesAvailable, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_triggerRestart = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_setResourceContention = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_triggerOnResourcesAvailable = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
  }
  public static final java.lang.String DESCRIPTOR = "android$media$soundtrigger_middleware$IInjectGlobalEvent".replace('$', '.');
  /**
   * Trigger a fake STHAL restart.
   * This invalidates the {@link IInjectGlobalEvent}.
   */
  public void triggerRestart() throws android.os.RemoteException;
  /**
   * Set global resource contention within the fake STHAL. Loads/startRecognition
   * will fail with {@code RESOURCE_CONTENTION} until unset.
   * @param isContended - true to enable resource contention. false to disable resource contention
   *                      and resume normal functionality.
   * @param callback - Call {@link IAcknowledgeEvent#eventReceived()} on this interface once
   * the contention status is successfully set.
   */
  public void setResourceContention(boolean isContended, android.media.soundtrigger_middleware.IAcknowledgeEvent callback) throws android.os.RemoteException;
  /**
   * Trigger an
   * {@link android.hardware.soundtrigger3.ISoundTriggerHwGlobalCallback#onResourcesAvailable}
   * callback from the fake STHAL. This callback is used to signal to the framework that
   * previous operations which failed may now succeed.
   */
  public void triggerOnResourcesAvailable() throws android.os.RemoteException;
}
