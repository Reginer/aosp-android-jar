package com.android.clockwork.power;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.input.InputManager;
import android.view.InputDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/** Test for {@link InputDeviceProvider} */
@RunWith(RobolectricTestRunner.class)
public class InputDeviceProviderTest {
    private static final int TEST_SOURCE_1 = 0b10;
    private static final int TEST_SOURCE_2 = 0b11;

    private static final int TEST_DEVICE_ID_1 = 100;
    private static final int TEST_DEVICE_ID_2 = 200;

    @Mock Context mockContext;
    @Mock InputManager mockInputManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockContext.getSystemService(InputManager.class)).thenReturn(mockInputManager);
    }

    @Test
    public void testRegisterDeviceListener() {
        InputDeviceProvider provider = new InputDeviceProvider(mockContext, TEST_SOURCE_1);

        provider.startListeningForDeviceUpdates();

        verify(mockInputManager).registerInputDeviceListener(provider, null);
    }

    @Test
    public void testGetDevice_deviceAvailable() {
        setupDevices(new TestInputDevice(TEST_DEVICE_ID_1, TEST_SOURCE_1));

        InputDeviceProvider provider = new InputDeviceProvider(mockContext, TEST_SOURCE_1);

        EnablableInputDevice device = provider.getDevice();
        assertThat(device.getId()).isEqualTo(TEST_DEVICE_ID_1);
        assertThat(device == provider.getDevice()).isTrue();
    }


    @Test
    public void testGetDevice_deviceNotAvailable() {
        InputDeviceProvider provider = new InputDeviceProvider(mockContext, TEST_SOURCE_1);

        assertThat(provider.getDevice()).isNull();
    }

    @Test
    public void deviceAdded_attemptsRefreshingCachedDeviceIfNoDevicePresent() {
        InputDeviceProvider provider = new InputDeviceProvider(mockContext, TEST_SOURCE_1);

        setupDevices(new TestInputDevice(TEST_DEVICE_ID_1, TEST_SOURCE_1));
        provider.onInputDeviceAdded(TEST_DEVICE_ID_1);

        assertThat(provider.getDevice().getId()).isEqualTo(TEST_DEVICE_ID_1);
    }

    @Test
    public void deviceChanged_withSameId_attemptsRefreshingCachedDevice() {
        setupDevices(new TestInputDevice(TEST_DEVICE_ID_1, TEST_SOURCE_1));
        InputDeviceProvider provider = new InputDeviceProvider(mockContext, TEST_SOURCE_1);
        EnablableInputDevice firstDevice = provider.getDevice();

        setupDevices(new TestInputDevice(TEST_DEVICE_ID_1, TEST_SOURCE_1));
        provider.onInputDeviceChanged(TEST_DEVICE_ID_1);

        assertThat(firstDevice == provider.getDevice()).isFalse();
    }

    @Test
    public void deviceChanged_withDifferentId_doesNotAttemptRefreshingCachedDevice() {
        setupDevices(new TestInputDevice(TEST_DEVICE_ID_1, TEST_SOURCE_1));
        InputDeviceProvider provider = new InputDeviceProvider(mockContext, TEST_SOURCE_1);
        EnablableInputDevice firstDevice = provider.getDevice();

        setupDevices(new TestInputDevice(TEST_DEVICE_ID_2, TEST_SOURCE_2));
        provider.onInputDeviceChanged(TEST_DEVICE_ID_2);

        assertThat(firstDevice == provider.getDevice()).isTrue();
    }

    @Test
    public void deviceRemoved_withSameId_attemptsRefreshingCachedDevice() {
        setupDevices(new TestInputDevice(TEST_DEVICE_ID_1, TEST_SOURCE_1));
        InputDeviceProvider provider = new InputDeviceProvider(mockContext, TEST_SOURCE_1);

        when(mockInputManager.getInputDeviceIds()).thenReturn(new int[] {});
        provider.onInputDeviceChanged(TEST_DEVICE_ID_1);

        assertThat(provider.getDevice()).isNull();
    }

    @Test
    public void deviceRemoved_withDifferentId_doesNotAttemptRefreshingCachedDevice() {
        setupDevices(new TestInputDevice(TEST_DEVICE_ID_1, TEST_SOURCE_1));
        InputDeviceProvider provider = new InputDeviceProvider(mockContext, TEST_SOURCE_1);

        provider.onInputDeviceChanged(TEST_DEVICE_ID_2);

        assertThat(provider.getDevice()).isNotNull();
    }

    private void setupDevices(TestInputDevice... devices) {
        int[] deviceIds = new int[devices.length];
        int idx = 0;

        for (TestInputDevice device : devices) {
            deviceIds[idx++] = device.mId;
            InputDevice mockDevice = mock(InputDevice.class);
            when(mockDevice.getId()).thenReturn(device.mId);
            when(mockDevice.getSources()).thenReturn(device.mSource);
            when(mockInputManager.getInputDevice(device.mId)).thenReturn(mockDevice);
        }

        when(mockInputManager.getInputDeviceIds()).thenReturn(deviceIds);
    }

    private static final class TestInputDevice {
        final int mId;
        final int mSource;

        TestInputDevice(int id, int source) {
            mId = id;
            mSource = source;
        }
    }
}
