/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.server.pm;

import static android.content.Intent.FLAG_ACTIVITY_MATCH_EXTERNAL;

import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.ACTION_INSTANT_APP_RESOLUTION_PHASE_ONE;
import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.ACTION_INSTANT_APP_RESOLUTION_PHASE_TWO;
import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.FIELD_INSTANT_APP_LAUNCH_TOKEN;
import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.FIELD_INSTANT_APP_RESOLUTION_DELAY_MS;
import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.FIELD_INSTANT_APP_RESOLUTION_STATUS;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.AuxiliaryResolveInfo;
import android.content.pm.InstantAppIntentFilter;
import android.content.pm.InstantAppRequest;
import android.content.pm.InstantAppRequestInfo;
import android.content.pm.InstantAppResolveInfo;
import android.content.pm.InstantAppResolveInfo.InstantAppDigest;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;
import com.android.server.pm.InstantAppResolverConnection.ConnectionException;
import com.android.server.pm.InstantAppResolverConnection.PhaseTwoCallback;
import com.android.server.pm.resolution.ComponentResolver;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** @hide */
public abstract class InstantAppResolver {
    private static final boolean DEBUG_INSTANT = Build.IS_DEBUGGABLE;
    private static final String TAG = "PackageManager";

    private static final int RESOLUTION_SUCCESS = 0;
    private static final int RESOLUTION_FAILURE = 1;
    /** Binding to the external service timed out */
    private static final int RESOLUTION_BIND_TIMEOUT = 2;
    /** The call to retrieve an instant application response timed out */
    private static final int RESOLUTION_CALL_TIMEOUT = 3;

    @IntDef(flag = true, prefix = { "RESOLUTION_" }, value = {
            RESOLUTION_SUCCESS,
            RESOLUTION_FAILURE,
            RESOLUTION_BIND_TIMEOUT,
            RESOLUTION_CALL_TIMEOUT,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ResolutionStatus {}

    private static MetricsLogger sMetricsLogger;

    private static MetricsLogger getLogger() {
        if (sMetricsLogger == null) {
            sMetricsLogger = new MetricsLogger();
        }
        return sMetricsLogger;
    }

    /**
     * Returns an intent with potential PII removed from the original intent. Fields removed
     * include extras and the host + path of the data, if defined.
     */
    public static Intent sanitizeIntent(Intent origIntent) {
        final Intent sanitizedIntent;
        sanitizedIntent = new Intent(origIntent.getAction());
        Set<String> categories = origIntent.getCategories();
        if (categories != null) {
            for (String category : categories) {
                sanitizedIntent.addCategory(category);
            }
        }
        Uri sanitizedUri = origIntent.getData() == null
                ? null
                : Uri.fromParts(origIntent.getScheme(), "", "");
        sanitizedIntent.setDataAndType(sanitizedUri, origIntent.getType());
        sanitizedIntent.addFlags(origIntent.getFlags());
        sanitizedIntent.setPackage(origIntent.getPackage());
        return sanitizedIntent;
    }

    /**
     * Generate an {@link InstantAppDigest} from an {@link Intent} which contains hashes of the
     * host. The object contains both secure and insecure hash array variants, and the secure
     * version must be passed along to ensure the random data is consistent.
     */
    @NonNull
    public static InstantAppDigest parseDigest(@NonNull Intent origIntent) {
        if (origIntent.getData() != null && !TextUtils.isEmpty(origIntent.getData().getHost())) {
            return new InstantAppResolveInfo.InstantAppDigest(origIntent.getData().getHost(),
                    5 /*maxDigests*/);
        } else {
            return InstantAppResolveInfo.InstantAppDigest.UNDEFINED;
        }
    }

    public static AuxiliaryResolveInfo doInstantAppResolutionPhaseOne(@NonNull Computer computer,
            @NonNull UserManagerService userManager, InstantAppResolverConnection connection,
            InstantAppRequest requestObj) {
        final long startTime = System.currentTimeMillis();
        final String token = requestObj.token;
        if (DEBUG_INSTANT) {
            Log.d(TAG, "[" + token + "] Phase1; resolving");
        }

        AuxiliaryResolveInfo resolveInfo = null;
        @ResolutionStatus int resolutionStatus = RESOLUTION_SUCCESS;
        Intent origIntent = requestObj.origIntent;
        try {
            final List<InstantAppResolveInfo> instantAppResolveInfoList =
                    connection.getInstantAppResolveInfoList(buildRequestInfo(requestObj));
            if (instantAppResolveInfoList != null && instantAppResolveInfoList.size() > 0) {
                resolveInfo = InstantAppResolver.filterInstantAppIntent(computer, userManager,
                        instantAppResolveInfoList, origIntent, requestObj.resolvedType,
                        requestObj.userId, origIntent.getPackage(), token,
                        requestObj.hostDigestPrefixSecure);
            }
        } catch (ConnectionException e) {
            if (e.failure == ConnectionException.FAILURE_BIND) {
                resolutionStatus = RESOLUTION_BIND_TIMEOUT;
            } else if (e.failure == ConnectionException.FAILURE_CALL) {
                resolutionStatus = RESOLUTION_CALL_TIMEOUT;
            } else {
                resolutionStatus = RESOLUTION_FAILURE;
            }
        }
        // Only log successful instant application resolution
        if (requestObj.resolveForStart && resolutionStatus == RESOLUTION_SUCCESS) {
            logMetrics(ACTION_INSTANT_APP_RESOLUTION_PHASE_ONE, startTime, token,
                    resolutionStatus);
        }
        if (DEBUG_INSTANT && resolveInfo == null) {
            if (resolutionStatus == RESOLUTION_BIND_TIMEOUT) {
                Log.d(TAG, "[" + token + "] Phase1; bind timed out");
            } else if (resolutionStatus == RESOLUTION_CALL_TIMEOUT) {
                Log.d(TAG, "[" + token + "] Phase1; call timed out");
            } else if (resolutionStatus != RESOLUTION_SUCCESS) {
                Log.d(TAG, "[" + token + "] Phase1; service connection error");
            } else {
                Log.d(TAG, "[" + token + "] Phase1; No results matched");
            }
        }
        // if the match external flag is set, return an empty resolve info instead of a null result.
        if (resolveInfo == null && (origIntent.getFlags() & FLAG_ACTIVITY_MATCH_EXTERNAL) != 0) {
            return new AuxiliaryResolveInfo(token, false, createFailureIntent(origIntent, token),
                    null /* filters */, requestObj.hostDigestPrefixSecure);
        }
        return resolveInfo;
    }

    public static void doInstantAppResolutionPhaseTwo(Context context, @NonNull Computer computer,
            @NonNull UserManagerService userManager, InstantAppResolverConnection connection,
            InstantAppRequest requestObj, ActivityInfo instantAppInstaller,
            Handler callbackHandler) {
        final long startTime = System.currentTimeMillis();
        final String token = requestObj.token;
        if (DEBUG_INSTANT) {
            Log.d(TAG, "[" + token + "] Phase2; resolving");
        }
        final Intent origIntent = requestObj.origIntent;
        final Intent sanitizedIntent = sanitizeIntent(origIntent);

        final PhaseTwoCallback callback = new PhaseTwoCallback() {
            @Override
            void onPhaseTwoResolved(List<InstantAppResolveInfo> instantAppResolveInfoList,
                    long startTime) {
                final Intent failureIntent;
                if (instantAppResolveInfoList != null && instantAppResolveInfoList.size() > 0) {
                    final AuxiliaryResolveInfo instantAppIntentInfo =
                            InstantAppResolver.filterInstantAppIntent(computer, userManager,
                                    instantAppResolveInfoList, origIntent, null /*resolvedType*/,
                                    0 /*userId*/, origIntent.getPackage(),
                                    token, requestObj.hostDigestPrefixSecure);
                    if (instantAppIntentInfo != null) {
                        failureIntent = instantAppIntentInfo.failureIntent;
                    } else {
                        failureIntent = null;
                    }
                } else {
                    failureIntent = null;
                }
                final Intent installerIntent = buildEphemeralInstallerIntent(
                        requestObj.origIntent,
                        sanitizedIntent,
                        failureIntent,
                        requestObj.callingPackage,
                        requestObj.callingFeatureId,
                        requestObj.verificationBundle,
                        requestObj.resolvedType,
                        requestObj.userId,
                        requestObj.responseObj.installFailureActivity,
                        token,
                        false /*needsPhaseTwo*/,
                        requestObj.responseObj.filters);
                installerIntent.setComponent(new ComponentName(
                        instantAppInstaller.packageName, instantAppInstaller.name));

                logMetrics(ACTION_INSTANT_APP_RESOLUTION_PHASE_TWO, startTime, token,
                        requestObj.responseObj.filters != null ? RESOLUTION_SUCCESS : RESOLUTION_FAILURE);

                context.startActivity(installerIntent);
            }
        };
        try {
            connection.getInstantAppIntentFilterList(buildRequestInfo(requestObj), callback,
                    callbackHandler, startTime);
        } catch (ConnectionException e) {
            @ResolutionStatus int resolutionStatus = RESOLUTION_FAILURE;
            if (e.failure == ConnectionException.FAILURE_BIND) {
                resolutionStatus = RESOLUTION_BIND_TIMEOUT;
            }
            logMetrics(ACTION_INSTANT_APP_RESOLUTION_PHASE_TWO, startTime, token,
                    resolutionStatus);
            if (DEBUG_INSTANT) {
                if (resolutionStatus == RESOLUTION_BIND_TIMEOUT) {
                    Log.d(TAG, "[" + token + "] Phase2; bind timed out");
                } else {
                    Log.d(TAG, "[" + token + "] Phase2; service connection error");
                }
            }
        }
    }

    /**
     * Builds and returns an intent to launch the instant installer.
     */
    public static Intent buildEphemeralInstallerIntent(
            @NonNull Intent origIntent,
            @NonNull Intent sanitizedIntent,
            @Nullable Intent failureIntent,
            @NonNull String callingPackage,
            @Nullable String callingFeatureId,
            @Nullable Bundle verificationBundle,
            @NonNull String resolvedType,
            int userId,
            @Nullable ComponentName installFailureActivity,
            @Nullable String token,
            boolean needsPhaseTwo,
            List<AuxiliaryResolveInfo.AuxiliaryFilter> filters) {
        // Construct the intent that launches the instant installer
        int flags = origIntent.getFlags();
        final Intent intent = new Intent();
        intent.setFlags(flags
                | Intent.FLAG_ACTIVITY_NO_HISTORY
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        if (token != null) {
            intent.putExtra(Intent.EXTRA_INSTANT_APP_TOKEN, token);
        }
        if (origIntent.getData() != null) {
            intent.putExtra(Intent.EXTRA_INSTANT_APP_HOSTNAME, origIntent.getData().getHost());
        }
        intent.putExtra(Intent.EXTRA_INSTANT_APP_ACTION, origIntent.getAction());
        intent.putExtra(Intent.EXTRA_INTENT, sanitizedIntent);

        if (needsPhaseTwo) {
            intent.setAction(Intent.ACTION_RESOLVE_INSTANT_APP_PACKAGE);
        } else {
            ActivityOptions options = ActivityOptions.makeBasic()
                    .setPendingIntentCreatorBackgroundActivityStartMode(
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
            // We have all of the data we need; just start the installer without a second phase
            if (failureIntent != null || installFailureActivity != null) {
                // Intent that is launched if the package couldn't be installed for any reason.
                try {
                    final Intent onFailureIntent;
                    if (installFailureActivity != null) {
                        onFailureIntent = new Intent();
                        onFailureIntent.setComponent(installFailureActivity);
                        if (filters != null && filters.size() == 1) {
                            onFailureIntent.putExtra(Intent.EXTRA_SPLIT_NAME,
                                    filters.get(0).splitName);
                        }
                        onFailureIntent.putExtra(Intent.EXTRA_INTENT, origIntent);
                    } else {
                        onFailureIntent = failureIntent;
                    }
                    final IIntentSender failureIntentTarget = ActivityManager.getService()
                            .getIntentSenderWithFeature(
                                    ActivityManager.INTENT_SENDER_ACTIVITY, callingPackage,
                                    callingFeatureId, null /*token*/, null /*resultWho*/,
                                    1 /*requestCode*/,
                                    new Intent[] { onFailureIntent },
                                    new String[] { resolvedType },
                                    PendingIntent.FLAG_CANCEL_CURRENT
                                            | PendingIntent.FLAG_ONE_SHOT
                                            | PendingIntent.FLAG_IMMUTABLE,
                                    options.toBundle(), userId);
                    IntentSender failureSender = new IntentSender(failureIntentTarget);
                    // TODO(b/72700831): remove populating old extra
                    intent.putExtra(Intent.EXTRA_INSTANT_APP_FAILURE, failureSender);
                } catch (RemoteException ignore) { /* ignore; same process */ }
            }

            // Intent that is launched if the package was installed successfully.
            final Intent successIntent = new Intent(origIntent);
            successIntent.setLaunchToken(token);
            try {
                final IIntentSender successIntentTarget = ActivityManager.getService()
                        .getIntentSenderWithFeature(
                                ActivityManager.INTENT_SENDER_ACTIVITY, callingPackage,
                                callingFeatureId, null /*token*/, null /*resultWho*/,
                                0 /*requestCode*/,
                                new Intent[] { successIntent },
                                new String[] { resolvedType },
                                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT
                                        | PendingIntent.FLAG_IMMUTABLE,
                                options.toBundle(), userId);
                IntentSender successSender = new IntentSender(successIntentTarget);
                intent.putExtra(Intent.EXTRA_INSTANT_APP_SUCCESS, successSender);
            } catch (RemoteException ignore) { /* ignore; same process */ }
            if (verificationBundle != null) {
                intent.putExtra(Intent.EXTRA_VERIFICATION_BUNDLE, verificationBundle);
            }
            intent.putExtra(Intent.EXTRA_CALLING_PACKAGE, callingPackage);

            if (filters != null) {
                Bundle resolvableFilters[] = new Bundle[filters.size()];
                for (int i = 0, max = filters.size(); i < max; i++) {
                    Bundle resolvableFilter = new Bundle();
                    AuxiliaryResolveInfo.AuxiliaryFilter filter = filters.get(i);
                    resolvableFilter.putBoolean(Intent.EXTRA_UNKNOWN_INSTANT_APP,
                            filter.resolveInfo != null
                                    && filter.resolveInfo.shouldLetInstallerDecide());
                    resolvableFilter.putString(Intent.EXTRA_PACKAGE_NAME, filter.packageName);
                    resolvableFilter.putString(Intent.EXTRA_SPLIT_NAME, filter.splitName);
                    resolvableFilter.putLong(Intent.EXTRA_LONG_VERSION_CODE, filter.versionCode);
                    resolvableFilter.putBundle(Intent.EXTRA_INSTANT_APP_EXTRAS, filter.extras);
                    resolvableFilters[i] = resolvableFilter;
                    if (i == 0) {
                        // for backwards compat, always set the first result on the intent and add
                        // the int version code
                        intent.putExtras(resolvableFilter);
                        intent.putExtra(Intent.EXTRA_VERSION_CODE, (int) filter.versionCode);
                    }
                }
                intent.putExtra(Intent.EXTRA_INSTANT_APP_BUNDLES, resolvableFilters);
            }
            intent.setAction(Intent.ACTION_INSTALL_INSTANT_APP_PACKAGE);
        }
        return intent;
    }

    private static InstantAppRequestInfo buildRequestInfo(InstantAppRequest request) {
        return new InstantAppRequestInfo(
                sanitizeIntent(request.origIntent),
                // This must only expose the secured version of the host
                request.hostDigestPrefixSecure,
                UserHandle.of(request.userId),
                request.isRequesterInstantApp,
                request.token
        );
    }

    private static AuxiliaryResolveInfo filterInstantAppIntent(@NonNull Computer computer,
            @NonNull UserManagerService userManager,
            List<InstantAppResolveInfo> instantAppResolveInfoList, Intent origIntent,
            String resolvedType, int userId, String packageName, String token,
            int[] hostDigestPrefixSecure) {
        InstantAppDigest digest = InstantAppResolver.parseDigest(origIntent);
        final int[] shaPrefix = digest.getDigestPrefix();
        final byte[][] digestBytes = digest.getDigestBytes();
        boolean requiresSecondPhase = false;
        ArrayList<AuxiliaryResolveInfo.AuxiliaryFilter> filters = null;
        boolean requiresPrefixMatch = origIntent.isWebIntent() || (shaPrefix.length > 0
                        && (origIntent.getFlags() & Intent.FLAG_ACTIVITY_MATCH_EXTERNAL) == 0);
        for (InstantAppResolveInfo instantAppResolveInfo : instantAppResolveInfoList) {
            if (requiresPrefixMatch && instantAppResolveInfo.shouldLetInstallerDecide()) {
                Slog.d(TAG, "InstantAppResolveInfo with mShouldLetInstallerDecide=true when digest"
                        + " required; ignoring");
                continue;
            }
            byte[] filterDigestBytes = instantAppResolveInfo.getDigestBytes();
            // Only include matching digests if we have a prefix and we're either dealing with a
            // prefixed request or the resolveInfo specifies digest details.
            if (shaPrefix.length > 0 && (requiresPrefixMatch || filterDigestBytes.length > 0)) {
                boolean matchFound = false;
                // Go in reverse order so we match the narrowest scope first.
                for (int i = shaPrefix.length - 1; i >= 0; --i) {
                    if (Arrays.equals(digestBytes[i], filterDigestBytes)) {
                        matchFound = true;
                        break;
                    }
                }
                if (!matchFound) {
                    continue;
                }
            }
            // We matched a resolve info; resolve the filters to see if anything matches completely.
            List<AuxiliaryResolveInfo.AuxiliaryFilter> matchFilters = computeResolveFilters(
                    computer, userManager, origIntent, resolvedType, userId, packageName, token,
                    instantAppResolveInfo);
            if (matchFilters != null) {
                if (matchFilters.isEmpty()) {
                    requiresSecondPhase = true;
                }
                if (filters == null) {
                    filters = new ArrayList<>(matchFilters);
                } else {
                    filters.addAll(matchFilters);
                }
            }
        }
        if (filters != null && !filters.isEmpty()) {
            return new AuxiliaryResolveInfo(token, requiresSecondPhase,
                    createFailureIntent(origIntent, token), filters, hostDigestPrefixSecure);
        }
        // Hash or filter mis-match; no instant apps for this domain.
        return null;
    }

    /**
     * Creates a failure intent for the installer to send in the case that the instant app cannot be
     * launched for any reason.
     */
    private static Intent createFailureIntent(Intent origIntent, String token) {
        final Intent failureIntent = new Intent(origIntent);
        failureIntent.setFlags(failureIntent.getFlags() | Intent.FLAG_IGNORE_EPHEMERAL);
        failureIntent.setFlags(failureIntent.getFlags() & ~Intent.FLAG_ACTIVITY_MATCH_EXTERNAL);
        failureIntent.setLaunchToken(token);
        return failureIntent;
    }

    /**
     * Returns one of three states: <p/>
     * <ul>
     *     <li>{@code null} if there are no matches will not be; resolution is unnecessary.</li>
     *     <li>An empty list signifying that a 2nd phase of resolution is required.</li>
     *     <li>A populated list meaning that matches were found and should be sent directly to the
     *     installer</li>
     * </ul>
     *
     */
    private static List<AuxiliaryResolveInfo.AuxiliaryFilter> computeResolveFilters(
            @NonNull Computer computer, @NonNull UserManagerService userManager, Intent origIntent,
            String resolvedType, int userId, String packageName, String token,
            InstantAppResolveInfo instantAppInfo) {
        if (instantAppInfo.shouldLetInstallerDecide()) {
            return Collections.singletonList(
                    new AuxiliaryResolveInfo.AuxiliaryFilter(
                            instantAppInfo, null /* splitName */,
                            instantAppInfo.getExtras()));
        }
        if (packageName != null
                && !packageName.equals(instantAppInfo.getPackageName())) {
            return null;
        }
        final List<InstantAppIntentFilter> instantAppFilters =
                instantAppInfo.getIntentFilters();
        if (instantAppFilters == null || instantAppFilters.isEmpty()) {
            // No filters on web intent; no matches, 2nd phase unnecessary.
            if (origIntent.isWebIntent()) {
                return null;
            }
            // No filters; we need to start phase two
            if (DEBUG_INSTANT) {
                Log.d(TAG, "No app filters; go to phase 2");
            }
            return Collections.emptyList();
        }
        final ComponentResolver.InstantAppIntentResolver instantAppResolver =
                new ComponentResolver.InstantAppIntentResolver(userManager);
        for (int j = instantAppFilters.size() - 1; j >= 0; --j) {
            final InstantAppIntentFilter instantAppFilter = instantAppFilters.get(j);
            final List<IntentFilter> splitFilters = instantAppFilter.getFilters();
            if (splitFilters == null || splitFilters.isEmpty()) {
                continue;
            }
            for (int k = splitFilters.size() - 1; k >= 0; --k) {
                IntentFilter filter = splitFilters.get(k);
                Iterator<IntentFilter.AuthorityEntry> authorities =
                        filter.authoritiesIterator();
                // ignore http/s-only filters.
                if ((authorities == null || !authorities.hasNext())
                        && (filter.hasDataScheme("http") || filter.hasDataScheme("https"))
                        && filter.hasAction(Intent.ACTION_VIEW)
                        && filter.hasCategory(Intent.CATEGORY_BROWSABLE)) {
                    continue;
                }
                instantAppResolver.addFilter(computer,
                        new AuxiliaryResolveInfo.AuxiliaryFilter(
                                filter,
                                instantAppInfo,
                                instantAppFilter.getSplitName(),
                                instantAppInfo.getExtras()
                        ));
            }
        }
        List<AuxiliaryResolveInfo.AuxiliaryFilter> matchedResolveInfoList =
                instantAppResolver.queryIntent(computer, origIntent, resolvedType,
                        false /*defaultOnly*/, userId);
        if (!matchedResolveInfoList.isEmpty()) {
            if (DEBUG_INSTANT) {
                Log.d(TAG, "[" + token + "] Found match(es); " + matchedResolveInfoList);
            }
            return matchedResolveInfoList;
        } else if (DEBUG_INSTANT) {
            Log.d(TAG, "[" + token + "] No matches found"
                    + " package: " + instantAppInfo.getPackageName()
                    + ", versionCode: " + instantAppInfo.getVersionCode());
        }
        return null;
    }

    private static void logMetrics(int action, long startTime, String token,
            @ResolutionStatus int status) {
        final LogMaker logMaker = new LogMaker(action)
                .setType(MetricsProto.MetricsEvent.TYPE_ACTION)
                .addTaggedData(FIELD_INSTANT_APP_RESOLUTION_DELAY_MS,
                        new Long(System.currentTimeMillis() - startTime))
                .addTaggedData(FIELD_INSTANT_APP_LAUNCH_TOKEN, token)
                .addTaggedData(FIELD_INSTANT_APP_RESOLUTION_STATUS, new Integer(status));
        getLogger().write(logMaker);
    }
}
