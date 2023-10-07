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

package com.android.setupwizardlib.template;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import android.view.View;
import com.android.setupwizardlib.view.BottomScrollView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(sdk = {Config.OLDEST_SDK, Config.NEWEST_SDK})
@RunWith(RobolectricTestRunner.class)
public class ScrollViewScrollHandlingDelegateTest {

  @Mock private RequireScrollMixin requireScrollMixin;

  private BottomScrollView scrollView;
  private ScrollViewScrollHandlingDelegate delegate;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    scrollView = new BottomScrollView(application);
    View childView = new View(application);
    scrollView.addView(childView);
    delegate = new ScrollViewScrollHandlingDelegate(requireScrollMixin, scrollView);

    scrollView.layout(0, 0, 500, 500);
    childView.layout(0, 0, 1000, 1000);
  }

  @Test
  public void testRequireScroll() throws Throwable {
    delegate.startListening();

    scrollView.getBottomScrollListener().onRequiresScroll();
    verify(requireScrollMixin).notifyScrollabilityChange(true);
  }

  @Test
  public void testScrolledToBottom() throws Throwable {
    delegate.startListening();

    scrollView.getBottomScrollListener().onRequiresScroll();
    verify(requireScrollMixin).notifyScrollabilityChange(true);

    scrollView.getBottomScrollListener().onScrolledToBottom();

    verify(requireScrollMixin).notifyScrollabilityChange(false);
  }

  @Test
  public void testPageScrollDown() throws Throwable {
    delegate.pageScrollDown();
    assertThat(scrollView.getScrollY()).isEqualTo(500);
  }
}
