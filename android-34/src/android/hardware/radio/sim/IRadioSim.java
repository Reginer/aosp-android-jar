/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.sim;
public interface IRadioSim extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "4f348cc7aca716cc41c09ea95895c4b261231035";
  /** Default implementation for IRadioSim. */
  public static class Default implements android.hardware.radio.sim.IRadioSim
  {
    @Override public void areUiccApplicationsEnabled(int serial) throws android.os.RemoteException
    {
    }
    @Override public void changeIccPin2ForApp(int serial, java.lang.String oldPin2, java.lang.String newPin2, java.lang.String aid) throws android.os.RemoteException
    {
    }
    @Override public void changeIccPinForApp(int serial, java.lang.String oldPin, java.lang.String newPin, java.lang.String aid) throws android.os.RemoteException
    {
    }
    @Override public void enableUiccApplications(int serial, boolean enable) throws android.os.RemoteException
    {
    }
    @Override public void getAllowedCarriers(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getCdmaSubscription(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getCdmaSubscriptionSource(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getFacilityLockForApp(int serial, java.lang.String facility, java.lang.String password, int serviceClass, java.lang.String appId) throws android.os.RemoteException
    {
    }
    @Override public void getIccCardStatus(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getImsiForApp(int serial, java.lang.String aid) throws android.os.RemoteException
    {
    }
    @Override public void getSimPhonebookCapacity(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getSimPhonebookRecords(int serial) throws android.os.RemoteException
    {
    }
    /** @deprecated use iccCloseLogicalChannelWithSessionInfo instead. */
    @Override public void iccCloseLogicalChannel(int serial, int channelId) throws android.os.RemoteException
    {
    }
    @Override public void iccIoForApp(int serial, android.hardware.radio.sim.IccIo iccIo) throws android.os.RemoteException
    {
    }
    @Override public void iccOpenLogicalChannel(int serial, java.lang.String aid, int p2) throws android.os.RemoteException
    {
    }
    @Override public void iccTransmitApduBasicChannel(int serial, android.hardware.radio.sim.SimApdu message) throws android.os.RemoteException
    {
    }
    @Override public void iccTransmitApduLogicalChannel(int serial, android.hardware.radio.sim.SimApdu message) throws android.os.RemoteException
    {
    }
    @Override public void reportStkServiceIsRunning(int serial) throws android.os.RemoteException
    {
    }
    @Override public void requestIccSimAuthentication(int serial, int authContext, java.lang.String authData, java.lang.String aid) throws android.os.RemoteException
    {
    }
    @Override public void responseAcknowledgement() throws android.os.RemoteException
    {
    }
    @Override public void sendEnvelope(int serial, java.lang.String contents) throws android.os.RemoteException
    {
    }
    @Override public void sendEnvelopeWithStatus(int serial, java.lang.String contents) throws android.os.RemoteException
    {
    }
    @Override public void sendTerminalResponseToSim(int serial, java.lang.String contents) throws android.os.RemoteException
    {
    }
    @Override public void setAllowedCarriers(int serial, android.hardware.radio.sim.CarrierRestrictions carriers, int multiSimPolicy) throws android.os.RemoteException
    {
    }
    @Override public void setCarrierInfoForImsiEncryption(int serial, android.hardware.radio.sim.ImsiEncryptionInfo imsiEncryptionInfo) throws android.os.RemoteException
    {
    }
    @Override public void setCdmaSubscriptionSource(int serial, int cdmaSub) throws android.os.RemoteException
    {
    }
    @Override public void setFacilityLockForApp(int serial, java.lang.String facility, boolean lockState, java.lang.String password, int serviceClass, java.lang.String appId) throws android.os.RemoteException
    {
    }
    @Override public void setResponseFunctions(android.hardware.radio.sim.IRadioSimResponse radioSimResponse, android.hardware.radio.sim.IRadioSimIndication radioSimIndication) throws android.os.RemoteException
    {
    }
    @Override public void setSimCardPower(int serial, int powerUp) throws android.os.RemoteException
    {
    }
    @Override public void setUiccSubscription(int serial, android.hardware.radio.sim.SelectUiccSub uiccSub) throws android.os.RemoteException
    {
    }
    @Override public void supplyIccPin2ForApp(int serial, java.lang.String pin2, java.lang.String aid) throws android.os.RemoteException
    {
    }
    @Override public void supplyIccPinForApp(int serial, java.lang.String pin, java.lang.String aid) throws android.os.RemoteException
    {
    }
    @Override public void supplyIccPuk2ForApp(int serial, java.lang.String puk2, java.lang.String pin2, java.lang.String aid) throws android.os.RemoteException
    {
    }
    @Override public void supplyIccPukForApp(int serial, java.lang.String puk, java.lang.String pin, java.lang.String aid) throws android.os.RemoteException
    {
    }
    @Override public void supplySimDepersonalization(int serial, int persoType, java.lang.String controlKey) throws android.os.RemoteException
    {
    }
    @Override public void updateSimPhonebookRecords(int serial, android.hardware.radio.sim.PhonebookRecordInfo recordInfo) throws android.os.RemoteException
    {
    }
    @Override public void iccCloseLogicalChannelWithSessionInfo(int serial, android.hardware.radio.sim.SessionInfo sessionInfo) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.radio.sim.IRadioSim
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.radio.sim.IRadioSim interface,
     * generating a proxy if needed.
     */
    public static android.hardware.radio.sim.IRadioSim asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.radio.sim.IRadioSim))) {
        return ((android.hardware.radio.sim.IRadioSim)iin);
      }
      return new android.hardware.radio.sim.IRadioSim.Stub.Proxy(obj);
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
        case TRANSACTION_areUiccApplicationsEnabled:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.areUiccApplicationsEnabled(_arg0);
          break;
        }
        case TRANSACTION_changeIccPin2ForApp:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          java.lang.String _arg3;
          _arg3 = data.readString();
          data.enforceNoDataAvail();
          this.changeIccPin2ForApp(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_changeIccPinForApp:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          java.lang.String _arg3;
          _arg3 = data.readString();
          data.enforceNoDataAvail();
          this.changeIccPinForApp(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_enableUiccApplications:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.enableUiccApplications(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getAllowedCarriers:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getAllowedCarriers(_arg0);
          break;
        }
        case TRANSACTION_getCdmaSubscription:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getCdmaSubscription(_arg0);
          break;
        }
        case TRANSACTION_getCdmaSubscriptionSource:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getCdmaSubscriptionSource(_arg0);
          break;
        }
        case TRANSACTION_getFacilityLockForApp:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          int _arg3;
          _arg3 = data.readInt();
          java.lang.String _arg4;
          _arg4 = data.readString();
          data.enforceNoDataAvail();
          this.getFacilityLockForApp(_arg0, _arg1, _arg2, _arg3, _arg4);
          break;
        }
        case TRANSACTION_getIccCardStatus:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getIccCardStatus(_arg0);
          break;
        }
        case TRANSACTION_getImsiForApp:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          data.enforceNoDataAvail();
          this.getImsiForApp(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getSimPhonebookCapacity:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getSimPhonebookCapacity(_arg0);
          break;
        }
        case TRANSACTION_getSimPhonebookRecords:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getSimPhonebookRecords(_arg0);
          break;
        }
        case TRANSACTION_iccCloseLogicalChannel:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.iccCloseLogicalChannel(_arg0, _arg1);
          break;
        }
        case TRANSACTION_iccIoForApp:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.sim.IccIo _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.sim.IccIo.CREATOR);
          data.enforceNoDataAvail();
          this.iccIoForApp(_arg0, _arg1);
          break;
        }
        case TRANSACTION_iccOpenLogicalChannel:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.iccOpenLogicalChannel(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_iccTransmitApduBasicChannel:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.sim.SimApdu _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.sim.SimApdu.CREATOR);
          data.enforceNoDataAvail();
          this.iccTransmitApduBasicChannel(_arg0, _arg1);
          break;
        }
        case TRANSACTION_iccTransmitApduLogicalChannel:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.sim.SimApdu _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.sim.SimApdu.CREATOR);
          data.enforceNoDataAvail();
          this.iccTransmitApduLogicalChannel(_arg0, _arg1);
          break;
        }
        case TRANSACTION_reportStkServiceIsRunning:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.reportStkServiceIsRunning(_arg0);
          break;
        }
        case TRANSACTION_requestIccSimAuthentication:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          java.lang.String _arg2;
          _arg2 = data.readString();
          java.lang.String _arg3;
          _arg3 = data.readString();
          data.enforceNoDataAvail();
          this.requestIccSimAuthentication(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_responseAcknowledgement:
        {
          this.responseAcknowledgement();
          break;
        }
        case TRANSACTION_sendEnvelope:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          data.enforceNoDataAvail();
          this.sendEnvelope(_arg0, _arg1);
          break;
        }
        case TRANSACTION_sendEnvelopeWithStatus:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          data.enforceNoDataAvail();
          this.sendEnvelopeWithStatus(_arg0, _arg1);
          break;
        }
        case TRANSACTION_sendTerminalResponseToSim:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          data.enforceNoDataAvail();
          this.sendTerminalResponseToSim(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setAllowedCarriers:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.sim.CarrierRestrictions _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.sim.CarrierRestrictions.CREATOR);
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.setAllowedCarriers(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_setCarrierInfoForImsiEncryption:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.sim.ImsiEncryptionInfo _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.sim.ImsiEncryptionInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setCarrierInfoForImsiEncryption(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setCdmaSubscriptionSource:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.setCdmaSubscriptionSource(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setFacilityLockForApp:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          boolean _arg2;
          _arg2 = data.readBoolean();
          java.lang.String _arg3;
          _arg3 = data.readString();
          int _arg4;
          _arg4 = data.readInt();
          java.lang.String _arg5;
          _arg5 = data.readString();
          data.enforceNoDataAvail();
          this.setFacilityLockForApp(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
          break;
        }
        case TRANSACTION_setResponseFunctions:
        {
          android.hardware.radio.sim.IRadioSimResponse _arg0;
          _arg0 = android.hardware.radio.sim.IRadioSimResponse.Stub.asInterface(data.readStrongBinder());
          android.hardware.radio.sim.IRadioSimIndication _arg1;
          _arg1 = android.hardware.radio.sim.IRadioSimIndication.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.setResponseFunctions(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setSimCardPower:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.setSimCardPower(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setUiccSubscription:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.sim.SelectUiccSub _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.sim.SelectUiccSub.CREATOR);
          data.enforceNoDataAvail();
          this.setUiccSubscription(_arg0, _arg1);
          break;
        }
        case TRANSACTION_supplyIccPin2ForApp:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          data.enforceNoDataAvail();
          this.supplyIccPin2ForApp(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_supplyIccPinForApp:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          data.enforceNoDataAvail();
          this.supplyIccPinForApp(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_supplyIccPuk2ForApp:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          java.lang.String _arg3;
          _arg3 = data.readString();
          data.enforceNoDataAvail();
          this.supplyIccPuk2ForApp(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_supplyIccPukForApp:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          java.lang.String _arg3;
          _arg3 = data.readString();
          data.enforceNoDataAvail();
          this.supplyIccPukForApp(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_supplySimDepersonalization:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          java.lang.String _arg2;
          _arg2 = data.readString();
          data.enforceNoDataAvail();
          this.supplySimDepersonalization(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_updateSimPhonebookRecords:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.sim.PhonebookRecordInfo _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.sim.PhonebookRecordInfo.CREATOR);
          data.enforceNoDataAvail();
          this.updateSimPhonebookRecords(_arg0, _arg1);
          break;
        }
        case TRANSACTION_iccCloseLogicalChannelWithSessionInfo:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.sim.SessionInfo _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.sim.SessionInfo.CREATOR);
          data.enforceNoDataAvail();
          this.iccCloseLogicalChannelWithSessionInfo(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.radio.sim.IRadioSim
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
      @Override public void areUiccApplicationsEnabled(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_areUiccApplicationsEnabled, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method areUiccApplicationsEnabled is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void changeIccPin2ForApp(int serial, java.lang.String oldPin2, java.lang.String newPin2, java.lang.String aid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(oldPin2);
          _data.writeString(newPin2);
          _data.writeString(aid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_changeIccPin2ForApp, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method changeIccPin2ForApp is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void changeIccPinForApp(int serial, java.lang.String oldPin, java.lang.String newPin, java.lang.String aid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(oldPin);
          _data.writeString(newPin);
          _data.writeString(aid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_changeIccPinForApp, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method changeIccPinForApp is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void enableUiccApplications(int serial, boolean enable) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeBoolean(enable);
          boolean _status = mRemote.transact(Stub.TRANSACTION_enableUiccApplications, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method enableUiccApplications is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getAllowedCarriers(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getAllowedCarriers, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getAllowedCarriers is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getCdmaSubscription(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCdmaSubscription, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getCdmaSubscription is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getCdmaSubscriptionSource(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCdmaSubscriptionSource, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getCdmaSubscriptionSource is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getFacilityLockForApp(int serial, java.lang.String facility, java.lang.String password, int serviceClass, java.lang.String appId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(facility);
          _data.writeString(password);
          _data.writeInt(serviceClass);
          _data.writeString(appId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getFacilityLockForApp, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getFacilityLockForApp is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getIccCardStatus(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getIccCardStatus, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getIccCardStatus is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getImsiForApp(int serial, java.lang.String aid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(aid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getImsiForApp, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getImsiForApp is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getSimPhonebookCapacity(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSimPhonebookCapacity, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getSimPhonebookCapacity is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getSimPhonebookRecords(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSimPhonebookRecords, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getSimPhonebookRecords is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      /** @deprecated use iccCloseLogicalChannelWithSessionInfo instead. */
      @Override public void iccCloseLogicalChannel(int serial, int channelId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(channelId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_iccCloseLogicalChannel, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method iccCloseLogicalChannel is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void iccIoForApp(int serial, android.hardware.radio.sim.IccIo iccIo) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedObject(iccIo, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_iccIoForApp, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method iccIoForApp is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void iccOpenLogicalChannel(int serial, java.lang.String aid, int p2) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(aid);
          _data.writeInt(p2);
          boolean _status = mRemote.transact(Stub.TRANSACTION_iccOpenLogicalChannel, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method iccOpenLogicalChannel is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void iccTransmitApduBasicChannel(int serial, android.hardware.radio.sim.SimApdu message) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedObject(message, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_iccTransmitApduBasicChannel, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method iccTransmitApduBasicChannel is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void iccTransmitApduLogicalChannel(int serial, android.hardware.radio.sim.SimApdu message) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedObject(message, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_iccTransmitApduLogicalChannel, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method iccTransmitApduLogicalChannel is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void reportStkServiceIsRunning(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_reportStkServiceIsRunning, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method reportStkServiceIsRunning is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void requestIccSimAuthentication(int serial, int authContext, java.lang.String authData, java.lang.String aid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(authContext);
          _data.writeString(authData);
          _data.writeString(aid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_requestIccSimAuthentication, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method requestIccSimAuthentication is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void responseAcknowledgement() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_responseAcknowledgement, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method responseAcknowledgement is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendEnvelope(int serial, java.lang.String contents) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(contents);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendEnvelope, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendEnvelope is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendEnvelopeWithStatus(int serial, java.lang.String contents) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(contents);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendEnvelopeWithStatus, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendEnvelopeWithStatus is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendTerminalResponseToSim(int serial, java.lang.String contents) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(contents);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendTerminalResponseToSim, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendTerminalResponseToSim is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setAllowedCarriers(int serial, android.hardware.radio.sim.CarrierRestrictions carriers, int multiSimPolicy) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedObject(carriers, 0);
          _data.writeInt(multiSimPolicy);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setAllowedCarriers, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setAllowedCarriers is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setCarrierInfoForImsiEncryption(int serial, android.hardware.radio.sim.ImsiEncryptionInfo imsiEncryptionInfo) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedObject(imsiEncryptionInfo, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCarrierInfoForImsiEncryption, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setCarrierInfoForImsiEncryption is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setCdmaSubscriptionSource(int serial, int cdmaSub) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(cdmaSub);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCdmaSubscriptionSource, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setCdmaSubscriptionSource is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setFacilityLockForApp(int serial, java.lang.String facility, boolean lockState, java.lang.String password, int serviceClass, java.lang.String appId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(facility);
          _data.writeBoolean(lockState);
          _data.writeString(password);
          _data.writeInt(serviceClass);
          _data.writeString(appId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setFacilityLockForApp, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setFacilityLockForApp is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setResponseFunctions(android.hardware.radio.sim.IRadioSimResponse radioSimResponse, android.hardware.radio.sim.IRadioSimIndication radioSimIndication) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(radioSimResponse);
          _data.writeStrongInterface(radioSimIndication);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setResponseFunctions, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setResponseFunctions is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setSimCardPower(int serial, int powerUp) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(powerUp);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setSimCardPower, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setSimCardPower is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setUiccSubscription(int serial, android.hardware.radio.sim.SelectUiccSub uiccSub) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedObject(uiccSub, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setUiccSubscription, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setUiccSubscription is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void supplyIccPin2ForApp(int serial, java.lang.String pin2, java.lang.String aid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(pin2);
          _data.writeString(aid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_supplyIccPin2ForApp, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method supplyIccPin2ForApp is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void supplyIccPinForApp(int serial, java.lang.String pin, java.lang.String aid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(pin);
          _data.writeString(aid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_supplyIccPinForApp, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method supplyIccPinForApp is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void supplyIccPuk2ForApp(int serial, java.lang.String puk2, java.lang.String pin2, java.lang.String aid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(puk2);
          _data.writeString(pin2);
          _data.writeString(aid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_supplyIccPuk2ForApp, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method supplyIccPuk2ForApp is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void supplyIccPukForApp(int serial, java.lang.String puk, java.lang.String pin, java.lang.String aid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(puk);
          _data.writeString(pin);
          _data.writeString(aid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_supplyIccPukForApp, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method supplyIccPukForApp is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void supplySimDepersonalization(int serial, int persoType, java.lang.String controlKey) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(persoType);
          _data.writeString(controlKey);
          boolean _status = mRemote.transact(Stub.TRANSACTION_supplySimDepersonalization, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method supplySimDepersonalization is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void updateSimPhonebookRecords(int serial, android.hardware.radio.sim.PhonebookRecordInfo recordInfo) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedObject(recordInfo, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_updateSimPhonebookRecords, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method updateSimPhonebookRecords is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void iccCloseLogicalChannelWithSessionInfo(int serial, android.hardware.radio.sim.SessionInfo sessionInfo) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedObject(sessionInfo, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_iccCloseLogicalChannelWithSessionInfo, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method iccCloseLogicalChannelWithSessionInfo is unimplemented.");
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
    static final int TRANSACTION_areUiccApplicationsEnabled = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_changeIccPin2ForApp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_changeIccPinForApp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_enableUiccApplications = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getAllowedCarriers = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getCdmaSubscription = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getCdmaSubscriptionSource = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getFacilityLockForApp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_getIccCardStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_getImsiForApp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_getSimPhonebookCapacity = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_getSimPhonebookRecords = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_iccCloseLogicalChannel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_iccIoForApp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_iccOpenLogicalChannel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_iccTransmitApduBasicChannel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_iccTransmitApduLogicalChannel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_reportStkServiceIsRunning = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_requestIccSimAuthentication = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_responseAcknowledgement = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
    static final int TRANSACTION_sendEnvelope = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
    static final int TRANSACTION_sendEnvelopeWithStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
    static final int TRANSACTION_sendTerminalResponseToSim = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
    static final int TRANSACTION_setAllowedCarriers = (android.os.IBinder.FIRST_CALL_TRANSACTION + 23);
    static final int TRANSACTION_setCarrierInfoForImsiEncryption = (android.os.IBinder.FIRST_CALL_TRANSACTION + 24);
    static final int TRANSACTION_setCdmaSubscriptionSource = (android.os.IBinder.FIRST_CALL_TRANSACTION + 25);
    static final int TRANSACTION_setFacilityLockForApp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 26);
    static final int TRANSACTION_setResponseFunctions = (android.os.IBinder.FIRST_CALL_TRANSACTION + 27);
    static final int TRANSACTION_setSimCardPower = (android.os.IBinder.FIRST_CALL_TRANSACTION + 28);
    static final int TRANSACTION_setUiccSubscription = (android.os.IBinder.FIRST_CALL_TRANSACTION + 29);
    static final int TRANSACTION_supplyIccPin2ForApp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 30);
    static final int TRANSACTION_supplyIccPinForApp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 31);
    static final int TRANSACTION_supplyIccPuk2ForApp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 32);
    static final int TRANSACTION_supplyIccPukForApp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 33);
    static final int TRANSACTION_supplySimDepersonalization = (android.os.IBinder.FIRST_CALL_TRANSACTION + 34);
    static final int TRANSACTION_updateSimPhonebookRecords = (android.os.IBinder.FIRST_CALL_TRANSACTION + 35);
    static final int TRANSACTION_iccCloseLogicalChannelWithSessionInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 36);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$radio$sim$IRadioSim".replace('$', '.');
  public void areUiccApplicationsEnabled(int serial) throws android.os.RemoteException;
  public void changeIccPin2ForApp(int serial, java.lang.String oldPin2, java.lang.String newPin2, java.lang.String aid) throws android.os.RemoteException;
  public void changeIccPinForApp(int serial, java.lang.String oldPin, java.lang.String newPin, java.lang.String aid) throws android.os.RemoteException;
  public void enableUiccApplications(int serial, boolean enable) throws android.os.RemoteException;
  public void getAllowedCarriers(int serial) throws android.os.RemoteException;
  public void getCdmaSubscription(int serial) throws android.os.RemoteException;
  public void getCdmaSubscriptionSource(int serial) throws android.os.RemoteException;
  public void getFacilityLockForApp(int serial, java.lang.String facility, java.lang.String password, int serviceClass, java.lang.String appId) throws android.os.RemoteException;
  public void getIccCardStatus(int serial) throws android.os.RemoteException;
  public void getImsiForApp(int serial, java.lang.String aid) throws android.os.RemoteException;
  public void getSimPhonebookCapacity(int serial) throws android.os.RemoteException;
  public void getSimPhonebookRecords(int serial) throws android.os.RemoteException;
  /** @deprecated use iccCloseLogicalChannelWithSessionInfo instead. */
  @Deprecated
  public void iccCloseLogicalChannel(int serial, int channelId) throws android.os.RemoteException;
  public void iccIoForApp(int serial, android.hardware.radio.sim.IccIo iccIo) throws android.os.RemoteException;
  public void iccOpenLogicalChannel(int serial, java.lang.String aid, int p2) throws android.os.RemoteException;
  public void iccTransmitApduBasicChannel(int serial, android.hardware.radio.sim.SimApdu message) throws android.os.RemoteException;
  public void iccTransmitApduLogicalChannel(int serial, android.hardware.radio.sim.SimApdu message) throws android.os.RemoteException;
  public void reportStkServiceIsRunning(int serial) throws android.os.RemoteException;
  public void requestIccSimAuthentication(int serial, int authContext, java.lang.String authData, java.lang.String aid) throws android.os.RemoteException;
  public void responseAcknowledgement() throws android.os.RemoteException;
  public void sendEnvelope(int serial, java.lang.String contents) throws android.os.RemoteException;
  public void sendEnvelopeWithStatus(int serial, java.lang.String contents) throws android.os.RemoteException;
  public void sendTerminalResponseToSim(int serial, java.lang.String contents) throws android.os.RemoteException;
  public void setAllowedCarriers(int serial, android.hardware.radio.sim.CarrierRestrictions carriers, int multiSimPolicy) throws android.os.RemoteException;
  public void setCarrierInfoForImsiEncryption(int serial, android.hardware.radio.sim.ImsiEncryptionInfo imsiEncryptionInfo) throws android.os.RemoteException;
  public void setCdmaSubscriptionSource(int serial, int cdmaSub) throws android.os.RemoteException;
  public void setFacilityLockForApp(int serial, java.lang.String facility, boolean lockState, java.lang.String password, int serviceClass, java.lang.String appId) throws android.os.RemoteException;
  public void setResponseFunctions(android.hardware.radio.sim.IRadioSimResponse radioSimResponse, android.hardware.radio.sim.IRadioSimIndication radioSimIndication) throws android.os.RemoteException;
  public void setSimCardPower(int serial, int powerUp) throws android.os.RemoteException;
  public void setUiccSubscription(int serial, android.hardware.radio.sim.SelectUiccSub uiccSub) throws android.os.RemoteException;
  public void supplyIccPin2ForApp(int serial, java.lang.String pin2, java.lang.String aid) throws android.os.RemoteException;
  public void supplyIccPinForApp(int serial, java.lang.String pin, java.lang.String aid) throws android.os.RemoteException;
  public void supplyIccPuk2ForApp(int serial, java.lang.String puk2, java.lang.String pin2, java.lang.String aid) throws android.os.RemoteException;
  public void supplyIccPukForApp(int serial, java.lang.String puk, java.lang.String pin, java.lang.String aid) throws android.os.RemoteException;
  public void supplySimDepersonalization(int serial, int persoType, java.lang.String controlKey) throws android.os.RemoteException;
  public void updateSimPhonebookRecords(int serial, android.hardware.radio.sim.PhonebookRecordInfo recordInfo) throws android.os.RemoteException;
  public void iccCloseLogicalChannelWithSessionInfo(int serial, android.hardware.radio.sim.SessionInfo sessionInfo) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
