/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed urnder the Apache License, Version 2.0 (the "License");
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
package android.net;

import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.annotation.TargetApi;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.OutcomeReceiver;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.system.OsConstants;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * A class allowing apps handling the {@link ConnectivityManager#ACTION_CAPTIVE_PORTAL_SIGN_IN}
 * activity to indicate to the system different outcomes of captive portal sign in.  This class is
 * passed as an extra named {@link ConnectivityManager#EXTRA_CAPTIVE_PORTAL} with the
 * {@code ACTION_CAPTIVE_PORTAL_SIGN_IN} activity.
 */
public class CaptivePortal implements Parcelable {
    /**
     * Response code from the captive portal application, indicating that the portal was dismissed
     * and the network should be re-validated.
     * @see ICaptivePortal#appResponse(int)
     * @see android.net.INetworkMonitor#notifyCaptivePortalAppFinished(int)
     * @hide
     */
    @SystemApi
    public static final int APP_RETURN_DISMISSED    = 0;
    /**
     * Response code from the captive portal application, indicating that the user did not login and
     * does not want to use the captive portal network.
     * @see ICaptivePortal#appResponse(int)
     * @see android.net.INetworkMonitor#notifyCaptivePortalAppFinished(int)
     * @hide
     */
    @SystemApi
    public static final int APP_RETURN_UNWANTED     = 1;
    /**
     * Response code from the captive portal application, indicating that the user does not wish to
     * login but wants to use the captive portal network as-is.
     * @see ICaptivePortal#appResponse(int)
     * @see android.net.INetworkMonitor#notifyCaptivePortalAppFinished(int)
     * @hide
     */
    @SystemApi
    public static final int APP_RETURN_WANTED_AS_IS = 2;
    /** Event offset of request codes from captive portal application. */
    private static final int APP_REQUEST_BASE = 100;
    /**
     * Request code from the captive portal application, indicating that the network condition may
     * have changed and the network should be re-validated.
     * @see ICaptivePortal#appRequest(int)
     * @see android.net.INetworkMonitor#forceReevaluation(int)
     * @hide
     */
    @SystemApi
    public static final int APP_REQUEST_REEVALUATION_REQUIRED = APP_REQUEST_BASE + 0;

    /**
     * Binder object used for tracking the lifetime of the process, so CS can perform cleanup if
     * the CaptivePortal app dies. This binder is not parcelled as part of this object. It is
     * created in the client process and sent to the server by setDelegateUid so that the server
     * can use it to register a death recipient.
     *
     */
    private final Binder mLifetimeBinder = new Binder();

    private final IBinder mBinder;

    /** @hide */
    public CaptivePortal(@NonNull IBinder binder) {
        mBinder = binder;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeStrongBinder(mBinder);
    }

    public static final @android.annotation.NonNull Parcelable.Creator<CaptivePortal> CREATOR
            = new Parcelable.Creator<CaptivePortal>() {
        @Override
        public CaptivePortal createFromParcel(Parcel in) {
            return new CaptivePortal(in.readStrongBinder());
        }

        @Override
        public CaptivePortal[] newArray(int size) {
            return new CaptivePortal[size];
        }
    };

    /**
     * Indicate to the system that the captive portal has been
     * dismissed.  In response the framework will re-evaluate the network's
     * connectivity and might take further action thereafter.
     */
    public void reportCaptivePortalDismissed() {
        try {
            ICaptivePortal.Stub.asInterface(mBinder).appResponse(APP_RETURN_DISMISSED);
        } catch (RemoteException e) {
        }
    }

    /**
     * Indicate to the system that the user does not want to pursue signing in to the
     * captive portal and the system should continue to prefer other networks
     * without captive portals for use as the default active data network.  The
     * system will not retest the network for a captive portal so as to avoid
     * disturbing the user with further sign in to network notifications.
     */
    public void ignoreNetwork() {
        try {
            ICaptivePortal.Stub.asInterface(mBinder).appResponse(APP_RETURN_UNWANTED);
        } catch (RemoteException e) {
        }
    }

    /**
     * Indicate to the system the user wants to use this network as is, even though
     * the captive portal is still in place.  The system will treat the network
     * as if it did not have a captive portal when selecting the network to use
     * as the default active data network. This may result in this network
     * becoming the default active data network, which could disrupt network
     * connectivity for apps because the captive portal is still in place.
     * @hide
     */
    @SystemApi
    public void useNetwork() {
        try {
            ICaptivePortal.Stub.asInterface(mBinder).appResponse(APP_RETURN_WANTED_AS_IS);
        } catch (RemoteException e) {
        }
    }

    /**
     * Request that the system reevaluates the captive portal status.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_STACK)
    public void reevaluateNetwork() {
        try {
            ICaptivePortal.Stub.asInterface(mBinder).appRequest(APP_REQUEST_REEVALUATION_REQUIRED);
        } catch (RemoteException e) {
        }
    }

    /**
     * Log a captive portal login event.
     * @param eventId one of the CAPTIVE_PORTAL_LOGIN_* constants in metrics_constants.proto.
     * @param packageName captive portal application package name.
     * @hide
     * @deprecated The event will not be logged in Android S and above. The
     * caller is migrating to statsd.
     */
    @Deprecated
    @SystemApi
    public void logEvent(int eventId, @NonNull String packageName) {
    }

    /**
     * Sets the UID of the app that is allowed to perform network traffic for captive
     * portal login.
     *
     * This app will be allowed to communicate directly on the captive
     * portal by binding to the {@link android.net.Network} extra passed in the
     * ACTION_CAPTIVE_PORTAL_SIGN_IN broadcast that contained this object.
     *
     * Communication will bypass network access restrictions such as VPNs and
     * Private DNS settings, so the delegated UID must be trusted to ensure that only
     * traffic intended for captive portal login binds to that network.
     *
     * By default, no UID is delegated. The delegation can be cleared by calling
     * this method again with {@link android.os.Process.INVALID_UID}. Only one UID can
     * be delegated at any given time.
     *
     * The operation is asynchronous. The uid is only guaranteed to have access when
     * the provided OutcomeReceiver is called.
     *
     * @hide
     */
    @RequiresPermission(NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK)
    // OutcomeReceiver is not available on R, but the mainline version of this
    // class is only available on S+.
    @TargetApi(Build.VERSION_CODES.S)
    public void setDelegateUid(int uid, @NonNull Executor executor,
            @NonNull final OutcomeReceiver<Void, ServiceSpecificException> receiver) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);
        try {
            ICaptivePortal.Stub.asInterface(mBinder).setDelegateUid(
                    uid,
                    mLifetimeBinder,
                    new IIntResultListener.Stub() {
                        @Override
                        public void onResult(int resultCode) {
                            if (resultCode != 0) {
                                final String msg = "Fail to set the delegate UID " + uid
                                        + ", error: " + OsConstants.errnoName(resultCode);
                                executor.execute(() -> {
                                    receiver.onError(new ServiceSpecificException(resultCode, msg));
                                });
                            } else {
                                executor.execute(() -> receiver.onResult(null));
                            }
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
