/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.internal.protolog;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.ServiceManager;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.protolog.IProtoLogConfigurationService.RegisterClientArgs;
import com.android.internal.protolog.common.ILogger;
import com.android.internal.protolog.common.IProtoLogGroup;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class ProcessedPerfettoProtoLogImpl extends PerfettoProtoLogImpl {
    private static final String LOG_TAG = "PerfettoProtoLogImpl";

    @NonNull
    private final ProtoLogViewerConfigReader mViewerConfigReader;
    @Deprecated
    @NonNull
    private final ViewerConfigInputStreamProvider mViewerConfigInputStreamProvider;
    @NonNull
    private final String mViewerConfigFilePath;

    public ProcessedPerfettoProtoLogImpl(
            @NonNull ProtoLogDataSource datasource,
            @NonNull String viewerConfigFilePath,
            @NonNull ProtoLogCacheUpdater cacheUpdater,
            @NonNull IProtoLogGroup[] groups) throws ServiceManager.ServiceNotFoundException {
        this(datasource, viewerConfigFilePath, new ViewerConfigInputStreamProvider() {
                    @NonNull
                    @Override
                    public AutoClosableProtoInputStream getInputStream() {
                        try {
                            final var protoFileInputStream =
                                    new FileInputStream(viewerConfigFilePath);
                            return new AutoClosableProtoInputStream(protoFileInputStream);
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(
                                    "Failed to load viewer config file " + viewerConfigFilePath, e);
                        }
                    }
                },
                cacheUpdater, groups);
    }

    @VisibleForTesting
    public ProcessedPerfettoProtoLogImpl(
            @NonNull ProtoLogDataSource datasource,
            @NonNull String viewerConfigFilePath,
            @NonNull ViewerConfigInputStreamProvider viewerConfigInputStreamProvider,
            @NonNull ProtoLogCacheUpdater cacheUpdater,
            @NonNull IProtoLogGroup[] groups) throws ServiceManager.ServiceNotFoundException {
        super(datasource, cacheUpdater, groups);

        this.mViewerConfigFilePath = viewerConfigFilePath;

        this.mViewerConfigInputStreamProvider = viewerConfigInputStreamProvider;
        this.mViewerConfigReader = new ProtoLogViewerConfigReader(viewerConfigInputStreamProvider);

        loadLogcatGroupsViewerConfig(groups);
    }

    @VisibleForTesting
    public ProcessedPerfettoProtoLogImpl(
            @NonNull ProtoLogDataSource datasource,
            @NonNull String viewerConfigFilePath,
            @NonNull ViewerConfigInputStreamProvider viewerConfigInputStreamProvider,
            @NonNull ProtoLogViewerConfigReader viewerConfigReader,
            @NonNull ProtoLogCacheUpdater cacheUpdater,
            @NonNull IProtoLogGroup[] groups,
            @Nullable IProtoLogConfigurationService configurationService)
            throws ServiceManager.ServiceNotFoundException {
        super(datasource, cacheUpdater, groups, configurationService);

        this.mViewerConfigFilePath = viewerConfigFilePath;

        this.mViewerConfigInputStreamProvider = viewerConfigInputStreamProvider;
        this.mViewerConfigReader = viewerConfigReader;

        loadLogcatGroupsViewerConfig(groups);
    }

    @NonNull
    @Override
    protected RegisterClientArgs createConfigurationServiceRegisterClientArgs() {
        var args = new RegisterClientArgs();
        args.viewerConfigFile = mViewerConfigFilePath;
        return args;
    }

    /**
     * Start text logging
     * @param groups Groups to start text logging for
     * @param logger A logger to write status updates to
     * @return status code
     */
    @Override
    public int startLoggingToLogcat(String[] groups, @NonNull ILogger logger) {
        if (!validateGroups(logger, groups)) {
            return -1;
        }

        mViewerConfigReader.loadViewerConfig(groups, logger);
        return super.startLoggingToLogcat(groups, logger);
    }

    /**
     * Stop text logging
     * @param groups Groups to start text logging for
     * @param logger A logger to write status updates to
     * @return status code
     */
    @Override
    public int stopLoggingToLogcat(String[] groups, @NonNull ILogger logger) {
        if (!validateGroups(logger, groups)) {
            return -1;
        }

        var status = super.stopLoggingToLogcat(groups, logger);

        if (status != 0) {
            throw new RuntimeException("Failed to stop logging to logcat");
        }

        // If we successfully disabled logging, unload the viewer config.
        mViewerConfigReader.unloadViewerConfig(groups, logger);
        return status;
    }

    @Deprecated
    @Override
    void dumpViewerConfig() {
        Log.d(LOG_TAG, "Dumping viewer config to trace from " + mViewerConfigFilePath);
        Utils.dumpViewerConfig(mDataSource, mViewerConfigInputStreamProvider);
        Log.d(LOG_TAG, "Successfully dumped viewer config to trace from " + mViewerConfigFilePath);
    }

    @NonNull
    @Override
    String getLogcatMessageString(@NonNull Message message) {
        String messageString;
        messageString = message.getMessage(mViewerConfigReader);

        if (messageString == null) {
            // Either we failed to load the config for this log message from the viewer config file
            // into memory, or the message hash is simply not available in the viewer config file.
            // We want to confirm that the message hash is not available in the viewer config file
            // before throwing an exception.
            throw new RuntimeException(getReasonForFailureToGetMessageString(message));
        }

        return messageString;
    }

    private String getReasonForFailureToGetMessageString(Message message) {
        if (message.getMessageHash() == null) {
            return "Trying to get message from null message hash";
        }

        try {
            if (mViewerConfigReader.messageHashIsAvailableInFile(message.getMessageHash())) {
                return "Failed to decode message for logcat logging. "
                        + "Message hash (" + message.getMessageHash() + ") is not available in "
                        + "viewerConfig file (" +  mViewerConfigFilePath + "). This might be due "
                        + "to the viewer config file and the executing code being out of sync.";
            } else {
                return "Failed to decode message for logcat. "
                        + "Message hash (" + message.getMessageHash() + ") was available in the "
                        + "viewerConfig file (" +  mViewerConfigFilePath + ") but wasn't loaded "
                        + "into memory from file before decoding! This is likely a bug.";
            }
        } catch (IOException e) {
            return "Failed to get string message to log but could not identify the root cause due "
                    + "to an IO error in reading the viewer config file.";
        }
    }

    private void loadLogcatGroupsViewerConfig(@NonNull IProtoLogGroup[] protoLogGroups) {
        final var groupsLoggingToLogcat = new ArrayList<String>();
        for (IProtoLogGroup protoLogGroup : protoLogGroups) {
            if (protoLogGroup.isLogToLogcat()) {
                groupsLoggingToLogcat.add(protoLogGroup.name());
            }
        }

        // Load in background to avoid delay in boot process.
        // The caveat is that any log message that is also logged to logcat will not be
        // successfully decoded until this completes.
        mBackgroundLoggingService.execute(() -> {
            mViewerConfigReader.loadViewerConfig(groupsLoggingToLogcat.toArray(new String[0]));
            readyToLogToLogcat();
        });
    }
}
