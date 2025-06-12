/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.adservices.common;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.adservices.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Represents the user's choice for the modules in AdServices. Can be unknown, opted-in, or
 * opted-out.
 *
 * @hide
 */
@FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
public final class AdServicesModuleUserChoice implements Parcelable {

    /** Default user choice state */
    public static final int USER_CHOICE_UNKNOWN = AdServicesCommonManager.USER_CHOICE_UNKNOWN;

    /** User opted in state */
    public static final int USER_CHOICE_OPTED_IN = AdServicesCommonManager.USER_CHOICE_OPTED_IN;

    /** User opted out state */
    public static final int USER_CHOICE_OPTED_OUT = AdServicesCommonManager.USER_CHOICE_OPTED_OUT;

    /**
     * Result codes that are common across various modules.
     *
     * @hide
     */
    @IntDef(
            prefix = {""},
            value = {USER_CHOICE_UNKNOWN, USER_CHOICE_OPTED_IN, USER_CHOICE_OPTED_OUT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ModuleUserChoiceCode {}

    @Module.ModuleCode private int mModule;

    @ModuleUserChoiceCode private int mUserChoice;

    private AdServicesModuleUserChoice(Parcel in) {
        Objects.requireNonNull(in, "Parcel is null");
        mModule = in.readInt();
        mUserChoice = in.readInt();
    }

    /**
     * Constructor for a user choice.
     *
     * @param module desired module
     * @param userChoice desired user choice
     */
    public AdServicesModuleUserChoice(
            @Module.ModuleCode int module, @ModuleUserChoiceCode int userChoice) {
        this.mModule = Module.validate(module);
        this.mUserChoice = validate(userChoice);
    }

    /**
     * Validates a user choice. For this function doesn't alter the input and just returns it back,
     * if not valid fails with {@link IllegalArgumentException}.
     *
     * @param userChoice user choice to validate
     * @return user choice
     */
    @ModuleUserChoiceCode
    private static int validate(@ModuleUserChoiceCode int userChoice) {
        return switch (userChoice) {
            case USER_CHOICE_UNKNOWN, USER_CHOICE_OPTED_IN, USER_CHOICE_OPTED_OUT -> userChoice;
            default -> throw new IllegalArgumentException("Invalid User Choice:" + userChoice);
        };
    }

    @NonNull
    public static final Creator<AdServicesModuleUserChoice> CREATOR =
            new Creator<>() {
                @Override
                public AdServicesModuleUserChoice createFromParcel(Parcel in) {
                    return new AdServicesModuleUserChoice(in);
                }

                @Override
                public AdServicesModuleUserChoice[] newArray(int size) {
                    return new AdServicesModuleUserChoice[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest, "Parcel is null");
        dest.writeInt(mModule);
        dest.writeInt(mUserChoice);
    }

    /** Gets the name of current module */
    public @Module.ModuleCode int getModule() {
        return mModule;
    }

    /** Gets the user opted in/out choice of current module */
    public @ModuleUserChoiceCode int getUserChoice() {
        return mUserChoice;
    }
}
