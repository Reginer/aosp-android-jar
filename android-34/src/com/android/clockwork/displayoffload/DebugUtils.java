/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.clockwork.displayoffload;

import android.annotation.Nullable;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.HidlMemoryUtil;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import vendor.google_clockwork.displayoffload.V1_0.PngResource;
import vendor.google_clockwork.displayoffload.V1_0.TtfFontResource;
import vendor.google_clockwork.displayoffload.V2_0.BitmapDrawable;
import vendor.google_clockwork.displayoffload.V2_0.PixelFormat;

/** Debug related utilities. */
public class DebugUtils {
    private static final String TAG = "DODebug";
    private static final String DUMP_PREFIX = "/data/system/displayoffload/";

    public static final boolean DEBUG_CALLBACK = Build.IS_DEBUGGABLE;
    public static final boolean DEBUG_DATA_SNAPSHOT = false;
    public static final boolean DEBUG_DOZE = Build.IS_DEBUGGABLE;
    public static final boolean DEBUG_HAL = Build.IS_DEBUGGABLE;
    public static final boolean DEBUG_UID = false;
    public static boolean DEBUG_FONT_DUMP = false;
    public static boolean DEBUG_FONT_SUBSETTING = false;
    public static boolean DEBUG_HAL_DUMP = false;
    public static boolean DEBUG_AMBIENT_TRACKER = false;

    static String dumpObjectIdType(int id, @Nullable Object object) {
        return "[" + id + "]:" + dumpObjectType(object);
    }

    static String dumpObjectIdType(String id, @Nullable Object object) {
        return "[" + id + "]:" + dumpObjectType(object);
    }

    static String dumpObjectType(@Nullable Object object) {
        return object != null ? object.getClass().getSimpleName() : " ? ";
    }

    static void dump(@Nullable Object halObject) {
        if (halObject == null) return;
        byte[] data = null;
        String id = null;
        if (halObject instanceof PngResource) {
            id = "png." + ((PngResource) halObject).id + ".png";
            data = Utils.convertToByteArray(((PngResource) halObject).data);
        }
        if (halObject instanceof BitmapDrawable) {
            id = "bmp." + ((BitmapDrawable) halObject).id + ".png";
            data = convertToPng(((BitmapDrawable) halObject).bitmap);
        }
        if (data != null) {
            dumpAsFile(data, id);
        }
    }

    static String dumpObjectDetails(@Nullable Object object) {
        if (object == null) {
            return "null";
        }
        if (object instanceof PngResource
                || object instanceof BitmapDrawable
                || object instanceof TtfFontResource
                || object instanceof vendor.google_clockwork.displayoffload.V2_0.TtfFontResource) {
            return String.format("{ %s omitted }", dumpObjectType(object));
        }
        return object.toString();
    }

    static void ensureDirectoryForDump() {
        try {
            // Create directory
            File storageDir = new File(DUMP_PREFIX);
            storageDir.mkdirs();
        } catch (Exception e) {
            Log.w(TAG, "Failed to create dump directory");
        }
    }

    static void dumpAsFile(byte[] data, String fileName) {
        try {
            ensureDirectoryForDump();
            String path = DUMP_PREFIX + fileName;
            File file = new File(path);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
            Log.i(TAG, "Dumped to " + path);
        } catch (Exception e) {
            Log.w(TAG, "Failed to dump as file " + fileName, e);
        }
    }

    private static byte[] convertToPng(
            vendor.google_clockwork.displayoffload.V2_0.Bitmap halBitmap) {
        // convert from HAL pixel format to Bitmap.Config
        Bitmap.Config config = null;
        int bpp = 0;
        switch (halBitmap.format) {
            case PixelFormat.RGBA_8888: {
                config = Bitmap.Config.ARGB_8888;
                bpp = 4;
                break;
            }
            case PixelFormat.RGB_565: {
                config = Bitmap.Config.RGB_565;
                bpp = 2;
                break;
            }
            case PixelFormat.ALPHA_8: {
                config = Bitmap.Config.ALPHA_8;
                bpp = 1;
                break;
            }
        }

        // Create a new Android bitmap to store copy of HidlMemory contents
        Bitmap bmp = Bitmap.createBitmap(halBitmap.width, halBitmap.height, config);

        byte[] src = HidlMemoryUtil.hidlMemoryToByteArray(halBitmap.data);
        ByteBuffer dst = ByteBuffer.wrap(src);
        if (halBitmap.rowBytes != bmp.getRowBytes()) {
            // copy one row at a time from hidlmemory into the new rowbyte order.
            dst = ByteBuffer.allocate(bmp.getAllocationByteCount());
            for (int i = 0; i < halBitmap.height; i++) {
                dst.position(i * bmp.getRowBytes());
                dst.put(src, i * halBitmap.rowBytes, bpp * halBitmap.width);
            }
            dst.rewind();
        }

        // copy pixels into Android bitmap and compress the bitmap to a PNG
        bmp.copyPixelsFromBuffer(dst);
        ByteArrayOutputStream pngReceiver = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, /* quality= */ 0, pngReceiver);
        return pngReceiver.toByteArray();
    }
}
