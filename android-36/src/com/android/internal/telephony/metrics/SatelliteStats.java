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

import static android.telephony.satellite.NtnSignalStrength.NTN_SIGNAL_STRENGTH_NONE;
import static android.telephony.TelephonyManager.UNKNOWN_CARRIER_ID;

import static com.android.internal.telephony.satellite.SatelliteConstants.TRIGGERING_EVENT_UNKNOWN;

import android.telephony.satellite.NtnSignalStrength;
import android.telephony.satellite.SatelliteManager;

import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.nano.PersistAtomsProto.CarrierRoamingSatelliteControllerStats;
import com.android.internal.telephony.nano.PersistAtomsProto.CarrierRoamingSatelliteSession;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteAccessController;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteConfigUpdater;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteController;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteEntitlement;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteIncomingDatagram;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteOutgoingDatagram;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteProvision;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteSession;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteSosMessageRecommender;
import com.android.internal.telephony.satellite.SatelliteConstants;
import com.android.telephony.Rlog;

import java.util.Arrays;
import java.util.Optional;

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
        private final int mCountOfDemoModeSatelliteServiceEnablementsSuccess;
        private final int mCountOfDemoModeSatelliteServiceEnablementsFail;
        private final int mCountOfDemoModeOutgoingDatagramSuccess;
        private final int mCountOfDemoModeOutgoingDatagramFail;
        private final int mCountOfDemoModeIncomingDatagramSuccess;
        private final int mCountOfDemoModeIncomingDatagramFail;
        private final int mCountOfDatagramTypeKeepAliveSuccess;
        private final int mCountOfDatagramTypeKeepAliveFail;
        private final int mCountOfAllowedSatelliteAccess;
        private final int mCountOfDisallowedSatelliteAccess;
        private final int mCountOfSatelliteAccessCheckFail;
        private static boolean sIsProvisioned;
        private static int sCarrierId = UNKNOWN_CARRIER_ID;
        private final int mCountOfSatelliteAllowedStateChangedEvents;
        private final int mCountOfSuccessfulLocationQueries;
        private final int mCountOfFailedLocationQueries;
        private final int mCountOfP2PSmsAvailableNotificationShown;
        private final int mCountOfP2PSmsAvailableNotificationRemoved;
        private static boolean sIsNtnOnlyCarrier;
        private static int sVersionOfSatelliteAccessConfig;

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
            this.mCountOfDemoModeSatelliteServiceEnablementsSuccess =
                    builder.mCountOfDemoModeSatelliteServiceEnablementsSuccess;
            this.mCountOfDemoModeSatelliteServiceEnablementsFail =
                    builder.mCountOfDemoModeSatelliteServiceEnablementsFail;
            this.mCountOfDemoModeOutgoingDatagramSuccess =
                    builder.mCountOfDemoModeOutgoingDatagramSuccess;
            this.mCountOfDemoModeOutgoingDatagramFail =
                    builder.mCountOfDemoModeOutgoingDatagramFail;
            this.mCountOfDemoModeIncomingDatagramSuccess =
                    builder.mCountOfDemoModeIncomingDatagramSuccess;
            this.mCountOfDemoModeIncomingDatagramFail =
                    builder.mCountOfDemoModeIncomingDatagramFail;
            this.mCountOfDatagramTypeKeepAliveSuccess =
                    builder.mCountOfDatagramTypeKeepAliveSuccess;
            this.mCountOfDatagramTypeKeepAliveFail =
                    builder.mCountOfDatagramTypeKeepAliveFail;
            this.mCountOfAllowedSatelliteAccess =
                    builder.mCountOfAllowedSatelliteAccess;
            this.mCountOfDisallowedSatelliteAccess =
                    builder.mCountOfDisallowedSatelliteAccess;
            this.mCountOfSatelliteAccessCheckFail =
                    builder.mCountOfSatelliteAccessCheckFail;

            // isProvisioned value should be updated only when it is meaningful.
            if (builder.mIsProvisioned.isPresent()) {
                this.sIsProvisioned = builder.mIsProvisioned.get();
            }

            // Carrier ID value should be updated only when it is meaningful.
            if (builder.mCarrierId.isPresent()) {
                this.sCarrierId = builder.mCarrierId.get();
            }

            this.mCountOfSatelliteAllowedStateChangedEvents =
                    builder.mCountOfSatelliteAllowedStateChangedEvents;
            this.mCountOfSuccessfulLocationQueries =
                    builder.mCountOfSuccessfulLocationQueries;
            this.mCountOfFailedLocationQueries =
                    builder.mCountOfFailedLocationQueries;
            this.mCountOfP2PSmsAvailableNotificationShown =
                    builder.mCountOfP2PSmsAvailableNotificationShown;
            this.mCountOfP2PSmsAvailableNotificationRemoved =
                    builder.mCountOfP2PSmsAvailableNotificationRemoved;

            // Ntn only carrier value should be updated only when it is meaningful.
            if (builder.mIsNtnOnlyCarrier.isPresent()) {
                this.sIsNtnOnlyCarrier = builder.mIsNtnOnlyCarrier.get();
            }
            // version satellite access config value should be updated only when it is meaningful.
            if (builder.mVersionOfSatelliteAccessConfig.isPresent()) {
                this.sVersionOfSatelliteAccessConfig =
                        builder.mVersionOfSatelliteAccessConfig.get();
            }
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

        public int getCountOfDemoModeSatelliteServiceEnablementsSuccess() {
            return mCountOfDemoModeSatelliteServiceEnablementsSuccess;
        }

        public int getCountOfDemoModeSatelliteServiceEnablementsFail() {
            return mCountOfDemoModeSatelliteServiceEnablementsFail;
        }

        public int getCountOfDemoModeOutgoingDatagramSuccess() {
            return mCountOfDemoModeOutgoingDatagramSuccess;
        }

        public int getCountOfDemoModeOutgoingDatagramFail() {
            return mCountOfDemoModeOutgoingDatagramFail;
        }

        public int getCountOfDemoModeIncomingDatagramSuccess() {
            return mCountOfDemoModeIncomingDatagramSuccess;
        }

        public int getCountOfDemoModeIncomingDatagramFail() {
            return mCountOfDemoModeIncomingDatagramFail;
        }

        public int getCountOfDatagramTypeKeepAliveSuccess() {
            return mCountOfDatagramTypeKeepAliveSuccess;
        }

        public int getCountOfDatagramTypeKeepAliveFail() {
            return mCountOfDatagramTypeKeepAliveFail;
        }

        public int getCountOfAllowedSatelliteAccess() {
            return mCountOfAllowedSatelliteAccess;
        }

        public int getCountOfDisallowedSatelliteAccess() {
            return mCountOfDisallowedSatelliteAccess;
        }

        public int getCountOfSatelliteAccessCheckFail() {
            return mCountOfSatelliteAccessCheckFail;
        }

        public static boolean isProvisioned() {
            return sIsProvisioned;
        }

        public static int getCarrierId() {
            return sCarrierId;
        }

        public int getCountOfSatelliteAllowedStateChangedEvents() {
            return mCountOfSatelliteAllowedStateChangedEvents;
        }

        public int getCountOfSuccessfulLocationQueries() {
            return mCountOfSuccessfulLocationQueries;
        }

        public int getCountOfFailedLocationQueries() {
            return mCountOfFailedLocationQueries;
        }

        public int getCountOfP2PSmsAvailableNotificationShown() {
            return mCountOfP2PSmsAvailableNotificationShown;
        }

        public int getCountOfP2PSmsAvailableNotificationRemoved() {
            return mCountOfP2PSmsAvailableNotificationRemoved;
        }

        public static boolean isNtnOnlyCarrier() {
            return sIsNtnOnlyCarrier;
        }

        public static int getVersionSatelliteAccessConfig() {
            return sVersionOfSatelliteAccessConfig;
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
            private int mCountOfDemoModeSatelliteServiceEnablementsSuccess = 0;
            private int mCountOfDemoModeSatelliteServiceEnablementsFail = 0;
            private int mCountOfDemoModeOutgoingDatagramSuccess = 0;
            private int mCountOfDemoModeOutgoingDatagramFail = 0;
            private int mCountOfDemoModeIncomingDatagramSuccess = 0;
            private int mCountOfDemoModeIncomingDatagramFail = 0;
            private int mCountOfDatagramTypeKeepAliveSuccess = 0;
            private int mCountOfDatagramTypeKeepAliveFail = 0;
            private int mCountOfAllowedSatelliteAccess = 0;
            private int mCountOfDisallowedSatelliteAccess = 0;
            private int mCountOfSatelliteAccessCheckFail = 0;
            private Optional<Boolean> mIsProvisioned = Optional.empty();
            private Optional<Integer> mCarrierId = Optional.empty();
            private int mCountOfSatelliteAllowedStateChangedEvents = 0;
            private int mCountOfSuccessfulLocationQueries = 0;
            private int mCountOfFailedLocationQueries = 0;
            private int mCountOfP2PSmsAvailableNotificationShown = 0;
            private int mCountOfP2PSmsAvailableNotificationRemoved = 0;
            private Optional<Boolean> mIsNtnOnlyCarrier = Optional.empty();
            private Optional<Integer> mVersionOfSatelliteAccessConfig = Optional.empty();

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
             * Sets countOfDemoModeSatelliteServiceEnablementsSuccess value of
             * {@link SatelliteController} atom then returns Builder class
             */
            public Builder setCountOfDemoModeSatelliteServiceEnablementsSuccess(
                    int countOfDemoModeSatelliteServiceEnablementsSuccess) {
                this.mCountOfDemoModeSatelliteServiceEnablementsSuccess =
                        countOfDemoModeSatelliteServiceEnablementsSuccess;
                return this;
            }

            /**
             * Sets countOfDemoModeSatelliteServiceEnablementsFail value of
             * {@link SatelliteController} atom then returns Builder class
             */
            public Builder setCountOfDemoModeSatelliteServiceEnablementsFail(
                    int countOfDemoModeSatelliteServiceEnablementsFail) {
                this.mCountOfDemoModeSatelliteServiceEnablementsFail =
                        countOfDemoModeSatelliteServiceEnablementsFail;
                return this;
            }

            /**
             * Sets countOfDemoModeOutgoingDatagramSuccess value of {@link SatelliteController} atom
             * then returns Builder class
             */
            public Builder setCountOfDemoModeOutgoingDatagramSuccess(
                    int countOfDemoModeOutgoingDatagramSuccess) {
                this.mCountOfDemoModeOutgoingDatagramSuccess =
                        countOfDemoModeOutgoingDatagramSuccess;
                return this;
            }

            /**
             * Sets countOfDemoModeOutgoingDatagramFail value of {@link SatelliteController} atom
             * then returns Builder class
             */
            public Builder setCountOfDemoModeOutgoingDatagramFail(
                    int countOfDemoModeOutgoingDatagramFail) {
                this.mCountOfDemoModeOutgoingDatagramFail = countOfDemoModeOutgoingDatagramFail;
                return this;
            }

            /**
             * Sets countOfDemoModeIncomingDatagramSuccess value of {@link SatelliteController} atom
             * then returns Builder class
             */
            public Builder setCountOfDemoModeIncomingDatagramSuccess(
                    int countOfDemoModeIncomingDatagramSuccess) {
                this.mCountOfDemoModeIncomingDatagramSuccess =
                        countOfDemoModeIncomingDatagramSuccess;
                return this;
            }

            /**
             * Sets countOfDemoModeIncomingDatagramFail value of {@link SatelliteController} atom
             * then returns Builder class
             */
            public Builder setCountOfDemoModeIncomingDatagramFail(
                    int countOfDemoModeIncomingDatagramFail) {
                this.mCountOfDemoModeIncomingDatagramFail = countOfDemoModeIncomingDatagramFail;
                return this;
            }

            /**
             * Sets countOfDatagramTypeKeepAliveSuccess value of {@link SatelliteController} atom
             * then returns Builder class
             */
            public Builder setCountOfDatagramTypeKeepAliveSuccess(
                    int countOfDatagramTypeKeepAliveSuccess) {
                this.mCountOfDatagramTypeKeepAliveSuccess = countOfDatagramTypeKeepAliveSuccess;
                return this;
            }

            /**
             * Sets countOfDatagramTypeKeepAliveFail value of {@link SatelliteController} atom
             * then returns Builder class
             */
            public Builder setCountOfDatagramTypeKeepAliveFail(
                    int countOfDatagramTypeKeepAliveFail) {
                this.mCountOfDatagramTypeKeepAliveFail = countOfDatagramTypeKeepAliveFail;
                return this;
            }

            /**
             * Sets countOfAllowedSatelliteAccess value of {@link SatelliteController} atom
             * then returns Builder class
             */
            public Builder setCountOfAllowedSatelliteAccess(
                    int countOfAllowedSatelliteAccess) {
                this.mCountOfAllowedSatelliteAccess =
                        countOfAllowedSatelliteAccess;
                return this;
            }

            /**
             * Sets countOfDisallowedSatelliteAccess value of {@link SatelliteController} atom
             * then returns Builder class
             */
            public Builder setCountOfDisallowedSatelliteAccess(
                    int countOfDisallowedSatelliteAccess) {
                this.mCountOfDisallowedSatelliteAccess = countOfDisallowedSatelliteAccess;
                return this;
            }

            /**
             * Sets countOfSatelliteAccessCheckFail value of {@link SatelliteController} atom
             * then returns Builder class
             */
            public Builder setCountOfSatelliteAccessCheckFail(
                    int countOfSatelliteAccessCheckFail) {
                this.mCountOfSatelliteAccessCheckFail = countOfSatelliteAccessCheckFail;
                return this;
            }

            /**
             * Sets isProvisioned value of {@link SatelliteController} atom
             * then returns Builder class
             */
            public Builder setIsProvisioned(boolean isProvisioned) {
                this.mIsProvisioned = Optional.of(isProvisioned);
                return this;
            }

            /**
             * Sets Carrier ID value of {@link SatelliteController} atom
             * then returns Builder class
             */
            public Builder setCarrierId(int carrierId) {
                this.mCarrierId = Optional.of(carrierId);
                return this;
            }

            /**
             * Sets countOfSatelliteAllowedStateChangedEvents value of {@link SatelliteController}
             * atom
             * then returns Builder class
             */
            public Builder setCountOfSatelliteAllowedStateChangedEvents(
                    int countOfSatelliteAllowedStateChangedEvents) {
                this.mCountOfSatelliteAllowedStateChangedEvents =
                        countOfSatelliteAllowedStateChangedEvents;
                return this;
            }

            /**
             * Sets countOfSuccessfulLocationQueries value of {@link SatelliteController} atom
             * then returns Builder class
             */
            public Builder setCountOfSuccessfulLocationQueries(
                    int countOfSuccessfulLocationQueries) {
                this.mCountOfSuccessfulLocationQueries = countOfSuccessfulLocationQueries;
                return this;
            }

            /**
             * Sets countOfFailedLocationQueries value of {@link SatelliteController} atom
             * then returns Builder class
             */
            public Builder setCountOfFailedLocationQueries(int countOfFailedLocationQueries) {
                this.mCountOfFailedLocationQueries = countOfFailedLocationQueries;
                return this;
            }

            /**
             * Sets countOfP2PSmsAvailableNotificationShown value of {@link SatelliteController}
             * atom then returns Builder class
             */
            public Builder setCountOfP2PSmsAvailableNotificationShown(
                    int countOfP2PSmsAvailableNotificationShown) {
                this.mCountOfP2PSmsAvailableNotificationShown =
                        countOfP2PSmsAvailableNotificationShown;
                return this;
            }

            /**
             * Sets countOfP2PSmsAvailableNotificationRemoved value of {@link SatelliteController}
             * atom then returns Builder class
             */
            public Builder setCountOfP2PSmsAvailableNotificationRemoved(
                    int countOfP2PSmsAvailableNotificationRemoved) {
                this.mCountOfP2PSmsAvailableNotificationRemoved =
                        countOfP2PSmsAvailableNotificationRemoved;
                return this;
            }

            /**
             * Sets isNtnOnlyCarrier value of {@link SatelliteController} atom
             * then returns Builder class
             */
            public Builder setIsNtnOnlyCarrier(boolean isNtnOnlyCarrier) {
                this.mIsNtnOnlyCarrier = Optional.of(isNtnOnlyCarrier);
                return this;
            }

            /**
             * Sets versionOfSatelliteAccessConfig value of {@link SatelliteController} atom
             * then returns Builder class
             */
            public Builder setVersionOfSatelliteAccessControl(int version) {
                this.mVersionOfSatelliteAccessConfig = Optional.of(version);
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
                    + ", countOfDemoModeSatelliteServiceEnablementsSuccess="
                    + mCountOfDemoModeSatelliteServiceEnablementsSuccess
                    + ", countOfDemoModeSatelliteServiceEnablementsFail="
                    + mCountOfDemoModeSatelliteServiceEnablementsFail
                    + ", countOfDemoModeOutgoingDatagramSuccess="
                    + mCountOfDemoModeOutgoingDatagramSuccess
                    + ", countOfDemoModeOutgoingDatagramFail="
                    + mCountOfDemoModeOutgoingDatagramFail
                    + ", countOfDemoModeIncomingDatagramSuccess="
                    + mCountOfDemoModeIncomingDatagramSuccess
                    + ", countOfDemoModeIncomingDatagramFail="
                    + mCountOfDemoModeIncomingDatagramFail
                    + ", countOfDatagramTypeKeepAliveSuccess="
                    + mCountOfDatagramTypeKeepAliveSuccess
                    + ", countOfDatagramTypeKeepAliveFail="
                    + mCountOfDatagramTypeKeepAliveFail
                    + ", countOfAllowedSatelliteAccess=" + mCountOfAllowedSatelliteAccess
                    + ", countOfDisallowedSatelliteAccess=" + mCountOfDisallowedSatelliteAccess
                    + ", countOfSatelliteAccessCheckFail=" + mCountOfSatelliteAccessCheckFail
                    + ", isProvisioned=" + sIsProvisioned
                    + ", carrierId=" + sCarrierId
                    + ", countOfSatelliteAllowedStateChangedEvents="
                    + mCountOfSatelliteAllowedStateChangedEvents
                    + ", countOfSuccessfulLocationQueries=" + mCountOfSuccessfulLocationQueries
                    + ", countOfFailedLocationQueries=" + mCountOfFailedLocationQueries
                    + ", countOfP2PSmsAvailableNotificationShown="
                    + mCountOfP2PSmsAvailableNotificationShown
                    + ", countOfP2PSmsAvailableNotificationRemoved="
                    + mCountOfP2PSmsAvailableNotificationRemoved
                    + ", versionOfSatelliteAccessConfig=" + sVersionOfSatelliteAccessConfig
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
        private final int mTerminationResult;
        private final long mInitializationProcessingTimeMillis;
        private final long mTerminationProcessingTimeMillis;
        private final int mSessionDurationSec;
        private final int mCountOfOutgoingDatagramSuccess;
        private final int mCountOfOutgoingDatagramFailed;
        private final int mCountOfIncomingDatagramSuccess;
        private final int mCountOfIncomingDatagramFailed;
        private final boolean mIsDemoMode;
        private final @NtnSignalStrength.NtnSignalStrengthLevel int mMaxNtnSignalStrengthLevel;
        private final int mCarrierId;
        private final int mCountOfSatelliteNotificationDisplayed;
        private final int mCountOfAutoExitDueToScreenOff;
        private final int mCountOfAutoExitDueToTnNetwork;
        private final boolean mIsEmergency;
        private final int mMaxInactivityDurationSec;
        private final boolean mIsNtnOnlyCarrier;

        private SatelliteSessionParams(Builder builder) {
            this.mSatelliteServiceInitializationResult =
                    builder.mSatelliteServiceInitializationResult;
            this.mSatelliteTechnology = builder.mSatelliteTechnology;
            this.mTerminationResult = builder.mTerminationResult;
            this.mInitializationProcessingTimeMillis = builder.mInitializationProcessingTimeMillis;
            this.mTerminationProcessingTimeMillis =
                    builder.mTerminationProcessingTimeMillis;
            this.mSessionDurationSec = builder.mSessionDurationSec;
            this.mCountOfOutgoingDatagramSuccess = builder.mCountOfOutgoingDatagramSuccess;
            this.mCountOfOutgoingDatagramFailed = builder.mCountOfOutgoingDatagramFailed;
            this.mCountOfIncomingDatagramSuccess = builder.mCountOfIncomingDatagramSuccess;
            this.mCountOfIncomingDatagramFailed = builder.mCountOfIncomingDatagramFailed;
            this.mIsDemoMode = builder.mIsDemoMode;
            this.mMaxNtnSignalStrengthLevel = builder.mMaxNtnSignalStrengthLevel;
            this.mCarrierId = builder.mCarrierId;
            this.mCountOfSatelliteNotificationDisplayed =
                    builder.mCountOfSatelliteNotificationDisplayed;
            this.mCountOfAutoExitDueToScreenOff = builder.mCountOfAutoExitDueToScreenOff;
            this.mCountOfAutoExitDueToTnNetwork = builder.mCountOfAutoExitDueToTnNetwork;
            this.mIsEmergency = builder.mIsEmergency;
            this.mIsNtnOnlyCarrier = builder.mIsNtnOnlyCarrier;
            this.mMaxInactivityDurationSec = builder.mMaxInactivityDurationSec;
        }

        public int getSatelliteServiceInitializationResult() {
            return mSatelliteServiceInitializationResult;
        }

        public int getSatelliteTechnology() {
            return mSatelliteTechnology;
        }

        public int getTerminationResult() {
            return mTerminationResult;
        }

        public long getInitializationProcessingTime() {
            return mInitializationProcessingTimeMillis;
        }

        public long getTerminationProcessingTime() {
            return mTerminationProcessingTimeMillis;
        }

        public int getSessionDuration() {
            return mSessionDurationSec;
        }

        public int getCountOfOutgoingDatagramSuccess() {
            return mCountOfOutgoingDatagramSuccess;
        }

        public int getCountOfOutgoingDatagramFailed() {
            return mCountOfOutgoingDatagramFailed;
        }

        public int getCountOfIncomingDatagramSuccess() {
            return mCountOfIncomingDatagramSuccess;
        }

        public int getCountOfIncomingDatagramFailed() {
            return mCountOfIncomingDatagramFailed;
        }

        public boolean getIsDemoMode() {
            return mIsDemoMode;
        }

        public @NtnSignalStrength.NtnSignalStrengthLevel int getMaxNtnSignalStrengthLevel() {
            return mMaxNtnSignalStrengthLevel;
        }

        public int getCarrierId() {
            return mCarrierId;
        }

        public int getCountOfSatelliteNotificationDisplayed() {
            return mCountOfSatelliteNotificationDisplayed;
        }

        public int getCountOfAutoExitDueToScreenOff() {
            return mCountOfAutoExitDueToScreenOff;
        }

        public int getCountOfAutoExitDueToTnNetwork() {
            return mCountOfAutoExitDueToTnNetwork;
        }

        public boolean getIsEmergency() {
            return mIsEmergency;
        }

        public boolean isNtnOnlyCarrier() {
            return mIsNtnOnlyCarrier;
        }

        public int getMaxInactivityDurationSec() {
            return mMaxInactivityDurationSec;
        }

        /**
         * A builder class to create {@link SatelliteSessionParams} data structure class
         */
        public static class Builder {
            private int mSatelliteServiceInitializationResult = -1;
            private int mSatelliteTechnology = -1;
            private int mTerminationResult = -1;
            private long mInitializationProcessingTimeMillis = -1;
            private long mTerminationProcessingTimeMillis = -1;
            private int mSessionDurationSec = -1;
            private int mCountOfOutgoingDatagramSuccess = -1;
            private int mCountOfOutgoingDatagramFailed = -1;
            private int mCountOfIncomingDatagramSuccess = -1;
            private int mCountOfIncomingDatagramFailed = -1;
            private boolean mIsDemoMode = false;
            private @NtnSignalStrength.NtnSignalStrengthLevel int mMaxNtnSignalStrengthLevel =
                    NTN_SIGNAL_STRENGTH_NONE;
            private int mCarrierId = UNKNOWN_CARRIER_ID;
            private int mCountOfSatelliteNotificationDisplayed = -1;
            private int mCountOfAutoExitDueToScreenOff = -1;
            private int mCountOfAutoExitDueToTnNetwork = -1;
            private boolean mIsEmergency = false;
            private boolean mIsNtnOnlyCarrier = false;
            private int mMaxInactivityDurationSec = -1;

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

            /** Sets the satellite de-initialization result. */
            public Builder setTerminationResult(
                    @SatelliteManager.SatelliteResult int result) {
                this.mTerminationResult = result;
                return this;
            }

            /** Sets the satellite initialization processing time. */
            public Builder setInitializationProcessingTime(long processingTime) {
                this.mInitializationProcessingTimeMillis = processingTime;
                return this;
            }

            /** Sets the satellite de-initialization processing time. */
            public Builder setTerminationProcessingTime(long processingTime) {
                this.mTerminationProcessingTimeMillis = processingTime;
                return this;
            }

            /** Sets the total enabled time for the satellite session. */
            public Builder setSessionDuration(int sessionDurationSec) {
                this.mSessionDurationSec = sessionDurationSec;
                return this;
            }

            /** Sets the total number of successful outgoing datagram transmission. */
            public Builder setCountOfOutgoingDatagramSuccess(int countOfoutgoingDatagramSuccess) {
                this.mCountOfOutgoingDatagramSuccess = countOfoutgoingDatagramSuccess;
                return this;
            }

            /** Sets the total number of failed outgoing datagram transmission. */
            public Builder setCountOfOutgoingDatagramFailed(int countOfoutgoingDatagramFailed) {
                this.mCountOfOutgoingDatagramFailed = countOfoutgoingDatagramFailed;
                return this;
            }

            /** Sets the total number of successful incoming datagram transmission. */
            public Builder setCountOfIncomingDatagramSuccess(int countOfincomingDatagramSuccess) {
                this.mCountOfIncomingDatagramSuccess = countOfincomingDatagramSuccess;
                return this;
            }

            /** Sets the total number of failed incoming datagram transmission. */
            public Builder setCountOfIncomingDatagramFailed(int countOfincomingDatagramFailed) {
                this.mCountOfIncomingDatagramFailed = countOfincomingDatagramFailed;
                return this;
            }

            /** Sets whether enabled satellite session is for demo mode or not. */
            public Builder setIsDemoMode(boolean isDemoMode) {
                this.mIsDemoMode = isDemoMode;
                return this;
            }

            /** Sets the max ntn signal strength for the satellite session. */
            public Builder setMaxNtnSignalStrengthLevel(
                    @NtnSignalStrength.NtnSignalStrengthLevel int maxNtnSignalStrengthLevel) {
                this.mMaxNtnSignalStrengthLevel = maxNtnSignalStrengthLevel;
                return this;
            }

            /** Sets the currently active NB-IoT NTN carrier ID. */
            public Builder setCarrierId(int carrierId) {
                this.mCarrierId = carrierId;
                return this;
            }

            /**
             * Sets Total number of times the user is notified that the device is eligible for
             * satellite service for this session.
             */
            public Builder setCountOfSatelliteNotificationDisplayed(
                    int countOfSatelliteNotificationDisplayed) {
                this.mCountOfSatelliteNotificationDisplayed = countOfSatelliteNotificationDisplayed;
                return this;
            }

            /**
             * Sets Total number of times exit P2P message service automatically due to screen is
             * off and timer is expired.
             */
            public Builder setCountOfAutoExitDueToScreenOff(
                    int countOfAutoExitDueToScreenOff) {
                this.mCountOfAutoExitDueToScreenOff = countOfAutoExitDueToScreenOff;
                return this;
            }

            /**
             * Sets Total number of times times exit P2P message service automatically due to
             * scan TN network.
             */
            public Builder setCountOfAutoExitDueToTnNetwork(
                    int countOfAutoExitDueToTnNetwork) {
                this.mCountOfAutoExitDueToTnNetwork = countOfAutoExitDueToTnNetwork;
                return this;
            }

            /** Sets whether enabled satellite session is for emergency or not. */
            public Builder setIsEmergency(boolean isEmergency) {
                this.mIsEmergency = isEmergency;
                return this;
            }

            /**
             * Sets isNtnOnlyCarrier value of {@link SatelliteSession} atom
             * then returns Builder class
            */
            public Builder setIsNtnOnlyCarrier(boolean isNtnOnlyCarrier) {
                this.mIsNtnOnlyCarrier = isNtnOnlyCarrier;
                return this;
            }

            /** Sets the max user inactivity duration in seconds. */
            public Builder setMaxInactivityDurationSec(int maxInactivityDurationSec) {
                this.mMaxInactivityDurationSec = maxInactivityDurationSec;
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
                    + ", TerminationResult=" + mTerminationResult
                    + ", InitializationProcessingTimeMillis=" + mInitializationProcessingTimeMillis
                    + ", TerminationProcessingTimeMillis=" + mTerminationProcessingTimeMillis
                    + ", SessionDurationSec=" + mSessionDurationSec
                    + ", CountOfOutgoingDatagramSuccess=" + mCountOfOutgoingDatagramSuccess
                    + ", CountOfOutgoingDatagramFailed=" + mCountOfOutgoingDatagramFailed
                    + ", CountOfIncomingDatagramSuccess=" + mCountOfIncomingDatagramSuccess
                    + ", CountOfIncomingDatagramFailed=" + mCountOfIncomingDatagramFailed
                    + ", IsDemoMode=" + mIsDemoMode
                    + ", MaxNtnSignalStrengthLevel=" + mMaxNtnSignalStrengthLevel
                    + ", CarrierId=" + mCarrierId
                    + ", CountOfSatelliteNotificationDisplayed"
                    + mCountOfSatelliteNotificationDisplayed
                    + ", CountOfAutoExitDueToScreenOff" + mCountOfAutoExitDueToScreenOff
                    + ", CountOfAutoExitDueToTnNetwork" + mCountOfAutoExitDueToTnNetwork
                    + ", IsEmergency=" + mIsEmergency
                    + ", IsNtnOnlyCarrier=" + mIsNtnOnlyCarrier
                    + ", MaxInactivityDurationSec=" + mMaxInactivityDurationSec
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
        private final boolean mIsDemoMode;
        private final int mCarrierId;
        private final boolean mIsNtnOnlyCarrier;

        private SatelliteIncomingDatagramParams(Builder builder) {
            this.mResultCode = builder.mResultCode;
            this.mDatagramSizeBytes = builder.mDatagramSizeBytes;
            this.mDatagramTransferTimeMillis = builder.mDatagramTransferTimeMillis;
            this.mIsDemoMode = builder.mIsDemoMode;
            this.mCarrierId = builder.mCarrierId;
            this.mIsNtnOnlyCarrier = builder.mIsNtnOnlyCarrier;
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

        public boolean getIsDemoMode() {
            return mIsDemoMode;
        }

        public int getCarrierId() {
            return mCarrierId;
        }

        public boolean isNtnOnlyCarrier() {
            return mIsNtnOnlyCarrier;
        }

        /**
         * A builder class to create {@link SatelliteIncomingDatagramParams} data structure class
         */
        public static class Builder {
            private int mResultCode = -1;
            private int mDatagramSizeBytes = -1;
            private long mDatagramTransferTimeMillis = -1;
            private boolean mIsDemoMode = false;
            private int mCarrierId = UNKNOWN_CARRIER_ID;
            private boolean mIsNtnOnlyCarrier = false;

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
             * Sets whether transferred datagram is in demo mode or not
             * then returns Builder class
             */
            public Builder setIsDemoMode(boolean isDemoMode) {
                this.mIsDemoMode = isDemoMode;
                return this;
            }

            /** Sets the currently active NB-IoT NTN carrier ID. */
            public Builder setCarrierId(int carrierId) {
                this.mCarrierId = carrierId;
                return this;
            }

            /**
             * Sets isNtnOnlyCarrier value of {@link SatelliteIncomingDatagram} atom
             * then returns Builder class
            */
            public Builder setIsNtnOnlyCarrier(boolean isNtnOnlyCarrier) {
                this.mIsNtnOnlyCarrier = isNtnOnlyCarrier;
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
                    + ", datagramTransferTimeMillis=" + mDatagramTransferTimeMillis
                    + ", isDemoMode=" + mIsDemoMode
                    + ", CarrierId=" + mCarrierId
                    + ", isNtnOnlyCarrier=" + mIsNtnOnlyCarrier
                    + ")";
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
        private final boolean mIsDemoMode;
        private final int mCarrierId;
        private final boolean mIsNtnOnlyCarrier;

        private SatelliteOutgoingDatagramParams(Builder builder) {
            this.mDatagramType = builder.mDatagramType;
            this.mResultCode = builder.mResultCode;
            this.mDatagramSizeBytes = builder.mDatagramSizeBytes;
            this.mDatagramTransferTimeMillis = builder.mDatagramTransferTimeMillis;
            this.mIsDemoMode = builder.mIsDemoMode;
            this.mCarrierId = builder.mCarrierId;
            this.mIsNtnOnlyCarrier = builder.mIsNtnOnlyCarrier;
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

        public boolean getIsDemoMode() {
            return mIsDemoMode;
        }

        public int getCarrierId() {
            return mCarrierId;
        }

        public boolean isNtnOnlyCarrier() {
            return mIsNtnOnlyCarrier;
        }

        /**
         * A builder class to create {@link SatelliteOutgoingDatagramParams} data structure class
         */
        public static class Builder {
            private int mDatagramType = -1;
            private int mResultCode = -1;
            private int mDatagramSizeBytes = -1;
            private long mDatagramTransferTimeMillis = -1;
            private boolean mIsDemoMode = false;
            private int mCarrierId = UNKNOWN_CARRIER_ID;
            private boolean mIsNtnOnlyCarrier = false;

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
             * Sets whether transferred datagram is in demo mode or not
             * then returns Builder class
             */
            public Builder setIsDemoMode(boolean isDemoMode) {
                this.mIsDemoMode = isDemoMode;
                return this;
            }

            /** Sets the currently active NB-IoT NTN carrier ID. */
            public Builder setCarrierId(int carrierId) {
                this.mCarrierId = carrierId;
                return this;
            }

            /**
             * Sets isNtnOnlyCarrier value of {@link SatelliteOutgoingDatagram} atom
             * then returns Builder class
            */
            public Builder setIsNtnOnlyCarrier(boolean isNtnOnlyCarrier) {
                this.mIsNtnOnlyCarrier = isNtnOnlyCarrier;
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
                    + ", datagramTransferTimeMillis=" + mDatagramTransferTimeMillis
                    + ", isDemoMode=" + mIsDemoMode
                    + ", CarrierId=" + mCarrierId
                    + ", isNtnOnlyCarrier=" + mIsNtnOnlyCarrier
                    + ")";
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
        private final int mCarrierId;
        private final boolean mIsNtnOnlyCarrier;

        private SatelliteProvisionParams(Builder builder) {
            this.mResultCode = builder.mResultCode;
            this.mProvisioningTimeSec = builder.mProvisioningTimeSec;
            this.mIsProvisionRequest = builder.mIsProvisionRequest;
            this.mIsCanceled = builder.mIsCanceled;
            this.mCarrierId = builder.mCarrierId;
            this.mIsNtnOnlyCarrier = builder.mIsNtnOnlyCarrier;
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

        public int getCarrierId() {
            return mCarrierId;
        }

        public boolean isNtnOnlyCarrier() {
            return mIsNtnOnlyCarrier;
        }

        /**
         * A builder class to create {@link SatelliteProvisionParams} data structure class
         */
        public static class Builder {
            private int mResultCode = -1;
            private int mProvisioningTimeSec = -1;
            private boolean mIsProvisionRequest = false;
            private boolean mIsCanceled = false;
            private int mCarrierId = UNKNOWN_CARRIER_ID;
            private boolean mIsNtnOnlyCarrier = false;

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

            /** Sets the currently active NB-IoT NTN carrier ID. */
            public Builder setCarrierId(int carrierId) {
                this.mCarrierId = carrierId;
                return this;
            }

            /**
             * Sets isNtnOnlyCarrier value of {@link SatelliteProvision} atom
             * then returns Builder class
            */
            public Builder setIsNtnOnlyCarrier(boolean isNtnOnlyCarrier) {
                this.mIsNtnOnlyCarrier = isNtnOnlyCarrier;
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
                    + ", isCanceled" + mIsCanceled
                    + ", CarrierId=" + mCarrierId
                    + ", isNtnOnlyCarrier=" + mIsNtnOnlyCarrier
                    + ")";
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
        private final boolean mIsMultiSim;
        private final int mRecommendingHandoverType;
        private final boolean mIsSatelliteAllowedInCurrentLocation;
        private final boolean mIsWifiConnected;
        private final int mCarrierId;
        private final boolean mIsNtnOnlyCarrier;

        private SatelliteSosMessageRecommenderParams(Builder builder) {
            this.mIsDisplaySosMessageSent = builder.mIsDisplaySosMessageSent;
            this.mCountOfTimerStarted = builder.mCountOfTimerStarted;
            this.mIsImsRegistered = builder.mIsImsRegistered;
            this.mCellularServiceState = builder.mCellularServiceState;
            this.mIsMultiSim = builder.mIsMultiSim;
            this.mRecommendingHandoverType = builder.mRecommendingHandoverType;
            this.mIsSatelliteAllowedInCurrentLocation =
                    builder.mIsSatelliteAllowedInCurrentLocation;
            this.mIsWifiConnected = builder.mIsWifiConnected;
            this.mCarrierId = builder.mCarrierId;
            this.mIsNtnOnlyCarrier = builder.mIsNtnOnlyCarrier;
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

        public boolean isMultiSim() {
            return mIsMultiSim;
        }

        public int getRecommendingHandoverType() {
            return mRecommendingHandoverType;
        }

        public boolean isSatelliteAllowedInCurrentLocation() {
            return mIsSatelliteAllowedInCurrentLocation;
        }

        public boolean isWifiConnected() {
            return mIsWifiConnected;
        }

        public int getCarrierId() {
            return mCarrierId;
        }

        public boolean isNtnOnlyCarrier() {
            return mIsNtnOnlyCarrier;
        }

        /**
         * A builder class to create {@link SatelliteSosMessageRecommender} data structure class
         */
        public static class Builder {
            private boolean mIsDisplaySosMessageSent = false;
            private int mCountOfTimerStarted = -1;
            private boolean mIsImsRegistered = false;
            private int mCellularServiceState = -1;
            private boolean mIsMultiSim = false;
            private int mRecommendingHandoverType = -1;
            private boolean mIsSatelliteAllowedInCurrentLocation = false;
            private boolean mIsWifiConnected = false;
            private int mCarrierId = UNKNOWN_CARRIER_ID;
            private boolean mIsNtnOnlyCarrier = false;

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
             * Sets isMultiSim value of {@link SatelliteSosMessageRecommender} atom
             * then returns Builder class
             */
            public Builder setIsMultiSim(boolean isMultiSim) {
                this.mIsMultiSim = isMultiSim;
                return this;
            }

            /**
             * Sets recommendingHandoverType value of {@link SatelliteSosMessageRecommender} atom
             * then returns Builder class
             */
            public Builder setRecommendingHandoverType(int recommendingHandoverType) {
                this.mRecommendingHandoverType = recommendingHandoverType;
                return this;
            }

            /**
             * Sets isSatelliteAllowedInCurrentLocation value of
             * {@link SatelliteSosMessageRecommender} atom then returns Builder class.
             */
            public Builder setIsSatelliteAllowedInCurrentLocation(
                    boolean satelliteAllowedInCurrentLocation) {
                mIsSatelliteAllowedInCurrentLocation = satelliteAllowedInCurrentLocation;
                return this;
            }

            /**
             * Sets whether Wi-Fi is connected value of {@link SatelliteSosMessageRecommender} atom
             * then returns Builder class
             */
            public Builder setIsWifiConnected(boolean isWifiConnected) {
                this.mIsWifiConnected = isWifiConnected;
                return this;
            }

            /**
             * Sets carrier ID value of {@link SatelliteSosMessageRecommender} atom then returns
             * Builder class.
             */
            public Builder setCarrierId(int carrierId) {
                this.mCarrierId = carrierId;
                return this;
            }

            /**
             * Sets isNtnOnlyCarrier value of {@link SatelliteSosMessageRecommender} atom
             * then returns Builder class
            */
            public Builder setIsNtnOnlyCarrier(boolean isNtnOnlyCarrier) {
                this.mIsNtnOnlyCarrier = isNtnOnlyCarrier;
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
                    + ", cellularServiceState=" + mCellularServiceState
                    + ", isMultiSim=" + mIsMultiSim
                    + ", recommendingHandoverType=" + mRecommendingHandoverType
                    + ", isSatelliteAllowedInCurrentLocation="
                    + mIsSatelliteAllowedInCurrentLocation
                    + ", isWifiConnected=" + mIsWifiConnected
                    + ", carrierId=" + mCarrierId
                    + ", isNtnOnlyCarrier=" + mIsNtnOnlyCarrier
                    + ")";
        }
    }

    /**
     * A data class to contain whole component of {@link CarrierRoamingSatelliteSession} atom.
     * Refer to {@link #onCarrierRoamingSatelliteSessionMetrics(
     * CarrierRoamingSatelliteSessionParams)}.
     */
    public class CarrierRoamingSatelliteSessionParams {
        private final int mCarrierId;
        private final boolean mIsNtnRoamingInHomeCountry;
        private final int mTotalSatelliteModeTimeSec;
        private final int mNumberOfSatelliteConnections;
        private final int mAvgDurationOfSatelliteConnectionSec;
        private final int mSatelliteConnectionGapMinSec;
        private final int mSatelliteConnectionGapAvgSec;
        private final int mSatelliteConnectionGapMaxSec;
        private final int mRsrpAvg;
        private final int mRsrpMedian;
        private final int mRssnrAvg;
        private final int mRssnrMedian;
        private final int mCountOfIncomingSms;
        private final int mCountOfOutgoingSms;
        private final int mCountOfIncomingMms;
        private final int mCountOfOutgoingMms;

        private CarrierRoamingSatelliteSessionParams(Builder builder) {
            this.mCarrierId = builder.mCarrierId;
            this.mIsNtnRoamingInHomeCountry = builder.mIsNtnRoamingInHomeCountry;
            this.mTotalSatelliteModeTimeSec = builder.mTotalSatelliteModeTimeSec;
            this.mNumberOfSatelliteConnections = builder.mNumberOfSatelliteConnections;
            this.mAvgDurationOfSatelliteConnectionSec =
                    builder.mAvgDurationOfSatelliteConnectionSec;
            this.mSatelliteConnectionGapMinSec = builder.mSatelliteConnectionGapMinSec;
            this.mSatelliteConnectionGapAvgSec = builder.mSatelliteConnectionGapAvgSec;
            this.mSatelliteConnectionGapMaxSec = builder.mSatelliteConnectionGapMaxSec;
            this.mRsrpAvg = builder.mRsrpAvg;
            this.mRsrpMedian = builder.mRsrpMedian;
            this.mRssnrAvg = builder.mRssnrAvg;
            this.mRssnrMedian = builder.mRssnrMedian;
            this.mCountOfIncomingSms = builder.mCountOfIncomingSms;
            this.mCountOfOutgoingSms = builder.mCountOfOutgoingSms;
            this.mCountOfIncomingMms = builder.mCountOfIncomingMms;
            this.mCountOfOutgoingMms = builder.mCountOfOutgoingMms;
        }

        public int getCarrierId() {
            return mCarrierId;
        }

        public boolean getIsNtnRoamingInHomeCountry() {
            return mIsNtnRoamingInHomeCountry;
        }

        public int getTotalSatelliteModeTimeSec() {
            return mTotalSatelliteModeTimeSec;
        }

        public int getNumberOfSatelliteConnections() {
            return mNumberOfSatelliteConnections;
        }

        public int getAvgDurationOfSatelliteConnectionSec() {
            return mAvgDurationOfSatelliteConnectionSec;
        }

        public int getSatelliteConnectionGapMinSec() {
            return mSatelliteConnectionGapMinSec;
        }

        public int getSatelliteConnectionGapAvgSec() {
            return mSatelliteConnectionGapAvgSec;
        }

        public int getSatelliteConnectionGapMaxSec() {
            return mSatelliteConnectionGapMaxSec;
        }

        public int getRsrpAvg() {
            return mRsrpAvg;
        }

        public int getRsrpMedian() {
            return mRsrpMedian;
        }

        public int getRssnrAvg() {
            return mRssnrAvg;
        }

        public int getRssnrMedian() {
            return mRssnrMedian;
        }

        public int getCountOfIncomingSms() {
            return mCountOfIncomingSms;
        }

        public int getCountOfOutgoingSms() {
            return mCountOfOutgoingSms;
        }

        public int getCountOfIncomingMms() {
            return mCountOfIncomingMms;
        }

        public int getCountOfOutgoingMms() {
            return mCountOfOutgoingMms;
        }

        /**
         * A builder class to create {@link CarrierRoamingSatelliteSessionParams} data structure
         * class
         */
        public static class Builder {
            private int mCarrierId = -1;
            private boolean mIsNtnRoamingInHomeCountry = false;
            private int mTotalSatelliteModeTimeSec = 0;
            private int mNumberOfSatelliteConnections = 0;
            private int mAvgDurationOfSatelliteConnectionSec = 0;
            private int mSatelliteConnectionGapMinSec = 0;
            private int mSatelliteConnectionGapAvgSec = 0;
            private int mSatelliteConnectionGapMaxSec = 0;
            private int mRsrpAvg = 0;
            private int mRsrpMedian = 0;
            private int mRssnrAvg = 0;
            private int mRssnrMedian = 0;
            private int mCountOfIncomingSms = 0;
            private int mCountOfOutgoingSms = 0;
            private int mCountOfIncomingMms = 0;
            private int mCountOfOutgoingMms = 0;

            /**
             * Sets carrierId value of {@link CarrierRoamingSatelliteSession} atom
             * then returns Builder class
             */
            public Builder setCarrierId(int carrierId) {
                this.mCarrierId = carrierId;
                return this;
            }

            /**
             * Sets isNtnRoamingInHomeCountry value of {@link CarrierRoamingSatelliteSession} atom
             * then returns Builder class
             */
            public Builder setIsNtnRoamingInHomeCountry(boolean isNtnRoamingInHomeCountry) {
                this.mIsNtnRoamingInHomeCountry = isNtnRoamingInHomeCountry;
                return this;
            }

            /**
             * Sets totalSatelliteModeTimeSec value of {@link CarrierRoamingSatelliteSession} atom
             * then returns Builder class
             */
            public Builder setTotalSatelliteModeTimeSec(int totalSatelliteModeTimeSec) {
                this.mTotalSatelliteModeTimeSec = totalSatelliteModeTimeSec;
                return this;
            }


            /**
             * Sets numberOfSatelliteConnections value of {@link CarrierRoamingSatelliteSession}
             * atom then returns Builder class
             */
            public Builder setNumberOfSatelliteConnections(int numberOfSatelliteConnections) {
                this.mNumberOfSatelliteConnections = numberOfSatelliteConnections;
                return this;
            }

            /**
             * Sets avgDurationOfSatelliteConnectionSec value of
             * {@link CarrierRoamingSatelliteSession} atom then returns Builder class
             */
            public Builder setAvgDurationOfSatelliteConnectionSec(
                    int avgDurationOfSatelliteConnectionSec) {
                this.mAvgDurationOfSatelliteConnectionSec = avgDurationOfSatelliteConnectionSec;
                return this;
            }

            /**
             * Sets satelliteConnectionGapMinSec value of {@link CarrierRoamingSatelliteSession}
             * atom then returns Builder class
             */
            public Builder setSatelliteConnectionGapMinSec(int satelliteConnectionGapMinSec) {
                this.mSatelliteConnectionGapMinSec = satelliteConnectionGapMinSec;
                return this;
            }

            /**
             * Sets satelliteConnectionGapAvgSec value of {@link CarrierRoamingSatelliteSession}
             * atom then returns Builder class
             */
            public Builder setSatelliteConnectionGapAvgSec(int satelliteConnectionGapAvgSec) {
                this.mSatelliteConnectionGapAvgSec = satelliteConnectionGapAvgSec;
                return this;
            }

            /**
             * Sets satelliteConnectionGapMaxSec value of {@link CarrierRoamingSatelliteSession}
             * atom then returns Builder class
             */
            public Builder setSatelliteConnectionGapMaxSec(int satelliteConnectionGapMaxSec) {
                this.mSatelliteConnectionGapMaxSec = satelliteConnectionGapMaxSec;
                return this;
            }

            /**
             * Sets rsrpAvg value of {@link CarrierRoamingSatelliteSession}
             * atom then returns Builder class
             */
            public Builder setRsrpAvg(int rsrpAvg) {
                this.mRsrpAvg = rsrpAvg;
                return this;
            }

            /**
             * Sets rsrpMedian value of {@link CarrierRoamingSatelliteSession}
             * atom then returns Builder class
             */
            public Builder setRsrpMedian(int rsrpMedian) {
                this.mRsrpMedian = rsrpMedian;
                return this;
            }

            /**
             * Sets rssnrAvg value of {@link CarrierRoamingSatelliteSession}
             * atom then returns Builder class
             */
            public Builder setRssnrAvg(int rssnrAvg) {
                this.mRssnrAvg = rssnrAvg;
                return this;
            }

            /**
             * Sets rssnrMedian value of {@link CarrierRoamingSatelliteSession}
             * atom then returns Builder class
             */
            public Builder setRssnrMedian(int rssnrMedian) {
                this.mRssnrMedian = rssnrMedian;
                return this;
            }


            /**
             * Sets countOfIncomingSms value of {@link CarrierRoamingSatelliteSession}
             * atom then returns Builder class
             */
            public Builder setCountOfIncomingSms(int countOfIncomingSms) {
                this.mCountOfIncomingSms = countOfIncomingSms;
                return this;
            }

            /**
             * Sets countOfOutgoingSms value of {@link CarrierRoamingSatelliteSession}
             * atom then returns Builder class
             */
            public Builder setCountOfOutgoingSms(int countOfOutgoingSms) {
                this.mCountOfOutgoingSms = countOfOutgoingSms;
                return this;
            }

            /**
             * Sets countOfIncomingMms value of {@link CarrierRoamingSatelliteSession}
             * atom then returns Builder class
             */
            public Builder setCountOfIncomingMms(int countOfIncomingMms) {
                this.mCountOfIncomingMms = countOfIncomingMms;
                return this;
            }

            /**
             * Sets countOfOutgoingMms value of {@link CarrierRoamingSatelliteSession}
             * atom then returns Builder class
             */
            public Builder setCountOfOutgoingMms(int countOfOutgoingMms) {
                this.mCountOfOutgoingMms = countOfOutgoingMms;
                return this;
            }

            /**
             * Returns CarrierRoamingSatelliteSessionParams, which contains whole component of
             * {@link CarrierRoamingSatelliteSession} atom
             */
            public CarrierRoamingSatelliteSessionParams build() {
                return new SatelliteStats()
                        .new CarrierRoamingSatelliteSessionParams(Builder.this);
            }
        }

        @Override
        public String toString() {
            return "CarrierRoamingSatelliteSessionParams("
                    + "carrierId=" + mCarrierId
                    + ", isNtnRoamingInHomeCountry=" + mIsNtnRoamingInHomeCountry
                    + ", totalSatelliteModeTimeSec=" + mTotalSatelliteModeTimeSec
                    + ", numberOfSatelliteConnections=" + mNumberOfSatelliteConnections
                    + ", avgDurationOfSatelliteConnectionSec="
                    + mAvgDurationOfSatelliteConnectionSec
                    + ", satelliteConnectionGapMinSec=" + mSatelliteConnectionGapMinSec
                    + ", satelliteConnectionGapAvgSec=" + mSatelliteConnectionGapAvgSec
                    + ", satelliteConnectionGapMaxSec=" + mSatelliteConnectionGapMaxSec
                    + ", rsrpAvg=" + mRsrpAvg
                    + ", rsrpMedian=" + mRsrpMedian
                    + ", rssnrAvg=" + mRssnrAvg
                    + ", rssnrMedian=" + mRssnrMedian
                    + ", countOfIncomingSms=" + mCountOfIncomingSms
                    + ", countOfOutgoingSms=" + mCountOfOutgoingSms
                    + ", countOfIncomingMms=" + mCountOfIncomingMms
                    + ", countOfOutgoingMms=" + mCountOfOutgoingMms
                    + ")";
        }
    }

    /**
     * A data class to contain whole component of {@link CarrierRoamingSatelliteControllerStats}
     * atom. Refer to {@link #onCarrierRoamingSatelliteControllerStatsMetrics(
     * CarrierRoamingSatelliteControllerStatsParams)}.
     */
    public class CarrierRoamingSatelliteControllerStatsParams {
        private final int mConfigDataSource;
        private final int mCountOfEntitlementStatusQueryRequest;
        private final int mCountOfSatelliteConfigUpdateRequest;
        private final int mCountOfSatelliteNotificationDisplayed;
        private final int mSatelliteSessionGapMinSec;
        private final int mSatelliteSessionGapAvgSec;
        private final int mSatelliteSessionGapMaxSec;
        private static int sCarrierId;
        private static boolean sIsDeviceEntitled;

        private CarrierRoamingSatelliteControllerStatsParams(Builder builder) {
            this.mConfigDataSource = builder.mConfigDataSource;
            this.mCountOfEntitlementStatusQueryRequest =
                    builder.mCountOfEntitlementStatusQueryRequest;
            this.mCountOfSatelliteConfigUpdateRequest =
                    builder.mCountOfSatelliteConfigUpdateRequest;
            this.mCountOfSatelliteNotificationDisplayed =
                    builder.mCountOfSatelliteNotificationDisplayed;
            this.mSatelliteSessionGapMinSec = builder.mSatelliteSessionGapMinSec;
            this.mSatelliteSessionGapAvgSec = builder.mSatelliteSessionGapAvgSec;
            this.mSatelliteSessionGapMaxSec = builder.mSatelliteSessionGapMaxSec;

            // Carrier ID value should be updated only when it is meaningful.
            if (builder.mCarrierId.isPresent()) {
                this.sCarrierId = builder.mCarrierId.get();
            }

            // isDeviceEntitled value should be updated only when it is meaningful.
            if (builder.mIsDeviceEntitled.isPresent()) {
                this.sIsDeviceEntitled = builder.mIsDeviceEntitled.get();
            }
        }

        public int getConfigDataSource() {
            return mConfigDataSource;
        }


        public int getCountOfEntitlementStatusQueryRequest() {
            return mCountOfEntitlementStatusQueryRequest;
        }

        public int getCountOfSatelliteConfigUpdateRequest() {
            return mCountOfSatelliteConfigUpdateRequest;
        }

        public int getCountOfSatelliteNotificationDisplayed() {
            return mCountOfSatelliteNotificationDisplayed;
        }

        public int getSatelliteSessionGapMinSec() {
            return mSatelliteSessionGapMinSec;
        }

        public int getSatelliteSessionGapAvgSec() {
            return mSatelliteSessionGapAvgSec;
        }

        public int getSatelliteSessionGapMaxSec() {
            return mSatelliteSessionGapMaxSec;
        }

        public int getCarrierId() {
            return sCarrierId;
        }

        public boolean isDeviceEntitled() {
            return sIsDeviceEntitled;
        }

        /**
         * A builder class to create {@link CarrierRoamingSatelliteControllerStatsParams}
         * data structure class
         */
        public static class Builder {
            private int mConfigDataSource = SatelliteConstants.CONFIG_DATA_SOURCE_UNKNOWN;
            private int mCountOfEntitlementStatusQueryRequest = 0;
            private int mCountOfSatelliteConfigUpdateRequest = 0;
            private int mCountOfSatelliteNotificationDisplayed = 0;
            private int mSatelliteSessionGapMinSec = 0;
            private int mSatelliteSessionGapAvgSec = 0;
            private int mSatelliteSessionGapMaxSec = 0;
            private Optional<Integer> mCarrierId = Optional.empty();
            private Optional<Boolean> mIsDeviceEntitled = Optional.empty();

            /**
             * Sets configDataSource value of {@link CarrierRoamingSatelliteControllerStats} atom
             * then returns Builder class
             */
            public Builder setConfigDataSource(int configDataSource) {
                this.mConfigDataSource = configDataSource;
                return this;
            }

            /**
             * Sets countOfEntitlementStatusQueryRequest value of
             * {@link CarrierRoamingSatelliteControllerStats} atom then returns Builder class
             */
            public Builder setCountOfEntitlementStatusQueryRequest(
                    int countOfEntitlementStatusQueryRequest) {
                this.mCountOfEntitlementStatusQueryRequest = countOfEntitlementStatusQueryRequest;
                return this;
            }

            /**
             * Sets countOfSatelliteConfigUpdateRequest value of
             * {@link CarrierRoamingSatelliteControllerStats} atom then returns Builder class
             */
            public Builder setCountOfSatelliteConfigUpdateRequest(
                    int countOfSatelliteConfigUpdateRequest) {
                this.mCountOfSatelliteConfigUpdateRequest = countOfSatelliteConfigUpdateRequest;
                return this;
            }

            /**
             * Sets countOfSatelliteNotificationDisplayed value of
             * {@link CarrierRoamingSatelliteControllerStats} atom then returns Builder class
             */
            public Builder setCountOfSatelliteNotificationDisplayed(
                    int countOfSatelliteNotificationDisplayed) {
                this.mCountOfSatelliteNotificationDisplayed = countOfSatelliteNotificationDisplayed;
                return this;
            }

            /**
             * Sets satelliteSessionGapMinSec value of
             * {@link CarrierRoamingSatelliteControllerStats} atom then returns Builder class
             */
            public Builder setSatelliteSessionGapMinSec(int satelliteSessionGapMinSec) {
                this.mSatelliteSessionGapMinSec = satelliteSessionGapMinSec;
                return this;
            }

            /**
             * Sets satelliteSessionGapAvgSec value of
             * {@link CarrierRoamingSatelliteControllerStats} atom then returns Builder class
             */
            public Builder setSatelliteSessionGapAvgSec(int satelliteSessionGapAvgSec) {
                this.mSatelliteSessionGapAvgSec = satelliteSessionGapAvgSec;
                return this;
            }

            /**
             * Sets satelliteSessionGapMaxSec value of
             * {@link CarrierRoamingSatelliteControllerStats} atom then returns Builder class
             */
            public Builder setSatelliteSessionGapMaxSec(int satelliteSessionGapMaxSec) {
                this.mSatelliteSessionGapMaxSec = satelliteSessionGapMaxSec;
                return this;
            }

            /** Sets the currently active NB-IoT NTN carrier ID. */
            public Builder setCarrierId(int carrierId) {
                this.mCarrierId = Optional.of(carrierId);
                return this;
            }

            /** Sets whether the device is currently entitled or not. */
            public Builder setIsDeviceEntitled(boolean isDeviceEntitled) {
                this.mIsDeviceEntitled = Optional.of(isDeviceEntitled);
                return this;
            }

            /**
             * Returns CarrierRoamingSatelliteControllerStatsParams, which contains whole component
             * of {@link CarrierRoamingSatelliteControllerStats} atom
             */
            public CarrierRoamingSatelliteControllerStatsParams build() {
                return new SatelliteStats()
                        .new CarrierRoamingSatelliteControllerStatsParams(Builder.this);
            }
        }

        @Override
        public String toString() {
            return "CarrierRoamingSatelliteControllerStatsParams("
                    + "configDataSource=" + mConfigDataSource
                    + ", countOfEntitlementStatusQueryRequest="
                    + mCountOfEntitlementStatusQueryRequest
                    + ", countOfSatelliteConfigUpdateRequest="
                    + mCountOfSatelliteConfigUpdateRequest
                    + ", countOfSatelliteNotificationDisplayed="
                    + mCountOfSatelliteNotificationDisplayed
                    + ", satelliteSessionGapMinSec=" + mSatelliteSessionGapMinSec
                    + ", satelliteSessionGapAvgSec=" + mSatelliteSessionGapAvgSec
                    + ", satelliteSessionGapMaxSec=" + mSatelliteSessionGapMaxSec
                    + ", carrierId=" + sCarrierId
                    + ", isDeviceEntitled=" + sIsDeviceEntitled
                    + ")";
        }
    }

    /**
     * A data class to contain whole component of {@link SatelliteEntitlement} atom.
     * Refer to {@link #onSatelliteEntitlementMetrics(SatelliteEntitlementParams)}.
     */
    public class SatelliteEntitlementParams {
        private final int mCarrierId;
        private final int mResult;
        private final int mEntitlementStatus;
        private final boolean mIsRetry;
        private final int mCount;

        private SatelliteEntitlementParams(Builder builder) {
            this.mCarrierId = builder.mCarrierId;
            this.mResult = builder.mResult;
            this.mEntitlementStatus = builder.mEntitlementStatus;
            this.mIsRetry = builder.mIsRetry;
            this.mCount = builder.mCount;
        }

        public int getCarrierId() {
            return mCarrierId;
        }

        public int getResult() {
            return mResult;
        }

        public int getEntitlementStatus() {
            return mEntitlementStatus;
        }

        public boolean getIsRetry() {
            return mIsRetry;
        }

        public int getCount() {
            return mCount;
        }

        /**
         * A builder class to create {@link SatelliteEntitlementParams} data structure class
         */
        public static class Builder {
            private int mCarrierId = -1;
            private int mResult = -1;
            private int mEntitlementStatus = -1;
            private boolean mIsRetry = false;
            private int mCount = -1;

            /**
             * Sets carrierId value of {@link SatelliteEntitlement} atom
             * then returns Builder class
             */
            public Builder setCarrierId(int carrierId) {
                this.mCarrierId = carrierId;
                return this;
            }

            /**
             * Sets result value of {@link SatelliteEntitlement} atom
             * then returns Builder class
             */
            public Builder setResult(int result) {
                this.mResult = result;
                return this;
            }

            /**
             * Sets entitlementStatus value of {@link SatelliteEntitlement} atom
             * then returns Builder class
             */
            public Builder setEntitlementStatus(int entitlementStatus) {
                this.mEntitlementStatus = entitlementStatus;
                return this;
            }

            /**
             * Sets isRetry value of {@link SatelliteEntitlement} atom
             * then returns Builder class
             */
            public Builder setIsRetry(boolean isRetry) {
                this.mIsRetry = isRetry;
                return this;
            }

            /**
             * Sets count value of {@link SatelliteEntitlement} atom
             * then returns Builder class
             */
            public Builder setCount(int count) {
                this.mCount = count;
                return this;
            }

            /**
             * Returns SatelliteEntitlementParams, which contains whole component of
             * {@link SatelliteEntitlement} atom
             */
            public SatelliteEntitlementParams build() {
                return new SatelliteStats()
                        .new SatelliteEntitlementParams(Builder.this);
            }
        }

        @Override
        public String toString() {
            return "SatelliteEntitlementParams("
                    + "carrierId=" + mCarrierId
                    + ", result=" + mResult
                    + ", entitlementStatus=" + mEntitlementStatus
                    + ", isRetry=" + mIsRetry
                    + ", count=" + mCount + ")";
        }
    }

    /**
     * A data class to contain whole component of {@link SatelliteConfigUpdater} atom.
     * Refer to {@link #onSatelliteConfigUpdaterMetrics(SatelliteConfigUpdaterParams)}.
     */
    public class SatelliteConfigUpdaterParams {
        private final int mConfigVersion;
        private final int mOemConfigResult;
        private final int mCarrierConfigResult;
        private final int mCount;

        private SatelliteConfigUpdaterParams(Builder builder) {
            this.mConfigVersion = builder.mConfigVersion;
            this.mOemConfigResult = builder.mOemConfigResult;
            this.mCarrierConfigResult = builder.mCarrierConfigResult;
            this.mCount = builder.mCount;
        }

        public int getConfigVersion() {
            return mConfigVersion;
        }

        public int getOemConfigResult() {
            return mOemConfigResult;
        }

        public int getCarrierConfigResult() {
            return mCarrierConfigResult;
        }

        public int getCount() {
            return mCount;
        }

        /**
         * A builder class to create {@link SatelliteConfigUpdaterParams} data structure class
         */
        public static class Builder {
            private int mConfigVersion = -1;
            private int mOemConfigResult = -1;
            private int mCarrierConfigResult = -1;
            private int mCount = -1;

            /**
             * Sets configVersion value of {@link SatelliteConfigUpdater} atom
             * then returns Builder class
             */
            public Builder setConfigVersion(int configVersion) {
                this.mConfigVersion = configVersion;
                return this;
            }

            /**
             * Sets oemConfigResult value of {@link SatelliteConfigUpdater} atom
             * then returns Builder class
             */
            public Builder setOemConfigResult(int oemConfigResult) {
                this.mOemConfigResult = oemConfigResult;
                return this;
            }

            /**
             * Sets carrierConfigResult value of {@link SatelliteConfigUpdater} atom
             * then returns Builder class
             */
            public Builder setCarrierConfigResult(int carrierConfigResult) {
                this.mCarrierConfigResult = carrierConfigResult;
                return this;
            }

            /**
             * Sets count value of {@link SatelliteConfigUpdater} atom
             * then returns Builder class
             */
            public Builder setCount(int count) {
                this.mCount = count;
                return this;
            }

            /**
             * Returns SatelliteConfigUpdaterParams, which contains whole component of
             * {@link SatelliteConfigUpdater} atom
             */
            public SatelliteConfigUpdaterParams build() {
                return new SatelliteStats()
                        .new SatelliteConfigUpdaterParams(Builder.this);
            }
        }

        @Override
        public String toString() {
            return "SatelliteConfigUpdaterParams("
                    + "configVersion=" + mConfigVersion
                    + ", oemConfigResult=" + mOemConfigResult
                    + ", carrierConfigResult=" + mCarrierConfigResult
                    + ", count=" + mCount + ")";
        }
    }

    /**
     * A data class to contain whole component of {@link SatelliteAccessControllerParams} atom.
     * Refer to {@link #onSatelliteAccessControllerMetrics(SatelliteAccessControllerParams)}.
     */
    public class SatelliteAccessControllerParams {
        private final @SatelliteConstants.AccessControlType int mAccessControlType;
        private final long mLocationQueryTimeMillis;
        private final long mOnDeviceLookupTimeMillis;
        private final long mTotalCheckingTimeMillis;
        private final boolean mIsAllowed;
        private final boolean mIsEmergency;
        private final @SatelliteManager.SatelliteResult int mResultCode;
        private final String[] mCountryCodes;
        private final @SatelliteConstants.ConfigDataSource int mConfigDataSource;
        private final int mCarrierId;
        private final int mTriggeringEvent;
        private final boolean mIsNtnOnlyCarrier;

        private SatelliteAccessControllerParams(Builder builder) {
            this.mAccessControlType = builder.mAccessControlType;
            this.mLocationQueryTimeMillis = builder.mLocationQueryTimeMillis;
            this.mOnDeviceLookupTimeMillis = builder.mOnDeviceLookupTimeMillis;
            this.mTotalCheckingTimeMillis = builder.mTotalCheckingTimeMillis;
            this.mIsAllowed = builder.mIsAllowed;
            this.mIsEmergency = builder.mIsEmergency;
            this.mResultCode = builder.mResultCode;
            this.mCountryCodes = builder.mCountryCodes;
            this.mConfigDataSource = builder.mConfigDataSource;
            this.mCarrierId = builder.mCarrierId;
            this.mTriggeringEvent = builder.mTriggeringEvent;
            this.mIsNtnOnlyCarrier = builder.mIsNtnOnlyCarrier;
        }

        public @SatelliteConstants.AccessControlType int getAccessControlType() {
            return mAccessControlType;
        }

        public long getLocationQueryTime() {
            return mLocationQueryTimeMillis;
        }

        public long getOnDeviceLookupTime() {
            return mOnDeviceLookupTimeMillis;
        }

        public long getTotalCheckingTime() {
            return mTotalCheckingTimeMillis;
        }

        public boolean getIsAllowed() {
            return mIsAllowed;
        }

        public boolean getIsEmergency() {
            return mIsEmergency;
        }

        public @SatelliteManager.SatelliteResult int getResultCode() {
            return mResultCode;
        }

        public String[] getCountryCodes() {
            return mCountryCodes;
        }

        public @SatelliteConstants.ConfigDataSource int getConfigDataSource() {
            return mConfigDataSource;
        }

        public int getCarrierId() {
            return mCarrierId;
        }

        @SatelliteConstants.TriggeringEvent public int getTriggeringEvent() {
            return mTriggeringEvent;
        }

        public boolean isNtnOnlyCarrier() {
            return mIsNtnOnlyCarrier;
        }

        /**
         * A builder class to create {@link SatelliteAccessControllerParams} data structure class
         */
        public static class Builder {
            private @SatelliteConstants.AccessControlType int mAccessControlType;
            private long mLocationQueryTimeMillis;
            private long mOnDeviceLookupTimeMillis;
            private long mTotalCheckingTimeMillis;
            private boolean mIsAllowed;
            private boolean mIsEmergency;
            private @SatelliteManager.SatelliteResult int mResultCode;
            private String[] mCountryCodes;
            private @SatelliteConstants.ConfigDataSource int mConfigDataSource;
            private int mCarrierId = UNKNOWN_CARRIER_ID;
            private @SatelliteConstants.TriggeringEvent int mTriggeringEvent =
                    TRIGGERING_EVENT_UNKNOWN;
            private boolean mIsNtnOnlyCarrier = false;

            /**
             * Sets AccessControlType value of {@link #SatelliteAccessController}
             * atom then returns Builder class
             */
            public Builder setAccessControlType(
                    @SatelliteConstants.AccessControlType int accessControlType) {
                this.mAccessControlType = accessControlType;
                return this;
            }

            /** Sets the location query time for current satellite enablement. */
            public Builder setLocationQueryTime(long locationQueryTimeMillis) {
                this.mLocationQueryTimeMillis = locationQueryTimeMillis;
                return this;
            }

            /** Sets the on device lookup time for current satellite enablement. */
            public Builder setOnDeviceLookupTime(long onDeviceLookupTimeMillis) {
                this.mOnDeviceLookupTimeMillis = onDeviceLookupTimeMillis;
                return this;
            }

            /** Sets the total checking time for current satellite enablement. */
            public Builder setTotalCheckingTime(long totalCheckingTimeMillis) {
                this.mTotalCheckingTimeMillis = totalCheckingTimeMillis;
                return this;
            }

            /** Sets whether the satellite communication is allowed from current location. */
            public Builder setIsAllowed(boolean isAllowed) {
                this.mIsAllowed = isAllowed;
                return this;
            }

            /** Sets whether the current satellite enablement is for emergency or not. */
            public Builder setIsEmergency(boolean isEmergency) {
                this.mIsEmergency = isEmergency;
                return this;
            }

            /** Sets the result code for checking whether satellite service is allowed from current
             location. */
            public Builder setResult(int result) {
                this.mResultCode = result;
                return this;
            }

            /** Sets the country code for current location while attempting satellite enablement. */
            public Builder setCountryCodes(String[] countryCodes) {
                this.mCountryCodes = Arrays.stream(countryCodes).toArray(String[]::new);
                return this;
            }

            /** Sets the config data source for checking whether satellite service is allowed from
             current location. */
            public Builder setConfigDatasource(int configDatasource) {
                this.mConfigDataSource = configDatasource;
                return this;
            }

            /** Sets the currently active NB-IoT NTN carrier ID. */
            public Builder setCarrierId(int carrierId) {
                this.mCarrierId = carrierId;
                return this;
            }

            /** Sets the triggering evenr for current satellite access controller metric. */
            public Builder setTriggeringEvent(
                    @SatelliteConstants.TriggeringEvent int triggeringEvent) {
                this.mTriggeringEvent = triggeringEvent;
                return this;
            }

            /**
             * Sets isNtnOnlyCarrier value of {@link SatelliteAccessController} atom
             * then returns Builder class
            */
            public Builder setIsNtnOnlyCarrier(boolean isNtnOnlyCarrier) {
                this.mIsNtnOnlyCarrier = isNtnOnlyCarrier;
                return this;
            }

            /**
             * Returns AccessControllerParams, which contains whole component of
             * {@link #SatelliteAccessController} atom
             */
            public SatelliteAccessControllerParams build() {
                return new SatelliteStats()
                        .new SatelliteAccessControllerParams(this);
            }
        }

        @Override
        public String toString() {
            return "AccessControllerParams("
                    + ", AccessControlType=" + mAccessControlType
                    + ", LocationQueryTime=" + mLocationQueryTimeMillis
                    + ", OnDeviceLookupTime=" + mOnDeviceLookupTimeMillis
                    + ", TotalCheckingTime=" + mTotalCheckingTimeMillis
                    + ", IsAllowed=" + mIsAllowed
                    + ", IsEmergency=" + mIsEmergency
                    + ", ResultCode=" + mResultCode
                    + ", CountryCodes=" + Arrays.toString(mCountryCodes)
                    + ", ConfigDataSource=" + mConfigDataSource
                    + ", CarrierId=" + mCarrierId
                    + ", TriggeringEvent=" + mTriggeringEvent
                    + ", IsNtnOnlyCarrier=" + mIsNtnOnlyCarrier
                    + ")";
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
        proto.countOfDemoModeSatelliteServiceEnablementsSuccess =
                param.getCountOfDemoModeSatelliteServiceEnablementsSuccess();
        proto.countOfDemoModeSatelliteServiceEnablementsFail =
                param.getCountOfDemoModeSatelliteServiceEnablementsFail();
        proto.countOfDemoModeOutgoingDatagramSuccess =
                param.getCountOfDemoModeOutgoingDatagramSuccess();
        proto.countOfDemoModeOutgoingDatagramFail = param.getCountOfDemoModeOutgoingDatagramFail();
        proto.countOfDemoModeIncomingDatagramSuccess =
                param.getCountOfDemoModeIncomingDatagramSuccess();
        proto.countOfDemoModeIncomingDatagramFail = param.getCountOfDemoModeIncomingDatagramFail();
        proto.countOfDatagramTypeKeepAliveSuccess = param.getCountOfDatagramTypeKeepAliveSuccess();
        proto.countOfDatagramTypeKeepAliveFail = param.getCountOfDatagramTypeKeepAliveFail();
        proto.countOfAllowedSatelliteAccess = param.getCountOfAllowedSatelliteAccess();
        proto.countOfDisallowedSatelliteAccess = param.getCountOfDisallowedSatelliteAccess();
        proto.countOfSatelliteAccessCheckFail = param.getCountOfSatelliteAccessCheckFail();
        proto.isProvisioned = param.isProvisioned();
        proto.carrierId = param.getCarrierId();
        proto.countOfSatelliteAllowedStateChangedEvents =
                param.getCountOfSatelliteAllowedStateChangedEvents();
        proto.countOfSuccessfulLocationQueries = param.getCountOfSuccessfulLocationQueries();
        proto.countOfFailedLocationQueries = param.getCountOfFailedLocationQueries();
        proto.countOfP2PSmsAvailableNotificationShown =
                param.getCountOfP2PSmsAvailableNotificationShown();
        proto.countOfP2PSmsAvailableNotificationRemoved =
                param.getCountOfP2PSmsAvailableNotificationRemoved();
        proto.isNtnOnlyCarrier = param.isNtnOnlyCarrier();
        proto.versionOfSatelliteAccessConfig = param.getVersionSatelliteAccessConfig();

        mAtomsStorage.addSatelliteControllerStats(proto);
    }

    /**  Create a new atom or update an existing atom for SatelliteSession metrics */
    public synchronized void onSatelliteSessionMetrics(SatelliteSessionParams param) {
        SatelliteSession proto = new SatelliteSession();
        proto.satelliteServiceInitializationResult =
                param.getSatelliteServiceInitializationResult();
        proto.satelliteTechnology = param.getSatelliteTechnology();
        proto.count = 1;
        proto.satelliteServiceTerminationResult = param.getTerminationResult();
        proto.initializationProcessingTimeMillis = param.getInitializationProcessingTime();
        proto.terminationProcessingTimeMillis = param.getTerminationProcessingTime();
        proto.sessionDurationSeconds = param.getSessionDuration();
        proto.countOfOutgoingDatagramSuccess = param.getCountOfIncomingDatagramSuccess();
        proto.countOfOutgoingDatagramFailed = param.getCountOfOutgoingDatagramFailed();
        proto.countOfIncomingDatagramSuccess = param.getCountOfIncomingDatagramSuccess();
        proto.countOfIncomingDatagramFailed = param.getCountOfOutgoingDatagramFailed();
        proto.isDemoMode = param.getIsDemoMode();
        proto.maxNtnSignalStrengthLevel = param.getMaxNtnSignalStrengthLevel();
        proto.carrierId = param.getCarrierId();
        proto.countOfSatelliteNotificationDisplayed =
                param.getCountOfSatelliteNotificationDisplayed();
        proto.countOfAutoExitDueToScreenOff = param.getCountOfAutoExitDueToScreenOff();
        proto.countOfAutoExitDueToTnNetwork = param.getCountOfAutoExitDueToTnNetwork();
        proto.isEmergency = param.getIsEmergency();
        proto.isNtnOnlyCarrier = param.isNtnOnlyCarrier();
        proto.maxInactivityDurationSec = param.getMaxInactivityDurationSec();
        mAtomsStorage.addSatelliteSessionStats(proto);
    }

    /**  Create a new atom for SatelliteIncomingDatagram metrics */
    public synchronized void onSatelliteIncomingDatagramMetrics(
            SatelliteIncomingDatagramParams param) {
        SatelliteIncomingDatagram proto = new SatelliteIncomingDatagram();
        proto.resultCode = param.getResultCode();
        proto.datagramSizeBytes = param.getDatagramSizeBytes();
        proto.datagramTransferTimeMillis = param.getDatagramTransferTimeMillis();
        proto.isDemoMode = param.getIsDemoMode();
        proto.carrierId = param.getCarrierId();
        proto.isNtnOnlyCarrier = param.isNtnOnlyCarrier();
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
        proto.isDemoMode = param.getIsDemoMode();
        proto.carrierId = param.getCarrierId();
        proto.isNtnOnlyCarrier = param.isNtnOnlyCarrier();
        mAtomsStorage.addSatelliteOutgoingDatagramStats(proto);
    }

    /**  Create a new atom for SatelliteProvision metrics */
    public synchronized void onSatelliteProvisionMetrics(SatelliteProvisionParams param) {
        SatelliteProvision proto = new SatelliteProvision();
        proto.resultCode = param.getResultCode();
        proto.provisioningTimeSec = param.getProvisioningTimeSec();
        proto.isProvisionRequest = param.getIsProvisionRequest();
        proto.isCanceled = param.getIsCanceled();
        proto.carrierId = param.getCarrierId();
        proto.isNtnOnlyCarrier = param.isNtnOnlyCarrier();
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
        proto.isMultiSim = param.isMultiSim();
        proto.recommendingHandoverType = param.getRecommendingHandoverType();
        proto.isSatelliteAllowedInCurrentLocation = param.isSatelliteAllowedInCurrentLocation();
        proto.isWifiConnected = param.isWifiConnected();
        proto.carrierId = param.getCarrierId();
        proto.isNtnOnlyCarrier = param.isNtnOnlyCarrier();
        proto.count = 1;
        mAtomsStorage.addSatelliteSosMessageRecommenderStats(proto);
    }

    /**  Create a new atom for CarrierRoamingSatelliteSession metrics */
    public synchronized  void onCarrierRoamingSatelliteSessionMetrics(
            CarrierRoamingSatelliteSessionParams param) {
        CarrierRoamingSatelliteSession proto = new CarrierRoamingSatelliteSession();
        proto.carrierId = param.getCarrierId();
        proto.isNtnRoamingInHomeCountry = param.getIsNtnRoamingInHomeCountry();
        proto.totalSatelliteModeTimeSec = param.getTotalSatelliteModeTimeSec();
        proto.numberOfSatelliteConnections = param.getNumberOfSatelliteConnections();
        proto.avgDurationOfSatelliteConnectionSec = param.getAvgDurationOfSatelliteConnectionSec();
        proto.satelliteConnectionGapMinSec = param.mSatelliteConnectionGapMinSec;
        proto.satelliteConnectionGapAvgSec = param.mSatelliteConnectionGapAvgSec;
        proto.satelliteConnectionGapMaxSec = param.mSatelliteConnectionGapMaxSec;
        proto.rsrpAvg = param.mRsrpAvg;
        proto.rsrpMedian = param.mRsrpMedian;
        proto.rssnrAvg = param.mRssnrAvg;
        proto.rssnrMedian = param.mRssnrMedian;
        proto.countOfIncomingSms = param.mCountOfIncomingSms;
        proto.countOfOutgoingSms = param.mCountOfOutgoingSms;
        proto.countOfIncomingMms = param.mCountOfIncomingMms;
        proto.countOfOutgoingMms = param.mCountOfOutgoingMms;
        mAtomsStorage.addCarrierRoamingSatelliteSessionStats(proto);
    }

    /**  Create a new atom for CarrierRoamingSatelliteSession metrics */
    public synchronized  void onCarrierRoamingSatelliteControllerStatsMetrics(
            CarrierRoamingSatelliteControllerStatsParams param) {
        CarrierRoamingSatelliteControllerStats proto = new CarrierRoamingSatelliteControllerStats();
        proto.configDataSource = param.mConfigDataSource;
        proto.countOfEntitlementStatusQueryRequest = param.mCountOfEntitlementStatusQueryRequest;
        proto.countOfSatelliteConfigUpdateRequest = param.mCountOfSatelliteConfigUpdateRequest;
        proto.countOfSatelliteNotificationDisplayed = param.mCountOfSatelliteNotificationDisplayed;
        proto.satelliteSessionGapMinSec = param.mSatelliteSessionGapMinSec;
        proto.satelliteSessionGapAvgSec = param.mSatelliteSessionGapAvgSec;
        proto.satelliteSessionGapMaxSec = param.mSatelliteSessionGapMaxSec;
        proto.carrierId = param.getCarrierId();
        proto.isDeviceEntitled = param.isDeviceEntitled();
        mAtomsStorage.addCarrierRoamingSatelliteControllerStats(proto);
    }

    /**  Create a new atom for SatelliteEntitlement metrics */
    public synchronized  void onSatelliteEntitlementMetrics(SatelliteEntitlementParams param) {
        SatelliteEntitlement proto = new SatelliteEntitlement();
        proto.carrierId = param.getCarrierId();
        proto.result = param.getResult();
        proto.entitlementStatus = param.getEntitlementStatus();
        proto.isRetry = param.getIsRetry();
        proto.count = param.getCount();
        mAtomsStorage.addSatelliteEntitlementStats(proto);
    }

    /**  Create a new atom for SatelliteConfigUpdater metrics */
    public synchronized  void onSatelliteConfigUpdaterMetrics(SatelliteConfigUpdaterParams param) {
        SatelliteConfigUpdater proto = new SatelliteConfigUpdater();
        proto.configVersion = param.getConfigVersion();
        proto.oemConfigResult = param.getOemConfigResult();
        proto.carrierConfigResult = param.getCarrierConfigResult();
        proto.count = param.getCount();
        mAtomsStorage.addSatelliteConfigUpdaterStats(proto);
    }

    /**  Create a new atom or update an existing atom for SatelliteAccessController metrics */
    public synchronized void onSatelliteAccessControllerMetrics(
            SatelliteAccessControllerParams param) {
        SatelliteAccessController proto = new SatelliteAccessController();
        proto.accessControlType = param.getAccessControlType();
        proto.locationQueryTimeMillis = param.getLocationQueryTime();
        proto.onDeviceLookupTimeMillis = param.getOnDeviceLookupTime();
        proto.totalCheckingTimeMillis = param.getTotalCheckingTime();
        proto.isAllowed = param.getIsAllowed();
        proto.isEmergency = param.getIsEmergency();
        proto.resultCode = param.getResultCode();
        proto.countryCodes = param.getCountryCodes();
        proto.configDataSource = param.getConfigDataSource();
        proto.carrierId = param.getCarrierId();
        proto.triggeringEvent = param.getTriggeringEvent();
        proto.isNtnOnlyCarrier = param.isNtnOnlyCarrier();
        mAtomsStorage.addSatelliteAccessControllerStats(proto);
    }
}
