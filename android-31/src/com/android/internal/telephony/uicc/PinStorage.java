/*
 * Copyright 2020 The Android Open Source Project
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

package com.android.internal.telephony.uicc;

import static android.security.keystore.KeyProperties.AUTH_DEVICE_CREDENTIAL;
import static android.security.keystore.KeyProperties.BLOCK_MODE_GCM;
import static android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE;
import static android.security.keystore.KeyProperties.KEY_ALGORITHM_AES;
import static android.security.keystore.KeyProperties.PURPOSE_DECRYPT;
import static android.security.keystore.KeyProperties.PURPOSE_ENCRYPT;

import static com.android.internal.telephony.TelephonyStatsLog.PIN_STORAGE_EVENT;
import static com.android.internal.telephony.TelephonyStatsLog.PIN_STORAGE_EVENT__EVENT__CACHED_PIN_DISCARDED;
import static com.android.internal.telephony.TelephonyStatsLog.PIN_STORAGE_EVENT__EVENT__PIN_COUNT_NOT_MATCHING_AFTER_REBOOT;
import static com.android.internal.telephony.TelephonyStatsLog.PIN_STORAGE_EVENT__EVENT__PIN_DECRYPTION_ERROR;
import static com.android.internal.telephony.TelephonyStatsLog.PIN_STORAGE_EVENT__EVENT__PIN_ENCRYPTION_ERROR;
import static com.android.internal.telephony.TelephonyStatsLog.PIN_STORAGE_EVENT__EVENT__PIN_REQUIRED_AFTER_REBOOT;
import static com.android.internal.telephony.TelephonyStatsLog.PIN_STORAGE_EVENT__EVENT__PIN_STORED_FOR_VERIFICATION;
import static com.android.internal.telephony.TelephonyStatsLog.PIN_STORAGE_EVENT__EVENT__PIN_VERIFICATION_FAILURE;
import static com.android.internal.telephony.TelephonyStatsLog.PIN_STORAGE_EVENT__EVENT__PIN_VERIFICATION_SKIPPED_SIM_CARD_MISMATCH;
import static com.android.internal.telephony.TelephonyStatsLog.PIN_STORAGE_EVENT__EVENT__PIN_VERIFICATION_SUCCESS;
import static com.android.internal.telephony.uicc.IccCardStatus.PinState.PINSTATE_ENABLED_NOT_VERIFIED;
import static com.android.internal.telephony.uicc.IccCardStatus.PinState.PINSTATE_ENABLED_VERIFIED;

import android.annotation.Nullable;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.security.keystore.KeyGenParameterSpec;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.SimState;
import android.util.Base64;
import android.util.SparseArray;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionInfoUpdater;
import com.android.internal.telephony.TelephonyStatsLog;
import com.android.internal.telephony.nano.StoredPinProto.EncryptedPin;
import com.android.internal.telephony.nano.StoredPinProto.StoredPin;
import com.android.internal.telephony.nano.StoredPinProto.StoredPin.PinStatus;
import com.android.internal.telephony.uicc.IccCardStatus.PinState;
import com.android.internal.util.ArrayUtils;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * This class stores the SIM PIN for automatic verification after an unattended reboot.
 */
public class PinStorage extends Handler {
    private static final String TAG = "PinStorage";
    private static final boolean VDBG = false;  // STOPSHIP if true

    /**
     * Time duration in milliseconds to allow automatic PIN verification after reboot. All unused
     * PINs are discarded when the timer expires.
     */
    private static final int TIMER_VALUE_AFTER_OTA_MILLIS = 20_000;

    /**
     * Time duration in milliseconds to reboot the device after {@code prepareUnattendedReboot}
     * is invoked. After the time expires, a new invocation of {@code prepareUnattendedReboot} is
     * required to perform the automatic PIN verification after reboot.
     */
    private static final int TIMER_VALUE_BEFORE_OTA_MILLIS = 20_000;

    /** Minimum valid length of the ICCID. */
    private static final int MIN_ICCID_LENGTH = 12;
    /** Minimum length of the SIM PIN, as per 3GPP TS 31.101. */
    private static final int MIN_PIN_LENGTH = 4;
    /** Maximum length of the SIM PIN, as per 3GPP TS 31.101. */
    private static final int MAX_PIN_LENGTH = 8;

    // Variables related to the encryption of the SIM PIN.
    private static final String ANDROID_KEY_STORE_PROVIDER = "AndroidKeyStore";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_PARAMETER_TAG_BIT_LEN = 128;
    private static final int SHORT_TERM_KEY_DURATION_MINUTES = 15;

    /** Alias of the long-term key that does not require user authentication. */
    private static final String KEYSTORE_ALIAS_LONG_TERM_ALWAYS = "PinStorage_longTerm_always_key";
    /** Alias of the user authentication blound long-term key. */
    private static final String KEYSTORE_ALIAS_LONG_TERM_USER_AUTH = "PinStorage_longTerm_ua_key";
    /** Alias of the short-term key (30 minutes) used before and after an unattended reboot. */
    private static final String KEYSTORE_ALIAS_SHORT_TERM = "PinStorage_shortTerm_key";

    // Constants related to the storage of the encrypted SIM PIN to non-volatile memory.
    // Data is stored in two separate files:
    //  - "available" is for the PIN(s) in AVAILABLE state and uses a key that does not expire
    //  - "reboot" is for the PIN(s) in other states and uses a short-term key (30 minutes)
    private static final String SHARED_PREFS_NAME = "pinstorage_prefs";
    private static final String SHARED_PREFS_AVAILABLE_PIN_BASE_KEY = "encrypted_pin_available_";
    private static final String SHARED_PREFS_REBOOT_PIN_BASE_KEY = "encrypted_pin_reboot_";
    private static final String SHARED_PREFS_STORED_PINS = "stored_pins";

    // Events
    private static final int ICC_CHANGED_EVENT = 1;
    private static final int CARRIER_CONFIG_CHANGED_EVENT = 2;
    private static final int TIMER_EXPIRATION_EVENT = 3;
    private static final int USER_UNLOCKED_EVENT = 4;
    private static final int SUPPLY_PIN_COMPLETE = 5;

    private final Context mContext;
    private final int mBootCount;
    private final KeyStore mKeyStore;

    private SecretKey mLongTermSecretKey;
    private SecretKey mShortTermSecretKey;

    private boolean mIsDeviceSecure;
    private boolean mIsDeviceLocked;
    private boolean mLastCommitResult = true;

    /** Duration of the short-term key, in minutes. */
    @VisibleForTesting
    public int mShortTermSecretKeyDurationMinutes;

    /** RAM storage is used on secure devices before the device is unlocked. */
    private final SparseArray<byte[]> mRamStorage;

    /** Receiver for the required intents. */
    private final BroadcastReceiver mCarrierConfigChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED.equals(action)) {
                int slotId = intent.getIntExtra(CarrierConfigManager.EXTRA_SLOT_INDEX, -1);
                sendMessage(obtainMessage(CARRIER_CONFIG_CHANGED_EVENT, slotId, 0));
            } else if (TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED.equals(action)
                    || TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED.equals(action)) {
                int slotId = intent.getIntExtra(PhoneConstants.PHONE_KEY, -1);
                int state = intent.getIntExtra(
                        TelephonyManager.EXTRA_SIM_STATE, TelephonyManager.SIM_STATE_UNKNOWN);
                if (validateSlotId(slotId)) {
                    sendMessage(obtainMessage(ICC_CHANGED_EVENT, slotId, state));
                }
            } else if (Intent.ACTION_USER_UNLOCKED.equals(action)) {
                sendMessage(obtainMessage(USER_UNLOCKED_EVENT));
            }
        }
    };

    public PinStorage(Context context) {
        mContext = context;
        mBootCount = getBootCount();
        mKeyStore = initializeKeyStore();
        mShortTermSecretKeyDurationMinutes = SHORT_TERM_KEY_DURATION_MINUTES;

        mIsDeviceSecure = isDeviceSecure();
        mIsDeviceLocked = mIsDeviceSecure ? isDeviceLocked() : false;

        // Register for necessary intents.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        intentFilter.addAction(TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED);
        intentFilter.addAction(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED);
        intentFilter.addAction(Intent.ACTION_USER_UNLOCKED);
        mContext.registerReceiver(mCarrierConfigChangedReceiver, intentFilter);

        // Initialize the long term secret key. This needs to be present in all cases:
        //  - if the device is not secure or is locked: key does not require user authentication
        //  - if the device is secure and unlocked: key requires user authentication.
        // The short term key is retrieved later when needed.
        String alias = (!mIsDeviceSecure || mIsDeviceLocked)
                ? KEYSTORE_ALIAS_LONG_TERM_ALWAYS : KEYSTORE_ALIAS_LONG_TERM_USER_AUTH;
        mLongTermSecretKey = initializeSecretKey(alias, /*createIfAbsent=*/ true);

        // If the device is not securee or is unlocked, we can start logic. Otherwise we need to
        // wait for the device to be unlocked and store any temporary PIN in RAM.
        if (!mIsDeviceSecure || !mIsDeviceLocked) {
            mRamStorage = null;
            onDeviceReady();
        } else {
            logd("Device is locked - Postponing initialization");
            mRamStorage = new SparseArray<>();
        }
    }

    /** Store the {@code pin} for the {@code slotId}. */
    public synchronized void storePin(String pin, int slotId) {
        String iccid = getIccid(slotId);

        if (!validatePin(pin) || !validateIccid(iccid) || !validateSlotId(slotId)) {
            // We are unable to store the PIN. At least clear the old one, if present.
            loge("storePin[%d] - Invalid PIN, slotId or ICCID", slotId);
            clearPin(slotId);
            return;
        }
        if (!isCacheAllowed(slotId)) {
            logd("storePin[%d]: caching it not allowed", slotId);
            return;
        }

        logd("storePin[%d]", slotId);

        StoredPin storedPin = new StoredPin();
        storedPin.iccid = iccid;
        storedPin.pin = pin;
        storedPin.slotId = slotId;
        storedPin.status = PinStatus.AVAILABLE;

        savePinInformation(slotId, storedPin);
    }

    /** Clear the cached pin for the {@code slotId}. */
    public synchronized void clearPin(int slotId) {
        logd("clearPin[%d]", slotId);

        if (!validateSlotId(slotId)) {
            return;
        }
        savePinInformation(slotId, null);
    }

    /**
     * Return the cached pin for the SIM card identified by {@code slotId} and {@code iccid}, or
     * an empty string if it is not available.
     *
     * The method returns the PIN only if the state is VERIFICATION_READY. If the PIN is found,
     * its state changes to AVAILABLE, so that it cannot be retrieved a second time during the
     * same boot cycle. If the PIN verification fails, it will be removed after the failed attempt.
     */
    public synchronized String getPin(int slotId, String iccid) {
        if (!validateSlotId(slotId) || !validateIccid(iccid)) {
            return "";
        }

        StoredPin storedPin = loadPinInformation(slotId);
        if (storedPin != null) {
            if (!storedPin.iccid.equals(iccid)) {
                // The ICCID does not match: it's possible that the SIM card was changed.
                // Delete the cached PIN.
                savePinInformation(slotId, null);
                TelephonyStatsLog.write(PIN_STORAGE_EVENT,
                        PIN_STORAGE_EVENT__EVENT__PIN_VERIFICATION_SKIPPED_SIM_CARD_MISMATCH,
                        /* number_of_pins= */ 1);
            } else if (storedPin.status == PinStatus.VERIFICATION_READY) {
                logd("getPin[%d] - Found PIN ready for verification", slotId);
                // Move the state to AVAILABLE, so that it cannot be retrieved again.
                storedPin.status = PinStatus.AVAILABLE;
                savePinInformation(slotId, storedPin);
                return storedPin.pin;
            }
        }
        return "";
    }

    /**
     * Prepare for an unattended reboot.
     *
     * All PINs in AVAILABLE and VERIFICATION_READY state are moved to REBOOT_READY state. A
     * timer is started to make sure that reboot occurs shortly after invoking this method.
     *
     * @return The result of the reboot preparation.
     */
    @TelephonyManager.PrepareUnattendedRebootResult
    public synchronized int prepareUnattendedReboot() {
        // Unattended reboot should never occur before the device is unlocked.
        if (mIsDeviceLocked) {
            loge("prepareUnattendedReboot - Device is locked");
            return TelephonyManager.PREPARE_UNATTENDED_REBOOT_ERROR;
        }

        // Start timer to make sure that device is rebooted shortly after this is executed.
        if (!startTimer(TIMER_VALUE_BEFORE_OTA_MILLIS)) {
            return TelephonyManager.PREPARE_UNATTENDED_REBOOT_ERROR;
        }

        int numSlots = getSlotCount();
        SparseArray<StoredPin> storedPins = loadPinInformation();

        // Delete any previous short-term key, if present: a new one is created (if needed).
        deleteSecretKey(KEYSTORE_ALIAS_SHORT_TERM);
        mShortTermSecretKey = null;

        // If any PIN is present, generate a new short-term key to save PIN(s) to
        // non-volatile memory.
        if (storedPins.size() > 0) {
            mShortTermSecretKey =
                    initializeSecretKey(KEYSTORE_ALIAS_SHORT_TERM, /*createIfAbsent=*/ true);
        }

        @TelephonyManager.PrepareUnattendedRebootResult
        int result =  TelephonyManager.PREPARE_UNATTENDED_REBOOT_SUCCESS;
        int storedCount = 0;
        int notAvailableCount = 0;

        for (int slotId = 0; slotId < numSlots; slotId++) {
            StoredPin storedPin = storedPins.get(slotId);
            if (storedPin != null) {
                storedPin.status = PinStatus.REBOOT_READY;
                if (!savePinInformation(slotId, storedPin)) {
                    result = TelephonyManager.PREPARE_UNATTENDED_REBOOT_ERROR;
                    break;
                }
                storedCount++;
            } else if (isPinState(slotId, PINSTATE_ENABLED_VERIFIED)) {
                // If PIN is not available, check if PIN will be required after reboot (current PIN
                // status is enabled and verified).
                loge("Slot %d requires PIN and is not cached", slotId);
                result = TelephonyManager.PREPARE_UNATTENDED_REBOOT_PIN_REQUIRED;
                notAvailableCount++;
            }
        }

        // Generate metrics
        if (result == TelephonyManager.PREPARE_UNATTENDED_REBOOT_SUCCESS) {
            logd("prepareUnattendedReboot - Stored %d PINs", storedCount);
            TelephonyStatsLog.write(PIN_STORAGE_EVENT,
                    PIN_STORAGE_EVENT__EVENT__PIN_STORED_FOR_VERIFICATION, storedCount);
        } else if (result == TelephonyManager.PREPARE_UNATTENDED_REBOOT_PIN_REQUIRED) {
            logd("prepareUnattendedReboot - Required %d PINs after reboot", notAvailableCount);
            TelephonyStatsLog.write(PIN_STORAGE_EVENT,
                    PIN_STORAGE_EVENT__EVENT__PIN_REQUIRED_AFTER_REBOOT, notAvailableCount);
        }

        // Save number of PINs to generate metrics after reboot
        saveNumberOfCachedPins(storedCount);

        return result;
    }

    /**
     * Execute logic when a secure device is unlocked.
     *
     * The temporary long-term key that does not require user verification is replaced by the long
     * term key that requires user verification. The cached PIN temporarily stored in RAM are
     * merged with those on disk from the previous boot.
     */
    private synchronized void onUserUnlocked() {
        if (!mIsDeviceLocked) {
            // This should never happen.
            // Nothing to do because the device was already unlocked before
            return;
        }

        logd("onUserUnlocked - Device is unlocked");

        // It's possible that SIM PIN was already verified and stored temporarily in RAM. Load the
        // data and erase the memory.
        SparseArray<StoredPin> storedPinInRam = loadPinInformation();
        cleanRamStorage();

        // Mark the device as unlocked
        mIsDeviceLocked = false;

        // Replace the temporary long-term key without user authentication with a new long-term
        // key that requires user authentication to save all PINs previously in RAM (all in
        // AVAILABLE state) to disk.
        mLongTermSecretKey =
                initializeSecretKey(KEYSTORE_ALIAS_LONG_TERM_USER_AUTH, /*createIfAbsent=*/ true);

        // Save the PINs previously in RAM to disk, overwriting any PIN that might already exists.
        for (int i = 0; i < storedPinInRam.size(); i++) {
            savePinInformation(storedPinInRam.keyAt(i), storedPinInRam.valueAt(i));
        }

        // At this point the module is fully initialized. Execute the start logic.
        onDeviceReady();

        // Verify any pending PIN for SIM cards that need it.
        verifyPendingPins();
    }

    /**
     * Executes logic when module is fully ready. This occurs immediately if the device is not
     * secure or after the user unlocks the device.
     *
     * At this point, the short-term key is initialized (if present), the configuration is read
     * and the status of each PIN is updated as needed.
     */
    private void onDeviceReady() {
        logd("onDeviceReady");

        // Try to initialize the short term key, if present, as this would be required to read
        // stored PIN for verification.
        mShortTermSecretKey =
                initializeSecretKey(KEYSTORE_ALIAS_SHORT_TERM, /*createIfAbsent=*/ false);

        int verificationReadyCount = 0;
        int slotCount = getSlotCount();
        for (int slotId = 0; slotId < slotCount; slotId++) {
            // Read PIN information from storage
            StoredPin storedPin = loadPinInformation(slotId);
            if (storedPin == null) {
                continue;
            }

            // For each PIN in AVAILABLE state, check the boot count.
            // If the boot count matches, it means that module crashed and it's ok to preserve
            // the PIN code. If the boot count does not match, then delete those PINs.
            if (storedPin.status == PinStatus.AVAILABLE) {
                if (storedPin.bootCount != mBootCount) {
                    logd("Boot count [%d] does not match - remove PIN", slotId);
                    savePinInformation(slotId, null);
                    continue;
                }
                logd("Boot count [%d] matches - keep stored PIN", slotId);
            }

            // If there is any PIN in REBOOT_READY state, move it to VERIFICATION_READY and start
            // the timer. Don't change PINs that might be already in VERIFICATION_READY state
            // (e.g. due to crash).
            if (storedPin.status == PinStatus.REBOOT_READY) {
                storedPin.status = PinStatus.VERIFICATION_READY;
                savePinInformation(slotId, storedPin);
                verificationReadyCount++;
            }
        }
        if (verificationReadyCount > 0) {
            startTimer(TIMER_VALUE_AFTER_OTA_MILLIS);
        }

        // Generate metrics for PINs that had been stored before reboot, but are not available
        // after. This can happen if there is an excessive delay in unlocking the device (short
        // term key expires), but also if a new SIM card without PIN is present.
        int prevCachedPinCount = saveNumberOfCachedPins(0);
        if (prevCachedPinCount > verificationReadyCount) {
            TelephonyStatsLog.write(PIN_STORAGE_EVENT,
                    PIN_STORAGE_EVENT__EVENT__PIN_COUNT_NOT_MATCHING_AFTER_REBOOT,
                    prevCachedPinCount - verificationReadyCount);
        }
    }

    /**
     * Executes logic at the expiration of the timer. This method is common for two cases:
     *  - timer started after unattended reeboot to verify the SIM PIN automatically
     *  - timer started after prepareUnattendedReboot() is invoked.
     */
    private synchronized void onTimerExpiration() {
        logd("onTimerExpiration");

        int discardedPin = 0;
        int slotCount = getSlotCount();
        for (int slotId = 0; slotId < slotCount; slotId++) {
            // Read PIN information from storage
            StoredPin storedPin = loadPinInformation(slotId);
            if (storedPin == null) {
                continue;
            }

            // Delete all PINs in VERIFICATION_READY state. This happens when reboot occurred after
            // OTA, but the SIM card is not detected on the device.
            if (storedPin.status == PinStatus.VERIFICATION_READY) {
                logd("onTimerExpiration - Discarding PIN in slot %d", slotId);
                savePinInformation(slotId, null);
                discardedPin++;
                continue;
            }

            // Move all PINs in REBOOT_READY to AVAILABLE. This happens when
            // prepareUnattendedReboot() is invoked, but the reboot does not occur.
            if (storedPin.status == PinStatus.REBOOT_READY) {
                logd("onTimerExpiration - Moving PIN in slot %d back to AVAILABLE", slotId);
                storedPin.status = PinStatus.AVAILABLE;
                savePinInformation(slotId, storedPin);
                continue;
            }
        }

        // Delete short term key no matter the reason of the timer expiration.
        // This is done after loading the PIN information, so that it's possible to change
        // the status of the PIN as needed.
        deleteSecretKey(KEYSTORE_ALIAS_SHORT_TERM);
        mShortTermSecretKey = null;

        // Reset number of stored PINs (applicable if timer expired before unattended reboot).
        saveNumberOfCachedPins(0);

        // Write metrics about number of discarded PINs
        if (discardedPin > 0) {
            TelephonyStatsLog.write(PIN_STORAGE_EVENT,
                    PIN_STORAGE_EVENT__EVENT__CACHED_PIN_DISCARDED, discardedPin);
        }
    }

    /** Handle the update of the {@code state} of the SIM card in {@code slotId}. */
    private synchronized void onSimStatusChange(int slotId, @SimState int state) {
        logd("SIM card/application changed[%d]: %s",
                slotId, SubscriptionInfoUpdater.simStateString(state));
        switch (state) {
            case TelephonyManager.SIM_STATE_ABSENT:
            case TelephonyManager.SIM_STATE_PIN_REQUIRED: {
                // These states are likely to occur after a reboot, so we don't clear cached PINs
                // in VERIFICATION_READY state, as they might be verified later, when the SIM is
                // detected. On the other hand, we remove PINs in AVAILABLE state.
                StoredPin storedPin = loadPinInformation(slotId);
                if (storedPin != null && storedPin.status != PinStatus.VERIFICATION_READY) {
                    savePinInformation(slotId, null);
                }
                break;
            }
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
            case TelephonyManager.SIM_STATE_PERM_DISABLED:
            case TelephonyManager.SIM_STATE_CARD_IO_ERROR:
                // These states indicate that the SIM card will need a manual PIN verification.
                // Delete the cached PIN regardless of its state.
                clearPin(slotId);
                break;
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
            case TelephonyManager.SIM_STATE_CARD_RESTRICTED:
            case TelephonyManager.SIM_STATE_LOADED:
            case TelephonyManager.SIM_STATE_READY: {
                // These states can occur after successful PIN caching, so we don't clear cached
                // PINs in AVAILABLE state, as they need to be retained. We clear any PIN in
                // other states, as they are no longer needed for automatic verification.
                StoredPin storedPin = loadPinInformation(slotId);
                if (storedPin != null && storedPin.status != PinStatus.AVAILABLE) {
                    savePinInformation(slotId, null);
                }
                break;
            }

            case TelephonyManager.SIM_STATE_NOT_READY:
            case TelephonyManager.SIM_STATE_PRESENT:
            default:
                break;
        }
    }

    private void onCarrierConfigChanged(int slotId) {
        logv("onCarrierConfigChanged[%d]", slotId);
        if (!isCacheAllowed(slotId)) {
            logd("onCarrierConfigChanged[%d] - PIN caching not allowed", slotId);
            clearPin(slotId);
        }
    }

    private void onSupplyPinComplete(int slotId, boolean success) {
        logd("onSupplyPinComplete[%d] - success: %s", slotId, success);
        if (!success) {
            // In case of failure to verify the PIN, delete the stored value.
            // Otherwise nothing to do.
            clearPin(slotId);
        }
        // Update metrics:
        TelephonyStatsLog.write(
                PIN_STORAGE_EVENT,
                success
                    ? PIN_STORAGE_EVENT__EVENT__PIN_VERIFICATION_SUCCESS
                    : PIN_STORAGE_EVENT__EVENT__PIN_VERIFICATION_FAILURE,
                /* number_of_pins= */ 1);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case ICC_CHANGED_EVENT:
                onSimStatusChange(/* slotId= */ msg.arg1, /* state= */ msg.arg2);
                break;
            case CARRIER_CONFIG_CHANGED_EVENT:
                onCarrierConfigChanged(/* slotId= */ msg.arg1);
                break;
            case TIMER_EXPIRATION_EVENT:
                onTimerExpiration();
                break;
            case USER_UNLOCKED_EVENT:
                onUserUnlocked();
                break;
            case SUPPLY_PIN_COMPLETE:
                AsyncResult ar = (AsyncResult) msg.obj;
                boolean success = ar != null && ar.exception == null;
                onSupplyPinComplete(/* slotId= */ msg.arg2, success);
                break;
            default:
                // Nothing to do
                break;
        }
    }

    /** Return if the device is secure (device PIN is enabled). */
    private boolean isDeviceSecure() {
        KeyguardManager keyguardManager = mContext.getSystemService(KeyguardManager.class);
        return keyguardManager != null ? keyguardManager.isDeviceSecure() : false;
    }

    /** Return if the device is locked (device PIN is enabled and not verified). */
    private boolean isDeviceLocked() {
        KeyguardManager keyguardManager = mContext.getSystemService(KeyguardManager.class);
        return keyguardManager != null
                ? keyguardManager.isDeviceSecure() && keyguardManager.isDeviceLocked()
                : false;
    }

    /** Loads the stored PIN informations for all SIM slots. */
    private SparseArray<StoredPin> loadPinInformation() {
        SparseArray<StoredPin> result = new SparseArray<>();
        int slotCount = getSlotCount();
        for (int slotId = 0; slotId < slotCount; slotId++) {
            StoredPin storedPin = loadPinInformation(slotId);
            if (storedPin != null) {
                result.put(slotId, storedPin);
            }
        }
        return result;
    }

    /**
     * Loads the stored PIN information for the {@code slotId}.
     *
     * The RAM storage is used if the device is locked, the disk storage is used otherwise.
     * This method tries to use both the long-term key and the short-term key (if available)
     * to retrieve the PIN information, regardless of its status.
     *
     * @return the stored {@code StoredPin}, or null if not present.
     */
    @Nullable
    private StoredPin loadPinInformation(int slotId) {
        if (!mLastCommitResult) {
            // If the last commit failed, do not read from file, as we might retrieve stale data.
            loge("Last commit failed - returning empty values");
            return null;
        }

        StoredPin result = null;

        if (mIsDeviceLocked) {
            // If the device is still locked, retrieve data from RAM storage.
            if (mRamStorage != null && mRamStorage.get(slotId) != null) {
                result =  decryptStoredPin(mRamStorage.get(slotId), mLongTermSecretKey);
            }
        } else {
            // Load both the stored PIN in available state (with long-term key) and in other states
            // (with short-term key). At most one of them should be present at any given time and
            // we treat the case wheere both are present as an error.
            StoredPin availableStoredPin = loadPinInformationFromDisk(
                    slotId, SHARED_PREFS_AVAILABLE_PIN_BASE_KEY, mLongTermSecretKey);
            StoredPin rebootStoredPin = loadPinInformationFromDisk(
                    slotId, SHARED_PREFS_REBOOT_PIN_BASE_KEY, mShortTermSecretKey);
            if (availableStoredPin != null && rebootStoredPin == null) {
                result = availableStoredPin;
            } else if (availableStoredPin == null && rebootStoredPin != null) {
                result = rebootStoredPin;
            }
        }

        // Validate the slot ID of the retrieved PIN information
        if (result != null && result.slotId != slotId) {
            loge("Load PIN: slot ID does not match (%d != %d)", result.slotId, slotId);
            result = null;
        }

        if (result != null) {
            logv("Load PIN: %s", result.toString());
        } else {
            logv("Load PIN for slot %d: null", slotId);
        }
        return result;
    }

    /**
     * Load the PIN information from a specific file in non-volatile memory.
     *
     * @param key the key in the {@code SharedPreferences} to read
     * @param secretKey the key used for encryption/decryption
     * @return the {@code StoredPin} from non-volatile memory. It returns a default instance in
     * case of error.
     */
    @Nullable
    private StoredPin loadPinInformationFromDisk(
            int slotId, String key, @Nullable SecretKey secretKey) {
        String base64encryptedPin =
                mContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
                    .getString(key + slotId, "");
        if (!base64encryptedPin.isEmpty()) {
            try {
                byte[] blob = Base64.decode(base64encryptedPin, Base64.DEFAULT);
                return decryptStoredPin(blob, secretKey);
            } catch (Exception e) {
                // Nothing to do
            }
        }
        return null;
    }

    /** Load the PIN information from an encrypted binary blob.
     *
     * @param blob the encrypted binary blob
     * @param secretKey the key used for encryption/decryption
     * @return the decrypted {@code StoredPin}, or null in case of error.
     */
    @Nullable
    private StoredPin decryptStoredPin(byte[] blob, @Nullable SecretKey secretKey) {
        if (secretKey != null) {
            try {
                byte[] decryptedPin = decrypt(secretKey, blob);
                if (decryptedPin.length > 0) {
                    return StoredPin.parseFrom(decryptedPin);
                }
            } catch (Exception e) {
                loge("cannot decrypt/parse PIN information", e);
            }
        }
        return null;
    }

    /**
     * Stores the PIN information.
     *
     * If the device is locked, the PIN information is stored to RAM, othewrwise to disk.
     * The PIN information is divided based on the PIN status and stored in two separate
     * files in non-volatile memory, each encrypted with a different key.
     *
     * @param slotId the slot ID
     * @param storedPin the PIN information to be stored
     * @return true if the operation was successfully done, false otherwise.
     */
    private boolean savePinInformation(int slotId, @Nullable StoredPin storedPin) {
        // Populate the boot count
        if (storedPin != null) {
            storedPin.bootCount = mBootCount;
        }

        // If the device is still locked, we can only save PINs in AVAILABLE state in RAM.
        // NOTE: at this point, there should not be any PIN in any other state.
        if (mIsDeviceLocked) {
            return savePinInformationToRam(slotId, storedPin);
        }

        // Remove any prvious key related to this slot.
        SharedPreferences.Editor editor =
                mContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .remove(SHARED_PREFS_AVAILABLE_PIN_BASE_KEY + slotId)
                    .remove(SHARED_PREFS_REBOOT_PIN_BASE_KEY + slotId);

        boolean result = true;
        if (storedPin != null) {
            // Available PINs are stored with a long-term key, while the PINs in other states
            // are stored with a short-term key.
            logd("Saving PIN for slot %d", slotId);
            if (storedPin.status == PinStatus.AVAILABLE) {
                result = savePinInformation(editor, slotId, storedPin,
                        SHARED_PREFS_AVAILABLE_PIN_BASE_KEY, mLongTermSecretKey);
            } else {
                result = savePinInformation(editor, slotId, storedPin,
                        SHARED_PREFS_REBOOT_PIN_BASE_KEY, mShortTermSecretKey);
            }
        } else {
            logv("Deleting PIN for slot %d (if existed)", slotId);
        }

        mLastCommitResult = editor.commit() && result;
        return mLastCommitResult;
    }

    /**
     * Store the PIN information to a specific file in non-volatile memory.
     *
     * @param editor the {@code SharedPreferences.Editor} to use for storage
     * @param slotId the slot ID
     * @param storedPin the PIN information to store
     * @param baseKey the base name of the key in the {@code SharedPreferences}. The full name is
     *        derived appending the value of {@code slotId}.
     * @param secretKey the key used for encryption/decryption
     * @return true if the operation was successful, false otherwise
     */
    private boolean savePinInformation(SharedPreferences.Editor editor, int slotId,
            StoredPin storedPin, String baseKey, SecretKey secretKey) {
        if (secretKey == null) {
            // Secret key for encryption is missing
            return false;
        }
        if (slotId != storedPin.slotId) {
            loge("Save PIN: the slotId does not match (%d != %d)", slotId, storedPin.slotId);
            return false;
        }

        logv("Save PIN: %s", storedPin.toString());

        byte[] encryptedPin = encrypt(secretKey, StoredPin.toByteArray(storedPin));
        if (encryptedPin.length > 0) {
            editor.putString(
                    baseKey + slotId, Base64.encodeToString(encryptedPin, Base64.DEFAULT));
            return true;
        } else {
            return false;
        }
    }

    /** Stored PIN information for slot {@code slotId} in RAM. */
    private boolean savePinInformationToRam(int slotId, @Nullable StoredPin storedPin) {
        // Clear the RAM in all cases, to avoid leaking any previous PIN.
        cleanRamStorage(slotId);

        if (storedPin == null) {
            return true;
        }

        if (storedPin.status == PinStatus.AVAILABLE) {
            byte[] encryptedPin = encrypt(mLongTermSecretKey, StoredPin.toByteArray(storedPin));
            if (encryptedPin != null && encryptedPin.length > 0) {
                logd("Saving PIN for slot %d in RAM", slotId);
                mRamStorage.put(slotId, encryptedPin);
                return true;
            }
        }
        return false;
    }


    /** Erases all the PINs stored in RAM before a secure device is unlocked. */
    private void cleanRamStorage() {
        int slotCount = getSlotCount();
        for (int slotId = 0; slotId < slotCount; slotId++) {
            cleanRamStorage(slotId);
        }
    }

    /** Erases the PIN of slot {@code slotId} stored in RAM before a secure device is unlocked. */
    private void cleanRamStorage(int slotId) {
        if (mRamStorage != null) {
            byte[] data = mRamStorage.get(slotId);
            if (data != null) {
                Arrays.fill(data, (byte) 0);
            }
            mRamStorage.delete(slotId);
        }
    }

    /**
     * Verifies all pending PIN codes that are ready for verification.
     *
     * The PIN verificartion is done if the PIN state is VERIFICATION_READY and the SIM
     * card has the PIN enabled and not verified.
     */
    private void verifyPendingPins() {
        int slotCount = getSlotCount();
        for (int slotId = 0; slotId < slotCount; slotId++) {
            if (isPinState(slotId, PINSTATE_ENABLED_NOT_VERIFIED)) {
                verifyPendingPin(slotId);
            }
        }
    }

    /** Verifies the PIN code for a given SIM card in slot {@code slotId}. */
    private void verifyPendingPin(int slotId) {
        // We intentionally invoke getPin() here, as it updates the status and makes sure that
        // same PIN is not used more than once
        String pin = getPin(slotId, getIccid(slotId));
        if (pin.isEmpty()) {
            // PIN is not available for verification: return.
            return;
        }

        logd("Perform automatic verification of PIN in slot %d", slotId);

        UiccProfile profile = UiccController.getInstance().getUiccProfileForPhone(slotId);
        if (profile != null) {
            Message onComplete = obtainMessage(SUPPLY_PIN_COMPLETE);
            onComplete.arg2 = slotId;  // arg1 is the number of remaining attempts in the response
            profile.supplyPin(pin, onComplete);
        } else {
            logd("Perform automatic verification of PIN in slot %d not possible", slotId);
        }
    }

    /** Returns the boot count. */
    private int getBootCount() {
        return Settings.Global.getInt(
                mContext.getContentResolver(),
                Settings.Global.BOOT_COUNT,
                -1);
    }

    /** Returns the number of available SIM slots. */
    private int getSlotCount() {
        // Count the number of slots as the number of Phones.
        // At power up, it is possible that number of phones is still unknown, so we query
        // TelephonyManager for it.
        try {
            return PhoneFactory.getPhones().length;
        } catch (Exception ex) {
            return TelephonyManager.getDefault().getActiveModemCount();
        }
    }

    /**
     * Saves the number of cached PINs ready for verification after reboot and returns the
     * previous value.
     */
    private int saveNumberOfCachedPins(int storedCount) {
        SharedPreferences sharedPrefs =
                mContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        int previousValue = sharedPrefs.getInt(SHARED_PREFS_STORED_PINS, 0);
        sharedPrefs.edit().putInt(SHARED_PREFS_STORED_PINS, storedCount).commit();
        return previousValue;
    }

    private boolean startTimer(int duration) {
        removeMessages(TIMER_EXPIRATION_EVENT);
        return duration > 0 ? sendEmptyMessageDelayed(TIMER_EXPIRATION_EVENT, duration) : true;
    }

    /** Returns the ICCID of the SIM card for the given {@code slotId}. */
    private String getIccid(int slotId) {
        Phone phone = PhoneFactory.getPhone(slotId);
        return phone != null ? phone.getFullIccSerialNumber() : "";
    }

    private boolean validatePin(String pin) {
        return pin != null && pin.length() >= MIN_PIN_LENGTH && pin.length() <= MAX_PIN_LENGTH;
    }

    private boolean validateIccid(String iccid) {
        return iccid != null && iccid.length() >= MIN_ICCID_LENGTH;
    }

    private boolean validateSlotId(int slotId) {
        return slotId >= 0 && slotId < getSlotCount();
    }

    /** Checks if the PIN status of the SIM in slot {@code slotId} is a given {@code PinState}. */
    private boolean isPinState(int slotId, PinState pinState) {
        UiccProfile profile = UiccController.getInstance().getUiccProfileForPhone(slotId);
        if (profile != null) {
            // Loop thru all possible app families to identify at least one that is available in
            // order to check the PIN state.
            int[] families = {
                    UiccController.APP_FAM_3GPP,
                    UiccController.APP_FAM_3GPP2,
                    UiccController.APP_FAM_IMS };
            for (int i = 0; i < families.length; i++) {
                UiccCardApplication app = profile.getApplication(i);
                if (app != null) {
                    return app.getPin1State() == pinState;
                }
            }
        }
        return false;
    }

    /** Returns if the PIN cache is allowed for a given slot. */
    private boolean isCacheAllowed(int slotId) {
        return isCacheAllowedByDevice() && isCacheAllowedByCarrier(slotId);
    }

    /** Returns if the PIN cache is allowed by the device. */
    private boolean isCacheAllowedByDevice() {
        if (!mContext.getResources().getBoolean(
                R.bool.config_allow_pin_storage_for_unattended_reboot)) {
            logv("Pin caching disabled in resources");
            return false;
        }
        return true;
    }

    /** Returns if the PIN cache is allowed by carrier for a given slot. */
    private boolean isCacheAllowedByCarrier(int slotId) {
        PersistableBundle config = null;
        CarrierConfigManager configManager =
                mContext.getSystemService(CarrierConfigManager.class);
        if (configManager != null) {
            Phone phone = PhoneFactory.getPhone(slotId);
            if (phone != null) {
                 // If an invalid subId is used, this bundle will contain default values.
                config = configManager.getConfigForSubId(phone.getSubId());
            }
        }
        if (config == null) {
            config = CarrierConfigManager.getDefaultConfig();
        }

        return config.getBoolean(
                CarrierConfigManager.KEY_STORE_SIM_PIN_FOR_UNATTENDED_REBOOT_BOOL, true);
    }

    /** Initializes KeyStore and returns the instance. */
    @Nullable
    private static KeyStore initializeKeyStore() {
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE_PROVIDER);
            keyStore.load(/*param=*/ null);
        } catch (Exception e) {
            // Should never happen.
            loge("Error loading KeyStore", e);
            return null;
        }
        logv("KeyStore ready");
        return keyStore;
    }

    /**
     * Initializes a secret key and returns it.
     *
     * @param alias alias of the key in {@link KeyStore}.
     * @param createIfAbsent indicates weather the key must be created if not already present.
     * @return the {@link SecretKey}, or null if the key does not exist.
     */
    @Nullable
    private SecretKey initializeSecretKey(String alias, boolean createIfAbsent) {
        if (mKeyStore == null) {
            return null;
        }

        SecretKey secretKey = getSecretKey(alias);
        if (secretKey != null) {
            logd("KeyStore: alias %s exists", alias);
            return secretKey;
        } else if (createIfAbsent) {
            Date expiration =
                    KEYSTORE_ALIAS_SHORT_TERM.equals(alias) ? getShortLivedKeyValidityEnd() : null;
            boolean isUserAuthRequired =
                    !KEYSTORE_ALIAS_LONG_TERM_ALWAYS.equals(alias) && isDeviceSecure();
            logd("KeyStore: alias %s does not exist - Creating (exp=%s, auth=%s)",
                    alias, expiration != null ? expiration.toString() : "", isUserAuthRequired);
            return createSecretKey(alias, expiration, isUserAuthRequired);
        } else {
            // Nothing to do
            logd("KeyStore: alias %s does not exist - Nothing to do", alias);
            return null;
        }
    }

    /**
     * Retrieves the secret key previously stored in {@link KeyStore}.
     *
     * @param alias alias of the key in {@link KeyStore}.
     * @return the {@link SecretKey}, or null in case of error or if the key does not exist.
     */
    @Nullable
    private SecretKey getSecretKey(String alias) {
        try {
            final KeyStore.SecretKeyEntry secretKeyEntry =
                    (KeyStore.SecretKeyEntry) mKeyStore.getEntry(alias, null);
            if (secretKeyEntry != null) {
                return secretKeyEntry.getSecretKey();
            }
        } catch (Exception e) {
            // In case of exception, it means that key exists, but cannot be retrieved
            // We delete the old key, so that a new key can be created.
            loge("Exception with getting the key " + alias, e);
            deleteSecretKey(alias);
        }
        return null;
    }

    /**
     * Generates a new secret key in {@link KeyStore}.
     *
     * @param alias alias of the key in {@link KeyStore}.
     * @param expiration expiration of the key, or null if the key does not expire.
     * @param isUserAuthRequired indicates if user authentication is required to use the key
     * @return the created {@link SecretKey}, or null in case of error
     */
    @Nullable
    private SecretKey createSecretKey(String alias, Date expiration, boolean isUserAuthRequired) {
        try {
            final KeyGenerator keyGenerator =
                    KeyGenerator.getInstance(KEY_ALGORITHM_AES, ANDROID_KEY_STORE_PROVIDER);
            KeyGenParameterSpec.Builder keyGenParameterSpec =
                    new KeyGenParameterSpec.Builder(alias, PURPOSE_ENCRYPT | PURPOSE_DECRYPT)
                        .setBlockModes(BLOCK_MODE_GCM)
                        .setEncryptionPaddings(ENCRYPTION_PADDING_NONE);
            if (expiration != null) {
                keyGenParameterSpec = keyGenParameterSpec
                        .setKeyValidityEnd(expiration);
            }
            if (isUserAuthRequired) {
                keyGenParameterSpec = keyGenParameterSpec
                        .setUserAuthenticationRequired(true)
                        .setUserAuthenticationParameters(Integer.MAX_VALUE, AUTH_DEVICE_CREDENTIAL);
            }
            keyGenerator.init(keyGenParameterSpec.build());
            return keyGenerator.generateKey();
        } catch (Exception e) {
            loge("Create key exception", e);
            return null;
        }
    }

    /** Returns the validity end of a new short-lived key, or null if key does not expire. */
    @Nullable
    private Date getShortLivedKeyValidityEnd() {
        if (mShortTermSecretKeyDurationMinutes > 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.MINUTE, mShortTermSecretKeyDurationMinutes);
            return calendar.getTime();
        } else {
            return null;
        }
    }

    /** Deletes the short term key from KeyStore, if it exists. */
    private void deleteSecretKey(String alias) {
        if (mKeyStore != null) {
            logd("Delete key: %s", alias);
            try {
                mKeyStore.deleteEntry(alias);
            } catch (Exception e) {
                // Nothing to do. Even if the key removal fails, it becomes unusable.
                loge("Delete key exception");
            }
        }
    }

    /** Returns the encrypted version of {@code input}, or an empty array in case of error. */
    private byte[] encrypt(SecretKey secretKey, byte[] input) {
        if (secretKey == null) {
            loge("Encrypt: Secret key is null");
            return new byte[0];
        }

        try {
            final Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            EncryptedPin encryptedPin = new EncryptedPin();
            encryptedPin.iv = cipher.getIV();
            encryptedPin.encryptedStoredPin = cipher.doFinal(input);
            return EncryptedPin.toByteArray(encryptedPin);
        } catch (Exception e) {
            loge("Encrypt exception", e);
            TelephonyStatsLog.write(PIN_STORAGE_EVENT,
                    PIN_STORAGE_EVENT__EVENT__PIN_ENCRYPTION_ERROR, 1);
        }
        return new byte[0];
    }

    /** Returns the decrypted version of {@code input}, or an empty array in case of error. */
    private byte[] decrypt(SecretKey secretKey, byte[] input) {
        if (secretKey == null) {
            loge("Decrypt: Secret key is null");
            return new byte[0];
        }

        try {
            EncryptedPin encryptedPin = EncryptedPin.parseFrom(input);
            if (!ArrayUtils.isEmpty(encryptedPin.encryptedStoredPin)
                    && !ArrayUtils.isEmpty(encryptedPin.iv)) {
                final Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
                final GCMParameterSpec spec =
                        new GCMParameterSpec(GCM_PARAMETER_TAG_BIT_LEN, encryptedPin.iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
                return cipher.doFinal(encryptedPin.encryptedStoredPin);
            }
        } catch (Exception e) {
            loge("Decrypt exception", e);
            TelephonyStatsLog.write(PIN_STORAGE_EVENT,
                    PIN_STORAGE_EVENT__EVENT__PIN_DECRYPTION_ERROR, 1);
        }
        return new byte[0];
    }

    private static void logv(String format, Object... args) {
        if (VDBG) {
            Rlog.d(TAG, String.format(format, args));
        }
    }

    private static void logd(String format, Object... args) {
        Rlog.d(TAG, String.format(format, args));
    }

    private static void loge(String format, Object... args) {
        Rlog.e(TAG, String.format(format, args));
    }

    private static void loge(String msg, Throwable tr) {
        Rlog.e(TAG, msg, tr);
    }

    void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("PinStorage:");
        pw.println(" mIsDeviceSecure=" + mIsDeviceSecure);
        pw.println(" mIsDeviceLocked=" + mIsDeviceLocked);
        pw.println(" isLongTermSecretKey=" + (boolean) (mLongTermSecretKey != null));
        pw.println(" isShortTermSecretKey=" + (boolean) (mShortTermSecretKey != null));
        pw.println(" isCacheAllowedByDevice=" + isCacheAllowedByDevice());
        int slotCount = getSlotCount();
        for (int i = 0; i < slotCount; i++) {
            pw.println(" isCacheAllowedByCarrier[" + i + "]=" + isCacheAllowedByCarrier(i));
        }
        if (VDBG) {
            SparseArray<StoredPin> storedPins = loadPinInformation();
            for (int i = 0; i < storedPins.size(); i++) {
                pw.println(" pin=" + storedPins.valueAt(i).toString());
            }
        }
    }
}
