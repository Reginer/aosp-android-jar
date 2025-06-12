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

package com.android.server.am;

import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.ComponentName.WithComponentName;
import android.os.Binder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import android.util.SparseArray;

import com.android.internal.os.TransferPipe;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.DumpUtils;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Keeps track of content providers by authority (name) and class. It separates the mapping by
 * user and ones that are not user-specific (system providers).
 */
public final class ProviderMap {

    private static final String TAG = "ProviderMap";

    private static final boolean DBG = false;

    private final ActivityManagerService mAm;

    private final HashMap<String, ContentProviderRecord> mSingletonByName
            = new HashMap<String, ContentProviderRecord>();
    private final HashMap<ComponentName, ContentProviderRecord> mSingletonByClass
            = new HashMap<ComponentName, ContentProviderRecord>();

    private final SparseArray<HashMap<String, ContentProviderRecord>> mProvidersByNamePerUser
            = new SparseArray<HashMap<String, ContentProviderRecord>>();
    private final SparseArray<HashMap<ComponentName, ContentProviderRecord>> mProvidersByClassPerUser
            = new SparseArray<HashMap<ComponentName, ContentProviderRecord>>();

    ProviderMap(ActivityManagerService am) {
        mAm = am;
    }

    ContentProviderRecord getProviderByName(String name) {
        return getProviderByName(name, -1);
    }

    ContentProviderRecord getProviderByName(String name, int userId) {
        if (DBG) {
            Slog.i(TAG, "getProviderByName: " + name + " , callingUid = " + Binder.getCallingUid());
        }
        // Try to find it in the global list
        ContentProviderRecord record = mSingletonByName.get(name);
        if (record != null) {
            return record;
        }

        // Check the current user's list
        return getProvidersByName(userId).get(name);
    }

    ContentProviderRecord getProviderByClass(ComponentName name) {
        return getProviderByClass(name, -1);
    }

    ContentProviderRecord getProviderByClass(ComponentName name, int userId) {
        if (DBG) {
            Slog.i(TAG, "getProviderByClass: " + name + ", callingUid = " + Binder.getCallingUid());
        }
        // Try to find it in the global list
        ContentProviderRecord record = mSingletonByClass.get(name);
        if (record != null) {
            return record;
        }

        // Check the current user's list
        return getProvidersByClass(userId).get(name);
    }

    void putProviderByName(String name, ContentProviderRecord record) {
        if (DBG) {
            Slog.i(TAG, "putProviderByName: " + name + " , callingUid = " + Binder.getCallingUid()
                + ", record uid = " + record.appInfo.uid);
        }
        if (record.singleton) {
            mSingletonByName.put(name, record);
        } else {
            final int userId = UserHandle.getUserId(record.appInfo.uid);
            getProvidersByName(userId).put(name, record);
        }
    }

    void putProviderByClass(ComponentName name, ContentProviderRecord record) {
        if (DBG) {
            Slog.i(TAG, "putProviderByClass: " + name + " , callingUid = " + Binder.getCallingUid()
                + ", record uid = " + record.appInfo.uid);
        }
        if (record.singleton) {
            mSingletonByClass.put(name, record);
        } else {
            final int userId = UserHandle.getUserId(record.appInfo.uid);
            getProvidersByClass(userId).put(name, record);
        }
    }

    void removeProviderByName(String name, int userId) {
        if (mSingletonByName.containsKey(name)) {
            if (DBG)
                Slog.i(TAG, "Removing from globalByName name=" + name);
            mSingletonByName.remove(name);
        } else {
            if (userId < 0) throw new IllegalArgumentException("Bad user " + userId);
            if (DBG)
                Slog.i(TAG,
                        "Removing from providersByName name=" + name + " user=" + userId);
            HashMap<String, ContentProviderRecord> map = getProvidersByName(userId);
            // map returned by getProvidersByName wouldn't be null
            map.remove(name);
            if (map.size() == 0) {
                mProvidersByNamePerUser.remove(userId);
            }
        }
    }

    void removeProviderByClass(ComponentName name, int userId) {
        if (mSingletonByClass.containsKey(name)) {
            if (DBG)
                Slog.i(TAG, "Removing from globalByClass name=" + name);
            mSingletonByClass.remove(name);
        } else {
            if (userId < 0) throw new IllegalArgumentException("Bad user " + userId);
            if (DBG)
                Slog.i(TAG,
                        "Removing from providersByClass name=" + name + " user=" + userId);
            HashMap<ComponentName, ContentProviderRecord> map = getProvidersByClass(userId);
            // map returned by getProvidersByClass wouldn't be null
            map.remove(name);
            if (map.size() == 0) {
                mProvidersByClassPerUser.remove(userId);
            }
        }
    }

    private HashMap<String, ContentProviderRecord> getProvidersByName(int userId) {
        if (userId < 0) throw new IllegalArgumentException("Bad user " + userId);
        final HashMap<String, ContentProviderRecord> map = mProvidersByNamePerUser.get(userId);
        if (map == null) {
            HashMap<String, ContentProviderRecord> newMap = new HashMap<String, ContentProviderRecord>();
            mProvidersByNamePerUser.put(userId, newMap);
            return newMap;
        } else {
            return map;
        }
    }

    HashMap<ComponentName, ContentProviderRecord> getProvidersByClass(int userId) {
        if (userId < 0) throw new IllegalArgumentException("Bad user " + userId);
        final HashMap<ComponentName, ContentProviderRecord> map
                = mProvidersByClassPerUser.get(userId);
        if (map == null) {
            HashMap<ComponentName, ContentProviderRecord> newMap
                    = new HashMap<ComponentName, ContentProviderRecord>();
            mProvidersByClassPerUser.put(userId, newMap);
            return newMap;
        } else {
            return map;
        }
    }

    private boolean collectPackageProvidersLocked(String packageName,
            Set<String> filterByClasses, boolean doit, boolean evenPersistent,
            HashMap<ComponentName, ContentProviderRecord> providers,
            ArrayList<ContentProviderRecord> result) {
        boolean didSomething = false;
        for (ContentProviderRecord provider : providers.values()) {
            final boolean sameComponent = packageName == null
                    || (provider.info.packageName.equals(packageName)
                        && (filterByClasses == null
                            || filterByClasses.contains(provider.name.getClassName())));
            if (sameComponent
                    && (provider.proc == null || evenPersistent || !provider.proc.isPersistent())) {
                if (!doit) {
                    return true;
                }
                didSomething = true;
                result.add(provider);
            }
        }
        return didSomething;
    }

    boolean collectPackageProvidersLocked(String packageName, Set<String> filterByClasses,
            boolean doit, boolean evenPersistent, int userId,
            ArrayList<ContentProviderRecord> result) {
        boolean didSomething = false;
        if (userId == UserHandle.USER_ALL || userId == UserHandle.USER_SYSTEM) {
            didSomething = collectPackageProvidersLocked(packageName, filterByClasses,
                    doit, evenPersistent, mSingletonByClass, result);
        }
        if (!doit && didSomething) {
            return true;
        }
        if (userId == UserHandle.USER_ALL) {
            for (int i = 0; i < mProvidersByClassPerUser.size(); i++) {
                if (collectPackageProvidersLocked(packageName, filterByClasses,
                        doit, evenPersistent, mProvidersByClassPerUser.valueAt(i), result)) {
                    if (!doit) {
                        return true;
                    }
                    didSomething = true;
                }
            }
        } else {
            HashMap<ComponentName, ContentProviderRecord> items
                    = getProvidersByClass(userId);
            if (items != null) {
                didSomething |= collectPackageProvidersLocked(packageName, filterByClasses,
                        doit, evenPersistent, items, result);
            }
        }
        return didSomething;
    }

    private boolean dumpProvidersByClassLocked(PrintWriter pw, boolean dumpAll, String dumpPackage,
            String header, boolean needSep, HashMap<ComponentName, ContentProviderRecord> map) {
        Iterator<Map.Entry<ComponentName, ContentProviderRecord>> it = map.entrySet().iterator();
        boolean written = false;
        while (it.hasNext()) {
            Map.Entry<ComponentName, ContentProviderRecord> e = it.next();
            ContentProviderRecord r = e.getValue();
            if (dumpPackage != null && !dumpPackage.equals(r.appInfo.packageName)) {
                continue;
            }
            if (needSep) {
                pw.println("");
                needSep = false;
            }
            if (header != null) {
                pw.println(header);
                header = null;
            }
            written = true;
            pw.print("  * ");
            pw.println(r);
            r.dump(pw, "    ", dumpAll);
        }
        return written;
    }

    private boolean dumpProvidersByNameLocked(PrintWriter pw, String dumpPackage,
            String header, boolean needSep, HashMap<String, ContentProviderRecord> map) {
        Iterator<Map.Entry<String, ContentProviderRecord>> it = map.entrySet().iterator();
        boolean written = false;
        while (it.hasNext()) {
            Map.Entry<String, ContentProviderRecord> e = it.next();
            ContentProviderRecord r = e.getValue();
            if (dumpPackage != null && !dumpPackage.equals(r.appInfo.packageName)) {
                continue;
            }
            if (needSep) {
                pw.println("");
                needSep = false;
            }
            if (header != null) {
                pw.println(header);
                header = null;
            }
            written = true;
            pw.print("  ");
            pw.print(e.getKey());
            pw.print(": ");
            pw.println(r.toShortString());
        }
        return written;
    }

    boolean dumpProvidersLocked(PrintWriter pw, boolean dumpAll, String dumpPackage) {
        boolean needSep = false;

        if (mSingletonByClass.size() > 0) {
            needSep |= dumpProvidersByClassLocked(pw, dumpAll, dumpPackage,
                    "  Published single-user content providers (by class):", needSep,
                    mSingletonByClass);
        }

        for (int i = 0; i < mProvidersByClassPerUser.size(); i++) {
            HashMap<ComponentName, ContentProviderRecord> map = mProvidersByClassPerUser.valueAt(i);
            needSep |= dumpProvidersByClassLocked(pw, dumpAll, dumpPackage,
                    "  Published user " + mProvidersByClassPerUser.keyAt(i)
                            + " content providers (by class):", needSep, map);
        }

        if (dumpAll) {
            needSep |= dumpProvidersByNameLocked(pw, dumpPackage,
                    "  Single-user authority to provider mappings:", needSep, mSingletonByName);

            for (int i = 0; i < mProvidersByNamePerUser.size(); i++) {
                needSep |= dumpProvidersByNameLocked(pw, dumpPackage,
                        "  User " + mProvidersByNamePerUser.keyAt(i)
                                + " authority to provider mappings:", needSep,
                        mProvidersByNamePerUser.valueAt(i));
            }
        }
        return needSep;
    }

    private ArrayList<ContentProviderRecord> getProvidersForName(String name) {
        ArrayList<ContentProviderRecord> allProviders = new ArrayList<ContentProviderRecord>();
        final ArrayList<ContentProviderRecord> ret = new ArrayList<>();

        final Predicate<ContentProviderRecord> filter = DumpUtils.filterRecord(name);

        synchronized (mAm) {
            allProviders.addAll(mSingletonByClass.values());
            for (int i=0; i<mProvidersByClassPerUser.size(); i++) {
                allProviders.addAll(mProvidersByClassPerUser.valueAt(i).values());
            }

            CollectionUtils.addIf(allProviders, ret, filter);
        }
        // Sort by component name.
        ret.sort(Comparator.comparing(WithComponentName::getComponentName));
        return ret;
    }

    protected boolean dumpProvider(FileDescriptor fd, PrintWriter pw, String name, String[] args,
            int opti, boolean dumpAll) {
        try {
            mAm.mOomAdjuster.mCachedAppOptimizer.enableFreezer(false);
            ArrayList<ContentProviderRecord> providers = getProvidersForName(name);

            if (providers.size() <= 0) {
                return false;
            }

            boolean needSep = false;
            for (int i=0; i<providers.size(); i++) {
                if (needSep) {
                    pw.println();
                }
                needSep = true;
                dumpProvider("", fd, pw, providers.get(i), args, dumpAll);
            }
            return true;
        } finally {
            mAm.mOomAdjuster.mCachedAppOptimizer.enableFreezer(true);
        }
    }

    /**
     * Before invoking IApplicationThread.dumpProvider(), print meta information to the print
     * writer and handle passed flags.
     */
    private void dumpProvider(String prefix, FileDescriptor fd, PrintWriter pw,
            final ContentProviderRecord r, String[] args, boolean dumpAll) {
        final IApplicationThread thread = r.proc != null ? r.proc.getThread() : null;
        for (String s: args) {
            if (!dumpAll && s.contains("--proto")) {
                if (thread != null) {
                    dumpToTransferPipe(null , fd, pw, r, thread, args);
                }
                return;
            }
        }
        String innerPrefix = prefix + "  ";
        synchronized (mAm) {
            pw.print(prefix); pw.print("PROVIDER ");
            pw.print(r);
            pw.print(" pid=");
            if (r.proc != null) {
                pw.println(r.proc.getPid());
            } else {
                pw.println("(not running)");
            }
            if (dumpAll) {
                r.dump(pw, innerPrefix, true);
            }
        }
        if (thread != null) {
            pw.println("    Client:");
            pw.flush();
            dumpToTransferPipe("      ", fd, pw, r, thread, args);
        }
    }

    /**
     * Similar to the dumpProvider, but only dumps the first matching provider.
     * The provider is responsible for dumping as proto.
     */
    protected boolean dumpProviderProto(FileDescriptor fd, PrintWriter pw, String name,
            String[] args) {
        //add back the --proto arg, which was stripped out by PriorityDump
        String[] newArgs = Arrays.copyOf(args, args.length + 1);
        newArgs[args.length] = "--proto";

        ArrayList<ContentProviderRecord> providers = getProvidersForName(name);

        if (providers.size() <= 0) {
            return false;
        }

        // Only dump the first provider, since we are dumping in proto format
        for (int i = 0; i < providers.size(); i++) {
            final ContentProviderRecord r = providers.get(i);
            IApplicationThread thread;
            if (r.proc != null && (thread = r.proc.getThread()) != null) {
                dumpToTransferPipe(null, fd, pw, r, thread, newArgs);
                return true;
            }
        }
        return false;
    }

    /**
     * Invokes IApplicationThread.dumpProvider() on the thread of the specified provider without
     * any meta string (e.g., provider info, indentation) written to the file descriptor.
     */
    private void dumpToTransferPipe(String prefix, FileDescriptor fd, PrintWriter pw,
            final ContentProviderRecord r, final IApplicationThread thread, String[] args) {
        try {
            TransferPipe tp = new TransferPipe();
            try {
                thread.dumpProvider(
                    tp.getWriteFd(), r.provider.asBinder(), args);
                tp.setBufferPrefix(prefix);
                // Short timeout, since blocking here can
                // deadlock with the application.
                tp.go(fd, 2000);
            } finally {
                tp.kill();
            }
        } catch (IOException ex) {
            pw.println("      Failure while dumping the provider: " + ex);
        } catch (RemoteException ex) {
            pw.println("      Got a RemoteException while dumping the service");
        }
    }
}
