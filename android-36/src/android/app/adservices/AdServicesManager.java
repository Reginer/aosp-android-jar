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

import static android.Manifest.permission.DUMP;
import static android.adservices.common.AdServicesPermissions.ACCESS_ADSERVICES_MANAGER;

import android.annotation.IntDef;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.app.adservices.consent.ConsentParcel;
import android.app.adservices.topics.TopicParcel;
import android.app.sdksandbox.SdkSandboxManager;
import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

import com.android.adservices.LogUtil;
import com.android.internal.annotations.GuardedBy;

import java.io.PrintWriter;
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
@RequiresApi(Build.VERSION_CODES.S)
public final class AdServicesManager {

    public static final String AD_SERVICES_SYSTEM_SERVICE = "adservices_manager";

    private static final Object SINGLETON_LOCK = new Object();

    // TODO(b/366313883): get rid of this reference (and remove the Context arg from getInstance())
    @SuppressLint("StaticFieldLeak")
    @GuardedBy("SINGLETON_LOCK")
    private static Context sContext;

    @GuardedBy("SINGLETON_LOCK")
    private static AdServicesManager sSingleton;

    private final IAdServicesManager mService;

    @IntDef(value = {MEASUREMENT_DELETION})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DeletionApiType {}

    public static final int MEASUREMENT_DELETION = 0;

    // TODO(b/267789077): Create bit for other APIs.

    public AdServicesManager(IAdServicesManager service) {
        mService = Objects.requireNonNull(service, "IAdServicesManager cannot be null!");
    }

    // TODO(b/366313883): remove context once it's moved to AdServices service
    // code (so it can use ApplicationContextSingleton)
    /**
     * Gets the singleton instance.
     *
     * @param context (global) application context
     * @return the singleton, or {@code null} when called on T- devices
     * @throws IllegalArgumentException if called with a {@code context} that's not the same used to
     *     lazy load the singleton in the first call.
     */
    @Nullable
    public static AdServicesManager getInstance(Context context) {
        Objects.requireNonNull(context, "context cannot be null");

        synchronized (SINGLETON_LOCK) {
            if (sContext == null) {
                LogUtil.i("Setting AdServicesManager static context as %s", context);
                sContext = context;
            } else if (sContext != context) {
                throw new IllegalArgumentException(
                        "getInstance(" + context + "): context already set as " + sContext);
            }
            if (sSingleton == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    // Get the AdServicesManagerService's Binder from the SdkSandboxManager.
                    // This is a workaround for bug 262282035 that's only needed on TM - there is
                    // a CTS test that guarantees the service is published on UDC+
                    // (AdServicesJUnit4DeviceTest#testBinderServiceIsPublished, from
                    // CtsAdServicesDeviceTestCases)
                    LogUtil.d(
                            "AdServicesManager.getInstance(): getting binder from SdkSandboxManager"
                                    + " on TM");
                    IBinder iBinder =
                            context.getSystemService(SdkSandboxManager.class)
                                    .getAdServicesManager();

                    sSingleton =
                            new AdServicesManager(IAdServicesManager.Stub.asInterface(iBinder));
                } else {
                    LogUtil.d(
                            "AdServicesManager.getInstance(): getting binder from AdServicesManager"
                                    + " on UDC+");
                    sSingleton = context.getSystemService(AdServicesManager.class);
                }
                LogUtil.v("AdServicesManager.getInstance(): singleton set as %s", sSingleton);
            }
            return sSingleton;
        }
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

    /** Return the nullable User Consent */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public ConsentParcel getConsentNullable(@ConsentParcel.ConsentApiType int consentApiType) {
        try {
            return mService.getConsentNullable(consentApiType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Set the User Consent */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void setConsent(ConsentParcel consentParcel) {
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
    public void recordNotificationDisplayed(boolean wasNotificationDisplayed) {
        try {
            mService.recordNotificationDisplayed(wasNotificationDisplayed);
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
    public void recordGaUxNotificationDisplayed(boolean wasNotificationDisplayed) {
        try {
            mService.recordGaUxNotificationDisplayed(wasNotificationDisplayed);
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
     * Returns information whether Consent PAS Notification was displayed or not.
     *
     * @return true if PAS Notification was displayed, otherwise false.
     */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public boolean wasPasNotificationDisplayed() {
        try {
            return mService.wasPasNotificationDisplayed();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Saves information to the storage that PAS notification was displayed for the first time to
     * the user.
     */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void recordPasNotificationDisplayed(boolean wasNotificationDisplayed) {
        try {
            mService.recordPasNotificationDisplayed(wasNotificationDisplayed);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns information whether Consent PAS Notification was opened or not.
     *
     * @return true if PAS Notification was opened, otherwise false.
     */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public boolean wasPasNotificationOpened() {
        try {
            return mService.wasPasNotificationOpened();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Saves information to the storage that PAS notification was opened. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void recordPasNotificationOpened(boolean wasNotificationOpened) {
        try {
            mService.recordPasNotificationOpened(wasNotificationOpened);
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
    public void recordBlockedTopic(List<TopicParcel> blockedTopicParcels) {
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
    public void removeBlockedTopic(TopicParcel blockedTopicParcel) {
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

    /** Returns whether the isAdIdEnabled bit is true. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public boolean isAdIdEnabled() {
        try {
            return mService.isAdIdEnabled();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Saves the isAdIdEnabled bit. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void setAdIdEnabled(boolean isAdIdEnabled) {
        try {
            mService.setAdIdEnabled(isAdIdEnabled);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Returns whether the isU18Account bit is true. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public boolean isU18Account() {
        try {
            return mService.isU18Account();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Saves the isU18Account bit. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void setU18Account(boolean isU18Account) {
        try {
            mService.setU18Account(isU18Account);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Returns whether the isEntryPointEnabled bit is true. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public boolean isEntryPointEnabled() {
        try {
            return mService.isEntryPointEnabled();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Saves the isEntryPointEnabled bit. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void setEntryPointEnabled(boolean isEntryPointEnabled) {
        try {
            mService.setEntryPointEnabled(isEntryPointEnabled);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Returns whether the isAdultAccount bit is true. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public boolean isAdultAccount() {
        try {
            return mService.isAdultAccount();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Saves the isAdultAccount bit. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void setAdultAccount(boolean isAdultAccount) {
        try {
            mService.setAdultAccount(isAdultAccount);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Returns whether the wasU18NotificationDisplayed bit is true. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public boolean wasU18NotificationDisplayed() {
        try {
            return mService.wasU18NotificationDisplayed();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Saves the wasU18NotificationDisplayed bit. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void setU18NotificationDisplayed(boolean wasU18NotificationDisplayed) {
        try {
            mService.setU18NotificationDisplayed(wasU18NotificationDisplayed);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Returns the current UX. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public String getUx() {
        try {
            return mService.getUx();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Set the current UX. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void setUx(String ux) {
        try {
            mService.setUx(ux);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Returns the current enrollment channel. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public String getEnrollmentChannel() {
        try {
            return mService.getEnrollmentChannel();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Set the current enrollment channel. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void setEnrollmentChannel(String enrollmentChannel) {
        try {
            mService.setEnrollmentChannel(enrollmentChannel);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Returns whether the isMeasurementDataReset bit is true. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public boolean isMeasurementDataReset() {
        try {
            return mService.isMeasurementDataReset();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Saves the isMeasurementDataReset bit. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void setMeasurementDataReset(boolean isMeasurementDataReset) {
        try {
            mService.setMeasurementDataReset(isMeasurementDataReset);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Returns whether the isPaDataReset bit is true. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public boolean isPaDataReset() {
        try {
            return mService.isPaDataReset();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Saves the isPaDataReset bit. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void setPaDataReset(boolean isPaDataReset) {
        try {
            mService.setPaDataReset(isPaDataReset);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Gets the module enrollment data. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public String getModuleEnrollmentState() {
        try {
            return mService.getModuleEnrollmentState();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Saves the module enrollment data. */
    @RequiresPermission(ACCESS_ADSERVICES_MANAGER)
    public void setModuleEnrollmentState(String enrollmentState) {
        try {
            mService.setModuleEnrollmentState(enrollmentState);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Dumps its internal state. */
    @RequiresPermission(DUMP)
    public static void dump(PrintWriter pw) {
        Objects.requireNonNull(pw, "PrintWriter cannot be null");

        pw.printf("AdServicesManager: ");
        synchronized (SINGLETON_LOCK) {
            pw.printf("sContext=%s, sSingleton=%s", sContext, sSingleton);
            if (sSingleton != null) {
                pw.printf(" (service=%s)", sSingleton.mService);
            }
            pw.println();
            return;
        }
    }
}
