/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.internal.net.ipsec.ike.crypto;

/**
 * IkeCrypto is an abstract class that represents common information for all negotiated
 * cryptographic algorithms that are used to build IKE SA and protect IKE message.
 */
abstract class IkeCrypto {
    private final int mAlgorithmId;
    private final int mKeyLength;
    private final String mAlgorithmName;

    // IKE crypto algorithm that is not supported by Java Cryptography Extension(JCE)
    protected static final String ALGO_NAME_JCE_UNSUPPORTED = "ALGO_NAME_JCE_UNSUPPORTED";

    protected IkeCrypto(int algorithmId, int keyLength, String algorithmName) {
        mAlgorithmId = algorithmId;
        mKeyLength = keyLength;
        mAlgorithmName = algorithmName;
    }

    protected int getAlgorithmId() {
        return mAlgorithmId;
    }

    protected String getAlgorithmName() {
        return mAlgorithmName;
    }

    /**
     * Gets key length of this algorithm (in bytes).
     *
     * @return the key length (in bytes).
     */
    public int getKeyLength() {
        return mKeyLength;
    }

    /**
     * Returns algorithm type as a String.
     *
     * @return the algorithm type as a String.
     */
    public abstract String getTypeString();
}
