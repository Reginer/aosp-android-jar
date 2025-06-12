/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/packages/modules/Virtualization/android/virtualizationservice/aidl/android.system.virtualizationcommon_interface/preprocessed.aidl --ninja -d out/soong/.intermediates/packages/modules/Virtualization/android/virtualizationservice/aidl/android.system.virtualizationservice-java-source/gen/android/system/virtualizationservice/IVirtualMachine.java.d -o out/soong/.intermediates/packages/modules/Virtualization/android/virtualizationservice/aidl/android.system.virtualizationservice-java-source/gen -Npackages/modules/Virtualization/android/virtualizationservice/aidl packages/modules/Virtualization/android/virtualizationservice/aidl/android/system/virtualizationservice/IVirtualMachine.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
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
    /** Access to the VM's memory balloon. */
    @Override public long getMemoryBalloon() throws android.os.RemoteException
    {
      return 0L;
    }
    @Override public void setMemoryBalloon(long num_bytes) throws android.os.RemoteException
    {
    }
    /** Open a vsock connection to the CID of the VM on the given port. */
    @Override public android.os.ParcelFileDescriptor connectVsock(int port) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Create an Accessor in libbinder that will open a vsock connection
     * to the CID of the VM on the given port.
     * 
     * \param instance name of the service that the accessor is responsible for.
     *        This is the same instance that we expect clients to use when trying
     *        to get the service with the ServiceManager APIs.
     * 
     * \return IBinder of the IAccessor on success, or throws a service specific exception
     *         on error. See the ERROR_* values above.
     */
    @Override public android.os.IBinder createAccessorBinder(java.lang.String instance, int port) throws android.os.RemoteException
    {
      return null;
    }
    /** Set the name of the peer end (ptsname) of the host console. */
    @Override public void setHostConsoleName(java.lang.String pathname) throws android.os.RemoteException
    {
    }
    /** Suspends the VM vcpus. */
    @Override public void suspend() throws android.os.RemoteException
    {
    }
    /** Resumes the suspended VM vcpus. */
    @Override public void resume() throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.system.virtualizationservice.IVirtualMachine
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
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
      if (code == INTERFACE_TRANSACTION) {
        reply.writeString(descriptor);
        return true;
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
        case TRANSACTION_getMemoryBalloon:
        {
          long _result = this.getMemoryBalloon();
          reply.writeNoException();
          reply.writeLong(_result);
          break;
        }
        case TRANSACTION_setMemoryBalloon:
        {
          long _arg0;
          _arg0 = data.readLong();
          data.enforceNoDataAvail();
          this.setMemoryBalloon(_arg0);
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
        case TRANSACTION_createAccessorBinder:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          android.os.IBinder _result = this.createAccessorBinder(_arg0, _arg1);
          reply.writeNoException();
          reply.writeStrongBinder(_result);
          break;
        }
        case TRANSACTION_setHostConsoleName:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          data.enforceNoDataAvail();
          this.setHostConsoleName(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_suspend:
        {
          this.suspend();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_resume:
        {
          this.resume();
          reply.writeNoException();
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
      /** Access to the VM's memory balloon. */
      @Override public long getMemoryBalloon() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        long _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getMemoryBalloon, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readLong();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void setMemoryBalloon(long num_bytes) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(num_bytes);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setMemoryBalloon, _data, _reply, 0);
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
      /**
       * Create an Accessor in libbinder that will open a vsock connection
       * to the CID of the VM on the given port.
       * 
       * \param instance name of the service that the accessor is responsible for.
       *        This is the same instance that we expect clients to use when trying
       *        to get the service with the ServiceManager APIs.
       * 
       * \return IBinder of the IAccessor on success, or throws a service specific exception
       *         on error. See the ERROR_* values above.
       */
      @Override public android.os.IBinder createAccessorBinder(java.lang.String instance, int port) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.os.IBinder _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(instance);
          _data.writeInt(port);
          boolean _status = mRemote.transact(Stub.TRANSACTION_createAccessorBinder, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readStrongBinder();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Set the name of the peer end (ptsname) of the host console. */
      @Override public void setHostConsoleName(java.lang.String pathname) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(pathname);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setHostConsoleName, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Suspends the VM vcpus. */
      @Override public void suspend() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_suspend, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Resumes the suspended VM vcpus. */
      @Override public void resume() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_resume, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_getCid = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_getState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_registerCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_start = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_stop = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getMemoryBalloon = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_setMemoryBalloon = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_connectVsock = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_createAccessorBinder = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_setHostConsoleName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_suspend = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_resume = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android.system.virtualizationservice.IVirtualMachine";
  /**
   * Encountered an unexpected error. This is an implementation detail and the client
   * can do nothing about it.
   * This is used as a Service Specific Exception.
   */
  public static final int ERROR_UNEXPECTED = -1;
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
  /** Access to the VM's memory balloon. */
  public long getMemoryBalloon() throws android.os.RemoteException;
  public void setMemoryBalloon(long num_bytes) throws android.os.RemoteException;
  /** Open a vsock connection to the CID of the VM on the given port. */
  public android.os.ParcelFileDescriptor connectVsock(int port) throws android.os.RemoteException;
  /**
   * Create an Accessor in libbinder that will open a vsock connection
   * to the CID of the VM on the given port.
   * 
   * \param instance name of the service that the accessor is responsible for.
   *        This is the same instance that we expect clients to use when trying
   *        to get the service with the ServiceManager APIs.
   * 
   * \return IBinder of the IAccessor on success, or throws a service specific exception
   *         on error. See the ERROR_* values above.
   */
  public android.os.IBinder createAccessorBinder(java.lang.String instance, int port) throws android.os.RemoteException;
  /** Set the name of the peer end (ptsname) of the host console. */
  public void setHostConsoleName(java.lang.String pathname) throws android.os.RemoteException;
  /** Suspends the VM vcpus. */
  public void suspend() throws android.os.RemoteException;
  /** Resumes the suspended VM vcpus. */
  public void resume() throws android.os.RemoteException;
}
