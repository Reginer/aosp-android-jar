/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash fc7eeb47f5238e538dead4af7575507920c359f7 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.config-V4-java-source/gen/android/hardware/radio/config/IRadioConfig.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.config-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.config/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.config/4/android/hardware/radio/config/IRadioConfig.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.config;
/** @hide */
public interface IRadioConfig extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 4;
  public static final String HASH = "fc7eeb47f5238e538dead4af7575507920c359f7";
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
    @Override public void getSimultaneousCallingSupport(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getSimTypeInfo(int serial) throws android.os.RemoteException
    {
    }
    @Override public void setSimType(int serial, int[] simTypes) throws android.os.RemoteException
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
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
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
        case TRANSACTION_getSimultaneousCallingSupport:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getSimultaneousCallingSupport(_arg0);
          break;
        }
        case TRANSACTION_getSimTypeInfo:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getSimTypeInfo(_arg0);
          break;
        }
        case TRANSACTION_setSimType:
        {
          int _arg0;
          _arg0 = data.readInt();
          int[] _arg1;
          _arg1 = data.createIntArray();
          data.enforceNoDataAvail();
          this.setSimType(_arg0, _arg1);
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
      @Override public void getSimultaneousCallingSupport(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSimultaneousCallingSupport, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getSimultaneousCallingSupport is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getSimTypeInfo(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSimTypeInfo, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getSimTypeInfo is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setSimType(int serial, int[] simTypes) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeIntArray(simTypes);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setSimType, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setSimType is unimplemented.");
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
    static final int TRANSACTION_getSimultaneousCallingSupport = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_getSimTypeInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_setSimType = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android$hardware$radio$config$IRadioConfig".replace('$', '.');
  public void getHalDeviceCapabilities(int serial) throws android.os.RemoteException;
  public void getNumOfLiveModems(int serial) throws android.os.RemoteException;
  public void getPhoneCapability(int serial) throws android.os.RemoteException;
  public void getSimSlotsStatus(int serial) throws android.os.RemoteException;
  public void setNumOfLiveModems(int serial, byte numOfLiveModems) throws android.os.RemoteException;
  public void setPreferredDataModem(int serial, byte modemId) throws android.os.RemoteException;
  public void setResponseFunctions(android.hardware.radio.config.IRadioConfigResponse radioConfigResponse, android.hardware.radio.config.IRadioConfigIndication radioConfigIndication) throws android.os.RemoteException;
  public void setSimSlotsMapping(int serial, android.hardware.radio.config.SlotPortMapping[] slotMap) throws android.os.RemoteException;
  public void getSimultaneousCallingSupport(int serial) throws android.os.RemoteException;
  public void getSimTypeInfo(int serial) throws android.os.RemoteException;
  public void setSimType(int serial, int[] simTypes) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
