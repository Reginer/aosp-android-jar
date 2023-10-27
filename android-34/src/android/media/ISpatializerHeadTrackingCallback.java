/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * The ISpatializerHeadTrackingCallback interface is a callback associated to the
 * Spatializer head tracking function. It can be registered via the ISpatializer
 * interface to monitor head tracking related states.
 * {@hide}
 */
public interface ISpatializerHeadTrackingCallback extends android.os.IInterface
{
  /** Default implementation for ISpatializerHeadTrackingCallback. */
  public static class Default implements android.media.ISpatializerHeadTrackingCallback
  {
    /** Called when the head tracking mode has changed */
    @Override public void onHeadTrackingModeChanged(byte mode) throws android.os.RemoteException
    {
    }
    /**
     * Called when the head to stage pose hase been updated
     * The head to stage pose is conveyed as a vector of 6 elements,
     * where the first three are a translation vector and
     * the last three are a rotation vector.
     */
    @Override public void onHeadToSoundStagePoseUpdated(float[] headToStage) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.media.ISpatializerHeadTrackingCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.media.ISpatializerHeadTrackingCallback interface,
     * generating a proxy if needed.
     */
    public static android.media.ISpatializerHeadTrackingCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.media.ISpatializerHeadTrackingCallback))) {
        return ((android.media.ISpatializerHeadTrackingCallback)iin);
      }
      return new android.media.ISpatializerHeadTrackingCallback.Stub.Proxy(obj);
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
        case TRANSACTION_onHeadTrackingModeChanged:
        {
          byte _arg0;
          _arg0 = data.readByte();
          data.enforceNoDataAvail();
          this.onHeadTrackingModeChanged(_arg0);
          break;
        }
        case TRANSACTION_onHeadToSoundStagePoseUpdated:
        {
          float[] _arg0;
          _arg0 = data.createFloatArray();
          data.enforceNoDataAvail();
          this.onHeadToSoundStagePoseUpdated(_arg0);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.media.ISpatializerHeadTrackingCallback
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
      /** Called when the head tracking mode has changed */
      @Override public void onHeadTrackingModeChanged(byte mode) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(mode);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onHeadTrackingModeChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Called when the head to stage pose hase been updated
       * The head to stage pose is conveyed as a vector of 6 elements,
       * where the first three are a translation vector and
       * the last three are a rotation vector.
       */
      @Override public void onHeadToSoundStagePoseUpdated(float[] headToStage) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloatArray(headToStage);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onHeadToSoundStagePoseUpdated, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onHeadTrackingModeChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onHeadToSoundStagePoseUpdated = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
  }
  public static final java.lang.String DESCRIPTOR = "android$media$ISpatializerHeadTrackingCallback".replace('$', '.');
  /** Called when the head tracking mode has changed */
  public void onHeadTrackingModeChanged(byte mode) throws android.os.RemoteException;
  /**
   * Called when the head to stage pose hase been updated
   * The head to stage pose is conveyed as a vector of 6 elements,
   * where the first three are a translation vector and
   * the last three are a rotation vector.
   */
  public void onHeadToSoundStagePoseUpdated(float[] headToStage) throws android.os.RemoteException;
}
