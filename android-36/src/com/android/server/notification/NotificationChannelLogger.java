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

package com.android.server.notification;

import static android.app.NotificationManager.IMPORTANCE_HIGH;

import android.annotation.NonNull;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.stats.sysui.NotificationEnums;

import com.android.internal.logging.UiEvent;
import com.android.internal.logging.UiEventLogger;
import com.android.internal.util.FrameworkStatsLog;

/**
 * Interface for logging NotificationChannelModified statsd atoms.  Provided as an interface to
 * enable unit-testing - use standard implementation NotificationChannelLoggerImpl in production.
 */
public interface NotificationChannelLogger {
    // The logging interface. Not anticipating a need to override these high-level methods, which by
    // default forward to a lower-level interface.

    /**
     * Log the creation of a notification channel.
     * @param channel The channel.
     * @param uid UID of app that owns the channel.
     * @param pkg Package of app that owns the channel.
     */
    default void logNotificationChannelCreated(@NonNull NotificationChannel channel, int uid,
            String pkg) {
        logNotificationChannel(
                NotificationChannelEvent.getCreated(channel),
                channel, uid, pkg, 0, getLoggingImportance(channel));
    }

    /**
     * Log the deletion of a notification channel.
     * @param channel The channel.
     * @param uid UID of app that owns the channel.
     * @param pkg Package of app that owns the channel.
     */
    default void logNotificationChannelDeleted(@NonNull NotificationChannel channel, int uid,
            String pkg) {
        logNotificationChannel(
                NotificationChannelEvent.getDeleted(channel),
                channel, uid, pkg, getLoggingImportance(channel), 0);
    }

    /**
     * Log the modification of a notification channel.
     * @param channel The channel.
     * @param uid UID of app that owns the channel.
     * @param pkg Package of app that owns the channel.
     * @param oldLoggingImportance Previous logging importance level of the channel.
     * @param byUser True if the modification was user-specified.
     */
    default void logNotificationChannelModified(@NonNull NotificationChannel channel, int uid,
            String pkg, int oldLoggingImportance, boolean byUser) {
        logNotificationChannel(NotificationChannelEvent.getUpdated(byUser),
                channel, uid, pkg, oldLoggingImportance, getLoggingImportance(channel));
    }

    /**
     * Log the creation or modification of a notification channel group.
     * @param channelGroup The notification channel group.
     * @param uid UID of app that owns the channel.
     * @param pkg Package of app that owns the channel.
     * @param isNew True if this is a creation of a new group.
     * @param wasBlocked
     */
    default void logNotificationChannelGroup(@NonNull NotificationChannelGroup channelGroup,
            int uid, String pkg, boolean isNew, boolean wasBlocked) {
        logNotificationChannelGroup(NotificationChannelEvent.getGroupUpdated(isNew),
                channelGroup, uid, pkg, wasBlocked);
    }

    /**
     * Log the deletion of a notification channel group.
     * @param channelGroup The notification channel group.
     * @param uid UID of app that owns the channel.
     * @param pkg Package of app that owns the channel.
     */
    default void logNotificationChannelGroupDeleted(@NonNull NotificationChannelGroup channelGroup,
            int uid, String pkg) {
        logNotificationChannelGroup(NotificationChannelEvent.NOTIFICATION_CHANNEL_GROUP_DELETED,
                channelGroup, uid, pkg, false);
    }

    /**
     * Log blocking or unblocking of the entire app's notifications.
     * @param uid UID of the app.
     * @param pkg Package name of the app.
     * @param enabled If true, notifications are now allowed.
     */
    default void logAppNotificationsAllowed(int uid, String pkg, boolean enabled) {
        logAppEvent(NotificationChannelEvent.getBlocked(enabled), uid, pkg);
    }

    /**
     * Low-level interface for logging events, to be implemented.
     * @param event Event to log.
     * @param channel Notification channel.
     * @param uid UID of app that owns the channel.
     * @param pkg Package of app that owns the channel.
     * @param oldImportance Old importance of the channel, if applicable (0 otherwise).
     * @param newImportance New importance of the channel, if applicable (0 otherwise).
     */
    void logNotificationChannel(@NonNull NotificationChannelEvent event,
            @NonNull NotificationChannel channel, int uid, String pkg,
            int oldImportance, int newImportance);

    /**
     * Low-level interface for logging channel group events, to be implemented.
     * @param event Event to log.
     * @param channelGroup Notification channel group.
     * @param uid UID of app that owns the channel.
     * @param pkg Package of app that owns the channel.
     * @param wasBlocked True if the channel is being modified and was previously blocked.
     */
    void logNotificationChannelGroup(@NonNull NotificationChannelEvent event,
            @NonNull NotificationChannelGroup channelGroup, int uid, String pkg,
            boolean wasBlocked);

    /**
     * Low-level interface for logging app-as-a-whole events, to be implemented.
     * @param uid UID of app.
     * @param pkg Package of app.
     */
    void logAppEvent(@NonNull NotificationChannelEvent event, int uid, String pkg);

    /**
     * The UiEvent enums that this class can log.
     */
    enum NotificationChannelEvent implements UiEventLogger.UiEventEnum {
        @UiEvent(doc = "App created a new notification channel")
        NOTIFICATION_CHANNEL_CREATED(219),
        @UiEvent(doc = "App modified an existing notification channel")
        NOTIFICATION_CHANNEL_UPDATED(220),
        @UiEvent(doc = "User modified a new notification channel")
        NOTIFICATION_CHANNEL_UPDATED_BY_USER(221),
        @UiEvent(doc = "App deleted an existing notification channel")
        NOTIFICATION_CHANNEL_DELETED(222),
        @UiEvent(doc = "App created a new notification channel group")
        NOTIFICATION_CHANNEL_GROUP_CREATED(223),
        @UiEvent(doc = "App modified an existing notification channel group")
        NOTIFICATION_CHANNEL_GROUP_UPDATED(224),
        @UiEvent(doc = "App deleted an existing notification channel group")
        NOTIFICATION_CHANNEL_GROUP_DELETED(226),
        @UiEvent(doc = "System created a new conversation (sub-channel in a notification channel)")
        NOTIFICATION_CHANNEL_CONVERSATION_CREATED(272),
        @UiEvent(doc = "System deleted a new conversation (sub-channel in a notification channel)")
        NOTIFICATION_CHANNEL_CONVERSATION_DELETED(274),
        @UiEvent(doc = "All notifications for the app were blocked.")
        APP_NOTIFICATIONS_BLOCKED(557),
        @UiEvent(doc = "Notifications for the app as a whole were unblocked.")
        APP_NOTIFICATIONS_UNBLOCKED(558);

        private final int mId;
        NotificationChannelEvent(int id) {
            mId = id;
        }
        @Override public int getId() {
            return mId;
        }

        public static NotificationChannelEvent getUpdated(boolean byUser) {
            return byUser
                    ? NotificationChannelEvent.NOTIFICATION_CHANNEL_UPDATED_BY_USER
                    : NotificationChannelEvent.NOTIFICATION_CHANNEL_UPDATED;
        }

        public static NotificationChannelEvent getCreated(@NonNull NotificationChannel channel) {
            return channel.getConversationId() != null
                    ? NotificationChannelEvent.NOTIFICATION_CHANNEL_CONVERSATION_CREATED
                    : NotificationChannelEvent.NOTIFICATION_CHANNEL_CREATED;
        }

        public static NotificationChannelEvent getDeleted(@NonNull NotificationChannel channel) {
            return channel.getConversationId() != null
                    ? NotificationChannelEvent.NOTIFICATION_CHANNEL_CONVERSATION_DELETED
                    : NotificationChannelEvent.NOTIFICATION_CHANNEL_DELETED;
        }

        public static NotificationChannelEvent getGroupUpdated(boolean isNew) {
            return isNew
                    ? NotificationChannelEvent.NOTIFICATION_CHANNEL_GROUP_CREATED
                    : NotificationChannelEvent.NOTIFICATION_CHANNEL_GROUP_DELETED;
        }

        public static NotificationChannelEvent getBlocked(boolean enabled) {
            return enabled ? APP_NOTIFICATIONS_UNBLOCKED : APP_NOTIFICATIONS_BLOCKED;
        }
    }

    /**
     * @return Small hash of the channel ID, if present, or 0 otherwise.
     */
    static int getIdHash(@NonNull NotificationChannel channel) {
        return SmallHash.hash(channel.getId());
    }

    /**
     * @return Small hash of the conversation ID, if present, or 0 otherwise.
     */
    static int getConversationIdHash(@NonNull NotificationChannel channel) {
        return SmallHash.hash(channel.getConversationId());
    }

    /**
     * @return Small hash of the channel ID, if present, or 0 otherwise.
     */
    static int getIdHash(@NonNull NotificationChannelGroup group) {
        return SmallHash.hash(group.getId());
    }

    /**
     * @return Logging importance for a channel: the regular importance, or
     *     IMPORTANCE_IMPORTANT_CONVERSATION for a HIGH-importance conversation tagged important.
     */
    static int getLoggingImportance(@NonNull NotificationChannel channel) {
        return getLoggingImportance(channel, channel.getImportance());
    }

    /**
     * @return Logging importance for a channel or notification: the regular importance, or
     *     IMPORTANCE_IMPORTANT_CONVERSATION for a HIGH-importance conversation tagged important.
     */
    static int getLoggingImportance(@NonNull NotificationChannel channel, int importance) {
        if (channel.getConversationId() == null || importance < IMPORTANCE_HIGH) {
            return importance;
        }
        return (channel.isImportantConversation())
                ? NotificationEnums.IMPORTANCE_IMPORTANT_CONVERSATION
                : importance;
    }

    /**
     * @return "Importance" for a channel group
     */
    static int getImportance(@NonNull NotificationChannelGroup channelGroup) {
        return getImportance(channelGroup.isBlocked());
    }

    /**
     * @return "Importance" for a channel group, from its blocked status
     */
    static int getImportance(boolean isBlocked) {
        return isBlocked
                ? FrameworkStatsLog.NOTIFICATION_CHANNEL_MODIFIED__IMPORTANCE__IMPORTANCE_NONE
                : FrameworkStatsLog.NOTIFICATION_CHANNEL_MODIFIED__IMPORTANCE__IMPORTANCE_DEFAULT;
    }

}
