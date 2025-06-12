/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 2 --hash e47d23f579ff7a897fb03e7e7f1c3006cfc6036b -t --stability vintf --min_sdk_version platform_apis --ninja -d out/soong/.intermediates/frameworks/hardware/interfaces/location/altitude/aidl/android.frameworks.location.altitude-V2-java-source/gen/android/frameworks/location/altitude/IAltitudeService.java.d -o out/soong/.intermediates/frameworks/hardware/interfaces/location/altitude/aidl/android.frameworks.location.altitude-V2-java-source/gen -Nframeworks/hardware/interfaces/location/altitude/aidl/aidl_api/android.frameworks.location.altitude/2 frameworks/hardware/interfaces/location/altitude/aidl/aidl_api/android.frameworks.location.altitude/2/android/frameworks/location/altitude/IAltitudeService.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.frameworks.location.altitude;
/** @hide */
public interface IAltitudeService extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "e47d23f579ff7a897fb03e7e7f1c3006cfc6036b";
  /** Default implementation for IAltitudeService. */
  public static class Default implements android.frameworks.location.altitude.IAltitudeService
  {
    @Override public android.frameworks.location.altitude.AddMslAltitudeToLocationResponse addMslAltitudeToLocation(android.frameworks.location.altitude.AddMslAltitudeToLocationRequest request) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.frameworks.location.altitude.GetGeoidHeightResponse getGeoidHeight(android.frameworks.location.altitude.GetGeoidHeightRequest request) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.frameworks.location.altitude.IAltitudeService
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.frameworks.location.altitude.IAltitudeService interface,
     * generating a proxy if needed.
     */
    public static android.frameworks.location.altitude.IAltitudeService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.frameworks.location.altitude.IAltitudeService))) {
        return ((android.frameworks.location.altitude.IAltitudeService)iin);
      }
      return new android.frameworks.location.altitude.IAltitudeService.Stub.Proxy(obj);
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
        case TRANSACTION_addMslAltitudeToLocation:
        {
          return "addMslAltitudeToLocation";
        }
        case TRANSACTION_getGeoidHeight:
        {
          return "getGeoidHeight";
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
        case TRANSACTION_addMslAltitudeToLocation:
        {
          android.frameworks.location.altitude.AddMslAltitudeToLocationRequest _arg0;
          _arg0 = data.readTypedObject(android.frameworks.location.altitude.AddMslAltitudeToLocationRequest.CREATOR);
          data.enforceNoDataAvail();
          android.frameworks.location.altitude.AddMslAltitudeToLocationResponse _result = this.addMslAltitudeToLocation(_arg0);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getGeoidHeight:
        {
          android.frameworks.location.altitude.GetGeoidHeightRequest _arg0;
          _arg0 = data.readTypedObject(android.frameworks.location.altitude.GetGeoidHeightRequest.CREATOR);
          data.enforceNoDataAvail();
          android.frameworks.location.altitude.GetGeoidHeightResponse _result = this.getGeoidHeight(_arg0);
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
    private static class Proxy implements android.frameworks.location.altitude.IAltitudeService
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
      @Override public android.frameworks.location.altitude.AddMslAltitudeToLocationResponse addMslAltitudeToLocation(android.frameworks.location.altitude.AddMslAltitudeToLocationRequest request) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.frameworks.location.altitude.AddMslAltitudeToLocationResponse _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(request, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_addMslAltitudeToLocation, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method addMslAltitudeToLocation is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.frameworks.location.altitude.AddMslAltitudeToLocationResponse.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.frameworks.location.altitude.GetGeoidHeightResponse getGeoidHeight(android.frameworks.location.altitude.GetGeoidHeightRequest request) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.frameworks.location.altitude.GetGeoidHeightResponse _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(request, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getGeoidHeight, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getGeoidHeight is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.frameworks.location.altitude.GetGeoidHeightResponse.CREATOR);
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
    static final int TRANSACTION_addMslAltitudeToLocation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_getGeoidHeight = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android$frameworks$location$altitude$IAltitudeService".replace('$', '.');
  public android.frameworks.location.altitude.AddMslAltitudeToLocationResponse addMslAltitudeToLocation(android.frameworks.location.altitude.AddMslAltitudeToLocationRequest request) throws android.os.RemoteException;
  public android.frameworks.location.altitude.GetGeoidHeightResponse getGeoidHeight(android.frameworks.location.altitude.GetGeoidHeightRequest request) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
