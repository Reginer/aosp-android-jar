/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.content;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.compat.annotation.ChangeId;
import android.compat.annotation.Disabled;
import android.compat.annotation.Overridable;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.pm.Flags;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PatternMatcher;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.AndroidException;
import android.util.ArraySet;
import android.util.Log;
import android.util.Printer;
import android.util.proto.ProtoOutputStream;

import com.android.internal.util.XmlUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Structured description of Intent values to be matched.  An IntentFilter can
 * match against actions, categories, and data (either via its type, scheme,
 * and/or path) in an Intent.  It also includes a "priority" value which is
 * used to order multiple matching filters.
 *
 * <p>IntentFilter objects are often created in XML as part of a package's
 * {@link android.R.styleable#AndroidManifest AndroidManifest.xml} file,
 * using {@link android.R.styleable#AndroidManifestIntentFilter intent-filter}
 * tags.
 *
 * <p>There are three Intent characteristics you can filter on: the
 * <em>action</em>, <em>data</em>, and <em>categories</em>.  For each of these
 * characteristics you can provide
 * multiple possible matching values (via {@link #addAction},
 * {@link #addDataType}, {@link #addDataScheme}, {@link #addDataSchemeSpecificPart},
 * {@link #addDataAuthority}, {@link #addDataPath}, and {@link #addCategory}, respectively).
 * For actions, if no data characteristics are specified, then the filter will
 * only match intents that contain no data.
 *
 * <p>The data characteristic is
 * itself divided into three attributes: type, scheme, authority, and path.
 * Any that are
 * specified must match the contents of the Intent.  If you specify a scheme
 * but no type, only Intent that does not have a type (such as mailto:) will
 * match; a content: URI will never match because they always have a MIME type
 * that is supplied by their content provider.  Specifying a type with no scheme
 * has somewhat special meaning: it will match either an Intent with no URI
 * field, or an Intent with a content: or file: URI.  If you specify neither,
 * then only an Intent with no data or type will match.  To specify an authority,
 * you must also specify one or more schemes that it is associated with.
 * To specify a path, you also must specify both one or more authorities and
 * one or more schemes it is associated with.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For information about how to create and resolve intents, read the
 * <a href="{@docRoot}guide/topics/intents/intents-filters.html">Intents and Intent Filters</a>
 * developer guide.</p>
 * </div>
 *
 * <h3>Filter Rules</h3>
 * <p>A match is based on the following rules.  Note that
 * for an IntentFilter to match an Intent, three conditions must hold:
 * the <strong>action</strong> and <strong>category</strong> must match, and
 * the data (both the <strong>data type</strong> and
 * <strong>data scheme+authority+path</strong> if specified) must match
 * (see {@link #match(ContentResolver, Intent, boolean, String)} for more details
 * on how the data fields match).
 *
 * <p><strong>Action</strong> matches if any of the given values match the
 * Intent action; if the filter specifies no actions, then it will only match
 * Intents that do not contain an action.
 *
 * <p><strong>Data Type</strong> matches if any of the given values match the
 * Intent type.  The Intent
 * type is determined by calling {@link Intent#resolveType}.  A wildcard can be
 * used for the MIME sub-type, in both the Intent and IntentFilter, so that the
 * type "audio/*" will match "audio/mpeg", "audio/aiff", "audio/*", etc.
 * <em>Note that MIME type matching here is <b>case sensitive</b>, unlike
 * formal RFC MIME types!</em>  You should thus always use lower case letters
 * for your MIME types.
 *
 * <p><strong>Data Scheme</strong> matches if any of the given values match the
 * Intent data's scheme.
 * The Intent scheme is determined by calling {@link Intent#getData}
 * and {@link android.net.Uri#getScheme} on that URI.
 * <em>Note that scheme matching here is <b>case sensitive</b>, unlike
 * formal RFC schemes!</em>  You should thus always use lower case letters
 * for your schemes.
 *
 * <p><strong>Data Scheme Specific Part</strong> matches if any of the given values match
 * the Intent's data scheme specific part <em>and</em> one of the data schemes in the filter
 * has matched the Intent, <em>or</em> no scheme specific parts were supplied in the filter.
 * The Intent scheme specific part is determined by calling
 * {@link Intent#getData} and {@link android.net.Uri#getSchemeSpecificPart} on that URI.
 * <em>Note that scheme specific part matching is <b>case sensitive</b>.</em>
 *
 * <p><strong>Data Authority</strong> matches if any of the given values match
 * the Intent's data authority <em>and</em> one of the data schemes in the filter
 * has matched the Intent, <em>or</em> no authorities were supplied in the filter.
 * The Intent authority is determined by calling
 * {@link Intent#getData} and {@link android.net.Uri#getAuthority} on that URI.
 * <em>Note that authority matching here is <b>case sensitive</b>, unlike
 * formal RFC host names!</em>  You should thus always use lower case letters
 * for your authority.
 *
 * <p><strong>Data Path</strong> matches if any of the given values match the
 * Intent's data path <em>and</em> both a scheme and authority in the filter
 * has matched against the Intent, <em>or</em> no paths were supplied in the
 * filter.  The Intent authority is determined by calling
 * {@link Intent#getData} and {@link android.net.Uri#getPath} on that URI.
 *
 * <p><strong>Categories</strong> match if <em>all</em> of the categories in
 * the Intent match categories given in the filter.  Extra categories in the
 * filter that are not in the Intent will not cause the match to fail.  Note
 * that unlike the action, an IntentFilter with no categories
 * will only match an Intent that does not have any categories.
 */
@android.ravenwood.annotation.RavenwoodKeepWholeClass
public class IntentFilter implements Parcelable {
    private static final String TAG = "IntentFilter";

    private static final String AGLOB_STR = "aglob";
    private static final String SGLOB_STR = "sglob";
    private static final String PREFIX_STR = "prefix";
    private static final String SUFFIX_STR = "suffix";
    private static final String LITERAL_STR = "literal";
    private static final String PATH_STR = "path";
    private static final String PORT_STR = "port";
    private static final String HOST_STR = "host";
    private static final String AUTH_STR = "auth";
    private static final String SSP_STR = "ssp";
    private static final String SCHEME_STR = "scheme";
    private static final String STATIC_TYPE_STR = "staticType";
    private static final String TYPE_STR = "type";
    private static final String GROUP_STR = "group";
    private static final String CAT_STR = "cat";
    private static final String NAME_STR = "name";
    private static final String ACTION_STR = "action";
    private static final String AUTO_VERIFY_STR = "autoVerify";
    private static final String EXTRAS_STR = "extras";
    private static final String URI_RELATIVE_FILTER_GROUP_STR = "uriRelativeFilterGroup";

    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final long[] EMPTY_LONG_ARRAY = new long[0];
    private static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];

    /**
     * An intent with action set as null used to always pass the action test during intent
     * filter matching. This causes a lot of confusion and unexpected intent matches.
     * Null action intents should be blocked when the intent sender application targets V or higher.
     *
     * @hide
     */
    @ChangeId
    @Disabled
    @Overridable
    public static final long BLOCK_NULL_ACTION_INTENTS = 293560872;

    /**
     * The filter {@link #setPriority} value at which system high-priority
     * receivers are placed; that is, receivers that should execute before
     * application code. Applications should never use filters with this or
     * higher priorities.
     *
     * @see #setPriority
     */
    public static final int SYSTEM_HIGH_PRIORITY = 1000;

    /**
     * The filter {@link #setPriority} value at which system low-priority
     * receivers are placed; that is, receivers that should execute after
     * application code. Applications should never use filters with this or
     * lower priorities.
     *
     * @see #setPriority
     */
    public static final int SYSTEM_LOW_PRIORITY = -1000;

    /**
     * The part of a match constant that describes the category of match
     * that occurred.  May be either {@link #MATCH_CATEGORY_EMPTY},
     * {@link #MATCH_CATEGORY_SCHEME}, {@link #MATCH_CATEGORY_SCHEME_SPECIFIC_PART},
     * {@link #MATCH_CATEGORY_HOST}, {@link #MATCH_CATEGORY_PORT},
     * {@link #MATCH_CATEGORY_PATH}, or {@link #MATCH_CATEGORY_TYPE}.  Higher
     * values indicate a better match.
     */
    public static final int MATCH_CATEGORY_MASK = 0xfff0000;

    /**
     * The part of a match constant that applies a quality adjustment to the
     * basic category of match.  The value {@link #MATCH_ADJUSTMENT_NORMAL}
     * is no adjustment; higher numbers than that improve the quality, while
     * lower numbers reduce it.
     */
    public static final int MATCH_ADJUSTMENT_MASK = 0x000ffff;

    /**
     * Quality adjustment applied to the category of match that signifies
     * the default, base value; higher numbers improve the quality while
     * lower numbers reduce it.
     */
    public static final int MATCH_ADJUSTMENT_NORMAL = 0x8000;

    /**
     * The filter matched an intent that had no data specified.
     */
    public static final int MATCH_CATEGORY_EMPTY = 0x0100000;
    /**
     * The filter matched an intent with the same data URI scheme.
     */
    public static final int MATCH_CATEGORY_SCHEME = 0x0200000;
    /**
     * The filter matched an intent with the same data URI scheme and
     * authority host.
     */
    public static final int MATCH_CATEGORY_HOST = 0x0300000;
    /**
     * The filter matched an intent with the same data URI scheme and
     * authority host and port.
     */
    public static final int MATCH_CATEGORY_PORT = 0x0400000;
    /**
     * The filter matched an intent with the same data URI scheme,
     * authority, and path.
     */
    public static final int MATCH_CATEGORY_PATH = 0x0500000;
    /**
     * The filter matched an intent with the same data URI scheme and
     * scheme specific part.
     */
    public static final int MATCH_CATEGORY_SCHEME_SPECIFIC_PART = 0x0580000;
    /**
     * The filter matched an intent with the same data MIME type.
     */
    public static final int MATCH_CATEGORY_TYPE = 0x0600000;

    /**
     * The filter didn't match due to different MIME types.
     */
    public static final int NO_MATCH_TYPE = -1;
    /**
     * The filter didn't match due to different data URIs.
     */
    public static final int NO_MATCH_DATA = -2;
    /**
     * The filter didn't match due to different actions.
     */
    public static final int NO_MATCH_ACTION = -3;
    /**
     * The filter didn't match because it required one or more categories
     * that were not in the Intent.
     */
    public static final int NO_MATCH_CATEGORY = -4;
    /**
     * That filter didn't match due to different extras data.
     * @hide
     */
    public static final int NO_MATCH_EXTRAS = -5;

    /**
     * HTTP scheme.
     *
     * @see #addDataScheme(String)
     * @hide
     */
    public static final String SCHEME_HTTP = "http";
    /**
     * HTTPS scheme.
     *
     * @see #addDataScheme(String)
     * @hide
     */
    public static final String SCHEME_HTTPS = "https";

    /**
     * Package scheme
     *
     * @see #addDataScheme(String)
     * @hide
     */
    public static final String SCHEME_PACKAGE = "package";

    /**
     * The value to indicate a wildcard for incoming match arguments.
     * @hide
     */
    public static final String WILDCARD = "*";
    /** @hide */
    public static final String WILDCARD_PATH = "/" + WILDCARD;

    private int mPriority;
    @UnsupportedAppUsage
    private int mOrder;
    @UnsupportedAppUsage
    private final ArraySet<String> mActions;
    private ArrayList<String> mCategories = null;
    private ArrayList<String> mDataSchemes = null;
    private ArrayList<PatternMatcher> mDataSchemeSpecificParts = null;
    private ArrayList<AuthorityEntry> mDataAuthorities = null;
    private ArrayList<PatternMatcher> mDataPaths = null;
    private ArrayList<UriRelativeFilterGroup> mUriRelativeFilterGroups = null;
    private ArrayList<String> mStaticDataTypes = null;
    private ArrayList<String> mDataTypes = null;
    private ArrayList<String> mMimeGroups = null;
    private boolean mHasStaticPartialTypes = false;
    private boolean mHasDynamicPartialTypes = false;
    private PersistableBundle mExtras = null;

    private static final int STATE_VERIFY_AUTO         = 0x00000001;
    private static final int STATE_NEED_VERIFY         = 0x00000010;
    private static final int STATE_NEED_VERIFY_CHECKED = 0x00000100;
    private static final int STATE_VERIFIED            = 0x00001000;

    private int mVerifyState;
    /** @hide */
    public static final int VISIBILITY_NONE = 0;
    /** @hide */
    public static final int VISIBILITY_EXPLICIT = 1;
    /** @hide */
    public static final int VISIBILITY_IMPLICIT = 2;
    /** @hide */
    @IntDef(prefix = { "VISIBILITY_" }, value = {
            VISIBILITY_NONE,
            VISIBILITY_EXPLICIT,
            VISIBILITY_IMPLICIT,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface InstantAppVisibility {}
    /** Whether or not the intent filter is visible to instant apps. */
    private @InstantAppVisibility int mInstantAppVisibility;
    // These functions are the start of more optimized code for managing
    // the string sets...  not yet implemented.

    private static int findStringInSet(String[] set, String string,
            int[] lengths, int lenPos) {
        if (set == null) return -1;
        final int N = lengths[lenPos];
        for (int i=0; i<N; i++) {
            if (set[i].equals(string)) return i;
        }
        return -1;
    }

    private static String[] addStringToSet(String[] set, String string,
            int[] lengths, int lenPos) {
        if (findStringInSet(set, string, lengths, lenPos) >= 0) return set;
        if (set == null) {
            set = new String[2];
            set[0] = string;
            lengths[lenPos] = 1;
            return set;
        }
        final int N = lengths[lenPos];
        if (N < set.length) {
            set[N] = string;
            lengths[lenPos] = N+1;
            return set;
        }

        String[] newSet = new String[(N*3)/2 + 2];
        System.arraycopy(set, 0, newSet, 0, N);
        set = newSet;
        set[N] = string;
        lengths[lenPos] = N+1;
        return set;
    }

    private static String[] removeStringFromSet(String[] set, String string,
            int[] lengths, int lenPos) {
        int pos = findStringInSet(set, string, lengths, lenPos);
        if (pos < 0) return set;
        final int N = lengths[lenPos];
        if (N > (set.length/4)) {
            int copyLen = N-(pos+1);
            if (copyLen > 0) {
                System.arraycopy(set, pos+1, set, pos, copyLen);
            }
            set[N-1] = null;
            lengths[lenPos] = N-1;
            return set;
        }

        String[] newSet = new String[set.length/3];
        if (pos > 0) System.arraycopy(set, 0, newSet, 0, pos);
        if ((pos+1) < N) System.arraycopy(set, pos+1, newSet, pos, N-(pos+1));
        return newSet;
    }

    /**
     * This exception is thrown when a given MIME type does not have a valid
     * syntax.
     */
    public static class MalformedMimeTypeException extends AndroidException {
        public MalformedMimeTypeException() {
        }

        public MalformedMimeTypeException(String name) {
            super(name);
        }
    }

    /**
     * Create a new IntentFilter instance with a specified action and MIME
     * type, where you know the MIME type is correctly formatted.  This catches
     * the {@link MalformedMimeTypeException} exception that the constructor
     * can call and turns it into a runtime exception.
     *
     * @param action The action to match, such as Intent.ACTION_VIEW.
     * @param dataType The type to match, such as "vnd.android.cursor.dir/person".
     *
     * @return A new IntentFilter for the given action and type.
     *
     * @see #IntentFilter(String, String)
     */
    public static IntentFilter create(String action, String dataType) {
        try {
            return new IntentFilter(action, dataType);
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("Bad MIME type", e);
        }
    }

    /**
     * New empty IntentFilter.
     */
    public IntentFilter() {
        mPriority = 0;
        mActions = new ArraySet<>();
    }

    /**
     * New IntentFilter that matches a single action with no data.  If
     * no data characteristics are subsequently specified, then the
     * filter will only match intents that contain no data.
     *
     * @param action The action to match, such as Intent.ACTION_MAIN.
     */
    public IntentFilter(String action) {
        mPriority = 0;
        mActions = new ArraySet<>();
        addAction(action);
    }

    /**
     * New IntentFilter that matches a single action and data type.
     *
     * <p><em>Note: MIME type matching in the Android framework is
     * case-sensitive, unlike formal RFC MIME types.  As a result,
     * you should always write your MIME types with lower case letters,
     * and any MIME types you receive from outside of Android should be
     * converted to lower case before supplying them here.</em></p>
     *
     * <p>Throws {@link MalformedMimeTypeException} if the given MIME type is
     * not syntactically correct.
     *
     * @param action The action to match, such as Intent.ACTION_VIEW.
     * @param dataType The type to match, such as "vnd.android.cursor.dir/person".
     *
     */
    public IntentFilter(String action, String dataType)
        throws MalformedMimeTypeException {
        mPriority = 0;
        mActions = new ArraySet<>();
        addAction(action);
        addDataType(dataType);
    }

    /**
     * New IntentFilter containing a copy of an existing filter.
     *
     * @param o The original filter to copy.
     */
    public IntentFilter(IntentFilter o) {
        mPriority = o.mPriority;
        mOrder = o.mOrder;
        mActions = new ArraySet<>(o.mActions);
        if (o.mCategories != null) {
            mCategories = new ArrayList<String>(o.mCategories);
        }
        if (o.mStaticDataTypes != null) {
            mStaticDataTypes = new ArrayList<String>(o.mStaticDataTypes);
        }
        if (o.mDataTypes != null) {
            mDataTypes = new ArrayList<String>(o.mDataTypes);
        }
        if (o.mDataSchemes != null) {
            mDataSchemes = new ArrayList<String>(o.mDataSchemes);
        }
        if (o.mDataSchemeSpecificParts != null) {
            mDataSchemeSpecificParts = new ArrayList<PatternMatcher>(o.mDataSchemeSpecificParts);
        }
        if (o.mDataAuthorities != null) {
            mDataAuthorities = new ArrayList<AuthorityEntry>(o.mDataAuthorities);
        }
        if (o.mDataPaths != null) {
            mDataPaths = new ArrayList<PatternMatcher>(o.mDataPaths);
        }
        if (o.mUriRelativeFilterGroups != null) {
            mUriRelativeFilterGroups =
                    new ArrayList<UriRelativeFilterGroup>(o.mUriRelativeFilterGroups);
        }
        if (o.mMimeGroups != null) {
            mMimeGroups = new ArrayList<String>(o.mMimeGroups);
        }
        if (o.mExtras != null) {
            mExtras = new PersistableBundle(o.mExtras);
        }
        mHasStaticPartialTypes = o.mHasStaticPartialTypes;
        mHasDynamicPartialTypes = o.mHasDynamicPartialTypes;
        mVerifyState = o.mVerifyState;
        mInstantAppVisibility = o.mInstantAppVisibility;
    }

    /** @hide */
    public String toLongString() {
        // Not implemented directly as toString() due to potential memory regression
        final StringBuilder sb = new StringBuilder();
        sb.append("IntentFilter {");
        sb.append(" pri=");
        sb.append(mPriority);
        if (countActions() > 0) {
            sb.append(" act=");
            sb.append(mActions.toString());
        }
        if (countCategories() > 0) {
            sb.append(" cat=");
            sb.append(mCategories.toString());
        }
        if (countDataSchemes() > 0) {
            sb.append(" sch=");
            sb.append(mDataSchemes.toString());
        }
        if (Flags.relativeReferenceIntentFilters() && countUriRelativeFilterGroups() > 0) {
            sb.append(" grp=");
            sb.append(mUriRelativeFilterGroups.toString());
        }
        sb.append(" }");
        return sb.toString();
    }

    /**
     * Modify priority of this filter.  This only affects receiver filters.
     * The priority of activity filters are set in XML and cannot be changed
     * programmatically. The default priority is 0. Positive values will be
     * before the default, lower values will be after it. Applications should
     * use a value that is larger than {@link #SYSTEM_LOW_PRIORITY} and
     * smaller than {@link #SYSTEM_HIGH_PRIORITY} .
     *
     * @param priority The new priority value.
     *
     * @see #getPriority
     * @see #SYSTEM_LOW_PRIORITY
     * @see #SYSTEM_HIGH_PRIORITY
     */
    public final void setPriority(int priority) {
        mPriority = priority;
    }

    /**
     * Return the priority of this filter.
     *
     * @return The priority of the filter.
     *
     * @see #setPriority
     */
    public final int getPriority() {
        return mPriority;
    }

    /** @hide */
    @SystemApi
    public final void setOrder(int order) {
        mOrder = order;
    }

    /** @hide */
    @SystemApi
    public final int getOrder() {
        return mOrder;
    }

    /**
     * Set whether this filter will needs to be automatically verified against its data URIs or not.
     * The default is false.
     *
     * The verification would need to happen only and only if the Intent action is
     * {@link android.content.Intent#ACTION_VIEW} and the Intent category is
     * {@link android.content.Intent#CATEGORY_BROWSABLE} and the Intent data scheme
     * is "http" or "https".
     *
     * True means that the filter will need to use its data URIs to be verified.
     *
     * @param autoVerify The new autoVerify value.
     *
     * @see #getAutoVerify()
     * @see #addAction(String)
     * @see #getAction(int)
     * @see #addCategory(String)
     * @see #getCategory(int)
     * @see #addDataScheme(String)
     * @see #getDataScheme(int)
     *
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public final void setAutoVerify(boolean autoVerify) {
        mVerifyState &= ~STATE_VERIFY_AUTO;
        if (autoVerify) mVerifyState |= STATE_VERIFY_AUTO;
    }

    /**
     * Return if this filter will needs to be automatically verified again its data URIs or not.
     *
     * @return True if the filter will needs to be automatically verified. False otherwise.
     *
     * @see #setAutoVerify(boolean)
     *
     * @hide
     */
    public final boolean getAutoVerify() {
        return ((mVerifyState & STATE_VERIFY_AUTO) == STATE_VERIFY_AUTO);
    }

    /**
     * Return if this filter handle all HTTP or HTTPS data URI or not.  This is the
     * core check for whether a given activity qualifies as a "browser".
     *
     * @return True if the filter handle all HTTP or HTTPS data URI. False otherwise.
     *
     * This will check if:
     *
     * - either the Intent category is {@link android.content.Intent#CATEGORY_APP_BROWSER}
     * - either the Intent action is {@link android.content.Intent#ACTION_VIEW} and
     * the Intent category is {@link android.content.Intent#CATEGORY_BROWSABLE} and the Intent
     * data scheme is "http" or "https" and that there is no specific host defined.
     *
     * @hide
     */
    public final boolean handleAllWebDataURI() {
        return hasCategory(Intent.CATEGORY_APP_BROWSER) ||
                (handlesWebUris(false) && countDataAuthorities() == 0);
    }

    /**
     * Return if this filter handles HTTP or HTTPS data URIs.
     *
     * @return True if the filter handles ACTION_VIEW/CATEGORY_BROWSABLE,
     * has at least one HTTP or HTTPS data URI pattern defined, and optionally
     * does not define any non-http/https data URI patterns.
     *
     * This will check if the Intent action is {@link android.content.Intent#ACTION_VIEW} and
     * the Intent category is {@link android.content.Intent#CATEGORY_BROWSABLE} and the Intent
     * data scheme is "http" or "https".
     *
     * @param onlyWebSchemes When true, requires that the intent filter declare
     *     that it handles *only* http: or https: schemes.  This is a requirement for
     *     the intent filter's domain linkage being verifiable.
     * @hide
     */
    public final boolean handlesWebUris(boolean onlyWebSchemes) {
        // Require ACTION_VIEW, CATEGORY_BROWSEABLE, and at least one scheme
        if (!hasAction(Intent.ACTION_VIEW)
            || !hasCategory(Intent.CATEGORY_BROWSABLE)
            || mDataSchemes == null
            || mDataSchemes.size() == 0) {
            return false;
        }

        // Now allow only the schemes "http" and "https"
        final int N = mDataSchemes.size();
        for (int i = 0; i < N; i++) {
            final String scheme = mDataSchemes.get(i);
            final boolean isWebScheme =
                    SCHEME_HTTP.equals(scheme) || SCHEME_HTTPS.equals(scheme);
            if (onlyWebSchemes) {
                // If we're specifically trying to ensure that there are no non-web schemes
                // declared in this filter, then if we ever see a non-http/https scheme then
                // we know it's a failure.
                if (!isWebScheme) {
                    return false;
                }
            } else {
                // If we see any http/https scheme declaration in this case then the
                // filter matches what we're looking for.
                if (isWebScheme) {
                    return true;
                }
            }
        }

        // We get here if:
        //   1) onlyWebSchemes and no non-web schemes were found, i.e. success; or
        //   2) !onlyWebSchemes and no http/https schemes were found, i.e. failure.
        return onlyWebSchemes;
    }

    /**
     * Return if this filter needs to be automatically verified again its data URIs or not.
     *
     * @return True if the filter needs to be automatically verified. False otherwise.
     *
     * This will check if the Intent action is {@link android.content.Intent#ACTION_VIEW} and
     * the Intent category is {@link android.content.Intent#CATEGORY_BROWSABLE} and the Intent
     * data scheme is "http" or "https".
     *
     * @see #setAutoVerify(boolean)
     *
     * @hide
     */
    public final boolean needsVerification() {
        return getAutoVerify() && handlesWebUris(true);
    }

    /**
     * Return if this filter has been verified
     *
     * @return true if the filter has been verified or if autoVerify is false.
     *
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    public final boolean isVerified() {
        if ((mVerifyState & STATE_NEED_VERIFY_CHECKED) == STATE_NEED_VERIFY_CHECKED) {
            return ((mVerifyState & STATE_NEED_VERIFY) == STATE_NEED_VERIFY);
        }
        return false;
    }

    /**
     * Set if this filter has been verified
     *
     * @param verified true if this filter has been verified. False otherwise.
     *
     * @hide
     */
    public void setVerified(boolean verified) {
        mVerifyState |= STATE_NEED_VERIFY_CHECKED;
        mVerifyState &= ~STATE_VERIFIED;
        if (verified) mVerifyState |= STATE_VERIFIED;
    }

    /** @hide */
    public void setVisibilityToInstantApp(@InstantAppVisibility int visibility) {
        mInstantAppVisibility = visibility;
    }
    /** @hide */
    public @InstantAppVisibility int getVisibilityToInstantApp() {
        return mInstantAppVisibility;
    }
    /** @hide */
    public boolean isVisibleToInstantApp() {
        return mInstantAppVisibility != VISIBILITY_NONE;
    }
    /** @hide */
    public boolean isExplicitlyVisibleToInstantApp() {
        return mInstantAppVisibility == VISIBILITY_EXPLICIT;
    }
    /** @hide */
    public boolean isImplicitlyVisibleToInstantApp() {
        return mInstantAppVisibility == VISIBILITY_IMPLICIT;
    }

    /**
     * Add a new Intent action to match against.  If any actions are included
     * in the filter, then an Intent's action must be one of those values for
     * it to match.  If no actions are included, the Intent action is ignored.
     *
     * @param action Name of the action to match, such as Intent.ACTION_VIEW.
     */
    public final void addAction(String action) {
        mActions.add(action.intern());
    }

    /**
     * Return the number of actions in the filter.
     */
    public final int countActions() {
        return mActions.size();
    }

    /**
     * Returns the number of actions in the filter, or {@code 0} if there are no actions.
     * <p> This method provides a safe alternative to {@link #countActions()}, which
     * may throw an exception if there are no actions.
     * @hide
     */
    public final int safeCountActions() {
        return mActions == null ? 0 : mActions.size();
    }

    /**
     * Return an action in the filter.
     */
    public final String getAction(int index) {
        return mActions.valueAt(index);
    }

    /**
     * Is the given action included in the filter?  Note that if the filter
     * does not include any actions, false will <em>always</em> be returned.
     *
     * @param action The action to look for.
     *
     * @return True if the action is explicitly mentioned in the filter.
     */
    public final boolean hasAction(String action) {
        return action != null && mActions.contains(action);
    }

    /**
     * Match this filter against an Intent's action.  If the filter does not
     * specify any actions, the match will always fail.
     *
     * @param action The desired action to look for.
     *
     * @return True if the action is listed in the filter.
     */
    public final boolean matchAction(String action) {
        return matchAction(action, false /*wildcardSupported*/, null /*ignoreActions*/);
    }

    /**
     * Variant of {@link #matchAction(String)} that allows a wildcard for the provided action.
     * @param wildcardSupported if true, will allow action to use wildcards
     * @param ignoreActions if not null, the set of actions to should not be considered valid while
     *                      calculating the match
     */
    private boolean matchAction(String action, boolean wildcardSupported,
            @Nullable Collection<String> ignoreActions) {
        if (wildcardSupported && WILDCARD.equals(action)) {
            if (ignoreActions == null) {
                return !mActions.isEmpty();
            }
            if (mActions.size() > ignoreActions.size()) {
                return true;    // some actions are definitely not ignored
            }
            for (int i = mActions.size() - 1; i >= 0; i--) {
                if (!ignoreActions.contains(mActions.valueAt(i))) {
                    return true;
                }
            }
            return false;
        }
        if (ignoreActions != null && ignoreActions.contains(action)) {
            return false;
        }
        return hasAction(action);
    }

    /**
     * Return an iterator over the filter's actions.  If there are no actions,
     * returns null.
     */
    public final Iterator<String> actionsIterator() {
        return mActions != null ? mActions.iterator() : null;
    }

    /**
     * Add a new Intent data type to match against.  If any types are
     * included in the filter, then an Intent's data must be <em>either</em>
     * one of these types <em>or</em> a matching scheme.  If no data types
     * are included, then an Intent will only match if it specifies no data.
     *
     * <p><em>Note: MIME type matching in the Android framework is
     * case-sensitive, unlike formal RFC MIME types.  As a result,
     * you should always write your MIME types with lower case letters,
     * and any MIME types you receive from outside of Android should be
     * converted to lower case before supplying them here.</em></p>
     *
     * <p>Throws {@link MalformedMimeTypeException} if the given MIME type is
     * not syntactically correct.
     *
     * @param type Name of the data type to match, such as "vnd.android.cursor.dir/person".
     *
     * @see #matchData
     */
    public final void addDataType(String type)
        throws MalformedMimeTypeException {
        processMimeType(type, (internalType, isPartial) -> {
            if (mDataTypes == null) {
                mDataTypes = new ArrayList<>();
            }
            if (mStaticDataTypes == null) {
                mStaticDataTypes = new ArrayList<>();
            }

            if (mDataTypes.contains(internalType)) {
                return;
            }

            mDataTypes.add(internalType.intern());
            mStaticDataTypes.add(internalType.intern());
            mHasStaticPartialTypes = mHasStaticPartialTypes || isPartial;
        });
    }

    /**
     * Add a new Intent data type <em>from MIME group</em> to match against.  If any types are
     * included in the filter, then an Intent's data must be <em>either</em>
     * one of these types <em>or</em> a matching scheme.  If no data types
     * are included, then an Intent will only match if it specifies no data.
     *
     * <p><em>Note: MIME type matching in the Android framework is
     * case-sensitive, unlike formal RFC MIME types.  As a result,
     * you should always write your MIME types with lower case letters,
     * and any MIME types you receive from outside of Android should be
     * converted to lower case before supplying them here.</em></p>
     *
     * <p>Throws {@link MalformedMimeTypeException} if the given MIME type is
     * not syntactically correct.
     *
     * @param type Name of the data type to match, such as "vnd.android.cursor.dir/person".
     *
     * @see #clearDynamicDataTypes()
     * @hide
     */
    public final void addDynamicDataType(String type)
            throws MalformedMimeTypeException {
        processMimeType(type, (internalType, isPartial) -> {
            if (mDataTypes == null) {
                mDataTypes = new ArrayList<>();
            }

            if (!mDataTypes.contains(internalType)) {
                mDataTypes.add(internalType.intern());

                mHasDynamicPartialTypes = mHasDynamicPartialTypes || isPartial;
            }
        });
    }

    /**
     * Process mime type - convert to representation used internally and check if type is partial,
     * and then call provided action
     */
    private void processMimeType(String type, BiConsumer<String, Boolean> action)
            throws MalformedMimeTypeException {
        final int slashpos = type.indexOf('/');
        final int typelen = type.length();
        if (slashpos <= 0 || typelen < slashpos + 2) {
            throw new MalformedMimeTypeException(type);
        }

        String internalType = type;
        boolean isPartialType = false;
        if (typelen == slashpos + 2 && type.charAt(slashpos + 1) == '*') {
            internalType = type.substring(0, slashpos);
            isPartialType = true;
        }

        action.accept(internalType, isPartialType);
    }

    /**
     * Remove all previously added Intent data types from IntentFilter.
     *
     * @see #addDynamicDataType(String)
     * @hide
     */
    public final void clearDynamicDataTypes() {
        if (mDataTypes == null) {
            return;
        }

        if (mStaticDataTypes != null) {
            mDataTypes.clear();
            mDataTypes.addAll(mStaticDataTypes);
        } else {
            mDataTypes = null;
        }

        mHasDynamicPartialTypes = false;
    }

    /**
     * Return the number of static data types in the filter.
     * @hide
     */
    public int countStaticDataTypes() {
        return mStaticDataTypes != null ? mStaticDataTypes.size() : 0;
    }

    /**
     * Is the given data type included in the filter?  Note that if the filter
     * does not include any type, false will <em>always</em> be returned.
     *
     * @param type The data type to look for.
     *
     * @return True if the type is explicitly mentioned in the filter.
     */
    public final boolean hasDataType(String type) {
        return mDataTypes != null && findMimeType(type);
    }

    /** @hide */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public final boolean hasExactDataType(String type) {
        return mDataTypes != null && mDataTypes.contains(type);
    }

    /** @hide */
    public final boolean hasExactDynamicDataType(String type) {
        return hasExactDataType(type) && !hasExactStaticDataType(type);
    }

    /** @hide */
    public final boolean hasExactStaticDataType(String type) {
        return mStaticDataTypes != null && mStaticDataTypes.contains(type);
    }

    /**
     * Return the number of data types in the filter.
     */
    public final int countDataTypes() {
        return mDataTypes != null ? mDataTypes.size() : 0;
    }

    /**
     * Return a data type in the filter.
     */
    public final String getDataType(int index) {
        return mDataTypes.get(index);
    }

    /**
     * Return an iterator over the filter's data types.
     */
    public final Iterator<String> typesIterator() {
        return mDataTypes != null ? mDataTypes.iterator() : null;
    }

    /**
     * Return copy of filter's data types.
     * @hide
     */
    public final List<String> dataTypes() {
        return mDataTypes != null ? new ArrayList<>(mDataTypes) : null;
    }

    /** @hide */
    public final void addMimeGroup(String name) {
        if (mMimeGroups == null) {
            mMimeGroups = new ArrayList<>();
        }
        if (!mMimeGroups.contains(name)) {
            mMimeGroups.add(name);
        }
    }

    /** @hide */
    public final boolean hasMimeGroup(String name) {
        return mMimeGroups != null && mMimeGroups.contains(name);
    }

    /** @hide */
    public final String getMimeGroup(int index) {
        return mMimeGroups.get(index);
    }

    /** @hide */
    public final int countMimeGroups() {
        return mMimeGroups != null ? mMimeGroups.size() : 0;
    }

    /** @hide */
    public final Iterator<String> mimeGroupsIterator() {
        return mMimeGroups != null ? mMimeGroups.iterator() : null;
    }

    /**
     * Add a new Intent data scheme to match against.  If any schemes are
     * included in the filter, then an Intent's data must be <em>either</em>
     * one of these schemes <em>or</em> a matching data type.  If no schemes
     * are included, then an Intent will match only if it includes no data.
     *
     * <p><em>Note: scheme matching in the Android framework is
     * case-sensitive, unlike formal RFC schemes.  As a result,
     * you should always write your schemes with lower case letters,
     * and any schemes you receive from outside of Android should be
     * converted to lower case before supplying them here.</em></p>
     *
     * @param scheme Name of the scheme to match, such as "http".
     *
     * @see #matchData
     */
    public final void addDataScheme(String scheme) {
        if (mDataSchemes == null) mDataSchemes = new ArrayList<String>();
        if (!mDataSchemes.contains(scheme)) {
            mDataSchemes.add(scheme.intern());
        }
    }

    /**
     * Return the number of data schemes in the filter.
     */
    public final int countDataSchemes() {
        return mDataSchemes != null ? mDataSchemes.size() : 0;
    }

    /**
     * Return a data scheme in the filter.
     */
    public final String getDataScheme(int index) {
        return mDataSchemes.get(index);
    }

    /**
     * Is the given data scheme included in the filter?  Note that if the
     * filter does not include any scheme, false will <em>always</em> be
     * returned.
     *
     * @param scheme The data scheme to look for.
     *
     * @return True if the scheme is explicitly mentioned in the filter.
     */
    public final boolean hasDataScheme(String scheme) {
        return mDataSchemes != null && mDataSchemes.contains(scheme);
    }

    /**
     * Return an iterator over the filter's data schemes.
     */
    public final Iterator<String> schemesIterator() {
        return mDataSchemes != null ? mDataSchemes.iterator() : null;
    }

    /**
     * This is an entry for a single authority in the Iterator returned by
     * {@link #authoritiesIterator()}.
     */
    public final static class AuthorityEntry {
        private final String mOrigHost;
        private final String mHost;
        private final boolean mWild;
        private final int mPort;

        public AuthorityEntry(String host, String port) {
            mOrigHost = host;
            mWild = host.length() > 0 && host.charAt(0) == '*';
            mHost = mWild ? host.substring(1).intern() : host;
            mPort = port != null ? Integer.parseInt(port) : -1;
        }

        AuthorityEntry(Parcel src) {
            mOrigHost = src.readString();
            mHost = src.readString();
            mWild = src.readInt() != 0;
            mPort = src.readInt();
        }

        void writeToParcel(Parcel dest) {
            dest.writeString(mOrigHost);
            dest.writeString(mHost);
            dest.writeInt(mWild ? 1 : 0);
            dest.writeInt(mPort);
        }

        void dumpDebug(ProtoOutputStream proto, long fieldId) {
            long token = proto.start(fieldId);
            // The original host information is already contained in host and wild, no output now.
            proto.write(AuthorityEntryProto.HOST, mHost);
            proto.write(AuthorityEntryProto.WILD, mWild);
            proto.write(AuthorityEntryProto.PORT, mPort);
            proto.end(token);
        }

        public String getHost() {
            return mOrigHost;
        }

        public int getPort() {
            return mPort;
        }

        /** @hide */
        public boolean match(AuthorityEntry other) {
            if (mWild != other.mWild) {
                return false;
            }
            if (!mHost.equals(other.mHost)) {
                return false;
            }
            if (mPort != other.mPort) {
                return false;
            }
            return true;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof AuthorityEntry) {
                final AuthorityEntry other = (AuthorityEntry)obj;
                return match(other);
            }
            return false;
        }

        /**
         * Determine whether this AuthorityEntry matches the given data Uri.
         * <em>Note that this comparison is case-sensitive, unlike formal
         * RFC host names.  You thus should always normalize to lower-case.</em>
         *
         * @param data The Uri to match.
         * @return Returns either {@link IntentFilter#NO_MATCH_DATA},
         * {@link IntentFilter#MATCH_CATEGORY_PORT}, or
         * {@link IntentFilter#MATCH_CATEGORY_HOST}.
         */
        public int match(Uri data) {
            return match(data, false);
        }

        /**
         * Variant of {@link #match(Uri)} that supports wildcards on the scheme, host and
         * path of the provided {@link Uri}
         *
         * @param wildcardSupported if true, will allow parameters to use wildcards
         * @hide
         */
        public int match(Uri data, boolean wildcardSupported) {
            String host = data.getHost();
            if (host == null) {
                if (wildcardSupported && mWild && mHost.isEmpty()) {
                    // special case, if no host is provided, but the Authority is wildcard, match
                    return MATCH_CATEGORY_HOST;
                } else {
                    return NO_MATCH_DATA;
                }
            }
            if (false) Log.v("IntentFilter",
                    "Match host " + host + ": " + mHost);
            if (!wildcardSupported || !WILDCARD.equals(host)) {
                if (mWild) {
                    if (host.length() < mHost.length()) {
                        return NO_MATCH_DATA;
                    }
                    host = host.substring(host.length() - mHost.length());
                }
                if (host.compareToIgnoreCase(mHost) != 0) {
                    return NO_MATCH_DATA;
                }
            }
            // if we're dealing with wildcard support, we ignore ports
            if (!wildcardSupported && mPort >= 0) {
                if (mPort != data.getPort()) {
                    return NO_MATCH_DATA;
                }
                return MATCH_CATEGORY_PORT;
            }
            return MATCH_CATEGORY_HOST;
        }
    }

    /**
     * Add a new Intent data "scheme specific part" to match against.  The filter must
     * include one or more schemes (via {@link #addDataScheme}) for the
     * scheme specific part to be considered.  If any scheme specific parts are
     * included in the filter, then an Intent's data must match one of
     * them.  If no scheme specific parts are included, then only the scheme must match.
     *
     * <p>The "scheme specific part" that this matches against is the string returned
     * by {@link android.net.Uri#getSchemeSpecificPart() Uri.getSchemeSpecificPart}.
     * For Uris that contain a path, this kind of matching is not generally of interest,
     * since {@link #addDataAuthority(String, String)} and
     * {@link #addDataPath(String, int)} can provide a better mechanism for matching
     * them.  However, for Uris that do not contain a path, the authority and path
     * are empty, so this is the only way to match against the non-scheme part.</p>
     *
     * @param ssp Either a raw string that must exactly match the scheme specific part
     * path, or a simple pattern, depending on <var>type</var>.
     * @param type Determines how <var>ssp</var> will be compared to
     * determine a match: either {@link PatternMatcher#PATTERN_LITERAL},
     * {@link PatternMatcher#PATTERN_PREFIX},
     * {@link PatternMatcher#PATTERN_SUFFIX}, or
     * {@link PatternMatcher#PATTERN_SIMPLE_GLOB}.
     *
     * @see #matchData
     * @see #addDataScheme
     */
    public final void addDataSchemeSpecificPart(String ssp, int type) {
        addDataSchemeSpecificPart(new PatternMatcher(ssp, type));
    }

    /** @hide */
    public final void addDataSchemeSpecificPart(PatternMatcher ssp) {
        if (mDataSchemeSpecificParts == null) {
            mDataSchemeSpecificParts = new ArrayList<PatternMatcher>();
        }
        mDataSchemeSpecificParts.add(ssp);
    }

    /**
     * Return the number of data scheme specific parts in the filter.
     */
    public final int countDataSchemeSpecificParts() {
        return mDataSchemeSpecificParts != null ? mDataSchemeSpecificParts.size() : 0;
    }

    /**
     * Return a data scheme specific part in the filter.
     */
    public final PatternMatcher getDataSchemeSpecificPart(int index) {
        return mDataSchemeSpecificParts.get(index);
    }

    /**
     * Is the given data scheme specific part included in the filter?  Note that if the
     * filter does not include any scheme specific parts, false will <em>always</em> be
     * returned.
     *
     * @param data The scheme specific part that is being looked for.
     *
     * @return Returns true if the data string matches a scheme specific part listed in the
     *         filter.
     */
    public final boolean hasDataSchemeSpecificPart(String data) {
        return hasDataSchemeSpecificPart(data, false);
    }

    /**
     * Variant of {@link #hasDataSchemeSpecificPart(String)} that supports wildcards on the provided
     * ssp.
     * @param supportWildcards if true, will allow parameters to use wildcards
     */
    private boolean hasDataSchemeSpecificPart(String data, boolean supportWildcards) {
        if (mDataSchemeSpecificParts == null) {
            return false;
        }
        if (supportWildcards && WILDCARD.equals(data) && mDataSchemeSpecificParts.size() > 0) {
            return true;
        }
        final int numDataSchemeSpecificParts = mDataSchemeSpecificParts.size();
        for (int i = 0; i < numDataSchemeSpecificParts; i++) {
            final PatternMatcher pe = mDataSchemeSpecificParts.get(i);
            if (pe.match(data)) {
                return true;
            }
        }
        return false;
    }

    /** @hide */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public final boolean hasDataSchemeSpecificPart(PatternMatcher ssp) {
        if (mDataSchemeSpecificParts == null) {
            return false;
        }
        final int numDataSchemeSpecificParts = mDataSchemeSpecificParts.size();
        for (int i = 0; i < numDataSchemeSpecificParts; i++) {
            final PatternMatcher pe = mDataSchemeSpecificParts.get(i);
            if (pe.getType() == ssp.getType() && pe.getPath().equals(ssp.getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return an iterator over the filter's data scheme specific parts.
     */
    public final Iterator<PatternMatcher> schemeSpecificPartsIterator() {
        return mDataSchemeSpecificParts != null ? mDataSchemeSpecificParts.iterator() : null;
    }

    /**
     * Add a new Intent data authority to match against.  The filter must
     * include one or more schemes (via {@link #addDataScheme}) for the
     * authority to be considered.  If any authorities are
     * included in the filter, then an Intent's data must match one of
     * them.  If no authorities are included, then only the scheme must match.
     *
     * <p><em>Note: host name in the Android framework is
     * case-sensitive, unlike formal RFC host names.  As a result,
     * you should always write your host names with lower case letters,
     * and any host names you receive from outside of Android should be
     * converted to lower case before supplying them here.</em></p>
     *
     * @param host The host part of the authority to match.  May start with a
     *             single '*' to wildcard the front of the host name.
     * @param port Optional port part of the authority to match.  If null, any
     *             port is allowed.
     *
     * @see #matchData
     * @see #addDataScheme
     */
    public final void addDataAuthority(String host, String port) {
        if (port != null) port = port.intern();
        addDataAuthority(new AuthorityEntry(host.intern(), port));
    }

    /** @hide */
    public final void addDataAuthority(AuthorityEntry ent) {
        if (mDataAuthorities == null) mDataAuthorities =
                new ArrayList<AuthorityEntry>();
        mDataAuthorities.add(ent);
    }

    /**
     * Return the number of data authorities in the filter.
     */
    public final int countDataAuthorities() {
        return mDataAuthorities != null ? mDataAuthorities.size() : 0;
    }

    /**
     * Return a data authority in the filter.
     */
    public final AuthorityEntry getDataAuthority(int index) {
        return mDataAuthorities.get(index);
    }

    /**
     * Is the given data authority included in the filter?  Note that if the
     * filter does not include any authorities, false will <em>always</em> be
     * returned.
     *
     * @param data The data whose authority is being looked for.
     *
     * @return Returns true if the data string matches an authority listed in the
     *         filter.
     */
    public final boolean hasDataAuthority(Uri data) {
        return matchDataAuthority(data) >= 0;
    }

    /** @hide */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public final boolean hasDataAuthority(AuthorityEntry auth) {
        if (mDataAuthorities == null) {
            return false;
        }
        final int numDataAuthorities = mDataAuthorities.size();
        for (int i = 0; i < numDataAuthorities; i++) {
            if (mDataAuthorities.get(i).match(auth)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return an iterator over the filter's data authorities.
     */
    public final Iterator<AuthorityEntry> authoritiesIterator() {
        return mDataAuthorities != null ? mDataAuthorities.iterator() : null;
    }

    /**
     * Add a new Intent data path to match against.  The filter must
     * include one or more schemes (via {@link #addDataScheme}) <em>and</em>
     * one or more authorities (via {@link #addDataAuthority}) for the
     * path to be considered.  If any paths are
     * included in the filter, then an Intent's data must match one of
     * them.  If no paths are included, then only the scheme/authority must
     * match.
     *
     * <p>The path given here can either be a literal that must directly
     * match or match against a prefix, or it can be a simple globbing pattern.
     * If the latter, you can use '*' anywhere in the pattern to match zero
     * or more instances of the previous character, '.' as a wildcard to match
     * any character, and '\' to escape the next character.
     *
     * @param path Either a raw string that must exactly match the file
     * path, or a simple pattern, depending on <var>type</var>.
     * @param type Determines how <var>path</var> will be compared to
     * determine a match: either {@link PatternMatcher#PATTERN_LITERAL},
     * {@link PatternMatcher#PATTERN_PREFIX},
     * {@link PatternMatcher#PATTERN_SUFFIX}, or
     * {@link PatternMatcher#PATTERN_SIMPLE_GLOB}.
     *
     * @see #matchData
     * @see #addDataScheme
     * @see #addDataAuthority
     */
    public final void addDataPath(String path, int type) {
        addDataPath(new PatternMatcher(path.intern(), type));
    }

    /** @hide */
    public final void addDataPath(PatternMatcher path) {
        if (mDataPaths == null) mDataPaths = new ArrayList<PatternMatcher>();
        mDataPaths.add(path);
    }

    /**
     * Return the number of data paths in the filter.
     */
    public final int countDataPaths() {
        return mDataPaths != null ? mDataPaths.size() : 0;
    }

    /**
     * Return a data path in the filter.
     */
    public final PatternMatcher getDataPath(int index) {
        return mDataPaths.get(index);
    }

    /**
     * Is the given data path included in the filter?  Note that if the
     * filter does not include any paths, false will <em>always</em> be
     * returned.
     *
     * @param data The data path to look for.  This is without the scheme
     *             prefix.
     *
     * @return True if the data string matches a path listed in the
     *         filter.
     */
    public final boolean hasDataPath(String data) {
        return hasDataPath(data, false);
    }

    /**
     * Variant of {@link #hasDataPath(String)} that supports wildcards on the provided scheme, host,
     * and path.
     * @param wildcardSupported if true, will allow parameters to use wildcards
     */
    private boolean hasDataPath(String data, boolean wildcardSupported) {
        if (mDataPaths == null) {
            return false;
        }
        if (wildcardSupported && WILDCARD_PATH.equals(data)) {
            return true;
        }
        final int numDataPaths = mDataPaths.size();
        for (int i = 0; i < numDataPaths; i++) {
            final PatternMatcher pe = mDataPaths.get(i);
            if (pe.match(data)) {
                return true;
            }
        }
        return false;
    }

    /** @hide */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public final boolean hasDataPath(PatternMatcher path) {
        if (mDataPaths == null) {
            return false;
        }
        final int numDataPaths = mDataPaths.size();
        for (int i = 0; i < numDataPaths; i++) {
            final PatternMatcher pe = mDataPaths.get(i);
            if (pe.getType() == path.getType() && pe.getPath().equals(path.getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return an iterator over the filter's data paths.
     */
    public final Iterator<PatternMatcher> pathsIterator() {
        return mDataPaths != null ? mDataPaths.iterator() : null;
    }

    /**
     * Add a new URI relative filter group to match against the Intent data.  The
     * intent filter must include one or more schemes (via {@link #addDataScheme})
     * <em>and</em> one or more authorities (via {@link #addDataAuthority}) for
     * the group to be considered.
     *
     * <p>Groups will be matched in the order they were added and matching will only
     * be done if no data paths match or if none are included. If both data paths and
     * groups are not included, then only the scheme/authority must match.</p>
     *
     * @param group A {@link UriRelativeFilterGroup} to match the URI.
     *
     * @see UriRelativeFilterGroup
     * @see #matchData
     * @see #addDataScheme
     * @see #addDataAuthority
     */
    @FlaggedApi(Flags.FLAG_RELATIVE_REFERENCE_INTENT_FILTERS)
    public final void addUriRelativeFilterGroup(@NonNull UriRelativeFilterGroup group) {
        Objects.requireNonNull(group);
        if (mUriRelativeFilterGroups == null) {
            mUriRelativeFilterGroups = new ArrayList<>();
        }
        mUriRelativeFilterGroups.add(group);
    }

    /**
     * Return the number of URI relative filter groups in the intent filter.
     */
    @FlaggedApi(Flags.FLAG_RELATIVE_REFERENCE_INTENT_FILTERS)
    public final int countUriRelativeFilterGroups() {
        return mUriRelativeFilterGroups == null ? 0 : mUriRelativeFilterGroups.size();
    }

    /**
     * Return a URI relative filter group in the intent filter.
     *
     * <p>Note: use of this method will result in a NullPointerException
     * if no groups exists for this intent filter.</p>
     *
     * @param index index of the element to return
     * @throws IndexOutOfBoundsException if index is out of range
     */
    @FlaggedApi(Flags.FLAG_RELATIVE_REFERENCE_INTENT_FILTERS)
    @NonNull
    public final UriRelativeFilterGroup getUriRelativeFilterGroup(int index) {
        return mUriRelativeFilterGroups.get(index);
    }

    /**
     * Removes all existing URI relative filter groups in the intent filter.
     */
    @FlaggedApi(Flags.FLAG_RELATIVE_REFERENCE_INTENT_FILTERS)
    public final void clearUriRelativeFilterGroups() {
        mUriRelativeFilterGroups = null;
    }

    /**
     * Match this intent filter against the given Intent data.  This ignores
     * the data scheme -- unlike {@link #matchData}, the authority will match
     * regardless of whether there is a matching scheme.
     *
     * @param data The data whose authority is being looked for.
     *
     * @return Returns either {@link #MATCH_CATEGORY_HOST},
     * {@link #MATCH_CATEGORY_PORT}, {@link #NO_MATCH_DATA}.
     */
    public final int matchDataAuthority(Uri data) {
        return matchDataAuthority(data, false);
    }

    /**
     * Variant of {@link #matchDataAuthority(Uri)} that allows wildcard matching of the provided
     * authority.
     *
     * @param wildcardSupported if true, will allow parameters to use wildcards
     * @hide
     */
    public final int matchDataAuthority(Uri data, boolean wildcardSupported) {
        if (data == null || mDataAuthorities == null) {
            return NO_MATCH_DATA;
        }
        final int numDataAuthorities = mDataAuthorities.size();
        for (int i = 0; i < numDataAuthorities; i++) {
            final AuthorityEntry ae = mDataAuthorities.get(i);
            int match = ae.match(data, wildcardSupported);
            if (match >= 0) {
                return match;
            }
        }
        return NO_MATCH_DATA;
    }

    /**
     * Match this filter against an Intent's data (type, scheme and path). If
     * the filter does not specify any types and does not specify any
     * schemes/paths, the match will only succeed if the intent does not
     * also specify a type or data.  If the filter does not specify any schemes,
     * it will implicitly match intents with no scheme, or the schemes "content:"
     * or "file:" (basically performing a MIME-type only match).  If the filter
     * does not specify any MIME types, the Intent also must not specify a MIME
     * type.
     *
     * <p>Be aware that to match against an authority, you must also specify a base
     * scheme the authority is in.  To match against a data path, both a scheme
     * and authority must be specified.  If the filter does not specify any
     * types or schemes that it matches against, it is considered to be empty
     * (any authority or data path given is ignored, as if it were empty as
     * well).
     *
     * <p><em>Note: MIME type, Uri scheme, and host name matching in the
     * Android framework is case-sensitive, unlike the formal RFC definitions.
     * As a result, you should always write these elements with lower case letters,
     * and normalize any MIME types or Uris you receive from
     * outside of Android to ensure these elements are lower case before
     * supplying them here.</em></p>
     *
     * @param type The desired data type to look for, as returned by
     *             Intent.resolveType().
     * @param scheme The desired data scheme to look for, as returned by
     *               Intent.getScheme().
     * @param data The full data string to match against, as supplied in
     *             Intent.data.
     *
     * @return Returns either a valid match constant (a combination of
     * {@link #MATCH_CATEGORY_MASK} and {@link #MATCH_ADJUSTMENT_MASK}),
     * or one of the error codes {@link #NO_MATCH_TYPE} if the type didn't match
     * or {@link #NO_MATCH_DATA} if the scheme/path didn't match.
     *
     * @see #match
     */
    public final int matchData(String type, String scheme, Uri data) {
        return matchData(type, scheme, data, false);
    }

    /**
     * Variant of {@link #matchData(String, String, Uri)} that allows wildcard matching
     * of the provided type, scheme, host and path parameters.
     * @param wildcardSupported if true, will allow parameters to use wildcards
     */
    private int matchData(String type, String scheme, Uri data, boolean wildcardSupported) {
        final boolean wildcardWithMimegroups = wildcardSupported && countMimeGroups() != 0;
        final List<String> types = mDataTypes;
        final ArrayList<String> schemes = mDataSchemes;

        int match = MATCH_CATEGORY_EMPTY;

        if (!wildcardWithMimegroups && types == null && schemes == null) {
            return ((type == null && data == null)
                ? (MATCH_CATEGORY_EMPTY+MATCH_ADJUSTMENT_NORMAL) : NO_MATCH_DATA);
        }

        if (schemes != null) {
            if (schemes.contains(scheme != null ? scheme : "")
                    || wildcardSupported && WILDCARD.equals(scheme)) {
                match = MATCH_CATEGORY_SCHEME;
            } else {
                return NO_MATCH_DATA;
            }

            final ArrayList<PatternMatcher> schemeSpecificParts = mDataSchemeSpecificParts;
            if (schemeSpecificParts != null && data != null) {
                match = hasDataSchemeSpecificPart(data.getSchemeSpecificPart(), wildcardSupported)
                        ? MATCH_CATEGORY_SCHEME_SPECIFIC_PART : NO_MATCH_DATA;
            }
            if (match != MATCH_CATEGORY_SCHEME_SPECIFIC_PART) {
                // If there isn't any matching ssp, we need to match an authority.
                final ArrayList<AuthorityEntry> authorities = mDataAuthorities;
                if (authorities != null) {
                    int authMatch = matchDataAuthority(data, wildcardSupported);
                    if (authMatch >= 0) {
                        final ArrayList<PatternMatcher> paths = mDataPaths;
                        final ArrayList<UriRelativeFilterGroup> groups = mUriRelativeFilterGroups;
                        if (Flags.relativeReferenceIntentFilters()) {
                            if (paths == null && groups == null) {
                                match = authMatch;
                            } else if (hasDataPath(data.getPath(), wildcardSupported)
                                    || matchRelRefGroups(data)) {
                                match = MATCH_CATEGORY_PATH;
                            } else {
                                return NO_MATCH_DATA;
                            }
                        } else {
                            if (paths == null) {
                                match = authMatch;
                            } else if (hasDataPath(data.getPath(), wildcardSupported)) {
                                match = MATCH_CATEGORY_PATH;
                            } else {
                                return NO_MATCH_DATA;
                            }
                        }
                    } else {
                        return NO_MATCH_DATA;
                    }
                }
            }
            // If neither an ssp nor an authority matched, we're done.
            if (match == NO_MATCH_DATA) {
                return NO_MATCH_DATA;
            }
        } else {
            // Special case: match either an Intent with no data URI,
            // or with a scheme: URI.  This is to give a convenience for
            // the common case where you want to deal with data in a
            // content provider, which is done by type, and we don't want
            // to force everyone to say they handle content: or file: URIs.
            if (scheme != null && !"".equals(scheme)
                    && !"content".equals(scheme)
                    && !"file".equals(scheme)
                    && !(wildcardSupported && WILDCARD.equals(scheme))) {
                return NO_MATCH_DATA;
            }
        }

        if (wildcardWithMimegroups) {
            return MATCH_CATEGORY_TYPE;
        } else if (types != null) {
            if (findMimeType(type)) {
                match = MATCH_CATEGORY_TYPE;
            } else {
                return NO_MATCH_TYPE;
            }
        } else {
            // If no MIME types are specified, then we will only match against
            // an Intent that does not have a MIME type.
            if (type != null) {
                return NO_MATCH_TYPE;
            }
        }

        return match + MATCH_ADJUSTMENT_NORMAL;
    }

    private boolean matchRelRefGroups(Uri data) {
        if (mUriRelativeFilterGroups == null) {
            return false;
        }
        return UriRelativeFilterGroup.matchGroupsToUri(mUriRelativeFilterGroups, data);
    }

    /**
     * Add a new Intent category to match against.  The semantics of
     * categories is the opposite of actions -- an Intent includes the
     * categories that it requires, all of which must be included in the
     * filter in order to match.  In other words, adding a category to the
     * filter has no impact on matching unless that category is specified in
     * the intent.
     *
     * @param category Name of category to match, such as Intent.CATEGORY_EMBED.
     */
    public final void addCategory(String category) {
        if (mCategories == null) mCategories = new ArrayList<String>();
        if (!mCategories.contains(category)) {
            mCategories.add(category.intern());
        }
    }

    /**
     * Return the number of categories in the filter.
     */
    public final int countCategories() {
        return mCategories != null ? mCategories.size() : 0;
    }

    /**
     * Return a category in the filter.
     */
    public final String getCategory(int index) {
        return mCategories.get(index);
    }

    /**
     * Is the given category included in the filter?
     *
     * @param category The category that the filter supports.
     *
     * @return True if the category is explicitly mentioned in the filter.
     */
    public final boolean hasCategory(String category) {
        return mCategories != null && mCategories.contains(category);
    }

    /**
     * Return an iterator over the filter's categories.
     *
     * @return Iterator if this filter has categories or {@code null} if none.
     */
    public final Iterator<String> categoriesIterator() {
        return mCategories != null ? mCategories.iterator() : null;
    }

    /**
     * Match this filter against an Intent's categories.  Each category in
     * the Intent must be specified by the filter; if any are not in the
     * filter, the match fails.
     *
     * @param categories The categories included in the intent, as returned by
     *                   Intent.getCategories().
     *
     * @return If all categories match (success), null; else the name of the
     *         first category that didn't match.
     */
    public final String matchCategories(Set<String> categories) {
        if (categories == null) {
            return null;
        }

        Iterator<String> it = categories.iterator();

        if (mCategories == null) {
            return it.hasNext() ? it.next() : null;
        }

        while (it.hasNext()) {
            final String category = it.next();
            if (!mCategories.contains(category)) {
                return category;
            }
        }

        return null;
    }

    /**
     * Match this filter against an Intent's extras. An intent must
     * have all the extras specified by the filter with the same values,
     * for the match to succeed.
     *
     * <p> An intent con have other extras in addition to those specified
     * by the filter and it would not affect whether the match succeeds or not.
     *
     * @param extras The extras included in the intent, as returned by
     *               Intent.getExtras().
     *
     * @return If all extras match (success), null; else the name of the
     *         first extra that didn't match.
     *
     * @hide
     */
    private String matchExtras(@Nullable Bundle extras) {
        if (mExtras == null) {
            return null;
        }
        final Set<String> keys = mExtras.keySet();
        for (String key : keys) {
            if (extras == null) {
                return key;
            }
            final Object value = mExtras.get(key);
            final Object otherValue = extras.get(key);
            if (otherValue == null || value.getClass() != otherValue.getClass()
                    || !Objects.deepEquals(value, otherValue)) {
                return key;
            }
        }
        return null;
    }

    /**
     * Add a new extra name and value to match against. If an extra is included in the filter,
     * then an Intent must have an extra with the same {@code name} and {@code value} for it to
     * match.
     *
     * <p> Subsequent calls to this method with the same {@code name} will override any previously
     * set {@code value}.
     *
     * @param name the name of the extra to match against.
     * @param value the value of the extra to match against.
     *
     * @hide
     */
    public final void addExtra(@NonNull String name, int value) {
        Objects.requireNonNull(name);
        if (mExtras == null) {
            mExtras = new PersistableBundle();
        }
        mExtras.putInt(name, value);
    }

    /**
     * Return the value of the extra with {@code name} included in the filter.
     *
     * @return the value that was previously set using {@link #addExtra(String, int)} or
     *         {@code 0} if no value has been set.
     * @hide
     */
    public final int getIntExtra(@NonNull String name) {
        Objects.requireNonNull(name);
        return mExtras == null ? 0 : mExtras.getInt(name);
    }

    /**
     * Add a new extra name and value to match against. If an extra is included in the filter,
     * then an Intent must have an extra with the same {@code name} and {@code value} for it to
     * match.
     *
     * <p> Subsequent calls to this method with the same {@code name} will override any previously
     * set {@code value}.
     *
     * @param name the name of the extra to match against.
     * @param value the value of the extra to match against.
     *
     * @hide
     */
    public final void addExtra(@NonNull String name, @NonNull int[] value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        if (mExtras == null) {
            mExtras = new PersistableBundle();
        }
        mExtras.putIntArray(name, value);
    }

    /**
     * Return the value of the extra with {@code name} included in the filter.
     *
     * @return the value that was previously set using {@link #addExtra(String, int[])} or
     *         an empty int array if no value has been set.
     * @hide
     */
    @NonNull
    public final int[] getIntArrayExtra(@NonNull String name) {
        Objects.requireNonNull(name);
        return mExtras == null ? EMPTY_INT_ARRAY : mExtras.getIntArray(name);
    }

    /**
     * Add a new extra name and value to match against. If an extra is included in the filter,
     * then an Intent must have an extra with the same {@code name} and {@code value} for it to
     * match.
     *
     * <p> Subsequent calls to this method with the same {@code name} will override any previously
     * set {@code value}.
     *
     * @param name the name of the extra to match against.
     * @param value the value of the extra to match against.
     *
     * @hide
     */
    public final void addExtra(@NonNull String name, long value) {
        Objects.requireNonNull(name);
        if (mExtras == null) {
            mExtras = new PersistableBundle();
        }
        mExtras.putLong(name, value);
    }

    /**
     * Return the value of the extra with {@code name} included in the filter.
     *
     * @return the value that was previously set using {@link #addExtra(String, long)} or
     *         {@code 0L} if no value has been set.
     * @hide
     */
    public final long getLongExtra(@NonNull String name) {
        Objects.requireNonNull(name);
        return mExtras == null ? 0L : mExtras.getLong(name);
    }

    /**
     * Add a new extra name and value to match against. If an extra is included in the filter,
     * then an Intent must have an extra with the same {@code name} and {@code value} for it to
     * match.
     *
     * <p> Subsequent calls to this method with the same {@code name} will override any previously
     * set {@code value}.
     *
     * @param name the name of the extra to match against.
     * @param value the value of the extra to match against.
     *
     * @hide
     */
    public final void addExtra(@NonNull String name, @NonNull long[] value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        if (mExtras == null) {
            mExtras = new PersistableBundle();
        }
        mExtras.putLongArray(name, value);
    }

    /**
     * Return the value of the extra with {@code name} included in the filter.
     *
     * @return the value that was previously set using {@link #addExtra(String, long[])} or
     *         an empty long array if no value has been set.
     * @hide
     */
    @NonNull
    public final long[] getLongArrayExtra(@NonNull String name) {
        Objects.requireNonNull(name);
        return mExtras == null ? EMPTY_LONG_ARRAY : mExtras.getLongArray(name);
    }

    /**
     * Add a new extra name and value to match against. If an extra is included in the filter,
     * then an Intent must have an extra with the same {@code name} and {@code value} for it to
     * match.
     *
     * <p> Subsequent calls to this method with the same {@code name} will override any previously
     * set {@code value}.
     *
     * @param name the name of the extra to match against.
     * @param value the value of the extra to match against.
     *
     * @hide
     */
    public final void addExtra(@NonNull String name, double value) {
        Objects.requireNonNull(name);
        if (mExtras == null) {
            mExtras = new PersistableBundle();
        }
        mExtras.putDouble(name, value);
    }

    /**
     * Return the value of the extra with {@code name} included in the filter.
     *
     * @return the value that was previously set using {@link #addExtra(String, double)} or
     *         {@code 0.0} if no value has been set.
     * @hide
     */
    @NonNull
    public final double getDoubleExtra(@NonNull String name) {
        Objects.requireNonNull(name);
        return mExtras == null ? 0.0 : mExtras.getDouble(name);
    }

    /**
     * Add a new extra name and value to match against. If an extra is included in the filter,
     * then an Intent must have an extra with the same {@code name} and {@code value} for it to
     * match.
     *
     * <p> Subsequent calls to this method with the same {@code name} will override any previously
     * set {@code value}.
     *
     * @param name the name of the extra to match against.
     * @param value the value of the extra to match against.
     *
     * @hide
     */
    public final void addExtra(@NonNull String name, @NonNull double[] value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        if (mExtras == null) {
            mExtras = new PersistableBundle();
        }
        mExtras.putDoubleArray(name, value);
    }

    /**
     * Return the value of the extra with {@code name} included in the filter.
     *
     * @return the value that was previously set using {@link #addExtra(String, double[])} or
     *         an empty double array if no value has been set.
     * @hide
     */
    @NonNull
    public final double[] getDoubleArrayExtra(@NonNull String name) {
        Objects.requireNonNull(name);
        return mExtras == null ? EMPTY_DOUBLE_ARRAY : mExtras.getDoubleArray(name);
    }

    /**
     * Add a new extra name and value to match against. If an extra is included in the filter,
     * then an Intent must have an extra with the same {@code name} and {@code value} for it to
     * match.
     *
     * <p> Subsequent calls to this method with the same {@code name} will override any previously
     * set {@code value}.
     *
     * @param name the name of the extra to match against.
     * @param value the value of the extra to match against.
     *
     * @hide
     */
    public final void addExtra(@NonNull String name, @NonNull String value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        if (mExtras == null) {
            mExtras = new PersistableBundle();
        }
        mExtras.putString(name, value);
    }

    /**
     * Return the value of the extra with {@code name} included in the filter.
     *
     * @return the value that was previously set using {@link #addExtra(String, String)} or a
     *         {@code null} if no value has been set.
     * @hide
     */
    @Nullable
    public final String getStringExtra(@NonNull String name) {
        Objects.requireNonNull(name);
        return mExtras == null ? null : mExtras.getString(name);
    }

    /**
     * Add a new extra name and value to match against. If an extra is included in the filter,
     * then an Intent must have an extra with the same {@code name} and {@code value} for it to
     * match.
     *
     * <p> Subsequent calls to this method with the same {@code name} will override any previously
     * set {@code value}.
     *
     * @param name the name of the extra to match against.
     * @param value the value of the extra to match against.
     *
     * @hide
     */
    public final void addExtra(@NonNull String name, @NonNull String[] value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        if (mExtras == null) {
            mExtras = new PersistableBundle();
        }
        mExtras.putStringArray(name, value);
    }

    /**
     * Return the value of the extra with {@code name} included in the filter.
     *
     * @return the value that was previously set using {@link #addExtra(String, String[])} or
     *         an empty string array if no value has been set.
     * @hide
     */
    @NonNull
    public final String[] getStringArrayExtra(@NonNull String name) {
        Objects.requireNonNull(name);
        return mExtras == null ? EMPTY_STRING_ARRAY : mExtras.getStringArray(name);
    }

    /**
     * Add a new extra name and value to match against. If an extra is included in the filter,
     * then an Intent must have an extra with the same {@code name} and {@code value} for it to
     * match.
     *
     * <p> Subsequent calls to this method with the same {@code name} will override any previously
     * set {@code value}.
     *
     * @param name the name of the extra to match against.
     * @param value the value of the extra to match against.
     *
     * @hide
     */
    public final void addExtra(@NonNull String name, boolean value) {
        Objects.requireNonNull(name);
        if (mExtras == null) {
            mExtras = new PersistableBundle();
        }
        mExtras.putBoolean(name, value);
    }

    /**
     * Return the value of the extra with {@code name} included in the filter.
     *
     * @return the value that was previously set using {@link #addExtra(String, boolean)} or
     *         {@code false} if no value has been set.
     * @hide
     */
    public final boolean getBooleanExtra(@NonNull String name) {
        Objects.requireNonNull(name);
        return mExtras == null ? false : mExtras.getBoolean(name);
    }

    /**
     * Add a new extra name and value to match against. If an extra is included in the filter,
     * then an Intent must have an extra with the same {@code name} and {@code value} for it to
     * match.
     *
     * <p> Subsequent calls to this method with the same {@code name} will override any previously
     * set {@code value}.
     *
     * @param name the name of the extra to match against.
     * @param value the value of the extra to match against.
     *
     * @hide
     */
    public final void addExtra(@NonNull String name, @NonNull boolean[] value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        if (mExtras == null) {
            mExtras = new PersistableBundle();
        }
        mExtras.putBooleanArray(name, value);
    }

    /**
     * Return the value of the extra with {@code name} included in the filter.
     *
     * @return the value that was previously set using {@link #addExtra(String, boolean[])} or
     *         an empty boolean array if no value has been set.
     * @hide
     */
    @NonNull
    public final boolean[] getBooleanArrayExtra(@NonNull String name) {
        Objects.requireNonNull(name);
        return mExtras == null ? EMPTY_BOOLEAN_ARRAY : mExtras.getBooleanArray(name);
    }

    /**
     * Returns whether or not an extra with {@code name} is included in the filter.
     *
     * @return {@code true} if an extra with {@code name} is included in the filter.
     *         Otherwise {@code false}.
     * @hide
     */
    public final boolean hasExtra(@NonNull String name) {
        Objects.requireNonNull(name);
        return mExtras == null ? false : mExtras.containsKey(name);
    }

    /**
     * Set the intent extras to match against. For this filter to match an
     * intent, it must contain all the {@code extras} set.
     *
     * <p> Subsequent calls to this method  will override any previously set extras.
     *
     * @param extras The intent extras to match against.
     * @hide
     */
    public final void setExtras(@NonNull PersistableBundle extras) {
        mExtras = extras;
    }

    /**
     * Return the intent extras included in this filter.
     *
     * @return the extras that were previously set using {@link #setExtras(PersistableBundle)} or
     *         an empty {@link PersistableBundle} object if no extras were set.
     * @hide
     */
    public final @NonNull PersistableBundle getExtras() {
        return mExtras == null ? new PersistableBundle() : mExtras;
    }

    /**
     * Return a {@link Predicate} which tests whether this filter matches the
     * given <var>intent</var>.
     * <p>
     * The intent's type will always be tested using a simple
     * {@link Intent#getType()} check. To instead perform a detailed type
     * resolution before matching, use
     * {@link #asPredicateWithTypeResolution(ContentResolver)}.
     */
    public @NonNull Predicate<Intent> asPredicate() {
        return i -> match(null, i, false, TAG) >= 0;
    }

    /**
     * Return a {@link Predicate} which tests whether this filter matches the
     * given <var>intent</var>.
     * <p>
     * The intent's type will always be resolved by calling
     * {@link Intent#resolveType(ContentResolver)} before matching.
     *
     * @param resolver to be used when calling
     *            {@link Intent#resolveType(ContentResolver)} before matching.
     */
    public @NonNull Predicate<Intent> asPredicateWithTypeResolution(
            @NonNull ContentResolver resolver) {
        Objects.requireNonNull(resolver);
        return i -> match(resolver, i, true, TAG) >= 0;
    }

    /**
     * Test whether this filter matches the given <var>intent</var>.
     *
     * @param intent The Intent to compare against.
     * @param resolve If true, the intent's type will be resolved by calling
     *                Intent.resolveType(); otherwise a simple match against
     *                Intent.type will be performed.
     * @param logTag Tag to use in debugging messages.
     *
     * @return Returns either a valid match constant (a combination of
     * {@link #MATCH_CATEGORY_MASK} and {@link #MATCH_ADJUSTMENT_MASK}),
     * or one of the error codes {@link #NO_MATCH_TYPE} if the type didn't match,
     * {@link #NO_MATCH_DATA} if the scheme/path didn't match,
     * {@link #NO_MATCH_ACTION} if the action didn't match, or
     * {@link #NO_MATCH_CATEGORY} if one or more categories didn't match.
     *
     * @see #match(String, String, String, android.net.Uri , Set, String)
     */
    public final int match(ContentResolver resolver, Intent intent,
            boolean resolve, String logTag) {
        String type = resolve ? intent.resolveType(resolver) : intent.getType();
        return match(intent.getAction(), type, intent.getScheme(),
                     intent.getData(), intent.getCategories(), logTag,
                     false /* supportWildcards */, null /* ignoreActions */,
                     intent.getExtras());
    }

    /**
     * Test whether this filter matches the given intent data.  A match is
     * only successful if the actions and categories in the Intent match
     * against the filter, as described in {@link IntentFilter}; in that case,
     * the match result returned will be as per {@link #matchData}.
     *
     * @param action The intent action to match against (Intent.getAction).
     * @param type The intent type to match against (Intent.resolveType()).
     * @param scheme The data scheme to match against (Intent.getScheme()).
     * @param data The data URI to match against (Intent.getData()).
     * @param categories The categories to match against
     *                   (Intent.getCategories()).
     * @param logTag Tag to use in debugging messages.
     *
     * @return Returns either a valid match constant (a combination of
     * {@link #MATCH_CATEGORY_MASK} and {@link #MATCH_ADJUSTMENT_MASK}),
     * or one of the error codes {@link #NO_MATCH_TYPE} if the type didn't match,
     * {@link #NO_MATCH_DATA} if the scheme/path didn't match,
     * {@link #NO_MATCH_ACTION} if the action didn't match, or
     * {@link #NO_MATCH_CATEGORY} if one or more categories didn't match.
     *
     * @see #matchData
     * @see Intent#getAction
     * @see Intent#resolveType
     * @see Intent#getScheme
     * @see Intent#getData
     * @see Intent#getCategories
     */
    public final int match(String action, String type, String scheme,
            Uri data, Set<String> categories, String logTag) {
        return match(action, type, scheme, data, categories, logTag, false /*supportWildcards*/,
                null /*ignoreActions*/);
    }

    /**
     * Variant of {@link #match(ContentResolver, Intent, boolean, String)} that supports wildcards
     * in the action, type, scheme, host and path.
     * @param supportWildcards if true, will allow supported parameters to use wildcards to match
     *                         this filter
     * @param ignoreActions a collection of actions that, if not null should be ignored and not used
     *                      if provided as the matching action or the set of actions defined by this
     *                      filter
     * @hide
     */
    public final int match(String action, String type, String scheme,
            Uri data, Set<String> categories, String logTag, boolean supportWildcards,
            @Nullable Collection<String> ignoreActions) {
        return match(action, type, scheme, data, categories, logTag, supportWildcards,
                ignoreActions, null /* extras */);
    }

    /**
     * Variant of {@link #match(String, String, String, Uri, Set, String, boolean, Collection)}
     * that supports matching the extra values in the intent.
     *
     * @hide
     */
    public final int match(String action, String type, String scheme,
            Uri data, Set<String> categories, String logTag, boolean supportWildcards,
            @Nullable Collection<String> ignoreActions, @Nullable Bundle extras) {
        if (action != null && !matchAction(action, supportWildcards, ignoreActions)) {
            if (false) Log.v(
                logTag, "No matching action " + action + " for " + this);
            return NO_MATCH_ACTION;
        }

        int dataMatch = matchData(type, scheme, data, supportWildcards);
        if (dataMatch < 0) {
            if (false) {
                if (dataMatch == NO_MATCH_TYPE) {
                    Log.v(logTag, "No matching type " + type
                          + " for " + this);
                }
                if (dataMatch == NO_MATCH_DATA) {
                    Log.v(logTag, "No matching scheme/path " + data
                          + " for " + this);
                }
            }
            return dataMatch;
        }

        String categoryMismatch = matchCategories(categories);
        if (categoryMismatch != null) {
            if (false) {
                Log.v(logTag, "No matching category " + categoryMismatch + " for " + this);
            }
            return NO_MATCH_CATEGORY;
        }

        String extraMismatch = matchExtras(extras);
        if (extraMismatch != null) {
            if (false) {
                Log.v(logTag, "Mismatch for extra key " + extraMismatch + " for " + this);
            }
            return NO_MATCH_EXTRAS;
        }

        // It would be nice to treat container activities as more
        // important than ones that can be embedded, but this is not the way...
        if (false) {
            if (categories != null) {
                dataMatch -= mCategories.size() - categories.size();
            }
        }

        return dataMatch;
    }

    /**
     * Write the contents of the IntentFilter as an XML stream.
     */
    public void writeToXml(XmlSerializer serializer) throws IOException {

        if (getAutoVerify()) {
            serializer.attribute(null, AUTO_VERIFY_STR, Boolean.toString(true));
        }

        int N = countActions();
        for (int i=0; i<N; i++) {
            serializer.startTag(null, ACTION_STR);
            serializer.attribute(null, NAME_STR, mActions.valueAt(i));
            serializer.endTag(null, ACTION_STR);
        }
        N = countCategories();
        for (int i=0; i<N; i++) {
            serializer.startTag(null, CAT_STR);
            serializer.attribute(null, NAME_STR, mCategories.get(i));
            serializer.endTag(null, CAT_STR);
        }
        writeDataTypesToXml(serializer);
        N = countMimeGroups();
        for (int i=0; i<N; i++) {
            serializer.startTag(null, GROUP_STR);
            serializer.attribute(null, NAME_STR, mMimeGroups.get(i));
            serializer.endTag(null, GROUP_STR);
        }
        N = countDataSchemes();
        for (int i=0; i<N; i++) {
            serializer.startTag(null, SCHEME_STR);
            serializer.attribute(null, NAME_STR, mDataSchemes.get(i));
            serializer.endTag(null, SCHEME_STR);
        }
        N = countDataSchemeSpecificParts();
        for (int i=0; i<N; i++) {
            serializer.startTag(null, SSP_STR);
            PatternMatcher pe = mDataSchemeSpecificParts.get(i);
            switch (pe.getType()) {
                case PatternMatcher.PATTERN_LITERAL:
                    serializer.attribute(null, LITERAL_STR, pe.getPath());
                    break;
                case PatternMatcher.PATTERN_PREFIX:
                    serializer.attribute(null, PREFIX_STR, pe.getPath());
                    break;
                case PatternMatcher.PATTERN_SIMPLE_GLOB:
                    serializer.attribute(null, SGLOB_STR, pe.getPath());
                    break;
                case PatternMatcher.PATTERN_ADVANCED_GLOB:
                    serializer.attribute(null, AGLOB_STR, pe.getPath());
                    break;
                case PatternMatcher.PATTERN_SUFFIX:
                    serializer.attribute(null, SUFFIX_STR, pe.getPath());
                    break;
            }
            serializer.endTag(null, SSP_STR);
        }
        N = countDataAuthorities();
        for (int i=0; i<N; i++) {
            serializer.startTag(null, AUTH_STR);
            AuthorityEntry ae = mDataAuthorities.get(i);
            serializer.attribute(null, HOST_STR, ae.getHost());
            if (ae.getPort() >= 0) {
                serializer.attribute(null, PORT_STR, Integer.toString(ae.getPort()));
            }
            serializer.endTag(null, AUTH_STR);
        }
        N = countDataPaths();
        for (int i=0; i<N; i++) {
            serializer.startTag(null, PATH_STR);
            PatternMatcher pe = mDataPaths.get(i);
            switch (pe.getType()) {
                case PatternMatcher.PATTERN_LITERAL:
                    serializer.attribute(null, LITERAL_STR, pe.getPath());
                    break;
                case PatternMatcher.PATTERN_PREFIX:
                    serializer.attribute(null, PREFIX_STR, pe.getPath());
                    break;
                case PatternMatcher.PATTERN_SIMPLE_GLOB:
                    serializer.attribute(null, SGLOB_STR, pe.getPath());
                    break;
                case PatternMatcher.PATTERN_ADVANCED_GLOB:
                    serializer.attribute(null, AGLOB_STR, pe.getPath());
                    break;
                case PatternMatcher.PATTERN_SUFFIX:
                    serializer.attribute(null, SUFFIX_STR, pe.getPath());
                    break;
            }
            serializer.endTag(null, PATH_STR);
        }
        if (mExtras != null) {
            serializer.startTag(null, EXTRAS_STR);
            try {
                mExtras.saveToXml(serializer);
            } catch (XmlPullParserException e) {
                throw new IllegalStateException("Failed to write extras: " + mExtras.toString(), e);
            }
            serializer.endTag(null, EXTRAS_STR);
        }
        if (Flags.relativeReferenceIntentFilters()) {
            N = countUriRelativeFilterGroups();
            for (int i = 0; i < N; i++) {
                mUriRelativeFilterGroups.get(i).writeToXml(serializer);
            }
        }
    }

    /**
     * Write data types (both static and dynamic) to XML.
     * In implementation we rely on two facts:
     * - {@link #mStaticDataTypes} is subsequence of {@link #mDataTypes}
     * - both {@link #mStaticDataTypes} and {@link #mDataTypes} does not contain duplicates
     */
    private void writeDataTypesToXml(XmlSerializer serializer) throws IOException {
        if (mStaticDataTypes == null) {
            return;
        }

        int i = 0;
        for (String staticType: mStaticDataTypes) {
            while (!mDataTypes.get(i).equals(staticType)) {
                writeDataTypeToXml(serializer, mDataTypes.get(i), TYPE_STR);
                i++;
            }

            writeDataTypeToXml(serializer, staticType, STATIC_TYPE_STR);
            i++;
        }

        while (i < mDataTypes.size()) {
            writeDataTypeToXml(serializer, mDataTypes.get(i), TYPE_STR);
            i++;
        }
    }

    private void writeDataTypeToXml(XmlSerializer serializer, String type, String tag)
            throws IOException {
        serializer.startTag(null, tag);

        if (type.indexOf('/') < 0) {
            type = type + "/*";
        }

        serializer.attribute(null, NAME_STR, type);
        serializer.endTag(null, tag);
    }

    public void readFromXml(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        String autoVerify = parser.getAttributeValue(null, AUTO_VERIFY_STR);
        setAutoVerify(TextUtils.isEmpty(autoVerify) ? false : Boolean.getBoolean(autoVerify));

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
            if (tagName.equals(ACTION_STR)) {
                String name = parser.getAttributeValue(null, NAME_STR);
                if (name != null) {
                    addAction(name);
                }
            } else if (tagName.equals(CAT_STR)) {
                String name = parser.getAttributeValue(null, NAME_STR);
                if (name != null) {
                    addCategory(name);
                }
            } else if (tagName.equals(STATIC_TYPE_STR)) {
                String name = parser.getAttributeValue(null, NAME_STR);
                if (name != null) {
                    try {
                        addDataType(name);
                    } catch (MalformedMimeTypeException e) {
                    }
                }
            } else if (tagName.equals(TYPE_STR)) {
                String name = parser.getAttributeValue(null, NAME_STR);
                if (name != null) {
                    try {
                        addDynamicDataType(name);
                    } catch (MalformedMimeTypeException e) {
                    }
                }
            } else if (tagName.equals(GROUP_STR)) {
                String name = parser.getAttributeValue(null, NAME_STR);
                if (name != null) {
                    addMimeGroup(name);
                }
            } else if (tagName.equals(SCHEME_STR)) {
                String name = parser.getAttributeValue(null, NAME_STR);
                if (name != null) {
                    addDataScheme(name);
                }
            } else if (tagName.equals(SSP_STR)) {
                String ssp = parser.getAttributeValue(null, LITERAL_STR);
                if (ssp != null) {
                    addDataSchemeSpecificPart(ssp, PatternMatcher.PATTERN_LITERAL);
                } else if ((ssp=parser.getAttributeValue(null, PREFIX_STR)) != null) {
                    addDataSchemeSpecificPart(ssp, PatternMatcher.PATTERN_PREFIX);
                } else if ((ssp=parser.getAttributeValue(null, SGLOB_STR)) != null) {
                    addDataSchemeSpecificPart(ssp, PatternMatcher.PATTERN_SIMPLE_GLOB);
                } else if ((ssp=parser.getAttributeValue(null, AGLOB_STR)) != null) {
                    addDataSchemeSpecificPart(ssp, PatternMatcher.PATTERN_ADVANCED_GLOB);
                } else if ((ssp=parser.getAttributeValue(null, SUFFIX_STR)) != null) {
                    addDataSchemeSpecificPart(ssp, PatternMatcher.PATTERN_SUFFIX);
                }
            } else if (tagName.equals(AUTH_STR)) {
                String host = parser.getAttributeValue(null, HOST_STR);
                String port = parser.getAttributeValue(null, PORT_STR);
                if (host != null) {
                    addDataAuthority(host, port);
                }
            } else if (tagName.equals(PATH_STR)) {
                String path = parser.getAttributeValue(null, LITERAL_STR);
                if (path != null) {
                    addDataPath(path, PatternMatcher.PATTERN_LITERAL);
                } else if ((path=parser.getAttributeValue(null, PREFIX_STR)) != null) {
                    addDataPath(path, PatternMatcher.PATTERN_PREFIX);
                } else if ((path=parser.getAttributeValue(null, SGLOB_STR)) != null) {
                    addDataPath(path, PatternMatcher.PATTERN_SIMPLE_GLOB);
                } else if ((path=parser.getAttributeValue(null, AGLOB_STR)) != null) {
                    addDataPath(path, PatternMatcher.PATTERN_ADVANCED_GLOB);
                } else if ((path=parser.getAttributeValue(null, SUFFIX_STR)) != null) {
                    addDataPath(path, PatternMatcher.PATTERN_SUFFIX);
                }
            } else if (tagName.equals(EXTRAS_STR)) {
                mExtras = PersistableBundle.restoreFromXml(parser);
            } else if (Flags.relativeReferenceIntentFilters()
                    && URI_RELATIVE_FILTER_GROUP_STR.equals(tagName)) {
                addUriRelativeFilterGroup(new UriRelativeFilterGroup(parser));
            } else {
                Log.w("IntentFilter", "Unknown tag parsing IntentFilter: " + tagName);
            }
            XmlUtils.skipCurrentTag(parser);
        }
    }

    /** @hide */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        if (mActions.size() > 0) {
            Iterator<String> it = mActions.iterator();
            while (it.hasNext()) {
                proto.write(IntentFilterProto.ACTIONS, it.next());
            }
        }
        if (mCategories != null) {
            Iterator<String> it = mCategories.iterator();
            while (it.hasNext()) {
                proto.write(IntentFilterProto.CATEGORIES, it.next());
            }
        }
        if (mDataSchemes != null) {
            Iterator<String> it = mDataSchemes.iterator();
            while (it.hasNext()) {
                proto.write(IntentFilterProto.DATA_SCHEMES, it.next());
            }
        }
        if (mDataSchemeSpecificParts != null) {
            Iterator<PatternMatcher> it = mDataSchemeSpecificParts.iterator();
            while (it.hasNext()) {
                it.next().dumpDebug(proto, IntentFilterProto.DATA_SCHEME_SPECS);
            }
        }
        if (mDataAuthorities != null) {
            Iterator<AuthorityEntry> it = mDataAuthorities.iterator();
            while (it.hasNext()) {
                it.next().dumpDebug(proto, IntentFilterProto.DATA_AUTHORITIES);
            }
        }
        if (mDataPaths != null) {
            Iterator<PatternMatcher> it = mDataPaths.iterator();
            while (it.hasNext()) {
                it.next().dumpDebug(proto, IntentFilterProto.DATA_PATHS);
            }
        }
        if (mDataTypes != null) {
            Iterator<String> it = mDataTypes.iterator();
            while (it.hasNext()) {
                proto.write(IntentFilterProto.DATA_TYPES, it.next());
            }
        }
        if (mMimeGroups != null) {
            Iterator<String> it = mMimeGroups.iterator();
            while (it.hasNext()) {
                proto.write(IntentFilterProto.MIME_GROUPS, it.next());
            }
        }
        if (mPriority != 0 || hasPartialTypes()) {
            proto.write(IntentFilterProto.PRIORITY, mPriority);
            proto.write(IntentFilterProto.HAS_PARTIAL_TYPES, hasPartialTypes());
        }
        proto.write(IntentFilterProto.GET_AUTO_VERIFY, getAutoVerify());
        if (mExtras != null) {
            mExtras.dumpDebug(proto, IntentFilterProto.EXTRAS);
        }
        if (Flags.relativeReferenceIntentFilters() && mUriRelativeFilterGroups != null) {
            Iterator<UriRelativeFilterGroup> it = mUriRelativeFilterGroups.iterator();
            while (it.hasNext()) {
                it.next().dumpDebug(proto, IntentFilterProto.URI_RELATIVE_FILTER_GROUPS);
            }
        }
        proto.end(token);
    }

    public void dump(Printer du, String prefix) {
        StringBuilder sb = new StringBuilder(256);
        if (mActions.size() > 0) {
            Iterator<String> it = mActions.iterator();
            while (it.hasNext()) {
                sb.setLength(0);
                sb.append(prefix); sb.append("Action: \"");
                        sb.append(it.next()); sb.append("\"");
                du.println(sb.toString());
            }
        }
        if (mCategories != null) {
            Iterator<String> it = mCategories.iterator();
            while (it.hasNext()) {
                sb.setLength(0);
                sb.append(prefix); sb.append("Category: \"");
                        sb.append(it.next()); sb.append("\"");
                du.println(sb.toString());
            }
        }
        if (mDataSchemes != null) {
            Iterator<String> it = mDataSchemes.iterator();
            while (it.hasNext()) {
                sb.setLength(0);
                sb.append(prefix); sb.append("Scheme: \"");
                        sb.append(it.next()); sb.append("\"");
                du.println(sb.toString());
            }
        }
        if (mDataSchemeSpecificParts != null) {
            Iterator<PatternMatcher> it = mDataSchemeSpecificParts.iterator();
            while (it.hasNext()) {
                PatternMatcher pe = it.next();
                sb.setLength(0);
                sb.append(prefix); sb.append("Ssp: \"");
                        sb.append(pe); sb.append("\"");
                du.println(sb.toString());
            }
        }
        if (mDataAuthorities != null) {
            Iterator<AuthorityEntry> it = mDataAuthorities.iterator();
            while (it.hasNext()) {
                AuthorityEntry ae = it.next();
                sb.setLength(0);
                sb.append(prefix); sb.append("Authority: \"");
                        sb.append(ae.mHost); sb.append("\": ");
                        sb.append(ae.mPort);
                if (ae.mWild) sb.append(" WILD");
                du.println(sb.toString());
            }
        }
        if (mDataPaths != null) {
            Iterator<PatternMatcher> it = mDataPaths.iterator();
            while (it.hasNext()) {
                PatternMatcher pe = it.next();
                sb.setLength(0);
                sb.append(prefix); sb.append("Path: \"");
                        sb.append(pe); sb.append("\"");
                du.println(sb.toString());
            }
        }
        if (mUriRelativeFilterGroups != null) {
            Iterator<UriRelativeFilterGroup> it = mUriRelativeFilterGroups.iterator();
            while (it.hasNext()) {
                sb.setLength(0);
                sb.append(prefix); sb.append("UriRelativeFilterGroup: \"");
                sb.append(it.next()); sb.append("\"");
                du.println(sb.toString());
            }
        }
        if (mStaticDataTypes != null) {
            Iterator<String> it = mStaticDataTypes.iterator();
            while (it.hasNext()) {
                sb.setLength(0);
                sb.append(prefix); sb.append("StaticType: \"");
                sb.append(it.next()); sb.append("\"");
                du.println(sb.toString());
            }
        }
        if (mDataTypes != null) {
            Iterator<String> it = mDataTypes.iterator();
            while (it.hasNext()) {
                String dataType = it.next();
                if (hasExactStaticDataType(dataType)) {
                    continue;
                }

                sb.setLength(0);
                sb.append(prefix); sb.append("Type: \"");
                sb.append(dataType); sb.append("\"");
                du.println(sb.toString());
            }
        }
        if (mMimeGroups != null) {
            Iterator<String> it = mMimeGroups.iterator();
            while (it.hasNext()) {
                sb.setLength(0);
                sb.append(prefix); sb.append("MimeGroup: \"");
                sb.append(it.next()); sb.append("\"");
                du.println(sb.toString());
            }
        }
        if (mPriority != 0 || mOrder != 0 || hasPartialTypes()) {
            sb.setLength(0);
            sb.append(prefix);
            sb.append("mPriority="); sb.append(mPriority);
            sb.append(", mOrder="); sb.append(mOrder);
            sb.append(", mHasStaticPartialTypes="); sb.append(mHasStaticPartialTypes);
            sb.append(", mHasDynamicPartialTypes="); sb.append(mHasDynamicPartialTypes);
            du.println(sb.toString());
        }
        if (getAutoVerify()) {
            sb.setLength(0);
            sb.append(prefix); sb.append("AutoVerify="); sb.append(getAutoVerify());
            du.println(sb.toString());
        }
        if (mExtras != null) {
            sb.setLength(0);
            sb.append(prefix); sb.append("mExtras="); sb.append(mExtras.toString());
            du.println(sb.toString());
        }
    }

    public static final @android.annotation.NonNull Parcelable.Creator<IntentFilter> CREATOR
            = new Parcelable.Creator<IntentFilter>() {
        public IntentFilter createFromParcel(Parcel source) {
            return new IntentFilter(source);
        }

        public IntentFilter[] newArray(int size) {
            return new IntentFilter[size];
        }
    };

    public final int describeContents() {
        return 0;
    }

    public final void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(mActions.toArray(new String[mActions.size()]));
        if (mCategories != null) {
            dest.writeInt(1);
            dest.writeStringList(mCategories);
        } else {
            dest.writeInt(0);
        }
        if (mDataSchemes != null) {
            dest.writeInt(1);
            dest.writeStringList(mDataSchemes);
        } else {
            dest.writeInt(0);
        }
        if (mStaticDataTypes != null) {
            dest.writeInt(1);
            dest.writeStringList(mStaticDataTypes);
        } else {
            dest.writeInt(0);
        }
        if (mDataTypes != null) {
            dest.writeInt(1);
            dest.writeStringList(mDataTypes);
        } else {
            dest.writeInt(0);
        }
        if (mMimeGroups != null) {
            dest.writeInt(1);
            dest.writeStringList(mMimeGroups);
        } else {
            dest.writeInt(0);
        }
        if (mDataSchemeSpecificParts != null) {
            final int N = mDataSchemeSpecificParts.size();
            dest.writeInt(N);
            for (int i=0; i<N; i++) {
                mDataSchemeSpecificParts.get(i).writeToParcel(dest, flags);
            }
        } else {
            dest.writeInt(0);
        }
        if (mDataAuthorities != null) {
            final int N = mDataAuthorities.size();
            dest.writeInt(N);
            for (int i=0; i<N; i++) {
                mDataAuthorities.get(i).writeToParcel(dest);
            }
        } else {
            dest.writeInt(0);
        }
        if (mDataPaths != null) {
            final int N = mDataPaths.size();
            dest.writeInt(N);
            for (int i=0; i<N; i++) {
                mDataPaths.get(i).writeToParcel(dest, flags);
            }
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(mPriority);
        dest.writeInt(mHasStaticPartialTypes ? 1 : 0);
        dest.writeInt(mHasDynamicPartialTypes ? 1 : 0);
        dest.writeInt(getAutoVerify() ? 1 : 0);
        dest.writeInt(mInstantAppVisibility);
        dest.writeInt(mOrder);
        if (mExtras != null) {
            dest.writeInt(1);
            mExtras.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        if (Flags.relativeReferenceIntentFilters() && mUriRelativeFilterGroups != null) {
            final int N = mUriRelativeFilterGroups.size();
            dest.writeInt(N);
            for (int i = 0; i < N; i++) {
                mUriRelativeFilterGroups.get(i).writeToParcel(dest, flags);
            }
        } else {
            dest.writeInt(0);
        }
    }

    /**
     * For debugging -- perform a check on the filter, return true if it passed
     * or false if it failed.
     *
     * {@hide}
     */
    public boolean debugCheck() {
        return true;

        // This code looks for intent filters that do not specify data.
        /*
        if (mActions != null && mActions.size() == 1
                && mActions.contains(Intent.ACTION_MAIN)) {
            return true;
        }

        if (mDataTypes == null && mDataSchemes == null) {
            Log.w("IntentFilter", "QUESTIONABLE INTENT FILTER:");
            dump(Log.WARN, "IntentFilter", "  ");
            return false;
        }

        return true;
        */
    }

    /**
     * Perform a check on data paths and scheme specific parts of the intent filter.
     * Return true if it passed.
     * @hide
     */
    public boolean checkDataPathAndSchemeSpecificParts() {
        final int numDataPath = mDataPaths == null
                ? 0 : mDataPaths.size();
        final int numDataSchemeSpecificParts = mDataSchemeSpecificParts == null
                ? 0 : mDataSchemeSpecificParts.size();
        for (int i = 0; i < numDataPath; i++) {
            if (!mDataPaths.get(i).check()) {
                return false;
            }
        }
        for (int i = 0; i < numDataSchemeSpecificParts; i++) {
            if (!mDataSchemeSpecificParts.get(i).check()) {
                return false;
            }
        }
        return true;
    }

    /** @hide */
    public IntentFilter(Parcel source) {
        List<String> actions = new ArrayList<>();
        source.readStringList(actions);
        mActions = new ArraySet<>(actions);
        if (source.readInt() != 0) {
            mCategories = new ArrayList<String>();
            source.readStringList(mCategories);
        }
        if (source.readInt() != 0) {
            mDataSchemes = new ArrayList<String>();
            source.readStringList(mDataSchemes);
        }
        if (source.readInt() != 0) {
            mStaticDataTypes = new ArrayList<String>();
            source.readStringList(mStaticDataTypes);
        }
        if (source.readInt() != 0) {
            mDataTypes = new ArrayList<String>();
            source.readStringList(mDataTypes);
        }
        if (source.readInt() != 0) {
            mMimeGroups = new ArrayList<String>();
            source.readStringList(mMimeGroups);
        }
        int N = source.readInt();
        if (N > 0) {
            mDataSchemeSpecificParts = new ArrayList<PatternMatcher>(N);
            for (int i=0; i<N; i++) {
                mDataSchemeSpecificParts.add(new PatternMatcher(source));
            }
        }
        N = source.readInt();
        if (N > 0) {
            mDataAuthorities = new ArrayList<AuthorityEntry>(N);
            for (int i=0; i<N; i++) {
                mDataAuthorities.add(new AuthorityEntry(source));
            }
        }
        N = source.readInt();
        if (N > 0) {
            mDataPaths = new ArrayList<PatternMatcher>(N);
            for (int i=0; i<N; i++) {
                mDataPaths.add(new PatternMatcher(source));
            }
        }
        mPriority = source.readInt();
        mHasStaticPartialTypes = source.readInt() > 0;
        mHasDynamicPartialTypes = source.readInt() > 0;
        setAutoVerify(source.readInt() > 0);
        setVisibilityToInstantApp(source.readInt());
        mOrder = source.readInt();
        if (source.readInt() != 0) {
            mExtras = PersistableBundle.CREATOR.createFromParcel(source);
        }
        N = source.readInt();
        if (Flags.relativeReferenceIntentFilters() && N > 0) {
            mUriRelativeFilterGroups = new ArrayList<UriRelativeFilterGroup>(N);
            for (int i = 0; i < N; i++) {
                mUriRelativeFilterGroups.add(new UriRelativeFilterGroup(source));
            }
        }
    }

    private boolean hasPartialTypes() {
        return mHasStaticPartialTypes || mHasDynamicPartialTypes;
    }

    private final boolean findMimeType(String type) {
        final ArrayList<String> t = mDataTypes;

        if (type == null) {
            return false;
        }

        if (t.contains(type)) {
            return true;
        }

        // Deal with an Intent wanting to match every type in the IntentFilter.
        final int typeLength = type.length();
        if (typeLength == 3 && type.equals("*/*")) {
            return !t.isEmpty();
        }

        // Deal with this IntentFilter wanting to match every Intent type.
        if (hasPartialTypes() && t.contains("*")) {
            return true;
        }

        final int slashpos = type.indexOf('/');
        if (slashpos > 0) {
            if (hasPartialTypes() && t.contains(type.substring(0, slashpos))) {
                return true;
            }
            if (typeLength == slashpos+2 && type.charAt(slashpos+1) == '*') {
                // Need to look through all types for one that matches
                // our base...
                final int numTypes = t.size();
                for (int i = 0; i < numTypes; i++) {
                    final String v = t.get(i);
                    if (type.regionMatches(0, v, 0, slashpos+1)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * @hide
     */
    public ArrayList<String> getHostsList() {
        ArrayList<String> result = new ArrayList<>();
        Iterator<IntentFilter.AuthorityEntry> it = authoritiesIterator();
        if (it != null) {
            while (it.hasNext()) {
                IntentFilter.AuthorityEntry entry = it.next();
                result.add(entry.getHost());
            }
        }
        return result;
    }

    /**
     * @hide
     */
    public String[] getHosts() {
        ArrayList<String> list = getHostsList();
        return list.toArray(new String[list.size()]);
    }

    /**
     * @hide
     */
    public static boolean filterEquals(IntentFilter f1, IntentFilter f2) {
        int s1 = f1.countActions();
        int s2 = f2.countActions();
        if (s1 != s2) {
            return false;
        }
        for (int i=0; i<s1; i++) {
            if (!f2.hasAction(f1.getAction(i))) {
                return false;
            }
        }
        s1 = f1.countCategories();
        s2 = f2.countCategories();
        if (s1 != s2) {
            return false;
        }
        for (int i=0; i<s1; i++) {
            if (!f2.hasCategory(f1.getCategory(i))) {
                return false;
            }
        }
        s1 = f1.countDataTypes();
        s2 = f2.countDataTypes();
        if (s1 != s2) {
            return false;
        }
        for (int i=0; i<s1; i++) {
            if (!f2.hasExactDataType(f1.getDataType(i))) {
                return false;
            }
        }
        s1 = f1.countDataSchemes();
        s2 = f2.countDataSchemes();
        if (s1 != s2) {
            return false;
        }
        for (int i=0; i<s1; i++) {
            if (!f2.hasDataScheme(f1.getDataScheme(i))) {
                return false;
            }
        }
        s1 = f1.countDataAuthorities();
        s2 = f2.countDataAuthorities();
        if (s1 != s2) {
            return false;
        }
        for (int i=0; i<s1; i++) {
            if (!f2.hasDataAuthority(f1.getDataAuthority(i))) {
                return false;
            }
        }
        s1 = f1.countDataPaths();
        s2 = f2.countDataPaths();
        if (s1 != s2) {
            return false;
        }
        for (int i=0; i<s1; i++) {
            if (!f2.hasDataPath(f1.getDataPath(i))) {
                return false;
            }
        }
        s1 = f1.countDataSchemeSpecificParts();
        s2 = f2.countDataSchemeSpecificParts();
        if (s1 != s2) {
            return false;
        }
        for (int i=0; i<s1; i++) {
            if (!f2.hasDataSchemeSpecificPart(f1.getDataSchemeSpecificPart(i))) {
                return false;
            }
        }
        return true;
    }
}
