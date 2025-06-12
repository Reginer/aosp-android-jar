/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash fc7eeb47f5238e538dead4af7575507920c359f7 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.config-V4-java-source/gen/android/hardware/radio/config/IRadioConfigIndication.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.config-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.config/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.config/4/android/hardware/radio/config/IRadioConfigIndication.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.config;
/** @hide */
public interface IRadioConfigIndication extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 4;
  public static final String HASH = "fc7eeb47f5238e538dead4af7575507920c359f7";
  /** Default implementation for IRadioConfigIndication. */
  public static class Default implements android.hardware.radio.config.IRadioConfigIndication
  {
    @Override public void simSlotsStatusChanged(int type, android.hardware.radio.config.SimSlotStatus[] slotStatus) throws android.os.RemoteException
    {
    }
    @Override public void onSimultaneousCallingSupportChanged(int[] enabledLogicalSlots) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.radio.config.IRadioConfigIndication
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.radio.config.IRadioConfigIndication interface,
     * generating a proxy if needed.
     */
    public static android.hardware.radio.config.IRadioConfigIndication asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.radio.config.IRadioConfigIndication))) {
        return ((android.hardware.radio.config.IRadioConfigIndication)iin);
      }
      return new android.hardware.radio.config.IRadioConfigIndication.Stub.Proxy(obj);
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
        case TRANSACTION_simSlotsStatusChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.config.SimSlotStatus[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.radio.config.SimSlotStatus.CREATOR);
          data.enforceNoDataAvail();
          this.simSlotsStatusChanged(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onSimultaneousCallingSupportChanged:
        {
          int[] _arg0;
          _arg0 = data.createIntArray();
          data.enforceNoDataAvail();
          this.onSimultaneousCallingSupportChanged(_arg0);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.radio.config.IRadioConfigIndication
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
      @Override public void simSlotsStatusChanged(int type, android.hardware.radio.config.SimSlotStatus[] slotStatus) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedArray(slotStatus, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_simSlotsStatusChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method simSlotsStatusChanged is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onSimultaneousCallingSupportChanged(int[] enabledLogicalSlots) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeIntArray(enabledLogicalSlots);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onSimultaneousCallingSupportChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method onSimultaneousCallingSupportChanged is unimplemented.");
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
    static final int TRANSACTION_simSlotsStatusChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onSimultaneousCallingSupportChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android$hardware$radio$config$IRadioConfigIndication".replace('$', '.');
  public void simSlotsStatusChanged(int type, android.hardware.radio.config.SimSlotStatus[] slotStatus) throws android.os.RemoteException;
  public void onSimultaneousCallingSupportChanged(int[] enabledLogicalSlots) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
