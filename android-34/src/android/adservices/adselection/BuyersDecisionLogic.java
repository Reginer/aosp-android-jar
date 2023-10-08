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

import android.adservices.common.AdTechIdentifier;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.adservices.AdServicesParcelableUtil;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * @return The override for the decision logic for each buyer that is used by contextual ads for
 *     reporting, which may be extended to updating bid values for contextual ads in the future
 * @hide
 */
public final class BuyersDecisionLogic implements Parcelable {

    @NonNull
    public static final BuyersDecisionLogic EMPTY = new BuyersDecisionLogic(Collections.emptyMap());

    @NonNull private Map<AdTechIdentifier, DecisionLogic> mLogicMap;

    public BuyersDecisionLogic(@NonNull Map<AdTechIdentifier, DecisionLogic> logicMap) {
        Objects.requireNonNull(logicMap);
        mLogicMap = logicMap;
    }

    private BuyersDecisionLogic(@NonNull Parcel in) {
        mLogicMap =
                AdServicesParcelableUtil.readMapFromParcel(
                        in, AdTechIdentifier::fromString, DecisionLogic.class);
    }

    @NonNull
    public static final Creator<BuyersDecisionLogic> CREATOR =
            new Creator<BuyersDecisionLogic>() {
                @Override
                public BuyersDecisionLogic createFromParcel(Parcel in) {
                    Objects.requireNonNull(in);
                    return new BuyersDecisionLogic(in);
                }

                @Override
                public BuyersDecisionLogic[] newArray(int size) {
                    return new BuyersDecisionLogic[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);
        AdServicesParcelableUtil.writeMapToParcel(dest, mLogicMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mLogicMap);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BuyersDecisionLogic)) return false;
        BuyersDecisionLogic logicMap = (BuyersDecisionLogic) o;
        return mLogicMap.equals(logicMap.getLogicMap());
    }

    @NonNull
    public Map<AdTechIdentifier, DecisionLogic> getLogicMap() {
        return mLogicMap;
    }
}
