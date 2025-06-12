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

package com.android.server.pm.verify.domain;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.content.UriRelativeFilter;
import android.content.UriRelativeFilterGroup;
import android.content.pm.Signature;
import android.content.pm.verify.domain.DomainVerificationState;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.PackageUtils;
import android.util.SparseArray;

import com.android.modules.utils.TypedXmlPullParser;
import com.android.modules.utils.TypedXmlSerializer;
import com.android.server.pm.SettingsXml;
import com.android.server.pm.verify.domain.models.DomainVerificationInternalUserState;
import com.android.server.pm.verify.domain.models.DomainVerificationPkgState;
import com.android.server.pm.verify.domain.models.DomainVerificationStateMap;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class DomainVerificationPersistence {

    private static final String TAG = "DomainVerificationPersistence";

    public static final String TAG_DOMAIN_VERIFICATIONS = "domain-verifications";
    public static final String TAG_ACTIVE = "active";
    public static final String TAG_RESTORED = "restored";

    public static final String TAG_PACKAGE_STATE = "package-state";
    private static final String ATTR_PACKAGE_NAME = "packageName";
    private static final String ATTR_ID = "id";
    private static final String ATTR_HAS_AUTO_VERIFY_DOMAINS = "hasAutoVerifyDomains";
    private static final String ATTR_SIGNATURE = "signature";
    private static final String TAG_USER_STATES = "user-states";

    public static final String TAG_USER_STATE = "user-state";
    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_ALLOW_LINK_HANDLING = "allowLinkHandling";
    public static final String TAG_ENABLED_HOSTS = "enabled-hosts";
    public static final String TAG_HOST = "host";

    private static final String TAG_STATE = "state";
    public static final String TAG_DOMAIN = "domain";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_STATE = "state";
    public static final String TAG_URI_RELATIVE_FILTER_GROUPS = "uri-relative-filter-groups";
    public static final String TAG_URI_RELATIVE_FILTER_GROUP = "uri-relative-filter-group";
    public static final String ATTR_ACTION = "action";
    public static final String TAG_URI_RELATIVE_FILTER = "uri-relative-filter";
    public static final String ATTR_URI_PART = "uri-part";
    public static final String ATTR_PATTERN_TYPE = "pattern-type";
    public static final String ATTR_FILTER = "filter";

    /**
     * @param pkgNameToSignature Converts package name to a string representation of its signature.
     *                           Usually this is the SHA-256 hash from
     *                           {@link PackageUtils#computeSignaturesSha256Digest(Signature[])},
     *                           but can be an arbitrary string for testing purposes. Pass non-null
     *                           to write out signatures, or null to ignore.
     */
    public static void writeToXml(@NonNull TypedXmlSerializer xmlSerializer,
            @NonNull DomainVerificationStateMap<DomainVerificationPkgState> attached,
            @NonNull ArrayMap<String, DomainVerificationPkgState> pending,
            @NonNull ArrayMap<String, DomainVerificationPkgState> restored,
            @UserIdInt int userId, @Nullable Function<String, String> pkgNameToSignature)
            throws IOException {
        try (SettingsXml.Serializer serializer = SettingsXml.serializer(xmlSerializer)) {
            try (SettingsXml.WriteSection ignored = serializer.startSection(
                    TAG_DOMAIN_VERIFICATIONS)) {
                // Both attached and pending states are written to the active set, since both
                // should be restored when the device reboots or runs a backup. They're merged into
                // the same list because at read time the distinction isn't relevant. The pending
                // list should generally be empty at this point anyways.
                ArraySet<DomainVerificationPkgState> active = new ArraySet<>();

                int attachedSize = attached.size();
                for (int attachedIndex = 0; attachedIndex < attachedSize; attachedIndex++) {
                    active.add(attached.valueAt(attachedIndex));
                }

                int pendingSize = pending.size();
                for (int pendingIndex = 0; pendingIndex < pendingSize; pendingIndex++) {
                    active.add(pending.valueAt(pendingIndex));
                }

                try (SettingsXml.WriteSection activeSection = serializer.startSection(TAG_ACTIVE)) {
                    writePackageStates(activeSection, active, userId, pkgNameToSignature);
                }

                try (SettingsXml.WriteSection restoredSection = serializer.startSection(
                        TAG_RESTORED)) {
                    writePackageStates(restoredSection, restored.values(), userId,
                            pkgNameToSignature);
                }
            }
        }
    }

    private static void writePackageStates(@NonNull SettingsXml.WriteSection section,
            @NonNull Collection<DomainVerificationPkgState> states, int userId,
            @Nullable Function<String, String> pkgNameToSignature) throws IOException {
        if (states.isEmpty()) {
            return;
        }

        for (DomainVerificationPkgState state : states) {
            writePkgStateToXml(section, state, userId, pkgNameToSignature);
        }
    }

    @NonNull
    public static ReadResult readFromXml(@NonNull TypedXmlPullParser parentParser)
            throws IOException, XmlPullParserException {
        ArrayMap<String, DomainVerificationPkgState> active = new ArrayMap<>();
        ArrayMap<String, DomainVerificationPkgState> restored = new ArrayMap<>();

        SettingsXml.ChildSection child = SettingsXml.parser(parentParser).children();
        while (child.moveToNext()) {
            switch (child.getName()) {
                case TAG_ACTIVE:
                    readPackageStates(child, active);
                    break;
                case TAG_RESTORED:
                    readPackageStates(child, restored);
                    break;
            }
        }

        return new ReadResult(active, restored);
    }

    private static void readPackageStates(@NonNull SettingsXml.ReadSection section,
            @NonNull ArrayMap<String, DomainVerificationPkgState> map) {
        SettingsXml.ChildSection child = section.children();
        while (child.moveToNext(TAG_PACKAGE_STATE)) {
            DomainVerificationPkgState pkgState = createPkgStateFromXml(child);
            if (pkgState != null) {
                // State is unique by package name
                map.put(pkgState.getPackageName(), pkgState);
            }
        }
    }

    /**
     * Reads a package state from XML. Assumes the starting {@link #TAG_PACKAGE_STATE} has already
     * been entered.
     */
    @Nullable
    private static DomainVerificationPkgState createPkgStateFromXml(
            @NonNull SettingsXml.ReadSection section) {
        String packageName = section.getString(ATTR_PACKAGE_NAME);
        String idString = section.getString(ATTR_ID);
        boolean hasAutoVerifyDomains = section.getBoolean(ATTR_HAS_AUTO_VERIFY_DOMAINS);
        String signature = section.getString(ATTR_SIGNATURE);
        if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(idString)) {
            return null;
        }
        UUID id = UUID.fromString(idString);

        final ArrayMap<String, Integer> stateMap = new ArrayMap<>();
        final SparseArray<DomainVerificationInternalUserState> userStates = new SparseArray<>();
        final ArrayMap<String, List<UriRelativeFilterGroup>> groupMap = new ArrayMap<>();

        SettingsXml.ChildSection child = section.children();
        while (child.moveToNext()) {
            switch (child.getName()) {
                case TAG_STATE:
                    readDomainStates(child, stateMap);
                    break;
                case TAG_USER_STATES:
                    readUserStates(child, userStates);
                    break;
                case TAG_URI_RELATIVE_FILTER_GROUPS:
                    readUriRelativeFilterGroups(child, groupMap);
                    break;
            }
        }

        return new DomainVerificationPkgState(packageName, id, hasAutoVerifyDomains, stateMap,
                userStates, signature, groupMap);
    }

    private static void readUriRelativeFilterGroups(@NonNull SettingsXml.ReadSection section,
            @NonNull ArrayMap<String, List<UriRelativeFilterGroup>> groupMap) {
        SettingsXml.ChildSection child = section.children();
        while (child.moveToNext(TAG_DOMAIN)) {
            String domain = child.getString(ATTR_NAME);
            groupMap.put(domain, createUriRelativeFilterGroupsFromXml(child));
        }
    }

    private static ArrayList<UriRelativeFilterGroup> createUriRelativeFilterGroupsFromXml(
            @NonNull SettingsXml.ReadSection section) {
        SettingsXml.ChildSection child = section.children();
        ArrayList<UriRelativeFilterGroup> groups = new ArrayList<>();
        while (child.moveToNext(TAG_URI_RELATIVE_FILTER_GROUP)) {
            UriRelativeFilterGroup group = new UriRelativeFilterGroup(section.getInt(ATTR_ACTION));
            readUriRelativeFiltersFromXml(child, group);
            groups.add(group);
        }
        return groups;
    }

    private static void readUriRelativeFiltersFromXml(
            @NonNull SettingsXml.ReadSection section, UriRelativeFilterGroup group) {
        SettingsXml.ChildSection child = section.children();
        while (child.moveToNext(TAG_URI_RELATIVE_FILTER)) {
            String filter = child.getString(ATTR_FILTER);
            if (filter != null) {
                group.addUriRelativeFilter(new UriRelativeFilter(child.getInt(ATTR_URI_PART),
                        child.getInt(ATTR_PATTERN_TYPE), filter));
            }
        }
    }

    private static void readUserStates(@NonNull SettingsXml.ReadSection section,
            @NonNull SparseArray<DomainVerificationInternalUserState> userStates) {
        SettingsXml.ChildSection child = section.children();
        while (child.moveToNext(TAG_USER_STATE)) {
            DomainVerificationInternalUserState userState = createUserStateFromXml(child);
            if (userState != null) {
                userStates.put(userState.getUserId(), userState);
            }
        }
    }

    private static void readDomainStates(@NonNull SettingsXml.ReadSection stateSection,
            @NonNull ArrayMap<String, Integer> stateMap) {
        SettingsXml.ChildSection child = stateSection.children();
        while (child.moveToNext(TAG_DOMAIN)) {
            String name = child.getString(ATTR_NAME);
            int state = child.getInt(ATTR_STATE, DomainVerificationState.STATE_NO_RESPONSE);
            stateMap.put(name, state);
        }
    }

    private static void writePkgStateToXml(@NonNull SettingsXml.WriteSection parentSection,
            @NonNull DomainVerificationPkgState pkgState, @UserIdInt int userId,
            @Nullable Function<String, String> pkgNameToSignature) throws IOException {
        String packageName = pkgState.getPackageName();
        String signature = pkgNameToSignature == null
                ? null : pkgNameToSignature.apply(packageName);
        if (signature == null) {
            // If a package isn't available to get its signature, fallback to the previously stored
            // result, which can occur if the package has been marked for restore but hasn't
            // been installed on the new device yet.
            signature = pkgState.getBackupSignatureHash();
        }

        try (SettingsXml.WriteSection ignored =
                     parentSection.startSection(TAG_PACKAGE_STATE)
                             .attribute(ATTR_PACKAGE_NAME, packageName)
                             .attribute(ATTR_ID, pkgState.getId().toString())
                             .attribute(ATTR_HAS_AUTO_VERIFY_DOMAINS,
                                     pkgState.isHasAutoVerifyDomains())
                             .attribute(ATTR_SIGNATURE, signature)) {
            writeStateMap(parentSection, pkgState.getStateMap());
            writeUserStates(parentSection, userId, pkgState.getUserStates());
            writeUriRelativeFilterGroupMap(parentSection, pkgState.getUriRelativeFilterGroupMap());
        }
    }

    private static void writeUserStates(@NonNull SettingsXml.WriteSection parentSection,
            @UserIdInt int userId,
            @NonNull SparseArray<DomainVerificationInternalUserState> states) throws IOException {
        int size = states.size();
        if (size == 0) {
            return;
        }

        try (SettingsXml.WriteSection section = parentSection.startSection(TAG_USER_STATES)) {
            if (userId == UserHandle.USER_ALL) {
                for (int index = 0; index < size; index++) {
                    writeUserStateToXml(section, states.valueAt(index));
                }
            } else {
                DomainVerificationInternalUserState userState = states.get(userId);
                if (userState != null) {
                    writeUserStateToXml(section, userState);
                }
            }
        }
    }

    private static void writeStateMap(@NonNull SettingsXml.WriteSection parentSection,
            @NonNull ArrayMap<String, Integer> stateMap) throws IOException {
        if (stateMap.isEmpty()) {
            return;
        }

        try (SettingsXml.WriteSection stateSection = parentSection.startSection(TAG_STATE)) {
            int size = stateMap.size();
            for (int index = 0; index < size; index++) {
                stateSection.startSection(TAG_DOMAIN)
                        .attribute(ATTR_NAME, stateMap.keyAt(index))
                        .attribute(ATTR_STATE, stateMap.valueAt(index))
                        .finish();
            }
        }
    }

    /**
     * Reads a user state from XML. Assumes the starting {@link #TAG_USER_STATE} has already been
     * entered.
     */
    @Nullable
    private static DomainVerificationInternalUserState createUserStateFromXml(
            @NonNull SettingsXml.ReadSection section) {
        int userId = section.getInt(ATTR_USER_ID);
        if (userId == -1) {
            return null;
        }

        boolean allowLinkHandling = section.getBoolean(ATTR_ALLOW_LINK_HANDLING, false);
        ArraySet<String> enabledHosts = new ArraySet<>();

        SettingsXml.ChildSection child = section.children();
        while (child.moveToNext(TAG_ENABLED_HOSTS)) {
            readEnabledHosts(child, enabledHosts);
        }

        return new DomainVerificationInternalUserState(userId, enabledHosts, allowLinkHandling);
    }

    private static void readEnabledHosts(@NonNull SettingsXml.ReadSection section,
            @NonNull ArraySet<String> enabledHosts) {
        SettingsXml.ChildSection child = section.children();
        while (child.moveToNext(TAG_HOST)) {
            String hostName = child.getString(ATTR_NAME);
            if (!TextUtils.isEmpty(hostName)) {
                enabledHosts.add(hostName);
            }
        }
    }

    private static void writeUserStateToXml(@NonNull SettingsXml.WriteSection parentSection,
            @NonNull DomainVerificationInternalUserState userState) throws IOException {
        try (SettingsXml.WriteSection section =
                     parentSection.startSection(TAG_USER_STATE)
                             .attribute(ATTR_USER_ID, userState.getUserId())
                             .attribute(ATTR_ALLOW_LINK_HANDLING,
                                     userState.isLinkHandlingAllowed())) {
            ArraySet<String> enabledHosts = userState.getEnabledHosts();
            if (!enabledHosts.isEmpty()) {
                try (SettingsXml.WriteSection enabledHostsSection =
                             section.startSection(TAG_ENABLED_HOSTS)) {
                    int size = enabledHosts.size();
                    for (int index = 0; index < size; index++) {
                        enabledHostsSection.startSection(TAG_HOST)
                                .attribute(ATTR_NAME, enabledHosts.valueAt(index))
                                .finish();
                    }
                }
            }
        }
    }

    private static void writeUriRelativeFilterGroupMap(
            @NonNull SettingsXml.WriteSection parentSection,
            @NonNull ArrayMap<String, List<UriRelativeFilterGroup>> groupMap) throws IOException {
        if (groupMap.isEmpty()) {
            return;
        }
        try (SettingsXml.WriteSection section =
                     parentSection.startSection(TAG_URI_RELATIVE_FILTER_GROUPS)) {
            for (int i = 0; i < groupMap.size(); i++) {
                writeUriRelativeFilterGroups(section, groupMap.keyAt(i), groupMap.valueAt(i));
            }
        }
    }

    private static void writeUriRelativeFilterGroups(
            @NonNull SettingsXml.WriteSection parentSection, @NonNull String domain,
            @NonNull List<UriRelativeFilterGroup> groups) throws IOException {
        if (groups.isEmpty()) {
            return;
        }
        try (SettingsXml.WriteSection section =
                     parentSection.startSection(TAG_DOMAIN)
                             .attribute(ATTR_NAME, domain)) {
            for (int i = 0; i < groups.size(); i++) {
                writeUriRelativeFilterGroup(section, groups.get(i));
            }
        }
    }

    private static void writeUriRelativeFilterGroup(
            @NonNull SettingsXml.WriteSection parentSection,
            @NonNull UriRelativeFilterGroup group) throws IOException {
        try (SettingsXml.WriteSection section =
                     parentSection.startSection(TAG_URI_RELATIVE_FILTER_GROUP)
                             .attribute(ATTR_ACTION, group.getAction())) {
            Iterator<UriRelativeFilter> it = group.getUriRelativeFilters().iterator();
            while (it.hasNext()) {
                UriRelativeFilter filter = it.next();
                section.startSection(TAG_URI_RELATIVE_FILTER)
                        .attribute(ATTR_URI_PART, filter.getUriPart())
                        .attribute(ATTR_PATTERN_TYPE, filter.getPatternType())
                        .attribute(ATTR_FILTER, filter.getFilter()).finish();
            }
        }
    }

    public static class ReadResult {

        @NonNull
        public final ArrayMap<String, DomainVerificationPkgState> active;

        @NonNull
        public final ArrayMap<String, DomainVerificationPkgState> restored;

        public ReadResult(@NonNull ArrayMap<String, DomainVerificationPkgState> active,
                @NonNull ArrayMap<String, DomainVerificationPkgState> restored) {
            this.active = active;
            this.restored = restored;
        }
    }
}
