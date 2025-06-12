/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current --ninja -d out/soong/.intermediates/frameworks/native/libs/binder/packagemanager_aidl-java-source/gen/android/content/pm/IStagedApexObserver.java.d -o out/soong/.intermediates/frameworks/native/libs/binder/packagemanager_aidl-java-source/gen -Nframeworks/native/libs/binder/aidl frameworks/native/libs/binder/aidl/android/content/pm/IStagedApexObserver.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.content.pm;
/**
 * This is a non-blocking notification when set of staged apex has changed
 * 
 * @hide
 */
public interface IStagedApexObserver extends android.os.IInterface
{
  /** Default implementation for IStagedApexObserver. */
  public static class Default implements android.content.pm.IStagedApexObserver
  {
    @Override public void onApexStaged(android.content.pm.ApexStagedEvent event) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.content.pm.IStagedApexObserver
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.content.pm.IStagedApexObserver interface,
     * generating a proxy if needed.
     */
    public static android.content.pm.IStagedApexObserver asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.content.pm.IStagedApexObserver))) {
        return ((android.content.pm.IStagedApexObserver)iin);
      }
      return new android.content.pm.IStagedApexObserver.Stub.Proxy(obj);
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
        case TRANSACTION_onApexStaged:
        {
          android.content.pm.ApexStagedEvent _arg0;
          _arg0 = data.readTypedObject(android.content.pm.ApexStagedEvent.CREATOR);
          data.enforceNoDataAvail();
          this.onApexStaged(_arg0);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.content.pm.IStagedApexObserver
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
      @Override public void onApexStaged(android.content.pm.ApexStagedEvent event) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(event, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onApexStaged, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onApexStaged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android.content.pm.IStagedApexObserver";
  public void onApexStaged(android.content.pm.ApexStagedEvent event) throws android.os.RemoteException;
}
