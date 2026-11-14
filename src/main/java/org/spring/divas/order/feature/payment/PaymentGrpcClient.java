package org.spring.divas.order.feature.payment;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.divas.order.feature.order.Order;
import org.spring.divas.order.feature.order.OrderRepository;
import org.spring.divas.order.feature.order.OrderStatus;
import org.spring.divas.payment.v1.Money;
import org.spring.divas.payment.v1.OrderCancellation;
import org.spring.divas.payment.v1.OrderPaymentInfo;
import org.spring.divas.payment.v1.OrderPaymentItem;
import org.spring.divas.payment.v1.PaymentConfirmation;
import org.spring.divas.payment.v1.PaymentServiceGrpc;
import org.spring.divas.payment.v1.PaymentStreamRequest;
import org.spring.divas.payment.v1.PaymentStreamResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentGrpcClient {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 100;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    private final PaymentServiceGrpc.PaymentServiceStub asyncStub;
    private final OrderRepository orderRepository;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public CompletableFuture<PaymentStreamResponse> processRealtimePayment(Order order, boolean simulateFailure) {

        CompletableFuture<PaymentStreamResponse> future = new CompletableFuture<>();
        AtomicReference<StreamObserver<PaymentStreamRequest>> requestRef = new AtomicReference<>();

        BigDecimal total = calculateTotal(order);
        Money amount = PaymentProtoMapper.toMoney(total, "USD");

        StreamObserver<PaymentStreamResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(PaymentStreamResponse response) {
                log.info("[gRPC Client] Received response: status={}, msg={}", response.getStatus(), response.getMessage());

                if (response.getStatus() == org.spring.divas.payment.v1.PaymentStatus.PAYMENT_STATUS_PENDING) {
                    order.setStatus(OrderStatus.PAYMENT_PENDING);
                    orderRepository.save(order);

                    PaymentStreamRequest confirmReq = PaymentStreamRequest.newBuilder()
                            .setConfirmation(PaymentConfirmation.newBuilder()
                                    .setPaymentId(response.getPaymentId())
                                    .setOrderId(order.getId())
                                    .setConfirmed(true)
                                    .build())
                            .build();

                    log.info("[gRPC Client Step 4] Sending confirmation for paymentId={}", response.getPaymentId());
                    requestRef.get().onNext(confirmReq);

                } else if (response.getStatus() == org.spring.divas.payment.v1.PaymentStatus.PAYMENT_STATUS_PAID) {
                    log.info("[gRPC Client Step 6] Order orderId={} updated to PAID", order.getId());
                    order.setStatus(OrderStatus.PAID);
                    orderRepository.save(order);

                    future.complete(response);
                    requestRef.get().onCompleted();

                } else if (response.getStatus() == org.spring.divas.payment.v1.PaymentStatus.PAYMENT_STATUS_FAILED) {
                    log.warn("[gRPC Client Step 6] Payment declined: {}", response.getFailureReason());
                    order.setStatus(OrderStatus.PAYMENT_FAILED);
                    orderRepository.save(order);

                    future.complete(response);
                    requestRef.get().onCompleted();
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("[gRPC Client] Payment stream error: {}", t.getMessage());
                future.completeExceptionally(t);
            }

            @Override
            public void onCompleted() {
                log.info("[gRPC Client] Server completed payment stream");
            }
        };

        StreamObserver<PaymentStreamRequest> requestObserver =
                asyncStub.processPaymentStream(responseObserver);
        requestRef.set(requestObserver);

        OrderPaymentInfo.Builder infoBuilder = OrderPaymentInfo.newBuilder()
                .setOrderId(order.getId())
                .setUserId(order.getUserId() != null ? order.getUserId() : 1L)
                .setAmount(amount)
                .setCustomerEmail(order.getEmail() != null ? order.getEmail() : "")
                .setIdempotencyKey("order-grpc-" + order.getId())
                .setSimulateFailure(simulateFailure);

        if (order.getItems() != null) {
            order.getItems().forEach(item -> infoBuilder.addItems(
                    OrderPaymentItem.newBuilder()
                            .setDishId(item.getDishId())
                            .setQuantity(item.getQuantity())
                            .setPrice(PaymentProtoMapper.toMoney(item.getPrice(), "USD"))
                            .build()
            ));
        }

        PaymentStreamRequest firstRequest = PaymentStreamRequest.newBuilder()
                .setOrderInfo(infoBuilder.build())
                .build();

        log.info("[gRPC Client Step 1] Sending order details for orderId={}", order.getId());
        requestObserver.onNext(firstRequest);

        return future;
    }


    public CompletableFuture<PaymentStreamResponse> sendOrderCancellation(
            Long orderId, Long paymentId, String reason) {

        CompletableFuture<PaymentStreamResponse> future = new CompletableFuture<>();

        StreamObserver<PaymentStreamResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(PaymentStreamResponse response) {
                log.info("[gRPC Client Step 7] Received cancellation confirmation: status={}", response.getStatus());
                future.complete(response);
            }

            @Override
            public void onError(Throwable t) {
                log.error("[gRPC Client Step 7] Error during cancellation: {}", t.getMessage());
                future.completeExceptionally(t);
            }

            @Override
            public void onCompleted() {
                log.info("[gRPC Client Step 7] Cancellation stream closed");
            }
        };

        StreamObserver<PaymentStreamRequest> requestObserver = asyncStub.processPaymentStream(responseObserver);

        PaymentStreamRequest cancelReq = PaymentStreamRequest.newBuilder()
                .setCancellation(OrderCancellation.newBuilder()
                        .setOrderId(orderId)
                        .setPaymentId(paymentId != null ? paymentId : 0L)
                        .setReason(reason != null ? reason : "Order was cancelled by customer")
                        .build())
                .build();

        requestObserver.onNext(cancelReq);
        requestObserver.onCompleted();

        return future;
    }

    public CompletableFuture<PaymentStreamResponse> processRealtimePaymentWithRetry(
            Order order, boolean simulateFailure) {
        return executeWithRetry(order, simulateFailure, 1, INITIAL_BACKOFF_MS);
    }

    private CompletableFuture<PaymentStreamResponse> executeWithRetry(
            Order order, boolean simulateFailure, int attempt, long currentBackoffMs) {

        log.info("[gRPC Retry Policy] Attempt #{} for orderId={}", attempt, order.getId());
        CompletableFuture<PaymentStreamResponse> result = new CompletableFuture<>();

        processRealtimePayment(order, simulateFailure).whenComplete((response, throwable) -> {
            if (throwable == null) {
                result.complete(response);
            } else {
                boolean isUnavailable = isUnavailableError(throwable);

                if (isUnavailable && attempt < DEFAULT_MAX_ATTEMPTS) {
                    log.warn("[gRPC Retry Policy] Received UNAVAILABLE on attempt #{}. "
                                    + "Retrying in {} ms...",
                            attempt, currentBackoffMs);

                    scheduler.schedule(() -> {
                        long nextBackoff = (long) (currentBackoffMs * BACKOFF_MULTIPLIER);
                        executeWithRetry(order, simulateFailure, attempt + 1, nextBackoff)
                                .whenComplete((retryRes, retryErr) -> {
                                    if (retryErr != null) {
                                        result.completeExceptionally(retryErr);
                                    } else {
                                        result.complete(retryRes);
                                    }
                                });
                    }, currentBackoffMs, TimeUnit.MILLISECONDS);
                } else {
                    if (isUnavailable) {
                        log.error("[gRPC Retry Policy] All {} attempts exhausted for orderId={}",
                                DEFAULT_MAX_ATTEMPTS, order.getId());
                    }
                    result.completeExceptionally(throwable);
                }
            }
        });

        return result;
    }

    private boolean isUnavailableError(Throwable throwable) {
        if (throwable instanceof StatusRuntimeException sre) {
            return sre.getStatus().getCode() == Status.Code.UNAVAILABLE;
        }
        if (throwable.getCause() instanceof StatusRuntimeException sre) {
            return sre.getStatus().getCode() == Status.Code.UNAVAILABLE;
        }
        return false;
    }

    private BigDecimal calculateTotal(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return order.getItems().stream()
                .map(item -> item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
