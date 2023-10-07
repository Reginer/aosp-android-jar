/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package benchmarks.regression;

import com.google.caliper.Param;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class MessageDigestBenchmark {

    private static final int DATA_SIZE = 8192;
    private static final byte[] DATA = new byte[DATA_SIZE];
    static {
        for (int i = 0; i < DATA_SIZE; i++) {
            DATA[i] = (byte)i;
        }
    }

    private static final int LARGE_DATA_SIZE = 256 * 1024;
    private static final byte[] LARGE_DATA = new byte[LARGE_DATA_SIZE];
    static {
        for (int i = 0; i < LARGE_DATA_SIZE; i++) {
            LARGE_DATA[i] = (byte)i;
        }
    }

    private static final ByteBuffer SMALL_BUFFER = ByteBuffer.wrap(DATA);
    private static final ByteBuffer SMALL_DIRECT_BUFFER = ByteBuffer.allocateDirect(DATA_SIZE);
    static {
        SMALL_DIRECT_BUFFER.put(DATA);
        SMALL_DIRECT_BUFFER.flip();
    }

    private static final ByteBuffer LARGE_BUFFER = ByteBuffer.wrap(LARGE_DATA);
    private static final ByteBuffer LARGE_DIRECT_BUFFER =
            ByteBuffer.allocateDirect(LARGE_DATA_SIZE);
    static {
        LARGE_DIRECT_BUFFER.put(LARGE_DATA);
        LARGE_DIRECT_BUFFER.flip();
    }

    @Param private Algorithm algorithm;

    public enum Algorithm { MD5, SHA1, SHA256,  SHA384, SHA512 };

    @Param private Provider provider;

    public enum Provider { AndroidOpenSSL, BC };

    public void time(int reps) throws Exception {
        for (int i = 0; i < reps; ++i) {
            MessageDigest digest = MessageDigest.getInstance(algorithm.toString(),
                                                             provider.toString());
            digest.update(DATA, 0, DATA_SIZE);
            digest.digest();
        }
    }

    public void timeLargeArray(int reps) throws Exception {
        for (int i = 0; i < reps; ++i) {
            MessageDigest digest = MessageDigest.getInstance(algorithm.toString(),
                                                             provider.toString());
            digest.update(LARGE_DATA, 0, LARGE_DATA_SIZE);
            digest.digest();
        }
    }

    public void timeSmallChunkOfLargeArray(int reps) throws Exception {
        for (int i = 0; i < reps; ++i) {
            MessageDigest digest = MessageDigest.getInstance(algorithm.toString(),
                                                             provider.toString());
            digest.update(LARGE_DATA, LARGE_DATA_SIZE / 2, DATA_SIZE);
            digest.digest();
        }
    }

    public void timeSmallByteBuffer(int reps) throws Exception {
        for (int i = 0; i < reps; ++i) {
            MessageDigest digest = MessageDigest.getInstance(algorithm.toString(),
                                                             provider.toString());
            SMALL_BUFFER.position(0);
            SMALL_BUFFER.limit(SMALL_BUFFER.capacity());
            digest.update(SMALL_BUFFER);
            digest.digest();
        }
    }

    public void timeSmallDirectByteBuffer(int reps) throws Exception {
        for (int i = 0; i < reps; ++i) {
            MessageDigest digest = MessageDigest.getInstance(algorithm.toString(),
                                                             provider.toString());
            SMALL_DIRECT_BUFFER.position(0);
            SMALL_DIRECT_BUFFER.limit(SMALL_DIRECT_BUFFER.capacity());
            digest.update(SMALL_DIRECT_BUFFER);
            digest.digest();
        }
    }

    public void timeLargeByteBuffer(int reps) throws Exception {
        for (int i = 0; i < reps; ++i) {
            MessageDigest digest = MessageDigest.getInstance(algorithm.toString(),
                                                             provider.toString());
            LARGE_BUFFER.position(0);
            LARGE_BUFFER.limit(LARGE_BUFFER.capacity());
            digest.update(LARGE_BUFFER);
            digest.digest();
        }
    }

    public void timeLargeDirectByteBuffer(int reps) throws Exception {
        for (int i = 0; i < reps; ++i) {
            MessageDigest digest = MessageDigest.getInstance(algorithm.toString(),
                                                             provider.toString());
            LARGE_DIRECT_BUFFER.position(0);
            LARGE_DIRECT_BUFFER.limit(LARGE_DIRECT_BUFFER.capacity());
            digest.update(LARGE_DIRECT_BUFFER);
            digest.digest();
        }
    }

    public void timeSmallChunkOfLargeByteBuffer(int reps) throws Exception {
        for (int i = 0; i < reps; ++i) {
            MessageDigest digest = MessageDigest.getInstance(algorithm.toString(),
                                                             provider.toString());
            LARGE_BUFFER.position(LARGE_BUFFER.capacity() / 2);
            LARGE_BUFFER.limit(LARGE_BUFFER.position() + DATA_SIZE);
            digest.update(LARGE_BUFFER);
            digest.digest();
        }
    }

    public void timeSmallChunkOfLargeDirectByteBuffer(int reps) throws Exception {
        for (int i = 0; i < reps; ++i) {
            MessageDigest digest = MessageDigest.getInstance(algorithm.toString(),
                                                             provider.toString());
            LARGE_DIRECT_BUFFER.position(LARGE_DIRECT_BUFFER.capacity() / 2);
            LARGE_DIRECT_BUFFER.limit(LARGE_DIRECT_BUFFER.position() + DATA_SIZE);
            digest.update(LARGE_DIRECT_BUFFER);
            digest.digest();
        }
    }
}
