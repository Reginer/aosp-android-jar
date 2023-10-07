package com.android.networkstack.tethering.companionproxy.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.os.ParcelFileDescriptor;

import com.android.networkstack.tethering.companionproxy.io.AsyncFile;
import com.android.networkstack.tethering.companionproxy.io.BufferedFile;
import com.android.networkstack.tethering.companionproxy.io.EventManager;
import com.android.networkstack.tethering.companionproxy.io.FileHandle;
import com.android.networkstack.tethering.companionproxy.io.ReadableDataAnswer;
import com.android.networkstack.tethering.companionproxy.util.ReadableByteBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class StreamingPacketFileTest {
    private static final int MAX_PACKET_SIZE = 100;

    @Mock EventManager mockEventManager;
    @Mock PacketFile.Listener mockFileListener;
    @Mock AsyncFile mockAsyncFile;
    @Mock ParcelFileDescriptor mockParcelFileDescriptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(ignoreStubs(mockFileListener, mockAsyncFile, mockEventManager));
    }

    @Test
    public void continueReadingAndClose() throws Exception {
        final int maxBufferedInboundPackets = 3;
        final int maxBufferedOutboundPackets = 5;

        final StreamingPacketFile file =
            createFile(maxBufferedInboundPackets, maxBufferedOutboundPackets);
        final BufferedFile bufferedFile = file.getUnderlyingFileForTest();

        assertEquals(maxBufferedInboundPackets * (MAX_PACKET_SIZE + 2),
            bufferedFile.getInboundBufferFreeSizeForTest());
        assertEquals(maxBufferedOutboundPackets * (MAX_PACKET_SIZE + 2),
            bufferedFile.getOutboundBufferFreeSize());
        assertEquals(bufferedFile.getOutboundBufferFreeSize() - 2,
            file.getOutboundFreeSize());

        file.continueReading();
        verify(mockAsyncFile).enableReadEvents(true);

        file.close();
        verify(mockAsyncFile).close();
    }

    @Test
    public void enqueueOutboundPacket() throws Exception {
        final int maxBufferedInboundPackets = 10;
        final int maxBufferedOutboundPackets = 20;

        final StreamingPacketFile file =
            createFile(maxBufferedInboundPackets, maxBufferedOutboundPackets);
        final BufferedFile bufferedFile = file.getUnderlyingFileForTest();

        final byte[] packet1 = new byte[11];
        final byte[] packet2 = new byte[12];
        packet1[0] = (byte) 1;
        packet2[0] = (byte) 2;

        assertEquals(0, bufferedFile.getOutboundBufferSize());

        when(mockAsyncFile.write(any(), anyInt(), anyInt())).thenReturn(0);
        assertTrue(file.enqueueOutboundPacket(packet1, 0, packet1.length));
        verify(mockAsyncFile).enableWriteEvents(true);

        assertEquals(packet1.length + 2, bufferedFile.getOutboundBufferSize());

        checkAndResetMocks();

        final int totalLen = packet1.length + packet2.length + 4;

        final ArgumentCaptor<byte[]> arrayCaptor = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Integer> posCaptor = ArgumentCaptor.forClass(Integer.class);
        final ArgumentCaptor<Integer> lenCaptor = ArgumentCaptor.forClass(Integer.class);
        when(mockAsyncFile.write(
            arrayCaptor.capture(), posCaptor.capture(), lenCaptor.capture())).thenReturn(totalLen);

        assertTrue(file.enqueueOutboundPacket(packet2, 0, packet2.length));

        assertEquals(0, bufferedFile.getInboundBuffer().size());
        assertEquals(0, bufferedFile.getOutboundBufferSize());

        assertEquals(0, posCaptor.getValue().intValue());
        assertEquals(totalLen, lenCaptor.getValue().intValue());

        final byte[] capturedData = arrayCaptor.getValue();
        assertEquals(packet1.length, NetUtils.decodeNetworkUnsignedInt16(capturedData, 0));
        assertEquals(packet2.length,
            NetUtils.decodeNetworkUnsignedInt16(capturedData, packet1.length + 2));
        assertEquals(packet1[0], capturedData[2]);
        assertEquals(packet2[0], capturedData[packet1.length + 4]);
    }

    @Test
    public void onInboundPacket() throws Exception {
        final int maxBufferedInboundPackets = 10;
        final int maxBufferedOutboundPackets = 20;

        final StreamingPacketFile file =
            createFile(maxBufferedInboundPackets, maxBufferedOutboundPackets);
        final BufferedFile bufferedFile = file.getUnderlyingFileForTest();
        final ReadableByteBuffer inboundBuffer = bufferedFile.getInboundBuffer();

        final int len1 = 11;
        final int len2 = 12;
        final byte[] data = new byte[len1 + len2 + 4];
        NetUtils.encodeNetworkUnsignedInt16(len1, data, 0);
        NetUtils.encodeNetworkUnsignedInt16(len2, data, 11 + 2);
        data[2] = (byte) 1;
        data[len1 + 4] = (byte) 2;

        final ReadableDataAnswer dataAnswer = new ReadableDataAnswer(data);

        final ArgumentCaptor<byte[]> arrayCaptor = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Integer> posCaptor = ArgumentCaptor.forClass(Integer.class);
        final ArgumentCaptor<Integer> lenCaptor = ArgumentCaptor.forClass(Integer.class);

        when(mockAsyncFile.read(any(), anyInt(), anyInt())).thenAnswer(dataAnswer);
        when(mockFileListener.onPreambleData(any(), eq(0), eq(data.length))).thenReturn(0);
        bufferedFile.onReadReady(mockAsyncFile);
        verify(mockAsyncFile).enableReadEvents(true);
        verify(mockFileListener).onInboundBuffered(data.length, data.length);
        verify(mockFileListener).onInboundPacket(
            arrayCaptor.capture(), posCaptor.capture(), lenCaptor.capture());
        verify(mockEventManager).execute(any());

        byte[] capturedData = arrayCaptor.getValue();
        assertEquals(2, posCaptor.getValue().intValue());
        assertEquals(len1, lenCaptor.getValue().intValue());
        assertEquals((byte) 1, capturedData[2]);

        checkAndResetMocks();

        when(mockAsyncFile.read(any(), anyInt(), anyInt())).thenAnswer(dataAnswer);
        file.onBufferedFileInboundData(0);
        verify(mockFileListener).onInboundPacket(
            arrayCaptor.capture(), posCaptor.capture(), lenCaptor.capture());
        verify(mockEventManager).execute(any());

        capturedData = arrayCaptor.getValue();
        assertEquals(2, posCaptor.getValue().intValue());
        assertEquals(len2, lenCaptor.getValue().intValue());
        assertEquals((byte) 2, capturedData[2]);

        assertEquals(0, bufferedFile.getOutboundBufferSize());
        assertEquals(0, inboundBuffer.size());
    }

    @Test
    public void onReadReady_preambleData() throws Exception {
        final int maxBufferedInboundPackets = 10;
        final int maxBufferedOutboundPackets = 20;

        final StreamingPacketFile file =
            createFile(maxBufferedInboundPackets, maxBufferedOutboundPackets);
        final BufferedFile bufferedFile = file.getUnderlyingFileForTest();
        final ReadableByteBuffer inboundBuffer = bufferedFile.getInboundBuffer();

        final int preambleLen = 23;
        final int len1 = 11;
        final byte[] data = new byte[preambleLen + 2 + len1];
        NetUtils.encodeNetworkUnsignedInt16(len1, data, preambleLen);
        data[preambleLen + 2] = (byte) 1;

        final ReadableDataAnswer dataAnswer = new ReadableDataAnswer(data);

        when(mockAsyncFile.read(any(), anyInt(), anyInt())).thenAnswer(dataAnswer);
        when(mockFileListener.onPreambleData(any(), eq(0), eq(data.length))).thenReturn(5);
        when(mockFileListener.onPreambleData(
            any(), eq(0), eq(data.length - 5))).thenReturn(preambleLen - 5);
        when(mockFileListener.onPreambleData(
            any(), eq(0), eq(data.length - preambleLen))).thenReturn(0);

        bufferedFile.onReadReady(mockAsyncFile);

        final ArgumentCaptor<byte[]> arrayCaptor = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Integer> posCaptor = ArgumentCaptor.forClass(Integer.class);
        final ArgumentCaptor<Integer> lenCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(mockFileListener).onInboundBuffered(data.length, data.length);
        verify(mockFileListener).onInboundPacket(
            arrayCaptor.capture(), posCaptor.capture(), lenCaptor.capture());
        verify(mockEventManager).execute(any());
        verify(mockAsyncFile).enableReadEvents(true);

        final byte[] capturedData = arrayCaptor.getValue();
        assertEquals(2, posCaptor.getValue().intValue());
        assertEquals(len1, lenCaptor.getValue().intValue());
        assertEquals((byte) 1, capturedData[2]);

        assertEquals(0, bufferedFile.getOutboundBufferSize());
        assertEquals(0, inboundBuffer.size());
    }

    @Test
    public void shutdownReading() throws Exception {
        final int maxBufferedInboundPackets = 10;
        final int maxBufferedOutboundPackets = 20;

        final StreamingPacketFile file =
            createFile(maxBufferedInboundPackets, maxBufferedOutboundPackets);
        final BufferedFile bufferedFile = file.getUnderlyingFileForTest();

        final byte[] data = new byte[100];
        final ReadableDataAnswer dataAnswer = new ReadableDataAnswer(data);
        when(mockAsyncFile.read(any(), anyInt(), anyInt())).thenAnswer(dataAnswer);

        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) {
                file.shutdownReading();
                return Integer.valueOf(-1);
            }}).when(mockFileListener).onPreambleData(any(), anyInt(), anyInt());

        bufferedFile.onReadReady(mockAsyncFile);

        verify(mockFileListener).onInboundBuffered(data.length, data.length);
        verify(mockAsyncFile).enableReadEvents(false);

        assertEquals(0, bufferedFile.getInboundBuffer().size());
    }

    private void checkAndResetMocks() {
        verifyNoMoreInteractions(ignoreStubs(mockFileListener, mockAsyncFile, mockEventManager,
            mockParcelFileDescriptor));
        reset(mockFileListener, mockAsyncFile, mockEventManager);
    }

    private StreamingPacketFile createFile(
            int maxBufferedInboundPackets, int maxBufferedOutboundPackets) throws Exception {
        when(mockEventManager.registerFile(any(), any())).thenReturn(mockAsyncFile);
        return new StreamingPacketFile(
            mockEventManager,
            FileHandle.fromFileDescriptor(mockParcelFileDescriptor),
            mockFileListener,
            MAX_PACKET_SIZE,
            maxBufferedInboundPackets,
            maxBufferedOutboundPackets);
    }
}
