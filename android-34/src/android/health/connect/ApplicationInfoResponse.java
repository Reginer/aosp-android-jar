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

package android.health.connect;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.health.connect.datatypes.AppInfo;

import java.util.List;
import java.util.Objects;

/**
 * Response for {@link HealthConnectManager#getContributorApplicationsInfo}.
 *
 * @hide
 */
@SystemApi
public class ApplicationInfoResponse {
    private final List<AppInfo> mApplicationInfos;

    /** @hide */
    public ApplicationInfoResponse(@NonNull List<AppInfo> applicationInfos) {
        Objects.requireNonNull(applicationInfos);
        mApplicationInfos = applicationInfos;
    }

    /** Returns a list of {@link AppInfo} objects. */
    @NonNull
    public List<AppInfo> getApplicationInfoList() {
        return mApplicationInfos;
    }
}
