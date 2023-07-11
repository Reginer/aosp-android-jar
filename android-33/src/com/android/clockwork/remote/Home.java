package com.android.clockwork.remote;

/**
 * Power intents that Clockwork Home broadcasts to interact with Settings and framework.
 *
 * See {@link com.google.android.clockwork.battery.wear.PowerIntents}
 */
public final class Home {
    private Home() {}

    public static final String ACTION_SLEEP = "com.google.android.wearable.home.action.SLEEP";

    public static final String ACTION_ENABLE_MULTICORE =
            "com.google.android.wearable.home.action.ENABLE_MULTICORE";

    public static final String ACTION_DISABLE_MULTICORE =
            "com.google.android.wearable.home.action.DISABLE_MULTICORE";

    public static final String ACTION_ENABLE_TOUCH =
            "com.google.android.wearable.home.action.ENABLE_TOUCH";

    public static final String ACTION_DISABLE_TOUCH =
            "com.google.android.wearable.home.action.DISABLE_TOUCH";
}
