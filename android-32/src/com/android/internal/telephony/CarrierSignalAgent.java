/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.telephony.CarrierConfigManager.KEY_CARRIER_APP_NO_WAKE_SIGNAL_CONFIG_STRING_ARRAY;
import static android.telephony.CarrierConfigManager.KEY_CARRIER_APP_WAKE_SIGNAL_CONFIG_STRING_ARRAY;

import android.annotation.Nullable;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;

import com.android.internal.telephony.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class act as an CarrierSignalling Agent.
 * it load registered carrier signalling receivers from carrier config, cache the result to avoid
 * repeated polling and send the intent to the interested receivers.
 * Each CarrierSignalAgent is associated with a phone object.
 */
public class CarrierSignalAgent extends Handler {

    private static final String LOG_TAG = CarrierSignalAgent.class.getSimpleName();
    private static final boolean DBG = true;
    private static final boolean VDBG = Rlog.isLoggable(LOG_TAG, Log.VERBOSE);
    private static final boolean WAKE = true;
    private static final boolean NO_WAKE = false;

    /** delimiters for parsing config of the form: pakName./receiverName : signal1, signal2,..*/
    private static final String COMPONENT_NAME_DELIMITER = "\\s*:\\s*";
    private static final String CARRIER_SIGNAL_DELIMITER = "\\s*,\\s*";

    /** Member variables */
    private final Phone mPhone;
    private boolean mDefaultNetworkAvail;

    /**
     * This is a map of intent action -> set of component name of statically registered
     * carrier signal receivers(wakeup receivers).
     * Those intents are declared in the Manifest files, aiming to wakeup broadcast receivers.
     * Carrier apps should be careful when configuring the wake signal list to avoid unnecessary
     * wakeup. Note we use Set as the entry value to compare config directly regardless of element
     * order.
     * @see CarrierConfigManager#KEY_CARRIER_APP_WAKE_SIGNAL_CONFIG_STRING_ARRAY
     */
    private Map<String, Set<ComponentName>> mCachedWakeSignalConfigs = new HashMap<>();

    /**
     * This is a map of intent action -> set of component name of dynamically registered
     * carrier signal receivers(non-wakeup receivers). Those intents will not wake up the apps.
     * Note Carrier apps should avoid configuring no wake signals in there Manifest files.
     * Note we use Set as the entry value to compare config directly regardless of element order.
     * @see CarrierConfigManager#KEY_CARRIER_APP_NO_WAKE_SIGNAL_CONFIG_STRING_ARRAY
     */
    private Map<String, Set<ComponentName>> mCachedNoWakeSignalConfigs = new HashMap<>();

    private static final int EVENT_REGISTER_DEFAULT_NETWORK_AVAIL = 0;

    /**
     * This is a list of supported signals from CarrierSignalAgent
     */
    private static final Set<String> VALID_CARRIER_SIGNAL_ACTIONS = new HashSet<>(Arrays.asList(
            TelephonyManager.ACTION_CARRIER_SIGNAL_PCO_VALUE,
            TelephonyManager.ACTION_CARRIER_SIGNAL_REDIRECTED,
            TelephonyManager.ACTION_CARRIER_SIGNAL_REQUEST_NETWORK_FAILED,
            TelephonyManager.ACTION_CARRIER_SIGNAL_RESET,
            TelephonyManager.ACTION_CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE));

    private static final Map<String, String> NEW_ACTION_TO_COMPAT_MAP =
            new HashMap<String, String>() {{
                put(TelephonyManager.ACTION_CARRIER_SIGNAL_PCO_VALUE,
                        TelephonyIntents.ACTION_CARRIER_SIGNAL_PCO_VALUE);
                put(TelephonyManager.ACTION_CARRIER_SIGNAL_REDIRECTED,
                        TelephonyIntents.ACTION_CARRIER_SIGNAL_REDIRECTED);
                put(TelephonyManager.ACTION_CARRIER_SIGNAL_REQUEST_NETWORK_FAILED,
                        TelephonyIntents.ACTION_CARRIER_SIGNAL_REQUEST_NETWORK_FAILED);
                put(TelephonyManager.ACTION_CARRIER_SIGNAL_RESET,
                        TelephonyIntents.ACTION_CARRIER_SIGNAL_RESET);
                put(TelephonyManager.ACTION_CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE,
                        TelephonyIntents.ACTION_CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE);
            }};

    private static final Map<String, String> COMPAT_ACTION_TO_NEW_MAP = NEW_ACTION_TO_COMPAT_MAP
            .entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    private final LocalLog mErrorLocalLog = new LocalLog(20);

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DBG) log("CarrierSignalAgent receiver action: " + action);
            if (action.equals(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED)) {
                loadCarrierConfig();
            }
        }
    };

    private ConnectivityManager.NetworkCallback mNetworkCallback;

    /** Constructor */
    public CarrierSignalAgent(Phone phone) {
        mPhone = phone;
        loadCarrierConfig();
        // reload configurations on CARRIER_CONFIG_CHANGED
        mPhone.getContext().registerReceiver(mReceiver,
                new IntentFilter(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED));
        mPhone.getCarrierActionAgent().registerForCarrierAction(
                CarrierActionAgent.CARRIER_ACTION_REPORT_DEFAULT_NETWORK_STATUS, this,
                EVENT_REGISTER_DEFAULT_NETWORK_AVAIL, null, false);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_REGISTER_DEFAULT_NETWORK_AVAIL:
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    Rlog.e(LOG_TAG, "Register default network exception: " + ar.exception);
                    return;
                }
                final ConnectivityManager connectivityMgr = mPhone.getContext()
                        .getSystemService(ConnectivityManager.class);
                if ((boolean) ar.result) {
                    mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(Network network) {
                            // an optimization to avoid signaling on every default network switch.
                            if (!mDefaultNetworkAvail) {
                                if (DBG) log("Default network available: " + network);
                                Intent intent = new Intent(TelephonyManager
                                        .ACTION_CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE);
                                intent.putExtra(
                                        TelephonyManager.EXTRA_DEFAULT_NETWORK_AVAILABLE, true);
                                notifyCarrierSignalReceivers(intent);
                                mDefaultNetworkAvail = true;
                            }
                        }
                        @Override
                        public void onLost(Network network) {
                            if (DBG) log("Default network lost: " + network);
                            Intent intent = new Intent(TelephonyManager
                                    .ACTION_CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE);
                            intent.putExtra(
                                    TelephonyManager.EXTRA_DEFAULT_NETWORK_AVAILABLE, false);
                            notifyCarrierSignalReceivers(intent);
                            mDefaultNetworkAvail = false;
                        }
                    };
                    connectivityMgr.registerDefaultNetworkCallback(mNetworkCallback, mPhone);
                    log("Register default network");

                } else if (mNetworkCallback != null) {
                    connectivityMgr.unregisterNetworkCallback(mNetworkCallback);
                    mNetworkCallback = null;
                    mDefaultNetworkAvail = false;
                    log("unregister default network");
                }
                break;
            default:
                break;
        }
    }

    /**
     * load carrier config and cached the results into a hashMap action -> array list of components.
     */
    private void loadCarrierConfig() {
        CarrierConfigManager configManager = (CarrierConfigManager) mPhone.getContext()
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle b = null;
        if (configManager != null) {
            b = configManager.getConfig();
        }
        if (b != null) {
            synchronized (mCachedWakeSignalConfigs) {
                log("Loading carrier config: " + KEY_CARRIER_APP_WAKE_SIGNAL_CONFIG_STRING_ARRAY);
                Map<String, Set<ComponentName>> config = parseAndCache(
                        b.getStringArray(KEY_CARRIER_APP_WAKE_SIGNAL_CONFIG_STRING_ARRAY));
                // In some rare cases, up-to-date config could be fetched with delay and all signals
                // have already been delivered the receivers from the default carrier config.
                // To handle this raciness, we should notify those receivers (from old configs)
                // and reset carrier actions. This should be done before cached Config got purged
                // and written with the up-to-date value, Otherwise those receivers from the
                // old config might lingers without properly clean-up.
                if (!mCachedWakeSignalConfigs.isEmpty()
                        && !config.equals(mCachedWakeSignalConfigs)) {
                    if (VDBG) log("carrier config changed, reset receivers from old config");
                    mPhone.getCarrierActionAgent().sendEmptyMessage(
                            CarrierActionAgent.CARRIER_ACTION_RESET);
                }
                mCachedWakeSignalConfigs = config;
            }

            synchronized (mCachedNoWakeSignalConfigs) {
                log("Loading carrier config: "
                        + KEY_CARRIER_APP_NO_WAKE_SIGNAL_CONFIG_STRING_ARRAY);
                Map<String, Set<ComponentName>> config = parseAndCache(
                        b.getStringArray(KEY_CARRIER_APP_NO_WAKE_SIGNAL_CONFIG_STRING_ARRAY));
                if (!mCachedNoWakeSignalConfigs.isEmpty()
                        && !config.equals(mCachedNoWakeSignalConfigs)) {
                    if (VDBG) log("carrier config changed, reset receivers from old config");
                    mPhone.getCarrierActionAgent().sendEmptyMessage(
                            CarrierActionAgent.CARRIER_ACTION_RESET);
                }
                mCachedNoWakeSignalConfigs = config;
            }
        }
    }

    /**
     * Parse each config with the form {pakName./receiverName : signal1, signal2,.} and cached the
     * result internally to avoid repeated polling
     * @see #CARRIER_SIGNAL_DELIMITER
     * @see #COMPONENT_NAME_DELIMITER
     * @param configs raw information from carrier config
     */
    private Map<String, Set<ComponentName>> parseAndCache(String[] configs) {
        Map<String, Set<ComponentName>> newCachedWakeSignalConfigs = new HashMap<>();
        if (!ArrayUtils.isEmpty(configs)) {
            for (String config : configs) {
                if (!TextUtils.isEmpty(config)) {
                    String[] splitStr = config.trim().split(COMPONENT_NAME_DELIMITER, 2);
                    if (splitStr.length == 2) {
                        ComponentName componentName = ComponentName
                                .unflattenFromString(splitStr[0]);
                        if (componentName == null) {
                            loge("Invalid component name: " + splitStr[0]);
                            continue;
                        }
                        String[] signals = splitStr[1].split(CARRIER_SIGNAL_DELIMITER);
                        for (String s : signals) {
                            if (!VALID_CARRIER_SIGNAL_ACTIONS.contains(s)) {
                                // It could be a legacy action in the com.android.internal.telephony
                                // namespace. If that's the case, translate it to the new actions.
                                if (COMPAT_ACTION_TO_NEW_MAP.containsKey(s)) {
                                    s = COMPAT_ACTION_TO_NEW_MAP.get(s);
                                } else {
                                    loge("Invalid signal name: " + s);
                                    continue;
                                }
                            }
                            Set<ComponentName> componentList = newCachedWakeSignalConfigs.get(s);
                            if (componentList == null) {
                                componentList = new HashSet<>();
                                newCachedWakeSignalConfigs.put(s, componentList);
                            }
                            componentList.add(componentName);
                            if (VDBG) {
                                logv("Add config " + "{signal: " + s
                                        + " componentName: " + componentName + "}");
                            }
                        }
                    } else {
                        loge("invalid config format: " + config);
                    }
                }
            }
        }
        return newCachedWakeSignalConfigs;
    }

    /**
     * Check if there are registered carrier broadcast receivers to handle the passing intent
     */
    public boolean hasRegisteredReceivers(String action) {
        return mCachedWakeSignalConfigs.containsKey(action)
                || mCachedNoWakeSignalConfigs.containsKey(action);
    }

    /**
     * Broadcast the intents explicitly.
     * Some correctness checks will be applied before broadcasting.
     * - for non-wakeup(runtime) receivers, make sure the intent is not declared in their manifests
     * and apply FLAG_EXCLUDE_STOPPED_PACKAGES to avoid wake-up
     * - for wakeup(manifest) receivers, make sure there are matched receivers with registered
     * intents.
     *
     * @param intent intent which signals carrier apps
     * @param receivers a list of component name for broadcast receivers.
     *                  Those receivers could either be statically declared in Manifest or
     *                  registered during run-time.
     * @param wakeup true indicate wakeup receivers otherwise non-wakeup receivers
     */
    private void broadcast(Intent intent, Set<ComponentName> receivers, boolean wakeup) {
        final PackageManager packageManager = mPhone.getContext().getPackageManager();
        for (ComponentName name : receivers) {
            Intent signal = new Intent(intent);
            if (wakeup) {
                signal.setComponent(name);
            } else {
                // Explicit intents won't reach dynamically registered receivers -- set the package
                // instead.
                signal.setPackage(name.getPackageName());
            }

            if (wakeup && packageManager.queryBroadcastReceivers(signal,
                    PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
                loge("Carrier signal receivers are configured but unavailable: "
                        + signal.getComponent());
                continue;
            }
            if (!wakeup && !packageManager.queryBroadcastReceivers(signal,
                    PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
                loge("Runtime signals shouldn't be configured in Manifest: "
                        + signal.getComponent());
                continue;
            }

            SubscriptionManager.putSubscriptionIdExtra(signal, mPhone.getSubId());
            signal.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            if (!wakeup) signal.setFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);

            Intent compatIntent = null;
            try {
                if (mPhone.getContext().getPackageManager()
                        .getApplicationInfo(name.getPackageName(), 0).targetSdkVersion
                        <= Build.VERSION_CODES.R) {
                    compatIntent = createCompatIntent(signal);
                }
            } catch (PackageManager.NameNotFoundException e) {
                // ignore, don't do anything special for compatibility
            }
            try {
                Intent intentToSend = compatIntent == null ? signal : compatIntent;
                mPhone.getContext().sendBroadcastAsUser(intentToSend, UserHandle.ALL);
                if (DBG) {
                    log("Sending signal " + intentToSend.getAction()
                            + " to the carrier signal receiver: " + intentToSend.getComponent());
                }
            } catch (ActivityNotFoundException e) {
                loge("Send broadcast failed: " + e);
            }
        }
    }

    /**
     * Match the intent against cached tables to find a list of registered carrier signal
     * receivers and broadcast the intent.
     * @param intent broadcasting intent, it could belong to wakeup, non-wakeup signal list or both
     *
     */
    public void notifyCarrierSignalReceivers(Intent intent) {
        Set<ComponentName> receiverSet;

        synchronized (mCachedWakeSignalConfigs) {
            receiverSet = mCachedWakeSignalConfigs.get(intent.getAction());
            if (!ArrayUtils.isEmpty(receiverSet)) {
                broadcast(intent, receiverSet, WAKE);
            }
        }

        synchronized (mCachedNoWakeSignalConfigs) {
            receiverSet = mCachedNoWakeSignalConfigs.get(intent.getAction());
            if (!ArrayUtils.isEmpty(receiverSet)) {
                broadcast(intent, receiverSet, NO_WAKE);
            }
        }
    }

    private static @Nullable Intent createCompatIntent(Intent original) {
        String compatAction = NEW_ACTION_TO_COMPAT_MAP.get(original.getAction());
        if (compatAction == null) {
            Rlog.i(LOG_TAG, "intent action " + original.getAction() + " does not have a"
                    + " compat alternative for component " + original.getComponent());
            return null;
        }
        Intent compatIntent = new Intent(original);
        compatIntent.setAction(compatAction);
        for (String extraKey : original.getExtras().keySet()) {
            switch (extraKey) {
                case TelephonyManager.EXTRA_REDIRECTION_URL:
                    compatIntent.putExtra(TelephonyIntents.EXTRA_REDIRECTION_URL,
                            original.getStringExtra(TelephonyManager.EXTRA_REDIRECTION_URL));
                    break;
                case TelephonyManager.EXTRA_DATA_FAIL_CAUSE:
                    compatIntent.putExtra(TelephonyIntents.EXTRA_ERROR_CODE,
                            original.getIntExtra(TelephonyManager.EXTRA_DATA_FAIL_CAUSE, -1));
                    break;
                case TelephonyManager.EXTRA_PCO_ID:
                    compatIntent.putExtra(TelephonyIntents.EXTRA_PCO_ID,
                            original.getIntExtra(TelephonyManager.EXTRA_PCO_ID, -1));
                    break;
                case TelephonyManager.EXTRA_PCO_VALUE:
                    compatIntent.putExtra(TelephonyIntents.EXTRA_PCO_VALUE,
                            original.getByteArrayExtra(TelephonyManager.EXTRA_PCO_VALUE));
                    break;
                case TelephonyManager.EXTRA_DEFAULT_NETWORK_AVAILABLE:
                    compatIntent.putExtra(TelephonyIntents.EXTRA_DEFAULT_NETWORK_AVAILABLE,
                            original.getBooleanExtra(
                                    TelephonyManager.EXTRA_DEFAULT_NETWORK_AVAILABLE, false));
                    break;
                case TelephonyManager.EXTRA_APN_TYPE:
                    int apnType = original.getIntExtra(TelephonyManager.EXTRA_APN_TYPE,
                            ApnSetting.TYPE_DEFAULT);
                    compatIntent.putExtra(TelephonyIntents.EXTRA_APN_TYPE_INT, apnType);
                    compatIntent.putExtra(TelephonyIntents.EXTRA_APN_TYPE,
                            ApnSetting.getApnTypesStringFromBitmask(apnType));
                    break;
                case TelephonyManager.EXTRA_APN_PROTOCOL:
                    int apnProtocol = original.getIntExtra(TelephonyManager.EXTRA_APN_PROTOCOL, -1);
                    compatIntent.putExtra(TelephonyIntents.EXTRA_APN_PROTOCOL_INT, apnProtocol);
                    compatIntent.putExtra(TelephonyIntents.EXTRA_APN_PROTOCOL,
                            ApnSetting.getProtocolStringFromInt(apnProtocol));
                    break;
            }
        }
        return compatIntent;
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, "[" + mPhone.getPhoneId() + "]" + s);
    }

    private void loge(String s) {
        mErrorLocalLog.log(s);
        Rlog.e(LOG_TAG, "[" + mPhone.getPhoneId() + "]" + s);
    }

    private void logv(String s) {
        Rlog.v(LOG_TAG, "[" + mPhone.getPhoneId() + "]" + s);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        pw.println("mCachedWakeSignalConfigs:");
        ipw.increaseIndent();
        for (Map.Entry<String, Set<ComponentName>> entry : mCachedWakeSignalConfigs.entrySet()) {
            pw.println("signal: " + entry.getKey() + " componentName list: " + entry.getValue());
        }
        ipw.decreaseIndent();

        pw.println("mCachedNoWakeSignalConfigs:");
        ipw.increaseIndent();
        for (Map.Entry<String, Set<ComponentName>> entry : mCachedNoWakeSignalConfigs.entrySet()) {
            pw.println("signal: " + entry.getKey() + " componentName list: " + entry.getValue());
        }
        ipw.decreaseIndent();

        pw.println("mDefaultNetworkAvail: " + mDefaultNetworkAvail);

        pw.println("error log:");
        ipw.increaseIndent();
        mErrorLocalLog.dump(fd, pw, args);
        ipw.decreaseIndent();
    }
}
