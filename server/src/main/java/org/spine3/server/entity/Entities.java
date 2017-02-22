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

package org.spine3.server.entity;

import org.spine3.server.BoundedContext;
import org.spine3.server.aggregate.AggregatePart;
import org.spine3.server.aggregate.AggregateRoot;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * The utility class for working with entities.
 *
 * @author Illia Shepilov
 */
public class Entities {

    private Entities() {
    }

    /**
     * Obtains constructor for the passed entity class.
     *
     * <p>The entity class must have a constructor with the single parameter of type defined by
     * generic type {@code <I>}.
     *
     * @param entityClass the entity class
     * @param idClass     the class of entity identifiers
     * @param <E>         the entity type
     * @param <I>         the ID type
     * @return the constructor
     * @throws IllegalStateException if the entity class does not have the required constructor
     */
    public static <E extends Entity<I, ?>, I> Constructor<E>
    getConstructor(Class<E> entityClass, Class<I> idClass) {
        checkNotNull(entityClass);
        checkNotNull(idClass);

        try {
            final Constructor<E> result = entityClass.getDeclaredConstructor(idClass);
            result.setAccessible(true);
            return result;
        } catch (NoSuchMethodException ignored) {
            throw noSuchConstructor(entityClass.getName(), idClass.getName());
        }
    }

    private static IllegalStateException noSuchConstructor(String entityClass, String idClass) {
        final String errMsg = format(
                "%s class must declare a constructor with a single %s ID parameter.",
                entityClass, idClass
        );
        return new IllegalStateException(new NoSuchMethodException(errMsg));
    }

    /**
     * Obtains the constructor for the passed aggregate part class.
     *
     * <p>The part class must have a constructor with ID and {@code AggregateRoot} parameters.
     *
     * @param entityClass the {@code AggregatePart} class
     * @param idClass     the class of entity identifiers
     * @param <E>         the entity type
     * @param <I>         the ID type
     * @return the {@code AggregatePart} constructor
     * @throws IllegalStateException if the entity class does not have the required constructor
     */
    public static <E extends AggregatePart, I, R extends AggregateRoot<I>> Constructor<E>
    getAggregatePartConstructor(Class<E> entityClass, Class<R> rootClass, Class<I> idClass) {
        checkNotNull(entityClass);
        checkNotNull(rootClass);
        checkNotNull(idClass);

        final Constructor<E> result = getAggregatePartSupertypeCtor(entityClass, idClass);
        if (result != null) {
            return result;
        }

        try {
            final Constructor<E> constructor =
                    entityClass.getDeclaredConstructor(idClass, rootClass);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            final String errMsg = format("%s class must declare a constructor " +
                                         "with ID and AggregateRoot parameters.", entityClass);
            throw new IllegalStateException(errMsg, e);
        }
    }

    /**
     * Obtains the constructor for the passed aggregate part class.
     *
     * <p>Returns the constructor if the first argument is aggregate ID
     * and the second is {@code AggregateRoot}. For example:
     *
     * <pre>{@code AggregatePartCtor(AnAggregateId id, AggregateRoot root).}</pre>
     *
     * <p>Does not return constructor if the second constructor parameter
     * is subtype of the {@code AggregateRoot}. For example:
     *
     * <pre> {@code SubAggregateRoot extends AggregateRoot{...};}
     * {@code AggregatePartCtor(AnAggregateId id, SubAggregateRoot root)};</pre>
     *
     * @param entityClass the {@code AggregatePart} class
     * @param idClass     the ID class of the {@code AggregatePart} class
     * @param <E>         the {@code Message} entity type
     * @param <I>         the {@code Message} ID type of the {@code idClass}
     * @return the obtained constructor, if constructor was not found,
     * the {@code null} will be returned
     */
    @Nullable
    @SuppressWarnings("unchecked")
    // It is OK because the constructor arguments are checked, before returning the constructor.
    private static <E extends AggregatePart, I> Constructor<E> getAggregatePartSupertypeCtor
    (Class<E> entityClass, Class<I> idClass) {
        checkNotNull(entityClass);
        checkNotNull(idClass);

        final Constructor[] constructors = entityClass.getDeclaredConstructors();
        for (Constructor constructor : constructors) {
            final Class[] parameters = constructor.getParameterTypes();
            final int length = parameters.length;
            if (length != 2) {
                continue;
            }
            final boolean isSuperType = parameters[0].equals(idClass) &&
                                        parameters[1].equals(AggregateRoot.class);
            if (isSuperType) {
                constructor.setAccessible(true);
                return constructor;
            }
        }
        return null;
    }

    /**
     * Creates a new {@code AggregateRoot} entity and sets it to the default state.
     *
     * @param id             the ID of the {@code AggregatePart} is managed by {@code AggregateRoot}
     * @param boundedContext the {@code BoundedContext} to use
     * @param rootClass      the class of the {@code AggregateRoot}
     * @param <I>            the type of entity IDs
     * @return a {@code AggregateRoot} instance
     */
    public static <I, R extends AggregateRoot<I>> R
    createAggregateRootEntity(I id, BoundedContext boundedContext, Class<R> rootClass) {
        checkNotNull(id);
        checkNotNull(boundedContext);
        checkNotNull(rootClass);

        try {
            final Constructor<R> rootConstructor =
                    rootClass.getDeclaredConstructor(boundedContext.getClass(), id.getClass());
            rootConstructor.setAccessible(true);
            R root = rootConstructor.newInstance(boundedContext, id);
            return root;
        } catch (NoSuchMethodException | InvocationTargetException |
                InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Creates a new {@code AggregatePart} entity and sets it to the default state.
     *
     * @param ctor the constructor to use
     * @param id   the ID of the entity
     * @param <I>  the type of entity IDs
     * @param <E>  the type of the entity
     * @return an {@code AggregatePart} instance
     */
    public static <I, E extends AbstractEntity<I, ?>> E
    createAggregatePartEntity(Constructor<E> ctor, I id, AggregateRoot<I> root) {
        checkNotNull(ctor);
        checkNotNull(id);
        checkNotNull(root);

        try {
            final E result = ctor.newInstance(id, root);
            result.init();
            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Creates a new entity and sets it to the default state.
     *
     * @param ctor the constructor to use
     * @param id   the ID of the entity
     * @param <I>  the type of entity IDs
     * @param <E>  the type of the entity
     * @return new entity
     */
    public static <I, E extends AbstractEntity<I, ?>> E createEntity(Constructor<E> ctor, I id) {
        checkNotNull(ctor);
        checkNotNull(id);

        try {
            final E result = ctor.newInstance(id);
            result.init();
            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
