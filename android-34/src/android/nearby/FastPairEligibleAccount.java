/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.nearby;

import android.accounts.Account;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.nearby.aidl.FastPairEligibleAccountParcel;

/**
 * Class for FastPairEligibleAccount and its builder.
 *
 * @hide
 */
public class FastPairEligibleAccount {

    FastPairEligibleAccountParcel mAccountParcel;

    FastPairEligibleAccount(FastPairEligibleAccountParcel accountParcel) {
        this.mAccountParcel = accountParcel;
    }

    /**
     * Get Account.
     *
     * @hide
     */
    @Nullable
    public Account getAccount() {
        return this.mAccountParcel.account;
    }

    /**
     * Get OptIn Status.
     *
     * @hide
     */
    public boolean isOptIn() {
        return this.mAccountParcel.optIn;
    }

    /**
     * Builder used to create FastPairEligibleAccount.
     *
     * @hide
     */
    public static final class Builder {

        private final FastPairEligibleAccountParcel mBuilderParcel;

        /**
         * Default constructor of Builder.
         *
         * @hide
         */
        public Builder() {
            mBuilderParcel = new FastPairEligibleAccountParcel();
            mBuilderParcel.account = null;
            mBuilderParcel.optIn = false;
        }

        /**
         * Set Account.
         *
         * @param account Fast Pair eligible account.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setAccount(@Nullable Account account) {
            mBuilderParcel.account = account;
            return this;
        }

        /**
         * Set whether the account is opt into Fast Pair.
         *
         * @param optIn Whether the Fast Pair eligible account opts into Fast Pair.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setOptIn(boolean optIn) {
            mBuilderParcel.optIn = optIn;
            return this;
        }

        /**
         * Build {@link FastPairEligibleAccount} with the currently set configuration.
         *
         * @hide
         */
        @NonNull
        public FastPairEligibleAccount build() {
            return new FastPairEligibleAccount(mBuilderParcel);
        }
    }
}
