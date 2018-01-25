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
package io.spine.server.stand;

import io.spine.base.Error;
import io.spine.client.Subscription;

/**
 * An exception thrown in case an invalid or unsupported {@link Subscription}
 * has been submitted to {@linkplain Stand}.
 *
 * @author Alex Tymchenko
 */
public class InvalidSubscriptionException extends InvalidRequestException {

    private static final long serialVersionUID = 0L;

    /**
     * Creates a new instance.
     *
     * @param messageText  an error message text
     * @param subscription a related subscription
     * @param error        an error occurred
     */
    protected InvalidSubscriptionException(String messageText,
                                           Subscription subscription,
                                           Error error) {
        super(messageText, subscription, error);
    }

    @Override
    public Subscription getRequest() {
        return (Subscription) super.getRequest();
    }
}