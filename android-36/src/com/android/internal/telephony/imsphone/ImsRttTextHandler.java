/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.internal.telephony.imsphone;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telecom.Connection;

import com.android.internal.annotations.VisibleForTesting;
import com.android.telephony.Rlog;

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.CountDownLatch;

public class ImsRttTextHandler extends Handler {
    public interface NetworkWriter {
        void write(String s);
    }

    private static final String LOG_TAG = "ImsRttTextHandler";
    // RTT buffering and sending tuning constants.
    // TODO: put this in carrier config?

    // These count Unicode codepoints, not Java char types.
    public static final int MAX_CODEPOINTS_PER_SECOND = 30;
    // Assuming that we do not exceed the rate limit, this is the maximum time between when a
    // piece of text is received and when it is actually sent over the network.
    public static final int MAX_BUFFERING_DELAY_MILLIS = 200;
    // Assuming that we do not exceed the rate limit, this is the maximum size we will allow
    // the buffer to grow to before sending as many as we can.
    public static final int MAX_BUFFERED_CHARACTER_COUNT = 5;
    private static final int MILLIS_PER_SECOND = 1000;

    // Messages for the handler.
    // Initializes the text handler. Should have an RttTextStream set in msg.obj
    private static final int INITIALIZE = 1;
    // Appends a string to the buffer to send to the network. Should have the string in msg.obj
    private static final int APPEND_TO_NETWORK_BUFFER = 2;
    // Send a string received from the network to the in-call app. Should have the string in
    // msg.obj.
    private static final int SEND_TO_INCALL = 3;
    // Send as many characters as possible, as constrained by the rate limit. No extra data.
    private static final int ATTEMPT_SEND_TO_NETWORK = 4;
    // Indicates that N characters were sent a second ago and should be ignored by the rate
    // limiter. msg.arg1 should be set to N.
    private static final int EXPIRE_SENT_CODEPOINT_COUNT = 5;
    // Indicates that the call is over and we should teardown everything we have set up.
    private static final int TEARDOWN = 9999;

    private Connection.RttTextStream mRttTextStream;
    // For synchronization during testing
    private CountDownLatch mReadNotifier;

    private class InCallReaderThread extends Thread {
        private final Connection.RttTextStream mReaderThreadRttTextStream;

        public InCallReaderThread(Connection.RttTextStream textStream) {
            mReaderThreadRttTextStream = textStream;
        }

        @Override
        public void run() {
            while (true) {
                String charsReceived;
                try {
                    charsReceived = mReaderThreadRttTextStream.read();
                } catch (ClosedByInterruptException e) {
                    Rlog.i(LOG_TAG, "RttReaderThread - Thread interrupted. Finishing.");
                    break;
                } catch (IOException e) {
                    Rlog.e(LOG_TAG, "RttReaderThread - IOException encountered " +
                            "reading from in-call: ", e);
                    obtainMessage(TEARDOWN).sendToTarget();
                    break;
                }
                if (charsReceived == null) {
                    Rlog.e(LOG_TAG, "RttReaderThread - Stream closed unexpectedly. Attempt to " +
                            "reinitialize.");
                    obtainMessage(TEARDOWN).sendToTarget();
                    break;
                }
                if (charsReceived.length() == 0) {
                    continue;
                }
                obtainMessage(APPEND_TO_NETWORK_BUFFER, charsReceived)
                        .sendToTarget();
                if (mReadNotifier != null) {
                    mReadNotifier.countDown();
                }
            }
        }
    }

    private int mCodepointsAvailableForTransmission = MAX_CODEPOINTS_PER_SECOND;
    private StringBuffer mBufferedTextToNetwork = new StringBuffer();
    private InCallReaderThread mReaderThread;
    // This is only ever used when the pipes fail and we have to re-setup. Messages received
    // from the network are buffered here until Telecom gets back to us with the new pipes.
    private StringBuffer mBufferedTextToIncall = new StringBuffer();
    private final NetworkWriter mNetworkWriter;

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case INITIALIZE:
                if (mRttTextStream != null || mReaderThread != null) {
                    Rlog.e(LOG_TAG, "RTT text stream already initialized. Ignoring.");
                    return;
                }
                mRttTextStream = (Connection.RttTextStream) msg.obj;
                mReaderThread = new InCallReaderThread(mRttTextStream);
                mReaderThread.start();
                break;
            case SEND_TO_INCALL:
                if (msg.obj == null) {
                    Rlog.e(LOG_TAG, "RTT msg.obj is null. Ignoring.");
                    return;
                }
                String messageToIncall = (String) msg.obj;
                if (mRttTextStream == null) {
                    Rlog.e(LOG_TAG, "RTT text stream is null. Writing to in-call buffer.");
                    mBufferedTextToIncall.append(messageToIncall);
                    return;
                }
                try {
                    mRttTextStream.write(messageToIncall);
                } catch (IOException e) {
                    Rlog.e(LOG_TAG, "IOException encountered writing to in-call: %s", e);
                    obtainMessage(TEARDOWN).sendToTarget();
                    mBufferedTextToIncall.append(messageToIncall);
                }
                break;
            case APPEND_TO_NETWORK_BUFFER:
                // First, append the text-to-send to the string buffer
                mBufferedTextToNetwork.append((String) msg.obj);
                // Check to see how many codepoints we have buffered. If we have more than 5,
                // send immediately, otherwise, wait until a timeout happens.
                int numCodepointsBuffered = mBufferedTextToNetwork
                        .codePointCount(0, mBufferedTextToNetwork.length());
                if (numCodepointsBuffered >= MAX_BUFFERED_CHARACTER_COUNT) {
                    sendMessage(obtainMessage(ATTEMPT_SEND_TO_NETWORK));
                } else {
                    sendEmptyMessageDelayed(
                            ATTEMPT_SEND_TO_NETWORK, MAX_BUFFERING_DELAY_MILLIS);
                }
                break;
            case ATTEMPT_SEND_TO_NETWORK:
                // Check to see how many codepoints we can send, and send that many.
                int numCodePointsAvailableInBuffer = mBufferedTextToNetwork.codePointCount(0,
                        mBufferedTextToNetwork.length());
                int numCodePointsSent = Math.min(numCodePointsAvailableInBuffer,
                        mCodepointsAvailableForTransmission);
                if (numCodePointsSent == 0) {
                    break;
                }
                int endSendIndex = mBufferedTextToNetwork.offsetByCodePoints(0,
                        numCodePointsSent);

                String stringToSend = mBufferedTextToNetwork.substring(0, endSendIndex);

                mBufferedTextToNetwork.delete(0, endSendIndex);
                mNetworkWriter.write(stringToSend);
                mCodepointsAvailableForTransmission -= numCodePointsSent;
                sendMessageDelayed(
                        obtainMessage(EXPIRE_SENT_CODEPOINT_COUNT, numCodePointsSent, 0),
                        MILLIS_PER_SECOND);
                break;
            case EXPIRE_SENT_CODEPOINT_COUNT:
                mCodepointsAvailableForTransmission += msg.arg1;
                if (mCodepointsAvailableForTransmission > 0) {
                    sendMessage(obtainMessage(ATTEMPT_SEND_TO_NETWORK));
                }
                break;
            case TEARDOWN:
                try {
                    if (mReaderThread != null) {
                        mReaderThread.interrupt();
                        mReaderThread.join(1000);
                    }
                } catch (InterruptedException e) {
                    // Ignore and assume it'll finish on its own.
                }
                mReaderThread = null;
                mRttTextStream = null;
                break;
        }
    }

    public ImsRttTextHandler(Looper looper, NetworkWriter networkWriter) {
        super(looper);
        mNetworkWriter = networkWriter;
    }

    public void sendToInCall(String msg) {
        obtainMessage(SEND_TO_INCALL, msg).sendToTarget();
    }

    public void initialize(Connection.RttTextStream rttTextStream) {
        Rlog.i(LOG_TAG, "Initializing: " + this);
        obtainMessage(INITIALIZE, rttTextStream).sendToTarget();
    }

    public void tearDown() {
        obtainMessage(TEARDOWN).sendToTarget();
    }

    @VisibleForTesting
    public void setReadNotifier(CountDownLatch latch) {
        mReadNotifier = latch;
    }

    @VisibleForTesting
    public StringBuffer getBufferedTextToIncall() {
        return mBufferedTextToIncall;
    }

    @VisibleForTesting
    public void setRttTextStream(Connection.RttTextStream rttTextStream) {
        mRttTextStream = rttTextStream;
    }

    @VisibleForTesting
    public int getSendToIncall() {
        return SEND_TO_INCALL;
    }

    public String getNetworkBufferText() {
        return mBufferedTextToNetwork.toString();
    }
}
