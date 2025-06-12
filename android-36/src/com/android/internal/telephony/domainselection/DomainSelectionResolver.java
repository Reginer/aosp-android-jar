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
import android.content.ComponentName;
import android.content.Context;
import android.os.SystemProperties;
import android.telephony.DomainSelectionService;
import android.text.TextUtils;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.flags.Flags;
import com.android.internal.telephony.util.TelephonyUtils;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * This class is an entry point to provide whether the AOSP domain selection is supported or not,
 * and bind the {@link DomainSelectionController} with the given {@link DomainSelectionService} to
 * provide a specific {@link DomainSelectionConnection} object for communicating with each domain
 * selector.
 */
public class DomainSelectionResolver {
    @VisibleForTesting
    protected static final String PACKAGE_NAME_NONE = "none";
    private static final String TAG = DomainSelectionResolver.class.getSimpleName();
    private static final boolean DBG = TelephonyUtils.IS_DEBUGGABLE;
    /** For test purpose only with userdebug release */
    private static final String PROP_DISABLE_DOMAIN_SELECTION =
            "telephony.test.disable_domain_selection";
    private static DomainSelectionResolver sInstance = null;

    /**
     * Creates the DomainSelectionResolver singleton instance.
     *
     * @param context The context of the application.
     * @param flattenedComponentName A flattened component name for the domain selection service
     *                               to be bound to the domain selection controller.
     */
    public static void make(Context context, String flattenedComponentName) {
        Log.i(TAG, "make useOem=" + Flags.useOemDomainSelectionService());
        if (sInstance == null) {
            sInstance = new DomainSelectionResolver(context, flattenedComponentName);
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
         * Returns a {@link DomainSelectionController} created using the specified context.
         */
        DomainSelectionController create(@NonNull Context context);
    }

    private DomainSelectionControllerFactory mDomainSelectionControllerFactory =
            new DomainSelectionControllerFactory() {
                @Override
                public DomainSelectionController create(@NonNull Context context) {
                    return new DomainSelectionController(context);
                }
            };

    // Persistent Logging
    private final LocalLog mEventLog = new LocalLog(10);
    private final Context mContext;
    // Stores the default component name to bind the domain selection service so that
    // the test can override this component name with their own domain selection service.
    private final ComponentName mDefaultComponentName;
    // DomainSelectionController, which are bound to DomainSelectionService.
    private DomainSelectionController mController;

    public DomainSelectionResolver(Context context, String flattenedComponentName) {
        mContext = context;
        flattenedComponentName = (flattenedComponentName == null) ? "" : flattenedComponentName;
        mDefaultComponentName = ComponentName.unflattenFromString(flattenedComponentName);
        logi("DomainSelectionResolver created: componentName=[" + flattenedComponentName + "]");
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
        if (DBG && SystemProperties.getBoolean(PROP_DISABLE_DOMAIN_SELECTION, false)) {
            logi("Disabled for test");
            return false;
        }
        return mDefaultComponentName != null && PhoneFactory.getDefaultPhone()
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

        if (phone == null || phone.getImsPhone() == null
                || (!(isEmergency && selectorType == DomainSelectionService.SELECTOR_TYPE_CALLING)
                        && !phone.isImsAvailable())) {
            // In case of emergency calls, to recover the temporary failure in IMS service
            // connection, DomainSelection shall be started even when IMS isn't available.
            // DomainSelector will keep finding next available transport.
            // For other telephony services, if the binder of ImsService is not available,
            // CS domain will be used.
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
     * Creates the {@link DomainSelectionController} and requests the domain selection controller
     * to bind to the {@link DomainSelectionService} with the component name.
     */
    public void initialize() {
        logi("Initialize");
        mController = mDomainSelectionControllerFactory.create(mContext);
        if (mDefaultComponentName != null) {
            mController.bind(mDefaultComponentName);
        } else {
            logi("No component name specified for domain selection service.");
        }
    }

    /**
     * Sets the component name of domain selection service to be bound.
     *
     * NOTE: This should only be used for testing.
     *
     * @return {@code true} if the requested operation is successfully done,
     *         {@code false} otherwise.
     */
    public boolean setDomainSelectionServiceOverride(@NonNull ComponentName componentName) {
        if (mController == null) {
            logd("Controller is not initialized.");
            return false;
        }
        logi("setDomainSelectionServiceOverride: " + componentName);
        if (TextUtils.isEmpty(componentName.getPackageName())
                || TextUtils.equals(PACKAGE_NAME_NONE, componentName.getPackageName())) {
            // Unbind the active service connection to the domain selection service.
            mController.unbind();
            return true;
        }
        // Override the domain selection service with the given component name.
        return mController.bind(componentName);
    }

    /**
     * Clears the overridden domain selection service and restores the domain selection service
     * with the default component.
     *
     * NOTE: This should only be used for testing.
     *
     * @return {@code true} if the requested operation is successfully done,
     *         {@code false} otherwise.
     */
    public boolean clearDomainSelectionServiceOverride() {
        if (mController == null) {
            logd("Controller is not initialized.");
            return false;
        }
        logi("clearDomainSelectionServiceOverride");
        mController.unbind();
        return mController.bind(mDefaultComponentName);
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

    private void logd(String s) {
        Log.d(TAG, s);
    }

    private void logi(String s) {
        Log.i(TAG, s);
        mEventLog.log(s);
    }
}
