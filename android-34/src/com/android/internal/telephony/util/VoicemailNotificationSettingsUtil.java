/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.internal.telephony.util;

import android.app.NotificationChannel;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

public class VoicemailNotificationSettingsUtil {
    private static final String VOICEMAIL_NOTIFICATION_RINGTONE_SHARED_PREFS_KEY_PREFIX =
            "voicemail_notification_ringtone_";
    private static final String VOICEMAIL_NOTIFICATION_VIBRATION_SHARED_PREFS_KEY_PREFIX =
            "voicemail_notification_vibrate_";

    // Old voicemail notification vibration string constants used for migration.
    private static final String OLD_VOICEMAIL_NOTIFICATION_RINGTONE_SHARED_PREFS_KEY =
            "button_voicemail_notification_ringtone_key";
    private static final String OLD_VOICEMAIL_NOTIFICATION_VIBRATION_SHARED_PREFS_KEY =
            "button_voicemail_notification_vibrate_key";
    private static final String OLD_VOICEMAIL_VIBRATE_WHEN_SHARED_PREFS_KEY =
            "button_voicemail_notification_vibrate_when_key";
    private static final String OLD_VOICEMAIL_RINGTONE_SHARED_PREFS_KEY =
            "button_voicemail_notification_ringtone_key";
    private static final String OLD_VOICEMAIL_VIBRATION_ALWAYS = "always";
    private static final String OLD_VOICEMAIL_VIBRATION_NEVER = "never";

    public static void setVibrationEnabled(Context context, boolean isEnabled) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(getVoicemailVibrationSharedPrefsKey(), isEnabled);
        editor.commit();
    }

    public static boolean isVibrationEnabled(Context context) {
        final NotificationChannel channel = NotificationChannelController.getChannel(
                NotificationChannelController.CHANNEL_ID_VOICE_MAIL, context);
        return (channel != null) ? channel.shouldVibrate() : getVibrationPreference(context);
    }

    public static boolean getVibrationPreference(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        migrateVoicemailVibrationSettingsIfNeeded(context, prefs);
        return prefs.getBoolean(getVoicemailVibrationSharedPrefsKey(), false /* defValue */);
    }

   public static void setRingtoneUri(Context context, Uri ringtoneUri) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String ringtoneUriStr = ringtoneUri != null ? ringtoneUri.toString() : "";

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getVoicemailRingtoneSharedPrefsKey(), ringtoneUriStr);
        editor.commit();
    }

    public static Uri getRingtoneUri(Context context) {
        final NotificationChannel channel = NotificationChannelController.getChannel(
                NotificationChannelController.CHANNEL_ID_VOICE_MAIL, context);
        return (channel != null) ? channel.getSound() : getRingTonePreference(context);
    }

    public static Uri getRingTonePreference(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        migrateVoicemailRingtoneSettingsIfNeeded(context, prefs);
        String uriString = prefs.getString(
                getVoicemailRingtoneSharedPrefsKey(),
                Settings.System.DEFAULT_NOTIFICATION_URI.toString());
        return !TextUtils.isEmpty(uriString) ? Uri.parse(uriString) : null;
    }

    /**
     * Migrate voicemail settings from {@link #OLD_VIBRATE_WHEN_KEY} or
     * {@link #OLD_VOICEMAIL_NOTIFICATION_VIBRATE_KEY}.
     *
     * TODO: Add helper which migrates settings from old version to new version.
     */
    private static void migrateVoicemailVibrationSettingsIfNeeded(
            Context context, SharedPreferences prefs) {
        String key = getVoicemailVibrationSharedPrefsKey();
        TelephonyManager telephonyManager = TelephonyManager.from(context);

        // Skip if a preference exists, or if phone is MSIM.
        if (prefs.contains(key) || telephonyManager.getPhoneCount() != 1) {
            return;
        }

        if (prefs.contains(OLD_VOICEMAIL_NOTIFICATION_VIBRATION_SHARED_PREFS_KEY)) {
            boolean voicemailVibrate = prefs.getBoolean(
                    OLD_VOICEMAIL_NOTIFICATION_VIBRATION_SHARED_PREFS_KEY, false /* defValue */);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(key, voicemailVibrate)
                    .remove(OLD_VOICEMAIL_VIBRATE_WHEN_SHARED_PREFS_KEY)
                    .commit();
        }

        if (prefs.contains(OLD_VOICEMAIL_VIBRATE_WHEN_SHARED_PREFS_KEY)) {
            // If vibrateWhen is always, then voicemailVibrate should be true.
            // If it is "only in silent mode", or "never", then voicemailVibrate should be false.
            String vibrateWhen = prefs.getString(
                    OLD_VOICEMAIL_VIBRATE_WHEN_SHARED_PREFS_KEY, OLD_VOICEMAIL_VIBRATION_NEVER);
            boolean voicemailVibrate = vibrateWhen.equals(OLD_VOICEMAIL_VIBRATION_ALWAYS);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(key, voicemailVibrate)
                    .remove(OLD_VOICEMAIL_NOTIFICATION_VIBRATION_SHARED_PREFS_KEY)
                    .commit();
        }
    }

    /**
     * Migrate voicemail settings from OLD_VOICEMAIL_NOTIFICATION_RINGTONE_SHARED_PREFS_KEY.
     *
     * TODO: Add helper which migrates settings from old version to new version.
     */
    private static void migrateVoicemailRingtoneSettingsIfNeeded(
            Context context, SharedPreferences prefs) {
        String key = getVoicemailRingtoneSharedPrefsKey();
        TelephonyManager telephonyManager = TelephonyManager.from(context);

        // Skip if a preference exists, or if phone is MSIM.
        if (prefs.contains(key) || telephonyManager.getPhoneCount() != 1) {
            return;
        }

        if (prefs.contains(OLD_VOICEMAIL_NOTIFICATION_RINGTONE_SHARED_PREFS_KEY)) {
            String uriString = prefs.getString(
                    OLD_VOICEMAIL_NOTIFICATION_RINGTONE_SHARED_PREFS_KEY, null /* defValue */);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(key, uriString)
                    .remove(OLD_VOICEMAIL_NOTIFICATION_RINGTONE_SHARED_PREFS_KEY)
                    .commit();
        }
    }

    private static String getVoicemailVibrationSharedPrefsKey() {
        return VOICEMAIL_NOTIFICATION_VIBRATION_SHARED_PREFS_KEY_PREFIX
                + SubscriptionManager.getDefaultSubscriptionId();
    }

    private static String getVoicemailRingtoneSharedPrefsKey() {
        return VOICEMAIL_NOTIFICATION_RINGTONE_SHARED_PREFS_KEY_PREFIX
                + SubscriptionManager.getDefaultSubscriptionId();
    }
}
