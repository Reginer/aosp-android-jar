/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.content.pm;

import static android.content.res.Resources.ID_NULL;

import android.annotation.DrawableRes;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.StringRes;
import android.annotation.SystemApi;
import android.content.res.ResourceId;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.util.Slog;

import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.modules.utils.TypedXmlPullParser;
import com.android.modules.utils.TypedXmlSerializer;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;
import java.util.Objects;

/**
 * A container to describe the dialog to be shown when the user tries to launch a suspended
 * application. The suspending app can customize the dialog's following attributes:
 * <ul>
 * <li>The dialog icon, by providing a resource id.
 * <li>The title text, by providing a resource id.
 * <li>The text of the dialog's body, by providing a resource id or a string.
 * <li>The text on the neutral button by providing a resource id.
 * <li>The action performed on tapping the neutral button. Only {@link #BUTTON_ACTION_UNSUSPEND}
 * and {@link #BUTTON_ACTION_MORE_DETAILS} are currently supported.
 * </ul>
 * System defaults are used whenever any of these are not provided, or any of the provided resource
 * ids cannot be resolved at the time of displaying the dialog.
 *
 * @hide
 * @see PackageManager#setPackagesSuspended(String[], boolean, PersistableBundle, PersistableBundle,
 * SuspendDialogInfo)
 * @see Builder
 */
@SystemApi
public final class SuspendDialogInfo implements Parcelable {
    private static final String TAG = SuspendDialogInfo.class.getSimpleName();
    private static final String XML_ATTR_ICON_RES_ID = "iconResId";
    private static final String XML_ATTR_TITLE_RES_ID = "titleResId";
    private static final String XML_ATTR_TITLE = "title";
    private static final String XML_ATTR_DIALOG_MESSAGE_RES_ID = "dialogMessageResId";
    private static final String XML_ATTR_DIALOG_MESSAGE = "dialogMessage";
    private static final String XML_ATTR_BUTTON_TEXT_RES_ID = "buttonTextResId";
    private static final String XML_ATTR_BUTTON_TEXT = "buttonText";
    private static final String XML_ATTR_BUTTON_ACTION = "buttonAction";

    private final int mIconResId;
    private final int mTitleResId;
    private final String mTitle;
    private final int mDialogMessageResId;
    private final String mDialogMessage;
    private final int mNeutralButtonTextResId;
    private final String mNeutralButtonText;
    private final int mNeutralButtonAction;

    /**
     * Used with {@link Builder#setNeutralButtonAction(int)} to create a neutral button that
     * starts the {@link android.content.Intent#ACTION_SHOW_SUSPENDED_APP_DETAILS} activity.
     * @see Builder#setNeutralButtonAction(int)
     */
    public static final int BUTTON_ACTION_MORE_DETAILS = 0;

    /**
     * Used with {@link Builder#setNeutralButtonAction(int)} to create a neutral button that
     * unsuspends the app that the user was trying to launch and continues with the launch. The
     * system also sends the broadcast
     * {@link android.content.Intent#ACTION_PACKAGE_UNSUSPENDED_MANUALLY} to the suspending app
     * when this happens.
     * @see Builder#setNeutralButtonAction(int)
     * @see android.content.Intent#ACTION_PACKAGE_UNSUSPENDED_MANUALLY
     */
    public static final int BUTTON_ACTION_UNSUSPEND = 1;

    /**
     * Button actions to specify what happens when the user taps on the neutral button.
     * To be used with {@link Builder#setNeutralButtonAction(int)}.
     *
     * @hide
     * @see Builder#setNeutralButtonAction(int)
     */
    @IntDef(flag = true, prefix = {"BUTTON_ACTION_"}, value = {
            BUTTON_ACTION_MORE_DETAILS,
            BUTTON_ACTION_UNSUSPEND
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ButtonAction {
    }

    /**
     * @return the resource id of the icon to be used with the dialog
     * @hide
     */
    @DrawableRes
    public int getIconResId() {
        return mIconResId;
    }

    /**
     * @return the resource id of the title to be used with the dialog
     * @hide
     */
    @StringRes
    public int getTitleResId() {
        return mTitleResId;
    }

    /**
     * @return the title to be shown on the dialog. Returns {@code null} if {@link #getTitleResId()}
     * returns a valid resource id
     * @hide
     */
    @Nullable
    public String getTitle() {
        return mTitle;
    }

    /**
     * @return the resource id of the text to be shown in the dialog's body
     * @hide
     */
    @StringRes
    public int getDialogMessageResId() {
        return mDialogMessageResId;
    }

    /**
     * @return the text to be shown in the dialog's body. Returns {@code null} if {@link
     * #getDialogMessageResId()} returns a valid resource id
     * @hide
     */
    @Nullable
    public String getDialogMessage() {
        return mDialogMessage;
    }

    /**
     * @return the text to be shown on the neutral button
     * @hide
     */
    @StringRes
    public int getNeutralButtonTextResId() {
        return mNeutralButtonTextResId;
    }

    /**
     * @return the text to be shown on the neutral button. Returns {@code null} if
     * {@link #getNeutralButtonTextResId()} returns a valid resource id
     * @hide
     */
    @Nullable
    public String getNeutralButtonText() {
        return mNeutralButtonText;
    }

    /**
     * @return The {@link ButtonAction} that happens on tapping this button
     * @hide
     */
    @ButtonAction
    public int getNeutralButtonAction() {
        return mNeutralButtonAction;
    }

    /**
     * @hide
     */
    public void saveToXml(TypedXmlSerializer out) throws IOException {
        if (mIconResId != ID_NULL) {
            out.attributeInt(null, XML_ATTR_ICON_RES_ID, mIconResId);
        }
        if (mTitleResId != ID_NULL) {
            out.attributeInt(null, XML_ATTR_TITLE_RES_ID, mTitleResId);
        } else {
            XmlUtils.writeStringAttribute(out, XML_ATTR_TITLE, mTitle);
        }
        if (mDialogMessageResId != ID_NULL) {
            out.attributeInt(null, XML_ATTR_DIALOG_MESSAGE_RES_ID, mDialogMessageResId);
        } else {
            XmlUtils.writeStringAttribute(out, XML_ATTR_DIALOG_MESSAGE, mDialogMessage);
        }
        if (mNeutralButtonTextResId != ID_NULL) {
            out.attributeInt(null, XML_ATTR_BUTTON_TEXT_RES_ID, mNeutralButtonTextResId);
        } else {
            XmlUtils.writeStringAttribute(out, XML_ATTR_BUTTON_TEXT, mNeutralButtonText);
        }
        out.attributeInt(null, XML_ATTR_BUTTON_ACTION, mNeutralButtonAction);
    }

    /**
     * @hide
     */
    public static SuspendDialogInfo restoreFromXml(TypedXmlPullParser in) {
        final SuspendDialogInfo.Builder dialogInfoBuilder = new SuspendDialogInfo.Builder();
        try {
            final int iconId = in.getAttributeInt(null, XML_ATTR_ICON_RES_ID, ID_NULL);
            final int titleId = in.getAttributeInt(null, XML_ATTR_TITLE_RES_ID, ID_NULL);
            final String title = XmlUtils.readStringAttribute(in, XML_ATTR_TITLE);
            final int buttonTextId =
                    in.getAttributeInt(null, XML_ATTR_BUTTON_TEXT_RES_ID, ID_NULL);
            final String buttonText = XmlUtils.readStringAttribute(in, XML_ATTR_BUTTON_TEXT);
            final int buttonAction =
                    in.getAttributeInt(null, XML_ATTR_BUTTON_ACTION, BUTTON_ACTION_MORE_DETAILS);
            final int dialogMessageResId =
                    in.getAttributeInt(null, XML_ATTR_DIALOG_MESSAGE_RES_ID, ID_NULL);
            final String dialogMessage = XmlUtils.readStringAttribute(in, XML_ATTR_DIALOG_MESSAGE);

            if (iconId != ID_NULL) {
                dialogInfoBuilder.setIcon(iconId);
            }
            if (titleId != ID_NULL) {
                dialogInfoBuilder.setTitle(titleId);
            } else if (title != null) {
                dialogInfoBuilder.setTitle(title);
            }
            if (buttonTextId != ID_NULL) {
                dialogInfoBuilder.setNeutralButtonText(buttonTextId);
            } else if (buttonText != null) {
                dialogInfoBuilder.setNeutralButtonText(buttonText);
            }
            if (dialogMessageResId != ID_NULL) {
                dialogInfoBuilder.setMessage(dialogMessageResId);
            } else if (dialogMessage != null) {
                dialogInfoBuilder.setMessage(dialogMessage);
            }
            dialogInfoBuilder.setNeutralButtonAction(buttonAction);
        } catch (Exception e) {
            Slog.e(TAG, "Exception while parsing from xml. Some fields may default", e);
        }
        return dialogInfoBuilder.build();
    }

    @Override
    public int hashCode() {
        int hashCode = mIconResId;
        hashCode = 31 * hashCode + mTitleResId;
        hashCode = 31 * hashCode + Objects.hashCode(mTitle);
        hashCode = 31 * hashCode + mNeutralButtonTextResId;
        hashCode = 31 * hashCode + Objects.hashCode(mNeutralButtonText);
        hashCode = 31 * hashCode + mDialogMessageResId;
        hashCode = 31 * hashCode + Objects.hashCode(mDialogMessage);
        hashCode = 31 * hashCode + mNeutralButtonAction;
        return hashCode;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SuspendDialogInfo)) {
            return false;
        }
        final SuspendDialogInfo otherDialogInfo = (SuspendDialogInfo) obj;
        return mIconResId == otherDialogInfo.mIconResId
                && mTitleResId == otherDialogInfo.mTitleResId
                && Objects.equals(mTitle, otherDialogInfo.mTitle)
                && mDialogMessageResId == otherDialogInfo.mDialogMessageResId
                && Objects.equals(mDialogMessage, otherDialogInfo.mDialogMessage)
                && mNeutralButtonTextResId == otherDialogInfo.mNeutralButtonTextResId
                && Objects.equals(mNeutralButtonText, otherDialogInfo.mNeutralButtonText)
                && mNeutralButtonAction == otherDialogInfo.mNeutralButtonAction;
    }

    @NonNull
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("SuspendDialogInfo: {");
        if (mIconResId != ID_NULL) {
            builder.append("mIconId = 0x");
            builder.append(Integer.toHexString(mIconResId));
            builder.append(" ");
        }
        if (mTitleResId != ID_NULL) {
            builder.append("mTitleResId = 0x");
            builder.append(Integer.toHexString(mTitleResId));
            builder.append(" ");
        } else if (mTitle != null) {
            builder.append("mTitle = \"");
            builder.append(mTitle);
            builder.append("\"");
        }
        if (mNeutralButtonTextResId != ID_NULL) {
            builder.append("mNeutralButtonTextResId = 0x");
            builder.append(Integer.toHexString(mNeutralButtonTextResId));
            builder.append(" ");
        } else if (mNeutralButtonText != null) {
            builder.append("mNeutralButtonText = \"");
            builder.append(mNeutralButtonText);
            builder.append("\"");
        }
        if (mDialogMessageResId != ID_NULL) {
            builder.append("mDialogMessageResId = 0x");
            builder.append(Integer.toHexString(mDialogMessageResId));
            builder.append(" ");
        } else if (mDialogMessage != null) {
            builder.append("mDialogMessage = \"");
            builder.append(mDialogMessage);
            builder.append("\" ");
        }
        builder.append("mNeutralButtonAction = ");
        builder.append(mNeutralButtonAction);
        builder.append("}");
        return builder.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        dest.writeInt(mIconResId);
        dest.writeInt(mTitleResId);
        dest.writeString(mTitle);
        dest.writeInt(mDialogMessageResId);
        dest.writeString(mDialogMessage);
        dest.writeInt(mNeutralButtonTextResId);
        dest.writeString(mNeutralButtonText);
        dest.writeInt(mNeutralButtonAction);
    }

    private SuspendDialogInfo(Parcel source) {
        mIconResId = source.readInt();
        mTitleResId = source.readInt();
        mTitle = source.readString();
        mDialogMessageResId = source.readInt();
        mDialogMessage = source.readString();
        mNeutralButtonTextResId = source.readInt();
        mNeutralButtonText = source.readString();
        mNeutralButtonAction = source.readInt();
    }

    SuspendDialogInfo(Builder b) {
        mIconResId = b.mIconResId;
        mTitleResId = b.mTitleResId;
        mTitle = (mTitleResId == ID_NULL) ? b.mTitle : null;
        mDialogMessageResId = b.mDialogMessageResId;
        mDialogMessage = (mDialogMessageResId == ID_NULL) ? b.mDialogMessage : null;
        mNeutralButtonTextResId = b.mNeutralButtonTextResId;
        mNeutralButtonText = (mNeutralButtonTextResId == ID_NULL) ? b.mNeutralButtonText : null;
        mNeutralButtonAction = b.mNeutralButtonAction;
    }

    public static final @NonNull Creator<SuspendDialogInfo> CREATOR =
            new Creator<SuspendDialogInfo>() {
        @Override
        public SuspendDialogInfo createFromParcel(Parcel source) {
            return new SuspendDialogInfo(source);
        }

        @Override
        public SuspendDialogInfo[] newArray(int size) {
            return new SuspendDialogInfo[size];
        }
    };

    /**
     * Builder to build a {@link SuspendDialogInfo} object.
     */
    public static final class Builder {
        private int mDialogMessageResId = ID_NULL;
        private String mDialogMessage;
        private int mTitleResId = ID_NULL;
        private String mTitle;
        private int mIconResId = ID_NULL;
        private int mNeutralButtonTextResId = ID_NULL;
        private String mNeutralButtonText;
        private int mNeutralButtonAction = BUTTON_ACTION_MORE_DETAILS;

        /**
         * Set the resource id of the icon to be used. If not provided, no icon will be shown.
         *
         * @param resId The resource id of the icon.
         * @return this builder object.
         */
        @NonNull
        public Builder setIcon(@DrawableRes int resId) {
            Preconditions.checkArgument(ResourceId.isValid(resId), "Invalid resource id provided");
            mIconResId = resId;
            return this;
        }

        /**
         * Set the resource id of the title text to be displayed. If this is not provided, the
         * system will use a default title.
         *
         * @param resId The resource id of the title.
         * @return this builder object.
         */
        @NonNull
        public Builder setTitle(@StringRes int resId) {
            Preconditions.checkArgument(ResourceId.isValid(resId), "Invalid resource id provided");
            mTitleResId = resId;
            return this;
        }

        /**
         * Set the title text of the dialog. Ignored if a resource id is set via
         * {@link #setTitle(int)}
         *
         * @param title The title of the dialog.
         * @return this builder object.
         * @see #setTitle(int)
         */
        @NonNull
        public Builder setTitle(@NonNull String title) {
            Preconditions.checkStringNotEmpty(title, "Title cannot be null or empty");
            mTitle = title;
            return this;
        }

        /**
         * Set the text to show in the body of the dialog. Ignored if a resource id is set via
         * {@link #setMessage(int)}.
         * <p>
         * The system will use {@link String#format(Locale, String, Object...) String.format} to
         * insert the suspended app name into the message, so an example format string could be
         * {@code "The app %1$s is currently suspended"}. This is optional - if the string passed in
         * {@code message} does not accept an argument, it will be used as is.
         *
         * @param message The dialog message.
         * @return this builder object.
         * @see #setMessage(int)
         */
        @NonNull
        public Builder setMessage(@NonNull String message) {
            Preconditions.checkStringNotEmpty(message, "Message cannot be null or empty");
            mDialogMessage = message;
            return this;
        }

        /**
         * Set the resource id of the dialog message to be shown. If no dialog message is provided
         * via either this method or {@link #setMessage(String)}, the system will use a default
         * message.
         * <p>
         * The system will use {@link android.content.res.Resources#getString(int, Object...)
         * getString} to insert the suspended app name into the message, so an example format string
         * could be {@code "The app %1$s is currently suspended"}. This is optional - if the string
         * referred to by {@code resId} does not accept an argument, it will be used as is.
         *
         * @param resId The resource id of the dialog message.
         * @return this builder object.
         * @see #setMessage(String)
         */
        @NonNull
        public Builder setMessage(@StringRes int resId) {
            Preconditions.checkArgument(ResourceId.isValid(resId), "Invalid resource id provided");
            mDialogMessageResId = resId;
            return this;
        }

        /**
         * Set the resource id of text to be shown on the neutral button. Tapping this button would
         * perform the {@link ButtonAction action} specified through
         * {@link #setNeutralButtonAction(int)}. If this is not provided, the system will use a
         * default text.
         *
         * @param resId The resource id of the button text
         * @return this builder object.
         */
        @NonNull
        public Builder setNeutralButtonText(@StringRes int resId) {
            Preconditions.checkArgument(ResourceId.isValid(resId), "Invalid resource id provided");
            mNeutralButtonTextResId = resId;
            return this;
        }

        /**
         * Set the text to be shown on the neutral button. Ignored if a resource id is set via
         * {@link #setNeutralButtonText(int)}
         *
         * @param neutralButtonText The title of the dialog.
         * @return this builder object.
         * @see #setNeutralButtonText(int)
         */
        @NonNull
        public Builder setNeutralButtonText(@NonNull String neutralButtonText) {
            Preconditions.checkStringNotEmpty(neutralButtonText,
                    "Button text cannot be null or empty");
            mNeutralButtonText = neutralButtonText;
            return this;
        }

        /**
         * Set the action expected to happen on neutral button tap. Defaults to
         * {@link #BUTTON_ACTION_MORE_DETAILS} if this is not provided.
         *
         * @param buttonAction Either {@link #BUTTON_ACTION_MORE_DETAILS} or
         *                     {@link #BUTTON_ACTION_UNSUSPEND}.
         * @return this builder object
         */
        @NonNull
        public Builder setNeutralButtonAction(@ButtonAction int buttonAction) {
            Preconditions.checkArgument(buttonAction == BUTTON_ACTION_MORE_DETAILS
                    || buttonAction == BUTTON_ACTION_UNSUSPEND, "Invalid button action");
            mNeutralButtonAction = buttonAction;
            return this;
        }

        /**
         * Build the final object based on given inputs.
         *
         * @return The {@link SuspendDialogInfo} object built using this builder.
         */
        @NonNull
        public SuspendDialogInfo build() {
            return new SuspendDialogInfo(this);
        }
    }
}
