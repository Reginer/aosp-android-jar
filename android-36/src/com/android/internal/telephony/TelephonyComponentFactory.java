/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStatVfs;
import android.text.TextUtils;

import com.android.ims.ImsManager;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.cdma.EriManager;
import com.android.internal.telephony.data.AccessNetworksManager;
import com.android.internal.telephony.data.DataNetworkController;
import com.android.internal.telephony.data.DataProfileManager;
import com.android.internal.telephony.data.DataServiceManager;
import com.android.internal.telephony.data.DataSettingsManager;
import com.android.internal.telephony.data.LinkBandwidthEstimator;
import com.android.internal.telephony.data.PhoneSwitcher;
import com.android.internal.telephony.emergency.EmergencyNumberTracker;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.flags.FeatureFlagsImpl;
import com.android.internal.telephony.imsphone.ImsExternalCallTracker;
import com.android.internal.telephony.imsphone.ImsNrSaModeHandler;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneCallTracker;
import com.android.internal.telephony.nitz.NitzStateMachineImpl;
import com.android.internal.telephony.security.CellularIdentifierDisclosureNotifier;
import com.android.internal.telephony.security.CellularNetworkSecuritySafetySource;
import com.android.internal.telephony.security.NullCipherNotifier;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccProfile;
import com.android.telephony.Rlog;

import dalvik.system.PathClassLoader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This class has one-line methods to instantiate objects only. The purpose is to make code
 * unit-test friendly and use this class as a way to do dependency injection. Instantiating objects
 * this way makes it easier to mock them in tests.
 */
public class TelephonyComponentFactory {

    private static final String TAG = TelephonyComponentFactory.class.getSimpleName();

    private static TelephonyComponentFactory sInstance;
    private final TelephonyFacade mTelephonyFacade = new TelephonyFacade();

    private InjectedComponents mInjectedComponents;

    private static class InjectedComponents {
        private static final String ATTRIBUTE_JAR = "jar";
        private static final String ATTRIBUTE_PACKAGE = "package";
        private static final String TAG_INJECTION = "injection";
        private static final String TAG_COMPONENTS = "components";
        private static final String TAG_COMPONENT = "component";
        private static final String SYSTEM = "/system/";
        private static final String PRODUCT = "/product/";
        private static final String SYSTEM_EXT = "/system_ext/";

        private final Set<String> mComponentNames = new HashSet<>();
        private TelephonyComponentFactory mInjectedInstance;
        private String mPackageName;
        private String mJarPath;

        /**
         * @return paths correctly configured to inject.
         * 1) PackageName and JarPath mustn't be empty.
         * 2) JarPath is restricted under /system or /product or /system_ext only.
         * 3) JarPath is on a READ-ONLY partition.
         */
        private @Nullable String getValidatedPaths() {
            if (TextUtils.isEmpty(mPackageName) || TextUtils.isEmpty(mJarPath)) {
                return null;
            }
            // filter out invalid paths
            return Arrays.stream(mJarPath.split(File.pathSeparator))
                    .filter(s -> (s.startsWith(SYSTEM) || s.startsWith(PRODUCT)
                            || s.startsWith(SYSTEM_EXT)))
                    .filter(s -> {
                        try {
                            // This will also throw an error if the target doesn't exist.
                            StructStatVfs vfs = Os.statvfs(s);
                            return (vfs.f_flag & OsConstants.ST_RDONLY) != 0;
                        } catch (ErrnoException e) {
                            Rlog.w(TAG, "Injection jar is not protected , path: " + s
                                    + e.getMessage());
                            return false;
                        }
                    }).distinct()
                    .collect(Collectors.joining(File.pathSeparator));
        }

        private void makeInjectedInstance() {
            String validatedPaths = getValidatedPaths();
            Rlog.d(TAG, "validated paths: " + validatedPaths);
            if (!TextUtils.isEmpty(validatedPaths)) {
                try {
                    PathClassLoader classLoader = new PathClassLoader(validatedPaths,
                            ClassLoader.getSystemClassLoader());
                    Class<?> cls = classLoader.loadClass(mPackageName);
                    mInjectedInstance = (TelephonyComponentFactory) cls.newInstance();
                } catch (ClassNotFoundException e) {
                    Rlog.e(TAG, "failed: " + e.getMessage());
                } catch (IllegalAccessException | InstantiationException e) {
                    Rlog.e(TAG, "injection failed: " + e.getMessage());
                }
            }
        }

        private boolean isComponentInjected(String componentName) {
            if (mInjectedInstance == null) {
                return false;
            }
            return mComponentNames.contains(componentName);
        }

        /**
         * Find the injection tag, set attributes, and then parse the injection.
         */
        private void parseXml(@NonNull XmlPullParser parser) {
            parseXmlByTag(parser, false, p -> {
                setAttributes(p);
                parseInjection(p);
            }, TAG_INJECTION);
        }

        /**
         * Only parse the first injection tag. Find the components tag, then try parse it next.
         */
        private void parseInjection(@NonNull XmlPullParser parser) {
            parseXmlByTag(parser, false, p -> parseComponents(p), TAG_COMPONENTS);
        }

        /**
         * Only parse the first components tag. Find the component tags, then try parse them next.
         */
        private void parseComponents(@NonNull XmlPullParser parser) {
            parseXmlByTag(parser, true, p -> parseComponent(p), TAG_COMPONENT);
        }

        /**
         * Extract text values from component tags.
         */
        private void parseComponent(@NonNull XmlPullParser parser) {
            try {
                int outerDepth = parser.getDepth();
                int type;
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                        && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                    if (type == XmlPullParser.TEXT) {
                        mComponentNames.add(parser.getText());
                    }
                }
            } catch (XmlPullParserException | IOException e) {
                Rlog.e(TAG, "Failed to parse the component." , e);
            }
        }

        /**
         * Iterates the tags, finds the corresponding tag and then applies the consumer.
         */
        private void parseXmlByTag(@NonNull XmlPullParser parser, boolean allowDuplicate,
                @NonNull Consumer<XmlPullParser> consumer, @NonNull final String tag) {
            try {
                int outerDepth = parser.getDepth();
                int type;
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                        && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                    if (type == XmlPullParser.START_TAG && tag.equals(parser.getName())) {
                        consumer.accept(parser);
                        if (!allowDuplicate) {
                            return;
                        }
                    }
                }
            } catch (XmlPullParserException | IOException e) {
                Rlog.e(TAG, "Failed to parse or find tag: " + tag, e);
            }
        }

        /**
         * Sets the mPackageName and mJarPath by <injection/> tag.
         * @param parser
         * @return
         */
        private void setAttributes(@NonNull XmlPullParser parser) {
            for (int i = 0; i < parser.getAttributeCount(); i++) {
                String name = parser.getAttributeName(i);
                String value = parser.getAttributeValue(i);
                if (InjectedComponents.ATTRIBUTE_PACKAGE.equals(name)) {
                    mPackageName = value;
                } else if (InjectedComponents.ATTRIBUTE_JAR.equals(name)) {
                    mJarPath = value;
                }
            }
        }
    }

    public static TelephonyComponentFactory getInstance() {
        if (sInstance == null) {
            sInstance = new TelephonyComponentFactory();
        }
        return sInstance;
    }

    /**
     * Inject TelephonyComponentFactory using a xml config file.
     * @param parser a nullable {@link XmlResourceParser} created with the injection config file.
     * The config xml should has below formats:
     * <injection package="package.InjectedTelephonyComponentFactory" jar="path to jar file">
     *     <components>
     *         <component>example.package.ComponentAbc</component>
     *         <component>example.package.ComponentXyz</component>
     *         <!-- e.g. com.android.internal.telephony.GsmCdmaPhone -->
     *     </components>
     * </injection>
     */
    public void injectTheComponentFactory(XmlResourceParser parser) {
        if (mInjectedComponents != null) {
            Rlog.d(TAG, "Already injected.");
            return;
        }

        if (parser != null) {
            mInjectedComponents = new InjectedComponents();
            mInjectedComponents.parseXml(parser);
            mInjectedComponents.makeInjectedInstance();
            boolean injectSuccessful = !TextUtils.isEmpty(mInjectedComponents.getValidatedPaths());
            Rlog.d(TAG, "Total components injected: " + (injectSuccessful
                    ? mInjectedComponents.mComponentNames.size() : 0));
        }
    }

    /**
     * Use the injected TelephonyComponentFactory if configured. Otherwise, use the default.
     * @param componentName Name of the component class uses the injected component factory,
     * e.g. GsmCdmaPhone.class.getName() for {@link GsmCdmaPhone}
     * @return injected component factory. If not configured or injected, return the default one.
     */
    public TelephonyComponentFactory inject(String componentName) {
        if (mInjectedComponents != null && mInjectedComponents.isComponentInjected(componentName)) {
            return mInjectedComponents.mInjectedInstance;
        }
        return sInstance;
    }

    /**
     * Create a new GsmCdmaCallTracker
     * @param phone GsmCdmaPhone
     * @param featureFlags Telephony feature flag
     */
    public GsmCdmaCallTracker makeGsmCdmaCallTracker(GsmCdmaPhone phone,
            @NonNull FeatureFlags featureFlags) {
        return new GsmCdmaCallTracker(phone, featureFlags);
    }

    /**
     * Create {@link SmsStorageMonitor} instance.
     *
     * @param phone Phone instance
     * @param flags Android feature flags
     *
     * @return The created instance
     */
    public SmsStorageMonitor makeSmsStorageMonitor(Phone phone, @NonNull FeatureFlags flags) {
        return new SmsStorageMonitor(phone, flags);
    }

    public SmsUsageMonitor makeSmsUsageMonitor(Context context, FeatureFlags flags) {
        return new SmsUsageMonitor(context, flags);
    }

    public ServiceStateTracker makeServiceStateTracker(GsmCdmaPhone phone, CommandsInterface ci,
            @NonNull FeatureFlags featureFlags) {
        return new ServiceStateTracker(phone, ci, featureFlags);
    }

    /**
     * Create a new EmergencyNumberTracker.
     */
    public EmergencyNumberTracker makeEmergencyNumberTracker(Phone phone, CommandsInterface ci,
            @NonNull FeatureFlags featureFlags) {
        return new EmergencyNumberTracker(phone, ci, featureFlags);
    }

    private static final boolean USE_NEW_NITZ_STATE_MACHINE = true;

    /**
     * Returns a new {@link NitzStateMachine} instance.
     */
    public NitzStateMachine makeNitzStateMachine(GsmCdmaPhone phone) {
        return NitzStateMachineImpl.createInstance(phone);
    }

    public SimActivationTracker makeSimActivationTracker(Phone phone) {
        return new SimActivationTracker(phone);
    }

    public CarrierSignalAgent makeCarrierSignalAgent(Phone phone) {
        return new CarrierSignalAgent(phone);
    }

    public CarrierActionAgent makeCarrierActionAgent(Phone phone) {
        return new CarrierActionAgent(phone);
    }

    /**
     * Create {@link CarrierResolver} instance
     *
     * @param phone The phone instance
     * @param flags Android feature flags
     *
     * @return The created instance
     */
    public CarrierResolver makeCarrierResolver(Phone phone, FeatureFlags flags) {
        return new CarrierResolver(phone, flags);
    }

    public IccPhoneBookInterfaceManager makeIccPhoneBookInterfaceManager(Phone phone) {
        return new IccPhoneBookInterfaceManager(phone);
    }

    /**
     * Returns a new {@link IccSmsInterfaceManager} instance.
     */
    public IccSmsInterfaceManager makeIccSmsInterfaceManager(Phone phone,
            @NonNull FeatureFlags featureFlags) {
        return new IccSmsInterfaceManager(phone, featureFlags);
    }

    /**
     * Create a new UiccProfile object.
     */
    public UiccProfile makeUiccProfile(Context context, CommandsInterface ci, IccCardStatus ics,
                                       int phoneId, UiccCard uiccCard, Object lock,
            @NonNull FeatureFlags flags) {
        return new UiccProfile(context, ci, ics, phoneId, uiccCard, lock, flags);
    }

    public EriManager makeEriManager(Phone phone, int eriFileSource) {
        return new EriManager(phone, eriFileSource);
    }

    public WspTypeDecoder makeWspTypeDecoder(byte[] pdu) {
        return new WspTypeDecoder(pdu);
    }

    /**
     * Create a tracker for a single-part SMS.
     */
    public InboundSmsTracker makeInboundSmsTracker(Context context, byte[] pdu, long timestamp,
            int destPort, boolean is3gpp2, boolean is3gpp2WapPdu, String address,
            String displayAddr, String messageBody, boolean isClass0, int subId,
            @InboundSmsHandler.SmsSource int smsSource) {
        return new InboundSmsTracker(context, pdu, timestamp, destPort, is3gpp2, is3gpp2WapPdu,
                address, displayAddr, messageBody, isClass0, subId, smsSource);
    }

    /**
     * Create a tracker for a multi-part SMS.
     */
    public InboundSmsTracker makeInboundSmsTracker(Context context, byte[] pdu, long timestamp,
            int destPort, boolean is3gpp2, String address, String displayAddr, int referenceNumber,
            int sequenceNumber, int messageCount, boolean is3gpp2WapPdu, String messageBody,
            boolean isClass0, int subId, @InboundSmsHandler.SmsSource int smsSource) {
        return new InboundSmsTracker(context, pdu, timestamp, destPort, is3gpp2, address,
                displayAddr, referenceNumber, sequenceNumber, messageCount, is3gpp2WapPdu,
                messageBody, isClass0, subId, smsSource);
    }

    /**
     * Create a tracker from a row of raw table
     */
    public InboundSmsTracker makeInboundSmsTracker(Context context, Cursor cursor,
            boolean isCurrentFormat3gpp2) {
        return new InboundSmsTracker(context, cursor, isCurrentFormat3gpp2);
    }

    /**
     * Create an ImsPhoneCallTracker.
     *
     * @param imsPhone imsphone
     * @return ImsPhoneCallTracker newly created ImsPhoneCallTracker
     * @deprecated Use {@link #makeImsPhoneCallTracker(ImsPhone, FeatureFlags)} instead
     */
    public ImsPhoneCallTracker makeImsPhoneCallTracker(ImsPhone imsPhone) {
        return makeImsPhoneCallTracker(imsPhone, new FeatureFlagsImpl());
    }

    /**
     * Create a ims phone call tracker.
     *
     * @param imsPhone imsphone
     * @param featureFlags feature flags
     * @return ImsPhoneCallTracker newly created ImsPhoneCallTracker
     */
    public ImsPhoneCallTracker makeImsPhoneCallTracker(ImsPhone imsPhone,
                                                       @NonNull FeatureFlags featureFlags) {
        return new ImsPhoneCallTracker(imsPhone, ImsManager::getConnector, featureFlags);
    }

    public ImsExternalCallTracker makeImsExternalCallTracker(ImsPhone imsPhone) {

        return new ImsExternalCallTracker(imsPhone, imsPhone.getContext().getMainExecutor());
    }

    /**
     * Create an ImsNrSaModeHandler.
     */
    public ImsNrSaModeHandler makeImsNrSaModeHandler(ImsPhone imsPhone) {

        return new ImsNrSaModeHandler(imsPhone, imsPhone.getLooper());
    }

    /**
     * Create an AppSmsManager for per-app SMS message.
     */
    public AppSmsManager makeAppSmsManager(Context context) {
        return new AppSmsManager(context);
    }

    /**
     * Create a DeviceStateMonitor.
     */
    public DeviceStateMonitor makeDeviceStateMonitor(Phone phone,
            @NonNull FeatureFlags featureFlags) {
        return new DeviceStateMonitor(phone, featureFlags);
    }

    /**
     * Make access networks manager
     *
     * @param phone The phone instance
     * @param looper Looper for the handler.
     * @return The access networks manager
     * @deprecated {@link #makeAccessNetworksManager(Phone, Looper, FeatureFlags)} instead
     */
    public AccessNetworksManager makeAccessNetworksManager(Phone phone, Looper looper) {
        return new AccessNetworksManager(phone, looper, new FeatureFlagsImpl());
    }

    /**
     * Make access networks manager
     *
     * @param phone The phone instance
     * @param looper Looper for the handler.
     * @param featureFlags feature flags.
     * @return The access networks manager
     */
    public AccessNetworksManager makeAccessNetworksManager(Phone phone, Looper looper,
            @NonNull FeatureFlags featureFlags) {
        return new AccessNetworksManager(phone, looper, featureFlags);
    }

    public CdmaSubscriptionSourceManager
    getCdmaSubscriptionSourceManagerInstance(Context context, CommandsInterface ci, Handler h,
                                             int what, Object obj) {
        return CdmaSubscriptionSourceManager.getInstance(context, ci, h, what, obj);
    }

    public LocaleTracker makeLocaleTracker(Phone phone, NitzStateMachine nitzStateMachine,
                                           Looper looper, @NonNull FeatureFlags featureFlags) {
        return new LocaleTracker(phone, nitzStateMachine, looper, featureFlags);
    }

    public Phone makePhone(Context context, CommandsInterface ci, PhoneNotifier notifier,
            int phoneId, int precisePhoneType,
            TelephonyComponentFactory telephonyComponentFactory,
            @NonNull FeatureFlags featureFlags) {
        return new GsmCdmaPhone(context, ci, notifier, phoneId, precisePhoneType,
                telephonyComponentFactory, featureFlags);
    }

    public PhoneSwitcher makePhoneSwitcher(int maxDataAttachModemCount, Context context,
            Looper looper, @NonNull FeatureFlags featureFlags) {
        return PhoneSwitcher.make(maxDataAttachModemCount, context, looper, featureFlags);
    }

    /**
     * Create a new DisplayInfoController.
     */
    public DisplayInfoController makeDisplayInfoController(Phone phone, FeatureFlags featureFlags) {
        return new DisplayInfoController(phone, featureFlags);
    }

    /**
     * Initialize multi sim settings controller.
     *
     * @param c The context.
     * @return The multi sim settings controller instance.
     */
    public MultiSimSettingController initMultiSimSettingController(Context c,
            @NonNull FeatureFlags featureFlags) {
        return MultiSimSettingController.init(c, featureFlags);
    }

    /**
     * Create a new SignalStrengthController instance.
     */
    public SignalStrengthController makeSignalStrengthController(GsmCdmaPhone phone) {
        return new SignalStrengthController(phone);
    }

    /**
     * Create a new LinkBandwidthEstimator.
     */
    public LinkBandwidthEstimator makeLinkBandwidthEstimator(Phone phone, Looper looper) {
        return new LinkBandwidthEstimator(phone, looper, mTelephonyFacade);
    }

    /**
     * Create a new data network controller instance. The instance is per-SIM. On multi-sim devices,
     * there will be multiple {@link DataNetworkController} instances.
     *
     * @param phone The phone object
     * @param looper The looper for event handling
     * @param featureFlags The feature flag.
     * @return The data network controller instance
     */
    public DataNetworkController makeDataNetworkController(Phone phone, Looper looper,
            @NonNull FeatureFlags featureFlags) {
        return new DataNetworkController(phone, looper, featureFlags);
    }

    /**
     * Create data profile manager.
     *
     * @param phone The phone instance.
     * @param dataNetworkController Data network controller instance.
     * @param dataServiceManager Data service manager instance.
     * @param looper The looper to be used by the handler. Currently the handler thread is the phone
     * process's main thread.
     * @param featureFlags Feature flags controlling which feature is enabled.     *
     * @param callback Callback for passing events back to data network controller.
     * @return The data profile manager instance.
     */
    public @NonNull DataProfileManager makeDataProfileManager(@NonNull Phone phone,
            @NonNull DataNetworkController dataNetworkController,
            @NonNull DataServiceManager dataServiceManager, @NonNull Looper looper,
            @NonNull FeatureFlags featureFlags,
            @NonNull DataProfileManager.DataProfileManagerCallback callback) {
        return new DataProfileManager(phone, dataNetworkController, dataServiceManager, looper,
                featureFlags, callback);
    }

    /**
     * Create data settings manager.
     *
     * @param phone The phone instance.
     * @param dataNetworkController Data network controller instance.
     * @param looper The looper to be used by the handler. Currently the handler thread is the phone
     * process's main thread.
     * @param callback Callback for passing events back to data network controller.
     * @return The data settings manager instance.
     */
    public @NonNull DataSettingsManager makeDataSettingsManager(@NonNull Phone phone,
            @NonNull DataNetworkController dataNetworkController,
            @NonNull FeatureFlags featureFlags, @NonNull Looper looper,
            @NonNull DataSettingsManager.DataSettingsManagerCallback callback) {
        return new DataSettingsManager(phone, dataNetworkController, featureFlags, looper,
                callback);
    }

    /** Create CellularNetworkSecuritySafetySource. */
    public CellularNetworkSecuritySafetySource makeCellularNetworkSecuritySafetySource(
            Context context) {
        return CellularNetworkSecuritySafetySource.getInstance(context);
    }

    /** Create CellularIdentifierDisclosureNotifier. */
    public CellularIdentifierDisclosureNotifier makeIdentifierDisclosureNotifier(
            CellularNetworkSecuritySafetySource safetySource) {
        return CellularIdentifierDisclosureNotifier.getInstance(safetySource);
    }

    /** Create NullCipherNotifier. */
    public NullCipherNotifier makeNullCipherNotifier(
            CellularNetworkSecuritySafetySource safetySource) {
        return NullCipherNotifier.getInstance(safetySource);
    }
}
