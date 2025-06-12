/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.view.inputmethod;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ActivityThread;
import android.app.compat.CompatChanges;
import android.compat.annotation.ChangeId;
import android.compat.annotation.EnabledSince;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.Display;
import android.widget.inline.InlinePresentationSpec;

import com.android.internal.util.DataClass;
import com.android.internal.util.Preconditions;
import com.android.internal.widget.InlinePresentationStyleUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents an inline suggestion request made by one app to get suggestions from the
 * other source. See {@link InlineSuggestion} for more information.
 */
@DataClass(genEqualsHashCode = true, genToString = true, genBuilder = true)
public final class InlineSuggestionsRequest implements Parcelable {

    /** Constant used to indicate not putting a cap on the number of suggestions to return. */
    public static final int SUGGESTION_COUNT_UNLIMITED = Integer.MAX_VALUE;

    /**
     * Max number of suggestions expected from the response. It must be a positive value.
     * Defaults to {@code SUGGESTION_COUNT_UNLIMITED} if not set.
     *
     * <p>In practice, it is recommended that the max suggestion count does not exceed <b>5</b>
     * for performance reasons.</p>
     */
    private final int mMaxSuggestionCount;

    /**
     * The {@link InlinePresentationSpec} for each suggestion in the response. If the max suggestion
     * count is larger than the number of specs in the list, then the last spec is used for the
     * remainder of the suggestions. The list should not be empty.
     */
    private final @NonNull List<InlinePresentationSpec> mInlinePresentationSpecs;

    /**
     * The package name of the app that requests for the inline suggestions and will host the
     * embedded suggestion views. The app does not have to set the value for the field because
     * it'll be set by the system for safety reasons.
     */
    private @NonNull String mHostPackageName;

    /**
     * The IME provided locales for the request. If non-empty, the inline suggestions should
     * return languages from the supported locales. If not provided, it'll default to be empty if
     * target SDK is S or above, and default to system locale otherwise.
     *
     * <p>Note for Autofill Providers: It is <b>recommended</b> for the returned inline suggestions
     * to have one locale to guarantee consistent UI rendering.</p>
     */
    private @NonNull LocaleList mSupportedLocales;

    /**
     * The extras state propagated from the IME to pass extra data.
     *
     * <p>Note: There should be no remote objects in the bundle, all included remote objects will
     * be removed from the bundle before transmission.</p>
     */
    private @NonNull Bundle mExtras;

    /**
     * The host input token of the IME that made the request. This will be set by the system for
     * safety reasons.
     *
     * @hide
     */
    private @Nullable IBinder mHostInputToken;

    /**
     * The host display id of the IME that made the request. This will be set by the system for
     * safety reasons.
     *
     * @hide
     */
    private int mHostDisplayId;

    /**
     * Specifies the UI specification for the inline suggestion tooltip in the response.
     */
    private @Nullable InlinePresentationSpec mInlineTooltipPresentationSpec;

    /**
     * @hide
     * @see {@link #mHostInputToken}.
     */
    public void setHostInputToken(IBinder hostInputToken) {
        mHostInputToken = hostInputToken;
    }

    private boolean extrasEquals(@NonNull Bundle extras) {
        return InlinePresentationStyleUtils.bundleEquals(mExtras, extras);
    }

    // TODO(b/149609075): remove once IBinder parcelling is natively supported
    private void parcelHostInputToken(@NonNull Parcel parcel, int flags) {
        parcel.writeStrongBinder(mHostInputToken);
    }

    // TODO(b/149609075): remove once IBinder parcelling is natively supported
    private @Nullable IBinder unparcelHostInputToken(Parcel parcel) {
        return parcel.readStrongBinder();
    }

    /**
     * @hide
     * @see {@link #mHostDisplayId}.
     */
    public void setHostDisplayId(int hostDisplayId) {
        mHostDisplayId = hostDisplayId;
    }

    private void onConstructed() {
        Preconditions.checkState(!mInlinePresentationSpecs.isEmpty());
        Preconditions.checkState(mMaxSuggestionCount >= mInlinePresentationSpecs.size());
    }

    /**
     * Removes the remote objects from the bundles within the {@Code mExtras} and the
     * {@code mInlinePresentationSpecs}.
     *
     * @hide
     */
    public void filterContentTypes() {
        InlinePresentationStyleUtils.filterContentTypes(mExtras);
        for (int i = 0; i < mInlinePresentationSpecs.size(); i++) {
            mInlinePresentationSpecs.get(i).filterContentTypes();
        }

        if (mInlineTooltipPresentationSpec != null) {
            mInlineTooltipPresentationSpec.filterContentTypes();
        }
    }

    private static int defaultMaxSuggestionCount() {
        return SUGGESTION_COUNT_UNLIMITED;
    }

    private static String defaultHostPackageName() {
        return ActivityThread.currentPackageName();
    }

    private static InlinePresentationSpec defaultInlineTooltipPresentationSpec() {
        return null;
    }

    /**
     * The {@link InlineSuggestionsRequest#getSupportedLocales()} now returns empty locale list when
     * it's not set, instead of the default system locale.
     */
    @ChangeId
    @EnabledSince(targetSdkVersion = Build.VERSION_CODES.S)
    private static final long IME_AUTOFILL_DEFAULT_SUPPORTED_LOCALES_IS_EMPTY = 169273070L;

    private static LocaleList defaultSupportedLocales() {
        if (CompatChanges.isChangeEnabled(IME_AUTOFILL_DEFAULT_SUPPORTED_LOCALES_IS_EMPTY)) {
            return LocaleList.getEmptyLocaleList();
        }
        return LocaleList.getDefault();
    }

    @Nullable
    private static IBinder defaultHostInputToken() {
        return null;
    }

    @Nullable
    private static int defaultHostDisplayId() {
        return Display.INVALID_DISPLAY;
    }

    @NonNull
    private static Bundle defaultExtras() {
        return Bundle.EMPTY;
    }

    /** @hide */
    abstract static class BaseBuilder {
        abstract Builder setInlinePresentationSpecs(
                @NonNull List<android.widget.inline.InlinePresentationSpec> specs);

        abstract Builder setHostPackageName(@Nullable String value);

        abstract Builder setHostInputToken(IBinder hostInputToken);

        abstract Builder setHostDisplayId(int value);
    }

    // Code below generated by codegen v1.0.23.
    //
    // DO NOT MODIFY!
    // CHECKSTYLE:OFF Generated code
    //
    // To regenerate run:
    // $ codegen $ANDROID_BUILD_TOP/./frameworks/base/core/java/android/view/inputmethod/InlineSuggestionsRequest.java
    //
    // To exclude the generated code from IntelliJ auto-formatting enable (one-time):
    //   Settings > Editor > Code Style > Formatter Control
    //@formatter:off


    @DataClass.Generated.Member
    /* package-private */ InlineSuggestionsRequest(
            int maxSuggestionCount,
            @NonNull List<InlinePresentationSpec> inlinePresentationSpecs,
            @NonNull String hostPackageName,
            @NonNull LocaleList supportedLocales,
            @NonNull Bundle extras,
            @Nullable IBinder hostInputToken,
            int hostDisplayId,
            @Nullable InlinePresentationSpec inlineTooltipPresentationSpec) {
        this.mMaxSuggestionCount = maxSuggestionCount;
        this.mInlinePresentationSpecs = inlinePresentationSpecs;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mInlinePresentationSpecs);
        this.mHostPackageName = hostPackageName;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mHostPackageName);
        this.mSupportedLocales = supportedLocales;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mSupportedLocales);
        this.mExtras = extras;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mExtras);
        this.mHostInputToken = hostInputToken;
        this.mHostDisplayId = hostDisplayId;
        this.mInlineTooltipPresentationSpec = inlineTooltipPresentationSpec;

        onConstructed();
    }

    /**
     * Max number of suggestions expected from the response. It must be a positive value.
     * Defaults to {@code SUGGESTION_COUNT_UNLIMITED} if not set.
     *
     * <p>In practice, it is recommended that the max suggestion count does not exceed <b>5</b>
     * for performance reasons.</p>
     */
    @DataClass.Generated.Member
    public int getMaxSuggestionCount() {
        return mMaxSuggestionCount;
    }

    /**
     * The {@link InlinePresentationSpec} for each suggestion in the response. If the max suggestion
     * count is larger than the number of specs in the list, then the last spec is used for the
     * remainder of the suggestions. The list should not be empty.
     */
    @DataClass.Generated.Member
    public @NonNull List<InlinePresentationSpec> getInlinePresentationSpecs() {
        return mInlinePresentationSpecs;
    }

    /**
     * The package name of the app that requests for the inline suggestions and will host the
     * embedded suggestion views. The app does not have to set the value for the field because
     * it'll be set by the system for safety reasons.
     */
    @DataClass.Generated.Member
    public @NonNull String getHostPackageName() {
        return mHostPackageName;
    }

    /**
     * The IME provided locales for the request. If non-empty, the inline suggestions should
     * return languages from the supported locales. If not provided, it'll default to be empty if
     * target SDK is S or above, and default to system locale otherwise.
     *
     * <p>Note for Autofill Providers: It is <b>recommended</b> for the returned inline suggestions
     * to have one locale to guarantee consistent UI rendering.</p>
     */
    @DataClass.Generated.Member
    public @NonNull LocaleList getSupportedLocales() {
        return mSupportedLocales;
    }

    /**
     * The extras state propagated from the IME to pass extra data.
     *
     * <p>Note: There should be no remote objects in the bundle, all included remote objects will
     * be removed from the bundle before transmission.</p>
     */
    @DataClass.Generated.Member
    public @NonNull Bundle getExtras() {
        return mExtras;
    }

    /**
     * The host input token of the IME that made the request. This will be set by the system for
     * safety reasons.
     *
     * @hide
     */
    @DataClass.Generated.Member
    public @Nullable IBinder getHostInputToken() {
        return mHostInputToken;
    }

    /**
     * The host display id of the IME that made the request. This will be set by the system for
     * safety reasons.
     *
     * @hide
     */
    @DataClass.Generated.Member
    public int getHostDisplayId() {
        return mHostDisplayId;
    }

    /**
     * Specifies the UI specification for the inline suggestion tooltip in the response.
     */
    @DataClass.Generated.Member
    public @Nullable InlinePresentationSpec getInlineTooltipPresentationSpec() {
        return mInlineTooltipPresentationSpec;
    }

    @Override
    @DataClass.Generated.Member
    public String toString() {
        // You can override field toString logic by defining methods like:
        // String fieldNameToString() { ... }

        return "InlineSuggestionsRequest { " +
                "maxSuggestionCount = " + mMaxSuggestionCount + ", " +
                "inlinePresentationSpecs = " + mInlinePresentationSpecs + ", " +
                "hostPackageName = " + mHostPackageName + ", " +
                "supportedLocales = " + mSupportedLocales + ", " +
                "extras = " + mExtras + ", " +
                "hostInputToken = " + mHostInputToken + ", " +
                "hostDisplayId = " + mHostDisplayId + ", " +
                "inlineTooltipPresentationSpec = " + mInlineTooltipPresentationSpec +
        " }";
    }

    @Override
    @DataClass.Generated.Member
    public boolean equals(@Nullable Object o) {
        // You can override field equality logic by defining either of the methods like:
        // boolean fieldNameEquals(InlineSuggestionsRequest other) { ... }
        // boolean fieldNameEquals(FieldType otherValue) { ... }

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @SuppressWarnings("unchecked")
        InlineSuggestionsRequest that = (InlineSuggestionsRequest) o;
        //noinspection PointlessBooleanExpression
        return true
                && mMaxSuggestionCount == that.mMaxSuggestionCount
                && java.util.Objects.equals(mInlinePresentationSpecs, that.mInlinePresentationSpecs)
                && java.util.Objects.equals(mHostPackageName, that.mHostPackageName)
                && java.util.Objects.equals(mSupportedLocales, that.mSupportedLocales)
                && extrasEquals(that.mExtras)
                && java.util.Objects.equals(mHostInputToken, that.mHostInputToken)
                && mHostDisplayId == that.mHostDisplayId
                && java.util.Objects.equals(mInlineTooltipPresentationSpec, that.mInlineTooltipPresentationSpec);
    }

    @Override
    @DataClass.Generated.Member
    public int hashCode() {
        // You can override field hashCode logic by defining methods like:
        // int fieldNameHashCode() { ... }

        int _hash = 1;
        _hash = 31 * _hash + mMaxSuggestionCount;
        _hash = 31 * _hash + java.util.Objects.hashCode(mInlinePresentationSpecs);
        _hash = 31 * _hash + java.util.Objects.hashCode(mHostPackageName);
        _hash = 31 * _hash + java.util.Objects.hashCode(mSupportedLocales);
        _hash = 31 * _hash + java.util.Objects.hashCode(mExtras);
        _hash = 31 * _hash + java.util.Objects.hashCode(mHostInputToken);
        _hash = 31 * _hash + mHostDisplayId;
        _hash = 31 * _hash + java.util.Objects.hashCode(mInlineTooltipPresentationSpec);
        return _hash;
    }

    @Override
    @DataClass.Generated.Member
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        // You can override field parcelling by defining methods like:
        // void parcelFieldName(Parcel dest, int flags) { ... }

        int flg = 0;
        if (mHostInputToken != null) flg |= 0x20;
        if (mInlineTooltipPresentationSpec != null) flg |= 0x80;
        dest.writeInt(flg);
        dest.writeInt(mMaxSuggestionCount);
        dest.writeParcelableList(mInlinePresentationSpecs, flags);
        dest.writeString(mHostPackageName);
        dest.writeTypedObject(mSupportedLocales, flags);
        dest.writeBundle(mExtras);
        parcelHostInputToken(dest, flags);
        dest.writeInt(mHostDisplayId);
        if (mInlineTooltipPresentationSpec != null) dest.writeTypedObject(mInlineTooltipPresentationSpec, flags);
    }

    @Override
    @DataClass.Generated.Member
    public int describeContents() { return 0; }

    /** @hide */
    @SuppressWarnings({"unchecked", "RedundantCast"})
    @DataClass.Generated.Member
    /* package-private */ InlineSuggestionsRequest(@NonNull Parcel in) {
        // You can override field unparcelling by defining methods like:
        // static FieldType unparcelFieldName(Parcel in) { ... }

        int flg = in.readInt();
        int maxSuggestionCount = in.readInt();
        List<InlinePresentationSpec> inlinePresentationSpecs = new ArrayList<>();
        in.readParcelableList(inlinePresentationSpecs, InlinePresentationSpec.class.getClassLoader());
        String hostPackageName = in.readString();
        LocaleList supportedLocales = (LocaleList) in.readTypedObject(LocaleList.CREATOR);
        Bundle extras = in.readBundle();
        IBinder hostInputToken = unparcelHostInputToken(in);
        int hostDisplayId = in.readInt();
        InlinePresentationSpec inlineTooltipPresentationSpec = (flg & 0x80) == 0 ? null : (InlinePresentationSpec) in.readTypedObject(InlinePresentationSpec.CREATOR);

        this.mMaxSuggestionCount = maxSuggestionCount;
        this.mInlinePresentationSpecs = inlinePresentationSpecs;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mInlinePresentationSpecs);
        this.mHostPackageName = hostPackageName;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mHostPackageName);
        this.mSupportedLocales = supportedLocales;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mSupportedLocales);
        this.mExtras = extras;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mExtras);
        this.mHostInputToken = hostInputToken;
        this.mHostDisplayId = hostDisplayId;
        this.mInlineTooltipPresentationSpec = inlineTooltipPresentationSpec;

        onConstructed();
    }

    @DataClass.Generated.Member
    public static final @NonNull Parcelable.Creator<InlineSuggestionsRequest> CREATOR
            = new Parcelable.Creator<InlineSuggestionsRequest>() {
        @Override
        public InlineSuggestionsRequest[] newArray(int size) {
            return new InlineSuggestionsRequest[size];
        }

        @Override
        public InlineSuggestionsRequest createFromParcel(@NonNull Parcel in) {
            return new InlineSuggestionsRequest(in);
        }
    };

    /**
     * A builder for {@link InlineSuggestionsRequest}
     */
    @SuppressWarnings("WeakerAccess")
    @DataClass.Generated.Member
    public static final class Builder extends BaseBuilder {

        private int mMaxSuggestionCount;
        private @NonNull List<InlinePresentationSpec> mInlinePresentationSpecs;
        private @NonNull String mHostPackageName;
        private @NonNull LocaleList mSupportedLocales;
        private @NonNull Bundle mExtras;
        private @Nullable IBinder mHostInputToken;
        private int mHostDisplayId;
        private @Nullable InlinePresentationSpec mInlineTooltipPresentationSpec;

        private long mBuilderFieldsSet = 0L;

        /**
         * Creates a new Builder.
         *
         * @param inlinePresentationSpecs
         *   The {@link InlinePresentationSpec} for each suggestion in the response. If the max suggestion
         *   count is larger than the number of specs in the list, then the last spec is used for the
         *   remainder of the suggestions. The list should not be empty.
         */
        public Builder(
                @NonNull List<InlinePresentationSpec> inlinePresentationSpecs) {
            mInlinePresentationSpecs = inlinePresentationSpecs;
            com.android.internal.util.AnnotationValidations.validate(
                    NonNull.class, null, mInlinePresentationSpecs);
        }

        /**
         * Max number of suggestions expected from the response. It must be a positive value.
         * Defaults to {@code SUGGESTION_COUNT_UNLIMITED} if not set.
         *
         * <p>In practice, it is recommended that the max suggestion count does not exceed <b>5</b>
         * for performance reasons.</p>
         */
        @DataClass.Generated.Member
        public @NonNull Builder setMaxSuggestionCount(int value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x1;
            mMaxSuggestionCount = value;
            return this;
        }

        /**
         * The {@link InlinePresentationSpec} for each suggestion in the response. If the max suggestion
         * count is larger than the number of specs in the list, then the last spec is used for the
         * remainder of the suggestions. The list should not be empty.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setInlinePresentationSpecs(@NonNull List<InlinePresentationSpec> value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x2;
            mInlinePresentationSpecs = value;
            return this;
        }

        /** @see #setInlinePresentationSpecs */
        @DataClass.Generated.Member
        public @NonNull Builder addInlinePresentationSpecs(@NonNull InlinePresentationSpec value) {
            // You can refine this method's name by providing item's singular name, e.g.:
            // @DataClass.PluralOf("item")) mItems = ...

            if (mInlinePresentationSpecs == null) setInlinePresentationSpecs(new ArrayList<>());
            mInlinePresentationSpecs.add(value);
            return this;
        }

        /**
         * The package name of the app that requests for the inline suggestions and will host the
         * embedded suggestion views. The app does not have to set the value for the field because
         * it'll be set by the system for safety reasons.
         */
        @DataClass.Generated.Member
        @Override
        @NonNull Builder setHostPackageName(@NonNull String value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x4;
            mHostPackageName = value;
            return this;
        }

        /**
         * The IME provided locales for the request. If non-empty, the inline suggestions should
         * return languages from the supported locales. If not provided, it'll default to be empty if
         * target SDK is S or above, and default to system locale otherwise.
         *
         * <p>Note for Autofill Providers: It is <b>recommended</b> for the returned inline suggestions
         * to have one locale to guarantee consistent UI rendering.</p>
         */
        @DataClass.Generated.Member
        public @NonNull Builder setSupportedLocales(@NonNull LocaleList value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x8;
            mSupportedLocales = value;
            return this;
        }

        /**
         * The extras state propagated from the IME to pass extra data.
         *
         * <p>Note: There should be no remote objects in the bundle, all included remote objects will
         * be removed from the bundle before transmission.</p>
         */
        @DataClass.Generated.Member
        public @NonNull Builder setExtras(@NonNull Bundle value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x10;
            mExtras = value;
            return this;
        }

        /**
         * The host input token of the IME that made the request. This will be set by the system for
         * safety reasons.
         *
         * @hide
         */
        @DataClass.Generated.Member
        @Override
        @NonNull Builder setHostInputToken(@NonNull IBinder value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x20;
            mHostInputToken = value;
            return this;
        }

        /**
         * The host display id of the IME that made the request. This will be set by the system for
         * safety reasons.
         *
         * @hide
         */
        @DataClass.Generated.Member
        @Override
        @NonNull Builder setHostDisplayId(int value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x40;
            mHostDisplayId = value;
            return this;
        }

        /**
         * Specifies the UI specification for the inline suggestion tooltip in the response.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setInlineTooltipPresentationSpec(@NonNull InlinePresentationSpec value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x80;
            mInlineTooltipPresentationSpec = value;
            return this;
        }

        /** Builds the instance. This builder should not be touched after calling this! */
        public @NonNull InlineSuggestionsRequest build() {
            checkNotUsed();
            mBuilderFieldsSet |= 0x100; // Mark builder used

            if ((mBuilderFieldsSet & 0x1) == 0) {
                mMaxSuggestionCount = defaultMaxSuggestionCount();
            }
            if ((mBuilderFieldsSet & 0x4) == 0) {
                mHostPackageName = defaultHostPackageName();
            }
            if ((mBuilderFieldsSet & 0x8) == 0) {
                mSupportedLocales = defaultSupportedLocales();
            }
            if ((mBuilderFieldsSet & 0x10) == 0) {
                mExtras = defaultExtras();
            }
            if ((mBuilderFieldsSet & 0x20) == 0) {
                mHostInputToken = defaultHostInputToken();
            }
            if ((mBuilderFieldsSet & 0x40) == 0) {
                mHostDisplayId = defaultHostDisplayId();
            }
            if ((mBuilderFieldsSet & 0x80) == 0) {
                mInlineTooltipPresentationSpec = defaultInlineTooltipPresentationSpec();
            }
            InlineSuggestionsRequest o = new InlineSuggestionsRequest(
                    mMaxSuggestionCount,
                    mInlinePresentationSpecs,
                    mHostPackageName,
                    mSupportedLocales,
                    mExtras,
                    mHostInputToken,
                    mHostDisplayId,
                    mInlineTooltipPresentationSpec);
            return o;
        }

        private void checkNotUsed() {
            if ((mBuilderFieldsSet & 0x100) != 0) {
                throw new IllegalStateException(
                        "This Builder should not be reused. Use a new Builder instance instead");
            }
        }
    }

    @DataClass.Generated(
            time = 1696889841006L,
            codegenVersion = "1.0.23",
            sourceFile = "frameworks/base/core/java/android/view/inputmethod/InlineSuggestionsRequest.java",
            inputSignatures = "public static final  int SUGGESTION_COUNT_UNLIMITED\nprivate final  int mMaxSuggestionCount\nprivate final @android.annotation.NonNull java.util.List<android.widget.inline.InlinePresentationSpec> mInlinePresentationSpecs\nprivate @android.annotation.NonNull java.lang.String mHostPackageName\nprivate @android.annotation.NonNull android.os.LocaleList mSupportedLocales\nprivate @android.annotation.NonNull android.os.Bundle mExtras\nprivate @android.annotation.Nullable android.os.IBinder mHostInputToken\nprivate  int mHostDisplayId\nprivate @android.annotation.Nullable android.widget.inline.InlinePresentationSpec mInlineTooltipPresentationSpec\nprivate static final @android.compat.annotation.ChangeId @android.compat.annotation.EnabledSince long IME_AUTOFILL_DEFAULT_SUPPORTED_LOCALES_IS_EMPTY\npublic  void setHostInputToken(android.os.IBinder)\nprivate  boolean extrasEquals(android.os.Bundle)\nprivate  void parcelHostInputToken(android.os.Parcel,int)\nprivate @android.annotation.Nullable android.os.IBinder unparcelHostInputToken(android.os.Parcel)\npublic  void setHostDisplayId(int)\nprivate  void onConstructed()\npublic  void filterContentTypes()\nprivate static  int defaultMaxSuggestionCount()\nprivate static  java.lang.String defaultHostPackageName()\nprivate static  android.widget.inline.InlinePresentationSpec defaultInlineTooltipPresentationSpec()\nprivate static  android.os.LocaleList defaultSupportedLocales()\nprivate static @android.annotation.Nullable android.os.IBinder defaultHostInputToken()\nprivate static @android.annotation.Nullable int defaultHostDisplayId()\nprivate static @android.annotation.NonNull android.os.Bundle defaultExtras()\nclass InlineSuggestionsRequest extends java.lang.Object implements [android.os.Parcelable]\nabstract  android.view.inputmethod.InlineSuggestionsRequest.Builder setInlinePresentationSpecs(java.util.List<android.widget.inline.InlinePresentationSpec>)\nabstract  android.view.inputmethod.InlineSuggestionsRequest.Builder setHostPackageName(java.lang.String)\nabstract  android.view.inputmethod.InlineSuggestionsRequest.Builder setHostInputToken(android.os.IBinder)\nabstract  android.view.inputmethod.InlineSuggestionsRequest.Builder setHostDisplayId(int)\nclass BaseBuilder extends java.lang.Object implements []\n@com.android.internal.util.DataClass(genEqualsHashCode=true, genToString=true, genBuilder=true)\nabstract  android.view.inputmethod.InlineSuggestionsRequest.Builder setInlinePresentationSpecs(java.util.List<android.widget.inline.InlinePresentationSpec>)\nabstract  android.view.inputmethod.InlineSuggestionsRequest.Builder setHostPackageName(java.lang.String)\nabstract  android.view.inputmethod.InlineSuggestionsRequest.Builder setHostInputToken(android.os.IBinder)\nabstract  android.view.inputmethod.InlineSuggestionsRequest.Builder setHostDisplayId(int)\nclass BaseBuilder extends java.lang.Object implements []")
    @Deprecated
    private void __metadata() {}


    //@formatter:on
    // End of generated code

}
