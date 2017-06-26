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

package io.spine.server.aggregate;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.client.ActorRequestFactory;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.IsSent;
import io.spine.grpc.StreamObservers;
import io.spine.protobuf.Wrapper;
import io.spine.protobuf.Wrappers;
import io.spine.server.BoundedContext;
import io.spine.server.command.Assign;
import io.spine.server.entity.idfunc.IdCommandFunction;
import io.spine.validate.StringValueVBuilder;
import org.junit.Before;
import org.junit.Test;

import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("OptionalGetWithoutIsPresent") // we do get() in assertions.
public class AggregateRepositoryViewsShould {

    private final ActorRequestFactory requestFactory = TestActorRequestFactory.newInstance(getClass());
    private BoundedContext boundedContext;
    /**
     * The default behaviour of an {@code AggregateRepository}.
     */
    private AggregateRepository<Long, SHAggregate> repository;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // It's on purpose for tests.
    private Optional<SHAggregate> aggregate;

    /** The Aggregate ID used in all tests */
    private static final Long id = 100L;

    @Before
    public void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        repository = new SHRepository(boundedContext);
        boundedContext.register(repository);

        // Create the aggregate instance.
        postCommand("createCommand");
    }

    /** Creates a command and posts it to {@code CommandBus}
     * for being processed by the repository. */
    private void postCommand(String cmd) {
        final Command command =
                requestFactory.command().create(SHRepository.createCommandMessage(id, cmd));
        boundedContext.getCommandBus().post(command, StreamObservers.<IsSent>noOpObserver());
    }

    @Test
    public void load_aggregate_if_no_status_flags_set() {
        aggregate = repository.find(id);

        assertTrue(aggregate.isPresent());
        final SHAggregate agg = aggregate.get();
        assertFalse(agg.isArchived());
        assertFalse(agg.isDeleted());
    }

    @Test
    public void not_load_aggregates_with_archived_status() {
        postCommand("archive");

        aggregate = repository.find(id);

        assertFalse(aggregate.isPresent());
    }

    @Test
    public void not_load_aggregates_with_deleted_status() {
        postCommand("delete");

        aggregate = repository.find(id);

        assertFalse(aggregate.isPresent());
    }

    /**
     * The aggregate that can handle status flags.
     *
     * <p>We use {@code StringValue} for messages to save on code generation
     * in the tests. Real aggregates should use generated messages.
     */
    @SuppressWarnings("RedundantMethodOverride") // We expose methods to the tests.
    private static class SHAggregate
            extends Aggregate<Long, StringValue, StringValueVBuilder> {
        private SHAggregate(Long id) {
            super(id);
        }

        @Assign
        StringValue handle(StringValue commandMessage) {
            final String msg = commandMessage.getValue();
            // Transform the command to the event (the fact in the past).
            return Wrapper.forString(msg + 'd');
        }

        @Apply
        private void on(StringValue eventMessage) {
            final String msg = SHRepository.getMessage(eventMessage);
            if (archived.name().equalsIgnoreCase(msg)) {
                setArchived(true);
            }
            if (deleted.name().equalsIgnoreCase(msg)) {
                setDeleted(true);
            }
            getBuilder().setValue(msg);
        }
    }

    /**
     * The aggregate repository under tests.
     *
     * <p>The repository accepts commands as {@code StringValue}s in the form:
     * {@code AggregateId-CommandMessage}.
     */
    private static class SHRepository extends AggregateRepository<Long, SHAggregate> {

        private static final char SEPARATOR = '-';

        private static StringValue createCommandMessage(Long id, String msg) {
            return Wrappers.format("%d%s" + msg, id, SEPARATOR);
        }

        private static Long getId(StringValue commandMessage) {
            return Long.valueOf(Splitter.on(SEPARATOR)
                                        .splitToList(commandMessage.getValue())
                                        .get(0));
        }

        private static String getMessage(StringValue commandMessage) {
            return Splitter.on(SEPARATOR)
                           .splitToList(commandMessage.getValue())
                           .get(1);
        }

        /**
         * Custom {@code IdCommandFunction} that parses an aggregate ID from {@code StringValue}.
         */
        private static final IdCommandFunction<Long, Message> parsingFunc =
                new IdCommandFunction<Long, Message>() {
            @Override
            public Long apply(Message message, CommandContext context) {
                final Long result = getId((StringValue)message);
                return result;
            }
        };

        private SHRepository(BoundedContext boundedContext) {
            super();
        }

        /**
         * Provides custom function for obtaining aggregate IDs from commands.
         */
        @SuppressWarnings("MethodDoesntCallSuperMethod") // This is the purpose of the method.
        @Override
        protected IdCommandFunction<Long, Message> getIdFunction() {
            return parsingFunc;
        }
    }
}