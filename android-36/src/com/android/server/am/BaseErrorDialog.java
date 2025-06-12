/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.server.am;

import static android.content.Context.RECEIVER_EXPORTED;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;

import com.android.internal.R;

public class BaseErrorDialog extends AlertDialog {
    private static final int ENABLE_BUTTONS = 0;
    private static final int DISABLE_BUTTONS = 1;

    private boolean mConsuming = true;
    private BroadcastReceiver mReceiver;

    public BaseErrorDialog(Context context) {
        super(context, com.android.internal.R.style.Theme_DeviceDefault_Dialog_AppError);
        context.assertRuntimeOverlayThemable();

        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle("Error Dialog");
        getWindow().setAttributes(attrs);
    }

    @Override
    public void onStart() {
        super.onStart();
        mHandler.sendEmptyMessage(DISABLE_BUTTONS);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(ENABLE_BUTTONS), 1000);
        if (mReceiver == null) {
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(intent.getAction())) {
                        closeDialog();
                    }
                }
            };
            getContext().registerReceiver(mReceiver,
                    new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS), RECEIVER_EXPORTED);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mReceiver != null) {
            try {
                getContext().unregisterReceiver(mReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver not registered exception.
                android.util.Slog.e("BaseErrorDialog",
                        "unregisterReceiver threw exception: " + e.getMessage());
            }
            mReceiver = null;
        }
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mConsuming) {
            //Slog.i(TAG, "Consuming: " + event);
            return true;
        }
        //Slog.i(TAG, "Dispatching: " + event);
        return super.dispatchKeyEvent(event);
    }

    private void setEnabled(boolean enabled) {
        Button b = (Button)findViewById(R.id.button1);
        if (b != null) {
            b.setEnabled(enabled);
        }
        b = (Button)findViewById(R.id.button2);
        if (b != null) {
            b.setEnabled(enabled);
        }
        b = (Button)findViewById(R.id.button3);
        if (b != null) {
            b.setEnabled(enabled);
        }
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == ENABLE_BUTTONS) {
                mConsuming = false;
                setEnabled(true);
            } else if (msg.what == DISABLE_BUTTONS) {
                setEnabled(false);
            }
        }
    };

    /**
     * Called when received ACTION_CLOSE_SYSTEM_DIALOGS.
     */
    protected void closeDialog() {
        if (mCancelable) {
            cancel();
        } else {
            dismiss();
        }
    }
}
