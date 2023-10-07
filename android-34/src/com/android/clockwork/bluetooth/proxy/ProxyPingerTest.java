package com.android.clockwork.bluetooth.proxy;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.os.SystemClock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/**
 * Test for {@link ProxyPinger}
 */
@RunWith(RobolectricTestRunner.class)
public class ProxyPingerTest {

    @Mock
    ProxyGattServer mMockGattServer;
    private ProxyPinger mProxyPinger;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mProxyPinger = new ProxyPinger(mMockGattServer);
    }

    @Test
    public void testPingsOnFirstOutgoingData() {
        mProxyPinger.pingIfNeeded();
        verify(mMockGattServer).sendPing();
    }

    @Test
    public void testPingsOnlyOnce() {
        mProxyPinger.setMinPingIntervalMs(1000);
        mProxyPinger.pingIfNeeded();
        mProxyPinger.pingIfNeeded();
        mProxyPinger.pingIfNeeded();
        mProxyPinger.pingIfNeeded();
        verify(mMockGattServer, times(1)).sendPing();
    }

    @Test
    public void testPingsAgainIfMinIntervalPassed() throws Exception {
        mProxyPinger.setMinPingIntervalMs(1000);
        mProxyPinger.pingIfNeeded();

        SystemClock.sleep(2000);  // Advances the shadow clock without actually sleeping.
        mProxyPinger.pingIfNeeded();
        verify(mMockGattServer, times(2)).sendPing();
    }

    @Test
    public void testPingsAgainIfIntervalIsReset() throws Exception {
        mProxyPinger.setMinPingIntervalMs(1000);
        mProxyPinger.pingIfNeeded();

        mProxyPinger.setMinPingIntervalMs(500);
        mProxyPinger.pingIfNeeded();
        verify(mMockGattServer, times(2)).sendPing();
    }

    @Test
    public void testPingsAgainIfIntervalIsNotChecked() throws Exception {
        mProxyPinger.setMinPingIntervalMs(1000);
        mProxyPinger.pingIfNeeded();
        mProxyPinger.ping();
        verify(mMockGattServer, times(2)).sendPing();
    }

    @Test
    public void testUsesLastMinPingInterval() throws Exception {
        mProxyPinger.setMinPingIntervalMs(100000);
        mProxyPinger.pingIfNeeded();

        mProxyPinger.setMinPingIntervalMs(1000);
        mProxyPinger.pingIfNeeded();
        SystemClock.sleep(2000);  // Advances the shadow clock without actually sleeping.
        mProxyPinger.pingIfNeeded();
        verify(mMockGattServer, times(3)).sendPing();
    }
}
