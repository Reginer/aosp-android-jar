/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.tv.tunerresourcemanager;
/**
 * Interface to receive callbacks from ITunerResourceManager.
 * 
 * @hide
 */
public interface IResourcesReclaimListener extends android.os.IInterface
{
  /** Default implementation for IResourcesReclaimListener. */
  public static class Default implements android.media.tv.tunerresourcemanager.IResourcesReclaimListener
  {
    /**
     * TRM invokes this method when the client's resources need to be reclaimed.
     * 
     * <p>This method is implemented in Tuner Framework to take the reclaiming
     * actions. It's a synchronous call. TRM would wait on the call to finish
     * then grant the resource.
     */
    @Override public void onReclaimResources() throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.media.tv.tunerresourcemanager.IResourcesReclaimListener
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.media.tv.tunerresourcemanager.IResourcesReclaimListener interface,
     * generating a proxy if needed.
     */
    public static android.media.tv.tunerresourcemanager.IResourcesReclaimListener asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.media.tv.tunerresourcemanager.IResourcesReclaimListener))) {
        return ((android.media.tv.tunerresourcemanager.IResourcesReclaimListener)iin);
      }
      return new android.media.tv.tunerresourcemanager.IResourcesReclaimListener.Stub.Proxy(obj);
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
        case TRANSACTION_onReclaimResources:
        {
          this.onReclaimResources();
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
    private static class Proxy implements android.media.tv.tunerresourcemanager.IResourcesReclaimListener
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
       * TRM invokes this method when the client's resources need to be reclaimed.
       * 
       * <p>This method is implemented in Tuner Framework to take the reclaiming
       * actions. It's a synchronous call. TRM would wait on the call to finish
       * then grant the resource.
       */
      @Override public void onReclaimResources() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onReclaimResources, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onReclaimResources = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
  }
  public static final java.lang.String DESCRIPTOR = "android$media$tv$tunerresourcemanager$IResourcesReclaimListener".replace('$', '.');
  /**
   * TRM invokes this method when the client's resources need to be reclaimed.
   * 
   * <p>This method is implemented in Tuner Framework to take the reclaiming
   * actions. It's a synchronous call. TRM would wait on the call to finish
   * then grant the resource.
   */
  public void onReclaimResources() throws android.os.RemoteException;
}
