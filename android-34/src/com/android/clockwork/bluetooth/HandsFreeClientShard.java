package com.android.clockwork.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;

import com.android.clockwork.common.DebugAssert;

import java.io.Closeable;
import java.io.PrintWriter;

/**
 * A Shard that insists on establishing a client connection to the HandsFree profile on the
 * companion.
 */
/* package private */ class HandsFreeClientShard implements Closeable {
  private static final String TAG = HandsFreeClientShard.class.getSimpleName();
  // b/189877996 - an artificial delay is added to HFP reconnects so ensure that this does not
  // trigger re-pairing after an unbond event (before this device processes the unbond event)
  private static final int CONNECT_DELAY_MS = 5_000;
  private static final int POLL_PERIOD_MS = 30_000;
  private static final int MAX_RECONNECT_TIMES = 5;

  private static final int MSG_RECONNECT = 0;
  private static final int MSG_STOP_SHARD = 1;

  private static final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
  private final Context context;
  private final BluetoothDevice companionDevice;
  private final BluetoothShardRunner mRunner;
  private final ContentResolver mContentResolver;
  private BluetoothHeadsetClient handsFreeProfile;
  private boolean isClosed;
  private int mReconnectCount = 0;
  public HandsFreeClientShard(final Context context, final BluetoothDevice device,
      BluetoothShardRunner runner) {
    DebugAssert.isMainThread();
    this.context = context;
    mContentResolver = context.getContentResolver();
    companionDevice = device;
    mRunner = runner;
    adapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET_CLIENT);
    context.registerReceiver(stateChangeReceiver,
            new IntentFilter(BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED));
    mContentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.BLUETOOTH_DISABLED_PROFILES),
            false,
            disabledProfileObserver);
  }
  @Override
  public void close() {
    DebugAssert.isMainThread();
    if (isClosed) {
      return;
    }
    // Remove the retry message since this shard is closed.
    handler.removeMessages(MSG_RECONNECT);
    context.unregisterReceiver(stateChangeReceiver);
    if (handsFreeProfile != null) {
      handsFreeProfile.disconnect(companionDevice);
      adapter.closeProfileProxy(BluetoothProfile.HEADSET_CLIENT, handsFreeProfile);
    }
    mContentResolver.unregisterContentObserver(disabledProfileObserver);
    isClosed = true;
  }
  private void onProfileAvailable() {
    // Nothing to do if the profile is already connected.
    if (handsFreeProfile.getConnectionState(companionDevice) == BluetoothProfile.STATE_CONNECTED) {
      Log.i(TAG, "HandsFree client profile is already connected.");
      return;
    }
    // Try to connect and set up a retry loop in case it fails.
    Log.i(TAG, "Connecting HandsFree client profile (startup).");
    handsFreeProfile.setConnectionPolicy(
            companionDevice, BluetoothProfile.CONNECTION_POLICY_ALLOWED);
    handler.removeMessages(MSG_RECONNECT);
    handler.sendEmptyMessageDelayed(MSG_RECONNECT, POLL_PERIOD_MS);
  }
  private final Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch(msg.what) {
        case MSG_RECONNECT:
          mReconnectCount++;
          if (handsFreeProfile != null) {
            if (mReconnectCount > MAX_RECONNECT_TIMES) {
              Log.i(TAG, "Reached maximum retry, close the shard");
              handler.sendEmptyMessage(MSG_STOP_SHARD);
              return;
            }
            Log.d(TAG, "Connecting HandsFree client profile (retry #" + mReconnectCount + ").");
            handsFreeProfile.disconnect(companionDevice);
            handsFreeProfile.connect(companionDevice);
            handler.sendEmptyMessageDelayed(MSG_RECONNECT, POLL_PERIOD_MS);
          }
          break;
        case MSG_STOP_SHARD:
          handler.removeMessages(MSG_RECONNECT);
          mRunner.stopHfcShard();
          break;
      }
    }
  };
  private final BroadcastReceiver stateChangeReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (!BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED.equals(intent.getAction())) {
        throw new IllegalStateException(
                "Expected ACTION_CONNECTION_STATE_CHANGED, received " + intent.getAction());
      }
      final int newState =
              intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED);
      if (handsFreeProfile != null && newState == BluetoothProfile.STATE_DISCONNECTED) {
        // If there's a retry scheduled already, we shouldn't preempt it here.
        if (!handler.hasMessages(MSG_RECONNECT)) {
          handler.sendEmptyMessageDelayed(MSG_RECONNECT, CONNECT_DELAY_MS);
        }
      } else if (newState == BluetoothProfile.STATE_CONNECTED) {
        // Stop polling since we just connected.
        Log.i(TAG, "HFPC is connected, stop reconnection");
        handler.removeMessages(MSG_RECONNECT);
      }
    }
  };
  private final BluetoothProfile.ServiceListener profileListener =
          new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
              DebugAssert.isMainThread();
              handsFreeProfile = (BluetoothHeadsetClient) proxy;
              if (isClosed) {
                onServiceDisconnected(profile);
                return;
              }
              // The profile might be disabled but still be available. This happens when the
              // bluetooth stack did no restart after a change in the list of disabled profiles
              // (e.g. in the case of bluetooth reconnection).
              if (isHandsFreeProfileEnabled()) {
                onProfileAvailable();
              }
            }
            @Override
            public void onServiceDisconnected(int profile) {
              DebugAssert.isMainThread();
              adapter.closeProfileProxy(BluetoothProfile.HEADSET_CLIENT, handsFreeProfile);
              handsFreeProfile = null;
              handler.sendEmptyMessage(MSG_STOP_SHARD);
            }
          };

  private final ContentObserver disabledProfileObserver = new ContentObserver(new Handler()) {
    @Override
    public void onChange(boolean selfChange) {
      // No need to handle the case where the profile is enabled as it would restart the bluetooth
      // stack be handled when the profile proxy is available.
      if (handsFreeProfile == null) {
        return;
      }
      // Disconnect and disallow HFP connections to this device when the HFP profile has been
      // disabled.
      if (!isHandsFreeProfileEnabled()) {
        Log.d(TAG, "HFP Disabled. Disconnect the hands free profile.");
        handsFreeProfile.setConnectionPolicy(
                companionDevice, BluetoothProfile.CONNECTION_POLICY_FORBIDDEN);
      }
    }
  };

  private boolean isHandsFreeProfileEnabled() {
    final long disabledProfileSetting = Settings.Global.getLong(
            mContentResolver, Settings.Global.BLUETOOTH_DISABLED_PROFILES, 0);
    final long hfpMask = 1 << BluetoothProfile.HEADSET_CLIENT;
    return (disabledProfileSetting & hfpMask) == 0;
  }

  public void dump(final PrintWriter writer) {
    writer.printf("HandsFreeClient [%s]\n", companionDevice);
    if (handsFreeProfile != null) {
      final int state = handsFreeProfile.getConnectionState(companionDevice);
      writer.printf("  Profile state: %d\n", state);
      writer.printf("  Retry scheduled: %b\n", handler.hasMessages(MSG_RECONNECT));
    } else {
      writer.println("  Profile unavailable.");
    }
  }
}
