package com.android.clockwork.connectivity;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkRequest;
import android.os.Message;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class WearNetworkObserverTest {

    // TODO(b/268443329): Add high bandwidth and cellular requests tests.

    final ShadowApplication mShadowApplication = ShadowApplication.getInstance();

    @Mock ConnectivityManager.NetworkCallback mMockNetworkCallback;
    @Mock WearNetworkObserver.Listener mMockListener;
    @Mock WearConnectivityPackageManager mMockWearConnectivityPackageManager;

    private WearNetworkObserver mObserver;
    private int mNextId = 10000;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mObserver =
                new WearNetworkObserver(
                        RuntimeEnvironment.application,
                        mMockWearConnectivityPackageManager,
                        mMockListener);
    }

    @Test
    public void testUnmeteredRequests() {
        NetworkRequest request1 =
                new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                        .build();
        mObserver.needNetworkFor(request1);
        verify(mMockListener).onUnmeteredRequestsChanged(1);
        reset(mMockListener);

        NetworkRequest request2 =
                new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();
        mObserver.needNetworkFor(request2);
        verify(mMockListener).onUnmeteredRequestsChanged(2);
        reset(mMockListener);

        mObserver.releaseNetworkFor(request1);
        verify(mMockListener).onUnmeteredRequestsChanged(1);
        reset(mMockListener);

        mObserver.releaseNetworkFor(request2);
        verify(mMockListener).onUnmeteredRequestsChanged(0);

        verifyNoMoreInteractions(mMockWearConnectivityPackageManager);
    }

    @Test
    public void testWifiTransportRequests() {
        NetworkRequest request1 =
                new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build();
        mObserver.needNetworkFor(request1);
        verify(mMockListener).onWifiRequestsChanged(1);
        reset(mMockListener);

        NetworkRequest request2 =
                new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();
        mObserver.needNetworkFor(request2);
        verify(mMockListener).onWifiRequestsChanged(2);
        reset(mMockListener);

        mObserver.releaseNetworkFor(request1);
        verify(mMockListener).onWifiRequestsChanged(1);
        reset(mMockListener);

        mObserver.releaseNetworkFor(request2);
        verify(mMockListener).onWifiRequestsChanged(0);

        verifyNoMoreInteractions(mMockWearConnectivityPackageManager);
    }
}
