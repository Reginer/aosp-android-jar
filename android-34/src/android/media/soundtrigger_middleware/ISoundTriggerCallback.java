/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.soundtrigger_middleware;
/**
 * Main interface for a client to get notifications of events coming from this module.
 * 
 * {@hide}
 */
public interface ISoundTriggerCallback extends android.os.IInterface
{
  /** Default implementation for ISoundTriggerCallback. */
  public static class Default implements android.media.soundtrigger_middleware.ISoundTriggerCallback
  {
    /**
     * Invoked whenever a recognition event is triggered (typically, on recognition, but also in
     * case of external aborting of a recognition or a forced recognition event - see the status
     * code in the event for determining).
     * In case of abortion, the caller may retry after the next onRecognitionAvailabilityChange()
     * callback.
     */
    @Override public void onRecognition(int modelHandle, android.media.soundtrigger_middleware.RecognitionEventSys event, int captureSession) throws android.os.RemoteException
    {
    }
    /**
     * Invoked whenever a phrase recognition event is triggered (typically, on recognition, but
     * also in case of external aborting of a recognition or a forced recognition event - see the
     * status code in the event for determining).
     * In case of abortion, the caller may retry after the next onRecognitionAvailabilityChange()
     * callback.
     */
    @Override public void onPhraseRecognition(int modelHandle, android.media.soundtrigger_middleware.PhraseRecognitionEventSys event, int captureSession) throws android.os.RemoteException
    {
    }
    /**
     * Notifies the client that some start/load operations that have previously failed for resource
     * reasons (threw a ServiceSpecificException(RESOURCE_CONTENTION) or have been preempted) may
     * now succeed. This is not a guarantee, but a hint for the client to retry.
     */
    @Override public void onResourcesAvailable() throws android.os.RemoteException
    {
    }
    /**
     * Notifies the client that a model had been preemptively unloaded by the service.
     * The caller may retry after the next onRecognitionAvailabilityChange() callback.
     */
    @Override public void onModelUnloaded(int modelHandle) throws android.os.RemoteException
    {
    }
    /**
     * Notifies the client that the associated module has crashed and restarted. The module instance
     * is no longer usable and will throw a ServiceSpecificException with a Status.DEAD_OBJECT code
     * for every call. The client should detach, then re-attach to the module in order to get a new,
     * usable instance. All state for this module has been lost.
     */
    @Override public void onModuleDied() throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.media.soundtrigger_middleware.ISoundTriggerCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.media.soundtrigger_middleware.ISoundTriggerCallback interface,
     * generating a proxy if needed.
     */
    public static android.media.soundtrigger_middleware.ISoundTriggerCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.media.soundtrigger_middleware.ISoundTriggerCallback))) {
        return ((android.media.soundtrigger_middleware.ISoundTriggerCallback)iin);
      }
      return new android.media.soundtrigger_middleware.ISoundTriggerCallback.Stub.Proxy(obj);
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
        case TRANSACTION_onRecognition:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.media.soundtrigger_middleware.RecognitionEventSys _arg1;
          _arg1 = data.readTypedObject(android.media.soundtrigger_middleware.RecognitionEventSys.CREATOR);
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.onRecognition(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_onPhraseRecognition:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.media.soundtrigger_middleware.PhraseRecognitionEventSys _arg1;
          _arg1 = data.readTypedObject(android.media.soundtrigger_middleware.PhraseRecognitionEventSys.CREATOR);
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.onPhraseRecognition(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_onResourcesAvailable:
        {
          this.onResourcesAvailable();
          break;
        }
        case TRANSACTION_onModelUnloaded:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.onModelUnloaded(_arg0);
          break;
        }
        case TRANSACTION_onModuleDied:
        {
          this.onModuleDied();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.media.soundtrigger_middleware.ISoundTriggerCallback
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
       * Invoked whenever a recognition event is triggered (typically, on recognition, but also in
       * case of external aborting of a recognition or a forced recognition event - see the status
       * code in the event for determining).
       * In case of abortion, the caller may retry after the next onRecognitionAvailabilityChange()
       * callback.
       */
      @Override public void onRecognition(int modelHandle, android.media.soundtrigger_middleware.RecognitionEventSys event, int captureSession) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(modelHandle);
          _data.writeTypedObject(event, 0);
          _data.writeInt(captureSession);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onRecognition, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Invoked whenever a phrase recognition event is triggered (typically, on recognition, but
       * also in case of external aborting of a recognition or a forced recognition event - see the
       * status code in the event for determining).
       * In case of abortion, the caller may retry after the next onRecognitionAvailabilityChange()
       * callback.
       */
      @Override public void onPhraseRecognition(int modelHandle, android.media.soundtrigger_middleware.PhraseRecognitionEventSys event, int captureSession) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(modelHandle);
          _data.writeTypedObject(event, 0);
          _data.writeInt(captureSession);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onPhraseRecognition, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Notifies the client that some start/load operations that have previously failed for resource
       * reasons (threw a ServiceSpecificException(RESOURCE_CONTENTION) or have been preempted) may
       * now succeed. This is not a guarantee, but a hint for the client to retry.
       */
      @Override public void onResourcesAvailable() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onResourcesAvailable, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Notifies the client that a model had been preemptively unloaded by the service.
       * The caller may retry after the next onRecognitionAvailabilityChange() callback.
       */
      @Override public void onModelUnloaded(int modelHandle) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(modelHandle);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onModelUnloaded, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Notifies the client that the associated module has crashed and restarted. The module instance
       * is no longer usable and will throw a ServiceSpecificException with a Status.DEAD_OBJECT code
       * for every call. The client should detach, then re-attach to the module in order to get a new,
       * usable instance. All state for this module has been lost.
       */
      @Override public void onModuleDied() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onModuleDied, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onRecognition = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onPhraseRecognition = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_onResourcesAvailable = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_onModelUnloaded = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_onModuleDied = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
  }
  public static final java.lang.String DESCRIPTOR = "android$media$soundtrigger_middleware$ISoundTriggerCallback".replace('$', '.');
  /**
   * Invoked whenever a recognition event is triggered (typically, on recognition, but also in
   * case of external aborting of a recognition or a forced recognition event - see the status
   * code in the event for determining).
   * In case of abortion, the caller may retry after the next onRecognitionAvailabilityChange()
   * callback.
   */
  public void onRecognition(int modelHandle, android.media.soundtrigger_middleware.RecognitionEventSys event, int captureSession) throws android.os.RemoteException;
  /**
   * Invoked whenever a phrase recognition event is triggered (typically, on recognition, but
   * also in case of external aborting of a recognition or a forced recognition event - see the
   * status code in the event for determining).
   * In case of abortion, the caller may retry after the next onRecognitionAvailabilityChange()
   * callback.
   */
  public void onPhraseRecognition(int modelHandle, android.media.soundtrigger_middleware.PhraseRecognitionEventSys event, int captureSession) throws android.os.RemoteException;
  /**
   * Notifies the client that some start/load operations that have previously failed for resource
   * reasons (threw a ServiceSpecificException(RESOURCE_CONTENTION) or have been preempted) may
   * now succeed. This is not a guarantee, but a hint for the client to retry.
   */
  public void onResourcesAvailable() throws android.os.RemoteException;
  /**
   * Notifies the client that a model had been preemptively unloaded by the service.
   * The caller may retry after the next onRecognitionAvailabilityChange() callback.
   */
  public void onModelUnloaded(int modelHandle) throws android.os.RemoteException;
  /**
   * Notifies the client that the associated module has crashed and restarted. The module instance
   * is no longer usable and will throw a ServiceSpecificException with a Status.DEAD_OBJECT code
   * for every call. The client should detach, then re-attach to the module in order to get a new,
   * usable instance. All state for this module has been lost.
   */
  public void onModuleDied() throws android.os.RemoteException;
}
