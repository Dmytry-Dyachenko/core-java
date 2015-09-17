/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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
package org.spine3.server;

import org.spine3.util.Classes;

import javax.annotation.Nullable;

/**
 * Base interface for repositories.
 *
 * @param <I> the type of the IDs of entity objects
 * @param <E> the type of the stored object
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public interface Repository<I, E extends Entity<I, ?>> extends Storage<I, E> {

    /**
     * Create a new entity instance with its default state.
     *
     * @param id the id of the entity
     * @return new entity instance
     */
    E create(I id);

    /**
     * Stores the passed object.
     *
     * @param obj an instance to store
     */
    void store(E obj);

    /**
     * Loads the entity with the passed ID.
     *
     * @param id the id of the entity to load
     * @return the entity or {@code null} if there's no entity with such id
     */
    @Nullable
    E load(I id);

    @SuppressWarnings("UtilityClass")
    class TypeInfo {
        /**
         * The index of the declaration of the generic type {@code I} in the {@link Repository} interface.
         */
        private static final int ID_CLASS_GENERIC_INDEX = 0;
        /**
         * The index of the declaration of the generic type {@code E} in the {@link Repository} interface.
         */
        private static final int ENTITY_CLASS_GENERIC_INDEX = 1;

        private TypeInfo() {
        }

        /**
         * Returns {@link Class} of entity IDs of the passed repository.
         *
         * @return the aggregate id {@link Class}
         */
        public static <I> Class<I> getIdClass(Class<? extends Repository> clazz) {
            return Classes.getGenericParameterType(clazz, ID_CLASS_GENERIC_INDEX);
        }

        /**
         * Returns {@link Class} object representing the aggregate root type of the given repository.
         *
         * @return the aggregate root {@link Class}
         */
        public static <E extends Entity> Class<E> getEntityClass(Class<? extends Repository> clazz) {
            return Classes.getGenericParameterType(clazz, ENTITY_CLASS_GENERIC_INDEX);
        }
    }
}
