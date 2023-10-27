/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.config;
public interface IRadioConfig extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "0be135cf3de9586d6aabb58cb6af0ba425431743";
  /** Default implementation for IRadioConfig. */
  public static class Default implements android.hardware.radio.config.IRadioConfig
  {
    @Override public void getHalDeviceCapabilities(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getNumOfLiveModems(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getPhoneCapability(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getSimSlotsStatus(int serial) throws android.os.RemoteException
    {
    }
    @Override public void setNumOfLiveModems(int serial, byte numOfLiveModems) throws android.os.RemoteException
    {
    }
    @Override public void setPreferredDataModem(int serial, byte modemId) throws android.os.RemoteException
    {
    }
    @Override public void setResponseFunctions(android.hardware.radio.config.IRadioConfigResponse radioConfigResponse, android.hardware.radio.config.IRadioConfigIndication radioConfigIndication) throws android.os.RemoteException
    {
    }
    @Override public void setSimSlotsMapping(int serial, android.hardware.radio.config.SlotPortMapping[] slotMap) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.radio.config.IRadioConfig
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.radio.config.IRadioConfig interface,
     * generating a proxy if needed.
     */
    public static android.hardware.radio.config.IRadioConfig asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.radio.config.IRadioConfig))) {
        return ((android.hardware.radio.config.IRadioConfig)iin);
      }
      return new android.hardware.radio.config.IRadioConfig.Stub.Proxy(obj);
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
        case TRANSACTION_getHalDeviceCapabilities:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getHalDeviceCapabilities(_arg0);
          break;
        }
        case TRANSACTION_getNumOfLiveModems:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getNumOfLiveModems(_arg0);
          break;
        }
        case TRANSACTION_getPhoneCapability:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getPhoneCapability(_arg0);
          break;
        }
        case TRANSACTION_getSimSlotsStatus:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getSimSlotsStatus(_arg0);
          break;
        }
        case TRANSACTION_setNumOfLiveModems:
        {
          int _arg0;
          _arg0 = data.readInt();
          byte _arg1;
          _arg1 = data.readByte();
          data.enforceNoDataAvail();
          this.setNumOfLiveModems(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setPreferredDataModem:
        {
          int _arg0;
          _arg0 = data.readInt();
          byte _arg1;
          _arg1 = data.readByte();
          data.enforceNoDataAvail();
          this.setPreferredDataModem(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setResponseFunctions:
        {
          android.hardware.radio.config.IRadioConfigResponse _arg0;
          _arg0 = android.hardware.radio.config.IRadioConfigResponse.Stub.asInterface(data.readStrongBinder());
          android.hardware.radio.config.IRadioConfigIndication _arg1;
          _arg1 = android.hardware.radio.config.IRadioConfigIndication.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.setResponseFunctions(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setSimSlotsMapping:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.config.SlotPortMapping[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.radio.config.SlotPortMapping.CREATOR);
          data.enforceNoDataAvail();
          this.setSimSlotsMapping(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.radio.config.IRadioConfig
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
      @Override public void getHalDeviceCapabilities(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getHalDeviceCapabilities, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getHalDeviceCapabilities is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getNumOfLiveModems(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getNumOfLiveModems, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getNumOfLiveModems is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getPhoneCapability(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getPhoneCapability, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getPhoneCapability is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getSimSlotsStatus(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSimSlotsStatus, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getSimSlotsStatus is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setNumOfLiveModems(int serial, byte numOfLiveModems) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeByte(numOfLiveModems);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setNumOfLiveModems, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setNumOfLiveModems is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setPreferredDataModem(int serial, byte modemId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeByte(modemId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setPreferredDataModem, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setPreferredDataModem is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setResponseFunctions(android.hardware.radio.config.IRadioConfigResponse radioConfigResponse, android.hardware.radio.config.IRadioConfigIndication radioConfigIndication) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(radioConfigResponse);
          _data.writeStrongInterface(radioConfigIndication);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setResponseFunctions, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setResponseFunctions is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setSimSlotsMapping(int serial, android.hardware.radio.config.SlotPortMapping[] slotMap) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedArray(slotMap, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setSimSlotsMapping, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setSimSlotsMapping is unimplemented.");
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
    static final int TRANSACTION_getHalDeviceCapabilities = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_getNumOfLiveModems = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_getPhoneCapability = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_getSimSlotsStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_setNumOfLiveModems = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_setPreferredDataModem = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_setResponseFunctions = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_setSimSlotsMapping = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$radio$config$IRadioConfig".replace('$', '.');
  public void getHalDeviceCapabilities(int serial) throws android.os.RemoteException;
  public void getNumOfLiveModems(int serial) throws android.os.RemoteException;
  public void getPhoneCapability(int serial) throws android.os.RemoteException;
  public void getSimSlotsStatus(int serial) throws android.os.RemoteException;
  public void setNumOfLiveModems(int serial, byte numOfLiveModems) throws android.os.RemoteException;
  public void setPreferredDataModem(int serial, byte modemId) throws android.os.RemoteException;
  public void setResponseFunctions(android.hardware.radio.config.IRadioConfigResponse radioConfigResponse, android.hardware.radio.config.IRadioConfigIndication radioConfigIndication) throws android.os.RemoteException;
  public void setSimSlotsMapping(int serial, android.hardware.radio.config.SlotPortMapping[] slotMap) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
