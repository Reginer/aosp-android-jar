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

import java.security.spec.EncodedKeySpec;
import java.util.Arrays;
import java.util.Objects;
import libcore.util.NonNull;

/**
 * External Diffie–Hellman (XDH) key spec holding either a public or private key.
 * <p>
 * Subclasses {@code EncodedKeySpec} using the non-Standard "raw" format.  The XdhKeyFactory
 * class utilises this in order to create XDH keys from raw bytes and to return them
 * as an XdhKeySpec allowing the raw key material to be extracted from an XDH key.
 */
public final class XdhKeySpec extends EncodedKeySpec {
    /**
     * Creates an instance of {@link XdhKeySpec} by passing a public or private key in its raw
     * format.
     */
    public XdhKeySpec(@NonNull byte[] encoded) {
        super(encoded);
    }

    @Override
    @NonNull public String getFormat() {
        return "raw";
    }

    /**
     * Returns the public or private key in its raw format.
     *
     * @return key in its raw format.
     */
    @NonNull public byte[] getKey() {
        return getEncoded();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncodedKeySpec)) return false;
        EncodedKeySpec that = (EncodedKeySpec) o;
        return (getFormat().equals(that.getFormat())
                && (Arrays.equals(getEncoded(), that.getEncoded())));
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFormat(), Arrays.hashCode(getEncoded()));
    }
}
