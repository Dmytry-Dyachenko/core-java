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
import org.spine3.time.LocalTime;
import org.spine3.time.LocalTimes;

import java.io.Serializable;
import java.text.ParseException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.util.Exceptions.illegalArgumentWithCauseOf;

/**
 * The default stringifier for {@link LocalTime} values.
 *
 * @author Alexander Yevsyukov
 */
final class LocalTimeStringifier extends Stringifier<LocalTime> implements Serializable {

    private static final long serialVersionUID = 1;
    private static final LocalTimeStringifier INSTANCE = new LocalTimeStringifier();

    static LocalTimeStringifier getInstance() {
        return INSTANCE;
    }

    @Override
    protected String toString(LocalTime time) {
        checkNotNull(time);
        final String result = LocalTimes.toString(time);
        return result;
    }

    @Override
    protected LocalTime fromString(String str) {
        checkNotNull(str);
        final LocalTime time;
        try {
          time = LocalTimes.parse(str);
        } catch (ParseException e) {
            throw illegalArgumentWithCauseOf(e);
        }
        return time;
    }

    @Override
    public String toString() {
        return "TimeStringifiers.forLocalTime()";
    }

    private Object readResolve() {
        return INSTANCE;
    }
}