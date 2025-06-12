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

package com.android.server.media.quality;

import static android.media.quality.AmbientBacklightEvent.AMBIENT_BACKLIGHT_EVENT_ENABLED;
import static android.media.quality.AmbientBacklightEvent.AMBIENT_BACKLIGHT_EVENT_DISABLED;
import static android.media.quality.AmbientBacklightEvent.AMBIENT_BACKLIGHT_EVENT_METADATA_AVAILABLE;
import static android.media.quality.AmbientBacklightEvent.AMBIENT_BACKLIGHT_EVENT_INTERRUPTED;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.audio.effect.DefaultExtension;
import android.hardware.tv.mediaquality.AmbientBacklightColorFormat;
import android.hardware.tv.mediaquality.IMediaQuality;
import android.hardware.tv.mediaquality.IPictureProfileAdjustmentListener;
import android.hardware.tv.mediaquality.IPictureProfileChangedListener;
import android.hardware.tv.mediaquality.ISoundProfileAdjustmentListener;
import android.hardware.tv.mediaquality.ISoundProfileChangedListener;
import android.hardware.tv.mediaquality.ParamCapability;
import android.hardware.tv.mediaquality.PictureParameter;
import android.hardware.tv.mediaquality.PictureParameters;
import android.hardware.tv.mediaquality.SoundParameter;
import android.hardware.tv.mediaquality.SoundParameters;
import android.hardware.tv.mediaquality.StreamStatus;
import android.hardware.tv.mediaquality.VendorParamCapability;
import android.media.quality.ActiveProcessingPicture;
import android.media.quality.AmbientBacklightEvent;
import android.media.quality.AmbientBacklightMetadata;
import android.media.quality.AmbientBacklightSettings;
import android.media.quality.IActiveProcessingPictureListener;
import android.media.quality.IAmbientBacklightCallback;
import android.media.quality.IMediaQualityManager;
import android.media.quality.IPictureProfileCallback;
import android.media.quality.ISoundProfileCallback;
import android.media.quality.MediaQualityContract.BaseParameters;
import android.media.quality.ParameterCapability;
import android.media.quality.PictureProfile;
import android.media.quality.PictureProfileHandle;
import android.media.quality.SoundProfile;
import android.media.quality.SoundProfileHandle;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.view.SurfaceControlActivePicture;
import android.view.SurfaceControlActivePictureListener;

import com.android.internal.annotations.GuardedBy;
import com.android.server.SystemService;
import com.android.server.utils.Slogf;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * This service manage picture profile and sound profile for TV setting. Also communicates with the
 * database to save, update the profiles.
 */
public class MediaQualityService extends SystemService {

    private static final boolean DEBUG = false;
    private static final String TAG = "MediaQualityService";
    private static final String ALLOWLIST = "allowlist";
    private static final String PICTURE_PROFILE_PREFERENCE = "picture_profile_preference";
    private static final String SOUND_PROFILE_PREFERENCE = "sound_profile_preference";
    private static final String COMMA_DELIMITER = ",";
    private final Context mContext;
    private final MediaQualityDbHelper mMediaQualityDbHelper;
    private final BiMap<Long, String> mPictureProfileTempIdMap;
    private final BiMap<Long, String> mSoundProfileTempIdMap;
    private final Map<String, Long> mPackageDefaultPictureProfileHandleMap = new HashMap<>();
    private IMediaQuality mMediaQuality;
    private PictureProfileAdjustmentListenerImpl mPictureProfileAdjListener;
    private SoundProfileAdjustmentListenerImpl mSoundProfileAdjListener;
    private IPictureProfileChangedListener mPpChangedListener;
    private ISoundProfileChangedListener mSpChangedListener;
    private final HalAmbientBacklightCallback mHalAmbientBacklightCallback;
    private final Map<String, AmbientBacklightCallbackRecord> mCallbackRecords = new HashMap<>();
    private final PackageManager mPackageManager;
    private final SparseArray<UserState> mUserStates = new SparseArray<>();
    private SharedPreferences mPictureProfileSharedPreference;
    private SharedPreferences mSoundProfileSharedPreference;
    private HalNotifier mHalNotifier;
    private MqManagerNotifier mMqManagerNotifier;
    private MqDatabaseUtils mMqDatabaseUtils;
    private Handler mHandler;
    private SurfaceControlActivePictureListener mSurfaceControlActivePictureListener;

    // A global lock for picture profile objects.
    private final Object mPictureProfileLock = new Object();
    // A global lock for sound profile objects.
    private final Object mSoundProfileLock = new Object();
    // A global lock for user state objects.
    private final Object mUserStateLock = new Object();
    // A global lock for ambient backlight objects.
    private final Object mAmbientBacklightLock = new Object();

    private final Map<Long, PictureProfile> mHandleToPictureProfile = new HashMap<>();
    private final BiMap<Long, Long> mCurrentPictureHandleToOriginal = new BiMap<>();

    public MediaQualityService(Context context) {
        super(context);
        mContext = context;
        mHalAmbientBacklightCallback = new HalAmbientBacklightCallback();
        mPackageManager = mContext.getPackageManager();
        mPictureProfileTempIdMap = new BiMap<>();
        mSoundProfileTempIdMap = new BiMap<>();
        mMediaQualityDbHelper = new MediaQualityDbHelper(mContext);
        mMediaQualityDbHelper.setWriteAheadLoggingEnabled(true);
        mMediaQualityDbHelper.setIdleConnectionTimeout(30);
        mMqManagerNotifier = new MqManagerNotifier();
        mMqDatabaseUtils = new MqDatabaseUtils();
        mHalNotifier = new HalNotifier();
        mPictureProfileAdjListener = new PictureProfileAdjustmentListenerImpl();
        mSoundProfileAdjListener = new SoundProfileAdjustmentListenerImpl();
        mHandler = new Handler(Looper.getMainLooper());

        // The package info in the context isn't initialized in the way it is for normal apps,
        // so the standard, name-based context.getSharedPreferences doesn't work. Instead, we
        // build the path manually below using the same policy that appears in ContextImpl.
        final Context deviceContext = mContext.createDeviceProtectedStorageContext();
        final File pictureProfilePrefs = new File(Environment.getDataSystemDirectory(),
                PICTURE_PROFILE_PREFERENCE);
        mPictureProfileSharedPreference = deviceContext.getSharedPreferences(
                pictureProfilePrefs, Context.MODE_PRIVATE);
        final File soundProfilePrefs = new File(Environment.getDataSystemDirectory(),
                SOUND_PROFILE_PREFERENCE);
        mSoundProfileSharedPreference = deviceContext.getSharedPreferences(
                soundProfilePrefs, Context.MODE_PRIVATE);
    }

    @GuardedBy("mPictureProfileLock")
    @Override
    public void onStart() {
        IBinder binder = ServiceManager.getService(IMediaQuality.DESCRIPTOR + "/default");
        if (binder == null) {
            Slogf.d(TAG, "Binder is null");
            return;
        }
        Slogf.d(TAG, "Binder is not null");

        mSurfaceControlActivePictureListener = new SurfaceControlActivePictureListener() {
            @Override
            public void onActivePicturesChanged(SurfaceControlActivePicture[] activePictures) {
                handleOnActivePicturesChanged(activePictures);
            }
        };
        mSurfaceControlActivePictureListener.startListening(); // TODO: stop listening

        mMediaQuality = IMediaQuality.Stub.asInterface(binder);
        if (mMediaQuality != null) {
            try {
                mMediaQuality.setAmbientBacklightCallback(mHalAmbientBacklightCallback);

                mPpChangedListener = mMediaQuality.getPictureProfileListener();
                mSpChangedListener = mMediaQuality.getSoundProfileListener();

                mMediaQuality.setPictureProfileAdjustmentListener(mPictureProfileAdjListener);
                mMediaQuality.setSoundProfileAdjustmentListener(mSoundProfileAdjListener);

                synchronized (mPictureProfileLock) {
                    String selection = BaseParameters.PARAMETER_TYPE + " = ? AND "
                            + BaseParameters.PARAMETER_NAME + " = ?";
                    String[] selectionArguments = {
                            Integer.toString(PictureProfile.TYPE_SYSTEM),
                            PictureProfile.NAME_DEFAULT
                    };
                    List<PictureProfile> packageDefaultPictureProfiles =
                            mMqDatabaseUtils.getPictureProfilesBasedOnConditions(MediaQualityUtils
                                .getMediaProfileColumns(false), selection, selectionArguments);
                    mPackageDefaultPictureProfileHandleMap.clear();
                    for (PictureProfile profile : packageDefaultPictureProfiles) {
                        mPackageDefaultPictureProfileHandleMap.put(
                                profile.getPackageName(), profile.getHandle().getId());
                    }
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to set ambient backlight detector callback", e);
            }
        }

        publishBinderService(Context.MEDIA_QUALITY_SERVICE, new BinderService());
    }

    private void handleOnActivePicturesChanged(SurfaceControlActivePicture[] scActivePictures) {
        if (DEBUG) {
            Slog.d(TAG, "handleOnActivePicturesChanged");
        }
        synchronized (mPictureProfileLock) {
        // TODO handle other users
            UserState userState = getOrCreateUserState(UserHandle.USER_SYSTEM);
            int n = userState.mActiveProcessingPictureCallbackList.beginBroadcast();
            for (int i = 0; i < n; ++i) {
                try {
                    IActiveProcessingPictureListener l = userState
                            .mActiveProcessingPictureCallbackList
                            .getBroadcastItem(i);
                    ActiveProcessingPictureListenerInfo info =
                            userState.mActiveProcessingPictureListenerMap.get(l);
                    if (info == null) {
                        continue;
                    }
                    int uid = info.mUid;
                    boolean hasGlobalPermission = mContext.checkPermission(
                            android.Manifest.permission.MANAGE_GLOBAL_PICTURE_QUALITY_SERVICE,
                            info.mPid, uid)
                            == PackageManager.PERMISSION_GRANTED;
                    List<ActiveProcessingPicture> aps = new ArrayList<>();
                    for (SurfaceControlActivePicture scap : scActivePictures) {
                        if (!hasGlobalPermission && scap.getOwnerUid() != uid) {
                            // should not receive the event
                            continue;
                        }
                        String profileId = mPictureProfileTempIdMap.getValue(
                                scap.getPictureProfileHandle().getId());
                        if (profileId == null) {
                            continue;
                        }
                        aps.add(new ActiveProcessingPicture(
                                scap.getLayerId(), profileId, scap.getOwnerUid() != uid));

                    }

                    l.onActiveProcessingPicturesChanged(aps);
                } catch (RemoteException e) {
                    Slog.e(TAG, "failed to report added AD service to callback", e);
                }
            }
            userState.mActiveProcessingPictureCallbackList.finishBroadcast();
        }
    }

    private final class BinderService extends IMediaQualityManager.Stub {

        @GuardedBy("mPictureProfileLock")
        @Override
        public void createPictureProfile(PictureProfile pp, int userId) {
            mHandler.post(
                    () -> {
                        if ((pp.getPackageName() != null && !pp.getPackageName().isEmpty()
                                && !incomingPackageEqualsCallingUidPackage(pp.getPackageName()))
                                && !hasGlobalPictureQualityServicePermission()) {
                            mMqManagerNotifier.notifyOnPictureProfileError(null,
                                    PictureProfile.ERROR_NO_PERMISSION,
                                    Binder.getCallingUid(), Binder.getCallingPid());
                        }

                        synchronized (mPictureProfileLock) {
                            SQLiteDatabase db = mMediaQualityDbHelper.getWritableDatabase();

                            ContentValues values = MediaQualityUtils.getContentValues(null,
                                    pp.getProfileType(),
                                    pp.getName(),
                                    pp.getPackageName() == null || pp.getPackageName().isEmpty()
                                            ? getPackageOfCallingUid() : pp.getPackageName(),
                                    pp.getInputId(),
                                    pp.getParameters());

                            // id is auto-generated by SQLite upon successful insertion of row
                            Long id = db.insert(mMediaQualityDbHelper.PICTURE_QUALITY_TABLE_NAME,
                                    null, values);
                            MediaQualityUtils.populateTempIdMap(mPictureProfileTempIdMap, id);
                            String value = mPictureProfileTempIdMap.getValue(id);
                            pp.setProfileId(value);
                            mMqManagerNotifier.notifyOnPictureProfileAdded(value, pp,
                                    Binder.getCallingUid(), Binder.getCallingPid());
                            if (isPackageDefaultPictureProfile(pp)) {
                                mPackageDefaultPictureProfileHandleMap.put(
                                    pp.getPackageName(), pp.getHandle().getId());
                            }
                        }
                    }
            );
        }

        @GuardedBy("mPictureProfileLock")
        @Override
        public void updatePictureProfile(String id, PictureProfile pp, int userId) {
            mHandler.post(() -> {
                Long dbId = mPictureProfileTempIdMap.getKey(id);
                if (!hasPermissionToUpdatePictureProfile(dbId, pp)) {
                    mMqManagerNotifier.notifyOnPictureProfileError(id,
                            PictureProfile.ERROR_NO_PERMISSION,
                            Binder.getCallingUid(), Binder.getCallingPid());
                }
                synchronized (mPictureProfileLock) {
                    ContentValues values = MediaQualityUtils.getContentValues(dbId,
                            pp.getProfileType(),
                            pp.getName(),
                            pp.getPackageName(),
                            pp.getInputId(),
                            pp.getParameters());
                    updateDatabaseOnPictureProfileAndNotifyManagerAndHal(values,
                            pp.getParameters());
                    if (isPackageDefaultPictureProfile(pp)) {
                        mPackageDefaultPictureProfileHandleMap.put(
                            pp.getPackageName(), pp.getHandle().getId());
                    }
                }
            });
        }

        private boolean hasPermissionToUpdatePictureProfile(Long dbId, PictureProfile toUpdate) {
            PictureProfile fromDb = mMqDatabaseUtils.getPictureProfile(dbId);
            return fromDb.getProfileType() == toUpdate.getProfileType()
                    && fromDb.getPackageName().equals(toUpdate.getPackageName())
                    && fromDb.getName().equals(toUpdate.getName())
                    && fromDb.getName().equals(getPackageOfCallingUid());
        }

        @GuardedBy("mPictureProfileLock")
        @Override
        public void removePictureProfile(String id, int userId) {
            mHandler.post(() -> {
                synchronized (mPictureProfileLock) {
                    Long dbId = mPictureProfileTempIdMap.getKey(id);

                    PictureProfile toDelete = mMqDatabaseUtils.getPictureProfile(dbId);
                    if (!hasPermissionToRemovePictureProfile(toDelete)) {
                        mMqManagerNotifier.notifyOnPictureProfileError(id,
                                PictureProfile.ERROR_NO_PERMISSION,
                                Binder.getCallingUid(), Binder.getCallingPid());
                    }

                    if (dbId != null) {
                        SQLiteDatabase db = mMediaQualityDbHelper.getWritableDatabase();
                        String selection = BaseParameters.PARAMETER_ID + " = ?";
                        String[] selectionArgs = {Long.toString(dbId)};
                        int result = db.delete(mMediaQualityDbHelper.PICTURE_QUALITY_TABLE_NAME,
                                selection, selectionArgs);
                        if (result == 0) {
                            mMqManagerNotifier.notifyOnPictureProfileError(id,
                                    PictureProfile.ERROR_INVALID_ARGUMENT,
                                    Binder.getCallingUid(), Binder.getCallingPid());
                        } else {
                            mMqManagerNotifier.notifyOnPictureProfileRemoved(
                                    mPictureProfileTempIdMap.getValue(dbId), toDelete,
                                    Binder.getCallingUid(), Binder.getCallingPid());
                            mPictureProfileTempIdMap.remove(dbId);
                            mHalNotifier.notifyHalOnPictureProfileChange(dbId, null);
                        }
                    }

                    if (isPackageDefaultPictureProfile(toDelete)) {
                        mPackageDefaultPictureProfileHandleMap.remove(
                                toDelete.getPackageName());
                    }
                }
            });
        }

        private boolean hasPermissionToRemovePictureProfile(PictureProfile toDelete) {
            if (toDelete != null) {
                return toDelete.getName().equalsIgnoreCase(getPackageOfCallingUid());
            }
            return false;
        }

        @GuardedBy("mPictureProfileLock")
        @Override
        public PictureProfile getPictureProfile(int type, String name, boolean includeParams,
                int userId) {
            String selection = BaseParameters.PARAMETER_TYPE + " = ? AND "
                    + BaseParameters.PARAMETER_NAME + " = ? AND "
                    + BaseParameters.PARAMETER_PACKAGE + " = ?";
            String[] selectionArguments = {Integer.toString(type), name, getPackageOfCallingUid()};

            synchronized (mPictureProfileLock) {
                try (
                        Cursor cursor = mMqDatabaseUtils.getCursorAfterQuerying(
                                mMediaQualityDbHelper.PICTURE_QUALITY_TABLE_NAME,
                                MediaQualityUtils.getMediaProfileColumns(includeParams), selection,
                                selectionArguments)
                ) {
                    int count = cursor.getCount();
                    if (count == 0) {
                        return null;
                    }
                    if (count > 1) {
                        Log.wtf(TAG, TextUtils.formatSimple(String.valueOf(Locale.US), "%d "
                                        + "entries found for type=%d and name=%s in %s. Should"
                                        + " only ever be 0 or 1.", count, type, name,
                                mMediaQualityDbHelper.PICTURE_QUALITY_TABLE_NAME));
                        return null;
                    }
                    cursor.moveToFirst();
                    return MediaQualityUtils.convertCursorToPictureProfileWithTempId(cursor,
                            mPictureProfileTempIdMap);
                }
            }
        }

        @GuardedBy("mPictureProfileLock")
        @Override
        public List<PictureProfile> getPictureProfilesByPackage(
                String packageName, boolean includeParams, int userId) {
            if (!hasGlobalPictureQualityServicePermission()) {
                mMqManagerNotifier.notifyOnPictureProfileError(null,
                        PictureProfile.ERROR_NO_PERMISSION,
                        Binder.getCallingUid(), Binder.getCallingPid());
            }

            synchronized (mPictureProfileLock) {
                String selection = BaseParameters.PARAMETER_PACKAGE + " = ?";
                String[] selectionArguments = {packageName};
                return mMqDatabaseUtils.getPictureProfilesBasedOnConditions(MediaQualityUtils
                                .getMediaProfileColumns(includeParams),
                        selection, selectionArguments);
            }
        }

        @GuardedBy("mPictureProfileLock")
        @Override
        public List<PictureProfile> getAvailablePictureProfiles(boolean includeParams, int userId) {
            String packageName = getPackageOfCallingUid();
            if (packageName != null) {
                return getPictureProfilesByPackage(packageName, includeParams, userId);
            }
            return new ArrayList<>();
        }

        @GuardedBy("mPictureProfileLock")
        @Override
        public boolean setDefaultPictureProfile(String profileId, int userId) {
            if (!hasGlobalPictureQualityServicePermission()) {
                mMqManagerNotifier.notifyOnPictureProfileError(profileId,
                        PictureProfile.ERROR_NO_PERMISSION,
                        Binder.getCallingUid(), Binder.getCallingPid());
            }

            Long longId = mPictureProfileTempIdMap.getKey(profileId);
            if (longId == null) {
                return false;
            }
            PictureProfile pictureProfile = mMqDatabaseUtils.getPictureProfile(longId);
            PersistableBundle params = pictureProfile.getParameters();

            try {
                if (mMediaQuality != null) {
                    PictureParameters pp = new PictureParameters();
                    // put ID in params for profile update in HAL
                    // TODO: update HAL API for this case
                    params.putLong(BaseParameters.PARAMETER_ID, longId);
                    PictureParameter[] pictureParameters = MediaQualityUtils
                            .convertPersistableBundleToPictureParameterList(params);

                    Parcel parcel = Parcel.obtain();
                    setVendorPictureParameters(pp, parcel, params);

                    pp.pictureParameters = pictureParameters;

                    mMediaQuality.sendDefaultPictureParameters(pp);
                    parcel.recycle();
                    return true;
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to set default picture profile", e);
            }
            return false;
        }

        @GuardedBy("mPictureProfileLock")
        @Override
        public List<String> getPictureProfilePackageNames(int userId) {
            if (!hasGlobalPictureQualityServicePermission()) {
                mMqManagerNotifier.notifyOnPictureProfileError(null,
                        PictureProfile.ERROR_NO_PERMISSION,
                        Binder.getCallingUid(), Binder.getCallingPid());
            }
            String [] column = {BaseParameters.PARAMETER_PACKAGE};
            synchronized (mPictureProfileLock) {
                List<PictureProfile> pictureProfiles =
                        mMqDatabaseUtils.getPictureProfilesBasedOnConditions(column, null, null);
                return pictureProfiles.stream()
                        .map(PictureProfile::getPackageName)
                        .distinct()
                        .collect(Collectors.toList());
            }
        }

        @GuardedBy("mPictureProfileLock")
        @Override
        public List<PictureProfileHandle> getPictureProfileHandle(String[] ids, int userId) {
            List<PictureProfileHandle> toReturn = new ArrayList<>();
            synchronized (mPictureProfileLock) {
                for (String id : ids) {
                    Long key = mPictureProfileTempIdMap.getKey(id);
                    if (key != null) {
                        toReturn.add(new PictureProfileHandle(key));
                    } else {
                        toReturn.add(null);
                    }
                }
            }
            return toReturn;
        }

        @GuardedBy("mPictureProfileLock")
        @Override
        public long getPictureProfileHandleValue(String id, int userId) {
            synchronized (mPictureProfileLock) {
                Long value = mPictureProfileTempIdMap.getKey(id);
                return value != null ? value : -1;
            }
        }

        @GuardedBy("mPictureProfileLock")
        @Override
        public long getDefaultPictureProfileHandleValue(int userId) {
            synchronized (mPictureProfileLock) {
                String packageName = getPackageOfCallingUid();
                Long value = null;
                if (packageName != null) {
                    value = mPackageDefaultPictureProfileHandleMap.get(packageName);
                }
                return value != null ? value : -1;
            }
        }

        @GuardedBy("mPictureProfileLock")
        @Override
        public void notifyPictureProfileHandleSelection(long handle, int userId) {
            PictureProfile profile = mMqDatabaseUtils.getPictureProfile(handle);
            if (profile != null) {
                mHalNotifier.notifyHalOnPictureProfileChange(handle, profile.getParameters());
            }
        }

        public long getPictureProfileForTvInput(String inputId, int userId) {
            // TODO: cache profiles
            if (!hasGlobalPictureQualityServicePermission()) {
                mMqManagerNotifier.notifyOnPictureProfileError(null,
                        PictureProfile.ERROR_NO_PERMISSION,
                        Binder.getCallingUid(), Binder.getCallingPid());
            }
            String[] columns = {BaseParameters.PARAMETER_ID};
            String selection = BaseParameters.PARAMETER_TYPE + " = ? AND "
                    + BaseParameters.PARAMETER_NAME + " = ? AND "
                    + BaseParameters.PARAMETER_INPUT_ID + " = ?";
            String[] selectionArguments = {
                    Integer.toString(PictureProfile.TYPE_SYSTEM),
                    PictureProfile.NAME_DEFAULT,
                    inputId
            };
            synchronized (mPictureProfileLock) {
                try (Cursor cursor = mMqDatabaseUtils.getCursorAfterQuerying(
                        mMediaQualityDbHelper.PICTURE_QUALITY_TABLE_NAME,
                        columns, selection, selectionArguments)) {
                    int count = cursor.getCount();
                    if (count == 0) {
                        return -1;
                    }
                    long handle = -1;
                    cursor.moveToFirst();
                    int colIndex = cursor.getColumnIndex(BaseParameters.PARAMETER_ID);
                    if (colIndex != -1) {
                        handle = cursor.getLong(colIndex);
                    }
                    return handle;
                }
            }
        }

        @GuardedBy("mSoundProfileLock")
        @Override
        public List<SoundProfileHandle> getSoundProfileHandle(String[] ids, int userId) {
            List<SoundProfileHandle> toReturn = new ArrayList<>();
            synchronized (mSoundProfileLock) {
                for (String id : ids) {
                    Long key = mSoundProfileTempIdMap.getKey(id);
                    if (key != null) {
                        toReturn.add(MediaQualityUtils.SOUND_PROFILE_HANDLE_NONE);
                    } else {
                        toReturn.add(null);
                    }
                }
            }
            return toReturn;
        }

        @GuardedBy("mSoundProfileLock")
        @Override
        public void createSoundProfile(SoundProfile sp, int userId) {
            mHandler.post(() -> {
                if ((sp.getPackageName() != null && !sp.getPackageName().isEmpty()
                        && !incomingPackageEqualsCallingUidPackage(sp.getPackageName()))
                        && !hasGlobalSoundQualityServicePermission()) {
                    mMqManagerNotifier.notifyOnSoundProfileError(null, SoundProfile.ERROR_NO_PERMISSION,
                            Binder.getCallingUid(), Binder.getCallingPid());
                }

                synchronized (mSoundProfileLock) {
                    SQLiteDatabase db = mMediaQualityDbHelper.getWritableDatabase();

                    ContentValues values = MediaQualityUtils.getContentValues(null,
                            sp.getProfileType(),
                            sp.getName(),
                            sp.getPackageName() == null || sp.getPackageName().isEmpty()
                                    ? getPackageOfCallingUid() : sp.getPackageName(),
                            sp.getInputId(),
                            sp.getParameters());

                    // id is auto-generated by SQLite upon successful insertion of row
                    Long id = db.insert(mMediaQualityDbHelper.SOUND_QUALITY_TABLE_NAME,
                            null, values);
                    MediaQualityUtils.populateTempIdMap(mSoundProfileTempIdMap, id);
                    String value = mSoundProfileTempIdMap.getValue(id);
                    sp.setProfileId(value);
                    mMqManagerNotifier.notifyOnSoundProfileAdded(value, sp, Binder.getCallingUid(),
                            Binder.getCallingPid());
                }
            });
        }

        @GuardedBy("mSoundProfileLock")
        @Override
        public void updateSoundProfile(String id, SoundProfile sp, int userId) {
            mHandler.post(() -> {
                Long dbId = mSoundProfileTempIdMap.getKey(id);
                if (!hasPermissionToUpdateSoundProfile(dbId, sp)) {
                    mMqManagerNotifier.notifyOnSoundProfileError(id,
                            SoundProfile.ERROR_NO_PERMISSION,
                            Binder.getCallingUid(), Binder.getCallingPid());
                }

                synchronized (mSoundProfileLock) {
                    ContentValues values = MediaQualityUtils.getContentValues(dbId,
                            sp.getProfileType(),
                            sp.getName(),
                            sp.getPackageName(),
                            sp.getInputId(),
                            sp.getParameters());

                    updateDatabaseOnSoundProfileAndNotifyManagerAndHal(values, sp.getParameters());
                }
            });
        }

        private boolean hasPermissionToUpdateSoundProfile(Long dbId, SoundProfile sp) {
            SoundProfile fromDb = mMqDatabaseUtils.getSoundProfile(dbId);
            return fromDb.getProfileType() == sp.getProfileType()
                    && fromDb.getPackageName().equals(sp.getPackageName())
                    && fromDb.getName().equals(sp.getName())
                    && fromDb.getName().equals(getPackageOfCallingUid());
        }

        @GuardedBy("mSoundProfileLock")
        @Override
        public void removeSoundProfile(String id, int userId) {
            mHandler.post(() -> {
                synchronized (mSoundProfileLock) {
                    Long dbId = mSoundProfileTempIdMap.getKey(id);
                    SoundProfile toDelete = mMqDatabaseUtils.getSoundProfile(dbId);
                    if (!hasPermissionToRemoveSoundProfile(toDelete)) {
                        mMqManagerNotifier.notifyOnSoundProfileError(id,
                                SoundProfile.ERROR_NO_PERMISSION,
                                Binder.getCallingUid(), Binder.getCallingPid());
                    }
                    if (dbId != null) {
                        SQLiteDatabase db = mMediaQualityDbHelper.getWritableDatabase();
                        String selection = BaseParameters.PARAMETER_ID + " = ?";
                        String[] selectionArgs = {Long.toString(dbId)};
                        int result = db.delete(mMediaQualityDbHelper.SOUND_QUALITY_TABLE_NAME,
                                selection,
                                selectionArgs);
                        if (result == 0) {
                            mMqManagerNotifier.notifyOnSoundProfileError(id,
                                    SoundProfile.ERROR_INVALID_ARGUMENT,
                                    Binder.getCallingUid(), Binder.getCallingPid());
                        } else {
                            mMqManagerNotifier.notifyOnSoundProfileRemoved(
                                    mSoundProfileTempIdMap.getValue(dbId), toDelete,
                                    Binder.getCallingUid(), Binder.getCallingPid());
                            mSoundProfileTempIdMap.remove(dbId);
                            mHalNotifier.notifyHalOnSoundProfileChange(dbId, null);
                        }
                    }
                }
            });
        }

        private boolean hasPermissionToRemoveSoundProfile(SoundProfile toDelete) {
            if (toDelete != null) {
                return toDelete.getName().equalsIgnoreCase(getPackageOfCallingUid());
            }
            return false;
        }

        @GuardedBy("mSoundProfileLock")
        @Override
        public SoundProfile getSoundProfile(int type, String name, boolean includeParams,
                int userId) {
            String selection = BaseParameters.PARAMETER_TYPE + " = ? AND "
                    + BaseParameters.PARAMETER_NAME + " = ? AND "
                    + BaseParameters.PARAMETER_PACKAGE + " = ?";
            String[] selectionArguments = {String.valueOf(type), name, getPackageOfCallingUid()};

            synchronized (mSoundProfileLock) {
                try (
                        Cursor cursor = mMqDatabaseUtils.getCursorAfterQuerying(
                                mMediaQualityDbHelper.SOUND_QUALITY_TABLE_NAME,
                                MediaQualityUtils.getMediaProfileColumns(includeParams), selection,
                                selectionArguments)
                ) {
                    int count = cursor.getCount();
                    if (count == 0) {
                        return null;
                    }
                    if (count > 1) {
                        Log.wtf(TAG, TextUtils.formatSimple(String.valueOf(Locale.US), "%d "
                                        + "entries found for name=%s in %s. Should only ever "
                                        + "be 0 or 1.", String.valueOf(count), name,
                                mMediaQualityDbHelper.SOUND_QUALITY_TABLE_NAME));
                        return null;
                    }
                    cursor.moveToFirst();
                    return MediaQualityUtils.convertCursorToSoundProfileWithTempId(cursor,
                            mSoundProfileTempIdMap);
                }
            }
        }

        @GuardedBy("mSoundProfileLock")
        @Override
        public List<SoundProfile> getSoundProfilesByPackage(
                String packageName, boolean includeParams, int userId) {
            if (!hasGlobalSoundQualityServicePermission()) {
                mMqManagerNotifier.notifyOnSoundProfileError(null, SoundProfile.ERROR_NO_PERMISSION,
                        Binder.getCallingUid(), Binder.getCallingPid());
            }

            synchronized (mSoundProfileLock) {
                String selection = BaseParameters.PARAMETER_PACKAGE + " = ?";
                String[] selectionArguments = {packageName};
                return mMqDatabaseUtils.getSoundProfilesBasedOnConditions(MediaQualityUtils
                                .getMediaProfileColumns(includeParams),
                        selection, selectionArguments);
            }
        }

        @GuardedBy("mSoundProfileLock")
        @Override
        public List<SoundProfile> getAvailableSoundProfiles(boolean includeParams, int userId) {
            String packageName = getPackageOfCallingUid();
            if (packageName != null) {
                return getSoundProfilesByPackage(packageName, includeParams, userId);
            }
            return new ArrayList<>();
        }

        @GuardedBy("mSoundProfileLock")
        @Override
        public boolean setDefaultSoundProfile(String profileId, int userId) {
            if (!hasGlobalSoundQualityServicePermission()) {
                mMqManagerNotifier.notifyOnSoundProfileError(profileId,
                        SoundProfile.ERROR_NO_PERMISSION,
                        Binder.getCallingUid(), Binder.getCallingPid());
            }

            Long longId = mSoundProfileTempIdMap.getKey(profileId);
            if (longId == null) {
                return false;
            }

            SoundProfile soundProfile = mMqDatabaseUtils.getSoundProfile(longId);
            PersistableBundle params = soundProfile.getParameters();

            try {
                if (mMediaQuality != null) {
                    SoundParameters sp = new SoundParameters();
                    // put ID in params for profile update in HAL
                    // TODO: update HAL API for this case
                    params.putLong(BaseParameters.PARAMETER_ID, longId);
                    SoundParameter[] soundParameters =
                            MediaQualityUtils.convertPersistableBundleToSoundParameterList(params);

                    Parcel parcel = Parcel.obtain();
                    setVendorSoundParameters(sp, parcel, params);
                    sp.soundParameters = soundParameters;

                    mMediaQuality.sendDefaultSoundParameters(sp);
                    return true;
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to set default sound profile", e);
            }
            return false;
        }

        @GuardedBy("mSoundProfileLock")
        @Override
        public List<String> getSoundProfilePackageNames(int userId) {
            if (!hasGlobalSoundQualityServicePermission()) {
                mMqManagerNotifier.notifyOnSoundProfileError(null, SoundProfile.ERROR_NO_PERMISSION,
                        Binder.getCallingUid(), Binder.getCallingPid());
            }
            String [] column = {BaseParameters.PARAMETER_NAME};

            synchronized (mSoundProfileLock) {
                List<SoundProfile> soundProfiles =
                        mMqDatabaseUtils.getSoundProfilesBasedOnConditions(column,
                        null, null);
                return soundProfiles.stream()
                        .map(SoundProfile::getPackageName)
                        .distinct()
                        .collect(Collectors.toList());
            }
        }

        private String getPackageOfCallingUid() {
            String[] packageNames = mPackageManager.getPackagesForUid(
                    Binder.getCallingUid());
            if (packageNames != null && packageNames.length == 1 && !packageNames[0].isEmpty()) {
                return packageNames[0];
            }
            return null;
        }

        private boolean incomingPackageEqualsCallingUidPackage(String incomingPackage) {
            return incomingPackage.equalsIgnoreCase(getPackageOfCallingUid());
        }

        private boolean hasGlobalPictureQualityServicePermission() {
            return mContext.checkCallingPermission(
                    android.Manifest.permission.MANAGE_GLOBAL_PICTURE_QUALITY_SERVICE)
                    == PackageManager.PERMISSION_GRANTED;
        }

        private boolean hasGlobalSoundQualityServicePermission() {
            return mContext.checkCallingPermission(
                    android.Manifest.permission.MANAGE_GLOBAL_SOUND_QUALITY_SERVICE)
                    == PackageManager.PERMISSION_GRANTED;
        }

        private boolean hasReadColorZonesPermission() {
            return mContext.checkCallingPermission(
                    android.Manifest.permission.READ_COLOR_ZONES)
                    == PackageManager.PERMISSION_GRANTED;
        }

        @Override
        public void registerPictureProfileCallback(final IPictureProfileCallback callback) {
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();

            UserState userState = getOrCreateUserState(Binder.getCallingUid());
            userState.mPictureProfileCallbackPidUidMap.put(callback,
                    Pair.create(callingPid, callingUid));
        }

        @Override
        public void registerSoundProfileCallback(final ISoundProfileCallback callback) {
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();

            UserState userState = getOrCreateUserState(Binder.getCallingUid());
            userState.mSoundProfileCallbackPidUidMap.put(callback,
                    Pair.create(callingPid, callingUid));
        }

        @Override
        public void registerActiveProcessingPictureListener(
                final IActiveProcessingPictureListener l) {
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();

            UserState userState = getOrCreateUserState(Binder.getCallingUid());
            String packageName = getPackageOfCallingUid();
            userState.mActiveProcessingPictureListenerMap.put(l,
                    new ActiveProcessingPictureListenerInfo(callingUid, callingPid, packageName));
        }

        @Override
        public void registerAmbientBacklightCallback(IAmbientBacklightCallback callback) {
            if (DEBUG) {
                Slogf.d(TAG, "registerAmbientBacklightCallback");
            }

            if (!hasReadColorZonesPermission()) {
                //TODO: error handling
            }

            String callingPackageName = getPackageOfCallingUid();

            synchronized (mCallbackRecords) {
                AmbientBacklightCallbackRecord record = mCallbackRecords.get(callingPackageName);
                if (record != null) {
                    if (record.mCallback.asBinder().equals(callback.asBinder())) {
                        Slog.w(TAG, "AmbientBacklight Callback already registered");
                        return;
                    }
                    record.release();
                    mCallbackRecords.remove(callingPackageName);
                }
                mCallbackRecords.put(callingPackageName,
                        new AmbientBacklightCallbackRecord(callingPackageName, callback));
            }
        }

        public void unregisterAmbientBacklightCallback(IAmbientBacklightCallback callback) {
            if (DEBUG) {
                Slogf.d(TAG, "unregisterAmbientBacklightCallback");
            }

            if (!hasReadColorZonesPermission()) {
                //TODO: error handling
            }

            synchronized (mCallbackRecords) {
                for (AmbientBacklightCallbackRecord record : mCallbackRecords.values()) {
                    if (record.mCallback.asBinder().equals(callback.asBinder())) {
                        record.release();
                        mCallbackRecords.remove(record.mPackageName);
                        return;
                    }
                }
            }
        }

        @GuardedBy("mAmbientBacklightLock")
        @Override
        public void setAmbientBacklightSettings(
                AmbientBacklightSettings settings, int userId) {
            if (DEBUG) {
                Slogf.d(TAG, "setAmbientBacklightSettings " + settings);
            }

            if (!hasReadColorZonesPermission()) {
                //TODO: error handling
            }

            try {
                if (mMediaQuality != null) {
                    android.hardware.tv.mediaquality.AmbientBacklightSettings halSettings =
                            new android.hardware.tv.mediaquality.AmbientBacklightSettings();
                    halSettings.uid = Binder.getCallingUid();
                    halSettings.source = (byte) settings.getSource();
                    halSettings.maxFramerate = settings.getMaxFps();
                    halSettings.colorFormat = (byte) settings.getColorFormat();
                    halSettings.hZonesNumber = settings.getHorizontalZonesCount();
                    halSettings.vZonesNumber = settings.getVerticalZonesCount();
                    halSettings.hasLetterbox = settings.isLetterboxOmitted();
                    halSettings.colorThreshold = settings.getThreshold();

                    mMediaQuality.setAmbientBacklightDetector(halSettings);

                    mHalAmbientBacklightCallback.setAmbientBacklightClientPackageName(
                            getPackageOfCallingUid());

                    if (DEBUG) {
                        Slogf.d(TAG, "set ambient settings package: " + halSettings.uid);
                    }
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to set ambient backlight settings", e);
            }
        }

        @GuardedBy("mAmbientBacklightLock")
        @Override
        public void setAmbientBacklightEnabled(boolean enabled, int userId) {
            if (DEBUG) {
                Slogf.d(TAG, "setAmbientBacklightEnabled " + enabled);
            }
            if (!hasReadColorZonesPermission()) {
                //TODO: error handling
            }
            try {
                if (mMediaQuality != null) {
                    mMediaQuality.setAmbientBacklightDetectionEnabled(enabled);
                }
            } catch (UnsupportedOperationException e) {
                Slog.e(TAG, "The current device is not supported");
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to set ambient backlight enabled", e);
            }
        }

        @Override
        public List<ParameterCapability> getParameterCapabilities(
                List<String> names, int userId) {
            byte[] byteArray = MediaQualityUtils.convertParameterToByteArray(names);
            ParamCapability[] caps = new ParamCapability[byteArray.length];
            try {
                mMediaQuality.getParamCaps(byteArray, caps);
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to get parameter capabilities", e);
            }

            return getListParameterCapability(caps);
        }

        private List<ParameterCapability> getListParameterCapability(ParamCapability[] caps) {
            List<ParameterCapability> pcList = new ArrayList<>();

            if (caps != null) {
                for (ParamCapability pcHal : caps) {
                    if (pcHal != null) {
                        String name = MediaQualityUtils.getParameterName(pcHal.name);
                        boolean isSupported = pcHal.isSupported;
                        int type = pcHal.defaultValue == null ? 0 : pcHal.defaultValue.getTag() + 1;
                        Bundle bundle = MediaQualityUtils.convertToCaps(type, pcHal.range);

                        pcList.add(new ParameterCapability(name, isSupported, type, bundle));
                    }
                }
            }

            return pcList;
        }

        @GuardedBy("mPictureProfileLock")
        @Override
        public List<String> getPictureProfileAllowList(int userId) {
            if (!hasGlobalPictureQualityServicePermission()) {
                mMqManagerNotifier.notifyOnPictureProfileError(null,
                        PictureProfile.ERROR_NO_PERMISSION,
                        Binder.getCallingUid(), Binder.getCallingPid());
            }
            String allowlist = mPictureProfileSharedPreference.getString(ALLOWLIST, null);
            if (allowlist != null) {
                String[] stringArray = allowlist.split(COMMA_DELIMITER);
                return new ArrayList<>(Arrays.asList(stringArray));
            }
            return new ArrayList<>();
        }

        @GuardedBy("mPictureProfileLock")
        @Override
        public void setPictureProfileAllowList(List<String> packages, int userId) {
            mHandler.post(() -> {
                if (!hasGlobalPictureQualityServicePermission()) {
                    mMqManagerNotifier.notifyOnPictureProfileError(null,
                            PictureProfile.ERROR_NO_PERMISSION,
                            Binder.getCallingUid(), Binder.getCallingPid());
                }
                SharedPreferences.Editor editor = mPictureProfileSharedPreference.edit();
                editor.putString(ALLOWLIST, String.join(COMMA_DELIMITER, packages));
                editor.commit();
            });
        }

        @GuardedBy("mSoundProfileLock")
        @Override
        public List<String> getSoundProfileAllowList(int userId) {
            if (!hasGlobalSoundQualityServicePermission()) {
                mMqManagerNotifier.notifyOnSoundProfileError(null, SoundProfile.ERROR_NO_PERMISSION,
                        Binder.getCallingUid(), Binder.getCallingPid());
            }
            String allowlist = mSoundProfileSharedPreference.getString(ALLOWLIST, null);
            if (allowlist != null) {
                String[] stringArray = allowlist.split(COMMA_DELIMITER);
                return new ArrayList<>(Arrays.asList(stringArray));
            }
            return new ArrayList<>();
        }

        @GuardedBy("mSoundProfileLock")
        @Override
        public void setSoundProfileAllowList(List<String> packages, int userId) {
            mHandler.post(() -> {
                if (!hasGlobalSoundQualityServicePermission()) {
                    mMqManagerNotifier.notifyOnSoundProfileError(null,
                            SoundProfile.ERROR_NO_PERMISSION,
                            Binder.getCallingUid(), Binder.getCallingPid());
                }
                SharedPreferences.Editor editor = mSoundProfileSharedPreference.edit();
                editor.putString(ALLOWLIST, String.join(COMMA_DELIMITER, packages));
                editor.commit();
            });
        }

        @Override
        public boolean isSupported(int userId) {
            return false;
        }

        @GuardedBy("mPictureProfileLock")
        @Override
        public void setAutoPictureQualityEnabled(boolean enabled, int userId) {
            mHandler.post(() -> {
                if (!hasGlobalPictureQualityServicePermission()) {
                    mMqManagerNotifier.notifyOnPictureProfileError(null,
                            PictureProfile.ERROR_NO_PERMISSION,
                            Binder.getCallingUid(), Binder.getCallingPid());
                }
                synchronized (mPictureProfileLock) {
                    try {
                        if (mMediaQuality != null) {
                            if (mMediaQuality.isAutoPqSupported()) {
                                mMediaQuality.setAutoPqEnabled(enabled);
                            }
                        }
                    } catch (RemoteException e) {
                        Slog.e(TAG, "Failed to set auto picture quality", e);
                    }
                }
            });
        }

        @GuardedBy("mPictureProfileLock")
        @Override
        public boolean isAutoPictureQualityEnabled(int userId) {
            synchronized (mPictureProfileLock) {
                try {
                    if (mMediaQuality != null) {
                        if (mMediaQuality.isAutoPqSupported()) {
                            return mMediaQuality.getAutoPqEnabled();
                        }
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to get auto picture quality", e);
                }
                return false;
            }
        }

        @GuardedBy("mPictureProfileLock")
        @Override
        public void setSuperResolutionEnabled(boolean enabled, int userId) {
            mHandler.post(() -> {
                if (!hasGlobalPictureQualityServicePermission()) {
                    mMqManagerNotifier.notifyOnPictureProfileError(null,
                            PictureProfile.ERROR_NO_PERMISSION,
                            Binder.getCallingUid(), Binder.getCallingPid());
                }
                synchronized (mPictureProfileLock) {
                    try {
                        if (mMediaQuality != null) {
                            if (mMediaQuality.isAutoSrSupported()) {
                                mMediaQuality.setAutoSrEnabled(enabled);
                            }
                        }
                    } catch (RemoteException e) {
                        Slog.e(TAG, "Failed to set super resolution", e);
                    }
                }
            });
        }

        @GuardedBy("mPictureProfileLock")
        @Override
        public boolean isSuperResolutionEnabled(int userId) {
            synchronized (mPictureProfileLock) {
                try {
                    if (mMediaQuality != null) {
                        if (mMediaQuality.isAutoSrSupported()) {
                            return mMediaQuality.getAutoSrEnabled();
                        }
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to get super resolution", e);
                }
                return false;
            }
        }

        @GuardedBy("mSoundProfileLock")
        @Override
        public void setAutoSoundQualityEnabled(boolean enabled, int userId) {
            mHandler.post(() -> {
                if (!hasGlobalSoundQualityServicePermission()) {
                    mMqManagerNotifier.notifyOnSoundProfileError(null,
                            SoundProfile.ERROR_NO_PERMISSION,
                            Binder.getCallingUid(), Binder.getCallingPid());
                }

                synchronized (mSoundProfileLock) {
                    try {
                        if (mMediaQuality != null) {
                            if (mMediaQuality.isAutoAqSupported()) {
                                mMediaQuality.setAutoAqEnabled(enabled);
                            }
                        }
                    } catch (RemoteException e) {
                        Slog.e(TAG, "Failed to set auto sound quality", e);
                    }
                }
            });
        }

        @GuardedBy("mSoundProfileLock")
        @Override
        public boolean isAutoSoundQualityEnabled(int userId) {
            synchronized (mSoundProfileLock) {
                try {
                    if (mMediaQuality != null) {
                        if (mMediaQuality.isAutoAqSupported()) {
                            return mMediaQuality.getAutoAqEnabled();
                        }
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to get auto sound quality", e);
                }
                return false;
            }
        }

        @GuardedBy("mAmbientBacklightLock")
        @Override
        public boolean isAmbientBacklightEnabled(int userId) {
            return false;
        }
    }

    public void updatePictureProfileFromHal(Long dbId, PersistableBundle bundle) {
        ContentValues values = MediaQualityUtils.getContentValues(dbId,
                null,
                null,
                null,
                null,
                bundle);

        updateDatabaseOnPictureProfileAndNotifyManagerAndHal(values, bundle);
    }

    public void updateDatabaseOnPictureProfileAndNotifyManagerAndHal(ContentValues values,
            PersistableBundle bundle) {
        SQLiteDatabase db = mMediaQualityDbHelper.getWritableDatabase();
        db.replace(mMediaQualityDbHelper.PICTURE_QUALITY_TABLE_NAME,
                null, values);
        Long dbId = values.getAsLong(BaseParameters.PARAMETER_ID);
        mMqManagerNotifier.notifyOnPictureProfileUpdated(mPictureProfileTempIdMap.getValue(dbId),
                mMqDatabaseUtils.getPictureProfile(dbId), Binder.getCallingUid(),
                Binder.getCallingPid());
        mHalNotifier.notifyHalOnPictureProfileChange(dbId, bundle);
    }

    public void updateSoundProfileFromHal(Long dbId, PersistableBundle bundle) {
        ContentValues values = MediaQualityUtils.getContentValues(dbId,
                null,
                null,
                null,
                null,
                bundle);

        updateDatabaseOnSoundProfileAndNotifyManagerAndHal(values, bundle);
    }

    public void updateDatabaseOnSoundProfileAndNotifyManagerAndHal(ContentValues values,
            PersistableBundle bundle) {
        SQLiteDatabase db = mMediaQualityDbHelper.getWritableDatabase();
        db.replace(mMediaQualityDbHelper.SOUND_QUALITY_TABLE_NAME,
                null, values);
        Long dbId = values.getAsLong(BaseParameters.PARAMETER_ID);
        mMqManagerNotifier.notifyOnSoundProfileUpdated(mSoundProfileTempIdMap.getValue(dbId),
                mMqDatabaseUtils.getSoundProfile(dbId), Binder.getCallingUid(),
                Binder.getCallingPid());
        mHalNotifier.notifyHalOnSoundProfileChange(dbId, bundle);
    }

    private class MediaQualityManagerPictureProfileCallbackList extends
            RemoteCallbackList<IPictureProfileCallback> {
        @Override
        public void onCallbackDied(IPictureProfileCallback callback) {
            synchronized (mPictureProfileLock) {
                for (int i = 0; i < mUserStates.size(); i++) {
                    int userId = mUserStates.keyAt(i);
                    UserState userState = getOrCreateUserState(userId);
                    userState.mPictureProfileCallbackPidUidMap.remove(callback);
                }
            }
        }
    }

    private class MediaQualityManagerSoundProfileCallbackList extends
            RemoteCallbackList<ISoundProfileCallback> {
        @Override
        public void onCallbackDied(ISoundProfileCallback callback) {
            synchronized (mSoundProfileLock) {
                for (int i = 0; i < mUserStates.size(); i++) {
                    int userId = mUserStates.keyAt(i);
                    UserState userState = getOrCreateUserState(userId);
                    userState.mSoundProfileCallbackPidUidMap.remove(callback);
                }
            }
        }
    }

    private class ActiveProcessingPictureCallbackList extends
            RemoteCallbackList<IActiveProcessingPictureListener> {
        @Override
        public void onCallbackDied(IActiveProcessingPictureListener l) {
            synchronized (mPictureProfileLock) {
                for (int i = 0; i < mUserStates.size(); i++) {
                    int userId = mUserStates.keyAt(i);
                    UserState userState = getOrCreateUserState(userId);
                    userState.mActiveProcessingPictureListenerMap.remove(l);
                }
            }
        }
    }

    private final class UserState {
        // A list of callbacks.
        private final MediaQualityManagerPictureProfileCallbackList mPictureProfileCallbacks =
                new MediaQualityManagerPictureProfileCallbackList();

        private final MediaQualityManagerSoundProfileCallbackList mSoundProfileCallbacks =
                new MediaQualityManagerSoundProfileCallbackList();

        private final ActiveProcessingPictureCallbackList mActiveProcessingPictureCallbackList =
                new ActiveProcessingPictureCallbackList();

        private final Map<IPictureProfileCallback, Pair<Integer, Integer>>
                mPictureProfileCallbackPidUidMap = new HashMap<>();

        private final Map<ISoundProfileCallback, Pair<Integer, Integer>>
                mSoundProfileCallbackPidUidMap = new HashMap<>();

        private final Map<IActiveProcessingPictureListener, ActiveProcessingPictureListenerInfo>
                mActiveProcessingPictureListenerMap = new HashMap<>();

        private UserState(Context context, int userId) {

        }
    }

    private final class ActiveProcessingPictureListenerInfo {
        private int mUid;
        private int mPid;
        private String mPackageName;

        ActiveProcessingPictureListenerInfo(int uid, int pid, String packageName) {
            mUid = uid;
            mPid = pid;
            mPackageName = packageName;
        }
    }

    private UserState getOrCreateUserState(int userId) {
        UserState userState = getUserState(userId);
        if (userState == null) {
            userState = new UserState(mContext, userId);
            synchronized (mUserStateLock) {
                mUserStates.put(userId, userState);
            }
        }
        return userState;
    }

    @GuardedBy("mUserStateLock")
    private UserState getUserState(int userId) {
        synchronized (mUserStateLock) {
            return mUserStates.get(userId);
        }
    }

    private final class MqDatabaseUtils {

        private PictureProfile getPictureProfile(Long dbId) {
            return getPictureProfile(dbId, false);
        }

        private PictureProfile getPictureProfile(Long dbId, boolean includeParams) {
            String selection = BaseParameters.PARAMETER_ID + " = ?";
            String[] selectionArguments = {Long.toString(dbId)};

            try (Cursor cursor = getCursorAfterQuerying(
                    mMediaQualityDbHelper.PICTURE_QUALITY_TABLE_NAME,
                    MediaQualityUtils.getMediaProfileColumns(includeParams), selection,
                    selectionArguments)) {
                int count = cursor.getCount();
                if (count == 0) {
                    return null;
                }
                if (count > 1) {
                    Log.wtf(TAG, TextUtils.formatSimple(String.valueOf(Locale.US), "%d entries "
                                    + "found for id=%d in %s. Should only ever be 0 or 1.",
                            count, dbId, mMediaQualityDbHelper.PICTURE_QUALITY_TABLE_NAME));
                    return null;
                }
                cursor.moveToFirst();
                return MediaQualityUtils.convertCursorToPictureProfileWithTempId(cursor,
                        mPictureProfileTempIdMap);
            }
        }

        private List<PictureProfile> getPictureProfilesBasedOnConditions(String[] columns,
                String selection, String[] selectionArguments) {
            try (Cursor cursor = getCursorAfterQuerying(
                    mMediaQualityDbHelper.PICTURE_QUALITY_TABLE_NAME, columns, selection,
                    selectionArguments)) {
                List<PictureProfile> pictureProfiles = new ArrayList<>();
                while (cursor.moveToNext()) {
                    pictureProfiles.add(MediaQualityUtils.convertCursorToPictureProfileWithTempId(
                            cursor, mPictureProfileTempIdMap));
                }
                return pictureProfiles;
            }
        }

        private SoundProfile getSoundProfile(Long dbId) {
            String selection = BaseParameters.PARAMETER_ID + " = ?";
            String[] selectionArguments = {Long.toString(dbId)};

            try (Cursor cursor = mMqDatabaseUtils.getCursorAfterQuerying(
                    mMediaQualityDbHelper.SOUND_QUALITY_TABLE_NAME,
                    MediaQualityUtils.getMediaProfileColumns(false), selection,
                    selectionArguments)) {
                int count = cursor.getCount();
                if (count == 0) {
                    return null;
                }
                if (count > 1) {
                    Log.wtf(TAG, TextUtils.formatSimple(String.valueOf(Locale.US), "%d entries "
                                    + "found for id=%s in %s. Should only ever be 0 or 1.", count,
                            dbId, mMediaQualityDbHelper.SOUND_QUALITY_TABLE_NAME));
                    return null;
                }
                cursor.moveToFirst();
                return MediaQualityUtils.convertCursorToSoundProfileWithTempId(
                        cursor, mSoundProfileTempIdMap);
            }
        }

        private List<SoundProfile> getSoundProfilesBasedOnConditions(String[] columns,
                String selection, String[] selectionArguments) {
            try (Cursor cursor = mMqDatabaseUtils.getCursorAfterQuerying(
                    mMediaQualityDbHelper.SOUND_QUALITY_TABLE_NAME, columns, selection,
                    selectionArguments)) {
                List<SoundProfile> soundProfiles = new ArrayList<>();
                while (cursor.moveToNext()) {
                    soundProfiles.add(MediaQualityUtils.convertCursorToSoundProfileWithTempId(
                            cursor, mSoundProfileTempIdMap));
                }
                return soundProfiles;
            }
        }

        private Cursor getCursorAfterQuerying(String table, String[] columns, String selection,
                String[] selectionArgs) {
            SQLiteDatabase db = mMediaQualityDbHelper.getReadableDatabase();
            return db.query(table, columns, selection, selectionArgs,
                    /*groupBy=*/ null, /*having=*/ null, /*orderBy=*/ null);
        }

        private MqDatabaseUtils() {
        }
    }

    private final class MqManagerNotifier {

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        private @interface ProfileModes {
            int ADD = 1;
            int UPDATE = 2;
            int REMOVE = 3;
            int ERROR = 4;
            int PARAMETER_CAPABILITY_CHANGED = 5;
        }

        private void notifyOnPictureProfileAdded(String profileId, PictureProfile profile,
                int uid, int pid) {
            notifyPictureProfileHelper(ProfileModes.ADD, profileId, profile, null, null, uid, pid);
        }

        private void notifyOnPictureProfileUpdated(String profileId, PictureProfile profile,
                int uid, int pid) {
            notifyPictureProfileHelper(ProfileModes.UPDATE, profileId, profile, null, null, uid,
                    pid);
        }

        private void notifyOnPictureProfileRemoved(String profileId, PictureProfile profile,
                int uid, int pid) {
            notifyPictureProfileHelper(ProfileModes.REMOVE, profileId, profile, null, null, uid,
                    pid);
        }

        private void notifyOnPictureProfileError(String profileId, int errorCode,
                int uid, int pid) {
            notifyPictureProfileHelper(ProfileModes.ERROR, profileId, null, errorCode, null, uid,
                    pid);
        }

        private void notifyOnPictureProfileParameterCapabilitiesChanged(Long profileId,
                ParamCapability[] caps, int uid, int pid) {
            String uuid = mPictureProfileTempIdMap.getValue(profileId);
            List<ParameterCapability> paramCaps = new ArrayList<>();
            for (ParamCapability cap: caps) {
                String name = MediaQualityUtils.getParameterName(cap.name);
                boolean isSupported = cap.isSupported;
                int type = cap.defaultValue == null ? 0 : cap.defaultValue.getTag() + 1;
                Bundle bundle = MediaQualityUtils.convertToCaps(type, cap.range);

                paramCaps.add(new ParameterCapability(name, isSupported, type, bundle));
            }
            notifyPictureProfileHelper(ProfileModes.PARAMETER_CAPABILITY_CHANGED, uuid,
                    null, null, paramCaps , uid, pid);
        }

        private void notifyPictureProfileHelper(int mode, String profileId,
                PictureProfile profile, Integer errorCode,
                List<ParameterCapability> paramCaps, int uid, int pid) {
            UserState userState = getOrCreateUserState(UserHandle.USER_SYSTEM);
            int n = userState.mPictureProfileCallbacks.beginBroadcast();

            for (int i = 0; i < n; ++i) {
                try {
                    IPictureProfileCallback callback = userState.mPictureProfileCallbacks
                            .getBroadcastItem(i);
                    Pair<Integer, Integer> pidUid = userState.mPictureProfileCallbackPidUidMap
                            .get(callback);

                    if (pidUid.first == pid && pidUid.second == uid) {
                        if (mode == ProfileModes.ADD) {
                            userState.mPictureProfileCallbacks.getBroadcastItem(i)
                                    .onPictureProfileAdded(profileId, profile);
                        } else if (mode == ProfileModes.UPDATE) {
                            userState.mPictureProfileCallbacks.getBroadcastItem(i)
                                    .onPictureProfileUpdated(profileId, profile);
                        } else if (mode == ProfileModes.REMOVE) {
                            userState.mPictureProfileCallbacks.getBroadcastItem(i)
                                    .onPictureProfileRemoved(profileId, profile);
                        } else if (mode == ProfileModes.ERROR) {
                            userState.mPictureProfileCallbacks.getBroadcastItem(i)
                                    .onError(profileId, errorCode);
                        } else if (mode == ProfileModes.PARAMETER_CAPABILITY_CHANGED) {
                            userState.mPictureProfileCallbacks.getBroadcastItem(i)
                                    .onParameterCapabilitiesChanged(profileId, paramCaps);
                        }
                    }
                } catch (RemoteException e) {
                    if (mode == ProfileModes.ADD) {
                        Slog.e(TAG, "Failed to report added picture profile to callback", e);
                    } else if (mode == ProfileModes.UPDATE) {
                        Slog.e(TAG, "Failed to report updated picture profile to callback", e);
                    } else if (mode == ProfileModes.REMOVE) {
                        Slog.e(TAG, "Failed to report removed picture profile to callback", e);
                    } else if (mode == ProfileModes.ERROR) {
                        Slog.e(TAG, "Failed to report picture profile error to callback", e);
                    } else if (mode == ProfileModes.PARAMETER_CAPABILITY_CHANGED) {
                        Slog.e(TAG, "Failed to report picture profile parameter capability change "
                                + "to callback", e);
                    }
                }
            }
            userState.mPictureProfileCallbacks.finishBroadcast();
        }

        private void notifyOnSoundProfileAdded(String profileId, SoundProfile profile,
                int uid, int pid) {
            notifySoundProfileHelper(ProfileModes.ADD, profileId, profile, null, null, uid, pid);
        }

        private void notifyOnSoundProfileUpdated(String profileId, SoundProfile profile,
                int uid, int pid) {
            notifySoundProfileHelper(ProfileModes.UPDATE, profileId, profile, null, null, uid, pid);
        }

        private void notifyOnSoundProfileRemoved(String profileId, SoundProfile profile,
                int uid, int pid) {
            notifySoundProfileHelper(ProfileModes.REMOVE, profileId, profile, null, null, uid, pid);
        }

        private void notifyOnSoundProfileError(String profileId, int errorCode, int uid, int pid) {
            notifySoundProfileHelper(ProfileModes.ERROR, profileId, null, errorCode, null, uid,
                    pid);
        }

        private void notifyOnSoundProfileParameterCapabilitiesChanged(Long profileId,
                ParamCapability[] caps, int uid, int pid) {
            String uuid = mSoundProfileTempIdMap.getValue(profileId);
            List<ParameterCapability> paramCaps = new ArrayList<>();
            for (ParamCapability cap: caps) {
                String name = MediaQualityUtils.getParameterName(cap.name);
                boolean isSupported = cap.isSupported;
                int type = cap.defaultValue == null ? 0 : cap.defaultValue.getTag() + 1;
                Bundle bundle = MediaQualityUtils.convertToCaps(type, cap.range);

                paramCaps.add(new ParameterCapability(name, isSupported, type, bundle));
            }
            notifySoundProfileHelper(ProfileModes.PARAMETER_CAPABILITY_CHANGED, uuid,
                    null, null, paramCaps , uid, pid);
        }

        private void notifySoundProfileHelper(int mode, String profileId,
                SoundProfile profile, Integer errorCode,
                List<ParameterCapability> paramCaps, int uid, int pid) {
            UserState userState = getOrCreateUserState(UserHandle.USER_SYSTEM);
            int n = userState.mSoundProfileCallbacks.beginBroadcast();

            for (int i = 0; i < n; ++i) {
                try {
                    ISoundProfileCallback callback = userState.mSoundProfileCallbacks
                            .getBroadcastItem(i);
                    Pair<Integer, Integer> pidUid = userState.mSoundProfileCallbackPidUidMap
                            .get(callback);

                    if (pidUid.first == pid && pidUid.second == uid) {
                        if (mode == ProfileModes.ADD) {
                            userState.mSoundProfileCallbacks.getBroadcastItem(i)
                                    .onSoundProfileAdded(profileId, profile);
                        } else if (mode == ProfileModes.UPDATE) {
                            userState.mSoundProfileCallbacks.getBroadcastItem(i)
                                    .onSoundProfileUpdated(profileId, profile);
                        } else if (mode == ProfileModes.REMOVE) {
                            userState.mSoundProfileCallbacks.getBroadcastItem(i)
                                    .onSoundProfileRemoved(profileId, profile);
                        } else if (mode == ProfileModes.ERROR) {
                            userState.mSoundProfileCallbacks.getBroadcastItem(i)
                                    .onError(profileId, errorCode);
                        } else if (mode == ProfileModes.PARAMETER_CAPABILITY_CHANGED) {
                            userState.mSoundProfileCallbacks.getBroadcastItem(i)
                                    .onParameterCapabilitiesChanged(profileId, paramCaps);
                        }
                    }
                } catch (RemoteException e) {
                    if (mode == ProfileModes.ADD) {
                        Slog.e(TAG, "Failed to report added sound profile to callback", e);
                    } else if (mode == ProfileModes.UPDATE) {
                        Slog.e(TAG, "Failed to report updated sound profile to callback", e);
                    } else if (mode == ProfileModes.REMOVE) {
                        Slog.e(TAG, "Failed to report removed sound profile to callback", e);
                    } else if (mode == ProfileModes.ERROR) {
                        Slog.e(TAG, "Failed to report sound profile error to callback", e);
                    } else if (mode == ProfileModes.PARAMETER_CAPABILITY_CHANGED) {
                        Slog.e(TAG, "Failed to report sound profile parameter capability change "
                                + "to callback", e);
                    }
                }
            }
            userState.mSoundProfileCallbacks.finishBroadcast();
        }

        private MqManagerNotifier() {

        }
    }

    private final class HalNotifier {

        private void notifyHalOnPictureProfileChange(Long dbId, PersistableBundle params) {
            // TODO: only notify HAL when the profile is active / being used
            if (mPpChangedListener != null) {
                Long currentHandle = mCurrentPictureHandleToOriginal.getKey(dbId);
                if (currentHandle != null) {
                    // this handle maps to another current profile, skip
                    return;
                }
                try {
                    Long idForHal = dbId;
                    Long originalHandle = mCurrentPictureHandleToOriginal.getValue(dbId);
                    if (originalHandle != null) {
                        // the original id is used in HAL because of status change
                        idForHal = originalHandle;
                    }
                    mPpChangedListener.onPictureProfileChanged(convertToHalPictureProfile(idForHal,
                            params));
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to notify HAL on picture profile change.", e);
                }
            }
        }

        private android.hardware.tv.mediaquality.PictureProfile convertToHalPictureProfile(Long id,
                PersistableBundle params) {
            PictureParameters pictureParameters = new PictureParameters();
            pictureParameters.pictureParameters =
                    MediaQualityUtils.convertPersistableBundleToPictureParameterList(
                            params);

            Parcel parcel = Parcel.obtain();
            if (params != null) {
                setVendorPictureParameters(pictureParameters, parcel, params);
            }

            android.hardware.tv.mediaquality.PictureProfile toReturn =
                    new android.hardware.tv.mediaquality.PictureProfile();
            toReturn.pictureProfileId = id;
            toReturn.parameters = pictureParameters;

            parcel.recycle();
            return toReturn;
        }

        private void notifyHalOnSoundProfileChange(Long dbId, PersistableBundle params) {
            // TODO: only notify HAL when the profile is active / being used
            if (mSpChangedListener != null) {
                try {
                    mSpChangedListener
                            .onSoundProfileChanged(convertToHalSoundProfile(dbId, params));
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to notify HAL on sound profile change.", e);
                }
            }
        }

        private android.hardware.tv.mediaquality.SoundProfile convertToHalSoundProfile(Long id,
                PersistableBundle params) {
            SoundParameters soundParameters = new SoundParameters();
            soundParameters.soundParameters =
                    MediaQualityUtils.convertPersistableBundleToSoundParameterList(params);

            Parcel parcel = Parcel.obtain();
            if (params != null) {
                setVendorSoundParameters(soundParameters, parcel, params);
            }

            android.hardware.tv.mediaquality.SoundProfile toReturn =
                    new android.hardware.tv.mediaquality.SoundProfile();
            toReturn.soundProfileId = id;
            toReturn.parameters = soundParameters;

            return toReturn;
        }

        private HalNotifier() {

        }
    }

    private final class AmbientBacklightCallbackRecord implements IBinder.DeathRecipient {
        final String mPackageName;
        final IAmbientBacklightCallback mCallback;

        AmbientBacklightCallbackRecord(@NonNull String pkgName,
                @NonNull IAmbientBacklightCallback cb) {
            mPackageName = pkgName;
            mCallback = cb;
            try {
                mCallback.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to link to death", e);
            }
        }

        void release() {
            try {
                mCallback.asBinder().unlinkToDeath(this, 0);
            } catch (NoSuchElementException e) {
                Slog.e(TAG, "Failed to unlink to death", e);
            }
        }

        @Override
        public void binderDied() {
            synchronized (mCallbackRecords) {
                mCallbackRecords.remove(mPackageName);
            }
        }
    }

    private final class PictureProfileAdjustmentListenerImpl extends
            IPictureProfileAdjustmentListener.Stub {

        @Override
        public void onPictureProfileAdjusted(
                android.hardware.tv.mediaquality.PictureProfile pictureProfile)
                throws RemoteException {
            Long dbId = pictureProfile.pictureProfileId;
            if (dbId != null) {
                updatePictureProfileFromHal(dbId,
                        MediaQualityUtils.convertPictureParameterListToPersistableBundle(
                                pictureProfile.parameters.pictureParameters));
            }
        }

        @Override
        public void onParamCapabilityChanged(long pictureProfileId, ParamCapability[] caps)
                throws RemoteException {
            mMqManagerNotifier.notifyOnPictureProfileParameterCapabilitiesChanged(
                    pictureProfileId, caps, Binder.getCallingUid(), Binder.getCallingPid());
        }

        @Override
        public void onVendorParamCapabilityChanged(long pictureProfileId,
                VendorParamCapability[] caps) throws RemoteException {
            // TODO
        }

        @Override
        public void requestPictureParameters(long pictureProfileId) throws RemoteException {
            PictureProfile profile = mMqDatabaseUtils.getPictureProfile(pictureProfileId);
            if (profile != null) {
                mHalNotifier.notifyHalOnPictureProfileChange(pictureProfileId,
                        profile.getParameters());
            }
        }

        @Override
        public void onStreamStatusChanged(long profileHandle, byte status)
                throws RemoteException {
            mHandler.post(() -> {
                synchronized (mPictureProfileLock) {
                    // get from map if exists
                    PictureProfile previous = mHandleToPictureProfile.get(profileHandle);
                    if (previous == null) {
                        // get from DB if not exists
                        previous = mMqDatabaseUtils.getPictureProfile(profileHandle);
                        if (previous == null) {
                            return;
                        }
                    }
                    String[] arr = splitNameAndStatus(previous.getName());
                    String profileName = arr[0];
                    String profileStatus = arr[1];
                    if (status == StreamStatus.HDR10) {
                        if (isHdr(profileStatus)) {
                            // already HDR
                            return;
                        }
                        if (isSdr(profileStatus)) {
                            // SDR to HDR
                            String selection = BaseParameters.PARAMETER_TYPE + " = ? AND "
                                    + BaseParameters.PARAMETER_PACKAGE + " = ? AND "
                                    + BaseParameters.PARAMETER_NAME + " = ?";
                            String[] selectionArguments = {
                                    Integer.toString(previous.getProfileType()),
                                    previous.getPackageName(),
                                    profileName + "/" + PictureProfile.STATUS_HDR
                            };
                            List<PictureProfile> list =
                                    mMqDatabaseUtils.getPictureProfilesBasedOnConditions(
                                            MediaQualityUtils.getMediaProfileColumns(true),
                                            selection,
                                            selectionArguments);
                            if (list.isEmpty()) {
                                // HDR profile not found
                                return;
                            }
                            PictureProfile current = list.get(0);
                            mHandleToPictureProfile.put(profileHandle, current);
                            mCurrentPictureHandleToOriginal.put(
                                    current.getHandle().getId(), profileHandle);

                            mHalNotifier.notifyHalOnPictureProfileChange(profileHandle,
                                    current.getParameters());

                        }
                    } else if (status == StreamStatus.SDR) {
                        if (isSdr(profileStatus)) {
                            // already SDR
                            return;
                        }
                        if (isHdr(profileStatus)) {
                            // HDR to SDR
                            String selection = BaseParameters.PARAMETER_TYPE + " = ? AND "
                                    + BaseParameters.PARAMETER_PACKAGE + " = ? AND ("
                                    + BaseParameters.PARAMETER_NAME + " = ? OR "
                                    + BaseParameters.PARAMETER_NAME + " = ?)";
                            String[] selectionArguments = {
                                    Integer.toString(previous.getProfileType()),
                                    previous.getPackageName(),
                                    profileName,
                                    profileName + "/" + PictureProfile.STATUS_SDR
                            };
                            List<PictureProfile> list =
                                    mMqDatabaseUtils.getPictureProfilesBasedOnConditions(
                                            MediaQualityUtils.getMediaProfileColumns(true),
                                            selection,
                                            selectionArguments);
                            if (list.isEmpty()) {
                                // SDR profile not found
                                return;
                            }
                            PictureProfile current = list.get(0);
                            mHandleToPictureProfile.put(profileHandle, current);
                            mCurrentPictureHandleToOriginal.put(
                                    current.getHandle().getId(), profileHandle);

                            mHalNotifier.notifyHalOnPictureProfileChange(profileHandle,
                                    current.getParameters());
                        }
                    }
                }
            });

        }

        @NonNull
        private String[] splitNameAndStatus(@NonNull String nameAndStatus) {
            int index = nameAndStatus.lastIndexOf('/');
            if (index == -1 || index == nameAndStatus.length() - 1) {
                // no status in the original name
                return new String[] {nameAndStatus, ""};
            }
            return new String[] {
                    nameAndStatus.substring(0, index),
                    nameAndStatus.substring(index + 1)
            };

        }

        private boolean isSdr(@NonNull String profileStatus) {
            return profileStatus.equals(PictureProfile.STATUS_SDR)
                    || profileStatus.isEmpty();
        }

        private boolean isHdr(@NonNull String profileStatus) {
            return profileStatus.equals(PictureProfile.STATUS_HDR);
        }

        @Override
        public int getInterfaceVersion() throws RemoteException {
            return 0;
        }

        @Override
        public String getInterfaceHash() throws RemoteException {
            return null;
        }

        private PictureProfileAdjustmentListenerImpl() {

        }
    }

    private final class SoundProfileAdjustmentListenerImpl extends
            ISoundProfileAdjustmentListener.Stub {

        @Override
        public void onSoundProfileAdjusted(
                android.hardware.tv.mediaquality.SoundProfile soundProfile) throws RemoteException {
            Long dbId = soundProfile.soundProfileId;
            if (dbId != null) {
                updateSoundProfileFromHal(dbId,
                        MediaQualityUtils.convertSoundParameterListToPersistableBundle(
                                soundProfile.parameters.soundParameters));
            }
        }

        @Override
        public void onParamCapabilityChanged(long soundProfileId, ParamCapability[] caps)
                throws RemoteException {
            mMqManagerNotifier.notifyOnSoundProfileParameterCapabilitiesChanged(
                    soundProfileId, caps, Binder.getCallingUid(), Binder.getCallingPid());
        }

        @Override
        public void onVendorParamCapabilityChanged(long pictureProfileId,
                VendorParamCapability[] caps) throws RemoteException {
            // TODO
        }

        @Override
        public void requestSoundParameters(long soundProfileId) throws RemoteException {
            SoundProfile profile = mMqDatabaseUtils.getSoundProfile(soundProfileId);
            if (profile != null) {
                mHalNotifier.notifyHalOnSoundProfileChange(soundProfileId,
                        profile.getParameters());
            }
        }

        @Override
        public int getInterfaceVersion() throws RemoteException {
            return 0;
        }

        @Override
        public String getInterfaceHash() throws RemoteException {
            return null;
        }

        private SoundProfileAdjustmentListenerImpl() {

        }
    }

    private final class HalAmbientBacklightCallback
            extends android.hardware.tv.mediaquality.IMediaQualityCallback.Stub {
        private final Object mLock = new Object();
        private String mAmbientBacklightClientPackageName;

        void setAmbientBacklightClientPackageName(@NonNull String packageName) {
            synchronized (mLock) {
                if (TextUtils.equals(mAmbientBacklightClientPackageName, packageName)) {
                    return;
                }
                handleAmbientBacklightInterrupted();
                mAmbientBacklightClientPackageName = packageName;
            }
        }

        void handleAmbientBacklightInterrupted() {
            synchronized (mCallbackRecords) {
                if (mAmbientBacklightClientPackageName == null) {
                    Slog.e(TAG, "Invalid package name in interrupted event");
                    return;
                }
                AmbientBacklightCallbackRecord record = mCallbackRecords.get(
                        mAmbientBacklightClientPackageName);
                if (record == null) {
                    Slog.e(TAG, "Callback record not found for ambient backlight");
                    return;
                }
                AmbientBacklightEvent event =
                        new AmbientBacklightEvent(
                                AMBIENT_BACKLIGHT_EVENT_INTERRUPTED, null);
                try {
                    record.mCallback.onAmbientBacklightEvent(event);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Deliver ambient backlight interrupted event failed", e);
                }
            }
        }

        void handleAmbientBacklightEnabled(boolean enabled) {
            AmbientBacklightEvent event =
                    new AmbientBacklightEvent(
                            enabled ? AMBIENT_BACKLIGHT_EVENT_ENABLED :
                                    AMBIENT_BACKLIGHT_EVENT_DISABLED, null);
            synchronized (mCallbackRecords) {
                for (AmbientBacklightCallbackRecord record : mCallbackRecords.values()) {
                    try {
                        record.mCallback.onAmbientBacklightEvent(event);
                    } catch (RemoteException e) {
                        Slog.e(TAG, "Deliver ambient backlight enabled event failed", e);
                    }
                }
            }
        }

        void handleAmbientBacklightMetadataEvent(
                @NonNull android.hardware.tv.mediaquality.AmbientBacklightMetadata
                        halMetadata) {
            String halPackageName = mContext.getPackageManager()
                                    .getNameForUid(halMetadata.settings.uid);
            if (!TextUtils.equals(mAmbientBacklightClientPackageName, halPackageName)) {
                Slog.e(TAG, "Invalid package name in metadata event");
                return;
            }

            AmbientBacklightColorFormat[] zonesColorsUnion = halMetadata.zonesColors;
            int[] zonesColorsInt = new int[zonesColorsUnion.length];

            for (int i = 0; i < zonesColorsUnion.length; i++) {
                zonesColorsInt[i] = zonesColorsUnion[i].RGB888;
            }

            AmbientBacklightMetadata metadata =
                    new AmbientBacklightMetadata(
                            halPackageName,
                            halMetadata.compressAlgorithm,
                            halMetadata.settings.source,
                            halMetadata.settings.colorFormat,
                            halMetadata.settings.hZonesNumber,
                            halMetadata.settings.vZonesNumber,
                            zonesColorsInt);
            AmbientBacklightEvent event =
                    new AmbientBacklightEvent(
                            AMBIENT_BACKLIGHT_EVENT_METADATA_AVAILABLE, metadata);

            synchronized (mCallbackRecords) {
                AmbientBacklightCallbackRecord record = mCallbackRecords
                                                .get(halPackageName);
                if (record == null) {
                    Slog.e(TAG, "Callback record not found for ambient backlight metadata");
                    return;
                }

                try {
                    record.mCallback.onAmbientBacklightEvent(event);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Deliver ambient backlight metadata event failed", e);
                }
            }
        }

        @Override
        public void notifyAmbientBacklightEvent(
                android.hardware.tv.mediaquality.AmbientBacklightEvent halEvent) {
            synchronized (mLock) {
                if (halEvent.getTag() == android.hardware.tv.mediaquality
                                .AmbientBacklightEvent.Tag.enabled) {
                    boolean enabled = halEvent.getEnabled();
                    if (enabled) {
                        handleAmbientBacklightEnabled(true);
                    } else {
                        handleAmbientBacklightEnabled(false);
                    }
                } else if (halEvent.getTag() == android.hardware.tv.mediaquality
                                    .AmbientBacklightEvent.Tag.metadata) {
                    handleAmbientBacklightMetadataEvent(halEvent.getMetadata());
                } else {
                    Slog.e(TAG, "Invalid event type in ambient backlight event");
                }
            }
        }

        @Override
        public synchronized String getInterfaceHash() throws android.os.RemoteException {
            return android.hardware.tv.mediaquality.IMediaQualityCallback.Stub.HASH;
        }

        @Override
        public int getInterfaceVersion() throws android.os.RemoteException {
            return android.hardware.tv.mediaquality.IMediaQualityCallback.Stub.VERSION;
        }
    }

    private void setVendorPictureParameters(
            PictureParameters pictureParameters,
            Parcel parcel,
            PersistableBundle vendorPictureParameters) {
        vendorPictureParameters.writeToParcel(parcel, 0);
        byte[] vendorBundleToByteArray = parcel.marshall();
        DefaultExtension defaultExtension = new DefaultExtension();
        defaultExtension.bytes = Arrays.copyOf(
                vendorBundleToByteArray, vendorBundleToByteArray.length);
        pictureParameters.vendorPictureParameters.setParcelable(defaultExtension);
    }

    private void setVendorSoundParameters(
            SoundParameters soundParameters,
            Parcel parcel,
            PersistableBundle vendorSoundParameters) {
        vendorSoundParameters.writeToParcel(parcel, 0);
        byte[] vendorBundleToByteArray = parcel.marshall();
        DefaultExtension defaultExtension = new DefaultExtension();
        defaultExtension.bytes = Arrays.copyOf(
                vendorBundleToByteArray, vendorBundleToByteArray.length);
        soundParameters.vendorSoundParameters.setParcelable(defaultExtension);
    }

    private boolean isPackageDefaultPictureProfile(PictureProfile pp) {
        return pp != null && pp.getProfileType() == PictureProfile.TYPE_SYSTEM &&
               pp.getName().equals(PictureProfile.NAME_DEFAULT);
    }
}
