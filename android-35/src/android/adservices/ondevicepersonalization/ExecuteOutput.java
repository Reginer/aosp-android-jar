/*
 * Copyright (C) 2022 The Android Open Source Project
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

import com.android.adservices.ondevicepersonalization.flags.Flags;
import com.android.ondevicepersonalization.internal.util.AnnotationValidations;
import com.android.ondevicepersonalization.internal.util.DataClass;

import java.util.Collections;
import java.util.List;

/**
 * The result returned by
 * {@link IsolatedWorker#onExecute(ExecuteInput, android.os.OutcomeReceiver)} in response to a call to
 * {@code OnDevicePersonalizationManager#execute(ComponentName, PersistableBundle,
 * java.util.concurrent.Executor, OutcomeReceiver)}
 * from a client app.
 */
@FlaggedApi(Flags.FLAG_ON_DEVICE_PERSONALIZATION_APIS_ENABLED)
@DataClass(genBuilder = true, genEqualsHashCode = true)
public final class ExecuteOutput {
    /**
     * Persistent data to be written to the REQUESTS table after
     * {@link IsolatedWorker#onExecute(ExecuteInput, android.os.OutcomeReceiver)}
     * completes. If null, no persistent data will be written.
     */
    @DataClass.MaySetToNull
    @Nullable private RequestLogRecord mRequestLogRecord = null;

    /**
     * A {@link RenderingConfig} object that contains information about the content to be rendered
     * in the client app view. Can be null if no content is to be rendered.
     */
    @DataClass.MaySetToNull
    @Nullable private RenderingConfig mRenderingConfig = null;

    /**
     * A list of {@link EventLogRecord} objects to be written to the EVENTS table. Each
     * {@link EventLogRecord} must be associated with an existing {@link RequestLogRecord} in
     * the REQUESTS table, specified using
     * {@link EventLogRecord.Builder#setRequestLogRecord(RequestLogRecord)}.
     * If the {@link RequestLogRecord} is not specified, the {@link EventLogRecord} will not be
     * written.
     */
    @DataClass.PluralOf("eventLogRecord")
    @NonNull private List<EventLogRecord> mEventLogRecords = Collections.emptyList();

    /**
     * A byte array that an {@link IsolatedService} may optionally return to to a calling app,
     * by setting this field to a non-null value.
     * The contents of this array will be returned to the caller of
     * {@link OnDevicePersonalizationManager#execute(ComponentName, PersistableBundle, java.util.concurrent.Executor, OutcomeReceiver)}
     * if returning data from isolated processes is allowed by policy and the
     * (calling app package, isolated service package) pair is present in an allowlist that
     * permits data to be returned.
     */
    @DataClass.MaySetToNull
    @Nullable private byte[] mOutputData = null;



    // Code below generated by codegen v1.0.23.
    //
    // DO NOT MODIFY!
    // CHECKSTYLE:OFF Generated code
    //
    // To regenerate run:
    // $ codegen $ANDROID_BUILD_TOP/packages/modules/OnDevicePersonalization/framework/java/android/adservices/ondevicepersonalization/ExecuteOutput.java
    //
    // To exclude the generated code from IntelliJ auto-formatting enable (one-time):
    //   Settings > Editor > Code Style > Formatter Control
    //@formatter:off


    @DataClass.Generated.Member
    /* package-private */ ExecuteOutput(
            @Nullable RequestLogRecord requestLogRecord,
            @Nullable RenderingConfig renderingConfig,
            @NonNull List<EventLogRecord> eventLogRecords,
            @Nullable byte[] outputData) {
        this.mRequestLogRecord = requestLogRecord;
        this.mRenderingConfig = renderingConfig;
        this.mEventLogRecords = eventLogRecords;
        AnnotationValidations.validate(
                NonNull.class, null, mEventLogRecords);
        this.mOutputData = outputData;

        // onConstructed(); // You can define this method to get a callback
    }

    /**
     * Persistent data to be written to the REQUESTS table after
     * {@link IsolatedWorker#onExecute(ExecuteInput, android.os.OutcomeReceiver)}
     * completes. If null, no persistent data will be written.
     */
    @DataClass.Generated.Member
    public @Nullable RequestLogRecord getRequestLogRecord() {
        return mRequestLogRecord;
    }

    /**
     * A {@link RenderingConfig} object that contains information about the content to be rendered
     * in the client app view. Can be null if no content is to be rendered.
     */
    @DataClass.Generated.Member
    public @Nullable RenderingConfig getRenderingConfig() {
        return mRenderingConfig;
    }

    /**
     * A list of {@link EventLogRecord} objects to be written to the EVENTS table. Each
     * {@link EventLogRecord} must be associated with an existing {@link RequestLogRecord} in
     * the REQUESTS table, specified using
     * {@link EventLogRecord.Builder#setRequestLogRecord(RequestLogRecord)}.
     * If the {@link RequestLogRecord} is not specified, the {@link EventLogRecord} will not be
     * written.
     */
    @DataClass.Generated.Member
    public @NonNull List<EventLogRecord> getEventLogRecords() {
        return mEventLogRecords;
    }

    /**
     * A byte array that an {@link IsolatedService} may optionally return to to a calling app,
     * by setting this field to a non-null value.
     * The contents of this array will be returned to the caller of
     * {@link OnDevicePersonalizationManager#execute(ComponentName, PersistableBundle, java.util.concurrent.Executor, OutcomeReceiver)}
     * if returning data from isolated processes is allowed by policy and the
     * (calling app package, isolated service package) pair is present in an allowlist that
     * permits data to be returned.
     */
    @DataClass.Generated.Member
    public @Nullable byte[] getOutputData() {
        return mOutputData;
    }

    @Override
    @DataClass.Generated.Member
    public boolean equals(@Nullable Object o) {
        // You can override field equality logic by defining either of the methods like:
        // boolean fieldNameEquals(ExecuteOutput other) { ... }
        // boolean fieldNameEquals(FieldType otherValue) { ... }

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @SuppressWarnings("unchecked")
        ExecuteOutput that = (ExecuteOutput) o;
        //noinspection PointlessBooleanExpression
        return true
                && java.util.Objects.equals(mRequestLogRecord, that.mRequestLogRecord)
                && java.util.Objects.equals(mRenderingConfig, that.mRenderingConfig)
                && java.util.Objects.equals(mEventLogRecords, that.mEventLogRecords)
                && java.util.Arrays.equals(mOutputData, that.mOutputData);
    }

    @Override
    @DataClass.Generated.Member
    public int hashCode() {
        // You can override field hashCode logic by defining methods like:
        // int fieldNameHashCode() { ... }

        int _hash = 1;
        _hash = 31 * _hash + java.util.Objects.hashCode(mRequestLogRecord);
        _hash = 31 * _hash + java.util.Objects.hashCode(mRenderingConfig);
        _hash = 31 * _hash + java.util.Objects.hashCode(mEventLogRecords);
        _hash = 31 * _hash + java.util.Arrays.hashCode(mOutputData);
        return _hash;
    }

    /**
     * A builder for {@link ExecuteOutput}
     */
    @SuppressWarnings("WeakerAccess")
    @DataClass.Generated.Member
    public static final class Builder {

        private @Nullable RequestLogRecord mRequestLogRecord;
        private @Nullable RenderingConfig mRenderingConfig;
        private @NonNull List<EventLogRecord> mEventLogRecords;
        private @Nullable byte[] mOutputData;

        private long mBuilderFieldsSet = 0L;

        public Builder() {
        }

        /**
         * Persistent data to be written to the REQUESTS table after
         * {@link IsolatedWorker#onExecute(ExecuteInput, android.os.OutcomeReceiver)}
         * completes. If null, no persistent data will be written.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setRequestLogRecord(@Nullable RequestLogRecord value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x1;
            mRequestLogRecord = value;
            return this;
        }

        /**
         * A {@link RenderingConfig} object that contains information about the content to be rendered
         * in the client app view. Can be null if no content is to be rendered.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setRenderingConfig(@Nullable RenderingConfig value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x2;
            mRenderingConfig = value;
            return this;
        }

        /**
         * A list of {@link EventLogRecord} objects to be written to the EVENTS table. Each
         * {@link EventLogRecord} must be associated with an existing {@link RequestLogRecord} in
         * the REQUESTS table, specified using
         * {@link EventLogRecord.Builder#setRequestLogRecord(RequestLogRecord)}.
         * If the {@link RequestLogRecord} is not specified, the {@link EventLogRecord} will not be
         * written.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setEventLogRecords(@NonNull List<EventLogRecord> value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x4;
            mEventLogRecords = value;
            return this;
        }

        /** @see #setEventLogRecords */
        @DataClass.Generated.Member
        public @NonNull Builder addEventLogRecord(@NonNull EventLogRecord value) {
            if (mEventLogRecords == null) setEventLogRecords(new java.util.ArrayList<>());
            mEventLogRecords.add(value);
            return this;
        }

        /**
         * A byte array that an {@link IsolatedService} may optionally return to to a calling app,
         * by setting this field to a non-null value.
         * The contents of this array will be returned to the caller of
         * {@link OnDevicePersonalizationManager#execute(ComponentName, PersistableBundle, java.util.concurrent.Executor, OutcomeReceiver)}
         * if returning data from isolated processes is allowed by policy and the
         * (calling app package, isolated service package) pair is present in an allowlist that
         * permits data to be returned.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setOutputData(@Nullable byte... value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x8;
            mOutputData = value;
            return this;
        }

        /** Builds the instance. This builder should not be touched after calling this! */
        public @NonNull ExecuteOutput build() {
            checkNotUsed();
            mBuilderFieldsSet |= 0x10; // Mark builder used

            if ((mBuilderFieldsSet & 0x1) == 0) {
                mRequestLogRecord = null;
            }
            if ((mBuilderFieldsSet & 0x2) == 0) {
                mRenderingConfig = null;
            }
            if ((mBuilderFieldsSet & 0x4) == 0) {
                mEventLogRecords = Collections.emptyList();
            }
            if ((mBuilderFieldsSet & 0x8) == 0) {
                mOutputData = null;
            }
            ExecuteOutput o = new ExecuteOutput(
                    mRequestLogRecord,
                    mRenderingConfig,
                    mEventLogRecords,
                    mOutputData);
            return o;
        }

        private void checkNotUsed() {
            if ((mBuilderFieldsSet & 0x10) != 0) {
                throw new IllegalStateException(
                        "This Builder should not be reused. Use a new Builder instance instead");
            }
        }
    }

    @DataClass.Generated(
            time = 1707251143585L,
            codegenVersion = "1.0.23",
            sourceFile = "packages/modules/OnDevicePersonalization/framework/java/android/adservices/ondevicepersonalization/ExecuteOutput.java",
            inputSignatures = "private @com.android.ondevicepersonalization.internal.util.DataClass.MaySetToNull @android.annotation.Nullable android.adservices.ondevicepersonalization.RequestLogRecord mRequestLogRecord\nprivate @com.android.ondevicepersonalization.internal.util.DataClass.MaySetToNull @android.annotation.Nullable android.adservices.ondevicepersonalization.RenderingConfig mRenderingConfig\nprivate @com.android.ondevicepersonalization.internal.util.DataClass.PluralOf(\"eventLogRecord\") @android.annotation.NonNull java.util.List<android.adservices.ondevicepersonalization.EventLogRecord> mEventLogRecords\nprivate @com.android.ondevicepersonalization.internal.util.DataClass.MaySetToNull @android.annotation.Nullable byte[] mOutputData\nclass ExecuteOutput extends java.lang.Object implements []\n@com.android.ondevicepersonalization.internal.util.DataClass(genBuilder=true, genEqualsHashCode=true)")
    @Deprecated
    private void __metadata() {}


    //@formatter:on
    // End of generated code

}
