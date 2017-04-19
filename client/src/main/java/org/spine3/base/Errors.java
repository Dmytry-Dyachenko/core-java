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

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import io.grpc.Metadata;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for working with {@link Error}s.
 *
 * @author Alexander Yevsyukov
 */
public class Errors {

    private Errors() {
        // Prevent instantiation of this utility class.
    }

    /** Creates new instance of {@link Error} by the passed exception. */
    public static Error fromException(Exception exception) {
        checkNotNull(exception);
        final String message = exception.getMessage();
        final Error result = Error.newBuilder()
                                  .setType(exception.getClass()
                                                    .getName())
                                  .setMessage(message)
                                  .setStacktrace(Throwables.getStackTraceAsString(exception))
                                  .build();
        return result;
    }

    /** Creates new instance of {@link Error} by the passed {@code Throwable}. */
    public static Error fromThrowable(Throwable throwable) {
        checkNotNull(throwable);
        final String message = Strings.nullToEmpty(throwable.getMessage());
        final Error result = Error.newBuilder()
                                  .setType(throwable.getClass()
                                                    .getName())
                                  .setMessage(message)
                                  .setStacktrace(Throwables.getStackTraceAsString(throwable))
                                  .build();
        return result;
    }

    /**
     * Extracts a {@linkplain Error system error} from the
     * {@linkplain io.grpc.stub.StreamObserver#onError(Throwable) Throwable},
     * received on a client-side as a result of a failed gRPC call to server-side routines.
     *
     * <p>The {@code Error} is extracted from the trailer metadata of
     * either {@link StatusRuntimeException} or {@link StatusException} only.
     *
     * <p>If any other exception is passed, {@code Optional.absent()} is returned.
     *
     * @param throwable the {@code Throwable} to extract an {@link Error}
     * @return the extracted error or {@code Optional.absent()} if the extraction failed
     */
    @SuppressWarnings("ChainOfInstanceofChecks") // Only way to check an exact throwable type.
    public static Optional<Error> fromStreamError(Throwable throwable) {
        checkNotNull(throwable);
        if (throwable instanceof StatusRuntimeException) {
            final Metadata metadata = ((StatusRuntimeException) throwable).getTrailers();
            return MetadataConverter.toError(metadata);
        }
        if (throwable instanceof StatusException) {
            final Metadata metadata = ((StatusException) throwable).getTrailers();
            return MetadataConverter.toError(metadata);
        }

        return Optional.absent();
    }
}
