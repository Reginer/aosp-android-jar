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

import com.android.ide.common.rendering.api.ResourceNamespace.Resolver;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.layout.remote.util.RemoteResolverAdapter;
import com.android.tools.layoutlib.annotations.NotNull;
import com.android.tools.layoutlib.annotations.Nullable;

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * Wrapper for {@link ResourceValue} that can be transferred to a different VM.
 *
 * @param <T> the ResourceValue instance type
 */
public class RemoteResourceValue<T extends ResourceValue> implements Serializable {
    private static final RemoteResourceValue<ResourceValue> NULL_INSTANCE =
            new RemoteResourceValue<>(null, null);

    private final T mResourceValue;
    private final RemoteNamespaceResolver mRemoteResolver;

    private RemoteResourceValue(@Nullable T resourceValue,
            @Nullable RemoteNamespaceResolver remoteResolver) {
        mResourceValue = resourceValue;
        mRemoteResolver = remoteResolver;
    }

    /**
     * Returns a RemoteResourceValue that wraps the given {@link ResourceValue} instance. The passed
     * resource value can be null.
     */
    @NotNull
    public static <T extends ResourceValue> RemoteResourceValue<T> fromResourceValue(
            T resourceValue) {
        if (resourceValue == null) {
            //noinspection unchecked
            return (RemoteResourceValue<T>) NULL_INSTANCE;
        }
        try {
            return new RemoteResourceValue<>(resourceValue,
                    RemoteResolverAdapter.create(resourceValue.getNamespaceResolver()));

        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the {@link ResourceValue} wrapped by a remote wrapper
     */
    @NotNull
    private static <T extends ResourceValue> T toResourceValue(
            @NotNull RemoteResourceValue<T> remoteResourceValue) {
        T remoteValue = remoteResourceValue.mResourceValue;
        if (remoteValue == null) {
            return null;
        }

        // The Resolver is not transferred in the ResourceValue (it's transient) so we use the
        // information in the wrapper to reconstruct it.
        RemoteNamespaceResolver remoteResolver = remoteResourceValue.mRemoteResolver;

//        TODO: Rethink this as setNamespaceResolver is not available in prebuilts any more
//        if (remoteResolver != null) {
//            remoteValue.setNamespaceResolver(new Resolver() {
//                @Override
//                public String prefixToUri(String namespacePrefix) {
//                    try {
//                        return remoteResolver.remotePrefixToUri(namespacePrefix);
//                    } catch (RemoteException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//
//                @Override
//                public String uriToPrefix(String namespaceUri) {
//                    try {
//                        return remoteResolver.remoteUriToPrefix(namespaceUri);
//                    } catch (RemoteException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            });
//        }
//        else {
//            remoteValue.setNamespaceResolver(Resolver.EMPTY_RESOLVER);
//        }

        return remoteValue;
    }

    /**
     * Returns the {@link ResourceValue} wrapped by this remote wrapper
     */
    @NotNull
    public T toResourceValue() {
        return RemoteResourceValue.toResourceValue(this);
    }
}
