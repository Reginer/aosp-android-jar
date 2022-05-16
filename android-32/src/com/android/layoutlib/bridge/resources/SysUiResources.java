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

package com.android.layoutlib.bridge.resources;

import com.android.ide.common.rendering.api.ResourceNamespace;
import com.android.layoutlib.bridge.Bridge;
import com.android.layoutlib.bridge.android.BridgeContext;
import com.android.layoutlib.bridge.android.BridgeXmlBlockParser;
import com.android.layoutlib.bridge.bars.Config;
import com.android.layoutlib.bridge.impl.ParserFactory;
import com.android.resources.Density;
import com.android.resources.LayoutDirection;
import com.android.tools.layoutlib.annotations.NotNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap_Delegate;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;

public class SysUiResources {
    private static final ResourceNamespace PRIVATE_LAYOUTLIB_NAMESPACE =
            ResourceNamespace.fromPackageName("com.android.layoutlib");

    @NotNull
    public static BridgeXmlBlockParser loadXml(BridgeContext context, int apiLevel, String
            layoutName) {
        for (String resourceRepository : Config.getResourceDirs(apiLevel)) {
            String path = resourceRepository + layoutName;
            InputStream stream = SysUiResources.class.getResourceAsStream(path);
            if (stream != null) {
                try {
                    XmlPullParser parser = ParserFactory.create(stream, layoutName);

                    // TODO(b/156609434): does the namespace matter here?
                    return new BridgeXmlBlockParser(parser, context, PRIVATE_LAYOUTLIB_NAMESPACE);
                } catch (XmlPullParserException e) {
                    // Should not happen as the resource is bundled with the jar, and  ParserFactory should
                    // have been initialized.
                    assert false;
                }
            }
        }

        assert false;
        return null;
    }

    public static ImageView loadIcon(Context context, int api, ImageView imageView, String
            iconName,
            Density density, boolean
            isRtl) {
        LayoutDirection dir = isRtl ? LayoutDirection.RTL : null;
        IconLoader iconLoader = new IconLoader(iconName, density, api,
                dir);
        InputStream stream = iconLoader.getIcon();

        if (stream != null) {
            density = iconLoader.getDensity();
            String path = iconLoader.getPath();
            // look for a cached bitmap
            Bitmap bitmap = Bridge.getCachedBitmap(path, Boolean.TRUE /*isFramework*/);
            if (bitmap == null) {
                try {
                    bitmap = Bitmap_Delegate.createBitmap(stream, false /*isMutable*/, density);
                    Bridge.setCachedBitmap(path, bitmap, Boolean.TRUE /*isFramework*/);
                } catch (IOException e) {
                    return imageView;
                }
            }

            if (bitmap != null) {
                BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
                imageView.setImageDrawable(drawable);
            }
        }

        return imageView;
    }
}
