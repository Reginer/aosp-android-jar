/* GENERATED SOURCE. DO NOT MODIFY. */
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

package com.android.org.conscrypt;

import java.security.Principal;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLEngine;

/**
 * @hide This class is not part of the Android public SDK API
 * @hide This class is not part of the Android public SDK API
 */
@Internal
public class Spake2PlusKeyManager implements KeyManager {
    private final byte[] context;
    private final byte[] password;
    private final byte[] idProver;
    private final byte[] idVerifier;
    private final boolean isClient;

    Spake2PlusKeyManager(byte[] context, byte[] password, byte[] idProver,
            byte[] idVerifier, boolean isClient) {
        this.context = context == null ? new byte[0] : context;
        this.password = password;
        this.idProver = idProver == null ? new byte[0] : idProver;
        this.idVerifier = idVerifier == null ? new byte[0] : idVerifier;
        this.isClient = isClient;
    }

    public String chooseEngineAlias(String keyType, Principal[] issuers,
            SSLEngine engine) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String chooseEngineClientAlias(String[] keyType, Principal[] issuers,
            SSLEngine engine) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public byte[] getContext() {
        return context;
    }

    public byte[] getPassword() {
        return password;
    }

    public byte[] getIdProver() {
        return idProver;
    }

    public byte[] getIdVerifier() {
        return idVerifier;
    }

    public boolean isClient() {
        return isClient;
    }
}
