package com.android.clockwork.bluetooth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.MockBluetoothProxyHelper;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.ParcelFileDescriptor;
import android.os.ParcelUuid;

import com.android.clockwork.bluetooth.proxy.ProxyServiceHelper;
import com.android.clockwork.bluetooth.proxy.WearProxyConstants;
import com.android.internal.util.IndentingPrintWriter;

import com.google.android.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPackageManager;

import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;

/** Test for {@link CompanionProxyShard} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class})
public class CompanionProxyShardTest {
    final ShadowApplication mShadowApplication = ShadowApplication.getInstance();

    private static final int INSTANCE = -1;
    private static final int FD = 2;
    private static final int NETWORK_SCORE = 123;
    private static final int NETWORK_SCORE2 = 456;
    private static final int DISCONNECT_STATUS = 789;
    private static final int CONNECTION_PORT = 0;

    private static final int WHAT_START_SYSPROXY = 1;
    private static final int WHAT_JNI_ACTIVE_NETWORK_STATE = 2;
    private static final int WHAT_JNI_DISCONNECTED = 3;
    private static final int WHAT_RESET_CONNECTION = 4;

    private static final boolean CONNECTED = true;
    private static final boolean DISCONNECTED = !CONNECTED;
    private static final boolean WITH_INTERNET = true;
    private static final boolean NO_INTERNET = !WITH_INTERNET;

    private static final int INVALID_NETWORK_TYPE = ConnectivityManager.TYPE_NONE;

    @Mock BluetoothAdapter mMockBluetoothAdapter;
    @Mock CompanionTracker mMockCompanionTracker;
    @Mock BluetoothDevice mBluetoothDevice;
    @Mock IndentingPrintWriter mMockIndentingPrintWriter;
    @Mock ParcelFileDescriptor mMockParcelFileDescriptor;
    @Mock ProxyServiceHelper mMockProxyServiceHelper;
    @Mock CompanionProxyShard.Listener mMockCompanionProxyShardListener;
    @Mock BluetoothSocketMonitor mMockBluetoothSocketMonitor;

    private Context mContext;
    private CompanionProxyShardTestClass mCompanionProxyShard;
    private MockBluetoothProxyHelper mBluetoothProxyHelper;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;

        ShadowLooper.pauseMainLooper();

        when(mMockParcelFileDescriptor.detachFd()).thenReturn(FD);
        when(mMockParcelFileDescriptor.dup()).thenReturn(mMockParcelFileDescriptor);
        when(mMockParcelFileDescriptor.getFileDescriptor()).thenReturn(new FileDescriptor());
        mBluetoothProxyHelper = new MockBluetoothProxyHelper(mMockBluetoothAdapter);
        mBluetoothProxyHelper.setMockParcelFileDescriptor(mMockParcelFileDescriptor);
        when(mMockProxyServiceHelper.getNetworkScore()).thenReturn(NETWORK_SCORE);
        when(mBluetoothDevice.getUuids())
                .thenReturn(new ParcelUuid[] {WearProxyConstants.PROXY_UUID_V1});
        when(mMockCompanionTracker.getCompanion()).thenReturn(mBluetoothDevice);

        ShadowBluetoothAdapter.setAdapter(mMockBluetoothAdapter);
    }

    @Test
    public void testStartNetworkWithWifiInternet_WasDisconnected() {
        mCompanionProxyShard = createCompanionProxyShard();

        ShadowLooper.runMainLooperOneTask(); // WHAT_START_SYSPROXY
        ShadowLooper.runMainLooperOneTask(); // ASYNC TASK RFCOMM SOCKET
        verify(mMockParcelFileDescriptor).detachFd();

        ShadowLooper.runMainLooperOneTask(); // ASYNC TASK SYSPROXY DELIVER
        assertEquals(1, mCompanionProxyShard.mConnectNativeCount);

        // Simulate JNI callback
        mCompanionProxyShard.simulateJniCallbackConnect(ConnectivityManager.TYPE_WIFI, false);

        ShadowLooper.runMainLooperOneTask(); // WHAT_JNI_ACTIVE_NETWORK_STATE

        verify(mMockCompanionProxyShardListener)
                .onProxyConnectionChange(CONNECTED, NETWORK_SCORE, WITH_INTERNET);
        verify(mMockProxyServiceHelper).startNetworkSession(anyString(), any());

        ensureMessageQueueEmpty();
    }

    @Test
    public void testStartNetworkNoInternet_WasDisconnected() {
        mCompanionProxyShard = createCompanionProxyShard();

        ShadowLooper.runMainLooperOneTask(); // WHAT_START_SYSPROXY
        ShadowLooper.runMainLooperOneTask(); // ASYNC TASK RFCOMM SOCKET
        verify(mMockParcelFileDescriptor).detachFd();

        ShadowLooper.runMainLooperOneTask(); // ASYNC TASK SYSPROXY DELIVER
        assertEquals(1, mCompanionProxyShard.mConnectNativeCount);

        // Simulate JNI callback
        mCompanionProxyShard.simulateJniCallbackConnect(INVALID_NETWORK_TYPE, false);

        ShadowLooper.runMainLooperOneTask(); // WHAT_JNI_ACTIVE_NETWORK_STATE

        verify(mMockCompanionProxyShardListener)
                .onProxyConnectionChange(CONNECTED, NETWORK_SCORE, NO_INTERNET);
        verify(mMockProxyServiceHelper).stopNetworkSession(anyString());

        ensureMessageQueueEmpty();
    }

    @Test
    public void testStartNetwork_WasConnectedWithWifiInternet() throws Exception {
        connectNetworkWithWifiInternet();
        verify(mMockCompanionProxyShardListener)
                .onProxyConnectionChange(CONNECTED, NETWORK_SCORE, WITH_INTERNET);

        List<InetAddress> dnsServers = Lists.newArrayList(InetAddress.getByName("1.2.3.4"));
        mCompanionProxyShard.startNetwork(
                NETWORK_SCORE, dnsServers, CONNECTION_PORT, mMockCompanionProxyShardListener);
        ShadowLooper.runMainLooperOneTask(); // ASYNC TASK SYSPROXY DELIVER

        verify(mMockCompanionProxyShardListener, times(2))
                .onProxyConnectionChange(CONNECTED, NETWORK_SCORE, WITH_INTERNET);
        verify(mMockProxyServiceHelper).setDnsServers(dnsServers);
        verify(mMockProxyServiceHelper, times(2)).startNetworkSession(anyString(), any());

        ensureMessageQueueEmpty();
    }

    @Test
    public void testStartNetwork_WasConnectedNoInternet() throws Exception {
        connectNetworkNoInternet();
        verify(mMockCompanionProxyShardListener)
                .onProxyConnectionChange(CONNECTED, NETWORK_SCORE, NO_INTERNET);

        List<InetAddress> dnsServers = Lists.newArrayList(InetAddress.getByName("1.2.3.4"));
        mCompanionProxyShard.startNetwork(
                NETWORK_SCORE, dnsServers, CONNECTION_PORT, mMockCompanionProxyShardListener);
        ShadowLooper.runMainLooperOneTask(); // WHAT_START_SYSPROXY

        verify(mMockCompanionProxyShardListener, times(2))
                .onProxyConnectionChange(CONNECTED, NETWORK_SCORE, NO_INTERNET);
        verify(mMockProxyServiceHelper).setDnsServers(dnsServers);
        verify(mMockProxyServiceHelper, times(2)).stopNetworkSession(anyString());

        ensureMessageQueueEmpty();
    }

    @Test
    public void testStartNetwork_AdapterIsNull() {
        // Force bluetooth adapter to return null
        ShadowBluetoothAdapter.forceNull = true;

        mCompanionProxyShard = createCompanionProxyShard();
        ShadowLooper.runMainLooperOneTask(); // WHAT_START_SYSPROXY
        ShadowLooper.runMainLooperOneTask(); // ASYNC TASK RFCOMM SOCKET

        verify(mMockParcelFileDescriptor, never()).detachFd();
        // Restore bluetooth adapter to return a valid instance
        ShadowBluetoothAdapter.forceNull = false;
    }

    @Test
    public void testStartNetwork_NullParcelFileDescriptor() {
        mBluetoothProxyHelper.setMockParcelFileDescriptor(null);

        mCompanionProxyShard = createCompanionProxyShard();
        ShadowLooper.runMainLooperOneTask(); // WHAT_START_SYSPROXY
        ShadowLooper.runMainLooperOneTask(); // ASYNC TASK RFCOMM SOCKET

        // Simulate JNI callback
        assertEquals(0, mCompanionProxyShard.mConnectNativeCount);

        assertTrue(mCompanionProxyShard.mHandler.hasMessages(WHAT_RESET_CONNECTION));
    }

    @Test
    public void testStartNetwork_BluetoothServiceIsNull() {
        mBluetoothProxyHelper.setBluetoothService(null);

        mCompanionProxyShard = createCompanionProxyShard();

        ShadowLooper.runMainLooperOneTask(); // WHAT_START_SYSPROXY
        ShadowLooper.runMainLooperOneTask(); // ASYNC TASK RFCOMM SOCKET

        verify(mMockParcelFileDescriptor, never()).detachFd();
        assertTrue(mCompanionProxyShard.mHandler.hasMessages(WHAT_RESET_CONNECTION));
    }

    @Test
    public void testStartNetwork_BleCompanion() {
        when(mMockCompanionTracker.isCompanionBle()).thenReturn(true);

        mCompanionProxyShard = createCompanionProxyShard();

        ShadowLooper.runMainLooperOneTask(); // WHAT_START_SYSPROXY
        ShadowLooper.runMainLooperOneTask(); // ASYNC TASK RFCOMM SOCKET
        verify(mMockBluetoothSocketMonitor).start(mMockParcelFileDescriptor);

        mCompanionProxyShard.mBluetoothSocketMonitorListener.onBluetoothData();
        ShadowLooper.idleMainLooper(); // Listener call is posted to the main looper.
        verify(mMockCompanionProxyShardListener).onProxyBleData();
    }

    @Test
    public void testUpdateNetwork_ConnectedWithWifiInternet() {
        connectNetworkWithWifiInternet();
        verify(mMockCompanionProxyShardListener)
                .onProxyConnectionChange(CONNECTED, NETWORK_SCORE, WITH_INTERNET);

        when(mMockProxyServiceHelper.getNetworkScore()).thenReturn(NETWORK_SCORE2);
        mCompanionProxyShard.updateNetwork(NETWORK_SCORE2);

        verify(mMockProxyServiceHelper).setNetworkScore(NETWORK_SCORE);
        verify(mMockCompanionProxyShardListener)
                .onProxyConnectionChange(CONNECTED, NETWORK_SCORE2, WITH_INTERNET);
    }

    @Test
    public void testUpdateNetwork_ConnectedNoInternet() {
        connectNetworkNoInternet();

        when(mMockProxyServiceHelper.getNetworkScore()).thenReturn(NETWORK_SCORE2);
        mCompanionProxyShard.updateNetwork(NETWORK_SCORE2);

        verify(mMockProxyServiceHelper).setNetworkScore(NETWORK_SCORE2);
        verify(mMockCompanionProxyShardListener)
                .onProxyConnectionChange(CONNECTED, NETWORK_SCORE2, NO_INTERNET);
    }

    @Test
    public void testUpdateNetwork_Disconnected() {
        when(mMockProxyServiceHelper.getNetworkScore()).thenReturn(NETWORK_SCORE2);
        mCompanionProxyShard = createCompanionProxyShard();
        mCompanionProxyShard.updateNetwork(NETWORK_SCORE2);

        verify(mMockProxyServiceHelper).setNetworkScore(NETWORK_SCORE2);
        verify(mMockCompanionProxyShardListener)
                .onProxyConnectionChange(DISCONNECTED, NETWORK_SCORE2, false);
    }

    @Test
    public void testUpdateNetwork_UpdateDnsServers() throws Exception {
        List<InetAddress> dnsServers =
                Lists.newArrayList(
                        InetAddress.getByName("9.8.7.6"), InetAddress.getByName("2.2.2.2"));
        mCompanionProxyShard = createCompanionProxyShard();
        mCompanionProxyShard.updateNetwork(dnsServers);

        verify(mMockProxyServiceHelper).setDnsServers(dnsServers);
    }

    @Test
    public void testWifiToCell() {
        connectNetworkWithWifiInternet();

        mCompanionProxyShard.simulateJniCallbackConnect(ConnectivityManager.TYPE_MOBILE, true);
        ShadowLooper.runMainLooperOneTask(); // WHAT_JNI_ACTIVE_NETWORK_STATE

        assertEquals(ConnectivityManager.TYPE_MOBILE, mCompanionProxyShard.mNetworkType);
        verify(mMockCompanionProxyShardListener, times(2))
                .onProxyConnectionChange(CONNECTED, NETWORK_SCORE, WITH_INTERNET);
        verify(mMockProxyServiceHelper).setMetered(false);
        verify(mMockProxyServiceHelper).setMetered(true);
    }

    @Test
    public void testCellToWifi() {
        connectNetworkWithCellInternet();

        mCompanionProxyShard.simulateJniCallbackConnect(ConnectivityManager.TYPE_WIFI, false);
        ShadowLooper.runMainLooperOneTask(); // WHAT_JNI_ACTIVE_NETWORK_STATE

        assertEquals(ConnectivityManager.TYPE_WIFI, mCompanionProxyShard.mNetworkType);
        verify(mMockCompanionProxyShardListener, times(2))
                .onProxyConnectionChange(CONNECTED, NETWORK_SCORE, WITH_INTERNET);
        verify(mMockProxyServiceHelper).setMetered(true);
        verify(mMockProxyServiceHelper).setMetered(false);
    }

    @Test
    public void testJniActiveNetworkState_AlreadyClosed() {
        connectNetworkWithWifiInternet();

        mCompanionProxyShard.mIsShardStarted = false;
        mCompanionProxyShard.simulateJniCallbackConnect(ConnectivityManager.TYPE_WIFI, true);
        ShadowLooper.runMainLooperOneTask(); // WHAT_JNI_ACTIVE_NETWORK_STATE

        ensureMessageQueueEmpty();
    }

    @Test
    public void testJniActiveNetworkState_ConnectedPhoneWithCell() {
        mCompanionProxyShard = createCompanionProxyShard();

        ShadowLooper.runMainLooperOneTask(); // WHAT_START_SYSPROXY
        ShadowLooper.runMainLooperOneTask(); // ASYNC TASK RFCOMM SOCKET
        ShadowLooper.runMainLooperOneTask(); // ASYNC TASK SYSPROXY DELIVER

        mCompanionProxyShard.simulateJniCallbackConnect(ConnectivityManager.TYPE_MOBILE, true);
        ShadowLooper.runMainLooperOneTask(); // WHAT_JNI_ACTIVE_NETWORK_STATE

        verify(mMockProxyServiceHelper).startNetworkSession(anyString(), any());
        verify(mMockCompanionProxyShardListener).onProxyConnectionChange(true, 123, true);
        verify(mMockProxyServiceHelper).setMetered(true);
        ensureMessageQueueEmpty();
    }

    @Test
    public void testJniActiveNetworkState_ConnectedPhoneNoInternet() {
        mCompanionProxyShard = createCompanionProxyShard();

        ShadowLooper.runMainLooperOneTask(); // WHAT_START_SYSPROXY
        ShadowLooper.runMainLooperOneTask(); // ASYNC TASK RFCOMM SOCKET
        ShadowLooper.runMainLooperOneTask(); // ASYNC TASK SYSPROXY DELIVER

        mCompanionProxyShard.simulateJniCallbackConnect(INVALID_NETWORK_TYPE, true);
        ShadowLooper.runMainLooperOneTask(); // WHAT_JNI_ACTIVE_NETWORK_STATE

        verify(mMockProxyServiceHelper).stopNetworkSession(anyString());
        verify(mMockCompanionProxyShardListener)
                .onProxyConnectionChange(CONNECTED, NETWORK_SCORE, NO_INTERNET);

        ensureMessageQueueEmpty();
    }

    @Test
    public void testJniDisconnect_NotClosed() {
        mCompanionProxyShard = createCompanionProxyShard();

        ShadowLooper.runMainLooperOneTask(); // WHAT_START_SYSPROXY
        ShadowLooper.runMainLooperOneTask(); // ASYNC TASK RFCOMM SOCKET
        ShadowLooper.runMainLooperOneTask(); // ASYNC TASK SYSPROXY DELIVER

        mCompanionProxyShard.simulateJniCallbackDisconnect(-1);
        ShadowLooper.runMainLooperOneTask(); // WHAT_JNI_DISCONNECT

        verify(mMockCompanionProxyShardListener)
                .onProxyConnectionChange(DISCONNECTED, NETWORK_SCORE, false);
        verify(mMockProxyServiceHelper).stopNetworkSession(anyString());

        assertTrue(mCompanionProxyShard.mHandler.hasMessages(WHAT_START_SYSPROXY));
    }

    @Test
    public void testJniDisconnect_Closed() {
        mCompanionProxyShard = createCompanionProxyShard();

        ShadowLooper.runMainLooperOneTask(); // WHAT_START_SYSPROXY
        ShadowLooper.runMainLooperOneTask(); // ASYNC TASK RFCOMM SOCKET
        ShadowLooper.runMainLooperOneTask(); // ASYNC TASK SYSPROXY DELIVER

        mCompanionProxyShard.mIsShardStarted = false;

        mCompanionProxyShard.simulateJniCallbackDisconnect(-1);
        ShadowLooper.runMainLooperOneTask(); // WHAT_JNI_DISCONNECT

        ensureMessageQueueEmpty();
    }

    @Test
    public void testClose_WasConnectedWithWifiInternet() {
        connectNetworkWithWifiInternet();

        mCompanionProxyShard.stop();

        verify(mMockProxyServiceHelper).stopNetworkSession(anyString());
        verify(mMockCompanionProxyShardListener)
                .onProxyConnectionChange(DISCONNECTED, NETWORK_SCORE, false);
    }

    @Test
    public void testDump() {
        mCompanionProxyShard = createCompanionProxyShard();

        mCompanionProxyShard.dump(mMockIndentingPrintWriter);
        verify(mMockIndentingPrintWriter).increaseIndent();
        verify(mMockIndentingPrintWriter).decreaseIndent();
    }

    // Create the companion proxy shard to be used in the tests.
    // The class abstracts away dependencies on difficult framework methods and fields.
    private CompanionProxyShardTestClass createCompanionProxyShard(int expectedSysproxyVersion) {
        CompanionProxyShardTestClass companionProxyShard =
                new CompanionProxyShardTestClass(
                        mContext,
                        mMockProxyServiceHelper,
                        mMockCompanionTracker,
                        expectedSysproxyVersion);

        companionProxyShard.startNetwork(
                NETWORK_SCORE,
                Lists.newArrayList(),
                CONNECTION_PORT,
                mMockCompanionProxyShardListener);
        return companionProxyShard;
    }

    private CompanionProxyShardTestClass createCompanionProxyShard() {
        return createCompanionProxyShard(1);
    }

    private void ensureMessageQueueEmpty() {
        for (int i = WHAT_START_SYSPROXY; i <= WHAT_RESET_CONNECTION; i++) {
            assertFalse(mCompanionProxyShard.mHandler.hasMessages(i));
        }
    }

    private void connectNetworkWithWifiInternet() {
        doStartNetwork(1, ConnectivityManager.TYPE_WIFI, false);
        assertEquals(ConnectivityManager.TYPE_WIFI, mCompanionProxyShard.mNetworkType);
    }

    private void connectNetworkWithCellInternet() {
        doStartNetwork(1, ConnectivityManager.TYPE_MOBILE, true);
        assertEquals(ConnectivityManager.TYPE_MOBILE, mCompanionProxyShard.mNetworkType);
    }

    private void connectNetworkNoInternet() {
        doStartNetwork(1, INVALID_NETWORK_TYPE, false);
    }

    private void doStartNetwork(int expectedSysproxyVersion, int networkType, boolean metered) {
        mCompanionProxyShard = createCompanionProxyShard(expectedSysproxyVersion);
        assertTrue(mCompanionProxyShard.mHandler.hasMessages(WHAT_START_SYSPROXY));

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Simulate JNI callback
        mCompanionProxyShard.simulateJniCallbackConnect(networkType, metered);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertEquals(1, mCompanionProxyShard.mConnectNativeCount);
    }

    private class CompanionProxyShardTestClass extends CompanionProxyShard {
        final int mExpectedSysproxyVersion;
        int mConnectNativeCount;
        int mDisconnectNativeCount;
        int mUnregisterCount;
        /** The listener of the last socket monitor created or null if none created yet. */
        BluetoothSocketMonitor.Listener mBluetoothSocketMonitorListener;

        int mConnectReturnValue = 0;
        boolean mDisconnectReturnValue = true;

        CompanionProxyShardTestClass(
                final Context context,
                final ProxyServiceHelper proxyServiceHelper,
                final CompanionTracker companionTracker,
                int expectedSysproxyVersion) {
            super(context, proxyServiceHelper, companionTracker);
            this.mExpectedSysproxyVersion = expectedSysproxyVersion;
        }

        @Override
        protected int connectNative(int fd, int sysproxyVersion) {
            mConnectNativeCount += 1;
            assertEquals(mExpectedSysproxyVersion, sysproxyVersion);
            return mConnectReturnValue;
        }

        @Override
        protected int continueConnectNative() {
            mConnectNativeCount += 1;
            return mConnectReturnValue;
        }

        void simulateJniCallbackConnect(int networkType, boolean isMetered) {
            super.onActiveNetworkState(networkType, isMetered);
        }

        @Override
        protected boolean disconnectNative() {
            mDisconnectNativeCount += 1;
            return mDisconnectReturnValue;
        }

        @Override
        protected BluetoothSocketMonitor createBluetoothSocketMonitor(
                BluetoothSocketMonitor.Listener listener) {
            mBluetoothSocketMonitorListener = listener;
            return mMockBluetoothSocketMonitor;
        }

        void simulateJniCallbackDisconnect(int status) {
            super.onDisconnect(status);
        }
    }

    private void setWaitingForAsyncDisconnectResponse(final boolean isWaiting) {
        try {
            Field field =
                    CompanionProxyShard.class.getDeclaredField(
                            "sWaitingForAsyncDisconnectResponse");
            field.setAccessible(true);
            field.set(null, isWaiting);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            fail();
        }
    }

    private boolean getWaitingForAsyncDisconnectResponse() {
        boolean isWaiting = false;
        try {
            Field field =
                    CompanionProxyShard.class.getDeclaredField(
                            "sWaitingForAsyncDisconnectResponse");
            field.setAccessible(true);
            isWaiting = field.getBoolean(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            fail();
        }
        return isWaiting;
    }

    public static <InetAddress> boolean listEqualsIgnoreOrder(
            List<InetAddress> list1, List<InetAddress> list2) {
        return new HashSet<>(list1).equals(new HashSet<>(list2));
    }
}
