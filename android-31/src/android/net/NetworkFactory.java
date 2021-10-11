/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.net;

import static com.android.modules.utils.build.SdkLevel.isAtLeastS;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * A NetworkFactory is an entity that creates NetworkAgent objects.
 * The bearers register with ConnectivityService using {@link #register} and
 * their factory will start receiving scored NetworkRequests.  NetworkRequests
 * can be filtered 3 ways: by NetworkCapabilities, by score and more complexly by
 * overridden function.  All of these can be dynamic - changing NetworkCapabilities
 * or score forces re-evaluation of all current requests.
 *
 * If any requests pass the filter some overrideable functions will be called.
 * If the bearer only cares about very simple start/stopNetwork callbacks, those
 * functions can be overridden.  If the bearer needs more interaction, it can
 * override addNetworkRequest and removeNetworkRequest which will give it each
 * request that passes their current filters.
 *
 * This class is mostly a shim which delegates to one of two implementations depending
 * on the SDK level of the device it's running on.
 *
 * @hide
 **/
public class NetworkFactory {
    static final boolean DBG = true;
    static final boolean VDBG = false;

    final NetworkFactoryShim mImpl;

    private final String LOG_TAG;

    // Ideally the filter argument would be non-null, but null has historically meant to see
    // no requests and telephony passes null.
    public NetworkFactory(Looper looper, Context context, String logTag,
            @Nullable final NetworkCapabilities filter) {
        LOG_TAG = logTag;
        if (isAtLeastS()) {
            mImpl = new NetworkFactoryImpl(this, looper, context, filter);
        } else {
            mImpl = new NetworkFactoryLegacyImpl(this, looper, context, filter);
        }
    }

    // TODO : these two constants and the method are only used by telephony tests. Replace it in
    // the tests and remove them and the associated code.
    public static final int CMD_REQUEST_NETWORK = 1;
    public static final int CMD_CANCEL_REQUEST = 2;
    /** Like Handler#obtainMessage */
    @VisibleForTesting
    public Message obtainMessage(final int what, final int arg1, final int arg2,
            final @Nullable Object obj) {
        return mImpl.obtainMessage(what, arg1, arg2, obj);
    }

    // Called by BluetoothNetworkFactory
    public final Looper getLooper() {
        return mImpl.getLooper();
    }

    // Refcount for simple mode requests
    private int mRefCount = 0;

    /* Registers this NetworkFactory with the system. May only be called once per factory. */
    public void register() {
        mImpl.register(LOG_TAG);
    }

    /**
     * Registers this NetworkFactory with the system ignoring the score filter. This will let
     * the factory always see all network requests matching its capabilities filter.
     * May only be called once per factory.
     */
    public void registerIgnoringScore() {
        mImpl.registerIgnoringScore(LOG_TAG);
    }

    /** Unregisters this NetworkFactory. After this call, the object can no longer be used. */
    public void terminate() {
        mImpl.terminate();
    }

    protected final void reevaluateAllRequests() {
        mImpl.reevaluateAllRequests();
    }

    /**
     * Overridable function to provide complex filtering.
     * Called for every request every time a new NetworkRequest is seen
     * and whenever the filterScore or filterNetworkCapabilities change.
     *
     * acceptRequest can be overridden to provide complex filter behavior
     * for the incoming requests
     *
     * For output, this class will call {@link #needNetworkFor} and
     * {@link #releaseNetworkFor} for every request that passes the filters.
     * If you don't need to see every request, you can leave the base
     * implementations of those two functions and instead override
     * {@link #startNetwork} and {@link #stopNetwork}.
     *
     * If you want to see every score fluctuation on every request, set
     * your score filter to a very high number and watch {@link #needNetworkFor}.
     *
     * @return {@code true} to accept the request.
     */
    public boolean acceptRequest(@NonNull final NetworkRequest request) {
        return true;
    }

    /**
     * Can be called by a factory to release a request as unfulfillable: the request will be
     * removed, and the caller will get a
     * {@link ConnectivityManager.NetworkCallback#onUnavailable()} callback after this function
     * returns.
     *
     * Note: this should only be called by factory which KNOWS that it is the ONLY factory which
     * is able to fulfill this request!
     */
    protected void releaseRequestAsUnfulfillableByAnyFactory(NetworkRequest r) {
        mImpl.releaseRequestAsUnfulfillableByAnyFactory(r);
    }

    // override to do simple mode (request independent)
    protected void startNetwork() { }
    protected void stopNetwork() { }

    // override to do fancier stuff
    protected void needNetworkFor(@NonNull final NetworkRequest networkRequest) {
        if (++mRefCount == 1) startNetwork();
    }

    protected void releaseNetworkFor(@NonNull final NetworkRequest networkRequest) {
        if (--mRefCount == 0) stopNetwork();
    }

    /**
     * @deprecated this method was never part of the API (system or public) and is only added
     *   for migration of existing clients.
     */
    @Deprecated
    public void setScoreFilter(final int score) {
        mImpl.setScoreFilter(score);
    }

    /**
     * Set a score filter for this factory.
     *
     * This should include the transports the factory knows its networks will have, and
     * an optimistic view of the attributes it may have. This does not commit the factory
     * to being able to bring up such a network ; it only lets it avoid hearing about
     * requests that it has no chance of fulfilling.
     *
     * @param score the filter
     */
    public void setScoreFilter(@NonNull final NetworkScore score) {
        mImpl.setScoreFilter(score);
    }

    public void setCapabilityFilter(NetworkCapabilities netCap) {
        mImpl.setCapabilityFilter(netCap);
    }

    @VisibleForTesting
    protected int getRequestCount() {
        return mImpl.getRequestCount();
    }

    public int getSerialNumber() {
        return mImpl.getSerialNumber();
    }

    public NetworkProvider getProvider() {
        return mImpl.getProvider();
    }

    protected void log(String s) {
        Log.d(LOG_TAG, s);
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        mImpl.dump(fd, writer, args);
    }

    @Override
    public String toString() {
        return "{" + LOG_TAG + " " + mImpl.toString() + "}";
    }
}
