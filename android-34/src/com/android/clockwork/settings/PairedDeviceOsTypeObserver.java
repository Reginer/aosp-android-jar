package com.android.clockwork.settings;

import static android.provider.Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE;

import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import com.android.clockwork.setup.PostSetupPackageHelper;
import com.android.internal.annotations.VisibleForTesting;

final class PairedDeviceOsTypeObserver implements WearPersistentSettingsObserver.Observer {
    private static final String TAG = "PairedDeviceOsTypeObserver";

    private final PostSetupPackageHelper mPostSetupPackageHelper;

    @VisibleForTesting
    PairedDeviceOsTypeObserver(PostSetupPackageHelper postSetupPackageHelper) {
        mPostSetupPackageHelper = postSetupPackageHelper;
    }

    PairedDeviceOsTypeObserver(Context context) {
        this(new PostSetupPackageHelper(context));
    }

    @Override
    public void onChange() {
        Log.d(TAG, "Received update on PAIRED_DEVICE_OS_TYPE");
        mPostSetupPackageHelper.run();
    }

    @Override
    public Uri getUri() {
        return Settings.Global.getUriFor(PAIRED_DEVICE_OS_TYPE);
    }

    @Override
    public boolean shouldTriggerObserverOnStart() {
        return true;
    }
}