/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.net;

import static android.net.ConnectivityManager.MULTIPATH_PREFERENCE_HANDOVER;
import static android.net.ConnectivityManager.MULTIPATH_PREFERENCE_PERFORMANCE;
import static android.net.ConnectivityManager.MULTIPATH_PREFERENCE_RELIABILITY;

import static com.android.net.module.util.ConnectivitySettingsUtils.getPrivateDnsModeAsString;

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager.MultipathPreference;
import android.os.Binder;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Range;

import com.android.net.module.util.ConnectivitySettingsUtils;
import com.android.net.module.util.ProxyUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * A manager class for connectivity module settings.
 *
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public class ConnectivitySettingsManager {
    private static final String TAG = ConnectivitySettingsManager.class.getSimpleName();

    private ConnectivitySettingsManager() {}

    /** Data activity timeout settings */

    /**
     * Inactivity timeout to track mobile data activity.
     *
     * If set to a positive integer, it indicates the inactivity timeout value in seconds to
     * infer the data activity of mobile network. After a period of no activity on mobile
     * networks with length specified by the timeout, an {@code ACTION_DATA_ACTIVITY_CHANGE}
     * intent is fired to indicate a transition of network status from "active" to "idle". Any
     * subsequent activity on mobile networks triggers the firing of {@code
     * ACTION_DATA_ACTIVITY_CHANGE} intent indicating transition from "idle" to "active".
     *
     * Network activity refers to transmitting or receiving data on the network interfaces.
     *
     * Tracking is disabled if set to zero or negative value.
     *
     * @hide
     */
    public static final String DATA_ACTIVITY_TIMEOUT_MOBILE = "data_activity_timeout_mobile";

    /**
     * Timeout to tracking Wifi data activity. Same as {@code DATA_ACTIVITY_TIMEOUT_MOBILE}
     * but for Wifi network.
     *
     * @hide
     */
    public static final String DATA_ACTIVITY_TIMEOUT_WIFI = "data_activity_timeout_wifi";

    /** Dns resolver settings */

    /**
     * Sample validity in seconds to configure for the system DNS resolver.
     *
     * @hide
     */
    public static final String DNS_RESOLVER_SAMPLE_VALIDITY_SECONDS =
            "dns_resolver_sample_validity_seconds";

    /**
     * Success threshold in percent for use with the system DNS resolver.
     *
     * @hide
     */
    public static final String DNS_RESOLVER_SUCCESS_THRESHOLD_PERCENT =
            "dns_resolver_success_threshold_percent";

    /**
     * Minimum number of samples needed for statistics to be considered meaningful in the
     * system DNS resolver.
     *
     * @hide
     */
    public static final String DNS_RESOLVER_MIN_SAMPLES = "dns_resolver_min_samples";

    /**
     * Maximum number taken into account for statistics purposes in the system DNS resolver.
     *
     * @hide
     */
    public static final String DNS_RESOLVER_MAX_SAMPLES = "dns_resolver_max_samples";

    private static final int DNS_RESOLVER_DEFAULT_MIN_SAMPLES = 8;
    private static final int DNS_RESOLVER_DEFAULT_MAX_SAMPLES = 64;

    /** Network switch notification settings */

    /**
     * The maximum number of notifications shown in 24 hours when switching networks.
     *
     * @hide
     */
    public static final String NETWORK_SWITCH_NOTIFICATION_DAILY_LIMIT =
            "network_switch_notification_daily_limit";

    /**
     * The minimum time in milliseconds between notifications when switching networks.
     *
     * @hide
     */
    public static final String NETWORK_SWITCH_NOTIFICATION_RATE_LIMIT_MILLIS =
            "network_switch_notification_rate_limit_millis";

    /** Captive portal settings */

    /**
     * The URL used for HTTP captive portal detection upon a new connection.
     * A 204 response code from the server is used for validation.
     *
     * @hide
     */
    public static final String CAPTIVE_PORTAL_HTTP_URL = "captive_portal_http_url";

    /**
     * What to do when connecting a network that presents a captive portal.
     * Must be one of the CAPTIVE_PORTAL_MODE_* constants below.
     *
     * The default for this setting is CAPTIVE_PORTAL_MODE_PROMPT.
     *
     * @hide
     */
    public static final String CAPTIVE_PORTAL_MODE = "captive_portal_mode";

    /**
     * Don't attempt to detect captive portals.
     */
    public static final int CAPTIVE_PORTAL_MODE_IGNORE = 0;

    /**
     * When detecting a captive portal, display a notification that
     * prompts the user to sign in.
     */
    public static final int CAPTIVE_PORTAL_MODE_PROMPT = 1;

    /**
     * When detecting a captive portal, immediately disconnect from the
     * network and do not reconnect to that network in the future; except
     * on Wear platform companion proxy networks (transport BLUETOOTH)
     * will stay behind captive portal.
     */
    public static final int CAPTIVE_PORTAL_MODE_AVOID = 2;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            CAPTIVE_PORTAL_MODE_IGNORE,
            CAPTIVE_PORTAL_MODE_PROMPT,
            CAPTIVE_PORTAL_MODE_AVOID,
    })
    public @interface CaptivePortalMode {}

    /** Global http proxy settings */

    /**
     * Host name for global http proxy. Set via ConnectivityManager.
     *
     * @hide
     */
    public static final String GLOBAL_HTTP_PROXY_HOST = "global_http_proxy_host";

    /**
     * Integer host port for global http proxy. Set via ConnectivityManager.
     *
     * @hide
     */
    public static final String GLOBAL_HTTP_PROXY_PORT = "global_http_proxy_port";

    /**
     * Exclusion list for global proxy. This string contains a list of
     * comma-separated domains where the global proxy does not apply.
     * Domains should be listed in a comma- separated list. Example of
     * acceptable formats: ".domain1.com,my.domain2.com" Use
     * ConnectivityManager to set/get.
     *
     * @hide
     */
    public static final String GLOBAL_HTTP_PROXY_EXCLUSION_LIST =
            "global_http_proxy_exclusion_list";

    /**
     * The location PAC File for the proxy.
     *
     * @hide
     */
    public static final String GLOBAL_HTTP_PROXY_PAC = "global_proxy_pac_url";

    /** Private dns settings */

    /**
     * The requested Private DNS mode (string), and an accompanying specifier (string).
     *
     * Currently, the specifier holds the chosen provider name when the mode requests
     * a specific provider. It may be used to store the provider name even when the
     * mode changes so that temporarily disabling and re-enabling the specific
     * provider mode does not necessitate retyping the provider hostname.
     *
     * @hide
     */
    public static final String PRIVATE_DNS_MODE = "private_dns_mode";

    /**
     * The specific Private DNS provider name.
     *
     * @hide
     */
    public static final String PRIVATE_DNS_SPECIFIER = "private_dns_specifier";

    /**
     * Forced override of the default mode (hardcoded as "automatic", nee "opportunistic").
     * This allows changing the default mode without effectively disabling other modes,
     * all of which require explicit user action to enable/configure. See also b/79719289.
     *
     * Value is a string, suitable for assignment to PRIVATE_DNS_MODE above.
     *
     * @hide
     */
    public static final String PRIVATE_DNS_DEFAULT_MODE = "private_dns_default_mode";

    /** Other settings */

    /**
     * The number of milliseconds to hold on to a PendingIntent based request. This delay gives
     * the receivers of the PendingIntent an opportunity to make a new network request before
     * the Network satisfying the request is potentially removed.
     *
     * @hide
     */
    public static final String CONNECTIVITY_RELEASE_PENDING_INTENT_DELAY_MS =
            "connectivity_release_pending_intent_delay_ms";

    /**
     * Whether the mobile data connection should remain active even when higher
     * priority networks like WiFi are active, to help make network switching faster.
     *
     * See ConnectivityService for more info.
     *
     * (0 = disabled, 1 = enabled)
     *
     * @hide
     */
    public static final String MOBILE_DATA_ALWAYS_ON = "mobile_data_always_on";

    /**
     * Whether the wifi data connection should remain active even when higher
     * priority networks like Ethernet are active, to keep both networks.
     * In the case where higher priority networks are connected, wifi will be
     * unused unless an application explicitly requests to use it.
     *
     * See ConnectivityService for more info.
     *
     * (0 = disabled, 1 = enabled)
     *
     * @hide
     */
    public static final String WIFI_ALWAYS_REQUESTED = "wifi_always_requested";

    /**
     * Whether to automatically switch away from wifi networks that lose Internet access.
     * Only meaningful if config_networkAvoidBadWifi is set to 0, otherwise the system always
     * avoids such networks. Valid values are:
     *
     * 0: Don't avoid bad wifi, don't prompt the user. Get stuck on bad wifi like it's 2013.
     * null: Ask the user whether to switch away from bad wifi.
     * 1: Avoid bad wifi.
     *
     * @hide
     */
    public static final String NETWORK_AVOID_BAD_WIFI = "network_avoid_bad_wifi";

    /**
     * Don't avoid bad wifi, don't prompt the user. Get stuck on bad wifi like it's 2013.
     */
    public static final int NETWORK_AVOID_BAD_WIFI_IGNORE = 0;

    /**
     * Ask the user whether to switch away from bad wifi.
     */
    public static final int NETWORK_AVOID_BAD_WIFI_PROMPT = 1;

    /**
     * Avoid bad wifi.
     */
    public static final int NETWORK_AVOID_BAD_WIFI_AVOID = 2;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            NETWORK_AVOID_BAD_WIFI_IGNORE,
            NETWORK_AVOID_BAD_WIFI_PROMPT,
            NETWORK_AVOID_BAD_WIFI_AVOID,
    })
    public @interface NetworkAvoidBadWifi {}

    /**
     * User setting for ConnectivityManager.getMeteredMultipathPreference(). This value may be
     * overridden by the system based on device or application state. If null, the value
     * specified by config_networkMeteredMultipathPreference is used.
     *
     * @hide
     */
    public static final String NETWORK_METERED_MULTIPATH_PREFERENCE =
            "network_metered_multipath_preference";

    /**
     * A list of uids that should go on cellular networks in preference even when higher-priority
     * networks are connected.
     *
     * @hide
     */
    public static final String MOBILE_DATA_PREFERRED_UIDS = "mobile_data_preferred_uids";

    /**
     * One of the private DNS modes that indicates the private DNS mode is off.
     */
    public static final int PRIVATE_DNS_MODE_OFF = ConnectivitySettingsUtils.PRIVATE_DNS_MODE_OFF;

    /**
     * One of the private DNS modes that indicates the private DNS mode is automatic, which
     * will try to use the current DNS as private DNS.
     */
    public static final int PRIVATE_DNS_MODE_OPPORTUNISTIC =
            ConnectivitySettingsUtils.PRIVATE_DNS_MODE_OPPORTUNISTIC;

    /**
     * One of the private DNS modes that indicates the private DNS mode is strict and the
     * {@link #PRIVATE_DNS_SPECIFIER} is required, which will try to use the value of
     * {@link #PRIVATE_DNS_SPECIFIER} as private DNS.
     */
    public static final int PRIVATE_DNS_MODE_PROVIDER_HOSTNAME =
            ConnectivitySettingsUtils.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            PRIVATE_DNS_MODE_OFF,
            PRIVATE_DNS_MODE_OPPORTUNISTIC,
            PRIVATE_DNS_MODE_PROVIDER_HOSTNAME,
    })
    public @interface PrivateDnsMode {}

    /**
     * A list of uids that is allowed to use restricted networks.
     *
     * @hide
     */
    public static final String UIDS_ALLOWED_ON_RESTRICTED_NETWORKS =
            "uids_allowed_on_restricted_networks";

    /**
     * A global rate limit that applies to all networks with NET_CAPABILITY_INTERNET when enabled.
     *
     * @hide
     */
    public static final String INGRESS_RATE_LIMIT_BYTES_PER_SECOND =
            "ingress_rate_limit_bytes_per_second";

    /**
     * Get mobile data activity timeout from {@link Settings}.
     *
     * @param context The {@link Context} to query the setting.
     * @param def The default timeout if no setting value.
     * @return The {@link Duration} of timeout to track mobile data activity.
     */
    @NonNull
    public static Duration getMobileDataActivityTimeout(@NonNull Context context,
            @NonNull Duration def) {
        final int timeout = Settings.Global.getInt(
                context.getContentResolver(), DATA_ACTIVITY_TIMEOUT_MOBILE, (int) def.getSeconds());
        return Duration.ofSeconds(timeout);
    }

    /**
     * Set mobile data activity timeout to {@link Settings}.
     * Tracking is disabled if set to zero or negative value.
     *
     * Note: Only use the number of seconds in this duration, lower second(nanoseconds) will be
     * ignored.
     *
     * @param context The {@link Context} to set the setting.
     * @param timeout The mobile data activity timeout.
     */
    public static void setMobileDataActivityTimeout(@NonNull Context context,
            @NonNull Duration timeout) {
        Settings.Global.putInt(context.getContentResolver(), DATA_ACTIVITY_TIMEOUT_MOBILE,
                (int) timeout.getSeconds());
    }

    /**
     * Get wifi data activity timeout from {@link Settings}.
     *
     * @param context The {@link Context} to query the setting.
     * @param def The default timeout if no setting value.
     * @return The {@link Duration} of timeout to track wifi data activity.
     */
    @NonNull
    public static Duration getWifiDataActivityTimeout(@NonNull Context context,
            @NonNull Duration def) {
        final int timeout = Settings.Global.getInt(
                context.getContentResolver(), DATA_ACTIVITY_TIMEOUT_WIFI, (int) def.getSeconds());
        return Duration.ofSeconds(timeout);
    }

    /**
     * Set wifi data activity timeout to {@link Settings}.
     * Tracking is disabled if set to zero or negative value.
     *
     * Note: Only use the number of seconds in this duration, lower second(nanoseconds) will be
     * ignored.
     *
     * @param context The {@link Context} to set the setting.
     * @param timeout The wifi data activity timeout.
     */
    public static void setWifiDataActivityTimeout(@NonNull Context context,
            @NonNull Duration timeout) {
        Settings.Global.putInt(context.getContentResolver(), DATA_ACTIVITY_TIMEOUT_WIFI,
                (int) timeout.getSeconds());
    }

    /**
     * Get dns resolver sample validity duration from {@link Settings}.
     *
     * @param context The {@link Context} to query the setting.
     * @param def The default duration if no setting value.
     * @return The {@link Duration} of sample validity duration to configure for the system DNS
     *         resolver.
     */
    @NonNull
    public static Duration getDnsResolverSampleValidityDuration(@NonNull Context context,
            @NonNull Duration def) {
        final int duration = Settings.Global.getInt(context.getContentResolver(),
                DNS_RESOLVER_SAMPLE_VALIDITY_SECONDS, (int) def.getSeconds());
        return Duration.ofSeconds(duration);
    }

    /**
     * Set dns resolver sample validity duration to {@link Settings}. The duration must be a
     * positive number of seconds.
     *
     * @param context The {@link Context} to set the setting.
     * @param duration The sample validity duration.
     */
    public static void setDnsResolverSampleValidityDuration(@NonNull Context context,
            @NonNull Duration duration) {
        final int time = (int) duration.getSeconds();
        if (time <= 0) {
            throw new IllegalArgumentException("Invalid duration");
        }
        Settings.Global.putInt(
                context.getContentResolver(), DNS_RESOLVER_SAMPLE_VALIDITY_SECONDS, time);
    }

    /**
     * Get dns resolver success threshold percent from {@link Settings}.
     *
     * @param context The {@link Context} to query the setting.
     * @param def The default value if no setting value.
     * @return The success threshold in percent for use with the system DNS resolver.
     */
    public static int getDnsResolverSuccessThresholdPercent(@NonNull Context context, int def) {
        return Settings.Global.getInt(
                context.getContentResolver(), DNS_RESOLVER_SUCCESS_THRESHOLD_PERCENT, def);
    }

    /**
     * Set dns resolver success threshold percent to {@link Settings}. The threshold percent must
     * be 0~100.
     *
     * @param context The {@link Context} to set the setting.
     * @param percent The success threshold percent.
     */
    public static void setDnsResolverSuccessThresholdPercent(@NonNull Context context,
            @IntRange(from = 0, to = 100) int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Percent must be 0~100");
        }
        Settings.Global.putInt(
                context.getContentResolver(), DNS_RESOLVER_SUCCESS_THRESHOLD_PERCENT, percent);
    }

    /**
     * Get dns resolver samples range from {@link Settings}.
     *
     * @param context The {@link Context} to query the setting.
     * @return The {@link Range<Integer>} of samples needed for statistics to be considered
     *         meaningful in the system DNS resolver.
     */
    @NonNull
    public static Range<Integer> getDnsResolverSampleRanges(@NonNull Context context) {
        final int minSamples = Settings.Global.getInt(context.getContentResolver(),
                DNS_RESOLVER_MIN_SAMPLES, DNS_RESOLVER_DEFAULT_MIN_SAMPLES);
        final int maxSamples = Settings.Global.getInt(context.getContentResolver(),
                DNS_RESOLVER_MAX_SAMPLES, DNS_RESOLVER_DEFAULT_MAX_SAMPLES);
        return new Range<>(minSamples, maxSamples);
    }

    /**
     * Set dns resolver samples range to {@link Settings}.
     *
     * @param context The {@link Context} to set the setting.
     * @param range The samples range. The minimum number should be more than 0 and the maximum
     *              number should be less that 64.
     */
    public static void setDnsResolverSampleRanges(@NonNull Context context,
            @NonNull Range<Integer> range) {
        if (range.getLower() < 0 || range.getUpper() > 64) {
            throw new IllegalArgumentException("Argument must be 0~64");
        }
        Settings.Global.putInt(
                context.getContentResolver(), DNS_RESOLVER_MIN_SAMPLES, range.getLower());
        Settings.Global.putInt(
                context.getContentResolver(), DNS_RESOLVER_MAX_SAMPLES, range.getUpper());
    }

    /**
     * Get maximum count (from {@link Settings}) of switching network notifications shown in 24
     * hours.
     *
     * @param context The {@link Context} to query the setting.
     * @param def The default value if no setting value.
     * @return The maximum count of notifications shown in 24 hours when switching networks.
     */
    public static int getNetworkSwitchNotificationMaximumDailyCount(@NonNull Context context,
            int def) {
        return Settings.Global.getInt(
                context.getContentResolver(), NETWORK_SWITCH_NOTIFICATION_DAILY_LIMIT, def);
    }

    /**
     * Set maximum count (to {@link Settings}) of switching network notifications shown in 24 hours.
     * The count must be at least 0.
     *
     * @param context The {@link Context} to set the setting.
     * @param count The maximum count of switching network notifications shown in 24 hours.
     */
    public static void setNetworkSwitchNotificationMaximumDailyCount(@NonNull Context context,
            @IntRange(from = 0) int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count must be more than 0.");
        }
        Settings.Global.putInt(
                context.getContentResolver(), NETWORK_SWITCH_NOTIFICATION_DAILY_LIMIT, count);
    }

    /**
     * Get minimum duration (from {@link Settings}) between each switching network notifications.
     *
     * @param context The {@link Context} to query the setting.
     * @param def The default time if no setting value.
     * @return The minimum duration between notifications when switching networks.
     */
    @NonNull
    public static Duration getNetworkSwitchNotificationRateDuration(@NonNull Context context,
            @NonNull Duration def) {
        final int duration = Settings.Global.getInt(context.getContentResolver(),
                NETWORK_SWITCH_NOTIFICATION_RATE_LIMIT_MILLIS, (int) def.toMillis());
        return Duration.ofMillis(duration);
    }

    /**
     * Set minimum duration (to {@link Settings}) between each switching network notifications.
     * The duration will be rounded down to the next millisecond, and must be positive.
     *
     * @param context The {@link Context} to set the setting.
     * @param duration The minimum duration between notifications when switching networks.
     */
    public static void setNetworkSwitchNotificationRateDuration(@NonNull Context context,
            @NonNull Duration duration) {
        final int time = (int) duration.toMillis();
        if (time < 0) {
            throw new IllegalArgumentException("Invalid duration.");
        }
        Settings.Global.putInt(context.getContentResolver(),
                NETWORK_SWITCH_NOTIFICATION_RATE_LIMIT_MILLIS, time);
    }

    /**
     * Get URL (from {@link Settings}) used for HTTP captive portal detection upon a new connection.
     *
     * @param context The {@link Context} to query the setting.
     * @return The URL used for HTTP captive portal detection upon a new connection.
     */
    @Nullable
    public static String getCaptivePortalHttpUrl(@NonNull Context context) {
        return Settings.Global.getString(context.getContentResolver(), CAPTIVE_PORTAL_HTTP_URL);
    }

    /**
     * Set URL (to {@link Settings}) used for HTTP captive portal detection upon a new connection.
     * The URL is accessed to check for connectivity and presence of a captive portal on a network.
     * The URL should respond with HTTP status 204 to a GET request, and the stack will use
     * redirection status as a signal for captive portal detection.
     * If the URL is set to null or is otherwise incorrect or inaccessible, the stack will fail to
     * detect connectivity and portals. This will often result in loss of connectivity.
     *
     * @param context The {@link Context} to set the setting.
     * @param url The URL used for HTTP captive portal detection upon a new connection.
     */
    public static void setCaptivePortalHttpUrl(@NonNull Context context, @Nullable String url) {
        Settings.Global.putString(context.getContentResolver(), CAPTIVE_PORTAL_HTTP_URL, url);
    }

    /**
     * Get mode (from {@link Settings}) when connecting a network that presents a captive portal.
     *
     * @param context The {@link Context} to query the setting.
     * @param def The default mode if no setting value.
     * @return The mode when connecting a network that presents a captive portal.
     */
    @CaptivePortalMode
    public static int getCaptivePortalMode(@NonNull Context context,
            @CaptivePortalMode int def) {
        return Settings.Global.getInt(context.getContentResolver(), CAPTIVE_PORTAL_MODE, def);
    }

    /**
     * Set mode (to {@link Settings}) when connecting a network that presents a captive portal.
     *
     * @param context The {@link Context} to set the setting.
     * @param mode The mode when connecting a network that presents a captive portal.
     */
    public static void setCaptivePortalMode(@NonNull Context context, @CaptivePortalMode int mode) {
        if (!(mode == CAPTIVE_PORTAL_MODE_IGNORE
                || mode == CAPTIVE_PORTAL_MODE_PROMPT
                || mode == CAPTIVE_PORTAL_MODE_AVOID)) {
            throw new IllegalArgumentException("Invalid captive portal mode");
        }
        Settings.Global.putInt(context.getContentResolver(), CAPTIVE_PORTAL_MODE, mode);
    }

    /**
     * Get the global HTTP proxy applied to the device, or null if none.
     *
     * @param context The {@link Context} to query the setting.
     * @return The {@link ProxyInfo} which build from global http proxy settings.
     */
    @Nullable
    public static ProxyInfo getGlobalProxy(@NonNull Context context) {
        final String host = Settings.Global.getString(
                context.getContentResolver(), GLOBAL_HTTP_PROXY_HOST);
        final int port = Settings.Global.getInt(
                context.getContentResolver(), GLOBAL_HTTP_PROXY_PORT, 0 /* def */);
        final String exclusionList = Settings.Global.getString(
                context.getContentResolver(), GLOBAL_HTTP_PROXY_EXCLUSION_LIST);
        final String pacFileUrl = Settings.Global.getString(
                context.getContentResolver(), GLOBAL_HTTP_PROXY_PAC);

        if (TextUtils.isEmpty(host) && TextUtils.isEmpty(pacFileUrl)) {
            return null; // No global proxy.
        }

        if (TextUtils.isEmpty(pacFileUrl)) {
            return ProxyInfo.buildDirectProxy(
                    host, port, ProxyUtils.exclusionStringAsList(exclusionList));
        } else {
            return ProxyInfo.buildPacProxy(Uri.parse(pacFileUrl));
        }
    }

    /**
     * Set global http proxy settings from given {@link ProxyInfo}.
     *
     * <p class="note">
     * While a {@link ProxyInfo} for a PAC proxy can be specified, not all devices support
     * PAC proxies. In particular, smaller devices like watches often do not have the capabilities
     * necessary to interpret the PAC file. In such cases, calling this API with a PAC proxy
     * results in undefined behavior, including possibly breaking networking for applications.
     * You can test for this by checking for the presence of {@link PackageManager.FEATURE_WEBVIEW}.
     * </p>
     *
     * @param context The {@link Context} to set the setting.
     * @param proxyInfo The {@link ProxyInfo} for global http proxy settings which build from
     *                    {@link ProxyInfo#buildPacProxy(Uri)} or
     *                    {@link ProxyInfo#buildDirectProxy(String, int, List)}
     * @throws UnsupportedOperationException if |proxyInfo| codes for a PAC proxy but the system
     *                                       does not support PAC proxies.
     */
    public static void setGlobalProxy(@NonNull Context context, @NonNull ProxyInfo proxyInfo) {
        final String host = proxyInfo.getHost();
        final int port = proxyInfo.getPort();
        final String exclusionList = proxyInfo.getExclusionListAsString();
        final String pacFileUrl = proxyInfo.getPacFileUrl().toString();


        if (!TextUtils.isEmpty(pacFileUrl)) {
            final PackageManager pm = context.getPackageManager();
            if (null != pm && !pm.hasSystemFeature(PackageManager.FEATURE_WEBVIEW)) {
                Log.wtf(TAG, "PAC proxy can't be installed on a device without FEATURE_WEBVIEW");
            }
        }

        if (TextUtils.isEmpty(pacFileUrl)) {
            Settings.Global.putString(context.getContentResolver(), GLOBAL_HTTP_PROXY_HOST, host);
            Settings.Global.putInt(context.getContentResolver(), GLOBAL_HTTP_PROXY_PORT, port);
            Settings.Global.putString(
                    context.getContentResolver(), GLOBAL_HTTP_PROXY_EXCLUSION_LIST, exclusionList);
            Settings.Global.putString(
                    context.getContentResolver(), GLOBAL_HTTP_PROXY_PAC, "" /* value */);
        } else {
            Settings.Global.putString(
                    context.getContentResolver(), GLOBAL_HTTP_PROXY_PAC, pacFileUrl);
            Settings.Global.putString(
                    context.getContentResolver(), GLOBAL_HTTP_PROXY_HOST, "" /* value */);
            Settings.Global.putInt(
                    context.getContentResolver(), GLOBAL_HTTP_PROXY_PORT, 0 /* value */);
            Settings.Global.putString(
                    context.getContentResolver(), GLOBAL_HTTP_PROXY_EXCLUSION_LIST, "" /* value */);
        }
    }

    /**
     * Clear all global http proxy settings.
     *
     * @param context The {@link Context} to set the setting.
     */
    public static void clearGlobalProxy(@NonNull Context context) {
        Settings.Global.putString(
                context.getContentResolver(), GLOBAL_HTTP_PROXY_HOST, "" /* value */);
        Settings.Global.putInt(
                context.getContentResolver(), GLOBAL_HTTP_PROXY_PORT, 0 /* value */);
        Settings.Global.putString(
                context.getContentResolver(), GLOBAL_HTTP_PROXY_EXCLUSION_LIST, "" /* value */);
        Settings.Global.putString(
                context.getContentResolver(), GLOBAL_HTTP_PROXY_PAC, "" /* value */);
    }

    /**
     * Get private DNS mode from settings.
     *
     * @param context The Context to query the private DNS mode from settings.
     * @return A string of private DNS mode.
     */
    @PrivateDnsMode
    public static int getPrivateDnsMode(@NonNull Context context) {
        return ConnectivitySettingsUtils.getPrivateDnsMode(context);
    }

    /**
     * Set private DNS mode to settings.
     *
     * @param context The {@link Context} to set the private DNS mode.
     * @param mode The private dns mode. This should be one of the PRIVATE_DNS_MODE_* constants.
     */
    public static void setPrivateDnsMode(@NonNull Context context, @PrivateDnsMode int mode) {
        ConnectivitySettingsUtils.setPrivateDnsMode(context, mode);
    }

    /**
     * Get specific private dns provider name from {@link Settings}.
     *
     * @param context The {@link Context} to query the setting.
     * @return The specific private dns provider name, or null if no setting value.
     */
    @Nullable
    public static String getPrivateDnsHostname(@NonNull Context context) {
        return ConnectivitySettingsUtils.getPrivateDnsHostname(context);
    }

    /**
     * Set specific private dns provider name to {@link Settings}.
     *
     * @param context The {@link Context} to set the setting.
     * @param specifier The specific private dns provider name.
     */
    public static void setPrivateDnsHostname(@NonNull Context context, @Nullable String specifier) {
        ConnectivitySettingsUtils.setPrivateDnsHostname(context, specifier);
    }

    /**
     * Get default private dns mode from {@link Settings}.
     *
     * @param context The {@link Context} to query the setting.
     * @return The default private dns mode.
     */
    @PrivateDnsMode
    @NonNull
    public static String getPrivateDnsDefaultMode(@NonNull Context context) {
        return Settings.Global.getString(context.getContentResolver(), PRIVATE_DNS_DEFAULT_MODE);
    }

    /**
     * Set default private dns mode to {@link Settings}.
     *
     * @param context The {@link Context} to set the setting.
     * @param mode The default private dns mode. This should be one of the PRIVATE_DNS_MODE_*
     *             constants.
     */
    public static void setPrivateDnsDefaultMode(@NonNull Context context,
            @NonNull @PrivateDnsMode int mode) {
        if (!(mode == PRIVATE_DNS_MODE_OFF
                || mode == PRIVATE_DNS_MODE_OPPORTUNISTIC
                || mode == PRIVATE_DNS_MODE_PROVIDER_HOSTNAME)) {
            throw new IllegalArgumentException("Invalid private dns mode");
        }
        Settings.Global.putString(context.getContentResolver(), PRIVATE_DNS_DEFAULT_MODE,
                getPrivateDnsModeAsString(mode));
    }

    /**
     * Get duration (from {@link Settings}) to keep a PendingIntent-based request.
     *
     * @param context The {@link Context} to query the setting.
     * @param def The default duration if no setting value.
     * @return The duration to keep a PendingIntent-based request.
     */
    @NonNull
    public static Duration getConnectivityKeepPendingIntentDuration(@NonNull Context context,
            @NonNull Duration def) {
        final int duration = Settings.Secure.getInt(context.getContentResolver(),
                CONNECTIVITY_RELEASE_PENDING_INTENT_DELAY_MS, (int) def.toMillis());
        return Duration.ofMillis(duration);
    }

    /**
     * Set duration (to {@link Settings}) to keep a PendingIntent-based request.
     * The duration will be rounded down to the next millisecond, and must be positive.
     *
     * @param context The {@link Context} to set the setting.
     * @param duration The duration to keep a PendingIntent-based request.
     */
    public static void setConnectivityKeepPendingIntentDuration(@NonNull Context context,
            @NonNull Duration duration) {
        final int time = (int) duration.toMillis();
        if (time < 0) {
            throw new IllegalArgumentException("Invalid duration.");
        }
        Settings.Secure.putInt(
                context.getContentResolver(), CONNECTIVITY_RELEASE_PENDING_INTENT_DELAY_MS, time);
    }

    /**
     * Read from {@link Settings} whether the mobile data connection should remain active
     * even when higher priority networks are active.
     *
     * @param context The {@link Context} to query the setting.
     * @param def The default value if no setting value.
     * @return Whether the mobile data connection should remain active even when higher
     *         priority networks are active.
     */
    public static boolean getMobileDataAlwaysOn(@NonNull Context context, boolean def) {
        final int enable = Settings.Global.getInt(
                context.getContentResolver(), MOBILE_DATA_ALWAYS_ON, (def ? 1 : 0));
        return (enable != 0) ? true : false;
    }

    /**
     * Write into {@link Settings} whether the mobile data connection should remain active
     * even when higher priority networks are active.
     *
     * @param context The {@link Context} to set the setting.
     * @param enable Whether the mobile data connection should remain active even when higher
     *               priority networks are active.
     */
    public static void setMobileDataAlwaysOn(@NonNull Context context, boolean enable) {
        Settings.Global.putInt(
                context.getContentResolver(), MOBILE_DATA_ALWAYS_ON, (enable ? 1 : 0));
    }

    /**
     * Read from {@link Settings} whether the wifi data connection should remain active
     * even when higher priority networks are active.
     *
     * @param context The {@link Context} to query the setting.
     * @param def The default value if no setting value.
     * @return Whether the wifi data connection should remain active even when higher
     *         priority networks are active.
     */
    public static boolean getWifiAlwaysRequested(@NonNull Context context, boolean def) {
        final int enable = Settings.Global.getInt(
                context.getContentResolver(), WIFI_ALWAYS_REQUESTED, (def ? 1 : 0));
        return (enable != 0) ? true : false;
    }

    /**
     * Write into {@link Settings} whether the wifi data connection should remain active
     * even when higher priority networks are active.
     *
     * @param context The {@link Context} to set the setting.
     * @param enable Whether the wifi data connection should remain active even when higher
     *               priority networks are active
     */
    public static void setWifiAlwaysRequested(@NonNull Context context, boolean enable) {
        Settings.Global.putInt(
                context.getContentResolver(), WIFI_ALWAYS_REQUESTED, (enable ? 1 : 0));
    }

    /**
     * Get avoid bad wifi setting from {@link Settings}.
     *
     * @param context The {@link Context} to query the setting.
     * @return The setting whether to automatically switch away from wifi networks that lose
     *         internet access.
     */
    @NetworkAvoidBadWifi
    public static int getNetworkAvoidBadWifi(@NonNull Context context) {
        final String setting =
                Settings.Global.getString(context.getContentResolver(), NETWORK_AVOID_BAD_WIFI);
        if ("0".equals(setting)) {
            return NETWORK_AVOID_BAD_WIFI_IGNORE;
        } else if ("1".equals(setting)) {
            return NETWORK_AVOID_BAD_WIFI_AVOID;
        } else {
            return NETWORK_AVOID_BAD_WIFI_PROMPT;
        }
    }

    /**
     * Set avoid bad wifi setting to {@link Settings}.
     *
     * @param context The {@link Context} to set the setting.
     * @param value Whether to automatically switch away from wifi networks that lose internet
     *              access.
     */
    public static void setNetworkAvoidBadWifi(@NonNull Context context,
            @NetworkAvoidBadWifi int value) {
        final String setting;
        if (value == NETWORK_AVOID_BAD_WIFI_IGNORE) {
            setting = "0";
        } else if (value == NETWORK_AVOID_BAD_WIFI_AVOID) {
            setting = "1";
        } else if (value == NETWORK_AVOID_BAD_WIFI_PROMPT) {
            setting = null;
        } else {
            throw new IllegalArgumentException("Invalid avoid bad wifi setting");
        }
        Settings.Global.putString(context.getContentResolver(), NETWORK_AVOID_BAD_WIFI, setting);
    }

    /**
     * Get network metered multipath preference from {@link Settings}.
     *
     * @param context The {@link Context} to query the setting.
     * @return The network metered multipath preference which should be one of
     *         ConnectivityManager#MULTIPATH_PREFERENCE_* value or null if the value specified
     *         by config_networkMeteredMultipathPreference is used.
     */
    @Nullable
    public static String getNetworkMeteredMultipathPreference(@NonNull Context context) {
        return Settings.Global.getString(
                context.getContentResolver(), NETWORK_METERED_MULTIPATH_PREFERENCE);
    }

    /**
     * Set network metered multipath preference to {@link Settings}.
     *
     * @param context The {@link Context} to set the setting.
     * @param preference The network metered multipath preference which should be one of
     *                   ConnectivityManager#MULTIPATH_PREFERENCE_* value or null if the value
     *                   specified by config_networkMeteredMultipathPreference is used.
     */
    public static void setNetworkMeteredMultipathPreference(@NonNull Context context,
            @NonNull @MultipathPreference String preference) {
        if (!(Integer.valueOf(preference) == MULTIPATH_PREFERENCE_HANDOVER
                || Integer.valueOf(preference) == MULTIPATH_PREFERENCE_RELIABILITY
                || Integer.valueOf(preference) == MULTIPATH_PREFERENCE_PERFORMANCE)) {
            throw new IllegalArgumentException("Invalid private dns mode");
        }
        Settings.Global.putString(
                context.getContentResolver(), NETWORK_METERED_MULTIPATH_PREFERENCE, preference);
    }

    private static Set<Integer> getUidSetFromString(@Nullable String uidList) {
        final Set<Integer> uids = new ArraySet<>();
        if (TextUtils.isEmpty(uidList)) {
            return uids;
        }
        for (String uid : uidList.split(";")) {
            uids.add(Integer.valueOf(uid));
        }
        return uids;
    }

    private static String getUidStringFromSet(@NonNull Set<Integer> uidList) {
        final StringJoiner joiner = new StringJoiner(";");
        for (Integer uid : uidList) {
            if (uid < 0 || UserHandle.getAppId(uid) > Process.LAST_APPLICATION_UID) {
                throw new IllegalArgumentException("Invalid uid");
            }
            joiner.add(uid.toString());
        }
        return joiner.toString();
    }

    /**
     * Get the list of uids(from {@link Settings}) that should go on cellular networks in preference
     * even when higher-priority networks are connected.
     *
     * @param context The {@link Context} to query the setting.
     * @return A list of uids that should go on cellular networks in preference even when
     *         higher-priority networks are connected or null if no setting value.
     */
    @NonNull
    public static Set<Integer> getMobileDataPreferredUids(@NonNull Context context) {
        final String uidList = Settings.Secure.getString(
                context.getContentResolver(), MOBILE_DATA_PREFERRED_UIDS);
        return getUidSetFromString(uidList);
    }

    /**
     * Set the list of uids(to {@link Settings}) that should go on cellular networks in preference
     * even when higher-priority networks are connected.
     *
     * @param context The {@link Context} to set the setting.
     * @param uidList A list of uids that should go on cellular networks in preference even when
     *             higher-priority networks are connected.
     */
    public static void setMobileDataPreferredUids(@NonNull Context context,
            @NonNull Set<Integer> uidList) {
        final String uids = getUidStringFromSet(uidList);
        Settings.Secure.putString(context.getContentResolver(), MOBILE_DATA_PREFERRED_UIDS, uids);
    }

    /**
     * Get the list of uids (from {@link Settings}) allowed to use restricted networks.
     *
     * Access to restricted networks is controlled by the (preinstalled-only)
     * CONNECTIVITY_USE_RESTRICTED_NETWORKS permission, but highly privileged
     * callers can also set a list of uids that can access restricted networks.
     *
     * This is useful for example in some jurisdictions where government apps,
     * that can't be preinstalled, must still have access to emergency services.
     *
     * @param context The {@link Context} to query the setting.
     * @return A list of uids that is allowed to use restricted networks or null if no setting
     *         value.
     */
    @NonNull
    public static Set<Integer> getUidsAllowedOnRestrictedNetworks(@NonNull Context context) {
        final String uidList = Settings.Global.getString(
                context.getContentResolver(), UIDS_ALLOWED_ON_RESTRICTED_NETWORKS);
        return getUidSetFromString(uidList);
    }

    private static boolean isCallingFromSystem() {
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        if (uid == Process.SYSTEM_UID && pid == Process.myPid()) {
            return true;
        }
        return false;
    }

    /**
     * Set the list of uids(from {@link Settings}) that is allowed to use restricted networks.
     *
     * @param context The {@link Context} to set the setting.
     * @param uidList A list of uids that is allowed to use restricted networks.
     */
    public static void setUidsAllowedOnRestrictedNetworks(@NonNull Context context,
            @NonNull Set<Integer> uidList) {
        final boolean calledFromSystem = isCallingFromSystem();
        if (!calledFromSystem) {
            // Enforce NETWORK_SETTINGS check if it's debug build. This is for MTS test only.
            if (!Build.isDebuggable()) {
                throw new SecurityException("Only system can set this setting.");
            }
            context.enforceCallingOrSelfPermission(android.Manifest.permission.NETWORK_SETTINGS,
                    "Requires NETWORK_SETTINGS permission");
        }
        final String uids = getUidStringFromSet(uidList);
        Settings.Global.putString(context.getContentResolver(), UIDS_ALLOWED_ON_RESTRICTED_NETWORKS,
                uids);
    }

    /**
     * Get the network bandwidth ingress rate limit.
     *
     * The limit is only applicable to networks that provide internet connectivity. -1 codes for no
     * bandwidth limitation.
     *
     * @param context The {@link Context} to query the setting.
     * @return The rate limit in number of bytes per second or -1 if disabled.
     */
    public static long getIngressRateLimitInBytesPerSecond(@NonNull Context context) {
        return Settings.Global.getLong(context.getContentResolver(),
                INGRESS_RATE_LIMIT_BYTES_PER_SECOND, -1);
    }

    /**
     * Set the network bandwidth ingress rate limit.
     *
     * The limit is applied to all networks that provide internet connectivity. It is applied on a
     * per-network basis, meaning that global ingress rate could exceed the limit when communicating
     * on multiple networks simultaneously.
     *
     * @param context The {@link Context} to set the setting.
     * @param rateLimitInBytesPerSec The rate limit in number of bytes per second or -1 to disable.
     */
    public static void setIngressRateLimitInBytesPerSecond(@NonNull Context context,
            @IntRange(from = -1L, to = 0xFFFFFFFFL) long rateLimitInBytesPerSec) {
        if (rateLimitInBytesPerSec < -1) {
            throw new IllegalArgumentException(
                    "Rate limit must be within the range [-1, Integer.MAX_VALUE]");
        }
        Settings.Global.putLong(context.getContentResolver(),
                INGRESS_RATE_LIMIT_BYTES_PER_SECOND,
                rateLimitInBytesPerSec);
    }
}
