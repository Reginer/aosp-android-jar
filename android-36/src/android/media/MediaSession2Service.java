/*
 * Copyright 2019 The Android Open Source Project
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

package android.media;

import static android.media.MediaConstants.KEY_CONNECTION_HINTS;
import static android.media.MediaConstants.KEY_PACKAGE_NAME;
import static android.media.MediaConstants.KEY_PID;

import android.annotation.CallSuper;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaSession2.ControllerInfo;
import android.media.session.MediaSessionManager;
import android.media.session.MediaSessionManager.RemoteUserInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.ArrayMap;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This API is not generally intended for third party application developers.
 * Use the <a href="{@docRoot}jetpack/androidx.html">AndroidX</a>
 * <a href="{@docRoot}media/media3/session/control-playback">Media3 session
 * Library</a> for consistent behavior across all devices.
 * <p>
 * Service containing {@link MediaSession2}.
 */
public abstract class MediaSession2Service extends Service {
    /**
     * The {@link Intent} that must be declared as handled by the service.
     */
    public static final String SERVICE_INTERFACE = "android.media.MediaSession2Service";

    private static final String TAG = "MediaSession2Service";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final MediaSession2.ForegroundServiceEventCallback mForegroundServiceEventCallback =
            new MediaSession2.ForegroundServiceEventCallback() {
                @Override
                public void onPlaybackActiveChanged(MediaSession2 session, boolean playbackActive) {
                    MediaSession2Service.this.onPlaybackActiveChanged(session, playbackActive);
                }

                @Override
                public void onSessionClosed(MediaSession2 session) {
                    removeSession(session);
                }
            };

    private final Object mLock = new Object();
    //@GuardedBy("mLock")
    private NotificationManager mNotificationManager;
    //@GuardedBy("mLock")
    private MediaSessionManager mMediaSessionManager;
    //@GuardedBy("mLock")
    private Intent mStartSelfIntent;
    //@GuardedBy("mLock")
    private Map<String, MediaSession2> mSessions = new ArrayMap<>();
    //@GuardedBy("mLock")
    private Map<MediaSession2, MediaNotification> mNotifications = new ArrayMap<>();
    //@GuardedBy("mLock")
    private MediaSession2ServiceStub mStub;

    /**
     * Called by the system when the service is first created. Do not call this method directly.
     * <p>
     * Override this method if you need your own initialization. Derived classes MUST call through
     * to the super class's implementation of this method.
     */
    @CallSuper
    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (mLock) {
            mStub = new MediaSession2ServiceStub(this);
            mStartSelfIntent = new Intent(this, this.getClass());
            mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mMediaSessionManager =
                    (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        }
    }

    @CallSuper
    @Override
    @Nullable
    public IBinder onBind(@NonNull Intent intent) {
        if (SERVICE_INTERFACE.equals(intent.getAction())) {
            synchronized (mLock) {
                return mStub;
            }
        }
        return null;
    }

    /**
     * Called by the system to notify that it is no longer used and is being removed. Do not call
     * this method directly.
     * <p>
     * Override this method if you need your own clean up. Derived classes MUST call through
     * to the super class's implementation of this method.
     */
    @CallSuper
    @Override
    public void onDestroy() {
        super.onDestroy();
        synchronized (mLock) {
            List<MediaSession2> sessions = getSessions();
            for (MediaSession2 session : sessions) {
                removeSession(session);
            }
            mSessions.clear();
            mNotifications.clear();
        }
        mStub.close();
    }

    /**
     * Called when a {@link MediaController2} is created with the this service's
     * {@link Session2Token}. Return the session for telling the controller which session to
     * connect. Return {@code null} to reject the connection from this controller.
     * <p>
     * Session returned here will be added to this service automatically. You don't need to call
     * {@link #addSession(MediaSession2)} for that.
     * <p>
     * This method is always called on the main thread.
     *
     * @param controllerInfo information of the controller which is trying to connect.
     * @return a {@link MediaSession2} instance for the controller to connect to, or {@code null}
     *         to reject connection
     * @see MediaSession2.Builder
     * @see #getSessions()
     */
    @Nullable
    public abstract MediaSession2 onGetSession(@NonNull ControllerInfo controllerInfo);

    /**
     * Called to update the media notification when the playback state changes.
     * <p>
     * If playback is active and a notification is returned, the service uses it to become a
     * foreground service. If playback is not active then the notification is still posted, but the
     * service does not become a foreground service.
     * <p>
     * Apps must request the {@link android.Manifest.permission#FOREGROUND_SERVICE} permission
     * in order to use this API. For apps targeting {@link android.os.Build.VERSION_CODES#TIRAMISU}
     * or later, notifications will only be posted if the app has also been granted the
     * {@link android.Manifest.permission#POST_NOTIFICATIONS} permission.
     *
     * @param session the session for which an updated media notification is required.
     * @return the {@link MediaNotification}. Can be {@code null}.
     */
    @Nullable
    public abstract MediaNotification onUpdateNotification(@NonNull MediaSession2 session);

    /**
     * Adds a session to this service.
     * <p>
     * Added session will be removed automatically when it's closed, or removed when
     * {@link #removeSession} is called.
     *
     * @param session a session to be added.
     * @see #removeSession(MediaSession2)
     */
    public final void addSession(@NonNull MediaSession2 session) {
        if (session == null) {
            throw new IllegalArgumentException("session shouldn't be null");
        }
        if (session.isClosed()) {
            throw new IllegalArgumentException("session is already closed");
        }
        synchronized (mLock) {
            MediaSession2 previousSession = mSessions.get(session.getId());
            if (previousSession != null) {
                if (previousSession != session) {
                    Log.w(TAG, "Session ID should be unique, ID=" + session.getId()
                            + ", previous=" + previousSession + ", session=" + session);
                }
                return;
            }
            mSessions.put(session.getId(), session);
            session.setForegroundServiceEventCallback(mForegroundServiceEventCallback);
        }
    }

    /**
     * Removes a session from this service.
     *
     * @param session a session to be removed.
     * @see #addSession(MediaSession2)
     */
    public final void removeSession(@NonNull MediaSession2 session) {
        if (session == null) {
            throw new IllegalArgumentException("session shouldn't be null");
        }
        MediaNotification notification;
        synchronized (mLock) {
            if (mSessions.get(session.getId()) != session) {
                // Session isn't added or removed already.
                return;
            }
            mSessions.remove(session.getId());
            notification = mNotifications.remove(session);
        }
        session.setForegroundServiceEventCallback(null);
        if (notification != null) {
            mNotificationManager.cancel(notification.getNotificationId());
        }
        if (getSessions().isEmpty()) {
            stopForeground(false);
        }
    }

    /**
     * Gets the list of {@link MediaSession2}s that you've added to this service.
     *
     * @return sessions
     */
    public final @NonNull List<MediaSession2> getSessions() {
        List<MediaSession2> list = new ArrayList<>();
        synchronized (mLock) {
            list.addAll(mSessions.values());
        }
        return list;
    }

    /**
     * Returns the {@link MediaSessionManager}.
     */
    @NonNull
    MediaSessionManager getMediaSessionManager() {
        synchronized (mLock) {
            return mMediaSessionManager;
        }
    }

    /**
     * Called by registered {@link MediaSession2.ForegroundServiceEventCallback}
     *
     * @param session session with change
     * @param playbackActive {@code true} if playback is active.
     */
    void onPlaybackActiveChanged(MediaSession2 session, boolean playbackActive) {
        MediaNotification mediaNotification = onUpdateNotification(session);
        if (mediaNotification == null) {
            // The service implementation doesn't want to use the automatic start/stopForeground
            // feature.
            return;
        }
        synchronized (mLock) {
            mNotifications.put(session, mediaNotification);
        }
        int id = mediaNotification.getNotificationId();
        Notification notification = mediaNotification.getNotification();
        if (!playbackActive) {
            mNotificationManager.notify(id, notification);
            return;
        }
        // playbackActive == true
        startForegroundService(mStartSelfIntent);
        startForeground(id, notification);
    }

    /**
     * This API is not generally intended for third party application developers.
     * Use the <a href="{@docRoot}jetpack/androidx.html">AndroidX</a>
     * <a href="{@docRoot}media/media3/session/control-playback">Media3 session
     * Library</a> for consistent behavior across all devices.
     * <p>
     * Returned by {@link #onUpdateNotification(MediaSession2)} for making session service
     * foreground service to keep playback running in the background. It's highly recommended to
     * show media style notification here.
     */
    public static class MediaNotification {
        private final int mNotificationId;
        private final Notification mNotification;

        /**
         * Default constructor
         *
         * @param notificationId notification id to be used for
         *        {@link NotificationManager#notify(int, Notification)}.
         * @param notification a notification to make session service run in the foreground. Media
         *        style notification is recommended here.
         */
        public MediaNotification(int notificationId, @NonNull Notification notification) {
            if (notification == null) {
                throw new IllegalArgumentException("notification shouldn't be null");
            }
            mNotificationId = notificationId;
            mNotification = notification;
        }

        /**
         * Gets the id of the notification.
         *
         * @return the notification id
         */
        public int getNotificationId() {
            return mNotificationId;
        }

        /**
         * Gets the notification.
         *
         * @return the notification
         */
        @NonNull
        public Notification getNotification() {
            return mNotification;
        }
    }

    private static final class MediaSession2ServiceStub extends IMediaSession2Service.Stub
            implements AutoCloseable {
        final WeakReference<MediaSession2Service> mService;
        final Handler mHandler;

        MediaSession2ServiceStub(MediaSession2Service service) {
            mService = new WeakReference<>(service);
            mHandler = new Handler(service.getMainLooper());
        }

        @Override
        public void connect(Controller2Link caller, int seq, Bundle connectionRequest) {
            if (mService.get() == null) {
                if (DEBUG) {
                    Log.d(TAG, "Service is already destroyed");
                }
                return;
            }
            if (caller == null || connectionRequest == null) {
                if (DEBUG) {
                    Log.d(TAG, "Ignoring calls with illegal arguments, caller=" + caller
                            + ", connectionRequest=" + connectionRequest);
                }
                return;
            }
            final int pid = Binder.getCallingPid();
            final int uid = Binder.getCallingUid();
            final long token = Binder.clearCallingIdentity();
            try {
                mHandler.post(() -> {
                    boolean shouldNotifyDisconnected = true;
                    try {
                        final MediaSession2Service service = mService.get();
                        if (service == null) {
                            if (DEBUG) {
                                Log.d(TAG, "Service isn't available");
                            }
                            return;
                        }

                        String callingPkg = connectionRequest.getString(KEY_PACKAGE_NAME);
                        // The Binder.getCallingPid() can be 0 for an oneway call from the
                        // remote process. If it's the case, use PID from the connectionRequest.
                        RemoteUserInfo remoteUserInfo = new RemoteUserInfo(
                                callingPkg,
                                pid == 0 ? connectionRequest.getInt(KEY_PID) : pid,
                                uid);

                        Bundle connectionHints = connectionRequest.getBundle(KEY_CONNECTION_HINTS);
                        if (connectionHints == null) {
                            Log.w(TAG, "connectionHints shouldn't be null.");
                            connectionHints = Bundle.EMPTY;
                        } else if (MediaSession2.hasCustomParcelable(connectionHints)) {
                            Log.w(TAG, "connectionHints contain custom parcelable. Ignoring.");
                            connectionHints = Bundle.EMPTY;
                        }

                        final ControllerInfo controllerInfo = new ControllerInfo(
                                remoteUserInfo,
                                service.getMediaSessionManager()
                                        .isTrustedForMediaControl(remoteUserInfo),
                                caller,
                                connectionHints);

                        if (DEBUG) {
                            Log.d(TAG, "Handling incoming connection request from the"
                                    + " controller=" + controllerInfo);
                        }

                        final MediaSession2 session;
                        session = service.onGetSession(controllerInfo);

                        if (session == null) {
                            if (DEBUG) {
                                Log.d(TAG, "Rejecting incoming connection request from the"
                                        + " controller=" + controllerInfo);
                            }
                            // Note: Trusted controllers also can be rejected according to the
                            // service implementation.
                            return;
                        }
                        service.addSession(session);
                        shouldNotifyDisconnected = false;
                        session.onConnect(caller, pid, uid, seq, connectionRequest);
                    } catch (Exception e) {
                        // Don't propagate exception in service to the controller.
                        Log.w(TAG, "Failed to add a session to session service", e);
                    } finally {
                        // Trick to call onDisconnected() in one place.
                        if (shouldNotifyDisconnected) {
                            if (DEBUG) {
                                Log.d(TAG, "Notifying the controller of its disconnection");
                            }
                            try {
                                caller.notifyDisconnected(0);
                            } catch (RuntimeException e) {
                                // Controller may be died prematurely.
                                // Not an issue because we'll ignore it anyway.
                            }
                        }
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void close() {
            mHandler.removeCallbacksAndMessages(null);
            mService.clear();
        }
    }
}
