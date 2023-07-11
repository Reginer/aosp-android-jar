package com.android.clockwork.wifi;

import com.android.internal.util.IndentingPrintWriter;

/**
 * Use this implementation in WifiMediator to disable WifiBackoff.
 */
public class NoWifiBackoff implements WifiBackoff {
    @Override
    public void setListener(Listener listener) {
    }

    @Override
    public boolean isInBackoff() {
        return false;
    }

    @Override
    public void scheduleBackoff() {
    }

    @Override
    public void cancelBackoff() {
    }

    @Override
    public void dump(IndentingPrintWriter ipw) {
        ipw.println("No WifiBackoff");
    }
}
