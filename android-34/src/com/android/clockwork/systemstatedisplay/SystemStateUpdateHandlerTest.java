package com.android.clockwork.systemstatedisplay;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.Context;
import android.view.View;
import android.view.WindowManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public final class SystemStateUpdateHandlerTest {
  private static final int TEST_UPDATE_INTERVAL_MILLIS = 10;
  private static final List<SystemState> TEST_STATES =
      Arrays.asList(new SystemState("test_title", "test_value"));

  @Mock Context mockContext;
  @Mock WindowManager mockWindowManager;
  @Mock SystemStateProviders mockStateProviders;
  @Mock SystemStateViewHolder mockStateView;
  @Mock View mockView;

  private SystemStateUpdateHandler mSystemStateUpdateHandler;

  @Before
  public void setUp() {
    initMocks(this);

    when(mockContext.getSystemService(eq(WindowManager.class))).thenReturn(mockWindowManager);
    mSystemStateUpdateHandler =
        new SystemStateUpdateHandler(
            mockContext, mockStateProviders, mockStateView, TEST_UPDATE_INTERVAL_MILLIS);
  }

  @Test
  public void startUpdates_addsStateView() {
    when(mockStateProviders.getSystemStates()).thenReturn(TEST_STATES);
    when(mockStateView.getView()).thenReturn(mockView);

    mSystemStateUpdateHandler.startUpdates();

    verify(mockWindowManager).addView(mockView, SystemStateUpdateHandler.WINDOW_MANAGER_PARAMS);
    verify(mockStateView).update(TEST_STATES);
  }

  @Test
  public void stopUpdates_removesStateView() {
    when(mockStateView.getView()).thenReturn(mockView);

    mSystemStateUpdateHandler.startUpdates();
    mSystemStateUpdateHandler.stopUpdates();

    verify(mockWindowManager).removeView(mockView);
  }
}
