package com.android.clockwork.time;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.SystemService;

import java.io.File;

public class WearTimeService extends SystemService {
    private static final String TAG = WearTimeService.class.getSimpleName();

    TimeStateRecorder mTimeStateRecorder;

    public WearTimeService(Context context) {
        this(context, new TimeStateRecorder());
    }

    @VisibleForTesting
    WearTimeService(Context context, TimeStateRecorder timeStateRecorder) {
        super(context);
        mTimeStateRecorder = timeStateRecorder;
    }

    @Override
    public void onStart() {}

    @Override
    public void onBootPhase(int phase) {
        if (phase != SystemService.PHASE_SYSTEM_SERVICES_READY) {
            return;
        }
        if (injectIsSafeMode()) {
            return;
        }

        Context context = getContext();

        if (!mTimeStateRecorder.init(context,
                new TimeState(new File(Environment.getDataDirectory(), "bootanim/time")))) {
            mTimeStateRecorder = null;
            Log.e(TAG, "Could not init TimeStateRecorder");
            return;
        }

        context.registerReceiver(mTimeStateRecorder,
                new IntentFilter(Intent.ACTION_TIME_CHANGED));
    }

    boolean injectIsSafeMode() {
        return isSafeMode();
    }

}
