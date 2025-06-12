/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current --ninja -d out/soong/.intermediates/frameworks/base/core/java/dynamic_instrumentation_manager_aidl-java-source/gen/android/os/instrumentation/IOffsetCallback.java.d -o out/soong/.intermediates/frameworks/base/core/java/dynamic_instrumentation_manager_aidl-java-source/gen -Nframeworks/base/core/java frameworks/base/core/java/android/os/instrumentation/IOffsetCallback.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.os.instrumentation;
/**
 * System private API for providing dynamic instrumentation offset results.
 * 
 * {@hide}
 */
public interface IOffsetCallback extends android.os.IInterface
{
  /** Default implementation for IOffsetCallback. */
  public static class Default implements android.os.instrumentation.IOffsetCallback
  {
    @Override public void onResult(android.os.instrumentation.ExecutableMethodFileOffsets offsets) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.os.instrumentation.IOffsetCallback
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.os.instrumentation.IOffsetCallback interface,
     * generating a proxy if needed.
     */
    public static android.os.instrumentation.IOffsetCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.os.instrumentation.IOffsetCallback))) {
        return ((android.os.instrumentation.IOffsetCallback)iin);
      }
      return new android.os.instrumentation.IOffsetCallback.Stub.Proxy(obj);
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
        case TRANSACTION_onResult:
        {
          android.os.instrumentation.ExecutableMethodFileOffsets _arg0;
          _arg0 = data.readTypedObject(android.os.instrumentation.ExecutableMethodFileOffsets.CREATOR);
          data.enforceNoDataAvail();
          this.onResult(_arg0);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.os.instrumentation.IOffsetCallback
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
      @Override public void onResult(android.os.instrumentation.ExecutableMethodFileOffsets offsets) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(offsets, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onResult, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onResult = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android.os.instrumentation.IOffsetCallback";
  public void onResult(android.os.instrumentation.ExecutableMethodFileOffsets offsets) throws android.os.RemoteException;
}
