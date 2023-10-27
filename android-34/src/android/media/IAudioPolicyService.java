/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * IAudioPolicyService interface (see AudioPolicyInterface for method descriptions).
 * 
 * {@hide}
 */
public interface IAudioPolicyService extends android.os.IInterface
{
  /** Default implementation for IAudioPolicyService. */
  public static class Default implements android.media.IAudioPolicyService
  {
    @Override public void onNewAudioModulesAvailable() throws android.os.RemoteException
    {
    }
    @Override public void setDeviceConnectionState(int state, android.media.audio.common.AudioPort port, android.media.audio.common.AudioFormatDescription encodedFormat) throws android.os.RemoteException
    {
    }
    @Override public int getDeviceConnectionState(android.media.audio.common.AudioDevice device) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void handleDeviceConfigChange(android.media.audio.common.AudioDevice device, java.lang.String deviceName, android.media.audio.common.AudioFormatDescription encodedFormat) throws android.os.RemoteException
    {
    }
    @Override public void setPhoneState(int state, int uid) throws android.os.RemoteException
    {
    }
    @Override public void setForceUse(int usage, int config) throws android.os.RemoteException
    {
    }
    @Override public int getForceUse(int usage) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int getOutput(int stream) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public android.media.GetOutputForAttrResponse getOutputForAttr(android.media.AudioAttributesInternal attr, int session, android.content.AttributionSourceState attributionSource, android.media.audio.common.AudioConfig config, int flags, int selectedDeviceId) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void startOutput(int portId) throws android.os.RemoteException
    {
    }
    @Override public void stopOutput(int portId) throws android.os.RemoteException
    {
    }
    @Override public void releaseOutput(int portId) throws android.os.RemoteException
    {
    }
    @Override public android.media.GetInputForAttrResponse getInputForAttr(android.media.AudioAttributesInternal attr, int input, int riid, int session, android.content.AttributionSourceState attributionSource, android.media.audio.common.AudioConfigBase config, int flags, int selectedDeviceId) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void startInput(int portId) throws android.os.RemoteException
    {
    }
    @Override public void stopInput(int portId) throws android.os.RemoteException
    {
    }
    @Override public void releaseInput(int portId) throws android.os.RemoteException
    {
    }
    @Override public void initStreamVolume(int stream, int indexMin, int indexMax) throws android.os.RemoteException
    {
    }
    @Override public void setStreamVolumeIndex(int stream, android.media.audio.common.AudioDeviceDescription device, int index) throws android.os.RemoteException
    {
    }
    @Override public int getStreamVolumeIndex(int stream, android.media.audio.common.AudioDeviceDescription device) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void setVolumeIndexForAttributes(android.media.AudioAttributesInternal attr, android.media.audio.common.AudioDeviceDescription device, int index) throws android.os.RemoteException
    {
    }
    @Override public int getVolumeIndexForAttributes(android.media.AudioAttributesInternal attr, android.media.audio.common.AudioDeviceDescription device) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int getMaxVolumeIndexForAttributes(android.media.AudioAttributesInternal attr) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int getMinVolumeIndexForAttributes(android.media.AudioAttributesInternal attr) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int getStrategyForStream(int stream) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public android.media.audio.common.AudioDevice[] getDevicesForAttributes(android.media.AudioAttributesInternal attr, boolean forVolume) throws android.os.RemoteException
    {
      return null;
    }
    @Override public int getOutputForEffect(android.media.EffectDescriptor desc) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void registerEffect(android.media.EffectDescriptor desc, int io, int strategy, int session, int id) throws android.os.RemoteException
    {
    }
    @Override public void unregisterEffect(int id) throws android.os.RemoteException
    {
    }
    @Override public void setEffectEnabled(int id, boolean enabled) throws android.os.RemoteException
    {
    }
    @Override public void moveEffectsToIo(int[] ids, int io) throws android.os.RemoteException
    {
    }
    @Override public boolean isStreamActive(int stream, int inPastMs) throws android.os.RemoteException
    {
      return false;
    }
    @Override public boolean isStreamActiveRemotely(int stream, int inPastMs) throws android.os.RemoteException
    {
      return false;
    }
    @Override public boolean isSourceActive(int source) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * On input, count represents the maximum length of the returned array.
     * On output, count is the total number of elements, which may be larger than the array size.
     * Passing '0' on input and inspecting the value on output is a common way of determining the
     * number of elements without actually retrieving them.
     */
    @Override public android.media.EffectDescriptor[] queryDefaultPreProcessing(int audioSession, android.media.audio.common.Int count) throws android.os.RemoteException
    {
      return null;
    }
    @Override public int addSourceDefaultEffect(android.media.audio.common.AudioUuid type, java.lang.String opPackageName, android.media.audio.common.AudioUuid uuid, int priority, int source) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int addStreamDefaultEffect(android.media.audio.common.AudioUuid type, java.lang.String opPackageName, android.media.audio.common.AudioUuid uuid, int priority, int usage) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void removeSourceDefaultEffect(int id) throws android.os.RemoteException
    {
    }
    @Override public void removeStreamDefaultEffect(int id) throws android.os.RemoteException
    {
    }
    @Override public void setSupportedSystemUsages(int[] systemUsages) throws android.os.RemoteException
    {
    }
    @Override public void setAllowedCapturePolicy(int uid, int capturePolicy) throws android.os.RemoteException
    {
    }
    /**
     * Check if offload is possible for given format, stream type, sample rate,
     * bit rate, duration, video and streaming or offload property is enabled.
     */
    @Override public int getOffloadSupport(android.media.audio.common.AudioOffloadInfo info) throws android.os.RemoteException
    {
      return 0;
    }
    /** Check if direct playback is possible for given format, sample rate, channel mask and flags. */
    @Override public boolean isDirectOutputSupported(android.media.audio.common.AudioConfigBase config, android.media.AudioAttributesInternal attributes) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * List currently attached audio ports and their attributes. Returns the generation.
     * The generation is incremented each time when anything changes in the ports
     * configuration.
     * 
     * On input, count represents the maximum length of the returned array.
     * On output, count is the total number of elements, which may be larger than the array size.
     * Passing '0' on input and inspecting the value on output is a common way of determining the
     * number of elements without actually retrieving them.
     */
    @Override public int listAudioPorts(int role, int type, android.media.audio.common.Int count, android.media.AudioPortFw[] ports) throws android.os.RemoteException
    {
      return 0;
    }
    /**
     * List all device ports declared in the configuration (including currently detached ones)
     * 'role' can be 'NONE' to get both input and output devices,
     * 'SINK' for output devices, and 'SOURCE' for input devices.
     */
    @Override public android.media.AudioPortFw[] listDeclaredDevicePorts(int role) throws android.os.RemoteException
    {
      return null;
    }
    /** Get attributes for the audio port with the given id (AudioPort.hal.id field). */
    @Override public android.media.AudioPortFw getAudioPort(int portId) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Create an audio patch between several source and sink ports.
     * The handle argument is used when updating an existing patch.
     */
    @Override public int createAudioPatch(android.media.AudioPatchFw patch, int handle) throws android.os.RemoteException
    {
      return 0;
    }
    /** Release an audio patch. */
    @Override public void releaseAudioPatch(int handle) throws android.os.RemoteException
    {
    }
    /**
     * List existing audio patches. Returns the generation.
     * 
     * On input, count represents the maximum length of the returned array.
     * On output, count is the total number of elements, which may be larger than the array size.
     * Passing '0' on input and inspecting the value on output is a common way of determining the
     * number of elements without actually retrieving them.
     */
    @Override public int listAudioPatches(android.media.audio.common.Int count, android.media.AudioPatchFw[] patches) throws android.os.RemoteException
    {
      return 0;
    }
    /** Set audio port configuration. */
    @Override public void setAudioPortConfig(android.media.AudioPortConfigFw config) throws android.os.RemoteException
    {
    }
    @Override public void registerClient(android.media.IAudioPolicyServiceClient client) throws android.os.RemoteException
    {
    }
    @Override public void setAudioPortCallbacksEnabled(boolean enabled) throws android.os.RemoteException
    {
    }
    @Override public void setAudioVolumeGroupCallbacksEnabled(boolean enabled) throws android.os.RemoteException
    {
    }
    @Override public android.media.SoundTriggerSession acquireSoundTriggerSession() throws android.os.RemoteException
    {
      return null;
    }
    @Override public void releaseSoundTriggerSession(int session) throws android.os.RemoteException
    {
    }
    @Override public int getPhoneState() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void registerPolicyMixes(android.media.AudioMix[] mixes, boolean registration) throws android.os.RemoteException
    {
    }
    @Override public void setUidDeviceAffinities(int uid, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException
    {
    }
    @Override public void removeUidDeviceAffinities(int uid) throws android.os.RemoteException
    {
    }
    @Override public void setUserIdDeviceAffinities(int userId, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException
    {
    }
    @Override public void removeUserIdDeviceAffinities(int userId) throws android.os.RemoteException
    {
    }
    @Override public int startAudioSource(android.media.AudioPortConfigFw source, android.media.AudioAttributesInternal attributes) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void stopAudioSource(int portId) throws android.os.RemoteException
    {
    }
    @Override public void setMasterMono(boolean mono) throws android.os.RemoteException
    {
    }
    @Override public boolean getMasterMono() throws android.os.RemoteException
    {
      return false;
    }
    @Override public float getStreamVolumeDB(int stream, int index, android.media.audio.common.AudioDeviceDescription device) throws android.os.RemoteException
    {
      return 0.0f;
    }
    /**
     * Populates supported surround formats and their enabled state in formats and formatsEnabled.
     * 
     * On input, count represents the maximum length of the returned array.
     * On output, count is the total number of elements, which may be larger than the array size.
     * Passing '0' on input and inspecting the value on output is a common way of determining the
     * number of elements without actually retrieving them.
     */
    @Override public void getSurroundFormats(android.media.audio.common.Int count, android.media.audio.common.AudioFormatDescription[] formats, boolean[] formatsEnabled) throws android.os.RemoteException
    {
    }
    /**
     * Populates the surround formats reported by the HDMI devices in formats.
     * 
     * On input, count represents the maximum length of the returned array.
     * On output, count is the total number of elements, which may be larger than the array size.
     * Passing '0' on input and inspecting the value on output is a common way of determining the
     * number of elements without actually retrieving them.
     */
    @Override public void getReportedSurroundFormats(android.media.audio.common.Int count, android.media.audio.common.AudioFormatDescription[] formats) throws android.os.RemoteException
    {
    }
    @Override public android.media.audio.common.AudioFormatDescription[] getHwOffloadFormatsSupportedForBluetoothMedia(android.media.audio.common.AudioDeviceDescription device) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void setSurroundFormatEnabled(android.media.audio.common.AudioFormatDescription audioFormat, boolean enabled) throws android.os.RemoteException
    {
    }
    @Override public void setAssistantServicesUids(int[] uids) throws android.os.RemoteException
    {
    }
    @Override public void setActiveAssistantServicesUids(int[] activeUids) throws android.os.RemoteException
    {
    }
    @Override public void setA11yServicesUids(int[] uids) throws android.os.RemoteException
    {
    }
    @Override public void setCurrentImeUid(int uid) throws android.os.RemoteException
    {
    }
    @Override public boolean isHapticPlaybackSupported() throws android.os.RemoteException
    {
      return false;
    }
    @Override public boolean isUltrasoundSupported() throws android.os.RemoteException
    {
      return false;
    }
    /**
     * Queries if there is hardware support for requesting audio capture content from
     * the DSP hotword pipeline.
     * 
     * @param lookbackAudio true if additionally querying for the ability to capture audio
     *                      from the pipeline prior to capture stream open.
     */
    @Override public boolean isHotwordStreamSupported(boolean lookbackAudio) throws android.os.RemoteException
    {
      return false;
    }
    @Override public android.media.AudioProductStrategy[] listAudioProductStrategies() throws android.os.RemoteException
    {
      return null;
    }
    @Override public int getProductStrategyFromAudioAttributes(android.media.AudioAttributesInternal aa, boolean fallbackOnDefault) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public android.media.AudioVolumeGroup[] listAudioVolumeGroups() throws android.os.RemoteException
    {
      return null;
    }
    @Override public int getVolumeGroupFromAudioAttributes(android.media.AudioAttributesInternal aa, boolean fallbackOnDefault) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void setRttEnabled(boolean enabled) throws android.os.RemoteException
    {
    }
    @Override public boolean isCallScreenModeSupported() throws android.os.RemoteException
    {
      return false;
    }
    @Override public void setDevicesRoleForStrategy(int strategy, int role, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException
    {
    }
    @Override public void removeDevicesRoleForStrategy(int strategy, int role, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException
    {
    }
    @Override public void clearDevicesRoleForStrategy(int strategy, int role) throws android.os.RemoteException
    {
    }
    @Override public android.media.audio.common.AudioDevice[] getDevicesForRoleAndStrategy(int strategy, int role) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void setDevicesRoleForCapturePreset(int audioSource, int role, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException
    {
    }
    @Override public void addDevicesRoleForCapturePreset(int audioSource, int role, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException
    {
    }
    @Override public void removeDevicesRoleForCapturePreset(int audioSource, int role, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException
    {
    }
    @Override public void clearDevicesRoleForCapturePreset(int audioSource, int role) throws android.os.RemoteException
    {
    }
    @Override public android.media.audio.common.AudioDevice[] getDevicesForRoleAndCapturePreset(int audioSource, int role) throws android.os.RemoteException
    {
      return null;
    }
    @Override public boolean registerSoundTriggerCaptureStateListener(android.media.ICaptureStateListener listener) throws android.os.RemoteException
    {
      return false;
    }
    /**
     * If a spatializer stage effect is present on the platform, this will return an
     * ISpatializer interface (see GetSpatializerResponse,aidl) to control this
     * feature.
     * If no spatializer stage is present, a null interface is returned.
     * The INativeSpatializerCallback passed must not be null.
     * Only one ISpatializer interface can exist at a given time. The native audio policy
     * service will reject the request if an interface was already acquired and previous owner
     * did not die or call ISpatializer.release().
     */
    @Override public android.media.GetSpatializerResponse getSpatializer(android.media.INativeSpatializerCallback callback) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Queries if some kind of spatialization will be performed if the audio playback context
     * described by the provided arguments is present.
     * The context is made of:
     * - The audio attributes describing the playback use case.
     * - The audio configuration describing the audio format, channels, sampling rate...
     * - The devices describing the sink audio device selected for playback.
     * All arguments are optional and only the specified arguments are used to match against
     * supported criteria. For instance, supplying no argument will tell if spatialization is
     * supported or not in general.
     */
    @Override public boolean canBeSpatialized(android.media.AudioAttributesInternal attr, android.media.audio.common.AudioConfig config, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException
    {
      return false;
    }
    /** Query how the direct playback is currently supported on the device. */
    @Override public int getDirectPlaybackSupport(android.media.AudioAttributesInternal attr, android.media.audio.common.AudioConfig config) throws android.os.RemoteException
    {
      return 0;
    }
    /**
     * Query audio profiles available for direct playback on the current output device(s)
     * for the specified audio attributes.
     */
    @Override public android.media.audio.common.AudioProfile[] getDirectProfilesForAttributes(android.media.AudioAttributesInternal attr) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Return a list of AudioMixerAttributes that can be used to set preferred mixer attributes
     * for the given device.
     */
    @Override public android.media.AudioMixerAttributesInternal[] getSupportedMixerAttributes(int portId) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Set preferred mixer attributes for a given device on a given audio attributes.
     * When conflicting requests are received, the last request will be honored.
     * The preferred mixer attributes can only be set when 1) the usage is media, 2) the
     * given device is currently available, 3) the given device is usb device, 4) the given mixer
     * attributes is supported by the given device.
     * 
     * @param attr the audio attributes whose mixer attributes should be set.
     * @param portId the port id of the device to be routed.
     * @param uid the uid of the request client. The uid will be used to recognize the ownership for
     *            the preferred mixer attributes. All the playback with same audio attributes from
     *            the same uid will be attached to the mixer with the preferred attributes if the
     *            playback is routed to the given device.
     * @param mixerAttr the preferred mixer attributes.
     */
    @Override public void setPreferredMixerAttributes(android.media.AudioAttributesInternal attr, int portId, int uid, android.media.AudioMixerAttributesInternal mixerAttr) throws android.os.RemoteException
    {
    }
    /**
     * Get preferred mixer attributes for a given device on a given audio attributes.
     * Null will be returned if there is no preferred mixer attributes set or it has
     * been cleared.
     * 
     * @param attr the audio attributes whose mixer attributes should be set.
     * @param portId the port id of the device to be routed.
     */
    @Override public android.media.AudioMixerAttributesInternal getPreferredMixerAttributes(android.media.AudioAttributesInternal attr, int portId) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Clear preferred mixer attributes for a given device on a given audio attributes that
     * is previously set via setPreferredMixerAttributes.
     * 
     * @param attr the audio attributes whose mixer attributes should be set.
     * @param portId the port id of the device to be routed.
     * @param uid the uid of the request client. The uid is used to identify the ownership for the
     *            preferred mixer attributes. The preferred mixer attributes will only be cleared
     *            if the uid is the same as the owner of current preferred mixer attributes.
     */
    @Override public void clearPreferredMixerAttributes(android.media.AudioAttributesInternal attr, int portId, int uid) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.media.IAudioPolicyService
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.media.IAudioPolicyService interface,
     * generating a proxy if needed.
     */
    public static android.media.IAudioPolicyService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.media.IAudioPolicyService))) {
        return ((android.media.IAudioPolicyService)iin);
      }
      return new android.media.IAudioPolicyService.Stub.Proxy(obj);
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
        case TRANSACTION_onNewAudioModulesAvailable:
        {
          this.onNewAudioModulesAvailable();
          break;
        }
        case TRANSACTION_setDeviceConnectionState:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.media.audio.common.AudioPort _arg1;
          _arg1 = data.readTypedObject(android.media.audio.common.AudioPort.CREATOR);
          android.media.audio.common.AudioFormatDescription _arg2;
          _arg2 = data.readTypedObject(android.media.audio.common.AudioFormatDescription.CREATOR);
          data.enforceNoDataAvail();
          this.setDeviceConnectionState(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getDeviceConnectionState:
        {
          android.media.audio.common.AudioDevice _arg0;
          _arg0 = data.readTypedObject(android.media.audio.common.AudioDevice.CREATOR);
          data.enforceNoDataAvail();
          int _result = this.getDeviceConnectionState(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_handleDeviceConfigChange:
        {
          android.media.audio.common.AudioDevice _arg0;
          _arg0 = data.readTypedObject(android.media.audio.common.AudioDevice.CREATOR);
          java.lang.String _arg1;
          _arg1 = data.readString();
          android.media.audio.common.AudioFormatDescription _arg2;
          _arg2 = data.readTypedObject(android.media.audio.common.AudioFormatDescription.CREATOR);
          data.enforceNoDataAvail();
          this.handleDeviceConfigChange(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setPhoneState:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.setPhoneState(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setForceUse:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.setForceUse(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getForceUse:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          int _result = this.getForceUse(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getOutput:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          int _result = this.getOutput(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getOutputForAttr:
        {
          android.media.AudioAttributesInternal _arg0;
          _arg0 = data.readTypedObject(android.media.AudioAttributesInternal.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          android.content.AttributionSourceState _arg2;
          _arg2 = data.readTypedObject(android.content.AttributionSourceState.CREATOR);
          android.media.audio.common.AudioConfig _arg3;
          _arg3 = data.readTypedObject(android.media.audio.common.AudioConfig.CREATOR);
          int _arg4;
          _arg4 = data.readInt();
          int _arg5;
          _arg5 = data.readInt();
          data.enforceNoDataAvail();
          android.media.GetOutputForAttrResponse _result = this.getOutputForAttr(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_startOutput:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.startOutput(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_stopOutput:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.stopOutput(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_releaseOutput:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.releaseOutput(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getInputForAttr:
        {
          android.media.AudioAttributesInternal _arg0;
          _arg0 = data.readTypedObject(android.media.AudioAttributesInternal.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          int _arg3;
          _arg3 = data.readInt();
          android.content.AttributionSourceState _arg4;
          _arg4 = data.readTypedObject(android.content.AttributionSourceState.CREATOR);
          android.media.audio.common.AudioConfigBase _arg5;
          _arg5 = data.readTypedObject(android.media.audio.common.AudioConfigBase.CREATOR);
          int _arg6;
          _arg6 = data.readInt();
          int _arg7;
          _arg7 = data.readInt();
          data.enforceNoDataAvail();
          android.media.GetInputForAttrResponse _result = this.getInputForAttr(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_startInput:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.startInput(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_stopInput:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.stopInput(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_releaseInput:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.releaseInput(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_initStreamVolume:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.initStreamVolume(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setStreamVolumeIndex:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.media.audio.common.AudioDeviceDescription _arg1;
          _arg1 = data.readTypedObject(android.media.audio.common.AudioDeviceDescription.CREATOR);
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.setStreamVolumeIndex(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getStreamVolumeIndex:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.media.audio.common.AudioDeviceDescription _arg1;
          _arg1 = data.readTypedObject(android.media.audio.common.AudioDeviceDescription.CREATOR);
          data.enforceNoDataAvail();
          int _result = this.getStreamVolumeIndex(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_setVolumeIndexForAttributes:
        {
          android.media.AudioAttributesInternal _arg0;
          _arg0 = data.readTypedObject(android.media.AudioAttributesInternal.CREATOR);
          android.media.audio.common.AudioDeviceDescription _arg1;
          _arg1 = data.readTypedObject(android.media.audio.common.AudioDeviceDescription.CREATOR);
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.setVolumeIndexForAttributes(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getVolumeIndexForAttributes:
        {
          android.media.AudioAttributesInternal _arg0;
          _arg0 = data.readTypedObject(android.media.AudioAttributesInternal.CREATOR);
          android.media.audio.common.AudioDeviceDescription _arg1;
          _arg1 = data.readTypedObject(android.media.audio.common.AudioDeviceDescription.CREATOR);
          data.enforceNoDataAvail();
          int _result = this.getVolumeIndexForAttributes(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getMaxVolumeIndexForAttributes:
        {
          android.media.AudioAttributesInternal _arg0;
          _arg0 = data.readTypedObject(android.media.AudioAttributesInternal.CREATOR);
          data.enforceNoDataAvail();
          int _result = this.getMaxVolumeIndexForAttributes(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getMinVolumeIndexForAttributes:
        {
          android.media.AudioAttributesInternal _arg0;
          _arg0 = data.readTypedObject(android.media.AudioAttributesInternal.CREATOR);
          data.enforceNoDataAvail();
          int _result = this.getMinVolumeIndexForAttributes(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getStrategyForStream:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          int _result = this.getStrategyForStream(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getDevicesForAttributes:
        {
          android.media.AudioAttributesInternal _arg0;
          _arg0 = data.readTypedObject(android.media.AudioAttributesInternal.CREATOR);
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          android.media.audio.common.AudioDevice[] _result = this.getDevicesForAttributes(_arg0, _arg1);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getOutputForEffect:
        {
          android.media.EffectDescriptor _arg0;
          _arg0 = data.readTypedObject(android.media.EffectDescriptor.CREATOR);
          data.enforceNoDataAvail();
          int _result = this.getOutputForEffect(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_registerEffect:
        {
          android.media.EffectDescriptor _arg0;
          _arg0 = data.readTypedObject(android.media.EffectDescriptor.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          int _arg3;
          _arg3 = data.readInt();
          int _arg4;
          _arg4 = data.readInt();
          data.enforceNoDataAvail();
          this.registerEffect(_arg0, _arg1, _arg2, _arg3, _arg4);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_unregisterEffect:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.unregisterEffect(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setEffectEnabled:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setEffectEnabled(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_moveEffectsToIo:
        {
          int[] _arg0;
          _arg0 = data.createIntArray();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.moveEffectsToIo(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_isStreamActive:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          boolean _result = this.isStreamActive(_arg0, _arg1);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_isStreamActiveRemotely:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          boolean _result = this.isStreamActiveRemotely(_arg0, _arg1);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_isSourceActive:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          boolean _result = this.isSourceActive(_arg0);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_queryDefaultPreProcessing:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.media.audio.common.Int _arg1;
          _arg1 = data.readTypedObject(android.media.audio.common.Int.CREATOR);
          data.enforceNoDataAvail();
          android.media.EffectDescriptor[] _result = this.queryDefaultPreProcessing(_arg0, _arg1);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          reply.writeTypedObject(_arg1, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_addSourceDefaultEffect:
        {
          android.media.audio.common.AudioUuid _arg0;
          _arg0 = data.readTypedObject(android.media.audio.common.AudioUuid.CREATOR);
          java.lang.String _arg1;
          _arg1 = data.readString();
          android.media.audio.common.AudioUuid _arg2;
          _arg2 = data.readTypedObject(android.media.audio.common.AudioUuid.CREATOR);
          int _arg3;
          _arg3 = data.readInt();
          int _arg4;
          _arg4 = data.readInt();
          data.enforceNoDataAvail();
          int _result = this.addSourceDefaultEffect(_arg0, _arg1, _arg2, _arg3, _arg4);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_addStreamDefaultEffect:
        {
          android.media.audio.common.AudioUuid _arg0;
          _arg0 = data.readTypedObject(android.media.audio.common.AudioUuid.CREATOR);
          java.lang.String _arg1;
          _arg1 = data.readString();
          android.media.audio.common.AudioUuid _arg2;
          _arg2 = data.readTypedObject(android.media.audio.common.AudioUuid.CREATOR);
          int _arg3;
          _arg3 = data.readInt();
          int _arg4;
          _arg4 = data.readInt();
          data.enforceNoDataAvail();
          int _result = this.addStreamDefaultEffect(_arg0, _arg1, _arg2, _arg3, _arg4);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_removeSourceDefaultEffect:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.removeSourceDefaultEffect(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_removeStreamDefaultEffect:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.removeStreamDefaultEffect(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setSupportedSystemUsages:
        {
          int[] _arg0;
          _arg0 = data.createIntArray();
          data.enforceNoDataAvail();
          this.setSupportedSystemUsages(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setAllowedCapturePolicy:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.setAllowedCapturePolicy(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getOffloadSupport:
        {
          android.media.audio.common.AudioOffloadInfo _arg0;
          _arg0 = data.readTypedObject(android.media.audio.common.AudioOffloadInfo.CREATOR);
          data.enforceNoDataAvail();
          int _result = this.getOffloadSupport(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_isDirectOutputSupported:
        {
          android.media.audio.common.AudioConfigBase _arg0;
          _arg0 = data.readTypedObject(android.media.audio.common.AudioConfigBase.CREATOR);
          android.media.AudioAttributesInternal _arg1;
          _arg1 = data.readTypedObject(android.media.AudioAttributesInternal.CREATOR);
          data.enforceNoDataAvail();
          boolean _result = this.isDirectOutputSupported(_arg0, _arg1);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_listAudioPorts:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          android.media.audio.common.Int _arg2;
          _arg2 = data.readTypedObject(android.media.audio.common.Int.CREATOR);
          android.media.AudioPortFw[] _arg3;
          int _arg3_length = data.readInt();
          if (_arg3_length < 0) {
            _arg3 = null;
          } else {
            _arg3 = new android.media.AudioPortFw[_arg3_length];
          }
          data.enforceNoDataAvail();
          int _result = this.listAudioPorts(_arg0, _arg1, _arg2, _arg3);
          reply.writeNoException();
          reply.writeInt(_result);
          reply.writeTypedObject(_arg2, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          reply.writeTypedArray(_arg3, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_listDeclaredDevicePorts:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          android.media.AudioPortFw[] _result = this.listDeclaredDevicePorts(_arg0);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getAudioPort:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          android.media.AudioPortFw _result = this.getAudioPort(_arg0);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_createAudioPatch:
        {
          android.media.AudioPatchFw _arg0;
          _arg0 = data.readTypedObject(android.media.AudioPatchFw.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          int _result = this.createAudioPatch(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_releaseAudioPatch:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.releaseAudioPatch(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_listAudioPatches:
        {
          android.media.audio.common.Int _arg0;
          _arg0 = data.readTypedObject(android.media.audio.common.Int.CREATOR);
          android.media.AudioPatchFw[] _arg1;
          int _arg1_length = data.readInt();
          if (_arg1_length < 0) {
            _arg1 = null;
          } else {
            _arg1 = new android.media.AudioPatchFw[_arg1_length];
          }
          data.enforceNoDataAvail();
          int _result = this.listAudioPatches(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(_result);
          reply.writeTypedObject(_arg0, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          reply.writeTypedArray(_arg1, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_setAudioPortConfig:
        {
          android.media.AudioPortConfigFw _arg0;
          _arg0 = data.readTypedObject(android.media.AudioPortConfigFw.CREATOR);
          data.enforceNoDataAvail();
          this.setAudioPortConfig(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_registerClient:
        {
          android.media.IAudioPolicyServiceClient _arg0;
          _arg0 = android.media.IAudioPolicyServiceClient.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.registerClient(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setAudioPortCallbacksEnabled:
        {
          boolean _arg0;
          _arg0 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setAudioPortCallbacksEnabled(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setAudioVolumeGroupCallbacksEnabled:
        {
          boolean _arg0;
          _arg0 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setAudioVolumeGroupCallbacksEnabled(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_acquireSoundTriggerSession:
        {
          android.media.SoundTriggerSession _result = this.acquireSoundTriggerSession();
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_releaseSoundTriggerSession:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.releaseSoundTriggerSession(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getPhoneState:
        {
          int _result = this.getPhoneState();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_registerPolicyMixes:
        {
          android.media.AudioMix[] _arg0;
          _arg0 = data.createTypedArray(android.media.AudioMix.CREATOR);
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.registerPolicyMixes(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setUidDeviceAffinities:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.media.audio.common.AudioDevice[] _arg1;
          _arg1 = data.createTypedArray(android.media.audio.common.AudioDevice.CREATOR);
          data.enforceNoDataAvail();
          this.setUidDeviceAffinities(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_removeUidDeviceAffinities:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.removeUidDeviceAffinities(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setUserIdDeviceAffinities:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.media.audio.common.AudioDevice[] _arg1;
          _arg1 = data.createTypedArray(android.media.audio.common.AudioDevice.CREATOR);
          data.enforceNoDataAvail();
          this.setUserIdDeviceAffinities(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_removeUserIdDeviceAffinities:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.removeUserIdDeviceAffinities(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_startAudioSource:
        {
          android.media.AudioPortConfigFw _arg0;
          _arg0 = data.readTypedObject(android.media.AudioPortConfigFw.CREATOR);
          android.media.AudioAttributesInternal _arg1;
          _arg1 = data.readTypedObject(android.media.AudioAttributesInternal.CREATOR);
          data.enforceNoDataAvail();
          int _result = this.startAudioSource(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_stopAudioSource:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.stopAudioSource(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setMasterMono:
        {
          boolean _arg0;
          _arg0 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setMasterMono(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getMasterMono:
        {
          boolean _result = this.getMasterMono();
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_getStreamVolumeDB:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          android.media.audio.common.AudioDeviceDescription _arg2;
          _arg2 = data.readTypedObject(android.media.audio.common.AudioDeviceDescription.CREATOR);
          data.enforceNoDataAvail();
          float _result = this.getStreamVolumeDB(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeFloat(_result);
          break;
        }
        case TRANSACTION_getSurroundFormats:
        {
          android.media.audio.common.Int _arg0;
          _arg0 = data.readTypedObject(android.media.audio.common.Int.CREATOR);
          android.media.audio.common.AudioFormatDescription[] _arg1;
          int _arg1_length = data.readInt();
          if (_arg1_length < 0) {
            _arg1 = null;
          } else {
            _arg1 = new android.media.audio.common.AudioFormatDescription[_arg1_length];
          }
          boolean[] _arg2;
          int _arg2_length = data.readInt();
          if (_arg2_length < 0) {
            _arg2 = null;
          } else {
            _arg2 = new boolean[_arg2_length];
          }
          data.enforceNoDataAvail();
          this.getSurroundFormats(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeTypedObject(_arg0, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          reply.writeTypedArray(_arg1, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          reply.writeBooleanArray(_arg2);
          break;
        }
        case TRANSACTION_getReportedSurroundFormats:
        {
          android.media.audio.common.Int _arg0;
          _arg0 = data.readTypedObject(android.media.audio.common.Int.CREATOR);
          android.media.audio.common.AudioFormatDescription[] _arg1;
          int _arg1_length = data.readInt();
          if (_arg1_length < 0) {
            _arg1 = null;
          } else {
            _arg1 = new android.media.audio.common.AudioFormatDescription[_arg1_length];
          }
          data.enforceNoDataAvail();
          this.getReportedSurroundFormats(_arg0, _arg1);
          reply.writeNoException();
          reply.writeTypedObject(_arg0, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          reply.writeTypedArray(_arg1, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getHwOffloadFormatsSupportedForBluetoothMedia:
        {
          android.media.audio.common.AudioDeviceDescription _arg0;
          _arg0 = data.readTypedObject(android.media.audio.common.AudioDeviceDescription.CREATOR);
          data.enforceNoDataAvail();
          android.media.audio.common.AudioFormatDescription[] _result = this.getHwOffloadFormatsSupportedForBluetoothMedia(_arg0);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_setSurroundFormatEnabled:
        {
          android.media.audio.common.AudioFormatDescription _arg0;
          _arg0 = data.readTypedObject(android.media.audio.common.AudioFormatDescription.CREATOR);
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setSurroundFormatEnabled(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setAssistantServicesUids:
        {
          int[] _arg0;
          _arg0 = data.createIntArray();
          data.enforceNoDataAvail();
          this.setAssistantServicesUids(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setActiveAssistantServicesUids:
        {
          int[] _arg0;
          _arg0 = data.createIntArray();
          data.enforceNoDataAvail();
          this.setActiveAssistantServicesUids(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setA11yServicesUids:
        {
          int[] _arg0;
          _arg0 = data.createIntArray();
          data.enforceNoDataAvail();
          this.setA11yServicesUids(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setCurrentImeUid:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.setCurrentImeUid(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_isHapticPlaybackSupported:
        {
          boolean _result = this.isHapticPlaybackSupported();
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_isUltrasoundSupported:
        {
          boolean _result = this.isUltrasoundSupported();
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_isHotwordStreamSupported:
        {
          boolean _arg0;
          _arg0 = data.readBoolean();
          data.enforceNoDataAvail();
          boolean _result = this.isHotwordStreamSupported(_arg0);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_listAudioProductStrategies:
        {
          android.media.AudioProductStrategy[] _result = this.listAudioProductStrategies();
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getProductStrategyFromAudioAttributes:
        {
          android.media.AudioAttributesInternal _arg0;
          _arg0 = data.readTypedObject(android.media.AudioAttributesInternal.CREATOR);
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          int _result = this.getProductStrategyFromAudioAttributes(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_listAudioVolumeGroups:
        {
          android.media.AudioVolumeGroup[] _result = this.listAudioVolumeGroups();
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getVolumeGroupFromAudioAttributes:
        {
          android.media.AudioAttributesInternal _arg0;
          _arg0 = data.readTypedObject(android.media.AudioAttributesInternal.CREATOR);
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          int _result = this.getVolumeGroupFromAudioAttributes(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_setRttEnabled:
        {
          boolean _arg0;
          _arg0 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setRttEnabled(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_isCallScreenModeSupported:
        {
          boolean _result = this.isCallScreenModeSupported();
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_setDevicesRoleForStrategy:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          android.media.audio.common.AudioDevice[] _arg2;
          _arg2 = data.createTypedArray(android.media.audio.common.AudioDevice.CREATOR);
          data.enforceNoDataAvail();
          this.setDevicesRoleForStrategy(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_removeDevicesRoleForStrategy:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          android.media.audio.common.AudioDevice[] _arg2;
          _arg2 = data.createTypedArray(android.media.audio.common.AudioDevice.CREATOR);
          data.enforceNoDataAvail();
          this.removeDevicesRoleForStrategy(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_clearDevicesRoleForStrategy:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.clearDevicesRoleForStrategy(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getDevicesForRoleAndStrategy:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          android.media.audio.common.AudioDevice[] _result = this.getDevicesForRoleAndStrategy(_arg0, _arg1);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_setDevicesRoleForCapturePreset:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          android.media.audio.common.AudioDevice[] _arg2;
          _arg2 = data.createTypedArray(android.media.audio.common.AudioDevice.CREATOR);
          data.enforceNoDataAvail();
          this.setDevicesRoleForCapturePreset(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_addDevicesRoleForCapturePreset:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          android.media.audio.common.AudioDevice[] _arg2;
          _arg2 = data.createTypedArray(android.media.audio.common.AudioDevice.CREATOR);
          data.enforceNoDataAvail();
          this.addDevicesRoleForCapturePreset(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_removeDevicesRoleForCapturePreset:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          android.media.audio.common.AudioDevice[] _arg2;
          _arg2 = data.createTypedArray(android.media.audio.common.AudioDevice.CREATOR);
          data.enforceNoDataAvail();
          this.removeDevicesRoleForCapturePreset(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_clearDevicesRoleForCapturePreset:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.clearDevicesRoleForCapturePreset(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getDevicesForRoleAndCapturePreset:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          android.media.audio.common.AudioDevice[] _result = this.getDevicesForRoleAndCapturePreset(_arg0, _arg1);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_registerSoundTriggerCaptureStateListener:
        {
          android.media.ICaptureStateListener _arg0;
          _arg0 = android.media.ICaptureStateListener.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          boolean _result = this.registerSoundTriggerCaptureStateListener(_arg0);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_getSpatializer:
        {
          android.media.INativeSpatializerCallback _arg0;
          _arg0 = android.media.INativeSpatializerCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          android.media.GetSpatializerResponse _result = this.getSpatializer(_arg0);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_canBeSpatialized:
        {
          android.media.AudioAttributesInternal _arg0;
          _arg0 = data.readTypedObject(android.media.AudioAttributesInternal.CREATOR);
          android.media.audio.common.AudioConfig _arg1;
          _arg1 = data.readTypedObject(android.media.audio.common.AudioConfig.CREATOR);
          android.media.audio.common.AudioDevice[] _arg2;
          _arg2 = data.createTypedArray(android.media.audio.common.AudioDevice.CREATOR);
          data.enforceNoDataAvail();
          boolean _result = this.canBeSpatialized(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        case TRANSACTION_getDirectPlaybackSupport:
        {
          android.media.AudioAttributesInternal _arg0;
          _arg0 = data.readTypedObject(android.media.AudioAttributesInternal.CREATOR);
          android.media.audio.common.AudioConfig _arg1;
          _arg1 = data.readTypedObject(android.media.audio.common.AudioConfig.CREATOR);
          data.enforceNoDataAvail();
          int _result = this.getDirectPlaybackSupport(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getDirectProfilesForAttributes:
        {
          android.media.AudioAttributesInternal _arg0;
          _arg0 = data.readTypedObject(android.media.AudioAttributesInternal.CREATOR);
          data.enforceNoDataAvail();
          android.media.audio.common.AudioProfile[] _result = this.getDirectProfilesForAttributes(_arg0);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getSupportedMixerAttributes:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          android.media.AudioMixerAttributesInternal[] _result = this.getSupportedMixerAttributes(_arg0);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_setPreferredMixerAttributes:
        {
          android.media.AudioAttributesInternal _arg0;
          _arg0 = data.readTypedObject(android.media.AudioAttributesInternal.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          android.media.AudioMixerAttributesInternal _arg3;
          _arg3 = data.readTypedObject(android.media.AudioMixerAttributesInternal.CREATOR);
          data.enforceNoDataAvail();
          this.setPreferredMixerAttributes(_arg0, _arg1, _arg2, _arg3);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getPreferredMixerAttributes:
        {
          android.media.AudioAttributesInternal _arg0;
          _arg0 = data.readTypedObject(android.media.AudioAttributesInternal.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          android.media.AudioMixerAttributesInternal _result = this.getPreferredMixerAttributes(_arg0, _arg1);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_clearPreferredMixerAttributes:
        {
          android.media.AudioAttributesInternal _arg0;
          _arg0 = data.readTypedObject(android.media.AudioAttributesInternal.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.clearPreferredMixerAttributes(_arg0, _arg1, _arg2);
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
    private static class Proxy implements android.media.IAudioPolicyService
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
      @Override public void onNewAudioModulesAvailable() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onNewAudioModulesAvailable, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setDeviceConnectionState(int state, android.media.audio.common.AudioPort port, android.media.audio.common.AudioFormatDescription encodedFormat) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(state);
          _data.writeTypedObject(port, 0);
          _data.writeTypedObject(encodedFormat, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setDeviceConnectionState, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public int getDeviceConnectionState(android.media.audio.common.AudioDevice device) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(device, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getDeviceConnectionState, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void handleDeviceConfigChange(android.media.audio.common.AudioDevice device, java.lang.String deviceName, android.media.audio.common.AudioFormatDescription encodedFormat) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(device, 0);
          _data.writeString(deviceName);
          _data.writeTypedObject(encodedFormat, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_handleDeviceConfigChange, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setPhoneState(int state, int uid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(state);
          _data.writeInt(uid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setPhoneState, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setForceUse(int usage, int config) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(usage);
          _data.writeInt(config);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setForceUse, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public int getForceUse(int usage) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(usage);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getForceUse, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int getOutput(int stream) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(stream);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getOutput, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.media.GetOutputForAttrResponse getOutputForAttr(android.media.AudioAttributesInternal attr, int session, android.content.AttributionSourceState attributionSource, android.media.audio.common.AudioConfig config, int flags, int selectedDeviceId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.GetOutputForAttrResponse _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(attr, 0);
          _data.writeInt(session);
          _data.writeTypedObject(attributionSource, 0);
          _data.writeTypedObject(config, 0);
          _data.writeInt(flags);
          _data.writeInt(selectedDeviceId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getOutputForAttr, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readTypedObject(android.media.GetOutputForAttrResponse.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void startOutput(int portId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(portId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startOutput, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void stopOutput(int portId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(portId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stopOutput, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void releaseOutput(int portId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(portId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_releaseOutput, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public android.media.GetInputForAttrResponse getInputForAttr(android.media.AudioAttributesInternal attr, int input, int riid, int session, android.content.AttributionSourceState attributionSource, android.media.audio.common.AudioConfigBase config, int flags, int selectedDeviceId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.GetInputForAttrResponse _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(attr, 0);
          _data.writeInt(input);
          _data.writeInt(riid);
          _data.writeInt(session);
          _data.writeTypedObject(attributionSource, 0);
          _data.writeTypedObject(config, 0);
          _data.writeInt(flags);
          _data.writeInt(selectedDeviceId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getInputForAttr, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readTypedObject(android.media.GetInputForAttrResponse.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void startInput(int portId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(portId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startInput, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void stopInput(int portId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(portId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stopInput, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void releaseInput(int portId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(portId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_releaseInput, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void initStreamVolume(int stream, int indexMin, int indexMax) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(stream);
          _data.writeInt(indexMin);
          _data.writeInt(indexMax);
          boolean _status = mRemote.transact(Stub.TRANSACTION_initStreamVolume, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setStreamVolumeIndex(int stream, android.media.audio.common.AudioDeviceDescription device, int index) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(stream);
          _data.writeTypedObject(device, 0);
          _data.writeInt(index);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setStreamVolumeIndex, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public int getStreamVolumeIndex(int stream, android.media.audio.common.AudioDeviceDescription device) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(stream);
          _data.writeTypedObject(device, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getStreamVolumeIndex, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void setVolumeIndexForAttributes(android.media.AudioAttributesInternal attr, android.media.audio.common.AudioDeviceDescription device, int index) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(attr, 0);
          _data.writeTypedObject(device, 0);
          _data.writeInt(index);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setVolumeIndexForAttributes, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public int getVolumeIndexForAttributes(android.media.AudioAttributesInternal attr, android.media.audio.common.AudioDeviceDescription device) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(attr, 0);
          _data.writeTypedObject(device, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getVolumeIndexForAttributes, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int getMaxVolumeIndexForAttributes(android.media.AudioAttributesInternal attr) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(attr, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getMaxVolumeIndexForAttributes, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int getMinVolumeIndexForAttributes(android.media.AudioAttributesInternal attr) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(attr, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getMinVolumeIndexForAttributes, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int getStrategyForStream(int stream) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(stream);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getStrategyForStream, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.media.audio.common.AudioDevice[] getDevicesForAttributes(android.media.AudioAttributesInternal attr, boolean forVolume) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.audio.common.AudioDevice[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(attr, 0);
          _data.writeBoolean(forVolume);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getDevicesForAttributes, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.media.audio.common.AudioDevice.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int getOutputForEffect(android.media.EffectDescriptor desc) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(desc, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getOutputForEffect, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void registerEffect(android.media.EffectDescriptor desc, int io, int strategy, int session, int id) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(desc, 0);
          _data.writeInt(io);
          _data.writeInt(strategy);
          _data.writeInt(session);
          _data.writeInt(id);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerEffect, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void unregisterEffect(int id) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(id);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterEffect, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setEffectEnabled(int id, boolean enabled) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(id);
          _data.writeBoolean(enabled);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setEffectEnabled, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void moveEffectsToIo(int[] ids, int io) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeIntArray(ids);
          _data.writeInt(io);
          boolean _status = mRemote.transact(Stub.TRANSACTION_moveEffectsToIo, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public boolean isStreamActive(int stream, int inPastMs) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(stream);
          _data.writeInt(inPastMs);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isStreamActive, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readBoolean();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean isStreamActiveRemotely(int stream, int inPastMs) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(stream);
          _data.writeInt(inPastMs);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isStreamActiveRemotely, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readBoolean();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean isSourceActive(int source) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(source);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isSourceActive, _data, _reply, 0);
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
       * On input, count represents the maximum length of the returned array.
       * On output, count is the total number of elements, which may be larger than the array size.
       * Passing '0' on input and inspecting the value on output is a common way of determining the
       * number of elements without actually retrieving them.
       */
      @Override public android.media.EffectDescriptor[] queryDefaultPreProcessing(int audioSession, android.media.audio.common.Int count) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.EffectDescriptor[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(audioSession);
          _data.writeTypedObject(count, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_queryDefaultPreProcessing, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.media.EffectDescriptor.CREATOR);
          if ((0!=_reply.readInt())) {
            count.readFromParcel(_reply);
          }
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int addSourceDefaultEffect(android.media.audio.common.AudioUuid type, java.lang.String opPackageName, android.media.audio.common.AudioUuid uuid, int priority, int source) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(type, 0);
          _data.writeString(opPackageName);
          _data.writeTypedObject(uuid, 0);
          _data.writeInt(priority);
          _data.writeInt(source);
          boolean _status = mRemote.transact(Stub.TRANSACTION_addSourceDefaultEffect, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int addStreamDefaultEffect(android.media.audio.common.AudioUuid type, java.lang.String opPackageName, android.media.audio.common.AudioUuid uuid, int priority, int usage) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(type, 0);
          _data.writeString(opPackageName);
          _data.writeTypedObject(uuid, 0);
          _data.writeInt(priority);
          _data.writeInt(usage);
          boolean _status = mRemote.transact(Stub.TRANSACTION_addStreamDefaultEffect, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void removeSourceDefaultEffect(int id) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(id);
          boolean _status = mRemote.transact(Stub.TRANSACTION_removeSourceDefaultEffect, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void removeStreamDefaultEffect(int id) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(id);
          boolean _status = mRemote.transact(Stub.TRANSACTION_removeStreamDefaultEffect, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setSupportedSystemUsages(int[] systemUsages) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeIntArray(systemUsages);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setSupportedSystemUsages, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setAllowedCapturePolicy(int uid, int capturePolicy) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(uid);
          _data.writeInt(capturePolicy);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setAllowedCapturePolicy, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Check if offload is possible for given format, stream type, sample rate,
       * bit rate, duration, video and streaming or offload property is enabled.
       */
      @Override public int getOffloadSupport(android.media.audio.common.AudioOffloadInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getOffloadSupport, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Check if direct playback is possible for given format, sample rate, channel mask and flags. */
      @Override public boolean isDirectOutputSupported(android.media.audio.common.AudioConfigBase config, android.media.AudioAttributesInternal attributes) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(config, 0);
          _data.writeTypedObject(attributes, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isDirectOutputSupported, _data, _reply, 0);
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
       * List currently attached audio ports and their attributes. Returns the generation.
       * The generation is incremented each time when anything changes in the ports
       * configuration.
       * 
       * On input, count represents the maximum length of the returned array.
       * On output, count is the total number of elements, which may be larger than the array size.
       * Passing '0' on input and inspecting the value on output is a common way of determining the
       * number of elements without actually retrieving them.
       */
      @Override public int listAudioPorts(int role, int type, android.media.audio.common.Int count, android.media.AudioPortFw[] ports) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(role);
          _data.writeInt(type);
          _data.writeTypedObject(count, 0);
          _data.writeInt(ports.length);
          boolean _status = mRemote.transact(Stub.TRANSACTION_listAudioPorts, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
          if ((0!=_reply.readInt())) {
            count.readFromParcel(_reply);
          }
          _reply.readTypedArray(ports, android.media.AudioPortFw.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * List all device ports declared in the configuration (including currently detached ones)
       * 'role' can be 'NONE' to get both input and output devices,
       * 'SINK' for output devices, and 'SOURCE' for input devices.
       */
      @Override public android.media.AudioPortFw[] listDeclaredDevicePorts(int role) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.AudioPortFw[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(role);
          boolean _status = mRemote.transact(Stub.TRANSACTION_listDeclaredDevicePorts, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.media.AudioPortFw.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Get attributes for the audio port with the given id (AudioPort.hal.id field). */
      @Override public android.media.AudioPortFw getAudioPort(int portId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.AudioPortFw _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(portId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getAudioPort, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readTypedObject(android.media.AudioPortFw.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Create an audio patch between several source and sink ports.
       * The handle argument is used when updating an existing patch.
       */
      @Override public int createAudioPatch(android.media.AudioPatchFw patch, int handle) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(patch, 0);
          _data.writeInt(handle);
          boolean _status = mRemote.transact(Stub.TRANSACTION_createAudioPatch, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Release an audio patch. */
      @Override public void releaseAudioPatch(int handle) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(handle);
          boolean _status = mRemote.transact(Stub.TRANSACTION_releaseAudioPatch, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * List existing audio patches. Returns the generation.
       * 
       * On input, count represents the maximum length of the returned array.
       * On output, count is the total number of elements, which may be larger than the array size.
       * Passing '0' on input and inspecting the value on output is a common way of determining the
       * number of elements without actually retrieving them.
       */
      @Override public int listAudioPatches(android.media.audio.common.Int count, android.media.AudioPatchFw[] patches) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(count, 0);
          _data.writeInt(patches.length);
          boolean _status = mRemote.transact(Stub.TRANSACTION_listAudioPatches, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
          if ((0!=_reply.readInt())) {
            count.readFromParcel(_reply);
          }
          _reply.readTypedArray(patches, android.media.AudioPatchFw.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Set audio port configuration. */
      @Override public void setAudioPortConfig(android.media.AudioPortConfigFw config) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(config, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setAudioPortConfig, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void registerClient(android.media.IAudioPolicyServiceClient client) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(client);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerClient, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setAudioPortCallbacksEnabled(boolean enabled) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeBoolean(enabled);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setAudioPortCallbacksEnabled, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setAudioVolumeGroupCallbacksEnabled(boolean enabled) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeBoolean(enabled);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setAudioVolumeGroupCallbacksEnabled, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public android.media.SoundTriggerSession acquireSoundTriggerSession() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.SoundTriggerSession _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_acquireSoundTriggerSession, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readTypedObject(android.media.SoundTriggerSession.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void releaseSoundTriggerSession(int session) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(session);
          boolean _status = mRemote.transact(Stub.TRANSACTION_releaseSoundTriggerSession, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public int getPhoneState() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getPhoneState, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void registerPolicyMixes(android.media.AudioMix[] mixes, boolean registration) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedArray(mixes, 0);
          _data.writeBoolean(registration);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerPolicyMixes, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setUidDeviceAffinities(int uid, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(uid);
          _data.writeTypedArray(devices, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setUidDeviceAffinities, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void removeUidDeviceAffinities(int uid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(uid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_removeUidDeviceAffinities, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setUserIdDeviceAffinities(int userId, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(userId);
          _data.writeTypedArray(devices, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setUserIdDeviceAffinities, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void removeUserIdDeviceAffinities(int userId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(userId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_removeUserIdDeviceAffinities, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public int startAudioSource(android.media.AudioPortConfigFw source, android.media.AudioAttributesInternal attributes) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(source, 0);
          _data.writeTypedObject(attributes, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startAudioSource, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void stopAudioSource(int portId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(portId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stopAudioSource, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setMasterMono(boolean mono) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeBoolean(mono);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setMasterMono, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public boolean getMasterMono() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getMasterMono, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readBoolean();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public float getStreamVolumeDB(int stream, int index, android.media.audio.common.AudioDeviceDescription device) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        float _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(stream);
          _data.writeInt(index);
          _data.writeTypedObject(device, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getStreamVolumeDB, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readFloat();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Populates supported surround formats and their enabled state in formats and formatsEnabled.
       * 
       * On input, count represents the maximum length of the returned array.
       * On output, count is the total number of elements, which may be larger than the array size.
       * Passing '0' on input and inspecting the value on output is a common way of determining the
       * number of elements without actually retrieving them.
       */
      @Override public void getSurroundFormats(android.media.audio.common.Int count, android.media.audio.common.AudioFormatDescription[] formats, boolean[] formatsEnabled) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(count, 0);
          _data.writeInt(formats.length);
          _data.writeInt(formatsEnabled.length);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSurroundFormats, _data, _reply, 0);
          _reply.readException();
          if ((0!=_reply.readInt())) {
            count.readFromParcel(_reply);
          }
          _reply.readTypedArray(formats, android.media.audio.common.AudioFormatDescription.CREATOR);
          _reply.readBooleanArray(formatsEnabled);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Populates the surround formats reported by the HDMI devices in formats.
       * 
       * On input, count represents the maximum length of the returned array.
       * On output, count is the total number of elements, which may be larger than the array size.
       * Passing '0' on input and inspecting the value on output is a common way of determining the
       * number of elements without actually retrieving them.
       */
      @Override public void getReportedSurroundFormats(android.media.audio.common.Int count, android.media.audio.common.AudioFormatDescription[] formats) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(count, 0);
          _data.writeInt(formats.length);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getReportedSurroundFormats, _data, _reply, 0);
          _reply.readException();
          if ((0!=_reply.readInt())) {
            count.readFromParcel(_reply);
          }
          _reply.readTypedArray(formats, android.media.audio.common.AudioFormatDescription.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public android.media.audio.common.AudioFormatDescription[] getHwOffloadFormatsSupportedForBluetoothMedia(android.media.audio.common.AudioDeviceDescription device) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.audio.common.AudioFormatDescription[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(device, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getHwOffloadFormatsSupportedForBluetoothMedia, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.media.audio.common.AudioFormatDescription.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void setSurroundFormatEnabled(android.media.audio.common.AudioFormatDescription audioFormat, boolean enabled) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(audioFormat, 0);
          _data.writeBoolean(enabled);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setSurroundFormatEnabled, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setAssistantServicesUids(int[] uids) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeIntArray(uids);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setAssistantServicesUids, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setActiveAssistantServicesUids(int[] activeUids) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeIntArray(activeUids);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setActiveAssistantServicesUids, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setA11yServicesUids(int[] uids) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeIntArray(uids);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setA11yServicesUids, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setCurrentImeUid(int uid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(uid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCurrentImeUid, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public boolean isHapticPlaybackSupported() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isHapticPlaybackSupported, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readBoolean();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean isUltrasoundSupported() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isUltrasoundSupported, _data, _reply, 0);
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
       * Queries if there is hardware support for requesting audio capture content from
       * the DSP hotword pipeline.
       * 
       * @param lookbackAudio true if additionally querying for the ability to capture audio
       *                      from the pipeline prior to capture stream open.
       */
      @Override public boolean isHotwordStreamSupported(boolean lookbackAudio) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeBoolean(lookbackAudio);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isHotwordStreamSupported, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readBoolean();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.media.AudioProductStrategy[] listAudioProductStrategies() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.AudioProductStrategy[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_listAudioProductStrategies, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.media.AudioProductStrategy.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int getProductStrategyFromAudioAttributes(android.media.AudioAttributesInternal aa, boolean fallbackOnDefault) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(aa, 0);
          _data.writeBoolean(fallbackOnDefault);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getProductStrategyFromAudioAttributes, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.media.AudioVolumeGroup[] listAudioVolumeGroups() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.AudioVolumeGroup[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_listAudioVolumeGroups, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.media.AudioVolumeGroup.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int getVolumeGroupFromAudioAttributes(android.media.AudioAttributesInternal aa, boolean fallbackOnDefault) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(aa, 0);
          _data.writeBoolean(fallbackOnDefault);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getVolumeGroupFromAudioAttributes, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void setRttEnabled(boolean enabled) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeBoolean(enabled);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setRttEnabled, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public boolean isCallScreenModeSupported() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isCallScreenModeSupported, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readBoolean();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void setDevicesRoleForStrategy(int strategy, int role, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(strategy);
          _data.writeInt(role);
          _data.writeTypedArray(devices, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setDevicesRoleForStrategy, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void removeDevicesRoleForStrategy(int strategy, int role, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(strategy);
          _data.writeInt(role);
          _data.writeTypedArray(devices, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_removeDevicesRoleForStrategy, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void clearDevicesRoleForStrategy(int strategy, int role) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(strategy);
          _data.writeInt(role);
          boolean _status = mRemote.transact(Stub.TRANSACTION_clearDevicesRoleForStrategy, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public android.media.audio.common.AudioDevice[] getDevicesForRoleAndStrategy(int strategy, int role) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.audio.common.AudioDevice[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(strategy);
          _data.writeInt(role);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getDevicesForRoleAndStrategy, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.media.audio.common.AudioDevice.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void setDevicesRoleForCapturePreset(int audioSource, int role, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(audioSource);
          _data.writeInt(role);
          _data.writeTypedArray(devices, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setDevicesRoleForCapturePreset, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void addDevicesRoleForCapturePreset(int audioSource, int role, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(audioSource);
          _data.writeInt(role);
          _data.writeTypedArray(devices, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_addDevicesRoleForCapturePreset, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void removeDevicesRoleForCapturePreset(int audioSource, int role, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(audioSource);
          _data.writeInt(role);
          _data.writeTypedArray(devices, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_removeDevicesRoleForCapturePreset, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void clearDevicesRoleForCapturePreset(int audioSource, int role) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(audioSource);
          _data.writeInt(role);
          boolean _status = mRemote.transact(Stub.TRANSACTION_clearDevicesRoleForCapturePreset, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public android.media.audio.common.AudioDevice[] getDevicesForRoleAndCapturePreset(int audioSource, int role) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.audio.common.AudioDevice[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(audioSource);
          _data.writeInt(role);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getDevicesForRoleAndCapturePreset, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.media.audio.common.AudioDevice.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean registerSoundTriggerCaptureStateListener(android.media.ICaptureStateListener listener) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(listener);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerSoundTriggerCaptureStateListener, _data, _reply, 0);
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
       * If a spatializer stage effect is present on the platform, this will return an
       * ISpatializer interface (see GetSpatializerResponse,aidl) to control this
       * feature.
       * If no spatializer stage is present, a null interface is returned.
       * The INativeSpatializerCallback passed must not be null.
       * Only one ISpatializer interface can exist at a given time. The native audio policy
       * service will reject the request if an interface was already acquired and previous owner
       * did not die or call ISpatializer.release().
       */
      @Override public android.media.GetSpatializerResponse getSpatializer(android.media.INativeSpatializerCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.GetSpatializerResponse _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSpatializer, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readTypedObject(android.media.GetSpatializerResponse.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Queries if some kind of spatialization will be performed if the audio playback context
       * described by the provided arguments is present.
       * The context is made of:
       * - The audio attributes describing the playback use case.
       * - The audio configuration describing the audio format, channels, sampling rate...
       * - The devices describing the sink audio device selected for playback.
       * All arguments are optional and only the specified arguments are used to match against
       * supported criteria. For instance, supplying no argument will tell if spatialization is
       * supported or not in general.
       */
      @Override public boolean canBeSpatialized(android.media.AudioAttributesInternal attr, android.media.audio.common.AudioConfig config, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(attr, 0);
          _data.writeTypedObject(config, 0);
          _data.writeTypedArray(devices, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_canBeSpatialized, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readBoolean();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Query how the direct playback is currently supported on the device. */
      @Override public int getDirectPlaybackSupport(android.media.AudioAttributesInternal attr, android.media.audio.common.AudioConfig config) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(attr, 0);
          _data.writeTypedObject(config, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getDirectPlaybackSupport, _data, _reply, 0);
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
       * Query audio profiles available for direct playback on the current output device(s)
       * for the specified audio attributes.
       */
      @Override public android.media.audio.common.AudioProfile[] getDirectProfilesForAttributes(android.media.AudioAttributesInternal attr) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.audio.common.AudioProfile[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(attr, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getDirectProfilesForAttributes, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.media.audio.common.AudioProfile.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Return a list of AudioMixerAttributes that can be used to set preferred mixer attributes
       * for the given device.
       */
      @Override public android.media.AudioMixerAttributesInternal[] getSupportedMixerAttributes(int portId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.AudioMixerAttributesInternal[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(portId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSupportedMixerAttributes, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.media.AudioMixerAttributesInternal.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Set preferred mixer attributes for a given device on a given audio attributes.
       * When conflicting requests are received, the last request will be honored.
       * The preferred mixer attributes can only be set when 1) the usage is media, 2) the
       * given device is currently available, 3) the given device is usb device, 4) the given mixer
       * attributes is supported by the given device.
       * 
       * @param attr the audio attributes whose mixer attributes should be set.
       * @param portId the port id of the device to be routed.
       * @param uid the uid of the request client. The uid will be used to recognize the ownership for
       *            the preferred mixer attributes. All the playback with same audio attributes from
       *            the same uid will be attached to the mixer with the preferred attributes if the
       *            playback is routed to the given device.
       * @param mixerAttr the preferred mixer attributes.
       */
      @Override public void setPreferredMixerAttributes(android.media.AudioAttributesInternal attr, int portId, int uid, android.media.AudioMixerAttributesInternal mixerAttr) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(attr, 0);
          _data.writeInt(portId);
          _data.writeInt(uid);
          _data.writeTypedObject(mixerAttr, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setPreferredMixerAttributes, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Get preferred mixer attributes for a given device on a given audio attributes.
       * Null will be returned if there is no preferred mixer attributes set or it has
       * been cleared.
       * 
       * @param attr the audio attributes whose mixer attributes should be set.
       * @param portId the port id of the device to be routed.
       */
      @Override public android.media.AudioMixerAttributesInternal getPreferredMixerAttributes(android.media.AudioAttributesInternal attr, int portId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.AudioMixerAttributesInternal _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(attr, 0);
          _data.writeInt(portId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getPreferredMixerAttributes, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readTypedObject(android.media.AudioMixerAttributesInternal.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Clear preferred mixer attributes for a given device on a given audio attributes that
       * is previously set via setPreferredMixerAttributes.
       * 
       * @param attr the audio attributes whose mixer attributes should be set.
       * @param portId the port id of the device to be routed.
       * @param uid the uid of the request client. The uid is used to identify the ownership for the
       *            preferred mixer attributes. The preferred mixer attributes will only be cleared
       *            if the uid is the same as the owner of current preferred mixer attributes.
       */
      @Override public void clearPreferredMixerAttributes(android.media.AudioAttributesInternal attr, int portId, int uid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(attr, 0);
          _data.writeInt(portId);
          _data.writeInt(uid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_clearPreferredMixerAttributes, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onNewAudioModulesAvailable = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_setDeviceConnectionState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_getDeviceConnectionState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_handleDeviceConfigChange = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_setPhoneState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_setForceUse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getForceUse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getOutput = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_getOutputForAttr = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_startOutput = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_stopOutput = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_releaseOutput = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_getInputForAttr = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_startInput = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_stopInput = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_releaseInput = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_initStreamVolume = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_setStreamVolumeIndex = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_getStreamVolumeIndex = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_setVolumeIndexForAttributes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
    static final int TRANSACTION_getVolumeIndexForAttributes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
    static final int TRANSACTION_getMaxVolumeIndexForAttributes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
    static final int TRANSACTION_getMinVolumeIndexForAttributes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
    static final int TRANSACTION_getStrategyForStream = (android.os.IBinder.FIRST_CALL_TRANSACTION + 23);
    static final int TRANSACTION_getDevicesForAttributes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 24);
    static final int TRANSACTION_getOutputForEffect = (android.os.IBinder.FIRST_CALL_TRANSACTION + 25);
    static final int TRANSACTION_registerEffect = (android.os.IBinder.FIRST_CALL_TRANSACTION + 26);
    static final int TRANSACTION_unregisterEffect = (android.os.IBinder.FIRST_CALL_TRANSACTION + 27);
    static final int TRANSACTION_setEffectEnabled = (android.os.IBinder.FIRST_CALL_TRANSACTION + 28);
    static final int TRANSACTION_moveEffectsToIo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 29);
    static final int TRANSACTION_isStreamActive = (android.os.IBinder.FIRST_CALL_TRANSACTION + 30);
    static final int TRANSACTION_isStreamActiveRemotely = (android.os.IBinder.FIRST_CALL_TRANSACTION + 31);
    static final int TRANSACTION_isSourceActive = (android.os.IBinder.FIRST_CALL_TRANSACTION + 32);
    static final int TRANSACTION_queryDefaultPreProcessing = (android.os.IBinder.FIRST_CALL_TRANSACTION + 33);
    static final int TRANSACTION_addSourceDefaultEffect = (android.os.IBinder.FIRST_CALL_TRANSACTION + 34);
    static final int TRANSACTION_addStreamDefaultEffect = (android.os.IBinder.FIRST_CALL_TRANSACTION + 35);
    static final int TRANSACTION_removeSourceDefaultEffect = (android.os.IBinder.FIRST_CALL_TRANSACTION + 36);
    static final int TRANSACTION_removeStreamDefaultEffect = (android.os.IBinder.FIRST_CALL_TRANSACTION + 37);
    static final int TRANSACTION_setSupportedSystemUsages = (android.os.IBinder.FIRST_CALL_TRANSACTION + 38);
    static final int TRANSACTION_setAllowedCapturePolicy = (android.os.IBinder.FIRST_CALL_TRANSACTION + 39);
    static final int TRANSACTION_getOffloadSupport = (android.os.IBinder.FIRST_CALL_TRANSACTION + 40);
    static final int TRANSACTION_isDirectOutputSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 41);
    static final int TRANSACTION_listAudioPorts = (android.os.IBinder.FIRST_CALL_TRANSACTION + 42);
    static final int TRANSACTION_listDeclaredDevicePorts = (android.os.IBinder.FIRST_CALL_TRANSACTION + 43);
    static final int TRANSACTION_getAudioPort = (android.os.IBinder.FIRST_CALL_TRANSACTION + 44);
    static final int TRANSACTION_createAudioPatch = (android.os.IBinder.FIRST_CALL_TRANSACTION + 45);
    static final int TRANSACTION_releaseAudioPatch = (android.os.IBinder.FIRST_CALL_TRANSACTION + 46);
    static final int TRANSACTION_listAudioPatches = (android.os.IBinder.FIRST_CALL_TRANSACTION + 47);
    static final int TRANSACTION_setAudioPortConfig = (android.os.IBinder.FIRST_CALL_TRANSACTION + 48);
    static final int TRANSACTION_registerClient = (android.os.IBinder.FIRST_CALL_TRANSACTION + 49);
    static final int TRANSACTION_setAudioPortCallbacksEnabled = (android.os.IBinder.FIRST_CALL_TRANSACTION + 50);
    static final int TRANSACTION_setAudioVolumeGroupCallbacksEnabled = (android.os.IBinder.FIRST_CALL_TRANSACTION + 51);
    static final int TRANSACTION_acquireSoundTriggerSession = (android.os.IBinder.FIRST_CALL_TRANSACTION + 52);
    static final int TRANSACTION_releaseSoundTriggerSession = (android.os.IBinder.FIRST_CALL_TRANSACTION + 53);
    static final int TRANSACTION_getPhoneState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 54);
    static final int TRANSACTION_registerPolicyMixes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 55);
    static final int TRANSACTION_setUidDeviceAffinities = (android.os.IBinder.FIRST_CALL_TRANSACTION + 56);
    static final int TRANSACTION_removeUidDeviceAffinities = (android.os.IBinder.FIRST_CALL_TRANSACTION + 57);
    static final int TRANSACTION_setUserIdDeviceAffinities = (android.os.IBinder.FIRST_CALL_TRANSACTION + 58);
    static final int TRANSACTION_removeUserIdDeviceAffinities = (android.os.IBinder.FIRST_CALL_TRANSACTION + 59);
    static final int TRANSACTION_startAudioSource = (android.os.IBinder.FIRST_CALL_TRANSACTION + 60);
    static final int TRANSACTION_stopAudioSource = (android.os.IBinder.FIRST_CALL_TRANSACTION + 61);
    static final int TRANSACTION_setMasterMono = (android.os.IBinder.FIRST_CALL_TRANSACTION + 62);
    static final int TRANSACTION_getMasterMono = (android.os.IBinder.FIRST_CALL_TRANSACTION + 63);
    static final int TRANSACTION_getStreamVolumeDB = (android.os.IBinder.FIRST_CALL_TRANSACTION + 64);
    static final int TRANSACTION_getSurroundFormats = (android.os.IBinder.FIRST_CALL_TRANSACTION + 65);
    static final int TRANSACTION_getReportedSurroundFormats = (android.os.IBinder.FIRST_CALL_TRANSACTION + 66);
    static final int TRANSACTION_getHwOffloadFormatsSupportedForBluetoothMedia = (android.os.IBinder.FIRST_CALL_TRANSACTION + 67);
    static final int TRANSACTION_setSurroundFormatEnabled = (android.os.IBinder.FIRST_CALL_TRANSACTION + 68);
    static final int TRANSACTION_setAssistantServicesUids = (android.os.IBinder.FIRST_CALL_TRANSACTION + 69);
    static final int TRANSACTION_setActiveAssistantServicesUids = (android.os.IBinder.FIRST_CALL_TRANSACTION + 70);
    static final int TRANSACTION_setA11yServicesUids = (android.os.IBinder.FIRST_CALL_TRANSACTION + 71);
    static final int TRANSACTION_setCurrentImeUid = (android.os.IBinder.FIRST_CALL_TRANSACTION + 72);
    static final int TRANSACTION_isHapticPlaybackSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 73);
    static final int TRANSACTION_isUltrasoundSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 74);
    static final int TRANSACTION_isHotwordStreamSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 75);
    static final int TRANSACTION_listAudioProductStrategies = (android.os.IBinder.FIRST_CALL_TRANSACTION + 76);
    static final int TRANSACTION_getProductStrategyFromAudioAttributes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 77);
    static final int TRANSACTION_listAudioVolumeGroups = (android.os.IBinder.FIRST_CALL_TRANSACTION + 78);
    static final int TRANSACTION_getVolumeGroupFromAudioAttributes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 79);
    static final int TRANSACTION_setRttEnabled = (android.os.IBinder.FIRST_CALL_TRANSACTION + 80);
    static final int TRANSACTION_isCallScreenModeSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 81);
    static final int TRANSACTION_setDevicesRoleForStrategy = (android.os.IBinder.FIRST_CALL_TRANSACTION + 82);
    static final int TRANSACTION_removeDevicesRoleForStrategy = (android.os.IBinder.FIRST_CALL_TRANSACTION + 83);
    static final int TRANSACTION_clearDevicesRoleForStrategy = (android.os.IBinder.FIRST_CALL_TRANSACTION + 84);
    static final int TRANSACTION_getDevicesForRoleAndStrategy = (android.os.IBinder.FIRST_CALL_TRANSACTION + 85);
    static final int TRANSACTION_setDevicesRoleForCapturePreset = (android.os.IBinder.FIRST_CALL_TRANSACTION + 86);
    static final int TRANSACTION_addDevicesRoleForCapturePreset = (android.os.IBinder.FIRST_CALL_TRANSACTION + 87);
    static final int TRANSACTION_removeDevicesRoleForCapturePreset = (android.os.IBinder.FIRST_CALL_TRANSACTION + 88);
    static final int TRANSACTION_clearDevicesRoleForCapturePreset = (android.os.IBinder.FIRST_CALL_TRANSACTION + 89);
    static final int TRANSACTION_getDevicesForRoleAndCapturePreset = (android.os.IBinder.FIRST_CALL_TRANSACTION + 90);
    static final int TRANSACTION_registerSoundTriggerCaptureStateListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 91);
    static final int TRANSACTION_getSpatializer = (android.os.IBinder.FIRST_CALL_TRANSACTION + 92);
    static final int TRANSACTION_canBeSpatialized = (android.os.IBinder.FIRST_CALL_TRANSACTION + 93);
    static final int TRANSACTION_getDirectPlaybackSupport = (android.os.IBinder.FIRST_CALL_TRANSACTION + 94);
    static final int TRANSACTION_getDirectProfilesForAttributes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 95);
    static final int TRANSACTION_getSupportedMixerAttributes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 96);
    static final int TRANSACTION_setPreferredMixerAttributes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 97);
    static final int TRANSACTION_getPreferredMixerAttributes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 98);
    static final int TRANSACTION_clearPreferredMixerAttributes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 99);
  }
  public static final java.lang.String DESCRIPTOR = "android$media$IAudioPolicyService".replace('$', '.');
  public void onNewAudioModulesAvailable() throws android.os.RemoteException;
  public void setDeviceConnectionState(int state, android.media.audio.common.AudioPort port, android.media.audio.common.AudioFormatDescription encodedFormat) throws android.os.RemoteException;
  public int getDeviceConnectionState(android.media.audio.common.AudioDevice device) throws android.os.RemoteException;
  public void handleDeviceConfigChange(android.media.audio.common.AudioDevice device, java.lang.String deviceName, android.media.audio.common.AudioFormatDescription encodedFormat) throws android.os.RemoteException;
  public void setPhoneState(int state, int uid) throws android.os.RemoteException;
  public void setForceUse(int usage, int config) throws android.os.RemoteException;
  public int getForceUse(int usage) throws android.os.RemoteException;
  public int getOutput(int stream) throws android.os.RemoteException;
  public android.media.GetOutputForAttrResponse getOutputForAttr(android.media.AudioAttributesInternal attr, int session, android.content.AttributionSourceState attributionSource, android.media.audio.common.AudioConfig config, int flags, int selectedDeviceId) throws android.os.RemoteException;
  public void startOutput(int portId) throws android.os.RemoteException;
  public void stopOutput(int portId) throws android.os.RemoteException;
  public void releaseOutput(int portId) throws android.os.RemoteException;
  public android.media.GetInputForAttrResponse getInputForAttr(android.media.AudioAttributesInternal attr, int input, int riid, int session, android.content.AttributionSourceState attributionSource, android.media.audio.common.AudioConfigBase config, int flags, int selectedDeviceId) throws android.os.RemoteException;
  public void startInput(int portId) throws android.os.RemoteException;
  public void stopInput(int portId) throws android.os.RemoteException;
  public void releaseInput(int portId) throws android.os.RemoteException;
  public void initStreamVolume(int stream, int indexMin, int indexMax) throws android.os.RemoteException;
  public void setStreamVolumeIndex(int stream, android.media.audio.common.AudioDeviceDescription device, int index) throws android.os.RemoteException;
  public int getStreamVolumeIndex(int stream, android.media.audio.common.AudioDeviceDescription device) throws android.os.RemoteException;
  public void setVolumeIndexForAttributes(android.media.AudioAttributesInternal attr, android.media.audio.common.AudioDeviceDescription device, int index) throws android.os.RemoteException;
  public int getVolumeIndexForAttributes(android.media.AudioAttributesInternal attr, android.media.audio.common.AudioDeviceDescription device) throws android.os.RemoteException;
  public int getMaxVolumeIndexForAttributes(android.media.AudioAttributesInternal attr) throws android.os.RemoteException;
  public int getMinVolumeIndexForAttributes(android.media.AudioAttributesInternal attr) throws android.os.RemoteException;
  public int getStrategyForStream(int stream) throws android.os.RemoteException;
  public android.media.audio.common.AudioDevice[] getDevicesForAttributes(android.media.AudioAttributesInternal attr, boolean forVolume) throws android.os.RemoteException;
  public int getOutputForEffect(android.media.EffectDescriptor desc) throws android.os.RemoteException;
  public void registerEffect(android.media.EffectDescriptor desc, int io, int strategy, int session, int id) throws android.os.RemoteException;
  public void unregisterEffect(int id) throws android.os.RemoteException;
  public void setEffectEnabled(int id, boolean enabled) throws android.os.RemoteException;
  public void moveEffectsToIo(int[] ids, int io) throws android.os.RemoteException;
  public boolean isStreamActive(int stream, int inPastMs) throws android.os.RemoteException;
  public boolean isStreamActiveRemotely(int stream, int inPastMs) throws android.os.RemoteException;
  public boolean isSourceActive(int source) throws android.os.RemoteException;
  /**
   * On input, count represents the maximum length of the returned array.
   * On output, count is the total number of elements, which may be larger than the array size.
   * Passing '0' on input and inspecting the value on output is a common way of determining the
   * number of elements without actually retrieving them.
   */
  public android.media.EffectDescriptor[] queryDefaultPreProcessing(int audioSession, android.media.audio.common.Int count) throws android.os.RemoteException;
  public int addSourceDefaultEffect(android.media.audio.common.AudioUuid type, java.lang.String opPackageName, android.media.audio.common.AudioUuid uuid, int priority, int source) throws android.os.RemoteException;
  public int addStreamDefaultEffect(android.media.audio.common.AudioUuid type, java.lang.String opPackageName, android.media.audio.common.AudioUuid uuid, int priority, int usage) throws android.os.RemoteException;
  public void removeSourceDefaultEffect(int id) throws android.os.RemoteException;
  public void removeStreamDefaultEffect(int id) throws android.os.RemoteException;
  public void setSupportedSystemUsages(int[] systemUsages) throws android.os.RemoteException;
  public void setAllowedCapturePolicy(int uid, int capturePolicy) throws android.os.RemoteException;
  /**
   * Check if offload is possible for given format, stream type, sample rate,
   * bit rate, duration, video and streaming or offload property is enabled.
   */
  public int getOffloadSupport(android.media.audio.common.AudioOffloadInfo info) throws android.os.RemoteException;
  /** Check if direct playback is possible for given format, sample rate, channel mask and flags. */
  public boolean isDirectOutputSupported(android.media.audio.common.AudioConfigBase config, android.media.AudioAttributesInternal attributes) throws android.os.RemoteException;
  /**
   * List currently attached audio ports and their attributes. Returns the generation.
   * The generation is incremented each time when anything changes in the ports
   * configuration.
   * 
   * On input, count represents the maximum length of the returned array.
   * On output, count is the total number of elements, which may be larger than the array size.
   * Passing '0' on input and inspecting the value on output is a common way of determining the
   * number of elements without actually retrieving them.
   */
  public int listAudioPorts(int role, int type, android.media.audio.common.Int count, android.media.AudioPortFw[] ports) throws android.os.RemoteException;
  /**
   * List all device ports declared in the configuration (including currently detached ones)
   * 'role' can be 'NONE' to get both input and output devices,
   * 'SINK' for output devices, and 'SOURCE' for input devices.
   */
  public android.media.AudioPortFw[] listDeclaredDevicePorts(int role) throws android.os.RemoteException;
  /** Get attributes for the audio port with the given id (AudioPort.hal.id field). */
  public android.media.AudioPortFw getAudioPort(int portId) throws android.os.RemoteException;
  /**
   * Create an audio patch between several source and sink ports.
   * The handle argument is used when updating an existing patch.
   */
  public int createAudioPatch(android.media.AudioPatchFw patch, int handle) throws android.os.RemoteException;
  /** Release an audio patch. */
  public void releaseAudioPatch(int handle) throws android.os.RemoteException;
  /**
   * List existing audio patches. Returns the generation.
   * 
   * On input, count represents the maximum length of the returned array.
   * On output, count is the total number of elements, which may be larger than the array size.
   * Passing '0' on input and inspecting the value on output is a common way of determining the
   * number of elements without actually retrieving them.
   */
  public int listAudioPatches(android.media.audio.common.Int count, android.media.AudioPatchFw[] patches) throws android.os.RemoteException;
  /** Set audio port configuration. */
  public void setAudioPortConfig(android.media.AudioPortConfigFw config) throws android.os.RemoteException;
  public void registerClient(android.media.IAudioPolicyServiceClient client) throws android.os.RemoteException;
  public void setAudioPortCallbacksEnabled(boolean enabled) throws android.os.RemoteException;
  public void setAudioVolumeGroupCallbacksEnabled(boolean enabled) throws android.os.RemoteException;
  public android.media.SoundTriggerSession acquireSoundTriggerSession() throws android.os.RemoteException;
  public void releaseSoundTriggerSession(int session) throws android.os.RemoteException;
  public int getPhoneState() throws android.os.RemoteException;
  public void registerPolicyMixes(android.media.AudioMix[] mixes, boolean registration) throws android.os.RemoteException;
  public void setUidDeviceAffinities(int uid, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException;
  public void removeUidDeviceAffinities(int uid) throws android.os.RemoteException;
  public void setUserIdDeviceAffinities(int userId, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException;
  public void removeUserIdDeviceAffinities(int userId) throws android.os.RemoteException;
  public int startAudioSource(android.media.AudioPortConfigFw source, android.media.AudioAttributesInternal attributes) throws android.os.RemoteException;
  public void stopAudioSource(int portId) throws android.os.RemoteException;
  public void setMasterMono(boolean mono) throws android.os.RemoteException;
  public boolean getMasterMono() throws android.os.RemoteException;
  public float getStreamVolumeDB(int stream, int index, android.media.audio.common.AudioDeviceDescription device) throws android.os.RemoteException;
  /**
   * Populates supported surround formats and their enabled state in formats and formatsEnabled.
   * 
   * On input, count represents the maximum length of the returned array.
   * On output, count is the total number of elements, which may be larger than the array size.
   * Passing '0' on input and inspecting the value on output is a common way of determining the
   * number of elements without actually retrieving them.
   */
  public void getSurroundFormats(android.media.audio.common.Int count, android.media.audio.common.AudioFormatDescription[] formats, boolean[] formatsEnabled) throws android.os.RemoteException;
  /**
   * Populates the surround formats reported by the HDMI devices in formats.
   * 
   * On input, count represents the maximum length of the returned array.
   * On output, count is the total number of elements, which may be larger than the array size.
   * Passing '0' on input and inspecting the value on output is a common way of determining the
   * number of elements without actually retrieving them.
   */
  public void getReportedSurroundFormats(android.media.audio.common.Int count, android.media.audio.common.AudioFormatDescription[] formats) throws android.os.RemoteException;
  public android.media.audio.common.AudioFormatDescription[] getHwOffloadFormatsSupportedForBluetoothMedia(android.media.audio.common.AudioDeviceDescription device) throws android.os.RemoteException;
  public void setSurroundFormatEnabled(android.media.audio.common.AudioFormatDescription audioFormat, boolean enabled) throws android.os.RemoteException;
  public void setAssistantServicesUids(int[] uids) throws android.os.RemoteException;
  public void setActiveAssistantServicesUids(int[] activeUids) throws android.os.RemoteException;
  public void setA11yServicesUids(int[] uids) throws android.os.RemoteException;
  public void setCurrentImeUid(int uid) throws android.os.RemoteException;
  public boolean isHapticPlaybackSupported() throws android.os.RemoteException;
  public boolean isUltrasoundSupported() throws android.os.RemoteException;
  /**
   * Queries if there is hardware support for requesting audio capture content from
   * the DSP hotword pipeline.
   * 
   * @param lookbackAudio true if additionally querying for the ability to capture audio
   *                      from the pipeline prior to capture stream open.
   */
  public boolean isHotwordStreamSupported(boolean lookbackAudio) throws android.os.RemoteException;
  public android.media.AudioProductStrategy[] listAudioProductStrategies() throws android.os.RemoteException;
  public int getProductStrategyFromAudioAttributes(android.media.AudioAttributesInternal aa, boolean fallbackOnDefault) throws android.os.RemoteException;
  public android.media.AudioVolumeGroup[] listAudioVolumeGroups() throws android.os.RemoteException;
  public int getVolumeGroupFromAudioAttributes(android.media.AudioAttributesInternal aa, boolean fallbackOnDefault) throws android.os.RemoteException;
  public void setRttEnabled(boolean enabled) throws android.os.RemoteException;
  public boolean isCallScreenModeSupported() throws android.os.RemoteException;
  public void setDevicesRoleForStrategy(int strategy, int role, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException;
  public void removeDevicesRoleForStrategy(int strategy, int role, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException;
  public void clearDevicesRoleForStrategy(int strategy, int role) throws android.os.RemoteException;
  public android.media.audio.common.AudioDevice[] getDevicesForRoleAndStrategy(int strategy, int role) throws android.os.RemoteException;
  public void setDevicesRoleForCapturePreset(int audioSource, int role, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException;
  public void addDevicesRoleForCapturePreset(int audioSource, int role, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException;
  public void removeDevicesRoleForCapturePreset(int audioSource, int role, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException;
  public void clearDevicesRoleForCapturePreset(int audioSource, int role) throws android.os.RemoteException;
  public android.media.audio.common.AudioDevice[] getDevicesForRoleAndCapturePreset(int audioSource, int role) throws android.os.RemoteException;
  public boolean registerSoundTriggerCaptureStateListener(android.media.ICaptureStateListener listener) throws android.os.RemoteException;
  /**
   * If a spatializer stage effect is present on the platform, this will return an
   * ISpatializer interface (see GetSpatializerResponse,aidl) to control this
   * feature.
   * If no spatializer stage is present, a null interface is returned.
   * The INativeSpatializerCallback passed must not be null.
   * Only one ISpatializer interface can exist at a given time. The native audio policy
   * service will reject the request if an interface was already acquired and previous owner
   * did not die or call ISpatializer.release().
   */
  public android.media.GetSpatializerResponse getSpatializer(android.media.INativeSpatializerCallback callback) throws android.os.RemoteException;
  /**
   * Queries if some kind of spatialization will be performed if the audio playback context
   * described by the provided arguments is present.
   * The context is made of:
   * - The audio attributes describing the playback use case.
   * - The audio configuration describing the audio format, channels, sampling rate...
   * - The devices describing the sink audio device selected for playback.
   * All arguments are optional and only the specified arguments are used to match against
   * supported criteria. For instance, supplying no argument will tell if spatialization is
   * supported or not in general.
   */
  public boolean canBeSpatialized(android.media.AudioAttributesInternal attr, android.media.audio.common.AudioConfig config, android.media.audio.common.AudioDevice[] devices) throws android.os.RemoteException;
  /** Query how the direct playback is currently supported on the device. */
  public int getDirectPlaybackSupport(android.media.AudioAttributesInternal attr, android.media.audio.common.AudioConfig config) throws android.os.RemoteException;
  /**
   * Query audio profiles available for direct playback on the current output device(s)
   * for the specified audio attributes.
   */
  public android.media.audio.common.AudioProfile[] getDirectProfilesForAttributes(android.media.AudioAttributesInternal attr) throws android.os.RemoteException;
  /**
   * Return a list of AudioMixerAttributes that can be used to set preferred mixer attributes
   * for the given device.
   */
  public android.media.AudioMixerAttributesInternal[] getSupportedMixerAttributes(int portId) throws android.os.RemoteException;
  /**
   * Set preferred mixer attributes for a given device on a given audio attributes.
   * When conflicting requests are received, the last request will be honored.
   * The preferred mixer attributes can only be set when 1) the usage is media, 2) the
   * given device is currently available, 3) the given device is usb device, 4) the given mixer
   * attributes is supported by the given device.
   * 
   * @param attr the audio attributes whose mixer attributes should be set.
   * @param portId the port id of the device to be routed.
   * @param uid the uid of the request client. The uid will be used to recognize the ownership for
   *            the preferred mixer attributes. All the playback with same audio attributes from
   *            the same uid will be attached to the mixer with the preferred attributes if the
   *            playback is routed to the given device.
   * @param mixerAttr the preferred mixer attributes.
   */
  public void setPreferredMixerAttributes(android.media.AudioAttributesInternal attr, int portId, int uid, android.media.AudioMixerAttributesInternal mixerAttr) throws android.os.RemoteException;
  /**
   * Get preferred mixer attributes for a given device on a given audio attributes.
   * Null will be returned if there is no preferred mixer attributes set or it has
   * been cleared.
   * 
   * @param attr the audio attributes whose mixer attributes should be set.
   * @param portId the port id of the device to be routed.
   */
  public android.media.AudioMixerAttributesInternal getPreferredMixerAttributes(android.media.AudioAttributesInternal attr, int portId) throws android.os.RemoteException;
  /**
   * Clear preferred mixer attributes for a given device on a given audio attributes that
   * is previously set via setPreferredMixerAttributes.
   * 
   * @param attr the audio attributes whose mixer attributes should be set.
   * @param portId the port id of the device to be routed.
   * @param uid the uid of the request client. The uid is used to identify the ownership for the
   *            preferred mixer attributes. The preferred mixer attributes will only be cleared
   *            if the uid is the same as the owner of current preferred mixer attributes.
   */
  public void clearPreferredMixerAttributes(android.media.AudioAttributesInternal attr, int portId, int uid) throws android.os.RemoteException;
}
