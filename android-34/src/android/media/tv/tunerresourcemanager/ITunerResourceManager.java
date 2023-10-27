/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.tv.tunerresourcemanager;
/**
 * Interface of the Tuner Resource Manager. It manages resources used by TV Tuners.
 * <p>Resources include:
 * <ul>
 * <li>TunerFrontend {@link android.media.tv.tuner.frontend}.
 * <li>TunerLnb {@link android.media.tv.tuner.Lnb}.
 * <li>MediaCas {@link android.media.MediaCas}.
 * <li>TvInputHardware {@link android.media.tv.TvInputHardwareInfo}.
 * <ul>
 * 
 * <p>Expected workflow is:
 * <ul>
 * <li>Tuner Java/MediaCas/TIF update resources of the current device with TRM.
 * <li>Client registers its profile through {@link #registerClientProfile(ResourceClientProfile,
 * IResourcesReclaimListener, int[])}.
 * <li>Client requests resources through request APIs.
 * <li>If the resource needs to be handed to a higher priority client from a lower priority
 * one, TRM calls IResourcesReclaimListener registered by the lower priority client to release
 * the resource.
 * <ul>
 * 
 * @hide
 */
public interface ITunerResourceManager extends android.os.IInterface
{
  /** Default implementation for ITunerResourceManager. */
  public static class Default implements android.media.tv.tunerresourcemanager.ITunerResourceManager
  {
    /**
     * This API is used by the client to register their profile with the Tuner Resource manager.
     * 
     * <p>The profile contains information that can show the base priority score of the client.
     * 
     * @param profile {@link ResourceClientProfile} profile of the current client
     * @param listener {@link IResourcesReclaimListener} a callback to
     *                 reclaim clients' resources when needed.
     * @param clientId returns a clientId from the resource manager when the
     *                 the client registers its profile.
     */
    @Override public void registerClientProfile(android.media.tv.tunerresourcemanager.ResourceClientProfile profile, android.media.tv.tunerresourcemanager.IResourcesReclaimListener listener, int[] clientId) throws android.os.RemoteException
    {
    }
    /**
     * This API is used by the client to unregister their profile with the Tuner Resource manager.
     * 
     * @param clientId the client id that needs to be unregistered.
     */
    @Override public void unregisterClientProfile(int clientId) throws android.os.RemoteException
    {
    }
    /**
     * Updates a registered client's priority and niceValue.
     * 
     * @param clientId the id of the client that is updating its profile.
     * @param priority the priority that the client would like to update to.
     * @param niceValue the nice value that the client would like to update to.
     * 
     * @return true if the update is successful.
     */
    @Override public boolean updateClientPriority(int clientId, int priority, int niceValue) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * Checks if there is any unused frontend resource of the specified type.
     * 
     * @param frontendType the specific type of frontend resource to be checked for.
     * 
     * @return true if there is any unused resource of the specified type.
     */
    @Override public boolean hasUnusedFrontend(int frontendType) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * Checks if the client has the lowest priority among the clients that are holding
     * the frontend resource of the specified type.
     * 
     * <p> When this function returns false, it means that there is at least one client with the
     * strictly lower priority (than clientId) that is reclaimable by the system.
     * 
     * @param clientId The client ID to be checked the priority for.
     * @param frontendType The specific frontend type to be checked for.
     * 
     * @return false if there is another client holding the frontend resource of the specified type
     * that can be reclaimed. Otherwise true.
     */
    @Override public boolean isLowestPriority(int clientId, int frontendType) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * Updates the available Frontend resources information on the current device.
     * 
     * <p><strong>Note:</strong> This update must happen before the first
     * {@link #requestFrontend(TunerFrontendRequest,int[])} and {@link #releaseFrontend(int, int)}
     * call.
     * 
     * @param infos an array of the available {@link TunerFrontendInfo} information.
     */
    @Override public void setFrontendInfoList(android.media.tv.tunerresourcemanager.TunerFrontendInfo[] infos) throws android.os.RemoteException
    {
    }
    /**
     * Updates the available Cas resource information on the current device.
     * 
     * <p><strong>Note:</strong> This update must happen before the first
     * {@link #requestCasSession(CasSessionRequest, int[])} and {@link #releaseCasSession(int, int)}
     * call.
     * 
     * @param casSystemId id of the updating CAS system.
     * @param maxSessionNum the max session number of the CAS system that is updated.
     */
    @Override public void updateCasInfo(int casSystemId, int maxSessionNum) throws android.os.RemoteException
    {
    }
    /**
     * Updates the available Demux resources information on the current device.
     * 
     * <p><strong>Note:</strong> This update must happen before the first
     * {@link #requestDemux(TunerDemux,int[])} and {@link #releaseDemux(int, int)}
     * call.
     * 
     * @param infos an array of the available {@link TunerDemux} information.
     */
    @Override public void setDemuxInfoList(android.media.tv.tunerresourcemanager.TunerDemuxInfo[] infos) throws android.os.RemoteException
    {
    }
    /**
     * Updates the available Lnb resource information on the current device.
     * 
     * <p><strong>Note:</strong> This update must happen before the first
     * {@link #requestLnb(TunerLnbRequest, int[])} and {@link #releaseLnb(int, int)} call.
     * 
     * @param lnbIds ids of the updating lnbs.
     */
    @Override public void setLnbInfoList(int[] lnbIds) throws android.os.RemoteException
    {
    }
    /**
     * This API is used by the Tuner framework to request a frontend from the TunerHAL.
     * 
     * <p>There are two cases:
     * <ul>
     * <li>If the desiredId is not {@link TunerFrontendRequest#DEFAULT_DESIRED_ID}
     * <li><li>If the desired frontend with the given frontendType is available, the API would send
     * the id back.
     * <li><li>If the desired frontend with the given frontendType is in use but the current request
     * info can show higher priority than other uses of Frontend, the API will send
     * {@link IResourcesReclaimListener#onReclaimResources()} to the {@link Tuner}. Tuner would
     * handle the resource reclaim on the holder of lower priority and notify the holder of its
     * resource loss.
     * <li><li>If no frontend can be granted, the API would return false.
     * <ul>
     * 
     * <li>If the desiredId is {@link TunerFrontendRequest#DEFAULT_DESIRED_ID}
     * <li><li>If there is frontend available, the API would send the id back.
     * <li><li>If no Frontend is available but the current request info can show higher priority
     * than other uses of Frontend, the API will send
     * {@link IResourcesReclaimListener#onReclaimResources()} to the {@link Tuner}. Tuner would
     * handle the resource reclaim on the holder of lower priority and notify the holder of its
     * resource loss.
     * <li><li>If no frontend can be granted, the API would return false.
     * <ul>
     * 
     * <p><strong>Note:</strong> {@link #setFrontendInfoList(TunerFrontendInfo[])} must be called
     * before this request.
     * 
     * @param request {@link TunerFrontendRequest} information of the current request.
     * @param frontendHandle a one-element array to return the granted frontendHandle.
     * 
     * @return true if there is frontend granted.
     */
    @Override public boolean requestFrontend(android.media.tv.tunerresourcemanager.TunerFrontendRequest request, int[] frontendHandle) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * Sets the maximum usable frontends number of a given frontend type. It is used to enable or
     * disable frontends when cable connection status is changed by user.
     * 
     * @param frontendType the frontendType which the maximum usable number will be set for.
     * @param maxNumber the new maximum usable number.
     * 
     * @return true if  successful and false otherwise.
     */
    @Override public boolean setMaxNumberOfFrontends(int frontendType, int maxNum) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * Get the maximum usable frontends number of a given frontend type.
     * 
     * @param frontendType the frontendType which the maximum usable number will be queried for.
     * 
     * @return the maximum usable number of the queried frontend type. Returns -1 when the
     *         frontendType is invalid
     */
    @Override public int getMaxNumberOfFrontends(int frontendType) throws android.os.RemoteException
    {
      return 0;
    }
    /**
     * Requests to share frontend with an existing client.
     * 
     * <p><strong>Note:</strong> {@link #setFrontendInfoList(TunerFrontendInfo[])} must be called
     * before this request.
     * 
     * @param selfClientId the id of the client that sends the request.
     * @param targetClientId the id of the client to share the frontend with.
     */
    @Override public void shareFrontend(int selfClientId, int targetClientId) throws android.os.RemoteException
    {
    }
    /**
     * Transfers the ownership of the shared resource.
     * 
     * <p><strong>Note:</strong> Only the existing frontend sharee can be the new owner.
     * 
     * @param resourceType the type of resource to transfer the ownership for.
     * @param currentOwnerId the id of the current owner client.
     * @param newOwnerId the id of the new owner client.
     * 
     * @return true if successful. false otherwise.
     */
    @Override public boolean transferOwner(int resourceType, int currentOwnerId, int newOwnerId) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * This API is used by the Tuner framework to request an available demux from the TunerHAL.
     * 
     * <p>There are three possible scenarios:
     * <ul>
     * <li>If there is demux available, the API would send the handle back.
     * 
     * <li>If no Demux is available but the current request info can show higher priority than
     * other uses of demuxes, the API will send
     * {@link IResourcesReclaimListener#onReclaimResources()} to the {@link Tuner}. Tuner would
     * handle the resource reclaim on the holder of lower priority and notify the holder of its
     * resource loss.
     * 
     * <li>If no demux can be granted, the API would return false.
     * <ul>
     * 
     * @param request {@link TunerDemuxRequest} information of the current request.
     * @param demuxHandle a one-element array to return the granted demux handle.
     * 
     * @return true if there is demux granted.
     */
    @Override public boolean requestDemux(android.media.tv.tunerresourcemanager.TunerDemuxRequest request, int[] demuxHandle) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * This API is used by the Tuner framework to request an available descrambler from the
     * TunerHAL.
     * 
     * <p>There are three possible scenarios:
     * <ul>
     * <li>If there is descrambler available, the API would send the handle back.
     * 
     * <li>If no Descrambler is available but the current request info can show higher priority than
     * other uses of Descrambler, the API will send
     * {@link IResourcesReclaimListener#onReclaimResources()} to the {@link Tuner}. Tuner would
     * handle the resource reclaim on the holder of lower priority and notify the holder of its
     * resource loss.
     * 
     * <li>If no Descrambler can be granted, the API would return false.
     * <ul>
     * 
     * @param request {@link TunerDescramblerRequest} information of the current request.
     * @param descramblerHandle a one-element array to return the granted descrambler handle.
     * 
     * @return true if there is Descrambler granted.
     */
    @Override public boolean requestDescrambler(android.media.tv.tunerresourcemanager.TunerDescramblerRequest request, int[] descramblerHandle) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * This API is used by the Tuner framework to request an available Cas session. This session
     * needs to be under the CAS system with the id indicated in the {@code request}.
     * 
     * <p>There are three possible scenarios:
     * <ul>
     * <li>If there is Cas session available, the API would send the id back.
     * 
     * <li>If no Cas session is available but the current request info can show higher priority than
     * other uses of the sessions under the requested CAS system, the API will send
     * {@link ITunerResourceManagerCallback#onReclaimResources()} to the {@link Tuner}. Tuner would
     * handle the resource reclaim on the holder of lower priority and notify the holder of its
     * resource loss.
     * 
     * <li>If no Cas session can be granted, the API would return false.
     * <ul>
     * 
     * <p><strong>Note:</strong> {@link #updateCasInfo(int, int)} must be called before this request.
     * 
     * @param request {@link CasSessionRequest} information of the current request.
     * @param casSessionHandle a one-element array to return the granted cas session handle.
     * 
     * @return true if there is CAS session granted.
     */
    @Override public boolean requestCasSession(android.media.tv.tunerresourcemanager.CasSessionRequest request, int[] casSessionHandle) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * This API is used by the Tuner framework to request an available CuCam.
     * 
     * <p>There are three possible scenarios:
     * <ul>
     * <li>If there is CiCam available, the API would send the handle back.
     * 
     * <li>If no CiCma is available but the current request info can show higher priority than
     * other uses of the ciCam, the API will send
     * {@link ITunerResourceManagerCallback#onReclaimResources()} to the {@link Tuner}. Tuner would
     * handle the resource reclaim on the holder of lower priority and notify the holder of its
     * resource loss.
     * 
     * <li>If no CiCam can be granted, the API would return false.
     * <ul>
     * 
     * <p><strong>Note:</strong> {@link #updateCasInfo(int, int)} must be called before this request.
     * 
     * @param request {@link TunerCiCamRequest} information of the current request.
     * @param ciCamHandle a one-element array to return the granted ciCam handle.
     * 
     * @return true if there is CiCam granted.
     */
    @Override public boolean requestCiCam(android.media.tv.tunerresourcemanager.TunerCiCamRequest request, int[] ciCamHandle) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * This API is used by the Tuner framework to request an available Lnb from the TunerHAL.
     * 
     * <p>There are three possible scenarios:
     * <ul>
     * <li>If there is Lnb available, the API would send the id back.
     * 
     * <li>If no Lnb is available but the current request has a higher priority than other uses of
     * lnbs, the API will send {@link ITunerResourceManagerCallback#onReclaimResources()} to the
     * {@link Tuner}. Tuner would handle the resource reclaim on the holder of lower priority and
     * notify the holder of its resource loss.
     * 
     * <li>If no Lnb system can be granted, the API would return false.
     * <ul>
     * 
     * <p><strong>Note:</strong> {@link #setLnbInfos(int[])} must be called before this request.
     * 
     * @param request {@link TunerLnbRequest} information of the current request.
     * @param lnbHandle a one-element array to return the granted Lnb handle.
     * 
     * @return true if there is Lnb granted.
     */
    @Override public boolean requestLnb(android.media.tv.tunerresourcemanager.TunerLnbRequest request, int[] lnbHandle) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * Notifies the TRM that the given frontend has been released.
     * 
     * <p>Client must call this whenever it releases a Tuner frontend.
     * 
     * <p><strong>Note:</strong> {@link #setFrontendInfoList(TunerFrontendInfo[])} must be called
     * before this release.
     * 
     * @param frontendHandle the handle of the released frontend.
     * @param clientId the id of the client that is releasing the frontend.
     */
    @Override public void releaseFrontend(int frontendHandle, int clientId) throws android.os.RemoteException
    {
    }
    /**
     * Notifies the TRM that the Demux with the given handle was released.
     * 
     * <p>Client must call this whenever it releases a demux.
     * 
     * @param demuxHandle the handle of the released Tuner Demux.
     * @param clientId the id of the client that is releasing the demux.
     */
    @Override public void releaseDemux(int demuxHandle, int clientId) throws android.os.RemoteException
    {
    }
    /**
     * Notifies the TRM that the Descrambler with the given handle was released.
     * 
     * <p>Client must call this whenever it releases a descrambler.
     * 
     * @param descramblerHandle the handle of the released Tuner Descrambler.
     * @param clientId the id of the client that is releasing the descrambler.
     */
    @Override public void releaseDescrambler(int descramblerHandle, int clientId) throws android.os.RemoteException
    {
    }
    /**
     * Notifies the TRM that the given Cas session has been released.
     * 
     * <p>Client must call this whenever it releases a Cas session.
     * 
     * <p><strong>Note:</strong> {@link #updateCasInfo(int, int)} must be called before this release.
     * 
     * @param casSessionHandle the handle of the released CAS session.
     * @param clientId the id of the client that is releasing the cas session.
     */
    @Override public void releaseCasSession(int casSessionHandle, int clientId) throws android.os.RemoteException
    {
    }
    /**
     * Notifies the TRM that the given CiCam has been released.
     * 
     * <p>Client must call this whenever it releases a CiCam.
     * 
     * <p><strong>Note:</strong> {@link #updateCasInfo(int, int)} must be called before this
     * release.
     * 
     * @param ciCamHandle the handle of the releasing CiCam.
     * @param clientId the id of the client that is releasing the CiCam.
     */
    @Override public void releaseCiCam(int ciCamHandle, int clientId) throws android.os.RemoteException
    {
    }
    /**
     * Notifies the TRM that the Lnb with the given handle was released.
     * 
     * <p>Client must call this whenever it releases an Lnb.
     * 
     * <p><strong>Note:</strong> {@link #setLnbInfos(int[])} must be called before this release.
     * 
     * @param lnbHandle the handle of the released Tuner Lnb.
     * @param clientId the id of the client that is releasing the lnb.
     */
    @Override public void releaseLnb(int lnbHandle, int clientId) throws android.os.RemoteException
    {
    }
    /**
     * Compare two clients' priority.
     * 
     * @param challengerProfile the {@link ResourceClientProfile} of the challenger.
     * @param holderProfile the {@link ResourceClientProfile} of the holder of the resource.
     * 
     * @return true if the challenger has higher priority than the holder.
     */
    @Override public boolean isHigherPriority(android.media.tv.tunerresourcemanager.ResourceClientProfile challengerProfile, android.media.tv.tunerresourcemanager.ResourceClientProfile holderProfile) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * Stores Frontend resource map for the later restore.
     * 
     * <p>This is API is only for testing purpose and should be used in pair with
     * restoreResourceMap(), which allows testing of {@link Tuner} APIs
     * that behave differently based on different sets of resource map.
     * 
     * @param resourceType The resource type to store the map for.
     */
    @Override public void storeResourceMap(int resourceType) throws android.os.RemoteException
    {
    }
    /**
     * Clears the frontend resource map.
     * 
     * <p>This is API is only for testing purpose and should be called right after
     * storeResourceMap(), so TRMService#removeFrontendResource() does not
     * get called in TRMService#setFrontendInfoListInternal() for custom frontend
     * resource map creation.
     * 
     * @param resourceType The resource type to clear the map for.
     */
    @Override public void clearResourceMap(int resourceType) throws android.os.RemoteException
    {
    }
    /**
     * Restores Frontend resource map if it was stored before.
     * 
     * <p>This is API is only for testing purpose and should be used in pair with
     * storeResourceMap(), which allows testing of {@link Tuner} APIs
     * that behave differently based on different sets of resource map.
     * 
     * @param resourceType The resource type to restore the map for.
     */
    @Override public void restoreResourceMap(int resourceType) throws android.os.RemoteException
    {
    }
    /**
     * Grants the lock to the caller for public {@link Tuner} APIs
     * 
     * <p>{@link Tuner} functions that call both [@link TunerResourceManager} APIs and
     * grabs lock that are also used in {@link IResourcesReclaimListener#onReclaimResources()}
     * must call this API before acquiring lock used in onReclaimResources().
     * 
     * <p>This API will block until it releases the lock or fails
     * 
     * @param clientId The ID of the caller.
     * 
     * @return true if the lock is granted. If false is returned, calling this API again is not
     * guaranteed to work and may be unrecoverrable. (This should not happen.)
     */
    @Override public boolean acquireLock(int clientId, long clientThreadId) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * Releases the lock to the caller for public {@link Tuner} APIs
     * 
     * <p>This API must be called in pair with {@link #acquireLock(int, int)}
     * 
     * <p>This API will block until it releases the lock or fails
     * 
     * @param clientId The ID of the caller.
     * 
     * @return true if the lock is granted. If false is returned, calling this API again is not
     * guaranteed to work and may be unrecoverrable. (This should not happen.)
     */
    @Override public boolean releaseLock(int clientId) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * Returns a priority for the given use case type and the client's foreground or background
     * status.
     * 
     * @param useCase the use case type of the client. When the given use case type is invalid,
     *        the default use case type will be used. {@see TvInputService#PriorityHintUseCaseType}.
     * @param pid the pid of the client. When the pid is invalid, background status will be used as
     *        a client's status. Otherwise, client's app corresponding to the given session id will
     *        be used as a client. {@see TvInputService#onCreateSession(String, String)}.
     * 
     * @return the client priority..
     */
    @Override public int getClientPriority(int useCase, int pid) throws android.os.RemoteException
    {
      return 0;
    }
    /**
     * Returns a config priority for the given use case type and the foreground or background
     * status.
     * 
     * @param useCase the use case type of the client. When the given use case type is invalid,
     *        the default use case type will be used. {@see TvInputService#PriorityHintUseCaseType}.
     * @param isForeground {@code true} if foreground, {@code false} otherwise.
     * 
     * @return the config priority.
     */
    @Override public int getConfigPriority(int useCase, boolean isForeground) throws android.os.RemoteException
    {
      return 0;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.media.tv.tunerresourcemanager.ITunerResourceManager
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.media.tv.tunerresourcemanager.ITunerResourceManager interface,
     * generating a proxy if needed.
     */
    public static android.media.tv.tunerresourcemanager.ITunerResourceManager asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.media.tv.tunerresourcemanager.ITunerResourceManager))) {
        return ((android.media.tv.tunerresourcemanager.ITunerResourceManager)iin);
      }
      return new android.media.tv.tunerresourcemanager.ITunerResourceManager.Stub.Proxy(obj);
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
        case TRANSACTION_registerClientProfile:
        {
          android.media.tv.tunerresourcemanager.ResourceClientProfile _arg0;
          _arg0 = data.readTypedObject(android.media.tv.tunerresourcemanager.ResourceClientProfile.CREATOR);
          android.media.tv.tunerresourcemanager.IResourcesReclaimListener _arg1;
          _arg1 = android.media.tv.tunerresourcemanager.IResourcesReclaimListener.Stub.asInterface(data.readStrongBinder());
          int[] _arg2;
          int _arg2_length = data.readInt();
          if (_arg2_length < 0) {
            _arg2 = null;
          } else {
            _arg2 = new int[_arg2_length];
          }
          data.enforceNoDataAvail();
          this.registerClientProfile(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeIntArray(_arg2);
          break;
        }
        case TRANSACTION_unregisterClientProfile:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.unregisterClientProfile(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_updateClientPriority:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          boolean _result = this.updateClientPriority(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_hasUnusedFrontend:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          boolean _result = this.hasUnusedFrontend(_arg0);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_isLowestPriority:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          boolean _result = this.isLowestPriority(_arg0, _arg1);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_setFrontendInfoList:
        {
          android.media.tv.tunerresourcemanager.TunerFrontendInfo[] _arg0;
          _arg0 = data.createTypedArray(android.media.tv.tunerresourcemanager.TunerFrontendInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setFrontendInfoList(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_updateCasInfo:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.updateCasInfo(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setDemuxInfoList:
        {
          android.media.tv.tunerresourcemanager.TunerDemuxInfo[] _arg0;
          _arg0 = data.createTypedArray(android.media.tv.tunerresourcemanager.TunerDemuxInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setDemuxInfoList(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setLnbInfoList:
        {
          int[] _arg0;
          _arg0 = data.createIntArray();
          data.enforceNoDataAvail();
          this.setLnbInfoList(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_requestFrontend:
        {
          android.media.tv.tunerresourcemanager.TunerFrontendRequest _arg0;
          _arg0 = data.readTypedObject(android.media.tv.tunerresourcemanager.TunerFrontendRequest.CREATOR);
          int[] _arg1;
          int _arg1_length = data.readInt();
          if (_arg1_length < 0) {
            _arg1 = null;
          } else {
            _arg1 = new int[_arg1_length];
          }
          data.enforceNoDataAvail();
          boolean _result = this.requestFrontend(_arg0, _arg1);
          reply.writeNoException();
          reply.writeBoolean(_result);
          reply.writeIntArray(_arg1);
          break;
        }
        case TRANSACTION_setMaxNumberOfFrontends:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          boolean _result = this.setMaxNumberOfFrontends(_arg0, _arg1);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_getMaxNumberOfFrontends:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          int _result = this.getMaxNumberOfFrontends(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_shareFrontend:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.shareFrontend(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_transferOwner:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          boolean _result = this.transferOwner(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_requestDemux:
        {
          android.media.tv.tunerresourcemanager.TunerDemuxRequest _arg0;
          _arg0 = data.readTypedObject(android.media.tv.tunerresourcemanager.TunerDemuxRequest.CREATOR);
          int[] _arg1;
          int _arg1_length = data.readInt();
          if (_arg1_length < 0) {
            _arg1 = null;
          } else {
            _arg1 = new int[_arg1_length];
          }
          data.enforceNoDataAvail();
          boolean _result = this.requestDemux(_arg0, _arg1);
          reply.writeNoException();
          reply.writeBoolean(_result);
          reply.writeIntArray(_arg1);
          break;
        }
        case TRANSACTION_requestDescrambler:
        {
          android.media.tv.tunerresourcemanager.TunerDescramblerRequest _arg0;
          _arg0 = data.readTypedObject(android.media.tv.tunerresourcemanager.TunerDescramblerRequest.CREATOR);
          int[] _arg1;
          int _arg1_length = data.readInt();
          if (_arg1_length < 0) {
            _arg1 = null;
          } else {
            _arg1 = new int[_arg1_length];
          }
          data.enforceNoDataAvail();
          boolean _result = this.requestDescrambler(_arg0, _arg1);
          reply.writeNoException();
          reply.writeBoolean(_result);
          reply.writeIntArray(_arg1);
          break;
        }
        case TRANSACTION_requestCasSession:
        {
          android.media.tv.tunerresourcemanager.CasSessionRequest _arg0;
          _arg0 = data.readTypedObject(android.media.tv.tunerresourcemanager.CasSessionRequest.CREATOR);
          int[] _arg1;
          int _arg1_length = data.readInt();
          if (_arg1_length < 0) {
            _arg1 = null;
          } else {
            _arg1 = new int[_arg1_length];
          }
          data.enforceNoDataAvail();
          boolean _result = this.requestCasSession(_arg0, _arg1);
          reply.writeNoException();
          reply.writeBoolean(_result);
          reply.writeIntArray(_arg1);
          break;
        }
        case TRANSACTION_requestCiCam:
        {
          android.media.tv.tunerresourcemanager.TunerCiCamRequest _arg0;
          _arg0 = data.readTypedObject(android.media.tv.tunerresourcemanager.TunerCiCamRequest.CREATOR);
          int[] _arg1;
          int _arg1_length = data.readInt();
          if (_arg1_length < 0) {
            _arg1 = null;
          } else {
            _arg1 = new int[_arg1_length];
          }
          data.enforceNoDataAvail();
          boolean _result = this.requestCiCam(_arg0, _arg1);
          reply.writeNoException();
          reply.writeBoolean(_result);
          reply.writeIntArray(_arg1);
          break;
        }
        case TRANSACTION_requestLnb:
        {
          android.media.tv.tunerresourcemanager.TunerLnbRequest _arg0;
          _arg0 = data.readTypedObject(android.media.tv.tunerresourcemanager.TunerLnbRequest.CREATOR);
          int[] _arg1;
          int _arg1_length = data.readInt();
          if (_arg1_length < 0) {
            _arg1 = null;
          } else {
            _arg1 = new int[_arg1_length];
          }
          data.enforceNoDataAvail();
          boolean _result = this.requestLnb(_arg0, _arg1);
          reply.writeNoException();
          reply.writeBoolean(_result);
          reply.writeIntArray(_arg1);
          break;
        }
        case TRANSACTION_releaseFrontend:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.releaseFrontend(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_releaseDemux:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.releaseDemux(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_releaseDescrambler:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.releaseDescrambler(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_releaseCasSession:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.releaseCasSession(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_releaseCiCam:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.releaseCiCam(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_releaseLnb:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.releaseLnb(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_isHigherPriority:
        {
          android.media.tv.tunerresourcemanager.ResourceClientProfile _arg0;
          _arg0 = data.readTypedObject(android.media.tv.tunerresourcemanager.ResourceClientProfile.CREATOR);
          android.media.tv.tunerresourcemanager.ResourceClientProfile _arg1;
          _arg1 = data.readTypedObject(android.media.tv.tunerresourcemanager.ResourceClientProfile.CREATOR);
          data.enforceNoDataAvail();
          boolean _result = this.isHigherPriority(_arg0, _arg1);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_storeResourceMap:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.storeResourceMap(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_clearResourceMap:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.clearResourceMap(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_restoreResourceMap:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.restoreResourceMap(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_acquireLock:
        {
          int _arg0;
          _arg0 = data.readInt();
          long _arg1;
          _arg1 = data.readLong();
          data.enforceNoDataAvail();
          boolean _result = this.acquireLock(_arg0, _arg1);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_releaseLock:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          boolean _result = this.releaseLock(_arg0);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_getClientPriority:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          int _result = this.getClientPriority(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getConfigPriority:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          int _result = this.getConfigPriority(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.media.tv.tunerresourcemanager.ITunerResourceManager
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
       * This API is used by the client to register their profile with the Tuner Resource manager.
       * 
       * <p>The profile contains information that can show the base priority score of the client.
       * 
       * @param profile {@link ResourceClientProfile} profile of the current client
       * @param listener {@link IResourcesReclaimListener} a callback to
       *                 reclaim clients' resources when needed.
       * @param clientId returns a clientId from the resource manager when the
       *                 the client registers its profile.
       */
      @Override public void registerClientProfile(android.media.tv.tunerresourcemanager.ResourceClientProfile profile, android.media.tv.tunerresourcemanager.IResourcesReclaimListener listener, int[] clientId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(profile, 0);
          _data.writeStrongInterface(listener);
          _data.writeInt(clientId.length);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerClientProfile, _data, _reply, 0);
          _reply.readException();
          _reply.readIntArray(clientId);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * This API is used by the client to unregister their profile with the Tuner Resource manager.
       * 
       * @param clientId the client id that needs to be unregistered.
       */
      @Override public void unregisterClientProfile(int clientId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(clientId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterClientProfile, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Updates a registered client's priority and niceValue.
       * 
       * @param clientId the id of the client that is updating its profile.
       * @param priority the priority that the client would like to update to.
       * @param niceValue the nice value that the client would like to update to.
       * 
       * @return true if the update is successful.
       */
      @Override public boolean updateClientPriority(int clientId, int priority, int niceValue) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(clientId);
          _data.writeInt(priority);
          _data.writeInt(niceValue);
          boolean _status = mRemote.transact(Stub.TRANSACTION_updateClientPriority, _data, _reply, 0);
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
       * Checks if there is any unused frontend resource of the specified type.
       * 
       * @param frontendType the specific type of frontend resource to be checked for.
       * 
       * @return true if there is any unused resource of the specified type.
       */
      @Override public boolean hasUnusedFrontend(int frontendType) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(frontendType);
          boolean _status = mRemote.transact(Stub.TRANSACTION_hasUnusedFrontend, _data, _reply, 0);
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
       * Checks if the client has the lowest priority among the clients that are holding
       * the frontend resource of the specified type.
       * 
       * <p> When this function returns false, it means that there is at least one client with the
       * strictly lower priority (than clientId) that is reclaimable by the system.
       * 
       * @param clientId The client ID to be checked the priority for.
       * @param frontendType The specific frontend type to be checked for.
       * 
       * @return false if there is another client holding the frontend resource of the specified type
       * that can be reclaimed. Otherwise true.
       */
      @Override public boolean isLowestPriority(int clientId, int frontendType) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(clientId);
          _data.writeInt(frontendType);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isLowestPriority, _data, _reply, 0);
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
       * Updates the available Frontend resources information on the current device.
       * 
       * <p><strong>Note:</strong> This update must happen before the first
       * {@link #requestFrontend(TunerFrontendRequest,int[])} and {@link #releaseFrontend(int, int)}
       * call.
       * 
       * @param infos an array of the available {@link TunerFrontendInfo} information.
       */
      @Override public void setFrontendInfoList(android.media.tv.tunerresourcemanager.TunerFrontendInfo[] infos) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedArray(infos, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setFrontendInfoList, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Updates the available Cas resource information on the current device.
       * 
       * <p><strong>Note:</strong> This update must happen before the first
       * {@link #requestCasSession(CasSessionRequest, int[])} and {@link #releaseCasSession(int, int)}
       * call.
       * 
       * @param casSystemId id of the updating CAS system.
       * @param maxSessionNum the max session number of the CAS system that is updated.
       */
      @Override public void updateCasInfo(int casSystemId, int maxSessionNum) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(casSystemId);
          _data.writeInt(maxSessionNum);
          boolean _status = mRemote.transact(Stub.TRANSACTION_updateCasInfo, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Updates the available Demux resources information on the current device.
       * 
       * <p><strong>Note:</strong> This update must happen before the first
       * {@link #requestDemux(TunerDemux,int[])} and {@link #releaseDemux(int, int)}
       * call.
       * 
       * @param infos an array of the available {@link TunerDemux} information.
       */
      @Override public void setDemuxInfoList(android.media.tv.tunerresourcemanager.TunerDemuxInfo[] infos) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedArray(infos, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setDemuxInfoList, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Updates the available Lnb resource information on the current device.
       * 
       * <p><strong>Note:</strong> This update must happen before the first
       * {@link #requestLnb(TunerLnbRequest, int[])} and {@link #releaseLnb(int, int)} call.
       * 
       * @param lnbIds ids of the updating lnbs.
       */
      @Override public void setLnbInfoList(int[] lnbIds) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeIntArray(lnbIds);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setLnbInfoList, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * This API is used by the Tuner framework to request a frontend from the TunerHAL.
       * 
       * <p>There are two cases:
       * <ul>
       * <li>If the desiredId is not {@link TunerFrontendRequest#DEFAULT_DESIRED_ID}
       * <li><li>If the desired frontend with the given frontendType is available, the API would send
       * the id back.
       * <li><li>If the desired frontend with the given frontendType is in use but the current request
       * info can show higher priority than other uses of Frontend, the API will send
       * {@link IResourcesReclaimListener#onReclaimResources()} to the {@link Tuner}. Tuner would
       * handle the resource reclaim on the holder of lower priority and notify the holder of its
       * resource loss.
       * <li><li>If no frontend can be granted, the API would return false.
       * <ul>
       * 
       * <li>If the desiredId is {@link TunerFrontendRequest#DEFAULT_DESIRED_ID}
       * <li><li>If there is frontend available, the API would send the id back.
       * <li><li>If no Frontend is available but the current request info can show higher priority
       * than other uses of Frontend, the API will send
       * {@link IResourcesReclaimListener#onReclaimResources()} to the {@link Tuner}. Tuner would
       * handle the resource reclaim on the holder of lower priority and notify the holder of its
       * resource loss.
       * <li><li>If no frontend can be granted, the API would return false.
       * <ul>
       * 
       * <p><strong>Note:</strong> {@link #setFrontendInfoList(TunerFrontendInfo[])} must be called
       * before this request.
       * 
       * @param request {@link TunerFrontendRequest} information of the current request.
       * @param frontendHandle a one-element array to return the granted frontendHandle.
       * 
       * @return true if there is frontend granted.
       */
      @Override public boolean requestFrontend(android.media.tv.tunerresourcemanager.TunerFrontendRequest request, int[] frontendHandle) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(request, 0);
          _data.writeInt(frontendHandle.length);
          boolean _status = mRemote.transact(Stub.TRANSACTION_requestFrontend, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readBoolean();
          _reply.readIntArray(frontendHandle);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Sets the maximum usable frontends number of a given frontend type. It is used to enable or
       * disable frontends when cable connection status is changed by user.
       * 
       * @param frontendType the frontendType which the maximum usable number will be set for.
       * @param maxNumber the new maximum usable number.
       * 
       * @return true if  successful and false otherwise.
       */
      @Override public boolean setMaxNumberOfFrontends(int frontendType, int maxNum) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(frontendType);
          _data.writeInt(maxNum);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setMaxNumberOfFrontends, _data, _reply, 0);
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
       * Get the maximum usable frontends number of a given frontend type.
       * 
       * @param frontendType the frontendType which the maximum usable number will be queried for.
       * 
       * @return the maximum usable number of the queried frontend type. Returns -1 when the
       *         frontendType is invalid
       */
      @Override public int getMaxNumberOfFrontends(int frontendType) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(frontendType);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getMaxNumberOfFrontends, _data, _reply, 0);
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
       * Requests to share frontend with an existing client.
       * 
       * <p><strong>Note:</strong> {@link #setFrontendInfoList(TunerFrontendInfo[])} must be called
       * before this request.
       * 
       * @param selfClientId the id of the client that sends the request.
       * @param targetClientId the id of the client to share the frontend with.
       */
      @Override public void shareFrontend(int selfClientId, int targetClientId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(selfClientId);
          _data.writeInt(targetClientId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_shareFrontend, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Transfers the ownership of the shared resource.
       * 
       * <p><strong>Note:</strong> Only the existing frontend sharee can be the new owner.
       * 
       * @param resourceType the type of resource to transfer the ownership for.
       * @param currentOwnerId the id of the current owner client.
       * @param newOwnerId the id of the new owner client.
       * 
       * @return true if successful. false otherwise.
       */
      @Override public boolean transferOwner(int resourceType, int currentOwnerId, int newOwnerId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(resourceType);
          _data.writeInt(currentOwnerId);
          _data.writeInt(newOwnerId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_transferOwner, _data, _reply, 0);
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
       * This API is used by the Tuner framework to request an available demux from the TunerHAL.
       * 
       * <p>There are three possible scenarios:
       * <ul>
       * <li>If there is demux available, the API would send the handle back.
       * 
       * <li>If no Demux is available but the current request info can show higher priority than
       * other uses of demuxes, the API will send
       * {@link IResourcesReclaimListener#onReclaimResources()} to the {@link Tuner}. Tuner would
       * handle the resource reclaim on the holder of lower priority and notify the holder of its
       * resource loss.
       * 
       * <li>If no demux can be granted, the API would return false.
       * <ul>
       * 
       * @param request {@link TunerDemuxRequest} information of the current request.
       * @param demuxHandle a one-element array to return the granted demux handle.
       * 
       * @return true if there is demux granted.
       */
      @Override public boolean requestDemux(android.media.tv.tunerresourcemanager.TunerDemuxRequest request, int[] demuxHandle) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(request, 0);
          _data.writeInt(demuxHandle.length);
          boolean _status = mRemote.transact(Stub.TRANSACTION_requestDemux, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readBoolean();
          _reply.readIntArray(demuxHandle);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * This API is used by the Tuner framework to request an available descrambler from the
       * TunerHAL.
       * 
       * <p>There are three possible scenarios:
       * <ul>
       * <li>If there is descrambler available, the API would send the handle back.
       * 
       * <li>If no Descrambler is available but the current request info can show higher priority than
       * other uses of Descrambler, the API will send
       * {@link IResourcesReclaimListener#onReclaimResources()} to the {@link Tuner}. Tuner would
       * handle the resource reclaim on the holder of lower priority and notify the holder of its
       * resource loss.
       * 
       * <li>If no Descrambler can be granted, the API would return false.
       * <ul>
       * 
       * @param request {@link TunerDescramblerRequest} information of the current request.
       * @param descramblerHandle a one-element array to return the granted descrambler handle.
       * 
       * @return true if there is Descrambler granted.
       */
      @Override public boolean requestDescrambler(android.media.tv.tunerresourcemanager.TunerDescramblerRequest request, int[] descramblerHandle) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(request, 0);
          _data.writeInt(descramblerHandle.length);
          boolean _status = mRemote.transact(Stub.TRANSACTION_requestDescrambler, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readBoolean();
          _reply.readIntArray(descramblerHandle);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * This API is used by the Tuner framework to request an available Cas session. This session
       * needs to be under the CAS system with the id indicated in the {@code request}.
       * 
       * <p>There are three possible scenarios:
       * <ul>
       * <li>If there is Cas session available, the API would send the id back.
       * 
       * <li>If no Cas session is available but the current request info can show higher priority than
       * other uses of the sessions under the requested CAS system, the API will send
       * {@link ITunerResourceManagerCallback#onReclaimResources()} to the {@link Tuner}. Tuner would
       * handle the resource reclaim on the holder of lower priority and notify the holder of its
       * resource loss.
       * 
       * <li>If no Cas session can be granted, the API would return false.
       * <ul>
       * 
       * <p><strong>Note:</strong> {@link #updateCasInfo(int, int)} must be called before this request.
       * 
       * @param request {@link CasSessionRequest} information of the current request.
       * @param casSessionHandle a one-element array to return the granted cas session handle.
       * 
       * @return true if there is CAS session granted.
       */
      @Override public boolean requestCasSession(android.media.tv.tunerresourcemanager.CasSessionRequest request, int[] casSessionHandle) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(request, 0);
          _data.writeInt(casSessionHandle.length);
          boolean _status = mRemote.transact(Stub.TRANSACTION_requestCasSession, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readBoolean();
          _reply.readIntArray(casSessionHandle);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * This API is used by the Tuner framework to request an available CuCam.
       * 
       * <p>There are three possible scenarios:
       * <ul>
       * <li>If there is CiCam available, the API would send the handle back.
       * 
       * <li>If no CiCma is available but the current request info can show higher priority than
       * other uses of the ciCam, the API will send
       * {@link ITunerResourceManagerCallback#onReclaimResources()} to the {@link Tuner}. Tuner would
       * handle the resource reclaim on the holder of lower priority and notify the holder of its
       * resource loss.
       * 
       * <li>If no CiCam can be granted, the API would return false.
       * <ul>
       * 
       * <p><strong>Note:</strong> {@link #updateCasInfo(int, int)} must be called before this request.
       * 
       * @param request {@link TunerCiCamRequest} information of the current request.
       * @param ciCamHandle a one-element array to return the granted ciCam handle.
       * 
       * @return true if there is CiCam granted.
       */
      @Override public boolean requestCiCam(android.media.tv.tunerresourcemanager.TunerCiCamRequest request, int[] ciCamHandle) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(request, 0);
          _data.writeInt(ciCamHandle.length);
          boolean _status = mRemote.transact(Stub.TRANSACTION_requestCiCam, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readBoolean();
          _reply.readIntArray(ciCamHandle);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * This API is used by the Tuner framework to request an available Lnb from the TunerHAL.
       * 
       * <p>There are three possible scenarios:
       * <ul>
       * <li>If there is Lnb available, the API would send the id back.
       * 
       * <li>If no Lnb is available but the current request has a higher priority than other uses of
       * lnbs, the API will send {@link ITunerResourceManagerCallback#onReclaimResources()} to the
       * {@link Tuner}. Tuner would handle the resource reclaim on the holder of lower priority and
       * notify the holder of its resource loss.
       * 
       * <li>If no Lnb system can be granted, the API would return false.
       * <ul>
       * 
       * <p><strong>Note:</strong> {@link #setLnbInfos(int[])} must be called before this request.
       * 
       * @param request {@link TunerLnbRequest} information of the current request.
       * @param lnbHandle a one-element array to return the granted Lnb handle.
       * 
       * @return true if there is Lnb granted.
       */
      @Override public boolean requestLnb(android.media.tv.tunerresourcemanager.TunerLnbRequest request, int[] lnbHandle) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(request, 0);
          _data.writeInt(lnbHandle.length);
          boolean _status = mRemote.transact(Stub.TRANSACTION_requestLnb, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readBoolean();
          _reply.readIntArray(lnbHandle);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Notifies the TRM that the given frontend has been released.
       * 
       * <p>Client must call this whenever it releases a Tuner frontend.
       * 
       * <p><strong>Note:</strong> {@link #setFrontendInfoList(TunerFrontendInfo[])} must be called
       * before this release.
       * 
       * @param frontendHandle the handle of the released frontend.
       * @param clientId the id of the client that is releasing the frontend.
       */
      @Override public void releaseFrontend(int frontendHandle, int clientId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(frontendHandle);
          _data.writeInt(clientId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_releaseFrontend, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Notifies the TRM that the Demux with the given handle was released.
       * 
       * <p>Client must call this whenever it releases a demux.
       * 
       * @param demuxHandle the handle of the released Tuner Demux.
       * @param clientId the id of the client that is releasing the demux.
       */
      @Override public void releaseDemux(int demuxHandle, int clientId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(demuxHandle);
          _data.writeInt(clientId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_releaseDemux, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Notifies the TRM that the Descrambler with the given handle was released.
       * 
       * <p>Client must call this whenever it releases a descrambler.
       * 
       * @param descramblerHandle the handle of the released Tuner Descrambler.
       * @param clientId the id of the client that is releasing the descrambler.
       */
      @Override public void releaseDescrambler(int descramblerHandle, int clientId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(descramblerHandle);
          _data.writeInt(clientId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_releaseDescrambler, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Notifies the TRM that the given Cas session has been released.
       * 
       * <p>Client must call this whenever it releases a Cas session.
       * 
       * <p><strong>Note:</strong> {@link #updateCasInfo(int, int)} must be called before this release.
       * 
       * @param casSessionHandle the handle of the released CAS session.
       * @param clientId the id of the client that is releasing the cas session.
       */
      @Override public void releaseCasSession(int casSessionHandle, int clientId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(casSessionHandle);
          _data.writeInt(clientId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_releaseCasSession, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Notifies the TRM that the given CiCam has been released.
       * 
       * <p>Client must call this whenever it releases a CiCam.
       * 
       * <p><strong>Note:</strong> {@link #updateCasInfo(int, int)} must be called before this
       * release.
       * 
       * @param ciCamHandle the handle of the releasing CiCam.
       * @param clientId the id of the client that is releasing the CiCam.
       */
      @Override public void releaseCiCam(int ciCamHandle, int clientId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(ciCamHandle);
          _data.writeInt(clientId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_releaseCiCam, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Notifies the TRM that the Lnb with the given handle was released.
       * 
       * <p>Client must call this whenever it releases an Lnb.
       * 
       * <p><strong>Note:</strong> {@link #setLnbInfos(int[])} must be called before this release.
       * 
       * @param lnbHandle the handle of the released Tuner Lnb.
       * @param clientId the id of the client that is releasing the lnb.
       */
      @Override public void releaseLnb(int lnbHandle, int clientId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(lnbHandle);
          _data.writeInt(clientId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_releaseLnb, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Compare two clients' priority.
       * 
       * @param challengerProfile the {@link ResourceClientProfile} of the challenger.
       * @param holderProfile the {@link ResourceClientProfile} of the holder of the resource.
       * 
       * @return true if the challenger has higher priority than the holder.
       */
      @Override public boolean isHigherPriority(android.media.tv.tunerresourcemanager.ResourceClientProfile challengerProfile, android.media.tv.tunerresourcemanager.ResourceClientProfile holderProfile) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(challengerProfile, 0);
          _data.writeTypedObject(holderProfile, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isHigherPriority, _data, _reply, 0);
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
       * Stores Frontend resource map for the later restore.
       * 
       * <p>This is API is only for testing purpose and should be used in pair with
       * restoreResourceMap(), which allows testing of {@link Tuner} APIs
       * that behave differently based on different sets of resource map.
       * 
       * @param resourceType The resource type to store the map for.
       */
      @Override public void storeResourceMap(int resourceType) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(resourceType);
          boolean _status = mRemote.transact(Stub.TRANSACTION_storeResourceMap, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Clears the frontend resource map.
       * 
       * <p>This is API is only for testing purpose and should be called right after
       * storeResourceMap(), so TRMService#removeFrontendResource() does not
       * get called in TRMService#setFrontendInfoListInternal() for custom frontend
       * resource map creation.
       * 
       * @param resourceType The resource type to clear the map for.
       */
      @Override public void clearResourceMap(int resourceType) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(resourceType);
          boolean _status = mRemote.transact(Stub.TRANSACTION_clearResourceMap, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Restores Frontend resource map if it was stored before.
       * 
       * <p>This is API is only for testing purpose and should be used in pair with
       * storeResourceMap(), which allows testing of {@link Tuner} APIs
       * that behave differently based on different sets of resource map.
       * 
       * @param resourceType The resource type to restore the map for.
       */
      @Override public void restoreResourceMap(int resourceType) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(resourceType);
          boolean _status = mRemote.transact(Stub.TRANSACTION_restoreResourceMap, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Grants the lock to the caller for public {@link Tuner} APIs
       * 
       * <p>{@link Tuner} functions that call both [@link TunerResourceManager} APIs and
       * grabs lock that are also used in {@link IResourcesReclaimListener#onReclaimResources()}
       * must call this API before acquiring lock used in onReclaimResources().
       * 
       * <p>This API will block until it releases the lock or fails
       * 
       * @param clientId The ID of the caller.
       * 
       * @return true if the lock is granted. If false is returned, calling this API again is not
       * guaranteed to work and may be unrecoverrable. (This should not happen.)
       */
      @Override public boolean acquireLock(int clientId, long clientThreadId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(clientId);
          _data.writeLong(clientThreadId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_acquireLock, _data, _reply, 0);
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
       * Releases the lock to the caller for public {@link Tuner} APIs
       * 
       * <p>This API must be called in pair with {@link #acquireLock(int, int)}
       * 
       * <p>This API will block until it releases the lock or fails
       * 
       * @param clientId The ID of the caller.
       * 
       * @return true if the lock is granted. If false is returned, calling this API again is not
       * guaranteed to work and may be unrecoverrable. (This should not happen.)
       */
      @Override public boolean releaseLock(int clientId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(clientId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_releaseLock, _data, _reply, 0);
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
       * Returns a priority for the given use case type and the client's foreground or background
       * status.
       * 
       * @param useCase the use case type of the client. When the given use case type is invalid,
       *        the default use case type will be used. {@see TvInputService#PriorityHintUseCaseType}.
       * @param pid the pid of the client. When the pid is invalid, background status will be used as
       *        a client's status. Otherwise, client's app corresponding to the given session id will
       *        be used as a client. {@see TvInputService#onCreateSession(String, String)}.
       * 
       * @return the client priority..
       */
      @Override public int getClientPriority(int useCase, int pid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(useCase);
          _data.writeInt(pid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getClientPriority, _data, _reply, 0);
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
       * Returns a config priority for the given use case type and the foreground or background
       * status.
       * 
       * @param useCase the use case type of the client. When the given use case type is invalid,
       *        the default use case type will be used. {@see TvInputService#PriorityHintUseCaseType}.
       * @param isForeground {@code true} if foreground, {@code false} otherwise.
       * 
       * @return the config priority.
       */
      @Override public int getConfigPriority(int useCase, boolean isForeground) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(useCase);
          _data.writeBoolean(isForeground);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getConfigPriority, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_registerClientProfile = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_unregisterClientProfile = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_updateClientPriority = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_hasUnusedFrontend = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_isLowestPriority = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_setFrontendInfoList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_updateCasInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_setDemuxInfoList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_setLnbInfoList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_requestFrontend = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_setMaxNumberOfFrontends = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_getMaxNumberOfFrontends = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_shareFrontend = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_transferOwner = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_requestDemux = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_requestDescrambler = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_requestCasSession = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_requestCiCam = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_requestLnb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_releaseFrontend = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
    static final int TRANSACTION_releaseDemux = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
    static final int TRANSACTION_releaseDescrambler = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
    static final int TRANSACTION_releaseCasSession = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
    static final int TRANSACTION_releaseCiCam = (android.os.IBinder.FIRST_CALL_TRANSACTION + 23);
    static final int TRANSACTION_releaseLnb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 24);
    static final int TRANSACTION_isHigherPriority = (android.os.IBinder.FIRST_CALL_TRANSACTION + 25);
    static final int TRANSACTION_storeResourceMap = (android.os.IBinder.FIRST_CALL_TRANSACTION + 26);
    static final int TRANSACTION_clearResourceMap = (android.os.IBinder.FIRST_CALL_TRANSACTION + 27);
    static final int TRANSACTION_restoreResourceMap = (android.os.IBinder.FIRST_CALL_TRANSACTION + 28);
    static final int TRANSACTION_acquireLock = (android.os.IBinder.FIRST_CALL_TRANSACTION + 29);
    static final int TRANSACTION_releaseLock = (android.os.IBinder.FIRST_CALL_TRANSACTION + 30);
    static final int TRANSACTION_getClientPriority = (android.os.IBinder.FIRST_CALL_TRANSACTION + 31);
    static final int TRANSACTION_getConfigPriority = (android.os.IBinder.FIRST_CALL_TRANSACTION + 32);
  }
  public static final java.lang.String DESCRIPTOR = "android$media$tv$tunerresourcemanager$ITunerResourceManager".replace('$', '.');
  /**
   * This API is used by the client to register their profile with the Tuner Resource manager.
   * 
   * <p>The profile contains information that can show the base priority score of the client.
   * 
   * @param profile {@link ResourceClientProfile} profile of the current client
   * @param listener {@link IResourcesReclaimListener} a callback to
   *                 reclaim clients' resources when needed.
   * @param clientId returns a clientId from the resource manager when the
   *                 the client registers its profile.
   */
  public void registerClientProfile(android.media.tv.tunerresourcemanager.ResourceClientProfile profile, android.media.tv.tunerresourcemanager.IResourcesReclaimListener listener, int[] clientId) throws android.os.RemoteException;
  /**
   * This API is used by the client to unregister their profile with the Tuner Resource manager.
   * 
   * @param clientId the client id that needs to be unregistered.
   */
  public void unregisterClientProfile(int clientId) throws android.os.RemoteException;
  /**
   * Updates a registered client's priority and niceValue.
   * 
   * @param clientId the id of the client that is updating its profile.
   * @param priority the priority that the client would like to update to.
   * @param niceValue the nice value that the client would like to update to.
   * 
   * @return true if the update is successful.
   */
  public boolean updateClientPriority(int clientId, int priority, int niceValue) throws android.os.RemoteException;
  /**
   * Checks if there is any unused frontend resource of the specified type.
   * 
   * @param frontendType the specific type of frontend resource to be checked for.
   * 
   * @return true if there is any unused resource of the specified type.
   */
  public boolean hasUnusedFrontend(int frontendType) throws android.os.RemoteException;
  /**
   * Checks if the client has the lowest priority among the clients that are holding
   * the frontend resource of the specified type.
   * 
   * <p> When this function returns false, it means that there is at least one client with the
   * strictly lower priority (than clientId) that is reclaimable by the system.
   * 
   * @param clientId The client ID to be checked the priority for.
   * @param frontendType The specific frontend type to be checked for.
   * 
   * @return false if there is another client holding the frontend resource of the specified type
   * that can be reclaimed. Otherwise true.
   */
  public boolean isLowestPriority(int clientId, int frontendType) throws android.os.RemoteException;
  /**
   * Updates the available Frontend resources information on the current device.
   * 
   * <p><strong>Note:</strong> This update must happen before the first
   * {@link #requestFrontend(TunerFrontendRequest,int[])} and {@link #releaseFrontend(int, int)}
   * call.
   * 
   * @param infos an array of the available {@link TunerFrontendInfo} information.
   */
  public void setFrontendInfoList(android.media.tv.tunerresourcemanager.TunerFrontendInfo[] infos) throws android.os.RemoteException;
  /**
   * Updates the available Cas resource information on the current device.
   * 
   * <p><strong>Note:</strong> This update must happen before the first
   * {@link #requestCasSession(CasSessionRequest, int[])} and {@link #releaseCasSession(int, int)}
   * call.
   * 
   * @param casSystemId id of the updating CAS system.
   * @param maxSessionNum the max session number of the CAS system that is updated.
   */
  public void updateCasInfo(int casSystemId, int maxSessionNum) throws android.os.RemoteException;
  /**
   * Updates the available Demux resources information on the current device.
   * 
   * <p><strong>Note:</strong> This update must happen before the first
   * {@link #requestDemux(TunerDemux,int[])} and {@link #releaseDemux(int, int)}
   * call.
   * 
   * @param infos an array of the available {@link TunerDemux} information.
   */
  public void setDemuxInfoList(android.media.tv.tunerresourcemanager.TunerDemuxInfo[] infos) throws android.os.RemoteException;
  /**
   * Updates the available Lnb resource information on the current device.
   * 
   * <p><strong>Note:</strong> This update must happen before the first
   * {@link #requestLnb(TunerLnbRequest, int[])} and {@link #releaseLnb(int, int)} call.
   * 
   * @param lnbIds ids of the updating lnbs.
   */
  public void setLnbInfoList(int[] lnbIds) throws android.os.RemoteException;
  /**
   * This API is used by the Tuner framework to request a frontend from the TunerHAL.
   * 
   * <p>There are two cases:
   * <ul>
   * <li>If the desiredId is not {@link TunerFrontendRequest#DEFAULT_DESIRED_ID}
   * <li><li>If the desired frontend with the given frontendType is available, the API would send
   * the id back.
   * <li><li>If the desired frontend with the given frontendType is in use but the current request
   * info can show higher priority than other uses of Frontend, the API will send
   * {@link IResourcesReclaimListener#onReclaimResources()} to the {@link Tuner}. Tuner would
   * handle the resource reclaim on the holder of lower priority and notify the holder of its
   * resource loss.
   * <li><li>If no frontend can be granted, the API would return false.
   * <ul>
   * 
   * <li>If the desiredId is {@link TunerFrontendRequest#DEFAULT_DESIRED_ID}
   * <li><li>If there is frontend available, the API would send the id back.
   * <li><li>If no Frontend is available but the current request info can show higher priority
   * than other uses of Frontend, the API will send
   * {@link IResourcesReclaimListener#onReclaimResources()} to the {@link Tuner}. Tuner would
   * handle the resource reclaim on the holder of lower priority and notify the holder of its
   * resource loss.
   * <li><li>If no frontend can be granted, the API would return false.
   * <ul>
   * 
   * <p><strong>Note:</strong> {@link #setFrontendInfoList(TunerFrontendInfo[])} must be called
   * before this request.
   * 
   * @param request {@link TunerFrontendRequest} information of the current request.
   * @param frontendHandle a one-element array to return the granted frontendHandle.
   * 
   * @return true if there is frontend granted.
   */
  public boolean requestFrontend(android.media.tv.tunerresourcemanager.TunerFrontendRequest request, int[] frontendHandle) throws android.os.RemoteException;
  /**
   * Sets the maximum usable frontends number of a given frontend type. It is used to enable or
   * disable frontends when cable connection status is changed by user.
   * 
   * @param frontendType the frontendType which the maximum usable number will be set for.
   * @param maxNumber the new maximum usable number.
   * 
   * @return true if  successful and false otherwise.
   */
  public boolean setMaxNumberOfFrontends(int frontendType, int maxNum) throws android.os.RemoteException;
  /**
   * Get the maximum usable frontends number of a given frontend type.
   * 
   * @param frontendType the frontendType which the maximum usable number will be queried for.
   * 
   * @return the maximum usable number of the queried frontend type. Returns -1 when the
   *         frontendType is invalid
   */
  public int getMaxNumberOfFrontends(int frontendType) throws android.os.RemoteException;
  /**
   * Requests to share frontend with an existing client.
   * 
   * <p><strong>Note:</strong> {@link #setFrontendInfoList(TunerFrontendInfo[])} must be called
   * before this request.
   * 
   * @param selfClientId the id of the client that sends the request.
   * @param targetClientId the id of the client to share the frontend with.
   */
  public void shareFrontend(int selfClientId, int targetClientId) throws android.os.RemoteException;
  /**
   * Transfers the ownership of the shared resource.
   * 
   * <p><strong>Note:</strong> Only the existing frontend sharee can be the new owner.
   * 
   * @param resourceType the type of resource to transfer the ownership for.
   * @param currentOwnerId the id of the current owner client.
   * @param newOwnerId the id of the new owner client.
   * 
   * @return true if successful. false otherwise.
   */
  public boolean transferOwner(int resourceType, int currentOwnerId, int newOwnerId) throws android.os.RemoteException;
  /**
   * This API is used by the Tuner framework to request an available demux from the TunerHAL.
   * 
   * <p>There are three possible scenarios:
   * <ul>
   * <li>If there is demux available, the API would send the handle back.
   * 
   * <li>If no Demux is available but the current request info can show higher priority than
   * other uses of demuxes, the API will send
   * {@link IResourcesReclaimListener#onReclaimResources()} to the {@link Tuner}. Tuner would
   * handle the resource reclaim on the holder of lower priority and notify the holder of its
   * resource loss.
   * 
   * <li>If no demux can be granted, the API would return false.
   * <ul>
   * 
   * @param request {@link TunerDemuxRequest} information of the current request.
   * @param demuxHandle a one-element array to return the granted demux handle.
   * 
   * @return true if there is demux granted.
   */
  public boolean requestDemux(android.media.tv.tunerresourcemanager.TunerDemuxRequest request, int[] demuxHandle) throws android.os.RemoteException;
  /**
   * This API is used by the Tuner framework to request an available descrambler from the
   * TunerHAL.
   * 
   * <p>There are three possible scenarios:
   * <ul>
   * <li>If there is descrambler available, the API would send the handle back.
   * 
   * <li>If no Descrambler is available but the current request info can show higher priority than
   * other uses of Descrambler, the API will send
   * {@link IResourcesReclaimListener#onReclaimResources()} to the {@link Tuner}. Tuner would
   * handle the resource reclaim on the holder of lower priority and notify the holder of its
   * resource loss.
   * 
   * <li>If no Descrambler can be granted, the API would return false.
   * <ul>
   * 
   * @param request {@link TunerDescramblerRequest} information of the current request.
   * @param descramblerHandle a one-element array to return the granted descrambler handle.
   * 
   * @return true if there is Descrambler granted.
   */
  public boolean requestDescrambler(android.media.tv.tunerresourcemanager.TunerDescramblerRequest request, int[] descramblerHandle) throws android.os.RemoteException;
  /**
   * This API is used by the Tuner framework to request an available Cas session. This session
   * needs to be under the CAS system with the id indicated in the {@code request}.
   * 
   * <p>There are three possible scenarios:
   * <ul>
   * <li>If there is Cas session available, the API would send the id back.
   * 
   * <li>If no Cas session is available but the current request info can show higher priority than
   * other uses of the sessions under the requested CAS system, the API will send
   * {@link ITunerResourceManagerCallback#onReclaimResources()} to the {@link Tuner}. Tuner would
   * handle the resource reclaim on the holder of lower priority and notify the holder of its
   * resource loss.
   * 
   * <li>If no Cas session can be granted, the API would return false.
   * <ul>
   * 
   * <p><strong>Note:</strong> {@link #updateCasInfo(int, int)} must be called before this request.
   * 
   * @param request {@link CasSessionRequest} information of the current request.
   * @param casSessionHandle a one-element array to return the granted cas session handle.
   * 
   * @return true if there is CAS session granted.
   */
  public boolean requestCasSession(android.media.tv.tunerresourcemanager.CasSessionRequest request, int[] casSessionHandle) throws android.os.RemoteException;
  /**
   * This API is used by the Tuner framework to request an available CuCam.
   * 
   * <p>There are three possible scenarios:
   * <ul>
   * <li>If there is CiCam available, the API would send the handle back.
   * 
   * <li>If no CiCma is available but the current request info can show higher priority than
   * other uses of the ciCam, the API will send
   * {@link ITunerResourceManagerCallback#onReclaimResources()} to the {@link Tuner}. Tuner would
   * handle the resource reclaim on the holder of lower priority and notify the holder of its
   * resource loss.
   * 
   * <li>If no CiCam can be granted, the API would return false.
   * <ul>
   * 
   * <p><strong>Note:</strong> {@link #updateCasInfo(int, int)} must be called before this request.
   * 
   * @param request {@link TunerCiCamRequest} information of the current request.
   * @param ciCamHandle a one-element array to return the granted ciCam handle.
   * 
   * @return true if there is CiCam granted.
   */
  public boolean requestCiCam(android.media.tv.tunerresourcemanager.TunerCiCamRequest request, int[] ciCamHandle) throws android.os.RemoteException;
  /**
   * This API is used by the Tuner framework to request an available Lnb from the TunerHAL.
   * 
   * <p>There are three possible scenarios:
   * <ul>
   * <li>If there is Lnb available, the API would send the id back.
   * 
   * <li>If no Lnb is available but the current request has a higher priority than other uses of
   * lnbs, the API will send {@link ITunerResourceManagerCallback#onReclaimResources()} to the
   * {@link Tuner}. Tuner would handle the resource reclaim on the holder of lower priority and
   * notify the holder of its resource loss.
   * 
   * <li>If no Lnb system can be granted, the API would return false.
   * <ul>
   * 
   * <p><strong>Note:</strong> {@link #setLnbInfos(int[])} must be called before this request.
   * 
   * @param request {@link TunerLnbRequest} information of the current request.
   * @param lnbHandle a one-element array to return the granted Lnb handle.
   * 
   * @return true if there is Lnb granted.
   */
  public boolean requestLnb(android.media.tv.tunerresourcemanager.TunerLnbRequest request, int[] lnbHandle) throws android.os.RemoteException;
  /**
   * Notifies the TRM that the given frontend has been released.
   * 
   * <p>Client must call this whenever it releases a Tuner frontend.
   * 
   * <p><strong>Note:</strong> {@link #setFrontendInfoList(TunerFrontendInfo[])} must be called
   * before this release.
   * 
   * @param frontendHandle the handle of the released frontend.
   * @param clientId the id of the client that is releasing the frontend.
   */
  public void releaseFrontend(int frontendHandle, int clientId) throws android.os.RemoteException;
  /**
   * Notifies the TRM that the Demux with the given handle was released.
   * 
   * <p>Client must call this whenever it releases a demux.
   * 
   * @param demuxHandle the handle of the released Tuner Demux.
   * @param clientId the id of the client that is releasing the demux.
   */
  public void releaseDemux(int demuxHandle, int clientId) throws android.os.RemoteException;
  /**
   * Notifies the TRM that the Descrambler with the given handle was released.
   * 
   * <p>Client must call this whenever it releases a descrambler.
   * 
   * @param descramblerHandle the handle of the released Tuner Descrambler.
   * @param clientId the id of the client that is releasing the descrambler.
   */
  public void releaseDescrambler(int descramblerHandle, int clientId) throws android.os.RemoteException;
  /**
   * Notifies the TRM that the given Cas session has been released.
   * 
   * <p>Client must call this whenever it releases a Cas session.
   * 
   * <p><strong>Note:</strong> {@link #updateCasInfo(int, int)} must be called before this release.
   * 
   * @param casSessionHandle the handle of the released CAS session.
   * @param clientId the id of the client that is releasing the cas session.
   */
  public void releaseCasSession(int casSessionHandle, int clientId) throws android.os.RemoteException;
  /**
   * Notifies the TRM that the given CiCam has been released.
   * 
   * <p>Client must call this whenever it releases a CiCam.
   * 
   * <p><strong>Note:</strong> {@link #updateCasInfo(int, int)} must be called before this
   * release.
   * 
   * @param ciCamHandle the handle of the releasing CiCam.
   * @param clientId the id of the client that is releasing the CiCam.
   */
  public void releaseCiCam(int ciCamHandle, int clientId) throws android.os.RemoteException;
  /**
   * Notifies the TRM that the Lnb with the given handle was released.
   * 
   * <p>Client must call this whenever it releases an Lnb.
   * 
   * <p><strong>Note:</strong> {@link #setLnbInfos(int[])} must be called before this release.
   * 
   * @param lnbHandle the handle of the released Tuner Lnb.
   * @param clientId the id of the client that is releasing the lnb.
   */
  public void releaseLnb(int lnbHandle, int clientId) throws android.os.RemoteException;
  /**
   * Compare two clients' priority.
   * 
   * @param challengerProfile the {@link ResourceClientProfile} of the challenger.
   * @param holderProfile the {@link ResourceClientProfile} of the holder of the resource.
   * 
   * @return true if the challenger has higher priority than the holder.
   */
  public boolean isHigherPriority(android.media.tv.tunerresourcemanager.ResourceClientProfile challengerProfile, android.media.tv.tunerresourcemanager.ResourceClientProfile holderProfile) throws android.os.RemoteException;
  /**
   * Stores Frontend resource map for the later restore.
   * 
   * <p>This is API is only for testing purpose and should be used in pair with
   * restoreResourceMap(), which allows testing of {@link Tuner} APIs
   * that behave differently based on different sets of resource map.
   * 
   * @param resourceType The resource type to store the map for.
   */
  public void storeResourceMap(int resourceType) throws android.os.RemoteException;
  /**
   * Clears the frontend resource map.
   * 
   * <p>This is API is only for testing purpose and should be called right after
   * storeResourceMap(), so TRMService#removeFrontendResource() does not
   * get called in TRMService#setFrontendInfoListInternal() for custom frontend
   * resource map creation.
   * 
   * @param resourceType The resource type to clear the map for.
   */
  public void clearResourceMap(int resourceType) throws android.os.RemoteException;
  /**
   * Restores Frontend resource map if it was stored before.
   * 
   * <p>This is API is only for testing purpose and should be used in pair with
   * storeResourceMap(), which allows testing of {@link Tuner} APIs
   * that behave differently based on different sets of resource map.
   * 
   * @param resourceType The resource type to restore the map for.
   */
  public void restoreResourceMap(int resourceType) throws android.os.RemoteException;
  /**
   * Grants the lock to the caller for public {@link Tuner} APIs
   * 
   * <p>{@link Tuner} functions that call both [@link TunerResourceManager} APIs and
   * grabs lock that are also used in {@link IResourcesReclaimListener#onReclaimResources()}
   * must call this API before acquiring lock used in onReclaimResources().
   * 
   * <p>This API will block until it releases the lock or fails
   * 
   * @param clientId The ID of the caller.
   * 
   * @return true if the lock is granted. If false is returned, calling this API again is not
   * guaranteed to work and may be unrecoverrable. (This should not happen.)
   */
  public boolean acquireLock(int clientId, long clientThreadId) throws android.os.RemoteException;
  /**
   * Releases the lock to the caller for public {@link Tuner} APIs
   * 
   * <p>This API must be called in pair with {@link #acquireLock(int, int)}
   * 
   * <p>This API will block until it releases the lock or fails
   * 
   * @param clientId The ID of the caller.
   * 
   * @return true if the lock is granted. If false is returned, calling this API again is not
   * guaranteed to work and may be unrecoverrable. (This should not happen.)
   */
  public boolean releaseLock(int clientId) throws android.os.RemoteException;
  /**
   * Returns a priority for the given use case type and the client's foreground or background
   * status.
   * 
   * @param useCase the use case type of the client. When the given use case type is invalid,
   *        the default use case type will be used. {@see TvInputService#PriorityHintUseCaseType}.
   * @param pid the pid of the client. When the pid is invalid, background status will be used as
   *        a client's status. Otherwise, client's app corresponding to the given session id will
   *        be used as a client. {@see TvInputService#onCreateSession(String, String)}.
   * 
   * @return the client priority..
   */
  public int getClientPriority(int useCase, int pid) throws android.os.RemoteException;
  /**
   * Returns a config priority for the given use case type and the foreground or background
   * status.
   * 
   * @param useCase the use case type of the client. When the given use case type is invalid,
   *        the default use case type will be used. {@see TvInputService#PriorityHintUseCaseType}.
   * @param isForeground {@code true} if foreground, {@code false} otherwise.
   * 
   * @return the config priority.
   */
  public int getConfigPriority(int useCase, boolean isForeground) throws android.os.RemoteException;
}
