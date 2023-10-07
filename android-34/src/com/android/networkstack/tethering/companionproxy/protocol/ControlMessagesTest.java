package com.android.networkstack.tethering.companionproxy.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.CodedOutputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ControlMessagesTest {
    private static final int INT_5BYTES   = -1;
    private static final int INT_4BYTES   = -1 >>> 4;
    private static final int INT_3BYTES   = -1 >>> 11;
    private static final int INT_2BYTES   = -1 >>> 18;
    private static final int INT_1BYTES   = -1 >>> 25;

    @Test
    public void intSizes() {
        assertEquals(1, CodedOutputStream.computeUInt32SizeNoTag(INT_1BYTES));
        assertEquals(2, CodedOutputStream.computeUInt32SizeNoTag(INT_2BYTES));
        assertEquals(3, CodedOutputStream.computeUInt32SizeNoTag(INT_3BYTES));
        assertEquals(4, CodedOutputStream.computeUInt32SizeNoTag(INT_4BYTES));
        assertEquals(5, CodedOutputStream.computeUInt32SizeNoTag(INT_5BYTES));
    }

    @Test
    public void handshakeData() throws Exception {
        HandshakeData obj = new HandshakeData();
        obj.version = INT_5BYTES;
        obj.capabilities = INT_3BYTES;
        obj.maxRxWindowSize = INT_2BYTES;
        obj.maxTxWindowSize = INT_1BYTES;

        ProtoTestHelper.testSerialization(obj, WireProto.HandshakeData.class);
    }

    @Test
    public void linkUsageStats() throws Exception {
        LinkUsageStats obj = new LinkUsageStats();
        obj.hasBuffersAboveThreshold = false;

        ProtoTestHelper.testSerialization(obj, WireProto.LinkUsageStats.class);

        obj.hasBuffersAboveThreshold = true;

        ProtoTestHelper.testSerialization(obj, WireProto.LinkUsageStats.class);
    }

    @Test
    public void linkInfo() throws Exception {
        NetworkConfig.LinkInfo obj = new NetworkConfig.LinkInfo();
        obj.netId = INT_5BYTES;
        obj.transports = INT_3BYTES;
        obj.capabilities = 3L * Integer.MAX_VALUE;

        ProtoTestHelper.testSerialization(obj, WireProto.NetworkLinkInfo.class);
    }

    @Test
    public void linkInfo_addAndRemoveTransport() throws Exception {
        NetworkConfig.LinkInfo obj = new NetworkConfig.LinkInfo();
        obj.addTransport(18);
        obj.addCapability(45);

        assertTrue(obj.hasTransport(18));
        assertTrue(obj.hasCapability(45));
    }

    @Test
    public void networkConfig() throws Exception {
        NetworkConfig obj = new NetworkConfig();

        ProtoTestHelper.testSerialization(obj, WireProto.NetworkConfig.class);

        for (int i = 0; i < 50; i++) {
            NetworkConfig.LinkInfo link = new NetworkConfig.LinkInfo();
            link.netId = i * 1000 + 1;
            link.transports = i * 1000 + 2;
            link.capabilities = i * 1000 + 3;
            obj.links.add(link);
        }

        ProtoTestHelper.testSerialization(obj, WireProto.NetworkConfig.class);
    }
}
