/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.soundtrigger_middleware;
/**
 * Interface for injecting recognition events into the ST Mock HAL.
 * {@hide}
 */
public interface IInjectRecognitionEvent extends android.os.IInterface
{
  /** Default implementation for IInjectRecognitionEvent. */
  public static class Default implements android.media.soundtrigger_middleware.IInjectRecognitionEvent
  {
    /**
     * Trigger a recognition event for the recognition session associated with
     * this object.
     * This invalidates the {@link IInjectRecognitionEvent}.
     * @param data the recognition data that the client of this model will receive
     * @param phraseExtras extra data only delivered for keyphrase models.
     */
    @Override public void triggerRecognitionEvent(byte[] data, android.media.soundtrigger.PhraseRecognitionExtra[] phraseExtras) throws android.os.RemoteException
    {
    }
    /**
     * Trigger an abort event for the recognition session associated with this object.
     * This invalidates the {@link IInjectRecognitionEvent}.
     */
    @Override public void triggerAbortRecognition() throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.media.soundtrigger_middleware.IInjectRecognitionEvent
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.media.soundtrigger_middleware.IInjectRecognitionEvent interface,
     * generating a proxy if needed.
     */
    public static android.media.soundtrigger_middleware.IInjectRecognitionEvent asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.media.soundtrigger_middleware.IInjectRecognitionEvent))) {
        return ((android.media.soundtrigger_middleware.IInjectRecognitionEvent)iin);
      }
      return new android.media.soundtrigger_middleware.IInjectRecognitionEvent.Stub.Proxy(obj);
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
        case TRANSACTION_triggerRecognitionEvent:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          android.media.soundtrigger.PhraseRecognitionExtra[] _arg1;
          _arg1 = data.createTypedArray(android.media.soundtrigger.PhraseRecognitionExtra.CREATOR);
          data.enforceNoDataAvail();
          this.triggerRecognitionEvent(_arg0, _arg1);
          break;
        }
        case TRANSACTION_triggerAbortRecognition:
        {
          this.triggerAbortRecognition();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.media.soundtrigger_middleware.IInjectRecognitionEvent
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
       * Trigger a recognition event for the recognition session associated with
       * this object.
       * This invalidates the {@link IInjectRecognitionEvent}.
       * @param data the recognition data that the client of this model will receive
       * @param phraseExtras extra data only delivered for keyphrase models.
       */
      @Override public void triggerRecognitionEvent(byte[] data, android.media.soundtrigger.PhraseRecognitionExtra[] phraseExtras) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(data);
          _data.writeTypedArray(phraseExtras, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_triggerRecognitionEvent, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Trigger an abort event for the recognition session associated with this object.
       * This invalidates the {@link IInjectRecognitionEvent}.
       */
      @Override public void triggerAbortRecognition() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_triggerAbortRecognition, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_triggerRecognitionEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_triggerAbortRecognition = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
  }
  public static final java.lang.String DESCRIPTOR = "android$media$soundtrigger_middleware$IInjectRecognitionEvent".replace('$', '.');
  /**
   * Trigger a recognition event for the recognition session associated with
   * this object.
   * This invalidates the {@link IInjectRecognitionEvent}.
   * @param data the recognition data that the client of this model will receive
   * @param phraseExtras extra data only delivered for keyphrase models.
   */
  public void triggerRecognitionEvent(byte[] data, android.media.soundtrigger.PhraseRecognitionExtra[] phraseExtras) throws android.os.RemoteException;
  /**
   * Trigger an abort event for the recognition session associated with this object.
   * This invalidates the {@link IInjectRecognitionEvent}.
   */
  public void triggerAbortRecognition() throws android.os.RemoteException;
}
