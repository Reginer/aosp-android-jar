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

import com.android.ide.common.rendering.api.Bridge;
import com.android.ide.common.rendering.api.ILayoutLog;
import com.android.ide.common.rendering.api.RenderSession;
import com.android.ide.common.rendering.api.Result;
import com.android.tools.layoutlib.annotations.NotNull;
import com.android.tools.layoutlib.annotations.Nullable;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * Interface that defines the operations available in the remote bridge. This is a remote version of
 * the local {@link Bridge} API. Most of the methods are mapped 1:1 with the {@link Bridge} API
 * unless there is a need for a method to be adapted for remote use.
 */
public interface RemoteBridge extends Remote {
    /**
     * Initializes the Bridge object.
     *
     * @param platformProperties The build properties for the platform.
     * @param fontLocation the location of the fonts.
     * @param nativeLibPath the absolute path of the JNI library for layoutlib.
     * @param icuDataPath the location of the ICU data used natively.
     * @param enumValueMap map attrName ⇒ { map enumFlagName ⇒ Integer value }. This is typically
     * read from attrs.xml in the SDK target.
     * @param log a {@link ILayoutLog} object. Can be null.
     *
     * @return true if success.
     */
    boolean init(@NotNull Map<String, String> platformProperties, File fontLocation,
            @Nullable String nativeLibPath, @Nullable String icuDataPath,
            @NotNull Map<String, Map<String, Integer>> enumValueMap,
            @Nullable RemoteLayoutLog log) throws RemoteException;

    /**
     * Prepares the layoutlib to be unloaded.
     */
    boolean dispose() throws RemoteException;

    /**
     * Starts a layout session by inflating and rendering it. The method returns a {@link
     * RenderSession} on which further actions can be taken.
     *
     * @return a new {@link RenderSession} object that contains the result of the scene creation and
     * first rendering.
     */
    @NotNull
    RemoteRenderSession createSession(@NotNull RemoteSessionParams params) throws RemoteException;

    /**
     * Renders a Drawable. If the rendering is successful, the result image is accessible through
     * {@link Result#getData()}. It is of type {@link BufferedImage}
     *
     * @param params the rendering parameters.
     *
     * @return the result of the action.
     */
    @NotNull
    Result renderDrawable(@NotNull RemoteDrawableParams params) throws RemoteException;

    /**
     * Clears the resource cache for a specific project.
     *
     * <p>This cache contains bitmaps and nine patches that are loaded from the disk and reused
     * until this method is called.
     *
     * <p>The cache is not configuration dependent and should only be cleared when a resource
     * changes (at this time only bitmaps and 9 patches go into the cache).
     *
     * <p>The project key provided must be similar to the one passed in {@link RenderParams}.
     *
     * @param projectKey the key for the project.
     */
    void clearResourceCaches(String projectKey) throws RemoteException;

    /**
     * Returns true if the character orientation of the locale is right to left.
     *
     * @param locale The locale formatted as language-region
     *
     * @return true if the locale is right to left.
     */
    boolean isRtl(String locale) throws RemoteException;
}
