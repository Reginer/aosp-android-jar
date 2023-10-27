/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.messaging;
public interface IRadioMessagingResponse extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "50aefda34c9dd40090c8d5925e71d5b84530c3d0";
  /** Default implementation for IRadioMessagingResponse. */
  public static class Default implements android.hardware.radio.messaging.IRadioMessagingResponse
  {
    @Override public void acknowledgeIncomingGsmSmsWithPduResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void acknowledgeLastIncomingCdmaSmsResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void acknowledgeLastIncomingGsmSmsResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void acknowledgeRequest(int serial) throws android.os.RemoteException
    {
    }
    @Override public void deleteSmsOnRuimResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void deleteSmsOnSimResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void getCdmaBroadcastConfigResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.CdmaBroadcastSmsConfigInfo[] configs) throws android.os.RemoteException
    {
    }
    @Override public void getGsmBroadcastConfigResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.GsmBroadcastSmsConfigInfo[] configs) throws android.os.RemoteException
    {
    }
    @Override public void getSmscAddressResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String smsc) throws android.os.RemoteException
    {
    }
    @Override public void reportSmsMemoryStatusResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void sendCdmaSmsExpectMoreResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.SendSmsResult sms) throws android.os.RemoteException
    {
    }
    @Override public void sendCdmaSmsResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.SendSmsResult sms) throws android.os.RemoteException
    {
    }
    @Override public void sendImsSmsResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.SendSmsResult sms) throws android.os.RemoteException
    {
    }
    @Override public void sendSmsExpectMoreResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.SendSmsResult sms) throws android.os.RemoteException
    {
    }
    @Override public void sendSmsResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.SendSmsResult sms) throws android.os.RemoteException
    {
    }
    @Override public void setCdmaBroadcastActivationResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setCdmaBroadcastConfigResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setGsmBroadcastActivationResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setGsmBroadcastConfigResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setSmscAddressResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void writeSmsToRuimResponse(android.hardware.radio.RadioResponseInfo info, int index) throws android.os.RemoteException
    {
    }
    @Override public void writeSmsToSimResponse(android.hardware.radio.RadioResponseInfo info, int index) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.radio.messaging.IRadioMessagingResponse
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.radio.messaging.IRadioMessagingResponse interface,
     * generating a proxy if needed.
     */
    public static android.hardware.radio.messaging.IRadioMessagingResponse asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.radio.messaging.IRadioMessagingResponse))) {
        return ((android.hardware.radio.messaging.IRadioMessagingResponse)iin);
      }
      return new android.hardware.radio.messaging.IRadioMessagingResponse.Stub.Proxy(obj);
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
        case TRANSACTION_acknowledgeIncomingGsmSmsWithPduResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.acknowledgeIncomingGsmSmsWithPduResponse(_arg0);
          break;
        }
        case TRANSACTION_acknowledgeLastIncomingCdmaSmsResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.acknowledgeLastIncomingCdmaSmsResponse(_arg0);
          break;
        }
        case TRANSACTION_acknowledgeLastIncomingGsmSmsResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.acknowledgeLastIncomingGsmSmsResponse(_arg0);
          break;
        }
        case TRANSACTION_acknowledgeRequest:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.acknowledgeRequest(_arg0);
          break;
        }
        case TRANSACTION_deleteSmsOnRuimResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.deleteSmsOnRuimResponse(_arg0);
          break;
        }
        case TRANSACTION_deleteSmsOnSimResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.deleteSmsOnSimResponse(_arg0);
          break;
        }
        case TRANSACTION_getCdmaBroadcastConfigResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.messaging.CdmaBroadcastSmsConfigInfo[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.radio.messaging.CdmaBroadcastSmsConfigInfo.CREATOR);
          data.enforceNoDataAvail();
          this.getCdmaBroadcastConfigResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getGsmBroadcastConfigResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.messaging.GsmBroadcastSmsConfigInfo[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.radio.messaging.GsmBroadcastSmsConfigInfo.CREATOR);
          data.enforceNoDataAvail();
          this.getGsmBroadcastConfigResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getSmscAddressResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          java.lang.String _arg1;
          _arg1 = data.readString();
          data.enforceNoDataAvail();
          this.getSmscAddressResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_reportSmsMemoryStatusResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.reportSmsMemoryStatusResponse(_arg0);
          break;
        }
        case TRANSACTION_sendCdmaSmsExpectMoreResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.messaging.SendSmsResult _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.messaging.SendSmsResult.CREATOR);
          data.enforceNoDataAvail();
          this.sendCdmaSmsExpectMoreResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_sendCdmaSmsResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.messaging.SendSmsResult _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.messaging.SendSmsResult.CREATOR);
          data.enforceNoDataAvail();
          this.sendCdmaSmsResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_sendImsSmsResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.messaging.SendSmsResult _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.messaging.SendSmsResult.CREATOR);
          data.enforceNoDataAvail();
          this.sendImsSmsResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_sendSmsExpectMoreResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.messaging.SendSmsResult _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.messaging.SendSmsResult.CREATOR);
          data.enforceNoDataAvail();
          this.sendSmsExpectMoreResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_sendSmsResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.messaging.SendSmsResult _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.messaging.SendSmsResult.CREATOR);
          data.enforceNoDataAvail();
          this.sendSmsResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setCdmaBroadcastActivationResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setCdmaBroadcastActivationResponse(_arg0);
          break;
        }
        case TRANSACTION_setCdmaBroadcastConfigResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setCdmaBroadcastConfigResponse(_arg0);
          break;
        }
        case TRANSACTION_setGsmBroadcastActivationResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setGsmBroadcastActivationResponse(_arg0);
          break;
        }
        case TRANSACTION_setGsmBroadcastConfigResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setGsmBroadcastConfigResponse(_arg0);
          break;
        }
        case TRANSACTION_setSmscAddressResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setSmscAddressResponse(_arg0);
          break;
        }
        case TRANSACTION_writeSmsToRuimResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.writeSmsToRuimResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_writeSmsToSimResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.writeSmsToSimResponse(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.radio.messaging.IRadioMessagingResponse
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
      @Override public void acknowledgeIncomingGsmSmsWithPduResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_acknowledgeIncomingGsmSmsWithPduResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method acknowledgeIncomingGsmSmsWithPduResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void acknowledgeLastIncomingCdmaSmsResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_acknowledgeLastIncomingCdmaSmsResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method acknowledgeLastIncomingCdmaSmsResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void acknowledgeLastIncomingGsmSmsResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_acknowledgeLastIncomingGsmSmsResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method acknowledgeLastIncomingGsmSmsResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
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
      @Override public void deleteSmsOnRuimResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_deleteSmsOnRuimResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method deleteSmsOnRuimResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void deleteSmsOnSimResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_deleteSmsOnSimResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method deleteSmsOnSimResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getCdmaBroadcastConfigResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.CdmaBroadcastSmsConfigInfo[] configs) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedArray(configs, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCdmaBroadcastConfigResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getCdmaBroadcastConfigResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getGsmBroadcastConfigResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.GsmBroadcastSmsConfigInfo[] configs) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedArray(configs, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getGsmBroadcastConfigResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getGsmBroadcastConfigResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getSmscAddressResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String smsc) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeString(smsc);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSmscAddressResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getSmscAddressResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void reportSmsMemoryStatusResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_reportSmsMemoryStatusResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method reportSmsMemoryStatusResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendCdmaSmsExpectMoreResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.SendSmsResult sms) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(sms, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendCdmaSmsExpectMoreResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendCdmaSmsExpectMoreResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendCdmaSmsResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.SendSmsResult sms) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(sms, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendCdmaSmsResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendCdmaSmsResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendImsSmsResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.SendSmsResult sms) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(sms, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendImsSmsResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendImsSmsResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendSmsExpectMoreResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.SendSmsResult sms) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(sms, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendSmsExpectMoreResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendSmsExpectMoreResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendSmsResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.SendSmsResult sms) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(sms, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendSmsResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendSmsResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setCdmaBroadcastActivationResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCdmaBroadcastActivationResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setCdmaBroadcastActivationResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setCdmaBroadcastConfigResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCdmaBroadcastConfigResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setCdmaBroadcastConfigResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setGsmBroadcastActivationResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setGsmBroadcastActivationResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setGsmBroadcastActivationResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setGsmBroadcastConfigResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setGsmBroadcastConfigResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setGsmBroadcastConfigResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setSmscAddressResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setSmscAddressResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setSmscAddressResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void writeSmsToRuimResponse(android.hardware.radio.RadioResponseInfo info, int index) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeInt(index);
          boolean _status = mRemote.transact(Stub.TRANSACTION_writeSmsToRuimResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method writeSmsToRuimResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void writeSmsToSimResponse(android.hardware.radio.RadioResponseInfo info, int index) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeInt(index);
          boolean _status = mRemote.transact(Stub.TRANSACTION_writeSmsToSimResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method writeSmsToSimResponse is unimplemented.");
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
    static final int TRANSACTION_acknowledgeIncomingGsmSmsWithPduResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_acknowledgeLastIncomingCdmaSmsResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_acknowledgeLastIncomingGsmSmsResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_acknowledgeRequest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_deleteSmsOnRuimResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_deleteSmsOnSimResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getCdmaBroadcastConfigResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getGsmBroadcastConfigResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_getSmscAddressResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_reportSmsMemoryStatusResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_sendCdmaSmsExpectMoreResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_sendCdmaSmsResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_sendImsSmsResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_sendSmsExpectMoreResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_sendSmsResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_setCdmaBroadcastActivationResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_setCdmaBroadcastConfigResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_setGsmBroadcastActivationResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_setGsmBroadcastConfigResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_setSmscAddressResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
    static final int TRANSACTION_writeSmsToRuimResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
    static final int TRANSACTION_writeSmsToSimResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$radio$messaging$IRadioMessagingResponse".replace('$', '.');
  public void acknowledgeIncomingGsmSmsWithPduResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void acknowledgeLastIncomingCdmaSmsResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void acknowledgeLastIncomingGsmSmsResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void acknowledgeRequest(int serial) throws android.os.RemoteException;
  public void deleteSmsOnRuimResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void deleteSmsOnSimResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void getCdmaBroadcastConfigResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.CdmaBroadcastSmsConfigInfo[] configs) throws android.os.RemoteException;
  public void getGsmBroadcastConfigResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.GsmBroadcastSmsConfigInfo[] configs) throws android.os.RemoteException;
  public void getSmscAddressResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String smsc) throws android.os.RemoteException;
  public void reportSmsMemoryStatusResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void sendCdmaSmsExpectMoreResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.SendSmsResult sms) throws android.os.RemoteException;
  public void sendCdmaSmsResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.SendSmsResult sms) throws android.os.RemoteException;
  public void sendImsSmsResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.SendSmsResult sms) throws android.os.RemoteException;
  public void sendSmsExpectMoreResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.SendSmsResult sms) throws android.os.RemoteException;
  public void sendSmsResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.messaging.SendSmsResult sms) throws android.os.RemoteException;
  public void setCdmaBroadcastActivationResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setCdmaBroadcastConfigResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setGsmBroadcastActivationResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setGsmBroadcastConfigResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setSmscAddressResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void writeSmsToRuimResponse(android.hardware.radio.RadioResponseInfo info, int index) throws android.os.RemoteException;
  public void writeSmsToSimResponse(android.hardware.radio.RadioResponseInfo info, int index) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
