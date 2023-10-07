package com.android.clockwork.healthservices;

import static com.android.clockwork.healthservices.HealthService.BindingAgent;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class ConnectionTrackerTest {

  @Mock Context mockContext;
  @Mock BindingAgent mockBindingAgent;
  @Mock IBinder mockBinder;
  @Mock IBinder.DeathRecipient mockDeathReceipient;

  private ConnectionTracker mConnectionTracker;

  @Before
  public void setUp() {
    initMocks(this);

    mConnectionTracker = new ConnectionTracker(mockDeathReceipient);
    mConnectionTracker.setBindingAgent(mockBindingAgent);
  }

  @Test
  public void onServiceConnected_nonWhsService_noLinkToDeath() throws Exception {
    mConnectionTracker.setConnected(false);
    when(mockBinder.isBinderAlive()).thenReturn(true);

    mConnectionTracker.onServiceConnected(new ComponentName("random", "random"), mockBinder);

    verify(mockBindingAgent, never()).cancelPendingBinds();
    verify(mockBinder, never()).linkToDeath(any(), anyInt());
  }

  @Test
  public void onServiceConnected_whsService_whsAlreadyConnected_noLinkToDeath() throws Exception {
    mConnectionTracker.setConnected(true);
    when(mockBinder.isBinderAlive()).thenReturn(true);

    mConnectionTracker.onServiceConnected(
        HandlerBindingAgent.WHS_SERVICE_COMPONENT_NAME, mockBinder);

    verify(mockBindingAgent, never()).cancelPendingBinds();
    verify(mockBinder, never()).linkToDeath(any(), anyInt());
  }

  @Test
  public void onServiceConnected_whsService_deadBinder_noLinkToDeath() throws Exception {
    mConnectionTracker.setConnected(false);
    when(mockBinder.isBinderAlive()).thenReturn(false);

    mConnectionTracker.onServiceConnected(
        HandlerBindingAgent.WHS_SERVICE_COMPONENT_NAME, mockBinder);

    verify(mockBindingAgent, never()).cancelPendingBinds();
    verify(mockBinder, never()).linkToDeath(any(), anyInt());
  }

  @Test
  public void onServiceConnected_whsService_liveBinder_linksToDeath() throws Exception {
    mConnectionTracker.setConnected(false);
    when(mockBinder.isBinderAlive()).thenReturn(true);

    mConnectionTracker.onServiceConnected(
        HandlerBindingAgent.WHS_SERVICE_COMPONENT_NAME, mockBinder);

    verify(mockBindingAgent).cancelPendingBinds();
    verify(mockBinder).linkToDeath(eq(mockDeathReceipient), /* flags= */ eq(0));
  }
}
