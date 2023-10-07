package com.android.clockwork.bluetooth;


import android.bluetooth.BluetoothAdapter;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

/**
 * Shadow class for {@link BluetoothAdapter}.
 *
 * Extended to allow for testing null adapters
 *
 */
@Implements(BluetoothAdapter.class)
public class ShadowBluetoothAdapter extends org.robolectric.shadows.ShadowBluetoothAdapter {
    private static BluetoothAdapter instanceForTest;
    public static boolean forceNull;

    public static void setAdapter(BluetoothAdapter adapter) {
        instanceForTest = adapter;
    }

    @Implementation
    public static BluetoothAdapter getDefaultAdapter() {
        if (instanceForTest == null) {
            instanceForTest = Shadow.newInstanceOf(BluetoothAdapter.class);
        }
        if (forceNull) {
            return null;
        }
        return instanceForTest;
    }
}
