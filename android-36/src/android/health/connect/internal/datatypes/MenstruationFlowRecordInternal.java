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
package android.health.connect.internal.datatypes;

import android.annotation.NonNull;
import android.health.connect.datatypes.Identifier;
import android.health.connect.datatypes.MenstruationFlowRecord;
import android.health.connect.datatypes.MenstruationFlowRecord.MenstruationFlowType;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.os.Parcel;

/**
 * @see MenstruationFlowRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_FLOW)
public final class MenstruationFlowRecordInternal
        extends InstantRecordInternal<MenstruationFlowRecord> {
    private int mFlow;

    @MenstruationFlowType.MenstruationFlowTypes
    public int getFlow() {
        return mFlow;
    }

    /** returns this object with the specified flow */
    @NonNull
    public MenstruationFlowRecordInternal setFlow(int flow) {
        this.mFlow = flow;
        return this;
    }

    @NonNull
    @Override
    public MenstruationFlowRecord toExternalRecord() {
        return new MenstruationFlowRecord.Builder(buildMetaData(), getTime(), getFlow())
                .setZoneOffset(getZoneOffset())
                .buildWithoutValidation();
    }

    @Override
    void populateInstantRecordFrom(@NonNull Parcel parcel) {
        mFlow = parcel.readInt();
    }

    @Override
    void populateInstantRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mFlow);
    }
}
