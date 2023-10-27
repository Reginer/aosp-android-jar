/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.data;
public interface IRadioDataResponse extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "cb458326b02e0e87143f24118543e8cc7d6a9e8e";
  /** Default implementation for IRadioDataResponse. */
  public static class Default implements android.hardware.radio.data.IRadioDataResponse
  {
    @Override public void acknowledgeRequest(int serial) throws android.os.RemoteException
    {
    }
    @Override public void allocatePduSessionIdResponse(android.hardware.radio.RadioResponseInfo info, int id) throws android.os.RemoteException
    {
    }
    @Override public void cancelHandoverResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void deactivateDataCallResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void getDataCallListResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.data.SetupDataCallResult[] dcResponse) throws android.os.RemoteException
    {
    }
    @Override public void getSlicingConfigResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.data.SlicingConfig slicingConfig) throws android.os.RemoteException
    {
    }
    @Override public void releasePduSessionIdResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setDataAllowedResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setDataProfileResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setDataThrottlingResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setInitialAttachApnResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void setupDataCallResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.data.SetupDataCallResult dcResponse) throws android.os.RemoteException
    {
    }
    @Override public void startHandoverResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
    {
    }
    @Override public void startKeepaliveResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.data.KeepaliveStatus status) throws android.os.RemoteException
    {
    }
    @Override public void stopKeepaliveResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.radio.data.IRadioDataResponse
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.radio.data.IRadioDataResponse interface,
     * generating a proxy if needed.
     */
    public static android.hardware.radio.data.IRadioDataResponse asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.radio.data.IRadioDataResponse))) {
        return ((android.hardware.radio.data.IRadioDataResponse)iin);
      }
      return new android.hardware.radio.data.IRadioDataResponse.Stub.Proxy(obj);
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
        case TRANSACTION_allocatePduSessionIdResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.allocatePduSessionIdResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_cancelHandoverResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.cancelHandoverResponse(_arg0);
          break;
        }
        case TRANSACTION_deactivateDataCallResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.deactivateDataCallResponse(_arg0);
          break;
        }
        case TRANSACTION_getDataCallListResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.data.SetupDataCallResult[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.radio.data.SetupDataCallResult.CREATOR);
          data.enforceNoDataAvail();
          this.getDataCallListResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getSlicingConfigResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.data.SlicingConfig _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.data.SlicingConfig.CREATOR);
          data.enforceNoDataAvail();
          this.getSlicingConfigResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_releasePduSessionIdResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.releasePduSessionIdResponse(_arg0);
          break;
        }
        case TRANSACTION_setDataAllowedResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setDataAllowedResponse(_arg0);
          break;
        }
        case TRANSACTION_setDataProfileResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setDataProfileResponse(_arg0);
          break;
        }
        case TRANSACTION_setDataThrottlingResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setDataThrottlingResponse(_arg0);
          break;
        }
        case TRANSACTION_setInitialAttachApnResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setInitialAttachApnResponse(_arg0);
          break;
        }
        case TRANSACTION_setupDataCallResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.data.SetupDataCallResult _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.data.SetupDataCallResult.CREATOR);
          data.enforceNoDataAvail();
          this.setupDataCallResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_startHandoverResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.startHandoverResponse(_arg0);
          break;
        }
        case TRANSACTION_startKeepaliveResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          android.hardware.radio.data.KeepaliveStatus _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.data.KeepaliveStatus.CREATOR);
          data.enforceNoDataAvail();
          this.startKeepaliveResponse(_arg0, _arg1);
          break;
        }
        case TRANSACTION_stopKeepaliveResponse:
        {
          android.hardware.radio.RadioResponseInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.radio.RadioResponseInfo.CREATOR);
          data.enforceNoDataAvail();
          this.stopKeepaliveResponse(_arg0);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.radio.data.IRadioDataResponse
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
      @Override public void allocatePduSessionIdResponse(android.hardware.radio.RadioResponseInfo info, int id) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeInt(id);
          boolean _status = mRemote.transact(Stub.TRANSACTION_allocatePduSessionIdResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method allocatePduSessionIdResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void cancelHandoverResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_cancelHandoverResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method cancelHandoverResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void deactivateDataCallResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_deactivateDataCallResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method deactivateDataCallResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getDataCallListResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.data.SetupDataCallResult[] dcResponse) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedArray(dcResponse, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getDataCallListResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getDataCallListResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getSlicingConfigResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.data.SlicingConfig slicingConfig) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(slicingConfig, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSlicingConfigResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getSlicingConfigResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void releasePduSessionIdResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_releasePduSessionIdResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method releasePduSessionIdResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setDataAllowedResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setDataAllowedResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setDataAllowedResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setDataProfileResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setDataProfileResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setDataProfileResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setDataThrottlingResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setDataThrottlingResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setDataThrottlingResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setInitialAttachApnResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setInitialAttachApnResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setInitialAttachApnResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setupDataCallResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.data.SetupDataCallResult dcResponse) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(dcResponse, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setupDataCallResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setupDataCallResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void startHandoverResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startHandoverResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method startHandoverResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void startKeepaliveResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.data.KeepaliveStatus status) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          _data.writeTypedObject(status, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startKeepaliveResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method startKeepaliveResponse is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void stopKeepaliveResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stopKeepaliveResponse, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method stopKeepaliveResponse is unimplemented.");
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
    static final int TRANSACTION_allocatePduSessionIdResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_cancelHandoverResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_deactivateDataCallResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getDataCallListResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getSlicingConfigResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_releasePduSessionIdResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_setDataAllowedResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_setDataProfileResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_setDataThrottlingResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_setInitialAttachApnResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_setupDataCallResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_startHandoverResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_startKeepaliveResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_stopKeepaliveResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$radio$data$IRadioDataResponse".replace('$', '.');
  public void acknowledgeRequest(int serial) throws android.os.RemoteException;
  public void allocatePduSessionIdResponse(android.hardware.radio.RadioResponseInfo info, int id) throws android.os.RemoteException;
  public void cancelHandoverResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void deactivateDataCallResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void getDataCallListResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.data.SetupDataCallResult[] dcResponse) throws android.os.RemoteException;
  public void getSlicingConfigResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.data.SlicingConfig slicingConfig) throws android.os.RemoteException;
  public void releasePduSessionIdResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setDataAllowedResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setDataProfileResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setDataThrottlingResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setInitialAttachApnResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void setupDataCallResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.data.SetupDataCallResult dcResponse) throws android.os.RemoteException;
  public void startHandoverResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public void startKeepaliveResponse(android.hardware.radio.RadioResponseInfo info, android.hardware.radio.data.KeepaliveStatus status) throws android.os.RemoteException;
  public void stopKeepaliveResponse(android.hardware.radio.RadioResponseInfo info) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
