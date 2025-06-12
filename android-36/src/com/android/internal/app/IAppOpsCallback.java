/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current --ninja -d out/soong/.intermediates/frameworks/native/libs/permission/framework-permission-aidl-java-source/gen/com/android/internal/app/IAppOpsCallback.java.d -o out/soong/.intermediates/frameworks/native/libs/permission/framework-permission-aidl-java-source/gen -Nframeworks/native/libs/permission/aidl frameworks/native/libs/permission/aidl/com/android/internal/app/IAppOpsCallback.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package com.android.internal.app;
public interface IAppOpsCallback extends android.os.IInterface
{
  /** Default implementation for IAppOpsCallback. */
  public static class Default implements com.android.internal.app.IAppOpsCallback
  {
    @Override public void opChanged(int op, int uid, java.lang.String packageName, java.lang.String persistentDeviceId) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.android.internal.app.IAppOpsCallback
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.android.internal.app.IAppOpsCallback interface,
     * generating a proxy if needed.
     */
    public static com.android.internal.app.IAppOpsCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.android.internal.app.IAppOpsCallback))) {
        return ((com.android.internal.app.IAppOpsCallback)iin);
      }
      return new com.android.internal.app.IAppOpsCallback.Stub.Proxy(obj);
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
        case TRANSACTION_opChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          java.lang.String _arg2;
          _arg2 = data.readString();
          java.lang.String _arg3;
          _arg3 = data.readString();
          data.enforceNoDataAvail();
          this.opChanged(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.android.internal.app.IAppOpsCallback
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
      @Override public void opChanged(int op, int uid, java.lang.String packageName, java.lang.String persistentDeviceId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(op);
          _data.writeInt(uid);
          _data.writeString(packageName);
          _data.writeString(persistentDeviceId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_opChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    /** @hide */
    public static final java.lang.String DESCRIPTOR = "com.android.internal.app.IAppOpsCallback";
    static final int TRANSACTION_opChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
  }
  public void opChanged(int op, int uid, java.lang.String packageName, java.lang.String persistentDeviceId) throws android.os.RemoteException;
}
