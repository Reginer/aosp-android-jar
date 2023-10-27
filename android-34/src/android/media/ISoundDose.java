/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * Interface used to push the sound dose related information from the
 * AudioService#SoundDoseHelper to the audio server
 */
public interface ISoundDose extends android.os.IInterface
{
  /** Default implementation for ISoundDose. */
  public static class Default implements android.media.ISoundDose
  {
    /** Set a new RS2 upper bound used for momentary exposure warnings. */
    @Override public void setOutputRs2UpperBound(float rs2Value) throws android.os.RemoteException
    {
    }
    /**
     * Resets the native CSD values. This can happen after a crash in the
     * audio server or after booting when restoring the previous state.
     * 'currentCsd' represents the restored CSD value and 'records' contains the
     * dosage values and MELs together with their timestamps that lead to this
     * CSD.
     */
    @Override public void resetCsd(float currentCsd, android.media.SoundDoseRecord[] records) throws android.os.RemoteException
    {
    }
    /**
     * Updates the attenuation used for the MEL calculation when the volume is
     * not applied by the audio framework. This can be the case when for example
     * the absolute volume is used for a particular device.
     * 
     * @param attenuationDB the attenuation as a negative value in dB that will
     *                      be applied for the internal MEL when computing CSD.
     *                      A value of 0 represents no attenuation for the MELs
     * @param device        the audio_devices_t type for which we will apply the
     *                      attenuation
     */
    @Override public void updateAttenuation(float attenuationDB, int device) throws android.os.RemoteException
    {
    }
    /**
     * Enables/disables the calculation of sound dose. This has the effect that
     * if disabled no MEL values will be computed on the framework side. The MEL
     * returned from the IHalSoundDoseCallbacks will be ignored.
     */
    @Override public void setCsdEnabled(boolean enabled) throws android.os.RemoteException
    {
    }
    /**
     * -------------------------- Test API methods --------------------------
     * /** Get the currently used RS2 upper bound.
     */
    @Override public float getOutputRs2UpperBound() throws android.os.RemoteException
    {
      return 0.0f;
    }
    /** Get the current CSD from audioserver. */
    @Override public float getCsd() throws android.os.RemoteException
    {
      return 0.0f;
    }
    /**
     * Returns true if the HAL supports the ISoundDose interface. Can be either
     * as part of IModule or standalon sound dose HAL.
     */
    @Override public boolean isSoundDoseHalSupported() throws android.os.RemoteException
    {
      return false;
    }
    /** Enables/Disables MEL computations from framework. */
    @Override public void forceUseFrameworkMel(boolean useFrameworkMel) throws android.os.RemoteException
    {
    }
    /** Enables/Disables the computation of CSD on all devices. */
    @Override public void forceComputeCsdOnAllDevices(boolean computeCsdOnAllDevices) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.media.ISoundDose
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.media.ISoundDose interface,
     * generating a proxy if needed.
     */
    public static android.media.ISoundDose asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.media.ISoundDose))) {
        return ((android.media.ISoundDose)iin);
      }
      return new android.media.ISoundDose.Stub.Proxy(obj);
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
        case TRANSACTION_setOutputRs2UpperBound:
        {
          float _arg0;
          _arg0 = data.readFloat();
          data.enforceNoDataAvail();
          this.setOutputRs2UpperBound(_arg0);
          break;
        }
        case TRANSACTION_resetCsd:
        {
          float _arg0;
          _arg0 = data.readFloat();
          android.media.SoundDoseRecord[] _arg1;
          _arg1 = data.createTypedArray(android.media.SoundDoseRecord.CREATOR);
          data.enforceNoDataAvail();
          this.resetCsd(_arg0, _arg1);
          break;
        }
        case TRANSACTION_updateAttenuation:
        {
          float _arg0;
          _arg0 = data.readFloat();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.updateAttenuation(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setCsdEnabled:
        {
          boolean _arg0;
          _arg0 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setCsdEnabled(_arg0);
          break;
        }
        case TRANSACTION_getOutputRs2UpperBound:
        {
          float _result = this.getOutputRs2UpperBound();
          reply.writeNoException();
          reply.writeFloat(_result);
          break;
        }
        case TRANSACTION_getCsd:
        {
          float _result = this.getCsd();
          reply.writeNoException();
          reply.writeFloat(_result);
          break;
        }
        case TRANSACTION_isSoundDoseHalSupported:
        {
          boolean _result = this.isSoundDoseHalSupported();
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_forceUseFrameworkMel:
        {
          boolean _arg0;
          _arg0 = data.readBoolean();
          data.enforceNoDataAvail();
          this.forceUseFrameworkMel(_arg0);
          break;
        }
        case TRANSACTION_forceComputeCsdOnAllDevices:
        {
          boolean _arg0;
          _arg0 = data.readBoolean();
          data.enforceNoDataAvail();
          this.forceComputeCsdOnAllDevices(_arg0);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.media.ISoundDose
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
      /** Set a new RS2 upper bound used for momentary exposure warnings. */
      @Override public void setOutputRs2UpperBound(float rs2Value) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloat(rs2Value);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setOutputRs2UpperBound, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Resets the native CSD values. This can happen after a crash in the
       * audio server or after booting when restoring the previous state.
       * 'currentCsd' represents the restored CSD value and 'records' contains the
       * dosage values and MELs together with their timestamps that lead to this
       * CSD.
       */
      @Override public void resetCsd(float currentCsd, android.media.SoundDoseRecord[] records) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloat(currentCsd);
          _data.writeTypedArray(records, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_resetCsd, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Updates the attenuation used for the MEL calculation when the volume is
       * not applied by the audio framework. This can be the case when for example
       * the absolute volume is used for a particular device.
       * 
       * @param attenuationDB the attenuation as a negative value in dB that will
       *                      be applied for the internal MEL when computing CSD.
       *                      A value of 0 represents no attenuation for the MELs
       * @param device        the audio_devices_t type for which we will apply the
       *                      attenuation
       */
      @Override public void updateAttenuation(float attenuationDB, int device) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloat(attenuationDB);
          _data.writeInt(device);
          boolean _status = mRemote.transact(Stub.TRANSACTION_updateAttenuation, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Enables/disables the calculation of sound dose. This has the effect that
       * if disabled no MEL values will be computed on the framework side. The MEL
       * returned from the IHalSoundDoseCallbacks will be ignored.
       */
      @Override public void setCsdEnabled(boolean enabled) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeBoolean(enabled);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCsdEnabled, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * -------------------------- Test API methods --------------------------
       * /** Get the currently used RS2 upper bound.
       */
      @Override public float getOutputRs2UpperBound() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        float _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getOutputRs2UpperBound, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readFloat();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Get the current CSD from audioserver. */
      @Override public float getCsd() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        float _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCsd, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readFloat();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Returns true if the HAL supports the ISoundDose interface. Can be either
       * as part of IModule or standalon sound dose HAL.
       */
      @Override public boolean isSoundDoseHalSupported() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isSoundDoseHalSupported, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readBoolean();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Enables/Disables MEL computations from framework. */
      @Override public void forceUseFrameworkMel(boolean useFrameworkMel) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeBoolean(useFrameworkMel);
          boolean _status = mRemote.transact(Stub.TRANSACTION_forceUseFrameworkMel, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Enables/Disables the computation of CSD on all devices. */
      @Override public void forceComputeCsdOnAllDevices(boolean computeCsdOnAllDevices) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeBoolean(computeCsdOnAllDevices);
          boolean _status = mRemote.transact(Stub.TRANSACTION_forceComputeCsdOnAllDevices, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_setOutputRs2UpperBound = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_resetCsd = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_updateAttenuation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_setCsdEnabled = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getOutputRs2UpperBound = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getCsd = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_isSoundDoseHalSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_forceUseFrameworkMel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_forceComputeCsdOnAllDevices = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
  }
  public static final java.lang.String DESCRIPTOR = "android$media$ISoundDose".replace('$', '.');
  /** Set a new RS2 upper bound used for momentary exposure warnings. */
  public void setOutputRs2UpperBound(float rs2Value) throws android.os.RemoteException;
  /**
   * Resets the native CSD values. This can happen after a crash in the
   * audio server or after booting when restoring the previous state.
   * 'currentCsd' represents the restored CSD value and 'records' contains the
   * dosage values and MELs together with their timestamps that lead to this
   * CSD.
   */
  public void resetCsd(float currentCsd, android.media.SoundDoseRecord[] records) throws android.os.RemoteException;
  /**
   * Updates the attenuation used for the MEL calculation when the volume is
   * not applied by the audio framework. This can be the case when for example
   * the absolute volume is used for a particular device.
   * 
   * @param attenuationDB the attenuation as a negative value in dB that will
   *                      be applied for the internal MEL when computing CSD.
   *                      A value of 0 represents no attenuation for the MELs
   * @param device        the audio_devices_t type for which we will apply the
   *                      attenuation
   */
  public void updateAttenuation(float attenuationDB, int device) throws android.os.RemoteException;
  /**
   * Enables/disables the calculation of sound dose. This has the effect that
   * if disabled no MEL values will be computed on the framework side. The MEL
   * returned from the IHalSoundDoseCallbacks will be ignored.
   */
  public void setCsdEnabled(boolean enabled) throws android.os.RemoteException;
  /**
   * -------------------------- Test API methods --------------------------
   * /** Get the currently used RS2 upper bound.
   */
  public float getOutputRs2UpperBound() throws android.os.RemoteException;
  /** Get the current CSD from audioserver. */
  public float getCsd() throws android.os.RemoteException;
  /**
   * Returns true if the HAL supports the ISoundDose interface. Can be either
   * as part of IModule or standalon sound dose HAL.
   */
  public boolean isSoundDoseHalSupported() throws android.os.RemoteException;
  /** Enables/Disables MEL computations from framework. */
  public void forceUseFrameworkMel(boolean useFrameworkMel) throws android.os.RemoteException;
  /** Enables/Disables the computation of CSD on all devices. */
  public void forceComputeCsdOnAllDevices(boolean computeCsdOnAllDevices) throws android.os.RemoteException;
}
