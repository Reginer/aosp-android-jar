/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.app.compat.CompatChanges;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.bluetooth.annotations.RequiresBluetoothLocationPermission;
import android.bluetooth.annotations.RequiresBluetoothScanPermission;
import android.bluetooth.annotations.RequiresLegacyBluetoothAdminPermission;
import android.bluetooth.annotations.RequiresLegacyBluetoothPermission;
import android.companion.AssociationRequest;
import android.compat.annotation.ChangeId;
import android.compat.annotation.EnabledSince;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.AttributionSource;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.IpcDataCache;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;

import com.android.modules.utils.SynchronousResultReceiver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Represents a remote Bluetooth device. A {@link BluetoothDevice} lets you
 * create a connection with the respective device or query information about
 * it, such as the name, address, class, and bonding state.
 *
 * <p>This class is really just a thin wrapper for a Bluetooth hardware
 * address. Objects of this class are immutable. Operations on this class
 * are performed on the remote Bluetooth hardware address, using the
 * {@link BluetoothAdapter} that was used to create this {@link
 * BluetoothDevice}.
 *
 * <p>To get a {@link BluetoothDevice}, use
 * {@link BluetoothAdapter#getRemoteDevice(String)
 * BluetoothAdapter.getRemoteDevice(String)} to create one representing a device
 * of a known MAC address (which you can get through device discovery with
 * {@link BluetoothAdapter}) or get one from the set of bonded devices
 * returned by {@link BluetoothAdapter#getBondedDevices()
 * BluetoothAdapter.getBondedDevices()}. You can then open a
 * {@link BluetoothSocket} for communication with the remote device, using
 * {@link #createRfcommSocketToServiceRecord(UUID)} over Bluetooth BR/EDR or using
 * {@link #createL2capChannel(int)} over Bluetooth LE.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>
 * For more information about using Bluetooth, read the <a href=
 * "{@docRoot}guide/topics/connectivity/bluetooth.html">Bluetooth</a> developer
 * guide.
 * </p>
 * </div>
 *
 * {@see BluetoothAdapter}
 * {@see BluetoothSocket}
 */
public final class BluetoothDevice implements Parcelable, Attributable {
    private static final String TAG = "BluetoothDevice";
    private static final boolean DBG = false;

    /**
     * Connection state bitmask as returned by getConnectionState.
     */
    private static final int CONNECTION_STATE_DISCONNECTED = 0;
    private static final int CONNECTION_STATE_CONNECTED = 1;
    private static final int CONNECTION_STATE_ENCRYPTED_BREDR = 2;
    private static final int CONNECTION_STATE_ENCRYPTED_LE = 4;

    /**
     * Sentinel error value for this class. Guaranteed to not equal any other
     * integer constant in this class. Provided as a convenience for functions
     * that require a sentinel error value, for example:
     * <p><code>Intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
     * BluetoothDevice.ERROR)</code>
     */
    public static final int ERROR = Integer.MIN_VALUE;

    /**
     * Broadcast Action: Remote device discovered.
     * <p>Sent when a remote device is found during discovery.
     * <p>Always contains the extra fields {@link #EXTRA_DEVICE} and {@link
     * #EXTRA_CLASS}. Can contain the extra fields {@link #EXTRA_NAME} and/or
     * {@link #EXTRA_RSSI} and/or {@link #EXTRA_IS_COORDINATED_SET_MEMBER} if they are available.
     */
    // TODO: Change API to not broadcast RSSI if not available (incoming connection)
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothScanPermission
    @RequiresBluetoothLocationPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_FOUND =
            "android.bluetooth.device.action.FOUND";

    /**
     * Broadcast Action: Bluetooth class of a remote device has changed.
     * <p>Always contains the extra fields {@link #EXTRA_DEVICE} and {@link
     * #EXTRA_CLASS}.
     * {@see BluetoothClass}
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CLASS_CHANGED =
            "android.bluetooth.device.action.CLASS_CHANGED";

    /**
     * Broadcast Action: Indicates a low level (ACL) connection has been
     * established with a remote device.
     * <p>Always contains the extra fields {@link #EXTRA_DEVICE} and {@link #EXTRA_TRANSPORT}.
     * <p>ACL connections are managed automatically by the Android Bluetooth
     * stack.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_ACL_CONNECTED =
            "android.bluetooth.device.action.ACL_CONNECTED";

    /**
     * Broadcast Action: Indicates that a low level (ACL) disconnection has
     * been requested for a remote device, and it will soon be disconnected.
     * <p>This is useful for graceful disconnection. Applications should use
     * this intent as a hint to immediately terminate higher level connections
     * (RFCOMM, L2CAP, or profile connections) to the remote device.
     * <p>Always contains the extra field {@link #EXTRA_DEVICE}.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_ACL_DISCONNECT_REQUESTED =
            "android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED";

    /**
     * Broadcast Action: Indicates a low level (ACL) disconnection from a
     * remote device.
     * <p>Always contains the extra fields {@link #EXTRA_DEVICE} and {@link #EXTRA_TRANSPORT}.
     * <p>ACL connections are managed automatically by the Android Bluetooth
     * stack.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_ACL_DISCONNECTED =
            "android.bluetooth.device.action.ACL_DISCONNECTED";

    /**
     * Broadcast Action: Indicates the friendly name of a remote device has
     * been retrieved for the first time, or changed since the last retrieval.
     * <p>Always contains the extra fields {@link #EXTRA_DEVICE} and {@link
     * #EXTRA_NAME}.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_NAME_CHANGED =
            "android.bluetooth.device.action.NAME_CHANGED";

    /**
     * Broadcast Action: Indicates the alias of a remote device has been
     * changed.
     * <p>Always contains the extra field {@link #EXTRA_DEVICE}.
     */
    @SuppressLint("ActionValue")
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_ALIAS_CHANGED =
            "android.bluetooth.device.action.ALIAS_CHANGED";

    /**
     * Broadcast Action: Indicates a change in the bond state of a remote
     * device. For example, if a device is bonded (paired).
     * <p>Always contains the extra fields {@link #EXTRA_DEVICE}, {@link
     * #EXTRA_BOND_STATE} and {@link #EXTRA_PREVIOUS_BOND_STATE}.
     */
    // Note: When EXTRA_BOND_STATE is BOND_NONE then this will also
    // contain a hidden extra field EXTRA_UNBOND_REASON with the result code.
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_BOND_STATE_CHANGED =
            "android.bluetooth.device.action.BOND_STATE_CHANGED";

    /**
     * Broadcast Action: Indicates the battery level of a remote device has
     * been retrieved for the first time, or changed since the last retrieval
     * <p>Always contains the extra fields {@link #EXTRA_DEVICE} and {@link
     * #EXTRA_BATTERY_LEVEL}.
     *
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @SuppressLint("ActionValue")
    public static final String ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED";

    /**
     * Broadcast Action: Indicates the audio buffer size should be switched
     * between a low latency buffer size and a higher and larger latency buffer size.
     * Only registered receivers will receive this intent.
     * <p>Always contains the extra fields {@link #EXTRA_DEVICE} and {@link
     * #EXTRA_LOW_LATENCY_BUFFER_SIZE}.
     *
     * @hide
     */
    @SuppressLint("ActionValue")
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @SystemApi
    public static final String ACTION_SWITCH_BUFFER_SIZE =
            "android.bluetooth.device.action.SWITCH_BUFFER_SIZE";

    /**
     * Used as an Integer extra field in {@link #ACTION_BATTERY_LEVEL_CHANGED}
     * intent. It contains the most recently retrieved battery level information
     * ranging from 0% to 100% for a remote device, {@link #BATTERY_LEVEL_UNKNOWN}
     * when the valid is unknown or there is an error, {@link #BATTERY_LEVEL_BLUETOOTH_OFF} when the
     * bluetooth is off
     *
     * @hide
     */
    @SuppressLint("ActionValue")
    @SystemApi
    public static final String EXTRA_BATTERY_LEVEL =
            "android.bluetooth.device.extra.BATTERY_LEVEL";

    /**
     * Used as the unknown value for {@link #EXTRA_BATTERY_LEVEL} and {@link #getBatteryLevel()}
     *
     * @hide
     */
    @SystemApi
    public static final int BATTERY_LEVEL_UNKNOWN = -1;

    /**
     * Used as an error value for {@link #getBatteryLevel()} to represent bluetooth is off
     *
     * @hide
     */
    @SystemApi
    public static final int BATTERY_LEVEL_BLUETOOTH_OFF = -100;

    /**
     * Used as a Parcelable {@link BluetoothDevice} extra field in every intent
     * broadcast by this class. It contains the {@link BluetoothDevice} that
     * the intent applies to.
     */
    public static final String EXTRA_DEVICE = "android.bluetooth.device.extra.DEVICE";

    /**
     * Used as a String extra field in {@link #ACTION_NAME_CHANGED} and {@link
     * #ACTION_FOUND} intents. It contains the friendly Bluetooth name.
     */
    public static final String EXTRA_NAME = "android.bluetooth.device.extra.NAME";

    /**
     * Used as a Parcelable {@link BluetoothQualityReport} extra field in
     * {@link #ACTION_REMOTE_ISSUE_OCCURRED} intent. It contains the {@link BluetoothQualityReport}.
     * @hide
     */
    public static final String EXTRA_BQR = "android.bluetooth.qti.extra.EXTRA_BQR";

    /**
     * Used as an optional short extra field in {@link #ACTION_FOUND} intents.
     * Contains the RSSI value of the remote device as reported by the
     * Bluetooth hardware.
     */
    public static final String EXTRA_RSSI = "android.bluetooth.device.extra.RSSI";

    /**
    * Used as a boolean extra field in {@link #ACTION_FOUND} intents.
    * It contains the information if device is discovered as member of a coordinated set or not.
    * Pairing with device that belongs to a set would trigger pairing with the rest of set members.
    * See Bluetooth CSIP specification for more details.
    */
    public static final String EXTRA_IS_COORDINATED_SET_MEMBER =
            "android.bluetooth.extra.IS_COORDINATED_SET_MEMBER";

    /**
     * Used as a Parcelable {@link BluetoothClass} extra field in {@link
     * #ACTION_FOUND} and {@link #ACTION_CLASS_CHANGED} intents.
     */
    public static final String EXTRA_CLASS = "android.bluetooth.device.extra.CLASS";

    /**
     * Used as an int extra field in {@link #ACTION_BOND_STATE_CHANGED} intents.
     * Contains the bond state of the remote device.
     * <p>Possible values are:
     * {@link #BOND_NONE},
     * {@link #BOND_BONDING},
     * {@link #BOND_BONDED}.
     */
    public static final String EXTRA_BOND_STATE = "android.bluetooth.device.extra.BOND_STATE";
    /**
     * Used as an int extra field in {@link #ACTION_BOND_STATE_CHANGED} intents.
     * Contains the previous bond state of the remote device.
     * <p>Possible values are:
     * {@link #BOND_NONE},
     * {@link #BOND_BONDING},
     * {@link #BOND_BONDED}.
     */
    public static final String EXTRA_PREVIOUS_BOND_STATE =
            "android.bluetooth.device.extra.PREVIOUS_BOND_STATE";

    /**
     * Used as a boolean extra field to indicate if audio buffer size is low latency or not
     *
     * @hide
     */
    @SuppressLint("ActionValue")
    @SystemApi
    public static final String EXTRA_LOW_LATENCY_BUFFER_SIZE =
            "android.bluetooth.device.extra.LOW_LATENCY_BUFFER_SIZE";

    /**
     * Indicates the remote device is not bonded (paired).
     * <p>There is no shared link key with the remote device, so communication
     * (if it is allowed at all) will be unauthenticated and unencrypted.
     */
    public static final int BOND_NONE = 10;
    /**
     * Indicates bonding (pairing) is in progress with the remote device.
     */
    public static final int BOND_BONDING = 11;
    /**
     * Indicates the remote device is bonded (paired).
     * <p>A shared link keys exists locally for the remote device, so
     * communication can be authenticated and encrypted.
     * <p><i>Being bonded (paired) with a remote device does not necessarily
     * mean the device is currently connected. It just means that the pending
     * procedure was completed at some earlier time, and the link key is still
     * stored locally, ready to use on the next connection.
     * </i>
     */
    public static final int BOND_BONDED = 12;

    /**
     * Used as an int extra field in {@link #ACTION_PAIRING_REQUEST} intents for unbond reason.
     * Possible value are :
     *  - {@link #UNBOND_REASON_AUTH_FAILED}
     *  - {@link #UNBOND_REASON_AUTH_REJECTED}
     *  - {@link #UNBOND_REASON_AUTH_CANCELED}
     *  - {@link #UNBOND_REASON_REMOTE_DEVICE_DOWN}
     *  - {@link #UNBOND_REASON_DISCOVERY_IN_PROGRESS}
     *  - {@link #UNBOND_REASON_AUTH_TIMEOUT}
     *  - {@link #UNBOND_REASON_REPEATED_ATTEMPTS}
     *  - {@link #UNBOND_REASON_REMOTE_AUTH_CANCELED}
     *  - {@link #UNBOND_REASON_REMOVED}
     *
     * Note: Can be added as a hidden extra field for {@link #ACTION_BOND_STATE_CHANGED} when the
     * {@link #EXTRA_BOND_STATE} is {@link #BOND_NONE}
     *
     * @hide
     */
    @SystemApi
    @SuppressLint("ActionValue")
    public static final String EXTRA_UNBOND_REASON = "android.bluetooth.device.extra.REASON";

    /**
     * Use {@link EXTRA_UNBOND_REASON} instead
     * @hide
     */
    @UnsupportedAppUsage
    public static final String EXTRA_REASON = EXTRA_UNBOND_REASON;


    /**
     * Used as an int extra field in {@link #ACTION_PAIRING_REQUEST}
     * intents to indicate pairing method used. Possible values are:
     * {@link #PAIRING_VARIANT_PIN},
     * {@link #PAIRING_VARIANT_PASSKEY_CONFIRMATION},
     */
    public static final String EXTRA_PAIRING_VARIANT =
            "android.bluetooth.device.extra.PAIRING_VARIANT";

    /**
     * Used as an int extra field in {@link #ACTION_PAIRING_REQUEST}
     * intents as the value of passkey.
     * The Bluetooth Passkey is a 6-digit numerical value represented as integer value
     * in the range 0x00000000 – 0x000F423F (000000 to 999999).
     */
    public static final String EXTRA_PAIRING_KEY = "android.bluetooth.device.extra.PAIRING_KEY";

    /**
     * Used as an int extra field in {@link #ACTION_PAIRING_REQUEST}
     * intents as the location of initiator. Possible value are:
     * {@link #EXTRA_PAIRING_INITIATOR_FOREGROUND},
     * {@link #EXTRA_PAIRING_INITIATOR_BACKGROUND},
     *
     * @hide
     */
    @SystemApi
    @SuppressLint("ActionValue")
    public static final String EXTRA_PAIRING_INITIATOR =
            "android.bluetooth.device.extra.PAIRING_INITIATOR";

    /**
     * Bluetooth pairing initiator, Foreground App
     * @hide
     */
    @SystemApi
    public static final int EXTRA_PAIRING_INITIATOR_FOREGROUND = 1;

    /**
     * Bluetooth pairing initiator, Background
     * @hide
     */
    @SystemApi
    public static final int EXTRA_PAIRING_INITIATOR_BACKGROUND = 2;

    /**
     * Bluetooth device type, Unknown
     */
    public static final int DEVICE_TYPE_UNKNOWN = 0;

    /**
     * Bluetooth device type, Classic - BR/EDR devices
     */
    public static final int DEVICE_TYPE_CLASSIC = 1;

    /**
     * Bluetooth device type, Low Energy - LE-only
     */
    public static final int DEVICE_TYPE_LE = 2;

    /**
     * Bluetooth device type, Dual Mode - BR/EDR/LE
     */
    public static final int DEVICE_TYPE_DUAL = 3;


    /** @hide */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static final String ACTION_SDP_RECORD =
            "android.bluetooth.device.action.SDP_RECORD";

    /** @hide */
    @IntDef(prefix = "METADATA_", value = {
            METADATA_MANUFACTURER_NAME,
            METADATA_MODEL_NAME,
            METADATA_SOFTWARE_VERSION,
            METADATA_HARDWARE_VERSION,
            METADATA_COMPANION_APP,
            METADATA_MAIN_ICON,
            METADATA_IS_UNTETHERED_HEADSET,
            METADATA_UNTETHERED_LEFT_ICON,
            METADATA_UNTETHERED_RIGHT_ICON,
            METADATA_UNTETHERED_CASE_ICON,
            METADATA_UNTETHERED_LEFT_BATTERY,
            METADATA_UNTETHERED_RIGHT_BATTERY,
            METADATA_UNTETHERED_CASE_BATTERY,
            METADATA_UNTETHERED_LEFT_CHARGING,
            METADATA_UNTETHERED_RIGHT_CHARGING,
            METADATA_UNTETHERED_CASE_CHARGING,
            METADATA_ENHANCED_SETTINGS_UI_URI,
            METADATA_DEVICE_TYPE,
            METADATA_MAIN_BATTERY,
            METADATA_MAIN_CHARGING,
            METADATA_MAIN_LOW_BATTERY_THRESHOLD,
            METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD,
            METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD,
            METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD,
            METADATA_SPATIAL_AUDIO,
            METADATA_FAST_PAIR_CUSTOMIZED_FIELDS,
            METADATA_LE_AUDIO,
            METADATA_GMCS_CCCD,
            METADATA_GTBS_CCCD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MetadataKey{}

    /**
     * Maximum length of a metadata entry, this is to avoid exploding Bluetooth
     * disk usage
     * @hide
     */
    @SystemApi
    public static final int METADATA_MAX_LENGTH = 2048;

    /**
     * Manufacturer name of this Bluetooth device
     * Data type should be {@String} as {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_MANUFACTURER_NAME = 0;

    /**
     * Model name of this Bluetooth device
     * Data type should be {@String} as {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_MODEL_NAME = 1;

    /**
     * Software version of this Bluetooth device
     * Data type should be {@String} as {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_SOFTWARE_VERSION = 2;

    /**
     * Hardware version of this Bluetooth device
     * Data type should be {@String} as {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_HARDWARE_VERSION = 3;

    /**
     * Package name of the companion app, if any
     * Data type should be {@String} as {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_COMPANION_APP = 4;

    /**
     * URI to the main icon shown on the settings UI
     * Data type should be {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_MAIN_ICON = 5;

    /**
     * Whether this device is an untethered headset with left, right and case
     * Data type should be {@String} as {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_IS_UNTETHERED_HEADSET = 6;

    /**
     * URI to icon of the left headset
     * Data type should be {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_UNTETHERED_LEFT_ICON = 7;

    /**
     * URI to icon of the right headset
     * Data type should be {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_UNTETHERED_RIGHT_ICON = 8;

    /**
     * URI to icon of the headset charging case
     * Data type should be {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_UNTETHERED_CASE_ICON = 9;

    /**
     * Battery level of left headset
     * Data type should be {@String} 0-100 as {@link Byte} array, otherwise
     * as invalid.
     * @hide
     */
    @SystemApi
    public static final int METADATA_UNTETHERED_LEFT_BATTERY = 10;

    /**
     * Battery level of rigth headset
     * Data type should be {@String} 0-100 as {@link Byte} array, otherwise
     * as invalid.
     * @hide
     */
    @SystemApi
    public static final int METADATA_UNTETHERED_RIGHT_BATTERY = 11;

    /**
     * Battery level of the headset charging case
     * Data type should be {@String} 0-100 as {@link Byte} array, otherwise
     * as invalid.
     * @hide
     */
    @SystemApi
    public static final int METADATA_UNTETHERED_CASE_BATTERY = 12;

    /**
     * Whether the left headset is charging
     * Data type should be {@String} as {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_UNTETHERED_LEFT_CHARGING = 13;

    /**
     * Whether the right headset is charging
     * Data type should be {@String} as {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_UNTETHERED_RIGHT_CHARGING = 14;

    /**
     * Whether the headset charging case is charging
     * Data type should be {@String} as {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_UNTETHERED_CASE_CHARGING = 15;

    /**
     * URI to the enhanced settings UI slice
     * Data type should be {@String} as {@link Byte} array, null means
     * the UI does not exist.
     * @hide
     */
    @SystemApi
    public static final int METADATA_ENHANCED_SETTINGS_UI_URI = 16;

    /**
     * @hide
     */
    public static final String COMPANION_TYPE_PRIMARY = "COMPANION_PRIMARY";

    /**
     * @hide
     */
    public static final String COMPANION_TYPE_SECONDARY = "COMPANION_SECONDARY";

    /**
     * @hide
     */
    public static final String COMPANION_TYPE_NONE = "COMPANION_NONE";

    /**
     * Type of the Bluetooth device, must be within the list of
     * BluetoothDevice.DEVICE_TYPE_*
     * Data type should be {@String} as {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_DEVICE_TYPE = 17;

    /**
     * Battery level of the Bluetooth device, use when the Bluetooth device
     * does not support HFP battery indicator.
     * Data type should be {@String} as {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_MAIN_BATTERY = 18;

    /**
     * Whether the device is charging.
     * Data type should be {@String} as {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_MAIN_CHARGING = 19;

    /**
     * The battery threshold of the Bluetooth device to show low battery icon.
     * Data type should be {@String} as {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_MAIN_LOW_BATTERY_THRESHOLD = 20;

    /**
     * The battery threshold of the left headset to show low battery icon.
     * Data type should be {@String} as {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD = 21;

    /**
     * The battery threshold of the right headset to show low battery icon.
     * Data type should be {@String} as {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD = 22;

    /**
     * The battery threshold of the case to show low battery icon.
     * Data type should be {@String} as {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD = 23;


    /**
     * The metadata of the audio spatial data.
     * Data type should be {@link Byte} array.
     * @hide
     */
    public static final int METADATA_SPATIAL_AUDIO = 24;

    /**
     * The metadata of the Fast Pair for any custmized feature.
     * Data type should be {@link Byte} array.
     * @hide
     */
    public static final int METADATA_FAST_PAIR_CUSTOMIZED_FIELDS = 25;

    /**
     * The metadata of the Fast Pair for LE Audio capable devices.
     * Data type should be {@link Byte} array.
     * @hide
     */
    @SystemApi
    public static final int METADATA_LE_AUDIO = 26;

    /**
     * The UUIDs (16-bit) of registered to CCC characteristics from Media Control services.
     * Data type should be {@link Byte} array.
     * @hide
     */
    public static final int METADATA_GMCS_CCCD = 27;

    /**
     * The UUIDs (16-bit) of registered to CCC characteristics from Telephony Bearer service.
     * Data type should be {@link Byte} array.
     * @hide
     */
    public static final int METADATA_GTBS_CCCD = 28;

    private static final int METADATA_MAX_KEY = METADATA_GTBS_CCCD;

    /**
     * Device type which is used in METADATA_DEVICE_TYPE
     * Indicates this Bluetooth device is a standard Bluetooth accessory or
     * not listed in METADATA_DEVICE_TYPE_*.
     * @hide
     */
    @SystemApi
    public static final String DEVICE_TYPE_DEFAULT = "Default";

    /**
     * Device type which is used in METADATA_DEVICE_TYPE
     * Indicates this Bluetooth device is a watch.
     * @hide
     */
    @SystemApi
    public static final String DEVICE_TYPE_WATCH = "Watch";

    /**
     * Device type which is used in METADATA_DEVICE_TYPE
     * Indicates this Bluetooth device is an untethered headset.
     * @hide
     */
    @SystemApi
    public static final String DEVICE_TYPE_UNTETHERED_HEADSET = "Untethered Headset";

    /**
     * Device type which is used in METADATA_DEVICE_TYPE
     * Indicates this Bluetooth device is a stylus.
     * @hide
     */
    @SystemApi
    public static final String DEVICE_TYPE_STYLUS = "Stylus";

    /**
     * Broadcast Action: This intent is used to broadcast the {@link UUID}
     * wrapped as a {@link android.os.ParcelUuid} of the remote device after it
     * has been fetched. This intent is sent only when the UUIDs of the remote
     * device are requested to be fetched using Service Discovery Protocol
     * <p> Always contains the extra field {@link #EXTRA_DEVICE}
     * <p> Always contains the extra field {@link #EXTRA_UUID}
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_UUID =
            "android.bluetooth.device.action.UUID";

    /** @hide */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_MAS_INSTANCE =
            "android.bluetooth.device.action.MAS_INSTANCE";

    /**
     * Broadcast Action: Indicates a failure to retrieve the name of a remote
     * device.
     * <p>Always contains the extra field {@link #EXTRA_DEVICE}.
     *
     * @hide
     */
    //TODO: is this actually useful?
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_NAME_FAILED =
            "android.bluetooth.device.action.NAME_FAILED";

    /**
     * Broadcast Action: This intent is used to broadcast PAIRING REQUEST
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_PAIRING_REQUEST =
            "android.bluetooth.device.action.PAIRING_REQUEST";

    /**
     * Starting from {@link android.os.Build.VERSION_CODES#UPSIDE_DOWN_CAKE},
     * the return value of {@link BluetoothDevice#toString()} has changed
     * to improve privacy.
     */
    @ChangeId
    @EnabledSince(targetSdkVersion = android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private static final long CHANGE_TO_STRING_REDACTED = 265103382L;

    /**
     * Broadcast Action: This intent is used to broadcast PAIRING CANCEL
     *
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @SuppressLint("ActionValue")
    public static final String ACTION_PAIRING_CANCEL =
            "android.bluetooth.device.action.PAIRING_CANCEL";

    /**
     * Broadcast Action: This intent is used to broadcast CONNECTION ACCESS REQUEST
     *
     * This action will trigger a prompt for the user to accept or deny giving the
     * permission for this device. Permissions can be specified with
     * {@link #EXTRA_ACCESS_REQUEST_TYPE}.
     *
     * The reply will be an {@link #ACTION_CONNECTION_ACCESS_REPLY} sent to the specified
     * {@link #EXTRA_PACKAGE_NAME} and {@link #EXTRA_CLASS_NAME}.
     *
     * This action can be cancelled with {@link #ACTION_CONNECTION_ACCESS_CANCEL}.
     *
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @SuppressLint("ActionValue")
    public static final String ACTION_CONNECTION_ACCESS_REQUEST =
            "android.bluetooth.device.action.CONNECTION_ACCESS_REQUEST";

    /**
     * Broadcast Action: This intent is used to broadcast CONNECTION ACCESS REPLY
     *
     * This action is the reply from {@link #ACTION_CONNECTION_ACCESS_REQUEST}
     * that is sent to the specified {@link #EXTRA_PACKAGE_NAME}
     * and {@link #EXTRA_CLASS_NAME}.
     *
     * See the extra fields {@link #EXTRA_CONNECTION_ACCESS_RESULT} and
     * {@link #EXTRA_ALWAYS_ALLOWED} for possible results.
     *
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @SuppressLint("ActionValue")
    public static final String ACTION_CONNECTION_ACCESS_REPLY =
            "android.bluetooth.device.action.CONNECTION_ACCESS_REPLY";

    /**
     * Broadcast Action: This intent is used to broadcast CONNECTION ACCESS CANCEL
     *
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @SuppressLint("ActionValue")
    public static final String ACTION_CONNECTION_ACCESS_CANCEL =
            "android.bluetooth.device.action.CONNECTION_ACCESS_CANCEL";

    /**
     * Intent to broadcast silence mode changed.
     * Alway contains the extra field {@link #EXTRA_DEVICE}
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @SystemApi
    public static final String ACTION_SILENCE_MODE_CHANGED =
            "android.bluetooth.device.action.SILENCE_MODE_CHANGED";

    /**
     * Used as an extra field in {@link #ACTION_CONNECTION_ACCESS_REQUEST}.
     *
     * Possible values are {@link #REQUEST_TYPE_PROFILE_CONNECTION},
     * {@link #REQUEST_TYPE_PHONEBOOK_ACCESS}, {@link #REQUEST_TYPE_MESSAGE_ACCESS}
     * and {@link #REQUEST_TYPE_SIM_ACCESS}
     *
     * @hide
     */
    @SystemApi
    @SuppressLint("ActionValue")
    public static final String EXTRA_ACCESS_REQUEST_TYPE =
            "android.bluetooth.device.extra.ACCESS_REQUEST_TYPE";

    /** @hide */
    @SystemApi
    public static final int REQUEST_TYPE_PROFILE_CONNECTION = 1;

    /** @hide */
    @SystemApi
    public static final int REQUEST_TYPE_PHONEBOOK_ACCESS = 2;

    /** @hide */
    @SystemApi
    public static final int REQUEST_TYPE_MESSAGE_ACCESS = 3;

    /** @hide */
    @SystemApi
    public static final int REQUEST_TYPE_SIM_ACCESS = 4;

    /**
     * Used as an extra field in {@link #ACTION_CONNECTION_ACCESS_REQUEST} intents,
     * Contains package name to return reply intent to.
     *
     * @hide
     */
    public static final String EXTRA_PACKAGE_NAME = "android.bluetooth.device.extra.PACKAGE_NAME";

    /**
     * Used as an extra field in {@link #ACTION_CONNECTION_ACCESS_REQUEST} intents,
     * Contains class name to return reply intent to.
     *
     * @hide
     */
    public static final String EXTRA_CLASS_NAME = "android.bluetooth.device.extra.CLASS_NAME";

    /**
     * Used as an extra field in {@link #ACTION_CONNECTION_ACCESS_REPLY} intent.
     *
     * Possible values are {@link #CONNECTION_ACCESS_YES} and {@link #CONNECTION_ACCESS_NO}.
     *
     * @hide
     */
    @SystemApi
    @SuppressLint("ActionValue")
    public static final String EXTRA_CONNECTION_ACCESS_RESULT =
            "android.bluetooth.device.extra.CONNECTION_ACCESS_RESULT";

    /** @hide */
    @SystemApi
    public static final int CONNECTION_ACCESS_YES = 1;

    /** @hide */
    @SystemApi
    public static final int CONNECTION_ACCESS_NO = 2;

    /**
     * Used as an extra field in {@link #ACTION_CONNECTION_ACCESS_REPLY} intents,
     * Contains boolean to indicate if the allowed response is once-for-all so that
     * next request will be granted without asking user again.
     *
     * @hide
     */
    @SystemApi
    @SuppressLint("ActionValue")
    public static final String EXTRA_ALWAYS_ALLOWED =
            "android.bluetooth.device.extra.ALWAYS_ALLOWED";

    /**
     * A bond attempt succeeded
     *
     * @hide
     */
    public static final int BOND_SUCCESS = 0;

    /**
     * A bond attempt failed because pins did not match, or remote device did
     * not respond to pin request in time
     *
     * @hide
     */
    @SystemApi
    public static final int UNBOND_REASON_AUTH_FAILED = 1;

    /**
     * A bond attempt failed because the other side explicitly rejected
     * bonding
     *
     * @hide
     */
    @SystemApi
    public static final int UNBOND_REASON_AUTH_REJECTED = 2;

    /**
     * A bond attempt failed because we canceled the bonding process
     *
     * @hide
     */
    @SystemApi
    public static final int UNBOND_REASON_AUTH_CANCELED = 3;

    /**
     * A bond attempt failed because we could not contact the remote device
     *
     * @hide
     */
    @SystemApi
    public static final int UNBOND_REASON_REMOTE_DEVICE_DOWN = 4;

    /**
     * A bond attempt failed because a discovery is in progress
     *
     * @hide
     */
    @SystemApi
    public static final int UNBOND_REASON_DISCOVERY_IN_PROGRESS = 5;

    /**
     * A bond attempt failed because of authentication timeout
     *
     * @hide
     */
    @SystemApi
    public static final int UNBOND_REASON_AUTH_TIMEOUT = 6;

    /**
     * A bond attempt failed because of repeated attempts
     *
     * @hide
     */
    @SystemApi
    public static final int UNBOND_REASON_REPEATED_ATTEMPTS = 7;

    /**
     * A bond attempt failed because we received an Authentication Cancel
     * by remote end
     *
     * @hide
     */
    @SystemApi
    public static final int UNBOND_REASON_REMOTE_AUTH_CANCELED = 8;

    /**
     * An existing bond was explicitly revoked
     *
     * @hide
     */
    @SystemApi
    public static final int UNBOND_REASON_REMOVED = 9;

    /**
     * The user will be prompted to enter a pin or
     * an app will enter a pin for user.
     */
    public static final int PAIRING_VARIANT_PIN = 0;

    /**
     * The user will be prompted to enter a passkey
     *
     * @hide
     */
    @SystemApi
    public static final int PAIRING_VARIANT_PASSKEY = 1;

    /**
     * The user will be prompted to confirm the passkey displayed on the screen or
     * an app will confirm the passkey for the user.
     */
    public static final int PAIRING_VARIANT_PASSKEY_CONFIRMATION = 2;

    /**
     * The user will be prompted to accept or deny the incoming pairing request
     *
     * @hide
     */
    @SystemApi
    public static final int PAIRING_VARIANT_CONSENT = 3;

    /**
     * The user will be prompted to enter the passkey displayed on remote device
     * This is used for Bluetooth 2.1 pairing.
     *
     * @hide
     */
    @SystemApi
    public static final int PAIRING_VARIANT_DISPLAY_PASSKEY = 4;

    /**
     * The user will be prompted to enter the PIN displayed on remote device.
     * This is used for Bluetooth 2.0 pairing.
     *
     * @hide
     */
    @SystemApi
    public static final int PAIRING_VARIANT_DISPLAY_PIN = 5;

    /**
     * The user will be prompted to accept or deny the OOB pairing request.
     * This is used for Bluetooth 2.1 secure simple pairing.
     *
     * @hide
     */
    @SystemApi
    public static final int PAIRING_VARIANT_OOB_CONSENT = 6;

    /**
     * The user will be prompted to enter a 16 digit pin or
     * an app will enter a 16 digit pin for user.
     *
     * @hide
     */
    @SystemApi
    public static final int PAIRING_VARIANT_PIN_16_DIGITS = 7;

    /**
     * Used as an extra field in {@link #ACTION_UUID} intents,
     * Contains the {@link android.os.ParcelUuid}s of the remote device which
     * is a parcelable version of {@link UUID}.
     * A {@code null} EXTRA_UUID indicates a timeout.
     */
    public static final String EXTRA_UUID = "android.bluetooth.device.extra.UUID";

    /** @hide */
    public static final String EXTRA_SDP_RECORD =
            "android.bluetooth.device.extra.SDP_RECORD";

    /** @hide */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static final String EXTRA_SDP_SEARCH_STATUS =
            "android.bluetooth.device.extra.SDP_SEARCH_STATUS";

    /** @hide */
    @IntDef(prefix = "ACCESS_", value = {ACCESS_UNKNOWN,
            ACCESS_ALLOWED, ACCESS_REJECTED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AccessPermission{}

    /**
     * For {@link #getPhonebookAccessPermission}, {@link #setPhonebookAccessPermission},
     * {@link #getMessageAccessPermission} and {@link #setMessageAccessPermission}.
     *
     * @hide
     */
    @SystemApi
    public static final int ACCESS_UNKNOWN = 0;

    /**
     * For {@link #getPhonebookAccessPermission}, {@link #setPhonebookAccessPermission},
     * {@link #getMessageAccessPermission} and {@link #setMessageAccessPermission}.
     *
     * @hide
     */
    @SystemApi
    public static final int ACCESS_ALLOWED = 1;

    /**
     * For {@link #getPhonebookAccessPermission}, {@link #setPhonebookAccessPermission},
     * {@link #getMessageAccessPermission} and {@link #setMessageAccessPermission}.
     *
     * @hide
     */
    @SystemApi
    public static final int ACCESS_REJECTED = 2;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        prefix = { "TRANSPORT_" },
        value = {
            TRANSPORT_AUTO,
            TRANSPORT_BREDR,
            TRANSPORT_LE,
        }
    )
    public @interface Transport {}

    /**
     * No preference of physical transport for GATT connections to remote dual-mode devices
     */
    public static final int TRANSPORT_AUTO = 0;

    /**
     * Constant representing the BR/EDR transport.
     */
    public static final int TRANSPORT_BREDR = 1;

    /**
     * Constant representing the Bluetooth Low Energy (BLE) Transport.
     */
    public static final int TRANSPORT_LE = 2;

    /**
     * Bluetooth LE 1M PHY. Used to refer to LE 1M Physical Channel for advertising, scanning or
     * connection.
     */
    public static final int PHY_LE_1M = 1;

    /**
     * Bluetooth LE 2M PHY. Used to refer to LE 2M Physical Channel for advertising, scanning or
     * connection.
     */
    public static final int PHY_LE_2M = 2;

    /**
     * Bluetooth LE Coded PHY. Used to refer to LE Coded Physical Channel for advertising, scanning
     * or connection.
     */
    public static final int PHY_LE_CODED = 3;

    /**
     * Bluetooth LE 1M PHY mask. Used to specify LE 1M Physical Channel as one of many available
     * options in a bitmask.
     */
    public static final int PHY_LE_1M_MASK = 1;

    /**
     * Bluetooth LE 2M PHY mask. Used to specify LE 2M Physical Channel as one of many available
     * options in a bitmask.
     */
    public static final int PHY_LE_2M_MASK = 2;

    /**
     * Bluetooth LE Coded PHY mask. Used to specify LE Coded Physical Channel as one of many
     * available options in a bitmask.
     */
    public static final int PHY_LE_CODED_MASK = 4;

    /**
     * No preferred coding when transmitting on the LE Coded PHY.
     */
    public static final int PHY_OPTION_NO_PREFERRED = 0;

    /**
     * Prefer the S=2 coding to be used when transmitting on the LE Coded PHY.
     */
    public static final int PHY_OPTION_S2 = 1;

    /**
     * Prefer the S=8 coding to be used when transmitting on the LE Coded PHY.
     */
    public static final int PHY_OPTION_S8 = 2;


    /** @hide */
    public static final String EXTRA_MAS_INSTANCE =
            "android.bluetooth.device.extra.MAS_INSTANCE";

    /**
     * Used as an int extra field in {@link #ACTION_ACL_CONNECTED} and
     * {@link #ACTION_ACL_DISCONNECTED} intents to indicate which transport is connected.
     * Possible values are: {@link #TRANSPORT_BREDR} and {@link #TRANSPORT_LE}.
     */
    @SuppressLint("ActionValue")
    public static final String EXTRA_TRANSPORT = "android.bluetooth.device.extra.TRANSPORT";

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        prefix = { "ADDRESS_TYPE_" },
        value = {
            ADDRESS_TYPE_PUBLIC,
            ADDRESS_TYPE_RANDOM,
            ADDRESS_TYPE_UNKNOWN,
        }
    )
    public @interface AddressType {}

    /** Hardware MAC Address of the device */
    public static final int ADDRESS_TYPE_PUBLIC = 0;
    /** Address is either resolvable, non-resolvable or static. */
    public static final int ADDRESS_TYPE_RANDOM = 1;
    /** Address type is unknown or unavailable **/
    public static final int ADDRESS_TYPE_UNKNOWN = 0xFFFF;

    private static final String NULL_MAC_ADDRESS = "00:00:00:00:00:00";

    private final String mAddress;
    @AddressType private final int mAddressType;

    private static boolean sIsLogRedactionFlagSynced = false;
    private static boolean sIsLogRedactionEnabled = true;

    private AttributionSource mAttributionSource;

    static IBluetooth getService() {
        return BluetoothAdapter.getDefaultAdapter().getBluetoothService();
    }

    /**
     * Create a new BluetoothDevice.
     * Bluetooth MAC address must be upper case, such as "00:11:22:33:AA:BB",
     * and is validated in this constructor.
     *
     * @param address valid Bluetooth MAC address
     * @param addressType valid address type
     * @throws RuntimeException Bluetooth is not available on this platform
     * @throws IllegalArgumentException address or addressType is invalid
     * @hide
     */
    /*package*/ BluetoothDevice(String address, int addressType) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            throw new IllegalArgumentException(address + " is not a valid Bluetooth address");
        }

        if (addressType != ADDRESS_TYPE_PUBLIC && addressType != ADDRESS_TYPE_RANDOM) {
            throw new IllegalArgumentException(addressType + " is not a Bluetooth address type");
        }

        mAddress = address;
        mAddressType = addressType;
        mAttributionSource = AttributionSource.myAttributionSource();
    }

    /**
     * Create a new BluetoothDevice.
     * Bluetooth MAC address must be upper case, such as "00:11:22:33:AA:BB",
     * and is validated in this constructor.
     *
     * @param address valid Bluetooth MAC address
     * @throws RuntimeException Bluetooth is not available on this platform
     * @throws IllegalArgumentException address is invalid
     * @hide
     */
    @UnsupportedAppUsage
    /*package*/ BluetoothDevice(String address) {
        this(address, ADDRESS_TYPE_PUBLIC);
    }

    /**
     * Create a new BluetoothDevice.
     *
     * @param in valid parcel
     * @throws RuntimeException Bluetooth is not available on this platform
     * @throws IllegalArgumentException address is invalid
     * @hide
     */
    @UnsupportedAppUsage
    /*package*/ BluetoothDevice(Parcel in) {
        this(in.readString(), in.readInt());
    }

    /** {@hide} */
    public void setAttributionSource(@NonNull AttributionSource attributionSource) {
        mAttributionSource = attributionSource;
    }

    /**
     * Method should never be used anywhere. Only exception is from {@link Intent}
     * Used to set the device current attribution source
     *
     * @param attributionSource The associated {@link AttributionSource} for this device in this
     * process
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED)
    public void prepareToEnterProcess(@NonNull AttributionSource attributionSource) {
        setAttributionSource(attributionSource);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o instanceof BluetoothDevice) {
            return mAddress.equals(((BluetoothDevice) o).getAddress());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mAddress.hashCode();
    }

    /**
     * Returns a string representation of this BluetoothDevice.
     * <p> For apps targeting {@link android.os.Build.VERSION_CODES#UPSIDE_DOWN_CAKE}
     * (API level 34) or higher, this returns the MAC address of the device redacted
     * by replacing the hexadecimal digits of leftmost 4 bytes (in big endian order)
     * with "XX", e.g., "XX:XX:XX:XX:12:34". For apps targeting earlier versions,
     * the MAC address is returned without redaction.
     *
     * Warning: The return value of {@link #toString()} may change in the future.
     * It is intended to be used in logging statements. Thus apps should never rely
     * on the return value of {@link #toString()} in their logic. Always use other
     * appropriate APIs instead (e.g., use {@link #getAddress()} to get the MAC address).
     *
     * @return string representation of this BluetoothDevice
     */
    @Override
    public String toString() {
        if (!CompatChanges.isChangeEnabled(CHANGE_TO_STRING_REDACTED)) {
            return mAddress;
        }
        return toStringForLogging();
    }

    private static boolean shouldLogBeRedacted() {
        boolean defaultValue = true;
        if (!sIsLogRedactionFlagSynced) {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null || !adapter.isEnabled()) {
                return defaultValue;
            }
            IBluetooth service = adapter.getBluetoothService();

            if (service == null) {
                Log.e(TAG, "Bluetooth service is not enabled");
                return defaultValue;
            }

            try {
                sIsLogRedactionEnabled = service.isLogRedactionEnabled();
                sIsLogRedactionFlagSynced = true;
            } catch (RemoteException e) {
                // by default, set to true
                Log.e(TAG, "Failed to call IBluetooth.isLogRedactionEnabled"
                            + e.toString() + "\n"
                            + Log.getStackTraceString(new Throwable()));
                return true;
            }
        }
        return sIsLogRedactionEnabled;
    }

    /**
     * Returns a string representation of this BluetoothDevice for logging.
     * So far, this function only returns hardware address.
     * If more information is needed, add it here
     *
     * @return string representation of this BluetoothDevice used for logging
     * @hide
     */
    public String toStringForLogging() {
        return getAddressForLogging();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final @NonNull Creator<BluetoothDevice> CREATOR = new Creator<>() {
        public BluetoothDevice createFromParcel(Parcel in) {
            return new BluetoothDevice(in);
        }

        public BluetoothDevice[] newArray(int size) {
            return new BluetoothDevice[size];
        }
    };

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mAddress);
        out.writeInt(mAddressType);
    }

    /**
     * Returns the hardware address of this BluetoothDevice.
     * <p> For example, "00:11:22:AA:BB:CC".
     *
     * @return Bluetooth hardware address as string
     */
    public String getAddress() {
        if (DBG) Log.d(TAG, "getAddress: mAddress=" + getAddressForLogging());
        return mAddress;
    }

    /**
     * Returns the address type of this BluetoothDevice.
     *
     * @return Bluetooth address type
     * @hide
     */
    public int getAddressType() {
        if (DBG) Log.d(TAG, "mAddressType: " + mAddressType);
        return mAddressType;
    }

    /**
     * Returns the anonymized hardware address of this BluetoothDevice. The first three octets
     * will be suppressed for anonymization.
     * <p> For example, "XX:XX:XX:AA:BB:CC".
     *
     * @return Anonymized bluetooth hardware address as string
     * @hide
     */
    @SystemApi
    @NonNull
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED)
    public String getAnonymizedAddress() {
        return BluetoothUtils.toAnonymizedAddress(mAddress);
    }

    /**
     * Returns string representation of the hardware address of this BluetoothDevice
     * for logging purpose. Depending on the build type and device config,
     * this function returns either full address string (returned by getAddress),
     * or a redacted string with the leftmost 4 bytes shown as 'xx',
     * <p> For example, "xx:xx:xx:xx:aa:bb".
     * This function is intended to avoid leaking full address in logs.
     *
     * @return string representation of the hardware address for logging
     * @hide
     */
    public String getAddressForLogging() {
        if (shouldLogBeRedacted()) {
            return getAnonymizedAddress();
        }
        return mAddress;
    }

    /**
     * Returns the identity address of this BluetoothDevice.
     * <p> For example, "00:11:22:AA:BB:CC".
     *
     * @return Bluetooth identity address as a string
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @Nullable String getIdentityAddress() {
        if (DBG) log("getIdentityAddress()");
        final IBluetooth service = getService();
        final String defaultValue = null;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "BT not enabled. Cannot get identity address");
        } else {
            try {
                final SynchronousResultReceiver<String> recv = SynchronousResultReceiver.get();
                service.getIdentityAddress(mAddress, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(null);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get the friendly Bluetooth name of the remote device.
     *
     * <p>The local adapter will automatically retrieve remote names when
     * performing a device scan, and will cache them. This method just returns
     * the name for this device from the cache.
     *
     * @return the Bluetooth name, or null if there was a problem.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public String getName() {
        if (DBG) log("getName()");
        final IBluetooth service = getService();
        final String defaultValue = null;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "BT not enabled. Cannot get Remote Device name");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<String> recv = SynchronousResultReceiver.get();
                service.getRemoteName(this, mAttributionSource, recv);
                String name = recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
                if (name != null) {
                    // remove whitespace characters from the name
                    return name
                        .replace('\t', ' ')
                        .replace('\n', ' ')
                        .replace('\r', ' ');
                }
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get the Bluetooth device type of the remote device.
     *
     * @return the device type {@link #DEVICE_TYPE_CLASSIC}, {@link #DEVICE_TYPE_LE} {@link
     * #DEVICE_TYPE_DUAL}. {@link #DEVICE_TYPE_UNKNOWN} if it's not available
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public int getType() {
        if (DBG) log("getType()");
        final IBluetooth service = getService();
        final int defaultValue = DEVICE_TYPE_UNKNOWN;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "BT not enabled. Cannot get Remote Device type");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getRemoteType(this, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get the locally modifiable name (alias) of the remote Bluetooth device.
     *
     * @return the Bluetooth alias, the friendly device name if no alias, or
     * null if there was a problem
     */
    @Nullable
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public String getAlias() {
        if (DBG) log("getAlias()");
        final IBluetooth service = getService();
        final String defaultValue = null;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "BT not enabled. Cannot get Remote Device Alias");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<String> recv = SynchronousResultReceiver.get();
                service.getRemoteAlias(this, mAttributionSource, recv);
                String alias = recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
                if (alias == null) {
                    return getName();
                }
                return alias
                        .replace('\t', ' ')
                        .replace('\n', ' ')
                        .replace('\r', ' ');
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            BluetoothStatusCodes.SUCCESS,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ALLOWED,
            BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION,
            BluetoothStatusCodes.ERROR_DEVICE_NOT_BONDED
    })
    public @interface SetAliasReturnValues{}

    /**
     * Sets the locally modifiable name (alias) of the remote Bluetooth device. This method
     * overwrites the previously stored alias. The new alias is saved in local
     * storage so that the change is preserved over power cycles.
     *
     * <p>This method requires the calling app to be associated with Companion Device Manager (see
     * {@link android.companion.CompanionDeviceManager#associate(AssociationRequest,
     * android.companion.CompanionDeviceManager.Callback, Handler)}) and have the
     * {@link android.Manifest.permission#BLUETOOTH_CONNECT} permission. Alternatively, if the
     * caller has the {@link android.Manifest.permission#BLUETOOTH_PRIVILEGED} permission, they can
     * bypass the Companion Device Manager association requirement as well as other permission
     * requirements.
     *
     * @param alias is the new locally modifiable name for the remote Bluetooth device which must
     *              be the empty string. If null, we clear the alias.
     * @return whether the alias was successfully changed
     * @throws IllegalArgumentException if the alias is the empty string
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public @SetAliasReturnValues int setAlias(@Nullable String alias) {
        if (alias != null && alias.isEmpty()) {
            throw new IllegalArgumentException("alias cannot be the empty string");
        }
        if (DBG) log("setAlias(" + alias + ")");
        final IBluetooth service = getService();
        final int defaultValue = BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "BT not enabled. Cannot set Remote Device name");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.setRemoteAlias(this, alias, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                throw e.rethrowFromSystemServer();
            }
        }
        return defaultValue;
    }

    /**
     * Get the most recent identified battery level of this Bluetooth device
     *
     * @return Battery level in percents from 0 to 100, {@link #BATTERY_LEVEL_BLUETOOTH_OFF} if
     * Bluetooth is disabled or {@link #BATTERY_LEVEL_UNKNOWN} if device is disconnected, or does
     * not have any battery reporting service, or return value is invalid
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public @IntRange(from = -100, to = 100) int getBatteryLevel() {
        if (DBG) log("getBatteryLevel()");
        final IBluetooth service = getService();
        final int defaultValue = BATTERY_LEVEL_BLUETOOTH_OFF;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth disabled. Cannot get remote device battery level");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getBatteryLevel(this, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Start the bonding (pairing) process with the remote device.
     * <p>This is an asynchronous call, it will return immediately. Register
     * for {@link #ACTION_BOND_STATE_CHANGED} intents to be notified when
     * the bonding process completes, and its result.
     * <p>Android system services will handle the necessary user interactions
     * to confirm and complete the bonding process.
     *
     * @return false on immediate error, true if bonding will begin
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean createBond() {
        return createBond(TRANSPORT_AUTO);
    }

    /**
     * Start the bonding (pairing) process with the remote device using the
     * specified transport.
     *
     * <p>This is an asynchronous call, it will return immediately. Register
     * for {@link #ACTION_BOND_STATE_CHANGED} intents to be notified when
     * the bonding process completes, and its result.
     * <p>Android system services will handle the necessary user interactions
     * to confirm and complete the bonding process.
     *
     * @param transport The transport to use for the pairing procedure.
     * @return false on immediate error, true if bonding will begin
     * @throws IllegalArgumentException if an invalid transport was specified
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean createBond(int transport) {
        return createBondInternal(transport, null, null);
    }

    /**
     * Start the bonding (pairing) process with the remote device using the
     * Out Of Band mechanism.
     *
     * <p>This is an asynchronous call, it will return immediately. Register
     * for {@link #ACTION_BOND_STATE_CHANGED} intents to be notified when
     * the bonding process completes, and its result.
     *
     * <p>Android system services will handle the necessary user interactions
     * to confirm and complete the bonding process.
     *
     * <p>There are two possible versions of OOB Data.  This data can come in as
     * P192 or P256.  This is a reference to the cryptography used to generate the key.
     * The caller may pass one or both.  If both types of data are passed, then the
     * P256 data will be preferred, and thus used.
     *
     * @param transport - Transport to use
     * @param remoteP192Data - Out Of Band data (P192) or null
     * @param remoteP256Data - Out Of Band data (P256) or null
     * @return false on immediate error, true if bonding will begin
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean createBondOutOfBand(int transport, @Nullable OobData remoteP192Data,
            @Nullable OobData remoteP256Data) {
        if (remoteP192Data == null && remoteP256Data == null) {
            throw new IllegalArgumentException(
                "One or both arguments for the OOB data types are required to not be null."
                + "  Please use createBond() instead if you do not have OOB data to pass.");
        }
        return createBondInternal(transport, remoteP192Data, remoteP256Data);
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    private boolean createBondInternal(int transport, @Nullable OobData remoteP192Data,
            @Nullable OobData remoteP256Data) {
        if (DBG) log("createBondOutOfBand()");
        final IBluetooth service = getService();
        final boolean defaultValue = false;
        if (service == null || !isBluetoothEnabled()) {
            Log.w(TAG, "BT not enabled, createBondOutOfBand failed");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (NULL_MAC_ADDRESS.equals(mAddress)) {
            Log.e(TAG, "Unable to create bond, invalid address " + mAddress);
        } else {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.createBond(this, transport, remoteP192Data, remoteP256Data,
                        mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Gets whether bonding was initiated locally
     *
     * @return true if bonding is initiated locally, false otherwise
     *
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean isBondingInitiatedLocally() {
        if (DBG) log("isBondingInitiatedLocally()");
        final IBluetooth service = getService();
        final boolean defaultValue = false;
        if (service == null || !isBluetoothEnabled()) {
            Log.w(TAG, "BT not enabled, isBondingInitiatedLocally failed");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.isBondingInitiatedLocally(this, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Cancel an in-progress bonding request started with {@link #createBond}.
     *
     * @return true on success, false on error
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean cancelBondProcess() {
        if (DBG) log("cancelBondProcess()");
        final IBluetooth service = getService();
        final boolean defaultValue = false;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "BT not enabled. Cannot cancel Remote Device bond");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            Log.i(TAG, "cancelBondProcess() for device " + toStringForLogging()
                    + " called by pid: " + Process.myPid()
                    + " tid: " + Process.myTid());
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.cancelBondProcess(this, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Remove bond (pairing) with the remote device.
     * <p>Delete the link key associated with the remote device, and
     * immediately terminate connections to that device that require
     * authentication and encryption.
     *
     * @return true on success, false on error
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean removeBond() {
        if (DBG) log("removeBond()");
        final IBluetooth service = getService();
        final boolean defaultValue = false;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "BT not enabled. Cannot remove Remote Device bond");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            Log.i(TAG, "removeBond() for device " + toStringForLogging()
                    + " called by pid: " + Process.myPid()
                    + " tid: " + Process.myTid());
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.removeBond(this, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
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

    private static final IpcDataCache
            .QueryHandler<Pair<IBluetooth, Pair<AttributionSource, BluetoothDevice>>, Integer>
            sBluetoothBondQuery = new IpcDataCache.QueryHandler<>() {
                @RequiresLegacyBluetoothPermission
                @RequiresBluetoothConnectPermission
                @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
                @Override
                public Integer apply(Pair<IBluetooth,
                        Pair<AttributionSource, BluetoothDevice>> pairQuery) {
                    IBluetooth service = pairQuery.first;
                    AttributionSource source = pairQuery.second.first;
                    BluetoothDevice device = pairQuery.second.second;
                    if (DBG) {
                        log("getBondState(" + device.toStringForLogging() + ") uncached");
                    }
                    try {
                        final SynchronousResultReceiver<Integer> recv =
                                SynchronousResultReceiver.get();
                        service.getBondState(device, source, recv);
                        return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(BOND_NONE);
                    } catch (RemoteException | TimeoutException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

    private static final String GET_BOND_STATE_API = "BluetoothDevice_getBondState";

    private static final
            BluetoothCache<Pair<IBluetooth, Pair<AttributionSource, BluetoothDevice>>, Integer>
            sBluetoothBondCache = new BluetoothCache<>(GET_BOND_STATE_API, sBluetoothBondQuery);

    /** @hide */
    public void disableBluetoothGetBondStateCache() {
        sBluetoothBondCache.disableForCurrentProcess();
    }

    /** @hide */
    public static void invalidateBluetoothGetBondStateCache() {
        invalidateCache(GET_BOND_STATE_API);
    }

    /**
     * Get the bond state of the remote device.
     * <p>Possible values for the bond state are:
     * {@link #BOND_NONE},
     * {@link #BOND_BONDING},
     * {@link #BOND_BONDED}.
     *
     * @return the bond state
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public int getBondState() {
        if (DBG) log("getBondState(" + toStringForLogging() + ")");
        final IBluetooth service = getService();
        if (service == null) {
            Log.e(TAG, "BT not enabled. Cannot get bond state");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                return sBluetoothBondCache.query(
                        new Pair<>(service, new Pair<>(mAttributionSource, BluetoothDevice.this)));
            } catch (RuntimeException e) {
                if (!(e.getCause() instanceof TimeoutException)
                        && !(e.getCause() instanceof RemoteException)) {
                    throw e;
                }
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return BOND_NONE;
    }

    /**
     * Checks whether this bluetooth device is associated with CDM and meets the criteria to skip
     * the bluetooth pairing dialog because it has been already consented by the CDM prompt.
     *
     * @return true if we can bond without the dialog, false otherwise
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean canBondWithoutDialog() {
        if (DBG) log("canBondWithoutDialog, device: " + toStringForLogging());
        final IBluetooth service = getService();
        final boolean defaultValue = false;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "BT not enabled. Cannot check if we can skip pairing dialog");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.canBondWithoutDialog(this, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Gets the package name of the application that initiate bonding with this device
     *
     * @return package name of the application, or null of no application initiate bonding with
     * this device
     *
     * @hide
     */
    @SystemApi
    @Nullable
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public String getPackageNameOfBondingApplication() {
        if (DBG) log("getPackageNameOfBondingApplication()");
        final IBluetooth service = getService();
        final String defaultValue = null;
        if (service == null || !isBluetoothEnabled()) {
            Log.w(TAG, "BT not enabled, getPackageNameOfBondingApplication failed");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<String> recv = SynchronousResultReceiver.get();
                service.getPackageNameOfBondingApplication(this, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            BluetoothStatusCodes.SUCCESS,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ALLOWED,
            BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION,
            BluetoothStatusCodes.ERROR_DEVICE_NOT_BONDED
    })
    public @interface ConnectionReturnValues{}

    /**
     * Connects all user enabled and supported bluetooth profiles between the local and remote
     * device. If no profiles are user enabled (e.g. first connection), we connect all supported
     * profiles. If the device is not already connected, this will page the device before initiating
     * profile connections. Connection is asynchronous and you should listen to each profile's
     * broadcast intent ACTION_CONNECTION_STATE_CHANGED to verify whether connection was successful.
     * For example, to verify a2dp is connected, you would listen for
     * {@link BluetoothA2dp#ACTION_CONNECTION_STATE_CHANGED}
     *
     * @return whether the messages were successfully sent to try to connect all profiles
     * @throws IllegalArgumentException if the device address is invalid
     *
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
            android.Manifest.permission.MODIFY_PHONE_STATE,
    })
    public @ConnectionReturnValues int connect() {
        if (DBG) log("connect()");
        if (!BluetoothAdapter.checkBluetoothAddress(getAddress())) {
            throw new IllegalArgumentException("device cannot have an invalid address");
        }
        final IBluetooth service = getService();
        final int defaultValue = BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "BT not enabled. Cannot connect to remote device.");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.connectAllEnabledProfiles(this, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                throw e.rethrowFromSystemServer();
            }
        }
        return defaultValue;
    }

    /**
     * Disconnects all connected bluetooth profiles between the local and remote device.
     * Disconnection is asynchronous, so you should listen to each profile's broadcast intent
     * ACTION_CONNECTION_STATE_CHANGED to verify whether disconnection was successful. For example,
     * to verify a2dp is disconnected, you would listen for
     * {@link BluetoothA2dp#ACTION_CONNECTION_STATE_CHANGED}. Once all profiles have disconnected,
     * the ACL link should come down and {@link #ACTION_ACL_DISCONNECTED} should be broadcast.
     * <p>
     * In the rare event that one or more profiles fail to disconnect, call this method again to
     * send another request to disconnect each connected profile.
     *
     * @return whether the messages were successfully sent to try to disconnect all profiles
     * @throws IllegalArgumentException if the device address is invalid
     *
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @ConnectionReturnValues int disconnect() {
        if (DBG) log("disconnect()");
        if (!BluetoothAdapter.checkBluetoothAddress(getAddress())) {
            throw new IllegalArgumentException("device cannot have an invalid address");
        }
        final IBluetooth service = getService();
        final int defaultValue = BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "BT not enabled. Cannot disconnect to remote device.");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.disconnectAllEnabledProfiles(this, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                throw e.rethrowFromSystemServer();
            }
        }
        return defaultValue;
    }

    /**
     * Returns whether there is an open connection to this device.
     *
     * @return True if there is at least one open connection to this device.
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean isConnected() {
        if (DBG) log("isConnected()");
        final IBluetooth service = getService();
        final int defaultValue = CONNECTION_STATE_DISCONNECTED;
        if (service == null || !isBluetoothEnabled()) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getConnectionState(this, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue)
                        != CONNECTION_STATE_DISCONNECTED;
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        // BT is not enabled, we cannot be connected.
        return false;
    }

    /**
     * Returns the ACL connection handle associated with an open connection to
     * this device on the given transport.
     *
     * This handle is a unique identifier for the connection while it remains
     * active. Refer to the Bluetooth Core Specification Version 5.4 Vol 4 Part E
     * Section 5.3.1 Controller Handles for details.
     *
     * @return the ACL handle, or {@link BluetoothDevice#ERROR} if no connection currently exists on
     *         the given transport.
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public int getConnectionHandle(@Transport int transport) {
        if (DBG) {
            log("getConnectionHandle()");
        }
        final IBluetooth service = getService();
        if (service == null || !isBluetoothEnabled()) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) {
                log(Log.getStackTraceString(new Throwable()));
            }
        } else {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getConnectionHandle(this, transport, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(-1);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        // BT is not enabled, we cannot be connected.
        return BluetoothDevice.ERROR;
    }

    /**
     * Returns whether there is an open connection to this device
     * that has been encrypted.
     *
     * @return True if there is at least one encrypted connection to this device.
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean isEncrypted() {
        if (DBG) log("isEncrypted()");
        final IBluetooth service = getService();
        final int defaultValue = CONNECTION_STATE_DISCONNECTED;
        if (service == null || !isBluetoothEnabled()) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getConnectionState(this, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue)
                        > CONNECTION_STATE_CONNECTED;
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        // BT is not enabled, we cannot be encrypted.
        return false;
    }

    /**
     * Get the Bluetooth class of the remote device.
     *
     * @return Bluetooth class object, or null on error
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothClass getBluetoothClass() {
        if (DBG) log("getBluetoothClass()");
        final IBluetooth service = getService();
        final int defaultValue = 0;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "BT not enabled. Cannot get Bluetooth Class");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getRemoteClass(this, mAttributionSource, recv);
                int classInt = recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
                if (classInt == BluetoothClass.ERROR) return null;
                return new BluetoothClass(classInt);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return null;
    }

    /**
     * Returns the supported features (UUIDs) of the remote device.
     *
     * <p>This method does not start a service discovery procedure to retrieve the UUIDs
     * from the remote device. Instead, the local cached copy of the service
     * UUIDs are returned.
     * <p>Use {@link #fetchUuidsWithSdp} if fresh UUIDs are desired.
     *
     * @return the supported features (UUIDs) of the remote device, or null on error
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public ParcelUuid[] getUuids() {
        if (DBG) log("getUuids()");
        final IBluetooth service = getService();
        final ParcelUuid[] defaultValue = null;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "BT not enabled. Cannot get remote device Uuids");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<List<ParcelUuid>> recv =
                        SynchronousResultReceiver.get();
                service.getRemoteUuids(this, mAttributionSource, recv);
                List<ParcelUuid> parcels = recv.awaitResultNoInterrupt(getSyncTimeout())
                        .getValue(null);
                return parcels != null ? parcels.toArray(new ParcelUuid[parcels.size()]) : null;
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Perform a service discovery on the remote device to get the UUIDs supported.
     *
     * <p>This API is asynchronous and {@link #ACTION_UUID} intent is sent,
     * with the UUIDs supported by the remote end. If there is an error
     * in getting the SDP records or if the process takes a long time, or the device is bonding and
     * we have its UUIDs cached, {@link #ACTION_UUID} intent is sent with the UUIDs that is
     * currently present in the cache. Clients should use the {@link #getUuids} to get UUIDs
     * if service discovery is not to be performed. If there is an ongoing bonding process,
     * service discovery or device inquiry, the request will be queued.
     *
     * @return False if the check fails, True if the process of initiating an ACL connection
     * to the remote device was started or cached UUIDs will be broadcast.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean fetchUuidsWithSdp() {
        return fetchUuidsWithSdp(TRANSPORT_AUTO);
    }

    /**
     * Perform a service discovery on the remote device to get the UUIDs supported with the
     * specific transport.
     *
     * <p>This API is asynchronous and {@link #ACTION_UUID} intent is sent,
     * with the UUIDs supported by the remote end. If there is an error
     * in getting the SDP or GATT records or if the process takes a long time, or the device
     * is bonding and we have its UUIDs cached, {@link #ACTION_UUID} intent is sent with the
     * UUIDs that is currently present in the cache. Clients should use the {@link #getUuids}
     * to get UUIDs if service discovery is not to be performed. If there is an ongoing bonding
     * process, service discovery or device inquiry, the request will be queued.
     *
     * @param transport - provide type of transport (e.g. LE or Classic).
     * @return False if the check fails, True if the process of initiating an ACL connection
     * to the remote device was started or cached UUIDs will be broadcast with the specific
     * transport.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean fetchUuidsWithSdp(@Transport int transport) {
        if (DBG) log("fetchUuidsWithSdp()");
        final IBluetooth service = getService();
        final boolean defaultValue = false;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "BT not enabled. Cannot fetchUuidsWithSdp");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.fetchRemoteUuids(this, transport, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Perform a service discovery on the remote device to get the SDP records associated
     * with the specified UUID.
     *
     * <p>This API is asynchronous and {@link #ACTION_SDP_RECORD} intent is sent,
     * with the SDP records found on the remote end. If there is an error
     * in getting the SDP records or if the process takes a long time,
     * {@link #ACTION_SDP_RECORD} intent is sent with an status value in
     * {@link #EXTRA_SDP_SEARCH_STATUS} different from 0.
     * Detailed status error codes can be found by members of the Bluetooth package in
     * the AbstractionLayer class.
     * <p>The SDP record data will be stored in the intent as {@link #EXTRA_SDP_RECORD}.
     * The object type will match one of the SdpXxxRecord types, depending on the UUID searched
     * for.
     *
     * @return False if the check fails, True if the process
     *               of initiating an ACL connection to the remote device
     *               was started.
     */
    /** @hide */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean sdpSearch(ParcelUuid uuid) {
        if (DBG) log("sdpSearch()");
        final IBluetooth service = getService();
        final boolean defaultValue = false;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "BT not enabled. Cannot query remote device sdp records");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.sdpSearch(this, uuid, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Set the pin during pairing when the pairing method is {@link #PAIRING_VARIANT_PIN}
     *
     * @return true pin has been set false for error
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean setPin(byte[] pin) {
        if (DBG) log("setPin()");
        final IBluetooth service = getService();
        final boolean defaultValue = false;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "BT not enabled. Cannot set Remote Device pin");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.setPin(this, true, pin.length, pin, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Set the pin during pairing when the pairing method is {@link #PAIRING_VARIANT_PIN}
     *
     * @return true pin has been set false for error
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean setPin(@NonNull String pin) {
        byte[] pinBytes = convertPinToBytes(pin);
        if (pinBytes == null) {
            return false;
        }
        return setPin(pinBytes);
    }

    /**
     * Confirm passkey for {@link #PAIRING_VARIANT_PASSKEY_CONFIRMATION} pairing.
     *
     * @return true confirmation has been sent out false for error
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean setPairingConfirmation(boolean confirm) {
        if (DBG) log("setPairingConfirmation()");
        final IBluetooth service = getService();
        final boolean defaultValue = false;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "BT not enabled. Cannot set pairing confirmation");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.setPairingConfirmation(this, confirm, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    boolean isBluetoothEnabled() {
        boolean ret = false;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && adapter.isEnabled()) {
            ret = true;
        }
        return ret;
    }

    /**
     * Gets whether the phonebook access is allowed for this bluetooth device
     *
     * @return Whether the phonebook access is allowed to this device. Can be {@link
     * #ACCESS_UNKNOWN}, {@link #ACCESS_ALLOWED} or {@link #ACCESS_REJECTED}.
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public @AccessPermission int getPhonebookAccessPermission() {
        if (DBG) log("getPhonebookAccessPermission()");
        final IBluetooth service = getService();
        final int defaultValue = ACCESS_UNKNOWN;
        if (service == null || !isBluetoothEnabled()) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getPhonebookAccessPermission(this, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Sets whether the {@link BluetoothDevice} enters silence mode. Audio will not
     * be routed to the {@link BluetoothDevice} if set to {@code true}.
     *
     * When the {@link BluetoothDevice} enters silence mode, and the {@link BluetoothDevice}
     * is an active device (for A2DP or HFP), the active device for that profile
     * will be set to null.
     * If the {@link BluetoothDevice} exits silence mode while the A2DP or HFP
     * active device is null, the {@link BluetoothDevice} will be set as the
     * active device for that profile.
     * If the {@link BluetoothDevice} is disconnected, it exits silence mode.
     * If the {@link BluetoothDevice} is set as the active device for A2DP or
     * HFP, while silence mode is enabled, then the device will exit silence mode.
     * If the {@link BluetoothDevice} is in silence mode, AVRCP position change
     * event and HFP AG indicators will be disabled.
     * If the {@link BluetoothDevice} is not connected with A2DP or HFP, it cannot
     * enter silence mode.
     *
     * @param silence true to enter silence mode, false to exit
     * @return true on success, false on error.
     * @throws IllegalStateException if Bluetooth is not turned ON.
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean setSilenceMode(boolean silence) {
        if (DBG) log("setSilenceMode()");
        final IBluetooth service = getService();
        final boolean defaultValue = false;
        if (service == null || !isBluetoothEnabled()) {
            throw new IllegalStateException("Bluetooth is not turned ON");
        } else {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.setSilenceMode(this, silence, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Check whether the {@link BluetoothDevice} is in silence mode
     *
     * @return true on device in silence mode, otherwise false.
     * @throws IllegalStateException if Bluetooth is not turned ON.
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean isInSilenceMode() {
        if (DBG) log("isInSilenceMode()");
        final IBluetooth service = getService();
        final boolean defaultValue = false;
        if (service == null || !isBluetoothEnabled()) {
            throw new IllegalStateException("Bluetooth is not turned ON");
        } else {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.getSilenceMode(this, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Sets whether the phonebook access is allowed to this device.
     *
     * @param value Can be {@link #ACCESS_UNKNOWN}, {@link #ACCESS_ALLOWED} or {@link
     * #ACCESS_REJECTED}.
     * @return Whether the value has been successfully set.
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean setPhonebookAccessPermission(@AccessPermission int value) {
        if (DBG) log("setPhonebookAccessPermission()");
        final IBluetooth service = getService();
        final boolean defaultValue = false;
        if (service == null || !isBluetoothEnabled()) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.setPhonebookAccessPermission(this, value, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Gets whether message access is allowed to this bluetooth device
     *
     * @return Whether the message access is allowed to this device.
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public @AccessPermission int getMessageAccessPermission() {
        if (DBG) log("getMessageAccessPermission()");
        final IBluetooth service = getService();
        final int defaultValue = ACCESS_UNKNOWN;
        if (service == null || !isBluetoothEnabled()) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getMessageAccessPermission(this, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Sets whether the message access is allowed to this device.
     *
     * @param value Can be {@link #ACCESS_UNKNOWN} if the device is unbonded,
     * {@link #ACCESS_ALLOWED} if the permission is being granted, or {@link #ACCESS_REJECTED} if
     * the permission is not being granted.
     * @return Whether the value has been successfully set.
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean setMessageAccessPermission(@AccessPermission int value) {
        // Validates param value is one of the accepted constants
        if (value != ACCESS_ALLOWED && value != ACCESS_REJECTED && value != ACCESS_UNKNOWN) {
            throw new IllegalArgumentException(value + "is not a valid AccessPermission value");
        }
        if (DBG) log("setMessageAccessPermission()");
        final IBluetooth service = getService();
        final boolean defaultValue = false;
        if (service == null || !isBluetoothEnabled()) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.setMessageAccessPermission(this, value, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Gets whether sim access is allowed for this bluetooth device
     *
     * @return Whether the Sim access is allowed to this device.
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public @AccessPermission int getSimAccessPermission() {
        if (DBG) log("getSimAccessPermission()");
        final IBluetooth service = getService();
        final int defaultValue = ACCESS_UNKNOWN;
        if (service == null || !isBluetoothEnabled()) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getSimAccessPermission(this, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Sets whether the Sim access is allowed to this device.
     *
     * @param value Can be {@link #ACCESS_UNKNOWN} if the device is unbonded,
     * {@link #ACCESS_ALLOWED} if the permission is being granted, or {@link #ACCESS_REJECTED} if
     * the permission is not being granted.
     * @return Whether the value has been successfully set.
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean setSimAccessPermission(int value) {
        if (DBG) log("setSimAccessPermission()");
        final IBluetooth service = getService();
        final boolean defaultValue = false;
        if (service == null || !isBluetoothEnabled()) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.setSimAccessPermission(this, value, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Create an RFCOMM {@link BluetoothSocket} ready to start a secure
     * outgoing connection to this remote device on given channel.
     * <p>The remote device will be authenticated and communication on this
     * socket will be encrypted.
     * <p> Use this socket only if an authenticated socket link is possible.
     * Authentication refers to the authentication of the link key to
     * prevent person-in-the-middle type of attacks.
     * For example, for Bluetooth 2.1 devices, if any of the devices does not
     * have an input and output capability or just has the ability to
     * display a numeric key, a secure socket connection is not possible.
     * In such a case, use {@link createInsecureRfcommSocket}.
     * For more details, refer to the Security Model section 5.2 (vol 3) of
     * Bluetooth Core Specification version 2.1 + EDR.
     * <p>Use {@link BluetoothSocket#connect} to initiate the outgoing
     * connection.
     * <p>Valid RFCOMM channels are in range 1 to 30.
     *
     * @param channel RFCOMM channel to connect to
     * @return a RFCOMM BluetoothServerSocket ready for an outgoing connection
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions
     * @hide
     */
    @UnsupportedAppUsage
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("AndroidFrameworkRequiresPermission")
    public BluetoothSocket createRfcommSocket(int channel) throws IOException {
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled");
            throw new IOException();
        }
        return new BluetoothSocket(BluetoothSocket.TYPE_RFCOMM, -1, true, true, this, channel,
                null);
    }

    /**
     * Create an L2cap {@link BluetoothSocket} ready to start a secure
     * outgoing connection to this remote device on given channel.
     * <p>The remote device will be authenticated and communication on this
     * socket will be encrypted.
     * <p> Use this socket only if an authenticated socket link is possible.
     * Authentication refers to the authentication of the link key to
     * prevent person-in-the-middle type of attacks.
     * For example, for Bluetooth 2.1 devices, if any of the devices does not
     * have an input and output capability or just has the ability to
     * display a numeric key, a secure socket connection is not possible.
     * In such a case, use {@link createInsecureRfcommSocket}.
     * For more details, refer to the Security Model section 5.2 (vol 3) of
     * Bluetooth Core Specification version 2.1 + EDR.
     * <p>Use {@link BluetoothSocket#connect} to initiate the outgoing
     * connection.
     * <p>Valid L2CAP PSM channels are in range 1 to 2^16.
     *
     * @param channel L2cap PSM/channel to connect to
     * @return a RFCOMM BluetoothServerSocket ready for an outgoing connection
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions
     * @hide
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("AndroidFrameworkRequiresPermission")
    public BluetoothSocket createL2capSocket(int channel) throws IOException {
        return new BluetoothSocket(BluetoothSocket.TYPE_L2CAP, -1, true, true, this, channel,
                null);
    }

    /**
     * Create an L2cap {@link BluetoothSocket} ready to start an insecure
     * outgoing connection to this remote device on given channel.
     * <p>The remote device will be not authenticated and communication on this
     * socket will not be encrypted.
     * <p>Use {@link BluetoothSocket#connect} to initiate the outgoing
     * connection.
     * <p>Valid L2CAP PSM channels are in range 1 to 2^16.
     *
     * @param channel L2cap PSM/channel to connect to
     * @return a RFCOMM BluetoothServerSocket ready for an outgoing connection
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions
     * @hide
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("AndroidFrameworkRequiresPermission")
    public BluetoothSocket createInsecureL2capSocket(int channel) throws IOException {
        return new BluetoothSocket(BluetoothSocket.TYPE_L2CAP, -1, false, false, this, channel,
                null);
    }

    /**
     * Create an RFCOMM {@link BluetoothSocket} ready to start a secure
     * outgoing connection to this remote device using SDP lookup of uuid.
     * <p>This is designed to be used with {@link
     * BluetoothAdapter#listenUsingRfcommWithServiceRecord} for peer-peer
     * Bluetooth applications.
     * <p>Use {@link BluetoothSocket#connect} to initiate the outgoing
     * connection. This will also perform an SDP lookup of the given uuid to
     * determine which channel to connect to.
     * <p>The remote device will be authenticated and communication on this
     * socket will be encrypted.
     * <p> Use this socket only if an authenticated socket link is possible.
     * Authentication refers to the authentication of the link key to
     * prevent person-in-the-middle type of attacks.
     * For example, for Bluetooth 2.1 devices, if any of the devices does not
     * have an input and output capability or just has the ability to
     * display a numeric key, a secure socket connection is not possible.
     * In such a case, use {@link #createInsecureRfcommSocketToServiceRecord}.
     * For more details, refer to the Security Model section 5.2 (vol 3) of
     * Bluetooth Core Specification version 2.1 + EDR.
     * <p>Hint: If you are connecting to a Bluetooth serial board then try
     * using the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB.
     * However if you are connecting to an Android peer then please generate
     * your own unique UUID.
     *
     * @param uuid service record uuid to lookup RFCOMM channel
     * @return a RFCOMM BluetoothServerSocket ready for an outgoing connection
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("AndroidFrameworkRequiresPermission")
    public BluetoothSocket createRfcommSocketToServiceRecord(UUID uuid) throws IOException {
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled");
            throw new IOException();
        }

        return new BluetoothSocket(BluetoothSocket.TYPE_RFCOMM, -1, true, true, this, -1,
                new ParcelUuid(uuid));
    }

    /**
     * Create an RFCOMM {@link BluetoothSocket} socket ready to start an insecure
     * outgoing connection to this remote device using SDP lookup of uuid.
     * <p> The communication channel will not have an authenticated link key
     * i.e it will be subject to person-in-the-middle attacks. For Bluetooth 2.1
     * devices, the link key will be encrypted, as encryption is mandatory.
     * For legacy devices (pre Bluetooth 2.1 devices) the link key will
     * be not be encrypted. Use {@link #createRfcommSocketToServiceRecord} if an
     * encrypted and authenticated communication channel is desired.
     * <p>This is designed to be used with {@link
     * BluetoothAdapter#listenUsingInsecureRfcommWithServiceRecord} for peer-peer
     * Bluetooth applications.
     * <p>Use {@link BluetoothSocket#connect} to initiate the outgoing
     * connection. This will also perform an SDP lookup of the given uuid to
     * determine which channel to connect to.
     * <p>The remote device will be authenticated and communication on this
     * socket will be encrypted.
     * <p>Hint: If you are connecting to a Bluetooth serial board then try
     * using the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB.
     * However if you are connecting to an Android peer then please generate
     * your own unique UUID.
     *
     * @param uuid service record uuid to lookup RFCOMM channel
     * @return a RFCOMM BluetoothServerSocket ready for an outgoing connection
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("AndroidFrameworkRequiresPermission")
    public BluetoothSocket createInsecureRfcommSocketToServiceRecord(UUID uuid) throws IOException {
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled");
            throw new IOException();
        }
        return new BluetoothSocket(BluetoothSocket.TYPE_RFCOMM, -1, false, false, this, -1,
                new ParcelUuid(uuid));
    }

    /**
     * Construct an insecure RFCOMM socket ready to start an outgoing
     * connection.
     * Call #connect on the returned #BluetoothSocket to begin the connection.
     * The remote device will not be authenticated and communication on this
     * socket will not be encrypted.
     *
     * @param port remote port
     * @return An RFCOMM BluetoothSocket
     * @throws IOException On error, for example Bluetooth not available, or insufficient
     * permissions.
     * @hide
     */
    @UnsupportedAppUsage(publicAlternatives = "Use "
            + "{@link #createInsecureRfcommSocketToServiceRecord} instead.")
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("AndroidFrameworkRequiresPermission")
    public BluetoothSocket createInsecureRfcommSocket(int port) throws IOException {
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled");
            throw new IOException();
        }
        return new BluetoothSocket(BluetoothSocket.TYPE_RFCOMM, -1, false, false, this, port,
                null);
    }

    /**
     * Construct a SCO socket ready to start an outgoing connection.
     * Call #connect on the returned #BluetoothSocket to begin the connection.
     *
     * @return a SCO BluetoothSocket
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions.
     * @hide
     */
    @UnsupportedAppUsage
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("AndroidFrameworkRequiresPermission")
    public BluetoothSocket createScoSocket() throws IOException {
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled");
            throw new IOException();
        }
        return new BluetoothSocket(BluetoothSocket.TYPE_SCO, -1, true, true, this, -1, null);
    }

    /**
     * Check that a pin is valid and convert to byte array.
     *
     * Bluetooth pin's are 1 to 16 bytes of UTF-8 characters.
     *
     * @param pin pin as java String
     * @return the pin code as a UTF-8 byte array, or null if it is an invalid Bluetooth pin.
     * @hide
     */
    @UnsupportedAppUsage
    public static byte[] convertPinToBytes(String pin) {
        if (pin == null) {
            return null;
        }
        byte[] pinBytes;
        try {
            pinBytes = pin.getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
            Log.e(TAG, "UTF-8 not supported?!?");  // this should not happen
            return null;
        }
        if (pinBytes.length <= 0 || pinBytes.length > 16) {
            return null;
        }
        return pinBytes;
    }

    /**
     * Connect to GATT Server hosted by this device. Caller acts as GATT client.
     * The callback is used to deliver results to Caller, such as connection status as well
     * as any further GATT client operations.
     * The method returns a BluetoothGatt instance. You can use BluetoothGatt to conduct
     * GATT client operations.
     *
     * @param callback GATT callback handler that will receive asynchronous callbacks.
     * @param autoConnect Whether to directly connect to the remote device (false) or to
     * automatically connect as soon as the remote device becomes available (true).
     * @throws IllegalArgumentException if callback is null
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothGatt connectGatt(Context context, boolean autoConnect,
            BluetoothGattCallback callback) {
        return (connectGatt(context, autoConnect, callback, TRANSPORT_AUTO));
    }

    /**
     * Connect to GATT Server hosted by this device. Caller acts as GATT client.
     * The callback is used to deliver results to Caller, such as connection status as well
     * as any further GATT client operations.
     * The method returns a BluetoothGatt instance. You can use BluetoothGatt to conduct
     * GATT client operations.
     *
     * @param callback GATT callback handler that will receive asynchronous callbacks.
     * @param autoConnect Whether to directly connect to the remote device (false) or to
     * automatically connect as soon as the remote device becomes available (true).
     * @param transport preferred transport for GATT connections to remote dual-mode devices {@link
     * BluetoothDevice#TRANSPORT_AUTO} or {@link BluetoothDevice#TRANSPORT_BREDR} or {@link
     * BluetoothDevice#TRANSPORT_LE}
     * @throws IllegalArgumentException if callback is null
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothGatt connectGatt(Context context, boolean autoConnect,
            BluetoothGattCallback callback, int transport) {
        return (connectGatt(context, autoConnect, callback, transport, PHY_LE_1M_MASK));
    }

    /**
     * Connect to GATT Server hosted by this device. Caller acts as GATT client.
     * The callback is used to deliver results to Caller, such as connection status as well
     * as any further GATT client operations.
     * The method returns a BluetoothGatt instance. You can use BluetoothGatt to conduct
     * GATT client operations.
     *
     * @param callback GATT callback handler that will receive asynchronous callbacks.
     * @param autoConnect Whether to directly connect to the remote device (false) or to
     * automatically connect as soon as the remote device becomes available (true).
     * @param transport preferred transport for GATT connections to remote dual-mode devices {@link
     * BluetoothDevice#TRANSPORT_AUTO} or {@link BluetoothDevice#TRANSPORT_BREDR} or {@link
     * BluetoothDevice#TRANSPORT_LE}
     * @param phy preferred PHY for connections to remote LE device. Bitwise OR of any of {@link
     * BluetoothDevice#PHY_LE_1M_MASK}, {@link BluetoothDevice#PHY_LE_2M_MASK}, and {@link
     * BluetoothDevice#PHY_LE_CODED_MASK}. This option does not take effect if {@code autoConnect}
     * is set to true.
     * @throws NullPointerException if callback is null
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothGatt connectGatt(Context context, boolean autoConnect,
            BluetoothGattCallback callback, int transport, int phy) {
        return connectGatt(context, autoConnect, callback, transport, phy, null);
    }

    /**
     * Connect to GATT Server hosted by this device. Caller acts as GATT client.
     * The callback is used to deliver results to Caller, such as connection status as well
     * as any further GATT client operations.
     * The method returns a BluetoothGatt instance. You can use BluetoothGatt to conduct
     * GATT client operations.
     *
     * @param callback GATT callback handler that will receive asynchronous callbacks.
     * @param autoConnect Whether to directly connect to the remote device (false) or to
     * automatically connect as soon as the remote device becomes available (true).
     * @param transport preferred transport for GATT connections to remote dual-mode devices {@link
     * BluetoothDevice#TRANSPORT_AUTO} or {@link BluetoothDevice#TRANSPORT_BREDR} or {@link
     * BluetoothDevice#TRANSPORT_LE}
     * @param phy preferred PHY for connections to remote LE device. Bitwise OR of any of {@link
     * BluetoothDevice#PHY_LE_1M_MASK}, {@link BluetoothDevice#PHY_LE_2M_MASK}, an d{@link
     * BluetoothDevice#PHY_LE_CODED_MASK}. This option does not take effect if {@code autoConnect}
     * is set to true.
     * @param handler The handler to use for the callback. If {@code null}, callbacks will happen on
     * an un-specified background thread.
     * @throws NullPointerException if callback is null
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothGatt connectGatt(Context context, boolean autoConnect,
            BluetoothGattCallback callback, int transport, int phy,
            Handler handler) {
        return connectGatt(context, autoConnect, callback, transport, false, phy, handler);
    }

    /**
     * Connect to GATT Server hosted by this device. Caller acts as GATT client.
     * The callback is used to deliver results to Caller, such as connection status as well
     * as any further GATT client operations.
     * The method returns a BluetoothGatt instance. You can use BluetoothGatt to conduct
     * GATT client operations.
     *
     * @param callback GATT callback handler that will receive asynchronous callbacks.
     * @param autoConnect Whether to directly connect to the remote device (false) or to
     * automatically connect as soon as the remote device becomes available (true).
     * @param transport preferred transport for GATT connections to remote dual-mode devices {@link
     * BluetoothDevice#TRANSPORT_AUTO} or {@link BluetoothDevice#TRANSPORT_BREDR} or {@link
     * BluetoothDevice#TRANSPORT_LE}
     * @param opportunistic Whether this GATT client is opportunistic. An opportunistic GATT client
     * does not hold a GATT connection. It automatically disconnects when no other GATT connections
     * are active for the remote device.
     * @param phy preferred PHY for connections to remote LE device. Bitwise OR of any of {@link
     * BluetoothDevice#PHY_LE_1M_MASK}, {@link BluetoothDevice#PHY_LE_2M_MASK}, an d{@link
     * BluetoothDevice#PHY_LE_CODED_MASK}. This option does not take effect if {@code autoConnect}
     * is set to true.
     * @param handler The handler to use for the callback. If {@code null}, callbacks will happen on
     * an un-specified background thread.
     * @return A BluetoothGatt instance. You can use BluetoothGatt to conduct GATT client
     * operations.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothGatt connectGatt(Context context, boolean autoConnect,
            BluetoothGattCallback callback, int transport,
            boolean opportunistic, int phy, Handler handler) {
        if (callback == null) {
            throw new NullPointerException("callback is null");
        }

        // TODO(Bluetooth) check whether platform support BLE
        //     Do the check here or in GattServer?
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        IBluetoothManager managerService = adapter.getBluetoothManager();
        try {
            IBluetoothGatt iGatt = managerService.getBluetoothGatt();
            if (iGatt == null) {
                // BLE is not supported
                return null;
            } else if (NULL_MAC_ADDRESS.equals(mAddress)) {
                Log.e(TAG, "Unable to connect gatt, invalid address " + mAddress);
                return null;
            }
            BluetoothGatt gatt = new BluetoothGatt(
                    iGatt, this, transport, opportunistic, phy, mAttributionSource);
            gatt.connect(autoConnect, callback, handler);
            return gatt;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    /**
     * Create a Bluetooth L2CAP Connection-oriented Channel (CoC) {@link BluetoothSocket} that can
     * be used to start a secure outgoing connection to the remote device with the same dynamic
     * protocol/service multiplexer (PSM) value. The supported Bluetooth transport is LE only.
     * <p>This is designed to be used with {@link BluetoothAdapter#listenUsingL2capChannel()} for
     * peer-peer Bluetooth applications.
     * <p>Use {@link BluetoothSocket#connect} to initiate the outgoing connection.
     * <p>Application using this API is responsible for obtaining PSM value from remote device.
     * <p>The remote device will be authenticated and communication on this socket will be
     * encrypted.
     * <p> Use this socket if an authenticated socket link is possible. Authentication refers
     * to the authentication of the link key to prevent person-in-the-middle type of attacks.
     *
     * @param psm dynamic PSM value from remote device
     * @return a CoC #BluetoothSocket ready for an outgoing connection
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("AndroidFrameworkRequiresPermission")
    public @NonNull BluetoothSocket createL2capChannel(int psm) throws IOException {
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "createL2capChannel: Bluetooth is not enabled");
            throw new IOException();
        }
        if (DBG) Log.d(TAG, "createL2capChannel: psm=" + psm);
        return new BluetoothSocket(BluetoothSocket.TYPE_L2CAP_LE, -1, true, true, this, psm,
                null);
    }

    /**
     * Create a Bluetooth L2CAP Connection-oriented Channel (CoC) {@link BluetoothSocket} that can
     * be used to start a secure outgoing connection to the remote device with the same dynamic
     * protocol/service multiplexer (PSM) value. The supported Bluetooth transport is LE only.
     * <p>This is designed to be used with {@link
     * BluetoothAdapter#listenUsingInsecureL2capChannel()} for peer-peer Bluetooth applications.
     * <p>Use {@link BluetoothSocket#connect} to initiate the outgoing connection.
     * <p>Application using this API is responsible for obtaining PSM value from remote device.
     * <p> The communication channel may not have an authenticated link key, i.e. it may be subject
     * to person-in-the-middle attacks. Use {@link #createL2capChannel(int)} if an encrypted and
     * authenticated communication channel is possible.
     *
     * @param psm dynamic PSM value from remote device
     * @return a CoC #BluetoothSocket ready for an outgoing connection
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("AndroidFrameworkRequiresPermission")
    public @NonNull BluetoothSocket createInsecureL2capChannel(int psm) throws IOException {
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "createInsecureL2capChannel: Bluetooth is not enabled");
            throw new IOException();
        }
        if (DBG) {
            Log.d(TAG, "createInsecureL2capChannel: psm=" + psm);
        }
        return new BluetoothSocket(BluetoothSocket.TYPE_L2CAP_LE, -1, false, false, this, psm,
                null);
    }

    /**
     * Set a keyed metadata of this {@link BluetoothDevice} to a
     * {@link String} value.
     * Only bonded devices's metadata will be persisted across Bluetooth
     * restart.
     * Metadata will be removed when the device's bond state is moved to
     * {@link #BOND_NONE}.
     *
     * @param key must be within the list of BluetoothDevice.METADATA_*
     * @param value a byte array data to set for key. Must be less than
     * {@link BluetoothAdapter#METADATA_MAX_LENGTH} characters in length
     * @return true on success, false on error
     * @hide
    */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean setMetadata(@MetadataKey int key, @NonNull byte[] value) {
        if (DBG) log("setMetadata()");
        final IBluetooth service = getService();
        final boolean defaultValue = false;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled. Cannot set metadata");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (value.length > METADATA_MAX_LENGTH) {
            throw new IllegalArgumentException("value length is " + value.length
                    + ", should not over " + METADATA_MAX_LENGTH);
        } else {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.setMetadata(this, key, value, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get a keyed metadata for this {@link BluetoothDevice} as {@link String}
     *
     * @param key must be within the list of BluetoothDevice.METADATA_*
     * @return Metadata of the key as byte array, null on error or not found
     * @hide
     */
    @SystemApi
    @Nullable
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public byte[] getMetadata(@MetadataKey int key) {
        if (DBG) log("getMetadata()");
        final IBluetooth service = getService();
        final byte[] defaultValue = null;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled. Cannot get metadata");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<byte[]> recv = SynchronousResultReceiver.get();
                service.getMetadata(this, key, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get the maxinum metadata key ID.
     *
     * @return the last supported metadata key
     * @hide
     */
    public static @MetadataKey int getMaxMetadataKey() {
        return METADATA_MAX_KEY;
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        prefix = { "FEATURE_" },
        value = {
            BluetoothStatusCodes.FEATURE_NOT_CONFIGURED,
            BluetoothStatusCodes.FEATURE_SUPPORTED,
            BluetoothStatusCodes.FEATURE_NOT_SUPPORTED,
        }
    )

    public @interface AudioPolicyRemoteSupport {}

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            BluetoothStatusCodes.SUCCESS,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ALLOWED,
            BluetoothStatusCodes.ERROR_DEVICE_NOT_BONDED,
            BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION,
            BluetoothStatusCodes.ERROR_PROFILE_NOT_CONNECTED,
    })
    public @interface AudioPolicyReturnValues{}

    /**
     * Returns whether the audio policy feature is supported by
     * both the local and the remote device.
     * This is configured during initiating the connection between the devices through
     * one of the transport protocols (e.g. HFP Vendor specific protocol). So if the API
     * is invoked before this initial configuration is completed, it returns
     * {@link BluetoothStatusCodes#FEATURE_NOT_CONFIGURED} to indicate the remote
     * device has not yet relayed this information. After the internal configuration,
     * the support status will be set to either
     * {@link BluetoothStatusCodes#FEATURE_NOT_SUPPORTED} or
     * {@link BluetoothStatusCodes#FEATURE_SUPPORTED}.
     * The rest of the APIs related to this feature in both {@link BluetoothDevice}
     * and {@link BluetoothSinkAudioPolicy} should be invoked  only after getting a
     * {@link BluetoothStatusCodes#FEATURE_SUPPORTED} response from this API.
     * <p>Note that this API is intended to be used by a client device to send these requests
     * to the server represented by this BluetoothDevice object.
     *
     * @return if call audio policy feature is supported by both local and remote
     * device or not
     *
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @AudioPolicyRemoteSupport int isRequestAudioPolicyAsSinkSupported() {
        if (DBG) log("isRequestAudioPolicyAsSinkSupported()");
        final IBluetooth service = getService();
        final int defaultValue = BluetoothStatusCodes.FEATURE_NOT_CONFIGURED;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "BT not enabled. Cannot retrieve audio policy support status.");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.isRequestAudioPolicyAsSinkSupported(this, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                throw e.rethrowFromSystemServer();
            }
        }
        return defaultValue;
    }

    /**
     * Sets call audio preferences and sends them to the remote device.
     * <p>Note that the caller should check if the feature is supported by
     * invoking {@link BluetoothDevice#isRequestAudioPolicyAsSinkSupported} first.
     * <p>This API will throw an exception if the feature is not supported but still
     * invoked.
     * <p>Note that this API is intended to be used by a client device to send these requests
     * to the server represented by this BluetoothDevice object.
     *
     * @param policies call audio policy preferences
     * @return whether audio policy was requested successfully or not
     *
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @AudioPolicyReturnValues int requestAudioPolicyAsSink(
            @NonNull BluetoothSinkAudioPolicy policies) {
        if (DBG) log("requestAudioPolicyAsSink");
        final IBluetooth service = getService();
        final int defaultValue = BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled. Cannot set Audio Policy.");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.requestAudioPolicyAsSink(this, policies, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Gets the call audio preferences for the remote device.
     * <p>Note that the caller should check if the feature is supported by
     * invoking {@link BluetoothDevice#isRequestAudioPolicyAsSinkSupported} first.
     * <p>This API will throw an exception if the feature is not supported but still
     * invoked.
     * <p>This API will return null if
     * 1. The bleutooth service is not started yet,
     * 2. It is invoked for a device which is not bonded, or
     * 3. The used transport, for example, HFP Client profile is not enabled or
     * connected yet.
     * <p>Note that this API is intended to be used by a client device to send these requests
     * to the server represented by this BluetoothDevice object.
     *
     * @return call audio policy as {@link BluetoothSinkAudioPolicy} object
     *
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @Nullable BluetoothSinkAudioPolicy getRequestedAudioPolicyAsSink() {
        if (DBG) log("getRequestedAudioPolicyAsSink");
        final IBluetooth service = getService();
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled. Cannot get Audio Policy.");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<BluetoothSinkAudioPolicy>
                        recv = SynchronousResultReceiver.get();
                service.getRequestedAudioPolicyAsSink(this, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(null);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return null;
    }

    /**
     * Enable or disable audio low latency for this {@link BluetoothDevice}.
     *
     * @param allowed true if low latency is allowed, false if low latency is disallowed.
     * @return true if the value is successfully set,
     * false if there is a error when setting the value.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean setLowLatencyAudioAllowed(boolean allowed) {
        if (DBG) log("setLowLatencyAudioAllowed(" + allowed + ")");
        final IBluetooth service = getService();
        final boolean defaultValue = false;
        if (service == null || !isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled. Cannot allow low latency");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.allowLowLatencyAudio(allowed, this, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
