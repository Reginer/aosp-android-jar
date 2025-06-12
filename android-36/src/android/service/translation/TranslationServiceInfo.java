/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.service.translation;

import android.Manifest;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;

import com.android.internal.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * {@link ServiceInfo} and meta-data about an {@link TranslationService}.
 *
 * @hide
 */
public final class TranslationServiceInfo {

    private static final String TAG = "TranslationServiceInfo";
    private static final String XML_TAG_SERVICE = "translation-service";

    @NonNull
    private final ServiceInfo mServiceInfo;

    @Nullable
    private final String mSettingsActivity;

    private static ServiceInfo getServiceInfoOrThrow(ComponentName comp, boolean isTemp,
            @UserIdInt int userId) throws PackageManager.NameNotFoundException {
        int flags = PackageManager.GET_META_DATA;
        if (!isTemp) {
            flags |= PackageManager.MATCH_SYSTEM_ONLY;
        }

        ServiceInfo si = null;
        try {
            si = AppGlobals.getPackageManager().getServiceInfo(comp, flags, userId);
        } catch (RemoteException e) {
        }
        if (si == null) {
            throw new NameNotFoundException("Could not get serviceInfo for "
                    + (isTemp ? " (temp)" : "(default system)")
                    + " " + comp.flattenToShortString());
        }
        return si;
    }

    @NonNull
    public ServiceInfo getServiceInfo() {
        return mServiceInfo;
    }

    @Nullable
    public String getSettingsActivity() {
        return mSettingsActivity;
    }

    public TranslationServiceInfo(@NonNull Context context, @NonNull ComponentName comp,
            boolean isTemporaryService, @UserIdInt int userId)
            throws PackageManager.NameNotFoundException {
        this(context, getServiceInfoOrThrow(comp, isTemporaryService, userId));
    }

    private TranslationServiceInfo(@NonNull Context context, @NonNull ServiceInfo si) {
        // Check for permission.
        if (!Manifest.permission.BIND_TRANSLATION_SERVICE.equals(si.permission)) {
            Slog.w(TAG, "TranslationServiceInfo from '" + si.packageName
                    + "' does not require permission "
                    + Manifest.permission.BIND_TRANSLATION_SERVICE);
            throw new SecurityException("Service does not require permission "
                    + Manifest.permission.BIND_TRANSLATION_SERVICE);
        }

        mServiceInfo = si;

        // Get the metadata, if declared.
        // TODO: Try to find more easier way to do this.
        final XmlResourceParser parser = si.loadXmlMetaData(context.getPackageManager(),
                TranslationService.SERVICE_META_DATA);
        if (parser == null) {
            mSettingsActivity = null;
            return;
        }

        String settingsActivity = null;

        try {
            final Resources resources = context.getPackageManager().getResourcesForApplication(
                    si.applicationInfo);

            int type = 0;
            while (type != XmlPullParser.END_DOCUMENT && type != XmlPullParser.START_TAG) {
                type = parser.next();
            }

            if (XML_TAG_SERVICE.equals(parser.getName())) {
                final AttributeSet allAttributes = Xml.asAttributeSet(parser);
                TypedArray afsAttributes = null;
                try {
                    afsAttributes = resources.obtainAttributes(allAttributes,
                            com.android.internal.R.styleable.TranslationService);
                    settingsActivity = afsAttributes.getString(
                            R.styleable.ContentCaptureService_settingsActivity);
                } finally {
                    if (afsAttributes != null) {
                        afsAttributes.recycle();
                    }
                }
            } else {
                Log.e(TAG, "Meta-data does not start with translation-service tag");
            }
        } catch (PackageManager.NameNotFoundException | IOException | XmlPullParserException e) {
            Log.e(TAG, "Error parsing auto fill service meta-data", e);
        }

        mSettingsActivity = settingsActivity;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append("[").append(mServiceInfo);
        builder.append(", settings:").append(mSettingsActivity);
        return builder.toString();
    }

    /**
     * Dumps the service information.
     */
    public void dump(@NonNull String prefix, @NonNull PrintWriter pw) {
        pw.print(prefix);
        pw.print("Component: ");
        pw.println(getServiceInfo().getComponentName());
        pw.print(prefix);
        pw.print("Settings: ");
        pw.println(mSettingsActivity);
    }
}
