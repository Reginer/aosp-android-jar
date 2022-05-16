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

package android.graphics.drawable;

import com.android.tools.layoutlib.annotations.LayoutlibDelegate;

import android.content.res.Resources;
import android.content.res.Resources_Delegate;
import android.util.PathParser;

import static com.android.layoutlib.bridge.android.RenderParamsFlags.FLAG_KEY_ADAPTIVE_ICON_MASK_PATH;

public class AdaptiveIconDrawable_Delegate {

    @LayoutlibDelegate
    /*package*/ static void constructor_after(AdaptiveIconDrawable icon) {
        String pathString = Resources_Delegate.getLayoutlibCallback(Resources.getSystem()).getFlag(
                FLAG_KEY_ADAPTIVE_ICON_MASK_PATH);
        if (pathString != null) {
            AdaptiveIconDrawable.sMask = PathParser.createPathFromPathData(pathString);
        }
    }
}
