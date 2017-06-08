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

package io.spine.server.projection;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.base.Event;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.EntityStorageConverter;
import io.spine.server.entity.idfunc.EventTargetsFunction;
import io.spine.server.event.EventStore;
import io.spine.server.event.EventStoreIO;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.storage.RecordStorageIO;
import io.spine.server.tenant.TenantAwareFunction0;
import io.spine.users.TenantId;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.CoderRegistry;
import org.apache.beam.sdk.coders.IterableCoder;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.FlatMapElements;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.Keys;
import org.apache.beam.sdk.transforms.Max;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.join.CoGbkResult;
import org.apache.beam.sdk.transforms.join.CoGroupByKey;
import org.apache.beam.sdk.transforms.join.KeyedPCollectionTuple;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.sdk.values.TupleTagList;
import org.apache.beam.sdk.values.TypeDescriptor;

/**
 * Performs of a catch-up for the repository for one tenant.
 *
 * @param <I> the type of projection identifiers
 * @author Alexander Yevsyukov
 */
class TenantCatchup<I> {

    private final ProjectionRepository<I, ?, ?> repository;
    private final PipelineOptions options;

    private final TenantId tenantId;

    private final KvCoder<I, Event> eventTupleCoder;
    private final KvCoder<I, Iterable<Event>> idToEventsCoder;
    private final EventTargetsFunction<I, Message> eventTargetsFunction;
    private final RecordStorageIO.ReadFn<I> readRecordFn;
    private final EntityStorageConverter<I, ?, ?> converter;
    private final KvCoder<I, EntityRecord> kvCoder;
    private final ProjectionRepositoryIO.WriteTimestamp writeLastEventTimestamp;
    private final RecordStorageIO.Write<I> writeRecords;
    private final PTransform<PCollection<EventStreamQuery>, PCollection<Event>> queryEvents;

    TenantCatchup(TenantId tenantId,
                  ProjectionRepository<I, ?, ?> repository,
                  PipelineOptions options) {
        this.tenantId = tenantId;
        this.repository = repository;
        this.options = options;

        final ProjectionRepositoryIO<I, ?, ?> repositoryIO = repository.getIO();

        final Coder<I> idCoder = repositoryIO.getIdCoder();
        final Coder<Event> eventCoder = EventStoreIO.getEventCoder();
        this.eventTupleCoder = KvCoder.of(idCoder, eventCoder);
        this.idToEventsCoder = KvCoder.of(idCoder, IterableCoder.of(eventCoder));

        this.eventTargetsFunction = repository.getIdSetFunction();

        this.readRecordFn = repositoryIO.read(tenantId);
        this.converter = repositoryIO.getConverter();
        this.kvCoder = repositoryIO.getKvCoder();
        this.writeLastEventTimestamp = repositoryIO.writeLastHandledEventTime(tenantId);
        this.writeRecords = repositoryIO.write(tenantId);
        final EventStore eventStore = repository.getEventStore();
        this.queryEvents = EventStoreIO.query(tenantId, eventStore);
    }

    @SuppressWarnings({
            "SerializableInnerClassWithNonSerializableOuterClass"
                               /* OK as no dependencies to the outer class in the body. */,
            "serial" /* OK to rely on default Java mechanism for the tags. */})
    private Pipeline createPipeline() {
        Pipeline pipeline = Pipeline.create(options);

        final CoderRegistry coderRegistry = pipeline.getCoderRegistry();
        coderRegistry.registerCoderForType(
                new TypeDescriptor<KV<I, Event>>() {},
                eventTupleCoder);

        // Read events matching the query.
        final EventStreamQuery query = createStreamQuery();
        final PCollection<EventStreamQuery> eventStreamQuery = pipeline.apply(Create.of(query));
        final PCollection<Event> events = eventStreamQuery.apply(
                "QueryEvents", queryEvents);

        // Compose a flat map where a key is a projection ID and a value is an event to apply.
        final PCollection<KV<I, Event>> flatMap = events.apply(
                "MapIdsToEvents", FlatMapElements.via(new GetIds<>(eventTargetsFunction))
        ).setCoder(eventTupleCoder);

        // Group events by projection IDs.
        final PCollection<KV<I, Iterable<Event>>> grouppedEvents =
                flatMap.apply("GroupEvents", GroupByKey.<I, Event>create())
                       .setCoder(idToEventsCoder);

        final PCollection<I> ids = grouppedEvents.apply(Keys.<I>create());
        final PCollection<KV<I, EntityRecord>> projectionStates =
                ids.apply(ParDo.of(readRecordFn));

        // Join ID-to-Event collection with ID-to-EntityRecord collection.
        final TupleTag<Iterable<Event>> eventsTag = new TupleTag<>();
        final TupleTag<EntityRecord> entityRecordsTag = new TupleTag<>();

        PCollection<KV<I, CoGbkResult>> joinedCollection =
                KeyedPCollectionTuple.of(eventsTag, grouppedEvents)
                                     .and(entityRecordsTag, projectionStates)
                                     .apply(CoGroupByKey.<I>create());

        // Apply events to projections.
        final TupleTag<KV<I, EntityRecord>> recordsTag = new ApplyEvents.RecordsTag<>();
        final TupleTag<Timestamp> timestampTag = new ApplyEvents.TimestampTupleTag();

        final PCollectionTuple collectionTuple = joinedCollection.apply(
                "ApplyEvents",
                ParDo.of(new ApplyEvents<>(eventsTag,
                                           entityRecordsTag,
                                           converter,
                                           timestampTag))
                     .withOutputTags(recordsTag, TupleTagList.of(timestampTag))
        );

        // Store projections.
        final PCollection<KV<I, EntityRecord>> records = collectionTuple.get(recordsTag);
        records.setCoder(kvCoder)
               .apply("WriteRecords", writeRecords);

        // Sort last timestamps of last events and write last handled event time.
        final PCollection<Timestamp> timestamps = collectionTuple.get(timestampTag);
        final PCollection<Timestamp> lastTimestamp = timestamps.apply(
                "MaxTimestamp",
                Max.<Timestamp, TimestampComparator>globally(TimestampComparator.getInstance())
        );
        lastTimestamp.apply(writeLastEventTimestamp);

        return pipeline;
    }

    private EventStreamQuery createStreamQuery() {
        final TenantAwareFunction0<EventStreamQuery> fn =
                new TenantAwareFunction0<EventStreamQuery>(tenantId) {
            @Override
            public EventStreamQuery apply() {
                final EventStreamQuery result = repository.createStreamQuery();
                return result;
            }
        };
        return fn.execute();
    }

    public PipelineResult run() {
        final Pipeline pipeline = createPipeline();
        final PipelineResult result = pipeline.run();
        return result;
    }
}
