/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.soundtrigger_middleware;
/**
 * Opaque callback for acknowledging oneway events.
 * Since there is no return channel for oneway events,
 * passing this interface in a oneway method allows the service to call
 * back to the client to indicate the event was registered.
 * This essentially functions like a <code> Future<void> </code> without
 * an error channel.
 * {@hide}
 */
public interface IAcknowledgeEvent extends android.os.IInterface
{
  /** Default implementation for IAcknowledgeEvent. */
  public static class Default implements android.media.soundtrigger_middleware.IAcknowledgeEvent
  {
    /** Acknowledge that the event has been received. */
    @Override public void eventReceived() throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.media.soundtrigger_middleware.IAcknowledgeEvent
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.media.soundtrigger_middleware.IAcknowledgeEvent interface,
     * generating a proxy if needed.
     */
    public static android.media.soundtrigger_middleware.IAcknowledgeEvent asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.media.soundtrigger_middleware.IAcknowledgeEvent))) {
        return ((android.media.soundtrigger_middleware.IAcknowledgeEvent)iin);
      }
      return new android.media.soundtrigger_middleware.IAcknowledgeEvent.Stub.Proxy(obj);
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
        case TRANSACTION_eventReceived:
        {
          this.eventReceived();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.media.soundtrigger_middleware.IAcknowledgeEvent
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
      /** Acknowledge that the event has been received. */
      @Override public void eventReceived() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_eventReceived, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_eventReceived = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
  }
  public static final java.lang.String DESCRIPTOR = "android$media$soundtrigger_middleware$IAcknowledgeEvent".replace('$', '.');
  /** Acknowledge that the event has been received. */
  public void eventReceived() throws android.os.RemoteException;
}
