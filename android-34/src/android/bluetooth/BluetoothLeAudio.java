/*
 * Copyright 2020 HIMSA II K/S - www.himsa.com.
 * Represented by EHIMA - www.ehima.com
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

package android.bluetooth;

import static android.bluetooth.BluetoothUtils.getSyncTimeout;

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.bluetooth.annotations.RequiresLegacyBluetoothPermission;
import android.content.AttributionSource;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.CloseGuard;
import android.util.Log;

import com.android.modules.utils.SynchronousResultReceiver;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

/**
 * This class provides the public APIs to control the LeAudio profile.
 *
 * <p>BluetoothLeAudio is a proxy object for controlling the Bluetooth LE Audio
 * Service via IPC. Use {@link BluetoothAdapter#getProfileProxy} to get
 * the BluetoothLeAudio proxy object.
 *
 * <p> Android only supports one set of connected Bluetooth LeAudio device at a time. Each
 * method is protected with its appropriate permission.
 */
public final class BluetoothLeAudio implements BluetoothProfile, AutoCloseable {
    private static final String TAG = "BluetoothLeAudio";
    private static final boolean DBG = false;
    private static final boolean VDBG = false;

    private final Map<Callback, Executor> mCallbackExecutorMap = new HashMap<>();

    private CloseGuard mCloseGuard;

    private final class LeAudioServiceListener extends ForwardingServiceListener {
        LeAudioServiceListener(ServiceListener listener) {
            super(listener);
        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            try {
                if (profile == LE_AUDIO) {
                    // re-register the service-to-app callback
                    synchronized (mCallbackExecutorMap) {
                        if (mCallbackExecutorMap.isEmpty()) {
                            return;
                        }
                        try {
                            final IBluetoothLeAudio service = getService();
                            if (service != null) {
                                final SynchronousResultReceiver<Integer> recv =
                                        SynchronousResultReceiver.get();
                                service.registerCallback(mCallback, mAttributionSource, recv);
                                recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(null);
                            }
                        } catch (TimeoutException e) {
                            Log.e(TAG, "Failed to register callback", e);
                        } catch (RemoteException e) {
                            throw e.rethrowFromSystemServer();
                        }
                    }
                }
            } finally {
                super.onServiceConnected(profile, proxy);
            }
        }
    }

    /**
     * This class provides a callback that is invoked when audio codec config changes on
     * the remote device.
     *
     * @hide
     */
    @SystemApi
    public interface Callback {
        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {
            GROUP_STATUS_ACTIVE,
            GROUP_STATUS_INACTIVE,
        })
        @interface GroupStatus {}

        /**
         * Callback invoked when callback is registered and when codec config
         * changes on the remote device.
         *
         * @param groupId the group id
         * @param status latest codec status for this group
         * @hide
         */
        @SystemApi
        void onCodecConfigChanged(int groupId,
                                  @NonNull BluetoothLeAudioCodecStatus status);

        /**
         * Callback invoked when a device has been added to the group.
         * It usually happens after connection or on bluetooth startup if
         * the device is bonded.
         *
         * @param device the device which is added to the group
         * @param groupId the group id
         * @hide
         */
        @SystemApi
        void onGroupNodeAdded(@NonNull BluetoothDevice device, int groupId);

        /**
         * Callback invoked when a device has been removed from the group.
         * It usually happens when device gets unbonded.
         *
         * @param device the device which is removed from the group
         * @param groupId the group id
         *
         * @hide
         */
        @SystemApi
        void onGroupNodeRemoved(@NonNull BluetoothDevice device, int groupId);

        /**
         * Callback invoked the group's active state changes.
         *
         * @param groupId the group id
         * @param groupStatus active or inactive state.
         * @hide
         */
        @SystemApi
        void onGroupStatusChanged(int groupId, @GroupStatus int groupStatus);
    }

    @SuppressLint("AndroidFrameworkBluetoothPermission")
    private final IBluetoothLeAudioCallback mCallback = new IBluetoothLeAudioCallback.Stub() {
        @Override
        public void onCodecConfigChanged(int groupId,
                                         @NonNull BluetoothLeAudioCodecStatus status) {
            for (Map.Entry<BluetoothLeAudio.Callback, Executor> callbackExecutorEntry:
                    mCallbackExecutorMap.entrySet()) {
                BluetoothLeAudio.Callback callback = callbackExecutorEntry.getKey();
                Executor executor = callbackExecutorEntry.getValue();
                executor.execute(() -> callback.onCodecConfigChanged(groupId, status));
            }
        }

        @Override
        public void onGroupNodeAdded(@NonNull BluetoothDevice device, int groupId) {
            Attributable.setAttributionSource(device, mAttributionSource);
            for (Map.Entry<BluetoothLeAudio.Callback, Executor> callbackExecutorEntry:
                    mCallbackExecutorMap.entrySet()) {
                BluetoothLeAudio.Callback callback = callbackExecutorEntry.getKey();
                Executor executor = callbackExecutorEntry.getValue();
                executor.execute(() -> callback.onGroupNodeAdded(device, groupId));
            }
        }

        @Override
        public void onGroupNodeRemoved(@NonNull BluetoothDevice device, int groupId) {
            Attributable.setAttributionSource(device, mAttributionSource);
            for (Map.Entry<BluetoothLeAudio.Callback, Executor> callbackExecutorEntry:
                    mCallbackExecutorMap.entrySet()) {
                BluetoothLeAudio.Callback callback = callbackExecutorEntry.getKey();
                Executor executor = callbackExecutorEntry.getValue();
                executor.execute(() -> callback.onGroupNodeRemoved(device, groupId));
            }
        }

        @Override
        public void onGroupStatusChanged(int groupId, int groupStatus) {
            for (Map.Entry<BluetoothLeAudio.Callback, Executor> callbackExecutorEntry:
                    mCallbackExecutorMap.entrySet()) {
                BluetoothLeAudio.Callback callback = callbackExecutorEntry.getKey();
                Executor executor = callbackExecutorEntry.getValue();
                executor.execute(() -> callback.onGroupStatusChanged(groupId, groupStatus));
            }
        }
    };

    /**
     * Intent used to broadcast the change in connection state of the LeAudio
     * profile. Please note that in the binaural case, there will be two different LE devices for
     * the left and right side and each device will have their own connection state changes.
     *
     * <p>This intent will have 3 extras:
     * <ul>
     * <li> {@link #EXTRA_STATE} - The current state of the profile. </li>
     * <li> {@link #EXTRA_PREVIOUS_STATE}- The previous state of the profile.</li>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device. </li>
     * </ul>
     *
     * <p>{@link #EXTRA_STATE} or {@link #EXTRA_PREVIOUS_STATE} can be any of
     * {@link #STATE_DISCONNECTED}, {@link #STATE_CONNECTING},
     * {@link #STATE_CONNECTED}, {@link #STATE_DISCONNECTING}.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED =
            "android.bluetooth.action.LE_AUDIO_CONNECTION_STATE_CHANGED";

    /**
     * Intent used to broadcast the selection of a connected device as active.
     *
     * <p>This intent will have one extra:
     * <ul>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device. It can
     * be null if no device is active. </li>
     * </ul>
     *
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_LE_AUDIO_ACTIVE_DEVICE_CHANGED =
            "android.bluetooth.action.LE_AUDIO_ACTIVE_DEVICE_CHANGED";

    /**
     * Indicates invalid/unset audio context.
     * @hide
     */
    public static final int CONTEXT_TYPE_INVALID = 0x0000;

    /**
     * Indicates unspecified audio content.
     * @hide
     */
    public static final int CONTEXT_TYPE_UNSPECIFIED = 0x0001;

    /**
     * Indicates conversation between humans as, for example, in telephony or video calls.
     * @hide
     */
    public static final int CONTEXT_TYPE_CONVERSATIONAL = 0x0002;

    /**
     * Indicates media as, for example, in music, public radio, podcast or video soundtrack.
     * @hide
     */
    public static final int CONTEXT_TYPE_MEDIA = 0x0004;

    /**
     * Indicates audio associated with a video gaming.
     * @hide
     */
    public static final int CONTEXT_TYPE_GAME = 0x0008;

    /**
     * Indicates instructional audio as, for example, in navigation, announcements or user
     * guidance.
     * @hide
     */
    public static final int CONTEXT_TYPE_INSTRUCTIONAL = 0x0010;

    /**
     * Indicates man machine communication as, for example, with voice recognition or virtual
     * assistant.
     * @hide
     */
    public static final int CONTEXT_TYPE_VOICE_ASSISTANTS = 0x0020;

    /**
     * Indicates audio associated with a live audio stream.
     *
     * @hide
     */
    public static final int CONTEXT_TYPE_LIVE = 0x0040;

    /**
     * Indicates sound effects as, for example, in keyboard, touch feedback; menu and user
     * interface sounds, and other system sounds.
     * @hide
     */
    public static final int CONTEXT_TYPE_SOUND_EFFECTS = 0x0080;

    /**
     * Indicates notification and reminder sounds, attention-seeking audio, for example, in beeps
     * signaling the arrival of a message.
     * @hide
     */
    public static final int CONTEXT_TYPE_NOTIFICATIONS = 0x0100;


    /**
     * Indicates ringtone as in a call alert.
     * @hide
     */
    public static final int CONTEXT_TYPE_RINGTONE = 0x0200;

    /**
     * Indicates alerts and timers, immediate alerts as, for example, in a low battery alarm,
     * timer expiry or alarm clock.
     * @hide
     */
    public static final int CONTEXT_TYPE_ALERTS = 0x0400;


    /**
     * Indicates emergency alarm as, for example, with fire alarms or other urgent alerts.
     * @hide
     */
    public static final int CONTEXT_TYPE_EMERGENCY_ALARM = 0x0800;

    /**
     * This represents an invalid group ID.
     */
    public static final int GROUP_ID_INVALID = IBluetoothLeAudio.LE_AUDIO_GROUP_ID_INVALID;

    /**
     * This represents an invalid audio location.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_INVALID = 0;

    /**
     * This represents an audio location front left.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_FRONT_LEFT = 0x01 << 0;

    /**
     * This represents an audio location front right.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_FRONT_RIGHT = 0x01 << 1;

    /**
     * This represents an audio location front center.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_FRONT_CENTER = 0x01 << 2;

    /**
     * This represents an audio location low frequency effects 1.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_LOW_FREQ_EFFECTS_ONE = 0x01 << 3;

    /**
     * This represents an audio location back left.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_BACK_LEFT = 0x01 << 4;

    /**
     * This represents an audio location back right.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_BACK_RIGHT = 0x01 << 5;

    /**
     * This represents an audio location front left of center.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_FRONT_LEFT_OF_CENTER = 0x01 << 6;

    /**
     * This represents an audio location front right of center.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_FRONT_RIGHT_OF_CENTER = 0x01 << 7;

    /**
     * This represents an audio location back center.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_BACK_CENTER = 0x01 << 8;

    /**
     * This represents an audio location low frequency effects 2.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_LOW_FREQ_EFFECTS_TWO = 0x01 << 9;

    /**
     * This represents an audio location side left.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_SIDE_LEFT = 0x01 << 10;

    /**
     * This represents an audio location side right.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_SIDE_RIGHT = 0x01 << 11;

    /**
     * This represents an audio location top front left.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_TOP_FRONT_LEFT = 0x01 << 12;

    /**
     * This represents an audio location top front right.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_TOP_FRONT_RIGHT = 0x01 << 13;

    /**
     * This represents an audio location top front center.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_TOP_FRONT_CENTER = 0x01 << 14;

    /**
     * This represents an audio location top center.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_TOP_CENTER = 0x01 << 15;

    /**
     * This represents an audio location top back left.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_TOP_BACK_LEFT = 0x01 << 16;

    /**
     * This represents an audio location top back right.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_TOP_BACK_RIGHT = 0x01 << 17;

    /**
     * This represents an audio location top side left.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_TOP_SIDE_LEFT = 0x01 << 18;

    /**
     * This represents an audio location top side right.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_TOP_SIDE_RIGHT = 0x01 << 19;

    /**
     * This represents an audio location top back center.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_TOP_BACK_CENTER = 0x01 << 20;

    /**
     * This represents an audio location bottom front center.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_BOTTOM_FRONT_CENTER = 0x01 << 21;

    /**
     * This represents an audio location bottom front left.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_BOTTOM_FRONT_LEFT = 0x01 << 22;

    /**
     * This represents an audio location bottom front right.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_BOTTOM_FRONT_RIGHT = 0x01 << 23;

    /**
     * This represents an audio location front left wide.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_FRONT_LEFT_WIDE = 0x01 << 24;

    /**
     * This represents an audio location front right wide.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_FRONT_RIGHT_WIDE = 0x01 << 25;

    /**
     * This represents an audio location left surround.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_LEFT_SURROUND = 0x01 << 26;

    /**
     * This represents an audio location right surround.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_LOCATION_RIGHT_SURROUND = 0x01 << 27;

    /** @hide */
    @IntDef(flag = true, prefix = "AUDIO_LOCATION_",
            value = {
            AUDIO_LOCATION_FRONT_LEFT,
            AUDIO_LOCATION_FRONT_RIGHT,
            AUDIO_LOCATION_FRONT_CENTER,
            AUDIO_LOCATION_LOW_FREQ_EFFECTS_ONE,
            AUDIO_LOCATION_BACK_LEFT,
            AUDIO_LOCATION_BACK_RIGHT,
            AUDIO_LOCATION_FRONT_LEFT_OF_CENTER,
            AUDIO_LOCATION_FRONT_RIGHT_OF_CENTER,
            AUDIO_LOCATION_BACK_CENTER,
            AUDIO_LOCATION_LOW_FREQ_EFFECTS_TWO,
            AUDIO_LOCATION_SIDE_LEFT,
            AUDIO_LOCATION_SIDE_RIGHT,
            AUDIO_LOCATION_TOP_FRONT_LEFT,
            AUDIO_LOCATION_TOP_FRONT_RIGHT,
            AUDIO_LOCATION_TOP_FRONT_CENTER,
            AUDIO_LOCATION_TOP_CENTER,
            AUDIO_LOCATION_TOP_BACK_LEFT,
            AUDIO_LOCATION_TOP_BACK_RIGHT,
            AUDIO_LOCATION_TOP_SIDE_LEFT,
            AUDIO_LOCATION_TOP_SIDE_RIGHT,
            AUDIO_LOCATION_TOP_BACK_CENTER,
            AUDIO_LOCATION_BOTTOM_FRONT_CENTER,
            AUDIO_LOCATION_BOTTOM_FRONT_LEFT,
            AUDIO_LOCATION_BOTTOM_FRONT_RIGHT,
            AUDIO_LOCATION_FRONT_LEFT_WIDE,
            AUDIO_LOCATION_FRONT_RIGHT_WIDE,
            AUDIO_LOCATION_LEFT_SURROUND,
            AUDIO_LOCATION_RIGHT_SURROUND,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AudioLocation {}

    /**
     * Contains group id.
     * @hide
     */
    @SystemApi
    public static final String EXTRA_LE_AUDIO_GROUP_ID =
            "android.bluetooth.extra.LE_AUDIO_GROUP_ID";

    /**
     * Contains bit mask for direction, bit 0 set when Sink, bit 1 set when Source.
     * @hide
     */
    public static final String EXTRA_LE_AUDIO_DIRECTION =
            "android.bluetooth.extra.LE_AUDIO_DIRECTION";

    /**
     * Contains source location as per Bluetooth Assigned Numbers
     * @hide
     */
    public static final String EXTRA_LE_AUDIO_SOURCE_LOCATION =
            "android.bluetooth.extra.LE_AUDIO_SOURCE_LOCATION";

    /**
     * Contains sink location as per Bluetooth Assigned Numbers
     * @hide
     */
    public static final String EXTRA_LE_AUDIO_SINK_LOCATION =
            "android.bluetooth.extra.LE_AUDIO_SINK_LOCATION";

    /**
     * Contains available context types for group as per Bluetooth Assigned Numbers
     * @hide
     */
    public static final String EXTRA_LE_AUDIO_AVAILABLE_CONTEXTS =
            "android.bluetooth.extra.LE_AUDIO_AVAILABLE_CONTEXTS";

    private final BluetoothAdapter mAdapter;
    private final AttributionSource mAttributionSource;
    /**
     * Indicating that group is Active ( Audio device is available )
     * @hide
     */
    public static final int GROUP_STATUS_ACTIVE = IBluetoothLeAudio.GROUP_STATUS_ACTIVE;

    /**
     * Indicating that group is Inactive ( Audio device is not available )
     * @hide
     */
    public static final int GROUP_STATUS_INACTIVE = IBluetoothLeAudio.GROUP_STATUS_INACTIVE;

    private final BluetoothProfileConnector<IBluetoothLeAudio> mProfileConnector =
            new BluetoothProfileConnector(this, BluetoothProfile.LE_AUDIO, "BluetoothLeAudio",
                    IBluetoothLeAudio.class.getName()) {
                @Override
                public IBluetoothLeAudio getServiceInterface(IBinder service) {
                    return IBluetoothLeAudio.Stub.asInterface(service);
                }
    };

    /**
     * Create a BluetoothLeAudio proxy object for interacting with the local
     * Bluetooth LeAudio service.
     */
    /* package */ BluetoothLeAudio(Context context, ServiceListener listener,
            BluetoothAdapter adapter) {
        mAdapter = adapter;
        mAttributionSource = adapter.getAttributionSource();
        mProfileConnector.connect(context, new LeAudioServiceListener(listener));

        mCloseGuard = new CloseGuard();
        mCloseGuard.open("close");
    }

    /** @hide */
    @Override
    public void close() {
        mProfileConnector.disconnect();
    }

    private IBluetoothLeAudio getService() {
        return mProfileConnector.getService();
    }

    protected void finalize() {
        if (mCloseGuard != null) {
            mCloseGuard.warnIfOpen();
        }
        close();
    }

    /**
     * Initiate connection to a profile of the remote bluetooth device.
     *
     * <p> This API returns false in scenarios like the profile on the
     * device is already connected or Bluetooth is not turned on.
     * When this API returns true, it is guaranteed that
     * connection state intent for the profile will be broadcasted with
     * the state. Users can get the connection state of the profile
     * from this intent.
     *
     *
     * @param device Remote Bluetooth Device
     * @return false on immediate error, true otherwise
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean connect(@Nullable BluetoothDevice device) {
        if (DBG) log("connect(" + device + ")");
        final IBluetoothLeAudio service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.connect(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Initiate disconnection from a profile
     *
     * <p> This API will return false in scenarios like the profile on the
     * Bluetooth device is not in connected state etc. When this API returns,
     * true, it is guaranteed that the connection state change
     * intent will be broadcasted with the state. Users can get the
     * disconnection state of the profile from this intent.
     *
     * <p> If the disconnection is initiated by a remote device, the state
     * will transition from {@link #STATE_CONNECTED} to
     * {@link #STATE_DISCONNECTED}. If the disconnect is initiated by the
     * host (local) device the state will transition from
     * {@link #STATE_CONNECTED} to state {@link #STATE_DISCONNECTING} to
     * state {@link #STATE_DISCONNECTED}. The transition to
     * {@link #STATE_DISCONNECTING} can be used to distinguish between the
     * two scenarios.
     *
     *
     * @param device Remote Bluetooth Device
     * @return false on immediate error, true otherwise
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean disconnect(@Nullable BluetoothDevice device) {
        if (DBG) log("disconnect(" + device + ")");
        final IBluetoothLeAudio service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.disconnect(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get Lead device for the group.
     *
     * Lead device is the device that can be used as an active device in the system.
     * Active devices points to the Audio Device for the Le Audio group.
     * This method returns the Lead devices for the connected LE Audio
     * group and this device should be used in the setActiveDevice() method by other parts
     * of the system, which wants to set to active a particular Le Audio group.
     *
     * Note: getActiveDevice() returns the Lead device for the currently active LE Audio group.
     * Note: When Lead device gets disconnected while Le Audio group is active and has more devices
     * in the group, then Lead device will not change. If Lead device gets disconnected, for the
     * Le Audio group which is not active, a new Lead device will be chosen
     *
     * @param groupId The group id.
     * @return group lead device.
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public @Nullable BluetoothDevice getConnectedGroupLeadDevice(int groupId) {
        if (VDBG) log("getConnectedGroupLeadDevice()");
        final IBluetoothLeAudio service = getService();
        final BluetoothDevice defaultValue = null;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled()) {
            try {
                final SynchronousResultReceiver<BluetoothDevice> recv =
                        SynchronousResultReceiver.get();
                service.getConnectedGroupLeadDevice(groupId, mAttributionSource, recv);
                return Attributable.setAttributionSource(
                        recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue),
                        mAttributionSource);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public @NonNull List<BluetoothDevice> getConnectedDevices() {
        if (VDBG) log("getConnectedDevices()");
        final IBluetoothLeAudio service = getService();
        final List<BluetoothDevice> defaultValue = new ArrayList<BluetoothDevice>();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled()) {
            try {
                final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                        SynchronousResultReceiver.get();
                service.getConnectedDevices(mAttributionSource, recv);
                return Attributable.setAttributionSource(
                        recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue),
                        mAttributionSource);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @NonNull
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(
            @NonNull int[] states) {
        if (VDBG) log("getDevicesMatchingStates()");
        final IBluetoothLeAudio service = getService();
        final List<BluetoothDevice> defaultValue = new ArrayList<BluetoothDevice>();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled()) {
            try {
                final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                        SynchronousResultReceiver.get();
                service.getDevicesMatchingConnectionStates(states, mAttributionSource, recv);
                return Attributable.setAttributionSource(
                        recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue),
                        mAttributionSource);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public @BtProfileState int getConnectionState(@NonNull BluetoothDevice device) {
        if (VDBG) log("getState(" + device + ")");
        final IBluetoothLeAudio service = getService();
        final int defaultValue = BluetoothProfile.STATE_DISCONNECTED;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getConnectionState(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Register a {@link Callback} that will be invoked during the
     * operation of this profile.
     *
     * Repeated registration of the same <var>callback</var> object will have no effect after
     * the first call to this method, even when the <var>executor</var> is different. API caller
     * would have to call {@link #unregisterCallback(Callback)} with
     * the same callback object before registering it again.
     *
     * <p> The {@link Callback} will be invoked only if there is codec status changed for the
     * remote device or the device is connected/disconnected in a certain group or the group
     * status is changed.
     *
     * @param executor an {@link Executor} to execute given callback
     * @param callback user implementation of the {@link Callback}
     * @throws NullPointerException if a null executor or callback is given
     * @throws IllegalArgumentException the callback is already registered
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public void registerCallback(@NonNull @CallbackExecutor Executor executor,
            @NonNull Callback callback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(callback, "callback cannot be null");
        if (DBG) log("registerCallback");

        synchronized (mCallbackExecutorMap) {
            // If the callback map is empty, we register the service-to-app callback
            if (mCallbackExecutorMap.isEmpty()) {
                if (!mAdapter.isEnabled()) {
                    /* If Bluetooth is off, just store callback and it will be registered
                     * when Bluetooth is on
                     */
                    mCallbackExecutorMap.put(callback, executor);
                    return;
                }
                try {
                    final IBluetoothLeAudio service = getService();
                    if (service != null) {
                        final SynchronousResultReceiver<Integer> recv =
                                SynchronousResultReceiver.get();
                        service.registerCallback(mCallback, mAttributionSource, recv);
                        recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(null);
                    }
                } catch (TimeoutException e) {
                    Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }

            // Adds the passed in callback to our map of callbacks to executors
            if (mCallbackExecutorMap.containsKey(callback)) {
                throw new IllegalArgumentException("This callback has already been registered");
            }
            mCallbackExecutorMap.put(callback, executor);
        }
    }

    /**
     * Unregister the specified {@link Callback}.
     * <p>The same {@link Callback} object used when calling
     * {@link #registerCallback(Executor, Callback)} must be used.
     *
     * <p>Callbacks are automatically unregistered when application process goes away
     *
     * @param callback user implementation of the {@link Callback}
     * @throws NullPointerException when callback is null
     * @throws IllegalArgumentException when no callback is registered
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public void unregisterCallback(@NonNull Callback callback) {
        Objects.requireNonNull(callback, "callback cannot be null");
        if (DBG) log("unregisterCallback");

        synchronized (mCallbackExecutorMap) {
            if (mCallbackExecutorMap.remove(callback) == null) {
                throw new IllegalArgumentException("This callback has not been registered");
            }
        }

        // If the callback map is empty, we unregister the service-to-app callback
        if (mCallbackExecutorMap.isEmpty()) {
            try {
                final IBluetoothLeAudio service = getService();
                if (service != null) {
                    final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                    service.unregisterCallback(mCallback, mAttributionSource, recv);
                    recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(null);
                }
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Select a connected device as active.
     *
     * The active device selection is per profile. An active device's
     * purpose is profile-specific. For example, LeAudio audio
     * streaming is to the active LeAudio device. If a remote device
     * is not connected, it cannot be selected as active.
     *
     * <p> This API returns false in scenarios like the profile on the
     * device is not connected or Bluetooth is not turned on.
     * When this API returns true, it is guaranteed that the
     * {@link #ACTION_LE_AUDIO_ACTIVE_DEVICE_CHANGED} intent will be broadcasted
     * with the active device.
     *
     *
     * @param device the remote Bluetooth device. Could be null to clear
     * the active device and stop streaming audio to a Bluetooth device.
     * @return false on immediate error, true otherwise
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean setActiveDevice(@Nullable BluetoothDevice device) {
        if (DBG) log("setActiveDevice(" + device + ")");
        final IBluetoothLeAudio service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled() && ((device == null) || isValidDevice(device))) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.setActiveDevice(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get the connected LeAudio devices that are active
     *
     * @return the list of active devices. Returns empty list on error.
     * @hide
     */
    @NonNull
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public List<BluetoothDevice> getActiveDevices() {
        if (VDBG) log("getActiveDevice()");
        final IBluetoothLeAudio service = getService();
        final List<BluetoothDevice> defaultValue = new ArrayList<BluetoothDevice>();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled()) {
            try {
                final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                        SynchronousResultReceiver.get();
                service.getActiveDevices(mAttributionSource, recv);
                return Attributable.setAttributionSource(
                        recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue),
                        mAttributionSource);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get device group id. Devices with same group id belong to same group (i.e left and right
     * earbud)
     * @param device LE Audio capable device
     * @return group id that this device currently belongs to, {@link #GROUP_ID_INVALID} when this
     *         device does not belong to any group
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public int getGroupId(@NonNull BluetoothDevice device) {
        if (VDBG) log("getGroupId()");
        final IBluetoothLeAudio service = getService();
        final int defaultValue = GROUP_ID_INVALID;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled()) {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getGroupId(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Set volume for the streaming devices
     *
     * @param volume volume to set
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    public void setVolume(@IntRange(from = 0, to = 255) int volume) {
        if (VDBG) log("setVolume(vol: " + volume + " )");
        final IBluetoothLeAudio service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled()) {
            try {
                final SynchronousResultReceiver recv = SynchronousResultReceiver.get();
                service.setVolume(volume, mAttributionSource, recv);
                recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(null);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    /**
     * Add device to the given group.
     * @param groupId group ID the device is being added to
     * @param device the active device
     * @return true on success, otherwise false
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    public boolean groupAddNode(int groupId, @NonNull BluetoothDevice device) {
        if (VDBG) log("groupAddNode()");
        final IBluetoothLeAudio service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled()) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.groupAddNode(groupId, device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Remove device from a given group.
     * @param groupId group ID the device is being removed from
     * @param device the active device
     * @return true on success, otherwise false
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    public boolean groupRemoveNode(int groupId, @NonNull BluetoothDevice device) {
        if (VDBG) log("groupRemoveNode()");
        final IBluetoothLeAudio service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled()) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.groupRemoveNode(groupId, device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get the audio location for the device. The return value is a bit field. The bit definition
     * is included in Bluetooth SIG Assigned Numbers - Generic Audio - Audio Location Definitions.
     * ex. Front Left: 0x00000001
     *     Front Right: 0x00000002
     *     Front Left | Front Right: 0x00000003
     *
     * @param device the bluetooth device
     * @return The bit field of audio location for the device, if bluetooth is off, return
     * AUDIO_LOCATION_INVALID.
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    @SystemApi
    public @AudioLocation int getAudioLocation(@NonNull BluetoothDevice device) {
        if (VDBG) log("getAudioLocation()");
        final IBluetoothLeAudio service = getService();
        final int defaultLocation = AUDIO_LOCATION_INVALID;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getAudioLocation(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultLocation);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultLocation;
    }

    /**
     * Check if inband ringtone is enabled by the LE Audio group.
     * Group id for the device can be found with  {@link BluetoothLeAudio#getGroupId}.
     *
     * @param groupId LE Audio group id
     * @return {@code true} if inband ringtone is enabled, {@code false} otherwise
     * @hide
     */
    @RequiresPermission(allOf = {android.Manifest.permission.BLUETOOTH_CONNECT,
                                android.Manifest.permission.BLUETOOTH_PRIVILEGED})
    @SystemApi
    public boolean isInbandRingtoneEnabled(int groupId) {
        if (VDBG) {
            log("isInbandRingtoneEnabled(), groupId: " + groupId);
        }
        final IBluetoothLeAudio service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) {
                log(Log.getStackTraceString(new Throwable()));
            }
        } else if (mAdapter.isEnabled()) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.isInbandRingtoneEnabled(mAttributionSource, recv, groupId);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                e.rethrowFromSystemServer();
            }
        }
        return defaultValue;
    }

    /**
     * Set connection policy of the profile
     *
     * <p> The device should already be paired.
     * Connection policy can be one of {@link #CONNECTION_POLICY_ALLOWED},
     * {@link #CONNECTION_POLICY_FORBIDDEN}, {@link #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Paired bluetooth device
     * @param connectionPolicy is the connection policy to set to for this profile
     * @return true if connectionPolicy is set, false on error
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean setConnectionPolicy(@NonNull BluetoothDevice device,
            @ConnectionPolicy int connectionPolicy) {
        if (DBG) log("setConnectionPolicy(" + device + ", " + connectionPolicy + ")");
        final IBluetoothLeAudio service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled() && isValidDevice(device)
                    && (connectionPolicy == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN
                        || connectionPolicy == BluetoothProfile.CONNECTION_POLICY_ALLOWED)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.setConnectionPolicy(device, connectionPolicy, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get the connection policy of the profile.
     *
     * <p> The connection policy can be any of:
     * {@link #CONNECTION_POLICY_ALLOWED}, {@link #CONNECTION_POLICY_FORBIDDEN},
     * {@link #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Bluetooth device
     * @return connection policy of the device
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    public @ConnectionPolicy int getConnectionPolicy(@Nullable BluetoothDevice device) {
        if (VDBG) log("getConnectionPolicy(" + device + ")");
        final IBluetoothLeAudio service = getService();
        final int defaultValue = BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getConnectionPolicy(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }


    /**
     * Helper for converting a state to a string.
     *
     * For debug use only - strings are not internationalized.
     *
     * @hide
     */
    public static String stateToString(int state) {
        switch (state) {
            case STATE_DISCONNECTED:
                return "disconnected";
            case STATE_CONNECTING:
                return "connecting";
            case STATE_CONNECTED:
                return "connected";
            case STATE_DISCONNECTING:
                return "disconnecting";
            default:
                return "<unknown state " + state + ">";
        }
    }

    private boolean isValidDevice(@Nullable BluetoothDevice device) {
        if (device == null) return false;

        if (BluetoothAdapter.checkBluetoothAddress(device.getAddress())) return true;
        return false;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    /**
     * Gets the current codec status (configuration and capability).
     *
     * @param groupId The group id
     * @return the current codec status
     * @hide
     */
    @SystemApi
    @Nullable
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    public BluetoothLeAudioCodecStatus getCodecStatus(int groupId) {
        if (DBG) {
            Log.d(TAG, "getCodecStatus(" + groupId + ")");
        }

        final IBluetoothLeAudio service = getService();
        final BluetoothLeAudioCodecStatus defaultValue = null;

        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled()) {
            try {
                final SynchronousResultReceiver<BluetoothLeAudioCodecStatus> recv =
                        SynchronousResultReceiver.get();
                service.getCodecStatus(groupId, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                e.rethrowFromSystemServer();
            }
        }
        return defaultValue;
    }

    /**
     * Sets the codec configuration preference.
     *
     * @param groupId the groupId
     * @param inputCodecConfig the input codec configuration preference
     * @param outputCodecConfig the output codec configuration preference
     * @throws IllegalStateException if LE Audio Service is null
     * @throws NullPointerException if any of the configs is null
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    public void setCodecConfigPreference(int groupId,
                                         @NonNull BluetoothLeAudioCodecConfig inputCodecConfig,
                                         @NonNull BluetoothLeAudioCodecConfig outputCodecConfig) {
        if (DBG) Log.d(TAG, "setCodecConfigPreference(" + groupId + ")");

        Objects.requireNonNull(inputCodecConfig, " inputCodecConfig shall not be null");
        Objects.requireNonNull(outputCodecConfig, " outputCodecConfig shall not be null");

        final IBluetoothLeAudio service = getService();

        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
            throw new IllegalStateException("Service is unavailable");
        } else if (mAdapter.isEnabled()) {
            try {
                service.setCodecConfigPreference(groupId, inputCodecConfig, outputCodecConfig,
                                        mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                e.rethrowFromSystemServer();
            }
        }
    }

}
