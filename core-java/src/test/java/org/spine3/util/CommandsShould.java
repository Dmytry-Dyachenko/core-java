/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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
package org.spine3.util;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.junit.Test;
import org.spine3.base.CommandId;
import org.spine3.base.UserId;
import org.spine3.client.CommandRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.spine3.testdata.TestCommandFactory.createProject;
import static org.spine3.util.Commands.generateId;
import static org.spine3.util.Identifiers.USER_ID_AND_TIME_DELIMITER;
import static org.spine3.util.Identifiers.timestampToString;
import static org.spine3.util.Users.newUserId;

/**
 * @author Mikhail Melnik
 */
@SuppressWarnings({"InstanceMethodNamingConvention"/*we have another convention in tests*/,
"DuplicateStringLiteralInspection"/*ok in this case*/})
public class CommandsShould {

    @Test
    public void generate() {

        final UserId userId = newUserId("commands-should-test");

        final CommandId result = generateId(userId);

        assertThat(result, allOf(
                hasProperty("actor", equalTo(userId)),
                hasProperty("timestamp")));
    }

    @SuppressWarnings("ConstantConditions"/*ok in this case*/)
    @Test(expected = NullPointerException.class)
    public void fail_on_null_parameter() {
        generateId(null);
    }

    @Test
    public void sort() {

        final Timestamp when = TimeUtil.createTimestampFromMillis(System.currentTimeMillis() - 1000);

        final CommandRequest commandRequest1 = createProject(when);
        final CommandRequest commandRequest2 = createProject();
        final CommandRequest commandRequest3 = createProject();

        final Collection<CommandRequest> sortedList = new ArrayList<>();
        sortedList.add(commandRequest1);
        sortedList.add(commandRequest2);
        sortedList.add(commandRequest3);

        final List<CommandRequest> unSortedList = new ArrayList<>();
        unSortedList.add(commandRequest3);
        unSortedList.add(commandRequest1);
        unSortedList.add(commandRequest2);

        assertFalse(sortedList.equals(unSortedList));

        Commands.sort(unSortedList);

        assertEquals(sortedList, unSortedList);
    }

    @Test
    public void convert_to_string_command_id_message() {

        final String userIdString = "user123";
        final Timestamp currentTime = getCurrentTime();
        final CommandId id = generateId(userIdString, currentTime);

        /* TODO:2015-09-21:alexander.litus: create parse() method that would restore an object from its String representation.
           Use the restored object for equality check with the original object.
         */
        final String expected = userIdString + USER_ID_AND_TIME_DELIMITER + timestampToString(currentTime);

        final String actual = Commands.idToString(id);

        assertEquals(expected, actual);
    }
}