package com.android.clockwork.bluetooth.proxy;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.net.NetworkCapabilities;
import android.os.RemoteException;

import com.android.internal.util.IndentingPrintWriter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/** Test for {@link ProxyNetworkFactory} */
@RunWith(RobolectricTestRunner.class)
public class ProxyNetworkFactoryTest {
    private static final int NETWORK_SCORE = 123;

    @Mock Context mockContext;
    @Mock IndentingPrintWriter mockIndentingPrintWriter;
    @Mock NetworkCapabilities mockCapabilities;

    private ProxyNetworkFactoryTestClass mProxyNetworkFactory;

    @Before
    public void setUp() throws RemoteException {
        MockitoAnnotations.initMocks(this);
        mProxyNetworkFactory = new ProxyNetworkFactoryTestClass(
                mockContext,
                mockCapabilities);
       assertEquals(1, mProxyNetworkFactory.registerMethod);
    }

    @Test
    public void testSetNetworkScore_SameScore() {
        mProxyNetworkFactory.setNetworkScore(0);
        assertEquals(0, mProxyNetworkFactory.setScoreFilterMethod);
        assertEquals(0, mProxyNetworkFactory.scoreFilter);
    }

    @Test
    public void testSetNetworkScore_GreaterScore() {
        mProxyNetworkFactory.setNetworkScore(NETWORK_SCORE);
        assertEquals(1, mProxyNetworkFactory.setScoreFilterMethod);
        assertEquals(NETWORK_SCORE, mProxyNetworkFactory.scoreFilter);
    }

    @Test
    public void testSetNetworkScore_LesserScore() {
        mProxyNetworkFactory.setNetworkScore(NETWORK_SCORE);
        mProxyNetworkFactory.setNetworkScore(0);

        assertEquals(1, mProxyNetworkFactory.setScoreFilterMethod);
        assertEquals(NETWORK_SCORE, mProxyNetworkFactory.scoreFilter);
    }

    @Test
    public void testDump() {
        mProxyNetworkFactory.dump(mockIndentingPrintWriter);
        verify(mockIndentingPrintWriter).printPair(anyString(), anyInt());
    }

    private class ProxyNetworkFactoryTestClass extends ProxyNetworkFactory {
        public int registerMethod;
        public int setScoreFilterMethod;
        public int scoreFilter;

        public ProxyNetworkFactoryTestClass(
                Context context,
                NetworkCapabilities capabilities) {
            super(context, capabilities);
        }

        @Override
        public void register() {
            registerMethod  += 1;
        }

        @Override
        public void setScoreFilter(int score) {
            scoreFilter = score;
            setScoreFilterMethod += 1;
        }
    }
}
