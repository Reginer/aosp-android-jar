/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.clockwork.displayoffload;

import static com.android.clockwork.displayoffload.DebugUtils.DEBUG_HAL;
import static com.android.clockwork.displayoffload.DebugUtils.dumpObjectDetails;
import static com.android.clockwork.displayoffload.DebugUtils.dumpObjectIdType;

import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_HAL_REMOTE_EXCEPTION;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_HAL_STATUS_NOT_OK;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_LAYOUT_CONTAINS_CYCLE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_LAYOUT_INVALID_RESOURCE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.FIELD_HAL_STATUS;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.FIELD_TRACE;

import android.annotation.NonNull;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import com.android.clockwork.displayoffload.HalTypeConverter.HalTypeConverterSupplier;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

import com.google.android.clockwork.ambient.offload.IDisplayOffloadService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import vendor.google_clockwork.displayoffload.V1_0.Status;

/**
 * Class that encapsulates add/replace/validation/send logic for HAL resource types.
 */
class HalResourceStore {
    private static final String TAG = "HalResourceStore";
    private static final boolean DEBUG = false;

    @VisibleForTesting
    @GuardedBy("this")
    final ArrayMap<Integer, Object> mResources = new ArrayMap<>();

    @VisibleForTesting
    @GuardedBy("this")
    final ArrayMap<String, Object> mStringIdentifiedBindings = new ArrayMap<>();

    @GuardedBy("this")
    private final ArraySet<Integer> mDirtyIds = new ArraySet<>();

    @GuardedBy("this")
    private final ArraySet<String> mDirtyStringIds = new ArraySet<>();

    private final HalTypeConverterSupplier mHalTypeConverter;

    @VisibleForTesting
    @GuardedBy("this")
    int[] mSendOrder = new int[]{};

    @GuardedBy("this")
    private boolean mDirtySendOrder = true;

    public HalResourceStore(HalTypeConverterSupplier halTypeConverter) {
        mHalTypeConverter = halTypeConverter;
    }

    /**
     * Store all the {@link ResourceObject} in objects to HalResourceStore.
     *
     * @param resourceObjects A list of {@link ResourceObject}.
     */
    public synchronized void addReplaceResource(@NonNull List<ResourceObject> resourceObjects) {
        for (ResourceObject resourceObject : resourceObjects) {
            addReplaceResource(resourceObject);
        }
    }

    /**
     * Store a single {@link ResourceObject} to HalResourceStore.
     *
     * @param resourceObject A single {@link ResourceObject}.
     */
    public synchronized void addReplaceResource(@NonNull ResourceObject resourceObject) {
        if (resourceObject.useStringName()) {
            addReplaceResource(resourceObject.getName(), resourceObject.getObject());
        } else {
            addReplaceResource(resourceObject.getId(), resourceObject.getObject());
        }
    }

    /**
     * Store one pair of <Integer, Object> to HalResourceStore.
     *
     * @param id     The integer identifier/key to query the HalResourceStore for the hal resource.
     * @param object The resource corresponding to id.
     */
    @VisibleForTesting
    synchronized void addReplaceResource(int id, @NonNull Object object) {
        if (Objects.equals(object, mResources.get(id))) return;
        Log.d(
                TAG,
                "addReplaceResource: id="
                        + dumpObjectIdType(id, object)
                        + (DEBUG ? (" object=" + dumpObjectDetails(object)) : ""));
        mDirtySendOrder = true;
        mDirtyIds.add(id);
        mResources.put(id, object);
    }

    /**
     * Store one pair of <String, Object> to HalResourceStore.
     *
     * @param name   The string identifier/key to query the HalResourceStore for the hal resource.
     * @param object The resource corresponding to id.
     */
    @VisibleForTesting
    synchronized void addReplaceResource(String name, @NonNull Object object) {
        if (Objects.equals(object, mStringIdentifiedBindings.get(name))) return;
        Log.d(
                TAG,
                "addReplaceResource: name="
                        + dumpObjectIdType(name, object)
                        + (DEBUG ? (" object=" + dumpObjectDetails(object)) : ""));
        mDirtySendOrder = true;
        mDirtyStringIds.add(name);
        mStringIdentifiedBindings.put(name, object);
    }

    public synchronized Object get(int id) {
        return mResources.get(id);
    }

    public synchronized Object get(String name) {
        return mStringIdentifiedBindings.get(name);
    }

    public synchronized boolean isEmpty() {
        return mResources.isEmpty();
    }

    public synchronized void clear() {
        mResources.clear();
        mStringIdentifiedBindings.clear();
        mDirtyIds.clear();
        mDirtyStringIds.clear();
        mDirtySendOrder = false;
    }

    public synchronized void markAllDirty() {
        // No need to mark sendOrder as dirty since it's already done
        mDirtyIds.clear();
        mDirtyStringIds.clear();
        mDirtyStringIds.addAll(mStringIdentifiedBindings.keySet());
        mDirtyIds.addAll(mResources.keySet());
    }

    private static void updateAdjacencyListIndegree(
            Map<Integer, List<Integer>> adj,
            Map<Integer, Integer> indegree,
            int node,
            List<Integer> children) {
        // Indegree is as the number of dependant/children that one resource has
        indegree.putIfAbsent(node, 0);
        if (children == null) {
            return;
        }
        // AdjacencyList records connection between parent resource with its children
        adj.put(node, children);
        // Compute indegree for each resource
        for (int child : children) {
            indegree.put(child, indegree.getOrDefault(child, 0) + 1);
        }
    }

    public synchronized int getRootId() throws DisplayOffloadException {
        if (mDirtySendOrder) {
            generateSendOrder();
        }
        if (mSendOrder == null || mSendOrder.length == 0) {
            return -1;
        }
        return mSendOrder[mSendOrder.length - 1];
    }

    public synchronized void generateSendOrder() throws DisplayOffloadException {
        if (mResources.size() == 0) {
            // No HAL resources
            mDirtySendOrder = false;
            mDirtyIds.clear();
            mSendOrder = new int[]{};
            return;
        }

        // Always regenerate virtual root.
        mResources.remove(IDisplayOffloadService.RESOURCE_ID_VIRTUAL_ROOT);
        mDirtyIds.remove(IDisplayOffloadService.RESOURCE_ID_VIRTUAL_ROOT);

        // Indegree is  the number of resources that depends on a certain resource
        Map<Integer, Integer> inDegree = new ArrayMap<>();
        for (Map.Entry<Integer, Object> entry : mResources.entrySet()) {
            Object object = entry.getValue();
            if (object == null) {
                // mResources is incorrect
                mDirtySendOrder = true;
                // Exception
                throw new DisplayOffloadException(ERROR_LAYOUT_INVALID_RESOURCE);
            }
            inDegree.putIfAbsent(entry.getKey(), 0);
            final List<Integer> children = mHalTypeConverter.getConverter().getIdReferenced(object);
            if (DEBUG) {
                Log.i(
                        TAG,
                        "Children of "
                                + dumpObjectIdType(entry.getKey(), object)
                                + " : "
                                + children);
            }
            if (children != null) {
                for (int child : children) {
                    inDegree.put(child, inDegree.getOrDefault(child, 0) + 1);
                }
            }
        }

        ArrayList<Integer> possibleRootIds = new ArrayList<>(inDegree.size());
        for (Map.Entry<Integer, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                // Is a possible root
                possibleRootIds.add(entry.getKey());
            }
        }
        if (DEBUG) {
            Log.i(TAG, "Possible roots are " + possibleRootIds);
        }

        if (possibleRootIds.size() > 1) {
            // Multiple roots
            Log.i(TAG, "Multiple roots possible. Inserting virtual root.");
            addReplaceResource(
                    mHalTypeConverter.getConverter().createDummyTranslationGroup(
                            IDisplayOffloadService.RESOURCE_ID_VIRTUAL_ROOT, possibleRootIds));
        } else {
            Log.i(TAG, "Single root. No need for virtual root.");
        }

        inDegree.clear();
        Map<Integer, List<Integer>> references = new ArrayMap<>();
        for (Map.Entry<Integer, Object> entry : mResources.entrySet()) {
            int id = entry.getKey();
            Object object = entry.getValue();
            if (object == null) {
                // mResources is incorrect
                mDirtySendOrder = true;
                // Exception
                throw new DisplayOffloadException(ERROR_LAYOUT_INVALID_RESOURCE);
            }
            updateAdjacencyListIndegree(references, inDegree, id,
                    mHalTypeConverter.getConverter().getIdReferenced(object));
        }

        int[] order = new int[inDegree.size()];
        int index = inDegree.size() - 1;
        // Check for cycle references
        while (!inDegree.isEmpty()) {
            boolean hasZeroInDegree = false;
            ArrayList<Integer> toRemove = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : inDegree.entrySet()) {
                int id = entry.getKey();
                if (entry.getValue() == 0) {
                    hasZeroInDegree = true;
                    order[index--] = id;
                    if (references.get(id) != null) {
                        for (int child : references.get(id)) {
                            inDegree.put(child, inDegree.get(child) - 1);
                        }
                    }
                    toRemove.add(id);
                }
            }
            for (int i : toRemove) {
                inDegree.remove(i);
            }
            if (!hasZeroInDegree) {
                // No zero in degree is found, graph has a cycle.
                mDirtySendOrder = true;
                throw new DisplayOffloadException(ERROR_LAYOUT_CONTAINS_CYCLE);
            }
        }

        mDirtySendOrder = false;
        mSendOrder = order;
    }

    private void throwIfHalFailure(int status, Object o) throws DisplayOffloadException {
        // Check HAL status
        if (status == Status.OK) {
            return;
        }
        Bundle exceptionBundle = new Bundle();
        exceptionBundle.putInt(FIELD_HAL_STATUS, status);
        throw new DisplayOffloadException(
                ERROR_HAL_STATUS_NOT_OK, exceptionBundle, "HAL call failed for: " + o.toString());
    }

    public synchronized void sendToHal(HalAdapter halAdapter)
            throws DisplayOffloadException {
        if (mDirtySendOrder) {
            // Need to re-generate order
            generateSendOrder();
        }

        if (DEBUG) {
            Log.d(TAG, "sendToHal: sendOrder: " + Arrays.toString(mSendOrder));
        }

        try {
            int status;
            // Send all variables first
            for (String name : mDirtyStringIds) {
                Object o = get(name);
                if (DEBUG_HAL) {
                    Log.d(TAG, "send: " + dumpObjectIdType(name, o));
                }
                status = halAdapter.send(o);
                throwIfHalFailure(status, name);
            }

            for (int i : mSendOrder) {
                if (!mDirtyIds.contains(i)) {
                    // Resource not dirty, can be skipped
                    Log.d(TAG, "sendToHal: skip sending to HAL: id=" + i);
                } else {
                    Object o = get(i);
                    if (DEBUG_HAL) {
                        Log.d(TAG, "send: " + dumpObjectIdType(i, o));
                    }
                    status = halAdapter.send(o);
                    throwIfHalFailure(status, i);
                }
            }

            if (mSendOrder.length == 0) {
                // Nothing to send, don't set root.
                Log.w(TAG, "mSendOrder.length = 0, empty layout.");
            } else {
                int root = mSendOrder[mSendOrder.length - 1];
                status = halAdapter.setRoot(root);
                throwIfHalFailure(status, "setting root as " + root);
            }
        } catch (RemoteException e) {
            Bundle exceptionBundle = new Bundle();
            exceptionBundle.putString(FIELD_TRACE, Utils.getStackTrace(e));
            throw new DisplayOffloadException(
                    ERROR_HAL_REMOTE_EXCEPTION,
                    exceptionBundle,
                    "RemoteException occurred: " + e.getMessage());
        } catch (DisplayOffloadException e) {
            Log.e(TAG, "Exception while sending to HAL.", e);
            throw e;
        }

        mDirtyIds.clear();
    }

    void dumpRecursively(int index, IndentingPrintWriter ipw) throws DisplayOffloadException {
        Object o = mResources.get(index);
        ipw.printf(dumpObjectIdType(index, o));
        ipw.println();
        if (o == null) return;

        ipw.increaseIndent();
        for (int child : mHalTypeConverter.getConverter().getIdReferenced(o)) {
            dumpRecursively(child, ipw);
        }
        ipw.decreaseIndent();
    }

    void dump(IndentingPrintWriter ipw) {
        ipw.printPair("mDirtySendOrder", mDirtySendOrder);
        ipw.printPair("mSendOrder", Arrays.toString(mSendOrder));
        ipw.println();

        ipw.printPair("mDirtyStringIds", mDirtyStringIds);
        ipw.println();

        ipw.printPair("mDirtyIds", mDirtyIds);
        ipw.println();

        int rootId =
                mSendOrder != null && mSendOrder.length != 0
                        ? mSendOrder[mSendOrder.length - 1]
                        : -1;
        ipw.printPair("Root ID", rootId);
        ipw.println();

        ipw.println("mResources");
        ipw.println("[Tree View]");
        ipw.increaseIndent();
        if (mSendOrder.length > 0) {
            try {
                dumpRecursively(rootId, ipw);
            } catch (DisplayOffloadException e) {
                ipw.println(e);
            }
        }
        ipw.println();
        ipw.decreaseIndent();

        ipw.println("[List View]");
        ipw.increaseIndent();
        for (int key : mSendOrder) {
            Object object = mResources.get(key);
            ipw.printf("%-25.25s:[%s]", dumpObjectIdType(key, object), dumpObjectDetails(object));
            ipw.println();
        }
        ipw.decreaseIndent();

        ipw.println("mStringIdentifiedBindings");
        ipw.increaseIndent();
        mStringIdentifiedBindings.forEach(
                (key, object) -> {
                    ipw.printf(
                            "%-25.25s:[%s]",
                            dumpObjectIdType(key, object), dumpObjectDetails(object));
                    ipw.println();
                });
        ipw.decreaseIndent();
    }
}
