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

package org.spine3.type;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Descriptors;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt64Value;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.Commands;
import org.spine3.base.Event;
import org.spine3.base.Version;
import org.spine3.client.ActorRequestFactory;
import org.spine3.options.IfMissingOption;
import org.spine3.protobuf.Wrapper;
import org.spine3.server.command.EventFactory;
import org.spine3.test.TestActorRequestFactory;
import org.spine3.test.Tests;
import org.spine3.time.Time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.spine3.test.Tests.newUuidValue;

/**
 * Provides only class-level tests.
 *
 * <p>Other methods of {@link TypeName} are just over {@link TypeUrl} which are tested by
 * its own set of tests.
 */
public class TypeNameShould {

    private static final ActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(TypeNameShould.class);

    @Test
    public void pass_the_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(Command.class, Command.getDefaultInstance())
                .setDefault(Descriptors.Descriptor.class, Command.getDefaultInstance()
                                                                 .getDescriptorForType())
                .testAllPublicStaticMethods(TypeName.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void reject_empty_name() {
        TypeName.of("");
    }

    @Test
    public void return_simple_type_name() {
        assertEquals(StringValue.class.getSimpleName(), TypeName.of(StringValue.class)
                                                                .getSimpleName());
    }

    @Test
    public void return_simple_name_if_no_package() {
        // A msg type without Protobuf package
        final String name = IfMissingOption.class.getSimpleName();
        final TypeUrl typeUrl = TypeName.of(name)
                                        .toUrl();

        final String actual = TypeName.from(typeUrl)
                                      .getSimpleName();

        assertEquals(name, actual);
    }

    @Test
    public void obtain_instance_for_message() {
        final TypeName typeName = TypeName.of(StringValue.getDefaultInstance());
        assertNotNull(typeName);
        assertEquals(StringValue.class.getSimpleName(), typeName.getSimpleName());
    }

    @Test
    public void obtain_instance_for_Java_class() {
        final TypeName typeName = TypeName.of(StringValue.class);
        assertNotNull(typeName);
        assertEquals(StringValue.class.getSimpleName(), typeName.getSimpleName());
    }

    @Test
    public void obtain_type_of_command() {
        final Command command = requestFactory.command().create(newUuidValue());

        final TypeName typeName = TypeName.ofCommand(command);
        assertNotNull(typeName);
        assertEquals(StringValue.class.getSimpleName(), typeName.getSimpleName());
    }

    @Test
    public void obtain_type_name_of_event() {
        final Command command = requestFactory.command().create(newUuidValue());
        final StringValue producerId = Wrapper.forString(getClass().getSimpleName());
        final EventFactory ef = EventFactory.newBuilder()
                                            .setCommandId(Commands.generateId())
                                            .setProducerId(producerId)
                                            .setCommandContext(command.getContext())
                                            .build();
        final Event event = ef.createEvent(Time.getCurrentTime(), Tests.<Version>nullRef());

        final TypeName typeName = TypeName.ofEvent(event);
        assertNotNull(typeName);
        assertEquals(Timestamp.class.getSimpleName(), typeName.getSimpleName());
    }

    @Test
    public void obtain_instance_by_descriptor() {
        final TypeName typeName = TypeName.from(UInt64Value.getDescriptor());
        assertNotNull(typeName);
        assertEquals(UInt64Value.class.getSimpleName(), typeName.getSimpleName());
    }
}
