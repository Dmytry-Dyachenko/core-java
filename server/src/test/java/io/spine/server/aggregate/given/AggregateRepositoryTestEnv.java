/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.aggregate.given;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.BoolValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import com.google.protobuf.util.Timestamps;
import io.spine.Identifier;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.EventContext;
import io.spine.core.MessageEnvelope;
import io.spine.core.React;
import io.spine.core.RejectionContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.AggregateRepositoryShould;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.entity.given.Given;
import io.spine.server.entity.rejection.CannotModifyArchivedEntity;
import io.spine.server.route.CommandRoute;
import io.spine.server.route.EventRoute;
import io.spine.server.route.RejectionRoute;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectVBuilder;
import io.spine.test.aggregate.Status;
import io.spine.test.aggregate.SubProjectList;
import io.spine.test.aggregate.SubProjectListVBuilder;
import io.spine.test.aggregate.command.AggAddTask;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.command.AggCreateProjectWithChildren;
import io.spine.test.aggregate.command.AggStartProject;
import io.spine.test.aggregate.command.AggStartProjectWithChildren;
import io.spine.test.aggregate.event.AggProjectArchived;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.event.AggProjectDeleted;
import io.spine.test.aggregate.event.AggProjectStarted;
import io.spine.test.aggregate.event.AggTaskAdded;
import io.spine.test.aggregate.rejection.AggCannotStartArchivedProject;
import io.spine.test.aggregate.rejection.Rejections;
import io.spine.testdata.Sample;
import io.spine.time.Time;
import io.spine.validate.BoolValueVBuilder;
import io.spine.validate.StringValueVBuilder;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

import static io.spine.core.Events.nothing;
import static io.spine.server.aggregate.AggregateMessageDispatcher.dispatchCommand;
import static java.util.Collections.emptyList;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"TypeMayBeWeakened", "ResultOfMethodCallIgnored"})
public class AggregateRepositoryTestEnv {

    private static final TestActorRequestFactory factory =
            TestActorRequestFactory.newInstance(AggregateRepositoryShould.class);

    private AggregateRepositoryTestEnv() {
        // Prevent instantiation of this utility class.
    }

    /** Generates a command for the passed message and wraps it into the envelope. */
    private static CommandEnvelope env(Message commandMessage) {
        return CommandEnvelope.of(factory.command()
                                         .create(commandMessage));
    }

    /** Utility factory for test aggregates. */
    public static class GivenAggregate {

        /** Prevents instantiation of this utility class. */
        private GivenAggregate() {
        }

        public static ProjectAggregate withUncommittedEvents() {
            return withUncommittedEvents(Sample.messageOfType(ProjectId.class));
        }

        public static ProjectAggregate withUncommittedEvents(ProjectId id) {
            final ProjectAggregate aggregate = Given.aggregateOfClass(ProjectAggregate.class)
                                                    .withId(id)
                                                    .build();

            final AggCreateProject createProject =
                    ((AggCreateProject.Builder) Sample.builderForType(AggCreateProject.class))
                            .setProjectId(id)
                            .build();
            final AggAddTask addTask =
                    ((AggAddTask.Builder) Sample.builderForType(AggAddTask.class))
                            .setProjectId(id)
                            .build();
            final AggStartProject startProject =
                    ((AggStartProject.Builder) Sample.builderForType(AggStartProject.class))
                            .setProjectId(id)
                            .build();

            dispatchCommand(aggregate, env(createProject));
            dispatchCommand(aggregate, env(addTask));
            dispatchCommand(aggregate, env(startProject));

            return aggregate;
        }
    }

    /**
     * The main aggregate class for positive scenarios tests.
     */
    public static class ProjectAggregate
            extends Aggregate<ProjectId, Project, ProjectVBuilder> {

        private ProjectAggregate(ProjectId id) {
            super(id);
        }

        @Override
        public int uncommittedEventsCount() {
            return super.uncommittedEventsCount();
        }

        @Assign
        AggProjectCreated handle(AggCreateProject msg) {
            return AggProjectCreated.newBuilder()
                                    .setProjectId(msg.getProjectId())
                                    .setName(msg.getName())
                                    .build();
        }

        @Apply
        private void apply(AggProjectCreated event) {
            getBuilder().setId(event.getProjectId())
                        .setName(event.getName());
        }

        @Assign
        AggTaskAdded handle(AggAddTask msg) {
            return AggTaskAdded.newBuilder()
                               .setProjectId(msg.getProjectId())
                               .setTask(msg.getTask())
                               .build();
        }

        @Apply
        private void apply(AggTaskAdded event) {
            getBuilder().setId(event.getProjectId())
                        .addTask(event.getTask());
        }

        @Assign
        AggProjectStarted handle(AggStartProject msg) {
            return AggProjectStarted.newBuilder()
                                    .setProjectId(msg.getProjectId())
                                    .build();
        }

        @Apply
        private void apply(AggProjectStarted event) {
            getBuilder().setStatus(Status.STARTED);
        }

        /**
         * Emits {@link AggProjectArchived} if the event is from the parent project.
         * Otherwise returns empty iterable.
         */
        @React
        private Iterable<AggProjectArchived> on(AggProjectArchived event) {
            if (event.getChildProjectIdList()
                     .contains(getId())) {
                return ImmutableList.of(AggProjectArchived.newBuilder()
                                                          .setProjectId(getId())
                                                          .build());
            }
            return nothing();
        }

        @Apply
        private void apply(AggProjectArchived event) {
            setArchived(true);
        }

        /**
         * Emits {@link AggProjectDeleted} if the event is from the parent project.
         * Otherwise returns empty iterable.
         */
        @React
        private Iterable<AggProjectDeleted> on(AggProjectDeleted event) {
            if (event.getChildProjectIdList()
                     .contains(getId())) {
                return ImmutableList.of(AggProjectDeleted.newBuilder()
                                                         .setProjectId(getId())
                                                         .build());
            }
            return nothing();
        }

        /**
         * {@inheritDoc}
         *
         * <p>Overrides to open the method to the test suite.
         */
        @Override
        @VisibleForTesting
        public void setArchived(boolean archived) {
            super.setArchived(archived);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Overrides to open the method to the test suite.
         */
        @Override
        @VisibleForTesting
        public void setDeleted(boolean deleted) {
            super.setDeleted(deleted);
        }
    }

    /**
     * The repository of positive scenarios {@linkplain ProjectAggregate  aggregates}.
     */
    @SuppressWarnings("SerializableInnerClassWithNonSerializableOuterClass")
    public static class ProjectAggregateRepository
            extends AggregateRepository<ProjectId, ProjectAggregate> {

        public static final ProjectId troublesome = ProjectId.newBuilder()
                                                             .setId("INVALID_ID")
                                                             .build();

        public ProjectAggregateRepository() {
            super();
            getEventRouting()
                    .route(AggProjectArchived.class,
                           new EventRoute<ProjectId, AggProjectArchived>() {
                               private static final long serialVersionUID = 0L;

                               @Override
                               public Set<ProjectId> apply(AggProjectArchived msg,
                                                           EventContext ctx) {
                                   return ImmutableSet.copyOf(msg.getChildProjectIdList());
                               }
                           })
                    .route(AggProjectDeleted.class,
                           new EventRoute<ProjectId, AggProjectDeleted>() {
                               private static final long serialVersionUID = 0L;

                               @Override
                               public Set<ProjectId> apply(AggProjectDeleted msg,
                                                           EventContext ctx) {
                                   return ImmutableSet.copyOf(msg.getChildProjectIdList());
                               }
                           });
        }

        @Override
        public Optional<ProjectAggregate> find(ProjectId id) {
            if (id.equals(troublesome)) {
                return Optional.absent();
            }
            return super.find(id);
        }
    }

    /**
     * The aggregate which throws {@link IllegalArgumentException} in response to negative numbers.
     *
     * <p>Normally aggregates should reject commands via command rejections. This class is test
     * environment for testing of now
     * {@linkplain AggregateRepository#logError(String, MessageEnvelope, RuntimeException) logs
     * errors}.
     *
     * @see FailingAggregateRepository
     */
    static class FailingAggregate extends Aggregate<Long, StringValue, StringValueVBuilder> {

        private FailingAggregate(Long id) {
            super(id);
        }

        @SuppressWarnings("NumericCastThatLosesPrecision") // Int. part as ID.
        static long toId(FloatValue message) {
            final float floatValue = message.getValue();
            return (long) Math.abs(floatValue);
        }

        /** Rejects a negative value via command rejection. */
        @Assign
        Timestamp on(UInt32Value value) {
            if (value.getValue() < 0) {
                throw new IllegalArgumentException("Negative value passed");
            }
            return Time.getCurrentTime();
        }

        /** Rejects a negative value via command rejection. */
        @Assign
        Timestamp on(UInt64Value value) throws CannotModifyArchivedEntity {
            if (value.getValue() < 0) {
                throw new CannotModifyArchivedEntity(Identifier.pack(getId()));
            }
            return Time.getCurrentTime();
        }

        /** Invalid command handler, which does not produce events. */
        @Assign
        List<Message> on(Int32Value value) {
            return emptyList();
        }

        @Apply
        void apply(Timestamp timestamp) {
            getBuilder().setValue(getState().getValue()
                                          + System.lineSeparator()
                                          + Timestamps.toString(timestamp));
        }

        @React
        Timestamp on(FloatValue value) {
            final float floatValue = value.getValue();
            if (floatValue < 0) {
                final long longValue = toId(value);
                // Complain only if the passed value represents ID of this aggregate.
                // This would allow other aggregates react on this message.
                if (longValue == getId()) {
                    throw new IllegalArgumentException("Negative floating point value passed");
                }
            }
            return Time.getCurrentTime();
        }
    }

    /**
     * The repository of {@link FailingAggregate}s.
     */
    public static class FailingAggregateRepository
            extends AggregateRepository<Long, FailingAggregate> {

        private boolean errorLogged;
        @Nullable
        private MessageEnvelope lastErrorEnvelope;
        @Nullable
        private RuntimeException lastException;

        @SuppressWarnings("SerializableInnerClassWithNonSerializableOuterClass")
        public FailingAggregateRepository() {
            super();
            getCommandRouting().replaceDefault(
                    // Simplistic routing function that takes absolute value as ID.
                    new CommandRoute<Long, Message>() {
                        private static final long serialVersionUID = 0L;

                        @Override
                        public Long apply(Message message, CommandContext context) {
                            if (message instanceof UInt32Value) {
                                UInt32Value uInt32Value = (UInt32Value) message;
                                return (long) Math.abs(uInt32Value.getValue());
                            }
                            return 0L;
                        }
                    }
            );

            getEventRouting().replaceDefault(
                    new EventRoute<Long, Message>() {
                        private static final long serialVersionUID = 0L;

                        /**
                         * Returns several entity identifiers to check error isolation.
                         * @see FailingAggregate#on(FloatValue)
                         */
                        @Override
                        public Set<Long> apply(Message message, EventContext context) {
                            if (message instanceof FloatValue) {
                                final long absValue = FailingAggregate.toId((FloatValue) message);
                                return ImmutableSet.of(absValue, absValue + 100, absValue + 200);
                            }
                            return ImmutableSet.of(1L, 2L);
                        }
                    });
        }

        @Override
        protected void logError(String msgFormat,
                                MessageEnvelope envelope,
                                RuntimeException exception) {
            super.logError(msgFormat, envelope, exception);
            errorLogged = true;
            lastErrorEnvelope = envelope;
            lastException = exception;
        }

        public boolean isErrorLogged() {
            return errorLogged;
        }

        @Nullable
        public MessageEnvelope getLastErrorEnvelope() {
            return lastErrorEnvelope;
        }

        @Nullable
        public RuntimeException getLastException() {
            return lastException;
        }
    }

    /**
     * An aggregate class which neither handles commands nor reacts on events or rejections.
     */
    static class AnemicAggregate extends Aggregate<Integer, BoolValue, BoolValueVBuilder> {
        private AnemicAggregate(Integer id) {
            super(id);
        }
    }

    /**
     * The repository of {@link AnemicAggregate}.
     */
    public static class AnemicAggregateRepository
            extends AggregateRepository<Integer, AnemicAggregate> {
    }

    /**
     * An aggregate class that reacts only on events and does not handle commands.
     */
    public static class ReactingAggregate
            extends Aggregate<ProjectId, StringValue, StringValueVBuilder> {

        public static final String PROJECT_ARCHIVED = "PROJECT_ARCHIVED";
        public static final String PROJECT_DELETED = "PROJECT_DELETED";

        private ReactingAggregate(ProjectId id) {
            super(id);
        }

        /**
         * Emits {@link AggProjectArchived} if the event is from the parent project.
         * Otherwise returns empty iterable.
         */
        @React
        private Iterable<AggProjectArchived> on(AggProjectArchived event) {
            if (event.getChildProjectIdList()
                     .contains(getId())) {
                return ImmutableList.of(AggProjectArchived.newBuilder()
                                                          .setProjectId(getId())
                                                          .build());
            }
            return nothing();
        }

        /**
         * Emits {@link AggProjectDeleted} if the event is from the parent project.
         * Otherwise returns empty iterable.
         */
        @React
        private Iterable<AggProjectDeleted> on(AggProjectDeleted event) {
            if (event.getChildProjectIdList()
                     .contains(getId())) {
                return ImmutableList.of(AggProjectDeleted.newBuilder()
                                                         .setProjectId(getId())
                                                         .build());
            }
            return nothing();
        }

        @Apply
        private void apply(AggProjectArchived event) {
            getBuilder().setValue(PROJECT_ARCHIVED);
        }

        @Apply
        private void apply(AggProjectDeleted event) {
            getBuilder().setValue(PROJECT_DELETED);
        }
    }

    /**
     * The repository of {@link ReactingAggregate}.
     */
    @SuppressWarnings("SerializableInnerClassWithNonSerializableOuterClass")
    public static class ReactingRepository
            extends AggregateRepository<ProjectId, ReactingAggregate> {

        public ReactingRepository() {
            super();
            getEventRouting()
                    .route(AggProjectArchived.class,
                           new EventRoute<ProjectId, AggProjectArchived>() {
                               private static final long serialVersionUID = 0L;

                               @Override
                               public Set<ProjectId> apply(AggProjectArchived message,
                                                           EventContext context) {
                                   return ImmutableSet.copyOf(message.getChildProjectIdList());
                               }
                           });
        }
    }

    /*
     * Test environment for testing aggregates reacting on rejections.
     *******************************************************************/

    /**
     * The aggregate with the state of {@linkplain AggProjectCreated creation event}
     * wrapped into {@code Any}. The event keeps the list of child project IDs, which we use
     * in rejecting subsequent commands.
     */
    static class RejectingAggregate
            extends Aggregate<ProjectId, SubProjectList, SubProjectListVBuilder> {

        private RejectingAggregate(ProjectId id) {
            super(id);
        }

        @Assign
        AggProjectCreated on(AggCreateProjectWithChildren cmd) {
            return AggProjectCreated.newBuilder()
                                    .setProjectId(cmd.getProjectId())
                                    .addAllChildProjectId(cmd.getChildProjectIdList())
                                    .build();
        }

        @Apply
        void event(AggProjectCreated event) {
            getBuilder().addAllItem(event.getChildProjectIdList());
        }

        @Assign
        AggProjectStarted on(AggStartProjectWithChildren cmd) throws AggCannotStartArchivedProject {
            throw new AggCannotStartArchivedProject(getId(), getState().getItemList());
        }
    }

    public static class RejectingRepository
            extends AggregateRepository<ProjectId, RejectingAggregate> {
    }

    /**
     * An aggregate class that reacts only on rejections, and neither handles commands, nor
     * reacts on events.
     */
    public static class RejectionReactingAggregate
            extends Aggregate<ProjectId, StringValue, StringValueVBuilder> {

        public static final String PARENT_ARCHIVED = "parent-archived";

        private RejectionReactingAggregate(ProjectId id) {
            super(id);
        }

        @React
        private Iterable<AggProjectArchived> on(
                Rejections.AggCannotStartArchivedProject rejection) {
            final List<ProjectId> childIdList = rejection.getChildProjectIdList();
            if (childIdList.contains(getId())) {
                return ImmutableList.of(AggProjectArchived.newBuilder()
                                                          .setProjectId(getId())
                                                          .build());
            }
            return nothing();
        }

        @Apply
        void event(AggProjectArchived event) {
            getBuilder().setValue(PARENT_ARCHIVED);
        }
    }

    @SuppressWarnings("SerializableInnerClassWithNonSerializableOuterClass")
    public static class RejectionReactingRepository
            extends AggregateRepository<ProjectId, RejectionReactingAggregate> {

        public RejectionReactingRepository() {
            super();
            getRejectionRouting()
                    .route(Rejections.AggCannotStartArchivedProject.class,
                           new RejectionRoute<ProjectId,
                                   Rejections.AggCannotStartArchivedProject>() {
                               private static final long serialVersionUID = 0L;

                               @Override
                               public Set<ProjectId> apply(
                                       Rejections.AggCannotStartArchivedProject message,
                                       RejectionContext context) {
                                   return ImmutableSet.copyOf(message.getChildProjectIdList());
                               }
                           });
        }
    }
}
