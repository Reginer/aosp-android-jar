/*
 * Copyright 2009-2016 The Android Open Source Project
 * Copyright 2015 Samsung LSI
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

import static java.util.Objects.requireNonNull;

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice.AddressType;
import android.bluetooth.BluetoothDevice.Transport;
import android.bluetooth.BluetoothProfile.ConnectionPolicy;
import android.bluetooth.annotations.RequiresBluetoothAdvertisePermission;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.bluetooth.annotations.RequiresBluetoothLocationPermission;
import android.bluetooth.annotations.RequiresBluetoothScanPermission;
import android.bluetooth.annotations.RequiresLegacyBluetoothAdminPermission;
import android.bluetooth.annotations.RequiresLegacyBluetoothPermission;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.DistanceMeasurementManager;
import android.bluetooth.le.PeriodicAdvertisingManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.AttributionSource;
import android.content.Context;
import android.os.Binder;
import android.os.BluetoothServiceManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IpcDataCache;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.sysprop.BluetoothProperties;
import android.util.Log;
import android.util.Pair;

import com.android.internal.annotations.GuardedBy;
import com.android.modules.utils.SynchronousResultReceiver;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents the local device Bluetooth adapter. The {@link BluetoothAdapter}
 * lets you perform fundamental Bluetooth tasks, such as initiate
 * device discovery, query a list of bonded (paired) devices,
 * instantiate a {@link BluetoothDevice} using a known MAC address, and create
 * a {@link BluetoothServerSocket} to listen for connection requests from other
 * devices, and start a scan for Bluetooth LE devices.
 *
 * <p>To get a {@link BluetoothAdapter} representing the local Bluetooth
 * adapter, call the {@link BluetoothManager#getAdapter} function on {@link BluetoothManager}.
 * On JELLY_BEAN_MR1 and below you will need to use the static {@link #getDefaultAdapter}
 * method instead.
 * </p><p>
 * Fundamentally, this is your starting point for all
 * Bluetooth actions. Once you have the local adapter, you can get a set of
 * {@link BluetoothDevice} objects representing all paired devices with
 * {@link #getBondedDevices()}; start device discovery with
 * {@link #startDiscovery()}; or create a {@link BluetoothServerSocket} to
 * listen for incoming RFComm connection requests with {@link
 * #listenUsingRfcommWithServiceRecord(String, UUID)}; listen for incoming L2CAP Connection-oriented
 * Channels (CoC) connection requests with {@link #listenUsingL2capChannel()}; or start a scan for
 * Bluetooth LE devices with {@link #startLeScan(LeScanCallback callback)}.
 * </p>
 * <p>This class is thread safe.</p>
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>
 * For more information about using Bluetooth, read the <a href=
 * "{@docRoot}guide/topics/connectivity/bluetooth.html">Bluetooth</a> developer
 * guide.
 * </p>
 * </div>
 *
 * {@see BluetoothDevice}
 * {@see BluetoothServerSocket}
 */
public final class BluetoothAdapter {
    private static final String TAG = "BluetoothAdapter";
    private static final String DESCRIPTOR = "android.bluetooth.BluetoothAdapter";
    private static final boolean DBG = true;
    private static final boolean VDBG = false;

    /**
     * Default MAC address reported to a client that does not have the
     * {@link android.Manifest.permission#LOCAL_MAC_ADDRESS} permission.
     *
     *
     * @hide
     */
    public static final String DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";

    /**
     * Sentinel error value for this class. Guaranteed to not equal any other
     * integer constant in this class. Provided as a convenience for functions
     * that require a sentinel error value, for example:
     * <p><code>Intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
     * BluetoothAdapter.ERROR)</code>
     */
    public static final int ERROR = Integer.MIN_VALUE;

    /**
     * Broadcast Action: The state of the local Bluetooth adapter has been
     * changed.
     * <p>For example, Bluetooth has been turned on or off.
     * <p>Always contains the extra fields {@link #EXTRA_STATE} and {@link
     * #EXTRA_PREVIOUS_STATE} containing the new and old states
     * respectively.
     */
    @RequiresLegacyBluetoothPermission
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION) public static final String
            ACTION_STATE_CHANGED = "android.bluetooth.adapter.action.STATE_CHANGED";

    /**
     * Used as an int extra field in {@link #ACTION_STATE_CHANGED}
     * intents to request the current power state. Possible values are:
     * {@link #STATE_OFF},
     * {@link #STATE_TURNING_ON},
     * {@link #STATE_ON},
     * {@link #STATE_TURNING_OFF},
     */
    public static final String EXTRA_STATE = "android.bluetooth.adapter.extra.STATE";
    /**
     * Used as an int extra field in {@link #ACTION_STATE_CHANGED}
     * intents to request the previous power state. Possible values are:
     * {@link #STATE_OFF},
     * {@link #STATE_TURNING_ON},
     * {@link #STATE_ON},
     * {@link #STATE_TURNING_OFF}
     */
    public static final String EXTRA_PREVIOUS_STATE =
            "android.bluetooth.adapter.extra.PREVIOUS_STATE";

    /** @hide */
    @IntDef(prefix = { "STATE_" }, value = {
            STATE_OFF,
            STATE_TURNING_ON,
            STATE_ON,
            STATE_TURNING_OFF,
            STATE_BLE_TURNING_ON,
            STATE_BLE_ON,
            STATE_BLE_TURNING_OFF
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface InternalAdapterState {}

    /** @hide */
    @IntDef(prefix = { "STATE_" }, value = {
            STATE_OFF,
            STATE_TURNING_ON,
            STATE_ON,
            STATE_TURNING_OFF,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AdapterState {}

    /**
     * Indicates the local Bluetooth adapter is off.
     */
    public static final int STATE_OFF = 10;
    /**
     * Indicates the local Bluetooth adapter is turning on. However local
     * clients should wait for {@link #STATE_ON} before attempting to
     * use the adapter.
     */
    public static final int STATE_TURNING_ON = 11;
    /**
     * Indicates the local Bluetooth adapter is on, and ready for use.
     */
    public static final int STATE_ON = 12;
    /**
     * Indicates the local Bluetooth adapter is turning off. Local clients
     * should immediately attempt graceful disconnection of any remote links.
     */
    public static final int STATE_TURNING_OFF = 13;

    /**
     * Indicates the local Bluetooth adapter is turning Bluetooth LE mode on.
     *
     * @hide
     */
    public static final int STATE_BLE_TURNING_ON = 14;

    /**
     * Indicates the local Bluetooth adapter is in LE only mode.
     *
     * @hide
     */
    @SystemApi
    public static final int STATE_BLE_ON = 15;

    /**
     * Indicates the local Bluetooth adapter is turning off LE only mode.
     *
     * @hide
     */
    public static final int STATE_BLE_TURNING_OFF = 16;

    /**
     * UUID of the GATT Read Characteristics for LE_PSM value.
     *
     * @hide
     */
    public static final UUID LE_PSM_CHARACTERISTIC_UUID =
            UUID.fromString("2d410339-82b6-42aa-b34e-e2e01df8cc1a");

    /**
     * Used as an optional extra field for the {@link PendingIntent} provided to {@link
     * #startRfcommServer(String, UUID, PendingIntent)}. This is useful for when an
     * application registers multiple RFCOMM listeners, and needs a way to determine which service
     * record the incoming {@link BluetoothSocket} is using.
     *
     * @hide
     */
    @SystemApi
    @SuppressLint("ActionValue")
    public static final String EXTRA_RFCOMM_LISTENER_ID =
            "android.bluetooth.adapter.extra.RFCOMM_LISTENER_ID";

    /** @hide */
    @IntDef(value = {
            BluetoothStatusCodes.SUCCESS,
            BluetoothStatusCodes.ERROR_TIMEOUT,
            BluetoothStatusCodes.RFCOMM_LISTENER_START_FAILED_UUID_IN_USE,
            BluetoothStatusCodes.RFCOMM_LISTENER_OPERATION_FAILED_NO_MATCHING_SERVICE_RECORD,
            BluetoothStatusCodes.RFCOMM_LISTENER_OPERATION_FAILED_DIFFERENT_APP,
            BluetoothStatusCodes.RFCOMM_LISTENER_FAILED_TO_CREATE_SERVER_SOCKET,
            BluetoothStatusCodes.RFCOMM_LISTENER_FAILED_TO_CLOSE_SERVER_SOCKET,
            BluetoothStatusCodes.RFCOMM_LISTENER_NO_SOCKET_AVAILABLE,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RfcommListenerResult {}

    /**
     * Human-readable string helper for AdapterState and InternalAdapterState
     *
     * @hide
     */
    @SystemApi
    @RequiresNoPermission
    @NonNull
    public static String nameForState(@InternalAdapterState int state) {
        switch (state) {
            case STATE_OFF:
                return "OFF";
            case STATE_TURNING_ON:
                return "TURNING_ON";
            case STATE_ON:
                return "ON";
            case STATE_TURNING_OFF:
                return "TURNING_OFF";
            case STATE_BLE_TURNING_ON:
                return "BLE_TURNING_ON";
            case STATE_BLE_ON:
                return "BLE_ON";
            case STATE_BLE_TURNING_OFF:
                return "BLE_TURNING_OFF";
            default:
                return "?!?!? (" + state + ")";
        }
    }

    /**
     * Activity Action: Show a system activity that requests discoverable mode.
     * This activity will also request the user to turn on Bluetooth if it
     * is not currently enabled.
     * <p>Discoverable mode is equivalent to {@link
     * #SCAN_MODE_CONNECTABLE_DISCOVERABLE}. It allows remote devices to see
     * this Bluetooth adapter when they perform a discovery.
     * <p>For privacy, Android is not discoverable by default.
     * <p>The sender of this Intent can optionally use extra field {@link
     * #EXTRA_DISCOVERABLE_DURATION} to request the duration of
     * discoverability. Currently the default duration is 120 seconds, and
     * maximum duration is capped at 300 seconds for each request.
     * <p>Notification of the result of this activity is posted using the
     * {@link android.app.Activity#onActivityResult} callback. The
     * <code>resultCode</code>
     * will be the duration (in seconds) of discoverability or
     * {@link android.app.Activity#RESULT_CANCELED} if the user rejected
     * discoverability or an error has occurred.
     * <p>Applications can also listen for {@link #ACTION_SCAN_MODE_CHANGED}
     * for global notification whenever the scan mode changes. For example, an
     * application can be notified when the device has ended discoverability.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothAdvertisePermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE)
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION) public static final String
            ACTION_REQUEST_DISCOVERABLE = "android.bluetooth.adapter.action.REQUEST_DISCOVERABLE";

    /**
     * Used as an optional int extra field in {@link
     * #ACTION_REQUEST_DISCOVERABLE} intents to request a specific duration
     * for discoverability in seconds. The current default is 120 seconds, and
     * requests over 300 seconds will be capped. These values could change.
     */
    public static final String EXTRA_DISCOVERABLE_DURATION =
            "android.bluetooth.adapter.extra.DISCOVERABLE_DURATION";

    /**
     * Activity Action: Show a system activity that allows the user to turn on
     * Bluetooth.
     * <p>This system activity will return once Bluetooth has completed turning
     * on, or the user has decided not to turn Bluetooth on.
     * <p>Notification of the result of this activity is posted using the
     * {@link android.app.Activity#onActivityResult} callback. The
     * <code>resultCode</code>
     * will be {@link android.app.Activity#RESULT_OK} if Bluetooth has been
     * turned on or {@link android.app.Activity#RESULT_CANCELED} if the user
     * has rejected the request or an error has occurred.
     * <p>Applications can also listen for {@link #ACTION_STATE_CHANGED}
     * for global notification whenever Bluetooth is turned on or off.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION) public static final String
            ACTION_REQUEST_ENABLE = "android.bluetooth.adapter.action.REQUEST_ENABLE";

    /**
     * Activity Action: Show a system activity that allows the user to turn off
     * Bluetooth. This is used only if permission review is enabled which is for
     * apps targeting API less than 23 require a permission review before any of
     * the app's components can run.
     * <p>This system activity will return once Bluetooth has completed turning
     * off, or the user has decided not to turn Bluetooth off.
     * <p>Notification of the result of this activity is posted using the
     * {@link android.app.Activity#onActivityResult} callback. The
     * <code>resultCode</code>
     * will be {@link android.app.Activity#RESULT_OK} if Bluetooth has been
     * turned off or {@link android.app.Activity#RESULT_CANCELED} if the user
     * has rejected the request or an error has occurred.
     * <p>Applications can also listen for {@link #ACTION_STATE_CHANGED}
     * for global notification whenever Bluetooth is turned on or off.
     *
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    @SuppressLint("ActionValue")
    public static final String
            ACTION_REQUEST_DISABLE = "android.bluetooth.adapter.action.REQUEST_DISABLE";

    /**
     * Activity Action: Show a system activity that allows user to enable BLE scans even when
     * Bluetooth is turned off.<p>
     *
     * Notification of result of this activity is posted using
     * {@link android.app.Activity#onActivityResult}. The <code>resultCode</code> will be
     * {@link android.app.Activity#RESULT_OK} if BLE scan always available setting is turned on or
     * {@link android.app.Activity#RESULT_CANCELED} if the user has rejected the request or an
     * error occurred.
     *
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_REQUEST_BLE_SCAN_ALWAYS_AVAILABLE =
            "android.bluetooth.adapter.action.REQUEST_BLE_SCAN_ALWAYS_AVAILABLE";

    /**
     * Broadcast Action: Indicates the Bluetooth scan mode of the local Adapter
     * has changed.
     * <p>Always contains the extra fields {@link #EXTRA_SCAN_MODE} and {@link
     * #EXTRA_PREVIOUS_SCAN_MODE} containing the new and old scan modes
     * respectively.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothScanPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION) public static final String
            ACTION_SCAN_MODE_CHANGED = "android.bluetooth.adapter.action.SCAN_MODE_CHANGED";

    /**
     * Used as an int extra field in {@link #ACTION_SCAN_MODE_CHANGED}
     * intents to request the current scan mode. Possible values are:
     * {@link #SCAN_MODE_NONE},
     * {@link #SCAN_MODE_CONNECTABLE},
     * {@link #SCAN_MODE_CONNECTABLE_DISCOVERABLE},
     */
    public static final String EXTRA_SCAN_MODE = "android.bluetooth.adapter.extra.SCAN_MODE";
    /**
     * Used as an int extra field in {@link #ACTION_SCAN_MODE_CHANGED}
     * intents to request the previous scan mode. Possible values are:
     * {@link #SCAN_MODE_NONE},
     * {@link #SCAN_MODE_CONNECTABLE},
     * {@link #SCAN_MODE_CONNECTABLE_DISCOVERABLE},
     */
    public static final String EXTRA_PREVIOUS_SCAN_MODE =
            "android.bluetooth.adapter.extra.PREVIOUS_SCAN_MODE";

    /** @hide */
    @IntDef(prefix = { "SCAN_" }, value = {
            SCAN_MODE_NONE,
            SCAN_MODE_CONNECTABLE,
            SCAN_MODE_CONNECTABLE_DISCOVERABLE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScanMode {}

    /** @hide */
    @IntDef(value = {
            BluetoothStatusCodes.SUCCESS,
            BluetoothStatusCodes.ERROR_UNKNOWN,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED,
            BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_SCAN_PERMISSION
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScanModeStatusCode {}

    /**
     * Indicates that both inquiry scan and page scan are disabled on the local
     * Bluetooth adapter. Therefore this device is neither discoverable
     * nor connectable from remote Bluetooth devices.
     */
    public static final int SCAN_MODE_NONE = 20;
    /**
     * Indicates that inquiry scan is disabled, but page scan is enabled on the
     * local Bluetooth adapter. Therefore this device is not discoverable from
     * remote Bluetooth devices, but is connectable from remote devices that
     * have previously discovered this device.
     */
    public static final int SCAN_MODE_CONNECTABLE = 21;
    /**
     * Indicates that both inquiry scan and page scan are enabled on the local
     * Bluetooth adapter. Therefore this device is both discoverable and
     * connectable from remote Bluetooth devices.
     */
    public static final int SCAN_MODE_CONNECTABLE_DISCOVERABLE = 23;


    /**
     * Used as parameter for {@link #setBluetoothHciSnoopLoggingMode}, indicates that
     * the Bluetooth HCI snoop logging should be disabled.
     *
     * @hide
     */
    @SystemApi
    public static final int BT_SNOOP_LOG_MODE_DISABLED = 0;

    /**
     * Used as parameter for {@link #setBluetoothHciSnoopLoggingMode}, indicates that
     * the Bluetooth HCI snoop logging should be enabled without collecting potential
     * Personally Identifiable Information and packet data.
     *
     * See {@link #BT_SNOOP_LOG_MODE_FULL} to enable logging of all information available.
     *
     * @hide
     */
    @SystemApi
    public static final int BT_SNOOP_LOG_MODE_FILTERED = 1;

    /**
     * Used as parameter for {@link #setSnoopLogMode}, indicates that the Bluetooth HCI snoop
     * logging should be enabled.
     *
     * See {@link #BT_SNOOP_LOG_MODE_FILTERED} to enable logging with filtered information.
     *
     * @hide
     */
    @SystemApi
    public static final int BT_SNOOP_LOG_MODE_FULL = 2;

    /** @hide */
    @IntDef(value = {
            BT_SNOOP_LOG_MODE_DISABLED,
            BT_SNOOP_LOG_MODE_FILTERED,
            BT_SNOOP_LOG_MODE_FULL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface BluetoothSnoopLogMode {}

    /** @hide */
    @IntDef(value = {
            BluetoothStatusCodes.SUCCESS,
            BluetoothStatusCodes.ERROR_UNKNOWN,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SetSnoopLogModeStatusCode {}

    /**
     * Device only has a display.
     *
     * @hide
     */
    public static final int IO_CAPABILITY_OUT = 0;

    /**
     * Device has a display and the ability to input Yes/No.
     *
     * @hide
     */
    public static final int IO_CAPABILITY_IO = 1;

    /**
     * Device only has a keyboard for entry but no display.
     *
     * @hide
     */
    public static final int IO_CAPABILITY_IN = 2;

    /**
     * Device has no Input or Output capability.
     *
     * @hide
     */
    public static final int IO_CAPABILITY_NONE = 3;

    /**
     * Device has a display and a full keyboard.
     *
     * @hide
     */
    public static final int IO_CAPABILITY_KBDISP = 4;

    /**
     * Maximum range value for Input/Output capabilities.
     *
     * <p>This should be updated when adding a new Input/Output capability. Other code
     * like validation depends on this being accurate.
     *
     * @hide
     */
    public static final int IO_CAPABILITY_MAX = 5;

    /**
     * The Input/Output capability of the device is unknown.
     *
     * @hide
     */
    public static final int IO_CAPABILITY_UNKNOWN = 255;

    /** @hide */
    @IntDef({IO_CAPABILITY_OUT, IO_CAPABILITY_IO, IO_CAPABILITY_IN, IO_CAPABILITY_NONE,
            IO_CAPABILITY_KBDISP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface IoCapability {}

    /** @hide */
    @IntDef(prefix = "ACTIVE_DEVICE_", value = {ACTIVE_DEVICE_AUDIO,
            ACTIVE_DEVICE_PHONE_CALL, ACTIVE_DEVICE_ALL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActiveDeviceUse {}

    /**
     * Use the specified device for audio (a2dp and hearing aid profile)
     *
     * @hide
     */
    @SystemApi
    public static final int ACTIVE_DEVICE_AUDIO = 0;

    /**
     * Use the specified device for phone calls (headset profile and hearing
     * aid profile)
     *
     * @hide
     */
    @SystemApi
    public static final int ACTIVE_DEVICE_PHONE_CALL = 1;

    /**
     * Use the specified device for a2dp, hearing aid profile, and headset profile
     *
     * @hide
     */
    @SystemApi
    public static final int ACTIVE_DEVICE_ALL = 2;

    /** @hide */
    @IntDef({BluetoothProfile.HEADSET, BluetoothProfile.A2DP,
            BluetoothProfile.HEARING_AID})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActiveDeviceProfile {}

    /**
     * Broadcast Action: The local Bluetooth adapter has started the remote
     * device discovery process.
     * <p>This usually involves an inquiry scan of about 12 seconds, followed
     * by a page scan of each new device to retrieve its Bluetooth name.
     * <p>Register for {@link BluetoothDevice#ACTION_FOUND} to be notified as
     * remote Bluetooth devices are found.
     * <p>Device discovery is a heavyweight procedure. New connections to
     * remote Bluetooth devices should not be attempted while discovery is in
     * progress, and existing connections will experience limited bandwidth
     * and high latency. Use {@link #cancelDiscovery()} to cancel an ongoing
     * discovery.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothScanPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION) public static final String
            ACTION_DISCOVERY_STARTED = "android.bluetooth.adapter.action.DISCOVERY_STARTED";
    /**
     * Broadcast Action: The local Bluetooth adapter has finished the device
     * discovery process.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothScanPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION) public static final String
            ACTION_DISCOVERY_FINISHED = "android.bluetooth.adapter.action.DISCOVERY_FINISHED";

    /**
     * Broadcast Action: The local Bluetooth adapter has changed its friendly
     * Bluetooth name.
     * <p>This name is visible to remote Bluetooth devices.
     * <p>Always contains the extra field {@link #EXTRA_LOCAL_NAME} containing
     * the name.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION) public static final String
            ACTION_LOCAL_NAME_CHANGED = "android.bluetooth.adapter.action.LOCAL_NAME_CHANGED";
    /**
     * Used as a String extra field in {@link #ACTION_LOCAL_NAME_CHANGED}
     * intents to request the local Bluetooth name.
     */
    public static final String EXTRA_LOCAL_NAME = "android.bluetooth.adapter.extra.LOCAL_NAME";

    /**
     * Intent used to broadcast the change in connection state of the local
     * Bluetooth adapter to a profile of the remote device. When the adapter is
     * not connected to any profiles of any remote devices and it attempts a
     * connection to a profile this intent will be sent. Once connected, this intent
     * will not be sent for any more connection attempts to any profiles of any
     * remote device. When the adapter disconnects from the last profile its
     * connected to of any remote device, this intent will be sent.
     *
     * <p> This intent is useful for applications that are only concerned about
     * whether the local adapter is connected to any profile of any device and
     * are not really concerned about which profile. For example, an application
     * which displays an icon to display whether Bluetooth is connected or not
     * can use this intent.
     *
     * <p>This intent will have 3 extras:
     * {@link #EXTRA_CONNECTION_STATE} - The current connection state.
     * {@link #EXTRA_PREVIOUS_CONNECTION_STATE}- The previous connection state.
     * {@link BluetoothDevice#EXTRA_DEVICE} - The remote device.
     *
     * {@link #EXTRA_CONNECTION_STATE} or {@link #EXTRA_PREVIOUS_CONNECTION_STATE}
     * can be any of {@link #STATE_DISCONNECTED}, {@link #STATE_CONNECTING},
     * {@link #STATE_CONNECTED}, {@link #STATE_DISCONNECTING}.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION) public static final String
            ACTION_CONNECTION_STATE_CHANGED =
            "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED";

    /**
     * Extra used by {@link #ACTION_CONNECTION_STATE_CHANGED}
     *
     * This extra represents the current connection state.
     */
    public static final String EXTRA_CONNECTION_STATE =
            "android.bluetooth.adapter.extra.CONNECTION_STATE";

    /**
     * Extra used by {@link #ACTION_CONNECTION_STATE_CHANGED}
     *
     * This extra represents the previous connection state.
     */
    public static final String EXTRA_PREVIOUS_CONNECTION_STATE =
            "android.bluetooth.adapter.extra.PREVIOUS_CONNECTION_STATE";

    /**
     * Broadcast Action: The Bluetooth adapter state has changed in LE only mode.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @SystemApi public static final String ACTION_BLE_STATE_CHANGED =
            "android.bluetooth.adapter.action.BLE_STATE_CHANGED";

    /**
     * Intent used to broadcast the change in the Bluetooth address
     * of the local Bluetooth adapter.
     * <p>Always contains the extra field {@link
     * #EXTRA_BLUETOOTH_ADDRESS} containing the Bluetooth address.
     *
     * Note: only system level processes are allowed to send this
     * defined broadcast.
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_BLUETOOTH_ADDRESS_CHANGED =
            "android.bluetooth.adapter.action.BLUETOOTH_ADDRESS_CHANGED";

    /**
     * Used as a String extra field in {@link
     * #ACTION_BLUETOOTH_ADDRESS_CHANGED} intent to store the local
     * Bluetooth address.
     *
     * @hide
     */
    public static final String EXTRA_BLUETOOTH_ADDRESS =
            "android.bluetooth.adapter.extra.BLUETOOTH_ADDRESS";

    /**
     * Broadcast Action: The notifys Bluetooth ACL connected event. This will be
     * by BLE Always on enabled application to know the ACL_CONNECTED event
     * when Bluetooth state in STATE_BLE_ON. This denotes GATT connection
     * as Bluetooth LE is the only feature available in STATE_BLE_ON
     *
     * This is counterpart of {@link BluetoothDevice#ACTION_ACL_CONNECTED} which
     * works in Bluetooth state STATE_ON
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_BLE_ACL_CONNECTED =
            "android.bluetooth.adapter.action.BLE_ACL_CONNECTED";

    /**
     * Broadcast Action: The notifys Bluetooth ACL connected event. This will be
     * by BLE Always on enabled application to know the ACL_DISCONNECTED event
     * when Bluetooth state in STATE_BLE_ON. This denotes GATT disconnection as Bluetooth
     * LE is the only feature available in STATE_BLE_ON
     *
     * This is counterpart of {@link BluetoothDevice#ACTION_ACL_DISCONNECTED} which
     * works in Bluetooth state STATE_ON
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_BLE_ACL_DISCONNECTED =
            "android.bluetooth.adapter.action.BLE_ACL_DISCONNECTED";

    /** The profile is in disconnected state */
    public static final int STATE_DISCONNECTED =
            0; //BluetoothProtoEnums.CONNECTION_STATE_DISCONNECTED;
    /** The profile is in connecting state */
    public static final int STATE_CONNECTING = 1; //BluetoothProtoEnums.CONNECTION_STATE_CONNECTING;
    /** The profile is in connected state */
    public static final int STATE_CONNECTED = 2; //BluetoothProtoEnums.CONNECTION_STATE_CONNECTED;
    /** The profile is in disconnecting state */
    public static final int STATE_DISCONNECTING =
            3; //BluetoothProtoEnums.CONNECTION_STATE_DISCONNECTING;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "STATE_" }, value = {
        STATE_DISCONNECTED,
        STATE_CONNECTING,
        STATE_CONNECTED,
        STATE_DISCONNECTING,
    })
    public @interface ConnectionState {}

    /**
     * Audio mode representing output only.
     * @hide
     */
    @SystemApi
    public static final String AUDIO_MODE_OUTPUT_ONLY = "audio_mode_output_only";

    /**
     * Audio mode representing both output and microphone input.
     * @hide
     */
    @SystemApi
    public static final String AUDIO_MODE_DUPLEX = "audio_mode_duplex";

    /** @hide */
    public static final String BLUETOOTH_MANAGER_SERVICE = "bluetooth_manager";
    private final IBinder mToken;


    /**
     * When creating a ServerSocket using listenUsingRfcommOn() or
     * listenUsingL2capOn() use SOCKET_CHANNEL_AUTO_STATIC to create
     * a ServerSocket that auto assigns a channel number to the first
     * bluetooth socket.
     * The channel number assigned to this first Bluetooth Socket will
     * be stored in the ServerSocket, and reused for subsequent Bluetooth
     * sockets.
     *
     * @hide
     */
    public static final int SOCKET_CHANNEL_AUTO_STATIC_NO_SDP = -2;


    private static final int ADDRESS_LENGTH = 17;

    /**
     * Lazily initialized singleton. Guaranteed final after first object
     * constructed.
     */
    private static BluetoothAdapter sAdapter;

    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private PeriodicAdvertisingManager mPeriodicAdvertisingManager;
    private DistanceMeasurementManager mDistanceMeasurementManager;

    private final IBluetoothManager mManagerService;
    private final AttributionSource mAttributionSource;

    // Yeah, keeping both mService and sService isn't pretty, but it's too late
    // in the current release for a major refactoring, so we leave them both
    // intact until this can be cleaned up in a future release

    @UnsupportedAppUsage
    @GuardedBy("mServiceLock")
    private IBluetooth mService;
    private final ReentrantReadWriteLock mServiceLock = new ReentrantReadWriteLock();

    @GuardedBy("sServiceLock")
    private static boolean sServiceRegistered;
    @GuardedBy("sServiceLock")
    private static IBluetooth sService;
    private static final Object sServiceLock = new Object();

    private final Object mLock = new Object();
    private final Map<LeScanCallback, ScanCallback> mLeScanClients;
    private final Map<BluetoothDevice, List<Pair<OnMetadataChangedListener, Executor>>>
                mMetadataListeners = new HashMap<>();
    private final Map<BluetoothConnectionCallback, Executor>
            mBluetoothConnectionCallbackExecutorMap = new HashMap<>();
    private final Map<PreferredAudioProfilesChangedCallback, Executor>
            mAudioProfilesChangedCallbackExecutorMap = new HashMap<>();
    private final Map<BluetoothQualityReportReadyCallback, Executor>
            mBluetoothQualityReportReadyCallbackExecutorMap = new HashMap<>();

    /**
     * Bluetooth metadata listener. Overrides the default BluetoothMetadataListener
     * implementation.
     */
    @SuppressLint("AndroidFrameworkBluetoothPermission")
    private final IBluetoothMetadataListener mBluetoothMetadataListener =
            new IBluetoothMetadataListener.Stub() {
        @Override
        public void onMetadataChanged(BluetoothDevice device, int key, byte[] value) {
            Attributable.setAttributionSource(device, mAttributionSource);
            synchronized (mMetadataListeners) {
                if (mMetadataListeners.containsKey(device)) {
                    List<Pair<OnMetadataChangedListener, Executor>> list =
                            mMetadataListeners.get(device);
                    for (Pair<OnMetadataChangedListener, Executor> pair : list) {
                        OnMetadataChangedListener listener = pair.first;
                        Executor executor = pair.second;
                        executor.execute(() -> {
                            listener.onMetadataChanged(device, key, value);
                        });
                    }
                }
            }
            return;
        }
    };

    /** @hide */
    @IntDef(value = {
            BluetoothStatusCodes.ERROR_UNKNOWN,
            BluetoothStatusCodes.FEATURE_NOT_SUPPORTED,
            BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface BluetoothActivityEnergyInfoCallbackError {}

    /**
     * Interface for Bluetooth activity energy info callback. Should be implemented by applications
     * and set when calling {@link #requestControllerActivityEnergyInfo}.
     *
     * @hide
     */
    @SystemApi
    public interface OnBluetoothActivityEnergyInfoCallback {
        /**
         * Called when Bluetooth activity energy info is available.
         * Note: this callback is triggered at most once for each call to
         * {@link #requestControllerActivityEnergyInfo}.
         *
         * @param info the latest {@link BluetoothActivityEnergyInfo}
         */
        void onBluetoothActivityEnergyInfoAvailable(
                @NonNull BluetoothActivityEnergyInfo info);

        /**
         * Called when the latest {@link BluetoothActivityEnergyInfo} can't be retrieved.
         * The reason of the failure is indicated by the {@link BluetoothStatusCodes}
         * passed as an argument to this method.
         * Note: this callback is triggered at most once for each call to
         * {@link #requestControllerActivityEnergyInfo}.
         *
         * @param error code indicating the reason for the failure
         */
        void onBluetoothActivityEnergyInfoError(
                @BluetoothActivityEnergyInfoCallbackError int error);
    }

    private static class OnBluetoothActivityEnergyInfoProxy
            extends IBluetoothActivityEnergyInfoListener.Stub {
        private final Object mLock = new Object();
        @Nullable @GuardedBy("mLock") private Executor mExecutor;
        @Nullable @GuardedBy("mLock") private OnBluetoothActivityEnergyInfoCallback mCallback;

        OnBluetoothActivityEnergyInfoProxy(Executor executor,
                OnBluetoothActivityEnergyInfoCallback callback) {
            mExecutor = executor;
            mCallback = callback;
        }

        @Override
        public void onBluetoothActivityEnergyInfoAvailable(BluetoothActivityEnergyInfo info) {
            Executor executor;
            OnBluetoothActivityEnergyInfoCallback callback;
            synchronized (mLock) {
                if (mExecutor == null || mCallback == null) {
                    return;
                }
                executor = mExecutor;
                callback = mCallback;
                mExecutor = null;
                mCallback = null;
            }
            final long identity = Binder.clearCallingIdentity();
            try {
                if (info == null) {
                    executor.execute(() -> callback.onBluetoothActivityEnergyInfoError(
                            BluetoothStatusCodes.FEATURE_NOT_SUPPORTED));
                } else {
                    executor.execute(() -> callback.onBluetoothActivityEnergyInfoAvailable(info));
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /**
         * Framework only method that is called when the service can't be reached.
         */
        public void onError(int errorCode) {
            Executor executor;
            OnBluetoothActivityEnergyInfoCallback callback;
            synchronized (mLock) {
                if (mExecutor == null || mCallback == null) {
                    return;
                }
                executor = mExecutor;
                callback = mCallback;
                mExecutor = null;
                mCallback = null;
            }
            final long identity = Binder.clearCallingIdentity();
            try {
                executor.execute(() -> callback.onBluetoothActivityEnergyInfoError(
                        errorCode));
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    /**
     * Get a handle to the default local Bluetooth adapter.
     * <p>
     * Currently Android only supports one Bluetooth adapter, but the API could
     * be extended to support more. This will always return the default adapter.
     * </p>
     *
     * @return the default local adapter, or null if Bluetooth is not supported
     *         on this hardware platform
     * @deprecated this method will continue to work, but developers are
     *             strongly encouraged to migrate to using
     *             {@link BluetoothManager#getAdapter()}, since that approach
     *             enables support for {@link Context#createAttributionContext}.
     */
    @Deprecated
    @RequiresNoPermission
    public static synchronized BluetoothAdapter getDefaultAdapter() {
        if (sAdapter == null) {
            sAdapter = createAdapter(AttributionSource.myAttributionSource());
        }
        return sAdapter;
    }

    /** {@hide} */
    public static BluetoothAdapter createAdapter(AttributionSource attributionSource) {
        BluetoothServiceManager manager =
                BluetoothFrameworkInitializer.getBluetoothServiceManager();
        if (manager == null) {
            Log.e(TAG, "BluetoothServiceManager is null");
            return null;
        }
        IBluetoothManager service = IBluetoothManager.Stub.asInterface(
                manager.getBluetoothManagerServiceRegisterer().get());
        if (service != null) {
            return new BluetoothAdapter(service, attributionSource);
        } else {
            Log.e(TAG, "Bluetooth service is null");
            return null;
        }
    }

    /**
     * Use {@link #getDefaultAdapter} to get the BluetoothAdapter instance.
     */
    BluetoothAdapter(IBluetoothManager managerService, AttributionSource attributionSource) {
        mManagerService = requireNonNull(managerService);
        mAttributionSource = requireNonNull(attributionSource);
        mServiceLock.writeLock().lock();
        try {
            mService = getBluetoothService(mManagerCallback);
        } finally {
            mServiceLock.writeLock().unlock();
        }
        mLeScanClients = new HashMap<LeScanCallback, ScanCallback>();
        mToken = new Binder(DESCRIPTOR);
    }

    /**
     * Get a {@link BluetoothDevice} object for the given Bluetooth hardware
     * address.
     * <p>Valid Bluetooth hardware addresses must be upper case, in big endian byte order, and in a
     * format such as "00:11:22:33:AA:BB". The helper {@link #checkBluetoothAddress} is
     * available to validate a Bluetooth address.
     * <p>A {@link BluetoothDevice} will always be returned for a valid
     * hardware address, even if this adapter has never seen that device.
     *
     * @param address valid Bluetooth MAC address
     * @throws IllegalArgumentException if address is invalid
     */
    @RequiresNoPermission
    public BluetoothDevice getRemoteDevice(String address) {
        final BluetoothDevice res = new BluetoothDevice(address);
        res.setAttributionSource(mAttributionSource);
        return res;
    }

    /**
     * Get a {@link BluetoothDevice} object for the given Bluetooth hardware
     * address and addressType.
     * <p>Valid Bluetooth hardware addresses must be upper case, in big endian byte order, and in a
     * format such as "00:11:22:33:AA:BB". The helper {@link #checkBluetoothAddress} is
     * available to validate a Bluetooth address.
     * <p>A {@link BluetoothDevice} will always be returned for a valid
     * hardware address and type, even if this adapter has never seen that device.
     *
     * @param address valid Bluetooth MAC address
     * @param addressType Bluetooth address type
     * @throws IllegalArgumentException if address is invalid
     */
    @RequiresNoPermission
    @NonNull
    public BluetoothDevice getRemoteLeDevice(@NonNull String address,
            @AddressType int addressType) {
        final BluetoothDevice res = new BluetoothDevice(address, addressType);
        res.setAttributionSource(mAttributionSource);
        return res;
    }

    /**
     * Get a {@link BluetoothDevice} object for the given Bluetooth hardware
     * address.
     * <p>Valid Bluetooth hardware addresses must be 6 bytes. This method
     * expects the address in network byte order (MSB first).
     * <p>A {@link BluetoothDevice} will always be returned for a valid
     * hardware address, even if this adapter has never seen that device.
     *
     * @param address Bluetooth MAC address (6 bytes)
     * @throws IllegalArgumentException if address is invalid
     */
    @RequiresNoPermission
    public BluetoothDevice getRemoteDevice(byte[] address) {
        if (address == null || address.length != 6) {
            throw new IllegalArgumentException("Bluetooth address must have 6 bytes");
        }
        final BluetoothDevice res = new BluetoothDevice(
                String.format(Locale.US, "%02X:%02X:%02X:%02X:%02X:%02X", address[0], address[1],
                        address[2], address[3], address[4], address[5]));
        res.setAttributionSource(mAttributionSource);
        return res;
    }

    /**
     * Returns a {@link BluetoothLeAdvertiser} object for Bluetooth LE Advertising operations.
     * Will return null if Bluetooth is turned off or if Bluetooth LE Advertising is not
     * supported on this device.
     * <p>
     * Use {@link #isMultipleAdvertisementSupported()} to check whether LE Advertising is supported
     * on this device before calling this method.
     */
    @RequiresNoPermission
    public BluetoothLeAdvertiser getBluetoothLeAdvertiser() {
        if (!getLeAccess()) {
            return null;
        }
        synchronized (mLock) {
            if (mBluetoothLeAdvertiser == null) {
                mBluetoothLeAdvertiser = new BluetoothLeAdvertiser(this);
            }
            return mBluetoothLeAdvertiser;
        }
    }

    /**
     * Returns a {@link PeriodicAdvertisingManager} object for Bluetooth LE Periodic Advertising
     * operations. Will return null if Bluetooth is turned off or if Bluetooth LE Periodic
     * Advertising is not supported on this device.
     * <p>
     * Use {@link #isLePeriodicAdvertisingSupported()} to check whether LE Periodic Advertising is
     * supported on this device before calling this method.
     *
     * @hide
     */
    @RequiresNoPermission
    public PeriodicAdvertisingManager getPeriodicAdvertisingManager() {
        if (!getLeAccess()) {
            return null;
        }

        if (!isLePeriodicAdvertisingSupported()) {
            return null;
        }

        synchronized (mLock) {
            if (mPeriodicAdvertisingManager == null) {
                mPeriodicAdvertisingManager = new PeriodicAdvertisingManager(this);
            }
            return mPeriodicAdvertisingManager;
        }
    }

    /**
     * Returns a {@link BluetoothLeScanner} object for Bluetooth LE scan operations.
     */
    @RequiresNoPermission
    public BluetoothLeScanner getBluetoothLeScanner() {
        if (!getLeAccess()) {
            return null;
        }
        synchronized (mLock) {
            if (mBluetoothLeScanner == null) {
                mBluetoothLeScanner = new BluetoothLeScanner(this);
            }
            return mBluetoothLeScanner;
        }
    }

     /**
     * Get a {@link DistanceMeasurementManager} object for distance measurement operations.
     * <p>
     * Use {@link #isDistanceMeasurementSupported()} to check whether distance
     * measurement is supported on this device before calling this method.
     *
     * @return a new instance of {@link DistanceMeasurementManager}, or {@code null} if Bluetooth is
     * turned off
     * @throws UnsupportedOperationException if distance measurement is not supported on this device
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @Nullable DistanceMeasurementManager getDistanceMeasurementManager() {
        if (!getLeAccess()) {
            return null;
        }

        if (isDistanceMeasurementSupported() != BluetoothStatusCodes.FEATURE_SUPPORTED) {
            throw new UnsupportedOperationException("Distance measurement is unsupported");
        }

        synchronized (mLock) {
            if (mDistanceMeasurementManager == null) {
                mDistanceMeasurementManager = new DistanceMeasurementManager(this);
            }
            return mDistanceMeasurementManager;
        }
    }

    /**
     * Return true if Bluetooth is currently enabled and ready for use.
     * <p>Equivalent to:
     * <code>getBluetoothState() == STATE_ON</code>
     *
     * @return true if the local adapter is turned on
     */
    @RequiresLegacyBluetoothPermission
    @RequiresNoPermission
    public boolean isEnabled() {
        return getState() == BluetoothAdapter.STATE_ON;
    }

    /**
     * Return true if Bluetooth LE(Always BLE On feature) is currently
     * enabled and ready for use
     * <p>This returns true if current state is either STATE_ON or STATE_BLE_ON
     *
     * @return true if the local Bluetooth LE adapter is turned on
     * @hide
     */
    @SystemApi
    @RequiresNoPermission
    public boolean isLeEnabled() {
        final int state = getLeState();
        if (DBG) {
            Log.d(TAG, "isLeEnabled(): " + BluetoothAdapter.nameForState(state));
        }
        return (state == BluetoothAdapter.STATE_ON
                || state == BluetoothAdapter.STATE_BLE_ON
                || state == BluetoothAdapter.STATE_TURNING_ON
                || state == BluetoothAdapter.STATE_TURNING_OFF);
    }

    /**
     * Turns off Bluetooth LE which was earlier turned on by calling enableBLE().
     *
     * <p> If the internal Adapter state is STATE_BLE_ON, this would trigger the transition
     * to STATE_OFF and completely shut-down Bluetooth
     *
     * <p> If the Adapter state is STATE_ON, This would unregister the existance of
     * special Bluetooth LE application and hence the further turning off of Bluetooth
     * from UI would ensure the complete turn-off of Bluetooth rather than staying back
     * BLE only state
     *
     * <p>This is an asynchronous call: it will return immediately, and
     * clients should listen for {@link #ACTION_BLE_STATE_CHANGED}
     * to be notified of subsequent adapter state changes If this call returns
     * true, then the adapter state will immediately transition from {@link
     * #STATE_ON} to {@link #STATE_TURNING_OFF}, and some time
     * later transition to either {@link #STATE_BLE_ON} or {@link
     * #STATE_OFF} based on the existance of the further Always BLE ON enabled applications
     * If this call returns false then there was an
     * immediate problem that will prevent the QAdapter from being turned off -
     * such as the QAadapter already being turned off.
     *
     * @return true to indicate success, or false on immediate error
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean disableBLE() {
        if (!isBleScanAlwaysAvailable()) {
            return false;
        }
        try {
            return mManagerService.disableBle(mAttributionSource, mToken);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
        return false;
    }

    /**
     * Applications who want to only use Bluetooth Low Energy (BLE) can call enableBLE.
     *
     * enableBLE registers the existence of an app using only LE functions.
     *
     * enableBLE may enable Bluetooth to an LE only mode so that an app can use
     * LE related features (BluetoothGatt or BluetoothGattServer classes)
     *
     * If the user disables Bluetooth while an app is registered to use LE only features,
     * Bluetooth will remain on in LE only mode for the app.
     *
     * When Bluetooth is in LE only mode, it is not shown as ON to the UI.
     *
     * <p>This is an asynchronous call: it returns immediately, and
     * clients should listen for {@link #ACTION_BLE_STATE_CHANGED}
     * to be notified of adapter state changes.
     *
     * If this call returns * true, then the adapter state is either in a mode where
     * LE is available, or will transition from {@link #STATE_OFF} to {@link #STATE_BLE_TURNING_ON},
     * and some time later transition to either {@link #STATE_OFF} or {@link #STATE_BLE_ON}.
     *
     * If this call returns false then there was an immediate problem that prevents the
     * adapter from being turned on - such as Airplane mode.
     *
     * {@link #ACTION_BLE_STATE_CHANGED} returns the Bluetooth Adapter's various
     * states, It includes all the classic Bluetooth Adapter states along with
     * internal BLE only states
     *
     * @return true to indicate Bluetooth LE will be available, or false on immediate error
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean enableBLE() {
        if (!isBleScanAlwaysAvailable()) {
            return false;
        }
        try {
            return mManagerService.enableBle(mAttributionSource, mToken);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }

        return false;
    }

    /**
     * There are several instances of IpcDataCache used in this class.
     * BluetoothCache wraps up the common code.  All caches are created with a maximum of
     * eight entries, and the key is in the bluetooth module.  The name is set to the api.
     */
    private static class BluetoothCache<Q, R> extends IpcDataCache<Q, R> {
        BluetoothCache(String api, IpcDataCache.QueryHandler query) {
            super(8, IpcDataCache.MODULE_BLUETOOTH, api, api, query);
        }};

    /**
     * Invalidate a bluetooth cache.  This method is just a short-hand wrapper that
     * enforces the bluetooth module.
     */
    private static void invalidateCache(@NonNull String api) {
        IpcDataCache.invalidateCache(IpcDataCache.MODULE_BLUETOOTH, api);
    }

    private static final IpcDataCache.QueryHandler<IBluetooth, Integer> sBluetoothGetStateQuery =
            new IpcDataCache.QueryHandler<>() {
            @RequiresLegacyBluetoothPermission
            @RequiresNoPermission
            @Override
            public @InternalAdapterState Integer apply(IBluetooth serviceQuery) {
                try {
                    final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                    serviceQuery.getState(recv);
                    return recv.awaitResultNoInterrupt(getSyncTimeout())
                        .getValue(BluetoothAdapter.STATE_OFF);
                } catch (RemoteException | TimeoutException e) {
                    throw new RuntimeException(e);
                }
            }
        };

    private static final String GET_STATE_API = "BluetoothAdapter_getState";

    private static final IpcDataCache<IBluetooth, Integer> sBluetoothGetStateCache =
            new BluetoothCache<>(GET_STATE_API, sBluetoothGetStateQuery);

    /** @hide */
    @RequiresNoPermission
    public void disableBluetoothGetStateCache() {
        sBluetoothGetStateCache.disableForCurrentProcess();
    }

    /** @hide */
    public static void invalidateBluetoothGetStateCache() {
        invalidateCache(GET_STATE_API);
    }

    /**
     * Fetch the current bluetooth state.  If the service is down, return
     * OFF.
     */
    private @InternalAdapterState int getStateInternal() {
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                return sBluetoothGetStateCache.query(mService);
            }
        } catch (RuntimeException e) {
            if (!(e.getCause() instanceof TimeoutException)
                    && !(e.getCause() instanceof RemoteException)) {
                throw e;
            }
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return STATE_OFF;
    }

    /**
     * Get the current state of the local Bluetooth adapter.
     * <p>Possible return values are
     * {@link #STATE_OFF},
     * {@link #STATE_TURNING_ON},
     * {@link #STATE_ON},
     * {@link #STATE_TURNING_OFF}.
     *
     * @return current state of Bluetooth adapter
     */
    @RequiresLegacyBluetoothPermission
    @RequiresNoPermission
    public @AdapterState int getState() {
        int state = getStateInternal();

        // Consider all internal states as OFF
        if (state == BluetoothAdapter.STATE_BLE_ON || state == BluetoothAdapter.STATE_BLE_TURNING_ON
                || state == BluetoothAdapter.STATE_BLE_TURNING_OFF) {
            if (VDBG) {
                Log.d(TAG, "Consider " + BluetoothAdapter.nameForState(state) + " state as OFF");
            }
            state = BluetoothAdapter.STATE_OFF;
        }
        if (VDBG) {
            Log.d(TAG, "" + hashCode() + ": getState(). Returning " + BluetoothAdapter.nameForState(
                    state));
        }
        return state;
    }

    /**
     * Get the current state of the local Bluetooth adapter
     * <p>This returns current internal state of Adapter including LE ON/OFF
     *
     * <p>Possible return values are
     * {@link #STATE_OFF},
     * {@link #STATE_BLE_TURNING_ON},
     * {@link #STATE_BLE_ON},
     * {@link #STATE_TURNING_ON},
     * {@link #STATE_ON},
     * {@link #STATE_TURNING_OFF},
     * {@link #STATE_BLE_TURNING_OFF}.
     *
     * @return current state of Bluetooth adapter
     * @hide
     */
    @RequiresLegacyBluetoothPermission
    @RequiresNoPermission
    @UnsupportedAppUsage(publicAlternatives = "Use {@link #getState()} instead to determine "
            + "whether you can use BLE & BT classic.")
    public @InternalAdapterState int getLeState() {
        int state = getStateInternal();

        if (VDBG) {
            Log.d(TAG, "getLeState() returning " + BluetoothAdapter.nameForState(state));
        }
        return state;
    }

    boolean getLeAccess() {
        if (getLeState() == STATE_ON) {
            return true;
        } else if (getLeState() == STATE_BLE_ON) {
            return true; // TODO: FILTER SYSTEM APPS HERE <--
        }

        return false;
    }

    /**
     * Turn on the local Bluetooth adapter&mdash;do not use without explicit
     * user action to turn on Bluetooth.
     * <p>This powers on the underlying Bluetooth hardware, and starts all
     * Bluetooth system services.
     * <p class="caution"><strong>Bluetooth should never be enabled without
     * direct user consent</strong>. If you want to turn on Bluetooth in order
     * to create a wireless connection, you should use the {@link
     * #ACTION_REQUEST_ENABLE} Intent, which will raise a dialog that requests
     * user permission to turn on Bluetooth. The {@link #enable()} method is
     * provided only for applications that include a user interface for changing
     * system settings, such as a "power manager" app.</p>
     * <p>This is an asynchronous call: it will return immediately, and
     * clients should listen for {@link #ACTION_STATE_CHANGED}
     * to be notified of subsequent adapter state changes. If this call returns
     * true, then the adapter state will immediately transition from {@link
     * #STATE_OFF} to {@link #STATE_TURNING_ON}, and some time
     * later transition to either {@link #STATE_OFF} or {@link
     * #STATE_ON}. If this call returns false then there was an
     * immediate problem that will prevent the adapter from being turned on -
     * such as Airplane mode, or the adapter is already turned on.
     *
     * @return true to indicate adapter startup has begun, or false on immediate error
     *
     * @deprecated Starting with {@link android.os.Build.VERSION_CODES#TIRAMISU}, applications
     * are not allowed to enable/disable Bluetooth.
     * <b>Compatibility Note:</b> For applications targeting
     * {@link android.os.Build.VERSION_CODES#TIRAMISU} or above, this API will always fail and return
     * {@code false}. If apps are targeting an older SDK ({@link android.os.Build.VERSION_CODES#S}
     * or below), they can continue to use this API.
     * <p>
     * Deprecation Exemptions:
     * <ul>
     * <li>Device Owner (DO), Profile Owner (PO) and system apps.
     * </ul>
     */
    @Deprecated
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean enable() {
        if (isEnabled()) {
            if (DBG) {
                Log.d(TAG, "enable(): BT already enabled!");
            }
            return true;
        }
        try {
            return mManagerService.enable(mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
        return false;
    }

    /**
     * Turn off the local Bluetooth adapter&mdash;do not use without explicit
     * user action to turn off Bluetooth.
     * <p>This gracefully shuts down all Bluetooth connections, stops Bluetooth
     * system services, and powers down the underlying Bluetooth hardware.
     * <p class="caution"><strong>Bluetooth should never be disabled without
     * direct user consent</strong>. The {@link #disable()} method is
     * provided only for applications that include a user interface for changing
     * system settings, such as a "power manager" app.</p>
     * <p>This is an asynchronous call: it will return immediately, and
     * clients should listen for {@link #ACTION_STATE_CHANGED}
     * to be notified of subsequent adapter state changes. If this call returns
     * true, then the adapter state will immediately transition from {@link
     * #STATE_ON} to {@link #STATE_TURNING_OFF}, and some time
     * later transition to either {@link #STATE_OFF} or {@link
     * #STATE_ON}. If this call returns false then there was an
     * immediate problem that will prevent the adapter from being turned off -
     * such as the adapter already being turned off.
     *
     * @return true to indicate adapter shutdown has begun, or false on immediate error
     *
     * @deprecated Starting with {@link android.os.Build.VERSION_CODES#TIRAMISU}, applications
     * are not allowed to enable/disable Bluetooth.
     * <b>Compatibility Note:</b> For applications targeting
     * {@link android.os.Build.VERSION_CODES#TIRAMISU} or above, this API will always fail and return
     * {@code false}. If apps are targeting an older SDK ({@link android.os.Build.VERSION_CODES#S}
     * or below), they can continue to use this API.
     * <p>
     * Deprecation Exemptions:
     * <ul>
     * <li>Device Owner (DO), Profile Owner (PO) and system apps.
     * </ul>
     */
    @Deprecated
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean disable() {
        return disable(true);
    }

    /**
     * Turn off the local Bluetooth adapter and don't persist the setting.
     *
     * @param persist Indicate whether the off state should be persisted following the next reboot
     * @return true to indicate adapter shutdown has begun, or false on immediate error
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean disable(boolean persist) {
        try {
            return mManagerService.disable(mAttributionSource, persist);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
        return false;
    }

    /**
     * Returns the hardware address of the local Bluetooth adapter.
     * <p>For example, "00:11:22:AA:BB:CC".
     *
     * @return Bluetooth hardware address as string
     *
     * Requires {@code android.Manifest.permission#LOCAL_MAC_ADDRESS} and
     * {@link android.Manifest.permission#BLUETOOTH_CONNECT}.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    public String getAddress() {
        try {
            return mManagerService.getAddress(mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    /**
     * Get the friendly Bluetooth name of the local Bluetooth adapter.
     * <p>This name is visible to remote Bluetooth devices.
     *
     * @return the Bluetooth name, or null on error
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public String getName() {
        try {
            return mManagerService.getName(mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    /** {@hide} */
    @RequiresBluetoothAdvertisePermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE)
    public int getNameLengthForAdvertise() {
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                mService.getNameLengthForAdvertise(mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(-1);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return -1;
    }

    /**
     * Factory reset bluetooth settings.
     *
     * @return true to indicate that the config file was successfully cleared
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean clearBluetooth() {
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                mService.factoryReset(mAttributionSource, recv);
                if (recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(false)
                        && mManagerService != null
                        && mManagerService.onFactoryReset(mAttributionSource)) {
                    return true;
                }
            }
            Log.e(TAG, "factoryReset(): Setting persist.bluetooth.factoryreset to retry later");
            BluetoothProperties.factory_reset(true);
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return false;
    }

     /**
     * See {@link #clearBluetooth()}
     *
     * @return true to indicate that the config file was successfully cleared
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean factoryReset() {
        return clearBluetooth();
    }

    /**
     * Get the UUIDs supported by the local Bluetooth adapter.
     *
     * @return the UUIDs supported by the local Bluetooth Adapter.
     * @hide
     */
    @UnsupportedAppUsage
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public @NonNull ParcelUuid[] getUuids() {
        List<ParcelUuid> parcels = getUuidsList();
        return parcels.toArray(new ParcelUuid[parcels.size()]);
    }

    /**
     * Get the UUIDs supported by the local Bluetooth adapter.
     *
     * @return a list of the UUIDs supported by the local Bluetooth Adapter.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public @NonNull List<ParcelUuid> getUuidsList() {
        List<ParcelUuid> defaultValue = new ArrayList<>();
        if (getState() != STATE_ON) {
            return defaultValue;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<List<ParcelUuid>> recv =
                        SynchronousResultReceiver.get();
                mService.getUuids(mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return defaultValue;
    }

    /**
     * Set the friendly Bluetooth name of the local Bluetooth adapter.
     * <p>This name is visible to remote Bluetooth devices.
     * <p>Valid Bluetooth names are a maximum of 248 bytes using UTF-8
     * encoding, although many remote devices can only display the first
     * 40 characters, and some may be limited to just 20.
     * <p>If Bluetooth state is not {@link #STATE_ON}, this API
     * will return false. After turning on Bluetooth,
     * wait for {@link #ACTION_STATE_CHANGED} with {@link #STATE_ON}
     * to get the updated value.
     *
     * @param name a valid Bluetooth name
     * @return true if the name was set, false otherwise
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean setName(String name) {
        if (getState() != STATE_ON) {
            return false;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                mService.setName(name, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(false);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return false;
    }

    /**
     * Returns the Input/Output capability of the device for classic Bluetooth.
     *
     * @return Input/Output capability of the device. One of {@link #IO_CAPABILITY_OUT},
     *         {@link #IO_CAPABILITY_IO}, {@link #IO_CAPABILITY_IN}, {@link #IO_CAPABILITY_NONE},
     *         {@link #IO_CAPABILITY_KBDISP} or {@link #IO_CAPABILITY_UNKNOWN}.
     *
     * @hide
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @IoCapability
    public int getIoCapability() {
        if (getState() != STATE_ON) return BluetoothAdapter.IO_CAPABILITY_UNKNOWN;
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Integer> recv =
                        SynchronousResultReceiver.get();
                mService.getIoCapability(mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout())
                    .getValue(BluetoothAdapter.IO_CAPABILITY_UNKNOWN);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return BluetoothAdapter.IO_CAPABILITY_UNKNOWN;
    }

    /**
     * Sets the Input/Output capability of the device for classic Bluetooth.
     *
     * <p>Changing the Input/Output capability of a device only takes effect on restarting the
     * Bluetooth stack. You would need to restart the stack using {@link BluetoothAdapter#disable()}
     * and {@link BluetoothAdapter#enable()} to see the changes.
     *
     * @param capability Input/Output capability of the device. One of {@link #IO_CAPABILITY_OUT},
     *                   {@link #IO_CAPABILITY_IO}, {@link #IO_CAPABILITY_IN},
     *                   {@link #IO_CAPABILITY_NONE} or {@link #IO_CAPABILITY_KBDISP}.
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean setIoCapability(@IoCapability int capability) {
        if (getState() != STATE_ON) return false;
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                mService.setIoCapability(capability, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(false);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return false;
    }

    /**
     * Get the current Bluetooth scan mode of the local Bluetooth adapter.
     * <p>The Bluetooth scan mode determines if the local adapter is
     * connectable and/or discoverable from remote Bluetooth devices.
     * <p>Possible values are:
     * {@link #SCAN_MODE_NONE},
     * {@link #SCAN_MODE_CONNECTABLE},
     * {@link #SCAN_MODE_CONNECTABLE_DISCOVERABLE}.
     * <p>If Bluetooth state is not {@link #STATE_ON}, this API
     * will return {@link #SCAN_MODE_NONE}. After turning on Bluetooth,
     * wait for {@link #ACTION_STATE_CHANGED} with {@link #STATE_ON}
     * to get the updated value.
     *
     * @return scan mode
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothScanPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    @ScanMode
    public int getScanMode() {
        if (getState() != STATE_ON) {
            return SCAN_MODE_NONE;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                mService.getScanMode(mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(SCAN_MODE_NONE);
            }
        } catch (TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } finally {
            mServiceLock.readLock().unlock();
        }
        return SCAN_MODE_NONE;
    }

    /**
     * Set the local Bluetooth adapter connectablility and discoverability.
     * <p>If the scan mode is set to {@link #SCAN_MODE_CONNECTABLE_DISCOVERABLE},
     * it will change to {@link #SCAN_MODE_CONNECTABLE} after the discoverable timeout.
     * The discoverable timeout can be set with {@link #setDiscoverableTimeout} and
     * checked with {@link #getDiscoverableTimeout}. By default, the timeout is usually
     * 120 seconds on phones which is enough for a remote device to initiate and complete
     * its discovery process.
     * <p>Applications cannot set the scan mode. They should use
     * {@link #ACTION_REQUEST_DISCOVERABLE} instead.
     *
     * @param mode represents the desired state of the local device scan mode
     *
     * @return status code indicating whether the scan mode was successfully set
     * @throws IllegalArgumentException if the mode is not a valid scan mode
     * @hide
     */
    @SystemApi
    @RequiresBluetoothScanPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @ScanModeStatusCode
    public int setScanMode(@ScanMode int mode) {
        if (getState() != STATE_ON) {
            return BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
        }
        if (mode != SCAN_MODE_NONE && mode != SCAN_MODE_CONNECTABLE
                && mode != SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            throw new IllegalArgumentException("Invalid scan mode param value");
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                mService.setScanMode(mode, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout())
                        .getValue(BluetoothStatusCodes.ERROR_UNKNOWN);
            }
        } catch (TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } finally {
            mServiceLock.readLock().unlock();
        }
        return BluetoothStatusCodes.ERROR_UNKNOWN;
    }

    /**
     * Get the timeout duration of the {@link #SCAN_MODE_CONNECTABLE_DISCOVERABLE}.
     *
     * @return the duration of the discoverable timeout or null if an error has occurred
     */
    @RequiresBluetoothScanPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    public @Nullable Duration getDiscoverableTimeout() {
        if (getState() != STATE_ON) {
            return null;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Long> recv = SynchronousResultReceiver.get();
                mService.getDiscoverableTimeout(mAttributionSource, recv);
                long timeout = recv.awaitResultNoInterrupt(getSyncTimeout()).getValue((long) -1);
                return (timeout == -1) ? null : Duration.ofSeconds(timeout);
            }
        } catch (TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } finally {
            mServiceLock.readLock().unlock();
        }
        return null;
    }

    /**
     * Set the total time the Bluetooth local adapter will stay discoverable when
     * {@link #setScanMode} is called with {@link #SCAN_MODE_CONNECTABLE_DISCOVERABLE} mode.
     * After this timeout, the scan mode will fallback to {@link #SCAN_MODE_CONNECTABLE}.
     * <p>If <code>timeout</code> is set to 0, no timeout will occur and the scan mode will
     * be persisted until a subsequent call to {@link #setScanMode}.
     *
     * @param timeout represents the total duration the local Bluetooth adapter will remain
     *                discoverable, or no timeout if set to 0
     * @return whether the timeout was successfully set
     * @throws IllegalArgumentException if <code>timeout</code> duration in seconds is more
     *         than {@link Integer#MAX_VALUE}
     * @hide
     */
    @SystemApi
    @RequiresBluetoothScanPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @ScanModeStatusCode
    public int setDiscoverableTimeout(@NonNull Duration timeout) {
        if (getState() != STATE_ON) {
            return BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
        }
        if (timeout.toSeconds() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Timeout in seconds must be less or equal to "
                    + Integer.MAX_VALUE);
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                mService.setDiscoverableTimeout(timeout.toSeconds(), mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout())
                        .getValue(BluetoothStatusCodes.ERROR_UNKNOWN);
            }
        } catch (TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } finally {
            mServiceLock.readLock().unlock();
        }
        return BluetoothStatusCodes.ERROR_UNKNOWN;
    }

    /**
     * Get the end time of the latest remote device discovery process.
     *
     * @return the latest time that the bluetooth adapter was/will be in discovery mode, in
     * milliseconds since the epoch. This time can be in the future if {@link #startDiscovery()} has
     * been called recently.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public long getDiscoveryEndMillis() {
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Long> recv = SynchronousResultReceiver.get();
                mService.getDiscoveryEndMillis(mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue((long) -1);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return -1;
    }

    /**
     * Start the remote device discovery process.
     * <p>The discovery process usually involves an inquiry scan of about 12
     * seconds, followed by a page scan of each new device to retrieve its
     * Bluetooth name.
     * <p>This is an asynchronous call, it will return immediately. Register
     * for {@link #ACTION_DISCOVERY_STARTED} and {@link
     * #ACTION_DISCOVERY_FINISHED} intents to determine exactly when the
     * discovery starts and completes. Register for {@link
     * BluetoothDevice#ACTION_FOUND} to be notified as remote Bluetooth devices
     * are found.
     * <p>Device discovery is a heavyweight procedure. New connections to
     * remote Bluetooth devices should not be attempted while discovery is in
     * progress, and existing connections will experience limited bandwidth
     * and high latency. Use {@link #cancelDiscovery()} to cancel an ongoing
     * discovery. Discovery is not managed by the Activity,
     * but is run as a system service, so an application should always call
     * {@link BluetoothAdapter#cancelDiscovery()} even if it
     * did not directly request a discovery, just to be sure.
     * <p>Device discovery will only find remote devices that are currently
     * <i>discoverable</i> (inquiry scan enabled). Many Bluetooth devices are
     * not discoverable by default, and need to be entered into a special mode.
     * <p>If Bluetooth state is not {@link #STATE_ON}, this API
     * will return false. After turning on Bluetooth, wait for {@link #ACTION_STATE_CHANGED}
     * with {@link #STATE_ON} to get the updated value.
     * <p>If a device is currently bonding, this request will be queued and executed once that
     * device has finished bonding. If a request is already queued, this request will be ignored.
     *
     * @return true on success, false on error
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothScanPermission
    @RequiresBluetoothLocationPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    public boolean startDiscovery() {
        if (getState() != STATE_ON) {
            return false;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                mService.startDiscovery(mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(false);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return false;
    }

    /**
     * Cancel the current device discovery process.
     * <p>Because discovery is a heavyweight procedure for the Bluetooth
     * adapter, this method should always be called before attempting to connect
     * to a remote device with {@link
     * android.bluetooth.BluetoothSocket#connect()}. Discovery is not managed by
     * the  Activity, but is run as a system service, so an application should
     * always call cancel discovery even if it did not directly request a
     * discovery, just to be sure.
     * <p>If Bluetooth state is not {@link #STATE_ON}, this API
     * will return false. After turning on Bluetooth,
     * wait for {@link #ACTION_STATE_CHANGED} with {@link #STATE_ON}
     * to get the updated value.
     *
     * @return true on success, false on error
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothScanPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    public boolean cancelDiscovery() {
        if (getState() != STATE_ON) {
            return false;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                mService.cancelDiscovery(mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(false);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return false;
    }

    /**
     * Return true if the local Bluetooth adapter is currently in the device
     * discovery process.
     * <p>Device discovery is a heavyweight procedure. New connections to
     * remote Bluetooth devices should not be attempted while discovery is in
     * progress, and existing connections will experience limited bandwidth
     * and high latency. Use {@link #cancelDiscovery()} to cancel an ongoing
     * discovery.
     * <p>Applications can also register for {@link #ACTION_DISCOVERY_STARTED}
     * or {@link #ACTION_DISCOVERY_FINISHED} to be notified when discovery
     * starts or completes.
     * <p>If Bluetooth state is not {@link #STATE_ON}, this API
     * will return false. After turning on Bluetooth,
     * wait for {@link #ACTION_STATE_CHANGED} with {@link #STATE_ON}
     * to get the updated value.
     *
     * @return true if discovering
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothScanPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    public boolean isDiscovering() {
        if (getState() != STATE_ON) {
            return false;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                mService.isDiscovering(mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(false);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return false;
    }

    /**
     * Removes the active device for the grouping of @ActiveDeviceUse specified
     *
     * @param profiles represents the purpose for which we are setting this as the active device.
     *                 Possible values are:
     *                 {@link BluetoothAdapter#ACTIVE_DEVICE_AUDIO},
     *                 {@link BluetoothAdapter#ACTIVE_DEVICE_PHONE_CALL},
     *                 {@link BluetoothAdapter#ACTIVE_DEVICE_ALL}
     * @return false on immediate error, true otherwise
     * @throws IllegalArgumentException if device is null or profiles is not one of
     * {@link ActiveDeviceUse}
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
            android.Manifest.permission.MODIFY_PHONE_STATE,
    })
    public boolean removeActiveDevice(@ActiveDeviceUse int profiles) {
        if (profiles != ACTIVE_DEVICE_AUDIO && profiles != ACTIVE_DEVICE_PHONE_CALL
                && profiles != ACTIVE_DEVICE_ALL) {
            Log.e(TAG, "Invalid profiles param value in removeActiveDevice");
            throw new IllegalArgumentException("Profiles must be one of "
                    + "BluetoothAdapter.ACTIVE_DEVICE_AUDIO, "
                    + "BluetoothAdapter.ACTIVE_DEVICE_PHONE_CALL, or "
                    + "BluetoothAdapter.ACTIVE_DEVICE_ALL");
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                if (DBG) Log.d(TAG, "removeActiveDevice, profiles: " + profiles);
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                mService.removeActiveDevice(profiles, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(false);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }

        return false;
    }

    /**
     * Sets device as the active devices for the use cases passed into the function. Note that in
     * order to make a device active for LE Audio, it must be the active device for audio and
     * phone calls.
     *
     * @param device is the remote bluetooth device
     * @param profiles represents the purpose for which we are setting this as the active device.
     *                 Possible values are:
     *                 {@link BluetoothAdapter#ACTIVE_DEVICE_AUDIO},
     *                 {@link BluetoothAdapter#ACTIVE_DEVICE_PHONE_CALL},
     *                 {@link BluetoothAdapter#ACTIVE_DEVICE_ALL}
     * @return false on immediate error, true otherwise
     * @throws IllegalArgumentException if device is null or profiles is not one of
     * {@link ActiveDeviceUse}
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
            android.Manifest.permission.MODIFY_PHONE_STATE,
    })
    public boolean setActiveDevice(@NonNull BluetoothDevice device,
            @ActiveDeviceUse int profiles) {
        if (device == null) {
            Log.e(TAG, "setActiveDevice: Null device passed as parameter");
            throw new IllegalArgumentException("device cannot be null");
        }
        if (profiles != ACTIVE_DEVICE_AUDIO && profiles != ACTIVE_DEVICE_PHONE_CALL
                && profiles != ACTIVE_DEVICE_ALL) {
            Log.e(TAG, "Invalid profiles param value in setActiveDevice");
            throw new IllegalArgumentException("Profiles must be one of "
                    + "BluetoothAdapter.ACTIVE_DEVICE_AUDIO, "
                    + "BluetoothAdapter.ACTIVE_DEVICE_PHONE_CALL, or "
                    + "BluetoothAdapter.ACTIVE_DEVICE_ALL");
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                if (DBG) {
                    Log.d(TAG, "setActiveDevice, device: " + device + ", profiles: " + profiles);
                }
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                mService.setActiveDevice(device, profiles, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(false);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }

        return false;
    }

    /**
     * Get the active devices for the BluetoothProfile specified
     *
     * @param profile is the profile from which we want the active devices.
     *                Possible values are:
     *                {@link BluetoothProfile#HEADSET},
     *                {@link BluetoothProfile#A2DP},
     *                {@link BluetoothProfile#HEARING_AID}
     *                {@link BluetoothProfile#LE_AUDIO}
     * @return A list of active bluetooth devices
     * @throws IllegalArgumentException If profile is not one of {@link ActiveDeviceProfile}
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @NonNull List<BluetoothDevice> getActiveDevices(@ActiveDeviceProfile int profile) {
        if (profile != BluetoothProfile.HEADSET
                && profile != BluetoothProfile.A2DP
                && profile != BluetoothProfile.HEARING_AID
                && profile != BluetoothProfile.LE_AUDIO) {
            Log.e(TAG, "Invalid profile param value in getActiveDevices");
            throw new IllegalArgumentException("Profiles must be one of "
                    + "BluetoothProfile.A2DP, "
                    + "BluetoothProfile.HEARING_AID, or"
                    + "BluetoothProfile.HEARING_AID"
                    + "BluetoothProfile.LE_AUDIO");
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                if (DBG) {
                    Log.d(TAG, "getActiveDevices(profile= "
                            + BluetoothProfile.getProfileName(profile) + ")");
                }
                final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                        SynchronousResultReceiver.get();
                mService.getActiveDevices(profile, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(new ArrayList<>());
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }

        return new ArrayList<>();
    }

    /**
     * Return true if the multi advertisement is supported by the chipset
     *
     * @return true if Multiple Advertisement feature is supported
     */
    @RequiresLegacyBluetoothPermission
    @RequiresNoPermission
    public boolean isMultipleAdvertisementSupported() {
        if (getState() != STATE_ON) {
            return false;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                mService.isMultiAdvertisementSupported(recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(false);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return false;
    }

    /**
     * Returns {@code true} if BLE scan is always available, {@code false} otherwise. <p>
     *
     * If this returns {@code true}, application can issue {@link BluetoothLeScanner#startScan} and
     * fetch scan results even when Bluetooth is turned off.<p>
     *
     * To change this setting, use {@link #ACTION_REQUEST_BLE_SCAN_ALWAYS_AVAILABLE}.
     *
     * @hide
     */
    @SystemApi
    @RequiresNoPermission
    public boolean isBleScanAlwaysAvailable() {
        try {
            return mManagerService.isBleScanAlwaysAvailable();
        } catch (RemoteException e) {
            Log.e(TAG, "remote exception when calling isBleScanAlwaysAvailable", e);
            return false;
        }
    }

    private static final IpcDataCache.QueryHandler<IBluetooth, Boolean> sBluetoothFilteringQuery =
            new IpcDataCache.QueryHandler<>() {
        @RequiresLegacyBluetoothPermission
        @RequiresNoPermission
        @Override
        public Boolean apply(IBluetooth serviceQuery) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                serviceQuery.isOffloadedFilteringSupported(recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(false);
            } catch (RemoteException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        }};

    private static final String FILTERING_API = "BluetoothAdapter_isOffloadedFilteringSupported";

    private static final IpcDataCache<IBluetooth, Boolean> sBluetoothFilteringCache =
            new BluetoothCache<>(FILTERING_API, sBluetoothFilteringQuery);

    /** @hide */
    @RequiresNoPermission
    public void disableIsOffloadedFilteringSupportedCache() {
        sBluetoothFilteringCache.disableForCurrentProcess();
    }

    /** @hide */
    public static void invalidateIsOffloadedFilteringSupportedCache() {
        invalidateCache(FILTERING_API);
    }

    /**
     * Return true if offloaded filters are supported
     *
     * @return true if chipset supports on-chip filtering
     */
    @RequiresLegacyBluetoothPermission
    @RequiresNoPermission
    public boolean isOffloadedFilteringSupported() {
        if (!getLeAccess()) {
            return false;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) return sBluetoothFilteringCache.query(mService);
        } catch (RuntimeException e) {
            if (!(e.getCause() instanceof TimeoutException)
                    && !(e.getCause() instanceof RemoteException)) {
                throw e;
            }
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return false;
    }

    /**
     * Return true if offloaded scan batching is supported
     *
     * @return true if chipset supports on-chip scan batching
     */
    @RequiresLegacyBluetoothPermission
    @RequiresNoPermission
    public boolean isOffloadedScanBatchingSupported() {
        if (!getLeAccess()) {
            return false;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                mService.isOffloadedScanBatchingSupported(recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(false);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return false;
    }

    /**
     * Return true if LE 2M PHY feature is supported.
     *
     * @return true if chipset supports LE 2M PHY feature
     */
    @RequiresLegacyBluetoothPermission
    @RequiresNoPermission
    public boolean isLe2MPhySupported() {
        if (!getLeAccess()) {
            return false;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                mService.isLe2MPhySupported(recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(false);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return false;
    }

    /**
     * Return true if LE Coded PHY feature is supported.
     *
     * @return true if chipset supports LE Coded PHY feature
     */
    @RequiresLegacyBluetoothPermission
    @RequiresNoPermission
    public boolean isLeCodedPhySupported() {
        if (!getLeAccess()) {
            return false;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                mService.isLeCodedPhySupported(recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(false);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return false;
    }

    /**
     * Return true if LE Extended Advertising feature is supported.
     *
     * @return true if chipset supports LE Extended Advertising feature
     */
    @RequiresLegacyBluetoothPermission
    @RequiresNoPermission
    public boolean isLeExtendedAdvertisingSupported() {
        if (!getLeAccess()) {
            return false;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                mService.isLeExtendedAdvertisingSupported(recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(false);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return false;
    }

    /**
     * Return true if LE Periodic Advertising feature is supported.
     *
     * @return true if chipset supports LE Periodic Advertising feature
     */
    @RequiresLegacyBluetoothPermission
    @RequiresNoPermission
    public boolean isLePeriodicAdvertisingSupported() {
        if (!getLeAccess()) {
            return false;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                mService.isLePeriodicAdvertisingSupported(recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(false);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return false;
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            BluetoothStatusCodes.FEATURE_SUPPORTED,
            BluetoothStatusCodes.ERROR_UNKNOWN,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED,
            BluetoothStatusCodes.FEATURE_NOT_SUPPORTED,
    })
    public @interface LeFeatureReturnValues {}

    /**
     * Returns {@link BluetoothStatusCodes#FEATURE_SUPPORTED} if the LE audio feature is
     * supported, {@link BluetoothStatusCodes#FEATURE_NOT_SUPPORTED} if the feature is not
     * supported, or an error code.
     *
     * @return whether the LE audio is supported
     * @throws IllegalStateException if the bluetooth service is null
     */
    @RequiresNoPermission
    public @LeFeatureReturnValues int isLeAudioSupported() {
        if (!getLeAccess()) {
            return BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                mService.isLeAudioSupported(recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout())
                    .getValue(BluetoothStatusCodes.ERROR_UNKNOWN);
            } else {
                throw new IllegalStateException(
                        "LE state is on, but there is no bluetooth service.");
            }
        } catch (TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        } finally {
            mServiceLock.readLock().unlock();
        }
        return BluetoothStatusCodes.ERROR_UNKNOWN;
    }

    /**
     * Returns {@link BluetoothStatusCodes#FEATURE_SUPPORTED} if the LE audio broadcast source
     * feature is supported, {@link BluetoothStatusCodes#FEATURE_NOT_SUPPORTED} if the feature
     * is not supported, or an error code.
     *
     * @return whether the LE audio broadcast source is supported
     * @throws IllegalStateException if the bluetooth service is null
     */
    @RequiresNoPermission
    public @LeFeatureReturnValues int isLeAudioBroadcastSourceSupported() {
        if (!getLeAccess()) {
            return BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                mService.isLeAudioBroadcastSourceSupported(recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout())
                    .getValue(BluetoothStatusCodes.ERROR_UNKNOWN);
            } else {
                throw new IllegalStateException(
                        "LE state is on, but there is no bluetooth service.");
            }
        } catch (TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        } finally {
            mServiceLock.readLock().unlock();
        }

        return BluetoothStatusCodes.ERROR_UNKNOWN;
    }

    /**
     * Returns {@link BluetoothStatusCodes#FEATURE_SUPPORTED} if the LE audio broadcast assistant
     * feature is supported, {@link BluetoothStatusCodes#FEATURE_NOT_SUPPORTED} if the feature is
     * not supported, or an error code.
     *
     * @return whether the LE audio broadcast assistent is supported
     * @throws IllegalStateException if the bluetooth service is null
     */
    @RequiresNoPermission
    public @LeFeatureReturnValues int isLeAudioBroadcastAssistantSupported() {
        if (!getLeAccess()) {
            return BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                mService.isLeAudioBroadcastAssistantSupported(recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout())
                    .getValue(BluetoothStatusCodes.ERROR_UNKNOWN);
            } else {
                throw new IllegalStateException(
                        "LE state is on, but there is no bluetooth service.");
            }
        } catch (TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        } finally {
            mServiceLock.readLock().unlock();
        }
        return BluetoothStatusCodes.ERROR_UNKNOWN;
    }

    /**
     * Returns whether the distance measurement feature is supported.
     *
     * @return whether the Bluetooth distance measurement is supported
     * @throws IllegalStateException if the bluetooth service is null
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @LeFeatureReturnValues int isDistanceMeasurementSupported() {
        if (!getLeAccess()) {
            return BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                mService.isDistanceMeasurementSupported(mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout())
                    .getValue(BluetoothStatusCodes.ERROR_UNKNOWN);
            } else {
                throw new IllegalStateException(
                        "LE state is on, but there is no bluetooth service.");
            }
        } catch (TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        } finally {
            mServiceLock.readLock().unlock();
        }
        return BluetoothStatusCodes.ERROR_UNKNOWN;
    }

    /**
     * Return the maximum LE advertising data length in bytes,
     * if LE Extended Advertising feature is supported, 0 otherwise.
     *
     * @return the maximum LE advertising data length.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresNoPermission
    public int getLeMaximumAdvertisingDataLength() {
        if (!getLeAccess()) {
            return 0;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                mService.getLeMaximumAdvertisingDataLength(recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(0);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return 0;
    }

    /**
     * Return true if Hearing Aid Profile is supported.
     *
     * @return true if phone supports Hearing Aid Profile
     */
    @RequiresNoPermission
    private boolean isHearingAidProfileSupported() {
        try {
            return mManagerService.isHearingAidProfileSupported();
        } catch (RemoteException e) {
            Log.e(TAG, "remote exception when calling isHearingAidProfileSupported", e);
            return false;
        }
    }

    /**
     * Get the maximum number of connected devices per audio profile for this device.
     *
     * @return the number of allowed simultaneous connected devices for each audio profile
     *         for this device, or -1 if the Bluetooth service can't be reached
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public int getMaxConnectedAudioDevices() {
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                mService.getMaxConnectedAudioDevices(mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(1);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return -1;
    }

    /**
     * Return true if hardware has entries available for matching beacons
     *
     * @return true if there are hw entries available for matching beacons
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean isHardwareTrackingFiltersAvailable() {
        if (!getLeAccess()) {
            return false;
        }
        try {
            IBluetoothGatt iGatt = mManagerService.getBluetoothGatt();
            if (iGatt == null) {
                // BLE is not supported
                return false;
            }
            final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
            iGatt.numHwTrackFiltersAvailable(mAttributionSource, recv);
            return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(0) != 0;
        } catch (TimeoutException | RemoteException e) {
            Log.e(TAG, "", e);
        }
        return false;
    }

    /**
     * Request the record of {@link BluetoothActivityEnergyInfo} object that
     * has the activity and energy info. This can be used to ascertain what
     * the controller has been up to, since the last sample.
     *
     * The callback will be called only once, when the record is available.
     *
     * @param executor the executor that the callback will be invoked on
     * @param callback the callback that will be called with either the
     *                 {@link BluetoothActivityEnergyInfo} object, or the
     *                 error code if an error has occurred
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public void requestControllerActivityEnergyInfo(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OnBluetoothActivityEnergyInfoCallback callback) {
        requireNonNull(executor, "executor cannot be null");
        requireNonNull(callback, "callback cannot be null");
        OnBluetoothActivityEnergyInfoProxy proxy =
                new OnBluetoothActivityEnergyInfoProxy(executor, callback);
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                mService.requestActivityInfo(
                        proxy,
                        mAttributionSource);
            } else {
                proxy.onError(BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "getControllerActivityEnergyInfoCallback: " + e);
            proxy.onError(BluetoothStatusCodes.ERROR_UNKNOWN);
        } finally {
            mServiceLock.readLock().unlock();
        }
    }

    /**
     * Fetches a list of the most recently connected bluetooth devices ordered by how recently they
     * were connected with most recently first and least recently last
     *
     * @return {@link List} of bonded {@link BluetoothDevice} ordered by how recently they were
     * connected
     *
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @NonNull List<BluetoothDevice> getMostRecentlyConnectedDevices() {
        if (getState() != STATE_ON) {
            return new ArrayList<>();
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                        SynchronousResultReceiver.get();
                mService.getMostRecentlyConnectedDevices(mAttributionSource, recv);
                return Attributable.setAttributionSource(
                        recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(new ArrayList<>()),
                        mAttributionSource);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return new ArrayList<>();
    }

    /**
     * Return the set of {@link BluetoothDevice} objects that are bonded
     * (paired) to the local adapter.
     * <p>If Bluetooth state is not {@link #STATE_ON}, this API
     * will return an empty set. After turning on Bluetooth,
     * wait for {@link #ACTION_STATE_CHANGED} with {@link #STATE_ON}
     * to get the updated value.
     *
     * @return unmodifiable set of {@link BluetoothDevice}, or null on error
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public Set<BluetoothDevice> getBondedDevices() {
        if (getState() != STATE_ON) {
            return toDeviceSet(Arrays.asList());
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                        SynchronousResultReceiver.get();
                mService.getBondedDevices(mAttributionSource, recv);
                return toDeviceSet(Attributable.setAttributionSource(
                        recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(new ArrayList<>()),
                        mAttributionSource));
            }
            return toDeviceSet(Arrays.asList());
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return null;
    }

    /**
     * Gets the currently supported profiles by the adapter.
     *
     * <p> This can be used to check whether a profile is supported before attempting
     * to connect to its respective proxy.
     *
     * @return a list of integers indicating the ids of supported profiles as defined in {@link
     * BluetoothProfile}.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @NonNull List<Integer> getSupportedProfiles() {
        final ArrayList<Integer> supportedProfiles = new ArrayList<Integer>();

        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Long> recv = SynchronousResultReceiver.get();
                mService.getSupportedProfiles(mAttributionSource, recv);
                final long supportedProfilesBitMask =
                        recv.awaitResultNoInterrupt(getSyncTimeout()).getValue((long) 0);

                for (int i = 0; i <= BluetoothProfile.MAX_PROFILE_ID; i++) {
                    if ((supportedProfilesBitMask & (1 << i)) != 0) {
                        supportedProfiles.add(i);
                    }
                }
            } else {
                // Bluetooth is disabled. Just fill in known supported Profiles
                if (isHearingAidProfileSupported()) {
                    supportedProfiles.add(BluetoothProfile.HEARING_AID);
                }
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return supportedProfiles;
    }

    private static final IpcDataCache.QueryHandler<IBluetooth, Integer>
            sBluetoothGetAdapterConnectionStateQuery = new IpcDataCache.QueryHandler<>() {
                @RequiresLegacyBluetoothPermission
                @RequiresNoPermission
                @Override
                public Integer apply(IBluetooth serviceQuery) {
                    try {
                        final SynchronousResultReceiver<Integer> recv =
                                SynchronousResultReceiver.get();
                        serviceQuery.getAdapterConnectionState(recv);
                        return recv.awaitResultNoInterrupt(getSyncTimeout())
                            .getValue(STATE_DISCONNECTED);
                    } catch (RemoteException | TimeoutException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

    private static final String GET_CONNECTION_API = "BluetoothAdapter_getConnectionState";

    private static final IpcDataCache<IBluetooth, Integer>
            sBluetoothGetAdapterConnectionStateCache = new BluetoothCache<>(GET_CONNECTION_API,
                    sBluetoothGetAdapterConnectionStateQuery);

    /** @hide */
    @RequiresNoPermission
    public void disableGetAdapterConnectionStateCache() {
        sBluetoothGetAdapterConnectionStateCache.disableForCurrentProcess();
    }

    /** @hide */
    public static void invalidateGetAdapterConnectionStateCache() {
        invalidateCache(GET_CONNECTION_API);
    }

    /**
     * Get the current connection state of the local Bluetooth adapter.
     * This can be used to check whether the local Bluetooth adapter is connected
     * to any profile of any other remote Bluetooth Device.
     *
     * <p> Use this function along with {@link #ACTION_CONNECTION_STATE_CHANGED}
     * intent to get the connection state of the adapter.
     *
     * @return the connection state
     * @hide
     */
    @SystemApi
    @RequiresNoPermission
    public @ConnectionState int getConnectionState() {
        if (getState() != STATE_ON) {
            return BluetoothAdapter.STATE_DISCONNECTED;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) return sBluetoothGetAdapterConnectionStateCache.query(mService);
        } catch (RuntimeException e) {
            if (!(e.getCause() instanceof TimeoutException)
                    && !(e.getCause() instanceof RemoteException)) {
                throw e;
            }
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return STATE_DISCONNECTED;
    }

    private static final IpcDataCache
            .QueryHandler<Pair<IBluetooth, Pair<AttributionSource, Integer>>, Integer>
            sBluetoothProfileQuery = new IpcDataCache.QueryHandler<>() {
                @RequiresNoPermission
                @Override
                public Integer apply(Pair<IBluetooth, Pair<AttributionSource, Integer>> pairQuery) {
                    IBluetooth service = pairQuery.first;
                    AttributionSource source = pairQuery.second.first;
                    Integer profile = pairQuery.second.second;
                    final int defaultValue = STATE_DISCONNECTED;
                    try {
                        final SynchronousResultReceiver<Integer> recv =
                                SynchronousResultReceiver.get();
                        service.getProfileConnectionState(profile, source, recv);
                        return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
                    } catch (RemoteException | TimeoutException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

    private static final String PROFILE_API = "BluetoothAdapter_getProfileConnectionState";

    private static final IpcDataCache<Pair<IBluetooth, Pair<AttributionSource, Integer>>, Integer>
            sGetProfileConnectionStateCache = new BluetoothCache<>(PROFILE_API,
                    sBluetoothProfileQuery);

    /**
     * @hide
     */
    @RequiresNoPermission
    public void disableGetProfileConnectionStateCache() {
        sGetProfileConnectionStateCache.disableForCurrentProcess();
    }

    /**
     * @hide
     */
    public static void invalidateGetProfileConnectionStateCache() {
        invalidateCache(PROFILE_API);
    }

    /**
     * Get the current connection state of a profile.
     * This function can be used to check whether the local Bluetooth adapter
     * is connected to any remote device for a specific profile.
     * Profile can be one of {@link BluetoothProfile#HEADSET}, {@link BluetoothProfile#A2DP}.
     *
     * <p> Return the profile connection state
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public @ConnectionState int getProfileConnectionState(int profile) {
        if (getState() != STATE_ON) {
            return STATE_DISCONNECTED;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                return sGetProfileConnectionStateCache.query(
                        new Pair<>(mService, new Pair<>(mAttributionSource, profile)));
            }
        } catch (RuntimeException e) {
            if (!(e.getCause() instanceof TimeoutException)
                    && !(e.getCause() instanceof RemoteException)) {
                throw e;
            }
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return STATE_DISCONNECTED;
    }

    /**
     * Create a listening, secure RFCOMM Bluetooth socket.
     * <p>A remote device connecting to this socket will be authenticated and
     * communication on this socket will be encrypted.
     * <p>Use {@link BluetoothServerSocket#accept} to retrieve incoming
     * connections from a listening {@link BluetoothServerSocket}.
     * <p>Valid RFCOMM channels are in range 1 to 30.
     *
     * @param channel RFCOMM channel to listen on
     * @return a listening RFCOMM BluetoothServerSocket
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions, or channel in use.
     * @hide
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothServerSocket listenUsingRfcommOn(int channel) throws IOException {
        return listenUsingRfcommOn(channel, false, false);
    }

    /**
     * Create a listening, secure RFCOMM Bluetooth socket.
     * <p>A remote device connecting to this socket will be authenticated and
     * communication on this socket will be encrypted.
     * <p>Use {@link BluetoothServerSocket#accept} to retrieve incoming
     * connections from a listening {@link BluetoothServerSocket}.
     * <p>Valid RFCOMM channels are in range 1 to 30.
     * <p>To auto assign a channel without creating a SDP record use
     * {@link #SOCKET_CHANNEL_AUTO_STATIC_NO_SDP} as channel number.
     *
     * @param channel RFCOMM channel to listen on
     * @param mitm enforce person-in-the-middle protection for authentication.
     * @param min16DigitPin enforce a pin key length og minimum 16 digit for sec mode 2
     * connections.
     * @return a listening RFCOMM BluetoothServerSocket
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions, or channel in use.
     * @hide
     */
    @UnsupportedAppUsage
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothServerSocket listenUsingRfcommOn(int channel, boolean mitm,
            boolean min16DigitPin) throws IOException {
        BluetoothServerSocket socket =
                new BluetoothServerSocket(BluetoothSocket.TYPE_RFCOMM, true, true, channel, mitm,
                        min16DigitPin);
        int errno = socket.mSocket.bindListen();
        if (channel == SOCKET_CHANNEL_AUTO_STATIC_NO_SDP) {
            socket.setChannel(socket.mSocket.getPort());
        }
        if (errno != 0) {
            //TODO(BT): Throw the same exception error code
            // that the previous code was using.
            //socket.mSocket.throwErrnoNative(errno);
            throw new IOException("Error: " + errno);
        }
        return socket;
    }

    /**
     * Create a listening, secure RFCOMM Bluetooth socket with Service Record.
     * <p>A remote device connecting to this socket will be authenticated and
     * communication on this socket will be encrypted.
     * <p>Use {@link BluetoothServerSocket#accept} to retrieve incoming
     * connections from a listening {@link BluetoothServerSocket}.
     * <p>The system will assign an unused RFCOMM channel to listen on.
     * <p>The system will also register a Service Discovery
     * Protocol (SDP) record with the local SDP server containing the specified
     * UUID, service name, and auto-assigned channel. Remote Bluetooth devices
     * can use the same UUID to query our SDP server and discover which channel
     * to connect to. This SDP record will be removed when this socket is
     * closed, or if this application closes unexpectedly.
     * <p>Use {@link BluetoothDevice#createRfcommSocketToServiceRecord} to
     * connect to this socket from another device using the same {@link UUID}.
     *
     * @param name service name for SDP record
     * @param uuid uuid for SDP record
     * @return a listening RFCOMM BluetoothServerSocket
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions, or channel in use.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothServerSocket listenUsingRfcommWithServiceRecord(String name, UUID uuid)
            throws IOException {
        return createNewRfcommSocketAndRecord(name, uuid, true, true);
    }

    /**
     * Requests the framework to start an RFCOMM socket server which listens based on the provided
     * {@code name} and {@code uuid}.
     * <p>
     * Incoming connections will cause the system to start the component described in the {@link
     * PendingIntent}, {@code pendingIntent}. After the component is started, it should obtain a
     * {@link BluetoothAdapter} and retrieve the {@link BluetoothSocket} via {@link
     * #retrieveConnectedRfcommSocket(UUID)}.
     * <p>
     * An application may register multiple RFCOMM listeners. It is recommended to set the extra
     * field {@link #EXTRA_RFCOMM_LISTENER_ID} to help determine which service record the incoming
     * {@link BluetoothSocket} is using.
     * <p>
     * The provided {@link PendingIntent} must be created with the {@link
     * PendingIntent#FLAG_IMMUTABLE} flag.
     *
     * @param name service name for SDP record
     * @param uuid uuid for SDP record
     * @param pendingIntent component which is called when a new RFCOMM connection is available
     * @return a status code from {@link BluetoothStatusCodes}
     * @throws IllegalArgumentException if {@code pendingIntent} is not created with the {@link
     *         PendingIntent#FLAG_IMMUTABLE} flag.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    @RfcommListenerResult
    public int startRfcommServer(@NonNull String name, @NonNull UUID uuid,
            @NonNull PendingIntent pendingIntent) {
        if (!pendingIntent.isImmutable()) {
            throw new IllegalArgumentException("The provided PendingIntent is not immutable");
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                mService.startRfcommListener(
                        name, new ParcelUuid(uuid), pendingIntent, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout())
                    .getValue(BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, "Failed to transact RFCOMM listener start request", e);
            return BluetoothStatusCodes.ERROR_TIMEOUT;
        } finally {
            mServiceLock.readLock().unlock();
        }
        return BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND;
    }

    /**
     * Closes the RFCOMM socket server listening on the given SDP record name and UUID. This can be
     * called by applications after calling {@link #startRfcommServer(String, UUID,
     * PendingIntent)} to stop listening for incoming RFCOMM connections.
     *
     * @param uuid uuid for SDP record
     * @return a status code from {@link BluetoothStatusCodes}
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @RfcommListenerResult
    public int stopRfcommServer(@NonNull UUID uuid) {
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                mService.stopRfcommListener(new ParcelUuid(uuid), mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout())
                    .getValue(BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, "Failed to transact RFCOMM listener stop request", e);
            return BluetoothStatusCodes.ERROR_TIMEOUT;
        } finally {
            mServiceLock.readLock().unlock();
        }
        return BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND;
    }

    /**
     * Retrieves a connected {@link BluetoothSocket} for the given service record from a RFCOMM
     * listener which was registered with {@link #startRfcommServer(String, UUID, PendingIntent)}.
     * <p>
     * This method should be called by the component started by the {@link PendingIntent} which was
     * registered during the call to {@link #startRfcommServer(String, UUID, PendingIntent)} in
     * order to retrieve the socket.
     *
     * @param uuid the same UUID used to register the listener previously
     * @return a connected {@link BluetoothSocket} or {@code null} if no socket is available
     * @throws IllegalStateException if the socket could not be retrieved because the application is
     *         trying to obtain a socket for a listener it did not register (incorrect {@code
     *         uuid}).
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @NonNull BluetoothSocket retrieveConnectedRfcommSocket(@NonNull UUID uuid) {
        IncomingRfcommSocketInfo socketInfo = null;

        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<IncomingRfcommSocketInfo> recv =
                        SynchronousResultReceiver.get();
                mService.retrievePendingSocketForServiceRecord(new ParcelUuid(uuid),
                        mAttributionSource, recv);
                socketInfo = recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(null);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            return null;
        } finally {
            mServiceLock.readLock().unlock();
        }
        if (socketInfo == null) {
            return null;
        }

        switch (socketInfo.status) {
            case BluetoothStatusCodes.SUCCESS:
                try {
                    return BluetoothSocket.createSocketFromOpenFd(
                            socketInfo.pfd,
                            socketInfo.bluetoothDevice,
                            new ParcelUuid(uuid));
                } catch (IOException e) {
                    return null;
                }
            case BluetoothStatusCodes.RFCOMM_LISTENER_OPERATION_FAILED_DIFFERENT_APP:
                throw new IllegalStateException(
                        String.format(
                                "RFCOMM listener for UUID %s was not registered by this app",
                                uuid));
            case BluetoothStatusCodes.RFCOMM_LISTENER_NO_SOCKET_AVAILABLE:
                return null;
            default:
                Log.e(TAG,
                        String.format(
                                "Unexpected result: (%d), from the adapter service while retrieving"
                                        + " an rfcomm socket",
                                socketInfo.status));
                return null;
        }
    }

    /**
     * Create a listening, insecure RFCOMM Bluetooth socket with Service Record.
     * <p>The link key is not required to be authenticated, i.e the communication may be
     * vulnerable to Person In the Middle attacks. For Bluetooth 2.1 devices,
     * the link will be encrypted, as encryption is mandatory.
     * For legacy devices (pre Bluetooth 2.1 devices) the link will not
     * be encrypted. Use {@link #listenUsingRfcommWithServiceRecord}, if an
     * encrypted and authenticated communication channel is desired.
     * <p>Use {@link BluetoothServerSocket#accept} to retrieve incoming
     * connections from a listening {@link BluetoothServerSocket}.
     * <p>The system will assign an unused RFCOMM channel to listen on.
     * <p>The system will also register a Service Discovery
     * Protocol (SDP) record with the local SDP server containing the specified
     * UUID, service name, and auto-assigned channel. Remote Bluetooth devices
     * can use the same UUID to query our SDP server and discover which channel
     * to connect to. This SDP record will be removed when this socket is
     * closed, or if this application closes unexpectedly.
     * <p>Use {@link BluetoothDevice#createInsecureRfcommSocketToServiceRecord} to
     * connect to this socket from another device using the same {@link UUID}.
     *
     * @param name service name for SDP record
     * @param uuid uuid for SDP record
     * @return a listening RFCOMM BluetoothServerSocket
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions, or channel in use.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothServerSocket listenUsingInsecureRfcommWithServiceRecord(String name, UUID uuid)
            throws IOException {
        return createNewRfcommSocketAndRecord(name, uuid, false, false);
    }

    /**
     * Create a listening, encrypted,
     * RFCOMM Bluetooth socket with Service Record.
     * <p>The link will be encrypted, but the link key is not required to be authenticated
     * i.e the communication is vulnerable to Person In the Middle attacks. Use
     * {@link #listenUsingRfcommWithServiceRecord}, to ensure an authenticated link key.
     * <p> Use this socket if authentication of link key is not possible.
     * For example, for Bluetooth 2.1 devices, if any of the devices does not have
     * an input and output capability or just has the ability to display a numeric key,
     * a secure socket connection is not possible and this socket can be used.
     * Use {@link #listenUsingInsecureRfcommWithServiceRecord}, if encryption is not required.
     * For Bluetooth 2.1 devices, the link will be encrypted, as encryption is mandatory.
     * For more details, refer to the Security Model section 5.2 (vol 3) of
     * Bluetooth Core Specification version 2.1 + EDR.
     * <p>Use {@link BluetoothServerSocket#accept} to retrieve incoming
     * connections from a listening {@link BluetoothServerSocket}.
     * <p>The system will assign an unused RFCOMM channel to listen on.
     * <p>The system will also register a Service Discovery
     * Protocol (SDP) record with the local SDP server containing the specified
     * UUID, service name, and auto-assigned channel. Remote Bluetooth devices
     * can use the same UUID to query our SDP server and discover which channel
     * to connect to. This SDP record will be removed when this socket is
     * closed, or if this application closes unexpectedly.
     * <p>Use {@link BluetoothDevice#createRfcommSocketToServiceRecord} to
     * connect to this socket from another device using the same {@link UUID}.
     *
     * @param name service name for SDP record
     * @param uuid uuid for SDP record
     * @return a listening RFCOMM BluetoothServerSocket
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions, or channel in use.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothServerSocket listenUsingEncryptedRfcommWithServiceRecord(String name, UUID uuid)
            throws IOException {
        return createNewRfcommSocketAndRecord(name, uuid, false, true);
    }

    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    private BluetoothServerSocket createNewRfcommSocketAndRecord(String name, UUID uuid,
            boolean auth, boolean encrypt) throws IOException {
        BluetoothServerSocket socket;
        socket = new BluetoothServerSocket(BluetoothSocket.TYPE_RFCOMM, auth, encrypt,
                new ParcelUuid(uuid));
        socket.setServiceName(name);
        int errno = socket.mSocket.bindListen();
        if (errno != 0) {
            //TODO(BT): Throw the same exception error code
            // that the previous code was using.
            //socket.mSocket.throwErrnoNative(errno);
            throw new IOException("Error: " + errno);
        }
        return socket;
    }

    /**
     * Construct an unencrypted, unauthenticated, RFCOMM server socket.
     * Call #accept to retrieve connections to this socket.
     *
     * @return An RFCOMM BluetoothServerSocket
     * @throws IOException On error, for example Bluetooth not available, or insufficient
     * permissions.
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothServerSocket listenUsingInsecureRfcommOn(int port) throws IOException {
        BluetoothServerSocket socket =
                new BluetoothServerSocket(BluetoothSocket.TYPE_RFCOMM, false, false, port);
        int errno = socket.mSocket.bindListen();
        if (port == SOCKET_CHANNEL_AUTO_STATIC_NO_SDP) {
            socket.setChannel(socket.mSocket.getPort());
        }
        if (errno != 0) {
            //TODO(BT): Throw the same exception error code
            // that the previous code was using.
            //socket.mSocket.throwErrnoNative(errno);
            throw new IOException("Error: " + errno);
        }
        return socket;
    }

    /**
     * Construct an encrypted, authenticated, L2CAP server socket.
     * Call #accept to retrieve connections to this socket.
     * <p>To auto assign a port without creating a SDP record use
     * {@link #SOCKET_CHANNEL_AUTO_STATIC_NO_SDP} as port number.
     *
     * @param port the PSM to listen on
     * @param mitm enforce person-in-the-middle protection for authentication.
     * @param min16DigitPin enforce a pin key length og minimum 16 digit for sec mode 2
     * connections.
     * @return An L2CAP BluetoothServerSocket
     * @throws IOException On error, for example Bluetooth not available, or insufficient
     * permissions.
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothServerSocket listenUsingL2capOn(int port, boolean mitm, boolean min16DigitPin)
            throws IOException {
        BluetoothServerSocket socket =
                new BluetoothServerSocket(BluetoothSocket.TYPE_L2CAP, true, true, port, mitm,
                        min16DigitPin);
        int errno = socket.mSocket.bindListen();
        if (port == SOCKET_CHANNEL_AUTO_STATIC_NO_SDP) {
            int assignedChannel = socket.mSocket.getPort();
            if (DBG) Log.d(TAG, "listenUsingL2capOn: set assigned channel to " + assignedChannel);
            socket.setChannel(assignedChannel);
        }
        if (errno != 0) {
            //TODO(BT): Throw the same exception error code
            // that the previous code was using.
            //socket.mSocket.throwErrnoNative(errno);
            throw new IOException("Error: " + errno);
        }
        return socket;
    }

    /**
     * Construct an encrypted, authenticated, L2CAP server socket.
     * Call #accept to retrieve connections to this socket.
     * <p>To auto assign a port without creating a SDP record use
     * {@link #SOCKET_CHANNEL_AUTO_STATIC_NO_SDP} as port number.
     *
     * @param port the PSM to listen on
     * @return An L2CAP BluetoothServerSocket
     * @throws IOException On error, for example Bluetooth not available, or insufficient
     * permissions.
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothServerSocket listenUsingL2capOn(int port) throws IOException {
        return listenUsingL2capOn(port, false, false);
    }

    /**
     * Construct an insecure L2CAP server socket.
     * Call #accept to retrieve connections to this socket.
     * <p>To auto assign a port without creating a SDP record use
     * {@link #SOCKET_CHANNEL_AUTO_STATIC_NO_SDP} as port number.
     *
     * @param port the PSM to listen on
     * @return An L2CAP BluetoothServerSocket
     * @throws IOException On error, for example Bluetooth not available, or insufficient
     * permissions.
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothServerSocket listenUsingInsecureL2capOn(int port) throws IOException {
        Log.d(TAG, "listenUsingInsecureL2capOn: port=" + port);
        BluetoothServerSocket socket =
                new BluetoothServerSocket(BluetoothSocket.TYPE_L2CAP, false, false, port, false,
                                          false);
        int errno = socket.mSocket.bindListen();
        if (port == SOCKET_CHANNEL_AUTO_STATIC_NO_SDP) {
            int assignedChannel = socket.mSocket.getPort();
            if (DBG) {
                Log.d(TAG, "listenUsingInsecureL2capOn: set assigned channel to "
                        + assignedChannel);
            }
            socket.setChannel(assignedChannel);
        }
        if (errno != 0) {
            //TODO(BT): Throw the same exception error code
            // that the previous code was using.
            //socket.mSocket.throwErrnoNative(errno);
            throw new IOException("Error: " + errno);
        }
        return socket;

    }

    /**
     * Read the local Out of Band Pairing Data
     *
     * @return Pair<byte[], byte[]> of Hash and Randomizer
     * @hide
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("AndroidFrameworkRequiresPermission")
    public Pair<byte[], byte[]> readOutOfBandData() {
        return null;
    }

    /**
     * Get the profile proxy object associated with the profile.
     *
     * <p>Profile can be one of {@link BluetoothProfile#HEADSET}, {@link BluetoothProfile#A2DP},
     * {@link BluetoothProfile#GATT}, {@link BluetoothProfile#HEARING_AID}, or {@link
     * BluetoothProfile#GATT_SERVER}. Clients must implement {@link
     * BluetoothProfile.ServiceListener} to get notified of the connection status and to get the
     * proxy object.
     *
     * @param context Context of the application
     * @param listener The service Listener for connection callbacks.
     * @param profile The Bluetooth profile; either {@link BluetoothProfile#HEADSET},
     * {@link BluetoothProfile#A2DP}, {@link BluetoothProfile#GATT}, {@link
     * BluetoothProfile#HEARING_AID} or {@link BluetoothProfile#GATT_SERVER}.
     * @return true on success, false on error
     */
    @SuppressLint({
            "AndroidFrameworkRequiresPermission",
            "AndroidFrameworkBluetoothPermission"
    })
    public boolean getProfileProxy(Context context, BluetoothProfile.ServiceListener listener,
            int profile) {
        if (context == null || listener == null) {
            return false;
        }

        if (profile == BluetoothProfile.HEADSET) {
            BluetoothHeadset headset = new BluetoothHeadset(context, listener, this);
            return true;
        } else if (profile == BluetoothProfile.A2DP) {
            BluetoothA2dp a2dp = new BluetoothA2dp(context, listener, this);
            return true;
        } else if (profile == BluetoothProfile.A2DP_SINK) {
            BluetoothA2dpSink a2dpSink = new BluetoothA2dpSink(context, listener, this);
            return true;
        } else if (profile == BluetoothProfile.AVRCP_CONTROLLER) {
            BluetoothAvrcpController avrcp = new BluetoothAvrcpController(context, listener, this);
            return true;
        } else if (profile == BluetoothProfile.HID_HOST) {
            BluetoothHidHost iDev = new BluetoothHidHost(context, listener, this);
            return true;
        } else if (profile == BluetoothProfile.PAN) {
            BluetoothPan pan = new BluetoothPan(context, listener, this);
            return true;
        } else if (profile == BluetoothProfile.PBAP) {
            BluetoothPbap pbap = new BluetoothPbap(context, listener, this);
            return true;
        } else if (profile == BluetoothProfile.HEALTH) {
            Log.e(TAG, "getProfileProxy(): BluetoothHealth is deprecated");
            return false;
        } else if (profile == BluetoothProfile.MAP) {
            BluetoothMap map = new BluetoothMap(context, listener, this);
            return true;
        } else if (profile == BluetoothProfile.HEADSET_CLIENT) {
            BluetoothHeadsetClient headsetClient =
                    new BluetoothHeadsetClient(context, listener, this);
            return true;
        } else if (profile == BluetoothProfile.SAP) {
            BluetoothSap sap = new BluetoothSap(context, listener, this);
            return true;
        } else if (profile == BluetoothProfile.PBAP_CLIENT) {
            BluetoothPbapClient pbapClient = new BluetoothPbapClient(context, listener, this);
            return true;
        } else if (profile == BluetoothProfile.MAP_CLIENT) {
            BluetoothMapClient mapClient = new BluetoothMapClient(context, listener, this);
            return true;
        } else if (profile == BluetoothProfile.HID_DEVICE) {
            BluetoothHidDevice hidDevice = new BluetoothHidDevice(context, listener, this);
            return true;
        } else if (profile == BluetoothProfile.HAP_CLIENT) {
            BluetoothHapClient HapClient = new BluetoothHapClient(context, listener);
            return true;
        } else if (profile == BluetoothProfile.HEARING_AID) {
            if (isHearingAidProfileSupported()) {
                BluetoothHearingAid hearingAid = new BluetoothHearingAid(context, listener, this);
                return true;
            }
            return false;
        } else if (profile == BluetoothProfile.LE_AUDIO) {
            BluetoothLeAudio leAudio = new BluetoothLeAudio(context, listener, this);
            return true;
        } else if (profile == BluetoothProfile.LE_AUDIO_BROADCAST) {
            BluetoothLeBroadcast leAudio = new BluetoothLeBroadcast(context, listener);
            return true;
        } else if (profile == BluetoothProfile.VOLUME_CONTROL) {
            BluetoothVolumeControl vcs = new BluetoothVolumeControl(context, listener, this);
            return true;
        } else if (profile == BluetoothProfile.CSIP_SET_COORDINATOR) {
            BluetoothCsipSetCoordinator csipSetCoordinator =
                    new BluetoothCsipSetCoordinator(context, listener, this);
            return true;
        } else if (profile == BluetoothProfile.LE_CALL_CONTROL) {
            BluetoothLeCallControl tbs = new BluetoothLeCallControl(context, listener);
            return true;
        } else if (profile == BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT) {
            BluetoothLeBroadcastAssistant leAudioBroadcastAssistant =
                    new BluetoothLeBroadcastAssistant(context, listener);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Close the connection of the profile proxy to the Service.
     *
     * <p>Clients should call this when they are no longer using the proxy obtained from {@link
     * #getProfileProxy}. Profile can be one of {@link BluetoothProfile#HEADSET} or {@link
     * BluetoothProfile#A2DP}
     *
     * @param unusedProfile
     * @param proxy Profile proxy object
     */
    @SuppressLint({"AndroidFrameworkRequiresPermission", "AndroidFrameworkBluetoothPermission"})
    public void closeProfileProxy(int unusedProfile, BluetoothProfile proxy) {
        if (proxy == null) {
            return;
        }
        proxy.close();
    }

    private static final IBluetoothManagerCallback sManagerCallback =
            new IBluetoothManagerCallback.Stub() {
                public void onBluetoothServiceUp(IBluetooth bluetoothService) {
                    if (DBG) {
                        Log.d(TAG, "onBluetoothServiceUp: " + bluetoothService);
                    }

                    synchronized (sServiceLock) {
                        sService = bluetoothService;
                        for (IBluetoothManagerCallback cb : sProxyServiceStateCallbacks.keySet()) {
                            try {
                                if (cb != null) {
                                    cb.onBluetoothServiceUp(bluetoothService);
                                } else {
                                    Log.d(TAG, "onBluetoothServiceUp: cb is null!");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "", e);
                            }
                        }
                    }
                }

                public void onBluetoothServiceDown() {
                    if (DBG) {
                        Log.d(TAG, "onBluetoothServiceDown");
                    }

                    synchronized (sServiceLock) {
                        sService = null;
                        for (IBluetoothManagerCallback cb : sProxyServiceStateCallbacks.keySet()) {
                            try {
                                if (cb != null) {
                                    cb.onBluetoothServiceDown();
                                } else {
                                    Log.d(TAG, "onBluetoothServiceDown: cb is null!");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "", e);
                            }
                        }
                    }
                }

                public void onBrEdrDown() {
                    if (VDBG) {
                        Log.i(TAG, "onBrEdrDown");
                    }

                    synchronized (sServiceLock) {
                        for (IBluetoothManagerCallback cb : sProxyServiceStateCallbacks.keySet()) {
                            try {
                                if (cb != null) {
                                    cb.onBrEdrDown();
                                } else {
                                    Log.d(TAG, "onBrEdrDown: cb is null!");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "", e);
                            }
                        }
                    }
                }
            };

    private final IBluetoothManagerCallback mManagerCallback =
            new IBluetoothManagerCallback.Stub() {
                public void onBluetoothServiceUp(@NonNull IBluetooth bluetoothService) {
                    requireNonNull(bluetoothService, "bluetoothService cannot be null");
                    mServiceLock.writeLock().lock();
                    try {
                        mService = bluetoothService;
                    } finally {
                        // lock downgrade is possible in ReentrantReadWriteLock
                        mServiceLock.readLock().lock();
                        mServiceLock.writeLock().unlock();
                    }
                    try {
                        synchronized (mMetadataListeners) {
                            mMetadataListeners.forEach((device, pair) -> {
                                try {
                                    final SynchronousResultReceiver recv =
                                        SynchronousResultReceiver.get();
                                    mService.registerMetadataListener(mBluetoothMetadataListener,
                                            device, mAttributionSource, recv);
                                    recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(null);
                                } catch (RemoteException | TimeoutException e) {
                                    Log.e(TAG, "Failed to register metadata listener", e);
                                    Log.e(TAG, e.toString() + "\n"
                                            + Log.getStackTraceString(new Throwable()));
                                }
                            });
                        }
                        synchronized (mAudioProfilesChangedCallbackExecutorMap) {
                            if (!mAudioProfilesChangedCallbackExecutorMap.isEmpty()) {
                                try {
                                    final SynchronousResultReceiver recv =
                                        SynchronousResultReceiver.get();
                                    mService.registerPreferredAudioProfilesChangedCallback(
                                            mPreferredAudioProfilesChangedCallback,
                                            mAttributionSource, recv);
                                    recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(
                                            BluetoothStatusCodes.ERROR_UNKNOWN);
                                } catch (RemoteException | TimeoutException e) {
                                    Log.e(TAG, "onBluetoothServiceUp: Failed to register bluetooth"
                                            + "connection callback", e);
                                }
                            }
                        }
                        synchronized (mBluetoothQualityReportReadyCallbackExecutorMap) {
                            if (!mBluetoothQualityReportReadyCallbackExecutorMap.isEmpty()) {
                                try {
                                    final SynchronousResultReceiver recv =
                                        SynchronousResultReceiver.get();
                                    mService.registerBluetoothQualityReportReadyCallback(
                                            mBluetoothQualityReportReadyCallback,
                                            mAttributionSource, recv);
                                    recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(
                                            BluetoothStatusCodes.ERROR_UNKNOWN);
                                } catch (RemoteException | TimeoutException e) {
                                    Log.e(TAG, "onBluetoothServiceUp: Failed to register bluetooth"
                                            + "quality report callback", e);
                                }
                            }
                        }
                        synchronized (mBluetoothConnectionCallbackExecutorMap) {
                            if (!mBluetoothConnectionCallbackExecutorMap.isEmpty()) {
                                try {
                                    final SynchronousResultReceiver recv =
                                            SynchronousResultReceiver.get();
                                    mService.registerBluetoothConnectionCallback(
                                            mConnectionCallback,
                                            mAttributionSource, recv);
                                    recv.awaitResultNoInterrupt(getSyncTimeout())
                                            .getValue(null);
                                } catch (RemoteException | TimeoutException e) {
                                    Log.e(TAG, "onBluetoothServiceUp: Failed to register "
                                            + "bluetooth connection callback", e);
                                }
                            }
                        }
                    } finally {
                        mServiceLock.readLock().unlock();
                    }
                }

                public void onBluetoothServiceDown() {
                    mServiceLock.writeLock().lock();
                    try {
                        mService = null;
                        if (mLeScanClients != null) {
                            mLeScanClients.clear();
                        }
                        if (mBluetoothLeAdvertiser != null) {
                            mBluetoothLeAdvertiser.cleanup();
                        }
                        if (mBluetoothLeScanner != null) {
                            mBluetoothLeScanner.cleanup();
                        }
                    } finally {
                        mServiceLock.writeLock().unlock();
                    }
                }

                public void onBrEdrDown() {
                }
            };

    /**
     * Enable the Bluetooth Adapter, but don't auto-connect devices
     * and don't persist state. Only for use by system applications.
     *
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean enableNoAutoConnect() {
        if (isEnabled()) {
            if (DBG) {
                Log.d(TAG, "enableNoAutoConnect(): BT already enabled!");
            }
            return true;
        }
        try {
            return mManagerService.enableNoAutoConnect(mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
        return false;
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            BluetoothStatusCodes.ERROR_UNKNOWN,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED,
            BluetoothStatusCodes.ERROR_ANOTHER_ACTIVE_OOB_REQUEST,
    })
    public @interface OobError {}

    /**
     * Provides callback methods for receiving {@link OobData} from the host stack, as well as an
     * error interface in order to allow the caller to determine next steps based on the {@code
     * ErrorCode}.
     *
     * @hide
     */
    @SystemApi
    public interface OobDataCallback {
        /**
         * Handles the {@link OobData} received from the host stack.
         *
         * @param transport - whether the {@link OobData} is generated for LE or Classic.
         * @param oobData - data generated in the host stack(LE) or controller (Classic)
         */
        void onOobData(@Transport int transport, @NonNull OobData oobData);

        /**
         * Provides feedback when things don't go as expected.
         *
         * @param errorCode - the code describing the type of error that occurred.
         */
        void onError(@OobError int errorCode);
    }

    /**
     * Wraps an AIDL interface around an {@link OobDataCallback} interface.
     *
     * @see {@link IBluetoothOobDataCallback} for interface definition.
     *
     * @hide
     */
    public class WrappedOobDataCallback extends IBluetoothOobDataCallback.Stub {
        private final OobDataCallback mCallback;
        private final Executor mExecutor;

        /**
         * @param callback - object to receive {@link OobData} must be a non null argument
         *
         * @throws NullPointerException if the callback is null.
         */
        WrappedOobDataCallback(@NonNull OobDataCallback callback,
                @NonNull @CallbackExecutor Executor executor) {
            requireNonNull(callback);
            requireNonNull(executor);
            mCallback = callback;
            mExecutor = executor;
        }
        /**
         * Wrapper function to relay to the {@link OobDataCallback#onOobData}
         *
         * @param transport - whether the {@link OobData} is generated for LE or Classic.
         * @param oobData - data generated in the host stack(LE) or controller (Classic)
         *
         * @hide
         */
        public void onOobData(@Transport int transport, @NonNull OobData oobData) {
            mExecutor.execute(new Runnable() {
                public void run() {
                    mCallback.onOobData(transport, oobData);
                }
            });
        }
        /**
         * Wrapper function to relay to the {@link OobDataCallback#onError}
         *
         * @param errorCode - the code descibing the type of error that occurred.
         *
         * @hide
         */
        public void onError(@OobError int errorCode) {
            mExecutor.execute(new Runnable() {
                public void run() {
                    mCallback.onError(errorCode);
                }
            });
        }
    }

    /**
     * Fetches a secret data value that can be used for a secure and simple pairing experience.
     *
     * <p>This is the Local Out of Band data the comes from the
     *
     * <p>This secret is the local Out of Band data.  This data is used to securely and quickly
     * pair two devices with minimal user interaction.
     *
     * <p>For example, this secret can be transferred to a remote device out of band (meaning any
     * other way besides using bluetooth).  Once the remote device finds this device using the
     * information given in the data, such as the PUBLIC ADDRESS, the remote device could then
     * connect to this device using this secret when the pairing sequenece asks for the secret.
     * This device will respond by automatically accepting the pairing due to the secret being so
     * trustworthy.
     *
     * @param transport - provide type of transport (e.g. LE or Classic).
     * @param callback - target object to receive the {@link OobData} value.
     *
     * @throws NullPointerException if callback is null.
     * @throws IllegalArgumentException if the transport is not valid.
     *
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public void generateLocalOobData(@Transport int transport,
            @NonNull @CallbackExecutor Executor executor, @NonNull OobDataCallback callback) {
        if (transport != BluetoothDevice.TRANSPORT_BREDR && transport
                != BluetoothDevice.TRANSPORT_LE) {
            throw new IllegalArgumentException("Invalid transport '" + transport + "'!");
        }
        requireNonNull(callback);
        if (!isEnabled()) {
            Log.w(TAG, "generateLocalOobData(): Adapter isn't enabled!");
            callback.onError(BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED);
        } else {
            mServiceLock.readLock().lock();
            try {
                if (mService != null) {
                    final SynchronousResultReceiver recv = SynchronousResultReceiver.get();
                    mService.generateLocalOobData(transport, new WrappedOobDataCallback(callback,
                            executor), mAttributionSource, recv);
                    recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(null);
                }
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } finally {
                mServiceLock.readLock().unlock();
            }
        }
    }

    /**
     * Enable control of the Bluetooth Adapter for a single application.
     *
     * <p>Some applications need to use Bluetooth for short periods of time to
     * transfer data but don't want all the associated implications like
     * automatic connection to headsets etc.
     *
     * <p> Multiple applications can call this. This is reference counted and
     * Bluetooth disabled only when no one else is using it. There will be no UI
     * shown to the user while bluetooth is being enabled. Any user action will
     * override this call. For example, if user wants Bluetooth on and the last
     * user of this API wanted to disable Bluetooth, Bluetooth will not be
     * turned off.
     *
     * <p> This API is only meant to be used by internal applications. Third
     * party applications but use {@link #enable} and {@link #disable} APIs.
     *
     * <p> If this API returns true, it means the callback will be called.
     * The callback will be called with the current state of Bluetooth.
     * If the state is not what was requested, an internal error would be the
     * reason. If Bluetooth is already on and if this function is called to turn
     * it on, the api will return true and a callback will be called.
     *
     * @param on True for on, false for off.
     * @param callback The callback to notify changes to the state.
     * @hide
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("AndroidFrameworkRequiresPermission")
    public boolean changeApplicationBluetoothState(boolean on,
            BluetoothStateChangeCallback callback) {
        return false;
    }

    /**
     * @hide
     */
    public interface BluetoothStateChangeCallback {
        /**
         * @hide
         */
        void onBluetoothStateChange(boolean on);
    }

    /**
     * @hide
     */
    public class StateChangeCallbackWrapper extends IBluetoothStateChangeCallback.Stub {
        private BluetoothStateChangeCallback mCallback;

        StateChangeCallbackWrapper(BluetoothStateChangeCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onBluetoothStateChange(boolean on) {
            mCallback.onBluetoothStateChange(on);
        }
    }

    private Set<BluetoothDevice> toDeviceSet(List<BluetoothDevice> devices) {
        Set<BluetoothDevice> deviceSet = new HashSet<BluetoothDevice>(devices);
        return Collections.unmodifiableSet(deviceSet);
    }

    @SuppressLint("GenericException")
    protected void finalize() throws Throwable {
        try {
            removeServiceStateCallback(mManagerCallback);
        } finally {
            super.finalize();
        }
    }

    /**
     * Validate a String Bluetooth address, such as "00:43:A8:23:10:F0"
     * <p>Alphabetic characters must be uppercase to be valid.
     *
     * @param address Bluetooth address as string
     * @return true if the address is valid, false otherwise
     */
    public static boolean checkBluetoothAddress(String address) {
        if (address == null || address.length() != ADDRESS_LENGTH) {
            return false;
        }
        for (int i = 0; i < ADDRESS_LENGTH; i++) {
            char c = address.charAt(i);
            switch (i % 3) {
                case 0:
                case 1:
                    if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F')) {
                        // hex character, OK
                        break;
                    }
                    return false;
                case 2:
                    if (c == ':') {
                        break;  // OK
                    }
                    return false;
            }
        }
        return true;
    }

    /**
     * Determines whether a String Bluetooth address, such as "F0:43:A8:23:10:00"
     * is a RANDOM STATIC address.
     *
     * RANDOM STATIC: (addr & 0xC0) == 0xC0
     * RANDOM RESOLVABLE: (addr &  0xC0) == 0x40
     * RANDOM non-RESOLVABLE: (addr &  0xC0) == 0x00
     *
     * @param address Bluetooth address as string
     * @return true if the 2 Most Significant Bits of the address equals 0xC0.
     *
     * @hide
     */
    public static boolean isAddressRandomStatic(@NonNull String address) {
        requireNonNull(address);
        return checkBluetoothAddress(address)
                && (Integer.parseInt(address.split(":")[0], 16) & 0xC0) == 0xC0;
    }

    /** {@hide} */
    @UnsupportedAppUsage
    @RequiresNoPermission
    public IBluetoothManager getBluetoothManager() {
        return mManagerService;
    }

    /** {@hide} */
    @RequiresNoPermission
    public AttributionSource getAttributionSource() {
        return mAttributionSource;
    }

    @GuardedBy("sServiceLock")
    private static final WeakHashMap<IBluetoothManagerCallback, Void> sProxyServiceStateCallbacks =
            new WeakHashMap<>();

    /*package*/ IBluetooth getBluetoothService() {
        synchronized (sServiceLock) {
            return sService;
        }
    }

    /**
     * Registers a IBluetoothManagerCallback and returns the cached
     * Bluetooth service proxy object.
     *
     * TODO: rename this API to registerBlueoothManagerCallback or something?
     * the current name does not match what it does very well.
     *
     * /
    @UnsupportedAppUsage
    /*package*/ IBluetooth getBluetoothService(IBluetoothManagerCallback cb) {
        requireNonNull(cb);
        synchronized (sServiceLock) {
            sProxyServiceStateCallbacks.put(cb, null);
            registerOrUnregisterAdapterLocked();
            return sService;
        }
    }

    /*package*/ void removeServiceStateCallback(IBluetoothManagerCallback cb) {
        requireNonNull(cb);
        synchronized (sServiceLock) {
            sProxyServiceStateCallbacks.remove(cb);
            registerOrUnregisterAdapterLocked();
        }
    }

    /**
     * Handle registering (or unregistering) a single process-wide
     * {@link IBluetoothManagerCallback} based on the presence of local
     * {@link #sProxyServiceStateCallbacks} clients.
     */
    @GuardedBy("sServiceLock")
    private void registerOrUnregisterAdapterLocked() {
        final boolean isRegistered = sServiceRegistered;
        final boolean wantRegistered = !sProxyServiceStateCallbacks.isEmpty();

        if (isRegistered != wantRegistered) {
            if (wantRegistered) {
                try {
                    sService = mManagerService.registerAdapter(sManagerCallback);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            } else {
                try {
                    mManagerService.unregisterAdapter(sManagerCallback);
                    sService = null;
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            sServiceRegistered = wantRegistered;
        }
    }

    /**
     * Callback interface used to deliver LE scan results.
     *
     * @see #startLeScan(LeScanCallback)
     * @see #startLeScan(UUID[], LeScanCallback)
     */
    public interface LeScanCallback {
        /**
         * Callback reporting an LE device found during a device scan initiated
         * by the {@link BluetoothAdapter#startLeScan} function.
         *
         * @param device Identifies the remote device
         * @param rssi The RSSI value for the remote device as reported by the Bluetooth hardware. 0
         * if no RSSI value is available.
         * @param scanRecord The content of the advertisement record offered by the remote device.
         */
        void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord);
    }

    /**
     * Register a callback to receive events whenever the bluetooth stack goes down and back up,
     * e.g. in the event the bluetooth is turned off/on via settings.
     *
     * If the bluetooth stack is currently up, there will not be an initial callback call.
     * You can use the return value as an indication of this being the case.
     *
     * Callbacks will be delivered on a binder thread.
     *
     * @return whether bluetooth is already up currently
     *
     * @hide
     */
    @RequiresNoPermission
    public boolean registerServiceLifecycleCallback(@NonNull ServiceLifecycleCallback callback) {
        return getBluetoothService(callback.mRemote) != null;
    }

    /**
     * Unregister a callback registered via {@link #registerServiceLifecycleCallback}
     *
     * @hide
     */
    @RequiresNoPermission
    public void unregisterServiceLifecycleCallback(@NonNull ServiceLifecycleCallback callback) {
        removeServiceStateCallback(callback.mRemote);
    }

    /**
     * A callback for {@link #registerServiceLifecycleCallback}
     *
     * @hide
     */
    public abstract static class ServiceLifecycleCallback {

        /** Called when the bluetooth stack is up */
        public abstract void onBluetoothServiceUp();

        /** Called when the bluetooth stack is down */
        public abstract void onBluetoothServiceDown();

        IBluetoothManagerCallback mRemote = new IBluetoothManagerCallback.Stub() {
            @Override
            public void onBluetoothServiceUp(IBluetooth bluetoothService) {
                ServiceLifecycleCallback.this.onBluetoothServiceUp();
            }

            @Override
            public void onBluetoothServiceDown() {
                ServiceLifecycleCallback.this.onBluetoothServiceDown();
            }

            @Override
            public void onBrEdrDown() {}
        };
    }

    /**
     * Starts a scan for Bluetooth LE devices.
     *
     * <p>Results of the scan are reported using the
     * {@link LeScanCallback#onLeScan} callback.
     *
     * @param callback the callback LE scan results are delivered
     * @return true, if the scan was started successfully
     * @deprecated use {@link BluetoothLeScanner#startScan(List, ScanSettings, ScanCallback)}
     * instead.
     */
    @Deprecated
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothScanPermission
    @RequiresBluetoothLocationPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    public boolean startLeScan(LeScanCallback callback) {
        return startLeScan(null, callback);
    }

    /**
     * Starts a scan for Bluetooth LE devices, looking for devices that
     * advertise given services.
     *
     * <p>Devices which advertise all specified services are reported using the
     * {@link LeScanCallback#onLeScan} callback.
     *
     * @param serviceUuids Array of services to look for
     * @param callback the callback LE scan results are delivered
     * @return true, if the scan was started successfully
     * @deprecated use {@link BluetoothLeScanner#startScan(List, ScanSettings, ScanCallback)}
     * instead.
     */
    @Deprecated
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothScanPermission
    @RequiresBluetoothLocationPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    public boolean startLeScan(final UUID[] serviceUuids, final LeScanCallback callback) {
        if (DBG) {
            Log.d(TAG, "startLeScan(): " + Arrays.toString(serviceUuids));
        }
        if (callback == null) {
            if (DBG) {
                Log.e(TAG, "startLeScan: null callback");
            }
            return false;
        }
        BluetoothLeScanner scanner = getBluetoothLeScanner();
        if (scanner == null) {
            if (DBG) {
                Log.e(TAG, "startLeScan: cannot get BluetoothLeScanner");
            }
            return false;
        }

        synchronized (mLeScanClients) {
            if (mLeScanClients.containsKey(callback)) {
                if (DBG) {
                    Log.e(TAG, "LE Scan has already started");
                }
                return false;
            }

            try {
                IBluetoothGatt iGatt = mManagerService.getBluetoothGatt();
                if (iGatt == null) {
                    // BLE is not supported
                    return false;
                }

                @SuppressLint("AndroidFrameworkBluetoothPermission")
                ScanCallback scanCallback = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        if (callbackType != ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                            // Should not happen.
                            Log.e(TAG, "LE Scan has already started");
                            return;
                        }
                        ScanRecord scanRecord = result.getScanRecord();
                        if (scanRecord == null) {
                            return;
                        }
                        if (serviceUuids != null) {
                            List<ParcelUuid> uuids = new ArrayList<ParcelUuid>();
                            for (UUID uuid : serviceUuids) {
                                uuids.add(new ParcelUuid(uuid));
                            }
                            List<ParcelUuid> scanServiceUuids = scanRecord.getServiceUuids();
                            if (scanServiceUuids == null || !scanServiceUuids.containsAll(uuids)) {
                                if (DBG) {
                                    Log.d(TAG, "uuids does not match");
                                }
                                return;
                            }
                        }
                        callback.onLeScan(result.getDevice(), result.getRssi(),
                                scanRecord.getBytes());
                    }
                };
                ScanSettings settings = new ScanSettings.Builder().setCallbackType(
                        ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();

                List<ScanFilter> filters = new ArrayList<ScanFilter>();
                if (serviceUuids != null && serviceUuids.length > 0) {
                    // Note scan filter does not support matching an UUID array so we put one
                    // UUID to hardware and match the whole array in callback.
                    ScanFilter filter =
                            new ScanFilter.Builder().setServiceUuid(new ParcelUuid(serviceUuids[0]))
                                    .build();
                    filters.add(filter);
                }
                scanner.startScan(filters, settings, scanCallback);

                mLeScanClients.put(callback, scanCallback);
                return true;

            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }
        return false;
    }

    /**
     * Stops an ongoing Bluetooth LE device scan.
     *
     * @param callback used to identify which scan to stop must be the same handle used to start the
     * scan
     * @deprecated Use {@link BluetoothLeScanner#stopScan(ScanCallback)} instead.
     */
    @Deprecated
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothScanPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    public void stopLeScan(LeScanCallback callback) {
        if (DBG) {
            Log.d(TAG, "stopLeScan()");
        }
        BluetoothLeScanner scanner = getBluetoothLeScanner();
        if (scanner == null) {
            return;
        }
        synchronized (mLeScanClients) {
            ScanCallback scanCallback = mLeScanClients.remove(callback);
            if (scanCallback == null) {
                if (DBG) {
                    Log.d(TAG, "scan not started yet");
                }
                return;
            }
            scanner.stopScan(scanCallback);
        }
    }

    /**
     * Create a secure L2CAP Connection-oriented Channel (CoC) {@link BluetoothServerSocket} and
     * assign a dynamic protocol/service multiplexer (PSM) value. This socket can be used to listen
     * for incoming connections. The supported Bluetooth transport is LE only.
     * <p>A remote device connecting to this socket will be authenticated and communication on this
     * socket will be encrypted.
     * <p>Use {@link BluetoothServerSocket#accept} to retrieve incoming connections from a listening
     * {@link BluetoothServerSocket}.
     * <p>The system will assign a dynamic PSM value. This PSM value can be read from the {@link
     * BluetoothServerSocket#getPsm()} and this value will be released when this server socket is
     * closed, Bluetooth is turned off, or the application exits unexpectedly.
     * <p>The mechanism of disclosing the assigned dynamic PSM value to the initiating peer is
     * defined and performed by the application.
     * <p>Use {@link BluetoothDevice#createL2capChannel(int)} to connect to this server
     * socket from another Android device that is given the PSM value.
     *
     * @return an L2CAP CoC BluetoothServerSocket
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions, or unable to start this CoC
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public @NonNull BluetoothServerSocket listenUsingL2capChannel()
            throws IOException {
        BluetoothServerSocket socket =
                            new BluetoothServerSocket(BluetoothSocket.TYPE_L2CAP_LE, true, true,
                                      SOCKET_CHANNEL_AUTO_STATIC_NO_SDP, false, false);
        int errno = socket.mSocket.bindListen();
        if (errno != 0) {
            throw new IOException("Error: " + errno);
        }

        int assignedPsm = socket.mSocket.getPort();
        if (assignedPsm == 0) {
            throw new IOException("Error: Unable to assign PSM value");
        }
        if (DBG) {
            Log.d(TAG, "listenUsingL2capChannel: set assigned PSM to "
                    + assignedPsm);
        }
        socket.setChannel(assignedPsm);

        return socket;
    }

    /**
     * Create an insecure L2CAP Connection-oriented Channel (CoC) {@link BluetoothServerSocket} and
     * assign a dynamic PSM value. This socket can be used to listen for incoming connections. The
     * supported Bluetooth transport is LE only.
     * <p>The link key is not required to be authenticated, i.e the communication may be vulnerable
     * to person-in-the-middle attacks. Use {@link #listenUsingL2capChannel}, if an encrypted and
     * authenticated communication channel is desired.
     * <p>Use {@link BluetoothServerSocket#accept} to retrieve incoming connections from a listening
     * {@link BluetoothServerSocket}.
     * <p>The system will assign a dynamic protocol/service multiplexer (PSM) value. This PSM value
     * can be read from the {@link BluetoothServerSocket#getPsm()} and this value will be released
     * when this server socket is closed, Bluetooth is turned off, or the application exits
     * unexpectedly.
     * <p>The mechanism of disclosing the assigned dynamic PSM value to the initiating peer is
     * defined and performed by the application.
     * <p>Use {@link BluetoothDevice#createInsecureL2capChannel(int)} to connect to this server
     * socket from another Android device that is given the PSM value.
     *
     * @return an L2CAP CoC BluetoothServerSocket
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions, or unable to start this CoC
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public @NonNull BluetoothServerSocket listenUsingInsecureL2capChannel()
            throws IOException {
        BluetoothServerSocket socket =
                            new BluetoothServerSocket(BluetoothSocket.TYPE_L2CAP_LE, false, false,
                                      SOCKET_CHANNEL_AUTO_STATIC_NO_SDP, false, false);
        int errno = socket.mSocket.bindListen();
        if (errno != 0) {
            throw new IOException("Error: " + errno);
        }

        int assignedPsm = socket.mSocket.getPort();
        if (assignedPsm == 0) {
            throw new IOException("Error: Unable to assign PSM value");
        }
        if (DBG) {
            Log.d(TAG, "listenUsingInsecureL2capChannel: set assigned PSM to "
                    + assignedPsm);
        }
        socket.setChannel(assignedPsm);

        return socket;
    }

    /**
     * Register a {@link #OnMetadataChangedListener} to receive update about metadata
     * changes for this {@link BluetoothDevice}.
     * Registration must be done when Bluetooth is ON and will last until
     * {@link #removeOnMetadataChangedListener(BluetoothDevice)} is called, even when Bluetooth
     * restarted in the middle.
     * All input parameters should not be null or {@link NullPointerException} will be triggered.
     * The same {@link BluetoothDevice} and {@link #OnMetadataChangedListener} pair can only be
     * registered once, double registration would cause {@link IllegalArgumentException}.
     *
     * @param device {@link BluetoothDevice} that will be registered
     * @param executor the executor for listener callback
     * @param listener {@link #OnMetadataChangedListener} that will receive asynchronous callbacks
     * @return true on success, false on error
     * @throws NullPointerException If one of {@code listener}, {@code device} or {@code executor}
     * is null.
     * @throws IllegalArgumentException The same {@link #OnMetadataChangedListener} and
     * {@link BluetoothDevice} are registered twice.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean addOnMetadataChangedListener(@NonNull BluetoothDevice device,
            @NonNull Executor executor, @NonNull OnMetadataChangedListener listener) {
        if (DBG) Log.d(TAG, "addOnMetadataChangedListener()");

        if (listener == null) {
            throw new NullPointerException("listener is null");
        }
        if (device == null) {
            throw new NullPointerException("device is null");
        }
        if (executor == null) {
            throw new NullPointerException("executor is null");
        }

        mServiceLock.readLock().lock();
        try {
            if (mService == null) {
                Log.e(TAG, "Bluetooth is not enabled. Cannot register metadata listener");
                return false;
            }


            synchronized (mMetadataListeners) {
                List<Pair<OnMetadataChangedListener, Executor>> listenerList =
                    mMetadataListeners.get(device);
                if (listenerList == null) {
                    // Create new listener/executor list for registration
                    listenerList = new ArrayList<>();
                    mMetadataListeners.put(device, listenerList);
                } else {
                    // Check whether this device is already registered by the listener
                    if (listenerList.stream().anyMatch((pair) -> (pair.first.equals(listener)))) {
                        throw new IllegalArgumentException("listener was already regestered"
                                + " for the device");
                    }
                }

                Pair<OnMetadataChangedListener, Executor> listenerPair =
                        new Pair(listener, executor);
                listenerList.add(listenerPair);

                boolean ret = false;
                try {
                    final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                    mService.registerMetadataListener(mBluetoothMetadataListener, device,
                            mAttributionSource, recv);
                    ret = recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(false);
                } catch (RemoteException | TimeoutException e) {
                    Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                } finally {
                    if (!ret) {
                        // Remove listener registered earlier when fail.
                        listenerList.remove(listenerPair);
                        if (listenerList.isEmpty()) {
                            // Remove the device if its listener list is empty
                            mMetadataListeners.remove(device);
                        }
                    }
                }
                return ret;
            }
        } finally {
            mServiceLock.readLock().unlock();
        }
    }

    /**
     * Unregister a {@link #OnMetadataChangedListener} from a registered {@link BluetoothDevice}.
     * Unregistration can be done when Bluetooth is either ON or OFF.
     * {@link #addOnMetadataChangedListener(OnMetadataChangedListener, BluetoothDevice, Executor)}
     * must be called before unregisteration.
     *
     * @param device {@link BluetoothDevice} that will be unregistered. It
     * should not be null or {@link NullPointerException} will be triggered.
     * @param listener {@link OnMetadataChangedListener} that will be unregistered. It
     * should not be null or {@link NullPointerException} will be triggered.
     * @return true on success, false on error
     * @throws NullPointerException If {@code listener} or {@code device} is null.
     * @throws IllegalArgumentException If {@code device} has not been registered before.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean removeOnMetadataChangedListener(@NonNull BluetoothDevice device,
            @NonNull OnMetadataChangedListener listener) {
        if (DBG) Log.d(TAG, "removeOnMetadataChangedListener()");
        if (device == null) {
            throw new NullPointerException("device is null");
        }
        if (listener == null) {
            throw new NullPointerException("listener is null");
        }

        synchronized (mMetadataListeners) {
            if (!mMetadataListeners.containsKey(device)) {
                throw new IllegalArgumentException("device was not registered");
            }
            // Remove issued listener from the registered device
            mMetadataListeners.get(device).removeIf((pair) -> (pair.first.equals(listener)));

            if (mMetadataListeners.get(device).isEmpty()) {
                // Unregister to Bluetooth service if all listeners are removed from
                // the registered device
                mMetadataListeners.remove(device);
                mServiceLock.readLock().lock();
                try {
                    if (mService != null) {
                        final SynchronousResultReceiver<Boolean> recv =
                                SynchronousResultReceiver.get();
                        mService.unregisterMetadataListener(device, mAttributionSource, recv);
                        return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(false);
                    }
                } catch (RemoteException | TimeoutException e) {
                    Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                    return false;
                } finally {
                    mServiceLock.readLock().unlock();
                }

            }
        }
        return true;
    }

    /**
     * This interface is used to implement {@link BluetoothAdapter} metadata listener.
     * @hide
     */
    @SystemApi
    public interface OnMetadataChangedListener {
        /**
         * Callback triggered if the metadata of {@link BluetoothDevice} registered in
         * {@link #addOnMetadataChangedListener}.
         *
         * @param device changed {@link BluetoothDevice}.
         * @param key changed metadata key, one of BluetoothDevice.METADATA_*.
         * @param value the new value of metadata as byte array.
         */
        void onMetadataChanged(@NonNull BluetoothDevice device, int key,
                @Nullable byte[] value);
    }

    @SuppressLint("AndroidFrameworkBluetoothPermission")
    private final IBluetoothConnectionCallback mConnectionCallback =
            new IBluetoothConnectionCallback.Stub() {
        @Override
        public void onDeviceConnected(BluetoothDevice device) {
            Attributable.setAttributionSource(device, mAttributionSource);
            for (Map.Entry<BluetoothConnectionCallback, Executor> callbackExecutorEntry:
                    mBluetoothConnectionCallbackExecutorMap.entrySet()) {
                BluetoothConnectionCallback callback = callbackExecutorEntry.getKey();
                Executor executor = callbackExecutorEntry.getValue();
                executor.execute(() -> callback.onDeviceConnected(device));
            }
        }

        @Override
        public void onDeviceDisconnected(BluetoothDevice device, int hciReason) {
            Attributable.setAttributionSource(device, mAttributionSource);
            for (Map.Entry<BluetoothConnectionCallback, Executor> callbackExecutorEntry:
                    mBluetoothConnectionCallbackExecutorMap.entrySet()) {
                BluetoothConnectionCallback callback = callbackExecutorEntry.getKey();
                Executor executor = callbackExecutorEntry.getValue();
                executor.execute(() -> callback.onDeviceDisconnected(device, hciReason));
            }
        }
    };

    /**
     * Registers the BluetoothConnectionCallback to receive callback events when a bluetooth device
     * (classic or low energy) is connected or disconnected.
     *
     * @param executor is the callback executor
     * @param callback is the connection callback you wish to register
     * @return true if the callback was registered successfully, false otherwise
     * @throws IllegalArgumentException if the callback is already registered
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean registerBluetoothConnectionCallback(@NonNull @CallbackExecutor Executor executor,
            @NonNull BluetoothConnectionCallback callback) {
        if (DBG) Log.d(TAG, "registerBluetoothConnectionCallback()");
        if (callback == null || executor == null) {
            return false;
        }

        synchronized (mBluetoothConnectionCallbackExecutorMap) {
            // If the callback map is empty, we register the service-to-app callback
            if (mBluetoothConnectionCallbackExecutorMap.isEmpty()) {
                mServiceLock.readLock().lock();
                try {
                    if (mService != null) {
                        final SynchronousResultReceiver<Boolean> recv =
                                SynchronousResultReceiver.get();
                        mService.registerBluetoothConnectionCallback(mConnectionCallback,
                                mAttributionSource, recv);
                        if (!recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(false)) {
                            return false;
                        }
                    }
                } catch (RemoteException | TimeoutException e) {
                    Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                    mBluetoothConnectionCallbackExecutorMap.remove(callback);
                } finally {
                    mServiceLock.readLock().unlock();
                }
            }

            // Adds the passed in callback to our map of callbacks to executors
            if (mBluetoothConnectionCallbackExecutorMap.containsKey(callback)) {
                throw new IllegalArgumentException("This callback has already been registered");
            }
            mBluetoothConnectionCallbackExecutorMap.put(callback, executor);
        }

        return true;
    }

    /**
     * Unregisters the BluetoothConnectionCallback that was previously registered by the application
     *
     * @param callback is the connection callback you wish to unregister
     * @return true if the callback was unregistered successfully, false otherwise
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean unregisterBluetoothConnectionCallback(
            @NonNull BluetoothConnectionCallback callback) {
        if (DBG) Log.d(TAG, "unregisterBluetoothConnectionCallback()");
        if (callback == null) {
            return false;
        }

        synchronized (mBluetoothConnectionCallbackExecutorMap) {
            if (mBluetoothConnectionCallbackExecutorMap.remove(callback) != null) {
                return false;
            }
        }

        if (!mBluetoothConnectionCallbackExecutorMap.isEmpty()) {
            return true;
        }

        // If the callback map is empty, we unregister the service-to-app callback
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                mService.unregisterBluetoothConnectionCallback(mConnectionCallback,
                        mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(false);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }

        return false;
    }

    /**
     * This abstract class is used to implement callbacks for when a bluetooth classic or Bluetooth
     * Low Energy (BLE) device is either connected or disconnected.
     *
     * @hide
     */
    @SystemApi
    public abstract static class BluetoothConnectionCallback {
        /**
         * Callback triggered when a bluetooth device (classic or BLE) is connected
         * @param device is the connected bluetooth device
         */
        public void onDeviceConnected(@NonNull BluetoothDevice device) {}

        /**
         * Callback triggered when a bluetooth device (classic or BLE) is disconnected
         * @param device is the disconnected bluetooth device
         * @param reason is the disconnect reason
         */
        public void onDeviceDisconnected(@NonNull BluetoothDevice device,
                @DisconnectReason int reason) {}

        /**
         * @hide
         */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(prefix = { "REASON_" }, value = {
                BluetoothStatusCodes.ERROR_UNKNOWN,
                BluetoothStatusCodes.ERROR_DISCONNECT_REASON_LOCAL_REQUEST,
                BluetoothStatusCodes.ERROR_DISCONNECT_REASON_REMOTE_REQUEST,
                BluetoothStatusCodes.ERROR_DISCONNECT_REASON_LOCAL,
                BluetoothStatusCodes.ERROR_DISCONNECT_REASON_REMOTE,
                BluetoothStatusCodes.ERROR_DISCONNECT_REASON_TIMEOUT,
                BluetoothStatusCodes.ERROR_DISCONNECT_REASON_SECURITY,
                BluetoothStatusCodes.ERROR_DISCONNECT_REASON_SYSTEM_POLICY,
                BluetoothStatusCodes.ERROR_DISCONNECT_REASON_RESOURCE_LIMIT_REACHED,
                BluetoothStatusCodes.ERROR_DISCONNECT_REASON_CONNECTION_ALREADY_EXISTS,
                BluetoothStatusCodes.ERROR_DISCONNECT_REASON_BAD_PARAMETERS})
        public @interface DisconnectReason {}

        /**
         * Returns human-readable strings corresponding to {@link DisconnectReason}.
         */
        @NonNull
        public static String disconnectReasonToString(@DisconnectReason int reason) {
            switch (reason) {
                case BluetoothStatusCodes.ERROR_UNKNOWN:
                    return "Reason unknown";
                case BluetoothStatusCodes.ERROR_DISCONNECT_REASON_LOCAL_REQUEST:
                    return "Local request";
                case BluetoothStatusCodes.ERROR_DISCONNECT_REASON_REMOTE_REQUEST:
                    return "Remote request";
                case BluetoothStatusCodes.ERROR_DISCONNECT_REASON_LOCAL:
                    return "Local error";
                case BluetoothStatusCodes.ERROR_DISCONNECT_REASON_REMOTE:
                    return "Remote error";
                case BluetoothStatusCodes.ERROR_DISCONNECT_REASON_TIMEOUT:
                    return "Timeout";
                case BluetoothStatusCodes.ERROR_DISCONNECT_REASON_SECURITY:
                    return "Security";
                case BluetoothStatusCodes.ERROR_DISCONNECT_REASON_SYSTEM_POLICY:
                    return "System policy";
                case BluetoothStatusCodes.ERROR_DISCONNECT_REASON_RESOURCE_LIMIT_REACHED:
                    return "Resource constrained";
                case BluetoothStatusCodes.ERROR_DISCONNECT_REASON_CONNECTION_ALREADY_EXISTS:
                    return "Connection already exists";
                case BluetoothStatusCodes.ERROR_DISCONNECT_REASON_BAD_PARAMETERS:
                    return "Bad parameters";
                default:
                    return "Unrecognized disconnect reason: " + reason;
            }
        }
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            BluetoothStatusCodes.SUCCESS,
            BluetoothStatusCodes.ERROR_ANOTHER_ACTIVE_REQUEST,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ALLOWED,
            BluetoothStatusCodes.ERROR_DEVICE_NOT_BONDED,
            BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION,
            BluetoothStatusCodes.ERROR_NOT_DUAL_MODE_AUDIO_DEVICE,
            BluetoothStatusCodes.ERROR_UNKNOWN,
            BluetoothStatusCodes.FEATURE_NOT_SUPPORTED,
    })
    public @interface SetPreferredAudioProfilesReturnValues {}

    /**
     * Sets the preferred profiles for each audio mode for system routed audio. The audio framework
     * and Telecomm will read this preference when routing system managed audio. Not supplying an
     * audio mode in the Bundle will reset that audio mode to the default profile preference for
     * that mode (e.g. an empty Bundle resets all audio modes to their default profiles).
     * <p>
     * Note: apps that invoke profile-specific audio APIs are not subject to the preference noted
     * here. These preferences will also be ignored if the remote device is not simultaneously
     * connected to a classic audio profile (A2DP and/or HFP) and LE Audio at the same time. If the
     * remote device does not support both BR/EDR audio and LE Audio, this API returns
     * {@link BluetoothStatusCodes#ERROR_NOT_DUAL_MODE_AUDIO_DEVICE}. If the system property
     * persist.bluetooth.enable_dual_mode_audio is set to {@code false}, this API returns
     * {@link BluetoothStatusCodes#FEATURE_NOT_SUPPORTED}.
     * <p>
     * The Bundle is expected to contain the following mappings:
     * 1. For key {@link #AUDIO_MODE_OUTPUT_ONLY}, it expects an integer value of either
     * {@link BluetoothProfile#A2DP} or {@link BluetoothProfile#LE_AUDIO}.
     * 2. For key {@link #AUDIO_MODE_DUPLEX}, it expects an integer value of either
     * {@link BluetoothProfile#HEADSET} or {@link BluetoothProfile#LE_AUDIO}.
     * <p>
     * Apps should register for a callback with
     * {@link #registerPreferredAudioProfilesChangedCallback(Executor,
     * PreferredAudioProfilesChangedCallback)} to know if the preferences were successfully applied
     * to the audio framework. If there is an active preference change for this device that has not
     * taken effect with the audio framework, no additional calls to this API will be allowed until
     * that completes.
     *
     * @param modeToProfileBundle a mapping to indicate the preferred profile for each audio mode
     * @return whether the preferred audio profiles were requested to be set
     * @throws NullPointerException if modeToProfileBundle or device is null
     * @throws IllegalArgumentException if this BluetoothDevice object has an invalid address or the
     * Bundle doesn't conform to its requirements
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @SetPreferredAudioProfilesReturnValues
    public int setPreferredAudioProfiles(@NonNull BluetoothDevice device,
            @NonNull Bundle modeToProfileBundle) {
        if (DBG) {
            Log.d(TAG, "setPreferredAudioProfiles( " + modeToProfileBundle + ", " + device + ")");
        }
        requireNonNull(modeToProfileBundle, "modeToProfileBundle must not be null");
        requireNonNull(device, "device must not be null");
        if (!BluetoothAdapter.checkBluetoothAddress(getAddress())) {
            throw new IllegalArgumentException("device cannot have an invalid address");
        }
        if (!modeToProfileBundle.containsKey(AUDIO_MODE_OUTPUT_ONLY)
                && !modeToProfileBundle.containsKey(AUDIO_MODE_DUPLEX)) {
            throw new IllegalArgumentException("Bundle does not contain a key "
                    + "AUDIO_MODE_OUTPUT_ONLY or AUDIO_MODE_DUPLEX");
        }
        if (modeToProfileBundle.containsKey(AUDIO_MODE_OUTPUT_ONLY)
                && modeToProfileBundle.getInt(AUDIO_MODE_OUTPUT_ONLY) != BluetoothProfile.A2DP
                && modeToProfileBundle.getInt(
                AUDIO_MODE_OUTPUT_ONLY) != BluetoothProfile.LE_AUDIO) {
            throw new IllegalArgumentException("Key AUDIO_MODE_OUTPUT_ONLY has an invalid value: "
                    + modeToProfileBundle.getInt(AUDIO_MODE_OUTPUT_ONLY));
        }
        if (modeToProfileBundle.containsKey(AUDIO_MODE_DUPLEX)
                && modeToProfileBundle.getInt(AUDIO_MODE_DUPLEX) != BluetoothProfile.HEADSET
                && modeToProfileBundle.getInt(AUDIO_MODE_DUPLEX) != BluetoothProfile.LE_AUDIO) {
            throw new IllegalArgumentException("Key AUDIO_MODE_DUPLEX has an invalid value: "
                    + modeToProfileBundle.getInt(AUDIO_MODE_DUPLEX));
        }

        final int defaultValue = BluetoothStatusCodes.ERROR_UNKNOWN;
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                mService.setPreferredAudioProfiles(device, modeToProfileBundle,
                        mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } else {
                return BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
            }
        } catch (RemoteException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            throw e.rethrowFromSystemServer();
        } catch (TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return defaultValue;
    }

    /**
     * Gets the preferred profile for each audio mode for system routed audio. This API
     * returns a Bundle with mappings between each audio mode and its preferred audio profile. If no
     * values are set via {@link #setPreferredAudioProfiles(BluetoothDevice, Bundle)}, this API
     * returns the default system preferences set via the sysprops
     * {@link BluetoothProperties#getDefaultOutputOnlyAudioProfile()} and
     * {@link BluetoothProperties#getDefaultDuplexAudioProfile()}.
     * <p>
     * An audio capable device must support at least one audio mode with a preferred audio profile.
     * If a device does not support an audio mode, the audio mode will be omitted from the keys of
     * the Bundle. If the device is not recognized as a dual mode audio capable device (e.g. because
     * it is not bonded, does not support any audio profiles, or does not support both BR/EDR audio
     * and LE Audio), this API returns an empty Bundle. If the system property
     * persist.bluetooth.enable_dual_mode_audio is set to {@code false}, this API returns an empty
     * Bundle.
     * <p>
     * The Bundle can contain the following mappings:
     * <ul>
     * <li>For key {@link #AUDIO_MODE_OUTPUT_ONLY}, if an audio profile preference was set, this
     *     will have an int value of either {@link BluetoothProfile#A2DP} or
     *     {@link BluetoothProfile#LE_AUDIO}.
     * <li>For key {@link #AUDIO_MODE_DUPLEX}, if an audio profile preference was set, this will
     *     have an int value of either {@link BluetoothProfile#HEADSET} or
     *     {@link BluetoothProfile#LE_AUDIO}.
     * </ul>
     *
     * @return a Bundle mapping each set audio mode and preferred audio profile pair
     * @throws NullPointerException if modeToProfileBundle or device is null
     * @throws IllegalArgumentException if this BluetoothDevice object has an invalid address or the
     * Bundle doesn't conform to its requirements
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @NonNull
    public Bundle getPreferredAudioProfiles(@NonNull BluetoothDevice device) {
        if (DBG) Log.d(TAG, "getPreferredAudioProfiles(" + device + ")");
        requireNonNull(device, "device cannot be null");
        if (!BluetoothAdapter.checkBluetoothAddress(device.getAddress())) {
            throw new IllegalArgumentException("device cannot have an invalid address");
        }

        final Bundle defaultValue = Bundle.EMPTY;
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Bundle> recv = SynchronousResultReceiver.get();
                mService.getPreferredAudioProfiles(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            }
        } catch (RemoteException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            throw e.rethrowFromSystemServer();
        } catch (TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }

        return defaultValue;
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            BluetoothStatusCodes.SUCCESS,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ALLOWED,
            BluetoothStatusCodes.ERROR_DEVICE_NOT_BONDED,
            BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION,
            BluetoothStatusCodes.ERROR_UNKNOWN
    })
    public @interface NotifyActiveDeviceChangeAppliedReturnValues {}

    /**
     * Called by audio framework to inform the Bluetooth stack that an active device change has
     * taken effect. If this active device change is triggered by an app calling
     * {@link #setPreferredAudioProfiles(BluetoothDevice, Bundle)}, the Bluetooth stack will invoke
     * {@link PreferredAudioProfilesChangedCallback#onPreferredAudioProfilesChanged(
     * BluetoothDevice, Bundle, int)} if all requested changes for the device have been applied.
     * <p>
     * This method will return
     * {@link BluetoothStatusCodes#ERROR_BLUETOOTH_NOT_ALLOWED} if called outside system server.
     *
     * @param device is the BluetoothDevice that had its preferred audio profile changed
     * @return whether the Bluetooth stack acknowledged the change successfully
     * @throws NullPointerException if device is null
     * @throws IllegalArgumentException if the device's address is invalid
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @NotifyActiveDeviceChangeAppliedReturnValues
    public int notifyActiveDeviceChangeApplied(@NonNull BluetoothDevice device) {
        if (DBG) Log.d(TAG, "notifyActiveDeviceChangeApplied(" + device + ")");
        requireNonNull(device, "device cannot be null");
        if (!BluetoothAdapter.checkBluetoothAddress(device.getAddress())) {
            throw new IllegalArgumentException("device cannot have an invalid address");
        }

        final int defaultValue = BluetoothStatusCodes.ERROR_UNKNOWN;
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                mService.notifyActiveDeviceChangeApplied(device,
                        mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            }
        } catch (RemoteException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            throw e.rethrowFromSystemServer();
        } catch (TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }

        return defaultValue;
    }

    @SuppressLint("AndroidFrameworkBluetoothPermission")
    private final IBluetoothPreferredAudioProfilesCallback mPreferredAudioProfilesChangedCallback =
            new IBluetoothPreferredAudioProfilesCallback.Stub() {
                @Override
                public void onPreferredAudioProfilesChanged(BluetoothDevice device,
                        Bundle preferredAudioProfiles, int status) {
                    for (Map.Entry<PreferredAudioProfilesChangedCallback, Executor>
                            callbackExecutorEntry:
                            mAudioProfilesChangedCallbackExecutorMap.entrySet()) {
                        PreferredAudioProfilesChangedCallback callback =
                                callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onPreferredAudioProfilesChanged(device,
                                preferredAudioProfiles, status));
                    }
                }
            };

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            BluetoothStatusCodes.SUCCESS,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ALLOWED,
            BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION,
            BluetoothStatusCodes.ERROR_UNKNOWN,
            BluetoothStatusCodes.FEATURE_NOT_SUPPORTED
    })
    public @interface RegisterPreferredAudioProfilesCallbackReturnValues {}

    /**
     * Registers a callback to be notified when the preferred audio profile changes have taken
     * effect. To unregister this callback, call
     * {@link #unregisterPreferredAudioProfilesChangedCallback(
     * PreferredAudioProfilesChangedCallback)}. If the system property
     * persist.bluetooth.enable_dual_mode_audio is set to {@code false}, this API returns
     * {@link BluetoothStatusCodes#FEATURE_NOT_SUPPORTED}.
     *
     * @param executor an {@link Executor} to execute the callbacks
     * @param callback user implementation of the {@link PreferredAudioProfilesChangedCallback}
     * @return whether the callback was registered successfully
     * @throws NullPointerException if executor or callback is null
     * @throws IllegalArgumentException if the callback is already registered
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @RegisterPreferredAudioProfilesCallbackReturnValues
    public int registerPreferredAudioProfilesChangedCallback(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull PreferredAudioProfilesChangedCallback callback) {
        if (DBG) Log.d(TAG, "registerPreferredAudioProfilesChangedCallback()");
        requireNonNull(executor, "executor cannot be null");
        requireNonNull(callback, "callback cannot be null");

        final int defaultValue = BluetoothStatusCodes.ERROR_UNKNOWN;
        int serviceCallStatus = defaultValue;
        synchronized (mAudioProfilesChangedCallbackExecutorMap) {
            // If the callback map is empty, we register the service-to-app callback
            if (mAudioProfilesChangedCallbackExecutorMap.isEmpty()) {
                mServiceLock.readLock().lock();
                try {
                    if (mService != null) {
                        final SynchronousResultReceiver<Integer> recv =
                                SynchronousResultReceiver.get();
                        mService.registerPreferredAudioProfilesChangedCallback(
                                mPreferredAudioProfilesChangedCallback, mAttributionSource, recv);
                        serviceCallStatus = recv.awaitResultNoInterrupt(getSyncTimeout())
                                .getValue(defaultValue);
                    }
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                } catch (TimeoutException e) {
                    Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                } finally {
                    mServiceLock.readLock().unlock();
                }
                if (serviceCallStatus != BluetoothStatusCodes.SUCCESS) {
                    return serviceCallStatus;
                }
            }

            // Adds the passed in callback to our local mapping
            if (mAudioProfilesChangedCallbackExecutorMap.containsKey(callback)) {
                throw new IllegalArgumentException("This callback has already been registered");
            } else {
                mAudioProfilesChangedCallbackExecutorMap.put(callback, executor);
            }
        }

        return BluetoothStatusCodes.SUCCESS;
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            BluetoothStatusCodes.SUCCESS,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ALLOWED,
            BluetoothStatusCodes.ERROR_CALLBACK_NOT_REGISTERED,
            BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION,
            BluetoothStatusCodes.ERROR_UNKNOWN,
            BluetoothStatusCodes.FEATURE_NOT_SUPPORTED
    })
    public @interface UnRegisterPreferredAudioProfilesCallbackReturnValues {}

    /**
     * Unregisters a callback that was previously registered with
     * {@link #registerPreferredAudioProfilesChangedCallback(Executor,
     * PreferredAudioProfilesChangedCallback)}.
     *
     * @param callback user implementation of the {@link PreferredAudioProfilesChangedCallback}
     * @return whether the callback was successfully unregistered
     * @throws NullPointerException if the callback is null
     * @throws IllegalArgumentException if the callback has not been registered
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @UnRegisterPreferredAudioProfilesCallbackReturnValues
    public int unregisterPreferredAudioProfilesChangedCallback(
            @NonNull PreferredAudioProfilesChangedCallback callback) {
        if (DBG) Log.d(TAG, "unregisterPreferredAudioProfilesChangedCallback()");
        requireNonNull(callback, "callback cannot be null");

        synchronized (mAudioProfilesChangedCallbackExecutorMap) {
            if (mAudioProfilesChangedCallbackExecutorMap.remove(callback) == null) {
                throw new IllegalArgumentException("This callback has not been registered");
            }
        }

        if (!mBluetoothConnectionCallbackExecutorMap.isEmpty()) {
            return BluetoothStatusCodes.SUCCESS;
        }

        final int defaultValue = BluetoothStatusCodes.ERROR_UNKNOWN;
        // If the callback map is empty, we unregister the service-to-app callback
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                mService.unregisterPreferredAudioProfilesChangedCallback(
                        mPreferredAudioProfilesChangedCallback, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            }
        } catch (TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } finally {
            mServiceLock.readLock().unlock();
        }

        return defaultValue;
    }

    /**
     * A callback for preferred audio profile changes that arise from calls to
     * {@link #setPreferredAudioProfiles(BluetoothDevice, Bundle)}.
     *
     * @hide
     */
    @SystemApi
    public interface PreferredAudioProfilesChangedCallback {
        /**
         * Called when the preferred audio profile change from a call to
         * {@link #setPreferredAudioProfiles(BluetoothDevice, Bundle)} has taken effect in the audio
         * framework or timed out. This callback includes a Bundle that indicates the current
         * preferred audio profile for each audio mode, if one was set. If an audio mode does not
         * have a profile preference, its key will be omitted from the Bundle. If both audio modes
         * do not have a preferred profile set, the Bundle will be empty.
         *
         * <p>
         * The Bundle can contain the following mappings:
         * <ul>
         * <li>For key {@link #AUDIO_MODE_OUTPUT_ONLY}, if an audio profile preference was set, this
         *     will have an int value of either {@link BluetoothProfile#A2DP} or
         *     {@link BluetoothProfile#LE_AUDIO}.
         * <li>For key {@link #AUDIO_MODE_DUPLEX}, if an audio profile preference was set, this will
         *     have an int value of either {@link BluetoothProfile#HEADSET} or
         *     {@link BluetoothProfile#LE_AUDIO}.
         * </ul>
         *
         * @param device is the device which had its preferred audio profiles changed
         * @param preferredAudioProfiles a Bundle mapping audio mode to its preferred audio profile
         * @param status whether the operation succeeded or timed out
         *
         * @hide
         */
        @SystemApi
        void onPreferredAudioProfilesChanged(@NonNull BluetoothDevice device, @NonNull
                Bundle preferredAudioProfiles, int status);
    }

    @SuppressLint("AndroidFrameworkBluetoothPermission")
    private final IBluetoothQualityReportReadyCallback mBluetoothQualityReportReadyCallback =
            new IBluetoothQualityReportReadyCallback.Stub() {
                @Override
                public void onBluetoothQualityReportReady(BluetoothDevice device,
                        BluetoothQualityReport bluetoothQualityReport, int status) {
                    for (Map.Entry<BluetoothQualityReportReadyCallback, Executor>
                            callbackExecutorEntry:
                            mBluetoothQualityReportReadyCallbackExecutorMap.entrySet()) {
                        BluetoothQualityReportReadyCallback callback =
                                callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onBluetoothQualityReportReady(device,
                                bluetoothQualityReport, status));
                    }
                }
            };

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            BluetoothStatusCodes.SUCCESS,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ALLOWED,
            BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION,
            BluetoothStatusCodes.ERROR_UNKNOWN
    })
    public @interface RegisterBluetoothQualityReportReadyCallbackReturnValues {}

    /**
     * Registers a callback to be notified when Bluetooth Quality Report is ready. To unregister
     * this callback, call
     * {@link #unregisterBluetoothQualityReportReadyCallback(
     * BluetoothQualityReportReadyCallback)}.
     *
     * @param executor an {@link Executor} to execute the callbacks
     * @param callback user implementation of the {@link BluetoothQualityReportReadyCallback}
     * @return whether the callback was registered successfully
     * @throws NullPointerException if executor or callback is null
     * @throws IllegalArgumentException if the callback is already registered
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @RegisterBluetoothQualityReportReadyCallbackReturnValues
    public int registerBluetoothQualityReportReadyCallback(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull BluetoothQualityReportReadyCallback callback) {
        if (DBG) Log.d(TAG, "registerBluetoothQualityReportReadyCallback()");
        requireNonNull(executor, "executor cannot be null");
        requireNonNull(callback, "callback cannot be null");

        final int defaultValue = BluetoothStatusCodes.ERROR_UNKNOWN;
        int serviceCallStatus = defaultValue;
        synchronized (mBluetoothQualityReportReadyCallbackExecutorMap) {
            // If the callback map is empty, we register the service-to-app callback
            if (mBluetoothQualityReportReadyCallbackExecutorMap.isEmpty()) {
                mServiceLock.readLock().lock();
                try {
                    if (mService != null) {
                        final SynchronousResultReceiver<Integer> recv =
                                SynchronousResultReceiver.get();
                        mService.registerBluetoothQualityReportReadyCallback(
                                mBluetoothQualityReportReadyCallback, mAttributionSource, recv);
                        serviceCallStatus = recv.awaitResultNoInterrupt(getSyncTimeout())
                                .getValue(defaultValue);
                    }
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                } catch (TimeoutException e) {
                    Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                } finally {
                    mServiceLock.readLock().unlock();
                }
                if (serviceCallStatus != BluetoothStatusCodes.SUCCESS) {
                    return serviceCallStatus;
                }
            }

            // Adds the passed in callback to our local mapping
            if (mBluetoothQualityReportReadyCallbackExecutorMap.containsKey(callback)) {
                throw new IllegalArgumentException("This callback has already been registered");
            } else {
                mBluetoothQualityReportReadyCallbackExecutorMap.put(callback, executor);
            }
        }

        return BluetoothStatusCodes.SUCCESS;
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            BluetoothStatusCodes.SUCCESS,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ALLOWED,
            BluetoothStatusCodes.ERROR_CALLBACK_NOT_REGISTERED,
            BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION,
            BluetoothStatusCodes.ERROR_UNKNOWN
    })
    public @interface UnRegisterBluetoothQualityReportReadyCallbackReturnValues {}

    /**
     * Unregisters a callback that was previously registered with
     * {@link #registerBluetoothQualityReportReadyCallback(Executor,
     * BluetoothQualityReportReadyCallback)}.
     *
     * @param callback user implementation of the {@link BluetoothQualityReportReadyCallback}
     * @return whether the callback was successfully unregistered
     * @throws NullPointerException if the callback is null
     * @throws IllegalArgumentException if the callback has not been registered
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @UnRegisterBluetoothQualityReportReadyCallbackReturnValues
    public int unregisterBluetoothQualityReportReadyCallback(
            @NonNull BluetoothQualityReportReadyCallback callback) {
        if (DBG) Log.d(TAG, "unregisterBluetoothQualityReportReadyCallback()");
        requireNonNull(callback, "callback cannot be null");

        synchronized (mBluetoothQualityReportReadyCallbackExecutorMap) {
            if (mBluetoothQualityReportReadyCallbackExecutorMap.remove(callback) == null) {
                throw new IllegalArgumentException("This callback has not been registered");
            }
        }

        if (!mBluetoothConnectionCallbackExecutorMap.isEmpty()) {
            return BluetoothStatusCodes.SUCCESS;
        }

        final int defaultValue = BluetoothStatusCodes.ERROR_UNKNOWN;
        // If the callback map is empty, we unregister the service-to-app callback
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                mService.unregisterBluetoothQualityReportReadyCallback(
                        mBluetoothQualityReportReadyCallback, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            }
        } catch (TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } finally {
            mServiceLock.readLock().unlock();
        }

        return defaultValue;
    }

    /**
     * A callback for Bluetooth Quality Report that arise from the controller.
     *
     * @hide
     */
    @SystemApi
    public interface BluetoothQualityReportReadyCallback {
        /**
         * Called when the Bluetooth Quality Report coming from the controller is ready. This
         * callback includes a Parcel that contains information about Bluetooth Quality.
         * Currently the report supports five event types: Quality monitor event,
         * Approaching LSTO event, A2DP choppy event, SCO choppy event and Connect fail event.
         * To know which kind of event is wrapped in this {@link BluetoothQualityReport} object,
         * you need to call {@link #getQualityReportId}.
         *
         *
         * @param device is the BluetoothDevice which connection quality is being reported
         * @param bluetoothQualityReport a Parcel that contains info about Bluetooth Quality
         * @param status whether the operation succeeded or timed out
         *
         * @hide
         */
        @SystemApi
        void onBluetoothQualityReportReady(@NonNull BluetoothDevice device, @NonNull
                BluetoothQualityReport bluetoothQualityReport, int status);
    }

    /**
     * Converts old constant of priority to the new for connection policy
     *
     * @param priority is the priority to convert to connection policy
     * @return the equivalent connection policy constant to the priority
     *
     * @hide
     */
    public static @ConnectionPolicy int priorityToConnectionPolicy(int priority) {
        switch(priority) {
            case BluetoothProfile.PRIORITY_AUTO_CONNECT:
                return BluetoothProfile.CONNECTION_POLICY_ALLOWED;
            case BluetoothProfile.PRIORITY_ON:
                return BluetoothProfile.CONNECTION_POLICY_ALLOWED;
            case BluetoothProfile.PRIORITY_OFF:
                return BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
            case BluetoothProfile.PRIORITY_UNDEFINED:
                return BluetoothProfile.CONNECTION_POLICY_UNKNOWN;
            default:
                Log.e(TAG, "setPriority: Invalid priority: " + priority);
                return BluetoothProfile.CONNECTION_POLICY_UNKNOWN;
        }
    }

    /**
     * Converts new constant of connection policy to the old for priority
     *
     * @param connectionPolicy is the connection policy to convert to priority
     * @return the equivalent priority constant to the connectionPolicy
     *
     * @hide
     */
    public static int connectionPolicyToPriority(@ConnectionPolicy int connectionPolicy) {
        switch(connectionPolicy) {
            case BluetoothProfile.CONNECTION_POLICY_ALLOWED:
                return BluetoothProfile.PRIORITY_ON;
            case BluetoothProfile.CONNECTION_POLICY_FORBIDDEN:
                return BluetoothProfile.PRIORITY_OFF;
            case BluetoothProfile.CONNECTION_POLICY_UNKNOWN:
                return BluetoothProfile.PRIORITY_UNDEFINED;
        }
        return BluetoothProfile.PRIORITY_UNDEFINED;
    }

    /**
     * Sets the desired mode of the HCI snoop logging applied at Bluetooth startup.
     *
     * Please note that Bluetooth needs to be restarted in order for the change
     * to take effect.
     *
     * @param mode
     * @return status code indicating whether the logging mode was successfully set
     * @throws IllegalArgumentException if the mode is not a valid logging mode
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED)
    @SetSnoopLogModeStatusCode
    public int setBluetoothHciSnoopLoggingMode(@BluetoothSnoopLogMode int mode) {
        if (mode != BT_SNOOP_LOG_MODE_DISABLED && mode != BT_SNOOP_LOG_MODE_FILTERED
                && mode != BT_SNOOP_LOG_MODE_FULL) {
            throw new IllegalArgumentException("Invalid Bluetooth HCI snoop log mode param value");
        }
        try {
            return mManagerService.setBtHciSnoopLogMode(mode);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
        return BluetoothStatusCodes.ERROR_UNKNOWN;
    }

    /**
     * Gets the current desired mode of HCI snoop logging applied at Bluetooth startup.
     *
     * @return the current HCI snoop logging mode applied at Bluetooth startup
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED)
    @BluetoothSnoopLogMode
    public int getBluetoothHciSnoopLoggingMode() {
        try {
            return mManagerService.getBtHciSnoopLogMode();
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
        return BT_SNOOP_LOG_MODE_DISABLED;
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            BluetoothStatusCodes.FEATURE_SUPPORTED,
            BluetoothStatusCodes.ERROR_UNKNOWN,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED,
            BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_SCAN_PERMISSION,
            BluetoothStatusCodes.FEATURE_NOT_SUPPORTED
    })
    public @interface GetOffloadedTransportDiscoveryDataScanSupportedReturnValues {}

    /**
     * Check if offloaded transport discovery data scan is supported or not.
     *
     * @return  {@code BluetoothStatusCodes.FEATURE_SUPPORTED} if chipset supports on-chip tds
     *          filter scan
     * @hide
     */
    @SystemApi
    @RequiresBluetoothScanPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @GetOffloadedTransportDiscoveryDataScanSupportedReturnValues
    public int getOffloadedTransportDiscoveryDataScanSupported() {
        if (!getLeAccess()) {
            return BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
        }
        mServiceLock.readLock().lock();
        try {
            if (mService != null) {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                mService.getOffloadedTransportDiscoveryDataScanSupported(mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout())
                        .getValue(BluetoothStatusCodes.ERROR_UNKNOWN);
            }
        } catch (RemoteException | TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } finally {
            mServiceLock.readLock().unlock();
        }
        return BluetoothStatusCodes.ERROR_UNKNOWN;
    }
}
