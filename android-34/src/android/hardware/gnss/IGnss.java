/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.gnss;
/** @hide */
public interface IGnss extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "fc957f1d3d261d065ff5e5415f2d21caa79c310f";
  /** Default implementation for IGnss. */
  public static class Default implements android.hardware.gnss.IGnss
  {
    @Override public void setCallback(android.hardware.gnss.IGnssCallback callback) throws android.os.RemoteException
    {
    }
    @Override public void close() throws android.os.RemoteException
    {
    }
    @Override public android.hardware.gnss.IGnssPsds getExtensionPsds() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.gnss.IGnssConfiguration getExtensionGnssConfiguration() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.gnss.IGnssMeasurementInterface getExtensionGnssMeasurement() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.gnss.IGnssPowerIndication getExtensionGnssPowerIndication() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.gnss.IGnssBatching getExtensionGnssBatching() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.gnss.IGnssGeofence getExtensionGnssGeofence() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.gnss.IGnssNavigationMessageInterface getExtensionGnssNavigationMessage() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.gnss.IAGnss getExtensionAGnss() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.gnss.IAGnssRil getExtensionAGnssRil() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.gnss.IGnssDebug getExtensionGnssDebug() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.gnss.visibility_control.IGnssVisibilityControl getExtensionGnssVisibilityControl() throws android.os.RemoteException
    {
      return null;
    }
    @Override public void start() throws android.os.RemoteException
    {
    }
    @Override public void stop() throws android.os.RemoteException
    {
    }
    @Override public void injectTime(long timeMs, long timeReferenceMs, int uncertaintyMs) throws android.os.RemoteException
    {
    }
    @Override public void injectLocation(android.hardware.gnss.GnssLocation location) throws android.os.RemoteException
    {
    }
    @Override public void injectBestLocation(android.hardware.gnss.GnssLocation location) throws android.os.RemoteException
    {
    }
    @Override public void deleteAidingData(int aidingDataFlags) throws android.os.RemoteException
    {
    }
    @Override public void setPositionMode(android.hardware.gnss.IGnss.PositionModeOptions options) throws android.os.RemoteException
    {
    }
    @Override public android.hardware.gnss.IGnssAntennaInfo getExtensionGnssAntennaInfo() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.gnss.measurement_corrections.IMeasurementCorrectionsInterface getExtensionMeasurementCorrections() throws android.os.RemoteException
    {
      return null;
    }
    @Override public void startSvStatus() throws android.os.RemoteException
    {
    }
    @Override public void stopSvStatus() throws android.os.RemoteException
    {
    }
    @Override public void startNmea() throws android.os.RemoteException
    {
    }
    @Override public void stopNmea() throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.gnss.IGnss
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.gnss.IGnss interface,
     * generating a proxy if needed.
     */
    public static android.hardware.gnss.IGnss asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.gnss.IGnss))) {
        return ((android.hardware.gnss.IGnss)iin);
      }
      return new android.hardware.gnss.IGnss.Stub.Proxy(obj);
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
        case TRANSACTION_setCallback:
        {
          return "setCallback";
        }
        case TRANSACTION_close:
        {
          return "close";
        }
        case TRANSACTION_getExtensionPsds:
        {
          return "getExtensionPsds";
        }
        case TRANSACTION_getExtensionGnssConfiguration:
        {
          return "getExtensionGnssConfiguration";
        }
        case TRANSACTION_getExtensionGnssMeasurement:
        {
          return "getExtensionGnssMeasurement";
        }
        case TRANSACTION_getExtensionGnssPowerIndication:
        {
          return "getExtensionGnssPowerIndication";
        }
        case TRANSACTION_getExtensionGnssBatching:
        {
          return "getExtensionGnssBatching";
        }
        case TRANSACTION_getExtensionGnssGeofence:
        {
          return "getExtensionGnssGeofence";
        }
        case TRANSACTION_getExtensionGnssNavigationMessage:
        {
          return "getExtensionGnssNavigationMessage";
        }
        case TRANSACTION_getExtensionAGnss:
        {
          return "getExtensionAGnss";
        }
        case TRANSACTION_getExtensionAGnssRil:
        {
          return "getExtensionAGnssRil";
        }
        case TRANSACTION_getExtensionGnssDebug:
        {
          return "getExtensionGnssDebug";
        }
        case TRANSACTION_getExtensionGnssVisibilityControl:
        {
          return "getExtensionGnssVisibilityControl";
        }
        case TRANSACTION_start:
        {
          return "start";
        }
        case TRANSACTION_stop:
        {
          return "stop";
        }
        case TRANSACTION_injectTime:
        {
          return "injectTime";
        }
        case TRANSACTION_injectLocation:
        {
          return "injectLocation";
        }
        case TRANSACTION_injectBestLocation:
        {
          return "injectBestLocation";
        }
        case TRANSACTION_deleteAidingData:
        {
          return "deleteAidingData";
        }
        case TRANSACTION_setPositionMode:
        {
          return "setPositionMode";
        }
        case TRANSACTION_getExtensionGnssAntennaInfo:
        {
          return "getExtensionGnssAntennaInfo";
        }
        case TRANSACTION_getExtensionMeasurementCorrections:
        {
          return "getExtensionMeasurementCorrections";
        }
        case TRANSACTION_startSvStatus:
        {
          return "startSvStatus";
        }
        case TRANSACTION_stopSvStatus:
        {
          return "stopSvStatus";
        }
        case TRANSACTION_startNmea:
        {
          return "startNmea";
        }
        case TRANSACTION_stopNmea:
        {
          return "stopNmea";
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
        case TRANSACTION_setCallback:
        {
          android.hardware.gnss.IGnssCallback _arg0;
          _arg0 = android.hardware.gnss.IGnssCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.setCallback(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_close:
        {
          this.close();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getExtensionPsds:
        {
          android.hardware.gnss.IGnssPsds _result = this.getExtensionPsds();
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_getExtensionGnssConfiguration:
        {
          android.hardware.gnss.IGnssConfiguration _result = this.getExtensionGnssConfiguration();
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_getExtensionGnssMeasurement:
        {
          android.hardware.gnss.IGnssMeasurementInterface _result = this.getExtensionGnssMeasurement();
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_getExtensionGnssPowerIndication:
        {
          android.hardware.gnss.IGnssPowerIndication _result = this.getExtensionGnssPowerIndication();
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_getExtensionGnssBatching:
        {
          android.hardware.gnss.IGnssBatching _result = this.getExtensionGnssBatching();
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_getExtensionGnssGeofence:
        {
          android.hardware.gnss.IGnssGeofence _result = this.getExtensionGnssGeofence();
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_getExtensionGnssNavigationMessage:
        {
          android.hardware.gnss.IGnssNavigationMessageInterface _result = this.getExtensionGnssNavigationMessage();
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_getExtensionAGnss:
        {
          android.hardware.gnss.IAGnss _result = this.getExtensionAGnss();
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_getExtensionAGnssRil:
        {
          android.hardware.gnss.IAGnssRil _result = this.getExtensionAGnssRil();
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_getExtensionGnssDebug:
        {
          android.hardware.gnss.IGnssDebug _result = this.getExtensionGnssDebug();
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_getExtensionGnssVisibilityControl:
        {
          android.hardware.gnss.visibility_control.IGnssVisibilityControl _result = this.getExtensionGnssVisibilityControl();
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_start:
        {
          this.start();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_stop:
        {
          this.stop();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_injectTime:
        {
          long _arg0;
          _arg0 = data.readLong();
          long _arg1;
          _arg1 = data.readLong();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.injectTime(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_injectLocation:
        {
          android.hardware.gnss.GnssLocation _arg0;
          _arg0 = data.readTypedObject(android.hardware.gnss.GnssLocation.CREATOR);
          data.enforceNoDataAvail();
          this.injectLocation(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_injectBestLocation:
        {
          android.hardware.gnss.GnssLocation _arg0;
          _arg0 = data.readTypedObject(android.hardware.gnss.GnssLocation.CREATOR);
          data.enforceNoDataAvail();
          this.injectBestLocation(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_deleteAidingData:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.deleteAidingData(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setPositionMode:
        {
          android.hardware.gnss.IGnss.PositionModeOptions _arg0;
          _arg0 = data.readTypedObject(android.hardware.gnss.IGnss.PositionModeOptions.CREATOR);
          data.enforceNoDataAvail();
          this.setPositionMode(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getExtensionGnssAntennaInfo:
        {
          android.hardware.gnss.IGnssAntennaInfo _result = this.getExtensionGnssAntennaInfo();
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_getExtensionMeasurementCorrections:
        {
          android.hardware.gnss.measurement_corrections.IMeasurementCorrectionsInterface _result = this.getExtensionMeasurementCorrections();
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_startSvStatus:
        {
          this.startSvStatus();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_stopSvStatus:
        {
          this.stopSvStatus();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_startNmea:
        {
          this.startNmea();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_stopNmea:
        {
          this.stopNmea();
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
    private static class Proxy implements android.hardware.gnss.IGnss
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
      @Override public void setCallback(android.hardware.gnss.IGnssCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCallback, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setCallback is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void close() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_close, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method close is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public android.hardware.gnss.IGnssPsds getExtensionPsds() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.gnss.IGnssPsds _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getExtensionPsds, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getExtensionPsds is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.gnss.IGnssPsds.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.gnss.IGnssConfiguration getExtensionGnssConfiguration() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.gnss.IGnssConfiguration _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getExtensionGnssConfiguration, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getExtensionGnssConfiguration is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.gnss.IGnssConfiguration.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.gnss.IGnssMeasurementInterface getExtensionGnssMeasurement() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.gnss.IGnssMeasurementInterface _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getExtensionGnssMeasurement, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getExtensionGnssMeasurement is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.gnss.IGnssMeasurementInterface.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.gnss.IGnssPowerIndication getExtensionGnssPowerIndication() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.gnss.IGnssPowerIndication _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getExtensionGnssPowerIndication, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getExtensionGnssPowerIndication is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.gnss.IGnssPowerIndication.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.gnss.IGnssBatching getExtensionGnssBatching() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.gnss.IGnssBatching _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getExtensionGnssBatching, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getExtensionGnssBatching is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.gnss.IGnssBatching.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.gnss.IGnssGeofence getExtensionGnssGeofence() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.gnss.IGnssGeofence _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getExtensionGnssGeofence, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getExtensionGnssGeofence is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.gnss.IGnssGeofence.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.gnss.IGnssNavigationMessageInterface getExtensionGnssNavigationMessage() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.gnss.IGnssNavigationMessageInterface _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getExtensionGnssNavigationMessage, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getExtensionGnssNavigationMessage is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.gnss.IGnssNavigationMessageInterface.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.gnss.IAGnss getExtensionAGnss() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.gnss.IAGnss _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getExtensionAGnss, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getExtensionAGnss is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.gnss.IAGnss.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.gnss.IAGnssRil getExtensionAGnssRil() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.gnss.IAGnssRil _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getExtensionAGnssRil, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getExtensionAGnssRil is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.gnss.IAGnssRil.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.gnss.IGnssDebug getExtensionGnssDebug() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.gnss.IGnssDebug _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getExtensionGnssDebug, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getExtensionGnssDebug is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.gnss.IGnssDebug.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.gnss.visibility_control.IGnssVisibilityControl getExtensionGnssVisibilityControl() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.gnss.visibility_control.IGnssVisibilityControl _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getExtensionGnssVisibilityControl, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getExtensionGnssVisibilityControl is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.gnss.visibility_control.IGnssVisibilityControl.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void start() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_start, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method start is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void stop() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stop, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method stop is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void injectTime(long timeMs, long timeReferenceMs, int uncertaintyMs) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(timeMs);
          _data.writeLong(timeReferenceMs);
          _data.writeInt(uncertaintyMs);
          boolean _status = mRemote.transact(Stub.TRANSACTION_injectTime, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method injectTime is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void injectLocation(android.hardware.gnss.GnssLocation location) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(location, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_injectLocation, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method injectLocation is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void injectBestLocation(android.hardware.gnss.GnssLocation location) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(location, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_injectBestLocation, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method injectBestLocation is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void deleteAidingData(int aidingDataFlags) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(aidingDataFlags);
          boolean _status = mRemote.transact(Stub.TRANSACTION_deleteAidingData, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method deleteAidingData is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setPositionMode(android.hardware.gnss.IGnss.PositionModeOptions options) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(options, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setPositionMode, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setPositionMode is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public android.hardware.gnss.IGnssAntennaInfo getExtensionGnssAntennaInfo() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.gnss.IGnssAntennaInfo _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getExtensionGnssAntennaInfo, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getExtensionGnssAntennaInfo is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.gnss.IGnssAntennaInfo.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.gnss.measurement_corrections.IMeasurementCorrectionsInterface getExtensionMeasurementCorrections() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.gnss.measurement_corrections.IMeasurementCorrectionsInterface _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getExtensionMeasurementCorrections, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getExtensionMeasurementCorrections is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.gnss.measurement_corrections.IMeasurementCorrectionsInterface.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void startSvStatus() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startSvStatus, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method startSvStatus is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void stopSvStatus() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stopSvStatus, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method stopSvStatus is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void startNmea() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startNmea, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method startNmea is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void stopNmea() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stopNmea, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method stopNmea is unimplemented.");
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
    static final int TRANSACTION_setCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_close = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_getExtensionPsds = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_getExtensionGnssConfiguration = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getExtensionGnssMeasurement = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getExtensionGnssPowerIndication = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getExtensionGnssBatching = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getExtensionGnssGeofence = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_getExtensionGnssNavigationMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_getExtensionAGnss = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_getExtensionAGnssRil = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_getExtensionGnssDebug = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_getExtensionGnssVisibilityControl = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_start = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_stop = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_injectTime = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_injectLocation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_injectBestLocation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_deleteAidingData = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_setPositionMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
    static final int TRANSACTION_getExtensionGnssAntennaInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
    static final int TRANSACTION_getExtensionMeasurementCorrections = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
    static final int TRANSACTION_startSvStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
    static final int TRANSACTION_stopSvStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 23);
    static final int TRANSACTION_startNmea = (android.os.IBinder.FIRST_CALL_TRANSACTION + 24);
    static final int TRANSACTION_stopNmea = (android.os.IBinder.FIRST_CALL_TRANSACTION + 25);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$gnss$IGnss".replace('$', '.');
  public static final int ERROR_INVALID_ARGUMENT = 1;
  public static final int ERROR_ALREADY_INIT = 2;
  public static final int ERROR_GENERIC = 3;
  public void setCallback(android.hardware.gnss.IGnssCallback callback) throws android.os.RemoteException;
  public void close() throws android.os.RemoteException;
  public android.hardware.gnss.IGnssPsds getExtensionPsds() throws android.os.RemoteException;
  public android.hardware.gnss.IGnssConfiguration getExtensionGnssConfiguration() throws android.os.RemoteException;
  public android.hardware.gnss.IGnssMeasurementInterface getExtensionGnssMeasurement() throws android.os.RemoteException;
  public android.hardware.gnss.IGnssPowerIndication getExtensionGnssPowerIndication() throws android.os.RemoteException;
  public android.hardware.gnss.IGnssBatching getExtensionGnssBatching() throws android.os.RemoteException;
  public android.hardware.gnss.IGnssGeofence getExtensionGnssGeofence() throws android.os.RemoteException;
  public android.hardware.gnss.IGnssNavigationMessageInterface getExtensionGnssNavigationMessage() throws android.os.RemoteException;
  public android.hardware.gnss.IAGnss getExtensionAGnss() throws android.os.RemoteException;
  public android.hardware.gnss.IAGnssRil getExtensionAGnssRil() throws android.os.RemoteException;
  public android.hardware.gnss.IGnssDebug getExtensionGnssDebug() throws android.os.RemoteException;
  public android.hardware.gnss.visibility_control.IGnssVisibilityControl getExtensionGnssVisibilityControl() throws android.os.RemoteException;
  public void start() throws android.os.RemoteException;
  public void stop() throws android.os.RemoteException;
  public void injectTime(long timeMs, long timeReferenceMs, int uncertaintyMs) throws android.os.RemoteException;
  public void injectLocation(android.hardware.gnss.GnssLocation location) throws android.os.RemoteException;
  public void injectBestLocation(android.hardware.gnss.GnssLocation location) throws android.os.RemoteException;
  public void deleteAidingData(int aidingDataFlags) throws android.os.RemoteException;
  public void setPositionMode(android.hardware.gnss.IGnss.PositionModeOptions options) throws android.os.RemoteException;
  public android.hardware.gnss.IGnssAntennaInfo getExtensionGnssAntennaInfo() throws android.os.RemoteException;
  public android.hardware.gnss.measurement_corrections.IMeasurementCorrectionsInterface getExtensionMeasurementCorrections() throws android.os.RemoteException;
  public void startSvStatus() throws android.os.RemoteException;
  public void stopSvStatus() throws android.os.RemoteException;
  public void startNmea() throws android.os.RemoteException;
  public void stopNmea() throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
  public static @interface GnssPositionMode {
    public static final int STANDALONE = 0;
    public static final int MS_BASED = 1;
    public static final int MS_ASSISTED = 2;
  }
  public static @interface GnssPositionRecurrence {
    public static final int RECURRENCE_PERIODIC = 0;
    public static final int RECURRENCE_SINGLE = 1;
  }
  public static @interface GnssAidingData {
    public static final int EPHEMERIS = 1;
    public static final int ALMANAC = 2;
    public static final int POSITION = 4;
    public static final int TIME = 8;
    public static final int IONO = 16;
    public static final int UTC = 32;
    public static final int HEALTH = 64;
    public static final int SVDIR = 128;
    public static final int SVSTEER = 256;
    public static final int SADATA = 512;
    public static final int RTI = 1024;
    public static final int CELLDB_INFO = 32768;
    public static final int ALL = 65535;
  }
  public static class PositionModeOptions implements android.os.Parcelable
  {
    public int mode;
    public int recurrence;
    public int minIntervalMs = 0;
    public int preferredAccuracyMeters = 0;
    public int preferredTimeMs = 0;
    public boolean lowPowerMode = false;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<PositionModeOptions> CREATOR = new android.os.Parcelable.Creator<PositionModeOptions>() {
      @Override
      public PositionModeOptions createFromParcel(android.os.Parcel _aidl_source) {
        PositionModeOptions _aidl_out = new PositionModeOptions();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public PositionModeOptions[] newArray(int _aidl_size) {
        return new PositionModeOptions[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeInt(mode);
      _aidl_parcel.writeInt(recurrence);
      _aidl_parcel.writeInt(minIntervalMs);
      _aidl_parcel.writeInt(preferredAccuracyMeters);
      _aidl_parcel.writeInt(preferredTimeMs);
      _aidl_parcel.writeBoolean(lowPowerMode);
      int _aidl_end_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.setDataPosition(_aidl_start_pos);
      _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
      _aidl_parcel.setDataPosition(_aidl_end_pos);
    }
    public final void readFromParcel(android.os.Parcel _aidl_parcel)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      int _aidl_parcelable_size = _aidl_parcel.readInt();
      try {
        if (_aidl_parcelable_size < 4) throw new android.os.BadParcelableException("Parcelable too small");;
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        mode = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        recurrence = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        minIntervalMs = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        preferredAccuracyMeters = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        preferredTimeMs = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        lowPowerMode = _aidl_parcel.readBoolean();
      } finally {
        if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
          throw new android.os.BadParcelableException("Overflow in the size of parcelable");
        }
        _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
      }
    }
    @Override
    public int describeContents() {
      int _mask = 0;
      return _mask;
    }
  }
}
