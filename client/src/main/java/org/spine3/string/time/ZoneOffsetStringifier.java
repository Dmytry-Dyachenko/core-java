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

package org.spine3.string.time;

import org.spine3.string.Stringifier;
import org.spine3.time.ZoneOffset;
import org.spine3.time.ZoneOffsets;

import java.io.Serializable;
import java.text.ParseException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.util.Exceptions.wrappedCause;

/**
 * The default stringifier for {@code ZoneOffset} values.
 *
 * @author Alexander Yevsyukov
 */
final class ZoneOffsetStringifier extends Stringifier<ZoneOffset> implements Serializable {

    private static final long serialVersionUID = 1;
    private static final ZoneOffsetStringifier INSTANCE = new ZoneOffsetStringifier();

    static ZoneOffsetStringifier instance() {
        return INSTANCE;
    }

    @Override
    protected String toString(ZoneOffset offset) {
        checkNotNull(offset);
        final String result = ZoneOffsets.toString(offset);
        return result;
    }

    @Override
    protected ZoneOffset fromString(String str) {
        checkNotNull(str);
        final ZoneOffset result;
        try {
            result = ZoneOffsets.parse(str);
        } catch (ParseException e) {
            throw wrappedCause(e);
        }
        return result;
    }

    @Override
    public String toString() {
        return "TimeStringifiers.forZoneOffset()";
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
