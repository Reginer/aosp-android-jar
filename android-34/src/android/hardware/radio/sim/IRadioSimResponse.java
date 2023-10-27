/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.sim;
public interface IRadioSimResponse extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "4f348cc7aca716cc41c09ea95895c4b261231035";
  /** Default implementation for IRadioSimResponse. */
  public static class Default implements android.hardware.radio.sim.IRadioSimResponse
  {
    @Override public void acknowledgeRequest(int serial) throws android.os.RemoteException
    {
    }
    @Override public void areUiccApplicationsEnabledResponse(android.hardware.radio.RadioResponseInfo info, boolean enabled) throws android.os.RemoteException
    {
    }
    @Override public void changeIccPin2ForAppResponse(android.hardware.radio.RadioResponseInfo info, int remainingRetries) throws android.os.RemoteException
    {
    }
    @Override public void changeIccPinForAppResponse(android.hardware.radio.RadioResponseInfo info, int remainingRetries) throws android.os.RemoteException
    {
    }
    @Override public void enableUiccApplicationsResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void getAllowedCarriersResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.CarrierRestrictions carriers, int multiSimPolicy) throws android.os.RemoteException
    {
    }
    @Override public void getCdmaSubscriptionResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String mdn, java.lang.String hSid, java.lang.String hNid, java.lang.String min, java.lang.String prl) throws android.os.RemoteException
    {
    }
    @Override public void getCdmaSubscriptionSourceResponse(android.hardware.radio.RadioResponseInfo info, int source) throws android.os.RemoteException
    {
    }
    @Override public void getFacilityLockForAppResponse(android.hardware.radio.RadioResponseInfo info, int response) throws android.os.RemoteException
    {
    }
    @Override public void getIccCardStatusResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.CardStatus cardStatus) throws android.os.RemoteException
    {
    }
    @Override public void getImsiForAppResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String imsi) throws android.os.RemoteException
    {
    }
    @Override public void getSimPhonebookCapacityResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.PhonebookCapacity capacity) throws android.os.RemoteException
    {
    }
    @Override public void getSimPhonebookRecordsResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    /** @deprecated use iccCloseLogicalChannelWithSessionInfoResponse instead. */
    @Override public void iccCloseLogicalChannelResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void iccIoForAppResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.IccIoResult iccIo) throws android.os.RemoteException
    {
    }
    @Override public void iccOpenLogicalChannelResponse(android.hardware.radio.RadioResponseInfo info, int channelId, byte[] selectResponse) throws android.os.RemoteException
    {
    }
    @Override public void iccTransmitApduBasicChannelResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.IccIoResult result) throws android.os.RemoteException
    {
    }
    @Override public void iccTransmitApduLogicalChannelResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.IccIoResult result) throws android.os.RemoteException
    {
    }
    @Override public void reportStkServiceIsRunningResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void requestIccSimAuthenticationResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.IccIoResult result) throws android.os.RemoteException
    {
    }
    @Override public void sendEnvelopeResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String commandResponse) throws android.os.RemoteException
    {
    }
    @Override public void sendEnvelopeWithStatusResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.IccIoResult iccIo) throws android.os.RemoteException
    {
    }
    @Override public void sendTerminalResponseToSimResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setAllowedCarriersResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setCarrierInfoForImsiEncryptionResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setCdmaSubscriptionSourceResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setFacilityLockForAppResponse(android.hardware.radio.RadioResponseInfo info, int retry) throws android.os.RemoteException
    {
    }
    @Override public void setSimCardPowerResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setUiccSubscriptionResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void supplyIccPin2ForAppResponse(android.hardware.radio.RadioResponseInfo info, int remainingRetries) throws android.os.RemoteException
    {
    }
    @Override public void supplyIccPinForAppResponse(android.hardware.radio.RadioResponseInfo info, int remainingRetries) throws android.os.RemoteException
    {
    }
    @Override public void supplyIccPuk2ForAppResponse(android.hardware.radio.RadioResponseInfo info, int remainingRetries) throws android.os.RemoteException
    {
    }
    @Override public void supplyIccPukForAppResponse(android.hardware.radio.RadioResponseInfo info, int remainingRetries) throws android.os.RemoteException
    {
    }
    @Override public void supplySimDepersonalizationResponse(android.hardware.radio.RadioResponseInfo info, int persoType, int remainingRetries) throws android.os.RemoteException
    {
    }
    @Override public void updateSimPhonebookRecordsResponse(android.hardware.radio.RadioResponseInfo info, int updatedRecordIndex) throws android.os.RemoteException
    {
    }
    @Override public void iccCloseLogicalChannelWithSessionInfoResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.radio.sim.IRadioSimResponse
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.radio.sim.IRadioSimResponse interface,
     * generating a proxy if needed.
     */
    public static android.hardware.radio.sim.IRadioSimResponse asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.radio.sim.IRadioSimResponse))) {
        return ((android.hardware.radio.sim.IRadioSimResponse)iin);
      }
      return new android.hardware.radio.sim.IRadioSimResponse.Stub.Proxy(obj);
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
        case TRANSACTION_areUiccApplicationsEnabledResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.areUiccApplicationsEnabledResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_changeIccPin2ForAppResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.changeIccPin2ForAppResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_changeIccPinForAppResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.changeIccPinForAppResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_enableUiccApplicationsResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.enableUiccApplicationsResponse(_arg0);
          break;
        }
        case TRANSACTION_getAllowedCarriersResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.sim.CarrierRestrictions _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.sim.CarrierRestrictions.CREATOR);
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.getAllowedCarriersResponse(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_getCdmaSubscriptionResponse:
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
          java.lang.String _arg5;
          _arg5 = data.readString();
          data.enforceNoDataAvail();
          this.getCdmaSubscriptionResponse(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
          break;
        }
        case TRANSACTION_getCdmaSubscriptionSourceResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.getCdmaSubscriptionSourceResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getFacilityLockForAppResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.getFacilityLockForAppResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getIccCardStatusResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.sim.CardStatus _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.sim.CardStatus.CREATOR);
          data.enforceNoDataAvail();
          this.getIccCardStatusResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getImsiForAppResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          java.lang.String _arg1;
          _arg1 = data.readString();
          data.enforceNoDataAvail();
          this.getImsiForAppResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getSimPhonebookCapacityResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.sim.PhonebookCapacity _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.sim.PhonebookCapacity.CREATOR);
          data.enforceNoDataAvail();
          this.getSimPhonebookCapacityResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getSimPhonebookRecordsResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.getSimPhonebookRecordsResponse(_arg0);
          break;
        }
        case TRANSACTION_iccCloseLogicalChannelResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.iccCloseLogicalChannelResponse(_arg0);
          break;
        }
        case TRANSACTION_iccIoForAppResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.sim.IccIoResult _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.sim.IccIoResult.CREATOR);
          data.enforceNoDataAvail();
          this.iccIoForAppResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_iccOpenLogicalChannelResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          byte[] _arg2;
          _arg2 = data.createByteArray();
          data.enforceNoDataAvail();
          this.iccOpenLogicalChannelResponse(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_iccTransmitApduBasicChannelResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.sim.IccIoResult _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.sim.IccIoResult.CREATOR);
          data.enforceNoDataAvail();
          this.iccTransmitApduBasicChannelResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_iccTransmitApduLogicalChannelResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.sim.IccIoResult _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.sim.IccIoResult.CREATOR);
          data.enforceNoDataAvail();
          this.iccTransmitApduLogicalChannelResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_reportStkServiceIsRunningResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.reportStkServiceIsRunningResponse(_arg0);
          break;
        }
        case TRANSACTION_requestIccSimAuthenticationResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.sim.IccIoResult _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.sim.IccIoResult.CREATOR);
          data.enforceNoDataAvail();
          this.requestIccSimAuthenticationResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_sendEnvelopeResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          java.lang.String _arg1;
          _arg1 = data.readString();
          data.enforceNoDataAvail();
          this.sendEnvelopeResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_sendEnvelopeWithStatusResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.sim.IccIoResult _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.sim.IccIoResult.CREATOR);
          data.enforceNoDataAvail();
          this.sendEnvelopeWithStatusResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_sendTerminalResponseToSimResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.sendTerminalResponseToSimResponse(_arg0);
          break;
        }
        case TRANSACTION_setAllowedCarriersResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setAllowedCarriersResponse(_arg0);
          break;
        }
        case TRANSACTION_setCarrierInfoForImsiEncryptionResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setCarrierInfoForImsiEncryptionResponse(_arg0);
          break;
        }
        case TRANSACTION_setCdmaSubscriptionSourceResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setCdmaSubscriptionSourceResponse(_arg0);
          break;
        }
        case TRANSACTION_setFacilityLockForAppResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.setFacilityLockForAppResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setSimCardPowerResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setSimCardPowerResponse(_arg0);
          break;
        }
        case TRANSACTION_setUiccSubscriptionResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setUiccSubscriptionResponse(_arg0);
          break;
        }
        case TRANSACTION_supplyIccPin2ForAppResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.supplyIccPin2ForAppResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_supplyIccPinForAppResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.supplyIccPinForAppResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_supplyIccPuk2ForAppResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.supplyIccPuk2ForAppResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_supplyIccPukForAppResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.supplyIccPukForAppResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_supplySimDepersonalizationResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.supplySimDepersonalizationResponse(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_updateSimPhonebookRecordsResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.updateSimPhonebookRecordsResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_iccCloseLogicalChannelWithSessionInfoResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.iccCloseLogicalChannelWithSessionInfoResponse(_arg0);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.radio.sim.IRadioSimResponse
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
      @Override public void areUiccApplicationsEnabledResponse(android.hardware.radio.RadioResponseInfo info, boolean enabled) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeBoolean(enabled);
          boolean _status = mRemote.transact(Stub.TRANSACTION_areUiccApplicationsEnabledResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method areUiccApplicationsEnabledResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void changeIccPin2ForAppResponse(android.hardware.radio.RadioResponseInfo info, int remainingRetries) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeInt(remainingRetries);
          boolean _status = mRemote.transact(Stub.TRANSACTION_changeIccPin2ForAppResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method changeIccPin2ForAppResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void changeIccPinForAppResponse(android.hardware.radio.RadioResponseInfo info, int remainingRetries) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeInt(remainingRetries);
          boolean _status = mRemote.transact(Stub.TRANSACTION_changeIccPinForAppResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method changeIccPinForAppResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void enableUiccApplicationsResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_enableUiccApplicationsResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method enableUiccApplicationsResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getAllowedCarriersResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.CarrierRestrictions carriers, int multiSimPolicy) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(carriers, 0);
          _data.writeInt(multiSimPolicy);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getAllowedCarriersResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getAllowedCarriersResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getCdmaSubscriptionResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String mdn, java.lang.String hSid, java.lang.String hNid, java.lang.String min, java.lang.String prl) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeString(mdn);
          _data.writeString(hSid);
          _data.writeString(hNid);
          _data.writeString(min);
          _data.writeString(prl);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCdmaSubscriptionResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getCdmaSubscriptionResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getCdmaSubscriptionSourceResponse(android.hardware.radio.RadioResponseInfo info, int source) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeInt(source);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCdmaSubscriptionSourceResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getCdmaSubscriptionSourceResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getFacilityLockForAppResponse(android.hardware.radio.RadioResponseInfo info, int response) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeInt(response);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getFacilityLockForAppResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getFacilityLockForAppResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getIccCardStatusResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.CardStatus cardStatus) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(cardStatus, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getIccCardStatusResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getIccCardStatusResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getImsiForAppResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String imsi) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeString(imsi);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getImsiForAppResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getImsiForAppResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getSimPhonebookCapacityResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.PhonebookCapacity capacity) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(capacity, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSimPhonebookCapacityResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getSimPhonebookCapacityResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getSimPhonebookRecordsResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSimPhonebookRecordsResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getSimPhonebookRecordsResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      /** @deprecated use iccCloseLogicalChannelWithSessionInfoResponse instead. */
      @Override public void iccCloseLogicalChannelResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_iccCloseLogicalChannelResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method iccCloseLogicalChannelResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void iccIoForAppResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.IccIoResult iccIo) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(iccIo, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_iccIoForAppResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method iccIoForAppResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void iccOpenLogicalChannelResponse(android.hardware.radio.RadioResponseInfo info, int channelId, byte[] selectResponse) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeInt(channelId);
          _data.writeByteArray(selectResponse);
          boolean _status = mRemote.transact(Stub.TRANSACTION_iccOpenLogicalChannelResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method iccOpenLogicalChannelResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void iccTransmitApduBasicChannelResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.IccIoResult result) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(result, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_iccTransmitApduBasicChannelResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method iccTransmitApduBasicChannelResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void iccTransmitApduLogicalChannelResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.IccIoResult result) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(result, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_iccTransmitApduLogicalChannelResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method iccTransmitApduLogicalChannelResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void reportStkServiceIsRunningResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_reportStkServiceIsRunningResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method reportStkServiceIsRunningResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void requestIccSimAuthenticationResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.IccIoResult result) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(result, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_requestIccSimAuthenticationResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method requestIccSimAuthenticationResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendEnvelopeResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String commandResponse) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeString(commandResponse);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendEnvelopeResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendEnvelopeResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendEnvelopeWithStatusResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.IccIoResult iccIo) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(iccIo, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendEnvelopeWithStatusResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendEnvelopeWithStatusResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendTerminalResponseToSimResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendTerminalResponseToSimResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendTerminalResponseToSimResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setAllowedCarriersResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setAllowedCarriersResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setAllowedCarriersResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setCarrierInfoForImsiEncryptionResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCarrierInfoForImsiEncryptionResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setCarrierInfoForImsiEncryptionResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setCdmaSubscriptionSourceResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCdmaSubscriptionSourceResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setCdmaSubscriptionSourceResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setFacilityLockForAppResponse(android.hardware.radio.RadioResponseInfo info, int retry) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeInt(retry);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setFacilityLockForAppResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setFacilityLockForAppResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setSimCardPowerResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setSimCardPowerResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setSimCardPowerResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setUiccSubscriptionResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setUiccSubscriptionResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setUiccSubscriptionResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void supplyIccPin2ForAppResponse(android.hardware.radio.RadioResponseInfo info, int remainingRetries) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeInt(remainingRetries);
          boolean _status = mRemote.transact(Stub.TRANSACTION_supplyIccPin2ForAppResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method supplyIccPin2ForAppResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void supplyIccPinForAppResponse(android.hardware.radio.RadioResponseInfo info, int remainingRetries) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeInt(remainingRetries);
          boolean _status = mRemote.transact(Stub.TRANSACTION_supplyIccPinForAppResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method supplyIccPinForAppResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void supplyIccPuk2ForAppResponse(android.hardware.radio.RadioResponseInfo info, int remainingRetries) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeInt(remainingRetries);
          boolean _status = mRemote.transact(Stub.TRANSACTION_supplyIccPuk2ForAppResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method supplyIccPuk2ForAppResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void supplyIccPukForAppResponse(android.hardware.radio.RadioResponseInfo info, int remainingRetries) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeInt(remainingRetries);
          boolean _status = mRemote.transact(Stub.TRANSACTION_supplyIccPukForAppResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method supplyIccPukForAppResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void supplySimDepersonalizationResponse(android.hardware.radio.RadioResponseInfo info, int persoType, int remainingRetries) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeInt(persoType);
          _data.writeInt(remainingRetries);
          boolean _status = mRemote.transact(Stub.TRANSACTION_supplySimDepersonalizationResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method supplySimDepersonalizationResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void updateSimPhonebookRecordsResponse(android.hardware.radio.RadioResponseInfo info, int updatedRecordIndex) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeInt(updatedRecordIndex);
          boolean _status = mRemote.transact(Stub.TRANSACTION_updateSimPhonebookRecordsResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method updateSimPhonebookRecordsResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void iccCloseLogicalChannelWithSessionInfoResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_iccCloseLogicalChannelWithSessionInfoResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method iccCloseLogicalChannelWithSessionInfoResponse is unimplemented.");
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
    static final int TRANSACTION_areUiccApplicationsEnabledResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_changeIccPin2ForAppResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_changeIccPinForAppResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_enableUiccApplicationsResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getAllowedCarriersResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getCdmaSubscriptionResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getCdmaSubscriptionSourceResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_getFacilityLockForAppResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_getIccCardStatusResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_getImsiForAppResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_getSimPhonebookCapacityResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_getSimPhonebookRecordsResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_iccCloseLogicalChannelResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_iccIoForAppResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_iccOpenLogicalChannelResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_iccTransmitApduBasicChannelResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_iccTransmitApduLogicalChannelResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_reportStkServiceIsRunningResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_requestIccSimAuthenticationResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
    static final int TRANSACTION_sendEnvelopeResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
    static final int TRANSACTION_sendEnvelopeWithStatusResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
    static final int TRANSACTION_sendTerminalResponseToSimResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
    static final int TRANSACTION_setAllowedCarriersResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 23);
    static final int TRANSACTION_setCarrierInfoForImsiEncryptionResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 24);
    static final int TRANSACTION_setCdmaSubscriptionSourceResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 25);
    static final int TRANSACTION_setFacilityLockForAppResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 26);
    static final int TRANSACTION_setSimCardPowerResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 27);
    static final int TRANSACTION_setUiccSubscriptionResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 28);
    static final int TRANSACTION_supplyIccPin2ForAppResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 29);
    static final int TRANSACTION_supplyIccPinForAppResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 30);
    static final int TRANSACTION_supplyIccPuk2ForAppResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 31);
    static final int TRANSACTION_supplyIccPukForAppResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 32);
    static final int TRANSACTION_supplySimDepersonalizationResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 33);
    static final int TRANSACTION_updateSimPhonebookRecordsResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 34);
    static final int TRANSACTION_iccCloseLogicalChannelWithSessionInfoResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 35);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$radio$sim$IRadioSimResponse".replace('$', '.');
  public void acknowledgeRequest(int serial) throws android.os.RemoteException;
  public void areUiccApplicationsEnabledResponse(android.hardware.radio.RadioResponseInfo info, boolean enabled) throws android.os.RemoteException;
  public void changeIccPin2ForAppResponse(android.hardware.radio.RadioResponseInfo info, int remainingRetries) throws android.os.RemoteException;
  public void changeIccPinForAppResponse(android.hardware.radio.RadioResponseInfo info, int remainingRetries) throws android.os.RemoteException;
  public void enableUiccApplicationsResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void getAllowedCarriersResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.CarrierRestrictions carriers, int multiSimPolicy) throws android.os.RemoteException;
  public void getCdmaSubscriptionResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String mdn, java.lang.String hSid, java.lang.String hNid, java.lang.String min, java.lang.String prl) throws android.os.RemoteException;
  public void getCdmaSubscriptionSourceResponse(android.hardware.radio.RadioResponseInfo info, int source) throws android.os.RemoteException;
  public void getFacilityLockForAppResponse(android.hardware.radio.RadioResponseInfo info, int response) throws android.os.RemoteException;
  public void getIccCardStatusResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.CardStatus cardStatus) throws android.os.RemoteException;
  public void getImsiForAppResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String imsi) throws android.os.RemoteException;
  public void getSimPhonebookCapacityResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.PhonebookCapacity capacity) throws android.os.RemoteException;
  public void getSimPhonebookRecordsResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  /** @deprecated use iccCloseLogicalChannelWithSessionInfoResponse instead. */
  @Deprecated
  public void iccCloseLogicalChannelResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void iccIoForAppResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.IccIoResult iccIo) throws android.os.RemoteException;
  public void iccOpenLogicalChannelResponse(android.hardware.radio.RadioResponseInfo info, int channelId, byte[] selectResponse) throws android.os.RemoteException;
  public void iccTransmitApduBasicChannelResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.IccIoResult result) throws android.os.RemoteException;
  public void iccTransmitApduLogicalChannelResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.IccIoResult result) throws android.os.RemoteException;
  public void reportStkServiceIsRunningResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void requestIccSimAuthenticationResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.IccIoResult result) throws android.os.RemoteException;
  public void sendEnvelopeResponse(android.hardware.radio.RadioResponseInfo info, java.lang.String commandResponse) throws android.os.RemoteException;
  public void sendEnvelopeWithStatusResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.sim.IccIoResult iccIo) throws android.os.RemoteException;
  public void sendTerminalResponseToSimResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setAllowedCarriersResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setCarrierInfoForImsiEncryptionResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setCdmaSubscriptionSourceResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setFacilityLockForAppResponse(android.hardware.radio.RadioResponseInfo info, int retry) throws android.os.RemoteException;
  public void setSimCardPowerResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setUiccSubscriptionResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void supplyIccPin2ForAppResponse(android.hardware.radio.RadioResponseInfo info, int remainingRetries) throws android.os.RemoteException;
  public void supplyIccPinForAppResponse(android.hardware.radio.RadioResponseInfo info, int remainingRetries) throws android.os.RemoteException;
  public void supplyIccPuk2ForAppResponse(android.hardware.radio.RadioResponseInfo info, int remainingRetries) throws android.os.RemoteException;
  public void supplyIccPukForAppResponse(android.hardware.radio.RadioResponseInfo info, int remainingRetries) throws android.os.RemoteException;
  public void supplySimDepersonalizationResponse(android.hardware.radio.RadioResponseInfo info, int persoType, int remainingRetries) throws android.os.RemoteException;
  public void updateSimPhonebookRecordsResponse(android.hardware.radio.RadioResponseInfo info, int updatedRecordIndex) throws android.os.RemoteException;
  public void iccCloseLogicalChannelWithSessionInfoResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
