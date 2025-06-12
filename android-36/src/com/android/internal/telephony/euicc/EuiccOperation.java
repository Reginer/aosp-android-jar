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

import android.annotation.IntDef;
import android.annotation.Nullable;
import android.app.PendingIntent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.euicc.EuiccService;
import android.telephony.TelephonyManager;
import android.telephony.euicc.DownloadableSubscription;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Representation of an {@link EuiccController} operation which failed with a resolvable error.
 *
 * <p>This class tracks the operation which failed and the reason for failure. Once the error is
 * resolved, the operation can be resumed with {@link #continueOperation}.
 */
@VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
public class EuiccOperation implements Parcelable {
    private static final String TAG = "EuiccOperation";

    public static final Creator<EuiccOperation> CREATOR = new Creator<EuiccOperation>() {
        @Override
        public EuiccOperation createFromParcel(Parcel in) {
            return new EuiccOperation(in);
        }

        @Override
        public EuiccOperation[] newArray(int size) {
            return new EuiccOperation[size];
        }
    };

    @VisibleForTesting
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            ACTION_GET_METADATA_DEACTIVATE_SIM,
            ACTION_DOWNLOAD_DEACTIVATE_SIM,
            ACTION_DOWNLOAD_NO_PRIVILEGES,
            ACTION_GET_DEFAULT_LIST_DEACTIVATE_SIM,
            ACTION_SWITCH_DEACTIVATE_SIM,
            ACTION_SWITCH_NO_PRIVILEGES,
            ACTION_DOWNLOAD_RESOLVABLE_ERRORS,
    })
    @interface Action {}

    @VisibleForTesting
    static final int ACTION_GET_METADATA_DEACTIVATE_SIM = 1;
    @VisibleForTesting
    static final int ACTION_DOWNLOAD_DEACTIVATE_SIM = 2;
    @VisibleForTesting
    static final int ACTION_DOWNLOAD_NO_PRIVILEGES = 3;
    @VisibleForTesting
    static final int ACTION_GET_DEFAULT_LIST_DEACTIVATE_SIM = 4;
    @VisibleForTesting
    static final int ACTION_SWITCH_DEACTIVATE_SIM = 5;
    @VisibleForTesting
    static final int ACTION_SWITCH_NO_PRIVILEGES = 6;
    @VisibleForTesting
    static final int ACTION_DOWNLOAD_RESOLVABLE_ERRORS = 7;
    /**
     * @deprecated Use ACTION_DOWNLOAD_RESOLVABLE_ERRORS and pass the resolvable errors in bit map.
     */
    @VisibleForTesting
    @Deprecated
    static final int ACTION_DOWNLOAD_CONFIRMATION_CODE = 8;
    /**
     * ACTION_DOWNLOAD_CHECK_METADATA can be used for either NO_PRIVILEGES or DEACTIVATE_SIM.
     */
    @VisibleForTesting
    static final int ACTION_DOWNLOAD_NO_PRIVILEGES_OR_DEACTIVATE_SIM_CHECK_METADATA = 9;

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public final @Action int mAction;

    private final long mCallingToken;

    @Nullable
    private final DownloadableSubscription mDownloadableSubscription;
    private final int mSubscriptionId;
    private final boolean mSwitchAfterDownload;
    @Nullable
    private final String mCallingPackage;
    @Nullable
    private final int mResolvableErrors;

    /**
     * {@link EuiccManager#getDownloadableSubscriptionMetadata} failed with
     * {@link EuiccService#RESULT_MUST_DEACTIVATE_SIM}.
     */
    static EuiccOperation forGetMetadataDeactivateSim(long callingToken,
            DownloadableSubscription subscription, String callingPackage) {
        return new EuiccOperation(ACTION_GET_METADATA_DEACTIVATE_SIM, callingToken,
                subscription, 0 /* subscriptionId */, false /* switchAfterDownload */,
                callingPackage);
    }

    /**
     * {@link EuiccManager#downloadSubscription} failed with a mustDeactivateSim error. Should only
     * be used for privileged callers; for unprivileged callers, use
     * {@link #forDownloadNoPrivileges} to avoid a double prompt.
     */
    static EuiccOperation forDownloadDeactivateSim(long callingToken,
            DownloadableSubscription subscription, boolean switchAfterDownload,
            String callingPackage) {
        return new EuiccOperation(ACTION_DOWNLOAD_DEACTIVATE_SIM, callingToken,
                subscription,  0 /* subscriptionId */, switchAfterDownload, callingPackage);
    }

    /**
     * {@link EuiccManager#downloadSubscription} failed because the calling app does not have
     * permission to manage the current active subscription.
     */
    static EuiccOperation forDownloadNoPrivileges(long callingToken,
            DownloadableSubscription subscription, boolean switchAfterDownload,
            String callingPackage) {
        return new EuiccOperation(ACTION_DOWNLOAD_NO_PRIVILEGES, callingToken,
                subscription,  0 /* subscriptionId */, switchAfterDownload, callingPackage);
    }

    /**
     * {@link EuiccManager#downloadSubscription} failed because the caller can't manage the target
     * SIM, or we cannot determine the privileges without deactivating the current SIM first.
     */
    static EuiccOperation forDownloadNoPrivilegesOrDeactivateSimCheckMetadata(long callingToken,
            DownloadableSubscription subscription, boolean switchAfterDownload,
            String callingPackage) {
        return new EuiccOperation(ACTION_DOWNLOAD_NO_PRIVILEGES_OR_DEACTIVATE_SIM_CHECK_METADATA,
                callingToken, subscription,  0 /* subscriptionId */,
                switchAfterDownload, callingPackage);
    }

    /**
     * {@link EuiccManager#downloadSubscription} failed with
     * {@link EuiccService#RESULT_NEED_CONFIRMATION_CODE} error.
     *
     * @deprecated Use
     * {@link #forDownloadResolvableErrors(long, DownloadableSubscription, boolean, String, int)}
     * instead.
     */
    @Deprecated
    public static EuiccOperation forDownloadConfirmationCode(long callingToken,
            DownloadableSubscription subscription, boolean switchAfterDownload,
            String callingPackage) {
        return new EuiccOperation(ACTION_DOWNLOAD_CONFIRMATION_CODE, callingToken,
            subscription, 0 /* subscriptionId */, switchAfterDownload, callingPackage);
    }

    /**
     * {@link EuiccManager#downloadSubscription} failed with
     * {@link EuiccService#RESULT_RESOLVABLE_ERRORS} error.
     */
    static EuiccOperation forDownloadResolvableErrors(long callingToken,
            DownloadableSubscription subscription, boolean switchAfterDownload,
            String callingPackage, int resolvableErrors) {
        return new EuiccOperation(ACTION_DOWNLOAD_RESOLVABLE_ERRORS, callingToken,
                subscription, 0 /* subscriptionId */, switchAfterDownload,
                callingPackage, resolvableErrors);
    }

    static EuiccOperation forGetDefaultListDeactivateSim(long callingToken, String callingPackage) {
        return new EuiccOperation(ACTION_GET_DEFAULT_LIST_DEACTIVATE_SIM, callingToken,
                null /* downloadableSubscription */, 0 /* subscriptionId */,
                false /* switchAfterDownload */, callingPackage);
    }

    static EuiccOperation forSwitchDeactivateSim(long callingToken, int subscriptionId,
            String callingPackage) {
        return new EuiccOperation(ACTION_SWITCH_DEACTIVATE_SIM, callingToken,
                null /* downloadableSubscription */, subscriptionId,
                false /* switchAfterDownload */, callingPackage);
    }

    static EuiccOperation forSwitchNoPrivileges(long callingToken, int subscriptionId,
            String callingPackage) {
        return new EuiccOperation(ACTION_SWITCH_NO_PRIVILEGES, callingToken,
                null /* downloadableSubscription */, subscriptionId,
                false /* switchAfterDownload */, callingPackage);
    }

    EuiccOperation(@Action int action,
            long callingToken,
            @Nullable DownloadableSubscription downloadableSubscription,
            int subscriptionId,
            boolean switchAfterDownload,
            String callingPackage,
            int resolvableErrors) {
        mAction = action;
        mCallingToken = callingToken;
        mDownloadableSubscription = downloadableSubscription;
        mSubscriptionId = subscriptionId;
        mSwitchAfterDownload = switchAfterDownload;
        mCallingPackage = callingPackage;
        mResolvableErrors = resolvableErrors;
    }

    EuiccOperation(@Action int action,
            long callingToken,
            @Nullable DownloadableSubscription downloadableSubscription,
            int subscriptionId,
            boolean switchAfterDownload,
            String callingPackage) {
        mAction = action;
        mCallingToken = callingToken;
        mDownloadableSubscription = downloadableSubscription;
        mSubscriptionId = subscriptionId;
        mSwitchAfterDownload = switchAfterDownload;
        mCallingPackage = callingPackage;
        mResolvableErrors = 0;
    }

    EuiccOperation(Parcel in) {
        mAction = in.readInt();
        mCallingToken = in.readLong();
        mDownloadableSubscription = in.readTypedObject(DownloadableSubscription.CREATOR);
        mSubscriptionId = in.readInt();
        mSwitchAfterDownload = in.readBoolean();
        mCallingPackage = in.readString();
        mResolvableErrors = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mAction);
        dest.writeLong(mCallingToken);
        dest.writeTypedObject(mDownloadableSubscription, flags);
        dest.writeInt(mSubscriptionId);
        dest.writeBoolean(mSwitchAfterDownload);
        dest.writeString(mCallingPackage);
        dest.writeInt(mResolvableErrors);
    }

    /**
     * Resume this operation based on the results of the resolution activity.
     *
     * @param resolutionExtras The resolution extras as provided to
     *     {@link EuiccManager#continueOperation}.
     * @param callbackIntent The callback intent to trigger after the operation completes.
     */
    public void continueOperation(int cardId, Bundle resolutionExtras,
            PendingIntent callbackIntent) {
        // Restore the identity of the caller. We should err on the side of caution and redo any
        // permission checks before continuing with the operation in case the caller state has
        // changed. Resolution flows can re-clear the identity if required.
        Binder.restoreCallingIdentity(mCallingToken);

        switch (mAction) {
            case ACTION_GET_METADATA_DEACTIVATE_SIM:
                resolvedGetMetadataDeactivateSim(cardId,
                        resolutionExtras.getBoolean(EuiccService.EXTRA_RESOLUTION_CONSENT),
                        callbackIntent);
                break;
            case ACTION_DOWNLOAD_DEACTIVATE_SIM:
                resolvedDownloadDeactivateSim(cardId,
                        resolutionExtras.getInt(EuiccService.EXTRA_RESOLUTION_PORT_INDEX,
                                TelephonyManager.DEFAULT_PORT_INDEX),
                        resolutionExtras.getBoolean(EuiccService.EXTRA_RESOLUTION_CONSENT),
                        callbackIntent);
                break;
            case ACTION_DOWNLOAD_NO_PRIVILEGES:
                resolvedDownloadNoPrivileges(cardId,
                        resolutionExtras.getBoolean(EuiccService.EXTRA_RESOLUTION_CONSENT),
                        callbackIntent);
                break;
            case ACTION_DOWNLOAD_NO_PRIVILEGES_OR_DEACTIVATE_SIM_CHECK_METADATA:
                resolvedDownloadNoPrivilegesOrDeactivateSimCheckMetadata(cardId,
                        resolutionExtras.getInt(EuiccService.EXTRA_RESOLUTION_PORT_INDEX,
                                TelephonyManager.DEFAULT_PORT_INDEX),
                        resolutionExtras.getBoolean(EuiccService.EXTRA_RESOLUTION_CONSENT),
                        callbackIntent);
                break;
            case ACTION_DOWNLOAD_CONFIRMATION_CODE: // Deprecated case
                resolvedDownloadConfirmationCode(cardId,
                        resolutionExtras.getString(EuiccService.EXTRA_RESOLUTION_CONFIRMATION_CODE),
                        callbackIntent);
                break;
            case ACTION_DOWNLOAD_RESOLVABLE_ERRORS:
                resolvedDownloadResolvableErrors(cardId, resolutionExtras, callbackIntent);
                break;
            case ACTION_GET_DEFAULT_LIST_DEACTIVATE_SIM:
                resolvedGetDefaultListDeactivateSim(cardId,
                        resolutionExtras.getBoolean(EuiccService.EXTRA_RESOLUTION_CONSENT),
                        callbackIntent);
                break;
            case ACTION_SWITCH_DEACTIVATE_SIM: {
                // get portIndex from original operation
                final int portIndex = resolutionExtras.getInt(
                        EuiccService.EXTRA_RESOLUTION_PORT_INDEX,
                        0);
                // get whether legacy API was called from original operation
                final boolean usePortIndex = resolutionExtras.getBoolean(
                        EuiccService.EXTRA_RESOLUTION_USE_PORT_INDEX,
                        false);
                resolvedSwitchDeactivateSim(cardId, portIndex,
                        resolutionExtras.getBoolean(EuiccService.EXTRA_RESOLUTION_CONSENT),
                        callbackIntent, usePortIndex);
                break;
            }
            case ACTION_SWITCH_NO_PRIVILEGES: {
                // get portIndex from original operation
                final int portIndex = resolutionExtras.getInt(
                        EuiccService.EXTRA_RESOLUTION_PORT_INDEX,
                        0);
                // get whether port index was passed in from original operation
                final boolean usePortIndex = resolutionExtras.getBoolean(
                        EuiccService.EXTRA_RESOLUTION_USE_PORT_INDEX,
                        false);
                resolvedSwitchNoPrivileges(cardId, portIndex,
                        resolutionExtras.getBoolean(EuiccService.EXTRA_RESOLUTION_CONSENT),
                        callbackIntent, usePortIndex);
                break;
            }
            default:
                Log.wtf(TAG, "Unknown action: " + mAction);
                break;
        }
    }

    private void resolvedGetMetadataDeactivateSim(int cardId, boolean consent,
            PendingIntent callbackIntent) {
        if (consent) {
            // User has consented; perform the lookup, but this time, tell the LPA to deactivate any
            // required active SIMs.
            EuiccController.get().getDownloadableSubscriptionMetadata(
                    cardId,
                    mDownloadableSubscription,
                    true /* forceDeactivateSim */,
                    mCallingPackage,
                    callbackIntent);
        } else {
            // User has not consented; fail the operation.
            fail(callbackIntent);
        }
    }

    private void resolvedDownloadDeactivateSim(int cardId, int portIndex, boolean consent,
            PendingIntent callbackIntent) {
        if (consent) {
            // User has consented; perform the download, but this time, tell the LPA to deactivate
            // any required active SIMs.
            EuiccController.get().downloadSubscription(
                    cardId,
                    portIndex,
                    mDownloadableSubscription,
                    mSwitchAfterDownload,
                    mCallingPackage,
                    true /* forceDeactivateSim */,
                    null /* resolvedBundle */,
                    callbackIntent);
        } else {
            // User has not consented; fail the operation.
            fail(callbackIntent);
        }
    }

    private void resolvedDownloadNoPrivileges(int cardId, boolean consent,
            PendingIntent callbackIntent) {
        if (consent) {
            // User has consented; perform the download with full privileges.
            long token = Binder.clearCallingIdentity();
            try {
                // Note: We turn on "forceDeactivateSim" here under the assumption that the
                // privilege prompt should also cover permission to deactivate an active SIM, as
                // the privilege prompt makes it clear that we're switching from the current
                // carrier.
                // Action {@link #ACTION_DOWNLOAD_NO_PRIVILEGES} is no more used in platform,this
                // method will never get called, pass {@link TelephonyManager#DEFAULT_PORT_INDEX}
                // as portIndex.
                EuiccController.get().downloadSubscriptionPrivileged(
                        cardId,
                        TelephonyManager.DEFAULT_PORT_INDEX,
                        token,
                        mDownloadableSubscription,
                        mSwitchAfterDownload,
                        true /* forceDeactivateSim */,
                        mCallingPackage,
                        null /* resolvedBundle */,
                        callbackIntent);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        } else {
            // User has not consented; fail the operation.
            fail(callbackIntent);
        }
    }

    private void resolvedDownloadNoPrivilegesOrDeactivateSimCheckMetadata(int cardId,
            int portIndex, boolean consent, PendingIntent callbackIntent) {
        if (consent) {
            // User has consented; perform the download with full privileges.
            long token = Binder.clearCallingIdentity();
            try {
                // Note: We turn on "forceDeactivateSim" here under the assumption that the
                // privilege prompt should also cover permission to deactivate an active SIM, as
                // the privilege prompt makes it clear that we're switching from the current
                // carrier.
                EuiccController.get().downloadSubscriptionPrivilegedCheckMetadata(
                        cardId,
                        portIndex,
                        token,
                        mDownloadableSubscription,
                        mSwitchAfterDownload,
                        true /* forceDeactivateSim */,
                        mCallingPackage,
                        null /* resolvedBundle */,
                        callbackIntent);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        } else {
            // User has not consented; fail the operation.
            fail(callbackIntent);
        }
    }

    /**
     * @deprecated The resolvable errors in download step are solved by
     * {@link #resolvedDownloadResolvableErrors(Bundle, PendingIntent)} from Q.
     */
    @Deprecated
    private void resolvedDownloadConfirmationCode(int cardId, String confirmationCode,
            PendingIntent callbackIntent) {
        if (TextUtils.isEmpty(confirmationCode)) {
            fail(callbackIntent);
        } else {
            mDownloadableSubscription.setConfirmationCode(confirmationCode);
            // Action {@link #ACTION_DOWNLOAD_CONFIRMATION_CODE} is not any more used from LPA with
            // targetSDK >=Q, pass {@link TelephonyManager#DEFAULT_PORT_INDEX} as portIndex.
            EuiccController.get().downloadSubscription(
                    cardId,
                    TelephonyManager.DEFAULT_PORT_INDEX,
                    mDownloadableSubscription,
                    mSwitchAfterDownload,
                    mCallingPackage,
                    true /* forceDeactivateSim */,
                    null,
                    callbackIntent);
        }
    }

    private void resolvedDownloadResolvableErrors(int cardId, Bundle resolvedBundle,
            PendingIntent callbackIntent) {
        boolean pass = true;
        String confirmationCode = null;
        if ((mResolvableErrors & EuiccService.RESOLVABLE_ERROR_POLICY_RULES) != 0) {
            if (!resolvedBundle.getBoolean(EuiccService.EXTRA_RESOLUTION_ALLOW_POLICY_RULES)) {
                pass = false;
            }
        }
        if ((mResolvableErrors & EuiccService.RESOLVABLE_ERROR_CONFIRMATION_CODE) != 0) {
            confirmationCode = resolvedBundle.getString(
                EuiccService.EXTRA_RESOLUTION_CONFIRMATION_CODE);
            // The check here just makes sure the entered confirmation code is non-empty. The actual
            // check to valid the confirmation code is done by LPA on the ensuing download attemp.
            if (TextUtils.isEmpty(confirmationCode)) {
                pass = false;
            }
        }

        if (!pass) {
            fail(callbackIntent);
        } else {
            mDownloadableSubscription.setConfirmationCode(confirmationCode);
            EuiccController.get().downloadSubscription(
                    cardId,
                    resolvedBundle.getInt(EuiccService.EXTRA_RESOLUTION_PORT_INDEX,
                            TelephonyManager.DEFAULT_PORT_INDEX),
                    mDownloadableSubscription,
                    mSwitchAfterDownload,
                    mCallingPackage,
                    true /* forceDeactivateSim */,
                    resolvedBundle,
                    callbackIntent);
        }
    }

    private void resolvedGetDefaultListDeactivateSim(int cardId, boolean consent,
            PendingIntent callbackIntent) {
        if (consent) {
            // User has consented; perform the lookup, but this time, tell the LPA to deactivate any
            // required active SIMs.
            EuiccController.get().getDefaultDownloadableSubscriptionList(
                    cardId,
                    true /* forceDeactivateSim */,
                    mCallingPackage,
                    callbackIntent);
        } else {
            // User has not consented; fail the operation.
            fail(callbackIntent);
        }
    }

    private void resolvedSwitchDeactivateSim(int cardId, int portIndex, boolean consent,
            PendingIntent callbackIntent, boolean usePortIndex) {
        if (consent) {
            // User has consented; perform the switch, but this time, tell the LPA to deactivate any
            // required active SIMs.
            EuiccController euiccController = EuiccController.get();
            euiccController.switchToSubscription(
                    cardId,
                    mSubscriptionId,
                    portIndex,
                    true /* forceDeactivateSim */,
                    mCallingPackage,
                    callbackIntent,
                    usePortIndex);
        } else {
            // User has not consented; fail the operation.
            fail(callbackIntent);
        }
    }

    private void resolvedSwitchNoPrivileges(int cardId, int portIndex, boolean consent,
            PendingIntent callbackIntent, boolean usePortIndex) {
        if (consent) {
            // User has consented; perform the switch with full privileges.
            long token = Binder.clearCallingIdentity();
            try {
                // Note: We turn on "forceDeactivateSim" here under the assumption that the
                // privilege prompt should also cover permission to deactivate an active SIM, as
                // the privilege prompt makes it clear that we're switching from the current
                // carrier. Also note that in practice, we'd need to deactivate the active SIM to
                // even reach this point, because we cannot fetch the metadata needed to check the
                // privileges without doing so.
                EuiccController euiccController = EuiccController.get();
                euiccController.switchToSubscriptionPrivileged(
                        cardId,
                        portIndex,
                        token,
                        mSubscriptionId,
                        true /* forceDeactivateSim */,
                        mCallingPackage,
                        callbackIntent,
                        usePortIndex);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        } else {
            // User has not consented; fail the operation.
            fail(callbackIntent);
        }
    }

    private static void fail(PendingIntent callbackIntent) {
        EuiccController.get().sendResult(
                callbackIntent,
                EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR,
                null /* extrasIntent */);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
