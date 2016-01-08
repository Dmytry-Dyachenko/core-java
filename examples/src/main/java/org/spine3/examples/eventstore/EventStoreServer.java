/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.examples.eventstore;

import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.server.EventStore;
import org.spine3.server.storage.memory.InMemoryStorageFactory;

/**
 * @author Alexander Yevsyukov
 */
public class EventStoreServer {

    /* The port on which the server should run */
    private static final int port = 50051;
    private Server server;

    private void start() throws InterruptedException {
        server = EventStore.createServer(
                       MoreExecutors.directExecutor(),
                       InMemoryStorageFactory.getInstance().createEventStorage(),
                       port);
        log().info("EventStore server started. Listening on port: " + port);
        addShutdownHook();
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            @SuppressWarnings("UseOfSystemOutOrSystemErr")
            @Override
            public void run() {
                System.err.println("Shutting down the EventStore server since JVM is shutting down...");
                server.shutdown();
                System.err.println("EventStore server shut down.");
            }
        }));
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        final EventStoreServer server = new EventStoreServer();
        server.start();
        server.blockUntilShutdown();
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(EventStoreServer.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

}
