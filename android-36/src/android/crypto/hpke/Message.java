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

@FlaggedApi(com.android.libcore.Flags.FLAG_HPKE_PUBLIC_API)
public class Message {
    private final byte[] encapsulated;
    private final byte[] ciphertext;

    public Message(
            @NonNull byte[] encapsulated, @NonNull byte[] ciphertext) {
        this.encapsulated = encapsulated;
        this.ciphertext = ciphertext;
    }

    public @NonNull byte[] getEncapsulated() {
        return encapsulated;
    }

    public @NonNull byte[] getCiphertext() {
        return ciphertext;
    }
}
