/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current --ninja -d out/soong/.intermediates/frameworks/base/core/java/dynamic_instrumentation_manager_aidl-java-source/gen/android/os/instrumentation/IDynamicInstrumentationManager.java.d -o out/soong/.intermediates/frameworks/base/core/java/dynamic_instrumentation_manager_aidl-java-source/gen -Nframeworks/base/core/java frameworks/base/core/java/android/os/instrumentation/IDynamicInstrumentationManager.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.os.instrumentation;
/**
 * System private API for managing the dynamic attachment of instrumentation.
 * 
 * {@hide}
 */
public interface IDynamicInstrumentationManager extends android.os.IInterface
{
  /** Default implementation for IDynamicInstrumentationManager. */
  public static class Default implements android.os.instrumentation.IDynamicInstrumentationManager
  {
    /** Provides ART metadata about the described compiled method within the target process */
    @Override public void getExecutableMethodFileOffsets(android.os.instrumentation.TargetProcess targetProcess, android.os.instrumentation.MethodDescriptor methodDescriptor, android.os.instrumentation.IOffsetCallback callback) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.os.instrumentation.IDynamicInstrumentationManager
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.os.instrumentation.IDynamicInstrumentationManager interface,
     * generating a proxy if needed.
     */
    public static android.os.instrumentation.IDynamicInstrumentationManager asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.os.instrumentation.IDynamicInstrumentationManager))) {
        return ((android.os.instrumentation.IDynamicInstrumentationManager)iin);
      }
      return new android.os.instrumentation.IDynamicInstrumentationManager.Stub.Proxy(obj);
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
        case TRANSACTION_getExecutableMethodFileOffsets:
        {
          android.os.instrumentation.TargetProcess _arg0;
          _arg0 = data.readTypedObject(android.os.instrumentation.TargetProcess.CREATOR);
          android.os.instrumentation.MethodDescriptor _arg1;
          _arg1 = data.readTypedObject(android.os.instrumentation.MethodDescriptor.CREATOR);
          android.os.instrumentation.IOffsetCallback _arg2;
          _arg2 = android.os.instrumentation.IOffsetCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.getExecutableMethodFileOffsets(_arg0, _arg1, _arg2);
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
    private static class Proxy implements android.os.instrumentation.IDynamicInstrumentationManager
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
      /** Provides ART metadata about the described compiled method within the target process */
      @Override public void getExecutableMethodFileOffsets(android.os.instrumentation.TargetProcess targetProcess, android.os.instrumentation.MethodDescriptor methodDescriptor, android.os.instrumentation.IOffsetCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(targetProcess, 0);
          _data.writeTypedObject(methodDescriptor, 0);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getExecutableMethodFileOffsets, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_getExecutableMethodFileOffsets = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android.os.instrumentation.IDynamicInstrumentationManager";
  /** Provides ART metadata about the described compiled method within the target process */
  public void getExecutableMethodFileOffsets(android.os.instrumentation.TargetProcess targetProcess, android.os.instrumentation.MethodDescriptor methodDescriptor, android.os.instrumentation.IOffsetCallback callback) throws android.os.RemoteException;
}
