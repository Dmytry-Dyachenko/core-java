/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.reflect;

import com.google.common.base.Predicate;
import com.google.protobuf.Message;
import org.spine3.base.CommandContext;
import org.spine3.server.command.Assign;
import org.spine3.server.command.CommandHandler;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * The wrapper for a command handler method.
 *
 * @author Alexander Yevsyukov
 */
public class CommandHandlerMethod extends HandlerMethod<CommandContext> {

    /**
     * The instance of the predicate to filter command handler methods of a class.
     */
    public static final Predicate<Method> PREDICATE = new FilterPredicate();

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    public CommandHandlerMethod(Method method) {
        super(method);
    }

    /**
     * {@inheritDoc}
     *
     * @return the list of event messages (or an empty list if the handler returns nothing)
     */
    @Override
    public <R> R invoke(Object target, Message message, CommandContext context) throws InvocationTargetException {
        final R handlingResult = super.invoke(target, message, context);

        final List<? extends Message> events = toList(handlingResult);
        // The list of event messages/records is the return type expected.
        @SuppressWarnings("unchecked")
        final R result = (R) events;
        return result;
    }

    /**
     * Casts a command handling result to a list of event messages.
     *
     * @param handlingResult the command handler method return value. Could be a {@link Message}, a list of messages,
     *                       or {@code null}.
     * @return the list of event messages or an empty list if {@code null} is passed
     */
    private static <R> List<? extends Message> toList(@Nullable R handlingResult) {
        if (handlingResult == null) {
            return emptyList();
        }

        final Class<?> resultClass = handlingResult.getClass();
        if (List.class.isAssignableFrom(resultClass)) {
            // Cast to the list of messages as it is the one of the return types we expect by methods we call.
            @SuppressWarnings("unchecked")
            final List<? extends Message> result = (List<? extends Message>) handlingResult;
            return result;
        } else {
            // Another type of result is single event message (as Message).
            final List<Message> result = singletonList((Message) handlingResult);
            return result;
        }
    }

    /**
     * Returns a map of the command handler methods from the passed instance.
     *
     * @param object the object that keeps command handler methods
     * @return immutable map
     */
    @CheckReturnValue
    public static MethodMap<CommandHandlerMethod> scan(CommandHandler object) {
        final MethodMap<CommandHandlerMethod> handlers = MethodMap.create(object.getClass(), factory());
        return handlers;
    }

    /**
     * Verifies that passed methods are declared {@code public}.
     *
     * <p>Logs warning for the methods with a non-public modifier.
     *
     * @param methods the methods to check
     * @see HandlerMethod#log()
     */
    public static void checkModifiers(Iterable<Method> methods) {
        for (Method method : methods) {
            final boolean isPublic = Modifier.isPublic(method.getModifiers());
            if (!isPublic) {
                warnOnWrongModifier("Command handler method {} should be declared 'public'.", method);
            }
        }
    }

    public static HandlerMethod.Factory<CommandHandlerMethod> factory() {
        return Factory.instance();
    }

    /**
     * The factory for filtering methods that match {@code CommandHandlerMethod} specification.
     */
    private static class Factory implements HandlerMethod.Factory<CommandHandlerMethod> {

        @Override
        public Class<CommandHandlerMethod> getMethodClass() {
            return CommandHandlerMethod.class;
        }

        @Override
        public CommandHandlerMethod create(Method method) {
            return new CommandHandlerMethod(method);
        }

        @Override
        public Predicate<Method> getPredicate() {
            return PREDICATE;
        }

        private enum Singleton {
            INSTANCE;
            @SuppressWarnings("NonSerializableFieldInSerializableClass")
            private final Factory value = new Factory();
        }

        private static Factory instance() {
            return Singleton.INSTANCE.value;
        }
    }

    /**
     * The predicate class that allows to filter command handling methods.
     */
    private static class FilterPredicate implements Predicate<Method> {

        /**
         * A command must be the first parameter of a handling method.
         */
        private static final int MESSAGE_PARAM_INDEX = 0;

        /**
         * A {@code CommandContext} must be the second parameter of the handling method.
         */
        private static final int COMMAND_CONTEXT_PARAM_INDEX = 1;

        /**
         * A command handling method accepts two parameters.
         */
        private static final int COMMAND_HANDLER_PARAM_COUNT = 2;

        private static boolean isAnnotatedCorrectly(Method method) {
            final boolean isAnnotated = method.isAnnotationPresent(Assign.class);
            return isAnnotated;
        }

        private static boolean acceptsCorrectParams(Method method) {
            final Class<?>[] paramTypes = method.getParameterTypes();
            final boolean paramCountIsCorrect = paramTypes.length == COMMAND_HANDLER_PARAM_COUNT;
            if (!paramCountIsCorrect) {
                return false;
            }
            final boolean acceptsCorrectParams =
                    Message.class.isAssignableFrom(paramTypes[MESSAGE_PARAM_INDEX]) &&
                            CommandContext.class.equals(paramTypes[COMMAND_CONTEXT_PARAM_INDEX]);
            return acceptsCorrectParams;
        }

        private static boolean returnsMessageOrList(Method method) {
            final Class<?> returnType = method.getReturnType();

            if (Message.class.isAssignableFrom(returnType)) {
                return true;
            }
            //noinspection RedundantIfStatement
            if (List.class.isAssignableFrom(returnType)) {
                return true;
            }

            return false;
        }

        @Override
        public boolean apply(@Nullable Method method) {
            //noinspection SimplifiableIfStatement
            if (method == null) {
                return false;
            }
            return isAnnotatedCorrectly(method)
                    && acceptsCorrectParams(method)
                    && returnsMessageOrList(method);
        }
    }
}