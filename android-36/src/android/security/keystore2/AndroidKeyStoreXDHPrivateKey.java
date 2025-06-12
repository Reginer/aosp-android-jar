/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.security.keystore2;

import android.annotation.NonNull;
import android.security.KeyStoreSecurityLevel;
import android.system.keystore2.Authorization;
import android.system.keystore2.KeyDescriptor;

import java.security.PrivateKey;
import java.security.interfaces.XECPrivateKey;
import java.security.spec.NamedParameterSpec;
import java.util.Optional;

/**
 * X25519 Private Key backed by Keystore.
 * instance of {@link PrivateKey} and {@link XECPrivateKey}
 *
 * @hide
 */
public class AndroidKeyStoreXDHPrivateKey extends AndroidKeyStorePrivateKey
        implements XECPrivateKey {
    public AndroidKeyStoreXDHPrivateKey(
            @NonNull KeyDescriptor descriptor, long keyId,
            @NonNull Authorization[] authorizations,
            @NonNull String algorithm,
            @NonNull KeyStoreSecurityLevel securityLevel) {
        super(descriptor, keyId, authorizations, algorithm, securityLevel);
    }

    @Override
    public NamedParameterSpec getParams() {
        return NamedParameterSpec.X25519;
    }

    @Override
    public Optional<byte[]> getScalar() {
        /* An empty Optional if the scalar cannot be extracted (e.g. if the provider is a hardware
         * token and the private key is not allowed to leave the crypto boundary).
         */
        return Optional.empty();
    }
}
