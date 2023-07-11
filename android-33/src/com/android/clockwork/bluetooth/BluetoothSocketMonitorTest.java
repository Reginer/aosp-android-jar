package com.android.clockwork.bluetooth;

import static org.mockito.Mockito.verify;

import android.os.ParcelFileDescriptor;
import android.system.OsConstants;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Test for {@link BluetoothSocketMonitor}
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowParcelFileDescriptor.class, ShadowOs.class})
public class BluetoothSocketMonitorTest {

    // Specifies a timeout to fail early in case tests get stuck while doing IO.
    @Rule
    public Timeout testTimeout = Timeout.seconds(10);

    @Mock
    BluetoothSocketMonitor.Listener mMockListener;

    private BluetoothSocketMonitor mSocketMonitor;
    /**
     * Used to simulate data written and read by the bluetoothd process.
     */
    private ParcelFileDescriptor mBluetoothRemoteFd;
    /**
     * Used to simulate data written and read by the sysproxy process.
     */
    private ParcelFileDescriptor mSysproxyRemoteFd;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mSocketMonitor = new BluetoothSocketMonitor(mMockListener);
        ParcelFileDescriptor[] fds = ParcelFileDescriptor.createSocketPair(
                OsConstants.SOCK_SEQPACKET);
        ParcelFileDescriptor bluetoothLocalFd = fds[0];
        mBluetoothRemoteFd = fds[1];
        mSysproxyRemoteFd = mSocketMonitor.start(bluetoothLocalFd);
    }

    @After
    public void tearDown() throws Exception {
        // To ensure tests don't forget to close their file descriptors.
        blockUntilClosed(mBluetoothRemoteFd);
        blockUntilClosed(mSysproxyRemoteFd);
    }

    @Test
    public void testForwardsDataWrittenOnBluetoothFdToSysproxyFd() throws Exception {
        // Simulates Bluetooth process writing data.
        writeString(mBluetoothRemoteFd, "test");
        writeString(mBluetoothRemoteFd, "");
        writeString(mBluetoothRemoteFd, "_data");

        String expectedData = "test_data";
        Assert.assertEquals(readAsString(mSysproxyRemoteFd, expectedData.length()), expectedData);

        expectedData = "again";
        writeString(mBluetoothRemoteFd, expectedData);
        Assert.assertEquals(readAsString(mSysproxyRemoteFd, expectedData.length()), expectedData);

        mBluetoothRemoteFd.close();
    }

    @Test
    public void testForwardsDataWrittenOnSysproxyFdToBluetoothFd() throws Exception {
        // Simulates sysproxy process writing data.
        writeString(mSysproxyRemoteFd, "test");
        writeString(mSysproxyRemoteFd, "");
        writeString(mSysproxyRemoteFd, "_data");

        String expectedData = "test_data";
        Assert.assertEquals(readAsString(mBluetoothRemoteFd, expectedData.length()), expectedData);

        expectedData = "again";
        writeString(mSysproxyRemoteFd, expectedData);
        Assert.assertEquals(readAsString(mBluetoothRemoteFd, expectedData.length()), expectedData);

        mBluetoothRemoteFd.close();
    }

    @Test
    public void testForwardsDataSimultaneouslyOnBothDirections() throws Exception {
        String request = "tcp_request1";
        String response = "tcp_response1";

        writeString(mSysproxyRemoteFd, request);
        writeString(mBluetoothRemoteFd, response);

        Assert.assertEquals(readAsString(mSysproxyRemoteFd, response.length()), response);
        Assert.assertEquals(readAsString(mBluetoothRemoteFd, request.length()), request);

        request = "tcp_request2";
        response = "tcp_response2";
        writeString(mSysproxyRemoteFd, request);
        writeString(mBluetoothRemoteFd, response);

        Assert.assertEquals(readAsString(mSysproxyRemoteFd, response.length()), response);
        Assert.assertEquals(readAsString(mBluetoothRemoteFd, request.length()), request);

        mBluetoothRemoteFd.close();
    }

    @Test
    public void testNotifiesListenerWhenSysproxySendsDataToBluetooth() throws Exception {
        writeString(mSysproxyRemoteFd, "test");

        // Waits until part of the written data makes it to the Bluetooth descriptor.
        readAsString(mBluetoothRemoteFd, 1);
        verify(mMockListener).onBluetoothData();

        mBluetoothRemoteFd.close();
    }

    @Test
    public void testNotifiesListenerWhenSysproxyReceivesDataOnBluetooth() throws Exception {
        writeString(mBluetoothRemoteFd, "test");

        // Waits until part of the written data makes it to the sysproxy descriptor.
        readAsString(mSysproxyRemoteFd, 1);
        verify(mMockListener).onBluetoothData();

        mBluetoothRemoteFd.close();
    }

    @Test
    public void testClosesSysproxyFdWhenBluetoothFdIsClosed() throws Exception {
        writeString(mBluetoothRemoteFd, "test_data");
        mBluetoothRemoteFd.close();

        blockUntilClosed(mSysproxyRemoteFd);
    }

    @Test
    public void testClosesBluetoothFdWhenSysproxyFdIsClosed() throws Exception {
        writeString(mSysproxyRemoteFd, "test_data");
        mSysproxyRemoteFd.close();

        blockUntilClosed(mBluetoothRemoteFd);
    }

    /**
     * Writes the given string to the file descriptor using UTF-8 encdoing.
     */
    private void writeString(ParcelFileDescriptor fd, String value) throws IOException {
        ShadowParcelFileDescriptor shadowFd = Shadow.extract(fd);
        shadowFd.getOutputStream().write(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns the content of the given input stream as a string assuming UTF-8 encoding.
     *
     * <p>Blocks until either EOF is reached or {@code maxBytes} is read.
     */
    private String readAsString(ParcelFileDescriptor fd, int maxBytes) throws IOException {
        ShadowParcelFileDescriptor shadowFd = Shadow.extract(fd);
        return new String(readFully(shadowFd.getInputStream(), maxBytes), StandardCharsets.UTF_8);
    }

    /**
     * Blocks until the given file descriptor is closed.
     */
    private void blockUntilClosed(ParcelFileDescriptor fd) throws IOException {
        try {
            // The tests never write 1000 bytes so this only finishes blocking if descriptor is
            // closed.
            readAsString(fd, 1000);
        } catch (IOException e) {
            // Probably already closed. Nothing to do.
        }
    }

    /**
     * Returns the content of the given input stream as a byte array.
     *
     * <p>Blocks until either EOF is reached or {@code maxBytes} is read.
     */
    private byte[] readFully(InputStream inputStream, int maxBytes) throws IOException {
        byte[] buffer = new byte[maxBytes];
        int position = 0;
        while (position < buffer.length) {
            int readCount = inputStream.read(buffer, position, buffer.length - position);
            if (readCount <= 0) {
                break;
            }
            position += readCount;
        }
        return Arrays.copyOfRange(buffer, 0, position);
    }
}
