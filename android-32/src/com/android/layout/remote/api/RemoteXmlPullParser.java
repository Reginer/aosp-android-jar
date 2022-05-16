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

package com.android.layout.remote.api;

import com.android.layout.remote.util.RemoteInputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.Reader;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote version of the {@link XmlPullParser} interface
 */
public interface RemoteXmlPullParser extends Remote {
    void setFeature(String name, boolean state) throws XmlPullParserException, RemoteException;

    boolean getFeature(String name) throws RemoteException;

    void setProperty(String name, Object value) throws XmlPullParserException, RemoteException;

    Object getProperty(String name) throws RemoteException;

    void setInput(Reader in) throws XmlPullParserException, RemoteException;

    void setInput(RemoteInputStream inputStream, String inputEncoding)
            throws XmlPullParserException, RemoteException;

    String getInputEncoding() throws RemoteException;

    void defineEntityReplacementText(String entityName, String replacementText)
            throws XmlPullParserException, RemoteException;

    int getNamespaceCount(int depth) throws XmlPullParserException, RemoteException;

    String getNamespacePrefix(int pos) throws XmlPullParserException, RemoteException;

    String getNamespaceUri(int pos) throws XmlPullParserException, RemoteException;

    String getNamespace(String prefix) throws RemoteException;

    int getDepth() throws RemoteException;

    String getPositionDescription() throws RemoteException;

    int getLineNumber() throws RemoteException;

    int getColumnNumber() throws RemoteException;

    boolean isWhitespace() throws XmlPullParserException, RemoteException;

    String getText() throws RemoteException;

    char[] getTextCharacters(int[] holderForStartAndLength) throws RemoteException;

    String getNamespace() throws RemoteException;

    String getName() throws RemoteException;

    String getPrefix() throws RemoteException;

    boolean isEmptyElementTag() throws XmlPullParserException, RemoteException;

    int getAttributeCount() throws RemoteException;

    String getAttributeNamespace(int index) throws RemoteException;

    String getAttributeName(int index) throws RemoteException;

    String getAttributePrefix(int index) throws RemoteException;

    String getAttributeType(int index) throws RemoteException;

    boolean isAttributeDefault(int index) throws RemoteException;

    String getAttributeValue(int index) throws RemoteException;

    String getAttributeValue(String namespace, String name) throws RemoteException;

    int getEventType() throws XmlPullParserException, RemoteException;

    int next() throws XmlPullParserException, IOException, RemoteException;

    int nextToken() throws XmlPullParserException, IOException, RemoteException;

    void require(int type, String namespace, String name)
            throws XmlPullParserException, IOException, RemoteException;

    String nextText() throws XmlPullParserException, IOException, RemoteException;

    int nextTag() throws XmlPullParserException, IOException, RemoteException;
}
