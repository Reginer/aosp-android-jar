package com.android.networkstack.tethering.companionproxy.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
public class BufferedFileTest {
    @Mock EventManager mockEventManager;
    @Mock BufferedFile.Listener mockFileListener;
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
    public void onClosed() throws Exception {
        final int inboundBufferSize = 1024;
        final int outboundBufferSize = 768;

        final BufferedFile file = createFile(inboundBufferSize, outboundBufferSize);

        file.onClosed(mockAsyncFile);

        verify(mockFileListener).onBufferedFileClosed();
    }

    @Test
    public void continueReadingAndClose() throws Exception {
        final int inboundBufferSize = 1024;
        final int outboundBufferSize = 768;

        final BufferedFile file = createFile(inboundBufferSize, outboundBufferSize);

        assertEquals(inboundBufferSize, file.getInboundBufferFreeSizeForTest());
        assertEquals(outboundBufferSize, file.getOutboundBufferFreeSize());

        file.continueReading();
        verify(mockAsyncFile).enableReadEvents(true);

        file.close();
        verify(mockAsyncFile).close();
    }

    @Test
    public void enqueueOutboundData() throws Exception {
        final int inboundBufferSize = 10;
        final int outboundBufferSize = 250;

        final BufferedFile file = createFile(inboundBufferSize, outboundBufferSize);

        final byte[] data1 = new byte[101];
        final byte[] data2 = new byte[102];
        data1[0] = (byte) 1;
        data2[0] = (byte) 2;

        assertEquals(0, file.getOutboundBufferSize());

        final int totalLen = data1.length + data2.length;

        when(mockAsyncFile.write(any(), anyInt(), anyInt())).thenReturn(0);
        assertTrue(file.enqueueOutboundData(data1, 0, data1.length, null, 0, 0));
        verify(mockAsyncFile).enableWriteEvents(true);

        assertEquals(data1.length, file.getOutboundBufferSize());

        checkAndResetMocks();

        final ArgumentCaptor<byte[]> arrayCaptor = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Integer> posCaptor = ArgumentCaptor.forClass(Integer.class);
        final ArgumentCaptor<Integer> lenCaptor = ArgumentCaptor.forClass(Integer.class);
        when(mockAsyncFile.write(
            arrayCaptor.capture(), posCaptor.capture(), lenCaptor.capture())).thenReturn(totalLen);

        assertTrue(file.enqueueOutboundData(data2, 0, data2.length, null, 0, 0));

        assertEquals(0, file.getInboundBuffer().size());
        assertEquals(0, file.getOutboundBufferSize());

        assertEquals(0, posCaptor.getValue().intValue());
        assertEquals(totalLen, lenCaptor.getValue().intValue());
        assertEquals(data1[0], arrayCaptor.getValue()[0]);
        assertEquals(data2[0], arrayCaptor.getValue()[data1.length]);
    }

    @Test
    public void enqueueOutboundData_combined() throws Exception {
        final int inboundBufferSize = 10;
        final int outboundBufferSize = 250;

        final BufferedFile file = createFile(inboundBufferSize, outboundBufferSize);

        final byte[] data1 = new byte[101];
        final byte[] data2 = new byte[102];
        data1[0] = (byte) 1;
        data2[0] = (byte) 2;

        assertEquals(0, file.getOutboundBufferSize());

        final int totalLen = data1.length + data2.length;

        final ArgumentCaptor<byte[]> arrayCaptor = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Integer> posCaptor = ArgumentCaptor.forClass(Integer.class);
        final ArgumentCaptor<Integer> lenCaptor = ArgumentCaptor.forClass(Integer.class);
        when(mockAsyncFile.write(
            arrayCaptor.capture(), posCaptor.capture(), lenCaptor.capture())).thenReturn(totalLen);

        assertTrue(file.enqueueOutboundData(data1, 0, data1.length, data2, 0, data2.length));

        assertEquals(0, file.getInboundBuffer().size());
        assertEquals(0, file.getOutboundBufferSize());

        assertEquals(0, posCaptor.getValue().intValue());
        assertEquals(totalLen, lenCaptor.getValue().intValue());
        assertEquals(data1[0], arrayCaptor.getValue()[0]);
        assertEquals(data2[0], arrayCaptor.getValue()[data1.length]);
    }

    @Test
    public void enableWriteEvents() throws Exception {
        final int inboundBufferSize = 10;
        final int outboundBufferSize = 250;

        final BufferedFile file = createFile(inboundBufferSize, outboundBufferSize);

        final byte[] data1 = new byte[101];
        final byte[] data2 = new byte[102];
        final byte[] data3 = new byte[103];
        data1[0] = (byte) 1;
        data2[0] = (byte) 2;
        data3[0] = (byte) 3;

        assertEquals(0, file.getOutboundBufferSize());

        // Write first 2 buffers, but fail to flush them, causing async write request.
        final int data1And2Len = data1.length + data2.length;
        when(mockAsyncFile.write(any(), eq(0), eq(data1And2Len))).thenReturn(0);
        assertTrue(file.enqueueOutboundData(data1, 0, data1.length, data2, 0, data2.length));
        assertEquals(0, file.getInboundBuffer().size());
        assertEquals(data1And2Len, file.getOutboundBufferSize());
        verify(mockAsyncFile).enableWriteEvents(true);

        // Try to write 3rd buffers, which won't fit, then fail to flush.
        when(mockAsyncFile.write(any(), eq(0), eq(data1And2Len))).thenReturn(0);
        assertFalse(file.enqueueOutboundData(data3, 0, data3.length, null, 0, 0));
        assertEquals(0, file.getInboundBuffer().size());
        assertEquals(data1And2Len, file.getOutboundBufferSize());
        verify(mockAsyncFile, times(2)).enableWriteEvents(true);

        checkAndResetMocks();

        // Simulate writeability event, and successfully flush.
        final ArgumentCaptor<byte[]> arrayCaptor = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Integer> posCaptor = ArgumentCaptor.forClass(Integer.class);
        final ArgumentCaptor<Integer> lenCaptor = ArgumentCaptor.forClass(Integer.class);
        when(mockAsyncFile.write(arrayCaptor.capture(),
                posCaptor.capture(), lenCaptor.capture())).thenReturn(data1And2Len);
        file.onWriteReady(mockAsyncFile);
        verify(mockAsyncFile).enableWriteEvents(false);
        verify(mockFileListener).onBufferedFileOutboundSpace();
        assertEquals(0, file.getOutboundBufferSize());

        assertEquals(0, posCaptor.getValue().intValue());
        assertEquals(data1And2Len, lenCaptor.getValue().intValue());
        assertEquals(data1[0], arrayCaptor.getValue()[0]);
        assertEquals(data2[0], arrayCaptor.getValue()[data1.length]);

        checkAndResetMocks();

        // Now write, but fail to flush the third buffer.
        when(mockAsyncFile.write(arrayCaptor.capture(),
                posCaptor.capture(), lenCaptor.capture())).thenReturn(0);
        assertTrue(file.enqueueOutboundData(data3, 0, data3.length, null, 0, 0));
        verify(mockAsyncFile).enableWriteEvents(true);
        assertEquals(data3.length, file.getOutboundBufferSize());

        assertEquals(data1And2Len, posCaptor.getValue().intValue());
        assertEquals(outboundBufferSize - data1And2Len, lenCaptor.getValue().intValue());
        assertEquals(data3[0], arrayCaptor.getValue()[data1And2Len]);
    }

    @Test
    public void read() throws Exception {
        final int inboundBufferSize = 250;
        final int outboundBufferSize = 10;

        final BufferedFile file = createFile(inboundBufferSize, outboundBufferSize);

        final byte[] data1 = new byte[101];
        final byte[] data2 = new byte[102];
        data1[0] = (byte) 1;
        data2[0] = (byte) 2;

        final ReadableDataAnswer dataAnswer = new ReadableDataAnswer(data1, data2);
        final ReadableByteBuffer inboundBuffer = file.getInboundBuffer();

        when(mockAsyncFile.read(any(), anyInt(), anyInt())).thenAnswer(dataAnswer);
        file.onReadReady(mockAsyncFile);
        verify(mockAsyncFile).enableReadEvents(true);
        verify(mockFileListener).onBufferedFileInboundData(eq(data1.length + data2.length));

        assertEquals(0, file.getOutboundBufferSize());
        assertEquals(data1.length + data2.length, inboundBuffer.size());
        assertEquals((byte) 1, inboundBuffer.peek(0));
        assertEquals((byte) 2, inboundBuffer.peek(data1.length));
    }

    @Test
    public void enableReadEvents() throws Exception {
        final int inboundBufferSize = 250;
        final int outboundBufferSize = 10;

        final BufferedFile file = createFile(inboundBufferSize, outboundBufferSize);

        final byte[] data1 = new byte[101];
        final byte[] data2 = new byte[102];
        final byte[] data3 = new byte[103];
        data1[0] = (byte) 1;
        data2[0] = (byte) 2;
        data3[0] = (byte) 3;

        final ReadableDataAnswer dataAnswer = new ReadableDataAnswer(data1, data2, data3);
        final ReadableByteBuffer inboundBuffer = file.getInboundBuffer();

        when(mockAsyncFile.read(any(), anyInt(), anyInt())).thenAnswer(dataAnswer);
        file.onReadReady(mockAsyncFile);
        verify(mockAsyncFile).enableReadEvents(false);
        verify(mockFileListener).onBufferedFileInboundData(eq(inboundBufferSize));

        assertEquals(0, file.getOutboundBufferSize());
        assertEquals(inboundBufferSize, inboundBuffer.size());
        assertEquals((byte) 1, inboundBuffer.peek(0));
        assertEquals((byte) 2, inboundBuffer.peek(data1.length));
        assertEquals((byte) 3, inboundBuffer.peek(data1.length + data2.length));

        checkAndResetMocks();

        // Cannot enable read events since the buffer is full.
        file.continueReading();

        checkAndResetMocks();

        final byte[] tmp = new byte[inboundBufferSize];
        inboundBuffer.readBytes(tmp, 0, data1.length);
        assertEquals(inboundBufferSize - data1.length, inboundBuffer.size());

        file.continueReading();

        inboundBuffer.readBytes(tmp, 0, data2.length);
        assertEquals(inboundBufferSize - data1.length - data2.length, inboundBuffer.size());

        when(mockAsyncFile.read(any(), anyInt(), anyInt())).thenAnswer(dataAnswer);
        file.onReadReady(mockAsyncFile);
        verify(mockAsyncFile, times(2)).enableReadEvents(true);
        verify(mockFileListener).onBufferedFileInboundData(
            eq(data1.length + data2.length + data3.length - inboundBufferSize));

        assertEquals(data3.length, inboundBuffer.size());
        assertEquals((byte) 3, inboundBuffer.peek(0));
    }

    @Test
    public void shutdownReading() throws Exception {
        final int inboundBufferSize = 250;
        final int outboundBufferSize = 10;

        final BufferedFile file = createFile(inboundBufferSize, outboundBufferSize);

        final byte[] data = new byte[100];
        final ReadableDataAnswer dataAnswer = new ReadableDataAnswer(data);
        when(mockAsyncFile.read(any(), anyInt(), anyInt())).thenAnswer(dataAnswer);

        file.shutdownReading();
        file.onReadReady(mockAsyncFile);

        verify(mockAsyncFile).enableReadEvents(false);

        assertEquals(0, file.getInboundBuffer().size());
        assertEquals(data.length, dataAnswer.getRemainingSize());
    }

    @Test
    public void shutdownReading_inCallback() throws Exception {
        final int inboundBufferSize = 250;
        final int outboundBufferSize = 10;

        final BufferedFile file = createFile(inboundBufferSize, outboundBufferSize);

        final byte[] data = new byte[100];
        final ReadableDataAnswer dataAnswer = new ReadableDataAnswer(data);
        when(mockAsyncFile.read(any(), anyInt(), anyInt())).thenAnswer(dataAnswer);

        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) {
                file.shutdownReading();
                return null;
            }}).when(mockFileListener).onBufferedFileInboundData(anyInt());

        file.onReadReady(mockAsyncFile);

        verify(mockAsyncFile).enableReadEvents(false);

        assertEquals(0, file.getInboundBuffer().size());
        assertEquals(0, dataAnswer.getRemainingSize());
    }

    private void checkAndResetMocks() {
        verifyNoMoreInteractions(ignoreStubs(mockFileListener, mockAsyncFile, mockEventManager,
            mockParcelFileDescriptor));
        reset(mockFileListener, mockAsyncFile, mockEventManager);
    }

    private BufferedFile createFile(
            int inboundBufferSize, int outboundBufferSize) throws Exception {
        when(mockEventManager.registerFile(any(), any())).thenReturn(mockAsyncFile);
        return BufferedFile.create(
            mockEventManager,
            FileHandle.fromFileDescriptor(mockParcelFileDescriptor),
            mockFileListener,
            inboundBufferSize,
            outboundBufferSize);
    }
}
