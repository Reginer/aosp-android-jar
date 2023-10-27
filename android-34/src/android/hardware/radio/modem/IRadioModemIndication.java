/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.modem;
public interface IRadioModemIndication extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "09927560afccc75a063944fbbab3af48099261ca";
  /** Default implementation for IRadioModemIndication. */
  public static class Default implements android.hardware.radio.modem.IRadioModemIndication
  {
    @Override public void hardwareConfigChanged(int type, android.hardware.radio.modem.HardwareConfig[] configs) throws android.os.RemoteException
    {
    }
    @Override public void modemReset(int type, java.lang.String reason) throws android.os.RemoteException
    {
    }
    @Override public void radioCapabilityIndication(int type, android.hardware.radio.modem.RadioCapability rc) throws android.os.RemoteException
    {
    }
    @Override public void radioStateChanged(int type, int radioState) throws android.os.RemoteException
    {
    }
    @Override public void rilConnected(int type) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.radio.modem.IRadioModemIndication
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.radio.modem.IRadioModemIndication interface,
     * generating a proxy if needed.
     */
    public static android.hardware.radio.modem.IRadioModemIndication asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.radio.modem.IRadioModemIndication))) {
        return ((android.hardware.radio.modem.IRadioModemIndication)iin);
      }
      return new android.hardware.radio.modem.IRadioModemIndication.Stub.Proxy(obj);
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
        case TRANSACTION_hardwareConfigChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.modem.HardwareConfig[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.radio.modem.HardwareConfig.CREATOR);
          data.enforceNoDataAvail();
          this.hardwareConfigChanged(_arg0, _arg1);
          break;
        }
        case TRANSACTION_modemReset:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          data.enforceNoDataAvail();
          this.modemReset(_arg0, _arg1);
          break;
        }
        case TRANSACTION_radioCapabilityIndication:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.modem.RadioCapability _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.modem.RadioCapability.CREATOR);
          data.enforceNoDataAvail();
          this.radioCapabilityIndication(_arg0, _arg1);
          break;
        }
        case TRANSACTION_radioStateChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.radioStateChanged(_arg0, _arg1);
          break;
        }
        case TRANSACTION_rilConnected:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.rilConnected(_arg0);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.radio.modem.IRadioModemIndication
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
      @Override public void hardwareConfigChanged(int type, android.hardware.radio.modem.HardwareConfig[] configs) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedArray(configs, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_hardwareConfigChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method hardwareConfigChanged is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void modemReset(int type, java.lang.String reason) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeString(reason);
          boolean _status = mRemote.transact(Stub.TRANSACTION_modemReset, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method modemReset is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void radioCapabilityIndication(int type, android.hardware.radio.modem.RadioCapability rc) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedObject(rc, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_radioCapabilityIndication, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method radioCapabilityIndication is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void radioStateChanged(int type, int radioState) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeInt(radioState);
          boolean _status = mRemote.transact(Stub.TRANSACTION_radioStateChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method radioStateChanged is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void rilConnected(int type) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          boolean _status = mRemote.transact(Stub.TRANSACTION_rilConnected, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method rilConnected is unimplemented.");
          }
        }
        finally {
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
    static final int TRANSACTION_hardwareConfigChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_modemReset = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_radioCapabilityIndication = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_radioStateChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_rilConnected = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$radio$modem$IRadioModemIndication".replace('$', '.');
  public void hardwareConfigChanged(int type, android.hardware.radio.modem.HardwareConfig[] configs) throws android.os.RemoteException;
  public void modemReset(int type, java.lang.String reason) throws android.os.RemoteException;
  public void radioCapabilityIndication(int type, android.hardware.radio.modem.RadioCapability rc) throws android.os.RemoteException;
  public void radioStateChanged(int type, int radioState) throws android.os.RemoteException;
  public void rilConnected(int type) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
