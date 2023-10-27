/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.usb.gadget;
public interface IUsbGadget extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 1;
  public static final String HASH = "cb628c69682659911bca5c1d04042adba7f0de4b";
  /** Default implementation for IUsbGadget. */
  public static class Default implements android.hardware.usb.gadget.IUsbGadget
  {
    @Override public void setCurrentUsbFunctions(long functions, android.hardware.usb.gadget.IUsbGadgetCallback callback, long timeoutMs, long transactionId) throws android.os.RemoteException
    {
    }
    @Override public void getCurrentUsbFunctions(android.hardware.usb.gadget.IUsbGadgetCallback callback, long transactionId) throws android.os.RemoteException
    {
    }
    @Override public void getUsbSpeed(android.hardware.usb.gadget.IUsbGadgetCallback callback, long transactionId) throws android.os.RemoteException
    {
    }
    @Override public void reset(android.hardware.usb.gadget.IUsbGadgetCallback callback, long transactionId) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.usb.gadget.IUsbGadget
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.usb.gadget.IUsbGadget interface,
     * generating a proxy if needed.
     */
    public static android.hardware.usb.gadget.IUsbGadget asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.usb.gadget.IUsbGadget))) {
        return ((android.hardware.usb.gadget.IUsbGadget)iin);
      }
      return new android.hardware.usb.gadget.IUsbGadget.Stub.Proxy(obj);
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
        case TRANSACTION_setCurrentUsbFunctions:
        {
          long _arg0;
          _arg0 = data.readLong();
          android.hardware.usb.gadget.IUsbGadgetCallback _arg1;
          _arg1 = android.hardware.usb.gadget.IUsbGadgetCallback.Stub.asInterface(data.readStrongBinder());
          long _arg2;
          _arg2 = data.readLong();
          long _arg3;
          _arg3 = data.readLong();
          data.enforceNoDataAvail();
          this.setCurrentUsbFunctions(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_getCurrentUsbFunctions:
        {
          android.hardware.usb.gadget.IUsbGadgetCallback _arg0;
          _arg0 = android.hardware.usb.gadget.IUsbGadgetCallback.Stub.asInterface(data.readStrongBinder());
          long _arg1;
          _arg1 = data.readLong();
          data.enforceNoDataAvail();
          this.getCurrentUsbFunctions(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getUsbSpeed:
        {
          android.hardware.usb.gadget.IUsbGadgetCallback _arg0;
          _arg0 = android.hardware.usb.gadget.IUsbGadgetCallback.Stub.asInterface(data.readStrongBinder());
          long _arg1;
          _arg1 = data.readLong();
          data.enforceNoDataAvail();
          this.getUsbSpeed(_arg0, _arg1);
          break;
        }
        case TRANSACTION_reset:
        {
          android.hardware.usb.gadget.IUsbGadgetCallback _arg0;
          _arg0 = android.hardware.usb.gadget.IUsbGadgetCallback.Stub.asInterface(data.readStrongBinder());
          long _arg1;
          _arg1 = data.readLong();
          data.enforceNoDataAvail();
          this.reset(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.usb.gadget.IUsbGadget
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
      @Override public void setCurrentUsbFunctions(long functions, android.hardware.usb.gadget.IUsbGadgetCallback callback, long timeoutMs, long transactionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(functions);
          _data.writeStrongInterface(callback);
          _data.writeLong(timeoutMs);
          _data.writeLong(transactionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCurrentUsbFunctions, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setCurrentUsbFunctions is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getCurrentUsbFunctions(android.hardware.usb.gadget.IUsbGadgetCallback callback, long transactionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          _data.writeLong(transactionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCurrentUsbFunctions, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getCurrentUsbFunctions is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getUsbSpeed(android.hardware.usb.gadget.IUsbGadgetCallback callback, long transactionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          _data.writeLong(transactionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getUsbSpeed, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getUsbSpeed is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void reset(android.hardware.usb.gadget.IUsbGadgetCallback callback, long transactionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          _data.writeLong(transactionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_reset, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method reset is unimplemented.");
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
    static final int TRANSACTION_setCurrentUsbFunctions = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_getCurrentUsbFunctions = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_getUsbSpeed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_reset = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$usb$gadget$IUsbGadget".replace('$', '.');
  public void setCurrentUsbFunctions(long functions, android.hardware.usb.gadget.IUsbGadgetCallback callback, long timeoutMs, long transactionId) throws android.os.RemoteException;
  public void getCurrentUsbFunctions(android.hardware.usb.gadget.IUsbGadgetCallback callback, long transactionId) throws android.os.RemoteException;
  public void getUsbSpeed(android.hardware.usb.gadget.IUsbGadgetCallback callback, long transactionId) throws android.os.RemoteException;
  public void reset(android.hardware.usb.gadget.IUsbGadgetCallback callback, long transactionId) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
