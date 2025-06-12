/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash b2a615a151c7114c4216b1987fd32d40c797d00a --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.ims-V3-java-source/gen/android/hardware/radio/ims/IRadioIms.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.ims-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.ims/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.ims/3/android/hardware/radio/ims/IRadioIms.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.ims;
/** @hide */
public interface IRadioIms extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 3;
  public static final String HASH = "b2a615a151c7114c4216b1987fd32d40c797d00a";
  /** Default implementation for IRadioIms. */
  public static class Default implements android.hardware.radio.ims.IRadioIms
  {
    @Override public void setSrvccCallInfo(int serial, android.hardware.radio.ims.SrvccCall[] srvccCalls) throws android.os.RemoteException
    {
    }
    @Override public void updateImsRegistrationInfo(int serial, android.hardware.radio.ims.ImsRegistration imsRegistration) throws android.os.RemoteException
    {
    }
    @Override public void startImsTraffic(int serial, int token, int imsTrafficType, int accessNetworkType, int trafficDirection) throws android.os.RemoteException
    {
    }
    @Override public void stopImsTraffic(int serial, int token) throws android.os.RemoteException
    {
    }
    @Override public void triggerEpsFallback(int serial, int reason) throws android.os.RemoteException
    {
    }
    @Override public void setResponseFunctions(android.hardware.radio.ims.IRadioImsResponse radioImsResponse, android.hardware.radio.ims.IRadioImsIndication radioImsIndication) throws android.os.RemoteException
    {
    }
    @Override public void sendAnbrQuery(int serial, int mediaType, int direction, int bitsPerSecond) throws android.os.RemoteException
    {
    }
    @Override public void updateImsCallStatus(int serial, android.hardware.radio.ims.ImsCall[] imsCalls) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.radio.ims.IRadioIms
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.radio.ims.IRadioIms interface,
     * generating a proxy if needed.
     */
    public static android.hardware.radio.ims.IRadioIms asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.radio.ims.IRadioIms))) {
        return ((android.hardware.radio.ims.IRadioIms)iin);
      }
      return new android.hardware.radio.ims.IRadioIms.Stub.Proxy(obj);
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
        case TRANSACTION_setSrvccCallInfo:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.ims.SrvccCall[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.radio.ims.SrvccCall.CREATOR);
          data.enforceNoDataAvail();
          this.setSrvccCallInfo(_arg0, _arg1);
          break;
        }
        case TRANSACTION_updateImsRegistrationInfo:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.ims.ImsRegistration _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.ims.ImsRegistration.CREATOR);
          data.enforceNoDataAvail();
          this.updateImsRegistrationInfo(_arg0, _arg1);
          break;
        }
        case TRANSACTION_startImsTraffic:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          int _arg3;
          _arg3 = data.readInt();
          int _arg4;
          _arg4 = data.readInt();
          data.enforceNoDataAvail();
          this.startImsTraffic(_arg0, _arg1, _arg2, _arg3, _arg4);
          break;
        }
        case TRANSACTION_stopImsTraffic:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.stopImsTraffic(_arg0, _arg1);
          break;
        }
        case TRANSACTION_triggerEpsFallback:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.triggerEpsFallback(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setResponseFunctions:
        {
          android.hardware.radio.ims.IRadioImsResponse _arg0;
          _arg0 = android.hardware.radio.ims.IRadioImsResponse.Stub.asInterface(data.readStrongBinder());
          android.hardware.radio.ims.IRadioImsIndication _arg1;
          _arg1 = android.hardware.radio.ims.IRadioImsIndication.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.setResponseFunctions(_arg0, _arg1);
          break;
        }
        case TRANSACTION_sendAnbrQuery:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          int _arg3;
          _arg3 = data.readInt();
          data.enforceNoDataAvail();
          this.sendAnbrQuery(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_updateImsCallStatus:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.ims.ImsCall[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.radio.ims.ImsCall.CREATOR);
          data.enforceNoDataAvail();
          this.updateImsCallStatus(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.radio.ims.IRadioIms
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
      @Override public void setSrvccCallInfo(int serial, android.hardware.radio.ims.SrvccCall[] srvccCalls) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedArray(srvccCalls, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setSrvccCallInfo, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setSrvccCallInfo is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void updateImsRegistrationInfo(int serial, android.hardware.radio.ims.ImsRegistration imsRegistration) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedObject(imsRegistration, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_updateImsRegistrationInfo, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method updateImsRegistrationInfo is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void startImsTraffic(int serial, int token, int imsTrafficType, int accessNetworkType, int trafficDirection) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(token);
          _data.writeInt(imsTrafficType);
          _data.writeInt(accessNetworkType);
          _data.writeInt(trafficDirection);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startImsTraffic, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method startImsTraffic is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void stopImsTraffic(int serial, int token) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(token);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stopImsTraffic, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method stopImsTraffic is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void triggerEpsFallback(int serial, int reason) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(reason);
          boolean _status = mRemote.transact(Stub.TRANSACTION_triggerEpsFallback, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method triggerEpsFallback is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setResponseFunctions(android.hardware.radio.ims.IRadioImsResponse radioImsResponse, android.hardware.radio.ims.IRadioImsIndication radioImsIndication) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(radioImsResponse);
          _data.writeStrongInterface(radioImsIndication);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setResponseFunctions, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setResponseFunctions is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sendAnbrQuery(int serial, int mediaType, int direction, int bitsPerSecond) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(mediaType);
          _data.writeInt(direction);
          _data.writeInt(bitsPerSecond);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendAnbrQuery, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method sendAnbrQuery is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void updateImsCallStatus(int serial, android.hardware.radio.ims.ImsCall[] imsCalls) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedArray(imsCalls, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_updateImsCallStatus, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method updateImsCallStatus is unimplemented.");
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
    static final int TRANSACTION_setSrvccCallInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_updateImsRegistrationInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_startImsTraffic = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_stopImsTraffic = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_triggerEpsFallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_setResponseFunctions = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_sendAnbrQuery = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_updateImsCallStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android$hardware$radio$ims$IRadioIms".replace('$', '.');
  public void setSrvccCallInfo(int serial, android.hardware.radio.ims.SrvccCall[] srvccCalls) throws android.os.RemoteException;
  public void updateImsRegistrationInfo(int serial, android.hardware.radio.ims.ImsRegistration imsRegistration) throws android.os.RemoteException;
  public void startImsTraffic(int serial, int token, int imsTrafficType, int accessNetworkType, int trafficDirection) throws android.os.RemoteException;
  public void stopImsTraffic(int serial, int token) throws android.os.RemoteException;
  public void triggerEpsFallback(int serial, int reason) throws android.os.RemoteException;
  public void setResponseFunctions(android.hardware.radio.ims.IRadioImsResponse radioImsResponse, android.hardware.radio.ims.IRadioImsIndication radioImsIndication) throws android.os.RemoteException;
  public void sendAnbrQuery(int serial, int mediaType, int direction, int bitsPerSecond) throws android.os.RemoteException;
  public void updateImsCallStatus(int serial, android.hardware.radio.ims.ImsCall[] imsCalls) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
