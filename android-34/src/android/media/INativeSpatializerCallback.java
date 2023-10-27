/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * The INativeSpatializerCallback interface is a callback associated to the
 * ISpatializer interface. The callback is used by the spatializer
 * implementation in native audio server to communicate state changes to the
 * client controlling the spatializer with the ISpatializer interface.
 * {@hide}
 */
public interface INativeSpatializerCallback extends android.os.IInterface
{
  /** Default implementation for INativeSpatializerCallback. */
  public static class Default implements android.media.INativeSpatializerCallback
  {
    /**
     * Called when the spatialization level applied by the spatializer changes
     * (e.g. when the spatializer is enabled or disabled)
     */
    @Override public void onLevelChanged(byte level) throws android.os.RemoteException
    {
    }
    /**
     * Called when the output stream the Spatializer is attached to changes.
     * Indicates the IO Handle of the new output.
     */
    @Override public void onOutputChanged(int output) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.media.INativeSpatializerCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.media.INativeSpatializerCallback interface,
     * generating a proxy if needed.
     */
    public static android.media.INativeSpatializerCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.media.INativeSpatializerCallback))) {
        return ((android.media.INativeSpatializerCallback)iin);
      }
      return new android.media.INativeSpatializerCallback.Stub.Proxy(obj);
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
        case TRANSACTION_onLevelChanged:
        {
          byte _arg0;
          _arg0 = data.readByte();
          data.enforceNoDataAvail();
          this.onLevelChanged(_arg0);
          break;
        }
        case TRANSACTION_onOutputChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.onOutputChanged(_arg0);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.media.INativeSpatializerCallback
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
       * Called when the spatialization level applied by the spatializer changes
       * (e.g. when the spatializer is enabled or disabled)
       */
      @Override public void onLevelChanged(byte level) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(level);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onLevelChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Called when the output stream the Spatializer is attached to changes.
       * Indicates the IO Handle of the new output.
       */
      @Override public void onOutputChanged(int output) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(output);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onOutputChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onLevelChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onOutputChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
  }
  public static final java.lang.String DESCRIPTOR = "android$media$INativeSpatializerCallback".replace('$', '.');
  /**
   * Called when the spatialization level applied by the spatializer changes
   * (e.g. when the spatializer is enabled or disabled)
   */
  public void onLevelChanged(byte level) throws android.os.RemoteException;
  /**
   * Called when the output stream the Spatializer is attached to changes.
   * Indicates the IO Handle of the new output.
   */
  public void onOutputChanged(int output) throws android.os.RemoteException;
}
