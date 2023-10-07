package android.bluetooth;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.when;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Mock {@link BluetoothAdapter} in the same package to access package private methods */
public class MockBluetoothProxyHelper {
    @Mock IBluetooth mMockBluetoothService;
    @Mock IBluetoothSocketManager mMockBluetoothSocketManager;
    @Mock ParcelFileDescriptor mMockParcelFileDescriptor;

    private BluetoothAdapter mMockBluetoothAdapter;

    public MockBluetoothProxyHelper(BluetoothAdapter mockBluetoothAdapter) {
        this.mMockBluetoothAdapter = mockBluetoothAdapter;

        MockitoAnnotations.initMocks(this);

        /** Mocks out package protected method */
        when(mockBluetoothAdapter.getBluetoothService(any())).thenReturn(mMockBluetoothService);

        /** IBluetooth package protected method */
        try {
            when(mMockBluetoothService.getSocketManager()).thenReturn(mMockBluetoothSocketManager);
        } catch (RemoteException e) {
            fail(e.getMessage());
        }

        /** IBluetooth package protected method */
        try {
            when(mMockBluetoothSocketManager.connectSocket(
                            any(), anyInt(), any(), anyInt(), anyInt()))
                    .thenReturn(mMockParcelFileDescriptor);
        } catch (RemoteException e) {
            fail(e.getMessage());
        }
    }

    public void setBluetoothService(IBluetooth bluetoothProxyService) {
        when(mMockBluetoothAdapter.getBluetoothService(any())).thenReturn(bluetoothProxyService);
    }

    public void setBluetoothSocketManager(IBluetoothSocketManager bluetoothSocketManager) {
        try {
            when(mMockBluetoothService.getSocketManager()).thenReturn(bluetoothSocketManager);
        } catch (RemoteException e) {
            fail(e.getMessage());
        }
    }

    public void setMockParcelFileDescriptor(ParcelFileDescriptor parcelFileDescriptor) {
        try {
            when(mMockBluetoothSocketManager.connectSocket(
                            any(), anyInt(), any(), anyInt(), anyInt()))
                    .thenReturn(parcelFileDescriptor);
        } catch (RemoteException e) {
            fail(e.getMessage());
        }
    }
}
