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

package org.spine3.server.storage;

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import org.spine3.server.entity.Visibility;

/**
 * A storage that allows to update visibility status of entities.
 *
 * @author Alexander Yevsyukov
 */
public interface StorageWithVisibility<I, R extends Message> extends Storage<I, R> {

    /**
     * Reads the visibility status for the entity with the passed ID.
     *
     * <p>This method returns {@code Optional.absent()} if none of the
     * flags of visibility flags were set before. This means that
     * the entity is visible to the regular queries.
     *
     * @param id the ID of the entity
     * @return the aggregate visibility or {@code Optional.absent()}
     */
    Optional<Visibility> readVisibility(I id);

    /**
     * Writes the visibility status for the entity with the passed ID.
     *
     * @param id         the ID of the entity for which to update the status
     * @param visibility the status to write
     */
    void writeVisibility(I id, Visibility visibility);
}
