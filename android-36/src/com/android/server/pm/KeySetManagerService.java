/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.server.pm;

import static android.content.pm.PackageManager.INSTALL_FAILED_INVALID_APK;

import static com.android.server.pm.PackageManagerService.SCAN_INITIAL;

import android.annotation.NonNull;
import android.content.pm.parsing.FrameworkParsingPackageUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Base64;
import android.util.LongSparseArray;
import android.util.Slog;

import com.android.modules.utils.TypedXmlPullParser;
import com.android.modules.utils.TypedXmlSerializer;
import com.android.server.pm.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.SharedUserApi;
import com.android.server.utils.WatchedArrayMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.PublicKey;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/*
 * Manages system-wide KeySet state.
 */
public class KeySetManagerService {

    static final String TAG = "KeySetManagerService";

    /* original keysets implementation had no versioning info, so this is the first */
    public static final int FIRST_VERSION = 1;

    public static final int CURRENT_VERSION = FIRST_VERSION;

    /** Sentinel value returned when a {@code KeySet} is not found. */
    public static final long KEYSET_NOT_FOUND = -1;

    /** Sentinel value returned when public key is not found. */
    protected static final long PUBLIC_KEY_NOT_FOUND = -1;

    private final LongSparseArray<KeySetHandle> mKeySets;

    private final LongSparseArray<PublicKeyHandle> mPublicKeys;

    protected final LongSparseArray<ArraySet<Long>> mKeySetMapping;

    private final WatchedArrayMap<String, PackageSetting> mPackages;

    private long lastIssuedKeySetId = 0;

    private long lastIssuedKeyId = 0;

    class PublicKeyHandle {
        private final PublicKey mKey;
        private final long mId;
        private int mRefCount;

        public PublicKeyHandle(long id, PublicKey key) {
            mId = id;
            mRefCount = 1;
            mKey = key;
        }

        /*
         * Only used when reading state from packages.xml
         */
        private PublicKeyHandle(long id, int refCount, PublicKey key) {
            mId = id;
            mRefCount = refCount;
            mKey = key;
        }

        public long getId() {
            return mId;
        }

        public PublicKey getKey() {
            return mKey;
        }

        public int getRefCountLPr() {
            return mRefCount;
        }

        public void incrRefCountLPw() {
            mRefCount++;
            return;
        }

        public long decrRefCountLPw() {
            mRefCount--;
            return mRefCount;
        }
    }

    public KeySetManagerService(WatchedArrayMap<String, PackageSetting> packages) {
        mKeySets = new LongSparseArray<>();
        mPublicKeys = new LongSparseArray<>();
        mKeySetMapping = new LongSparseArray<>();
        mPackages = packages;
    }

    public KeySetManagerService(@NonNull KeySetManagerService other,
            @NonNull WatchedArrayMap<String, PackageSetting> packages) {
        mKeySets = other.mKeySets.clone();
        mPublicKeys = other.mPublicKeys.clone();
        mKeySetMapping = other.mKeySetMapping.clone();
        mPackages = packages;
    }

    /**
     * Determine if a package is signed by the given KeySet.
     *
     * Returns false if the package was not signed by all the
     * keys in the KeySet.
     *
     * Returns true if the package was signed by at least the
     * keys in the given KeySet.
     *
     * Note that this can return true for multiple KeySets.
     */
    public boolean packageIsSignedByLPr(String packageName, KeySetHandle ks) {
        PackageSetting pkg = mPackages.get(packageName);
        if (pkg == null) {
            throw new NullPointerException("Invalid package name");
        }
        if (pkg.getKeySetData() == null) {
            throw new NullPointerException("Package has no KeySet data");
        }
        long id = getIdByKeySetLPr(ks);
        if (id == KEYSET_NOT_FOUND) {
                return false;
        }
        ArraySet<Long> pkgKeys = mKeySetMapping.get(pkg.getKeySetData().getProperSigningKeySet());
        ArraySet<Long> testKeys = mKeySetMapping.get(id);
        return pkgKeys.containsAll(testKeys);
    }

    /**
     * Determine if a package is signed by the given KeySet.
     *
     * Returns false if the package was not signed by all the
     * keys in the KeySet, or if the package was signed by keys
     * not in the KeySet.
     *
     * Note that this can return only for one KeySet.
     */
    public boolean packageIsSignedByExactlyLPr(String packageName, KeySetHandle ks) {
        PackageSetting pkg = mPackages.get(packageName);
        if (pkg == null) {
            throw new NullPointerException("Invalid package name");
        }
        if (pkg.getKeySetData() == null
            || pkg.getKeySetData().getProperSigningKeySet()
            == PackageKeySetData.KEYSET_UNASSIGNED) {
            throw new NullPointerException("Package has no KeySet data");
         }
        long id = getIdByKeySetLPr(ks);
        if (id == KEYSET_NOT_FOUND) {
                return false;
        }
        ArraySet<Long> pkgKeys = mKeySetMapping.get(pkg.getKeySetData().getProperSigningKeySet());
        ArraySet<Long> testKeys = mKeySetMapping.get(id);
        return pkgKeys.equals(testKeys);
    }

    /**
     * addScannedPackageLPw directly modifies the package metadata in  pm.Settings
     * at a point of no-return.  We need to make sure that the scanned package does
     * not contain bad keyset meta-data that could generate an incorrect
     * PackageSetting. Verify that there is a signing keyset, there are no issues
     * with null objects, and the upgrade and defined keysets match.
     *
     * Returns true if the package can safely be added to the keyset metadata.
     */
    public void assertScannedPackageValid(AndroidPackage pkg)
            throws PackageManagerException {
        if (pkg == null || pkg.getPackageName() == null) {
            throw new PackageManagerException(INSTALL_FAILED_INVALID_APK,
                    "Passed invalid package to keyset validation.");
        }
        ArraySet<PublicKey> signingKeys = pkg.getSigningDetails().getPublicKeys();
        if (signingKeys == null || !(signingKeys.size() > 0) || signingKeys.contains(null)) {
            throw new PackageManagerException(INSTALL_FAILED_INVALID_APK,
                    "Package has invalid signing-key-set.");
        }
        Map<String, ArraySet<PublicKey>> definedMapping = pkg.getKeySetMapping();
        if (definedMapping != null) {
            if (definedMapping.containsKey(null) || definedMapping.containsValue(null)) {
                throw new PackageManagerException(INSTALL_FAILED_INVALID_APK,
                        "Package has null defined key set.");
            }
            for (ArraySet<PublicKey> value : definedMapping.values()) {
                if (!(value.size() > 0) || value.contains(null)) {
                    throw new PackageManagerException(INSTALL_FAILED_INVALID_APK,
                            "Package has null/no public keys for defined key-sets.");
                }
            }
        }
        Set<String> upgradeAliases = pkg.getUpgradeKeySets();
        if (upgradeAliases != null) {
            if (definedMapping == null || !(definedMapping.keySet().containsAll(upgradeAliases))) {
                throw new PackageManagerException(INSTALL_FAILED_INVALID_APK,
                        "Package has upgrade-key-sets without corresponding definitions.");
            }
        }
    }

    public void addScannedPackageLPw(AndroidPackage pkg) {
        Objects.requireNonNull(pkg, "Attempted to add null pkg to ksms.");
        Objects.requireNonNull(pkg.getPackageName(), "Attempted to add null pkg to ksms.");
        PackageSetting ps = mPackages.get(pkg.getPackageName());
        Objects.requireNonNull(ps, "pkg: " + pkg.getPackageName()
                    + "does not have a corresponding entry in mPackages.");
        addSigningKeySetToPackageLPw(ps, pkg.getSigningDetails().getPublicKeys());
        if (pkg.getKeySetMapping() != null) {
            addDefinedKeySetsToPackageLPw(ps, pkg.getKeySetMapping());
            if (pkg.getUpgradeKeySets() != null) {
                addUpgradeKeySetsToPackageLPw(ps, pkg.getUpgradeKeySets());
            }
        }
    }

    /**
     * Informs the system that the given package was signed by the provided KeySet.
     */
    void addSigningKeySetToPackageLPw(PackageSetting pkg,
            ArraySet<PublicKey> signingKeys) {

        /* check existing keyset for reuse or removal */
        long signingKeySetId = pkg.getKeySetData().getProperSigningKeySet();

        if (signingKeySetId != PackageKeySetData.KEYSET_UNASSIGNED) {
            ArraySet<PublicKey> existingKeys = getPublicKeysFromKeySetLPr(signingKeySetId);
            if (existingKeys != null && existingKeys.equals(signingKeys)) {

                /* no change in signing keys, leave PackageSetting alone */
                return;
            } else {

                /* old keyset no longer valid, remove ref */
                decrementKeySetLPw(signingKeySetId);
            }
        }

        /* create and add a new keyset */
        KeySetHandle ks = addKeySetLPw(signingKeys);
        long id = ks.getId();
        pkg.getKeySetData().setProperSigningKeySet(id);
        return;
    }

    /**
     * Fetches the stable identifier associated with the given KeySet. Returns
     * {@link #KEYSET_NOT_FOUND} if the KeySet... wasn't found.
     */
    private long getIdByKeySetLPr(KeySetHandle ks) {
        for (int keySetIndex = 0; keySetIndex < mKeySets.size(); keySetIndex++) {
            KeySetHandle value = mKeySets.valueAt(keySetIndex);
            if (ks.equals(value)) {
                return mKeySets.keyAt(keySetIndex);
            }
        }
        return KEYSET_NOT_FOUND;
    }

    /**
     * Inform the system that the given package defines the given KeySets.
     * Remove any KeySets the package no longer defines.
     */
    void addDefinedKeySetsToPackageLPw(PackageSetting pkg,
            Map<String, ArraySet<PublicKey>> definedMapping) {
        ArrayMap<String, Long> prevDefinedKeySets = pkg.getKeySetData().getAliases();

        /* add all of the newly defined KeySets */
        Map<String, Long> newKeySetAliases = new ArrayMap<>();
        for (Map.Entry<String, ArraySet<PublicKey>> entry : definedMapping.entrySet()) {
            String alias = entry.getKey();
            ArraySet<PublicKey> pubKeys = entry.getValue();
            if (alias != null && pubKeys != null && pubKeys.size() > 0) {
                KeySetHandle ks = addKeySetLPw(pubKeys);
                newKeySetAliases.put(alias, ks.getId());
            }
        }

        /* remove each of the old references */
        final int prevDefSize = prevDefinedKeySets.size();
        for (int i = 0; i < prevDefSize; i++) {
            decrementKeySetLPw(prevDefinedKeySets.valueAt(i));
        }
        pkg.getKeySetData().removeAllUpgradeKeySets();

        /* switch to the just-added */
        pkg.getKeySetData().setAliases(newKeySetAliases);
        return;
    }

    /**
     * This informs the system that the given package has defined a KeySet
     * alias in its manifest to be an upgradeKeySet.  This must be called
     * after all of the defined KeySets have been added.
     */
    void addUpgradeKeySetsToPackageLPw(PackageSetting pkg,
            Set<String> upgradeAliases) {
        for (String upgradeAlias : upgradeAliases) {
            pkg.getKeySetData().addUpgradeKeySet(upgradeAlias);
        }
    }

    /**
     * Fetched the {@link KeySetHandle} that a given package refers to by the
     * provided alias.  Returns null if the package is unknown or does not have a
     * KeySet corresponding to that alias.
     */
    public KeySetHandle getKeySetByAliasAndPackageNameLPr(String packageName, String alias) {
        PackageSetting p = mPackages.get(packageName);
        if (p == null || p.getKeySetData() == null) {
            return null;
        }
        final ArrayMap<String, Long> aliases = p.getKeySetData().getAliases();
        Long keySetId = aliases.get(alias);
        if (keySetId == null) {
            throw new IllegalArgumentException("Unknown KeySet alias: " + alias + ", aliases = " + aliases);
        }
        return mKeySets.get(keySetId);
    }

    /* Checks if an identifier refers to a known keyset */
    public boolean isIdValidKeySetId(long id) {
        return mKeySets.get(id) != null;
    }

    public boolean shouldCheckUpgradeKeySetLocked(PackageStateInternal oldPs,
            SharedUserApi sharedUserSetting, int scanFlags) {
        // Can't rotate keys during boot or if sharedUser.
        if (oldPs == null || (scanFlags & SCAN_INITIAL) != 0 || (sharedUserSetting != null)
                || !oldPs.getKeySetData().isUsingUpgradeKeySets()) {
            return false;
        }
        // app is using upgradeKeySets; make sure all are valid
        long[] upgradeKeySets = oldPs.getKeySetData().getUpgradeKeySets();
        for (int i = 0; i < upgradeKeySets.length; i++) {
            if (!isIdValidKeySetId(upgradeKeySets[i])) {
                Slog.wtf(TAG, "Package "
                         + (oldPs.getPackageName() != null ? oldPs.getPackageName() : "<null>")
                         + " contains upgrade-key-set reference to unknown key-set: "
                         + upgradeKeySets[i]
                         + " reverting to signatures check.");
                return false;
            }
        }
        return true;
    }

    public boolean checkUpgradeKeySetLocked(PackageStateInternal oldPS, AndroidPackage pkg) {
        // Upgrade keysets are being used.  Determine if new package has a superset of the
        // required keys.
        long[] upgradeKeySets = oldPS.getKeySetData().getUpgradeKeySets();
        for (int i = 0; i < upgradeKeySets.length; i++) {
            Set<PublicKey> upgradeSet = getPublicKeysFromKeySetLPr(upgradeKeySets[i]);
            if (upgradeSet != null
                    && pkg.getSigningDetails().getPublicKeys().containsAll(upgradeSet)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fetches the {@link PublicKey public keys} which belong to the specified
     * KeySet id.
     *
     * Returns {@code null} if the identifier doesn't
     * identify a {@link KeySetHandle}.
     */
    public ArraySet<PublicKey> getPublicKeysFromKeySetLPr(long id) {
        ArraySet<Long> pkIds = mKeySetMapping.get(id);
        if (pkIds == null) {
            return null;
        }
        ArraySet<PublicKey> mPubKeys = new ArraySet<PublicKey>();
        final int pkSize = pkIds.size();
        for (int i = 0; i < pkSize; i++) {
            mPubKeys.add(mPublicKeys.get(pkIds.valueAt(i)).getKey());
        }
        return mPubKeys;
    }

    /**
     * Fetches the proper {@link KeySetHandle KeySet} that signed the given
     * package.
     *
     * @throws IllegalArgumentException if the package has no keyset data.
     * @throws NullPointerException if the packgae is unknown.
     */
    public KeySetHandle  getSigningKeySetByPackageNameLPr(String packageName) {
        PackageSetting p = mPackages.get(packageName);
        if (p == null
            || p.getKeySetData() == null
            || p.getKeySetData().getProperSigningKeySet()
            == PackageKeySetData.KEYSET_UNASSIGNED) {
            return null;
        }
        return mKeySets.get(p.getKeySetData().getProperSigningKeySet());
    }

    /**
     * Creates a new KeySet corresponding to the given keys.
     *
     * If the {@link PublicKey PublicKeys} aren't known to the system, this
     * adds them. Otherwise, they're deduped and the reference count
     * incremented.
     *
     * If the KeySet isn't known to the system, this adds that and creates the
     * mapping to the PublicKeys. If it is known, then it's deduped and the
     * reference count is incremented.
     *
     * Throws if the provided set is {@code null}.
     */
    private KeySetHandle addKeySetLPw(ArraySet<PublicKey> keys) {
        if (keys == null || keys.size() == 0) {
            throw new IllegalArgumentException("Cannot add an empty set of keys!");
        }

        /* add each of the keys in the provided set */
        ArraySet<Long> addedKeyIds = new ArraySet<Long>(keys.size());
        final int kSize = keys.size();
        for (int i = 0; i < kSize; i++) {
            long id = addPublicKeyLPw(keys.valueAt(i));
            addedKeyIds.add(id);
        }

        /* check to see if the resulting keyset is new */
        long existingKeySetId = getIdFromKeyIdsLPr(addedKeyIds);
        if (existingKeySetId != KEYSET_NOT_FOUND) {

            /* public keys were incremented, but we aren't adding a new keyset: undo */
            for (int i = 0; i < kSize; i++) {
                decrementPublicKeyLPw(addedKeyIds.valueAt(i));
            }
            KeySetHandle ks = mKeySets.get(existingKeySetId);
            ks.incrRefCountLPw();
            return ks;
        }

        // get the next keyset id
        long id = getFreeKeySetIDLPw();

        // create the KeySet object and add to mKeySets and mapping
        KeySetHandle ks = new KeySetHandle(id);
        mKeySets.put(id, ks);
        mKeySetMapping.put(id, addedKeyIds);
        return ks;
    }

    /*
     * Decrements the reference to KeySet represented by the given id.  If this
     * drops to zero, then also decrement the reference to each public key it
     * contains and remove the KeySet.
     */
    private void decrementKeySetLPw(long id) {
        KeySetHandle ks = mKeySets.get(id);
        if (ks == null) {
            /* nothing to do */
            return;
        }
        if (ks.decrRefCountLPw() <= 0) {
            ArraySet<Long> pubKeys = mKeySetMapping.get(id);
            final int pkSize = pubKeys.size();
            for (int i = 0; i < pkSize; i++) {
                decrementPublicKeyLPw(pubKeys.valueAt(i));
            }
            mKeySets.delete(id);
            mKeySetMapping.delete(id);
        }
    }

    /*
     * Decrements the reference to PublicKey represented by the given id.  If
     * this drops to zero, then remove it.
     */
    private void decrementPublicKeyLPw(long id) {
        PublicKeyHandle pk = mPublicKeys.get(id);
        if (pk == null) {
            /* nothing to do */
            return;
        }
        if (pk.decrRefCountLPw() <= 0) {
            mPublicKeys.delete(id);
        }
    }

    /**
     * Adds the given PublicKey to the system, deduping as it goes.
     */
    private long addPublicKeyLPw(PublicKey key) {
        Objects.requireNonNull(key, "Cannot add null public key!");
        long id = getIdForPublicKeyLPr(key);
        if (id != PUBLIC_KEY_NOT_FOUND) {

            /* We already know about this key, increment its ref count and ret */
            mPublicKeys.get(id).incrRefCountLPw();
            return id;
        }

        /* if it's new find the first unoccupied slot in the public keys */
        id = getFreePublicKeyIdLPw();
        mPublicKeys.put(id, new PublicKeyHandle(id, key));
        return id;
    }

    /**
     * Finds the stable identifier for a KeySet based on a set of PublicKey stable IDs.
     *
     * Returns KEYSET_NOT_FOUND if there isn't one.
     */
    private long getIdFromKeyIdsLPr(Set<Long> publicKeyIds) {
        for (int keyMapIndex = 0; keyMapIndex < mKeySetMapping.size(); keyMapIndex++) {
            ArraySet<Long> value = mKeySetMapping.valueAt(keyMapIndex);
            if (value.equals(publicKeyIds)) {
                return mKeySetMapping.keyAt(keyMapIndex);
            }
        }
        return KEYSET_NOT_FOUND;
    }

    /**
     * Finds the stable identifier for a PublicKey or PUBLIC_KEY_NOT_FOUND.
     */
    private long getIdForPublicKeyLPr(PublicKey k) {
        String encodedPublicKey = new String(k.getEncoded());
        for (int publicKeyIndex = 0; publicKeyIndex < mPublicKeys.size(); publicKeyIndex++) {
            PublicKey value = mPublicKeys.valueAt(publicKeyIndex).getKey();
            String encodedExistingKey = new String(value.getEncoded());
            if (encodedPublicKey.equals(encodedExistingKey)) {
                return mPublicKeys.keyAt(publicKeyIndex);
            }
        }
        return PUBLIC_KEY_NOT_FOUND;
    }

    /**
     * Gets an unused stable identifier for a KeySet.
     */
    private long getFreeKeySetIDLPw() {
        lastIssuedKeySetId += 1;
        return lastIssuedKeySetId;
    }

    /**
     * Same as above, but for public keys.
     */
    private long getFreePublicKeyIdLPw() {
        lastIssuedKeyId += 1;
        return lastIssuedKeyId;
    }

    /*
     * This package is being removed from the system, so we need to
     * remove its keyset and public key references, then remove its
     * keyset data.
     */
    public void removeAppKeySetDataLPw(String packageName) {

        /* remove refs from common keysets and public keys */
        PackageSetting pkg = mPackages.get(packageName);
        Objects.requireNonNull(pkg, "pkg name: " + packageName
                + "does not have a corresponding entry in mPackages.");
        long signingKeySetId = pkg.getKeySetData().getProperSigningKeySet();
        decrementKeySetLPw(signingKeySetId);
        ArrayMap<String, Long> definedKeySets = pkg.getKeySetData().getAliases();
        for (int i = 0; i < definedKeySets.size(); i++) {
            decrementKeySetLPw(definedKeySets.valueAt(i));
        }

        /* remove from package */
        clearPackageKeySetDataLPw(pkg);
        return;
    }

    private void clearPackageKeySetDataLPw(PackageSetting pkg) {
        pkg.getKeySetData().setProperSigningKeySet(PackageKeySetData.KEYSET_UNASSIGNED);
        pkg.getKeySetData().removeAllDefinedKeySets();
        pkg.getKeySetData().removeAllUpgradeKeySets();
        return;
    }

    @Deprecated
    public String encodePublicKey(PublicKey k) throws IOException {
        return new String(Base64.encode(k.getEncoded(), Base64.NO_WRAP));
    }

    public void dumpLPr(PrintWriter pw, String packageName,
                        DumpState dumpState) {
        boolean printedHeader = false;
        for (ArrayMap.Entry<String, PackageSetting> e : mPackages.entrySet()) {
            String keySetPackage = e.getKey();
            if (packageName != null && !packageName.equals(keySetPackage)) {
                continue;
            }
            if (!printedHeader) {
                if (dumpState.onTitlePrinted())
                    pw.println();
                pw.println("Key Set Manager:");
                printedHeader = true;
            }
            PackageSetting pkg = e.getValue();
            pw.print("  ["); pw.print(keySetPackage); pw.println("]");
            if (pkg.getKeySetData() != null) {
                boolean printedLabel = false;
                for (ArrayMap.Entry<String, Long> entry :
                        pkg.getKeySetData().getAliases().entrySet()) {
                    if (!printedLabel) {
                        pw.print("      KeySets Aliases: ");
                        printedLabel = true;
                    } else {
                        pw.print(", ");
                    }
                    pw.print(entry.getKey());
                    pw.print('=');
                    pw.print(Long.toString(entry.getValue()));
                }
                if (printedLabel) {
                    pw.println("");
                }
                printedLabel = false;
                if (pkg.getKeySetData().isUsingDefinedKeySets()) {
                    ArrayMap<String, Long> definedKeySets = pkg.getKeySetData().getAliases();
                    final int dksSize = definedKeySets.size();
                    for (int i = 0; i < dksSize; i++) {
                        if (!printedLabel) {
                            pw.print("      Defined KeySets: ");
                            printedLabel = true;
                        } else {
                            pw.print(", ");
                        }
                        pw.print(Long.toString(definedKeySets.valueAt(i)));
                    }
                }
                if (printedLabel) {
                    pw.println("");
                }
                printedLabel = false;
                final long signingKeySet = pkg.getKeySetData().getProperSigningKeySet();
                pw.print("      Signing KeySets: ");
                pw.print(Long.toString(signingKeySet));
                pw.println("");
                if (pkg.getKeySetData().isUsingUpgradeKeySets()) {
                    for (long keySetId : pkg.getKeySetData().getUpgradeKeySets()) {
                        if (!printedLabel) {
                            pw.print("      Upgrade KeySets: ");
                            printedLabel = true;
                        } else {
                            pw.print(", ");
                        }
                        pw.print(Long.toString(keySetId));
                    }
                }
                if (printedLabel) {
                    pw.println("");
                }
            }
        }
    }

    void writeKeySetManagerServiceLPr(TypedXmlSerializer serializer) throws IOException {
        serializer.startTag(null, "keyset-settings");
        serializer.attributeInt(null, "version", CURRENT_VERSION);
        writePublicKeysLPr(serializer);
        writeKeySetsLPr(serializer);
        serializer.startTag(null, "lastIssuedKeyId");
        serializer.attributeLong(null, "value", lastIssuedKeyId);
        serializer.endTag(null, "lastIssuedKeyId");
        serializer.startTag(null, "lastIssuedKeySetId");
        serializer.attributeLong(null, "value", lastIssuedKeySetId);
        serializer.endTag(null, "lastIssuedKeySetId");
        serializer.endTag(null, "keyset-settings");
    }

    void writePublicKeysLPr(TypedXmlSerializer serializer) throws IOException {
        serializer.startTag(null, "keys");
        for (int pKeyIndex = 0; pKeyIndex < mPublicKeys.size(); pKeyIndex++) {
            long id = mPublicKeys.keyAt(pKeyIndex);
            PublicKeyHandle pkh = mPublicKeys.valueAt(pKeyIndex);
            serializer.startTag(null, "public-key");
            serializer.attributeLong(null, "identifier", id);
            serializer.attributeBytesBase64(null, "value", pkh.getKey().getEncoded());
            serializer.endTag(null, "public-key");
        }
        serializer.endTag(null, "keys");
    }

    void writeKeySetsLPr(TypedXmlSerializer serializer) throws IOException {
        serializer.startTag(null, "keysets");
        for (int keySetIndex = 0; keySetIndex < mKeySetMapping.size(); keySetIndex++) {
            long id = mKeySetMapping.keyAt(keySetIndex);
            ArraySet<Long> keys = mKeySetMapping.valueAt(keySetIndex);
            serializer.startTag(null, "keyset");
            serializer.attributeLong(null, "identifier", id);
            for (long keyId : keys) {
                serializer.startTag(null, "key-id");
                serializer.attributeLong(null, "identifier", keyId);
                serializer.endTag(null, "key-id");
            }
            serializer.endTag(null, "keyset");
        }
        serializer.endTag(null, "keysets");
    }

    void readKeySetsLPw(TypedXmlPullParser parser, ArrayMap<Long, Integer> keySetRefCounts)
            throws XmlPullParserException, IOException {
        int type;
        int outerDepth = parser.getDepth();
        String recordedVersionStr = parser.getAttributeValue(null, "version");
        if (recordedVersionStr == null) {
            // The keyset information comes from pre-versioned devices, and
            // is inaccurate, don't collect any of it.
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                continue;
            }
            // The KeySet information read previously from packages.xml is invalid.
            // Destroy it all.
            for (PackageSetting p : mPackages.values()) {
                clearPackageKeySetDataLPw(p);
            }
            return;
        }
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
               && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                continue;
            }
            final String tagName = parser.getName();
            if (tagName.equals("keys")) {
                readKeysLPw(parser);
            } else if (tagName.equals("keysets")) {
                readKeySetListLPw(parser);
            } else if (tagName.equals("lastIssuedKeyId")) {
                lastIssuedKeyId = parser.getAttributeLong(null, "value");
            } else if (tagName.equals("lastIssuedKeySetId")) {
                lastIssuedKeySetId = parser.getAttributeLong(null, "value");
            }
        }

        addRefCountsFromSavedPackagesLPw(keySetRefCounts);
    }

    void readKeysLPw(TypedXmlPullParser parser)
            throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                continue;
            }
            final String tagName = parser.getName();
            if (tagName.equals("public-key")) {
                readPublicKeyLPw(parser);
            }
        }
    }

    void readKeySetListLPw(TypedXmlPullParser parser)
            throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        int type;
        long currentKeySetId = 0;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                continue;
            }
            final String tagName = parser.getName();
            if (tagName.equals("keyset")) {
                currentKeySetId = parser.getAttributeLong(null, "identifier");
                int refCount = 0;
                mKeySets.put(currentKeySetId, new KeySetHandle(currentKeySetId, refCount));
                mKeySetMapping.put(currentKeySetId, new ArraySet<Long>());
            } else if (tagName.equals("key-id")) {
                long id = parser.getAttributeLong(null, "identifier");
                mKeySetMapping.get(currentKeySetId).add(id);
            }
        }
    }

    void readPublicKeyLPw(TypedXmlPullParser parser)
            throws XmlPullParserException {
        long identifier = parser.getAttributeLong(null, "identifier");
        int refCount = 0;
        byte[] publicKey = parser.getAttributeBytesBase64(null, "value", null);
        PublicKey pub = FrameworkParsingPackageUtils.parsePublicKey(publicKey);
        if (pub != null) {
            PublicKeyHandle pkh = new PublicKeyHandle(identifier, refCount, pub);
            mPublicKeys.put(identifier, pkh);
        }
    }

    /*
     * Set each KeySet ref count.  Also increment all public keys in each keyset.
     */
    private void addRefCountsFromSavedPackagesLPw(ArrayMap<Long, Integer> keySetRefCounts) {
        final int numRefCounts = keySetRefCounts.size();
        for (int i = 0; i < numRefCounts; i++) {
            KeySetHandle ks = mKeySets.get(keySetRefCounts.keyAt(i));
            if (ks == null) {
                /* something went terribly wrong and we have references to a non-existent key-set */
                Slog.wtf(TAG, "Encountered non-existent key-set reference when reading settings");
                continue;
            }
            ks.setRefCountLPw(keySetRefCounts.valueAt(i));
        }

        /*
         * In case something went terribly wrong and we have keysets with no associated packges
         * that refer to them, record the orphaned keyset ids, and remove them using
         * decrementKeySetLPw() after all keyset references have been set so that the associtaed
         * public keys have the appropriate references from all keysets.
         */
        ArraySet<Long> orphanedKeySets = new ArraySet<Long>();
        final int numKeySets = mKeySets.size();
        for (int i = 0; i < numKeySets; i++) {
            if (mKeySets.valueAt(i).getRefCountLPr() == 0) {
                Slog.wtf(TAG, "Encountered key-set w/out package references when reading settings");
                orphanedKeySets.add(mKeySets.keyAt(i));
            }
            ArraySet<Long> pubKeys = mKeySetMapping.valueAt(i);
            final int pkSize = pubKeys.size();
            for (int j = 0; j < pkSize; j++) {
                mPublicKeys.get(pubKeys.valueAt(j)).incrRefCountLPw();
            }
        }
        final int numOrphans = orphanedKeySets.size();
        for (int i = 0; i < numOrphans; i++) {
            decrementKeySetLPw(orphanedKeySets.valueAt(i));
        }
    }
}
