/*
 * Copyright (C) 2008 The Android Open Source Project
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

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_PRIVILEGED;
import static android.Manifest.permission.MODIFY_PHONE_STATE;

import static java.util.Objects.requireNonNull;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.bluetooth.annotations.RequiresLegacyBluetoothAdminPermission;
import android.bluetooth.annotations.RequiresLegacyBluetoothPermission;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.AttributionSource;
import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;

/**
 * Public API for controlling the Bluetooth Headset Service. This includes both Bluetooth Headset
 * and Handsfree (v1.5) profiles.
 *
 * <p>BluetoothHeadset is a proxy object for controlling the Bluetooth Headset Service via IPC.
 *
 * <p>Use {@link BluetoothAdapter#getProfileProxy} to get the BluetoothHeadset proxy object. Use
 * {@link BluetoothAdapter#closeProfileProxy} to close the service connection.
 *
 * <p>Android only supports one connected Bluetooth Headset at a time. Each method is protected with
 * its appropriate permission.
 */
public final class BluetoothHeadset implements BluetoothProfile {
    private static final String TAG = "BluetoothHeadset";

    private static final boolean DBG = Log.isLoggable(TAG, Log.DEBUG);
    private static final boolean VDBG = false;

    /**
     * Intent used to broadcast the change in connection state of the Headset profile.
     *
     * <p>This intent will have 3 extras:
     *
     * <ul>
     *   <li>{@link #EXTRA_STATE} - The current state of the profile.
     *   <li>{@link #EXTRA_PREVIOUS_STATE}- The previous state of the profile.
     *   <li>{@link BluetoothDevice#EXTRA_DEVICE} - The remote device.
     * </ul>
     *
     * <p>{@link #EXTRA_STATE} or {@link #EXTRA_PREVIOUS_STATE} can be any of {@link
     * #STATE_DISCONNECTED}, {@link #STATE_CONNECTING}, {@link #STATE_CONNECTED}, {@link
     * #STATE_DISCONNECTING}.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CONNECTION_STATE_CHANGED =
            "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED";

    /**
     * Intent used to broadcast the change in the Audio Connection state of the HFP profile.
     *
     * <p>This intent will have 3 extras:
     *
     * <ul>
     *   <li>{@link #EXTRA_STATE} - The current state of the profile.
     *   <li>{@link #EXTRA_PREVIOUS_STATE}- The previous state of the profile.
     *   <li>{@link BluetoothDevice#EXTRA_DEVICE} - The remote device.
     * </ul>
     *
     * <p>{@link #EXTRA_STATE} or {@link #EXTRA_PREVIOUS_STATE} can be any of {@link
     * #STATE_AUDIO_CONNECTED}, {@link #STATE_AUDIO_DISCONNECTED},
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_AUDIO_STATE_CHANGED =
            "android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED";

    /**
     * Intent used to broadcast the selection of a connected device as active.
     *
     * <p>This intent will have one extra:
     *
     * <ul>
     *   <li>{@link BluetoothDevice#EXTRA_DEVICE} - The remote device. It can be null if no device
     *       is active.
     * </ul>
     *
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @SuppressLint("ActionValue")
    public static final String ACTION_ACTIVE_DEVICE_CHANGED =
            "android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED";

    /**
     * Intent used to broadcast that the headset has posted a vendor-specific event.
     *
     * <p>This intent will have 4 extras and 1 category.
     *
     * <ul>
     *   <li>{@link BluetoothDevice#EXTRA_DEVICE} - The remote Bluetooth Device
     *   <li>{@link #EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD} - The vendor specific command
     *   <li>{@link #EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE} - The AT command type which can
     *       be one of {@link #AT_CMD_TYPE_READ}, {@link #AT_CMD_TYPE_TEST}, or {@link
     *       #AT_CMD_TYPE_SET}, {@link #AT_CMD_TYPE_BASIC},{@link #AT_CMD_TYPE_ACTION}.
     *   <li>{@link #EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS} - Command arguments.
     * </ul>
     *
     * <p>The category is the Company ID of the vendor defining the vendor-specific command. {@link
     * BluetoothAssignedNumbers}
     *
     * <p>For example, for Plantronics specific events Category will be {@link
     * #VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY}.55
     *
     * <p>For example, an AT+XEVENT=foo,3 will get translated into
     *
     * <ul>
     *   <li>EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD = +XEVENT
     *   <li>EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE = AT_CMD_TYPE_SET
     *   <li>EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS = foo, 3
     * </ul>
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_VENDOR_SPECIFIC_HEADSET_EVENT =
            "android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT";

    /**
     * A String extra field in {@link #ACTION_VENDOR_SPECIFIC_HEADSET_EVENT} intents that contains
     * the name of the vendor-specific command.
     */
    public static final String EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD =
            "android.bluetooth.headset.extra.VENDOR_SPECIFIC_HEADSET_EVENT_CMD";

    /**
     * An int extra field in {@link #ACTION_VENDOR_SPECIFIC_HEADSET_EVENT} intents that contains the
     * AT command type of the vendor-specific command.
     */
    public static final String EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE =
            "android.bluetooth.headset.extra.VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE";

    /**
     * AT command type READ used with {@link #EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE} For
     * example, AT+VGM?. There are no arguments for this command type.
     */
    public static final int AT_CMD_TYPE_READ = 0;

    /**
     * AT command type TEST used with {@link #EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE} For
     * example, AT+VGM=?. There are no arguments for this command type.
     */
    public static final int AT_CMD_TYPE_TEST = 1;

    /**
     * AT command type SET used with {@link #EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE} For
     * example, AT+VGM=<args>.
     */
    public static final int AT_CMD_TYPE_SET = 2;

    /**
     * AT command type BASIC used with {@link #EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE} For
     * example, ATD. Single character commands and everything following the character are arguments.
     */
    public static final int AT_CMD_TYPE_BASIC = 3;

    /**
     * AT command type ACTION used with {@link #EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE} For
     * example, AT+CHUP. There are no arguments for action commands.
     */
    public static final int AT_CMD_TYPE_ACTION = 4;

    /**
     * A Parcelable String array extra field in {@link #ACTION_VENDOR_SPECIFIC_HEADSET_EVENT}
     * intents that contains the arguments to the vendor-specific command.
     */
    public static final String EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS =
            "android.bluetooth.headset.extra.VENDOR_SPECIFIC_HEADSET_EVENT_ARGS";

    /**
     * The intent category to be used with {@link #ACTION_VENDOR_SPECIFIC_HEADSET_EVENT} for the
     * companyId
     */
    public static final String VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY =
            "android.bluetooth.headset.intent.category.companyid";

    /** A vendor-specific command for unsolicited result code. */
    public static final String VENDOR_RESULT_CODE_COMMAND_ANDROID = "+ANDROID";

    /**
     * A vendor-specific AT command
     *
     * @hide
     */
    public static final String VENDOR_SPECIFIC_HEADSET_EVENT_XAPL = "+XAPL";

    /**
     * A vendor-specific AT command
     *
     * @hide
     */
    public static final String VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV = "+IPHONEACCEV";

    /**
     * Battery level indicator associated with {@link #VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV}
     *
     * @hide
     */
    public static final int VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV_BATTERY_LEVEL = 1;

    /**
     * A vendor-specific AT command
     *
     * @hide
     */
    public static final String VENDOR_SPECIFIC_HEADSET_EVENT_XEVENT = "+XEVENT";

    /**
     * Battery level indicator associated with {@link #VENDOR_SPECIFIC_HEADSET_EVENT_XEVENT}
     *
     * @hide
     */
    public static final String VENDOR_SPECIFIC_HEADSET_EVENT_XEVENT_BATTERY_LEVEL = "BATTERY";

    /**
     * A vendor-specific AT command that asks for the information about device manufacturer.
     *
     * @hide
     */
    public static final String VENDOR_SPECIFIC_HEADSET_EVENT_CGMI = "+CGMI";

    /**
     * A vendor-specific AT command that asks for the information about the model of the device.
     *
     * @hide
     */
    public static final String VENDOR_SPECIFIC_HEADSET_EVENT_CGMM = "+CGMM";

    /**
     * A vendor-specific AT command that asks for the revision information, for Android we will
     * return the OS version and build number.
     *
     * @hide
     */
    public static final String VENDOR_SPECIFIC_HEADSET_EVENT_CGMR = "+CGMR";

    /**
     * A vendor-specific AT command that asks for the device's serial number.
     *
     * @hide
     */
    public static final String VENDOR_SPECIFIC_HEADSET_EVENT_CGSN = "+CGSN";

    /**
     * Headset state when SCO audio is not connected. This state can be one of {@link #EXTRA_STATE}
     * or {@link #EXTRA_PREVIOUS_STATE} of {@link #ACTION_AUDIO_STATE_CHANGED} intent.
     */
    public static final int STATE_AUDIO_DISCONNECTED = 10;

    /**
     * Headset state when SCO audio is connecting. This state can be one of {@link #EXTRA_STATE} or
     * {@link #EXTRA_PREVIOUS_STATE} of {@link #ACTION_AUDIO_STATE_CHANGED} intent.
     */
    public static final int STATE_AUDIO_CONNECTING = 11;

    /**
     * Headset state when SCO audio is connected. This state can be one of {@link #EXTRA_STATE} or
     * {@link #EXTRA_PREVIOUS_STATE} of {@link #ACTION_AUDIO_STATE_CHANGED} intent.
     */
    public static final int STATE_AUDIO_CONNECTED = 12;

    /**
     * Intent used to broadcast the headset's indicator status
     *
     * <p>This intent will have 3 extras:
     *
     * <ul>
     *   <li>{@link #EXTRA_HF_INDICATORS_IND_ID} - The Assigned number of headset Indicator which is
     *       supported by the headset ( as indicated by AT+BIND command in the SLC sequence) or
     *       whose value is changed (indicated by AT+BIEV command)
     *   <li>{@link #EXTRA_HF_INDICATORS_IND_VALUE} - Updated value of headset indicator.
     *   <li>{@link BluetoothDevice#EXTRA_DEVICE} - Remote device.
     * </ul>
     *
     * <p>{@link #EXTRA_HF_INDICATORS_IND_ID} is defined by Bluetooth SIG and each of the indicators
     * are given an assigned number. Below shows the assigned number of Indicator added so far -
     * Enhanced Safety - 1, Valid Values: 0 - Disabled, 1 - Enabled - Battery Level - 2, Valid
     * Values: 0~100 - Remaining level of Battery
     *
     * @hide
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_HF_INDICATORS_VALUE_CHANGED =
            "android.bluetooth.headset.action.HF_INDICATORS_VALUE_CHANGED";

    /**
     * A int extra field in {@link #ACTION_HF_INDICATORS_VALUE_CHANGED} intents that contains the
     * assigned number of the headset indicator as defined by Bluetooth SIG that is being sent.
     * Value range is 0-65535 as defined in HFP 1.7
     *
     * @hide
     */
    public static final String EXTRA_HF_INDICATORS_IND_ID =
            "android.bluetooth.headset.extra.HF_INDICATORS_IND_ID";

    /**
     * A int extra field in {@link #ACTION_HF_INDICATORS_VALUE_CHANGED} intents that contains the
     * value of the Headset indicator that is being sent.
     *
     * @hide
     */
    public static final String EXTRA_HF_INDICATORS_IND_VALUE =
            "android.bluetooth.headset.extra.HF_INDICATORS_IND_VALUE";

    private final BluetoothAdapter mAdapter;
    private final AttributionSource mAttributionSource;

    private IBluetoothHeadset mService;

    /** Create a BluetoothHeadset proxy object. */
    /* package */ BluetoothHeadset(Context context, BluetoothAdapter adapter) {
        mAdapter = adapter;
        mAttributionSource = adapter.getAttributionSource();
        mService = null;
    }

    /**
     * Close the connection to the backing service. Other public functions of BluetoothHeadset will
     * return default error results once close() has been called. Multiple invocations of close()
     * are ok.
     *
     * @hide
     */
    @UnsupportedAppUsage
    public void close() {
        mAdapter.closeProfileProxy(this);
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public void onServiceConnected(IBinder service) {
        mService = IBluetoothHeadset.Stub.asInterface(service);
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public void onServiceDisconnected() {
        mService = null;
    }

    private IBluetoothHeadset getService() {
        return mService;
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public BluetoothAdapter getAdapter() {
        return mAdapter;
    }

    /** @hide */
    @Override
    @SuppressWarnings("Finalize") // empty finalize for api signature
    protected void finalize() throws Throwable {
        // The empty finalize needs to be kept or the
        // cts signature tests would fail.
    }

    /**
     * Initiate connection to a profile of the remote bluetooth device.
     *
     * <p>Currently, the system supports only 1 connection to the headset/handsfree profile. The API
     * will automatically disconnect connected devices before connecting.
     *
     * <p>This API returns false in scenarios like the profile on the device is already connected or
     * Bluetooth is not turned on. When this API returns true, it is guaranteed that connection
     * state intent for the profile will be broadcasted with the state. Users can get the connection
     * state of the profile from this intent.
     *
     * @param device Remote Bluetooth Device
     * @return false on immediate error, true otherwise
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                MODIFY_PHONE_STATE,
            })
    public boolean connect(BluetoothDevice device) {
        if (DBG) log("connect(" + device + ")");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                return service.connect(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /**
     * Initiate disconnection from a profile
     *
     * <p>This API will return false in scenarios like the profile on the Bluetooth device is not in
     * connected state etc. When this API returns, true, it is guaranteed that the connection state
     * change intent will be broadcasted with the state. Users can get the disconnection state of
     * the profile from this intent.
     *
     * <p>If the disconnection is initiated by a remote device, the state will transition from
     * {@link #STATE_CONNECTED} to {@link #STATE_DISCONNECTED}. If the disconnect is initiated by
     * the host (local) device the state will transition from {@link #STATE_CONNECTED} to state
     * {@link #STATE_DISCONNECTING} to state {@link #STATE_DISCONNECTED}. The transition to {@link
     * #STATE_DISCONNECTING} can be used to distinguish between the two scenarios.
     *
     * @param device Remote Bluetooth Device
     * @return false on immediate error, true otherwise
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean disconnect(BluetoothDevice device) {
        if (DBG) log("disconnect(" + device + ")");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                return service.disconnect(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public List<BluetoothDevice> getConnectedDevices() {
        if (VDBG) log("getConnectedDevices()");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                return Attributable.setAttributionSource(
                        service.getConnectedDevices(mAttributionSource), mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        if (VDBG) log("getDevicesMatchingStates()");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                return Attributable.setAttributionSource(
                        service.getDevicesMatchingConnectionStates(states, mAttributionSource),
                        mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public int getConnectionState(BluetoothDevice device) {
        if (VDBG) log("getConnectionState(" + device + ")");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                return service.getConnectionState(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    /**
     * Set connection policy of the profile
     *
     * <p>The device should already be paired. Connection policy can be one of {@link
     * #CONNECTION_POLICY_ALLOWED}, {@link #CONNECTION_POLICY_FORBIDDEN}, {@link
     * #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Paired bluetooth device
     * @param connectionPolicy is the connection policy to set to for this profile
     * @return true if connectionPolicy is set, false on error
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
                MODIFY_PHONE_STATE,
            })
    public boolean setConnectionPolicy(
            @NonNull BluetoothDevice device, @ConnectionPolicy int connectionPolicy) {
        if (DBG) log("setConnectionPolicy(" + device + ", " + connectionPolicy + ")");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()
                && isValidDevice(device)
                && (connectionPolicy == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN
                        || connectionPolicy == BluetoothProfile.CONNECTION_POLICY_ALLOWED)) {
            try {
                return service.setConnectionPolicy(device, connectionPolicy, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /**
     * Get the priority of the profile.
     *
     * <p>The priority can be any of: {@link #PRIORITY_AUTO_CONNECT}, {@link #PRIORITY_OFF}, {@link
     * #PRIORITY_ON}, {@link #PRIORITY_UNDEFINED}
     *
     * @param device Bluetooth device
     * @return priority of the device
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public int getPriority(BluetoothDevice device) {
        if (VDBG) log("getPriority(" + device + ")");
        return BluetoothAdapter.connectionPolicyToPriority(getConnectionPolicy(device));
    }

    /**
     * Get the connection policy of the profile.
     *
     * <p>The connection policy can be any of: {@link #CONNECTION_POLICY_ALLOWED}, {@link
     * #CONNECTION_POLICY_FORBIDDEN}, {@link #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Bluetooth device
     * @return connection policy of the device
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @ConnectionPolicy int getConnectionPolicy(@NonNull BluetoothDevice device) {
        if (VDBG) log("getConnectionPolicy(" + device + ")");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                return service.getConnectionPolicy(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
    }

    /**
     * Checks whether the headset supports some form of noise reduction
     *
     * @param device Bluetooth device
     * @return true if echo cancellation and/or noise reduction is supported, false otherwise
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean isNoiseReductionSupported(@NonNull BluetoothDevice device) {
        if (DBG) log("isNoiseReductionSupported()");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                return service.isNoiseReductionSupported(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /**
     * Checks whether the headset supports voice recognition
     *
     * @param device Bluetooth device
     * @return true if voice recognition is supported, false otherwise
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean isVoiceRecognitionSupported(@NonNull BluetoothDevice device) {
        if (DBG) log("isVoiceRecognitionSupported()");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                return service.isVoiceRecognitionSupported(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /**
     * Start Bluetooth voice recognition. This methods sends the voice recognition AT command to the
     * headset and establishes the audio connection.
     *
     * <p>Users can listen to {@link #ACTION_AUDIO_STATE_CHANGED}. If this function returns true,
     * this intent will be broadcasted with {@link #EXTRA_STATE} set to {@link
     * #STATE_AUDIO_CONNECTING}.
     *
     * <p>{@link #EXTRA_STATE} will transition from {@link #STATE_AUDIO_CONNECTING} to {@link
     * #STATE_AUDIO_CONNECTED} when audio connection is established and to {@link
     * #STATE_AUDIO_DISCONNECTED} in case of failure to establish the audio connection.
     *
     * @param device Bluetooth headset
     * @return false if there is no headset connected, or the connected headset doesn't support
     *     voice recognition, or voice recognition is already started, or audio channel is occupied,
     *     or on error, true otherwise
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean startVoiceRecognition(BluetoothDevice device) {
        if (DBG) log("startVoiceRecognition()");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                return service.startVoiceRecognition(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /**
     * Stop Bluetooth Voice Recognition mode, and shut down the Bluetooth audio path.
     *
     * <p>Users can listen to {@link #ACTION_AUDIO_STATE_CHANGED}. If this function returns true,
     * this intent will be broadcasted with {@link #EXTRA_STATE} set to {@link
     * #STATE_AUDIO_DISCONNECTED}.
     *
     * @param device Bluetooth headset
     * @return false if there is no headset connected, or voice recognition has not started, or
     *     voice recognition has ended on this headset, or on error, true otherwise
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean stopVoiceRecognition(BluetoothDevice device) {
        if (DBG) log("stopVoiceRecognition()");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                return service.stopVoiceRecognition(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /**
     * Check if Bluetooth SCO audio is connected.
     *
     * @param device Bluetooth headset
     * @return true if SCO is connected, false otherwise or on error
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean isAudioConnected(BluetoothDevice device) {
        if (VDBG) log("isAudioConnected()");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                return service.isAudioConnected(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                BluetoothHeadset.STATE_AUDIO_DISCONNECTED,
                BluetoothHeadset.STATE_AUDIO_CONNECTING,
                BluetoothHeadset.STATE_AUDIO_CONNECTED,
                BluetoothStatusCodes.ERROR_TIMEOUT
            })
    public @interface GetAudioStateReturnValues {}

    /**
     * Get the current audio state of the Headset.
     *
     * @param device is the Bluetooth device for which the audio state is being queried
     * @return the audio state of the device or an error code
     * @throws NullPointerException if the device is null
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    public @GetAudioStateReturnValues int getAudioState(@NonNull BluetoothDevice device) {
        if (VDBG) log("getAudioState");
        requireNonNull(device);
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (!isDisabled()) {
            try {
                return service.getAudioState(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return BluetoothHeadset.STATE_AUDIO_DISCONNECTED;
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                BluetoothStatusCodes.SUCCESS,
                BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND,
                BluetoothStatusCodes.ERROR_TIMEOUT,
                BluetoothStatusCodes.ERROR_UNKNOWN,
            })
    public @interface SetAudioRouteAllowedReturnValues {}

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                BluetoothStatusCodes.ALLOWED,
                BluetoothStatusCodes.NOT_ALLOWED,
                BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND,
                BluetoothStatusCodes.ERROR_TIMEOUT,
                BluetoothStatusCodes.ERROR_UNKNOWN,
            })
    public @interface GetAudioRouteAllowedReturnValues {}

    /**
     * Sets whether audio routing is allowed. When set to {@code false}, the AG will not route any
     * audio to the HF unless explicitly told to. This method should be used in cases where the SCO
     * channel is shared between multiple profiles and must be delegated by a source knowledgeable.
     *
     * @param allowed {@code true} if the profile can reroute audio, {@code false} otherwise.
     * @return {@link BluetoothStatusCodes#SUCCESS} upon successful setting, otherwise an error
     *     code.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    public @SetAudioRouteAllowedReturnValues int setAudioRouteAllowed(boolean allowed) {
        if (VDBG) log("setAudioRouteAllowed");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
            return BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND;
        } else if (isEnabled()) {
            try {
                service.setAudioRouteAllowed(allowed, mAttributionSource);
                return BluetoothStatusCodes.SUCCESS;
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }

        Log.e(TAG, "setAudioRouteAllowed: Bluetooth disabled, but profile service still bound");
        return BluetoothStatusCodes.ERROR_UNKNOWN;
    }

    /**
     * @return {@link BluetoothStatusCodes#ALLOWED} if audio routing is allowed, {@link
     *     BluetoothStatusCodes#NOT_ALLOWED} if audio routing is not allowed, or an error code if an
     *     error occurs. see {@link #setAudioRouteAllowed(boolean)}.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    public @GetAudioRouteAllowedReturnValues int getAudioRouteAllowed() {
        if (VDBG) log("getAudioRouteAllowed");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
            return BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND;
        } else if (isEnabled()) {
            try {
                return service.getAudioRouteAllowed(mAttributionSource)
                        ? BluetoothStatusCodes.ALLOWED
                        : BluetoothStatusCodes.NOT_ALLOWED;
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }

        Log.e(TAG, "getAudioRouteAllowed: Bluetooth disabled, but profile service still bound");
        return BluetoothStatusCodes.ERROR_UNKNOWN;
    }

    /**
     * Force SCO audio to be opened regardless any other restrictions
     *
     * @param forced Whether or not SCO audio connection should be forced: True to force SCO audio
     *     False to use SCO audio in normal manner
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public void setForceScoAudio(boolean forced) {
        if (VDBG) log("setForceScoAudio " + String.valueOf(forced));
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                service.setForceScoAudio(forced, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                BluetoothStatusCodes.SUCCESS,
                BluetoothStatusCodes.ERROR_UNKNOWN,
                BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND,
                BluetoothStatusCodes.ERROR_TIMEOUT,
                BluetoothStatusCodes.ERROR_AUDIO_DEVICE_ALREADY_CONNECTED,
                BluetoothStatusCodes.ERROR_NO_ACTIVE_DEVICES,
                BluetoothStatusCodes.ERROR_NOT_ACTIVE_DEVICE,
                BluetoothStatusCodes.ERROR_AUDIO_ROUTE_BLOCKED,
                BluetoothStatusCodes.ERROR_CALL_ACTIVE,
                BluetoothStatusCodes.ERROR_PROFILE_NOT_CONNECTED
            })
    public @interface ConnectAudioReturnValues {}

    /**
     * Initiates a connection of SCO audio to the current active HFP device. The active HFP device
     * can be identified with {@link BluetoothAdapter#getActiveDevices(int)}.
     *
     * <p>If this function returns {@link BluetoothStatusCodes#SUCCESS}, the intent {@link
     * #ACTION_AUDIO_STATE_CHANGED} will be broadcasted twice. First with {@link #EXTRA_STATE} set
     * to {@link #STATE_AUDIO_CONNECTING}. This will be followed by a broadcast with {@link
     * #EXTRA_STATE} set to either {@link #STATE_AUDIO_CONNECTED} if the audio connection is
     * established or {@link #STATE_AUDIO_DISCONNECTED} if there was a failure in establishing the
     * audio connection.
     *
     * @return whether the connection was successfully initiated or an error code on failure
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    public @ConnectAudioReturnValues int connectAudio() {
        if (VDBG) log("connectAudio()");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
            return BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND;
        } else if (isEnabled()) {
            try {
                return service.connectAudio(mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }

        Log.e(TAG, "connectAudio: Bluetooth disabled, but profile service still bound");
        return BluetoothStatusCodes.ERROR_UNKNOWN;
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                BluetoothStatusCodes.SUCCESS,
                BluetoothStatusCodes.ERROR_UNKNOWN,
                BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND,
                BluetoothStatusCodes.ERROR_TIMEOUT,
                BluetoothStatusCodes.ERROR_PROFILE_NOT_CONNECTED,
                BluetoothStatusCodes.ERROR_AUDIO_DEVICE_ALREADY_DISCONNECTED
            })
    public @interface DisconnectAudioReturnValues {}

    /**
     * Initiates a disconnection of HFP SCO audio from actively connected devices. It also tears
     * down voice recognition or virtual voice call, if any exists.
     *
     * <p>If this function returns {@link BluetoothStatusCodes#SUCCESS}, the intent {@link
     * #ACTION_AUDIO_STATE_CHANGED} will be broadcasted with {@link #EXTRA_STATE} set to {@link
     * #STATE_AUDIO_DISCONNECTED}.
     *
     * @return whether the disconnection was initiated successfully or an error code on failure
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    public @DisconnectAudioReturnValues int disconnectAudio() {
        if (VDBG) log("disconnectAudio()");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
            return BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND;
        } else if (isEnabled()) {
            try {
                return service.disconnectAudio(mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }

        Log.e(TAG, "disconnectAudio: Bluetooth disabled, but profile service still bound");
        return BluetoothStatusCodes.ERROR_UNKNOWN;
    }

    /**
     * Initiates a SCO channel connection as a virtual voice call to the current active device
     * Active handsfree device will be notified of incoming call and connected call.
     *
     * <p>Users can listen to {@link #ACTION_AUDIO_STATE_CHANGED}. If this function returns true,
     * this intent will be broadcasted with {@link #EXTRA_STATE} set to {@link
     * #STATE_AUDIO_CONNECTING}.
     *
     * <p>{@link #EXTRA_STATE} will transition from {@link #STATE_AUDIO_CONNECTING} to {@link
     * #STATE_AUDIO_CONNECTED} when audio connection is established and to {@link
     * #STATE_AUDIO_DISCONNECTED} in case of failure to establish the audio connection.
     *
     * @return true if successful, false if one of the following case applies - SCO audio is not
     *     idle (connecting or connected) - virtual call has already started - there is no active
     *     device - a Telecom managed call is going on - binder is dead or Bluetooth is disabled or
     *     other error
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                MODIFY_PHONE_STATE,
                BLUETOOTH_PRIVILEGED,
            })
    public boolean startScoUsingVirtualVoiceCall() {
        if (DBG) log("startScoUsingVirtualVoiceCall()");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                return service.startScoUsingVirtualVoiceCall(mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /**
     * Terminates an ongoing SCO connection and the associated virtual call.
     *
     * <p>Users can listen to {@link #ACTION_AUDIO_STATE_CHANGED}. If this function returns true,
     * this intent will be broadcasted with {@link #EXTRA_STATE} set to {@link
     * #STATE_AUDIO_DISCONNECTED}.
     *
     * @return true if successful, false if one of the following case applies - virtual voice call
     *     is not started or has ended - binder is dead or Bluetooth is disabled or other error
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                MODIFY_PHONE_STATE,
                BLUETOOTH_PRIVILEGED,
            })
    public boolean stopScoUsingVirtualVoiceCall() {
        if (DBG) log("stopScoUsingVirtualVoiceCall()");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                return service.stopScoUsingVirtualVoiceCall(mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /**
     * Notify Headset of phone state change. This is a backdoor for phone app to call
     * BluetoothHeadset since there is currently not a good way to get precise call state change
     * outside of phone app.
     *
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                MODIFY_PHONE_STATE,
            })
    public void phoneStateChanged(
            int numActive, int numHeld, int callState, String number, int type, String name) {
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                service.phoneStateChanged(
                        numActive, numHeld, callState, number, type, name, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    /**
     * Sends a vendor-specific unsolicited result code to the headset.
     *
     * <p>The actual string to be sent is <code>command + ": " + arg</code>. For example, if {@code
     * command} is {@link #VENDOR_RESULT_CODE_COMMAND_ANDROID} and {@code arg} is {@code "0"}, the
     * string <code>"+ANDROID: 0"</code> will be sent.
     *
     * <p>Currently only {@link #VENDOR_RESULT_CODE_COMMAND_ANDROID} is allowed as {@code command}.
     *
     * @param device Bluetooth headset.
     * @param command A vendor-specific command.
     * @param arg The argument that will be attached to the command.
     * @return {@code false} if there is no headset connected, or if the command is not an allowed
     *     vendor-specific unsolicited result code, or on error. {@code true} otherwise.
     * @throws IllegalArgumentException if {@code command} is {@code null}.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean sendVendorSpecificResultCode(
            BluetoothDevice device, String command, String arg) {
        if (DBG) {
            log("sendVendorSpecificResultCode()");
        }
        if (command == null) {
            throw new IllegalArgumentException("command is null");
        }
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                return service.sendVendorSpecificResultCode(
                        device, command, arg, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /**
     * Select a connected device as active.
     *
     * <p>The active device selection is per profile. An active device's purpose is
     * profile-specific. For example, in HFP and HSP profiles, it is the device used for phone call
     * audio. If a remote device is not connected, it cannot be selected as active.
     *
     * <p>This API returns false in scenarios like the profile on the device is not connected or
     * Bluetooth is not turned on. When this API returns true, it is guaranteed that the {@link
     * #ACTION_ACTIVE_DEVICE_CHANGED} intent will be broadcasted with the active device.
     *
     * @param device Remote Bluetooth Device, could be null if phone call audio should not be
     *     streamed to a headset
     * @return false on immediate error, true otherwise
     * @hide
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                MODIFY_PHONE_STATE,
            })
    @UnsupportedAppUsage(trackingBug = 171933273)
    public boolean setActiveDevice(@Nullable BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "setActiveDevice: " + device);
        }
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && (device == null || isValidDevice(device))) {
            try {
                return service.setActiveDevice(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /**
     * Get the connected device that is active.
     *
     * @return the connected device that is active or null if no device is active.
     * @hide
     */
    @UnsupportedAppUsage(trackingBug = 171933273)
    @Nullable
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public BluetoothDevice getActiveDevice() {
        if (VDBG) Log.d(TAG, "getActiveDevice");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                return Attributable.setAttributionSource(
                        service.getActiveDevice(mAttributionSource), mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return null;
    }

    /**
     * Check if in-band ringing is currently enabled. In-band ringing could be disabled during an
     * active connection.
     *
     * @return true if in-band ringing is enabled, false if in-band ringing is disabled
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    public boolean isInbandRingingEnabled() {
        if (DBG) log("isInbandRingingEnabled()");
        final IBluetoothHeadset service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                return service.isInbandRingingEnabled(mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    @UnsupportedAppUsage
    private boolean isEnabled() {
        return mAdapter.getState() == BluetoothAdapter.STATE_ON;
    }

    private boolean isDisabled() {
        return mAdapter.getState() == BluetoothAdapter.STATE_OFF;
    }

    private static boolean isValidDevice(BluetoothDevice device) {
        return device != null && BluetoothAdapter.checkBluetoothAddress(device.getAddress());
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
