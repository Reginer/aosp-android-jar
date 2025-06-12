/*
 * Copyright 2024 The Android Open Source Project
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

package android.ranging;

import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresNoPermission;
import android.annotation.SystemService;
import android.content.AttributionSource;
import android.content.Context;

import com.android.ranging.flags.Flags;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;
import java.util.concurrent.Executor;


/**
 * This class provides a way to perform ranging operations such as querying the
 * device's capabilities and determining the distance and angle between the local device and a
 * remote device.
 *
 * <p>To get a {@link RangingManager}, call the
 * <code>Context.getSystemService(RangingManager.class)</code>.
 *
 */

@SystemService(Context.RANGING_SERVICE)
@FlaggedApi(Flags.FLAG_RANGING_STACK_ENABLED)
public final class RangingManager {
    private static final String TAG = "RangingManager";

    private final Context mContext;
    private final IRangingAdapter mRangingAdapter;
    private final CapabilitiesListener mCapabilitiesListener;

    private final RangingSessionManager mRangingSessionManager;

    /**
     * The interface Ranging technology.
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.TYPE_USE})
    @IntDef({
            UWB,
            BLE_CS,
            WIFI_NAN_RTT,
            BLE_RSSI,
    })
    public @interface RangingTechnology {}
    /**
     * Ultra-Wideband (UWB) technology.
     */
    public static final int UWB = 0;

    /**
     * Bluetooth Channel Sounding (BT-CS) technology.
     */
    public static final int BLE_CS = 1;

    /**
     * WiFi Round Trip Time (WiFi-RTT) technology.
     */
    public static final int WIFI_NAN_RTT = 2;

    /**
     * Bluetooth Low Energy (BLE) RSSI-based ranging technology.
     */
    public static final int BLE_RSSI = 3;

    /**
     * @hide
     */
    public RangingManager(@NonNull Context context, @NonNull IRangingAdapter adapter) {
        mContext = context;
        mRangingAdapter = adapter;
        mCapabilitiesListener = new CapabilitiesListener(adapter);
        mRangingSessionManager = new RangingSessionManager(adapter);
    }

    /**
     * Registers a callback to receive ranging capabilities updates.
     *
     * @param executor The {@link Executor} on which the callback will be executed.
     *                 Must not be null.
     * @param callback The {@link RangingCapabilitiesCallback} that will handle the
     *                 capabilities updates. Must not be null.
     * @throws NullPointerException if the {@code executor} or {@code callback} is null.
     */
    @RequiresNoPermission
    @NonNull
    public void registerCapabilitiesCallback(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull RangingCapabilitiesCallback callback) {
        Objects.requireNonNull(executor, "Executor cannot be null");
        Objects.requireNonNull(callback, "Capabilities callback cannot be null");
        mCapabilitiesListener.register(executor, callback);
    }

    /**
     * Unregisters a previously registered ranging capabilities callback.
     *
     * @param callback The {@link RangingCapabilitiesCallback} to be unregistered.
     *                 Must not be null.
     * @throws NullPointerException if the {@code callback} is null.
     */
    @RequiresNoPermission
    @NonNull
    public void unregisterCapabilitiesCallback(@NonNull RangingCapabilitiesCallback callback) {
        Objects.requireNonNull(callback, "Capabilities callback cannot be null");
        mCapabilitiesListener.unregister(callback);
    }

    /**
     * Creates a new ranging session. A ranging session enables the application
     * to perform ranging operations using available technologies such as
     * UWB (Ultra-Wideband) or WiFi RTT (Round Trip Time).
     *
     * <p>This method returns a {@link RangingSession} instance, which can be
     * used to initiate, manage, and stop ranging operations. The provided
     * {@link RangingSession.Callback} will be used to receive session-related
     * events, such as session start, stop, and ranging updates.
     *
     * <p>It is recommended to provide an appropriate {@link Executor} to ensure
     * that callback events are handled on a suitable thread.
     *
     * @param callback the {@link RangingSession.Callback} to handle session-related events.
     *                 Must not be {@code null}.
     * @param executor the {@link Executor} on which the callback will be invoked.
     *                 Must not be {@code null}.
     * @return the {@link RangingSession} instance if the session was successfully created,
     * or {@code null} if the session could not be created.
     * @throws NullPointerException if {@code callback} or {@code executor} is null.
     * @throws SecurityException    if the calling app does not have the necessary permissions
     *                              to create a ranging session.
     */
    @RequiresNoPermission
    @Nullable
    public RangingSession createRangingSession(@NonNull Executor executor,
            @NonNull RangingSession.Callback callback) {
        Objects.requireNonNull(executor, "Executor cannot be null");
        Objects.requireNonNull(callback, "Callback cannot be null");
        return createRangingSessionInternal(mContext.getAttributionSource(), callback, executor);
    }

    private RangingSession createRangingSessionInternal(AttributionSource attributionSource,
            RangingSession.Callback callback, Executor executor) {
        return mRangingSessionManager.createRangingSessionInstance(attributionSource, callback,
                executor);
    }

    /**
     * Callback interface to receive the availabilities and capabilities of all the ranging
     * technology supported by the device.
     *
     * <p>This interface is used to asynchronously provide information about the
     * supported ranging capabilities of the device. The callback is invoked first time when
     * registered and if any capabilities are updated until it is unregistered. </p>
     */
    public interface RangingCapabilitiesCallback {

        /**
         * Called when the ranging capabilities are available.
         *
         * @param capabilities the {@link RangingCapabilities} object containing
         *                     detailed information about the supported features
         *                     and limitations of the ranging technology.
         */
        void onRangingCapabilities(@NonNull RangingCapabilities capabilities);
    }
}
