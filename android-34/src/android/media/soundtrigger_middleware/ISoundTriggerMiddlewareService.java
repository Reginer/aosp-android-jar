/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.soundtrigger_middleware;
/**
 * Main entry point into this module.
 * 
 * Allows the client to enumerate the available soundtrigger devices and their capabilities, then
 * attach to either one of them in order to use it.
 * 
 * {@hide}
 */
public interface ISoundTriggerMiddlewareService extends android.os.IInterface
{
  /** Default implementation for ISoundTriggerMiddlewareService. */
  public static class Default implements android.media.soundtrigger_middleware.ISoundTriggerMiddlewareService
  {
    /**
     * Query the available modules and their capabilities.
     * 
     * This variant is intended for use by the originator of the operations for permission
     * enforcement purposes. The provided identity's uid/pid fields will be ignored and overridden
     * by the ones provided by Binder.getCallingUid() / Binder.getCallingPid().
     */
    @Override public android.media.soundtrigger_middleware.SoundTriggerModuleDescriptor[] listModulesAsOriginator(android.media.permission.Identity identity) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Query the available modules and their capabilities.
     * 
     * This variant is intended for use by a trusted "middleman", acting on behalf of some identity
     * other than itself. The caller must provide:
     * - Its own identity, which will be used to establish trust via the
     *   SOUNDTRIGGER_DELEGATE_IDENTITY permission. This identity's uid/pid fields will be ignored
     *   and overridden by the ones provided by Binder.getCallingUid() / Binder.getCallingPid().
     *   This implies that the caller must clear its caller identity to protect from the case where
     *   it resides in the same process as the callee.
     * - The identity of the entity on behalf of which module operations are to be performed.
     */
    @Override public android.media.soundtrigger_middleware.SoundTriggerModuleDescriptor[] listModulesAsMiddleman(android.media.permission.Identity middlemanIdentity, android.media.permission.Identity originatorIdentity) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Attach to one of the available modules.
     * 
     * This variant is intended for use by the originator of the operations for permission
     * enforcement purposes. The provided identity's uid/pid fields will be ignored and overridden
     * by the ones provided by Binder.getCallingUid() / Binder.getCallingPid().
     * 
     * listModules() must be called prior to calling this method and the provided handle must be
     * one of the handles from the returned list.
     */
    @Override public android.media.soundtrigger_middleware.ISoundTriggerModule attachAsOriginator(int handle, android.media.permission.Identity identity, android.media.soundtrigger_middleware.ISoundTriggerCallback callback) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Attach to one of the available modules.
     * 
     * This variant is intended for use by a trusted "middleman", acting on behalf of some identity
     * other than itself. The caller must provide:
     * - Its own identity, which will be used to establish trust via the
     *   SOUNDTRIGGER_DELEGATE_IDENTITY permission. This identity's uid/pid fields will be ignored
     *   and overridden by the ones provided by Binder.getCallingUid() / Binder.getCallingPid().
     *   This implies that the caller must clear its caller identity to protect from the case where
     *   it resides in the same process as the callee.
     * - The identity of the entity on behalf of which module operations are to be performed.
     * @param isTrusted - {@code true} if the middleware should not audit data delivery, since the
     * callback is being delivered to another trusted component which will audit access.
     * listModules() must be called prior to calling this method and the provided handle must be
     * one of the handles from the returned list.
     */
    @Override public android.media.soundtrigger_middleware.ISoundTriggerModule attachAsMiddleman(int handle, android.media.permission.Identity middlemanIdentity, android.media.permission.Identity originatorIdentity, android.media.soundtrigger_middleware.ISoundTriggerCallback callback, boolean isTrusted) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Attach an injection interface interface to the ST mock HAL.
     * See {@link ISoundTriggerInjection} for injection details.
     * If another client attaches, this session will be pre-empted.
     */
    @Override public void attachFakeHalInjection(android.media.soundtrigger_middleware.ISoundTriggerInjection injection) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.media.soundtrigger_middleware.ISoundTriggerMiddlewareService
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.media.soundtrigger_middleware.ISoundTriggerMiddlewareService interface,
     * generating a proxy if needed.
     */
    public static android.media.soundtrigger_middleware.ISoundTriggerMiddlewareService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.media.soundtrigger_middleware.ISoundTriggerMiddlewareService))) {
        return ((android.media.soundtrigger_middleware.ISoundTriggerMiddlewareService)iin);
      }
      return new android.media.soundtrigger_middleware.ISoundTriggerMiddlewareService.Stub.Proxy(obj);
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
        case TRANSACTION_listModulesAsOriginator:
        {
          android.media.permission.Identity _arg0;
          _arg0 = data.readTypedObject(android.media.permission.Identity.CREATOR);
          data.enforceNoDataAvail();
          android.media.soundtrigger_middleware.SoundTriggerModuleDescriptor[] _result = this.listModulesAsOriginator(_arg0);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_listModulesAsMiddleman:
        {
          android.media.permission.Identity _arg0;
          _arg0 = data.readTypedObject(android.media.permission.Identity.CREATOR);
          android.media.permission.Identity _arg1;
          _arg1 = data.readTypedObject(android.media.permission.Identity.CREATOR);
          data.enforceNoDataAvail();
          android.media.soundtrigger_middleware.SoundTriggerModuleDescriptor[] _result = this.listModulesAsMiddleman(_arg0, _arg1);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_attachAsOriginator:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.media.permission.Identity _arg1;
          _arg1 = data.readTypedObject(android.media.permission.Identity.CREATOR);
          android.media.soundtrigger_middleware.ISoundTriggerCallback _arg2;
          _arg2 = android.media.soundtrigger_middleware.ISoundTriggerCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          android.media.soundtrigger_middleware.ISoundTriggerModule _result = this.attachAsOriginator(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_attachAsMiddleman:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.media.permission.Identity _arg1;
          _arg1 = data.readTypedObject(android.media.permission.Identity.CREATOR);
          android.media.permission.Identity _arg2;
          _arg2 = data.readTypedObject(android.media.permission.Identity.CREATOR);
          android.media.soundtrigger_middleware.ISoundTriggerCallback _arg3;
          _arg3 = android.media.soundtrigger_middleware.ISoundTriggerCallback.Stub.asInterface(data.readStrongBinder());
          boolean _arg4;
          _arg4 = data.readBoolean();
          data.enforceNoDataAvail();
          android.media.soundtrigger_middleware.ISoundTriggerModule _result = this.attachAsMiddleman(_arg0, _arg1, _arg2, _arg3, _arg4);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_attachFakeHalInjection:
        {
          android.media.soundtrigger_middleware.ISoundTriggerInjection _arg0;
          _arg0 = android.media.soundtrigger_middleware.ISoundTriggerInjection.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.attachFakeHalInjection(_arg0);
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
    private static class Proxy implements android.media.soundtrigger_middleware.ISoundTriggerMiddlewareService
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
       * Query the available modules and their capabilities.
       * 
       * This variant is intended for use by the originator of the operations for permission
       * enforcement purposes. The provided identity's uid/pid fields will be ignored and overridden
       * by the ones provided by Binder.getCallingUid() / Binder.getCallingPid().
       */
      @Override public android.media.soundtrigger_middleware.SoundTriggerModuleDescriptor[] listModulesAsOriginator(android.media.permission.Identity identity) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.soundtrigger_middleware.SoundTriggerModuleDescriptor[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(identity, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_listModulesAsOriginator, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.media.soundtrigger_middleware.SoundTriggerModuleDescriptor.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Query the available modules and their capabilities.
       * 
       * This variant is intended for use by a trusted "middleman", acting on behalf of some identity
       * other than itself. The caller must provide:
       * - Its own identity, which will be used to establish trust via the
       *   SOUNDTRIGGER_DELEGATE_IDENTITY permission. This identity's uid/pid fields will be ignored
       *   and overridden by the ones provided by Binder.getCallingUid() / Binder.getCallingPid().
       *   This implies that the caller must clear its caller identity to protect from the case where
       *   it resides in the same process as the callee.
       * - The identity of the entity on behalf of which module operations are to be performed.
       */
      @Override public android.media.soundtrigger_middleware.SoundTriggerModuleDescriptor[] listModulesAsMiddleman(android.media.permission.Identity middlemanIdentity, android.media.permission.Identity originatorIdentity) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.soundtrigger_middleware.SoundTriggerModuleDescriptor[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(middlemanIdentity, 0);
          _data.writeTypedObject(originatorIdentity, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_listModulesAsMiddleman, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.media.soundtrigger_middleware.SoundTriggerModuleDescriptor.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Attach to one of the available modules.
       * 
       * This variant is intended for use by the originator of the operations for permission
       * enforcement purposes. The provided identity's uid/pid fields will be ignored and overridden
       * by the ones provided by Binder.getCallingUid() / Binder.getCallingPid().
       * 
       * listModules() must be called prior to calling this method and the provided handle must be
       * one of the handles from the returned list.
       */
      @Override public android.media.soundtrigger_middleware.ISoundTriggerModule attachAsOriginator(int handle, android.media.permission.Identity identity, android.media.soundtrigger_middleware.ISoundTriggerCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.soundtrigger_middleware.ISoundTriggerModule _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(handle);
          _data.writeTypedObject(identity, 0);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_attachAsOriginator, _data, _reply, 0);
          _reply.readException();
          _result = android.media.soundtrigger_middleware.ISoundTriggerModule.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Attach to one of the available modules.
       * 
       * This variant is intended for use by a trusted "middleman", acting on behalf of some identity
       * other than itself. The caller must provide:
       * - Its own identity, which will be used to establish trust via the
       *   SOUNDTRIGGER_DELEGATE_IDENTITY permission. This identity's uid/pid fields will be ignored
       *   and overridden by the ones provided by Binder.getCallingUid() / Binder.getCallingPid().
       *   This implies that the caller must clear its caller identity to protect from the case where
       *   it resides in the same process as the callee.
       * - The identity of the entity on behalf of which module operations are to be performed.
       * @param isTrusted - {@code true} if the middleware should not audit data delivery, since the
       * callback is being delivered to another trusted component which will audit access.
       * listModules() must be called prior to calling this method and the provided handle must be
       * one of the handles from the returned list.
       */
      @Override public android.media.soundtrigger_middleware.ISoundTriggerModule attachAsMiddleman(int handle, android.media.permission.Identity middlemanIdentity, android.media.permission.Identity originatorIdentity, android.media.soundtrigger_middleware.ISoundTriggerCallback callback, boolean isTrusted) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.media.soundtrigger_middleware.ISoundTriggerModule _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(handle);
          _data.writeTypedObject(middlemanIdentity, 0);
          _data.writeTypedObject(originatorIdentity, 0);
          _data.writeStrongInterface(callback);
          _data.writeBoolean(isTrusted);
          boolean _status = mRemote.transact(Stub.TRANSACTION_attachAsMiddleman, _data, _reply, 0);
          _reply.readException();
          _result = android.media.soundtrigger_middleware.ISoundTriggerModule.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Attach an injection interface interface to the ST mock HAL.
       * See {@link ISoundTriggerInjection} for injection details.
       * If another client attaches, this session will be pre-empted.
       */
      @Override public void attachFakeHalInjection(android.media.soundtrigger_middleware.ISoundTriggerInjection injection) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(injection);
          boolean _status = mRemote.transact(Stub.TRANSACTION_attachFakeHalInjection, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_listModulesAsOriginator = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_listModulesAsMiddleman = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_attachAsOriginator = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_attachAsMiddleman = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_attachFakeHalInjection = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
  }
  public static final java.lang.String DESCRIPTOR = "android$media$soundtrigger_middleware$ISoundTriggerMiddlewareService".replace('$', '.');
  /**
   * Query the available modules and their capabilities.
   * 
   * This variant is intended for use by the originator of the operations for permission
   * enforcement purposes. The provided identity's uid/pid fields will be ignored and overridden
   * by the ones provided by Binder.getCallingUid() / Binder.getCallingPid().
   */
  public android.media.soundtrigger_middleware.SoundTriggerModuleDescriptor[] listModulesAsOriginator(android.media.permission.Identity identity) throws android.os.RemoteException;
  /**
   * Query the available modules and their capabilities.
   * 
   * This variant is intended for use by a trusted "middleman", acting on behalf of some identity
   * other than itself. The caller must provide:
   * - Its own identity, which will be used to establish trust via the
   *   SOUNDTRIGGER_DELEGATE_IDENTITY permission. This identity's uid/pid fields will be ignored
   *   and overridden by the ones provided by Binder.getCallingUid() / Binder.getCallingPid().
   *   This implies that the caller must clear its caller identity to protect from the case where
   *   it resides in the same process as the callee.
   * - The identity of the entity on behalf of which module operations are to be performed.
   */
  public android.media.soundtrigger_middleware.SoundTriggerModuleDescriptor[] listModulesAsMiddleman(android.media.permission.Identity middlemanIdentity, android.media.permission.Identity originatorIdentity) throws android.os.RemoteException;
  /**
   * Attach to one of the available modules.
   * 
   * This variant is intended for use by the originator of the operations for permission
   * enforcement purposes. The provided identity's uid/pid fields will be ignored and overridden
   * by the ones provided by Binder.getCallingUid() / Binder.getCallingPid().
   * 
   * listModules() must be called prior to calling this method and the provided handle must be
   * one of the handles from the returned list.
   */
  public android.media.soundtrigger_middleware.ISoundTriggerModule attachAsOriginator(int handle, android.media.permission.Identity identity, android.media.soundtrigger_middleware.ISoundTriggerCallback callback) throws android.os.RemoteException;
  /**
   * Attach to one of the available modules.
   * 
   * This variant is intended for use by a trusted "middleman", acting on behalf of some identity
   * other than itself. The caller must provide:
   * - Its own identity, which will be used to establish trust via the
   *   SOUNDTRIGGER_DELEGATE_IDENTITY permission. This identity's uid/pid fields will be ignored
   *   and overridden by the ones provided by Binder.getCallingUid() / Binder.getCallingPid().
   *   This implies that the caller must clear its caller identity to protect from the case where
   *   it resides in the same process as the callee.
   * - The identity of the entity on behalf of which module operations are to be performed.
   * @param isTrusted - {@code true} if the middleware should not audit data delivery, since the
   * callback is being delivered to another trusted component which will audit access.
   * listModules() must be called prior to calling this method and the provided handle must be
   * one of the handles from the returned list.
   */
  public android.media.soundtrigger_middleware.ISoundTriggerModule attachAsMiddleman(int handle, android.media.permission.Identity middlemanIdentity, android.media.permission.Identity originatorIdentity, android.media.soundtrigger_middleware.ISoundTriggerCallback callback, boolean isTrusted) throws android.os.RemoteException;
  /**
   * Attach an injection interface interface to the ST mock HAL.
   * See {@link ISoundTriggerInjection} for injection details.
   * If another client attaches, this session will be pre-empted.
   */
  public void attachFakeHalInjection(android.media.soundtrigger_middleware.ISoundTriggerInjection injection) throws android.os.RemoteException;
}
