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

package io.spine.server.procman;

import com.google.common.collect.Lists;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.Identifier;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.core.Subscribe;
import io.spine.core.TenantId;
import io.spine.core.given.GivenEvent;
import io.spine.server.BoundedContext;
import io.spine.server.entity.RecordBasedRepository;
import io.spine.server.entity.RecordBasedRepositoryShould;
import io.spine.server.entity.given.Given;
import io.spine.server.entity.rejection.Rejections.EntityAlreadyArchived;
import io.spine.server.entity.rejection.Rejections.EntityAlreadyDeleted;
import io.spine.server.event.EventSubscriber;
import io.spine.server.procman.given.ProcessManagerRepositoryTestEnv.TestProcessManager;
import io.spine.server.procman.given.ProcessManagerRepositoryTestEnv.TestProcessManagerRepository;
import io.spine.test.procman.Project;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.command.PmAddTask;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.command.PmStartProject;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.event.PmTaskAdded;
import io.spine.testdata.Sample;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

import static io.spine.Identifier.newUuid;
import static io.spine.core.Rejections.createRejection;
import static io.spine.server.TestCommandClasses.assertContains;
import static io.spine.server.TestEventClasses.assertContains;
import static io.spine.server.TestRejectionClasses.assertContains;
import static io.spine.server.procman.ProcessManagerRepositoryShould.GivenCommandMessage.ID;
import static io.spine.server.procman.ProcessManagerRepositoryShould.GivenCommandMessage.addTask;
import static io.spine.server.procman.ProcessManagerRepositoryShould.GivenCommandMessage.createProject;
import static io.spine.server.procman.ProcessManagerRepositoryShould.GivenCommandMessage.projectCreated;
import static io.spine.server.procman.ProcessManagerRepositoryShould.GivenCommandMessage.projectStarted;
import static io.spine.server.procman.ProcessManagerRepositoryShould.GivenCommandMessage.startProject;
import static io.spine.server.procman.ProcessManagerRepositoryShould.GivenCommandMessage.taskAdded;
import static io.spine.test.TestValues.newUuidValue;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
public class ProcessManagerRepositoryShould
        extends RecordBasedRepositoryShould<TestProcessManager,
        ProjectId,
        Project> {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass(),
                                                TenantId.newBuilder()
                                                        .setValue(newUuid())
                                                        .build());
    private BoundedContext boundedContext;

    public static class GivenCommandMessage {

        public static final ProjectId ID = Sample.messageOfType(ProjectId.class);

        /** Prevents instantiation on this utility class. */
        private GivenCommandMessage() {
        }

        public static PmCreateProject createProject() {
            return ((PmCreateProject.Builder) Sample.builderForType(PmCreateProject.class))
                    .setProjectId(ID)
                    .build();
        }

        public static PmStartProject startProject() {
            return ((PmStartProject.Builder) Sample.builderForType(PmStartProject.class))
                    .setProjectId(ID)
                    .build();
        }

        public static PmAddTask addTask() {
            return ((PmAddTask.Builder) Sample.builderForType(PmAddTask.class))
                    .setProjectId(ID)
                    .build();
        }

        public static PmProjectStarted projectStarted() {
            return ((PmProjectStarted.Builder) Sample.builderForType(PmProjectStarted.class))
                    .setProjectId(ID)
                    .build();
        }

        public static PmProjectCreated projectCreated() {
            return ((PmProjectCreated.Builder) Sample.builderForType(PmProjectCreated.class))
                    .setProjectId(ID)
                    .build();
        }

        public static PmTaskAdded taskAdded() {
            return ((PmTaskAdded.Builder) Sample.builderForType(PmTaskAdded.class))
                    .setProjectId(ID)
                    .build();
        }
    }

    @Override
    protected RecordBasedRepository<ProjectId, TestProcessManager, Project> createRepository() {
        final TestProcessManagerRepository repo = new TestProcessManagerRepository();
        return repo;
    }

    @Override
    protected TestProcessManager createEntity() {
        final ProjectId id = ProjectId.newBuilder()
                                      .setId(newUuid())
                                      .build();
        final TestProcessManager result = Given.processManagerOfClass(TestProcessManager.class)
                                               .withId(id)
                                               .build();
        return result;
    }

    @Override
    protected List<TestProcessManager> createEntities(int count) {
        final List<TestProcessManager> procmans = Lists.newArrayList();

        for (int i = 0; i < count; i++) {
            final ProjectId id = createId(i);

            procmans.add(new TestProcessManager(id));
        }
        return procmans;
    }

    /*
     * Tests
     *************/

    @Override
    protected ProjectId createId(int value) {
        return ProjectId.newBuilder()
                        .setId(format("procman-number-%s", value))
                        .build();
    }

    @Override
    @Before
    public void setUp() {
        super.setUp();
        boundedContext = BoundedContext.newBuilder()
                                       .setMultitenant(true)
                                       .build();

        boundedContext.register(repository);
        TestProcessManager.clearMessageDeliveryHistory();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        boundedContext.close();
        super.tearDown();
    }

    ProcessManagerRepository<?, ?, ?> repository() {
        return (ProcessManagerRepository<?, ?, ?>) repository;
    }

    private void testDispatchEvent(Message eventMessage) {
        final Event event = GivenEvent.withMessage(eventMessage);
        repository().dispatch(EventEnvelope.of(event));
        assertTrue(TestProcessManager.processed(eventMessage));
    }

    @Test
    public void dispatch_event_and_load_manager() {
        testDispatchEvent(projectCreated());
    }

    @Test
    public void dispatch_several_events() {
        testDispatchEvent(projectCreated());
        testDispatchEvent(taskAdded());
        testDispatchEvent(projectStarted());
    }

    @Test
    public void dispatch_command() throws InvocationTargetException {
        testDispatchCommand(addTask());
    }

    @Test
    public void dispatch_several_commands() throws InvocationTargetException {
        testDispatchCommand(createProject());
        testDispatchCommand(addTask());
        testDispatchCommand(startProject());
    }

    private void testDispatchCommand(Message cmdMsg) throws InvocationTargetException {
        final Command cmd = requestFactory.command()
                                          .create(cmdMsg);

        repository().dispatchCommand(CommandEnvelope.of(cmd));
        assertTrue(TestProcessManager.processed(cmdMsg));
    }

    @Test
    public void dispatch_command_and_post_events() throws InvocationTargetException {
        final RememberingSubscriber subscriber = new RememberingSubscriber();
        boundedContext.getEventBus()
                      .register(subscriber);

        testDispatchCommand(addTask());

        final PmTaskAdded message = subscriber.remembered;
        assertNotNull(message);
        assertEquals(ID, message.getProjectId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_dispatch_unknown_command() {
        final Command unknownCommand =
                requestFactory.createCommand(Int32Value.getDefaultInstance());
        final CommandEnvelope request = CommandEnvelope.of(unknownCommand);
        repository().dispatchCommand(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_dispatch_unknown_event() {
        final StringValue unknownEventMessage = StringValue.getDefaultInstance();
        final Event event = GivenEvent.withMessage(unknownEventMessage);
        repository().dispatch(EventEnvelope.of(event));
    }

    @Test
    public void return_command_classes() {
        final Set<CommandClass> commandClasses = repository().getCommandClasses();

        assertContains(commandClasses,
                       PmCreateProject.class, PmCreateProject.class, PmStartProject.class);
    }

    @Test
    public void return_event_classes() {
        final Set<EventClass> eventClasses = repository().getMessageClasses();

        assertContains(eventClasses,
                       PmProjectCreated.class, PmTaskAdded.class, PmProjectStarted.class);
    }

    @Test
    public void return_rejection_classes() {
        final Set<RejectionClass> rejectionClasses = repository().getRejectionClasses();

        assertContains(rejectionClasses,
                       EntityAlreadyArchived.class, EntityAlreadyDeleted.class);
    }

    @Test
    public void dispatch_rejection() {
        final CommandEnvelope ce = CommandEnvelope.of(requestFactory.createCommand(newUuidValue()));
        final EntityAlreadyArchived rejectionMessage =
                EntityAlreadyArchived.newBuilder()
                                     .setEntityId(newUuid())
                                     .build();
        final Rejection rejection = createRejection(rejectionMessage,
                                                    ce.getCommand());
        final ProjectId id = GivenCommandMessage.ID;
        final Rejection.Builder builder =
                rejection.toBuilder()
                         .setContext(rejection.getContext()
                                              .toBuilder()
                                              .setProducerId(Identifier.pack(id)));
        final RejectionEnvelope re = RejectionEnvelope.of(builder.build());

        final Set<?> delivered = repository().dispatchRejection(re);

        assertTrue(delivered.contains(id));

        assertTrue(TestProcessManager.processed(rejectionMessage));
    }

    /**
     * Helper event subscriber which remembers an event message.
     */
    private static class RememberingSubscriber extends EventSubscriber {

        @Nullable
        private PmTaskAdded remembered;

        @Subscribe
        void on(PmTaskAdded msg) {
            remembered = msg;
        }
    }
}
