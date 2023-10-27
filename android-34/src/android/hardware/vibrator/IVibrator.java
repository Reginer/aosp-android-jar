/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.vibrator;
public interface IVibrator extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "ea8742d6993e1a82917da38b9938e537aa7fcb54";
  /** Default implementation for IVibrator. */
  public static class Default implements android.hardware.vibrator.IVibrator
  {
    @Override public int getCapabilities() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void off() throws android.os.RemoteException
    {
    }
    @Override public void on(int timeoutMs, android.hardware.vibrator.IVibratorCallback callback) throws android.os.RemoteException
    {
    }
    @Override public int perform(int effect, byte strength, android.hardware.vibrator.IVibratorCallback callback) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int[] getSupportedEffects() throws android.os.RemoteException
    {
      return null;
    }
    @Override public void setAmplitude(float amplitude) throws android.os.RemoteException
    {
    }
    @Override public void setExternalControl(boolean enabled) throws android.os.RemoteException
    {
    }
    @Override public int getCompositionDelayMax() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int getCompositionSizeMax() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int[] getSupportedPrimitives() throws android.os.RemoteException
    {
      return null;
    }
    @Override public int getPrimitiveDuration(int primitive) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void compose(android.hardware.vibrator.CompositeEffect[] composite, android.hardware.vibrator.IVibratorCallback callback) throws android.os.RemoteException
    {
    }
    @Override public int[] getSupportedAlwaysOnEffects() throws android.os.RemoteException
    {
      return null;
    }
    @Override public void alwaysOnEnable(int id, int effect, byte strength) throws android.os.RemoteException
    {
    }
    @Override public void alwaysOnDisable(int id) throws android.os.RemoteException
    {
    }
    @Override public float getResonantFrequency() throws android.os.RemoteException
    {
      return 0.0f;
    }
    @Override public float getQFactor() throws android.os.RemoteException
    {
      return 0.0f;
    }
    @Override public float getFrequencyResolution() throws android.os.RemoteException
    {
      return 0.0f;
    }
    @Override public float getFrequencyMinimum() throws android.os.RemoteException
    {
      return 0.0f;
    }
    @Override public float[] getBandwidthAmplitudeMap() throws android.os.RemoteException
    {
      return null;
    }
    @Override public int getPwlePrimitiveDurationMax() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int getPwleCompositionSizeMax() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int[] getSupportedBraking() throws android.os.RemoteException
    {
      return null;
    }
    @Override public void composePwle(android.hardware.vibrator.PrimitivePwle[] composite, android.hardware.vibrator.IVibratorCallback callback) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.vibrator.IVibrator
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.vibrator.IVibrator interface,
     * generating a proxy if needed.
     */
    public static android.hardware.vibrator.IVibrator asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.vibrator.IVibrator))) {
        return ((android.hardware.vibrator.IVibrator)iin);
      }
      return new android.hardware.vibrator.IVibrator.Stub.Proxy(obj);
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
        case TRANSACTION_getCapabilities:
        {
          int _result = this.getCapabilities();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_off:
        {
          this.off();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_on:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.vibrator.IVibratorCallback _arg1;
          _arg1 = android.hardware.vibrator.IVibratorCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.on(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_perform:
        {
          int _arg0;
          _arg0 = data.readInt();
          byte _arg1;
          _arg1 = data.readByte();
          android.hardware.vibrator.IVibratorCallback _arg2;
          _arg2 = android.hardware.vibrator.IVibratorCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          int _result = this.perform(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getSupportedEffects:
        {
          int[] _result = this.getSupportedEffects();
          reply.writeNoException();
          reply.writeIntArray(_result);
          break;
        }
        case TRANSACTION_setAmplitude:
        {
          float _arg0;
          _arg0 = data.readFloat();
          data.enforceNoDataAvail();
          this.setAmplitude(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setExternalControl:
        {
          boolean _arg0;
          _arg0 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setExternalControl(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getCompositionDelayMax:
        {
          int _result = this.getCompositionDelayMax();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getCompositionSizeMax:
        {
          int _result = this.getCompositionSizeMax();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getSupportedPrimitives:
        {
          int[] _result = this.getSupportedPrimitives();
          reply.writeNoException();
          reply.writeIntArray(_result);
          break;
        }
        case TRANSACTION_getPrimitiveDuration:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          int _result = this.getPrimitiveDuration(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_compose:
        {
          android.hardware.vibrator.CompositeEffect[] _arg0;
          _arg0 = data.createTypedArray(android.hardware.vibrator.CompositeEffect.CREATOR);
          android.hardware.vibrator.IVibratorCallback _arg1;
          _arg1 = android.hardware.vibrator.IVibratorCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.compose(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getSupportedAlwaysOnEffects:
        {
          int[] _result = this.getSupportedAlwaysOnEffects();
          reply.writeNoException();
          reply.writeIntArray(_result);
          break;
        }
        case TRANSACTION_alwaysOnEnable:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          byte _arg2;
          _arg2 = data.readByte();
          data.enforceNoDataAvail();
          this.alwaysOnEnable(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_alwaysOnDisable:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.alwaysOnDisable(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getResonantFrequency:
        {
          float _result = this.getResonantFrequency();
          reply.writeNoException();
          reply.writeFloat(_result);
          break;
        }
        case TRANSACTION_getQFactor:
        {
          float _result = this.getQFactor();
          reply.writeNoException();
          reply.writeFloat(_result);
          break;
        }
        case TRANSACTION_getFrequencyResolution:
        {
          float _result = this.getFrequencyResolution();
          reply.writeNoException();
          reply.writeFloat(_result);
          break;
        }
        case TRANSACTION_getFrequencyMinimum:
        {
          float _result = this.getFrequencyMinimum();
          reply.writeNoException();
          reply.writeFloat(_result);
          break;
        }
        case TRANSACTION_getBandwidthAmplitudeMap:
        {
          float[] _result = this.getBandwidthAmplitudeMap();
          reply.writeNoException();
          reply.writeFloatArray(_result);
          break;
        }
        case TRANSACTION_getPwlePrimitiveDurationMax:
        {
          int _result = this.getPwlePrimitiveDurationMax();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getPwleCompositionSizeMax:
        {
          int _result = this.getPwleCompositionSizeMax();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getSupportedBraking:
        {
          int[] _result = this.getSupportedBraking();
          reply.writeNoException();
          reply.writeIntArray(_result);
          break;
        }
        case TRANSACTION_composePwle:
        {
          android.hardware.vibrator.PrimitivePwle[] _arg0;
          _arg0 = data.createTypedArray(android.hardware.vibrator.PrimitivePwle.CREATOR);
          android.hardware.vibrator.IVibratorCallback _arg1;
          _arg1 = android.hardware.vibrator.IVibratorCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.composePwle(_arg0, _arg1);
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
    private static class Proxy implements android.hardware.vibrator.IVibrator
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
      @Override public int getCapabilities() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCapabilities, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getCapabilities is unimplemented.");
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
      @Override public void off() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_off, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method off is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void on(int timeoutMs, android.hardware.vibrator.IVibratorCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(timeoutMs);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_on, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method on is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public int perform(int effect, byte strength, android.hardware.vibrator.IVibratorCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(effect);
          _data.writeByte(strength);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_perform, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method perform is unimplemented.");
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
      @Override public int[] getSupportedEffects() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSupportedEffects, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getSupportedEffects is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createIntArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void setAmplitude(float amplitude) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloat(amplitude);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setAmplitude, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setAmplitude is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setExternalControl(boolean enabled) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeBoolean(enabled);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setExternalControl, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setExternalControl is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public int getCompositionDelayMax() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCompositionDelayMax, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getCompositionDelayMax is unimplemented.");
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
      @Override public int getCompositionSizeMax() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCompositionSizeMax, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getCompositionSizeMax is unimplemented.");
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
      @Override public int[] getSupportedPrimitives() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSupportedPrimitives, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getSupportedPrimitives is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createIntArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int getPrimitiveDuration(int primitive) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(primitive);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getPrimitiveDuration, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getPrimitiveDuration is unimplemented.");
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
      @Override public void compose(android.hardware.vibrator.CompositeEffect[] composite, android.hardware.vibrator.IVibratorCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedArray(composite, 0);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_compose, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method compose is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public int[] getSupportedAlwaysOnEffects() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSupportedAlwaysOnEffects, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getSupportedAlwaysOnEffects is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createIntArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void alwaysOnEnable(int id, int effect, byte strength) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(id);
          _data.writeInt(effect);
          _data.writeByte(strength);
          boolean _status = mRemote.transact(Stub.TRANSACTION_alwaysOnEnable, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method alwaysOnEnable is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void alwaysOnDisable(int id) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(id);
          boolean _status = mRemote.transact(Stub.TRANSACTION_alwaysOnDisable, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method alwaysOnDisable is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public float getResonantFrequency() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        float _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getResonantFrequency, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getResonantFrequency is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readFloat();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public float getQFactor() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        float _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getQFactor, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getQFactor is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readFloat();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public float getFrequencyResolution() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        float _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getFrequencyResolution, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getFrequencyResolution is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readFloat();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public float getFrequencyMinimum() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        float _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getFrequencyMinimum, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getFrequencyMinimum is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readFloat();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public float[] getBandwidthAmplitudeMap() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        float[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getBandwidthAmplitudeMap, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getBandwidthAmplitudeMap is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createFloatArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int getPwlePrimitiveDurationMax() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getPwlePrimitiveDurationMax, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getPwlePrimitiveDurationMax is unimplemented.");
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
      @Override public int getPwleCompositionSizeMax() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getPwleCompositionSizeMax, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getPwleCompositionSizeMax is unimplemented.");
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
      @Override public int[] getSupportedBraking() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSupportedBraking, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getSupportedBraking is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createIntArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void composePwle(android.hardware.vibrator.PrimitivePwle[] composite, android.hardware.vibrator.IVibratorCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedArray(composite, 0);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_composePwle, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method composePwle is unimplemented.");
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
    static final int TRANSACTION_getCapabilities = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_off = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_on = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_perform = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getSupportedEffects = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_setAmplitude = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_setExternalControl = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getCompositionDelayMax = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_getCompositionSizeMax = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_getSupportedPrimitives = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_getPrimitiveDuration = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_compose = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_getSupportedAlwaysOnEffects = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_alwaysOnEnable = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_alwaysOnDisable = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_getResonantFrequency = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_getQFactor = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_getFrequencyResolution = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_getFrequencyMinimum = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_getBandwidthAmplitudeMap = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
    static final int TRANSACTION_getPwlePrimitiveDurationMax = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
    static final int TRANSACTION_getPwleCompositionSizeMax = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
    static final int TRANSACTION_getSupportedBraking = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
    static final int TRANSACTION_composePwle = (android.os.IBinder.FIRST_CALL_TRANSACTION + 23);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$vibrator$IVibrator".replace('$', '.');
  public static final int CAP_ON_CALLBACK = 1;
  public static final int CAP_PERFORM_CALLBACK = 2;
  public static final int CAP_AMPLITUDE_CONTROL = 4;
  public static final int CAP_EXTERNAL_CONTROL = 8;
  public static final int CAP_EXTERNAL_AMPLITUDE_CONTROL = 16;
  public static final int CAP_COMPOSE_EFFECTS = 32;
  public static final int CAP_ALWAYS_ON_CONTROL = 64;
  public static final int CAP_GET_RESONANT_FREQUENCY = 128;
  public static final int CAP_GET_Q_FACTOR = 256;
  public static final int CAP_FREQUENCY_CONTROL = 512;
  public static final int CAP_COMPOSE_PWLE_EFFECTS = 1024;
  public int getCapabilities() throws android.os.RemoteException;
  public void off() throws android.os.RemoteException;
  public void on(int timeoutMs, android.hardware.vibrator.IVibratorCallback callback) throws android.os.RemoteException;
  public int perform(int effect, byte strength, android.hardware.vibrator.IVibratorCallback callback) throws android.os.RemoteException;
  public int[] getSupportedEffects() throws android.os.RemoteException;
  public void setAmplitude(float amplitude) throws android.os.RemoteException;
  public void setExternalControl(boolean enabled) throws android.os.RemoteException;
  public int getCompositionDelayMax() throws android.os.RemoteException;
  public int getCompositionSizeMax() throws android.os.RemoteException;
  public int[] getSupportedPrimitives() throws android.os.RemoteException;
  public int getPrimitiveDuration(int primitive) throws android.os.RemoteException;
  public void compose(android.hardware.vibrator.CompositeEffect[] composite, android.hardware.vibrator.IVibratorCallback callback) throws android.os.RemoteException;
  public int[] getSupportedAlwaysOnEffects() throws android.os.RemoteException;
  public void alwaysOnEnable(int id, int effect, byte strength) throws android.os.RemoteException;
  public void alwaysOnDisable(int id) throws android.os.RemoteException;
  public float getResonantFrequency() throws android.os.RemoteException;
  public float getQFactor() throws android.os.RemoteException;
  public float getFrequencyResolution() throws android.os.RemoteException;
  public float getFrequencyMinimum() throws android.os.RemoteException;
  public float[] getBandwidthAmplitudeMap() throws android.os.RemoteException;
  public int getPwlePrimitiveDurationMax() throws android.os.RemoteException;
  public int getPwleCompositionSizeMax() throws android.os.RemoteException;
  public int[] getSupportedBraking() throws android.os.RemoteException;
  public void composePwle(android.hardware.vibrator.PrimitivePwle[] composite, android.hardware.vibrator.IVibratorCallback callback) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
