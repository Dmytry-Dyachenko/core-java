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

package io.spine.server.event;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import org.junit.Test;

import java.util.Set;

/**
 * @author Alexander Yevsyukov
 */
public class DelegatingEventDispatcherShould {

    @Test
    public void pass_null_tolerance_test() {
        new NullPointerTester()
                .setDefault(EventDispatcherDelegate.class, new EmptyEventDispatcherDelegate())
                .testAllPublicStaticMethods(DelegatingEventDispatcher.class);
    }

    private static final class EmptyEventDispatcherDelegate implements EventDispatcherDelegate {

        @Override
        public Set<EventClass> getEventClasses() {
            return ImmutableSet.of();
        }

        @Override
        public void dispatchEvent(EventEnvelope envelope) {
            // Do nothing.
        }
    }
}
