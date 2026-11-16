package org.spring.divas.order.feature.dish.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.divas.dish.v1.DishChange;
import org.spring.divas.dish.v1.DishStreamServiceGrpc;
import org.spring.divas.dish.v1.SubscriptionRequest;
import org.spring.divas.order.feature.dish.DishService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@ConditionalOnProperty(name = "dish.sync.transport", havingValue = "grpc")
@RequiredArgsConstructor
public class DishStreamGrpcClient {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final DishStreamServiceGrpc.DishStreamServiceStub asyncStub;
    private final DishService dishService;
    private final DishProtoMapper dishProtoMapper;
    private final AtomicBoolean subscribed = new AtomicBoolean();

    @EventListener(ApplicationReadyEvent.class)
    public void subscribeOnStartup() {
        subscribe();
    }

    @Scheduled(fixedDelayString = "${dish-stream.reconnect-delay-ms}")
    public void reconnect() {
        if (!subscribed.get()) {
            subscribe();
        }
    }

    private void subscribe() {
        if (!subscribed.compareAndSet(false, true)) {
            return;
        }
        asyncStub.subscribeDishes(SubscriptionRequest.getDefaultInstance(), new StreamObserver<>() {
            @Override
            public void onNext(DishChange change) {
                dishService.upsert(dishProtoMapper.toRequestDto(change));
            }

            @Override
            public void onError(Throwable throwable) {
                subscribed.set(false);
            }

            @Override
            public void onCompleted() {
                subscribed.set(false);
            }
        });
    }
}
