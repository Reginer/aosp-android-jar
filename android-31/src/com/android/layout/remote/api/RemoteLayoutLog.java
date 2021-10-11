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

import com.android.ide.common.rendering.api.ILayoutLog;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote version of the {@link ILayoutLog} class
 */
public interface RemoteLayoutLog extends Remote {
    /**
     * Logs a warning.
     *
     * @param tag a tag describing the type of the warning
     * @param message the message of the warning
     * @param viewCookie optional cookie of the view associated to this error
     * @param data an optional data bundle that the client can use to improve the warning display.
     */
    void warning(String tag, String message, Object viewCookie, Serializable data) throws RemoteException;

    /**
     * Logs a fidelity warning.
     * <p>
     * This type of warning indicates that the render will not be the same as the rendering on a
     * device due to limitation of the Java rendering API.
     *
     * @param tag a tag describing the type of the warning
     * @param message the message of the warning
     * @param throwable an optional Throwable that triggered the warning
     * @param viewCookie optional cookie of the view associated to this error
     * @param data an optional data bundle that the client can use to improve the warning display.
     */
    void fidelityWarning(String tag, String message, Throwable throwable, Object viewCookie,
            Object data) throws RemoteException;

    /**
     * Logs an error.
     *
     * @param tag a tag describing the type of the error
     * @param message the message of the error
     * @param viewCookie optional cookie of the view associated to this error
     * @param data an optional data bundle that the client can use to improve the error display.
     */
    void error(String tag, String message, Object viewCookie, Serializable data) throws RemoteException;

    /**
     * Logs an error, and the {@link Throwable} that triggered it.
     *
     * @param tag a tag describing the type of the error
     * @param message the message of the error
     * @param throwable the Throwable that triggered the error
     * @param viewCookie optional cookie of the view associated to this error
     * @param data an optional data bundle that the client can use to improve the error display.
     */
    void error(String tag, String message, Throwable throwable, Object viewCookie, Serializable data)
            throws RemoteException;

    /** 
     * Logs messages coming from the Android Framework. 
     *
     * @param priority the priority level of the message 
     * @param tag a tag describing the type of the error
     * @param message the message of the error
     */
    void logAndroidFramework(int priority, String tag, String message) throws RemoteException;
}
