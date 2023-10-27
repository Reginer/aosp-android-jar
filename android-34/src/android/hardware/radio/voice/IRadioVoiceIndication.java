/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.voice;
public interface IRadioVoiceIndication extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "8c5e0d53dc67b5ed221b2da0570a17684d973a20";
  /** Default implementation for IRadioVoiceIndication. */
  public static class Default implements android.hardware.radio.voice.IRadioVoiceIndication
  {
    @Override public void callRing(int type, boolean isGsm, android.hardware.radio.voice.CdmaSignalInfoRecord record) throws android.os.RemoteException
    {
    }
    @Override public void callStateChanged(int type) throws android.os.RemoteException
    {
    }
    @Override public void cdmaCallWaiting(int type, android.hardware.radio.voice.CdmaCallWaiting callWaitingRecord) throws android.os.RemoteException
    {
    }
    @Override public void cdmaInfoRec(int type, android.hardware.radio.voice.CdmaInformationRecord[] records) throws android.os.RemoteException
    {
    }
    @Override public void cdmaOtaProvisionStatus(int type, int status) throws android.os.RemoteException
    {
    }
    @Override public void currentEmergencyNumberList(int type, android.hardware.radio.voice.EmergencyNumber[] emergencyNumberList) throws android.os.RemoteException
    {
    }
    @Override public void enterEmergencyCallbackMode(int type) throws android.os.RemoteException
    {
    }
    @Override public void exitEmergencyCallbackMode(int type) throws android.os.RemoteException
    {
    }
    @Override public void indicateRingbackTone(int type, boolean start) throws android.os.RemoteException
    {
    }
    @Override public void onSupplementaryServiceIndication(int type, android.hardware.radio.voice.StkCcUnsolSsResult ss) throws android.os.RemoteException
    {
    }
    @Override public void onUssd(int type, int modeType, java.lang.String msg) throws android.os.RemoteException
    {
    }
    @Override public void resendIncallMute(int type) throws android.os.RemoteException
    {
    }
    @Override public void srvccStateNotify(int type, int state) throws android.os.RemoteException
    {
    }
    @Override public void stkCallControlAlphaNotify(int type, java.lang.String alpha) throws android.os.RemoteException
    {
    }
    @Override public void stkCallSetup(int type, long timeout) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.radio.voice.IRadioVoiceIndication
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.radio.voice.IRadioVoiceIndication interface,
     * generating a proxy if needed.
     */
    public static android.hardware.radio.voice.IRadioVoiceIndication asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.radio.voice.IRadioVoiceIndication))) {
        return ((android.hardware.radio.voice.IRadioVoiceIndication)iin);
      }
      return new android.hardware.radio.voice.IRadioVoiceIndication.Stub.Proxy(obj);
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
        case TRANSACTION_callRing:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          android.hardware.radio.voice.CdmaSignalInfoRecord _arg2;
          _arg2 = data.readTypedObject(android.hardware.radio.voice.CdmaSignalInfoRecord.CREATOR);
          data.enforceNoDataAvail();
          this.callRing(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_callStateChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.callStateChanged(_arg0);
          break;
        }
        case TRANSACTION_cdmaCallWaiting:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.voice.CdmaCallWaiting _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.voice.CdmaCallWaiting.CREATOR);
          data.enforceNoDataAvail();
          this.cdmaCallWaiting(_arg0, _arg1);
          break;
        }
        case TRANSACTION_cdmaInfoRec:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.voice.CdmaInformationRecord[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.radio.voice.CdmaInformationRecord.CREATOR);
          data.enforceNoDataAvail();
          this.cdmaInfoRec(_arg0, _arg1);
          break;
        }
        case TRANSACTION_cdmaOtaProvisionStatus:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.cdmaOtaProvisionStatus(_arg0, _arg1);
          break;
        }
        case TRANSACTION_currentEmergencyNumberList:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.voice.EmergencyNumber[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.radio.voice.EmergencyNumber.CREATOR);
          data.enforceNoDataAvail();
          this.currentEmergencyNumberList(_arg0, _arg1);
          break;
        }
        case TRANSACTION_enterEmergencyCallbackMode:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.enterEmergencyCallbackMode(_arg0);
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
        case TRANSACTION_indicateRingbackTone:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.indicateRingbackTone(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onSupplementaryServiceIndication:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.voice.StkCcUnsolSsResult _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.voice.StkCcUnsolSsResult.CREATOR);
          data.enforceNoDataAvail();
          this.onSupplementaryServiceIndication(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onUssd:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          java.lang.String _arg2;
          _arg2 = data.readString();
          data.enforceNoDataAvail();
          this.onUssd(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_resendIncallMute:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.resendIncallMute(_arg0);
          break;
        }
        case TRANSACTION_srvccStateNotify:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.srvccStateNotify(_arg0, _arg1);
          break;
        }
        case TRANSACTION_stkCallControlAlphaNotify:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          data.enforceNoDataAvail();
          this.stkCallControlAlphaNotify(_arg0, _arg1);
          break;
        }
        case TRANSACTION_stkCallSetup:
        {
          int _arg0;
          _arg0 = data.readInt();
          long _arg1;
          _arg1 = data.readLong();
          data.enforceNoDataAvail();
          this.stkCallSetup(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.radio.voice.IRadioVoiceIndication
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
      @Override public void callRing(int type, boolean isGsm, android.hardware.radio.voice.CdmaSignalInfoRecord record) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeBoolean(isGsm);
          _data.writeTypedObject(record, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_callRing, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method callRing is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void callStateChanged(int type) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          boolean _status = mRemote.transact(Stub.TRANSACTION_callStateChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method callStateChanged is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void cdmaCallWaiting(int type, android.hardware.radio.voice.CdmaCallWaiting callWaitingRecord) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedObject(callWaitingRecord, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_cdmaCallWaiting, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method cdmaCallWaiting is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void cdmaInfoRec(int type, android.hardware.radio.voice.CdmaInformationRecord[] records) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedArray(records, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_cdmaInfoRec, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method cdmaInfoRec is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void cdmaOtaProvisionStatus(int type, int status) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeInt(status);
          boolean _status = mRemote.transact(Stub.TRANSACTION_cdmaOtaProvisionStatus, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method cdmaOtaProvisionStatus is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void currentEmergencyNumberList(int type, android.hardware.radio.voice.EmergencyNumber[] emergencyNumberList) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedArray(emergencyNumberList, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_currentEmergencyNumberList, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method currentEmergencyNumberList is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void enterEmergencyCallbackMode(int type) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          boolean _status = mRemote.transact(Stub.TRANSACTION_enterEmergencyCallbackMode, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method enterEmergencyCallbackMode is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void exitEmergencyCallbackMode(int type) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          boolean _status = mRemote.transact(Stub.TRANSACTION_exitEmergencyCallbackMode, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method exitEmergencyCallbackMode is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void indicateRingbackTone(int type, boolean start) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeBoolean(start);
          boolean _status = mRemote.transact(Stub.TRANSACTION_indicateRingbackTone, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method indicateRingbackTone is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onSupplementaryServiceIndication(int type, android.hardware.radio.voice.StkCcUnsolSsResult ss) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedObject(ss, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onSupplementaryServiceIndication, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method onSupplementaryServiceIndication is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onUssd(int type, int modeType, java.lang.String msg) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeInt(modeType);
          _data.writeString(msg);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onUssd, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method onUssd is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void resendIncallMute(int type) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          boolean _status = mRemote.transact(Stub.TRANSACTION_resendIncallMute, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method resendIncallMute is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void srvccStateNotify(int type, int state) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeInt(state);
          boolean _status = mRemote.transact(Stub.TRANSACTION_srvccStateNotify, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method srvccStateNotify is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void stkCallControlAlphaNotify(int type, java.lang.String alpha) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeString(alpha);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stkCallControlAlphaNotify, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method stkCallControlAlphaNotify is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void stkCallSetup(int type, long timeout) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeLong(timeout);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stkCallSetup, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method stkCallSetup is unimplemented.");
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
    static final int TRANSACTION_callRing = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_callStateChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_cdmaCallWaiting = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_cdmaInfoRec = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_cdmaOtaProvisionStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_currentEmergencyNumberList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_enterEmergencyCallbackMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_exitEmergencyCallbackMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_indicateRingbackTone = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_onSupplementaryServiceIndication = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_onUssd = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_resendIncallMute = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_srvccStateNotify = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_stkCallControlAlphaNotify = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_stkCallSetup = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$radio$voice$IRadioVoiceIndication".replace('$', '.');
  public void callRing(int type, boolean isGsm, android.hardware.radio.voice.CdmaSignalInfoRecord record) throws android.os.RemoteException;
  public void callStateChanged(int type) throws android.os.RemoteException;
  public void cdmaCallWaiting(int type, android.hardware.radio.voice.CdmaCallWaiting callWaitingRecord) throws android.os.RemoteException;
  public void cdmaInfoRec(int type, android.hardware.radio.voice.CdmaInformationRecord[] records) throws android.os.RemoteException;
  public void cdmaOtaProvisionStatus(int type, int status) throws android.os.RemoteException;
  public void currentEmergencyNumberList(int type, android.hardware.radio.voice.EmergencyNumber[] emergencyNumberList) throws android.os.RemoteException;
  public void enterEmergencyCallbackMode(int type) throws android.os.RemoteException;
  public void exitEmergencyCallbackMode(int type) throws android.os.RemoteException;
  public void indicateRingbackTone(int type, boolean start) throws android.os.RemoteException;
  public void onSupplementaryServiceIndication(int type, android.hardware.radio.voice.StkCcUnsolSsResult ss) throws android.os.RemoteException;
  public void onUssd(int type, int modeType, java.lang.String msg) throws android.os.RemoteException;
  public void resendIncallMute(int type) throws android.os.RemoteException;
  public void srvccStateNotify(int type, int state) throws android.os.RemoteException;
  public void stkCallControlAlphaNotify(int type, java.lang.String alpha) throws android.os.RemoteException;
  public void stkCallSetup(int type, long timeout) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
