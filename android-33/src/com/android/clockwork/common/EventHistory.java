package com.android.clockwork.common;

import com.android.internal.util.IndentingPrintWriter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for recording and dumping events.
 */
public class EventHistory<T extends EventHistory.Event> {

    public static abstract class Event {
        public final long timestamp;
        public Event() {
            timestamp = System.currentTimeMillis();
        }

        public abstract String getName();

        public long getTimestampMs() {
            return timestamp;
        }

        public boolean isDuplicateOf(Event event) {
            return this.getName().equals(event.getName());
        }
    }

    private final String mName;
    private final int mSize;
    private final boolean mRecordDuplicateEvents;

    // mEventHistory is a queue - elements are added to the front of the list (addFirst) and
    // removed from the back of the list (getLast)
    // this is done so that events are stored in reverse chronological order
    private final LinkedList<T> mEventHistory;

    public EventHistory(String name, int maxSize, boolean recordDuplicateEvents) {
        mName = name;
        mEventHistory = new LinkedList<>();
        mSize = maxSize;
        mRecordDuplicateEvents = recordDuplicateEvents;
    }

    /**
     * Returns true if the event was added, or false if the event was duplicate and ignored
     * (if recordDuplicateEvents is set to false).
     */
    public boolean recordEvent(T event) {
        if (!mRecordDuplicateEvents && !mEventHistory.isEmpty()) {
            if (event.isDuplicateOf(mEventHistory.getFirst())) {
                return false;
            }
        }
        mEventHistory.addFirst(event);
        while (mEventHistory.size() > mSize) {
            mEventHistory.removeLast();
        }
        return true;
    }

    public T getMostRecentEvent() {
        if (mEventHistory.isEmpty()) {
            return null;
        }
        return mEventHistory.getFirst();
    }

    public List<T> getAllEvents() {
        return new ArrayList(mEventHistory);
    }

    private String getDateFromTimestamp(long timestamp) {
        if (timestamp <= 0) {
            return Long.toString(timestamp);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US);
        return sdf.format(new Date(timestamp));
    }

    public void dump(IndentingPrintWriter ipw) {
        ipw.println(mName + (mRecordDuplicateEvents ? "" : " (duplicate events filtered)"));
        ipw.increaseIndent();
        for (Event event : mEventHistory) {
            ipw.println(getDateFromTimestamp(event.getTimestampMs()) + " - " + event.getName());
        }
        ipw.decreaseIndent();
    }
}
