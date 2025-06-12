/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.ComponentName;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.util.Slog;

import com.android.internal.util.XmlUtils;
import com.android.modules.utils.TypedXmlPullParser;
import com.android.modules.utils.TypedXmlSerializer;
import com.android.server.LocalServices;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.PackageUserStateInternal;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class PreferredComponent {
    private static final String TAG_SET = "set";
    private static final String ATTR_ALWAYS = "always"; // boolean
    private static final String ATTR_MATCH = "match"; // number
    private static final String ATTR_NAME = "name"; // component name
    private static final String ATTR_SET = "set"; // number

    public final int mMatch;
    public final ComponentName mComponent;
    // Whether this is to be the one that's always chosen. If false, it's the most recently chosen.
    public final boolean mAlways;

    final String[] mSetPackages;
    final String[] mSetClasses;
    final String[] mSetComponents;
    final String mShortComponent;
    private String mParseError;

    private final Callbacks mCallbacks;

    public interface Callbacks {
        public boolean onReadTag(String tagName, TypedXmlPullParser parser)
                throws XmlPullParserException, IOException;
    }

    public PreferredComponent(Callbacks callbacks, int match, ComponentName[] set,
            ComponentName component, boolean always) {
        mCallbacks = callbacks;
        mMatch = match&IntentFilter.MATCH_CATEGORY_MASK;
        mComponent = component;
        mAlways = always;
        mShortComponent = component.flattenToShortString();
        mParseError = null;
        if (set != null) {
            final int N = set.length;
            String[] myPackages = new String[N];
            String[] myClasses = new String[N];
            String[] myComponents = new String[N];
            for (int i=0; i<N; i++) {
                ComponentName cn = set[i];
                if (cn == null) {
                    mSetPackages = null;
                    mSetClasses = null;
                    mSetComponents = null;
                    return;
                }
                myPackages[i] = cn.getPackageName().intern();
                myClasses[i] = cn.getClassName().intern();
                myComponents[i] = cn.flattenToShortString();
            }
            mSetPackages = myPackages;
            mSetClasses = myClasses;
            mSetComponents = myComponents;
        } else {
            mSetPackages = null;
            mSetClasses = null;
            mSetComponents = null;
        }
    }

    public PreferredComponent(Callbacks callbacks, TypedXmlPullParser parser)
            throws XmlPullParserException, IOException {
        mCallbacks = callbacks;
        mShortComponent = parser.getAttributeValue(null, ATTR_NAME);
        mComponent = ComponentName.unflattenFromString(mShortComponent);
        if (mComponent == null) {
            mParseError = "Bad activity name " + mShortComponent;
        }
        mMatch = parser.getAttributeIntHex(null, ATTR_MATCH, 0);
        int setCount = parser.getAttributeInt(null, ATTR_SET, 0);
        mAlways = parser.getAttributeBoolean(null, ATTR_ALWAYS, true);

        String[] myPackages = setCount > 0 ? new String[setCount] : null;
        String[] myClasses = setCount > 0 ? new String[setCount] : null;
        String[] myComponents = setCount > 0 ? new String[setCount] : null;

        int setPos = 0;

        int outerDepth = parser.getDepth();
        int type;
        while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
               && (type != XmlPullParser.END_TAG
                       || parser.getDepth() > outerDepth)) {
            if (type == XmlPullParser.END_TAG
                    || type == XmlPullParser.TEXT) {
                continue;
            }

            String tagName = parser.getName();
            //Log.i(TAG, "Parse outerDepth=" + outerDepth + " depth="
            //        + parser.getDepth() + " tag=" + tagName);
            if (tagName.equals(TAG_SET)) {
                String name = parser.getAttributeValue(null, ATTR_NAME);
                if (name == null) {
                    if (mParseError == null) {
                        mParseError = "No name in set tag in preferred activity "
                            + mShortComponent;
                    }
                } else if (setPos >= setCount) {
                    if (mParseError == null) {
                        mParseError = "Too many set tags in preferred activity "
                            + mShortComponent;
                    }
                } else {
                    ComponentName cn = ComponentName.unflattenFromString(name);
                    if (cn == null) {
                        if (mParseError == null) {
                            mParseError = "Bad set name " + name + " in preferred activity "
                                + mShortComponent;
                        }
                    } else {
                        myPackages[setPos] = cn.getPackageName();
                        myClasses[setPos] = cn.getClassName();
                        myComponents[setPos] = name;
                        setPos++;
                    }
                }
                XmlUtils.skipCurrentTag(parser);
            } else if (!mCallbacks.onReadTag(tagName, parser)) {
                Slog.w("PreferredComponent", "Unknown element: " + parser.getName());
                XmlUtils.skipCurrentTag(parser);
            }
        }

        if (setPos != setCount) {
            if (mParseError == null) {
                mParseError = "Not enough set tags (expected " + setCount
                    + " but found " + setPos + ") in " + mShortComponent;
            }
        }

        mSetPackages = myPackages;
        mSetClasses = myClasses;
        mSetComponents = myComponents;
    }

    public String getParseError() {
        return mParseError;
    }

    public void writeToXml(TypedXmlSerializer serializer, boolean full) throws IOException {
        final int NS = mSetClasses != null ? mSetClasses.length : 0;
        serializer.attribute(null, ATTR_NAME, mShortComponent);
        if (full) {
            if (mMatch != 0) {
                serializer.attributeIntHex(null, ATTR_MATCH, mMatch);
            }
            serializer.attributeBoolean(null, ATTR_ALWAYS, mAlways);
            serializer.attributeInt(null, ATTR_SET, NS);
            for (int s=0; s<NS; s++) {
                serializer.startTag(null, TAG_SET);
                serializer.attribute(null, ATTR_NAME, mSetComponents[s]);
                serializer.endTag(null, TAG_SET);
            }
        }
    }

    public boolean sameSet(List<ResolveInfo> query, boolean excludeSetupWizardPackage, int userId) {
        if (mSetPackages == null) {
            return query == null;
        }
        if (query == null) {
            return false;
        }
        final int NQ = query.size();
        final int NS = mSetPackages.length;
        final PackageManagerInternal pmi = LocalServices.getService(PackageManagerInternal.class);
        String setupWizardPackageName = pmi.getSetupWizardPackageName();
        int numMatch = 0;
        for (int i=0; i<NQ; i++) {
            ResolveInfo ri = query.get(i);
            ActivityInfo ai = ri.activityInfo;
            boolean good = false;

            // ignore SetupWizard package's launcher capability because it is only existed
            // during SetupWizard is running
            if (excludeSetupWizardPackage && ai.packageName.equals(setupWizardPackageName)) {
                continue;
            }

            // Avoid showing the disambiguation dialog if the package is not installed or
            // installed with reason INSTALL_REASON_DEVICE_SETUP.
            final PackageStateInternal ps = pmi.getPackageStateInternal(ai.packageName);
            if (ps == null) {
                continue;
            }
            final PackageUserStateInternal pkgUserState = ps.getUserStates().get(userId);
            if (pkgUserState == null
                    || pkgUserState.getInstallReason()
                    == PackageManager.INSTALL_REASON_DEVICE_SETUP) {
                continue;
            }

            for (int j=0; j<NS; j++) {
                if (mSetPackages[j].equals(ai.packageName)
                        && mSetClasses[j].equals(ai.name)) {
                    numMatch++;
                    good = true;
                    break;
                }
            }
            if (!good) return false;
        }
        return numMatch == NS;
    }

    public boolean sameSet(ComponentName[] comps) {
        if (mSetPackages == null) return false;
        final int NQ = comps.length;
        final int NS = mSetPackages.length;
        int numMatch = 0;
        for (int i=0; i<NQ; i++) {
            ComponentName cn = comps[i];
            boolean good = false;
            for (int j=0; j<NS; j++) {
                if (mSetPackages[j].equals(cn.getPackageName())
                        && mSetClasses[j].equals(cn.getClassName())) {
                    numMatch++;
                    good = true;
                    break;
                }
            }
            if (!good) return false;
        }
        return numMatch == NS;
    }

    public boolean sameSet(PreferredComponent pc) {
        if (mSetPackages == null || pc == null || pc.mSetPackages == null
                || !sameComponent(pc.mComponent)) {
            return false;
        }
        final int otherPackageCount = pc.mSetPackages.length;
        final int packageCount = mSetPackages.length;
        if (otherPackageCount != packageCount) {
            return false;
        }
        for (int i = 0; i < packageCount; i++) {
            if (!mSetPackages[i].equals(pc.mSetPackages[i])
                    || !mSetClasses[i].equals(pc.mSetClasses[i])) {
                return false;
            }
        }
        return true;
    }

    /** Returns true if the preferred component represents the provided ComponentName. */
    private boolean sameComponent(ComponentName comp) {
        if (mComponent == null || comp == null) {
            return false;
        }
        if (mComponent.getPackageName().equals(comp.getPackageName())
                && mComponent.getClassName().equals(comp.getClassName())) {
            return true;
        }
        return false;
    }

    public boolean isSuperset(List<ResolveInfo> query, boolean excludeSetupWizardPackage) {
        if (mSetPackages == null) {
            return query == null;
        }
        if (query == null) {
            return true;
        }
        final int NQ = query.size();
        final int NS = mSetPackages.length;
        if (!excludeSetupWizardPackage && NS < NQ) {
            return false;
        }
        final PackageManagerInternal pmi = LocalServices.getService(PackageManagerInternal.class);
        String setupWizardPackageName = pmi.getSetupWizardPackageName();
        for (int i=0; i<NQ; i++) {
            ResolveInfo ri = query.get(i);
            ActivityInfo ai = ri.activityInfo;
            boolean foundMatch = false;

            // ignore SetupWizard package's launcher capability because it is only existed
            // during SetupWizard is running
            if (excludeSetupWizardPackage && ai.packageName.equals(setupWizardPackageName)) {
                continue;
            }

            for (int j=0; j<NS; j++) {
                if (mSetPackages[j].equals(ai.packageName) && mSetClasses[j].equals(ai.name)) {
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) return false;
        }
        return true;
    }

    /** Returns components from mSetPackages that are present in query. */
    public ComponentName[] discardObsoleteComponents(List<ResolveInfo> query) {
        if (mSetPackages == null || query == null) {
            return new ComponentName[0];
        }
        final int NQ = query.size();
        final int NS = mSetPackages.length;
        ArrayList<ComponentName> aliveComponents = new ArrayList<>();
        for (int i = 0; i < NQ; i++) {
            ResolveInfo ri = query.get(i);
            ActivityInfo ai = ri.activityInfo;
            for (int j = 0; j < NS; j++) {
                if (mSetPackages[j].equals(ai.packageName) && mSetClasses[j].equals(ai.name)) {
                    aliveComponents.add(new ComponentName(mSetPackages[j], mSetClasses[j]));
                    break;
                }
            }
        }
        return aliveComponents.toArray(new ComponentName[aliveComponents.size()]);
    }

    public void dump(PrintWriter out, String prefix, Object ident) {
        out.print(prefix); out.print(
                Integer.toHexString(System.identityHashCode(ident)));
                out.print(' ');
                out.println(mShortComponent);
        out.print(prefix); out.print(" mMatch=0x");
                out.print(Integer.toHexString(mMatch));
                out.print(" mAlways="); out.println(mAlways);
        if (mSetComponents != null) {
            out.print(prefix); out.println("  Selected from:");
            for (int i=0; i<mSetComponents.length; i++) {
                out.print(prefix); out.print("    ");
                        out.println(mSetComponents[i]);
            }
        }
    }
}
