package com.android.networkstack.tethering.companionproxy.protocol;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import com.android.networkstack.tethering.companionproxy.util.CircularByteBuffer;

@RunWith(RobolectricTestRunner.class)
public class NetUtilsTest {
    @Test
    public void decodeNetworkUnsignedInt16() {
        final byte[] data = new byte[4];
        data[0] = (byte) 0xFF;
        data[1] = (byte) 1;
        data[2] = (byte) 2;
        data[3] = (byte) 0xFF;

        assertEquals(0x0102, NetUtils.decodeNetworkUnsignedInt16(data, 1));

        CircularByteBuffer buffer = new CircularByteBuffer(100);
        buffer.writeBytes(data, 0, data.length);

        assertEquals(0x0102, NetUtils.decodeNetworkUnsignedInt16(buffer, 1));
    }

    @Test
    public void encodeNetworkUnsignedInt16() {
        final byte[] data = new byte[4];
        data[0] = (byte) 0xFF;
        data[3] = (byte) 0xFF;
        NetUtils.encodeNetworkUnsignedInt16(0x0102, data, 1);

        assertEquals((byte) 0xFF, data[0]);
        assertEquals((byte) 1, data[1]);
        assertEquals((byte) 2, data[2]);
        assertEquals((byte) 0xFF, data[3]);
    }
}
