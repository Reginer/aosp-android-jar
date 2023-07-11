package com.android.clockwork.bluetooth.proxy;

import android.net.NetworkInfo;
import org.robolectric.annotation.Implements;

/**
 * Shadow of {@link NetworkInfo}.
 *
 * Extends to be able to create NetworkInfos, as this class
 * by default accesses uninstantiated static lookup maps.
 *
 */
@Implements(NetworkInfo.class)
public class ShadowNetworkInfo extends org.robolectric.shadows.ShadowNetworkInfo {

    // NetworkInfo consults a static map during construction breaking things
    public void __constructor__(int type, int subtype, String typeName, String subtypeName) {}

    public void setDetailedState(NetworkInfo.DetailedState detailedState,
            String reason, String extraInfo) {
        super.setDetailedState(detailedState);
    }
}
