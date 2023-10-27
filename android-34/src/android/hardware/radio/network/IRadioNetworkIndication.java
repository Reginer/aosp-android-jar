/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public interface IRadioNetworkIndication extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "1b6608f238bd0b1c642df315621a7b605eafc883";
  /** Default implementation for IRadioNetworkIndication. */
  public static class Default implements android.hardware.radio.network.IRadioNetworkIndication
  {
    @Override public void barringInfoChanged(int type, android.hardware.radio.network.CellIdentity cellIdentity, android.hardware.radio.network.BarringInfo[] barringInfos) throws android.os.RemoteException
    {
    }
    @Override public void cdmaPrlChanged(int type, int version) throws android.os.RemoteException
    {
    }
    @Override public void cellInfoList(int type, android.hardware.radio.network.CellInfo[] records) throws android.os.RemoteException
    {
    }
    @Override public void currentLinkCapacityEstimate(int type, android.hardware.radio.network.LinkCapacityEstimate lce) throws android.os.RemoteException
    {
    }
    @Override public void currentPhysicalChannelConfigs(int type, android.hardware.radio.network.PhysicalChannelConfig[] configs) throws android.os.RemoteException
    {
    }
    @Override public void currentSignalStrength(int type, android.hardware.radio.network.SignalStrength signalStrength) throws android.os.RemoteException
    {
    }
    @Override public void imsNetworkStateChanged(int type) throws android.os.RemoteException
    {
    }
    @Override public void networkScanResult(int type, android.hardware.radio.network.NetworkScanResult result) throws android.os.RemoteException
    {
    }
    @Override public void networkStateChanged(int type) throws android.os.RemoteException
    {
    }
    @Override public void nitzTimeReceived(int type, java.lang.String nitzTime, long receivedTimeMs, long ageMs) throws android.os.RemoteException
    {
    }
    @Override public void registrationFailed(int type, android.hardware.radio.network.CellIdentity cellIdentity, java.lang.String chosenPlmn, int domain, int causeCode, int additionalCauseCode) throws android.os.RemoteException
    {
    }
    @Override public void restrictedStateChanged(int type, int state) throws android.os.RemoteException
    {
    }
    @Override public void suppSvcNotify(int type, android.hardware.radio.network.SuppSvcNotification suppSvc) throws android.os.RemoteException
    {
    }
    @Override public void voiceRadioTechChanged(int type, int rat) throws android.os.RemoteException
    {
    }
    @Override public void emergencyNetworkScanResult(int type, android.hardware.radio.network.EmergencyRegResult result) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.radio.network.IRadioNetworkIndication
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.radio.network.IRadioNetworkIndication interface,
     * generating a proxy if needed.
     */
    public static android.hardware.radio.network.IRadioNetworkIndication asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.radio.network.IRadioNetworkIndication))) {
        return ((android.hardware.radio.network.IRadioNetworkIndication)iin);
      }
      return new android.hardware.radio.network.IRadioNetworkIndication.Stub.Proxy(obj);
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
        case TRANSACTION_barringInfoChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.network.CellIdentity _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.network.CellIdentity.CREATOR);
          android.hardware.radio.network.BarringInfo[] _arg2;
          _arg2 = data.createTypedArray(android.hardware.radio.network.BarringInfo.CREATOR);
          data.enforceNoDataAvail();
          this.barringInfoChanged(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_cdmaPrlChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.cdmaPrlChanged(_arg0, _arg1);
          break;
        }
        case TRANSACTION_cellInfoList:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.network.CellInfo[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.radio.network.CellInfo.CREATOR);
          data.enforceNoDataAvail();
          this.cellInfoList(_arg0, _arg1);
          break;
        }
        case TRANSACTION_currentLinkCapacityEstimate:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.network.LinkCapacityEstimate _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.network.LinkCapacityEstimate.CREATOR);
          data.enforceNoDataAvail();
          this.currentLinkCapacityEstimate(_arg0, _arg1);
          break;
        }
        case TRANSACTION_currentPhysicalChannelConfigs:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.network.PhysicalChannelConfig[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.radio.network.PhysicalChannelConfig.CREATOR);
          data.enforceNoDataAvail();
          this.currentPhysicalChannelConfigs(_arg0, _arg1);
          break;
        }
        case TRANSACTION_currentSignalStrength:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.network.SignalStrength _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.network.SignalStrength.CREATOR);
          data.enforceNoDataAvail();
          this.currentSignalStrength(_arg0, _arg1);
          break;
        }
        case TRANSACTION_imsNetworkStateChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.imsNetworkStateChanged(_arg0);
          break;
        }
        case TRANSACTION_networkScanResult:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.network.NetworkScanResult _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.network.NetworkScanResult.CREATOR);
          data.enforceNoDataAvail();
          this.networkScanResult(_arg0, _arg1);
          break;
        }
        case TRANSACTION_networkStateChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.networkStateChanged(_arg0);
          break;
        }
        case TRANSACTION_nitzTimeReceived:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          long _arg2;
          _arg2 = data.readLong();
          long _arg3;
          _arg3 = data.readLong();
          data.enforceNoDataAvail();
          this.nitzTimeReceived(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_registrationFailed:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.network.CellIdentity _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.network.CellIdentity.CREATOR);
          java.lang.String _arg2;
          _arg2 = data.readString();
          int _arg3;
          _arg3 = data.readInt();
          int _arg4;
          _arg4 = data.readInt();
          int _arg5;
          _arg5 = data.readInt();
          data.enforceNoDataAvail();
          this.registrationFailed(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
          break;
        }
        case TRANSACTION_restrictedStateChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.restrictedStateChanged(_arg0, _arg1);
          break;
        }
        case TRANSACTION_suppSvcNotify:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.network.SuppSvcNotification _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.network.SuppSvcNotification.CREATOR);
          data.enforceNoDataAvail();
          this.suppSvcNotify(_arg0, _arg1);
          break;
        }
        case TRANSACTION_voiceRadioTechChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.voiceRadioTechChanged(_arg0, _arg1);
          break;
        }
        case TRANSACTION_emergencyNetworkScanResult:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.network.EmergencyRegResult _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.network.EmergencyRegResult.CREATOR);
          data.enforceNoDataAvail();
          this.emergencyNetworkScanResult(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.radio.network.IRadioNetworkIndication
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
      @Override public void barringInfoChanged(int type, android.hardware.radio.network.CellIdentity cellIdentity, android.hardware.radio.network.BarringInfo[] barringInfos) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedObject(cellIdentity, 0);
          _data.writeTypedArray(barringInfos, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_barringInfoChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method barringInfoChanged is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void cdmaPrlChanged(int type, int version) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeInt(version);
          boolean _status = mRemote.transact(Stub.TRANSACTION_cdmaPrlChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method cdmaPrlChanged is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void cellInfoList(int type, android.hardware.radio.network.CellInfo[] records) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedArray(records, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_cellInfoList, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method cellInfoList is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void currentLinkCapacityEstimate(int type, android.hardware.radio.network.LinkCapacityEstimate lce) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedObject(lce, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_currentLinkCapacityEstimate, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method currentLinkCapacityEstimate is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void currentPhysicalChannelConfigs(int type, android.hardware.radio.network.PhysicalChannelConfig[] configs) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedArray(configs, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_currentPhysicalChannelConfigs, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method currentPhysicalChannelConfigs is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void currentSignalStrength(int type, android.hardware.radio.network.SignalStrength signalStrength) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedObject(signalStrength, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_currentSignalStrength, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method currentSignalStrength is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void imsNetworkStateChanged(int type) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          boolean _status = mRemote.transact(Stub.TRANSACTION_imsNetworkStateChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method imsNetworkStateChanged is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void networkScanResult(int type, android.hardware.radio.network.NetworkScanResult result) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedObject(result, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_networkScanResult, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method networkScanResult is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void networkStateChanged(int type) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          boolean _status = mRemote.transact(Stub.TRANSACTION_networkStateChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method networkStateChanged is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void nitzTimeReceived(int type, java.lang.String nitzTime, long receivedTimeMs, long ageMs) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeString(nitzTime);
          _data.writeLong(receivedTimeMs);
          _data.writeLong(ageMs);
          boolean _status = mRemote.transact(Stub.TRANSACTION_nitzTimeReceived, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method nitzTimeReceived is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void registrationFailed(int type, android.hardware.radio.network.CellIdentity cellIdentity, java.lang.String chosenPlmn, int domain, int causeCode, int additionalCauseCode) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedObject(cellIdentity, 0);
          _data.writeString(chosenPlmn);
          _data.writeInt(domain);
          _data.writeInt(causeCode);
          _data.writeInt(additionalCauseCode);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registrationFailed, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method registrationFailed is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void restrictedStateChanged(int type, int state) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeInt(state);
          boolean _status = mRemote.transact(Stub.TRANSACTION_restrictedStateChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method restrictedStateChanged is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void suppSvcNotify(int type, android.hardware.radio.network.SuppSvcNotification suppSvc) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedObject(suppSvc, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_suppSvcNotify, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method suppSvcNotify is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void voiceRadioTechChanged(int type, int rat) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeInt(rat);
          boolean _status = mRemote.transact(Stub.TRANSACTION_voiceRadioTechChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method voiceRadioTechChanged is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void emergencyNetworkScanResult(int type, android.hardware.radio.network.EmergencyRegResult result) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeTypedObject(result, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_emergencyNetworkScanResult, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method emergencyNetworkScanResult is unimplemented.");
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
    static final int TRANSACTION_barringInfoChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_cdmaPrlChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_cellInfoList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_currentLinkCapacityEstimate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_currentPhysicalChannelConfigs = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_currentSignalStrength = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_imsNetworkStateChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_networkScanResult = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_networkStateChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_nitzTimeReceived = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_registrationFailed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_restrictedStateChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_suppSvcNotify = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_voiceRadioTechChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_emergencyNetworkScanResult = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$radio$network$IRadioNetworkIndication".replace('$', '.');
  public void barringInfoChanged(int type, android.hardware.radio.network.CellIdentity cellIdentity, android.hardware.radio.network.BarringInfo[] barringInfos) throws android.os.RemoteException;
  public void cdmaPrlChanged(int type, int version) throws android.os.RemoteException;
  public void cellInfoList(int type, android.hardware.radio.network.CellInfo[] records) throws android.os.RemoteException;
  public void currentLinkCapacityEstimate(int type, android.hardware.radio.network.LinkCapacityEstimate lce) throws android.os.RemoteException;
  public void currentPhysicalChannelConfigs(int type, android.hardware.radio.network.PhysicalChannelConfig[] configs) throws android.os.RemoteException;
  public void currentSignalStrength(int type, android.hardware.radio.network.SignalStrength signalStrength) throws android.os.RemoteException;
  public void imsNetworkStateChanged(int type) throws android.os.RemoteException;
  public void networkScanResult(int type, android.hardware.radio.network.NetworkScanResult result) throws android.os.RemoteException;
  public void networkStateChanged(int type) throws android.os.RemoteException;
  public void nitzTimeReceived(int type, java.lang.String nitzTime, long receivedTimeMs, long ageMs) throws android.os.RemoteException;
  public void registrationFailed(int type, android.hardware.radio.network.CellIdentity cellIdentity, java.lang.String chosenPlmn, int domain, int causeCode, int additionalCauseCode) throws android.os.RemoteException;
  public void restrictedStateChanged(int type, int state) throws android.os.RemoteException;
  public void suppSvcNotify(int type, android.hardware.radio.network.SuppSvcNotification suppSvc) throws android.os.RemoteException;
  public void voiceRadioTechChanged(int type, int rat) throws android.os.RemoteException;
  public void emergencyNetworkScanResult(int type, android.hardware.radio.network.EmergencyRegResult result) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
