/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.system.virtualizationservice;
public interface IVirtualMachine extends android.os.IInterface
{
  /** Default implementation for IVirtualMachine. */
  public static class Default implements android.system.virtualizationservice.IVirtualMachine
  {
    /** Get the CID allocated to the VM. */
    @Override public int getCid() throws android.os.RemoteException
    {
      return 0;
    }
    /** Returns the current lifecycle state of the VM. */
    @Override public int getState() throws android.os.RemoteException
    {
      return 0;
    }
    /**
     * Register a Binder object to get callbacks when the state of the VM changes, such as if it
     * dies.
     */
    @Override public void registerCallback(android.system.virtualizationservice.IVirtualMachineCallback callback) throws android.os.RemoteException
    {
    }
    /** Starts running the VM. */
    @Override public void start() throws android.os.RemoteException
    {
    }
    /**
     * Stops this virtual machine. Stopping a virtual machine is like pulling the plug on a real
     * computer; the machine halts immediately. Software running on the virtual machine is not
     * notified with the event.
     */
    @Override public void stop() throws android.os.RemoteException
    {
    }
    /** Communicate app low-memory notifications to the VM. */
    @Override public void onTrimMemory(int level) throws android.os.RemoteException
    {
    }
    /** Open a vsock connection to the CID of the VM on the given port. */
    @Override public android.os.ParcelFileDescriptor connectVsock(int port) throws android.os.RemoteException
    {
      return null;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.system.virtualizationservice.IVirtualMachine
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.system.virtualizationservice.IVirtualMachine interface,
     * generating a proxy if needed.
     */
    public static android.system.virtualizationservice.IVirtualMachine asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.system.virtualizationservice.IVirtualMachine))) {
        return ((android.system.virtualizationservice.IVirtualMachine)iin);
      }
      return new android.system.virtualizationservice.IVirtualMachine.Stub.Proxy(obj);
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
      }
      switch (code)
      {
        case TRANSACTION_getCid:
        {
          int _result = this.getCid();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getState:
        {
          int _result = this.getState();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_registerCallback:
        {
          android.system.virtualizationservice.IVirtualMachineCallback _arg0;
          _arg0 = android.system.virtualizationservice.IVirtualMachineCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.registerCallback(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_start:
        {
          this.start();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_stop:
        {
          this.stop();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onTrimMemory:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.onTrimMemory(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_connectVsock:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          android.os.ParcelFileDescriptor _result = this.connectVsock(_arg0);
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
    private static class Proxy implements android.system.virtualizationservice.IVirtualMachine
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      /** Get the CID allocated to the VM. */
      @Override public int getCid() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCid, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Returns the current lifecycle state of the VM. */
      @Override public int getState() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getState, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Register a Binder object to get callbacks when the state of the VM changes, such as if it
       * dies.
       */
      @Override public void registerCallback(android.system.virtualizationservice.IVirtualMachineCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerCallback, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Starts running the VM. */
      @Override public void start() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_start, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Stops this virtual machine. Stopping a virtual machine is like pulling the plug on a real
       * computer; the machine halts immediately. Software running on the virtual machine is not
       * notified with the event.
       */
      @Override public void stop() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stop, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Communicate app low-memory notifications to the VM. */
      @Override public void onTrimMemory(int level) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(level);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onTrimMemory, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Open a vsock connection to the CID of the VM on the given port. */
      @Override public android.os.ParcelFileDescriptor connectVsock(int port) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.os.ParcelFileDescriptor _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(port);
          boolean _status = mRemote.transact(Stub.TRANSACTION_connectVsock, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readTypedObject(android.os.ParcelFileDescriptor.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_getCid = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_getState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_registerCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_start = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_stop = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_onTrimMemory = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_connectVsock = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
  }
  public static final java.lang.String DESCRIPTOR = "android$system$virtualizationservice$IVirtualMachine".replace('$', '.');
  /** Get the CID allocated to the VM. */
  public int getCid() throws android.os.RemoteException;
  /** Returns the current lifecycle state of the VM. */
  public int getState() throws android.os.RemoteException;
  /**
   * Register a Binder object to get callbacks when the state of the VM changes, such as if it
   * dies.
   */
  public void registerCallback(android.system.virtualizationservice.IVirtualMachineCallback callback) throws android.os.RemoteException;
  /** Starts running the VM. */
  public void start() throws android.os.RemoteException;
  /**
   * Stops this virtual machine. Stopping a virtual machine is like pulling the plug on a real
   * computer; the machine halts immediately. Software running on the virtual machine is not
   * notified with the event.
   */
  public void stop() throws android.os.RemoteException;
  /** Communicate app low-memory notifications to the VM. */
  public void onTrimMemory(int level) throws android.os.RemoteException;
  /** Open a vsock connection to the CID of the VM on the given port. */
  public android.os.ParcelFileDescriptor connectVsock(int port) throws android.os.RemoteException;
}
