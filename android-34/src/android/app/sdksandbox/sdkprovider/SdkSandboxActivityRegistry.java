/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.app.sdksandbox.sdkprovider;

import android.annotation.NonNull;
import android.app.Activity;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.ArrayMap;

import androidx.annotation.RequiresApi;

import com.android.internal.annotations.GuardedBy;

import java.util.Map;

/**
 * It is a Singleton class to store the registered {@link SdkSandboxActivityHandler} instances and
 * their associated {@link Activity} instances.
 *
 * @hide
 */
public class SdkSandboxActivityRegistry {
    private static final Object sLock = new Object();

    @GuardedBy("sLock")
    private static SdkSandboxActivityRegistry sInstance;

    // A lock to keep all map synchronized
    private final Object mMapsLock = new Object();

    @GuardedBy("mMapsLock")
    private final Map<SdkSandboxActivityHandler, HandlerInfo> mHandlerToHandlerInfoMap =
            new ArrayMap<>();

    @GuardedBy("mMapsLock")
    private final Map<IBinder, HandlerInfo> mTokenToHandlerInfoMap = new ArrayMap<>();

    private SdkSandboxActivityRegistry() {}

    /** Returns a singleton instance of this class. */
    public static SdkSandboxActivityRegistry getInstance() {
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new SdkSandboxActivityRegistry();
            }
            return sInstance;
        }
    }

    /**
     * Registers the passed {@link SdkSandboxActivityHandler} and returns a {@link IBinder} token
     * that identifies it.
     *
     * <p>If {@link SdkSandboxActivityHandler} is already registered, its {@link IBinder} identifier
     * will be returned.
     *
     * @param sdkName is the name of the SDK registering {@link SdkSandboxActivityHandler}
     * @param handler is the {@link SdkSandboxActivityHandler} to register.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @NonNull
    public IBinder register(@NonNull String sdkName, @NonNull SdkSandboxActivityHandler handler) {
        synchronized (mMapsLock) {
            if (mHandlerToHandlerInfoMap.containsKey(handler)) {
                HandlerInfo handlerInfo = mHandlerToHandlerInfoMap.get(handler);
                return handlerInfo.getToken();
            }

            IBinder token = new Binder();
            HandlerInfo handlerInfo = new HandlerInfo(sdkName, handler, token);
            mHandlerToHandlerInfoMap.put(handlerInfo.getHandler(), handlerInfo);
            mTokenToHandlerInfoMap.put(handlerInfo.getToken(), handlerInfo);
            return token;
        }
    }

    /**
     * Unregisters the passed {@link SdkSandboxActivityHandler}.
     *
     * @param handler is the {@link SdkSandboxActivityHandler} to unregister.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void unregister(@NonNull SdkSandboxActivityHandler handler) {
        synchronized (mMapsLock) {
            HandlerInfo handlerInfo = mHandlerToHandlerInfoMap.get(handler);
            if (handlerInfo == null) {
                return;
            }
            mHandlerToHandlerInfoMap.remove(handlerInfo.getHandler());
            mTokenToHandlerInfoMap.remove(handlerInfo.getToken());
        }
    }

    /**
     * It notifies the SDK about {@link Activity} creation.
     *
     * <p>This should be called by the sandbox {@link Activity} while being created to notify the
     * SDK that registered the {@link SdkSandboxActivityHandler} that identified by the passed
     * {@link IBinder} token.
     *
     * @param token is the {@link IBinder} identifier for the {@link SdkSandboxActivityHandler}.
     * @param activity is the {@link Activity} is being created.
     * @throws IllegalArgumentException if there is no registered handler identified by the passed
     *     {@link IBinder} token (that mostly would mean that the handler is de-registered before
     *     the passed {@link Activity} is created), or the {@link SdkSandboxActivityHandler} is
     *     already notified about a previous {@link Activity}, in both cases the passed {@link
     *     Activity} will not start.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void notifyOnActivityCreation(@NonNull IBinder token, @NonNull Activity activity) {
        synchronized (mMapsLock) {
            HandlerInfo handlerInfo = mTokenToHandlerInfoMap.get(token);
            if (handlerInfo == null) {
                throw new IllegalArgumentException(
                        "There is no registered SdkSandboxActivityHandler to notify");
            }
            handlerInfo.getHandler().onActivityCreated(activity);
        }
    }

    /**
     * Holds the information about {@link SdkSandboxActivityHandler}.
     *
     * @hide
     */
    private static class HandlerInfo {
        private final String mSdkName;
        private final SdkSandboxActivityHandler mHandler;
        private final IBinder mToken;


        HandlerInfo(String sdkName, SdkSandboxActivityHandler handler, IBinder token) {
            this.mSdkName = sdkName;
            this.mHandler = handler;
            this.mToken = token;
        }

        @NonNull
        public String getSdkName() {
            return mSdkName;
        }

        @NonNull
        public SdkSandboxActivityHandler getHandler() {
            return mHandler;
        }

        @NonNull
        public IBinder getToken() {
            return mToken;
        }
    }
}
