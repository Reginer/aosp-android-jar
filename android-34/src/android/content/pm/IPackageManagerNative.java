/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.content.pm;
/**
 * Parallel implementation of certain {@link PackageManager} APIs that need to
 * be exposed to native code.
 * <p>These APIs are a parallel definition to the APIs in PackageManager, so,
 * they can technically diverge. However, it's good practice to keep these
 * APIs in sync with each other.
 * <p>Because these APIs are exposed to native code, it's possible they will
 * be exposed to privileged components [such as UID 0]. Care should be taken
 * to avoid exposing potential security holes for methods where permission
 * checks are bypassed based upon UID alone.
 * 
 * @hide
 */
public interface IPackageManagerNative extends android.os.IInterface
{
  /** Default implementation for IPackageManagerNative. */
  public static class Default implements android.content.pm.IPackageManagerNative
  {
    /**
     * Returns a set of names for the given UIDs.
     * IMPORTANT: Unlike the Java version of this API, unknown UIDs are
     * not represented by 'null's. Instead, they are represented by empty
     * strings.
     */
    @Override public java.lang.String[] getNamesForUids(int[] uids) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Returns the name of the installer (a package) which installed the named
     * package. Preloaded packages return the string "preload". Sideloaded packages
     * return an empty string. Unknown or unknowable are returned as empty strings.
     */
    @Override public java.lang.String getInstallerForPackage(java.lang.String packageName) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Returns the version code of the named package.
     * Unknown or unknowable versions are returned as 0.
     */
    @Override public long getVersionCodeForPackage(java.lang.String packageName) throws android.os.RemoteException
    {
      return 0L;
    }
    /**
     * Return if each app, identified by its package name allows its audio to be recorded.
     * Unknown packages are mapped to false.
     */
    @Override public boolean[] isAudioPlaybackCaptureAllowed(java.lang.String[] packageNames) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Returns a set of bitflags about package location.
     * LOCATION_SYSTEM: getApplicationInfo(packageName).isSystemApp()
     * LOCATION_VENDOR: getApplicationInfo(packageName).isVendor()
     * LOCATION_PRODUCT: getApplicationInfo(packageName).isProduct()
     */
    @Override public int getLocationFlags(java.lang.String packageName) throws android.os.RemoteException
    {
      return 0;
    }
    /**
     * Returns the target SDK version for the given package.
     * Unknown packages will cause the call to fail. The caller must check the
     * returned Status before using the result of this function.
     */
    @Override public int getTargetSdkVersionForPackage(java.lang.String packageName) throws android.os.RemoteException
    {
      return 0;
    }
    /**
     * Returns the name of module metadata package, or empty string if device doesn't have such
     * package.
     */
    @Override public java.lang.String getModuleMetadataPackageName() throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Returns true if the package has the SHA 256 version of the signing certificate.
     * @see PackageManager#hasSigningCertificate(String, byte[], int), where type
     * has been set to {@link PackageManager#CERT_INPUT_SHA256}.
     */
    @Override public boolean hasSha256SigningCertificate(java.lang.String packageName, byte[] certificate) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * Returns the debug flag for the given package.
     * Unknown packages will cause the call to fail.
     */
    @Override public boolean isPackageDebuggable(java.lang.String packageName) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * Check whether the given feature name and version is one of the available
     * features as returned by {@link PackageManager#getSystemAvailableFeatures()}. Since
     * features are defined to always be backwards compatible, this returns true
     * if the available feature version is greater than or equal to the
     * requested version.
     */
    @Override public boolean hasSystemFeature(java.lang.String featureName, int version) throws android.os.RemoteException
    {
      return false;
    }
    /** Register a observer for change in set of staged APEX ready for installation */
    @Override public void registerStagedApexObserver(android.content.pm.IStagedApexObserver observer) throws android.os.RemoteException
    {
    }
    /**
     * Unregister an existing staged apex observer.
     * This does nothing if this observer was not already registered.
     */
    @Override public void unregisterStagedApexObserver(android.content.pm.IStagedApexObserver observer) throws android.os.RemoteException
    {
    }
    /** Get APEX module names of all APEX that are staged ready for installation */
    @Override public java.lang.String[] getStagedApexModuleNames() throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Get information of APEX which is staged ready for installation.
     * Returns null if no such APEX is found.
     */
    @Override public android.content.pm.StagedApexInfo getStagedApexInfo(java.lang.String moduleName) throws android.os.RemoteException
    {
      return null;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.content.pm.IPackageManagerNative
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.content.pm.IPackageManagerNative interface,
     * generating a proxy if needed.
     */
    public static android.content.pm.IPackageManagerNative asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.content.pm.IPackageManagerNative))) {
        return ((android.content.pm.IPackageManagerNative)iin);
      }
      return new android.content.pm.IPackageManagerNative.Stub.Proxy(obj);
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
        case TRANSACTION_getNamesForUids:
        {
          int[] _arg0;
          _arg0 = data.createIntArray();
          data.enforceNoDataAvail();
          java.lang.String[] _result = this.getNamesForUids(_arg0);
          reply.writeNoException();
          reply.writeStringArray(_result);
          break;
        }
        case TRANSACTION_getInstallerForPackage:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          data.enforceNoDataAvail();
          java.lang.String _result = this.getInstallerForPackage(_arg0);
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        case TRANSACTION_getVersionCodeForPackage:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          data.enforceNoDataAvail();
          long _result = this.getVersionCodeForPackage(_arg0);
          reply.writeNoException();
          reply.writeLong(_result);
          break;
        }
        case TRANSACTION_isAudioPlaybackCaptureAllowed:
        {
          java.lang.String[] _arg0;
          _arg0 = data.createStringArray();
          data.enforceNoDataAvail();
          boolean[] _result = this.isAudioPlaybackCaptureAllowed(_arg0);
          reply.writeNoException();
          reply.writeBooleanArray(_result);
          break;
        }
        case TRANSACTION_getLocationFlags:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          data.enforceNoDataAvail();
          int _result = this.getLocationFlags(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getTargetSdkVersionForPackage:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          data.enforceNoDataAvail();
          int _result = this.getTargetSdkVersionForPackage(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getModuleMetadataPackageName:
        {
          java.lang.String _result = this.getModuleMetadataPackageName();
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        case TRANSACTION_hasSha256SigningCertificate:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          byte[] _arg1;
          _arg1 = data.createByteArray();
          data.enforceNoDataAvail();
          boolean _result = this.hasSha256SigningCertificate(_arg0, _arg1);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_isPackageDebuggable:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          data.enforceNoDataAvail();
          boolean _result = this.isPackageDebuggable(_arg0);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_hasSystemFeature:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          boolean _result = this.hasSystemFeature(_arg0, _arg1);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_registerStagedApexObserver:
        {
          android.content.pm.IStagedApexObserver _arg0;
          _arg0 = android.content.pm.IStagedApexObserver.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.registerStagedApexObserver(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_unregisterStagedApexObserver:
        {
          android.content.pm.IStagedApexObserver _arg0;
          _arg0 = android.content.pm.IStagedApexObserver.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.unregisterStagedApexObserver(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getStagedApexModuleNames:
        {
          java.lang.String[] _result = this.getStagedApexModuleNames();
          reply.writeNoException();
          reply.writeStringArray(_result);
          break;
        }
        case TRANSACTION_getStagedApexInfo:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          data.enforceNoDataAvail();
          android.content.pm.StagedApexInfo _result = this.getStagedApexInfo(_arg0);
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
    private static class Proxy implements android.content.pm.IPackageManagerNative
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
       * Returns a set of names for the given UIDs.
       * IMPORTANT: Unlike the Java version of this API, unknown UIDs are
       * not represented by 'null's. Instead, they are represented by empty
       * strings.
       */
      @Override public java.lang.String[] getNamesForUids(int[] uids) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeIntArray(uids);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getNamesForUids, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createStringArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Returns the name of the installer (a package) which installed the named
       * package. Preloaded packages return the string "preload". Sideloaded packages
       * return an empty string. Unknown or unknowable are returned as empty strings.
       */
      @Override public java.lang.String getInstallerForPackage(java.lang.String packageName) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getInstallerForPackage, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Returns the version code of the named package.
       * Unknown or unknowable versions are returned as 0.
       */
      @Override public long getVersionCodeForPackage(java.lang.String packageName) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        long _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getVersionCodeForPackage, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readLong();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Return if each app, identified by its package name allows its audio to be recorded.
       * Unknown packages are mapped to false.
       */
      @Override public boolean[] isAudioPlaybackCaptureAllowed(java.lang.String[] packageNames) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStringArray(packageNames);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isAudioPlaybackCaptureAllowed, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createBooleanArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Returns a set of bitflags about package location.
       * LOCATION_SYSTEM: getApplicationInfo(packageName).isSystemApp()
       * LOCATION_VENDOR: getApplicationInfo(packageName).isVendor()
       * LOCATION_PRODUCT: getApplicationInfo(packageName).isProduct()
       */
      @Override public int getLocationFlags(java.lang.String packageName) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getLocationFlags, _data, _reply, 0);
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
       * Returns the target SDK version for the given package.
       * Unknown packages will cause the call to fail. The caller must check the
       * returned Status before using the result of this function.
       */
      @Override public int getTargetSdkVersionForPackage(java.lang.String packageName) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getTargetSdkVersionForPackage, _data, _reply, 0);
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
       * Returns the name of module metadata package, or empty string if device doesn't have such
       * package.
       */
      @Override public java.lang.String getModuleMetadataPackageName() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getModuleMetadataPackageName, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Returns true if the package has the SHA 256 version of the signing certificate.
       * @see PackageManager#hasSigningCertificate(String, byte[], int), where type
       * has been set to {@link PackageManager#CERT_INPUT_SHA256}.
       */
      @Override public boolean hasSha256SigningCertificate(java.lang.String packageName, byte[] certificate) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          _data.writeByteArray(certificate);
          boolean _status = mRemote.transact(Stub.TRANSACTION_hasSha256SigningCertificate, _data, _reply, 0);
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
       * Returns the debug flag for the given package.
       * Unknown packages will cause the call to fail.
       */
      @Override public boolean isPackageDebuggable(java.lang.String packageName) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isPackageDebuggable, _data, _reply, 0);
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
       * Check whether the given feature name and version is one of the available
       * features as returned by {@link PackageManager#getSystemAvailableFeatures()}. Since
       * features are defined to always be backwards compatible, this returns true
       * if the available feature version is greater than or equal to the
       * requested version.
       */
      @Override public boolean hasSystemFeature(java.lang.String featureName, int version) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(featureName);
          _data.writeInt(version);
          boolean _status = mRemote.transact(Stub.TRANSACTION_hasSystemFeature, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readBoolean();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Register a observer for change in set of staged APEX ready for installation */
      @Override public void registerStagedApexObserver(android.content.pm.IStagedApexObserver observer) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(observer);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerStagedApexObserver, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Unregister an existing staged apex observer.
       * This does nothing if this observer was not already registered.
       */
      @Override public void unregisterStagedApexObserver(android.content.pm.IStagedApexObserver observer) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(observer);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterStagedApexObserver, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Get APEX module names of all APEX that are staged ready for installation */
      @Override public java.lang.String[] getStagedApexModuleNames() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getStagedApexModuleNames, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createStringArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Get information of APEX which is staged ready for installation.
       * Returns null if no such APEX is found.
       */
      @Override public android.content.pm.StagedApexInfo getStagedApexInfo(java.lang.String moduleName) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.content.pm.StagedApexInfo _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(moduleName);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getStagedApexInfo, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readTypedObject(android.content.pm.StagedApexInfo.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    public static final java.lang.String DESCRIPTOR = "android$content$pm$IPackageManagerNative".replace('$', '.');
    static final int TRANSACTION_getNamesForUids = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_getInstallerForPackage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_getVersionCodeForPackage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_isAudioPlaybackCaptureAllowed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getLocationFlags = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getTargetSdkVersionForPackage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getModuleMetadataPackageName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_hasSha256SigningCertificate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_isPackageDebuggable = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_hasSystemFeature = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_registerStagedApexObserver = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_unregisterStagedApexObserver = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_getStagedApexModuleNames = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_getStagedApexInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
  }
  /** ApplicationInfo.isSystemApp() == true */
  public static final int LOCATION_SYSTEM = 1;
  /** ApplicationInfo.isVendor() == true */
  public static final int LOCATION_VENDOR = 2;
  /** ApplicationInfo.isProduct() == true */
  public static final int LOCATION_PRODUCT = 4;
  /**
   * Returns a set of names for the given UIDs.
   * IMPORTANT: Unlike the Java version of this API, unknown UIDs are
   * not represented by 'null's. Instead, they are represented by empty
   * strings.
   */
  public java.lang.String[] getNamesForUids(int[] uids) throws android.os.RemoteException;
  /**
   * Returns the name of the installer (a package) which installed the named
   * package. Preloaded packages return the string "preload". Sideloaded packages
   * return an empty string. Unknown or unknowable are returned as empty strings.
   */
  public java.lang.String getInstallerForPackage(java.lang.String packageName) throws android.os.RemoteException;
  /**
   * Returns the version code of the named package.
   * Unknown or unknowable versions are returned as 0.
   */
  public long getVersionCodeForPackage(java.lang.String packageName) throws android.os.RemoteException;
  /**
   * Return if each app, identified by its package name allows its audio to be recorded.
   * Unknown packages are mapped to false.
   */
  public boolean[] isAudioPlaybackCaptureAllowed(java.lang.String[] packageNames) throws android.os.RemoteException;
  /**
   * Returns a set of bitflags about package location.
   * LOCATION_SYSTEM: getApplicationInfo(packageName).isSystemApp()
   * LOCATION_VENDOR: getApplicationInfo(packageName).isVendor()
   * LOCATION_PRODUCT: getApplicationInfo(packageName).isProduct()
   */
  public int getLocationFlags(java.lang.String packageName) throws android.os.RemoteException;
  /**
   * Returns the target SDK version for the given package.
   * Unknown packages will cause the call to fail. The caller must check the
   * returned Status before using the result of this function.
   */
  public int getTargetSdkVersionForPackage(java.lang.String packageName) throws android.os.RemoteException;
  /**
   * Returns the name of module metadata package, or empty string if device doesn't have such
   * package.
   */
  public java.lang.String getModuleMetadataPackageName() throws android.os.RemoteException;
  /**
   * Returns true if the package has the SHA 256 version of the signing certificate.
   * @see PackageManager#hasSigningCertificate(String, byte[], int), where type
   * has been set to {@link PackageManager#CERT_INPUT_SHA256}.
   */
  public boolean hasSha256SigningCertificate(java.lang.String packageName, byte[] certificate) throws android.os.RemoteException;
  /**
   * Returns the debug flag for the given package.
   * Unknown packages will cause the call to fail.
   */
  public boolean isPackageDebuggable(java.lang.String packageName) throws android.os.RemoteException;
  /**
   * Check whether the given feature name and version is one of the available
   * features as returned by {@link PackageManager#getSystemAvailableFeatures()}. Since
   * features are defined to always be backwards compatible, this returns true
   * if the available feature version is greater than or equal to the
   * requested version.
   */
  public boolean hasSystemFeature(java.lang.String featureName, int version) throws android.os.RemoteException;
  /** Register a observer for change in set of staged APEX ready for installation */
  public void registerStagedApexObserver(android.content.pm.IStagedApexObserver observer) throws android.os.RemoteException;
  /**
   * Unregister an existing staged apex observer.
   * This does nothing if this observer was not already registered.
   */
  public void unregisterStagedApexObserver(android.content.pm.IStagedApexObserver observer) throws android.os.RemoteException;
  /** Get APEX module names of all APEX that are staged ready for installation */
  public java.lang.String[] getStagedApexModuleNames() throws android.os.RemoteException;
  /**
   * Get information of APEX which is staged ready for installation.
   * Returns null if no such APEX is found.
   */
  public android.content.pm.StagedApexInfo getStagedApexInfo(java.lang.String moduleName) throws android.os.RemoteException;
}
