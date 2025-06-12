/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation -t --min_sdk_version platform_apis -pout/soong/.intermediates/system/hardware/interfaces/keystore2/aidl/android.system.keystore2_interface/5/preprocessed.aidl --ninja -d out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen/android/security/metrics/IKeystoreMetrics.java.d -o out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen -Nsystem/security/keystore2/aidl system/security/keystore2/aidl/android/security/metrics/IKeystoreMetrics.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.security.metrics;
/**
 * IKeystoreMetrics interface exposes the method for system server to pull metrics from keystore.
 * @hide
 */
public interface IKeystoreMetrics extends android.os.IInterface
{
  /** Default implementation for IKeystoreMetrics. */
  public static class Default implements android.security.metrics.IKeystoreMetrics
  {
    /**
     * Allows the metrics routing proxy to pull the metrics from keystore.
     * 
     * @return an array of KeystoreAtom objects with the atomID. There can be multiple atom objects
     * for the same atomID, encapsulating different combinations of values for the atom fields.
     * If there is no atom object found for the atomID in the metrics store, an empty array is
     * returned.
     * 
     * Callers require 'PullMetrics' permission.
     * 
     * @param atomID - ID of the atom to be pulled.
     * 
     * Errors are reported as service specific errors.
     */
    @Override public android.security.metrics.KeystoreAtom[] pullMetrics(int atomID) throws android.os.RemoteException
    {
      return null;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.security.metrics.IKeystoreMetrics
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.security.metrics.IKeystoreMetrics interface,
     * generating a proxy if needed.
     */
    public static android.security.metrics.IKeystoreMetrics asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.security.metrics.IKeystoreMetrics))) {
        return ((android.security.metrics.IKeystoreMetrics)iin);
      }
      return new android.security.metrics.IKeystoreMetrics.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    /** @hide */
    public static java.lang.String getDefaultTransactionName(int transactionCode)
    {
      switch (transactionCode)
      {
        case TRANSACTION_pullMetrics:
        {
          return "pullMetrics";
        }
        default:
        {
          return null;
        }
      }
    }
    /** @hide */
    public java.lang.String getTransactionName(int transactionCode)
    {
      return this.getDefaultTransactionName(transactionCode);
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
        case TRANSACTION_pullMetrics:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          android.security.metrics.KeystoreAtom[] _result = this.pullMetrics(_arg0);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.security.metrics.IKeystoreMetrics
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
       * Allows the metrics routing proxy to pull the metrics from keystore.
       * 
       * @return an array of KeystoreAtom objects with the atomID. There can be multiple atom objects
       * for the same atomID, encapsulating different combinations of values for the atom fields.
       * If there is no atom object found for the atomID in the metrics store, an empty array is
       * returned.
       * 
       * Callers require 'PullMetrics' permission.
       * 
       * @param atomID - ID of the atom to be pulled.
       * 
       * Errors are reported as service specific errors.
       */
      @Override public android.security.metrics.KeystoreAtom[] pullMetrics(int atomID) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.security.metrics.KeystoreAtom[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(atomID);
          boolean _status = mRemote.transact(Stub.TRANSACTION_pullMetrics, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createTypedArray(android.security.metrics.KeystoreAtom.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_pullMetrics = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 0;
    }
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android.security.metrics.IKeystoreMetrics";
  /**
   * Allows the metrics routing proxy to pull the metrics from keystore.
   * 
   * @return an array of KeystoreAtom objects with the atomID. There can be multiple atom objects
   * for the same atomID, encapsulating different combinations of values for the atom fields.
   * If there is no atom object found for the atomID in the metrics store, an empty array is
   * returned.
   * 
   * Callers require 'PullMetrics' permission.
   * 
   * @param atomID - ID of the atom to be pulled.
   * 
   * Errors are reported as service specific errors.
   */
  public android.security.metrics.KeystoreAtom[] pullMetrics(int atomID) throws android.os.RemoteException;
}
