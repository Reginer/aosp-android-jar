/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.ims;
public interface IRadioImsIndication extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 1;
  public static final String HASH = "b09f8d98a60fbe74cefaca7aea9903ab5450110a";
  /** Default implementation for IRadioImsIndication. */
  public static class Default implements android.hardware.radio.ims.IRadioImsIndication
  {
    @Override public void onConnectionSetupFailure(int type, int token, android.hardware.radio.ims.ConnectionFailureInfo info) throws android.os.RemoteException
    {
    }
    @Override public void notifyAnbr(int type, int mediaType, int direction, int bitsPerSecond) throws android.os.RemoteException
    {
    }
    @Override public void triggerImsDeregistration(int type, int reason) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.radio.ims.IRadioImsIndication
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.radio.ims.IRadioImsIndication interface,
     * generating a proxy if needed.
     */
    public static android.hardware.radio.ims.IRadioImsIndication asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.radio.ims.IRadioImsIndication))) {
        return ((android.hardware.radio.ims.IRadioImsIndication)iin);
      }
      return new android.hardware.radio.ims.IRadioImsIndication.Stub.Proxy(obj);
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
        case TRANSACTION_onConnectionSetupFailure:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          android.hardware.radio.ims.ConnectionFailureInfo _arg2;
          _arg2 = data.readTypedObject(android.hardware.radio.ims.ConnectionFailureInfo.CREATOR);
          data.enforceNoDataAvail();
          this.onConnectionSetupFailure(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_notifyAnbr:
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
          this.notifyAnbr(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_triggerImsDeregistration:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.triggerImsDeregistration(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.radio.ims.IRadioImsIndication
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
      @Override public void onConnectionSetupFailure(int type, int token, android.hardware.radio.ims.ConnectionFailureInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeInt(token);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onConnectionSetupFailure, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method onConnectionSetupFailure is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void notifyAnbr(int type, int mediaType, int direction, int bitsPerSecond) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeInt(mediaType);
          _data.writeInt(direction);
          _data.writeInt(bitsPerSecond);
          boolean _status = mRemote.transact(Stub.TRANSACTION_notifyAnbr, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method notifyAnbr is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void triggerImsDeregistration(int type, int reason) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeInt(reason);
          boolean _status = mRemote.transact(Stub.TRANSACTION_triggerImsDeregistration, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method triggerImsDeregistration is unimplemented.");
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
    static final int TRANSACTION_onConnectionSetupFailure = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_notifyAnbr = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_triggerImsDeregistration = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$radio$ims$IRadioImsIndication".replace('$', '.');
  public void onConnectionSetupFailure(int type, int token, android.hardware.radio.ims.ConnectionFailureInfo info) throws android.os.RemoteException;
  public void notifyAnbr(int type, int mediaType, int direction, int bitsPerSecond) throws android.os.RemoteException;
  public void triggerImsDeregistration(int type, int reason) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
