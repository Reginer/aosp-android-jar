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

package com.android.server.location.gnss;

import static android.location.LocationManager.GPS_PROVIDER;

import static com.android.internal.util.ConcurrentUtils.DIRECT_EXECUTOR;
import static com.android.server.location.LocationPermissions.PERMISSION_FINE;
import static com.android.server.location.gnss.GnssManagerService.TAG;

import android.annotation.Nullable;
import android.location.LocationManagerInternal;
import android.location.LocationManagerInternal.ProviderEnabledListener;
import android.location.util.identity.CallerIdentity;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Process;
import android.util.ArraySet;

import com.android.internal.util.Preconditions;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.location.injector.AppForegroundHelper;
import com.android.server.location.injector.Injector;
import com.android.server.location.injector.LocationPermissionsHelper;
import com.android.server.location.injector.PackageResetHelper;
import com.android.server.location.injector.SettingsHelper;
import com.android.server.location.injector.UserInfoHelper;
import com.android.server.location.injector.UserInfoHelper.UserListener;
import com.android.server.location.listeners.BinderListenerRegistration;
import com.android.server.location.listeners.ListenerMultiplexer;

import java.util.Collection;
import java.util.Objects;

/**
 * Manager for all GNSS related listeners. This class handles deactivating listeners that do not
 * belong to the current user, that do not have the appropriate permissions, or that are not
 * currently in the foreground. It will also disable listeners if the GNSS provider is disabled.
 * Listeners must be registered with the associated IBinder as the key, if the IBinder dies, the
 * registration will automatically be removed.
 *
 * @param <TRequest>            request type
 * @param <TListener>           listener type
 * @param <TMergedRegistration> merged registration type
 */
public abstract class GnssListenerMultiplexer<TRequest, TListener extends IInterface,
        TMergedRegistration> extends
        ListenerMultiplexer<IBinder, TListener,
                GnssListenerMultiplexer<TRequest, TListener, TMergedRegistration>
                        .GnssListenerRegistration, TMergedRegistration> {

    /**
     * Registration object for GNSS listeners.
     */
    protected class GnssListenerRegistration extends
            BinderListenerRegistration<IBinder, TListener> {

        private final TRequest mRequest;
        private final CallerIdentity mIdentity;

        // we store these values because we don't trust the listeners not to give us dupes, not to
        // spam us, and because checking the values may be more expensive
        private boolean mForeground;
        private boolean mPermitted;

        protected GnssListenerRegistration(TRequest request, CallerIdentity identity,
                TListener listener) {
            super(identity.isMyProcess() ? FgThread.getExecutor() : DIRECT_EXECUTOR, listener);
            mRequest = request;
            mIdentity = identity;
        }

        public final TRequest getRequest() {
            return mRequest;
        }

        public final CallerIdentity getIdentity() {
            return mIdentity;
        }

        @Override
        public String getTag() {
            return TAG;
        }

        @Override
        protected GnssListenerMultiplexer<TRequest, TListener, TMergedRegistration> getOwner() {
            return GnssListenerMultiplexer.this;
        }

        @Override
        protected IBinder getBinderFromKey(IBinder key) {
            return key;
        }

        /**
         * Returns true if this registration is currently in the foreground.
         */
        public boolean isForeground() {
            return mForeground;
        }

        boolean isPermitted() {
            return mPermitted;
        }

        @Override
        protected void onRegister() {
            super.onRegister();

            mPermitted = mLocationPermissionsHelper.hasLocationPermissions(PERMISSION_FINE,
                    mIdentity);
            mForeground = mAppForegroundHelper.isAppForeground(mIdentity.getUid());
        }

        boolean onLocationPermissionsChanged(@Nullable String packageName) {
            if (packageName == null || mIdentity.getPackageName().equals(packageName)) {
                return onLocationPermissionsChanged();
            }

            return false;
        }

        boolean onLocationPermissionsChanged(int uid) {
            if (mIdentity.getUid() == uid) {
                return onLocationPermissionsChanged();
            }

            return false;
        }

        private boolean onLocationPermissionsChanged() {
            boolean permitted = mLocationPermissionsHelper.hasLocationPermissions(PERMISSION_FINE,
                    mIdentity);
            if (permitted != mPermitted) {
                mPermitted = permitted;
                return true;
            }

            return false;
        }

        boolean onForegroundChanged(int uid, boolean foreground) {
            if (mIdentity.getUid() == uid && foreground != mForeground) {
                mForeground = foreground;
                return true;
            }

            return false;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(mIdentity);

            ArraySet<String> flags = new ArraySet<>(2);
            if (!mForeground) {
                flags.add("bg");
            }
            if (!mPermitted) {
                flags.add("na");
            }
            if (!flags.isEmpty()) {
                builder.append(" ").append(flags);
            }

            if (mRequest != null) {
                builder.append(" ").append(mRequest);
            }
            return builder.toString();
        }
    }

    protected final UserInfoHelper mUserInfoHelper;
    protected final SettingsHelper mSettingsHelper;
    protected final LocationPermissionsHelper mLocationPermissionsHelper;
    protected final AppForegroundHelper mAppForegroundHelper;
    protected final LocationManagerInternal mLocationManagerInternal;
    private final PackageResetHelper mPackageResetHelper;

    private final UserListener mUserChangedListener = this::onUserChanged;
    private final ProviderEnabledListener mProviderEnabledChangedListener =
            this::onProviderEnabledChanged;
    private final SettingsHelper.GlobalSettingChangedListener
            mBackgroundThrottlePackageWhitelistChangedListener =
            this::onBackgroundThrottlePackageAllowlistChanged;
    private final SettingsHelper.UserSettingChangedListener
            mLocationPackageBlacklistChangedListener =
            this::onLocationPackageDenylistChanged;
    private final LocationPermissionsHelper.LocationPermissionsListener
            mLocationPermissionsListener =
            new LocationPermissionsHelper.LocationPermissionsListener() {
                @Override
                public void onLocationPermissionsChanged(@Nullable String packageName) {
                    GnssListenerMultiplexer.this.onLocationPermissionsChanged(packageName);
                }

                @Override
                public void onLocationPermissionsChanged(int uid) {
                    GnssListenerMultiplexer.this.onLocationPermissionsChanged(uid);
                }
            };
    private final AppForegroundHelper.AppForegroundListener mAppForegroundChangedListener =
            this::onAppForegroundChanged;
    private final PackageResetHelper.Responder mPackageResetResponder =
            new PackageResetHelper.Responder() {
                @Override
                public void onPackageReset(String packageName) {
                    GnssListenerMultiplexer.this.onPackageReset(packageName);
                }

                @Override
                public boolean isResetableForPackage(String packageName) {
                    return GnssListenerMultiplexer.this.isResetableForPackage(packageName);
                }
            };

    protected GnssListenerMultiplexer(Injector injector) {
        mUserInfoHelper = injector.getUserInfoHelper();
        mSettingsHelper = injector.getSettingsHelper();
        mLocationPermissionsHelper = injector.getLocationPermissionsHelper();
        mAppForegroundHelper = injector.getAppForegroundHelper();
        mPackageResetHelper = injector.getPackageResetHelper();
        mLocationManagerInternal = Objects.requireNonNull(
                LocalServices.getService(LocationManagerInternal.class));
    }

    /**
     * May be overridden by subclasses to return whether the service is supported or not. This value
     * should never change for the lifetime of the multiplexer. If the service is unsupported, all
     * registrations will be treated as inactive and the backing service will never be registered.
     *
     */
    public boolean isSupported() {
        return true;
    }

    /**
     * Adds a listener with the given identity.
     */
    protected void addListener(CallerIdentity identity, TListener listener) {
        addListener(null, identity, listener);
    }

    /**
     * Adds a listener with the given identity and request.
     */
    protected void addListener(TRequest request, CallerIdentity callerIdentity,
            TListener listener) {
        final long identity = Binder.clearCallingIdentity();
        try {
            putRegistration(listener.asBinder(),
                    createRegistration(request, callerIdentity, listener));
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /**
     * May be overridden by subclasses to change the registration type.
     */
    protected GnssListenerRegistration createRegistration(TRequest request,
            CallerIdentity callerIdentity, TListener listener) {
        return new GnssListenerRegistration(request, callerIdentity, listener);
    }

    /**
     * Removes the given listener.
     */
    public void removeListener(TListener listener) {
        final long identity = Binder.clearCallingIdentity();
        try {
            removeRegistration(listener.asBinder());
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @Override
    protected boolean isActive(GnssListenerRegistration registration) {
        if (!isSupported()) {
            return false;
        }

        CallerIdentity identity = registration.getIdentity();
        return registration.isPermitted()
                && (registration.isForeground() || isBackgroundRestrictionExempt(identity))
                && isActive(identity);
    }

    private boolean isActive(CallerIdentity identity) {
        if (identity.isSystemServer()) {
            if (!mLocationManagerInternal.isProviderEnabledForUser(GPS_PROVIDER,
                    mUserInfoHelper.getCurrentUserId())) {
                return false;
            }
        } else {
            if (!mLocationManagerInternal.isProviderEnabledForUser(GPS_PROVIDER,
                    identity.getUserId())) {
                return false;
            }
            if (!mUserInfoHelper.isVisibleUserId(identity.getUserId())) {
                return false;
            }
            if (mSettingsHelper.isLocationPackageBlacklisted(identity.getUserId(),
                    identity.getPackageName())) {
                return false;
            }
        }

        return true;
    }

    private boolean isBackgroundRestrictionExempt(CallerIdentity identity) {
        if (identity.getUid() == Process.SYSTEM_UID) {
            return true;
        }

        if (mSettingsHelper.getBackgroundThrottlePackageWhitelist().contains(
                identity.getPackageName())) {
            return true;
        }

        return mLocationManagerInternal.isProvider(null, identity);
    }

    // this provides a default implementation for all further subclasses which assumes that there is
    // never an associated request object, and thus nothing interesting to merge. the majority of
    // gnss listener multiplexers do not current have associated requests, and the ones that do can
    // override this implementation.
    protected TMergedRegistration mergeRegistrations(
            Collection<GnssListenerRegistration> gnssListenerRegistrations) {
        if (Build.IS_DEBUGGABLE) {
            for (GnssListenerRegistration registration : gnssListenerRegistrations) {
                Preconditions.checkState(registration.getRequest() == null);
            }
        }

        return null;
    }

    @Override
    protected void onRegister() {
        if (!isSupported()) {
            return;
        }

        mUserInfoHelper.addListener(mUserChangedListener);
        mLocationManagerInternal.addProviderEnabledListener(GPS_PROVIDER,
                mProviderEnabledChangedListener);
        mSettingsHelper.addOnBackgroundThrottlePackageWhitelistChangedListener(
                mBackgroundThrottlePackageWhitelistChangedListener);
        mSettingsHelper.addOnLocationPackageBlacklistChangedListener(
                mLocationPackageBlacklistChangedListener);
        mLocationPermissionsHelper.addListener(mLocationPermissionsListener);
        mAppForegroundHelper.addListener(mAppForegroundChangedListener);
        mPackageResetHelper.register(mPackageResetResponder);
    }

    @Override
    protected void onUnregister() {
        if (!isSupported()) {
            return;
        }

        mUserInfoHelper.removeListener(mUserChangedListener);
        mLocationManagerInternal.removeProviderEnabledListener(GPS_PROVIDER,
                mProviderEnabledChangedListener);
        mSettingsHelper.removeOnBackgroundThrottlePackageWhitelistChangedListener(
                mBackgroundThrottlePackageWhitelistChangedListener);
        mSettingsHelper.removeOnLocationPackageBlacklistChangedListener(
                mLocationPackageBlacklistChangedListener);
        mLocationPermissionsHelper.removeListener(mLocationPermissionsListener);
        mAppForegroundHelper.removeListener(mAppForegroundChangedListener);
        mPackageResetHelper.unregister(mPackageResetResponder);
    }

    private void onUserChanged(int userId, int change) {
        // current user changes affect whether system server location requests are allowed to access
        // location, and visibility changes affect whether any given user may access location.
        if (change == UserListener.CURRENT_USER_CHANGED
                || change == UserListener.USER_VISIBILITY_CHANGED) {
            updateRegistrations(registration -> registration.getIdentity().getUserId() == userId);
        }
    }

    private void onProviderEnabledChanged(String provider, int userId, boolean enabled) {
        Preconditions.checkState(GPS_PROVIDER.equals(provider));
        updateRegistrations(registration -> registration.getIdentity().getUserId() == userId);
    }

    private void onBackgroundThrottlePackageAllowlistChanged() {
        updateRegistrations(registration -> true);
    }

    private void onLocationPackageDenylistChanged(int userId) {
        updateRegistrations(registration -> registration.getIdentity().getUserId() == userId);
    }

    private void onLocationPermissionsChanged(@Nullable String packageName) {
        updateRegistrations(registration -> registration.onLocationPermissionsChanged(packageName));
    }

    private void onLocationPermissionsChanged(int uid) {
        updateRegistrations(registration -> registration.onLocationPermissionsChanged(uid));
    }

    private void onAppForegroundChanged(int uid, boolean foreground) {
        updateRegistrations(registration -> registration.onForegroundChanged(uid, foreground));
    }

    private void onPackageReset(String packageName) {
        updateRegistrations(
                registration -> {
                    if (registration.getIdentity().getPackageName().equals(
                            packageName)) {
                        registration.remove();
                    }

                    return false;
                });
    }

    private boolean isResetableForPackage(String packageName) {
        // invoked to find out if the given package has any state that can be "force quit"
        return findRegistration(
                registration -> registration.getIdentity().getPackageName().equals(packageName));
    }

    @Override
    protected String getServiceState() {
        if (!isSupported()) {
            return "unsupported";
        } else {
            return super.getServiceState();
        }
    }
}
