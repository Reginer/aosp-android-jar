/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.layoutlib.bridge.remote.server;

import com.android.layout.remote.api.RemoteBridge;
import com.android.tools.layoutlib.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Main server class. The main method will start an RMI server for the {@link RemoteBridgeImpl}
 * class.
 */
public class ServerMain {
    private static final String RUNNING_SERVER_STR = "Server is running on port ";
    public static int REGISTRY_BASE_PORT = 9000;

    private final int mPort;
    private final Registry mRegistry;

    private ServerMain(int port, @NotNull Registry registry) {
        mPort = port;
        mRegistry = registry;
    }

    public int getPort() {
        return mPort;
    }

    public void stop() {
        try {
            UnicastRemoteObject.unexportObject(mRegistry, true);
        } catch (NoSuchObjectException ignored) {
        }
    }

    private static Thread createOutputProcessor(String outputProcessorName,
            InputStream inputStream,
            Consumer<String> consumer) {
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(inputStream));
        Thread thread = new Thread(() -> inputReader.lines().forEach(consumer));
        thread.setName(outputProcessorName);
        thread.start();
        return thread;
    }

    /**
     * This will start a new JVM and connect to the new JVM RMI registry.
     * <p/>
     * The server will start looking for ports available for the {@link Registry} until a free one
     * is found. The port number will be returned.
     * If no ports are available, a {@link RemoteException} will be thrown.
     * @param basePort port number to start looking for available ports
     * @param limit number of ports to check. The last port to be checked will be (basePort + limit)
     */
    public static ServerMain forkAndStartServer(int basePort, int limit)
            throws IOException, InterruptedException {
        // We start a new VM by copying all the parameter that we received in the current VM.
        // We only remove the agentlib parameter since that could cause a port collision and avoid
        // the new VM from starting.
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments().stream()
                .filter(arg -> !arg.contains("-agentlib")) // Filter agentlib to avoid conflicts
                .collect(Collectors.toList());

        Path javaPath = Paths.get(System.getProperty("java.home"), "bin", "java");
        String thisClassName = ServerMain.class.getName()
                .replace('.','/');

        List<String> cmd = new ArrayList<>();
        cmd.add(javaPath.toString());

        // Inherited arguments
        cmd.addAll(arguments);

        // Classpath
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));

        // Class name and path
        cmd.add(thisClassName);

        // ServerMain parameters [basePort. limit]
        cmd.add(Integer.toString(basePort));
        cmd.add(Integer.toString(limit));

        Process process = new ProcessBuilder()
                .command(cmd)
                .start();

        BlockingQueue<String> outputQueue = new ArrayBlockingQueue<>(10);
        Thread outputThread = createOutputProcessor("output", process.getInputStream(),
                outputQueue::offer);
        Thread errorThread = createOutputProcessor("error", process.getErrorStream(),
                System.err::println);

        Runnable killServer = () -> {
            process.destroyForcibly();
            outputThread.interrupt();
            errorThread.interrupt();
            try {
                outputThread.join();
            } catch (InterruptedException ignore) {
            }

            try {
                errorThread.join();
            } catch (InterruptedException ignore) {
            }
        };

        // Try to read the "Running on port" line in 10 lines. If it's not there just fail.
        for (int i = 0; i < 10; i++) {
            String line = outputQueue.poll(5, TimeUnit.SECONDS);

            if (line != null && line.startsWith(RUNNING_SERVER_STR)) {
                int runningPort = Integer.parseInt(line.substring(RUNNING_SERVER_STR.length()));
                System.out.println("Running on port " + runningPort);

                // We already know where the server is running so we just need to get the registry
                // and return our own instance of ServerMain
                Registry registry = LocateRegistry.getRegistry(runningPort);
                return new ServerMain(runningPort, registry) {
                    @Override
                    public void stop() {
                        killServer.run();
                    }
                };
            }
        }

        killServer.run();
        throw new IOException("Unable to find start string");
    }

    /**
     * The server will start looking for ports available for the {@link Registry} until a free one
     * is found. The port number will be returned.
     * If no ports are available, a {@link RemoteException} will be thrown.
     * @param basePort port number to start looking for available ports
     * @param limit number of ports to check. The last port to be checked will be (basePort + limit)
     */
    private static ServerMain startServer(int basePort, int limit) throws RemoteException {
        RemoteBridgeImpl remoteBridge = new RemoteBridgeImpl();
        RemoteBridge stub = (RemoteBridge) UnicastRemoteObject.exportObject(remoteBridge, 0);

        RemoteException lastException = null;
        for (int port = basePort; port <= basePort + limit; port++) {
            try {
                Registry registry = LocateRegistry.createRegistry(port);
                registry.rebind(RemoteBridge.class.getName(), stub);
                return new ServerMain(port, registry);
            } catch (RemoteException e) {
                lastException = e;
            }
        }

        if (lastException == null) {
            lastException = new RemoteException("Unable to start server");
        }

        throw lastException;
    }

    /**
     * Starts an RMI server that runs in the current JVM. Only for debugging.
     */
    public static ServerMain startLocalJvmServer() throws RemoteException {
        System.err.println("Starting server in the local JVM");
        return startServer(REGISTRY_BASE_PORT, 10);
    }

    public static void main(String[] args) throws RemoteException {
        int basePort = args.length > 0 ? Integer.parseInt(args[0]) : REGISTRY_BASE_PORT;
        int limit = args.length > 1 ? Integer.parseInt(args[1]) : 10;

        ServerMain server = startServer(basePort, limit);
        System.out.println(RUNNING_SERVER_STR + server.getPort());
    }
}
