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

package com.android.clockwork.displayoffload;

import static com.android.clockwork.displayoffload.DebugUtils.DEBUG_HAL;
import static com.android.clockwork.displayoffload.DebugUtils.DEBUG_HAL_DUMP;

import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_HAL_STATUS_NOT_OK;

import android.content.Context;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.os.Bundle;
import android.os.IHwBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.clockwork.displayoffload.HalTypeConverter.HalTypeConverterSupplier;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import vendor.google_clockwork.displayoffload.V1_0.CustomResource;
import vendor.google_clockwork.displayoffload.V1_0.LinearMetricMapping;
import vendor.google_clockwork.displayoffload.V1_0.ModuloMapping;
import vendor.google_clockwork.displayoffload.V1_0.OffloadConstant;
import vendor.google_clockwork.displayoffload.V1_0.OffloadExpression;
import vendor.google_clockwork.displayoffload.V1_0.OffloadMetric;
import vendor.google_clockwork.displayoffload.V1_0.OffloadString;
import vendor.google_clockwork.displayoffload.V1_0.PngResource;
import vendor.google_clockwork.displayoffload.V1_0.RangeMapping;
import vendor.google_clockwork.displayoffload.V1_0.RotationGroup;
import vendor.google_clockwork.displayoffload.V1_0.SpriteSheetPngResource;
import vendor.google_clockwork.displayoffload.V1_0.Status;
import vendor.google_clockwork.displayoffload.V1_0.StringResource;
import vendor.google_clockwork.displayoffload.V1_0.TranslationGroup;
import vendor.google_clockwork.displayoffload.V1_1.ArcPathResource;
import vendor.google_clockwork.displayoffload.V1_1.CeilMapping;
import vendor.google_clockwork.displayoffload.V1_1.FloorMapping;
import vendor.google_clockwork.displayoffload.V1_1.IDisplayOffload;
import vendor.google_clockwork.displayoffload.V1_1.LocaleConfig;
import vendor.google_clockwork.displayoffload.V1_1.NumberFormatMapping;
import vendor.google_clockwork.displayoffload.V1_1.ReciprocalMapping;
import vendor.google_clockwork.displayoffload.V1_1.RectShapeResource;
import vendor.google_clockwork.displayoffload.V1_1.RoundRectShapeResource;
import vendor.google_clockwork.displayoffload.V2_0.BinaryOperation;
import vendor.google_clockwork.displayoffload.V2_0.BitmapDrawable;
import vendor.google_clockwork.displayoffload.V2_0.LinePath;
import vendor.google_clockwork.displayoffload.V2_0.NumberFormatResource;
import vendor.google_clockwork.displayoffload.V2_0.Primitive;
import vendor.google_clockwork.displayoffload.V2_0.RectShape;
import vendor.google_clockwork.displayoffload.V2_0.RoundRectShape;
import vendor.google_clockwork.displayoffload.V2_0.TernaryOperation;
import vendor.google_clockwork.displayoffload.V2_0.UnaryOperation;

/**
 * HalAdapter is a wrapper class for different versions of HALs.
 *
 * The purpose of this class is to decouple the hal references from DisplayOffloadService(DOS).
 * This way, DOS can request for HALs' operations without caring about HALs' version. Therefore,
 * when new HAL version is added to framework, DOS doesn't need to change.
 *
 * The HalAdapter registers all versions of HAL. When an HAL operation is requested,
 * HalAdapter tries to use the highest version of (successfully registered)HAL.
 */
public class HalAdapter extends IServiceNotification.Stub
        implements IHwBinder.DeathRecipient, HalTypeConverterSupplier {
    private static final String TAG = "DOHalAdapter";
    private static final boolean STUB_HAL = false;
    private static final boolean DEBUG_V2_RETURN_OK = true;
    private final Context mContext;
    private final HalTypeConverter mHalTypeConverterV1;
    private final HalTypeConverter mHalTypeConverterV2;
    private boolean mFirstConnectionAfterBoot = true;

    // HAL V1 & V1.1
    @VisibleForTesting
    vendor.google_clockwork.displayoffload.V1_0.IDisplayOffload mDisplayOffloadV1_0;
    @VisibleForTesting
    vendor.google_clockwork.displayoffload.V1_1.IDisplayOffload mDisplayOffloadV1_1;
    // HAL V2
    @VisibleForTesting
    vendor.google_clockwork.displayoffload.V2_0.IDisplayOffload mDisplayOffloadV2_0;

    @VisibleForTesting HalListener mHalListener;

    HalAdapter(Context context) {
        mContext = context;
        mHalTypeConverterV1 = new HalTypeConverterV1(mContext);
        mHalTypeConverterV2 = new HalTypeConverterV2(mContext);
    }

    synchronized void attachHalListener(HalListener listener) {
        mHalListener = listener;
    }

    @VisibleForTesting
    void registerHalV1(vendor.google_clockwork.displayoffload.V1_0.IDisplayOffload halV1_0) {
        try {
            vendor.google_clockwork.displayoffload.V1_0.IDisplayOffload hal =
                    vendor.google_clockwork.displayoffload.V1_0.IDisplayOffload.getService();
            hal = hal == null ? halV1_0 : hal;
            if (hal == null) {
                Log.i(TAG, "[HAL] 1.0 is unavailable.");
                return;
            }
            Log.i(TAG, "[HAL] Got 1.0.");
            mDisplayOffloadV1_0 = hal;
            mDisplayOffloadV1_0.linkToDeath(this, 1);
        } catch (RemoteException e) {
            Log.e(TAG, "[HAL] Exception talking to the HAL: ", e);
        }

        try {
            IServiceManager serviceManager = IServiceManager.getService();
            if (serviceManager != null && (serviceManager.getTransport(
                    vendor.google_clockwork.displayoffload.V1_1.IDisplayOffload.kInterfaceName,
                    "default") != IServiceManager.Transport.EMPTY)) {
                mDisplayOffloadV1_1 =
                        vendor.google_clockwork.displayoffload.V1_1.IDisplayOffload.castFrom(
                                mDisplayOffloadV1_0);
            } else {
                mDisplayOffloadV1_1 = (IDisplayOffload) halV1_0;
            }
            if (mDisplayOffloadV1_1 != null) {
                mDisplayOffloadV1_1.linkToDeath(this, 1);
                Log.i(TAG, "[HAL] Got 1.1.");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "[HAL] Exception while operating on IServiceManager: " + e);
        } catch (NoSuchElementException e) {
            Log.e(TAG, "[HAL] Failed to convert to V1.1 interface", e);
        }
    }

    @VisibleForTesting
    void registerHalV2(vendor.google_clockwork.displayoffload.V2_0.IDisplayOffload halV2_0) {
        try {
            vendor.google_clockwork.displayoffload.V2_0.IDisplayOffload hal =
                    vendor.google_clockwork.displayoffload.V2_0.IDisplayOffload.getService();
            hal = hal == null ? halV2_0 : hal;
            if (hal == null) {
                Log.i(TAG, "[HAL] 2.0 is unavailable.");
                return;
            }
            Log.i(TAG, "[HAL] Got 2.0.");
            mDisplayOffloadV2_0 = hal;
            mDisplayOffloadV2_0.linkToDeath(this, 2);
        } catch (RemoteException e) {
            Log.e(TAG, "[HAL] Exception talking to the HAL: ", e);
        }
    }

    synchronized boolean isHalConnected() {
        return mDisplayOffloadV1_0 != null || mDisplayOffloadV1_1 != null
                || mDisplayOffloadV2_0 != null;
    }

    synchronized List<String> getConnectedHalVersions() {
        List<String> connectedHals = new ArrayList<>();
        if (mDisplayOffloadV1_0 != null) {
            connectedHals.add("V1.0");
        }
        if (mDisplayOffloadV1_1 != null) {
            connectedHals.add("V1.1");
        }
        if (mDisplayOffloadV2_0 != null) {
            connectedHals.add("V2.0");
        }
        return connectedHals;
    }

    synchronized int beginDisplay() throws RemoteException {
        if (mDisplayOffloadV2_0 != null) {
            return mDisplayOffloadV2_0.beginDisplayControl();
        } else if (mDisplayOffloadV1_0 != null) {
            return mDisplayOffloadV1_0.beginDisplay();
        }
        return Status.UNSUPPORTED_OPERATION;
    }

    synchronized void endDisplay() throws RemoteException {
        if (mDisplayOffloadV2_0 != null) {
            mDisplayOffloadV2_0.endDisplayControl();
        } else if (mDisplayOffloadV1_0 != null) {
            mDisplayOffloadV1_0.endDisplay();
        }
    }

    synchronized int beginRendering() throws RemoteException {
        if (mDisplayOffloadV2_0 != null) {
            return mDisplayOffloadV2_0.beginRendering();
        } else if (mDisplayOffloadV1_1 != null) {
            return mDisplayOffloadV1_1.beginRendering();
        }
        return Status.UNSUPPORTED_OPERATION;
    }

    synchronized void endRendering() throws RemoteException {
        if (mDisplayOffloadV2_0 != null) {
            mDisplayOffloadV2_0.endRendering();
        } else if (mDisplayOffloadV1_1 != null) {
            mDisplayOffloadV1_1.endRendering();
        }
    }

    synchronized boolean isRenderControlSupported() {
        return (mDisplayOffloadV1_1 != null || mDisplayOffloadV2_0 != null);
    }

    synchronized Bundle fetchDataSnapshot() throws RemoteException {
        if (mDisplayOffloadV2_0 != null) {
            return mHalTypeConverterV2.callbackBundleFromAnnotatedKeyValuePairs(
                    mDisplayOffloadV2_0.fetchDataSnapshot());
        } else if (mDisplayOffloadV1_1 != null) {
            return mHalTypeConverterV1.callbackBundleFromAnnotatedKeyValuePairs(
                    mDisplayOffloadV1_1.fetchDataSnapshot());
        }
        return null;
    }

    synchronized int setBrightnessConfiguration(boolean brightenOnWristTilt,
            ArrayList<Short> alsThresholds, ArrayList<Short> brightnessValuesDim,
            ArrayList<Short> brightnessValuesBright) throws RemoteException {
        if (mDisplayOffloadV2_0 != null) {
            return mDisplayOffloadV2_0.setBrightnessConfiguration(brightenOnWristTilt,
                    alsThresholds, brightnessValuesDim, brightnessValuesBright);
        } else if (mDisplayOffloadV1_0 != null) {
            return mDisplayOffloadV1_0.setBrightnessConfiguration(brightenOnWristTilt,
                    alsThresholds, brightnessValuesDim, brightnessValuesBright);
        }
        Log.e(TAG, "sendBrightnessCurvesLocked(): No Display Offload HAL!");
        return -1;
    }

    // LINT.IfChange(hal_send)
    private synchronized int sendV1(Object halObject)
            throws RemoteException, DisplayOffloadException {
        int status = Status.UNSUPPORTED_OPERATION;
        // displayoffload V1.0
        if (halObject instanceof CustomResource) {
            status = mDisplayOffloadV1_0.sendCustomResource((CustomResource) halObject);
        } else if (halObject instanceof TranslationGroup) {
            status = mDisplayOffloadV1_0.sendTranslationGroup((TranslationGroup) halObject);
        } else if (halObject instanceof RotationGroup) {
            status = mDisplayOffloadV1_0.sendRotationGroup((RotationGroup) halObject);
        } else if (halObject instanceof PngResource) {
            status = mDisplayOffloadV1_0.sendPngResource((PngResource) halObject);
        } else if (halObject instanceof SpriteSheetPngResource) {
            status = mDisplayOffloadV1_0.sendSpriteSheetPngResource(
                    (SpriteSheetPngResource) halObject);
        } else if (halObject instanceof TtfFontAdapter) {
            status = mDisplayOffloadV1_0.sendTtfFontResource(
                    ((TtfFontAdapter) halObject).getTtfFontResourceV1());
        } else if (halObject instanceof StringResource) {
            status = mDisplayOffloadV1_0.sendStringResource((StringResource) halObject);
        } else if (halObject instanceof OffloadString) {
            status = mDisplayOffloadV1_0.sendOffloadString((OffloadString) halObject);
        } else if (halObject instanceof OffloadExpression) {
            status = mDisplayOffloadV1_0.sendOffloadExpression((OffloadExpression) halObject);
        } else if (halObject instanceof OffloadMetric) {
            status = mDisplayOffloadV1_0.sendOffloadMetric((OffloadMetric) halObject);
        } else if (halObject instanceof OffloadConstant) {
            status = mDisplayOffloadV1_0.sendOffloadConstant((OffloadConstant) halObject);
        } else if (halObject instanceof LinearMetricMapping) {
            status = mDisplayOffloadV1_0.sendLinearMetricMapping((LinearMetricMapping) halObject);
        } else if (halObject instanceof RangeMapping) {
            status = mDisplayOffloadV1_0.sendRangeMapping((RangeMapping) halObject);
        } else if (halObject instanceof ModuloMapping) {
            status = mDisplayOffloadV1_0.sendModuloMapping((ModuloMapping) halObject);
        } else {
            // displayoffload V1.1 types
            if (mDisplayOffloadV1_1 == null) {
                Log.e(TAG, "HAL 1.1 not up");
                return status = Status.UNSUPPORTED_OPERATION;
            } else if (halObject instanceof ArcPathResource) {
                status = mDisplayOffloadV1_1.sendArcPathResource((ArcPathResource) halObject);
            } else if (halObject instanceof RectShapeResource) {
                status = mDisplayOffloadV1_1.sendRectShapeResource((RectShapeResource) halObject);
            } else if (halObject instanceof RoundRectShapeResource) {
                status = mDisplayOffloadV1_1.sendRoundRectShapeResource(
                        (RoundRectShapeResource) halObject);
            } else if (halObject instanceof StaticTextAdapter) {
                status = mDisplayOffloadV1_1.sendStaticTextResource(
                        ((StaticTextAdapter) halObject).getV1());
            } else if (halObject instanceof DynamicTextAdapter) {
                status = mDisplayOffloadV1_1.sendDynamicTextResource(
                        ((DynamicTextAdapter) halObject).getV1());
            } else if (halObject instanceof NumberFormatMapping) {
                status = mDisplayOffloadV1_1.sendNumberFormatMapping(
                        (NumberFormatMapping) halObject);
            } else if (halObject instanceof ReciprocalMapping) {
                status = mDisplayOffloadV1_1.sendReciprocalMapping((ReciprocalMapping) halObject);
            } else if (halObject instanceof CeilMapping) {
                status = mDisplayOffloadV1_1.sendCeilMapping((CeilMapping) halObject);
            } else if (halObject instanceof FloorMapping) {
                status = mDisplayOffloadV1_1.sendFloorMapping((FloorMapping) halObject);
            } else {
                // Unrecognized type
                Log.e(TAG, "Unrecognized type for object: " + halObject);
            }
        }
        return status;
    }

    private synchronized int sendV2(Object halObject)
            throws RemoteException, DisplayOffloadException {
        int status = DEBUG_V2_RETURN_OK ? Status.OK : Status.UNSUPPORTED_OPERATION;

        if (halObject instanceof vendor.google_clockwork.displayoffload.V2_0.ArcPath) {
            status =
                    mDisplayOffloadV2_0.addArcPath(
                            (vendor.google_clockwork.displayoffload.V2_0.ArcPath) halObject);
        } else if (halObject instanceof BinaryOperation) {
            status = mDisplayOffloadV2_0.addBinaryOperation((BinaryOperation) halObject);
        } else if (halObject
                instanceof vendor.google_clockwork.displayoffload.V2_0.CustomResource) {
            status =
                    mDisplayOffloadV2_0.addCustomResource(
                            (vendor.google_clockwork.displayoffload.V2_0.CustomResource) halObject);
        } else if (halObject instanceof DynamicTextAdapter) {
            status = mDisplayOffloadV2_0.addDynamicText(((DynamicTextAdapter) halObject).getV2());
        } else if (halObject instanceof LinePath) {
            status = mDisplayOffloadV2_0.addLinePath((LinePath) halObject);
        } else if (halObject instanceof NumberFormatResource) {
            status = mDisplayOffloadV2_0.addNumberFormatResource((NumberFormatResource) halObject);
        } else if (halObject instanceof vendor.google_clockwork.displayoffload.V2_0.OffloadMetric) {
            status =
                    mDisplayOffloadV2_0.addOffloadMetric(
                            (vendor.google_clockwork.displayoffload.V2_0.OffloadMetric) halObject);
        } else if (halObject instanceof BitmapDrawable) {
            status = mDisplayOffloadV2_0.addBitmapDrawable((BitmapDrawable) halObject);
        } else if (halObject instanceof vendor.google_clockwork.displayoffload.V2_0.Primitive) {
            status = mDisplayOffloadV2_0.addPrimitive((Primitive) halObject);
        } else if (halObject instanceof vendor.google_clockwork.displayoffload.V2_0.RectShape) {
            status = mDisplayOffloadV2_0.addRectShape((RectShape) halObject);
        } else if (halObject instanceof vendor.google_clockwork.displayoffload.V2_0.RotationGroup) {
            status = mDisplayOffloadV2_0.addRotationGroup(
                    (vendor.google_clockwork.displayoffload.V2_0.RotationGroup) halObject);
        } else if (halObject
                instanceof vendor.google_clockwork.displayoffload.V2_0.RoundRectShape) {
            status = mDisplayOffloadV2_0.addRoundRectShape((RoundRectShape) halObject);
        } else if (halObject instanceof StaticTextAdapter) {
            status = mDisplayOffloadV2_0.addStaticText(((StaticTextAdapter) halObject).getV2());
        } else if (halObject instanceof TernaryOperation) {
            status = mDisplayOffloadV2_0.addTernaryOperation((TernaryOperation) halObject);
        } else if (halObject
                instanceof vendor.google_clockwork.displayoffload.V2_0.TranslationGroup) {
            status =
                    mDisplayOffloadV2_0.addTranslationGroup(
                            (vendor.google_clockwork.displayoffload.V2_0.TranslationGroup)
                                    halObject);
        } else if (halObject instanceof TtfFontAdapter) {
            status = mDisplayOffloadV2_0.addTtfFontResource(
                    ((TtfFontAdapter) halObject).getTtfFontResourceV2());
        } else if (halObject instanceof UnaryOperation) {
            status = mDisplayOffloadV2_0.addUnaryOperation((UnaryOperation) halObject);
        }
        return status;
    }
    // LINT.ThenChange(HalTypeConverter.java:aidl_type_conversions)

    // Send to Hal
    public synchronized int send(Object halObject) throws RemoteException, DisplayOffloadException {
        if (DEBUG_HAL_DUMP) {
            DebugUtils.dump(halObject);
        }

        if (STUB_HAL) {
            return Status.OK;
        }

        if (mDisplayOffloadV2_0 != null) {
            return sendV2(halObject);
        } else if (mDisplayOffloadV1_0 != null) {
            return sendV1(halObject);
        }

        Log.e(TAG, "No DisplayOffload HAL is available, skipping.");
        return Status.OK;
    }

    public synchronized int setRoot(int rootId) throws RemoteException {
        if (DEBUG_HAL) {
            Log.d(TAG, "setRoot: id=" + rootId);
        }

        if (STUB_HAL) {
            return Status.OK;
        }

        int status;
        try {
            if (mDisplayOffloadV2_0 != null) {
                status = mDisplayOffloadV2_0.setRootResourceId(rootId);
            } else if (mDisplayOffloadV1_0 != null) {
                status = mDisplayOffloadV1_0.setRootResourceId(rootId);
            } else {
                // HAL is not up, return OK to unblock testing.
                Log.e(TAG, "HAL not up");
                return Status.OK;
            }
            if (status != Status.OK) {
                Log.e(TAG, "setRoot: HAL setRoot not OK, returned " + status);
            }
            return status;
        } catch (RemoteException e) {
            Log.e(TAG, "setRoot: HAL setRoot RemoteException", e);
        }
        return Status.UNKNOWN_ERROR;
    }

    public synchronized void resetResource() {
        if (DEBUG_HAL) {
            Log.d(TAG, "resetResource");
        }

        if (STUB_HAL) {
            return;
        }

        int status;
        try {
            if (mDisplayOffloadV2_0 != null) {
                status = mDisplayOffloadV2_0.reset();
            } else if (mDisplayOffloadV1_0 != null) {
                status = mDisplayOffloadV1_0.resetResource();
            } else {
                // HAL is not up
                Log.e(TAG, "HAL not up");
                return;
            }

            if (status != Status.OK) {
                Log.e(TAG, "resetHALStateLocked: HAL resetResource not OK, returned " + status);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "resetHALStateLocked: HAL resetResource RemoteException", e);
        }
    }

    public synchronized void setLocaleConfig() {
        if (DEBUG_HAL) {
            Log.d(TAG, "setLocaleConfig");
        }

        if (STUB_HAL) {
            return;
        }

        int status;
        try {
            LocaleConfig localeConfig =
                    LocaleHelper.getCurrentLocaleConfig(mContext).getHalObject();
            if (mDisplayOffloadV2_0 != null) {
                Log.d(TAG, "setLocaleConfig 2.0 ");
                status = mDisplayOffloadV2_0.setLocaleConfig(localeConfig);
            } else if (mDisplayOffloadV1_1 != null) {
                Log.d(TAG, "setLocaleConfig 1.1 ");
                status = mDisplayOffloadV1_1.setLocaleConfig(localeConfig);
            } else {
                // HAL is not up
                Log.e(TAG, "HAL not up or not supporting setLocaleConfig");
                return;
            }
            if (status != Status.OK) {
                Log.e(TAG, "setLocaleConfig: HAL not OK, returned " + status);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "setLocaleConfig: HAL RemoteException", e);
        }
    }

    public synchronized void registerHalRegistrationNotification() {
        try {
            IServiceManager serviceManager = IServiceManager.getService();

            List<Boolean> halCallbackRegistrationStatusList = new ArrayList<>();
            halCallbackRegistrationStatusList.add(
                    serviceManager.registerForNotifications(
                            vendor.google_clockwork.displayoffload.V1_0.IDisplayOffload
                                    .kInterfaceName,
                            "",
                            this));
            halCallbackRegistrationStatusList.add(
                    serviceManager.registerForNotifications(
                            vendor.google_clockwork.displayoffload.V2_0.IDisplayOffload
                                    .kInterfaceName,
                            "",
                            this));

            for (int i = 0; i < halCallbackRegistrationStatusList.size(); i++) {
                if (!halCallbackRegistrationStatusList.get(i)) {
                    Log.e(TAG, "HAL V" + i + " service manager callback registration failed");
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Exception while registering callbacks: " + e);
        }
    }

    public synchronized void onRegistration(String fqName, String name, boolean preexisting)
            throws RemoteException {
        Log.d(
                TAG,
                "[HAL] onRegistration: service notification fqName="
                        + fqName
                        + ", name="
                        + name
                        + ", preexisting="
                        + preexisting);
        if (fqName.equals(
                vendor.google_clockwork.displayoffload.V1_0.IDisplayOffload.kInterfaceName)) {
            registerHalV1(null);
        } else if (fqName.equals(
                vendor.google_clockwork.displayoffload.V2_0.IDisplayOffload.kInterfaceName)) {
            registerHalV2(null);
        }

        if (isAnyHalAvailable()) {
            resetResource();
            if (mFirstConnectionAfterBoot) {
                mFirstConnectionAfterBoot = false;
                endDisplay();
            }
            if (mHalListener != null) {
                mHalListener.onHalRegistered();
            }
        }
    }

    private boolean isAnyHalAvailable() {
        return mDisplayOffloadV1_0 != null
                || mDisplayOffloadV1_1 != null
                || (mDisplayOffloadV2_0 != null);
    }

    @Override
    public void serviceDied(long cookie) {
        Log.w(TAG, "[HAL] V" + cookie + " Died");
        boolean shouldNotify = isAnyHalAvailable();
        if (cookie == 1) {
            if (mDisplayOffloadV1_0 != null) {
                mDisplayOffloadV1_0.asBinder().unlinkToDeath(this);
                mDisplayOffloadV1_0 = null;
            }
            if (mDisplayOffloadV1_1 != null) {
                mDisplayOffloadV1_1.asBinder().unlinkToDeath(this);
                mDisplayOffloadV1_1 = null;
            }
        } else if (cookie == 2) {
            if (mDisplayOffloadV2_0 != null) {
                mDisplayOffloadV2_0.asBinder().unlinkToDeath(this);
                mDisplayOffloadV2_0 = null;
            }
        } else {
            Log.e(TAG, "Unexpected Behavior. Version = " + cookie);
        }
        if (shouldNotify) {
            if (mHalListener != null) {
                mHalListener.onHalDied();
            }
        }
    }

    @Override
    public HalTypeConverter getConverter() throws DisplayOffloadException {
        if (mDisplayOffloadV2_0 != null) {
            return mHalTypeConverterV2;
        }
        if (mDisplayOffloadV1_1 != null || mDisplayOffloadV1_0 != null) {
            return mHalTypeConverterV1;
        }
        throw new DisplayOffloadException(ERROR_HAL_STATUS_NOT_OK, "HAL is not connected");
    }

    // Callback
    interface HalListener {
        void onHalRegistered();

        void onHalDied();
    }
}
