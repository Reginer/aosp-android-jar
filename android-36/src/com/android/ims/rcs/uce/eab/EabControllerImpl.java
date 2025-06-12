/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.ims.rcs.uce.eab;

import static android.telephony.ims.RcsContactUceCapability.CAPABILITY_MECHANISM_OPTIONS;
import static android.telephony.ims.RcsContactUceCapability.CAPABILITY_MECHANISM_PRESENCE;
import static android.telephony.ims.RcsContactUceCapability.REQUEST_RESULT_NOT_FOUND;
import static android.telephony.ims.RcsContactUceCapability.SOURCE_TYPE_CACHED;

import static com.android.ims.rcs.uce.eab.EabProvider.EAB_OPTIONS_TABLE_NAME;
import static com.android.ims.rcs.uce.eab.EabProvider.EAB_PRESENCE_TUPLE_TABLE_NAME;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ProvisioningManager;
import android.telephony.ims.RcsContactPresenceTuple;
import android.telephony.ims.RcsContactPresenceTuple.ServiceCapabilities;
import android.telephony.ims.RcsContactUceCapability;
import android.telephony.ims.RcsContactUceCapability.OptionsBuilder;
import android.telephony.ims.RcsContactUceCapability.PresenceBuilder;
import android.text.TextUtils;
import android.util.Log;

import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber;
import com.android.ims.RcsFeatureManager;
import com.android.ims.rcs.uce.UceController.UceControllerCallback;
import com.android.internal.annotations.VisibleForTesting;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The implementation of EabController.
 */
public class EabControllerImpl implements EabController {
    private static final String TAG = "EabControllerImpl";

    // 7 days
    private static final int DEFAULT_NON_RCS_CAPABILITY_CACHE_EXPIRATION_SEC = 7 * 24 * 60 * 60;
    // 1 day
    private static final int DEFAULT_CAPABILITY_CACHE_EXPIRATION_SEC = 24 * 60 * 60;
    private static final int DEFAULT_AVAILABILITY_CACHE_EXPIRATION_SEC = 60;

    // 1 week
    private static final int CLEAN_UP_LEGACY_CAPABILITY_SEC = 7 * 24 * 60 * 60;
    private static final int CLEAN_UP_LEGACY_CAPABILITY_DELAY_MILLI_SEC = 30 * 1000;

    private final Context mContext;
    private final int mSubId;
    private final EabBulkCapabilityUpdater mEabBulkCapabilityUpdater;
    private final Handler mHandler;

    private UceControllerCallback mUceControllerCallback;
    private volatile boolean mIsSetDestroyedFlag = false;

    private ExpirationTimeFactory mExpirationTimeFactory = () -> Instant.now().getEpochSecond();

    @VisibleForTesting
    public final Runnable mCapabilityCleanupRunnable = () -> {
        Log.d(TAG, "Cleanup Capabilities");
        cleanupExpiredCapabilities();
    };

    @VisibleForTesting
    public interface ExpirationTimeFactory {
        long getExpirationTime();
    }

    public EabControllerImpl(Context context, int subId, UceControllerCallback c, Looper looper) {
        mContext = context;
        mSubId = subId;
        mUceControllerCallback = c;
        mHandler = new Handler(looper);
        mEabBulkCapabilityUpdater = new EabBulkCapabilityUpdater(mContext, mSubId,
                this,
                new EabContactSyncController(),
                mUceControllerCallback,
                mHandler);
    }

    @Override
    public void onRcsConnected(RcsFeatureManager manager) {
    }

    @Override
    public void onRcsDisconnected() {
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mIsSetDestroyedFlag = true;
        mEabBulkCapabilityUpdater.onDestroy();
    }

    @Override
    public void onCarrierConfigChanged() {
        // Pick up changes to CarrierConfig and run any applicable cleanup tasks associated with
        // that configuration.
        mCapabilityCleanupRunnable.run();
        cleanupOrphanedRows();
        if (!mIsSetDestroyedFlag) {
            mEabBulkCapabilityUpdater.onCarrierConfigChanged();
        }
    }

    /**
     * Set the callback for sending the request to UceController.
     */
    @Override
    public void setUceRequestCallback(UceControllerCallback c) {
        Objects.requireNonNull(c);
        if (mIsSetDestroyedFlag) {
            Log.d(TAG, "EabController destroyed.");
            return;
        }
        mUceControllerCallback = c;
        mEabBulkCapabilityUpdater.setUceRequestCallback(c);
    }

    /**
     * Retrieve the contacts' capabilities from the EAB database.
     */
    @Override
    public @NonNull List<EabCapabilityResult> getCapabilities(@NonNull List<Uri> uris) {
        Objects.requireNonNull(uris);
        if (mIsSetDestroyedFlag) {
            Log.d(TAG, "EabController destroyed.");
            return generateDestroyedResult(uris);
        }

        Log.d(TAG, "getCapabilities uri size=" + uris.size());
        List<EabCapabilityResult> capabilityResultList = new ArrayList();

        for (Uri uri : uris) {
            EabCapabilityResult result = generateEabResult(uri, this::isCapabilityExpired);
            capabilityResultList.add(result);
        }
        return capabilityResultList;
    }

    /**
     * Retrieve the contacts' capabilities from the EAB database including expired capabilities.
     */
    @Override
    public @NonNull List<EabCapabilityResult> getCapabilitiesIncludingExpired(
            @NonNull List<Uri> uris) {
        Objects.requireNonNull(uris);
        if (mIsSetDestroyedFlag) {
            Log.d(TAG, "EabController destroyed.");
            return generateDestroyedResult(uris);
        }

        Log.d(TAG, "getCapabilitiesIncludingExpired uri size=" + uris.size());
        List<EabCapabilityResult> capabilityResultList = new ArrayList();

        for (Uri uri : uris) {
            EabCapabilityResult result = generateEabResultIncludingExpired(uri,
                    this::isCapabilityExpired);
            capabilityResultList.add(result);
        }
        return capabilityResultList;
    }

    /**
     * Retrieve the contact's capabilities from the availability cache.
     */
    @Override
    public @NonNull EabCapabilityResult getAvailability(@NonNull Uri contactUri) {
        Objects.requireNonNull(contactUri);
        if (mIsSetDestroyedFlag) {
            Log.d(TAG, "EabController destroyed.");
            return new EabCapabilityResult(
                    contactUri,
                    EabCapabilityResult.EAB_CONTROLLER_DESTROYED_FAILURE,
                    null);
        }
        return generateEabResult(contactUri, this::isAvailabilityExpired);
    }

    /**
     * Retrieve the contact's capabilities from the availability cache including expired
     * capabilities.
     */
    @Override
    public @NonNull EabCapabilityResult getAvailabilityIncludingExpired(@NonNull Uri contactUri) {
        Objects.requireNonNull(contactUri);
        if (mIsSetDestroyedFlag) {
            Log.d(TAG, "EabController destroyed.");
            return new EabCapabilityResult(
                contactUri,
                EabCapabilityResult.EAB_CONTROLLER_DESTROYED_FAILURE,
                null);
        }
        return generateEabResultIncludingExpired(contactUri, this::isAvailabilityExpired);
    }

    /**
     * Update the availability catch and save the capabilities to the EAB database.
     */
    @Override
    public void saveCapabilities(@NonNull List<RcsContactUceCapability> contactCapabilities) {
        Objects.requireNonNull(contactCapabilities);
        if (mIsSetDestroyedFlag) {
            Log.d(TAG, "EabController destroyed.");
            return;
        }

        Log.d(TAG, "Save capabilities: " + contactCapabilities.size());

        // Update the capabilities
        for (RcsContactUceCapability capability : contactCapabilities) {
            String phoneNumber = getNumberFromUri(mContext, capability.getContactUri());
            Cursor c = mContext.getContentResolver().query(
                    EabProvider.CONTACT_URI, null,
                    EabProvider.ContactColumns.PHONE_NUMBER + "=?",
                    new String[]{phoneNumber}, null);

            if (c != null && c.moveToNext()) {
                int contactId = getIntValue(c, EabProvider.ContactColumns._ID);
                if (capability.getCapabilityMechanism() == CAPABILITY_MECHANISM_PRESENCE) {
                    Log.d(TAG, "Insert presence capability");
                    deleteOldPresenceCapability(contactId);
                    insertNewPresenceCapability(contactId, capability);
                } else if (capability.getCapabilityMechanism() == CAPABILITY_MECHANISM_OPTIONS) {
                    Log.d(TAG, "Insert options capability");
                    deleteOldOptionCapability(contactId);
                    insertNewOptionCapability(contactId, capability);
                }
            } else {
                Log.e(TAG, "The phone number can't find in contact table. ");
                int contactId = insertNewContact(phoneNumber);
                if (capability.getCapabilityMechanism() == CAPABILITY_MECHANISM_PRESENCE) {
                    insertNewPresenceCapability(contactId, capability);
                } else if (capability.getCapabilityMechanism() == CAPABILITY_MECHANISM_OPTIONS) {
                    insertNewOptionCapability(contactId, capability);
                }
            }

            if (c != null) {
                c.close();
            }
        }
        cleanupOrphanedRows();
        mEabBulkCapabilityUpdater.updateExpiredTimeAlert();

        if (mHandler.hasCallbacks(mCapabilityCleanupRunnable)) {
            mHandler.removeCallbacks(mCapabilityCleanupRunnable);
        }
        mHandler.postDelayed(mCapabilityCleanupRunnable,
                CLEAN_UP_LEGACY_CAPABILITY_DELAY_MILLI_SEC);
    }

    /**
     * Cleanup the entry of common table that can't map to presence or option table
     */
    @VisibleForTesting
    public void cleanupOrphanedRows() {
        String presenceSelection =
                " (SELECT " + EabProvider.PresenceTupleColumns.EAB_COMMON_ID +
                        " FROM " + EAB_PRESENCE_TUPLE_TABLE_NAME + ") ";
        String optionSelection =
                " (SELECT " + EabProvider.OptionsColumns.EAB_COMMON_ID +
                        " FROM " + EAB_OPTIONS_TABLE_NAME + ") ";

        mContext.getContentResolver().delete(
                EabProvider.COMMON_URI,
                EabProvider.EabCommonColumns._ID + " NOT IN " + presenceSelection +
                        " AND " + EabProvider.EabCommonColumns._ID+ " NOT IN " + optionSelection,
                null);
    }

    private List<EabCapabilityResult> generateDestroyedResult(List<Uri> contactUri) {
        List<EabCapabilityResult> destroyedResult = new ArrayList<>();
        for (Uri uri : contactUri) {
            destroyedResult.add(new EabCapabilityResult(
                    uri,
                    EabCapabilityResult.EAB_CONTROLLER_DESTROYED_FAILURE,
                    null));
        }
        return destroyedResult;
    }

    private EabCapabilityResult generateEabResult(Uri contactUri,
            Predicate<Cursor> isExpiredMethod) {
        RcsUceCapabilityBuilderWrapper builder = null;
        EabCapabilityResult result;

        // query EAB provider
        Uri queryUri = Uri.withAppendedPath(
                Uri.withAppendedPath(EabProvider.ALL_DATA_URI, String.valueOf(mSubId)),
                getNumberFromUri(mContext, contactUri));
        Cursor cursor = mContext.getContentResolver().query(
                queryUri, null, null, null, null);

        if (cursor != null && cursor.getCount() != 0) {
            while (cursor.moveToNext()) {
                if (isExpiredMethod.test(cursor)) {
                    continue;
                }

                if (builder == null) {
                    builder = createNewBuilder(contactUri, cursor);
                } else {
                    updateCapability(contactUri, cursor, builder);
                }
            }
            cursor.close();

            if (builder == null) {
                result = new EabCapabilityResult(contactUri,
                        EabCapabilityResult.EAB_CONTACT_EXPIRED_FAILURE,
                        null);
            } else {
                if (builder.getMechanism() == CAPABILITY_MECHANISM_PRESENCE) {
                    PresenceBuilder presenceBuilder = builder.getPresenceBuilder();
                    result = new EabCapabilityResult(contactUri,
                            EabCapabilityResult.EAB_QUERY_SUCCESSFUL,
                            presenceBuilder.build());
                } else {
                    OptionsBuilder optionsBuilder = builder.getOptionsBuilder();
                    result = new EabCapabilityResult(contactUri,
                            EabCapabilityResult.EAB_QUERY_SUCCESSFUL,
                            optionsBuilder.build());
                }

            }
        } else {
            result = new EabCapabilityResult(contactUri,
                    EabCapabilityResult.EAB_CONTACT_NOT_FOUND_FAILURE, null);
        }
        return result;
    }

    private EabCapabilityResult generateEabResultIncludingExpired(Uri contactUri,
            Predicate<Cursor> isExpiredMethod) {
        RcsUceCapabilityBuilderWrapper builder = null;
        EabCapabilityResult result;
        Optional<Boolean> isExpired = Optional.empty();

        // query EAB provider
        Uri queryUri = Uri.withAppendedPath(
                Uri.withAppendedPath(EabProvider.ALL_DATA_URI, String.valueOf(mSubId)),
                getNumberFromUri(mContext, contactUri));
        Cursor cursor = mContext.getContentResolver().query(queryUri, null, null, null, null);

        if (cursor != null && cursor.getCount() != 0) {
            while (cursor.moveToNext()) {
                // Record whether it has expired.
                if (!isExpired.isPresent()) {
                    isExpired = Optional.of(isExpiredMethod.test(cursor));
                }
                if (builder == null) {
                    builder = createNewBuilder(contactUri, cursor);
                } else {
                    updateCapability(contactUri, cursor, builder);
                }
            }
            cursor.close();

            // Determine the query result
            int eabResult = EabCapabilityResult.EAB_QUERY_SUCCESSFUL;
            if (isExpired.orElse(false)) {
                eabResult = EabCapabilityResult.EAB_CONTACT_EXPIRED_FAILURE;
            }

            if (builder.getMechanism() == CAPABILITY_MECHANISM_PRESENCE) {
                PresenceBuilder presenceBuilder = builder.getPresenceBuilder();
                result = new EabCapabilityResult(contactUri, eabResult, presenceBuilder.build());
            } else {
                OptionsBuilder optionsBuilder = builder.getOptionsBuilder();
                result = new EabCapabilityResult(contactUri, eabResult, optionsBuilder.build());
            }
        } else {
            result = new EabCapabilityResult(contactUri,
                    EabCapabilityResult.EAB_CONTACT_NOT_FOUND_FAILURE, null);
        }
        return result;
    }

    private void updateCapability(Uri contactUri, Cursor cursor,
                RcsUceCapabilityBuilderWrapper builderWrapper) {
        if (builderWrapper.getMechanism() == CAPABILITY_MECHANISM_PRESENCE) {
            PresenceBuilder builder = builderWrapper.getPresenceBuilder();
            if (builder == null) {
                return;
            }
            RcsContactPresenceTuple presenceTuple = createPresenceTuple(contactUri, cursor);
            if (presenceTuple != null) {
                builder.addCapabilityTuple(presenceTuple);
            }
        } else {
            OptionsBuilder builder = builderWrapper.getOptionsBuilder();
            if (builder != null) {
                builder.addFeatureTag(createOptionTuple(cursor));
            }
        }
    }

    private RcsUceCapabilityBuilderWrapper createNewBuilder(Uri contactUri, Cursor cursor) {
        int mechanism = getIntValue(cursor, EabProvider.EabCommonColumns.MECHANISM);
        int result = getIntValue(cursor, EabProvider.EabCommonColumns.REQUEST_RESULT);
        RcsUceCapabilityBuilderWrapper builderWrapper =
                new RcsUceCapabilityBuilderWrapper(mechanism);

        if (mechanism == CAPABILITY_MECHANISM_PRESENCE) {
            PresenceBuilder builder = new PresenceBuilder(
                    contactUri, SOURCE_TYPE_CACHED, result);
            RcsContactPresenceTuple tuple = createPresenceTuple(contactUri, cursor);
            if (tuple != null) {
                builder.addCapabilityTuple(tuple);
            }
            String entityUri = getStringValue(cursor, EabProvider.EabCommonColumns.ENTITY_URI);
            if (!TextUtils.isEmpty(entityUri)) {
                builder.setEntityUri(Uri.parse(entityUri));
            }
            builderWrapper.setPresenceBuilder(builder);
        } else {
            OptionsBuilder builder = new OptionsBuilder(contactUri, SOURCE_TYPE_CACHED);
            builder.setRequestResult(result);
            builder.addFeatureTag(createOptionTuple(cursor));
            builderWrapper.setOptionsBuilder(builder);
        }
        return builderWrapper;
    }

    private String createOptionTuple(Cursor cursor) {
        return getStringValue(cursor, EabProvider.OptionsColumns.FEATURE_TAG);
    }

    private RcsContactPresenceTuple createPresenceTuple(Uri contactUri, Cursor cursor) {
        // RcsContactPresenceTuple fields
        String status = getStringValue(cursor, EabProvider.PresenceTupleColumns.BASIC_STATUS);
        String serviceId = getStringValue(cursor, EabProvider.PresenceTupleColumns.SERVICE_ID);
        String version = getStringValue(cursor, EabProvider.PresenceTupleColumns.SERVICE_VERSION);
        String description = getStringValue(cursor, EabProvider.PresenceTupleColumns.DESCRIPTION);
        String timeStamp = getStringValue(cursor,
                EabProvider.PresenceTupleColumns.REQUEST_TIMESTAMP);

        // ServiceCapabilities fields
        boolean audioCapable = getIntValue(cursor,
                EabProvider.PresenceTupleColumns.AUDIO_CAPABLE) == 1;
        boolean videoCapable = getIntValue(cursor,
                EabProvider.PresenceTupleColumns.VIDEO_CAPABLE) == 1;
        String duplexModes = getStringValue(cursor,
                EabProvider.PresenceTupleColumns.DUPLEX_MODE);
        String unsupportedDuplexModes = getStringValue(cursor,
                EabProvider.PresenceTupleColumns.UNSUPPORTED_DUPLEX_MODE);
        String[] duplexModeList, unsupportedDuplexModeList;

        if (!TextUtils.isEmpty(duplexModes)) {
            duplexModeList = duplexModes.split(",");
        } else {
            duplexModeList = new String[0];
        }
        if (!TextUtils.isEmpty(unsupportedDuplexModes)) {
            unsupportedDuplexModeList = unsupportedDuplexModes.split(",");
        } else {
            unsupportedDuplexModeList = new String[0];
        }

        // Create ServiceCapabilities
        ServiceCapabilities serviceCapabilities;
        ServiceCapabilities.Builder serviceCapabilitiesBuilder =
                new ServiceCapabilities.Builder(audioCapable, videoCapable);
        if (!TextUtils.isEmpty(duplexModes)
                || !TextUtils.isEmpty(unsupportedDuplexModes)) {
            for (String duplexMode : duplexModeList) {
                serviceCapabilitiesBuilder.addSupportedDuplexMode(duplexMode);
            }
            for (String unsupportedDuplex : unsupportedDuplexModeList) {
                serviceCapabilitiesBuilder.addUnsupportedDuplexMode(unsupportedDuplex);
            }
        }
        serviceCapabilities = serviceCapabilitiesBuilder.build();

        // Create RcsContactPresenceTuple
        boolean isTupleEmpty = TextUtils.isEmpty(status) && TextUtils.isEmpty(serviceId)
                && TextUtils.isEmpty(version);
        if (!isTupleEmpty) {
            RcsContactPresenceTuple.Builder rcsContactPresenceTupleBuilder =
                    new RcsContactPresenceTuple.Builder(status, serviceId, version);
            if (description != null) {
                rcsContactPresenceTupleBuilder.setServiceDescription(description);
            }
            if (contactUri != null) {
                rcsContactPresenceTupleBuilder.setContactUri(contactUri);
            }
            if (serviceCapabilities != null) {
                rcsContactPresenceTupleBuilder.setServiceCapabilities(serviceCapabilities);
            }
            if (timeStamp != null) {
                try {
                    Instant instant = Instant.ofEpochSecond(Long.parseLong(timeStamp));
                    rcsContactPresenceTupleBuilder.setTime(instant);
                } catch (NumberFormatException ex) {
                    Log.w(TAG, "Create presence tuple: NumberFormatException");
                } catch (DateTimeParseException e) {
                    Log.w(TAG, "Create presence tuple: parse timestamp failed");
                }
            }
            return rcsContactPresenceTupleBuilder.build();
        } else {
            return null;
        }
    }

    private boolean isCapabilityExpired(Cursor cursor) {
        boolean expired = false;
        String requestTimeStamp = getRequestTimestamp(cursor);
        int capabilityCacheExpiration;

        if (isNonRcsCapability(cursor)) {
            capabilityCacheExpiration = getNonRcsCapabilityCacheExpiration(mSubId);
        } else {
            capabilityCacheExpiration = getCapabilityCacheExpiration(mSubId);
        }

        if (requestTimeStamp != null) {
            Instant expiredTimestamp = Instant
                    .ofEpochSecond(Long.parseLong(requestTimeStamp))
                    .plus(capabilityCacheExpiration, ChronoUnit.SECONDS);
            expired = expiredTimestamp.isBefore(Instant.now());
            Log.d(TAG, "Capability expiredTimestamp: " + expiredTimestamp.getEpochSecond() +
                    ", isNonRcsCapability: " +  isNonRcsCapability(cursor) +
                    ", capabilityCacheExpiration: " + capabilityCacheExpiration +
                    ", expired:" + expired);
        } else {
            Log.d(TAG, "Capability requestTimeStamp is null");
        }
        return expired;
    }

    private boolean isNonRcsCapability(Cursor cursor) {
        int result = getIntValue(cursor, EabProvider.EabCommonColumns.REQUEST_RESULT);
        return result == REQUEST_RESULT_NOT_FOUND;
    }

    private boolean isAvailabilityExpired(Cursor cursor) {
        boolean expired = false;
        String requestTimeStamp = getRequestTimestamp(cursor);

        if (requestTimeStamp != null) {
            Instant expiredTimestamp = Instant
                    .ofEpochSecond(Long.parseLong(requestTimeStamp))
                    .plus(getAvailabilityCacheExpiration(mSubId), ChronoUnit.SECONDS);
            expired = expiredTimestamp.isBefore(Instant.now());
            Log.d(TAG, "Availability insertedTimestamp: "
                    + expiredTimestamp.getEpochSecond() + ", expired:" + expired);
        } else {
            Log.d(TAG, "Capability requestTimeStamp is null");
        }
        return expired;
    }

    private String getRequestTimestamp(Cursor cursor) {
        String expiredTimestamp = null;
        int mechanism = getIntValue(cursor, EabProvider.EabCommonColumns.MECHANISM);
        if (mechanism == CAPABILITY_MECHANISM_PRESENCE) {
            expiredTimestamp = getStringValue(cursor,
                    EabProvider.PresenceTupleColumns.REQUEST_TIMESTAMP);

        } else if (mechanism == CAPABILITY_MECHANISM_OPTIONS) {
            expiredTimestamp = getStringValue(cursor, EabProvider.OptionsColumns.REQUEST_TIMESTAMP);
        }
        return expiredTimestamp;
    }

    private int getNonRcsCapabilityCacheExpiration(int subId) {
        int value;
        PersistableBundle carrierConfig =
                mContext.getSystemService(CarrierConfigManager.class).getConfigForSubId(subId);

        if (carrierConfig != null) {
            value = carrierConfig.getInt(
                    CarrierConfigManager.Ims.KEY_NON_RCS_CAPABILITIES_CACHE_EXPIRATION_SEC_INT);
        } else {
            value = DEFAULT_NON_RCS_CAPABILITY_CACHE_EXPIRATION_SEC;
            Log.e(TAG, "getNonRcsCapabilityCacheExpiration: " +
                    "CarrierConfig is null, returning default");
        }
        return value;
    }

    protected int getCapabilityCacheExpiration(int subId) {
        int value = -1;
        try {
            ProvisioningManager pm = ProvisioningManager.createForSubscriptionId(subId);
            value = pm.getProvisioningIntValue(
                    ProvisioningManager.KEY_RCS_CAPABILITIES_CACHE_EXPIRATION_SEC);
        } catch (Exception ex) {
            Log.e(TAG, "Exception in getCapabilityCacheExpiration(): " + ex);
        }

        if (value <= 0) {
            value = DEFAULT_CAPABILITY_CACHE_EXPIRATION_SEC;
            Log.e(TAG, "The capability expiration cannot be less than 0.");
        }
        return value;
    }

    protected long getAvailabilityCacheExpiration(int subId) {
        long value = -1;
        try {
            ProvisioningManager pm = ProvisioningManager.createForSubscriptionId(subId);
            value = pm.getProvisioningIntValue(
                    ProvisioningManager.KEY_RCS_AVAILABILITY_CACHE_EXPIRATION_SEC);
        } catch (Exception ex) {
            Log.e(TAG, "Exception in getAvailabilityCacheExpiration(): " + ex);
        }

        if (value <= 0) {
            value = DEFAULT_AVAILABILITY_CACHE_EXPIRATION_SEC;
            Log.e(TAG, "The Availability expiration cannot be less than 0.");
        }
        return value;
    }

    private int insertNewContact(String phoneNumber) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(EabProvider.ContactColumns.PHONE_NUMBER, phoneNumber);
        Uri result = mContext.getContentResolver().insert(EabProvider.CONTACT_URI, contentValues);
        return Integer.parseInt(result.getLastPathSegment());
    }

    private void deleteOldPresenceCapability(int id) {
        Cursor c = mContext.getContentResolver().query(
                EabProvider.COMMON_URI,
                new String[]{EabProvider.EabCommonColumns._ID},
                EabProvider.EabCommonColumns.EAB_CONTACT_ID + "=?",
                new String[]{String.valueOf(id)}, null);

        if (c != null && c.getCount() > 0) {
            while(c.moveToNext()) {
                int commonId = c.getInt(c.getColumnIndex(EabProvider.EabCommonColumns._ID));
                mContext.getContentResolver().delete(
                        EabProvider.PRESENCE_URI,
                        EabProvider.PresenceTupleColumns.EAB_COMMON_ID + "=?",
                        new String[]{String.valueOf(commonId)});
            }
        }

        if (c != null) {
            c.close();
        }
    }

    private void insertNewPresenceCapability(int contactId, RcsContactUceCapability capability) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(EabProvider.EabCommonColumns.EAB_CONTACT_ID, contactId);
        contentValues.put(EabProvider.EabCommonColumns.MECHANISM, CAPABILITY_MECHANISM_PRESENCE);
        contentValues.put(EabProvider.EabCommonColumns.SUBSCRIPTION_ID, mSubId);
        contentValues.put(EabProvider.EabCommonColumns.REQUEST_RESULT,
                capability.getRequestResult());
        if (capability.getEntityUri() != null) {
            contentValues.put(EabProvider.EabCommonColumns.ENTITY_URI,
                    capability.getEntityUri().toString());
        }
        Uri result = mContext.getContentResolver().insert(EabProvider.COMMON_URI, contentValues);
        int commonId = Integer.parseInt(result.getLastPathSegment());
        Log.d(TAG, "Insert into common table. Id: " + commonId);

        if (capability.getCapabilityTuples().size() == 0) {
            insertEmptyTuple(commonId);
        } else {
            insertAllTuples(commonId, capability);
        }
    }

    private void insertEmptyTuple(int commonId) {
        Log.d(TAG, "Insert empty tuple into presence table.");
        ContentValues contentValues = new ContentValues();
        contentValues.put(EabProvider.PresenceTupleColumns.EAB_COMMON_ID, commonId);
        // Using current timestamp instead of network timestamp since there is not use cases for
        // network timestamp and the network timestamp may cause capability expire immediately.
        contentValues.put(EabProvider.PresenceTupleColumns.REQUEST_TIMESTAMP,
                mExpirationTimeFactory.getExpirationTime());
        mContext.getContentResolver().insert(EabProvider.PRESENCE_URI, contentValues);
    }

    private void insertAllTuples(int commonId, RcsContactUceCapability capability) {
        ContentValues[] presenceContent =
                new ContentValues[capability.getCapabilityTuples().size()];

        for (int i = 0; i < presenceContent.length; i++) {
            RcsContactPresenceTuple tuple = capability.getCapabilityTuples().get(i);

            // Create new ServiceCapabilities
            ServiceCapabilities serviceCapabilities = tuple.getServiceCapabilities();
            String duplexMode = null, unsupportedDuplexMode = null;
            if (serviceCapabilities != null) {
                List<String> duplexModes = serviceCapabilities.getSupportedDuplexModes();
                if (duplexModes.size() != 0) {
                    duplexMode = TextUtils.join(",", duplexModes);
                }

                List<String> unsupportedDuplexModes =
                        serviceCapabilities.getUnsupportedDuplexModes();
                if (unsupportedDuplexModes.size() != 0) {
                    unsupportedDuplexMode =
                            TextUtils.join(",", unsupportedDuplexModes);
                }
            }

            ContentValues contentValues = new ContentValues();
            contentValues.put(EabProvider.PresenceTupleColumns.EAB_COMMON_ID, commonId);
            contentValues.put(EabProvider.PresenceTupleColumns.BASIC_STATUS, tuple.getStatus());
            contentValues.put(EabProvider.PresenceTupleColumns.SERVICE_ID, tuple.getServiceId());
            contentValues.put(EabProvider.PresenceTupleColumns.SERVICE_VERSION,
                    tuple.getServiceVersion());
            contentValues.put(EabProvider.PresenceTupleColumns.DESCRIPTION,
                    tuple.getServiceDescription());

            // Using current timestamp instead of network timestamp since there is not use cases for
            // network timestamp and the network timestamp may cause capability expire immediately.
            contentValues.put(EabProvider.PresenceTupleColumns.REQUEST_TIMESTAMP,
                    mExpirationTimeFactory.getExpirationTime());
            contentValues.put(EabProvider.PresenceTupleColumns.CONTACT_URI,
                    tuple.getContactUri().toString());
            if (serviceCapabilities != null) {
                contentValues.put(EabProvider.PresenceTupleColumns.DUPLEX_MODE, duplexMode);
                contentValues.put(EabProvider.PresenceTupleColumns.UNSUPPORTED_DUPLEX_MODE,
                        unsupportedDuplexMode);

                contentValues.put(EabProvider.PresenceTupleColumns.AUDIO_CAPABLE,
                        serviceCapabilities.isAudioCapable());
                contentValues.put(EabProvider.PresenceTupleColumns.VIDEO_CAPABLE,
                        serviceCapabilities.isVideoCapable());
            }
            presenceContent[i] = contentValues;
        }
        Log.d(TAG, "Insert into presence table. count: " + presenceContent.length);
        mContext.getContentResolver().bulkInsert(EabProvider.PRESENCE_URI, presenceContent);
    }

    private void deleteOldOptionCapability(int contactId) {
        Cursor c = mContext.getContentResolver().query(
                EabProvider.COMMON_URI,
                new String[]{EabProvider.EabCommonColumns._ID},
                EabProvider.EabCommonColumns.EAB_CONTACT_ID + "=?",
                new String[]{String.valueOf(contactId)}, null);

        if (c != null && c.getCount() > 0) {
            while(c.moveToNext()) {
                int commonId = c.getInt(c.getColumnIndex(EabProvider.EabCommonColumns._ID));
                mContext.getContentResolver().delete(
                        EabProvider.OPTIONS_URI,
                        EabProvider.OptionsColumns.EAB_COMMON_ID + "=?",
                        new String[]{String.valueOf(commonId)});
            }
        }

        if (c != null) {
            c.close();
        }
    }

    private void insertNewOptionCapability(int contactId, RcsContactUceCapability capability) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(EabProvider.EabCommonColumns.EAB_CONTACT_ID, contactId);
        contentValues.put(EabProvider.EabCommonColumns.MECHANISM, CAPABILITY_MECHANISM_OPTIONS);
        contentValues.put(EabProvider.EabCommonColumns.SUBSCRIPTION_ID, mSubId);
        contentValues.put(EabProvider.EabCommonColumns.REQUEST_RESULT,
                capability.getRequestResult());
        Uri result = mContext.getContentResolver().insert(EabProvider.COMMON_URI, contentValues);

        int commonId = Integer.valueOf(result.getLastPathSegment());
        List<ContentValues> optionContentList = new ArrayList<>();
        for (String feature : capability.getFeatureTags()) {
            contentValues = new ContentValues();
            contentValues.put(EabProvider.OptionsColumns.EAB_COMMON_ID, commonId);
            contentValues.put(EabProvider.OptionsColumns.FEATURE_TAG, feature);
            contentValues.put(EabProvider.OptionsColumns.REQUEST_TIMESTAMP,
                    Instant.now().getEpochSecond());
            optionContentList.add(contentValues);
        }

        ContentValues[] optionContent = new ContentValues[optionContentList.size()];
        optionContent = optionContentList.toArray(optionContent);
        mContext.getContentResolver().bulkInsert(EabProvider.OPTIONS_URI, optionContent);
    }

    private void cleanupExpiredCapabilities() {
        // Cleanup the capabilities that expired more than 1 week
        long rcsCapabilitiesExpiredTime = Instant.now().getEpochSecond() -
                getCapabilityCacheExpiration(mSubId) -
                CLEAN_UP_LEGACY_CAPABILITY_SEC;

        // Cleanup the capabilities that expired more than 1 week
        long nonRcsCapabilitiesExpiredTime = Instant.now().getEpochSecond() -
                getNonRcsCapabilityCacheExpiration(mSubId) -
                CLEAN_UP_LEGACY_CAPABILITY_SEC;

        cleanupCapabilities(rcsCapabilitiesExpiredTime, getRcsCommonIdList());
        cleanupCapabilities(nonRcsCapabilitiesExpiredTime, getNonRcsCommonIdList());
    }

    private void cleanupCapabilities(long rcsCapabilitiesExpiredTime, List<Integer> commonIdList) {
        if (commonIdList.size() > 0) {
            String presenceClause =
                    EabProvider.PresenceTupleColumns.EAB_COMMON_ID +
                            " IN (" + TextUtils.join(",", commonIdList) + ") " + " AND " +
                            EabProvider.PresenceTupleColumns.REQUEST_TIMESTAMP + "<?";

            String optionClause =
                    EabProvider.PresenceTupleColumns.EAB_COMMON_ID +
                            " IN (" + TextUtils.join(",", commonIdList) + ") " + " AND " +
                            EabProvider.OptionsColumns.REQUEST_TIMESTAMP + "<?";

            int deletePresenceCount = mContext.getContentResolver().delete(
                    EabProvider.PRESENCE_URI,
                    presenceClause,
                    new String[]{String.valueOf(rcsCapabilitiesExpiredTime)});

            int deleteOptionsCount = mContext.getContentResolver().delete(
                    EabProvider.OPTIONS_URI,
                    optionClause,
                    new String[]{String.valueOf(rcsCapabilitiesExpiredTime)});

            Log.d(TAG, "Cleanup capabilities. deletePresenceCount: " + deletePresenceCount +
                ",deleteOptionsCount: " + deleteOptionsCount);
        }
    }

    private List<Integer> getRcsCommonIdList() {
        ArrayList<Integer> list = new ArrayList<>();
        Cursor cursor = mContext.getContentResolver().query(
                EabProvider.COMMON_URI,
                null,
                EabProvider.EabCommonColumns.REQUEST_RESULT + "<>?",
                new String[]{String.valueOf(REQUEST_RESULT_NOT_FOUND)},
                null);

        if (cursor == null) return list;

        while (cursor.moveToNext()) {
            list.add(cursor.getInt(cursor.getColumnIndex(EabProvider.EabCommonColumns._ID)));
        }
        cursor.close();

        return list;
    }

    private List<Integer> getNonRcsCommonIdList() {
        ArrayList<Integer> list = new ArrayList<>();
        Cursor cursor = mContext.getContentResolver().query(
                EabProvider.COMMON_URI,
                null,
                EabProvider.EabCommonColumns.REQUEST_RESULT + "=?",
                new String[]{String.valueOf(REQUEST_RESULT_NOT_FOUND)},
                null);

        if (cursor == null) return list;

        while (cursor.moveToNext()) {
            list.add(cursor.getInt(cursor.getColumnIndex(EabProvider.EabCommonColumns._ID)));
        }
        cursor.close();

        return list;
    }

    private String getStringValue(Cursor cursor, String column) {
        return cursor.getString(cursor.getColumnIndex(column));
    }

    private int getIntValue(Cursor cursor, String column) {
        return cursor.getInt(cursor.getColumnIndex(column));
    }

    private static String getNumberFromUri(Context context, Uri uri) {
        String number = uri.getSchemeSpecificPart();
        String[] numberParts = number.split("[@;:]");
        if (numberParts.length == 0) {
            return null;
        }
        return formatNumber(context, numberParts[0]);
    }

    static String formatNumber(Context context, String number) {
        TelephonyManager manager = context.getSystemService(TelephonyManager.class);
        String simCountryIso = manager.getSimCountryIso();
        if (simCountryIso != null) {
            simCountryIso = simCountryIso.toUpperCase();
            PhoneNumberUtil util = PhoneNumberUtil.getInstance();
            try {
                Phonenumber.PhoneNumber phoneNumber = util.parse(number, simCountryIso);
                return util.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
            } catch (NumberParseException e) {
                Log.w(TAG, "formatNumber: could not format " + number + ", error: " + e);
            }
        }
        return number;
    }

    @VisibleForTesting
    public void setExpirationTimeFactory(ExpirationTimeFactory factory) {
        mExpirationTimeFactory = factory;
    }
}
