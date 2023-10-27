/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.gnss.visibility_control;
/** @hide */
public interface IGnssVisibilityControlCallback extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "fc957f1d3d261d065ff5e5415f2d21caa79c310f";
  /** Default implementation for IGnssVisibilityControlCallback. */
  public static class Default implements android.hardware.gnss.visibility_control.IGnssVisibilityControlCallback
  {
    @Override public void nfwNotifyCb(android.hardware.gnss.visibility_control.IGnssVisibilityControlCallback.NfwNotification notification) throws android.os.RemoteException
    {
    }
    @Override public boolean isInEmergencySession() throws android.os.RemoteException
    {
      return false;
    }
    @Override
    public int getInterfaceVersion() {
      return 0;
    }
    @Override
    public String getInterfaceHash() {
      return "";
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.hardware.gnss.visibility_control.IGnssVisibilityControlCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.gnss.visibility_control.IGnssVisibilityControlCallback interface,
     * generating a proxy if needed.
     */
    public static android.hardware.gnss.visibility_control.IGnssVisibilityControlCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.gnss.visibility_control.IGnssVisibilityControlCallback))) {
        return ((android.hardware.gnss.visibility_control.IGnssVisibilityControlCallback)iin);
      }
      return new android.hardware.gnss.visibility_control.IGnssVisibilityControlCallback.Stub.Proxy(obj);
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
        case TRANSACTION_nfwNotifyCb:
        {
          return "nfwNotifyCb";
        }
        case TRANSACTION_isInEmergencySession:
        {
          return "isInEmergencySession";
        }
        case TRANSACTION_getInterfaceVersion:
        {
          return "getInterfaceVersion";
        }
        case TRANSACTION_getInterfaceHash:
        {
          return "getInterfaceHash";
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
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
        case TRANSACTION_getInterfaceVersion:
        {
          reply.writeNoException();
          reply.writeInt(getInterfaceVersion());
          return true;
        }
        case TRANSACTION_getInterfaceHash:
        {
          reply.writeNoException();
          reply.writeString(getInterfaceHash());
          return true;
        }
      }
      switch (code)
      {
        case TRANSACTION_nfwNotifyCb:
        {
          android.hardware.gnss.visibility_control.IGnssVisibilityControlCallback.NfwNotification _arg0;
          _arg0 = data.readTypedObject(android.hardware.gnss.visibility_control.IGnssVisibilityControlCallback.NfwNotification.CREATOR);
          data.enforceNoDataAvail();
          this.nfwNotifyCb(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_isInEmergencySession:
        {
          boolean _result = this.isInEmergencySession();
          reply.writeNoException();
          reply.writeBoolean(_result);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.gnss.visibility_control.IGnssVisibilityControlCallback
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      private int mCachedVersion = -1;
      private String mCachedHash = "-1";
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public void nfwNotifyCb(android.hardware.gnss.visibility_control.IGnssVisibilityControlCallback.NfwNotification notification) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(notification, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_nfwNotifyCb, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method nfwNotifyCb is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public boolean isInEmergencySession() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isInEmergencySession, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method isInEmergencySession is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readBoolean();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override
      public int getInterfaceVersion() throws android.os.RemoteException {
        if (mCachedVersion == -1) {
          android.os.Parcel data = android.os.Parcel.obtain(asBinder());
          android.os.Parcel reply = android.os.Parcel.obtain();
          try {
            data.writeInterfaceToken(DESCRIPTOR);
            boolean _status = mRemote.transact(Stub.TRANSACTION_getInterfaceVersion, data, reply, 0);
            reply.readException();
            mCachedVersion = reply.readInt();
          } finally {
            reply.recycle();
            data.recycle();
          }
        }
        return mCachedVersion;
      }
      @Override
      public synchronized String getInterfaceHash() throws android.os.RemoteException {
        if ("-1".equals(mCachedHash)) {
          android.os.Parcel data = android.os.Parcel.obtain(asBinder());
          android.os.Parcel reply = android.os.Parcel.obtain();
          try {
            data.writeInterfaceToken(DESCRIPTOR);
            boolean _status = mRemote.transact(Stub.TRANSACTION_getInterfaceHash, data, reply, 0);
            reply.readException();
            mCachedHash = reply.readString();
          } finally {
            reply.recycle();
            data.recycle();
          }
        }
        return mCachedHash;
      }
    }
    static final int TRANSACTION_nfwNotifyCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_isInEmergencySession = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$gnss$visibility_control$IGnssVisibilityControlCallback".replace('$', '.');
  public void nfwNotifyCb(android.hardware.gnss.visibility_control.IGnssVisibilityControlCallback.NfwNotification notification) throws android.os.RemoteException;
  public boolean isInEmergencySession() throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
  public static @interface NfwProtocolStack {
    public static final int CTRL_PLANE = 0;
    public static final int SUPL = 1;
    public static final int IMS = 10;
    public static final int SIM = 11;
    public static final int OTHER_PROTOCOL_STACK = 100;
  }
  public static @interface NfwRequestor {
    public static final int CARRIER = 0;
    public static final int OEM = 10;
    public static final int MODEM_CHIPSET_VENDOR = 11;
    public static final int GNSS_CHIPSET_VENDOR = 12;
    public static final int OTHER_CHIPSET_VENDOR = 13;
    public static final int AUTOMOBILE_CLIENT = 20;
    public static final int OTHER_REQUESTOR = 100;
  }
  public static @interface NfwResponseType {
    public static final int REJECTED = 0;
    public static final int ACCEPTED_NO_LOCATION_PROVIDED = 1;
    public static final int ACCEPTED_LOCATION_PROVIDED = 2;
  }
  public static class NfwNotification implements android.os.Parcelable
  {
    public java.lang.String proxyAppPackageName;
    public int protocolStack;
    public java.lang.String otherProtocolStackName;
    public int requestor;
    public java.lang.String requestorId;
    public int responseType;
    public boolean inEmergencyMode = false;
    public boolean isCachedLocation = false;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<NfwNotification> CREATOR = new android.os.Parcelable.Creator<NfwNotification>() {
      @Override
      public NfwNotification createFromParcel(android.os.Parcel _aidl_source) {
        NfwNotification _aidl_out = new NfwNotification();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public NfwNotification[] newArray(int _aidl_size) {
        return new NfwNotification[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeString(proxyAppPackageName);
      _aidl_parcel.writeInt(protocolStack);
      _aidl_parcel.writeString(otherProtocolStackName);
      _aidl_parcel.writeInt(requestor);
      _aidl_parcel.writeString(requestorId);
      _aidl_parcel.writeInt(responseType);
      _aidl_parcel.writeBoolean(inEmergencyMode);
      _aidl_parcel.writeBoolean(isCachedLocation);
      int _aidl_end_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.setDataPosition(_aidl_start_pos);
      _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
      _aidl_parcel.setDataPosition(_aidl_end_pos);
    }
    public final void readFromParcel(android.os.Parcel _aidl_parcel)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      int _aidl_parcelable_size = _aidl_parcel.readInt();
      try {
        if (_aidl_parcelable_size < 4) throw new android.os.BadParcelableException("Parcelable too small");;
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        proxyAppPackageName = _aidl_parcel.readString();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        protocolStack = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        otherProtocolStackName = _aidl_parcel.readString();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        requestor = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        requestorId = _aidl_parcel.readString();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        responseType = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        inEmergencyMode = _aidl_parcel.readBoolean();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        isCachedLocation = _aidl_parcel.readBoolean();
      } finally {
        if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
          throw new android.os.BadParcelableException("Overflow in the size of parcelable");
        }
        _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
      }
    }
    @Override
    public int describeContents() {
      int _mask = 0;
      return _mask;
    }
  }
}
