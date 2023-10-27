/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.system.virtualizationservice;
public interface IVirtualizationService extends android.os.IInterface
{
  /** Default implementation for IVirtualizationService. */
  public static class Default implements android.system.virtualizationservice.IVirtualizationService
  {
    /**
     * Create the VM with the given config file, and return a handle to it ready to start it. If
     * `consoleFd` is provided then console output from the VM will be sent to it. If `osLogFd` is
     * provided then the OS-level logs will be sent to it. `osLogFd` is supported only when the OS
     * running in the VM has the logging system. In case of Microdroid, the logging system is logd.
     */
    @Override public android.system.virtualizationservice.IVirtualMachine createVm(android.system.virtualizationservice.VirtualMachineConfig config, android.os.ParcelFileDescriptor consoleFd, android.os.ParcelFileDescriptor osLogFd) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Initialise an empty partition image of the given size to be used as a writable partition.
     * 
     * The file must be open with both read and write permissions, and should be a new empty file.
     */
    @Override public void initializeWritablePartition(android.os.ParcelFileDescriptor imageFd, long sizeBytes, int type) throws android.os.RemoteException
    {
    }
    /**
     * Create or update an idsig file that digests the given APK file. The idsig file follows the
     * idsig format that is defined by the APK Signature Scheme V4. The idsig file is not updated
     * when it is up to date with the input file, which is checked by comparing the
     * signing_info.apk_digest field in the idsig file with the signer.signed_data.digests.digest
     * field in the input APK file.
     */
    @Override public void createOrUpdateIdsigFile(android.os.ParcelFileDescriptor inputFd, android.os.ParcelFileDescriptor idsigFd) throws android.os.RemoteException
    {
    }
    /**
     * Get a list of all currently running VMs. This method is only intended for debug purposes,
     * and as such is only permitted from the shell user.
     */
    @Override public android.system.virtualizationservice.VirtualMachineDebugInfo[] debugListVms() throws android.os.RemoteException
    {
      return null;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.system.virtualizationservice.IVirtualizationService
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.system.virtualizationservice.IVirtualizationService interface,
     * generating a proxy if needed.
     */
    public static android.system.virtualizationservice.IVirtualizationService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.system.virtualizationservice.IVirtualizationService))) {
        return ((android.system.virtualizationservice.IVirtualizationService)iin);
      }
      return new android.system.virtualizationservice.IVirtualizationService.Stub.Proxy(obj);
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
        case TRANSACTION_createVm:
        {
          android.system.virtualizationservice.VirtualMachineConfig _arg0;
          _arg0 = data.readTypedObject(android.system.virtualizationservice.VirtualMachineConfig.CREATOR);
          android.os.ParcelFileDescriptor _arg1;
          _arg1 = data.readTypedObject(android.os.ParcelFileDescriptor.CREATOR);
          android.os.ParcelFileDescriptor _arg2;
          _arg2 = data.readTypedObject(android.os.ParcelFileDescriptor.CREATOR);
          data.enforceNoDataAvail();
          android.system.virtualizationservice.IVirtualMachine _result = this.createVm(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_initializeWritablePartition:
        {
          android.os.ParcelFileDescriptor _arg0;
          _arg0 = data.readTypedObject(android.os.ParcelFileDescriptor.CREATOR);
          long _arg1;
          _arg1 = data.readLong();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.initializeWritablePartition(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_createOrUpdateIdsigFile:
        {
          android.os.ParcelFileDescriptor _arg0;
          _arg0 = data.readTypedObject(android.os.ParcelFileDescriptor.CREATOR);
          android.os.ParcelFileDescriptor _arg1;
          _arg1 = data.readTypedObject(android.os.ParcelFileDescriptor.CREATOR);
          data.enforceNoDataAvail();
          this.createOrUpdateIdsigFile(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_debugListVms:
        {
          android.system.virtualizationservice.VirtualMachineDebugInfo[] _result = this.debugListVms();
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.system.virtualizationservice.IVirtualizationService
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
      /**
       * Create the VM with the given config file, and return a handle to it ready to start it. If
       * `consoleFd` is provided then console output from the VM will be sent to it. If `osLogFd` is
       * provided then the OS-level logs will be sent to it. `osLogFd` is supported only when the OS
       * running in the VM has the logging system. In case of Microdroid, the logging system is logd.
       */
      @Override public android.system.virtualizationservice.IVirtualMachine createVm(android.system.virtualizationservice.VirtualMachineConfig config, android.os.ParcelFileDescriptor consoleFd, android.os.ParcelFileDescriptor osLogFd) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.system.virtualizationservice.IVirtualMachine _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(config, 0);
          _data.writeTypedObject(consoleFd, 0);
          _data.writeTypedObject(osLogFd, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_createVm, _data, _reply, 0);
          _reply.readException();
          _result = android.system.virtualizationservice.IVirtualMachine.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Initialise an empty partition image of the given size to be used as a writable partition.
       * 
       * The file must be open with both read and write permissions, and should be a new empty file.
       */
      @Override public void initializeWritablePartition(android.os.ParcelFileDescriptor imageFd, long sizeBytes, int type) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(imageFd, 0);
          _data.writeLong(sizeBytes);
          _data.writeInt(type);
          boolean _status = mRemote.transact(Stub.TRANSACTION_initializeWritablePartition, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Create or update an idsig file that digests the given APK file. The idsig file follows the
       * idsig format that is defined by the APK Signature Scheme V4. The idsig file is not updated
       * when it is up to date with the input file, which is checked by comparing the
       * signing_info.apk_digest field in the idsig file with the signer.signed_data.digests.digest
       * field in the input APK file.
       */
      @Override public void createOrUpdateIdsigFile(android.os.ParcelFileDescriptor inputFd, android.os.ParcelFileDescriptor idsigFd) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(inputFd, 0);
          _data.writeTypedObject(idsigFd, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_createOrUpdateIdsigFile, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Get a list of all currently running VMs. This method is only intended for debug purposes,
       * and as such is only permitted from the shell user.
       */
      @Override public android.system.virtualizationservice.VirtualMachineDebugInfo[] debugListVms() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.system.virtualizationservice.VirtualMachineDebugInfo[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_debugListVms, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.system.virtualizationservice.VirtualMachineDebugInfo.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_createVm = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_initializeWritablePartition = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_createOrUpdateIdsigFile = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_debugListVms = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
  }
  public static final java.lang.String DESCRIPTOR = "android$system$virtualizationservice$IVirtualizationService".replace('$', '.');
  /**
   * Create the VM with the given config file, and return a handle to it ready to start it. If
   * `consoleFd` is provided then console output from the VM will be sent to it. If `osLogFd` is
   * provided then the OS-level logs will be sent to it. `osLogFd` is supported only when the OS
   * running in the VM has the logging system. In case of Microdroid, the logging system is logd.
   */
  public android.system.virtualizationservice.IVirtualMachine createVm(android.system.virtualizationservice.VirtualMachineConfig config, android.os.ParcelFileDescriptor consoleFd, android.os.ParcelFileDescriptor osLogFd) throws android.os.RemoteException;
  /**
   * Initialise an empty partition image of the given size to be used as a writable partition.
   * 
   * The file must be open with both read and write permissions, and should be a new empty file.
   */
  public void initializeWritablePartition(android.os.ParcelFileDescriptor imageFd, long sizeBytes, int type) throws android.os.RemoteException;
  /**
   * Create or update an idsig file that digests the given APK file. The idsig file follows the
   * idsig format that is defined by the APK Signature Scheme V4. The idsig file is not updated
   * when it is up to date with the input file, which is checked by comparing the
   * signing_info.apk_digest field in the idsig file with the signer.signed_data.digests.digest
   * field in the input APK file.
   */
  public void createOrUpdateIdsigFile(android.os.ParcelFileDescriptor inputFd, android.os.ParcelFileDescriptor idsigFd) throws android.os.RemoteException;
  /**
   * Get a list of all currently running VMs. This method is only intended for debug purposes,
   * and as such is only permitted from the shell user.
   */
  public android.system.virtualizationservice.VirtualMachineDebugInfo[] debugListVms() throws android.os.RemoteException;
}
