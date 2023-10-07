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

package com.android.internal.telephony.metrics;

import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteController;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteIncomingDatagram;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteOutgoingDatagram;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteProvision;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteSession;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteSosMessageRecommender;
import com.android.telephony.Rlog;

/** Tracks Satellite metrics for each phone */
public class SatelliteStats {
    private static final String TAG = SatelliteStats.class.getSimpleName();

    private final PersistAtomsStorage mAtomsStorage =
            PhoneFactory.getMetricsCollector().getAtomsStorage();

    private static SatelliteStats sInstance = null;

    /** Gets the instance of SatelliteStats */
    public static SatelliteStats getInstance() {
        if (sInstance == null) {
            Rlog.d(TAG, "SatelliteStats created.");
            synchronized (SatelliteStats.class) {
                sInstance = new SatelliteStats();
            }
        }
        return sInstance;
    }

    /**
     * A data class to contain whole component of {@link SatelliteController) atom.
     * Refer to {@link #onSatelliteControllerMetrics(SatelliteControllerParams)}.
     */
    public class SatelliteControllerParams {
        private final int mCountOfSatelliteServiceEnablementsSuccess;
        private final int mCountOfSatelliteServiceEnablementsFail;
        private final int mCountOfOutgoingDatagramSuccess;
        private final int mCountOfOutgoingDatagramFail;
        private final int mCountOfIncomingDatagramSuccess;
        private final int mCountOfIncomingDatagramFail;
        private final int mCountOfDatagramTypeSosSmsSuccess;
        private final int mCountOfDatagramTypeSosSmsFail;
        private final int mCountOfDatagramTypeLocationSharingSuccess;
        private final int mCountOfDatagramTypeLocationSharingFail;
        private final int mCountOfProvisionSuccess;
        private final int mCountOfProvisionFail;
        private final int mCountOfDeprovisionSuccess;
        private final int mCountOfDeprovisionFail;
        private final int mTotalServiceUptimeSec;
        private final int mTotalBatteryConsumptionPercent;
        private final int mTotalBatteryChargedTimeSec;

        private SatelliteControllerParams(Builder builder) {
            this.mCountOfSatelliteServiceEnablementsSuccess =
                    builder.mCountOfSatelliteServiceEnablementsSuccess;
            this.mCountOfSatelliteServiceEnablementsFail =
                    builder.mCountOfSatelliteServiceEnablementsFail;
            this.mCountOfOutgoingDatagramSuccess = builder.mCountOfOutgoingDatagramSuccess;
            this.mCountOfOutgoingDatagramFail = builder.mCountOfOutgoingDatagramFail;
            this.mCountOfIncomingDatagramSuccess = builder.mCountOfIncomingDatagramSuccess;
            this.mCountOfIncomingDatagramFail = builder.mCountOfIncomingDatagramFail;
            this.mCountOfDatagramTypeSosSmsSuccess = builder.mCountOfDatagramTypeSosSmsSuccess;
            this.mCountOfDatagramTypeSosSmsFail = builder.mCountOfDatagramTypeSosSmsFail;
            this.mCountOfDatagramTypeLocationSharingSuccess =
                    builder.mCountOfDatagramTypeLocationSharingSuccess;
            this.mCountOfDatagramTypeLocationSharingFail =
                    builder.mCountOfDatagramTypeLocationSharingFail;
            this.mCountOfProvisionSuccess = builder.mCountOfProvisionSuccess;
            this.mCountOfProvisionFail = builder.mCountOfProvisionFail;
            this.mCountOfDeprovisionSuccess = builder.mCountOfDeprovisionSuccess;
            this.mCountOfDeprovisionFail = builder.mCountOfDeprovisionFail;
            this.mTotalServiceUptimeSec = builder.mTotalServiceUptimeSec;
            this.mTotalBatteryConsumptionPercent = builder.mTotalBatteryConsumptionPercent;
            this.mTotalBatteryChargedTimeSec = builder.mTotalBatteryChargedTimeSec;
        }

        public int getCountOfSatelliteServiceEnablementsSuccess() {
            return mCountOfSatelliteServiceEnablementsSuccess;
        }

        public int getCountOfSatelliteServiceEnablementsFail() {
            return mCountOfSatelliteServiceEnablementsFail;
        }

        public int getCountOfOutgoingDatagramSuccess() {
            return mCountOfOutgoingDatagramSuccess;
        }

        public int getCountOfOutgoingDatagramFail() {
            return mCountOfOutgoingDatagramFail;
        }

        public int getCountOfIncomingDatagramSuccess() {
            return mCountOfIncomingDatagramSuccess;
        }

        public int getCountOfIncomingDatagramFail() {
            return mCountOfIncomingDatagramFail;
        }

        public int getCountOfDatagramTypeSosSmsSuccess() {
            return mCountOfDatagramTypeSosSmsSuccess;
        }

        public int getCountOfDatagramTypeSosSmsFail() {
            return mCountOfDatagramTypeSosSmsFail;
        }

        public int getCountOfDatagramTypeLocationSharingSuccess() {
            return mCountOfDatagramTypeLocationSharingSuccess;
        }

        public int getCountOfDatagramTypeLocationSharingFail() {
            return mCountOfDatagramTypeLocationSharingFail;
        }

        public int getCountOfProvisionSuccess() {
            return mCountOfProvisionSuccess;
        }

        public int getCountOfProvisionFail() {
            return mCountOfProvisionFail;
        }

        public int getCountOfDeprovisionSuccess() {
            return mCountOfDeprovisionSuccess;
        }

        public int getCountOfDeprovisionFail() {
            return mCountOfDeprovisionFail;
        }

        public int getTotalServiceUptimeSec() {
            return mTotalServiceUptimeSec;
        }

        public int getTotalBatteryConsumptionPercent() {
            return mTotalBatteryConsumptionPercent;
        }

        public int getTotalBatteryChargedTimeSec() {
            return mTotalBatteryChargedTimeSec;
        }

        /**
         * A builder class to create {@link SatelliteControllerParams} data structure class
         */
        public static class Builder {
            private int mCountOfSatelliteServiceEnablementsSuccess = 0;
            private int mCountOfSatelliteServiceEnablementsFail = 0;
            private int mCountOfOutgoingDatagramSuccess = 0;
            private int mCountOfOutgoingDatagramFail = 0;
            private int mCountOfIncomingDatagramSuccess = 0;
            private int mCountOfIncomingDatagramFail = 0;
            private int mCountOfDatagramTypeSosSmsSuccess = 0;
            private int mCountOfDatagramTypeSosSmsFail = 0;
            private int mCountOfDatagramTypeLocationSharingSuccess = 0;
            private int mCountOfDatagramTypeLocationSharingFail = 0;
            private int mCountOfProvisionSuccess;
            private int mCountOfProvisionFail;
            private int mCountOfDeprovisionSuccess;
            private int mCountOfDeprovisionFail;
            private int mTotalServiceUptimeSec = 0;
            private int mTotalBatteryConsumptionPercent = 0;
            private int mTotalBatteryChargedTimeSec = 0;

            /**
             * Sets countOfSatelliteServiceEnablementsSuccess value of {@link SatelliteController}
             * atom then returns Builder class
             */
            public Builder setCountOfSatelliteServiceEnablementsSuccess(
                    int countOfSatelliteServiceEnablementsSuccess) {
                this.mCountOfSatelliteServiceEnablementsSuccess =
                        countOfSatelliteServiceEnablementsSuccess;
                return this;
            }

            /**
             * Sets countOfSatelliteServiceEnablementsFail value of {@link SatelliteController} atom
             * then returns Builder class
             */
            public Builder setCountOfSatelliteServiceEnablementsFail(
                    int countOfSatelliteServiceEnablementsFail) {
                this.mCountOfSatelliteServiceEnablementsFail =
                        countOfSatelliteServiceEnablementsFail;
                return this;
            }

            /**
             * Sets countOfOutgoingDatagramSuccess value of {@link SatelliteController} atom then
             * returns Builder class
             */
            public Builder setCountOfOutgoingDatagramSuccess(int countOfOutgoingDatagramSuccess) {
                this.mCountOfOutgoingDatagramSuccess = countOfOutgoingDatagramSuccess;
                return this;
            }

            /**
             * Sets countOfOutgoingDatagramFail value of {@link SatelliteController} atom then
             * returns Builder class
             */
            public Builder setCountOfOutgoingDatagramFail(int countOfOutgoingDatagramFail) {
                this.mCountOfOutgoingDatagramFail = countOfOutgoingDatagramFail;
                return this;
            }

            /**
             * Sets countOfIncomingDatagramSuccess value of {@link SatelliteController} atom then
             * returns Builder class
             */
            public Builder setCountOfIncomingDatagramSuccess(int countOfIncomingDatagramSuccess) {
                this.mCountOfIncomingDatagramSuccess = countOfIncomingDatagramSuccess;
                return this;
            }

            /**
             * Sets countOfIncomingDatagramFail value of {@link SatelliteController} atom then
             * returns Builder class
             */
            public Builder setCountOfIncomingDatagramFail(int countOfIncomingDatagramFail) {
                this.mCountOfIncomingDatagramFail = countOfIncomingDatagramFail;
                return this;
            }

            /**
             * Sets countOfDatagramTypeSosSmsSuccess value of {@link SatelliteController} atom then
             * returns Builder class
             */
            public Builder setCountOfDatagramTypeSosSmsSuccess(
                    int countOfDatagramTypeSosSmsSuccess) {
                this.mCountOfDatagramTypeSosSmsSuccess = countOfDatagramTypeSosSmsSuccess;
                return this;
            }

            /**
             * Sets countOfDatagramTypeSosSmsFail value of {@link SatelliteController} atom then
             * returns Builder class
             */
            public Builder setCountOfDatagramTypeSosSmsFail(int countOfDatagramTypeSosSmsFail) {
                this.mCountOfDatagramTypeSosSmsFail = countOfDatagramTypeSosSmsFail;
                return this;
            }

            /**
             * Sets countOfDatagramTypeLocationSharingSuccess value of {@link SatelliteController}
             * atom then returns Builder class
             */
            public Builder setCountOfDatagramTypeLocationSharingSuccess(
                    int countOfDatagramTypeLocationSharingSuccess) {
                this.mCountOfDatagramTypeLocationSharingSuccess =
                        countOfDatagramTypeLocationSharingSuccess;
                return this;
            }

            /**
             * Sets countOfDatagramTypeLocationSharingFail value of {@link SatelliteController}
             * atom then returns Builder class
             */
            public Builder setCountOfDatagramTypeLocationSharingFail(
                    int countOfDatagramTypeLocationSharingFail) {
                this.mCountOfDatagramTypeLocationSharingFail =
                        countOfDatagramTypeLocationSharingFail;
                return this;
            }

            /**
             * Sets countOfProvisionSuccess value of {@link SatelliteController}
             * atom then returns Builder class
             */
            public Builder setCountOfProvisionSuccess(int countOfProvisionSuccess) {
                this.mCountOfProvisionSuccess = countOfProvisionSuccess;
                return this;
            }

            /**
             * Sets countOfProvisionFail value of {@link SatelliteController}
             * atom then returns Builder class
             */
            public Builder setCountOfProvisionFail(int countOfProvisionFail) {
                this.mCountOfProvisionFail = countOfProvisionFail;
                return this;
            }

            /**
             * Sets countOfDeprovisionSuccess value of {@link SatelliteController}
             * atom then returns Builder class
             */
            public Builder setCountOfDeprovisionSuccess(int countOfDeprovisionSuccess) {
                this.mCountOfDeprovisionSuccess = countOfDeprovisionSuccess;
                return this;
            }

            /**
             * Sets countOfDeprovisionSuccess value of {@link SatelliteController}
             * atom then returns Builder class
             */
            public Builder setCountOfDeprovisionFail(int countOfDeprovisionFail) {
                this.mCountOfDeprovisionFail = countOfDeprovisionFail;
                return this;
            }

            /**
             * Sets totalServiceUptimeSec value of {@link SatelliteController} atom then
             * returns Builder class
             */
            public Builder setTotalServiceUptimeSec(int totalServiceUptimeSec) {
                this.mTotalServiceUptimeSec = totalServiceUptimeSec;
                return this;
            }

            /**
             * Sets totalBatteryConsumptionPercent value of {@link SatelliteController} atom then
             * returns Builder class
             */
            public Builder setTotalBatteryConsumptionPercent(int totalBatteryConsumptionPercent) {
                this.mTotalBatteryConsumptionPercent = totalBatteryConsumptionPercent;
                return this;
            }

            /**
             * Sets totalBatteryChargedTimeSec value of {@link SatelliteController} atom then
             * returns Builder class
             */
            public Builder setTotalBatteryChargedTimeSec(int totalBatteryChargedTimeSec) {
                this.mTotalBatteryChargedTimeSec = totalBatteryChargedTimeSec;
                return this;
            }

            /**
             * Returns ControllerParams, which contains whole component of
             * {@link SatelliteController} atom
             */
            public SatelliteControllerParams build() {
                return new SatelliteStats()
                        .new SatelliteControllerParams(this);
            }
        }

        @Override
        public String toString() {
            return "ControllerParams("
                    + ", countOfSatelliteServiceEnablementsSuccess="
                    + mCountOfSatelliteServiceEnablementsSuccess
                    + ", countOfSatelliteServiceEnablementsFail="
                    + mCountOfSatelliteServiceEnablementsFail
                    + ", countOfOutgoingDatagramSuccess=" + mCountOfOutgoingDatagramSuccess
                    + ", countOfOutgoingDatagramFail=" + mCountOfOutgoingDatagramFail
                    + ", countOfIncomingDatagramSuccess=" + mCountOfIncomingDatagramSuccess
                    + ", countOfIncomingDatagramFail=" + mCountOfIncomingDatagramFail
                    + ", countOfDatagramTypeSosSms=" + mCountOfDatagramTypeSosSmsSuccess
                    + ", countOfDatagramTypeSosSms=" + mCountOfDatagramTypeSosSmsFail
                    + ", countOfDatagramTypeLocationSharing="
                    + mCountOfDatagramTypeLocationSharingSuccess
                    + ", countOfDatagramTypeLocationSharing="
                    + mCountOfDatagramTypeLocationSharingFail
                    + ", serviceUptimeSec=" + mTotalServiceUptimeSec
                    + ", batteryConsumptionPercent=" + mTotalBatteryConsumptionPercent
                    + ", batteryChargedTimeSec=" + mTotalBatteryChargedTimeSec
                    + ")";
        }
    }

    /**
     * A data class to contain whole component of {@link SatelliteSession) atom.
     * Refer to {@link #onSatelliteSessionMetrics(SatelliteSessionParams)}.
     */
    public class SatelliteSessionParams {
        private final int mSatelliteServiceInitializationResult;
        private final int mSatelliteTechnology;

        private SatelliteSessionParams(Builder builder) {
            this.mSatelliteServiceInitializationResult =
                    builder.mSatelliteServiceInitializationResult;
            this.mSatelliteTechnology = builder.mSatelliteTechnology;
        }

        public int getSatelliteServiceInitializationResult() {
            return mSatelliteServiceInitializationResult;
        }

        public int getSatelliteTechnology() {
            return mSatelliteTechnology;
        }

        /**
         * A builder class to create {@link SatelliteSessionParams} data structure class
         */
        public static class Builder {
            private int mSatelliteServiceInitializationResult = -1;
            private int mSatelliteTechnology = -1;

            /**
             * Sets satelliteServiceInitializationResult value of {@link SatelliteSession}
             * atom then returns Builder class
             */
            public Builder setSatelliteServiceInitializationResult(
                    int satelliteServiceInitializationResult) {
                this.mSatelliteServiceInitializationResult = satelliteServiceInitializationResult;
                return this;
            }

            /**
             * Sets satelliteTechnology value of {@link SatelliteSession} atoms then
             * returns Builder class
             */
            public Builder setSatelliteTechnology(int satelliteTechnology) {
                this.mSatelliteTechnology = satelliteTechnology;
                return this;
            }

            /**
             * Returns SessionParams, which contains whole component of
             * {@link SatelliteSession} atom
             */
            public SatelliteSessionParams build() {
                return new SatelliteStats()
                        .new SatelliteSessionParams(this);
            }
        }

        @Override
        public String toString() {
            return "SessionParams("
                    + ", satelliteServiceInitializationResult="
                    + mSatelliteServiceInitializationResult
                    + ", satelliteTechnology=" + mSatelliteTechnology
                    + ")";
        }
    }

    /**
     * A data class to contain whole component of {@link SatelliteIncomingDatagram} atom.
     * Refer to {@link #onSatelliteIncomingDatagramMetrics(SatelliteIncomingDatagramParams)}.
     */
    public class SatelliteIncomingDatagramParams {
        private final int mResultCode;
        private final int mDatagramSizeBytes;
        private final long mDatagramTransferTimeMillis;

        private SatelliteIncomingDatagramParams(Builder builder) {
            this.mResultCode = builder.mResultCode;
            this.mDatagramSizeBytes = builder.mDatagramSizeBytes;
            this.mDatagramTransferTimeMillis = builder.mDatagramTransferTimeMillis;
        }

        public int getResultCode() {
            return mResultCode;
        }

        public int getDatagramSizeBytes() {
            return mDatagramSizeBytes;
        }

        public long getDatagramTransferTimeMillis() {
            return mDatagramTransferTimeMillis;
        }

        /**
         * A builder class to create {@link SatelliteIncomingDatagramParams} data structure class
         */
        public static class Builder {
            private int mResultCode = -1;
            private int mDatagramSizeBytes = -1;
            private long mDatagramTransferTimeMillis = -1;

            /**
             * Sets resultCode value of {@link SatelliteIncomingDatagram} atom
             * then returns Builder class
             */
            public Builder setResultCode(int resultCode) {
                this.mResultCode = resultCode;
                return this;
            }

            /**
             * Sets datagramSizeBytes value of {@link SatelliteIncomingDatagram} atom
             * then returns Builder class
             */
            public Builder setDatagramSizeBytes(int datagramSizeBytes) {
                this.mDatagramSizeBytes = datagramSizeBytes;
                return this;
            }

            /**
             * Sets datagramTransferTimeMillis value of {@link SatelliteIncomingDatagram} atom
             * then returns Builder class
             */
            public Builder setDatagramTransferTimeMillis(long datagramTransferTimeMillis) {
                this.mDatagramTransferTimeMillis = datagramTransferTimeMillis;
                return this;
            }

            /**
             * Returns IncomingDatagramParams, which contains whole component of
             * {@link SatelliteIncomingDatagram} atom
             */
            public SatelliteIncomingDatagramParams build() {
                return new SatelliteStats()
                        .new SatelliteIncomingDatagramParams(Builder.this);
            }
        }

        @Override
        public String toString() {
            return "IncomingDatagramParams("
                    + ", resultCode=" + mResultCode
                    + ", datagramSizeBytes=" + mDatagramSizeBytes
                    + ", datagramTransferTimeMillis=" + mDatagramTransferTimeMillis + ")";
        }
    }

    /**
     * A data class to contain whole component of {@link SatelliteOutgoingDatagram} atom.
     * Refer to {@link #onSatelliteOutgoingDatagramMetrics(SatelliteOutgoingDatagramParams)}.
     */
    public class SatelliteOutgoingDatagramParams {
        private final int mDatagramType;
        private final int mResultCode;
        private final int mDatagramSizeBytes;
        private final long mDatagramTransferTimeMillis;

        private SatelliteOutgoingDatagramParams(Builder builder) {
            this.mDatagramType = builder.mDatagramType;
            this.mResultCode = builder.mResultCode;
            this.mDatagramSizeBytes = builder.mDatagramSizeBytes;
            this.mDatagramTransferTimeMillis = builder.mDatagramTransferTimeMillis;
        }

        public int getDatagramType() {
            return mDatagramType;
        }

        public int getResultCode() {
            return mResultCode;
        }

        public int getDatagramSizeBytes() {
            return mDatagramSizeBytes;
        }

        public long getDatagramTransferTimeMillis() {
            return mDatagramTransferTimeMillis;
        }

        /**
         * A builder class to create {@link SatelliteOutgoingDatagramParams} data structure class
         */
        public static class Builder {
            private int mDatagramType = -1;
            private int mResultCode = -1;
            private int mDatagramSizeBytes = -1;
            private long mDatagramTransferTimeMillis = -1;

            /**
             * Sets datagramType value of {@link SatelliteOutgoingDatagram} atom
             * then returns Builder class
             */
            public Builder setDatagramType(int datagramType) {
                this.mDatagramType = datagramType;
                return this;
            }

            /**
             * Sets resultCode value of {@link SatelliteOutgoingDatagram} atom
             * then returns Builder class
             */
            public Builder setResultCode(int resultCode) {
                this.mResultCode = resultCode;
                return this;
            }

            /**
             * Sets datagramSizeBytes value of {@link SatelliteOutgoingDatagram} atom
             * then returns Builder class
             */
            public Builder setDatagramSizeBytes(int datagramSizeBytes) {
                this.mDatagramSizeBytes = datagramSizeBytes;
                return this;
            }

            /**
             * Sets datagramTransferTimeMillis value of {@link SatelliteOutgoingDatagram} atom
             * then returns Builder class
             */
            public Builder setDatagramTransferTimeMillis(long datagramTransferTimeMillis) {
                this.mDatagramTransferTimeMillis = datagramTransferTimeMillis;
                return this;
            }

            /**
             * Returns OutgoingDatagramParams, which contains whole component of
             * {@link SatelliteOutgoingDatagram} atom
             */
            public SatelliteOutgoingDatagramParams build() {
                return new SatelliteStats()
                        .new SatelliteOutgoingDatagramParams(Builder.this);
            }
        }

        @Override
        public String toString() {
            return "OutgoingDatagramParams("
                    + "datagramType=" + mDatagramType
                    + ", resultCode=" + mResultCode
                    + ", datagramSizeBytes=" + mDatagramSizeBytes
                    + ", datagramTransferTimeMillis=" + mDatagramTransferTimeMillis + ")";
        }
    }

    /**
     * A data class to contain whole component of {@link SatelliteProvision} atom.
     * Refer to {@link #onSatelliteProvisionMetrics(SatelliteProvisionParams)}.
     */
    public class SatelliteProvisionParams {
        private final int mResultCode;
        private final int mProvisioningTimeSec;
        private final boolean mIsProvisionRequest;
        private final boolean mIsCanceled;

        private SatelliteProvisionParams(Builder builder) {
            this.mResultCode = builder.mResultCode;
            this.mProvisioningTimeSec = builder.mProvisioningTimeSec;
            this.mIsProvisionRequest = builder.mIsProvisionRequest;
            this.mIsCanceled = builder.mIsCanceled;
        }

        public int getResultCode() {
            return mResultCode;
        }

        public int getProvisioningTimeSec() {
            return mProvisioningTimeSec;
        }

        public boolean getIsProvisionRequest() {
            return mIsProvisionRequest;
        }

        public boolean getIsCanceled() {
            return mIsCanceled;
        }

        /**
         * A builder class to create {@link SatelliteProvisionParams} data structure class
         */
        public static class Builder {
            private int mResultCode = -1;
            private int mProvisioningTimeSec = -1;
            private boolean mIsProvisionRequest = false;
            private boolean mIsCanceled = false;

            /**
             * Sets resultCode value of {@link SatelliteProvision} atom
             * then returns Builder class
             */
            public Builder setResultCode(int resultCode) {
                this.mResultCode = resultCode;
                return this;
            }

            /**
             * Sets provisioningTimeSec value of {@link SatelliteProvision} atom
             * then returns Builder class
             */
            public Builder setProvisioningTimeSec(int provisioningTimeSec) {
                this.mProvisioningTimeSec = provisioningTimeSec;
                return this;
            }

            /**
             * Sets isProvisionRequest value of {@link SatelliteProvision} atom
             * then returns Builder class
             */
            public Builder setIsProvisionRequest(boolean isProvisionRequest) {
                this.mIsProvisionRequest = isProvisionRequest;
                return this;
            }

            /**
             * Sets isCanceled value of {@link SatelliteProvision} atom
             * then returns Builder class
             */
            public Builder setIsCanceled(boolean isCanceled) {
                this.mIsCanceled = isCanceled;
                return this;
            }

            /**
             * Returns ProvisionParams, which contains whole component of
             * {@link SatelliteProvision} atom
             */
            public SatelliteProvisionParams build() {
                return new SatelliteStats()
                        .new SatelliteProvisionParams(Builder.this);
            }
        }

        @Override
        public String toString() {
            return "ProvisionParams("
                    + "resultCode=" + mResultCode
                    + ", provisioningTimeSec=" + mProvisioningTimeSec
                    + ", isProvisionRequest=" + mIsProvisionRequest
                    + ", isCanceled" + mIsCanceled + ")";
        }
    }

    /**
     * A data class to contain whole component of {@link SatelliteSosMessageRecommender} atom.
     * Refer to {@link #onSatelliteSosMessageRecommender(SatelliteSosMessageRecommenderParams)}.
     */
    public class SatelliteSosMessageRecommenderParams {
        private final boolean mIsDisplaySosMessageSent;
        private final int mCountOfTimerStarted;
        private final boolean mIsImsRegistered;
        private final int mCellularServiceState;

        private SatelliteSosMessageRecommenderParams(Builder builder) {
            this.mIsDisplaySosMessageSent = builder.mIsDisplaySosMessageSent;
            this.mCountOfTimerStarted = builder.mCountOfTimerStarted;
            this.mIsImsRegistered = builder.mIsImsRegistered;
            this.mCellularServiceState = builder.mCellularServiceState;
        }

        public boolean isDisplaySosMessageSent() {
            return mIsDisplaySosMessageSent;
        }

        public int getCountOfTimerStarted() {
            return mCountOfTimerStarted;
        }

        public boolean isImsRegistered() {
            return mIsImsRegistered;
        }

        public int getCellularServiceState() {
            return mCellularServiceState;
        }

        /**
         * A builder class to create {@link SatelliteProvisionParams} data structure class
         */
        public static class Builder {
            private boolean mIsDisplaySosMessageSent = false;
            private int mCountOfTimerStarted = -1;
            private boolean mIsImsRegistered = false;
            private int mCellularServiceState = -1;

            /**
             * Sets resultCode value of {@link SatelliteSosMessageRecommender} atom
             * then returns Builder class
             */
            public Builder setDisplaySosMessageSent(
                    boolean isDisplaySosMessageSent) {
                this.mIsDisplaySosMessageSent = isDisplaySosMessageSent;
                return this;
            }

            /**
             * Sets countOfTimerIsStarted value of {@link SatelliteSosMessageRecommender} atom
             * then returns Builder class
             */
            public Builder setCountOfTimerStarted(int countOfTimerStarted) {
                this.mCountOfTimerStarted = countOfTimerStarted;
                return this;
            }

            /**
             * Sets isImsRegistered value of {@link SatelliteSosMessageRecommender} atom
             * then returns Builder class
             */
            public Builder setImsRegistered(boolean isImsRegistered) {
                this.mIsImsRegistered = isImsRegistered;
                return this;
            }

            /**
             * Sets cellularServiceState value of {@link SatelliteSosMessageRecommender} atom
             * then returns Builder class
             */
            public Builder setCellularServiceState(int cellularServiceState) {
                this.mCellularServiceState = cellularServiceState;
                return this;
            }

            /**
             * Returns SosMessageRecommenderParams, which contains whole component of
             * {@link SatelliteSosMessageRecommenderParams} atom
             */
            public SatelliteSosMessageRecommenderParams build() {
                return new SatelliteStats()
                        .new SatelliteSosMessageRecommenderParams(Builder.this);
            }
        }

        @Override
        public String toString() {
            return "SosMessageRecommenderParams("
                    + "isDisplaySosMessageSent=" + mIsDisplaySosMessageSent
                    + ", countOfTimerStarted=" + mCountOfTimerStarted
                    + ", isImsRegistered=" + mIsImsRegistered
                    + ", cellularServiceState=" + mCellularServiceState + ")";
        }
    }

    /**  Create a new atom or update an existing atom for SatelliteController metrics */
    public synchronized void onSatelliteControllerMetrics(SatelliteControllerParams param) {
        SatelliteController proto = new SatelliteController();
        proto.countOfSatelliteServiceEnablementsSuccess =
                param.getCountOfSatelliteServiceEnablementsSuccess();
        proto.countOfSatelliteServiceEnablementsFail =
                param.getCountOfSatelliteServiceEnablementsFail();
        proto.countOfOutgoingDatagramSuccess = param.getCountOfOutgoingDatagramSuccess();
        proto.countOfOutgoingDatagramFail = param.getCountOfOutgoingDatagramFail();
        proto.countOfIncomingDatagramSuccess = param.getCountOfIncomingDatagramSuccess();
        proto.countOfIncomingDatagramFail = param.getCountOfIncomingDatagramFail();
        proto.countOfDatagramTypeSosSmsSuccess = param.getCountOfDatagramTypeSosSmsSuccess();
        proto.countOfDatagramTypeSosSmsFail = param.getCountOfDatagramTypeSosSmsFail();
        proto.countOfDatagramTypeLocationSharingSuccess =
                param.getCountOfDatagramTypeLocationSharingSuccess();
        proto.countOfDatagramTypeLocationSharingFail =
                param.getCountOfDatagramTypeLocationSharingFail();
        proto.countOfProvisionSuccess = param.getCountOfProvisionSuccess();
        proto.countOfProvisionFail = param.getCountOfProvisionFail();
        proto.countOfDeprovisionSuccess = param.getCountOfDeprovisionSuccess();
        proto.countOfDeprovisionFail = param.getCountOfDeprovisionFail();
        proto.totalServiceUptimeSec = param.getTotalServiceUptimeSec();
        proto.totalBatteryConsumptionPercent = param.getTotalBatteryConsumptionPercent();
        proto.totalBatteryChargedTimeSec = param.getTotalBatteryChargedTimeSec();

        mAtomsStorage.addSatelliteControllerStats(proto);
    }

    /**  Create a new atom or update an existing atom for SatelliteSession metrics */
    public synchronized void onSatelliteSessionMetrics(SatelliteSessionParams param) {
        SatelliteSession proto = new SatelliteSession();
        proto.satelliteServiceInitializationResult =
                param.getSatelliteServiceInitializationResult();
        proto.satelliteTechnology = param.getSatelliteTechnology();
        proto.count = 1;
        mAtomsStorage.addSatelliteSessionStats(proto);
    }

    /**  Create a new atom for SatelliteIncomingDatagram metrics */
    public synchronized void onSatelliteIncomingDatagramMetrics(
            SatelliteIncomingDatagramParams param) {
        SatelliteIncomingDatagram proto = new SatelliteIncomingDatagram();
        proto.resultCode = param.getResultCode();
        proto.datagramSizeBytes = param.getDatagramSizeBytes();
        proto.datagramTransferTimeMillis = param.getDatagramTransferTimeMillis();
        mAtomsStorage.addSatelliteIncomingDatagramStats(proto);
    }

    /**  Create a new atom for SatelliteOutgoingDatagram metrics */
    public synchronized void onSatelliteOutgoingDatagramMetrics(
            SatelliteOutgoingDatagramParams param) {
        SatelliteOutgoingDatagram proto = new SatelliteOutgoingDatagram();
        proto.datagramType = param.getDatagramType();
        proto.resultCode = param.getResultCode();
        proto.datagramSizeBytes = param.getDatagramSizeBytes();
        proto.datagramTransferTimeMillis = param.getDatagramTransferTimeMillis();
        mAtomsStorage.addSatelliteOutgoingDatagramStats(proto);
    }

    /**  Create a new atom for SatelliteProvision metrics */
    public synchronized void onSatelliteProvisionMetrics(SatelliteProvisionParams param) {
        SatelliteProvision proto = new SatelliteProvision();
        proto.resultCode = param.getResultCode();
        proto.provisioningTimeSec = param.getProvisioningTimeSec();
        proto.isProvisionRequest = param.getIsProvisionRequest();
        proto.isCanceled = param.getIsCanceled();
        mAtomsStorage.addSatelliteProvisionStats(proto);
    }

    /**  Create a new atom or update an existing atom for SatelliteSosMessageRecommender metrics */
    public synchronized void onSatelliteSosMessageRecommender(
            SatelliteSosMessageRecommenderParams param) {
        SatelliteSosMessageRecommender proto = new SatelliteSosMessageRecommender();
        proto.isDisplaySosMessageSent = param.isDisplaySosMessageSent();
        proto.countOfTimerStarted = param.getCountOfTimerStarted();
        proto.isImsRegistered = param.isImsRegistered();
        proto.cellularServiceState = param.getCellularServiceState();
        proto.count = 1;
        mAtomsStorage.addSatelliteSosMessageRecommenderStats(proto);
    }
}
