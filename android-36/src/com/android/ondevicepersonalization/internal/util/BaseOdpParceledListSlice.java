/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.ondevicepersonalization.internal.util;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

/**
 * Copy of BaseParceledListSlice from framework.
 *
 * Transfer a large list of Parcelable objects across an IPC.  Splits into
 * multiple transactions if needed.
 *
 * Caveat: for efficiency and security, all elements must be the same concrete type.
 * In order to avoid writing the class name of each object, we must ensure that
 * each object is the same type, or else unparceling then reparceling the data may yield
 * a different result if the class name encoded in the Parcelable is a Base type.
 * See b/17671747.
 */
abstract class BaseOdpParceledListSlice<T> implements Parcelable {
    private static final LoggerFactory.Logger sLogger = LoggerFactory.getLogger();
    private static final boolean DEBUG = false;
    private static final String TAG = BaseOdpParceledListSlice.class.getSimpleName();
    private static final int MAX_IPC_SIZE = IBinder.getSuggestedMaxIpcSizeBytes();

    private final List<T> mList;

    private int mInlineCountLimit = Integer.MAX_VALUE;

    BaseOdpParceledListSlice(List<T> list) {
        mList = list;
    }

    @SuppressWarnings("unchecked")
    BaseOdpParceledListSlice(Parcel p, ClassLoader loader) {
        final int numItems = p.readInt();
        mList = new ArrayList<T>(numItems);
        if (DEBUG) sLogger.d(TAG + ": Retrieving " + numItems + " items");
        if (numItems <= 0) {
            return;
        }

        Creator<?> creator = readParcelableCreator(p, loader);
        Class<?> listElementClass = null;

        int i = 0;
        while (i < numItems) {
            if (p.readInt() == 0) {
                break;
            }

            final T parcelable = readCreator(creator, p, loader);
            if (listElementClass == null) {
                listElementClass = parcelable.getClass();
            } else {
                verifySameType(listElementClass, parcelable.getClass());
            }

            mList.add(parcelable);

            if (DEBUG) sLogger.d(TAG + ": Read inline #" + i + ": " + mList.get(mList.size() - 1));
            i++;
        }
        if (i >= numItems) {
            return;
        }
        final IBinder retriever = p.readStrongBinder();
        while (i < numItems) {
            if (DEBUG) {
                sLogger.d(TAG
                        + ": Reading more @" + i + " of " + numItems + ": retriever=" + retriever);
            }
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInt(i);
            try {
                retriever.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, 0);
            } catch (RemoteException e) {
                sLogger.w(e, TAG + ": Failure retrieving array; only received " + i + " of "
                        + numItems);
                return;
            }
            while (i < numItems && reply.readInt() != 0) {
                final T parcelable = readCreator(creator, reply, loader);
                verifySameType(listElementClass, parcelable.getClass());

                mList.add(parcelable);

                if (DEBUG) {
                    sLogger.d(TAG + ": Read extra #" + i + ": " + mList.get(mList.size() - 1));
                }
                i++;
            }
            reply.recycle();
            data.recycle();
        }
    }

    private static void verifySameType(final Class<?> expected, final Class<?> actual) {
        if (!actual.equals(expected)) {
            throw new IllegalArgumentException("Can't unparcel type "
                    + (actual == null ? null : actual.getName()) + " in list of type "
                    + (expected == null ? null : expected.getName()));
        }
    }

    private T readCreator(Creator<?> creator, Parcel p, ClassLoader loader) {
        if (creator instanceof ClassLoaderCreator<?>) {
            ClassLoaderCreator<?> classLoaderCreator =
                    (ClassLoaderCreator<?>) creator;
            return (T) classLoaderCreator.createFromParcel(p, loader);
        }
        return (T) creator.createFromParcel(p);
    }

    public List<T> getList() {
        return mList;
    }

    /**
     * Set a limit on the maximum number of entries in the array that will be included
     * inline in the initial parcelling of this object.
     */
    public void setInlineCountLimit(int maxCount) {
        mInlineCountLimit = maxCount;
    }

    /**
     * Write this to another Parcel. Note that this discards the internal Parcel
     * and should not be used anymore. This is so we can pass this to a Binder
     * where we won't have a chance to call recycle on this.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        final int numItems = mList.size();
        final int callFlags = flags;
        dest.writeInt(numItems);
        if (DEBUG) sLogger.d(TAG + ": Writing " + numItems + " items");
        if (numItems > 0) {
            final Class<?> listElementClass = mList.get(0).getClass();
            writeParcelableCreator(mList.get(0), dest);
            int i = 0;
            while (i < numItems && i < mInlineCountLimit && dest.dataSize() < MAX_IPC_SIZE) {
                dest.writeInt(1);

                final T parcelable = mList.get(i);
                verifySameType(listElementClass, parcelable.getClass());
                writeElement(parcelable, dest, callFlags);

                if (DEBUG) sLogger.d(TAG + ": Wrote inline #" + i + ": " + mList.get(i));
                i++;
            }
            if (i < numItems) {
                dest.writeInt(0);
                Binder retriever = new Binder() {
                    @Override
                    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                            throws RemoteException {
                        if (code != FIRST_CALL_TRANSACTION) {
                            return super.onTransact(code, data, reply, flags);
                        }
                        int i = data.readInt();
                        if (DEBUG) sLogger.d(TAG + ": Writing more @" + i + " of " + numItems);
                        while (i < numItems && reply.dataSize() < MAX_IPC_SIZE) {
                            reply.writeInt(1);

                            final T parcelable = mList.get(i);
                            verifySameType(listElementClass, parcelable.getClass());
                            writeElement(parcelable, reply, callFlags);

                            if (DEBUG) {
                                sLogger.d(TAG + ": Wrote extra #" + i + ": " + mList.get(i));
                            }
                            i++;
                        }
                        if (i < numItems) {
                            if (DEBUG) sLogger.d(TAG + ": Breaking @" + i + " of " + numItems);
                            reply.writeInt(0);
                        }
                        return true;
                    }
                };
                if (DEBUG) {
                    sLogger.d(TAG
                            + ": Breaking @" + i + " of " + numItems + ": retriever=" + retriever);
                }
                dest.writeStrongBinder(retriever);
            }
        }
    }

    protected abstract void writeElement(T parcelable, Parcel reply, int callFlags);

    protected abstract void writeParcelableCreator(T parcelable, Parcel dest);

    protected abstract Creator<?> readParcelableCreator(Parcel from, ClassLoader loader);
}
