/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.soundtrigger3;
public interface ISoundTriggerHw extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 1;
  public static final String HASH = "7d8d63478cd50e766d2072140c8aa3457f9fb585";
  /** Default implementation for ISoundTriggerHw. */
  public static class Default implements android.hardware.soundtrigger3.ISoundTriggerHw
  {
    @Override public android.media.soundtrigger.Properties getProperties() throws android.os.RemoteException
    {
      return null;
    }
    @Override public void registerGlobalCallback(android.hardware.soundtrigger3.ISoundTriggerHwGlobalCallback callback) throws android.os.RemoteException
    {
    }
    @Override public int loadSoundModel(android.media.soundtrigger.SoundModel soundModel, android.hardware.soundtrigger3.ISoundTriggerHwCallback callback) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int loadPhraseSoundModel(android.media.soundtrigger.PhraseSoundModel soundModel, android.hardware.soundtrigger3.ISoundTriggerHwCallback callback) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void unloadSoundModel(int modelHandle) throws android.os.RemoteException
    {
    }
    @Override public void startRecognition(int modelHandle, int deviceHandle, int ioHandle, android.media.soundtrigger.RecognitionConfig config) throws android.os.RemoteException
    {
    }
    @Override public void stopRecognition(int modelHandle) throws android.os.RemoteException
    {
    }
    @Override public void forceRecognitionEvent(int modelHandle) throws android.os.RemoteException
    {
    }
    @Override public android.media.soundtrigger.ModelParameterRange queryParameter(int modelHandle, int modelParam) throws android.os.RemoteException
    {
      return null;
    }
    @Override public int getParameter(int modelHandle, int modelParam) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void setParameter(int modelHandle, int modelParam, int value) throws android.os.RemoteException
    {
    }
    @Override
    public int getInterfaceVersion() {
      return 0;
    }
    @Override
    public String getInterfaceHash() {
      return "";
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.hardware.soundtrigger3.ISoundTriggerHw
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.soundtrigger3.ISoundTriggerHw interface,
     * generating a proxy if needed.
     */
    public static android.hardware.soundtrigger3.ISoundTriggerHw asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.soundtrigger3.ISoundTriggerHw))) {
        return ((android.hardware.soundtrigger3.ISoundTriggerHw)iin);
      }
      return new android.hardware.soundtrigger3.ISoundTriggerHw.Stub.Proxy(obj);
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
        case TRANSACTION_getInterfaceVersion:
        {
          reply.writeNoException();
          reply.writeInt(getInterfaceVersion());
          return true;
        }
        case TRANSACTION_getInterfaceHash:
        {
          reply.writeNoException();
          reply.writeString(getInterfaceHash());
          return true;
        }
      }
      switch (code)
      {
        case TRANSACTION_getProperties:
        {
          android.media.soundtrigger.Properties _result = this.getProperties();
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_registerGlobalCallback:
        {
          android.hardware.soundtrigger3.ISoundTriggerHwGlobalCallback _arg0;
          _arg0 = android.hardware.soundtrigger3.ISoundTriggerHwGlobalCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.registerGlobalCallback(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_loadSoundModel:
        {
          android.media.soundtrigger.SoundModel _arg0;
          _arg0 = data.readTypedObject(android.media.soundtrigger.SoundModel.CREATOR);
          android.hardware.soundtrigger3.ISoundTriggerHwCallback _arg1;
          _arg1 = android.hardware.soundtrigger3.ISoundTriggerHwCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          int _result = this.loadSoundModel(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_loadPhraseSoundModel:
        {
          android.media.soundtrigger.PhraseSoundModel _arg0;
          _arg0 = data.readTypedObject(android.media.soundtrigger.PhraseSoundModel.CREATOR);
          android.hardware.soundtrigger3.ISoundTriggerHwCallback _arg1;
          _arg1 = android.hardware.soundtrigger3.ISoundTriggerHwCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          int _result = this.loadPhraseSoundModel(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_unloadSoundModel:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.unloadSoundModel(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_startRecognition:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          android.media.soundtrigger.RecognitionConfig _arg3;
          _arg3 = data.readTypedObject(android.media.soundtrigger.RecognitionConfig.CREATOR);
          data.enforceNoDataAvail();
          this.startRecognition(_arg0, _arg1, _arg2, _arg3);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_stopRecognition:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.stopRecognition(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_forceRecognitionEvent:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.forceRecognitionEvent(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_queryParameter:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          android.media.soundtrigger.ModelParameterRange _result = this.queryParameter(_arg0, _arg1);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getParameter:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          int _result = this.getParameter(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_setParameter:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.setParameter(_arg0, _arg1, _arg2);
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
    private static class Proxy implements android.hardware.soundtrigger3.ISoundTriggerHw
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      private int mCachedVersion = -1;
      private String mCachedHash = "-1";
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public android.media.soundtrigger.Properties getProperties() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.soundtrigger.Properties _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getProperties, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getProperties is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.media.soundtrigger.Properties.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void registerGlobalCallback(android.hardware.soundtrigger3.ISoundTriggerHwGlobalCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerGlobalCallback, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method registerGlobalCallback is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public int loadSoundModel(android.media.soundtrigger.SoundModel soundModel, android.hardware.soundtrigger3.ISoundTriggerHwCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(soundModel, 0);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_loadSoundModel, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method loadSoundModel is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int loadPhraseSoundModel(android.media.soundtrigger.PhraseSoundModel soundModel, android.hardware.soundtrigger3.ISoundTriggerHwCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(soundModel, 0);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_loadPhraseSoundModel, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method loadPhraseSoundModel is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void unloadSoundModel(int modelHandle) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(modelHandle);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unloadSoundModel, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method unloadSoundModel is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void startRecognition(int modelHandle, int deviceHandle, int ioHandle, android.media.soundtrigger.RecognitionConfig config) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(modelHandle);
          _data.writeInt(deviceHandle);
          _data.writeInt(ioHandle);
          _data.writeTypedObject(config, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startRecognition, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method startRecognition is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void stopRecognition(int modelHandle) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(modelHandle);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stopRecognition, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method stopRecognition is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void forceRecognitionEvent(int modelHandle) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(modelHandle);
          boolean _status = mRemote.transact(Stub.TRANSACTION_forceRecognitionEvent, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method forceRecognitionEvent is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public android.media.soundtrigger.ModelParameterRange queryParameter(int modelHandle, int modelParam) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.soundtrigger.ModelParameterRange _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(modelHandle);
          _data.writeInt(modelParam);
          boolean _status = mRemote.transact(Stub.TRANSACTION_queryParameter, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method queryParameter is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.media.soundtrigger.ModelParameterRange.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int getParameter(int modelHandle, int modelParam) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(modelHandle);
          _data.writeInt(modelParam);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getParameter, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getParameter is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void setParameter(int modelHandle, int modelParam, int value) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(modelHandle);
          _data.writeInt(modelParam);
          _data.writeInt(value);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setParameter, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setParameter is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override
      public int getInterfaceVersion() throws android.os.RemoteException {
        if (mCachedVersion == -1) {
          android.os.Parcel data = android.os.Parcel.obtain(asBinder());
          android.os.Parcel reply = android.os.Parcel.obtain();
          try {
            data.writeInterfaceToken(DESCRIPTOR);
            boolean _status = mRemote.transact(Stub.TRANSACTION_getInterfaceVersion, data, reply, 0);
            reply.readException();
            mCachedVersion = reply.readInt();
          } finally {
            reply.recycle();
            data.recycle();
          }
        }
        return mCachedVersion;
      }
      @Override
      public synchronized String getInterfaceHash() throws android.os.RemoteException {
        if ("-1".equals(mCachedHash)) {
          android.os.Parcel data = android.os.Parcel.obtain(asBinder());
          android.os.Parcel reply = android.os.Parcel.obtain();
          try {
            data.writeInterfaceToken(DESCRIPTOR);
            boolean _status = mRemote.transact(Stub.TRANSACTION_getInterfaceHash, data, reply, 0);
            reply.readException();
            mCachedHash = reply.readString();
          } finally {
            reply.recycle();
            data.recycle();
          }
        }
        return mCachedHash;
      }
    }
    static final int TRANSACTION_getProperties = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_registerGlobalCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_loadSoundModel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_loadPhraseSoundModel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_unloadSoundModel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_startRecognition = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_stopRecognition = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_forceRecognitionEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_queryParameter = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_getParameter = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_setParameter = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$soundtrigger3$ISoundTriggerHw".replace('$', '.');
  public android.media.soundtrigger.Properties getProperties() throws android.os.RemoteException;
  public void registerGlobalCallback(android.hardware.soundtrigger3.ISoundTriggerHwGlobalCallback callback) throws android.os.RemoteException;
  public int loadSoundModel(android.media.soundtrigger.SoundModel soundModel, android.hardware.soundtrigger3.ISoundTriggerHwCallback callback) throws android.os.RemoteException;
  public int loadPhraseSoundModel(android.media.soundtrigger.PhraseSoundModel soundModel, android.hardware.soundtrigger3.ISoundTriggerHwCallback callback) throws android.os.RemoteException;
  public void unloadSoundModel(int modelHandle) throws android.os.RemoteException;
  public void startRecognition(int modelHandle, int deviceHandle, int ioHandle, android.media.soundtrigger.RecognitionConfig config) throws android.os.RemoteException;
  public void stopRecognition(int modelHandle) throws android.os.RemoteException;
  public void forceRecognitionEvent(int modelHandle) throws android.os.RemoteException;
  public android.media.soundtrigger.ModelParameterRange queryParameter(int modelHandle, int modelParam) throws android.os.RemoteException;
  public int getParameter(int modelHandle, int modelParam) throws android.os.RemoteException;
  public void setParameter(int modelHandle, int modelParam, int value) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
