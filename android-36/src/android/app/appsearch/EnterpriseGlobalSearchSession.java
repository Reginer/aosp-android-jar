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
import android.os.UserHandle;

import com.android.appsearch.flags.Flags;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Provides a connection to all enterprise (work profile) AppSearch databases the querying
 * application has been granted access to.
 *
 * <p>This session can be created from any user profile but will only properly return results when
 * created from the main profile. If the user is not the main profile or an associated work profile
 * does not exist, queries will still successfully complete but with empty results.
 *
 * <p>Schemas must be explicitly tagged enterprise and may require additional permissions to be
 * visible from an enterprise session. Retrieved documents may also have certain fields restricted
 * or modified unlike if they were retrieved directly from {@link GlobalSearchSession} on the work
 * profile.
 *
 * <p>This class is thread safe.
 *
 * @see GlobalSearchSession
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
