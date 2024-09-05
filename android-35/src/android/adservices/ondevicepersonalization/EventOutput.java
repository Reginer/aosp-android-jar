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
import android.annotation.Nullable;

import com.android.adservices.ondevicepersonalization.flags.Flags;
import com.android.ondevicepersonalization.internal.util.DataClass;

/**
 *  The result returned by {@link IsolatedWorker#onEvent(EventInput, android.os.OutcomeReceiver)}.
 */
@FlaggedApi(Flags.FLAG_ON_DEVICE_PERSONALIZATION_APIS_ENABLED)
@DataClass(genBuilder = true, genEqualsHashCode = true)
public final class EventOutput {
    /**
     * An {@link EventLogRecord} to be written to the EVENTS table, if not null. Each
     * {@link EventLogRecord} is associated with a row in an existing {@link RequestLogRecord} that
     * has been written to the REQUESTS table.
     */
    @DataClass.MaySetToNull
    @Nullable EventLogRecord mEventLogRecord = null;



    // Code below generated by codegen v1.0.23.
    //
    // DO NOT MODIFY!
    // CHECKSTYLE:OFF Generated code
    //
    // To regenerate run:
    // $ codegen $ANDROID_BUILD_TOP/packages/modules/OnDevicePersonalization/framework/java/android/adservices/ondevicepersonalization/EventOutput.java
    //
    // To exclude the generated code from IntelliJ auto-formatting enable (one-time):
    //   Settings > Editor > Code Style > Formatter Control
    //@formatter:off


    @DataClass.Generated.Member
    /* package-private */ EventOutput(
            @Nullable EventLogRecord eventLogRecord) {
        this.mEventLogRecord = eventLogRecord;

        // onConstructed(); // You can define this method to get a callback
    }

    /**
     * An {@link EventLogRecord} to be written to the EVENTS table, if not null. Each
     * {@link EventLogRecord} is associated with a row in an existing {@link RequestLogRecord} that
     * has been written to the REQUESTS table.
     */
    @DataClass.Generated.Member
    public @Nullable EventLogRecord getEventLogRecord() {
        return mEventLogRecord;
    }

    @Override
    @DataClass.Generated.Member
    public boolean equals(@Nullable Object o) {
        // You can override field equality logic by defining either of the methods like:
        // boolean fieldNameEquals(EventOutput other) { ... }
        // boolean fieldNameEquals(FieldType otherValue) { ... }

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @SuppressWarnings("unchecked")
        EventOutput that = (EventOutput) o;
        //noinspection PointlessBooleanExpression
        return true
                && java.util.Objects.equals(mEventLogRecord, that.mEventLogRecord);
    }

    @Override
    @DataClass.Generated.Member
    public int hashCode() {
        // You can override field hashCode logic by defining methods like:
        // int fieldNameHashCode() { ... }

        int _hash = 1;
        _hash = 31 * _hash + java.util.Objects.hashCode(mEventLogRecord);
        return _hash;
    }

    /**
     * A builder for {@link EventOutput}
     */
    @SuppressWarnings("WeakerAccess")
    @DataClass.Generated.Member
    public static final class Builder {

        private @Nullable EventLogRecord mEventLogRecord;

        private long mBuilderFieldsSet = 0L;

        public Builder() {
        }

        /**
         * An {@link EventLogRecord} to be written to the EVENTS table, if not null. Each
         * {@link EventLogRecord} is associated with a row in an existing {@link RequestLogRecord} that
         * has been written to the REQUESTS table.
         */
        @DataClass.Generated.Member
        public @android.annotation.NonNull Builder setEventLogRecord(@Nullable EventLogRecord value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x1;
            mEventLogRecord = value;
            return this;
        }

        /** Builds the instance. This builder should not be touched after calling this! */
        public @android.annotation.NonNull EventOutput build() {
            checkNotUsed();
            mBuilderFieldsSet |= 0x2; // Mark builder used

            if ((mBuilderFieldsSet & 0x1) == 0) {
                mEventLogRecord = null;
            }
            EventOutput o = new EventOutput(
                    mEventLogRecord);
            return o;
        }

        private void checkNotUsed() {
            if ((mBuilderFieldsSet & 0x2) != 0) {
                throw new IllegalStateException(
                        "This Builder should not be reused. Use a new Builder instance instead");
            }
        }
    }

    @DataClass.Generated(
            time = 1707253681044L,
            codegenVersion = "1.0.23",
            sourceFile = "packages/modules/OnDevicePersonalization/framework/java/android/adservices/ondevicepersonalization/EventOutput.java",
            inputSignatures = " @com.android.ondevicepersonalization.internal.util.DataClass.MaySetToNull @android.annotation.Nullable android.adservices.ondevicepersonalization.EventLogRecord mEventLogRecord\nclass EventOutput extends java.lang.Object implements []\n@com.android.ondevicepersonalization.internal.util.DataClass(genBuilder=true, genEqualsHashCode=true)")
    @Deprecated
    private void __metadata() {}


    //@formatter:on
    // End of generated code

}