/*
 * Copyright (C) 2009 The Android Open Source Project
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
import android.app.slice.Slice;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.wallpaper.WallpaperService;
import android.util.AttributeSet;
import android.util.Printer;
import android.util.Xml;
import android.view.SurfaceHolder;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * This class is used to specify meta information of a wallpaper service.
 */
public final class WallpaperInfo implements Parcelable {
    static final String TAG = "WallpaperInfo";
    
    /**
     * The Service that implements this wallpaper component.
     */
    final ResolveInfo mService;
    
    /**
     * The wallpaper setting activity's name, to
     * launch the setting activity of this wallpaper.
     */
    final String mSettingsActivityName;

    /**
     * Resource identifier for this wallpaper's thumbnail image.
     */
    final int mThumbnailResource;

    /**
     * Resource identifier for a string indicating the author of the wallpaper.
     */
    final int mAuthorResource;

    /**
     * Resource identifier for a string containing a short description of the wallpaper.
     */
    final int mDescriptionResource;

    final int mContextUriResource;
    final int mContextDescriptionResource;
    final boolean mShowMetadataInPreview;
    final boolean mSupportsAmbientMode;
    final boolean mShouldUseDefaultUnfoldTransition;
    final String mSettingsSliceUri;
    final boolean mSupportMultipleDisplays;

    /**
     * Constructor.
     * 
     * @param context The Context in which we are parsing the wallpaper.
     * @param service The ResolveInfo returned from the package manager about
     * this wallpaper's component.
     */
    public WallpaperInfo(Context context, ResolveInfo service)
            throws XmlPullParserException, IOException {
        mService = service;
        ServiceInfo si = service.serviceInfo;

        final PackageManager pm = context.getPackageManager();
        try (XmlResourceParser parser = si.loadXmlMetaData(pm,
                WallpaperService.SERVICE_META_DATA)) {
            if (parser == null) {
                throw new XmlPullParserException("No "
                        + WallpaperService.SERVICE_META_DATA + " meta-data");
            }

            Resources res = pm.getResourcesForApplication(si.applicationInfo);

            AttributeSet attrs = Xml.asAttributeSet(parser);

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
            }

            String nodeName = parser.getName();
            if (!"wallpaper".equals(nodeName)) {
                throw new XmlPullParserException(
                        "Meta-data does not start with wallpaper tag");
            }

            TypedArray sa = res.obtainAttributes(attrs,
                    com.android.internal.R.styleable.Wallpaper);
            mSettingsActivityName = sa.getString(
                    com.android.internal.R.styleable.Wallpaper_settingsActivity);
            mThumbnailResource = sa.getResourceId(
                    com.android.internal.R.styleable.Wallpaper_thumbnail,
                    -1);
            mAuthorResource = sa.getResourceId(
                    com.android.internal.R.styleable.Wallpaper_author,
                    -1);
            mDescriptionResource = sa.getResourceId(
                    com.android.internal.R.styleable.Wallpaper_description,
                    -1);
            mContextUriResource = sa.getResourceId(
                    com.android.internal.R.styleable.Wallpaper_contextUri,
                    -1);
            mContextDescriptionResource = sa.getResourceId(
                    com.android.internal.R.styleable.Wallpaper_contextDescription,
                    -1);
            mShowMetadataInPreview = sa.getBoolean(
                    com.android.internal.R.styleable.Wallpaper_showMetadataInPreview,
                    false);

            // Watch wallpapers support ambient mode by default.
            final boolean defSupportsAmbientMode =
                    pm.hasSystemFeature(PackageManager.FEATURE_WATCH);
            mSupportsAmbientMode = sa.getBoolean(
                    com.android.internal.R.styleable.Wallpaper_supportsAmbientMode,
                    defSupportsAmbientMode);
            mShouldUseDefaultUnfoldTransition = sa.getBoolean(
                    com.android.internal.R.styleable
                            .Wallpaper_shouldUseDefaultUnfoldTransition, true);
            mSettingsSliceUri = sa.getString(
                    com.android.internal.R.styleable.Wallpaper_settingsSliceUri);
            mSupportMultipleDisplays = sa.getBoolean(
                    com.android.internal.R.styleable.Wallpaper_supportsMultipleDisplays,
                    false);

            sa.recycle();
        } catch (NameNotFoundException e) {
            throw new XmlPullParserException(
                    "Unable to create context for: " + si.packageName);
        }
    }

    WallpaperInfo(Parcel source) {
        mSettingsActivityName = source.readString();
        mThumbnailResource = source.readInt();
        mAuthorResource = source.readInt();
        mDescriptionResource = source.readInt();
        mContextUriResource = source.readInt();
        mContextDescriptionResource = source.readInt();
        mShowMetadataInPreview = source.readInt() != 0;
        mSupportsAmbientMode = source.readInt() != 0;
        mSettingsSliceUri = source.readString();
        mSupportMultipleDisplays = source.readInt() != 0;
        mShouldUseDefaultUnfoldTransition = source.readInt() != 0;
        mService = ResolveInfo.CREATOR.createFromParcel(source);
    }
    
    /**
     * Return the .apk package that implements this wallpaper.
     */
    public String getPackageName() {
        return mService.serviceInfo.packageName;
    }
    
    /**
     * Return the class name of the service component that implements
     * this wallpaper.
     */
    public String getServiceName() {
        return mService.serviceInfo.name;
    }

    /**
     * Return the raw information about the Service implementing this
     * wallpaper.  Do not modify the returned object.
     */
    public ServiceInfo getServiceInfo() {
        return mService.serviceInfo;
    }

    /**
     * Return the component of the service that implements this wallpaper.
     */
    public ComponentName getComponent() {
        return new ComponentName(mService.serviceInfo.packageName,
                mService.serviceInfo.name);
    }
    
    /**
     * Load the user-displayed label for this wallpaper.
     * 
     * @param pm Supply a PackageManager used to load the wallpaper's
     * resources.
     */
    public CharSequence loadLabel(PackageManager pm) {
        return mService.loadLabel(pm);
    }
    
    /**
     * Load the user-displayed icon for this wallpaper.
     * 
     * @param pm Supply a PackageManager used to load the wallpaper's
     * resources.
     */
    public Drawable loadIcon(PackageManager pm) {
        return mService.loadIcon(pm);
    }
    
    /**
     * Load the thumbnail image for this wallpaper.
     * 
     * @param pm Supply a PackageManager used to load the wallpaper's
     * resources.
     */
    public Drawable loadThumbnail(PackageManager pm) {
        if (mThumbnailResource < 0) return null;

        return pm.getDrawable(mService.serviceInfo.packageName,
                              mThumbnailResource,
                              mService.serviceInfo.applicationInfo);
    }

    /**
     * Return a string indicating the author(s) of this wallpaper.
     */
    public CharSequence loadAuthor(PackageManager pm) throws NotFoundException {
        if (mAuthorResource <= 0) throw new NotFoundException();
        String packageName = mService.resolvePackageName;
        ApplicationInfo applicationInfo = null;
        if (packageName == null) {
            packageName = mService.serviceInfo.packageName;
            applicationInfo = mService.serviceInfo.applicationInfo;
        }
        return pm.getText(packageName, mAuthorResource, applicationInfo);
    }

    /**
     * Return a brief summary of this wallpaper's behavior.
     */
    public CharSequence loadDescription(PackageManager pm) throws NotFoundException {
        String packageName = mService.resolvePackageName;
        ApplicationInfo applicationInfo = null;
        if (packageName == null) {
            packageName = mService.serviceInfo.packageName;
            applicationInfo = mService.serviceInfo.applicationInfo;
        }
        if (mService.serviceInfo.descriptionRes != 0) {
            return pm.getText(packageName, mService.serviceInfo.descriptionRes,
                    applicationInfo);
            
        }
        if (mDescriptionResource <= 0) throw new NotFoundException();
        return pm.getText(packageName, mDescriptionResource,
                mService.serviceInfo.applicationInfo);
    }

    /**
     * Returns an URI that specifies a link for further context about this wallpaper.
     *
     * @param pm An instance of {@link PackageManager} to retrieve the URI.
     * @return The URI.
     */
    public Uri loadContextUri(PackageManager pm) throws NotFoundException {
        if (mContextUriResource <= 0) throw new NotFoundException();
        String packageName = mService.resolvePackageName;
        ApplicationInfo applicationInfo = null;
        if (packageName == null) {
            packageName = mService.serviceInfo.packageName;
            applicationInfo = mService.serviceInfo.applicationInfo;
        }
        CharSequence contextUriCharSequence = pm.getText(
                packageName, mContextUriResource, applicationInfo);
        if (contextUriCharSequence == null) {
            return null;
        }
        return Uri.parse(contextUriCharSequence.toString());
    }

    /**
     * Retrieves a title of the URI that specifies a link for further context about this wallpaper.
     *
     * @param pm An instance of {@link PackageManager} to retrieve the title.
     * @return The title.
     */
    public CharSequence loadContextDescription(PackageManager pm) throws NotFoundException {
        if (mContextDescriptionResource <= 0) throw new NotFoundException();
        String packageName = mService.resolvePackageName;
        ApplicationInfo applicationInfo = null;
        if (packageName == null) {
            packageName = mService.serviceInfo.packageName;
            applicationInfo = mService.serviceInfo.applicationInfo;
        }
        return pm.getText(packageName, mContextDescriptionResource, applicationInfo).toString();
    }

    /**
     * Queries whether any metadata should be shown when previewing the wallpaper. If this value is
     * set to true, any component that shows a preview of this live wallpaper should also show
     * accompanying information like {@link #loadLabel},
     * {@link #loadDescription}, {@link #loadAuthor} and
     * {@link #loadContextDescription(PackageManager)}, so the user gets to know further information
     * about this wallpaper.
     *
     * @return Whether any metadata should be shown when previewing the wallpaper.
     */
    public boolean getShowMetadataInPreview() {
        return mShowMetadataInPreview;
    }

    /**
     * Returns whether a wallpaper was optimized or not for ambient mode and can be drawn in there.
     *
     * @see WallpaperService.Engine#onAmbientModeChanged(boolean, boolean)
     * @see WallpaperService.Engine#isInAmbientMode()
     * @return {@code true} if wallpaper can draw when in ambient mode.
     * @hide
     */
    @SystemApi
    public boolean supportsAmbientMode() {
        return mSupportsAmbientMode;
    }

    /**
     * Return the class name of an activity that provides a settings UI for
     * the wallpaper.  You can launch this activity be starting it with
     * an {@link android.content.Intent} whose action is MAIN and with an
     * explicit {@link android.content.ComponentName}
     * composed of {@link #getPackageName} and the class name returned here.
     * 
     * <p>{@code null} will be returned if there is no settings activity associated
     * with the wallpaper.
     */
    public String getSettingsActivity() {
        return mSettingsActivityName;
    }
    
    /**
     * Returns an URI that provides a settings {@link Slice} for this wallpaper.
     * The wallpaper should implement a SliceProvider associated with this URI.
     * The system will display the Slice in the customization section while previewing the live
     * wallpaper. Because this URI is accessible to other apps, it is recommended to protect it
     * with the android.permission.BIND_WALLPAPER permission.
     *
     * <p>{@code null} will be returned if there is no settings Slice URI associated
     * with the wallpaper.
     *
     * @return The URI.
     */
    @Nullable
    public Uri getSettingsSliceUri() {
        if (mSettingsSliceUri == null) {
            return null;
        }
        return Uri.parse(mSettingsSliceUri);
    }

    /**
     * Returns whether this wallpaper service can support multiple engines to render on each surface
     * independently. An example use case is a multi-display set-up where the wallpaper service can
     * render surfaces to each of the connected displays.
     * <p>
     * This corresponds to the value {@link android.R.styleable#Wallpaper_supportsMultipleDisplays}
     * in the XML description of the wallpaper.
     * <p>
     * The default value is {@code false}.
     *
     * @see WallpaperService#onCreateEngine()
     * @see WallpaperService.Engine#onCreate(SurfaceHolder)
     * @return {@code true} if multiple engines can render independently on each surface.
     *
     * @attr ref android.R.styleable#Wallpaper_supportsMultipleDisplays
     */
    public boolean supportsMultipleDisplays() {
        return mSupportMultipleDisplays;
    }

    /**
     * Returns whether this wallpaper should receive default zooming updates when the device
     * changes its state (e.g. when folding or unfolding a foldable device).
     * If set to false the wallpaper will not receive zoom events when changing the device state,
     * so it can implement its own transition instead.
     * <p>
     * This corresponds to the value {@link
     * android.R.styleable#Wallpaper_shouldUseDefaultUnfoldTransition} in the
     * XML description of the wallpaper.
     * <p>
     * The default value is {@code true}.
     *
     * @see android.R.styleable#Wallpaper_shouldUseDefaultUnfoldTransition
     * @return {@code true} if wallpaper should receive default device state change
     * transition updates
     *
     * @attr ref android.R.styleable#Wallpaper_shouldUseDefaultUnfoldTransition
     */
    public boolean shouldUseDefaultUnfoldTransition() {
        return mShouldUseDefaultUnfoldTransition;
    }

    public void dump(Printer pw, String prefix) {
        pw.println(prefix + "Service:");
        mService.dump(pw, prefix + "  ");
        pw.println(prefix + "mSettingsActivityName=" + mSettingsActivityName);
    }
    
    @Override
    public String toString() {
        return "WallpaperInfo{" + mService.serviceInfo.name
                + ", settings: "
                + mSettingsActivityName + "}";
    }

    /**
     * Used to package this object into a {@link Parcel}.
     * 
     * @param dest The {@link Parcel} to be written.
     * @param flags The flags used for parceling.
     */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSettingsActivityName);
        dest.writeInt(mThumbnailResource);
        dest.writeInt(mAuthorResource);
        dest.writeInt(mDescriptionResource);
        dest.writeInt(mContextUriResource);
        dest.writeInt(mContextDescriptionResource);
        dest.writeInt(mShowMetadataInPreview ? 1 : 0);
        dest.writeInt(mSupportsAmbientMode ? 1 : 0);
        dest.writeString(mSettingsSliceUri);
        dest.writeInt(mSupportMultipleDisplays ? 1 : 0);
        dest.writeInt(mShouldUseDefaultUnfoldTransition ? 1 : 0);
        mService.writeToParcel(dest, flags);
    }

    /**
     * Used to make this class parcelable.
     */
    public static final @android.annotation.NonNull Parcelable.Creator<WallpaperInfo> CREATOR = new Parcelable.Creator<WallpaperInfo>() {
        public WallpaperInfo createFromParcel(Parcel source) {
            return new WallpaperInfo(source);
        }

        public WallpaperInfo[] newArray(int size) {
            return new WallpaperInfo[size];
        }
    };

    public int describeContents() {
        return 0;
    }
}
