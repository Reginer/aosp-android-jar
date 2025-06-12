/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.ddm;


import android.os.Debug;
import android.util.Log;

import org.apache.harmony.dalvik.ddmc.Chunk;
import org.apache.harmony.dalvik.ddmc.ChunkHandler;
import org.apache.harmony.dalvik.ddmc.DdmServer;

import java.nio.ByteBuffer;

/**
 * Handle profiling requests.
 */
public class DdmHandleProfiling extends DdmHandle {

    public static final int CHUNK_MPRS = ChunkHandler.type("MPRS");
    public static final int CHUNK_MPRE = ChunkHandler.type("MPRE");
    public static final int CHUNK_MPSS = ChunkHandler.type("MPSS");
    public static final int CHUNK_MPSE = ChunkHandler.type("MPSE");
    public static final int CHUNK_MPRQ = ChunkHandler.type("MPRQ");
    public static final int CHUNK_SPSS = ChunkHandler.type("SPSS");
    public static final int CHUNK_SPSE = ChunkHandler.type("SPSE");

    private static final boolean DEBUG = false;
    private static DdmHandleProfiling mInstance = new DdmHandleProfiling();


    /* singleton, do not instantiate */
    private DdmHandleProfiling() {}

    /**
     * Register for the messages we're interested in.
     */
    public static void register() {
        DdmServer.registerHandler(CHUNK_MPRS, mInstance);
        DdmServer.registerHandler(CHUNK_MPRE, mInstance);
        DdmServer.registerHandler(CHUNK_MPSS, mInstance);
        DdmServer.registerHandler(CHUNK_MPSE, mInstance);
        DdmServer.registerHandler(CHUNK_MPRQ, mInstance);
        DdmServer.registerHandler(CHUNK_SPSS, mInstance);
        DdmServer.registerHandler(CHUNK_SPSE, mInstance);
    }

    /**
     * Called when the DDM server connects.  The handler is allowed to
     * send messages to the server.
     */
    public void onConnected() {}

    /**
     * Called when the DDM server disconnects.  Can be used to disable
     * periodic transmissions or clean up saved state.
     */
    public void onDisconnected() {}

    /**
     * Handle a chunk of data.
     */
    public Chunk handleChunk(Chunk request) {
        if (DEBUG)
            Log.v("ddm-heap", "Handling " + name(request.type) + " chunk");
        int type = request.type;

        if (type == CHUNK_MPRS) {
            return handleMPRS(request);
        } else if (type == CHUNK_MPRE) {
            return handleMPRE(request);
        } else if (type == CHUNK_MPSS) {
            return handleMPSS(request);
        } else if (type == CHUNK_MPSE) {
            return handleMPSEOrSPSE(request, "Method");
        } else if (type == CHUNK_MPRQ) {
            return handleMPRQ(request);
        } else if (type == CHUNK_SPSS) {
            return handleSPSS(request);
        } else if (type == CHUNK_SPSE) {
            return handleMPSEOrSPSE(request, "Sample");
        } else {
            throw new RuntimeException("Unknown packet " + name(type));
        }
    }

    /*
     * Handle a "Method PRofiling Start" request.
     */
    private Chunk handleMPRS(Chunk request) {
        ByteBuffer in = wrapChunk(request);

        int bufferSize = in.getInt();
        int flags = in.getInt();
        int len = in.getInt();
        String fileName = getString(in, len);
        if (DEBUG)
            Log.v("ddm-heap", "Method profiling start: filename='" + fileName
                + "', size=" + bufferSize + ", flags=" + flags);

        try {
            Debug.startMethodTracing(fileName, bufferSize, flags);
            return null;        // empty response
        } catch (RuntimeException re) {
            return createFailChunk(1, re.getMessage());
        }
    }

    /*
     * Handle a "Method PRofiling End" request.
     */
    private Chunk handleMPRE(Chunk request) {
        byte result;

        try {
            Debug.stopMethodTracing();
            result = 0;
        } catch (RuntimeException re) {
            Log.w("ddm-heap", "Method profiling end failed: "
                + re.getMessage());
            result = 1;
        }

        /* create a non-empty reply so the handler fires on completion */
        byte[] reply = { result };
        return new Chunk(CHUNK_MPRE, reply, 0, reply.length);
    }

    /*
     * Handle a "Method Profiling w/Streaming Start" request.
     */
    private Chunk handleMPSS(Chunk request) {
        ByteBuffer in = wrapChunk(request);

        int bufferSize = in.getInt();
        int flags = in.getInt();
        if (DEBUG) {
            Log.v("ddm-heap", "Method prof stream start: size=" + bufferSize
                + ", flags=" + flags);
        }

        try {
            Debug.startMethodTracingDdms(bufferSize, flags, false, 0);
            return null;        // empty response
        } catch (RuntimeException re) {
            return createFailChunk(1, re.getMessage());
        }
    }

    /*
     * Handle a "Method Profiling w/Streaming End" request or a
     * "Sample Profiling w/Streaming End" request.
     */
    private Chunk handleMPSEOrSPSE(Chunk request, String type) {
        if (DEBUG) {
            Log.v("ddm-heap", type + " prof stream end");
        }

        try {
            Debug.stopMethodTracing();
        } catch (RuntimeException re) {
            Log.w("ddm-heap", type + " prof stream end failed: "
                + re.getMessage());
            return createFailChunk(1, re.getMessage());
        }

        /* VM sent the (perhaps very large) response directly */
        return null;
    }

    /*
     * Handle a "Method PRofiling Query" request.
     */
    private Chunk handleMPRQ(Chunk request) {
        int result = Debug.getMethodTracingMode();

        /* create a non-empty reply so the handler fires on completion */
        byte[] reply = { (byte) result };
        return new Chunk(CHUNK_MPRQ, reply, 0, reply.length);
    }

    /*
     * Handle a "Sample Profiling w/Streaming Start" request.
     */
    private Chunk handleSPSS(Chunk request) {
        ByteBuffer in = wrapChunk(request);

        int bufferSize = in.getInt();
        int flags = in.getInt();
        int interval = in.getInt();
        if (DEBUG) {
            Log.v("ddm-heap", "Sample prof stream start: size=" + bufferSize
                + ", flags=" + flags + ", interval=" + interval);
        }

        try {
            Debug.startMethodTracingDdms(bufferSize, flags, true, interval);
            return null;        // empty response
        } catch (RuntimeException re) {
            return createFailChunk(1, re.getMessage());
        }
    }
}

