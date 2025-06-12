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

package android.crypto.hpke;

import android.annotation.FlaggedApi;

import libcore.util.NonNull;

import java.security.spec.NamedParameterSpec;

/**
 * Specifies algorithm parameters for the AEAD component of an HPKE suite
 * which are determined by standard names as per RFC 9180.
 * <p>
 * These parameters can be composed into a full HKPE suite name using
 * {@link Hpke#getSuiteName(KemParameterSpec, KdfParameterSpec, AeadParameterSpec)}.
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9180.html#section-7.3">RFC 9180 Section 7.3</a>
 * @see NamedParameterSpec
 */
@FlaggedApi(com.android.libcore.Flags.FLAG_HPKE_PUBLIC_API)
public class AeadParameterSpec extends NamedParameterSpec {
    /**
     * @see NamedParameterSpec
     */
    private AeadParameterSpec(@NonNull String stdName) {
        super(stdName);
    }

    public static final AeadParameterSpec AES_128_GCM
            = new AeadParameterSpec("AES_128_GCM");
    public static final AeadParameterSpec AES_256_GCM
            = new AeadParameterSpec("AES_256_GCM");
    public static final AeadParameterSpec CHACHA20POLY1305
            = new AeadParameterSpec("CHACHA20POLY1305");
}
