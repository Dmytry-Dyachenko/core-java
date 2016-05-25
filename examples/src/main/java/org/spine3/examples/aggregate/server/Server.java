/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.spine3.examples.aggregate.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.server.BoundedContext;
import org.spine3.server.ClientService;
import org.spine3.server.event.EventSubscriber;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;

import java.io.IOException;

import static org.spine3.client.ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT;

/**
 * Sample gRPC server implementation.
 *
 * @author Mikhail Melnik
 * @author Alexander Litus
 */
public class Server {

    private final StorageFactory storageFactory;
    private final BoundedContext boundedContext;
    private final EventSubscriber eventLogger = new EventLogger();
    private final io.grpc.Server grpcServer;

    /**
     * @param storageFactory the {@link StorageFactory} used to create and set up storages.
     */
    public Server(StorageFactory storageFactory) {
        this.storageFactory = storageFactory;

        this.boundedContext = BoundedContext.newBuilder()
                                            .setStorageFactory(storageFactory)
                                            .build();

        this.grpcServer = ClientService.createGrpcServer(this.boundedContext, DEFAULT_CLIENT_SERVICE_PORT);
    }

    /**
     * The entry point of the server application.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final StorageFactory storageFactory = InMemoryStorageFactory.getInstance();

        final Server server = new Server(storageFactory);
        server.start();
        server.awaitTermination();
    }

    /**
     * Starts the server.
     *
     * @throws IOException if unable to bind.
     */
    public void start() throws IOException {
        initBoundedContext();

        grpcServer.start();
        addShutdownHook(this);
        log().info("Server started, listening to commands on the port " + DEFAULT_CLIENT_SERVICE_PORT);
    }

    private void initBoundedContext() {
        // Register repository with the bounded context. This will register it in Command Bus too.
        final OrderRepository repository = new OrderRepository(boundedContext);

        //TODO:2016-05-25:alexander.yevsyukov: Make this operation in BoundedContext if a repository
        // does not have a storage assigned upon registration.
        repository.initStorage(storageFactory);

        boundedContext.register(repository);

        // Register event subscribers.
        boundedContext.getEventBus().subscribe(eventLogger);
    }

    /**
     * Closes the Bounded Context and stops the gRPC server.
     */
    public void shutdown() throws Exception {
        boundedContext.close();
        grpcServer.shutdown();
    }

    /**
     * Waits for the server to become terminated.
     */
    public void awaitTermination() throws InterruptedException {
        grpcServer.awaitTermination();
    }

    private static void addShutdownHook(final Server server) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            @SuppressWarnings("UseOfSystemOutOrSystemErr")
            @Override
            public void run() {
                final String serverClass = getClass().getName();
                System.err.println("Shutting down " + serverClass + "  since JVM is shutting down...");
                try {
                    server.shutdown();
                } catch (Exception e) {
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace(System.err);
                }
                System.err.println(serverClass + " shut down.");
            }
        }));
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(Server.class);
    }
}
