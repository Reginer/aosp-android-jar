/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * Interface used to push the sound dose related information from the audio
 * server to the AudioService#SoundDoseHelper.
 */
public interface ISoundDoseCallback extends android.os.IInterface
{
  /** Default implementation for ISoundDoseCallback. */
  public static class Default implements android.media.ISoundDoseCallback
  {
    /** Called whenever the momentary exposure exceeds the RS2 value. */
    @Override public void onMomentaryExposure(float currentMel, int deviceId) throws android.os.RemoteException
    {
    }
    /**
     * Notifies that the CSD value has changed. The currentCsd is normalized
     * with value 1 representing 100% of sound dose. SoundDoseRecord represents
     * the newest record that lead to the new currentCsd.
     */
    @Override public void onNewCsdValue(float currentCsd, android.media.SoundDoseRecord[] records) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.media.ISoundDoseCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.media.ISoundDoseCallback interface,
     * generating a proxy if needed.
     */
    public static android.media.ISoundDoseCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.media.ISoundDoseCallback))) {
        return ((android.media.ISoundDoseCallback)iin);
      }
      return new android.media.ISoundDoseCallback.Stub.Proxy(obj);
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
        case TRANSACTION_onMomentaryExposure:
        {
          float _arg0;
          _arg0 = data.readFloat();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.onMomentaryExposure(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onNewCsdValue:
        {
          float _arg0;
          _arg0 = data.readFloat();
          android.media.SoundDoseRecord[] _arg1;
          _arg1 = data.createTypedArray(android.media.SoundDoseRecord.CREATOR);
          data.enforceNoDataAvail();
          this.onNewCsdValue(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.media.ISoundDoseCallback
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
      /** Called whenever the momentary exposure exceeds the RS2 value. */
      @Override public void onMomentaryExposure(float currentMel, int deviceId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloat(currentMel);
          _data.writeInt(deviceId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onMomentaryExposure, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Notifies that the CSD value has changed. The currentCsd is normalized
       * with value 1 representing 100% of sound dose. SoundDoseRecord represents
       * the newest record that lead to the new currentCsd.
       */
      @Override public void onNewCsdValue(float currentCsd, android.media.SoundDoseRecord[] records) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloat(currentCsd);
          _data.writeTypedArray(records, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onNewCsdValue, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onMomentaryExposure = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onNewCsdValue = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
  }
  public static final java.lang.String DESCRIPTOR = "android$media$ISoundDoseCallback".replace('$', '.');
  /** Called whenever the momentary exposure exceeds the RS2 value. */
  public void onMomentaryExposure(float currentMel, int deviceId) throws android.os.RemoteException;
  /**
   * Notifies that the CSD value has changed. The currentCsd is normalized
   * with value 1 representing 100% of sound dose. SoundDoseRecord represents
   * the newest record that lead to the new currentCsd.
   */
  public void onNewCsdValue(float currentCsd, android.media.SoundDoseRecord[] records) throws android.os.RemoteException;
}
