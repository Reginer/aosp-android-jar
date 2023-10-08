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

package android.adservices.adselection;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Generic Decision logic that could be provided by the buyer or seller.
 *
 * @hide
 */
public final class DecisionLogic implements Parcelable {

    @NonNull private String mDecisionLogic;

    public DecisionLogic(@NonNull String buyerDecisionLogic) {
        Objects.requireNonNull(buyerDecisionLogic);
        mDecisionLogic = buyerDecisionLogic;
    }

    private DecisionLogic(@NonNull Parcel in) {
        this(in.readString());
    }

    @NonNull
    public static final Creator<DecisionLogic> CREATOR =
            new Creator<DecisionLogic>() {
                @Override
                public DecisionLogic createFromParcel(Parcel in) {
                    Objects.requireNonNull(in);
                    return new DecisionLogic(in);
                }

                @Override
                public DecisionLogic[] newArray(int size) {
                    return new DecisionLogic[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);
        dest.writeString(mDecisionLogic);
    }

    @Override
    public String toString() {
        return mDecisionLogic;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mDecisionLogic);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DecisionLogic)) return false;
        DecisionLogic decisionLogic = (DecisionLogic) o;
        return mDecisionLogic.equals(decisionLogic.getLogic());
    }

    @NonNull
    public String getLogic() {
        return mDecisionLogic;
    }
}
