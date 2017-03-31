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

package org.spine3.server.commandstore;

import org.spine3.base.Command;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.base.Error;
import org.spine3.base.Failure;
import org.spine3.server.commandbus.CommandRecord;
import org.spine3.server.entity.AbstractEntity;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.CommandStatus.ERROR;
import static org.spine3.base.CommandStatus.FAILURE;
import static org.spine3.base.CommandStatus.OK;

/**
 * An entity for storing a command and its processing status.
 *
 * @author Alexander Yevyukov
 */
class Entity extends AbstractEntity<CommandId, CommandRecord> {

    /**
     * {@inheritDoc}
     */
    private Entity(CommandId id) {
        super(id);
    }

    private static Entity create(CommandId commandId) {
        return new Entity(commandId);
    }

    static Entity createForStatus(Command command, CommandStatus status) {
        checkNotNull(command);
        checkNotNull(status);

        final CommandId commandId = command.getContext()
                                           .getCommandId();
        final Entity entity = create(commandId);
        entity.setCommandAndStatus(command, status);
        return entity;
    }

    static Entity createForError(Command command, Error error) {
        checkNotNull(command);
        checkNotNull(error);

        final CommandId id = Records.getOrGenerateCommandId(command);

        final Entity result = create(id);
        result.setError(id, command, error);
        return result;
    }

    private void setCommandAndStatus(Command command, CommandStatus status) {
        final CommandRecord record = Records.newRecordBuilder(command,
                                                              status,
                                                              null).build();
        updateState(record);
    }

    private void setError(CommandId id, Command command, Error error) {
        final CommandRecord.Builder builder = Records.newRecordBuilder(command, ERROR, id);
        builder.getStatusBuilder()
               .setError(error);
        final CommandRecord record = builder.build();
        updateState(record);
    }

    void setOkStatus() {
        final CommandRecord.Builder builder = getState().toBuilder();
        builder.getStatusBuilder()
               .setCode(OK);
        final CommandRecord record = builder.build();
        updateState(record);
    }

    void setToError(Error error) {
        final CommandRecord.Builder builder = getState().toBuilder();
        builder.getStatusBuilder()
               .setCode(ERROR)
               .setError(error);
        final CommandRecord record = builder.build();
        updateState(record);
    }

    void setToFailure(Failure failure) {
        final CommandRecord.Builder builder = getState().toBuilder();
        builder.getStatusBuilder()
               .setCode(FAILURE)
               .setFailure(failure);
        final CommandRecord record = builder.build();
        updateState(record);
    }
}
