/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.server.ethernet;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.IEthernetManager;
import android.net.IEthernetServiceListener;
import android.net.ITetheredInterfaceCallback;
import android.net.IpConfiguration;
import android.net.NetworkStack;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.util.PrintWriterPrinter;

import com.android.internal.util.IndentingPrintWriter;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * EthernetServiceImpl handles remote Ethernet operation requests by implementing
 * the IEthernetManager interface.
 */
public class EthernetServiceImpl extends IEthernetManager.Stub {
    private static final String TAG = "EthernetServiceImpl";

    private final Context mContext;
    private final AtomicBoolean mStarted = new AtomicBoolean(false);

    private Handler mHandler;
    private EthernetTracker mTracker;

    public EthernetServiceImpl(Context context) {
        mContext = context;
    }

    private void enforceAccessPermission() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_NETWORK_STATE,
                "EthernetService");
    }

    private void enforceUseRestrictedNetworksPermission() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.CONNECTIVITY_USE_RESTRICTED_NETWORKS,
                "ConnectivityService");
    }

    private boolean checkUseRestrictedNetworksPermission() {
        return mContext.checkCallingOrSelfPermission(
                android.Manifest.permission.CONNECTIVITY_USE_RESTRICTED_NETWORKS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void start() {
        Log.i(TAG, "Starting Ethernet service");

        HandlerThread handlerThread = new HandlerThread("EthernetServiceThread");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());

        mTracker = new EthernetTracker(mContext, mHandler);
        mTracker.start();

        mStarted.set(true);
    }

    @Override
    public String[] getAvailableInterfaces() throws RemoteException {
        enforceAccessPermission();

        return mTracker.getInterfaces(checkUseRestrictedNetworksPermission());
    }

    /**
     * Get Ethernet configuration
     * @return the Ethernet Configuration, contained in {@link IpConfiguration}.
     */
    @Override
    public IpConfiguration getConfiguration(String iface) {
        enforceAccessPermission();

        if (mTracker.isRestrictedInterface(iface)) {
            enforceUseRestrictedNetworksPermission();
        }

        return new IpConfiguration(mTracker.getIpConfiguration(iface));
    }

    /**
     * Set Ethernet configuration
     */
    @Override
    public void setConfiguration(String iface, IpConfiguration config) {
        if (!mStarted.get()) {
            Log.w(TAG, "System isn't ready enough to change ethernet configuration");
        }

        NetworkStack.checkNetworkStackPermission(mContext);

        if (mTracker.isRestrictedInterface(iface)) {
            enforceUseRestrictedNetworksPermission();
        }

        // TODO: this does not check proxy settings, gateways, etc.
        // Fix this by making IpConfiguration a complete representation of static configuration.
        mTracker.updateIpConfiguration(iface, new IpConfiguration(config));
    }

    /**
     * Indicates whether given interface is available.
     */
    @Override
    public boolean isAvailable(String iface) {
        enforceAccessPermission();

        if (mTracker.isRestrictedInterface(iface)) {
            enforceUseRestrictedNetworksPermission();
        }

        return mTracker.isTrackingInterface(iface);
    }

    /**
     * Adds a listener.
     * @param listener A {@link IEthernetServiceListener} to add.
     */
    public void addListener(IEthernetServiceListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        enforceAccessPermission();
        mTracker.addListener(listener, checkUseRestrictedNetworksPermission());
    }

    /**
     * Removes a listener.
     * @param listener A {@link IEthernetServiceListener} to remove.
     */
    public void removeListener(IEthernetServiceListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        enforceAccessPermission();
        mTracker.removeListener(listener);
    }

    @Override
    public void setIncludeTestInterfaces(boolean include) {
        NetworkStack.checkNetworkStackPermissionOr(mContext,
                android.Manifest.permission.NETWORK_SETTINGS);
        mTracker.setIncludeTestInterfaces(include);
    }

    @Override
    public void requestTetheredInterface(ITetheredInterfaceCallback callback) {
        NetworkStack.checkNetworkStackPermissionOr(mContext,
                android.Manifest.permission.NETWORK_SETTINGS);
        mTracker.requestTetheredInterface(callback);
    }

    @Override
    public void releaseTetheredInterface(ITetheredInterfaceCallback callback) {
        NetworkStack.checkNetworkStackPermissionOr(mContext,
                android.Manifest.permission.NETWORK_SETTINGS);
        mTracker.releaseTetheredInterface(callback);
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        final IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.DUMP)
                != PackageManager.PERMISSION_GRANTED) {
            pw.println("Permission Denial: can't dump EthernetService from pid="
                    + Binder.getCallingPid()
                    + ", uid=" + Binder.getCallingUid());
            return;
        }

        pw.println("Current Ethernet state: ");
        pw.increaseIndent();
        mTracker.dump(fd, pw, args);
        pw.decreaseIndent();

        pw.println("Handler:");
        pw.increaseIndent();
        mHandler.dump(new PrintWriterPrinter(pw), "EthernetServiceImpl");
        pw.decreaseIndent();
    }
}
