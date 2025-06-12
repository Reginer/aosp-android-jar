/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.adservices.common;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;

import com.android.adservices.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents the notification type for notifications sent by AdServices to notify users of updates
 * to PPAPI-related technology on their device.
 *
 * <p>Can be {@link #NOTIFICATION_NONE}, {@link #NOTIFICATION_ONGOING}, or {@link
 * #NOTIFICATION_REGULAR}
 *
 * @hide
 */
@FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
public final class NotificationType {
    /** Don't show any notification during the enrollment. */
    public static final int NOTIFICATION_NONE = AdServicesCommonManager.NOTIFICATION_NONE;

    /** Shows ongoing notification during the enrollment, which user can not dismiss. */
    public static final int NOTIFICATION_ONGOING = AdServicesCommonManager.NOTIFICATION_ONGOING;

    /** Shows regular notification during the enrollment, which user can dismiss. */
    public static final int NOTIFICATION_REGULAR = AdServicesCommonManager.NOTIFICATION_REGULAR;

    /** Default Contractor, make it private so that it won't show in the system-current.txt */
    private NotificationType() {}

    /**
     * Result codes that are common across various APIs.
     *
     * @hide
     */
    @IntDef(value = {NOTIFICATION_NONE, NOTIFICATION_ONGOING, NOTIFICATION_REGULAR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface NotificationTypeCode {}
}
