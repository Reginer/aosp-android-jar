/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.soundtrigger_middleware;
/**
 * Interface for injecting model events into the fake ST HAL.
 * 
 * {@hide}
 */
public interface IInjectModelEvent extends android.os.IInterface
{
  /** Default implementation for IInjectModelEvent. */
  public static class Default implements android.media.soundtrigger_middleware.IInjectModelEvent
  {
    /**
     * Trigger a preemptive model unload for the model session associated with
     * this object.
     * This invalidates the {@link IInjectModelEvent} session.
     */
    @Override public void triggerUnloadModel() throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.media.soundtrigger_middleware.IInjectModelEvent
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.media.soundtrigger_middleware.IInjectModelEvent interface,
     * generating a proxy if needed.
     */
    public static android.media.soundtrigger_middleware.IInjectModelEvent asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.media.soundtrigger_middleware.IInjectModelEvent))) {
        return ((android.media.soundtrigger_middleware.IInjectModelEvent)iin);
      }
      return new android.media.soundtrigger_middleware.IInjectModelEvent.Stub.Proxy(obj);
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
        case TRANSACTION_triggerUnloadModel:
        {
          this.triggerUnloadModel();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.media.soundtrigger_middleware.IInjectModelEvent
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
       * Trigger a preemptive model unload for the model session associated with
       * this object.
       * This invalidates the {@link IInjectModelEvent} session.
       */
      @Override public void triggerUnloadModel() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_triggerUnloadModel, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_triggerUnloadModel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
  }
  public static final java.lang.String DESCRIPTOR = "android$media$soundtrigger_middleware$IInjectModelEvent".replace('$', '.');
  /**
   * Trigger a preemptive model unload for the model session associated with
   * this object.
   * This invalidates the {@link IInjectModelEvent} session.
   */
  public void triggerUnloadModel() throws android.os.RemoteException;
}
