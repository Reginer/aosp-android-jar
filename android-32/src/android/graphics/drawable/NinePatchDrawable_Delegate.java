/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.graphics.drawable;

import com.android.ide.common.rendering.api.ILayoutLog;
import com.android.layoutlib.bridge.Bridge;
import com.android.tools.layoutlib.annotations.LayoutlibDelegate;

import android.graphics.PixelFormat;

public class NinePatchDrawable_Delegate {

    @LayoutlibDelegate
    static int getOpacity(NinePatchDrawable thisDrawable) {
        // User-defined nine-patches can have a null underlying bitmap, if incorrectly constructed.
        // Trying to preview such a nine-patch drawable will trigger a NullPointerException in the
        // getOpacity, which then get logged by the Studio crash reporting system.
        // Do not crash here, but let it instead crash during draw, which gets logged by layoutlib
        // and is then handled better by Studio.
        try {
            return thisDrawable.getOpacity_Original();
        } catch (NullPointerException ignore) {
            Bridge.getLog().warning(ILayoutLog.TAG_BROKEN, "The source for the nine-patch " +
                    "drawable has not been correctly defined.", null , null);
            return PixelFormat.OPAQUE;
        }
    }
}
