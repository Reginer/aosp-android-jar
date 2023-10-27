/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.voice;
public interface IRadioVoiceResponse extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "8c5e0d53dc67b5ed221b2da0570a17684d973a20";
  /** Default implementation for IRadioVoiceResponse. */
  public static class Default implements android.hardware.radio.voice.IRadioVoiceResponse
  {
    @Override public void acceptCallResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void acknowledgeRequest(int serial) throws android.os.RemoteException
    {
    }
    @Override public void cancelPendingUssdResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void conferenceResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void dialResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void emergencyDialResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void exitEmergencyCallbackModeResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void explicitCallTransferResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void getCallForwardStatusResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.voice.CallForwardInfo[] callForwardInfos) throws android.os.RemoteException
    {
    }
    @Override public void getCallWaitingResponse(android.hardware.radio.RadioResponseInfo info, boolean enable, int serviceClass) throws android.os.RemoteException
    {
    }
    @Override public void getClipResponse(android.hardware.radio.RadioResponseInfo info, int status) throws android.os.RemoteException
    {
    }
    @Override public void getClirResponse(android.hardware.radio.RadioResponseInfo info, int n, int m) throws android.os.RemoteException
    {
    }
    @Override public void getCurrentCallsResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.voice.Call[] calls) throws android.os.RemoteException
    {
    }
    @Override public void getLastCallFailCauseResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.voice.LastCallFailCauseInfo failCauseinfo) throws android.os.RemoteException
    {
    }
    @Override public void getMuteResponse(android.hardware.radio.RadioResponseInfo info, boolean enable) throws android.os.RemoteException
    {
    }
    @Override public void getPreferredVoicePrivacyResponse(android.hardware.radio.RadioResponseInfo info, boolean enable) throws android.os.RemoteException
    {
    }
    @Override public void getTtyModeResponse(android.hardware.radio.RadioResponseInfo info, int mode) throws android.os.RemoteException
    {
    }
    @Override public void handleStkCallSetupRequestFromSimResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void hangupConnectionResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void hangupForegroundResumeBackgroundResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void hangupWaitingOrBackgroundResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void isVoNrEnabledResponse(android.hardware.radio.RadioResponseInfo info, boolean enable) throws android.os.RemoteException
    {
    }
    @Override public void rejectCallResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void sendBurstDtmfResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void sendCdmaFeatureCodeResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void sendDtmfResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void sendUssdResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void separateConnectionResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setCallForwardResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setCallWaitingResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setClirResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setMuteResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setPreferredVoicePrivacyResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setTtyModeResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setVoNrEnabledResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void startDtmfResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void stopDtmfResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void switchWaitingOrHoldingAndActiveResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.radio.voice.IRadioVoiceResponse
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.radio.voice.IRadioVoiceResponse interface,
     * generating a proxy if needed.
     */
    public static android.hardware.radio.voice.IRadioVoiceResponse asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.radio.voice.IRadioVoiceResponse))) {
        return ((android.hardware.radio.voice.IRadioVoiceResponse)iin);
      }
      return new android.hardware.radio.voice.IRadioVoiceResponse.Stub.Proxy(obj);
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
        case TRANSACTION_acceptCallResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.acceptCallResponse(_arg0);
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
        case TRANSACTION_cancelPendingUssdResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.cancelPendingUssdResponse(_arg0);
          break;
        }
        case TRANSACTION_conferenceResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.conferenceResponse(_arg0);
          break;
        }
        case TRANSACTION_dialResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.dialResponse(_arg0);
          break;
        }
        case TRANSACTION_emergencyDialResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.emergencyDialResponse(_arg0);
          break;
        }
        case TRANSACTION_exitEmergencyCallbackModeResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.exitEmergencyCallbackModeResponse(_arg0);
          break;
        }
        case TRANSACTION_explicitCallTransferResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.explicitCallTransferResponse(_arg0);
          break;
        }
        case TRANSACTION_getCallForwardStatusResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.voice.CallForwardInfo[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.radio.voice.CallForwardInfo.CREATOR);
          data.enforceNoDataAvail();
          this.getCallForwardStatusResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getCallWaitingResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          boolean _arg1;
          _arg1 = data.readBoolean();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.getCallWaitingResponse(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_getClipResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.getClipResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getClirResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.getClirResponse(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_getCurrentCallsResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.voice.Call[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.radio.voice.Call.CREATOR);
          data.enforceNoDataAvail();
          this.getCurrentCallsResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getLastCallFailCauseResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.voice.LastCallFailCauseInfo _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.voice.LastCallFailCauseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.getLastCallFailCauseResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getMuteResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.getMuteResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getPreferredVoicePrivacyResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.getPreferredVoicePrivacyResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getTtyModeResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.getTtyModeResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_handleStkCallSetupRequestFromSimResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.handleStkCallSetupRequestFromSimResponse(_arg0);
          break;
        }
        case TRANSACTION_hangupConnectionResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.hangupConnectionResponse(_arg0);
          break;
        }
        case TRANSACTION_hangupForegroundResumeBackgroundResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.hangupForegroundResumeBackgroundResponse(_arg0);
          break;
        }
        case TRANSACTION_hangupWaitingOrBackgroundResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.hangupWaitingOrBackgroundResponse(_arg0);
          break;
        }
        case TRANSACTION_isVoNrEnabledResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.isVoNrEnabledResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_rejectCallResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.rejectCallResponse(_arg0);
          break;
        }
        case TRANSACTION_sendBurstDtmfResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.sendBurstDtmfResponse(_arg0);
          break;
        }
        case TRANSACTION_sendCdmaFeatureCodeResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.sendCdmaFeatureCodeResponse(_arg0);
          break;
        }
        case TRANSACTION_sendDtmfResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.sendDtmfResponse(_arg0);
          break;
        }
        case TRANSACTION_sendUssdResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.sendUssdResponse(_arg0);
          break;
        }
        case TRANSACTION_separateConnectionResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.separateConnectionResponse(_arg0);
          break;
        }
        case TRANSACTION_setCallForwardResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setCallForwardResponse(_arg0);
          break;
        }
        case TRANSACTION_setCallWaitingResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setCallWaitingResponse(_arg0);
          break;
        }
        case TRANSACTION_setClirResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setClirResponse(_arg0);
          break;
        }
        case TRANSACTION_setMuteResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setMuteResponse(_arg0);
          break;
        }
        case TRANSACTION_setPreferredVoicePrivacyResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setPreferredVoicePrivacyResponse(_arg0);
          break;
        }
        case TRANSACTION_setTtyModeResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setTtyModeResponse(_arg0);
          break;
        }
        case TRANSACTION_setVoNrEnabledResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setVoNrEnabledResponse(_arg0);
          break;
        }
        case TRANSACTION_startDtmfResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.startDtmfResponse(_arg0);
          break;
        }
        case TRANSACTION_stopDtmfResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.stopDtmfResponse(_arg0);
          break;
        }
        case TRANSACTION_switchWaitingOrHoldingAndActiveResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.switchWaitingOrHoldingAndActiveResponse(_arg0);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.radio.voice.IRadioVoiceResponse
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
      @Override public void acceptCallResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_acceptCallResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method acceptCallResponse is unimplemented.");
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
      @Override public void cancelPendingUssdResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_cancelPendingUssdResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method cancelPendingUssdResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void conferenceResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_conferenceResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method conferenceResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void dialResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_dialResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method dialResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void emergencyDialResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_emergencyDialResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method emergencyDialResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void exitEmergencyCallbackModeResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_exitEmergencyCallbackModeResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method exitEmergencyCallbackModeResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void explicitCallTransferResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_explicitCallTransferResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method explicitCallTransferResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getCallForwardStatusResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.voice.CallForwardInfo[] callForwardInfos) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedArray(callForwardInfos, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCallForwardStatusResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getCallForwardStatusResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getCallWaitingResponse(android.hardware.radio.RadioResponseInfo info, boolean enable, int serviceClass) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeBoolean(enable);
          _data.writeInt(serviceClass);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCallWaitingResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getCallWaitingResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getClipResponse(android.hardware.radio.RadioResponseInfo info, int status) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeInt(status);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getClipResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getClipResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getClirResponse(android.hardware.radio.RadioResponseInfo info, int n, int m) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeInt(n);
          _data.writeInt(m);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getClirResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getClirResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getCurrentCallsResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.voice.Call[] calls) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedArray(calls, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCurrentCallsResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getCurrentCallsResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getLastCallFailCauseResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.voice.LastCallFailCauseInfo failCauseinfo) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(failCauseinfo, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getLastCallFailCauseResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getLastCallFailCauseResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getMuteResponse(android.hardware.radio.RadioResponseInfo info, boolean enable) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeBoolean(enable);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getMuteResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getMuteResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getPreferredVoicePrivacyResponse(android.hardware.radio.RadioResponseInfo info, boolean enable) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeBoolean(enable);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getPreferredVoicePrivacyResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getPreferredVoicePrivacyResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getTtyModeResponse(android.hardware.radio.RadioResponseInfo info, int mode) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeInt(mode);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getTtyModeResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getTtyModeResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void handleStkCallSetupRequestFromSimResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_handleStkCallSetupRequestFromSimResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method handleStkCallSetupRequestFromSimResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void hangupConnectionResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_hangupConnectionResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method hangupConnectionResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void hangupForegroundResumeBackgroundResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_hangupForegroundResumeBackgroundResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method hangupForegroundResumeBackgroundResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void hangupWaitingOrBackgroundResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_hangupWaitingOrBackgroundResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method hangupWaitingOrBackgroundResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isVoNrEnabledResponse(android.hardware.radio.RadioResponseInfo info, boolean enable) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeBoolean(enable);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isVoNrEnabledResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method isVoNrEnabledResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void rejectCallResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_rejectCallResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method rejectCallResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendBurstDtmfResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendBurstDtmfResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendBurstDtmfResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendCdmaFeatureCodeResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendCdmaFeatureCodeResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendCdmaFeatureCodeResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendDtmfResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendDtmfResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendDtmfResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendUssdResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendUssdResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendUssdResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void separateConnectionResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_separateConnectionResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method separateConnectionResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setCallForwardResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCallForwardResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setCallForwardResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setCallWaitingResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCallWaitingResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setCallWaitingResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setClirResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setClirResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setClirResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setMuteResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setMuteResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setMuteResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setPreferredVoicePrivacyResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setPreferredVoicePrivacyResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setPreferredVoicePrivacyResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setTtyModeResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setTtyModeResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setTtyModeResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setVoNrEnabledResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setVoNrEnabledResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setVoNrEnabledResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void startDtmfResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startDtmfResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method startDtmfResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void stopDtmfResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stopDtmfResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method stopDtmfResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void switchWaitingOrHoldingAndActiveResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_switchWaitingOrHoldingAndActiveResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method switchWaitingOrHoldingAndActiveResponse is unimplemented.");
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
    static final int TRANSACTION_acceptCallResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_acknowledgeRequest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_cancelPendingUssdResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_conferenceResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_dialResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_emergencyDialResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_exitEmergencyCallbackModeResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_explicitCallTransferResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_getCallForwardStatusResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_getCallWaitingResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_getClipResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_getClirResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_getCurrentCallsResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_getLastCallFailCauseResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_getMuteResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_getPreferredVoicePrivacyResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_getTtyModeResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_handleStkCallSetupRequestFromSimResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_hangupConnectionResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_hangupForegroundResumeBackgroundResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
    static final int TRANSACTION_hangupWaitingOrBackgroundResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
    static final int TRANSACTION_isVoNrEnabledResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
    static final int TRANSACTION_rejectCallResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
    static final int TRANSACTION_sendBurstDtmfResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 23);
    static final int TRANSACTION_sendCdmaFeatureCodeResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 24);
    static final int TRANSACTION_sendDtmfResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 25);
    static final int TRANSACTION_sendUssdResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 26);
    static final int TRANSACTION_separateConnectionResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 27);
    static final int TRANSACTION_setCallForwardResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 28);
    static final int TRANSACTION_setCallWaitingResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 29);
    static final int TRANSACTION_setClirResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 30);
    static final int TRANSACTION_setMuteResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 31);
    static final int TRANSACTION_setPreferredVoicePrivacyResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 32);
    static final int TRANSACTION_setTtyModeResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 33);
    static final int TRANSACTION_setVoNrEnabledResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 34);
    static final int TRANSACTION_startDtmfResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 35);
    static final int TRANSACTION_stopDtmfResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 36);
    static final int TRANSACTION_switchWaitingOrHoldingAndActiveResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 37);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$radio$voice$IRadioVoiceResponse".replace('$', '.');
  public void acceptCallResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void acknowledgeRequest(int serial) throws android.os.RemoteException;
  public void cancelPendingUssdResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void conferenceResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void dialResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void emergencyDialResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void exitEmergencyCallbackModeResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void explicitCallTransferResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void getCallForwardStatusResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.voice.CallForwardInfo[] callForwardInfos) throws android.os.RemoteException;
  public void getCallWaitingResponse(android.hardware.radio.RadioResponseInfo info, boolean enable, int serviceClass) throws android.os.RemoteException;
  public void getClipResponse(android.hardware.radio.RadioResponseInfo info, int status) throws android.os.RemoteException;
  public void getClirResponse(android.hardware.radio.RadioResponseInfo info, int n, int m) throws android.os.RemoteException;
  public void getCurrentCallsResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.voice.Call[] calls) throws android.os.RemoteException;
  public void getLastCallFailCauseResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.voice.LastCallFailCauseInfo failCauseinfo) throws android.os.RemoteException;
  public void getMuteResponse(android.hardware.radio.RadioResponseInfo info, boolean enable) throws android.os.RemoteException;
  public void getPreferredVoicePrivacyResponse(android.hardware.radio.RadioResponseInfo info, boolean enable) throws android.os.RemoteException;
  public void getTtyModeResponse(android.hardware.radio.RadioResponseInfo info, int mode) throws android.os.RemoteException;
  public void handleStkCallSetupRequestFromSimResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void hangupConnectionResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void hangupForegroundResumeBackgroundResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void hangupWaitingOrBackgroundResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void isVoNrEnabledResponse(android.hardware.radio.RadioResponseInfo info, boolean enable) throws android.os.RemoteException;
  public void rejectCallResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void sendBurstDtmfResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void sendCdmaFeatureCodeResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void sendDtmfResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void sendUssdResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void separateConnectionResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setCallForwardResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setCallWaitingResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setClirResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setMuteResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setPreferredVoicePrivacyResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setTtyModeResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setVoNrEnabledResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void startDtmfResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void stopDtmfResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void switchWaitingOrHoldingAndActiveResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
