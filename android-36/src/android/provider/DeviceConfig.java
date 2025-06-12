/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.provider;

import static android.Manifest.permission.WRITE_ALLOWLISTED_DEVICE_CONFIG;
import static android.Manifest.permission.READ_DEVICE_CONFIG;
import static android.Manifest.permission.WRITE_DEVICE_CONFIG;
import static android.Manifest.permission.READ_WRITE_SYNC_DISABLED_MODE_CONFIG;
import static android.Manifest.permission.DUMP;

import android.Manifest;
import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.provider.DeviceConfigServiceManager;
import android.provider.DeviceConfigInitializer;
import android.provider.aidl.IDeviceConfigManager;
import android.provider.flags.Flags;
import android.ravenwood.annotation.RavenwoodKeepWholeClass;
import android.ravenwood.annotation.RavenwoodRedirect;
import android.ravenwood.annotation.RavenwoodRedirectionClass;
import android.ravenwood.annotation.RavenwoodThrow;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;

import com.android.internal.annotations.GuardedBy;
import com.android.modules.utils.build.SdkLevel;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executor;

/**
 * Device level configuration parameters which can be tuned by a separate configuration service.
 * Namespaces that end in "_native" such as {@link #NAMESPACE_NETD_NATIVE} are intended to be used
 * by native code and should be pushed to system properties to make them accessible.
 *
 * @hide
 */
@SystemApi
@RavenwoodKeepWholeClass
@RavenwoodRedirectionClass("DeviceConfig_host")
public final class DeviceConfig {

    /**
     * The name of the service that provides the logic to these APIs
     *
     * @hide
     */
    public static final String SERVICE_NAME = "device_config_updatable";

    /**
     * Namespace for all accessibility related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_ACCESSIBILITY = "accessibility";

    /**
     * Namespace for activity manager related features. These features will be applied
     * immediately upon change.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_ACTIVITY_MANAGER = "activity_manager";

    /**
     * Namespace for activity manager, specific to the "component alias" feature. We needed a
     * different namespace in order to avoid phonetype from resetting it.
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_ACTIVITY_MANAGER_COMPONENT_ALIAS = "activity_manager_ca";

    /**
     * Namespace for features related to auto pin confirmation.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_AUTO_PIN_CONFIRMATION = "auto_pin_confirmation";

    /**
     * Namespace for all activity manager related features that are used at the native level.
     * These features are applied at reboot.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_ACTIVITY_MANAGER_NATIVE_BOOT =
            "activity_manager_native_boot";

    /**
     * Namespace for AlarmManager configurations.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_ALARM_MANAGER = "alarm_manager";

    /**
     * Namespace for all app compat related features.  These features will be applied
     * immediately upon change.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_APP_COMPAT = "app_compat";

    /**
     * Namespace for all app hibernation related features.
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_APP_HIBERNATION = "app_hibernation";

    /**
     * Namespace for all AppSearch related features.
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_APPSEARCH = "appsearch";

    /**
     * Namespace for app standby configurations.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_APP_STANDBY = "app_standby";

    /**
     * Namespace for all App Cloning related features.
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_APP_CLONING = "app_cloning";

    /**
     * Namespace for AttentionManagerService related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_ATTENTION_MANAGER_SERVICE = "attention_manager_service";

    /**
     * Namespace for autofill feature that provides suggestions across all apps when
     * the user interacts with input fields.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_AUTOFILL = "autofill";

    /**
     * Namespace for battery saver feature.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_BATTERY_SAVER = "battery_saver";

    /**
     * Namespace for holding battery stats configuration.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_BATTERY_STATS = "battery_stats";

    /**
     * Namespace for blobstore feature that allows apps to share data blobs.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_BLOBSTORE = "blobstore";

    /**
     * Namespace for all Bluetooth related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_BLUETOOTH = "bluetooth";

    /**
     * Namespace for features relating to android core experiments team internal usage.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_CORE_EXPERIMENTS_TEAM_INTERNAL = "core_experiments_team_internal";

    /**
     * Namespace for all camera-related features that are used at the native level.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_CAMERA_NATIVE = "camera_native";

    /**
     * Namespace for cellular security related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_CELLULAR_SECURITY = "cellular_security";

    /**
     * Namespace for features relating to clipboard.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_CLIPBOARD = "clipboard";

    /**
     * Namespace for all networking connectivity related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_CONNECTIVITY = "connectivity";

    /**
     * Namespace for CaptivePortalLogin module.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_CAPTIVEPORTALLOGIN = "captive_portal_login";

    /**
     * Namespace for all EdgeTpu related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_EDGETPU_NATIVE = "edgetpu_native";

    /**
     * Namespace for all HealthFitness related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_HEALTH_FITNESS = "health_fitness";

    /**
     * Namespace for Tethering module.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_TETHERING = "tethering";


    /**
     * Namespace for Nearby module.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_NEARBY = "nearby";

    /**
     * Namespace for content capture feature used by on-device machine intelligence
     * to provide suggestions in a privacy-safe manner.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_CONTENT_CAPTURE = "content_capture";

    /**
     * Namespace for credential manager.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_CREDENTIAL = "credential_manager";

    /**
     * Namespace for device idle configurations.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_DEVICE_IDLE = "device_idle";

    /**
     * Namespace for how dex runs. The feature requires a reboot to reach a clean state.
     *
     * @deprecated No longer used
     * @hide
     */
    @Deprecated
    @SystemApi
    public static final String NAMESPACE_DEX_BOOT = "dex_boot";

    /**
     * Namespace for display manager related features. The names to access the properties in this
     * namespace should be defined in {@link android.hardware.display.DisplayManager}.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_DISPLAY_MANAGER = "display_manager";

    /**
     * Namespace for all Game Driver features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_GAME_DRIVER = "game_driver";

    /**
     * Namespace for all HDMI Control features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_HDMI_CONTROL = "hdmi_control";

    /**
     * Namespace for all input-related features that are used at the native level.
     * These features are applied at reboot.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_INPUT_NATIVE_BOOT = "input_native_boot";

    /**
     * Namespace for attention-based features provided by on-device machine intelligence.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_INTELLIGENCE_ATTENTION = "intelligence_attention";

    /**
     * Definitions for properties related to Content Suggestions.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_INTELLIGENCE_CONTENT_SUGGESTIONS =
            "intelligence_content_suggestions";

    /**
     * Namespace for JobScheduler configurations.
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_JOB_SCHEDULER = "jobscheduler";

    /**
     * Namespace for all lmkd related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_LMKD_NATIVE = "lmkd_native";

    /**
     * Namespace for all location related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_LOCATION = "location";

    /**
     * Namespace for all media related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_MEDIA = "media";

    /**
     * Namespace for all media native related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_MEDIA_NATIVE = "media_native";

    /**
     * Namespace for all Kernel Multi-Gen LRU feature.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_MGLRU_NATIVE = "mglru_native";

    /**
     * Namespace for all memory management related features.
     *
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_MMD_DEVICE_CONFIG)
    public static final String NAMESPACE_MM = "mm";

    /**
     * Namespace for all mmd native related features.
     *
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_MMD_DEVICE_CONFIG)
    public static final String NAMESPACE_MMD_NATIVE = "mmd_native";

    /**
     * Namespace for all netd related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_NETD_NATIVE = "netd_native";

    /**
     * Namespace for all Android NNAPI related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_NNAPI_NATIVE = "nnapi_native";

    /**
     * Namespace for all OnDevicePersonalization related feature.
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_ON_DEVICE_PERSONALIZATION = "on_device_personalization";

    /**
     * Namespace for features related to the Package Manager Service.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_PACKAGE_MANAGER_SERVICE = "package_manager_service";

    /**
     * Namespace for features related to the Profcollect native Service.
     * These features are applied at reboot.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_PROFCOLLECT_NATIVE_BOOT = "profcollect_native_boot";

    /**
     * Namespace for features related to Reboot Readiness detection.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_REBOOT_READINESS = "reboot_readiness";

    /**
     * Namespace for Remote Key Provisioning related features.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_REMOTE_KEY_PROVISIONING_NATIVE =
            "remote_key_provisioning_native";

    /**
     * Namespace for Rollback flags that are applied immediately.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_ROLLBACK = "rollback";

    /**
     * Namespace for Rollback flags that are applied after a reboot.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_ROLLBACK_BOOT = "rollback_boot";

    /**
     * Namespace for Rotation Resolver Manager Service.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_ROTATION_RESOLVER = "rotation_resolver";

    /**
     * Namespace for all runtime related features that don't require a reboot to become active.
     * There are no feature flags using NAMESPACE_RUNTIME.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_RUNTIME = "runtime";

    /**
     * Namespace for all runtime related features that require system properties for accessing
     * the feature flags from C++ or Java language code. One example is the app image startup
     * cache feature use_app_image_startup_cache.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_RUNTIME_NATIVE = "runtime_native";

    /**
     * Namespace for all runtime native boot related features. Boot in this case refers to the
     * fact that the properties only take effect after rebooting the device.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_RUNTIME_NATIVE_BOOT = "runtime_native_boot";

    /**
     * Namespace for system scheduler related features. These features will be applied
     * immediately upon change.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_SCHEDULER = "scheduler";

    /**
     * Namespace for all SdkSandbox related features.
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_SDK_SANDBOX = "sdk_sandbox";

    /**
     * Namespace for settings statistics features.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_SETTINGS_STATS = "settings_stats";

    /**
     * Namespace for all statsd java features that can be applied immediately.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_STATSD_JAVA = "statsd_java";

    /**
     * Namespace for all statsd java features that are applied on boot.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_STATSD_JAVA_BOOT = "statsd_java_boot";

    /**
     * Namespace for all statsd native features that can be applied immediately.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_STATSD_NATIVE = "statsd_native";

    /**
     * Namespace for all statsd native features that are applied on boot.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_STATSD_NATIVE_BOOT = "statsd_native_boot";

    /**
     * Namespace for storage-related features.
     *
     * @deprecated Replace storage namespace with storage_native_boot.
     * @hide
     */
    @Deprecated
    @SystemApi
    public static final String NAMESPACE_STORAGE = "storage";

    /**
     * Namespace for storage-related features, including native and boot.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_STORAGE_NATIVE_BOOT = "storage_native_boot";

    /**
     * Namespace for all AdServices related features.
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_ADSERVICES = "adservices";

    /**
     * Namespace for all SurfaceFlinger features that are used at the native level.
     * These features are applied on boot or after reboot.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_SURFACE_FLINGER_NATIVE_BOOT =
            "surface_flinger_native_boot";

    /**
     * Namespace for swcodec native related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_SWCODEC_NATIVE = "swcodec_native";


    /**
     * Namespace for System UI related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_SYSTEMUI = "systemui";

    /**
     * Namespace for system time and time zone detection related features / behavior.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_SYSTEM_TIME = "system_time";

    /**
     * Namespace for TARE configurations.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_TARE = "tare";

    /**
     * Telephony related properties.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_TELEPHONY = "telephony";

    /**
     * Namespace for TextClassifier related features.
     *
     * @hide
     * @see android.provider.Settings.Global.TEXT_CLASSIFIER_CONSTANTS
     */
    @SystemApi
    public static final String NAMESPACE_TEXTCLASSIFIER = "textclassifier";

    /**
     * Namespace for contacts provider related features.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_CONTACTS_PROVIDER = "contacts_provider";

    /**
     * Namespace for settings ui related features
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_SETTINGS_UI = "settings_ui";

    /**
     * Namespace for android related features, i.e. for flags that affect not just a single
     * component, but the entire system.
     *
     * The keys for this namespace are defined in {@link AndroidDeviceConfig}.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_ANDROID = "android";

    /**
     * Namespace for window manager related features.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_WINDOW_MANAGER = "window_manager";

    /**
     * Namespace for window manager features accessible by native code and
     * loaded once per boot.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_WINDOW_MANAGER_NATIVE_BOOT = "window_manager_native_boot";

    /**
     * Definitions for selection toolbar related functions.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_SELECTION_TOOLBAR = "selection_toolbar";

    /**
     * Definitions for voice interaction related functions.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_VOICE_INTERACTION = "voice_interaction";

    /**
     * Namespace for DevicePolicyManager related features.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_DEVICE_POLICY_MANAGER =
            "device_policy_manager";

    /**
     * List of namespaces which can be read without READ_DEVICE_CONFIG permission
     *
     * @hide
     */
    @NonNull
    private static final List<String> PUBLIC_NAMESPACES =
            Arrays.asList(NAMESPACE_TEXTCLASSIFIER, NAMESPACE_RUNTIME, NAMESPACE_STATSD_JAVA,
                    NAMESPACE_STATSD_JAVA_BOOT, NAMESPACE_SELECTION_TOOLBAR, NAMESPACE_AUTOFILL,
                    NAMESPACE_DEVICE_POLICY_MANAGER, NAMESPACE_CONTENT_CAPTURE);
    /**
     * Privacy related properties definitions.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_PRIVACY = "privacy";

    /**
     * Namespace for biometrics related features
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_BIOMETRICS = "biometrics";

    /**
     * Permission related properties definitions.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_PERMISSIONS = "permissions";

    /**
     * Namespace for ota related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_OTA = "ota";

    /**
     * Namespace for all widget related features.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_WIDGET = "widget";

    /**
     * Namespace for connectivity thermal power manager features.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_CONNECTIVITY_THERMAL_POWER_MANAGER =
            "connectivity_thermal_power_manager";

    /**
     * Namespace for configuration related features.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_CONFIGURATION = "configuration";

    /**
     * LatencyTracker properties definitions.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_LATENCY_TRACKER = "latency_tracker";

    /**
     * InteractionJankMonitor properties definitions.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    @SuppressLint("IntentName")
    public static final String NAMESPACE_INTERACTION_JANK_MONITOR = "interaction_jank_monitor";

    /**
     * Namespace for game overlay related features.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_GAME_OVERLAY = "game_overlay";

    /**
     * Namespace for Android Virtualization Framework related features accessible by native code.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_VIRTUALIZATION_FRAMEWORK_NATIVE =
            "virtualization_framework_native";

    /**
     * Namespace for Constrain Display APIs related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_CONSTRAIN_DISPLAY_APIS = "constrain_display_apis";

    /**
     * Namespace for App Compat Overrides related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_APP_COMPAT_OVERRIDES = "app_compat_overrides";

    /**
     * Namespace for all ultra wideband (uwb) related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_UWB = "uwb";

    /**
     * Namespace for AmbientContextEventManagerService related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_AMBIENT_CONTEXT_MANAGER_SERVICE =
            "ambient_context_manager_service";

    /**
     * Namespace for WearableSensingManagerService related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_WEARABLE_SENSING =
            "wearable_sensing";

    /**
     * Namespace for Vendor System Native related features.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_VENDOR_SYSTEM_NATIVE = "vendor_system_native";

    /**
     * Namespace for Vendor System Native Boot related features.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_VENDOR_SYSTEM_NATIVE_BOOT = "vendor_system_native_boot";

    /**
     * Namespace for memory safety related features (e.g. MTE) that need a reboot to be applied
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_MEMORY_SAFETY_NATIVE_BOOT = "memory_safety_native_boot";

    /**
     * Namespace for memory safety related features (e.g. MTE)
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_MEMORY_SAFETY_NATIVE = "memory_safety_native";

    /**
     * Namespace for wear OS platform features.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_WEAR = "wear";

    /**
     * Namespace for the input method manager platform features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_INPUT_METHOD_MANAGER = "input_method_manager";

    /**
     * Namespace for backup and restore service related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_BACKUP_AND_RESTORE = "backup_and_restore";

    /**
     * Namespace for ARC App Compat related features.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_ARC_APP_COMPAT = "arc_app_compat";

    /**
     * Namespace for remote authentication features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_REMOTE_AUTH = "remote_auth";


    /**
     * Namespace for tethering module native features.
     * Flags defined in this namespace are only usable on
     * {@link android.os.Build.VERSION_CODES#UPSIDE_DOWN_CAKE} and newer.
     * On older Android releases, they will not be propagated to native code.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_TETHERING_NATIVE =
            "tethering_u_or_later_native";

    /**
     * Namespace for all near field communication (nfc) related features.
     *
     * @hide
     */
    @SystemApi
    public static final String NAMESPACE_NFC = "nfc";

    /**
     * The modes that can be used when disabling syncs to the 'config' settings.
     * @hide
     */
    @IntDef(prefix = "SYNC_DISABLED_MODE_",
            value = { SYNC_DISABLED_MODE_NONE, SYNC_DISABLED_MODE_PERSISTENT,
                    SYNC_DISABLED_MODE_UNTIL_REBOOT })
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
    public @interface SyncDisabledMode {}

    /**
     * Sync is not disabled.
     *
     * @hide
     */
    @SystemApi
    public static final int SYNC_DISABLED_MODE_NONE = 0;

    /**
     * Disabling of Config bulk update / syncing is persistent, i.e. it survives a device
     * reboot.
     *
     * @hide
     */
    @SystemApi
    public static final int SYNC_DISABLED_MODE_PERSISTENT = 1;

    /**
     * Disabling of Config bulk update / syncing is not persistent, i.e. it will
     * not survive a device reboot.
     *
     * @hide
     */
    @SystemApi
    public static final int SYNC_DISABLED_MODE_UNTIL_REBOOT = 2;


    // NOTE: this API is only used by the framework code, but using MODULE_LIBRARIES causes a
    // build-time error on CtsDeviceConfigTestCases, so it's using PRIVILEGED_APPS.
    /**
     * Optional argument to {@link #dump(ParcelFileDescriptor, PrintWriter, String, String[])} to
     * indicate that the next argument is a namespace. How {@code dump()} will handle that
     * argument is documented there.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.PRIVILEGED_APPS)
    @FlaggedApi(Flags.FLAG_DUMP_IMPROVEMENTS)
    public static final String DUMP_ARG_NAMESPACE = "--namespace";

    private static final Object sLock = new Object();
    @GuardedBy("sLock")
    private static ArrayMap<OnPropertiesChangedListener, Pair<String, Executor>> sListeners =
            new ArrayMap<>();
    @GuardedBy("sLock")
    private static Map<String, Pair<ContentObserver, Integer>> sNamespaces = new HashMap<>();
    private static final String TAG = "DeviceConfig";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final DeviceConfigDataStore sDataStore = newDataStore();

    @RavenwoodRedirect
    private static DeviceConfigDataStore newDataStore() {
        return new SettingsConfigDataStore();
    }

    private static final String DEVICE_CONFIG_OVERRIDES_NAMESPACE =
            "device_config_overrides";

    /**
     * Interface for monitoring callback functions.
     *
     * @hide
     */
    @SystemApi
    public interface MonitorCallback {
        /**
         * Callback for updating a namespace.
         * Reports that a config in the given namespace has changed.
         * Isn't called for {@link DeviceConfig#getPublicNamespaces() public namespaces}.
         *
         * @param updatedNamespace the namespace, within which at least one config has changed.
         * @hide
         */
        @SystemApi
        void onNamespaceUpdate(@NonNull String updatedNamespace);

        /**
         * Callback for accessing device config.
         * Reports an access to a the given namespace and the given calling package.
         * Isn't called for {@link DeviceConfig#getPublicNamespaces() public namespaces}.
         *
         * @param callingPackage the calling package id.
         * @param namespace the namespace, within which one of its config has been accessed.
         * @hide
         */
        @SystemApi
        void onDeviceConfigAccess(@NonNull String callingPackage, @NonNull String namespace);
    }

    // Should never be invoked
    private DeviceConfig() {
    }

    /**
     * Look up the value of a property for a particular namespace.
     *
     * @param namespace The namespace containing the property to look up.
     * @param name      The name of the property to look up.
     * @return the corresponding value, or null if not present.
     * @hide
     */
    @SystemApi
    @Nullable
    public static String getProperty(@NonNull String namespace, @NonNull String name) {
        // Fetch all properties for the namespace at once and cache them in the local process, so we
        // incur the cost of the IPC less often. Lookups happen much more frequently than updates,
        // and we want to optimize the former.
        return getProperties(namespace, name).getString(name, null);
    }

    /**
     * Look up the values of multiple properties for a particular namespace. The lookup is atomic,
     * such that the values of these properties cannot change between the time when the first is
     * fetched and the time when the last is fetched.
     * <p>
     * Each call to {@link #setProperties(Properties)} is also atomic and ensures that either none
     * or all of the change is picked up here, but never only part of it.
     *
     * If there are any local overrides applied, they will take precedence over underlying values.
     *
     * @param namespace The namespace containing the properties to look up.
     * @param names     The names of properties to look up, or empty to fetch all properties for the
     *                  given namespace.
     * @return {@link Properties} object containing the requested properties. This reflects the
     *     state of these properties at the time of the lookup, and is not updated to reflect any
     *     future changes. The keyset of this Properties object will contain only the intersection
     *     of properties already set and properties requested via the names parameter. Properties
     *     that are already set but were not requested will not be contained here. Properties that
     *     are not set, but were requested will not be contained here either.
     * @hide
     */
    @SystemApi
    @NonNull
    @RequiresPermission(READ_DEVICE_CONFIG)
    public static Properties getProperties(@NonNull String namespace, @NonNull String... names) {
        Properties properties = getPropertiesWithoutOverrides(namespace, names);
        if (SdkLevel.isAtLeastV()) {
            applyOverrides(properties);
        }
        return properties;
    }

    @NonNull
    private static Properties getPropertiesWithoutOverrides(@NonNull String namespace,
        @NonNull String... names) {
        return sDataStore.getProperties(namespace, names);
    }

    private static void applyOverrides(@NonNull Properties properties) {
        Properties overrides =
                getPropertiesWithoutOverrides(DEVICE_CONFIG_OVERRIDES_NAMESPACE);

        final String prefix = properties.getNamespace() + ':';
        final int prefixLength = prefix.length();

        for (var override : overrides.getMap().entrySet()) {
            String fullKey = override.getKey();
            String value = override.getValue();
            if (value != null && fullKey.startsWith(prefix)) {
                properties.setString(fullKey.substring(prefixLength), value);
            }
        }
    }

    /**
     * List all stored flags.
     *
     * The keys take the form {@code namespace/name}, and the values are the flag values.
     *
     * @hide
     */
    @SystemApi
    @NonNull
    public static Set<Properties> getAllProperties() {
        Map<String, String> properties = sDataStore.getAllProperties();
        Map<String, Map<String, String>> propertyMaps = new HashMap<>();
        for (String flag : properties.keySet()) {
            String[] namespaceAndFlag = flag.split("/");
            String namespace = namespaceAndFlag[0];
            String flagName = namespaceAndFlag[1];
            String override =
                    getProperty(DEVICE_CONFIG_OVERRIDES_NAMESPACE, namespace + ":" + flagName);

            String value = override != null ? override : properties.get(flag);

            if (!propertyMaps.containsKey(namespace)) {
                propertyMaps.put(namespace, new HashMap<>());
            }
            propertyMaps.get(namespace).put(flagName, value);
        }

        HashSet<Properties> result = new HashSet<>();
        for (Map.Entry<String, Map<String, String>> entry : propertyMaps.entrySet()) {
            result.add(new Properties(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /**
     * Look up the String value of a property for a particular namespace.
     *
     * @param namespace    The namespace containing the property to look up.
     * @param name         The name of the property to look up.
     * @param defaultValue The value to return if the property does not exist or has no non-null
     *                     value.
     * @return the corresponding value, or defaultValue if none exists.
     * @hide
     */
    @SystemApi
    @RequiresPermission(READ_DEVICE_CONFIG)
    @Nullable
    public static String getString(@NonNull String namespace, @NonNull String name,
            @Nullable String defaultValue) {
        String value = getProperty(namespace, name);
        return value != null ? value : defaultValue;
    }

    /**
     * Look up the boolean value of a property for a particular namespace.
     *
     * @param namespace The namespace containing the property to look up.
     * @param name      The name of the property to look up.
     * @param defaultValue The value to return if the property does not exist or has no non-null
     *                     value.
     * @return the corresponding value, or defaultValue if none exists.
     * @hide
     */
    @SystemApi
    public static boolean getBoolean(@NonNull String namespace, @NonNull String name,
            boolean defaultValue) {
        String value = getProperty(namespace, name);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Look up the int value of a property for a particular namespace.
     *
     * @param namespace The namespace containing the property to look up.
     * @param name      The name of the property to look up.
     * @param defaultValue The value to return if the property does not exist, has no non-null
     *                     value, or fails to parse into an int.
     * @return the corresponding value, or defaultValue if either none exists or it does not parse.
     * @hide
     */
    @SystemApi
    @RequiresPermission(READ_DEVICE_CONFIG)
    public static int getInt(@NonNull String namespace, @NonNull String name, int defaultValue) {
        String value = getProperty(namespace, name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Parsing integer failed for " + namespace + ":" + name);
            return defaultValue;
        }
    }

    /**
     * Look up the long value of a property for a particular namespace.
     *
     * @param namespace The namespace containing the property to look up.
     * @param name      The name of the property to look up.
     * @param defaultValue The value to return if the property does not exist, has no non-null
     *                     value, or fails to parse into a long.
     * @return the corresponding value, or defaultValue if either none exists or it does not parse.
     * @hide
     */
    @SystemApi
    @RequiresPermission(READ_DEVICE_CONFIG)
    public static long getLong(@NonNull String namespace, @NonNull String name, long defaultValue) {
        String value = getProperty(namespace, name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Parsing long failed for " + namespace + ":" + name);
            return defaultValue;
        }
    }

    /**
     * Look up the float value of a property for a particular namespace.
     *
     * @param namespace The namespace containing the property to look up.
     * @param name      The name of the property to look up.
     * @param defaultValue The value to return if the property does not exist, has no non-null
     *                     value, or fails to parse into a float.
     * @return the corresponding value, or defaultValue if either none exists or it does not parse.
     * @hide
     */
    @SystemApi
    @RequiresPermission(READ_DEVICE_CONFIG)
    public static float getFloat(@NonNull String namespace, @NonNull String name,
            float defaultValue) {
        String value = getProperty(namespace, name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Parsing float failed for " + namespace + ":" + name);
            return defaultValue;
        }
    }

    /**
     * Set flag {@code namespace/name} to {@code value}, and ignores server-updates for this flag.
     *
     * Can still be called even if there is no underlying value set.
     *
     * Returns {@code true} if successful, or {@code false} if the storage implementation throws
     * errors.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(WRITE_DEVICE_CONFIG)
    public static boolean setLocalOverride(@NonNull String namespace, @NonNull String name,
        @NonNull String value) {
        return setProperty(DEVICE_CONFIG_OVERRIDES_NAMESPACE, namespace + ":" + name, value, false);
    }

    /**
     * Clear all local sticky overrides.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(WRITE_DEVICE_CONFIG)
    public static void clearAllLocalOverrides() {
        Properties overrides = getProperties(DEVICE_CONFIG_OVERRIDES_NAMESPACE);
        for (String overrideName : overrides.getKeyset()) {
            deleteProperty(DEVICE_CONFIG_OVERRIDES_NAMESPACE, overrideName);
        }
    }

    /**
     * Clear local sticky override for flag {@code namespace/name}.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(WRITE_DEVICE_CONFIG)
    public static void clearLocalOverride(@NonNull String namespace,
        @NonNull String name) {
        deleteProperty(DEVICE_CONFIG_OVERRIDES_NAMESPACE, namespace + ":" + name);
    }

    /**
     * Return a map containing all flags that have been overridden.
     *
     * The keys of the outer map are namespaces. They keys of the inner maps are
     * flag names. The values of the inner maps are the underlying flag values
     * (not to be confused with their overridden values).
     *
     * @hide
     */
    @NonNull
    @SystemApi
    public static Map<String, Map<String, String>> getUnderlyingValuesForOverriddenFlags() {
        Properties overrides = getProperties(DEVICE_CONFIG_OVERRIDES_NAMESPACE);
        HashMap<String, Map<String, String>> result = new HashMap<>();
        for (Map.Entry<String, String> entry : overrides.getPropertyValues().entrySet()) {
            String[] namespaceAndFlag = entry.getKey().split(":");
            String namespace = namespaceAndFlag[0];
            String flag = namespaceAndFlag[1];

            String actualValue =
                    getPropertiesWithoutOverrides(namespace, flag)
                    .getString(flag, null);
            if (result.get(namespace) != null) {
                result.get(namespace).put(flag, actualValue);
            } else {
                HashMap<String, String> innerMap = new HashMap<>();
                innerMap.put(flag, actualValue);
                result.put(namespace, innerMap);
            }
        }
        return result;
    }

    /**
     * Create a new property with the provided name and value in the provided namespace, or
     * update the value of such a property if it already exists. The same name can exist in multiple
     * namespaces and might have different values in any or all namespaces.
     * <p>
     * The method takes an argument indicating whether to make the value the default for this
     * property.
     * <p>
     * All properties stored for a particular scope can be reverted to their default values
     * by passing the namespace to {@link #resetToDefaults(int, String)}.
     *
     * @param namespace   The namespace containing the property to create or update.
     * @param name        The name of the property to create or update.
     * @param value       The value to store for the property.
     * @param makeDefault Whether to make the new value the default one.
     * @return {@code true} if the value was set, {@code false} if the storage implementation throws
     * errors.
     * @hide
     * @see #resetToDefaults(int, String).
     */
    @SystemApi
    @RequiresPermission(anyOf = {WRITE_DEVICE_CONFIG, WRITE_ALLOWLISTED_DEVICE_CONFIG})
    public static boolean setProperty(@NonNull String namespace, @NonNull String name,
            @Nullable String value, boolean makeDefault) {
        return sDataStore.setProperty(namespace, name, value, makeDefault);
    }

    /**
     * Set all of the properties for a specific namespace. Pre-existing properties will be updated
     * and new properties will be added if necessary. Any pre-existing properties for the specific
     * namespace which are not part of the provided {@link Properties} object will be deleted from
     * the namespace. These changes are all applied atomically, such that no calls to read or reset
     * these properties can happen in the middle of this update.
     * <p>
     * Each call to {@link #getProperties(String, String...)} is also atomic and ensures that either
     * none or all of this update is picked up, but never only part of it.
     *
     * @param properties the complete set of properties to set for a specific namespace.
     * @throws BadConfigException if the provided properties are banned by RescueParty.
     * @return {@code true} if the values were set, {@code false} otherwise.
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {WRITE_DEVICE_CONFIG, WRITE_ALLOWLISTED_DEVICE_CONFIG})
    public static boolean setProperties(@NonNull Properties properties) throws BadConfigException {
        return sDataStore.setProperties(properties);
    }

    /**
     * Delete a property with the provided name and value in the provided namespace
     *
     * @param namespace   The namespace containing the property to delete.
     * @param name        The name of the property to delete.
     * @return {@code true} if the property was deleted or it did not exist in the first place.
     * Return {@code false} if the storage implementation throws errors.
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {WRITE_DEVICE_CONFIG, WRITE_ALLOWLISTED_DEVICE_CONFIG})
    public static boolean deleteProperty(@NonNull String namespace, @NonNull String name) {
        return sDataStore.deleteProperty(namespace, name);
    }

    /**
     * Reset properties to their default values by removing the underlying values.
     * <p>
     * The method accepts an optional namespace parameter. If provided, only properties set within
     * that namespace will be reset. Otherwise, all properties will be reset.
     * <p>
     * Note: This method should only be used by {@link com.android.server.RescueParty}. It was
     * designed to be used in the event of boot or crash loops caused by flag changes. It does not
     * revert flag values to defaults - instead it removes the property entirely which causes the
     * consumer of the flag to use hardcoded defaults upon retrieval.
     * <p>
     * To clear values for a namespace without removing the underlying properties, construct a
     * {@link Properties} object with the caller's namespace and either an empty flag map, or some
     * snapshot of flag values. Then use {@link #setProperties(Properties)} to remove all flags
     * under the namespace, or set them to the values in the snapshot.
     * <p>
     * To revert values for testing, one should mock DeviceConfig using
     * {@link com.android.server.testables.TestableDeviceConfig} where possible. Otherwise, fallback
     * to using {@link #setProperties(Properties)} as outlined above.
     *
     * @param resetMode The reset mode to use.
     * @param namespace Optionally, the specific namespace which resets will be limited to.
     * @hide
     * @see #setProperty(String, String, String, boolean)
     */
    @SystemApi
    @RavenwoodThrow
    @RequiresPermission(anyOf = {WRITE_DEVICE_CONFIG, WRITE_ALLOWLISTED_DEVICE_CONFIG})
    public static void resetToDefaults(int resetMode, @Nullable String namespace) {
        sDataStore.resetToDefaults(resetMode, namespace);
    }

    /**
     * Disables or re-enables bulk modifications ({@link #setProperties(Properties)}) to device
     * config values. This is intended for use during tests to prevent a sync operation clearing
     * config values which could influence the outcome of the tests, i.e. by changing behavior.
     *
     * @param syncDisabledMode the mode to use, see {@link Settings.Config#SYNC_DISABLED_MODE_NONE},
     *     {@link Settings.Config#SYNC_DISABLED_MODE_PERSISTENT} and {@link
     *     Settings.Config#SYNC_DISABLED_MODE_UNTIL_REBOOT}
     *
     * @see #getSyncDisabledMode()
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {WRITE_DEVICE_CONFIG, READ_WRITE_SYNC_DISABLED_MODE_CONFIG})
    public static void setSyncDisabledMode(int syncDisabledMode) {
        sDataStore.setSyncDisabledMode(syncDisabledMode);
    }

    /**
     * Returns the current mode of sync disabling.
     *
     * @see #setSyncDisabledMode(int)
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {WRITE_DEVICE_CONFIG, READ_WRITE_SYNC_DISABLED_MODE_CONFIG})
    public static int getSyncDisabledMode() {
        return sDataStore.getSyncDisabledMode();
    }

    /**
     * Add a listener for property changes.
     * <p>
     * This listener will be called whenever properties in the specified namespace change. Callbacks
     * will be made on the specified executor. Future calls to this method with the same listener
     * will replace the old namespace and executor. Remove the listener entirely by calling
     * {@link #removeOnPropertiesChangedListener(OnPropertiesChangedListener)}.
     *
     * @param namespace                   The namespace containing properties to monitor.
     * @param executor                    The executor which will be used to run callbacks.
     * @param onPropertiesChangedListener The listener to add.
     * @hide
     * @see #removeOnPropertiesChangedListener(OnPropertiesChangedListener)
     */
    @SystemApi
    public static void addOnPropertiesChangedListener(
            @NonNull String namespace,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OnPropertiesChangedListener onPropertiesChangedListener) {
        synchronized (sLock) {
            Pair<String, Executor> oldNamespace = sListeners.get(onPropertiesChangedListener);
            if (oldNamespace == null) {
                // Brand new listener, add it to the list.
                sListeners.put(onPropertiesChangedListener, new Pair<>(namespace, executor));
                incrementNamespace(namespace);
            } else if (namespace.equals(oldNamespace.first)) {
                // Listener is already registered for this namespace, update executor just in case.
                sListeners.put(onPropertiesChangedListener, new Pair<>(namespace, executor));
            } else {
                // Update this listener from an old namespace to the new one.
                decrementNamespace(sListeners.get(onPropertiesChangedListener).first);
                sListeners.put(onPropertiesChangedListener, new Pair<>(namespace, executor));
                incrementNamespace(namespace);
            }
        }
    }

    // NOTE: this API is only used by the framework code, but using MODULE_LIBRARIES causes a
    // build-time error on CtsDeviceConfigTestCases, so it's using PRIVILEGED_APPS.
    /**
     * Dumps internal state into the given {@code fd} or {@code printWriter}.
     *
     * <p><b>Note:</b> Currently the only supported argument is {@link #DUMP_ARG_NAMESPACE} which
     * will filter the output using a substring of the next argument. But other arguments might be
     * dynamically added in the future, without documentation - this method is meant only for
     * debugging purposes, and should not be used as a formal API.
     *
     * @param printWriter print writer that will output the dump state.
     * @param prefix prefix added to each line
     * @param args (optional) arguments passed by {@code dumpsys}.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.PRIVILEGED_APPS)
    @FlaggedApi(Flags.FLAG_DUMP_IMPROVEMENTS)
    public static void dump(@NonNull PrintWriter printWriter, @NonNull String dumpPrefix,
            @Nullable String[] args) {
        if (DEBUG) {
            Slog.d(TAG, "dump(): args=" + Arrays.toString(args));
        }
        Objects.requireNonNull(printWriter, "printWriter cannot be null");

        Comparator<OnPropertiesChangedListener> comparator = (o1, o2) -> o1.toString()
                .compareTo(o2.toString());
        TreeMap<String, Set<OnPropertiesChangedListener>> listenersByNamespace  =
                new TreeMap<>();
        ArraySet<OnPropertiesChangedListener> uniqueListeners = new ArraySet<>();
        String filter = null;
        if (args.length > 0) {
            switch (args[0]) {
                case DUMP_ARG_NAMESPACE:
                    if (args.length < 2) {
                        throw new IllegalArgumentException(
                                "argument " + DUMP_ARG_NAMESPACE + " requires an extra argument");
                    }
                    filter = args[1];
                    if (DEBUG) {
                        Slog.d(TAG, "dump(): setting filter as " + filter);
                    }
                    break;
                default:
                    Slog.w(TAG, "dump(): ignoring invalid arguments: " + Arrays.toString(args));
                    break;
            }
        }
        int listenersSize;
        synchronized (sLock) {
            listenersSize = sListeners.size();
            for (int i = 0; i < listenersSize; i++) {
                var namespace = sListeners.valueAt(i).first;
                if (filter != null && !namespace.contains(filter)) {
                    continue;
                }
                var listener = sListeners.keyAt(i);
                var listeners = listenersByNamespace.get(namespace);
                if (listeners == null) {
                    // Life would be so much easier if Android provided a MultiMap implementation...
                    listeners = new TreeSet<>(comparator);
                    listenersByNamespace.put(namespace, listeners);
                }
                listeners.add(listener);
                uniqueListeners.add(listener);
            }
        }
        printWriter.printf("%s%d listeners for %d namespaces:\n", dumpPrefix, uniqueListeners.size(),
                listenersByNamespace.size());
        for (var entry : listenersByNamespace.entrySet()) {
            var namespace = entry.getKey();
            var listeners = entry.getValue();
            printWriter.printf("%s%s: %d listeners\n", dumpPrefix, namespace, listeners.size());
            for (var listener : listeners) {
                printWriter.printf("%s%s%s\n", dumpPrefix, dumpPrefix, listener);
            }
        }
    }

    /**
     * Remove a listener for property changes. The listener will receive no further notification of
     * property changes.
     *
     * @param onPropertiesChangedListener The listener to remove.
     * @hide
     * @see #addOnPropertiesChangedListener(String, Executor, OnPropertiesChangedListener)
     */
    @SystemApi
    public static void removeOnPropertiesChangedListener(
            @NonNull OnPropertiesChangedListener onPropertiesChangedListener) {
        Objects.requireNonNull(onPropertiesChangedListener);
        synchronized (sLock) {
            if (sListeners.containsKey(onPropertiesChangedListener)) {
                decrementNamespace(sListeners.get(onPropertiesChangedListener).first);
                sListeners.remove(onPropertiesChangedListener);
            }
        }
    }

    /**
     * Setter callback for monitoring Config table.
     *
     * @param executor the {@link Executor} on which to invoke the callback
     * @param callback callback to set
     *
     * @hide
     */
    @SystemApi
    @RavenwoodThrow
    @RequiresPermission(Manifest.permission.MONITOR_DEVICE_CONFIG_ACCESS)
    public static void setMonitorCallback(
            @NonNull ContentResolver resolver,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull MonitorCallback callback) {
        sDataStore.setMonitorCallback(resolver, executor, callback);
    }

    /**
     * Clear callback for monitoring Config table.
     * this may only be used to clear callback function registered by
     * {@link DeviceConfig#setMonitorCallback}
     * @hide
     */
    @SystemApi
    @RavenwoodThrow
    @RequiresPermission(Manifest.permission.MONITOR_DEVICE_CONFIG_ACCESS)
    public static void clearMonitorCallback(@NonNull ContentResolver resolver) {
        sDataStore.clearMonitorCallback(resolver);
    }

    /**
     * Increment the count used to represent the number of listeners subscribed to the given
     * namespace. If this is the first (i.e. incrementing from 0 to 1) for the given namespace, a
     * ContentObserver is registered.
     *
     * @param namespace The namespace to increment the count for.
     */
    @GuardedBy("sLock")
    private static void incrementNamespace(@NonNull String namespace) {
        Objects.requireNonNull(namespace);
        Pair<ContentObserver, Integer> namespaceCount = sNamespaces.get(namespace);
        if (namespaceCount != null) {
            sNamespaces.put(namespace, new Pair<>(namespaceCount.first, namespaceCount.second + 1));
        } else {
            // This is a new namespace, register a ContentObserver for it.
            ContentObserver contentObserver = new ContentObserver(null) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    if (uri != null) {
                        handleChange(uri);
                    }
                }
            };
            sDataStore
                    .registerContentObserver(namespace, true, contentObserver);
            sNamespaces.put(namespace, new Pair<>(contentObserver, 1));
        }
    }

    /**
     * Decrement the count used to represent the number of listeners subscribed to the given
     * namespace. If this is the final decrement call (i.e. decrementing from 1 to 0) for the given
     * namespace, the ContentObserver that had been tracking it will be removed.
     *
     * @param namespace The namespace to decrement the count for.
     */
    @GuardedBy("sLock")
    private static void decrementNamespace(@NonNull String namespace) {
        Objects.requireNonNull(namespace);
        Pair<ContentObserver, Integer> namespaceCount = sNamespaces.get(namespace);
        if (namespaceCount == null) {
            // This namespace is not registered and does not need to be decremented
            return;
        } else if (namespaceCount.second > 1) {
            sNamespaces.put(namespace, new Pair<>(namespaceCount.first, namespaceCount.second - 1));
        } else {
            // Decrementing a namespace to zero means we no longer need its ContentObserver.
            sDataStore.unregisterContentObserver(namespaceCount.first);
            sNamespaces.remove(namespace);
        }
    }

    private static void handleChange(@NonNull Uri uri) {
        Objects.requireNonNull(uri);
        List<String> pathSegments = uri.getPathSegments();
        // pathSegments(0) is "config"
        final String namespace = pathSegments.get(1);
        final Properties properties;
        if (pathSegments.size() > 2) {
            String[] keys = new String[pathSegments.size() - 2];
            for (int i = 2; i < pathSegments.size(); ++i) {
                keys[i - 2] = pathSegments.get(i);
            }

            try {
                properties = getProperties(namespace, keys);
            } catch (SecurityException e) {
                // Silently failing to not crash binder or listener threads.
                Slog.e(TAG, "OnPropertyChangedListener update failed: permission violation.");
                return;
            }

            // Make sure all keys are present.
            for (String key : keys) {
                properties.setString(key, properties.getString(key, null));
            }
        } else {
            properties = new Properties.Builder(namespace).build();
        }

        synchronized (sLock) {
            for (int i = 0; i < sListeners.size(); i++) {
                if (namespace.equals(sListeners.valueAt(i).first)) {
                    final OnPropertiesChangedListener listener = sListeners.keyAt(i);
                    sListeners.valueAt(i).second.execute(() -> {
                        listener.onPropertiesChanged(properties);
                    });
                }
            }
        }
    }

    /**
     * Returns list of namespaces that can be read without READ_DEVICE_CONFIG_PERMISSION;
     * @hide
     */
    @SystemApi
    public static @NonNull List<String> getPublicNamespaces() {
        return PUBLIC_NAMESPACES;
    }

    /**
     * Returns list of flags that can be written with adb as non-root.
     * @hide
     */
    @SystemApi
    public static @NonNull Set<String> getAdbWritableFlags() {
        return WritableFlags.ALLOWLIST;
    }

    /**
     * Returns the list of namespaces in which all flags can be written with adb as non-root.
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_DEVICE_CONFIG_WRITABLE_NAMESPACES_API)
    public static @NonNull Set<String> getAdbWritableNamespaces() {
        return WritableNamespaces.ALLOWLIST;
    }

    /**
     * Interface for monitoring changes to properties. Implementations will receive callbacks when
     * properties change, including a {@link Properties} object which contains a single namespace
     * and all of the properties which changed for that namespace. This includes properties which
     * were added, updated, or deleted. This is not necessarily a complete list of all properties
     * belonging to the namespace, as properties which don't change are omitted.
     * <p>
     * Override {@link #onPropertiesChanged(Properties)} to handle callbacks for changes.
     *
     * @hide
     */
    @SystemApi
    public interface OnPropertiesChangedListener {
        /**
         * Called when one or more properties have changed, providing a Properties object with all
         * of the changed properties. This object will contain only properties which have changed,
         * not the complete set of all properties belonging to the namespace.
         *
         * @param properties Contains the complete collection of properties which have changed for a
         *                   single namespace. This includes only those which were added, updated,
         *                   or deleted.
         */
        void onPropertiesChanged(@NonNull Properties properties);
    }

    /**
     * Thrown by {@link #setProperties(Properties)} when a configuration is rejected. This
     * happens if RescueParty has identified a bad configuration and reset the namespace.
     *
     * @hide
     */
    @SystemApi
    public static class BadConfigException extends Exception {}

    /**
     * A mapping of properties to values, as well as a single namespace which they all belong to.
     *
     * @hide
     */
    @SystemApi
    public static class Properties {
        private final String mNamespace;
        private final HashMap<String, String> mMap;
        private Set<String> mKeyset;

        /**
         * Create a mapping of properties to values and the namespace they belong to.
         *
         * @param namespace The namespace these properties belong to.
         * @param keyValueMap A map between property names and property values.
         * @hide
         */
        @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
        public Properties(@NonNull String namespace, @Nullable Map<String, String> keyValueMap) {
            Objects.requireNonNull(namespace);
            mNamespace = namespace;
            mMap = new HashMap();
            if (keyValueMap != null) {
                mMap.putAll(keyValueMap);
            }
        }

        /**
         * @return the namespace all properties within this instance belong to.
         */
        @NonNull
        public String getNamespace() {
            return mNamespace;
        }

        /**
         * @return the non-null set of property names.
         */
        @NonNull
        public Set<String> getKeyset() {
            if (mKeyset == null) {
                mKeyset = Collections.unmodifiableSet(mMap.keySet());
            }
            return mKeyset;
        }

        /**
         * Look up the String value of a property.
         *
         * @param name         The name of the property to look up.
         * @param defaultValue The value to return if the property has not been defined.
         * @return the corresponding value, or defaultValue if none exists.
         */
        @Nullable
        public String getString(@NonNull String name, @Nullable String defaultValue) {
            Objects.requireNonNull(name);
            String value = mMap.get(name);
            return value != null ? value : defaultValue;
        }

        @Nullable
        private String setString(@NonNull String name, @Nullable String value) {
            Objects.requireNonNull(name);
            mKeyset = null;
            return mMap.put(name, value);
        }

        @NonNull
        private Map<String, String> getMap() {
            return mMap;
        }

        /**
         * Look up the boolean value of a property.
         *
         * @param name         The name of the property to look up.
         * @param defaultValue The value to return if the property has not been defined.
         * @return the corresponding value, or defaultValue if none exists.
         */
        public boolean getBoolean(@NonNull String name, boolean defaultValue) {
            Objects.requireNonNull(name);
            String value = mMap.get(name);
            return value != null ? Boolean.parseBoolean(value) : defaultValue;
        }

        /**
         * Look up the int value of a property.
         *
         * @param name         The name of the property to look up.
         * @param defaultValue The value to return if the property has not been defined or fails to
         *                     parse into an int.
         * @return the corresponding value, or defaultValue if no valid int is available.
         */
        public int getInt(@NonNull String name, int defaultValue) {
            Objects.requireNonNull(name);
            String value = mMap.get(name);
            if (value == null) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                Slog.e(TAG, "Parsing int failed for " + name);
                return defaultValue;
            }
        }

        /**
         * Look up the long value of a property.
         *
         * @param name         The name of the property to look up.
         * @param defaultValue The value to return if the property has not been defined. or fails to
         *                     parse into a long.
         * @return the corresponding value, or defaultValue if no valid long is available.
         */
        public long getLong(@NonNull String name, long defaultValue) {
            Objects.requireNonNull(name);
            String value = mMap.get(name);
            if (value == null) {
                return defaultValue;
            }
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                Slog.e(TAG, "Parsing long failed for " + name);
                return defaultValue;
            }
        }

        /**
         * Look up the int value of a property.
         *
         * @param name         The name of the property to look up.
         * @param defaultValue The value to return if the property has not been defined. or fails to
         *                     parse into a float.
         * @return the corresponding value, or defaultValue if no valid float is available.
         */
        public float getFloat(@NonNull String name, float defaultValue) {
            Objects.requireNonNull(name);
            String value = mMap.get(name);
            if (value == null) {
                return defaultValue;
            }
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException e) {
                Slog.e(TAG, "Parsing float failed for " + name);
                return defaultValue;
            }
        }

        /**
         * Returns a map with the underlying property values defined by this object
         *
         * @hide
         */
        public @NonNull Map<String, String> getPropertyValues() {
            return Collections.unmodifiableMap(mMap);
        }

        /**
         * Builder class for the construction of {@link Properties} objects.
         */
        public static final class Builder {
            @NonNull
            private final String mNamespace;
            @NonNull
            private final Map<String, String> mKeyValues = new HashMap<>();

            /**
             * Create a new Builders for the specified namespace.
             * @param namespace non null namespace.
             */
            public Builder(@NonNull String namespace) {
                mNamespace = namespace;
            }

            /**
             * Add a new property with the specified key and value.
             * @param name non null name of the property.
             * @param value nullable string value of the property.
             * @return this Builder object
             */
            @NonNull
            public Builder setString(@NonNull String name, @Nullable String value) {
                mKeyValues.put(name, value);
                return this;
            }

            /**
             * Add a new property with the specified key and value.
             * @param name non null name of the property.
             * @param value nullable string value of the property.
             * @return this Builder object
             */
            @NonNull
            public Builder setBoolean(@NonNull String name, boolean value) {
                mKeyValues.put(name, Boolean.toString(value));
                return this;
            }

            /**
             * Add a new property with the specified key and value.
             * @param name non null name of the property.
             * @param value int value of the property.
             * @return this Builder object
             */
            @NonNull
            public Builder setInt(@NonNull String name, int value) {
                mKeyValues.put(name, Integer.toString(value));
                return this;
            }

            /**
             * Add a new property with the specified key and value.
             * @param name non null name of the property.
             * @param value long value of the property.
             * @return this Builder object
             */
            @NonNull
            public Builder setLong(@NonNull String name, long value) {
                mKeyValues.put(name, Long.toString(value));
                return this;
            }

            /**
             * Add a new property with the specified key and value.
             * @param name non null name of the property.
             * @param value float value of the property.
             * @return this Builder object
             */
            @NonNull
            public Builder setFloat(@NonNull String name, float value) {
                mKeyValues.put(name, Float.toString(value));
                return this;
            }

            /**
             * Create a new {@link Properties} object.
             * @return non null Properties.
             */
            @NonNull
            public Properties build() {
                return new Properties(mNamespace, mKeyValues);
            }
        }
    }

}
