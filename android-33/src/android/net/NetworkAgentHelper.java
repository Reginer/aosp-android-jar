package android.net;

import android.annotation.NonNull;

/**
 * Wrapper around {@link android.net.NetworkAgent} to help test coverage
 *
 * {@link NetworkAgent} will call non-public method unwanted() when the
 * agent should be disabled.
 */
public class NetworkAgentHelper {
    public static void callUnwanted(@NonNull NetworkAgent networkAgent) {
        System.out.println("NetworkAgentHelper Faking unwanted() call from connectivity manager");
        networkAgent.onNetworkUnwanted();
    }
}
