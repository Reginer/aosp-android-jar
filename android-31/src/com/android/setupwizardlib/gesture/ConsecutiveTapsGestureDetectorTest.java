/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.setupwizardlib.gesture;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.robolectric.RuntimeEnvironment.application;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(sdk = {Config.OLDEST_SDK, Config.NEWEST_SDK})
@RunWith(RobolectricTestRunner.class)
public class ConsecutiveTapsGestureDetectorTest {

  @Mock private ConsecutiveTapsGestureDetector.OnConsecutiveTapsListener listener;

  private ConsecutiveTapsGestureDetector detector;
  private int slop;
  private int tapTimeout;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    View view = new View(application);
    view.measure(500, 500);
    view.layout(0, 0, 500, 500);
    detector = new ConsecutiveTapsGestureDetector(listener, view);

    slop = ViewConfiguration.get(application).getScaledDoubleTapSlop();
    tapTimeout = ViewConfiguration.getDoubleTapTimeout();
  }

  @Test
  public void onTouchEvent_shouldTriggerCallbackOnFourTaps() {
    InOrder inOrder = inOrder(listener);

    tap(0, 25f, 25f);
    inOrder.verify(listener).onConsecutiveTaps(eq(1));

    tap(100, 25f, 25f);
    inOrder.verify(listener).onConsecutiveTaps(eq(2));

    tap(200, 25f, 25f);
    inOrder.verify(listener).onConsecutiveTaps(eq(3));

    tap(300, 25f, 25f);
    inOrder.verify(listener).onConsecutiveTaps(eq(4));
  }

  @Test
  public void onTouchEvent_tapOnDifferentLocation_shouldResetCounter() {
    InOrder inOrder = inOrder(listener);

    tap(0, 25f, 25f);
    inOrder.verify(listener).onConsecutiveTaps(eq(1));

    tap(100, 25f, 25f);
    inOrder.verify(listener).onConsecutiveTaps(eq(2));

    tap(200, 25f + slop * 2, 25f);
    inOrder.verify(listener).onConsecutiveTaps(eq(1));

    tap(300, 25f + slop * 2, 25f);
    inOrder.verify(listener).onConsecutiveTaps(eq(2));
  }

  @Test
  public void onTouchEvent_tapAfterTimeout_shouldResetCounter() {
    InOrder inOrder = inOrder(listener);

    tap(0, 25f, 25f);
    inOrder.verify(listener).onConsecutiveTaps(eq(1));

    tap(100, 25f, 25f);
    inOrder.verify(listener).onConsecutiveTaps(eq(2));

    tap(200 + tapTimeout, 25f, 25f);
    inOrder.verify(listener).onConsecutiveTaps(eq(1));

    tap(300 + tapTimeout, 25f, 25f);
    inOrder.verify(listener).onConsecutiveTaps(eq(2));
  }

  private void tap(int timeMillis, float x, float y) {
    detector.onTouchEvent(
        MotionEvent.obtain(timeMillis, timeMillis, MotionEvent.ACTION_DOWN, x, y, 0));
    detector.onTouchEvent(
        MotionEvent.obtain(timeMillis, timeMillis + 10, MotionEvent.ACTION_UP, x, y, 0));
  }
}
