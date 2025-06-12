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

package android.widget;

import android.annotation.IntDef;
import android.annotation.Nullable;
import android.annotation.TestApi;
import android.annotation.Widget;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewStructure;
import android.view.accessibility.AccessibilityEvent;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.view.inspector.InspectableProperty;

import com.android.internal.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

/**
 * Provides a widget for selecting a date.
 * <p>
 * When the {@link android.R.styleable#DatePicker_datePickerMode} attribute is
 * set to {@code spinner}, the date can be selected using year, month, and day
 * spinners or a {@link CalendarView}. The set of spinners and the calendar
 * view are automatically synchronized. The client can customize whether only
 * the spinners, or only the calendar view, or both to be displayed.
 * </p>
 * <p>
 * When the {@link android.R.styleable#DatePicker_datePickerMode} attribute is
 * set to {@code calendar}, the month and day can be selected using a
 * calendar-style view while the year can be selected separately using a list.
 * </p>
 * <p>
 * See the <a href="{@docRoot}guide/topics/ui/controls/pickers.html">Pickers</a>
 * guide.
 * </p>
 * <p>
 * For a dialog using this view, see {@link android.app.DatePickerDialog}.
 * </p>
 *
 * @attr ref android.R.styleable#DatePicker_startYear
 * @attr ref android.R.styleable#DatePicker_endYear
 * @attr ref android.R.styleable#DatePicker_maxDate
 * @attr ref android.R.styleable#DatePicker_minDate
 * @attr ref android.R.styleable#DatePicker_spinnersShown
 * @attr ref android.R.styleable#DatePicker_calendarViewShown
 * @attr ref android.R.styleable#DatePicker_dayOfWeekBackground
 * @attr ref android.R.styleable#DatePicker_dayOfWeekTextAppearance
 * @attr ref android.R.styleable#DatePicker_headerBackground
 * @attr ref android.R.styleable#DatePicker_headerMonthTextAppearance
 * @attr ref android.R.styleable#DatePicker_headerDayOfMonthTextAppearance
 * @attr ref android.R.styleable#DatePicker_headerYearTextAppearance
 * @attr ref android.R.styleable#DatePicker_yearListItemTextAppearance
 * @attr ref android.R.styleable#DatePicker_yearListSelectorColor
 * @attr ref android.R.styleable#DatePicker_calendarTextColor
 * @attr ref android.R.styleable#DatePicker_datePickerMode
 */
@Widget
public class DatePicker extends FrameLayout {
    private static final String LOG_TAG = DatePicker.class.getSimpleName();

    /**
     * Presentation mode for the Holo-style date picker that uses a set of
     * {@link android.widget.NumberPicker}s.
     *
     * @see #getMode()
     * @hide Visible for testing only.
     */
    @TestApi
    public static final int MODE_SPINNER = 1;

    /**
     * Presentation mode for the Material-style date picker that uses a
     * calendar.
     *
     * @see #getMode()
     * @hide Visible for testing only.
     */
    @TestApi
    public static final int MODE_CALENDAR = 2;

    /** @hide */
    @IntDef(prefix = { "MODE_" }, value = {
            MODE_SPINNER,
            MODE_CALENDAR
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface DatePickerMode {}

    @UnsupportedAppUsage
    private final DatePickerDelegate mDelegate;

    @DatePickerMode
    private final int mMode;

    /**
     * The callback used to indicate the user changed the date.
     */
    public interface OnDateChangedListener {

        /**
         * Called upon a date change.
         *
         * @param view The view associated with this listener.
         * @param year The year that was set.
         * @param monthOfYear The month that was set (0-11) for compatibility
         *            with {@link java.util.Calendar}.
         * @param dayOfMonth The day of the month that was set.
         */
        void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth);
    }

    public DatePicker(Context context) {
        this(context, null);
    }

    public DatePicker(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.datePickerStyle);
    }

    public DatePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DatePicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        // DatePicker is important by default, unless app developer overrode attribute.
        if (getImportantForAutofill() == IMPORTANT_FOR_AUTOFILL_AUTO) {
            setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_YES);
        }

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DatePicker,
                defStyleAttr, defStyleRes);
        saveAttributeDataForStyleable(context, R.styleable.DatePicker,
                attrs, a, defStyleAttr, defStyleRes);
        final boolean isDialogMode = a.getBoolean(R.styleable.DatePicker_dialogMode, false);
        final int requestedMode = a.getInt(R.styleable.DatePicker_datePickerMode, MODE_SPINNER);
        final int firstDayOfWeek = a.getInt(R.styleable.DatePicker_firstDayOfWeek, 0);
        a.recycle();

        if (requestedMode == MODE_CALENDAR && isDialogMode) {
            // You want MODE_CALENDAR? YOU CAN'T HANDLE MODE_CALENDAR! Well,
            // maybe you can depending on your screen size. Let's check...
            mMode = context.getResources().getInteger(R.integer.date_picker_mode);
        } else {
            mMode = requestedMode;
        }

        switch (mMode) {
            case MODE_CALENDAR:
                mDelegate = createCalendarUIDelegate(context, attrs, defStyleAttr, defStyleRes);
                break;
            case MODE_SPINNER:
            default:
                mDelegate = createSpinnerUIDelegate(context, attrs, defStyleAttr, defStyleRes);
                break;
        }

        if (firstDayOfWeek != 0) {
            setFirstDayOfWeek(firstDayOfWeek);
        }

        mDelegate.setAutoFillChangeListener((v, y, m, d) -> {
            final AutofillManager afm = context.getSystemService(AutofillManager.class);
            if (afm != null) {
                afm.notifyValueChanged(this);
            }
        });
    }

    private DatePickerDelegate createSpinnerUIDelegate(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        return new DatePickerSpinnerDelegate(this, context, attrs, defStyleAttr, defStyleRes);
    }

    private DatePickerDelegate createCalendarUIDelegate(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        return new DatePickerCalendarDelegate(this, context, attrs, defStyleAttr,
                defStyleRes);
    }

    /**
     * @return the picker's presentation mode, one of {@link #MODE_CALENDAR} or
     *         {@link #MODE_SPINNER}
     * @attr ref android.R.styleable#DatePicker_datePickerMode
     * @hide Visible for testing only.
     */
    @InspectableProperty(name = "datePickerMode", enumMapping = {
            @InspectableProperty.EnumEntry(value = MODE_SPINNER, name = "spinner"),
            @InspectableProperty.EnumEntry(value = MODE_CALENDAR, name = "calendar")
    })
    @DatePickerMode
    @TestApi
    public int getMode() {
        return mMode;
    }

    /**
     * Initialize the state. If the provided values designate an inconsistent
     * date the values are normalized before updating the spinners.
     *
     * @param year The initial year.
     * @param monthOfYear The initial month <strong>starting from zero</strong>.
     * @param dayOfMonth The initial day of the month.
     * @param onDateChangedListener How user is notified date is changed by
     *            user, can be null.
     */
    public void init(int year, int monthOfYear, int dayOfMonth,
                     OnDateChangedListener onDateChangedListener) {
        mDelegate.init(year, monthOfYear, dayOfMonth, onDateChangedListener);
    }

    /**
     * Set the callback that indicates the date has been adjusted by the user.
     *
     * @param onDateChangedListener How user is notified date is changed by
     *            user, can be null.
     */
    public void setOnDateChangedListener(OnDateChangedListener onDateChangedListener) {
        mDelegate.setOnDateChangedListener(onDateChangedListener);
    }

    /**
     * Update the current date.
     *
     * @param year The year.
     * @param month The month which is <strong>starting from zero</strong>.
     * @param dayOfMonth The day of the month.
     */
    public void updateDate(int year, int month, int dayOfMonth) {
        mDelegate.updateDate(year, month, dayOfMonth);
    }

    /**
     * @return The selected year.
     */
    @InspectableProperty(hasAttributeId = false)
    public int getYear() {
        return mDelegate.getYear();
    }

    /**
     * @return The selected month.
     */
    @InspectableProperty(hasAttributeId = false)
    public int getMonth() {
        return mDelegate.getMonth();
    }

    /**
     * @return The selected day of month.
     */
    @InspectableProperty(hasAttributeId = false)
    public int getDayOfMonth() {
        return mDelegate.getDayOfMonth();
    }

    /**
     * Gets the minimal date supported by this {@link DatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     * <p>
     * Note: The default minimal date is 01/01/1900.
     * <p>
     *
     * @return The minimal supported date.
     */
    @InspectableProperty
    public long getMinDate() {
        return mDelegate.getMinDate().getTimeInMillis();
    }

    /**
     * Sets the minimal date supported by this {@link NumberPicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     *
     * @param minDate The minimal supported date.
     */
    public void setMinDate(long minDate) {
        mDelegate.setMinDate(minDate);
    }

    /**
     * Gets the maximal date supported by this {@link DatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     * <p>
     * Note: The default maximal date is 12/31/2100.
     * <p>
     *
     * @return The maximal supported date.
     */
    @InspectableProperty
    public long getMaxDate() {
        return mDelegate.getMaxDate().getTimeInMillis();
    }

    /**
     * Sets the maximal date supported by this {@link DatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     *
     * @param maxDate The maximal supported date.
     */
    public void setMaxDate(long maxDate) {
        mDelegate.setMaxDate(maxDate);
    }

    /**
     * Sets the callback that indicates the current date is valid.
     *
     * @param callback the callback, may be null
     * @hide
     */
    @UnsupportedAppUsage
    public void setValidationCallback(@Nullable ValidationCallback callback) {
        mDelegate.setValidationCallback(callback);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mDelegate.isEnabled() == enabled) {
            return;
        }
        super.setEnabled(enabled);
        mDelegate.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return mDelegate.isEnabled();
    }

    /** @hide */
    @Override
    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent event) {
        return mDelegate.dispatchPopulateAccessibilityEvent(event);
    }

    /** @hide */
    @Override
    public void onPopulateAccessibilityEventInternal(AccessibilityEvent event) {
        super.onPopulateAccessibilityEventInternal(event);
        mDelegate.onPopulateAccessibilityEvent(event);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return DatePicker.class.getName();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDelegate.onConfigurationChanged(newConfig);
    }

    /**
     * Sets the first day of week.
     *
     * @param firstDayOfWeek The first day of the week conforming to the
     *            {@link CalendarView} APIs.
     * @see Calendar#SUNDAY
     * @see Calendar#MONDAY
     * @see Calendar#TUESDAY
     * @see Calendar#WEDNESDAY
     * @see Calendar#THURSDAY
     * @see Calendar#FRIDAY
     * @see Calendar#SATURDAY
     *
     * @attr ref android.R.styleable#DatePicker_firstDayOfWeek
     */
    public void setFirstDayOfWeek(int firstDayOfWeek) {
        if (firstDayOfWeek < Calendar.SUNDAY || firstDayOfWeek > Calendar.SATURDAY) {
            throw new IllegalArgumentException("firstDayOfWeek must be between 1 and 7");
        }
        mDelegate.setFirstDayOfWeek(firstDayOfWeek);
    }

    /**
     * Gets the first day of week.
     *
     * @return The first day of the week conforming to the {@link CalendarView}
     *         APIs.
     * @see Calendar#SUNDAY
     * @see Calendar#MONDAY
     * @see Calendar#TUESDAY
     * @see Calendar#WEDNESDAY
     * @see Calendar#THURSDAY
     * @see Calendar#FRIDAY
     * @see Calendar#SATURDAY
     *
     * @attr ref android.R.styleable#DatePicker_firstDayOfWeek
     */
    @InspectableProperty
    public int getFirstDayOfWeek() {
        return mDelegate.getFirstDayOfWeek();
    }

    /**
     * Returns whether the {@link CalendarView} is shown.
     * <p>
     * <strong>Note:</strong> This method returns {@code false} when the
     * {@link android.R.styleable#DatePicker_datePickerMode} attribute is set
     * to {@code calendar}.
     *
     * @return {@code true} if the calendar view is shown
     * @see #getCalendarView()
     * @deprecated Not supported by Material-style {@code calendar} mode
     */
    @InspectableProperty
    @Deprecated
    public boolean getCalendarViewShown() {
        return mDelegate.getCalendarViewShown();
    }

    /**
     * Returns the {@link CalendarView} used by this picker.
     * <p>
     * <strong>Note:</strong> This method throws an
     * {@link UnsupportedOperationException} when the
     * {@link android.R.styleable#DatePicker_datePickerMode} attribute is set
     * to {@code calendar}.
     *
     * @return the calendar view
     * @see #getCalendarViewShown()
     * @deprecated Not supported by Material-style {@code calendar} mode
     * @throws UnsupportedOperationException if called when the picker is
     *         displayed in {@code calendar} mode
     */
    @Deprecated
    public CalendarView getCalendarView() {
        return mDelegate.getCalendarView();
    }

    /**
     * Sets whether the {@link CalendarView} is shown.
     * <p>
     * <strong>Note:</strong> Calling this method has no effect when the
     * {@link android.R.styleable#DatePicker_datePickerMode} attribute is set
     * to {@code calendar}.
     *
     * @param shown {@code true} to show the calendar view, {@code false} to
     *              hide it
     * @deprecated Not supported by Material-style {@code calendar} mode
     */
    @Deprecated
    public void setCalendarViewShown(boolean shown) {
        mDelegate.setCalendarViewShown(shown);
    }

    /**
     * Returns whether the spinners are shown.
     * <p>
     * <strong>Note:</strong> this method returns {@code false} when the
     * {@link android.R.styleable#DatePicker_datePickerMode} attribute is set
     * to {@code calendar}.
     *
     * @return {@code true} if the spinners are shown
     * @deprecated Not supported by Material-style {@code calendar} mode
     */
    @InspectableProperty
    @Deprecated
    public boolean getSpinnersShown() {
        return mDelegate.getSpinnersShown();
    }

    /**
     * Sets whether the spinners are shown.
     * <p>
     * Calling this method has no effect when the
     * {@link android.R.styleable#DatePicker_datePickerMode} attribute is set
     * to {@code calendar}.
     *
     * @param shown {@code true} to show the spinners, {@code false} to hide
     *              them
     * @deprecated Not supported by Material-style {@code calendar} mode
     */
    @Deprecated
    public void setSpinnersShown(boolean shown) {
        mDelegate.setSpinnersShown(shown);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return mDelegate.onSaveInstanceState(superState);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        BaseSavedState ss = (BaseSavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mDelegate.onRestoreInstanceState(ss);
    }

    /**
     * A delegate interface that defined the public API of the DatePicker. Allows different
     * DatePicker implementations. This would need to be implemented by the DatePicker delegates
     * for the real behavior.
     *
     * @hide
     */
    interface DatePickerDelegate {
        void init(int year, int monthOfYear, int dayOfMonth,
                  OnDateChangedListener onDateChangedListener);

        void setOnDateChangedListener(OnDateChangedListener onDateChangedListener);
        void setAutoFillChangeListener(OnDateChangedListener onDateChangedListener);

        void updateDate(int year, int month, int dayOfMonth);

        int getYear();
        int getMonth();
        int getDayOfMonth();

        void autofill(AutofillValue value);
        AutofillValue getAutofillValue();

        void setFirstDayOfWeek(int firstDayOfWeek);
        int getFirstDayOfWeek();

        void setMinDate(long minDate);
        Calendar getMinDate();

        void setMaxDate(long maxDate);
        Calendar getMaxDate();

        void setEnabled(boolean enabled);
        boolean isEnabled();

        CalendarView getCalendarView();

        void setCalendarViewShown(boolean shown);
        boolean getCalendarViewShown();

        void setSpinnersShown(boolean shown);
        boolean getSpinnersShown();

        void setValidationCallback(ValidationCallback callback);

        void onConfigurationChanged(Configuration newConfig);

        Parcelable onSaveInstanceState(Parcelable superState);
        void onRestoreInstanceState(Parcelable state);

        boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event);
        void onPopulateAccessibilityEvent(AccessibilityEvent event);
    }

    /**
     * An abstract class which can be used as a start for DatePicker implementations
     */
    abstract static class AbstractDatePickerDelegate implements DatePickerDelegate {
        // The delegator
        protected DatePicker mDelegator;

        // The context
        protected Context mContext;

        // NOTE: when subclasses change this variable, they must call resetAutofilledValue().
        protected Calendar mCurrentDate;

        // The current locale
        protected Locale mCurrentLocale;

        // Callbacks
        protected OnDateChangedListener mOnDateChangedListener;
        protected OnDateChangedListener mAutoFillChangeListener;
        protected ValidationCallback mValidationCallback;

        // The value that was passed to autofill() - it must be stored because it getAutofillValue()
        // must return the exact same value that was autofilled, otherwise the widget will not be
        // properly highlighted after autofill().
        private long mAutofilledValue;

        public AbstractDatePickerDelegate(DatePicker delegator, Context context) {
            mDelegator = delegator;
            mContext = context;

            setCurrentLocale(Locale.getDefault());
        }

        protected void setCurrentLocale(Locale locale) {
            if (!locale.equals(mCurrentLocale)) {
                mCurrentLocale = locale;
                onLocaleChanged(locale);
            }
        }

        @Override
        public void setOnDateChangedListener(OnDateChangedListener callback) {
            mOnDateChangedListener = callback;
        }

        @Override
        public void setAutoFillChangeListener(OnDateChangedListener callback) {
            mAutoFillChangeListener = callback;
        }

        @Override
        public void setValidationCallback(ValidationCallback callback) {
            mValidationCallback = callback;
        }

        @Override
        public final void autofill(AutofillValue value) {
            if (value == null || !value.isDate()) {
                Log.w(LOG_TAG, value + " could not be autofilled into " + this);
                return;
            }

            final long time = value.getDateValue();

            final Calendar cal = Calendar.getInstance(mCurrentLocale);
            cal.setTimeInMillis(time);
            updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH));

            // Must set mAutofilledValue *after* calling subclass method to make sure the value
            // returned by getAutofillValue() matches it.
            mAutofilledValue = time;
        }

        @Override
        public final AutofillValue getAutofillValue() {
            final long time = mAutofilledValue != 0
                    ? mAutofilledValue
                    : mCurrentDate.getTimeInMillis();
            return AutofillValue.forDate(time);
        }

        /**
         * This method must be called every time the value of the year, month, and/or day is
         * changed by a subclass method.
         */
        protected void resetAutofilledValue() {
            mAutofilledValue = 0;
        }

        protected void onValidationChanged(boolean valid) {
            if (mValidationCallback != null) {
                mValidationCallback.onValidationChanged(valid);
            }
        }

        protected void onLocaleChanged(Locale locale) {
            // Stub.
        }

        @Override
        public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
            event.getText().add(getFormattedCurrentDate());
        }

        protected String getFormattedCurrentDate() {
           return DateUtils.formatDateTime(mContext, mCurrentDate.getTimeInMillis(),
                   DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR
                           | DateUtils.FORMAT_SHOW_WEEKDAY);
        }

        /**
         * Class for managing state storing/restoring.
         */
        static class SavedState extends View.BaseSavedState {
            private final int mSelectedYear;
            private final int mSelectedMonth;
            private final int mSelectedDay;
            private final long mMinDate;
            private final long mMaxDate;
            private final int mCurrentView;
            private final int mListPosition;
            private final int mListPositionOffset;

            public SavedState(Parcelable superState, int year, int month, int day, long minDate,
                    long maxDate) {
                this(superState, year, month, day, minDate, maxDate, 0, 0, 0);
            }

            /**
             * Constructor called from {@link DatePicker#onSaveInstanceState()}
             */
            public SavedState(Parcelable superState, int year, int month, int day, long minDate,
                    long maxDate, int currentView, int listPosition, int listPositionOffset) {
                super(superState);
                mSelectedYear = year;
                mSelectedMonth = month;
                mSelectedDay = day;
                mMinDate = minDate;
                mMaxDate = maxDate;
                mCurrentView = currentView;
                mListPosition = listPosition;
                mListPositionOffset = listPositionOffset;
            }

            /**
             * Constructor called from {@link #CREATOR}
             */
            private SavedState(Parcel in) {
                super(in);
                mSelectedYear = in.readInt();
                mSelectedMonth = in.readInt();
                mSelectedDay = in.readInt();
                mMinDate = in.readLong();
                mMaxDate = in.readLong();
                mCurrentView = in.readInt();
                mListPosition = in.readInt();
                mListPositionOffset = in.readInt();
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {
                super.writeToParcel(dest, flags);
                dest.writeInt(mSelectedYear);
                dest.writeInt(mSelectedMonth);
                dest.writeInt(mSelectedDay);
                dest.writeLong(mMinDate);
                dest.writeLong(mMaxDate);
                dest.writeInt(mCurrentView);
                dest.writeInt(mListPosition);
                dest.writeInt(mListPositionOffset);
            }

            public int getSelectedDay() {
                return mSelectedDay;
            }

            public int getSelectedMonth() {
                return mSelectedMonth;
            }

            public int getSelectedYear() {
                return mSelectedYear;
            }

            public long getMinDate() {
                return mMinDate;
            }

            public long getMaxDate() {
                return mMaxDate;
            }

            public int getCurrentView() {
                return mCurrentView;
            }

            public int getListPosition() {
                return mListPosition;
            }

            public int getListPositionOffset() {
                return mListPositionOffset;
            }

            @SuppressWarnings("all")
            // suppress unused and hiding
            public static final @android.annotation.NonNull Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {

                public SavedState createFromParcel(Parcel in) {
                    return new SavedState(in);
                }

                public SavedState[] newArray(int size) {
                    return new SavedState[size];
                }
            };
        }
    }

    /**
     * A callback interface for updating input validity when the date picker
     * when included into a dialog.
     *
     * @hide
     */
    public interface ValidationCallback {
        void onValidationChanged(boolean valid);
    }

    @Override
    public void dispatchProvideAutofillStructure(ViewStructure structure, int flags) {
        // This view is self-sufficient for autofill, so it needs to call
        // onProvideAutoFillStructure() to fill itself, but it does not need to call
        // dispatchProvideAutoFillStructure() to fill its children.
        structure.setAutofillId(getAutofillId());
        onProvideAutofillStructure(structure, flags);
    }

    @Override
    public void autofill(AutofillValue value) {
        if (!isEnabled()) return;

        mDelegate.autofill(value);
    }

    @Override
    public @AutofillType int getAutofillType() {
        return isEnabled() ? AUTOFILL_TYPE_DATE : AUTOFILL_TYPE_NONE;
    }

    @Override
    public AutofillValue getAutofillValue() {
        return isEnabled() ? mDelegate.getAutofillValue() : null;
    }
}
