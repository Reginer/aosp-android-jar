/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.internal.net.eap.crypto;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/** A factory class responsible for creating an instance of TlsSession */
public class TlsSessionFactory {

    /**
     * Retrieves a new instance of TlsSession
     *
     * @param serverCaCert the CA certificate for validating the received server certificate(s).If
     *     no certificate is provided, any root CA in the system's truststore is considered
     *     acceptable. * @param secureRandom the secure random to use
     * @return a {@link TlsSession}
     * @throws GeneralSecurityException if the TLS session cannot be intiailized
     * @throws IOException if there is an I/O issue with keystore data
     */
    public TlsSession newInstance(X509Certificate serverCaCert, SecureRandom secureRandom)
            throws GeneralSecurityException, IOException {
        return new TlsSession(serverCaCert, secureRandom);
    }
}
