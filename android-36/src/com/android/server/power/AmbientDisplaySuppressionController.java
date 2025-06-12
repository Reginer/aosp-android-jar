/**
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

package com.android.server.power;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;

import com.android.internal.statusbar.IStatusBarService;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Communicates with System UI to suppress the ambient display.
 */
public class AmbientDisplaySuppressionController {
    private static final String TAG = "AmbientDisplaySuppressionController";

    private final Set<Pair<String, Integer>> mSuppressionTokens;
    private final AmbientDisplaySuppressionChangedCallback mCallback;
    private IStatusBarService mStatusBarService;

    /** Interface to get a list of available logical devices. */
    interface AmbientDisplaySuppressionChangedCallback {
        /**
         * Called when the suppression state changes.
         *
         * @param isSuppressed Whether ambient is suppressed.
         */
        void onSuppressionChanged(boolean isSuppressed);
    }

    AmbientDisplaySuppressionController(
            @NonNull AmbientDisplaySuppressionChangedCallback callback) {
        mSuppressionTokens = Collections.synchronizedSet(new ArraySet<>());
        mCallback = requireNonNull(callback);
    }

    /**
     * Suppresses ambient display.
     *
     * @param token A persistible identifier for the ambient display suppression.
     * @param callingUid The uid of the calling application.
     * @param suppress If true, suppresses the ambient display. Otherwise, unsuppresses it.
     */
    public void suppress(@NonNull String token, int callingUid, boolean suppress) {
        Pair<String, Integer> suppressionToken = Pair.create(requireNonNull(token), callingUid);
        final boolean wasSuppressed = isSuppressed();

        if (suppress) {
            mSuppressionTokens.add(suppressionToken);
        } else {
            mSuppressionTokens.remove(suppressionToken);
        }

        final boolean isSuppressed = isSuppressed();
        if (isSuppressed != wasSuppressed) {
            mCallback.onSuppressionChanged(isSuppressed);
        }

        try {
            synchronized (mSuppressionTokens) {
                getStatusBar().suppressAmbientDisplay(isSuppressed);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to suppress ambient display", e);
        }
    }

    /**
     * Returns the tokens used to suppress ambient display through
     * {@link #suppress(String, int, boolean)}.
     *
     * @param callingUid The uid of the calling application.
     */
    List<String> getSuppressionTokens(int callingUid) {
        List<String> result = new ArrayList<>();
        synchronized (mSuppressionTokens) {
            for (Pair<String, Integer> token : mSuppressionTokens) {
                if (token.second == callingUid) {
                    result.add(token.first);
                }
            }
        }
        return result;
    }

    /**
     * Returns whether ambient display is suppressed for the given token.
     *
     * @param token A persistible identifier for the ambient display suppression.
     * @param callingUid The uid of the calling application.
     */
    public boolean isSuppressed(@NonNull String token, int callingUid) {
        return mSuppressionTokens.contains(Pair.create(requireNonNull(token), callingUid));
    }

    /**
     * Returns whether ambient display is suppressed.
     */
    public boolean isSuppressed() {
        return !mSuppressionTokens.isEmpty();
    }

    /**
     * Dumps the state of ambient display suppression and the list of suppression tokens into
     * {@code pw}.
     */
    public void dump(PrintWriter pw) {
        pw.println("AmbientDisplaySuppressionController:");
        pw.println(" ambientDisplaySuppressed=" + isSuppressed());
        pw.println(" mSuppressionTokens=" + mSuppressionTokens);
    }

    private synchronized IStatusBarService getStatusBar() {
        if (mStatusBarService == null) {
            mStatusBarService = IStatusBarService.Stub.asInterface(
                    ServiceManager.getService(Context.STATUS_BAR_SERVICE));
        }
        return mStatusBarService;
    }
}
