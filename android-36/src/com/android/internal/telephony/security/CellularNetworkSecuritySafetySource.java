/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.internal.telephony.security;

import static android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_REFRESH_REQUESTED;
import static android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED;
import static android.safetycenter.SafetySourceData.SEVERITY_LEVEL_INFORMATION;
import static android.safetycenter.SafetySourceData.SEVERITY_LEVEL_RECOMMENDATION;

import android.annotation.IntDef;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.safetycenter.SafetyCenterManager;
import android.safetycenter.SafetyEvent;
import android.safetycenter.SafetySourceData;
import android.safetycenter.SafetySourceIssue;
import android.safetycenter.SafetySourceStatus;
import android.text.format.DateFormat;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.subscription.SubscriptionInfoInternal;
import com.android.internal.telephony.subscription.SubscriptionManagerService;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Holds the state needed to report the Safety Center status and issues related to cellular
 * network security.
 */
public class CellularNetworkSecuritySafetySource {
    private static final String SAFETY_SOURCE_ID = "AndroidCellularNetworkSecurity";

    private static final String NULL_CIPHER_ISSUE_NON_ENCRYPTED_ID = "null_cipher_non_encrypted";
    private static final String NULL_CIPHER_ISSUE_ENCRYPTED_ID = "null_cipher_encrypted";

    private static final String NULL_CIPHER_ACTION_SETTINGS_ID = "cellular_security_settings";
    private static final String NULL_CIPHER_ACTION_LEARN_MORE_ID = "learn_more";

    private static final String IDENTIFIER_DISCLOSURE_ISSUE_ID = "identifier_disclosure";

    private static final Intent CELLULAR_NETWORK_SECURITY_SETTINGS_INTENT =
            new Intent("android.settings.CELLULAR_NETWORK_SECURITY");
    static final int NULL_CIPHER_STATE_ENCRYPTED = 0;
    static final int NULL_CIPHER_STATE_NOTIFY_ENCRYPTED = 1;
    static final int NULL_CIPHER_STATE_NOTIFY_NON_ENCRYPTED = 2;

    @IntDef(
        prefix = {"NULL_CIPHER_STATE_"},
        value = {
            NULL_CIPHER_STATE_ENCRYPTED,
            NULL_CIPHER_STATE_NOTIFY_ENCRYPTED,
            NULL_CIPHER_STATE_NOTIFY_NON_ENCRYPTED})
    @Retention(RetentionPolicy.SOURCE)
    @interface NullCipherState {}

    private static CellularNetworkSecuritySafetySource sInstance;

    private final SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;
    private final SubscriptionManagerService mSubscriptionManagerService;

    private boolean mNullCipherStateIssuesEnabled;
    private HashMap<Integer, Integer> mNullCipherStates = new HashMap<>();

    private boolean mIdentifierDisclosureIssuesEnabled;
    private HashMap<Integer, IdentifierDisclosure> mIdentifierDisclosures = new HashMap<>();

    /**
     * Gets a singleton CellularNetworkSecuritySafetySource.
     */
    public static synchronized CellularNetworkSecuritySafetySource getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CellularNetworkSecuritySafetySource(
                    new SafetyCenterManagerWrapper(context));
        }
        return sInstance;
    }

    @VisibleForTesting
    public CellularNetworkSecuritySafetySource(
            SafetyCenterManagerWrapper safetyCenterManagerWrapper) {
        mSafetyCenterManagerWrapper = safetyCenterManagerWrapper;
        mSubscriptionManagerService = SubscriptionManagerService.getInstance();
    }

    /** Enables or disables the null cipher issue and clears any current issues. */
    public synchronized void setNullCipherIssueEnabled(Context context, boolean enabled) {
        mNullCipherStateIssuesEnabled = enabled;
        mNullCipherStates.clear();
        updateSafetyCenter(context);
    }

    /** Sets the null cipher issue state for the identified subscription. */
    public synchronized void setNullCipherState(
            Context context, int subId, @NullCipherState int nullCipherState) {
        mNullCipherStates.put(subId, nullCipherState);
        updateSafetyCenter(context);
    }

    /**
     * Clears issue state for the identified subscription
     */
    public synchronized  void clearNullCipherState(Context context, int subId) {
        mNullCipherStates.remove(subId);
        updateSafetyCenter(context);
    }
    /**
     * Enables or disables the identifier disclosure issue and clears any current issues if the
     * enable state is changed.
     */
    public synchronized void setIdentifierDisclosureIssueEnabled(Context context, boolean enabled) {
        // This check ensures that if we're enabled and we are asked to enable ourselves again (can
        // happen if the modem restarts), we don't clear our state.
        if (enabled != mIdentifierDisclosureIssuesEnabled) {
            mIdentifierDisclosureIssuesEnabled = enabled;
            mIdentifierDisclosures.clear();
            updateSafetyCenter(context);
        }
    }

    /** Sets the identifier disclosure issue state for the identifier subscription. */
    public synchronized void setIdentifierDisclosure(
            Context context, int subId, int count, Instant start, Instant end) {
        IdentifierDisclosure disclosure = new IdentifierDisclosure(count, start, end);
        mIdentifierDisclosures.put(subId, disclosure);
        updateSafetyCenter(context);
    }

    /** Clears the identifier disclosure issue state for the identified subscription. */
    public synchronized void clearIdentifierDisclosure(Context context, int subId) {
        mIdentifierDisclosures.remove(subId);
        updateSafetyCenter(context);
    }

    /** Refreshed the safety source in response to the identified broadcast. */
    public synchronized void refresh(Context context, String refreshBroadcastId) {
        mSafetyCenterManagerWrapper.setRefreshedSafetySourceData(
                refreshBroadcastId, getSafetySourceData(context));
    }

    private void updateSafetyCenter(Context context) {
        mSafetyCenterManagerWrapper.setSafetySourceData(getSafetySourceData(context));
    }

    private boolean isSafetySourceHidden() {
        return !mNullCipherStateIssuesEnabled && !mIdentifierDisclosureIssuesEnabled;
    }

    private SafetySourceData getSafetySourceData(Context context) {
        if (isSafetySourceHidden()) {
            // The cellular network security safety source is configured with
            // initialDisplayState="hidden"
            return null;
        }

        Stream<Optional<SafetySourceIssue>> nullCipherIssues =
                mNullCipherStates.entrySet().stream()
                        .map(e -> getNullCipherIssue(context, e.getKey(), e.getValue()));
        Stream<Optional<SafetySourceIssue>> identifierDisclosureIssues =
                mIdentifierDisclosures.entrySet().stream()
                        .map(e -> getIdentifierDisclosureIssue(context, e.getKey(), e.getValue()));
        SafetySourceIssue[] issues = Stream.concat(nullCipherIssues, identifierDisclosureIssues)
                .flatMap(Optional::stream)
                .toArray(SafetySourceIssue[]::new);

        SafetySourceData.Builder builder = new SafetySourceData.Builder();
        int maxSeverity = SEVERITY_LEVEL_INFORMATION;
        for (SafetySourceIssue issue : issues) {
            builder.addIssue(issue);
            maxSeverity = Math.max(maxSeverity, issue.getSeverityLevel());
        }

        builder.setStatus(
                new SafetySourceStatus.Builder(
                        context.getString(R.string.scCellularNetworkSecurityTitle),
                        context.getString(R.string.scCellularNetworkSecuritySummary),
                        maxSeverity)
                    .setPendingIntent(mSafetyCenterManagerWrapper.getActivityPendingIntent(
                            context, CELLULAR_NETWORK_SECURITY_SETTINGS_INTENT))
                    .build());
        return builder.build();
    }

    /** Builds the null cipher issue if it's enabled and there are null ciphers to report. */
    private Optional<SafetySourceIssue> getNullCipherIssue(
            Context context, int subId, @NullCipherState int state) {
        if (!mNullCipherStateIssuesEnabled) {
            return Optional.empty();
        }

        SubscriptionInfoInternal subInfo =
                mSubscriptionManagerService.getSubscriptionInfoInternal(subId);
        final SafetySourceIssue.Builder builder;
        final SafetySourceIssue.Notification customNotification;
        switch (state) {
            case NULL_CIPHER_STATE_ENCRYPTED:
                return Optional.empty();
            case NULL_CIPHER_STATE_NOTIFY_NON_ENCRYPTED:
                builder = new SafetySourceIssue.Builder(
                        NULL_CIPHER_ISSUE_NON_ENCRYPTED_ID + "_" + subId,
                        context.getString(
                                R.string.scNullCipherIssueNonEncryptedTitle),
                        context.getString(
                              R.string.scNullCipherIssueNonEncryptedSummary,
                              subInfo.getDisplayName()),
                        SEVERITY_LEVEL_RECOMMENDATION,
                        NULL_CIPHER_ISSUE_NON_ENCRYPTED_ID);
                customNotification =
                         new SafetySourceIssue.Notification.Builder(
                                context.getString(R.string.scNullCipherIssueNonEncryptedTitle),
                                context.getString(
                                        R.string.scNullCipherIssueNonEncryptedSummaryNotification,
                                        subInfo.getDisplayName()))
                        .build();
                break;
            case NULL_CIPHER_STATE_NOTIFY_ENCRYPTED:
                builder = new SafetySourceIssue.Builder(
                        NULL_CIPHER_ISSUE_NON_ENCRYPTED_ID + "_" + subId,
                        context.getString(
                                R.string.scNullCipherIssueEncryptedTitle,
                                subInfo.getDisplayName()),
                        context.getString(
                                R.string.scNullCipherIssueEncryptedSummary,
                                subInfo.getDisplayName()),
                        SEVERITY_LEVEL_INFORMATION,
                        NULL_CIPHER_ISSUE_ENCRYPTED_ID);
                customNotification =
                        new SafetySourceIssue.Notification.Builder(
                                context.getString(
                                      R.string.scNullCipherIssueEncryptedTitle,
                                      subInfo.getDisplayName()),
                                context.getString(
                                      R.string.scNullCipherIssueEncryptedSummary,
                                      subInfo.getDisplayName()))
                        .build();
                break;
            default:
                throw new AssertionError();
        }
        builder
                .setNotificationBehavior(
                        SafetySourceIssue.NOTIFICATION_BEHAVIOR_IMMEDIATELY)
                .setIssueCategory(SafetySourceIssue.ISSUE_CATEGORY_DATA)
                .setCustomNotification(customNotification)
                .addAction(
                        new SafetySourceIssue.Action.Builder(
                                NULL_CIPHER_ACTION_SETTINGS_ID,
                                context.getString(R.string.scNullCipherIssueActionSettings),
                                mSafetyCenterManagerWrapper.getActivityPendingIntent(
                                        context, CELLULAR_NETWORK_SECURITY_SETTINGS_INTENT))
                                .build());

        Intent learnMoreIntent = getLearnMoreIntent(context);
        if (learnMoreIntent != null) {
            builder.addAction(
                    new SafetySourceIssue.Action.Builder(
                            NULL_CIPHER_ACTION_LEARN_MORE_ID,
                            context.getString(
                                    R.string.scNullCipherIssueActionLearnMore),
                            mSafetyCenterManagerWrapper.getActivityPendingIntent(
                                    context, learnMoreIntent))
                            .build());
        }

        return Optional.of(builder.build());
    }

    /** Builds the identity disclosure issue if it's enabled and there are disclosures to report. */
    private Optional<SafetySourceIssue> getIdentifierDisclosureIssue(
            Context context, int subId, IdentifierDisclosure disclosure) {
        if (!mIdentifierDisclosureIssuesEnabled || disclosure.getDisclosureCount() == 0) {
            return Optional.empty();
        }

        SubscriptionInfoInternal subInfo =
                mSubscriptionManagerService.getSubscriptionInfoInternal(subId);

        // Notifications have no buttons
        final SafetySourceIssue.Notification customNotification =
                new SafetySourceIssue.Notification.Builder(
                        context.getString(R.string.scIdentifierDisclosureIssueTitle),
                        context.getString(
                                R.string.scIdentifierDisclosureIssueSummaryNotification,
                                getCurrentTime(),
                                subInfo.getDisplayName())).build();
        SafetySourceIssue.Builder builder =
                new SafetySourceIssue.Builder(
                        IDENTIFIER_DISCLOSURE_ISSUE_ID + "_" + subId,
                        context.getString(R.string.scIdentifierDisclosureIssueTitle),
                        context.getString(
                                R.string.scIdentifierDisclosureIssueSummary,
                                getCurrentTime(),
                                subInfo.getDisplayName()),
                        SEVERITY_LEVEL_RECOMMENDATION,
                        IDENTIFIER_DISCLOSURE_ISSUE_ID)
                        .setNotificationBehavior(
                                SafetySourceIssue.NOTIFICATION_BEHAVIOR_IMMEDIATELY)
                        .setIssueCategory(SafetySourceIssue.ISSUE_CATEGORY_DATA)
                        .setCustomNotification(customNotification)
                        .addAction(
                                new SafetySourceIssue.Action.Builder(
                                        NULL_CIPHER_ACTION_SETTINGS_ID,
                                        context.getString(
                                                R.string.scNullCipherIssueActionSettings),
                                        mSafetyCenterManagerWrapper.getActivityPendingIntent(
                                                context,
                                                CELLULAR_NETWORK_SECURITY_SETTINGS_INTENT))
                                        .build());

        Intent learnMoreIntent = getLearnMoreIntent(context);
        if (learnMoreIntent != null) {
            builder.addAction(
                    new SafetySourceIssue.Action.Builder(
                            NULL_CIPHER_ACTION_LEARN_MORE_ID,
                            context.getString(R.string.scNullCipherIssueActionLearnMore),
                            mSafetyCenterManagerWrapper.getActivityPendingIntent(
                                    context, learnMoreIntent)).build()
            );
        }

        return Optional.of(builder.build());
    }

    private String getCurrentTime() {
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "hh:mm");
        return Instant.now().atZone(ZoneId.systemDefault())
              .format(DateTimeFormatter.ofPattern(pattern)).toString();
    }

    /**
     * Return Intent for learn more action, or null if resource associated with the Intent
     * uri is
     * missing or empty.
     */
    private Intent getLearnMoreIntent(Context context) {
        String learnMoreUri;
        try {
            learnMoreUri = context.getString(R.string.scCellularNetworkSecurityLearnMore);
        } catch (Resources.NotFoundException e) {
            return null;
        }

        if (learnMoreUri.isEmpty()) {
            return null;
        }

        return new Intent(Intent.ACTION_VIEW, Uri.parse(learnMoreUri));
    }

    /** A wrapper around {@link SafetyCenterManager} that can be instrumented in tests. */
    @VisibleForTesting
    public static class SafetyCenterManagerWrapper {
        private final SafetyCenterManager mSafetyCenterManager;

        public SafetyCenterManagerWrapper(Context context) {
            mSafetyCenterManager = context.getSystemService(SafetyCenterManager.class);
        }

        /** Retrieve a {@link PendingIntent} that will start a new activity. */
        public PendingIntent getActivityPendingIntent(Context context, Intent intent) {
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        }

        /** Set the {@link SafetySourceData} for this safety source. */
        public void setSafetySourceData(SafetySourceData safetySourceData) {
            mSafetyCenterManager.setSafetySourceData(
                    SAFETY_SOURCE_ID,
                    safetySourceData,
                    new SafetyEvent.Builder(SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED).build());
        }

        /** Sets the {@link SafetySourceData} in response to a refresh request. */
        public void setRefreshedSafetySourceData(
                String refreshBroadcastId, SafetySourceData safetySourceData) {
            mSafetyCenterManager.setSafetySourceData(
                    SAFETY_SOURCE_ID,
                    safetySourceData,
                    new SafetyEvent.Builder(SAFETY_EVENT_TYPE_REFRESH_REQUESTED)
                            .setRefreshBroadcastId(refreshBroadcastId)
                            .build());
        }
    }

    private static class IdentifierDisclosure {
        private final int mDisclosureCount;
        private final Instant mWindowStart;
        private final Instant mWindowEnd;

        private IdentifierDisclosure(int count, Instant start, Instant end) {
            mDisclosureCount = count;
            mWindowStart = start;
            mWindowEnd = end;
        }

        private int getDisclosureCount() {
            return mDisclosureCount;
        }

        private Instant getWindowStart() {
            return mWindowStart;
        }

        private Instant getWindowEnd() {
            return mWindowEnd;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof IdentifierDisclosure)) {
                return false;
            }
            IdentifierDisclosure other = (IdentifierDisclosure) o;
            return mDisclosureCount == other.mDisclosureCount
                    && Objects.equals(mWindowStart, other.mWindowStart)
                    && Objects.equals(mWindowEnd, other.mWindowEnd);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mDisclosureCount, mWindowStart, mWindowEnd);
        }
    }
}
