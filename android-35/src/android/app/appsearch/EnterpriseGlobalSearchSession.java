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

package android.app.appsearch;

import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.app.appsearch.aidl.AppSearchAttributionSource;
import android.app.appsearch.aidl.IAppSearchManager;
import android.app.appsearch.flags.Flags;
import android.os.UserHandle;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Provides a connection to the work profile's AppSearch databases that explicitly allow access from
 * enterprise sessions. Databases may have additional required permissions and restricted fields
 * when accessed through an enterprise session that they normally would not have.
 *
 * <p>EnterpriseGlobalSearchSession will only return results when created from the main user context
 * and when there is an associated work profile. If the given context is either not the main user or
 * does not have a work profile, queries will successfully complete with empty results, allowing
 * clients to query the work profile without having to account for whether it exists or not.
 */
@FlaggedApi(Flags.FLAG_ENABLE_ENTERPRISE_GLOBAL_SEARCH_SESSION)
public class EnterpriseGlobalSearchSession extends ReadOnlyGlobalSearchSession {

    /**
     * Creates an enterprise search session for the client, defined by the {@code userHandle} and
     * {@code packageName}.
     */
    static void createEnterpriseGlobalSearchSession(
            @NonNull IAppSearchManager service,
            @NonNull UserHandle userHandle,
            @NonNull AppSearchAttributionSource attributionSource,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<EnterpriseGlobalSearchSession>> callback) {
        EnterpriseGlobalSearchSession enterpriseGlobalSearchSession =
                new EnterpriseGlobalSearchSession(service, userHandle, attributionSource);
        enterpriseGlobalSearchSession.initialize(
                executor,
                result -> {
                    if (result.isSuccess()) {
                        callback.accept(
                                AppSearchResult.newSuccessfulResult(enterpriseGlobalSearchSession));
                    } else {
                        callback.accept(AppSearchResult.newFailedResult(result));
                    }
                });
    }

    private EnterpriseGlobalSearchSession(
            @NonNull IAppSearchManager service,
            @NonNull UserHandle userHandle,
            @NonNull AppSearchAttributionSource callerAttributionSource) {
        super(service, userHandle, callerAttributionSource, /* isForEnterprise= */ true);
    }
}
