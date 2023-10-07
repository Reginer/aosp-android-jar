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

package com.android.internal.telephony.domainselection;

import static android.telephony.TelephonyManager.HAL_SERVICE_NETWORK;

import static com.android.internal.telephony.RIL.RADIO_HAL_VERSION_2_1;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.telephony.DomainSelectionService;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * This class is an entry point to provide whether the AOSP domain selection is supported or not,
 * and bind the {@link DomainSelectionController} with the given {@link DomainSelectionService} to
 * provide a specific {@link DomainSelectionConnection} object for communicating with each domain
 * selector.
 */
public class DomainSelectionResolver {
    private static final String TAG = DomainSelectionResolver.class.getSimpleName();
    private static DomainSelectionResolver sInstance = null;

    /**
     * Creates the DomainSelectionResolver singleton instance.
     *
     * @param context The context of the application.
     * @param deviceConfigEnabled The flag to indicate whether or not the device supports
     *                            the domain selection service or not.
     */
    public static void make(Context context, boolean deviceConfigEnabled) {
        if (sInstance == null) {
            sInstance = new DomainSelectionResolver(context, deviceConfigEnabled);
        }
    }

    /**
     * Returns the singleton instance of DomainSelectionResolver.
     *
     * @return A {@link DomainSelectionResolver} instance.
     */
    public static DomainSelectionResolver getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("DomainSelectionResolver is not ready!");
        }
        return sInstance;
    }

    /**
     * Sets a {@link DomainSelectionResolver} for injecting mock DomainSelectionResolver.
     *
     * @param resolver A {@link DomainSelectionResolver} instance to test.
     */
    @VisibleForTesting
    public static void setDomainSelectionResolver(DomainSelectionResolver resolver) {
        sInstance = resolver;
    }

    /**
     * Testing interface for injecting mock DomainSelectionController.
     */
    @VisibleForTesting
    public interface DomainSelectionControllerFactory {
        /**
         * Returns a {@link DomainSelectionController} created using the specified
         * context and {@link DomainSelectionService} instance.
         */
        DomainSelectionController create(@NonNull Context context,
                @NonNull DomainSelectionService service);
    }

    private DomainSelectionControllerFactory mDomainSelectionControllerFactory =
            new DomainSelectionControllerFactory() {
        @Override
        public DomainSelectionController create(@NonNull Context context,
                @NonNull DomainSelectionService service) {
            return new DomainSelectionController(context, service);
        }
    };

    // Persistent Logging
    private final LocalLog mEventLog = new LocalLog(10);
    private final Context mContext;
    // The flag to indicate whether the device supports the domain selection service or not.
    private final boolean mDeviceConfigEnabled;
    // DomainSelectionController, which are bound to DomainSelectionService.
    private DomainSelectionController mController;

    public DomainSelectionResolver(Context context, boolean deviceConfigEnabled) {
        mContext = context;
        mDeviceConfigEnabled = deviceConfigEnabled;
        logi("DomainSelectionResolver created: device-config=" + deviceConfigEnabled);
    }

    /**
     * Checks if the device supports the domain selection service to route the call / SMS /
     * supplementary services to the appropriate domain.
     * This checks the device-config and Radio HAL version for supporting the domain selection.
     * The domain selection requires the Radio HAL version greater than or equal to 2.1.
     *
     * @return {@code true} if the domain selection is supported on the device,
     *         {@code false} otherwise.
     */
    public boolean isDomainSelectionSupported() {
        return mDeviceConfigEnabled && PhoneFactory.getDefaultPhone()
                .getHalVersion(HAL_SERVICE_NETWORK).greaterOrEqual(RADIO_HAL_VERSION_2_1);
    }

    /**
     * Returns a {@link DomainSelectionConnection} instance.
     *
     * @param phone The Phone instance for witch this request is.
     * @param selectorType Indicates the selector type requested.
     * @param isEmergency Indicates whether this is for emergency service.
     * @throws IllegalStateException If the {@link DomainSelectionController} is not created
     *         because {@link #initialize} method is not called even if the domain selection is
     *         supported.
     * @return A {@link DomainSelectionConnection} instance if the device supports
     *         AOSP domain selection and IMS is available or {@code null} otherwise.
     */
    public @Nullable DomainSelectionConnection getDomainSelectionConnection(Phone phone,
            @DomainSelectionService.SelectorType int selectorType, boolean isEmergency) {
        if (mController == null) {
            // If the caller calls this method without checking whether the domain selection
            // is supported or not, this exception will be thrown.
            throw new IllegalStateException("DomainSelection is not supported!");
        }

        if (phone == null || !phone.isImsAvailable()) {
            // If ImsPhone is null or the binder of ImsService is not available,
            // CS domain is used for the telephony services.
            return null;
        }

        return mController.getDomainSelectionConnection(phone, selectorType, isEmergency);
    }

    /** Sets a factory interface for creating {@link DomainSelectionController} instance. */
    @VisibleForTesting
    public void setDomainSelectionControllerFactory(DomainSelectionControllerFactory factory) {
        mDomainSelectionControllerFactory = factory;
    }

    /**
     * Needs to be called after the constructor to create a {@link DomainSelectionController} that
     * is bound to the given {@link DomainSelectionService}.
     *
     * @param service A {@link DomainSelectionService} to be bound.
     */
    public void initialize(@NonNull DomainSelectionService service) {
        logi("Initialize.");
        mController = mDomainSelectionControllerFactory.create(mContext, service);
    }

    /**
     * Dumps this instance into a readable format for dumpsys usage.
     */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        ipw.println("Resolver:");
        ipw.increaseIndent();
        ipw.println("Event Log:");
        ipw.increaseIndent();
        mEventLog.dump(ipw);
        ipw.decreaseIndent();
        ipw.decreaseIndent();

        ipw.println("Controller:");
        ipw.increaseIndent();
        DomainSelectionController controller = mController;
        if (controller == null) {
            ipw.println("no active controller");
        } else {
            controller.dump(ipw);
        }
        ipw.decreaseIndent();
    }

    private void logi(String s) {
        Log.i(TAG, s);
        mEventLog.log(s);
    }
}
