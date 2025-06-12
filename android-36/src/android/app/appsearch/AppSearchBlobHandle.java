/*
 * Copyright 2024 The Android Open Source Project
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

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.app.appsearch.util.IndentingStringBuilder;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.appsearch.flags.Flags;
import com.android.internal.util.Preconditions;

import java.util.Arrays;
import java.util.Objects;

/**
 * An identifier to represent a blob in AppSearch.
 *
 * <p>A "blob" is a large binary object. It is used to store a significant amount of data that is
 * not searchable, such as images, videos, audio files, or other binary data. Unlike other fields in
 * AppSearch, blobs are stored as blob files on disk rather than in memory, and use {@link
 * android.os.ParcelFileDescriptor} to read and write. This allows for efficient handling of large,
 * non-searchable content.
 *
 * <p>{@link AppSearchBlobHandle} is a light-weight {@code Property} of {@link GenericDocument},
 * which is a pointer to the heavy-weight blob data.
 *
 * <p>The blob data could be written via {@link AppSearchSession#openBlobForWrite} and read via
 * {@link AppSearchSession#openBlobForRead}.
 *
 * <p>A {@link GenericDocument} with {@link AppSearchBlobHandle} {@code Property} could be put and
 * read without the large blob data. This offers lazy retrieval to blob data when searching {@link
 * GenericDocument} in AppSearch.
 *
 * @see GenericDocument.Builder#setPropertyBlobHandle
 */
@FlaggedApi(Flags.FLAG_ENABLE_BLOB_STORE)
// TODO(b/384721898): Switch to JSpecify annotations
@SuppressWarnings({"HiddenSuperclass", "JSpecifyNullness"})
@SafeParcelable.Class(creator = "AppSearchBlobHandleCreator")
public final class AppSearchBlobHandle extends AbstractSafeParcelable {
    /** The length of the SHA-256 digest in bytes. SHA-256 produces a 256-bit (32-byte) digest. */
    private static final int SHA_256_DIGEST_BYTE_LENGTH = 32;

    @NonNull
    public static final Parcelable.Creator<AppSearchBlobHandle> CREATOR =
            new AppSearchBlobHandleCreator();

    @Field(id = 1, getter = "getSha256Digest")
    private final @NonNull byte[] mSha256Digest;

    @Field(id = 2, getter = "getPackageName")
    private final @NonNull String mPackageName;

    @Field(id = 3, getter = "getDatabaseName")
    private final @NonNull String mDatabaseName;

    @Field(id = 4, getter = "getNamespace")
    private final @NonNull String mNamespace;

    private @Nullable Integer mHashCode;

    /**
     * Build an {@link AppSearchBlobHandle}.
     *
     * @hide
     */
    @Constructor
    AppSearchBlobHandle(
            @Param(id = 1) @NonNull byte[] sha256Digest,
            @Param(id = 2) @NonNull String packageName,
            @Param(id = 3) @NonNull String databaseName,
            @Param(id = 4) @NonNull String namespace) {
        mSha256Digest = Objects.requireNonNull(sha256Digest);
        Preconditions.checkState(
                sha256Digest.length == SHA_256_DIGEST_BYTE_LENGTH,
                "The input digest isn't a sha-256 digest.");
        mPackageName = Objects.requireNonNull(packageName);
        mDatabaseName = Objects.requireNonNull(databaseName);
        mNamespace = Objects.requireNonNull(namespace);
    }

    /**
     * Returns the SHA-256 hash of the blob that this object is representing.
     *
     * <p>For two objects of {@link AppSearchBlobHandle} to be considered equal, the {@code
     * packageName}, {@code database}, {@code namespace} and {@code digest} must be equal.
     */
    public @NonNull byte[] getSha256Digest() {
        return mSha256Digest;
    }

    /**
     * Returns the package name indicating the owner app of the blob that this object is
     * representing.
     *
     * <p>For two objects of {@link AppSearchBlobHandle} to be considered equal, the {@code
     * packageName}, {@code database}, {@code namespace} and {@code digest} must be equal.
     */
    public @NonNull String getPackageName() {
        return mPackageName;
    }

    /**
     * Returns the name of database stored the blob that this object is representing.
     *
     * <p>For two objects of {@link AppSearchBlobHandle} to be considered equal, the {@code
     * packageName}, {@code database}, {@code namespace} and {@code digest} must be equal.
     */
    public @NonNull String getDatabaseName() {
        return mDatabaseName;
    }

    /**
     * Returns the app-defined namespace this blob resides in.
     *
     * <p>For two objects of {@link AppSearchBlobHandle} to be considered equal, the {@code
     * packageName}, {@code database}, {@code namespace} and {@code digest} must be equal.
     */
    public @NonNull String getNamespace() {
        return mNamespace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppSearchBlobHandle)) return false;

        AppSearchBlobHandle that = (AppSearchBlobHandle) o;
        if (!Arrays.equals(mSha256Digest, that.mSha256Digest)) return false;
        return mPackageName.equals(that.mPackageName)
                && mDatabaseName.equals(that.mDatabaseName)
                && mNamespace.equals(that.mNamespace);
    }

    @Override
    public int hashCode() {
        if (mHashCode == null) {
            mHashCode =
                    Objects.hash(
                            Arrays.hashCode(mSha256Digest),
                            mPackageName,
                            mDatabaseName,
                            mNamespace);
        }
        return mHashCode;
    }

    @Override
    public @NonNull String toString() {
        IndentingStringBuilder builder = new IndentingStringBuilder();
        builder.append("{\n");
        builder.increaseIndentLevel();
        builder.append("packageName: \"").append(mPackageName).append("\",\n");
        builder.append("databaseName: \"").append(mDatabaseName).append("\",\n");
        builder.append("namespace: \"").append(mNamespace).append("\",\n");
        builder.append("digest: \"");
        for (byte b : mSha256Digest) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                builder.append('0');
            }
            builder.append(hex);
        }
        builder.append("\",\n").decreaseIndentLevel();
        builder.append("}");

        return builder.toString();
    }

    /**
     * Create a new AppSearch blob identifier with given digest, package, database and namespace.
     *
     * <p>The package name and database name indicated where this blob will be stored. To write,
     * commit or read this blob via {@link AppSearchSession}, it must match the package name and
     * database name of {@link AppSearchSession}.
     *
     * <p>For two objects of {@link AppSearchBlobHandle} to be considered equal, the {@code
     * packageName}, {@code database}, {@code namespace} and {@code digest} must be equal.
     *
     * @param digest The SHA-256 hash of the blob this is representing.
     * @param packageName The package name of the owner of this Blob.
     * @param databaseName The database name of this blob to stored into.
     * @param namespace The namespace of this blob resides in.
     * @return a new instance of {@link AppSearchBlobHandle} object.
     */
    public static @NonNull AppSearchBlobHandle createWithSha256(
            @NonNull byte[] digest,
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull String namespace) {
        Objects.requireNonNull(digest);
        Preconditions.checkArgument(
                digest.length == SHA_256_DIGEST_BYTE_LENGTH, "The digest is not a SHA-256 digest");
        Objects.requireNonNull(packageName);
        Objects.requireNonNull(databaseName);
        Objects.requireNonNull(namespace);
        return new AppSearchBlobHandle(digest, packageName, databaseName, namespace);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        AppSearchBlobHandleCreator.writeToParcel(this, dest, flags);
    }
}
