/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.security.apc;
/**
 * This callback interface must be implemented by the client to receive the result of the user
 * confirmation.
 * @hide
 */
public interface IConfirmationCallback extends android.os.IInterface
{
  /** Default implementation for IConfirmationCallback. */
  public static class Default implements android.security.apc.IConfirmationCallback
  {
    /**
     * This callback gets called by the implementing service when a pending confirmation prompt
     * gets finalized.
     * 
     * @param result
     *  - ResponseCode.OK On success. In this case dataConfirmed must be non null.
     *  - ResponseCode.CANCELLED If the user cancelled the prompt. In this case dataConfirmed must
     *           be null.
     *  - ResponseCode.ABORTED If the client called IProtectedConfirmation.cancelPrompt() or if the
     *           prompt was cancelled by the system due to an asynchronous event. In this case
     *           dataConfirmed must be null.
     * 
     * @param dataConfirmed This is the message that was confirmed and for which a confirmation
     *           token is now available in implementing service. A subsequent attempt to sign this
     *           message with a confirmation bound key will succeed. The message is a CBOR map
     *           including the prompt text and the extra data.
     */
    @Override public void onCompleted(int result, byte[] dataConfirmed) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.security.apc.IConfirmationCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.security.apc.IConfirmationCallback interface,
     * generating a proxy if needed.
     */
    public static android.security.apc.IConfirmationCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.security.apc.IConfirmationCallback))) {
        return ((android.security.apc.IConfirmationCallback)iin);
      }
      return new android.security.apc.IConfirmationCallback.Stub.Proxy(obj);
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
        case TRANSACTION_onCompleted:
        {
          int _arg0;
          _arg0 = data.readInt();
          byte[] _arg1;
          _arg1 = data.createByteArray();
          data.enforceNoDataAvail();
          this.onCompleted(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.security.apc.IConfirmationCallback
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
       * This callback gets called by the implementing service when a pending confirmation prompt
       * gets finalized.
       * 
       * @param result
       *  - ResponseCode.OK On success. In this case dataConfirmed must be non null.
       *  - ResponseCode.CANCELLED If the user cancelled the prompt. In this case dataConfirmed must
       *           be null.
       *  - ResponseCode.ABORTED If the client called IProtectedConfirmation.cancelPrompt() or if the
       *           prompt was cancelled by the system due to an asynchronous event. In this case
       *           dataConfirmed must be null.
       * 
       * @param dataConfirmed This is the message that was confirmed and for which a confirmation
       *           token is now available in implementing service. A subsequent attempt to sign this
       *           message with a confirmation bound key will succeed. The message is a CBOR map
       *           including the prompt text and the extra data.
       */
      @Override public void onCompleted(int result, byte[] dataConfirmed) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(result);
          _data.writeByteArray(dataConfirmed);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onCompleted, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onCompleted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
  }
  public static final java.lang.String DESCRIPTOR = "android$security$apc$IConfirmationCallback".replace('$', '.');
  /**
   * This callback gets called by the implementing service when a pending confirmation prompt
   * gets finalized.
   * 
   * @param result
   *  - ResponseCode.OK On success. In this case dataConfirmed must be non null.
   *  - ResponseCode.CANCELLED If the user cancelled the prompt. In this case dataConfirmed must
   *           be null.
   *  - ResponseCode.ABORTED If the client called IProtectedConfirmation.cancelPrompt() or if the
   *           prompt was cancelled by the system due to an asynchronous event. In this case
   *           dataConfirmed must be null.
   * 
   * @param dataConfirmed This is the message that was confirmed and for which a confirmation
   *           token is now available in implementing service. A subsequent attempt to sign this
   *           message with a confirmation bound key will succeed. The message is a CBOR map
   *           including the prompt text and the extra data.
   */
  public void onCompleted(int result, byte[] dataConfirmed) throws android.os.RemoteException;
}
