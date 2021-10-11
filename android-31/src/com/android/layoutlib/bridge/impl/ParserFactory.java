/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.layoutlib.bridge.impl;

import com.android.ide.common.rendering.api.XmlParserFactory;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A factory for {@link XmlPullParser}.
 */
public class ParserFactory {
    public final static boolean LOG_PARSER = false;

    // Used to get a new XmlPullParser from the client.
    @Nullable
    private static XmlParserFactory sParserFactory;

    public static void setParserFactory(@Nullable XmlParserFactory parserFactory) {
        sParserFactory = parserFactory;
    }

    @Nullable
    public static XmlPullParser create(@NonNull String filePath)
            throws XmlPullParserException {
        return create(filePath, false);
    }

    @Nullable
    public static XmlPullParser create(@NonNull String filePath, boolean isLayout)
            throws XmlPullParserException {
        XmlPullParser parser = sParserFactory.createXmlParserForFile(filePath);
        if (parser != null && isLayout) {
            try {
                return new LayoutParserWrapper(parser).peekTillLayoutStart();
            } catch (IOException e) {
                throw new XmlPullParserException(null, parser, e);
            }
        }
        return parser;
    }

    @NonNull
    public static XmlPullParser create(@NonNull InputStream stream, @Nullable String name)
            throws XmlPullParserException {
        XmlPullParser parser = create();

        stream = readAndClose(stream, name);

        parser.setInput(stream, null);
        return parser;
    }

    @NonNull
    public static XmlPullParser create() throws XmlPullParserException {
        if (sParserFactory == null) {
            throw new XmlPullParserException("ParserFactory not initialized.");
        }
        XmlPullParser parser = sParserFactory.createXmlParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        return parser;
    }

    @NonNull
    private static InputStream readAndClose(@NonNull InputStream stream, @Nullable String name)
            throws XmlPullParserException {
        // Create a buffered stream to facilitate reading.
        try (BufferedInputStream bufferedStream = new BufferedInputStream(stream)) {
            int avail = bufferedStream.available();

            // Create the initial buffer and read it.
            byte[] buffer = new byte[avail];
            int read = stream.read(buffer);

            // Check if there is more to read (read() does not necessarily read all that
            // available() returned!)
            while ((avail = bufferedStream.available()) > 0) {
                if (read + avail > buffer.length) {
                    // just allocate what is needed. We're mostly reading small files
                    // so it shouldn't be too problematic.
                    byte[] moreBuffer = new byte[read + avail];
                    System.arraycopy(buffer, 0, moreBuffer, 0, read);
                    buffer = moreBuffer;
                }

                read += stream.read(buffer, read, avail);
            }

            // Return a new stream encapsulating this buffer.
            return new ByteArrayInputStream(buffer);
        } catch (IOException e) {
            throw new XmlPullParserException("Failed to read " + name, null, e);
        }
    }
}
