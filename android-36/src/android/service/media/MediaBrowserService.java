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

package android.service.media;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.app.Service;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.media.browse.MediaBrowser;
import android.media.browse.MediaBrowserUtils;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.MediaSessionManager.RemoteUserInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;

import com.android.media.flags.Flags;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for media browser services.
 * <p>
 * Media browser services enable applications to browse media content provided by an application
 * and ask the application to start playing it. They may also be used to control content that
 * is already playing by way of a {@link MediaSession}.
 * </p>
 *
 * To extend this class, you must declare the service in your manifest file with
 * an intent filter with the {@link #SERVICE_INTERFACE} action.
 *
 * For example:
 * </p><pre>
 * &lt;service android:name=".MyMediaBrowserService"
 *          android:label="&#64;string/service_name" >
 *     &lt;intent-filter>
 *         &lt;action android:name="android.media.browse.MediaBrowserService" />
 *     &lt;/intent-filter>
 * &lt;/service>
 * </pre>
 *
 */
public abstract class MediaBrowserService extends Service {
    private static final String TAG = "MediaBrowserService";
    private static final boolean DBG = false;

    /**
     * The {@link Intent} that must be declared as handled by the service.
     */
    @SdkConstant(SdkConstantType.SERVICE_ACTION)
    public static final String SERVICE_INTERFACE = "android.media.browse.MediaBrowserService";

    /**
     * A key for passing the MediaItem to the ResultReceiver in getItem.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static final String KEY_MEDIA_ITEM = "media_item";

    private static final int RESULT_FLAG_OPTION_NOT_HANDLED = 1 << 0;
    private static final int RESULT_FLAG_ON_LOAD_ITEM_NOT_IMPLEMENTED = 1 << 1;

    private static final int RESULT_ERROR = -1;
    private static final int RESULT_OK = 0;
    private final ServiceBinder mBinder;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = { RESULT_FLAG_OPTION_NOT_HANDLED,
            RESULT_FLAG_ON_LOAD_ITEM_NOT_IMPLEMENTED })
    private @interface ResultFlags { }

    private final Handler mHandler = new Handler();

    private final AtomicReference<ServiceState> mServiceState;

    // Holds the connection record associated with the currently executing callback operation, if
    // any. See getCurrentBrowserInfo for an example. Must only be accessed on mHandler.
    @Nullable private ConnectionRecord mCurrentConnectionOnHandler;

    /**
     * All the info about a connection.
     */
    private static class ConnectionRecord implements IBinder.DeathRecipient {
        public final ServiceState serviceState;
        public final String pkg;
        public final int pid;
        public final int uid;
        public final Bundle rootHints;
        public final IMediaBrowserServiceCallbacks callbacks;
        public final BrowserRoot root;
        public final HashMap<String, List<Pair<IBinder, Bundle>>> subscriptions = new HashMap<>();

        ConnectionRecord(
                ServiceState serviceState,
                String pkg,
                int pid,
                int uid,
                Bundle rootHints,
                IMediaBrowserServiceCallbacks callbacks,
                BrowserRoot root) {
            this.serviceState = serviceState;
            this.pkg = pkg;
            this.pid = pid;
            this.uid = uid;
            this.rootHints = rootHints;
            this.callbacks = callbacks;
            this.root = root;
        }

        @Override
        public void binderDied() {
            serviceState.postOnHandler(
                    () -> serviceState.mConnections.remove(callbacks.asBinder()));
        }
    }

    /**
     * Completion handler for asynchronous callback methods in {@link MediaBrowserService}.
     * <p>
     * Each of the methods that takes one of these to send the result must call
     * {@link #sendResult} to respond to the caller with the given results. If those
     * functions return without calling {@link #sendResult}, they must instead call
     * {@link #detach} before returning, and then may call {@link #sendResult} when
     * they are done. If more than one of those methods is called, an exception will
     * be thrown.
     *
     * @see #onLoadChildren
     * @see #onLoadItem
     */
    public class Result<T> {
        private Object mDebug;
        private boolean mDetachCalled;
        private boolean mSendResultCalled;
        @UnsupportedAppUsage
        private int mFlags;

        Result(Object debug) {
            mDebug = debug;
        }

        /**
         * Send the result back to the caller.
         */
        public void sendResult(T result) {
            if (mSendResultCalled) {
                throw new IllegalStateException("sendResult() called twice for: " + mDebug);
            }
            mSendResultCalled = true;
            onResultSent(result, mFlags);
        }

        /**
         * Detach this message from the current thread and allow the {@link #sendResult}
         * call to happen later.
         */
        public void detach() {
            if (mDetachCalled) {
                throw new IllegalStateException("detach() called when detach() had already"
                        + " been called for: " + mDebug);
            }
            if (mSendResultCalled) {
                throw new IllegalStateException("detach() called when sendResult() had already"
                        + " been called for: " + mDebug);
            }
            mDetachCalled = true;
        }

        boolean isDone() {
            return mDetachCalled || mSendResultCalled;
        }

        void setFlags(@ResultFlags int flags) {
            mFlags = flags;
        }

        /**
         * Called when the result is sent, after assertions about not being called twice
         * have happened.
         */
        void onResultSent(T result, @ResultFlags int flags) {
        }
    }

    private static class ServiceBinder extends IMediaBrowserService.Stub {
        private final AtomicReference<WeakReference<ServiceState>> mServiceState;

        private ServiceBinder(ServiceState serviceState) {
            mServiceState = new AtomicReference<>();
            setServiceState(serviceState);
        }

        public void setServiceState(ServiceState serviceState) {
            mServiceState.set(new WeakReference<>(serviceState));
        }

        @Override
        public void connect(final String pkg, final Bundle rootHints,
                final IMediaBrowserServiceCallbacks callbacks) {
            ServiceState serviceState = mServiceState.get().get();
            if (serviceState == null) {
                return;
            }

            final int pid = Binder.getCallingPid();
            final int uid = Binder.getCallingUid();
            if (!serviceState.isValidPackage(pkg, uid)) {
                throw new IllegalArgumentException("Package/uid mismatch: uid=" + uid
                        + " package=" + pkg);
            }

            serviceState.postOnHandler(
                    () -> serviceState.connectOnHandler(pkg, pid, uid, rootHints, callbacks));
        }

        @Override
        public void disconnect(final IMediaBrowserServiceCallbacks callbacks) {
            ServiceState serviceState = mServiceState.get().get();
            if (serviceState == null) {
                return;
            }

            serviceState.postOnHandler(
                    () -> serviceState.removeConnectionRecordOnHandler(callbacks));
        }

        @Override
        public void addSubscriptionDeprecated(String id, IMediaBrowserServiceCallbacks callbacks) {
            // do-nothing
        }

        @Override
        public void addSubscription(final String id, final IBinder token, final Bundle options,
                final IMediaBrowserServiceCallbacks callbacks) {
            ServiceState serviceState = mServiceState.get().get();
            if (serviceState == null) {
                return;
            }

            serviceState.postOnHandler(
                    () -> serviceState.addSubscriptionOnHandler(id, callbacks, token, options));
        }

        @Override
        public void removeSubscriptionDeprecated(
                String id, IMediaBrowserServiceCallbacks callbacks) {
            // do-nothing
        }

        @Override
        public void removeSubscription(final String id, final IBinder token,
                final IMediaBrowserServiceCallbacks callbacks) {
            ServiceState serviceState = mServiceState.get().get();
            if (serviceState == null) {
                return;
            }

            serviceState.postOnHandler(
                    () -> {
                        if (!serviceState.removeSubscriptionOnHandler(id, callbacks, token)) {
                            Log.w(TAG, "removeSubscription for id with no subscription: " + id);
                        }
                    });
        }

        @Override
        public void getMediaItem(final String mediaId, final ResultReceiver receiver,
                final IMediaBrowserServiceCallbacks callbacks) {
            ServiceState serviceState = mServiceState.get().get();
            if (serviceState == null) {
                return;
            }

            serviceState.postOnHandler(
                    () -> serviceState.performLoadItemOnHandler(mediaId, callbacks, receiver));
        }
    }

    /** Default constructor. */
    public MediaBrowserService() {
        mServiceState = new AtomicReference<>(new ServiceState());
        mBinder = new ServiceBinder(mServiceState.get());
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (SERVICE_INTERFACE.equals(intent.getAction())) {
            return mBinder;
        }

        return null;
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
    }

    /**
     * Called to get the root information for browsing by a particular client.
     * <p>
     * The implementation should verify that the client package has permission
     * to access browse media information before returning the root id; it
     * should return null if the client is not allowed to access this
     * information.
     * </p>
     *
     * @param clientPackageName The package name of the application which is
     *            requesting access to browse media.
     * @param clientUid The uid of the application which is requesting access to
     *            browse media.
     * @param rootHints An optional bundle of service-specific arguments to send
     *            to the media browser service when connecting and retrieving the
     *            root id for browsing, or null if none. The contents of this
     *            bundle may affect the information returned when browsing.
     * @return The {@link BrowserRoot} for accessing this app's content or null.
     * @see BrowserRoot#EXTRA_RECENT
     * @see BrowserRoot#EXTRA_OFFLINE
     * @see BrowserRoot#EXTRA_SUGGESTED
     */
    public abstract @Nullable BrowserRoot onGetRoot(@NonNull String clientPackageName,
            int clientUid, @Nullable Bundle rootHints);

    /**
     * Called to get information about the children of a media item.
     * <p>
     * Implementations must call {@link Result#sendResult result.sendResult}
     * with the list of children. If loading the children will be an expensive
     * operation that should be performed on another thread,
     * {@link Result#detach result.detach} may be called before returning from
     * this function, and then {@link Result#sendResult result.sendResult}
     * called when the loading is complete.
     * </p><p>
     * In case the media item does not have any children, call {@link Result#sendResult}
     * with an empty list. When the given {@code parentId} is invalid, implementations must
     * call {@link Result#sendResult result.sendResult} with {@code null}, which will invoke
     * {@link MediaBrowser.SubscriptionCallback#onError}.
     * </p>
     *
     * @param parentId The id of the parent media item whose children are to be
     *            queried.
     * @param result The Result to send the list of children to.
     */
    public abstract void onLoadChildren(@NonNull String parentId,
            @NonNull Result<List<MediaBrowser.MediaItem>> result);

    /**
     * Called to get information about the children of a media item.
     * <p>
     * Implementations must call {@link Result#sendResult result.sendResult}
     * with the list of children. If loading the children will be an expensive
     * operation that should be performed on another thread,
     * {@link Result#detach result.detach} may be called before returning from
     * this function, and then {@link Result#sendResult result.sendResult}
     * called when the loading is complete.
     * </p><p>
     * In case the media item does not have any children, call {@link Result#sendResult}
     * with an empty list. When the given {@code parentId} is invalid, implementations must
     * call {@link Result#sendResult result.sendResult} with {@code null}, which will invoke
     * {@link MediaBrowser.SubscriptionCallback#onError}.
     * </p>
     *
     * @param parentId The id of the parent media item whose children are to be
     *            queried.
     * @param result The Result to send the list of children to.
     * @param options The bundle of service-specific arguments sent from the media
     *            browser. The information returned through the result should be
     *            affected by the contents of this bundle.
     */
    public void onLoadChildren(@NonNull String parentId,
            @NonNull Result<List<MediaBrowser.MediaItem>> result, @NonNull Bundle options) {
        // To support backward compatibility, when the implementation of MediaBrowserService doesn't
        // override onLoadChildren() with options, onLoadChildren() without options will be used
        // instead, and the options will be applied in the implementation of result.onResultSent().
        result.setFlags(RESULT_FLAG_OPTION_NOT_HANDLED);
        onLoadChildren(parentId, result);
    }

    /**
     * Called to get information about a specific media item.
     * <p>
     * Implementations must call {@link Result#sendResult result.sendResult}. If
     * loading the item will be an expensive operation {@link Result#detach
     * result.detach} may be called before returning from this function, and
     * then {@link Result#sendResult result.sendResult} called when the item has
     * been loaded.
     * </p><p>
     * When the given {@code itemId} is invalid, implementations must call
     * {@link Result#sendResult result.sendResult} with {@code null}.
     * </p><p>
     * The default implementation will invoke {@link MediaBrowser.ItemCallback#onError}.
     * </p>
     *
     * @param itemId The id for the specific
     *            {@link android.media.browse.MediaBrowser.MediaItem}.
     * @param result The Result to send the item to.
     */
    public void onLoadItem(String itemId, Result<MediaBrowser.MediaItem> result) {
        result.setFlags(RESULT_FLAG_ON_LOAD_ITEM_NOT_IMPLEMENTED);
        result.sendResult(null);
    }

    /**
     * Call to set the media session.
     *
     * <p>This should be called as soon as possible during the service's startup. It may only be
     * called once.
     *
     * @param token The token for the service's {@link MediaSession}.
     */
    // TODO: b/185136506 - Update the javadoc to reflect API changes when
    // enableNullSessionInMediaBrowserService makes it to nextfood.
    public void setSessionToken(final MediaSession.Token token) {
        ServiceState serviceState = mServiceState.get();
        if (token == null) {
            if (!Flags.enableNullSessionInMediaBrowserService()) {
                throw new IllegalArgumentException("Session token may not be null.");
            } else if (serviceState.mSession != null) {
                ServiceState newServiceState = new ServiceState();
                mBinder.setServiceState(newServiceState);
                mServiceState.set(newServiceState);
                serviceState.release();
            } else {
                // Nothing to do. The session is already null.
            }
        } else if (serviceState.mSession != null) {
            throw new IllegalStateException("The session token has already been set.");
        } else {
            serviceState.mSession = token;
            mHandler.post(() -> serviceState.notifySessionTokenInitializedOnHandler(token));
        }
    }

    /**
     * Gets the session token, or null if it has not yet been created
     * or if it has been destroyed.
     */
    public @Nullable MediaSession.Token getSessionToken() {
        return mServiceState.get().mSession;
    }

    /**
     * Gets the root hints sent from the currently connected {@link MediaBrowser}.
     * The root hints are service-specific arguments included in an optional bundle sent to the
     * media browser service when connecting and retrieving the root id for browsing, or null if
     * none. The contents of this bundle may affect the information returned when browsing.
     *
     * @throws IllegalStateException If this method is called outside of {@link #onGetRoot} or
     *             {@link #onLoadChildren} or {@link #onLoadItem}.
     * @see MediaBrowserService.BrowserRoot#EXTRA_RECENT
     * @see MediaBrowserService.BrowserRoot#EXTRA_OFFLINE
     * @see MediaBrowserService.BrowserRoot#EXTRA_SUGGESTED
     */
    public final Bundle getBrowserRootHints() {
        ConnectionRecord currentConnection = mCurrentConnectionOnHandler;
        if (currentConnection == null) {
            throw new IllegalStateException("This should be called inside of onGetRoot or"
                    + " onLoadChildren or onLoadItem methods");
        }
        return currentConnection.rootHints == null ? null : new Bundle(currentConnection.rootHints);
    }

    /**
     * Gets the browser information who sent the current request.
     *
     * @throws IllegalStateException If this method is called outside of {@link #onGetRoot} or
     *             {@link #onLoadChildren} or {@link #onLoadItem}.
     * @see MediaSessionManager#isTrustedForMediaControl(RemoteUserInfo)
     */
    public final RemoteUserInfo getCurrentBrowserInfo() {
        ConnectionRecord currentConnection = mCurrentConnectionOnHandler;
        if (currentConnection == null) {
            throw new IllegalStateException("This should be called inside of onGetRoot or"
                    + " onLoadChildren or onLoadItem methods");
        }
        return new RemoteUserInfo(
                currentConnection.pkg, currentConnection.pid, currentConnection.uid);
    }

    /**
     * Notifies all connected media browsers that the children of
     * the specified parent id have changed in some way.
     * This will cause browsers to fetch subscribed content again.
     *
     * @param parentId The id of the parent media item whose
     * children changed.
     */
    public void notifyChildrenChanged(@NonNull String parentId) {
        notifyChildrenChanged(parentId, Bundle.EMPTY);
    }

    /**
     * Notifies all connected media browsers that the children of
     * the specified parent id have changed in some way.
     * This will cause browsers to fetch subscribed content again.
     *
     * @param parentId The id of the parent media item whose
     *            children changed.
     * @param options The bundle of service-specific arguments to send
     *            to the media browser. The contents of this bundle may
     *            contain the information about the change.
     */
    public void notifyChildrenChanged(@NonNull String parentId, @NonNull Bundle options) {
        if (options == null) {
            throw new IllegalArgumentException("options cannot be null in notifyChildrenChanged");
        }
        if (parentId == null) {
            throw new IllegalArgumentException("parentId cannot be null in notifyChildrenChanged");
        }
        mHandler.post(() -> mServiceState.get().notifyChildrenChangeOnHandler(parentId, options));
    }

    /**
     * Contains information that the browser service needs to send to the client
     * when first connected.
     */
    public static final class BrowserRoot {
        /**
         * The lookup key for a boolean that indicates whether the browser service should return a
         * browser root for recently played media items.
         *
         * <p>When creating a media browser for a given media browser service, this key can be
         * supplied as a root hint for retrieving media items that are recently played.
         * If the media browser service can provide such media items, the implementation must return
         * the key in the root hint when {@link #onGetRoot(String, int, Bundle)} is called back.
         *
         * <p>The root hint may contain multiple keys.
         *
         * @see #EXTRA_OFFLINE
         * @see #EXTRA_SUGGESTED
         */
        public static final String EXTRA_RECENT = "android.service.media.extra.RECENT";

        /**
         * The lookup key for a boolean that indicates whether the browser service should return a
         * browser root for offline media items.
         *
         * <p>When creating a media browser for a given media browser service, this key can be
         * supplied as a root hint for retrieving media items that are can be played without an
         * internet connection.
         * If the media browser service can provide such media items, the implementation must return
         * the key in the root hint when {@link #onGetRoot(String, int, Bundle)} is called back.
         *
         * <p>The root hint may contain multiple keys.
         *
         * @see #EXTRA_RECENT
         * @see #EXTRA_SUGGESTED
         */
        public static final String EXTRA_OFFLINE = "android.service.media.extra.OFFLINE";

        /**
         * The lookup key for a boolean that indicates whether the browser service should return a
         * browser root for suggested media items.
         *
         * <p>When creating a media browser for a given media browser service, this key can be
         * supplied as a root hint for retrieving the media items suggested by the media browser
         * service. The list of media items passed in {@link android.media.browse.MediaBrowser.SubscriptionCallback#onChildrenLoaded(String, List)}
         * is considered ordered by relevance, first being the top suggestion.
         * If the media browser service can provide such media items, the implementation must return
         * the key in the root hint when {@link #onGetRoot(String, int, Bundle)} is called back.
         *
         * <p>The root hint may contain multiple keys.
         *
         * @see #EXTRA_RECENT
         * @see #EXTRA_OFFLINE
         */
        public static final String EXTRA_SUGGESTED = "android.service.media.extra.SUGGESTED";

        private final String mRootId;
        private final Bundle mExtras;

        /**
         * Constructs a browser root.
         * @param rootId The root id for browsing.
         * @param extras Any extras about the browser service.
         */
        public BrowserRoot(@NonNull String rootId, @Nullable Bundle extras) {
            if (rootId == null) {
                throw new IllegalArgumentException("The root id in BrowserRoot cannot be null. "
                        + "Use null for BrowserRoot instead.");
            }
            mRootId = rootId;
            mExtras = extras;
        }

        /**
         * Gets the root id for browsing.
         */
        public String getRootId() {
            return mRootId;
        }

        /**
         * Gets any extras about the browser service.
         */
        public Bundle getExtras() {
            return mExtras;
        }
    }

    /**
     * Holds all state associated with {@link #mSession}.
     *
     * <p>This class decouples the state associated with the session from the lifecycle of the
     * service. This allows us to put the service in a valid state once the session is released
     * (which is an irrecoverable invalid state). More details about this in b/185136506.
     */
    private class ServiceState {

        // Fields accessed from any caller thread.
        @Nullable private MediaSession.Token mSession;

        // Fields accessed from mHandler only.
        @NonNull private final ArrayMap<IBinder, ConnectionRecord> mConnections = new ArrayMap<>();

        public ServiceBinder getBinder() {
            return mBinder;
        }

        public void postOnHandler(Runnable runnable) {
            mHandler.post(runnable);
        }

        public void release() {
            mHandler.postAtFrontOfQueue(this::clearConnectionsOnHandler);
        }

        private void clearConnectionsOnHandler() {
            Iterator<ConnectionRecord> iterator = mConnections.values().iterator();
            while (iterator.hasNext()) {
                ConnectionRecord record = iterator.next();
                iterator.remove();
                try {
                    record.callbacks.onDisconnect();
                } catch (RemoteException exception) {
                    Log.w(
                            TAG,
                            TextUtils.formatSimple("onDisconnectRequest for %s failed", record.pkg),
                            exception);
                }
            }
        }

        public void removeConnectionRecordOnHandler(IMediaBrowserServiceCallbacks callbacks) {
            IBinder b = callbacks.asBinder();
            // Clear out the old subscriptions. We are getting new ones.
            ConnectionRecord old = mConnections.remove(b);
            if (old != null) {
                old.callbacks.asBinder().unlinkToDeath(old, 0);
            }
        }

        public void notifySessionTokenInitializedOnHandler(MediaSession.Token token) {
            Iterator<ConnectionRecord> iter = mConnections.values().iterator();
            while (iter.hasNext()) {
                ConnectionRecord connection = iter.next();
                try {
                    connection.callbacks.onConnect(
                            connection.root.getRootId(), token, connection.root.getExtras());
                } catch (RemoteException e) {
                    Log.w(TAG, "Connection for " + connection.pkg + " is no longer valid.");
                    iter.remove();
                }
            }
        }

        public void notifyChildrenChangeOnHandler(String parentId, Bundle options) {
            for (IBinder binder : mConnections.keySet()) {
                ConnectionRecord connection = mConnections.get(binder);
                List<Pair<IBinder, Bundle>> callbackList = connection.subscriptions.get(parentId);
                if (callbackList != null) {
                    for (Pair<IBinder, Bundle> callback : callbackList) {
                        if (MediaBrowserUtils.hasDuplicatedItems(options, callback.second)) {
                            performLoadChildrenOnHandler(parentId, connection, callback.second);
                        }
                    }
                }
            }
        }

        /** Save the subscription and if it is a new subscription send the results. */
        public void addSubscriptionOnHandler(
                String id, IMediaBrowserServiceCallbacks callbacks, IBinder token, Bundle options) {
            IBinder b = callbacks.asBinder();
            // Get the record for the connection
            ConnectionRecord connection = mConnections.get(b);
            if (connection == null) {
                Log.w(TAG, "addSubscription for callback that isn't registered id=" + id);
                return;
            }

            // Save the subscription
            List<Pair<IBinder, Bundle>> callbackList = connection.subscriptions.get(id);
            if (callbackList == null) {
                callbackList = new ArrayList<>();
            }
            for (Pair<IBinder, Bundle> callback : callbackList) {
                if (token == callback.first
                        && MediaBrowserUtils.areSameOptions(options, callback.second)) {
                    return;
                }
            }
            callbackList.add(new Pair<>(token, options));
            connection.subscriptions.put(id, callbackList);
            // send the results
            performLoadChildrenOnHandler(id, connection, options);
        }

        public void connectOnHandler(
                String pkg,
                int pid,
                int uid,
                Bundle rootHints,
                IMediaBrowserServiceCallbacks callbacks) {
            IBinder b = callbacks.asBinder();
            // Clear out the old subscriptions. We are getting new ones.
            mConnections.remove(b);

            // Temporarily sets a placeholder ConnectionRecord to make getCurrentBrowserInfo() work
            // in onGetRoot().
            mCurrentConnectionOnHandler =
                    new ConnectionRecord(
                            /* serviceState= */ this,
                            pkg,
                            pid,
                            uid,
                            rootHints,
                            callbacks,
                            /* root= */ null);
            BrowserRoot root = onGetRoot(pkg, uid, rootHints);
            mCurrentConnectionOnHandler = null;

            // If they didn't return something, don't allow this client.
            if (root == null) {
                Log.i(TAG, "No root for client " + pkg + " from service " + getClass().getName());
                try {
                    callbacks.onConnectFailed();
                } catch (RemoteException ex) {
                    Log.w(TAG, "Calling onConnectFailed() failed. Ignoring. pkg=" + pkg);
                }
            } else {
                try {
                    ConnectionRecord connection =
                            new ConnectionRecord(
                                    /* serviceState= */ this,
                                    pkg,
                                    pid,
                                    uid,
                                    rootHints,
                                    callbacks,
                                    root);
                    mConnections.put(b, connection);
                    b.linkToDeath(connection, /* flags= */ 0);
                    if (mSession != null) {
                        callbacks.onConnect(
                                connection.root.getRootId(), mSession, connection.root.getExtras());
                    }
                } catch (RemoteException ex) {
                    Log.w(TAG, "Calling onConnect() failed. Dropping client. pkg=" + pkg);
                    mConnections.remove(b);
                }
            }
        }

        /** Remove the subscription. */
        public boolean removeSubscriptionOnHandler(
                String id, IMediaBrowserServiceCallbacks callbacks, IBinder token) {
            IBinder b = callbacks.asBinder();

            ConnectionRecord connection = mConnections.get(b);
            if (connection == null) {
                Log.w(TAG, "removeSubscription for callback that isn't registered id=" + id);
                return true;
            }

            if (token == null) {
                return connection.subscriptions.remove(id) != null;
            }
            boolean removed = false;
            List<Pair<IBinder, Bundle>> callbackList = connection.subscriptions.get(id);
            if (callbackList != null) {
                Iterator<Pair<IBinder, Bundle>> iter = callbackList.iterator();
                while (iter.hasNext()) {
                    if (token == iter.next().first) {
                        removed = true;
                        iter.remove();
                    }
                }
                if (callbackList.isEmpty()) {
                    connection.subscriptions.remove(id);
                }
            }
            return removed;
        }

        /**
         * Call onLoadChildren and then send the results back to the connection.
         *
         * <p>Callers must make sure that this connection is still connected.
         */
        public void performLoadChildrenOnHandler(
                String parentId, ConnectionRecord connection, Bundle options) {
            Result<List<MediaBrowser.MediaItem>> result =
                    new Result<>(parentId) {
                        @Override
                        void onResultSent(
                                List<MediaBrowser.MediaItem> list, @ResultFlags int flag) {
                            if (mConnections.get(connection.callbacks.asBinder()) != connection) {
                                if (DBG) {
                                    Log.d(
                                            TAG,
                                            "Not sending onLoadChildren result for connection that"
                                                    + " has been disconnected. pkg="
                                                    + connection.pkg
                                                    + " id="
                                                    + parentId);
                                }
                                return;
                            }

                            List<MediaBrowser.MediaItem> filteredList =
                                    (flag & RESULT_FLAG_OPTION_NOT_HANDLED) != 0
                                            ? MediaBrowserUtils.applyPagingOptions(list, options)
                                            : list;
                            ParceledListSlice<MediaBrowser.MediaItem> pls = null;
                            if (filteredList != null) {
                                pls = new ParceledListSlice<>(filteredList);
                                // Limit the size of initial Parcel to prevent binder buffer
                                // overflow as onLoadChildren is an async binder call.
                                pls.setInlineCountLimit(1);
                            }
                            try {
                                connection.callbacks.onLoadChildren(parentId, pls, options);
                            } catch (RemoteException ex) {
                                // The other side is in the process of crashing.
                                Log.w(
                                        TAG,
                                        "Calling onLoadChildren() failed for id="
                                                + parentId
                                                + " package="
                                                + connection.pkg);
                            }
                        }
                    };

            mCurrentConnectionOnHandler = connection;
            if (options == null) {
                onLoadChildren(parentId, result);
            } else {
                onLoadChildren(parentId, result, options);
            }
            mCurrentConnectionOnHandler = null;

            if (!result.isDone()) {
                throw new IllegalStateException(
                        "onLoadChildren must call detach() or sendResult()"
                                + " before returning for package="
                                + connection.pkg
                                + " id="
                                + parentId);
            }
        }

        public void performLoadItemOnHandler(
                String itemId,
                IMediaBrowserServiceCallbacks callbacks,
                ResultReceiver receiver) {
            IBinder b = callbacks.asBinder();
            ConnectionRecord connection = mConnections.get(b);
            if (connection == null) {
                Log.w(TAG, "getMediaItem for callback that isn't registered id=" + itemId);
                return;
            }

            Result<MediaBrowser.MediaItem> result =
                    new Result<>(itemId) {
                        @Override
                        void onResultSent(MediaBrowser.MediaItem item, @ResultFlags int flag) {
                            if (mConnections.get(connection.callbacks.asBinder()) != connection) {
                                if (DBG) {
                                    Log.d(
                                            TAG,
                                            "Not sending onLoadItem result for connection that has"
                                                    + " been disconnected. pkg="
                                                    + connection.pkg
                                                    + " id="
                                                    + itemId);
                                }
                                return;
                            }
                            if ((flag & RESULT_FLAG_ON_LOAD_ITEM_NOT_IMPLEMENTED) != 0) {
                                receiver.send(RESULT_ERROR, null);
                                return;
                            }
                            Bundle bundle = new Bundle();
                            bundle.putParcelable(KEY_MEDIA_ITEM, item);
                            receiver.send(RESULT_OK, bundle);
                        }
                    };

            mCurrentConnectionOnHandler = connection;
            onLoadItem(itemId, result);
            mCurrentConnectionOnHandler = null;

            if (!result.isDone()) {
                throw new IllegalStateException(
                        "onLoadItem must call detach() or sendResult() before returning for id="
                                + itemId);
            }
        }

        /** Return whether the given package corresponds to the given uid. */
        public boolean isValidPackage(String providedPackage, int uid) {
            if (providedPackage == null) {
                return false;
            }
            PackageManager pm = getPackageManager();
            for (String packageForUid : pm.getPackagesForUid(uid)) {
                if (packageForUid.equals(providedPackage)) {
                    return true;
                }
            }
            return false;
        }
    }
}
