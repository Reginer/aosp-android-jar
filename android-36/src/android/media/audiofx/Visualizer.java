/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.media.audiofx;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.AttributionSource;
import android.content.AttributionSource.ScopedParcelState;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;

import java.lang.ref.WeakReference;

/**
 * The Visualizer class enables application to retrieve part of the currently playing audio for
 * visualization purpose. It is not an audio recording interface and only returns partial and low
 * quality audio content. However, to protect privacy of certain audio data (e.g voice mail) the use
 * of the visualizer requires the permission android.permission.RECORD_AUDIO.
 * <p>The audio session ID passed to the constructor indicates which audio content should be
 * visualized:<br>
 * <ul>
 *   <li>If the session is 0, the audio output mix is visualized</li>
 *   <li>If the session is not 0, the audio from a particular {@link android.media.MediaPlayer} or
 *   {@link android.media.AudioTrack}
 *   using this audio session is visualized </li>
 * </ul>
 * <p>Two types of representation of audio content can be captured: <br>
 * <ul>
 *   <li>Waveform data: consecutive 8-bit (unsigned) mono samples by using the
 *   {@link #getWaveForm(byte[])} method</li>
 *   <li>Frequency data: 8-bit magnitude FFT by using the {@link #getFft(byte[])} method</li>
 * </ul>
 * <p>The length of the capture can be retrieved or specified by calling respectively
 * {@link #getCaptureSize()} and {@link #setCaptureSize(int)} methods. The capture size must be a
 * power of 2 in the range returned by {@link #getCaptureSizeRange()}.
 * <p>In addition to the polling capture mode described above with {@link #getWaveForm(byte[])} and
 *  {@link #getFft(byte[])} methods, a callback mode is also available by installing a listener by
 *  use of the {@link #setDataCaptureListener(OnDataCaptureListener, int, boolean, boolean)} method.
 *  The rate at which the listener capture method is called as well as the type of data returned is
 *  specified.
 * <p>Before capturing data, the Visualizer must be enabled by calling the
 * {@link #setEnabled(boolean)} method.
 * When data capture is not needed any more, the Visualizer should be disabled.
 * <p>It is good practice to call the {@link #release()} method when the Visualizer is not used
 * anymore to free up native resources associated to the Visualizer instance.
 * <p>Creating a Visualizer on the output mix (audio session 0) requires permission
 * {@link android.Manifest.permission#MODIFY_AUDIO_SETTINGS}
 * <p>The Visualizer class can also be used to perform measurements on the audio being played back.
 * The measurements to perform are defined by setting a mask of the requested measurement modes with
 * {@link #setMeasurementMode(int)}. Supported values are {@link #MEASUREMENT_MODE_NONE} to cancel
 * any measurement, and {@link #MEASUREMENT_MODE_PEAK_RMS} for peak and RMS monitoring.
 * Measurements can be retrieved through {@link #getMeasurementPeakRms(MeasurementPeakRms)}.
 */

public class Visualizer {

    static {
        System.loadLibrary("audioeffect_jni");
        native_init();
    }

    private final static String TAG = "Visualizer-JAVA";

    /**
     * State of a Visualizer object that was not successfully initialized upon creation
     */
    public static final int STATE_UNINITIALIZED = 0;
    /**
     * State of a Visualizer object that is ready to be used.
     */
    public static final int STATE_INITIALIZED   = 1;
    /**
     * State of a Visualizer object that is active.
     */
    public static final int STATE_ENABLED   = 2;

    // to keep in sync with system/media/audio_effects/include/audio_effects/effect_visualizer.h
    /**
     * Defines a capture mode where amplification is applied based on the content of the captured
     * data. This is the default Visualizer mode, and is suitable for music visualization.
     */
    public static final int SCALING_MODE_NORMALIZED = 0;
    /**
     * Defines a capture mode where the playback volume will affect (scale) the range of the
     * captured data. A low playback volume will lead to low sample and fft values, and vice-versa.
     */
    public static final int SCALING_MODE_AS_PLAYED = 1;

    /**
     * Defines a measurement mode in which no measurements are performed.
     */
    public static final int MEASUREMENT_MODE_NONE = 0;

    /**
     * Defines a measurement mode which computes the peak and RMS value in mB below the
     * "full scale", where 0mB is normally the maximum sample value (but see the note
     * below). Minimum value depends on the resolution of audio samples used by the audio
     * framework. The value of -9600mB is the minimum value for 16-bit audio systems and
     * -14400mB or below for "high resolution" systems. Values for peak and RMS can be
     * retrieved with {@link #getMeasurementPeakRms(MeasurementPeakRms)}.
     *
     * <p class=note><strong>Note:</strong> when Visualizer effect is attached to the
     * global session (with session ID 0), it is possible to observe RMS peaks higher than
     * 0 dBFS, for example in the case when there are multiple audio sources playing
     * simultaneously. In this case {@link #getMeasurementPeakRms(MeasurementPeakRms)}
     * method can return a positive value.
     */
    public static final int MEASUREMENT_MODE_PEAK_RMS = 1 << 0;

    // to keep in sync with frameworks/base/media/jni/audioeffect/android_media_Visualizer.cpp
    private static final int NATIVE_EVENT_PCM_CAPTURE = 0;
    private static final int NATIVE_EVENT_FFT_CAPTURE = 1;
    private static final int NATIVE_EVENT_SERVER_DIED = 2;

    // Error codes:
    /**
     * Successful operation.
     */
    public  static final int SUCCESS              = 0;
    /**
     * Unspecified error.
     */
    public  static final int ERROR                = -1;
    /**
     * Internal operation status. Not returned by any method.
     */
    public  static final int ALREADY_EXISTS       = -2;
    /**
     * Operation failed due to bad object initialization.
     */
    public  static final int ERROR_NO_INIT              = -3;
    /**
     * Operation failed due to bad parameter value.
     */
    public  static final int ERROR_BAD_VALUE            = -4;
    /**
     * Operation failed because it was requested in wrong state.
     */
    public  static final int ERROR_INVALID_OPERATION    = -5;
    /**
     * Operation failed due to lack of memory.
     */
    public  static final int ERROR_NO_MEMORY            = -6;
    /**
     * Operation failed due to dead remote object.
     */
    public  static final int ERROR_DEAD_OBJECT          = -7;

    //--------------------------------------------------------------------------
    // Member variables
    //--------------------
    /**
     * Indicates the state of the Visualizer instance
     */
    @GuardedBy("mStateLock")
    private int mState = STATE_UNINITIALIZED;
    /**
     * Lock to synchronize access to mState
     */
    private final Object mStateLock = new Object();
    /**
     * System wide unique Identifier of the visualizer engine used by this Visualizer instance
     */
    @GuardedBy("mStateLock")
    @UnsupportedAppUsage
    private int mId;

    /**
     * Lock to protect listeners updates against event notifications
     */
    private final Object mListenerLock = new Object();
    /**
     * Handler for events coming from the native code
     */
    @GuardedBy("mListenerLock")
    @Nullable private Handler mNativeEventHandler = null;
    /**
     *  PCM and FFT capture listener registered by client
     */
    @GuardedBy("mListenerLock")
    @Nullable private OnDataCaptureListener mCaptureListener = null;
    /**
     *  Server Died listener registered by client
     */
    @GuardedBy("mListenerLock")
    @Nullable private OnServerDiedListener mServerDiedListener = null;

    // accessed by native methods
    private long mNativeVisualizer;  // guarded by a static lock in native code
    private long mJniData;  // set in native_setup, _release;
                            // get in native_release, _setEnabled, _setPeriodicCapture
                            // thus, effectively guarded by mStateLock

    //--------------------------------------------------------------------------
    // Constructor, Finalize
    //--------------------
    /**
     * Class constructor.
     * @param audioSession system wide unique audio session identifier. If audioSession
     *  is not 0, the visualizer will be attached to the MediaPlayer or AudioTrack in the
     *  same audio session. Otherwise, the Visualizer will apply to the output mix.
     *
     * @throws java.lang.UnsupportedOperationException
     * @throws java.lang.RuntimeException
     */

    public Visualizer(int audioSession)
    throws UnsupportedOperationException, RuntimeException {
        int[] id = new int[1];

        synchronized (mStateLock) {
            mState = STATE_UNINITIALIZED;

            // native initialization
            // TODO b/182469354: make consistent with AudioRecord
            int result;
            try (ScopedParcelState attributionSourceState = AttributionSource.myAttributionSource()
                    .asScopedParcelState()) {
                result = native_setup(new WeakReference<>(this), audioSession, id,
                        attributionSourceState.getParcel());
            }
            if (result != SUCCESS && result != ALREADY_EXISTS) {
                Log.e(TAG, "Error code "+result+" when initializing Visualizer.");
                switch (result) {
                case ERROR_INVALID_OPERATION:
                    throw (new UnsupportedOperationException("Effect library not loaded"));
                default:
                    throw (new RuntimeException("Cannot initialize Visualizer engine, error: "
                            +result));
                }
            }
            mId = id[0];
            if (native_getEnabled()) {
                mState = STATE_ENABLED;
            } else {
                mState = STATE_INITIALIZED;
            }
        }
    }

    /**
     * Releases the native Visualizer resources. It is a good practice to release the
     * visualization engine when not in use.
     */
    public void release() {
        synchronized (mStateLock) {
            native_release();
            mState = STATE_UNINITIALIZED;
        }
    }

    @Override
    protected void finalize() {
        synchronized (mStateLock) {
            native_finalize();
        }
    }

    /**
     * Enable or disable the visualization engine.
     * @param enabled requested enable state
     * @return {@link #SUCCESS} in case of success,
     * {@link #ERROR_INVALID_OPERATION} or {@link #ERROR_DEAD_OBJECT} in case of failure.
     * @throws IllegalStateException
     */
    public int setEnabled(boolean enabled)
    throws IllegalStateException {
        synchronized (mStateLock) {
            if (mState == STATE_UNINITIALIZED) {
                throw(new IllegalStateException("setEnabled() called in wrong state: "+mState));
            }
            int status = SUCCESS;
            if ((enabled && (mState == STATE_INITIALIZED)) ||
                    (!enabled && (mState == STATE_ENABLED))) {
                status = native_setEnabled(enabled);
                if (status == SUCCESS) {
                    mState = enabled ? STATE_ENABLED : STATE_INITIALIZED;
                }
            }
            return status;
        }
    }

    /**
     * Get current activation state of the visualizer.
     * @return true if the visualizer is active, false otherwise
     */
    public boolean getEnabled()
    {
        synchronized (mStateLock) {
            if (mState == STATE_UNINITIALIZED) {
                throw(new IllegalStateException("getEnabled() called in wrong state: "+mState));
            }
            return native_getEnabled();
        }
    }

    /**
     * Returns the capture size range.
     * @return the mininum capture size is returned in first array element and the maximum in second
     * array element.
     */
    public static native int[] getCaptureSizeRange();

    /**
     * Returns the maximum capture rate for the callback capture method. This is the maximum value
     * for the rate parameter of the
     * {@link #setDataCaptureListener(OnDataCaptureListener, int, boolean, boolean)} method.
     * @return the maximum capture rate expressed in milliHertz
     */
    public static native int getMaxCaptureRate();

    /**
     * Sets the capture size, i.e. the number of bytes returned by {@link #getWaveForm(byte[])} and
     * {@link #getFft(byte[])} methods. The capture size must be a power of 2 in the range returned
     * by {@link #getCaptureSizeRange()}.
     * This method must not be called when the Visualizer is enabled.
     * @param size requested capture size
     * @return {@link #SUCCESS} in case of success,
     * {@link #ERROR_INVALID_OPERATION} if Visualizer effect enginer not enabled.
     * @throws IllegalStateException if the effect is not in proper state.
     * @throws IllegalArgumentException if the size parameter is invalid (out of supported range).
     */
    public int setCaptureSize(int size)
    throws IllegalStateException {
        synchronized (mStateLock) {
            if (mState != STATE_INITIALIZED) {
                throw(new IllegalStateException("setCaptureSize() called in wrong state: "+mState));
            }

            int ret = native_setCaptureSize(size);
            if (ret == ERROR_BAD_VALUE) {
                throw(new IllegalArgumentException("setCaptureSize to " + size + " failed"));
            }

            return ret;
        }
    }

    /**
     * Returns current capture size.
     * @return the capture size in bytes.
     */
    public int getCaptureSize()
    throws IllegalStateException {
        synchronized (mStateLock) {
            if (mState == STATE_UNINITIALIZED) {
                throw(new IllegalStateException("getCaptureSize() called in wrong state: "+mState));
            }
            return native_getCaptureSize();
        }
    }

    /**
     * Set the type of scaling applied on the captured visualization data.
     * @param mode see {@link #SCALING_MODE_NORMALIZED}
     *     and {@link #SCALING_MODE_AS_PLAYED}
     * @return {@link #SUCCESS} in case of success,
     *     {@link #ERROR_BAD_VALUE} in case of failure.
     * @throws IllegalStateException
     */
    public int setScalingMode(int mode)
    throws IllegalStateException {
        synchronized (mStateLock) {
            if (mState == STATE_UNINITIALIZED) {
                throw(new IllegalStateException("setScalingMode() called in wrong state: "
                        + mState));
            }
            return native_setScalingMode(mode);
        }
    }

    /**
     * Returns the current scaling mode on the captured visualization data.
     * @return the scaling mode, see {@link #SCALING_MODE_NORMALIZED}
     *     and {@link #SCALING_MODE_AS_PLAYED}.
     * @throws IllegalStateException
     */
    public int getScalingMode()
    throws IllegalStateException {
        synchronized (mStateLock) {
            if (mState == STATE_UNINITIALIZED) {
                throw(new IllegalStateException("getScalingMode() called in wrong state: "
                        + mState));
            }
            return native_getScalingMode();
        }
    }

    /**
     * Sets the combination of measurement modes to be performed by this audio effect.
     * @param mode a mask of the measurements to perform. The valid values are
     *     {@link #MEASUREMENT_MODE_NONE} (to cancel any measurement)
     *     or {@link #MEASUREMENT_MODE_PEAK_RMS}.
     * @return {@link #SUCCESS} in case of success, {@link #ERROR_BAD_VALUE} in case of failure.
     * @throws IllegalStateException
     */
    public int setMeasurementMode(int mode)
            throws IllegalStateException {
        synchronized (mStateLock) {
            if (mState == STATE_UNINITIALIZED) {
                throw(new IllegalStateException("setMeasurementMode() called in wrong state: "
                        + mState));
            }
            return native_setMeasurementMode(mode);
        }
    }

    /**
     * Returns the current measurement modes performed by this audio effect
     * @return the mask of the measurements,
     *     {@link #MEASUREMENT_MODE_NONE} (when no measurements are performed)
     *     or {@link #MEASUREMENT_MODE_PEAK_RMS}.
     * @throws IllegalStateException
     */
    public int getMeasurementMode()
            throws IllegalStateException {
        synchronized (mStateLock) {
            if (mState == STATE_UNINITIALIZED) {
                throw(new IllegalStateException("getMeasurementMode() called in wrong state: "
                        + mState));
            }
            return native_getMeasurementMode();
        }
    }

    /**
     * Returns the sampling rate of the captured audio.
     * @return the sampling rate in milliHertz.
     */
    public int getSamplingRate()
    throws IllegalStateException {
        synchronized (mStateLock) {
            if (mState == STATE_UNINITIALIZED) {
                throw(new IllegalStateException("getSamplingRate() called in wrong state: "+mState));
            }
            return native_getSamplingRate();
        }
    }

    /**
     * Returns a waveform capture of currently playing audio content. The capture consists in
     * a number of consecutive 8-bit (unsigned) mono PCM samples equal to the capture size returned
     * by {@link #getCaptureSize()}.
     * <p>This method must be called when the Visualizer is enabled.
     * @param waveform array of bytes where the waveform should be returned, array length must be
     * at least equals to the capture size returned by {@link #getCaptureSize()}.
     * @return {@link #SUCCESS} in case of success,
     * {@link #ERROR_NO_MEMORY}, {@link #ERROR_INVALID_OPERATION} or {@link #ERROR_DEAD_OBJECT}
     * in case of failure.
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     */
    public int getWaveForm(byte[] waveform)
    throws IllegalStateException {
        synchronized (mStateLock) {
            if (mState != STATE_ENABLED) {
                throw(new IllegalStateException("getWaveForm() called in wrong state: "+mState));
            }
            int captureSize = getCaptureSize();
            if (captureSize > waveform.length) {
                throw(new IllegalArgumentException("getWaveForm() called with illegal size: "
                                                   + waveform.length + " expecting at least "
                                                   + captureSize + " bytes"));
            }
            return native_getWaveForm(waveform);
        }
    }
    /**
     * Returns a frequency capture of currently playing audio content.
     * <p>This method must be called when the Visualizer is enabled.
     * <p>The capture is an 8-bit magnitude FFT, the frequency range covered being 0 (DC) to half of
     * the sampling rate returned by {@link #getSamplingRate()}. The capture returns the real and
     * imaginary parts of a number of frequency points equal to half of the capture size plus one.
     * <p>Note: only the real part is returned for the first point (DC) and the last point
     * (sampling frequency / 2).
     * <p>The layout in the returned byte array is as follows:
     * <ul>
     *   <li> n is the capture size returned by getCaptureSize()</li>
     *   <li> Rfk, Ifk are respectively  the real and imaginary parts of the kth frequency
     *   component</li>
     *   <li> If Fs is the sampling frequency retuned by getSamplingRate() the kth frequency is:
     *   k * Fs / n </li>
     * </ul>
     * <table border="0" cellspacing="0" cellpadding="0">
     * <tr><td>Index </p></td>
     *     <td>0 </p></td>
     *     <td>1 </p></td>
     *     <td>2 </p></td>
     *     <td>3 </p></td>
     *     <td>4 </p></td>
     *     <td>5 </p></td>
     *     <td>... </p></td>
     *     <td>n - 2 </p></td>
     *     <td>n - 1 </p></td></tr>
     * <tr><td>Data </p></td>
     *     <td>Rf0 </p></td>
     *     <td>Rf(n/2) </p></td>
     *     <td>Rf1 </p></td>
     *     <td>If1 </p></td>
     *     <td>Rf2 </p></td>
     *     <td>If2 </p></td>
     *     <td>... </p></td>
     *     <td>Rf(n/2-1) </p></td>
     *     <td>If(n/2-1) </p></td></tr>
     * </table>
     * <p>In order to obtain magnitude and phase values the following code can
     * be used:
     *    <pre class="prettyprint">
     *       int n = fft.size();
     *       float[] magnitudes = new float[n / 2 + 1];
     *       float[] phases = new float[n / 2 + 1];
     *       magnitudes[0] = (float)Math.abs(fft[0]);      // DC
     *       magnitudes[n / 2] = (float)Math.abs(fft[1]);  // Nyquist
     *       phases[0] = phases[n / 2] = 0;
     *       for (int k = 1; k &lt; n / 2; k++) {
     *           int i = k * 2;
     *           magnitudes[k] = (float)Math.hypot(fft[i], fft[i + 1]);
     *           phases[k] = (float)Math.atan2(fft[i + 1], fft[i]);
     *       }</pre>
     * @param fft array of bytes where the FFT should be returned
     * @return {@link #SUCCESS} in case of success,
     * {@link #ERROR_NO_MEMORY}, {@link #ERROR_INVALID_OPERATION} or {@link #ERROR_DEAD_OBJECT}
     * in case of failure.
     * @throws IllegalStateException
     */
    public int getFft(byte[] fft)
    throws IllegalStateException {
        synchronized (mStateLock) {
            if (mState != STATE_ENABLED) {
                throw(new IllegalStateException("getFft() called in wrong state: "+mState));
            }
            return native_getFft(fft);
        }
    }

    /**
     * A class to store peak and RMS values.
     * Peak and RMS are expressed in mB, as described in the
     * {@link Visualizer#MEASUREMENT_MODE_PEAK_RMS} measurement mode.
     */
    public static final class MeasurementPeakRms {
        /**
         * The peak value in mB.
         */
        public int mPeak;
        /**
         * The RMS value in mB.
         */
        public int mRms;
    }

    /**
     * Retrieves the latest peak and RMS measurement.
     * Sets the peak and RMS fields of the supplied {@link Visualizer.MeasurementPeakRms} to the
     * latest measured values.
     * @param measurement a non-null {@link Visualizer.MeasurementPeakRms} instance to store
     *    the measurement values.
     * @return {@link #SUCCESS} in case of success, {@link #ERROR_BAD_VALUE},
     *    {@link #ERROR_NO_MEMORY}, {@link #ERROR_INVALID_OPERATION} or {@link #ERROR_DEAD_OBJECT}
     *    in case of failure.
     */
    public int getMeasurementPeakRms(MeasurementPeakRms measurement) {
        if (measurement == null) {
            Log.e(TAG, "Cannot store measurements in a null object");
            return ERROR_BAD_VALUE;
        }
        synchronized (mStateLock) {
            if (mState != STATE_ENABLED) {
                throw (new IllegalStateException("getMeasurementPeakRms() called in wrong state: "
                        + mState));
            }
            return native_getPeakRms(measurement);
        }
    }

    //---------------------------------------------------------
    // Interface definitions
    //--------------------
    /**
     * The OnDataCaptureListener interface defines methods called by the Visualizer to periodically
     * update the audio visualization capture.
     * The client application can implement this interface and register the listener with the
     * {@link #setDataCaptureListener(OnDataCaptureListener, int, boolean, boolean)} method.
     */
    public interface OnDataCaptureListener  {
        /**
         * Method called when a new waveform capture is available.
         * <p>Data in the waveform buffer is valid only within the scope of the callback.
         * Applications which need access to the waveform data after returning from the callback
         * should make a copy of the data instead of holding a reference.
         * @param visualizer Visualizer object on which the listener is registered.
         * @param waveform array of bytes containing the waveform representation.
         * @param samplingRate sampling rate of the visualized audio.
         */
        void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate);

        /**
         * Method called when a new frequency capture is available.
         * <p>Data in the fft buffer is valid only within the scope of the callback.
         * Applications which need access to the fft data after returning from the callback
         * should make a copy of the data instead of holding a reference.
         * <p>For the explanation of the fft data array layout, and the example
         * code for processing it, please see the documentation for {@link #getFft(byte[])} method.
         *
         * @param visualizer Visualizer object on which the listener is registered.
         * @param fft array of bytes containing the frequency representation.
         * @param samplingRate sampling rate of the visualized audio.
         */
        void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate);
    }

    /**
     * Registers an OnDataCaptureListener interface and specifies the rate at which the capture
     * should be updated as well as the type of capture requested.
     * <p>Call this method with a null listener to stop receiving the capture updates.
     * @param listener OnDataCaptureListener registered
     * @param rate rate in milliHertz at which the capture should be updated
     * @param waveform true if a waveform capture is requested: the onWaveFormDataCapture()
     * method will be called on the OnDataCaptureListener interface.
     * @param fft true if a frequency capture is requested: the onFftDataCapture() method will be
     * called on the OnDataCaptureListener interface.
     * @return {@link #SUCCESS} in case of success,
     * {@link #ERROR_NO_INIT} or {@link #ERROR_BAD_VALUE} in case of failure.
     */
    public int setDataCaptureListener(@Nullable OnDataCaptureListener listener,
            int rate, boolean waveform, boolean fft) {
        if (listener == null) {
            // make sure capture callback is stopped in native code
            waveform = false;
            fft = false;
        }
        int status;
        synchronized (mStateLock) {
            status = native_setPeriodicCapture(rate, waveform, fft);
        }
        if (status == SUCCESS) {
            synchronized (mListenerLock) {
                mCaptureListener = listener;
                if ((listener != null) && (mNativeEventHandler == null)) {
                    Looper looper;
                    if ((looper = Looper.myLooper()) != null) {
                        mNativeEventHandler = new Handler(looper);
                    } else if ((looper = Looper.getMainLooper()) != null) {
                        mNativeEventHandler = new Handler(looper);
                    } else {
                        mNativeEventHandler = null;
                        status = ERROR_NO_INIT;
                    }
                }
            }
        }
        return status;
    }

    /**
     * @hide
     *
     * The OnServerDiedListener interface defines a method called by the Visualizer to indicate that
     * the connection to the native media server has been broken and that the Visualizer object will
     * need to be released and re-created.
     * The client application can implement this interface and register the listener with the
     * {@link #setServerDiedListener(OnServerDiedListener)} method.
     */
    public interface OnServerDiedListener  {
        /**
         * @hide
         *
         * Method called when the native media server has died.
         * <p>If the native media server encounters a fatal error and needs to restart, the binder
         * connection from the {@link #Visualizer} to the media server will be broken.  Data capture
         * callbacks will stop happening, and client initiated calls to the {@link #Visualizer}
         * instance will fail with the error code {@link #DEAD_OBJECT}.  To restore functionality,
         * clients should {@link #release()} their old visualizer and create a new instance.
         */
        void onServerDied();
    }

    /**
     * @hide
     *
     * Registers an OnServerDiedListener interface.
     * <p>Call this method with a null listener to stop receiving server death notifications.
     * @return {@link #SUCCESS} in case of success,
     */
    public int setServerDiedListener(@Nullable OnServerDiedListener listener) {
        synchronized (mListenerLock) {
            mServerDiedListener = listener;
        }
        return SUCCESS;
    }

    //---------------------------------------------------------
    // Interface definitions
    //--------------------

    private static native final void native_init();

    @GuardedBy("mStateLock")
    private native final int native_setup(Object audioeffect_this,
                                          int audioSession,
                                          int[] id,
                                          @NonNull Parcel attributionSource);

    @GuardedBy("mStateLock")
    private native final void native_finalize();

    @GuardedBy("mStateLock")
    private native final void native_release();

    @GuardedBy("mStateLock")
    private native final int native_setEnabled(boolean enabled);

    @GuardedBy("mStateLock")
    private native final boolean native_getEnabled();

    @GuardedBy("mStateLock")
    private native final int native_setCaptureSize(int size);

    @GuardedBy("mStateLock")
    private native final int native_getCaptureSize();

    @GuardedBy("mStateLock")
    private native final int native_setScalingMode(int mode);

    @GuardedBy("mStateLock")
    private native final int native_getScalingMode();

    @GuardedBy("mStateLock")
    private native final int native_setMeasurementMode(int mode);

    @GuardedBy("mStateLock")
    private native final int native_getMeasurementMode();

    @GuardedBy("mStateLock")
    private native final int native_getSamplingRate();

    @GuardedBy("mStateLock")
    private native final int native_getWaveForm(byte[] waveform);

    @GuardedBy("mStateLock")
    private native final int native_getFft(byte[] fft);

    @GuardedBy("mStateLock")
    private native final int native_getPeakRms(MeasurementPeakRms measurement);

    @GuardedBy("mStateLock")
    private native final int native_setPeriodicCapture(int rate, boolean waveForm, boolean fft);

    //---------------------------------------------------------
    // Java methods called from the native side
    //--------------------
    @SuppressWarnings("unused")
    private static void postEventFromNative(Object effect_ref,
            int what, int samplingRate, byte[] data) {
        final Visualizer visualizer = (Visualizer) ((WeakReference) effect_ref).get();
        if (visualizer == null) return;

        final Handler handler;
        synchronized (visualizer.mListenerLock) {
            handler = visualizer.mNativeEventHandler;
        }
        if (handler == null) return;

        switch (what) {
            case NATIVE_EVENT_PCM_CAPTURE:
            case NATIVE_EVENT_FFT_CAPTURE:
                handler.post(() -> {
                    final OnDataCaptureListener l;
                    synchronized (visualizer.mListenerLock) {
                        l = visualizer.mCaptureListener;
                    }
                    if (l != null) {
                        if (what == NATIVE_EVENT_PCM_CAPTURE) {
                            l.onWaveFormDataCapture(visualizer, data, samplingRate);
                        } else { // what == NATIVE_EVENT_FFT_CAPTURE
                            l.onFftDataCapture(visualizer, data, samplingRate);
                        }
                    }
                });
                break;
            case NATIVE_EVENT_SERVER_DIED:
                handler.post(() -> {
                    final OnServerDiedListener l;
                    synchronized (visualizer.mListenerLock) {
                        l = visualizer.mServerDiedListener;
                    }
                    if (l != null) {
                        l.onServerDied();
                    }
                });
                break;
            default:
                Log.e(TAG, "Unknown native event in postEventFromNative: " + what);
                break;
        }
    }
}
