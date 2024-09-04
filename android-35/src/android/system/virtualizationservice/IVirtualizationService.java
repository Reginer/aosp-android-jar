/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/packages/modules/Virtualization/virtualizationservice/aidl/android.system.virtualizationcommon_interface/preprocessed.aidl --ninja -d out/soong/.intermediates/packages/modules/Virtualization/virtualizationservice/aidl/android.system.virtualizationservice-java-source/gen/android/system/virtualizationservice/IVirtualizationService.java.d -o out/soong/.intermediates/packages/modules/Virtualization/virtualizationservice/aidl/android.system.virtualizationservice-java-source/gen -Npackages/modules/Virtualization/virtualizationservice/aidl packages/modules/Virtualization/virtualizationservice/aidl/android/system/virtualizationservice/IVirtualizationService.aidl
 */
package android.system.virtualizationservice;
public interface IVirtualizationService extends android.os.IInterface
{
  /** Default implementation for IVirtualizationService. */
  public static class Default implements android.system.virtualizationservice.IVirtualizationService
  {
    /**
     * Create the VM with the given config file, and return a handle to it ready to start it. If
     * `consoleOutFd` is provided then console output from the VM will be sent to it. If
     * `consoleInFd` is provided then console input to the VM will be read from it. If `osLogFd` is
     * provided then the OS-level logs will be sent to it. `osLogFd` is supported only when the OS
     * running in the VM has the logging system. In case of Microdroid, the logging system is logd.
     */
    @Override public android.system.virtualizationservice.IVirtualMachine createVm(android.system.virtualizationservice.VirtualMachineConfig config, android.os.ParcelFileDescriptor consoleOutFd, android.os.ParcelFileDescriptor consoleInFd, android.os.ParcelFileDescriptor osLogFd) throws android.os.RemoteException
    {
      return null;
    }
    /** Allocate an instance_id to the (newly created) VM. */
    @Override public byte[] allocateInstanceId() throws android.os.RemoteException
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
    /** Get a list of assignable device types. */
    @Override public android.system.virtualizationservice.AssignableDevice[] getAssignableDevices() throws android.os.RemoteException
    {
      return null;
    }
    /** Get a list of supported OSes. */
    @Override public java.lang.String[] getSupportedOSList() throws android.os.RemoteException
    {
      return null;
    }
    /** Returns whether given feature is enabled. */
    @Override public boolean isFeatureEnabled(java.lang.String feature) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * Provisions a key pair for the VM attestation testing, a fake certificate will be
     * associated to the fake key pair when the VM requests attestation in testing mode.
     */
    @Override public void enableTestAttestation() throws android.os.RemoteException
    {
    }
    /** Returns {@code true} if the pVM remote attestation feature is supported */
    @Override public boolean isRemoteAttestationSupported() throws android.os.RemoteException
    {
      return false;
    }
    /**
     * Check if Updatable VM feature is supported by AVF. Updatable VM allows secrets and data of
     * a VM instance to be accessible even after updates of boot images and apks.
     * For more info see packages/modules/Virtualization/docs/updatable_vm.md
     */
    @Override public boolean isUpdatableVmSupported() throws android.os.RemoteException
    {
      return false;
    }
    /**
     * Notification that state associated with a VM should be removed.
     * 
     * @param instanceId The ID for the VM.
     */
    @Override public void removeVmInstance(byte[] instanceId) throws android.os.RemoteException
    {
    }
    /**
     * Notification that ownership of a VM has been claimed by the caller.  Note that no permission
     * checks (with respect to the previous owner) are performed.
     * 
     * @param instanceId The ID for the VM.
     */
    @Override public void claimVmInstance(byte[] instanceId) throws android.os.RemoteException
    {
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
    @SuppressWarnings("this-escape")
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
      if (code == INTERFACE_TRANSACTION) {
        reply.writeString(descriptor);
        return true;
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
          android.os.ParcelFileDescriptor _arg3;
          _arg3 = data.readTypedObject(android.os.ParcelFileDescriptor.CREATOR);
          data.enforceNoDataAvail();
          android.system.virtualizationservice.IVirtualMachine _result = this.createVm(_arg0, _arg1, _arg2, _arg3);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_allocateInstanceId:
        {
          byte[] _result = this.allocateInstanceId();
          reply.writeNoException();
          reply.writeFixedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE, 64);
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
        case TRANSACTION_getAssignableDevices:
        {
          android.system.virtualizationservice.AssignableDevice[] _result = this.getAssignableDevices();
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getSupportedOSList:
        {
          java.lang.String[] _result = this.getSupportedOSList();
          reply.writeNoException();
          reply.writeStringArray(_result);
          break;
        }
        case TRANSACTION_isFeatureEnabled:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          data.enforceNoDataAvail();
          boolean _result = this.isFeatureEnabled(_arg0);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_enableTestAttestation:
        {
          this.enableTestAttestation();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_isRemoteAttestationSupported:
        {
          boolean _result = this.isRemoteAttestationSupported();
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_isUpdatableVmSupported:
        {
          boolean _result = this.isUpdatableVmSupported();
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_removeVmInstance:
        {
          byte[] _arg0;
          _arg0 = data.createFixedArray(byte[].class, 64);
          data.enforceNoDataAvail();
          this.removeVmInstance(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_claimVmInstance:
        {
          byte[] _arg0;
          _arg0 = data.createFixedArray(byte[].class, 64);
          data.enforceNoDataAvail();
          this.claimVmInstance(_arg0);
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
       * `consoleOutFd` is provided then console output from the VM will be sent to it. If
       * `consoleInFd` is provided then console input to the VM will be read from it. If `osLogFd` is
       * provided then the OS-level logs will be sent to it. `osLogFd` is supported only when the OS
       * running in the VM has the logging system. In case of Microdroid, the logging system is logd.
       */
      @Override public android.system.virtualizationservice.IVirtualMachine createVm(android.system.virtualizationservice.VirtualMachineConfig config, android.os.ParcelFileDescriptor consoleOutFd, android.os.ParcelFileDescriptor consoleInFd, android.os.ParcelFileDescriptor osLogFd) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.system.virtualizationservice.IVirtualMachine _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(config, 0);
          _data.writeTypedObject(consoleOutFd, 0);
          _data.writeTypedObject(consoleInFd, 0);
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
      /** Allocate an instance_id to the (newly created) VM. */
      @Override public byte[] allocateInstanceId() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        byte[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_allocateInstanceId, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createFixedArray(byte[].class, 64);
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
      /** Get a list of assignable device types. */
      @Override public android.system.virtualizationservice.AssignableDevice[] getAssignableDevices() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.system.virtualizationservice.AssignableDevice[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getAssignableDevices, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.system.virtualizationservice.AssignableDevice.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Get a list of supported OSes. */
      @Override public java.lang.String[] getSupportedOSList() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSupportedOSList, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createStringArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Returns whether given feature is enabled. */
      @Override public boolean isFeatureEnabled(java.lang.String feature) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(feature);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isFeatureEnabled, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readBoolean();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Provisions a key pair for the VM attestation testing, a fake certificate will be
       * associated to the fake key pair when the VM requests attestation in testing mode.
       */
      @Override public void enableTestAttestation() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_enableTestAttestation, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Returns {@code true} if the pVM remote attestation feature is supported */
      @Override public boolean isRemoteAttestationSupported() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isRemoteAttestationSupported, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readBoolean();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Check if Updatable VM feature is supported by AVF. Updatable VM allows secrets and data of
       * a VM instance to be accessible even after updates of boot images and apks.
       * For more info see packages/modules/Virtualization/docs/updatable_vm.md
       */
      @Override public boolean isUpdatableVmSupported() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isUpdatableVmSupported, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readBoolean();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Notification that state associated with a VM should be removed.
       * 
       * @param instanceId The ID for the VM.
       */
      @Override public void removeVmInstance(byte[] instanceId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFixedArray(instanceId, 0, 64);
          boolean _status = mRemote.transact(Stub.TRANSACTION_removeVmInstance, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Notification that ownership of a VM has been claimed by the caller.  Note that no permission
       * checks (with respect to the previous owner) are performed.
       * 
       * @param instanceId The ID for the VM.
       */
      @Override public void claimVmInstance(byte[] instanceId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFixedArray(instanceId, 0, 64);
          boolean _status = mRemote.transact(Stub.TRANSACTION_claimVmInstance, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_createVm = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_allocateInstanceId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_initializeWritablePartition = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_createOrUpdateIdsigFile = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_debugListVms = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getAssignableDevices = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getSupportedOSList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_isFeatureEnabled = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_enableTestAttestation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_isRemoteAttestationSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_isUpdatableVmSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_removeVmInstance = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_claimVmInstance = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android.system.virtualizationservice.IVirtualizationService";
  public static final String FEATURE_DICE_CHANGES = "com.android.kvm.DICE_CHANGES";
  public static final String FEATURE_LLPVM_CHANGES = "com.android.kvm.LLPVM_CHANGES";
  public static final String FEATURE_MULTI_TENANT = "com.android.kvm.MULTI_TENANT";
  public static final String FEATURE_REMOTE_ATTESTATION = "com.android.kvm.REMOTE_ATTESTATION";
  public static final String FEATURE_VENDOR_MODULES = "com.android.kvm.VENDOR_MODULES";
  /**
   * Create the VM with the given config file, and return a handle to it ready to start it. If
   * `consoleOutFd` is provided then console output from the VM will be sent to it. If
   * `consoleInFd` is provided then console input to the VM will be read from it. If `osLogFd` is
   * provided then the OS-level logs will be sent to it. `osLogFd` is supported only when the OS
   * running in the VM has the logging system. In case of Microdroid, the logging system is logd.
   */
  public android.system.virtualizationservice.IVirtualMachine createVm(android.system.virtualizationservice.VirtualMachineConfig config, android.os.ParcelFileDescriptor consoleOutFd, android.os.ParcelFileDescriptor consoleInFd, android.os.ParcelFileDescriptor osLogFd) throws android.os.RemoteException;
  /** Allocate an instance_id to the (newly created) VM. */
  public byte[] allocateInstanceId() throws android.os.RemoteException;
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
  /** Get a list of assignable device types. */
  public android.system.virtualizationservice.AssignableDevice[] getAssignableDevices() throws android.os.RemoteException;
  /** Get a list of supported OSes. */
  public java.lang.String[] getSupportedOSList() throws android.os.RemoteException;
  /** Returns whether given feature is enabled. */
  public boolean isFeatureEnabled(java.lang.String feature) throws android.os.RemoteException;
  /**
   * Provisions a key pair for the VM attestation testing, a fake certificate will be
   * associated to the fake key pair when the VM requests attestation in testing mode.
   */
  public void enableTestAttestation() throws android.os.RemoteException;
  /** Returns {@code true} if the pVM remote attestation feature is supported */
  public boolean isRemoteAttestationSupported() throws android.os.RemoteException;
  /**
   * Check if Updatable VM feature is supported by AVF. Updatable VM allows secrets and data of
   * a VM instance to be accessible even after updates of boot images and apks.
   * For more info see packages/modules/Virtualization/docs/updatable_vm.md
   */
  public boolean isUpdatableVmSupported() throws android.os.RemoteException;
  /**
   * Notification that state associated with a VM should be removed.
   * 
   * @param instanceId The ID for the VM.
   */
  public void removeVmInstance(byte[] instanceId) throws android.os.RemoteException;
  /**
   * Notification that ownership of a VM has been claimed by the caller.  Note that no permission
   * checks (with respect to the previous owner) are performed.
   * 
   * @param instanceId The ID for the VM.
   */
  public void claimVmInstance(byte[] instanceId) throws android.os.RemoteException;
}
