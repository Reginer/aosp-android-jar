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

import static com.android.clockwork.displayoffload.Utils.convertToIntArray;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.SharedMemory;

import androidx.test.core.app.ApplicationProvider;

import com.android.clockwork.displayoffload.HalTypeConverter.HalTypeConverterSupplier;
import com.android.clockwork.displayoffload.TextPreprocessor.ITextPreprocessorNative;
import com.google.android.clockwork.ambient.offload.types.DynamicTextResource;
import com.google.android.clockwork.ambient.offload.types.StaticTextResource;
import com.google.android.clockwork.ambient.offload.types.TextParam;
import com.google.android.clockwork.ambient.offload.types.TtfFontResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class TextPreprocessorTest {

    // fake font information
    private static final int FONT_ID = 1;
    private static final float FONT_SIZE = 12.0f;
    private static final int FONT_INDEX = 2;
    private static final ArrayList<Integer> GLYPH_INDICES = new ArrayList<>();
    private static final ArrayList<Float> GLYPH_POSITIONS = new ArrayList<>();
    private static final String STATIC_TXT_ORIGINAL_STR = "static txt original str";

    @Mock
    ITextPreprocessorNative mITextPreprocessorNativeMock;
    @Mock
    HalTypeConverter mHalTypeConverterMock;
    @Mock
    HalTypeConverterSupplier mHalTypeConverterSupplierMock;
    @Mock
    HalTypeConverter mHalTypeConverter;

    @Mock
    SystemFontHelper mSystemFontHelperMock;
    @Mock
    StaticTextAdapter mStaticTextAdapterMock;
    @Mock
    DynamicTextAdapter mDynamicTextAdapterMock;

    @Mock
    TtfFontAdapter mTtfFontAdapterMock;
    @Mock
    SharedMemory mSharedMemoryMock;
    @Mock
    TtfFontResource mSystemFontResourceMock;
    @Mock
    TtfFontResource mAppBundledFontResourceMock;

    private Context mContext = ApplicationProvider.getApplicationContext();
    private HalResourceStore mHalResourceStoreSpy;
    private vendor.google_clockwork.displayoffload.V1_1.DynamicTextResource mHalDynamicText;
    private ByteBuffer mFakeFontByteBuffer;
    private StaticTextResource mStaticTextResource;
    private DynamicTextResource mDynamicTextResource;
    private List<ResourceObject> mHalStaticTextResources;
    private List<ResourceObject> mHalDynamicTextResources;
    private List<ResourceObject> mHalTtfFontResources;
    private TextPreprocessor mTextPreprocessor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // NOTE: both mSystemFontResourceMock and mAppBundledFontResourceMock share the same font
        // id. As a result, only one of them should be used in a test. They are not meant to be
        // used together.
        mSystemFontResourceMock.id = FONT_ID;
        mSystemFontResourceMock.fontPath = "/path/to/system_font"; // fake system font
        mSystemFontResourceMock.fontIndex = FONT_INDEX;

        mAppBundledFontResourceMock.id = FONT_ID;
        mAppBundledFontResourceMock.ttfMemory = mSharedMemoryMock;

        mHalResourceStoreSpy = spy(new HalResourceStore(mHalTypeConverterSupplierMock));
        mHalDynamicText = new vendor.google_clockwork.displayoffload.V1_1.DynamicTextResource();
        // allocate an 8-byte fake font byte buffer
        mFakeFontByteBuffer = ByteBuffer.allocate(8);
        // fill the first byte with a cookie value so that it is different from a 0 buffer
        mFakeFontByteBuffer.put((byte) 64);

        mStaticTextResource = new StaticTextResource();
        mStaticTextResource.id = 0xCAFEF00D;
        mStaticTextResource.textParam = new TextParam();
        mStaticTextResource.textParam.ttfFont = FONT_ID; // the text uses our fake font
        mStaticTextResource.textParam.useTabularNum = true;

        mDynamicTextResource = new DynamicTextResource();
        mDynamicTextResource.id = 0x1CEDCAFE;
        mDynamicTextResource.textParam = new TextParam();
        mDynamicTextResource.textParam.ttfFont = FONT_ID; // the text uses our fake font
        mDynamicTextResource.textParam.useTabularNum = true;

        mHalStaticTextResources = List.of(
                ResourceObject.of(mStaticTextResource.id, mStaticTextAdapterMock));
        mHalDynamicTextResources = List.of(
                ResourceObject.of(mDynamicTextResource.id, mDynamicTextAdapterMock));
        mHalTtfFontResources = List.of(
                ResourceObject.of(FONT_ID, mTtfFontAdapterMock));

        // add some fake glyph indices
        for (int i = 64; i < 90; ++i) {
            GLYPH_INDICES.add(i);
        }

        // add some fake glyph position information
        for (float i = 128.0f; i < 132.0f; ++i) {
            GLYPH_POSITIONS.add(i);
        }

        when(mHalTypeConverterSupplierMock.getConverter()).thenReturn(mHalTypeConverterMock);
        when(mHalTypeConverterMock.toHalStaticText(any(StaticTextResource.class))).thenReturn(
                mHalStaticTextResources);
        when(mHalTypeConverterMock.toHalObject(any(DynamicTextResource.class))).thenReturn(
                mHalDynamicTextResources);
        when(mHalTypeConverterMock.toHalTtfFontResource(any(TtfFontResource.class)))
                .thenReturn(mHalTtfFontResources);

        when(mSystemFontHelperMock.getFontIndexForResourceId(eq(FONT_ID))).thenReturn(FONT_INDEX);
        when(mSystemFontHelperMock.mapFont(anyString())).thenReturn(mFakeFontByteBuffer);

        when(mStaticTextAdapterMock.getFontId()).thenReturn(FONT_ID);
        when(mStaticTextAdapterMock.getOriginalString()).thenReturn(STATIC_TXT_ORIGINAL_STR);
        when(mStaticTextAdapterMock.getFontSize()).thenReturn(FONT_SIZE);
        when(mStaticTextAdapterMock.getShapedGlyphIndices()).thenReturn(GLYPH_INDICES);
        when(mStaticTextAdapterMock.getShapedGlyphPositions()).thenReturn(GLYPH_POSITIONS);

        when(mDynamicTextAdapterMock.getFontId()).thenReturn(FONT_ID);

        when(mSharedMemoryMock.mapReadOnly()).thenReturn(mFakeFontByteBuffer);

        when(mTtfFontAdapterMock.getId()).thenReturn(FONT_ID);

        mTextPreprocessor =
                new TextPreprocessor(
                        mContext,
                        mHalTypeConverterSupplierMock,
                        mITextPreprocessorNativeMock,
                        mSystemFontHelperMock);

    }

    @After
    public void tearDown() {
        GLYPH_INDICES.clear();
        GLYPH_POSITIONS.clear();
    }

    @Test
    public void testAddFont_addSystemFont() throws DisplayOffloadException {
        mTextPreprocessor.addFont(mHalResourceStoreSpy, mSystemFontResourceMock);

        // verify font (adapter) is added to hal resource store
        assertEquals(mTtfFontAdapterMock, mHalResourceStoreSpy.get(FONT_ID));
    }

    @Test
    public void testAddFont_addAppBundledFont() throws DisplayOffloadException {
        mTextPreprocessor.addFont(mHalResourceStoreSpy, mAppBundledFontResourceMock);

        // verify font (adapter) is added to hal resource store
        assertEquals(mTtfFontAdapterMock, mHalResourceStoreSpy.get(FONT_ID));
    }

    @Test
    public void testAddFont_badFontResource() {
        // no font path nor is there a font in shared memory
        TtfFontResource ttfFontResourceMock = Mockito.mock(TtfFontResource.class);

        try {
            mTextPreprocessor.addFont(mHalResourceStoreSpy, ttfFontResourceMock);
            fail();
        } catch (DisplayOffloadException expected) {
        }
    }

    @Test
    public void testAddStaticText_systemFont() throws DisplayOffloadException {
        // add fake system font
        mTextPreprocessor.addFont(mHalResourceStoreSpy, mSystemFontResourceMock);

        mTextPreprocessor.addStaticText(mHalResourceStoreSpy, mStaticTextResource);

        // verify that the hal static text adapter for this text resource is registered in
        // the hal resource store
        assertEquals(mStaticTextAdapterMock, mHalResourceStoreSpy.get(mStaticTextResource.id));

        // verify call to TextPreprocessor native API to shape text
        verify(mITextPreprocessorNativeMock).shapeText(eq(STATIC_TXT_ORIGINAL_STR), eq(FONT_SIZE),
                eq(FONT_ID),
                eq(FONT_INDEX), eq(mFakeFontByteBuffer), eq(GLYPH_INDICES), eq(GLYPH_POSITIONS),
                eq(mStaticTextResource.textParam.useTabularNum));
    }

    @Test
    public void testAddStaticText_appBundledFont() throws Exception {
        // add fake app bundled font
        mTextPreprocessor.addFont(mHalResourceStoreSpy, mAppBundledFontResourceMock);

        mTextPreprocessor.addStaticText(mHalResourceStoreSpy, mStaticTextResource);

        // verify that the hal static text adapter for this text resource is registered in
        // the hal resource store
        assertEquals(mStaticTextAdapterMock, mHalResourceStoreSpy.get(mStaticTextResource.id));

        // verify call to TextPreprocessor native API to shape text
        verify(mITextPreprocessorNativeMock).shapeText(eq(STATIC_TXT_ORIGINAL_STR), eq(FONT_SIZE),
                eq(FONT_ID),
                eq(FONT_INDEX), eq(mFakeFontByteBuffer), eq(GLYPH_INDICES), eq(GLYPH_POSITIONS),
                eq(mStaticTextResource.textParam.useTabularNum));
    }

    @Test
    public void testAddDynamicText_systemFont() throws DisplayOffloadException {
        // add fake system font
        mTextPreprocessor.addFont(mHalResourceStoreSpy, mSystemFontResourceMock);

        mTextPreprocessor.addDynamicText(mHalResourceStoreSpy, mDynamicTextResource);

        // verify that the hal dynamic text for this text resource is registered in
        // the hal resource store
        assertEquals(mDynamicTextAdapterMock, mHalResourceStoreSpy.get(mDynamicTextResource.id));
    }

    @Test
    public void testAddDynamicText_appBundledFont() throws Exception {
        // add fake app bundled font
        mTextPreprocessor.addFont(mHalResourceStoreSpy, mAppBundledFontResourceMock);

        mTextPreprocessor.addDynamicText(mHalResourceStoreSpy, mDynamicTextResource);

        // verify that the hal dynamic text for this text resource is registered in
        // the hal resource store
        assertEquals(mDynamicTextAdapterMock, mHalResourceStoreSpy.get(mDynamicTextResource.id));
    }

    @Test
    public void testProcessTtfFontSubsetting() throws Exception {
        byte[] fakeSubset = new byte[10];
        when(mITextPreprocessorNativeMock.subsetTtf(any(ByteBuffer.class), anyInt(),
                any(int[].class),
                any(int[].class))).thenReturn(fakeSubset);

        // add the font and some static text to the resource store
        mTextPreprocessor.addFont(mHalResourceStoreSpy, mSystemFontResourceMock);
        mTextPreprocessor.addStaticText(mHalResourceStoreSpy, mStaticTextResource);

        // do font subsetting
        mTextPreprocessor.processTtfFontSubsetting(mHalResourceStoreSpy);

        // verify that the native interface is called with the correct arguments
        verify(mITextPreprocessorNativeMock).subsetTtf(eq(mFakeFontByteBuffer), eq(FONT_INDEX),
                eq(convertToIntArray(GLYPH_INDICES)), any(int[].class));

    }
}
