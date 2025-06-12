/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.hardware.radio;

import android.Manifest;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.graphics.Bitmap;
import android.os.Handler;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * RadioTuner interface provides methods to control a radio tuner on the device: selecting and
 * configuring the active band, muting/unmuting, scanning and tuning, etc...
 *
 * Obtain a RadioTuner interface by calling {@link RadioManager#openTuner(int,
 * RadioManager.BandConfig, boolean, RadioTuner.Callback, Handler)}.
 * @hide
 */
@SystemApi
public abstract class RadioTuner {

    /** Scanning direction UP for {@link #step(int, boolean)}, {@link #scan(int, boolean)} */
    public static final int DIRECTION_UP      = 0;

    /** Scanning directions DOWN for {@link #step(int, boolean)}, {@link #scan(int, boolean)} */
    public static final int DIRECTION_DOWN    = 1;

    /**
     * Close the tuner interface.
     *
     * <p>The {@link Callback} callback will not be called anymore and associated resources will be
     * released. Must be called when the tuner is not needed to make hardware resources available
     * to others.
     * */
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    public abstract void close();

    /**
     * Set the active band configuration for this module.
     *
     * <p>Must be a valid configuration obtained via buildConfig() from a valid BandDescriptor
     * listed in the ModuleProperties of the module with the specified ID.
     *
     * @param config The desired band configuration (FmBandConfig or AmBandConfig).
     * @return
     * <ul>
     *  <li>{@link RadioManager#STATUS_OK} in case of success, </li>
     *  <li>{@link RadioManager#STATUS_ERROR} in case of unspecified error, </li>
     *  <li>{@link RadioManager#STATUS_NO_INIT} if the native service cannot be reached, </li>
     *  <li>{@link RadioManager#STATUS_BAD_VALUE} if parameters are invalid, </li>
     *  <li>{@link RadioManager#STATUS_INVALID_OPERATION} if the call is out of sequence, </li>
     *  <li>{@link RadioManager#STATUS_DEAD_OBJECT} if the binder transaction to the native
     *  service fails, </li>
     * </ul>
     * @deprecated Only applicable for HAL 1.x.
     */
    @Deprecated
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    @RadioManager.RadioStatusType
    public abstract int setConfiguration(RadioManager.BandConfig config);

    /**
     * Get current configuration.
     *
     * @param config a BandConfig array of lengh 1 where the configuration is returned.
     * @return
     * <ul>
     *  <li>{@link RadioManager#STATUS_OK} in case of success, </li>
     *  <li>{@link RadioManager#STATUS_ERROR} in case of unspecified error, </li>
     *  <li>{@link RadioManager#STATUS_NO_INIT} if the native service cannot be reached, </li>
     *  <li>{@link RadioManager#STATUS_BAD_VALUE} if parameters are invalid, </li>
     *  <li>{@link RadioManager#STATUS_INVALID_OPERATION} if the call is out of sequence, </li>
     *  <li>{@link RadioManager#STATUS_DEAD_OBJECT} if the binder transaction to the native
     *  service fails, </li>
     * </ul>
     *
     * @deprecated Only applicable for HAL 1.x.
     */
    @Deprecated
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    @RadioManager.RadioStatusType
    public abstract int getConfiguration(RadioManager.BandConfig[] config);


    /**
     * Set mute state.
     *
     * <p>When muted, the radio tuner audio source is not available for playback on
     * any audio device. when unmuted, the radio tuner audio source is output as a media source
     * and renderd over the audio device selected for media use case.
     * The radio tuner audio source is muted by default when the tuner is first attached.
     * Only effective if the tuner is attached with audio enabled.
     *
     * @param mute the requested mute state.
     * @return
     * <ul>
     *  <li>{@link RadioManager#STATUS_OK} in case of success, </li>
     *  <li>{@link RadioManager#STATUS_ERROR} in case of unspecified error, </li>
     *  <li>{@link RadioManager#STATUS_NO_INIT} if the native service cannot be reached, </li>
     *  <li>{@link RadioManager#STATUS_INVALID_OPERATION} if the call is out of sequence, </li>
     *  <li>{@link RadioManager#STATUS_DEAD_OBJECT} if the binder transaction to the native
     *  service fails, </li>
     * </ul>
     */
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    @RadioManager.RadioStatusType
    public abstract int setMute(boolean mute);

    /**
     * Get mute state.
     *
     * @return {@code true} if the radio tuner audio source is muted or a problem occured
     * retrieving the mute state, {@code false} otherwise.
     */
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    public abstract boolean getMute();

    /**
     * Step up or down by one channel spacing.
     *
     * <p>The operation is asynchronous and {@link Callback#onProgramInfoChanged}
     * will be called when step completes or {@link Callback#onTuneFailed}
     * when timeout or canceled.
     *
     * <p>When this operation is called by users other than current user or system user, it is
     * ignored silently.
     *
     * @param direction {@link #DIRECTION_UP} or {@link #DIRECTION_DOWN}.
     * @param skipSubChannel indicates to skip sub channels when the configuration currently
     * selected supports sub channel (e.g HD Radio). N/A otherwise.
     * @return
     * <ul>
     *  <li>{@link RadioManager#STATUS_OK} in case of success, </li>
     *  <li>{@link RadioManager#STATUS_ERROR} in case of unspecified error, </li>
     *  <li>{@link RadioManager#STATUS_NO_INIT} if the native service cannot be reached, </li>
     *  <li>{@link RadioManager#STATUS_BAD_VALUE} if parameters are invalid, </li>
     *  <li>{@link RadioManager#STATUS_INVALID_OPERATION} if the call is out of sequence, </li>
     *  <li>{@link RadioManager#STATUS_DEAD_OBJECT} if the binder transaction to the native
     *  service fails, </li>
     * </ul>
     */
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    @RadioManager.RadioStatusType
    public abstract int step(int direction, boolean skipSubChannel);

    /**
     * Scan up or down to next valid station.
     *
     * <p>The operation is asynchronous and {@link Callback#onProgramInfoChanged}
     * will be called when scan completes or {@link Callback#onTuneFailed}
     * when timeout or canceled.
     *
     * <p>When this operation is called by users other than current user or system user, it is
     * ignored silently.
     *
     * @param direction {@link #DIRECTION_UP} or {@link #DIRECTION_DOWN}.
     * @param skipSubChannel indicates to skip sub channels when the configuration currently
     * selected supports sub channel (e.g HD Radio). N/A otherwise.
     * @return
     * <ul>
     *  <li>{@link RadioManager#STATUS_OK} in case of success, </li>
     *  <li>{@link RadioManager#STATUS_ERROR} in case of unspecified error, </li>
     *  <li>{@link RadioManager#STATUS_NO_INIT} if the native service cannot be reached, </li>
     *  <li>{@link RadioManager#STATUS_BAD_VALUE} if parameters are invalid, </li>
     *  <li>{@link RadioManager#STATUS_INVALID_OPERATION} if the call is out of sequence, </li>
     *  <li>{@link RadioManager#STATUS_DEAD_OBJECT} if the binder transaction to the native
     *  service fails, </li>
     * </ul>
     * @deprecated Use {@link #seek(int, boolean)} instead.
     */
    @Deprecated
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    @RadioManager.RadioStatusType
    public abstract int scan(int direction, boolean skipSubChannel);

    /**
     * Seek up or down to next valid station.
     *
     * <p>The operation is asynchronous and {@link Callback#onProgramInfoChanged}
     * will be called when seek completes or {@link Callback#onTuneFailed}
     * when timeout or canceled.
     *
     * <p>When this operation is called by users other than current user or system user, it is
     * ignore silently.
     *
     * @param direction {@link #DIRECTION_UP} or {@link #DIRECTION_DOWN}.
     * @param skipSubChannel indicates to skip sub channels when the configuration currently
     * selected supports sub channel (e.g HD Radio). N/A otherwise.
     * @return
     * <ul>
     *  <li>{@link RadioManager#STATUS_OK} in case of success, </li>
     *  <li>{@link RadioManager#STATUS_ERROR} in case of unspecified error, </li>
     *  <li>{@link RadioManager#STATUS_NO_INIT} if the native service cannot be reached, </li>
     *  <li>{@link RadioManager#STATUS_BAD_VALUE} if parameters are invalid, </li>
     *  <li>{@link RadioManager#STATUS_INVALID_OPERATION} if the call is out of sequence, </li>
     *  <li>{@link RadioManager#STATUS_DEAD_OBJECT} if the binder transaction to the native
     *  service fails, </li>
     * </ul>
     */
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    @RadioManager.RadioStatusType
    public int seek(int direction, boolean skipSubChannel) {
        throw new UnsupportedOperationException("Seeking is not supported");
    }

    /**
     * Tune to a specific frequency.
     *
     * <p>The operation is asynchronous and {@link Callback#onProgramInfoChanged}
     * will be called when tune completes or {@link Callback#onTuneFailed}
     * when timeout or canceled.
     *
     * <p>When this operation is called by users other than current user or system user, it is
     * ignored silently.
     *
     * @param channel the specific channel or frequency to tune to.
     * @param subChannel the specific sub-channel to tune to. N/A if the selected configuration
     * does not support cub channels.
     * @return
     * <ul>
     *  <li>{@link RadioManager#STATUS_OK} in case of success, </li>
     *  <li>{@link RadioManager#STATUS_ERROR} in case of unspecified error, </li>
     *  <li>{@link RadioManager#STATUS_NO_INIT} if the native service cannot be reached, </li>
     *  <li>{@link RadioManager#STATUS_BAD_VALUE} if parameters are invalid, </li>
     *  <li>{@link RadioManager#STATUS_INVALID_OPERATION} if the call is out of sequence, </li>
     *  <li>{@link RadioManager#STATUS_DEAD_OBJECT} if the binder transaction to the native
     *  service fails, </li>
     * </ul>
     * @deprecated Use {@link #tune(ProgramSelector)} instead.
     */
    @Deprecated
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    @RadioManager.RadioStatusType
    public abstract int tune(int channel, int subChannel);

    /**
     * Tune to a program.
     *
     * <p>The operation is asynchronous and {@link Callback#onProgramInfoChanged}
     * will be called when tune completes or {@link Callback#onTuneFailed}
     * when timeout or canceled.
     *
     * <p>When this operation is called by users other than current user or system user, it is
     * ignored silently.
     *
     * @throws IllegalArgumentException if the provided selector is invalid
     */
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    public abstract void tune(@NonNull ProgramSelector selector);

    /**
     * Cancel a pending scan or tune operation.
     *
     * <p>If an operation is pending, {@link Callback#onTuneFailed} will be called with
     * {@link #ERROR_CANCELLED}.
     *
     * <p>When this operation is called by users other than current user or system
     * user, it is ignored silently.
     *
     * @return
     * <ul>
     *  <li>{@link RadioManager#STATUS_OK} in case of success, </li>
     *  <li>{@link RadioManager#STATUS_ERROR} in case of unspecified error, </li>
     *  <li>{@link RadioManager#STATUS_NO_INIT} if the native service cannot be reached, </li>
     *  <li>{@link RadioManager#STATUS_BAD_VALUE} if parameters are invalid, </li>
     *  <li>{@link RadioManager#STATUS_INVALID_OPERATION} if the call is out of sequence, </li>
     *  <li>{@link RadioManager#STATUS_DEAD_OBJECT} if the binder transaction to the native
     *  service fails, </li>
     * </ul>
     */
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    @RadioManager.RadioStatusType
    public abstract int cancel();

    /**
     * Cancels traffic or emergency announcement.
     *
     * <p>If there was no announcement to cancel, no action is taken.
     *
     * <p>There is a race condition between calling cancelAnnouncement and the actual announcement
     * being finished, so onTrafficAnnouncement / onEmergencyAnnouncement callback should be
     * tracked with proper locking.
     * @deprecated Only applicable for HAL 1.x.
     */
    @Deprecated
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    public abstract void cancelAnnouncement();

    /**
     * Get current station information.
     *
     * @param info a ProgramInfo array of lengh 1 where the information is returned.
     * @return
     * <ul>
     *  <li>{@link RadioManager#STATUS_OK} in case of success, </li>
     *  <li>{@link RadioManager#STATUS_ERROR} in case of unspecified error, </li>
     *  <li>{@link RadioManager#STATUS_NO_INIT} if the native service cannot be reached, </li>
     *  <li>{@link RadioManager#STATUS_BAD_VALUE} if parameters are invalid, </li>
     *  <li>{@link RadioManager#STATUS_INVALID_OPERATION} if the call is out of sequence, </li>
     *  <li>{@link RadioManager#STATUS_DEAD_OBJECT} if the binder transaction to the native
     *  service fails, </li>
     * </ul>
     * @deprecated Use {@link Callback#onProgramInfoChanged(RadioManager.ProgramInfo)} callback
     * instead.
     */
    @Deprecated
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    @RadioManager.RadioStatusType
    public abstract int getProgramInformation(RadioManager.ProgramInfo[] info);

    /**
     * Retrieves a {@link Bitmap} for the given image ID or throw {@link IllegalArgumentException},
     * if the image was missing from the tuner.
     *
     * <p>This involves doing a call to the tuner, so the bitmap should be cached
     * on the application side.
     *
     * <p>If the method throws {@link IllegalArgumentException} for non-zero ID, it
     * means the image was updated on the tuner side. There is a race condition between
     * fetching image for an old ID and tuner updating the image (and cleaning up the
     * old image). In such case, a new ProgramInfo with updated image id will
     * be sent with a {@link Callback#onProgramInfoChanged(RadioManager.ProgramInfo)}
     * callback.
     *
     * @param id The image identifier, retrieved with
     *           {@link RadioMetadata#getBitmapId(String)}.
     * @return A {@link Bitmap} for the given image ID.
     * @throws IllegalArgumentException if id is 0 or the referenced image id no longer exists.
     */
    @FlaggedApi(Flags.FLAG_HD_RADIO_IMPROVED)
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    public @NonNull Bitmap getMetadataImage(int id) {
        throw new UnsupportedOperationException(
                "Getting metadata image must be implemented in child classes");
    }
    /**
     * Initiates a background scan to update internally cached program list.
     *
     * <p>It may not be necessary to initiate the scan explicitly - the scan MAY be performed on
     * boot.
     *
     * <p>The operation is asynchronous and {@link Callback} backgroundScanComplete or onError will
     * be called if the return value of this call was {@code true}. As result of this call
     * programListChanged may be triggered (if the scanned list differs).
     *
     * @return {@code true} if the scan was properly scheduled, {@code false} if the scan feature
     * is unavailable; ie. temporarily due to ongoing foreground playback in single-tuner device
     * or permanently if the feature is not supported
     * (see ModuleProperties#isBackgroundScanningSupported()).
     * @deprecated Only applicable for HAL 1.x.
     */
    @Deprecated
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    public abstract boolean startBackgroundScan();

    /**
     * Get the list of discovered radio stations.
     *
     * <p>To get the full list, set filter to null or empty map.
     * Keys must be prefixed with unique vendor Java-style namespace,
     * eg. 'com.somecompany.parameter1'.
     *
     * @param vendorFilter vendor-specific selector for radio stations.
     * @return a list of radio stations.
     * @throws IllegalStateException if the scan is in progress or has not been started,
     *         startBackgroundScan() call may fix it.
     * @throws IllegalArgumentException if the vendorFilter argument is not valid.
     * @deprecated Use {@link #getDynamicProgramList(ProgramList.Filter)} instead.
     */
    @Deprecated
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    public abstract @NonNull List<RadioManager.ProgramInfo>
            getProgramList(@Nullable Map<String, String> vendorFilter);

    /**
     * Get the dynamic list of discovered radio stations.
     *
     * <p>The list object is updated asynchronously; to get the updates register
     * with {@link ProgramList#registerListCallback(ProgramList.ListCallback)}
     * or {@link ProgramList#registerListCallback(Executor, ProgramList.ListCallback)}.
     *
     * <p>When the returned object is no longer used, it must be closed.
     *
     * @param filter filter for the list, or null to get the full list.
     * @return the dynamic program list object, close it after use
     *         or {@code null} if program list is not supported by the tuner
     */
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    public @Nullable ProgramList getDynamicProgramList(@Nullable ProgramList.Filter filter) {
        return null;
    }

    /**
     * Checks, if the analog playback is forced, see setAnalogForced.
     *
     * @throws IllegalStateException if the switch is not supported at current
     *         configuration.
     * @return {@code true} if analog is forced, {@code false} otherwise.
     * @deprecated Use {@link #isConfigFlagSet(int)} instead.
     */
    @Deprecated
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    public abstract boolean isAnalogForced();

    /**
     * Forces the analog playback for the supporting radio technology.
     *
     * <p>User may disable digital playback for FM HD Radio or hybrid FM/DAB with
     * this option. This is purely user choice, ie. does not reflect digital-
     * analog handover managed from the HAL implementation side.
     *
     * <p>Some radio technologies may not support this, ie. DAB.
     *
     * @param isForced {@code true} to force analog, {@code false} for a default behaviour.
     * @throws IllegalStateException if the switch is not supported at current
     *         configuration.
     * @deprecated Use {@link #setConfigFlag(int, boolean)}  instead.
     */
    @Deprecated
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    public abstract void setAnalogForced(boolean isForced);

    /**
     * Checks, if a given config flag is supported
     *
     * @param flag Flag to check.
     * @return True, if the flag is supported.
     */
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    public boolean isConfigFlagSupported(@RadioManager.ConfigFlag int flag) {
        return false;
    }

    /**
     * Fetches the current setting of a given config flag.
     *
     * <p>The success/failure result is consistent with isConfigFlagSupported.
     *
     * @param flag Flag to fetch.
     * @return The current value of the flag.
     * @throws IllegalStateException if the flag is not applicable right now.
     * @throws UnsupportedOperationException if the flag is not supported at all.
     */
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    public boolean isConfigFlagSet(@RadioManager.ConfigFlag int flag) {
        throw new UnsupportedOperationException("isConfigFlagSet is not supported");
    }

    /**
     * Sets the config flag.
     *
     * <p>The success/failure result is consistent with isConfigFlagSupported.
     *
     * <p>When this operation is called by users other than current user or system user,
     * it is ignored silently.
     *
     * @param flag Flag to set.
     * @param value The new value of a given flag.
     * @throws IllegalStateException if the flag is not applicable right now.
     * @throws UnsupportedOperationException if the flag is not supported at all.
     */
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    public void setConfigFlag(@RadioManager.ConfigFlag int flag, boolean value) {
        throw new UnsupportedOperationException("Setting config flag is not supported");
    }

    /**
     * Generic method for setting vendor-specific parameter values.
     * The framework does not interpret the parameters, they are passed
     * in an opaque manner between a vendor application and HAL.
     *
     * <p>Framework does not make any assumptions on the keys or values, other than
     * ones stated in VendorKeyValue documentation (a requirement of key
     * prefixes).
     * See VendorKeyValue at hardware/interfaces/broadcastradio/2.0/types.hal for
     * HIDL 2.0 HAL or
     * hardware/interfaces/broadcastradio/aidl/android/hardware/broadcastradio/VendorKeyValue.aidl
     * for AIDL HAL.
     *
     * <p>For each pair in the result map, the key will be one of the keys
     * contained in the input (possibly with wildcards expanded), and the value
     * will be a vendor-specific result status (such as "OK" or an error code).
     * The implementation may choose to return an empty map, or only return
     * a status for a subset of the provided inputs, at its discretion.
     *
     * <p>Application and HAL must not use keys with unknown prefix. In particular,
     * it must not place a key-value pair in results vector for unknown key from
     * parameters vector - instead, an unknown key should simply be ignored.
     * In other words, results vector may contain a subset of parameter keys
     * (however, the framework doesn't enforce a strict subset - the only
     * formal requirement is vendor domain prefix for keys).
     *
     * <p>When this operation is called by users other than current user or system user,
     * it is ignored silently.
     *
     * @param parameters Vendor-specific key-value pairs.
     * @return Operation completion status for parameters being set.
     */
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    public @NonNull Map<String, String>
            setParameters(@NonNull Map<String, String> parameters) {
        throw new UnsupportedOperationException("Setting parameters is not supported");
    }

    /**
     * Generic method for retrieving vendor-specific parameter values.
     * The framework does not interpret the parameters, they are passed
     * in an opaque manner between a vendor application and HAL.
     *
     * <p>Framework does not cache set/get requests, so it's possible for
     * getParameter to return a different value than previous setParameter call.
     *
     * <p>The syntax and semantics of keys are up to the vendor (as long as prefix
     * rules are obeyed). For instance, vendors may include some form of
     * wildcard support. In such case, result vector may be of different size
     * than requested keys vector. However, wildcards are not recognized by
     * framework and they are passed as-is to the HAL implementation.
     *
     * <p>Unknown keys must be ignored and not placed into results vector.
     *
     * @param keys Parameter keys to fetch.
     * @return Vendor-specific key-value pairs.
     */
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    public @NonNull Map<String, String>
            getParameters(@NonNull List<String> keys) {
        throw new UnsupportedOperationException("Getting parameters is not supported");
    }

    /**
     * Get current antenna connection state for current configuration.
     * Only valid if a configuration has been applied.
     * @return {@code true} if the antenna is connected, {@code false} otherwise.
     *
     * @deprecated Use {@link Callback#onAntennaState(boolean)} callback instead
     */
    @Deprecated
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    public abstract boolean isAntennaConnected();

    /**
     * Indicates if this client actually controls the tuner.
     *
     * <p>Control is always granted after
     * {@link RadioManager#openTuner(int,
     * RadioManager.BandConfig, boolean, Callback, Handler)}
     * returns a non null tuner interface.
     * Control is lost when another client opens an interface on the same tuner.
     * When this happens, {@link Callback#onControlChanged(boolean)} is received.
     * The client can either wait for control to be returned (which is indicated by the same
     * callback) or close and reopen the tuner interface.
     *
     * @return {@code true} if this interface controls the tuner,
     * {@code false} otherwise or if a problem occured retrieving the state.
     */
    @RequiresPermission(Manifest.permission.ACCESS_BROADCAST_RADIO)
    public abstract boolean hasControl();

    /** Indicates a failure of radio IC or driver.
     *
     * <p>The application must close and re open the tuner
     *
     * @deprecated See {@link RadioTuner.Callback#onError(int)} callback.
     */
    @Deprecated
    public static final int ERROR_HARDWARE_FAILURE = 0;
    /** Indicates a failure of the radio service.
     *
     * <p>The application must close and re open the tuner
     * @deprecated See {@link RadioTuner.Callback#onError(int)} callback.
     */
    @Deprecated
    public static final  int ERROR_SERVER_DIED = 1;
    /** A pending seek or tune operation was cancelled
     * @deprecated See {@link RadioTuner.Callback#onError(int)} callback.
     */
    @Deprecated
    public static final  int ERROR_CANCELLED = 2;
    /** A pending seek or tune operation timed out
     * @deprecated See {@link RadioTuner.Callback#onError(int)} callback.
     */
    @Deprecated
    public static final  int ERROR_SCAN_TIMEOUT = 3;
    /** The requested configuration could not be applied
     * @deprecated See {@link RadioTuner.Callback#onError(int)} callback.
     */
    @Deprecated
    public static final  int ERROR_CONFIG = 4;
    /** Background scan was interrupted due to hardware becoming temporarily unavailable.
     * @deprecated See {@link RadioTuner.Callback#onError(int)} callback.
     */
    @Deprecated
    public static final int ERROR_BACKGROUND_SCAN_UNAVAILABLE = 5;
    /** Background scan failed due to other error, ie. HW failure.
     * @deprecated See {@link RadioTuner.Callback#onError(int)} callback.
     */
    @Deprecated
    public static final int ERROR_BACKGROUND_SCAN_FAILED = 6;
    /** Result when a tune, seek, or step operation runs without error.
     */
    public static final int TUNER_RESULT_OK = 0;
    /** Result when internal error occurs in HAL.
     * See {@link RadioTuner.Callback#onTuneFailed(int, ProgramSelector)} callback.
     */
    public static final int TUNER_RESULT_INTERNAL_ERROR = 1;
    /** Result used when the input argument for the method is invalid.
     * See {@link RadioTuner.Callback#onTuneFailed(int, ProgramSelector)} callback.
     */
    public static final int TUNER_RESULT_INVALID_ARGUMENTS = 2;
    /** Result when HAL is of invalid state.
     * See {@link RadioTuner.Callback#onTuneFailed(int, ProgramSelector)} callback.
     */
    public static final int TUNER_RESULT_INVALID_STATE = 3;
    /** Result when the operation is not supported.
     * See {@link RadioTuner.Callback#onTuneFailed(int, ProgramSelector)} callback.
     */
    public static final int TUNER_RESULT_NOT_SUPPORTED = 4;
    /** Result when a tune, seek, or step operation is timeout
     * See {@link RadioTuner.Callback#onTuneFailed(int, ProgramSelector)} callback.
     */
    public static final int TUNER_RESULT_TIMEOUT = 5;
    /** Result when a tune, seek, or step operation is canceled before processed.
     * See {@link RadioTuner.Callback#onTuneFailed(int, ProgramSelector)} callback.
     */
    public static final int TUNER_RESULT_CANCELED = 6;
    /** Result when a tune, seek, or step operation fails due to unknown error.
     * See {@link RadioTuner.Callback#onTuneFailed(int, ProgramSelector)} callback.
     */
    public static final int TUNER_RESULT_UNKNOWN_ERROR = 7;

    /**
     *  Tuning operation result types
     *
     * @hide
     */
    @IntDef(prefix = { "TUNER_RESULT_" }, value = {
            TUNER_RESULT_OK,
            TUNER_RESULT_INTERNAL_ERROR,
            TUNER_RESULT_INVALID_ARGUMENTS,
            TUNER_RESULT_INVALID_STATE,
            TUNER_RESULT_NOT_SUPPORTED,
            TUNER_RESULT_TIMEOUT,
            TUNER_RESULT_CANCELED,
            TUNER_RESULT_UNKNOWN_ERROR,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TunerResultType{}

    /**
     * Callback provided by the client application when opening a {@link RadioTuner}
     * to receive asynchronous operation results, updates and error notifications.
     */
    public static abstract class Callback {
        /**
         * onError() is called when an error occured while performing an asynchronous
         * operation of when the hardware or system service experiences a problem.
         * status is one of {@link #ERROR_HARDWARE_FAILURE}, {@link #ERROR_SERVER_DIED},
         * {@link #ERROR_CANCELLED}, {@link #ERROR_SCAN_TIMEOUT},
         * {@link #ERROR_CONFIG}
         *
         * @deprecated Use {@link RadioTuner.Callback#onTuneFailed(int, ProgramSelector)} for
         *             tune, scan and step; other use cases (configuration, background scan)
         *             are already deprecated.
         */
        public void onError(int status) {}

        /**
         * Called when tune, scan or step operation fails.
         *
         * @param result cause of the failure
         * @param selector ProgramSelector argument of tune that failed;
         *                 null for scan and step.
         */
        public void onTuneFailed(@TunerResultType int result, @Nullable ProgramSelector selector) {}

        /**
         * onConfigurationChanged() is called upon successful completion of
         * {@link RadioManager#openTuner(int, RadioManager.BandConfig, boolean, Callback, Handler)}
         * or {@link RadioTuner#setConfiguration(RadioManager.BandConfig)}
         *
         * @deprecated Only applicable for HAL 1.x.
         */
        @Deprecated
        public void onConfigurationChanged(RadioManager.BandConfig config) {}

        /**
         * Called when program info (including metadata) for the current program has changed.
         *
         * <p>It happens either upon successful completion of {@link RadioTuner#step(int, boolean)},
         * {@link RadioTuner#scan(int, boolean)}, {@link RadioTuner#tune(int, int)}; when
         * a switching to alternate frequency occurs; or when metadata is updated.
         */
        public void onProgramInfoChanged(RadioManager.ProgramInfo info) {}

        /**
         * Called when metadata is updated for the current program.
         *
         * @deprecated Use {@link #onProgramInfoChanged(RadioManager.ProgramInfo)} instead.
         */
        @Deprecated
        public void onMetadataChanged(RadioMetadata metadata) {}

        /**
         * onTrafficAnnouncement() is called when a traffic announcement starts and stops.
         */
        public void onTrafficAnnouncement(boolean active) {}
        /**
         * onEmergencyAnnouncement() is called when an emergency annoucement starts and stops.
         */
        public void onEmergencyAnnouncement(boolean active) {}
        /**
         * onAntennaState() is called when the antenna is connected or disconnected.
         */
        public void onAntennaState(boolean connected) {}
        /**
         * onControlChanged() is called when the client loses or gains control of the radio tuner.
         * The control is always granted after a successful call to
         * {@link RadioManager#openTuner(int, RadioManager.BandConfig, boolean, Callback, Handler)}.
         * If another client opens the same tuner, onControlChanged() will be called with
         * control set to {@code false} to indicate loss of control.
         * At this point, RadioTuner APIs other than getters will return
         * {@link RadioManager#STATUS_INVALID_OPERATION}.
         * When the other client releases the tuner, onControlChanged() will be called
         * with control set to {@code true}.
         */
        public void onControlChanged(boolean control) {}

        /**
         * onBackgroundScanAvailabilityChange() is called when background scan
         * feature becomes available or not.
         *
         * @param isAvailable true, if the tuner turned temporarily background-
         *                    capable, false in the other case.
         */
        public void onBackgroundScanAvailabilityChange(boolean isAvailable) {}

        /**
         * Called when a background scan completes successfully.
         */
        public void onBackgroundScanComplete() {}

        /**
         * Called when available program list changed.
         *
         * Use {@link RadioTuner#getProgramList(Map)} to get an actual list.
         */
        public void onProgramListChanged() {}

        /**
         * Called when config flags are updated asynchronously due to internal events
         * in broadcast radio HAL.
         *
         * {@link RadioTuner#setConfigFlag(int, boolean)} must not trigger this
         * callback.
         *
         * @param flag Config flag updated
         * @param value Value of the updated config flag
         */
        public void onConfigFlagUpdated(@RadioManager.ConfigFlag int flag, boolean value) {}

        /**
         * Generic callback for passing updates to vendor-specific parameter values.
         *
         * <p>The framework does not interpret the parameters, they are passed
         * in an opaque manner between a vendor application and HAL.
         *
         * <p>It's up to the HAL implementation if and how to implement this callback,
         * as long as it obeys the prefix rule. In particular, only selected keys
         * may be notified this way. However, setParameters must not trigger
         * this callback, while an internal event can change parameters
         * asynchronously.
         *
         * @param parameters Vendor-specific key-value pairs.
         */
        public void onParametersUpdated(@NonNull Map<String, String> parameters) {}
    }

}

