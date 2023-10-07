package com.android.clockwork.systemstatedisplay;

import android.annotation.Nullable;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;
import com.android.internal.annotations.VisibleForTesting;

/** A handler for regular updates and displaying of the system's state. */
class SystemStateUpdateHandler extends Handler {
  private static final String TAG = "SystemStateUpdateHandler";

  private static final int MSG_INIT_STATE_VIEW = 0;
  private static final int MSG_START_STATE_VIEW_UPDATES = 1;
  private static final int MSG_UPDATE_STATE_VIEW = 2;
  private static final int MSG_STOP_STATE_VIEW_UPDATES = 3;

  @VisibleForTesting
  static final WindowManager.LayoutParams WINDOW_MANAGER_PARAMS =
      new WindowManager.LayoutParams(
          WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
          WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
              | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
          PixelFormat.TRANSPARENT);

  private final long mUpdateIntervalMillis;
  private final WindowManager mWindowManager;
  private final Context mContext;

  private SystemStateProviders mStateProviders;
  @Nullable private SystemStateViewHolder mStateView;
  private boolean mUpdatesActive;

  @VisibleForTesting
  SystemStateUpdateHandler(
      Context context,
      SystemStateProviders stateProviders,
      @Nullable SystemStateViewHolder stateView,
      long updateIntervalMillis) {
    super(Looper.getMainLooper(), null, /* async= */ true);

    mWindowManager = context.getSystemService(WindowManager.class);

    mContext = context;
    mUpdateIntervalMillis = updateIntervalMillis;
    mStateProviders = stateProviders;
    mStateView = stateView;

    sendEmptyMessage(MSG_INIT_STATE_VIEW);
  }

  SystemStateUpdateHandler(Context context, long updateIntervalMillis) {
    this(context, new SystemStateProviders(context), /* stateView= */ null, updateIntervalMillis);
  }

  @Override
  public void handleMessage(Message msg) {
    Log.d(TAG, "message: " + msg.what);

    switch (msg.what) {
      case MSG_INIT_STATE_VIEW:
        if (mStateView == null) mStateView = new SystemStateViewHolder(mContext);
        break;
      case MSG_START_STATE_VIEW_UPDATES:
        sendEmptyMessage(MSG_UPDATE_STATE_VIEW);
        mWindowManager.addView(mStateView.getView(), WINDOW_MANAGER_PARAMS);
        break;
      case MSG_UPDATE_STATE_VIEW:
        mStateView.update(mStateProviders.getSystemStates());
        // Schedule the next update to be processed after `mUpdateIntervalMillis`.
        sendEmptyMessageDelayed(MSG_UPDATE_STATE_VIEW, mUpdateIntervalMillis);
        break;
      case MSG_STOP_STATE_VIEW_UPDATES:
        mWindowManager.removeView(mStateView.getView());
        removeMessages(MSG_UPDATE_STATE_VIEW);
        break;
    }
  }

  /**
   * Starts regular system state updates, fetching the system state with an interval equal to the
   * handler's update interval and updating the state view accordingly.
   */
  public void startUpdates() {
    if (mUpdatesActive) return;

    Log.d(TAG, "Starting system state updates...");
    sendEmptyMessage(MSG_START_STATE_VIEW_UPDATES);

    mUpdatesActive = true;
  }

  /** Stops fetching system state updates and updating the state view. */
  public void stopUpdates() {
    if (!mUpdatesActive) return;

    Log.d(TAG, "Stopping system state updates...");
    sendEmptyMessage(MSG_STOP_STATE_VIEW_UPDATES);

    mUpdatesActive = false;
  }
}
