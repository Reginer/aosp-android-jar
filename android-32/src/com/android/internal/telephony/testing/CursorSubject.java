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

package com.android.internal.telephony.testing;

import static com.google.common.truth.Truth.assertAbout;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.DatabaseUtils;

import androidx.annotation.Nullable;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.IntegerSubject;
import com.google.common.truth.IterableSubject;
import com.google.common.truth.StringSubject;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;

import java.util.ArrayList;
import java.util.List;

/** Truth subject for making assertions about {@link Cursor}s. */
public final class CursorSubject extends Subject {

    private final Cursor mActual;

    private CursorSubject(FailureMetadata metadata, @Nullable Cursor actual) {
        super(metadata, new StringableCursor(actual));
        this.mActual = new StringableCursor(actual);
    }

    /** Returns the factory for this subject. */
    public static Factory<CursorSubject, Cursor> cursors() {
        return CursorSubject::new;
    }

    /** Starts an assertion. */
    public static CursorSubject assertThat(Cursor cursor) {
        return assertAbout(cursors()).that(cursor);
    }

    /** Asserts {@link Cursor#getCount()} has the specified value. */
    public void hasCount(int count) {
        check("getCount()").that(mActual.getCount()).isEqualTo(count);
    }

    /** Asserts {@link Cursor#getColumnNames()} match those specified. */
    public void hasColumnNames(String... columnNames) {
        check("getColumnNames()").that(mActual.getColumnNames()).asList()
                .containsExactlyElementsIn(columnNames).inOrder();
    }

    /** Positions the cursor under test at the specified row to make an assertion about it. */
    public CursorSubject atRow(int position) {
        check("moveToPosition").that(mActual.moveToPosition(position)).isTrue();
        return this;
    }

    /** Asserts that the row at the cursor's current position has the specified values. */
    public CursorSubject hasRowValues(Object... values) {
        check("getColumnCount()").that(mActual.getColumnCount()).isEqualTo(values.length);
        ContentValues expectedValues = new ContentValues();
        for (int i = 0; i < values.length; i++) {
            expectedValues.put(mActual.getColumnName(i), values[i].toString());
        }

        ContentValues actualValues = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(mActual, actualValues);

        check("Row: %s", mActual.getPosition()).that(actualValues).isEqualTo(expectedValues);
        return this;
    }

    /** Asserts that the cursor has a single row with the specified values. */
    public void hasSingleRow(Object... values) {
        hasCount(1);
        atRow(0).hasRowValues(values);
    }

    /**
     * Asserts that the row at the cursor's current position has the specified value for the
     * specified column.
     */
    public CursorSubject hasRowValue(String columnName, Object value) {
        int index = mActual.getColumnIndex(columnName);
        check("getColumnIndex()").that(index).isNotEqualTo(-1);

        check("Row[%s]: %s", columnName, index).that(mActual.getString(index))
                .isEqualTo(value.toString());
        return this;
    }

    /** Starts an assertion about the value of the specified column for the current row. */
    public IntegerSubject intField(String columnName) {
        int index = mActual.getColumnIndex(columnName);
        check("getColumnIndex()").that(index).isNotEqualTo(-1);
        check("getType()").that(mActual.getType(index)).isEqualTo(Cursor.FIELD_TYPE_INTEGER);

        return check("getInt()").that(mActual.getInt(index));
    }

    /** Starts an assertion about the value of the specified column for the current row. */
    public StringSubject stringField(String columnName) {
        int index = mActual.getColumnIndex(columnName);
        check("getColumnIndex()").that(index).isNotEqualTo(-1);
        check("getType()").that(mActual.getType(index)).isEqualTo(Cursor.FIELD_TYPE_STRING);

        return check("getString()").that(mActual.getString(index));
    }

    /** Asserts that the cursor rows match the data specified. */
    public void hasData(Object[][] rows) {
        hasCount(rows.length);
        for (int i = 0; i < rows.length; i++) {
            atRow(i).hasRowValues(rows[i]);
        }
    }

    /** Starts an assertion about the cursor's rows. */
    public IterableSubject asLists() {
        List<List<String>> result = new ArrayList<>();
        mActual.moveToPosition(-1);
        while (mActual.moveToNext()) {
            List<String> row = new ArrayList<>();
            for (int i = 0; i < mActual.getColumnCount(); i++) {
                row.add(mActual.getString(i));
            }
            result.add(row);
        }
        return Truth.assertThat(result);
    }

    private static class StringableCursor extends CursorWrapper {

        StringableCursor(Cursor cursor) {
            super(cursor);
        }

        @Override
        public String toString() {
            return DatabaseUtils.dumpCursorToString(getWrappedCursor());
        }
    }

}
