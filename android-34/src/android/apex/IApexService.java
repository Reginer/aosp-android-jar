/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.apex;
public interface IApexService extends android.os.IInterface
{
  /** Default implementation for IApexService. */
  public static class Default implements android.apex.IApexService
  {
    @Override public void submitStagedSession(android.apex.ApexSessionParams params, android.apex.ApexInfoList packages) throws android.os.RemoteException
    {
    }
    @Override public void markStagedSessionReady(int session_id) throws android.os.RemoteException
    {
    }
    @Override public void markStagedSessionSuccessful(int session_id) throws android.os.RemoteException
    {
    }
    @Override public android.apex.ApexSessionInfo[] getSessions() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.apex.ApexSessionInfo getStagedSessionInfo(int session_id) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.apex.ApexInfo[] getStagedApexInfos(android.apex.ApexSessionParams params) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.apex.ApexInfo[] getActivePackages() throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.apex.ApexInfo[] getAllPackages() throws android.os.RemoteException
    {
      return null;
    }
    @Override public void abortStagedSession(int session_id) throws android.os.RemoteException
    {
    }
    @Override public void revertActiveSessions() throws android.os.RemoteException
    {
    }
    /**
     * Copies the CE apex data directory for the given user to the backup
     * location.
     */
    @Override public void snapshotCeData(int user_id, int rollback_id, java.lang.String apex_name) throws android.os.RemoteException
    {
    }
    /**
     * Restores the snapshot of the CE apex data directory for the given user and
     * apex. Note the snapshot will be deleted after restoration succeeded.
     */
    @Override public void restoreCeData(int user_id, int rollback_id, java.lang.String apex_name) throws android.os.RemoteException
    {
    }
    /** Deletes device-encrypted snapshots for the given rollback id. */
    @Override public void destroyDeSnapshots(int rollback_id) throws android.os.RemoteException
    {
    }
    /** Deletes credential-encrypted snapshots for the given user, for the given rollback id. */
    @Override public void destroyCeSnapshots(int user_id, int rollback_id) throws android.os.RemoteException
    {
    }
    /**
     * Deletes all credential-encrypted snapshots for the given user, except for
     * those listed in retain_rollback_ids.
     */
    @Override public void destroyCeSnapshotsNotSpecified(int user_id, int[] retain_rollback_ids) throws android.os.RemoteException
    {
    }
    @Override public void unstagePackages(java.util.List<java.lang.String> active_package_paths) throws android.os.RemoteException
    {
    }
    /**
     * Returns the active package corresponding to |package_name| and null
     * if none exists.
     */
    @Override public android.apex.ApexInfo getActivePackage(java.lang.String package_name) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Not meant for use outside of testing. The call will not be
     * functional on user builds.
     */
    @Override public void stagePackages(java.util.List<java.lang.String> package_tmp_paths) throws android.os.RemoteException
    {
    }
    /**
     * Not meant for use outside of testing. The call will not be
     * functional on user builds.
     */
    @Override public void resumeRevertIfNeeded() throws android.os.RemoteException
    {
    }
    /**
     * Forces apexd to remount all active packages.
     * 
     * This call is mostly useful for speeding up development of APEXes.
     * Instead of going through a full APEX installation that requires a reboot,
     * developers can incorporate this method in much faster `adb sync` based
     * workflow:
     * 
     * 1. adb shell stop
     * 2. adb sync
     * 3. adb shell cmd -w apexservice remountPackages
     * 4. adb shell start
     * 
     * Note, that for an APEX package will be successfully remounted only if
     * there are no alive processes holding a reference to it.
     * 
     * Not meant for use outside of testing. This call will not be functional
     * on user builds. Only root is allowed to call this method.
     */
    @Override public void remountPackages() throws android.os.RemoteException
    {
    }
    /**
     * Forces apexd to recollect pre-installed data from the given |paths|.
     * 
     * Not meant for use outside of testing. This call will not be functional
     * on user builds. Only root is allowed to call this method.
     */
    @Override public void recollectPreinstalledData(java.util.List<java.lang.String> paths) throws android.os.RemoteException
    {
    }
    /**
     * Forces apexd to recollect data apex from the given |path|.
     * 
     * Not meant for use outside of testing. This call will not be functional
     * on user builds. Only root is allowed to call this method.
     */
    @Override public void recollectDataApex(java.lang.String path, java.lang.String decompression_dir) throws android.os.RemoteException
    {
    }
    /** Informs apexd that the boot has completed. */
    @Override public void markBootCompleted() throws android.os.RemoteException
    {
    }
    /**
     * Assuming the provided compressed APEX will be installed on next boot,
     * calculate how much space will be required for decompression
     */
    @Override public long calculateSizeForCompressedApex(android.apex.CompressedApexInfoList compressed_apex_info_list) throws android.os.RemoteException
    {
      return 0L;
    }
    /**
     * Reserve space on /data partition for compressed APEX decompression. Returns error if
     * reservation fails. If empty list is passed, then reserved space is deallocated.
     */
    @Override public void reserveSpaceForCompressedApex(android.apex.CompressedApexInfoList compressed_apex_info_list) throws android.os.RemoteException
    {
    }
    /**
     * Performs a non-staged install of the given APEX.
     * Note: don't confuse this to preInstall and postInstall binder calls which are only used to
     * test corresponding features of APEX packages.
     */
    @Override public android.apex.ApexInfo installAndActivatePackage(java.lang.String packagePath) throws android.os.RemoteException
    {
      return null;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.apex.IApexService
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.apex.IApexService interface,
     * generating a proxy if needed.
     */
    public static android.apex.IApexService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.apex.IApexService))) {
        return ((android.apex.IApexService)iin);
      }
      return new android.apex.IApexService.Stub.Proxy(obj);
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
        case TRANSACTION_submitStagedSession:
        {
          android.apex.ApexSessionParams _arg0;
          _arg0 = data.readTypedObject(android.apex.ApexSessionParams.CREATOR);
          android.apex.ApexInfoList _arg1;
          _arg1 = new android.apex.ApexInfoList();
          this.submitStagedSession(_arg0, _arg1);
          reply.writeNoException();
          reply.writeTypedObject(_arg1, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_markStagedSessionReady:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.markStagedSessionReady(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_markStagedSessionSuccessful:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.markStagedSessionSuccessful(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getSessions:
        {
          android.apex.ApexSessionInfo[] _result = this.getSessions();
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getStagedSessionInfo:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.apex.ApexSessionInfo _result = this.getStagedSessionInfo(_arg0);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getStagedApexInfos:
        {
          android.apex.ApexSessionParams _arg0;
          _arg0 = data.readTypedObject(android.apex.ApexSessionParams.CREATOR);
          android.apex.ApexInfo[] _result = this.getStagedApexInfos(_arg0);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getActivePackages:
        {
          android.apex.ApexInfo[] _result = this.getActivePackages();
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getAllPackages:
        {
          android.apex.ApexInfo[] _result = this.getAllPackages();
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_abortStagedSession:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.abortStagedSession(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_revertActiveSessions:
        {
          this.revertActiveSessions();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_snapshotCeData:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          java.lang.String _arg2;
          _arg2 = data.readString();
          this.snapshotCeData(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_restoreCeData:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          java.lang.String _arg2;
          _arg2 = data.readString();
          this.restoreCeData(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_destroyDeSnapshots:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.destroyDeSnapshots(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_destroyCeSnapshots:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          this.destroyCeSnapshots(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_destroyCeSnapshotsNotSpecified:
        {
          int _arg0;
          _arg0 = data.readInt();
          int[] _arg1;
          _arg1 = data.createIntArray();
          this.destroyCeSnapshotsNotSpecified(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_unstagePackages:
        {
          java.util.List<java.lang.String> _arg0;
          _arg0 = data.createStringArrayList();
          this.unstagePackages(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getActivePackage:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          android.apex.ApexInfo _result = this.getActivePackage(_arg0);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_stagePackages:
        {
          java.util.List<java.lang.String> _arg0;
          _arg0 = data.createStringArrayList();
          this.stagePackages(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_resumeRevertIfNeeded:
        {
          this.resumeRevertIfNeeded();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_remountPackages:
        {
          this.remountPackages();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_recollectPreinstalledData:
        {
          java.util.List<java.lang.String> _arg0;
          _arg0 = data.createStringArrayList();
          this.recollectPreinstalledData(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_recollectDataApex:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          this.recollectDataApex(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_markBootCompleted:
        {
          this.markBootCompleted();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_calculateSizeForCompressedApex:
        {
          android.apex.CompressedApexInfoList _arg0;
          _arg0 = data.readTypedObject(android.apex.CompressedApexInfoList.CREATOR);
          long _result = this.calculateSizeForCompressedApex(_arg0);
          reply.writeNoException();
          reply.writeLong(_result);
          break;
        }
        case TRANSACTION_reserveSpaceForCompressedApex:
        {
          android.apex.CompressedApexInfoList _arg0;
          _arg0 = data.readTypedObject(android.apex.CompressedApexInfoList.CREATOR);
          this.reserveSpaceForCompressedApex(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_installAndActivatePackage:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          android.apex.ApexInfo _result = this.installAndActivatePackage(_arg0);
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
    private static class Proxy implements android.apex.IApexService
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
      @Override public void submitStagedSession(android.apex.ApexSessionParams params, android.apex.ApexInfoList packages) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(params, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_submitStagedSession, _data, _reply, 0);
          _reply.readException();
          if ((0!=_reply.readInt())) {
            packages.readFromParcel(_reply);
          }
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void markStagedSessionReady(int session_id) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(session_id);
          boolean _status = mRemote.transact(Stub.TRANSACTION_markStagedSessionReady, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void markStagedSessionSuccessful(int session_id) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(session_id);
          boolean _status = mRemote.transact(Stub.TRANSACTION_markStagedSessionSuccessful, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public android.apex.ApexSessionInfo[] getSessions() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.apex.ApexSessionInfo[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSessions, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.apex.ApexSessionInfo.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.apex.ApexSessionInfo getStagedSessionInfo(int session_id) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.apex.ApexSessionInfo _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(session_id);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getStagedSessionInfo, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readTypedObject(android.apex.ApexSessionInfo.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.apex.ApexInfo[] getStagedApexInfos(android.apex.ApexSessionParams params) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.apex.ApexInfo[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(params, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getStagedApexInfos, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.apex.ApexInfo.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.apex.ApexInfo[] getActivePackages() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.apex.ApexInfo[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getActivePackages, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.apex.ApexInfo.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.apex.ApexInfo[] getAllPackages() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.apex.ApexInfo[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getAllPackages, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.apex.ApexInfo.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void abortStagedSession(int session_id) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(session_id);
          boolean _status = mRemote.transact(Stub.TRANSACTION_abortStagedSession, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void revertActiveSessions() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_revertActiveSessions, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Copies the CE apex data directory for the given user to the backup
       * location.
       */
      @Override public void snapshotCeData(int user_id, int rollback_id, java.lang.String apex_name) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(user_id);
          _data.writeInt(rollback_id);
          _data.writeString(apex_name);
          boolean _status = mRemote.transact(Stub.TRANSACTION_snapshotCeData, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Restores the snapshot of the CE apex data directory for the given user and
       * apex. Note the snapshot will be deleted after restoration succeeded.
       */
      @Override public void restoreCeData(int user_id, int rollback_id, java.lang.String apex_name) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(user_id);
          _data.writeInt(rollback_id);
          _data.writeString(apex_name);
          boolean _status = mRemote.transact(Stub.TRANSACTION_restoreCeData, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Deletes device-encrypted snapshots for the given rollback id. */
      @Override public void destroyDeSnapshots(int rollback_id) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(rollback_id);
          boolean _status = mRemote.transact(Stub.TRANSACTION_destroyDeSnapshots, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Deletes credential-encrypted snapshots for the given user, for the given rollback id. */
      @Override public void destroyCeSnapshots(int user_id, int rollback_id) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(user_id);
          _data.writeInt(rollback_id);
          boolean _status = mRemote.transact(Stub.TRANSACTION_destroyCeSnapshots, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Deletes all credential-encrypted snapshots for the given user, except for
       * those listed in retain_rollback_ids.
       */
      @Override public void destroyCeSnapshotsNotSpecified(int user_id, int[] retain_rollback_ids) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(user_id);
          _data.writeIntArray(retain_rollback_ids);
          boolean _status = mRemote.transact(Stub.TRANSACTION_destroyCeSnapshotsNotSpecified, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void unstagePackages(java.util.List<java.lang.String> active_package_paths) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStringList(active_package_paths);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unstagePackages, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Returns the active package corresponding to |package_name| and null
       * if none exists.
       */
      @Override public android.apex.ApexInfo getActivePackage(java.lang.String package_name) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.apex.ApexInfo _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(package_name);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getActivePackage, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readTypedObject(android.apex.ApexInfo.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Not meant for use outside of testing. The call will not be
       * functional on user builds.
       */
      @Override public void stagePackages(java.util.List<java.lang.String> package_tmp_paths) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStringList(package_tmp_paths);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stagePackages, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Not meant for use outside of testing. The call will not be
       * functional on user builds.
       */
      @Override public void resumeRevertIfNeeded() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_resumeRevertIfNeeded, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Forces apexd to remount all active packages.
       * 
       * This call is mostly useful for speeding up development of APEXes.
       * Instead of going through a full APEX installation that requires a reboot,
       * developers can incorporate this method in much faster `adb sync` based
       * workflow:
       * 
       * 1. adb shell stop
       * 2. adb sync
       * 3. adb shell cmd -w apexservice remountPackages
       * 4. adb shell start
       * 
       * Note, that for an APEX package will be successfully remounted only if
       * there are no alive processes holding a reference to it.
       * 
       * Not meant for use outside of testing. This call will not be functional
       * on user builds. Only root is allowed to call this method.
       */
      @Override public void remountPackages() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_remountPackages, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Forces apexd to recollect pre-installed data from the given |paths|.
       * 
       * Not meant for use outside of testing. This call will not be functional
       * on user builds. Only root is allowed to call this method.
       */
      @Override public void recollectPreinstalledData(java.util.List<java.lang.String> paths) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStringList(paths);
          boolean _status = mRemote.transact(Stub.TRANSACTION_recollectPreinstalledData, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Forces apexd to recollect data apex from the given |path|.
       * 
       * Not meant for use outside of testing. This call will not be functional
       * on user builds. Only root is allowed to call this method.
       */
      @Override public void recollectDataApex(java.lang.String path, java.lang.String decompression_dir) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(path);
          _data.writeString(decompression_dir);
          boolean _status = mRemote.transact(Stub.TRANSACTION_recollectDataApex, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Informs apexd that the boot has completed. */
      @Override public void markBootCompleted() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_markBootCompleted, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Assuming the provided compressed APEX will be installed on next boot,
       * calculate how much space will be required for decompression
       */
      @Override public long calculateSizeForCompressedApex(android.apex.CompressedApexInfoList compressed_apex_info_list) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        long _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(compressed_apex_info_list, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_calculateSizeForCompressedApex, _data, _reply, 0);
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
       * Reserve space on /data partition for compressed APEX decompression. Returns error if
       * reservation fails. If empty list is passed, then reserved space is deallocated.
       */
      @Override public void reserveSpaceForCompressedApex(android.apex.CompressedApexInfoList compressed_apex_info_list) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(compressed_apex_info_list, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_reserveSpaceForCompressedApex, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Performs a non-staged install of the given APEX.
       * Note: don't confuse this to preInstall and postInstall binder calls which are only used to
       * test corresponding features of APEX packages.
       */
      @Override public android.apex.ApexInfo installAndActivatePackage(java.lang.String packagePath) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.apex.ApexInfo _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packagePath);
          boolean _status = mRemote.transact(Stub.TRANSACTION_installAndActivatePackage, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readTypedObject(android.apex.ApexInfo.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_submitStagedSession = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_markStagedSessionReady = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_markStagedSessionSuccessful = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_getSessions = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getStagedSessionInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getStagedApexInfos = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getActivePackages = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getAllPackages = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_abortStagedSession = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_revertActiveSessions = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_snapshotCeData = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_restoreCeData = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_destroyDeSnapshots = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_destroyCeSnapshots = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_destroyCeSnapshotsNotSpecified = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_unstagePackages = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_getActivePackage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_stagePackages = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_resumeRevertIfNeeded = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_remountPackages = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
    static final int TRANSACTION_recollectPreinstalledData = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
    static final int TRANSACTION_recollectDataApex = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
    static final int TRANSACTION_markBootCompleted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
    static final int TRANSACTION_calculateSizeForCompressedApex = (android.os.IBinder.FIRST_CALL_TRANSACTION + 23);
    static final int TRANSACTION_reserveSpaceForCompressedApex = (android.os.IBinder.FIRST_CALL_TRANSACTION + 24);
    static final int TRANSACTION_installAndActivatePackage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 25);
  }
  public static final java.lang.String DESCRIPTOR = "android$apex$IApexService".replace('$', '.');
  public void submitStagedSession(android.apex.ApexSessionParams params, android.apex.ApexInfoList packages) throws android.os.RemoteException;
  public void markStagedSessionReady(int session_id) throws android.os.RemoteException;
  public void markStagedSessionSuccessful(int session_id) throws android.os.RemoteException;
  public android.apex.ApexSessionInfo[] getSessions() throws android.os.RemoteException;
  public android.apex.ApexSessionInfo getStagedSessionInfo(int session_id) throws android.os.RemoteException;
  public android.apex.ApexInfo[] getStagedApexInfos(android.apex.ApexSessionParams params) throws android.os.RemoteException;
  public android.apex.ApexInfo[] getActivePackages() throws android.os.RemoteException;
  public android.apex.ApexInfo[] getAllPackages() throws android.os.RemoteException;
  public void abortStagedSession(int session_id) throws android.os.RemoteException;
  public void revertActiveSessions() throws android.os.RemoteException;
  /**
   * Copies the CE apex data directory for the given user to the backup
   * location.
   */
  public void snapshotCeData(int user_id, int rollback_id, java.lang.String apex_name) throws android.os.RemoteException;
  /**
   * Restores the snapshot of the CE apex data directory for the given user and
   * apex. Note the snapshot will be deleted after restoration succeeded.
   */
  public void restoreCeData(int user_id, int rollback_id, java.lang.String apex_name) throws android.os.RemoteException;
  /** Deletes device-encrypted snapshots for the given rollback id. */
  public void destroyDeSnapshots(int rollback_id) throws android.os.RemoteException;
  /** Deletes credential-encrypted snapshots for the given user, for the given rollback id. */
  public void destroyCeSnapshots(int user_id, int rollback_id) throws android.os.RemoteException;
  /**
   * Deletes all credential-encrypted snapshots for the given user, except for
   * those listed in retain_rollback_ids.
   */
  public void destroyCeSnapshotsNotSpecified(int user_id, int[] retain_rollback_ids) throws android.os.RemoteException;
  public void unstagePackages(java.util.List<java.lang.String> active_package_paths) throws android.os.RemoteException;
  /**
   * Returns the active package corresponding to |package_name| and null
   * if none exists.
   */
  public android.apex.ApexInfo getActivePackage(java.lang.String package_name) throws android.os.RemoteException;
  /**
   * Not meant for use outside of testing. The call will not be
   * functional on user builds.
   */
  public void stagePackages(java.util.List<java.lang.String> package_tmp_paths) throws android.os.RemoteException;
  /**
   * Not meant for use outside of testing. The call will not be
   * functional on user builds.
   */
  public void resumeRevertIfNeeded() throws android.os.RemoteException;
  /**
   * Forces apexd to remount all active packages.
   * 
   * This call is mostly useful for speeding up development of APEXes.
   * Instead of going through a full APEX installation that requires a reboot,
   * developers can incorporate this method in much faster `adb sync` based
   * workflow:
   * 
   * 1. adb shell stop
   * 2. adb sync
   * 3. adb shell cmd -w apexservice remountPackages
   * 4. adb shell start
   * 
   * Note, that for an APEX package will be successfully remounted only if
   * there are no alive processes holding a reference to it.
   * 
   * Not meant for use outside of testing. This call will not be functional
   * on user builds. Only root is allowed to call this method.
   */
  public void remountPackages() throws android.os.RemoteException;
  /**
   * Forces apexd to recollect pre-installed data from the given |paths|.
   * 
   * Not meant for use outside of testing. This call will not be functional
   * on user builds. Only root is allowed to call this method.
   */
  public void recollectPreinstalledData(java.util.List<java.lang.String> paths) throws android.os.RemoteException;
  /**
   * Forces apexd to recollect data apex from the given |path|.
   * 
   * Not meant for use outside of testing. This call will not be functional
   * on user builds. Only root is allowed to call this method.
   */
  public void recollectDataApex(java.lang.String path, java.lang.String decompression_dir) throws android.os.RemoteException;
  /** Informs apexd that the boot has completed. */
  public void markBootCompleted() throws android.os.RemoteException;
  /**
   * Assuming the provided compressed APEX will be installed on next boot,
   * calculate how much space will be required for decompression
   */
  public long calculateSizeForCompressedApex(android.apex.CompressedApexInfoList compressed_apex_info_list) throws android.os.RemoteException;
  /**
   * Reserve space on /data partition for compressed APEX decompression. Returns error if
   * reservation fails. If empty list is passed, then reserved space is deallocated.
   */
  public void reserveSpaceForCompressedApex(android.apex.CompressedApexInfoList compressed_apex_info_list) throws android.os.RemoteException;
  /**
   * Performs a non-staged install of the given APEX.
   * Note: don't confuse this to preInstall and postInstall binder calls which are only used to
   * test corresponding features of APEX packages.
   */
  public android.apex.ApexInfo installAndActivatePackage(java.lang.String packagePath) throws android.os.RemoteException;
}
