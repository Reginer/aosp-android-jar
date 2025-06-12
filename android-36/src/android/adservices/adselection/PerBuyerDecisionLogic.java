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
import android.adservices.customaudience.CustomAudience;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.adservices.AdServicesParcelableUtil;
import com.android.adservices.flags.Flags;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * The override object for decision logic JS per buyer for {@link SignedContextualAds}.
 *
 * <p>This decision logic is used for reporting when an ad wins from a buyer's bundle of {@link
 * SignedContextualAds}.
 *
 * <p>This JS code may be extended to updating bid values for contextual ads in the future.
 *
 * <p>See {@link CustomAudience#getBiddingLogicUri()}.
 */
@FlaggedApi(Flags.FLAG_FLEDGE_AD_SELECTION_FILTERING_ENABLED)
public final class PerBuyerDecisionLogic implements Parcelable {

    @NonNull
    public static final PerBuyerDecisionLogic EMPTY =
            new PerBuyerDecisionLogic(Collections.emptyMap());

    @NonNull private final Map<AdTechIdentifier, DecisionLogic> mPerBuyerLogicMap;

    /**
     * Builds a {@link PerBuyerDecisionLogic} instance.
     *
     * @param perBuyerLogicMap map of buyers and their decision logic to be fetched during ad
     *     selection
     */
    public PerBuyerDecisionLogic(@NonNull Map<AdTechIdentifier, DecisionLogic> perBuyerLogicMap) {
        Objects.requireNonNull(perBuyerLogicMap);
        mPerBuyerLogicMap = perBuyerLogicMap;
    }

    private PerBuyerDecisionLogic(@NonNull Parcel in) {
        mPerBuyerLogicMap =
                AdServicesParcelableUtil.readMapFromParcel(
                        in, AdTechIdentifier::fromString, DecisionLogic.class);
    }

    @NonNull
    public static final Creator<PerBuyerDecisionLogic> CREATOR =
            new Creator<PerBuyerDecisionLogic>() {
                @Override
                public PerBuyerDecisionLogic createFromParcel(Parcel in) {
                    Objects.requireNonNull(in);
                    return new PerBuyerDecisionLogic(in);
                }

                @Override
                public PerBuyerDecisionLogic[] newArray(int size) {
                    return new PerBuyerDecisionLogic[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);
        AdServicesParcelableUtil.writeMapToParcel(dest, mPerBuyerLogicMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPerBuyerLogicMap);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PerBuyerDecisionLogic)) return false;
        PerBuyerDecisionLogic logicMap = (PerBuyerDecisionLogic) o;
        return mPerBuyerLogicMap.equals(logicMap.getPerBuyerLogicMap());
    }

    @NonNull
    public Map<AdTechIdentifier, DecisionLogic> getPerBuyerLogicMap() {
        return mPerBuyerLogicMap;
    }
}
