/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 6 --hash 13171cf98a48de298baf85167633376ea3db4ea0 -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/common/fmq/aidl/android.hardware.common.fmq_interface/1/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/power/aidl/android.hardware.power-V6-java-source/gen/android/hardware/power/IPower.java.d -o out/soong/.intermediates/hardware/interfaces/power/aidl/android.hardware.power-V6-java-source/gen -Nhardware/interfaces/power/aidl/aidl_api/android.hardware.power/6 hardware/interfaces/power/aidl/aidl_api/android.hardware.power/6/android/hardware/power/IPower.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.power;
public interface IPower extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 6;
  public static final String HASH = "13171cf98a48de298baf85167633376ea3db4ea0";
  /** Default implementation for IPower. */
  public static class Default implements android.hardware.power.IPower
  {
    @Override public void setMode(int type, boolean enabled) throws android.os.RemoteException
    {
    }
    @Override public boolean isModeSupported(int type) throws android.os.RemoteException
    {
      return false;
    }
    @Override public void setBoost(int type, int durationMs) throws android.os.RemoteException
    {
    }
    @Override public boolean isBoostSupported(int type) throws android.os.RemoteException
    {
      return false;
    }
    @Override public android.hardware.power.IPowerHintSession createHintSession(int tgid, int uid, int[] threadIds, long durationNanos) throws android.os.RemoteException
    {
      return null;
    }
    @Override public long getHintSessionPreferredRate() throws android.os.RemoteException
    {
      return 0L;
    }
    @Override public android.hardware.power.IPowerHintSession createHintSessionWithConfig(int tgid, int uid, int[] threadIds, long durationNanos, int tag, android.hardware.power.SessionConfig config) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.power.ChannelConfig getSessionChannel(int tgid, int uid) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void closeSessionChannel(int tgid, int uid) throws android.os.RemoteException
    {
    }
    @Override public android.hardware.power.SupportInfo getSupportInfo() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.power.CpuHeadroomResult getCpuHeadroom(android.hardware.power.CpuHeadroomParams params) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.power.GpuHeadroomResult getGpuHeadroom(android.hardware.power.GpuHeadroomParams params) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void sendCompositionData(android.hardware.power.CompositionData[] data) throws android.os.RemoteException
    {
    }
    @Override public void sendCompositionUpdate(android.hardware.power.CompositionUpdate update) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.power.IPower
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.power.IPower interface,
     * generating a proxy if needed.
     */
    public static android.hardware.power.IPower asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.power.IPower))) {
        return ((android.hardware.power.IPower)iin);
      }
      return new android.hardware.power.IPower.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    /** @hide */
    public static java.lang.String getDefaultTransactionName(int transactionCode)
    {
      switch (transactionCode)
      {
        case TRANSACTION_setMode:
        {
          return "setMode";
        }
        case TRANSACTION_isModeSupported:
        {
          return "isModeSupported";
        }
        case TRANSACTION_setBoost:
        {
          return "setBoost";
        }
        case TRANSACTION_isBoostSupported:
        {
          return "isBoostSupported";
        }
        case TRANSACTION_createHintSession:
        {
          return "createHintSession";
        }
        case TRANSACTION_getHintSessionPreferredRate:
        {
          return "getHintSessionPreferredRate";
        }
        case TRANSACTION_createHintSessionWithConfig:
        {
          return "createHintSessionWithConfig";
        }
        case TRANSACTION_getSessionChannel:
        {
          return "getSessionChannel";
        }
        case TRANSACTION_closeSessionChannel:
        {
          return "closeSessionChannel";
        }
        case TRANSACTION_getSupportInfo:
        {
          return "getSupportInfo";
        }
        case TRANSACTION_getCpuHeadroom:
        {
          return "getCpuHeadroom";
        }
        case TRANSACTION_getGpuHeadroom:
        {
          return "getGpuHeadroom";
        }
        case TRANSACTION_sendCompositionData:
        {
          return "sendCompositionData";
        }
        case TRANSACTION_sendCompositionUpdate:
        {
          return "sendCompositionUpdate";
        }
        case TRANSACTION_getInterfaceVersion:
        {
          return "getInterfaceVersion";
        }
        case TRANSACTION_getInterfaceHash:
        {
          return "getInterfaceHash";
        }
        default:
        {
          return null;
        }
      }
    }
    /** @hide */
    public java.lang.String getTransactionName(int transactionCode)
    {
      return this.getDefaultTransactionName(transactionCode);
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      if (code == INTERFACE_TRANSACTION) {
        reply.writeString(descriptor);
        return true;
      }
      else if (code == TRANSACTION_getInterfaceVersion) {
        reply.writeNoException();
        reply.writeInt(getInterfaceVersion());
        return true;
      }
      else if (code == TRANSACTION_getInterfaceHash) {
        reply.writeNoException();
        reply.writeString(getInterfaceHash());
        return true;
      }
      switch (code)
      {
        case TRANSACTION_setMode:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setMode(_arg0, _arg1);
          break;
        }
        case TRANSACTION_isModeSupported:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          boolean _result = this.isModeSupported(_arg0);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_setBoost:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.setBoost(_arg0, _arg1);
          break;
        }
        case TRANSACTION_isBoostSupported:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          boolean _result = this.isBoostSupported(_arg0);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_createHintSession:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          int[] _arg2;
          _arg2 = data.createIntArray();
          long _arg3;
          _arg3 = data.readLong();
          data.enforceNoDataAvail();
          android.hardware.power.IPowerHintSession _result = this.createHintSession(_arg0, _arg1, _arg2, _arg3);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_getHintSessionPreferredRate:
        {
          long _result = this.getHintSessionPreferredRate();
          reply.writeNoException();
          reply.writeLong(_result);
          break;
        }
        case TRANSACTION_createHintSessionWithConfig:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          int[] _arg2;
          _arg2 = data.createIntArray();
          long _arg3;
          _arg3 = data.readLong();
          int _arg4;
          _arg4 = data.readInt();
          android.hardware.power.SessionConfig _arg5;
          _arg5 = new android.hardware.power.SessionConfig();
          data.enforceNoDataAvail();
          android.hardware.power.IPowerHintSession _result = this.createHintSessionWithConfig(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          reply.writeTypedObject(_arg5, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getSessionChannel:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          android.hardware.power.ChannelConfig _result = this.getSessionChannel(_arg0, _arg1);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_closeSessionChannel:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.closeSessionChannel(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getSupportInfo:
        {
          android.hardware.power.SupportInfo _result = this.getSupportInfo();
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getCpuHeadroom:
        {
          android.hardware.power.CpuHeadroomParams _arg0;
          _arg0 = data.readTypedObject(android.hardware.power.CpuHeadroomParams.CREATOR);
          data.enforceNoDataAvail();
          android.hardware.power.CpuHeadroomResult _result = this.getCpuHeadroom(_arg0);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getGpuHeadroom:
        {
          android.hardware.power.GpuHeadroomParams _arg0;
          _arg0 = data.readTypedObject(android.hardware.power.GpuHeadroomParams.CREATOR);
          data.enforceNoDataAvail();
          android.hardware.power.GpuHeadroomResult _result = this.getGpuHeadroom(_arg0);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_sendCompositionData:
        {
          android.hardware.power.CompositionData[] _arg0;
          _arg0 = data.createTypedArray(android.hardware.power.CompositionData.CREATOR);
          data.enforceNoDataAvail();
          this.sendCompositionData(_arg0);
          break;
        }
        case TRANSACTION_sendCompositionUpdate:
        {
          android.hardware.power.CompositionUpdate _arg0;
          _arg0 = data.readTypedObject(android.hardware.power.CompositionUpdate.CREATOR);
          data.enforceNoDataAvail();
          this.sendCompositionUpdate(_arg0);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.power.IPower
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
      @Override public void setMode(int type, boolean enabled) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeBoolean(enabled);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setMode, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setMode is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public boolean isModeSupported(int type) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isModeSupported, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method isModeSupported is unimplemented.");
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
      @Override public void setBoost(int type, int durationMs) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeInt(durationMs);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setBoost, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setBoost is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public boolean isBoostSupported(int type) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isBoostSupported, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method isBoostSupported is unimplemented.");
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
      @Override public android.hardware.power.IPowerHintSession createHintSession(int tgid, int uid, int[] threadIds, long durationNanos) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.power.IPowerHintSession _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(tgid);
          _data.writeInt(uid);
          _data.writeIntArray(threadIds);
          _data.writeLong(durationNanos);
          boolean _status = mRemote.transact(Stub.TRANSACTION_createHintSession, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method createHintSession is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.power.IPowerHintSession.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public long getHintSessionPreferredRate() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        long _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getHintSessionPreferredRate, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getHintSessionPreferredRate is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readLong();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.power.IPowerHintSession createHintSessionWithConfig(int tgid, int uid, int[] threadIds, long durationNanos, int tag, android.hardware.power.SessionConfig config) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.power.IPowerHintSession _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(tgid);
          _data.writeInt(uid);
          _data.writeIntArray(threadIds);
          _data.writeLong(durationNanos);
          _data.writeInt(tag);
          boolean _status = mRemote.transact(Stub.TRANSACTION_createHintSessionWithConfig, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method createHintSessionWithConfig is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.power.IPowerHintSession.Stub.asInterface(_reply.readStrongBinder());
          if ((0!=_reply.readInt())) {
            config.readFromParcel(_reply);
          }
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.power.ChannelConfig getSessionChannel(int tgid, int uid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.power.ChannelConfig _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(tgid);
          _data.writeInt(uid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSessionChannel, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getSessionChannel is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.hardware.power.ChannelConfig.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void closeSessionChannel(int tgid, int uid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(tgid);
          _data.writeInt(uid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_closeSessionChannel, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method closeSessionChannel is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public android.hardware.power.SupportInfo getSupportInfo() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.power.SupportInfo _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSupportInfo, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getSupportInfo is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.hardware.power.SupportInfo.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.power.CpuHeadroomResult getCpuHeadroom(android.hardware.power.CpuHeadroomParams params) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.power.CpuHeadroomResult _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(params, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCpuHeadroom, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getCpuHeadroom is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.hardware.power.CpuHeadroomResult.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.power.GpuHeadroomResult getGpuHeadroom(android.hardware.power.GpuHeadroomParams params) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.power.GpuHeadroomResult _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(params, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getGpuHeadroom, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getGpuHeadroom is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.hardware.power.GpuHeadroomResult.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void sendCompositionData(android.hardware.power.CompositionData[] data) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedArray(data, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendCompositionData, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendCompositionData is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendCompositionUpdate(android.hardware.power.CompositionUpdate update) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(update, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendCompositionUpdate, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendCompositionUpdate is unimplemented.");
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
    static final int TRANSACTION_setMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_isModeSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_setBoost = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_isBoostSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_createHintSession = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getHintSessionPreferredRate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_createHintSessionWithConfig = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getSessionChannel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_closeSessionChannel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_getSupportInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_getCpuHeadroom = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_getGpuHeadroom = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_sendCompositionData = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_sendCompositionUpdate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android$hardware$power$IPower".replace('$', '.');
  public void setMode(int type, boolean enabled) throws android.os.RemoteException;
  public boolean isModeSupported(int type) throws android.os.RemoteException;
  public void setBoost(int type, int durationMs) throws android.os.RemoteException;
  public boolean isBoostSupported(int type) throws android.os.RemoteException;
  public android.hardware.power.IPowerHintSession createHintSession(int tgid, int uid, int[] threadIds, long durationNanos) throws android.os.RemoteException;
  public long getHintSessionPreferredRate() throws android.os.RemoteException;
  public android.hardware.power.IPowerHintSession createHintSessionWithConfig(int tgid, int uid, int[] threadIds, long durationNanos, int tag, android.hardware.power.SessionConfig config) throws android.os.RemoteException;
  public android.hardware.power.ChannelConfig getSessionChannel(int tgid, int uid) throws android.os.RemoteException;
  public void closeSessionChannel(int tgid, int uid) throws android.os.RemoteException;
  public android.hardware.power.SupportInfo getSupportInfo() throws android.os.RemoteException;
  public android.hardware.power.CpuHeadroomResult getCpuHeadroom(android.hardware.power.CpuHeadroomParams params) throws android.os.RemoteException;
  public android.hardware.power.GpuHeadroomResult getGpuHeadroom(android.hardware.power.GpuHeadroomParams params) throws android.os.RemoteException;
  public void sendCompositionData(android.hardware.power.CompositionData[] data) throws android.os.RemoteException;
  public void sendCompositionUpdate(android.hardware.power.CompositionUpdate update) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
