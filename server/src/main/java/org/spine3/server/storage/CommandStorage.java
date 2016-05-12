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

package org.spine3.server.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.protobuf.Message;
import org.spine3.SPI;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.base.Commands;
import org.spine3.base.Error;
import org.spine3.base.Failure;
import org.spine3.server.command.CommandStore;
import org.spine3.server.command.CommandValidator;
import org.spine3.server.entity.GetTargetIdFromCommand;
import org.spine3.type.TypeName;

import javax.annotation.Nullable;
import java.util.Iterator;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.spine3.base.CommandStatus.ERROR;
import static org.spine3.base.CommandStatus.RECEIVED;
import static org.spine3.base.Commands.generateId;
import static org.spine3.base.Commands.getId;
import static org.spine3.base.Identifiers.EMPTY_ID;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.validate.Validate.checkNotDefault;

/**
 * A storage used by {@link CommandStore} for keeping command data.
 *
 * @author Alexander Yevsyukov
 */
@SPI
public abstract class CommandStorage extends AbstractStorage<CommandId, CommandStorageRecord> {

    /**
     * Stores a command with the {@link CommandStatus#RECEIVED} status by a command ID from a command context.
     *
     * <p>Rewrites it if a command with such command ID already exists in the storage.
     *
     * @param command a complete command to store
     * @throws IllegalStateException if the storage is closed
     */
    public void store(Command command) {
        store(command, RECEIVED);
    }

    /**
     * Stores a command with the given status by a command ID from a command context.
     *
     * @param command a complete command to store
     * @param status a command status
     * @throws IllegalStateException if the storage is closed
     */
    public void store(Command command, CommandStatus status) {
        checkNotClosed();
        CommandValidator.checkCommand(command);

        final CommandStorageRecord record = newCommandStorageRecordBuilder(command, status).build();
        final CommandId commandId = getId(command);
        write(commandId, record);
    }

    /**
     * Stores a command with the {@link CommandStatus#ERROR} status by a command ID from a command context.
     *
     * <p>If there is no ID, a new one is generated is used.
     *
     * @param command a command to store
     * @param error an error occurred
     * @throws IllegalStateException if the storage is closed
     */
    public void store(Command command, Error error) {
        checkNotClosed();
        checkNotDefault(error);
        CommandId id = getId(command);
        if (idToString(id).equals(EMPTY_ID)) {
            id = generateId();
        }
        final CommandStorageRecord record = newCommandStorageRecordBuilder(command, ERROR)
                .setError(error)
                .setCommandId(idToString(id))
                .build();
        write(id, record);
    }

    /**
     * Loads all commands with the given status.
     *
     * @param status a command status to search by
     * @return commands with the given status
     * @throws IllegalStateException if the storage is closed
     */
    public Iterator<Command> load(CommandStatus status) {
        checkNotClosed();
        final Iterator<CommandStorageRecord> recordIterator = read(status);
        final Iterator<Command> commandIterator = toCommandIterator(recordIterator);
        return commandIterator;
    }

    /**
     * Reads all command records with the given status.
     *
     * @param status a command status to search by
     * @return records with the given status
     */
    protected abstract Iterator<CommandStorageRecord> read(CommandStatus status);

    /**
     * Updates the status of the command to {@link CommandStatus#OK}
     */
    public abstract void setOkStatus(CommandId commandId);

    /**
     * Updates the status of the command with the error.
     */
    public abstract void updateStatus(CommandId commandId, Error error);

    /**
     * Updates the status of the command with the business failure.
     */
    public abstract void updateStatus(CommandId commandId, Failure failure);

    /**
     * Creates a command storage record builder passed on the passed parameters.
     *
     * <p>{@code targetId} and {@code targetIdType} are set to empty strings if the command is not for an entity.
     *
     * @param command a command to convert to a record
     * @param status a command status to set to a record
     * @return a storage record
     */
    @VisibleForTesting
    /* package */ static CommandStorageRecord.Builder newCommandStorageRecordBuilder(Command command,
                                                                                     CommandStatus status) {
        final CommandContext context = command.getContext();

        final CommandId commandId = context.getCommandId();
        final String commandIdString = idToString(commandId);

        final Message commandMessage = Commands.getMessage(command);
        final String commandType = TypeName.of(commandMessage).nameOnly();

        final Object targetId = GetTargetIdFromCommand.asNullableObject(commandMessage);
        final String targetIdString;
        final String targetIdType;
        if (targetId != null) {
            targetIdString = idToString(targetId);
            targetIdType = targetId.getClass().getName();
        } else { // the command is not for an entity
            targetIdString = "";
            targetIdType = "";
        }

        final CommandStorageRecord.Builder builder = CommandStorageRecord.newBuilder()
                .setMessage(command.getMessage())
                .setTimestamp(getCurrentTime())
                .setCommandType(commandType)
                .setCommandId(commandIdString)
                .setStatus(status)
                .setTargetIdType(targetIdType)
                .setTargetId(targetIdString)
                .setContext(context);
        return builder;
    }

    /**
     * Converts {@code CommandStorageRecord}s to {@code Command}s.
     */
    @VisibleForTesting
    /* package */ static Iterator<Command> toCommandIterator(Iterator<CommandStorageRecord> records) {
        return Iterators.transform(records, TO_COMMAND);
    }

    private static final Function<CommandStorageRecord, Command> TO_COMMAND = new Function<CommandStorageRecord, Command>() {
        @Override
        public Command apply(@Nullable CommandStorageRecord record) {
            if (record == null) {
                return Command.getDefaultInstance();
            }
            final Command.Builder builder = Command.newBuilder()
                                                   .setMessage(record.getMessage())
                                                   .setContext(record.getContext());
            return builder.build();
        }
    };
}
