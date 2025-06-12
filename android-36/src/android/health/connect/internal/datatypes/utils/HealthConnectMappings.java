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

package android.health.connect.internal.datatypes.utils;

import static android.health.connect.Constants.DEFAULT_INT;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.HealthDataCategory;
import android.health.connect.HealthPermissionCategory;
import android.health.connect.HealthPermissions;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.RecordInternal;
import android.util.ArrayMap;
import android.util.ArraySet;

import com.android.healthfitness.flags.Flags;
import com.android.internal.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** @hide */
public final class HealthConnectMappings {
    private final Map<Integer, DataTypeDescriptor> mRecordIdToDescriptorMap;
    private final Map<Integer, String> mPermissionCategoryToReadPermissionMap;
    private final Map<Integer, String> mPermissionCategoryToWritePermissionMap;
    private final Map<String, Integer> mWritePermissionToDataCategoryMap;
    private final Map<Integer, String[]> mDataCategoryToWritePermissionsMap;
    private final Map<Integer, Class<? extends RecordInternal<?>>>
            mRecordIdToInternalRecordClassMap;
    private final Map<Integer, Class<? extends Record>> mRecordIdToRecordClassMap;
    private final Map<Class<? extends Record>, Integer> mRecordClassToRecordIdMap;
    private final Set<Integer> mHealthDataCategories;

    private final RecordMapper mRecordMapper = RecordMapper.getInstance();

    @SuppressWarnings("NullAway.Init") // TODO(b/317029272): fix this suppression
    private static volatile HealthConnectMappings sHealthConnectMappings;

    /** Exists for compatibility with classes which don't support injections yet. */
    // TODO(b/353283052): inject where possible instead of using the singleton.
    public static HealthConnectMappings getInstance() {
        if (sHealthConnectMappings == null) {
            sHealthConnectMappings = new HealthConnectMappings();
        }
        return sHealthConnectMappings;
    }

    /**
     * Resets the singleton instance.
     *
     * <p>Useful for unit tests where flag values might change between test cases.
     */
    @VisibleForTesting
    public static void resetInstanceForTesting() {
        sHealthConnectMappings = new HealthConnectMappings();
    }

    /**
     * Use {@link #getInstance()} to avoid creating multiple instances until it gets migrated off.
     */
    @VisibleForTesting
    public HealthConnectMappings() {
        var dataTypeDescriptors = DataTypeDescriptors.getAllDataTypeDescriptors();

        mRecordIdToDescriptorMap =
                Flags.healthConnectMappings()
                        ? toArrayMap(
                                dataTypeDescriptors,
                                DataTypeDescriptor::getRecordTypeIdentifier,
                                Function.identity())
                        : new ArrayMap<>();

        mPermissionCategoryToReadPermissionMap =
                Flags.healthConnectMappings()
                        ? toArrayMap(
                                dataTypeDescriptors,
                                DataTypeDescriptor::getPermissionCategory,
                                DataTypeDescriptor::getReadPermission)
                        : new ArrayMap<>();

        mPermissionCategoryToWritePermissionMap =
                Flags.healthConnectMappings()
                        ? toArrayMap(
                                dataTypeDescriptors,
                                DataTypeDescriptor::getPermissionCategory,
                                DataTypeDescriptor::getWritePermission)
                        : new ArrayMap<>();

        mWritePermissionToDataCategoryMap =
                Flags.healthConnectMappings()
                        ? toArrayMap(
                                dataTypeDescriptors,
                                DataTypeDescriptor::getWritePermission,
                                DataTypeDescriptor::getDataCategory)
                        : new ArrayMap<>();

        mDataCategoryToWritePermissionsMap =
                Flags.healthConnectMappings()
                        ? getDataCategoryToWritePermissionsMap(dataTypeDescriptors)
                        : new ArrayMap<>();

        mRecordIdToInternalRecordClassMap =
                Flags.healthConnectMappings()
                        ? toArrayMap(
                                dataTypeDescriptors,
                                DataTypeDescriptor::getRecordTypeIdentifier,
                                DataTypeDescriptor::getRecordInternalClass)
                        : new ArrayMap<>();

        mRecordIdToRecordClassMap =
                Flags.healthConnectMappings()
                        ? toArrayMap(
                                dataTypeDescriptors,
                                DataTypeDescriptor::getRecordTypeIdentifier,
                                DataTypeDescriptor::getRecordClass)
                        : new ArrayMap<>();

        mRecordClassToRecordIdMap =
                Flags.healthConnectMappings()
                        ? toArrayMap(
                                dataTypeDescriptors,
                                DataTypeDescriptor::getRecordClass,
                                DataTypeDescriptor::getRecordTypeIdentifier)
                        : new ArrayMap<>();

        mHealthDataCategories =
                Flags.healthConnectMappings()
                        ? toArraySet(dataTypeDescriptors, DataTypeDescriptor::getDataCategory)
                        : new ArraySet<>();
    }

    /**
     * Returns a set of all supported record type identifiers.
     *
     * @see RecordTypeIdentifier
     */
    public Set<Integer> getAllRecordTypeIdentifiers() {
        if (!Flags.healthConnectMappings()) {
            return RecordTypeIdentifier.VALID_TYPES;
        }

        return mRecordIdToDescriptorMap.keySet();
    }

    /**
     * @return true if {@code permissionName} is a write-permission
     * @hide
     */
    public boolean isWritePermission(@NonNull String permissionName) {
        if (!Flags.healthConnectMappings()) {
            return HealthPermissions.isWritePermission(permissionName);
        }

        return mWritePermissionToDataCategoryMap.containsKey(permissionName);
    }

    /** @hide */
    public String getHealthReadPermission(@HealthPermissionCategory.Type int permissionCategory) {
        if (!Flags.healthConnectMappings()) {
            return HealthPermissions.getHealthReadPermission(permissionCategory);
        }

        return Objects.requireNonNull(
                mPermissionCategoryToReadPermissionMap.get(permissionCategory),
                "Read permission not found for permission category:" + permissionCategory);
    }

    /** @hide */
    public String getHealthWritePermission(@HealthPermissionCategory.Type int permissionCategory) {
        if (!Flags.healthConnectMappings()) {
            return HealthPermissions.getHealthWritePermission(permissionCategory);
        }

        return Objects.requireNonNull(
                mPermissionCategoryToWritePermissionMap.get(permissionCategory),
                "Write permission not found for permission category:" + permissionCategory);
    }

    /**
     * @return {@link HealthDataCategory} for a WRITE {@code permissionName}. -1 if permission
     *     category for {@code permissionName} is not found (or if {@code permissionName} is READ)
     * @hide
     */
    @HealthDataCategory.Type
    public int getHealthDataCategoryForWritePermission(@Nullable String permissionName) {
        if (!Flags.healthConnectMappings()) {
            return HealthPermissions.getHealthDataCategoryForWritePermission(permissionName);
        }

        return mWritePermissionToDataCategoryMap.getOrDefault(permissionName, DEFAULT_INT);
    }

    /**
     * @return an array of write permission for given data category.
     * @hide
     */
    public String[] getWriteHealthPermissionsFor(@HealthDataCategory.Type int dataCategory) {
        if (!Flags.healthConnectMappings()) {
            return HealthPermissions.getWriteHealthPermissionsFor(dataCategory);
        }

        return mDataCategoryToWritePermissionsMap.getOrDefault(dataCategory, new String[] {});
    }

    /**
     * Returns a mapping from {@link RecordTypeIdentifier} to corresponding {@link RecordInternal}.
     */
    public Map<Integer, Class<? extends RecordInternal<?>>> getRecordIdToInternalRecordClassMap() {
        if (!Flags.healthConnectMappings()) {
            return mRecordMapper.getRecordIdToInternalRecordClassMap();
        }

        return mRecordIdToInternalRecordClassMap;
    }

    /** Returns a mapping from {@link RecordTypeIdentifier} to corresponding {@link Record}. */
    public Map<Integer, Class<? extends Record>> getRecordIdToExternalRecordClassMap() {
        if (!Flags.healthConnectMappings()) {
            return mRecordMapper.getRecordIdToExternalRecordClassMap();
        }

        return mRecordIdToRecordClassMap;
    }

    /** Returns record type id for give record class. */
    @RecordTypeIdentifier.RecordType
    public int getRecordType(Class<? extends Record> recordClass) {
        if (!Flags.healthConnectMappings()) {
            return mRecordMapper.getRecordType(recordClass);
        }

        return Objects.requireNonNull(mRecordClassToRecordIdMap.get(recordClass));
    }

    /** Checks whether the given {@code recordClass} can be mapped. */
    public boolean hasRecordType(Class<? extends Record> recordClass) {
        if (!Flags.healthConnectMappings()) {
            return mRecordMapper.hasRecordType(recordClass);
        }

        return mRecordClassToRecordIdMap.containsKey(recordClass);
    }

    /** Returns {@link HealthDataCategory} for the input {@link RecordTypeIdentifier.RecordType}. */
    @HealthPermissionCategory.Type
    public int getHealthPermissionCategoryForRecordType(
            @RecordTypeIdentifier.RecordType int recordType) {
        if (!Flags.healthConnectMappings()) {
            return RecordTypePermissionCategoryMapper.getHealthPermissionCategoryForRecordType(
                    recordType);
        }

        return Objects.requireNonNull(
                        mRecordIdToDescriptorMap.get(recordType),
                        "Unsupported record type: " + recordType)
                .getPermissionCategory();
    }

    /** Returns {@link HealthDataCategory} for the input {@link RecordTypeIdentifier.RecordType}. */
    @HealthDataCategory.Type
    public int getRecordCategoryForRecordType(@RecordTypeIdentifier.RecordType int recordType) {
        if (!Flags.healthConnectMappings()) {
            return RecordTypeRecordCategoryMapper.getRecordCategoryForRecordType(recordType);
        }

        return Objects.requireNonNull(
                        mRecordIdToDescriptorMap.get(recordType),
                        "Unsupported record type: " + recordType)
                .getDataCategory();
    }

    /** Returns a set of all supported data categories. */
    public Set<Integer> getAllHealthDataCategories() {
        if (!Flags.healthConnectMappings()) {
            return Set.of(
                    HealthDataCategory.ACTIVITY,
                    HealthDataCategory.BODY_MEASUREMENTS,
                    HealthDataCategory.CYCLE_TRACKING,
                    HealthDataCategory.NUTRITION,
                    HealthDataCategory.SLEEP,
                    HealthDataCategory.VITALS,
                    HealthDataCategory.WELLNESS);
        }

        return mHealthDataCategories;
    }

    private static ArrayMap<Integer, String[]> getDataCategoryToWritePermissionsMap(
            List<DataTypeDescriptor> descriptors) {
        Map<Integer, Set<String>> map =
                descriptors.stream()
                        .collect(
                                groupingBy(
                                        DataTypeDescriptor::getDataCategory,
                                        Collectors.mapping(
                                                DataTypeDescriptor::getWritePermission, toSet())));

        ArrayMap<Integer, String[]> result = new ArrayMap<>();
        map.forEach((k, v) -> result.put(k, v.toArray(new String[0])));
        return result;
    }

    private static <T, K, V> ArrayMap<K, V> toArrayMap(
            Collection<T> collection, Function<T, K> keyFunc, Function<T, V> valueFunc) {
        ArrayMap<K, V> map = new ArrayMap<>(collection.size());

        for (var item : collection) {
            K key = keyFunc.apply(item);
            V value = valueFunc.apply(item);
            map.put(key, value);
        }

        return map;
    }

    private static <T, R> ArraySet<R> toArraySet(Collection<T> collection, Function<T, R> mapFunc) {
        ArraySet<R> set = new ArraySet<>(collection.size());

        for (var item : collection) {
            set.add(mapFunc.apply(item));
        }

        return set;
    }
}
