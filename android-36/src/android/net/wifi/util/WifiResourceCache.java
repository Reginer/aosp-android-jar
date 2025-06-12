/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.net.wifi.util;

import android.content.Context;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class to cache the Wifi resource value and provide override mechanism from shell
 * @hide
 */
public class WifiResourceCache {
    private static final String TAG = "WifiResourceCache";
    private final Context mContext;

    private final Map<Integer, Boolean> mBooleanResourceMap;
    private final Map<Integer, Integer> mIntegerResourceMap;
    private final Map<Integer, String> mStringResourceMap;
    private final Map<Integer, String[]> mStringArrayResourceMap;
    private final Map<Integer, int[]> mIntArrayResourceMap;
    private final List<Map> mValueMapList;
    private final Map<String, Integer> mResourceNameMap;

    private int mTempId = -1;
    private final Object mLock = new Object();

    public WifiResourceCache(Context context) {
        mContext = context;
        mBooleanResourceMap = new HashMap<>();
        mIntegerResourceMap = new HashMap<>();
        mStringArrayResourceMap = new HashMap<>();
        mIntArrayResourceMap = new HashMap<>();
        mStringResourceMap = new HashMap<>();
        mValueMapList = List.of(mBooleanResourceMap, mIntegerResourceMap, mIntArrayResourceMap,
                mStringArrayResourceMap, mStringResourceMap);
        mResourceNameMap = new HashMap<>();
    }

    /**
     * Get and cache the boolean value as {@link android.content.res.Resources#getBoolean(int)}
     */
    public boolean getBoolean(int resourceId) {
        synchronized (mLock) {
            if (mBooleanResourceMap.containsKey(resourceId)) {
                return mBooleanResourceMap.get(resourceId);
            }

            String resourceName = mContext.getResources().getResourceEntryName(resourceId);
            if (mResourceNameMap.containsKey(resourceName)) {
                int tempId = mResourceNameMap.get(resourceName);
                if (mBooleanResourceMap.containsKey(tempId)) {
                    boolean value = mBooleanResourceMap.get(tempId);
                    mBooleanResourceMap.put(resourceId, value);
                    mBooleanResourceMap.remove(tempId);
                    mResourceNameMap.put(resourceName, resourceId);
                    return value;
                }
            }
            mResourceNameMap.put(resourceName, resourceId);
            return mBooleanResourceMap.computeIfAbsent(resourceId,
                    v -> mContext.getResources().getBoolean(resourceId));
        }
    }

    /**
     * Get and cache the integer value as {@link android.content.res.Resources#getInteger(int)}
     */
    public int getInteger(int resourceId) {
        synchronized (mLock) {
            if (mIntegerResourceMap.containsKey(resourceId)) {
                return mIntegerResourceMap.get(resourceId);
            }

            String resourceName = mContext.getResources().getResourceEntryName(resourceId);
            if (mResourceNameMap.containsKey(resourceName)) {
                int tempId = mResourceNameMap.get(resourceName);
                if (mIntegerResourceMap.containsKey(tempId)) {
                    int value = mIntegerResourceMap.get(tempId);
                    mIntegerResourceMap.put(resourceId, value);
                    mIntegerResourceMap.remove(tempId);
                    mResourceNameMap.put(resourceName, resourceId);
                    return value;
                }
            }
            mResourceNameMap.put(resourceName, resourceId);
            return mIntegerResourceMap.computeIfAbsent(resourceId,
                    v -> mContext.getResources().getInteger(resourceId));
        }
    }

    /**
     * Get and cache the integer value as {@link android.content.res.Resources#getString(int)}
     */
    public String getString(int resourceId) {
        synchronized (mLock) {
            if (mStringResourceMap.containsKey(resourceId)) {
                return mStringResourceMap.get(resourceId);
            }

            String resourceName = mContext.getResources().getResourceEntryName(resourceId);
            if (mResourceNameMap.containsKey(resourceName)) {
                int tempId = mResourceNameMap.get(resourceName);
                if (mStringResourceMap.containsKey(tempId)) {
                    String value = mStringResourceMap.get(tempId);
                    mStringResourceMap.put(resourceId, value);
                    mStringResourceMap.remove(tempId);
                    mResourceNameMap.put(resourceName, resourceId);
                    return value;
                }
            }
            mResourceNameMap.put(resourceName, resourceId);
            return mStringResourceMap.computeIfAbsent(resourceId,
                    v -> mContext.getResources().getString(resourceId));
        }
    }

    /**
     * Get and cache the integer value as {@link android.content.res.Resources#getStringArray(int)}
     */
    public String[] getStringArray(int resourceId) {
        synchronized (mLock) {
            if (mStringArrayResourceMap.containsKey(resourceId)) {
                return mStringArrayResourceMap.get(resourceId);
            }

            String resourceName = mContext.getResources().getResourceEntryName(resourceId);
            if (mResourceNameMap.containsKey(resourceName)) {
                int tempId = mResourceNameMap.get(resourceName);
                if (mStringArrayResourceMap.containsKey(tempId)) {
                    String[] value = mStringArrayResourceMap.get(tempId);
                    mStringArrayResourceMap.put(resourceId, value);
                    mStringArrayResourceMap.remove(tempId);
                    mResourceNameMap.put(resourceName, resourceId);
                    return value;
                }
            }
            mResourceNameMap.put(resourceName, resourceId);
            return mStringArrayResourceMap.computeIfAbsent(resourceId,
                    v -> mContext.getResources().getStringArray(resourceId));
        }
    }

    /**
     * Get and cache the integer value as {@link android.content.res.Resources#getIntArray(int)}
     */
    public int[] getIntArray(int resourceId) {
        synchronized (mLock) {
            if (mIntArrayResourceMap.containsKey(resourceId)) {
                return mIntArrayResourceMap.get(resourceId);
            }

            String resourceName = mContext.getResources().getResourceEntryName(resourceId);
            if (mResourceNameMap.containsKey(resourceName)) {
                int tempId = mResourceNameMap.get(resourceName);
                if (mIntArrayResourceMap.containsKey(tempId)) {
                    int[] value = mIntArrayResourceMap.get(tempId);
                    mIntArrayResourceMap.put(resourceId, value);
                    mIntArrayResourceMap.remove(tempId);
                    mResourceNameMap.put(resourceName, resourceId);
                    return value;
                }
            }
            mResourceNameMap.put(resourceName, resourceId);
            return mIntArrayResourceMap.computeIfAbsent(resourceId,
                    v -> mContext.getResources().getIntArray(resourceId));
        }
    }

    /**
     * Override the target boolean value
     *
     * @param resourceName the resource overlay name
     * @param value        override to this value
     */
    public void overrideBooleanValue(String resourceName, boolean value) {
        synchronized (mLock) {
            int resourceId = mResourceNameMap.computeIfAbsent(resourceName, v -> mTempId--);
            mBooleanResourceMap.put(resourceId, value);
        }
    }

    /**
     * Override the target boolean value
     */
    public void restoreBooleanValue(String resourceName) {
        synchronized (mLock) {
            mBooleanResourceMap.remove(mResourceNameMap.remove(resourceName));
        }
    }

    /**
     * Override the target integer value
     * @param resourceName the resource overlay name
     * @param value override to this value
     */
    public void overrideIntegerValue(String resourceName, int value) {
        synchronized (mLock) {
            int resourceId = mResourceNameMap.computeIfAbsent(resourceName, v -> mTempId--);
            mIntegerResourceMap.put(resourceId, value);
        }
    }

    /**
     * Override the target integer value
     */
    public void restoreIntegerValue(String resourceName) {
        synchronized (mLock) {
            mIntegerResourceMap.remove(mResourceNameMap.remove(resourceName));
        }
    }

    /**
     * Override the target String value
     * @param resourceName the resource overlay name
     * @param value override to this value
     */
    public void overrideStringValue(String resourceName, String value) {
        synchronized (mLock) {
            int resourceId = mResourceNameMap.computeIfAbsent(resourceName, v -> mTempId--);
            mStringResourceMap.put(resourceId, value);
        }
    }

    /**
     * Override the target integer value
     */
    public void restoreStringValue(String resourceName) {
        synchronized (mLock) {
            mStringResourceMap.remove(mResourceNameMap.remove(resourceName));
        }
    }

    /**
     * Override the target string array value
     * @param resourceName the resource overlay name
     * @param value override to this value
     */
    public void overrideStringArrayValue(String resourceName, String[] value) {
        synchronized (mLock) {
            int resourceId = mResourceNameMap.computeIfAbsent(resourceName, v -> mTempId--);
            mStringArrayResourceMap.put(resourceId, value);
        }
    }

    /**
     * Override the target string array value
     */
    public void restoreStringArrayValue(String resourceName) {
        synchronized (mLock) {
            mStringArrayResourceMap.remove(mResourceNameMap.remove(resourceName));
        }
    }

    /**
     * Override the target int array value
     * @param resourceName the resource overlay name
     * @param value override to this value
     */
    public void overrideIntArrayValue(String resourceName, int[] value) {
        synchronized (mLock) {
            int resourceId = mResourceNameMap.computeIfAbsent(resourceName, v -> mTempId--);
            mIntArrayResourceMap.put(resourceId, value);
        }
    }

    /**
     * Override the target int array value
     */
    public void restoreIntArrayValue(String resourceName) {
        synchronized (mLock) {
            mIntArrayResourceMap.remove(mResourceNameMap.remove(resourceName));
        }
    }

    /**
     * Dump of current resource value
     */
    public void dump(PrintWriter pw) {
        pw.println("Dump of WifiResourceCache");
        pw.println("WifiResourceCache - resource value Begin ----");
        synchronized (mLock) {
            for (Map.Entry<String, Integer> resourceEntry : mResourceNameMap.entrySet()) {
                for (Map m : mValueMapList) {
                    if (m.containsKey(resourceEntry.getValue())) {
                        pw.println("Resource Name: " + resourceEntry.getKey()
                                + ", value: " + valueToString(m.get(resourceEntry.getValue())));
                        break;
                    }
                }
            }
        }
        pw.println("WifiResourceCache - resource value End ----");
    }

    private String valueToString(Object input) {
        if (input instanceof Object[]) {
            return Arrays.deepToString((Object[]) input);
        }
        return input.toString();
    }

    /**
     * Remove all override value and set to default
     */
    public void reset() {
        synchronized (mLock) {
            for (Map m : mValueMapList) {
                m.clear();
            }
            mResourceNameMap.clear();
        }
    }

    /**
     * Handle the locale change to apply the translation
     */
    public void handleLocaleChange() {
        mStringResourceMap.clear();
        mStringArrayResourceMap.clear();
    }
}
