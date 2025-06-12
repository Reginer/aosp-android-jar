/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 4c4fc474c40b64963eb8d78b713b1095fecd72f0 -t --stability vintf --min_sdk_version platform_apis --ninja -d out/soong/.intermediates/hardware/interfaces/thermal/aidl/android.hardware.thermal-V3-java-source/gen/android/hardware/thermal/IThermal.java.d -o out/soong/.intermediates/hardware/interfaces/thermal/aidl/android.hardware.thermal-V3-java-source/gen -Nhardware/interfaces/thermal/aidl/aidl_api/android.hardware.thermal/3 hardware/interfaces/thermal/aidl/aidl_api/android.hardware.thermal/3/android/hardware/thermal/IThermal.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.thermal;
/** @hide */
public interface IThermal extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 3;
  public static final String HASH = "4c4fc474c40b64963eb8d78b713b1095fecd72f0";
  /** Default implementation for IThermal. */
  public static class Default implements android.hardware.thermal.IThermal
  {
    @Override public android.hardware.thermal.CoolingDevice[] getCoolingDevices() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.thermal.CoolingDevice[] getCoolingDevicesWithType(int type) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.thermal.Temperature[] getTemperatures() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.thermal.Temperature[] getTemperaturesWithType(int type) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.thermal.TemperatureThreshold[] getTemperatureThresholds() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.thermal.TemperatureThreshold[] getTemperatureThresholdsWithType(int type) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void registerThermalChangedCallback(android.hardware.thermal.IThermalChangedCallback callback) throws android.os.RemoteException
    {
    }
    @Override public void registerThermalChangedCallbackWithType(android.hardware.thermal.IThermalChangedCallback callback, int type) throws android.os.RemoteException
    {
    }
    @Override public void unregisterThermalChangedCallback(android.hardware.thermal.IThermalChangedCallback callback) throws android.os.RemoteException
    {
    }
    @Override public void registerCoolingDeviceChangedCallbackWithType(android.hardware.thermal.ICoolingDeviceChangedCallback callback, int type) throws android.os.RemoteException
    {
    }
    @Override public void unregisterCoolingDeviceChangedCallback(android.hardware.thermal.ICoolingDeviceChangedCallback callback) throws android.os.RemoteException
    {
    }
    @Override public float forecastSkinTemperature(int forecastSeconds) throws android.os.RemoteException
    {
      return 0.0f;
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.thermal.IThermal
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.thermal.IThermal interface,
     * generating a proxy if needed.
     */
    public static android.hardware.thermal.IThermal asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.thermal.IThermal))) {
        return ((android.hardware.thermal.IThermal)iin);
      }
      return new android.hardware.thermal.IThermal.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    /** @hide */
    public static java.lang.String getDefaultTransactionName(int transactionCode)
    {
      switch (transactionCode)
      {
        case TRANSACTION_getCoolingDevices:
        {
          return "getCoolingDevices";
        }
        case TRANSACTION_getCoolingDevicesWithType:
        {
          return "getCoolingDevicesWithType";
        }
        case TRANSACTION_getTemperatures:
        {
          return "getTemperatures";
        }
        case TRANSACTION_getTemperaturesWithType:
        {
          return "getTemperaturesWithType";
        }
        case TRANSACTION_getTemperatureThresholds:
        {
          return "getTemperatureThresholds";
        }
        case TRANSACTION_getTemperatureThresholdsWithType:
        {
          return "getTemperatureThresholdsWithType";
        }
        case TRANSACTION_registerThermalChangedCallback:
        {
          return "registerThermalChangedCallback";
        }
        case TRANSACTION_registerThermalChangedCallbackWithType:
        {
          return "registerThermalChangedCallbackWithType";
        }
        case TRANSACTION_unregisterThermalChangedCallback:
        {
          return "unregisterThermalChangedCallback";
        }
        case TRANSACTION_registerCoolingDeviceChangedCallbackWithType:
        {
          return "registerCoolingDeviceChangedCallbackWithType";
        }
        case TRANSACTION_unregisterCoolingDeviceChangedCallback:
        {
          return "unregisterCoolingDeviceChangedCallback";
        }
        case TRANSACTION_forecastSkinTemperature:
        {
          return "forecastSkinTemperature";
        }
        case TRANSACTION_getInterfaceVersion:
        {
          return "getInterfaceVersion";
        }
        case TRANSACTION_getInterfaceHash:
        {
          return "getInterfaceHash";
        }
        default:
        {
          return null;
        }
      }
    }
    /** @hide */
    public java.lang.String getTransactionName(int transactionCode)
    {
      return this.getDefaultTransactionName(transactionCode);
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      if (code == INTERFACE_TRANSACTION) {
        reply.writeString(descriptor);
        return true;
      }
      else if (code == TRANSACTION_getInterfaceVersion) {
        reply.writeNoException();
        reply.writeInt(getInterfaceVersion());
        return true;
      }
      else if (code == TRANSACTION_getInterfaceHash) {
        reply.writeNoException();
        reply.writeString(getInterfaceHash());
        return true;
      }
      switch (code)
      {
        case TRANSACTION_getCoolingDevices:
        {
          android.hardware.thermal.CoolingDevice[] _result = this.getCoolingDevices();
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getCoolingDevicesWithType:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          android.hardware.thermal.CoolingDevice[] _result = this.getCoolingDevicesWithType(_arg0);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getTemperatures:
        {
          android.hardware.thermal.Temperature[] _result = this.getTemperatures();
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getTemperaturesWithType:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          android.hardware.thermal.Temperature[] _result = this.getTemperaturesWithType(_arg0);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getTemperatureThresholds:
        {
          android.hardware.thermal.TemperatureThreshold[] _result = this.getTemperatureThresholds();
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getTemperatureThresholdsWithType:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          android.hardware.thermal.TemperatureThreshold[] _result = this.getTemperatureThresholdsWithType(_arg0);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_registerThermalChangedCallback:
        {
          android.hardware.thermal.IThermalChangedCallback _arg0;
          _arg0 = android.hardware.thermal.IThermalChangedCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.registerThermalChangedCallback(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_registerThermalChangedCallbackWithType:
        {
          android.hardware.thermal.IThermalChangedCallback _arg0;
          _arg0 = android.hardware.thermal.IThermalChangedCallback.Stub.asInterface(data.readStrongBinder());
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.registerThermalChangedCallbackWithType(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_unregisterThermalChangedCallback:
        {
          android.hardware.thermal.IThermalChangedCallback _arg0;
          _arg0 = android.hardware.thermal.IThermalChangedCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.unregisterThermalChangedCallback(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_registerCoolingDeviceChangedCallbackWithType:
        {
          android.hardware.thermal.ICoolingDeviceChangedCallback _arg0;
          _arg0 = android.hardware.thermal.ICoolingDeviceChangedCallback.Stub.asInterface(data.readStrongBinder());
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.registerCoolingDeviceChangedCallbackWithType(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_unregisterCoolingDeviceChangedCallback:
        {
          android.hardware.thermal.ICoolingDeviceChangedCallback _arg0;
          _arg0 = android.hardware.thermal.ICoolingDeviceChangedCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.unregisterCoolingDeviceChangedCallback(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_forecastSkinTemperature:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          float _result = this.forecastSkinTemperature(_arg0);
          reply.writeNoException();
          reply.writeFloat(_result);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.thermal.IThermal
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
      @Override public android.hardware.thermal.CoolingDevice[] getCoolingDevices() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.thermal.CoolingDevice[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCoolingDevices, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getCoolingDevices is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createTypedArray(android.hardware.thermal.CoolingDevice.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.thermal.CoolingDevice[] getCoolingDevicesWithType(int type) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.thermal.CoolingDevice[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCoolingDevicesWithType, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getCoolingDevicesWithType is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createTypedArray(android.hardware.thermal.CoolingDevice.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.thermal.Temperature[] getTemperatures() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.thermal.Temperature[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getTemperatures, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getTemperatures is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createTypedArray(android.hardware.thermal.Temperature.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.thermal.Temperature[] getTemperaturesWithType(int type) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.thermal.Temperature[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getTemperaturesWithType, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getTemperaturesWithType is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createTypedArray(android.hardware.thermal.Temperature.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.thermal.TemperatureThreshold[] getTemperatureThresholds() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.thermal.TemperatureThreshold[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getTemperatureThresholds, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getTemperatureThresholds is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createTypedArray(android.hardware.thermal.TemperatureThreshold.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.thermal.TemperatureThreshold[] getTemperatureThresholdsWithType(int type) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.thermal.TemperatureThreshold[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getTemperatureThresholdsWithType, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getTemperatureThresholdsWithType is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createTypedArray(android.hardware.thermal.TemperatureThreshold.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void registerThermalChangedCallback(android.hardware.thermal.IThermalChangedCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerThermalChangedCallback, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method registerThermalChangedCallback is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void registerThermalChangedCallbackWithType(android.hardware.thermal.IThermalChangedCallback callback, int type) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          _data.writeInt(type);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerThermalChangedCallbackWithType, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method registerThermalChangedCallbackWithType is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void unregisterThermalChangedCallback(android.hardware.thermal.IThermalChangedCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterThermalChangedCallback, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method unregisterThermalChangedCallback is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void registerCoolingDeviceChangedCallbackWithType(android.hardware.thermal.ICoolingDeviceChangedCallback callback, int type) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          _data.writeInt(type);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerCoolingDeviceChangedCallbackWithType, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method registerCoolingDeviceChangedCallbackWithType is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void unregisterCoolingDeviceChangedCallback(android.hardware.thermal.ICoolingDeviceChangedCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterCoolingDeviceChangedCallback, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method unregisterCoolingDeviceChangedCallback is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public float forecastSkinTemperature(int forecastSeconds) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        float _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(forecastSeconds);
          boolean _status = mRemote.transact(Stub.TRANSACTION_forecastSkinTemperature, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method forecastSkinTemperature is unimplemented.");
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
    static final int TRANSACTION_getCoolingDevices = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_getCoolingDevicesWithType = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_getTemperatures = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_getTemperaturesWithType = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getTemperatureThresholds = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getTemperatureThresholdsWithType = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_registerThermalChangedCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_registerThermalChangedCallbackWithType = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_unregisterThermalChangedCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_registerCoolingDeviceChangedCallbackWithType = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_unregisterCoolingDeviceChangedCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_forecastSkinTemperature = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android$hardware$thermal$IThermal".replace('$', '.');
  public android.hardware.thermal.CoolingDevice[] getCoolingDevices() throws android.os.RemoteException;
  public android.hardware.thermal.CoolingDevice[] getCoolingDevicesWithType(int type) throws android.os.RemoteException;
  public android.hardware.thermal.Temperature[] getTemperatures() throws android.os.RemoteException;
  public android.hardware.thermal.Temperature[] getTemperaturesWithType(int type) throws android.os.RemoteException;
  public android.hardware.thermal.TemperatureThreshold[] getTemperatureThresholds() throws android.os.RemoteException;
  public android.hardware.thermal.TemperatureThreshold[] getTemperatureThresholdsWithType(int type) throws android.os.RemoteException;
  public void registerThermalChangedCallback(android.hardware.thermal.IThermalChangedCallback callback) throws android.os.RemoteException;
  public void registerThermalChangedCallbackWithType(android.hardware.thermal.IThermalChangedCallback callback, int type) throws android.os.RemoteException;
  public void unregisterThermalChangedCallback(android.hardware.thermal.IThermalChangedCallback callback) throws android.os.RemoteException;
  public void registerCoolingDeviceChangedCallbackWithType(android.hardware.thermal.ICoolingDeviceChangedCallback callback, int type) throws android.os.RemoteException;
  public void unregisterCoolingDeviceChangedCallback(android.hardware.thermal.ICoolingDeviceChangedCallback callback) throws android.os.RemoteException;
  public float forecastSkinTemperature(int forecastSeconds) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
