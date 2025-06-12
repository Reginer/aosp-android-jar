/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.compat.annotation.UnsupportedAppUsage;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.Pair;

import com.android.internal.util.ArrayUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

/** @hide */
public class SyncStatusInfo implements Parcelable {
    private static final String TAG = "Sync";

    static final int VERSION = 6;

    private static final int MAX_EVENT_COUNT = 10;

    /**
     * Number of sync sources. KEEP THIS AND SyncStorageEngine.SOURCES IN SYNC.
     */
    private static final int SOURCE_COUNT = 6;

    @UnsupportedAppUsage
    public final int authorityId;

    /**
     * # of syncs for each sync source, etc.
     */
    public static class Stats {
        public long totalElapsedTime;
        public int numSyncs;
        public int numSourcePoll;
        public int numSourceOther;
        public int numSourceLocal;
        public int numSourceUser;
        public int numSourcePeriodic;
        public int numSourceFeed;
        public int numFailures;
        public int numCancels;

        /** Copy all the stats to another instance. */
        public void copyTo(Stats to) {
            to.totalElapsedTime = totalElapsedTime;
            to.numSyncs = numSyncs;
            to.numSourcePoll = numSourcePoll;
            to.numSourceOther = numSourceOther;
            to.numSourceLocal = numSourceLocal;
            to.numSourceUser = numSourceUser;
            to.numSourcePeriodic = numSourcePeriodic;
            to.numSourceFeed = numSourceFeed;
            to.numFailures = numFailures;
            to.numCancels = numCancels;
        }

        /** Clear all the stats. */
        public void clear() {
            totalElapsedTime = 0;
            numSyncs = 0;
            numSourcePoll = 0;
            numSourceOther = 0;
            numSourceLocal = 0;
            numSourceUser = 0;
            numSourcePeriodic = 0;
            numSourceFeed = 0;
            numFailures = 0;
            numCancels = 0;
        }

        /** Write all the stats to a parcel. */
        public void writeToParcel(Parcel parcel) {
            parcel.writeLong(totalElapsedTime);
            parcel.writeInt(numSyncs);
            parcel.writeInt(numSourcePoll);
            parcel.writeInt(numSourceOther);
            parcel.writeInt(numSourceLocal);
            parcel.writeInt(numSourceUser);
            parcel.writeInt(numSourcePeriodic);
            parcel.writeInt(numSourceFeed);
            parcel.writeInt(numFailures);
            parcel.writeInt(numCancels);
        }

        /** Read all the stats from a parcel. */
        public void readFromParcel(Parcel parcel) {
            totalElapsedTime = parcel.readLong();
            numSyncs = parcel.readInt();
            numSourcePoll = parcel.readInt();
            numSourceOther = parcel.readInt();
            numSourceLocal = parcel.readInt();
            numSourceUser = parcel.readInt();
            numSourcePeriodic = parcel.readInt();
            numSourceFeed = parcel.readInt();
            numFailures = parcel.readInt();
            numCancels = parcel.readInt();
        }
    }

    public long lastTodayResetTime;

    public final Stats totalStats = new Stats();
    public final Stats todayStats = new Stats();
    public final Stats yesterdayStats = new Stats();

    @UnsupportedAppUsage
    public long lastSuccessTime;
    @UnsupportedAppUsage
    public int lastSuccessSource;
    @UnsupportedAppUsage
    public long lastFailureTime;
    @UnsupportedAppUsage
    public int lastFailureSource;
    @UnsupportedAppUsage
    public String lastFailureMesg;
    @UnsupportedAppUsage
    public long initialFailureTime;
    @UnsupportedAppUsage
    public boolean pending;
    @UnsupportedAppUsage
    public boolean initialize;

    public final long[] perSourceLastSuccessTimes = new long[SOURCE_COUNT];
    public final long[] perSourceLastFailureTimes = new long[SOURCE_COUNT];

    // Warning: It is up to the external caller to ensure there are
    // no race conditions when accessing this list
    @UnsupportedAppUsage
    private ArrayList<Long> periodicSyncTimes;

    private final ArrayList<Long> mLastEventTimes = new ArrayList<>();
    private final ArrayList<String> mLastEvents = new ArrayList<>();

    @UnsupportedAppUsage
    public SyncStatusInfo(int authorityId) {
        this.authorityId = authorityId;
    }

    @UnsupportedAppUsage
    public int getLastFailureMesgAsInt(int def) {
        final int i = ContentResolver.syncErrorStringToInt(lastFailureMesg);
        if (i > 0) {
            return i;
        } else {
            Log.d(TAG, "Unknown lastFailureMesg:" + lastFailureMesg);
            return def;
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(VERSION);
        parcel.writeInt(authorityId);

        // Note we can't use Stats.writeToParcel() here; see the below constructor for the reason.
        parcel.writeLong(totalStats.totalElapsedTime);
        parcel.writeInt(totalStats.numSyncs);
        parcel.writeInt(totalStats.numSourcePoll);
        parcel.writeInt(totalStats.numSourceOther);
        parcel.writeInt(totalStats.numSourceLocal);
        parcel.writeInt(totalStats.numSourceUser);

        parcel.writeLong(lastSuccessTime);
        parcel.writeInt(lastSuccessSource);
        parcel.writeLong(lastFailureTime);
        parcel.writeInt(lastFailureSource);
        parcel.writeString(lastFailureMesg);
        parcel.writeLong(initialFailureTime);
        parcel.writeInt(pending ? 1 : 0);
        parcel.writeInt(initialize ? 1 : 0);
        if (periodicSyncTimes != null) {
            parcel.writeInt(periodicSyncTimes.size());
            for (long periodicSyncTime : periodicSyncTimes) {
                parcel.writeLong(periodicSyncTime);
            }
        } else {
            parcel.writeInt(-1);
        }
        parcel.writeInt(mLastEventTimes.size());
        for (int i = 0; i < mLastEventTimes.size(); i++) {
            parcel.writeLong(mLastEventTimes.get(i));
            parcel.writeString(mLastEvents.get(i));
        }
        // Version 4
        parcel.writeInt(totalStats.numSourcePeriodic);

        // Version 5
        parcel.writeInt(totalStats.numSourceFeed);
        parcel.writeInt(totalStats.numFailures);
        parcel.writeInt(totalStats.numCancels);

        parcel.writeLong(lastTodayResetTime);

        todayStats.writeToParcel(parcel);
        yesterdayStats.writeToParcel(parcel);

        // Version 6.
        parcel.writeLongArray(perSourceLastSuccessTimes);
        parcel.writeLongArray(perSourceLastFailureTimes);
    }

    @UnsupportedAppUsage
    public SyncStatusInfo(Parcel parcel) {
        int version = parcel.readInt();
        if (version != VERSION && version != 1) {
            Log.w("SyncStatusInfo", "Unknown version: " + version);
        }
        authorityId = parcel.readInt();

        // Note we can't use Stats.writeToParcel() here because the data is persisted and we need
        // to be able to read from the old format too.
        totalStats.totalElapsedTime = parcel.readLong();
        totalStats.numSyncs = parcel.readInt();
        totalStats.numSourcePoll = parcel.readInt();
        totalStats.numSourceOther = parcel.readInt();
        totalStats.numSourceLocal = parcel.readInt();
        totalStats.numSourceUser = parcel.readInt();
        lastSuccessTime = parcel.readLong();
        lastSuccessSource = parcel.readInt();
        lastFailureTime = parcel.readLong();
        lastFailureSource = parcel.readInt();
        lastFailureMesg = parcel.readString();
        initialFailureTime = parcel.readLong();
        pending = parcel.readInt() != 0;
        initialize = parcel.readInt() != 0;
        if (version == 1) {
            periodicSyncTimes = null;
        } else {
            final int count = parcel.readInt();
            if (count < 0) {
                periodicSyncTimes = null;
            } else {
                periodicSyncTimes = new ArrayList<Long>();
                for (int i = 0; i < count; i++) {
                    periodicSyncTimes.add(parcel.readLong());
                }
            }
            if (version >= 3) {
                mLastEventTimes.clear();
                mLastEvents.clear();
                final int nEvents = parcel.readInt();
                for (int i = 0; i < nEvents; i++) {
                    mLastEventTimes.add(parcel.readLong());
                    mLastEvents.add(parcel.readString());
                }
            }
        }
        if (version < 4) {
            // Before version 4, numSourcePeriodic wasn't persisted.
            totalStats.numSourcePeriodic =
                    totalStats.numSyncs - totalStats.numSourceLocal - totalStats.numSourcePoll
                            - totalStats.numSourceOther
                            - totalStats.numSourceUser;
            if (totalStats.numSourcePeriodic < 0) { // Consistency check.
                totalStats.numSourcePeriodic = 0;
            }
        } else {
            totalStats.numSourcePeriodic = parcel.readInt();
        }
        if (version >= 5) {
            totalStats.numSourceFeed = parcel.readInt();
            totalStats.numFailures = parcel.readInt();
            totalStats.numCancels = parcel.readInt();

            lastTodayResetTime = parcel.readLong();

            todayStats.readFromParcel(parcel);
            yesterdayStats.readFromParcel(parcel);
        }
        if (version >= 6) {
            parcel.readLongArray(perSourceLastSuccessTimes);
            parcel.readLongArray(perSourceLastFailureTimes);
        }
    }

    /**
     * Copies all data from the given SyncStatusInfo object.
     *
     * @param other the SyncStatusInfo object to copy data from
     */
    public SyncStatusInfo(SyncStatusInfo other) {
        authorityId = other.authorityId;
        copyFrom(other);
    }

    /**
     * Copies all data from the given SyncStatusInfo object except for its authority id.
     *
     * @param authorityId the new authority id
     * @param other the SyncStatusInfo object to copy data from
     */
    public SyncStatusInfo(int authorityId, SyncStatusInfo other) {
        this.authorityId = authorityId;
        copyFrom(other);
    }

    private void copyFrom(SyncStatusInfo other) {
        other.totalStats.copyTo(totalStats);
        other.todayStats.copyTo(todayStats);
        other.yesterdayStats.copyTo(yesterdayStats);

        lastTodayResetTime = other.lastTodayResetTime;

        lastSuccessTime = other.lastSuccessTime;
        lastSuccessSource = other.lastSuccessSource;
        lastFailureTime = other.lastFailureTime;
        lastFailureSource = other.lastFailureSource;
        lastFailureMesg = other.lastFailureMesg;
        initialFailureTime = other.initialFailureTime;
        pending = other.pending;
        initialize = other.initialize;
        if (other.periodicSyncTimes != null) {
            periodicSyncTimes = new ArrayList<Long>(other.periodicSyncTimes);
        }
        mLastEventTimes.addAll(other.mLastEventTimes);
        mLastEvents.addAll(other.mLastEvents);

        copy(perSourceLastSuccessTimes, other.perSourceLastSuccessTimes);
        copy(perSourceLastFailureTimes, other.perSourceLastFailureTimes);
    }

    private static void copy(long[] to, long[] from) {
        System.arraycopy(from, 0, to, 0, to.length);
    }

    public int getPeriodicSyncTimesSize() {
        return periodicSyncTimes == null ? 0 : periodicSyncTimes.size();
    }

    public void addPeriodicSyncTime(long time) {
        periodicSyncTimes = ArrayUtils.add(periodicSyncTimes, time);
    }

    @UnsupportedAppUsage
    public void setPeriodicSyncTime(int index, long when) {
        // The list is initialized lazily when scheduling occurs so we need to make sure
        // we initialize elements < index to zero (zero is ignore for scheduling purposes)
        ensurePeriodicSyncTimeSize(index);
        periodicSyncTimes.set(index, when);
    }

    @UnsupportedAppUsage
    public long getPeriodicSyncTime(int index) {
        if (periodicSyncTimes != null && index < periodicSyncTimes.size()) {
            return periodicSyncTimes.get(index);
        } else {
            return 0;
        }
    }

    @UnsupportedAppUsage
    public void removePeriodicSyncTime(int index) {
        if (periodicSyncTimes != null && index < periodicSyncTimes.size()) {
            periodicSyncTimes.remove(index);
        }
    }

    /**
     * Populates {@code mLastEventTimes} and {@code mLastEvents} with the given list. <br>
     * <i>Note: This method is mainly used to repopulate the event info from disk and it will clear
     * both {@code mLastEventTimes} and {@code mLastEvents} before populating.</i>
     *
     * @param lastEventInformation the list to populate with
     */
    public void populateLastEventsInformation(ArrayList<Pair<Long, String>> lastEventInformation) {
        mLastEventTimes.clear();
        mLastEvents.clear();
        final int size = lastEventInformation.size();
        for (int i = 0; i < size; i++) {
            final Pair<Long, String> lastEventInfo = lastEventInformation.get(i);
            mLastEventTimes.add(lastEventInfo.first);
            mLastEvents.add(lastEventInfo.second);
        }
    }

    /** */
    public void addEvent(String message) {
        if (mLastEventTimes.size() >= MAX_EVENT_COUNT) {
            mLastEventTimes.remove(MAX_EVENT_COUNT - 1);
            mLastEvents.remove(MAX_EVENT_COUNT - 1);
        }
        mLastEventTimes.add(0, System.currentTimeMillis());
        mLastEvents.add(0, message);
    }

    /** */
    public int getEventCount() {
        return mLastEventTimes.size();
    }

    /** */
    public long getEventTime(int i) {
        return mLastEventTimes.get(i);
    }

    /** */
    public String getEvent(int i) {
        return mLastEvents.get(i);
    }

    /** Call this when a sync has succeeded. */
    public void setLastSuccess(int source, long lastSyncTime) {
        lastSuccessTime = lastSyncTime;
        lastSuccessSource = source;
        lastFailureTime = 0;
        lastFailureSource = -1;
        lastFailureMesg = null;
        initialFailureTime = 0;

        if (0 <= source && source < perSourceLastSuccessTimes.length) {
            perSourceLastSuccessTimes[source] = lastSyncTime;
        }
    }

    /** Call this when a sync has failed. */
    public void setLastFailure(int source, long lastSyncTime, String failureMessage) {
        lastFailureTime = lastSyncTime;
        lastFailureSource = source;
        lastFailureMesg = failureMessage;
        if (initialFailureTime == 0) {
            initialFailureTime = lastSyncTime;
        }

        if (0 <= source && source < perSourceLastFailureTimes.length) {
            perSourceLastFailureTimes[source] = lastSyncTime;
        }
    }

    @UnsupportedAppUsage
    public static final @android.annotation.NonNull Creator<SyncStatusInfo> CREATOR = new Creator<SyncStatusInfo>() {
        public SyncStatusInfo createFromParcel(Parcel in) {
            return new SyncStatusInfo(in);
        }

        public SyncStatusInfo[] newArray(int size) {
            return new SyncStatusInfo[size];
        }
    };

    @UnsupportedAppUsage
    private void ensurePeriodicSyncTimeSize(int index) {
        if (periodicSyncTimes == null) {
            periodicSyncTimes = new ArrayList<Long>(0);
        }

        final int requiredSize = index + 1;
        if (periodicSyncTimes.size() < requiredSize) {
            for (int i = periodicSyncTimes.size(); i < requiredSize; i++) {
                periodicSyncTimes.add((long) 0);
            }
        }
    }

    /**
     * If the last reset was not today, move today's stats to yesterday's and clear today's.
     */
    public void maybeResetTodayStats(boolean clockValid, boolean force) {
        final long now = System.currentTimeMillis();

        if (!force) {
            // Last reset was the same day, nothing to do.
            if (areSameDates(now, lastTodayResetTime)) {
                return;
            }

            // Hack -- on devices with no RTC, until the NTP kicks in, the device won't have the
            // correct time. So if the time goes back, don't reset, unless we're sure the current
            // time is correct.
            if (now < lastTodayResetTime && !clockValid) {
                return;
            }
        }

        lastTodayResetTime = now;

        todayStats.copyTo(yesterdayStats);
        todayStats.clear();
    }

    private static boolean areSameDates(long time1, long time2) {
        final Calendar c1 = new GregorianCalendar();
        final Calendar c2 = new GregorianCalendar();

        c1.setTimeInMillis(time1);
        c2.setTimeInMillis(time2);

        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }
}
