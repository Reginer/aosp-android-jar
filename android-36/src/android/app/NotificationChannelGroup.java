/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.app;

import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.proto.ProtoOutputStream;

import com.android.modules.utils.TypedXmlPullParser;
import com.android.modules.utils.TypedXmlSerializer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A grouping of related notification channels. e.g., channels that all belong to a single account.
 */
public final class NotificationChannelGroup implements Parcelable {

    /**
     * The maximum length for text fields in a NotificationChannelGroup. Fields will be truncated at
     * this limit.
     * @hide
     */
    public static final int MAX_TEXT_LENGTH = 1000;

    private static final String TAG_GROUP = "channelGroup";
    private static final String ATT_NAME = "name";
    private static final String ATT_DESC = "desc";
    private static final String ATT_ID = "id";
    private static final String ATT_BLOCKED = "blocked";
    private static final String ATT_USER_LOCKED = "locked";

    /**
     * @hide
     */
    public static final int USER_LOCKED_BLOCKED_STATE = 0x00000001;

    /**
     * @see #getId()
     */
    @UnsupportedAppUsage
    private final String mId;
    private CharSequence mName;
    private String mDescription;
    private boolean mBlocked;
    private List<NotificationChannel> mChannels = new ArrayList();
    // Bitwise representation of fields that have been changed by the user
    private int mUserLockedFields;

    /**
     * Creates a notification channel group.
     *
     * @param id The id of the group. Must be unique per package.  the value may be truncated if
     *           it is too long.
     * @param name The user visible name of the group. You can rename this group when the system
     *             locale changes by listening for the {@link Intent#ACTION_LOCALE_CHANGED}
     *             broadcast. <p>The recommended maximum length is 40 characters; the value may be
     *             truncated if it is too long.
     */
    public NotificationChannelGroup(String id, CharSequence name) {
        this.mId = getTrimmedString(id);
        this.mName = name != null ? getTrimmedString(name.toString()) : null;
    }

    /**
     * @hide
     */
    protected NotificationChannelGroup(Parcel in) {
        if (in.readByte() != 0) {
            mId = getTrimmedString(in.readString());
        } else {
            mId = null;
        }
        if (in.readByte() != 0) {
            mName = getTrimmedString(in.readString());
        } else {
            mName = "";
        }
        if (in.readByte() != 0) {
            mDescription = getTrimmedString(in.readString());
        } else {
            mDescription = null;
        }
        if (in.readByte() != 0) {
            mChannels = in.readParcelable(NotificationChannelGroup.class.getClassLoader(),
                    ParceledListSlice.class).getList();
        } else {
            mChannels = new ArrayList<>();
        }
        mBlocked = in.readBoolean();
        mUserLockedFields = in.readInt();
    }

    private String getTrimmedString(String input) {
        if (input != null && input.length() > MAX_TEXT_LENGTH) {
            return input.substring(0, MAX_TEXT_LENGTH);
        }
        return input;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (mId != null) {
            dest.writeByte((byte) 1);
            dest.writeString(mId);
        } else {
            dest.writeByte((byte) 0);
        }
        if (mName != null) {
            dest.writeByte((byte) 1);
            dest.writeString(mName.toString());
        } else {
            dest.writeByte((byte) 0);
        }
        if (mDescription != null) {
            dest.writeByte((byte) 1);
            dest.writeString(mDescription);
        } else {
            dest.writeByte((byte) 0);
        }
        if (mChannels != null) {
            dest.writeByte((byte) 1);
            dest.writeParcelable(new ParceledListSlice<>(mChannels), flags);
        } else {
            dest.writeByte((byte) 0);
        }
        dest.writeBoolean(mBlocked);
        dest.writeInt(mUserLockedFields);
    }

    /**
     * Returns the id of this group.
     */
    public String getId() {
        return mId;
    }

    /**
     * Returns the user visible name of this group.
     */
    public CharSequence getName() {
        return mName;
    }

    /**
     * Returns the user visible description of this group.
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * Returns the list of channels that belong to this group
     */
    public List<NotificationChannel> getChannels() {
        return mChannels;
    }

    /**
     * Returns whether or not notifications posted to {@link NotificationChannel channels} belonging
     * to this group are blocked. This value is independent of
     * {@link NotificationManager#areNotificationsEnabled()} and
     * {@link NotificationChannel#getImportance()}.
     */
    public boolean isBlocked() {
        return mBlocked;
    }

    /**
     * Sets the user visible description of this group.
     *
     * <p>The recommended maximum length is 300 characters; the value may be truncated if it is too
     * long.
     */
    public void setDescription(String description) {
        mDescription = getTrimmedString(description);
    }

    /**
     * @hide
     */
    @TestApi
    public void setBlocked(boolean blocked) {
        mBlocked = blocked;
    }

    /**
     * @hide
     */
    public void addChannel(NotificationChannel channel) {
        mChannels.add(channel);
    }

    /**
     * @hide
     */
    public void setChannels(List<NotificationChannel> channels) {
        mChannels.clear();
        if (channels != null) {
            mChannels.addAll(channels);
        }
    }

    /**
     * @hide
     */
    @TestApi
    public void lockFields(int field) {
        mUserLockedFields |= field;
    }

    /**
     * @hide
     */
    public void unlockFields(int field) {
        mUserLockedFields &= ~field;
    }

    /**
     * @hide
     */
    @TestApi
    public int getUserLockedFields() {
        return mUserLockedFields;
    }

    /**
     * @hide
     */
    public void populateFromXml(TypedXmlPullParser parser) {
        // Name, id, and importance are set in the constructor.
        setDescription(parser.getAttributeValue(null, ATT_DESC));
        setBlocked(parser.getAttributeBoolean(null, ATT_BLOCKED, false));
    }

    /**
     * @hide
     */
    public void writeXml(TypedXmlSerializer out) throws IOException {
        out.startTag(null, TAG_GROUP);

        out.attribute(null, ATT_ID, getId());
        if (getName() != null) {
            out.attribute(null, ATT_NAME, getName().toString());
        }
        if (getDescription() != null) {
            out.attribute(null, ATT_DESC, getDescription().toString());
        }
        out.attributeBoolean(null, ATT_BLOCKED, isBlocked());
        out.attributeInt(null, ATT_USER_LOCKED, mUserLockedFields);

        out.endTag(null, TAG_GROUP);
    }

    /**
     * @hide
     */
    @SystemApi
    public JSONObject toJson() throws JSONException {
        JSONObject record = new JSONObject();
        record.put(ATT_ID, getId());
        record.put(ATT_NAME, getName());
        record.put(ATT_DESC, getDescription());
        record.put(ATT_BLOCKED, isBlocked());
        record.put(ATT_USER_LOCKED, mUserLockedFields);
        return record;
    }

    public static final @android.annotation.NonNull Creator<NotificationChannelGroup> CREATOR =
            new Creator<NotificationChannelGroup>() {
        @Override
        public NotificationChannelGroup createFromParcel(Parcel in) {
            return new NotificationChannelGroup(in);
        }

        @Override
        public NotificationChannelGroup[] newArray(int size) {
            return new NotificationChannelGroup[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationChannelGroup that = (NotificationChannelGroup) o;
        return isBlocked() == that.isBlocked() &&
                mUserLockedFields == that.mUserLockedFields &&
                Objects.equals(getId(), that.getId()) &&
                Objects.equals(getName(), that.getName()) &&
                Objects.equals(getDescription(), that.getDescription()) &&
                Objects.equals(getChannels(), that.getChannels());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getDescription(), isBlocked(), getChannels(),
                mUserLockedFields);
    }

    @Override
    public NotificationChannelGroup clone() {
        NotificationChannelGroup cloned = new NotificationChannelGroup(getId(), getName());
        cloned.setDescription(getDescription());
        cloned.setBlocked(isBlocked());
        for (NotificationChannel c : mChannels) {
            cloned.addChannel(c.copy());
        }
        cloned.lockFields(mUserLockedFields);
        return cloned;
    }

    @Override
    public String toString() {
        return "NotificationChannelGroup{"
                + "mId='" + mId + '\''
                + ", mName=" + mName
                + ", mDescription=" + (!TextUtils.isEmpty(mDescription) ? "hasDescription " : "")
                + ", mBlocked=" + mBlocked
                + ", mChannels=" + mChannels
                + ", mUserLockedFields=" + mUserLockedFields
                + '}';
    }

    /** @hide */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        final long token = proto.start(fieldId);

        proto.write(NotificationChannelGroupProto.ID, mId);
        proto.write(NotificationChannelGroupProto.NAME, mName.toString());
        proto.write(NotificationChannelGroupProto.DESCRIPTION, mDescription);
        proto.write(NotificationChannelGroupProto.IS_BLOCKED, mBlocked);
        for (NotificationChannel channel : mChannels) {
            channel.dumpDebug(proto, NotificationChannelGroupProto.CHANNELS);
        }
        proto.end(token);
    }
}
