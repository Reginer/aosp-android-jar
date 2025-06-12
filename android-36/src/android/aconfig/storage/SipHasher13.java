/*
 * Copyright (C) 2024 The Android Open Source Project
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
 * limitations under the License.
 */

package android.aconfig.storage;

public class SipHasher13 {
    static class State {
        private long v0;
        private long v2;
        private long v1;
        private long v3;

        public State(long k0, long k1) {
            v0 = k0 ^ 0x736f6d6570736575L;
            v1 = k1 ^ 0x646f72616e646f6dL;
            v2 = k0 ^ 0x6c7967656e657261L;
            v3 = k1 ^ 0x7465646279746573L;
        }

        public void compress(long m) {
            v3 ^= m;
            cRounds();
            v0 ^= m;
        }

        public long finish() {
            v2 ^= 0xff;
            dRounds();
            return v0 ^ v1 ^ v2 ^ v3;
        }

        private void cRounds() {
            v0 += v1;
            v1 = Long.rotateLeft(v1, 13);
            v1 ^= v0;
            v0 = Long.rotateLeft(v0, 32);
            v2 += v3;
            v3 = Long.rotateLeft(v3, 16);
            v3 ^= v2;
            v0 += v3;
            v3 = Long.rotateLeft(v3, 21);
            v3 ^= v0;
            v2 += v1;
            v1 = Long.rotateLeft(v1, 17);
            v1 ^= v2;
            v2 = Long.rotateLeft(v2, 32);
        }

        private void dRounds() {
            for (int i = 0; i < 3; i++) {
                v0 += v1;
                v1 = Long.rotateLeft(v1, 13);
                v1 ^= v0;
                v0 = Long.rotateLeft(v0, 32);
                v2 += v3;
                v3 = Long.rotateLeft(v3, 16);
                v3 ^= v2;
                v0 += v3;
                v3 = Long.rotateLeft(v3, 21);
                v3 ^= v0;
                v2 += v1;
                v1 = Long.rotateLeft(v1, 17);
                v1 ^= v2;
                v2 = Long.rotateLeft(v2, 32);
            }
        }
    }

    public static long hash(byte[] data) {
        State state = new State(0, 0);
        int len = data.length;
        int left = len & 0x7;
        int index = 0;

        while (index < len - left) {
            long mi = loadLe(data, index, 8);
            index += 8;
            state.compress(mi);
        }

        // padding the end with 0xff to be consistent with rust
        long m = (0xffL << (left * 8)) | loadLe(data, index, left);
        if (left == 0x7) {
            // compress the m w-2
            state.compress(m);
            m = 0L;
        }
        // len adds 1 since padded 0xff
        m |= (((len + 1) & 0xffL) << 56);
        state.compress(m);

        return state.finish();
    }

    private static long loadLe(byte[] data, int offset, int size) {
        long m = 0;
        for (int i = 0; i < size; i++) {
            m |= (data[i + offset] & 0xffL) << (i * 8);
        }
        return m;
    }
}
