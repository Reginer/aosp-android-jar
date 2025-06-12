/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.renderscript;

import android.app.compat.CompatChanges;
import android.compat.annotation.ChangeId;
import android.compat.annotation.EnabledAfter;
import android.content.res.Resources;
import android.util.Slog;

import java.io.IOException;
import java.io.InputStream;

/**
 * The superclass for all user-defined scripts. This is only
 * intended to be used by the generated derived classes.
 *
 * @deprecated Renderscript has been deprecated in API level 31. Please refer to the <a
 * href="https://developer.android.com/guide/topics/renderscript/migration-guide">migration
 * guide</a> for the proposed alternatives.
 **/
@Deprecated
public class ScriptC extends Script {
    private static final String TAG = "ScriptC";

    /**
     * In targetSdkVersion 36 and above, Renderscript's ScriptC stops being supported
     * and an exception is thrown when the class is instantiated.
     * In targetSdkVersion 35 and below, Renderscript's ScriptC still works.
     */
    @ChangeId
    @EnabledAfter(targetSdkVersion = 36)
    private static final long RENDERSCRIPT_SCRIPTC_DEPRECATION_CHANGE_ID = 297019750L;

    /**
     * Only intended for use by the generated derived classes.
     *
     * @param id
     * @param rs
     */
    protected ScriptC(int id, RenderScript rs) {
        super(id, rs);
    }
    /**
     * Only intended for use by the generated derived classes.
     *
     * @param id
     * @param rs
     *
     */
    protected ScriptC(long id, RenderScript rs) {
        super(id, rs);
    }
    /**
     * Only intended for use by the generated derived classes.
     *
     *
     * @param rs
     * @param resources
     * @param resourceID
     */
    protected ScriptC(RenderScript rs, Resources resources, int resourceID) {
        super(0, rs);
        long id = internalCreate(rs, resources, resourceID);
        if (id == 0) {
            throw new RSRuntimeException("Loading of ScriptC script failed.");
        }
        setID(id);
    }

    /**
     * Only intended for use by the generated derived classes.
     *
     * @param rs
     */
    protected ScriptC(RenderScript rs, String resName, byte[] bitcode32, byte[] bitcode64) {
        super(0, rs);
        long id = 0;
        if (RenderScript.sPointerSize == 4) {
            id = internalStringCreate(rs, resName, bitcode32);
        } else {
            id = internalStringCreate(rs, resName, bitcode64);
        }
        if (id == 0) {
            throw new RSRuntimeException("Loading of ScriptC script failed.");
        }
        setID(id);
    }

    private static void throwExceptionIfScriptCUnsupported() {
        // Checks that this device actually does have an ABI that supports ScriptC.
        //
        // For an explanation as to why `System.loadLibrary` is used, see discussion at
        // https://android-review.googlesource.com/c/platform/frameworks/base/+/2957974/comment/2f908b80_a05292ee
        try {
            System.loadLibrary("RS");
        } catch (UnsatisfiedLinkError e) {
            String s = "This device does not have an ABI that supports ScriptC.";
            throw new UnsupportedOperationException(s);
        }

        // Throw an exception if the target API is 36 or above
        String message =
                "ScriptC scripts are not supported when targeting an API Level >= 36. Please refer "
                    + "to https://developer.android.com/guide/topics/renderscript/migration-guide "
                    + "for proposed alternatives.";
        Slog.w(TAG, message);
        if (CompatChanges.isChangeEnabled(RENDERSCRIPT_SCRIPTC_DEPRECATION_CHANGE_ID)) {
            throw new UnsupportedOperationException(message);
        }
    }

    private static synchronized long internalCreate(RenderScript rs, Resources resources, int resourceID) {
        throwExceptionIfScriptCUnsupported();
        byte[] pgm;
        int pgmLength;
        InputStream is = resources.openRawResource(resourceID);
        try {
            try {
                pgm = new byte[1024];
                pgmLength = 0;
                while(true) {
                    int bytesLeft = pgm.length - pgmLength;
                    if (bytesLeft == 0) {
                        byte[] buf2 = new byte[pgm.length * 2];
                        System.arraycopy(pgm, 0, buf2, 0, pgm.length);
                        pgm = buf2;
                        bytesLeft = pgm.length - pgmLength;
                    }
                    int bytesRead = is.read(pgm, pgmLength, bytesLeft);
                    if (bytesRead <= 0) {
                        break;
                    }
                    pgmLength += bytesRead;
                }
            } finally {
                is.close();
            }
        } catch(IOException e) {
            throw new Resources.NotFoundException();
        }

        String resName = resources.getResourceEntryName(resourceID);

        //        Log.v(TAG, "Create script for resource = " + resName);
        return rs.nScriptCCreate(resName, RenderScript.getCachePath(), pgm, pgmLength);
    }

    private static synchronized long internalStringCreate(RenderScript rs, String resName, byte[] bitcode) {
        //        Log.v(TAG, "Create script for resource = " + resName);
        throwExceptionIfScriptCUnsupported();
        return rs.nScriptCCreate(resName, RenderScript.getCachePath(), bitcode, bitcode.length);
    }
}
