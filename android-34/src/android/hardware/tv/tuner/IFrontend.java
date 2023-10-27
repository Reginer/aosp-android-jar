/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public interface IFrontend extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "f8d74c149f04e76b6d622db2bd8e465dae24b08c";
  /** Default implementation for IFrontend. */
  public static class Default implements android.hardware.tv.tuner.IFrontend
  {
    @Override public void setCallback(android.hardware.tv.tuner.IFrontendCallback callback) throws android.os.RemoteException
    {
    }
    @Override public void tune(android.hardware.tv.tuner.FrontendSettings settings) throws android.os.RemoteException
    {
    }
    @Override public void stopTune() throws android.os.RemoteException
    {
    }
    @Override public void close() throws android.os.RemoteException
    {
    }
    @Override public void scan(android.hardware.tv.tuner.FrontendSettings settings, int type) throws android.os.RemoteException
    {
    }
    @Override public void stopScan() throws android.os.RemoteException
    {
    }
    @Override public android.hardware.tv.tuner.FrontendStatus[] getStatus(int[] statusTypes) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void setLnb(int lnbId) throws android.os.RemoteException
    {
    }
    @Override public int linkCiCam(int ciCamId) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void unlinkCiCam(int ciCamId) throws android.os.RemoteException
    {
    }
    @Override public java.lang.String getHardwareInfo() throws android.os.RemoteException
    {
      return null;
    }
    @Override public void removeOutputPid(int pid) throws android.os.RemoteException
    {
    }
    @Override public int[] getFrontendStatusReadiness(int[] statusTypes) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.tv.tuner.IFrontend
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.tv.tuner.IFrontend interface,
     * generating a proxy if needed.
     */
    public static android.hardware.tv.tuner.IFrontend asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.tv.tuner.IFrontend))) {
        return ((android.hardware.tv.tuner.IFrontend)iin);
      }
      return new android.hardware.tv.tuner.IFrontend.Stub.Proxy(obj);
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
        case TRANSACTION_setCallback:
        {
          android.hardware.tv.tuner.IFrontendCallback _arg0;
          _arg0 = android.hardware.tv.tuner.IFrontendCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.setCallback(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_tune:
        {
          android.hardware.tv.tuner.FrontendSettings _arg0;
          _arg0 = data.readTypedObject(android.hardware.tv.tuner.FrontendSettings.CREATOR);
          data.enforceNoDataAvail();
          this.tune(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_stopTune:
        {
          this.stopTune();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_close:
        {
          this.close();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_scan:
        {
          android.hardware.tv.tuner.FrontendSettings _arg0;
          _arg0 = data.readTypedObject(android.hardware.tv.tuner.FrontendSettings.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.scan(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_stopScan:
        {
          this.stopScan();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getStatus:
        {
          int[] _arg0;
          _arg0 = data.createIntArray();
          data.enforceNoDataAvail();
          android.hardware.tv.tuner.FrontendStatus[] _result = this.getStatus(_arg0);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_setLnb:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.setLnb(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_linkCiCam:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          int _result = this.linkCiCam(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_unlinkCiCam:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.unlinkCiCam(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getHardwareInfo:
        {
          java.lang.String _result = this.getHardwareInfo();
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        case TRANSACTION_removeOutputPid:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.removeOutputPid(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getFrontendStatusReadiness:
        {
          int[] _arg0;
          _arg0 = data.createIntArray();
          data.enforceNoDataAvail();
          int[] _result = this.getFrontendStatusReadiness(_arg0);
          reply.writeNoException();
          reply.writeIntArray(_result);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.tv.tuner.IFrontend
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
      @Override public void setCallback(android.hardware.tv.tuner.IFrontendCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCallback, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setCallback is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void tune(android.hardware.tv.tuner.FrontendSettings settings) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(settings, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_tune, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method tune is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void stopTune() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stopTune, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method stopTune is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void close() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_close, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method close is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void scan(android.hardware.tv.tuner.FrontendSettings settings, int type) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(settings, 0);
          _data.writeInt(type);
          boolean _status = mRemote.transact(Stub.TRANSACTION_scan, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method scan is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void stopScan() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stopScan, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method stopScan is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public android.hardware.tv.tuner.FrontendStatus[] getStatus(int[] statusTypes) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.tv.tuner.FrontendStatus[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeIntArray(statusTypes);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getStatus, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getStatus is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createTypedArray(android.hardware.tv.tuner.FrontendStatus.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void setLnb(int lnbId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(lnbId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setLnb, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setLnb is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public int linkCiCam(int ciCamId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(ciCamId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_linkCiCam, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method linkCiCam is unimplemented.");
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
      @Override public void unlinkCiCam(int ciCamId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(ciCamId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unlinkCiCam, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method unlinkCiCam is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public java.lang.String getHardwareInfo() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getHardwareInfo, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getHardwareInfo is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void removeOutputPid(int pid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(pid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_removeOutputPid, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method removeOutputPid is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public int[] getFrontendStatusReadiness(int[] statusTypes) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeIntArray(statusTypes);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getFrontendStatusReadiness, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getFrontendStatusReadiness is unimplemented.");
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
    static final int TRANSACTION_setCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_tune = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_stopTune = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_close = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_scan = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_stopScan = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_setLnb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_linkCiCam = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_unlinkCiCam = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_getHardwareInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_removeOutputPid = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_getFrontendStatusReadiness = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$tv$tuner$IFrontend".replace('$', '.');
  public void setCallback(android.hardware.tv.tuner.IFrontendCallback callback) throws android.os.RemoteException;
  public void tune(android.hardware.tv.tuner.FrontendSettings settings) throws android.os.RemoteException;
  public void stopTune() throws android.os.RemoteException;
  public void close() throws android.os.RemoteException;
  public void scan(android.hardware.tv.tuner.FrontendSettings settings, int type) throws android.os.RemoteException;
  public void stopScan() throws android.os.RemoteException;
  public android.hardware.tv.tuner.FrontendStatus[] getStatus(int[] statusTypes) throws android.os.RemoteException;
  public void setLnb(int lnbId) throws android.os.RemoteException;
  public int linkCiCam(int ciCamId) throws android.os.RemoteException;
  public void unlinkCiCam(int ciCamId) throws android.os.RemoteException;
  public java.lang.String getHardwareInfo() throws android.os.RemoteException;
  public void removeOutputPid(int pid) throws android.os.RemoteException;
  public int[] getFrontendStatusReadiness(int[] statusTypes) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
