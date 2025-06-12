/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.server.policy;

import android.app.ActivityManager;
import android.content.Context;
import android.os.UserManager;
import com.android.internal.globalactions.LongPressAction;
import com.android.internal.globalactions.SinglePressAction;
import com.android.internal.R;
import com.android.server.policy.WindowManagerPolicy;

public final class RestartAction extends SinglePressAction implements LongPressAction {
    private final Context mContext;
    private final WindowManagerPolicy.WindowManagerFuncs mWindowManagerFuncs;

    public RestartAction(Context context,
            WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) {
        super(R.drawable.ic_restart, R.string.global_action_restart);
        mContext = context;
        mWindowManagerFuncs = windowManagerFuncs;
    }

    @Override
    public boolean onLongPress() {
        // don't actually trigger the reboot if we are running stability
        // tests via monkey
        if (ActivityManager.isUserAMonkey()) {
            return false;
        }
        UserManager um = mContext.getSystemService(UserManager.class);
        if (!um.hasUserRestriction(UserManager.DISALLOW_SAFE_BOOT)) {
            mWindowManagerFuncs.rebootSafeMode(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean showDuringKeyguard() {
        return true;
    }

    @Override
    public boolean showBeforeProvisioning() {
        return true;
    }

    @Override
    public void onPress() {
        // don't actually trigger the reboot if we are running stability
        // tests via monkey
        if (ActivityManager.isUserAMonkey()) {
            return;
        }
        mWindowManagerFuncs.reboot(false /* confirm */);
    }
}
