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

import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_LAYOUT_CONVERSION_FAILURE;
import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import vendor.google_clockwork.displayoffload.V1_1.StaticTextResource;
import vendor.google_clockwork.displayoffload.V1_1.TextParam;
import vendor.google_clockwork.displayoffload.V2_0.BindingPtr;
import vendor.google_clockwork.displayoffload.V2_0.FontParam;
import vendor.google_clockwork.displayoffload.V2_0.StaticText;

/**
 * Tests for {@link com.google.android.clockwork.displayoffload.StaticTextAdapter}.
 */
@RunWith(AndroidJUnit4.class)
public class StaticTextAdapterTest {
    private static final int TEST_ID = 666;
    private static final String TEST_STRING = "STATIC_TEXT_TEST_STING";
    private static final ArrayList<Integer> TEST_SHAPEDGLYPHINDECES = new ArrayList<>(
            Arrays.asList(1, 2, 3, 4, 5, 0));
    private static final ArrayList<Float> TEST_SHAPEDGLYPHPOSITIONS = new ArrayList<>(
            Arrays.asList(0.0F, 5.0F, 4.0F, 3.0F, 2.0F, 1.0F));
    private static final int TEST_FONT_ID = 99;
    private static final float TEST_FONT_SIZE = 10.0F;

    private final TextParam v1Param = new TextParam();
    private final FontParam v2Param = new FontParam();

    private StaticTextResource v1StaticText = new StaticTextResource();
    private StaticText v2StaticText = new StaticText();
    private StaticTextAdapter v1StaticTextAdapter = new StaticTextAdapter(v1StaticText);
    private StaticTextAdapter v2StaticTextAdapter = new StaticTextAdapter(v2StaticText);

    @Before
    public void setUp() {
        v1StaticText.textParam = v1Param;
        v2StaticText.fontParam = v2Param;
    }

    @Test
    public void testGet() {
        // V1
        try {
            v1StaticTextAdapter.getV1();
        } catch (DisplayOffloadException e) {
            fail("Shouldn't trow exception.");
        }

        // V2
        try {
            v2StaticTextAdapter.getV2();
        } catch (DisplayOffloadException e) {
            fail("Shouldn't trow exception.");
        }
    }

    @Test
    public void testGetId() {
        // V1
        v1StaticText.id = TEST_ID;
        assertThat(v1StaticTextAdapter.getId()).isEqualTo(TEST_ID);

        // V2
        v2StaticText.id = TEST_ID;
        assertThat(v2StaticTextAdapter.getId()).isEqualTo(TEST_ID);
    }

    @Test
    public void testGetException() {
        List<DisplayOffloadException> capturedExceptions = new ArrayList<>();
        // V1
        try {
            new StaticTextAdapter(v2StaticText).getV1();
        } catch (DisplayOffloadException e) {
            capturedExceptions.add(e);
        }
        assertThat(capturedExceptions.size()).isEqualTo(1);
        assertThat(capturedExceptions.get(0).getErrorType()).isEqualTo(
                ERROR_LAYOUT_CONVERSION_FAILURE);

        // V2
        try {
            new StaticTextAdapter(v1StaticText).getV2();
        } catch (DisplayOffloadException e) {
            capturedExceptions.add(e);
        }
        assertThat(capturedExceptions.size()).isEqualTo(2);
        assertThat(capturedExceptions.get(1).getErrorType()).isEqualTo(
                ERROR_LAYOUT_CONVERSION_FAILURE);
    }

    @Test
    public void testGetOriginalString() {
        // V1
        v1StaticText.value = TEST_STRING;
        assertThat(v1StaticTextAdapter.getOriginalString()).isEqualTo(TEST_STRING);

        // V2
        v2StaticText.originalString = TEST_STRING;
        assertThat(v2StaticTextAdapter.getOriginalString()).isEqualTo(TEST_STRING);
    }

    @Test
    public void testGetShapedGlyphIndices() {
        // V1
        v1StaticText.shapedGlyphIndices = TEST_SHAPEDGLYPHINDECES;
        assertThat(v1StaticTextAdapter.getShapedGlyphIndices()).isEqualTo(TEST_SHAPEDGLYPHINDECES);

        // V2
        v2StaticText.shapedGlyphIndices = TEST_SHAPEDGLYPHINDECES;
        assertThat(v2StaticTextAdapter.getShapedGlyphIndices()).isEqualTo(TEST_SHAPEDGLYPHINDECES);
    }

    @Test
    public void testSetShapedGlyphIndices() {
        ArrayList<Integer> listBeforeSet = new ArrayList<>(
                Arrays.asList(1, 2, 3, 4, 5, 6));

        // V1
        v1StaticText.shapedGlyphIndices = listBeforeSet;
        v1StaticTextAdapter.setShapedGlyphIndices(5, 0);
        assertThat(v1StaticTextAdapter.getShapedGlyphIndices()).isEqualTo(TEST_SHAPEDGLYPHINDECES);

        // V2
        v2StaticText.shapedGlyphIndices = listBeforeSet;
        v2StaticTextAdapter.setShapedGlyphIndices(5, 0);
        assertThat(v2StaticTextAdapter.getShapedGlyphIndices()).isEqualTo(TEST_SHAPEDGLYPHINDECES);
    }

    @Test
    public void testGetShapedGlyphPositions() {
        // V1
        v1StaticText.shapedGlyphPositions = TEST_SHAPEDGLYPHPOSITIONS;
        assertThat(v1StaticTextAdapter.getShapedGlyphPositions()).isEqualTo(
                TEST_SHAPEDGLYPHPOSITIONS);

        // V2
        v2StaticText.shapedGlyphPositions = TEST_SHAPEDGLYPHPOSITIONS;
        assertThat(v2StaticTextAdapter.getShapedGlyphPositions()).isEqualTo(
                TEST_SHAPEDGLYPHPOSITIONS);
    }


    @Test
    public void testGetFontId() {
        // V1
        v1StaticText.textParam.ttfFont = TEST_FONT_ID;
        assertThat(v1StaticTextAdapter.getFontId()).isEqualTo(TEST_FONT_ID);

        // V2
        v2StaticText.fontParam.ttfFont = TEST_FONT_ID;
        assertThat(v2StaticTextAdapter.getFontId()).isEqualTo(TEST_FONT_ID);
    }

    @Test
    public void testGetFontSize() {
        // V1
        v1StaticText.textParam.ttfFontSize = TEST_FONT_SIZE;
        assertThat(v1StaticTextAdapter.getFontSize()).isEqualTo(TEST_FONT_SIZE);

        // V2
        v2StaticText.fontParam.ttfFontSize = TEST_FONT_SIZE;
        assertThat(v2StaticTextAdapter.getFontSize()).isEqualTo(TEST_FONT_SIZE);
    }

    @Test
    public void testGetVisibility() {
        BindingPtr visibilityPtr = new BindingPtr();

        // V2
        v2StaticText.visible = visibilityPtr;
        assertThat(v2StaticTextAdapter.getVisibility()).isEqualTo(visibilityPtr);
    }

    @Test
    public void testColor() {
        BindingPtr colorPtr = new BindingPtr();

        // V2
        v2StaticText.color = colorPtr;
        assertThat(v2StaticTextAdapter.getColor()).isEqualTo(colorPtr);
    }

    @Test
    public void testAllNullCases() {
        assertThat(new StaticTextAdapter((StaticTextResource) null).getId()).isEqualTo(-1);
        assertThat(new StaticTextAdapter((StaticText) null).getId()).isEqualTo(-1);

        assertThat(new StaticTextAdapter((StaticTextResource) null).getOriginalString()).isNull();
        assertThat(new StaticTextAdapter((StaticText) null).getOriginalString()).isNull();

        assertThat(
                new StaticTextAdapter((StaticTextResource) null).getShapedGlyphIndices()).isNull();
        assertThat(new StaticTextAdapter((StaticText) null).getShapedGlyphIndices()).isNull();

        v1StaticTextAdapter = spy(new StaticTextAdapter((StaticTextResource) null));
        v1StaticTextAdapter.setShapedGlyphIndices(5, 0);
        verify(v1StaticTextAdapter, never()).getShapedGlyphIndices();
        v2StaticTextAdapter = spy(new StaticTextAdapter((StaticText) null));
        v2StaticTextAdapter.setShapedGlyphIndices(5, 0);
        verify(v2StaticTextAdapter, never()).getShapedGlyphIndices();

        assertThat(new StaticTextAdapter(
                (StaticTextResource) null).getShapedGlyphPositions()).isNull();
        assertThat(new StaticTextAdapter((StaticText) null).getShapedGlyphPositions()).isNull();

        assertThat(new StaticTextAdapter((StaticTextResource) null).getFontId()).isEqualTo(-1);
        assertThat(new StaticTextAdapter((StaticText) null).getFontId()).isEqualTo(-1);

        assertThat(new StaticTextAdapter((StaticTextResource) null).getFontSize()).isEqualTo(-1.0F);
        assertThat(new StaticTextAdapter((StaticText) null).getFontSize()).isEqualTo(-1.0F);

        assertThat(new StaticTextAdapter((StaticTextResource) null).getVisibility()).isNull();
        assertThat(new StaticTextAdapter((StaticText) null).getVisibility()).isNull();

        assertThat(new StaticTextAdapter((StaticTextResource) null).getColor()).isNull();
        assertThat(new StaticTextAdapter((StaticText) null).getColor()).isNull();
    }
}
