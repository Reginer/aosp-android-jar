/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/frameworks/av/av-types-aidl-java-source/gen/android/media/IAudioManagerNative.java.d -o out/soong/.intermediates/frameworks/av/av-types-aidl-java-source/gen -Nframeworks/av/aidl frameworks/av/aidl/android/media/IAudioManagerNative.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media;
/**
 * Native accessible interface for AudioService.
 * Note this interface has a mix of oneway and non-oneway methods. This is intentional for certain
 * calls intended to come from audioserver.
 * {@hide}
 */
public interface IAudioManagerNative extends android.os.IInterface
{
  /** Default implementation for IAudioManagerNative. */
  public static class Default implements android.media.IAudioManagerNative
  {
    /**
     * audioserver is muting playback due to hardening.
     * Calls which aren't from uid 1041 are dropped.
     * @param uid - the uid whose playback is restricted
     * @param type - the level of playback restriction which was hit (full or partial)
     * @param bypassed - true if the client should be muted but was exempted (for example due to a
     * certain audio usage to prevent regressions)
     */
    @Override public void playbackHardeningEvent(int uid, byte type, boolean bypassed) throws android.os.RemoteException
    {
    }
    /** Block until AudioService synchronizes pending permission state with audioserver. */
    @Override public void permissionUpdateBarrier() throws android.os.RemoteException
    {
    }
    /**
     * Update mute state event for port
     * @param portId Port id to update
     * @param event the mute event containing info about the mute
     */
    @Override public void portMuteEvent(int portId, int event) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.media.IAudioManagerNative
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.media.IAudioManagerNative interface,
     * generating a proxy if needed.
     */
    public static android.media.IAudioManagerNative asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.media.IAudioManagerNative))) {
        return ((android.media.IAudioManagerNative)iin);
      }
      return new android.media.IAudioManagerNative.Stub.Proxy(obj);
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
        case TRANSACTION_playbackHardeningEvent:
        {
          int _arg0;
          _arg0 = data.readInt();
          byte _arg1;
          _arg1 = data.readByte();
          boolean _arg2;
          _arg2 = data.readBoolean();
          data.enforceNoDataAvail();
          this.playbackHardeningEvent(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_permissionUpdateBarrier:
        {
          this.permissionUpdateBarrier();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_portMuteEvent:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.portMuteEvent(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.media.IAudioManagerNative
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
       * audioserver is muting playback due to hardening.
       * Calls which aren't from uid 1041 are dropped.
       * @param uid - the uid whose playback is restricted
       * @param type - the level of playback restriction which was hit (full or partial)
       * @param bypassed - true if the client should be muted but was exempted (for example due to a
       * certain audio usage to prevent regressions)
       */
      @Override public void playbackHardeningEvent(int uid, byte type, boolean bypassed) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(uid);
          _data.writeByte(type);
          _data.writeBoolean(bypassed);
          boolean _status = mRemote.transact(Stub.TRANSACTION_playbackHardeningEvent, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Block until AudioService synchronizes pending permission state with audioserver. */
      @Override public void permissionUpdateBarrier() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_permissionUpdateBarrier, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Update mute state event for port
       * @param portId Port id to update
       * @param event the mute event containing info about the mute
       */
      @Override public void portMuteEvent(int portId, int event) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(portId);
          _data.writeInt(event);
          boolean _status = mRemote.transact(Stub.TRANSACTION_portMuteEvent, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_playbackHardeningEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_permissionUpdateBarrier = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_portMuteEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android.media.IAudioManagerNative";
  /**
   * audioserver is muting playback due to hardening.
   * Calls which aren't from uid 1041 are dropped.
   * @param uid - the uid whose playback is restricted
   * @param type - the level of playback restriction which was hit (full or partial)
   * @param bypassed - true if the client should be muted but was exempted (for example due to a
   * certain audio usage to prevent regressions)
   */
  public void playbackHardeningEvent(int uid, byte type, boolean bypassed) throws android.os.RemoteException;
  /** Block until AudioService synchronizes pending permission state with audioserver. */
  public void permissionUpdateBarrier() throws android.os.RemoteException;
  /**
   * Update mute state event for port
   * @param portId Port id to update
   * @param event the mute event containing info about the mute
   */
  public void portMuteEvent(int portId, int event) throws android.os.RemoteException;
  public static @interface HardeningType {
    // Restricted due to OP_CONTROL_AUDIO_PARTIAL
    // This OP is more permissive than OP_CONTROL_AUDIO, which allows apps in a foreground state
    // not associated with FGS to access audio
    public static final byte PARTIAL = 0;
    // Restricted due to OP_CONTROL_AUDIO
    public static final byte FULL = 1;
  }
}
