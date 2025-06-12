/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.internal.telephony;

import static android.telephony.TelephonyManager.HAL_SERVICE_RADIO;

import static com.android.internal.telephony.PhoneConstants.PHONE_TYPE_CDMA;
import static com.android.internal.telephony.PhoneConstants.PHONE_TYPE_CDMA_LTE;

import static java.util.Arrays.copyOf;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.LocalServerSocket;
import android.os.Build;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.AnomalyReporter;
import android.telephony.RadioAccessFamily;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.LocalLog;

import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.data.CellularNetworkValidator;
import com.android.internal.telephony.data.PhoneSwitcher;
import com.android.internal.telephony.data.TelephonyNetworkProvider;
import com.android.internal.telephony.euicc.EuiccCardController;
import com.android.internal.telephony.euicc.EuiccController;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.flags.FeatureFlagsImpl;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneFactory;
import com.android.internal.telephony.metrics.MetricsCollector;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.subscription.SubscriptionManagerService;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.util.NotificationChannelController;
import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * {@hide}
 */
public class PhoneFactory {
    static final String LOG_TAG = "PhoneFactory";
    static final int SOCKET_OPEN_RETRY_MILLIS = 2 * 1000;
    static final int SOCKET_OPEN_MAX_RETRY = 3;
    static final boolean DBG = false;

    //***** Class Variables

    // lock sLockProxyPhones protects sPhones, sPhone
    final static Object sLockProxyPhones = new Object();
    static private Phone[] sPhones = null;
    static private Phone sPhone = null;

    static private CommandsInterface[] sCommandsInterfaces = null;

    static private ProxyController sProxyController;
    static private UiccController sUiccController;
    private static IntentBroadcaster sIntentBroadcaster;
    private static @Nullable EuiccController sEuiccController;
    private static @Nullable EuiccCardController sEuiccCardController;
    private static SubscriptionManagerService sSubscriptionManagerService;

    @UnsupportedAppUsage
    static private boolean sMadeDefaults = false;
    @UnsupportedAppUsage
    static private PhoneNotifier sPhoneNotifier;
    @UnsupportedAppUsage
    static private Context sContext;
    static private PhoneConfigurationManager sPhoneConfigurationManager;
    static private SimultaneousCallingTracker sSimultaneousCallingTracker;
    static private PhoneSwitcher sPhoneSwitcher;
    private static TelephonyNetworkProvider sTelephonyNetworkProvider;
    static private NotificationChannelController sNotificationChannelController;
    static private CellularNetworkValidator sCellularNetworkValidator;

    static private final HashMap<String, LocalLog>sLocalLogs = new HashMap<String, LocalLog>();
    private static MetricsCollector sMetricsCollector;
    private static RadioInterfaceCapabilityController sRadioHalCapabilities;
    private static @NonNull FeatureFlags sFeatureFlags = new FeatureFlagsImpl();

    //***** Class Methods

    /**
     * @param context The context.
     * @param featureFlags The feature flag.
     */
    public static void makeDefaultPhones(Context context, @NonNull FeatureFlags featureFlags) {
        sFeatureFlags = featureFlags;
        makeDefaultPhone(context, featureFlags);
    }

    /**
     * FIXME replace this with some other way of making these
     * instances
     */
    @UnsupportedAppUsage
    public static void makeDefaultPhone(Context context, @NonNull FeatureFlags featureFlags) {
        synchronized (sLockProxyPhones) {
            if (!sMadeDefaults) {
                sContext = context;

                // create the telephony device controller.
                TelephonyDevController.create();

                TelephonyMetrics metrics = TelephonyMetrics.getInstance();
                metrics.setContext(context);

                int retryCount = 0;
                for(;;) {
                    boolean hasException = false;
                    retryCount ++;

                    try {
                        // use UNIX domain socket to
                        // prevent subsequent initialization
                        new LocalServerSocket("com.android.internal.telephony");
                    } catch (java.io.IOException ex) {
                        hasException = true;
                    }

                    if ( !hasException ) {
                        break;
                    } else if (retryCount > SOCKET_OPEN_MAX_RETRY) {
                        throw new RuntimeException("PhoneFactory probably already running");
                    } else {
                        try {
                            Thread.sleep(SOCKET_OPEN_RETRY_MILLIS);
                        } catch (InterruptedException er) {
                        }
                    }
                }

                // register statsd pullers.
                sMetricsCollector = new MetricsCollector(context, sFeatureFlags);

                sPhoneNotifier = new DefaultPhoneNotifier(context, featureFlags);

                int cdmaSubscription = CdmaSubscriptionSourceManager.getDefault(context);
                Rlog.i(LOG_TAG, "Cdma Subscription set to " + cdmaSubscription);

                /* In case of multi SIM mode two instances of Phone, RIL are created,
                   where as in single SIM mode only instance. isMultiSimEnabled() function checks
                   whether it is single SIM or multi SIM mode */
                int numPhones = TelephonyManager.getDefault().getActiveModemCount();

                int[] networkModes = new int[numPhones];
                sPhones = new Phone[numPhones];
                sCommandsInterfaces = new RIL[numPhones];

                for (int i = 0; i < numPhones; i++) {
                    // reads the system properties and makes commandsinterface
                    // Get preferred network type.
                    networkModes[i] = RILConstants.PREFERRED_NETWORK_MODE;

                    Rlog.i(LOG_TAG, "Network Mode set to " + Integer.toString(networkModes[i]));
                    sCommandsInterfaces[i] = new RIL(context,
                            RadioAccessFamily.getRafFromNetworkType(networkModes[i]),
                            cdmaSubscription, i, featureFlags);
                }

                if (numPhones > 0) {
                    final RadioConfig radioConfig = RadioConfig.make(context,
                            sCommandsInterfaces[0].getHalVersion(HAL_SERVICE_RADIO));
                    sRadioHalCapabilities = RadioInterfaceCapabilityController.init(radioConfig,
                            sCommandsInterfaces[0]);
                } else {
                    // There is no command interface to go off of
                    final RadioConfig radioConfig = RadioConfig.make(context, HalVersion.UNKNOWN);
                    sRadioHalCapabilities = RadioInterfaceCapabilityController.init(
                            radioConfig, null);
                }


                // Instantiate UiccController so that all other classes can just
                // call getInstance()
                sUiccController = UiccController.make(context, featureFlags);

                Rlog.i(LOG_TAG, "Creating SubscriptionManagerService");
                sSubscriptionManagerService = new SubscriptionManagerService(context,
                        Looper.myLooper(), featureFlags);

                TelephonyComponentFactory.getInstance().inject(MultiSimSettingController.class.
                        getName()).initMultiSimSettingController(context, featureFlags);

                if (context.getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_TELEPHONY_EUICC)) {
                    sEuiccController = EuiccController.init(context, sFeatureFlags);
                    sEuiccCardController = EuiccCardController.init(context, sFeatureFlags);
                }

                for (int i = 0; i < numPhones; i++) {
                    sPhones[i] = createPhone(context, i);
                }

                // Set the default phone in base class.
                // FIXME: This is a first best guess at what the defaults will be. It
                // FIXME: needs to be done in a more controlled manner in the future.
                if (numPhones > 0) sPhone = sPhones[0];

                // Ensure that we have a default SMS app. Requesting the app with
                // updateIfNeeded set to true is enough to configure a default SMS app.
                ComponentName componentName =
                        SmsApplication.getDefaultSmsApplication(context, true /* updateIfNeeded */);
                String packageName = "NONE";
                if (componentName != null) {
                    packageName = componentName.getPackageName();
                }
                Rlog.i(LOG_TAG, "defaultSmsApplication: " + packageName);

                if (sFeatureFlags.smsMmsDeliverBroadcastsRedirectToMainUser()) {
                    // Explicitly call this, even if the user has no default Sms application, to
                    // ensure that the System apps have the appropriate permissions.
                    SmsApplication.grantPermissionsToSystemApps(context);
                }

                // Set up monitor to watch for changes to SMS packages
                SmsApplication.initSmsPackageMonitor(context);

                sMadeDefaults = true;

                // Only bring up IMS if the device supports having an IMS stack.
                if (context.getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_TELEPHONY_IMS)) {
                    // Start monitoring after defaults have been made.
                    // Default phone must be ready before ImsPhone is created because ImsService
                    // might need it when it is being opened.
                    for (int i = 0; i < numPhones; i++) {
                        sPhones[i].createImsPhone();
                    }
                } else {
                    Rlog.i(LOG_TAG, "IMS is not supported on this device, skipping ImsResolver.");
                }

                sPhoneConfigurationManager = PhoneConfigurationManager.init(sContext, featureFlags);
                if (featureFlags.simultaneousCallingIndications()) {
                    sSimultaneousCallingTracker =
                            SimultaneousCallingTracker.init(sContext, featureFlags);
                }

                sCellularNetworkValidator = CellularNetworkValidator.make(sContext, sFeatureFlags);

                int maxActivePhones = sPhoneConfigurationManager
                        .getNumberOfModemsWithSimultaneousDataConnections();

                sPhoneSwitcher = TelephonyComponentFactory.getInstance().inject(
                        PhoneSwitcher.class.getName()).
                        makePhoneSwitcher(maxActivePhones, sContext, Looper.myLooper(),
                                featureFlags);

                sProxyController = ProxyController.getInstance(context, featureFlags);

                sIntentBroadcaster = IntentBroadcaster.getInstance(context);

                sNotificationChannelController = new NotificationChannelController(context);

                // Create the TelephonyNetworkProvider instance, which is a singleton.
                sTelephonyNetworkProvider = new TelephonyNetworkProvider(Looper.myLooper(),
                        context, featureFlags);
            }
        }
    }

    /**
     * Upon single SIM to dual SIM switch or vice versa, we dynamically allocate or de-allocate
     * Phone and CommandInterface objects.
     *
     * @param context The context
     * @param activeModemCount The number of active modems
     */
    public static void onMultiSimConfigChanged(Context context, int activeModemCount) {
        synchronized (sLockProxyPhones) {
            int prevActiveModemCount = sPhones.length;
            if (prevActiveModemCount == activeModemCount) return;

            // Currently we will not clean up the 2nd Phone object, so that it can be re-used if
            // user switches back.
            if (prevActiveModemCount > activeModemCount) return;

            sPhones = copyOf(sPhones, activeModemCount);
            sCommandsInterfaces = copyOf(sCommandsInterfaces, activeModemCount);

            int cdmaSubscription = CdmaSubscriptionSourceManager.getDefault(context);
            for (int i = prevActiveModemCount; i < activeModemCount; i++) {
                sCommandsInterfaces[i] = new RIL(context, RadioAccessFamily.getRafFromNetworkType(
                        RILConstants.PREFERRED_NETWORK_MODE),
                        cdmaSubscription, i, sFeatureFlags);
                sPhones[i] = createPhone(context, i);
                if (context.getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_TELEPHONY_IMS)) {
                    sPhones[i].createImsPhone();
                }
            }
        }
    }

    private static Phone createPhone(Context context, int phoneId) {
        int phoneType = TelephonyManager.getPhoneType(RILConstants.PREFERRED_NETWORK_MODE);
        Rlog.i(LOG_TAG, "Creating Phone with type = " + phoneType + " phoneId = " + phoneId);

        // We always use PHONE_TYPE_CDMA_LTE now.
        if (phoneType == PHONE_TYPE_CDMA) phoneType = PHONE_TYPE_CDMA_LTE;
        TelephonyComponentFactory injectedComponentFactory =
                TelephonyComponentFactory.getInstance().inject(GsmCdmaPhone.class.getName());

        return injectedComponentFactory.makePhone(context,
                sCommandsInterfaces[phoneId], sPhoneNotifier, phoneId, phoneType,
                TelephonyComponentFactory.getInstance(), sFeatureFlags);
    }

    @UnsupportedAppUsage
    public static Phone getDefaultPhone() {
        synchronized (sLockProxyPhones) {
            if (!sMadeDefaults) {
                throw new IllegalStateException("Default phones haven't been made yet!");
            }
            return sPhone;
        }
    }

    @UnsupportedAppUsage
    public static Phone getPhone(int phoneId) {
        Phone phone;
        String dbgInfo = "";

        synchronized (sLockProxyPhones) {
            if (!sMadeDefaults) {
                throw new IllegalStateException("Default phones haven't been made yet!");
                // CAF_MSIM FIXME need to introduce default phone id ?
            } else if (phoneId == SubscriptionManager.DEFAULT_PHONE_INDEX) {
                if (DBG) {
                    dbgInfo = "phoneId == DEFAULT_PHONE_ID return sPhone";
                }
                phone = sPhone;
            } else {
                if (DBG) {
                    dbgInfo = "phoneId != DEFAULT_PHONE_ID return sPhones[phoneId]";
                }
                phone = (phoneId >= 0 && phoneId < sPhones.length)
                            ? sPhones[phoneId] : null;
            }
            if (DBG) {
                Rlog.d(LOG_TAG, "getPhone:- " + dbgInfo + " phoneId=" + phoneId +
                        " phone=" + phone);
            }
            return phone;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static Phone[] getPhones() {
        synchronized (sLockProxyPhones) {
            if (!sMadeDefaults) {
                throw new IllegalStateException("Default phones haven't been made yet!");
            }
            return sPhones;
        }
    }

    public static TelephonyNetworkProvider getNetworkProvider() {
        return sTelephonyNetworkProvider;
    }

    /**
     * Returns the preferred network type bitmask that should be set in the modem.
     *
     * @param phoneId The phone's id.
     * @return the preferred network mode bitmask that should be set.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static int calculatePreferredNetworkType(int phoneId) {
        if (getPhone(phoneId) == null) {
            Rlog.d(LOG_TAG, "Invalid phoneId return default network mode ");
            return RadioAccessFamily.getRafFromNetworkType(RILConstants.PREFERRED_NETWORK_MODE);
        }
        int networkType = (int) getPhone(phoneId).getAllowedNetworkTypes(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER);
        Rlog.d(LOG_TAG, "calculatePreferredNetworkType: phoneId = " + phoneId + " networkType = "
                + networkType);
        return networkType;
    }

    /* Gets the default subscription */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static int getDefaultSubscription() {
        return SubscriptionManagerService.getInstance().getDefaultSubId();
    }

    /* Returns User SMS Prompt property,  enabled or not */
    public static boolean isSMSPromptEnabled() {
        boolean prompt = false;
        int value = 0;
        try {
            value = Settings.Global.getInt(sContext.getContentResolver(),
                    Settings.Global.MULTI_SIM_SMS_PROMPT);
        } catch (SettingNotFoundException snfe) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Dual Sim SMS Prompt Values");
        }
        prompt = (value == 0) ? false : true ;
        Rlog.d(LOG_TAG, "SMS Prompt option:" + prompt);

       return prompt;
    }

    /**
     * Makes a {@link ImsPhone} object.
     * @return the {@code ImsPhone} object or null if the exception occured
     */
    public static Phone makeImsPhone(PhoneNotifier phoneNotifier, Phone defaultPhone) {
        return ImsPhoneFactory.makePhone(sContext, phoneNotifier, defaultPhone, sFeatureFlags);
    }

    /**
     * Get the instance of {@link SmsController}.
     */
    public static SmsController getSmsController() {
        synchronized (sLockProxyPhones) {
            if (!sMadeDefaults) {
                throw new IllegalStateException("Default phones haven't been made yet!");
            }
            return sProxyController.getSmsController();
        }
    }

    /**
     * Get Command Interfaces.
     */
    public static CommandsInterface[] getCommandsInterfaces() {
        synchronized (sLockProxyPhones) {
            return sCommandsInterfaces;
        }
    }

    /**
     * Adds a local log category.
     *
     * Only used within the telephony process.  Use localLog to add log entries.
     *
     * TODO - is there a better way to do this?  Think about design when we have a minute.
     *
     * @param key the name of the category - will be the header in the service dump.
     * @param size the number of lines to maintain in this category
     */
    public static void addLocalLog(String key, int size) {
        synchronized(sLocalLogs) {
            if (sLocalLogs.containsKey(key)) {
                throw new IllegalArgumentException("key " + key + " already present");
            }
            sLocalLogs.put(key, new LocalLog(size));
        }
    }

    /**
     * Add a line to the named Local Log.
     *
     * This will appear in the TelephonyDebugService dump.
     *
     * @param key the name of the log category to put this in.  Must be created
     *            via addLocalLog.
     * @param log the string to add to the log.
     */
    public static void localLog(String key, String log) {
        synchronized(sLocalLogs) {
            if (sLocalLogs.containsKey(key) == false) {
                throw new IllegalArgumentException("key " + key + " not found");
            }
            sLocalLogs.get(key).log(log);
        }
    }

    /** Returns the MetricsCollector instance. */
    public static MetricsCollector getMetricsCollector() {
        return sMetricsCollector;
    }

    /**
     * Print all feature flag configurations that Telephony is using for debugging purposes.
     */
    private static void reflectAndPrintFlagConfigs(IndentingPrintWriter pw) {

        try {
            // Look away, a forbidden technique (reflection) is being used to allow us to get
            // all flag configs without having to add them manually to this method.
            Method[] methods = FeatureFlags.class.getMethods();
            if (methods.length == 0) {
                pw.println("NONE");
                return;
            }
            for (Method m : methods) {
                pw.println(m.getName() + "-> " + m.invoke(sFeatureFlags));
            }
        } catch (Exception e) {
            pw.println("[ERROR]");
        }
    }

    public static void dump(FileDescriptor fd, PrintWriter printwriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printwriter, "  ");
        pw.println("PhoneFactory:");
        pw.println(" sMadeDefaults=" + sMadeDefaults);

        sPhoneSwitcher.dump(fd, pw, args);
        pw.println();

        Phone[] phones = (Phone[])PhoneFactory.getPhones();
        for (int i = 0; i < phones.length; i++) {
            pw.increaseIndent();
            Phone phone = phones[i];

            try {
                phone.dump(fd, pw, args);
            } catch (Exception e) {
                pw.println("Telephony DebugService: Could not get Phone[" + i + "] e=" + e);
                continue;
            }

            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }

        pw.increaseIndent();
        sTelephonyNetworkProvider.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.println("++++++++++++++++++++++++++++++++");

        pw.println("UiccController:");
        pw.increaseIndent();
        try {
            sUiccController.dump(fd, pw, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        pw.flush();
        pw.decreaseIndent();
        pw.println("++++++++++++++++++++++++++++++++");

        pw.flush();
        pw.decreaseIndent();
        pw.println("++++++++++++++++++++++++++++++++");

        pw.println("sRadioHalCapabilities:");
        pw.increaseIndent();
        try {
            sRadioHalCapabilities.dump(fd, pw, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        pw.flush();
        pw.decreaseIndent();
        pw.println("++++++++++++++++++++++++++++++++");

        pw.println("LocalLogs:");
        pw.increaseIndent();
        synchronized (sLocalLogs) {
            for (String key : sLocalLogs.keySet()) {
                pw.println(key);
                pw.increaseIndent();
                sLocalLogs.get(key).dump(fd, pw, args);
                pw.decreaseIndent();
            }
            pw.flush();
        }
        pw.decreaseIndent();
        pw.println("++++++++++++++++++++++++++++++++");

        pw.println("SharedPreferences:");
        pw.increaseIndent();
        try {
            if (sContext != null) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(sContext);
                Map spValues = sp.getAll();
                for (Object key : spValues.keySet()) {
                    pw.println(key + " : " + spValues.get(key));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        pw.decreaseIndent();
        pw.println("++++++++++++++++++++++++++++++++");
        pw.println("DebugEvents:");
        pw.increaseIndent();
        try {
            AnomalyReporter.dump(fd, pw, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        pw.flush();
        pw.decreaseIndent();

        pw.println("++++++++++++++++++++++++++++++++");
        pw.println("Flag Configurations:");
        pw.increaseIndent();
        reflectAndPrintFlagConfigs(pw);
        pw.flush();
        pw.decreaseIndent();
        pw.println("++++++++++++++++++++++++++++++++");
    }
}
