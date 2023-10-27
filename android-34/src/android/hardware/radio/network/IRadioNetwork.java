/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public interface IRadioNetwork extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "1b6608f238bd0b1c642df315621a7b605eafc883";
  /** Default implementation for IRadioNetwork. */
  public static class Default implements android.hardware.radio.network.IRadioNetwork
  {
    @Override public void getAllowedNetworkTypesBitmap(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getAvailableBandModes(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getAvailableNetworks(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getBarringInfo(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getCdmaRoamingPreference(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getCellInfoList(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getDataRegistrationState(int serial) throws android.os.RemoteException
    {
    }
    /** @deprecated Deprecated starting from Android U. */
    @Override public void getImsRegistrationState(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getNetworkSelectionMode(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getOperator(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getSignalStrength(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getSystemSelectionChannels(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getVoiceRadioTechnology(int serial) throws android.os.RemoteException
    {
    }
    @Override public void getVoiceRegistrationState(int serial) throws android.os.RemoteException
    {
    }
    @Override public void isNrDualConnectivityEnabled(int serial) throws android.os.RemoteException
    {
    }
    @Override public void responseAcknowledgement() throws android.os.RemoteException
    {
    }
    @Override public void setAllowedNetworkTypesBitmap(int serial, int networkTypeBitmap) throws android.os.RemoteException
    {
    }
    @Override public void setBandMode(int serial, int mode) throws android.os.RemoteException
    {
    }
    @Override public void setBarringPassword(int serial, java.lang.String facility, java.lang.String oldPassword, java.lang.String newPassword) throws android.os.RemoteException
    {
    }
    @Override public void setCdmaRoamingPreference(int serial, int type) throws android.os.RemoteException
    {
    }
    @Override public void setCellInfoListRate(int serial, int rate) throws android.os.RemoteException
    {
    }
    @Override public void setIndicationFilter(int serial, int indicationFilter) throws android.os.RemoteException
    {
    }
    @Override public void setLinkCapacityReportingCriteria(int serial, int hysteresisMs, int hysteresisDlKbps, int hysteresisUlKbps, int[] thresholdsDownlinkKbps, int[] thresholdsUplinkKbps, int accessNetwork) throws android.os.RemoteException
    {
    }
    @Override public void setLocationUpdates(int serial, boolean enable) throws android.os.RemoteException
    {
    }
    @Override public void setNetworkSelectionModeAutomatic(int serial) throws android.os.RemoteException
    {
    }
    @Override public void setNetworkSelectionModeManual(int serial, java.lang.String operatorNumeric, int ran) throws android.os.RemoteException
    {
    }
    @Override public void setNrDualConnectivityState(int serial, byte nrDualConnectivityState) throws android.os.RemoteException
    {
    }
    @Override public void setResponseFunctions(android.hardware.radio.network.IRadioNetworkResponse radioNetworkResponse, android.hardware.radio.network.IRadioNetworkIndication radioNetworkIndication) throws android.os.RemoteException
    {
    }
    @Override public void setSignalStrengthReportingCriteria(int serial, android.hardware.radio.network.SignalThresholdInfo[] signalThresholdInfos) throws android.os.RemoteException
    {
    }
    @Override public void setSuppServiceNotifications(int serial, boolean enable) throws android.os.RemoteException
    {
    }
    @Override public void setSystemSelectionChannels(int serial, boolean specifyChannels, android.hardware.radio.network.RadioAccessSpecifier[] specifiers) throws android.os.RemoteException
    {
    }
    @Override public void startNetworkScan(int serial, android.hardware.radio.network.NetworkScanRequest request) throws android.os.RemoteException
    {
    }
    @Override public void stopNetworkScan(int serial) throws android.os.RemoteException
    {
    }
    @Override public void supplyNetworkDepersonalization(int serial, java.lang.String netPin) throws android.os.RemoteException
    {
    }
    @Override public void setUsageSetting(int serial, int usageSetting) throws android.os.RemoteException
    {
    }
    @Override public void getUsageSetting(int serial) throws android.os.RemoteException
    {
    }
    @Override public void setEmergencyMode(int serial, int emcModeType) throws android.os.RemoteException
    {
    }
    @Override public void triggerEmergencyNetworkScan(int serial, android.hardware.radio.network.EmergencyNetworkScanTrigger request) throws android.os.RemoteException
    {
    }
    @Override public void cancelEmergencyNetworkScan(int serial, boolean resetScan) throws android.os.RemoteException
    {
    }
    @Override public void exitEmergencyMode(int serial) throws android.os.RemoteException
    {
    }
    @Override public void setNullCipherAndIntegrityEnabled(int serial, boolean enabled) throws android.os.RemoteException
    {
    }
    @Override public void isNullCipherAndIntegrityEnabled(int serial) throws android.os.RemoteException
    {
    }
    @Override public void isN1ModeEnabled(int serial) throws android.os.RemoteException
    {
    }
    @Override public void setN1ModeEnabled(int serial, boolean enable) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.radio.network.IRadioNetwork
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.radio.network.IRadioNetwork interface,
     * generating a proxy if needed.
     */
    public static android.hardware.radio.network.IRadioNetwork asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.radio.network.IRadioNetwork))) {
        return ((android.hardware.radio.network.IRadioNetwork)iin);
      }
      return new android.hardware.radio.network.IRadioNetwork.Stub.Proxy(obj);
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
        case TRANSACTION_getAllowedNetworkTypesBitmap:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getAllowedNetworkTypesBitmap(_arg0);
          break;
        }
        case TRANSACTION_getAvailableBandModes:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getAvailableBandModes(_arg0);
          break;
        }
        case TRANSACTION_getAvailableNetworks:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getAvailableNetworks(_arg0);
          break;
        }
        case TRANSACTION_getBarringInfo:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getBarringInfo(_arg0);
          break;
        }
        case TRANSACTION_getCdmaRoamingPreference:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getCdmaRoamingPreference(_arg0);
          break;
        }
        case TRANSACTION_getCellInfoList:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getCellInfoList(_arg0);
          break;
        }
        case TRANSACTION_getDataRegistrationState:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getDataRegistrationState(_arg0);
          break;
        }
        case TRANSACTION_getImsRegistrationState:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getImsRegistrationState(_arg0);
          break;
        }
        case TRANSACTION_getNetworkSelectionMode:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getNetworkSelectionMode(_arg0);
          break;
        }
        case TRANSACTION_getOperator:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getOperator(_arg0);
          break;
        }
        case TRANSACTION_getSignalStrength:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getSignalStrength(_arg0);
          break;
        }
        case TRANSACTION_getSystemSelectionChannels:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getSystemSelectionChannels(_arg0);
          break;
        }
        case TRANSACTION_getVoiceRadioTechnology:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getVoiceRadioTechnology(_arg0);
          break;
        }
        case TRANSACTION_getVoiceRegistrationState:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getVoiceRegistrationState(_arg0);
          break;
        }
        case TRANSACTION_isNrDualConnectivityEnabled:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.isNrDualConnectivityEnabled(_arg0);
          break;
        }
        case TRANSACTION_responseAcknowledgement:
        {
          this.responseAcknowledgement();
          break;
        }
        case TRANSACTION_setAllowedNetworkTypesBitmap:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.setAllowedNetworkTypesBitmap(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setBandMode:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.setBandMode(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setBarringPassword:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          java.lang.String _arg3;
          _arg3 = data.readString();
          data.enforceNoDataAvail();
          this.setBarringPassword(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_setCdmaRoamingPreference:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.setCdmaRoamingPreference(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setCellInfoListRate:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.setCellInfoListRate(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setIndicationFilter:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.setIndicationFilter(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setLinkCapacityReportingCriteria:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          int _arg3;
          _arg3 = data.readInt();
          int[] _arg4;
          _arg4 = data.createIntArray();
          int[] _arg5;
          _arg5 = data.createIntArray();
          int _arg6;
          _arg6 = data.readInt();
          data.enforceNoDataAvail();
          this.setLinkCapacityReportingCriteria(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6);
          break;
        }
        case TRANSACTION_setLocationUpdates:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setLocationUpdates(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setNetworkSelectionModeAutomatic:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.setNetworkSelectionModeAutomatic(_arg0);
          break;
        }
        case TRANSACTION_setNetworkSelectionModeManual:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          this.setNetworkSelectionModeManual(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_setNrDualConnectivityState:
        {
          int _arg0;
          _arg0 = data.readInt();
          byte _arg1;
          _arg1 = data.readByte();
          data.enforceNoDataAvail();
          this.setNrDualConnectivityState(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setResponseFunctions:
        {
          android.hardware.radio.network.IRadioNetworkResponse _arg0;
          _arg0 = android.hardware.radio.network.IRadioNetworkResponse.Stub.asInterface(data.readStrongBinder());
          android.hardware.radio.network.IRadioNetworkIndication _arg1;
          _arg1 = android.hardware.radio.network.IRadioNetworkIndication.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          this.setResponseFunctions(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setSignalStrengthReportingCriteria:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.network.SignalThresholdInfo[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.radio.network.SignalThresholdInfo.CREATOR);
          data.enforceNoDataAvail();
          this.setSignalStrengthReportingCriteria(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setSuppServiceNotifications:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setSuppServiceNotifications(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setSystemSelectionChannels:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          android.hardware.radio.network.RadioAccessSpecifier[] _arg2;
          _arg2 = data.createTypedArray(android.hardware.radio.network.RadioAccessSpecifier.CREATOR);
          data.enforceNoDataAvail();
          this.setSystemSelectionChannels(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_startNetworkScan:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.network.NetworkScanRequest _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.network.NetworkScanRequest.CREATOR);
          data.enforceNoDataAvail();
          this.startNetworkScan(_arg0, _arg1);
          break;
        }
        case TRANSACTION_stopNetworkScan:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.stopNetworkScan(_arg0);
          break;
        }
        case TRANSACTION_supplyNetworkDepersonalization:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          data.enforceNoDataAvail();
          this.supplyNetworkDepersonalization(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setUsageSetting:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.setUsageSetting(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getUsageSetting:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.getUsageSetting(_arg0);
          break;
        }
        case TRANSACTION_setEmergencyMode:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.setEmergencyMode(_arg0, _arg1);
          break;
        }
        case TRANSACTION_triggerEmergencyNetworkScan:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.radio.network.EmergencyNetworkScanTrigger _arg1;
          _arg1 = data.readTypedObject(android.hardware.radio.network.EmergencyNetworkScanTrigger.CREATOR);
          data.enforceNoDataAvail();
          this.triggerEmergencyNetworkScan(_arg0, _arg1);
          break;
        }
        case TRANSACTION_cancelEmergencyNetworkScan:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.cancelEmergencyNetworkScan(_arg0, _arg1);
          break;
        }
        case TRANSACTION_exitEmergencyMode:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.exitEmergencyMode(_arg0);
          break;
        }
        case TRANSACTION_setNullCipherAndIntegrityEnabled:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setNullCipherAndIntegrityEnabled(_arg0, _arg1);
          break;
        }
        case TRANSACTION_isNullCipherAndIntegrityEnabled:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.isNullCipherAndIntegrityEnabled(_arg0);
          break;
        }
        case TRANSACTION_isN1ModeEnabled:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.isN1ModeEnabled(_arg0);
          break;
        }
        case TRANSACTION_setN1ModeEnabled:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setN1ModeEnabled(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.radio.network.IRadioNetwork
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
      @Override public void getAllowedNetworkTypesBitmap(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getAllowedNetworkTypesBitmap, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getAllowedNetworkTypesBitmap is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getAvailableBandModes(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getAvailableBandModes, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getAvailableBandModes is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getAvailableNetworks(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getAvailableNetworks, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getAvailableNetworks is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getBarringInfo(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getBarringInfo, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getBarringInfo is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getCdmaRoamingPreference(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCdmaRoamingPreference, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getCdmaRoamingPreference is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getCellInfoList(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCellInfoList, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getCellInfoList is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getDataRegistrationState(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getDataRegistrationState, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getDataRegistrationState is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      /** @deprecated Deprecated starting from Android U. */
      @Override public void getImsRegistrationState(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getImsRegistrationState, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getImsRegistrationState is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getNetworkSelectionMode(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getNetworkSelectionMode, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getNetworkSelectionMode is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getOperator(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getOperator, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getOperator is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getSignalStrength(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSignalStrength, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getSignalStrength is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getSystemSelectionChannels(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSystemSelectionChannels, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getSystemSelectionChannels is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getVoiceRadioTechnology(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getVoiceRadioTechnology, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getVoiceRadioTechnology is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getVoiceRegistrationState(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getVoiceRegistrationState, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getVoiceRegistrationState is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isNrDualConnectivityEnabled(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isNrDualConnectivityEnabled, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method isNrDualConnectivityEnabled is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void responseAcknowledgement() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_responseAcknowledgement, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method responseAcknowledgement is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setAllowedNetworkTypesBitmap(int serial, int networkTypeBitmap) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(networkTypeBitmap);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setAllowedNetworkTypesBitmap, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setAllowedNetworkTypesBitmap is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setBandMode(int serial, int mode) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(mode);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setBandMode, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setBandMode is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setBarringPassword(int serial, java.lang.String facility, java.lang.String oldPassword, java.lang.String newPassword) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(facility);
          _data.writeString(oldPassword);
          _data.writeString(newPassword);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setBarringPassword, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setBarringPassword is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setCdmaRoamingPreference(int serial, int type) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(type);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCdmaRoamingPreference, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setCdmaRoamingPreference is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setCellInfoListRate(int serial, int rate) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(rate);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCellInfoListRate, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setCellInfoListRate is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setIndicationFilter(int serial, int indicationFilter) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(indicationFilter);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setIndicationFilter, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setIndicationFilter is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setLinkCapacityReportingCriteria(int serial, int hysteresisMs, int hysteresisDlKbps, int hysteresisUlKbps, int[] thresholdsDownlinkKbps, int[] thresholdsUplinkKbps, int accessNetwork) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(hysteresisMs);
          _data.writeInt(hysteresisDlKbps);
          _data.writeInt(hysteresisUlKbps);
          _data.writeIntArray(thresholdsDownlinkKbps);
          _data.writeIntArray(thresholdsUplinkKbps);
          _data.writeInt(accessNetwork);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setLinkCapacityReportingCriteria, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setLinkCapacityReportingCriteria is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setLocationUpdates(int serial, boolean enable) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeBoolean(enable);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setLocationUpdates, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setLocationUpdates is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setNetworkSelectionModeAutomatic(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setNetworkSelectionModeAutomatic, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setNetworkSelectionModeAutomatic is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setNetworkSelectionModeManual(int serial, java.lang.String operatorNumeric, int ran) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(operatorNumeric);
          _data.writeInt(ran);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setNetworkSelectionModeManual, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setNetworkSelectionModeManual is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setNrDualConnectivityState(int serial, byte nrDualConnectivityState) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeByte(nrDualConnectivityState);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setNrDualConnectivityState, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setNrDualConnectivityState is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setResponseFunctions(android.hardware.radio.network.IRadioNetworkResponse radioNetworkResponse, android.hardware.radio.network.IRadioNetworkIndication radioNetworkIndication) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(radioNetworkResponse);
          _data.writeStrongInterface(radioNetworkIndication);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setResponseFunctions, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setResponseFunctions is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setSignalStrengthReportingCriteria(int serial, android.hardware.radio.network.SignalThresholdInfo[] signalThresholdInfos) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedArray(signalThresholdInfos, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setSignalStrengthReportingCriteria, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setSignalStrengthReportingCriteria is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setSuppServiceNotifications(int serial, boolean enable) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeBoolean(enable);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setSuppServiceNotifications, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setSuppServiceNotifications is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setSystemSelectionChannels(int serial, boolean specifyChannels, android.hardware.radio.network.RadioAccessSpecifier[] specifiers) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeBoolean(specifyChannels);
          _data.writeTypedArray(specifiers, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setSystemSelectionChannels, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setSystemSelectionChannels is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void startNetworkScan(int serial, android.hardware.radio.network.NetworkScanRequest request) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedObject(request, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startNetworkScan, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method startNetworkScan is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void stopNetworkScan(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stopNetworkScan, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method stopNetworkScan is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void supplyNetworkDepersonalization(int serial, java.lang.String netPin) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeString(netPin);
          boolean _status = mRemote.transact(Stub.TRANSACTION_supplyNetworkDepersonalization, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method supplyNetworkDepersonalization is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setUsageSetting(int serial, int usageSetting) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(usageSetting);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setUsageSetting, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setUsageSetting is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getUsageSetting(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getUsageSetting, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method getUsageSetting is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setEmergencyMode(int serial, int emcModeType) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeInt(emcModeType);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setEmergencyMode, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setEmergencyMode is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void triggerEmergencyNetworkScan(int serial, android.hardware.radio.network.EmergencyNetworkScanTrigger request) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeTypedObject(request, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_triggerEmergencyNetworkScan, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method triggerEmergencyNetworkScan is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void cancelEmergencyNetworkScan(int serial, boolean resetScan) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeBoolean(resetScan);
          boolean _status = mRemote.transact(Stub.TRANSACTION_cancelEmergencyNetworkScan, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method cancelEmergencyNetworkScan is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void exitEmergencyMode(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_exitEmergencyMode, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method exitEmergencyMode is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setNullCipherAndIntegrityEnabled(int serial, boolean enabled) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeBoolean(enabled);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setNullCipherAndIntegrityEnabled, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setNullCipherAndIntegrityEnabled is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isNullCipherAndIntegrityEnabled(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isNullCipherAndIntegrityEnabled, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method isNullCipherAndIntegrityEnabled is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isN1ModeEnabled(int serial) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isN1ModeEnabled, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method isN1ModeEnabled is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setN1ModeEnabled(int serial, boolean enable) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(serial);
          _data.writeBoolean(enable);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setN1ModeEnabled, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method setN1ModeEnabled is unimplemented.");
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
    static final int TRANSACTION_getAllowedNetworkTypesBitmap = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_getAvailableBandModes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_getAvailableNetworks = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_getBarringInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getCdmaRoamingPreference = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getCellInfoList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getDataRegistrationState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getImsRegistrationState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_getNetworkSelectionMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_getOperator = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_getSignalStrength = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_getSystemSelectionChannels = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_getVoiceRadioTechnology = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_getVoiceRegistrationState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_isNrDualConnectivityEnabled = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_responseAcknowledgement = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_setAllowedNetworkTypesBitmap = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_setBandMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_setBarringPassword = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_setCdmaRoamingPreference = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
    static final int TRANSACTION_setCellInfoListRate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
    static final int TRANSACTION_setIndicationFilter = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
    static final int TRANSACTION_setLinkCapacityReportingCriteria = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
    static final int TRANSACTION_setLocationUpdates = (android.os.IBinder.FIRST_CALL_TRANSACTION + 23);
    static final int TRANSACTION_setNetworkSelectionModeAutomatic = (android.os.IBinder.FIRST_CALL_TRANSACTION + 24);
    static final int TRANSACTION_setNetworkSelectionModeManual = (android.os.IBinder.FIRST_CALL_TRANSACTION + 25);
    static final int TRANSACTION_setNrDualConnectivityState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 26);
    static final int TRANSACTION_setResponseFunctions = (android.os.IBinder.FIRST_CALL_TRANSACTION + 27);
    static final int TRANSACTION_setSignalStrengthReportingCriteria = (android.os.IBinder.FIRST_CALL_TRANSACTION + 28);
    static final int TRANSACTION_setSuppServiceNotifications = (android.os.IBinder.FIRST_CALL_TRANSACTION + 29);
    static final int TRANSACTION_setSystemSelectionChannels = (android.os.IBinder.FIRST_CALL_TRANSACTION + 30);
    static final int TRANSACTION_startNetworkScan = (android.os.IBinder.FIRST_CALL_TRANSACTION + 31);
    static final int TRANSACTION_stopNetworkScan = (android.os.IBinder.FIRST_CALL_TRANSACTION + 32);
    static final int TRANSACTION_supplyNetworkDepersonalization = (android.os.IBinder.FIRST_CALL_TRANSACTION + 33);
    static final int TRANSACTION_setUsageSetting = (android.os.IBinder.FIRST_CALL_TRANSACTION + 34);
    static final int TRANSACTION_getUsageSetting = (android.os.IBinder.FIRST_CALL_TRANSACTION + 35);
    static final int TRANSACTION_setEmergencyMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 36);
    static final int TRANSACTION_triggerEmergencyNetworkScan = (android.os.IBinder.FIRST_CALL_TRANSACTION + 37);
    static final int TRANSACTION_cancelEmergencyNetworkScan = (android.os.IBinder.FIRST_CALL_TRANSACTION + 38);
    static final int TRANSACTION_exitEmergencyMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 39);
    static final int TRANSACTION_setNullCipherAndIntegrityEnabled = (android.os.IBinder.FIRST_CALL_TRANSACTION + 40);
    static final int TRANSACTION_isNullCipherAndIntegrityEnabled = (android.os.IBinder.FIRST_CALL_TRANSACTION + 41);
    static final int TRANSACTION_isN1ModeEnabled = (android.os.IBinder.FIRST_CALL_TRANSACTION + 42);
    static final int TRANSACTION_setN1ModeEnabled = (android.os.IBinder.FIRST_CALL_TRANSACTION + 43);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$radio$network$IRadioNetwork".replace('$', '.');
  public void getAllowedNetworkTypesBitmap(int serial) throws android.os.RemoteException;
  public void getAvailableBandModes(int serial) throws android.os.RemoteException;
  public void getAvailableNetworks(int serial) throws android.os.RemoteException;
  public void getBarringInfo(int serial) throws android.os.RemoteException;
  public void getCdmaRoamingPreference(int serial) throws android.os.RemoteException;
  public void getCellInfoList(int serial) throws android.os.RemoteException;
  public void getDataRegistrationState(int serial) throws android.os.RemoteException;
  /** @deprecated Deprecated starting from Android U. */
  @Deprecated
  public void getImsRegistrationState(int serial) throws android.os.RemoteException;
  public void getNetworkSelectionMode(int serial) throws android.os.RemoteException;
  public void getOperator(int serial) throws android.os.RemoteException;
  public void getSignalStrength(int serial) throws android.os.RemoteException;
  public void getSystemSelectionChannels(int serial) throws android.os.RemoteException;
  public void getVoiceRadioTechnology(int serial) throws android.os.RemoteException;
  public void getVoiceRegistrationState(int serial) throws android.os.RemoteException;
  public void isNrDualConnectivityEnabled(int serial) throws android.os.RemoteException;
  public void responseAcknowledgement() throws android.os.RemoteException;
  public void setAllowedNetworkTypesBitmap(int serial, int networkTypeBitmap) throws android.os.RemoteException;
  public void setBandMode(int serial, int mode) throws android.os.RemoteException;
  public void setBarringPassword(int serial, java.lang.String facility, java.lang.String oldPassword, java.lang.String newPassword) throws android.os.RemoteException;
  public void setCdmaRoamingPreference(int serial, int type) throws android.os.RemoteException;
  public void setCellInfoListRate(int serial, int rate) throws android.os.RemoteException;
  public void setIndicationFilter(int serial, int indicationFilter) throws android.os.RemoteException;
  public void setLinkCapacityReportingCriteria(int serial, int hysteresisMs, int hysteresisDlKbps, int hysteresisUlKbps, int[] thresholdsDownlinkKbps, int[] thresholdsUplinkKbps, int accessNetwork) throws android.os.RemoteException;
  public void setLocationUpdates(int serial, boolean enable) throws android.os.RemoteException;
  public void setNetworkSelectionModeAutomatic(int serial) throws android.os.RemoteException;
  public void setNetworkSelectionModeManual(int serial, java.lang.String operatorNumeric, int ran) throws android.os.RemoteException;
  public void setNrDualConnectivityState(int serial, byte nrDualConnectivityState) throws android.os.RemoteException;
  public void setResponseFunctions(android.hardware.radio.network.IRadioNetworkResponse radioNetworkResponse, android.hardware.radio.network.IRadioNetworkIndication radioNetworkIndication) throws android.os.RemoteException;
  public void setSignalStrengthReportingCriteria(int serial, android.hardware.radio.network.SignalThresholdInfo[] signalThresholdInfos) throws android.os.RemoteException;
  public void setSuppServiceNotifications(int serial, boolean enable) throws android.os.RemoteException;
  public void setSystemSelectionChannels(int serial, boolean specifyChannels, android.hardware.radio.network.RadioAccessSpecifier[] specifiers) throws android.os.RemoteException;
  public void startNetworkScan(int serial, android.hardware.radio.network.NetworkScanRequest request) throws android.os.RemoteException;
  public void stopNetworkScan(int serial) throws android.os.RemoteException;
  public void supplyNetworkDepersonalization(int serial, java.lang.String netPin) throws android.os.RemoteException;
  public void setUsageSetting(int serial, int usageSetting) throws android.os.RemoteException;
  public void getUsageSetting(int serial) throws android.os.RemoteException;
  public void setEmergencyMode(int serial, int emcModeType) throws android.os.RemoteException;
  public void triggerEmergencyNetworkScan(int serial, android.hardware.radio.network.EmergencyNetworkScanTrigger request) throws android.os.RemoteException;
  public void cancelEmergencyNetworkScan(int serial, boolean resetScan) throws android.os.RemoteException;
  public void exitEmergencyMode(int serial) throws android.os.RemoteException;
  public void setNullCipherAndIntegrityEnabled(int serial, boolean enabled) throws android.os.RemoteException;
  public void isNullCipherAndIntegrityEnabled(int serial) throws android.os.RemoteException;
  public void isN1ModeEnabled(int serial) throws android.os.RemoteException;
  public void setN1ModeEnabled(int serial, boolean enable) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
