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

package android.graphics;

import com.android.tools.layoutlib.annotations.LayoutlibDelegate;

import android.annotation.Nullable;
import android.content.res.AssetManager;
import android.graphics.Typeface.Builder;
import android.graphics.fonts.FontVariationAxis;

public class Typeface_Builder_Delegate {

    /**
     * Creates a unique id for a given AssetManager and asset path.
     *
     * @param mgr  AssetManager instance
     * @param path The path for the asset.
     * @param ttcIndex The TTC index for the font.
     * @param axes The font variation settings.
     * @return Unique id for a given AssetManager and asset path.
     */
    @LayoutlibDelegate
    public static String createAssetUid(final AssetManager mgr, String path, int ttcIndex,
            @Nullable FontVariationAxis[] axes, int weight, int italic, String fallback) {
        return Builder.createAssetUid_Original(mgr, path, ttcIndex, axes, weight, italic, fallback);
    }
}
