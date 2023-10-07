/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.clockwork.displayoffload;

import android.os.Binder;
import android.os.Build;
import android.os.Process;
import android.os.ShellCommand;

import java.io.PrintWriter;

/** ShellCommand for DisplayOffloadService (adb shell dumpsys DisplayOffloadService) */
public class DisplayOffloadShellCommand extends ShellCommand {
    private static final String TAG = "DOShellCmd";
    private final DisplayOffloadService.BinderService mService;
    private Binder mBinder;

    public DisplayOffloadShellCommand(DisplayOffloadService.BinderService service) {
        mService = service;
    }

    @Override
    public int onCommand(String cmd) {
        final PrintWriter out = getOutPrintWriter();
        if (cmd == null) {
            return -1;
        }
        if (Binder.getCallingUid() != Process.ROOT_UID) {
            out.println("Must be root.");
            return -1;
        }
        if ("control_lock".equals(cmd)) {
            return handleDisplayControlLock();
        }
        if ("font_debug".equals(cmd)) {
            return handleFontDebugOptions();
        }
        if ("hal_dump".equals(cmd)) {
            return handleHalDumpOptions();
        }
        return handleDefaultCommands(cmd);
    }

    int handleDisplayControlLock() {
        final PrintWriter out = getOutPrintWriter();
        String arg = getNextArg();
        if ("1".equals(arg)) {
            if (mBinder == null) {
                mBinder = new Binder(TAG);
            }
            out.println("Acquiring DisplayControlLock");
            mService.acquireDisplayControlLock(mBinder, TAG);
        } else if ("0".equals(arg)) {
            out.println("Releasing DisplayControlLock");
            mService.releaseDisplayControlLock(mBinder);
        } else {
            out.println("Unrecognized argument");
            return -1;
        }
        return 0;
    }

    int handleFontDebugOptions() {
        final PrintWriter out = getOutPrintWriter();
        if (Binder.getCallingUid() != Process.ROOT_UID) {
            out.println("Must be root.");
            return 0;
        }
        String arg = getNextArg();
        if ("1".equals(arg)) {
            DebugUtils.DEBUG_FONT_DUMP = Build.IS_DEBUGGABLE;
            DebugUtils.DEBUG_FONT_SUBSETTING = Build.IS_DEBUGGABLE;
            out.println("Font debug turned on.");
        } else if ("0".equals(arg)) {
            DebugUtils.DEBUG_FONT_DUMP = false;
            DebugUtils.DEBUG_FONT_SUBSETTING = false;
            out.println("Font debug turned off.");
        } else {
            out.println("Unrecognized argument.");
            return -1;
        }
        return 0;
    }

    int handleHalDumpOptions() {
        final PrintWriter out = getOutPrintWriter();
        String arg = getNextArg();
        if ("1".equals(arg)) {
            DebugUtils.DEBUG_HAL_DUMP = true;
            out.println("HAL dump turned on.");
        } else if ("0".equals(arg)) {
            DebugUtils.DEBUG_HAL_DUMP = false;
            out.println("HAL dump turned off.");
        } else {
            out.println("Unrecognized argument.");
            return -1;
        }
        return 0;
    }

    @Override
    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("DisplayOffloadService commands:");
        pw.println("  help");
        pw.println("      Print this help text.");
        pw.println("  control_lock");
        pw.println("      0 : release control lock.");
        pw.println("      1 : acquire control lock.");
        pw.println("  font_debug");
        pw.println("      0 : on.");
        pw.println("      1 : off.");
        pw.println("  hal_dump");
        pw.println("      0 : on.");
        pw.println("      1 : off.");
        pw.println();
    }
}
