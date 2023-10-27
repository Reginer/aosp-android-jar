/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.modem;
public interface IRadioModemResponse extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "09927560afccc75a063944fbbab3af48099261ca";
  /** Default implementation for IRadioModemResponse. */
  public static class Default implements android.hardware.radio.modem.IRadioModemResponse
  {
    @Override public void acknowledgeRequest(int serial) throws android.os.RemoteException
    {
    }
    @Override public void enableModemResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void getBasebandVersionResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String version) throws android.os.RemoteException
    {
    }
    /** @deprecated use getImeiResponse(RadioResponseInfo responseInfo, ImeiInfo imeiInfo) */
    @Override public void getDeviceIdentityResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String imei, java.lang.String imeisv, java.lang.String esn, java.lang.String meid) throws android.os.RemoteException
    {
    }
    @Override public void getHardwareConfigResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.modem.HardwareConfig[] config) throws android.os.RemoteException
    {
    }
    @Override public void getModemActivityInfoResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.modem.ActivityStatsInfo activityInfo) throws android.os.RemoteException
    {
    }
    @Override public void getModemStackStatusResponse(android.hardware.radio.RadioResponseInfo info, boolean isEnabled) throws android.os.RemoteException
    {
    }
    @Override public void getRadioCapabilityResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.modem.RadioCapability rc) throws android.os.RemoteException
    {
    }
    /** @deprecated NV APIs are deprecated starting from Android U. */
    @Override public void nvReadItemResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String result) throws android.os.RemoteException
    {
    }
    @Override public void nvResetConfigResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    /** @deprecated NV APIs are deprecated starting from Android U. */
    @Override public void nvWriteCdmaPrlResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    /** @deprecated NV APIs are deprecated starting from Android U. */
    @Override public void nvWriteItemResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void requestShutdownResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void sendDeviceStateResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setRadioCapabilityResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.modem.RadioCapability rc) throws android.os.RemoteException
    {
    }
    @Override public void setRadioPowerResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void getImeiResponse(android.hardware.radio.RadioResponseInfo responseInfo, android.hardware.radio.modem.ImeiInfo imeiInfo) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.radio.modem.IRadioModemResponse
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.radio.modem.IRadioModemResponse interface,
     * generating a proxy if needed.
     */
    public static android.hardware.radio.modem.IRadioModemResponse asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.radio.modem.IRadioModemResponse))) {
        return ((android.hardware.radio.modem.IRadioModemResponse)iin);
      }
      return new android.hardware.radio.modem.IRadioModemResponse.Stub.Proxy(obj);
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
        case TRANSACTION_acknowledgeRequest:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.acknowledgeRequest(_arg0);
          break;
        }
        case TRANSACTION_enableModemResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.enableModemResponse(_arg0);
          break;
        }
        case TRANSACTION_getBasebandVersionResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          java.lang.String _arg1;
          _arg1 = data.readString();
          data.enforceNoDataAvail();
          this.getBasebandVersionResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getDeviceIdentityResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          java.lang.String _arg3;
          _arg3 = data.readString();
          java.lang.String _arg4;
          _arg4 = data.readString();
          data.enforceNoDataAvail();
          this.getDeviceIdentityResponse(_arg0, _arg1, _arg2, _arg3, _arg4);
          break;
        }
        case TRANSACTION_getHardwareConfigResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.modem.HardwareConfig[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.radio.modem.HardwareConfig.CREATOR);
          data.enforceNoDataAvail();
          this.getHardwareConfigResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getModemActivityInfoResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.modem.ActivityStatsInfo _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.modem.ActivityStatsInfo.CREATOR);
          data.enforceNoDataAvail();
          this.getModemActivityInfoResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getModemStackStatusResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.getModemStackStatusResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getRadioCapabilityResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.modem.RadioCapability _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.modem.RadioCapability.CREATOR);
          data.enforceNoDataAvail();
          this.getRadioCapabilityResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_nvReadItemResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          java.lang.String _arg1;
          _arg1 = data.readString();
          data.enforceNoDataAvail();
          this.nvReadItemResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_nvResetConfigResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.nvResetConfigResponse(_arg0);
          break;
        }
        case TRANSACTION_nvWriteCdmaPrlResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.nvWriteCdmaPrlResponse(_arg0);
          break;
        }
        case TRANSACTION_nvWriteItemResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.nvWriteItemResponse(_arg0);
          break;
        }
        case TRANSACTION_requestShutdownResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.requestShutdownResponse(_arg0);
          break;
        }
        case TRANSACTION_sendDeviceStateResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.sendDeviceStateResponse(_arg0);
          break;
        }
        case TRANSACTION_setRadioCapabilityResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.modem.RadioCapability _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.modem.RadioCapability.CREATOR);
          data.enforceNoDataAvail();
          this.setRadioCapabilityResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setRadioPowerResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setRadioPowerResponse(_arg0);
          break;
        }
        case TRANSACTION_getImeiResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.modem.ImeiInfo _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.modem.ImeiInfo.CREATOR);
          data.enforceNoDataAvail();
          this.getImeiResponse(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.radio.modem.IRadioModemResponse
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
      @Override public void acknowledgeRequest(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_acknowledgeRequest, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method acknowledgeRequest is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void enableModemResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_enableModemResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method enableModemResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getBasebandVersionResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String version) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeString(version);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getBasebandVersionResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getBasebandVersionResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      /** @deprecated use getImeiResponse(RadioResponseInfo responseInfo, ImeiInfo imeiInfo) */
      @Override public void getDeviceIdentityResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String imei, java.lang.String imeisv, java.lang.String esn, java.lang.String meid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeString(imei);
          _data.writeString(imeisv);
          _data.writeString(esn);
          _data.writeString(meid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getDeviceIdentityResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getDeviceIdentityResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getHardwareConfigResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.modem.HardwareConfig[] config) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedArray(config, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getHardwareConfigResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getHardwareConfigResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getModemActivityInfoResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.modem.ActivityStatsInfo activityInfo) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(activityInfo, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getModemActivityInfoResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getModemActivityInfoResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getModemStackStatusResponse(android.hardware.radio.RadioResponseInfo info, boolean isEnabled) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeBoolean(isEnabled);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getModemStackStatusResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getModemStackStatusResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getRadioCapabilityResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.modem.RadioCapability rc) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(rc, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getRadioCapabilityResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getRadioCapabilityResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      /** @deprecated NV APIs are deprecated starting from Android U. */
      @Override public void nvReadItemResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String result) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeString(result);
          boolean _status = mRemote.transact(Stub.TRANSACTION_nvReadItemResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method nvReadItemResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void nvResetConfigResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_nvResetConfigResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method nvResetConfigResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      /** @deprecated NV APIs are deprecated starting from Android U. */
      @Override public void nvWriteCdmaPrlResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_nvWriteCdmaPrlResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method nvWriteCdmaPrlResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      /** @deprecated NV APIs are deprecated starting from Android U. */
      @Override public void nvWriteItemResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_nvWriteItemResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method nvWriteItemResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void requestShutdownResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_requestShutdownResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method requestShutdownResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendDeviceStateResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendDeviceStateResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendDeviceStateResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setRadioCapabilityResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.modem.RadioCapability rc) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(rc, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setRadioCapabilityResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setRadioCapabilityResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setRadioPowerResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setRadioPowerResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setRadioPowerResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getImeiResponse(android.hardware.radio.RadioResponseInfo responseInfo, android.hardware.radio.modem.ImeiInfo imeiInfo) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(responseInfo, 0);
          _data.writeTypedObject(imeiInfo, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getImeiResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getImeiResponse is unimplemented.");
          }
        }
        finally {
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
    static final int TRANSACTION_acknowledgeRequest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_enableModemResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_getBasebandVersionResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_getDeviceIdentityResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getHardwareConfigResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getModemActivityInfoResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getModemStackStatusResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getRadioCapabilityResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_nvReadItemResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_nvResetConfigResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_nvWriteCdmaPrlResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_nvWriteItemResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_requestShutdownResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_sendDeviceStateResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_setRadioCapabilityResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_setRadioPowerResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_getImeiResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$radio$modem$IRadioModemResponse".replace('$', '.');
  public void acknowledgeRequest(int serial) throws android.os.RemoteException;
  public void enableModemResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void getBasebandVersionResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String version) throws android.os.RemoteException;
  /** @deprecated use getImeiResponse(RadioResponseInfo responseInfo, ImeiInfo imeiInfo) */
  @Deprecated
  public void getDeviceIdentityResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String imei, java.lang.String imeisv, java.lang.String esn, java.lang.String meid) throws android.os.RemoteException;
  public void getHardwareConfigResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.modem.HardwareConfig[] config) throws android.os.RemoteException;
  public void getModemActivityInfoResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.modem.ActivityStatsInfo activityInfo) throws android.os.RemoteException;
  public void getModemStackStatusResponse(android.hardware.radio.RadioResponseInfo info, boolean isEnabled) throws android.os.RemoteException;
  public void getRadioCapabilityResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.modem.RadioCapability rc) throws android.os.RemoteException;
  /** @deprecated NV APIs are deprecated starting from Android U. */
  @Deprecated
  public void nvReadItemResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String result) throws android.os.RemoteException;
  public void nvResetConfigResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  /** @deprecated NV APIs are deprecated starting from Android U. */
  @Deprecated
  public void nvWriteCdmaPrlResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  /** @deprecated NV APIs are deprecated starting from Android U. */
  @Deprecated
  public void nvWriteItemResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void requestShutdownResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void sendDeviceStateResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setRadioCapabilityResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.modem.RadioCapability rc) throws android.os.RemoteException;
  public void setRadioPowerResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void getImeiResponse(android.hardware.radio.RadioResponseInfo responseInfo, android.hardware.radio.modem.ImeiInfo imeiInfo) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
