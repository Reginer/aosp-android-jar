package com.android.clockwork.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.android.internal.util.IndentingPrintWriter;

import java.util.Set;

/**
 * BluetoothScanModeEnforcer ensures the Wear device is in connectable scan mode whenever there is
 * at least one bonded non-companion Bluetooth classic peripheral. It also ensures scan mode is
 * disabled when there is only one bonded device (i.e. companion).
 *
 * The current implemention only turns on page scan when there is at least one BT classic
 * peripheral.  It does not take into account if a classic peripheral is connected or not.
 *
 * Related bugs: 17398253 and 33368797
 */
public class BluetoothScanModeEnforcer {
    private static final String TAG = WearBluetoothSettings.LOG_TAG;

    private final BluetoothAdapter mAdapter;
    private final CompanionTracker mCompanionTracker;

    public BluetoothScanModeEnforcer(Context context, BluetoothAdapter btAdapter,
            CompanionTracker companionTracker) {
        mAdapter = btAdapter;
        mCompanionTracker = companionTracker;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        context.registerReceiver(mReceiver, intentFilter);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Ignore intents until companion pairing is done.
            if (mCompanionTracker.getCompanion() == null) {
                return;
            }

            // Listen on scan mode and bond state changes to ensure the connectability
            // of the Wear device for classic peripherals, specifically for the following
            // cases:
            //
            // 1) A component has disabled scan mode not knowing connectability
            //    is required by a bonded classic peripheral, re-enable scan mode.
            // 2) After a peripheral is bonded, evaluate and enable scan mode if necessary.
            // 3) After a peripheral is unbonded, evaluate and disable scan mode if necessary.
            //
            if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                int scanMode = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                if (scanMode == BluetoothAdapter.SCAN_MODE_NONE
                        && getBondedClassicPeripheralCount() > 0) {
                    mAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int bondState = intent.getIntExtra(
                        BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                if (bondState == BluetoothDevice.BOND_BONDED
                        && getBondedClassicPeripheralCount() > 0) {
                    mAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
                } else if (bondState == BluetoothDevice.BOND_NONE
                        && getBondedClassicPeripheralCount() <= 0) {
                    // TODO: bring back once this class knows what devices are connected or not
                    // (not just bonded)
                    // mAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_NONE);
                }
            }
        }
    };

    private int getBondedClassicPeripheralCount() {
        int bondedClassicPeripherals = 0;

        // Look for all classic devices.
        final Set<BluetoothDevice> bondedDevices = mAdapter.getBondedDevices();

        for (final BluetoothDevice device : bondedDevices) {
            int deviceType = device.getType();
            if (!device.equals(mCompanionTracker.getCompanion())
                    && (deviceType == BluetoothDevice.DEVICE_TYPE_CLASSIC
                    || deviceType == BluetoothDevice.DEVICE_TYPE_DUAL)) {
                bondedClassicPeripherals++;
            }
        }

        if (mAdapter.isEnabled() && bondedClassicPeripherals < 0) {
            Log.w(TAG, "unexpected number of classic bonded devices: " + bondedClassicPeripherals);
        }

        return bondedClassicPeripherals;
    }

    public void dump(IndentingPrintWriter ipw) {
        ipw.println("======== BluetoothScanModeEnforcer ========");
        if (mAdapter.isEnabled()) {
            final int count = getBondedClassicPeripheralCount();
            ipw.println("Bluetooth Peripherals Count: " + count);
            ipw.println("Current mode should be : " + (count <= 0 ? "NONE" : "CONNECTABLE"));
        } else {
            ipw.println("Bluetooth adapter disabled");
        }
        ipw.println();
    }
}
