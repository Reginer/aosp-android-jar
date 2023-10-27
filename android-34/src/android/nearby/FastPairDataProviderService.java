/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.nearby;

import android.accounts.Account;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Service;
import android.content.Intent;
import android.nearby.aidl.ByteArrayParcel;
import android.nearby.aidl.FastPairAccountDevicesMetadataRequestParcel;
import android.nearby.aidl.FastPairAccountKeyDeviceMetadataParcel;
import android.nearby.aidl.FastPairAntispoofKeyDeviceMetadataRequestParcel;
import android.nearby.aidl.FastPairEligibleAccountParcel;
import android.nearby.aidl.FastPairEligibleAccountsRequestParcel;
import android.nearby.aidl.FastPairManageAccountDeviceRequestParcel;
import android.nearby.aidl.FastPairManageAccountRequestParcel;
import android.nearby.aidl.IFastPairAccountDevicesMetadataCallback;
import android.nearby.aidl.IFastPairAntispoofKeyDeviceMetadataCallback;
import android.nearby.aidl.IFastPairDataProvider;
import android.nearby.aidl.IFastPairEligibleAccountsCallback;
import android.nearby.aidl.IFastPairManageAccountCallback;
import android.nearby.aidl.IFastPairManageAccountDeviceCallback;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A service class for fast pair data providers outside the system server.
 *
 * Fast pair providers should be wrapped in a non-exported service which returns the result of
 * {@link #getBinder()} from the service's {@link android.app.Service#onBind(Intent)} method. The
 * service should not be exported so that components other than the system server cannot bind to it.
 * Alternatively, the service may be guarded by a permission that only system server can obtain.
 *
 * <p>Fast Pair providers are identified by their UID / package name.
 *
 * @hide
 */
public abstract class FastPairDataProviderService extends Service {
    /**
     * The action the wrapping service should have in its intent filter to implement the
     * {@link android.nearby.FastPairDataProviderBase}.
     *
     * @hide
     */
    public static final String ACTION_FAST_PAIR_DATA_PROVIDER =
            "android.nearby.action.FAST_PAIR_DATA_PROVIDER";

    /**
     * Manage request type to add, or opt-in.
     *
     * @hide
     */
    public static final int MANAGE_REQUEST_ADD = 0;

    /**
     * Manage request type to remove, or opt-out.
     *
     * @hide
     */
    public static final int MANAGE_REQUEST_REMOVE = 1;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            MANAGE_REQUEST_ADD,
            MANAGE_REQUEST_REMOVE})
    @interface ManageRequestType {}

    /**
     * Error code for bad request.
     *
     * @hide
     */
    public static final int ERROR_CODE_BAD_REQUEST = 0;

    /**
     * Error code for internal error.
     *
     * @hide
     */
    public static final int ERROR_CODE_INTERNAL_ERROR = 1;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            ERROR_CODE_BAD_REQUEST,
            ERROR_CODE_INTERNAL_ERROR})
    @interface ErrorCode {}

    private final IBinder mBinder;
    private final String mTag;

    /**
     * Constructor of FastPairDataProviderService.
     *
     * @param tag TAG for on device logging.
     * @hide
     */
    public FastPairDataProviderService(@NonNull String tag) {
        mBinder = new Service();
        mTag = tag;
    }

    @Override
    @NonNull
    public final IBinder onBind(@NonNull Intent intent) {
        return mBinder;
    }

    /**
     * Callback to be invoked when an AntispoofKeyed device metadata is loaded.
     *
     * @hide
     */
    public interface FastPairAntispoofKeyDeviceMetadataCallback {

        /**
         * Invoked once the meta data is loaded.
         *
         * @hide
         */
        void onFastPairAntispoofKeyDeviceMetadataReceived(
                @NonNull FastPairAntispoofKeyDeviceMetadata metadata);

        /** Invoked in case of error.
         *
         * @hide
         */
        void onError(@ErrorCode int code, @Nullable String message);
    }

    /**
     * Callback to be invoked when Fast Pair devices of a given account is loaded.
     *
     * @hide
     */
    public interface FastPairAccountDevicesMetadataCallback {

        /**
         * Should be invoked once the metadatas are loaded.
         *
         * @hide
         */
        void onFastPairAccountDevicesMetadataReceived(
                @NonNull Collection<FastPairAccountKeyDeviceMetadata> metadatas);
        /**
         * Invoked in case of error.
         *
         * @hide
         */
        void onError(@ErrorCode int code, @Nullable String message);
    }

    /**
     * Callback to be invoked when FastPair eligible accounts are loaded.
     *
     * @hide
     */
    public interface FastPairEligibleAccountsCallback {

        /**
         * Should be invoked once the eligible accounts are loaded.
         *
         * @hide
         */
        void onFastPairEligibleAccountsReceived(
                @NonNull Collection<FastPairEligibleAccount> accounts);
        /**
         * Invoked in case of error.
         *
         * @hide
         */
        void onError(@ErrorCode int code, @Nullable String message);
    }

    /**
     * Callback to be invoked when a management action is finished.
     *
     * @hide
     */
    public interface FastPairManageActionCallback {

        /**
         * Should be invoked once the manage action is successful.
         *
         * @hide
         */
        void onSuccess();
        /**
         * Invoked in case of error.
         *
         * @hide
         */
        void onError(@ErrorCode int code, @Nullable String message);
    }

    /**
     * Fulfills the Fast Pair device metadata request by using callback to send back the
     * device meta data of a given modelId.
     *
     * @hide
     */
    public abstract void onLoadFastPairAntispoofKeyDeviceMetadata(
            @NonNull FastPairAntispoofKeyDeviceMetadataRequest request,
            @NonNull FastPairAntispoofKeyDeviceMetadataCallback callback);

    /**
     * Fulfills the account tied Fast Pair devices metadata request by using callback to send back
     * all Fast Pair device's metadata of a given account.
     *
     * @hide
     */
    public abstract void onLoadFastPairAccountDevicesMetadata(
            @NonNull FastPairAccountDevicesMetadataRequest request,
            @NonNull FastPairAccountDevicesMetadataCallback callback);

    /**
     * Fulfills the Fast Pair eligible accounts request by using callback to send back Fast Pair
     * eligible accounts.
     *
     * @hide
     */
    public abstract void onLoadFastPairEligibleAccounts(
            @NonNull FastPairEligibleAccountsRequest request,
            @NonNull FastPairEligibleAccountsCallback callback);

    /**
     * Fulfills the Fast Pair account management request by using callback to send back result.
     *
     * @hide
     */
    public abstract void onManageFastPairAccount(
            @NonNull FastPairManageAccountRequest request,
            @NonNull FastPairManageActionCallback callback);

    /**
     * Fulfills the request to manage device-account mapping by using callback to send back result.
     *
     * @hide
     */
    public abstract void onManageFastPairAccountDevice(
            @NonNull FastPairManageAccountDeviceRequest request,
            @NonNull FastPairManageActionCallback callback);

    /**
     * Class for reading FastPairAntispoofKeyDeviceMetadataRequest, which specifies the model ID of
     * a Fast Pair device. To fulfill this request, corresponding
     * {@link FastPairAntispoofKeyDeviceMetadata} should be fetched and returned.
     *
     * @hide
     */
    public static class FastPairAntispoofKeyDeviceMetadataRequest {

        private final FastPairAntispoofKeyDeviceMetadataRequestParcel mMetadataRequestParcel;

        private FastPairAntispoofKeyDeviceMetadataRequest(
                final FastPairAntispoofKeyDeviceMetadataRequestParcel metaDataRequestParcel) {
            this.mMetadataRequestParcel = metaDataRequestParcel;
        }

        /**
         * Get modelId (24 bit), the key for FastPairAntispoofKeyDeviceMetadata in the same format
         * returned by Google at device registration time.
         *
         * ModelId format is defined at device registration time, see
         * <a href="https://developers.google.com/nearby/fast-pair/spec#model_id">Model ID</a>.
         * @return raw bytes of modelId in the same format returned by Google at device registration
         *         time.
         * @hide
         */
        public @NonNull byte[] getModelId() {
            return this.mMetadataRequestParcel.modelId;
        }
    }

    /**
     * Class for reading FastPairAccountDevicesMetadataRequest, which specifies the Fast Pair
     * account and the allow list of the FastPair device keys saved to the account (i.e., FastPair
     * accountKeys).
     *
     * A Fast Pair accountKey is created when a Fast Pair device is saved to an account. It is per
     * Fast Pair device per account.
     *
     * To retrieve all Fast Pair accountKeys saved to an account, the caller needs to set
     * account with an empty allow list.
     *
     * To retrieve metadata of a selected list of Fast Pair devices saved to an account, the caller
     * needs to set account with a non-empty allow list.
     * @hide
     */
    public static class FastPairAccountDevicesMetadataRequest {

        private final FastPairAccountDevicesMetadataRequestParcel mMetadataRequestParcel;

        private FastPairAccountDevicesMetadataRequest(
                final FastPairAccountDevicesMetadataRequestParcel metaDataRequestParcel) {
            this.mMetadataRequestParcel = metaDataRequestParcel;
        }

        /**
         * Get FastPair account, whose Fast Pair devices' metadata is requested.
         *
         * @return a FastPair account.
         * @hide
         */
        public @NonNull Account getAccount() {
            return this.mMetadataRequestParcel.account;
        }

        /**
         * Get allowlist of Fast Pair devices using a collection of deviceAccountKeys.
         * Note that as a special case, empty list actually means all FastPair devices under the
         * account instead of none.
         *
         * DeviceAccountKey is 16 bytes: first byte is 0x04. Other 15 bytes are randomly generated.
         *
         * @return allowlist of Fast Pair devices using a collection of deviceAccountKeys.
         * @hide
         */
        public @NonNull Collection<byte[]> getDeviceAccountKeys()  {
            if (this.mMetadataRequestParcel.deviceAccountKeys == null) {
                return new ArrayList<byte[]>(0);
            }
            List<byte[]> deviceAccountKeys =
                    new ArrayList<>(this.mMetadataRequestParcel.deviceAccountKeys.length);
            for (ByteArrayParcel deviceAccountKey : this.mMetadataRequestParcel.deviceAccountKeys) {
                deviceAccountKeys.add(deviceAccountKey.byteArray);
            }
            return deviceAccountKeys;
        }
    }

    /**
     *  Class for reading FastPairEligibleAccountsRequest. Upon receiving this request, Fast Pair
     *  eligible accounts should be returned to bind Fast Pair devices.
     *
     * @hide
     */
    public static class FastPairEligibleAccountsRequest {
        @SuppressWarnings("UnusedVariable")
        private final FastPairEligibleAccountsRequestParcel mAccountsRequestParcel;

        private FastPairEligibleAccountsRequest(
                final FastPairEligibleAccountsRequestParcel accountsRequestParcel) {
            this.mAccountsRequestParcel = accountsRequestParcel;
        }
    }

    /**
     * Class for reading FastPairManageAccountRequest. If the request type is MANAGE_REQUEST_ADD,
     * the account is enabled to bind Fast Pair devices; If the request type is
     * MANAGE_REQUEST_REMOVE, the account is disabled to bind more Fast Pair devices. Furthermore,
     * all existing bounded Fast Pair devices are unbounded.
     *
     * @hide
     */
    public static class FastPairManageAccountRequest {

        private final FastPairManageAccountRequestParcel mAccountRequestParcel;

        private FastPairManageAccountRequest(
                final FastPairManageAccountRequestParcel accountRequestParcel) {
            this.mAccountRequestParcel = accountRequestParcel;
        }

        /**
         * Get request type: MANAGE_REQUEST_ADD, or MANAGE_REQUEST_REMOVE.
         *
         * @hide
         */
        public @ManageRequestType int getRequestType() {
            return this.mAccountRequestParcel.requestType;
        }
        /**
         * Get account.
         *
         * @hide
         */
        public @NonNull Account getAccount() {
            return this.mAccountRequestParcel.account;
        }
    }

    /**
     *  Class for reading FastPairManageAccountDeviceRequest. If the request type is
     *  MANAGE_REQUEST_ADD, then a Fast Pair device is bounded to a Fast Pair account. If the
     *  request type is MANAGE_REQUEST_REMOVE, then a Fast Pair device is removed from a Fast Pair
     *  account.
     *
     * @hide
     */
    public static class FastPairManageAccountDeviceRequest {

        private final FastPairManageAccountDeviceRequestParcel mRequestParcel;

        private FastPairManageAccountDeviceRequest(
                final FastPairManageAccountDeviceRequestParcel requestParcel) {
            this.mRequestParcel = requestParcel;
        }

        /**
         * Get request type: MANAGE_REQUEST_ADD, or MANAGE_REQUEST_REMOVE.
         *
         * @hide
         */
        public @ManageRequestType int getRequestType() {
            return this.mRequestParcel.requestType;
        }
        /**
         * Get account.
         *
         * @hide
         */
        public @NonNull Account getAccount() {
            return this.mRequestParcel.account;
        }
        /**
         * Get account key device metadata.
         *
         * @hide
         */
        public @NonNull FastPairAccountKeyDeviceMetadata getAccountKeyDeviceMetadata() {
            return new FastPairAccountKeyDeviceMetadata(
                    this.mRequestParcel.accountKeyDeviceMetadata);
        }
    }

    /**
     * Callback class that sends back FastPairAntispoofKeyDeviceMetadata.
     */
    private final class WrapperFastPairAntispoofKeyDeviceMetadataCallback implements
            FastPairAntispoofKeyDeviceMetadataCallback {

        private IFastPairAntispoofKeyDeviceMetadataCallback mCallback;

        private WrapperFastPairAntispoofKeyDeviceMetadataCallback(
                IFastPairAntispoofKeyDeviceMetadataCallback callback) {
            mCallback = callback;
        }

        /**
         * Sends back FastPairAntispoofKeyDeviceMetadata.
         */
        @Override
        public void onFastPairAntispoofKeyDeviceMetadataReceived(
                @NonNull FastPairAntispoofKeyDeviceMetadata metadata) {
            try {
                mCallback.onFastPairAntispoofKeyDeviceMetadataReceived(metadata.mMetadataParcel);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (RuntimeException e) {
                Log.w(mTag, e);
            }
        }

        @Override
        public void onError(@ErrorCode int code, @Nullable String message) {
            try {
                mCallback.onError(code, message);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (RuntimeException e) {
                Log.w(mTag, e);
            }
        }
    }

    /**
     * Callback class that sends back collection of FastPairAccountKeyDeviceMetadata.
     */
    private final class WrapperFastPairAccountDevicesMetadataCallback implements
            FastPairAccountDevicesMetadataCallback {

        private IFastPairAccountDevicesMetadataCallback mCallback;

        private WrapperFastPairAccountDevicesMetadataCallback(
                IFastPairAccountDevicesMetadataCallback callback) {
            mCallback = callback;
        }

        /**
         * Sends back collection of FastPairAccountKeyDeviceMetadata.
         */
        @Override
        public void onFastPairAccountDevicesMetadataReceived(
                @NonNull Collection<FastPairAccountKeyDeviceMetadata> metadatas) {
            FastPairAccountKeyDeviceMetadataParcel[] metadataParcels =
                    new FastPairAccountKeyDeviceMetadataParcel[metadatas.size()];
            int i = 0;
            for (FastPairAccountKeyDeviceMetadata metadata : metadatas) {
                metadataParcels[i] = metadata.mMetadataParcel;
                i = i + 1;
            }
            try {
                mCallback.onFastPairAccountDevicesMetadataReceived(metadataParcels);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (RuntimeException e) {
                Log.w(mTag, e);
            }
        }

        @Override
        public void onError(@ErrorCode int code, @Nullable String message) {
            try {
                mCallback.onError(code, message);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (RuntimeException e) {
                Log.w(mTag, e);
            }
        }
    }

    /**
     * Callback class that sends back eligible Fast Pair accounts.
     */
    private final class WrapperFastPairEligibleAccountsCallback implements
            FastPairEligibleAccountsCallback {

        private IFastPairEligibleAccountsCallback mCallback;

        private WrapperFastPairEligibleAccountsCallback(
                IFastPairEligibleAccountsCallback callback) {
            mCallback = callback;
        }

        /**
         * Sends back the eligible Fast Pair accounts.
         */
        @Override
        public void onFastPairEligibleAccountsReceived(
                @NonNull Collection<FastPairEligibleAccount> accounts) {
            int i = 0;
            FastPairEligibleAccountParcel[] accountParcels =
                    new FastPairEligibleAccountParcel[accounts.size()];
            for (FastPairEligibleAccount account: accounts) {
                accountParcels[i] = account.mAccountParcel;
                i = i + 1;
            }
            try {
                mCallback.onFastPairEligibleAccountsReceived(accountParcels);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (RuntimeException e) {
                Log.w(mTag, e);
            }
        }

        @Override
        public void onError(@ErrorCode int code, @Nullable String message) {
            try {
                mCallback.onError(code, message);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (RuntimeException e) {
                Log.w(mTag, e);
            }
        }
    }

    /**
     * Callback class that sends back Fast Pair account management result.
     */
    private final class WrapperFastPairManageAccountCallback implements
            FastPairManageActionCallback {

        private IFastPairManageAccountCallback mCallback;

        private WrapperFastPairManageAccountCallback(
                IFastPairManageAccountCallback callback) {
            mCallback = callback;
        }

        /**
         * Sends back Fast Pair account opt in result.
         */
        @Override
        public void onSuccess() {
            try {
                mCallback.onSuccess();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (RuntimeException e) {
                Log.w(mTag, e);
            }
        }

        @Override
        public void onError(@ErrorCode int code, @Nullable String message) {
            try {
                mCallback.onError(code, message);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (RuntimeException e) {
                Log.w(mTag, e);
            }
        }
    }

    /**
     * Call back class that sends back account-device mapping management result.
     */
    private final class WrapperFastPairManageAccountDeviceCallback implements
            FastPairManageActionCallback {

        private IFastPairManageAccountDeviceCallback mCallback;

        private WrapperFastPairManageAccountDeviceCallback(
                IFastPairManageAccountDeviceCallback callback) {
            mCallback = callback;
        }

        /**
         * Sends back the account-device mapping management result.
         */
        @Override
        public void onSuccess() {
            try {
                mCallback.onSuccess();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (RuntimeException e) {
                Log.w(mTag, e);
            }
        }

        @Override
        public void onError(@ErrorCode int code, @Nullable String message) {
            try {
                mCallback.onError(code, message);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (RuntimeException e) {
                Log.w(mTag, e);
            }
        }
    }

    private final class Service extends IFastPairDataProvider.Stub {

        Service() {
        }

        @Override
        public void loadFastPairAntispoofKeyDeviceMetadata(
                @NonNull FastPairAntispoofKeyDeviceMetadataRequestParcel requestParcel,
                IFastPairAntispoofKeyDeviceMetadataCallback callback) {
            onLoadFastPairAntispoofKeyDeviceMetadata(
                    new FastPairAntispoofKeyDeviceMetadataRequest(requestParcel),
                    new WrapperFastPairAntispoofKeyDeviceMetadataCallback(callback));
        }

        @Override
        public void loadFastPairAccountDevicesMetadata(
                @NonNull FastPairAccountDevicesMetadataRequestParcel requestParcel,
                IFastPairAccountDevicesMetadataCallback callback) {
            onLoadFastPairAccountDevicesMetadata(
                    new FastPairAccountDevicesMetadataRequest(requestParcel),
                    new WrapperFastPairAccountDevicesMetadataCallback(callback));
        }

        @Override
        public void loadFastPairEligibleAccounts(
                @NonNull FastPairEligibleAccountsRequestParcel requestParcel,
                IFastPairEligibleAccountsCallback callback) {
            onLoadFastPairEligibleAccounts(new FastPairEligibleAccountsRequest(requestParcel),
                    new WrapperFastPairEligibleAccountsCallback(callback));
        }

        @Override
        public void manageFastPairAccount(
                @NonNull FastPairManageAccountRequestParcel requestParcel,
                IFastPairManageAccountCallback callback) {
            onManageFastPairAccount(new FastPairManageAccountRequest(requestParcel),
                    new WrapperFastPairManageAccountCallback(callback));
        }

        @Override
        public void manageFastPairAccountDevice(
                @NonNull FastPairManageAccountDeviceRequestParcel requestParcel,
                IFastPairManageAccountDeviceCallback callback) {
            onManageFastPairAccountDevice(new FastPairManageAccountDeviceRequest(requestParcel),
                    new WrapperFastPairManageAccountDeviceCallback(callback));
        }
    }
}
