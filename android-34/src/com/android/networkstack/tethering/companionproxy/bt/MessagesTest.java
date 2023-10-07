package com.android.networkstack.tethering.companionproxy.bt;

import static org.junit.Assert.assertEquals;

import com.android.networkstack.tethering.companionproxy.protocol.ProtoTestHelper;
import com.android.networkstack.tethering.companionproxy.protocol.WireProto;
import com.google.protobuf.CodedOutputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MessagesTest {
    @Test
    public void proxyConfig() throws Exception {
        ProxyConfig obj = new ProxyConfig();
        obj.psmValue = 50000;
        obj.channelChangeId = 1000;

        ProtoTestHelper.testSerialization(obj, WireProto.ProxyConfig.class);
    }

    @Test
    public void proxyConfig_maxSize() throws Exception {
        ProxyConfig obj = new ProxyConfig();
        obj.psmValue = -1;
        obj.channelChangeId = -1;

        final byte[] data = new byte[20];  // MAX_CHARACTERISTIC_VALUE_SIZE
        final CodedOutputStream out = CodedOutputStream.newInstance(data, 0, data.length);
        obj.serializeTo(out);

        // Checking the actual max size is less than MAX_CHARACTERISTIC_VALUE_SIZE, which is 20.
        assertEquals(12, out.getTotalBytesWritten());
    }
}
