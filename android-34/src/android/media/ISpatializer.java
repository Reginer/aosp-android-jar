/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * The ISpatializer interface is used to control the native audio service implementation
 * of the spatializer stage with headtracking when present on a platform.
 * It is intended for exclusive use by the java AudioService running in system_server.
 * It provides APIs to discover the feature availability and options as well as control and report
 * the active state and modes of the spatializer and head tracking effect.
 * {@hide}
 */
public interface ISpatializer extends android.os.IInterface
{
  /** Default implementation for ISpatializer. */
  public static class Default implements android.media.ISpatializer
  {
    /** Releases a ISpatializer interface previously acquired. */
    @Override public void release() throws android.os.RemoteException
    {
    }
    /**
     * Reports the list of supported spatialization levels (see SpatializationLevel.aidl).
     * The list should never be empty if an ISpatializer interface was successfully
     * retrieved with IAudioPolicyService.getSpatializer().
     */
    @Override public byte[] getSupportedLevels() throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Selects the desired spatialization level (see SpatializationLevel.aidl). Selecting a level
     * different from SpatializationLevel.NONE with create the specialized multichannel output
     * mixer, create and enable the spatializer effect and let the audio policy attach eligible
     * AudioTrack to this output stream.
     */
    @Override public void setLevel(byte level) throws android.os.RemoteException
    {
    }
    /** Gets the selected spatialization level (see SpatializationLevel.aidl) */
    @Override public byte getLevel() throws android.os.RemoteException
    {
      return 0;
    }
    /**
     * Reports if the spatializer engine supports head tracking or not.
     * This is a pre condition independent of the fact that a head tracking sensor is
     * registered or not.
     */
    @Override public boolean isHeadTrackingSupported() throws android.os.RemoteException
    {
      return false;
    }
    /**
     * Reports the list of supported head tracking modes (see SpatializerHeadTrackingMode.aidl).
     * The list always contains SpatializerHeadTrackingMode.DISABLED and can include other modes
     * if the spatializer effect implementation supports head tracking.
     * The result does not depend on currently connected sensors but reflects the capabilities
     * when sensors are available.
     */
    @Override public byte[] getSupportedHeadTrackingModes() throws android.os.RemoteException
    {
      return null;
    }
    /** Selects the desired head tracking mode (see SpatializerHeadTrackingMode.aidl) */
    @Override public void setDesiredHeadTrackingMode(byte mode) throws android.os.RemoteException
    {
    }
    /**
     * Gets the actual head tracking mode. Can be different from the desired mode if conditions to
     * enable the desired mode are not met (e.g if the head tracking device was removed)
     */
    @Override public byte getActualHeadTrackingMode() throws android.os.RemoteException
    {
      return 0;
    }
    /** Reset the head tracking algorithm to consider current head pose as neutral */
    @Override public void recenterHeadTracker() throws android.os.RemoteException
    {
    }
    /**
     * Set the screen to stage transform to use by the head tracking algorithm
     * The screen to stage transform is conveyed as a vector of 6 elements,
     * where the first three are a translation vector and
     * the last three are a rotation vector.
     */
    @Override public void setGlobalTransform(float[] screenToStage) throws android.os.RemoteException
    {
    }
    /**
     * Set the sensor that is to be used for head-tracking.
     * -1 can be used to disable head-tracking.
     */
    @Override public void setHeadSensor(int sensorHandle) throws android.os.RemoteException
    {
    }
    /**
     * Set the sensor that is to be used for screen-tracking.
     * -1 can be used to disable screen-tracking.
     */
    @Override public void setScreenSensor(int sensorHandle) throws android.os.RemoteException
    {
    }
    /**
     * Sets the display orientation.
     * 
     * This is the rotation of the displayed content relative to its natural orientation.
     * 
     * Orientation is expressed in the angle of rotation from the physical "up" side of the screen
     * to the logical "up" side of the content displayed the screen. Counterclockwise angles, as
     * viewed while facing the screen are positive.
     * 
     * Note: DisplayManager currently only returns this in increments of 90 degrees,
     * so the values will be 0, PI/2, PI, 3PI/2.
     */
    @Override public void setDisplayOrientation(float physicalToLogicalAngle) throws android.os.RemoteException
    {
    }
    /**
     * Sets the hinge angle for foldable devices.
     * 
     * Per the hinge angle sensor, this returns a value from 0 to 2PI.
     * The value of 0 is considered closed, and PI is considered flat open.
     */
    @Override public void setHingeAngle(float hingeAngle) throws android.os.RemoteException
    {
    }
    /**
     * Sets whether a foldable is considered "folded" or not.
     * 
     * The fold state may affect which physical screen is active for display.
     */
    @Override public void setFoldState(boolean folded) throws android.os.RemoteException
    {
    }
    /**
     * Reports the list of supported spatialization modess (see SpatializationMode.aidl).
     * The list should never be empty if an ISpatializer interface was successfully
     * retrieved with IAudioPolicyService.getSpatializer().
     */
    @Override public byte[] getSupportedModes() throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Registers a callback to monitor head tracking functions.
     * Only one callback can be registered on a Spatializer.
     * The last callback registered wins and passing a nullptr unregisters
     * last registered callback.
     */
    @Override public void registerHeadTrackingCallback(android.media.ISpatializerHeadTrackingCallback callback) throws android.os.RemoteException
    {
    }
    /**
     * Sets a parameter to the spatializer engine. Used by effect implementor for vendor
     * specific configuration.
     */
    @Override public void setParameter(int key, byte[] value) throws android.os.RemoteException
    {
    }
    /**
     * Gets a parameter from the spatializer engine. Used by effect implementor for vendor
     * specific configuration.
     */
    @Override public void getParameter(int key, byte[] value) throws android.os.RemoteException
    {
    }
    /** Gets the io handle of the output stream the spatializer is connected to. */
    @Override public int getOutput() throws android.os.RemoteException
    {
      return 0;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.media.ISpatializer
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.media.ISpatializer interface,
     * generating a proxy if needed.
     */
    public static android.media.ISpatializer asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.media.ISpatializer))) {
        return ((android.media.ISpatializer)iin);
      }
      return new android.media.ISpatializer.Stub.Proxy(obj);
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
        case TRANSACTION_release:
        {
          this.release();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getSupportedLevels:
        {
          byte[] _result = this.getSupportedLevels();
          reply.writeNoException();
          reply.writeByteArray(_result);
          break;
        }
        case TRANSACTION_setLevel:
        {
          byte _arg0;
          _arg0 = data.readByte();
          data.enforceNoDataAvail();
          this.setLevel(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getLevel:
        {
          byte _result = this.getLevel();
          reply.writeNoException();
          reply.writeByte(_result);
          break;
        }
        case TRANSACTION_isHeadTrackingSupported:
        {
          boolean _result = this.isHeadTrackingSupported();
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_getSupportedHeadTrackingModes:
        {
          byte[] _result = this.getSupportedHeadTrackingModes();
          reply.writeNoException();
          reply.writeByteArray(_result);
          break;
        }
        case TRANSACTION_setDesiredHeadTrackingMode:
        {
          byte _arg0;
          _arg0 = data.readByte();
          data.enforceNoDataAvail();
          this.setDesiredHeadTrackingMode(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getActualHeadTrackingMode:
        {
          byte _result = this.getActualHeadTrackingMode();
          reply.writeNoException();
          reply.writeByte(_result);
          break;
        }
        case TRANSACTION_recenterHeadTracker:
        {
          this.recenterHeadTracker();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setGlobalTransform:
        {
          float[] _arg0;
          _arg0 = data.createFloatArray();
          data.enforceNoDataAvail();
          this.setGlobalTransform(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setHeadSensor:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.setHeadSensor(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setScreenSensor:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.setScreenSensor(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setDisplayOrientation:
        {
          float _arg0;
          _arg0 = data.readFloat();
          data.enforceNoDataAvail();
          this.setDisplayOrientation(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setHingeAngle:
        {
          float _arg0;
          _arg0 = data.readFloat();
          data.enforceNoDataAvail();
          this.setHingeAngle(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setFoldState:
        {
          boolean _arg0;
          _arg0 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setFoldState(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getSupportedModes:
        {
          byte[] _result = this.getSupportedModes();
          reply.writeNoException();
          reply.writeByteArray(_result);
          break;
        }
        case TRANSACTION_registerHeadTrackingCallback:
        {
          android.media.ISpatializerHeadTrackingCallback _arg0;
          _arg0 = android.media.ISpatializerHeadTrackingCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.registerHeadTrackingCallback(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setParameter:
        {
          int _arg0;
          _arg0 = data.readInt();
          byte[] _arg1;
          _arg1 = data.createByteArray();
          data.enforceNoDataAvail();
          this.setParameter(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getParameter:
        {
          int _arg0;
          _arg0 = data.readInt();
          byte[] _arg1;
          _arg1 = data.createByteArray();
          data.enforceNoDataAvail();
          this.getParameter(_arg0, _arg1);
          reply.writeNoException();
          reply.writeByteArray(_arg1);
          break;
        }
        case TRANSACTION_getOutput:
        {
          int _result = this.getOutput();
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
    private static class Proxy implements android.media.ISpatializer
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
      /** Releases a ISpatializer interface previously acquired. */
      @Override public void release() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_release, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Reports the list of supported spatialization levels (see SpatializationLevel.aidl).
       * The list should never be empty if an ISpatializer interface was successfully
       * retrieved with IAudioPolicyService.getSpatializer().
       */
      @Override public byte[] getSupportedLevels() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        byte[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSupportedLevels, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createByteArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Selects the desired spatialization level (see SpatializationLevel.aidl). Selecting a level
       * different from SpatializationLevel.NONE with create the specialized multichannel output
       * mixer, create and enable the spatializer effect and let the audio policy attach eligible
       * AudioTrack to this output stream.
       */
      @Override public void setLevel(byte level) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(level);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setLevel, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Gets the selected spatialization level (see SpatializationLevel.aidl) */
      @Override public byte getLevel() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        byte _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getLevel, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readByte();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Reports if the spatializer engine supports head tracking or not.
       * This is a pre condition independent of the fact that a head tracking sensor is
       * registered or not.
       */
      @Override public boolean isHeadTrackingSupported() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isHeadTrackingSupported, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readBoolean();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Reports the list of supported head tracking modes (see SpatializerHeadTrackingMode.aidl).
       * The list always contains SpatializerHeadTrackingMode.DISABLED and can include other modes
       * if the spatializer effect implementation supports head tracking.
       * The result does not depend on currently connected sensors but reflects the capabilities
       * when sensors are available.
       */
      @Override public byte[] getSupportedHeadTrackingModes() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        byte[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSupportedHeadTrackingModes, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createByteArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Selects the desired head tracking mode (see SpatializerHeadTrackingMode.aidl) */
      @Override public void setDesiredHeadTrackingMode(byte mode) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(mode);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setDesiredHeadTrackingMode, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Gets the actual head tracking mode. Can be different from the desired mode if conditions to
       * enable the desired mode are not met (e.g if the head tracking device was removed)
       */
      @Override public byte getActualHeadTrackingMode() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        byte _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getActualHeadTrackingMode, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readByte();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Reset the head tracking algorithm to consider current head pose as neutral */
      @Override public void recenterHeadTracker() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_recenterHeadTracker, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Set the screen to stage transform to use by the head tracking algorithm
       * The screen to stage transform is conveyed as a vector of 6 elements,
       * where the first three are a translation vector and
       * the last three are a rotation vector.
       */
      @Override public void setGlobalTransform(float[] screenToStage) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloatArray(screenToStage);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setGlobalTransform, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Set the sensor that is to be used for head-tracking.
       * -1 can be used to disable head-tracking.
       */
      @Override public void setHeadSensor(int sensorHandle) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sensorHandle);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setHeadSensor, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Set the sensor that is to be used for screen-tracking.
       * -1 can be used to disable screen-tracking.
       */
      @Override public void setScreenSensor(int sensorHandle) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sensorHandle);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setScreenSensor, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Sets the display orientation.
       * 
       * This is the rotation of the displayed content relative to its natural orientation.
       * 
       * Orientation is expressed in the angle of rotation from the physical "up" side of the screen
       * to the logical "up" side of the content displayed the screen. Counterclockwise angles, as
       * viewed while facing the screen are positive.
       * 
       * Note: DisplayManager currently only returns this in increments of 90 degrees,
       * so the values will be 0, PI/2, PI, 3PI/2.
       */
      @Override public void setDisplayOrientation(float physicalToLogicalAngle) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloat(physicalToLogicalAngle);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setDisplayOrientation, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Sets the hinge angle for foldable devices.
       * 
       * Per the hinge angle sensor, this returns a value from 0 to 2PI.
       * The value of 0 is considered closed, and PI is considered flat open.
       */
      @Override public void setHingeAngle(float hingeAngle) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloat(hingeAngle);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setHingeAngle, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Sets whether a foldable is considered "folded" or not.
       * 
       * The fold state may affect which physical screen is active for display.
       */
      @Override public void setFoldState(boolean folded) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeBoolean(folded);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setFoldState, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Reports the list of supported spatialization modess (see SpatializationMode.aidl).
       * The list should never be empty if an ISpatializer interface was successfully
       * retrieved with IAudioPolicyService.getSpatializer().
       */
      @Override public byte[] getSupportedModes() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        byte[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSupportedModes, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createByteArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Registers a callback to monitor head tracking functions.
       * Only one callback can be registered on a Spatializer.
       * The last callback registered wins and passing a nullptr unregisters
       * last registered callback.
       */
      @Override public void registerHeadTrackingCallback(android.media.ISpatializerHeadTrackingCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerHeadTrackingCallback, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Sets a parameter to the spatializer engine. Used by effect implementor for vendor
       * specific configuration.
       */
      @Override public void setParameter(int key, byte[] value) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(key);
          _data.writeByteArray(value);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setParameter, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Gets a parameter from the spatializer engine. Used by effect implementor for vendor
       * specific configuration.
       */
      @Override public void getParameter(int key, byte[] value) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(key);
          _data.writeByteArray(value);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getParameter, _data, _reply, 0);
          _reply.readException();
          _reply.readByteArray(value);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Gets the io handle of the output stream the spatializer is connected to. */
      @Override public int getOutput() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getOutput, _data, _reply, 0);
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
    static final int TRANSACTION_release = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_getSupportedLevels = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_setLevel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_getLevel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_isHeadTrackingSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getSupportedHeadTrackingModes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_setDesiredHeadTrackingMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getActualHeadTrackingMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_recenterHeadTracker = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_setGlobalTransform = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_setHeadSensor = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_setScreenSensor = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_setDisplayOrientation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_setHingeAngle = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_setFoldState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_getSupportedModes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_registerHeadTrackingCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_setParameter = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_getParameter = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_getOutput = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
  }
  public static final java.lang.String DESCRIPTOR = "android$media$ISpatializer".replace('$', '.');
  /** Releases a ISpatializer interface previously acquired. */
  public void release() throws android.os.RemoteException;
  /**
   * Reports the list of supported spatialization levels (see SpatializationLevel.aidl).
   * The list should never be empty if an ISpatializer interface was successfully
   * retrieved with IAudioPolicyService.getSpatializer().
   */
  public byte[] getSupportedLevels() throws android.os.RemoteException;
  /**
   * Selects the desired spatialization level (see SpatializationLevel.aidl). Selecting a level
   * different from SpatializationLevel.NONE with create the specialized multichannel output
   * mixer, create and enable the spatializer effect and let the audio policy attach eligible
   * AudioTrack to this output stream.
   */
  public void setLevel(byte level) throws android.os.RemoteException;
  /** Gets the selected spatialization level (see SpatializationLevel.aidl) */
  public byte getLevel() throws android.os.RemoteException;
  /**
   * Reports if the spatializer engine supports head tracking or not.
   * This is a pre condition independent of the fact that a head tracking sensor is
   * registered or not.
   */
  public boolean isHeadTrackingSupported() throws android.os.RemoteException;
  /**
   * Reports the list of supported head tracking modes (see SpatializerHeadTrackingMode.aidl).
   * The list always contains SpatializerHeadTrackingMode.DISABLED and can include other modes
   * if the spatializer effect implementation supports head tracking.
   * The result does not depend on currently connected sensors but reflects the capabilities
   * when sensors are available.
   */
  public byte[] getSupportedHeadTrackingModes() throws android.os.RemoteException;
  /** Selects the desired head tracking mode (see SpatializerHeadTrackingMode.aidl) */
  public void setDesiredHeadTrackingMode(byte mode) throws android.os.RemoteException;
  /**
   * Gets the actual head tracking mode. Can be different from the desired mode if conditions to
   * enable the desired mode are not met (e.g if the head tracking device was removed)
   */
  public byte getActualHeadTrackingMode() throws android.os.RemoteException;
  /** Reset the head tracking algorithm to consider current head pose as neutral */
  public void recenterHeadTracker() throws android.os.RemoteException;
  /**
   * Set the screen to stage transform to use by the head tracking algorithm
   * The screen to stage transform is conveyed as a vector of 6 elements,
   * where the first three are a translation vector and
   * the last three are a rotation vector.
   */
  public void setGlobalTransform(float[] screenToStage) throws android.os.RemoteException;
  /**
   * Set the sensor that is to be used for head-tracking.
   * -1 can be used to disable head-tracking.
   */
  public void setHeadSensor(int sensorHandle) throws android.os.RemoteException;
  /**
   * Set the sensor that is to be used for screen-tracking.
   * -1 can be used to disable screen-tracking.
   */
  public void setScreenSensor(int sensorHandle) throws android.os.RemoteException;
  /**
   * Sets the display orientation.
   * 
   * This is the rotation of the displayed content relative to its natural orientation.
   * 
   * Orientation is expressed in the angle of rotation from the physical "up" side of the screen
   * to the logical "up" side of the content displayed the screen. Counterclockwise angles, as
   * viewed while facing the screen are positive.
   * 
   * Note: DisplayManager currently only returns this in increments of 90 degrees,
   * so the values will be 0, PI/2, PI, 3PI/2.
   */
  public void setDisplayOrientation(float physicalToLogicalAngle) throws android.os.RemoteException;
  /**
   * Sets the hinge angle for foldable devices.
   * 
   * Per the hinge angle sensor, this returns a value from 0 to 2PI.
   * The value of 0 is considered closed, and PI is considered flat open.
   */
  public void setHingeAngle(float hingeAngle) throws android.os.RemoteException;
  /**
   * Sets whether a foldable is considered "folded" or not.
   * 
   * The fold state may affect which physical screen is active for display.
   */
  public void setFoldState(boolean folded) throws android.os.RemoteException;
  /**
   * Reports the list of supported spatialization modess (see SpatializationMode.aidl).
   * The list should never be empty if an ISpatializer interface was successfully
   * retrieved with IAudioPolicyService.getSpatializer().
   */
  public byte[] getSupportedModes() throws android.os.RemoteException;
  /**
   * Registers a callback to monitor head tracking functions.
   * Only one callback can be registered on a Spatializer.
   * The last callback registered wins and passing a nullptr unregisters
   * last registered callback.
   */
  public void registerHeadTrackingCallback(android.media.ISpatializerHeadTrackingCallback callback) throws android.os.RemoteException;
  /**
   * Sets a parameter to the spatializer engine. Used by effect implementor for vendor
   * specific configuration.
   */
  public void setParameter(int key, byte[] value) throws android.os.RemoteException;
  /**
   * Gets a parameter from the spatializer engine. Used by effect implementor for vendor
   * specific configuration.
   */
  public void getParameter(int key, byte[] value) throws android.os.RemoteException;
  /** Gets the io handle of the output stream the spatializer is connected to. */
  public int getOutput() throws android.os.RemoteException;
}
