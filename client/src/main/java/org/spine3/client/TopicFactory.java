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
package org.spine3.client;

import com.google.protobuf.Message;
import org.spine3.time.ZoneOffset;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.client.Targets.composeTarget;

/**
 * @author Alex Tymchenko
 */
public class TopicFactory extends ActorRequestFactory<TopicFactory> {

    protected TopicFactory(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Creates a {@link Topic} for a subset of the entity states by specifying their IDs.
     *
     * @param entityClass the class of a target entity
     * @param ids         the IDs of interest
     * @return the instance of {@code Topic} assembled according to the parameters.
     */
    public Topic someOf(Class<? extends Message> entityClass, Set<? extends Message> ids) {
        checkNotNull(entityClass);
        checkNotNull(ids);

        final Target target = composeTarget(entityClass, ids);
        final Topic result = forTarget(target);
        return result;
    }

    /**
     * Creates a {@link Topic} for all of the specified entity states.
     *
     * @param entityClass the class of a target entity
     * @return the instance of {@code Topic} assembled according to the parameters.
     */
    public Topic allOf(Class<? extends Message> entityClass) {
        checkNotNull(entityClass);
        final Target target = composeTarget(entityClass, null);
        final Topic result = forTarget(target);
        return result;
    }

    /**
     * Creates a {@link Topic} for the specified {@linkplain Target}.
     *
     * @param target the {@code} Target to create a topic for.
     * @return the instance of {@code Topic}.
     */
    public Topic forTarget(Target target) {
        //TODO:4/7/17:alex.tymchenko: validate Target.
        checkNotNull(target);

        return Topic.newBuilder()
                    .setContext(actorContext())
                    .setTarget(target)
                    .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TopicFactory switchTimezone(ZoneOffset zoneOffset) {
        return switchTimezone(zoneOffset, newBuilder());
    }

    public static class Builder
            extends ActorRequestFactory.AbstractBuilder<TopicFactory, TopicFactory.Builder> {

        @Override
        protected TopicFactory.Builder thisInstance() {
            return this;
        }

        @Override
        public TopicFactory build() {
            super.build();
            final TopicFactory result = new TopicFactory(this);
            return result;
        }
    }
}
