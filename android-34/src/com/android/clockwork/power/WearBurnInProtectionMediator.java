package com.android.clockwork.power;

import android.content.Context;
import android.os.SystemProperties;

public class WearBurnInProtectionMediator implements AmbientConfig.Listener {
    private final AmbientConfig mAmbientConfig;
    private static final String SYSPROP_BURN_IN_PROTECTION_ENABLE =
            "sys.burn_in_protection.enabled";

    WearBurnInProtectionMediator(Context context, AmbientConfig ambientConfig) {
        mAmbientConfig = ambientConfig;
        mAmbientConfig.addListener(this);
        updateBurnInProtection();
    }

    @Override
    public void onAmbientConfigChanged() {
        updateBurnInProtection();
    }

    private void updateBurnInProtection() {
        boolean shouldDisable = mAmbientConfig.isWatchfaceDecomposable();
        SystemProperties.set(SYSPROP_BURN_IN_PROTECTION_ENABLE, shouldDisable ? "0" : "1");
    }
}
