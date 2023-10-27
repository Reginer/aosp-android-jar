/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.voice;
public interface IRadioVoice extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "8c5e0d53dc67b5ed221b2da0570a17684d973a20";
  /** Default implementation for IRadioVoice. */
  public static class Default implements android.hardware.radio.voice.IRadioVoice
  {
    @Override public void acceptCall(int serial) throws android.os.RemoteException
    {
    }
    @Override public void cancelPendingUssd(int serial) throws android.os.RemoteException
    {
    }
    @Override public void conference(int serial) throws android.os.RemoteException
    {
    }
    @Override public void dial(int serial, android.hardware.radio.voice.Dial dialInfo) throws android.os.RemoteException
    {
    }
    @Override public void emergencyDial(int serial, android.hardware.radio.voice.Dial dialInfo, int categories, java.lang.String[] urns, int routing, boolean hasKnownUserIntentEmergency, boolean isTesting) throws android.os.RemoteException
    {
    }
    @Override public void exitEmergencyCallbackMode(int serial) throws android.os.RemoteException
    {
    }
    @Override public void explicitCallTransfer(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getCallForwardStatus(int serial, android.hardware.radio.voice.CallForwardInfo callInfo) throws android.os.RemoteException
    {
    }
    @Override public void getCallWaiting(int serial, int serviceClass) throws android.os.RemoteException
    {
    }
    @Override public void getClip(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getClir(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getCurrentCalls(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getLastCallFailCause(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getMute(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getPreferredVoicePrivacy(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getTtyMode(int serial) throws android.os.RemoteException
    {
    }
    @Override public void handleStkCallSetupRequestFromSim(int serial, boolean accept) throws android.os.RemoteException
    {
    }
    @Override public void hangup(int serial, int gsmIndex) throws android.os.RemoteException
    {
    }
    @Override public void hangupForegroundResumeBackground(int serial) throws android.os.RemoteException
    {
    }
    @Override public void hangupWaitingOrBackground(int serial) throws android.os.RemoteException
    {
    }
    @Override public void isVoNrEnabled(int serial) throws android.os.RemoteException
    {
    }
    @Override public void rejectCall(int serial) throws android.os.RemoteException
    {
    }
    @Override public void responseAcknowledgement() throws android.os.RemoteException
    {
    }
    @Override public void sendBurstDtmf(int serial, java.lang.String dtmf, int on, int off) throws android.os.RemoteException
    {
    }
    @Override public void sendCdmaFeatureCode(int serial, java.lang.String featureCode) throws android.os.RemoteException
    {
    }
    @Override public void sendDtmf(int serial, java.lang.String s) throws android.os.RemoteException
    {
    }
    @Override public void sendUssd(int serial, java.lang.String ussd) throws android.os.RemoteException
    {
    }
    @Override public void separateConnection(int serial, int gsmIndex) throws android.os.RemoteException
    {
    }
    @Override public void setCallForward(int serial, android.hardware.radio.voice.CallForwardInfo callInfo) throws android.os.RemoteException
    {
    }
    @Override public void setCallWaiting(int serial, boolean enable, int serviceClass) throws android.os.RemoteException
    {
    }
    @Override public void setClir(int serial, int status) throws android.os.RemoteException
    {
    }
    @Override public void setMute(int serial, boolean enable) throws android.os.RemoteException
    {
    }
    @Override public void setPreferredVoicePrivacy(int serial, boolean enable) throws android.os.RemoteException
    {
    }
    @Override public void setResponseFunctions(android.hardware.radio.voice.IRadioVoiceResponse radioVoiceResponse, android.hardware.radio.voice.IRadioVoiceIndication radioVoiceIndication) throws android.os.RemoteException
    {
    }
    @Override public void setTtyMode(int serial, int mode) throws android.os.RemoteException
    {
    }
    @Override public void setVoNrEnabled(int serial, boolean enable) throws android.os.RemoteException
    {
    }
    @Override public void startDtmf(int serial, java.lang.String s) throws android.os.RemoteException
    {
    }
    @Override public void stopDtmf(int serial) throws android.os.RemoteException
    {
    }
    @Override public void switchWaitingOrHoldingAndActive(int serial) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.radio.voice.IRadioVoice
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.radio.voice.IRadioVoice interface,
     * generating a proxy if needed.
     */
    public static android.hardware.radio.voice.IRadioVoice asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.radio.voice.IRadioVoice))) {
        return ((android.hardware.radio.voice.IRadioVoice)iin);
      }
      return new android.hardware.radio.voice.IRadioVoice.Stub.Proxy(obj);
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
        case TRANSACTION_acceptCall:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.acceptCall(_arg0);
          break;
        }
        case TRANSACTION_cancelPendingUssd:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.cancelPendingUssd(_arg0);
          break;
        }
        case TRANSACTION_conference:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.conference(_arg0);
          break;
        }
        case TRANSACTION_dial:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.voice.Dial _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.voice.Dial.CREATOR);
          data.enforceNoDataAvail();
          this.dial(_arg0, _arg1);
          break;
        }
        case TRANSACTION_emergencyDial:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.voice.Dial _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.voice.Dial.CREATOR);
          int _arg2;
          _arg2 = data.readInt();
          java.lang.String[] _arg3;
          _arg3 = data.createStringArray();
          int _arg4;
          _arg4 = data.readInt();
          boolean _arg5;
          _arg5 = data.readBoolean();
          boolean _arg6;
          _arg6 = data.readBoolean();
          data.enforceNoDataAvail();
          this.emergencyDial(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6);
          break;
        }
        case TRANSACTION_exitEmergencyCallbackMode:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.exitEmergencyCallbackMode(_arg0);
          break;
        }
        case TRANSACTION_explicitCallTransfer:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.explicitCallTransfer(_arg0);
          break;
        }
        case TRANSACTION_getCallForwardStatus:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.voice.CallForwardInfo _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.voice.CallForwardInfo.CREATOR);
          data.enforceNoDataAvail();
          this.getCallForwardStatus(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getCallWaiting:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.getCallWaiting(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getClip:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getClip(_arg0);
          break;
        }
        case TRANSACTION_getClir:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getClir(_arg0);
          break;
        }
        case TRANSACTION_getCurrentCalls:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getCurrentCalls(_arg0);
          break;
        }
        case TRANSACTION_getLastCallFailCause:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getLastCallFailCause(_arg0);
          break;
        }
        case TRANSACTION_getMute:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getMute(_arg0);
          break;
        }
        case TRANSACTION_getPreferredVoicePrivacy:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getPreferredVoicePrivacy(_arg0);
          break;
        }
        case TRANSACTION_getTtyMode:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getTtyMode(_arg0);
          break;
        }
        case TRANSACTION_handleStkCallSetupRequestFromSim:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.handleStkCallSetupRequestFromSim(_arg0, _arg1);
          break;
        }
        case TRANSACTION_hangup:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.hangup(_arg0, _arg1);
          break;
        }
        case TRANSACTION_hangupForegroundResumeBackground:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.hangupForegroundResumeBackground(_arg0);
          break;
        }
        case TRANSACTION_hangupWaitingOrBackground:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.hangupWaitingOrBackground(_arg0);
          break;
        }
        case TRANSACTION_isVoNrEnabled:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.isVoNrEnabled(_arg0);
          break;
        }
        case TRANSACTION_rejectCall:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.rejectCall(_arg0);
          break;
        }
        case TRANSACTION_responseAcknowledgement:
        {
          this.responseAcknowledgement();
          break;
        }
        case TRANSACTION_sendBurstDtmf:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          int _arg2;
          _arg2 = data.readInt();
          int _arg3;
          _arg3 = data.readInt();
          data.enforceNoDataAvail();
          this.sendBurstDtmf(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_sendCdmaFeatureCode:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          data.enforceNoDataAvail();
          this.sendCdmaFeatureCode(_arg0, _arg1);
          break;
        }
        case TRANSACTION_sendDtmf:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          data.enforceNoDataAvail();
          this.sendDtmf(_arg0, _arg1);
          break;
        }
        case TRANSACTION_sendUssd:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          data.enforceNoDataAvail();
          this.sendUssd(_arg0, _arg1);
          break;
        }
        case TRANSACTION_separateConnection:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.separateConnection(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setCallForward:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.voice.CallForwardInfo _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.voice.CallForwardInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setCallForward(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setCallWaiting:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.setCallWaiting(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_setClir:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.setClir(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setMute:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setMute(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setPreferredVoicePrivacy:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setPreferredVoicePrivacy(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setResponseFunctions:
        {
          android.hardware.radio.voice.IRadioVoiceResponse _arg0;
          _arg0 = android.hardware.radio.voice.IRadioVoiceResponse.Stub.asInterface(data.readStrongBinder());
          android.hardware.radio.voice.IRadioVoiceIndication _arg1;
          _arg1 = android.hardware.radio.voice.IRadioVoiceIndication.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.setResponseFunctions(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setTtyMode:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.setTtyMode(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setVoNrEnabled:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setVoNrEnabled(_arg0, _arg1);
          break;
        }
        case TRANSACTION_startDtmf:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          data.enforceNoDataAvail();
          this.startDtmf(_arg0, _arg1);
          break;
        }
        case TRANSACTION_stopDtmf:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.stopDtmf(_arg0);
          break;
        }
        case TRANSACTION_switchWaitingOrHoldingAndActive:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.switchWaitingOrHoldingAndActive(_arg0);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.radio.voice.IRadioVoice
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
      @Override public void acceptCall(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_acceptCall, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method acceptCall is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void cancelPendingUssd(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_cancelPendingUssd, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method cancelPendingUssd is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void conference(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_conference, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method conference is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void dial(int serial, android.hardware.radio.voice.Dial dialInfo) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedObject(dialInfo, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_dial, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method dial is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void emergencyDial(int serial, android.hardware.radio.voice.Dial dialInfo, int categories, java.lang.String[] urns, int routing, boolean hasKnownUserIntentEmergency, boolean isTesting) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedObject(dialInfo, 0);
          _data.writeInt(categories);
          _data.writeStringArray(urns);
          _data.writeInt(routing);
          _data.writeBoolean(hasKnownUserIntentEmergency);
          _data.writeBoolean(isTesting);
          boolean _status = mRemote.transact(Stub.TRANSACTION_emergencyDial, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method emergencyDial is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void exitEmergencyCallbackMode(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_exitEmergencyCallbackMode, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method exitEmergencyCallbackMode is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void explicitCallTransfer(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_explicitCallTransfer, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method explicitCallTransfer is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getCallForwardStatus(int serial, android.hardware.radio.voice.CallForwardInfo callInfo) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedObject(callInfo, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCallForwardStatus, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getCallForwardStatus is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getCallWaiting(int serial, int serviceClass) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(serviceClass);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCallWaiting, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getCallWaiting is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getClip(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getClip, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getClip is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getClir(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getClir, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getClir is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getCurrentCalls(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCurrentCalls, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getCurrentCalls is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getLastCallFailCause(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getLastCallFailCause, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getLastCallFailCause is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getMute(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getMute, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getMute is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getPreferredVoicePrivacy(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getPreferredVoicePrivacy, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getPreferredVoicePrivacy is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getTtyMode(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getTtyMode, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getTtyMode is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void handleStkCallSetupRequestFromSim(int serial, boolean accept) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeBoolean(accept);
          boolean _status = mRemote.transact(Stub.TRANSACTION_handleStkCallSetupRequestFromSim, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method handleStkCallSetupRequestFromSim is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void hangup(int serial, int gsmIndex) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(gsmIndex);
          boolean _status = mRemote.transact(Stub.TRANSACTION_hangup, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method hangup is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void hangupForegroundResumeBackground(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_hangupForegroundResumeBackground, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method hangupForegroundResumeBackground is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void hangupWaitingOrBackground(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_hangupWaitingOrBackground, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method hangupWaitingOrBackground is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isVoNrEnabled(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isVoNrEnabled, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method isVoNrEnabled is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void rejectCall(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_rejectCall, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method rejectCall is unimplemented.");
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
      @Override public void sendBurstDtmf(int serial, java.lang.String dtmf, int on, int off) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(dtmf);
          _data.writeInt(on);
          _data.writeInt(off);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendBurstDtmf, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendBurstDtmf is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendCdmaFeatureCode(int serial, java.lang.String featureCode) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(featureCode);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendCdmaFeatureCode, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendCdmaFeatureCode is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendDtmf(int serial, java.lang.String s) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(s);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendDtmf, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendDtmf is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendUssd(int serial, java.lang.String ussd) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(ussd);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendUssd, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendUssd is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void separateConnection(int serial, int gsmIndex) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(gsmIndex);
          boolean _status = mRemote.transact(Stub.TRANSACTION_separateConnection, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method separateConnection is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setCallForward(int serial, android.hardware.radio.voice.CallForwardInfo callInfo) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedObject(callInfo, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCallForward, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setCallForward is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setCallWaiting(int serial, boolean enable, int serviceClass) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeBoolean(enable);
          _data.writeInt(serviceClass);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCallWaiting, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setCallWaiting is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setClir(int serial, int status) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(status);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setClir, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setClir is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setMute(int serial, boolean enable) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeBoolean(enable);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setMute, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setMute is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setPreferredVoicePrivacy(int serial, boolean enable) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeBoolean(enable);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setPreferredVoicePrivacy, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setPreferredVoicePrivacy is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setResponseFunctions(android.hardware.radio.voice.IRadioVoiceResponse radioVoiceResponse, android.hardware.radio.voice.IRadioVoiceIndication radioVoiceIndication) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(radioVoiceResponse);
          _data.writeStrongInterface(radioVoiceIndication);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setResponseFunctions, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setResponseFunctions is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setTtyMode(int serial, int mode) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(mode);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setTtyMode, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setTtyMode is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setVoNrEnabled(int serial, boolean enable) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeBoolean(enable);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setVoNrEnabled, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setVoNrEnabled is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void startDtmf(int serial, java.lang.String s) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(s);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startDtmf, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method startDtmf is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void stopDtmf(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stopDtmf, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method stopDtmf is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void switchWaitingOrHoldingAndActive(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_switchWaitingOrHoldingAndActive, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method switchWaitingOrHoldingAndActive is unimplemented.");
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
    static final int TRANSACTION_acceptCall = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_cancelPendingUssd = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_conference = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_dial = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_emergencyDial = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_exitEmergencyCallbackMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_explicitCallTransfer = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getCallForwardStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_getCallWaiting = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_getClip = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_getClir = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_getCurrentCalls = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_getLastCallFailCause = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_getMute = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_getPreferredVoicePrivacy = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_getTtyMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_handleStkCallSetupRequestFromSim = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_hangup = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_hangupForegroundResumeBackground = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_hangupWaitingOrBackground = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
    static final int TRANSACTION_isVoNrEnabled = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
    static final int TRANSACTION_rejectCall = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
    static final int TRANSACTION_responseAcknowledgement = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
    static final int TRANSACTION_sendBurstDtmf = (android.os.IBinder.FIRST_CALL_TRANSACTION + 23);
    static final int TRANSACTION_sendCdmaFeatureCode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 24);
    static final int TRANSACTION_sendDtmf = (android.os.IBinder.FIRST_CALL_TRANSACTION + 25);
    static final int TRANSACTION_sendUssd = (android.os.IBinder.FIRST_CALL_TRANSACTION + 26);
    static final int TRANSACTION_separateConnection = (android.os.IBinder.FIRST_CALL_TRANSACTION + 27);
    static final int TRANSACTION_setCallForward = (android.os.IBinder.FIRST_CALL_TRANSACTION + 28);
    static final int TRANSACTION_setCallWaiting = (android.os.IBinder.FIRST_CALL_TRANSACTION + 29);
    static final int TRANSACTION_setClir = (android.os.IBinder.FIRST_CALL_TRANSACTION + 30);
    static final int TRANSACTION_setMute = (android.os.IBinder.FIRST_CALL_TRANSACTION + 31);
    static final int TRANSACTION_setPreferredVoicePrivacy = (android.os.IBinder.FIRST_CALL_TRANSACTION + 32);
    static final int TRANSACTION_setResponseFunctions = (android.os.IBinder.FIRST_CALL_TRANSACTION + 33);
    static final int TRANSACTION_setTtyMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 34);
    static final int TRANSACTION_setVoNrEnabled = (android.os.IBinder.FIRST_CALL_TRANSACTION + 35);
    static final int TRANSACTION_startDtmf = (android.os.IBinder.FIRST_CALL_TRANSACTION + 36);
    static final int TRANSACTION_stopDtmf = (android.os.IBinder.FIRST_CALL_TRANSACTION + 37);
    static final int TRANSACTION_switchWaitingOrHoldingAndActive = (android.os.IBinder.FIRST_CALL_TRANSACTION + 38);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$radio$voice$IRadioVoice".replace('$', '.');
  public void acceptCall(int serial) throws android.os.RemoteException;
  public void cancelPendingUssd(int serial) throws android.os.RemoteException;
  public void conference(int serial) throws android.os.RemoteException;
  public void dial(int serial, android.hardware.radio.voice.Dial dialInfo) throws android.os.RemoteException;
  public void emergencyDial(int serial, android.hardware.radio.voice.Dial dialInfo, int categories, java.lang.String[] urns, int routing, boolean hasKnownUserIntentEmergency, boolean isTesting) throws android.os.RemoteException;
  public void exitEmergencyCallbackMode(int serial) throws android.os.RemoteException;
  public void explicitCallTransfer(int serial) throws android.os.RemoteException;
  public void getCallForwardStatus(int serial, android.hardware.radio.voice.CallForwardInfo callInfo) throws android.os.RemoteException;
  public void getCallWaiting(int serial, int serviceClass) throws android.os.RemoteException;
  public void getClip(int serial) throws android.os.RemoteException;
  public void getClir(int serial) throws android.os.RemoteException;
  public void getCurrentCalls(int serial) throws android.os.RemoteException;
  public void getLastCallFailCause(int serial) throws android.os.RemoteException;
  public void getMute(int serial) throws android.os.RemoteException;
  public void getPreferredVoicePrivacy(int serial) throws android.os.RemoteException;
  public void getTtyMode(int serial) throws android.os.RemoteException;
  public void handleStkCallSetupRequestFromSim(int serial, boolean accept) throws android.os.RemoteException;
  public void hangup(int serial, int gsmIndex) throws android.os.RemoteException;
  public void hangupForegroundResumeBackground(int serial) throws android.os.RemoteException;
  public void hangupWaitingOrBackground(int serial) throws android.os.RemoteException;
  public void isVoNrEnabled(int serial) throws android.os.RemoteException;
  public void rejectCall(int serial) throws android.os.RemoteException;
  public void responseAcknowledgement() throws android.os.RemoteException;
  public void sendBurstDtmf(int serial, java.lang.String dtmf, int on, int off) throws android.os.RemoteException;
  public void sendCdmaFeatureCode(int serial, java.lang.String featureCode) throws android.os.RemoteException;
  public void sendDtmf(int serial, java.lang.String s) throws android.os.RemoteException;
  public void sendUssd(int serial, java.lang.String ussd) throws android.os.RemoteException;
  public void separateConnection(int serial, int gsmIndex) throws android.os.RemoteException;
  public void setCallForward(int serial, android.hardware.radio.voice.CallForwardInfo callInfo) throws android.os.RemoteException;
  public void setCallWaiting(int serial, boolean enable, int serviceClass) throws android.os.RemoteException;
  public void setClir(int serial, int status) throws android.os.RemoteException;
  public void setMute(int serial, boolean enable) throws android.os.RemoteException;
  public void setPreferredVoicePrivacy(int serial, boolean enable) throws android.os.RemoteException;
  public void setResponseFunctions(android.hardware.radio.voice.IRadioVoiceResponse radioVoiceResponse, android.hardware.radio.voice.IRadioVoiceIndication radioVoiceIndication) throws android.os.RemoteException;
  public void setTtyMode(int serial, int mode) throws android.os.RemoteException;
  public void setVoNrEnabled(int serial, boolean enable) throws android.os.RemoteException;
  public void startDtmf(int serial, java.lang.String s) throws android.os.RemoteException;
  public void stopDtmf(int serial) throws android.os.RemoteException;
  public void switchWaitingOrHoldingAndActive(int serial) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
