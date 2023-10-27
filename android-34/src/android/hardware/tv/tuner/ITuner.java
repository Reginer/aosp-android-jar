/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public interface ITuner extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "f8d74c149f04e76b6d622db2bd8e465dae24b08c";
  /** Default implementation for ITuner. */
  public static class Default implements android.hardware.tv.tuner.ITuner
  {
    @Override public int[] getFrontendIds() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.tv.tuner.IFrontend openFrontendById(int frontendId) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.tv.tuner.IDemux openDemux(int[] demuxId) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.tv.tuner.DemuxCapabilities getDemuxCaps() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.tv.tuner.IDescrambler openDescrambler() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.tv.tuner.FrontendInfo getFrontendInfo(int frontendId) throws android.os.RemoteException
    {
      return null;
    }
    @Override public int[] getLnbIds() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.tv.tuner.ILnb openLnbById(int lnbId) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.tv.tuner.ILnb openLnbByName(java.lang.String lnbName, int[] lnbId) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void setLna(boolean bEnable) throws android.os.RemoteException
    {
    }
    @Override public void setMaxNumberOfFrontends(int frontendType, int maxNumber) throws android.os.RemoteException
    {
    }
    @Override public int getMaxNumberOfFrontends(int frontendType) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public boolean isLnaSupported() throws android.os.RemoteException
    {
      return false;
    }
    @Override public int[] getDemuxIds() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.tv.tuner.IDemux openDemuxById(int demuxId) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.tv.tuner.DemuxInfo getDemuxInfo(int demuxId) throws android.os.RemoteException
    {
      return null;
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.tv.tuner.ITuner
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.tv.tuner.ITuner interface,
     * generating a proxy if needed.
     */
    public static android.hardware.tv.tuner.ITuner asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.tv.tuner.ITuner))) {
        return ((android.hardware.tv.tuner.ITuner)iin);
      }
      return new android.hardware.tv.tuner.ITuner.Stub.Proxy(obj);
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
        case TRANSACTION_getFrontendIds:
        {
          int[] _result = this.getFrontendIds();
          reply.writeNoException();
          reply.writeIntArray(_result);
          break;
        }
        case TRANSACTION_openFrontendById:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          android.hardware.tv.tuner.IFrontend _result = this.openFrontendById(_arg0);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_openDemux:
        {
          int[] _arg0;
          int _arg0_length = data.readInt();
          if (_arg0_length < 0) {
            _arg0 = null;
          } else {
            _arg0 = new int[_arg0_length];
          }
          data.enforceNoDataAvail();
          android.hardware.tv.tuner.IDemux _result = this.openDemux(_arg0);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          reply.writeIntArray(_arg0);
          break;
        }
        case TRANSACTION_getDemuxCaps:
        {
          android.hardware.tv.tuner.DemuxCapabilities _result = this.getDemuxCaps();
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_openDescrambler:
        {
          android.hardware.tv.tuner.IDescrambler _result = this.openDescrambler();
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_getFrontendInfo:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          android.hardware.tv.tuner.FrontendInfo _result = this.getFrontendInfo(_arg0);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getLnbIds:
        {
          int[] _result = this.getLnbIds();
          reply.writeNoException();
          reply.writeIntArray(_result);
          break;
        }
        case TRANSACTION_openLnbById:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          android.hardware.tv.tuner.ILnb _result = this.openLnbById(_arg0);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_openLnbByName:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          int[] _arg1;
          int _arg1_length = data.readInt();
          if (_arg1_length < 0) {
            _arg1 = null;
          } else {
            _arg1 = new int[_arg1_length];
          }
          data.enforceNoDataAvail();
          android.hardware.tv.tuner.ILnb _result = this.openLnbByName(_arg0, _arg1);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          reply.writeIntArray(_arg1);
          break;
        }
        case TRANSACTION_setLna:
        {
          boolean _arg0;
          _arg0 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setLna(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setMaxNumberOfFrontends:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.setMaxNumberOfFrontends(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getMaxNumberOfFrontends:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          int _result = this.getMaxNumberOfFrontends(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_isLnaSupported:
        {
          boolean _result = this.isLnaSupported();
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_getDemuxIds:
        {
          int[] _result = this.getDemuxIds();
          reply.writeNoException();
          reply.writeIntArray(_result);
          break;
        }
        case TRANSACTION_openDemuxById:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          android.hardware.tv.tuner.IDemux _result = this.openDemuxById(_arg0);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_getDemuxInfo:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          android.hardware.tv.tuner.DemuxInfo _result = this.getDemuxInfo(_arg0);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.tv.tuner.ITuner
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
      @Override public int[] getFrontendIds() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getFrontendIds, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getFrontendIds is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createIntArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.tv.tuner.IFrontend openFrontendById(int frontendId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.tv.tuner.IFrontend _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(frontendId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_openFrontendById, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method openFrontendById is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.tv.tuner.IFrontend.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.tv.tuner.IDemux openDemux(int[] demuxId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.tv.tuner.IDemux _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(demuxId.length);
          boolean _status = mRemote.transact(Stub.TRANSACTION_openDemux, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method openDemux is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.tv.tuner.IDemux.Stub.asInterface(_reply.readStrongBinder());
          _reply.readIntArray(demuxId);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.tv.tuner.DemuxCapabilities getDemuxCaps() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.tv.tuner.DemuxCapabilities _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getDemuxCaps, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getDemuxCaps is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.hardware.tv.tuner.DemuxCapabilities.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.tv.tuner.IDescrambler openDescrambler() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.tv.tuner.IDescrambler _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_openDescrambler, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method openDescrambler is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.tv.tuner.IDescrambler.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.tv.tuner.FrontendInfo getFrontendInfo(int frontendId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.tv.tuner.FrontendInfo _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(frontendId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getFrontendInfo, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getFrontendInfo is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.hardware.tv.tuner.FrontendInfo.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int[] getLnbIds() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getLnbIds, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getLnbIds is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createIntArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.tv.tuner.ILnb openLnbById(int lnbId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.tv.tuner.ILnb _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(lnbId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_openLnbById, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method openLnbById is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.tv.tuner.ILnb.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.tv.tuner.ILnb openLnbByName(java.lang.String lnbName, int[] lnbId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.tv.tuner.ILnb _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(lnbName);
          _data.writeInt(lnbId.length);
          boolean _status = mRemote.transact(Stub.TRANSACTION_openLnbByName, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method openLnbByName is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.tv.tuner.ILnb.Stub.asInterface(_reply.readStrongBinder());
          _reply.readIntArray(lnbId);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void setLna(boolean bEnable) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeBoolean(bEnable);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setLna, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setLna is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setMaxNumberOfFrontends(int frontendType, int maxNumber) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(frontendType);
          _data.writeInt(maxNumber);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setMaxNumberOfFrontends, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setMaxNumberOfFrontends is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public int getMaxNumberOfFrontends(int frontendType) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(frontendType);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getMaxNumberOfFrontends, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getMaxNumberOfFrontends is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean isLnaSupported() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isLnaSupported, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method isLnaSupported is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readBoolean();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int[] getDemuxIds() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getDemuxIds, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getDemuxIds is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createIntArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.tv.tuner.IDemux openDemuxById(int demuxId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.tv.tuner.IDemux _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(demuxId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_openDemuxById, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method openDemuxById is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.tv.tuner.IDemux.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.tv.tuner.DemuxInfo getDemuxInfo(int demuxId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.tv.tuner.DemuxInfo _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(demuxId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getDemuxInfo, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getDemuxInfo is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.hardware.tv.tuner.DemuxInfo.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
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
    static final int TRANSACTION_getFrontendIds = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_openFrontendById = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_openDemux = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_getDemuxCaps = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_openDescrambler = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getFrontendInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getLnbIds = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_openLnbById = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_openLnbByName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_setLna = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_setMaxNumberOfFrontends = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_getMaxNumberOfFrontends = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_isLnaSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_getDemuxIds = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_openDemuxById = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_getDemuxInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$tv$tuner$ITuner".replace('$', '.');
  public int[] getFrontendIds() throws android.os.RemoteException;
  public android.hardware.tv.tuner.IFrontend openFrontendById(int frontendId) throws android.os.RemoteException;
  public android.hardware.tv.tuner.IDemux openDemux(int[] demuxId) throws android.os.RemoteException;
  public android.hardware.tv.tuner.DemuxCapabilities getDemuxCaps() throws android.os.RemoteException;
  public android.hardware.tv.tuner.IDescrambler openDescrambler() throws android.os.RemoteException;
  public android.hardware.tv.tuner.FrontendInfo getFrontendInfo(int frontendId) throws android.os.RemoteException;
  public int[] getLnbIds() throws android.os.RemoteException;
  public android.hardware.tv.tuner.ILnb openLnbById(int lnbId) throws android.os.RemoteException;
  public android.hardware.tv.tuner.ILnb openLnbByName(java.lang.String lnbName, int[] lnbId) throws android.os.RemoteException;
  public void setLna(boolean bEnable) throws android.os.RemoteException;
  public void setMaxNumberOfFrontends(int frontendType, int maxNumber) throws android.os.RemoteException;
  public int getMaxNumberOfFrontends(int frontendType) throws android.os.RemoteException;
  public boolean isLnaSupported() throws android.os.RemoteException;
  public int[] getDemuxIds() throws android.os.RemoteException;
  public android.hardware.tv.tuner.IDemux openDemuxById(int demuxId) throws android.os.RemoteException;
  public android.hardware.tv.tuner.DemuxInfo getDemuxInfo(int demuxId) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
