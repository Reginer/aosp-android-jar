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
 * Specifies algorithm parameters for the KDF component of an HPKE suite
 * which are determined by standard names as per RFC 9180.
 * <p>
 * These parameters can be composed into a full HKPE suite name using
 * {@link Hpke#getSuiteName(KemParameterSpec, KdfParameterSpec, AeadParameterSpec)}.
 * <p>
 * Note that currently only {@code HKDF_SHA256} is implemented.
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9180.html#section-7.2">RFC 9180 Section 7.2</a>
 * @see NamedParameterSpec
 */
@FlaggedApi(com.android.libcore.Flags.FLAG_HPKE_PUBLIC_API)
public class KdfParameterSpec extends NamedParameterSpec {
    /**
     * @see NamedParameterSpec
     */
    private KdfParameterSpec(@NonNull String stdName) {
        super(stdName);
    }

    public static final KdfParameterSpec HKDF_SHA256 = new KdfParameterSpec("HKDF_SHA256");

    public static final KdfParameterSpec HKDF_SHA384 = new KdfParameterSpec("HKDF_SHA384");

    public static final KdfParameterSpec HKDF_SHA512 = new KdfParameterSpec("HKDF_SHA512");
}
