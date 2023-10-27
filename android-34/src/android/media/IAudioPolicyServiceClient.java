/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/** {@hide} */
public interface IAudioPolicyServiceClient extends android.os.IInterface
{
  /** Default implementation for IAudioPolicyServiceClient. */
  public static class Default implements android.media.IAudioPolicyServiceClient
  {
    /** Notifies a change of volume group. */
    @Override public void onAudioVolumeGroupChanged(int group, int flags) throws android.os.RemoteException
    {
    }
    /** Notifies a change of audio port configuration. */
    @Override public void onAudioPortListUpdate() throws android.os.RemoteException
    {
    }
    /** Notifies a change of audio patch configuration. */
    @Override public void onAudioPatchListUpdate() throws android.os.RemoteException
    {
    }
    /** Notifies a change in the mixing state of a specific mix in a dynamic audio policy. */
    @Override public void onDynamicPolicyMixStateUpdate(java.lang.String regId, int state) throws android.os.RemoteException
    {
    }
    /** Notifies a change of audio recording configuration. */
    @Override public void onRecordingConfigurationUpdate(int event, android.media.RecordClientInfo clientInfo, android.media.audio.common.AudioConfigBase clientConfig, android.media.EffectDescriptor[] clientEffects, android.media.audio.common.AudioConfigBase deviceConfig, android.media.EffectDescriptor[] effects, int patchHandle, int source) throws android.os.RemoteException
    {
    }
    /** Notifies a change of audio routing */
    @Override public void onRoutingUpdated() throws android.os.RemoteException
    {
    }
    /** Notifies a request for volume index ranges to be reset after they were observed as invalid */
    @Override public void onVolumeRangeInitRequest() throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.media.IAudioPolicyServiceClient
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.media.IAudioPolicyServiceClient interface,
     * generating a proxy if needed.
     */
    public static android.media.IAudioPolicyServiceClient asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.media.IAudioPolicyServiceClient))) {
        return ((android.media.IAudioPolicyServiceClient)iin);
      }
      return new android.media.IAudioPolicyServiceClient.Stub.Proxy(obj);
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
        case TRANSACTION_onAudioVolumeGroupChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.onAudioVolumeGroupChanged(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onAudioPortListUpdate:
        {
          this.onAudioPortListUpdate();
          break;
        }
        case TRANSACTION_onAudioPatchListUpdate:
        {
          this.onAudioPatchListUpdate();
          break;
        }
        case TRANSACTION_onDynamicPolicyMixStateUpdate:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.onDynamicPolicyMixStateUpdate(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onRecordingConfigurationUpdate:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.media.RecordClientInfo _arg1;
          _arg1 = data.readTypedObject(android.media.RecordClientInfo.CREATOR);
          android.media.audio.common.AudioConfigBase _arg2;
          _arg2 = data.readTypedObject(android.media.audio.common.AudioConfigBase.CREATOR);
          android.media.EffectDescriptor[] _arg3;
          _arg3 = data.createTypedArray(android.media.EffectDescriptor.CREATOR);
          android.media.audio.common.AudioConfigBase _arg4;
          _arg4 = data.readTypedObject(android.media.audio.common.AudioConfigBase.CREATOR);
          android.media.EffectDescriptor[] _arg5;
          _arg5 = data.createTypedArray(android.media.EffectDescriptor.CREATOR);
          int _arg6;
          _arg6 = data.readInt();
          int _arg7;
          _arg7 = data.readInt();
          data.enforceNoDataAvail();
          this.onRecordingConfigurationUpdate(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7);
          break;
        }
        case TRANSACTION_onRoutingUpdated:
        {
          this.onRoutingUpdated();
          break;
        }
        case TRANSACTION_onVolumeRangeInitRequest:
        {
          this.onVolumeRangeInitRequest();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.media.IAudioPolicyServiceClient
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
      /** Notifies a change of volume group. */
      @Override public void onAudioVolumeGroupChanged(int group, int flags) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(group);
          _data.writeInt(flags);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAudioVolumeGroupChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Notifies a change of audio port configuration. */
      @Override public void onAudioPortListUpdate() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAudioPortListUpdate, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Notifies a change of audio patch configuration. */
      @Override public void onAudioPatchListUpdate() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAudioPatchListUpdate, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Notifies a change in the mixing state of a specific mix in a dynamic audio policy. */
      @Override public void onDynamicPolicyMixStateUpdate(java.lang.String regId, int state) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(regId);
          _data.writeInt(state);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onDynamicPolicyMixStateUpdate, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Notifies a change of audio recording configuration. */
      @Override public void onRecordingConfigurationUpdate(int event, android.media.RecordClientInfo clientInfo, android.media.audio.common.AudioConfigBase clientConfig, android.media.EffectDescriptor[] clientEffects, android.media.audio.common.AudioConfigBase deviceConfig, android.media.EffectDescriptor[] effects, int patchHandle, int source) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(event);
          _data.writeTypedObject(clientInfo, 0);
          _data.writeTypedObject(clientConfig, 0);
          _data.writeTypedArray(clientEffects, 0);
          _data.writeTypedObject(deviceConfig, 0);
          _data.writeTypedArray(effects, 0);
          _data.writeInt(patchHandle);
          _data.writeInt(source);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onRecordingConfigurationUpdate, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Notifies a change of audio routing */
      @Override public void onRoutingUpdated() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onRoutingUpdated, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Notifies a request for volume index ranges to be reset after they were observed as invalid */
      @Override public void onVolumeRangeInitRequest() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onVolumeRangeInitRequest, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onAudioVolumeGroupChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onAudioPortListUpdate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_onAudioPatchListUpdate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_onDynamicPolicyMixStateUpdate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_onRecordingConfigurationUpdate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_onRoutingUpdated = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_onVolumeRangeInitRequest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
  }
  public static final java.lang.String DESCRIPTOR = "android$media$IAudioPolicyServiceClient".replace('$', '.');
  /** Notifies a change of volume group. */
  public void onAudioVolumeGroupChanged(int group, int flags) throws android.os.RemoteException;
  /** Notifies a change of audio port configuration. */
  public void onAudioPortListUpdate() throws android.os.RemoteException;
  /** Notifies a change of audio patch configuration. */
  public void onAudioPatchListUpdate() throws android.os.RemoteException;
  /** Notifies a change in the mixing state of a specific mix in a dynamic audio policy. */
  public void onDynamicPolicyMixStateUpdate(java.lang.String regId, int state) throws android.os.RemoteException;
  /** Notifies a change of audio recording configuration. */
  public void onRecordingConfigurationUpdate(int event, android.media.RecordClientInfo clientInfo, android.media.audio.common.AudioConfigBase clientConfig, android.media.EffectDescriptor[] clientEffects, android.media.audio.common.AudioConfigBase deviceConfig, android.media.EffectDescriptor[] effects, int patchHandle, int source) throws android.os.RemoteException;
  /** Notifies a change of audio routing */
  public void onRoutingUpdated() throws android.os.RemoteException;
  /** Notifies a request for volume index ranges to be reset after they were observed as invalid */
  public void onVolumeRangeInitRequest() throws android.os.RemoteException;
}
