/*
 * Copyright 2017 The Android Open Source Project
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

package android.app.servertransaction;

import static com.android.internal.annotations.VisibleForTesting.Visibility.PACKAGE;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ClientTransactionHandler;
import android.app.IApplicationThread;
import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;

import com.android.internal.annotations.VisibleForTesting;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A container that holds a sequence of messages, which may be sent to a client.
 * This includes a list of callbacks and a final lifecycle state.
 *
 * @see com.android.server.wm.ClientLifecycleManager
 * @see ClientTransactionItem
 * @see ActivityLifecycleItem
 * @hide
 */
public class ClientTransaction implements Parcelable {

    /**
     * List of transaction items that should be executed in order. Including both
     * {@link ActivityLifecycleItem} and other {@link ClientTransactionItem}.
     */
    @NonNull
    private final List<ClientTransactionItem> mTransactionItems = new ArrayList<>();

    /** @deprecated use {@link #getTransactionItems} instead. */
    @Nullable
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            trackingBug = 324203798,
            publicAlternatives = "Use {@code #getTransactionItems()}")
    @Deprecated
    private List<ClientTransactionItem> mActivityCallbacks;

    /**
     * Final lifecycle state in which the client activity should be after the transaction is
     * executed.
     */
    // TODO(b/324203798): cleanup after remove UnsupportedAppUsage
    @Nullable
    private ActivityLifecycleItem mLifecycleStateRequest;

    /** Only kept for unsupportedAppUsage {@link #getActivityToken()}. Must not be used. */
    // TODO(b/324203798): cleanup after remove UnsupportedAppUsage
    @Nullable
    private IBinder mActivityToken;

    /**
     * The target client.
     * <p>
     * This field is null only if the object is:
     * - Read from a Parcel on the client side.
     * - Constructed for testing purposes.
     * <p>
     * When created directly on the server, this field represents the server's connection to the
     * target client's application thread. It is omitted during parceling and not sent to the
     * client. On the client side, this field becomes unnecessary.
     */
    @Nullable
    private final IApplicationThread mClient;

    @VisibleForTesting
    public ClientTransaction() {
        mClient = null;
    }

    public ClientTransaction(@NonNull IApplicationThread client) {
        mClient = requireNonNull(client);
    }

    /**
     * Gets the target client associated with this transaction.
     * <p>
     * This method is intended for server-side use only. Calling it from the client side
     * will always return {@code null}.
     *
     * @return the {@link IApplicationThread} representing the target client, or {@code null} if
     * called from the client side.
     * @see #mClient
     */
    public IApplicationThread getClient() {
        return mClient;
    }

    /**
     * Adds a message to the end of the sequence of transaction items.
     * @param item A single message that can contain a client activity/window request/callback.
     */
    public void addTransactionItem(@NonNull ClientTransactionItem item) {
        mTransactionItems.add(item);

        // TODO(b/324203798): cleanup after remove UnsupportedAppUsage
        // Populate even if mTransactionItems is set to support the UnsupportedAppUsage.
        if (item.isActivityLifecycleItem()) {
            setLifecycleStateRequest((ActivityLifecycleItem) item);
        } else {
            addCallback(item);
        }
    }

    /**
     * Gets the list of client window requests/callbacks.
     */
    @NonNull
    public List<ClientTransactionItem> getTransactionItems() {
        return mTransactionItems;
    }

    /**
     * Adds a message to the end of the sequence of callbacks.
     * @param activityCallback A single message that can contain a lifecycle request/callback.
     * @deprecated use {@link #addTransactionItem(ClientTransactionItem)} instead.
     */
    // TODO(b/324203798): cleanup after remove UnsupportedAppUsage
    @Deprecated
    private void addCallback(@NonNull ClientTransactionItem activityCallback) {
        if (mActivityCallbacks == null) {
            mActivityCallbacks = new ArrayList<>();
        }
        mActivityCallbacks.add(activityCallback);
        setActivityTokenIfNotSet(activityCallback);
    }

    /** @deprecated use {@link #getTransactionItems()} instead. */
    @VisibleForTesting
    @Nullable
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            trackingBug = 324203798,
            publicAlternatives = "Use {@code #getTransactionItems()}")
    @Deprecated
    public List<ClientTransactionItem> getCallbacks() {
        return mActivityCallbacks;
    }

    /**
     * A transaction can contain {@link ClientTransactionItem} of different activities,
     * this must not be used. For any unsupported app usages, please be aware that this is set to
     * the activity of the first item in {@link #getTransactionItems()}.
     *
     * @deprecated use {@link ClientTransactionItem#getActivityToken()} instead.
     */
    @VisibleForTesting
    @Nullable
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            trackingBug = 324203798,
            publicAlternatives = "Use {@code android.app.servertransaction"
                    + ".ClientTransactionItem#getActivityToken()}")
    @Deprecated
    public IBinder getActivityToken() {
        return mActivityToken;
    }

    /** @deprecated use {@link #getTransactionItems()} instead. */
    @VisibleForTesting(visibility = PACKAGE)
    @Nullable
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            trackingBug = 324203798,
            publicAlternatives = "Use {@code #getTransactionItems()}")
    @Deprecated
    public ActivityLifecycleItem getLifecycleStateRequest() {
        return mLifecycleStateRequest;
    }

    /**
     * Sets the lifecycle state in which the client should be after executing the transaction.
     * @param stateRequest A lifecycle request initialized with right parameters.
     * @deprecated use {@link #addTransactionItem(ClientTransactionItem)} instead.
     */
    // TODO(b/324203798): cleanup after remove UnsupportedAppUsage
    @Deprecated
    private void setLifecycleStateRequest(@NonNull ActivityLifecycleItem stateRequest) {
        if (mLifecycleStateRequest != null) {
            return;
        }
        mLifecycleStateRequest = stateRequest;
        setActivityTokenIfNotSet(stateRequest);
    }

    // TODO(b/324203798): cleanup after remove UnsupportedAppUsage
    private void setActivityTokenIfNotSet(@Nullable ClientTransactionItem item) {
        if (mActivityToken == null && item != null) {
            mActivityToken = item.getActivityToken();
        }
    }

    /**
     * Do what needs to be done while the transaction is being scheduled on the client side.
     * @param clientTransactionHandler Handler on the client side that will executed all operations
     *                                 requested by transaction items.
     */
    public void preExecute(@NonNull ClientTransactionHandler clientTransactionHandler) {
        final int size = mTransactionItems.size();
        for (int i = 0; i < size; ++i) {
            mTransactionItems.get(i).preExecute(clientTransactionHandler);
        }
    }

    /**
     * Schedule the transaction after it was initialized. It will be send to client and all its
     * individual parts will be applied in the following sequence:
     * 1. The client calls {@link #preExecute(ClientTransactionHandler)}, which triggers all work
     *    that needs to be done before actually scheduling the transaction for callbacks and
     *    lifecycle state request.
     * 2. The transaction message is scheduled.
     * 3. The client calls {@link TransactionExecutor#execute(ClientTransaction)}, which executes
     *    all callbacks and necessary lifecycle transitions.
     *
     * @return {@link RemoteException} if the transaction failed.
     */
    @Nullable
    public RemoteException schedule() {
        try {
            mClient.scheduleTransaction(this);
            return null;
        } catch (RemoteException e) {
            return e;
        }
    }

    // Parcelable implementation

    /** Writes to Parcel. */
    @SuppressWarnings("AndroidFrameworkEfficientParcelable") // Item class is not final.
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelableList(mTransactionItems, flags);
    }

    /** Reads from Parcel. */
    private ClientTransaction(@NonNull Parcel in) {
        mClient = null;  // This field is unnecessary on the client side.
        in.readParcelableList(mTransactionItems, getClass().getClassLoader(),
                ClientTransactionItem.class);

        // TODO(b/324203798): cleanup after remove UnsupportedAppUsage
        // Populate mLifecycleStateRequest and mActivityCallbacks from mTransactionItems so
        // that they have the same reference when there is UnsupportedAppUsage to those fields.
        final int size = mTransactionItems.size();
        for (int i = 0; i < size; i++) {
            final ClientTransactionItem item = mTransactionItems.get(i);
            if (item.isActivityLifecycleItem()) {
                setLifecycleStateRequest((ActivityLifecycleItem) item);
            } else {
                addCallback(item);
            }
        }
    }

    public static final @NonNull Creator<ClientTransaction> CREATOR = new Creator<>() {
        public ClientTransaction createFromParcel(@NonNull Parcel in) {
            return new ClientTransaction(in);
        }

        public ClientTransaction[] newArray(int size) {
            return new ClientTransaction[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ClientTransaction other = (ClientTransaction) o;
        return Objects.equals(mTransactionItems, other.mTransactionItems)
                && Objects.equals(mActivityCallbacks, other.mActivityCallbacks)
                && Objects.equals(mLifecycleStateRequest, other.mLifecycleStateRequest)
                && mClient == other.mClient
                && Objects.equals(mActivityToken, other.mActivityToken);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + Objects.hashCode(mTransactionItems);
        result = 31 * result + Objects.hashCode(mActivityCallbacks);
        result = 31 * result + Objects.hashCode(mLifecycleStateRequest);
        result = 31 * result + Objects.hashCode(mClient);
        result = 31 * result + Objects.hashCode(mActivityToken);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ClientTransaction{");
        sb.append("\n  transactionItems=[");
        final int size = mTransactionItems.size();
        for (int i = 0; i < size; i++) {
            sb.append("\n    ").append(mTransactionItems.get(i));
        }
        sb.append("\n  ]");
        sb.append("\n}");
        return sb.toString();
    }

    /** Dump transaction items callback items and final lifecycle state request. */
    void dump(@NonNull String prefix, @NonNull PrintWriter pw,
            @NonNull ClientTransactionHandler transactionHandler) {
        pw.append(prefix).println("ClientTransaction{");
        pw.append(prefix).print("  transactionItems=[");
        final String itemPrefix = prefix + "    ";
        final int size = mTransactionItems.size();
        if (size > 0) {
            pw.println();
            for (int i = 0; i < size; i++) {
                mTransactionItems.get(i).dump(itemPrefix, pw, transactionHandler);
            }
            pw.append(prefix).println("  ]");
        } else {
            pw.println("]");
        }
        pw.append(prefix).println("}");
    }
}
