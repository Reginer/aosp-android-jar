/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.layoutlib.bridge.remote.server.adapters;

import com.android.layout.remote.api.RemoteXmlPullParser;
import com.android.layout.remote.util.RemoteInputStreamAdapter;
import com.android.tools.layoutlib.annotations.NotNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.rmi.RemoteException;

class RemoteXmlPullParserAdapter implements XmlPullParser {
    protected RemoteXmlPullParser mDelegate;

    RemoteXmlPullParserAdapter(@NotNull RemoteXmlPullParser remote) {
        mDelegate = remote;
    }

    @Override
    public void setFeature(String name, boolean state) throws XmlPullParserException {
        try {
            mDelegate.setFeature(name, state);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean getFeature(String name) {
        try {
            return mDelegate.getFeature(name);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setProperty(String name, Object value) throws XmlPullParserException {
        try {
            mDelegate.setProperty(name, value);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getProperty(String name) {
        try {
            return mDelegate.getProperty(name);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setInput(Reader in) throws XmlPullParserException {
        try {
            mDelegate.setInput(in);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setInput(InputStream inputStream, String inputEncoding)
            throws XmlPullParserException {
        try {
            mDelegate.setInput(RemoteInputStreamAdapter.create(inputStream), inputEncoding);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getInputEncoding() {
        try {
            return mDelegate.getInputEncoding();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void defineEntityReplacementText(String entityName, String replacementText)
            throws XmlPullParserException {
        try {
            mDelegate.defineEntityReplacementText(entityName, replacementText);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getNamespaceCount(int depth) throws XmlPullParserException {
        try {
            return mDelegate.getNamespaceCount(depth);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getNamespacePrefix(int pos) throws XmlPullParserException {
        try {
            return mDelegate.getNamespacePrefix(pos);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getNamespaceUri(int pos) throws XmlPullParserException {
        try {
            return mDelegate.getNamespaceUri(pos);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getNamespace(String prefix) {
        try {
            return mDelegate.getNamespace(prefix);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getDepth() {
        try {
            return mDelegate.getDepth();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getPositionDescription() {
        try {
            return mDelegate.getPositionDescription();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getLineNumber() {
        try {
            return mDelegate.getLineNumber();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getColumnNumber() {
        try {
            return mDelegate.getColumnNumber();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isWhitespace() throws XmlPullParserException {
        try {
            return mDelegate.isWhitespace();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getText() {
        try {
            return mDelegate.getText();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public char[] getTextCharacters(int[] holderForStartAndLength) {
        try {
            return mDelegate.getTextCharacters(holderForStartAndLength);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getNamespace() {
        try {
            return mDelegate.getNamespace();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        try {
            return mDelegate.getName();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getPrefix() {
        try {
            return mDelegate.getPrefix();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isEmptyElementTag() throws XmlPullParserException {
        try {
            return mDelegate.isEmptyElementTag();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getAttributeCount() {
        try {
            return mDelegate.getAttributeCount();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getAttributeNamespace(int index) {
        try {
            return mDelegate.getAttributeNamespace(index);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getAttributeName(int index) {
        try {
            return mDelegate.getAttributeName(index);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getAttributePrefix(int index) {
        try {
            return mDelegate.getAttributePrefix(index);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getAttributeType(int index) {
        try {
            return mDelegate.getAttributeType(index);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isAttributeDefault(int index) {
        try {
            return mDelegate.isAttributeDefault(index);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getAttributeValue(int index) {
        try {
            return mDelegate.getAttributeValue(index);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        try {
            return mDelegate.getAttributeValue(namespace, name);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getEventType() throws XmlPullParserException {
        try {
            return mDelegate.getEventType();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int next() throws XmlPullParserException, IOException {
        return mDelegate.next();
    }

    @Override
    public int nextToken() throws XmlPullParserException, IOException {
        return mDelegate.nextToken();
    }

    @Override
    public void require(int type, String namespace, String name)
            throws XmlPullParserException, IOException {
        mDelegate.require(type, namespace, name);
    }

    @Override
    public String nextText() throws XmlPullParserException, IOException {
        return mDelegate.nextText();
    }

    @Override
    public int nextTag() throws XmlPullParserException, IOException {
        return mDelegate.nextTag();
    }
}
