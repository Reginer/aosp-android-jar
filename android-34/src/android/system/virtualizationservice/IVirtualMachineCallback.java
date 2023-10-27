/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.system.virtualizationservice;
/**
 * An object which a client may register with the VirtualizationService to get callbacks about the
 * state of a particular VM.
 */
public interface IVirtualMachineCallback extends android.os.IInterface
{
  /** Default implementation for IVirtualMachineCallback. */
  public static class Default implements android.system.virtualizationservice.IVirtualMachineCallback
  {
    /** Called when the payload starts in the VM. */
    @Override public void onPayloadStarted(int cid) throws android.os.RemoteException
    {
    }
    /** Called when the payload in the VM is ready to serve. */
    @Override public void onPayloadReady(int cid) throws android.os.RemoteException
    {
    }
    /** Called when the payload has finished in the VM. `exitCode` is the exit code of the payload. */
    @Override public void onPayloadFinished(int cid, int exitCode) throws android.os.RemoteException
    {
    }
    /** Called when an error occurs in the VM. */
    @Override public void onError(int cid, int errorCode, java.lang.String message) throws android.os.RemoteException
    {
    }
    /**
     * Called when the VM dies.
     * 
     * Note that this will not be called if the VirtualizationService itself dies, so you should
     * also use `link_to_death` to handle that.
     */
    @Override public void onDied(int cid, int reason) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.system.virtualizationservice.IVirtualMachineCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.system.virtualizationservice.IVirtualMachineCallback interface,
     * generating a proxy if needed.
     */
    public static android.system.virtualizationservice.IVirtualMachineCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.system.virtualizationservice.IVirtualMachineCallback))) {
        return ((android.system.virtualizationservice.IVirtualMachineCallback)iin);
      }
      return new android.system.virtualizationservice.IVirtualMachineCallback.Stub.Proxy(obj);
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
        case TRANSACTION_onPayloadStarted:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.onPayloadStarted(_arg0);
          break;
        }
        case TRANSACTION_onPayloadReady:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.onPayloadReady(_arg0);
          break;
        }
        case TRANSACTION_onPayloadFinished:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.onPayloadFinished(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onError:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          java.lang.String _arg2;
          _arg2 = data.readString();
          data.enforceNoDataAvail();
          this.onError(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_onDied:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.onDied(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.system.virtualizationservice.IVirtualMachineCallback
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
      /** Called when the payload starts in the VM. */
      @Override public void onPayloadStarted(int cid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(cid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onPayloadStarted, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Called when the payload in the VM is ready to serve. */
      @Override public void onPayloadReady(int cid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(cid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onPayloadReady, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Called when the payload has finished in the VM. `exitCode` is the exit code of the payload. */
      @Override public void onPayloadFinished(int cid, int exitCode) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(cid);
          _data.writeInt(exitCode);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onPayloadFinished, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Called when an error occurs in the VM. */
      @Override public void onError(int cid, int errorCode, java.lang.String message) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(cid);
          _data.writeInt(errorCode);
          _data.writeString(message);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onError, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Called when the VM dies.
       * 
       * Note that this will not be called if the VirtualizationService itself dies, so you should
       * also use `link_to_death` to handle that.
       */
      @Override public void onDied(int cid, int reason) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(cid);
          _data.writeInt(reason);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onDied, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onPayloadStarted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onPayloadReady = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_onPayloadFinished = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_onError = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_onDied = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
  }
  public static final java.lang.String DESCRIPTOR = "android$system$virtualizationservice$IVirtualMachineCallback".replace('$', '.');
  /** Called when the payload starts in the VM. */
  public void onPayloadStarted(int cid) throws android.os.RemoteException;
  /** Called when the payload in the VM is ready to serve. */
  public void onPayloadReady(int cid) throws android.os.RemoteException;
  /** Called when the payload has finished in the VM. `exitCode` is the exit code of the payload. */
  public void onPayloadFinished(int cid, int exitCode) throws android.os.RemoteException;
  /** Called when an error occurs in the VM. */
  public void onError(int cid, int errorCode, java.lang.String message) throws android.os.RemoteException;
  /**
   * Called when the VM dies.
   * 
   * Note that this will not be called if the VirtualizationService itself dies, so you should
   * also use `link_to_death` to handle that.
   */
  public void onDied(int cid, int reason) throws android.os.RemoteException;
}
