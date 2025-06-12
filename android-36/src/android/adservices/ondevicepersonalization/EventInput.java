/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.adservices.ondevicepersonalization;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.PersistableBundle;

import com.android.adservices.ondevicepersonalization.flags.Flags;

import java.util.Objects;

/**
 * The input data for {@link
 * IsolatedWorker#onEvent(EventInput, android.os.OutcomeReceiver)}.
 */
public final class EventInput {
    /**
     * The {@link RequestLogRecord} that was returned as a result of
     * {@link IsolatedWorker#onExecute(ExecuteInput, android.os.OutcomeReceiver)}.
     */
    @Nullable private RequestLogRecord mRequestLogRecord = null;

    /**
     * The Event URL parameters that the service passed to {@link
     * EventUrlProvider#createEventTrackingUrlWithResponse(PersistableBundle, byte[], String)}
     * or {@link EventUrlProvider#createEventTrackingUrlWithRedirect(PersistableBundle, Uri)}.
     */
    @NonNull private PersistableBundle mParameters = PersistableBundle.EMPTY;

    /** @hide */
    public EventInput(@NonNull EventInputParcel parcel) {
        this(parcel.getRequestLogRecord(), parcel.getParameters());
    }

    /**
     * Creates a new EventInput.
     *
     * @param requestLogRecord
     *   The {@link RequestLogRecord} that was returned as a result of
     *   {@link IsolatedWorker#onExecute(ExecuteInput, android.os.OutcomeReceiver)}.
     * @param parameters
     *   The Event URL parameters that the service passed to {@link
     *   EventUrlProvider#createEventTrackingUrlWithResponse(PersistableBundle, byte[], String)}
     *   or {@link EventUrlProvider#createEventTrackingUrlWithRedirect(PersistableBundle, Uri)}.
     */
    @FlaggedApi(Flags.FLAG_DATA_CLASS_MISSING_CTORS_AND_GETTERS_ENABLED)
    public EventInput(
            @Nullable RequestLogRecord requestLogRecord,
            @NonNull PersistableBundle parameters) {
        this.mRequestLogRecord = requestLogRecord;
        this.mParameters = Objects.requireNonNull(parameters);
    }

    /**
     * The {@link RequestLogRecord} that was returned as a result of
     * {@link IsolatedWorker#onExecute(ExecuteInput, android.os.OutcomeReceiver)}.
     */
    public @Nullable RequestLogRecord getRequestLogRecord() {
        return mRequestLogRecord;
    }

    /**
     * The Event URL parameters that the service passed to {@link
     * EventUrlProvider#createEventTrackingUrlWithResponse(PersistableBundle, byte[], String)}
     * or {@link EventUrlProvider#createEventTrackingUrlWithRedirect(PersistableBundle, Uri)}.
     */
    public @NonNull PersistableBundle getParameters() {
        return mParameters;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @SuppressWarnings("unchecked")
        EventInput that = (EventInput) o;
        //noinspection PointlessBooleanExpression
        return true
                && java.util.Objects.equals(mRequestLogRecord, that.mRequestLogRecord)
                && java.util.Objects.equals(mParameters, that.mParameters);
    }

    @Override
    public int hashCode() {
        int _hash = 1;
        _hash = 31 * _hash + java.util.Objects.hashCode(mRequestLogRecord);
        _hash = 31 * _hash + java.util.Objects.hashCode(mParameters);
        return _hash;
    }
}
