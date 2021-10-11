/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.layout.remote.api;

import com.android.tools.layoutlib.annotations.NotNull;
import com.android.tools.layoutlib.annotations.Nullable;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteNamespaceResolver extends Remote {
    static final RemoteNamespaceResolver EMPTY_RESOLVER = new EmptyResolver();

    /** Returns the full URI of an XML namespace for a given prefix, if defined. */
    @Nullable
    String remotePrefixToUri(@NotNull String namespacePrefix) throws RemoteException;

    @Nullable
    String remoteUriToPrefix(@NotNull String namespaceUri) throws RemoteException;

    class EmptyResolver implements Serializable, RemoteNamespaceResolver {
        private EmptyResolver() {
        }

        @Override
        public String remotePrefixToUri(String namespacePrefix) {
            return null;
        }

        @Override
        public String remoteUriToPrefix(String namespaceUri) {
            return null;
        }
    }
}
