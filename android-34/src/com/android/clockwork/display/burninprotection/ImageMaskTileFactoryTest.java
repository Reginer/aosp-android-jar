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

package com.android.clockwork.display.burninprotection;

import android.graphics.Color;
import android.graphics.Paint;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ImageMaskTileFactoryTest {

    private ImageMaskTileFactory mImageMaskFactory;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mImageMaskFactory = new ImageMaskTileFactory(2);
    }

    @Test
    public void createPaintTest() {
        final int color = Color.BLACK;
        Paint patternPaint = mImageMaskFactory.createTilePaint(color);
        Assert.assertEquals(color, patternPaint.getColor());
        Assert.assertEquals(Paint.Style.FILL, patternPaint.getStyle());
        Assert.assertEquals(0, patternPaint.getFlags() & Paint.ANTI_ALIAS_FLAG);
        Assert.assertEquals(0, patternPaint.getStrokeWidth(), 0);
    }
}
