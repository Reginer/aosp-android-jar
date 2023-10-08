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
package android.adservices.topics;

import static android.adservices.common.AdServicesPermissions.ACCESS_ADSERVICES_TOPICS;
import static android.adservices.common.AdServicesStatusUtils.ILLEGAL_STATE_EXCEPTION_ERROR_MESSAGE;

import android.adservices.common.AdServicesStatusUtils;
import android.adservices.common.CallerMetadata;
import android.adservices.common.SandboxedSdkContextUtils;
import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.annotation.TestApi;
import android.app.sdksandbox.SandboxedSdkContext;
import android.content.Context;
import android.os.Build;
import android.os.LimitExceededException;
import android.os.OutcomeReceiver;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import com.android.adservices.AdServicesCommon;
import com.android.adservices.LoggerFactory;
import com.android.adservices.ServiceBinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * TopicsManager provides APIs for App and Ad-Sdks to get the user interest topics in a privacy
 * preserving way.
 *
 * <p>The instance of the {@link TopicsManager} can be obtained using {@link
 * Context#getSystemService} and {@link TopicsManager} class.
 */
// TODO(b/269798827): Enable for R.
@RequiresApi(Build.VERSION_CODES.S)
public final class TopicsManager {
    private static final LoggerFactory.Logger sLogger = LoggerFactory.getTopicsLogger();
    /**
     * Constant that represents the service name for {@link TopicsManager} to be used in {@link
     * android.adservices.AdServicesFrameworkInitializer#registerServiceWrappers}
     *
     * @hide
     */
    public static final String TOPICS_SERVICE = "topics_service";

    // When an app calls the Topics API directly, it sets the SDK name to empty string.
    static final String EMPTY_SDK = "";

    // Default value is true to record SDK's Observation when it calls Topics API.
    static final boolean RECORD_OBSERVATION_DEFAULT = true;

    private Context mContext;
    private ServiceBinder<ITopicsService> mServiceBinder;

    /**
     * Factory method for creating an instance of TopicsManager.
     *
     * @param context The {@link Context} to use
     * @return A {@link TopicsManager} instance
     */
    @NonNull
    public static TopicsManager get(@NonNull Context context) {
        // TODO(b/269798827): Enable for R.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            throw new IllegalStateException(ILLEGAL_STATE_EXCEPTION_ERROR_MESSAGE);
        }
        // On TM+, context.getSystemService() does more than just call constructor.
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? context.getSystemService(TopicsManager.class)
                : new TopicsManager(context);
    }

    /**
     * Create TopicsManager
     *
     * @hide
     */
    public TopicsManager(Context context) {
        // TODO(b/269798827): Enable for R.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            throw new IllegalStateException(ILLEGAL_STATE_EXCEPTION_ERROR_MESSAGE);
        }
        // In case the TopicsManager is initiated from inside a sdk_sandbox process the fields
        // will be immediately rewritten by the initialize method below.
        initialize(context);
    }

    /**
     * Initializes {@link TopicsManager} with the given {@code context}.
     *
     * <p>This method is called by the {@link SandboxedSdkContext} to propagate the correct context.
     * For more information check the javadoc on the {@link
     * android.app.sdksandbox.SdkSandboxSystemServiceRegistry}.
     *
     * @hide
     * @see android.app.sdksandbox.SdkSandboxSystemServiceRegistry
     */
    public TopicsManager initialize(Context context) {
        mContext = context;
        mServiceBinder =
                ServiceBinder.getServiceBinder(
                        context,
                        AdServicesCommon.ACTION_TOPICS_SERVICE,
                        ITopicsService.Stub::asInterface);
        return this;
    }

    @NonNull
    private ITopicsService getService() {
        ITopicsService service = mServiceBinder.getService();
        if (service == null) {
            throw new IllegalStateException(ILLEGAL_STATE_EXCEPTION_ERROR_MESSAGE);
        }
        return service;
    }

    /**
     * Return the topics.
     *
     * @param getTopicsRequest The request for obtaining Topics.
     * @param executor The executor to run callback.
     * @param callback The callback that's called after topics are available or an error occurs.
     * @throws SecurityException if caller is not authorized to call this API.
     * @throws IllegalStateException if this API is not available.
     * @throws LimitExceededException if rate limit was reached.
     */
    @NonNull
    @RequiresPermission(ACCESS_ADSERVICES_TOPICS)
    public void getTopics(
            @NonNull GetTopicsRequest getTopicsRequest,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<GetTopicsResponse, Exception> callback) {
        Objects.requireNonNull(getTopicsRequest);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        CallerMetadata callerMetadata =
                new CallerMetadata.Builder()
                        .setBinderElapsedTimestamp(SystemClock.elapsedRealtime())
                        .build();
        final ITopicsService service = getService();
        String sdkName = getTopicsRequest.getAdsSdkName();
        String appPackageName = "";
        String sdkPackageName = "";
        // First check if context is SandboxedSdkContext or not
        SandboxedSdkContext sandboxedSdkContext =
                SandboxedSdkContextUtils.getAsSandboxedSdkContext(mContext);
        if (sandboxedSdkContext != null) {
            // This is the case with the Sandbox.
            sdkPackageName = sandboxedSdkContext.getSdkPackageName();
            appPackageName = sandboxedSdkContext.getClientPackageName();

            if (!TextUtils.isEmpty(sdkName)) {
                throw new IllegalArgumentException(
                        "When calling Topics API from Sandbox, caller should not set Ads Sdk Name");
            }

            String sdkNameFromSandboxedContext = sandboxedSdkContext.getSdkName();
            if (null == sdkNameFromSandboxedContext || sdkNameFromSandboxedContext.isEmpty()) {
                throw new IllegalArgumentException(
                        "Sdk Name From SandboxedSdkContext should not be null or empty");
            }

            sdkName = sdkNameFromSandboxedContext;
        } else {
            // This is the case without the Sandbox.
            if (null == sdkName) {
                // When adsSdkName is not set, we assume the App calls the Topics API directly.
                // We set the adsSdkName to empty to mark this.
                sdkName = EMPTY_SDK;
            }
            appPackageName = mContext.getPackageName();
        }
        try {
            service.getTopics(
                    new GetTopicsParam.Builder()
                            .setAppPackageName(appPackageName)
                            .setSdkName(sdkName)
                            .setSdkPackageName(sdkPackageName)
                            .setShouldRecordObservation(getTopicsRequest.shouldRecordObservation())
                            .build(),
                    callerMetadata,
                    new IGetTopicsCallback.Stub() {
                        @Override
                        public void onResult(GetTopicsResult resultParcel) {
                            executor.execute(
                                    () -> {
                                        if (resultParcel.isSuccess()) {
                                            callback.onResult(
                                                    new GetTopicsResponse.Builder(
                                                                    getTopicList(resultParcel))
                                                            .build());
                                        } else {
                                            // TODO: Errors should be returned in onFailure method.
                                            callback.onError(
                                                    AdServicesStatusUtils.asException(
                                                            resultParcel));
                                        }
                                    });
                        }

                        @Override
                        public void onFailure(int resultCode) {
                            executor.execute(
                                    () ->
                                            callback.onError(
                                                    AdServicesStatusUtils.asException(resultCode)));
                        }
                    });
        } catch (RemoteException e) {
            sLogger.e(e, "RemoteException");
            callback.onError(e);
        }
    }

    private List<Topic> getTopicList(GetTopicsResult resultParcel) {
        List<Long> taxonomyVersionsList = resultParcel.getTaxonomyVersions();
        List<Long> modelVersionsList = resultParcel.getModelVersions();
        List<Integer> topicsCodeList = resultParcel.getTopics();
        List<Topic> topicList = new ArrayList<>();
        int size = taxonomyVersionsList.size();
        for (int i = 0; i < size; i++) {
            Topic topic =
                    new Topic(
                            taxonomyVersionsList.get(i),
                            modelVersionsList.get(i),
                            topicsCodeList.get(i));
            topicList.add(topic);
        }

        return topicList;
    }

    /**
     * If the service is in an APK (as opposed to the system service), unbind it from the service to
     * allow the APK process to die.
     *
     * @hide Not sure if we'll need this functionality in the final API. For now, we need it for
     *     performance testing to simulate "cold-start" situations.
     */
    // TODO: change to @VisibleForTesting
    @TestApi
    public void unbindFromService() {
        mServiceBinder.unbindFromService();
    }
}
