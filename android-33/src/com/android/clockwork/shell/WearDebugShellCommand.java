package com.android.clockwork.shell;

import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.os.UserHandle;

import com.android.internal.annotations.GuardedBy;

import java.io.PrintWriter;

public final class WearDebugShellCommand extends ShellCommand {
    private static final String DEBUG_INTENT = "com.google.android.wearable.app.DEBUG_SURFACE";
    private static final String OPERATION_EXTRA_KEY = "operation";
    private static final String COMPONENT_EXTRA_KEY = "component";
    private static final String SET_WATCH_FACE_OPERATION_EXTRA_VALUE = "set-watchface";

    private final IActivityManager mActivityManager;

    WearDebugShellCommand(IActivityManager activityManager) {
        this.mActivityManager = activityManager;
    }

    @Override
    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("WearDebug commands:");
        pw.println("  help");
        pw.println("      Print this help text.");
        pw.println("  set-watchface [-D] <WATCH_FACE_COMPONENT_NAME>");
        pw.println("      -D : enable debugging.");
        pw.println();
    }

    @Override
    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        final PrintWriter pw = getOutPrintWriter();
        try {
            switch (cmd) {
                case "set-watchface":
                    return runSetWatchFace(pw);
                default:
                    return handleDefaultCommands(cmd);
            }
        } catch (RemoteException e) {
            pw.println("Remote exception: " + e);
        }
        return -1;
    }

    private static final class SetWatchFaceIntentReceiver extends IIntentReceiver.Stub {
        @GuardedBy("mLock")
        private boolean mFinished = false;

        private int mResult = 0;
        private final Object mLock = new Object();

        @Override
        public void performReceive(
                Intent intent,
                int resultCode,
                String data,
                Bundle extras,
                boolean ordered,
                boolean sticky,
                int sendingUser) {
            mResult = resultCode;
            synchronized (mLock) {
                mFinished = true;
                mLock.notifyAll();
            }
        }

        public int waitForFinish() {
            try {
                synchronized (mLock) {
                    while (!mFinished) mLock.wait();
                }
                return mResult;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    private int setWatchFace(ComponentName watchFace) throws RemoteException {
        Intent intent;
        intent = new Intent(DEBUG_INTENT);
        intent.addFlags(Intent.FLAG_RECEIVER_FROM_SHELL);
        intent.putExtra(OPERATION_EXTRA_KEY, SET_WATCH_FACE_OPERATION_EXTRA_VALUE);
        intent.putExtra(COMPONENT_EXTRA_KEY, watchFace);
        SetWatchFaceIntentReceiver receiver = new SetWatchFaceIntentReceiver();
        mActivityManager.broadcastIntent(
                null,
                intent,
                null,
                receiver,
                0,
                null,
                null,
                null,
                android.app.AppOpsManager.OP_NONE,
                null,
                true,
                true,
                UserHandle.USER_CURRENT);
        return receiver.waitForFinish();
    }

    private int runSetWatchFace(PrintWriter pw) throws RemoteException {
        final PrintWriter err = getErrPrintWriter();
        boolean debug = false;
        String opt;
        while ((opt = getNextOption()) != null) {
            if ("-D".equals(opt)) {
                debug = true;
            } else {
                err.println("Error: unknown option: " + opt);
                return -1;
            }
        }
        ComponentName watchFace = ComponentName.unflattenFromString(getNextArgRequired());

        if (debug) {
            mActivityManager.setDebugApp(watchFace.getPackageName(), true, false);
        }

        return setWatchFace(watchFace);
    }
}
