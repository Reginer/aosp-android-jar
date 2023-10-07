package com.android.clockwork.systemstatedisplay;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.Context;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class SystemStateProvidersTest {

  @Mock Context mockContext;
  @Mock SystemStateProvider mockProvider1;
  @Mock SystemStateProvider mockProvider2;
  @Mock SystemStateProvider mockProvider3;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void getSystemStates_providesAllStatesInOrderOfTheirTitle() {
    SystemState testState1 = new SystemState("Title A", "19");
    SystemState testState2 = new SystemState("Z's Title", "OFF");
    SystemState testState3 = new SystemState("Another Title", "200");
    when(mockProvider1.getSystemState()).thenReturn(testState1);
    when(mockProvider2.getSystemState()).thenReturn(testState2);
    when(mockProvider3.getSystemState()).thenReturn(testState3);

    SystemStateProviders providers =
        new SystemStateProviders(Arrays.asList(mockProvider1, mockProvider2, mockProvider3));
    List<SystemState> states = providers.getSystemStates();

    assertThat(states).containsExactly(testState3, testState1, testState2).inOrder();
  }

  @Test
  public void getSystemStates_ignoresInvalidStates() {
    when(mockProvider1.getSystemState()).thenReturn(new SystemState("Title A", "19"));
    when(mockProvider2.getSystemState()).thenReturn(SystemState.INVALID_STATE);

    SystemStateProviders providers =
        new SystemStateProviders(Arrays.asList(mockProvider1, mockProvider2));
    List<SystemState> states = providers.getSystemStates();

    assertThat(states).doesNotContain(SystemState.INVALID_STATE);
  }
}
