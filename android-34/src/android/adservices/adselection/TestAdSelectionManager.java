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

package android.adservices.adselection;

import static android.adservices.common.AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE;

import android.adservices.common.AdServicesStatusUtils;
import android.adservices.common.FledgeErrorResponse;
import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.os.Build;
import android.os.OutcomeReceiver;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

import com.android.adservices.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * {@link TestAdSelectionManager} provides APIs for apps and ad SDKs to test ad selection processes.
 *
 * <p>These APIs are intended to be used for end-to-end testing. They are enabled only for
 * debuggable apps on phones running a debuggable OS build with developer options enabled.
 */
// TODO(b/269798827): Enable for R.
@RequiresApi(Build.VERSION_CODES.S)
public class TestAdSelectionManager {
    private static final LoggerFactory.Logger sLogger = LoggerFactory.getFledgeLogger();

    private final AdSelectionManager mAdSelectionManager;

    TestAdSelectionManager(@NonNull AdSelectionManager adSelectionManager) {
        Objects.requireNonNull(adSelectionManager);

        mAdSelectionManager = adSelectionManager;
    }

    /**
     * Overrides the AdSelection API for a given {@link AdSelectionConfig} to avoid fetching data
     * from remote servers and use the data provided in {@link AddAdSelectionOverrideRequest}
     * instead. The {@link AddAdSelectionOverrideRequest} is provided by the Ads SDK.
     *
     * <p>This method is intended to be used for end-to-end testing. This API is enabled only for
     * apps in debug mode with developer options enabled.
     *
     * @throws IllegalStateException if this API is not enabled for the caller
     *     <p>The receiver either returns a {@code void} for a successful run, or an {@link
     *     Exception} indicates the error.
     */
    @RequiresPermission(ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public void overrideAdSelectionConfigRemoteInfo(
            @NonNull AddAdSelectionOverrideRequest request,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> receiver) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        try {
            final AdSelectionService service = mAdSelectionManager.getService();
            service.overrideAdSelectionConfigRemoteInfo(
                    request.getAdSelectionConfig(),
                    request.getDecisionLogicJs(),
                    request.getTrustedScoringSignals(),
                    request.getBuyersDecisionLogic(),
                    new AdSelectionOverrideCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            executor.execute(() -> receiver.onResult(new Object()));
                        }

                        @Override
                        public void onFailure(FledgeErrorResponse failureParcel) {
                            executor.execute(
                                    () ->
                                            receiver.onError(
                                                    AdServicesStatusUtils.asException(
                                                            failureParcel)));
                        }
                    });
        } catch (NullPointerException e) {
            sLogger.e(e, "Unable to find the AdSelection service.");
            receiver.onError(
                    new IllegalStateException("Unable to find the AdSelection service.", e));
        } catch (RemoteException e) {
            sLogger.e(e, "Exception");
            receiver.onError(new IllegalStateException("Failure of AdSelection service.", e));
        }
    }

    /**
     * Removes an override for {@link AdSelectionConfig} in the Ad Selection API with associated the
     * data in {@link RemoveAdSelectionOverrideRequest}. The {@link
     * RemoveAdSelectionOverrideRequest} is provided by the Ads SDK.
     *
     * <p>This method is intended to be used for end-to-end testing. This API is enabled only for
     * apps in debug mode with developer options enabled.
     *
     * @throws IllegalStateException if this API is not enabled for the caller
     *     <p>The receiver either returns a {@code void} for a successful run, or an {@link
     *     Exception} indicates the error.
     */
    @RequiresPermission(ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public void removeAdSelectionConfigRemoteInfoOverride(
            @NonNull RemoveAdSelectionOverrideRequest request,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> receiver) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        try {
            final AdSelectionService service = mAdSelectionManager.getService();
            service.removeAdSelectionConfigRemoteInfoOverride(
                    request.getAdSelectionConfig(),
                    new AdSelectionOverrideCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            executor.execute(() -> receiver.onResult(new Object()));
                        }

                        @Override
                        public void onFailure(FledgeErrorResponse failureParcel) {
                            executor.execute(
                                    () ->
                                            receiver.onError(
                                                    AdServicesStatusUtils.asException(
                                                            failureParcel)));
                        }
                    });
        } catch (NullPointerException e) {
            sLogger.e(e, "Unable to find the AdSelection service.");
            receiver.onError(
                    new IllegalStateException("Unable to find the AdSelection service.", e));
        } catch (RemoteException e) {
            sLogger.e(e, "Exception");
            receiver.onError(new IllegalStateException("Failure of AdSelection service.", e));
        }
    }

    /**
     * Removes all override data for {@link AdSelectionConfig} in the Ad Selection API.
     *
     * <p>This method is intended to be used for end-to-end testing. This API is enabled only for
     * apps in debug mode with developer options enabled.
     *
     * @throws IllegalStateException if this API is not enabled for the caller
     *     <p>The receiver either returns a {@code void} for a successful run, or an {@link
     *     Exception} indicates the error.
     */
    @RequiresPermission(ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public void resetAllAdSelectionConfigRemoteOverrides(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> receiver) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        try {
            final AdSelectionService service = mAdSelectionManager.getService();
            service.resetAllAdSelectionConfigRemoteOverrides(
                    new AdSelectionOverrideCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            executor.execute(() -> receiver.onResult(new Object()));
                        }

                        @Override
                        public void onFailure(FledgeErrorResponse failureParcel) {
                            executor.execute(
                                    () ->
                                            receiver.onError(
                                                    AdServicesStatusUtils.asException(
                                                            failureParcel)));
                        }
                    });
        } catch (NullPointerException e) {
            sLogger.e(e, "Unable to find the AdSelection service.");
            receiver.onError(
                    new IllegalStateException("Unable to find the AdSelection service.", e));
        } catch (RemoteException e) {
            sLogger.e(e, "Exception");
            receiver.onError(new IllegalStateException("Failure of AdSelection service.", e));
        }
    }

    /**
     * Overrides the AdSelection API for {@link AdSelectionFromOutcomesConfig} to avoid fetching
     * data from remote servers and use the data provided in {@link
     * AddAdSelectionFromOutcomesOverrideRequest} instead. The {@link
     * AddAdSelectionFromOutcomesOverrideRequest} is provided by the Ads SDK.
     *
     * <p>This method is intended to be used for end-to-end testing. This API is enabled only for
     * apps in debug mode with developer options enabled.
     *
     * @throws IllegalStateException if this API is not enabled for the caller
     *     <p>The receiver either returns a {@code void} for a successful run, or an {@link
     *     Exception} indicates the error.
     * @hide
     */
    @RequiresPermission(ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public void overrideAdSelectionFromOutcomesConfigRemoteInfo(
            @NonNull AddAdSelectionFromOutcomesOverrideRequest request,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> receiver) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        try {
            final AdSelectionService service = mAdSelectionManager.getService();
            service.overrideAdSelectionFromOutcomesConfigRemoteInfo(
                    request.getAdSelectionFromOutcomesConfig(),
                    request.getOutcomeSelectionLogicJs(),
                    request.getOutcomeSelectionTrustedSignals(),
                    new AdSelectionOverrideCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            executor.execute(() -> receiver.onResult(new Object()));
                        }

                        @Override
                        public void onFailure(FledgeErrorResponse failureParcel) {
                            executor.execute(
                                    () ->
                                            receiver.onError(
                                                    AdServicesStatusUtils.asException(
                                                            failureParcel)));
                        }
                    });
        } catch (NullPointerException e) {
            sLogger.e(e, "Unable to find the AdSelection service.");
            receiver.onError(
                    new IllegalStateException("Unable to find the AdSelection service.", e));
        } catch (RemoteException e) {
            sLogger.e(e, "Exception");
            receiver.onError(new IllegalStateException("Failure of AdSelection service.", e));
        }
    }

    /**
     * Removes an override for {@link AdSelectionFromOutcomesConfig} in th Ad Selection API with
     * associated the data in {@link RemoveAdSelectionOverrideRequest}. The {@link
     * RemoveAdSelectionOverrideRequest} is provided by the Ads SDK.
     *
     * <p>This method is intended to be used for end-to-end testing. This API is enabled only for
     * apps in debug mode with developer options enabled.
     *
     * @throws IllegalStateException if this API is not enabled for the caller
     *     <p>The receiver either returns a {@code void} for a successful run, or an {@link
     *     Exception} indicates the error.
     * @hide
     */
    @RequiresPermission(ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public void removeAdSelectionFromOutcomesConfigRemoteInfoOverride(
            @NonNull RemoveAdSelectionFromOutcomesOverrideRequest request,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> receiver) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        try {
            final AdSelectionService service = mAdSelectionManager.getService();
            service.removeAdSelectionFromOutcomesConfigRemoteInfoOverride(
                    request.getAdSelectionFromOutcomesConfig(),
                    new AdSelectionOverrideCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            executor.execute(() -> receiver.onResult(new Object()));
                        }

                        @Override
                        public void onFailure(FledgeErrorResponse failureParcel) {
                            executor.execute(
                                    () ->
                                            receiver.onError(
                                                    AdServicesStatusUtils.asException(
                                                            failureParcel)));
                        }
                    });
        } catch (NullPointerException e) {
            sLogger.e(e, "Unable to find the AdSelection service.");
            receiver.onError(
                    new IllegalStateException("Unable to find the AdSelection service.", e));
        } catch (RemoteException e) {
            sLogger.e(e, "Exception");
            receiver.onError(new IllegalStateException("Failure of AdSelection service.", e));
        }
    }

    /**
     * Removes all override data for {@link AdSelectionFromOutcomesConfig} in the Ad Selection API.
     *
     * <p>This method is intended to be used for end-to-end testing. This API is enabled only for
     * apps in debug mode with developer options enabled.
     *
     * @throws IllegalStateException if this API is not enabled for the caller
     *     <p>The receiver either returns a {@code void} for a successful run, or an {@link
     *     Exception} indicates the error.
     * @hide
     */
    @RequiresPermission(ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public void resetAllAdSelectionFromOutcomesConfigRemoteOverrides(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> receiver) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        try {
            final AdSelectionService service = mAdSelectionManager.getService();
            service.resetAllAdSelectionFromOutcomesConfigRemoteOverrides(
                    new AdSelectionOverrideCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            executor.execute(() -> receiver.onResult(new Object()));
                        }

                        @Override
                        public void onFailure(FledgeErrorResponse failureParcel) {
                            executor.execute(
                                    () ->
                                            receiver.onError(
                                                    AdServicesStatusUtils.asException(
                                                            failureParcel)));
                        }
                    });
        } catch (NullPointerException e) {
            sLogger.e(e, "Unable to find the AdSelection service.");
            receiver.onError(
                    new IllegalStateException("Unable to find the AdSelection service.", e));
        } catch (RemoteException e) {
            sLogger.e(e, "Exception");
            receiver.onError(new IllegalStateException("Failure of AdSelection service.", e));
        }
    }

    /**
     * Sets the override for event histogram data, which is used in frequency cap filtering during
     * ad selection.
     *
     * <p>This method is intended to be used for end-to-end testing. This API is enabled only for
     * apps in debug mode with developer options enabled.
     *
     * <p>The given {@code outcomeReceiver} either returns an empty {@link Object} if successful or
     * an {@link Exception} which indicates the error.
     *
     * @throws IllegalStateException if this API is not enabled for the caller
     * @hide
     */
    // TODO(b/221876775): Unhide for frequency cap API review
    @RequiresPermission(ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public void setAdCounterHistogramOverride(
            @NonNull SetAdCounterHistogramOverrideRequest setRequest,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> outcomeReceiver) {
        Objects.requireNonNull(setRequest, "Request must not be null");
        Objects.requireNonNull(executor, "Executor must not be null");
        Objects.requireNonNull(outcomeReceiver, "Outcome receiver must not be null");

        try {
            final AdSelectionService service =
                    Objects.requireNonNull(mAdSelectionManager.getService());
            service.setAdCounterHistogramOverride(
                    new SetAdCounterHistogramOverrideInput.Builder()
                            .setAdEventType(setRequest.getAdEventType())
                            .setAdCounterKey(setRequest.getAdCounterKey())
                            .setHistogramTimestamps(setRequest.getHistogramTimestamps())
                            .setBuyer(setRequest.getBuyer())
                            .setCustomAudienceOwner(setRequest.getCustomAudienceOwner())
                            .setCustomAudienceName(setRequest.getCustomAudienceName())
                            .build(),
                    new AdSelectionOverrideCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            executor.execute(() -> outcomeReceiver.onResult(new Object()));
                        }

                        @Override
                        public void onFailure(FledgeErrorResponse failureParcel) {
                            executor.execute(
                                    () ->
                                            outcomeReceiver.onError(
                                                    AdServicesStatusUtils.asException(
                                                            failureParcel)));
                        }
                    });
        } catch (NullPointerException e) {
            sLogger.e(e, "Unable to find the AdSelection service");
            outcomeReceiver.onError(
                    new IllegalStateException("Unable to find the AdSelection service", e));
        } catch (RemoteException e) {
            sLogger.e(e, "Remote exception encountered while updating ad counter histogram");
            outcomeReceiver.onError(new IllegalStateException("Failure of AdSelection service", e));
        }
    }

    /**
     * Removes an override for event histogram data, which is used in frequency cap filtering during
     * ad selection.
     *
     * <p>This method is intended to be used for end-to-end testing. This API is enabled only for
     * apps in debug mode with developer options enabled.
     *
     * <p>The given {@code outcomeReceiver} either returns an empty {@link Object} if successful or
     * an {@link Exception} which indicates the error.
     *
     * @throws IllegalStateException if this API is not enabled for the caller
     * @hide
     */
    // TODO(b/221876775): Unhide for frequency cap API review
    @RequiresPermission(ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public void removeAdCounterHistogramOverride(
            @NonNull RemoveAdCounterHistogramOverrideRequest removeRequest,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> outcomeReceiver) {
        Objects.requireNonNull(removeRequest, "Request must not be null");
        Objects.requireNonNull(executor, "Executor must not be null");
        Objects.requireNonNull(outcomeReceiver, "Outcome receiver must not be null");

        try {
            final AdSelectionService service =
                    Objects.requireNonNull(mAdSelectionManager.getService());
            service.removeAdCounterHistogramOverride(
                    new RemoveAdCounterHistogramOverrideInput.Builder()
                            .setAdEventType(removeRequest.getAdEventType())
                            .setAdCounterKey(removeRequest.getAdCounterKey())
                            .setBuyer(removeRequest.getBuyer())
                            .build(),
                    new AdSelectionOverrideCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            executor.execute(() -> outcomeReceiver.onResult(new Object()));
                        }

                        @Override
                        public void onFailure(FledgeErrorResponse failureParcel) {
                            executor.execute(
                                    () ->
                                            outcomeReceiver.onError(
                                                    AdServicesStatusUtils.asException(
                                                            failureParcel)));
                        }
                    });
        } catch (NullPointerException e) {
            sLogger.e(e, "Unable to find the AdSelection service");
            outcomeReceiver.onError(
                    new IllegalStateException("Unable to find the AdSelection service", e));
        } catch (RemoteException e) {
            sLogger.e(e, "Remote exception encountered while updating ad counter histogram");
            outcomeReceiver.onError(new IllegalStateException("Failure of AdSelection service", e));
        }
    }

    /**
     * Removes all previously set histogram overrides used in ad selection which were set by the
     * caller application.
     *
     * <p>This method is intended to be used for end-to-end testing. This API is enabled only for
     * apps in debug mode with developer options enabled.
     *
     * <p>The given {@code outcomeReceiver} either returns an empty {@link Object} if successful or
     * an {@link Exception} which indicates the error.
     *
     * @throws IllegalStateException if this API is not enabled for the caller
     * @hide
     */
    // TODO(b/221876775): Unhide for frequency cap API review
    @RequiresPermission(ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public void resetAllAdCounterHistogramOverrides(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> outcomeReceiver) {
        Objects.requireNonNull(executor, "Executor must not be null");
        Objects.requireNonNull(outcomeReceiver, "Outcome receiver must not be null");

        try {
            final AdSelectionService service =
                    Objects.requireNonNull(mAdSelectionManager.getService());
            service.resetAllAdCounterHistogramOverrides(
                    new AdSelectionOverrideCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            executor.execute(() -> outcomeReceiver.onResult(new Object()));
                        }

                        @Override
                        public void onFailure(FledgeErrorResponse failureParcel) {
                            executor.execute(
                                    () ->
                                            outcomeReceiver.onError(
                                                    AdServicesStatusUtils.asException(
                                                            failureParcel)));
                        }
                    });
        } catch (NullPointerException e) {
            sLogger.e(e, "Unable to find the AdSelection service");
            outcomeReceiver.onError(
                    new IllegalStateException("Unable to find the AdSelection service", e));
        } catch (RemoteException e) {
            sLogger.e(e, "Remote exception encountered while updating ad counter histogram");
            outcomeReceiver.onError(new IllegalStateException("Failure of AdSelection service", e));
        }
    }
}
