/*
 * Copyright (c) 2015, Motorola Mobility LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     - Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     - Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     - Neither the name of Motorola Mobility nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL MOTOROLA MOBILITY LLC BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package com.android.ims;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.net.Uri;

import com.android.ims.internal.Logger;

/**
 * RcsPresenceInfo is the class for presence information.
 * It is used to pass information to application for inent ACTION_PRESENCE_CHANGED
 * need to get it by the following statement:
 * ArrayList<RcsPresenceInfo> rcsPresenceInfoList = intent.getParcelableArrayListExtra(
 *           RcsPresence.EXTRA_PRESENCE_INFO_LIST);
 *
 * @see RcsPresence#ACTION_PRESENCE_CHANGED
 *
 * @hide
 */
public class RcsPresenceInfo implements Parcelable {
    /**
     * Key for save contact_number.
     * It is passed by getCapabilityByContacts or getAvailability
     *
     * @see #getContactNumber
     */
     private static final String CONTACT_NUMBER = "contact_number";

    /**
     * Key for the flag to indicate if the number is volte enabled.
     *
     * @see #getVolteStatus
     */
    public static final String VOLTE_STATUS = "volte_status";

    /**
     * The Volte status:
     * If the contact got the 404 for single contact fetch.
     * or it got "rejected", "noresource" and "giveup", then it is
     * VOLTE_DISABLED. Or it is VOLTE_ENBLED.
     * If we didn't get a success polling yet then it is VOLTE_UNKNOWN.
     */
    public static class VolteStatus{
        /**
         * Didn't poll yet.
         */
        public static final int VOLTE_UNKNOWN = -1;

        /**
         * Volte disabled for 404 response for single contact fetch
         * or get "rejected", "noresource" and "giveup" notification.
         */
        public static final int VOLTE_DISABLED = 0;

        /**
         * Volte enabled for get proper notification.
         */
        public static final int VOLTE_ENABLED = 1;
    }

    /**
     * For extension consideration we deinfed the sercice type here.
     * Currently we only support the VoLte call and VT call.
     *
     * The service type for RCS
     */
    public static interface ServiceType {
        /**
         * For VoLte call.
         */
        public static final int VOLTE_CALL = 1;

        /**
         * For VT call.
         */
        public static final int VT_CALL = 2;
    }

    /**
     * Service state
     *
     * @see #getServiceState
     */
    public static class ServiceState {
        /**
         * ONLINE means the servie is available.
         */
        public static final int ONLINE = 1;

        /**
         * OFFLINE means the service is not available.
         */
        public static final int OFFLINE = 0;

       /**
        * UNKNOWN means the presence service information didn't be got yet.
        */
        public static final int UNKNOWN = -1;
    }

    /**
     * The presence information is maintained by key and value pair.
     * ServiceInfoKey defines the key of the current supported information.
     */
    public static class ServiceInfoKey {
        /**
         * Service type. It is defined by ServiceType.
         *
         * @see ServiceType
         */
        public static final String SERVICE_TYPE = "service_type"; // VOLTE_CALL,etc

        /**
         * Service state. It is defined by ServiceState.
         *
         * @see ServiceState
         * @see #getServiceState
         */
        public static final String STATE = "state"; // ONLINE, etc.

        /**
         * The service contact. For example, the phone requests presence information for number
         * "12345678", the service responses the presence with "987654321" as the service number
         * of video call. Then the phone should start the video call with "987654321".
         * The "987654321" is the service number.
         *
         * @see #getServiceContact
         */
        public static final String SERVICE_CONTACT = "service_contact";

        /**
         * The timestamp which got from network.
         *
         * @see #getTimeStamp
         */
        public static final String TIMESTAMP = "timestamp";
    }

    /**
     * Return the contact number.
     * It is passed by getCapabilityByContacts or getAvailability
     *
     * @return the contact number which has been passed in.
     *
     * @see #CONTACT_NUMBER
     */
    public String getContactNumber() {
        return mServiceInfo.getString(CONTACT_NUMBER);
    }

    /**
     * @Return the VolteStatus.
     */
    public int getVolteStatus(){
        return mServiceInfo.getInt(VOLTE_STATUS);
    }

    /**
     * Return the ServiceState of the specific serviceType.
     *
     * @param serviceType it is defined by ServiceType.
     *
     * @return the service presence state which has been described in ServiceInfoKey.
     *
     * @see ServiceType
     * @see ServiceState
     * @see ServiceInfoKey#STATE
     */
    public int getServiceState(int serviceType) {
        return getServiceInfo(serviceType, ServiceInfoKey.STATE, ServiceState.UNKNOWN);
    }

    /**
     * Return the service contact of the specific serviceType.
     *
     * @param serviceType It is defined by ServiceType.
     *
     * @return the service contact which is described in ServiceInfoKey.
     *
     * @see ServiceType
     * @see ServiceInfoKey#SERVICE_CONTACT
     */
    public String getServiceContact(int serviceType) {
        return getServiceInfo(serviceType, ServiceInfoKey.SERVICE_CONTACT, "");
    }

    /**
     * Return the timestamp.
     *
     * @param serviceType It is defined by ServiceType.
     *
     * @return the timestamp which has been got from server.
     *
     * @see ServiceType
     * @see ServiceInfoKey#TIMESTAMP
     */
    public long getTimeStamp(int serviceType) {
        return getServiceInfo(serviceType, ServiceInfoKey.TIMESTAMP, 0L);
    }

    /**
     * @hide
     */
    public RcsPresenceInfo() {
    }

    /**
     * @hide
     */
    public RcsPresenceInfo(Parcel source) {
        mServiceInfo.readFromParcel(source);
    }

    /**
     * @hide
     */
    private Bundle getBundle() {
        return mServiceInfo;
    }

    /**
     * @hide
     */
    public RcsPresenceInfo(String contactNumber,int volteStatus,
            int ipVoiceCallState, String ipVoiceCallServiceNumber, long ipVoiceCallTimestamp,
            int ipVideoCallState, String ipVideoCallServiceNumber, long ipVideoCallTimestamp) {
        mServiceInfo.putString(CONTACT_NUMBER, contactNumber);
        mServiceInfo.putInt(VOLTE_STATUS, volteStatus);

        set(ServiceType.VOLTE_CALL, ipVoiceCallState, ipVoiceCallServiceNumber,
                ipVoiceCallTimestamp);

        set(ServiceType.VT_CALL, ipVideoCallState, ipVideoCallServiceNumber,
                ipVideoCallTimestamp);
    }

    private void set(int serviceType, int state, String serviceNumber, long timestamp) {
        Bundle capability = new Bundle();

        capability.putInt(ServiceInfoKey.SERVICE_TYPE, serviceType);
        capability.putInt(ServiceInfoKey.STATE, state);
        capability.putString(ServiceInfoKey.SERVICE_CONTACT, serviceNumber);
        capability.putLong(ServiceInfoKey.TIMESTAMP, timestamp);

        mServiceInfo.putBundle(String.valueOf(serviceType), capability);
    }

    /**
     * Overload
     * @hide
     */
    public static final Parcelable.Creator<RcsPresenceInfo> CREATOR = new
            Parcelable.Creator<RcsPresenceInfo>() {
        public RcsPresenceInfo createFromParcel(Parcel in) {
            return new RcsPresenceInfo(in);
        }

        public RcsPresenceInfo[] newArray(int size) {
            return new RcsPresenceInfo[size];
        }
    };

    /**
     * Overload
     * @hide
     */
    public void writeToParcel(Parcel dest, int flags) {
        mServiceInfo.writeToParcel(dest, flags);
    }

    /**
     * Overload
     * @hide
     */
    public int describeContents() {
        return 0;
    }

    private Bundle mServiceInfo = new Bundle();

    private String getServiceInfo(int serviceType, String infoKey, String defaultValue) {
        Bundle serviceInfo = mServiceInfo.getBundle(String.valueOf(serviceType));

        if (serviceInfo != null) {
            return serviceInfo.getString(infoKey);
        }
        return defaultValue;
    }

    private long getServiceInfo(int serviceType, String infoKey, long defaultValue) {
        Bundle serviceInfo = mServiceInfo.getBundle(String.valueOf(serviceType));
        if (serviceInfo != null) {
            return serviceInfo.getLong(infoKey);
        }

        return defaultValue;
    }

    private int getServiceInfo(int serviceType, String infoType, int defaultValue) {
        Bundle serviceInfo = mServiceInfo.getBundle(String.valueOf(serviceType));
        if (serviceInfo != null) {
            return serviceInfo.getInt(infoType);
        }
        return defaultValue;
    }

    private Uri getServiceInfo(int serviceType, String infoKey, Uri defaultValue) {
        Bundle serviceInfo = mServiceInfo.getBundle(String.valueOf(serviceType));
        if (serviceInfo != null) {
            return (Uri)serviceInfo.getParcelable(infoKey);
        }

        return defaultValue;
    }

    public String toString() {
        return" contactNumber=" + Logger.hidePhoneNumberPii(getContactNumber()) +
            " volteStatus=" + getVolteStatus() +
            " ipVoiceCallSate=" + getServiceState(ServiceType.VOLTE_CALL) +
            " ipVoiceCallServiceNumber=" +
                Logger.hidePhoneNumberPii(getServiceContact(ServiceType.VOLTE_CALL)) +
            " ipVoiceCallTimestamp=" + getTimeStamp(ServiceType.VOLTE_CALL) +
            " ipVideoCallSate=" + getServiceState(ServiceType.VT_CALL) +
            " ipVideoCallServiceNumber=" +
                Logger.hidePhoneNumberPii(getServiceContact(ServiceType.VT_CALL)) +
            " ipVideoCallTimestamp=" + getTimeStamp(ServiceType.VT_CALL);
    }
}

