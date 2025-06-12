/*
 * Copyright (C) 2007 The Android Open Source Project
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

package android.content;

import android.annotation.Nullable;
import android.compat.annotation.UnsupportedAppUsage;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.util.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This class is used to store a set of values that the {@link ContentResolver}
 * can process.
 */
@android.ravenwood.annotation.RavenwoodKeepWholeClass
public final class ContentValues implements Parcelable {
    public static final String TAG = "ContentValues";

    /**
     * @hide
     * @deprecated kept around for lame people doing reflection
     */
    @Deprecated
    @UnsupportedAppUsage
    private HashMap<String, Object> mValues;

    private final ArrayMap<String, Object> mMap;

    /**
     * Creates an empty set of values using the default initial size
     */
    public ContentValues() {
        mMap = new ArrayMap<>();
    }

    /**
     * Creates an empty set of values using the given initial size
     *
     * @param size the initial size of the set of values
     */
    public ContentValues(int size) {
        Preconditions.checkArgumentNonnegative(size);
        mMap = new ArrayMap<>(size);
    }

    /**
     * Creates a set of values copied from the given set
     *
     * @param from the values to copy
     */
    public ContentValues(ContentValues from) {
        Objects.requireNonNull(from);
        mMap = new ArrayMap<>(from.mMap);
    }

    /**
     * @hide
     * @deprecated kept around for lame people doing reflection
     */
    @Deprecated
    @UnsupportedAppUsage
    private ContentValues(HashMap<String, Object> from) {
        mMap = new ArrayMap<>();
        mMap.putAll(from);
    }

    /** {@hide} */
    private ContentValues(Parcel in) {
        mMap = new ArrayMap<>(in.readInt());
        in.readArrayMap(mMap, null);
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (!(object instanceof ContentValues)) {
            return false;
        }
        return mMap.equals(((ContentValues) object).mMap);
    }

    /** {@hide} */
    public ArrayMap<String, Object> getValues() {
        return mMap;
    }

    @Override
    public int hashCode() {
        return mMap.hashCode();
    }

    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    public void put(String key, String value) {
        mMap.put(key, value);
    }

    /**
     * Adds all values from the passed in ContentValues.
     *
     * @param other the ContentValues from which to copy
     */
    public void putAll(ContentValues other) {
        mMap.putAll(other.mMap);
    }

    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    public void put(String key, Byte value) {
        mMap.put(key, value);
    }

    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    public void put(String key, Short value) {
        mMap.put(key, value);
    }

    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    public void put(String key, Integer value) {
        mMap.put(key, value);
    }

    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    public void put(String key, Long value) {
        mMap.put(key, value);
    }

    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    public void put(String key, Float value) {
        mMap.put(key, value);
    }

    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    public void put(String key, Double value) {
        mMap.put(key, value);
    }

    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    public void put(String key, Boolean value) {
        mMap.put(key, value);
    }

    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    public void put(String key, byte[] value) {
        mMap.put(key, value);
    }

    /**
     * Adds a null value to the set.
     *
     * @param key the name of the value to make null
     */
    public void putNull(String key) {
        mMap.put(key, null);
    }

    /** {@hide} */
    public void putObject(@Nullable String key, @Nullable Object value) {
        if (value == null) {
            putNull(key);
        } else if (value instanceof String) {
            put(key, (String) value);
        } else if (value instanceof Byte) {
            put(key, (Byte) value);
        } else if (value instanceof Short) {
            put(key, (Short) value);
        } else if (value instanceof Integer) {
            put(key, (Integer) value);
        } else if (value instanceof Long) {
            put(key, (Long) value);
        } else if (value instanceof Float) {
            put(key, (Float) value);
        } else if (value instanceof Double) {
            put(key, (Double) value);
        } else if (value instanceof Boolean) {
            put(key, (Boolean) value);
        } else if (value instanceof byte[]) {
            put(key, (byte[]) value);
        } else {
            throw new IllegalArgumentException("Unsupported type " + value.getClass());
        }
    }

    /**
     * Returns the number of values.
     *
     * @return the number of values
     */
    public int size() {
        return mMap.size();
    }

    /**
     * Indicates whether this collection is empty.
     *
     * @return true iff size == 0
     */
    public boolean isEmpty() {
        return mMap.isEmpty();
    }

    /**
     * Remove a single value.
     *
     * @param key the name of the value to remove
     */
    public void remove(String key) {
        mMap.remove(key);
    }

    /**
     * Removes all values.
     */
    public void clear() {
        mMap.clear();
    }

    /**
     * Returns true if this object has the named value.
     *
     * @param key the value to check for
     * @return {@code true} if the value is present, {@code false} otherwise
     */
    public boolean containsKey(String key) {
        return mMap.containsKey(key);
    }

    /**
     * Gets a value. Valid value types are {@link String}, {@link Boolean},
     * {@link Number}, and {@code byte[]} implementations.
     *
     * @param key the value to get
     * @return the data for the value, or {@code null} if the value is missing or if {@code null}
     *         was previously added with the given {@code key}
     */
    public Object get(String key) {
        return mMap.get(key);
    }

    /**
     * Gets a value and converts it to a String.
     *
     * @param key the value to get
     * @return the String for the value
     */
    public String getAsString(String key) {
        Object value = mMap.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Gets a value and converts it to a Long.
     *
     * @param key the value to get
     * @return the Long value, or {@code null} if the value is missing or cannot be converted
     */
    public Long getAsLong(String key) {
        Object value = mMap.get(key);
        try {
            return value != null ? ((Number) value).longValue() : null;
        } catch (ClassCastException e) {
            if (value instanceof CharSequence) {
                try {
                    return Long.valueOf(value.toString());
                } catch (NumberFormatException e2) {
                    Log.e(TAG, "Cannot parse Long value for " + value + " at key " + key);
                    return null;
                }
            } else {
                Log.e(TAG, "Cannot cast value for " + key + " to a Long: " + value, e);
                return null;
            }
        }
    }

    /**
     * Gets a value and converts it to an Integer.
     *
     * @param key the value to get
     * @return the Integer value, or {@code null} if the value is missing or cannot be converted
     */
    public Integer getAsInteger(String key) {
        Object value = mMap.get(key);
        try {
            return value != null ? ((Number) value).intValue() : null;
        } catch (ClassCastException e) {
            if (value instanceof CharSequence) {
                try {
                    return Integer.valueOf(value.toString());
                } catch (NumberFormatException e2) {
                    Log.e(TAG, "Cannot parse Integer value for " + value + " at key " + key);
                    return null;
                }
            } else {
                Log.e(TAG, "Cannot cast value for " + key + " to a Integer: " + value, e);
                return null;
            }
        }
    }

    /**
     * Gets a value and converts it to a Short.
     *
     * @param key the value to get
     * @return the Short value, or {@code null} if the value is missing or cannot be converted
     */
    public Short getAsShort(String key) {
        Object value = mMap.get(key);
        try {
            return value != null ? ((Number) value).shortValue() : null;
        } catch (ClassCastException e) {
            if (value instanceof CharSequence) {
                try {
                    return Short.valueOf(value.toString());
                } catch (NumberFormatException e2) {
                    Log.e(TAG, "Cannot parse Short value for " + value + " at key " + key);
                    return null;
                }
            } else {
                Log.e(TAG, "Cannot cast value for " + key + " to a Short: " + value, e);
                return null;
            }
        }
    }

    /**
     * Gets a value and converts it to a Byte.
     *
     * @param key the value to get
     * @return the Byte value, or {@code null} if the value is missing or cannot be converted
     */
    public Byte getAsByte(String key) {
        Object value = mMap.get(key);
        try {
            return value != null ? ((Number) value).byteValue() : null;
        } catch (ClassCastException e) {
            if (value instanceof CharSequence) {
                try {
                    return Byte.valueOf(value.toString());
                } catch (NumberFormatException e2) {
                    Log.e(TAG, "Cannot parse Byte value for " + value + " at key " + key);
                    return null;
                }
            } else {
                Log.e(TAG, "Cannot cast value for " + key + " to a Byte: " + value, e);
                return null;
            }
        }
    }

    /**
     * Gets a value and converts it to a Double.
     *
     * @param key the value to get
     * @return the Double value, or {@code null} if the value is missing or cannot be converted
     */
    public Double getAsDouble(String key) {
        Object value = mMap.get(key);
        try {
            return value != null ? ((Number) value).doubleValue() : null;
        } catch (ClassCastException e) {
            if (value instanceof CharSequence) {
                try {
                    return Double.valueOf(value.toString());
                } catch (NumberFormatException e2) {
                    Log.e(TAG, "Cannot parse Double value for " + value + " at key " + key);
                    return null;
                }
            } else {
                Log.e(TAG, "Cannot cast value for " + key + " to a Double: " + value, e);
                return null;
            }
        }
    }

    /**
     * Gets a value and converts it to a Float.
     *
     * @param key the value to get
     * @return the Float value, or {@code null} if the value is missing or cannot be converted
     */
    public Float getAsFloat(String key) {
        Object value = mMap.get(key);
        try {
            return value != null ? ((Number) value).floatValue() : null;
        } catch (ClassCastException e) {
            if (value instanceof CharSequence) {
                try {
                    return Float.valueOf(value.toString());
                } catch (NumberFormatException e2) {
                    Log.e(TAG, "Cannot parse Float value for " + value + " at key " + key);
                    return null;
                }
            } else {
                Log.e(TAG, "Cannot cast value for " + key + " to a Float: " + value, e);
                return null;
            }
        }
    }

    /**
     * Gets a value and converts it to a Boolean.
     *
     * @param key the value to get
     * @return the Boolean value, or {@code null} if the value is missing or cannot be converted
     */
    public Boolean getAsBoolean(String key) {
        Object value = mMap.get(key);
        try {
            return (Boolean) value;
        } catch (ClassCastException e) {
            if (value instanceof CharSequence) {
                // Note that we also check against 1 here because SQLite's internal representation
                // for booleans is an integer with a value of 0 or 1. Without this check, boolean
                // values obtained via DatabaseUtils#cursorRowToContentValues will always return
                // false.
                return Boolean.valueOf(value.toString()) || "1".equals(value);
            } else if (value instanceof Number) {
                return ((Number) value).intValue() != 0;
            } else {
                Log.e(TAG, "Cannot cast value for " + key + " to a Boolean: " + value, e);
                return null;
            }
        }
    }

    /**
     * Gets a value that is a byte array. Note that this method will not convert
     * any other types to byte arrays.
     *
     * @param key the value to get
     * @return the {@code byte[]} value, or {@code null} is the value is missing or not a
     *         {@code byte[]}
     */
    public byte[] getAsByteArray(String key) {
        Object value = mMap.get(key);
        if (value instanceof byte[]) {
            return (byte[]) value;
        } else {
            return null;
        }
    }

    /**
     * Returns a set of all of the keys and values
     *
     * @return a set of all of the keys and values
     */
    public Set<Map.Entry<String, Object>> valueSet() {
        return mMap.entrySet();
    }

    /**
     * Returns a set of all of the keys
     *
     * @return a set of all of the keys
     */
    public Set<String> keySet() {
        return mMap.keySet();
    }

    public static final @android.annotation.NonNull Parcelable.Creator<ContentValues> CREATOR =
            new Parcelable.Creator<ContentValues>() {
        @Override
        public ContentValues createFromParcel(Parcel in) {
            return new ContentValues(in);
        }

        @Override
        public ContentValues[] newArray(int size) {
            return new ContentValues[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mMap.size());
        parcel.writeArrayMap(mMap);
    }

    /**
     * Unsupported, here until we get proper bulk insert APIs.
     * {@hide}
     */
    @Deprecated
    @UnsupportedAppUsage
    public void putStringArrayList(String key, ArrayList<String> value) {
        mMap.put(key, value);
    }

    /**
     * Unsupported, here until we get proper bulk insert APIs.
     * {@hide}
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    @UnsupportedAppUsage
    public ArrayList<String> getStringArrayList(String key) {
        return (ArrayList<String>) mMap.get(key);
    }

    /**
     * Returns a string containing a concise, human-readable description of this object.
     * @return a printable representation of this object.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String name : mMap.keySet()) {
            String value = getAsString(name);
            if (sb.length() > 0) sb.append(" ");
            sb.append(name + "=" + value);
        }
        return sb.toString();
    }

    /** {@hide} */
    public static boolean isSupportedValue(Object value) {
        if (value == null) {
            return true;
        } else if (value instanceof String) {
            return true;
        } else if (value instanceof Byte) {
            return true;
        } else if (value instanceof Short) {
            return true;
        } else if (value instanceof Integer) {
            return true;
        } else if (value instanceof Long) {
            return true;
        } else if (value instanceof Float) {
            return true;
        } else if (value instanceof Double) {
            return true;
        } else if (value instanceof Boolean) {
            return true;
        } else if (value instanceof byte[]) {
            return true;
        } else {
            return false;
        }
    }
}
