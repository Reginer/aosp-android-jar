/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.usb.gadget;
public interface IUsbGadgetCallback extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 1;
  public static final String HASH = "cb628c69682659911bca5c1d04042adba7f0de4b";
  /** Default implementation for IUsbGadgetCallback. */
  public static class Default implements android.hardware.usb.gadget.IUsbGadgetCallback
  {
    @Override public void setCurrentUsbFunctionsCb(long functions, int status, long transactionId) throws android.os.RemoteException
    {
    }
    @Override public void getCurrentUsbFunctionsCb(long functions, int status, long transactionId) throws android.os.RemoteException
    {
    }
    @Override public void getUsbSpeedCb(int speed, long transactionId) throws android.os.RemoteException
    {
    }
    @Override public void resetCb(int status, long transactionId) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.usb.gadget.IUsbGadgetCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.usb.gadget.IUsbGadgetCallback interface,
     * generating a proxy if needed.
     */
    public static android.hardware.usb.gadget.IUsbGadgetCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.usb.gadget.IUsbGadgetCallback))) {
        return ((android.hardware.usb.gadget.IUsbGadgetCallback)iin);
      }
      return new android.hardware.usb.gadget.IUsbGadgetCallback.Stub.Proxy(obj);
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
        case TRANSACTION_setCurrentUsbFunctionsCb:
        {
          long _arg0;
          _arg0 = data.readLong();
          int _arg1;
          _arg1 = data.readInt();
          long _arg2;
          _arg2 = data.readLong();
          data.enforceNoDataAvail();
          this.setCurrentUsbFunctionsCb(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_getCurrentUsbFunctionsCb:
        {
          long _arg0;
          _arg0 = data.readLong();
          int _arg1;
          _arg1 = data.readInt();
          long _arg2;
          _arg2 = data.readLong();
          data.enforceNoDataAvail();
          this.getCurrentUsbFunctionsCb(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_getUsbSpeedCb:
        {
          int _arg0;
          _arg0 = data.readInt();
          long _arg1;
          _arg1 = data.readLong();
          data.enforceNoDataAvail();
          this.getUsbSpeedCb(_arg0, _arg1);
          break;
        }
        case TRANSACTION_resetCb:
        {
          int _arg0;
          _arg0 = data.readInt();
          long _arg1;
          _arg1 = data.readLong();
          data.enforceNoDataAvail();
          this.resetCb(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.usb.gadget.IUsbGadgetCallback
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
      @Override public void setCurrentUsbFunctionsCb(long functions, int status, long transactionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(functions);
          _data.writeInt(status);
          _data.writeLong(transactionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCurrentUsbFunctionsCb, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setCurrentUsbFunctionsCb is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getCurrentUsbFunctionsCb(long functions, int status, long transactionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(functions);
          _data.writeInt(status);
          _data.writeLong(transactionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCurrentUsbFunctionsCb, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getCurrentUsbFunctionsCb is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getUsbSpeedCb(int speed, long transactionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(speed);
          _data.writeLong(transactionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getUsbSpeedCb, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getUsbSpeedCb is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void resetCb(int status, long transactionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(status);
          _data.writeLong(transactionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_resetCb, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method resetCb is unimplemented.");
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
    static final int TRANSACTION_setCurrentUsbFunctionsCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_getCurrentUsbFunctionsCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_getUsbSpeedCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_resetCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$usb$gadget$IUsbGadgetCallback".replace('$', '.');
  public void setCurrentUsbFunctionsCb(long functions, int status, long transactionId) throws android.os.RemoteException;
  public void getCurrentUsbFunctionsCb(long functions, int status, long transactionId) throws android.os.RemoteException;
  public void getUsbSpeedCb(int speed, long transactionId) throws android.os.RemoteException;
  public void resetCb(int status, long transactionId) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
