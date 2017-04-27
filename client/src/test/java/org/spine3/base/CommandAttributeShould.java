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

package org.spine3.base;

import com.google.protobuf.Empty;
import org.junit.Before;
import org.junit.Test;
import org.spine3.test.TestActorRequestFactory;
import org.spine3.time.Time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
public class CommandAttributeShould {

    private final TestActorRequestFactory factory =
            TestActorRequestFactory.newInstance(CommandAttributeShould.class);

    private CommandContext.Builder contextBuilder;

    @Before
    public void setUp() {
        Command command = factory.createCommand(Empty.getDefaultInstance(),
                                                Time.getCurrentTime());
        contextBuilder = command.getContext().toBuilder();
    }

    @Test
    public void set_and_get_bool_attribute() {
        final CommandAttribute<Boolean> boolAttr = new CommandAttribute<Boolean>("flag") {};
        boolAttr.set(contextBuilder, true);

        assertTrue(boolAttr.get(contextBuilder.build())
                           .get());
    }

    @Test
    public void set_and_get_string_attribute() {
        final CommandAttribute<String> strAttr = new CommandAttribute<String>("str") {};

        final String expected = getClass().getName();
        strAttr.set(contextBuilder, expected);

        assertEquals(expected, strAttr.get(contextBuilder.build())
                                      .get());
    }
}
