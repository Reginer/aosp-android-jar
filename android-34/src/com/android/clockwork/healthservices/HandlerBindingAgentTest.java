package com.android.clockwork.healthservices;

import static com.android.clockwork.healthservices.HandlerBindingAgent.WHS_SERVICE_COMPONENT_NAME;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.Context;
import android.content.Intent;
import android.os.Message;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public final class HandlerBindingAgentTest {

  @Mock Context mockContext;
  @Mock ConnectionTracker mockConnectionTracker;

  private final ArgumentCaptor<Intent> mIntentCaptor = ArgumentCaptor.forClass(Intent.class);

  private HandlerBindingAgent mBindingAgent;

  @Before
  public void setUp() {
    initMocks(this);

    mBindingAgent = new HandlerBindingAgent(mockContext);
    mBindingAgent.setConnectionTracker(mockConnectionTracker);
  }

  @Test
  public void handleMessage_bindMessage_binds() {
    mockBindingSuccess(/* success= */ true);
    Message message = new Message();
    message.what = HandlerBindingAgent.MSG_ATTEMPT_BIND;

    mBindingAgent.handleMessage(message);

    verify(mockContext)
        .bindService(
            mIntentCaptor.capture(),
            eq(mockConnectionTracker),
            eq(Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT));
    assertThat(mIntentCaptor.getValue().getComponent()).isEqualTo(WHS_SERVICE_COMPONENT_NAME);
  }

  @Test
  public void handleMessage_notBindMessage_noBind() {
    mockBindingSuccess(/* success= */ false);
    Message message = new Message();
    message.what = 534; // Random message that is not the bind message.

    mBindingAgent.handleMessage(message);

    verify(mockContext, never()).bindService(any(), any(), anyInt());
  }

  @Test
  public void bindFailed_retriesRightNumberOfTimes() {
    Message message = new Message();
    message.what = HandlerBindingAgent.MSG_ATTEMPT_BIND;

    mBindingAgent.handleMessage(message);

    verify(mockContext, times(HandlerBindingAgent.FRESH_NUM_RETRIES))
        .bindService(
            mIntentCaptor.capture(),
            eq(mockConnectionTracker),
            eq(Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT));
    for (Intent intent : mIntentCaptor.getAllValues()) {
      assertThat(intent.getComponent()).isEqualTo(WHS_SERVICE_COMPONENT_NAME);
    }
  }

  private void mockBindingSuccess(boolean success) {
    when(mockContext.bindService(
        any(Intent.class),
        eq(mockConnectionTracker),
        eq(Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT))).thenReturn(success);
  }
}
