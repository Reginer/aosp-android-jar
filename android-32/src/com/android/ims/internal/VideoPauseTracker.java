/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.ims.internal;

import android.telecom.VideoProfile;
import android.telephony.ims.ImsVideoCallProvider;
import android.util.ArraySet;
import android.util.Log;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Used by an {@link ImsVideoCallProviderWrapper} to track requests to pause video from various
 * sources.
 *
 * Requests to pause the video stream using the {@link VideoProfile#STATE_PAUSED} bit can come
 * from both the {@link android.telecom.InCallService}, as well as via the
 * {@link ImsVideoCallProviderWrapper#pauseVideo(int, int)} and
 * {@link ImsVideoCallProviderWrapper#resumeVideo(int, int)} methods.  As a result, multiple sources
 * can potentially pause or resume the video stream.
 *
 * This class is responsible for tracking any active requests to pause the video.
 */
public class VideoPauseTracker {
    /** The pause or resume request originated from an InCallService. */
    public static final int SOURCE_INCALL = 1;

    /**
     * The pause or resume request originated from a change to the data enabled state from the
     * {@code ImsPhoneCallTracker#onDataEnabledChanged(boolean, int)} callback.  This happens when
     * the user reaches their data limit or enables and disables data.
     */
    public static final int SOURCE_DATA_ENABLED = 2;

    private static final String SOURCE_INCALL_STR = "INCALL";
    private static final String SOURCE_DATA_ENABLED_STR = "DATA_ENABLED";

    private static final String LOG_TAG = VideoPauseTracker.class.getSimpleName();

    /**
     * Tracks the current sources of pause requests.
     */
    private Set<Integer> mPauseRequests = new ArraySet<Integer>(2);

    /**
     * Lock for the {@link #mPauseRequests} {@link ArraySet}.
     */
    private Object mPauseRequestsLock = new Object();

    /**
     * Tracks a request to pause the video for a source (see {@link #SOURCE_DATA_ENABLED},
     * {@link #SOURCE_INCALL}) and determines whether a pause request should be issued to the
     * video provider.
     *
     * We want to issue a pause request to the provider when we receive the first request
     * to pause via any source and we're not already paused.
     *
     * @param source The source of the pause request.
     * @return {@code true} if a pause should be issued to the
     *      {@link ImsVideoCallProvider}, {@code false} otherwise.
     */
    public boolean shouldPauseVideoFor(int source) {
        synchronized (mPauseRequestsLock) {
            boolean wasPaused = isPaused();
            mPauseRequests.add(source);

            if (!wasPaused) {
                Log.i(LOG_TAG, String.format(
                        "shouldPauseVideoFor: source=%s, pendingRequests=%s - should pause",
                        sourceToString(source), sourcesToString(mPauseRequests)));
                // There were previously no pause requests, but there is one now, so pause.
                return true;
            } else {
                Log.i(LOG_TAG, String.format(
                        "shouldPauseVideoFor: source=%s, pendingRequests=%s - already paused",
                        sourceToString(source), sourcesToString(mPauseRequests)));
                // There were already pause requests, so no need to re-pause.
                return false;
            }
        }
    }

    /**
     * Tracks a request to resume the video for a source (see {@link #SOURCE_DATA_ENABLED},
     * {@link #SOURCE_INCALL}) and determines whether a resume request should be issued to the
     * video provider.
     *
     * We want to issue a resume request to the provider when we have issued a corresponding
     * resume for each previously issued pause.
     *
     * @param source The source of the resume request.
     * @return {@code true} if a resume should be issued to the
     *      {@link ImsVideoCallProvider}, {@code false} otherwise.
     */
    public boolean shouldResumeVideoFor(int source) {
        synchronized (mPauseRequestsLock) {
            boolean wasPaused = isPaused();
            mPauseRequests.remove(source);
            boolean isPaused = isPaused();

            if (wasPaused && !isPaused) {
                Log.i(LOG_TAG, String.format(
                        "shouldResumeVideoFor: source=%s, pendingRequests=%s - should resume",
                        sourceToString(source), sourcesToString(mPauseRequests)));
                // This was the last pause request, so resume video.
                return true;
            } else if (wasPaused && isPaused) {
                Log.i(LOG_TAG, String.format(
                        "shouldResumeVideoFor: source=%s, pendingRequests=%s - stay paused",
                        sourceToString(source), sourcesToString(mPauseRequests)));
                // There are still pending pause requests, so don't resume.
                return false;
            } else {
                Log.i(LOG_TAG, String.format(
                        "shouldResumeVideoFor: source=%s, pendingRequests=%s - not paused",
                        sourceToString(source), sourcesToString(mPauseRequests)));
                // Although there are no pending pause requests, it is possible that we cleared the
                // pause tracker because the video state reported we're un-paused.  In this case it
                // is benign to just allow the resume request to be sent since it'll have no effect.
                // Re-writing it to squelch the resume would end up causing it to be a pause
                // request, which is bad.
                return true;
            }
        }
    }

    /**
     * @return {@code true} if the video should be paused, {@code false} otherwise.
     */
    public boolean isPaused() {
        synchronized (mPauseRequestsLock) {
            return !mPauseRequests.isEmpty();
        }
    }

    /**
     * @param source the source of the pause.
     * @return {@code true} if the specified source initiated a pause request and the video is
     *      currently paused, {@code false} otherwise.
     */
    public boolean wasVideoPausedFromSource(int source) {
        synchronized (mPauseRequestsLock) {
            return mPauseRequests.contains(source);
        }
    }

    /**
     * Clears pending pause requests for the tracker.
     */
    public void clearPauseRequests() {
        synchronized (mPauseRequestsLock) {
            mPauseRequests.clear();
        }
    }

    /**
     * Returns a string equivalent of a {@code SOURCE_*} constant.
     *
     * @param source A {@code SOURCE_*} constant.
     * @return String equivalent of the source.
     */
    private String sourceToString(int source) {
        switch (source) {
            case SOURCE_DATA_ENABLED:
                return SOURCE_DATA_ENABLED_STR;
            case SOURCE_INCALL:
                return SOURCE_INCALL_STR;
        }
        return "unknown";
    }

    /**
     * Returns a comma separated list of sources.
     *
     * @param sources The sources.
     * @return Comma separated list of sources.
     */
    private String sourcesToString(Collection<Integer> sources) {
        synchronized (mPauseRequestsLock) {
            return sources.stream()
                    .map(source -> sourceToString(source))
                    .collect(Collectors.joining(", "));
        }
    }
}
