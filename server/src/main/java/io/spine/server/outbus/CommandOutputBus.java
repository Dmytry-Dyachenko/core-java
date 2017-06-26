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
package io.spine.server.outbus;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.Error;
import io.spine.core.Event;
import io.spine.core.Failure;
import io.spine.core.IsSent;
import io.spine.core.MessageEnvelope;
import io.spine.server.bus.Bus;
import io.spine.server.bus.MessageDispatcher;
import io.spine.server.delivery.Delivery;
import io.spine.type.MessageClass;
import io.spine.util.Exceptions;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.bus.Buses.acknowledge;
import static io.spine.server.bus.Buses.reject;

/**
 * A base bus responsible for delivering the {@link io.spine.core.Command command} output.
 *
 * <p>The typical output artifacts of the command processing are:
 *
 * <ul>
 *     <li>{@linkplain Event events} — in case the command is handled successfully;
 *     <li>{@linkplain Failure business failures} — if the command contradicts
 *          the business rules.
 * </ul>
 *
 * <p>The instances of {@code CommandOutputBus} are responsible for a delivery of such output
 * artifacts to the corresponding destinations.
 *
 * @author Alex Tymchenko
 */
@Internal
public abstract class CommandOutputBus<M extends Message,
                                       E extends MessageEnvelope<M>,
                                       C extends MessageClass,
                                       D extends MessageDispatcher<C,E>>
                extends Bus<M, E, C, D> {

    /**
     * The strategy to deliver the messages to the dispatchers.
     */
    private final CommandOutputDelivery<E, C, D> delivery;

    protected CommandOutputBus(CommandOutputDelivery<E, C, D> delivery) {
        super();
        this.delivery = delivery;
        injectDispatcherProvider();
    }

    /**
     * Sets up the {@code CommandOutputDelivery} with an ability to obtain
     * {@linkplain MessageDispatcher message dispatchers} by a given
     * {@linkplain MessageClass message class} instance at runtime.
     */
    private void injectDispatcherProvider() {
        delivery().setConsumerProvider(
                new Function<C, Set<D>>() {
                    @Nullable
                    @Override
                    public Set<D> apply(@Nullable C messageClass) {
                        checkNotNull(messageClass);
                        final Set<D> dispatchers =
                                registry().getDispatchers(messageClass);
                        return dispatchers;
                    }
                });
    }

    /**
     * Verifies that a message can be posted to this {@code CommandOutputBus}.
     *
     * <p>A command output can be posted if its message has either dispatcher or handler
     * registered with this {@code CommandOutputBus}.
     *
     * <p>The message also must satisfy validation constraints defined in its Protobuf type.
     *
     * @param message          the command output message to check
     * @return a {@link Throwable} representing the found violation or
     *         {@link Optional#absent() Optional.absent()} if the given {@link Message} can be
     *         posted to this bus
     */
    public abstract Optional<Throwable> validate(Message message);

    /**
     * Enriches the message posted to this instance of {@code CommandOutputBus}.
     *
     * @param originalMessage the original message posted to the bus
     * @return the enriched message
     */
    protected abstract M enrich(M originalMessage);

    /**
     * Obtains the {@linkplain Delivery delivery strategy} configured for this bus.
     *
     * @return the delivery strategy
     */
    protected CommandOutputDelivery<E, C, D> delivery() {
        return this.delivery;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides for return type covariance.
     */
    @Override
    protected abstract OutputDispatcherRegistry<C, D> createRegistry();

    @Override
    protected Optional<IsSent> preProcess(final E message) {
        final Optional<Throwable> violation = this.validate(message.getMessage());
        final Optional<IsSent> result = violation.transform(
                new Function<Throwable, IsSent>() {
                    @Override
                    public IsSent apply(@Nullable Throwable input) {
                        checkNotNull(input);
                        final Error error = Exceptions.toError(input);
                        return reject(getId(message), error);
                    }
                }
        );
        return result;
    }

    @Override
    protected IsSent doPost(E envelope) {
        final M enriched = enrich(envelope.getOuterObject());
        final E enrichedEnvelope = toEnvelope(enriched);
        final int dispatchersCalled = callDispatchers(enrichedEnvelope);

        if (dispatchersCalled == 0) {
            handleDeadMessage(enrichedEnvelope);
        }
        final IsSent acknowledgement = acknowledge(getId(envelope));
        return acknowledgement;
    }

    /**
     * Call the dispatchers for the {@code eventEnvelope}.
     *
     * @param messageEnvelope the event envelope to pass to the dispatchers.
     * @return the number of the dispatchers called, or {@code 0} if there weren't any.
     */
    private int callDispatchers(E messageEnvelope) {
        @SuppressWarnings("unchecked")  // it's fine, since the message is validated previously.
        final C messageClass = (C) messageEnvelope.getMessageClass();
        final Collection<D> dispatchers = registry().getDispatchers(messageClass);
        delivery().deliver(messageEnvelope);
        return dispatchers.size();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides for return type covariance.
     */
    @Override
    protected OutputDispatcherRegistry<C, D> registry() {
        return (OutputDispatcherRegistry<C, D>) super.registry();
    }

    @Override
    public void close() throws Exception {
        registry().unregisterAll();
    }
}