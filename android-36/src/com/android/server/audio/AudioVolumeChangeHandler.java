/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.server.audio;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.media.INativeAudioVolumeGroupCallback;
import android.media.audio.common.AudioVolumeGroupChangeEvent;
import android.media.audiopolicy.IAudioVolumeChangeDispatcher;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Slog;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;

/**
 * The AudioVolumeChangeHandler handles AudioVolume callbacks invoked by native
 * {@link INativeAudioVolumeGroupCallback} callback.
 */
/* private package */ class AudioVolumeChangeHandler {
    private static final String TAG = "AudioVolumeChangeHandler";

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private final RemoteCallbackList<IAudioVolumeChangeDispatcher> mListeners =
            new RemoteCallbackList<>();
    private final @NonNull AudioSystemAdapter mAudioSystem;
    private @Nullable AudioVolumeGroupCallback mAudioVolumeGroupCallback;

    AudioVolumeChangeHandler(@NonNull AudioSystemAdapter asa) {
        mAudioSystem = asa;
    }

    @GuardedBy("mLock")
    private void lazyInitLocked() {
        mAudioVolumeGroupCallback = new AudioVolumeGroupCallback();
        mAudioSystem.registerAudioVolumeGroupCallback(mAudioVolumeGroupCallback);
    }

    private void sendAudioVolumeGroupChangedToClients(int groupId, int index) {
        RemoteCallbackList<IAudioVolumeChangeDispatcher> listeners;
        int nbDispatchers;
        synchronized (mLock) {
            listeners = mListeners;
            nbDispatchers = mListeners.beginBroadcast();
        }
        for (int i = 0; i < nbDispatchers; i++) {
            try {
                listeners.getBroadcastItem(i).onAudioVolumeGroupChanged(groupId, index);
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to broadcast Volume Changed event");
            }
        }
        synchronized (mLock) {
            mListeners.finishBroadcast();
        }
    }

   /**
    * @param cb the {@link IAudioVolumeChangeDispatcher} to register
    */
    public void registerListener(@NonNull IAudioVolumeChangeDispatcher cb) {
        Preconditions.checkNotNull(cb, "Volume group callback must not be null");
        synchronized (mLock) {
            if (mAudioVolumeGroupCallback == null) {
                lazyInitLocked();
            }
            mListeners.register(cb);
        }
    }

   /**
    * @param cb the {@link IAudioVolumeChangeDispatcher} to unregister
    */
    public void unregisterListener(@NonNull IAudioVolumeChangeDispatcher cb) {
        Preconditions.checkNotNull(cb, "Volume group callback must not be null");
        synchronized (mLock) {
            mListeners.unregister(cb);
        }
    }

    private final class AudioVolumeGroupCallback extends INativeAudioVolumeGroupCallback.Stub {
        public void onAudioVolumeGroupChanged(AudioVolumeGroupChangeEvent volumeEvent) {
            Slog.v(TAG, "onAudioVolumeGroupChanged volumeEvent=" + volumeEvent);
            sendAudioVolumeGroupChangedToClients(volumeEvent.groupId, volumeEvent.flags);
        }
    }
}
