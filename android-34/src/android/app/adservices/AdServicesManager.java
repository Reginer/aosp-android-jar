/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.app.adservices;

import static android.adservices.common.AdServicesPermissions.ACCESS_ADSERVICES_MANAGER;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.app.adservices.consent.ConsentParcel;
import android.app.adservices.topics.TopicParcel;
import android.app.sdksandbox.SdkSandboxManager;
import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Objects;

/**
 * AdServices Manager to handle the internal communication between PPAPI process and AdServices
 * System Service.
 *
 * @hide
 */
// TODO(b/269798827): Enable for R.
@RequiresApi(Build.VERSION_CODES.S)
public final class AdServicesManager {
    @GuardedBy("SINGLETON_LOCK")
    private static AdServicesManager sSingleton;

    private final IAdServicesManager mService;
    private static final Object SINGLETON_LOCK = new Object();

    @IntDef(value = {MEASUREMENT_DELETION})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DeletionApiType {}

    public static final int MEASUREMENT_DELETION = 0;

    // TODO(b/267789077): Create bit for other APIs.

    @VisibleForTesting
    public AdServicesManager(@NonNull IAdServicesManager iAdServicesManager) {
        Objects.requireNonNull(iAdServicesManager, "AdServicesManager is NULL!");
        mService = iAdServicesManager;
    }

    /** Get the singleton of AdServicesManager. Only used on T+ */
    @Nullable
    public static AdServicesManager getInstance(@NonNull Context context) {
        synchronized (SINGLETON_LOCK) {
            if (sSingleton == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // TODO(b/262282035): Fix this work around in U+.
                // Get the AdServicesManagerService's Binder from the SdkSandboxManager.
                // This is a workaround for b/262282035.
                IBinder iBinder =
                        context.getSystemService(SdkSandboxManager.class).getAdServicesManager();
                sSingleton = new AdServicesManager(IAdServicesManager.Stub.asInterface(iBinder));
            }
        }
        return sSingleton;
    }

    /** Return the User Consent */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public ConsentParcel getConsent(@ConsentParcel.ConsentApiType int consentApiType) {
        try {
            return mService.getConsent(consentApiType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Set the User Consent */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void setConsent(@NonNull ConsentParcel consentParcel) {
        Objects.requireNonNull(consentParcel);
        try {
            mService.setConsent(consentParcel);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Saves information to the storage that notification was displayed for the first time to the
     * user.
     */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void recordNotificationDisplayed() {
        try {
            mService.recordNotificationDisplayed();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns information whether Consent Notification was displayed or not.
     *
     * @return true if Consent Notification was displayed, otherwise false.
     */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public boolean wasNotificationDisplayed() {
        try {
            return mService.wasNotificationDisplayed();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Saves information to the storage that notification was displayed for the first time to the
     * user.
     */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void recordGaUxNotificationDisplayed() {
        try {
            mService.recordGaUxNotificationDisplayed();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns information whether user interacted with consent manually.
     *
     * @return
     *     <ul>
     *       <li>-1 when no manual interaction was recorded
     *       <li>0 when no data about interaction (similar to null)
     *       <li>1 when manual interaction was recorded
     *     </ul>
     */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public int getUserManualInteractionWithConsent() {
        try {
            return mService.getUserManualInteractionWithConsent();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Saves information to the storage that user interacted with consent manually. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void recordUserManualInteractionWithConsent(int interaction) {
        try {
            mService.recordUserManualInteractionWithConsent(interaction);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns information whether Consent GA UX Notification was displayed or not.
     *
     * @return true if Consent GA UX Notification was displayed, otherwise false.
     */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public boolean wasGaUxNotificationDisplayed() {
        try {
            return mService.wasGaUxNotificationDisplayed();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Record a blocked topic.
     *
     * @param blockedTopicParcels the blocked topic to record
     */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void recordBlockedTopic(@NonNull List<TopicParcel> blockedTopicParcels) {
        try {
            mService.recordBlockedTopic(blockedTopicParcels);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Remove a blocked topic.
     *
     * @param blockedTopicParcel the blocked topic to remove
     */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void removeBlockedTopic(@NonNull TopicParcel blockedTopicParcel) {
        try {
            mService.removeBlockedTopic(blockedTopicParcel);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get all blocked topics.
     *
     * @return a {@code List} of all blocked topics.
     */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public List<TopicParcel> retrieveAllBlockedTopics() {
        try {
            return mService.retrieveAllBlockedTopics();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Clear all Blocked Topics */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void clearAllBlockedTopics() {
        try {
            mService.clearAllBlockedTopics();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Returns the list of apps with consent. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public List<String> getKnownAppsWithConsent(List<String> installedPackages) {
        try {
            return mService.getKnownAppsWithConsent(installedPackages);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Returns the list of apps with revoked consent. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public List<String> getAppsWithRevokedConsent(List<String> installedPackages) {
        try {
            return mService.getAppsWithRevokedConsent(installedPackages);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Set user consent for an app */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void setConsentForApp(String packageName, int packageUid, boolean isConsentRevoked) {
        try {
            mService.setConsentForApp(packageName, packageUid, isConsentRevoked);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Reset all apps and blocked apps. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void clearKnownAppsWithConsent() {
        try {
            mService.clearKnownAppsWithConsent();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Reset all apps consent. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void clearAllAppConsentData() {
        try {
            mService.clearAllAppConsentData();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get if user consent is revoked for a given app.
     *
     * @return {@code true} if the user consent was revoked.
     */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public boolean isConsentRevokedForApp(String packageName, int packageUid) {
        try {
            return mService.isConsentRevokedForApp(packageName, packageUid);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Set user consent if the app first time request access and/or return consent value for the
     * app.
     *
     * @return {@code true} if user consent was given.
     */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public boolean setConsentForAppIfNew(
            String packageName, int packageUid, boolean isConsentRevoked) {
        try {
            return mService.setConsentForAppIfNew(packageName, packageUid, isConsentRevoked);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Clear the app consent entry for uninstalled app. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void clearConsentForUninstalledApp(String packageName, int packageUid) {
        try {
            mService.clearConsentForUninstalledApp(packageName, packageUid);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Saves information to the storage that a deletion of measurement data occurred. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void recordAdServicesDeletionOccurred(@DeletionApiType int deletionType) {
        try {
            mService.recordAdServicesDeletionOccurred(deletionType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Saves the PP API default consent of a user. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void recordDefaultConsent(boolean defaultConsent) {
        try {
            mService.recordDefaultConsent(defaultConsent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Saves the topics default consent of a user. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void recordTopicsDefaultConsent(boolean defaultConsent) {
        try {
            mService.recordTopicsDefaultConsent(defaultConsent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Saves the FLEDGE default consent of a user. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void recordFledgeDefaultConsent(boolean defaultConsent) {
        try {
            mService.recordFledgeDefaultConsent(defaultConsent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Saves the measurement default consent of a user. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void recordMeasurementDefaultConsent(boolean defaultConsent) {
        try {
            mService.recordMeasurementDefaultConsent(defaultConsent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Saves the default AdId state of a user. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void recordDefaultAdIdState(boolean defaultAdIdState) {
        try {
            mService.recordDefaultAdIdState(defaultAdIdState);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Checks whether the AdServices module needs to handle data reconciliation after a rollback.
     */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public boolean needsToHandleRollbackReconciliation(@DeletionApiType int deletionType) {
        try {
            return mService.needsToHandleRollbackReconciliation(deletionType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the PP API default consent of a user.
     *
     * @return true if the PP API default consent is given, false otherwise.
     */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public boolean getDefaultConsent() {
        try {
            return mService.getDefaultConsent();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the topics default consent of a user.
     *
     * @return true if the topics default consent is given, false otherwise.
     */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public boolean getTopicsDefaultConsent() {
        try {
            return mService.getTopicsDefaultConsent();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the FLEDGE default consent of a user.
     *
     * @return true if the FLEDGE default consent is given, false otherwise.
     */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public boolean getFledgeDefaultConsent() {
        try {
            return mService.getFledgeDefaultConsent();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the measurement default consent of a user.
     *
     * @return true if the measurement default consent is given, false otherwise.
     */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public boolean getMeasurementDefaultConsent() {
        try {
            return mService.getMeasurementDefaultConsent();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the default AdId state of a user.
     *
     * @return true if the default AdId State is enabled, false otherwise.
     */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public boolean getDefaultAdIdState() {
        try {
            return mService.getDefaultAdIdState();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Returns the current privacy sandbox feature. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public String getCurrentPrivacySandboxFeature() {
        try {
            return mService.getCurrentPrivacySandboxFeature();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Set the current privacy sandbox feature. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void setCurrentPrivacySandboxFeature(String featureType) {
        try {
            mService.setCurrentPrivacySandboxFeature(featureType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
