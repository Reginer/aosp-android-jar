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

import static org.mockito.Mockito.never;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Handler;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class ImageMaskViewControllerTest {

    @Mock
    private ImageMaskView mockImageMaskView;
    @Mock
    private ImageMaskTileFactory mockImageMaskFactory;

    private ImageMaskViewController mImageMaskViewController;
    @Before
    public void setup() {
        initMocks(this);
        Context context = ApplicationProvider.getApplicationContext();

        mImageMaskViewController =
                new ImageMaskViewController(context, (c, b, i) -> mockImageMaskView,
                        mockImageMaskFactory);
    }

    @Test
    public void enableImageMaskTest() {
        mImageMaskViewController.enableImageMask();

        Assert.assertTrue(mImageMaskViewController.isEnabled());
        Mockito.verify(mockImageMaskView).show();
        Mockito.verify(mockImageMaskView, never()).hide();
    }

    @Test
    public void disableImageMaskTest() {
        mImageMaskViewController.enableImageMask();

        Assert.assertTrue(mImageMaskViewController.isEnabled());
        Mockito.verify(mockImageMaskView).show();
        Mockito.verify(mockImageMaskView, never()).hide();

        Mockito.clearInvocations(mockImageMaskView);

        mImageMaskViewController.disableImageMask();

        Assert.assertFalse(mImageMaskViewController.isEnabled());
        Mockito.verify(mockImageMaskView).hide();
        Mockito.verify(mockImageMaskView, never()).show();
    }

    @Test
    public void updateImageMaskTest() {
        mImageMaskViewController.enableImageMask();
        Assert.assertTrue(mImageMaskViewController.isEnabled());

        mImageMaskViewController.updateImageMask();
        Mockito.verify(mockImageMaskView)
                .updateMaskType(Mockito.anyInt(), Mockito.any());
    }

    @Test
    public void getNextImageMaskTypeTest() {
        int[] testArray = {0, 0, 0, 0};

        Assert.assertEquals(1, mImageMaskViewController.getNextImageMaskType(testArray, 0));
        Assert.assertEquals(2, mImageMaskViewController.getNextImageMaskType(testArray, 1));
        Assert.assertEquals(3, mImageMaskViewController.getNextImageMaskType(testArray, 2));
        Assert.assertEquals(0, mImageMaskViewController.getNextImageMaskType(testArray, 3));
    }
}
