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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.os.IHwBinder;
import android.os.RemoteException;

import com.google.common.collect.ImmutableList;

import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import vendor.google_clockwork.displayoffload.V1_0.CustomResource;
import vendor.google_clockwork.displayoffload.V1_0.KeyValuePair;
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
import vendor.google_clockwork.displayoffload.V1_0.StringResource;
import vendor.google_clockwork.displayoffload.V1_0.TranslationGroup;
import vendor.google_clockwork.displayoffload.V1_0.TtfFontResource;
import vendor.google_clockwork.displayoffload.V1_1.ArcPathResource;
import vendor.google_clockwork.displayoffload.V1_1.CeilMapping;
import vendor.google_clockwork.displayoffload.V1_1.DynamicTextResource;
import vendor.google_clockwork.displayoffload.V1_1.FloorMapping;
import vendor.google_clockwork.displayoffload.V1_1.IDisplayOffload;
import vendor.google_clockwork.displayoffload.V1_1.LocaleConfig;
import vendor.google_clockwork.displayoffload.V1_1.NumberFormatMapping;
import vendor.google_clockwork.displayoffload.V1_1.ReciprocalMapping;
import vendor.google_clockwork.displayoffload.V1_1.RectShapeResource;
import vendor.google_clockwork.displayoffload.V1_1.RoundRectShapeResource;
import vendor.google_clockwork.displayoffload.V1_1.StaticTextResource;
import vendor.google_clockwork.displayoffload.V1_1.StrftimeMapping;

public class FakeHalImplV1_1 extends IDisplayOffload.Stub implements
        HalAdapterTest.IFakeDisplayOffloadHal {
    static CustomResource mCustomResource = new CustomResource();
    static LinearMetricMapping mLinearMetricMapping = new LinearMetricMapping();
    static ModuloMapping mModuloMapping = new ModuloMapping();
    static OffloadConstant mOffloadConstant = new OffloadConstant();
    static OffloadExpression mOffloadExpression = new OffloadExpression();
    static OffloadMetric mOffloadMetric = new OffloadMetric();
    static OffloadString mOffloadString = new OffloadString();
    static PngResource mPngResource = new PngResource();
    static RangeMapping mRangeMapping = new RangeMapping();
    static RotationGroup mRotationGroup = new RotationGroup();
    static SpriteSheetPngResource mSpriteSheetPngResource = new SpriteSheetPngResource();
    static StringResource mStringResource = new StringResource();
    static TranslationGroup mTranslationGroup = new TranslationGroup();
    static ArcPathResource mArcPathResource = new ArcPathResource();
    static CeilMapping mCeilMapping = new CeilMapping();
    static FloorMapping mFloorMapping = new FloorMapping();
    static NumberFormatMapping mNumberFormatMapping = new NumberFormatMapping();
    static ReciprocalMapping mReciprocalMapping = new ReciprocalMapping();
    static RectShapeResource mRectShapeResource = new RectShapeResource();
    static RoundRectShapeResource mRoundRectShapeResource = new RoundRectShapeResource();
    static StaticTextResource mStaticTextResource = new StaticTextResource();
    static StaticTextAdapter mStaticTextAdapter = new StaticTextAdapter(mStaticTextResource);
    static DynamicTextResource mDynamicTextResource = new DynamicTextResource();
    static DynamicTextAdapter mDynamicTextAdapter = new DynamicTextAdapter(mDynamicTextResource);
    static TtfFontResource mTtfFontResource = new TtfFontResource();
    static TtfFontAdapter mTtfFontAdapter = new TtfFontAdapter(mTtfFontResource);
    IHwBinder mHwBinder;

    public List<Object> listOfHALObjects() {
        return ImmutableList.of(mCustomResource, mLinearMetricMapping, mModuloMapping,
                mOffloadConstant, mOffloadExpression, mOffloadMetric, mOffloadString, mPngResource,
                mRangeMapping, mRotationGroup, mSpriteSheetPngResource, mStringResource,
                mTranslationGroup, mArcPathResource, mCeilMapping, mFloorMapping,
                mNumberFormatMapping, mReciprocalMapping, mRectShapeResource,
                mRoundRectShapeResource, mStaticTextAdapter, mDynamicTextAdapter, mTtfFontAdapter);
    }

    public void verifyHalInteractions(Object o) throws RemoteException {
        assertThat(Mockito.mockingDetails(this).isMock()).isTrue();
        if (o instanceof CustomResource) {
            verify(this).sendCustomResource(eq((CustomResource) o));
        } else if (o instanceof TranslationGroup) {
            verify(this).sendTranslationGroup(eq((TranslationGroup) o));
        } else if (o instanceof RotationGroup) {
            verify(this).sendRotationGroup(eq((RotationGroup) o));
        } else if (o instanceof PngResource) {
            verify(this).sendPngResource(eq((PngResource) o));
        } else if (o instanceof SpriteSheetPngResource) {
            verify(this).sendSpriteSheetPngResource(eq((SpriteSheetPngResource) o));
        } else if (o instanceof StringResource) {
            verify(this).sendStringResource(eq((StringResource) o));
        } else if (o instanceof OffloadString) {
            verify(this).sendOffloadString(eq((OffloadString) o));
        } else if (o instanceof OffloadExpression) {
            verify(this).sendOffloadExpression(eq((OffloadExpression) o));
        } else if (o instanceof OffloadMetric) {
            verify(this).sendOffloadMetric(eq((OffloadMetric) o));
        } else if (o instanceof OffloadConstant) {
            verify(this).sendOffloadConstant(eq((OffloadConstant) o));
        } else if (o instanceof LinearMetricMapping) {
            verify(this).sendLinearMetricMapping(eq((LinearMetricMapping) o));
        } else if (o instanceof RangeMapping) {
            verify(this).sendRangeMapping(eq((RangeMapping) o));
        } else if (o instanceof ModuloMapping) {
            verify(this).sendModuloMapping(eq((ModuloMapping) o));
        } else if (o instanceof ArcPathResource) {
            verify(this).sendArcPathResource(eq((ArcPathResource) o));
        } else if (o instanceof RectShapeResource) {
            verify(this).sendRectShapeResource(eq((RectShapeResource) o));
        } else if (o instanceof RoundRectShapeResource) {
            verify(this).sendRoundRectShapeResource(eq((RoundRectShapeResource) o));
        } else if (o instanceof NumberFormatMapping) {
            verify(this).sendNumberFormatMapping(eq((NumberFormatMapping) o));
        } else if (o instanceof ReciprocalMapping) {
            verify(this).sendReciprocalMapping(eq((ReciprocalMapping) o));
        } else if (o instanceof CeilMapping) {
            verify(this).sendCeilMapping(eq((CeilMapping) o));
        } else if (o instanceof FloorMapping) {
            verify(this).sendFloorMapping(eq((FloorMapping) o));
        } else if (o instanceof StaticTextAdapter) {
            verify(this).sendStaticTextResource(mStaticTextResource);
        } else if (o instanceof DynamicTextAdapter) {
            verify(this).sendDynamicTextResource(mDynamicTextResource);
        } else if (o instanceof TtfFontAdapter) {
            verify(this).sendTtfFontResource(mTtfFontResource);
        } else {
            throw new UnsupportedOperationException(
                    "Not implemented for " + o.getClass().getName());
        }
    }

    @Override
    public int reset() throws RemoteException {
        return 0;
    }

    @Override
    public int resetResource() throws RemoteException {
        return 0;
    }

    @Override
    public int setRootResourceId(int rootId) throws RemoteException {
        return 0;
    }

    @Override
    public int setBrightnessConfiguration(boolean brightenOnTilt, ArrayList<Short> alsThresholds,
            ArrayList<Short> brightnessValues, ArrayList<Short> brightnessValuesBright)
            throws RemoteException {
        return 0;
    }

    @Override
    public int beginDisplay() throws RemoteException {
        return 0;
    }

    @Override
    public void endDisplay() throws RemoteException {

    }

    @Override
    public int sendCustomCommand(ArrayList<Byte> data) throws RemoteException {
        return 0;
    }

    @Override
    public int sendTranslationGroup(TranslationGroup group) throws RemoteException {
        return 0;
    }

    @Override
    public int sendRotationGroup(RotationGroup group) throws RemoteException {
        return 0;
    }

    @Override
    public int sendPngResource(PngResource png) throws RemoteException {
        return 0;
    }

    @Override
    public int sendOffloadExpression(OffloadExpression expr) throws RemoteException {
        return 0;
    }

    @Override
    public int sendOffloadConstant(OffloadConstant c) throws RemoteException {
        return 0;
    }

    @Override
    public int sendOffloadMetric(OffloadMetric metric) throws RemoteException {
        return 0;
    }

    @Override
    public int sendOffloadString(OffloadString offloadStr) throws RemoteException {
        return 0;
    }

    @Override
    public int sendLinearMetricMapping(LinearMetricMapping mapping) throws RemoteException {
        return 0;
    }

    @Override
    public int sendModuloMapping(ModuloMapping mapping) throws RemoteException {
        return 0;
    }

    @Override
    public int sendRangeMapping(RangeMapping mapping) throws RemoteException {
        return 0;
    }

    @Override
    public int sendTtfFontResource(TtfFontResource spriteSheet) throws RemoteException {
        return 0;
    }

    @Override
    public int sendSpriteSheetPngResource(SpriteSheetPngResource spriteSheet)
            throws RemoteException {
        return 0;
    }

    @Override
    public int sendStringResource(StringResource str) throws RemoteException {
        return 0;
    }

    @Override
    public int sendCustomResource(CustomResource custom) throws RemoteException {
        return 0;
    }

    @Override
    public void readFramebuffer(readFramebufferCallback _hidl_cb) throws RemoteException {

    }

    @Override
    public int sendRectShapeResource(RectShapeResource rect) throws RemoteException {
        return 0;
    }

    @Override
    public int sendRoundRectShapeResource(RoundRectShapeResource round_rect)
            throws RemoteException {
        return 0;
    }

    @Override
    public int sendArcPathResource(ArcPathResource arc) throws RemoteException {
        return 0;
    }

    @Override
    public int sendStrftimeMapping(StrftimeMapping mapping) throws RemoteException {
        return 0;
    }

    @Override
    public int sendStaticTextResource(StaticTextResource staticText) throws RemoteException {
        return 0;
    }

    @Override
    public int sendDynamicTextResource(DynamicTextResource dynamicText) throws RemoteException {
        return 0;
    }

    @Override
    public int sendNumberFormatMapping(NumberFormatMapping mapping) throws RemoteException {
        return 0;
    }

    @Override
    public int sendFloorMapping(FloorMapping mapping) throws RemoteException {
        return 0;
    }

    @Override
    public int sendCeilMapping(CeilMapping mapping) throws RemoteException {
        return 0;
    }

    @Override
    public int sendReciprocalMapping(ReciprocalMapping mapping) throws RemoteException {
        return 0;
    }

    @Override
    public int setLocaleConfig(LocaleConfig localeConfig) throws RemoteException {
        return 0;
    }

    @Override
    public int beginRendering() throws RemoteException {
        return 0;
    }

    @Override
    public void endRendering() throws RemoteException {

    }

    @Override
    public ArrayList<KeyValuePair> fetchDataSnapshot() throws RemoteException {
        return null;
    }

    @Override
    public IHwBinder asBinder() {
        return mHwBinder;
    }
}
