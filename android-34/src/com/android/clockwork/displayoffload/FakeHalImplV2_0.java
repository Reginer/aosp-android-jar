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

import vendor.google_clockwork.displayoffload.V1_0.KeyValuePair;
import vendor.google_clockwork.displayoffload.V1_1.LocaleConfig;
import vendor.google_clockwork.displayoffload.V2_0.ArcPath;
import vendor.google_clockwork.displayoffload.V2_0.BinaryOperation;
import vendor.google_clockwork.displayoffload.V2_0.BitmapDrawable;
import vendor.google_clockwork.displayoffload.V2_0.CustomResource;
import vendor.google_clockwork.displayoffload.V2_0.DynamicText;
import vendor.google_clockwork.displayoffload.V2_0.GlyphSpritesResource;
import vendor.google_clockwork.displayoffload.V2_0.IDisplayOffload;
import vendor.google_clockwork.displayoffload.V2_0.LinePath;
import vendor.google_clockwork.displayoffload.V2_0.NumberFormatResource;
import vendor.google_clockwork.displayoffload.V2_0.OffloadMetric;
import vendor.google_clockwork.displayoffload.V2_0.Primitive;
import vendor.google_clockwork.displayoffload.V2_0.RectShape;
import vendor.google_clockwork.displayoffload.V2_0.RotationGroup;
import vendor.google_clockwork.displayoffload.V2_0.RoundRectShape;
import vendor.google_clockwork.displayoffload.V2_0.SpriteStringDrawable;
import vendor.google_clockwork.displayoffload.V2_0.StaticText;
import vendor.google_clockwork.displayoffload.V2_0.TernaryOperation;
import vendor.google_clockwork.displayoffload.V2_0.TranslationGroup;
import vendor.google_clockwork.displayoffload.V2_0.TtfFontResource;
import vendor.google_clockwork.displayoffload.V2_0.UnaryOperation;

public class FakeHalImplV2_0 extends IDisplayOffload.Stub implements
        HalAdapterTest.IFakeDisplayOffloadHal {

    IHwBinder mHwBinder;
    static ArcPath sArcPath = new ArcPath();
    static BinaryOperation sBinaryOperation = new BinaryOperation();
    static CustomResource sCustomResource = new CustomResource();
    static DynamicText sDynamicText = new DynamicText();
    static DynamicTextAdapter sDynamicTextAdapter = new DynamicTextAdapter(sDynamicText);
    static LinePath sLinePath = new LinePath();
    static NumberFormatResource sNumberFormatResource = new NumberFormatResource();
    static OffloadMetric sOffloadMetric = new OffloadMetric();
    static BitmapDrawable sBitmapDrawable = new BitmapDrawable();
    static Primitive sPrimitive = new Primitive();
    static RectShape sRectShape = new RectShape();
    static RotationGroup sRotationGroup = new RotationGroup();
    static RoundRectShape sRoundRectShape = new RoundRectShape();
    static StaticText sStaticText = new StaticText();
    static StaticTextAdapter sStaticTextAdapter = new StaticTextAdapter(sStaticText);
    static TernaryOperation sTernaryOperation = new TernaryOperation();
    static TranslationGroup sTranslationGroup = new TranslationGroup();
    static TtfFontResource sTtfFontResource = new TtfFontResource();
    static TtfFontAdapter sTtfFontAdapter = new TtfFontAdapter(sTtfFontResource);
    static UnaryOperation sUnaryOperation = new UnaryOperation();

    public List<Object> listOfHALObjects() {
        return ImmutableList.of(
                sArcPath,
                sBinaryOperation,
                sBitmapDrawable,
                sCustomResource,
                sDynamicTextAdapter,
                sLinePath,
                sNumberFormatResource,
                sOffloadMetric,
                sPrimitive,
                sRectShape,
                sRotationGroup,
                sRoundRectShape,
                sStaticTextAdapter,
                sTernaryOperation,
                sTranslationGroup,
                sTtfFontAdapter,
                sUnaryOperation);
    }

    public void verifyHalInteractions(Object o) throws RemoteException, DisplayOffloadException {
        assertThat(Mockito.mockingDetails(this).isMock()).isTrue();
        if (o instanceof LocaleConfig) {
            verify(this).setLocaleConfig(eq((LocaleConfig) o));
        } else if (o instanceof ArcPath) {
            verify(this).addArcPath(eq((ArcPath) o));
        } else if (o instanceof BinaryOperation) {
            verify(this).addBinaryOperation(eq((BinaryOperation) o));
        } else if (o instanceof CustomResource) {
            verify(this).addCustomResource(eq((CustomResource) o));
        } else if (o instanceof DynamicTextAdapter) {
            verify(this).addDynamicText(eq(((DynamicTextAdapter) o).getV2()));
        } else if (o instanceof LinePath) {
            verify(this).addLinePath(eq((LinePath) o));
        } else if (o instanceof NumberFormatResource) {
            verify(this).addNumberFormatResource(eq((NumberFormatResource) o));
        } else if (o instanceof OffloadMetric) {
            verify(this).addOffloadMetric(eq((OffloadMetric) o));
        } else if (o instanceof BitmapDrawable) {
            verify(this).addBitmapDrawable(eq((BitmapDrawable) o));
        } else if (o instanceof Primitive) {
            verify(this).addPrimitive(eq((Primitive) o));
        } else if (o instanceof RectShape) {
            verify(this).addRectShape(eq((RectShape) o));
        } else if (o instanceof RotationGroup) {
            verify(this).addRotationGroup(eq((RotationGroup) o));
        } else if (o instanceof RoundRectShape) {
            verify(this).addRoundRectShape(eq((RoundRectShape) o));
        } else if (o instanceof StaticTextAdapter) {
            verify(this).addStaticText(eq(((StaticTextAdapter) o).getV2()));
        } else if (o instanceof TernaryOperation) {
            verify(this).addTernaryOperation(eq((TernaryOperation) o));
        } else if (o instanceof TranslationGroup) {
            verify(this).addTranslationGroup(eq((TranslationGroup) o));
        } else if (o instanceof TtfFontAdapter) {
            verify(this).addTtfFontResource(eq(((TtfFontAdapter) o).getTtfFontResourceV2()));
        } else if (o instanceof UnaryOperation) {
            verify(this).addUnaryOperation(eq((UnaryOperation) o));
        } else {
            /* TODO(b/265843014): Connect bitmap fonts. */
            throw new UnsupportedOperationException(
                    "Not implemented for " + o.getClass().getName());
        }
    }

    @Override
    public int reset() throws RemoteException {
        return 0;
    }

    @Override
    public int setRootResourceId(int rootId) throws RemoteException {
        return 0;
    }

    @Override
    public int beginDisplayControl() throws RemoteException {
        return 0;
    }

    @Override
    public void endDisplayControl() throws RemoteException {

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
    public int setBrightnessConfiguration(boolean brightenOnTilt, ArrayList<Short> alsThresholdsUp,
            ArrayList<Short> brightnessValues, ArrayList<Short> brightnessValuesBright)
            throws RemoteException {
        return 0;
    }

    @Override
    public int addTranslationGroup(TranslationGroup group) throws RemoteException {
        return 0;
    }

    @Override
    public int addRotationGroup(RotationGroup group) throws RemoteException {
        return 0;
    }

    @Override
    public int addBitmapDrawable(BitmapDrawable bmp) throws RemoteException {
        return 0;
    }

    @Override
    public int addSpriteStringDrawable(SpriteStringDrawable spriteStringDrawable)
            throws RemoteException {
        return 0;
    }

    @Override
    public int addOffloadMetric(OffloadMetric metric) throws RemoteException {
        return 0;
    }

    @Override
    public int addPrimitive(Primitive val) throws RemoteException {
        return 0;
    }

    @Override
    public int addUnaryOperation(UnaryOperation op) throws RemoteException {
        return 0;
    }

    @Override
    public int addBinaryOperation(BinaryOperation op) throws RemoteException {
        return 0;
    }

    @Override
    public int addTernaryOperation(TernaryOperation op) throws RemoteException {
        return 0;
    }

    @Override
    public int addStaticText(StaticText staticText) throws RemoteException {
        return 0;
    }

    @Override
    public int addDynamicText(DynamicText dynamicText) throws RemoteException {
        return 0;
    }

    @Override
    public int addArcPath(ArcPath arcPath) throws RemoteException {
        return 0;
    }

    @Override
    public int addLinePath(LinePath linePath) throws RemoteException {
        return 0;
    }

    @Override
    public int addRectShape(RectShape rectShape) throws RemoteException {
        return 0;
    }

    @Override
    public int addRoundRectShape(RoundRectShape roundRectShape) throws RemoteException {
        return 0;
    }

    @Override
    public int addTtfFontResource(TtfFontResource ttf) throws RemoteException {
        return 0;
    }

    @Override
    public int addGlyphSpritesResource(GlyphSpritesResource glyphSpritesResource)
            throws RemoteException {
        return 0;
    }

    @Override
    public int addNumberFormatResource(NumberFormatResource format) throws RemoteException {
        return 0;
    }

    @Override
    public int addCustomResource(CustomResource custom) throws RemoteException {
        return 0;
    }

    @Override
    public int setLocaleConfig(LocaleConfig localeConfig) throws RemoteException {
        return 0;
    }

    @Override
    public void readFramebuffer(readFramebufferCallback _hidl_cb) throws RemoteException {

    }

    @Override
    public void evaluateBinding(int id, evaluateBindingCallback _hidl_cb) throws RemoteException {

    }

    @Override
    public int sendCustomCommand(ArrayList<Byte> data) throws RemoteException {
        return 0;
    }

    @Override
    public IHwBinder asBinder() {
        return mHwBinder;
    }
}
