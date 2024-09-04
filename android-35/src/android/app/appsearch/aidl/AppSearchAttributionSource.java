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

package android.app.appsearch.aidl;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresApi;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.content.AttributionSource;
import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;

import com.android.internal.annotations.VisibleForTesting;

import java.util.Objects;

/**
 * Compatibility version of AttributionSource.
 *
 * <p>Refactor AttributionSource to work on older API levels. For Android S+, this class maintains
 * the original implementation of AttributionSource methods. However, for Android R-, this class
 * creates a new implementation. Replace calls to AttributionSource with AppSearchAttributionSource.
 * For a given Context, replace calls to getAttributionSource with createAttributionSource.
 *
 * @hide
 */
@SafeParcelable.Class(creator = "AppSearchAttributionSourceCreator")
public final class AppSearchAttributionSource extends AbstractSafeParcelable {
    @NonNull
    public static final Parcelable.Creator<AppSearchAttributionSource> CREATOR =
            new AppSearchAttributionSourceCreator();

    @NonNull private final Compat mCompat;

    @Nullable
    @Field(id = 1, getter = "getAttributionSource")
    private final AttributionSource mAttributionSource;

    @NonNull
    @Field(id = 2, getter = "getPackageName")
    private final String mCallingPackageName;

    @Field(id = 3, getter = "getUid")
    private final int mCallingUid;

    @Field(id = 4, getter = "getPid")
    private int mCallingPid;

    private static final int INVALID_PID = -1;

    /**
     * Constructs an instance of AppSearchAttributionSource for AbstractSafeParcelable.
     *
     * @param attributionSource The attribution source that is accessing permission protected data.
     * @param callingPackageName The package that is accessing the permission protected data.
     * @param callingUid The UID that is accessing the permission protected data.
     */
    @Constructor
    AppSearchAttributionSource(
            @Param(id = 1) @Nullable AttributionSource attributionSource,
            @Param(id = 2) @NonNull String callingPackageName,
            @Param(id = 3) int callingUid,
            @Param(id = 4) int callingPid) {
        mAttributionSource = attributionSource;
        mCallingPackageName = Objects.requireNonNull(callingPackageName);
        mCallingUid = callingUid;
        mCallingPid = callingPid;
        if (VERSION.SDK_INT >= Build.VERSION_CODES.S && mAttributionSource != null) {
            mCompat = new Api31Impl(mAttributionSource, mCallingPid);
        } else {
            // If this object is being constructed as part of a oneway Binder call, getCallingPid
            // will return 0 instead of the true PID. In that case, invalidate the PID by setting it
            // to INVALID_PID (-1).
            final int callingPidFromBinder = Binder.getCallingPid();
            if (callingPidFromBinder == 0) {
                mCallingPid = INVALID_PID;
            }
            Api19Impl impl = new Api19Impl(mCallingPackageName, mCallingUid, mCallingPid);
            impl.enforceCallingUid();
            impl.enforceCallingPid();
            mCompat = impl;
        }
    }

    /**
     * Constructs an instance of AppSearchAttributionSource.
     *
     * @param compat The compat version that provides AttributionSource implementation on lower API
     *     levels.
     */
    private AppSearchAttributionSource(@NonNull Compat compat) {
        mCompat = Objects.requireNonNull(compat);
        mAttributionSource = mCompat.getAttributionSource();
        mCallingPackageName = mCompat.getPackageName();
        mCallingUid = mCompat.getUid();
        mCallingPid = mCompat.getPid();
    }

    /**
     * Constructs an instance of AppSearchAttributionSource for testing.
     *
     * @param callingPackageName The package that is accessing the permission protected data.
     * @param callingUid The UID that is accessing the permission protected data.
     */
    @VisibleForTesting
    public AppSearchAttributionSource(
            @NonNull String callingPackageName, int callingUid, int callingPid) {
        mCallingPackageName = Objects.requireNonNull(callingPackageName);
        mCallingUid = callingUid;
        mCallingPid = callingPid;

        if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // This constructor is only used in unit test, AttributionSource#setPid is only
            // available on 34+.
            AttributionSource.Builder attributionSourceBuilder =
                    new AttributionSource.Builder(mCallingUid).setPackageName(mCallingPackageName);
            if (VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE) {
                attributionSourceBuilder.setPid(callingPid);
            }
            mAttributionSource = attributionSourceBuilder.build();
            mCompat = new Api31Impl(mAttributionSource, mCallingPid);
        } else {
            mAttributionSource = null;
            mCompat = new Api19Impl(mCallingPackageName, mCallingUid, mCallingPid);
        }
    }

    /**
     * Provides a backward-compatible wrapper for AttributionSource.
     *
     * <p>This method is not supported on devices running SDK <= 30(R) since the AttributionSource
     * class will not be available.
     *
     * @param attributionSource AttributionSource class to wrap, must not be null
     * @return wrapped class
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @NonNull
    private static AppSearchAttributionSource toAppSearchAttributionSource(
            @NonNull AttributionSource attributionSource, int pid) {
        return new AppSearchAttributionSource(new Api31Impl(attributionSource, pid));
    }

    /**
     * Provides a backward-compatible wrapper for AttributionSource.
     *
     * <p>This method is not supported on devices running SDK <= 19(H) since the AttributionSource
     * class will not be available.
     *
     * @param packageName The package name to wrap, must not be null
     * @param uid The uid to wrap
     * @return wrapped class
     */
    private static AppSearchAttributionSource toAppSearchAttributionSource(
            @NonNull String packageName, int uid, int pid) {
        return new AppSearchAttributionSource(new Api19Impl(packageName, uid, pid));
    }

    /**
     * Create an instance of AppSearchAttributionSource.
     *
     * @param context Context the application is running on.
     */
    public static AppSearchAttributionSource createAttributionSource(
            @NonNull Context context, int callingPid) {
        if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return toAppSearchAttributionSource(context.getAttributionSource(), callingPid);
        }

        return toAppSearchAttributionSource(context.getPackageName(), Process.myUid(), callingPid);
    }

    /** Return AttributionSource on Android S+ and return null on Android R-. */
    @Nullable
    public AttributionSource getAttributionSource() {
        return mCompat.getAttributionSource();
    }

    @NonNull
    public String getPackageName() {
        return mCompat.getPackageName();
    }

    public int getUid() {
        return mCompat.getUid();
    }

    public int getPid() {
        return mCompat.getPid();
    }

    @Override
    public int hashCode() {
        if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AttributionSource attributionSource =
                    Objects.requireNonNull(mCompat.getAttributionSource());
            return attributionSource.hashCode();
        }

        return Objects.hash(mCompat.getUid(), mCompat.getPackageName());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null || !(o instanceof AppSearchAttributionSource)) {
            return false;
        }

        AppSearchAttributionSource that = (AppSearchAttributionSource) o;
        if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AttributionSource thisAttributionSource =
                    Objects.requireNonNull(mCompat.getAttributionSource());
            AttributionSource thatAttributionSource =
                    Objects.requireNonNull(that.getAttributionSource());
            return thisAttributionSource.equals(thatAttributionSource)
                    && (that.getPid() == mCompat.getPid());
        }

        return (Objects.equals(mCompat.getPackageName(), that.getPackageName())
                && (mCompat.getUid() == that.getUid())
                && mCompat.getPid() == that.getPid());
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        AppSearchAttributionSourceCreator.writeToParcel(this, dest, flags);
    }

    /** Compat class for AttributionSource to provide implementation for lower API levels. */
    private interface Compat {
        /** The package that is accessing the permission protected data. */
        @NonNull
        String getPackageName();

        /** The attribution source of the app accessing the permission protected data. */
        @Nullable
        AttributionSource getAttributionSource();

        /** The UID that is accessing the permission protected data. */
        int getUid();

        /** The PID that is accessing the permission protected data. */
        int getPid();
    }

    @RequiresApi(VERSION_CODES.S)
    private static final class Api31Impl implements Compat {

        private final AttributionSource mAttributionSource;
        private final int mPid;

        /**
         * Creates a new implementation for AppSearchAttributionSource's Compat for API levels 31+.
         *
         * @param attributionSource The attribution source that is accessing permission protected
         *     data.
         */
        Api31Impl(@NonNull AttributionSource attributionSource, int pid) {
            mAttributionSource = attributionSource;
            mPid = pid;
        }

        @Override
        @NonNull
        public String getPackageName() {
            // The {@link AttributionSource} in the constructor is set using
            // {@link Context#getAttributionSource} and not using the Builder. The
            // packageName returned from {@link AttributionSource#getPackageName} can be null as
            // AttributionSource can use either uid and package name to determine who has access
            // to the data, so either one of them can be null but not both. It is a common practice
            // to use {@link AttributionSource#getPackageName} without any known issues/bugs. If
            // we ever receive a null here we will throw a NullPointerException.
            return Objects.requireNonNull(mAttributionSource.getPackageName());
        }

        @Nullable
        @Override
        public AttributionSource getAttributionSource() {
            return mAttributionSource;
        }

        @Override
        public int getUid() {
            return mAttributionSource.getUid();
        }

        @Override
        public int getPid() {
            return mPid;
        }
    }

    private static class Api19Impl implements Compat {

        @NonNull private final String mPackageName;
        private final int mUid;
        private final int mPid;

        /**
         * Creates a new implementation for AppSearchAttributionSource's Compat for API levels 19+.
         *
         * @param packageName The package name that is accessing permission protected data.
         * @param uid The uid that is accessing permission protected data.
         */
        Api19Impl(@NonNull String packageName, int uid, int pid) {
            mPackageName = Objects.requireNonNull(packageName);
            mUid = uid;
            mPid = pid;
        }

        @Override
        @NonNull
        public String getPackageName() {
            return mPackageName;
        }

        @Nullable
        @Override
        public AttributionSource getAttributionSource() {
            // AttributionSource class was added in Api level 31 and hence it is unavailable on API
            // levels lower than 31. This class is used in AppSearch to get package name, uid etc,
            // this implementation has util methods for getPackageName, getUid etc which could
            // be used instead.
            return null;
        }

        @Override
        public int getUid() {
            return mUid;
        }

        @Override
        public int getPid() {
            return mPid;
        }

        /**
         * If you are handling an IPC and you don't trust the caller you need to validate whether
         * the attribution source is one for the calling app to prevent the caller to pass you a
         * source from another app without including themselves in the attribution chain.
         *
         * @throws SecurityException if the attribution source cannot be trusted to be from the
         *     caller.
         */
        private void enforceCallingUid() {
            if (!checkCallingUid()) {
                int callingUid = Binder.getCallingUid();
                throw new SecurityException(
                        "Calling uid: " + callingUid + " doesn't match source uid: " + mUid);
            }
            // The verification for calling package happens in the service during API call.
        }

        /**
         * If you are handling an IPC and you don't trust the caller you need to validate whether
         * the attribution source is one for the calling app to prevent the caller to pass you a
         * source from another app without including themselves in the attribution chain.
         *
         * @return if the attribution source cannot be trusted to be from the caller.
         */
        private boolean checkCallingUid() {
            final int callingUid = Binder.getCallingUid();
            if (callingUid != mUid) {
                return false;
            }
            // The verification for calling package happens in the service during API call.
            return true;
        }

        /**
         * Validate that the pid being claimed for the calling app is not spoofed.
         *
         * <p>Note that the PID may be unavailable, for example if we're in a oneway Binder call. In
         * this case, calling enforceCallingPid is guaranteed to fail. The caller should anticipate
         * this.
         *
         * @throws SecurityException if the attribution source cannot be trusted to be from the
         *     caller.
         */
        private void enforceCallingPid() {
            if (!checkCallingPid()) {
                if (Binder.getCallingPid() == 0) {
                    throw new SecurityException(
                            "Calling pid unavailable due to oneway Binder " + "call.");
                } else {
                    throw new SecurityException(
                            "Calling pid: "
                                    + Binder.getCallingPid()
                                    + " doesn't match source pid: "
                                    + mPid);
                }
            }
        }

        /**
         * Validate that the pid being claimed for the calling app is not spoofed
         *
         * @return if the attribution source cannot be trusted to be from the caller.
         */
        private boolean checkCallingPid() {
            final int callingPid = Binder.getCallingPid();
            if (mPid != INVALID_PID && mPid != callingPid) {
                // Only call this on the binder thread. If a new thread is created to handle the
                // client request, Binder.getCallingPid() will return the thread's own pid.
                return false;
            }
            return true;
        }
    }
}
