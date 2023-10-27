/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * ITranscodingClientCallback
 * 
 * Interface for the MediaTranscodingService to communicate with the client.
 * 
 * {@hide}
 */
public interface ITranscodingClientCallback extends android.os.IInterface
{
  /** Default implementation for ITranscodingClientCallback. */
  public static class Default implements android.media.ITranscodingClientCallback
  {
    /**
     * Called to open a raw file descriptor to access data under a URI
     * 
     * @param fileUri The path of the filename.
     * @param mode The file mode to use. Must be one of ("r, "w", "rw")
     * @return ParcelFileDescriptor if open the file successfully, null otherwise.
     */
    @Override public android.os.ParcelFileDescriptor openFileDescriptor(java.lang.String fileUri, java.lang.String mode) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Called when the transcoding associated with the sessionId finished.
     * This will only be called if client request to get all the status of the session.
     * 
     * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
     */
    @Override public void onTranscodingStarted(int sessionId) throws android.os.RemoteException
    {
    }
    /**
     * Called when the transcoding associated with the sessionId is paused.
     * This will only be called if client request to get all the status of the session.
     * 
     * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
     */
    @Override public void onTranscodingPaused(int sessionId) throws android.os.RemoteException
    {
    }
    /**
     * Called when the transcoding associated with the sessionId is resumed.
     * This will only be called if client request to get all the status of the session.
     * 
     * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
     */
    @Override public void onTranscodingResumed(int sessionId) throws android.os.RemoteException
    {
    }
    /**
     * Called when the transcoding associated with the sessionId finished.
     * 
     * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
     * @param result contains the transcoded file stats and other transcoding metrics if requested.
     */
    @Override public void onTranscodingFinished(int sessionId, android.media.TranscodingResultParcel result) throws android.os.RemoteException
    {
    }
    /**
     * Called when the transcoding associated with the sessionId failed.
     * 
     * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
     * @param errorCode error code that indicates the error.
     */
    @Override public void onTranscodingFailed(int sessionId, int errorCode) throws android.os.RemoteException
    {
    }
    /**
     * Called when the transcoding configuration associated with the sessionId gets updated, i.e. wait
     * number in the session queue.
     * 
     * <p> This will only be called if client set requestUpdate to be true in the TranscodingRequest
     * submitted to the MediaTranscodingService.
     * 
     * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
     * @param oldAwaitNumber previous number of sessions ahead of current session.
     * @param newAwaitNumber updated number of sessions ahead of current session.
     */
    @Override public void onAwaitNumberOfSessionsChanged(int sessionId, int oldAwaitNumber, int newAwaitNumber) throws android.os.RemoteException
    {
    }
    /**
     * Called when there is an update on the progress of the TranscodingSession.
     * 
     * <p> This will only be called if client set requestUpdate to be true in the TranscodingRequest
     * submitted to the MediaTranscodingService.
     * 
     * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
     * @param progress an integer number ranging from 0 ~ 100 inclusive.
     */
    @Override public void onProgressUpdate(int sessionId, int progress) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.media.ITranscodingClientCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.media.ITranscodingClientCallback interface,
     * generating a proxy if needed.
     */
    public static android.media.ITranscodingClientCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.media.ITranscodingClientCallback))) {
        return ((android.media.ITranscodingClientCallback)iin);
      }
      return new android.media.ITranscodingClientCallback.Stub.Proxy(obj);
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
        case TRANSACTION_openFileDescriptor:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          android.os.ParcelFileDescriptor _result = this.openFileDescriptor(_arg0, _arg1);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_onTranscodingStarted:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.onTranscodingStarted(_arg0);
          break;
        }
        case TRANSACTION_onTranscodingPaused:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.onTranscodingPaused(_arg0);
          break;
        }
        case TRANSACTION_onTranscodingResumed:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.onTranscodingResumed(_arg0);
          break;
        }
        case TRANSACTION_onTranscodingFinished:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.media.TranscodingResultParcel _arg1;
          _arg1 = data.readTypedObject(android.media.TranscodingResultParcel.CREATOR);
          this.onTranscodingFinished(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onTranscodingFailed:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          this.onTranscodingFailed(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onAwaitNumberOfSessionsChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          this.onAwaitNumberOfSessionsChanged(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_onProgressUpdate:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          this.onProgressUpdate(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.media.ITranscodingClientCallback
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
       * Called to open a raw file descriptor to access data under a URI
       * 
       * @param fileUri The path of the filename.
       * @param mode The file mode to use. Must be one of ("r, "w", "rw")
       * @return ParcelFileDescriptor if open the file successfully, null otherwise.
       */
      @Override public android.os.ParcelFileDescriptor openFileDescriptor(java.lang.String fileUri, java.lang.String mode) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.os.ParcelFileDescriptor _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(fileUri);
          _data.writeString(mode);
          boolean _status = mRemote.transact(Stub.TRANSACTION_openFileDescriptor, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readTypedObject(android.os.ParcelFileDescriptor.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Called when the transcoding associated with the sessionId finished.
       * This will only be called if client request to get all the status of the session.
       * 
       * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
       */
      @Override public void onTranscodingStarted(int sessionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sessionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onTranscodingStarted, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Called when the transcoding associated with the sessionId is paused.
       * This will only be called if client request to get all the status of the session.
       * 
       * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
       */
      @Override public void onTranscodingPaused(int sessionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sessionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onTranscodingPaused, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Called when the transcoding associated with the sessionId is resumed.
       * This will only be called if client request to get all the status of the session.
       * 
       * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
       */
      @Override public void onTranscodingResumed(int sessionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sessionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onTranscodingResumed, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Called when the transcoding associated with the sessionId finished.
       * 
       * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
       * @param result contains the transcoded file stats and other transcoding metrics if requested.
       */
      @Override public void onTranscodingFinished(int sessionId, android.media.TranscodingResultParcel result) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sessionId);
          _data.writeTypedObject(result, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onTranscodingFinished, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Called when the transcoding associated with the sessionId failed.
       * 
       * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
       * @param errorCode error code that indicates the error.
       */
      @Override public void onTranscodingFailed(int sessionId, int errorCode) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sessionId);
          _data.writeInt(errorCode);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onTranscodingFailed, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Called when the transcoding configuration associated with the sessionId gets updated, i.e. wait
       * number in the session queue.
       * 
       * <p> This will only be called if client set requestUpdate to be true in the TranscodingRequest
       * submitted to the MediaTranscodingService.
       * 
       * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
       * @param oldAwaitNumber previous number of sessions ahead of current session.
       * @param newAwaitNumber updated number of sessions ahead of current session.
       */
      @Override public void onAwaitNumberOfSessionsChanged(int sessionId, int oldAwaitNumber, int newAwaitNumber) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sessionId);
          _data.writeInt(oldAwaitNumber);
          _data.writeInt(newAwaitNumber);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAwaitNumberOfSessionsChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Called when there is an update on the progress of the TranscodingSession.
       * 
       * <p> This will only be called if client set requestUpdate to be true in the TranscodingRequest
       * submitted to the MediaTranscodingService.
       * 
       * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
       * @param progress an integer number ranging from 0 ~ 100 inclusive.
       */
      @Override public void onProgressUpdate(int sessionId, int progress) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sessionId);
          _data.writeInt(progress);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onProgressUpdate, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_openFileDescriptor = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onTranscodingStarted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_onTranscodingPaused = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_onTranscodingResumed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_onTranscodingFinished = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_onTranscodingFailed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_onAwaitNumberOfSessionsChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_onProgressUpdate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
  }
  public static final java.lang.String DESCRIPTOR = "android$media$ITranscodingClientCallback".replace('$', '.');
  /**
   * Called to open a raw file descriptor to access data under a URI
   * 
   * @param fileUri The path of the filename.
   * @param mode The file mode to use. Must be one of ("r, "w", "rw")
   * @return ParcelFileDescriptor if open the file successfully, null otherwise.
   */
  public android.os.ParcelFileDescriptor openFileDescriptor(java.lang.String fileUri, java.lang.String mode) throws android.os.RemoteException;
  /**
   * Called when the transcoding associated with the sessionId finished.
   * This will only be called if client request to get all the status of the session.
   * 
   * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
   */
  public void onTranscodingStarted(int sessionId) throws android.os.RemoteException;
  /**
   * Called when the transcoding associated with the sessionId is paused.
   * This will only be called if client request to get all the status of the session.
   * 
   * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
   */
  public void onTranscodingPaused(int sessionId) throws android.os.RemoteException;
  /**
   * Called when the transcoding associated with the sessionId is resumed.
   * This will only be called if client request to get all the status of the session.
   * 
   * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
   */
  public void onTranscodingResumed(int sessionId) throws android.os.RemoteException;
  /**
   * Called when the transcoding associated with the sessionId finished.
   * 
   * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
   * @param result contains the transcoded file stats and other transcoding metrics if requested.
   */
  public void onTranscodingFinished(int sessionId, android.media.TranscodingResultParcel result) throws android.os.RemoteException;
  /**
   * Called when the transcoding associated with the sessionId failed.
   * 
   * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
   * @param errorCode error code that indicates the error.
   */
  public void onTranscodingFailed(int sessionId, int errorCode) throws android.os.RemoteException;
  /**
   * Called when the transcoding configuration associated with the sessionId gets updated, i.e. wait
   * number in the session queue.
   * 
   * <p> This will only be called if client set requestUpdate to be true in the TranscodingRequest
   * submitted to the MediaTranscodingService.
   * 
   * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
   * @param oldAwaitNumber previous number of sessions ahead of current session.
   * @param newAwaitNumber updated number of sessions ahead of current session.
   */
  public void onAwaitNumberOfSessionsChanged(int sessionId, int oldAwaitNumber, int newAwaitNumber) throws android.os.RemoteException;
  /**
   * Called when there is an update on the progress of the TranscodingSession.
   * 
   * <p> This will only be called if client set requestUpdate to be true in the TranscodingRequest
   * submitted to the MediaTranscodingService.
   * 
   * @param sessionId sessionId assigned by the MediaTranscodingService upon receiving request.
   * @param progress an integer number ranging from 0 ~ 100 inclusive.
   */
  public void onProgressUpdate(int sessionId, int progress) throws android.os.RemoteException;
}
