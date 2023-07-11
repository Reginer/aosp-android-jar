/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.internal.telephony.metrics;

import static com.android.internal.telephony.metrics.TelephonyMetrics.toCallQualityProto;

import android.telephony.CallQuality;
import android.telephony.CellInfo;
import android.telephony.CellSignalStrengthLte;
import android.telephony.SignalStrength;
import android.util.Pair;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SignalStrengthController;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.telephony.Rlog;

import java.util.ArrayList;
import java.util.Collections;

/**
 * CallQualityMetrics is a utility for tracking the CallQuality during an ongoing call session. It
 * processes snapshots throughout the call to keep track of info like the best and worst
 * ServiceStates, durations of good and bad quality, and other summary statistics.
 */
public class CallQualityMetrics {

    private static final String TAG = CallQualityMetrics.class.getSimpleName();

    // certain metrics are only logged on userdebug
    private static final boolean IS_DEBUGGABLE = TelephonyUtils.IS_DEBUGGABLE;

    // We only log the first MAX_SNAPSHOTS changes to CallQuality
    private static final int MAX_SNAPSHOTS = 5;

    // value of mCallQualityState which means the CallQuality is EXCELLENT/GOOD/FAIR
    private static final int GOOD_QUALITY = 0;

    // value of mCallQualityState which means the CallQuality is BAD/POOR
    private static final int BAD_QUALITY = 1;

    private Phone mPhone;

    /** Snapshots of the call quality and SignalStrength (LTE-SNR for IMS calls) */
    // mUlSnapshots holds snapshots from uplink call quality changes. We log take snapshots of the
    // first MAX_SNAPSHOTS transitions between good and bad quality
    private ArrayList<Pair<CallQuality, Integer>> mUlSnapshots = new ArrayList<>();
    // mDlSnapshots holds snapshots from downlink call quality changes. We log take snapshots of
    // the first MAX_SNAPSHOTS transitions between good and bad quality
    private ArrayList<Pair<CallQuality, Integer>> mDlSnapshots = new ArrayList<>();

    // holds lightweight history of call quality and durations, used for calculating total time
    // spent with bad and good quality for metrics and bugreports. This is separate from the
    // snapshots because those are capped at MAX_SNAPSHOTS to avoid excessive memory use.
    private ArrayList<TimestampedQualitySnapshot> mFullUplinkQuality = new ArrayList<>();
    private ArrayList<TimestampedQualitySnapshot> mFullDownlinkQuality = new ArrayList<>();

    // Current downlink call quality
    private int mDlCallQualityState = GOOD_QUALITY;

    // Current uplink call quality
    private int mUlCallQualityState = GOOD_QUALITY;

    // The last logged CallQuality
    private CallQuality mLastCallQuality;

    /** Snapshots taken at best and worst SignalStrengths */
    private Pair<CallQuality, Integer> mWorstSsWithGoodDlQuality;
    private Pair<CallQuality, Integer> mBestSsWithGoodDlQuality;
    private Pair<CallQuality, Integer> mWorstSsWithBadDlQuality;
    private Pair<CallQuality, Integer> mBestSsWithBadDlQuality;
    private Pair<CallQuality, Integer> mWorstSsWithGoodUlQuality;
    private Pair<CallQuality, Integer> mBestSsWithGoodUlQuality;
    private Pair<CallQuality, Integer> mWorstSsWithBadUlQuality;
    private Pair<CallQuality, Integer> mBestSsWithBadUlQuality;

    /**
     * Construct a CallQualityMetrics object to be used to keep track of call quality for a single
     * call session.
     */
    public CallQualityMetrics(Phone phone) {
        mPhone = phone;
        mLastCallQuality = new CallQuality();
    }

    /**
     * Called when call quality changes.
     */
    public void saveCallQuality(CallQuality cq) {
        if (cq.getUplinkCallQualityLevel() == CallQuality.CALL_QUALITY_NOT_AVAILABLE
                || cq.getDownlinkCallQualityLevel() == CallQuality.CALL_QUALITY_NOT_AVAILABLE) {
            return;
        }

        // uplink and downlink call quality are tracked separately
        int newUlCallQualityState = BAD_QUALITY;
        int newDlCallQualityState = BAD_QUALITY;
        if (isGoodQuality(cq.getUplinkCallQualityLevel())) {
            newUlCallQualityState = GOOD_QUALITY;
        }
        if (isGoodQuality(cq.getDownlinkCallQualityLevel())) {
            newDlCallQualityState = GOOD_QUALITY;
        }

        if (IS_DEBUGGABLE) {
            if (newUlCallQualityState != mUlCallQualityState) {
                addSnapshot(cq, mUlSnapshots);
            }
            if (newDlCallQualityState != mDlCallQualityState) {
                addSnapshot(cq, mDlSnapshots);
            }
        }

        updateTotalDurations(cq);

        updateMinAndMaxSignalStrengthSnapshots(newDlCallQualityState, newUlCallQualityState, cq);

        mUlCallQualityState = newUlCallQualityState;
        mDlCallQualityState = newDlCallQualityState;
        // call duration updates sometimes come out of order
        if (cq.getCallDuration() > mLastCallQuality.getCallDuration()) {
            mLastCallQuality = cq;
        }
    }

    private void updateTotalDurations(CallQuality cq) {
        mFullDownlinkQuality.add(new TimestampedQualitySnapshot(cq.getCallDuration(),
                cq.getDownlinkCallQualityLevel()));
        mFullUplinkQuality.add(new TimestampedQualitySnapshot(cq.getCallDuration(),
                cq.getUplinkCallQualityLevel()));
    }

    private static boolean isGoodQuality(int callQualityLevel) {
        return callQualityLevel < CallQuality.CALL_QUALITY_BAD;
    }

    /**
     * Save a snapshot of the call quality and signal strength. This can be called with uplink or
     * downlink call quality level.
     */
    private void addSnapshot(CallQuality cq, ArrayList<Pair<CallQuality, Integer>> snapshots) {
        if (snapshots.size() < MAX_SNAPSHOTS) {
            Integer ss = getLteSnr();
            snapshots.add(Pair.create(cq, ss));
        }
    }

    /**
     * Updates the snapshots saved when signal strength is highest and lowest while the call quality
     * is good and bad for both uplink and downlink call quality.
     * <p>
     * At the end of the call we should have:
     *  - for both UL and DL:
     *     - snapshot of the best signal strength with bad call quality
     *     - snapshot of the worst signal strength with bad call quality
     *     - snapshot of the best signal strength with good call quality
     *     - snapshot of the worst signal strength with good call quality
     */
    private void updateMinAndMaxSignalStrengthSnapshots(int newDlCallQualityState,
            int newUlCallQualityState, CallQuality cq) {
        Integer ss = getLteSnr();
        if (ss.equals(CellInfo.UNAVAILABLE)) {
            return;
        }

        // downlink
        if (newDlCallQualityState == GOOD_QUALITY) {
            if (mWorstSsWithGoodDlQuality == null || ss < mWorstSsWithGoodDlQuality.second) {
                mWorstSsWithGoodDlQuality = Pair.create(cq, ss);
            }
            if (mBestSsWithGoodDlQuality == null || ss > mBestSsWithGoodDlQuality.second) {
                mBestSsWithGoodDlQuality = Pair.create(cq, ss);
            }
        } else {
            if (mWorstSsWithBadDlQuality == null || ss < mWorstSsWithBadDlQuality.second) {
                mWorstSsWithBadDlQuality = Pair.create(cq, ss);
            }
            if (mBestSsWithBadDlQuality == null || ss > mBestSsWithBadDlQuality.second) {
                mBestSsWithBadDlQuality = Pair.create(cq, ss);
            }
        }

        // uplink
        if (newUlCallQualityState == GOOD_QUALITY) {
            if (mWorstSsWithGoodUlQuality == null || ss < mWorstSsWithGoodUlQuality.second) {
                mWorstSsWithGoodUlQuality = Pair.create(cq, ss);
            }
            if (mBestSsWithGoodUlQuality == null || ss > mBestSsWithGoodUlQuality.second) {
                mBestSsWithGoodUlQuality = Pair.create(cq, ss);
            }
        } else {
            if (mWorstSsWithBadUlQuality == null || ss < mWorstSsWithBadUlQuality.second) {
                mWorstSsWithBadUlQuality = Pair.create(cq, ss);
            }
            if (mBestSsWithBadUlQuality == null || ss > mBestSsWithBadUlQuality.second) {
                mBestSsWithBadUlQuality = Pair.create(cq, ss);
            }
        }
    }

    // Returns the LTE signal to noise ratio, or 0 if unavailable
    private Integer getLteSnr() {
        SignalStrengthController ssc = mPhone.getDefaultPhone().getSignalStrengthController();
        if (ssc == null) {
            Rlog.e(TAG, "getLteSnr: unable to get SSC for phone " + mPhone.getPhoneId());
            return CellInfo.UNAVAILABLE;
        }

        SignalStrength ss = ssc.getSignalStrength();
        if (ss == null) {
            Rlog.e(TAG, "getLteSnr: unable to get SignalStrength for phone " + mPhone.getPhoneId());
            return CellInfo.UNAVAILABLE;
        }

        // There may be multiple CellSignalStrengthLte, so try to use one with available SNR
        for (CellSignalStrengthLte lteSs : ss.getCellSignalStrengths(CellSignalStrengthLte.class)) {
            int snr = lteSs.getRssnr();
            if (snr != CellInfo.UNAVAILABLE) {
                return snr;
            }
        }

        return CellInfo.UNAVAILABLE;
    }

    private static TelephonyCallSession.Event.SignalStrength toProto(int ss) {
        TelephonyCallSession.Event.SignalStrength ret =
                new TelephonyCallSession.Event.SignalStrength();
        ret.lteSnr = ss;
        return ret;
    }

    /**
     * Return the full downlink CallQualitySummary using the saved CallQuality records.
     */
    public TelephonyCallSession.Event.CallQualitySummary getCallQualitySummaryDl() {
        TelephonyCallSession.Event.CallQualitySummary summary =
                new TelephonyCallSession.Event.CallQualitySummary();
        Pair<Integer, Integer> totalGoodAndBadDurations = getTotalGoodAndBadQualityTimeMs(
                mFullDownlinkQuality);
        summary.totalGoodQualityDurationInSeconds = totalGoodAndBadDurations.first / 1000;
        summary.totalBadQualityDurationInSeconds = totalGoodAndBadDurations.second / 1000;
        // This value could be different from mLastCallQuality.getCallDuration if we support
        // handover from IMS->CS->IMS, but this is currently not possible
        // TODO(b/130302396) this also may be possible when we put a call on hold and continue with
        // another call
        summary.totalDurationWithQualityInformationInSeconds =
                mLastCallQuality.getCallDuration() / 1000;
        if (mWorstSsWithGoodDlQuality != null) {
            summary.snapshotOfWorstSsWithGoodQuality =
                    toCallQualityProto(mWorstSsWithGoodDlQuality.first);
            summary.worstSsWithGoodQuality = toProto(mWorstSsWithGoodDlQuality.second);
        }
        if (mBestSsWithGoodDlQuality != null) {
            summary.snapshotOfBestSsWithGoodQuality =
                    toCallQualityProto(mBestSsWithGoodDlQuality.first);
            summary.bestSsWithGoodQuality = toProto(mBestSsWithGoodDlQuality.second);
        }
        if (mWorstSsWithBadDlQuality != null) {
            summary.snapshotOfWorstSsWithBadQuality =
                    toCallQualityProto(mWorstSsWithBadDlQuality.first);
            summary.worstSsWithBadQuality = toProto(mWorstSsWithBadDlQuality.second);
        }
        if (mBestSsWithBadDlQuality != null) {
            summary.snapshotOfBestSsWithBadQuality =
                    toCallQualityProto(mBestSsWithBadDlQuality.first);
            summary.bestSsWithBadQuality = toProto(mBestSsWithBadDlQuality.second);
        }
        summary.snapshotOfEnd = toCallQualityProto(mLastCallQuality);
        return summary;
    }

    /**
     * Return the full uplink CallQualitySummary using the saved CallQuality records.
     */
    public TelephonyCallSession.Event.CallQualitySummary getCallQualitySummaryUl() {
        TelephonyCallSession.Event.CallQualitySummary summary =
                new TelephonyCallSession.Event.CallQualitySummary();
        Pair<Integer, Integer> totalGoodAndBadDurations = getTotalGoodAndBadQualityTimeMs(
                mFullUplinkQuality);
        summary.totalGoodQualityDurationInSeconds = totalGoodAndBadDurations.first / 1000;
        summary.totalBadQualityDurationInSeconds = totalGoodAndBadDurations.second / 1000;
        // This value could be different from mLastCallQuality.getCallDuration if we support
        // handover from IMS->CS->IMS, but this is currently not possible
        // TODO(b/130302396) this also may be possible when we put a call on hold and continue with
        // another call
        summary.totalDurationWithQualityInformationInSeconds =
                mLastCallQuality.getCallDuration() / 1000;
        if (mWorstSsWithGoodUlQuality != null) {
            summary.snapshotOfWorstSsWithGoodQuality =
                    toCallQualityProto(mWorstSsWithGoodUlQuality.first);
            summary.worstSsWithGoodQuality = toProto(mWorstSsWithGoodUlQuality.second);
        }
        if (mBestSsWithGoodUlQuality != null) {
            summary.snapshotOfBestSsWithGoodQuality =
                    toCallQualityProto(mBestSsWithGoodUlQuality.first);
            summary.bestSsWithGoodQuality = toProto(mBestSsWithGoodUlQuality.second);
        }
        if (mWorstSsWithBadUlQuality != null) {
            summary.snapshotOfWorstSsWithBadQuality =
                    toCallQualityProto(mWorstSsWithBadUlQuality.first);
            summary.worstSsWithBadQuality = toProto(mWorstSsWithBadUlQuality.second);
        }
        if (mBestSsWithBadUlQuality != null) {
            summary.snapshotOfBestSsWithBadQuality =
                    toCallQualityProto(mBestSsWithBadUlQuality.first);
            summary.bestSsWithBadQuality = toProto(mBestSsWithBadUlQuality.second);
        }
        summary.snapshotOfEnd = toCallQualityProto(mLastCallQuality);
        return summary;
    }


    /**
     * Container class for call quality level and signal strength at the time of snapshot. This
     * class implements compareTo so that it can be sorted by timestamp
     */
    private class TimestampedQualitySnapshot implements Comparable<TimestampedQualitySnapshot> {
        int mTimestampMs;
        int mCallQualityLevel;

        TimestampedQualitySnapshot(int timestamp, int cq) {
            mTimestampMs = timestamp;
            mCallQualityLevel = cq;
        }

        @Override
        public int compareTo(TimestampedQualitySnapshot o) {
            return this.mTimestampMs - o.mTimestampMs;
        }

        @Override
        public String toString() {
            return "mTimestampMs=" + mTimestampMs + " mCallQualityLevel=" + mCallQualityLevel;
        }
    }

    /**
     * Use a list of snapshots to calculate and return the total time spent in a call with good
     * quality and bad quality.
     * This is slightly expensive since it involves sorting the snapshots by timestamp.
     *
     * @param snapshots a list of uplink or downlink snapshots
     * @return a pair where the first element is the total good quality time and the second element
     * is the total bad quality time
     */
    private Pair<Integer, Integer> getTotalGoodAndBadQualityTimeMs(
            ArrayList<TimestampedQualitySnapshot> snapshots) {
        int totalGoodQualityTime = 0;
        int totalBadQualityTime = 0;
        int lastTimestamp = 0;
        // sort by timestamp using TimestampedQualitySnapshot.compareTo
        Collections.sort(snapshots);
        for (TimestampedQualitySnapshot snapshot : snapshots) {
            int timeSinceLastSnapshot = snapshot.mTimestampMs - lastTimestamp;
            if (isGoodQuality(snapshot.mCallQualityLevel)) {
                totalGoodQualityTime += timeSinceLastSnapshot;
            } else {
                totalBadQualityTime += timeSinceLastSnapshot;
            }
            lastTimestamp = snapshot.mTimestampMs;
        }

        return Pair.create(totalGoodQualityTime, totalBadQualityTime);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[CallQualityMetrics phone ");
        sb.append(mPhone.getPhoneId());
        sb.append(" mUlSnapshots: {");
        for (Pair<CallQuality, Integer> snapshot : mUlSnapshots) {
            sb.append(" {cq=");
            sb.append(snapshot.first);
            sb.append(" ss=");
            sb.append(snapshot.second);
            sb.append("}");
        }
        sb.append("}");
        sb.append(" mDlSnapshots:{");
        for (Pair<CallQuality, Integer> snapshot : mDlSnapshots) {
            sb.append(" {cq=");
            sb.append(snapshot.first);
            sb.append(" ss=");
            sb.append(snapshot.second);
            sb.append("}");
        }
        sb.append("}");
        sb.append(" ");
        Pair<Integer, Integer> dlTotals = getTotalGoodAndBadQualityTimeMs(mFullDownlinkQuality);
        Pair<Integer, Integer> ulTotals = getTotalGoodAndBadQualityTimeMs(mFullUplinkQuality);
        sb.append(" TotalDlGoodQualityTimeMs: ");
        sb.append(dlTotals.first);
        sb.append(" TotalDlBadQualityTimeMs: ");
        sb.append(dlTotals.second);
        sb.append(" TotalUlGoodQualityTimeMs: ");
        sb.append(ulTotals.first);
        sb.append(" TotalUlBadQualityTimeMs: ");
        sb.append(ulTotals.second);
        sb.append("]");
        return sb.toString();
    }
}
