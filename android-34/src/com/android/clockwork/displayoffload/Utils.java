/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.android.clockwork.displayoffload.DisplayOffloadService.ACTIVITY;
import static com.android.clockwork.displayoffload.DisplayOffloadService.ACTIVITY_CHANGED;
import static com.android.clockwork.displayoffload.DisplayOffloadService.AMBIENT_STATE_CHANGED;
import static com.android.clockwork.displayoffload.DisplayOffloadService.BEGIN_RECEIVE_ACTIVITY;
import static com.android.clockwork.displayoffload.DisplayOffloadService.BEGIN_RECEIVE_SYSTEMUI;
import static com.android.clockwork.displayoffload.DisplayOffloadService.BEGIN_RECEIVE_WATCHFACE;
import static com.android.clockwork.displayoffload.DisplayOffloadService.BEGIN_SEND_LAYOUT;
import static com.android.clockwork.displayoffload.DisplayOffloadService.END_DISPLAY_CONTROL;
import static com.android.clockwork.displayoffload.DisplayOffloadService.END_DISPLAY_MCU_RENDERING;
import static com.android.clockwork.displayoffload.DisplayOffloadService.END_RECEIVE_ACTIVITY;
import static com.android.clockwork.displayoffload.DisplayOffloadService.END_RECEIVE_SYSTEMUI;
import static com.android.clockwork.displayoffload.DisplayOffloadService.END_RECEIVE_WATCHFACE;
import static com.android.clockwork.displayoffload.DisplayOffloadService.END_SEND_LAYOUT;
import static com.android.clockwork.displayoffload.DisplayOffloadService.HAL_CONNECTED;
import static com.android.clockwork.displayoffload.DisplayOffloadService.NONE;
import static com.android.clockwork.displayoffload.DisplayOffloadService.OFFLOAD_UPDATE_DOZE_INTERVAL;
import static com.android.clockwork.displayoffload.DisplayOffloadService.RUN_HAL_OPERATIONS_WAITING_DOZE_OR_ON;
import static com.android.clockwork.displayoffload.DisplayOffloadService.START_DISPLAY_CONTROL;
import static com.android.clockwork.displayoffload.DisplayOffloadService.SYSTEMUI;
import static com.android.clockwork.displayoffload.DisplayOffloadService.UPDATE_LOCALE;
import static com.android.clockwork.displayoffload.DisplayOffloadService.WATCHFACE;
import static com.android.clockwork.displayoffload.DisplayOffloadService.WATCHFACE_CHANGED;

import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_LAYOUT_CONVERSION_FAILURE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.RESOURCE_ID_STATUS_BAR_BITMAP;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.RESOURCE_ID_STATUS_BAR_GROUP;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.SharedMemory;
import android.provider.Settings;
import android.system.ErrnoException;
import android.util.Log;
import android.view.Display;

import com.android.internal.util.FastPrintWriter;

import com.google.android.clockwork.ambient.offload.types.BindableFloat;
import com.google.android.clockwork.ambient.offload.types.BlendMode;
import com.google.android.clockwork.ambient.offload.types.TranslationGroup;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

import vendor.google_clockwork.displayoffload.V1_0.Status;

/** Various static definition and utility functions. */
class Utils {
    public static final String TAG = "DisplayOffload";

    public static final String ACTION_STOP_AMBIENT_DREAM =
            "com.google.android.wearable.action.STOP_AMBIENT_DREAM";
    public static final String ACTION_AMBIENT_STARTED =
            "com.google.android.wearable.action.AMBIENT_STARTED";
    public static final String ACTION_AMBIENT_STOPPED =
            "com.google.android.wearable.action.AMBIENT_STOPPED";

    private static final String CLOCKWORK_SYSUI_PACKAGE = "clockwork_sysui_package";

    static ArrayList<Integer> convertToArrayListInteger(int[] arrays) {
        if (arrays == null) {
            return new ArrayList<>();
        }

        ArrayList<Integer> out = new ArrayList<>(arrays.length);
        for (int i : arrays) {
            out.add(i);
        }
        return out;
    }

    static ArrayList<Float> convertToArrayListFloat(float[] arrays) {
        if (arrays == null) {
            return new ArrayList<>();
        }

        ArrayList<Float> out = new ArrayList<>(arrays.length);
        for (float i : arrays) {
            out.add(i);
        }
        return out;
    }

    static ArrayList<Byte> convertToArrayListByte(byte[] arrays) {
        if (arrays == null) {
            return new ArrayList<>();
        }

        ArrayList<Byte> out = new ArrayList<>(arrays.length);
        for (byte i : arrays) {
            out.add(i);
        }
        return out;
    }

    static ArrayList<Byte> convertToArrayListByte(SharedMemory sharedMemory) {
        return Utils.convertToArrayListByte(Utils.convertToByteArray(sharedMemory));
    }

    static ArrayList<Byte> convertToArrayListByte(Context context, Icon icon) {
        return Utils.convertToArrayListByte(Utils.iconToByteArray(context, icon));
    }

    static ArrayList<Short> convertToArrayListShort(char[] arrays) {
        if (arrays == null) {
            return new ArrayList<>();
        }

        ArrayList<Short> out = new ArrayList<>(arrays.length);
        for (char i : arrays) {
            out.add((short) i);
        }
        return out;
    }

    static byte[] convertToByteArray(Collection<Byte> collection) {
        if (collection == null) {
            return new byte[0];
        }

        byte[] out = new byte[collection.size()];
        int i = 0;
        for (Byte item : collection) {
            out[i++] = item;
        }
        return out;
    }

    static int[] convertToIntArray(Collection<Integer> collection) {
        if (collection == null) {
            return new int[0];
        }

        int[] out = new int[collection.size()];
        int i = 0;
        for (Integer item : collection) {
            out[i++] = item;
        }
        return out;
    }

    private static int getSysUiUid(Context context) {
        try {
            return context.getPackageManager()
                    .getPackageUid(
                            Settings.Global.getString(
                                    context.getContentResolver(), CLOCKWORK_SYSUI_PACKAGE),
                            0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to find SysUI package UID");
            return -1;
        }
    }

    static boolean isSysUiUid(Context context, int uid) {
        int sysUiUid = getSysUiUid(context);
        if (DebugUtils.DEBUG_UID) {
            Log.d(TAG, "isSysUiUid: sysUi=" + sysUiUid + " uid=" + uid);
        }
        if (sysUiUid == -1) {
            return false;
        }
        return sysUiUid == uid;
    }

    static String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new FastPrintWriter(sw, false, 256);
        e.printStackTrace(pw);
        pw.flush();
        pw.close();
        return sw.toString();
    }

    static com.google.android.clockwork.ambient.offload.types.BitmapResource createStatusBarBitmap(
            Icon icon, RectF bounds) {
        com.google.android.clockwork.ambient.offload.types.BitmapResource statusBarResource =
                new com.google.android.clockwork.ambient.offload.types.BitmapResource();
        statusBarResource.id = RESOURCE_ID_STATUS_BAR_BITMAP;
        statusBarResource.icon = icon;
        statusBarResource.height = (int) bounds.height();
        statusBarResource.width = (int) bounds.width();
        statusBarResource.blendMode = BlendMode.SRC_OVER;
        return statusBarResource;
    }

    static TranslationGroup createStatusBarTranslationGroup(RectF bounds) {
        TranslationGroup statusBarTranslationGroup = new TranslationGroup();
        statusBarTranslationGroup.id = RESOURCE_ID_STATUS_BAR_GROUP;
        statusBarTranslationGroup.offsetX = createBindableFloat(bounds.left);
        statusBarTranslationGroup.offsetY = createBindableFloat(bounds.top);
        statusBarTranslationGroup.contents = new int[]{RESOURCE_ID_STATUS_BAR_BITMAP};
        return statusBarTranslationGroup;
    }

    static BindableFloat createBindableFloat(float value) {
        BindableFloat bindableFloat = new BindableFloat();
        bindableFloat.value = value;
        return bindableFloat;
    }

    private static byte[] iconToByteArray(Context context, Icon icon) {
        if (icon == null) {
            return new byte[0];
        }

        Drawable drawable = icon.loadDrawable(context);
        if (drawable instanceof BitmapDrawable) {
            ByteArrayOutputStream pngReceiver = new ByteArrayOutputStream();
            // CompressFormat.PNG is lossless, so quality is ignored.
            ((BitmapDrawable) drawable).getBitmap().compress(
                    Bitmap.CompressFormat.PNG, /* quality= */ 0,
                    pngReceiver);
            return pngReceiver.toByteArray();
        } else {
            Log.e(TAG, "Icon is not for a Bitmap");
            return new byte[0];
        }
    }

    static int resultFromHALStatus(int halStatus) {
        switch (halStatus) {
            case Status.OK:
                return 1;
            case Status.UNKNOWN_ERROR:
            case Status.UNSUPPORTED_OPERATION:
            case Status.BAD_VALUE:
            case Status.INSUFFICIENT_RESOURCE:
            default:
                return -1;
        }
    }

    static byte[] convertToByteArray(SharedMemory sharedMemory) {
        if (sharedMemory == null) {
            return new byte[0];
        }

        byte[] bytes = null;
        ByteBuffer buffer = null;

        try {
            bytes = new byte[sharedMemory.getSize()];
            buffer = sharedMemory.mapReadOnly();
            buffer.get(bytes);
        } catch (ErrnoException e) {
            Log.e(TAG, "Error opening shared memory: ", e);
        } finally {
            if (buffer != null) {
                SharedMemory.unmap(buffer);
            }
        }

        return bytes;
    }

    static boolean isArrayLengthOne(int[] binding) {
        return binding != null && binding.length == 1;
    }

    static int checkNameValid(Integer name) throws DisplayOffloadException {
        if (name == null) {
            throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                    "Expression name cannot be empty.");
        }
        return name;
    }

    static boolean isDisplayDozeOrOn(int displayState) {
        return displayState == Display.STATE_DOZE || displayState == Display.STATE_ON;
    }

    // LINT.IfChange(name_for_events)
    static String nameForEvent(@DisplayOffloadService.DisplayOffloadEvent int event) {
        switch (event) {
            case BEGIN_RECEIVE_WATCHFACE:
                return "BEGIN_RECEIVE_WATCHFACE";
            case END_RECEIVE_WATCHFACE:
                return "END_RECEIVE_WATCHFACE";
            case BEGIN_RECEIVE_ACTIVITY:
                return "BEGIN_RECEIVE_ACTIVITY";
            case END_RECEIVE_ACTIVITY:
                return "END_RECEIVE_ACTIVITY";
            case BEGIN_RECEIVE_SYSTEMUI:
                return "BEGIN_RECEIVE_SYSTEMUI";
            case END_RECEIVE_SYSTEMUI:
                return "END_RECEIVE_SYSTEMUI";
            case BEGIN_SEND_LAYOUT:
                return "BEGIN_SEND_LAYOUT";
            case RUN_HAL_OPERATIONS_WAITING_DOZE_OR_ON:
                return "RUN_HAL_OPERATIONS_WAITING_DOZE_OR_ON";
            case END_SEND_LAYOUT:
                return "END_SEND_LAYOUT";
            case START_DISPLAY_CONTROL:
                return "START_DISPLAY_CONTROL";
            case END_DISPLAY_CONTROL:
                return "END_DISPLAY_CONTROL";
            case ACTIVITY_CHANGED:
                return "ACTIVITY_CHANGED";
            case WATCHFACE_CHANGED:
                return "WATCHFACE_CHANGED";
            case OFFLOAD_UPDATE_DOZE_INTERVAL:
                return "OFFLOAD_UPDATE_DOZE_INTERVAL";
            case UPDATE_LOCALE:
                return "UPDATE_LOCALE";
            case HAL_CONNECTED:
                return "HAL_CONNECTED";
            case AMBIENT_STATE_CHANGED:
                return "AMBIENT_STATE_CHANGED";
            case END_DISPLAY_MCU_RENDERING:
                return "END_DISPLAY_MCU_RENDERING";
            case DisplayOffloadService.HAL_DIED:
                return "HAL_DIED";
            default:
                return "!!!INVALID!!!";
        }
    }

    static String nameForLayoutType(@DisplayOffloadService.LayoutType int type) {
        switch (type) {
            case NONE:
                return "NONE";
            case WATCHFACE:
                return "WATCHFACE";
            case ACTIVITY:
                return "ACTIVITY";
            case SYSTEMUI:
                return "SYSTEMUI";
            default:
                return "!!!INVALID!!!";
        }
    }
    // LINT.ThenChange(DisplayOffloadService.java:events_int_def)
}
