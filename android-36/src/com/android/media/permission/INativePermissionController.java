/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current --ninja -d out/soong/.intermediates/frameworks/av/audio-permission-aidl-java-source/gen/com/android/media/permission/INativePermissionController.java.d -o out/soong/.intermediates/frameworks/av/audio-permission-aidl-java-source/gen -Nframeworks/av/aidl frameworks/av/aidl/com/android/media/permission/INativePermissionController.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package com.android.media.permission;
/**
 * This interface is used by system_server to communicate permission information
 * downwards towards native services.
 * {@hide}
 */
public interface INativePermissionController extends android.os.IInterface
{
  /** Default implementation for INativePermissionController. */
  public static class Default implements com.android.media.permission.INativePermissionController
  {
    /** Initialize app-ids and their corresponding packages, to be used for package validation. */
    @Override public void populatePackagesForUids(java.util.List<com.android.media.permission.UidPackageState> initialPackageStates) throws android.os.RemoteException
    {
    }
    /**
     * Replace or populate the list of packages associated with a given uid.
     * If the list is empty, the package no longer exists.
     */
    @Override public void updatePackagesForUid(com.android.media.permission.UidPackageState newPackageState) throws android.os.RemoteException
    {
    }
    /**
     * Populate or replace the list of uids which holds a particular permission.
     * Runtime permissions will need additional checks, and should not use the cache as-is.
     * Not virtual device aware.
     * Is is possible for updates to the permission state to be delayed during high traffic.
     * @param perm - Enum representing the permission for which holders are being supplied
     * @param uids - Uids (not app-ids) which hold the permission. Should be sorted
     */
    @Override public void populatePermissionState(byte perm, int[] uids) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.android.media.permission.INativePermissionController
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.android.media.permission.INativePermissionController interface,
     * generating a proxy if needed.
     */
    public static com.android.media.permission.INativePermissionController asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.android.media.permission.INativePermissionController))) {
        return ((com.android.media.permission.INativePermissionController)iin);
      }
      return new com.android.media.permission.INativePermissionController.Stub.Proxy(obj);
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
        case TRANSACTION_populatePackagesForUids:
        {
          java.util.List<com.android.media.permission.UidPackageState> _arg0;
          _arg0 = data.createTypedArrayList(com.android.media.permission.UidPackageState.CREATOR);
          data.enforceNoDataAvail();
          this.populatePackagesForUids(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_updatePackagesForUid:
        {
          com.android.media.permission.UidPackageState _arg0;
          _arg0 = data.readTypedObject(com.android.media.permission.UidPackageState.CREATOR);
          data.enforceNoDataAvail();
          this.updatePackagesForUid(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_populatePermissionState:
        {
          byte _arg0;
          _arg0 = data.readByte();
          int[] _arg1;
          _arg1 = data.createIntArray();
          data.enforceNoDataAvail();
          this.populatePermissionState(_arg0, _arg1);
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
    private static class Proxy implements com.android.media.permission.INativePermissionController
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
      /** Initialize app-ids and their corresponding packages, to be used for package validation. */
      @Override public void populatePackagesForUids(java.util.List<com.android.media.permission.UidPackageState> initialPackageStates) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedList(initialPackageStates, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_populatePackagesForUids, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Replace or populate the list of packages associated with a given uid.
       * If the list is empty, the package no longer exists.
       */
      @Override public void updatePackagesForUid(com.android.media.permission.UidPackageState newPackageState) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(newPackageState, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_updatePackagesForUid, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Populate or replace the list of uids which holds a particular permission.
       * Runtime permissions will need additional checks, and should not use the cache as-is.
       * Not virtual device aware.
       * Is is possible for updates to the permission state to be delayed during high traffic.
       * @param perm - Enum representing the permission for which holders are being supplied
       * @param uids - Uids (not app-ids) which hold the permission. Should be sorted
       */
      @Override public void populatePermissionState(byte perm, int[] uids) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(perm);
          _data.writeIntArray(uids);
          boolean _status = mRemote.transact(Stub.TRANSACTION_populatePermissionState, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_populatePackagesForUids = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_updatePackagesForUid = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_populatePermissionState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "com.android.media.permission.INativePermissionController";
  /** Initialize app-ids and their corresponding packages, to be used for package validation. */
  public void populatePackagesForUids(java.util.List<com.android.media.permission.UidPackageState> initialPackageStates) throws android.os.RemoteException;
  /**
   * Replace or populate the list of packages associated with a given uid.
   * If the list is empty, the package no longer exists.
   */
  public void updatePackagesForUid(com.android.media.permission.UidPackageState newPackageState) throws android.os.RemoteException;
  /**
   * Populate or replace the list of uids which holds a particular permission.
   * Runtime permissions will need additional checks, and should not use the cache as-is.
   * Not virtual device aware.
   * Is is possible for updates to the permission state to be delayed during high traffic.
   * @param perm - Enum representing the permission for which holders are being supplied
   * @param uids - Uids (not app-ids) which hold the permission. Should be sorted
   */
  public void populatePermissionState(byte perm, int[] uids) throws android.os.RemoteException;
}
