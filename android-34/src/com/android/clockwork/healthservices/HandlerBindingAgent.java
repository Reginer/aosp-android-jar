package com.android.clockwork.healthservices;

import static com.android.clockwork.healthservices.HealthService.WHS_PACKAGE;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;

/** An implementation of {@link HealthService.BindingAgent} that uses {@link Handler}. */
class HandlerBindingAgent extends Handler implements HealthService.BindingAgent {
  private static final String TAG = "HandlerBindingAgent";

  @VisibleForTesting static final int MSG_ATTEMPT_BIND = 0;

  private static final String WHS_SERVICE_TO_BIND_TO =
      WHS_PACKAGE + ".background.service.RecordingService";

  /** The number of retries available to start with. */
  @VisibleForTesting static final int FRESH_NUM_RETRIES = 20;
  /**
   * Number of attempts to be made without exponential retries. We will try to bind to WHS this
   * number of times with constant intervals. For the reminder of the retries, each retry will
   * be {@link EXPONENTIAL_MULTIPLIER} times later than the previous retry.
   */
  private static final int NUM_NONEXPONENTIAL_RETRIES = 10;

  private static final int EXPONENTIAL_MULTIPLIER = 2;

  static final ComponentName WHS_SERVICE_COMPONENT_NAME =
      new ComponentName(WHS_PACKAGE, WHS_SERVICE_TO_BIND_TO);

  private final Context mContext;

  private int mNumAttemptsLeft = FRESH_NUM_RETRIES;
  private long mLastBindingDelayMillis = 0;

  private ConnectionTracker mConnectionTracker;

  HandlerBindingAgent(Context context) {
    super(Looper.getMainLooper(), null, /* async= */ true);
    mContext = context;
  }

  @Override // Handler
  public void handleMessage(Message msg) {
    Log.d(TAG, "Received a message: " + msg.what);

    if (msg.what == MSG_ATTEMPT_BIND) {
      try {
        boolean bindingSuccess =
            mContext.bindService(
                new Intent().setComponent(WHS_SERVICE_COMPONENT_NAME),
                mConnectionTracker,
                Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT);
        Log.d(TAG, "Binding success=" + bindingSuccess);

        if (!bindingSuccess) {
          if (--mNumAttemptsLeft > 0) {
            int attemptsMadeBefore = FRESH_NUM_RETRIES - mNumAttemptsLeft;
            boolean exponentialRetry = attemptsMadeBefore >= NUM_NONEXPONENTIAL_RETRIES;
            long delay = mLastBindingDelayMillis * (exponentialRetry ? EXPONENTIAL_MULTIPLIER : 1);

            Log.d(TAG,
                "Binding failed: retrying after "
                + delay
                + "ms. Attempts left: "
                + mNumAttemptsLeft);

            bind(delay);
          } else {
            Log.d(TAG, "Bidning failed: all attempts to bind failed.");
          }
        } else {
          cancelPendingBinds();
          mNumAttemptsLeft = FRESH_NUM_RETRIES;
        }
      } catch (SecurityException e) {
        Log.e(TAG, "Error attempting binding to WHS.", e);
      }
    }
  }

  @Override // BindingAgent
  public void bind(long delayMillis) {
    cancelPendingBinds();
    mLastBindingDelayMillis = delayMillis;
    sendEmptyMessageDelayed(MSG_ATTEMPT_BIND, delayMillis);
  }

  @Override // BindingAgent
  public void cancelPendingBinds() {
    Log.d(TAG, "Cancelling pending binds");
    removeMessages(MSG_ATTEMPT_BIND);
  }

  void setConnectionTracker(ConnectionTracker connectionTracker) {
    mConnectionTracker = connectionTracker;
  }
}
