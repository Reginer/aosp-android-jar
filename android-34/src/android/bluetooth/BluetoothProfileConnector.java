/*
 * Copyright 2019 The Android Open Source Project
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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.CloseGuard;
import android.util.Log;

import java.util.List;
/**
 * Connector for Bluetooth profile proxies to bind manager service and
 * profile services
 * @param <T> The Bluetooth profile interface for this connection.
 * @hide
 */
@SuppressLint("AndroidFrameworkBluetoothPermission")
public abstract class BluetoothProfileConnector<T> {
    private final CloseGuard mCloseGuard = new CloseGuard();
    private final int mProfileId;
    private BluetoothProfile.ServiceListener mServiceListener;
    private final BluetoothProfile mProfileProxy;
    private Context mContext;
    private final String mProfileName;
    private final String mServiceName;
    private volatile T mService;

    // -3 match with UserHandle.USER_CURRENT_OR_SELF
    private static final UserHandle USER_HANDLE_CURRENT_OR_SELF = UserHandle.of(-3);

    private static final int MESSAGE_SERVICE_CONNECTED = 100;
    private static final int MESSAGE_SERVICE_DISCONNECTED = 101;

    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback =
            new IBluetoothStateChangeCallback.Stub() {
        public void onBluetoothStateChange(boolean up) {
            if (up) {
                doBind();
            } else {
                doUnbind();
            }
        }
    };

    private @Nullable ComponentName resolveSystemService(@NonNull Intent intent,
            @NonNull PackageManager pm) {
        List<ResolveInfo> results = pm.queryIntentServices(intent,
                PackageManager.ResolveInfoFlags.of(0));
        if (results == null) {
            return null;
        }
        ComponentName comp = null;
        for (int i = 0; i < results.size(); i++) {
            ResolveInfo ri = results.get(i);
            if ((ri.serviceInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                continue;
            }
            ComponentName foundComp = new ComponentName(ri.serviceInfo.applicationInfo.packageName,
                    ri.serviceInfo.name);
            if (comp != null) {
                throw new IllegalStateException("Multiple system services handle " + intent
                        + ": " + comp + ", " + foundComp);
            }
            comp = foundComp;
        }
        return comp;
    }

    private final IBluetoothProfileServiceConnection mConnection =
            new IBluetoothProfileServiceConnection.Stub() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            logDebug("Proxy object connected");
            mService = getServiceInterface(service);
            mHandler.sendMessage(mHandler.obtainMessage(
                    MESSAGE_SERVICE_CONNECTED));
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            logDebug("Proxy object disconnected");
            doUnbind();
            mHandler.sendMessage(mHandler.obtainMessage(
                    MESSAGE_SERVICE_DISCONNECTED));
        }
    };

    BluetoothProfileConnector(BluetoothProfile profile, int profileId, String profileName,
            String serviceName) {
        mProfileId = profileId;
        mProfileProxy = profile;
        mProfileName = profileName;
        mServiceName = serviceName;
    }

    /** {@hide} */
    @Override
    public void finalize() {
        mCloseGuard.warnIfOpen();
        doUnbind();
    }

    private boolean doBind() {
        synchronized (mConnection) {
            if (mService == null) {
                logDebug("Binding service...");
                mCloseGuard.open("doUnbind");
                try {
                    return BluetoothAdapter.getDefaultAdapter().getBluetoothManager()
                            .bindBluetoothProfileService(mProfileId, mServiceName, mConnection);
                } catch (RemoteException re) {
                    logError("Failed to bind service. " + re);
                    return false;
                }
            }
        }
        return true;
    }

    private void doUnbind() {
        synchronized (mConnection) {
            if (mService != null) {
                logDebug("Unbinding service...");
                mCloseGuard.close();
                try {
                    BluetoothAdapter.getDefaultAdapter().getBluetoothManager()
                            .unbindBluetoothProfileService(mProfileId, mConnection);
                } catch (RemoteException re) {
                    logError("Unable to unbind service: " + re);
                } finally {
                    mService = null;
                }
            }
        }
    }

    void connect(Context context, BluetoothProfile.ServiceListener listener) {
        mContext = context;
        mServiceListener = listener;
        IBluetoothManager mgr = BluetoothAdapter.getDefaultAdapter().getBluetoothManager();

        // Preserve legacy compatibility where apps were depending on
        // registerStateChangeCallback() performing a permissions check which
        // has been relaxed in modern platform versions
        if (context.getApplicationInfo().targetSdkVersion <= Build.VERSION_CODES.R
                && context.checkSelfPermission(android.Manifest.permission.BLUETOOTH)
                        != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Need BLUETOOTH permission");
        }

        if (mgr != null) {
            try {
                mgr.registerStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (RemoteException re) {
                logError("Failed to register state change callback. " + re);
            }
        }
        doBind();
    }

    void disconnect() {
        if (mServiceListener != null) {
            BluetoothProfile.ServiceListener listener = mServiceListener;
            mServiceListener = null;
            listener.onServiceDisconnected(mProfileId);
        }
        IBluetoothManager mgr = BluetoothAdapter.getDefaultAdapter().getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.unregisterStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (RemoteException re) {
                logError("Failed to unregister state change callback" + re);
            }
        }
        doUnbind();
    }

    T getService() {
        return mService;
    }

    /**
     * This abstract function is used to implement method to get the
     * connected Bluetooth service interface.
     * @param service the connected binder service.
     * @return T the binder interface of {@code service}.
     * @hide
     */
    public abstract T getServiceInterface(IBinder service);

    private void logDebug(String log) {
        Log.d(mProfileName, log);
    }

    private void logError(String log) {
        Log.e(mProfileName, log);
    }

    @SuppressLint("AndroidFrameworkBluetoothPermission")
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SERVICE_CONNECTED: {
                    if (mServiceListener != null) {
                        mServiceListener.onServiceConnected(mProfileId, mProfileProxy);
                    }
                    break;
                }
                case MESSAGE_SERVICE_DISCONNECTED: {
                    if (mServiceListener != null) {
                        mServiceListener.onServiceDisconnected(mProfileId);
                    }
                    break;
                }
            }
        }
    };
}
