package com.android.clockwork.healthservices;

import static com.android.clockwork.healthservices.HealthService.BindingAgent;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;

/** Tracks connection with WHS. */
class ConnectionTracker implements ServiceConnection {
  private static final String TAG = "WhsConnectionTracker";

  private final IBinder.DeathRecipient mDeathRecipient;

  private BindingAgent mBindingAgent;
  private boolean mConnected;

  ConnectionTracker(IBinder.DeathRecipient deathRecipient) {
    mDeathRecipient = deathRecipient;
  }

  @Override
  public void onServiceConnected(ComponentName className, IBinder service) {
    if (!className.equals(HandlerBindingAgent.WHS_SERVICE_COMPONENT_NAME)) {
      Log.e(TAG, "Unintended service connection event received: " + className);
      return;
    }

    if (mConnected) {
      Log.w(TAG, "onServiceConnected: WHS is already connected!");
      return;
    }
    if (service == null) {
      Log.w(TAG, "onServiceConnected: Connected service is null!");
      return;
    }
    if (!service.isBinderAlive()) {
      Log.w(TAG, "onServiceConnected: Connected service is not alive!");
      return;
    }

    Log.d(TAG, "onServiceConnected: Connected binder is alive: cancelling pending binds");
    mBindingAgent.cancelPendingBinds();
    mConnected = true;

    try {
      service.linkToDeath(mDeathRecipient, /* flags= */ 0);
      Log.d(TAG, "Linked to WHS's death.");
    } catch (RemoteException e) {
      Log.e(TAG, "Error linking to binder's death.", e);
    }
  }

  @Override
  public void onServiceDisconnected(ComponentName className) {
    if (!className.equals(HandlerBindingAgent.WHS_SERVICE_COMPONENT_NAME)) {
      Log.e(TAG, "Unintended service disconnection event received" + className);
      return;
    }
    mConnected = false;
  }

  /** Returns {@code true} if system is currently connected to WHS, {@code false} otherwise. */
  boolean isConnected() {
    return mConnected;
  }

  void setBindingAgent(BindingAgent BindingAgent) {
    mBindingAgent = BindingAgent;
  }

  @VisibleForTesting
  void setConnected(boolean connected) {
    mConnected = connected;
  }
}
