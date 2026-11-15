package org.spring.divas.order.feature.dish.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.Setter;
import org.spring.divas.dish.v1.DishChange;
import org.spring.divas.dish.v1.DishStreamServiceGrpc;
import org.spring.divas.dish.v1.SubscriptionRequest;

import java.util.concurrent.atomic.AtomicInteger;

final class TestDishStreamService extends DishStreamServiceGrpc.DishStreamServiceImplBase {
    private final AtomicInteger subscriptions = new AtomicInteger();

    @Setter
    private boolean failFirstSubscription;

    @Setter
    private DishChange change;

    @Override
    public void subscribeDishes(
            SubscriptionRequest request,
            StreamObserver<DishChange> responseObserver
    ) {
        int attempt = subscriptions.incrementAndGet();
        if (failFirstSubscription && attempt == 1) {
            responseObserver.onError(Status.UNAVAILABLE.asRuntimeException());
            return;
        }
        responseObserver.onNext(change);
    }

    int subscriptionCount() {
        return subscriptions.get();
    }
}
