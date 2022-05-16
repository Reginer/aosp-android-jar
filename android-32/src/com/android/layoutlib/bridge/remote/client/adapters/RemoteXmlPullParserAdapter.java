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

package com.android.layoutlib.bridge.remote.client.adapters;

import com.android.layout.remote.api.RemoteXmlPullParser;
import com.android.layout.remote.util.RemoteInputStream;
import com.android.layout.remote.util.StreamUtil;
import com.android.tools.layoutlib.annotations.NotNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.Reader;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteXmlPullParserAdapter implements RemoteXmlPullParser {
    protected XmlPullParser mDelegate;

    protected RemoteXmlPullParserAdapter(@NotNull XmlPullParser delegate) {
        mDelegate = delegate;
    }

    public static RemoteXmlPullParser create(@NotNull XmlPullParser delegate)
            throws RemoteException {
        return (RemoteXmlPullParser) UnicastRemoteObject.exportObject(
                new RemoteXmlPullParserAdapter(delegate), 0);
    }

    @Override
    public void setFeature(String name, boolean state)
            throws XmlPullParserException, RemoteException {
        mDelegate.setFeature(name, state);
    }

    @Override
    public boolean getFeature(String name) throws RemoteException {
        return mDelegate.getFeature(name);
    }

    @Override
    public void setProperty(String name, Object value)
            throws XmlPullParserException, RemoteException {
        mDelegate.setProperty(name, value);
    }

    @Override
    public Object getProperty(String name) throws RemoteException {
        return mDelegate.getProperty(name);
    }

    @Override
    public void setInput(Reader in) throws XmlPullParserException, RemoteException {
        mDelegate.setInput(in);
    }

    @Override
    public void setInput(RemoteInputStream inputStream, String inputEncoding)
            throws XmlPullParserException, RemoteException {
        mDelegate.setInput(StreamUtil.getInputStream(inputStream), inputEncoding);
    }

    @Override
    public String getInputEncoding() throws RemoteException {
        return mDelegate.getInputEncoding();
    }

    @Override
    public void defineEntityReplacementText(String entityName, String replacementText)
            throws XmlPullParserException {

    }

    @Override
    public int getNamespaceCount(int depth) throws XmlPullParserException, RemoteException {
        return mDelegate.getNamespaceCount(depth);
    }

    @Override
    public String getNamespacePrefix(int pos) throws XmlPullParserException, RemoteException {
        return mDelegate.getNamespacePrefix(pos);
    }

    @Override
    public String getNamespaceUri(int pos) throws XmlPullParserException, RemoteException {
        return mDelegate.getNamespaceUri(pos);
    }

    @Override
    public String getNamespace(String prefix) throws RemoteException {
        return mDelegate.getNamespace(prefix);
    }

    @Override
    public int getDepth() throws RemoteException {
        return mDelegate.getDepth();
    }

    @Override
    public String getPositionDescription() throws RemoteException {
        return mDelegate.getPositionDescription();
    }

    @Override
    public int getLineNumber() throws RemoteException {
        return mDelegate.getLineNumber();
    }

    @Override
    public int getColumnNumber() throws RemoteException {
        return mDelegate.getColumnNumber();
    }

    @Override
    public boolean isWhitespace() throws XmlPullParserException, RemoteException {
        return mDelegate.isWhitespace();
    }

    @Override
    public String getText() throws RemoteException {
        return mDelegate.getText();
    }

    @Override
    public char[] getTextCharacters(int[] holderForStartAndLength) throws RemoteException {
        return mDelegate.getTextCharacters(holderForStartAndLength);
    }

    @Override
    public String getNamespace() throws RemoteException {
        return mDelegate.getNamespace();
    }

    @Override
    public String getName() throws RemoteException {
        return mDelegate.getName();
    }

    @Override
    public String getPrefix() throws RemoteException {
        return mDelegate.getPrefix();
    }

    @Override
    public boolean isEmptyElementTag() throws XmlPullParserException, RemoteException {
        return mDelegate.isEmptyElementTag();
    }

    @Override
    public int getAttributeCount() throws RemoteException {
        return mDelegate.getAttributeCount();
    }

    @Override
    public String getAttributeNamespace(int index) throws RemoteException {
        return mDelegate.getAttributeNamespace(index);
    }

    @Override
    public String getAttributeName(int index) throws RemoteException {
        return mDelegate.getAttributeName(index);
    }

    @Override
    public String getAttributePrefix(int index) throws RemoteException {
        return mDelegate.getAttributePrefix(index);
    }

    @Override
    public String getAttributeType(int index) throws RemoteException {
        return mDelegate.getAttributeType(index);
    }

    @Override
    public boolean isAttributeDefault(int index) throws RemoteException {
        return mDelegate.isAttributeDefault(index);
    }

    @Override
    public String getAttributeValue(int index) throws RemoteException {
        return mDelegate.getAttributeValue(index);
    }

    @Override
    public String getAttributeValue(String namespace, String name) throws RemoteException {
        return mDelegate.getAttributeValue(namespace, name);
    }

    @Override
    public int getEventType() throws XmlPullParserException, RemoteException {
        return mDelegate.getEventType();
    }

    @Override
    public int next() throws XmlPullParserException, IOException, RemoteException {
        return mDelegate.next();
    }

    @Override
    public int nextToken() throws XmlPullParserException, IOException, RemoteException {
        return mDelegate.nextToken();
    }

    @Override
    public void require(int type, String namespace, String name)
            throws XmlPullParserException, IOException, RemoteException {
        mDelegate.require(type, namespace, name);
    }

    @Override
    public String nextText() throws XmlPullParserException, IOException, RemoteException {
        return mDelegate.nextText();
    }

    @Override
    public int nextTag() throws XmlPullParserException, IOException, RemoteException {
        return mDelegate.nextTag();
    }
}
