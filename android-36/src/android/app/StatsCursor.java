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

package android.app;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.annotation.SuppressLint;
import android.database.AbstractCursor;
import android.database.MatrixCursor;

/**
 * Custom cursor implementation to hold a cross-process cursor to pass data to caller.
 *
 * @hide
 */
@SystemApi
public class StatsCursor extends AbstractCursor {
    private final MatrixCursor mMatrixCursor;
    private final int[] mColumnTypes;
    private final String[] mColumnNames;
    private final int mRowCount;

    /**
     * @hide
     **/
    public StatsCursor(String[] queryData, String[] columnNames, int[] columnTypes, int rowCount) {
        mColumnTypes = columnTypes;
        mColumnNames = columnNames;
        mRowCount = rowCount;
        mMatrixCursor = new MatrixCursor(columnNames);
        for (int i = 0; i < rowCount; i++) {
            MatrixCursor.RowBuilder builder = mMatrixCursor.newRow();
            for (int j = 0; j < columnNames.length; j++) {
                int dataIndex = i * columnNames.length + j;
                builder.add(columnNames[j], queryData[dataIndex]);
            }
        }
    }

    /**
     * Returns the numbers of rows in the cursor.
     *
     * @return the number of rows in the cursor.
     */
    @Override
    public int getCount() {
        return mRowCount;
    }

    /**
     * Returns a string array holding the names of all of the columns in the
     * result set in the order in which they were listed in the result.
     *
     * @return the names of the columns returned in this query.
     */
    @Override
    @NonNull
    public String[] getColumnNames() {
        return mColumnNames;
    }

    /**
     * Returns the value of the requested column as a String.
     *
     * @param column the zero-based index of the target column.
     * @return the value of that column as a String.
     */
    @Override
    @NonNull
    public String getString(int column) {
        return mMatrixCursor.getString(column);
    }

    /**
     * Returns the value of the requested column as a short.
     *
     * @param column the zero-based index of the target column.
     * @return the value of that column as a short.
     */
    @Override
    @SuppressLint("NoByteOrShort")
    public short getShort(int column) {
        return mMatrixCursor.getShort(column);
    }

    /**
     * Returns the value of the requested column as an int.
     *
     * @param column the zero-based index of the target column.
     * @return the value of that column as an int.
     */
    @Override
    public int getInt(int column) {
        return mMatrixCursor.getInt(column);
    }

    /**
     * Returns the value of the requested column as a long.
     *
     * @param column the zero-based index of the target column.
     * @return the value of that column as a long.
     */
    @Override
    public long getLong(int column) {
        return mMatrixCursor.getLong(column);
    }

    /**
     * Returns the value of the requested column as a float.
     *
     * @param column the zero-based index of the target column.
     * @return the value of that column as a float.
     */
    @Override
    public float getFloat(int column) {
        return mMatrixCursor.getFloat(column);
    }

    /**
     * Returns the value of the requested column as a double.
     *
     * @param column the zero-based index of the target column.
     * @return the value of that column as a double.
     */
    @Override
    public double getDouble(int column) {
        return mMatrixCursor.getDouble(column);
    }

    /**
     * Returns <code>true</code> if the value in the indicated column is null.
     *
     * @param column the zero-based index of the target column.
     * @return whether the column value is null.
     */
    @Override
    public boolean isNull(int column) {
        return mMatrixCursor.isNull(column);
    }

    /**
     * Returns the data type of the given column's value.
     *
     * @param column the zero-based index of the target column.
     * @return column value type
     */
    @Override
    public int getType(int column) {
        return mColumnTypes[column];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        return mMatrixCursor.moveToPosition(newPosition);
    }
}
