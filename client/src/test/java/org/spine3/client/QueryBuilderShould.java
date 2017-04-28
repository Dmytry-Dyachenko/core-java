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

package org.spine3.client;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.ProtoJavaMapper;
import org.spine3.test.client.TestEntity;
import org.spine3.test.validate.msg.ProjectId;
import org.spine3.testdata.Sample;
import org.spine3.type.TypeName;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.client.ActorRequestFactory.QueryParameter.eq;
import static org.spine3.test.Verify.assertContains;
import static org.spine3.test.Verify.assertSize;

/**
 * @author Dmytro Dashenkov
 */
public class QueryBuilderShould extends ActorRequestFactoryShould {

    @Test
    public void create_queries_with_type_only() {
        final Class<? extends Message> testEntityClass = TestEntity.class;
        final Query query = factory().query()
                                     .select(testEntityClass)
                                     .build();
        assertNotNull(query);
        assertFalse(query.hasFieldMask());

        final Target target = query.getTarget();
        assertTrue(target.getIncludeAll());

        assertEquals(TypeName.of(testEntityClass)
                             .value(), target.getType());
    }

    @Test
    public void create_queries_with_ids() {
        final int id1 = 314;
        final int id2 = 271;
        final Query query = factory().query()
                                     .select(TestEntity.class)
                                     .whereIdIn(id1, id2)
                                     .build();
        assertNotNull(query);
        assertFalse(query.hasFieldMask());

        final Target target = query.getTarget();
        assertFalse(target.getIncludeAll());

        final EntityFilters entityFilters = target.getFilters();
        final EntityIdFilter idFilter = entityFilters.getIdFilter();
        final Collection<EntityId> idValues = idFilter.getIdsList();
        final Function<EntityId, Integer> transformer = new EntityIdUnpacker<>(int.class);
        final Collection<Integer> intIdValues = Collections2.transform(idValues, transformer);

        assertSize(2, idValues);
        assertThat(intIdValues, containsInAnyOrder(id1, id2));
    }

    @Test
    public void create_queries_with_field_mask() {
        final String fieldName = "TestEntity.firstField";
        final Query query = factory().query()
                                     .select(TestEntity.class)
                                     .fields(fieldName)
                                     .build();
        assertNotNull(query);
        assertTrue(query.hasFieldMask());

        final FieldMask mask = query.getFieldMask();
        final Collection<String> fieldNames = mask.getPathsList();
        assertSize(1, fieldNames);
        assertContains(fieldName, fieldNames);
    }

    @Test
    public void create_queries_with_param() {
        final String columnName = "myImaginaryColumn";
        final Object columnValue = 42;

        final Query query = factory().query()
                                     .select(TestEntity.class)
                                     .where(eq(columnName, columnValue))
                                     .build();
        assertNotNull(query);
        final Target target = query.getTarget();
        assertFalse(target.getIncludeAll());

        final EntityFilters entityFilters = target.getFilters();
        final Map<String, Any> columnFilters = entityFilters.getColumnFilterMap();
        assertSize(1, columnFilters);
        final Any actualValue = columnFilters.get(columnName);
        assertNotNull(columnValue);
        final Int32Value messageValue = AnyPacker.unpack(actualValue);
        final int actualGenericValue = messageValue.getValue();
        assertEquals(columnValue, actualGenericValue);
    }

    @Test
    public void create_queries_with_multiple_params() {
        final String columnName1 = "myColumn";
        final Object columnValue1 = 42;
        final String columnName2 = "oneMore";
        final Object columnValue2 = Sample.messageOfType(ProjectId.class);

        final Query query = factory().query()
                                     .select(TestEntity.class)
                                     .where(eq(columnName1, columnValue1),
                                            eq(columnName2, columnValue2))
                                     .build();
        assertNotNull(query);
        final Target target = query.getTarget();
        assertFalse(target.getIncludeAll());

        final EntityFilters entityFilters = target.getFilters();
        final Map<String, Any> columnFilters = entityFilters.getColumnFilterMap();
        assertSize(2, columnFilters);

        final Any actualValue1 = columnFilters.get(columnName1);
        assertNotNull(actualValue1);
        final int actualGenericValue1 = ProtoJavaMapper.map(actualValue1, int.class);
        assertEquals(columnValue1, actualGenericValue1);

        final Any actualValue2 = columnFilters.get(columnName2);
        assertNotNull(actualValue2);
        final Message actualGenericValue2 = ProtoJavaMapper.map(actualValue2, ProjectId.class);
        assertEquals(columnValue2, actualGenericValue2);
    }

    @SuppressWarnings("OverlyLongMethod")
    // A big test case covering the query arguments co-living
    @Test
    public void create_queries_with_all_arguments() {
        final Class<? extends Message> testEntityClass = TestEntity.class;
        final int id1 = 314;
        final int id2 = 271;
        final String columnName1 = "column1";
        final Object columnValue1 = 42;
        final String columnName2 = "column2";
        final Object columnValue2 = Sample.messageOfType(ProjectId.class);
        final String fieldName = "TestEntity.secondField";
        final Query query = factory().query()
                                     .select(testEntityClass)
                                     .fields(fieldName)
                                     .whereIdIn(id1, id2)
                                     .where(eq(columnName1, columnValue1),
                                            eq(columnName2, columnValue2))
                                     .build();
        assertNotNull(query);

        // Check FieldMask
        final FieldMask mask = query.getFieldMask();
        final Collection<String> fieldNames = mask.getPathsList();
        assertSize(1, fieldNames);
        assertContains(fieldName, fieldNames);

        final Target target = query.getTarget();
        assertFalse(target.getIncludeAll());
        final EntityFilters entityFilters = target.getFilters();

        // Check IDs
        final EntityIdFilter idFilter = entityFilters.getIdFilter();
        final Collection<EntityId> idValues = idFilter.getIdsList();
        final Function<EntityId, Integer> transformer = new EntityIdUnpacker<>(int.class);
        final Collection<Integer> intIdValues = Collections2.transform(idValues, transformer);

        assertSize(2, idValues);
        assertThat(intIdValues, containsInAnyOrder(id1, id2));

        // Check query params
        final Map<String, Any> columnFilters = entityFilters.getColumnFilterMap();
        assertSize(2, columnFilters);

        final Any actualValue1 = columnFilters.get(columnName1);
        assertNotNull(actualValue1);
        final int actualGenericValue1 = ProtoJavaMapper.map(actualValue1, int.class);
        assertEquals(columnValue1, actualGenericValue1);

        final Any actualValue2 = columnFilters.get(columnName2);
        assertNotNull(actualValue2);
        final Message actualGenericValue2 = ProtoJavaMapper.map(actualValue2, ProjectId.class);
        assertEquals(columnValue2, actualGenericValue2);
    }

    @Test
    public void persist_only_last_ids_clause() {
        final Iterable<?> genericIds = asList(newUuid(),
                                              -1,
                                              Sample.messageOfType(ProjectId.class));
        final long[] longIds = {1L, 2L, 3L};
        final Message[] messageIds = {
                Sample.messageOfType(ProjectId.class),
                Sample.messageOfType(ProjectId.class),
                Sample.messageOfType(ProjectId.class)
        };
        final String[] stringIds = {
                newUuid(),
                newUuid(),
                newUuid()
        };
        final int[] intIds = {4, 5, 6};

        final Query query = factory().query()
                                     .select(TestEntity.class)
                                     .whereIdIn(genericIds)
                                     .whereIdIn(longIds)
                                     .whereIdIn(stringIds)
                                     .whereIdIn(intIds)
                                     .whereIdIn(messageIds)
                                     .build();
        assertNotNull(query);

        final Target target = query.getTarget();
        final EntityFilters filters = target.getFilters();
        final Collection<EntityId> entityIds = filters.getIdFilter()
                                                      .getIdsList();
        assertSize(messageIds.length, entityIds);
        final Function<EntityId, ?> transformer = new EntityIdUnpacker<>(ProjectId.class);
        final Collection<?> actualValues = Collections2.transform(entityIds, transformer);
        assertContains(asList(messageIds), actualValues);
    }

    @Test
    public void persist_only_last_field_mask() {
        final Iterable<String> iterableFields = singleton("TestEntity.firstField");
        final String[] arrayFields = {"TestEntity.secondField"};

        final Query query = factory().query()
                                     .select(TestEntity.class)
                                     .fields(iterableFields)
                                     .fields(arrayFields)
                                     .build();
        assertNotNull(query);
        final FieldMask mask = query.getFieldMask();

        final Collection<String> maskFields = mask.getPathsList();
        assertSize(arrayFields.length, maskFields);
        assertContains(asList(arrayFields), maskFields);
    }

    private static class EntityIdUnpacker<T> implements Function<EntityId, T> {

        private final Class<T> targetClass;

        private EntityIdUnpacker(Class<T> targetClass) {
            this.targetClass = targetClass;
        }

        @Override
        public T apply(@Nullable EntityId entityId) {
            assertNotNull(entityId);
            final Any value = entityId.getId();
            final T actual = ProtoJavaMapper.map(value, targetClass);
            return actual;
        }
    }
}
