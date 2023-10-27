/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.data;
public interface IRadioData extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "cb458326b02e0e87143f24118543e8cc7d6a9e8e";
  /** Default implementation for IRadioData. */
  public static class Default implements android.hardware.radio.data.IRadioData
  {
    @Override public void allocatePduSessionId(int serial) throws android.os.RemoteException
    {
    }
    @Override public void cancelHandover(int serial, int callId) throws android.os.RemoteException
    {
    }
    @Override public void deactivateDataCall(int serial, int cid, int reason) throws android.os.RemoteException
    {
    }
    @Override public void getDataCallList(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getSlicingConfig(int serial) throws android.os.RemoteException
    {
    }
    @Override public void releasePduSessionId(int serial, int id) throws android.os.RemoteException
    {
    }
    @Override public void responseAcknowledgement() throws android.os.RemoteException
    {
    }
    @Override public void setDataAllowed(int serial, boolean allow) throws android.os.RemoteException
    {
    }
    @Override public void setDataProfile(int serial, android.hardware.radio.data.DataProfileInfo[] profiles) throws android.os.RemoteException
    {
    }
    @Override public void setDataThrottling(int serial, byte dataThrottlingAction, long completionDurationMillis) throws android.os.RemoteException
    {
    }
    @Override public void setInitialAttachApn(int serial, android.hardware.radio.data.DataProfileInfo dataProfileInfo) throws android.os.RemoteException
    {
    }
    @Override public void setResponseFunctions(android.hardware.radio.data.IRadioDataResponse radioDataResponse, android.hardware.radio.data.IRadioDataIndication radioDataIndication) throws android.os.RemoteException
    {
    }
    @Override public void setupDataCall(int serial, int accessNetwork, android.hardware.radio.data.DataProfileInfo dataProfileInfo, boolean roamingAllowed, int reason, android.hardware.radio.data.LinkAddress[] addresses, java.lang.String[] dnses, int pduSessionId, android.hardware.radio.data.SliceInfo sliceInfo, boolean matchAllRuleAllowed) throws android.os.RemoteException
    {
    }
    @Override public void startHandover(int serial, int callId) throws android.os.RemoteException
    {
    }
    @Override public void startKeepalive(int serial, android.hardware.radio.data.KeepaliveRequest keepalive) throws android.os.RemoteException
    {
    }
    @Override public void stopKeepalive(int serial, int sessionHandle) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.radio.data.IRadioData
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.radio.data.IRadioData interface,
     * generating a proxy if needed.
     */
    public static android.hardware.radio.data.IRadioData asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.radio.data.IRadioData))) {
        return ((android.hardware.radio.data.IRadioData)iin);
      }
      return new android.hardware.radio.data.IRadioData.Stub.Proxy(obj);
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
        case TRANSACTION_allocatePduSessionId:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.allocatePduSessionId(_arg0);
          break;
        }
        case TRANSACTION_cancelHandover:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.cancelHandover(_arg0, _arg1);
          break;
        }
        case TRANSACTION_deactivateDataCall:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.deactivateDataCall(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_getDataCallList:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getDataCallList(_arg0);
          break;
        }
        case TRANSACTION_getSlicingConfig:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getSlicingConfig(_arg0);
          break;
        }
        case TRANSACTION_releasePduSessionId:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.releasePduSessionId(_arg0, _arg1);
          break;
        }
        case TRANSACTION_responseAcknowledgement:
        {
          this.responseAcknowledgement();
          break;
        }
        case TRANSACTION_setDataAllowed:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setDataAllowed(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setDataProfile:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.data.DataProfileInfo[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.radio.data.DataProfileInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setDataProfile(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setDataThrottling:
        {
          int _arg0;
          _arg0 = data.readInt();
          byte _arg1;
          _arg1 = data.readByte();
          long _arg2;
          _arg2 = data.readLong();
          data.enforceNoDataAvail();
          this.setDataThrottling(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_setInitialAttachApn:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.data.DataProfileInfo _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.data.DataProfileInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setInitialAttachApn(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setResponseFunctions:
        {
          android.hardware.radio.data.IRadioDataResponse _arg0;
          _arg0 = android.hardware.radio.data.IRadioDataResponse.Stub.asInterface(data.readStrongBinder());
          android.hardware.radio.data.IRadioDataIndication _arg1;
          _arg1 = android.hardware.radio.data.IRadioDataIndication.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.setResponseFunctions(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setupDataCall:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          android.hardware.radio.data.DataProfileInfo _arg2;
          _arg2 = data.readTypedObject(android.hardware.radio.data.DataProfileInfo.CREATOR);
          boolean _arg3;
          _arg3 = data.readBoolean();
          int _arg4;
          _arg4 = data.readInt();
          android.hardware.radio.data.LinkAddress[] _arg5;
          _arg5 = data.createTypedArray(android.hardware.radio.data.LinkAddress.CREATOR);
          java.lang.String[] _arg6;
          _arg6 = data.createStringArray();
          int _arg7;
          _arg7 = data.readInt();
          android.hardware.radio.data.SliceInfo _arg8;
          _arg8 = data.readTypedObject(android.hardware.radio.data.SliceInfo.CREATOR);
          boolean _arg9;
          _arg9 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setupDataCall(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7, _arg8, _arg9);
          break;
        }
        case TRANSACTION_startHandover:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.startHandover(_arg0, _arg1);
          break;
        }
        case TRANSACTION_startKeepalive:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.data.KeepaliveRequest _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.data.KeepaliveRequest.CREATOR);
          data.enforceNoDataAvail();
          this.startKeepalive(_arg0, _arg1);
          break;
        }
        case TRANSACTION_stopKeepalive:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.stopKeepalive(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.radio.data.IRadioData
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
      @Override public void allocatePduSessionId(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_allocatePduSessionId, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method allocatePduSessionId is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void cancelHandover(int serial, int callId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(callId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_cancelHandover, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method cancelHandover is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void deactivateDataCall(int serial, int cid, int reason) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(cid);
          _data.writeInt(reason);
          boolean _status = mRemote.transact(Stub.TRANSACTION_deactivateDataCall, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method deactivateDataCall is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getDataCallList(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getDataCallList, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getDataCallList is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getSlicingConfig(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSlicingConfig, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getSlicingConfig is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void releasePduSessionId(int serial, int id) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(id);
          boolean _status = mRemote.transact(Stub.TRANSACTION_releasePduSessionId, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method releasePduSessionId is unimplemented.");
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
      @Override public void setDataAllowed(int serial, boolean allow) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeBoolean(allow);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setDataAllowed, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setDataAllowed is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setDataProfile(int serial, android.hardware.radio.data.DataProfileInfo[] profiles) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedArray(profiles, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setDataProfile, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setDataProfile is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setDataThrottling(int serial, byte dataThrottlingAction, long completionDurationMillis) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeByte(dataThrottlingAction);
          _data.writeLong(completionDurationMillis);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setDataThrottling, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setDataThrottling is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setInitialAttachApn(int serial, android.hardware.radio.data.DataProfileInfo dataProfileInfo) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedObject(dataProfileInfo, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setInitialAttachApn, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setInitialAttachApn is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setResponseFunctions(android.hardware.radio.data.IRadioDataResponse radioDataResponse, android.hardware.radio.data.IRadioDataIndication radioDataIndication) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(radioDataResponse);
          _data.writeStrongInterface(radioDataIndication);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setResponseFunctions, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setResponseFunctions is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setupDataCall(int serial, int accessNetwork, android.hardware.radio.data.DataProfileInfo dataProfileInfo, boolean roamingAllowed, int reason, android.hardware.radio.data.LinkAddress[] addresses, java.lang.String[] dnses, int pduSessionId, android.hardware.radio.data.SliceInfo sliceInfo, boolean matchAllRuleAllowed) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(accessNetwork);
          _data.writeTypedObject(dataProfileInfo, 0);
          _data.writeBoolean(roamingAllowed);
          _data.writeInt(reason);
          _data.writeTypedArray(addresses, 0);
          _data.writeStringArray(dnses);
          _data.writeInt(pduSessionId);
          _data.writeTypedObject(sliceInfo, 0);
          _data.writeBoolean(matchAllRuleAllowed);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setupDataCall, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setupDataCall is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void startHandover(int serial, int callId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(callId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startHandover, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method startHandover is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void startKeepalive(int serial, android.hardware.radio.data.KeepaliveRequest keepalive) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedObject(keepalive, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startKeepalive, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method startKeepalive is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void stopKeepalive(int serial, int sessionHandle) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(sessionHandle);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stopKeepalive, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method stopKeepalive is unimplemented.");
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
    static final int TRANSACTION_allocatePduSessionId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_cancelHandover = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_deactivateDataCall = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_getDataCallList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getSlicingConfig = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_releasePduSessionId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_responseAcknowledgement = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_setDataAllowed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_setDataProfile = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_setDataThrottling = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_setInitialAttachApn = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_setResponseFunctions = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_setupDataCall = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_startHandover = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_startKeepalive = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_stopKeepalive = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$radio$data$IRadioData".replace('$', '.');
  public void allocatePduSessionId(int serial) throws android.os.RemoteException;
  public void cancelHandover(int serial, int callId) throws android.os.RemoteException;
  public void deactivateDataCall(int serial, int cid, int reason) throws android.os.RemoteException;
  public void getDataCallList(int serial) throws android.os.RemoteException;
  public void getSlicingConfig(int serial) throws android.os.RemoteException;
  public void releasePduSessionId(int serial, int id) throws android.os.RemoteException;
  public void responseAcknowledgement() throws android.os.RemoteException;
  public void setDataAllowed(int serial, boolean allow) throws android.os.RemoteException;
  public void setDataProfile(int serial, android.hardware.radio.data.DataProfileInfo[] profiles) throws android.os.RemoteException;
  public void setDataThrottling(int serial, byte dataThrottlingAction, long completionDurationMillis) throws android.os.RemoteException;
  public void setInitialAttachApn(int serial, android.hardware.radio.data.DataProfileInfo dataProfileInfo) throws android.os.RemoteException;
  public void setResponseFunctions(android.hardware.radio.data.IRadioDataResponse radioDataResponse, android.hardware.radio.data.IRadioDataIndication radioDataIndication) throws android.os.RemoteException;
  public void setupDataCall(int serial, int accessNetwork, android.hardware.radio.data.DataProfileInfo dataProfileInfo, boolean roamingAllowed, int reason, android.hardware.radio.data.LinkAddress[] addresses, java.lang.String[] dnses, int pduSessionId, android.hardware.radio.data.SliceInfo sliceInfo, boolean matchAllRuleAllowed) throws android.os.RemoteException;
  public void startHandover(int serial, int callId) throws android.os.RemoteException;
  public void startKeepalive(int serial, android.hardware.radio.data.KeepaliveRequest keepalive) throws android.os.RemoteException;
  public void stopKeepalive(int serial, int sessionHandle) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
