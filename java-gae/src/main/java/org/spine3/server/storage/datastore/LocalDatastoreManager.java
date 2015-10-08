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

package org.spine3.server.storage.datastore;

import com.google.api.services.datastore.client.*;
import com.google.protobuf.Message;
import org.spine3.TypeName;

import static com.google.common.base.Throwables.propagate;

/**
 * Provides access to local Google Cloud Datastore.
 *
 * @author Alexander Litus
 */
public class LocalDatastoreManager<M extends Message> extends DatastoreManager<M> {

    private static final String PATH_TO_GCD = "C:\\gcd";
    private static final String LOCALHOST = "http://localhost:8080";
    private static final String LOCAL_DATASET_NAME = "spine-local-dataset";

    private static final DatastoreOptions DEFAULT_OPTIONS = new DatastoreOptions.Builder()
            .host(LOCALHOST)
            .dataset(LOCAL_DATASET_NAME)
            .build();

    private static final LocalDevelopmentDatastore LOCAL_DATASTORE = LocalDevelopmentDatastoreFactory.get().create(DEFAULT_OPTIONS);

    private LocalDatastoreManager(TypeName typeName) {
        super(LOCAL_DATASTORE, typeName);
    }

    public static <M extends Message> LocalDatastoreManager<M> newInstance(TypeName typeName) {
        return new LocalDatastoreManager<>(typeName);
    }

    /*
     * Starts the local Datastore server.
     */
    public void start() {
        try {
            LOCAL_DATASTORE.start(PATH_TO_GCD, LOCAL_DATASET_NAME);
        } catch (LocalDevelopmentDatastoreException e) {
            propagate(e);
        }
    }

    /*
     * Clears all data in the local Datastore.
     */
    public void clear() {
        try {
            LOCAL_DATASTORE.clear();
        } catch (LocalDevelopmentDatastoreException e) {
            propagate(e);
        }
    }

    /*
     * Stops the local Datastore server.
     */
    public void stop() {
        try {
            LOCAL_DATASTORE.stop();
        } catch (LocalDevelopmentDatastoreException e) {
            propagate(e);
        }
    }
}
