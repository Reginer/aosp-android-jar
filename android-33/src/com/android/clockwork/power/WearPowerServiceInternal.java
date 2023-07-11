package com.android.clockwork.power;

/**
 * Service for interacting with the WearPowerService.
 *
 * @hide Only for use within system server.
 */
public abstract class WearPowerServiceInternal {
    public abstract AmbientConfig getAmbientConfig();
    public abstract PowerTracker getPowerTracker();
    public abstract TimeOnlyMode getTimeOnlyMode();
}
