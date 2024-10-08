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

/**
 * The input data for {@link
 * IsolatedWorker#onDownloadCompleted(DownloadCompletedInput, android.os.OutcomeReceiver)}.
 *
 */
@FlaggedApi(Flags.FLAG_ON_DEVICE_PERSONALIZATION_APIS_ENABLED)
@DataClass(genHiddenBuilder = true, genEqualsHashCode = true)
public final class DownloadCompletedInput {
    /**
     * A {@link KeyValueStore} that contains the downloaded content.
     */
    @NonNull KeyValueStore mDownloadedContents;



    // Code below generated by codegen v1.0.23.
    //
    // DO NOT MODIFY!
    // CHECKSTYLE:OFF Generated code
    //
    // To regenerate run:
    // $ codegen $ANDROID_BUILD_TOP/packages/modules/OnDevicePersonalization/framework/java/android/adservices/ondevicepersonalization/DownloadCompletedInput.java
    //
    // To exclude the generated code from IntelliJ auto-formatting enable (one-time):
    //   Settings > Editor > Code Style > Formatter Control
    //@formatter:off


    @DataClass.Generated.Member
    /* package-private */ DownloadCompletedInput(
            @NonNull KeyValueStore downloadedContents) {
        this.mDownloadedContents = downloadedContents;
        AnnotationValidations.validate(
                NonNull.class, null, mDownloadedContents);

        // onConstructed(); // You can define this method to get a callback
    }

    /**
     * Map containing downloaded keys and values
     */
    @DataClass.Generated.Member
    public @NonNull KeyValueStore getDownloadedContents() {
        return mDownloadedContents;
    }

    @Override
    @DataClass.Generated.Member
    public boolean equals(@Nullable Object o) {
        // You can override field equality logic by defining either of the methods like:
        // boolean fieldNameEquals(DownloadCompletedInput other) { ... }
        // boolean fieldNameEquals(FieldType otherValue) { ... }

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @SuppressWarnings("unchecked")
        DownloadCompletedInput that = (DownloadCompletedInput) o;
        //noinspection PointlessBooleanExpression
        return true
                && java.util.Objects.equals(mDownloadedContents, that.mDownloadedContents);
    }

    @Override
    @DataClass.Generated.Member
    public int hashCode() {
        // You can override field hashCode logic by defining methods like:
        // int fieldNameHashCode() { ... }

        int _hash = 1;
        _hash = 31 * _hash + java.util.Objects.hashCode(mDownloadedContents);
        return _hash;
    }

    /**
     * A builder for {@link DownloadCompletedInput}
     * @hide
     */
    @SuppressWarnings("WeakerAccess")
    @DataClass.Generated.Member
    public static final class Builder {

        private @NonNull KeyValueStore mDownloadedContents;

        private long mBuilderFieldsSet = 0L;

        public Builder() {
        }

        /**
         * Creates a new Builder.
         *
         * @param downloadedContents
         *   Map containing downloaded keys and values
         */
        public Builder(
                @NonNull KeyValueStore downloadedContents) {
            mDownloadedContents = downloadedContents;
            AnnotationValidations.validate(
                    NonNull.class, null, mDownloadedContents);
        }

        /**
         * Map containing downloaded keys and values
         */
        @DataClass.Generated.Member
        public @NonNull Builder setDownloadedContents(@NonNull KeyValueStore value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x1;
            mDownloadedContents = value;
            return this;
        }

        /** Builds the instance. This builder should not be touched after calling this! */
        public @NonNull DownloadCompletedInput build() {
            checkNotUsed();
            mBuilderFieldsSet |= 0x2; // Mark builder used

            DownloadCompletedInput o = new DownloadCompletedInput(
                    mDownloadedContents);
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
            time = 1706205792643L,
            codegenVersion = "1.0.23",
            sourceFile = "packages/modules/OnDevicePersonalization/framework/java/android/adservices/ondevicepersonalization/DownloadCompletedInput.java",
            inputSignatures = " @android.annotation.NonNull android.adservices.ondevicepersonalization.KeyValueStore mDownloadedContents\nclass DownloadCompletedInput extends java.lang.Object implements []\n@com.android.ondevicepersonalization.internal.util.DataClass(genHiddenBuilder=true, genEqualsHashCode=true)")
    @Deprecated
    private void __metadata() {}


    //@formatter:on
    // End of generated code

}
