/*
 * Copyright (c) 2017 The Android Open Source Project
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

package android.service.notification;

import android.service.notification.ZenModeConfig.ScheduleInfo;
import android.util.ArraySet;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import java.util.Calendar;
import java.util.Objects;
import java.util.TimeZone;

/**
 * @hide
 */
public class ScheduleCalendar {
    public static final String TAG = "ScheduleCalendar";
    public static final boolean DEBUG = Log.isLoggable("ConditionProviders", Log.DEBUG);
    private final ArraySet<Integer> mDays = new ArraySet<Integer>();
    private final Calendar mCalendar = Calendar.getInstance();

    private ScheduleInfo mSchedule;

    @Override
    public String toString() {
        return "ScheduleCalendar[mDays=" + mDays + ", mSchedule=" + mSchedule + "]";
    }

    /**
     * @return true if schedule will exit on alarm, else false
     */
    public boolean exitAtAlarm() {
        return mSchedule.exitAtAlarm;
    }

    /**
     * Sets schedule information
     */
    public void setSchedule(ScheduleInfo schedule) {
        if (Objects.equals(mSchedule, schedule)) return;
        mSchedule = schedule;
        updateDays();
    }

    /**
     * Sets next alarm of the schedule
     * @param now current time in milliseconds
     * @param nextAlarm time of next alarm in milliseconds
     */
    public void maybeSetNextAlarm(long now, long nextAlarm) {
        if (mSchedule != null && mSchedule.exitAtAlarm) {
            if (nextAlarm == 0) {
                // alarm canceled
                mSchedule.nextAlarm = 0;
            } else if (nextAlarm > now) {
                // only allow alarms in the future
                mSchedule.nextAlarm = nextAlarm;
            } else if (mSchedule.nextAlarm < now) {
                if (DEBUG) {
                    Log.d(TAG, "All alarms are in the past " + mSchedule.nextAlarm);
                }
                mSchedule.nextAlarm = 0;
            }
        }
    }

    /**
     * Set calendar time zone to tz
     * @param tz current time zone
     */
    public void setTimeZone(TimeZone tz) {
        mCalendar.setTimeZone(tz);
    }

    /**
     * @param now current time in milliseconds
     * @return next time this rule changes (starts or ends)
     */
    public long getNextChangeTime(long now) {
        if (mSchedule == null) return 0;
        final long nextStart = getNextTime(now, mSchedule.startHour, mSchedule.startMinute, true);
        final long nextEnd = getNextTime(now, mSchedule.endHour, mSchedule.endMinute, false);
        long nextScheduleTime = Math.min(nextStart, nextEnd);

        return nextScheduleTime;
    }

    private long getNextTime(long now, int hr, int min, boolean adjust) {
        // The adjust parameter indicates whether to potentially adjust the time to the closest
        // actual time if the indicated time is one skipped due to daylight time.
        final long time = adjust ? getClosestActualTime(now, hr, min) : getTime(now, hr, min);
        if (time <= now) {
            final long tomorrow = addDays(time, 1);
            return adjust ? getClosestActualTime(tomorrow, hr, min) : getTime(tomorrow, hr, min);
        }
        return time;
    }

    private long getTime(long millis, int hour, int min) {
        mCalendar.setTimeInMillis(millis);
        mCalendar.set(Calendar.HOUR_OF_DAY, hour);
        mCalendar.set(Calendar.MINUTE, min);
        mCalendar.set(Calendar.SECOND, 0);
        mCalendar.set(Calendar.MILLISECOND, 0);
        return mCalendar.getTimeInMillis();
    }

    /**
     * @param time milliseconds since Epoch
     * @return true if time is within the schedule, else false
     */
    public boolean isInSchedule(long time) {
        if (mSchedule == null || mDays.size() == 0) return false;
        final long start = getClosestActualTime(time, mSchedule.startHour, mSchedule.startMinute);
        long end = getTime(time, mSchedule.endHour, mSchedule.endMinute);
        if (end <= start) {
            end = addDays(end, 1);
        }
        return isInSchedule(-1, time, start, end) || isInSchedule(0, time, start, end);
    }

    /**
     * @param alarm milliseconds since Epoch
     * @param now milliseconds since Epoch
     * @return true if alarm and now is within the schedule, else false
     */
    public boolean isAlarmInSchedule(long alarm, long now) {
        if (mSchedule == null || mDays.size() == 0) return false;
        final long start = getClosestActualTime(alarm, mSchedule.startHour, mSchedule.startMinute);
        long end = getTime(alarm, mSchedule.endHour, mSchedule.endMinute);
        if (end <= start) {
            end = addDays(end, 1);
        }
        return (isInSchedule(-1, alarm, start, end)
                && isInSchedule(-1, now, start, end))
                || (isInSchedule(0, alarm, start, end)
                && isInSchedule(0, now, start, end));
    }

    /**
     * @param time milliseconds since Epoch
     * @return true if should exit at time for next alarm, else false
     */
    public boolean shouldExitForAlarm(long time) {
        if (mSchedule == null) {
            return false;
        }
        return mSchedule.exitAtAlarm
                && mSchedule.nextAlarm != 0
                && time >= mSchedule.nextAlarm
                && isAlarmInSchedule(mSchedule.nextAlarm, time);
    }

    private boolean isInSchedule(int daysOffset, long time, long start, long end) {
        final int n = Calendar.SATURDAY;
        final int day = ((getDayOfWeek(time) - 1) + (daysOffset % n) + n) % n + 1;
        start = addDays(start, daysOffset);
        end = addDays(end, daysOffset);
        return mDays.contains(day) && time >= start && time < end;
    }

    private int getDayOfWeek(long time) {
        mCalendar.setTimeInMillis(time);
        return mCalendar.get(Calendar.DAY_OF_WEEK);
    }

    private void updateDays() {
        mDays.clear();
        if (mSchedule != null && mSchedule.days != null) {
            for (int i = 0; i < mSchedule.days.length; i++) {
                mDays.add(mSchedule.days[i]);
            }
        }
    }

    private long addDays(long time, int days) {
        mCalendar.setTimeInMillis(time);
        mCalendar.add(Calendar.DATE, days);
        return mCalendar.getTimeInMillis();
    }

    /**
     * This function returns the closest "actual" time to the provided hour/minute relative to the
     * reference time. For most times this will behave exactly the same as getTime, but for any time
     * during the hour skipped forward for daylight savings time (for instance, 02:xx when the
     * clock is set to 03:00 after 01:59), this method will return the time when the clock changes
     * (in this example, 03:00).
     *
     * Assumptions made in this implementation:
     *   - Time is moved forward on an hour boundary (minute 0) by exactly 1hr when clocks shift
     *   - a lenient Calendar implementation will interpret 02:xx on a day when 2-3AM is skipped
     *     as 03:xx
     *   - The skipped hour is never 11PM / 23:00.
     *
     * @hide
     */
    @VisibleForTesting
    public long getClosestActualTime(long refTime, int hour, int min) {
        long resTime = getTime(refTime, hour, min);
        if (!mCalendar.getTimeZone().observesDaylightTime()) {
            // Do nothing if the timezone doesn't observe daylight time at all.
            return resTime;
        }

        // Approach to identifying whether the time is "skipped": get the result from starting with
        // refTime and setting hour and minute, then re-extract the hour and minute of the resulting
        // moment in time. If the hour is exactly one more than the passed-in hour and the minute is
        // the same, then the provided hour is likely a skipped one. If the time doesn't fall into
        // this category, return the unmodified time instead.
        mCalendar.setTimeInMillis(resTime);
        int resHr = mCalendar.get(Calendar.HOUR_OF_DAY);
        int resMin = mCalendar.get(Calendar.MINUTE);
        if (resHr == hour + 1 && resMin == min) {
            return getTime(refTime, resHr, 0);
        }
        return resTime;
    }
}
