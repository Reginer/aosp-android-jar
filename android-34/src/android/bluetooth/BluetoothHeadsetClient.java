/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.bluetooth;

import static android.bluetooth.BluetoothUtils.getSyncTimeout;

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.bluetooth.annotations.RequiresLegacyBluetoothPermission;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.AttributionSource;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.CloseGuard;
import android.util.Log;

import com.android.modules.utils.SynchronousResultReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * This class provides the System APIs to interact with the Hands-Free Client profile.
 *
 * <p>BluetoothHeadsetClient is a proxy object for controlling the Bluetooth HFP Client
 * Service via IPC. Use {@link BluetoothAdapter#getProfileProxy} to get
 * the BluetoothHeadsetClient proxy object.
 *
 * @hide
 */
@SystemApi
public final class BluetoothHeadsetClient implements BluetoothProfile, AutoCloseable {
    private static final String TAG = "BluetoothHeadsetClient";
    private static final boolean DBG = true;
    private static final boolean VDBG = false;
    private final CloseGuard mCloseGuard;

    /**
     * Intent used to broadcast the change in connection state of the HFP Client profile.
     *
     * <p>This intent will have 3 extras:
     * <ul>
     * <li> {@link #EXTRA_STATE} - The current state of the profile. </li>
     * <li> {@link #EXTRA_PREVIOUS_STATE}- The previous state of the profile.</li>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device. </li>
     * </ul>
     *
     * <p>{@link #EXTRA_STATE} or {@link #EXTRA_PREVIOUS_STATE} can be any of
     * {@link #STATE_DISCONNECTED}, {@link #STATE_CONNECTING},
     * {@link #STATE_CONNECTED}, {@link #STATE_DISCONNECTING}.
     *
     * @hide
     */
    @SuppressLint("ActionValue")
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CONNECTION_STATE_CHANGED =
            "android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED";

    /**
     * Intent sent whenever audio state changes.
     *
     * <p>It includes two mandatory extras:
     * {@link BluetoothProfile#EXTRA_STATE},
     * {@link BluetoothProfile#EXTRA_PREVIOUS_STATE},
     * with possible values:
     * {@link #STATE_AUDIO_CONNECTING},
     * {@link #STATE_AUDIO_CONNECTED},
     * {@link #STATE_AUDIO_DISCONNECTED}</p>
     * <p>When <code>EXTRA_STATE</code> is set
     * to </code>STATE_AUDIO_CONNECTED</code>,
     * it also includes {@link #EXTRA_AUDIO_WBS}
     * indicating wide band speech support.</p>
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @SuppressLint("ActionValue")
    public static final String ACTION_AUDIO_STATE_CHANGED =
            "android.bluetooth.headsetclient.profile.action.AUDIO_STATE_CHANGED";

    /**
     * Intent sending updates of the Audio Gateway state.
     * Each extra is being sent only when value it
     * represents has been changed recently on AG.
     * <p>It can contain one or more of the following extras:
     * {@link #EXTRA_NETWORK_STATUS},
     * {@link #EXTRA_NETWORK_SIGNAL_STRENGTH},
     * {@link #EXTRA_NETWORK_ROAMING},
     * {@link #EXTRA_BATTERY_LEVEL},
     * {@link #EXTRA_OPERATOR_NAME},
     * {@link #EXTRA_VOICE_RECOGNITION},
     * {@link #EXTRA_IN_BAND_RING}</p>
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_AG_EVENT =
            "android.bluetooth.headsetclient.profile.action.AG_EVENT";

    /**
     * Intent sent whenever state of a call changes.
     *
     * <p>It includes:
     * {@link #EXTRA_CALL},
     * with value of {@link BluetoothHeadsetClientCall} instance,
     * representing actual call state.</p>
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CALL_CHANGED =
            "android.bluetooth.headsetclient.profile.action.AG_CALL_CHANGED";

    /**
     * Intent that notifies about the result of the last issued action.
     * Please note that not every action results in explicit action result code being sent.
     * Instead other notifications about new Audio Gateway state might be sent,
     * like <code>ACTION_AG_EVENT</code> with <code>EXTRA_VOICE_RECOGNITION</code> value
     * when for example user started voice recognition from HF unit.
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_RESULT =
            "android.bluetooth.headsetclient.profile.action.RESULT";

    /**
     * Intent that notifies about vendor specific event arrival. Events not defined in
     * HFP spec will be matched with supported vendor event list and this intent will
     * be broadcasted upon a match. Supported vendor events are of format of
     * of "+eventCode" or "+eventCode=xxxx" or "+eventCode:=xxxx".
     * Vendor event can be a response to an vendor specific command or unsolicited.
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_VENDOR_SPECIFIC_HEADSETCLIENT_EVENT =
            "android.bluetooth.headsetclient.profile.action.VENDOR_SPECIFIC_EVENT";

    /**
     * Intent that notifies about the number attached to the last voice tag
     * recorded on AG.
     *
     * <p>It contains:
     * {@link #EXTRA_NUMBER},
     * with a <code>String</code> value representing phone number.</p>
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_LAST_VTAG =
            "android.bluetooth.headsetclient.profile.action.LAST_VTAG";

    /**
     * @hide
     */
    public static final int STATE_AUDIO_DISCONNECTED = 0;

    /**
     * @hide
     */
    public static final int STATE_AUDIO_CONNECTING = 1;

    /**
     * @hide
     */
    public static final int STATE_AUDIO_CONNECTED = 2;

    /**
     * Extra with information if connected audio is WBS.
     * <p>Possible values: <code>true</code>,
     * <code>false</code>.</p>
     *
     * @hide
     */
    public static final String EXTRA_AUDIO_WBS =
            "android.bluetooth.headsetclient.extra.AUDIO_WBS";

    /**
     * Extra for AG_EVENT indicates network status.
     * <p>Value: 0 - network unavailable,
     * 1 - network available </p>
     *
     * @hide
     */
    public static final String EXTRA_NETWORK_STATUS =
            "android.bluetooth.headsetclient.extra.NETWORK_STATUS";

    /**
     * Extra for AG_EVENT intent indicates network signal strength.
     * <p>Value: <code>Integer</code> representing signal strength.</p>
     *
     * @hide
     */
    public static final String EXTRA_NETWORK_SIGNAL_STRENGTH =
            "android.bluetooth.headsetclient.extra.NETWORK_SIGNAL_STRENGTH";

    /**
     * Extra for AG_EVENT intent indicates roaming state.
     * <p>Value: 0 - no roaming
     * 1 - active roaming</p>
     *
     * @hide
     */
    public static final String EXTRA_NETWORK_ROAMING =
            "android.bluetooth.headsetclient.extra.NETWORK_ROAMING";

    /**
     * Extra for AG_EVENT intent indicates the battery level.
     * <p>Value: <code>Integer</code> representing signal strength.</p>
     *
     * @hide
     */
    public static final String EXTRA_BATTERY_LEVEL =
            "android.bluetooth.headsetclient.extra.BATTERY_LEVEL";

    /**
     * Extra for AG_EVENT intent indicates operator name.
     * <p>Value: <code>String</code> representing operator name.</p>
     *
     * @hide
     */
    public static final String EXTRA_OPERATOR_NAME =
            "android.bluetooth.headsetclient.extra.OPERATOR_NAME";

    /**
     * Extra for AG_EVENT intent indicates voice recognition state.
     * <p>Value:
     * 0 - voice recognition stopped,
     * 1 - voice recognition started.</p>
     *
     * @hide
     */
    public static final String EXTRA_VOICE_RECOGNITION =
            "android.bluetooth.headsetclient.extra.VOICE_RECOGNITION";

    /**
     * Extra for AG_EVENT intent indicates in band ring state.
     * <p>Value:
     * 0 - in band ring tone not supported, or
     * 1 - in band ring tone supported.</p>
     *
     * @hide
     */
    public static final String EXTRA_IN_BAND_RING =
            "android.bluetooth.headsetclient.extra.IN_BAND_RING";

    /**
     * Extra for AG_EVENT intent indicates subscriber info.
     * <p>Value: <code>String</code> containing subscriber information.</p>
     *
     * @hide
     */
    public static final String EXTRA_SUBSCRIBER_INFO =
            "android.bluetooth.headsetclient.extra.SUBSCRIBER_INFO";

    /**
     * Extra for AG_CALL_CHANGED intent indicates the
     * {@link BluetoothHeadsetClientCall} object that has changed.
     *
     * @hide
     */
    public static final String EXTRA_CALL =
            "android.bluetooth.headsetclient.extra.CALL";

    /**
     * Extra for ACTION_LAST_VTAG intent.
     * <p>Value: <code>String</code> representing phone number
     * corresponding to last voice tag recorded on AG</p>
     *
     * @hide
     */
    public static final String EXTRA_NUMBER =
            "android.bluetooth.headsetclient.extra.NUMBER";

    /**
     * Extra for ACTION_RESULT intent that shows the result code of
     * last issued action.
     * <p>Possible results:
     * {@link #ACTION_RESULT_OK},
     * {@link #ACTION_RESULT_ERROR},
     * {@link #ACTION_RESULT_ERROR_NO_CARRIER},
     * {@link #ACTION_RESULT_ERROR_BUSY},
     * {@link #ACTION_RESULT_ERROR_NO_ANSWER},
     * {@link #ACTION_RESULT_ERROR_DELAYED},
     * {@link #ACTION_RESULT_ERROR_BLACKLISTED},
     * {@link #ACTION_RESULT_ERROR_CME}</p>
     *
     * @hide
     */
    public static final String EXTRA_RESULT_CODE =
            "android.bluetooth.headsetclient.extra.RESULT_CODE";

    /**
     * Extra for ACTION_RESULT intent that shows the extended result code of
     * last issued action.
     * <p>Value: <code>Integer</code> - error code.</p>
     *
     * @hide
     */
    public static final String EXTRA_CME_CODE =
            "android.bluetooth.headsetclient.extra.CME_CODE";

    /**
     * Extra for VENDOR_SPECIFIC_HEADSETCLIENT_EVENT intent that
     * indicates vendor ID.
     *
     * @hide
     */
    public static final String EXTRA_VENDOR_ID =
            "android.bluetooth.headsetclient.extra.VENDOR_ID";

    /**
     * Extra for VENDOR_SPECIFIC_HEADSETCLIENT_EVENT intent that
     * indicates vendor event code.
     *
     * @hide
     */
    public static final String EXTRA_VENDOR_EVENT_CODE =
            "android.bluetooth.headsetclient.extra.VENDOR_EVENT_CODE";

    /**
     * Extra for VENDOR_SPECIFIC_HEADSETCLIENT_EVENT intent that
     * contains full vendor event including event code and full arguments.
     *
     * @hide
     */
    public static final String EXTRA_VENDOR_EVENT_FULL_ARGS =
            "android.bluetooth.headsetclient.extra.VENDOR_EVENT_FULL_ARGS";

    /* Extras for AG_FEATURES, extras type is boolean */
    // TODO verify if all of those are actually useful
    /**
     * AG feature: three way calling.
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_3WAY_CALLING =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_3WAY_CALLING";

    /**
     * AG feature: voice recognition.
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_VOICE_RECOGNITION =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_VOICE_RECOGNITION";

    /**
     * AG feature: fetching phone number for voice tagging procedure.
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_ATTACH_NUMBER_TO_VT =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_ATTACH_NUMBER_TO_VT";

    /**
     * AG feature: ability to reject incoming call.
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_REJECT_CALL =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_REJECT_CALL";

    /**
     * AG feature: enhanced call handling (terminate specific call, private consultation).
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_ECC =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_ECC";

    /**
     * AG feature: response and hold.
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_RESPONSE_AND_HOLD =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_RESPONSE_AND_HOLD";

    /**
     * AG call handling feature: accept held or waiting call in three way calling scenarios.
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_ACCEPT_HELD_OR_WAITING_CALL =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_ACCEPT_HELD_OR_WAITING_CALL";

    /**
     * AG call handling feature: release held or waiting call in three way calling scenarios.
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_RELEASE_HELD_OR_WAITING_CALL =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_RELEASE_HELD_OR_WAITING_CALL";

    /**
     * AG call handling feature: release active call and accept held or waiting call in three way
     * calling scenarios.
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_RELEASE_AND_ACCEPT =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_RELEASE_AND_ACCEPT";

    /**
     * AG call handling feature: merge two calls, held and active - multi party conference mode.
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_MERGE =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_MERGE";

    /**
     * AG call handling feature: merge calls and disconnect from multi party
     * conversation leaving peers connected to each other.
     * Note that this feature needs to be supported by mobile network operator
     * as it requires connection and billing transfer.
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_MERGE_AND_DETACH =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_MERGE_AND_DETACH";

    /* Action result codes */
    /**
     * @hide
     */
    public static final int ACTION_RESULT_OK = 0;

    /**
     * @hide
     */
    public static final int ACTION_RESULT_ERROR = 1;

    /**
     * @hide
     */
    public static final int ACTION_RESULT_ERROR_NO_CARRIER = 2;

    /**
     * @hide
     */
    public static final int ACTION_RESULT_ERROR_BUSY = 3;

    /**
     * @hide
     */
    public static final int ACTION_RESULT_ERROR_NO_ANSWER = 4;

    /**
     * @hide
     */
    public static final int ACTION_RESULT_ERROR_DELAYED = 5;

    /**
     * @hide
     */
    public static final int ACTION_RESULT_ERROR_BLACKLISTED = 6;

    /**
     * @hide
     */
    public static final int ACTION_RESULT_ERROR_CME = 7;

    /* Detailed CME error codes */
    /**
     * @hide
     */
    public static final int CME_PHONE_FAILURE = 0;

    /**
     * @hide
     */
    public static final int CME_NO_CONNECTION_TO_PHONE = 1;

    /**
     * @hide
     */
    public static final int CME_OPERATION_NOT_ALLOWED = 3;

    /**
     * @hide
     */
    public static final int CME_OPERATION_NOT_SUPPORTED = 4;

    /**
     * @hide
     */
    public static final int CME_PHSIM_PIN_REQUIRED = 5;

    /**
     * @hide
     */
    public static final int CME_PHFSIM_PIN_REQUIRED = 6;

    /**
     * @hide
     */
    public static final int CME_PHFSIM_PUK_REQUIRED = 7;

    /**
     * @hide
     */
    public static final int CME_SIM_NOT_INSERTED = 10;

    /**
     * @hide
     */
    public static final int CME_SIM_PIN_REQUIRED = 11;

    /**
     * @hide
     */
    public static final int CME_SIM_PUK_REQUIRED = 12;

    /**
     * @hide
     */
    public static final int CME_SIM_FAILURE = 13;

    /**
     * @hide
     */
    public static final int CME_SIM_BUSY = 14;

    /**
     * @hide
     */
    public static final int CME_SIM_WRONG = 15;

    /**
     * @hide
     */
    public static final int CME_INCORRECT_PASSWORD = 16;

    /**
     * @hide
     */
    public static final int CME_SIM_PIN2_REQUIRED = 17;

    /**
     * @hide
     */
    public static final int CME_SIM_PUK2_REQUIRED = 18;

    /**
     * @hide
     */
    public static final int CME_MEMORY_FULL = 20;

    /**
     * @hide
     */
    public static final int CME_INVALID_INDEX = 21;

    /**
     * @hide
     */
    public static final int CME_NOT_FOUND = 22;

    /**
     * @hide
     */
    public static final int CME_MEMORY_FAILURE = 23;

    /**
     * @hide
     */
    public static final int CME_TEXT_STRING_TOO_LONG = 24;

    /**
     * @hide
     */
    public static final int CME_INVALID_CHARACTER_IN_TEXT_STRING = 25;

    /**
     * @hide
     */
    public static final int CME_DIAL_STRING_TOO_LONG = 26;

    /**
     * @hide
     */
    public static final int CME_INVALID_CHARACTER_IN_DIAL_STRING = 27;

    /**
     * @hide
     */
    public static final int CME_NO_NETWORK_SERVICE = 30;

    /**
     * @hide
     */
    public static final int CME_NETWORK_TIMEOUT = 31;

    /**
     * @hide
     */
    public static final int CME_EMERGENCY_SERVICE_ONLY = 32;

    /**
     * @hide
     */
    public static final int CME_NO_SIMULTANOUS_VOIP_CS_CALLS = 33;

    /**
     * @hide
     */
    public static final int CME_NOT_SUPPORTED_FOR_VOIP = 34;
    /**
     * @hide
     */
    public static final int CME_SIP_RESPONSE_CODE = 35;

    /**
     * @hide
     */
    public static final int CME_NETWORK_PERSONALIZATION_PIN_REQUIRED = 40;

    /**
     * @hide
     */
    public static final int CME_NETWORK_PERSONALIZATION_PUK_REQUIRED = 41;

    /**
     * @hide
     */
    public static final int CME_NETWORK_SUBSET_PERSONALIZATION_PIN_REQUIRED = 42;

    /**
     * @hide
     */
    public static final int CME_NETWORK_SUBSET_PERSONALIZATION_PUK_REQUIRED = 43;

    /**
     * @hide
     */
    public static final int CME_SERVICE_PROVIDER_PERSONALIZATION_PIN_REQUIRED = 44;

    /**
     * @hide
     */
    public static final int CME_SERVICE_PROVIDER_PERSONALIZATION_PUK_REQUIRED = 45;

    /**
     * @hide
     */
    public static final int CME_CORPORATE_PERSONALIZATION_PIN_REQUIRED = 46;

    /**
     * @hide
     */
    public static final int CME_CORPORATE_PERSONALIZATION_PUK_REQUIRED = 47;

    /**
     * @hide
     */
    public static final int CME_HIDDEN_KEY_REQUIRED = 48;

    /**
     * @hide
     */
    public static final int CME_EAP_NOT_SUPPORTED = 49;

    /**
     * @hide
     */
    public static final int CME_INCORRECT_PARAMETERS = 50;

    /* Action policy for other calls when accepting call */
    /**
     * @hide
     */
    public static final int CALL_ACCEPT_NONE = 0;

    /**
     * @hide
     */
    public static final int CALL_ACCEPT_HOLD = 1;

    /**
     * @hide
     */
    public static final int CALL_ACCEPT_TERMINATE = 2;

    private final BluetoothAdapter mAdapter;
    private final AttributionSource mAttributionSource;
    private final BluetoothProfileConnector<IBluetoothHeadsetClient> mProfileConnector =
            new BluetoothProfileConnector(this, BluetoothProfile.HEADSET_CLIENT,
                    "BluetoothHeadsetClient", IBluetoothHeadsetClient.class.getName()) {
                @Override
                public IBluetoothHeadsetClient getServiceInterface(IBinder service) {
                    return IBluetoothHeadsetClient.Stub.asInterface(service);
                }
    };

    /**
     * Create a BluetoothHeadsetClient proxy object.
     */
    BluetoothHeadsetClient(Context context, ServiceListener listener,
            BluetoothAdapter adapter) {
        mAdapter = adapter;
        mAttributionSource = adapter.getAttributionSource();
        mProfileConnector.connect(context, listener);
        mCloseGuard = new CloseGuard();
        mCloseGuard.open("close");
    }

    /**
     * Close the connection to the backing service. Other public functions of BluetoothHeadsetClient
     * will return default error results once close() has been called. Multiple invocations of
     * close() are ok.
     *
     * @hide
     */
    @Override
    public void close() {
        if (VDBG) log("close()");
        mProfileConnector.disconnect();
        if (mCloseGuard != null) {
            mCloseGuard.close();
        }
    }

    private IBluetoothHeadsetClient getService() {
        return mProfileConnector.getService();
    }

    /** @hide */
    protected void finalize() {
        if (mCloseGuard != null) {
            mCloseGuard.warnIfOpen();
        }
        close();
    }

    /**
     * Connects to remote device.
     *
     * Currently, the system supports only 1 connection. So, in case of the
     * second connection, this implementation will disconnect already connected
     * device automatically and will process the new one.
     *
     * @param device a remote device we want connect to
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_CONNECTION_STATE_CHANGED} intent.
     *
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean connect(BluetoothDevice device) {
        if (DBG) log("connect(" + device + ")");
        final IBluetoothHeadsetClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.connect(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Disconnects remote device
     *
     * @param device a remote device we want disconnect
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_CONNECTION_STATE_CHANGED} intent.
     *
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean disconnect(BluetoothDevice device) {
        if (DBG) log("disconnect(" + device + ")");
        final IBluetoothHeadsetClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.disconnect(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     * @hide
     */
    @SystemApi
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @NonNull List<BluetoothDevice> getConnectedDevices() {
        if (VDBG) log("getConnectedDevices()");
        final IBluetoothHeadsetClient service = getService();
        final List<BluetoothDevice> defaultValue = new ArrayList<BluetoothDevice>();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                        SynchronousResultReceiver.get();
                service.getConnectedDevices(mAttributionSource, recv);
                return Attributable.setAttributionSource(
                        recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue),
                        mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                throw e.rethrowFromSystemServer();
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     * @hide
     */
    @SystemApi
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @NonNull
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(@NonNull int[] states) {
        if (VDBG) log("getDevicesMatchingStates()");
        final IBluetoothHeadsetClient service = getService();
        final List<BluetoothDevice> defaultValue = new ArrayList<BluetoothDevice>();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                        SynchronousResultReceiver.get();
                service.getDevicesMatchingConnectionStates(states, mAttributionSource, recv);
                return Attributable.setAttributionSource(
                        recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue),
                        mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                throw e.rethrowFromSystemServer();
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     * @hide
     */
    @SystemApi
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @BtProfileState int getConnectionState(@NonNull BluetoothDevice device) {
        if (VDBG) log("getConnectionState(" + device + ")");
        final IBluetoothHeadsetClient service = getService();
        final int defaultValue = BluetoothProfile.STATE_DISCONNECTED;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getConnectionState(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                throw e.rethrowFromSystemServer();
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Set priority of the profile
     *
     * <p> The device should already be paired.
     * Priority can be one of {@link #PRIORITY_ON} or {@link #PRIORITY_OFF}
     *
     * @param device Paired bluetooth device
     * @param priority
     * @return true if priority is set, false on error
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean setPriority(BluetoothDevice device, int priority) {
        if (DBG) log("setPriority(" + device + ", " + priority + ")");
        return setConnectionPolicy(device, BluetoothAdapter.priorityToConnectionPolicy(priority));
    }

    /**
     * Set connection policy of the profile
     *
     * <p> The device should already be paired.
     * Connection policy can be one of {@link #CONNECTION_POLICY_ALLOWED},
     * {@link #CONNECTION_POLICY_FORBIDDEN}, {@link #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Paired bluetooth device
     * @param connectionPolicy is the connection policy to set to for this profile
     * @return true if connectionPolicy is set, false on error
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean setConnectionPolicy(@NonNull BluetoothDevice device,
            @ConnectionPolicy int connectionPolicy) {
        if (DBG) log("setConnectionPolicy(" + device + ", " + connectionPolicy + ")");
        final IBluetoothHeadsetClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)
                && (connectionPolicy == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN
                    || connectionPolicy == BluetoothProfile.CONNECTION_POLICY_ALLOWED)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.setConnectionPolicy(device, connectionPolicy, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                throw e.rethrowFromSystemServer();
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get the priority of the profile.
     *
     * <p> The priority can be any of:
     * {@link #PRIORITY_OFF}, {@link #PRIORITY_ON}, {@link #PRIORITY_UNDEFINED}
     *
     * @param device Bluetooth device
     * @return priority of the device
     * @hide
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public int getPriority(BluetoothDevice device) {
        if (VDBG) log("getPriority(" + device + ")");
        return BluetoothAdapter.connectionPolicyToPriority(getConnectionPolicy(device));
    }

    /**
     * Get the connection policy of the profile.
     *
     * <p> The connection policy can be any of:
     * {@link #CONNECTION_POLICY_ALLOWED}, {@link #CONNECTION_POLICY_FORBIDDEN},
     * {@link #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Bluetooth device
     * @return connection policy of the device
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @ConnectionPolicy int getConnectionPolicy(@NonNull BluetoothDevice device) {
        if (VDBG) log("getConnectionPolicy(" + device + ")");
        final IBluetoothHeadsetClient service = getService();
        final @ConnectionPolicy int defaultValue = BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getConnectionPolicy(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                throw e.rethrowFromSystemServer();
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Starts voice recognition.
     *
     * @param device remote device
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_AG_EVENT} intent.
     *
     * <p>Feature required for successful execution is being reported by: {@link
     * #EXTRA_AG_FEATURE_VOICE_RECOGNITION}. This method invocation will fail silently when feature
     * is not supported.</p>
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean startVoiceRecognition(BluetoothDevice device) {
        if (DBG) log("startVoiceRecognition()");
        final IBluetoothHeadsetClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.startVoiceRecognition(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Send vendor specific AT command.
     *
     * @param device remote device
     * @param vendorId vendor number by Bluetooth SIG
     * @param atCommand command to be sent. It start with + prefix and only one command at one time.
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise.
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean sendVendorAtCommand(BluetoothDevice device, int vendorId, String atCommand) {
        if (DBG) log("sendVendorSpecificCommand()");
        final IBluetoothHeadsetClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.sendVendorAtCommand(device, vendorId, atCommand, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Stops voice recognition.
     *
     * @param device remote device
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_AG_EVENT} intent.
     *
     * <p>Feature required for successful execution is being reported by: {@link
     * #EXTRA_AG_FEATURE_VOICE_RECOGNITION}. This method invocation will fail silently when feature
     * is not supported.</p>
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean stopVoiceRecognition(BluetoothDevice device) {
        if (DBG) log("stopVoiceRecognition()");
        final IBluetoothHeadsetClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.stopVoiceRecognition(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Returns list of all calls in any state.
     *
     * @param device remote device
     * @return list of calls; empty list if none call exists
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public List<BluetoothHeadsetClientCall> getCurrentCalls(BluetoothDevice device) {
        if (DBG) log("getCurrentCalls()");
        final IBluetoothHeadsetClient service = getService();
        final List<BluetoothHeadsetClientCall> defaultValue = null;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<List<BluetoothHeadsetClientCall>> recv =
                        SynchronousResultReceiver.get();
                service.getCurrentCalls(device, mAttributionSource, recv);
                return Attributable.setAttributionSource(
                        recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue),
                        mAttributionSource);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Returns list of current values of AG indicators.
     *
     * @param device remote device
     * @return bundle of AG  indicators; null if device is not in CONNECTED state
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public Bundle getCurrentAgEvents(BluetoothDevice device) {
        if (DBG) log("getCurrentAgEvents()");
        final IBluetoothHeadsetClient service = getService();
        final Bundle defaultValue = null;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Bundle> recv = SynchronousResultReceiver.get();
                service.getCurrentAgEvents(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Accepts a call
     *
     * @param device remote device
     * @param flag action policy while accepting a call. Possible values {@link #CALL_ACCEPT_NONE},
     * {@link #CALL_ACCEPT_HOLD}, {@link #CALL_ACCEPT_TERMINATE}
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_CALL_CHANGED} intent.
     *
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean acceptCall(BluetoothDevice device, int flag) {
        if (DBG) log("acceptCall()");
        final IBluetoothHeadsetClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.acceptCall(device, flag, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Holds a call.
     *
     * @param device remote device
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_CALL_CHANGED} intent.
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean holdCall(BluetoothDevice device) {
        if (DBG) log("holdCall()");
        final IBluetoothHeadsetClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.holdCall(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Rejects a call.
     *
     * @param device remote device
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_CALL_CHANGED} intent.
     *
     * <p>Feature required for successful execution is being reported by: {@link
     * #EXTRA_AG_FEATURE_REJECT_CALL}. This method invocation will fail silently when feature is not
     * supported.</p>
     *
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean rejectCall(BluetoothDevice device) {
        if (DBG) log("rejectCall()");
        final IBluetoothHeadsetClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.rejectCall(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Terminates a specified call.
     *
     * Works only when Extended Call Control is supported by Audio Gateway.
     *
     * @param device remote device
     * @param call Handle of call obtained in {@link #dial(BluetoothDevice, String)} or obtained via
     * {@link #ACTION_CALL_CHANGED}. {@code call} may be null in which case we will hangup all active
     * calls.
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_CALL_CHANGED} intent.
     *
     * <p>Feature required for successful execution is being reported by: {@link
     * #EXTRA_AG_FEATURE_ECC}. This method invocation will fail silently when feature is not
     * supported.</p>
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean terminateCall(BluetoothDevice device, BluetoothHeadsetClientCall call) {
        if (DBG) log("terminateCall()");
        final IBluetoothHeadsetClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.terminateCall(device, call, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Enters private mode with a specified call.
     *
     * Works only when Extended Call Control is supported by Audio Gateway.
     *
     * @param device remote device
     * @param index index of the call to connect in private mode
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_CALL_CHANGED} intent.
     *
     * <p>Feature required for successful execution is being reported by: {@link
     * #EXTRA_AG_FEATURE_ECC}. This method invocation will fail silently when feature is not
     * supported.</p>
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean enterPrivateMode(BluetoothDevice device, int index) {
        if (DBG) log("enterPrivateMode()");
        final IBluetoothHeadsetClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.enterPrivateMode(device, index, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Performs explicit call transfer.
     *
     * That means connect other calls and disconnect.
     *
     * @param device remote device
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_CALL_CHANGED} intent.
     *
     * <p>Feature required for successful execution is being reported by: {@link
     * #EXTRA_AG_FEATURE_MERGE_AND_DETACH}. This method invocation will fail silently when feature
     * is not supported.</p>
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean explicitCallTransfer(BluetoothDevice device) {
        if (DBG) log("explicitCallTransfer()");
        final IBluetoothHeadsetClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.explicitCallTransfer(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Places a call with specified number.
     *
     * @param device remote device
     * @param number valid phone number
     * @return <code>{@link BluetoothHeadsetClientCall} call</code> if command has been issued
     * successfully; <code>{@link null}</code> otherwise; upon completion HFP sends {@link
     * #ACTION_CALL_CHANGED} intent in case of success; {@link #ACTION_RESULT} is sent otherwise;
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothHeadsetClientCall dial(BluetoothDevice device, String number) {
        if (DBG) log("dial()");
        final IBluetoothHeadsetClient service = getService();
        final BluetoothHeadsetClientCall defaultValue = null;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<BluetoothHeadsetClientCall> recv =
                        SynchronousResultReceiver.get();
                service.dial(device, number, mAttributionSource, recv);
                return Attributable.setAttributionSource(
                        recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue),
                        mAttributionSource);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Sends DTMF code.
     *
     * Possible code values : 0,1,2,3,4,5,6,7,8,9,A,B,C,D,*,#
     *
     * @param device remote device
     * @param code ASCII code
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_RESULT} intent;
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean sendDTMF(BluetoothDevice device, byte code) {
        if (DBG) log("sendDTMF()");
        final IBluetoothHeadsetClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.sendDTMF(device, code, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get a number corresponding to last voice tag recorded on AG.
     *
     * @param device remote device
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_LAST_VTAG} or {@link #ACTION_RESULT}
     * intent;
     *
     * <p>Feature required for successful execution is being reported by: {@link
     * #EXTRA_AG_FEATURE_ATTACH_NUMBER_TO_VT}. This method invocation will fail silently when
     * feature is not supported.</p>
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean getLastVoiceTagNumber(BluetoothDevice device) {
        if (DBG) log("getLastVoiceTagNumber()");
        final IBluetoothHeadsetClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.getLastVoiceTagNumber(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Returns current audio state of Audio Gateway.
     *
     * Note: This is an internal function and shouldn't be exposed
     *
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public int getAudioState(BluetoothDevice device) {
        if (VDBG) log("getAudioState");
        final IBluetoothHeadsetClient service = getService();
        final int defaultValue = BluetoothHeadsetClient.STATE_AUDIO_DISCONNECTED;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getAudioState(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        } else {
            return defaultValue;
        }
        return BluetoothHeadsetClient.STATE_AUDIO_DISCONNECTED;
    }

    /**
     * Sets whether audio routing is allowed.
     *
     * @param device remote device
     * @param allowed if routing is allowed to the device Note: This is an internal function and
     * shouldn't be exposed
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public void setAudioRouteAllowed(BluetoothDevice device, boolean allowed) {
        if (VDBG) log("setAudioRouteAllowed");
        final IBluetoothHeadsetClient service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver recv = SynchronousResultReceiver.get();
                service.setAudioRouteAllowed(device, allowed, mAttributionSource, recv);
                recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(null);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    /**
     * Returns whether audio routing is allowed.
     *
     * @param device remote device
     * @return whether the command succeeded Note: This is an internal function and shouldn't be
     * exposed
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean getAudioRouteAllowed(BluetoothDevice device) {
        if (VDBG) log("getAudioRouteAllowed");
        final IBluetoothHeadsetClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.getAudioRouteAllowed(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Initiates a connection of audio channel.
     *
     * It setup SCO channel with remote connected Handsfree AG device.
     *
     * @param device remote device
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_AUDIO_STATE_CHANGED} intent;
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean connectAudio(BluetoothDevice device) {
        if (VDBG) log("connectAudio");
        final IBluetoothHeadsetClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.connectAudio(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Disconnects audio channel.
     *
     * It tears down the SCO channel from remote AG device.
     *
     * @param device remote device
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_AUDIO_STATE_CHANGED} intent;
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean disconnectAudio(BluetoothDevice device) {
        if (VDBG) log("disconnectAudio");
        final IBluetoothHeadsetClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.disconnectAudio(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get Audio Gateway features
     *
     * @param device remote device
     * @return bundle of AG features; null if no service or AG not connected
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public Bundle getCurrentAgFeatures(BluetoothDevice device) {
        if (VDBG) log("getCurrentAgFeatures");
        final IBluetoothHeadsetClient service = getService();
        final Bundle defaultValue = null;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<Bundle> recv = SynchronousResultReceiver.get();
                service.getCurrentAgFeatures(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * A class that contains the network service info provided by the HFP Client profile
     *
     * @hide
     */
    @SystemApi
    public static final class NetworkServiceState implements Parcelable {
        /** The device associated with this service state */
        private final BluetoothDevice mDevice;

        /** True if there is service available, False otherwise */
        private final boolean mIsServiceAvailable;

        /** The name of the operator associated with the remote device's current network */
        private final String mOperatorName;

        /**
         * The general signal strength, from 0 to 5
         */
        private final int mSignalStrength;

        /** True if we are network roaming, False otherwise */
        private final boolean mIsRoaming;

        /**
         * Create a NetworkServiceState Object
         *
         * @param device The device associated with this network signal state
         * @param isServiceAvailable True if there is service available, False otherwise
         * @param operatorName The name of the operator associated with the remote device's current
         *                     network. Use Null if the value is unknown
         * @param signalStrength The general signal strength
         * @param isRoaming True if we are network roaming, False otherwise
         *
         * @hide
         */
        public NetworkServiceState(BluetoothDevice device, boolean isServiceAvailable,
                String operatorName, int signalStrength, boolean isRoaming) {
            mDevice = device;
            mIsServiceAvailable = isServiceAvailable;
            mOperatorName = operatorName;
            mSignalStrength = signalStrength;
            mIsRoaming = isRoaming;
        }

        /**
         * Get the device associated with this network service state
         *
         * @return a BluetoothDevice associated with this state
         *
         * @hide
         */
        @SystemApi
        public @NonNull BluetoothDevice getDevice() {
            return mDevice;
        }

        /**
         * Get the network service availablility state
         *
         * @return True if there is service available, False otherwise
         *
         * @hide
         */
        @SystemApi
        public boolean isServiceAvailable() {
            return mIsServiceAvailable;
        }

        /**
         * Get the network operator name
         *
         * @return A string representing the name of the operator the remote device is on, or null
         *         if unknown.
         *
         * @hide
         */
        @SystemApi
        public @Nullable String getNetworkOperatorName() {
            return mOperatorName;
        }

        /**
         * The HFP Client defined signal strength, from 0 to 5.
         *
         * Bluetooth HFP v1.8 specifies that the signal strength of a device can be [0, 5]. It does
         * not place any requirements on how a device derives those values. While they're typically
         * derived from signal quality/RSSI buckets, there's no way to be certain on the exact
         * meaning. Derivation methods can even change between wireless cellular technologies.
         *
         * That said, you can "generally" interpret the values relative to each other as follows:
         *   - Level 0: None/Unknown
         *   - Level 1: Very Poor
         *   - Level 2: Poor
         *   - Level 3: Fair
         *   - Level 4: Good
         *   - Level 5: Great
         *
         * @return the HFP Client defined signal strength, range [0, 5]
         *
         * @hide
         */
        @SystemApi
        public @IntRange(from = 0, to = 5) int getSignalStrength() {
            return mSignalStrength;
        }

        /**
         * Get the network service roaming status
         *
         * * @return True if we are network roaming, False otherwise
         *
         * @hide
         */
        @SystemApi
        public boolean isRoaming() {
            return mIsRoaming;
        }

        /**
         * {@link Parcelable.Creator} interface implementation.
         */
        public static final @NonNull Parcelable.Creator<NetworkServiceState> CREATOR =
                new Parcelable.Creator<NetworkServiceState>() {
            public NetworkServiceState createFromParcel(Parcel in) {
                return new NetworkServiceState((BluetoothDevice) in.readParcelable(null),
                        in.readInt() == 1, in.readString(), in.readInt(), in.readInt() == 1);
            }

            public @NonNull NetworkServiceState[] newArray(int size) {
                return new NetworkServiceState[size];
            }
        };

        /**
         * @hide
         */
        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            out.writeParcelable(mDevice, 0);
            out.writeInt(mIsServiceAvailable ? 1 : 0);
            out.writeString(mOperatorName);
            out.writeInt(mSignalStrength);
            out.writeInt(mIsRoaming ? 1 : 0);
        }

        /**
         * @hide
         */
        @Override
        public int describeContents() {
            return 0;
        }
    }

    /**
     * Intent used to broadcast the change in network service state of an HFP Client device
     *
     * <p>This intent will have 2 extras:
     * <ul>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device. </li>
     * <li> {@link EXTRA_NETWORK_SERVICE_STATE} - A {@link NetworkServiceState} object. </li>
     * </ul>
     *
     * @hide
     */
    @SuppressLint("ActionValue")
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_NETWORK_SERVICE_STATE_CHANGED =
            "android.bluetooth.headsetclient.profile.action.NETWORK_SERVICE_STATE_CHANGED";

    /**
     * Extra for the network service state changed intent.
     *
     * This extra represents the current network service state of a connected Bluetooth device.
     *
     * @hide
     */
    @SuppressLint("ActionValue")
    @SystemApi
    public static final String EXTRA_NETWORK_SERVICE_STATE =
            "android.bluetooth.headsetclient.extra.EXTRA_NETWORK_SERVICE_STATE";

    /**
     * Get the network service state for a device
     *
     * @param device The {@link BluetoothDevice} you want the network service state for
     * @return A {@link NetworkServiceState} representing the network service state of the device,
     *         or null if the device is not connected
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @Nullable NetworkServiceState getNetworkServiceState(@NonNull BluetoothDevice device) {
        if (device == null) {
            return null;
        }

        Bundle agEvents = getCurrentAgEvents(device);
        if (agEvents == null) {
            return null;
        }

        boolean isServiceAvailable = (agEvents.getInt(EXTRA_NETWORK_STATUS, 0) == 1);
        int signalStrength = agEvents.getInt(EXTRA_NETWORK_SIGNAL_STRENGTH, 0);
        String operatorName = agEvents.getString(EXTRA_OPERATOR_NAME, null);
        boolean isRoaming = (agEvents.getInt(EXTRA_NETWORK_ROAMING, 0) == 1);

        return new NetworkServiceState(device, isServiceAvailable, operatorName, signalStrength,
                isRoaming);
    }

    private boolean isEnabled() {
        return mAdapter.getState() == BluetoothAdapter.STATE_ON;
    }

    private static boolean isValidDevice(BluetoothDevice device) {
        return device != null && BluetoothAdapter.checkBluetoothAddress(device.getAddress());
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
