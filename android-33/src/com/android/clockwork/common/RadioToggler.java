package com.android.clockwork.common;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;

/**
 * RadioToggler is a generic class useful for managing enable/disable operations on a radio.
 *
 * Most radio enable/disable operations are asynchronous over binder.  The radios themselves
 * may be in a number of states between "fully enabled" and "fully disabled".  This class provides
 * an idempotent interface for toggling a radio's state.
 *
 * Currently the Radio interface does not allow for special treatment of transitional radio states.
 * In practice, many radio managers (BluetoothAdapter, WifiManager) abstract these away and
 * basically treat "enabled" as enabled, and all other states as disabled. But the interface
 * specified here does not require that behavior - it merely requires that a specific decision
 * is made for what getEnabled() ought to return for any given state.
 */
public class RadioToggler {

    public interface Radio {
        String logTag();
        void setEnabled(boolean enabled);
        boolean getEnabled();
    }

    private static final int MSG_REFRESH_RADIO_STATE = 1;
    private static final int MSG_TOGGLE_RADIO = 2;

    final HandlerThread mHandlerThread;
    private final RadioHandler mHandler;

    private final Radio mRadio;
    private final PartialWakeLock mWakeLock;
    private final long mRadioToggleWaitMs;

    private final Object mObject = new Object();

    // mRadioEnabled is read-only on the main thread and read/write by the handler thread.
    // synchronization on this variable should be avoided so that the main thread can never
    // be blocked when reading it.
    private volatile boolean mRadioEnabled;

    public RadioToggler(Radio radio, PartialWakeLock wakeLock, long radioToggleWaitMs) {
        mRadio = radio;
        mWakeLock = wakeLock;
        mRadioToggleWaitMs = radioToggleWaitMs;

        mHandlerThread = new HandlerThread(mRadio.logTag() + ".Toggler");
        mHandlerThread.start();
        mHandler = new RadioHandler(mHandlerThread.getLooper());

        refreshRadioState();
    }

    public boolean getRadioEnabled() {
        return mRadioEnabled;
    }

    /**
     * Notify RadioToggler that the underlying radio state may have changed.
     */
    public void refreshRadioState() {
        Message msg = mHandler.obtainMessage(MSG_REFRESH_RADIO_STATE);
        if (!mHandler.hasMessages(MSG_REFRESH_RADIO_STATE)) {
            mHandler.sendMessage(msg);
        }
    }

    /**
     * Set the radio to the desired enable/disable state.
     */
    public void toggleRadio(boolean enable) {
        if (Log.isLoggable(mRadio.logTag(), Log.VERBOSE)) {
            Log.v(mRadio.logTag(), "ToggleRadio to: " + enable);
        }
        Message msg = mHandler.obtainMessage(MSG_TOGGLE_RADIO, enable);
        mHandler.removeMessages(MSG_TOGGLE_RADIO);
        mHandler.sendMessage(msg);
    }

    private class RadioHandler extends Handler {

        public RadioHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REFRESH_RADIO_STATE:
                    mRadioEnabled = mRadio.getEnabled();
                    if (Log.isLoggable(mRadio.logTag(), Log.VERBOSE)) {
                        Log.v(mRadio.logTag(),
                                "REFRESH_RADIO [ enabled = " + mRadioEnabled + "]");
                    }
                    break;
                case MSG_TOGGLE_RADIO:
                    if (Log.isLoggable(mRadio.logTag(), Log.VERBOSE)) {
                      Log.v(mRadio.logTag(),
                              "TOGGLE_RADIO enter [enabled = " + mRadioEnabled + "]");
                    }

                    boolean enable = (boolean) msg.obj;
                    if (enable == mRadioEnabled) {
                        return;
                    }

                    try {
                        mWakeLock.acquire();
                        mRadio.setEnabled(enable);
                        if (mRadioToggleWaitMs > 0) {
                            synchronized (mObject) {
                                mObject.wait(mRadioToggleWaitMs);
                            }
                        }
                    } catch (InterruptedException e) {
                        // pass
                    } finally {
                        mWakeLock.release();
                    }

                    mRadioEnabled = mRadio.getEnabled();
                    if (Log.isLoggable(mRadio.logTag(), Log.VERBOSE)) {
                        Log.v(mRadio.logTag(),
                                "TOGGLE_RADIO exit [enabled = " + mRadioEnabled + "]");
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
