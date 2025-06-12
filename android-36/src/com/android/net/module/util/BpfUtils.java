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
package com.android.net.module.util;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.IOException;

/**
 * The classes and the methods for BPF utilization.
 *
 * {@hide}
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class BpfUtils {
    static {
        System.loadLibrary(JniUtil.getJniLibraryName(BpfUtils.class.getPackage()));
    }

    // Defined in include/uapi/linux/bpf.h. Only adding the CGROUPS currently being used for now.
    public static final int BPF_CGROUP_INET_INGRESS = 0;
    public static final int BPF_CGROUP_INET_EGRESS = 1;
    public static final int BPF_CGROUP_INET_SOCK_CREATE = 2;
    public static final int BPF_CGROUP_INET4_BIND = 8;
    public static final int BPF_CGROUP_INET6_BIND = 9;
    public static final int BPF_CGROUP_INET4_CONNECT = 10;
    public static final int BPF_CGROUP_INET6_CONNECT = 11;
    public static final int BPF_CGROUP_UDP4_SENDMSG = 14;
    public static final int BPF_CGROUP_UDP6_SENDMSG = 15;
    public static final int BPF_CGROUP_UDP4_RECVMSG = 19;
    public static final int BPF_CGROUP_UDP6_RECVMSG = 20;
    public static final int BPF_CGROUP_GETSOCKOPT = 21;
    public static final int BPF_CGROUP_SETSOCKOPT = 22;
    public static final int BPF_CGROUP_INET_SOCK_RELEASE = 34;

    // Note: This is only guaranteed to be accurate on U+ devices. It is likely to be accurate
    // on T+ devices as well, but this is not guaranteed.
    public static final String CGROUP_PATH = "/sys/fs/cgroup/";

    /**
     * Get BPF program Id from CGROUP.
     *
     * Note: This requires a 4.19 kernel which is only guaranteed on V+.
     *
     * @param attachType Bpf attach type. See bpf_attach_type in include/uapi/linux/bpf.h.
     * @return Positive integer for a Program Id. 0 if no program is attached.
     * @throws IOException if failed to open the cgroup directory or query bpf program.
     */
    public static int getProgramId(int attachType) throws IOException {
        return native_getProgramIdFromCgroup(attachType, CGROUP_PATH);
    }

    private static native int native_getProgramIdFromCgroup(int type, String cgroupPath)
            throws IOException;
}
