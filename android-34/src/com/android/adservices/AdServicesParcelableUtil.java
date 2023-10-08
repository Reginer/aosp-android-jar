/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.adservices;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * A utility extension to the {@link Parcelable} class for AdServices.
 *
 * @hide
 */
public final class AdServicesParcelableUtil {
    /**
     * Writes a nullable {@link Parcelable} object to a target {@link Parcel}.
     *
     * <p>An extra boolean is written to the {@code targetParcel} (see {@link
     * #readNullableFromParcel(Parcel, ParcelReader)}) for the {@code nullableField}, where {@code
     * true} is written if the field is not {@code null}.
     */
    public static <T> void writeNullableToParcel(
            @NonNull Parcel targetParcel,
            @Nullable T nullableField,
            @NonNull ParcelWriter<T> parcelWriter) {
        Objects.requireNonNull(targetParcel);
        Objects.requireNonNull(parcelWriter);

        boolean isFieldPresent = (nullableField != null);
        targetParcel.writeBoolean(isFieldPresent);
        if (isFieldPresent) {
            parcelWriter.write(targetParcel, nullableField);
        }
    }

    /**
     * Reads and returns a nullable object from a source {@link Parcel}.
     *
     * <p>This method expects a boolean (see {@link #writeNullableToParcel(Parcel, Object,
     * ParcelWriter)}) that will be {@code true} if the nullable field is not {@code null} and reads
     * and returns it using the given {@link Callable}.
     */
    public static <T> T readNullableFromParcel(
            @NonNull Parcel sourceParcel, @NonNull ParcelReader<T> parcelReader) {
        Objects.requireNonNull(sourceParcel);
        Objects.requireNonNull(parcelReader);

        if (sourceParcel.readBoolean()) {
            return parcelReader.read(sourceParcel);
        } else {
            return null;
        }
    }

    /**
     * Writes a {@link Map} of {@link Parcelable} keys and values to a target {@link Parcel}.
     *
     * <p>All keys of the {@code sourceMap} must be convertible to and from {@link String} objects.
     *
     * <p>Use to write a {@link Map} which will be later unparceled by {@link
     * #readMapFromParcel(Parcel, StringToObjectConverter, Class)}.
     */
    public static <K, V extends Parcelable> void writeMapToParcel(
            @NonNull Parcel targetParcel, @NonNull Map<K, V> sourceMap) {
        Objects.requireNonNull(targetParcel);
        Objects.requireNonNull(sourceMap);

        Bundle tempBundle = new Bundle();
        for (Map.Entry<K, V> entry : sourceMap.entrySet()) {
            tempBundle.putParcelable(entry.getKey().toString(), entry.getValue());
        }

        targetParcel.writeBundle(tempBundle);
    }

    /**
     * Reads and returns a {@link Map} of {@link Parcelable} objects from a source {@link Parcel}.
     *
     * <p>Use to read a {@link Map} written with {@link #writeMapToParcel(Parcel, Map)}.
     */
    public static <K, V extends Parcelable> Map<K, V> readMapFromParcel(
            @NonNull Parcel sourceParcel,
            @NonNull StringToObjectConverter<K> stringToKeyConverter,
            @NonNull Class<V> valueClass) {
        Objects.requireNonNull(sourceParcel);
        Objects.requireNonNull(stringToKeyConverter);
        Objects.requireNonNull(valueClass);

        Bundle tempBundle = Bundle.CREATOR.createFromParcel(sourceParcel);
        tempBundle.setClassLoader(valueClass.getClassLoader());
        Map<K, V> resultMap = new HashMap<>();
        for (String key : tempBundle.keySet()) {
            V value =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                            ? tempBundle.getParcelable(key)
                            : tempBundle.getParcelable(key, valueClass);
            resultMap.put(stringToKeyConverter.convertFromString(key), value);
        }

        return resultMap;
    }

    /**
     * Writes a {@link Set} of {@link Parcelable} objects to a target {@link Parcel} as an array.
     *
     * <p>Use to write a {@link Set} which will be unparceled by {@link #readSetFromParcel(Parcel,
     * Parcelable.Creator)} later.
     */
    public static <T extends Parcelable> void writeSetToParcel(
            @NonNull Parcel targetParcel, @NonNull Set<T> sourceSet) {
        Objects.requireNonNull(targetParcel);
        Objects.requireNonNull(sourceSet);

        ArrayList<T> tempList = new ArrayList<>(sourceSet);
        targetParcel.writeTypedList(tempList);
    }

    /**
     * Reads and returns a {@link Set} of {@link Parcelable} objects from a source {@link Parcel}.
     *
     * <p>Use to read a {@link Set} that was written by {@link #writeSetToParcel(Parcel, Set)}.
     */
    public static <T extends Parcelable> Set<T> readSetFromParcel(
            @NonNull Parcel sourceParcel, @NonNull Parcelable.Creator<T> creator) {
        Objects.requireNonNull(sourceParcel);
        Objects.requireNonNull(creator);

        return new HashSet<>(Objects.requireNonNull(sourceParcel.createTypedArrayList(creator)));
    }

    /**
     * Writes a {@link Set} of {@link String} objects to a target {@link Parcel} as a list.
     *
     * <p>Use to write a {@link Set} which will be unparceled by {@link
     * #readStringSetFromParcel(Parcel)} later.
     */
    public static void writeStringSetToParcel(
            @NonNull Parcel targetParcel, @NonNull Set<String> sourceSet) {
        Objects.requireNonNull(targetParcel);
        Objects.requireNonNull(sourceSet);

        ArrayList<String> tempList = new ArrayList<>(sourceSet);
        targetParcel.writeStringList(tempList);
    }

    /**
     * Reads and returns a {@link Set} of {@link String} objects from a source {@link Parcel}.
     *
     * <p>Use to read a {@link Set} that was written by {@link #writeStringSetToParcel(Parcel,
     * Set)}.
     */
    public static Set<String> readStringSetFromParcel(@NonNull Parcel sourceParcel) {
        Objects.requireNonNull(sourceParcel);

        return new HashSet<>(Objects.requireNonNull(sourceParcel.createStringArrayList()));
    }

    /**
     * Writes a {@link List} of {@link Instant} objects to a target {@link Parcel} as an array.
     *
     * <p>If an error is encountered while writing any element of the input {@code sourceList}, the
     * element will be skipped.
     *
     * <p>Use to write a {@link List} which will be unparceled by {@link
     * #readInstantListFromParcel(Parcel)} later.
     */
    public static void writeInstantListToParcel(
            @NonNull Parcel targetParcel, @NonNull List<Instant> sourceList) {
        Objects.requireNonNull(targetParcel);
        Objects.requireNonNull(sourceList);

        long[] tempArray = new long[sourceList.size()];
        int actualArraySize = 0;

        for (Instant instant : sourceList) {
            long instantAsEpochMilli;
            try {
                instantAsEpochMilli = instant.toEpochMilli();
            } catch (Exception exception) {
                LogUtil.w(
                        exception,
                        "Error encountered while parceling Instant %s to long; skipping element",
                        instant);
                continue;
            }
            tempArray[actualArraySize++] = instantAsEpochMilli;
        }

        // Writing the tempArray as is may write undefined values, so compress into a smaller
        // accurately-fit array
        long[] writeArray = new long[actualArraySize];
        System.arraycopy(tempArray, 0, writeArray, 0, actualArraySize);

        targetParcel.writeInt(actualArraySize);
        targetParcel.writeLongArray(writeArray);
    }

    /**
     * Reads and returns a {@link List} of {@link Instant} objects from a source {@link Parcel}.
     *
     * <p>If an error is encountered while reading an element from the {@code sourceParcel}, the
     * element will be skipped.
     *
     * <p>Use to read a {@link List} that was written by {@link #writeInstantListToParcel(Parcel,
     * List)}.
     */
    public static List<Instant> readInstantListFromParcel(@NonNull Parcel sourceParcel) {
        Objects.requireNonNull(sourceParcel);

        final int listSize = sourceParcel.readInt();
        long[] tempArray = new long[listSize];
        ArrayList<Instant> targetList = new ArrayList<>(listSize);

        sourceParcel.readLongArray(tempArray);
        for (int ii = 0; ii < listSize; ii++) {
            Instant instantFromMilli;
            try {
                instantFromMilli = Instant.ofEpochMilli(tempArray[ii]);
            } catch (Exception exception) {
                LogUtil.w(
                        exception,
                        "Error encountered while unparceling Instant from long %d; skipping"
                                + " element",
                        tempArray[ii]);
                continue;
            }
            targetList.add(instantFromMilli);
        }

        return targetList;
    }

    /**
     * A functional interface for writing a source object to a {@link Parcel}.
     *
     * @param <T> the type of the source object to be written
     * @hide
     */
    @FunctionalInterface
    public interface ParcelWriter<T> {
        /** Writes a {@code sourceObject} to the {@code targetParcel}. */
        void write(@NonNull Parcel targetParcel, @NonNull T sourceObject);
    }

    /**
     * A functional interface for reading an object from a {@link Parcel}.
     *
     * @param <T> the type of the object to be read from the source parcel
     * @hide
     */
    @FunctionalInterface
    public interface ParcelReader<T> {
        /** Reads and returns an object from the {@code sourceParcel}. */
        T read(@NonNull Parcel sourceParcel);
    }

    /**
     * A functional interface for converting a {@link String} to an object of type {@link T}.
     *
     * @param <T> the type of the object which will be converted from a {@link String}
     * @hide
     */
    @FunctionalInterface
    public interface StringToObjectConverter<T> {
        /** Converts the {@code sourceString} to an object of the specified type. */
        T convertFromString(@NonNull String sourceString);
    }
}
