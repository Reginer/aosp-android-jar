package com.android.clockwork.bluetooth;

import static android.provider.Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE;
import static android.provider.Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE_ANDROID;
import static android.provider.Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE_IOS;
import static android.provider.Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE_UNKNOWN;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import java.util.HashSet;
import java.util.Set;

/**
 * This class monitors and maintains the mapping to the currently paired Companion device.
 *
 * This class expects the PairingHandler in ClockworkHome to set the Companion address
 * under wear settings provider when the user initiates a successful pairing to the watch
 * from the Companion app.
 *
 * For the legacy case of there being a previously-paired device with no Companion address
 * written in Settings, CompanionTracker will infer the Companion device and store its address
 * into the same Settings location.
 *
 * Design doc:
 * https://docs.google.com/document/d/1E9-wBGZqHCB5Y7hJ-JI6ktb84tlG3e25AIazGOp7kEI/edit#
 */
public class CompanionTracker {
    public static final String TAG = WearBluetoothSettings.LOG_TAG;

    /** Callback when the companion has paired or unpaired to the watch */
    public interface Listener {
        void onCompanionChanged();
    }

    private final ContentResolver mContentResolver;
    @Nullable private final BluetoothAdapter mBtAdapter;
    @VisibleForTesting final SettingsObserver mSettingsObserver;
    private final Set<Listener> mListeners;

    private BluetoothDevice mCompanion;
    private int mCompanionOsType = PAIRED_DEVICE_OS_TYPE_UNKNOWN;

    public CompanionTracker(ContentResolver contentResolver, BluetoothAdapter btAdapter) {
        mContentResolver = contentResolver;
        mBtAdapter = btAdapter;
        mSettingsObserver = new SettingsObserver(new Handler(Looper.getMainLooper()));
        mListeners = new HashSet<>();
        mCompanion = null;

        contentResolver.registerContentObserver(
                WearBluetoothSettings.BLUETOOTH_URI,
                false,
                mSettingsObserver);
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    /**
     * Returns the BluetoothDevice object associated with the Companion device,
     * or null if no Companion device is paired.
     */
    public BluetoothDevice getCompanion() {
        return mCompanion;
    }

    /**
     * Returns the BluetoothDevice name associated with the Companion device,
     * or null if no Companion device is paired.
     */
    public String getCompanionName() {
        if (mCompanion != null) {
            return mCompanion.getName();
        }
        return null;
    }

    /** Returns the BluetoothDevice address associated with the Companion device.  */
    public String getCompanionAddress() {
        if (mCompanion != null) {
            return mCompanion.getAddress();
        } else {
            return getStringValueForKey(WearBluetoothSettings.KEY_COMPANION_ADDRESS, "");
        }
    }

    /**
     * Returns true iff the currently paired Companion device is an LE or DUAL device.
     *
     * <p>Normally, we should just check the device type of mCompanion. But b/62355127 revealed that
     * BluetoothDevice.getType() can return LE/DUAL even for an Android device (particularly when
     * bonding is unexpectedly lost and re-established). To workaround this, we rely on the
     * Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE setting written by ConnectionSetupHelper in
     * Setup, which during the initial pairing process correctly identifies the device type.
     *
     * <p>BluetoothDevice.getType() is used as a fallback only if for some reason the
     * Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE setting has not been populated.
     */
    public boolean isCompanionBle() {
        if (mCompanion == null) {
            return false;
        }

        if (mCompanionOsType != PAIRED_DEVICE_OS_TYPE_UNKNOWN) {
            return mCompanionOsType == PAIRED_DEVICE_OS_TYPE_IOS;
        }

        boolean deviceIsBle = mCompanion.getType() == BluetoothDevice.DEVICE_TYPE_LE
            || mCompanion.getType() == BluetoothDevice.DEVICE_TYPE_DUAL;

        mCompanionOsType =
                getIntValueForKey(
                        PAIRED_DEVICE_OS_TYPE,
                        PAIRED_DEVICE_OS_TYPE_UNKNOWN);
        if (mCompanionOsType == PAIRED_DEVICE_OS_TYPE_UNKNOWN) {
            Log.e(
                    TAG,
                    "Paired companion device OS type is unknown. "
                            + " Relying on device type instead: " + mCompanion.getType());
            return deviceIsBle;
        }

        boolean legacyModeIsBle = (mCompanionOsType == PAIRED_DEVICE_OS_TYPE_IOS);
        if (legacyModeIsBle != deviceIsBle) {
            Log.w(TAG, "Legacy BT Mode derived from OS type is different from paired device type. "
                    + "Paired device mode: " + mCompanion.getType() + "; "
                    + "OS type: " + mCompanionOsType);
        }
        return legacyModeIsBle;
    }

    /**
     * This needs to be called once per reboot in order to guarantee that CompanionTracker
     * will have the correct Companion Device retrieved from persistent storage and the Bluetooth
     * adapter.
     */
    public void onBluetoothAdapterReady() {
        if (mBtAdapter == null) {
            return;
        }

        String companionAddress =
                getStringValueForKey(WearBluetoothSettings.KEY_COMPANION_ADDRESS, null);
        if (companionAddress != null && !companionAddress.isEmpty()) {
            Log.d(TAG, "Loading companion by address from settings: " + companionAddress);
            updateCompanionDevice(companionAddress);
            return;
        }

        // if we got to here, we're either not paired or we need to migrate from an existing device
        Set<BluetoothDevice> bondedDevices = mBtAdapter.getBondedDevices();
        if (bondedDevices.isEmpty()) {
            // we're not paired, so just bail
            return;
        }

        // retrieve the pairing state indirectly by checking the paired device OS type.
        int pairedDeviceOSType =
                getIntValueForKey(
                        PAIRED_DEVICE_OS_TYPE,
                        PAIRED_DEVICE_OS_TYPE_UNKNOWN);
        Log.d(
                TAG,
                "Migrating legacy Companion address with paired device OS type : "
                        + pairedDeviceOSType);

        if (pairedDeviceOSType == PAIRED_DEVICE_OS_TYPE_IOS) {
            for (BluetoothDevice device : bondedDevices) {
                if (device.getType() == BluetoothDevice.DEVICE_TYPE_LE
                        || device.getType() == BluetoothDevice.DEVICE_TYPE_DUAL) {
                    mCompanion = device;
                    break;
                }
            }
        } else if (pairedDeviceOSType == PAIRED_DEVICE_OS_TYPE_ANDROID) {
            for (BluetoothDevice device : bondedDevices) {
                if (device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
                    final BluetoothClass btClass = device.getBluetoothClass();
                    if (btClass != null && btClass.getMajorDeviceClass()
                            == BluetoothClass.Device.Major.PHONE) {
                        mCompanion = device;
                        break;
                    }
                }
            }
        } else if (pairedDeviceOSType == PAIRED_DEVICE_OS_TYPE_UNKNOWN) {
            // When upgrading from E, add the first android device as companion.
            for (BluetoothDevice device : bondedDevices) {
                if (device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
                    final BluetoothClass btClass = device.getBluetoothClass();
                    if (btClass != null && btClass.getMajorDeviceClass() ==
                            BluetoothClass.Device.Major.PHONE) {
                        mCompanion = device;
                        break;
                    }
                }
            }
            if (mCompanion == null) {
                // add the first LE device as companion if we can't find android device.
                for (BluetoothDevice device : bondedDevices) {
                    if (device.getType() == BluetoothDevice.DEVICE_TYPE_LE ||
                            device.getType() == BluetoothDevice.DEVICE_TYPE_DUAL) {
                        mCompanion = device;
                        break;
                    }
                }
            }
        }

        // we found a legacy Companion pairing. update the database
        if (mCompanion != null) {
            Log.d(TAG, "legacy companion address: " + mCompanion.getAddress());
            WearBluetoothSettings.putString(
                    mContentResolver,
                    WearBluetoothSettings.KEY_COMPANION_ADDRESS,
                    mCompanion.getAddress());
        }
    }

    /**
     * A bluetooth device has just bonded.
     *
     * Check if the newly bonded device is our companion and notify
     * if we don't already have a companion initialized.
     */
    void receivedBondedAction(@NonNull final BluetoothDevice device) {
        final String companionAddress =
                getStringValueForKey(WearBluetoothSettings.KEY_COMPANION_ADDRESS, null);

        if (mCompanion == null && device.getAddress().equals(companionAddress)) {
            int osType =
                    getIntValueForKey(
                            PAIRED_DEVICE_OS_TYPE,
                            PAIRED_DEVICE_OS_TYPE_UNKNOWN);
            notifyIfCompanionChanged(device.getAddress(), osType);
        }
    }

    /**
     * Listens for changes to the Bluetooth Settings and updates our pointer to the
     * currently-paired Companion device if necessary.
     */
    final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange, Uri uri) {
            String newCompanionAddress =
                    getStringValueForKey(WearBluetoothSettings.KEY_COMPANION_ADDRESS, null);
            int osType = getIntValueForKey(
                                PAIRED_DEVICE_OS_TYPE,
                                PAIRED_DEVICE_OS_TYPE_UNKNOWN);
            Log.d(TAG, "Companion address Settings update: " + newCompanionAddress
                    + "; type: " + osType);
            notifyIfCompanionChanged(newCompanionAddress, osType);
        }
    }

    private void notifyIfCompanionChanged(
            @Nullable final String newCompanionAddress,
            int newOsType) {
        if (newCompanionAddress != null && newOsType != PAIRED_DEVICE_OS_TYPE_UNKNOWN) {
            if (updateCompanionDevice(newCompanionAddress) || newOsType != mCompanionOsType) {
                mCompanionOsType = newOsType;
                for (Listener listener : mListeners) {
                    listener.onCompanionChanged();
                }
            }
        }
    }

    /**
     * Returns true if the specified bluetooth device address matches a
     * currently bonded device.  If they match the companion
     * device address is updated to point to the specified bluetooth device.
     *
     * @param newDeviceAddr specified bluetooth device address.
     * @return
     */
    private boolean updateCompanionDevice(final String newDeviceAddr) {
        if (mBtAdapter == null) {
            return false;
        }

        if (mCompanion != null && mCompanion.getAddress().equals(newDeviceAddr)) {
            return false;
        }

        boolean updated = false;
        for (BluetoothDevice device : mBtAdapter.getBondedDevices()) {
            if (device.getAddress().equals(newDeviceAddr)) {
                mCompanion = device;
                updated = true;
            }
        }
        return updated;
    }

    private String getStringValueForKey(String key, String defaultValue) {
        String val = WearBluetoothSettings.getString(mContentResolver, key);
        if (val == null) {
            return defaultValue;
        } else {
            return val;
        }
    }

    private int getIntValueForKey(String key, int defaultValue) {
        return WearBluetoothSettings.getInt(mContentResolver, key, defaultValue);
    }

    /** Returns true if device's list of cached UUID's contains the given uuid. */
    static boolean isUuidPresent(BluetoothDevice device, ParcelUuid uuid) {
        ParcelUuid[] uuidArray = device.getUuids();
        if ((uuidArray == null || uuidArray.length == 0) && uuid == null) {
            return true;
        }
        if (uuidArray == null) {
            return false;
        }
        for (ParcelUuid element : uuidArray) {
            if (element.equals(uuid)) {
                return true;
            }
        }
        return false;
    }
}
