/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.am;

import static com.android.server.am.ActivityManagerDebugConfig.TAG_AM;
import static com.android.server.am.ActivityManagerDebugConfig.TAG_WITH_CLASS_NAME;
import static com.android.server.am.AppRestrictionController.DEVICE_CONFIG_SUBNAMESPACE_PREFIX;
import static com.android.server.am.BaseAppStateTracker.ONE_DAY;

import android.annotation.NonNull;
import android.app.ActivityManagerInternal.BindServiceEventListener;
import android.content.Context;
import android.os.AppBackgroundRestrictionsInfo;
import android.os.SystemClock;
import android.util.proto.ProtoOutputStream;

import com.android.server.am.AppBindServiceEventsTracker.AppBindServiceEventsPolicy;
import com.android.server.am.AppRestrictionController.TrackerType;
import com.android.server.am.BaseAppStateTimeSlotEventsTracker.SimpleAppStateTimeslotEvents;
import com.android.server.am.BaseAppStateTracker.Injector;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;

final class AppBindServiceEventsTracker extends BaseAppStateTimeSlotEventsTracker
        <AppBindServiceEventsPolicy, SimpleAppStateTimeslotEvents>
        implements BindServiceEventListener {

    static final String TAG = TAG_WITH_CLASS_NAME ? "AppBindServiceEventsTracker" : TAG_AM;

    static final boolean DEBUG_APP_STATE_BIND_SERVICE_EVENT_TRACKER = false;

    AppBindServiceEventsTracker(Context context, AppRestrictionController controller) {
        this(context, controller, null, null);
    }

    AppBindServiceEventsTracker(Context context, AppRestrictionController controller,
            Constructor<? extends Injector<AppBindServiceEventsPolicy>> injector,
            Object outerContext) {
        super(context, controller, injector, outerContext);
        mInjector.setPolicy(new AppBindServiceEventsPolicy(mInjector, this));
    }

    @Override
    public void onBindingService(String packageName, int uid) {
        if (mInjector.getPolicy().isEnabled()) {
            onNewEvent(packageName, uid);
        }
    }

    @Override
    @TrackerType int getType() {
        return AppRestrictionController.TRACKER_TYPE_BIND_SERVICE_EVENTS;
    }

    @Override
    void onSystemReady() {
        super.onSystemReady();
        mInjector.getActivityManagerInternal().addBindServiceEventListener(this);
    }

    @Override
    public SimpleAppStateTimeslotEvents createAppStateEvents(int uid, String packageName) {
        return new SimpleAppStateTimeslotEvents(uid, packageName,
                mInjector.getPolicy().getTimeSlotSize(), TAG, mInjector.getPolicy());
    }

    @Override
    public SimpleAppStateTimeslotEvents createAppStateEvents(SimpleAppStateTimeslotEvents other) {
        return new SimpleAppStateTimeslotEvents(other);
    }

    @Override
    byte[] getTrackerInfoForStatsd(int uid) {
        final long now = SystemClock.elapsedRealtime();
        final int numOfBindRequests = getTotalEventsLocked(uid, now);
        if (numOfBindRequests == 0) {
            // Not interested.
            return null;
        }
        final ProtoOutputStream proto = new ProtoOutputStream();
        proto.write(
                AppBackgroundRestrictionsInfo.BindServiceEventsTrackerInfo.BIND_SERVICE_REQUESTS,
                numOfBindRequests);
        proto.flush();
        return proto.getBytes();
    }

    @Override
    void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.println("APP BIND SERVICE EVENT TRACKER:");
        super.dump(pw, "  " + prefix);
    }

    static final class AppBindServiceEventsPolicy
            extends BaseAppStateTimeSlotEventsPolicy<AppBindServiceEventsTracker> {
        /**
         * Whether or not we should enable the monitoring on abusive service bindings requests.
         */
        static final String KEY_BG_BIND_SVC_MONITOR_ENABLED =
                DEVICE_CONFIG_SUBNAMESPACE_PREFIX + "bind_svc_monitor_enabled";

        /**
         * The size of the sliding window in which the number of service binding requests is checked
         * against the threshold.
         */
        static final String KEY_BG_BIND_SVC_WINDOW =
                DEVICE_CONFIG_SUBNAMESPACE_PREFIX + "bind_svc_window";

        /**
         * The threshold at where the number of service binding requests are considered as
         * "excessive" within the given window.
         */
        static final String KEY_BG_EX_BIND_SVC_THRESHOLD =
                DEVICE_CONFIG_SUBNAMESPACE_PREFIX + "ex_bind_svc_threshold";

        /**
         * Default value to {@link #mTrackerEnabled}.
         */
        static final boolean DEFAULT_BG_BIND_SVC_MONITOR_ENABLED = true;

        /**
         * Default value to {@link #mMaxTrackingDuration}.
         */
        static final long DEFAULT_BG_BIND_SVC_WINDOW = ONE_DAY;

        /**
         * Default value to {@link #mNumOfEventsThreshold}.
         */
        static final int DEFAULT_BG_EX_BIND_SVC_THRESHOLD = 10_000;

        AppBindServiceEventsPolicy(@NonNull Injector injector,
                @NonNull AppBindServiceEventsTracker tracker) {
            super(injector, tracker,
                    KEY_BG_BIND_SVC_MONITOR_ENABLED, DEFAULT_BG_BIND_SVC_MONITOR_ENABLED,
                    KEY_BG_BIND_SVC_WINDOW, DEFAULT_BG_BIND_SVC_WINDOW,
                    KEY_BG_EX_BIND_SVC_THRESHOLD, DEFAULT_BG_EX_BIND_SVC_THRESHOLD);
        }

        @Override
        String getEventName() {
            return "bindservice";
        }

        @Override
        void dump(PrintWriter pw, String prefix) {
            pw.print(prefix);
            pw.println("APP BIND SERVICE EVENT TRACKER POLICY SETTINGS:");
            super.dump(pw, "  " + prefix);
        }
    }
}
