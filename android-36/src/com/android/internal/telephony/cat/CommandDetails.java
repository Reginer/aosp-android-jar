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

package com.android.internal.telephony.cat;

import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

abstract class ValueObject {
    abstract ComprehensionTlvTag getTag();
}

/**
 * Class for Command Details object of proactive commands from SIM.
 * {@hide}
 */
public class CommandDetails extends ValueObject implements Parcelable {
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean compRequired;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int commandNumber;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int typeOfCommand;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int commandQualifier;

    @Override
    public ComprehensionTlvTag getTag() {
        return ComprehensionTlvTag.COMMAND_DETAILS;
    }

    CommandDetails() {
    }

    public boolean compareTo(CommandDetails other) {
        return (this.compRequired == other.compRequired &&
                this.commandNumber == other.commandNumber &&
                this.commandQualifier == other.commandQualifier &&
                this.typeOfCommand == other.typeOfCommand);
    }

    public CommandDetails(Parcel in) {
        compRequired = in.readInt() != 0;
        commandNumber = in.readInt();
        typeOfCommand = in.readInt();
        commandQualifier = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(compRequired ? 1 : 0);
        dest.writeInt(commandNumber);
        dest.writeInt(typeOfCommand);
        dest.writeInt(commandQualifier);
    }

    public static final Parcelable.Creator<CommandDetails> CREATOR =
                                new Parcelable.Creator<CommandDetails>() {
        @Override
        public CommandDetails createFromParcel(Parcel in) {
            return new CommandDetails(in);
        }

        @Override
        public CommandDetails[] newArray(int size) {
            return new CommandDetails[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return "CmdDetails: compRequired=" + compRequired +
                " commandNumber=" + commandNumber +
                " typeOfCommand=" + typeOfCommand +
                " commandQualifier=" + commandQualifier;
    }
}

class DeviceIdentities extends ValueObject {
    public int sourceId;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int destinationId;

    @Override
    ComprehensionTlvTag getTag() {
        return ComprehensionTlvTag.DEVICE_IDENTITIES;
    }
}

// Container class to hold icon identifier value.
class IconId extends ValueObject {
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    int recordNumber;
    boolean selfExplanatory;

    @Override
    ComprehensionTlvTag getTag() {
        return ComprehensionTlvTag.ICON_ID;
    }
}

// Container class to hold item icon identifier list value.
class ItemsIconId extends ValueObject {
    int [] recordNumbers;
    boolean selfExplanatory;

    @Override
    ComprehensionTlvTag getTag() {
        return ComprehensionTlvTag.ITEM_ICON_ID_LIST;
    }
}
