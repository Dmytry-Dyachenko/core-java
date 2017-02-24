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

package org.spine3.server.command;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import org.spine3.base.CommandClass;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandEnvelope;
import org.spine3.base.Event;
import org.spine3.server.event.EventBus;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * The abstract base for classes that expose command handling methods
 * and post their results to {@link EventBus}.
 *
 * <p>A command handler is responsible for:
 * <ol>
 *     <li>Changing the state of the business model in response to a command.
 *     <li>Producing corresponding events.
 *     <li>Posting events to {@code EventBus}.
 * </ol>
 *
 * <p>Event messages are returned as values of command handling methods.
 *
 * <p>A command handler does not have own state. So the state of the business
 * model it changes is external to it. Even though such a behaviour may be needed in
 * some rare cases, using {@linkplain org.spine3.server.aggregate.Aggregate aggregates}
 * is a preferred way of handling commands.
 *
 * <p>This class implements {@code CommandDispatcher} dispatching messages
 * to itself.
 *
 * @author Alexander Yevsyukov
 * @see org.spine3.server.aggregate.Aggregate Aggregate
 * @see CommandDispatcher
 */
public abstract class CommandHandler extends CommandHandlingEntity<String, Empty>
        implements CommandDispatcher {

    private final EventBus eventBus;

    @Nullable
    private Set<CommandClass> commandClasses;

    /**
     * Creates a new instance of the command handler.
     *
     * @param id       the ID which will be used for recognizing events produced by this handler
     * @param eventBus the {@code EventBus} to post events generated by this handler
     */
    protected CommandHandler(String id, EventBus eventBus) {
        super(id);
        this.eventBus = eventBus;
    }

    /**
     * Dispatches the command to the handler method and
     * posts resulting events to the {@link EventBus}.
     *
     * @param envelope the command to dispatch
     * @throws IllegalStateException if an exception occurred during command dispatching
     *                               with this exception as the cause
     */
    @Override
    public void dispatch(CommandEnvelope envelope) {
        handle(envelope.getMessage(), envelope.getCommandContext());
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField") // OK as we return immutable impl.
    @Override
    public Set<CommandClass> getMessageClasses() {
        if (commandClasses == null) {
            commandClasses = ImmutableSet.copyOf(getCommandClasses(getClass()));
        }
        return commandClasses;
    }

    /**
     * Dispatches the command to the handler method and
     * posts resulting events to the {@link EventBus}.
     *
     * @param commandMessage the command message
     * @param context        the command context
     * @throws IllegalStateException if an exception occurred during command dispatching
     *                               with this exception as the cause
     */
    void handle(Message commandMessage, CommandContext context) {
        final List<? extends Message> eventMessages = dispatchCommand(commandMessage, context);
        final List<Event> events = toEvents(eventMessages, context);
        postEvents(events);
    }

    /** Posts passed events to {@link EventBus}. */
    private void postEvents(Iterable<Event> events) {
        for (Event event : events) {
            eventBus.post(event);
        }
    }

    @Override
    @SuppressWarnings({"ConstantConditions" /* we compare with `null` */,
                       "MethodDoesntCallSuperMethod" /* OK as our state is only ID */})
    public boolean equals(Object otherObj) {
        if (this == otherObj) {
            return true;
        }
        if (otherObj == null ||
            getClass() != otherObj.getClass()) {
            return false;
        }
        final CommandHandler otherHandler = (CommandHandler) otherObj;
        final boolean equals = getId().equals(otherHandler.getId());
        return equals;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod") // OK as our state is only ID
    @Override
    public int hashCode() {
        final int result = getId().hashCode();
        return result;
    }
}
