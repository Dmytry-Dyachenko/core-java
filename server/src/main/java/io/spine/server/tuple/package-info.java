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

/**
 * This package provides tuples for return values of command handling methods.
 *
 * <p>Although tuples are <a href="https://github.com/google/guava/wiki/IdeaGraveyard#tuples-for-n--2">
 * considered harmful</a> in general, there is a valid case of their usage when there is a need for
 * returning more than one event message from a command handling method.
 *
 * <p>For example, the return value of the below method does not say much about the number and types
 * of returned event messages.
 * <pre>{@code
 *     {@literal @}Assign
 *     List<Message> on(CreateTask cmd) { ... }
 * }</pre>
 *
 * The below declaration gives both number and types of events:
 * <pre>{@code
 *     {@literal @}Assign
 *     Pair<TaskCreated, TaskAssigned> on(CreateTask cmd) { ... }
 * }</pre>
 *
 * <h2>Generic Types</h2>
 *
 * <p>Classes provided by this package can support up to 5 generic parameters. They are named from
 * {@code <A>} through {@code <E>}. Methods obtaining these values are named after the types:
 * {@code getA()}, {@code getB()} and so on.
 *
 * <p>The first generic parameter {@code <A>} must always be a specific
 * {@link com.google.protobuf.Message Message} class.
 *
 * <p>Types from {@code <B>} through {@code <E>} can be either {@code Message} or
 * {@link com.google.common.base.Optional Optional}. See sections below for details.
 *
 * <h2>Basic Tuples</h2>
 *
 * <p>The following tuple classes are provided:
 * <ul>
 *    <li>{@link io.spine.server.tuple.Pair Pair&lt;A, B&gt;}
 *    <li>{@link io.spine.server.tuple.Triplet Triplet&lt;A, B, C&gt;}
 *    <li>{@link io.spine.server.tuple.Quartet Quartet&lt;A, B, C, D&gt;}
 *    <li>{@link io.spine.server.tuple.Quintet Quintet&lt;A, B, C, D, E&gt;}
 * </ul>
 *
 * <p>Basic tuple classes allow {@link com.google.common.base.Optional Optional} starting from
 * the second generic argument.
 *
 * <h2>Alternatives</h2>
 *
 * <p>In order to define alternatively returned values, please use the following classes:
 * <ul>
 *     <li>{@link io.spine.server.tuple.EitherOfTwo EitherOfTwo&lt;A, B&gt;}
 *     <li>{@link io.spine.server.tuple.EitherOfThree EitherOfThree&lt;A, B, C&gt;}
 *     <li>{@link io.spine.server.tuple.EitherOfFour EitherOfFour&lt;A, B, C, D&gt;}
 *     <li>{@link io.spine.server.tuple.EitherOfFive EitherOfFive&lt;A, B, C, D, E&gt;}
 * </ul>
 *
 * <p>Generic parameters for alternatives can be only {@link com.google.protobuf.Message Message}.
 *
 * <p>We believe that a list of alternatives longer than five is hard to understand.
 * If you face a such a need, consider splitting a command into two or more independent commands
 * so that their outcome is more obvious.
 *
 * <h2>Using Tuples with Alternatives</h2>
 *
 * <p>A {@link io.spine.server.tuple.Pair Pair} can be defined with the second parameter being on
 * of the {@link io.spine.server.tuple.Either Either} subclasses, and created using
 * {@link io.spine.server.tuple.Pair#withEither(com.google.protobuf.Message,
 * io.spine.server.tuple.Either) Pair.withEither()}.
 */

@ParametersAreNonnullByDefault
package io.spine.server.tuple;

import javax.annotation.ParametersAreNonnullByDefault;
