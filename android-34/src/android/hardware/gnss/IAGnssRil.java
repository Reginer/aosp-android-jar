/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.gnss;
/** @hide */
public interface IAGnssRil extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "fc957f1d3d261d065ff5e5415f2d21caa79c310f";
  /** Default implementation for IAGnssRil. */
  public static class Default implements android.hardware.gnss.IAGnssRil
  {
    @Override public void setCallback(android.hardware.gnss.IAGnssRilCallback callback) throws android.os.RemoteException
    {
    }
    @Override public void setRefLocation(android.hardware.gnss.IAGnssRil.AGnssRefLocation agnssReflocation) throws android.os.RemoteException
    {
    }
    @Override public void setSetId(int type, java.lang.String setid) throws android.os.RemoteException
    {
    }
    @Override public void updateNetworkState(android.hardware.gnss.IAGnssRil.NetworkAttributes attributes) throws android.os.RemoteException
    {
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.gnss.IAGnssRil
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.gnss.IAGnssRil interface,
     * generating a proxy if needed.
     */
    public static android.hardware.gnss.IAGnssRil asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.gnss.IAGnssRil))) {
        return ((android.hardware.gnss.IAGnssRil)iin);
      }
      return new android.hardware.gnss.IAGnssRil.Stub.Proxy(obj);
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
        case TRANSACTION_setCallback:
        {
          return "setCallback";
        }
        case TRANSACTION_setRefLocation:
        {
          return "setRefLocation";
        }
        case TRANSACTION_setSetId:
        {
          return "setSetId";
        }
        case TRANSACTION_updateNetworkState:
        {
          return "updateNetworkState";
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
        case TRANSACTION_setCallback:
        {
          android.hardware.gnss.IAGnssRilCallback _arg0;
          _arg0 = android.hardware.gnss.IAGnssRilCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.setCallback(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setRefLocation:
        {
          android.hardware.gnss.IAGnssRil.AGnssRefLocation _arg0;
          _arg0 = data.readTypedObject(android.hardware.gnss.IAGnssRil.AGnssRefLocation.CREATOR);
          data.enforceNoDataAvail();
          this.setRefLocation(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setSetId:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          data.enforceNoDataAvail();
          this.setSetId(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_updateNetworkState:
        {
          android.hardware.gnss.IAGnssRil.NetworkAttributes _arg0;
          _arg0 = data.readTypedObject(android.hardware.gnss.IAGnssRil.NetworkAttributes.CREATOR);
          data.enforceNoDataAvail();
          this.updateNetworkState(_arg0);
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
    private static class Proxy implements android.hardware.gnss.IAGnssRil
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
      @Override public void setCallback(android.hardware.gnss.IAGnssRilCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCallback, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setCallback is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setRefLocation(android.hardware.gnss.IAGnssRil.AGnssRefLocation agnssReflocation) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(agnssReflocation, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setRefLocation, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setRefLocation is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setSetId(int type, java.lang.String setid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeString(setid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setSetId, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setSetId is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void updateNetworkState(android.hardware.gnss.IAGnssRil.NetworkAttributes attributes) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(attributes, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_updateNetworkState, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method updateNetworkState is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
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
    static final int TRANSACTION_setCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_setRefLocation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_setSetId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_updateNetworkState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$gnss$IAGnssRil".replace('$', '.');
  public static final int NETWORK_CAPABILITY_NOT_METERED = 1;
  public static final int NETWORK_CAPABILITY_NOT_ROAMING = 2;
  public void setCallback(android.hardware.gnss.IAGnssRilCallback callback) throws android.os.RemoteException;
  public void setRefLocation(android.hardware.gnss.IAGnssRil.AGnssRefLocation agnssReflocation) throws android.os.RemoteException;
  public void setSetId(int type, java.lang.String setid) throws android.os.RemoteException;
  public void updateNetworkState(android.hardware.gnss.IAGnssRil.NetworkAttributes attributes) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
  public static @interface AGnssRefLocationType {
    public static final int GSM_CELLID = 1;
    public static final int UMTS_CELLID = 2;
    public static final int LTE_CELLID = 4;
    public static final int NR_CELLID = 8;
  }
  public static @interface SetIdType {
    public static final int NONE = 0;
    public static final int IMSI = 1;
    public static final int MSISDM = 2;
  }
  public static class AGnssRefLocationCellID implements android.os.Parcelable
  {
    public int type;
    public int mcc = 0;
    public int mnc = 0;
    public int lac = 0;
    public long cid = 0L;
    public int tac = 0;
    public int pcid = 0;
    public int arfcn = 0;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<AGnssRefLocationCellID> CREATOR = new android.os.Parcelable.Creator<AGnssRefLocationCellID>() {
      @Override
      public AGnssRefLocationCellID createFromParcel(android.os.Parcel _aidl_source) {
        AGnssRefLocationCellID _aidl_out = new AGnssRefLocationCellID();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public AGnssRefLocationCellID[] newArray(int _aidl_size) {
        return new AGnssRefLocationCellID[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeInt(type);
      _aidl_parcel.writeInt(mcc);
      _aidl_parcel.writeInt(mnc);
      _aidl_parcel.writeInt(lac);
      _aidl_parcel.writeLong(cid);
      _aidl_parcel.writeInt(tac);
      _aidl_parcel.writeInt(pcid);
      _aidl_parcel.writeInt(arfcn);
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
        type = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        mcc = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        mnc = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        lac = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        cid = _aidl_parcel.readLong();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        tac = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        pcid = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        arfcn = _aidl_parcel.readInt();
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
  public static class AGnssRefLocation implements android.os.Parcelable
  {
    public int type;
    public android.hardware.gnss.IAGnssRil.AGnssRefLocationCellID cellID;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<AGnssRefLocation> CREATOR = new android.os.Parcelable.Creator<AGnssRefLocation>() {
      @Override
      public AGnssRefLocation createFromParcel(android.os.Parcel _aidl_source) {
        AGnssRefLocation _aidl_out = new AGnssRefLocation();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public AGnssRefLocation[] newArray(int _aidl_size) {
        return new AGnssRefLocation[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeInt(type);
      _aidl_parcel.writeTypedObject(cellID, _aidl_flag);
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
        type = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        cellID = _aidl_parcel.readTypedObject(android.hardware.gnss.IAGnssRil.AGnssRefLocationCellID.CREATOR);
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
      _mask |= describeContents(cellID);
      return _mask;
    }
    private int describeContents(Object _v) {
      if (_v == null) return 0;
      if (_v instanceof android.os.Parcelable) {
        return ((android.os.Parcelable) _v).describeContents();
      }
      return 0;
    }
  }
  public static class NetworkAttributes implements android.os.Parcelable
  {
    public long networkHandle = 0L;
    public boolean isConnected = false;
    public int capabilities = 0;
    public java.lang.String apn;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<NetworkAttributes> CREATOR = new android.os.Parcelable.Creator<NetworkAttributes>() {
      @Override
      public NetworkAttributes createFromParcel(android.os.Parcel _aidl_source) {
        NetworkAttributes _aidl_out = new NetworkAttributes();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public NetworkAttributes[] newArray(int _aidl_size) {
        return new NetworkAttributes[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeLong(networkHandle);
      _aidl_parcel.writeBoolean(isConnected);
      _aidl_parcel.writeInt(capabilities);
      _aidl_parcel.writeString(apn);
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
        networkHandle = _aidl_parcel.readLong();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        isConnected = _aidl_parcel.readBoolean();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        capabilities = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        apn = _aidl_parcel.readString();
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
