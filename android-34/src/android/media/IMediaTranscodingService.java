/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * Binder interface for MediaTranscodingService.
 * 
 * {@hide}
 */
public interface IMediaTranscodingService extends android.os.IInterface
{
  /** Default implementation for IMediaTranscodingService. */
  public static class Default implements android.media.IMediaTranscodingService
  {
    /**
     * Register the client with the MediaTranscodingService.
     * 
     * Client must call this function to register itself with the service in
     * order to perform transcoding tasks. This function will return an
     * ITranscodingClient interface object. The client should save and use it
     * for all future transactions with the service.
     * 
     * @param callback client interface for the MediaTranscodingService to call
     *        the client.
     * @param clientName name of the client.
     * @param opPackageName op package name of the client.
     * @return an ITranscodingClient interface object, with nullptr indicating
     *         failure to register.
     */
    @Override public android.media.ITranscodingClient registerClient(android.media.ITranscodingClientCallback callback, java.lang.String clientName, java.lang.String opPackageName) throws android.os.RemoteException
    {
      return null;
    }
    /** Returns the number of clients. This is used for debugging. */
    @Override public int getNumOfClients() throws android.os.RemoteException
    {
      return 0;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.media.IMediaTranscodingService
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.media.IMediaTranscodingService interface,
     * generating a proxy if needed.
     */
    public static android.media.IMediaTranscodingService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.media.IMediaTranscodingService))) {
        return ((android.media.IMediaTranscodingService)iin);
      }
      return new android.media.IMediaTranscodingService.Stub.Proxy(obj);
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
        case TRANSACTION_registerClient:
        {
          android.media.ITranscodingClientCallback _arg0;
          _arg0 = android.media.ITranscodingClientCallback.Stub.asInterface(data.readStrongBinder());
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          android.media.ITranscodingClient _result = this.registerClient(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_getNumOfClients:
        {
          int _result = this.getNumOfClients();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.media.IMediaTranscodingService
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
       * Register the client with the MediaTranscodingService.
       * 
       * Client must call this function to register itself with the service in
       * order to perform transcoding tasks. This function will return an
       * ITranscodingClient interface object. The client should save and use it
       * for all future transactions with the service.
       * 
       * @param callback client interface for the MediaTranscodingService to call
       *        the client.
       * @param clientName name of the client.
       * @param opPackageName op package name of the client.
       * @return an ITranscodingClient interface object, with nullptr indicating
       *         failure to register.
       */
      @Override public android.media.ITranscodingClient registerClient(android.media.ITranscodingClientCallback callback, java.lang.String clientName, java.lang.String opPackageName) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.ITranscodingClient _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          _data.writeString(clientName);
          _data.writeString(opPackageName);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerClient, _data, _reply, 0);
          _reply.readException();
          _result = android.media.ITranscodingClient.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Returns the number of clients. This is used for debugging. */
      @Override public int getNumOfClients() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getNumOfClients, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_registerClient = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_getNumOfClients = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
  }
  public static final java.lang.String DESCRIPTOR = "android$media$IMediaTranscodingService".replace('$', '.');
  /**
   * All MediaTranscoding service and device Binder calls may return a
   * ServiceSpecificException with the following error codes
   */
  public static final int ERROR_PERMISSION_DENIED = 1;
  public static final int ERROR_ALREADY_EXISTS = 2;
  public static final int ERROR_ILLEGAL_ARGUMENT = 3;
  public static final int ERROR_DISCONNECTED = 4;
  public static final int ERROR_TIMED_OUT = 5;
  public static final int ERROR_DISABLED = 6;
  public static final int ERROR_INVALID_OPERATION = 7;
  /**
   * Default UID/PID values for non-privileged callers of
   * registerClient().
   */
  public static final int USE_CALLING_UID = -1;
  public static final int USE_CALLING_PID = -1;
  /**
   * Register the client with the MediaTranscodingService.
   * 
   * Client must call this function to register itself with the service in
   * order to perform transcoding tasks. This function will return an
   * ITranscodingClient interface object. The client should save and use it
   * for all future transactions with the service.
   * 
   * @param callback client interface for the MediaTranscodingService to call
   *        the client.
   * @param clientName name of the client.
   * @param opPackageName op package name of the client.
   * @return an ITranscodingClient interface object, with nullptr indicating
   *         failure to register.
   */
  public android.media.ITranscodingClient registerClient(android.media.ITranscodingClientCallback callback, java.lang.String clientName, java.lang.String opPackageName) throws android.os.RemoteException;
  /** Returns the number of clients. This is used for debugging. */
  public int getNumOfClients() throws android.os.RemoteException;
}
