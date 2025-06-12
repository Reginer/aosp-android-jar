/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.annotation.Nullable;
import android.content.IIntentReceiver;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import android.util.proto.ProtoOutputStream;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * A receiver object that has registered for one or more broadcasts.
 * The ArrayList holds BroadcastFilter objects.
 */
final class ReceiverList extends ArrayList<BroadcastFilter>
        implements IBinder.DeathRecipient {
    final ActivityManagerService owner;
    public final IIntentReceiver receiver;
    public final @Nullable ProcessRecord app;
    public final int pid;
    public final int uid;
    public final int userId;
    BroadcastRecord curBroadcast = null;
    boolean linkedToDeath = false;

    String stringName;

    ReceiverList(ActivityManagerService _owner, @Nullable ProcessRecord _app,
            int _pid, int _uid, int _userId, IIntentReceiver _receiver) {
        owner = _owner;
        receiver = _receiver;
        app = _app;
        pid = _pid;
        uid = _uid;
        userId = _userId;
    }

    // Want object identity, not the array identity we are inheriting.
    public boolean equals(Object o) {
        return this == o;
    }
    public int hashCode() {
        return System.identityHashCode(this);
    }

    public void binderDied() {
        linkedToDeath = false;
        owner.unregisterReceiver(receiver);
    }

    public boolean containsFilter(IntentFilter filter) {
        final int N = size();
        for (int i = 0; i < N; i++) {
            final BroadcastFilter f = get(i);
            if (IntentFilter.filterEquals(f, filter)) {
                return true;
            }
        }
        return false;
    }

    void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        if (app != null) {
            app.dumpDebug(proto, ReceiverListProto.APP);
            proto.write(ReceiverListProto.NUMBER_RECEIVERS, app.mReceivers.numberOfReceivers());
        }
        proto.write(ReceiverListProto.PID, pid);
        proto.write(ReceiverListProto.UID, uid);
        proto.write(ReceiverListProto.USER, userId);
        if (curBroadcast != null) {
            curBroadcast.dumpDebug(proto, ReceiverListProto.CURRENT);
        }
        proto.write(ReceiverListProto.LINKED_TO_DEATH, linkedToDeath);
        final int N = size();
        for (int i=0; i<N; i++) {
            BroadcastFilter bf = get(i);
            bf.dumpDebug(proto, ReceiverListProto.FILTERS);
        }
        proto.write(ReceiverListProto.HEX_HASH, Integer.toHexString(System.identityHashCode(this)));
        proto.end(token);
    }

    void dumpLocal(PrintWriter pw, String prefix) {
        pw.print(prefix); pw.print("app="); pw.print(app != null ? app.toShortString() : null);
        pw.print(" pid="); pw.print(pid); pw.print(" uid="); pw.print(uid);
        pw.print(" user="); pw.print(userId);
        if (app != null) {
            pw.print(" #receivers="); pw.print(app.mReceivers.numberOfReceivers());
        }
        pw.println();
        if (curBroadcast != null || linkedToDeath) {
            pw.print(prefix); pw.print("curBroadcast="); pw.print(curBroadcast);
                pw.print(" linkedToDeath="); pw.println(linkedToDeath);
        }
    }

    void dump(PrintWriter pw, String prefix) {
        Printer pr = new PrintWriterPrinter(pw);
        dumpLocal(pw, prefix);
        String p2 = prefix + "  ";
        final int N = size();
        for (int i=0; i<N; i++) {
            BroadcastFilter bf = get(i);
            pw.print(prefix); pw.print("Filter #"); pw.print(i);
                    pw.print(": BroadcastFilter{");
                    pw.print(Integer.toHexString(System.identityHashCode(bf)));
                    pw.println('}');
            bf.dumpInReceiverList(pw, pr, p2);
        }
    }

    public String toString() {
        if (stringName != null) {
            return stringName;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("ReceiverList{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        sb.append(pid);
        sb.append(' ');
        sb.append((app != null ? app.processName : "(unknown name)"));
        sb.append('/');
        sb.append(uid);
        sb.append("/u");
        sb.append(userId);
        sb.append((receiver.asBinder() instanceof Binder) ? " local:" : " remote:");
        sb.append(Integer.toHexString(System.identityHashCode(receiver.asBinder())));
        sb.append('}');
        return stringName = sb.toString();
    }
}
