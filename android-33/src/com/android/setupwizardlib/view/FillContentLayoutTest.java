/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.setupwizardlib.view;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.RuntimeEnvironment.application;

import android.view.View;
import android.view.View.MeasureSpec;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK, Config.NEWEST_SDK})
public class FillContentLayoutTest {

  @Test
  public void testMeasureMinSize() {
    FillContentLayout layout =
        new FillContentLayout(
            application,
            Robolectric.buildAttributeSet()
                .addAttribute(android.R.attr.minWidth, "123dp")
                .addAttribute(android.R.attr.minHeight, "123dp")
                .build());
    layout.measure(
        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

    assertThat(layout.getMeasuredWidth()).isEqualTo(123);
    assertThat(layout.getMeasuredHeight()).isEqualTo(123);
  }

  @Test
  public void testMeasureChildIsSmallerThanMaxSize() {
    View child = new View(application);
    FillContentLayout layout =
        new FillContentLayout(
            application,
            Robolectric.buildAttributeSet()
                .addAttribute(android.R.attr.maxWidth, "123dp")
                .addAttribute(android.R.attr.maxHeight, "123dp")
                .build());
    layout.addView(child);
    layout.measure(
        MeasureSpec.makeMeasureSpec(300, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(300, MeasureSpec.EXACTLY));

    assertThat(child.getMeasuredWidth()).isEqualTo(123);
    assertThat(child.getMeasuredHeight()).isEqualTo(123);
  }

  @Test
  public void testMeasureChildIsSmallerThanParent() {
    View child = new View(application);
    FillContentLayout layout =
        new FillContentLayout(
            application,
            Robolectric.buildAttributeSet()
                .addAttribute(android.R.attr.maxWidth, "123dp")
                .addAttribute(android.R.attr.maxHeight, "123dp")
                .build());
    layout.addView(child);
    layout.measure(
        MeasureSpec.makeMeasureSpec(88, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(88, MeasureSpec.EXACTLY));

    assertThat(child.getMeasuredWidth()).isEqualTo(88);
    assertThat(child.getMeasuredHeight()).isEqualTo(88);
  }
}
