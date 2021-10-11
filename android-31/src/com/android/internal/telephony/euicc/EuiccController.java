/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.internal.telephony.euicc;

import android.Manifest;
import android.Manifest.permission;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.provider.Settings;
import android.service.euicc.DownloadSubscriptionResult;
import android.service.euicc.EuiccService;
import android.service.euicc.GetDefaultDownloadableSubscriptionListResult;
import android.service.euicc.GetDownloadableSubscriptionMetadataResult;
import android.service.euicc.GetEuiccProfileInfoListResult;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyFrameworkInitializer;
import android.telephony.TelephonyManager;
import android.telephony.UiccAccessRule;
import android.telephony.UiccCardInfo;
import android.telephony.euicc.DownloadableSubscription;
import android.telephony.euicc.EuiccCardManager.ResetOption;
import android.telephony.euicc.EuiccInfo;
import android.telephony.euicc.EuiccManager;
import android.telephony.euicc.EuiccManager.OtaStatus;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.euicc.EuiccConnector.OtaStatusChangedCallback;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Backing implementation of {@link android.telephony.euicc.EuiccManager}. */
public class EuiccController extends IEuiccController.Stub {
    private static final String TAG = "EuiccController";

    /** Extra set on resolution intents containing the {@link EuiccOperation}. */
    @VisibleForTesting
    static final String EXTRA_OPERATION = "operation";

    /**
     * Time out for {@link #dump(FileDescriptor, PrintWriter, String[])}
     */
    private static final int EUICC_DUMP_TIME_OUT_SECONDS = 5;

    // Aliases so line lengths stay short.
    private static final int OK = EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK;
    private static final int RESOLVABLE_ERROR =
            EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR;
    private static final int ERROR =
            EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR;
    private static final String EXTRA_EMBEDDED_SUBSCRIPTION_DOWNLOADABLE_SUBSCRIPTION =
            EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_DOWNLOADABLE_SUBSCRIPTION;

    /** Restrictions limiting access to the PendingIntent */
    private static final String RESOLUTION_ACTIVITY_PACKAGE_NAME = "com.android.phone";
    private static final String RESOLUTION_ACTIVITY_CLASS_NAME =
            "com.android.phone.euicc.EuiccResolutionUiDispatcherActivity";

    private static EuiccController sInstance;

    private final Context mContext;
    private final EuiccConnector mConnector;
    private final SubscriptionManager mSubscriptionManager;
    private final TelephonyManager mTelephonyManager;
    private final AppOpsManager mAppOpsManager;
    private final PackageManager mPackageManager;

    // These values should be set or updated upon 1) system boot, 2) EuiccService/LPA is bound to
    // the phone process, 3) values are updated remotely by server flags.
    private List<String> mSupportedCountries;
    private List<String> mUnsupportedCountries;

    /** Initialize the instance. Should only be called once. */
    public static EuiccController init(Context context) {
        synchronized (EuiccController.class) {
            if (sInstance == null) {
                sInstance = new EuiccController(context);
            } else {
                Log.wtf(TAG, "init() called multiple times! sInstance = " + sInstance);
            }
        }
        return sInstance;
    }

    /** Get an instance. Assumes one has already been initialized with {@link #init}. */
    public static EuiccController get() {
        if (sInstance == null) {
            synchronized (EuiccController.class) {
                if (sInstance == null) {
                    throw new IllegalStateException("get() called before init()");
                }
            }
        }
        return sInstance;
    }

    private EuiccController(Context context) {
        this(context, new EuiccConnector(context));
        TelephonyFrameworkInitializer
                .getTelephonyServiceManager().getEuiccControllerService().register(this);
    }

    @VisibleForTesting
    public EuiccController(Context context, EuiccConnector connector) {
        mContext = context;
        mConnector = connector;
        mSubscriptionManager = (SubscriptionManager)
                context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        mTelephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        mPackageManager = context.getPackageManager();
    }

    /**
     * Continue an operation which failed with a user-resolvable error.
     *
     * <p>The implementation here makes a key assumption that the resolutionIntent has not been
     * tampered with. This is guaranteed because:
     * <UL>
     * <LI>The intent is wrapped in a PendingIntent created by the phone process which is created
     * with {@link #EXTRA_OPERATION} already present. This means that the operation cannot be
     * overridden on the PendingIntent - a caller can only add new extras.
     * <LI>The resolution activity is restricted by a privileged permission; unprivileged apps
     * cannot start it directly. So the PendingIntent is the only way to start it.
     * </UL>
     */
    @Override
    public void continueOperation(int cardId, Intent resolutionIntent, Bundle resolutionExtras) {
        if (!callerCanWriteEmbeddedSubscriptions()) {
            throw new SecurityException(
                    "Must have WRITE_EMBEDDED_SUBSCRIPTIONS to continue operation");
        }
        long token = Binder.clearCallingIdentity();
        try {
            EuiccOperation op = resolutionIntent.getParcelableExtra(EXTRA_OPERATION);
            if (op == null) {
                throw new IllegalArgumentException("Invalid resolution intent");
            }

            PendingIntent callbackIntent =
                    resolutionIntent.getParcelableExtra(
                            EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_RESOLUTION_CALLBACK_INTENT);
            op.continueOperation(cardId, resolutionExtras, callbackIntent);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /**
     * Return the EID.
     *
     * <p>For API simplicity, this call blocks until completion; while it requires an IPC to load,
     * that IPC should generally be fast, and the EID shouldn't be needed in the normal course of
     * operation.
     */
    @Override
    public String getEid(int cardId, String callingPackage) {
        boolean callerCanReadPhoneStatePrivileged = callerCanReadPhoneStatePrivileged();
        try {
            mAppOpsManager.checkPackage(Binder.getCallingUid(), callingPackage);
        } catch (SecurityException e) {
            EventLog.writeEvent(0x534e4554, "159062405", -1, "Missing UID checking");
            throw e;
        }
        long token = Binder.clearCallingIdentity();
        try {
            if (!callerCanReadPhoneStatePrivileged
                    && !canManageSubscriptionOnTargetSim(cardId, callingPackage)) {
                throw new SecurityException(
                        "Must have carrier privileges on subscription to read EID for cardId="
                                + cardId);
            }

            return blockingGetEidFromEuiccService(cardId);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /**
     * Return the current status of OTA update.
     *
     * <p>For API simplicity, this call blocks until completion; while it requires an IPC to load,
     * that IPC should generally be fast.
     */
    @Override
    public @OtaStatus int getOtaStatus(int cardId) {
        if (!callerCanWriteEmbeddedSubscriptions()) {
            throw new SecurityException("Must have WRITE_EMBEDDED_SUBSCRIPTIONS to get OTA status");
        }
        long token = Binder.clearCallingIdentity();
        try {
            return blockingGetOtaStatusFromEuiccService(cardId);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /**
     * Start eUICC OTA update on the default eUICC if current eUICC OS is not the latest one. When
     * OTA is started or finished, the broadcast {@link EuiccManager#ACTION_OTA_STATUS_CHANGED} will
     * be sent.
     *
     * This function will only be called from phone process and isn't exposed to the other apps.
     *
     * (see {@link #startOtaUpdatingIfNecessary(int cardId)}).
     */
    public void startOtaUpdatingIfNecessary() {
        // TODO(b/120796772) Eventually, we should use startOtaUpdatingIfNecessary(cardId)
        startOtaUpdatingIfNecessary(mTelephonyManager.getCardIdForDefaultEuicc());
    }

    /**
     * Start eUICC OTA update on the given eUICC if current eUICC OS is not the latest one.
     */
    public void startOtaUpdatingIfNecessary(int cardId) {
        mConnector.startOtaIfNecessary(cardId,
                new OtaStatusChangedCallback() {
                    @Override
                    public void onOtaStatusChanged(int status) {
                        sendOtaStatusChangedBroadcast();
                    }

                    @Override
                    public void onEuiccServiceUnavailable() {}
                });
    }

    @Override
    public void getDownloadableSubscriptionMetadata(int cardId,
            DownloadableSubscription subscription, String callingPackage,
            PendingIntent callbackIntent) {
        getDownloadableSubscriptionMetadata(cardId,
                subscription, false /* forceDeactivateSim */, callingPackage, callbackIntent);
    }

    /**
     * Sets the supported or unsupported countries for eUICC.
     *
     * <p>If {@code isSupported} is true, the supported country list will be replaced by
     * {@code countriesList}. Otherwise, unsupported country list will be replaced by
     * {@code countriesList}. For how we determine whether a country is supported by checking
     * supported and unsupported country list please check {@link EuiccManager#isSupportedCountry}.
     *
     * @param isSupported should be true if caller wants to set supported country list. If
     * isSupported is false, un-supported country list will be updated.
     * @param countriesList is a list of strings contains country ISO codes in uppercase.
     */
    @Override
    public void setSupportedCountries(boolean isSupported, @NonNull List<String> countriesList) {
        if (!callerCanWriteEmbeddedSubscriptions()) {
            throw new SecurityException(
                    "Must have WRITE_EMBEDDED_SUBSCRIPTIONS to set supported countries");
        }
        if (isSupported) {
            mSupportedCountries = countriesList;
        } else {
            mUnsupportedCountries = countriesList;
        }
    }

    /**
     * Gets the supported or unsupported countries for eUICC.
     *
     * <p>If {@code isSupported} is true, the supported country list will be returned. Otherwise,
     * unsupported country list will be returned.
     *
     * @param isSupported should be true if caller wants to get supported country list. If
     * isSupported is false, unsupported country list will be returned.
     * @return a list of strings contains country ISO codes in uppercase.
     */
    @Override
    @NonNull
    public List<String> getSupportedCountries(boolean isSupported) {
        if (!callerCanWriteEmbeddedSubscriptions()) {
            throw new SecurityException(
                    "Must have WRITE_EMBEDDED_SUBSCRIPTIONS to get supported countries");
        }
        if (isSupported && mSupportedCountries != null) {
            return mSupportedCountries;
        } else if (!isSupported && mUnsupportedCountries != null) {
            return mUnsupportedCountries;
        }
        return Collections.emptyList();
    }

    /**
     * Returns whether the given country supports eUICC.
     *
     * <p>Supported country list has a higher prority than unsupported country list. If the
     * supported country list is not empty, {@code countryIso} will be considered as supported when
     * it exists in the supported country list. Otherwise {@code countryIso} is not supported. If
     * the supported country list is empty, {@code countryIso} will be considered as supported if it
     * does not exist in the unsupported country list. Otherwise {@code countryIso} is not
     * supported. If both supported and unsupported country lists are empty, then all countries are
     * consider be supported. For how to set supported and unsupported country list, please check
     * {@link #setSupportedCountries}.
     *
     * @param countryIso should be the ISO-3166 country code is provided in uppercase 2 character
     * format.
     * @return whether the given country supports eUICC or not.
     */
    @Override
    public boolean isSupportedCountry(@NonNull String countryIso) {
        if (!callerCanWriteEmbeddedSubscriptions()) {
            throw new SecurityException(
                    "Must have WRITE_EMBEDDED_SUBSCRIPTIONS to check if the country is supported");
        }
        if (mSupportedCountries == null || mSupportedCountries.isEmpty()) {
            Log.i(TAG, "Using deny list unsupportedCountries=" + mUnsupportedCountries);
            return !isEsimUnsupportedCountry(countryIso);
        } else {
            Log.i(TAG, "Using allow list supportedCountries=" + mSupportedCountries);
            return isEsimSupportedCountry(countryIso);
        }
    }

    private boolean isEsimSupportedCountry(String countryIso) {
        if (mSupportedCountries == null || TextUtils.isEmpty(countryIso)) {
            return true;
        }
        return mSupportedCountries.contains(countryIso);
    }

    private boolean isEsimUnsupportedCountry(String countryIso) {
        if (mUnsupportedCountries == null || TextUtils.isEmpty(countryIso)) {
            return false;
        }
        return mUnsupportedCountries.contains(countryIso);
    }

    void getDownloadableSubscriptionMetadata(int cardId, DownloadableSubscription subscription,
            boolean forceDeactivateSim, String callingPackage, PendingIntent callbackIntent) {
        if (!callerCanWriteEmbeddedSubscriptions()) {
            throw new SecurityException("Must have WRITE_EMBEDDED_SUBSCRIPTIONS to get metadata");
        }
        mAppOpsManager.checkPackage(Binder.getCallingUid(), callingPackage);
        long token = Binder.clearCallingIdentity();
        try {
            mConnector.getDownloadableSubscriptionMetadata(cardId,
                    subscription, forceDeactivateSim,
                    new GetMetadataCommandCallback(
                            token, subscription, callingPackage, callbackIntent));
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    class GetMetadataCommandCallback implements EuiccConnector.GetMetadataCommandCallback {
        protected final long mCallingToken;
        protected final DownloadableSubscription mSubscription;
        protected final String mCallingPackage;
        protected final PendingIntent mCallbackIntent;

        GetMetadataCommandCallback(
                long callingToken,
                DownloadableSubscription subscription,
                String callingPackage,
                PendingIntent callbackIntent) {
            mCallingToken = callingToken;
            mSubscription = subscription;
            mCallingPackage = callingPackage;
            mCallbackIntent = callbackIntent;
        }

        @Override
        public void onGetMetadataComplete(int cardId,
                GetDownloadableSubscriptionMetadataResult result) {
            Intent extrasIntent = new Intent();
            final int resultCode;
            switch (result.getResult()) {
                case EuiccService.RESULT_OK:
                    resultCode = OK;
                    extrasIntent.putExtra(
                            EXTRA_EMBEDDED_SUBSCRIPTION_DOWNLOADABLE_SUBSCRIPTION,
                            result.getDownloadableSubscription());
                    break;
                case EuiccService.RESULT_MUST_DEACTIVATE_SIM:
                    resultCode = RESOLVABLE_ERROR;
                    addResolutionIntent(extrasIntent,
                            EuiccService.ACTION_RESOLVE_DEACTIVATE_SIM,
                            mCallingPackage,
                            0 /* resolvableErrors */,
                            false /* confirmationCodeRetried */,
                            getOperationForDeactivateSim(),
                            cardId);
                    break;
                default:
                    resultCode = ERROR;
                    addExtrasToResultIntent(extrasIntent, result.getResult());
                    break;
            }

            sendResult(mCallbackIntent, resultCode, extrasIntent);
        }

        @Override
        public void onEuiccServiceUnavailable() {
            sendResult(mCallbackIntent, ERROR, null /* extrasIntent */);
        }

        protected EuiccOperation getOperationForDeactivateSim() {
            return EuiccOperation.forGetMetadataDeactivateSim(
                    mCallingToken, mSubscription, mCallingPackage);
        }
    }

    @Override
    public void downloadSubscription(int cardId, DownloadableSubscription subscription,
            boolean switchAfterDownload, String callingPackage, Bundle resolvedBundle,
            PendingIntent callbackIntent) {
        downloadSubscription(cardId, subscription, switchAfterDownload, callingPackage,
                false /* forceDeactivateSim */, resolvedBundle, callbackIntent);
    }

    /**
     * Given encoded error code described in
     * {@link android.telephony.euicc.EuiccManager#OPERATION_SMDX_SUBJECT_REASON_CODE} decode it
     * into SubjectCode[5.2.6.1] and ReasonCode[5.2.6.2] from GSMA (SGP.22 v2.2)
     *
     * @param resultCode from
     *               {@link android.telephony.euicc.EuiccManager#OPERATION_SMDX_SUBJECT_REASON_CODE}
     * @return a pair containing SubjectCode[5.2.6.1] and ReasonCode[5.2.6.2] from GSMA (SGP.22
     * v2.2)
     */
    Pair<String, String> decodeSmdxSubjectAndReasonCode(int resultCode) {
        final int numOfSections = 6;
        final int bitsPerSection = 4;
        final int sectionMask = 0xF;

        final Stack<Integer> sections = new Stack<>();

        // Extracting each section of digits backwards.
        for (int i = 0; i < numOfSections; ++i) {
            int sectionDigit = resultCode & sectionMask;
            sections.push(sectionDigit);
            resultCode = resultCode >>> bitsPerSection;
        }

        String subjectCode = sections.pop() + "." + sections.pop() + "." + sections.pop();
        String reasonCode = sections.pop() + "." + sections.pop() + "." + sections.pop();

        // drop the leading zeros, e.g 0.1 -> 1, 0.0.3 -> 3, 0.5.1 -> 5.1
        subjectCode = subjectCode.replaceAll("^(0\\.)*", "");
        reasonCode = reasonCode.replaceAll("^(0\\.)*", "");

        return Pair.create(subjectCode, reasonCode);
    }

    /**
     * Add more detailed information to the resulting intent.
     * Fields added includes(key -> value):
     * 1. {@link EuiccManager#EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE} -> original error code
     * 2. {@link EuiccManager#EXTRA_EMBEDDED_SUBSCRIPTION_OPERATION_CODE} ->
     * EuiccManager.OperationCode such as {@link EuiccManager#OPERATION_DOWNLOAD}
     * 3. if @link EuiccManager.OperationCode is not
     * {@link EuiccManager#OPERATION_SMDX_SUBJECT_REASON_CODE}:
     * {@link EuiccManager#EXTRA_EMBEDDED_SUBSCRIPTION_ERROR_CODE} -> @link
     * EuiccManager.ErrorCode such as {@link EuiccManager#OPERATION_SMDX}
     * 4. if EuiccManager.OperationCode is
     * {@link EuiccManager#OPERATION_SMDX_SUBJECT_REASON_CODE}:
     * a) {@link EuiccManager#EXTRA_EMBEDDED_SUBSCRIPTION_SMDX_SUBJECT_CODE} ->
     * SubjectCode[5.2.6.1] from GSMA (SGP.22 v2.2)
     * b) {@link EuiccManager#EXTRA_EMBEDDED_SUBSCRIPTION_SMDX_REASON_CODE} ->
     * ReasonCode[5.2.6.2] from GSMA (SGP.22 v2.2
     */
    private void addExtrasToResultIntent(Intent intent, int resultCode) {
        final int firstByteBitOffset = 24;
        int errorCodeMask = 0xFFFFFF;
        int operationCode = resultCode >>> firstByteBitOffset;

        intent.putExtra(
                EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE, resultCode);

        intent.putExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_OPERATION_CODE, operationCode);

        // check to see if the operation code is EuiccManager#OPERATION_SMDX_SUBJECT_REASON_CODE
        final boolean isSmdxSubjectReasonCode =
                (operationCode == EuiccManager.OPERATION_SMDX_SUBJECT_REASON_CODE);

        if (isSmdxSubjectReasonCode) {
            final Pair<String, String> subjectReasonCode = decodeSmdxSubjectAndReasonCode(
                    resultCode);
            final String subjectCode = subjectReasonCode.first;
            final String reasonCode = subjectReasonCode.second;
            intent.putExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_SMDX_SUBJECT_CODE,
                    subjectCode);
            intent.putExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_SMDX_REASON_CODE, reasonCode);
        } else {
            final int errorCode = resultCode & errorCodeMask;
            intent.putExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_ERROR_CODE, errorCode);
        }
    }

    void downloadSubscription(int cardId, DownloadableSubscription subscription,
            boolean switchAfterDownload, String callingPackage, boolean forceDeactivateSim,
            Bundle resolvedBundle, PendingIntent callbackIntent) {
        boolean callerCanWriteEmbeddedSubscriptions = callerCanWriteEmbeddedSubscriptions();
        mAppOpsManager.checkPackage(Binder.getCallingUid(), callingPackage);

        long token = Binder.clearCallingIdentity();
        try {
            if (callerCanWriteEmbeddedSubscriptions) {
                // With WRITE_EMBEDDED_SUBSCRIPTIONS, we can skip profile-specific permission checks
                // and move straight to the profile download.
                downloadSubscriptionPrivileged(cardId, token, subscription, switchAfterDownload,
                        forceDeactivateSim, callingPackage, resolvedBundle, callbackIntent);
                return;
            }

            // Without WRITE_EMBEDDED_SUBSCRIPTIONS, we first check whether the caller can manage
            // subscription on the target SIM (see comments below). If yes, the caller *must* be
            // whitelisted per the metadata of the profile to be downloaded, so check the metadata;
            // If no, ask the user's consent before proceed.
            // On a multi-active SIM device, if the caller can manage the active subscription on the
            // target SIM, or there is no active subscription on the target SIM and the caller can
            // manage any active subscription on other SIMs, we perform the download silently.
            // Otherwise, the user must provide consent. If it's a single-active SIM device,
            // determine whether the caller can manage the current profile; if so, we can perform
            // the download silently; if not, the user must provide consent.
            if (canManageSubscriptionOnTargetSim(cardId, callingPackage)) {
                mConnector.getDownloadableSubscriptionMetadata(cardId, subscription,
                    forceDeactivateSim,
                    new DownloadSubscriptionGetMetadataCommandCallback(token, subscription,
                        switchAfterDownload, callingPackage, forceDeactivateSim,
                        callbackIntent, false /* withUserConsent */));
            } else {
                Log.i(TAG, "Caller can't manage subscription on target SIM. "
                        + "Ask user's consent first");
                Intent extrasIntent = new Intent();
                addResolutionIntent(extrasIntent, EuiccService.ACTION_RESOLVE_NO_PRIVILEGES,
                        callingPackage,
                        0 /* resolvableErrors */,
                        false /* confirmationCodeRetried */,
                        EuiccOperation.forDownloadNoPrivilegesOrDeactivateSimCheckMetadata(token,
                                subscription, switchAfterDownload, callingPackage), cardId);
                sendResult(callbackIntent, RESOLVABLE_ERROR, extrasIntent);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    class DownloadSubscriptionGetMetadataCommandCallback extends GetMetadataCommandCallback {
        private final boolean mSwitchAfterDownload;
        private final boolean mForceDeactivateSim;
        private final boolean mWithUserConsent;

        DownloadSubscriptionGetMetadataCommandCallback(long callingToken,
                DownloadableSubscription subscription, boolean switchAfterDownload,
                String callingPackage, boolean forceDeactivateSim,
                PendingIntent callbackIntent, boolean withUserConsent) {
            super(callingToken, subscription, callingPackage, callbackIntent);
            mSwitchAfterDownload = switchAfterDownload;
            mForceDeactivateSim = forceDeactivateSim;
            mWithUserConsent = withUserConsent;
        }

        @Override
        public void onGetMetadataComplete(int cardId,
                GetDownloadableSubscriptionMetadataResult result) {
            DownloadableSubscription subscription = result.getDownloadableSubscription();
            if (mWithUserConsent) {
                // We won't get RESULT_MUST_DEACTIVATE_SIM for the case with user consent.
                if (result.getResult() != EuiccService.RESULT_OK) {
                    // Just propagate the error as normal.
                    super.onGetMetadataComplete(cardId, result);
                    return;
                }

                if (checkCarrierPrivilegeInMetadata(subscription, mCallingPackage)) {
                    // Caller can download this profile. Since we already have the user's consent,
                    // proceed to download.
                    downloadSubscriptionPrivileged(cardId,
                            mCallingToken, subscription, mSwitchAfterDownload,  mForceDeactivateSim,
                            mCallingPackage, null /* resolvedBundle */,
                            mCallbackIntent);
                } else {
                    Log.e(TAG, "Caller does not have carrier privilege in metadata.");
                    sendResult(mCallbackIntent, ERROR, null /* extrasIntent */);
                }
            } else { // !mWithUserConsent
                if (result.getResult() == EuiccService.RESULT_MUST_DEACTIVATE_SIM) {
                    // The caller can manage the target SIM. Ask the user's consent to deactivate
                    // the current SIM.
                    Intent extrasIntent = new Intent();
                    addResolutionIntent(extrasIntent, EuiccService.ACTION_RESOLVE_DEACTIVATE_SIM,
                            mCallingPackage,
                            0 /* resolvableErrors */,
                            false /* confirmationCodeRetried */,
                            EuiccOperation.forDownloadNoPrivilegesOrDeactivateSimCheckMetadata(
                                    mCallingToken, mSubscription, mSwitchAfterDownload,
                                    mCallingPackage),
                            cardId);
                    sendResult(mCallbackIntent, RESOLVABLE_ERROR, extrasIntent);
                    return;
                }

                if (result.getResult() != EuiccService.RESULT_OK) {
                    // Just propagate the error as normal.
                    super.onGetMetadataComplete(cardId, result);
                    return;
                }

                if (checkCarrierPrivilegeInMetadata(subscription, mCallingPackage)) {
                    // Caller can download this profile per profile metadata. Also, caller can
                    // manage the subscription on the target SIM, which is already checked.
                    downloadSubscriptionPrivileged(cardId,
                            mCallingToken, subscription, mSwitchAfterDownload, mForceDeactivateSim,
                            mCallingPackage, null /* resolvedBundle */,
                            mCallbackIntent);
                } else {
                    Log.e(TAG, "Caller is not permitted to download this profile per metadata");
                    sendResult(mCallbackIntent, ERROR, null /* extrasIntent */);
                }
            }
        }
    }

    // Already have user consent. Check metadata first before proceed to download.
    void downloadSubscriptionPrivilegedCheckMetadata(int cardId, final long callingToken,
            DownloadableSubscription subscription, boolean switchAfterDownload,
            boolean forceDeactivateSim, final String callingPackage, Bundle resolvedBundle,
            final PendingIntent callbackIntent) {
        mConnector.getDownloadableSubscriptionMetadata(cardId, subscription, forceDeactivateSim,
                new DownloadSubscriptionGetMetadataCommandCallback(callingToken, subscription,
                        switchAfterDownload, callingPackage, forceDeactivateSim, callbackIntent,
                        true /* withUserConsent */));
    }

    // Continue to download subscription without checking anything.
    void downloadSubscriptionPrivileged(int cardId, final long callingToken,
            DownloadableSubscription subscription, boolean switchAfterDownload,
            boolean forceDeactivateSim, final String callingPackage, Bundle resolvedBundle,
            final PendingIntent callbackIntent) {
        mConnector.downloadSubscription(
                cardId,
                subscription,
                switchAfterDownload,
                forceDeactivateSim,
                resolvedBundle,
                new EuiccConnector.DownloadCommandCallback() {
                    @Override
                    public void onDownloadComplete(DownloadSubscriptionResult result) {
                        Intent extrasIntent = new Intent();
                        final int resultCode;
                        switch (result.getResult()) {
                            case EuiccService.RESULT_OK:
                                resultCode = OK;
                                // Now that a profile has been successfully downloaded, mark the
                                // eUICC as provisioned so it appears in settings UI as appropriate.
                                Settings.Global.putInt(
                                        mContext.getContentResolver(),
                                        Settings.Global.EUICC_PROVISIONED,
                                        1);
                                extrasIntent.putExtra(
                                        EXTRA_EMBEDDED_SUBSCRIPTION_DOWNLOADABLE_SUBSCRIPTION,
                                        subscription);
                                if (!switchAfterDownload) {
                                    // Since we're not switching, nothing will trigger a
                                    // subscription list refresh on its own, so request one here.
                                    refreshSubscriptionsAndSendResult(
                                            callbackIntent, resultCode, extrasIntent);
                                    return;
                                }
                                break;
                            case EuiccService.RESULT_MUST_DEACTIVATE_SIM:
                                resultCode = RESOLVABLE_ERROR;
                                addResolutionIntent(extrasIntent,
                                        EuiccService.ACTION_RESOLVE_DEACTIVATE_SIM,
                                        callingPackage,
                                        0 /* resolvableErrors */,
                                        false /* confirmationCodeRetried */,
                                        EuiccOperation.forDownloadDeactivateSim(
                                                callingToken, subscription, switchAfterDownload,
                                                callingPackage),
                                        cardId);
                                break;
                            case EuiccService.RESULT_RESOLVABLE_ERRORS:
                                // Same value as the deprecated
                                // {@link EuiccService#RESULT_NEED_CONFIRMATION_CODE}. For the
                                // deprecated case, the resolvableErrors is set as 0 in
                                // EuiccService.
                                resultCode = RESOLVABLE_ERROR;
                                boolean retried = false;
                                if (!TextUtils.isEmpty(subscription.getConfirmationCode())) {
                                    retried = true;
                                }
                                if (result.getResolvableErrors() != 0) {
                                    addResolutionIntent(extrasIntent,
                                            EuiccService.ACTION_RESOLVE_RESOLVABLE_ERRORS,
                                            callingPackage,
                                            result.getResolvableErrors(),
                                            retried,
                                            EuiccOperation.forDownloadResolvableErrors(
                                                callingToken, subscription, switchAfterDownload,
                                                callingPackage, result.getResolvableErrors()),
                                            cardId);
                                }  else { // Deprecated case
                                    addResolutionIntent(extrasIntent,
                                            EuiccService.ACTION_RESOLVE_CONFIRMATION_CODE,
                                            callingPackage,
                                            0 /* resolvableErrors */,
                                            retried /* confirmationCodeRetried */,
                                            EuiccOperation.forDownloadConfirmationCode(
                                                callingToken, subscription, switchAfterDownload,
                                                callingPackage),
                                            cardId);
                                }
                                break;
                            default:
                                resultCode = ERROR;

                                addExtrasToResultIntent(extrasIntent, result.getResult());
                                break;
                        }

                        sendResult(callbackIntent, resultCode, extrasIntent);
                    }

                    @Override
                    public void onEuiccServiceUnavailable() {
                        sendResult(callbackIntent, ERROR, null /* extrasIntent */);
                    }
                });
    }

    /**
     * Blocking call to {@link EuiccService#onGetEuiccProfileInfoList} of the eUICC with card ID
     * {@code cardId}.
     *
     * <p>Does not perform permission checks as this is not an exposed API and is only used within
     * the phone process.
     */
    public GetEuiccProfileInfoListResult blockingGetEuiccProfileInfoList(int cardId) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<GetEuiccProfileInfoListResult> resultRef = new AtomicReference<>();
        mConnector.getEuiccProfileInfoList(
                cardId,
                new EuiccConnector.GetEuiccProfileInfoListCommandCallback() {
                    @Override
                    public void onListComplete(GetEuiccProfileInfoListResult result) {
                        resultRef.set(result);
                        latch.countDown();
                    }

                    @Override
                    public void onEuiccServiceUnavailable() {
                        latch.countDown();
                    }
                });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "blockingGetEuiccInfoFromEuiccService got InterruptedException e: " + e);
            Thread.currentThread().interrupt();
        }
        return resultRef.get();
    }

    @Override
    public void getDefaultDownloadableSubscriptionList(int cardId,
            String callingPackage, PendingIntent callbackIntent) {
        getDefaultDownloadableSubscriptionList(cardId,
                false /* forceDeactivateSim */, callingPackage, callbackIntent);
    }

    void getDefaultDownloadableSubscriptionList(int cardId,
            boolean forceDeactivateSim, String callingPackage, PendingIntent callbackIntent) {
        if (!callerCanWriteEmbeddedSubscriptions()) {
            throw new SecurityException(
                    "Must have WRITE_EMBEDDED_SUBSCRIPTIONS to get default list");
        }
        mAppOpsManager.checkPackage(Binder.getCallingUid(), callingPackage);
        long token = Binder.clearCallingIdentity();
        try {
            mConnector.getDefaultDownloadableSubscriptionList(cardId,
                    forceDeactivateSim, new GetDefaultListCommandCallback(
                            token, callingPackage, callbackIntent));
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    class GetDefaultListCommandCallback implements EuiccConnector.GetDefaultListCommandCallback {
        final long mCallingToken;
        final String mCallingPackage;
        final PendingIntent mCallbackIntent;

        GetDefaultListCommandCallback(long callingToken, String callingPackage,
                PendingIntent callbackIntent) {
            mCallingToken = callingToken;
            mCallingPackage = callingPackage;
            mCallbackIntent = callbackIntent;
        }

        @Override
        public void onGetDefaultListComplete(int cardId,
                GetDefaultDownloadableSubscriptionListResult result) {
            Intent extrasIntent = new Intent();
            final int resultCode;
            switch (result.getResult()) {
                case EuiccService.RESULT_OK:
                    resultCode = OK;
                    List<DownloadableSubscription> list = result.getDownloadableSubscriptions();
                    if (list != null && list.size() > 0) {
                        extrasIntent.putExtra(
                                EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_DOWNLOADABLE_SUBSCRIPTIONS,
                                list.toArray(new DownloadableSubscription[list.size()]));
                    }
                    break;
                case EuiccService.RESULT_MUST_DEACTIVATE_SIM:
                    resultCode = RESOLVABLE_ERROR;
                    addResolutionIntent(extrasIntent,
                            EuiccService.ACTION_RESOLVE_DEACTIVATE_SIM,
                            mCallingPackage,
                            0 /* resolvableErrors */,
                            false /* confirmationCodeRetried */,
                            EuiccOperation.forGetDefaultListDeactivateSim(
                                    mCallingToken, mCallingPackage),
                            cardId);
                    break;
                default:
                    resultCode = ERROR;
                    addExtrasToResultIntent(extrasIntent, result.getResult());
                    break;
            }

            sendResult(mCallbackIntent, resultCode, extrasIntent);
        }

        @Override
        public void onEuiccServiceUnavailable() {
            sendResult(mCallbackIntent, ERROR, null /* extrasIntent */);
        }
    }

    /**
     * Return the {@link EuiccInfo}.
     *
     * <p>For API simplicity, this call blocks until completion; while it requires an IPC to load,
     * that IPC should generally be fast, and this info shouldn't be needed in the normal course of
     * operation.
     */
    @Override
    public EuiccInfo getEuiccInfo(int cardId) {
        // No permissions required as EuiccInfo is not sensitive.
        long token = Binder.clearCallingIdentity();
        try {
            return blockingGetEuiccInfoFromEuiccService(cardId);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void deleteSubscription(int cardId, int subscriptionId, String callingPackage,
            PendingIntent callbackIntent) {
        boolean callerCanWriteEmbeddedSubscriptions = callerCanWriteEmbeddedSubscriptions();
        mAppOpsManager.checkPackage(Binder.getCallingUid(), callingPackage);

        long token = Binder.clearCallingIdentity();
        try {
            SubscriptionInfo sub = getSubscriptionForSubscriptionId(subscriptionId);
            if (sub == null) {
                Log.e(TAG, "Cannot delete nonexistent subscription: " + subscriptionId);
                sendResult(callbackIntent, ERROR, null /* extrasIntent */);
                return;
            }

            // For both single active SIM device and multi-active SIM device, if the caller is
            // system or the caller manage the target subscription, we let it continue. This is
            // because deleting subscription won't change status of any other subscriptions.
            if (!callerCanWriteEmbeddedSubscriptions
                    && !mSubscriptionManager.canManageSubscription(sub, callingPackage)) {
                Log.e(TAG, "No permissions: " + subscriptionId);
                sendResult(callbackIntent, ERROR, null /* extrasIntent */);
                return;
            }

            deleteSubscriptionPrivileged(cardId, sub.getIccId(), callbackIntent);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    void deleteSubscriptionPrivileged(int cardId, String iccid,
            final PendingIntent callbackIntent) {
        mConnector.deleteSubscription(
                cardId,
                iccid,
                new EuiccConnector.DeleteCommandCallback() {
                    @Override
                    public void onDeleteComplete(int result) {
                        Intent extrasIntent = new Intent();
                        final int resultCode;
                        switch (result) {
                            case EuiccService.RESULT_OK:
                                resultCode = OK;
                                refreshSubscriptionsAndSendResult(
                                        callbackIntent, resultCode, extrasIntent);
                                return;
                            default:
                                resultCode = ERROR;
                                addExtrasToResultIntent(extrasIntent, result);
                                break;
                        }

                        sendResult(callbackIntent, resultCode, extrasIntent);
                    }

                    @Override
                    public void onEuiccServiceUnavailable() {
                        sendResult(callbackIntent, ERROR, null /* extrasIntent */);
                    }
                });
    }

    @Override
    public void switchToSubscription(int cardId, int subscriptionId, String callingPackage,
            PendingIntent callbackIntent) {
        switchToSubscription(cardId,
                subscriptionId, false /* forceDeactivateSim */, callingPackage, callbackIntent);
    }

    void switchToSubscription(int cardId, int subscriptionId, boolean forceDeactivateSim,
            String callingPackage, PendingIntent callbackIntent) {
        boolean callerCanWriteEmbeddedSubscriptions = callerCanWriteEmbeddedSubscriptions();
        mAppOpsManager.checkPackage(Binder.getCallingUid(), callingPackage);

        long token = Binder.clearCallingIdentity();
        try {
            if (callerCanWriteEmbeddedSubscriptions) {
                // Assume that if a privileged caller is calling us, we don't need to prompt the
                // user about changing carriers, because the caller would only be acting in response
                // to user action.
                forceDeactivateSim = true;
            }

            final String iccid;
            boolean passConsent = false;
            if (subscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                if (callerCanWriteEmbeddedSubscriptions
                        || canManageActiveSubscriptionOnTargetSim(cardId, callingPackage)) {
                    passConsent = true;
                } else {
                    Log.e(TAG, "Not permitted to switch to empty subscription");
                    sendResult(callbackIntent, ERROR, null /* extrasIntent */);
                    return;
                }
                iccid = null;
            } else {
                SubscriptionInfo sub = getSubscriptionForSubscriptionId(subscriptionId);
                if (sub == null) {
                    Log.e(TAG, "Cannot switch to nonexistent sub: " + subscriptionId);
                    sendResult(callbackIntent, ERROR, null /* extrasIntent */);
                    return;
                }
                if (callerCanWriteEmbeddedSubscriptions) {
                    passConsent = true;
                } else {
                    if (!mSubscriptionManager.canManageSubscription(sub, callingPackage)) {
                        Log.e(TAG, "Not permitted to switch to sub: " + subscriptionId);
                        sendResult(callbackIntent, ERROR, null /* extrasIntent */);
                        return;
                    }

                    if (canManageSubscriptionOnTargetSim(cardId, callingPackage)) {
                        passConsent = true;
                    }
                }
                iccid = sub.getIccId();
            }

            if (!passConsent) {
                // Switch needs consent.
                Intent extrasIntent = new Intent();
                addResolutionIntent(extrasIntent,
                        EuiccService.ACTION_RESOLVE_NO_PRIVILEGES,
                        callingPackage,
                        0 /* resolvableErrors */,
                        false /* confirmationCodeRetried */,
                        EuiccOperation.forSwitchNoPrivileges(
                                token, subscriptionId, callingPackage),
                        cardId);
                sendResult(callbackIntent, RESOLVABLE_ERROR, extrasIntent);
                return;
            }

            switchToSubscriptionPrivileged(cardId, token, subscriptionId, iccid, forceDeactivateSim,
                    callingPackage, callbackIntent);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    void switchToSubscriptionPrivileged(int cardId, final long callingToken, int subscriptionId,
            boolean forceDeactivateSim, final String callingPackage,
            final PendingIntent callbackIntent) {
        String iccid = null;
        SubscriptionInfo sub = getSubscriptionForSubscriptionId(subscriptionId);
        if (sub != null) {
            iccid = sub.getIccId();
        }
        switchToSubscriptionPrivileged(cardId, callingToken, subscriptionId, iccid,
                forceDeactivateSim, callingPackage, callbackIntent);
    }

    void switchToSubscriptionPrivileged(int cardId, final long callingToken, int subscriptionId,
            @Nullable String iccid, boolean forceDeactivateSim, final String callingPackage,
            final PendingIntent callbackIntent) {
        mConnector.switchToSubscription(
                cardId,
                iccid,
                forceDeactivateSim,
                new EuiccConnector.SwitchCommandCallback() {
                    @Override
                    public void onSwitchComplete(int result) {
                        Intent extrasIntent = new Intent();
                        final int resultCode;
                        switch (result) {
                            case EuiccService.RESULT_OK:
                                resultCode = OK;
                                break;
                            case EuiccService.RESULT_MUST_DEACTIVATE_SIM:
                                resultCode = RESOLVABLE_ERROR;
                                addResolutionIntent(extrasIntent,
                                        EuiccService.ACTION_RESOLVE_DEACTIVATE_SIM,
                                        callingPackage,
                                        0 /* resolvableErrors */,
                                        false /* confirmationCodeRetried */,
                                        EuiccOperation.forSwitchDeactivateSim(
                                                callingToken, subscriptionId, callingPackage),
                                        cardId);
                                break;
                            default:
                                resultCode = ERROR;
                                addExtrasToResultIntent(extrasIntent, result);
                                break;
                        }

                        sendResult(callbackIntent, resultCode, extrasIntent);
                    }

                    @Override
                    public void onEuiccServiceUnavailable() {
                        sendResult(callbackIntent, ERROR, null /* extrasIntent */);
                    }
                });
    }

    @Override
    public void updateSubscriptionNickname(int cardId, int subscriptionId, String nickname,
            String callingPackage, PendingIntent callbackIntent) {
        boolean callerCanWriteEmbeddedSubscriptions = callerCanWriteEmbeddedSubscriptions();
        mAppOpsManager.checkPackage(Binder.getCallingUid(), callingPackage);

        long token = Binder.clearCallingIdentity();
        try {
            SubscriptionInfo sub = getSubscriptionForSubscriptionId(subscriptionId);
            if (sub == null) {
                Log.e(TAG, "Cannot update nickname to nonexistent sub: " + subscriptionId);
                sendResult(callbackIntent, ERROR, null /* extrasIntent */);
                return;
            }

            // For both single active SIM device and multi-active SIM device, if the caller is
            // system or the caller can manage the target subscription, we let it continue. This is
            // because updating subscription nickname won't affect any other subscriptions.
            if (!callerCanWriteEmbeddedSubscriptions
                    && !mSubscriptionManager.canManageSubscription(sub, callingPackage)) {
                Log.e(TAG, "No permissions: " + subscriptionId);
                sendResult(callbackIntent, ERROR, null /* extrasIntent */);
                return;
            }

            mConnector.updateSubscriptionNickname(cardId,
                    sub.getIccId(), nickname,
                    new EuiccConnector.UpdateNicknameCommandCallback() {
                        @Override
                        public void onUpdateNicknameComplete(int result) {
                            Intent extrasIntent = new Intent();
                            final int resultCode;
                            switch (result) {
                                case EuiccService.RESULT_OK:
                                    resultCode = OK;
                                    refreshSubscriptionsAndSendResult(
                                            callbackIntent, resultCode, extrasIntent);
                                    return;
                                default:
                                    resultCode = ERROR;
                                    addExtrasToResultIntent(extrasIntent, result);
                                    break;
                            }

                            sendResult(callbackIntent, resultCode, extrasIntent);
                        }

                        @Override
                        public void onEuiccServiceUnavailable() {
                            sendResult(callbackIntent, ERROR, null /* extrasIntent */);
                        }
                    });
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void eraseSubscriptions(int cardId, PendingIntent callbackIntent) {
        if (!callerCanWriteEmbeddedSubscriptions()) {
            throw new SecurityException(
                    "Must have WRITE_EMBEDDED_SUBSCRIPTIONS to erase subscriptions");
        }
        long token = Binder.clearCallingIdentity();
        try {
            mConnector.eraseSubscriptions(
                    cardId, new EuiccConnector.EraseCommandCallback() {
                        @Override
                        public void onEraseComplete(int result) {
                            Intent extrasIntent = new Intent();
                            final int resultCode;
                            switch (result) {
                                case EuiccService.RESULT_OK:
                                    resultCode = OK;
                                    refreshSubscriptionsAndSendResult(
                                            callbackIntent, resultCode, extrasIntent);
                                    return;
                                default:
                                    resultCode = ERROR;
                                    addExtrasToResultIntent(extrasIntent, result);
                                    break;
                            }

                            sendResult(callbackIntent, resultCode, extrasIntent);
                        }

                        @Override
                        public void onEuiccServiceUnavailable() {
                            sendResult(callbackIntent, ERROR, null /* extrasIntent */);
                        }
                    });
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void eraseSubscriptionsWithOptions(
            int cardId, @ResetOption int options, PendingIntent callbackIntent) {
        if (!callerCanWriteEmbeddedSubscriptions()) {
            throw new SecurityException(
                    "Must have WRITE_EMBEDDED_SUBSCRIPTIONS to erase subscriptions");
        }
        long token = Binder.clearCallingIdentity();
        try {
            mConnector.eraseSubscriptionsWithOptions(
                    cardId, options, new EuiccConnector.EraseCommandCallback() {
                @Override
                public void onEraseComplete(int result) {
                    Intent extrasIntent = new Intent();
                    final int resultCode;
                    switch (result) {
                        case EuiccService.RESULT_OK:
                            resultCode = OK;
                            refreshSubscriptionsAndSendResult(
                                    callbackIntent, resultCode, extrasIntent);
                            return;
                        default:
                            resultCode = ERROR;
                                    addExtrasToResultIntent(extrasIntent, result);
                            break;
                    }

                    sendResult(callbackIntent, resultCode, extrasIntent);
                }

                @Override
                public void onEuiccServiceUnavailable() {
                    sendResult(callbackIntent, ERROR, null /* extrasIntent */);
                }
            });
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void retainSubscriptionsForFactoryReset(int cardId, PendingIntent callbackIntent) {
        mContext.enforceCallingPermission(Manifest.permission.MASTER_CLEAR,
                "Must have MASTER_CLEAR to retain subscriptions for factory reset");
        long token = Binder.clearCallingIdentity();
        try {
            mConnector.retainSubscriptions(cardId,
                    new EuiccConnector.RetainSubscriptionsCommandCallback() {
                        @Override
                        public void onRetainSubscriptionsComplete(int result) {
                            Intent extrasIntent = new Intent();
                            final int resultCode;
                            switch (result) {
                                case EuiccService.RESULT_OK:
                                    resultCode = OK;
                                    break;
                                default:
                                    resultCode = ERROR;
                                    addExtrasToResultIntent(extrasIntent, result);
                                    break;
                            }

                            sendResult(callbackIntent, resultCode, extrasIntent);
                        }

                        @Override
                        public void onEuiccServiceUnavailable() {
                            sendResult(callbackIntent, ERROR, null /* extrasIntent */);
                        }
                    });
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /** Refresh the embedded subscription list and dispatch the given result upon completion. */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public void refreshSubscriptionsAndSendResult(
            PendingIntent callbackIntent, int resultCode, Intent extrasIntent) {
        SubscriptionController.getInstance()
                .requestEmbeddedSubscriptionInfoListRefresh(
                        () -> sendResult(callbackIntent, resultCode, extrasIntent));
    }

    /** Dispatch the given callback intent with the given result code and data. */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public void sendResult(PendingIntent callbackIntent, int resultCode, Intent extrasIntent) {
        try {
            callbackIntent.send(mContext, resultCode, extrasIntent);
        } catch (PendingIntent.CanceledException e) {
            // Caller canceled the callback; do nothing.
        }
    }

    /** Add a resolution intent to the given extras intent. */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public void addResolutionIntent(Intent extrasIntent, String resolutionAction,
            String callingPackage, int resolvableErrors, boolean confirmationCodeRetried,
            EuiccOperation op, int cardId) {
        Intent intent = new Intent(EuiccManager.ACTION_RESOLVE_ERROR);
        intent.setPackage(RESOLUTION_ACTIVITY_PACKAGE_NAME);
        intent.setComponent(new ComponentName(
                        RESOLUTION_ACTIVITY_PACKAGE_NAME, RESOLUTION_ACTIVITY_CLASS_NAME));
        intent.putExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_RESOLUTION_ACTION,
                resolutionAction);
        intent.putExtra(EuiccService.EXTRA_RESOLUTION_CALLING_PACKAGE, callingPackage);
        intent.putExtra(EuiccService.EXTRA_RESOLVABLE_ERRORS, resolvableErrors);
        intent.putExtra(EuiccService.EXTRA_RESOLUTION_CARD_ID, cardId);
        intent.putExtra(EuiccService.EXTRA_RESOLUTION_CONFIRMATION_CODE_RETRIED,
                confirmationCodeRetried);
        intent.putExtra(EXTRA_OPERATION, op);
        PendingIntent resolutionIntent = PendingIntent.getActivity(
                mContext,
                0 /* requestCode */,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_MUTABLE);
        extrasIntent.putExtra(
                EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_RESOLUTION_INTENT, resolutionIntent);
    }

    @Override
    public void dump(FileDescriptor fd, final PrintWriter pw, String[] args) {
        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.DUMP, "Requires DUMP");
        final long token = Binder.clearCallingIdentity();
        pw.println("===== BEGIN EUICC CLINIC =====");
        try {
            pw.println("===== EUICC CONNECTOR =====");
            mConnector.dump(fd, pw, args);
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            mConnector.dumpEuiccService(new EuiccConnector.DumpEuiccServiceCommandCallback() {
                @Override
                public void onDumpEuiccServiceComplete(String logs) {
                    pw.println("===== EUICC SERVICE =====");
                    pw.println(logs);
                    countDownLatch.countDown();
                }

                @Override
                public void onEuiccServiceUnavailable() {
                    pw.println("===== EUICC SERVICE UNAVAILABLE =====");
                    countDownLatch.countDown();
                }
            });

            // Wait up to 5 seconds
            if (!countDownLatch.await(EUICC_DUMP_TIME_OUT_SECONDS, TimeUnit.SECONDS)) {
                pw.println("===== EUICC SERVICE TIMEOUT =====");
            }
        } catch (InterruptedException e) {
            pw.println("===== EUICC SERVICE INTERRUPTED =====");
        } finally {
            pw.println("===== END EUICC CLINIC =====");
            Binder.restoreCallingIdentity(token);
        }
    }

    /**
     * Send broadcast {@link EuiccManager#ACTION_OTA_STATUS_CHANGED} for OTA status
     * changed.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public void sendOtaStatusChangedBroadcast() {
        Intent intent = new Intent(EuiccManager.ACTION_OTA_STATUS_CHANGED);
        ComponentInfo bestComponent = mConnector.findBestComponent(mContext.getPackageManager());
        if (bestComponent != null) {
            intent.setPackage(bestComponent.packageName);
        }
        mContext.sendBroadcast(intent, permission.WRITE_EMBEDDED_SUBSCRIPTIONS);
    }

    @Nullable
    private SubscriptionInfo getSubscriptionForSubscriptionId(int subscriptionId) {
        List<SubscriptionInfo> subs = mSubscriptionManager.getAvailableSubscriptionInfoList();
        int subCount = (subs != null) ? subs.size() : 0;
        for (int i = 0; i < subCount; i++) {
            SubscriptionInfo sub = subs.get(i);
            if (subscriptionId == sub.getSubscriptionId()) {
                return sub;
            }
        }
        return null;
    }

    @Nullable
    private String blockingGetEidFromEuiccService(int cardId) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> eidRef = new AtomicReference<>();
        mConnector.getEid(cardId, new EuiccConnector.GetEidCommandCallback() {
            @Override
            public void onGetEidComplete(String eid) {
                eidRef.set(eid);
                latch.countDown();
            }

            @Override
            public void onEuiccServiceUnavailable() {
                latch.countDown();
            }
        });
        return awaitResult(latch, eidRef);
    }

    private @OtaStatus int blockingGetOtaStatusFromEuiccService(int cardId) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Integer> statusRef =
                new AtomicReference<>(EuiccManager.EUICC_OTA_STATUS_UNAVAILABLE);
        mConnector.getOtaStatus(cardId, new EuiccConnector.GetOtaStatusCommandCallback() {
            @Override
            public void onGetOtaStatusComplete(@OtaStatus int status) {
                statusRef.set(status);
                latch.countDown();
            }

            @Override
            public void onEuiccServiceUnavailable() {
                latch.countDown();
            }
        });
        return awaitResult(latch, statusRef);
    }

    @Nullable
    private EuiccInfo blockingGetEuiccInfoFromEuiccService(int cardId) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<EuiccInfo> euiccInfoRef = new AtomicReference<>();
        mConnector.getEuiccInfo(cardId, new EuiccConnector.GetEuiccInfoCommandCallback() {
            @Override
            public void onGetEuiccInfoComplete(EuiccInfo euiccInfo) {
                euiccInfoRef.set(euiccInfo);
                latch.countDown();
            }

            @Override
            public void onEuiccServiceUnavailable() {
                latch.countDown();
            }
        });
        return awaitResult(latch, euiccInfoRef);
    }

    private static <T> T awaitResult(CountDownLatch latch, AtomicReference<T> resultRef) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return resultRef.get();
    }

    // Returns whether the caller has carrier privilege on the given subscription.
    private boolean checkCarrierPrivilegeInMetadata(DownloadableSubscription subscription,
            String callingPackage) {
        UiccAccessRule[] rules = null;
        List<UiccAccessRule> rulesList = subscription.getAccessRules();
        if (rulesList != null) {
            rules = rulesList.toArray(new UiccAccessRule[rulesList.size()]);
        }
        if (rules == null) {
            Log.e(TAG, "No access rules but caller is unprivileged");
            return false;
        }

        final PackageInfo info;
        try {
            info = mPackageManager.getPackageInfo(callingPackage,
                PackageManager.GET_SIGNING_CERTIFICATES);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Calling package valid but gone");
            return false;
        }

        for (int i = 0; i < rules.length; i++) {
            if (rules[i].getCarrierPrivilegeStatus(info)
                    == TelephonyManager.CARRIER_PRIVILEGE_STATUS_HAS_ACCESS) {
                Log.i(TAG, "Calling package has carrier privilege to this profile");
                return true;
            }
        }
        Log.e(TAG, "Calling package doesn't have carrier privilege to this profile");
        return false;
    }

    private boolean supportMultiActiveSlots() {
        return mTelephonyManager.getSupportedModemCount() > 1;
    }

    // Checks whether the caller can manage the active embedded subscription on the SIM with the
    // given cardId.
    private boolean canManageActiveSubscriptionOnTargetSim(int cardId, String callingPackage) {
        List<SubscriptionInfo> subInfoList = mSubscriptionManager
                .getActiveSubscriptionInfoList(/* userVisibleOnly */false);
        if (subInfoList == null || subInfoList.size() == 0) {
            // No active subscription on any SIM.
            return false;
        }
        for (SubscriptionInfo subInfo : subInfoList) {
            // If cardId == TelephonyManager.UNSUPPORTED_CARD_ID, we assume it does not support
            // multiple eSIMs. There are older multi-active SIM devices which do not implement HAL
            // 1.2 and if they have multiple eSIMs, we let it pass if the app can manage an active
            // subscription on any eSIM. That's the best we can do here.
            if ((cardId == TelephonyManager.UNSUPPORTED_CARD_ID || subInfo.getCardId() == cardId)
                    && subInfo.isEmbedded()
                    && mSubscriptionManager.canManageSubscription(subInfo, callingPackage)) {
                return true;
            }
        }
        return false;
    }

    // For a multi-active subscriptions phone, checks whether the caller can manage subscription on
    // the target SIM with the given cardId. The caller can only manage subscription on the target
    // SIM if it can manage the active subscription on the target SIM or there is no active
    // subscription on the target SIM, and the caller can manage any active subscription on any
    // other SIM. The target SIM should be an eUICC.
    // For a single-active subscription phone, checks whether the caller can manage any active
    // embedded subscription.
    private boolean canManageSubscriptionOnTargetSim(int cardId, String callingPackage) {
        List<SubscriptionInfo> subInfoList = mSubscriptionManager
                .getActiveSubscriptionInfoList(false /* userVisibleonly */);
        // No active subscription on any SIM.
        if (subInfoList == null || subInfoList.size() == 0) {
            return false;
        }
        // If it's a multi-active SIM device, we assume it's above HAL 1.2 which supports cardId.
        // There are older multi-active SIM devices but don't implement HAL 1.2. In this case,
        // platform can't even detect UiccCardInfo#isEuicc as true for eSIM, which won't let the
        // below check pass. That's the best we can do here.
        if (supportMultiActiveSlots()) {
            // The target card should be an eUICC.
            List<UiccCardInfo> cardInfos = mTelephonyManager.getUiccCardsInfo();
            if (cardInfos == null || cardInfos.isEmpty()) {
                return false;
            }
            boolean isEuicc = false;
            for (UiccCardInfo info : cardInfos) {
                if (info != null && info.getCardId() == cardId && info.isEuicc()) {
                    isEuicc = true;
                    break;
                }
            }
            if (!isEuicc) {
                Log.i(TAG, "The target SIM is not an eUICC.");
                return false;
            }

            // If the caller can't manage the active embedded subscription on the target SIM, return
            // false. If the caller can manage the active embedded subscription on the target SIM,
            // return true directly.
            for (SubscriptionInfo subInfo : subInfoList) {
                // subInfo.isEmbedded() can only be true for the target SIM.
                if (subInfo.isEmbedded() && subInfo.getCardId() == cardId) {
                    return mSubscriptionManager.canManageSubscription(subInfo, callingPackage);
                }
            }

            // There is no active subscription on the target SIM, checks whether the caller can
            // manage any active subscription on any other SIM.
            final long token = Binder.clearCallingIdentity();
            try {
                return mTelephonyManager.checkCarrierPrivilegesForPackageAnyPhone(callingPackage)
                    == TelephonyManager.CARRIER_PRIVILEGE_STATUS_HAS_ACCESS;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        } else {
            for (SubscriptionInfo subInfo : subInfoList) {
                if (subInfo.isEmbedded()
                        && mSubscriptionManager.canManageSubscription(subInfo, callingPackage)) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean callerCanReadPhoneStatePrivileged() {
        return mContext.checkCallingOrSelfPermission(
                Manifest.permission.READ_PRIVILEGED_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean callerCanWriteEmbeddedSubscriptions() {
        return mContext.checkCallingOrSelfPermission(
                Manifest.permission.WRITE_EMBEDDED_SUBSCRIPTIONS)
                == PackageManager.PERMISSION_GRANTED;
    }
}
