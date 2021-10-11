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

package com.android.layoutlib.bridge.android;

import com.android.ide.common.rendering.api.ResourceNamespace;

import org.xmlpull.v1.XmlPullParser;

import android.annotation.NonNull;
import android.annotation.Nullable;

/**
 * A {@link ResourceNamespace.Resolver} that delegates to the given {@link XmlPullParser} and falls
 * back to the "implicit" resolver which is assumed to contain namespaces predefined for the file
 * being parsed.
 *
 * <p>Note that the parser will start giving different results as the underlying parser moves in the
 * input, so it should either be discarded or reused with care.
 */
public class XmlPullParserResolver implements ResourceNamespace.Resolver {
    private final XmlPullParser mParser;
    private final ResourceNamespace.Resolver mImplicitNamespacesResolver;

    public XmlPullParserResolver(
            @NonNull XmlPullParser parser,
            @NonNull ResourceNamespace.Resolver implicitNamespacesResolver) {
        mParser = parser;
        mImplicitNamespacesResolver = implicitNamespacesResolver;
    }

    @Override
    @Nullable
    public String prefixToUri(@NonNull String namespacePrefix) {
        String result = mParser.getNamespace(namespacePrefix);
        if (result == null) {
            result = mImplicitNamespacesResolver.prefixToUri(namespacePrefix);
        }

        return result;
    }

    @Override
    @Nullable
    public String uriToPrefix(@NonNull String namespaceUri) {
        // This is needed when creating new XML snippets, we don't need to support that.
        throw new UnsupportedOperationException();
    }
}
