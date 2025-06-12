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

package android.app.ambientcontext;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcelable;
import android.os.PersistableBundle;

import com.android.internal.util.DataClass;
import com.android.internal.util.Parcelling;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;


/**
 * Represents a detected ambient event. Each event has a type, start time, end time,
 * plus some optional data.
 *
 * @hide
 */
@SystemApi
@DataClass(
        genBuilder = true,
        genConstructor = false,
        genHiddenConstDefs = true,
        genParcelable = true,
        genToString = true
)
public final class AmbientContextEvent implements Parcelable {
    /**
     * The integer indicating an unknown event was detected.
     */
    public static final int EVENT_UNKNOWN = 0;

    /**
     * The integer indicating a cough event was detected.
     */
    public static final int EVENT_COUGH = 1;

    /**
     * The integer indicating a snore event was detected.
     */
    public static final int EVENT_SNORE = 2;

    /**
     * The integer indicating a double-tap event was detected.
     * For detecting this event type, there's no specific consent activity to request access, but
     * the consent is implied through the double tap toggle in the Settings app.
     */
    public static final int EVENT_BACK_DOUBLE_TAP = 3;

    /**
     * Integer indicating the start of wearable vendor defined events that can be detected.
     * These depend on the vendor implementation.
     */
    public static final int EVENT_VENDOR_WEARABLE_START = 100000;

    /**
     * Name for the mVendorData object for this AmbientContextEvent. The mVendorData must be present
     * in the object, or it will be rejected.
     */
    public static final String KEY_VENDOR_WEARABLE_EVENT_NAME = "wearable_event_name";

    /** @hide */
    @IntDef(prefix = { "EVENT_" }, value = {
            EVENT_UNKNOWN,
            EVENT_COUGH,
            EVENT_SNORE,
            EVENT_BACK_DOUBLE_TAP,
            EVENT_VENDOR_WEARABLE_START,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface EventCode {}

    /** The integer indicating an unknown level. */
    public static final int LEVEL_UNKNOWN = 0;

    /** The integer indicating a low level. */
    public static final int LEVEL_LOW = 1;

    /** The integer indicating a medium low level. */
    public static final int LEVEL_MEDIUM_LOW = 2;

    /** The integer indicating a medium Level. */
    public static final int LEVEL_MEDIUM = 3;

    /** The integer indicating a medium high level. */
    public static final int LEVEL_MEDIUM_HIGH = 4;

    /** The integer indicating a high level. */
    public static final int LEVEL_HIGH = 5;

    /** @hide */
    @IntDef(prefix = {"LEVEL_"}, value = {
            LEVEL_UNKNOWN,
            LEVEL_LOW,
            LEVEL_MEDIUM_LOW,
            LEVEL_MEDIUM,
            LEVEL_MEDIUM_HIGH,
            LEVEL_HIGH
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface LevelValue {}

    @EventCode private final int mEventType;
    private static int defaultEventType() {
        return EVENT_UNKNOWN;
    }

    /** Event start time */
    @DataClass.ParcelWith(Parcelling.BuiltIn.ForInstant.class)
    @NonNull private final Instant mStartTime;
    @NonNull private static Instant defaultStartTime() {
        return Instant.MIN;
    }

    /** Event end time */
    @DataClass.ParcelWith(Parcelling.BuiltIn.ForInstant.class)
    @NonNull private final Instant mEndTime;
    @NonNull private static Instant defaultEndTime() {
        return Instant.MAX;
    }

    /**
     * Confidence level from LEVEL_LOW to LEVEL_HIGH, or LEVEL_NONE if not available.
     * Apps can add post-processing filter using this value if needed.
     */
    @LevelValue private final int mConfidenceLevel;
    private static int defaultConfidenceLevel() {
        return LEVEL_UNKNOWN;
    }

    /**
     * Density level from LEVEL_LOW to LEVEL_HIGH, or LEVEL_NONE if not available.
     * Apps can add post-processing filter using this value if needed.
     */
    @LevelValue private final int mDensityLevel;
    private static int defaultDensityLevel() {
        return LEVEL_UNKNOWN;
    }

    /**
     * Vendor defined specific values for vendor event types.
     *
     * <p> The use of this vendor data is discouraged. For data defined in the range above
     * {@code EVENT_VENDOR_WEARABLE_START} this bundle must include the
     * {@link KEY_VENDOR_WEARABLE_EVENT_NAME} field or it will be rejected. In addition, to increase
     * transparency of this data contents of this bundle will be logged to logcat.</p>
     */
    private final @NonNull PersistableBundle mVendorData;
    private static PersistableBundle defaultVendorData() {
        return new PersistableBundle();
    }



    // Code below generated by codegen v1.0.23.
    //
    // DO NOT MODIFY!
    // CHECKSTYLE:OFF Generated code
    //
    // To regenerate run:
    // $ codegen $ANDROID_BUILD_TOP/frameworks/base/core/java/android/app/ambientcontext/AmbientContextEvent.java
    //
    // To exclude the generated code from IntelliJ auto-formatting enable (one-time):
    //   Settings > Editor > Code Style > Formatter Control
    //@formatter:off


    /** @hide */
    @IntDef(prefix = "EVENT_", value = {
        EVENT_UNKNOWN,
        EVENT_COUGH,
        EVENT_SNORE,
        EVENT_BACK_DOUBLE_TAP,
        EVENT_VENDOR_WEARABLE_START
    })
    @Retention(RetentionPolicy.SOURCE)
    @DataClass.Generated.Member
    public @interface Event {}

    /** @hide */
    @DataClass.Generated.Member
    public static String eventToString(@Event int value) {
        switch (value) {
            case EVENT_UNKNOWN:
                    return "EVENT_UNKNOWN";
            case EVENT_COUGH:
                    return "EVENT_COUGH";
            case EVENT_SNORE:
                    return "EVENT_SNORE";
            case EVENT_BACK_DOUBLE_TAP:
                    return "EVENT_BACK_DOUBLE_TAP";
            case EVENT_VENDOR_WEARABLE_START:
                    return "EVENT_VENDOR_WEARABLE_START";
            default: return Integer.toHexString(value);
        }
    }

    /** @hide */
    @IntDef(prefix = "LEVEL_", value = {
        LEVEL_UNKNOWN,
        LEVEL_LOW,
        LEVEL_MEDIUM_LOW,
        LEVEL_MEDIUM,
        LEVEL_MEDIUM_HIGH,
        LEVEL_HIGH
    })
    @Retention(RetentionPolicy.SOURCE)
    @DataClass.Generated.Member
    public @interface Level {}

    /** @hide */
    @DataClass.Generated.Member
    public static String levelToString(@Level int value) {
        switch (value) {
            case LEVEL_UNKNOWN:
                    return "LEVEL_UNKNOWN";
            case LEVEL_LOW:
                    return "LEVEL_LOW";
            case LEVEL_MEDIUM_LOW:
                    return "LEVEL_MEDIUM_LOW";
            case LEVEL_MEDIUM:
                    return "LEVEL_MEDIUM";
            case LEVEL_MEDIUM_HIGH:
                    return "LEVEL_MEDIUM_HIGH";
            case LEVEL_HIGH:
                    return "LEVEL_HIGH";
            default: return Integer.toHexString(value);
        }
    }

    @DataClass.Generated.Member
    /* package-private */ AmbientContextEvent(
            @EventCode int eventType,
            @NonNull Instant startTime,
            @NonNull Instant endTime,
            @LevelValue int confidenceLevel,
            @LevelValue int densityLevel,
            @NonNull PersistableBundle vendorData) {
        this.mEventType = eventType;
        com.android.internal.util.AnnotationValidations.validate(
                EventCode.class, null, mEventType);
        this.mStartTime = startTime;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mStartTime);
        this.mEndTime = endTime;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mEndTime);
        this.mConfidenceLevel = confidenceLevel;
        com.android.internal.util.AnnotationValidations.validate(
                LevelValue.class, null, mConfidenceLevel);
        this.mDensityLevel = densityLevel;
        com.android.internal.util.AnnotationValidations.validate(
                LevelValue.class, null, mDensityLevel);
        this.mVendorData = vendorData;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mVendorData);

        // onConstructed(); // You can define this method to get a callback
    }

    @DataClass.Generated.Member
    public @EventCode int getEventType() {
        return mEventType;
    }

    /**
     * Event start time
     */
    @DataClass.Generated.Member
    public @NonNull Instant getStartTime() {
        return mStartTime;
    }

    /**
     * Event end time
     */
    @DataClass.Generated.Member
    public @NonNull Instant getEndTime() {
        return mEndTime;
    }

    /**
     * Confidence level from LEVEL_LOW to LEVEL_HIGH, or LEVEL_NONE if not available.
     * Apps can add post-processing filter using this value if needed.
     */
    @DataClass.Generated.Member
    public @LevelValue int getConfidenceLevel() {
        return mConfidenceLevel;
    }

    /**
     * Density level from LEVEL_LOW to LEVEL_HIGH, or LEVEL_NONE if not available.
     * Apps can add post-processing filter using this value if needed.
     */
    @DataClass.Generated.Member
    public @LevelValue int getDensityLevel() {
        return mDensityLevel;
    }

    /**
     * Vendor defined specific values for vendor event types.
     *
     * <p> The use of this vendor data is discouraged. For data defined in the range above
     * {@code EVENT_VENDOR_WEARABLE_START} this bundle must include the
     * {@link KEY_VENDOR_WEARABLE_EVENT_NAME} field or it will be rejected. In addition, to increase
     * transparency of this data contents of this bundle will be logged to logcat.</p>
     */
    @DataClass.Generated.Member
    public @NonNull PersistableBundle getVendorData() {
        return mVendorData;
    }

    @Override
    @DataClass.Generated.Member
    public String toString() {
        // You can override field toString logic by defining methods like:
        // String fieldNameToString() { ... }

        return "AmbientContextEvent { " +
                "eventType = " + mEventType + ", " +
                "startTime = " + mStartTime + ", " +
                "endTime = " + mEndTime + ", " +
                "confidenceLevel = " + mConfidenceLevel + ", " +
                "densityLevel = " + mDensityLevel + ", " +
                "vendorData = " + mVendorData +
        " }";
    }

    @DataClass.Generated.Member
    static Parcelling<Instant> sParcellingForStartTime =
            Parcelling.Cache.get(
                    Parcelling.BuiltIn.ForInstant.class);
    static {
        if (sParcellingForStartTime == null) {
            sParcellingForStartTime = Parcelling.Cache.put(
                    new Parcelling.BuiltIn.ForInstant());
        }
    }

    @DataClass.Generated.Member
    static Parcelling<Instant> sParcellingForEndTime =
            Parcelling.Cache.get(
                    Parcelling.BuiltIn.ForInstant.class);
    static {
        if (sParcellingForEndTime == null) {
            sParcellingForEndTime = Parcelling.Cache.put(
                    new Parcelling.BuiltIn.ForInstant());
        }
    }

    @Override
    @DataClass.Generated.Member
    public void writeToParcel(@NonNull android.os.Parcel dest, int flags) {
        // You can override field parcelling by defining methods like:
        // void parcelFieldName(Parcel dest, int flags) { ... }

        dest.writeInt(mEventType);
        sParcellingForStartTime.parcel(mStartTime, dest, flags);
        sParcellingForEndTime.parcel(mEndTime, dest, flags);
        dest.writeInt(mConfidenceLevel);
        dest.writeInt(mDensityLevel);
        dest.writeTypedObject(mVendorData, flags);
    }

    @Override
    @DataClass.Generated.Member
    public int describeContents() { return 0; }

    /** @hide */
    @SuppressWarnings({"unchecked", "RedundantCast"})
    @DataClass.Generated.Member
    /* package-private */ AmbientContextEvent(@NonNull android.os.Parcel in) {
        // You can override field unparcelling by defining methods like:
        // static FieldType unparcelFieldName(Parcel in) { ... }

        int eventType = in.readInt();
        Instant startTime = sParcellingForStartTime.unparcel(in);
        Instant endTime = sParcellingForEndTime.unparcel(in);
        int confidenceLevel = in.readInt();
        int densityLevel = in.readInt();
        PersistableBundle vendorData = (PersistableBundle) in.readTypedObject(PersistableBundle.CREATOR);

        this.mEventType = eventType;
        com.android.internal.util.AnnotationValidations.validate(
                EventCode.class, null, mEventType);
        this.mStartTime = startTime;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mStartTime);
        this.mEndTime = endTime;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mEndTime);
        this.mConfidenceLevel = confidenceLevel;
        com.android.internal.util.AnnotationValidations.validate(
                LevelValue.class, null, mConfidenceLevel);
        this.mDensityLevel = densityLevel;
        com.android.internal.util.AnnotationValidations.validate(
                LevelValue.class, null, mDensityLevel);
        this.mVendorData = vendorData;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mVendorData);

        // onConstructed(); // You can define this method to get a callback
    }

    @DataClass.Generated.Member
    public static final @NonNull Parcelable.Creator<AmbientContextEvent> CREATOR
            = new Parcelable.Creator<AmbientContextEvent>() {
        @Override
        public AmbientContextEvent[] newArray(int size) {
            return new AmbientContextEvent[size];
        }

        @Override
        public AmbientContextEvent createFromParcel(@NonNull android.os.Parcel in) {
            return new AmbientContextEvent(in);
        }
    };

    /**
     * A builder for {@link AmbientContextEvent}
     */
    @SuppressWarnings("WeakerAccess")
    @DataClass.Generated.Member
    public static final class Builder {

        private @EventCode int mEventType;
        private @NonNull Instant mStartTime;
        private @NonNull Instant mEndTime;
        private @LevelValue int mConfidenceLevel;
        private @LevelValue int mDensityLevel;
        private @NonNull PersistableBundle mVendorData;

        private long mBuilderFieldsSet = 0L;

        public Builder() {
        }

        @DataClass.Generated.Member
        public @NonNull Builder setEventType(@EventCode int value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x1;
            mEventType = value;
            return this;
        }

        /**
         * Event start time
         */
        @DataClass.Generated.Member
        public @NonNull Builder setStartTime(@NonNull Instant value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x2;
            mStartTime = value;
            return this;
        }

        /**
         * Event end time
         */
        @DataClass.Generated.Member
        public @NonNull Builder setEndTime(@NonNull Instant value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x4;
            mEndTime = value;
            return this;
        }

        /**
         * Confidence level from LEVEL_LOW to LEVEL_HIGH, or LEVEL_NONE if not available.
         * Apps can add post-processing filter using this value if needed.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setConfidenceLevel(@LevelValue int value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x8;
            mConfidenceLevel = value;
            return this;
        }

        /**
         * Density level from LEVEL_LOW to LEVEL_HIGH, or LEVEL_NONE if not available.
         * Apps can add post-processing filter using this value if needed.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setDensityLevel(@LevelValue int value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x10;
            mDensityLevel = value;
            return this;
        }

        /**
         * Vendor defined specific values for vendor event types.
         *
         * <p> The use of this vendor data is discouraged. For data defined in the range above
         * {@code EVENT_VENDOR_WEARABLE_START} this bundle must include the
         * {@link KEY_VENDOR_WEARABLE_EVENT_NAME} field or it will be rejected. In addition, to increase
         * transparency of this data contents of this bundle will be logged to logcat.</p>
         */
        @DataClass.Generated.Member
        public @NonNull Builder setVendorData(@NonNull PersistableBundle value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x20;
            mVendorData = value;
            return this;
        }

        /** Builds the instance. This builder should not be touched after calling this! */
        public @NonNull AmbientContextEvent build() {
            checkNotUsed();
            mBuilderFieldsSet |= 0x40; // Mark builder used

            if ((mBuilderFieldsSet & 0x1) == 0) {
                mEventType = defaultEventType();
            }
            if ((mBuilderFieldsSet & 0x2) == 0) {
                mStartTime = defaultStartTime();
            }
            if ((mBuilderFieldsSet & 0x4) == 0) {
                mEndTime = defaultEndTime();
            }
            if ((mBuilderFieldsSet & 0x8) == 0) {
                mConfidenceLevel = defaultConfidenceLevel();
            }
            if ((mBuilderFieldsSet & 0x10) == 0) {
                mDensityLevel = defaultDensityLevel();
            }
            if ((mBuilderFieldsSet & 0x20) == 0) {
                mVendorData = defaultVendorData();
            }
            AmbientContextEvent o = new AmbientContextEvent(
                    mEventType,
                    mStartTime,
                    mEndTime,
                    mConfidenceLevel,
                    mDensityLevel,
                    mVendorData);
            return o;
        }

        private void checkNotUsed() {
            if ((mBuilderFieldsSet & 0x40) != 0) {
                throw new IllegalStateException(
                        "This Builder should not be reused. Use a new Builder instance instead");
            }
        }
    }

    @DataClass.Generated(
            time = 1709014715064L,
            codegenVersion = "1.0.23",
            sourceFile = "frameworks/base/core/java/android/app/ambientcontext/AmbientContextEvent.java",
            inputSignatures = "public static final  int EVENT_UNKNOWN\npublic static final  int EVENT_COUGH\npublic static final  int EVENT_SNORE\npublic static final  int EVENT_BACK_DOUBLE_TAP\npublic static final  int EVENT_VENDOR_WEARABLE_START\npublic static final  java.lang.String KEY_VENDOR_WEARABLE_EVENT_NAME\npublic static final  int LEVEL_UNKNOWN\npublic static final  int LEVEL_LOW\npublic static final  int LEVEL_MEDIUM_LOW\npublic static final  int LEVEL_MEDIUM\npublic static final  int LEVEL_MEDIUM_HIGH\npublic static final  int LEVEL_HIGH\nprivate final @android.app.ambientcontext.AmbientContextEvent.EventCode int mEventType\nprivate final @com.android.internal.util.DataClass.ParcelWith(com.android.internal.util.Parcelling.BuiltIn.ForInstant.class) @android.annotation.NonNull java.time.Instant mStartTime\nprivate final @com.android.internal.util.DataClass.ParcelWith(com.android.internal.util.Parcelling.BuiltIn.ForInstant.class) @android.annotation.NonNull java.time.Instant mEndTime\nprivate final @android.app.ambientcontext.AmbientContextEvent.LevelValue int mConfidenceLevel\nprivate final @android.app.ambientcontext.AmbientContextEvent.LevelValue int mDensityLevel\nprivate final @android.annotation.NonNull android.os.PersistableBundle mVendorData\nprivate static  int defaultEventType()\nprivate static @android.annotation.NonNull java.time.Instant defaultStartTime()\nprivate static @android.annotation.NonNull java.time.Instant defaultEndTime()\nprivate static  int defaultConfidenceLevel()\nprivate static  int defaultDensityLevel()\nprivate static  android.os.PersistableBundle defaultVendorData()\nclass AmbientContextEvent extends java.lang.Object implements [android.os.Parcelable]\n@com.android.internal.util.DataClass(genBuilder=true, genConstructor=false, genHiddenConstDefs=true, genParcelable=true, genToString=true)")
    @Deprecated
    private void __metadata() {}


    //@formatter:on
    // End of generated code

}
