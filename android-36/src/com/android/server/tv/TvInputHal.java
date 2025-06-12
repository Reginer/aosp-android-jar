/*
 * Copyright 2014 The Android Open Source Project
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

package com.android.server.tv;

import android.hardware.tv.input.V1_0.Constants;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvStreamConfig;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.MessageQueue;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;

/**
 * Provides access to the low-level TV input hardware abstraction layer.
 */
final class TvInputHal implements Handler.Callback {
    private final static boolean DEBUG = false;
    private final static String TAG = TvInputHal.class.getSimpleName();

    public final static int SUCCESS = 0;
    public final static int ERROR_NO_INIT = -1;
    public final static int ERROR_STALE_CONFIG = -2;
    public final static int ERROR_UNKNOWN = -3;

    public static final int EVENT_DEVICE_AVAILABLE = Constants.EVENT_DEVICE_AVAILABLE;
    public static final int EVENT_DEVICE_UNAVAILABLE = Constants.EVENT_DEVICE_UNAVAILABLE;
    public static final int EVENT_STREAM_CONFIGURATION_CHANGED =
            Constants.EVENT_STREAM_CONFIGURATIONS_CHANGED;
    public static final int EVENT_FIRST_FRAME_CAPTURED = 4;

    public static final int EVENT_TV_MESSAGE = 5;

    public interface Callback {
        void onDeviceAvailable(TvInputHardwareInfo info, TvStreamConfig[] configs);
        void onDeviceUnavailable(int deviceId);
        void onStreamConfigurationChanged(int deviceId, TvStreamConfig[] configs,
                int cableConnectionStatus);
        void onFirstFrameCaptured(int deviceId, int streamId);
        void onTvMessage(int deviceId, int type, Bundle data);
    }

    private native long nativeOpen(MessageQueue queue);

    private static native int nativeAddOrUpdateStream(long ptr, int deviceId, int streamId,
            Surface surface);
    private static native int nativeRemoveStream(long ptr, int deviceId, int streamId);
    private static native TvStreamConfig[] nativeGetStreamConfigs(long ptr, int deviceId,
            int generation);
    private static native void nativeClose(long ptr);
    private static native int nativeSetTvMessageEnabled(long ptr, int deviceId, int streamId,
            int type, boolean enabled);

    private static native int nativeSetPictureProfile(
            long ptr, int deviceId, int streamId, long profileHandle);

    private final Object mLock = new Object();
    private long mPtr = 0;
    private final Callback mCallback;
    private final Handler mHandler;
    private final SparseIntArray mStreamConfigGenerations = new SparseIntArray();
    private final SparseArray<TvStreamConfig[]> mStreamConfigs = new SparseArray<>();

    public TvInputHal(Callback callback) {
        mCallback = callback;
        mHandler = new Handler(this);
    }

    public void init() {
        synchronized (mLock) {
            mPtr = nativeOpen(mHandler.getLooper().getQueue());
        }
    }

    public int addOrUpdateStream(int deviceId, Surface surface, TvStreamConfig streamConfig) {
        synchronized (mLock) {
            if (mPtr == 0) {
                return ERROR_NO_INIT;
            }
            int generation = mStreamConfigGenerations.get(deviceId, 0);
            if (generation != streamConfig.getGeneration()) {
                return ERROR_STALE_CONFIG;
            }
            if (nativeAddOrUpdateStream(mPtr, deviceId, streamConfig.getStreamId(), surface) == 0) {
                return SUCCESS;
            } else {
                return ERROR_UNKNOWN;
            }
        }
    }

    public int setTvMessageEnabled(int deviceId, TvStreamConfig streamConfig, int type,
            boolean enabled) {
        synchronized (mLock) {
            if (mPtr == 0) {
                return ERROR_NO_INIT;
            }
            int generation = mStreamConfigGenerations.get(deviceId, 0);
            if (generation != streamConfig.getGeneration()) {
                return ERROR_STALE_CONFIG;
            }
            if (nativeSetTvMessageEnabled(mPtr, deviceId, streamConfig.getStreamId(), type,
                    enabled) == 0) {
                return SUCCESS;
            } else {
                return ERROR_UNKNOWN;
            }
        }
    }

    public int setPictureProfile(int deviceId, TvStreamConfig streamConfig, long profileHandle) {
        synchronized (mLock) {
            if (mPtr == 0) {
                return ERROR_NO_INIT;
            }
            int generation = mStreamConfigGenerations.get(deviceId, 0);
            if (generation != streamConfig.getGeneration()) {
                return ERROR_STALE_CONFIG;
            }
            if (nativeSetPictureProfile(mPtr, deviceId, streamConfig.getStreamId(), profileHandle)
                    == 0) {
                return SUCCESS;
            } else {
                return ERROR_UNKNOWN;
            }
        }
    }

    public int removeStream(int deviceId, TvStreamConfig streamConfig) {
        synchronized (mLock) {
            if (mPtr == 0) {
                return ERROR_NO_INIT;
            }
            int generation = mStreamConfigGenerations.get(deviceId, 0);
            if (generation != streamConfig.getGeneration()) {
                return ERROR_STALE_CONFIG;
            }
            if (nativeRemoveStream(mPtr, deviceId, streamConfig.getStreamId()) == 0) {
                return SUCCESS;
            } else {
                return ERROR_UNKNOWN;
            }
        }
    }

    public void close() {
        synchronized (mLock) {
            if (mPtr != 0L) {
                nativeClose(mPtr);
            }
        }
    }

    private void retrieveStreamConfigsLocked(int deviceId) {
        int generation = mStreamConfigGenerations.get(deviceId, 0) + 1;
        mStreamConfigs.put(deviceId, nativeGetStreamConfigs(mPtr, deviceId, generation));
        mStreamConfigGenerations.put(deviceId, generation);
    }

    // Called from native
    private void deviceAvailableFromNative(TvInputHardwareInfo info) {
        if (DEBUG) {
            Slog.d(TAG, "deviceAvailableFromNative: info = " + info);
        }
        mHandler.obtainMessage(EVENT_DEVICE_AVAILABLE, info).sendToTarget();
    }

    private void deviceUnavailableFromNative(int deviceId) {
        mHandler.obtainMessage(EVENT_DEVICE_UNAVAILABLE, deviceId, 0).sendToTarget();
    }

    private void streamConfigsChangedFromNative(int deviceId, int cableConnectionStatus) {
        mHandler.obtainMessage(EVENT_STREAM_CONFIGURATION_CHANGED, deviceId,
            cableConnectionStatus).sendToTarget();
    }

    private void firstFrameCapturedFromNative(int deviceId, int streamId) {
        mHandler.sendMessage(
                mHandler.obtainMessage(EVENT_STREAM_CONFIGURATION_CHANGED, deviceId, streamId));
    }

    private void tvMessageReceivedFromNative(int deviceId, int type, Bundle data) {
        mHandler.obtainMessage(EVENT_TV_MESSAGE, deviceId, type, data).sendToTarget();
    }

    // Handler.Callback implementation

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_DEVICE_AVAILABLE: {
                TvStreamConfig[] configs;
                TvInputHardwareInfo info = (TvInputHardwareInfo)msg.obj;
                synchronized (mLock) {
                    retrieveStreamConfigsLocked(info.getDeviceId());
                    if (DEBUG) {
                        Slog.d(TAG, "EVENT_DEVICE_AVAILABLE: info = " + info);
                    }
                    configs = mStreamConfigs.get(info.getDeviceId());
                }
                mCallback.onDeviceAvailable(info, configs);
                break;
            }

            case EVENT_DEVICE_UNAVAILABLE: {
                int deviceId = msg.arg1;
                if (DEBUG) {
                    Slog.d(TAG, "EVENT_DEVICE_UNAVAILABLE: deviceId = " + deviceId);
                }
                mCallback.onDeviceUnavailable(deviceId);
                break;
            }

            case EVENT_STREAM_CONFIGURATION_CHANGED: {
                TvStreamConfig[] configs;
                int deviceId = msg.arg1;
                int cableConnectionStatus = msg.arg2;
                synchronized (mLock) {
                    if (DEBUG) {
                        Slog.d(TAG, "EVENT_STREAM_CONFIGURATION_CHANGED: deviceId = " + deviceId);
                    }
                    retrieveStreamConfigsLocked(deviceId);
                    configs = mStreamConfigs.get(deviceId);
                }
                mCallback.onStreamConfigurationChanged(deviceId, configs, cableConnectionStatus);
                break;
            }

            case EVENT_FIRST_FRAME_CAPTURED: {
                int deviceId = msg.arg1;
                int streamId = msg.arg2;
                mCallback.onFirstFrameCaptured(deviceId, streamId);
                break;
            }

            case EVENT_TV_MESSAGE: {
                int deviceId = msg.arg1;
                int type = msg.arg2;
                Bundle data = (Bundle) msg.obj;
                mCallback.onTvMessage(deviceId, type, data);
                break;
            }

            default:
                Slog.e(TAG, "Unknown event: " + msg);
                return false;
        }

        return true;
    }
}
