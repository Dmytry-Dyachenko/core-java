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

package org.spine3.sample;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.spine3.base.CommandRequest;
import org.spine3.base.EventRecord;
import org.spine3.base.UserId;
import org.spine3.eventbus.EventBus;
import org.spine3.sample.order.OrderId;
import org.spine3.sample.order.OrderRootRepository;
import org.spine3.server.*;
import org.spine3.util.UserIds;

import java.util.List;

/**
 * @author Mikhail Mikhaylov
 */
public abstract class BaseSample {

    protected void execute() {
        registerEventSubscribers();
        prepareEngine();

        List<CommandRequest> requests = prepareRequests();
        for (CommandRequest request : requests) {
            Engine.getInstance().handle(request);
        }

        getLog().info("All the requests were handled.");
    }

    protected List<CommandRequest> prepareRequests() {
        List<CommandRequest> result = Lists.newArrayList();

        for (int i = 0; i < 10; i++) {
            OrderId orderId = OrderId.newBuilder().setValue(String.valueOf(i)).build();
            UserId userId = UserIds.create("user" + i);

            CommandRequest createOrder = Requests.createOrder(userId, orderId);
            CommandRequest addOrderLine = Requests.addOrderLine(userId, orderId);
            CommandRequest payOrder = Requests.payOrder(userId, orderId);

            result.add(createOrder);
            result.add(addOrderLine);
            result.add(payOrder);
        }

        return result;
    }

    protected static void registerEventSubscribers() {
        EventBus.instance().register(new EventLogger());
    }

    protected void prepareEngine() {
        final EventStore eventStore = new EventStore(provideEventStoreStorage());
        final CommandStore commandStore = new CommandStore(provideCommandStoreStorage());

        final OrderRootRepository orderRootRepository = getOrderRootRepository();

        Engine.configure(commandStore, eventStore);
        final Engine engine = Engine.getInstance();
        engine.register(orderRootRepository);
    }

    protected abstract Logger getLog();

    protected abstract StorageWithTimelineAndVersion<EventRecord> provideEventStoreStorage();

    protected abstract StorageWithTimeline<CommandRequest> provideCommandStoreStorage();

    protected abstract SnapshotStorage provideSnapshotStorage();

    private OrderRootRepository getOrderRootRepository() {

        final RepositoryEventStore eventStore = new RepositoryEventStore(
                provideEventStoreStorage(),
                provideSnapshotStorage());

        final OrderRootRepository repository = new OrderRootRepository();
        repository.configure(eventStore);
        return repository;
    }
}