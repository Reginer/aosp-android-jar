/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.annotation.NonNull;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.android.internal.annotations.VisibleForTesting;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

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
 * @hide
 **/
// TODO(b/187083878): factor out common code between this and NetworkFactoryImpl
class NetworkFactoryLegacyImpl extends Handler
        implements NetworkFactoryShim {
    private static final boolean DBG = NetworkFactory.DBG;
    private static final boolean VDBG = NetworkFactory.VDBG;

    // TODO(b/187082970): Replace CMD_* with Handler.post(() -> { ... }) since all the CMDs do is to
    //  run the tasks asynchronously on the Handler thread.

    /**
     * Pass a network request to the bearer.  If the bearer believes it can
     * satisfy the request it should connect to the network and create a
     * NetworkAgent.  Once the NetworkAgent is fully functional it will
     * register itself with ConnectivityService using registerNetworkAgent.
     * If the bearer cannot immediately satisfy the request (no network,
     * user disabled the radio, lower-scored network) it should remember
     * any NetworkRequests it may be able to satisfy in the future.  It may
     * disregard any that it will never be able to service, for example
     * those requiring a different bearer.
     * msg.obj = NetworkRequest
     * msg.arg1 = score - the score of the network currently satisfying this
     *            request.  If this bearer knows in advance it cannot
     *            exceed this score it should not try to connect, holding the request
     *            for the future.
     *            Note that subsequent events may give a different (lower
     *            or higher) score for this request, transmitted to each
     *            NetworkFactory through additional CMD_REQUEST_NETWORK msgs
     *            with the same NetworkRequest but an updated score.
     *            Also, network conditions may change for this bearer
     *            allowing for a better score in the future.
     * msg.arg2 = the ID of the NetworkProvider currently responsible for the
     *            NetworkAgent handling this request, or NetworkProvider.ID_NONE if none.
     */
    public static final int CMD_REQUEST_NETWORK = 1;

    /**
     * Cancel a network request
     * msg.obj = NetworkRequest
     */
    public static final int CMD_CANCEL_REQUEST = 2;

    /**
     * Internally used to set our best-guess score.
     * msg.arg1 = new score
     */
    private static final int CMD_SET_SCORE = 3;

    /**
     * Internally used to set our current filter for coarse bandwidth changes with
     * technology changes.
     * msg.obj = new filter
     */
    private static final int CMD_SET_FILTER = 4;

    final Context mContext;
    final NetworkFactory mParent;

    private final Map<NetworkRequest, NetworkRequestInfo> mNetworkRequests =
            new LinkedHashMap<>();

    private int mScore;
    NetworkCapabilities mCapabilityFilter;

    NetworkProvider mProvider = null;

    NetworkFactoryLegacyImpl(NetworkFactory parent, Looper looper, Context context,
            NetworkCapabilities filter) {
        super(looper);
        mParent = parent;
        mContext = context;
        mCapabilityFilter = filter;
    }

    /* Registers this NetworkFactory with the system. May only be called once per factory. */
    @Override public void register(final String logTag) {
        if (mProvider != null) {
            throw new IllegalStateException("A NetworkFactory must only be registered once");
        }
        if (DBG) mParent.log("Registering NetworkFactory");

        mProvider = new NetworkProvider(mContext, NetworkFactoryLegacyImpl.this.getLooper(),
                logTag) {
            @Override
            public void onNetworkRequested(@NonNull NetworkRequest request, int score,
                    int servingProviderId) {
                handleAddRequest(request, score, servingProviderId);
            }

            @Override
            public void onNetworkRequestWithdrawn(@NonNull NetworkRequest request) {
                handleRemoveRequest(request);
            }
        };

        ((ConnectivityManager) mContext.getSystemService(
            Context.CONNECTIVITY_SERVICE)).registerNetworkProvider(mProvider);
    }

    /** Unregisters this NetworkFactory. After this call, the object can no longer be used. */
    @Override public void terminate() {
        if (mProvider == null) {
            throw new IllegalStateException("This NetworkFactory was never registered");
        }
        if (DBG) mParent.log("Unregistering NetworkFactory");

        ((ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
            .unregisterNetworkProvider(mProvider);

        // Remove all pending messages, since this object cannot be reused. Any message currently
        // being processed will continue to run.
        removeCallbacksAndMessages(null);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case CMD_REQUEST_NETWORK: {
                handleAddRequest((NetworkRequest) msg.obj, msg.arg1, msg.arg2);
                break;
            }
            case CMD_CANCEL_REQUEST: {
                handleRemoveRequest((NetworkRequest) msg.obj);
                break;
            }
            case CMD_SET_SCORE: {
                handleSetScore(msg.arg1);
                break;
            }
            case CMD_SET_FILTER: {
                handleSetFilter((NetworkCapabilities) msg.obj);
                break;
            }
        }
    }

    private static class NetworkRequestInfo {
        public final NetworkRequest request;
        public int score;
        public boolean requested; // do we have a request outstanding, limited by score
        public int providerId;

        NetworkRequestInfo(NetworkRequest request, int score, int providerId) {
            this.request = request;
            this.score = score;
            this.requested = false;
            this.providerId = providerId;
        }

        @Override
        public String toString() {
            return "{" + request + ", score=" + score + ", requested=" + requested + "}";
        }
    }

    /**
     * Add a NetworkRequest that the bearer may want to attempt to satisfy.
     * @see #CMD_REQUEST_NETWORK
     *
     * @param request the request to handle.
     * @param score the score of the NetworkAgent currently satisfying this request.
     * @param servingProviderId the ID of the NetworkProvider that created the NetworkAgent
     *        currently satisfying this request.
     */
    @VisibleForTesting
    protected void handleAddRequest(NetworkRequest request, int score, int servingProviderId) {
        NetworkRequestInfo n = mNetworkRequests.get(request);
        if (n == null) {
            if (DBG) {
                mParent.log("got request " + request + " with score " + score
                        + " and providerId " + servingProviderId);
            }
            n = new NetworkRequestInfo(request, score, servingProviderId);
            mNetworkRequests.put(n.request, n);
        } else {
            if (VDBG) {
                mParent.log("new score " + score + " for existing request " + request
                        + " and providerId " + servingProviderId);
            }
            n.score = score;
            n.providerId = servingProviderId;
        }
        if (VDBG) mParent.log("  my score=" + mScore + ", my filter=" + mCapabilityFilter);

        evalRequest(n);
    }

    private void handleRemoveRequest(NetworkRequest request) {
        NetworkRequestInfo n = mNetworkRequests.get(request);
        if (n != null) {
            mNetworkRequests.remove(request);
            if (n.requested) mParent.releaseNetworkFor(n.request);
        }
    }

    private void handleSetScore(int score) {
        mScore = score;
        evalRequests();
    }

    private void handleSetFilter(NetworkCapabilities netCap) {
        mCapabilityFilter = netCap;
        evalRequests();
    }

    /**
     * Overridable function to provide complex filtering.
     * Called for every request every time a new NetworkRequest is seen
     * and whenever the filterScore or filterNetworkCapabilities change.
     *
     * acceptRequest can be overridden to provide complex filter behavior
     * for the incoming requests
     *
     * For output, this class will call {@link NetworkFactory#needNetworkFor} and
     * {@link NetworkFactory#releaseNetworkFor} for every request that passes the filters.
     * If you don't need to see every request, you can leave the base
     * implementations of those two functions and instead override
     * {@link NetworkFactory#startNetwork} and {@link NetworkFactory#stopNetwork}.
     *
     * If you want to see every score fluctuation on every request, set
     * your score filter to a very high number and watch {@link NetworkFactory#needNetworkFor}.
     *
     * @return {@code true} to accept the request.
     */
    public boolean acceptRequest(NetworkRequest request) {
        return mParent.acceptRequest(request);
    }

    private void evalRequest(NetworkRequestInfo n) {
        if (VDBG) {
            mParent.log("evalRequest");
            mParent.log(" n.requests = " + n.requested);
            mParent.log(" n.score = " + n.score);
            mParent.log(" mScore = " + mScore);
            mParent.log(" request.providerId = " + n.providerId);
            mParent.log(" mProvider.id = " + mProvider.getProviderId());
        }
        if (shouldNeedNetworkFor(n)) {
            if (VDBG) mParent.log("  needNetworkFor");
            mParent.needNetworkFor(n.request);
            n.requested = true;
        } else if (shouldReleaseNetworkFor(n)) {
            if (VDBG) mParent.log("  releaseNetworkFor");
            mParent.releaseNetworkFor(n.request);
            n.requested = false;
        } else {
            if (VDBG) mParent.log("  done");
        }
    }

    private boolean shouldNeedNetworkFor(NetworkRequestInfo n) {
        // If this request is already tracked, it doesn't qualify for need
        return !n.requested
            // If the score of this request is higher or equal to that of this factory and some
            // other factory is responsible for it, then this factory should not track the request
            // because it has no hope of satisfying it.
            && (n.score < mScore || n.providerId == mProvider.getProviderId())
            // If this factory can't satisfy the capability needs of this request, then it
            // should not be tracked.
            && n.request.canBeSatisfiedBy(mCapabilityFilter)
            // Finally if the concrete implementation of the factory rejects the request, then
            // don't track it.
            && acceptRequest(n.request);
    }

    private boolean shouldReleaseNetworkFor(NetworkRequestInfo n) {
        // Don't release a request that's not tracked.
        return n.requested
            // The request should be released if it can't be satisfied by this factory. That
            // means either of the following conditions are met :
            // - Its score is too high to be satisfied by this factory and it's not already
            //   assigned to the factory
            // - This factory can't satisfy the capability needs of the request
            // - The concrete implementation of the factory rejects the request
            && ((n.score > mScore && n.providerId != mProvider.getProviderId())
                    || !n.request.canBeSatisfiedBy(mCapabilityFilter)
                    || !acceptRequest(n.request));
    }

    private void evalRequests() {
        for (NetworkRequestInfo n : mNetworkRequests.values()) {
            evalRequest(n);
        }
    }

    /**
     * Post a command, on this NetworkFactory Handler, to re-evaluate all
     * outstanding requests. Can be called from a factory implementation.
     */
    @Override public void reevaluateAllRequests() {
        post(this::evalRequests);
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
    @Override public void releaseRequestAsUnfulfillableByAnyFactory(NetworkRequest r) {
        post(() -> {
            if (DBG) mParent.log("releaseRequestAsUnfulfillableByAnyFactory: " + r);
            final NetworkProvider provider = mProvider;
            if (provider == null) {
                mParent.log("Ignoring attempt to release unregistered request as unfulfillable");
                return;
            }
            provider.declareNetworkRequestUnfulfillable(r);
        });
    }

    @Override public void setScoreFilter(int score) {
        sendMessage(obtainMessage(CMD_SET_SCORE, score, 0));
    }

    @Override public void setScoreFilter(@NonNull final NetworkScore score) {
        setScoreFilter(score.getLegacyInt());
    }

    @Override public void setCapabilityFilter(NetworkCapabilities netCap) {
        sendMessage(obtainMessage(CMD_SET_FILTER, new NetworkCapabilities(netCap)));
    }

    @Override public int getRequestCount() {
        return mNetworkRequests.size();
    }

    /* TODO: delete when all callers have migrated to NetworkProvider IDs. */
    @Override public int getSerialNumber() {
        return mProvider.getProviderId();
    }

    @Override public NetworkProvider getProvider() {
        return mProvider;
    }

    @Override public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.println(toString());
        for (NetworkRequestInfo n : mNetworkRequests.values()) {
            writer.println("  " + n);
        }
    }

    @Override public String toString() {
        return "providerId="
                + mProvider.getProviderId() + ", ScoreFilter="
                + mScore + ", Filter=" + mCapabilityFilter + ", requests="
                + mNetworkRequests.size();
    }
}
