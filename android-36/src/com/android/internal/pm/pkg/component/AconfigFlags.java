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

package com.android.internal.pm.pkg.component;

import static android.provider.flags.Flags.newStoragePublicApi;
import static com.android.internal.pm.pkg.parsing.ParsingUtils.ANDROID_RES_NAMESPACE;

import android.aconfig.DeviceProtos;
import android.aconfig.nano.Aconfig;
import android.aconfig.nano.Aconfig.parsed_flag;
import android.aconfig.nano.Aconfig.parsed_flags;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.res.Flags;
import android.os.Environment;
import android.os.Process;
import android.os.flagging.AconfigPackage;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.Xml;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.pm.pkg.parsing.ParsingPackage;
import com.android.modules.utils.TypedXmlPullParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class that manages a cache of all device feature flags and their default + override values.
 * This class performs a very similar job to the one in {@code SettingsProvider}, with an important
 * difference: this is a part of system server and is available for the server startup. Package
 * parsing happens at the startup when {@code SettingsProvider} isn't available yet, so we need an
 * own copy of the code here.
 * @hide
 */
public class AconfigFlags {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "AconfigFlags";
    private static final String OVERRIDE_PREFIX = "device_config_overrides/";
    private static final String STAGED_PREFIX = "staged/";

    private final Map<String, Boolean> mFlagValues = new ArrayMap<>();
    private final Map<String, AconfigPackage> mAconfigPackages = new ConcurrentHashMap<>();

    public AconfigFlags() {
        if (!Flags.manifestFlagging()) {
            if (DEBUG) {
                Slog.v(LOG_TAG, "Feature disabled, skipped all loading");
            }
            return;
        }

        if (useNewStorage()) {
            Slog.i(LOG_TAG, "Using new flag storage");
        } else {
            Slog.i(LOG_TAG, "Using OLD proto flag storage");
            final var defaultFlagProtoFiles =
                    (Process.myUid() == Process.SYSTEM_UID) ? DeviceProtos.parsedFlagsProtoPaths()
                            : Arrays.asList(DeviceProtos.PATHS);
            for (String fileName : defaultFlagProtoFiles) {
                final File protoFile = new File(fileName);
                if (!protoFile.isFile() || !protoFile.canRead()) {
                    continue;
                }
                try (var inputStream = new FileInputStream(protoFile)) {
                    loadAconfigDefaultValues(inputStream.readAllBytes());
                } catch (IOException e) {
                    Slog.w(LOG_TAG, "Failed to read Aconfig values from " + fileName, e);
                }
            }
            if (Process.myUid() == Process.SYSTEM_UID) {
                // Server overrides are only accessible to the system, no need to even try loading
                // them in user processes.
                loadServerOverrides();
            }
        }
    }

    private static boolean useNewStorage() {
        return newStoragePublicApi() && Flags.useNewAconfigStorage();
    }

    private void loadServerOverrides() {
        // Reading the proto files is enough for READ_ONLY flags but if it's a READ_WRITE flag
        // (which you can check with `flag.getPermission() == flag_permission.READ_WRITE`) then we
        // also need to check if there is a value pushed from the server in the file
        // `/data/system/users/0/settings_config.xml`. It will be in a <setting> node under the
        // root <settings> node with "name" attribute == "flag_namespace/flag_package.flag_name".
        // The "value" attribute will be true or false.
        //
        // The "name" attribute could also be "<namespace>/flag_namespace?flag_package.flag_name"
        // (prefixed with "staged/" or "device_config_overrides/" and a different separator between
        // namespace and name). This happens when a flag value is overridden either with a pushed
        // one from the server, or from the local command.
        // When the device reboots during package parsing, the staged value will still be there and
        // only later it will become a regular/non-staged value after SettingsProvider is
        // initialized.
        //
        // In all cases, when there is more than one value, the priority is:
        //      device_config_overrides > staged > default
        //

        final var settingsFile = new File(Environment.getUserSystemDirectory(0),
                "settings_config.xml");
        if (!settingsFile.isFile() || !settingsFile.canRead()) {
            return;
        }
        try (var inputStream = new FileInputStream(settingsFile)) {
            TypedXmlPullParser parser = Xml.resolvePullParser(inputStream);
            if (parser.next() != XmlPullParser.END_TAG && "settings".equals(parser.getName())) {
                final var flagPriority = new ArrayMap<String, Integer>();
                final int outerDepth = parser.getDepth();
                int type;
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                        && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                    if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                        continue;
                    }
                    if (!"setting".equals(parser.getName())) {
                        continue;
                    }
                    String name = parser.getAttributeValue(null, "name");
                    final String value = parser.getAttributeValue(null, "value");
                    if (name == null || value == null) {
                        continue;
                    }
                    // A non-boolean setting is definitely not an Aconfig flag value.
                    if (!"false".equalsIgnoreCase(value) && !"true".equalsIgnoreCase(value)) {
                        continue;
                    }
                    String separator = "/";
                    String prefix = "default";
                    int priority = 0;
                    if (name.startsWith(OVERRIDE_PREFIX)) {
                        prefix = OVERRIDE_PREFIX;
                        name = name.substring(OVERRIDE_PREFIX.length());
                        separator = ":";
                        priority = 20;
                    } else if (name.startsWith(STAGED_PREFIX)) {
                        prefix = STAGED_PREFIX;
                        name = name.substring(STAGED_PREFIX.length());
                        separator = "*";
                        priority = 10;
                    }
                    final String flagPackageAndName = parseFlagPackageAndName(name, separator);
                    if (flagPackageAndName == null) {
                        continue;
                    }
                    // We ignore all settings that aren't for flags. We'll know they are for flags
                    // if they correspond to flags read from the proto files.
                    if (!mFlagValues.containsKey(flagPackageAndName)) {
                        continue;
                    }
                    if (DEBUG) {
                        Slog.d(LOG_TAG, "Found " + prefix
                                + " Aconfig flag value in settings for " + flagPackageAndName
                                + " = " + value);
                    }
                    final Integer currentPriority = flagPriority.get(flagPackageAndName);
                    if (currentPriority != null && currentPriority >= priority) {
                        if (DEBUG) {
                            Slog.d(LOG_TAG, "Skipping " + prefix + " flag "
                                    + flagPackageAndName
                                    + " in settings because of existing one with priority "
                                    + currentPriority);
                        }
                        continue;
                    }
                    flagPriority.put(flagPackageAndName, priority);
                    mFlagValues.put(flagPackageAndName, Boolean.parseBoolean(value));
                }
            }
        } catch (IOException | XmlPullParserException e) {
            Slog.w(LOG_TAG, "Failed to read Aconfig values from settings_config.xml", e);
        }
    }

    private static String parseFlagPackageAndName(String fullName, String separator) {
        int index = fullName.indexOf(separator);
        if (index < 0) {
            return null;
        }
        return fullName.substring(index + 1);
    }

    private void loadAconfigDefaultValues(byte[] fileContents) throws IOException {
        parsed_flags parsedFlags = parsed_flags.parseFrom(fileContents);
        for (parsed_flag flag : parsedFlags.parsedFlag) {
            String flagPackageAndName = flag.package_ + "." + flag.name;
            boolean flagValue = (flag.state == Aconfig.ENABLED);
            mFlagValues.put(flagPackageAndName, flagValue);
        }
    }

    /**
     * Get the flag value, or null if the flag doesn't exist.
     * @param flagPackageAndName Full flag name formatted as 'package.flag'
     * @return the current value of the given Aconfig flag, or null if there is no such flag
     */
    @Nullable
    public Boolean getFlagValue(@NonNull String flagPackageAndName) {
        if (useNewStorage()) {
            return getFlagValueFromNewStorage(flagPackageAndName);
        } else {
            Boolean value = mFlagValues.get(flagPackageAndName);
            if (DEBUG) {
                Slog.v(LOG_TAG, "Aconfig flag value for " + flagPackageAndName + " = " + value);
            }
            return value;
        }
    }

    private Boolean getFlagValueFromNewStorage(String flagPackageAndName) {
        // We still need to check mFlagValues in case addFlagValuesForTesting() was called for
        // testing purposes.
        if (!mFlagValues.isEmpty() && mFlagValues.containsKey(flagPackageAndName)) {
            Boolean value = mFlagValues.get(flagPackageAndName);
            if (DEBUG) {
                Slog.v(
                        LOG_TAG,
                        "Aconfig flag value (FOR TESTING) for "
                                + flagPackageAndName
                                + " = "
                                + value);
            }
            return value;
        }

        int index = flagPackageAndName.lastIndexOf('.');
        if (index < 0) {
            Slog.e(LOG_TAG, "Unable to parse package name from " + flagPackageAndName);
            return null;
        }
        String flagPackage = flagPackageAndName.substring(0, index);
        String flagName = flagPackageAndName.substring(index + 1);
        Boolean value = null;
        AconfigPackage aconfigPackage = mAconfigPackages.computeIfAbsent(flagPackage, p -> {
            try {
                return AconfigPackage.load(p);
            } catch (Exception e) {
                Slog.e(LOG_TAG, "Failed to load aconfig package " + p, e);
                return null;
            }
        });
        if (aconfigPackage != null) {
            // Default value is false for when the flag is not found.
            // Note: Unlike with the old storage, with AconfigPackage, we don't have a way to
            // know if the flag is not found or if it's found but the value is false.
            try {
                value = aconfigPackage.getBooleanFlagValue(flagName, false);
            } catch (Exception e) {
                Slog.e(LOG_TAG, "Failed to read Aconfig flag value for " + flagPackageAndName, e);
                return null;
            }
        }
        if (DEBUG) {
            Slog.v(LOG_TAG, "Aconfig flag value for " + flagPackageAndName + " = " + value);
        }
        return value;
    }

    /**
     * Check if the element in {@code parser} should be skipped because of the feature flag.
     * @param pkg The package being parsed
     * @param parser XML parser object currently parsing an element
     * @return true if the element is disabled because of its feature flag
     */
    public boolean skipCurrentElement(@Nullable ParsingPackage pkg, @NonNull XmlPullParser parser) {
        return skipCurrentElement(pkg, parser, /* allowNoNamespace= */ false);
    }

    /**
     * Check if the element in {@code parser} should be skipped because of the feature flag.
     * @param pkg The package being parsed
     * @param parser XML parser object currently parsing an element
     * @param allowNoNamespace Whether to allow namespace null
     * @return true if the element is disabled because of its feature flag
     */
    public boolean skipCurrentElement(
        @Nullable ParsingPackage pkg,
        @NonNull XmlPullParser parser,
        boolean allowNoNamespace
    ) {
        if (!Flags.manifestFlagging()) {
            return false;
        }
        String featureFlag = parser.getAttributeValue(ANDROID_RES_NAMESPACE, "featureFlag");
        // If allow no namespace, make another attempt to parse feature flag with null namespace.
        if (featureFlag == null && allowNoNamespace) {
            featureFlag = parser.getAttributeValue(null, "featureFlag");
        }
        if (featureFlag == null) {
            return false;
        }
        featureFlag = featureFlag.strip();
        boolean negated = false;
        if (featureFlag.startsWith("!")) {
            negated = true;
            featureFlag = featureFlag.substring(1).strip();
        }
        Boolean flagValue = getFlagValue(featureFlag);
        boolean isUndefined = false;
        if (flagValue == null) {
            isUndefined = true;
            flagValue = false;
        }
        boolean shouldSkip = false;
        if (flagValue == negated) {
            // Skip if flag==false && attr=="flag" OR flag==true && attr=="!flag" (negated)
            shouldSkip = true;
        }
        if (pkg != null && android.content.pm.Flags.includeFeatureFlagsInPackageCacher()) {
            if (isUndefined) {
                pkg.addFeatureFlag(featureFlag, null);
            } else {
                pkg.addFeatureFlag(featureFlag, flagValue);
            }
        }
        return shouldSkip;
    }

    /**
     * Add Aconfig flag values for testing flagging of manifest entries.
     * @param flagValues A map of flag name -> value.
     */
    @VisibleForTesting
    public void addFlagValuesForTesting(@NonNull Map<String, Boolean> flagValues) {
        mFlagValues.putAll(flagValues);
    }
}
