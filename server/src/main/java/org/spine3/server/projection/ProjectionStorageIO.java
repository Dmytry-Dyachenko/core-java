/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.projection;

import com.google.protobuf.Timestamp;
import org.apache.beam.sdk.transforms.DoFn;
import org.spine3.server.storage.RecordStorageIO;
import org.spine3.users.TenantId;

/**
 * Beam I/O support for projection storages.
 *
 * @author Alexander Yevsyukov
 */
public abstract class ProjectionStorageIO<I> extends RecordStorageIO<I> {

    protected ProjectionStorageIO(Class<I> idClass) {
        super(idClass);
    }

    public abstract WriteLastHandledEventTimeFn writeLastHandledEventTimeFn(TenantId tenantId);

    public abstract static class WriteLastHandledEventTimeFn extends DoFn<Timestamp, Void> {
        private static final long serialVersionUID = 0L;
        private final TenantId tenantId;

        protected WriteLastHandledEventTimeFn(TenantId tenantId) {
            this.tenantId = tenantId;
        }

        @ProcessElement
        public void processElement(ProcessContext c) {
            final Timestamp timestamp = c.element();
            doWrite(tenantId, timestamp);
        }

        protected abstract void doWrite(TenantId tenantId, Timestamp timestamp);
    }
}
