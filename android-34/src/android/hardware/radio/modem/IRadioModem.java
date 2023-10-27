/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.modem;
public interface IRadioModem extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "09927560afccc75a063944fbbab3af48099261ca";
  /** Default implementation for IRadioModem. */
  public static class Default implements android.hardware.radio.modem.IRadioModem
  {
    @Override public void enableModem(int serial, boolean on) throws android.os.RemoteException
    {
    }
    @Override public void getBasebandVersion(int serial) throws android.os.RemoteException
    {
    }
    /** @deprecated use getImei(int serial) */
    @Override public void getDeviceIdentity(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getHardwareConfig(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getModemActivityInfo(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getModemStackStatus(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getRadioCapability(int serial) throws android.os.RemoteException
    {
    }
    /** @deprecated NV APIs are deprecated starting from Android U. */
    @Override public void nvReadItem(int serial, int itemId) throws android.os.RemoteException
    {
    }
    @Override public void nvResetConfig(int serial, int resetType) throws android.os.RemoteException
    {
    }
    /** @deprecated NV APIs are deprecated starting from Android U. */
    @Override public void nvWriteCdmaPrl(int serial, byte[] prl) throws android.os.RemoteException
    {
    }
    /** @deprecated NV APIs are deprecated starting from Android U. */
    @Override public void nvWriteItem(int serial, android.hardware.radio.modem.NvWriteItem item) throws android.os.RemoteException
    {
    }
    @Override public void requestShutdown(int serial) throws android.os.RemoteException
    {
    }
    @Override public void responseAcknowledgement() throws android.os.RemoteException
    {
    }
    @Override public void sendDeviceState(int serial, int deviceStateType, boolean state) throws android.os.RemoteException
    {
    }
    @Override public void setRadioCapability(int serial, android.hardware.radio.modem.RadioCapability rc) throws android.os.RemoteException
    {
    }
    @Override public void setRadioPower(int serial, boolean powerOn, boolean forEmergencyCall, boolean preferredForEmergencyCall) throws android.os.RemoteException
    {
    }
    @Override public void setResponseFunctions(android.hardware.radio.modem.IRadioModemResponse radioModemResponse, android.hardware.radio.modem.IRadioModemIndication radioModemIndication) throws android.os.RemoteException
    {
    }
    @Override public void getImei(int serial) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.radio.modem.IRadioModem
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.radio.modem.IRadioModem interface,
     * generating a proxy if needed.
     */
    public static android.hardware.radio.modem.IRadioModem asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.radio.modem.IRadioModem))) {
        return ((android.hardware.radio.modem.IRadioModem)iin);
      }
      return new android.hardware.radio.modem.IRadioModem.Stub.Proxy(obj);
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
        case TRANSACTION_enableModem:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.enableModem(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getBasebandVersion:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getBasebandVersion(_arg0);
          break;
        }
        case TRANSACTION_getDeviceIdentity:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getDeviceIdentity(_arg0);
          break;
        }
        case TRANSACTION_getHardwareConfig:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getHardwareConfig(_arg0);
          break;
        }
        case TRANSACTION_getModemActivityInfo:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getModemActivityInfo(_arg0);
          break;
        }
        case TRANSACTION_getModemStackStatus:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getModemStackStatus(_arg0);
          break;
        }
        case TRANSACTION_getRadioCapability:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getRadioCapability(_arg0);
          break;
        }
        case TRANSACTION_nvReadItem:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.nvReadItem(_arg0, _arg1);
          break;
        }
        case TRANSACTION_nvResetConfig:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.nvResetConfig(_arg0, _arg1);
          break;
        }
        case TRANSACTION_nvWriteCdmaPrl:
        {
          int _arg0;
          _arg0 = data.readInt();
          byte[] _arg1;
          _arg1 = data.createByteArray();
          data.enforceNoDataAvail();
          this.nvWriteCdmaPrl(_arg0, _arg1);
          break;
        }
        case TRANSACTION_nvWriteItem:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.modem.NvWriteItem _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.modem.NvWriteItem.CREATOR);
          data.enforceNoDataAvail();
          this.nvWriteItem(_arg0, _arg1);
          break;
        }
        case TRANSACTION_requestShutdown:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.requestShutdown(_arg0);
          break;
        }
        case TRANSACTION_responseAcknowledgement:
        {
          this.responseAcknowledgement();
          break;
        }
        case TRANSACTION_sendDeviceState:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          boolean _arg2;
          _arg2 = data.readBoolean();
          data.enforceNoDataAvail();
          this.sendDeviceState(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_setRadioCapability:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.modem.RadioCapability _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.modem.RadioCapability.CREATOR);
          data.enforceNoDataAvail();
          this.setRadioCapability(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setRadioPower:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          boolean _arg2;
          _arg2 = data.readBoolean();
          boolean _arg3;
          _arg3 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setRadioPower(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_setResponseFunctions:
        {
          android.hardware.radio.modem.IRadioModemResponse _arg0;
          _arg0 = android.hardware.radio.modem.IRadioModemResponse.Stub.asInterface(data.readStrongBinder());
          android.hardware.radio.modem.IRadioModemIndication _arg1;
          _arg1 = android.hardware.radio.modem.IRadioModemIndication.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.setResponseFunctions(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getImei:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getImei(_arg0);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.radio.modem.IRadioModem
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
      @Override public void enableModem(int serial, boolean on) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeBoolean(on);
          boolean _status = mRemote.transact(Stub.TRANSACTION_enableModem, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method enableModem is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getBasebandVersion(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getBasebandVersion, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getBasebandVersion is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      /** @deprecated use getImei(int serial) */
      @Override public void getDeviceIdentity(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getDeviceIdentity, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getDeviceIdentity is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getHardwareConfig(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getHardwareConfig, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getHardwareConfig is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getModemActivityInfo(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getModemActivityInfo, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getModemActivityInfo is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getModemStackStatus(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getModemStackStatus, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getModemStackStatus is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getRadioCapability(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getRadioCapability, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getRadioCapability is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      /** @deprecated NV APIs are deprecated starting from Android U. */
      @Override public void nvReadItem(int serial, int itemId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(itemId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_nvReadItem, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method nvReadItem is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void nvResetConfig(int serial, int resetType) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(resetType);
          boolean _status = mRemote.transact(Stub.TRANSACTION_nvResetConfig, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method nvResetConfig is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      /** @deprecated NV APIs are deprecated starting from Android U. */
      @Override public void nvWriteCdmaPrl(int serial, byte[] prl) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeByteArray(prl);
          boolean _status = mRemote.transact(Stub.TRANSACTION_nvWriteCdmaPrl, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method nvWriteCdmaPrl is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      /** @deprecated NV APIs are deprecated starting from Android U. */
      @Override public void nvWriteItem(int serial, android.hardware.radio.modem.NvWriteItem item) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedObject(item, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_nvWriteItem, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method nvWriteItem is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void requestShutdown(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_requestShutdown, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method requestShutdown is unimplemented.");
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
      @Override public void sendDeviceState(int serial, int deviceStateType, boolean state) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(deviceStateType);
          _data.writeBoolean(state);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendDeviceState, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendDeviceState is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setRadioCapability(int serial, android.hardware.radio.modem.RadioCapability rc) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedObject(rc, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setRadioCapability, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setRadioCapability is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setRadioPower(int serial, boolean powerOn, boolean forEmergencyCall, boolean preferredForEmergencyCall) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeBoolean(powerOn);
          _data.writeBoolean(forEmergencyCall);
          _data.writeBoolean(preferredForEmergencyCall);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setRadioPower, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setRadioPower is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setResponseFunctions(android.hardware.radio.modem.IRadioModemResponse radioModemResponse, android.hardware.radio.modem.IRadioModemIndication radioModemIndication) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(radioModemResponse);
          _data.writeStrongInterface(radioModemIndication);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setResponseFunctions, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setResponseFunctions is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getImei(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getImei, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getImei is unimplemented.");
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
    static final int TRANSACTION_enableModem = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_getBasebandVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_getDeviceIdentity = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_getHardwareConfig = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getModemActivityInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getModemStackStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getRadioCapability = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_nvReadItem = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_nvResetConfig = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_nvWriteCdmaPrl = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_nvWriteItem = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_requestShutdown = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_responseAcknowledgement = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_sendDeviceState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_setRadioCapability = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_setRadioPower = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_setResponseFunctions = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_getImei = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$radio$modem$IRadioModem".replace('$', '.');
  public void enableModem(int serial, boolean on) throws android.os.RemoteException;
  public void getBasebandVersion(int serial) throws android.os.RemoteException;
  /** @deprecated use getImei(int serial) */
  @Deprecated
  public void getDeviceIdentity(int serial) throws android.os.RemoteException;
  public void getHardwareConfig(int serial) throws android.os.RemoteException;
  public void getModemActivityInfo(int serial) throws android.os.RemoteException;
  public void getModemStackStatus(int serial) throws android.os.RemoteException;
  public void getRadioCapability(int serial) throws android.os.RemoteException;
  /** @deprecated NV APIs are deprecated starting from Android U. */
  @Deprecated
  public void nvReadItem(int serial, int itemId) throws android.os.RemoteException;
  public void nvResetConfig(int serial, int resetType) throws android.os.RemoteException;
  /** @deprecated NV APIs are deprecated starting from Android U. */
  @Deprecated
  public void nvWriteCdmaPrl(int serial, byte[] prl) throws android.os.RemoteException;
  /** @deprecated NV APIs are deprecated starting from Android U. */
  @Deprecated
  public void nvWriteItem(int serial, android.hardware.radio.modem.NvWriteItem item) throws android.os.RemoteException;
  public void requestShutdown(int serial) throws android.os.RemoteException;
  public void responseAcknowledgement() throws android.os.RemoteException;
  public void sendDeviceState(int serial, int deviceStateType, boolean state) throws android.os.RemoteException;
  public void setRadioCapability(int serial, android.hardware.radio.modem.RadioCapability rc) throws android.os.RemoteException;
  public void setRadioPower(int serial, boolean powerOn, boolean forEmergencyCall, boolean preferredForEmergencyCall) throws android.os.RemoteException;
  public void setResponseFunctions(android.hardware.radio.modem.IRadioModemResponse radioModemResponse, android.hardware.radio.modem.IRadioModemIndication radioModemIndication) throws android.os.RemoteException;
  public void getImei(int serial) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
