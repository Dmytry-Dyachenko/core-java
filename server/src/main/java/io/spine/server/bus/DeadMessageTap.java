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

package io.spine.server.bus;

import io.spine.annotation.Internal;
import io.spine.core.MessageEnvelope;

/**
 * A definition of a handler for a dead message.
 *
 * <p>If a message with no target dispatchers found is passed to the bus, it will result in a call
 * to {@link DeadMessageTap#capture
 * DeadMessageTap.capture(MessageEnvelope)}. The method produces
 * {@link MessageUnhandled} instance describing the dead message. It may also process the given
 * message (e.g. store it into the bus store).
 *
 * @author Dmytro Dashenkov
 */
@Internal
public interface DeadMessageTap<E extends MessageEnvelope<?, ?, ?>> {

    /**
     * Handles the dead message in a bus-specific way and produces an {@link MessageUnhandled} which
     * may be converted to a {@link Error} for notifying the poster about the absence of
     * dispatchers.
     *
     * @param message the dead message
     * @return the {@link MessageUnhandled} instance describing the dead message
     */
    MessageUnhandled capture(E message);
}
