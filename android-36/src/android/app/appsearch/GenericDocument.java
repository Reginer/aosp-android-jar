/*
 * Copyright 2020 The Android Open Source Project
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

package android.app.appsearch;

import android.annotation.CurrentTimeMillisLong;
import android.annotation.FlaggedApi;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.app.appsearch.annotation.CanIgnoreReturnValue;
import android.app.appsearch.safeparcel.GenericDocumentParcel;
import android.app.appsearch.safeparcel.PropertyParcel;
import android.app.appsearch.util.IndentingStringBuilder;
import android.os.Build;
import android.os.Parcel;
import android.util.Log;

import com.android.appsearch.flags.Flags;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a document unit.
 *
 * <p>Documents contain structured data conforming to their {@link AppSearchSchema} type. Each
 * document is uniquely identified by a namespace and a String ID within that namespace.
 *
 * <p>Documents are constructed by using the {@link GenericDocument.Builder}.
 *
 * @see AppSearchSession#put
 * @see AppSearchSession#getByDocumentId
 * @see AppSearchSession#search
 */
// TODO(b/384721898): Switch to JSpecify annotations
@SuppressWarnings("JSpecifyNullness")
public class GenericDocument {
    private static final String TAG = "AppSearchGenericDocumen";

    /** The maximum number of indexed properties a document can have. */
    private static final int MAX_INDEXED_PROPERTIES = 16;

    /**
     * Fixed constant synthetic property for parent types.
     *
     * @hide
     */
    public static final String PARENT_TYPES_SYNTHETIC_PROPERTY = "$$__AppSearch__parentTypes";

    /**
     * An immutable empty {@link GenericDocument}.
     *
     * @hide
     */
    public static final GenericDocument EMPTY = new GenericDocument.Builder<>("", "", "").build();

    /**
     * The maximum number of indexed properties a document can have.
     *
     * <p>Indexed properties are properties which are strings where the {@link
     * AppSearchSchema.StringPropertyConfig#getIndexingType} value is anything other than {@link
     * AppSearchSchema.StringPropertyConfig#INDEXING_TYPE_NONE}, as well as long properties where
     * the {@link AppSearchSchema.LongPropertyConfig#getIndexingType} value is {@link
     * AppSearchSchema.LongPropertyConfig#INDEXING_TYPE_RANGE}.
     */
    public static int getMaxIndexedProperties() {
        return MAX_INDEXED_PROPERTIES;
    }

    /** The class to hold all meta data and properties for this {@link GenericDocument}. */
    private final GenericDocumentParcel mDocumentParcel;

    /**
     * Rebuilds a {@link GenericDocument} from a {@link GenericDocumentParcel}.
     *
     * @param documentParcel Packaged {@link GenericDocument} data, such as the result of {@link
     *     #getDocumentParcel()}.
     * @hide
     */
    @SuppressWarnings("deprecation")
    public GenericDocument(@NonNull GenericDocumentParcel documentParcel) {
        mDocumentParcel = Objects.requireNonNull(documentParcel);
    }

    /**
     * Creates a new {@link GenericDocument} from an existing instance.
     *
     * <p>This method should be only used by constructor of a subclass.
     */
    protected GenericDocument(@NonNull GenericDocument document) {
        this(document.mDocumentParcel);
    }

    /**
     * Writes the {@link GenericDocument} to the given {@link Parcel}.
     *
     * @param dest The {@link Parcel} to write to.
     * @param flags The flags to use for parceling.
     * @hide
     */
    // GenericDocument is an open class that can be extended, whereas parcelable classes must be
    // final in those methods. Thus, we make this a system api to avoid 3p apps depending on it
    // and getting confused by the inheritability.
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    @FlaggedApi(Flags.FLAG_ENABLE_GENERIC_DOCUMENT_OVER_IPC)
    public final void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);
        dest.writeParcelable(mDocumentParcel, flags);
    }

    /**
     * Creates a {@link GenericDocument} from a {@link Parcel}.
     *
     * @param parcel The {@link Parcel} to read from.
     * @hide
     */
    // GenericDocument is an open class that can be extended, whereas parcelable classes must be
    // final in those methods. Thus, we make this a system api to avoid 3p apps depending on it
    // and getting confused by the inheritability.
    @SuppressWarnings("deprecation")
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    @FlaggedApi(Flags.FLAG_ENABLE_GENERIC_DOCUMENT_OVER_IPC)
    public static @NonNull GenericDocument createFromParcel(@NonNull Parcel parcel) {
        Objects.requireNonNull(parcel);
        GenericDocumentParcel documentParcel;

        // Code built in Framework cannot depend on Androidx libraries. Therefore, we must call
        // Parcel#readParcelable directly.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            documentParcel =
                    parcel.readParcelable(
                            GenericDocumentParcel.class.getClassLoader(),
                            GenericDocumentParcel.class);
        } else {
            // The Parcel#readParcelable(ClassLoader, Class) function has a known issue on Android
            // T. This was fixed on Android U. When on Android T, call the older version of
            // Parcel#readParcelable.
            documentParcel = parcel.readParcelable(GenericDocumentParcel.class.getClassLoader());
        }
        return new GenericDocument(documentParcel);
    }

    /**
     * Returns the {@link GenericDocumentParcel} holding the values for this {@link
     * GenericDocument}.
     *
     * @hide
     */
    public @NonNull GenericDocumentParcel getDocumentParcel() {
        return mDocumentParcel;
    }

    /** Returns the unique identifier of the {@link GenericDocument}. */
    public @NonNull String getId() {
        return mDocumentParcel.getId();
    }

    /** Returns the namespace of the {@link GenericDocument}. */
    public @NonNull String getNamespace() {
        return mDocumentParcel.getNamespace();
    }

    /** Returns the {@link AppSearchSchema} type of the {@link GenericDocument}. */
    public @NonNull String getSchemaType() {
        return mDocumentParcel.getSchemaType();
    }

    /**
     * Returns the list of parent types of the {@link GenericDocument}'s type.
     *
     * <p>It is guaranteed that child types appear before parent types in the list.
     *
     * @deprecated Parent types should no longer be set in {@link GenericDocument}. Use {@link
     *     SearchResult.Builder#getParentTypeMap()} instead.
     * @hide
     */
    @Deprecated
    public @Nullable List<String> getParentTypes() {
        List<String> result = mDocumentParcel.getParentTypes();
        if (result == null) {
            return null;
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns the creation timestamp of the {@link GenericDocument}, in milliseconds.
     *
     * <p>The value is in the {@link System#currentTimeMillis} time base.
     */
    @CurrentTimeMillisLong
    public long getCreationTimestampMillis() {
        return mDocumentParcel.getCreationTimestampMillis();
    }

    /**
     * Returns the TTL (time-to-live) of the {@link GenericDocument}, in milliseconds.
     *
     * <p>The TTL is measured against {@link #getCreationTimestampMillis}. At the timestamp of
     * {@code creationTimestampMillis + ttlMillis}, measured in the {@link System#currentTimeMillis}
     * time base, the document will be auto-deleted.
     *
     * <p>The default value is 0, which means the document is permanent and won't be auto-deleted
     * until the app is uninstalled or {@link AppSearchSession#remove} is called.
     */
    public long getTtlMillis() {
        return mDocumentParcel.getTtlMillis();
    }

    /**
     * Returns the score of the {@link GenericDocument}.
     *
     * <p>The score is a query-independent measure of the document's quality, relative to other
     * {@link GenericDocument} objects of the same {@link AppSearchSchema} type.
     *
     * <p>Results may be sorted by score using {@link SearchSpec.Builder#setRankingStrategy}.
     * Documents with higher scores are considered better than documents with lower scores.
     *
     * <p>Any non-negative integer can be used a score.
     */
    public int getScore() {
        return mDocumentParcel.getScore();
    }

    /** Returns the names of all properties defined in this document. */
    public @NonNull Set<String> getPropertyNames() {
        return Collections.unmodifiableSet(mDocumentParcel.getPropertyNames());
    }

    /**
     * Retrieves the property value with the given path as {@link Object}.
     *
     * <p>A path can be a simple property name, such as those returned by {@link #getPropertyNames}.
     * It may also be a dot-delimited path through the nested document hierarchy, with nested {@link
     * GenericDocument} properties accessed via {@code '.'} and repeated properties optionally
     * indexed into via {@code [n]}.
     *
     * <p>For example, given the following {@link GenericDocument}:
     *
     * <pre>
     *     (Message) {
     *         from: "sender@example.com"
     *         to: [{
     *             name: "Albert Einstein"
     *             email: "einstein@example.com"
     *           }, {
     *             name: "Marie Curie"
     *             email: "curie@example.com"
     *           }]
     *         tags: ["important", "inbox"]
     *         subject: "Hello"
     *     }
     * </pre>
     *
     * <p>Here are some example paths and their results:
     *
     * <ul>
     *   <li>{@code "from"} returns {@code "sender@example.com"} as a {@link String} array with one
     *       element
     *   <li>{@code "to"} returns the two nested documents containing contact information as a
     *       {@link GenericDocument} array with two elements
     *   <li>{@code "to[1]"} returns the second nested document containing Marie Curie's contact
     *       information as a {@link GenericDocument} array with one element
     *   <li>{@code "to[1].email"} returns {@code "curie@example.com"}
     *   <li>{@code "to[100].email"} returns {@code null} as this particular document does not have
     *       that many elements in its {@code "to"} array.
     *   <li>{@code "to.email"} aggregates emails across all nested documents that have them,
     *       returning {@code ["einstein@example.com", "curie@example.com"]} as a {@link String}
     *       array with two elements.
     * </ul>
     *
     * <p>If you know the expected type of the property you are retrieving, it is recommended to use
     * one of the typed versions of this method instead, such as {@link #getPropertyString} or
     * {@link #getPropertyStringArray}.
     *
     * <p>If the property was assigned as an empty array using one of the {@code
     * Builder#setProperty} functions, this method will return an empty array. If no such property
     * exists at all, this method returns {@code null}.
     *
     * <p>Note: If the property is an empty {@link GenericDocument}[] or {@code byte[][]}, this
     * method will return a {@code null} value in versions of Android prior to {@link
     * android.os.Build.VERSION_CODES#TIRAMISU Android T}. Starting in Android T it will return an
     * empty array if the property has been set as an empty array, matching the behavior of other
     * property types.
     *
     * @param path The path to look for.
     * @return The entry with the given path as an object or {@code null} if there is no such path.
     *     The returned object will be one of the following types: {@code String[]}, {@code long[]},
     *     {@code double[]}, {@code boolean[]}, {@code byte[][]}, {@code GenericDocument[]}.
     */
    public @Nullable Object getProperty(@NonNull String path) {
        Objects.requireNonNull(path);
        Object rawValue =
                getRawPropertyFromRawDocument(
                        new PropertyPath(path),
                        /* pathIndex= */ 0,
                        mDocumentParcel.getPropertyMap());

        // Unpack the raw value into the types the user expects, if required.
        if (rawValue instanceof GenericDocumentParcel) {
            // getRawPropertyFromRawDocument may return a document as a bare documentParcel
            // as a performance optimization for lookups.
            GenericDocument document = new GenericDocument((GenericDocumentParcel) rawValue);
            return new GenericDocument[] {document};
        }

        if (rawValue instanceof GenericDocumentParcel[]) {
            // The underlying parcelable of nested GenericDocuments is packed into
            // a Parcelable array.
            // We must unpack it into GenericDocument instances.
            GenericDocumentParcel[] docParcels = (GenericDocumentParcel[]) rawValue;
            GenericDocument[] documents = new GenericDocument[docParcels.length];
            for (int i = 0; i < docParcels.length; i++) {
                if (docParcels[i] == null) {
                    Log.e(TAG, "The inner parcel is null at " + i + ", for path: " + path);
                    continue;
                }
                documents[i] = new GenericDocument(docParcels[i]);
            }
            return documents;
        }

        // Otherwise the raw property is the same as the final property and needs no transformation.
        return rawValue;
    }

    /**
     * Looks up a property path within the given document bundle.
     *
     * <p>The return value may be any of GenericDocument's internal repeated storage types
     * (String[], long[], double[], boolean[], ArrayList&lt;Bundle&gt;, Parcelable[]).
     *
     * <p>Usually, this method takes a path and loops over it to get a property from the bundle. But
     * in the case where we collect documents across repeated nested documents, we need to recurse
     * back into this method, and so we also keep track of the index into the path.
     *
     * @param path the PropertyPath object representing the path
     * @param pathIndex the index into the path we start at
     * @param propertyMap the map containing the path we are looking up
     * @return the raw property
     */
    @SuppressWarnings("deprecation")
    private static @Nullable Object getRawPropertyFromRawDocument(
            @NonNull PropertyPath path,
            int pathIndex,
            @NonNull Map<String, PropertyParcel> propertyMap) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(propertyMap);
        for (int i = pathIndex; i < path.size(); i++) {
            PropertyPath.PathSegment segment = path.get(i);
            Object currentElementValue = propertyMap.get(segment.getPropertyName());
            if (currentElementValue == null) {
                return null;
            }

            // If the current PathSegment has an index, we now need to update currentElementValue to
            // contain the value of the indexed property. For example, for a path segment like
            // "recipients[0]", currentElementValue now contains the value of "recipients" while we
            // need the value of "recipients[0]".
            int index = segment.getPropertyIndex();
            if (index != PropertyPath.PathSegment.NON_REPEATED_CARDINALITY) {
                // For properties bundle, now we will only get PropertyParcel as the value.
                PropertyParcel propertyParcel = (PropertyParcel) currentElementValue;

                // Extract the right array element
                Object extractedValue = null;
                if (propertyParcel.getStringValues() != null) {
                    String[] stringValues = propertyParcel.getStringValues();
                    if (stringValues != null && index < stringValues.length) {
                        extractedValue = Arrays.copyOfRange(stringValues, index, index + 1);
                    }
                } else if (propertyParcel.getLongValues() != null) {
                    long[] longValues = propertyParcel.getLongValues();
                    if (longValues != null && index < longValues.length) {
                        extractedValue = Arrays.copyOfRange(longValues, index, index + 1);
                    }
                } else if (propertyParcel.getDoubleValues() != null) {
                    double[] doubleValues = propertyParcel.getDoubleValues();
                    if (doubleValues != null && index < doubleValues.length) {
                        extractedValue = Arrays.copyOfRange(doubleValues, index, index + 1);
                    }
                } else if (propertyParcel.getBooleanValues() != null) {
                    boolean[] booleanValues = propertyParcel.getBooleanValues();
                    if (booleanValues != null && index < booleanValues.length) {
                        extractedValue = Arrays.copyOfRange(booleanValues, index, index + 1);
                    }
                } else if (propertyParcel.getBytesValues() != null) {
                    byte[][] bytesValues = propertyParcel.getBytesValues();
                    if (bytesValues != null && index < bytesValues.length) {
                        extractedValue = Arrays.copyOfRange(bytesValues, index, index + 1);
                    }
                } else if (propertyParcel.getDocumentValues() != null) {
                    // Special optimization: to avoid creating new singleton arrays for traversing
                    // paths we return the bare document parcel in this particular case.
                    GenericDocumentParcel[] docValues = propertyParcel.getDocumentValues();
                    if (docValues != null && index < docValues.length) {
                        extractedValue = docValues[index];
                    }
                } else if (propertyParcel.getEmbeddingValues() != null) {
                    EmbeddingVector[] embeddingValues = propertyParcel.getEmbeddingValues();
                    if (embeddingValues != null && index < embeddingValues.length) {
                        extractedValue = Arrays.copyOfRange(embeddingValues, index, index + 1);
                    }
                } else if (propertyParcel.getBlobHandleValues() != null) {
                    AppSearchBlobHandle[] blobHandlesValues = propertyParcel.getBlobHandleValues();
                    if (blobHandlesValues != null && index < blobHandlesValues.length) {
                        extractedValue = Arrays.copyOfRange(blobHandlesValues, index, index + 1);
                    }
                } else {
                    throw new IllegalStateException(
                            "Unsupported value type: " + currentElementValue);
                }
                currentElementValue = extractedValue;
            }

            // at the end of the path, either something like "...foo" or "...foo[1]"
            if (currentElementValue == null || i == path.size() - 1) {
                if (currentElementValue != null && currentElementValue instanceof PropertyParcel) {
                    // Unlike previous bundle-based implementation, now each
                    // value is wrapped in PropertyParcel.
                    // Here we need to get and return the actual value for non-repeated fields.
                    currentElementValue = ((PropertyParcel) currentElementValue).getValues();
                }
                return currentElementValue;
            }

            // currentElementValue is now a GenericDocumentParcel or PropertyParcel,
            // we can continue down the path.
            if (currentElementValue instanceof GenericDocumentParcel) {
                propertyMap = ((GenericDocumentParcel) currentElementValue).getPropertyMap();
            } else if (currentElementValue instanceof PropertyParcel
                    && ((PropertyParcel) currentElementValue).getDocumentValues() != null) {
                GenericDocumentParcel[] docParcels =
                        ((PropertyParcel) currentElementValue).getDocumentValues();
                if (docParcels != null && docParcels.length == 1) {
                    propertyMap = docParcels[0].getPropertyMap();
                    continue;
                }

                // Slowest path: we're collecting values across repeated nested docs. (Example:
                // given a path like recipient.name, where recipient is a repeated field, we return
                // a string array where each recipient's name is an array element).
                //
                // Performance note: Suppose that we have a property path "a.b.c" where the "a"
                // property has N document values and each containing a "b" property with M document
                // values and each of those containing a "c" property with an int array.
                //
                // We'll allocate a new ArrayList for each of the "b" properties, add the M int
                // arrays from the "c" properties to it and then we'll allocate an int array in
                // flattenAccumulator before returning that (1 + M allocation per "b" property).
                //
                // When we're on the "a" properties, we'll allocate an ArrayList and add the N
                // flattened int arrays returned from the "b" properties to the list. Then we'll
                // allocate an int array in flattenAccumulator (1 + N ("b" allocs) allocations per
                // "a"). // So this implementation could incur 1 + N + NM allocs.
                //
                // However, we expect the vast majority of getProperty calls to be either for direct
                // property names (not paths) or else property paths returned from snippetting,
                // which always refer to exactly one property value and don't aggregate across
                // repeated values. The implementation is optimized for these two cases, requiring
                // no additional allocations. So we've decided that the above performance
                // characteristics are OK for the less used path.
                if (docParcels != null) {
                    List<Object> accumulator = new ArrayList<>(docParcels.length);
                    for (GenericDocumentParcel docParcel : docParcels) {
                        // recurse as we need to branch
                        Object value =
                                getRawPropertyFromRawDocument(
                                        path,
                                        /* pathIndex= */ i + 1,
                                        ((GenericDocumentParcel) docParcel).getPropertyMap());
                        if (value != null) {
                            accumulator.add(value);
                        }
                    }
                    // Break the path traversing loop
                    return flattenAccumulator(accumulator);
                }
            } else {
                Log.e(TAG, "Failed to apply path to document; no nested value found: " + path);
                return null;
            }
        }
        // Only way to get here is with an empty path list
        return null;
    }

    /**
     * Combines accumulated repeated properties from multiple documents into a single array.
     *
     * @param accumulator List containing objects of the following types: {@code String[]}, {@code
     *     long[]}, {@code double[]}, {@code boolean[]}, {@code byte[][]}, or {@code
     *     GenericDocumentParcelable[]}.
     * @return The result of concatenating each individual list element into a larger array/list of
     *     the same type.
     */
    private static @Nullable Object flattenAccumulator(@NonNull List<Object> accumulator) {
        if (accumulator.isEmpty()) {
            return null;
        }
        Object first = accumulator.get(0);
        if (first instanceof String[]) {
            int length = 0;
            for (int i = 0; i < accumulator.size(); i++) {
                length += ((String[]) accumulator.get(i)).length;
            }
            String[] result = new String[length];
            int total = 0;
            for (int i = 0; i < accumulator.size(); i++) {
                String[] castValue = (String[]) accumulator.get(i);
                System.arraycopy(castValue, 0, result, total, castValue.length);
                total += castValue.length;
            }
            return result;
        }
        if (first instanceof long[]) {
            int length = 0;
            for (int i = 0; i < accumulator.size(); i++) {
                length += ((long[]) accumulator.get(i)).length;
            }
            long[] result = new long[length];
            int total = 0;
            for (int i = 0; i < accumulator.size(); i++) {
                long[] castValue = (long[]) accumulator.get(i);
                System.arraycopy(castValue, 0, result, total, castValue.length);
                total += castValue.length;
            }
            return result;
        }
        if (first instanceof double[]) {
            int length = 0;
            for (int i = 0; i < accumulator.size(); i++) {
                length += ((double[]) accumulator.get(i)).length;
            }
            double[] result = new double[length];
            int total = 0;
            for (int i = 0; i < accumulator.size(); i++) {
                double[] castValue = (double[]) accumulator.get(i);
                System.arraycopy(castValue, 0, result, total, castValue.length);
                total += castValue.length;
            }
            return result;
        }
        if (first instanceof boolean[]) {
            int length = 0;
            for (int i = 0; i < accumulator.size(); i++) {
                length += ((boolean[]) accumulator.get(i)).length;
            }
            boolean[] result = new boolean[length];
            int total = 0;
            for (int i = 0; i < accumulator.size(); i++) {
                boolean[] castValue = (boolean[]) accumulator.get(i);
                System.arraycopy(castValue, 0, result, total, castValue.length);
                total += castValue.length;
            }
            return result;
        }
        if (first instanceof byte[][]) {
            int length = 0;
            for (int i = 0; i < accumulator.size(); i++) {
                length += ((byte[][]) accumulator.get(i)).length;
            }
            byte[][] result = new byte[length][];
            int total = 0;
            for (int i = 0; i < accumulator.size(); i++) {
                byte[][] castValue = (byte[][]) accumulator.get(i);
                System.arraycopy(castValue, 0, result, total, castValue.length);
                total += castValue.length;
            }
            return result;
        }
        if (first instanceof GenericDocumentParcel[]) {
            int length = 0;
            for (int i = 0; i < accumulator.size(); i++) {
                length += ((GenericDocumentParcel[]) accumulator.get(i)).length;
            }
            GenericDocumentParcel[] result = new GenericDocumentParcel[length];
            int total = 0;
            for (int i = 0; i < accumulator.size(); i++) {
                GenericDocumentParcel[] castValue = (GenericDocumentParcel[]) accumulator.get(i);
                System.arraycopy(castValue, 0, result, total, castValue.length);
                total += castValue.length;
            }
            return result;
        }
        throw new IllegalStateException("Unexpected property type: " + first);
    }

    /**
     * Retrieves a {@link String} property by path.
     *
     * <p>See {@link #getProperty} for a detailed description of the path syntax.
     *
     * @param path The path to look for.
     * @return The first {@link String} associated with the given path or {@code null} if there is
     *     no such value or the value is of a different type.
     */
    public @Nullable String getPropertyString(@NonNull String path) {
        Objects.requireNonNull(path);
        String[] propertyArray = getPropertyStringArray(path);
        if (propertyArray == null || propertyArray.length == 0) {
            return null;
        }
        warnIfSinglePropertyTooLong("String", path, propertyArray.length);
        return propertyArray[0];
    }

    /**
     * Retrieves a {@code long} property by path.
     *
     * <p>See {@link #getProperty} for a detailed description of the path syntax.
     *
     * @param path The path to look for.
     * @return The first {@code long} associated with the given path or default value {@code 0} if
     *     there is no such value or the value is of a different type.
     */
    public long getPropertyLong(@NonNull String path) {
        Objects.requireNonNull(path);
        long[] propertyArray = getPropertyLongArray(path);
        if (propertyArray == null || propertyArray.length == 0) {
            return 0;
        }
        warnIfSinglePropertyTooLong("Long", path, propertyArray.length);
        return propertyArray[0];
    }

    /**
     * Retrieves a {@code double} property by path.
     *
     * <p>See {@link #getProperty} for a detailed description of the path syntax.
     *
     * @param path The path to look for.
     * @return The first {@code double} associated with the given path or default value {@code 0.0}
     *     if there is no such value or the value is of a different type.
     */
    public double getPropertyDouble(@NonNull String path) {
        Objects.requireNonNull(path);
        double[] propertyArray = getPropertyDoubleArray(path);
        if (propertyArray == null || propertyArray.length == 0) {
            return 0.0;
        }
        warnIfSinglePropertyTooLong("Double", path, propertyArray.length);
        return propertyArray[0];
    }

    /**
     * Retrieves a {@code boolean} property by path.
     *
     * <p>See {@link #getProperty} for a detailed description of the path syntax.
     *
     * @param path The path to look for.
     * @return The first {@code boolean} associated with the given path or default value {@code
     *     false} if there is no such value or the value is of a different type.
     */
    public boolean getPropertyBoolean(@NonNull String path) {
        Objects.requireNonNull(path);
        boolean[] propertyArray = getPropertyBooleanArray(path);
        if (propertyArray == null || propertyArray.length == 0) {
            return false;
        }
        warnIfSinglePropertyTooLong("Boolean", path, propertyArray.length);
        return propertyArray[0];
    }

    /**
     * Retrieves a {@code byte[]} property by path.
     *
     * <p>See {@link #getProperty} for a detailed description of the path syntax.
     *
     * @param path The path to look for.
     * @return The first {@code byte[]} associated with the given path or {@code null} if there is
     *     no such value or the value is of a different type.
     */
    public @Nullable byte[] getPropertyBytes(@NonNull String path) {
        Objects.requireNonNull(path);
        byte[][] propertyArray = getPropertyBytesArray(path);
        if (propertyArray == null || propertyArray.length == 0) {
            return null;
        }
        warnIfSinglePropertyTooLong("ByteArray", path, propertyArray.length);
        return propertyArray[0];
    }

    /**
     * Retrieves a {@link GenericDocument} property by path.
     *
     * <p>See {@link #getProperty} for a detailed description of the path syntax.
     *
     * @param path The path to look for.
     * @return The first {@link GenericDocument} associated with the given path or {@code null} if
     *     there is no such value or the value is of a different type.
     */
    public @Nullable GenericDocument getPropertyDocument(@NonNull String path) {
        Objects.requireNonNull(path);
        GenericDocument[] propertyArray = getPropertyDocumentArray(path);
        if (propertyArray == null || propertyArray.length == 0) {
            return null;
        }
        warnIfSinglePropertyTooLong("Document", path, propertyArray.length);
        return propertyArray[0];
    }

    /**
     * Retrieves an {@code EmbeddingVector} property by path.
     *
     * <p>See {@link #getProperty} for a detailed description of the path syntax.
     *
     * @param path The path to look for.
     * @return The first {@code EmbeddingVector[]} associated with the given path or {@code null} if
     *     there is no such value or the value is of a different type.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public @Nullable EmbeddingVector getPropertyEmbedding(@NonNull String path) {
        Objects.requireNonNull(path);
        EmbeddingVector[] propertyArray = getPropertyEmbeddingArray(path);
        if (propertyArray == null || propertyArray.length == 0) {
            return null;
        }
        warnIfSinglePropertyTooLong("Embedding", path, propertyArray.length);
        return propertyArray[0];
    }

    /**
     * Retrieves an {@link AppSearchBlobHandle} property by path.
     *
     * <p>See {@link #getProperty} for a detailed description of the path syntax.
     *
     * <p>See {@link AppSearchSession#openBlobForRead} for how to use {@link AppSearchBlobHandle} to
     * retrieve blob data.
     *
     * @param path The path to look for.
     * @return The first {@link AppSearchBlobHandle} associated with the given path or {@code null}
     *     if there is no such value or the value is of a different type.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_BLOB_STORE)
    public @Nullable AppSearchBlobHandle getPropertyBlobHandle(@NonNull String path) {
        Objects.requireNonNull(path);
        AppSearchBlobHandle[] propertyArray = getPropertyBlobHandleArray(path);
        if (propertyArray == null || propertyArray.length == 0) {
            return null;
        }
        warnIfSinglePropertyTooLong("BlobHandle", path, propertyArray.length);
        return propertyArray[0];
    }

    /** Prints a warning to logcat if the given propertyLength is greater than 1. */
    private static void warnIfSinglePropertyTooLong(
            @NonNull String propertyType, @NonNull String path, int propertyLength) {
        if (propertyLength > 1) {
            Log.w(
                    TAG,
                    "The value for \""
                            + path
                            + "\" contains "
                            + propertyLength
                            + " elements. Only the first one will be returned from "
                            + "getProperty"
                            + propertyType
                            + "(). Try getProperty"
                            + propertyType
                            + "Array().");
        }
    }

    /**
     * Retrieves a repeated {@code String} property by path.
     *
     * <p>See {@link #getProperty} for a detailed description of the path syntax.
     *
     * <p>If the property has not been set via {@link Builder#setPropertyString}, this method
     * returns {@code null}.
     *
     * <p>If it has been set via {@link Builder#setPropertyString} to an empty {@code String[]},
     * this method returns an empty {@code String[]}.
     *
     * @param path The path to look for.
     * @return The {@code String[]} associated with the given path, or {@code null} if no value is
     *     set or the value is of a different type.
     */
    public @Nullable String[] getPropertyStringArray(@NonNull String path) {
        Objects.requireNonNull(path);
        Object value = getProperty(path);
        return safeCastProperty(path, value, String[].class);
    }

    /**
     * Retrieves a repeated {@code long[]} property by path.
     *
     * <p>See {@link #getProperty} for a detailed description of the path syntax.
     *
     * <p>If the property has not been set via {@link Builder#setPropertyLong}, this method returns
     * {@code null}.
     *
     * <p>If it has been set via {@link Builder#setPropertyLong} to an empty {@code long[]}, this
     * method returns an empty {@code long[]}.
     *
     * @param path The path to look for.
     * @return The {@code long[]} associated with the given path, or {@code null} if no value is set
     *     or the value is of a different type.
     */
    public @Nullable long[] getPropertyLongArray(@NonNull String path) {
        Objects.requireNonNull(path);
        Object value = getProperty(path);
        return safeCastProperty(path, value, long[].class);
    }

    /**
     * Retrieves a repeated {@code double} property by path.
     *
     * <p>See {@link #getProperty} for a detailed description of the path syntax.
     *
     * <p>If the property has not been set via {@link Builder#setPropertyDouble}, this method
     * returns {@code null}.
     *
     * <p>If it has been set via {@link Builder#setPropertyDouble} to an empty {@code double[]},
     * this method returns an empty {@code double[]}.
     *
     * @param path The path to look for.
     * @return The {@code double[]} associated with the given path, or {@code null} if no value is
     *     set or the value is of a different type.
     */
    public @Nullable double[] getPropertyDoubleArray(@NonNull String path) {
        Objects.requireNonNull(path);
        Object value = getProperty(path);
        return safeCastProperty(path, value, double[].class);
    }

    /**
     * Retrieves a repeated {@code boolean} property by path.
     *
     * <p>See {@link #getProperty} for a detailed description of the path syntax.
     *
     * <p>If the property has not been set via {@link Builder#setPropertyBoolean}, this method
     * returns {@code null}.
     *
     * <p>If it has been set via {@link Builder#setPropertyBoolean} to an empty {@code boolean[]},
     * this method returns an empty {@code boolean[]}.
     *
     * @param path The path to look for.
     * @return The {@code boolean[]} associated with the given path, or {@code null} if no value is
     *     set or the value is of a different type.
     */
    public @Nullable boolean[] getPropertyBooleanArray(@NonNull String path) {
        Objects.requireNonNull(path);
        Object value = getProperty(path);
        return safeCastProperty(path, value, boolean[].class);
    }

    /**
     * Retrieves a {@code byte[][]} property by path.
     *
     * <p>See {@link #getProperty} for a detailed description of the path syntax.
     *
     * <p>If the property has not been set via {@link Builder#setPropertyBytes}, this method returns
     * {@code null}.
     *
     * <p>If it has been set via {@link Builder#setPropertyBytes} to an empty {@code byte[][]}, this
     * method returns an empty {@code byte[][]} starting in {@link
     * android.os.Build.VERSION_CODES#TIRAMISU Android T} and {@code null} in earlier versions of
     * Android.
     *
     * @param path The path to look for.
     * @return The {@code byte[][]} associated with the given path, or {@code null} if no value is
     *     set or the value is of a different type.
     */
    @SuppressLint("ArrayReturn")
    public @Nullable byte[][] getPropertyBytesArray(@NonNull String path) {
        Objects.requireNonNull(path);
        Object value = getProperty(path);
        return safeCastProperty(path, value, byte[][].class);
    }

    /**
     * Retrieves a repeated {@link GenericDocument} property by path.
     *
     * <p>See {@link #getProperty} for a detailed description of the path syntax.
     *
     * <p>If the property has not been set via {@link Builder#setPropertyDocument}, this method
     * returns {@code null}.
     *
     * <p>If it has been set via {@link Builder#setPropertyDocument} to an empty {@code
     * GenericDocument[]}, this method returns an empty {@code GenericDocument[]} starting in {@link
     * android.os.Build.VERSION_CODES#TIRAMISU Android T} and {@code null} in earlier versions of
     * Android.
     *
     * @param path The path to look for.
     * @return The {@link GenericDocument}[] associated with the given path, or {@code null} if no
     *     value is set or the value is of a different type.
     */
    @SuppressLint("ArrayReturn")
    public @Nullable GenericDocument[] getPropertyDocumentArray(@NonNull String path) {
        Objects.requireNonNull(path);
        Object value = getProperty(path);
        return safeCastProperty(path, value, GenericDocument[].class);
    }

    /**
     * Retrieves a repeated {@code EmbeddingVector[]} property by path.
     *
     * <p>See {@link #getProperty} for a detailed description of the path syntax.
     *
     * <p>If the property has not been set via {@link Builder#setPropertyEmbedding}, this method
     * returns {@code null}.
     *
     * <p>If it has been set via {@link Builder#setPropertyEmbedding} to an empty {@code
     * EmbeddingVector[]}, this method returns an empty {@code EmbeddingVector[]}.
     *
     * @param path The path to look for.
     * @return The {@code EmbeddingVector[]} associated with the given path, or {@code null} if no
     *     value is set or the value is of a different type.
     */
    @SuppressLint({"ArrayReturn", "NullableCollection"})
    @FlaggedApi(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public @Nullable EmbeddingVector[] getPropertyEmbeddingArray(@NonNull String path) {
        Objects.requireNonNull(path);
        Object value = getProperty(path);
        return safeCastProperty(path, value, EmbeddingVector[].class);
    }

    /**
     * Retrieves a repeated {@code AppSearchBlobHandle[]} property by path.
     *
     * <p>See {@link #getProperty} for a detailed description of the path syntax.
     *
     * <p>If the property has not been set via {@link Builder#setPropertyBlobHandle}, this method
     * returns {@code null}.
     *
     * <p>If it has been set via {@link Builder#setPropertyBlobHandle} to an empty {@code
     * AppSearchBlobHandle[]}, this method returns an empty {@code AppSearchBlobHandle[]}.
     *
     * @param path The path to look for.
     * @return The {@code AppSearchBlobHandle[]} associated with the given path, or {@code null} if
     *     no value is set or the value is of a different type.
     */
    @SuppressLint({"ArrayReturn", "NullableCollection"})
    @FlaggedApi(Flags.FLAG_ENABLE_BLOB_STORE)
    public @Nullable AppSearchBlobHandle[] getPropertyBlobHandleArray(@NonNull String path) {
        Objects.requireNonNull(path);
        Object value = getProperty(path);
        return safeCastProperty(path, value, AppSearchBlobHandle[].class);
    }

    /**
     * Casts a repeated property to the provided type, logging an error and returning {@code null}
     * if the cast fails.
     *
     * @param path Path to the property within the document. Used for logging.
     * @param value Value of the property
     * @param tClass Class to cast the value into
     */
    private static <T> @Nullable T safeCastProperty(
            @NonNull String path, @Nullable Object value, @NonNull Class<T> tClass) {
        if (value == null) {
            return null;
        }
        try {
            return tClass.cast(value);
        } catch (ClassCastException e) {
            Log.w(TAG, "Error casting to requested type for path \"" + path + "\"", e);
            return null;
        }
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GenericDocument)) {
            return false;
        }
        GenericDocument otherDocument = (GenericDocument) other;
        return mDocumentParcel.equals(otherDocument.mDocumentParcel);
    }

    @Override
    public int hashCode() {
        return mDocumentParcel.hashCode();
    }

    @Override
    public @NonNull String toString() {
        IndentingStringBuilder stringBuilder = new IndentingStringBuilder();
        appendGenericDocumentString(stringBuilder);
        return stringBuilder.toString();
    }

    /**
     * Appends a debug string for the {@link GenericDocument} instance to the given string builder.
     *
     * @param builder the builder to append to.
     */
    void appendGenericDocumentString(@NonNull IndentingStringBuilder builder) {
        Objects.requireNonNull(builder);

        builder.append("{\n");
        builder.increaseIndentLevel();

        builder.append("namespace: \"").append(getNamespace()).append("\",\n");
        builder.append("id: \"").append(getId()).append("\",\n");
        builder.append("score: ").append(getScore()).append(",\n");
        builder.append("schemaType: \"").append(getSchemaType()).append("\",\n");
        List<String> parentTypes = getParentTypes();
        if (parentTypes != null) {
            builder.append("parentTypes: ").append(parentTypes).append("\n");
        }
        builder.append("creationTimestampMillis: ")
                .append(getCreationTimestampMillis())
                .append(",\n");
        builder.append("timeToLiveMillis: ").append(getTtlMillis()).append(",\n");

        builder.append("properties: {\n");

        String[] sortedProperties = getPropertyNames().toArray(new String[0]);
        Arrays.sort(sortedProperties);

        for (int i = 0; i < sortedProperties.length; i++) {
            Object property = Objects.requireNonNull(getProperty(sortedProperties[i]));
            builder.increaseIndentLevel();
            appendPropertyString(sortedProperties[i], property, builder);
            if (i != sortedProperties.length - 1) {
                builder.append(",\n");
            }
            builder.decreaseIndentLevel();
        }

        builder.append("\n");
        builder.append("}");

        builder.decreaseIndentLevel();
        builder.append("\n");
        builder.append("}");
    }

    /**
     * Appends a debug string for the given document property to the given string builder.
     *
     * @param propertyName name of property to create string for.
     * @param property property object to create string for.
     * @param builder the builder to append to.
     */
    private void appendPropertyString(
            @NonNull String propertyName,
            @NonNull Object property,
            @NonNull IndentingStringBuilder builder) {
        Objects.requireNonNull(propertyName);
        Objects.requireNonNull(property);
        Objects.requireNonNull(builder);

        builder.append("\"").append(propertyName).append("\": [");
        if (property instanceof GenericDocument[]) {
            GenericDocument[] documentValues = (GenericDocument[]) property;
            for (int i = 0; i < documentValues.length; ++i) {
                builder.append("\n");
                builder.increaseIndentLevel();
                documentValues[i].appendGenericDocumentString(builder);
                if (i != documentValues.length - 1) {
                    builder.append(",");
                }
                builder.append("\n");
                builder.decreaseIndentLevel();
            }
        } else {
            int propertyArrLength = Array.getLength(property);
            for (int i = 0; i < propertyArrLength; i++) {
                Object propertyElement = Array.get(property, i);
                if (propertyElement instanceof String) {
                    builder.append("\"").append((String) propertyElement).append("\"");
                } else if (propertyElement instanceof byte[]) {
                    builder.append(Arrays.toString((byte[]) propertyElement));
                } else if (propertyElement != null) {
                    builder.append(propertyElement.toString());
                }
                if (i != propertyArrLength - 1) {
                    builder.append(", ");
                }
            }
        }
        builder.append("]");
    }

    /**
     * The builder class for {@link GenericDocument}.
     *
     * @param <BuilderType> Type of subclass who extends this.
     */
    // This builder is specifically designed to be extended by classes deriving from
    // GenericDocument.
    @SuppressLint("StaticFinalBuilder")
    @SuppressWarnings("rawtypes")
    public static class Builder<BuilderType extends Builder> {
        private final GenericDocumentParcel.Builder mDocumentParcelBuilder;
        private final BuilderType mBuilderTypeInstance;

        /**
         * Creates a new {@link GenericDocument.Builder}.
         *
         * <p>Document IDs are unique within a namespace.
         *
         * <p>The number of namespaces per app should be kept small for efficiency reasons.
         *
         * @param namespace the namespace to set for the {@link GenericDocument}.
         * @param id the unique identifier for the {@link GenericDocument} in its namespace.
         * @param schemaType the {@link AppSearchSchema} type of the {@link GenericDocument}. The
         *     provided {@code schemaType} must be defined using {@link AppSearchSession#setSchema}
         *     prior to inserting a document of this {@code schemaType} into the AppSearch index
         *     using {@link AppSearchSession#put}. Otherwise, the document will be rejected by
         *     {@link AppSearchSession#put} with result code {@link
         *     AppSearchResult#RESULT_NOT_FOUND}.
         */
        @SuppressWarnings("unchecked")
        public Builder(@NonNull String namespace, @NonNull String id, @NonNull String schemaType) {
            Objects.requireNonNull(namespace);
            Objects.requireNonNull(id);
            Objects.requireNonNull(schemaType);

            mBuilderTypeInstance = (BuilderType) this;
            mDocumentParcelBuilder = new GenericDocumentParcel.Builder(namespace, id, schemaType);
        }

        /**
         * Creates a new {@link GenericDocument.Builder} from the given {@link
         * GenericDocumentParcel.Builder}.
         *
         * <p>The bundle is NOT copied.
         */
        @SuppressWarnings("unchecked")
        Builder(@NonNull GenericDocumentParcel.Builder documentParcelBuilder) {
            mDocumentParcelBuilder = Objects.requireNonNull(documentParcelBuilder);
            mBuilderTypeInstance = (BuilderType) this;
        }

        /**
         * Creates a new {@link GenericDocument.Builder} from the given GenericDocument.
         *
         * <p>The GenericDocument is deep copied, that is, it changes to a new GenericDocument
         * returned by this function and will NOT affect the original GenericDocument.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_GENERIC_DOCUMENT_COPY_CONSTRUCTOR)
        public Builder(@NonNull GenericDocument document) {
            this(new GenericDocumentParcel.Builder(document.mDocumentParcel));
        }

        /**
         * Sets the app-defined namespace this document resides in, changing the value provided in
         * the constructor. No special values are reserved or understood by the infrastructure.
         *
         * <p>Document IDs are unique within a namespace.
         *
         * <p>The number of namespaces per app should be kept small for efficiency reasons.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_GENERIC_DOCUMENT_BUILDER_HIDDEN_METHODS)
        @CanIgnoreReturnValue
        public @NonNull BuilderType setNamespace(@NonNull String namespace) {
            Objects.requireNonNull(namespace);
            mDocumentParcelBuilder.setNamespace(namespace);
            return mBuilderTypeInstance;
        }

        /**
         * Sets the ID of this document, changing the value provided in the constructor. No special
         * values are reserved or understood by the infrastructure.
         *
         * <p>Document IDs are unique within the combination of package, database, and namespace.
         *
         * <p>Setting a document with a duplicate id will overwrite the original document with the
         * new document, enforcing uniqueness within the above constraint.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_GENERIC_DOCUMENT_BUILDER_HIDDEN_METHODS)
        @CanIgnoreReturnValue
        public @NonNull BuilderType setId(@NonNull String id) {
            Objects.requireNonNull(id);
            mDocumentParcelBuilder.setId(id);
            return mBuilderTypeInstance;
        }

        /**
         * Sets the schema type of this document, changing the value provided in the constructor.
         *
         * <p>To successfully index a document, the schema type must match the name of an {@link
         * AppSearchSchema} object previously provided to {@link AppSearchSession#setSchema}.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_GENERIC_DOCUMENT_BUILDER_HIDDEN_METHODS)
        @CanIgnoreReturnValue
        public @NonNull BuilderType setSchemaType(@NonNull String schemaType) {
            Objects.requireNonNull(schemaType);
            mDocumentParcelBuilder.setSchemaType(schemaType);
            return mBuilderTypeInstance;
        }

        /**
         * Sets the list of parent types of the {@link GenericDocument}'s type.
         *
         * <p>Child types must appear before parent types in the list.
         *
         * @deprecated Parent types should no longer be set in {@link GenericDocument}. Use {@link
         *     SearchResult.Builder#setParentTypeMap(Map)} instead.
         * @hide
         */
        @CanIgnoreReturnValue
        @Deprecated
        public @NonNull BuilderType setParentTypes(@Nullable List<String> parentTypes) {
            mDocumentParcelBuilder.setParentTypes(parentTypes);
            return mBuilderTypeInstance;
        }

        /**
         * Sets the score of the {@link GenericDocument}.
         *
         * <p>The score is a query-independent measure of the document's quality, relative to other
         * {@link GenericDocument} objects of the same {@link AppSearchSchema} type.
         *
         * <p>Results may be sorted by score using {@link SearchSpec.Builder#setRankingStrategy}.
         * Documents with higher scores are considered better than documents with lower scores.
         *
         * <p>Any non-negative integer can be used a score. By default, scores are set to 0.
         *
         * @param score any non-negative {@code int} representing the document's score.
         * @throws IllegalArgumentException if the score is negative.
         */
        @CanIgnoreReturnValue
        public @NonNull BuilderType setScore(
                @IntRange(from = 0, to = Integer.MAX_VALUE) int score) {
            if (score < 0) {
                throw new IllegalArgumentException("Document score cannot be negative.");
            }
            mDocumentParcelBuilder.setScore(score);
            return mBuilderTypeInstance;
        }

        /**
         * Sets the creation timestamp of the {@link GenericDocument}, in milliseconds.
         *
         * <p>This should be set using a value obtained from the {@link System#currentTimeMillis}
         * time base.
         *
         * <p>If this method is not called, this will be set to the time the object is built.
         *
         * @param creationTimestampMillis a creation timestamp in milliseconds.
         */
        @CanIgnoreReturnValue
        public @NonNull BuilderType setCreationTimestampMillis(
                @CurrentTimeMillisLong long creationTimestampMillis) {
            mDocumentParcelBuilder.setCreationTimestampMillis(creationTimestampMillis);
            return mBuilderTypeInstance;
        }

        /**
         * Sets the TTL (time-to-live) of the {@link GenericDocument}, in milliseconds.
         *
         * <p>The TTL is measured against {@link #getCreationTimestampMillis}. At the timestamp of
         * {@code creationTimestampMillis + ttlMillis}, measured in the {@link
         * System#currentTimeMillis} time base, the document will be auto-deleted.
         *
         * <p>The default value is 0, which means the document is permanent and won't be
         * auto-deleted until the app is uninstalled or {@link AppSearchSession#remove} is called.
         *
         * @param ttlMillis a non-negative duration in milliseconds.
         * @throws IllegalArgumentException if ttlMillis is negative.
         */
        @CanIgnoreReturnValue
        public @NonNull BuilderType setTtlMillis(long ttlMillis) {
            if (ttlMillis < 0) {
                throw new IllegalArgumentException("Document ttlMillis cannot be negative.");
            }
            mDocumentParcelBuilder.setTtlMillis(ttlMillis);
            return mBuilderTypeInstance;
        }

        /**
         * Sets one or multiple {@code String} values for a property, replacing its previous values.
         *
         * @param name the name associated with the {@code values}. Must match the name for this
         *     property as given in {@link AppSearchSchema.PropertyConfig#getName}.
         * @param values the {@code String} values of the property.
         * @throws IllegalArgumentException if no values are provided, or if a passed in {@code
         *     String} is {@code null} or "".
         */
        @CanIgnoreReturnValue
        public @NonNull BuilderType setPropertyString(
                @NonNull String name, @NonNull String... values) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(values);
            validatePropertyName(name);
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) {
                    throw new IllegalArgumentException("The String at " + i + " is null.");
                }
            }
            mDocumentParcelBuilder.putInPropertyMap(name, values);
            return mBuilderTypeInstance;
        }

        /**
         * Sets one or multiple {@code boolean} values for a property, replacing its previous
         * values.
         *
         * @param name the name associated with the {@code values}. Must match the name for this
         *     property as given in {@link AppSearchSchema.PropertyConfig#getName}.
         * @param values the {@code boolean} values of the property.
         * @throws IllegalArgumentException if the name is empty or {@code null}.
         */
        @CanIgnoreReturnValue
        public @NonNull BuilderType setPropertyBoolean(
                @NonNull String name, @NonNull boolean... values) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(values);
            validatePropertyName(name);
            mDocumentParcelBuilder.putInPropertyMap(name, values);
            return mBuilderTypeInstance;
        }

        /**
         * Sets one or multiple {@code long} values for a property, replacing its previous values.
         *
         * @param name the name associated with the {@code values}. Must match the name for this
         *     property as given in {@link AppSearchSchema.PropertyConfig#getName}.
         * @param values the {@code long} values of the property.
         * @throws IllegalArgumentException if the name is empty or {@code null}.
         */
        @CanIgnoreReturnValue
        public @NonNull BuilderType setPropertyLong(@NonNull String name, @NonNull long... values) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(values);
            validatePropertyName(name);
            mDocumentParcelBuilder.putInPropertyMap(name, values);
            return mBuilderTypeInstance;
        }

        /**
         * Sets one or multiple {@code double} values for a property, replacing its previous values.
         *
         * @param name the name associated with the {@code values}. Must match the name for this
         *     property as given in {@link AppSearchSchema.PropertyConfig#getName}.
         * @param values the {@code double} values of the property.
         * @throws IllegalArgumentException if the name is empty or {@code null}.
         */
        @CanIgnoreReturnValue
        public @NonNull BuilderType setPropertyDouble(
                @NonNull String name, @NonNull double... values) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(values);
            validatePropertyName(name);
            mDocumentParcelBuilder.putInPropertyMap(name, values);
            return mBuilderTypeInstance;
        }

        /**
         * Sets one or multiple {@code byte[]} for a property, replacing its previous values.
         *
         * <p>For large byte data and lazy retrieval, see {@link #setPropertyBlobHandle}.
         *
         * @param name the name associated with the {@code values}. Must match the name for this
         *     property as given in {@link AppSearchSchema.PropertyConfig#getName}.
         * @param values the {@code byte[]} of the property.
         * @throws IllegalArgumentException if no values are provided, or if a passed in {@code
         *     byte[]} is {@code null}, or if name is empty.
         */
        @CanIgnoreReturnValue
        public @NonNull BuilderType setPropertyBytes(
                @NonNull String name, @NonNull byte[]... values) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(values);
            validatePropertyName(name);
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) {
                    throw new IllegalArgumentException("The byte[] at " + i + " is null.");
                }
            }
            mDocumentParcelBuilder.putInPropertyMap(name, values);
            return mBuilderTypeInstance;
        }

        /**
         * Sets one or multiple {@link GenericDocument} values for a property, replacing its
         * previous values.
         *
         * @param name the name associated with the {@code values}. Must match the name for this
         *     property as given in {@link AppSearchSchema.PropertyConfig#getName}.
         * @param values the {@link GenericDocument} values of the property.
         * @throws IllegalArgumentException if no values are provided, or if a passed in {@link
         *     GenericDocument} is {@code null}, or if name is empty.
         */
        @CanIgnoreReturnValue
        public @NonNull BuilderType setPropertyDocument(
                @NonNull String name, @NonNull GenericDocument... values) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(values);
            validatePropertyName(name);
            GenericDocumentParcel[] documentParcels = new GenericDocumentParcel[values.length];
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) {
                    throw new IllegalArgumentException("The document at " + i + " is null.");
                }
                documentParcels[i] = values[i].getDocumentParcel();
            }
            mDocumentParcelBuilder.putInPropertyMap(name, documentParcels);
            return mBuilderTypeInstance;
        }

        /**
         * Sets one or multiple {@code EmbeddingVector} values for a property, replacing its
         * previous values.
         *
         * @param name the name associated with the {@code values}. Must match the name for this
         *     property as given in {@link AppSearchSchema.PropertyConfig#getName}.
         * @param values the {@code EmbeddingVector} values of the property.
         * @throws IllegalArgumentException if the name is empty or {@code null}.
         */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
        public @NonNull BuilderType setPropertyEmbedding(
                @NonNull String name, @NonNull EmbeddingVector... values) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(values);
            validatePropertyName(name);
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) {
                    throw new IllegalArgumentException("The EmbeddingVector at " + i + " is null.");
                }
            }
            mDocumentParcelBuilder.putInPropertyMap(name, values);
            return mBuilderTypeInstance;
        }

        /**
         * Sets one or multiple {@link AppSearchBlobHandle} values for a property, replacing its
         * previous values.
         *
         * <p>{@link AppSearchBlobHandle} is a pointer to a blob of data.
         *
         * <p>Store large byte via the {@link android.os.ParcelFileDescriptor} returned from {@link
         * AppSearchSession#openBlobForWrite}. Once the blob data is committed via {@link
         * AppSearchSession#commitBlob}, the blob is retrievable via {@link
         * AppSearchSession#openBlobForRead}.
         *
         * @param name the name associated with the {@code values}. Must match the name for this
         *     property as given in {@link AppSearchSchema.PropertyConfig#getName}.
         * @param values the {@link AppSearchBlobHandle} values of the property.
         * @throws IllegalArgumentException if the name is empty or {@code null}.
         */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_BLOB_STORE)
        public @NonNull BuilderType setPropertyBlobHandle(
                @NonNull String name, @NonNull AppSearchBlobHandle... values) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(values);
            validatePropertyName(name);
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) {
                    throw new IllegalArgumentException("The BlobHandle at " + i + " is null.");
                }
            }
            mDocumentParcelBuilder.putInPropertyMap(name, values);
            return mBuilderTypeInstance;
        }

        /**
         * Clears the value for the property with the given name.
         *
         * <p>Note that this method does not support property paths.
         *
         * <p>You should check for the existence of the property in {@link #getPropertyNames} if you
         * need to make sure the property being cleared actually exists.
         *
         * <p>If the string passed is an invalid or nonexistent property, no error message or
         * behavior will be observed.
         *
         * @param name The name of the property to clear.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_GENERIC_DOCUMENT_BUILDER_HIDDEN_METHODS)
        @CanIgnoreReturnValue
        public @NonNull BuilderType clearProperty(@NonNull String name) {
            Objects.requireNonNull(name);
            mDocumentParcelBuilder.clearProperty(name);
            return mBuilderTypeInstance;
        }

        /** Builds the {@link GenericDocument} object. */
        public @NonNull GenericDocument build() {
            return new GenericDocument(mDocumentParcelBuilder.build());
        }

        /** Method to ensure property names are not blank */
        private void validatePropertyName(@NonNull String name) {
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Property name cannot be blank.");
            }
        }
    }
}
