package com.android.clockwork.shell;

import android.app.ActivityManager;
import android.content.Context;
import android.os.ResultReceiver;
import android.os.ShellCallback;

import com.android.server.SystemService;

import java.io.FileDescriptor;

public class WearDebugService extends SystemService {
    private static final String TAG = "wear.debug";

    public WearDebugService(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        publishBinderService(TAG, new BinderService());
    }

    private final class BinderService extends android.os.Binder implements android.os.IInterface {
        @Override
        public android.os.IBinder asBinder() {
            return this;
        }

        @Override
        public void onShellCommand(
                FileDescriptor in,
                FileDescriptor out,
                FileDescriptor err,
                String[] args,
                ShellCallback callback,
                ResultReceiver resultReceiver) {
            (new WearDebugShellCommand(ActivityManager.getService()))
                    .exec(this, in, out, err, args, callback, resultReceiver);
        }
    }
}
