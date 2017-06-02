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

package org.spine3.server.entity.storage;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.spine3.client.ColumnFilter;
import org.spine3.client.CompositeColumnFilter.CompositeOperator;

import javax.annotation.Nullable;
import java.io.Serializable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMultimap.copyOf;
import static org.spine3.client.CompositeColumnFilter.CompositeOperator.ALL;
import static org.spine3.server.storage.LifecycleFlagField.archived;
import static org.spine3.server.storage.LifecycleFlagField.deleted;

/**
 * A set of {@link ColumnFilter} instances joined by a logical
 * {@link CompositeOperator composite operator}.
 *
 * @author Dmytro Dashenkov
 */
public final class CompositeQueryParameter implements Serializable {

    private static final Predicate<Column> isLifecycleColumn = new Predicate<Column>() {
        @Override
        public boolean apply(@Nullable Column column) {
            checkNotNull(column);
            final boolean result = archived.name().equals(column.getName())
                    || deleted.name().equals(column.getName());
            return result;
        }
    };

    private static final long serialVersionUID = 1L;

    private final CompositeOperator operator;

    private final ImmutableMultimap<Column, ColumnFilter> filters;

    private final boolean hasLifecycle;

    /**
     * Creates a new instance of {@code CompositeQueryParameter} from the given filters joined
     * by the given operator.
     *
     * @param filters  the filters to aggregate
     * @param operator the operator to apply to the given filters
     * @return new instance of {@code CompositeQueryParameter}
     */
    static CompositeQueryParameter from(Multimap<Column, ColumnFilter> filters,
                                        CompositeOperator operator) {
        checkNotNull(filters);
        checkNotNull(operator);
        checkArgument(operator.getNumber() > 0, "Invalid aggregating operator %s.", operator);

        return new CompositeQueryParameter(operator, filters);
    }

    private CompositeQueryParameter(CompositeOperator operator,
                                    Multimap<Column, ColumnFilter> filters) {
        this.operator = operator;
        this.filters = copyOf(filters);
        this.hasLifecycle = containsLifecycle(filters.keySet());
    }

    private static boolean containsLifecycle(Iterable<Column> columns) {
        final boolean result = Iterables.any(columns, isLifecycleColumn);
        return result;
    }

    /**
     * @return the composite operator
     */
    public CompositeOperator getOperator() {
        return operator;
    }

    /**
     * @return the joined {@link ColumnFilter Column filters}
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // Immutable structure
    public ImmutableMultimap<Column, ColumnFilter> getFilters() {
        return filters;
    }

    /**
     * Merges current instance with the given instances by the rules of conjunction.
     *
     * <p>The resulting {@code CompositeQueryParameter} contains all the filters of the current and
     * the given instances joined by the {@linkplain CompositeOperator#ALL conjunction operator}.
     *
     * @param other the instances of the {@code CompositeQueryParameter} to merge with
     * @return new instance of {@code CompositeQueryParameter} joining all the parameters
     */
    public CompositeQueryParameter conjunct(Iterable<CompositeQueryParameter> other) {
        checkNotNull(other);

        final Multimap<Column, ColumnFilter> mergedFilters = LinkedListMultimap.create();
        mergedFilters.putAll(filters);
        for (CompositeQueryParameter parameter : other) {
            mergedFilters.putAll(parameter.getFilters());
        }
        final CompositeQueryParameter result = from(mergedFilters, ALL);
        return result;
    }

    /**
     * Merges current instance with the given filter.
     *
     * <p>The resulting {@code CompositeQueryParameter} is joined with
     * the {@link CompositeOperator#ALL ALL} operator.
     *
     * @param column the {@link Column} to add the filter to
     * @param columnFilter the value of the filter to add
     * @return new instance of {@code CompositeQueryParameter} merged from current instance and
     * the given filter
     */
    public CompositeQueryParameter and(Column column, ColumnFilter columnFilter) {
        checkNotNull(column);
        checkNotNull(columnFilter);

        final Multimap<Column, ColumnFilter> newFilters = HashMultimap.create(filters);
        newFilters.put(column, columnFilter);
        final CompositeQueryParameter parameter = from(newFilters, ALL);
        return parameter;
    }

    /**
     * @return whether this parameter contains filters by
     * the {@linkplain org.spine3.server.entity.LifecycleFlags Entity lifecycle columns} or not
     */
    public boolean hasLifecycle() {
        return hasLifecycle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CompositeQueryParameter parameter = (CompositeQueryParameter) o;
        return getOperator() == parameter.getOperator() &&
                Objects.equal(getFilters(), parameter.getFilters());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getOperator(), getFilters());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("operator", operator)
                          .add("filters", filters)
                          .toString();
    }
}
